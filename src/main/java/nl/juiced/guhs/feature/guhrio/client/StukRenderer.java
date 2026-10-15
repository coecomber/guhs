package nl.juiced.guhs.feature.guhrio.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhrio.GuhrioStukken;

/**
 * Draws the pieces that every player sees their own way: the ?-block (its ? or, once you bumped it, empty), the brick
 * (gone where you broke it), the hidden block (only once you found it), the coin and the big vadsmunt (turning; gone once
 * you took it, a shadow when you had it already), the switch (pressed while its channel is on for you) and the switched
 * blocks (solid or an outline), Guhshi's egg and Guhshi waiting. They hop when your head bumps them. The blocks
 * themselves are invisible in the world's own drawing ({@link GuhrioBlocks}); what you see is always this, with the state
 * your own game has for the piece ({@link GuhrioClient#staat}; outside a level: as built).
 */
public class StukRenderer implements BlockEntityRenderer<GuhrioBlocks.StukBlockEntity, StukRenderer.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
    /** How long and how high a bumped block hops. */
    public static final float HOP_TICKS = 7f, HOP = 0.3f;

    public static class State extends BlockEntityRenderState {
        final BlockModelRenderState blok = new BlockModelRenderState();
        final ItemStackRenderState ding = new ItemStackRenderState();
        boolean isDing;
        float hop, draai, schaal, hoog;
    }

    private final BlockModelResolver blokken;
    private final ItemModelResolver items;

    public StukRenderer(BlockEntityRendererProvider.Context context) {
        this.blokken = context.blockModelResolver();
        this.items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private void ding(State state, GuhrioBlocks.StukBlockEntity be, Item item, float schaal, float draai, float hoog) {
        state.isDing = true;
        state.schaal = schaal;
        state.draai = draai;
        state.hoog = hoog;
        items.updateForTopItem(state.ding, new ItemStack(item), ItemDisplayContext.FIXED, be.getLevel(), null, (int) be.getBlockPos().asLong());
    }

    @Override
    public void extractRenderState(GuhrioBlocks.StukBlockEntity be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.blok.clear();
        state.ding.clear();
        state.isDing = false;
        BlockState bs = be.getBlockState();
        Block blok = bs.getBlock();
        int staat = GuhrioClient.staat(be.getBlockPos());
        long nu = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        float sinds = (nu - be.bots) + partialTick;
        state.hop = sinds >= 0 && sinds < HOP_TICKS ? (float) Math.sin(sinds / HOP_TICKS * Math.PI) * HOP : 0f;
        float tol = ((nu % 40) + partialTick) * 9f;
        float dein = (float) Math.sin(((nu % 80) + partialTick) / 80.0 * Math.PI * 2) * 0.06f;
        if (blok instanceof GuhrioBlocks.VraagBlok) {
            blokken.update(state.blok, bs.setValue(GuhrioBlocks.LEEG, staat != 0), DISPLAY);
        } else if (blok instanceof GuhrioBlocks.SteenBlok) {
            if (staat == 0) {
                blokken.update(state.blok, bs, DISPLAY);
            }
        } else if (blok instanceof GuhrioStukken.OnzichtbaarBlok) {
            if (staat != 0) {
                blokken.update(state.blok, GuhrioFeature.VRAAGBLOK.get().defaultBlockState().setValue(GuhrioBlocks.LEEG, true), DISPLAY);
            }
        } else if (blok instanceof GuhrioStukken.SchakelaarBlok) {
            blokken.update(state.blok, bs.setValue(GuhrioStukken.SchakelaarBlok.INGEDRUKT, GuhrioClient.kanaal(bs.getValue(GuhrioStukken.KANAAL))), DISPLAY);
        } else if (blok instanceof GuhrioStukken.SchakelBlok) {
            boolean vast = GuhrioStukken.SchakelBlok.vast(bs, GuhrioClient.speelt() ? Minecraft.getInstance().player : null);
            blokken.update(state.blok, bs.setValue(GuhrioStukken.SchakelBlok.OPEN, !vast), DISPLAY);
        } else if (blok instanceof GuhrioBlocks.MuntBlok) {
            if (staat == 0) {
                ding(state, be, GuhrioFeature.MUNT.get().asItem(), 0.8f, tol, 0.5f);
            }
        } else if (blok instanceof GuhrioStukken.VadsmuntBlok) {
            if (staat == 0) {
                ding(state, be, GuhrioFeature.VADSMUNT.get().asItem(), 1.5f, tol * 0.6f, 0.55f + dein);
            } else if (staat == 2) {
                ding(state, be, GuhrioFeature.VADSMUNT_SCHIM.get(), 1.5f, tol * 0.6f, 0.55f);
            }
        } else if (blok instanceof GuhrioStukken.GuhshiEi) {
            if (staat == 0) {
                ding(state, be, GuhrioFeature.GUHSHI_EI.get().asItem(), 1.1f, 20f * (float) Math.sin(((nu % 30) + partialTick) / 30.0 * Math.PI * 2), 0.5f + dein);
            }
        } else if (blok instanceof GuhrioStukken.GuhshiPlek) {
            if (staat == 0) {
                ding(state, be, GuhrioFeature.GUHSHI_PLEK.get().asItem(), 1.4f, 0f, 0.6f + dein);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.isDing) {
            pose.pushPose();
            pose.translate(0.5, state.hoog, 0.5);
            // (a picture that does not turn by itself looks at the camera of your lane)
            pose.mulPose(Axis.YP.rotationDegrees(state.draai == 0f ? naarCamera() : state.draai));
            pose.scale(state.schaal, state.schaal, state.schaal);
            state.ding.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        } else if (!state.blok.isEmpty()) {
            pose.pushPose();
            pose.translate(0, state.hop, 0);
            state.blok.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    private static float naarCamera() {
        var baan = GuhrioClient.baan();
        if (baan == null) {
            return 0f;
        }
        Vec3 c = baan.naarCamera(BaanBesturing.stukNu());
        return (float) Math.toDegrees(Math.atan2(c.x, c.z));
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(GuhrioBlocks.StukBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.6);
    }
}
