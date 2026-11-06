package nl.juiced.guhs.feature.vadsrijk.zone5.droom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5Finale;
import nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5VerhaalPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensPlekken;

/**
 * The flow of the DROOM phase (T-DROOM). The story ({@code Z5Finale}) asks for it after the spy is beaten in De Wolkwaldroom:
 * <ol>
 *   <li><b>Inslapen:</b> the player (+ their party within 24) "sinks into the whale's dream" (WERELDVERHAAL 4.2 + the story's own
 *       line "je zakt omlaag, de walvis in, haar droom in"): a title card, then a teleport onto <b>de Droomwolk</b>, a round cloud
 *       floor (r {@link #STRAAL}) in the open sky, built by {@link #bouw} only into air. The dream is its own place so it never
 *       overlaps t-z5wezens' spy arena in the real Wolkwaldroom (kern would clear the boss bar every tick).</li>
 *   <li><b>De Grauwe Droom</b> ({@link GrauweDroom}, a loose boss, lv 60) swims rounds over the cloud. Falling off = a cloud catches
 *       you and puts you back ({@link #vangnet}).</li>
 *   <li><b>Opgevrolijkt:</b> every helper counts for the story ({@code Z5Finale.droomOpgevrolijkt}), the personal chests land on the
 *       cloud, and each player <b>wakes up</b> at the middle blowhole once their chest is opened (or after {@link #WAKKER_MAX}). After
 *       the fight, stepping off the cloud also wakes you.</li>
 *   <li><b>Herinnering</b> (replay, loot + pet): after the story, sneak 2 s on the middle blowhole ({@code Z5VerhaalPlekken.SPUITGAT})
 *       to dream it again.</li>
 * </ol>
 * Placement: {@code w("z5d_droomwolk", ...)}: t-z5wereld's generated {@code Z5WereldPlekken.plek(id)} wins (reflection), else the
 * number here (free sky east of the tail fluke, above everything of the whale).
 */
public final class Z5Droom {
    /** The middle of the cloud floor (players stand at this y). */
    public static final BlockPos PLEK = w("z5d_droomwolk", 3070, 272, -520);
    public static final int STRAAL = 17;
    /** Where you land (south edge of the cloud, facing the middle). */
    public static final BlockPos AANKOMST = PLEK.offset(0, 0, 13);
    public static final Gebieden.Gebied GEBIED = new Gebieden.Gebied("z5d_droom", PLEK.getX() - 40, PLEK.getZ() - 40, PLEK.getX() + 40, PLEK.getZ() + 40, 60, 60,
            false).metY(PLEK.getY() - 60, PLEK.getY() + 30);
    /** Max ticks between the plof and waking up (when a chest is never opened). */
    public static final int WAKKER_MAX = 20 * 90;
    static final int INTRO_NA = 30;

    /** Player -> game time of their intro (after the teleport, so the chunks are there). */
    private static final Map<UUID, Long> INTRO = new HashMap<>();
    /** Player -> game time they wake up at the latest. */
    private static final Map<UUID, Long> WAKKER = new HashMap<>();
    /** Player -> where their chest stands (woken as soon as it is gone). */
    private static final Map<UUID, BlockPos> KIST = new HashMap<>();
    /** Player -> ticks sneaking on the blowhole (the replay). */
    private static final Map<UUID, Integer> HURKEN = new HashMap<>();
    private static boolean gebouwd;

    private Z5Droom() {
    }

    private static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = Z5WezensPlekken.wereld(id);
        return b != null ? b : new BlockPos(x, y, z);
    }

    // ---- start -------------------------------------------------------------------------------------------------------------------

    /**
     * Starts (or joins) the dream for this player and their party within 24: true = it runs (the boss reports the end). Story
     * ({@code herinnering} false) or replay (true). Only in Het Vadsrijk.
     */
    public static boolean start(ServerPlayer p, boolean herinnering) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
            return false;
        }
        bouw(sl, PLEK, false);
        GrauweDroom b = baas(sl);
        if (b == null) {
            b = spawn(sl, PLEK);
        }
        if (b == null) {
            return false;
        }
        Set<ServerPlayer> wie = new LinkedHashSet<>();
        wie.add(p);
        for (ServerPlayer q : sl.players()) {
            if (q != p && q.distanceTo(p) < 24 && Partijen.zelfdeParty(p, q)) {
                wie.add(q);
            }
        }
        for (ServerPlayer q : wie) {
            naarDroom(q, herinnering);
        }
        return true;
    }

    /** The running dream boss, or null. */
    @Nullable
    public static GrauweDroom baas(ServerLevel sl) {
        if (!sl.isLoaded(PLEK)) {
            return null;
        }
        for (GrauweDroom b : sl.getEntitiesOfClass(GrauweDroom.class, new AABB(PLEK).inflate(STRAAL + 12, 24, STRAAL + 12), x -> !x.geplofd() && !x.isRemoved())) {
            return b;
        }
        return null;
    }

    /** A fresh Grauwe Droom over this cloud (dev/tests too). */
    @Nullable
    public static GrauweDroom spawn(ServerLevel sl, BlockPos midden) {
        GrauweDroom b = Z5DroomRegistry.GRAUWE_DROOM.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (b == null) {
            return null;
        }
        b.snapTo(midden.getX() + 0.5 + GrauweDroom.ZWEM_STRAAL, midden.getY() + 1, midden.getZ() + 0.5, 90, 0);
        b.stelIn(60, Element.WATER, false, Set.of());
        b.zetOntvadst(true);
        b.zetThuis(Vec3.atBottomCenterOf(midden));
        sl.addFreshEntity(b);
        return b;
    }

    /** Fall asleep: title card, the teleport onto the cloud, a little slow falling, the intro shortly after. */
    public static void naarDroom(ServerPlayer q, boolean herinnering) {
        if (!(q.level() instanceof ServerLevel sl)) {
            return;
        }
        RijkScenes.titel(q, Component.translatable("vadsrijk.z5droom.inslapen").withStyle(ChatFormatting.BOLD),
                Component.translatable(herinnering ? "vadsrijk.z5droom.inslapen.herinnering" : "vadsrijk.z5droom.inslapen.onder"), 50, 0x9A90A8);
        sl.getChunk(AANKOMST);
        q.teleportTo(sl, AANKOMST.getX() + 0.5, AANKOMST.getY() + 0.2, AANKOMST.getZ() + 0.5, Set.of(), 180, 0, true);
        q.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
        q.resetFallDistance();
        sl.playSound(null, AANKOMST, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.6f);
        INTRO.put(q.getUUID(), sl.getGameTime() + INTRO_NA);
        WAKKER.remove(q.getUUID());
        KIST.remove(q.getUUID());
    }

    // ---- the end ---------------------------------------------------------------------------------------------------------------------

    /** Per contributor after the plof: counts for the story, the replay flag, and the wake-up timer. */
    static void opgevrolijkt(ServerPlayer p) {
        CompoundTag extra = Spelers.data(p).extra("z5droom");
        extra.putBoolean("opgevrolijkt", true);
        extra.putInt("keer", extra.getIntOr("keer", 0) + 1);
        Spelers.opgeslagen(p);
        Z5Finale.droomOpgevrolijkt(p);
        WAKKER.put(p.getUUID(), p.level().getGameTime() + WAKKER_MAX);
    }

    /** The boss put this player's chest here (they wake once it is opened). */
    static void kist(ServerPlayer p, BlockPos pos) {
        KIST.put(p.getUUID(), pos);
    }

    public static boolean alOpgevrolijkt(ServerPlayer p) {
        return Spelers.data(p).extra("z5droom").getBooleanOr("opgevrolijkt", false);
    }

    /** Wakes the player at the middle blowhole on the whale's back. */
    public static void wakker(ServerPlayer p) {
        WAKKER.remove(p.getUUID());
        KIST.remove(p.getUUID());
        INTRO.remove(p.getUUID());
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        BlockPos doel = veilig(sl, Z5VerhaalPlekken.SPUITGAT.offset(3, 0, 0));
        RijkScenes.titel(p, Component.translatable("vadsrijk.z5droom.wakker").withStyle(ChatFormatting.BOLD),
                Component.translatable("vadsrijk.z5droom.wakker.onder"), 60, 0xFF8AD0);
        p.teleportTo(sl, doel.getX() + 0.5, doel.getY() + 0.1, doel.getZ() + 0.5, Set.of(), p.getYRot(), 0, true);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false));
        p.resetFallDistance();
        sl.sendParticles(new DustParticleOptions(0xFF8AD0, 1.6f), doel.getX() + 0.5, doel.getY() + 1, doel.getZ() + 0.5, 40, 0.8, 1, 0.8, 0);
        sl.playSound(null, doel, SoundEvents.BREEZE_IDLE_GROUND, SoundSource.PLAYERS, 1f, 1.3f);
    }

    /** The first spot from here upward (8 blocks) with a floor and two free blocks; else the spot itself. */
    static BlockPos veilig(ServerLevel sl, BlockPos b) {
        sl.getChunk(b);
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos q = b.above(dy);
            if (sl.getBlockState(q).isAir() && sl.getBlockState(q.above()).isAir() && !sl.getBlockState(q.below()).isAir()) {
                return q;
            }
        }
        return b;
    }

    // ---- the tick ----------------------------------------------------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        ServerLevel sl = server.getLevel(Vadsrijk.DIM);
        if (sl == null) {
            return;
        }
        long nu = sl.getGameTime();
        GrauweDroom b = null;
        boolean bZocht = false;
        for (Iterator<Map.Entry<UUID, Long>> it = INTRO.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> e = it.next();
            if (nu < e.getValue()) {
                continue;
            }
            it.remove();
            if (!bZocht) {
                b = baas(sl);
                bZocht = true;
            }
            if (b != null && sl.getEntity(e.getKey()) instanceof ServerPlayer p && b.zDoetMee(p)) {
                b.intro(p, Spelers.data(p).gezien.add("baas:z5droom"));
            }
        }
        for (Iterator<Map.Entry<UUID, Long>> it = new HashMap<>(WAKKER).entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> e = it.next();
            if (!(sl.getEntity(e.getKey()) instanceof ServerPlayer p)) {
                continue;
            }
            BlockPos k = KIST.get(p.getUUID());
            boolean kistOpen = k != null && nu % 10 == 0 && !(sl.getBlockState(k).getBlock() instanceof nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist);
            if (nu >= e.getValue() || kistOpen) {
                wakker(p);
            }
        }
        if (nu % 5 == 0) {
            vangnet(sl);
            herinnering(sl);
        }
    }

    /** Falling off the cloud: during the fight a cloud puts you back; after it, you wake up. */
    static void vangnet(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            double dx = p.getX() - (PLEK.getX() + 0.5), dz = p.getZ() - (PLEK.getZ() + 0.5);
            if (dx * dx + dz * dz > 40 * 40 || p.getY() > PLEK.getY() - 6 || p.getY() < PLEK.getY() - 120 || p.isSpectator()) {
                continue;
            }
            if (baas(sl) == null && !WAKKER.containsKey(p.getUUID())) {
                wakker(p);
                continue;
            }
            p.teleportTo(sl, AANKOMST.getX() + 0.5, AANKOMST.getY() + 0.5, AANKOMST.getZ() + 0.5, Set.of(), 180, 0, true);
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
            p.resetFallDistance();
            p.sendOverlayMessage(Component.translatable("vadsrijk.z5droom.vangnet").withStyle(ChatFormatting.LIGHT_PURPLE));
            sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 20, 0.6, 0.2, 0.6, 0.02);
        }
    }

    /** The replay: sneak 2 s on the middle blowhole (after the story's dream is cheered up). */
    static void herinnering(ServerLevel sl) {
        BlockPos s = Z5VerhaalPlekken.SPUITGAT;
        for (ServerPlayer p : sl.players()) {
            if (!p.isShiftKeyDown() || p.distanceToSqr(s.getX() + 0.5, s.getY(), s.getZ() + 0.5) > 3.5 * 3.5 || !alOpgevrolijkt(p)
                    || Z5Finale.volgende(p) != null) {
                HURKEN.remove(p.getUUID());
                continue;
            }
            int t = HURKEN.merge(p.getUUID(), 5, Integer::sum);
            if (t == 5) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.z5droom.herinnering.bezig").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (t >= 40) {
                HURKEN.remove(p.getUUID());
                start(p, true);
            }
        }
    }

    // ---- the cloud ----------------------------------------------------------------------------------------------------------------------

    static final BlockState WOLK = Blocks.WHITE_WOOL.defaultBlockState(), WOLK2 = Blocks.LIGHT_GRAY_WOOL.defaultBlockState(),
            NACHT = Blocks.GRAY_WOOL.defaultBlockState(), NACHT2 = Blocks.BLACK_WOOL.defaultBlockState();

    private static boolean vlek(int x, int z) {
        int h = x * 73856093 ^ z * 19349663;
        return Math.floorMod(h, 7) == 0;
    }

    /**
     * The cloud floor around {@code midden} (r {@link #STRAAL}, one block under the feet) + a puffy rim, ONLY into air (never
     * replaces anything). Once per server start unless {@code opnieuw}. Returns the blocks placed.
     */
    public static int bouw(ServerLevel sl, BlockPos midden, boolean opnieuw) {
        if (gebouwd && !opnieuw && midden.equals(PLEK)) {
            return 0;
        }
        if (midden.equals(PLEK)) {
            gebouwd = true;
        }
        int n = 0;
        for (int x = -STRAAL - 2; x <= STRAAL + 2; x++) {
            for (int z = -STRAAL - 2; z <= STRAAL + 2; z++) {
                double r = Math.sqrt(x * x + z * z);
                BlockPos v = midden.offset(x, -1, z);
                if (r <= STRAAL + 0.5) {
                    n += zet(sl, v, vlek(x, z) ? WOLK2 : WOLK);
                } else if (r <= STRAAL + 2.2) {
                    n += zet(sl, v, WOLK);
                    if (vlek(x + 3, z) || vlek(x, z + 5)) {
                        n += zet(sl, v.above(), WOLK);   // a puff on the rim (not on any path: the rim is the edge)
                    }
                }
            }
        }
        return n;
    }

    private static int zet(ServerLevel sl, BlockPos p, BlockState s) {
        if (sl.getBlockState(p).isAir()) {
            sl.setBlock(p, s, Block.UPDATE_CLIENTS);
            return 1;
        }
        return 0;
    }

    /** The "inverted" dream: the cloud floor turns night-grey (true) or back to white (false). Only our own floor blocks. */
    public static int keerOm(ServerLevel sl, BlockPos midden, boolean nacht) {
        int n = 0;
        for (int x = -STRAAL; x <= STRAAL; x++) {
            for (int z = -STRAAL; z <= STRAAL; z++) {
                if (x * x + z * z > (STRAAL + 0.5) * (STRAAL + 0.5)) {
                    continue;
                }
                BlockPos v = midden.offset(x, -1, z);
                BlockState s = sl.getBlockState(v);
                BlockState nieuw = nacht ? (s == WOLK ? NACHT : s == WOLK2 ? NACHT2 : null) : (s == NACHT ? WOLK : s == NACHT2 ? WOLK2 : null);
                if (nieuw != null) {
                    sl.setBlock(v, nieuw, Block.UPDATE_CLIENTS);
                    n++;
                }
            }
        }
        return n;
    }

    /** (dev) forget the "built" flag. */
    static void vergeetBouw() {
        gebouwd = false;
    }
}
