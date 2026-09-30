package nl.juiced.guhs.feature.guheinde.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guheinde.KnabbelkristalEntity;
import org.joml.Quaternionf;

import net.minecraft.client.renderer.rendertype.RenderTypes;
/** A knabbelkristal: the end crystal model (spinning cubes on a base) with a cheese-and-knabbel texture, and its beam. */
public class KnabbelkristalRenderer extends EntityRenderer<KnabbelkristalEntity> {
    private static final Identifier TEXTURE = Guhs.id("textures/entity/knabbelkristal.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
    private static final float SIN_45 = (float) Math.sin(Math.PI / 4);
    private final ModelPart cube, glass, base;

    public KnabbelkristalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
        ModelPart part = context.bakeLayer(ModelLayers.END_CRYSTAL);
        this.glass = part.getChild("glass");
        this.cube = part.getChild("cube");
        this.base = part.getChild("base");
    }

    @Override
    public void render(KnabbelkristalEntity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        float bob = EndCrystalRenderer.getY(entity, partialTicks);
        float spin = (entity.time + partialTicks) * 3f;
        VertexConsumer vc = buffer.getBuffer(RENDER_TYPE);
        pose.pushPose();
        pose.scale(2f, 2f, 2f);
        pose.translate(0f, -0.5f, 0f);
        int overlay = OverlayTexture.NO_OVERLAY;
        if (entity.showsBottom()) {
            base.render(pose, vc, light, overlay);
        }
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.translate(0f, 1.5f + bob / 2f, 0f);
        pose.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0f, SIN_45));
        glass.render(pose, vc, light, overlay);
        pose.scale(0.875f, 0.875f, 0.875f);
        pose.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0f, SIN_45));
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        glass.render(pose, vc, light, overlay);
        pose.scale(0.875f, 0.875f, 0.875f);
        pose.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0f, SIN_45));
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        cube.render(pose, vc, light, overlay);
        pose.popPose();
        pose.popPose();
        BlockPos target = entity.getBeamTarget();
        if (target != null) {
            float dx = (float) (target.getX() + 0.5 - entity.getX());
            float dy = (float) (target.getY() + 0.5 - entity.getY());
            float dz = (float) (target.getZ() + 0.5 - entity.getZ());
            pose.pushPose();
            pose.translate(dx, dy, dz);
            EnderDragonRenderer.renderCrystalBeams(-dx, -dy + bob, -dz, partialTicks, entity.time, pose, buffer, light);
            pose.popPose();
        }
        super.render(entity, yaw, partialTicks, pose, buffer, light);
    }

    @Override
    public Identifier getTextureLocation(KnabbelkristalEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(KnabbelkristalEntity entity, Frustum camera, double x, double y, double z) {
        return super.shouldRender(entity, camera, x, y, z) || entity.getBeamTarget() != null;
    }
}
