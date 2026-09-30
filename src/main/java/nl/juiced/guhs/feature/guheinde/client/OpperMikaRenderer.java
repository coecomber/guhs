package nl.juiced.guhs.feature.guheinde.client;

import java.util.function.BiConsumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.OpperMikaEntity;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import org.jspecify.annotations.Nullable;

/**
 * Opper-Mika: the Mika model in his own dark colours, with the Knabbelkroon (its 3D item model) on his head.
 * (1.1.0: the crown's item render state is made at extract time and drawn on the head bone, like GuhRenderer's paper.)
 */
public class OpperMikaRenderer extends GeoEntityRenderer<OpperMikaEntity, LivingEntityRenderState> {
    private static final Identifier TEXTURE = Guhs.id("textures/entity/opper_mika.png");

    public OpperMikaRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<OpperMikaEntity>(Guhs.id("mika")) {
            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                return TEXTURE;
            }
        }.withAltAnimations(Guhs.id("guh")));
        this.shadowRadius = 0.8f;
        withRenderLayer(new CrownLayer(this));
    }

    /** The "head" bone follows where he looks (GeckoLib 4: DefaultedEntityGeoModel(id, true)). */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
    }

    /** The Knabbelkroon on the head, between the ears. */
    static class CrownLayer extends GeoRenderLayer<OpperMikaEntity, Void, LivingEntityRenderState> {
        private static final DataTicket<ItemStackRenderState> CROWN = DataTicket.create("guhs_opper_mika_crown", ItemStackRenderState.class);
        private @Nullable ItemStack crown;

        CrownLayer(GeoRenderer<OpperMikaEntity, Void, LivingEntityRenderState> renderer) {
            super(renderer);
        }

        @Override
        public void addRenderData(OpperMikaEntity mika, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            if (crown == null) {
                crown = new ItemStack(GuheindeFeature.KNABBELKROON.get());
            }
            ItemStackRenderState item = new ItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver().updateForLiving(item, crown, ItemDisplayContext.HEAD, mika);
            state.addGeckolibData(CROWN, item);
        }

        @Override
        public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> info, BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
            ItemStackRenderState item = info.getGeckolibData(CROWN);
            if (item == null || item.isEmpty() || !info.willRender()) {
                return;
            }
            info.model().getBone("head").ifPresent(head -> consumer.accept(head, (pass, bone, collector) -> {
                PoseStack pose = pass.poseStack();
                pose.translate(0, 0.62, -0.1);            // on top of the head, between the ears
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.scale(0.7f, 0.7f, 0.7f);
                item.submit(pose, collector, pass.packedLight(), OverlayTexture.NO_OVERLAY, pass.renderState().outlineColor);
            }));
        }
    }
}
