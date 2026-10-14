package nl.juiced.guhs.feature.einde2.castvolk.client;

import java.util.List;

import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.feature.einde2.castvolk.CastVolk;
import nl.juiced.guhs.feature.einde2.castvolk.E2CastVolkFeature;

/**
 * einde2, slice cast_volk, client side: every kind of {@link CastVolk} draws its own model and sheet and plays its
 * character's animation file (a statue its own still one). A crowd kind (mix n) shows one of its n bone sets and hides the
 * others, picked from where it stands ({@code floorMod(x * 31 + z * 17, n)}): a resident keeps its look, nothing is synced.
 */
public final class E2CastVolkClient {
    public static void init(IEventBus modBus) {
        for (CastVolk rij : CastVolk.values()) {
            SittingGuhRenderers.NPC_MODELEN.put(rij.kind, Guhs.id("entity/guh_npc_" + rij.kind.id()));
            SittingGuhRenderers.NPC_ANIMATIES.put(rij.kind, Guhs.id("entity/guh_npc_" + rij.bestand.id()));
            int n = CastVolk.mix(rij.kind);
            if (n == 0) {
                continue;
            }
            List<List<String>> sets = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                sets.add(CastVolk.mixBotten(rij.kind, i));
            }
            SittingGuhRenderers.NPC_ANIMATORS.put(rij.kind, (npc, tick) -> {
                int keuze = E2CastVolkFeature.mixSet(npc.blockPosition(), n);
                return bones -> {
                    for (int i = 0; i < n; i++) {
                        if (i != keuze) {
                            for (String naam : sets.get(i)) {
                                bones.ifPresent(naam, b -> b.skipRender(true).skipChildrenRender(true));
                            }
                        }
                    }
                };
            });
        }
    }

    private E2CastVolkClient() {
    }
}
