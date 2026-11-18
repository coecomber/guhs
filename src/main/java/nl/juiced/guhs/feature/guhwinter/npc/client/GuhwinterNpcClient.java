package nl.juiced.guhs.feature.guhwinter.npc.client;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.guhwinter.npc.GuhwinterNpcFeature;
import nl.juiced.guhs.feature.guhwinter.npc.NpcDefs;
import nl.juiced.guhs.feature.guhwinter.npc.WinterGuh;

/**
 * Guhwinter (npc slice), client: the Winterplein's guhs are drawn by the normal {@link GuhRenderer} (real guh clothes), plus
 * one extra pass with the event's own look pieces: bones of the guh model shown with the texture
 * {@code textures/entity/guh_clothes/guhwinter_<look>.png} that tools/features/guhwinter_npc.py paints (LOOKS there must
 * name the same bones). These pieces are no wardrobe clothes: nobody else can wear them.
 */
public final class GuhwinterNpcClient {
    /** look -> the bone prefixes its pass shows (the textures paint exactly their swatches). */
    private static final Map<String, List<String>> BOTTEN = Map.of(
            // the big white beard (Guhdalf's beard bones), the red mantle with gold (the king's long cape), the golden staff (ours)
            "sint", List.of("outfit_ring_baard", "outfit_long_cape", "outfit_guhwinter_staf"),
            // Anthony's camera (ours)
            "anthony", List.of("outfit_guhwinter_camera"),
            // Liek's golden nose ring (ours)
            "liek", List.of("outfit_guhwinter_neusring"),
            // the pietenpakje (the suit bones), the beret with its feather, a white ruff (the scarf bones), the soot smudge (ours)
            "pakje_0", List.of("outfit_suit", "outfit_beret", "outfit_scarf", "outfit_guhwinter_roet"),
            "pakje_1", List.of("outfit_suit", "outfit_beret", "outfit_scarf", "outfit_guhwinter_roet"),
            "pakje_2", List.of("outfit_suit", "outfit_beret", "outfit_scarf", "outfit_guhwinter_roet"),
            "pakje_3", List.of("outfit_suit", "outfit_beret", "outfit_scarf", "outfit_guhwinter_roet"));

    /** One texture and one (cached: the same object every frame) bone test per look. */
    private record Look(Identifier texture, Predicate<String> toont) {
    }

    private static final Map<String, Look> LOOKS = new java.util.HashMap<>();

    static {
        for (Map.Entry<String, List<String>> e : BOTTEN.entrySet()) {
            List<String> prefixes = e.getValue();
            LOOKS.put(e.getKey(), new Look(Guhs.id("textures/entity/guh_clothes/guhwinter_" + e.getKey() + ".png"),
                    bone -> prefixes.stream().anyMatch(bone::startsWith)));
        }
    }

    private GuhwinterNpcClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(GuhwinterNpcFeature.GUH.get(), GuhRenderer::new));
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (!(guh instanceof WinterGuh w)) {
                return;
            }
            NpcDefs.Def d = w.def();
            Look look = d == null || d.look() == null ? null : LOOKS.get(d.look());
            if (look != null) {
                frame.pass(look.texture(), 0xFFFFFFFF, look.toont());
            }
        });
    }
}
