package nl.juiced.guhs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import nl.juiced.guhs.block.entity.GuhSpawnerBlockEntity;

/** The little spinning guh inside the spawner cage (same as vanilla spawners). */
public class GuhSpawnerRenderer implements BlockEntityRenderer<GuhSpawnerBlockEntity> {
    private final EntityRenderDispatcher entityRenderer;

    public GuhSpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(GuhSpawnerBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (be.getLevel() == null) {
            return;
        }
        BaseSpawner spawner = be.getSpawner();
        Entity entity = spawner.getOrCreateDisplayEntity(be.getLevel(), be.getBlockPos());
        if (entity != null) {
            SpawnerRenderer.renderEntityInSpawner(partialTick, poseStack, buffer, packedLight, entity, entityRenderer,
                    spawner.getoSpin(), spawner.getSpin());
        }
    }
}
