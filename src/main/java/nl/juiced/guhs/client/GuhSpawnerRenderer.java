package nl.juiced.guhs.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.block.entity.GuhSpawnerBlockEntity;

/**
 * The little spinning guh inside the spawner cage (same as vanilla spawners).
 * <p>
 * 1.1.0 (MC 26.1, reference block-entity renderer): {@link #extractRenderState} copies what is needed from the block
 * entity into a render state (here vanilla's {@link SpawnerRenderState}, the display entity as its own render state);
 * {@link #submit} draws from the state only.
 */
public class GuhSpawnerRenderer implements BlockEntityRenderer<GuhSpawnerBlockEntity, SpawnerRenderState> {
    private final EntityRenderDispatcher entityRenderer;

    public GuhSpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    @Override
    public SpawnerRenderState createRenderState() {
        return new SpawnerRenderState();
    }

    @Override
    public void extractRenderState(GuhSpawnerBlockEntity be, SpawnerRenderState state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.displayEntity = null;
        if (be.getLevel() == null) {
            return;
        }
        BaseSpawner spawner = be.getSpawner();
        Entity entity = spawner.getOrCreateDisplayEntity(be.getLevel(), be.getBlockPos());
        if (entity != null) {
            // (as vanilla TrialSpawnerRenderer.extractSpawnerData, which is package-private)
            state.displayEntity = entityRenderer.extractEntity(entity, partialTick);
            state.displayEntity.lightCoords = state.lightCoords;
            state.spin = (float) Mth.lerp(partialTick, spawner.getOSpin(), spawner.getSpin()) * 10.0F;
            state.scale = 0.53125F;
            float maxLength = Math.max(entity.getBbWidth(), entity.getBbHeight());
            if (maxLength > 1.0) {
                state.scale /= maxLength;
            }
        }
    }

    @Override
    public void submit(SpawnerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.displayEntity != null) {
            SpawnerRenderer.submitEntityInSpawner(poseStack, collector, state.displayEntity, entityRenderer, state.spin, state.scale, camera);
        }
    }
}
