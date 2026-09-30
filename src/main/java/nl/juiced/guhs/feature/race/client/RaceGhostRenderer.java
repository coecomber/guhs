package nl.juiced.guhs.feature.race.client;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import com.geckolib.util.Color;

import net.minecraft.client.renderer.rendertype.RenderTypes;
/** The ghost of your best race: a guh in the pale ghost-guh fur, half see-through, that always runs (the golden one: gold). */
public class RaceGhostRenderer extends GuhRenderer {
    private static final Identifier TEXTURE = GuhVariant.GHOST.texture();

    public RaceGhostRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.2f;
    }

    @Override
    public RenderType getRenderType(GuhEntity guh, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderTypes.entityTranslucent(TEXTURE);
    }

    /** Your own ghost is pale blue; the golden ghost (2.9: the world's track record) shines gold. */
    @Override
    public Color getRenderColor(GuhEntity guh, float partialTick, int packedLight) {
        if (guh instanceof nl.juiced.guhs.feature.race.RaceGhostEntity ghost && ghost.isGoud()) {
            return Color.ofRGBA(1f, 0.82f, 0.25f, 0.6f);
        }
        return Color.ofRGBA(0.8f, 0.9f, 1f, 0.45f);
    }

    /** It is moved by the race, not by walking: any movement at all plays the running animation. */
    @Override
    public float getMotionAnimThreshold(GuhEntity guh) {
        return 0f;
    }
}
