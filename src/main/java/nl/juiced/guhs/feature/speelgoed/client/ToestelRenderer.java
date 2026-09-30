package nl.juiced.guhs.feature.speelgoed.client;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.speelgoed.SchommelBlock;
import nl.juiced.guhs.feature.speelgoed.ToestelBlock;
import nl.juiced.guhs.feature.speelgoed.ToestelBlockEntity;
import nl.juiced.guhs.feature.speelgoed.WipBlock;

/**
 * The moving part of a wip (the plank, model guh_wip_plank) or a schommel (the seat on its ropes, model
 * guh_schommel_zitje): drawn turned around its pivot by the swing angle of the {@link ToestelBlockEntity} (the same
 * angle the seats use, so riders stay on it). The models are in the toy's local pixels, facing north.
 * <p>
 * 1.1.0: the two models are NeoForge standalone models, the angle is taken at extract time (render state).
 */
public class ToestelRenderer implements BlockEntityRenderer<ToestelBlockEntity, ToestelRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> PLANK = new StandaloneModelKey<>(() -> "guhs:block/guh_wip_plank");
    public static final StandaloneModelKey<BlockStateModelPart> ZITJE = new StandaloneModelKey<>(() -> "guhs:block/guh_schommel_zitje");

    public static class State extends BlockEntityRenderState {
        @Nullable
        StandaloneModelKey<BlockStateModelPart> model;
        Vec3 spil = Vec3.ZERO;
        Direction facing = Direction.NORTH;
        float hoek;
    }

    public ToestelRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Mod bus (client): the plank and the seat, which are no block state of their own. */
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(PLANK, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/guh_wip_plank")));
        event.register(ZITJE, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/guh_schommel_zitje")));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ToestelBlockEntity be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        BlockState block = be.getBlockState();
        state.model = null;
        if (block.getBlock() instanceof WipBlock) {
            state.model = PLANK;
            state.spil = WipBlock.SPIL;
        } else if (block.getBlock() instanceof SchommelBlock) {
            state.model = ZITJE;
            state.spil = SchommelBlock.SPIL;
        } else {
            return;
        }
        float tijd = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partialTick;
        state.hoek = be.hoek(tijd);
        state.facing = block.getValue(ToestelBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.model == null) {
            return;
        }
        Vec3 spil = state.spil;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180f - state.facing.toYRot()));
        pose.translate(-0.5, 0, -0.5);
        pose.translate(spil.x, spil.y, spil.z);
        pose.mulPose(Axis.XP.rotation(-state.hoek));
        pose.translate(-spil.x, -spil.y, -spil.z);
        collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(state.model)),
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ToestelBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 2.5, 0);
    }
}
