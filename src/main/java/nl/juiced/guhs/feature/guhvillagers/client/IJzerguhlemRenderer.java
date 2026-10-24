package nl.juiced.guhs.feature.guhvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.IronGolemCrackinessLayer;
import net.minecraft.client.renderer.entity.layers.IronGolemFlowerLayer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhvillagers.IJzerguhlem;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature;

/**
 * The IJzerguhlem: vanilla's IronGolemRenderer with the guhlem model (GuhVillagerModellen#ijzerguhlem, animated by the
 * vanilla IronGolemModel), its own texture and pluisgras instead of a poppy when it offers a flower. The cracks are
 * vanilla's (the body, arms and legs keep the golem's texture spots).
 */
public class IJzerguhlemRenderer extends MobRenderer<IJzerguhlem, IronGolemRenderState, IronGolemModel> {
    public static final Identifier TEXTURE = Guhs.id("textures/entity/ijzerguhlem.png");
    private final BlockModelResolver blockModelResolver;

    public IJzerguhlemRenderer(EntityRendererProvider.Context context) {
        super(context, new IronGolemModel(context.bakeLayer(GuhVillagerModellen.IJZERGUHLEM)), 0.7F);
        this.blockModelResolver = context.getBlockModelResolver();
        this.addLayer(new IronGolemCrackinessLayer(this));
        this.addLayer(new IronGolemFlowerLayer(this));
    }

    @Override
    public Identifier getTextureLocation(IronGolemRenderState state) {
        return TEXTURE;
    }

    @Override
    public IronGolemRenderState createRenderState() {
        return new IronGolemRenderState();
    }

    @Override
    public void extractRenderState(IJzerguhlem entity, IronGolemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackTicksRemaining = entity.getAttackAnimationTick() > 0.0F ? entity.getAttackAnimationTick() - partialTicks : 0.0F;
        state.offerFlowerTick = entity.getOfferFlowerTick();
        if (state.offerFlowerTick > 0) {
            this.blockModelResolver.update(state.flowerBlock, KnuffeldalFeature.PLUISGRAS.get().defaultBlockState(), IronGolemRenderer.BLOCK_DISPLAY_CONTEXT);
        } else {
            state.flowerBlock.clear();
        }
        state.crackiness = entity.getCrackiness();
    }

    @Override
    protected void setupRotations(IronGolemRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if (!(state.walkAnimationSpeed < 0.01)) {
            float wp = state.walkAnimationPos + 6.0F;
            float triangleWave = (Math.abs(wp % 13.0F - 6.5F) - 3.25F) / 3.25F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * triangleWave));
        }
    }
}
