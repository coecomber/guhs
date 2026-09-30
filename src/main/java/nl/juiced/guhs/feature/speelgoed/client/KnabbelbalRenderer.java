package nl.juiced.guhs.feature.speelgoed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.speelgoed.KnabbelbalEntity;

/**
 * The knabbelbal: a round fluffy pink guh ball (a core with three rounded-out slabs), two little guh ears, a guh face on
 * the front and a round window on its tummy with the kaasknabbel inside (textures knabbelbal / knabbelbal_leeg, made by
 * tools/features/speelgoed.py). It rolls the way it goes.
 */
public class KnabbelbalRenderer extends EntityRenderer<KnabbelbalEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Guhs.id("knabbelbal"), "main");
    private static final Identifier VOL = Guhs.id("textures/entity/knabbelbal.png");
    private static final Identifier LEEG = Guhs.id("textures/entity/knabbelbal_leeg.png");
    private final ModelPart bal;

    public KnabbelbalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.bal = context.bakeLayer(LAYER);
        this.shadowRadius = 0.22f;
    }

    /** Texture 64x64: the core at (0,0), the three slabs at (0,16), (0,32), (32,16) (6x6x9 each way), the ears at (48,0) and (56,0). */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("bal", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8)
                .texOffs(0, 16).addBox(-3, -3, -4.5f, 6, 6, 9)
                .texOffs(0, 32).addBox(-4.5f, -3, -3, 9, 6, 6)
                .texOffs(32, 16).addBox(-3, -4.5f, -3, 6, 9, 6)
                .texOffs(48, 0).addBox(-3.5f, -6f, -1, 2, 2, 2)
                .texOffs(56, 0).addBox(1.5f, -6f, -1, 2, 2, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(KnabbelbalEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, KnabbelbalEntity.SIZE / 2, 0);
        pose.mulPose(Axis.YP.rotation(entity.rollYaw));
        pose.mulPose(Axis.XP.rotation(Mth.lerp(partialTick, entity.oRoll, entity.roll)));
        pose.scale(-1, -1, 1);
        float s = KnabbelbalEntity.SIZE / 0.5625f;
        pose.scale(s, s, s);
        bal.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public Identifier getTextureLocation(KnabbelbalEntity entity) {
        return entity.isVol() ? VOL : LEEG;
    }
}
