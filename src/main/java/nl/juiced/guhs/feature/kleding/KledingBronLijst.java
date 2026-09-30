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
        b("kleermaker", RED_BOWTIE, PRIJS_STRIK + " smaragden");
        b("kleermaker", BLACK_BOWTIE, PRIJS_STRIK + " smaragden");
        b("kleermaker", SUNGLASSES, PRIJS_ZONNEBRIL + " smaragden");
        b("kleermaker", RAIN_HAT, PRIJS_REGENHOED + " smaragden");
        b("kleermaker", STRIPED_SWEATER, PRIJS_TRUI + " smaragden");
        b("kleermaker", RAINCOAT, PRIJS_REGENJAS + " smaragden");
        b("kleermaker", OORSTRIKJE_ROZE, PRIJS_OORSTRIKJE + " smaragden");
        b("kleermaker", OORSTRIKJE_MINT, PRIJS_OORSTRIKJE + " smaragden");
        b("kleermaker", OORSTRIKJE_GEEL, PRIJS_OORSTRIKJE + " smaragden");
        // --- moved (2.9) ---
        b("bakkerij", CHEF_HAT, KledingFeature.PRIJS_KOKSMUTS + " bakmunten bij Bakker Korstje");
        b("bakkerij", CHEF_JACKET, KledingFeature.PRIJS_KOKSBUIS + " bakmunten bij Bakker Korstje");
        b("boerderij", STRAW_HAT, "je eerste klusje voor Boerin Hooibaal");
        b("boerderij", OVERALLS, KledingFeature.OVERALL_KLUSJES + " klusjes voor Boerin Hooibaal");
        b("beroep_brandweer", FIREFIGHTER_HELMET, "help Brandweercommandant Blusguh");
        b("beroep_brandweer", FIREFIGHTER_JACKET, "help Brandweercommandant Blusguh");
        b("beroep_politie", POLICE_CAP, "los de Knabbeldief-zaak op");
        b("beroep_politie", POLICE_UNIFORM, "los de Knabbeldief-zaak op");
        b("beroep_apotheek", DOCTOR_COAT, "help Dokter Snotneus-guh");
        b("beroep_apotheek", STETHOSCOPE, "help Dokter Snotneus-guh");
        b("beroep_bouw", BUILDER_HELMET, "help Bob de Guhbouwer");
        b("beroep_bouw", SAFETY_VEST, "help Bob de Guhbouwer");
        b("loot_picknick", PARTY_HAT, "in de picknickmand");
        b("guhdex", HEART_GLASSES, "5 guhs gezien in de Guhdex");
        b("guhdex", MONOCLE, "8 guhs gezien en 3 getemd");
        b("guhdex", ROYAL_CROWN, "de hele Guhdex vol");
        b("brococolief", PINK_ONESIE, "tem de geheime Brococolief-guh");
        b("crafting", GUH_BACKPACK, "zelf maken: touw, leer, kist en roze wol");
        b("loot_kasteel", KONING_KROON, "de schatkamer van het guhkasteel");
        b("loot_kasteel", KONING_MANTEL, "de schatkamer van het guhkasteel");
        b("loot_kasteel", KONING_KETTING, "de schatkamer van het guhkasteel");
        b("loot_eilanden", WOLKENMUTS, "de wolkenkist op de zwevende eilandjes");
        b("loot_eilanden", WOLKENKRAAG, "de wolkenkist op de zwevende eilandjes");
        b("loot_grotten", KNIGHT_HELMET, "schatkisten in grotten en kasteel");
        b("loot_grotten", KNIGHT_ARMOUR, "schatkisten in grotten en kasteel");
        b("barbecuether", GRILL_KOKSMUTS, "24 kaasknabbels bij de Grillguh");
        b("vadswoud", BOSWACHTERSHOED, "10 kaasknabbels bij de Boswachterguh");
        b("vadswoud", PLUKMUTS, "20 knabbelbessen bij de Knabbelplukker");
        // --- loot, as it was ---
        b("loot_picknick", SANTA_HAT, "in de picknickmand");
        b("loot_picknick", CHRISTMAS_SWEATER, "in de picknickmand");
        b("loot_picknick", ORANGE_CROWN, "in de picknickmand");
        b("loot_picknick", ORANGE_SHIRT, "in de picknickmand");
        b("loot_hamsterhuis", SINT_MITRE, "schatkisten in hamsterhuizen");
        b("loot_hamsterhuis", PIET_BERET, "schatkisten in hamsterhuizen");
        b("loot_hamsterhuis", WINTER_SCARF, "schatkisten in hamsterhuizen");
        b("loot_mikahuis", WITCH_HAT, "kisten in Mika-huizen");
        b("loot_mikahuis", GHOST_SHEET, "kisten in Mika-huizen");
        b("loot_mikahuis", PUMPKIN_HEAD, "kisten in Mika-huizen");
        b("loot_grotten", PIRATE_HAT, "schatkisten in de guhgrotten");
        b("loot_grotten", EYEPATCH, "schatkisten in de guhgrotten");
        b("loot_guhramid", ROYAL_CAPE, "de schatten van de guhramide");
        b("loot_guhramid", WIZARD_HAT, "de schatten van de guhramide");
        b("loot_guhramid", WIZARD_ROBE, "de schatten van de guhramide");
        // --- the minigames and features: their own shops and rewards ---
        b("kermis", KERMIS_STRIK, "2 kermisbonnen");
        b("kermis", KERMIS_HOED, "4 kermisbonnen");
        b("kermis", KERMIS_JASJE, "6 kermisbonnen");
        b("verstop", DETECTIVE_VERGROOTGLAS, "2 verstopguhtickets");
        b("verstop", DETECTIVE_PET, "3 verstopguhtickets");
        b("verstop", DETECTIVE_JAS, "5 verstopguhtickets");
        b("beauty", SHOWSTER_STRIK, "6 showrozetten");
        b("beauty", SHOWSTER_SJERP, "10 showrozetten");
        b("beauty", SHOWSTER_TIARA, "16 showrozetten");
        b("race", RACEBRIL, "3 raceprijsjes");
        b("race", JOCKEY_PET, "5 raceprijsjes");
        b("race", JOCKEY_JASJE, "7 raceprijsjes");
        b("meppen", MIKAMEPPER_MEDAILLE, "4 mepmunten");
        b("meppen", MIKAJAGER_HOED, "6 mepmunten");
        b("meppen", MIKAJAGER_VEST, "8 mepmunten");
        b("disco", DISCO_BRIL, "4 discomunten");
        b("disco", DISCO_AFRO, "6 discomunten");
        b("disco", DISCO_GLITTERPAK, "10 discomunten");
        b("golf", GOLF_ZONNEKLEP, "8 golfballetjes");
        b("golf", GOLF_PET, "10 golfballetjes");
        b("golf", GOLF_TRUI, "16 golfballetjes");
        b("smul", SMUL_SLABBETJE, "5 smulmunten");
        b("smul", SMUL_BAKKERSMUTS, "7 smulmunten");
        b("smul", SMUL_SCHORT, "9 smulmunten");
        b("vissen", VISSERSHOEDJE, "6 visbonnen");
        b("vissen", VIS_AAN_DE_HAAK, "8 visbonnen");
        b("vissen", VISVEST, "10 visbonnen");
        b("kaasmijn", KAASMIJN_ZAKDOEK, "6 kaasbrokken");
        b("kaasmijn", KAASMIJN_HELM, "10 kaasbrokken");
        b("kaasmijn", KAASMIJN_OVERALL, "16 kaasbrokken");
        b("bibliotheek", BIEB_LEESBRIL, "3 boekenbonnen");
        b("bibliotheek", BIEB_VEST, "5 boekenbonnen");
        b("bibliotheek", BIEB_HOED, "8 boekenbonnen");
        b("onderwater", DUIKBRIL, "4 parels");
        b("onderwater", SNORKEL, "5 parels");
        b("onderwater", ZWEMBAND, "7 parels");
        b("vadsparade", VADSPARADE_SJAKO, "loop de Vadsparade helemaal mee");
        b("vadsparade", VADSPARADE_JASJE, "loop de Vadsparade helemaal mee");
        b("vadsparade", VADSPARADE_TROMMELTJE, "loop de Vadsparade helemaal mee");
        b("vadswoud", BOSWACHTERSJAS, "14 kaasknabbels bij de Boswachterguh");
        b("vadswoud", PLUKMANDJE, "28 knabbelbessen bij de Knabbelplukker");
        b("barbecuether", GRILL_HALSDOEK, "16 kaasknabbels bij de Grillguh");
        b("barbecuether", GRILL_SCHORT, "32 kaasknabbels bij de Grillguh");
        // --- Knuffeldal (as 2.8 made them) ---
        b("knuffeldal", BURGEMEESTERSSJERP, "de finale van het Grote Knusfeest");
        b("knuffeldal", BLOESEMKRANSJE, "de lente in Knuffeldal");
        b("knuffeldal", ZONNEHOEDJE, "de zomer in Knuffeldal");
        b("knuffeldal", KNUS_SJAALTJE, "de winter in Knuffeldal");
        b("bakkerij", BAKKERSMUTSJE, "5 bakmunten bij Bakker Korstje");
        b("bakkerij", BAKKERSSCHORTJE, "7 bakmunten bij Bakker Korstje");
        b("bakkerij", MEELSTRIKJE, "4 bakmunten bij Bakker Korstje");
        b("creche", BABYMUTSJE, "5 speenmunten");
        b("creche", ROMPERTJE, "8 speenmunten");
        b("creche", SPEENKETTINKJE, "4 speenmunten");
        b("theehuis", THEEMUTSJE, "je eerste gezellige theekransje");
        b("kapper", KAPPERSCAPE, "10 krulmunten");
        b("boerderij", BOERDERIJ_HOEDJE, "10 kaasknabbels bij Boerin Hooibaal");
        b("boerderij", BOERDERIJ_ZAKDOEK, "8 kaasknabbels bij Boerin Hooibaal");
        b("tuintjes", TUINHOEDJE, "10 kaasknabbels bij Boerin Hooibaal");
        b("tuintjes", TUINSCHORTJE, "12 kaasknabbels bij Boerin Hooibaal");
        b("sterrenwacht", STERRENKIJKERSMUTS, "4 wenssterren");
        b("sterrenwacht", STERRENCAPE, "6 wenssterren");
        b("ballon", BALLONBRIL, "4 ballonmunten");
        b("ballon", BALLONPET, "5 ballonmunten");
        b("kamperen", SLAAPMUTSJE, "10 kaasknabbels bij Opa Guh");
        b("kamperen", PYJAMA_PAKJE, "14 kaasknabbels bij Opa Guh");
        b("knuffelbad", BADMUTSJE, "4 eendjesmunten");
        b("knuffelbad", BADJASJE, "8 eendjesmunten");
        b("wereldleven", KOORSTRIKJE, "zing alle liedjes van het koortje");
        b("wereldleven", IJSCOPETJE, "12 kaasknabbels bij IJscoguh Tingeling");
    }

    private static void b(String bron, GuhClothes c, String prijs) {
        KledingBronnen.bron(c, bron, prijs);
    }

    private KledingBronLijst() {
    }
}
