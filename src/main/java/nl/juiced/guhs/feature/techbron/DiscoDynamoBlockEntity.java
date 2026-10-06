package nl.juiced.guhs.feature.techbron;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;

/**
 * The Disco-dynamo's source and turntable: the disc on it plays over and over (vanilla's {@link JukeboxSongPlayer}, so every
 * disc of any mod works and the song is heard like a jukebox's), free tamed guhs within {@link TechbronGetallen#BEREIK}
 * blocks come and dance on the floor ({@link GuhTrek}, the DANSEN emote), and every dancer gives
 * {@link VadsGetallen#DISCO_PER_GUH} VK, at most {@link VadsGetallen#DISCO_MAX_GUHS} dancers. A rare disc (item tag
 * {@code guhs:techbron/zeldzame_plaat}: the mod's own "Ze hangen aan me veh") makes every dancer give
 * {@link TechbronGetallen#DISCO_BONUS} more. Without a disc nobody dances and it gives nothing.
 */
public class DiscoDynamoBlockEntity extends BronBlockEntity {
    /** The four spots, as (to the right, to the back) of the middle of the floor seen from the front: a V behind the desk. */
    private static final double[][] PLEKKEN = {{-1.0, -0.15}, {1.0, -0.15}, {-0.5, 0.95}, {0.5, 0.95}};

    private final GuhTrek.Groep groep = new GuhTrek.Groep(VadsGetallen.DISCO_MAX_GUHS, Emote.DANSEN);
    private final JukeboxSongPlayer speler;
    private ItemStack plaat = ItemStack.EMPTY;
    private int dansers;

    public DiscoDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(TechbronFeature.DISCO_DYNAMO_BE.get(), pos, state);
        this.speler = new JukeboxSongPlayer(this::setChanged, pos);
    }

    // --- the disc ---

    /** The disc on the turntable (empty: none). */
    public ItemStack plaat() {
        return plaat;
    }

    /** The song of the disc on the turntable. */
    public Optional<Holder<JukeboxSong>> liedje() {
        return JukeboxSong.fromStack(plaat);
    }

    public boolean speelt() {
        return speler.isPlaying();
    }

    /** Is a rare disc on the turntable (every dancer gives a little more)? */
    public boolean zeldzaam() {
        return plaat.is(TechbronFeature.ZELDZAME_PLAAT);
    }

    /** The show of the disc on the turntable (both sides). */
    public LichtShow show() {
        return LichtShow.van(plaat);
    }

    /** How many guhs dance right now (synced). */
    public int dansers() {
        return dansers;
    }

    /** Puts this disc on the turntable (empty: takes it off); returns the one that was on it. The song starts next tick. */
    public ItemStack zetPlaat(ItemStack nieuw) {
        ItemStack oud = plaat;
        plaat = nieuw.isEmpty() ? ItemStack.EMPTY : nieuw.copyWithCount(1);
        if (level instanceof ServerLevel server) {
            speler.stop(server, getBlockState());
            if (plaat.isEmpty() || liedje().isEmpty()) {
                groep.laatLos(server, worldPosition);
                dansers = 0;
            }
        }
        sync();
        return oud;
    }

    // --- the floor ---

    /** The box a guh must be in to dance "on the floor". */
    public AABB zone() {
        return zone(0.1);
    }

    /** The four spots on the floor. */
    public List<Vec3> plekken() {
        Direction facing = getBlockState().getValue(BronBlock.FACING);
        Direction rechts = facing.getClockWise(), achter = facing.getOpposite();
        AABB vloer = vloer();
        Vec3 midden = new Vec3((vloer.minX + vloer.maxX) / 2, vloer.minY + DiscoDynamoBlock.VLOER / 16.0, (vloer.minZ + vloer.maxZ) / 2);
        List<Vec3> uit = new ArrayList<>(PLEKKEN.length);
        for (double[] p : PLEKKEN) {
            uit.add(midden.add(rechts.getStepX() * p[0] + achter.getStepX() * p[1], 0, rechts.getStepZ() * p[0] + achter.getStepZ() * p[1]));
        }
        return uit;
    }

    // --- vadskracht ---

    @Override
    public BronSoort vadsSoort() {
        return BronSoort.DISCO_DYNAMO;
    }

    /** What one dancer gives with this disc. */
    public int perDanser() {
        return VadsGetallen.DISCO_PER_GUH + (zeldzaam() ? TechbronGetallen.DISCO_BONUS : 0);
    }

    @Override
    public int vadsAanbod() {
        return liedje().isEmpty() ? 0 : Math.min(dansers, VadsGetallen.DISCO_MAX_GUHS) * perDanser();
    }

    @Override
    protected Snoet gezicht() {
        return liedje().isEmpty() ? Snoet.SLAAPT : dansers >= VadsGetallen.DISCO_MAX_GUHS ? Snoet.VOL : Snoet.WERKT;
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        Optional<Holder<JukeboxSong>> liedje = liedje();
        if (liedje.isEmpty()) {
            regels.accept(Component.translatable("gui.guhs.techbron.disco.geen_plaat").withStyle(ChatFormatting.GRAY));
            return;
        }
        regels.accept(Component.translatable("gui.guhs.techbron.disco.draait", liedje.get().value().description()).withStyle(ChatFormatting.AQUA));
        regels.accept(dansers <= 0 ? Component.translatable("gui.guhs.techbron.disco.leeg", TechbronGetallen.BEREIK).withStyle(ChatFormatting.GRAY)
                : Component.translatable("gui.guhs.techbron.disco.dansers", dansers, VadsGetallen.DISCO_MAX_GUHS).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (zeldzaam()) {
            regels.accept(Component.translatable("gui.guhs.techbron.disco.zeldzaam", TechbronGetallen.DISCO_BONUS).withStyle(ChatFormatting.GOLD));
        }
    }

    // --- ticking ---

    @Override
    protected void tik(ServerLevel level) {
        Optional<Holder<JukeboxSong>> liedje = liedje();
        if (liedje.isPresent()) {
            if (!speler.isPlaying()) {
                speler.play(level, liedje.get());   // the first time, after a load, and again when the song is over: it loops
            }
            speler.tick(level, getBlockState());
        } else if (speler.isPlaying()) {
            speler.stop(level, getBlockState());
        }
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), TechbronGetallen.KIJK) == 0) {
            kijk(level);
        }
    }

    /** One look at the guhs around the floor (once per second; tests call it to skip the wait). */
    public void kijk(ServerLevel level) {
        int nu = 0;
        if (liedje().isPresent()) {
            nu = groep.kijk(level, worldPosition, zone(), zone(0), plekken(), getBlockState().getValue(BronBlock.FACING).toYRot());
            for (GuhEntity guh : groep.bezig()) {
                if (guh.getRandom().nextInt(3) == 0) {
                    level.sendParticles(ParticleTypes.NOTE, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 0,
                            guh.getRandom().nextInt(24) / 24.0, 0, 0, 1);
                }
            }
            if (nu > 0) {
                beloon("disco");
            }
        }
        if (nu != dansers) {
            dansers = nu;
            sync();
        }
    }

    /** Client: glitter above the floor in the colours of the show. */
    @Override
    protected void clientTick() {
        if (level == null || plaat.isEmpty() || level.getRandom().nextInt(3) != 0) {
            return;
        }
        AABB vloer = vloer();
        int kolom = level.getRandom().nextInt(LichtShow.RASTER), rij = level.getRandom().nextInt(LichtShow.RASTER);
        int kleur = show().kleur(kolom, rij, level.getGameTime());
        level.addParticle(new net.minecraft.core.particles.DustParticleOptions(kleur & 0xFFFFFF, 0.7f),
                vloer.minX + level.getRandom().nextDouble() * vloer.getXsize(), vloer.minY + 0.35 + level.getRandom().nextDouble() * 0.5,
                vloer.minZ + level.getRandom().nextDouble() * vloer.getZsize(), 0, 0.02, 0);
    }

    // --- removal, saving ---

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            speler.stop(server, state);
            groep.laatLos(server, pos);
            if (!plaat.isEmpty()) {
                Containers.dropItemStack(server, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, plaat);
                plaat = ItemStack.EMPTY;
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        plaat = in.read("Plaat", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        dansers = Math.max(0, in.getIntOr("Dansers", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput uit) {
        super.saveAdditional(uit);
        if (!plaat.isEmpty()) {
            uit.store("Plaat", ItemStack.CODEC, plaat);
        }
        uit.putInt("Dansers", dansers);
    }
}
