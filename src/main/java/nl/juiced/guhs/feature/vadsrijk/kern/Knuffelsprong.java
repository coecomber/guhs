package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * De Knuffelsprong (fix round 1): the universal leap of every class, unlocked from the start. Key C (rebindable, category
 * Vadsrijk; {@code KernPayloads.Skill} slot {@value #SLOT}), the 5th small HUD slot. A forward arc of ~20 blocks, ~5 high, 5 s
 * cooldown, no energy, never fall damage. The client simulates the flight (normal collision: no clipping through walls).
 * <p>Two impulses: a small hop first (so the player is off the ground: on the ground the first tick would eat ~45 % of the
 * forward speed through block friction), then 2 ticks later the real arc.
 */
public final class Knuffelsprong {
    public static final String ID = "knuffelsprong";
    public static final int SLOT = 4, COOLDOWN = 100;
    /** The arc (blocks/tick): forward speed and upward speed of the second impulse; see NOTITIES_FIX_GEVECHT.md for the maths. */
    public static final double VOORUIT = 1.95, OMHOOG = 0.85, HOP = 0.5;
    /** Protected from fall damage until this game time (pushed on while in the air). */
    private static final Map<UUID, Long> BESCHERMD = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> GESTART = new ConcurrentHashMap<>();

    private Knuffelsprong() {
    }

    public static boolean gebruik(ServerPlayer p) {
        if (!Vadsrijk.in(p) || p.isSpectator() || RijkScenes.bezig(p) || Statussen.heeft(p, Status.VERDOOFD) || p.isPassenger() || p.isFallFlying()
                || p.getAbilities().flying) {
            return false;
        }
        SpelerData d = Spelers.data(p);
        long nu = p.level().getGameTime();
        if (d.klaarOp.getOrDefault(ID, 0L) > nu) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.kern.skill.cooldown").withStyle(ChatFormatting.GRAY));
            return false;
        }
        float yaw = p.getYRot() * ((float) Math.PI / 180f);
        Vec3 vooruit = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        ServerLevel sl = (ServerLevel) p.level();
        beschermTot(p, nu + COOLDOWN);
        GESTART.put(p.getUUID(), nu);
        p.resetFallDistance();
        boolean opGrond = p.onGround();
        if (opGrond) {
            duw(p, new Vec3(vooruit.x * 0.3, HOP, vooruit.z * 0.3));
        }
        Runnable boog = () -> {
            if (p.isRemoved() || !Vadsrijk.in(p)) {
                return;
            }
            duw(p, new Vec3(vooruit.x * VOORUIT, OMHOOG, vooruit.z * VOORUIT));
            p.resetFallDistance();
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 0.5, p.getZ(), 14, 0.4, 0.3, 0.4, 0.02);
        };
        if (opGrond) {
            LATER.put(p.getUUID(), new Uitgesteld(nu + 2, boog));
        } else {
            boog.run();
        }
        sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(), 16, 0.5, 0.1, 0.5, 0.05);
        sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1, p.getZ(), 4, 0.4, 0.4, 0.4, 0);
        sl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 1.7f);
        sl.playSound(null, p.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1f, 0.8f);
        d.klaarOp.put(ID, nu + COOLDOWN);
        Tips.toon(p, "vadsrijk.kern.tip.knuffelsprong");
        Spelers.sync(p);
        return true;
    }

    private record Uitgesteld(long op, Runnable wat) {
    }

    private static final Map<UUID, Uitgesteld> LATER = new ConcurrentHashMap<>();

    private static void duw(ServerPlayer p, Vec3 v) {
        p.setDeltaMovement(v);
        p.connection.send(new ClientboundSetEntityMotionPacket(p));
    }

    /** No fall damage until {@code tot} (also for tests). */
    public static void beschermTot(ServerPlayer p, long tot) {
        BESCHERMD.put(p.getUUID(), tot);
    }

    public static boolean beschermd(ServerPlayer p) {
        Long t = BESCHERMD.get(p.getUUID());
        return t != null && p.level().getGameTime() <= t;
    }

    /** Fall damage in the dimension (4 % of max Vads per vanilla point), except while protected by a Knuffelsprong. */
    public static double valSchade(ServerPlayer p, float vanilla) {
        if (beschermd(p)) {
            return 0;
        }
        return Schade.doe(new Schade.Klap(null, p, vanilla * Spelers.maxVads(p) * 0.04, Element.GEEN, Schade.Soort.WAAR, false, "val")).schade();
    }

    /** Every tick for players in the dimension (KernEvents): the second impulse, and the fall protection until landed. */
    static void tick(ServerPlayer p) {
        UUID id = p.getUUID();
        long nu = p.level().getGameTime();
        Uitgesteld u = LATER.get(id);
        if (u != null && nu >= u.op()) {
            LATER.remove(id);
            u.wat().run();
        }
        Long tot = BESCHERMD.get(id);
        if (tot == null) {
            return;
        }
        if (nu > tot) {
            BESCHERMD.remove(id);
            GESTART.remove(id);
            return;
        }
        p.resetFallDistance();
        long start = GESTART.getOrDefault(id, nu);
        if (nu - start > 8 && p.onGround() && LATER.get(id) == null) {
            BESCHERMD.put(id, Math.min(tot, nu + 10));   // landed: 0.5 s grace, then normal again
            GESTART.put(id, Long.MAX_VALUE / 2);
        }
    }

    static void vergeet(UUID id) {
        BESCHERMD.remove(id);
        GESTART.remove(id);
        LATER.remove(id);
    }
}
