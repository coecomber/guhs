package nl.juiced.guhs.feature.vadsrijk.vfx;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * T-VFX (roadmap 3.1 + 3.2): the VFX kit v1 and the boss bouwdoos. Registered from VadsrijkFeature (two add-only lines);
 * the client side is {@code vfx.client.VfxClient} (from VadsrijkClient). Resources: tools/features/vadsrijk_t_vfx.py.
 */
public final class VfxRegistry {
    private VfxRegistry() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(VfxCommando::register);
    }

    public static void payloads(PayloadRegistrar r) {
        VfxPayloads.register(r);
    }
}
