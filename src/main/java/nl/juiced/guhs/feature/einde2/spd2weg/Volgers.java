package nl.juiced.guhs.feature.einde2.spd2weg;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.spcast.SpCast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;

/**
 * einde2 "Sponsguh", slice sp_d2_weg (CONTRACT_E2_C 7.7): the player's own Sponsguh and Patguh walk with him from De Ruige
 * Kroeg to the souvenir shop (steps 5-8), inside the box of Schelpenstad. Per-player entities ({@code Zicht.alleenVoor}),
 * never saved ({@link E2SpD2Weg#TIJDELIJK}); they walk after the player and pop over to him when they fall behind (no
 * pathing through walls: a teleport behind him, with a little puff). Their looks follow the story: the moustache from
 * {@code snor} on; Patguh's is gone after his sneeze in {@code dennis_snor}; on the drying table they lie dried and still.
 */
final class Volgers {
    /** The tag of a companion (besides {@link E2SpD2Weg#TIJDELIJK}). */
    static final String TAG = "guhs_einde2_sp_d2_weg_volger";
    /** Blocks: walk after the player from this far; pop over to him from this far. */
    static final double LOOP = 3.2, SPRING = 13;

    private record Paar(SponsguhWezen sponsguh, SponsguhWezen patguh) {
        boolean weg() {
            return sponsguh.isRemoved() || patguh.isRemoved();
        }
    }

    private static final Map<UUID, Paar> PAREN = new ConcurrentHashMap<>();
    /** Per player: the companions lie on the drying table (the tear game): they stay there, dried, until it ends. */
    private static final Map<UUID, Vec3[]> OP_TAFEL = new ConcurrentHashMap<>();

    private Volgers() {
    }

    /** The companion "sponsguh" or "patguh" of this player (null: none right now). */
    @Nullable
    static SponsguhWezen van(ServerPlayer p, String wie) {
        Paar paar = PAREN.get(p.getUUID());
        if (paar == null || paar.weg()) {
            return null;
        }
        return wie.equals("patguh") ? paar.patguh() : paar.sponsguh();
    }

    /** From E2SpD2Weg.regie (every 10 ticks, steps 5-8, in the Guheinde with Schelpenstad known). */
    static void regie(ServerPlayer p, ServerLevel g, int stap) {
        if (!E2SpD2Weg.inSchelpenstad(p, g)) {
            weg(p);
            return;
        }
        Paar paar = PAREN.get(p.getUUID());
        if (paar == null || paar.weg() || paar.sponsguh().level() != g) {
            ruim(p.getUUID());                                         // (only the old pair: a place on the table stays)
            paar = maak(p, g);
            if (paar == null) {
                return;
            }
        }
        boolean snorS = stap > E2SpD2Weg.SNOR;
        boolean snorP = stap == E2SpD2Weg.LOOPGRAAF || stap == E2SpD2Weg.DENNIS && !E2SpD2Weg.lijn().vlag(p, E2SpD2Weg.V_JACHT);
        SpCast.zetLook(paar.sponsguh(), SpCast.SNOR, snorS);
        SpCast.zetLook(paar.patguh(), SpCast.SNOR, snorP);
        Vec3[] tafel = OP_TAFEL.get(p.getUUID());
        if (tafel != null) {
            leg(paar.sponsguh(), tafel[0], 90f);                       // (dried on the table: they stay there until the game ends)
            leg(paar.patguh(), tafel[1], -90f);
            return;
        }
        volg(p, paar.sponsguh(), -1);
        volg(p, paar.patguh(), 1);
    }

    @Nullable
    private static Paar maak(ServerPlayer p, ServerLevel g) {
        SponsguhWezen s = wezen(p, g, E2SponsguhFeature.SPONSGUH.get(), -1);
        SponsguhWezen pt = wezen(p, g, E2SponsguhFeature.PATGUH.get(), 1);
        if (s == null || pt == null) {
            if (s != null) {
                s.discard();
            }
            if (pt != null) {
                pt.discard();
            }
            return null;
        }
        Paar paar = new Paar(s, pt);
        PAREN.put(p.getUUID(), paar);
        return paar;
    }

    @Nullable
    private static SponsguhWezen wezen(ServerPlayer p, ServerLevel g, EntityType<? extends SponsguhWezen> type, int kant) {
        SponsguhWezen w = type.create(g, EntitySpawnReason.TRIGGERED);
        if (w == null) {
            return null;
        }
        Vec3 plek = achter(p, g, kant);
        w.snapTo(plek.x, plek.y, plek.z, p.getYRot(), 0f);
        w.setYHeadRot(p.getYRot());
        w.zetStil(false);
        w.addTag(TAG);
        E2SpD2Weg.eigen(w, p);
        g.addFreshEntity(w);
        g.sendParticles(p, ParticleTypes.POOF, true, false, plek.x, plek.y + 0.6, plek.z, 6, 0.2, 0.3, 0.2, 0.01);
        return w;
    }

    /** A spot just behind the player on his side {@code kant} (-1 left, 1 right), on the ground; the player's own spot if that is in a wall. */
    private static Vec3 achter(ServerPlayer p, ServerLevel g, int kant) {
        double yaw = Math.toRadians(p.getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), lx = Math.cos(yaw), lz = Math.sin(yaw);
        Vec3 v = p.position().add(-fx * 1.6 + lx * kant * -1.0, 0, -fz * 1.6 + lz * kant * -1.0);
        Vec3 op = E2SpD2Weg.grond(g, v);
        BlockPos b = BlockPos.containing(op);
        boolean vrij = g.getBlockState(b).getCollisionShape(g, b).isEmpty() && g.getBlockState(b.above()).getCollisionShape(g, b.above()).isEmpty();
        return vrij && Math.abs(op.y - p.getY()) <= 2 ? op : p.position();
    }

    private static void volg(ServerPlayer p, SponsguhWezen w, int kant) {
        double d = w.distanceTo(p);
        if (d > SPRING || Math.abs(w.getY() - p.getY()) > 6) {
            ServerLevel g = p.level();
            Vec3 plek = achter(p, g, kant);
            g.sendParticles(p, ParticleTypes.POOF, true, false, w.getX(), w.getY() + 0.6, w.getZ(), 5, 0.2, 0.3, 0.2, 0.01);
            w.getNavigation().stop();
            w.teleportTo(plek.x, plek.y, plek.z);
            g.sendParticles(p, ParticleTypes.POOF, true, false, plek.x, plek.y + 0.6, plek.z, 5, 0.2, 0.3, 0.2, 0.01);
        } else if (d > LOOP) {
            w.getNavigation().moveTo(p, p.isSprinting() ? 1.6 : 1.15);
        } else if (d < 1.8) {
            w.getNavigation().stop();
        }
    }

    /** The tear game: both lie dried on the drying table at these spots ({@code aan}), or get up again. */
    static void opTafel(ServerPlayer p, boolean aan, @Nullable Vec3 sponsguh, @Nullable Vec3 patguh) {
        if (aan && sponsguh != null && patguh != null) {
            OP_TAFEL.put(p.getUUID(), new Vec3[] {sponsguh, patguh});
        } else {
            OP_TAFEL.remove(p.getUUID());
        }
        Paar paar = PAREN.get(p.getUUID());
        if (paar == null || paar.weg()) {
            return;                                                    // (made on the next regie, straight onto the table)
        }
        if (aan && sponsguh != null && patguh != null) {
            leg(paar.sponsguh(), sponsguh, 90f);
            leg(paar.patguh(), patguh, -90f);
        } else {
            for (SponsguhWezen w : new SponsguhWezen[] {paar.sponsguh(), paar.patguh()}) {
                SpCast.zetLook(w, SpCast.DROOG, false);
                w.zetStil(false);
            }
        }
    }

    /** A companion dried and still on its spot of the table. */
    private static void leg(SponsguhWezen w, Vec3 plek, float yaw) {
        SpCast.zetLook(w, SpCast.DROOG, true);
        w.zetStil(true);
        w.getNavigation().stop();
        if (w.position().distanceToSqr(plek) > 0.09) {
            w.teleportTo(plek.x, plek.y, plek.z);
        }
        w.setYRot(yaw);
        w.setYBodyRot(yaw);
    }

    /** Takes the player's companions away (step done, left the place, logged out, died). */
    static void weg(ServerPlayer p) {
        weg(p.getUUID());
    }

    static void weg(UUID u) {
        OP_TAFEL.remove(u);
        ruim(u);
    }

    private static void ruim(UUID u) {
        Paar paar = PAREN.remove(u);
        if (paar != null) {
            paar.sponsguh().discard();
            paar.patguh().discard();
        }
    }

    /** (tests) has this player companions right now? */
    static boolean heeft(ServerPlayer p) {
        Paar paar = PAREN.get(p.getUUID());
        return paar != null && !paar.weg();
    }
}
