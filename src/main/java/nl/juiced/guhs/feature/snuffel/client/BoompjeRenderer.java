package nl.juiced.guhs.feature.snuffel.client;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.snuffel.BoompjeEntity;

/**
 * Draws the companion's tree in the stage of THIS client's own player ({@link EigenStand#boom}): the approved models
 * {@code snuffel_boompje_<1..4>} (kiem, scheutje, struikje, jong boompje). Before the first step only the ring of stones
 * with its patch of earth shows (the foot of model 1; the sprout is hidden), so the spot can be recognised.
 */
public class BoompjeRenderer extends GeoEntityRenderer<BoompjeEntity, EntityRenderState> {
    private static final DataTicket<Integer> STAP = DataTicket.create("guhs_snuffel_boom_stap", Integer.class);

    public BoompjeRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0f;
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info, BoneSnapshots bones) {
        Integer stap = info.getGeckolibData(STAP);
        if (stap != null && stap <= 0) {
            bones.ifPresent("stam", b -> b.skipRender(true).skipChildrenRender(true));
        }
    }

    private static String naam(int stap) {
        return "snuffel_boompje_" + Mth.clamp(stap, 1, 4);
    }

    static final class Model extends GeoModel<BoompjeEntity> {
        @Override
        public void addAdditionalStateData(BoompjeEntity boom, @Nullable Object related, GeoRenderState state) {
            state.addGeckolibData(STAP, EigenStand.boom());
        }

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            Integer stap = state.getGeckolibData(STAP);
            return Guhs.id("entity/" + naam(stap == null ? 1 : stap));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            Integer stap = state.getGeckolibData(STAP);
            return Guhs.id("textures/entity/" + naam(stap == null ? 1 : stap) + ".png");
        }

        @Override
        public Identifier getAnimationResource(BoompjeEntity boom) {
            return Guhs.id("entity/" + naam(EigenStand.boom()));
        }
    }
}
