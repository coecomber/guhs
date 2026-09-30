package nl.juiced.guhs.feature.knuffelbad.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.juiced.guhs.feature.knuffelbad.GlijPad;
import nl.juiced.guhs.feature.knuffelbad.ZwembandjeEntity;

/**
 * The camera on a water slide: it looks along the slide (worked out every frame from the path at the ride's exact time,
 * so it is perfectly smooth), pitches with the drops and rolls with the slide: banked in the bends, tilted up the wall of
 * a funnel, round and round in the tube. You can still look around with the mouse (it drifts back to straight ahead when
 * you let go). Faster = a wider view; a PLONS shakes it.
 */
public final class GlijCamera {
    /** How far you may look away from the slide's direction. */
    public static final float MAX_YAW = 110f, MAX_PITCH = 60f;

    private static float kijkYaw, kijkPitch;
    private static float gezetYaw = Float.NaN, gezetPitch;
    private static float roll;
    private static int stil;
    private static float schok;
    private static float fovExtra, fovExtraO;

    private GlijCamera() {
    }

    /** Every client tick: follow the mouse into the look offsets, let them drift back, the roll's smoothing, the shake. */
    static void tick() {
        ZwembandjeEntity ring = KnuffelbadClient.eigenRing();
        LocalPlayer p = Minecraft.getInstance().player;
        if (ring == null || p == null) {
            gezetYaw = Float.NaN;
            kijkYaw = kijkPitch = 0;
            roll = 0;
            schok = 0;
            fovExtra = fovExtraO = 0;
            return;
        }
        GlijPad.Stand st = ring.stand(ring.tau, ring.lat);
        float trackYaw = yaw(st.tangent()), trackPitch = pitch(st.tangent());
        if (!Float.isNaN(gezetYaw)) {
            float dy = Mth.wrapDegrees(p.getYRot() - gezetYaw), dp = p.getXRot() - gezetPitch;
            kijkYaw = Mth.clamp(kijkYaw + dy, -MAX_YAW, MAX_YAW);
            kijkPitch = Mth.clamp(kijkPitch + dp, -MAX_PITCH, MAX_PITCH);
            if (Math.abs(dy) + Math.abs(dp) < 0.05f) {
                if (++stil > 20) {                 // let go of the mouse: back to looking down the slide
                    kijkYaw *= 0.9f;
                    kijkPitch *= 0.9f;
                }
            } else {
                stil = 0;
            }
        }
        gezetYaw = trackYaw + kijkYaw;
        gezetPitch = Mth.clamp(trackPitch + kijkPitch, -89f, 89f);
        p.setYRot(gezetYaw);
        p.setXRot(gezetPitch);
        p.yRotO = gezetYaw;
        p.xRotO = gezetPitch;
        p.setYHeadRot(gezetYaw);
        schok *= 0.82f;
        fovExtraO = fovExtra;
        double v = ring.baan().pad().snelheid(ring.tau);
        int fx = st.fx();
        float doel = (float) Mth.clamp((v - 0.42) * 0.35, 0, 0.22) + ((fx & GlijPad.FX_VAL) != 0 ? 0.06f : 0f);
        fovExtra = Mth.lerp(0.15f, fovExtra, doel);
    }

    /** The camera's angles this frame. */
    static void hoeken(ViewportEvent.ComputeCameraAngles event) {
        ZwembandjeEntity ring = KnuffelbadClient.eigenRing();
        LocalPlayer p = Minecraft.getInstance().player;
        if (ring == null || p == null || Float.isNaN(gezetYaw)) {
            return;
        }
        float pt = (float) event.getPartialTick();
        GlijPad.Stand st = ring.stand(ring.tekenTau(pt), ring.tekenLat(pt));
        Vec3 t = st.tangent();
        // the mouse between two ticks moves the view at once (the player's own rotation, since we last set it)
        float muisYaw = Mth.wrapDegrees(p.getViewYRot(pt) - gezetYaw), muisPitch = p.getViewXRot(pt) - gezetPitch;
        float yaw = yaw(t) + Mth.clamp(kijkYaw + muisYaw, -MAX_YAW, MAX_YAW);
        float pitch = Mth.clamp(pitch(t) + Mth.clamp(kijkPitch + muisPitch, -MAX_PITCH, MAX_PITCH), -89f, 89f);
        // roll: how the slide's surface under you is turned about the direction you go
        Vec3 upRef = new Vec3(0, 1, 0).subtract(t.scale(t.y));
        upRef = upRef.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : upRef.normalize();
        Vec3 rightRef = t.cross(upRef).normalize();
        Vec3 n = st.normal();
        float doel = (float) Math.toDegrees(Math.atan2(n.dot(rightRef), n.dot(upRef)));
        // looking sideways, the roll turns into a tilt forward/back: only the part along where you look
        float kijk = (float) Math.cos(Math.toRadians(Mth.clamp(kijkYaw + muisYaw, -90, 90)));
        roll = Mth.lerp(0.12f, roll, doel * kijk);           // (smooth where the slide changes shape: into a funnel, a tube)
        float r = roll;
        float shake = schok * schok;
        event.setYaw(yaw + (float) (Math.sin(p.tickCount * 1.7 + pt) * shake * 2.0));
        event.setPitch(pitch + (float) (Math.cos(p.tickCount * 2.3 + pt) * shake * 1.6));
        event.setRoll(r + (float) (Math.sin(p.tickCount * 1.1) * shake));
    }

    static void fov(ViewportEvent.ComputeFov event) {
        if (KnuffelbadClient.eigenRing() == null) {
            return;
        }
        float f = Mth.lerp((float) event.getPartialTick(), fovExtraO, fovExtra);
        event.setFOV(event.getFOV() * (1.0f + f));
    }

    /** A PLONS (or a bump): the camera shakes for a moment. */
    static void schud(float sterkte) {
        schok = Math.max(schok, sterkte);
    }

    static float yaw(Vec3 t) {
        return (float) Math.toDegrees(Math.atan2(-t.x, t.z));
    }

    static float pitch(Vec3 t) {
        return (float) -Math.toDegrees(Math.asin(Mth.clamp(t.y, -1, 1)));
    }
}
