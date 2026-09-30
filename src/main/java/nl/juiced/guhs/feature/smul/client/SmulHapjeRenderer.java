package nl.juiced.guhs.feature.smul.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import nl.juiced.guhs.feature.smul.SmulHapje;

/**
 * Falling food: its item, big and slowly spinning (the golden smulknabbel glows in the dark).
 * 1.1.0: render state + submit (the item is resolved at extract time).
 */
public class SmulHapjeRenderer extends EntityRenderer<SmulHapje, SmulHapjeRenderer.State> {
    public static class State extends EntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float spin;
        boolean landed;
        boolean goud;
    }

    private final ItemModelResolver items;

    public SmulHapjeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemModelResolver();
        this.shadowRadius = 0.3f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SmulHapje hapje, State state, float partialTick) {
        super.extractRenderState(hapje, state, partialTick);
        state.spin = (hapje.tickCount + partialTick) * 5f + hapje.getId() * 37f;
        state.landed = hapje.hasLanded();
        state.goud = hapje.soort() == SmulHapje.Soort.GOUD;
        items.updateForNonLiving(state.item, hapje.stack(), ItemDisplayContext.GROUND, hapje);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        float spin = state.spin;
        pose.translate(0, 0.35 + (state.landed ? 0 : Mth.sin(spin * 0.05f) * 0.05f), 0);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.scale(1.7f, 1.7f, 1.7f);
        int l = state.goud ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords;
        state.item.submit(pose, collector, l, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
