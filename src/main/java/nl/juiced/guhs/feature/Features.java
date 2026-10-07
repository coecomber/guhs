package nl.juiced.guhs.feature;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * The big features of 2.4 (the minigames and the rare structures), each in its own package nl.juiced.guhs.feature.*:
 * each one registers its own blocks, items, entities... (register), its network messages (payloads), its creative tab
 * items (creative) and the role of its guh character (role). Resources come from tools/features/*.py.
 */
public final class Features {
    public static void register(IEventBus modBus) {
        Minigames.register();
        Protected.register();
        Protected.add(nl.juiced.guhs.world.VerstopProtection::protectedAt);
        Loaned.register();
        nl.juiced.guhs.quest.Kermis.register();               // 1.2.7: the kermis can't be broken and keeps its sleds
        nl.juiced.guhs.feature.knus.Knus.register(modBus);   // 2.8: the shared Knus framework, before every 2.8 feature
        nl.juiced.guhs.feature.beauty.BeautyFeature.register(modBus);
        nl.juiced.guhs.feature.race.RaceFeature.register(modBus);
        nl.juiced.guhs.feature.meppen.MeppenFeature.register(modBus);
        nl.juiced.guhs.feature.disco.DiscoFeature.register(modBus);
        nl.juiced.guhs.feature.golf.GolfFeature.register(modBus);
        nl.juiced.guhs.feature.smul.SmulFeature.register(modBus);
        nl.juiced.guhs.feature.vissen.VissenFeature.register(modBus);
        nl.juiced.guhs.feature.eilanden.EilandenFeature.register(modBus);
        nl.juiced.guhs.feature.kaasmijn.KaasmijnFeature.register(modBus);
        nl.juiced.guhs.feature.bibliotheek.BibliotheekFeature.register(modBus);
        nl.juiced.guhs.feature.evenementen.EvenementenFeature.register(modBus);
        nl.juiced.guhs.feature.emotes.EmotesFeature.register(modBus);
        nl.juiced.guhs.feature.onderwater.OnderwaterFeature.register(modBus);
        nl.juiced.guhs.feature.guheinde.GuheindeFeature.register(modBus);
        nl.juiced.guhs.feature.gatenkaas.GatenkaasFeature.register(modBus);
        nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature.register(modBus);
        nl.juiced.guhs.feature.vadswoud.VadswoudFeature.register(modBus);
        nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.register(modBus);
        nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature.register(modBus);
        // --- 2.8 (Knuffeldal) ---
        nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature.register(modBus);
        nl.juiced.guhs.feature.bakkerij.BakkerijFeature.register(modBus);
        nl.juiced.guhs.feature.creche.CrecheFeature.register(modBus);
        nl.juiced.guhs.feature.theehuis.TheehuisFeature.register(modBus);
        nl.juiced.guhs.feature.kapper.KapperFeature.register(modBus);
        nl.juiced.guhs.feature.boerderij.BoerderijFeature.register(modBus);
        nl.juiced.guhs.feature.tuintjes.TuintjesFeature.register(modBus);
        nl.juiced.guhs.feature.sterrenwacht.SterrenwachtFeature.register(modBus);
        nl.juiced.guhs.feature.ballon.BallonFeature.register(modBus);
        nl.juiced.guhs.feature.kamperen.KamperenFeature.register(modBus);
        nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature.register(modBus);
        nl.juiced.guhs.feature.wereldleven.WereldlevenFeature.register(modBus);
        nl.juiced.guhs.feature.piep.PiepFeature.register(modBus);   // 2.8.1 Piep
        // --- 2.9 (De Grote Guhspelen): the shared framework first, then the features ---
        nl.juiced.guhs.feature.spelen.SpelenFeature.register(modBus);
        nl.juiced.guhs.feature.kleding.KledingFeature.register(modBus);
        nl.juiced.guhs.feature.gids.GidsFeature.register(modBus);
        nl.juiced.guhs.feature.titels.TitelsFeature.register(modBus);   // 1.2.6: the titles
        nl.juiced.guhs.feature.sjoelen.SjoelenFeature.register(modBus);
        nl.juiced.guhs.feature.doolhof.DoolhofFeature.register(modBus);
        nl.juiced.guhs.feature.katapult.KatapultFeature.register(modBus);
        nl.juiced.guhs.feature.knabbelspelen.KnabbelspelenFeature.register(modBus);
        nl.juiced.guhs.feature.guhpolder.GuhpolderFeature.register(modBus);
        nl.juiced.guhs.feature.elftocht.ElftochtFeature.register(modBus);
        nl.juiced.guhs.feature.circuit.CircuitFeature.register(modBus);
        nl.juiced.guhs.feature.beroepen.BeroepenFeature.register(modBus);
        // --- 2.10 (Lieve vadsjes van elkaar) ---
        nl.juiced.guhs.feature.band.BandFeature.register(modBus);
        nl.juiced.guhs.feature.huisje.HuisjeFeature.register(modBus);
        nl.juiced.guhs.feature.klusjes.KlusjesFeature.register(modBus);
        nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature.register(modBus);
        nl.juiced.guhs.feature.guhkamer.GuhkamerFeature.register(modBus);
        nl.juiced.guhs.feature.samen.SamenFeature.register(modBus);
        nl.juiced.guhs.feature.favorietjes.FavorietjesFeature.register(modBus);
        // --- 3.0 (Guhverhalen) ---
        nl.juiced.guhs.feature.verhaal.VerhaalFeature.register(modBus);
        nl.juiced.guhs.feature.timmerguh.TimmerguhFeature.register(modBus);
        nl.juiced.guhs.feature.balto.BaltoFeature.register(modBus);
        nl.juiced.guhs.feature.baltoslee.BaltoSleeFeature.register(modBus);
        nl.juiced.guhs.feature.mewtwo.MewtwoFeature.register(modBus);
        nl.juiced.guhs.feature.hemel.HemelFeature.register(modBus);
        nl.juiced.guhs.feature.guhwaii.GuhwaiiFeature.register(modBus);
        nl.juiced.guhs.feature.guhwaiispellen.GuhwaiiSpellenFeature.register(modBus);
        nl.juiced.guhs.feature.vogels.VogelsFeature.register(modBus);
        nl.juiced.guhs.feature.waterdiertjes.WaterdiertjesFeature.register(modBus);
        nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature.register(modBus);
        // --- 1.2.0 ---
        nl.juiced.guhs.feature.weerder.WeerderFeature.register(modBus);
        // --- 1.2.5 ---
        nl.juiced.guhs.feature.guhoven.GuhovenFeature.register(modBus);
        // --- 1.2.8: het Bleekwoud ---
        nl.juiced.guhs.feature.bleekwoud.BleekwoudFeature.register(modBus);
        // --- guhpixel: the kern and its nine slices ---
        nl.juiced.guhs.feature.guhpixel.GuhpixelFeature.register(modBus);
        // --- bbq2 ---
        nl.juiced.guhs.feature.vadskracht.VadskrachtFeature.register(modBus);
        nl.juiced.guhs.feature.wereld.WereldFeature.register(modBus);
        nl.juiced.guhs.feature.bank.BankFeature.register(modBus);
        nl.juiced.guhs.feature.techbron.TechbronFeature.register(modBus);
        nl.juiced.guhs.feature.techbuis.TechbuisFeature.register(modBus);
        nl.juiced.guhs.feature.techmachine.TechmachineFeature.register(modBus);
        nl.juiced.guhs.feature.techsaus.TechsausFeature.register(modBus);
        nl.juiced.guhs.feature.techbezorg.TechbezorgFeature.register(modBus);
        nl.juiced.guhs.feature.techklus.TechklusFeature.register(modBus);
        nl.juiced.guhs.feature.techquest.TechquestFeature.register(modBus);
        nl.juiced.guhs.feature.paleizen.PaleizenFeature.register(modBus);
        nl.juiced.guhs.feature.bestaand.BestaandFeature.register(modBus);
        nl.juiced.guhs.feature.fossielmijn.FossielmijnFeature.register(modBus);
        nl.juiced.guhs.feature.sausdieren.SausdierenFeature.register(modBus);
        nl.juiced.guhs.feature.campingmarkt.CampingmarktFeature.register(modBus);
        nl.juiced.guhs.feature.torenpeper.TorenpeperFeature.register(modBus);
        nl.juiced.guhs.feature.ring.RingFeature.register(modBus);
        nl.juiced.guhs.feature.ringh1.RingH1Feature.register(modBus);
        nl.juiced.guhs.feature.ringh2.RingH2Feature.register(modBus);
        nl.juiced.guhs.feature.ringh3.RingH3Feature.register(modBus);
        nl.juiced.guhs.feature.ringh4.RingH4Feature.register(modBus);
        nl.juiced.guhs.feature.ringh5.RingH5Feature.register(modBus);
        nl.juiced.guhs.feature.ringh6.RingH6Feature.register(modBus);
        nl.juiced.guhs.feature.ringsausuman.RingSausumanFeature.register(modBus);
        nl.juiced.guhs.feature.guhrio.GuhrioFeature.register(modBus);
        nl.juiced.guhs.feature.guhriow1.GuhrioW1Feature.register(modBus);
        nl.juiced.guhs.feature.guhriow2.GuhrioW2Feature.register(modBus);
        nl.juiced.guhs.feature.guhriow3.GuhrioW3Feature.register(modBus);
        nl.juiced.guhs.feature.guhriobeloning.GuhrioBeloningFeature.register(modBus);
        nl.juiced.guhs.feature.ringknipoog.RingKnipoogFeature.register(modBus);
        nl.juiced.guhs.feature.guhpad.GuhpadFeature.register(modBus);   // --- verhalenpad: het Guhpad ---
        nl.juiced.guhs.feature.oudescenes.OudeScenesFeature.register(modBus);   // bbq2 verhalenpad: oude-scenes
        nl.juiced.guhs.feature.snuffel.SnuffelFeature.register(modBus);   // verhalenpad: snuffel-kern
        nl.juiced.guhs.feature.snuffelsteiger.SnuffelsteigerFeature.register(modBus);   // verhalenpad: snuffel-steiger
        nl.juiced.guhs.feature.snuffeldorp.SnuffeldorpFeature.register(modBus);   // verhalenpad: snuffel-dorp
    }

    public static void payloads(PayloadRegistrar registrar) {
        nl.juiced.guhs.feature.beauty.BeautyFeature.payloads(registrar);
        nl.juiced.guhs.feature.race.RaceFeature.payloads(registrar);
        nl.juiced.guhs.feature.meppen.MeppenFeature.payloads(registrar);
        nl.juiced.guhs.feature.disco.DiscoFeature.payloads(registrar);
        nl.juiced.guhs.feature.golf.GolfFeature.payloads(registrar);
        nl.juiced.guhs.feature.smul.SmulFeature.payloads(registrar);
        nl.juiced.guhs.feature.vissen.VissenFeature.payloads(registrar);
        nl.juiced.guhs.feature.eilanden.EilandenFeature.payloads(registrar);
        nl.juiced.guhs.feature.kaasmijn.KaasmijnFeature.payloads(registrar);
        nl.juiced.guhs.feature.bibliotheek.BibliotheekFeature.payloads(registrar);
        nl.juiced.guhs.feature.evenementen.EvenementenFeature.payloads(registrar);
        nl.juiced.guhs.feature.emotes.EmotesFeature.payloads(registrar);
        nl.juiced.guhs.feature.onderwater.OnderwaterFeature.payloads(registrar);
        nl.juiced.guhs.feature.guheinde.GuheindeFeature.payloads(registrar);
        nl.juiced.guhs.feature.gatenkaas.GatenkaasFeature.payloads(registrar);
        nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature.payloads(registrar);
        nl.juiced.guhs.feature.vadswoud.VadswoudFeature.payloads(registrar);
        nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.payloads(registrar);
        nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature.payloads(registrar);
        // --- 2.8 (Knuffeldal) ---
        nl.juiced.guhs.feature.knus.Knus.payloads(registrar);
        nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature.payloads(registrar);
        nl.juiced.guhs.feature.bakkerij.BakkerijFeature.payloads(registrar);
        nl.juiced.guhs.feature.creche.CrecheFeature.payloads(registrar);
        nl.juiced.guhs.feature.theehuis.TheehuisFeature.payloads(registrar);
        nl.juiced.guhs.feature.kapper.KapperFeature.payloads(registrar);
        nl.juiced.guhs.feature.boerderij.BoerderijFeature.payloads(registrar);
        nl.juiced.guhs.feature.tuintjes.TuintjesFeature.payloads(registrar);
        nl.juiced.guhs.feature.sterrenwacht.SterrenwachtFeature.payloads(registrar);
        nl.juiced.guhs.feature.ballon.BallonFeature.payloads(registrar);
        nl.juiced.guhs.feature.kamperen.KamperenFeature.payloads(registrar);
        nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature.payloads(registrar);
        nl.juiced.guhs.feature.wereldleven.WereldlevenFeature.payloads(registrar);
        nl.juiced.guhs.feature.piep.PiepFeature.payloads(registrar);   // 2.8.1 Piep
        // --- 2.9 (De Grote Guhspelen): the shared framework first, then the features ---
        nl.juiced.guhs.feature.spelen.SpelenFeature.payloads(registrar);
        nl.juiced.guhs.feature.kleding.KledingFeature.payloads(registrar);
        nl.juiced.guhs.feature.gids.GidsFeature.payloads(registrar);
        nl.juiced.guhs.feature.titels.TitelsFeature.payloads(registrar);   // 1.2.6: the titles
        nl.juiced.guhs.feature.sjoelen.SjoelenFeature.payloads(registrar);
        nl.juiced.guhs.feature.doolhof.DoolhofFeature.payloads(registrar);
        nl.juiced.guhs.feature.katapult.KatapultFeature.payloads(registrar);
        nl.juiced.guhs.feature.knabbelspelen.KnabbelspelenFeature.payloads(registrar);
        nl.juiced.guhs.feature.guhpolder.GuhpolderFeature.payloads(registrar);
        nl.juiced.guhs.feature.elftocht.ElftochtFeature.payloads(registrar);
        nl.juiced.guhs.feature.circuit.CircuitFeature.payloads(registrar);
        nl.juiced.guhs.feature.beroepen.BeroepenFeature.payloads(registrar);
        // --- 2.10 (Lieve vadsjes van elkaar) ---
        nl.juiced.guhs.feature.band.BandFeature.payloads(registrar);
        nl.juiced.guhs.feature.huisje.HuisjeFeature.payloads(registrar);
        nl.juiced.guhs.feature.klusjes.KlusjesFeature.payloads(registrar);
        nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature.payloads(registrar);
        nl.juiced.guhs.feature.guhkamer.GuhkamerFeature.payloads(registrar);
        nl.juiced.guhs.feature.samen.SamenFeature.payloads(registrar);
        nl.juiced.guhs.feature.favorietjes.FavorietjesFeature.payloads(registrar);
        // --- 3.0 (Guhverhalen) ---
        nl.juiced.guhs.feature.verhaal.VerhaalFeature.payloads(registrar);
        nl.juiced.guhs.feature.timmerguh.TimmerguhFeature.payloads(registrar);
        nl.juiced.guhs.feature.balto.BaltoFeature.payloads(registrar);
        nl.juiced.guhs.feature.baltoslee.BaltoSleeFeature.payloads(registrar);
        nl.juiced.guhs.feature.mewtwo.MewtwoFeature.payloads(registrar);
        nl.juiced.guhs.feature.hemel.HemelFeature.payloads(registrar);
        nl.juiced.guhs.feature.guhwaii.GuhwaiiFeature.payloads(registrar);
        nl.juiced.guhs.feature.guhwaiispellen.GuhwaiiSpellenFeature.payloads(registrar);
        nl.juiced.guhs.feature.vogels.VogelsFeature.payloads(registrar);
        nl.juiced.guhs.feature.waterdiertjes.WaterdiertjesFeature.payloads(registrar);
        nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature.payloads(registrar);
        // --- 1.2.0 ---
        nl.juiced.guhs.feature.weerder.WeerderFeature.payloads(registrar);
        // --- guhpixel ---
        nl.juiced.guhs.feature.guhpixel.GuhpixelFeature.payloads(registrar);
        // --- bbq2 ---
        nl.juiced.guhs.feature.vadskracht.VadskrachtFeature.payloads(registrar);
        nl.juiced.guhs.feature.wereld.WereldFeature.payloads(registrar);
        nl.juiced.guhs.feature.bank.BankFeature.payloads(registrar);
        nl.juiced.guhs.feature.techbron.TechbronFeature.payloads(registrar);
        nl.juiced.guhs.feature.techbuis.TechbuisFeature.payloads(registrar);
        nl.juiced.guhs.feature.techmachine.TechmachineFeature.payloads(registrar);
        nl.juiced.guhs.feature.techsaus.TechsausFeature.payloads(registrar);
        nl.juiced.guhs.feature.techbezorg.TechbezorgFeature.payloads(registrar);
        nl.juiced.guhs.feature.techklus.TechklusFeature.payloads(registrar);
        nl.juiced.guhs.feature.techquest.TechquestFeature.payloads(registrar);
        nl.juiced.guhs.feature.paleizen.PaleizenFeature.payloads(registrar);
        nl.juiced.guhs.feature.bestaand.BestaandFeature.payloads(registrar);
        nl.juiced.guhs.feature.fossielmijn.FossielmijnFeature.payloads(registrar);
        nl.juiced.guhs.feature.sausdieren.SausdierenFeature.payloads(registrar);
        nl.juiced.guhs.feature.campingmarkt.CampingmarktFeature.payloads(registrar);
        nl.juiced.guhs.feature.torenpeper.TorenpeperFeature.payloads(registrar);
        nl.juiced.guhs.feature.ring.RingFeature.payloads(registrar);
        nl.juiced.guhs.feature.ringh1.RingH1Feature.payloads(registrar);
        nl.juiced.guhs.feature.ringh2.RingH2Feature.payloads(registrar);
        nl.juiced.guhs.feature.ringh3.RingH3Feature.payloads(registrar);
        nl.juiced.guhs.feature.ringh4.RingH4Feature.payloads(registrar);
        nl.juiced.guhs.feature.ringh5.RingH5Feature.payloads(registrar);
        nl.juiced.guhs.feature.ringh6.RingH6Feature.payloads(registrar);
        nl.juiced.guhs.feature.ringsausuman.RingSausumanFeature.payloads(registrar);
        nl.juiced.guhs.feature.guhrio.GuhrioFeature.payloads(registrar);
        nl.juiced.guhs.feature.guhriow1.GuhrioW1Feature.payloads(registrar);
        nl.juiced.guhs.feature.guhriow2.GuhrioW2Feature.payloads(registrar);
        nl.juiced.guhs.feature.guhriow3.GuhrioW3Feature.payloads(registrar);
        nl.juiced.guhs.feature.guhriobeloning.GuhrioBeloningFeature.payloads(registrar);
        nl.juiced.guhs.feature.ringknipoog.RingKnipoogFeature.payloads(registrar);
        nl.juiced.guhs.feature.guhpad.GuhpadFeature.payloads(registrar);   // --- verhalenpad: het Guhpad ---
        nl.juiced.guhs.feature.oudescenes.OudeScenesFeature.payloads(registrar);   // bbq2 verhalenpad: oude-scenes
        nl.juiced.guhs.feature.snuffel.SnuffelFeature.payloads(registrar);   // verhalenpad: snuffel-kern
        nl.juiced.guhs.feature.snuffelsteiger.SnuffelsteigerFeature.payloads(registrar);   // verhalenpad: snuffel-steiger
    }

    public static void creative(Consumer<ItemStack> output) {
        nl.juiced.guhs.feature.beauty.BeautyFeature.creative(output);
        nl.juiced.guhs.feature.race.RaceFeature.creative(output);
        nl.juiced.guhs.feature.meppen.MeppenFeature.creative(output);
        nl.juiced.guhs.feature.disco.DiscoFeature.creative(output);
        nl.juiced.guhs.feature.golf.GolfFeature.creative(output);
        nl.juiced.guhs.feature.smul.SmulFeature.creative(output);
        nl.juiced.guhs.feature.vissen.VissenFeature.creative(output);
        nl.juiced.guhs.feature.eilanden.EilandenFeature.creative(output);
        nl.juiced.guhs.feature.kaasmijn.KaasmijnFeature.creative(output);
        nl.juiced.guhs.feature.bibliotheek.BibliotheekFeature.creative(output);
        nl.juiced.guhs.feature.evenementen.EvenementenFeature.creative(output);
        nl.juiced.guhs.feature.emotes.EmotesFeature.creative(output);
        nl.juiced.guhs.feature.onderwater.OnderwaterFeature.creative(output);
        nl.juiced.guhs.feature.guheinde.GuheindeFeature.creative(output);
        nl.juiced.guhs.feature.gatenkaas.GatenkaasFeature.creative(output);
        nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature.creative(output);
        nl.juiced.guhs.feature.vadswoud.VadswoudFeature.creative(output);
        nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.creative(output);
        nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature.creative(output);
        // --- 2.8 (Knuffeldal) ---
        nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature.creative(output);
        nl.juiced.guhs.feature.bakkerij.BakkerijFeature.creative(output);
        nl.juiced.guhs.feature.creche.CrecheFeature.creative(output);
        nl.juiced.guhs.feature.theehuis.TheehuisFeature.creative(output);
        nl.juiced.guhs.feature.kapper.KapperFeature.creative(output);
        nl.juiced.guhs.feature.boerderij.BoerderijFeature.creative(output);
        nl.juiced.guhs.feature.tuintjes.TuintjesFeature.creative(output);
        nl.juiced.guhs.feature.sterrenwacht.SterrenwachtFeature.creative(output);
        nl.juiced.guhs.feature.ballon.BallonFeature.creative(output);
        nl.juiced.guhs.feature.kamperen.KamperenFeature.creative(output);
        nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature.creative(output);
        nl.juiced.guhs.feature.wereldleven.WereldlevenFeature.creative(output);
        nl.juiced.guhs.feature.piep.PiepFeature.creative(output);   // 2.8.1 Piep
        // --- 2.9 (De Grote Guhspelen): the shared framework first, then the features ---
        nl.juiced.guhs.feature.spelen.SpelenFeature.creative(output);
        nl.juiced.guhs.feature.kleding.KledingFeature.creative(output);
        nl.juiced.guhs.feature.gids.GidsFeature.creative(output);
        nl.juiced.guhs.feature.sjoelen.SjoelenFeature.creative(output);
        nl.juiced.guhs.feature.doolhof.DoolhofFeature.creative(output);
        nl.juiced.guhs.feature.katapult.KatapultFeature.creative(output);
        nl.juiced.guhs.feature.knabbelspelen.KnabbelspelenFeature.creative(output);
        nl.juiced.guhs.feature.guhpolder.GuhpolderFeature.creative(output);
        nl.juiced.guhs.feature.elftocht.ElftochtFeature.creative(output);
        nl.juiced.guhs.feature.circuit.CircuitFeature.creative(output);
        nl.juiced.guhs.feature.beroepen.BeroepenFeature.creative(output);
        // --- 2.10 (Lieve vadsjes van elkaar) ---
        nl.juiced.guhs.feature.band.BandFeature.creative(output);
        nl.juiced.guhs.feature.huisje.HuisjeFeature.creative(output);
        nl.juiced.guhs.feature.klusjes.KlusjesFeature.creative(output);
        nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature.creative(output);
        nl.juiced.guhs.feature.guhkamer.GuhkamerFeature.creative(output);
        nl.juiced.guhs.feature.samen.SamenFeature.creative(output);
        nl.juiced.guhs.feature.favorietjes.FavorietjesFeature.creative(output);
        // --- 3.0 (Guhverhalen) ---
        nl.juiced.guhs.feature.verhaal.VerhaalFeature.creative(output);
        nl.juiced.guhs.feature.timmerguh.TimmerguhFeature.creative(output);
        nl.juiced.guhs.feature.balto.BaltoFeature.creative(output);
        nl.juiced.guhs.feature.baltoslee.BaltoSleeFeature.creative(output);
        nl.juiced.guhs.feature.mewtwo.MewtwoFeature.creative(output);
        nl.juiced.guhs.feature.hemel.HemelFeature.creative(output);
        nl.juiced.guhs.feature.guhwaii.GuhwaiiFeature.creative(output);
        nl.juiced.guhs.feature.guhwaiispellen.GuhwaiiSpellenFeature.creative(output);
        nl.juiced.guhs.feature.vogels.VogelsFeature.creative(output);
        nl.juiced.guhs.feature.waterdiertjes.WaterdiertjesFeature.creative(output);
        nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature.creative(output);
        // --- 1.2.0 ---
        nl.juiced.guhs.feature.weerder.WeerderFeature.creative(output);
        // --- 1.2.5 ---
        nl.juiced.guhs.feature.guhoven.GuhovenFeature.creative(output);
        // --- 1.2.8: het Bleekwoud ---
        nl.juiced.guhs.feature.bleekwoud.BleekwoudFeature.creative(output);
        // --- guhpixel ---
        nl.juiced.guhs.feature.guhpixel.GuhpixelFeature.creative(output);
        // --- bbq2 ---
        nl.juiced.guhs.feature.vadskracht.VadskrachtFeature.creative(output);
        nl.juiced.guhs.feature.wereld.WereldFeature.creative(output);
        nl.juiced.guhs.feature.bank.BankFeature.creative(output);
        nl.juiced.guhs.feature.techbron.TechbronFeature.creative(output);
        nl.juiced.guhs.feature.techbuis.TechbuisFeature.creative(output);
        nl.juiced.guhs.feature.techmachine.TechmachineFeature.creative(output);
        nl.juiced.guhs.feature.techsaus.TechsausFeature.creative(output);
        nl.juiced.guhs.feature.techbezorg.TechbezorgFeature.creative(output);
        nl.juiced.guhs.feature.techklus.TechklusFeature.creative(output);
        nl.juiced.guhs.feature.techquest.TechquestFeature.creative(output);
        nl.juiced.guhs.feature.paleizen.PaleizenFeature.creative(output);
        nl.juiced.guhs.feature.bestaand.BestaandFeature.creative(output);
        nl.juiced.guhs.feature.fossielmijn.FossielmijnFeature.creative(output);
        nl.juiced.guhs.feature.sausdieren.SausdierenFeature.creative(output);
        nl.juiced.guhs.feature.campingmarkt.CampingmarktFeature.creative(output);
        nl.juiced.guhs.feature.torenpeper.TorenpeperFeature.creative(output);
        nl.juiced.guhs.feature.ring.RingFeature.creative(output);
        nl.juiced.guhs.feature.ringh1.RingH1Feature.creative(output);
        nl.juiced.guhs.feature.ringh2.RingH2Feature.creative(output);
        nl.juiced.guhs.feature.ringh3.RingH3Feature.creative(output);
        nl.juiced.guhs.feature.ringh4.RingH4Feature.creative(output);
        nl.juiced.guhs.feature.ringh5.RingH5Feature.creative(output);
        nl.juiced.guhs.feature.ringh6.RingH6Feature.creative(output);
        nl.juiced.guhs.feature.ringsausuman.RingSausumanFeature.creative(output);
        nl.juiced.guhs.feature.guhrio.GuhrioFeature.creative(output);
        nl.juiced.guhs.feature.guhriow1.GuhrioW1Feature.creative(output);
        nl.juiced.guhs.feature.guhriow2.GuhrioW2Feature.creative(output);
        nl.juiced.guhs.feature.guhriow3.GuhrioW3Feature.creative(output);
        nl.juiced.guhs.feature.guhriobeloning.GuhrioBeloningFeature.creative(output);
        nl.juiced.guhs.feature.ringknipoog.RingKnipoogFeature.creative(output);
        nl.juiced.guhs.feature.guhpad.GuhpadFeature.creative(output);   // --- verhalenpad: het Guhpad ---
        nl.juiced.guhs.feature.oudescenes.OudeScenesFeature.creative(output);   // bbq2 verhalenpad: oude-scenes
        nl.juiced.guhs.feature.snuffel.SnuffelFeature.creative(output);   // verhalenpad: snuffel-kern
        nl.juiced.guhs.feature.snuffelsteiger.SnuffelsteigerFeature.creative(output);   // verhalenpad: snuffel-steiger
    }

    /** Things the minigames only lend you (tools/make_v2.py: the item tag guhs:loaned): they never go into storage. */
    public static final TagKey<Item> LOANED = TagKey.create(Registries.ITEM, nl.juiced.guhs.Guhs.id("loaned"));

    /** Is this a loaned item (a golf club, a fishing rod, a smul bowl, a mep hammer, a leenhouweel...)? */
    public static boolean isLoaned(ItemStack stack) {
        return !stack.isEmpty() && stack.is(LOANED);
    }

    @Nullable
    public static NpcRole role(GuhNpcEntity.Kind kind) {
        return switch (kind) {
            case SHOWGUH -> nl.juiced.guhs.feature.beauty.BeautyFeature.role();
            case RACEGUH -> nl.juiced.guhs.feature.race.RaceFeature.role();
            case MEPGUH -> nl.juiced.guhs.feature.meppen.MeppenFeature.role();
            case DJGUH -> nl.juiced.guhs.feature.disco.DiscoFeature.role();
            case GOLFGUH -> nl.juiced.guhs.feature.golf.GolfFeature.role();
            case SMULGUH -> nl.juiced.guhs.feature.smul.SmulFeature.role();
            case VISGUH -> nl.juiced.guhs.feature.vissen.VissenFeature.role();
            case MIJNGUH -> nl.juiced.guhs.feature.kaasmijn.KaasmijnFeature.role();
            case BIBLIOTHECARIS -> nl.juiced.guhs.feature.bibliotheek.BibliotheekFeature.role();
            case ZEEMEERGUH -> nl.juiced.guhs.feature.onderwater.OnderwaterFeature.role();
            case BOSWACHTERGUH -> nl.juiced.guhs.feature.vadswoud.VadswoudFeature.boswachterguh();
            case KNABBELPLUKKER -> nl.juiced.guhs.feature.vadswoud.VadswoudFeature.knabbelplukker();
            case GRILLGUH -> nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.role();
            // --- 2.8 (Knuffeldal) ---
            case BURGEMEESTERGUH -> nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature.burgemeester();
            case COCOTJE -> nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature.cocotje();
            case BAKKERGUH -> nl.juiced.guhs.feature.bakkerij.BakkerijFeature.role();
            case JUF_KNUFFEL -> nl.juiced.guhs.feature.creche.CrecheFeature.role();
            case THEEGUH -> nl.juiced.guhs.feature.theehuis.TheehuisFeature.role();
            case KAPPERGUH -> nl.juiced.guhs.feature.kapper.KapperFeature.role();
            case BOERINNEGUH -> nl.juiced.guhs.feature.boerderij.BoerderijFeature.role();
            case STERRENKIJKERGUH -> nl.juiced.guhs.feature.sterrenwacht.SterrenwachtFeature.role();
            case BALLONGUH -> nl.juiced.guhs.feature.ballon.BallonFeature.role();
            case OPA_GUH -> nl.juiced.guhs.feature.kamperen.KamperenFeature.role();
            case BADMEESTERGUH -> nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature.role();
            // --- 2.9 (De Grote Guhspelen) ---
            case SJOELGUH -> nl.juiced.guhs.feature.sjoelen.SjoelenFeature.role();
            case DOOLHOFGUH -> nl.juiced.guhs.feature.doolhof.DoolhofFeature.role();
            case KATAPULTGUH -> nl.juiced.guhs.feature.katapult.KatapultFeature.role();
            case SPELLEIDERGUH -> nl.juiced.guhs.feature.knabbelspelen.KnabbelspelenFeature.role();
            case SCHAATSMEESTERGUH -> nl.juiced.guhs.feature.elftocht.ElftochtFeature.schaatsmeester();
            case STEMPELGUH -> nl.juiced.guhs.feature.elftocht.ElftochtFeature.stempelguh();
            case CIRCUITGUH -> nl.juiced.guhs.feature.circuit.CircuitFeature.role();
            case BRANDWEERGUH, POLITIEGUH, APOTHEKERGUH, BOUWVAKKERGUH -> nl.juiced.guhs.feature.beroepen.BeroepenFeature.role(kind);
            default -> nl.juiced.guhs.feature.verhaal.NpcRollen.rol(kind);   // 3.0: the story characters (null for older kinds)
        };
    }

    private Features() {
    }
}
