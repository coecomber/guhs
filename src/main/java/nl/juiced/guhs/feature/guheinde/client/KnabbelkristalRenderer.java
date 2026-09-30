package nl.juiced.guhs.feature.guheinde.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guheinde.KnabbelkristalEntity;

/**
 * A knabbelkristal: the end crystal model (spinning cubes on a base) with a cheese-and-knabbel texture, and its beam.
 * (1.1.0: vanilla's 26.1 EndCrystalRenderer with our texture; the entity is no EndCrystal any more, see KnabbelkristalEntity.)
 */
public class KnabbelkristalRenderer extends EntityRenderer<KnabbelkristalEntity, EndCrystalRenderState> {
    private static final Identifier TEXTURE = Guhs.id("textures/entity/knabbelkristal.png");
    private final EndCrystalModel model;

    public KnabbelkristalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
        this.model = new EndCrystalModel(context.bakeLayer(ModelLayers.END_CRYSTAL));
    }

    @Override
    public EndCrystalRenderState createRenderState() {
        return new EndCrystalRenderState();
    }

    @Override
    public void extractRenderState(KnabbelkristalEntity entity, EndCrystalRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.time + partialTicks;
        state.showsBottom = entity.showsBottom();
        BlockPos target = entity.getBeamTarget();
        state.beamOffset = target == null ? null : Vec3.atCenterOf(target).subtract(entity.getPosition(partialTicks));
    }

    @Override
    public void submit(EndCrystalRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.scale(2f, 2f, 2f);
        pose.translate(0f, -0.5f, 0f);
        collector.submitModel(model, state, pose, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        pose.popPose();
        Vec3 beam = state.beamOffset;
        if (beam != null) {
            float bob = EndCrystalRenderer.getY(state.ageInTicks);
            pose.pushPose();
            pose.translate(beam);
            EnderDragonRenderer.submitCrystalBeams(-(float) beam.x, -(float) beam.y + bob, -(float) beam.z, state.ageInTicks, pose, collector,
                    state.lightCoords);
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }

    @Override
    public boolean shouldRender(KnabbelkristalEntity entity, Frustum camera, double x, double y, double z) {
        return super.shouldRender(entity, camera, x, y, z) || entity.getBeamTarget() != null;
    }
}
