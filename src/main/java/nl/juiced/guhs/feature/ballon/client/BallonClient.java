package nl.juiced.guhs.feature.ballon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
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
import javax.annotation.Nullable;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.constant.DataTickets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.particle.SingleQuadParticle;
/**
 * Client side of the Ballonfestival: the guh balloon (geckolib/models/entity/guh_luchtballon.geo.json in four colours, turned
 * along its flight and swaying gently, with Kapitein Wolkje in the basket while it flies), the cloud puffs, and the
 * Kapitein's own model (pilot's cap with goggles and a flying scarf).
 */
public final class BallonClient {
    public static final String[] KLEUREN = {"roze", "mint", "lavendel", "citroen"};

    public static void init(IEventBus modBus) {
        modBus.addListener(BallonClient::renderers);
        modBus.addListener(BallonClient::particles);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BALLONGUH, Guhs.id("entity/guh_npc_ballonguh"));
        // his flying scarf flutters
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.BALLONGUH, (npc, tick) -> {
            float t = (float) tick * 0.25f;
            return bones -> bones.ifPresent("kapitein_sjaalpunt", b -> {
                b.setRotY((float) Math.sin(t) * 0.35f);
                b.setRotX(0.2f + (float) Math.sin(t * 1.7f) * 0.12f);
            });
        });
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BallonFeature.LUCHTBALLON.get(), LuchtballonRenderer::new);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BallonFeature.BALLONWOLKJE.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Wolkje(level, x, y, z, dx, dy, dz, sprites.get(random)));
    }

    /**
     * The guh balloon: turned along the flight, swaying, with Kapitein Wolkje in the basket while it flies.
     * <p>
     * 1.1.0: everything the render needs is copied at extract time (colour, flying, sway time, the bobbing offset, and
     * Kapitein Wolkje's own render state, drawn after the balloon like the sled's pullers).
     */
    public static class LuchtballonRenderer extends GeoEntityRenderer<LuchtballonEntity, EntityRenderState> {
        private static final DataTicket<Integer> KLEUR = DataTicket.create("guhs_ballon_kleur", Integer.class);
        /** {sway time, sway strength, bob offset} */
        private static final DataTicket<float[]> ZWAAI = DataTicket.create("guhs_ballon_zwaai", float[].class);
        private static final DataTicket<Kapitein> KAPITEIN = DataTicket.create("guhs_ballon_kapitein", Kapitein.class);

        private record Kapitein(EntityRenderState state, Vec3 at) {
        }

        private final EntityRenderDispatcher dispatcher;
        private GuhNpcEntity wolkje;

        public LuchtballonRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<LuchtballonEntity>(Guhs.id("guh_luchtballon")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Guhs.id("textures/entity/guh_luchtballon_" + KLEUREN[state.getOrDefaultGeckolibData(KLEUR, 0)] + ".png");
                }
            });
            this.shadowRadius = 1.0f;
            this.dispatcher = context.getEntityRenderDispatcher();
        }

        @Override
        protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(LuchtballonEntity ballon) {
            return ballon.getBoundingBoxForCulling();
        }

        @Override
        public void addRenderData(LuchtballonEntity ballon, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(KLEUR, Math.floorMod(ballon.kleur(), KLEUREN.length));
            float yaw = Mth.rotLerp(partialTick, ballon.yRotO, ballon.getYRot());
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, yaw);
            float age = ballon.tickCount + partialTick;
            // waiting: bobbing a little on its ropes
            float bob = ballon.vliegt() ? 0 : Mth.sin((ballon.tickCount + partialTick + ballon.getId() * 7) * 0.06f) * 0.06f;
            state.addGeckolibData(ZWAAI, new float[]{age + ballon.getId() * 13, ballon.vliegt() ? 2.2f : 1.0f, bob});
            state.addGeckolibData(KAPITEIN, ballon.vliegt() ? kapitein(ballon, state, yaw, partialTick) : null);
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            super.applyRotations(info, poseStack, nativeScale);
            float[] z = info.getGeckolibData(ZWAAI);
            if (z != null) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(z[0] * 0.045f) * z[1]));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(z[0] * 0.037f) * z[1] * 0.8f));
            }
        }

        @Override
        public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            float[] z = state.getGeckolibData(ZWAAI);
            poseStack.pushPose();
            if (z != null && z[2] != 0) {
                poseStack.translate(0, z[2], 0);
            }
            super.submit(state, poseStack, collector, camera);
            poseStack.popPose();
            Kapitein k = state.getGeckolibData(KAPITEIN);
            if (k != null) {
                dispatcher.submit(k.state(), camera, k.at().x, k.at().y, k.at().z, poseStack, collector);
            }
        }

        private @Nullable Kapitein kapitein(LuchtballonEntity ballon, EntityRenderState ballonState, float yaw, float partialTick) {
            if (wolkje == null || wolkje.level() != ballon.level()) {
                wolkje = ModEntities.GUH_NPC.get().create(ballon.level(), EntitySpawnReason.TRIGGERED);
                if (wolkje == null) {
                    return null;
                }
                wolkje.setKind(GuhNpcEntity.Kind.BALLONGUH);
                wolkje.setCustomNameVisible(false);
            }
            Vec3 plek = new Vec3(0, 0.2, -0.5).yRot(-yaw * Mth.DEG_TO_RAD);
            wolkje.setYRot(yaw);
            wolkje.yRotO = yaw;
            wolkje.yBodyRot = wolkje.yBodyRotO = wolkje.yHeadRot = wolkje.yHeadRotO = yaw;
            wolkje.tickCount = ballon.tickCount;
            EntityRenderState s = dispatcher.extractEntity(wolkje, partialTick);
            s.lightCoords = ballonState.lightCoords;     // (the fake Kapitein stands nowhere: use the balloon's light)
            s.shadowPieces.clear();
            return new Kapitein(s, plek);
        }
    }

    /** A soft cloud puff: grows a little, drifts, fades out. */
    static class Wolkje extends SingleQuadParticle {
        private final float basis;

        Wolkje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
            super(level, x, y, z, dx, dy, dz, sprite);
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
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private BallonClient() {
    }
}
