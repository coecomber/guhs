package nl.juiced.guhs.feature.guhrio.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioFeature;

/**
 * Draws the pieces that every player sees their own way: the ?-block (its ? or, once you bumped it, empty), the brick
 * (gone where you broke it) and the coin (turning; gone once you took it). They hop when your head bumps them. The blocks
 * themselves are invisible in the world's own drawing ({@link GuhrioBlocks}); what you see is always this, with the state
 * your own game has for the piece ({@link GuhrioClient#staat}; outside a level: as built).
 */
public class StukRenderer implements BlockEntityRenderer<GuhrioBlocks.StukBlockEntity, StukRenderer.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
    /** How long and how high a bumped block hops. */
    public static final float HOP_TICKS = 7f, HOP = 0.3f;

    public static class State extends BlockEntityRenderState {
        final BlockModelRenderState blok = new BlockModelRenderState();
        final ItemStackRenderState munt = new ItemStackRenderState();
        boolean isMunt;
        float hop, draai;
    }

    private final BlockModelResolver blokken;
    private final ItemModelResolver items;
    private final ItemStack muntStack = new ItemStack(GuhrioFeature.MUNT.get());

    public StukRenderer(BlockEntityRendererProvider.Context context) {
        this.blokken = context.blockModelResolver();
        this.items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GuhrioBlocks.StukBlockEntity be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.blok.clear();
        state.munt.clear();
        state.isMunt = false;
        BlockState bs = be.getBlockState();
        int staat = GuhrioClient.staat(be.getBlockPos());
        long nu = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        float sinds = (nu - be.bots) + partialTick;
        state.hop = sinds >= 0 && sinds < HOP_TICKS ? (float) Math.sin(sinds / HOP_TICKS * Math.PI) * HOP : 0f;
        if (bs.getBlock() instanceof GuhrioBlocks.VraagBlok) {
            blokken.update(state.blok, bs.setValue(GuhrioBlocks.LEEG, staat != 0), DISPLAY);
        } else if (bs.getBlock() instanceof GuhrioBlocks.SteenBlok) {
            if (staat == 0) {
                blokken.update(state.blok, bs, DISPLAY);
            }
        } else if (bs.getBlock() instanceof GuhrioBlocks.MuntBlok && staat == 0) {
            state.isMunt = true;
            state.draai = ((nu % 40) + partialTick) * 9f;
            items.updateForTopItem(state.munt, muntStack, ItemDisplayContext.FIXED, be.getLevel(), null, (int) be.getBlockPos().asLong());
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.isMunt) {
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(state.draai));
            pose.scale(0.8f, 0.8f, 0.8f);
            state.munt.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        } else if (!state.blok.isEmpty()) {
            pose.pushPose();
            pose.translate(0, state.hop, 0);
            state.blok.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(GuhrioBlocks.StukBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.4);
    }
}
