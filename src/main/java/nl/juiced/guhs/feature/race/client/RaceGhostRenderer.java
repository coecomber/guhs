package nl.juiced.guhs.feature.race.client;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;

/**
 * The ghost of your best race: a guh in the pale ghost-guh fur, half see-through, that always runs (the golden one: gold).
 * <p>
 * 1.1.0: the colour is an ARGB int (GeckoLib 5 has no Color class); the shadow follows the guh's size like every guh
 * (1.0.0 set 0.2 here, but GuhRenderer overwrote it every frame).
 */
public class RaceGhostRenderer extends GuhRenderer {
    private static final Identifier TEXTURE = GuhVariant.GHOST.texture();

    public RaceGhostRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @Nullable RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
        return RenderTypes.entityTranslucent(TEXTURE);
    }

    /** Your own ghost is pale blue; the golden ghost (2.9: the world's track record) shines gold. */
    @Override
    public int getRenderColor(GuhEntity guh, @Nullable Void related, float partialTick) {
        if (guh instanceof nl.juiced.guhs.feature.race.RaceGhostEntity ghost && ghost.isGoud()) {
            return ARGB.colorFromFloat(0.6f, 1f, 0.82f, 0.25f);
        }
        return ARGB.colorFromFloat(0.45f, 0.8f, 0.9f, 1f);
    }

    /** It is moved by the race, not by walking: any movement at all plays the running animation. */
    @Override
    public float getMotionAnimThreshold(GuhEntity guh) {
        return 0f;
    }
}
