package nl.juiced.guhs.feature.guhrio.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.juiced.guhs.feature.guhrio.Baan;

/**
 * The side view of a level: the camera stands beside the lane ({@link Baan#afstand} blocks away, on the lane's camera
 * side), looks straight at it and moves along with you. It is set every frame by mixin.client.GuhrioCameraMixin (after
 * the game worked out its own third-person camera), so nothing between you and the camera can push it closer: the level
 * builder keeps that side open.
 * <ul>
 *     <li>Along the lane it follows you exactly (you never slide on the screen), a little ahead of where you face; near the
 *     two ends of the lane it stops, so you don't look far past the level.</li>
 *     <li>In height it stays calm: it keeps to the ground you last stood on and only comes along when you get more than a
 *     few blocks above it or drop below it (a jump does not shake the picture).</li>
 *     <li>The view always shows {@link Baan#hoogte} blocks above and below its middle, whatever the distance: the field of
 *     view is worked out from the two ({@link #fov}), so a level can put the camera far (flat, like a drawing) or near
 *     (when there is little room) and look the same size.</li>
 *     <li>Around a corner of the lane it swings with {@link Baan#cameraYaw}.</li>
 * </ul>
 */
public final class BaanCamera {
    /** The camera looks down at the lane by this much (degrees): just enough to see the tops of the blocks. */
    public static final float KANTELING = 4f;
    /** How far ahead of you (blocks along the lane) the camera looks, the way you face. */
    public static final double VOORUIT = 1.5;
    /** The ground is this far under the middle of the picture (as a part of {@link Baan#hoogte}). */
    public static final double GROND_ONDER = 0.5;
    /** You may rise this far above / sink this far under the camera's ground before it comes along. */
    public static final double OMHOOG = 3.2, OMLAAG = 0.4;
    /** Past the two ends of the lane the picture shows at most this many blocks. */
    public static final double VOORBIJ = 3.0;

    private static double grond, grondO, vooruit, vooruitO, vast;
    private static boolean klaar;

    private BaanCamera() {
    }

    /** Where the camera stands and looks this frame. */
    public record Stand(double x, double y, double z, float yaw, float pitch) {
    }

    /** A new level, a new lane or a jump back to your flag: the camera is there at once. */
    static void begin(@Nullable LocalPlayer p) {
        if (p == null) {
            return;
        }
        grond = grondO = vast = p.getY();
        vooruit = vooruitO = 0;
        klaar = true;
    }

    /** Every tick: the calm height and the look ahead. */
    static void tick(LocalPlayer p) {
        Baan baan = GuhrioClient.baan();
        if (baan == null) {
            return;
        }
        if (!klaar) {
            begin(p);
        }
        grondO = grond;
        vooruitO = vooruit;
        if (p.onGround() || BaanBesturing.inPijp()) {
            vast = p.getY();
        }
        double doel = Mth.clamp(vast, p.getY() - OMHOOG, p.getY() + OMLAAG);
        grond += (doel - grond) * 0.2;
        vooruit += (BaanBesturing.kijk() * VOORUIT - vooruit) * 0.06;
    }

    /** The camera for this frame, or null when you are not in a level (called by the camera mixin). */
    @Nullable
    public static Stand stand(float partial) {
        Baan baan = GuhrioClient.baan();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (baan == null || p == null || !klaar) {
            return null;
        }
        double px = Mth.lerp(partial, p.xo, p.getX()), pz = Mth.lerp(partial, p.zo, p.getZ());
        double s = baan.plek(px, pz).s() + Mth.lerp(partial, vooruitO, vooruit);
        // not far past the two ends of the lane
        double breed = baan.hoogte * mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        double min = breed - VOORBIJ, max = baan.lengte() - breed + VOORBIJ;
        s = min <= max ? Mth.clamp(s, min, max) : baan.lengte() / 2;
        double y = Mth.lerp(partial, grondO, grond) + baan.hoogte * GROND_ONDER;
        // not under the level's floor, not over its ceiling
        double laag = baan.onder + baan.hoogte - 1, hoog = baan.boven - baan.hoogte + 1;
        y = laag <= hoog ? Mth.clamp(y, laag, hoog) : y;
        Vec3 midden = baan.punt(s, y);
        float yaw = baan.cameraYaw(s);
        Vec3 kijk = Vec3.directionFromRotation(KANTELING, yaw);
        return new Stand(midden.x - kijk.x * baan.afstand, midden.y - kijk.y * baan.afstand, midden.z - kijk.z * baan.afstand, yaw, KANTELING);
    }

    /** The field of view that shows {@link Baan#hoogte} blocks above and below the middle at the lane. */
    static void fov(ViewportEvent.ComputeFov event) {
        Baan baan = GuhrioClient.baan();
        if (baan == null || !event.usedConfiguredFov()) {
            return;
        }
        event.setFOV((float) Math.toDegrees(2 * Math.atan(baan.hoogte / baan.afstand)));
    }

    static void einde() {
        klaar = false;
    }
}
