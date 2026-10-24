package nl.juiced.guhs.feature.toveren.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.toveren.GlinsteraltaarBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The fluffy lilac book that floats above the Glinsteraltaar: vanilla's enchanting-table book (same model, same
 * animation as {@code EnchantTableRenderer}), a little higher (above the crystal tips) and with the guh book's texture
 * ({@code textures/block/glinsteraltaar_boek.png}, 64 x 32 like vanilla's, so it lives on the block atlas).
 */
public class GlinsteraltaarRenderer implements BlockEntityRenderer<GlinsteraltaarBlockEntity, EnchantTableRenderState> {
    public static final SpriteId BOEK = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Guhs.id("block/glinsteraltaar_boek"));
    private final SpriteGetter sprites;
    private final BookModel boek;

    public GlinsteraltaarRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.boek = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public EnchantTableRenderState createRenderState() {
        return new EnchantTableRenderState();
    }

    @Override
    public void extractRenderState(GlinsteraltaarBlockEntity be, EnchantTableRenderState state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.flip = Mth.lerp(partialTicks, be.oFlip, be.flip);
        state.open = Mth.lerp(partialTicks, be.oOpen, be.open);
        state.time = be.time + partialTicks;
        float or = be.rot - be.oRot;
        while (or >= (float) Math.PI) {
            or -= (float) (Math.PI * 2);
        }
        while (or < (float) -Math.PI) {
            or += (float) (Math.PI * 2);
        }
        state.yRot = be.oRot + or * partialTicks;
    }

    @Override
    public void submit(EnchantTableRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.1F + Mth.sin(state.time * 0.1F) * 0.02F, 0.5F);
        poseStack.mulPose(Axis.YP.rotation(-state.yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
        float ff1 = Mth.frac(state.flip + 0.25F) * 1.6F - 0.3F;
        float ff2 = Mth.frac(state.flip + 0.75F) * 1.6F - 0.3F;
        BookModel.State bookState = BookModel.State.forAnimation(state.time, Mth.clamp(ff1, 0.0F, 1.0F), Mth.clamp(ff2, 0.0F, 1.0F), state.open);
        collector.submitModel(boek, bookState, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, BOEK, sprites, 0, state.breakProgress);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(GlinsteraltaarBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1., pos.getY() + 1.8, pos.getZ() + 1.);
    }
}
