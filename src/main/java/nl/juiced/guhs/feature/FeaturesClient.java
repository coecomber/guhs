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
        nl.juiced.guhs.feature.einde2.client.Einde2Client.init(modBus);   // einde2: the Guheinde expansion, see feature/einde2/Einde2
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
        nl.juiced.guhs.feature.bio.client.BioClient.init(modBus);   // biomes3
        nl.juiced.guhs.feature.park.client.ParkClient.init(modBus);   // 1.4.2: Het Knabbelpark
        // --- guhpixel ---
        nl.juiced.guhs.feature.guhpixel.client.GuhpixelClient.init(modBus);
        // --- bbq2 ---
        nl.juiced.guhs.feature.vadskracht.client.VadskrachtClient.init(modBus);
        nl.juiced.guhs.feature.wereld.client.WereldClient.init(modBus);
        nl.juiced.guhs.feature.bank.client.BankClient.init(modBus);
        nl.juiced.guhs.feature.techbron.client.TechbronClient.init(modBus);
        nl.juiced.guhs.feature.techbuis.client.TechbuisClient.init(modBus);
        nl.juiced.guhs.feature.techmachine.client.TechmachineClient.init(modBus);
        nl.juiced.guhs.feature.techsaus.client.TechsausClient.init(modBus);
        nl.juiced.guhs.feature.techbezorg.client.TechbezorgClient.init(modBus);
        nl.juiced.guhs.feature.techklus.client.TechklusClient.init(modBus);
        nl.juiced.guhs.feature.techquest.client.TechquestClient.init(modBus);
        nl.juiced.guhs.feature.paleizen.client.PaleizenClient.init(modBus);
        nl.juiced.guhs.feature.bestaand.client.BestaandClient.init(modBus);
        nl.juiced.guhs.feature.fossielmijn.client.FossielmijnClient.init(modBus);
        nl.juiced.guhs.feature.sausdieren.client.SausdierenClient.init(modBus);
        nl.juiced.guhs.feature.campingmarkt.client.CampingmarktClient.init(modBus);
        nl.juiced.guhs.feature.torenpeper.client.TorenpeperClient.init(modBus);
        nl.juiced.guhs.feature.ring.client.RingClient.init(modBus);
        nl.juiced.guhs.feature.ringh1.client.RingH1Client.init(modBus);
        nl.juiced.guhs.feature.ringh2.client.RingH2Client.init(modBus);
        nl.juiced.guhs.feature.ringh3.client.RingH3Client.init(modBus);
        nl.juiced.guhs.feature.ringh4.client.RingH4Client.init(modBus);
        nl.juiced.guhs.feature.ringh5.client.RingH5Client.init(modBus);
        nl.juiced.guhs.feature.ringh6.client.RingH6Client.init(modBus);
        nl.juiced.guhs.feature.ringsausuman.client.RingSausumanClient.init(modBus);
        nl.juiced.guhs.feature.guhrio.client.GuhrioClient.init(modBus);
        nl.juiced.guhs.feature.guhriow1.client.GuhrioW1Client.init(modBus);
        nl.juiced.guhs.feature.guhriow2.client.GuhrioW2Client.init(modBus);
        nl.juiced.guhs.feature.guhriow3.client.GuhrioW3Client.init(modBus);
        nl.juiced.guhs.feature.guhriobeloning.client.GuhrioBeloningClient.init(modBus);
        nl.juiced.guhs.feature.ringknipoog.client.RingKnipoogClient.init(modBus);
        nl.juiced.guhs.feature.guhpad.client.GuhpadClient.init(modBus);   // --- verhalenpad: het Guhpad ---
        nl.juiced.guhs.feature.oudescenes.client.OudeScenesClient.init(modBus);   // bbq2 verhalenpad: oude-scenes
        nl.juiced.guhs.feature.snuffel.client.SnuffelClient.init(modBus);   // verhalenpad: snuffel-kern
        nl.juiced.guhs.feature.snuffelsteiger.client.SnuffelsteigerClient.init(modBus);   // verhalenpad: snuffel-steiger
        nl.juiced.guhs.feature.ringvlucht.client.RingVluchtClient.init(modBus);   // 1.4.2: the Rookguh-vlucht of the Knabbelring
        nl.juiced.guhs.feature.safari.client.SafariClient.init(modBus);   // 1.6.0 Op Safari!
        nl.juiced.guhs.feature.safari.spullen.client.SafariSfeer.init(modBus);   // 1.6.0 Op Safari! S6: zone ambience + lobby music
        nl.juiced.guhs.feature.safari.camera.client.CameraClient.init(modBus);   // 1.6.0 Op Safari! S1: camera frame, flash, photo card
        nl.juiced.guhs.feature.safari.lobby.client.LobbyClient.init(modBus);   // 1.6.0 Op Safari! S2 (lobby)
        nl.juiced.guhs.feature.safari.verblijf.client.VerblijfClient.init(modBus);   // 1.6.0 Op Safari! S5
        nl.juiced.guhs.feature.safari.rit.client.RitClient.init(modBus);   // 1.6.0 S3: truck, kano, kabelbaan, struisvogelrace
        nl.juiced.guhs.feature.safari.momenten.client.SfeerClient.init(modBus);   // 1.6.0 S3: weather + atmosphere per zone
        nl.juiced.guhs.feature.safari.quests.client.SafariQuestsClient.init(modBus);   // 1.6.0 Op Safari! S4: glowing footprints
        nl.juiced.guhs.feature.knabbelvelden.client.KnabbelveldenClient.init(modBus);   // 1.6.0: De Eeuwige Knabbelvelden
        nl.juiced.guhs.feature.bouwhulp.client.BouwhulpClient.init(modBus);   // 1.6.0 deel 2: ghosts, the Bouwkeet
        nl.juiced.guhs.feature.bouwhulp.client.GereedschapClient.init(modBus);   // 1.6.0 deel 2 F9: tool screens, radial menu, previews
        nl.juiced.guhs.feature.onderwol.wezens.client.OnderWolWezensClient.init(modBus);   // 1.7.0 Onder de Wol: the cave glimworm, the Grotguh's glow
        nl.juiced.guhs.feature.toveren.client.ToverenClient.init(modBus);   // 1.7.0: the Glinsteraltaar's book, hearts, fluff
        nl.juiced.guhs.feature.mijnhulp.client.MijnhulpClient.init(modBus);   // 1.7.0 Onder de Wol: the Wolkenliftje's keys, tooltips
        nl.juiced.guhs.feature.guhvillagers.client.GuhVillagersClient.init(modBus);   // 1.7.0: the new guh villager look, the IJzerguhlem
        nl.juiced.guhs.feature.mijnhuisje.client.MijnhuisjeClient.init(modBus);   // 1.7.0 Onder de Wol: the Mijn-guhhuisje's screen
        nl.juiced.guhs.feature.grotspellen.client.GrotSpellenClient.init(modBus);   // 1.7.0 Onder de Wol: glimworms, the jar on your back, glowing clothes
        nl.juiced.guhs.feature.mijnspellen.client.MijnspellenClient.init(modBus);   // 1.7.0 Onder de Wol: ore carts, pins + ball (minigames2)
        nl.juiced.guhs.feature.blub.client.BlubClient.init(modBus);   // 1.8.0 (Guhdromenland): the Blub-guh's look and menu
        nl.juiced.guhs.feature.weer.client.WeerClient.init(modBus);   // 1.8.0: the Weer vadser's weather, the guh-weertjes, its menu
        nl.juiced.guhs.feature.slaapzorg.client.SlaapzorgClient.init(modBus);   // 1.8.0: the Mobiel's turning moon and stars
        nl.juiced.guhs.feature.droomwezens.client.DroomwezensClient.init(modBus);   // 1.8.0: the dream NPCs' models, the sterrenvisje
        nl.juiced.guhs.feature.sterrenveld.client.SterrenveldClient.init(modBus);   // 1.8.0: the falling stars
        nl.juiced.guhs.feature.molentje.client.MolentjeClient.init(modBus);   // 1.8.0: the Maanmolentje's turning sails
        nl.juiced.guhs.feature.zingendeweide.client.WeideClient.init(modBus);   // 1.8.0: De Zingende Weide's sky, trillertjes, bubbelsap
    }

    private FeaturesClient() {
    }
}
