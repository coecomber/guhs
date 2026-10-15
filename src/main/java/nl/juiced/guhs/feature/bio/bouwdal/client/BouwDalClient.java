package nl.juiced.guhs.feature.bio.bouwdal.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.bouwdal.BouwDalSlice;
import nl.juiced.guhs.feature.bio.bouwdal.WeebBallonEntity;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * Client side of the biomes3 slice "bouw-dal": the looks of Evivads and Nielsvads (their own models: hair, glasses, bow,
 * rugzakje) and the balloon of het weebhuisje, which is drawn with the model and the pictures of the Ballonfestival's
 * guh balloon.
 */
public final class BouwDalClient {
    private static final String[] KLEUREN = {"roze", "mint", "lavendel", "citroen"};

    public static void init(IEventBus modBus) {
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.WEEB_EVIVADS, Guhs.id("entity/guh_npc_weeb_evivads"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.WEEB_NIELSVADS, Guhs.id("entity/guh_npc_weeb_nielsvads"));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(BouwDalSlice.WEEB_BALLON.get(), BallonRenderer::new));
    }

    public static class BallonRenderer extends GeoEntityRenderer<WeebBallonEntity, EntityRenderState> {
        private static final DataTicket<Integer> KLEUR = DataTicket.create("guhs_weeb_ballon_kleur", Integer.class);
        private static final DataTicket<Float> ZWAAI = DataTicket.create("guhs_weeb_ballon_zwaai", Float.class);

        public BallonRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<WeebBallonEntity>(Guhs.id("guh_luchtballon")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Guhs.id("textures/entity/guh_luchtballon_" + KLEUREN[state.getOrDefaultGeckolibData(KLEUR, 0)] + ".png");
                }
            });
            this.shadowRadius = 1.0f;
        }

        @Override
        protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(WeebBallonEntity ballon) {
            return ballon.cullBox();
        }

        @Override
        public void addRenderData(WeebBallonEntity ballon, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(KLEUR, Math.floorMod(ballon.kleur(), KLEUREN.length));
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, ballon.yRotO, ballon.getYRot()));
            state.addGeckolibData(ZWAAI, ballon.tickCount + partialTick + ballon.getId() * 13);
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            super.applyRotations(info, poseStack, nativeScale);
            Float z = info.getGeckolibData(ZWAAI);
            if (z != null) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(z * 0.045f) * 1.2f));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(z * 0.037f) * 1.0f));
            }
        }
    }

    private BouwDalClient() {
    }
}
