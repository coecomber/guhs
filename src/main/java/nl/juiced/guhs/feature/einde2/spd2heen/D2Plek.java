package nl.juiced.guhs.feature.einde2.spd2heen;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * einde2 "Sponsguh", slice sp_d2_heen: where things are in Bikinibodem and Schelpenstad. Every spot is a marker of the two
 * builds (the sidecars {@code data/guhs/einde2/plekken/bikinibodem.json} / {@code schelpenstad.json}); the story only uses the
 * ONE guaranteed copy of each ({@link Plekken#oorsprong}), so a spot is "origin + marker" (a leegte_bouw is never turned).
 * Nothing here is saved; every answer may be null while the copy is not known yet (right after a server start).
 */
public final class D2Plek {
    public static final String BB = SpVerhaal.BIKINIBODEM, SP = SpVerhaal.SCHELPENSTAD;

    /**
     * (game tests only) Where template 0,0,0 of a structure stands for a player outside the Guheinde: a test sets this; null in
     * the game.
     */
    @Nullable
    static volatile BiFunction<ServerPlayer, String, BlockPos> testOorsprong;

    private D2Plek() {
    }

    /** The Guheinde, if the player is in it. */
    @Nullable
    static ServerLevel guheinde(ServerPlayer p) {
        return GuheindeFeature.isGuheinde(p.level()) ? p.level() : null;
    }

    /** Template 0,0,0 of the structure in the player's level (null: not in the Guheinde, or the copy is not known yet). */
    @Nullable
    static BlockPos oorsprong(ServerPlayer p, String structuur) {
        BiFunction<ServerPlayer, String, BlockPos> test = testOorsprong;
        if (test != null) {
            return test.apply(p, structuur);
        }
        ServerLevel g = guheinde(p);
        return g == null || !Plekken.bekend(g, structuur) ? null : Plekken.oorsprong(g, structuur);
    }

    /** The world block of a marker (null: no such marker, or the copy is not known). */
    @Nullable
    static BlockPos blok(ServerPlayer p, String structuur, String plek) {
        BlockPos o = oorsprong(p, structuur);
        Plekken.Plek m = Plekken.plek(structuur, plek);
        return o == null || m == null ? null : o.offset(m.pos());
    }

    /** The middle of the floor of a marker's block (a spot to stand on). */
    @Nullable
    static Vec3 staan(ServerPlayer p, String structuur, String plek) {
        BlockPos b = blok(p, structuur, plek);
        return b == null ? null : Vec3.atBottomCenterOf(b);
    }

    /** The world points of a pad marker (the middle of each block's floor; empty when unknown). */
    static List<Vec3> pad(ServerPlayer p, String structuur, String pad) {
        BlockPos o = oorsprong(p, structuur);
        Plekken.Plek m = Plekken.plek(structuur, pad);
        if (o == null || m == null) {
            return List.of();
        }
        return m.punten().stream().map(q -> Vec3.atBottomCenterOf(o.offset(q))).toList();
    }

    /** Is the player inside this zone (its box, one block of slack up and down, {@code rand} blocks of slack sideways)? */
    static boolean in(ServerPlayer p, String structuur, String zone, int rand) {
        BlockPos o = oorsprong(p, structuur);
        Plekken.Plek m = Plekken.plek(structuur, zone);
        if (o == null || m == null) {
            return false;
        }
        BlockPos a = o.offset(m.pos()), b = o.offset(m.tot() == null ? m.pos() : m.tot());
        BlockPos q = p.blockPosition();
        return q.getX() >= Math.min(a.getX(), b.getX()) - rand && q.getX() <= Math.max(a.getX(), b.getX()) + rand
                && q.getY() >= Math.min(a.getY(), b.getY()) - 1 && q.getY() <= Math.max(a.getY(), b.getY()) + 1
                && q.getZ() >= Math.min(a.getZ(), b.getZ()) - rand && q.getZ() <= Math.max(a.getZ(), b.getZ()) + rand;
    }

    /** Is the player within r blocks (sideways; at most 4 up or down) of a marker? */
    static boolean bij(ServerPlayer p, String structuur, String plek, double r) {
        Vec3 v = staan(p, structuur, plek);
        return v != null && dichtbij(p.position(), v, r);
    }

    /** Sideways within r, at most 4 blocks up or down. */
    static boolean dichtbij(Vec3 a, Vec3 b, double r) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return dx * dx + dz * dz <= r * r && Math.abs(a.y - b.y) <= 4;
    }

    /** The yaw that looks from a to b (Minecraft: 0 south, 90 west). */
    static float yaw(Vec3 van, Vec3 naar) {
        return (float) (Math.toDegrees(Math.atan2(-(naar.x - van.x), naar.z - van.z)));
    }

    /** A soft "poof" to a spot (a fade without a scene): items, health stay as they are; a vehicle is left. */
    static void breng(ServerPlayer p, Vec3 plek, float yaw) {
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.8, p.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        if (p.isPassenger()) {
            p.stopRiding();
        }
        p.teleportTo(level, plek.x, plek.y + 0.05, plek.z, Set.of(), yaw, 0f, true);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        level.playSound(null, plek.x, plek.y, plek.z, SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 0.8f, 1.2f);
    }
}
