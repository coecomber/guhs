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
    /**
     * W-SPEL: waiting for the world before the shot starts (-1 = not waiting; else ticks waited). The camera stands at the first
     * keyframe behind a black screen until the chunks where it looks are received and built, at most {@link #MAX_LADEN} ticks.
     */
    private static int laden = -1;
    /** W-SPEL: after a far scene: black until the surroundings of the player are back (-1 = not waiting). */
    private static int naLaden = -1;
    /** W-SPEL: the black screen fades out over this many ticks once the world is there. */
    private static int uitfade;
    static final int MAX_LADEN = nl.juiced.guhs.feature.vadsrijk.kern.scene.SceneLaden.MARGE, FADE = 8;

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
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            stop();
            naLaden = -1;   // W-SPEL
            uitfade = 0;
        });
    }

    public static boolean actief() {
        return scene != null;
    }

    /** (i-lore) The scene playing now, or null: lets the sky add a moment to a shot (WereldLucht, WERELDVERHAAL §10). */
    @Nullable
    public static RijkScene huidig() {
        return scene;
    }

    /** (i-lore) The tick of the scene playing now. */
    public static int tijd() {
        return t;
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
        if (mc.screen instanceof nl.juiced.guhs.feature.knuffeldal.client.PraatScherm praat) {
            praat.onClose();   // (q-dialoog: so the server hears the conversation ended, instead of waiting for it)
        } else if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)) {
            mc.setScreen(null);
        }
        camera = new Camera(mc.level);
        zet(0, true);
        oudeCamera = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(camera);
        naLaden = -1;
        if (wereldKlaar(camera.getEyePosition(), op(0)[1])) {
            laden = -1;
            begin(s);
        } else {
            laden = 0;   // W-SPEL: first the world (tick() starts the shot)
        }
    }

    /** The shot really starts (the title card is timed from here). */
    private static void begin(RijkScene s) {
        if (!s.titelKey().isEmpty()) {
            RijkHud.titelLater(Component.translatable(s.titelKey()), Component.translatable(s.ondertitelKey()), s.titelDuur(), s.kleur(), s.titelOp());
        }
    }

    /**
     * W-SPEL: are the chunks where this camera looks received and built? Points: the camera, the look target (at most 96 blocks
     * away) and two between, each in a loaded chunk with a compiled render section; plus the chunks right around the look point.
     * Points beyond what this client renders, or outside the build height, do not count.
     */
    static boolean wereldKlaar(Vec3 cam, Vec3 kijk) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.multiplayer.ClientLevel lv = mc.level;
        if (lv == null || mc.levelRenderer == null) {
            return true;
        }
        int rd = Math.max(2, mc.options.getEffectiveRenderDistance() - 1);
        int ccx = Mth.floor(cam.x) >> 4, ccz = Mth.floor(cam.z) >> 4;
        Vec3 d = kijk.subtract(cam);
        double len = d.length();
        Vec3 eind = len < 1e-3 ? cam : cam.add(d.scale(Math.min(len, 96) / len));
        Vec3[] punten = {cam, cam.lerp(eind, 0.33), cam.lerp(eind, 0.66), eind};
        for (Vec3 v : punten) {
            net.minecraft.core.BlockPos b = net.minecraft.core.BlockPos.containing(v);
            int cx = b.getX() >> 4, cz = b.getZ() >> 4;
            if (Math.max(Math.abs(cx - ccx), Math.abs(cz - ccz)) > rd) {
                continue;
            }
            if (!lv.getChunkSource().hasChunk(cx, cz)) {
                return false;
            }
            if (!lv.isOutsideBuildHeight(b.getY()) && !mc.levelRenderer.isSectionCompiledAndVisible(b)) {
                return false;
            }
        }
        int ex = Mth.floor(eind.x) >> 4, ez = Mth.floor(eind.z) >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.max(Math.abs(ex + dx - ccx), Math.abs(ez + dz - ccz)) <= rd && !lv.getChunkSource().hasChunk(ex + dx, ez + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** W-SPEL: 0..1 how black the screen is (waiting for the world before/after a shot, then a short fade). */
    public static float zwart(float pt) {
        if (laden >= 0 || naLaden >= 0) {
            return 1f;
        }
        return uitfade > 0 ? Mth.clamp((uitfade - pt) / FADE, 0, 1) : 0f;
    }

    /** W-SPEL: show the "laden..." text (only when the black screen stays a moment). */
    public static boolean ladenTekst() {
        return Math.max(laden, naLaden) >= 10;
    }

    static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (scene == null) {
            return;
        }
        scene = null;
        laden = -1;
        if (camera != null) {
            camera = null;
            mc.options.setCameraType(oudeCamera);
            if (mc.player != null) {
                mc.player.setYRot(spelerYaw);
                mc.player.setXRot(spelerPitch);
                mc.setCameraEntity(mc.player);
                // W-SPEL: after a far scene the chunks around the player come back from the server: black until they are built
                Vec3 oog = mc.player.getEyePosition();
                naLaden = wereldKlaar(oog, oog.add(mc.player.getLookAngle().scale(16))) ? -1 : 0;
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
        if (uitfade > 0) {
            uitfade--;
        }
        if (naLaden >= 0) {   // W-SPEL: back to the player: wait for the surroundings (max MAX_LADEN)
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                naLaden = -1;
            } else {
                Vec3 oog = mc.player.getEyePosition();
                if (++naLaden >= MAX_LADEN || wereldKlaar(oog, oog.add(mc.player.getLookAngle().scale(16)))) {
                    naLaden = -1;
                    uitfade = FADE;
                }
            }
        }
        RijkScene s = scene;
        if (s == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || camera == null || camera.level() != mc.level) {
            stop();
            return;
        }
        if (laden >= 0) {   // W-SPEL: the world first; the camera waits on its first keyframe
            zet(0, true);
            if (++laden >= MAX_LADEN || wereldKlaar(camera.getEyePosition(), op(0)[1])) {
                laden = -1;
                uitfade = FADE;
                begin(s);
            }
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
