package nl.juiced.guhs.feature.piep.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
import com.geckolib.event.GeoRenderEvent;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

import net.minecraft.world.entity.EntitySpawnReason;
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

    /** The pieppiepmuisje: hidden while it plays verstoppertje. */
    public static class MuisjeRenderer extends GeoEntityRenderer<PieppiepmuisjeEntity> {
        public MuisjeRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id("pieppiepmuisje"), true));
            this.shadowRadius = 0.18f;
        }

        @Override
        public void render(PieppiepmuisjeEntity muis, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!muis.isVerstopt()) {
                super.render(muis, yaw, partialTick, pose, buffers, light);
            }
        }
    }

    /** Poepschilly and Schilly (each its own model): hidden while inside a guh, smaller while it crawls in or out. */
    public static class SchillyRenderer<T extends PoepschillyEntity> extends GeoEntityRenderer<T> {
        public SchillyRenderer(EntityRendererProvider.Context context, String naam) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id(naam), true));
            this.shadowRadius = 0.3f;
        }

        @Override
        public void render(T schilly, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (schilly.isBinnen()) {
                return;
            }
            float krimp = schilly.krimp();
            if (krimp >= 0.999f) {
                super.render(schilly, yaw, partialTick, pose, buffers, light);
                return;
            }
            pose.pushPose();
            pose.scale(krimp, krimp, krimp);
            super.render(schilly, yaw, partialTick, pose, buffers, light);
            pose.popPose();
        }
    }

    /** The kaasknabbels: scaled down (or up, the Oppernabbel); they don't tip over when beaten, they flop (animation zieli). */
    public static class KnabbelRenderer<T extends BozeKaasknabbelEntity> extends GeoEntityRenderer<T> {
        public KnabbelRenderer(EntityRendererProvider.Context context, String naam, float schaal, float schaduw) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id(naam), false));
            this.scaleWidth = schaal;
            this.scaleHeight = schaal;
            this.shadowRadius = schaduw;
        }

        @Override
        protected float getDeathMaxRotation(T entity) {
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
    private static void schouder(net.neoforged.neoforge.client.event.RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (!PiepPayloads.CLIENT_SCHOUDERS.containsKey(player.getId()) || player.isInvisible() || player.isSleeping()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
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
        double rad = Math.toRadians(yaw);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(Math.cos(rad) * SCHOUDER_OPZIJ, SCHOUDER_HOOGTE - (player.isCrouching() ? SCHOUDER_BUKKEN : 0f), Math.sin(rad) * SCHOUDER_OPZIJ);
        pose.scale(schaal, schaal, schaal);
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(muis)
                .render(muis, kijk, pt, pose, event.getMultiBufferSource(), event.getPackedLight());
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
            e.load(data.getCompoundOrEmpty("Uiterlijk"));
        } catch (RuntimeException ignored) {
            // (a look that doesn't load: the plain maatje)
        }
        return e;
    }

    // --- the wiggling guh ------------------------------------------------------------------------------------------------------

    /** A guh with Poepschilly inside wiggles and hops a little (it tickles). */
    private static void wiebel(GeoRenderEvent.Entity.Pre event) {
        if (!(event.getEntity() instanceof GuhEntity guh)) {
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
        float t = (guh.tickCount + event.getPartialTick()) * 0.9f;
        PoseStack pose = event.getPoseStack();
        pose.translate(0, Math.abs(Math.sin(t * 0.5f)) * 0.08f, 0);
        pose.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(t) * 9f));
        pose.mulPose(Axis.YP.rotationDegrees((float) Math.sin(t * 0.7f) * 6f));
    }

    // --- particles ---------------------------------------------------------------------------------------------------------------

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(PiepFeature.PIEPJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Piepje(level, x, y, z, sprites));
        event.registerSpriteSet(PiepFeature.FRIS_SPARKEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Sparkel(level, x, y, z, sprites));
    }

    /** A little "piep!" note: floats up, wobbling, and fades. */
    static class Piepje extends TextureSheetParticle {
        Piepje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
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
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A sparkle of a guh that is fris van binnen: twinkles and is gone. */
    static class Sparkel extends TextureSheetParticle {
        private final SpriteSet sprites;

        Sparkel(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
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
        public int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private PiepClient() {
    }

    static boolean isSpeler(LivingEntity e) {
        return e instanceof Player;
    }
}
