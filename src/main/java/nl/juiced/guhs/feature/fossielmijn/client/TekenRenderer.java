package nl.juiced.guhs.feature.fossielmijn.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.fossielmijn.FossielmijnBlocks;

/**
 * Draws the two blocks of the Fossiel-opgraving that every player sees their own way: the bottenzand (brushed empty where
 * YOU brushed) and the stand (with the bones YOU put on it). The blocks themselves are invisible in the world's own
 * drawing ({@link FossielmijnBlocks.Getekend}); what you see is always this: the block's model for the state your own
 * progress gives it ({@link FossielmijnClient}). Nothing in the world changes, so any number of players dig side by side.
 */
public class TekenRenderer implements BlockEntityRenderer<FossielmijnBlocks.TekenBlockEntity, TekenRenderer.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    public static class State extends BlockEntityRenderState {
        final BlockModelRenderState blok = new BlockModelRenderState();
    }

    private final BlockModelResolver blokken;

    public TekenRenderer(BlockEntityRendererProvider.Context context) {
        this.blokken = context.blockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FossielmijnBlocks.TekenBlockEntity be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.blok.clear();
        BlockState bs = be.getBlockState();
        if (bs.getBlock() instanceof FossielmijnBlocks.Bottenzand) {
            blokken.update(state.blok, bs.setValue(FossielmijnBlocks.Bottenzand.LEEG, FossielmijnClient.isGekwast(be.getBlockPos())), DISPLAY);
        } else if (bs.getBlock() instanceof FossielmijnBlocks.Skeletrek) {
            blokken.update(state.blok, FossielmijnBlocks.Skeletrek.metDelen(bs, FossielmijnClient.rek()), DISPLAY);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.blok.isEmpty()) {
            state.blok.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    /** (the little skeleton sticks out of the stand's block: a block and a half to every side, nearly two up) */
    @Override
    public AABB getRenderBoundingBox(FossielmijnBlocks.TekenBlockEntity be) {
        return be.getBlockState().getBlock() instanceof FossielmijnBlocks.Skeletrek ? new AABB(be.getBlockPos()).inflate(1.6, 1.0, 1.6)
                : new AABB(be.getBlockPos());
    }
}
