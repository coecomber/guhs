package nl.juiced.guhs.feature.vadsrijk.kern.client;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;

/**
 * Plays a {@link RijkScene} on this client: a camera that glides between the keyframes (smoothstep), black bars, no input,
 * and the title card at its moment. Title cards on their own come in through {@link KernPayloads.Titel} and are drawn by
 * {@link RijkHud}. Non-click: it just runs and gives the view back at the end.
 */
public final class SceneSpeler {
    @Nullable
    private static RijkScene scene;
    private static int t;
    @Nullable
    private static Camera camera;
    private static CameraType oudeCamera = CameraType.FIRST_PERSON;
    private static float spelerYaw, spelerPitch;

    private SceneSpeler() {
    }

    static void init() {
        KernPayloads.sceneOntvanger = s -> Minecraft.getInstance().execute(() -> start(s.scene()));
        KernPayloads.titelOntvanger = ti -> Minecraft.getInstance().execute(() -> RijkHud.titel(ti.titel(), ti.onder(), ti.ticks(), ti.kleur()));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre e) -> voorTick());
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeCameraAngles e) -> hoeken(e));
        NeoForge.EVENT_BUS.addListener((RenderHandEvent e) -> {
            if (scene != null) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> stop());
    }

    public static boolean actief() {
        return scene != null;
    }

    /** 0..1 how far the black bars are in. */
    static float balk(float pt) {
        RijkScene s = scene;
        if (s == null) {
            return 0;
        }
        float tt = t + pt;
        return Mth.clamp(Math.min(tt / 10f, (s.duur() - tt) / 10f), 0, 1);
    }

    static void start(RijkScene s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        stop();
        scene = s;
        t = 0;
        spelerYaw = mc.player.getYRot();
        spelerPitch = mc.player.getXRot();
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)) {
            mc.setScreen(null);
        }
        camera = new Camera(mc.level);
        zet(0, true);
        oudeCamera = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(camera);
        if (!s.titelKey().isEmpty()) {
            RijkHud.titelLater(Component.translatable(s.titelKey()), Component.translatable(s.ondertitelKey()), s.titelDuur(), s.kleur(), s.titelOp());
        }
    }

    static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (scene == null) {
            return;
        }
        scene = null;
        if (camera != null) {
            camera = null;
            mc.options.setCameraType(oudeCamera);
            if (mc.player != null) {
                mc.player.setYRot(spelerYaw);
                mc.player.setXRot(spelerPitch);
                mc.setCameraEntity(mc.player);
            }
        }
    }

    private static void voorTick() {
        if (scene == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        for (KeyMapping k : mc.options.keyMappings) {
            if (k == mc.options.keyScreenshot || k == mc.options.keyFullscreen) {
                continue;
            }
            while (k.consumeClick()) {
                // (drained: no input during the scene)
            }
            k.setDown(false);
        }
    }

    private static void tick() {
        RijkScene s = scene;
        if (s == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || camera == null || camera.level() != mc.level) {
            stop();
            return;
        }
        t++;
        if (t >= s.duur()) {
            stop();
            return;
        }
        zet(t, false);
    }

    /** Position + angles at time tt. */
    private static Vec3[] op(double tt) {
        RijkScene s = scene;
        List<RijkScene.Punt> p = s.punten();
        if (p.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            return new Vec3[] {mc.player.getEyePosition(), mc.player.getEyePosition().add(mc.player.getLookAngle())};
        }
        RijkScene.Punt a = p.get(0), b = p.get(0);
        for (RijkScene.Punt q : p) {
            if (q.t() <= tt) {
                a = q;
                b = q;
            } else {
                b = q;
                break;
            }
        }
        double f = b.t() == a.t() ? 1 : Mth.clamp((tt - a.t()) / (b.t() - a.t()), 0, 1);
        f = f * f * (3 - 2 * f);
        Vec3 pos = a.pos().lerp(b.pos(), f);
        Vec3 ka = kijk(a), kb = kijk(b);
        return new Vec3[] {pos, ka.lerp(kb, f)};
    }

    private static Vec3 kijk(RijkScene.Punt p) {
        if (p.volgEntity() >= 0) {
            Minecraft mc = Minecraft.getInstance();
            Entity e = mc.level == null ? null : mc.level.getEntity(p.volgEntity());
            if (e != null) {
                return e.position().add(0, e.getBbHeight() * 0.6, 0);
            }
        }
        return p.kijk();
    }

    private static void zet(double tt, boolean sprong) {
        Camera c = camera;
        if (c == null) {
            return;
        }
        Vec3[] v = op(tt);
        c.setOldPosAndRot();
        c.setPos(v[0].x, v[0].y - c.getEyeHeight(), v[0].z);
        Vec3 d = v[1].subtract(v[0]);
        float yaw = (float) (Mth.atan2(d.z, d.x) * 180 / Math.PI) - 90f;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI);
        c.setYRot(yaw);
        c.setXRot(pitch);
        if (sprong) {
            c.setOldPosAndRot();
        }
    }

    private static void hoeken(ViewportEvent.ComputeCameraAngles e) {
        if (scene == null || camera == null) {
            return;
        }
        Vec3[] v = op(t + e.getPartialTick());
        Vec3 d = v[1].subtract(v[0]);
        e.setYaw((float) (Mth.atan2(d.z, d.x) * 180 / Math.PI) - 90f);
        e.setPitch((float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI));
    }

    /** The camera: an invisible client-only entity. */
    private static final class Camera extends Entity {
        private final EntityDimensions maat = EntityDimensions.fixed(0.1f, 0.1f).withEyeHeight(0.05f);

        Camera(Level level) {
            super(EntityType.MARKER, level);
            this.noPhysics = true;
            refreshDimensions();
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return maat == null ? super.getDimensions(pose) : maat;
        }

        @Override
        protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        }

        @Override
        public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float damage) {
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
