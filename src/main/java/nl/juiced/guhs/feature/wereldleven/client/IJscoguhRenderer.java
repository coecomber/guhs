package nl.juiced.guhs.feature.wereldleven.client;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.wereldleven.IJscoguhEntity;

/**
 * IJscoguh Tingeling on his ice-cream bike (geckolib/models/entity/ijscoguh.geo.json, tools/features/wereldleven.py): the
 * wheels turn as far as he rides, his feet pedal, the bell wiggles when it rings, the parasol sways a little.
 * <p>
 * 1.1.0 (GeckoLib 5): the bell's wiggle is worked out at extract time (data ticket), the bones move in
 * {@link #adjustModelBonesForRender} (was the model's setCustomAnimations); the head follows his look as before.
 */
public class IJscoguhRenderer extends GeoEntityRenderer<IJscoguhEntity, LivingEntityRenderState> {
    /** Client: per IJscoguh [bell counter seen, tick it rang]. */
    private static final Map<IJscoguhEntity, int[]> BEL = new WeakHashMap<>();
    private static final DataTicket<Float> WIEBEL = DataTicket.create("guhs_ijscoguh_bel", Float.class);

    public IJscoguhRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Guhs.id("ijscoguh")));
        this.shadowRadius = 0.8f;
    }

    @Override
    public void addRenderData(IJscoguhEntity ijsco, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
        float t = ijsco.tickCount + partialTick;
        int[] bel = BEL.computeIfAbsent(ijsco, e -> new int[] {e.belTeller(), -100});
        if (bel[0] != ijsco.belTeller()) {
            bel[0] = ijsco.belTeller();
            bel[1] = ijsco.tickCount;
        }
        float sinds = t - bel[1];
        state.addGeckolibData(WIEBEL, sinds < 30 ? (float) Math.sin(sinds * 1.3f) * 0.6f * (1f - sinds / 30f) : 0f);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");   // (was DefaultedEntityGeoModel(id, true))
        LivingEntityRenderState state = info.renderState();
        // (these bones have no base rotation: the snapshot values are the same numbers as 1.0.0's)
        float swing = state.walkAnimationPos;
        float wheel = swing * 1.6f;
        for (String w : new String[] {"wiel_links", "wiel_rechts"}) {
            bones.ifPresent(w, b -> b.setRotX(-wheel));
        }
        bones.ifPresent("wiel_achter", b -> b.setRotX(-wheel * 1.2f));
        float pedal = swing * 1.2f;
        bones.ifPresent("foot_left", b -> b.setRotX((float) Math.sin(pedal) * 0.6f));
        bones.ifPresent("foot_right", b -> b.setRotX((float) Math.sin(pedal + Math.PI) * 0.6f));
        float t = state.ageInTicks;
        float wiebel = info.getOrDefaultGeckolibData(WIEBEL, 0f);
        bones.ifPresent("bel", b -> b.setRotZ(wiebel));
        bones.ifPresent("parasol", b -> {
            b.setRotZ((float) Math.sin(t * 0.05f) * 0.03f);
            b.setRotX((float) Math.cos(t * 0.04f) * 0.03f);
        });
    }
}
