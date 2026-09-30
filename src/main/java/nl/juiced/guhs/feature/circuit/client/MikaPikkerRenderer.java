package nl.juiced.guhs.feature.circuit.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.circuit.MikaPikkerEntity;

/** A Mika-pikker: the Mika model with the guh animations, in its racing colours (a chequered bandana; pushers in orange). */
public class MikaPikkerRenderer extends GeoEntityRenderer<MikaPikkerEntity, LivingEntityRenderState> {
    private static final Identifier PIKKER = Guhs.id("textures/entity/circuit_mikapikker.png");
    private static final Identifier DUWER = Guhs.id("textures/entity/circuit_mikaduwer.png");
    /** 1.1.0: the model picks the texture from the render state. */
    private static final DataTicket<Boolean> DUWT = DataTicket.create("guhs_mikapikker_duwer", Boolean.class);

    public MikaPikkerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<MikaPikkerEntity>(Guhs.id("mika")) {
            @Override
            public void addAdditionalStateData(MikaPikkerEntity mika, @Nullable Object related, GeoRenderState state) {
                state.addGeckolibData(DUWT, mika.isDuwer());
            }

            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                return Boolean.TRUE.equals(state.getGeckolibData(DUWT)) ? DUWER : PIKKER;
            }
        }.withAltAnimations(Guhs.id("guh")));
        this.shadowRadius = 0.35f;
        this.withScale(0.8f);
    }

    /** The "head" bone follows where it looks (GeckoLib 4: DefaultedEntityGeoModel(id, true)). */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
    }
}
