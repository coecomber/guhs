package nl.juiced.guhs.feature.hemel.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.hemel.KnuffelhartBlockEntity;

/**
 * The Knuffelhart itself (models/block/knuffelhart_hart.json) floating under its glass dome, full bright, turning gently to
 * and fro and bobbing. Once it beats for you it goes "ba-dum" ({@link #SLAG} ticks per beat, two bumps) with a soft pink
 * glow around it (knuffelhart_gloed.json); while it still sleeps it only breathes slowly, a little smaller.
 */
public class KnuffelhartRenderer implements BlockEntityRenderer<KnuffelhartBlockEntity> {
    public static final ModelResourceLocation HART = ModelResourceLocation.standalone(Guhs.id("block/knuffelhart_hart"));
    public static final ModelResourceLocation GLOED = ModelResourceLocation.standalone(Guhs.id("block/knuffelhart_gloed"));
    /** Ticks per heartbeat (about 50 beats a minute: a calm, happy heart). */
    public static final int SLAG = 24;
    /** The middle of the heart in model pixels. */
    private static final float MX = 8.5f / 16f, MY = 9f / 16f, MZ = 8f / 16f;

    public KnuffelhartRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** The heart's size now: ba-dum (awake) or slow breathing (asleep). */
    public static float klop(float tijd, boolean wakker) {
        if (!wakker) {
            return 0.9f + 0.03f * Mth.sin(tijd * 0.08f);
        }
        float f = (tijd % SLAG) / SLAG;
        float ba = (float) Math.exp(-Math.pow((f - 0.05f) / 0.05f, 2)) * 0.16f;
        float dum = (float) Math.exp(-Math.pow((f - 0.25f) / 0.06f, 2)) * 0.1f;
        return 1f + ba + dum;
    }

    @Override
    public void render(KnuffelhartBlockEntity hart, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        boolean wakker = HemelClient.klopt();
        float tijd = hart.tijd + partialTick;
        float schaal = klop(tijd, wakker);
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(MX, MY + Mth.sin(tijd * 0.05f) * 0.03f, MZ);
        pose.mulPose(Axis.YP.rotationDegrees(Mth.sin(tijd * 0.03f) * 28f));
        pose.scale(schaal, schaal, schaal);
        pose.translate(-MX, -MY, -MZ);
        BakedModel model = mc.getModelManager().getModel(HART);
        float kleur = wakker ? 1f : 0.82f;
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.cutout()), hart.getBlockState(), model,
                kleur, kleur, kleur, LightCoordsUtil.FULL_BRIGHT, overlay);
        if (wakker) {
            float gloed = 1f + (schaal - 1f) * 1.8f;
            pose.translate(MX, MY, MZ);
            pose.scale(gloed, gloed, gloed);
            pose.translate(-MX, -MY, -MZ);
            mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.translucent()), hart.getBlockState(),
                    mc.getModelManager().getModel(GLOED), 1f, 1f, 1f, LightCoordsUtil.FULL_BRIGHT, overlay);
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(KnuffelhartBlockEntity hart) {
        return new AABB(hart.getBlockPos()).inflate(0.5);
    }
}
