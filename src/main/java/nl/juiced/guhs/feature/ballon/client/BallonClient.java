package nl.juiced.guhs.feature.ballon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ballon.BallonFeature;
import nl.juiced.guhs.feature.ballon.LuchtballonEntity;
import nl.juiced.guhs.registry.ModEntities;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Client side of the Ballonfestival: the guh balloon (geo/entity/guh_luchtballon.geo.json in four colours, turned
 * along its flight and swaying gently, with Kapitein Wolkje in the basket while it flies), the cloud puffs, and the
 * Kapitein's own model (pilot's cap with goggles and a flying scarf).
 */
public final class BallonClient {
    public static final String[] KLEUREN = {"roze", "mint", "lavendel", "citroen"};

    public static void init(IEventBus modBus) {
        modBus.addListener(BallonClient::renderers);
        modBus.addListener(BallonClient::particles);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BALLONGUH, Guhs.id("geo/entity/guh_npc_ballonguh.geo.json"));
        // his flying scarf flutters
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.BALLONGUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.25f;
            bot.apply("kapitein_sjaalpunt").ifPresent(b -> {
                b.setRotY((float) Math.sin(t) * 0.35f);
                b.setRotX(0.2f + (float) Math.sin(t * 1.7f) * 0.12f);
            });
        });
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BallonFeature.LUCHTBALLON.get(), LuchtballonRenderer::new);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BallonFeature.BALLONWOLKJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Wolkje(level, x, y, z, dx, dy, dz, sprites));
    }

    /** The guh balloon: turned along the flight, swaying, with Kapitein Wolkje in the basket while it flies. */
    public static class LuchtballonRenderer extends GeoEntityRenderer<LuchtballonEntity> {
        private final EntityRenderDispatcher dispatcher;
        private GuhNpcEntity wolkje;

        public LuchtballonRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<LuchtballonEntity>(Guhs.id("guh_luchtballon")) {
                @Override
                public Identifier getTextureResource(LuchtballonEntity ballon) {
                    return Guhs.id("textures/entity/guh_luchtballon_" + KLEUREN[ballon.kleur()] + ".png");
                }
            });
            this.shadowRadius = 1.0f;
            this.dispatcher = context.getEntityRenderDispatcher();
        }

        @Override
        protected void applyRotations(LuchtballonEntity ballon, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick,
                                      float nativeScale) {
            super.applyRotations(ballon, poseStack, ageInTicks, Mth.rotLerp(partialTick, ballon.yRotO, ballon.getYRot()), partialTick, nativeScale);
            float t = ageInTicks + ballon.getId() * 13;
            float zwaai = ballon.vliegt() ? 2.2f : 1.0f;
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 0.045f) * zwaai));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(t * 0.037f) * zwaai * 0.8f));
        }

        @Override
        public void render(LuchtballonEntity ballon, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
            poseStack.pushPose();
            if (!ballon.vliegt()) {           // waiting: bobbing a little on its ropes
                poseStack.translate(0, Mth.sin((ballon.tickCount + partialTick + ballon.getId() * 7) * 0.06f) * 0.06f, 0);
            }
            super.render(ballon, entityYaw, partialTick, poseStack, buffers, light);
            poseStack.popPose();
            if (!ballon.vliegt()) {
                return;
            }
            if (wolkje == null || wolkje.level() != ballon.level()) {
                wolkje = ModEntities.GUH_NPC.get().create(ballon.level(), EntitySpawnReason.TRIGGERED);
                if (wolkje == null) {
                    return;
                }
                wolkje.setKind(GuhNpcEntity.Kind.BALLONGUH);
                wolkje.setCustomNameVisible(false);
            }
            float yaw = Mth.rotLerp(partialTick, ballon.yRotO, ballon.getYRot());
            Vec3 plek = new Vec3(0, 0.2, -0.5).yRot(-yaw * Mth.DEG_TO_RAD);
            wolkje.setYRot(yaw);
            wolkje.yRotO = yaw;
            wolkje.yBodyRot = wolkje.yBodyRotO = wolkje.yHeadRot = wolkje.yHeadRotO = yaw;
            wolkje.tickCount = ballon.tickCount;
            dispatcher.render(wolkje, plek.x, plek.y, plek.z, yaw, partialTick, poseStack, buffers, light);
        }
    }

    /** A soft cloud puff: grows a little, drifts, fades out. */
    static class Wolkje extends TextureSheetParticle {
        private final float basis;

        Wolkje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz);
            pickSprite(sprites);
            lifetime = 60 + random.nextInt(60);
            basis = 0.35f + random.nextFloat() * 0.45f;
            quadSize = basis;
            gravity = 0;
            xd = dx + (random.nextDouble() - 0.5) * 0.02;
            yd = dy + random.nextDouble() * 0.005;
            zd = dz + (random.nextDouble() - 0.5) * 0.02;
            hasPhysics = false;
            alpha = 0;
        }

        @Override
        public void tick() {
            super.tick();
            float f = age / (float) lifetime;
            quadSize = basis * (0.8f + 0.5f * f);
            alpha = Math.min(1f, Math.min(f * 6f, (1f - f) * 3f)) * 0.85f;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private BallonClient() {
    }
}
