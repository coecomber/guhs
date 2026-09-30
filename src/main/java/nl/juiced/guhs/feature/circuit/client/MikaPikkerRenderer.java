package nl.juiced.guhs.feature.circuit.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.circuit.MikaPikkerEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** A Mika-pikker: the Mika model with the guh animations, in its racing colours (a chequered bandana; pushers in orange). */
public class MikaPikkerRenderer extends GeoEntityRenderer<MikaPikkerEntity> {
    private static final ResourceLocation PIKKER = Guhs.id("textures/entity/circuit_mikapikker.png");
    private static final ResourceLocation DUWER = Guhs.id("textures/entity/circuit_mikaduwer.png");

    public MikaPikkerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<MikaPikkerEntity>(Guhs.id("mika"), true) {
            @Override
            public ResourceLocation getTextureResource(MikaPikkerEntity mika) {
                return mika.isDuwer() ? DUWER : PIKKER;
            }
        }.withAltAnimations(Guhs.id("guh")));
        this.shadowRadius = 0.35f;
        this.withScale(0.8f);
    }
}
