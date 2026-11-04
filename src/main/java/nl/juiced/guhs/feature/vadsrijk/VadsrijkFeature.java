package nl.juiced.guhs.feature.vadsrijk;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Guhs 1.10.0 "Het Vadsrijk": the RPG dimension guhs:vadsrijk. The shared API and the slices: see CONTRACT_VADSRIJK.md.
 * Kern first (its registries and hooks), then every slice's registry class. Resources: tools/features/vadsrijk_*.py.
 */
public final class VadsrijkFeature {
    private VadsrijkFeature() {
    }

    public static void register(IEventBus modBus) {
        nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.klassen.KlassenRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.wereld.WereldRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.SfeerRegistry.register(modBus);   // fix round 1: atmosphere
        nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2NpcRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.economie.EconomieRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Z2SystemenRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2WezensRegistry.register(modBus);   // zone 2 (z2-wezens)
        nl.juiced.guhs.feature.vadsrijk.momenten.MomentenRegistry.register(modBus);   // MOMENTEN-HOOK (fix round 1: wave + caravan)
        nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierenRegistry.register(modBus);   // G-PETS
    }

    public static void payloads(PayloadRegistrar registrar) {
        nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry.payloads(registrar);
        nl.juiced.guhs.feature.vadsrijk.klassen.KlassenRegistry.payloads(registrar);
        nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry.payloads(registrar);
        nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry.payloads(registrar);
        nl.juiced.guhs.feature.vadsrijk.wereld.WereldRegistry.payloads(registrar);
        nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.SfeerRegistry.payloads(registrar);
        nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry.payloads(registrar);
        nl.juiced.guhs.feature.vadsrijk.economie.EconomieRegistry.payloads(registrar);
        nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Z2SystemenRegistry.payloads(registrar);
        nl.juiced.guhs.feature.vadsrijk.momenten.MomentenRegistry.payloads(registrar);   // MOMENTEN-HOOK
        nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenRegistry.payloads(registrar);
        nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierenRegistry.payloads(registrar);   // G-PETS
    }
}
