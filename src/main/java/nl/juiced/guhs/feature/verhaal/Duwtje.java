package nl.juiced.guhs.feature.verhaal;

import java.util.Set;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.9): the only way anything of this update "attacks" a player: a gentle shove or
 * being put back somewhere. Never damage, never lost items. Every Mika-type creature, obstacle and boss goes through
 * here (never {@code hurt}), so a player who watches a cutscene, reads a narrator card or talks to someone is left alone:
 * <pre>
 * if (Duwtje.mag(p)) {
 *     Duwtje.duw(p, p.position().subtract(mika.position()), 0.9);      // a shove away from the Mika
 * }
 * Duwtje.terug(p, dim, rustpunt, yaw);                                   // poof: back at the rest point
 * </pre>
 */
public final class Duwtje {
    /** May this player be shoved now? Not during a cutscene, a narrator card or a talking screen, and never a spectator. */
    public static boolean mag(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && !Vast.veilig(p) && !Praat.bezig(p);
    }

    /**
     * A shove: knockback only, in this (horizontal) direction with this strength (0.5 = a nudge, 1.0 = clearly, 1.5 = a
     * big one), with a little hop. A mount is shoved with its rider. Does nothing when {@link #mag} says no.
     */
    public static void duw(ServerPlayer p, Vec3 richting, double kracht) {
        if (!mag(p)) {
            return;
        }
        Vec3 vlak = new Vec3(richting.x, 0, richting.z);
        if (vlak.lengthSqr() < 1e-6) {
            vlak = Vec3.directionFromRotation(0, p.getYRot()).scale(-1);
        }
        Vec3 v = vlak.normalize().scale(kracht).add(0, Math.min(0.45, 0.2 + kracht * 0.15), 0);
        Entity wie = p.getRootVehicle();
        wie.setDeltaMovement(v);
        wie.hurtMarked = true;
        if (wie != p) {
            p.hurtMarked = true;
        }
        p.fallDistance = 0;
    }

    /**
     * Poof: puts the player back on this spot (in this dimension), facing yaw. The fall distance is reset; items and health
     * are untouched. A mount comes along with its rider (within the same dimension). Allowed at any time (a failed course
     * also puts back a player who is talking); the caller checks {@link #mag} when it is a shove by a creature.
     */
    public static void terug(ServerPlayer p, ResourceKey<Level> dim, Vec3 plek, float yaw) {
        ServerLevel naar = p.level().getServer().getLevel(dim);
        if (naar == null) {
            return;
        }
        ServerLevel van = p.level();
        van.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.9, p.getZ(), 14, 0.3, 0.5, 0.3, 0.02);
        Entity wie = p.getRootVehicle();
        if (naar != van && wie != p) {
            p.stopRiding();
            wie = p;
        }
        wie.setDeltaMovement(Vec3.ZERO);
        wie.teleportTo(naar, plek.x, plek.y, plek.z, Set.of(), yaw, wie == p ? p.getXRot() : wie.getXRot(), false);
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        wie.fallDistance = 0;
        p.hurtMarked = true;
        naar.sendParticles(ParticleTypes.POOF, plek.x, plek.y + 0.9, plek.z, 14, 0.3, 0.5, 0.3, 0.02);
        naar.playSound(null, plek.x, plek.y, plek.z, SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 0.9f, 1.3f);
    }

    private Duwtje() {
    }
}
