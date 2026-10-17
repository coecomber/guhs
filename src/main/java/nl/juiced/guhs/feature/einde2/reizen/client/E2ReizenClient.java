package nl.juiced.guhs.feature.einde2.reizen.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.reizen.E2ReizenFeature;
import nl.juiced.guhs.feature.einde2.reizen.ZukaSchipEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

/**
 * einde2 phase B/C, slice reizen, client side: the renderer of Zuka's ship, Appa's whistle list ({@link FluitScherm}) and
 * the furniture screen of an own whale ({@link InrichtingScherm}). The whale's furniture itself is drawn by the whale's
 * renderer (zwevers.client.E2ZweversClient: the {@code inr_*} bones).
 */
public final class E2ReizenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(E2ReizenFeature.ZUKA_SCHIP.get(),
                SchipRenderer::new));
    }

    /** (payload) the whistle list. */
    public static void fluitlijst(CompoundTag data) {
        Minecraft.getInstance().setScreen(new FluitScherm(data));
    }

    /** (payload, fix4-appa) on Appa, ready to leave: the "Yip yip!" dialog. */
    public static void vertrek(CompoundTag data) {
        Minecraft.getInstance().setScreen(new YipYipScherm(data));
    }

    /** The name of a row of Appa's menu: a building, or "<naam>s Appa-platform". */
    static net.minecraft.network.chat.MutableComponent naam(CompoundTag rij) {
        if (rij.getBooleanOr("Platform", false)) {
            return nl.juiced.guhs.feature.einde2.reizen.AppaPlatform.naam(rij.getStringOr("Speler", "")).copy();
        }
        return net.minecraft.network.chat.Component.translatable(rij.getStringOr("Naam", ""));
    }

    /** What a row adds in grey: the distance, or the dimension of a platform there / "door de leegte". Null: nothing. */
    @Nullable
    static net.minecraft.network.chat.Component extra(CompoundTag rij) {
        int afstand = rij.getIntOr("Afstand", -1);
        if (afstand >= 0) {
            return net.minecraft.network.chat.Component.translatable("gui.guhs.einde2_reizen.fluit.afstand", afstand);
        }
        if (rij.getBooleanOr("Platform", false)) {
            String dim = rij.getStringOr("Dim", "");
            return net.minecraft.network.chat.Component.translatable("gui.guhs.einde2_reizen.platform.daar",
                    net.minecraft.network.chat.Component.translatableWithFallback(nl.juiced.guhs.feature.einde2.reizen.AppaPlatform.dimSleutel(dim), dim));
        }
        if (!rij.getBooleanOr("Opstappen", false) && rij.getBooleanOr("Leegte", false)) {
            return net.minecraft.network.chat.Component.translatable("gui.guhs.einde2_reizen.fluit.leegte");
        }
        return null;
    }

    /** (payload) the furniture screen. */
    public static void inrichting(CompoundTag data) {
        Minecraft.getInstance().setScreen(new InrichtingScherm(data));
    }

    /** Zuka's ship: geckolib/models/entity/einde2_reizen_zuka_schip.geo.json at {@link ZukaSchipEntity#SCHAAL}, its lanterns glowing. */
    public static class SchipRenderer extends GeoEntityRenderer<ZukaSchipEntity, EntityRenderState> {
        private static final DataTicket<Float> YAW = DataTicket.create("guhs_einde2_reizen_schip_yaw", Float.class);

        public SchipRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<ZukaSchipEntity>(Guhs.id("einde2_reizen_zuka_schip")));
            withScale(ZukaSchipEntity.SCHAAL);
            withRenderLayer(new AutoGlowingGeoLayer<>(this));
            this.shadowRadius = 0f;
        }

        @Override
        protected AABB getBoundingBoxForCulling(ZukaSchipEntity schip) {
            return schip.getBoundingBox().inflate(8, 2, 8).expandTowards(0, 6, 0);
        }

        @Override
        public void addRenderData(ZukaSchipEntity schip, @Nullable Void related, EntityRenderState state, float partialTick) {
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

    private E2ReizenClient() {
    }
}
