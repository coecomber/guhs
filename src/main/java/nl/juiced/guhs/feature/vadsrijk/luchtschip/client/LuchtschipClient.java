package nl.juiced.guhs.feature.vadsrijk.luchtschip.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.LuchtschipEntity;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.LuchtschipRegistry;
import nl.juiced.guhs.feature.vadsrijk.luchtschip.Tijd;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

/** T-LUCHTSCHIP, client: the airship renderer, the departure-board text markers, the silhouettes (Schimmen) and the payloads. */
public final class LuchtschipClient {
    private LuchtschipClient() {
    }

    public static void init(IEventBus modBus) {
        DekLopen.init();   // U-LUCHTSCHIP: walking on the deck
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(LuchtschipRegistry.SCHIP.get(), SchipRenderer::new);
            event.registerEntityRenderer(LuchtschipRegistry.BORD.get(), NoopRenderer::new);
        });
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterSky e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && Vadsrijk.in(mc.level)) {
                Schimmen.teken(e);
            }
        });
        Tijd.schimmenOntvanger = s -> Minecraft.getInstance().execute(() -> {
            Schimmen.stand = s;
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                        "Schimmen: " + (s == 0 ? "uit" : s == 1 ? "aan" : "altijd (volle kracht)")));
            }
        });
    }

    /** The airship: geckolib vr_luchtschip at {@link LuchtschipEntity#SCHAAL}, lanterns + windows glowing at night. */
    public static class SchipRenderer extends GeoEntityRenderer<LuchtschipEntity, EntityRenderState> {
        private static final DataTicket<Float> YAW = DataTicket.create("guhs_vr_luchtschip_yaw", Float.class);

        public SchipRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<LuchtschipEntity>(Guhs.id("vr_luchtschip")));
            withScale(LuchtschipEntity.SCHAAL);
            withRenderLayer(new AutoGlowingGeoLayer<>(this));
            this.shadowRadius = 0f;
        }

        @Override
        protected AABB getBoundingBoxForCulling(LuchtschipEntity schip) {
            return schip.cullBox();
        }

        @Override
        public void addRenderData(LuchtschipEntity schip, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(YAW, Mth.rotLerp(partialTick, schip.yRotO, schip.getYRot()));
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            Float yaw = info.getGeckolibData(YAW);
            if (yaw != null) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180f - yaw));
            }
        }
    }
}
