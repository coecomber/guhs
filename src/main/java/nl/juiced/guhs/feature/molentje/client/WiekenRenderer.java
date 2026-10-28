package nl.juiced.guhs.feature.molentje.client;

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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.molentje.MolenBlokken;

/**
 * The four big sails of the Maanmolentje (models/block/molenwieken_draai.json, scaled up {@link #SCHAAL} times around the
 * hub): still at a slant, or turning while the mill grinds ({@link MolenBlokken.Wieken#DRAAIT}).
 */
public class WiekenRenderer implements BlockEntityRenderer<MolenBlokken.WiekenBlockEntity, WiekenRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> DRAAI = new StandaloneModelKey<>(() -> "guhs:block/molenwieken_draai");
    /** How much bigger the sails are drawn than their model (arms of 1.5 blocks become about 10.5). */
    public static final float SCHAAL = 7f;
    /** Degrees per tick while turning; the slant when still. */
    public static final float SNEL = 2.2f, STIL = 15f;

    public static class State extends BlockEntityRenderState {
        float hoek;
        float yaw;
    }

    public WiekenRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(DRAAI, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/molenwieken_draai")));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MolenBlokken.WiekenBlockEntity wieken, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(wieken, state, partialTick, camera, breakProgress);
        var bs = wieken.getBlockState();
        boolean draait = bs.hasProperty(MolenBlokken.Wieken.DRAAIT) && bs.getValue(MolenBlokken.Wieken.DRAAIT);
        long t = wieken.getLevel() == null ? 0 : wieken.getLevel().getGameTime();
        state.hoek = draait ? ((t % 3600) + partialTick) * SNEL % 360f : STIL;
        Direction f = bs.hasProperty(HorizontalDirectionalBlock.FACING) ? bs.getValue(HorizontalDirectionalBlock.FACING) : Direction.SOUTH;
        state.yaw = f.toYRot();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        pose.mulPose(Axis.ZP.rotationDegrees(state.hoek));
        pose.scale(SCHAAL, SCHAAL, SCHAAL);
        pose.translate(-0.5, -0.5, -0.5);
        collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(DRAAI)),
                BlockModelRenderState.EMPTY_TINTS, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MolenBlokken.WiekenBlockEntity wieken) {
        return new AABB(wieken.getBlockPos()).inflate(12);
    }
}
