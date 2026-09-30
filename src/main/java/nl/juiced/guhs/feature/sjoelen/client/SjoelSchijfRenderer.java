package nl.juiced.guhs.feature.sjoelen.client;

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
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.sjoelen.SjoelSchijfEntity;

/** A sjoelschijf: a round wooden puck (two crossed boxes) with a little guh face on top, turning as it slides. */
public class SjoelSchijfRenderer extends EntityRenderer<SjoelSchijfEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Guhs.id("sjoelschijf"), "main");
    private static final Identifier HOUT = Guhs.id("textures/entity/sjoelschijf.png"), ROZE = Guhs.id("textures/entity/sjoelschijf_roze.png");
    private final ModelPart puck;

    public SjoelSchijfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.puck = context.bakeLayer(LAYER);
        this.shadowRadius = 0.18f;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("puck", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5f, -2f, -2.5f, 7, 2, 5)
                .texOffs(0, 8).addBox(-2.5f, -1.8f, -3.5f, 5, 1.8f, 7), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public void render(SjoelSchijfEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(entity.draai()));
        pose.scale(-1, -1, 1);
        puck.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public Identifier getTextureLocation(SjoelSchijfEntity entity) {
        return entity.kleur() == 1 ? ROZE : HOUT;
    }
}
