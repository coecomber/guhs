package nl.juiced.guhs.feature.knuffelbad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knuffelbad.GlijPad;
import nl.juiced.guhs.feature.knuffelbad.ZwembandjeEntity;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

/**
 * The zwembandje (geo/entity/zwembandje.geo.json: a pink-and-white swim ring with a little guh head at the front), turned
 * exactly like the slide under it: along the ride, tilted with the surface (up a funnel's wall, round a tube).
 */
public class ZwembandjeRenderer extends GeoEntityRenderer<ZwembandjeEntity> {
    public ZwembandjeRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Guhs.id("zwembandje")));
        this.shadowRadius = 0.5f;
    }

    /**
     * Model frame: right +x, up +y, forward -z; turned to the slide's frame (right, the surface's normal, the ride's
     * direction). The normal leads: in the air it turns to straight up, so the ring flies level and lands flat (PLONS).
     */
    @Override
    protected void applyRotations(ZwembandjeEntity ring, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        GlijPad.Stand st = ring.stand(ring.tekenTau(partialTick), ring.tekenLat(partialTick));
        Vec3 n = st.normal().normalize();
        Vec3 r = st.tangent().cross(n);
        if (r.lengthSqr() < 1e-6) {
            return;
        }
        r = r.normalize();
        Vec3 t = n.cross(r).normalize();
        Matrix3f m = new Matrix3f((float) r.x, (float) r.y, (float) r.z, (float) n.x, (float) n.y, (float) n.z, (float) -t.x, (float) -t.y, (float) -t.z);
        poseStack.mulPose(new Quaternionf().setFromNormalized(m));
    }
}
