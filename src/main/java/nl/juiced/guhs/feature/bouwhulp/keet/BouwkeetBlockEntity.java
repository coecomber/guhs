package nl.juiced.guhs.feature.bouwhulp.keet;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.bouwhulp.BouwhulpFeature;
import nl.juiced.guhs.feature.bouwhulp.kern.Bouwer;
import nl.juiced.guhs.feature.bouwhulp.kern.Materiaal;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.bouwhulp.kern.Spookbouw;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;

/**
 * The Bouwkeet (design 2.2): the hub of the bouwhulp. A guh machine of 3 x 3 x 3 ({@link BouwkeetBlock}) with a level
 * ({@link BouwNiveau}: upgrades, VK while switched on), a linked Bank Guh (the Banksleutel), at most ONE active project
 * (a {@link Spook} it builds as its {@link Bouwer}) and up to four bouwguhs ({@link Bouwguhs}). Its menu is
 * {@code client.BouwkeetScreen} with the tabs of {@link BouwkeetTabs}.
 * <p>
 * Building speed: {@link #BASIS} blocks a second, {@link #PER_GUH} more for every bouwguh that is near, and never slower
 * than the whole project in an hour (design: "meestal binnen enkele minuten, max 1 uur").
 */
public class BouwkeetBlockEntity extends MachineBlockEntity implements Bouwer, GeoBlockEntity {
    public static final double BASIS = 5, PER_GUH = 5;
    public static final int MAX_BOUWGUHS = 4;
    /** Seconds the whole project may take at most. */
    public static final int MAX_SECONDEN = 3600;

    private int niveau;
    private boolean aanGezet = true;
    @Nullable
    private UUID bank;
    @Nullable
    private UUID project;
    private final List<UUID> bouwguhs = new ArrayList<>();
    // --- synced for the looks ---
    private boolean bouwt;
    private int gedaan, totaal;
    // --- server only ---
    private int geplaatstSindsGeluid;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BouwkeetBlockEntity(BlockPos pos, BlockState state) {
        super(BouwhulpFeature.BOUWKEET_BE.get(), pos, state, 0, 1);
    }

    // =====================================================================================================================
    // the machine
    // =====================================================================================================================

    @Override
    protected boolean kanWerken() {
        return false;   // (it builds through its project, see Spoken.tick)
    }

    @Override
    protected boolean isVol() {
        return false;
    }

    @Override
    protected void werk() {
    }

    @Override
    protected boolean aan() {
        return aanGezet;
    }

    @Override
    protected boolean isInvoer(int vak) {
        return false;
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return false;
    }

    /** Level 0 draws nothing; above it the level's VK while switched on. */
    @Override
    public int vadsVraag() {
        return aanGezet && niveau > 0 ? BouwNiveau.van(niveau).vk : 0;
    }

    // =====================================================================================================================
    // state
    // =====================================================================================================================

    /** The level it has (upgrades). */
    public BouwNiveau niveau() {
        return BouwNiveau.van(niveau);
    }

    /** What it can do now: its level while switched on with vadskracht, else handwerk. */
    public BouwNiveau werkNiveau() {
        if (niveau == 0 || !aanGezet) {
            return BouwNiveau.HANDWERK;
        }
        return heeftKracht() ? BouwNiveau.van(niveau) : BouwNiveau.HANDWERK;
    }

    public boolean aanGezet() {
        return aanGezet;
    }

    public void zetAan(boolean aan) {
        if (aan != aanGezet) {
            aanGezet = aan;
            meld();
            sync();
        }
    }

    /** One level up (an upgrade item). False: it is not the next level. */
    public boolean upgrade(BouwNiveau naar) {
        if (naar.nummer() != niveau + 1) {
            return false;
        }
        niveau = naar.nummer();
        meld();
        sync();
        return true;
    }

    /** (dev, tests) set the level directly. */
    public void zetNiveau(int n) {
        niveau = Math.max(0, Math.min(4, n));
        meld();
        sync();
    }

    @Nullable
    public UUID bank() {
        return bank;
    }

    public void zetBank(@Nullable UUID bank) {
        this.bank = bank;
        sync();
    }

    public boolean bouwt() {
        return bouwt;
    }

    public int gedaan() {
        return gedaan;
    }

    public int totaal() {
        return totaal;
    }

    /** The keet's front direction. */
    public Direction voorkant() {
        return getBlockState().hasProperty(MachineBlock.FACING) ? getBlockState().getValue(MachineBlock.FACING) : Direction.NORTH;
    }

    /** A good spot for a new ghost: a little in front of the keet, on the ground level of the keet. */
    public BlockPos voorDeDeur(int afstand) {
        return worldPosition.relative(voorkant(), afstand);
    }

    // =====================================================================================================================
    // the project
    // =====================================================================================================================

    /** The active project, or null (also when its ghost is gone). */
    @Nullable
    public Spook project() {
        if (project == null || !(level instanceof ServerLevel sl)) {
            return null;
        }
        Spook s = Spookbouw.get(sl, project);
        if (s == null || !worldPosition.equals(s.bouwerPos())) {
            return null;
        }
        return s;
    }

    @Nullable
    public UUID projectId() {
        return project;
    }

    /**
     * Makes this ghost the keet's one project (it builds it from now on). An earlier project that is not building stays
     * an open ghost without a keet; one that is still building keeps the keet (false).
     */
    public boolean zetProject(ServerLevel level, Spook s) {
        Spook oud = project();
        if (oud != null && oud != s) {
            if (oud.status() == Spook.Status.BOUWT) {
                return false;
            }
            Spookbouw.koppel(level, oud, null);
        }
        project = s.id();
        Spookbouw.koppel(level, s, worldPosition);
        totaal = s.totaal();
        gedaan = s.gedaan();
        sync();
        return true;
    }

    /** Lets go of the project (the ghost stays, without the keet). */
    public void laatProjectLos(ServerLevel level) {
        Spook s = project();
        if (s != null) {
            Spookbouw.pauzeer(level, s);
            Spookbouw.koppel(level, s, null);
        }
        project = null;
        bouwt = false;
        sync();
    }

    // =====================================================================================================================
    // bouwguhs
    // =====================================================================================================================

    public List<UUID> bouwguhs() {
        return bouwguhs;
    }

    void zetBouwguhs(List<UUID> lijst) {
        bouwguhs.clear();
        bouwguhs.addAll(lijst.subList(0, Math.min(MAX_BOUWGUHS, lijst.size())));
        sync();
    }

    // =====================================================================================================================
    // Bouwer: what the project asks
    // =====================================================================================================================

    @Override
    public double tempo(ServerLevel level, Spook spook) {
        int guhs = werkNiveau().bouwguhs() ? Bouwguhs.aanwezig(level, this) : 0;
        double t = BASIS + PER_GUH * guhs;
        return Math.max(t, spook.totaal() / (double) MAX_SECONDEN);
    }

    @Override
    public Materiaal.Bronnen bronnen(ServerLevel level, Spook spook, @Nullable ServerPlayer bouwer) {
        return Materiaal.bronnen(level, bouwer, bank, werkNiveau().draadloos() ? worldPosition : null);
    }

    @Override
    public boolean pof(ServerLevel level, Spook spook) {
        return werkNiveau().pof();
    }

    @Override
    public void geplaatst(ServerLevel level, Spook spook, BlockPos pos) {
        gedaan = spook.gedaan();
        if (!bouwt) {
            bouwt = true;
            sync();
        }
        if (++geplaatstSindsGeluid >= 6) {
            geplaatstSindsGeluid = 0;
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.4f, 1.1f + level.getRandom().nextFloat() * 0.3f);
        }
    }

    @Override
    public void gestopt(ServerLevel level, Spook spook, boolean klaar) {
        bouwt = false;
        gedaan = spook.gedaan();
        if (klaar) {
            project = null;
            Bouwguhs.juich(level, this);
        }
        sync();
    }

    /** Every tick on the server (also without vadskracht): progress for the looks, the bouwguhs. */
    void altijd() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        if (sl.getGameTime() % 20 == Math.floorMod(worldPosition.asLong(), 20)) {
            Spook s = project();
            boolean nu = s != null && s.status() == Spook.Status.BOUWT;
            int g = s == null ? 0 : s.gedaan(), t = s == null ? 0 : s.totaal();
            if (nu != bouwt || g != gedaan || t != totaal) {
                bouwt = nu;
                gedaan = g;
                totaal = t;
                sync();
            }
            if (s == null && project != null) {
                project = null;
                setChanged();
            }
            Bouwguhs.tickKeet(sl, this, s);
        }
    }

    // =====================================================================================================================
    // saving
    // =====================================================================================================================

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putInt("Niveau", niveau);
        uit.putBoolean("Aan", aanGezet);
        uit.storeNullable("Bank", UUIDUtil.CODEC, bank);
        uit.storeNullable("Project", UUIDUtil.CODEC, project);
        uit.store("Bouwguhs", UUIDUtil.CODEC.listOf(), List.copyOf(bouwguhs));
        uit.putBoolean("Bouwt", bouwt);
        uit.putInt("Gedaan", gedaan);
        uit.putInt("Totaal", totaal);
    }

    @Override
    protected void laden(ValueInput in) {
        niveau = in.getIntOr("Niveau", 0);
        aanGezet = in.getBooleanOr("Aan", true);
        bank = in.read("Bank", UUIDUtil.CODEC).orElse(null);
        project = in.read("Project", UUIDUtil.CODEC).orElse(null);
        bouwguhs.clear();
        in.read("Bouwguhs", UUIDUtil.CODEC.listOf()).ifPresent(bouwguhs::addAll);
        bouwt = in.getBooleanOr("Bouwt", false);
        gedaan = in.getIntOr("Gedaan", 0);
        totaal = in.getIntOr("Totaal", 0);
    }

    // =====================================================================================================================
    // GeckoLib: the crane turns and hoists while it builds; Pof! is a puff of the whole hut
    // =====================================================================================================================

    private static final RawAnimation RUST = RawAnimation.begin().thenLoop("animation.bouwkeet.rust");
    private static final RawAnimation BOUWT = RawAnimation.begin().thenLoop("animation.bouwkeet.bouwt");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("kraan", 8, state -> state.setAndContinue(bouwt ? BOUWT : RUST)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
