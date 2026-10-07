package nl.juiced.guhs.feature.verhaal.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.VerhaalPayloads;
import nl.juiced.guhs.storage.Nbt;

/**
 * bbq2 (verhaal engine): plays a {@link Cutscene} on the viewer's own client. The actors are entities that only exist in
 * this client's level (negative ids, so the server never talks about them); the player's stand-in is a second player
 * with the viewer's skin. The camera is an entity of its own that follows the scene's camera path: while it is the camera
 * the game sends no movement and takes no input from the real player, who is hidden (when the scene has a stand-in) and
 * pinned. The HUD is replaced by the black bars and the subtitles ({@link VerhaalHud}); every key except Esc (the pause
 * menu), the screenshot key and fullscreen does nothing. At the end the client tells the server
 * ({@code guhs:verhaal_klaar}), which then runs what comes after.
 */
public final class CutsceneSpeler {
    /** Ticks the bars take to slide in and out. */
    static final int BALK_TICKS = 12;

    @Nullable
    private static Cutscene scene;
    private static BlockPos anker = BlockPos.ZERO;
    private static Rotation draai = Rotation.NONE;
    @Nullable
    private static ClientLevel level;
    private static int t;
    private static final Map<String, Entity> ACTEURS = new HashMap<>();
    private static final Set<Integer> ACTEUR_IDS = new HashSet<>();
    private static final Map<String, Integer> SPRONG = new HashMap<>();
    @Nullable
    private static CameraEntiteit camera;
    private static CameraType oudeCamera = CameraType.FIRST_PERSON;
    private static boolean oudeHud;
    @Nullable
    private static Vec3 spelerPlek;
    private static float spelerYaw, spelerPitch;
    private static int volgendeId = -4_000_000;
    private static float schud;
    private static int schudTot;
    @Nullable
    private static Component ondertitel;
    private static int ondertitelTot;

    private CutsceneSpeler() {
    }

    public static boolean actief() {
        return scene != null;
    }

    /** The scene that plays (null: none) and how far it is (ticks). */
    @Nullable
    public static Cutscene scene() {
        return scene;
    }

    public static int tijd() {
        return t;
    }

    /** The subtitle to show now (null: none). */
    @Nullable
    static Component ondertitel() {
        return scene != null && t < ondertitelTot ? ondertitel : null;
    }

    /** How far the bars are in (0..1). */
    static float balk(float partial) {
        if (scene == null) {
            return 0;
        }
        float in = (t + partial) / BALK_TICKS, uit = (scene.duur() - t - partial) / BALK_TICKS;
        return Mth.clamp(Math.min(in, uit), 0, 1);
    }

    /** How black the screen is (0..1). */
    static float zwart(float partial) {
        return scene == null ? 0 : scene.zwartOp(t + partial);
    }

    // =====================================================================================================================
    // start / stop
    // =====================================================================================================================

    static void start(VerhaalPayloads.Speel p) {
        Minecraft mc = Minecraft.getInstance();
        Cutscene s = Cutscene.van(p.id());
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (s == null) {
            // (a scene this client doesn't know: say it is over, so the server goes on)
            ClientPacketDistributor.sendToServer(new VerhaalPayloads.Klaar(VerhaalPayloads.SCENE, p.id()));
            return;
        }
        stop(false);
        VertelScherm.sluit();
        scene = s;
        anker = p.anker();
        draai = p.rotatie();
        level = mc.level;
        t = 0;
        schud = 0;
        ondertitel = null;
        LocalPlayer speler = mc.player;
        oudeHud = mc.options.hideGui;
        spelerPlek = null;
        spelerYaw = speler.getYRot();
        spelerPitch = speler.getXRot();
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)) {
            mc.setScreen(null);
        }
        for (Cutscene.Acteur a : s.acteurs()) {
            Entity e = maak(a, speler);
            if (e == null) {
                continue;
            }
            Vec3 pos = Cutscene.wereld(anker, draai, a.start());
            float yaw = Cutscene.wereldYaw(draai, a.yaw());
            e.setId(volgendeId--);
            e.snapTo(pos.x, pos.y, pos.z, yaw, 0);
            e.setYHeadRot(yaw);
            e.setYBodyRot(yaw);
            e.setOldPosAndRot();
            if (e instanceof LivingEntity l) {
                l.yHeadRotO = yaw;
                l.yBodyRotO = yaw;
            }
            mc.level.addEntity(e);
            ACTEURS.put(a.naam(), e);
            ACTEUR_IDS.add(e.getId());
        }
        Vec3[] cam = s.cameraOp(0);
        if (cam != null) {
            camera = new CameraEntiteit(mc.level, speler.getEyeHeight());
            zetCamera(cam, true);
            oudeCamera = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setCameraEntity(camera);
        }
        VerhaalClient.zetBezig();
    }

    @Nullable
    private static Entity maak(Cutscene.Acteur a, LocalPlayer speler) {
        ClientLevel lvl = Minecraft.getInstance().level;
        if (a.type() == null) {
            return new Dubbelganger(lvl, speler);
        }
        EntityType<?> type = a.type().get();
        Entity e = type.create(lvl, EntitySpawnReason.LOAD);
        if (e != null && a.nbt() != null) {
            // (only what the scene says, read like a saved entity: a Kind, a Variant, clothes...; the rest stays default)
            CompoundTag tag = new CompoundTag();
            a.nbt().accept(tag);
            try {
                Nbt.load(e, tag);
            } catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger("guhs").warn("Cutscene actor {} could not read its data {}", a.naam(), tag, ex);
            }
        }
        return e;
    }

    /** Ends the scene on this client; meld = tell the server it was watched to the end. */
    static void stop(boolean meld) {
        Cutscene s = scene;
        if (s == null) {
            return;
        }
        scene = null;
        Minecraft mc = Minecraft.getInstance();
        if (level != null) {
            for (Entity e : ACTEURS.values()) {
                level.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
        ACTEURS.clear();
        ACTEUR_IDS.clear();
        SPRONG.clear();
        Cutscenes.wisAnimaties();
        mc.options.hideGui = oudeHud;
        if (camera != null) {
            camera = null;
            mc.options.setCameraType(oudeCamera);
            if (mc.player != null) {
                mc.player.setYRot(spelerYaw);
                mc.player.setXRot(spelerPitch);
                mc.player.yRotO = spelerYaw;
                mc.player.xRotO = spelerPitch;
                mc.setCameraEntity(mc.player);
            }
        }
        level = null;
        ondertitel = null;
        VerhaalClient.zetBezig();
        if (meld && mc.getConnection() != null) {
            ClientPacketDistributor.sendToServer(new VerhaalPayloads.Klaar(VerhaalPayloads.SCENE, s.id()));
        }
    }

    // =====================================================================================================================
    // every tick / every frame
    // =====================================================================================================================

    /** Before the game handles its keys this tick: nothing the player presses does anything. */
    static void voorTick() {
        if (scene == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        for (KeyMapping k : mc.options.keyMappings) {
            if (k == mc.options.keyScreenshot || k == mc.options.keyFullscreen) {
                continue;
            }
            while (k.consumeClick()) {
                // (drained)
            }
            k.setDown(false);
        }
    }

    static void tick() {
        Cutscene s = scene;
        if (s == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer speler = mc.player;
        if (speler == null || mc.level == null || mc.level != level || !speler.isAlive()) {
            stop(false);
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        // the HUD is off as with F1 (minimaps and other overlays of other mods go with it); our own bars and subtitles are a layer
        // that is drawn anyway
        mc.options.hideGui = true;
        // the real player stays where they are (the first ticks follow the server, which may set them on the ground)
        if (t < 10 || spelerPlek == null) {
            spelerPlek = speler.position();
        } else {
            speler.setPos(spelerPlek);
        }
        speler.setDeltaMovement(Vec3.ZERO);
        speler.fallDistance = 0;
        t++;
        for (Cutscene.Animatie a : s.animaties()) {
            if (a.t() == t) {
                animeer(a);
            }
        }
        for (Map.Entry<String, Entity> e : ACTEURS.entrySet()) {
            beweeg(s, e.getKey(), e.getValue());
        }
        for (Cutscene.Zeg z : s.zinnen()) {
            if (z.t() == t) {
                ondertitel = regel(s, z);
                ondertitelTot = t + z.ticks();
            }
        }
        for (Cutscene.Geluid g : s.geluiden()) {
            if (g.t() == t) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(g.geluid().get(), g.pitch(), g.volume()));
            }
        }
        RandomSource r = mc.level.getRandom();
        for (Cutscene.Deeltjes d : s.deeltjes()) {
            if (d.t() == t) {
                Vec3 pos = Cutscene.wereld(anker, draai, d.pos());
                for (int i = 0; i < d.aantal(); i++) {
                    mc.level.addAlwaysVisibleParticle(d.deeltje(), true, pos.x + r.nextGaussian() * d.spreiding(), pos.y + r.nextGaussian() * d.spreiding(),
                            pos.z + r.nextGaussian() * d.spreiding(), 0, 0.02, 0);
                }
            }
        }
        for (Cutscene.Schud sch : s.schudden()) {
            if (sch.t() == t) {
                schud = Math.max(schud, sch.kracht());
                schudTot = t + sch.ticks();
            }
        }
        if (t > schudTot) {
            schud *= 0.8f;
        }
        Vec3[] cam = s.cameraOp(t);
        if (cam != null && camera != null) {
            zetCamera(cam, t > 0 && knip(s, t));
        }
        if (t >= s.duur()) {
            stop(true);
        }
    }

    /** Does a new shot start exactly at this tick (the camera then jumps instead of gliding)? */
    private static boolean knip(Cutscene s, int tijd) {
        for (Cutscene.CameraPunt p : s.camera()) {
            if (p.knip() && p.t() == tijd) {
                return true;
            }
        }
        return false;
    }

    private static void zetCamera(Vec3[] cam, boolean sprong) {
        CameraEntiteit c = camera;
        if (c == null) {
            return;
        }
        Vec3 pos = Cutscene.wereld(anker, draai, cam[0]), kijk = Cutscene.wereld(anker, draai, cam[1]);
        Cutscene s = scene;
        if (s != null && s.cameraOntwijkt()) {
            pos = Cutscene.uitDeGrond(c.level(), pos, kijk);   // (PHASE3 R18: not inside a hill at a camp on unknown land)
        }
        Vec3 d = kijk.subtract(pos);
        float yaw = Cutscene.yawVan(d), pitch = Cutscene.pitchVan(d);
        c.setOldPosAndRot();
        c.setPos(pos.x, pos.y - c.getEyeHeight(), pos.z);
        c.setYRot(c.yRotO + Mth.wrapDegrees(yaw - c.yRotO));
        c.setXRot(pitch);
        if (sprong) {
            c.setYRot(yaw);
            c.setOldPosAndRot();
        }
    }

    private static void beweeg(Cutscene s, String naam, Entity e) {
        Vec3 pos = Cutscene.wereld(anker, draai, s.plek(naam, t));
        Integer sprong = SPRONG.get(naam);
        if (sprong != null) {
            int dt = t - sprong;
            if (dt >= 8) {
                SPRONG.remove(naam);
            } else {
                pos = pos.add(0, 0.55 * Math.sin(Math.PI * dt / 8.0), 0);
            }
        }
        float yaw = Cutscene.wereldYaw(draai, s.yaw(naam, t));
        e.moveOrInterpolateTo(pos, yaw, 0);
        e.setYHeadRot(yaw);
        if (!s.looptNu(naam, t)) {
            e.setYBodyRot(yaw);
        }
        e.setDeltaMovement(Vec3.ZERO);
        e.setOnGround(true);
    }

    private static void animeer(Cutscene.Animatie a) {
        Entity e = ACTEURS.get(a.acteur());
        if (e == null) {
            return;
        }
        switch (a.naam()) {
            case "zwaai" -> {
                if (e instanceof LivingEntity l) {
                    l.swing(InteractionHand.MAIN_HAND);
                }
            }
            case "buk" -> e.setPose(Pose.CROUCHING);
            case "sta" -> e.setPose(Pose.STANDING);
            case "spring" -> SPRONG.put(a.acteur(), t);
            default -> Cutscenes.zetAnimatie(e, a.naam());
        }
    }

    /** "<name>: text" (the narrator: only the text, slanted). */
    static Component regel(Cutscene s, Cutscene.Zeg z) {
        Component tekst = Component.translatable(s.tekstKey(z.key()));
        if (z.spreker().isEmpty()) {
            return tekst.copy().withStyle(net.minecraft.ChatFormatting.ITALIC);
        }
        return Component.empty().append(naam(s, z.spreker()).copy().withStyle(net.minecraft.ChatFormatting.GOLD)).append(": ").append(tekst);
    }

    /** The name of a speaker: the scene's own name for it (scene.guhs.&lt;id&gt;.naam.&lt;acteur&gt;), else the actor's, else the player's. */
    static Component naam(Cutscene s, String acteur) {
        String key = s.tekstKey("naam." + acteur);
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        Minecraft mc = Minecraft.getInstance();
        if (Cutscene.SPELER.equals(acteur) && mc.player != null) {
            return mc.player.getName();
        }
        Entity e = ACTEURS.get(acteur);
        if (e != null) {
            return e.getDisplayName();
        }
        Cutscene.Acteur a = s.acteur(acteur);
        return a != null && a.type() != null ? a.type().get().getDescription() : Component.literal(acteur);
    }

    /** The camera's angles this frame: exactly on the path at this frame's time, plus the shake. */
    static void hoeken(ViewportEvent.ComputeCameraAngles event) {
        Cutscene s = scene;
        if (s == null || camera == null) {
            return;
        }
        float pt = (float) event.getPartialTick();
        if (schud > 0.01f) {
            double tijd = t + pt;
            float k = schud * schud;
            event.setYaw(event.getYaw() + (float) (Math.sin(tijd * 1.9) * k * 1.4));
            event.setPitch(event.getPitch() + (float) (Math.cos(tijd * 2.6) * k * 1.1));
            event.setRoll(event.getRoll() + (float) (Math.sin(tijd * 1.3) * k * 0.9));
        }
    }

    /** (the render mixin) is this entity not drawn while the scene plays? The real player, and the real entities near the anchor. */
    public static boolean verborgen(Entity e) {
        Cutscene s = scene;
        if (s == null || ACTEUR_IDS.contains(e.getId())) {
            return false;
        }
        if (e instanceof LocalPlayer) {
            return s.heeftSpeler();
        }
        double straal = s.verbergEcht();
        return straal > 0 && e.distanceToSqr(anker.getX() + 0.5, anker.getY(), anker.getZ() + 0.5) <= straal * straal;
    }

    /** Is this one of the scene's actors? */
    public static boolean isActeur(Entity e) {
        return ACTEUR_IDS.contains(e.getId());
    }

    // =====================================================================================================================
    // the camera and the stand-in
    // =====================================================================================================================

    /** The camera: an entity nobody sees, with the eye height the view had, so the picture doesn't bob when it takes over. */
    private static final class CameraEntiteit extends Entity {
        private final EntityDimensions maat;

        CameraEntiteit(Level level, float oogHoogte) {
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

    /** The player's stand-in: another player in this client's level with the viewer's skin and name (never a name tag). */
    private static final class Dubbelganger extends RemotePlayer {
        private final LocalPlayer echt;

        Dubbelganger(ClientLevel level, LocalPlayer echt) {
            super(level, new GameProfile(java.util.UUID.randomUUID(), echt.getGameProfile().name()));
            this.echt = echt;
        }

        @Override
        public PlayerSkin getSkin() {
            return echt.getSkin();
        }

        @Override
        public boolean shouldShowName() {
            return false;
        }

        @Override
        public boolean isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart part) {
            return echt.isModelPartShown(part);
        }
    }
}
