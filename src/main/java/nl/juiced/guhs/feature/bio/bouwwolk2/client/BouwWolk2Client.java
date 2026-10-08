package nl.juiced.guhs.feature.bio.bouwwolk2.client;

import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.bouwwolk2.BouwWolk2Slice;
import nl.juiced.guhs.feature.bio.bouwwolk2.ReuzenguhEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * Client side of the biomes3 slice "bouw-wolk2": the giant (the guh's own model, five times as big; closed eyes, or one
 * eye open while he peeks), the smid-guh's model with his apron and hammer, and the two particles: the "z" of the sleeping
 * giant and the little flash of the thundercloud. BioClient calls {@link #init}.
 */
public final class BouwWolk2Client {
    static final DataTicket<Boolean> LOERT = DataTicket.create("guhs_bio_bouw_wolk2_reus_loert", Boolean.class);
    private static final Identifier SLAAP = Guhs.id("textures/entity/guh_slaap/guh.png"), LOER = Guhs.id("textures/entity/reuzenguh_loer.png");
    /** The pictures of reuzenguh_zzz, small to big. */
    private static final int ZZZ_BEELDEN = 3;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(BouwWolk2Slice.REUZENGUH.get(), ReusRenderer::new));
        modBus.addListener((RegisterParticleProvidersEvent event) -> {
            // (the giant passes which "z" it is as the x speed: 0, 1, 2)
            event.registerSpriteSet(BouwWolk2Slice.ZZZ.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                    new Zzz(level, x, y, z, dy, (int) Math.round(dx), sprites.get(Math.max(0, Math.min(ZZZ_BEELDEN - 1, (int) Math.round(dx))), ZZZ_BEELDEN - 1)));
            event.registerSpriteSet(BouwWolk2Slice.FLITS.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Flits(level, x, y, z, sprites.get(random)));
        });
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.SMIDGUH, Guhs.id("entity/guh_npc_smidguh"));
    }

    /** The giant: geckolib/models/entity/reuzenguh.geo.json (the guh's plain bones), the guh's own fur. */
    public static class ReusRenderer extends GeoEntityRenderer<ReuzenguhEntity, LivingEntityRenderState> {
        public ReusRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<ReuzenguhEntity>(Guhs.id("reuzenguh")) {
                @Override
                public void addAdditionalStateData(ReuzenguhEntity reus, @Nullable Object related, GeoRenderState state) {
                    state.addGeckolibData(LOERT, reus.staat() == ReuzenguhEntity.Staat.LOER);
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    Boolean loert = state.getGeckolibData(LOERT);
                    return loert != null && loert ? LOER : SLAAP;
                }
            });
            this.shadowRadius = 2.4f;
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            super.scaleModelForRender(info, widthScale * ReuzenguhEntity.SCHAAL, heightScale * ReuzenguhEntity.SCHAAL);
        }
    }

    /** A "z" from the sleeping giant: drifts up and to the side, grows a little, fades. */
    static final class Zzz extends SingleQuadParticle {
        private final float basis;

        Zzz(ClientLevel level, double x, double y, double z, double dy, int welke, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0, 0, 0, sprite);
            lifetime = 46 + random.nextInt(10);
            basis = 0.28f + 0.16f * welke;
            quadSize = basis;
            gravity = 0;
            friction = 0.98f;
            hasPhysics = false;
            xd = 0.012 + random.nextDouble() * 0.008;
            yd = dy;
            zd = (random.nextDouble() - 0.5) * 0.01;
            alpha = 0;
        }

        @Override
        public void tick() {
            super.tick();
            float f = age / (float) lifetime;
            quadSize = basis * (0.8f + 0.5f * f);
            alpha = Math.min(1f, Math.min(f * 5f, (1f - f) * 2.2f)) * 0.95f;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A flash on the thundercloud: there at once, gone in a blink, always bright. */
    static final class Flits extends SingleQuadParticle {
        Flits(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0, 0, 0, sprite);
            lifetime = 5 + random.nextInt(4);
            quadSize = 0.45f + random.nextFloat() * 0.35f;
            gravity = 0;
            hasPhysics = false;
            xd = yd = zd = 0;
            roll = oRoll = (random.nextFloat() - 0.5f) * 0.8f;
        }

        @Override
        public void tick() {
            super.tick();
            float f = age / (float) lifetime;
            alpha = f < 0.3f ? 1f : Math.max(0f, 1f - (f - 0.3f) / 0.7f) * (age % 2 == 0 ? 1f : 0.55f);
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }
    }

    private BouwWolk2Client() {
    }
}
