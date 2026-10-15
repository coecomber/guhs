package nl.juiced.guhs.feature.techsaus.client;

import net.neoforged.bus.api.IEventBus;

/**
 * Client side of bbq2 (tech-vloeistof). Nothing to set up: the six sauce blocks are plain block models whose face, gauge and
 * stamp follow the block state (tools/features/tech_vloeistof_modellen.py), their drips, bubbles and steam come from
 * {@code Block#animateTick}, the tooltip lines from {@code SausBlokItem}, and what a machine holds is read through the
 * vadskracht hover readout ({@code feature.vadskracht.client.VadsHover}).
 */
public final class TechsausClient {
    public static void init(IEventBus modBus) {
    }

    private TechsausClient() {
    }
}
