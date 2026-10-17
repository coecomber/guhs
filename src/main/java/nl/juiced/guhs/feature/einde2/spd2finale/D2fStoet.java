package nl.juiced.guhs.feature.einde2.spd2finale;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.DavidHasselguhEntity;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.EigenWezen;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spcast.SpCast;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.ring.Zicht;

/**
 * einde2 "Sponsguh", slice sp_d2_finale: the entities of ONE player's finale (CONTRACT_E2_C 2.6: per player, through
 * {@code Zicht.alleenVoor}): his Sponsguh (the crown in his paws) and Patguh walking with him, his own David Hasselguh, his
 * six bucket-helmet guhs and the ones he freed (they walk along to the Bellenkom), Plankmika on the stage. They live only as
 * long as this server run and this player's step need them: every one carries the tag {@value #MERK} and the id of the run
 * ({@link #RUN}); one that comes back from a saved chunk of an earlier run is thrown away when it joins the level
 * ({@link #opJoin}), and they all go when the player leaves the place, logs out, dies or his step moves on. The story's state
 * itself is never here: it is in the line (the step, the teller of the freed guhs).
 */
final class D2fStoet {
    static final String MERK = "guhs_einde2_sp_d2_finale";
    static final String RUN_KEY = "guhs_einde2_sp_d2_finale_run";
    /** This server run (an entity of another run is stale). */
    static final String RUN = UUID.randomUUID().toString();
    /** Followers: walk when further than this, pop up next to the player when further than that (or in another level). */
    static final double LOPEN = 3.5, SPRINGEN = 22;

    /** One player's entities. */
    static final class Sessie {
        @Nullable
        SponsguhWezen sponsguh, patguh, plankmika;
        @Nullable
        DavidHasselguhEntity david;
        final SponsguhWezen[] emmers = new SponsguhWezen[D2fEmmers.AANTAL];
        final List<SponsguhWezen> vrij = new ArrayList<>();
        /** Server tick of the last counter line (the action bar is refreshed once a second). */
        long laatsteTeller;
    }

    private static final Map<UUID, Sessie> SESSIES = new ConcurrentHashMap<>();

    private D2fStoet() {
    }

    static Sessie van(ServerPlayer p) {
        return SESSIES.computeIfAbsent(p.getUUID(), u -> new Sessie());
    }

    @Nullable
    static Sessie als(ServerPlayer p) {
        return SESSIES.get(p.getUUID());
    }

    // =====================================================================================================================
    // making and removing
    // =====================================================================================================================

    /** A new entity of this player's finale at this spot (only he sees it; stands still until told otherwise). */
    @Nullable
    static <T extends Entity> T maak(ServerPlayer p, EntityType<T> type, Vec3 plek, float yaw) {
        ServerLevel level = p.level();
        T e = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (e == null) {
            return null;
        }
        e.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        e.setYHeadRot(yaw);
        Zicht.alleenVoor(e, p.getUUID());
        e.addTag(MERK);
        e.getPersistentData().putString(RUN_KEY, RUN);
        e.setInvulnerable(true);
        if (e instanceof SponsguhWezen w) {
            w.zetStil(true);
            w.setYBodyRot(yaw);
        }
        if (e instanceof Mob m) {
            m.setPersistenceRequired();
        }
        level.addFreshEntity(e);
        return e;
    }

    static boolean levend(@Nullable Entity e, ServerPlayer p) {
        return e != null && e.isAlive() && !e.isRemoved() && e.level() == p.level();
    }

    /** Gone with a little puff of bubbles. */
    static void weg(@Nullable Entity e) {
        if (e == null || e.isRemoved()) {
            return;
        }
        if (e.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
        }
        e.stopRiding();
        e.discard();
    }

    /** Everything of this player's finale goes (the line keeps the story). */
    static void ruimOp(UUID speler) {
        Sessie s = SESSIES.remove(speler);
        if (s == null) {
            return;
        }
        weg(s.sponsguh);
        weg(s.patguh);
        weg(s.plankmika);
        if (s.david != null && !s.david.onderweg()) {
            weg(s.david);
        }
        for (SponsguhWezen e : s.emmers) {
            weg(e);
        }
        s.vrij.forEach(D2fStoet::weg);
    }

    static void ruimOp(ServerPlayer p) {
        ruimOp(p.getUUID());
    }

    // =====================================================================================================================
    // Sponsguh and Patguh with you
    // =====================================================================================================================

    /**
     * Sponsguh (the crown in his paws until the song) and Patguh walk with the player. A Sponsguh or Patguh of another slice
     * that is still with him (his own copies of the road trip, CONTRACT_E2_C 7.7) is sent home first: never two of the same.
     */
    static void metJe(ServerPlayer p, boolean kroon) {
        Sessie s = van(p);
        if (!levend(s.sponsguh, p) || !levend(s.patguh, p)) {
            stuurAndereWeg(p);
        }
        if (!levend(s.sponsguh, p)) {
            s.sponsguh = maak(p, E2SponsguhFeature.SPONSGUH.get(), naast(p, 1.6, -1.2), p.getYRot());
            if (s.sponsguh != null) {
                s.sponsguh.zetStil(false);
            }
        }
        if (!levend(s.patguh, p)) {
            s.patguh = maak(p, E2SponsguhFeature.PATGUH.get(), naast(p, -1.6, -1.2), p.getYRot());
            if (s.patguh != null) {
                s.patguh.zetStil(false);
            }
        }
        if (s.sponsguh != null) {
            if (SpCast.heeftLook(s.sponsguh, SpCast.KROON_VAST) != kroon) {
                SpCast.zetLook(s.sponsguh, SpCast.KROON_VAST, kroon);
            }
            volg(p, s.sponsguh);
        }
        if (s.patguh != null) {
            volg(p, s.patguh);
        }
    }

    /** Another slice's personal Sponsguh / Patguh of this player near him (not his buddy, not a resident, not ours). */
    static void stuurAndereWeg(ServerPlayer p) {
        for (SponsguhWezen w : p.level().getEntitiesOfClass(SponsguhWezen.class, p.getBoundingBox().inflate(64),
                w -> (w.cast() == Cast.SPONSGUH || w.cast() == Cast.PATGUH) && !w.entityTags().contains(MERK)
                        && p.getUUID().equals(Zicht.eigenaar(w)) && !Bewoners.isBewoner(w) && !(w instanceof EigenWezen ew && ew.heeftEigenaar()))) {
            weg(w);
        }
    }

    /** A spot beside and behind the player (opzij: + = his right), on the floor where there is one. */
    static Vec3 naast(ServerPlayer p, double opzij, double achter) {
        double yaw = Math.toRadians(p.getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);                     // forward
        double rx = -fz, rz = fx;                                            // right
        Vec3 plek = new Vec3(p.getX() + rx * opzij + fx * achter, p.getY(), p.getZ() + rz * opzij + fz * achter);
        BlockPos b = BlockPos.containing(plek);
        ServerLevel level = p.level();
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos q = b.above(dy);
            if (level.getBlockState(q).getCollisionShape(level, q).isEmpty() && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()
                    && !level.getBlockState(q.below()).getCollisionShape(level, q.below()).isEmpty()) {
                return new Vec3(plek.x, q.getY(), plek.z);
            }
        }
        return p.position();
    }

    /** A follower walks after the player; far away (or in another level) it pops up beside him. */
    static void volg(ServerPlayer p, Mob m) {
        if (m.isPassenger() || p.isPassenger()) {
            return;
        }
        if (m.level() != p.level() || m.distanceToSqr(p) > SPRINGEN * SPRINGEN) {
            if (m.level() != p.level()) {
                return;                                                      // (another level: the session is cleaned up by the step logic)
            }
            Vec3 naar = naast(p, (m.getId() % 3 - 1) * 1.4, -1.6);
            m.getNavigation().stop();
            m.teleportTo(naar.x, naar.y, naar.z);
            p.level().sendParticles(ParticleTypes.BUBBLE_POP, naar.x, naar.y + 0.6, naar.z, 6, 0.3, 0.3, 0.3, 0.02);
            return;
        }
        if (m.distanceToSqr(p) > LOPEN * LOPEN) {
            m.getNavigation().moveTo(p, 1.25);
        } else if (!m.getNavigation().isDone()) {
            m.getNavigation().stop();
            m.getLookControl().setLookAt(p);
        }
    }

    // =====================================================================================================================
    // stale entities of an earlier run, the end of a player
    // =====================================================================================================================

    /** An entity of the finale from a saved chunk of an earlier server run, or of a player who is not on it any more. */
    static void opJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (event.getLevel().isClientSide() || !e.entityTags().contains(MERK)) {
            return;
        }
        if (!RUN.equals(e.getPersistentData().getStringOr(RUN_KEY, ""))) {
            event.setCanceled(true);
            e.discard();
        }
    }
}
