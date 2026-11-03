package nl.juiced.guhs.feature.erfenis.client;

import javax.annotation.Nullable;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.juiced.guhs.feature.erfenis.Plot;
import org.lwjgl.glfw.GLFW;

/**
 * De Erfenis: the overview camera. While the overview is open the game looks through an invisible camera entity of
 * our own (like the cutscene camera of the verhaal engine) that hangs tilted above the plot: {@value #PITCH} degrees down,
 * a fixed field of view of {@value #FOV}, turned in quarter steps (Q/E). The player's body stays where it stood.
 * <p>
 * Every frame ({@link #frame}) the camera eases towards its target (pan with WASD/arrows or by dragging, zoom with the
 * wheel), clamped above the plot. The same numbers that place the camera are used for the mouse ray ({@link #straal})
 * and for projecting world points onto the screen ({@link #projecteer}), so picking matches what is drawn.
 */
public final class Overzicht {
    public static final float PITCH = 56f, FOV = 45f;
    static final double MIN_AFSTAND = 16, MAX_AFSTAND = 90;

    private static boolean actief;
    @Nullable
    private static Kijker camera;
    private static BlockPos oorsprong = BlockPos.ZERO;
    /** Target and current: the ground point looked at, the distance to it, the yaw. */
    private static double doelX, doelZ, x, z, doelAfstand = 56, afstand = 56;
    private static float doelYaw = 180f, yaw = 180f;
    private static long laatste;
    private static boolean oudeHud;
    private static CameraType oudeType = CameraType.FIRST_PERSON;

    private Overzicht() {
    }

    public static boolean actief() {
        return actief;
    }

    /** Takes over the camera above the plot with this min corner (keeps the view when it was already open on that plot). */
    static void start(BlockPos plot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        boolean zelfde = actief && plot.equals(oorsprong);
        oorsprong = plot;
        if (!zelfde) {
            doelX = x = plot.getX() + Plot.MAAT / 2.0;
            doelZ = z = plot.getZ() + Plot.MAAT / 2.0;
            doelAfstand = afstand = 40;
            doelYaw = yaw = 180f;
        }
        if (!actief) {
            camera = new Kijker(mc.level, mc.player.getEyeHeight());
            oudeHud = mc.options.hideGui;
            oudeType = mc.options.getCameraType();
            mc.options.hideGui = true;
            MinimapStil.uit();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            actief = true;
            laatste = System.nanoTime();
            zetCamera();
            camera.setOldPosAndRot();
            mc.setCameraEntity(camera);
        }
    }

    static void stop() {
        if (!actief) {
            return;
        }
        actief = false;
        Minecraft mc = Minecraft.getInstance();
        mc.options.hideGui = oudeHud;
        MinimapStil.terug();
        mc.options.setCameraType(oudeType);
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        camera = null;
    }

    public static BlockPos oorsprong() {
        return oorsprong;
    }

    // =====================================================================================================================
    // moving
    // =====================================================================================================================

    /** Every frame, before the camera is set up: keys, easing, clamping, the camera entity on its spot. */
    static void frame(boolean toetsen) {
        Minecraft mc = Minecraft.getInstance();
        if (!actief || camera == null) {
            return;
        }
        if (mc.level == null || mc.player == null || camera.level() != mc.level) {
            OverzichtScherm.sluitAlles();
            return;
        }
        long nu = System.nanoTime();
        double dt = Math.min(0.1, (nu - laatste) / 1e9);
        laatste = nu;
        if (toetsen) {
            var w = mc.getWindow();
            double vooruit = 0, opzij = 0;
            if (InputConstants.isKeyDown(w, GLFW.GLFW_KEY_W) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_UP)) {
                vooruit += 1;
            }
            if (InputConstants.isKeyDown(w, GLFW.GLFW_KEY_S) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_DOWN)) {
                vooruit -= 1;
            }
            if (InputConstants.isKeyDown(w, GLFW.GLFW_KEY_D) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT)) {
                opzij += 1;
            }
            if (InputConstants.isKeyDown(w, GLFW.GLFW_KEY_A) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT)) {
                opzij -= 1;
            }
            if (vooruit != 0 || opzij != 0) {
                double snelheid = doelAfstand * 0.9 * dt;
                double r = Math.toRadians(doelYaw);
                double fx = -Math.sin(r), fz = Math.cos(r);   // forward on the ground
                double rx = -fz, rz = fx;                       // right
                doelX += (fx * vooruit + rx * opzij) * snelheid;
                doelZ += (fz * vooruit + rz * opzij) * snelheid;
            }
        }
        klem();
        double k = 1 - Math.exp(-dt * 12);
        x += (doelX - x) * k;
        z += (doelZ - z) * k;
        afstand += (doelAfstand - afstand) * k;
        yaw += Mth.wrapDegrees(doelYaw - yaw) * (float) k;
        zetCamera();
        camera.setOldPosAndRot();
    }

    private static void klem() {
        double lo = -6, hi = Plot.MAAT + 6;
        doelX = Mth.clamp(doelX, oorsprong.getX() + lo, oorsprong.getX() + hi);
        doelZ = Mth.clamp(doelZ, oorsprong.getZ() + lo, oorsprong.getZ() + hi);
        doelAfstand = Mth.clamp(doelAfstand, MIN_AFSTAND, MAX_AFSTAND);
    }

    private static void zetCamera() {
        Vec3 p = positie();
        if (camera != null) {
            camera.setPos(p.x, p.y - camera.getEyeHeight(), p.z);
            camera.setYRot(yaw);
            camera.setXRot(PITCH);
        }
    }

    /** Looks at this world point from this distance (dev / AutoCheck). */
    static void richt(double wx, double wz, double dist) {
        doelX = wx;
        doelZ = wz;
        if (dist > 0) {
            doelAfstand = dist;
        }
        klem();
    }

    static void zoom(double stappen) {
        doelAfstand = Mth.clamp(doelAfstand * Math.pow(0.88, stappen), MIN_AFSTAND, MAX_AFSTAND);
    }

    static void draai(int kwart) {
        doelYaw = Math.round((doelYaw + 90f * kwart) / 90f) * 90f;
    }

    /** Moves the view by this much ground (dragging: right away, no easing, so the ground sticks to the mouse). */
    static void schuif(double dx, double dz) {
        doelX += dx;
        doelZ += dz;
        klem();
        x = doelX;
        z = doelZ;
        zetCamera();
        if (camera != null) {
            camera.setOldPosAndRot();
        }
    }

    // =====================================================================================================================
    // the camera's numbers, the mouse ray, projecting
    // =====================================================================================================================

    static double grondY() {
        return Plot.Y + 1;
    }

    private static Quaternionf draaiing() {
        return new Quaternionf().rotationYXZ((float) Math.PI - yaw * Mth.DEG_TO_RAD, -PITCH * Mth.DEG_TO_RAD, 0f);
    }

    private static Vec3 v(Vector3f f) {
        return new Vec3(f.x, f.y, f.z);
    }

    static Vec3 vooruit() {
        return v(draaiing().transform(new Vector3f(0, 0, -1)));
    }

    static Vec3 omhoog() {
        return v(draaiing().transform(new Vector3f(0, 1, 0)));
    }

    static Vec3 links() {
        return v(draaiing().transform(new Vector3f(-1, 0, 0)));
    }

    /** The camera's eye: {@code afstand} back from the looked-at ground point. */
    static Vec3 positie() {
        return new Vec3(x, grondY(), z).subtract(vooruit().scale(afstand));
    }

    private static double aspect() {
        var w = Minecraft.getInstance().getWindow();
        return w.getHeight() == 0 ? 1 : (double) w.getWidth() / w.getHeight();
    }

    /** The ray (origin, unit direction) under GUI point (mx, my) of a screen of gw x gh. */
    static Vec3[] straal(double mx, double my, double gw, double gh) {
        double nx = 2 * mx / gw - 1, ny = 1 - 2 * my / gh;
        double t = Math.tan(Math.toRadians(FOV) / 2);
        Vec3 d = vooruit().add(links().scale(-nx * t * aspect())).add(omhoog().scale(ny * t));
        return new Vec3[] {positie(), d.normalize()};
    }

    /** Where that ray hits the plot's ground (or null). */
    @Nullable
    static Vec3 grond(double mx, double my, double gw, double gh) {
        Vec3[] s = straal(mx, my, gw, gh);
        if (s[1].y > -1e-4) {
            return null;
        }
        double t = (grondY() - s[0].y) / s[1].y;
        return t <= 0 ? null : s[0].add(s[1].scale(t));
    }

    /** A world point on the screen (GUI coordinates), or null when it is behind the camera. */
    @Nullable
    static double[] projecteer(Vec3 p, double gw, double gh) {
        Vec3 rel = p.subtract(positie());
        double diep = rel.dot(vooruit());
        if (diep < 0.1) {
            return null;
        }
        double t = Math.tan(Math.toRadians(FOV) / 2);
        double nx = -rel.dot(links()) / (diep * t * aspect()), ny = rel.dot(omhoog()) / (diep * t);
        return new double[] {(nx + 1) / 2 * gw, (1 - ny) / 2 * gh};
    }

    // =====================================================================================================================
    // the hooks into the camera set-up
    // =====================================================================================================================

    static void hoeken(ViewportEvent.ComputeCameraAngles event) {
        if (actief && Minecraft.getInstance().getCameraEntity() == camera) {
            event.setYaw(yaw);
            event.setPitch(PITCH);
            event.setRoll(0);
        }
    }

    static void fov(ViewportEvent.ComputeFov event) {
        if (actief && event.usedConfiguredFov() && Minecraft.getInstance().getCameraEntity() == camera) {
            event.setFOV(FOV);
        }
    }

    /** The camera: an entity nobody sees, with the player's eye height (so the view doesn't glide when it takes over). */
    private static final class Kijker extends Entity {
        private final EntityDimensions maat;

        Kijker(Level level, float oogHoogte) {
            super(EntityType.MARKER, level);
            this.maat = EntityDimensions.fixed(0.1f, 0.1f).withEyeHeight(oogHoogte);
            this.noPhysics = true;
            refreshDimensions();
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return maat == null ? super.getDimensions(pose) : maat;
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
            return false;
        }

        @Override
        protected void readAdditionalSaveData(ValueInput input) {
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {
        }
    }
}
