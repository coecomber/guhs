package nl.juiced.guhs.feature.ringh6;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-h6): the coals the Frituurberg throws ({@link ValkoolEntity}). While a player climbs (steps 1..4 of the
 * chapter, on the flank of the mountain: not at a Rustvuurtje, not on the rope, not on Sam-guh's back) a coal comes down
 * every few seconds, aimed at where they are walking to, with a puff of sparks on the ground a second before it lands:
 * see it, step aside. Higher up they come faster. A hit only shoves (never damage, never fire).
 */
public final class Kolen {
    /** Ticks between two coals for one climber: at the foot and on the last stretch (plus up to half of it at random). */
    public static final int RUST_LAAG = 80, RUST_HOOG = 50;
    /** A coal starts this many blocks above its target (less under an overhang), and a Rustvuurtje keeps them this far away. */
    public static final int VALHOOGTE = 14;
    public static final double VEILIG = 6.0;

    private static final Map<UUID, Long> VOLGENDE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> GERAAKT = new ConcurrentHashMap<>();

    /** (once a second, a climbing player) time for a coal? */
    static void seconde(ServerPlayer p, Berg.Kopie berg, int stap) {
        ServerLevel level = p.level();
        long nu = level.getGameTime();
        Long volgende = VOLGENDE.get(p.getUUID());
        if (volgende == null) {
            VOLGENDE.put(p.getUUID(), nu + RUST_LAAG);
            return;
        }
        if (nu < volgende || !opDeFlank(p, berg)) {
            return;
        }
        int rust = stap >= 3 ? RUST_HOOG : RUST_LAAG;
        VOLGENDE.put(p.getUUID(), nu + rust + p.getRandom().nextInt(rust / 2));
        // where they will be in a second, give or take
        Vec3 v = p.getKnownMovement();
        Vec3 doel = p.position().add(v.x * 18 + (p.getRandom().nextDouble() - 0.5) * 2.4, 0, v.z * 18 + (p.getRandom().nextDouble() - 0.5) * 2.4);
        laatVallen(level, doel, p);
        if (p.getRandom().nextInt(3) == 0) {
            laatVallen(level, doel.add((p.getRandom().nextDouble() - 0.5) * 7, 0, (p.getRandom().nextDouble() - 0.5) * 7), p);
        }
        if (RingH6Feature.LIJN.eenmalig(p, "kool_sam") && Sam.van(p) != null) {
            GuhQuests.say(p, Sam.van(p), "quest.guhs.ringh6.kool.sam");
        }
    }

    /** On the flank: on the mountain above the camp, not safe at a fire, walking by themselves. */
    static boolean opDeFlank(ServerPlayer p, Berg.Kopie berg) {
        if (p.isPassenger() || Gaven.klimt(p) || p.isSpectator() || p.getY() < berg.wereld("kamp").getY() + 1.5) {
            return false;
        }
        for (String vuur : new String[]{"kamp_vuur", "vuur_1", "vuur_2", "vuur_3"}) {
            if (berg.wereld(vuur).distToCenterSqr(p.position()) < VEILIG * VEILIG) {
                return false;
            }
        }
        return berg.wereld("rand").distToCenterSqr(p.position()) > 10 * 10;
    }

    /**
     * A coal over this spot: it starts as high above it as there is room (at most {@link #VALHOOGTE}), a puff of sparks
     * marks where it will land. {@code voor}: the climber it is meant for (only they get the warning), or null.
     */
    @Nullable
    public static ValkoolEntity laatVallen(ServerLevel level, Vec3 doel, @Nullable ServerPlayer voor) {
        BlockPos onder = BlockPos.containing(doel.x, doel.y + 0.5, doel.z);
        int hoog = 0;
        while (hoog < VALHOOGTE && !level.getBlockState(onder.above(hoog + 2)).blocksMotion()) {
            hoog++;
        }
        if (hoog < 4) {
            return null;                                      // (under a low overhang: no coal gets through)
        }
        ValkoolEntity kool = RingH6Feature.VALKOOL.get().create(level, EntitySpawnReason.TRIGGERED);
        if (kool == null) {
            return null;
        }
        kool.snapTo(doel.x, onder.getY() + hoog + 0.5, doel.z, 0f, 0f);
        kool.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.04, -0.05, (level.getRandom().nextDouble() - 0.5) * 0.04);
        level.addFreshEntity(kool);
        level.playSound(null, kool.getX(), kool.getY(), kool.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.5f, 0.6f);
        if (voor != null) {
            level.sendParticles(voor, ParticleTypes.SMALL_FLAME, true, false, doel.x, doel.y + 0.15, doel.z, 10, 0.45, 0.02, 0.45, 0.005);
            level.sendParticles(voor, ParticleTypes.LAVA, true, false, doel.x, doel.y + 0.2, doel.z, 1, 0.2, 0, 0.2, 0);
        }
        return kool;
    }

    /** A coal shoved this player (for the "dodged them all"-free bookkeeping of the chapter: just a number). */
    static void geraakt(ServerPlayer p) {
        GERAAKT.merge(p.getUUID(), 1, Integer::sum);
        Ring.behaald(p, "ring_h6_kool");
    }

    /** How often a coal shoved this player since they logged in. */
    public static int geraaktAantal(ServerPlayer p) {
        return GERAAKT.getOrDefault(p.getUUID(), 0);
    }

    /** (tests) the next coal for this player comes at the next look. */
    static void zetNu(ServerPlayer p) {
        VOLGENDE.put(p.getUUID(), p.level().getGameTime());
    }

    static void vergeet(UUID speler) {
        VOLGENDE.remove(speler);
        GERAAKT.remove(speler);
    }

    static void wisAlles() {
        VOLGENDE.clear();
        GERAAKT.clear();
    }

    private Kolen() {
    }
}
