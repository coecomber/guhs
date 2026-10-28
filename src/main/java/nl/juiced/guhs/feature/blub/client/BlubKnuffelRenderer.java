package nl.juiced.guhs.feature.blub.client;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.knuffel.BlubKnuffelBlock;
import nl.juiced.guhs.feature.blub.knuffel.BlubKnuffelBlockEntity;

/**
 * 1.8.0: draws a Blub-knuffel: a client-only copy of its Blub (soort, meter, tint, strikje), small, sitting on the block and
 * looking the way the knuffel faces. No name tag and no shadow: it is a plush, not a Blub.
 */
public class BlubKnuffelRenderer implements BlockEntityRenderer<BlubKnuffelBlockEntity, BlubKnuffelRenderer.State> {
    /** How big the plush is (x the Blub's own size). */
    private static final float KNUFFEL = 0.62f;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        @Nullable
        EntityRenderState blub;
    }

    /** One client copy per knuffel (rebuilt when its Blub changes). */
    private static final Map<BlubKnuffelBlockEntity, Kopie> KOPIEEN = new WeakHashMap<>();

    private record Kopie(CompoundTag data, BlubGuhEntity blub) {
    }

    private final EntityRenderDispatcher entityRenderer;

    public BlubKnuffelRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlubKnuffelBlockEntity be, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breakProgress);
        state.facing = be.getBlockState().getValue(BlubKnuffelBlock.FACING);
        state.blub = null;
        if (be.getLevel() == null) {
            return;
        }
        Kopie k = KOPIEEN.get(be);
        if (k == null || !k.data().equals(be.blub()) || k.blub().level() != be.getLevel()) {
            BlubGuhEntity b = BlubFeature.BLUBGUH.get().create(be.getLevel(), EntitySpawnReason.LOAD);
            if (b == null) {
                return;
            }
            b.laadKnuffel(be.blub());
            k = new Kopie(be.blub().copy(), b);
            KOPIEEN.put(be, k);
        }
        BlubGuhEntity b = k.blub();
        float yaw = state.facing.toYRot();
        b.setYRot(yaw);
        b.yRotO = yaw;
        b.yBodyRot = b.yBodyRotO = b.yHeadRot = b.yHeadRotO = yaw;
        b.setXRot(0);
        state.blub = entityRenderer.extractEntity(b, 0f);
        state.blub.lightCoords = state.lightCoords;
        state.blub.shadowPieces.clear();
        state.blub.nameTag = null;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.blub == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.scale(KNUFFEL, KNUFFEL, KNUFFEL);
        entityRenderer.submit(state.blub, camera, 0, 0, 0, poseStack, collector);
        poseStack.popPose();
    }
}
