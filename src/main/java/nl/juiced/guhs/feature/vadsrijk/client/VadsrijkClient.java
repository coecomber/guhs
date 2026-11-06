package nl.juiced.guhs.feature.vadsrijk.client;

import net.neoforged.bus.api.IEventBus;

/** Het Vadsrijk, the client: kern's HUD/keys/scenes/renderers, then every slice's client class. */
public final class VadsrijkClient {
    private VadsrijkClient() {
    }

    public static void init(IEventBus modBus) {
        nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.klassen.client.KlassenClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.wezens.client.WezensClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.bazen.client.BazenClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.wereld.client.WereldClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client.SfeerClient.init(modBus);   // fix round 1: atmosphere
        nl.juiced.guhs.feature.vadsrijk.npc.client.NpcClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.economie.client.EconomieClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.zone2.systemen.client.Z2SystemenClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.zone2.wezens.client.Z2WezensClient.init(modBus);   // zone 2 (z2-wezens)
        nl.juiced.guhs.feature.vadsrijk.momenten.client.MomentenClient.init(modBus);   // MOMENTEN-HOOK (fix round 1: wave + caravan)
        nl.juiced.guhs.feature.vadsrijk.zone2.bazen.client.Z2BazenClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.zone3.bazen.client.Z3BazenClient.init(modBus);
        nl.juiced.guhs.feature.vadsrijk.imker.client.ImkerClient.init(modBus);   // T-IMKER
        UitrustingTips.init();   // fix round 2 (g-ui): jewellery how-to, "Los stuk", set piece lists
        nl.juiced.guhs.feature.vadsrijk.huisdieren.client.HuisdierClient.init(modBus);   // G-PETS
        nl.juiced.guhs.feature.vadsrijk.voltooiing.client.VoltooiingClient.init(modBus);   // I-VOLTOOIING (Verkenningsboek, key B)
        nl.juiced.guhs.feature.vadsrijk.stadsmuziek.client.StadsMuziekSpeler.init();   // I-AUDIO: town music + town ambience
        nl.juiced.guhs.feature.vadsrijk.zone3.systemen.client.Z3SystemenClient.init(modBus);   // zone 3 (z3-systemen)
        nl.juiced.guhs.feature.vadsrijk.items.client.ItemsClient.init(modBus);   // T-ITEMS: drankjesriem, item tooltips
        nl.juiced.guhs.feature.vadsrijk.zone3.wezens.client.Z3WezensClient.init(modBus);   // zone 3 (z3-wezens)
        nl.juiced.guhs.feature.vadsrijk.zone4.wezens.client.Z4WezensClient.init(modBus);   // T-Z4WEZENS
        nl.juiced.guhs.feature.vadsrijk.zone4.bazen.client.Z4BazenClient.init(modBus);   // T-Z4WEZENS
        nl.juiced.guhs.feature.vadsrijk.zone5.wezens.client.Z5WezensClient.init(modBus);   // T-Z5WEZENS: zone-5 combat
        nl.juiced.guhs.feature.vadsrijk.zone5.droom.client.Z5DroomClient.init(modBus);   // T-DROOM: the Grauwe Droom
        nl.juiced.guhs.feature.vadsrijk.vriendjes.client.VriendjesClient.init(modBus);   // N-EXTRA: the pink dragon + critters
        nl.juiced.guhs.feature.vadsrijk.dialoog.client.DialoogClient.init(modBus);   // T-DIALOOG: the Vadsrijk talking screen
        nl.juiced.guhs.feature.vadsrijk.geluid.client.GeluidClient.init(modBus);   // T-GELUID (after kern: wraps its title receiver)
        nl.juiced.guhs.feature.vadsrijk.vfx.client.VfxClient.init(modBus);   // T-VFX: decals, particles, cast bar, VFX level
        nl.juiced.guhs.feature.vadsrijk.zone4.boot.client.BootClient.init(modBus);   // T-Z4BOOT: the Zeilbootje (renderer, screen, tab, wind HUD)
        nl.juiced.guhs.feature.vadsrijk.ambacht.client.AmbachtClient.init(modBus);   // T-AMBACHT: the Ambachten (key Y, spots, screens)
        nl.juiced.guhs.feature.vadsrijk.opslag.client.OpslagClient.init(modBus);   // T-OPSLAG: knapzak/tas/bank screens + HUD additions
        nl.juiced.guhs.feature.vadsrijk.kleding.client.KledingClient.init(modBus);   // T-KLEDING: Kledingkast (key O), wings, effects
        nl.juiced.guhs.feature.vadsrijk.luchtschip.client.LuchtschipClient.init(modBus);   // T-LUCHTSCHIP: airships + silhouettes
        nl.juiced.guhs.feature.vadsrijk.karakter.client.KarakterClient.init(modBus);   // T-KARAKTER: character screen + badges
        nl.juiced.guhs.feature.vadsrijk.menu.client.MenuClient.init(modBus);   // U-TABS: the Vadsrijk-menu (key H, all tabs in one frame; keep last)
    }
}
