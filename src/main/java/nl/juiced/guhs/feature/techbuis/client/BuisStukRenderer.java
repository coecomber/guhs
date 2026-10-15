package nl.juiced.guhs.feature.techbuis.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.techbuis.BuisStukBlockEntity;
import nl.juiced.guhs.feature.techbuis.Buizen;

/**
 * Draws the items that roll through the Knabbelbuizen. The renderer hangs on the Richtingstuk / Filterstuk that sent them
 * (the tubes have no block entity), and draws every ride of that piece ({@link BuisRitten}) wherever it is by now: small,
 * tumbling, lit by the light of the tube it is in. An item whose tube was broken is not drawn any more (the server brings
 * it back to the piece by itself).
 */
public class BuisStukRenderer implements BlockEntityRenderer<BuisStukBlockEntity, BuisStukRenderer.State> {
    /** How big an item in a tube is (an item in an item frame = 1). */
    private static final float MAAT = 0.5f;

    /** One item to draw: where (from the piece's corner), how it is turned, how light it is. */
    public static class Ding {
        final ItemStackRenderState item = new ItemStackRenderState();
        double x, y, z;
        float draai;
        int licht;
    }

    public static class State extends BlockEntityRenderState {
        final List<Ding> dingen = new ArrayList<>();
        int aantal;

        Ding volgende() {
            if (aantal == dingen.size()) {
                dingen.add(new Ding());
            }
            return dingen.get(aantal++);
        }
    }

    private final ItemModelResolver items;

    public BuisStukRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BuisStukBlockEntity be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.aantal = 0;
        Level level = be.getLevel();
        List<BuisRitten.Rit> ritten = BuisRitten.van(be.getBlockPos());
        if (level == null || ritten.isEmpty()) {
            return;
        }
        BlockPos hier = be.getBlockPos();
        float nu = level.getGameTime() + partialTick;
        for (BuisRitten.Rit rit : ritten) {
            float t = rit.voortgang(nu);
            if (t < 0 || t >= rit.punten.length - 1) {
                continue;
            }
            int i = (int) t;
            BlockState buis = level.getBlockState(rit.pad[i]);
            if (!Buizen.isBuis(buis) && !Buizen.isStuk(buis)) {
                rit.laatVallen();   // somebody broke the tube under it
                continue;
            }
            Vec3 a = rit.punten[i], b = rit.punten[i + 1];
            float f = t - i;
            double x = a.x + (b.x - a.x) * f, y = a.y + (b.y - a.y) * f, z = a.z + (b.z - a.z) * f;
            Ding ding = state.volgende();
            items.updateForTopItem(ding.item, rit.stack, ItemDisplayContext.FIXED, level, null, rit.zaad);
            ding.x = x - hier.getX();
            ding.y = y - hier.getY();
            ding.z = z - hier.getZ();
            ding.draai = (nu * 9f + (rit.zaad & 255)) % 360f;
            ding.licht = LevelRenderer.getLightCoords(level, rit.pad[i]);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.aantal; i++) {
            Ding ding = state.dingen.get(i);
            if (ding.item.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(ding.x, ding.y, ding.z);
            pose.mulPose(Axis.YP.rotationDegrees(ding.draai));
            pose.mulPose(Axis.XP.rotationDegrees(ding.draai * 0.6f));
            pose.scale(MAAT, MAAT, MAAT);
            ding.item.submit(pose, collector, ding.licht, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    /** The items roll far away from the piece: draw also when the piece itself is out of sight. */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(BuisStukBlockEntity be) {
        return BuisRitten.van(be.getBlockPos()).isEmpty() ? new AABB(be.getBlockPos()) : AABB.INFINITE;
    }
}
