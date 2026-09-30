package nl.juiced.guhs.feature.elftocht.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.elftocht.ElftochtFeature;
import nl.juiced.guhs.feature.elftocht.ElftochtSchaatsen;

/**
 * How skating looks and sounds (every skater nearby, not just you): a speed skater's lean forward and a slow sway from
 * one blade to the other (the whole body, in the rhythm of the strides), the hiss of the blades (a loop that follows
 * the speed), a scrape at every stride and a little ice dust behind the skates.
 */
public final class SchaatsEffecten {
    /** Lean, roll and body yaw of a skating player (render state data, set at extract time). */
    static final ContextKey<float[]> KANTEL = new ContextKey<>(Guhs.id("elftocht_kantel"));
    /** Set on a render state whose pose we tilted (popped again after its render). */
    private static final ContextKey<Boolean> GEKANTELD = new ContextKey<>(Guhs.id("elftocht_gekanteld"));
    private static final Map<UUID, Glij> GELUIDEN = new HashMap<>();
    private static final Map<UUID, Double> VORIG_SWAY = new HashMap<>();

    /** Horizontal speed (blocks per tick) from the last tick's movement (also right for other players). */
    static double snelheid(Player p) {
        return Math.hypot(p.getX() - p.xo, p.getZ() - p.zo);
    }

    /** Skating visibly: skates in hand, on the ice, gliding. */
    static boolean schaatstZichtbaar(Player p) {
        return ElftochtSchaatsen.houdtSchaatsen(p) && p.onGround() && p.getBlockStateOn().is(ElftochtFeature.SCHAATSIJS)
                && !p.isPassenger() && !p.isSpectator();
    }

    /** The sway: lean forward (up to 11 degrees) and roll from blade to blade (up to 7). */
    static float[] houding(Player p, float partial) {
        double v = snelheid(p);
        float amp = (float) Mth.clamp((v - 0.05) / 0.3, 0, 1);
        float t = p.tickCount + partial;
        return new float[]{11f * amp, 7f * amp * Mth.sin(t * 0.28f)};
    }

    /** Extract time (render state modifier): the lean and sway of a skating player, with its body yaw. */
    static void extract(Player p, AvatarRenderState state) {
        if (!schaatstZichtbaar(p) || snelheid(p) < 0.05) {
            return;
        }
        float partial = state.partialTick;
        float[] h = houding(p, partial);
        state.setRenderData(KANTEL, new float[]{h[0], h[1], Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot)});
    }

    @SubscribeEvent
    public static void onRenderPre(RenderLivingEvent.Pre<?, ?, ?> event) {
        float[] h = event.getRenderState().getRenderData(KANTEL);
        if (h == null) {
            return;
        }
        float yaw = h[2];
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.mulPose(Axis.XP.rotationDegrees(h[0]));
        ps.mulPose(Axis.ZP.rotationDegrees(h[1]));
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        event.getRenderState().setRenderData(GEKANTELD, Boolean.TRUE);
    }

    @SubscribeEvent
    public static void onRenderPost(RenderLivingEvent.Post<?, ?, ?> event) {
        if (event.getRenderState().getRenderData(GEKANTELD) != null) {
            event.getRenderState().setRenderData(GEKANTELD, null);
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }
        GELUIDEN.values().removeIf(Glij::isStopped);
        for (Player p : level.players()) {
            if (mc.player != null && p.distanceToSqr(mc.player) > 40 * 40) {
                continue;
            }
            boolean schaatst = schaatstZichtbaar(p);
            double v = snelheid(p);
            if (schaatst && v > 0.06 && !GELUIDEN.containsKey(p.getUUID())) {
                Glij glij = new Glij(p);
                GELUIDEN.put(p.getUUID(), glij);
                mc.getSoundManager().play(glij);
            }
            if (!schaatst) {
                VORIG_SWAY.remove(p.getUUID());
                continue;
            }
            // a scrape at every stride (when the sway turns over)
            double sway = Math.sin((p.tickCount) * 0.28);
            Double vorig = VORIG_SWAY.put(p.getUUID(), sway);
            if (vorig != null && Math.signum(vorig) != Math.signum(sway) && v > 0.15) {
                level.playLocalSound(p.getX(), p.getY(), p.getZ(), ElftochtFeature.KRAS.get(), SoundSource.PLAYERS,
                        0.25f + (float) v * 0.4f, 0.9f + level.getRandom().nextFloat() * 0.25f, false);
            }
            // frosty breath: a little white puff in front of the face now and then (not in your own face in first person)
            boolean eigenOog = p == mc.player && mc.options.getCameraType().isFirstPerson();
            if (!eigenOog && (p.tickCount + p.getId()) % 32 == 0) {
                var kijk = p.getLookAngle();
                for (int i = 0; i < 3; i++) {
                    level.addParticle(ParticleTypes.WHITE_SMOKE, p.getX() + kijk.x * 0.45, p.getEyeY() - 0.15 + kijk.y * 0.3, p.getZ() + kijk.z * 0.45,
                            kijk.x * 0.03 + (p.getX() - p.xo) * 0.8, 0.012, kijk.z * 0.03 + (p.getZ() - p.zo) * 0.8);
                }
            }
            // ice dust behind the blades
            if (v > 0.12 && p.tickCount % 2 == 0) {
                double side = sway > 0 ? 0.2 : -0.2;
                double yaw = Math.toRadians(p.getYRot());
                double rx = Math.cos(yaw) * side, rz = Math.sin(yaw) * side;
                level.addParticle(ParticleTypes.SNOWFLAKE, p.getX() + rx, p.getY() + 0.05, p.getZ() + rz,
                        -(p.getX() - p.xo) * 0.3, 0.02, -(p.getZ() - p.zo) * 0.3);
            }
        }
    }

    /** The hiss of the blades under one skater: louder and higher the faster, gone when they stop skating. */
    static final class Glij extends AbstractTickableSoundInstance {
        private final Player speler;
        private int stil;

        Glij(Player speler) {
            super(ElftochtFeature.GLIJ.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.speler = speler;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.x = speler.getX();
            this.y = speler.getY();
            this.z = speler.getZ();
        }

        @Override
        public void tick() {
            if (speler.isRemoved()) {
                stop();
                return;
            }
            double v = snelheid(speler);
            boolean schaatst = schaatstZichtbaar(speler);
            stil = schaatst && v > 0.04 ? 0 : stil + 1;
            if (stil > 20) {
                stop();
                return;
            }
            float doel = schaatst ? (float) Mth.clamp(v * 2.4, 0, 1) * 0.55f : 0f;
            volume = Mth.lerp(0.3f, volume, doel);
            pitch = 0.75f + (float) Mth.clamp(v, 0, 0.6);
            x = speler.getX();
            y = speler.getY();
            z = speler.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }

    private SchaatsEffecten() {
    }
}
