package nl.juiced.guhs.feature.wereldleven.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.wereldleven.GrijpmachineBlockEntity;
import nl.juiced.guhs.feature.wereldleven.WereldlevenFeature;

/**
 * Draws what's inside the grijpmachine's glass case: the little plushies on its floor and the claw drifting around above
 * them (models/block/grijpmachine_klauw.json). The cabinet itself is the block model.
 */
public class GrijpmachineRenderer implements BlockEntityRenderer<GrijpmachineBlockEntity> {
    public static final ModelResourceLocation KLAUW = ModelResourceLocation.standalone(Guhs.id("block/grijpmachine_klauw"));
    /** The case (in model pixels): floor at y 16 (the top of the lower block), inside x/z 2..14, the claw's rail at y 27. */
    public static final float VLOER = 16.2f / 16f, BINNEN_MIN = 2.5f / 16f, BINNEN = 11f / 16f, RAIL = 26.5f / 16f;
    public static final float KNUFFEL_SCHAAL = 0.3f;

    public GrijpmachineRenderer(BlockEntityRendererProvider.Context context) {
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
    public void render(GrijpmachineBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = machine.getBlockState();
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING) || machine.getLevel() == null || machine.isBoven()) {
            return;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        int binnenLicht = LevelRenderer.getLightColor(machine.getLevel(), machine.getBlockPos().above());
        var blocks = Minecraft.getInstance().getBlockRenderer();
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + 180f)));
        pose.translate(-0.5, 0, -0.5);
        // the plushies on the floor of the case, each turned a little
        List<GrijpmachineBlockEntity.Prijs> prijzen = machine.prijzen();
        for (int i = 0; i < prijzen.size(); i++) {
            GrijpmachineBlockEntity.Prijs p = prijzen.get(i);
            Block block = WereldlevenFeature.knuffel(p.knuffel());
            if (block == null) {
                continue;
            }
            pose.pushPose();
            pose.translate(modelX(p.x()), VLOER, modelZ(p.z()));
            pose.mulPose(Axis.YP.rotationDegrees(((i * 47) % 70) - 35f));
            pose.mulPose(Axis.XP.rotationDegrees(((i * 29) % 3 - 1) * 8f));
            pose.scale(KNUFFEL_SCHAAL, KNUFFEL_SCHAAL, KNUFFEL_SCHAAL);
            pose.translate(-0.5, 0, -0.5);
            BlockState knuffel = block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
            blocks.renderSingleBlock(knuffel, pose, buffers, binnenLicht, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        // the claw, drifting slowly over the plushies (waiting for someone with a ticket)
        float t = (machine.getLevel().getGameTime() + partialTick) * 0.02f + machine.getBlockPos().hashCode() % 100;
        float x = 0.5f + 0.32f * (float) Math.sin(t), z = 0.5f + 0.3f * (float) Math.cos(t * 0.73f);
        BakedModel klauw = Minecraft.getInstance().getModelManager().getModel(KLAUW);
        pose.pushPose();
        pose.translate(modelX(x) - 0.5, RAIL - 1f, modelZ(z) - 0.5);
        blocks.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), state, klauw, 1f, 1f, 1f, binnenLicht,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.popPose();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(GrijpmachineBlockEntity machine) {
        return new net.minecraft.world.phys.AABB(machine.getBlockPos()).expandTowards(0, 1, 0);
    }
}
