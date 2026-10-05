package nl.juiced.guhs.feature;

import net.neoforged.bus.api.IEventBus;

/** The client sides of the 2.4 features (see {@link Features}); only called on the client. */
public final class FeaturesClient {
    public static void init(IEventBus modBus) {
        nl.juiced.guhs.feature.beauty.client.BeautyClient.init(modBus);
        nl.juiced.guhs.feature.race.client.RaceClient.init(modBus);
        nl.juiced.guhs.feature.meppen.client.MeppenClient.init(modBus);
        nl.juiced.guhs.feature.disco.client.DiscoClient.init(modBus);
        nl.juiced.guhs.feature.golf.client.GolfClient.init(modBus);
        nl.juiced.guhs.feature.smul.client.SmulClient.init(modBus);
        nl.juiced.guhs.feature.vissen.client.VissenClient.init(modBus);
        nl.juiced.guhs.feature.eilanden.client.EilandenClient.init(modBus);
        nl.juiced.guhs.feature.kaasmijn.client.KaasmijnClient.init(modBus);
        nl.juiced.guhs.feature.bibliotheek.client.BibliotheekClient.init(modBus);
        nl.juiced.guhs.feature.evenementen.client.EvenementenClient.init(modBus);
        nl.juiced.guhs.feature.emotes.client.EmotesClient.init(modBus);
        nl.juiced.guhs.feature.onderwater.client.OnderwaterClient.init(modBus);
        nl.juiced.guhs.feature.guheinde.client.GuheindeClient.init(modBus);
        nl.juiced.guhs.feature.gatenkaas.client.GatenkaasClient.init(modBus);
        nl.juiced.guhs.feature.kaasmoeras.client.KaasmoerasClient.init(modBus);
        nl.juiced.guhs.feature.vadswoud.client.VadswoudClient.init(modBus);
        nl.juiced.guhs.feature.barbecuether.client.BarbecuetherClient.init(modBus);
        nl.juiced.guhs.feature.spiesburcht.client.SpiesburchtClient.init(modBus);
        // --- 2.8 (Knuffeldal) ---
        nl.juiced.guhs.feature.knus.client.KnusClient.init(modBus);
        nl.juiced.guhs.feature.knuffeldal.client.KnuffeldalClient.init(modBus);
        nl.juiced.guhs.feature.bakkerij.client.BakkerijClient.init(modBus);
        nl.juiced.guhs.feature.creche.client.CrecheClient.init(modBus);
        nl.juiced.guhs.feature.theehuis.client.TheehuisClient.init(modBus);
        nl.juiced.guhs.feature.kapper.client.KapperClient.init(modBus);
        nl.juiced.guhs.feature.boerderij.client.BoerderijClient.init(modBus);
        nl.juiced.guhs.feature.tuintjes.client.TuintjesClient.init(modBus);
        nl.juiced.guhs.feature.sterrenwacht.client.SterrenwachtClient.init(modBus);
        nl.juiced.guhs.feature.ballon.client.BallonClient.init(modBus);
        nl.juiced.guhs.feature.kamperen.client.KamperenClient.init(modBus);
        nl.juiced.guhs.feature.knuffelbad.client.KnuffelbadClient.init(modBus);
        nl.juiced.guhs.feature.wereldleven.client.WereldlevenClient.init(modBus);
        nl.juiced.guhs.feature.piep.client.PiepClient.init(modBus);   // 2.8.1 Piep
        // --- 2.9 (De Grote Guhspelen) ---
        nl.juiced.guhs.feature.spelen.client.SpelenClient.init(modBus);
        nl.juiced.guhs.feature.kleding.client.KledingClient.init(modBus);
        nl.juiced.guhs.feature.gids.client.GidsClient.init(modBus);
        nl.juiced.guhs.feature.titels.client.TitelsCache.init();   // 1.2.6: the titles
        nl.juiced.guhs.feature.sjoelen.client.SjoelenClient.init(modBus);
        nl.juiced.guhs.feature.doolhof.client.DoolhofClient.init(modBus);
        nl.juiced.guhs.feature.katapult.client.KatapultClient.init(modBus);
        nl.juiced.guhs.feature.knabbelspelen.client.KnabbelspelenClient.init(modBus);
        nl.juiced.guhs.feature.guhpolder.client.GuhpolderClient.init(modBus);
        nl.juiced.guhs.feature.elftocht.client.ElftochtClient.init(modBus);
        nl.juiced.guhs.feature.circuit.client.CircuitClient.init(modBus);
        nl.juiced.guhs.feature.beroepen.client.BeroepenClient.init(modBus);
        // --- 2.10 (Lieve vadsjes van elkaar) ---
        nl.juiced.guhs.feature.band.client.BandClient.init(modBus);
        nl.juiced.guhs.feature.huisje.client.HuisjeClient.init(modBus);
        nl.juiced.guhs.feature.klusjes.client.KlusjesClient.init(modBus);
        nl.juiced.guhs.feature.speelgoed.client.SpeelgoedClient.init(modBus);
        nl.juiced.guhs.feature.guhkamer.client.GuhkamerClient.init(modBus);
        nl.juiced.guhs.feature.samen.client.SamenClient.init(modBus);
        nl.juiced.guhs.feature.favorietjes.client.FavorietjesClient.init(modBus);
        // --- 3.0 (Guhverhalen) ---
        nl.juiced.guhs.feature.verhaal.client.VerhaalClient.init(modBus);
        nl.juiced.guhs.feature.timmerguh.client.TimmerguhClient.init(modBus);
        nl.juiced.guhs.feature.balto.client.BaltoClient.init(modBus);
        nl.juiced.guhs.feature.baltoslee.client.BaltoSleeClient.init(modBus);
        nl.juiced.guhs.feature.mewtwo.client.MewtwoClient.init(modBus);
        nl.juiced.guhs.feature.hemel.client.HemelClient.init(modBus);
        nl.juiced.guhs.feature.guhwaii.client.GuhwaiiClient.init(modBus);
        nl.juiced.guhs.feature.guhwaiispellen.client.GuhwaiiSpellenClient.init(modBus);
        nl.juiced.guhs.feature.vogels.client.VogelsClient.init(modBus);
        nl.juiced.guhs.feature.waterdiertjes.client.WaterdiertjesClient.init(modBus);
        nl.juiced.guhs.feature.landdiertjes.client.LanddiertjesClient.init(modBus);
        // --- 1.2.0 ---
        nl.juiced.guhs.feature.weerder.client.WeerderClient.init(modBus);
        // --- 1.2.5 ---
        nl.juiced.guhs.feature.guhoven.client.GuhovenClient.init(modBus);
        // --- 1.2.8: het Bleekwoud ---
        nl.juiced.guhs.feature.bleekwoud.client.BleekwoudClient.init(modBus);
        // --- guhpixel ---
        nl.juiced.guhs.feature.guhpixel.client.GuhpixelClient.init(modBus);
    }

    private FeaturesClient() {
    }
}
