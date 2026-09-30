package nl.juiced.guhs.feature.piep.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.event.entity.CompileEntityRenderStateEvent;
import com.geckolib.event.entity.GeoEntityPreRenderEvent;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.piep.BozeKaasknabbelEntity;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.piep.PiepPayloads;
import nl.juiced.guhs.feature.piep.PieppiepmuisjeEntity;
import nl.juiced.guhs.feature.piep.PoepschillyEntity;

/**
 * Client side of Piep: the GeckoLib renderers (the exported models in assets/guhs/geo/entity: pieppiepmuisje, poepschilly,
 * boze_kaasknabbel, boze_oppernabbel), the muisje on a player's shoulder (a render layer on every player), the guh that
 * wiggles while Poepschilly is inside it, and the particles (piepje, fris_sparkel).
 */
public final class PiepClient {
    /** The in-game size of the kaasknabbel models (the model is 14 px tall). */
    public static final float KNABBEL_SCHAAL = 0.55f, OPPERNABBEL_SCHAAL = 1.65f;

    public static void init(IEventBus modBus) {
        modBus.addListener(PiepClient::renderers);
        modBus.addListener(PiepClient::particles);
        NeoForge.EVENT_BUS.addListener(PiepClient::wiebelData);
        NeoForge.EVENT_BUS.addListener(PiepClient::wiebel);
        NeoForge.EVENT_BUS.addListener(PiepClient::schouder);
        PiepMenuClient.init();                                  // (the little menus, and the poetsbeurt click on your own guh)
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PiepFeature.PIEPPIEPMUISJE.get(), MuisjeRenderer::new);
        event.registerEntityRenderer(PiepFeature.POEPSCHILLY.get(), c -> new SchillyRenderer<>(c, "poepschilly"));
        event.registerEntityRenderer(PiepFeature.SCHILLY.get(), c -> new SchillyRenderer<>(c, "schilly"));
        event.registerEntityRenderer(PiepFeature.BOZE_KAASKNABBEL.get(), c -> new KnabbelRenderer<>(c, "boze_kaasknabbel", KNABBEL_SCHAAL, 0.25f));
        event.registerEntityRenderer(PiepFeature.BOZE_OPPERNABBEL.get(), c -> new KnabbelRenderer<>(c, "boze_oppernabbel", OPPERNABBEL_SCHAAL, 0.6f));
    }

    /** Adds GeckoLib 4's "turnsHead" (the "head" bone follows where it looks). */
    static <R extends LivingEntityRenderState & GeoRenderState> void kijk(RenderPassInfo<R> info, BoneSnapshots bones) {
        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
    }

    /** Render state ticket: don't draw the model this frame (verstopt / inside a guh). */
    static final DataTicket<Boolean> WEG = DataTicket.create("guhs_piep_weg", Boolean.class);
    /** Render state ticket: Schilly's size while it crawls in or out. */
    static final DataTicket<Float> KRIMP = DataTicket.create("guhs_piep_krimp", Float.class);

    /** The pieppiepmuisje: hidden while it plays verstoppertje. */
    public static class MuisjeRenderer extends GeoEntityRenderer<PieppiepmuisjeEntity, LivingEntityRenderState> {
        public MuisjeRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id("pieppiepmuisje")));
            this.shadowRadius = 0.18f;
        }

        @Override
        public void addRenderData(PieppiepmuisjeEntity muis, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(WEG, muis.isVerstopt());
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            kijk(info, bones);
        }

        @Override
        public void submit(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            if (!Boolean.TRUE.equals(state.getGeckolibData(WEG))) {
                super.submit(state, pose, collector, camera);
            }
        }
    }

    /** Poepschilly and Schilly (each its own model): hidden while inside a guh, smaller while it crawls in or out. */
    public static class SchillyRenderer<T extends PoepschillyEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        public SchillyRenderer(EntityRendererProvider.Context context, String naam) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id(naam)));
            this.shadowRadius = 0.3f;
        }

        @Override
        public void addRenderData(T schilly, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(WEG, schilly.isBinnen());
            state.addGeckolibData(KRIMP, schilly.krimp());
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            kijk(info, bones);
        }

        @Override
        public void submit(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            if (Boolean.TRUE.equals(state.getGeckolibData(WEG))) {
                return;
            }
            Float k = state.getGeckolibData(KRIMP);
            float krimp = k == null ? 1f : k;
            if (krimp >= 0.999f) {
                super.submit(state, pose, collector, camera);
                return;
            }
            pose.pushPose();
            pose.scale(krimp, krimp, krimp);
            super.submit(state, pose, collector, camera);
            pose.popPose();
        }
    }

    /** The kaasknabbels: scaled down (or up, the Oppernabbel); they don't tip over when beaten, they flop (animation zieli). */
    public static class KnabbelRenderer<T extends BozeKaasknabbelEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        public KnabbelRenderer(EntityRendererProvider.Context context, String naam, float schaal, float schaduw) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id(naam)));
            withScale(schaal);
            this.shadowRadius = schaduw;
        }

        @Override
        protected float getDeathMaxRotation(GeoRenderState state) {
            return 0f;
        }
    }

    // --- the muisje on a shoulder ----------------------------------------------------------------------------------------------

    /** 3.0: any maatje (by its type), one per player. */
    private static final Map<Integer, net.minecraft.world.entity.Entity> SCHOUDER_MUISJES = new ConcurrentHashMap<>();
    /** Where the muisje sits: this far to the player's left, this high (the top of the shoulder). */
    public static final float SCHOUDER_OPZIJ = 0.36f, SCHOUDER_HOOGTE = 1.41f, SCHOUDER_BUKKEN = 0.22f;

    /**
     * Draws a muisje on the left shoulder of every player that carries one (see Schouder), after the player itself: in world
     * space (on the shoulder of the body, looking where the player looks), so it renders exactly like a muisje standing there.
     */
    private static void schouder(net.neoforged.neoforge.client.event.RenderPlayerEvent.Post<?> event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(event.getRenderState().id) instanceof Player player)) {
            return;
        }
        if (!PiepPayloads.CLIENT_SCHOUDERS.containsKey(player.getId()) || player.isInvisible() || player.isSleeping()) {
            return;
        }
        net.minecraft.nbt.CompoundTag data = PiepPayloads.CLIENT_SCHOUDERS.get(player.getId());
        String type = data == null || !data.contains("id") ? "guhs:pieppiepmuisje" : data.getStringOr("id", "");
        net.minecraft.world.entity.Entity e = SCHOUDER_MUISJES.compute(player.getId(), (id, m) -> m != null && m.level() == level
                && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).toString().equals(type) ? m
                : maak(data, level));
        if (!(e instanceof net.minecraft.world.entity.LivingEntity muis)) {
            return;
        }
        float pt = event.getPartialTick();
        float yaw = net.minecraft.util.Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot);
        float schaal = 0.9f;
        if (muis instanceof nl.juiced.guhs.feature.piep.PiepMaatje maatje) {
            maatje.opSchouder(true);
            schaal *= maatje.schouderSchaal();
        }
        muis.tickCount = player.tickCount;
        float kijk = net.minecraft.util.Mth.rotLerp(pt, player.yHeadRotO, player.yHeadRot);   // (it looks where you look)
        muis.yBodyRot = muis.yBodyRotO = kijk;
        muis.yHeadRot = muis.yHeadRotO = kijk;
        muis.setYRot(kijk);
        muis.yRotO = kijk;
        // 1.1.0: extract the copy's render state now and submit it with the player (was EntityRenderer#render)
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderState state = dispatcher.extractEntity(muis, pt);
        state.lightCoords = event.getRenderState().lightCoords;
        state.shadowPieces.clear();
        double rad = Math.toRadians(yaw);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(Math.cos(rad) * SCHOUDER_OPZIJ, SCHOUDER_HOOGTE - (player.isCrouching() ? SCHOUDER_BUKKEN : 0f), Math.sin(rad) * SCHOUDER_OPZIJ);
        pose.scale(schaal, schaal, schaal);
        dispatcher.submit(state, Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState, 0, 0, 0, pose,
                event.getSubmitNodeCollector());
        pose.popPose();
    }

    /** The shoulder copy of a maatje (its type and look from the SchouderData; a muisje for old data). */
    @javax.annotation.Nullable
    private static net.minecraft.world.entity.Entity maak(@javax.annotation.Nullable net.minecraft.nbt.CompoundTag data, ClientLevel level) {
        if (data == null || !data.contains("id")) {
            return PiepFeature.PIEPPIEPMUISJE.get().create(level, EntitySpawnReason.TRIGGERED);
        }
        net.minecraft.world.entity.Entity e = net.minecraft.world.entity.EntityType.byString(data.getStringOr("id", ""))
                .map(t -> (net.minecraft.world.entity.Entity) t.create(level, EntitySpawnReason.TRIGGERED)).orElse(null);
        if (e == null) {
            return PiepFeature.PIEPPIEPMUISJE.get().create(level, EntitySpawnReason.TRIGGERED);
        }
        try {
            nl.juiced.guhs.storage.Nbt.load(e, data.getCompoundOrEmpty("Uiterlijk"));
        } catch (RuntimeException ignored) {
            // (a look that doesn't load: the plain maatje)
        }
        return e;
    }

    // --- the wiggling guh ------------------------------------------------------------------------------------------------------

    /** Render state ticket: the wiggle clock of a guh with Poepschilly inside (absent: no wiggle). */
    static final DataTicket<Float> WIEBEL = DataTicket.create("guhs_piep_wiebel", Float.class);

    /**
     * A guh with Poepschilly inside wiggles and hops a little (it tickles). 1.1.0: at extract time (GeckoLib's render state
     * event, the guh is there) the wiggle clock goes into the render state ...
     */
    private static void wiebelData(CompileEntityRenderStateEvent<?, ?> event) {
        if (!(event.getAnimatable() instanceof GuhEntity guh)) {
            return;
        }
        Long tot = PiepPayloads.CLIENT_WIEBEL.get(guh.getId());
        if (tot == null) {
            return;
        }
        long now = guh.level().getGameTime();
        if (now > tot) {
            PiepPayloads.CLIENT_WIEBEL.remove(guh.getId());
            return;
        }
        event.getRenderState().addGeckolibData(WIEBEL, (guh.tickCount + event.getRenderState().partialTick) * 0.9f);
    }

    /** ... and at render time it moves the pose before GeckoLib turns and scales the model (like 1.0.0's GeoRenderEvent.Entity.Pre). */
    private static void wiebel(GeoEntityPreRenderEvent<?, ?> event) {
        Float wiebel = event.getRenderState().getGeckolibData(WIEBEL);
        if (wiebel == null) {
            return;
        }
        float t = wiebel;
        PoseStack pose = event.getPoseStack();
        pose.translate(0, Math.abs(Math.sin(t * 0.5f)) * 0.08f, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(t) * 9f));
        pose.mulPose(Axis.YP.rotationDegrees((float) Math.sin(t * 0.7f) * 6f));
    }

    // --- particles ---------------------------------------------------------------------------------------------------------------

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(PiepFeature.PIEPJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Piepje(level, x, y, z, sprites.get(random)));
        event.registerSpriteSet(PiepFeature.FRIS_SPARKEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Sparkel(level, x, y, z, sprites, random));
    }

    /** A little "piep!" note: floats up, wobbling, and fades. */
    static class Piepje extends SingleQuadParticle {
        Piepje(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
            lifetime = 24 + random.nextInt(10);
            quadSize = 0.12f;
            gravity = -0.02f;
            yd = 0.03;
            xd = (random.nextDouble() - 0.5) * 0.02;
            zd = (random.nextDouble() - 0.5) * 0.02;
            friction = 0.94f;
        }

        @Override
        public void tick() {
            super.tick();
            xd += Math.sin(age * 0.5) * 0.004;
            alpha = Math.min(1f, (lifetime - age) / 8f);
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A sparkle of a guh that is fris van binnen: twinkles and is gone. */
    static class Sparkel extends SingleQuadParticle {
        private final SpriteSet sprites;

        Sparkel(ClientLevel level, double x, double y, double z, SpriteSet sprites, RandomSource random) {
            super(level, x, y, z, sprites.get(random));
            this.sprites = sprites;
            setSpriteFromAge(sprites);
            lifetime = 12 + random.nextInt(8);
            quadSize = 0.1f + random.nextFloat() * 0.05f;
            gravity = 0;
            xd = yd = zd = 0;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            quadSize *= 0.96f;
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private PiepClient() {
    }

    static boolean isSpeler(LivingEntity e) {
        return e instanceof Player;
    }
}
