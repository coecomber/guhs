package nl.juiced.guhs.feature.circuit.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.circuit.MikaPikkerEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

/** A Mika-pikker: the Mika model with the guh animations, in its racing colours (a chequered bandana; pushers in orange). */
public class MikaPikkerRenderer extends GeoEntityRenderer<MikaPikkerEntity> {
    private static final Identifier PIKKER = Guhs.id("textures/entity/circuit_mikapikker.png");
    private static final Identifier DUWER = Guhs.id("textures/entity/circuit_mikaduwer.png");

    public MikaPikkerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<MikaPikkerEntity>(Guhs.id("mika"), true) {
            @Override
            public Identifier getTextureResource(MikaPikkerEntity mika) {
                return mika.isDuwer() ? DUWER : PIKKER;
            }
        }.withAltAnimations(Guhs.id("guh")));
        this.shadowRadius = 0.35f;
        this.withScale(0.8f);
    }
}
