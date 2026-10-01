package nl.juiced.guhs.feature.kleding;

import static nl.juiced.guhs.entity.GuhClothes.*;

import nl.juiced.guhs.entity.GuhClothes;

/**
 * Where every clothing piece from before 2.9 comes from (DESIGN_29 §10.1: exactly one source per piece), registered in
 * {@link KledingBronnen} with the price or a hint for the Guhdex's Kleding tab. The 2.9 slices register their own pieces.
 * Hair (the kapsels, Slot.HAAR) isn't an unlock: it isn't here.
 */
public final class KledingBronLijst {
    /** The kleermaker's fixed offer (the everyday set + the ear bows), in emeralds. */
    public static final int PRIJS_STRIK = 4, PRIJS_ZONNEBRIL = 5, PRIJS_REGENHOED = 6, PRIJS_OORSTRIKJE = 4, PRIJS_TRUI = 10, PRIJS_REGENJAS = 10;

    static void registreer() {
        // --- the kleermaker (guh village): his own everyday set, a fixed full offer ---
        b("kleermaker", RED_BOWTIE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smaragden", PRIJS_STRIK));
        b("kleermaker", BLACK_BOWTIE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smaragden", PRIJS_STRIK));
        b("kleermaker", SUNGLASSES, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smaragden", PRIJS_ZONNEBRIL));
        b("kleermaker", RAIN_HAT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smaragden", PRIJS_REGENHOED));
        b("kleermaker", STRIPED_SWEATER, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smaragden", PRIJS_TRUI));
        b("kleermaker", RAINCOAT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smaragden", PRIJS_REGENJAS));
        b("kleermaker", OORSTRIKJE_ROZE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smaragden", PRIJS_OORSTRIKJE));
        b("kleermaker", OORSTRIKJE_MINT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smaragden", PRIJS_OORSTRIKJE));
        b("kleermaker", OORSTRIKJE_GEEL, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smaragden", PRIJS_OORSTRIKJE));
        // --- moved (2.9) ---
        b("bakkerij", CHEF_HAT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.bakmunten_bij_bakker_korstje", KledingFeature.PRIJS_KOKSMUTS));
        b("bakkerij", CHEF_JACKET, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.bakmunten_bij_bakker_korstje", KledingFeature.PRIJS_KOKSBUIS));
        b("boerderij", STRAW_HAT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.je_eerste_klusje_voor_boerin_hooibaal"));
        b("boerderij", OVERALLS, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.klusjes_voor_boerin_hooibaal", KledingFeature.OVERALL_KLUSJES));
        b("beroep_brandweer", FIREFIGHTER_HELMET, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.help_brandweercommandant_blusguh"));
        b("beroep_brandweer", FIREFIGHTER_JACKET, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.help_brandweercommandant_blusguh"));
        b("beroep_politie", POLICE_CAP, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.los_de_knabbeldief_zaak_op"));
        b("beroep_politie", POLICE_UNIFORM, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.los_de_knabbeldief_zaak_op"));
        b("beroep_apotheek", DOCTOR_COAT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.help_dokter_snotneus_guh"));
        b("beroep_apotheek", STETHOSCOPE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.help_dokter_snotneus_guh"));
        b("beroep_bouw", BUILDER_HELMET, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.help_bob_de_guhbouwer"));
        b("beroep_bouw", SAFETY_VEST, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.help_bob_de_guhbouwer"));
        b("loot_picknick", PARTY_HAT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.in_de_picknickmand"));
        b("guhdex", HEART_GLASSES, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.5_guhs_gezien_in_de_guhdex"));
        b("guhdex", MONOCLE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.8_guhs_gezien_en_3_getemd"));
        b("guhdex", ROYAL_CROWN, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_hele_guhdex_vol"));
        b("brococolief", PINK_ONESIE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.tem_de_geheime_brococolief_guh"));
        b("crafting", GUH_BACKPACK, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.zelf_maken_touw_leer_kist_en_roze_wol"));
        b("loot_kasteel", KONING_KROON, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_schatkamer_van_het_guhkasteel"));
        b("loot_kasteel", KONING_MANTEL, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_schatkamer_van_het_guhkasteel"));
        b("loot_kasteel", KONING_KETTING, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_schatkamer_van_het_guhkasteel"));
        b("loot_eilanden", WOLKENMUTS, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_wolkenkist_op_de_zwevende_eilandjes"));
        b("loot_eilanden", WOLKENKRAAG, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_wolkenkist_op_de_zwevende_eilandjes"));
        b("loot_grotten", KNIGHT_HELMET, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.schatkisten_in_grotten_en_kasteel"));
        b("loot_grotten", KNIGHT_ARMOUR, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.schatkisten_in_grotten_en_kasteel"));
        b("barbecuether", GRILL_KOKSMUTS, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_de_grillguh", 24));
        b("vadswoud", BOSWACHTERSHOED, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_de_boswachterguh", 10));
        b("vadswoud", PLUKMUTS, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.knabbelbessen_bij_de_knabbelplukker", 20));
        // --- loot, as it was ---
        b("loot_picknick", SANTA_HAT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.in_de_picknickmand"));
        b("loot_picknick", CHRISTMAS_SWEATER, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.in_de_picknickmand"));
        b("loot_picknick", ORANGE_CROWN, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.in_de_picknickmand"));
        b("loot_picknick", ORANGE_SHIRT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.in_de_picknickmand"));
        b("loot_hamsterhuis", SINT_MITRE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.schatkisten_in_hamsterhuizen"));
        b("loot_hamsterhuis", PIET_BERET, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.schatkisten_in_hamsterhuizen"));
        b("loot_hamsterhuis", WINTER_SCARF, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.schatkisten_in_hamsterhuizen"));
        b("loot_mikahuis", WITCH_HAT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.kisten_in_mika_huizen"));
        b("loot_mikahuis", GHOST_SHEET, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.kisten_in_mika_huizen"));
        b("loot_mikahuis", PUMPKIN_HEAD, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.kisten_in_mika_huizen"));
        b("loot_grotten", PIRATE_HAT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.schatkisten_in_de_guhgrotten"));
        b("loot_grotten", EYEPATCH, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.schatkisten_in_de_guhgrotten"));
        b("loot_guhramid", ROYAL_CAPE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_schatten_van_de_guhramide"));
        b("loot_guhramid", WIZARD_HAT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_schatten_van_de_guhramide"));
        b("loot_guhramid", WIZARD_ROBE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_schatten_van_de_guhramide"));
        // --- the minigames and features: their own shops and rewards ---
        b("kermis", KERMIS_STRIK, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kermisbonnen", 2));
        b("kermis", KERMIS_HOED, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kermisbonnen", 4));
        b("kermis", KERMIS_JASJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kermisbonnen", 6));
        b("verstop", DETECTIVE_VERGROOTGLAS, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.verstopguhtickets", 2));
        b("verstop", DETECTIVE_PET, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.verstopguhtickets", 3));
        b("verstop", DETECTIVE_JAS, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.verstopguhtickets", 5));
        b("beauty", SHOWSTER_STRIK, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.showrozetten", 6));
        b("beauty", SHOWSTER_SJERP, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.showrozetten", 10));
        b("beauty", SHOWSTER_TIARA, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.showrozetten", 16));
        b("race", RACEBRIL, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.raceprijsjes", 3));
        b("race", JOCKEY_PET, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.raceprijsjes", 5));
        b("race", JOCKEY_JASJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.raceprijsjes", 7));
        b("meppen", MIKAMEPPER_MEDAILLE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.mepmunten", 4));
        b("meppen", MIKAJAGER_HOED, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.mepmunten", 6));
        b("meppen", MIKAJAGER_VEST, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.mepmunten", 8));
        b("disco", DISCO_BRIL, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.discomunten", 4));
        b("disco", DISCO_AFRO, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.discomunten", 6));
        b("disco", DISCO_GLITTERPAK, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.discomunten", 10));
        b("golf", GOLF_ZONNEKLEP, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.golfballetjes", 8));
        b("golf", GOLF_PET, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.golfballetjes", 10));
        b("golf", GOLF_TRUI, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.golfballetjes", 16));
        b("smul", SMUL_SLABBETJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smulmunten", 5));
        b("smul", SMUL_BAKKERSMUTS, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smulmunten", 7));
        b("smul", SMUL_SCHORT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.smulmunten", 9));
        b("vissen", VISSERSHOEDJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.visbonnen", 6));
        b("vissen", VIS_AAN_DE_HAAK, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.visbonnen", 8));
        b("vissen", VISVEST, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.visbonnen", 10));
        b("kaasmijn", KAASMIJN_ZAKDOEK, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasbrokken", 6));
        b("kaasmijn", KAASMIJN_HELM, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasbrokken", 10));
        b("kaasmijn", KAASMIJN_OVERALL, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasbrokken", 16));
        b("bibliotheek", BIEB_LEESBRIL, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.boekenbonnen", 3));
        b("bibliotheek", BIEB_VEST, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.boekenbonnen", 5));
        b("bibliotheek", BIEB_HOED, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.boekenbonnen", 8));
        b("onderwater", DUIKBRIL, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.parels", 4));
        b("onderwater", SNORKEL, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.parels", 5));
        b("onderwater", ZWEMBAND, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.parels", 7));
        b("vadsparade", VADSPARADE_SJAKO, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.loop_de_vadsparade_helemaal_mee"));
        b("vadsparade", VADSPARADE_JASJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.loop_de_vadsparade_helemaal_mee"));
        b("vadsparade", VADSPARADE_TROMMELTJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.loop_de_vadsparade_helemaal_mee"));
        b("vadswoud", BOSWACHTERSJAS, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_de_boswachterguh", 14));
        b("vadswoud", PLUKMANDJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.knabbelbessen_bij_de_knabbelplukker", 28));
        b("barbecuether", GRILL_HALSDOEK, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_de_grillguh", 16));
        b("barbecuether", GRILL_SCHORT, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_de_grillguh", 32));
        // --- Knuffeldal (as 2.8 made them) ---
        b("knuffeldal", BURGEMEESTERSSJERP, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_finale_van_het_grote_knusfeest"));
        b("knuffeldal", BLOESEMKRANSJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_lente_in_knuffeldal"));
        b("knuffeldal", ZONNEHOEDJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_zomer_in_knuffeldal"));
        b("knuffeldal", KNUS_SJAALTJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.de_winter_in_knuffeldal"));
        b("bakkerij", BAKKERSMUTSJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.bakmunten_bij_bakker_korstje", 5));
        b("bakkerij", BAKKERSSCHORTJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.bakmunten_bij_bakker_korstje", 7));
        b("bakkerij", MEELSTRIKJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.bakmunten_bij_bakker_korstje", 4));
        b("creche", BABYMUTSJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.speenmunten", 5));
        b("creche", ROMPERTJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.speenmunten", 8));
        b("creche", SPEENKETTINKJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.speenmunten", 4));
        b("theehuis", THEEMUTSJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.je_eerste_gezellige_theekransje"));
        b("kapper", KAPPERSCAPE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.krulmunten", 10));
        b("boerderij", BOERDERIJ_HOEDJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_boerin_hooibaal", 10));
        b("boerderij", BOERDERIJ_ZAKDOEK, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_boerin_hooibaal", 8));
        b("tuintjes", TUINHOEDJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_boerin_hooibaal", 10));
        b("tuintjes", TUINSCHORTJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_boerin_hooibaal", 12));
        b("sterrenwacht", STERRENKIJKERSMUTS, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.wenssterren", 4));
        b("sterrenwacht", STERRENCAPE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.wenssterren", 6));
        b("ballon", BALLONBRIL, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.ballonmunten", 4));
        b("ballon", BALLONPET, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.ballonmunten", 5));
        b("kamperen", SLAAPMUTSJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_opa_guh", 10));
        b("kamperen", PYJAMA_PAKJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_opa_guh", 14));
        b("knuffelbad", BADMUTSJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.eendjesmunten", 4));
        b("knuffelbad", BADJASJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.eendjesmunten", 8));
        b("wereldleven", KOORSTRIKJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.zing_alle_liedjes_van_het_koortje"));
        b("wereldleven", IJSCOPETJE, nl.juiced.guhs.feature.kleding.KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.kaasknabbels_bij_ijscoguh_tingeling", 12));
    }

    private static void b(String bron, GuhClothes c, net.minecraft.network.chat.Component prijs) {
        KledingBronnen.bron(c, bron, prijs);
    }

    private KledingBronLijst() {
    }
}
