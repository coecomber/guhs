package nl.juiced.guhs.feature.ring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * bbq2 (ring-kern): the Nine (DESIGN_130 4, "the ring while carried"). Whoever wears the Knabbelring in the Barbecuether
 * for longer than {@link #WACHT} ticks is hunted: nine Knekel-Mika riders ({@link KnekelRuiterEntity}) appear in a ring
 * around that player (only that player's game knows about them) and ride at them. Caught = poof, back at the last rest
 * point ({@link Ring#terugNaarRustpunt}); nothing is lost, nobody is hurt. Ways out: take the ring off (they lose the scent
 * and are gone after {@link KnekelRuiterEntity#KWIJT_TICKS} ticks), outrun them (they are slower than a sprint), or blind
 * them with the Lichtflesje ({@link #verblind}).
 * <p>
 * Chapters: {@link #patrouille} puts a rider on a fixed round (shared, for everybody whose story is there);
 * {@link #jaag} / {@link #einde} start and stop a hunt by hand; {@link #OVERAL} (tests) lets the hunt start in any dimension.
 */
public final class Negen {
    /** This long (ticks) the ring may be worn before the Nine come; the warning comes at {@link #WAARSCHUWING}. */
    public static final int WACHT = 100, WAARSCHUWING = 30;
    public static final int AANTAL = 9;
    /** They appear this far from their prey. */
    public static final double DICHTBIJ = 16, VER = 22;
    /** (tests) hunt in every dimension, not only in the Barbecuether. */
    public static boolean OVERAL;

    /** A running hunt: its riders. */
    private static final Map<UUID, List<UUID>> JACHT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> GEWAARSCHUWD = new ConcurrentHashMap<>();

    /** Do the Nine ride in this level? */
    public static boolean rijdenIn(ServerLevel level) {
        return OVERAL || level.dimension() == BarbecuetherFeature.BARBECUETHER;
    }

    public static boolean wordtGejaagd(ServerPlayer p) {
        return JACHT.containsKey(p.getUUID());
    }

    /** The riders of this player's hunt that are still there. */
    public static List<KnekelRuiterEntity> ruiters(ServerPlayer p) {
        List<KnekelRuiterEntity> out = new ArrayList<>();
        for (UUID id : JACHT.getOrDefault(p.getUUID(), List.of())) {
            if (p.level().getEntity(id) instanceof KnekelRuiterEntity r && r.isAlive()) {
                out.add(r);
            }
        }
        return out;
    }

    /** (every second, per player) the ring is on for too long: the Nine come; the hunt is over when the last rider is gone. */
    static void tick(ServerPlayer p) {
        ServerLevel level = p.level();
        if (wordtGejaagd(p)) {
            if (ruiters(p).isEmpty()) {
                JACHT.remove(p.getUUID());
                if (p.isAlive()) {
                    p.sendSystemMessage(Component.translatable("quest.guhs.ring.negen.kwijt").withStyle(ChatFormatting.GREEN));
                    Ring.behaald(p, "ring_negen_ontsnapt");
                }
            }
            return;
        }
        if (!Ring.om(p) || !rijdenIn(level) || p.isCreative() || p.isSpectator()) {
            GEWAARSCHUWD.remove(p.getUUID());
            return;
        }
        int om = Ring.omTicks(p);
        if (om >= WAARSCHUWING && GEWAARSCHUWD.putIfAbsent(p.getUUID(), level.getGameTime()) == null) {
            p.sendSystemMessage(Component.translatable("quest.guhs.ring.negen.waarschuwing").withStyle(ChatFormatting.RED));
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0f, 0.7f);
        }
        if (om >= WACHT) {
            jaag(p);
        }
    }

    /** The Nine appear around this player and ride at them. Returns how many found a place to stand (0: no hunt). */
    public static int jaag(ServerPlayer p) {
        if (wordtGejaagd(p)) {
            return ruiters(p).size();
        }
        ServerLevel level = p.level();
        List<UUID> ruiters = new ArrayList<>();
        double begin = level.getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; i < AANTAL; i++) {
            double hoek = begin + i * Math.PI * 2 / AANTAL + (level.getRandom().nextDouble() - 0.5) * 0.3;
            Vec3 plek = null;
            for (double afstand = VER; afstand >= DICHTBIJ - 6 && plek == null; afstand -= 3) {
                plek = grond(level, p.getX() + Math.cos(hoek) * afstand, p.getY(), p.getZ() + Math.sin(hoek) * afstand);
            }
            if (plek == null) {
                continue;
            }
            KnekelRuiterEntity r = RingFeature.KNEKEL_RUITER.get().create(level, EntitySpawnReason.TRIGGERED);
            if (r == null) {
                continue;
            }
            r.snapTo(plek.x, plek.y, plek.z, (float) Math.toDegrees(hoek) + 90f, 0f);
            r.zetJager(p.getUUID());
            Zicht.alleenVoor(r, p.getUUID());
            level.addFreshEntity(r);
            level.sendParticles(p, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, true, false, plek.x, plek.y + 1.0, plek.z, 10, 0.5, 0.8, 0.5, 0.01);
            ruiters.add(r.getUUID());
        }
        if (ruiters.isEmpty()) {
            return 0;
        }
        JACHT.put(p.getUUID(), ruiters);
        p.sendSystemMessage(Component.translatable("quest.guhs.ring.negen.komen").withStyle(ChatFormatting.DARK_RED));
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SKELETON_HORSE_AMBIENT, SoundSource.HOSTILE, 1.4f, 0.5f);
        Ring.behaald(p, "ring_negen_gezien");
        return ruiters.size();
    }

    /** A place for a rider to stand near x, z: solid ground with three blocks of air above it, close to height y. */
    @Nullable
    private static Vec3 grond(ServerLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z), by = (int) Math.floor(y);
        if (!level.isLoaded(pos.set(bx, by, bz))) {
            return null;
        }
        for (int i = 0; i <= 16; i++) {
            int dy = (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2);
            int yy = by + dy;
            if (level.getBlockState(pos.set(bx, yy - 1, bz)).blocksMotion() && level.getFluidState(pos.set(bx, yy, bz)).isEmpty()
                    && !level.getBlockState(pos.set(bx, yy, bz)).blocksMotion() && !level.getBlockState(pos.set(bx, yy + 1, bz)).blocksMotion()
                    && !level.getBlockState(pos.set(bx, yy + 2, bz)).blocksMotion()) {
                return new Vec3(bx + 0.5, yy, bz + 0.5);
            }
        }
        return null;
    }

    /** The hunt of this player is over: its riders vanish (caught, the story moved on, they logged out). */
    public static void einde(ServerPlayer p) {
        List<UUID> ruiters = JACHT.remove(p.getUUID());
        GEWAARSCHUWD.remove(p.getUUID());
        if (ruiters == null) {
            return;
        }
        for (ServerLevel level : p.level().getServer().getAllLevels()) {
            for (UUID id : ruiters) {
                if (level.getEntity(id) instanceof KnekelRuiterEntity r) {
                    r.verdwijn();
                }
            }
        }
    }

    /** The Lichtflesje: every rider within straal blocks is blind for ticks. Returns how many. */
    public static int verblind(ServerLevel level, Vec3 plek, double straal, int ticks) {
        int n = 0;
        for (KnekelRuiterEntity r : level.getEntitiesOfClass(KnekelRuiterEntity.class, new AABB(plek, plek).inflate(straal))) {
            if (r.position().distanceToSqr(plek) <= straal * straal) {
                r.verblind(ticks);
                n++;
            }
        }
        return n;
    }

    /**
     * A rider on a fixed round (chapter 5: the guards of the Zwarte Roosterpoort), for everybody whose story is there (it
     * stands behind the structure's sluier like every other creature). ronde: at least two world positions; it starts on
     * the first. It never leaves, is never hurt and only shoves.
     */
    @Nullable
    public static KnekelRuiterEntity patrouille(ServerLevel level, List<BlockPos> ronde) {
        KnekelRuiterEntity r = RingFeature.KNEKEL_RUITER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (r == null || ronde.isEmpty()) {
            return null;
        }
        BlockPos start = ronde.get(0);
        r.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0f, 0f);
        r.zetRoute(ronde);
        level.addFreshEntity(r);
        return r;
    }

    /** A hunter that came back with its chunk after a restart (no hunt knows it): away. */
    static boolean isWees(Entity e) {
        if (!(e instanceof KnekelRuiterEntity r) || !r.isJager()) {
            return false;
        }
        UUID prooi = r.prooi();
        return prooi == null || !JACHT.getOrDefault(prooi, List.of()).contains(r.getUUID());
    }

    static void wisAlles() {
        JACHT.clear();
        GEWAARSCHUWD.clear();
    }

    private Negen() {
    }
}
