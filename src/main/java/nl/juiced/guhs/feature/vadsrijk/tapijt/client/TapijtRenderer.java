package nl.juiced.guhs.feature.vadsrijk.tapijt.client;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EconomieClient;
import nl.juiced.guhs.feature.vadsrijk.tapijt.TapijtEntity;

/**
 * U-TAPIJT: the flying carpet's renderer: the normal mount renderer (model, weave texture, tassel overlay, cushion, pop-in) plus the
 * banking into turns, the nose dip when it speeds up / brakes, and a gentle bob (all looks only; the ripple of the cloth is the
 * GeckoLib animation). The rider gets the same tilt and bob (TapijtClient).
 * <p>Sign: like the sled, the model's +x is its right side; a right turn (yaw going up) lowers the right side. Wrong way round in
 * the client? {@code /guhs vadsrijk tapijt tune kanteling -3.5}.
 */
public class TapijtRenderer extends EconomieClient.RijdierRenderer {
    /** {roll, pitch, bob} */
    private static final DataTicket<float[]> KANTEL = DataTicket.create("guhs_vr_tapijt_kantel", float[].class);

    public TapijtRenderer(EntityRendererProvider.Context context) {
        super(context, RijdierSoort.TAPIJT);
        this.shadowRadius = 1.1f;
    }

    /** The gentle bob (blocks), the same for the carpet and its rider. */
    public static float bob(RijdierEntity r, float pt) {
        if (r.voorbeeld) {
            return 0;
        }
        float t = r.tickCount + pt + r.getId() * 11;
        return Mth.sin(t * 0.09f) * 0.045f;
    }

    @Override
    public void addRenderData(RijdierEntity r, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
        super.addRenderData(r, related, state, partialTick);
        if (r instanceof TapijtEntity t && !r.voorbeeld) {
            state.addGeckolibData(KANTEL, new float[] {t.roll(partialTick), t.pitch(partialTick), bob(r, partialTick)});
        }
    }

    @Override
    protected void applyRotations(RenderPassInfo<LivingEntityRenderState> info, PoseStack poseStack, float nativeScale) {
        super.applyRotations(info, poseStack, nativeScale);
        float[] k = info.getGeckolibData(KANTEL);
        if (k != null) {
            poseStack.translate(0, k[2], 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-k[0]));
            poseStack.mulPose(Axis.XP.rotationDegrees(-k[1]));
        }
    }
}
