package nl.juiced.guhs.feature.safari.verblijf;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.safari.ParkData;

/**
 * Op Safari! step 2 (S5): the verblijfsbord's block entity: which species, whose board, the home (a circle of 8..32 or a
 * rectangle), the optional sleeping spot, and the family that lives here - always exactly 1 male, 1 female and 2 little
 * ones ({@link #FAMILIE}). It looks after them: a member that is really gone (its whole area loaded and no sign of it) comes
 * back; the board remembers who belongs to it, so a member that wandered off in an unloaded chunk and turns up later sees it
 * is not family any more and goes back to the park (no duplicates, ever). Broken: the family goes back to the park, the
 * gifts drop. Now and then it counts how happy the family is ({@link Tevredenheid}), lets happy animals leave a gift
 * ({@link #cadeaus}), lets the ostrich lay a giant egg, and runs the stork's deliveries ({@link Ooievaar}).
 */
public class VerblijfsbordBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final String[] FAMILIE = {"man", "vrouw", "baby", "baby"};
    public static final int MIN_STRAAL = 8, MAX_STRAAL = 32, START_STRAAL = 12;
    /** Every board that is loaded (for the friends check). */
    static final List<WeakReference<VerblijfsbordBlockEntity>> BORDEN = new ArrayList<>();

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private String soort = "";
    @Nullable
    private UUID eigenaar, bordId;
    private String eigenaarNaam = "";
    private int straal = START_STRAAL, breedte = START_STRAAL, diepte = START_STRAAL;
    private boolean rechthoek;
    @Nullable
    private BlockPos slaapplek;
    private int variatie;
    /** The family: per member its entity id (null: not there yet) and name. */
    private final UUID[] leden = new UUID[FAMILIE.length];
    private final String[] namen = {"", "", "", ""};
    private final SimpleContainer cadeaus = new SimpleContainer(9);
    private long gevoerdDag = -1, verzorgdDag = -1;
    private CompoundTag uitslag = new CompoundTag();
    private long randTot;
    @Nullable
    private UUID randVoor;
    private int teller;
    private long volgendEi = -1;
    // the stork's delivery (Ooievaar)
    @Nullable
    UUID bestemming;
    final List<ItemStack> pakket = new ArrayList<>();
    long aankomst = -1;

    public VerblijfsbordBlockEntity(BlockPos pos, BlockState state) {
        super(VerblijfFeature.BORD_BE.get(), pos, state);
    }

    // --- set-up -----------------------------------------------------------------------------------------------------------

    /** Placed by {@code eigenaar}: a new board id and a new family. */
    public void plaats(ServerLevel level, @Nullable ServerPlayer eigenaar, String soort) {
        this.soort = soort;
        this.eigenaar = eigenaar == null ? null : eigenaar.getUUID();
        this.eigenaarNaam = eigenaar == null ? "" : eigenaar.getGameProfile().name();
        this.bordId = UUID.randomUUID();
        this.variatie = level.getRandom().nextInt(6);
        for (int i = 0; i < leden.length; i++) {
            leden[i] = null;
        }
        for (int i = 0; i < FAMILIE.length; i++) {
            spawnLid(level, i, true);
        }
        herreken(level);
        setChanged();
        sync();
    }

    private void spawnLid(ServerLevel level, int i, boolean welkom) {
        ThuisDier dier = VerblijfFeature.THUISDIER.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (dier == null) {
            return;
        }
        dier.koppel(this, FAMILIE[i], variatie);
        leden[i] = dier.getUUID();
        if (!namen[i].isEmpty()) {
            dier.setCustomName(Component.literal(namen[i]));
        }
        var r = level.getRandom();
        int x = middelpunt().getX(), z = middelpunt().getZ();
        for (int poging = 0; poging < 6; poging++) {
            int dx = r.nextInt(5) - 2, dz = r.nextInt(5) - 2;
            if (dx != 0 || dz != 0) {
                x = middelpunt().getX() + dx;
                z = middelpunt().getZ() + dz;
                break;
            }
        }
        BlockPos p = dier.grond(level, x, z);
        dier.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360f, 0f);
        level.addFreshEntity(dier);
        if (welkom) {
            dier.poef(level);
            dier.doeActie("blij", 60);
        }
        setChanged();
    }

    // --- the home ---------------------------------------------------------------------------------------------------------

    public String soort() {
        return soort;
    }

    @Nullable
    public UUID bordId() {
        return bordId;
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    public String eigenaarNaam() {
        return eigenaarNaam;
    }

    public boolean magBewerken(ServerPlayer p) {
        return eigenaar == null || eigenaar.equals(p.getUUID()) || p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    public int straal() {
        return rechthoek ? Math.max(breedte, diepte) : straal;
    }

    public boolean rechthoek() {
        return rechthoek;
    }

    public int breedte() {
        return breedte;
    }

    public int diepte() {
        return diepte;
    }

    /** The middle of the home: the board itself. */
    public BlockPos middelpunt() {
        return worldPosition;
    }

    @Nullable
    public BlockPos slaapplek() {
        return slaapplek;
    }

    public int variatie() {
        return variatie;
    }

    public boolean familie(UUID id) {
        for (UUID u : leden) {
            if (id.equals(u)) {
                return true;
            }
        }
        return false;
    }

    public String naam(int i) {
        return namen[i];
    }

    @Nullable
    public UUID lid(int i) {
        return leden[i];
    }

    public SimpleContainer cadeaus() {
        return cadeaus;
    }

    public CompoundTag uitslag() {
        return uitslag;
    }

    public void zetStraal(ServerLevel level, int s) {
        straal = Math.max(MIN_STRAAL, Math.min(MAX_STRAAL, s));
        vormVeranderd(level);
    }

    public void zetVorm(ServerLevel level, boolean rechthoek) {
        this.rechthoek = rechthoek;
        vormVeranderd(level);
    }

    public void zetBreedte(ServerLevel level, int b) {
        breedte = Math.max(4, Math.min(MAX_STRAAL, b));
        vormVeranderd(level);
    }

    public void zetDiepte(ServerLevel level, int d) {
        diepte = Math.max(4, Math.min(MAX_STRAAL, d));
        vormVeranderd(level);
    }

    public void zetSlaapplek(@Nullable BlockPos p) {
        slaapplek = p == null ? null : p.immutable();
        setChanged();
    }

    /** Is x,z inside the home (the same shape the animals keep to)? */
    public boolean binnen(int x, int z) {
        int dx = x - worldPosition.getX(), dz = z - worldPosition.getZ();
        return rechthoek ? Math.abs(dx) <= breedte && Math.abs(dz) <= diepte : dx * dx + dz * dz <= straal * straal;
    }

    private void vormVeranderd(ServerLevel level) {
        for (ThuisDier d : dieren(level)) {
            d.neemVorm(this);
        }
        randTonen(null, 200);
        setChanged();
        sync();
    }

    public void hernoem(ServerLevel level, int i, String naam) {
        if (i < 0 || i >= namen.length) {
            return;
        }
        namen[i] = naam.strip();
        if (leden[i] != null && level.getEntity(leden[i]) instanceof ThuisDier d) {
            d.setCustomName(namen[i].isEmpty() ? null : Component.literal(namen[i]));
        }
        setChanged();
    }

    /** The members that are loaded now. */
    public List<ThuisDier> dieren(ServerLevel level) {
        List<ThuisDier> uit = new ArrayList<>();
        for (UUID id : leden) {
            if (id != null && level.getEntity(id) instanceof ThuisDier d && !d.isRemoved()) {
                uit.add(d);
            }
        }
        return uit;
    }

    public void terugroepen(ServerLevel level) {
        for (ThuisDier d : dieren(level)) {
            d.poef(level);
            d.getNavigation().stop();
            BlockPos p = d.grond(level, worldPosition.getX(), worldPosition.getZ());
            d.teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            d.poef(level);
        }
    }

    public void verstoppen(ServerLevel level) {
        for (ThuisDier d : dieren(level)) {
            if (!d.weg()) {
                d.verstop(level, 400);
            }
        }
    }

    public void randTonen(@Nullable ServerPlayer voor, int ticks) {
        if (level != null) {
            randTot = level.getGameTime() + ticks;
            randVoor = voor == null ? randVoor : voor.getUUID();
        }
    }

    // --- care -------------------------------------------------------------------------------------------------------------

    private long dag() {
        return level == null ? 0 : level.getGameTime() / 24000L;
    }

    void gevoerd() {
        gevoerdDag = dag();
        setChanged();
    }

    void verzorgd() {
        verzorgdDag = dag();
        setChanged();
    }

    boolean vandaagGevoerd() {
        return gevoerdDag == dag();
    }

    boolean vandaagGeaaid() {
        return verzorgdDag == dag();
    }

    public int score() {
        return uitslag.getIntOr("Score", 0);
    }

    public void herreken(ServerLevel level) {
        uitslag = Tevredenheid.reken(level, this).tag();
        setChanged();
    }

    /** The mood shown in the menu for a member. */
    public String stemming(ThuisDier d) {
        if (d.slaapt()) {
            return "slaapt";
        }
        int s = score();
        return s >= 70 ? "blij" : s >= 40 ? "tevreden" : "mopperig";
    }

    // --- ticking ----------------------------------------------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, VerblijfsbordBlockEntity be) {
        if (level instanceof ServerLevel sl) {
            be.tick(sl);
        }
    }

    private void tick(ServerLevel level) {
        if (soort.isEmpty() || bordId == null) {
            return;
        }
        teller++;
        if (randTot > level.getGameTime() && teller % 10 == 0) {
            rand(level);
        }
        if (teller % 100 == 0) {
            bewaakFamilie(level);
        }
        if (teller % 600 == 37) {
            herreken(level);
            cadeautje(level);
            slaapplekken(level);
        }
        if (teller % 1200 == 3) {
            ei(level);
        }
        if (aankomst >= 0 && level.getGameTime() >= aankomst) {
            Ooievaar.aangekomen(level, this);
        }
    }

    /** Members that are really gone (all of the area's entities loaded, the member nowhere) come back. */
    private void bewaakFamilie(ServerLevel level) {
        int r = straal() + 10;
        for (int cx = (worldPosition.getX() - r) >> 4; cx <= (worldPosition.getX() + r) >> 4; cx++) {
            for (int cz = (worldPosition.getZ() - r) >> 4; cz <= (worldPosition.getZ() + r) >> 4; cz++) {
                if (!level.areEntitiesLoaded(ChunkPos.pack(cx, cz))) {
                    return;
                }
            }
        }
        for (int i = 0; i < leden.length; i++) {
            if (leden[i] == null || !(level.getEntity(leden[i]) instanceof ThuisDier d) || d.isRemoved()) {
                spawnLid(level, i, true);       // (the old id leaves the list: if that animal ever turns up, it goes)
            }
        }
    }

    private void rand(ServerLevel level) {
        ServerPlayer p = randVoor == null ? null : level.getServer().getPlayerList().getPlayer(randVoor);
        int n = rechthoek ? (breedte + diepte) * 2 : (int) (straal * 3);
        for (int i = 0; i < n; i++) {
            int x, z;
            if (rechthoek) {
                double t = i / (double) n * 4;
                int kant = (int) t;
                double f = t - kant;
                x = worldPosition.getX() + switch (kant) { case 0 -> (int) (-breedte + f * 2 * breedte); case 1 -> breedte;
                    case 2 -> (int) (breedte - f * 2 * breedte); default -> -breedte; };
                z = worldPosition.getZ() + switch (kant) { case 0 -> -diepte; case 1 -> (int) (-diepte + f * 2 * diepte);
                    case 2 -> diepte; default -> (int) (diepte - f * 2 * diepte); };
            } else {
                double hoek = i / (double) n * Math.PI * 2;
                x = worldPosition.getX() + (int) Math.round(Math.cos(hoek) * straal);
                z = worldPosition.getZ() + (int) Math.round(Math.sin(hoek) * straal);
            }
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (p != null) {
                level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, false, x + 0.5, y + 0.3, z + 0.5, 1, 0, 0, 0, 0);
            } else {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 0.3, z + 0.5, 1, 0, 0, 0, 0);
            }
        }
    }

    /** A happy family sometimes leaves a little gift in the board's gift box (design 1.9, "Cadeautjes"). */
    private void cadeautje(ServerLevel level) {
        VerblijfData.Dier d = VerblijfData.dier(soort);
        if (d == null || d.cadeaus().isEmpty() || score() < 60) {
            return;
        }
        if (level.getRandom().nextInt(200) >= score() / 2) {       // (100 %: about 1 in 4 every 30 s)
            return;
        }
        String id = d.cadeaus().get(level.getRandom().nextInt(d.cadeaus().size()));
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.parse(id));
        item.ifPresent(i -> {
            ItemStack rest = cadeaus.addItem(new ItemStack(i));
            if (rest.getCount() == 0) {
                for (ThuisDier t : dieren(level)) {
                    level.sendParticles(ParticleTypes.HEART, t.getX(), t.getY() + t.getBbHeight(), t.getZ(), 2, 0.2, 0.1, 0.2, 0);
                    break;
                }
                setChanged();
            }
        });
    }

    /** The ostrich (a happy female) lays a giant egg about once per in-game day. */
    private void ei(ServerLevel level) {
        VerblijfData.Dier d = VerblijfData.dier(soort);
        if (d == null || !d.ei()) {
            return;
        }
        long nu = level.getGameTime();
        if (volgendEi < 0) {
            volgendEi = nu + 24000;
            return;
        }
        if (nu < volgendEi || score() < 50) {
            return;
        }
        volgendEi = nu + 20000 + level.getRandom().nextInt(8000);
        for (ThuisDier t : dieren(level)) {
            if ("vrouw".equals(t.variant())) {
                t.doeActie("zit", 120);
                ItemStack ei = new ItemStack(VerblijfFeature.REUZEN_EI.get());
                if (cadeaus.addItem(ei).getCount() > 0) {
                    Containers.dropItemStack(level, t.getX(), t.getY() + 0.3, t.getZ(), ei);
                }
                ServerPlayer p = eigenaar == null ? null : level.getServer().getPlayerList().getPlayer(eigenaar);
                if (p != null && p.level() == level && p.distanceToSqr(t) < 64 * 64) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.ei"));
                }
                setChanged();
                return;
            }
        }
    }

    /** At night the family sleeps on the sleeping spot, when there is one (they pop over to it when they fall asleep). */
    private void slaapplekken(ServerLevel level) {
        if (slaapplek == null || !level.isDarkOutside()) {
            return;
        }
        for (ThuisDier d : dieren(level)) {
            if (d.slaapt() && !d.weg() && !d.isVehicle() && d.distanceToSqr(slaapplek.getX() + 0.5, d.getY(), slaapplek.getZ() + 0.5) > 9) {
                BlockPos p = d.grond(level, slaapplek.getX() + level.getRandom().nextInt(3) - 1, slaapplek.getZ() + level.getRandom().nextInt(3) - 1);
                d.teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            }
        }
    }

    /** Broken (or replaced): the family goes back to the park, the gifts and a parcel on its way drop here. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl) {
            for (ThuisDier d : dieren(sl)) {
                d.poef(sl);
                d.discard();
            }
            Containers.dropContents(sl, pos, cadeaus);
            for (ItemStack s : pakket) {
                Containers.dropItemStack(sl, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
            }
            pakket.clear();
            for (int i = 0; i < leden.length; i++) {
                leden[i] = null;
            }
            bordId = null;
        }
    }

    // --- the friends registry -------------------------------------------------------------------------------------------

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide()) {
            synchronized (BORDEN) {
                BORDEN.add(new WeakReference<>(this));
            }
        }
    }

    static List<VerblijfsbordBlockEntity> bordenBij(ServerLevel level, BlockPos p, int r) {
        List<VerblijfsbordBlockEntity> uit = new ArrayList<>();
        synchronized (BORDEN) {
            Iterator<WeakReference<VerblijfsbordBlockEntity>> it = BORDEN.iterator();
            while (it.hasNext()) {
                VerblijfsbordBlockEntity be = it.next().get();
                if (be == null || be.isRemoved()) {
                    it.remove();
                } else if (be.level == level && be.worldPosition.distSqr(p) <= (double) r * r) {
                    uit.add(be);
                }
            }
        }
        return uit;
    }

    // --- saving -----------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        soort = in.getStringOr("Soort", "");
        eigenaar = in.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        eigenaarNaam = in.getStringOr("EigenaarNaam", "");
        bordId = in.read("BordId", UUIDUtil.CODEC).orElse(null);
        straal = in.getIntOr("Straal", START_STRAAL);
        rechthoek = in.getBooleanOr("Rechthoek", false);
        breedte = in.getIntOr("Breedte", START_STRAAL);
        diepte = in.getIntOr("Diepte", START_STRAAL);
        int sy = in.getIntOr("SlaapY", Integer.MIN_VALUE);
        slaapplek = sy == Integer.MIN_VALUE ? null : new BlockPos(in.getIntOr("SlaapX", 0), sy, in.getIntOr("SlaapZ", 0));
        variatie = in.getIntOr("Variatie", 0);
        for (int i = 0; i < leden.length; i++) {
            leden[i] = in.read("Lid" + i, UUIDUtil.CODEC).orElse(null);
            namen[i] = in.getStringOr("Naam" + i, "");
        }
        cadeaus.clearContent();
        net.minecraft.core.NonNullList<ItemStack> lijst = net.minecraft.core.NonNullList.withSize(cadeaus.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in.childOrEmpty("Cadeaus"), lijst);
        for (int i = 0; i < lijst.size(); i++) {
            cadeaus.setItem(i, lijst.get(i));
        }
        gevoerdDag = in.getLongOr("GevoerdDag", -1L);
        verzorgdDag = in.getLongOr("VerzorgdDag", -1L);
        volgendEi = in.getLongOr("VolgendEi", -1L);
        bestemming = in.read("Bestemming", UUIDUtil.CODEC).orElse(null);
        aankomst = in.getLongOr("Aankomst", -1L);
        pakket.clear();
        net.minecraft.core.NonNullList<ItemStack> p = net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in.childOrEmpty("Pakket"), p);
        p.stream().filter(s -> !s.isEmpty()).forEach(pakket::add);
        uitslag = in.read("Uitslag", CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString("Soort", soort);
        out.storeNullable("Eigenaar", UUIDUtil.CODEC, eigenaar);
        out.putString("EigenaarNaam", eigenaarNaam);
        out.storeNullable("BordId", UUIDUtil.CODEC, bordId);
        out.putInt("Straal", straal);
        out.putBoolean("Rechthoek", rechthoek);
        out.putInt("Breedte", breedte);
        out.putInt("Diepte", diepte);
        if (slaapplek != null) {
            out.putInt("SlaapX", slaapplek.getX());
            out.putInt("SlaapY", slaapplek.getY());
            out.putInt("SlaapZ", slaapplek.getZ());
        }
        out.putInt("Variatie", variatie);
        for (int i = 0; i < leden.length; i++) {
            out.storeNullable("Lid" + i, UUIDUtil.CODEC, leden[i]);
            out.putString("Naam" + i, namen[i]);
        }
        net.minecraft.core.NonNullList<ItemStack> lijst = net.minecraft.core.NonNullList.withSize(cadeaus.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < lijst.size(); i++) {
            lijst.set(i, cadeaus.getItem(i));
        }
        ContainerHelper.saveAllItems(out.child("Cadeaus"), lijst);
        out.putLong("GevoerdDag", gevoerdDag);
        out.putLong("VerzorgdDag", verzorgdDag);
        out.putLong("VolgendEi", volgendEi);
        out.storeNullable("Bestemming", UUIDUtil.CODEC, bestemming);
        out.putLong("Aankomst", aankomst);
        net.minecraft.core.NonNullList<ItemStack> p = net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(9, pakket.size()); i++) {
            p.set(i, pakket.get(i));
        }
        ContainerHelper.saveAllItems(out.child("Pakket"), p);
        out.store("Uitslag", CompoundTag.CODEC, uitslag);
    }

    /** The client only needs the species (for the picture) and the owner's name. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        t.putString("Soort", soort);
        t.putString("EigenaarNaam", eigenaarNaam);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** The species on the client (from the update tag; loadAdditional reads the same key). */
    public String soortClient() {
        return soort;
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // (a sign: no animation)
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** A container next to the board (6 sides and the 4 diagonals at its height), or null: the stork's parcel source. */
    @Nullable
    Container kistErnaast(ServerLevel level) {
        for (BlockPos q : BlockPos.betweenClosed(worldPosition.offset(-1, -1, -1), worldPosition.offset(1, 1, 1))) {
            if (!q.equals(worldPosition) && level.getBlockEntity(q) instanceof Container c && !(level.getBlockEntity(q) instanceof OoievaarsbusBlockEntity)) {
                return c;
            }
        }
        return null;
    }

    /** (The ParkData species of this board, for the client and the menu.) */
    @Nullable
    ParkData.Soort info() {
        return ParkData.get().soort(soort);
    }
}
