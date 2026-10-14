package nl.juiced.guhs.feature.ringvlucht.client;

import javax.annotation.Nullable;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ARGB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.ringvlucht.RingVluchtFeature;
import nl.juiced.guhs.feature.ringvlucht.VluchtRookguhEntity;

/**
 * Client side of 1.4.2 (rookguh): the Reis-Rookguh's renderer (the Rookguh's own model, texture and animations,
 * {@link RookguhRenderer#SCHAAL} blocks big, round and rosy with its blushing cheeks: well fed) and its screen
 * ({@link VluchtScherm}).
 */
public final class RingVluchtClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(RingVluchtFeature.ROOKGUH.get(), RookguhRenderer::new));
    }

    /** (VluchtPayloads.Open) the Rookguh asks where to go. */
    public static void open(int rookguh, CompoundTag data) {
        Minecraft.getInstance().setScreen(new VluchtScherm(rookguh, data));
    }

    static class RookguhRenderer extends GeoEntityRenderer<VluchtRookguhEntity, LivingEntityRenderState> {
        /** How big the body is drawn (blocks; the wild Rookguh: 3.375). */
        static final float SCHAAL = 2.0f;

        RookguhRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<VluchtRookguhEntity>(Guhs.id("rookguh")));
            this.shadowRadius = 0.8f;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            bones.ifPresent("rook", b -> {
                b.setScaleX(1.08f);
                b.setScaleZ(1.08f);
                b.setScaleY(1.04f);
            });
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            super.scaleModelForRender(info, widthScale * SCHAAL, heightScale * SCHAAL);
        }

        @Override
        public int getRenderColor(VluchtRookguhEntity guh, @Nullable Void related, float partialTick) {
            return ARGB.colorFromFloat(1.0f, 1.0f, 0.85f, 0.92f);
        }
    }

    private RingVluchtClient() {
    }
}
