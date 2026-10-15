package nl.juiced.guhs.feature.snuffeldorp;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Het Snuffeleiland, the ISLAND and its FIRST SERIES (DESIGN_VERHALENPAD C, slice snuffel-dorp). It builds on the kern
 * ({@code feature/snuffel}: the dog form, sniffing, the tree, residents, travel) and adds no registry content of its own:
 * <ul>
 *   <li>the island itself is data and templates written by tools/features/snuffel_dorp.py (snuffel_dorp_bouw.py): the
 *   beach, Snuffeldorp with its plein, eight houses and the harbour with the captain's boat, the meadow with the trainer's
 *   spot and the tree's knoll, and the closed rest behind the roadblock; ten residents on their spots, the scents and
 *   where they lie ({@code data/guhs/snuffel/eiland.json}), this slice's own spots ({@code data/guhs/snuffeldorp/dorp.json},
 *   {@link Plekken});</li>
 *   <li>the story from the beach to the Guhstation ({@link Dorp}), what the residents say and do ({@link DorpRollen},
 *   {@link Gesprek}), the four scenes ({@link DorpScenes}) and the roadblock's rule ({@link Wegversperring}).</li>
 * </ul>
 * Dev and AutoCheck: {@code /guhs snuffeldorp ...} ({@link DorpCommando}). Tests: {@link SnuffeldorpGameTests}.
 */
public final class SnuffeldorpFeature {
    private SnuffeldorpFeature() {
    }

    public static void register(IEventBus modBus) {
        DorpScenes.init();
        Dorp.init();
        NeoForge.EVENT_BUS.register(DorpEvents.class);
        NeoForge.EVENT_BUS.addListener(DorpCommando::registreer);
    }
}
