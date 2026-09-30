package nl.juiced.guhs.feature.knus.client;

import nl.juiced.guhs.client.GuhRenderer;

/**
 * Extra render passes on every guh (2.8, client only), so features never edit the GuhRenderer: register a layer
 * from your client init, e.g. to draw a sparkle, pyjamas, an ice-cream hat or blushing cheeks when a {@code GuhHooks} flag is set.
 * <p>
 * 1.1.0 (GeckoLib 5): a layer is a {@link GuhRenderer.Hook}. It runs at extract time (the guh is there) and puts what has to
 * be drawn into the {@link nl.juiced.guhs.client.GuhRenderFrame}: the model again with only some bones and another texture
 * ({@code frame.pass(texture, colour, bone -> shows)}, was a reRender with hidden bones), items or other things in the old
 * layer pose ({@code frame.layerExtra((pose, collector, light) -> ..)}: entity origin, not rotated, baby size/squish scale).
 * The hook passes are drawn after the clothes, like the 1.0.0 layer. {@link #laag} is the same as {@link GuhRenderer#hook}.
 */
public final class GuhRenderHooks {
    /** Add a render layer for every guh (see the class comment). */
    public static void laag(GuhRenderer.Hook laag) {
        GuhRenderer.hook(laag);
    }

    private GuhRenderHooks() {
    }
}
