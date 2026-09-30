package nl.juiced.guhs.feature.guhpolder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.registry.ModSounds;

/**
 * PUBLIC API for the Elf-Guhjestocht (and anything else where a player skates): a player's tamed Pinguhs belly-slide
 * along next to them, one on the right, one on the left, the next ones a bit further out and behind.
 * <pre>
 *   List&lt;GuhEntity&gt; mee = PinguhMeeglijden.start(player);   // their tamed, not sitting Pinguhs within {@link #BEREIK} blocks join
 *   ...                                                         // they keep up by themselves (and hop back if they fall behind)
 *   PinguhMeeglijden.stop(player);                             // back to normal (also automatic on logout, death, other dimension)
 * </pre>
 * While sliding along, a Pinguh keeps its place beside the skater at the skater's own speed (up to {@link #MAX_SNELHEID}
 * blocks per tick), belly-slides on ice (the client shows the slide pose whenever it goes fast on ice) and waddles/hops
 * across land (a klunplek). Too far behind ({@link #INHALEN} blocks) it hops to its place. Cosy only: it never blocks,
 * pushes or slows the skater. Server side only; the state is not saved (after a restart the guh just follows as usual).
 */
public final class PinguhMeeglijden {
    /** Pinguhs this close to the skater join in. */
    public static final double BEREIK = 24;
    /** Further behind than this, a Pinguh hops (teleports) to its place. */
    public static final double INHALEN = 18;
    /** The fastest a Pinguh slides (blocks per tick). */
    public static final double MAX_SNELHEID = 1.4;

    /** A skater and the Pinguhs sliding along (in order: their places). */
    private static final class Tocht {
        final LinkedHashSet<UUID> guhs = new LinkedHashSet<>();
        final ServerLevel level;
        Vec3 last;
        Vec3 snelheid = Vec3.ZERO;

        Tocht(ServerPlayer player) {
            this.level = player.level();
            this.last = player.position();
        }
    }

    private static final Map<UUID, Tocht> TOCHTEN = new ConcurrentHashMap<>();
    /** guh UUID -> skater UUID (fast check from the goal). */
    private static final Map<UUID, UUID> MEE = new ConcurrentHashMap<>();

    private PinguhMeeglijden() {
    }

    // =================================================================================================================
    // the API
    // =================================================================================================================

    /**
     * The skater's tamed Pinguhs within {@link #BEREIK} blocks (not sitting, not ridden or on a lead) start sliding along.
     * Calling it again adds Pinguhs that came near since. Returns the ones sliding along now (may be empty).
     */
    public static List<GuhEntity> start(ServerPlayer skater) {
        Tocht tocht = TOCHTEN.computeIfAbsent(skater.getUUID(), id -> new Tocht(skater));
        for (GuhEntity guh : skater.level().getEntitiesOfClass(GuhEntity.class, new AABB(skater.blockPosition()).inflate(BEREIK),
                g -> (g.getVariant() == GuhVariant.PINGUH || ookMee(g, skater)) && g.isTame() && g.isOwnedBy(skater) && !g.isOrderedToSit()
                        && !g.isVehicle() && !g.isPassenger() && !g.isLeashed() && g.isAlive())) {
            if (tocht.guhs.add(guh.getUUID())) {
                MEE.put(guh.getUUID(), skater.getUUID());
                guh.getNavigation().stop();
                skater.level().sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 2,
                        0.2, 0.1, 0.2, 0);
                guh.playSound(ModSounds.GUH_HAPPY.get(), 1f, 1.3f);
            }
        }
        if (tocht.guhs.isEmpty()) {
            TOCHTEN.remove(skater.getUUID());
        }
        return meeglijders(skater);
    }

    /** 2.10 (samen): other guhs that may come along too (e.g. your own band guh skates next to you): (guh, skater). */
    private static final List<java.util.function.BiPredicate<GuhEntity, ServerPlayer>> OOK_MEE = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Lets more than Pinguhs come along: a guh for which this says yes joins like a tamed Pinguh does. */
    public static void ookMee(java.util.function.BiPredicate<GuhEntity, ServerPlayer> mag) {
        OOK_MEE.add(mag);
    }

    private static boolean ookMee(GuhEntity guh, ServerPlayer skater) {
        for (var mag : OOK_MEE) {
            if (mag.test(guh, skater)) {
                return true;
            }
        }
        return false;
    }

    /** The skater is done: their Pinguhs stop sliding along (and just follow them as usual). */
    public static void stop(ServerPlayer skater) {
        stop(skater.getUUID());
    }

    public static void stop(UUID skater) {
        Tocht tocht = TOCHTEN.remove(skater);
        if (tocht != null) {
            for (UUID g : tocht.guhs) {
                MEE.remove(g);
            }
        }
    }

    /** Is this guh sliding along with a skater right now? */
    public static boolean glijdtMee(GuhEntity guh) {
        return MEE.containsKey(guh.getUUID());
    }

    /** The Pinguhs sliding along with this skater (loaded ones, in place order). */
    public static List<GuhEntity> meeglijders(ServerPlayer skater) {
        List<GuhEntity> out = new ArrayList<>();
        Tocht tocht = TOCHTEN.get(skater.getUUID());
        if (tocht != null) {
            for (UUID id : tocht.guhs) {
                if (skater.level().getEntity(id) instanceof GuhEntity g) {
                    out.add(g);
                }
            }
        }
        return out;
    }

    /** The spot where the n-th Pinguh wants to be: beside the skater (right, left, further right...), a bit behind. */
    public static Vec3 plek(Vec3 skater, Vec3 richting, int n) {
        Vec3 dir = richting.horizontalDistanceSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : new Vec3(richting.x, 0, richting.z).normalize();
        Vec3 rechts = new Vec3(-dir.z, 0, dir.x);
        int rij = n / 2;
        double kant = (n % 2 == 0 ? 1 : -1) * (1.7 + rij * 1.3);
        return skater.add(rechts.scale(kant)).add(dir.scale(-0.4 - rij * 0.9));
    }

    // =================================================================================================================
    // keeping up
    // =================================================================================================================

    static void hooks() {
        GuhHooks.doelen((guh, goals) -> goals.addGoal(1, new MeeglijGoal(guh)));
    }

    @Nullable
    private static ServerPlayer skater(GuhEntity guh) {
        UUID id = MEE.get(guh.getUUID());
        if (id == null || !(guh.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getPlayerByUUID(id) instanceof ServerPlayer p ? p : null;
    }

    /** Every tick: the skater's speed (for the Pinguhs to match), and the end of a tour that can't go on. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        Tocht tocht = TOCHTEN.get(p.getUUID());
        if (tocht == null) {
            return;
        }
        if (!p.isAlive() || p.level() != tocht.level || p.isSpectator()) {
            stop(p);
            return;
        }
        Vec3 now = p.position();
        Vec3 d = now.subtract(tocht.last);
        tocht.snelheid = d.lengthSqr() > 64 ? Vec3.ZERO : tocht.snelheid.scale(0.4).add(d.scale(0.6));   // (a teleport isn't speed)
        tocht.last = now;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        stop(event.getEntity().getUUID());
    }

    /** The goal of every guh: while it slides along, nothing else moves it (no wandering, following or begging). */
    static final class MeeglijGoal extends Goal {
        private final GuhEntity guh;

        MeeglijGoal(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return MEE.containsKey(guh.getUUID());   // (a Pinguh, or a guh that was let in by ookMee)
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            guh.getNavigation().stop();
        }

        @Override
        public void tick() {
            ServerPlayer p = skater(guh);
            Tocht tocht = p == null ? null : TOCHTEN.get(p.getUUID());
            if (p == null || tocht == null || p.level() != guh.level() || guh.isOrderedToSit() || guh.isLeashed() || guh.isPassenger()) {
                MEE.remove(guh.getUUID());
                if (tocht != null) {
                    tocht.guhs.remove(guh.getUUID());
                }
                return;
            }
            int n = new ArrayList<>(tocht.guhs).indexOf(guh.getUUID());
            Vec3 richting = tocht.snelheid.horizontalDistanceSqr() > 0.0025 ? tocht.snelheid : Vec3.directionFromRotation(0, p.getYRot());
            Vec3 doel = plek(p.position(), richting, Math.max(0, n));
            Vec3 naar = new Vec3(doel.x - guh.getX(), 0, doel.z - guh.getZ());
            guh.getNavigation().stop();
            if (naar.length() > INHALEN) {
                inhalen(guh, doel);
                return;
            }
            Vec3 v = new Vec3(tocht.snelheid.x, 0, tocht.snelheid.z).add(naar.scale(0.3));
            if (v.length() > MAX_SNELHEID) {
                v = v.normalize().scale(MAX_SNELHEID);
            }
            guh.setDeltaMovement(v.x, guh.getDeltaMovement().y, v.z);
            if (guh.horizontalCollision && guh.onGround()) {
                guh.getJumpControl().jump();
            }
            Vec3 kijk = v.horizontalDistanceSqr() > 0.004 ? v : richting;
            float yaw = (float) (Mth.atan2(kijk.z, kijk.x) * (180F / Math.PI)) - 90f;
            guh.setYRot(yaw);
            guh.yBodyRot = yaw;
            guh.yHeadRot = yaw;
        }

        /** Hop to its place (on the ground there), with a little puff. */
        private void inhalen(GuhEntity guh, Vec3 doel) {
            if (!(guh.level() instanceof ServerLevel level)) {
                return;
            }
            BlockPos at = BlockPos.containing(doel);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
            if (Math.abs(y - doel.y) > 4) {
                y = (int) Math.floor(doel.y);
            }
            level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.3, guh.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
            guh.snapTo(doel.x, y, doel.z, guh.getYRot(), guh.getXRot());
            guh.setDeltaMovement(Vec3.ZERO);
            guh.resetFallDistance();
            level.sendParticles(ParticleTypes.SNOWFLAKE, guh.getX(), guh.getY() + 0.3, guh.getZ(), 6, 0.3, 0.2, 0.3, 0.02);
        }
    }

    /** (Tests) forget every tour. */
    static void wis() {
        TOCHTEN.clear();
        MEE.clear();
    }

    /** (Tests) whether an entity is sliding along with this skater. */
    static boolean met(Entity guh, ServerPlayer skater) {
        return skater.getUUID().equals(MEE.get(guh.getUUID()));
    }
}
