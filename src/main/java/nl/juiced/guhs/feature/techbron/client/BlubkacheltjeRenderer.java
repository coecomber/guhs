package nl.juiced.guhs.feature.techbron.client;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.techbron.BlubkacheltjeBlockEntity;
import nl.juiced.guhs.feature.techbron.BronBlock;

/**
 * The Sausblubje in the jar of a Blubkacheltje (the model {@code blubkacheltje_blubje}: a little blob of sauce with a
 * face). A warm blubje bobs up and down, squashing when it lands, and glows; a hungry one sits flat on the bottom of the
 * jar, sulking. The stove and the jar are the block's own model.
 */
public class BlubkacheltjeRenderer implements BlockEntityRenderer<BlubkacheltjeBlockEntity, BlubkacheltjeRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> BLUBJE = new StandaloneModelKey<>(() -> "guhs:block/blubkacheltje_blubje");
    /** The bottom of the jar (model pixels) and how high a warm blubje hops. */
    private static final float BODEM = 8.2f / 16f, HOP = 2.5f / 16f;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        boolean blubje, warm;
        float fase;
    }

    public BlubkacheltjeRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Mod bus (client): the blubje, which is no block state of its own. */
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(BLUBJE, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/blubkacheltje_blubje")));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlubkacheltjeBlockEntity kachel, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(kachel, state, partialTick, camera, breakProgress);
        state.facing = kachel.getBlockState().getValue(BronBlock.FACING);
        state.blubje = kachel.heeftBlubje();
        state.warm = kachel.warm();
        long tijd = kachel.getLevel() == null ? 0 : kachel.getLevel().getGameTime() + kachel.getBlockPos().asLong() % 40;
        state.fase = ((tijd % 16) + partialTick) / 16f;   // one hop per 16 ticks
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.blubje) {
            return;
        }
        float hoogte = 0f, plat = 0.6f, breed = 1.2f;
        if (state.warm) {
            float sprong = (float) Math.sin(state.fase * Math.PI);        // 0 on the bottom .. 1 at the top
            hoogte = sprong * HOP;
            plat = 0.8f + 0.35f * sprong;                                 // squashed when it lands, stretched in the air
            breed = 1.1f - 0.15f * sprong;
        }
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180f));
        pose.translate(0, BODEM + hoogte, 0);
        pose.scale(breed, plat, breed);
        pose.translate(-0.5, 0, -0.5);                                     // (the model stands on y 0, around the middle of the block)
        collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(BLUBJE)),
                BlockModelRenderState.EMPTY_TINTS, state.warm ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
