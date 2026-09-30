package nl.juiced.guhs.feature.knuffelbad.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knuffelbad.GlijPad;
import nl.juiced.guhs.feature.knuffelbad.ZwembandjeEntity;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;

/**
 * The zwembandje (geckolib/models/entity/zwembandje.geo.json: a pink-and-white swim ring with a little guh head at the front), turned
 * exactly like the slide under it: along the ride, tilted with the surface (up a funnel's wall, round a tube).
 * <p>
 * 1.1.0 (GeckoLib 5): the slide's frame is worked out at extract time and handed to the render as a rotation.
 */
public class ZwembandjeRenderer extends GeoEntityRenderer<ZwembandjeEntity, EntityRenderState> {
    /** The ring's turn (null: none, like 1.0.0 when the slide frame was degenerate). */
    static final DataTicket<Quaternionf> DRAAI = DataTicket.create("guhs_zwembandje_draai", Quaternionf.class);

    public ZwembandjeRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Guhs.id("zwembandje")));
        this.shadowRadius = 0.5f;
    }

    /**
     * Model frame: right +x, up +y, forward -z; turned to the slide's frame (right, the surface's normal, the ride's
     * direction). The normal leads: in the air it turns to straight up, so the ring flies level and lands flat (PLONS).
     */
    @Override
    public void addRenderData(ZwembandjeEntity ring, @Nullable Void related, EntityRenderState state, float partialTick) {
        GlijPad.Stand st = ring.stand(ring.tekenTau(partialTick), ring.tekenLat(partialTick));
        Vec3 n = st.normal().normalize();
        Vec3 r = st.tangent().cross(n);
        if (r.lengthSqr() < 1e-6) {
            return;
        }
        r = r.normalize();
        Vec3 t = n.cross(r).normalize();
        Matrix3f m = new Matrix3f((float) r.x, (float) r.y, (float) r.z, (float) n.x, (float) n.y, (float) n.z, (float) -t.x, (float) -t.y, (float) -t.z);
        state.addGeckolibData(DRAAI, new Quaternionf().setFromNormalized(m));
    }

    @Override
    protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
        Quaternionf draai = info.getGeckolibData(DRAAI);
        if (draai != null) {
            poseStack.mulPose(draai);
        }
    }
}
