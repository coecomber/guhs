package nl.juiced.guhs.feature.vadsrijk.party.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpeler;

/**
 * W-PARTY: a nep-speler drawn as a Minecraft player: the player model with one of the nine default (wide) skins, the item in its
 * hand, its name tag and, for 5 s, its last chat line above the name. Not drawn at all for a player with "Nep-spelers: uit".
 */
public class NepSpelerRenderer extends LivingEntityRenderer<NepSpeler, AvatarRenderState, PlayerModel> {
    private static final String[] NAMEN = {"alex", "ari", "efe", "kai", "makena", "noor", "steve", "sunny", "zuri"};
    private static final PlayerSkin[] SKINS = new PlayerSkin[NAMEN.length];

    static {
        for (int i = 0; i < NAMEN.length; i++) {
            SKINS[i] = new PlayerSkin(new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace("entity/player/wide/" + NAMEN[i])), null, null,
                    PlayerModelType.WIDE, true);
        }
    }

    public NepSpelerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new PlayerItemInHandLayer<>(this));
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public boolean shouldRender(NepSpeler e, Frustum culler, double x, double y, double z) {
        return NepClientConfig.aan() && super.shouldRender(e, culler, x, y, z);
    }

    @Override
    public void extractRenderState(NepSpeler e, AvatarRenderState state, float pt) {
        super.extractRenderState(e, state, pt);
        HumanoidMobRenderer.extractHumanoidRenderState(e, state, pt, itemModelResolver);
        state.skin = SKINS[Math.floorMod(e.skin(), SKINS.length)];
        state.rightArmPose = e.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        state.leftArmPose = e.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        state.id = e.getId();
        String zeg = e.zegKey();
        if (!zeg.isEmpty() && e.tickCount - e.zegSinds < NepSpeler.ZEG_TICKS && state.nameTag != null) {
            int bar = zeg.indexOf('|');
            Component regel = bar < 0 ? Component.translatable(zeg) : Component.translatable(zeg.substring(0, bar), zeg.substring(bar + 1));
            state.scoreText = state.nameTag;   // (the name drops one line; the chat line is above it)
            state.nameTag = Component.literal("\"").append(regel).append("\"");
        }
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }

    @Override
    protected void scale(AvatarRenderState state, PoseStack pose) {
        pose.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
