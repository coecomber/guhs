package nl.juiced.guhs.feature.snuffel.client;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.snuffel.Honden;
import nl.juiced.guhs.feature.snuffel.MaatjeEntity;

/**
 * Draws the companion: one of the three approved models (a Zweefzaadje, b Mos-eikeltje, c Zonnepluisje) with its happy
 * or its naughty face ({@code snuffel_maatje_<a|b|c>_<blij|ondeugend>}: geo, animations, texture), at half scale. Only
 * the companion's own player ever has the entity (the server sends it to nobody else), so there is nothing to hide here.
 */
public class MaatjeRenderer extends GeoEntityRenderer<MaatjeEntity, EntityRenderState> {
    record Uiterlijk(String naam, float yaw) {
    }

    private static final DataTicket<Uiterlijk> UITERLIJK = DataTicket.create("guhs_snuffel_maatje", Uiterlijk.class);

    public MaatjeRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.15f;
        withScale(Honden.MAATJE_SCHAAL);
    }

    static String naam(MaatjeEntity m) {
        return "snuffel_maatje_" + m.soort() + (m.ondeugend() ? "_ondeugend" : "_blij");
    }

    @Override
    protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
        Uiterlijk u = info.getGeckolibData(UITERLIJK);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - (u == null ? 0 : u.yaw())));
    }

    static final class Model extends GeoModel<MaatjeEntity> {
        @Override
        public void addAdditionalStateData(MaatjeEntity m, @Nullable Object related, GeoRenderState state) {
            float partial = net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            state.addGeckolibData(UITERLIJK, new Uiterlijk(naam(m), Mth.rotLerp(partial, m.yRotO, m.getYRot())));
        }

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            Uiterlijk u = state.getGeckolibData(UITERLIJK);
            return Guhs.id("entity/" + (u == null ? "snuffel_maatje_b_blij" : u.naam()));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            Uiterlijk u = state.getGeckolibData(UITERLIJK);
            return Guhs.id("textures/entity/" + (u == null ? "snuffel_maatje_b_blij" : u.naam()) + ".png");
        }

        @Override
        public Identifier getAnimationResource(MaatjeEntity m) {
            return Guhs.id("entity/" + naam(m));
        }
    }
}
