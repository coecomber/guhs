package nl.juiced.guhs.feature.wereldleven.client;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.wereldleven.IJscoguhEntity;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

/**
 * IJscoguh Tingeling on his ice-cream bike (geo/entity/ijscoguh.geo.json, tools/features/wereldleven.py): the wheels
 * turn as far as he rides, his feet pedal, the bell wiggles when it rings, the parasol sways a little.
 */
public class IJscoguhRenderer extends GeoEntityRenderer<IJscoguhEntity> {
    /** Client: per IJscoguh [bell counter seen, tick it rang]. */
    private static final Map<IJscoguhEntity, int[]> BEL = new WeakHashMap<>();

    public IJscoguhRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(Guhs.id("ijscoguh"), true) {
            @Override
            public void setCustomAnimations(IJscoguhEntity ijsco, long instanceId, AnimationTest<IJscoguhEntity> state) {
                super.setCustomAnimations(ijsco, instanceId, state);
                float swing = state.getLimbSwing();
                float wheel = swing * 1.6f;
                for (String w : new String[] {"wiel_links", "wiel_rechts"}) {
                    getBone(w).ifPresent(b -> b.setRotX(-wheel));
                }
                getBone("wiel_achter").ifPresent(b -> b.setRotX(-wheel * 1.2f));
                float pedal = swing * 1.2f;
                getBone("foot_left").ifPresent(b -> b.setRotX((float) Math.sin(pedal) * 0.6f));
                getBone("foot_right").ifPresent(b -> b.setRotX((float) Math.sin(pedal + Math.PI) * 0.6f));
                float t = ijsco.tickCount + state.getPartialTick();
                int[] bel = BEL.computeIfAbsent(ijsco, e -> new int[] {e.belTeller(), -100});
                if (bel[0] != ijsco.belTeller()) {
                    bel[0] = ijsco.belTeller();
                    bel[1] = ijsco.tickCount;
                }
                float sinds = t - bel[1];
                float wiebel = sinds < 30 ? (float) Math.sin(sinds * 1.3f) * 0.6f * (1f - sinds / 30f) : 0f;
                getBone("bel").ifPresent(b -> b.setRotZ(wiebel));
                getBone("parasol").ifPresent(b -> {
                    b.setRotZ((float) Math.sin(t * 0.05f) * 0.03f);
                    b.setRotX((float) Math.cos(t * 0.04f) * 0.03f);
                });
            }
        });
        this.shadowRadius = 0.8f;
    }
}
