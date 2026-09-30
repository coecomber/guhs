package nl.juiced.guhs.feature.wereldleven.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.wereldleven.GrijpmachineBlockEntity;
import nl.juiced.guhs.feature.wereldleven.WereldlevenFeature;

/**
 * Draws what's inside the grijpmachine's glass case: the little plushies on its floor and the claw drifting around above
 * them (models/block/grijpmachine_klauw.json). The cabinet itself is the block model.
 * <p>
 * 1.1.0: the claw is a standalone model; the plushies are block models resolved at extract time (like a minecart's
 * block) and submitted with the light inside the case.
 */
public class GrijpmachineRenderer implements BlockEntityRenderer<GrijpmachineBlockEntity, GrijpmachineRenderer.State> {
    public static final StandaloneModelKey<BlockStateModelPart> KLAUW = new StandaloneModelKey<>(() -> "guhs:block/grijpmachine_klauw");
    /** The case (in model pixels): floor at y 16 (the top of the lower block), inside x/z 2..14, the claw's rail at y 27. */
    public static final float VLOER = 16.2f / 16f, BINNEN_MIN = 2.5f / 16f, BINNEN = 11f / 16f, RAIL = 26.5f / 16f;
    public static final float KNUFFEL_SCHAAL = 0.3f;
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    /** One plushie on the floor of the case. */
    static final class Knuffel {
        final BlockModelRenderState model = new BlockModelRenderState();
        float x, z;
        int index;
    }

    public static class State extends BlockEntityRenderState {
        boolean zichtbaar;
        Direction facing = Direction.NORTH;
        int binnenLicht;
        float klauwX, klauwZ;
        final List<Knuffel> knuffels = new ArrayList<>();
        int aantal;
    }

    private final BlockModelResolver blockModels;

    public GrijpmachineRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModels = context.blockModelResolver();
    }

    /** Mod bus (client): the claw, which is no block state of its own. */
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        event.register(KLAUW, SimpleUnbakedStandaloneModel.simpleModelWrapper(Guhs.id("block/grijpmachine_klauw")));
    }

    /** Model x (0..1, the front is north / z = 0) of a case x (0 = left, 1 = right, seen from the front). */
    public static float modelX(float x) {
        return BINNEN_MIN + (1f - x) * BINNEN;
    }

    /** Model z of a case z (0 = back, 1 = front). */
    public static float modelZ(float z) {
        return BINNEN_MIN + (1f - z) * BINNEN;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GrijpmachineBlockEntity machine, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(machine, state, partialTick, camera, breakProgress);
        BlockState block = machine.getBlockState();
        state.zichtbaar = block.hasProperty(HorizontalDirectionalBlock.FACING) && machine.getLevel() != null && !machine.isBoven();
        state.aantal = 0;
        if (!state.zichtbaar) {
            return;
        }
        state.facing = block.getValue(HorizontalDirectionalBlock.FACING);
        state.binnenLicht = LevelRenderer.getLightCoords(machine.getLevel(), machine.getBlockPos().above());
        // the plushies on the floor of the case
        List<GrijpmachineBlockEntity.Prijs> prijzen = machine.prijzen();
        for (int i = 0; i < prijzen.size(); i++) {
            GrijpmachineBlockEntity.Prijs p = prijzen.get(i);
            Block knuffel = WereldlevenFeature.knuffel(p.knuffel());
            if (knuffel == null) {
                continue;
            }
            if (state.knuffels.size() <= state.aantal) {
                state.knuffels.add(new Knuffel());
            }
            Knuffel k = state.knuffels.get(state.aantal++);
            k.x = p.x();
            k.z = p.z();
            k.index = i;
            blockModels.update(k.model, knuffel.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), DISPLAY);
        }
        // the claw, drifting slowly over the plushies (waiting for someone with a ticket)
        float t = (machine.getLevel().getGameTime() + partialTick) * 0.02f + machine.getBlockPos().hashCode() % 100;
        state.klauwX = 0.5f + 0.32f * (float) Math.sin(t);
        state.klauwZ = 0.5f + 0.3f * (float) Math.cos(t * 0.73f);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.zichtbaar) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(state.facing.toYRot() + 180f)));
        pose.translate(-0.5, 0, -0.5);
        // the plushies on the floor of the case, each turned a little
        for (int n = 0; n < state.aantal; n++) {
            Knuffel k = state.knuffels.get(n);
            int i = k.index;
            pose.pushPose();
            pose.translate(modelX(k.x), VLOER, modelZ(k.z));
            pose.mulPose(Axis.YP.rotationDegrees(((i * 47) % 70) - 35f));
            pose.mulPose(Axis.XP.rotationDegrees(((i * 29) % 3 - 1) * 8f));
            pose.scale(KNUFFEL_SCHAAL, KNUFFEL_SCHAAL, KNUFFEL_SCHAAL);
            pose.translate(-0.5, 0, -0.5);
            k.model.submitMultiLayer(pose, collector, state.binnenLicht, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        // the claw
        pose.pushPose();
        pose.translate(modelX(state.klauwX) - 0.5, RAIL - 1f, modelZ(state.klauwZ) - 0.5);
        collector.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(Minecraft.getInstance().getModelManager().getStandaloneModel(KLAUW)),
                BlockModelRenderState.EMPTY_TINTS, state.binnenLicht, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        pose.popPose();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(GrijpmachineBlockEntity machine) {
        return new net.minecraft.world.phys.AABB(machine.getBlockPos()).expandTowards(0, 1, 0);
    }
}
