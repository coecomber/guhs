package nl.juiced.guhs.feature.guhpolder.client;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpolder.MolentjeBlock;
import nl.juiced.guhs.feature.guhpolder.MolentjeBlockEntity;

/**
 * The guh-molentje's sails (models/block/guh_molentje_wieken.json), turning around the hub in front of the cap. The
 * little mill itself (with its guh face and ears) is the normal block model.
 * <p>
 * 1.1.0: the sails are a NeoForge standalone model, submitted with the cutout block sheet (was {@code RenderType.cutout()}).
 */
public class MolentjeRenderer implements BlockEntityRenderer<MolentjeBlockEntity, MolentjeRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> WIEKEN = new StandaloneModelKey<>(() -> "guhs:block/guh_molentje_wieken");
    /** The hub, in model pixels (see guh_molentje_wieken.json). */
    public static final float HUB_Y = 10f / 16f, HUB_Z = 2.5f / 16f;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float hoek;
    }

    public MolentjeRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Mod bus (client): the sails, which are no block state of their own. */
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(WIEKEN, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/guh_molentje_wieken")));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MolentjeBlockEntity molen, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(molen, state, partialTick, camera, breakProgress);
        state.facing = molen.getBlockState().getValue(MolentjeBlock.FACING);
        state.hoek = molen.hoek(partialTick);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot() + 180f));
        pose.translate(-0.5, 0, -0.5);
        pose.translate(0.5, HUB_Y, HUB_Z);
        pose.mulPose(Axis.ZP.rotationDegrees(state.hoek));
        pose.translate(-0.5, -HUB_Y, -HUB_Z);
        collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(WIEKEN)),
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MolentjeBlockEntity molen) {
        return new AABB(molen.getBlockPos()).inflate(0.5);
    }
}
