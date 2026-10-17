package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * einde2 phase B, slice h4_vuur: where things are in the Vuurnatie-hoofdstad. Every spot of chapter 4 is a marker of the
 * build (bouw_vuur, {@code data/guhs/einde2/plekken/vuurnatie_hoofdstad.json}); the story only ever uses the ONE guaranteed
 * copy ({@link Plekken#oorsprong}), so a spot is "oorsprong + marker". Nothing here is saved; every answer may be null
 * while the spot of the copy is not known yet (right after a server start).
 */
public final class Plek {
    /** The structure of chapter 4 and the finale. */
    public static final String STRUCTUUR = Einde2Ids.VUURNATIE_HOOFDSTAD;

    private Plek() {
    }

    /** The Guheinde, if the player is in it. */
    @Nullable
    public static ServerLevel guheinde(ServerPlayer p) {
        return GuheindeFeature.isGuheinde(p.level()) ? p.level() : null;
    }

    /**
     * (fix3-h4, game tests only) Where template 0,0,0 of the capital stands for a player outside the Guheinde: a test that put
     * tiles of the capital down in its own world sets this; null in the game.
     */
    @Nullable
    static volatile java.util.function.Function<ServerPlayer, BlockPos> testOorsprong;

    /**
     * (fix4-finale) The level the spots of the capital are in for this player: the Guheinde, or (game tests) the test world
     * while {@link #testOorsprong} points the capital there. Null: neither.
     */
    @Nullable
    public static ServerLevel wereld(ServerPlayer p) {
        java.util.function.Function<ServerPlayer, BlockPos> test = testOorsprong;
        if (test != null && test.apply(p) != null) {
            return p.level();
        }
        return guheinde(p);
    }

    /** Template 0,0,0 of the capital in the player's Guheinde (null: not in the Guheinde, or not known yet). */
    @Nullable
    public static BlockPos oorsprong(ServerPlayer p) {
        java.util.function.Function<ServerPlayer, BlockPos> test = testOorsprong;
        if (test != null && test.apply(p) != null) {
            return test.apply(p);
        }
        ServerLevel g = guheinde(p);
        return g == null ? null : Plekken.oorsprong(g, STRUCTUUR);
    }

    /** The world block of a marker (null: no such marker, or the capital is not known). */
    @Nullable
    public static BlockPos blok(ServerPlayer p, String plek) {
        BlockPos o = oorsprong(p);
        Plekken.Plek m = Plekken.plek(STRUCTUUR, plek);
        return o == null || m == null ? null : o.offset(m.pos());
    }

    /** The middle of the floor of a marker's block (a spot to stand on). */
    @Nullable
    public static Vec3 staan(ServerPlayer p, String plek) {
        BlockPos b = blok(p, plek);
        return b == null ? null : Vec3.atBottomCenterOf(b);
    }

    /** The middle of a zone, on its floor (or of a single marker). */
    @Nullable
    public static Vec3 midden(ServerPlayer p, String zone) {
        BlockPos o = oorsprong(p);
        Plekken.Plek m = Plekken.plek(STRUCTUUR, zone);
        if (o == null || m == null) {
            return null;
        }
        BlockPos tot = m.tot() == null ? m.pos() : m.tot();
        return new Vec3(o.getX() + (m.pos().getX() + tot.getX()) / 2.0 + 0.5, o.getY() + Math.min(m.pos().getY(), tot.getY()),
                o.getZ() + (m.pos().getZ() + tot.getZ()) / 2.0 + 0.5);
    }

    /** The world points of a pad marker (empty when unknown). */
    public static List<Vec3> pad(ServerPlayer p, String pad) {
        BlockPos o = oorsprong(p);
        Plekken.Plek m = Plekken.plek(STRUCTUUR, pad);
        if (o == null || m == null) {
            return List.of();
        }
        return m.punten().stream().map(q -> Vec3.atBottomCenterOf(o.offset(q))).toList();
    }

    /** Is the player inside this zone of the capital (its box, one block of slack upwards)? */
    public static boolean in(ServerPlayer p, String zone) {
        BlockPos o = oorsprong(p);
        Plekken.Plek m = Plekken.plek(STRUCTUUR, zone);
        if (o == null || m == null) {
            return false;
        }
        BlockPos a = o.offset(m.pos()), b = o.offset(m.tot() == null ? m.pos() : m.tot());
        BlockPos q = p.blockPosition();
        return q.getX() >= Math.min(a.getX(), b.getX()) && q.getX() <= Math.max(a.getX(), b.getX())
                && q.getY() >= Math.min(a.getY(), b.getY()) - 1 && q.getY() <= Math.max(a.getY(), b.getY()) + 1
                && q.getZ() >= Math.min(a.getZ(), b.getZ()) && q.getZ() <= Math.max(a.getZ(), b.getZ());
    }

    /** Is the player within r blocks of a marker? */
    public static boolean bij(ServerPlayer p, String plek, double r) {
        BlockPos b = blok(p, plek);
        return b != null && Vec3.atBottomCenterOf(b).distanceToSqr(p.position()) <= r * r;
    }

    /** Is the player somewhere in the capital (its whole box, the veil's margin not included)? */
    public static boolean inHoofdstad(ServerPlayer p) {
        ServerLevel g = guheinde(p);
        BoundingBox doos = g == null ? null : Plekken.doos(g, STRUCTUUR);
        return doos != null && doos.isInside(p.blockPosition());
    }

    /** A soft "poof" to a spot of the capital (a fade without a scene): items, health and the mount stay as they are. */
    public static boolean breng(ServerPlayer p, String plek, float yaw) {
        Vec3 naar = staan(p, plek);
        ServerLevel g = guheinde(p);
        if (naar == null || g == null) {
            return false;
        }
        breng(p, g, naar, yaw);
        return true;
    }

    static void breng(ServerPlayer p, ServerLevel naar, Vec3 plek, float yaw) {
        p.level().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.8, p.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        if (p.isPassenger() && p.getRootVehicle().level() != naar) {
            p.stopRiding();
        }
        p.teleportTo(naar, plek.x, plek.y + 0.05, plek.z, Set.of(), yaw, 0f, true);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        naar.playSound(null, plek.x, plek.y, plek.z, SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 0.8f, 1.2f);
    }
}
