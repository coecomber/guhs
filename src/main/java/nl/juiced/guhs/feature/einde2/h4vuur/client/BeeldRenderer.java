package nl.juiced.guhs.feature.einde2.h4vuur.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.einde2.h4vuur.GuhvatarBeeldBlock;
import nl.juiced.guhs.feature.einde2.h4vuur.GuhvatarBeeldBlockEntity;

/**
 * einde2 phase B, slice h4_vuur: the figure on the Guhvatar statue: the player model in a proud pose (one arm forward, the
 * other out), with the skin of the player the statue was made for (or, on the Hall's pedestal, the skin of whoever looks at
 * it: {@link GuhvatarBeeldBlock#SPIEGEL}), standing on the plinth, its eyes glowing like in the Guhvatar state.
 */
public class BeeldRenderer implements BlockEntityRenderer<GuhvatarBeeldBlockEntity, BeeldRenderer.Staat> {
    /** How high the plinth is (the block model), and how big the figure is drawn. */
    private static final float SOKKEL = 5f / 16f, SCHAAL = 0.9f;

    public static class Staat extends BlockEntityRenderState {
        Direction richting = Direction.NORTH;
        @Nullable
        Identifier huid;
        boolean slank;
    }

    private final Model.Simple breed, slank;
    private final PlayerSkinRenderCache huiden;

    public BeeldRenderer(BlockEntityRendererProvider.Context context) {
        this.breed = new Model.Simple(pose(context.bakeLayer(ModelLayers.PLAYER)), RenderTypes::entityTranslucent);
        this.slank = new Model.Simple(pose(context.bakeLayer(ModelLayers.PLAYER_SLIM)), RenderTypes::entityTranslucent);
        this.huiden = context.playerSkinRenderCache();
    }

    /** The statue's pose: right arm forward (a form of the benders), left arm out to the side, chin up. */
    private static ModelPart pose(ModelPart root) {
        root.getChild("right_arm").xRot = -0.9f;
        root.getChild("right_arm").zRot = 0.15f;
        root.getChild("left_arm").zRot = -0.45f;
        root.getChild("head").xRot = -0.12f;
        root.getChild("right_leg").xRot = 0.12f;
        root.getChild("left_leg").xRot = -0.12f;
        return root;
    }

    @Override
    public Staat createRenderState() {
        return new Staat();
    }

    @Override
    public void extractRenderState(GuhvatarBeeldBlockEntity be, Staat state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        var blok = be.getBlockState();
        state.richting = blok.hasProperty(GuhvatarBeeldBlock.FACING) ? blok.getValue(GuhvatarBeeldBlock.FACING) : Direction.NORTH;
        PlayerSkin huid = null;
        if (blok.hasProperty(GuhvatarBeeldBlock.SPIEGEL) && blok.getValue(GuhvatarBeeldBlock.SPIEGEL) && Minecraft.getInstance().player != null) {
            huid = Minecraft.getInstance().player.getSkin();
        } else {
            ResolvableProfile profiel = be.eigenaar();
            if (profiel != null) {
                huid = huiden.getOrDefault(profiel).playerSkin();
            }
        }
        if (huid == null && Minecraft.getInstance().player != null) {
            huid = Minecraft.getInstance().player.getSkin();
        }
        state.huid = huid == null ? null : huid.body().texturePath();
        state.slank = huid != null && huid.model() == PlayerModelType.SLIM;
    }

    @Override
    public void submit(Staat state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.huid == null) {
            return;
        }
        Model.Simple model = state.slank ? slank : breed;
        pose.pushPose();
        pose.translate(0.5, SOKKEL, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180f - state.richting.toYRot()));
        pose.scale(-SCHAAL, -SCHAAL, SCHAAL);
        pose.translate(0.0, -1.501, 0.0);
        collector.submitModel(model, Unit.INSTANCE, pose, RenderTypes.entityTranslucent(state.huid), state.lightCoords, OverlayTexture.NO_OVERLAY, 0,
                state.breakProgress);
        collector.order(1).submitModel(model, Unit.INSTANCE, pose, GuhRenderTypes.eyes(LookLaag.OGEN), state.lightCoords, OverlayTexture.NO_OVERLAY, -1,
                null, 0, null);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(GuhvatarBeeldBlockEntity be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 2, 0);
    }
}
