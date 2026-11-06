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
        nl.juiced.guhs.feature.vadsrijk.zone3.npc.Z3NpcRegistry.register(modBus);   // Z3-NPC (zone 3 De Schemerwortel)
        nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5VerhaalRegistry.register(modBus);   // T-Z5VERHAAL (zone 5 Hoogvads: the story)
        nl.juiced.guhs.feature.vadsrijk.economie.EconomieRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Z2SystemenRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2WezensRegistry.register(modBus);   // zone 2 (z2-wezens)
        nl.juiced.guhs.feature.vadsrijk.momenten.MomentenRegistry.register(modBus);   // MOMENTEN-HOOK (fix round 1: wave + caravan)
        nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenRegistry.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.biomen.BiomenRegistry.register(modBus);   // fix round 2: one biome per area
        nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierenRegistry.register(modBus);   // G-PETS
        nl.juiced.guhs.feature.vadsrijk.rijdier.RijdierExtraRegistry.register(modBus);   // T-RIJDIER: mount looks + new mounts + pet abilities
        nl.juiced.guhs.feature.vadsrijk.voltooiing.VoltooiingRegistry.register(modBus);   // I-VOLTOOIING (Verkenningsboek)
        nl.juiced.guhs.feature.vadsrijk.stadsmuziek.StadsMuziek.register(modBus);   // I-AUDIO: town music + town ambience
        nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3SystemenRegistry.register(modBus);   // zone 3 (z3-systemen)
        nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3WezensRegistry.register(modBus);   // zone 3 (z3-wezens)
        nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4WezensRegistry.register(modBus);   // T-Z4WEZENS: zone 4 creatures + loot
        nl.juiced.guhs.feature.vadsrijk.zone4.bazen.Z4BazenRegistry.register(modBus);   // T-Z4WEZENS: Zeeslurf + Wrak
        nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensRegistry.register(modBus);   // T-Z5WEZENS: zone-5 combat
        nl.juiced.guhs.feature.vadsrijk.vriendjes.DraakVriendjes.register(modBus);   // N-EXTRA: critters around the pink dragon
        nl.juiced.guhs.feature.vadsrijk.indeling.IndelingRegistry.register(modBus);   // T-WERELD: world layout W-3 (barriers, dev tp)
        nl.juiced.guhs.feature.vadsrijk.dialoog.RijkDialoog.register(modBus);   // T-DIALOOG: the Vadsrijk talking screen
        nl.juiced.guhs.feature.vadsrijk.geluid.GeluidRegistry.register(modBus);   // T-GELUID: area music, boss themes, stingers, ambience
        nl.juiced.guhs.feature.vadsrijk.tmodels.TModels.register(modBus);   // T-MODELS: kern slimes as GeckoLib models + /guhs vadsrijk models
        nl.juiced.guhs.feature.vadsrijk.vfx.VfxRegistry.register(modBus);   // T-VFX: VFX kit v1 + boss bouwdoos
        nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootRegistry.register(modBus);   // T-Z4BOOT: the zone-4 Zeilbootje
        nl.juiced.guhs.feature.vadsrijk.web.WebRegistry.register(modBus);   // S-SITE: guhs.nl/vadsrijk live export (off by default) + wiki dump
        nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtRegistry.register(modBus);   // T-AMBACHT: the Ambachten (professions)
        nl.juiced.guhs.feature.vadsrijk.items.ItemsRegistry.register(modBus);   // T-ITEMS: itemisatie 2.0 (keep last: its loot sees every table)
        nl.juiced.guhs.feature.vadsrijk.puzzels.PuzzelRegistry.register(modBus);   // T-PUZZELS: puzzle toolbox + puzzles/secrets zone 1-3
        nl.juiced.guhs.feature.vadsrijk.opslag.OpslagRegistry.register(modBus);   // T-OPSLAG: knapzak, materiaaltas, bank, HUD additions
        nl.juiced.guhs.feature.vadsrijk.kleding.KledingRegistry.register(modBus);   // T-KLEDING: Kledingkast, wings, effects
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
        nl.juiced.guhs.feature.vadsrijk.voltooiing.VoltooiingRegistry.payloads(registrar);   // I-VOLTOOIING
        nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3SystemenRegistry.payloads(registrar);   // zone 3 (z3-systemen)
        nl.juiced.guhs.feature.vadsrijk.geluid.GeluidRegistry.payloads(registrar);   // T-GELUID
        nl.juiced.guhs.feature.vadsrijk.vfx.VfxRegistry.payloads(registrar);   // T-VFX
        nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootRegistry.payloads(registrar);   // T-Z4BOOT
        nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtRegistry.payloads(registrar);   // T-AMBACHT
        nl.juiced.guhs.feature.vadsrijk.items.ItemsRegistry.payloads(registrar);   // T-ITEMS
        nl.juiced.guhs.feature.vadsrijk.opslag.OpslagRegistry.payloads(registrar);   // T-OPSLAG
        nl.juiced.guhs.feature.vadsrijk.kleding.KledingRegistry.payloads(registrar);   // T-KLEDING
    }
}
