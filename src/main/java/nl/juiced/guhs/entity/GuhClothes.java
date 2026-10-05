package nl.juiced.guhs.entity;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;

/**
 * Clothes you can put on your own tamed guh: one piece per slot (head, eyes, body, neck, back). They use the clothing
 * bones in the guh model, drawn with their own texture (textures/entity/guh_clothes/&lt;id&gt;.png); the bones and the
 * textures are made by tools/make_guh_variants.py. Body clothes never cover the feet.
 * <p>
 * Where they come from: the Guh kleermaker (basics + work outfits) and themed structure loot (holidays, fantasy).
 * Wild guhs only ever wear the four {@link #WILD_OUTFITS}.
 */
public enum GuhClothes {
    // --- the originals --------------------------------------------------------------------------------------------
    PINK_ONESIE(Slot.BODY, "outfit_suit"),
    STRIPED_SWEATER(Slot.BODY, "outfit_suit"),
    RAINCOAT(Slot.BODY, "outfit_suit"),
    CHEF_JACKET(Slot.BODY, "outfit_suit"),
    RAIN_HAT(Slot.HEAD, "outfit_rain_hat"),
    PARTY_HAT(Slot.HEAD, "outfit_party_hat", "outfit_party_pom"),
    CHEF_HAT(Slot.HEAD, "outfit_chef_hat"),
    RED_BOWTIE(Slot.NECK, "outfit_bowtie"),
    BLACK_BOWTIE(Slot.NECK, "outfit_bowtie"),
    // --- glasses ----------------------------------------------------------------------------------------------------
    SUNGLASSES(Slot.EYES, "outfit_glasses"),
    HEART_GLASSES(Slot.EYES, "outfit_glasses"),
    MONOCLE(Slot.EYES, "outfit_monocle"),
    EYEPATCH(Slot.EYES, "outfit_eyepatch"),
    // --- work outfits -------------------------------------------------------------------------------------------------
    FIREFIGHTER_HELMET(Slot.HEAD, "outfit_helmet"),
    FIREFIGHTER_JACKET(Slot.BODY, "outfit_suit"),
    POLICE_CAP(Slot.HEAD, "outfit_cap"),
    POLICE_UNIFORM(Slot.BODY, "outfit_suit"),
    DOCTOR_COAT(Slot.BODY, "outfit_suit"),
    STETHOSCOPE(Slot.NECK, "outfit_stethoscope"),
    BUILDER_HELMET(Slot.HEAD, "outfit_helmet"),
    SAFETY_VEST(Slot.BODY, "outfit_suit"),
    STRAW_HAT(Slot.HEAD, "outfit_rain_hat"),
    OVERALLS(Slot.BODY, "outfit_suit"),
    // --- holidays -------------------------------------------------------------------------------------------------------
    SANTA_HAT(Slot.HEAD, "outfit_santa_hat", "outfit_santa_trim"),
    CHRISTMAS_SWEATER(Slot.BODY, "outfit_suit"),
    WINTER_SCARF(Slot.NECK, "outfit_scarf"),
    SINT_MITRE(Slot.HEAD, "outfit_mitre"),
    PIET_BERET(Slot.HEAD, "outfit_beret", "outfit_beret_feather"),
    WITCH_HAT(Slot.HEAD, "outfit_tall_hat"),
    GHOST_SHEET(Slot.BODY, "outfit_suit"),
    PUMPKIN_HEAD(Slot.HEAD, "outfit_pumpkin"),
    ORANGE_CROWN(Slot.HEAD, "outfit_crown"),
    ORANGE_SHIRT(Slot.BODY, "outfit_suit"),
    // --- fantasy ------------------------------------------------------------------------------------------------------------
    WIZARD_HAT(Slot.HEAD, "outfit_tall_hat"),
    WIZARD_ROBE(Slot.BODY, "outfit_suit"),
    KNIGHT_HELMET(Slot.HEAD, "outfit_helmet"),
    KNIGHT_ARMOUR(Slot.BODY, "outfit_suit"),
    ROYAL_CROWN(Slot.HEAD, "outfit_crown"),
    ROYAL_CAPE(Slot.BODY, "outfit_cape"),
    PIRATE_HAT(Slot.HEAD, "outfit_tricorn"),
    // --- the backpack: 18 slots of storage in the guh's wardrobe --------------------------------------------------------------
    GUH_BACKPACK(Slot.BACK, "outfit_backpack"),
    // --- the kermis outfit: bought with kermisbonnen from the Kermis-guh (new pieces always go last: saved by number) --
    KERMIS_HOED(Slot.HEAD, "outfit_tall_hat"),
    KERMIS_JASJE(Slot.BODY, "outfit_suit"),
    KERMIS_STRIK(Slot.NECK, "outfit_bowtie"),
    // --- the detective outfit: bought with verstopguhtickets from Verstopguhtje -------------------------------------------
    DETECTIVE_PET(Slot.HEAD, "outfit_cap"),
    DETECTIVE_VERGROOTGLAS(Slot.EYES, "outfit_monocle"),
    DETECTIVE_JAS(Slot.BODY, "outfit_suit"),
    // --- the guh king's outfit: only from the Koningguh (tame him, then undress him in the wardrobe) -----------------
    KONING_KROON(Slot.HEAD, "outfit_grand_crown"),
    KONING_MANTEL(Slot.BODY, "outfit_long_cape"),
    KONING_KETTING(Slot.NECK, "outfit_chain"),
    // <beauty> (clothes of the beauty feature: put them between these lines, each ending with a comma)
    SHOWSTER_TIARA(Slot.HEAD, "outfit_tiara"),
    SHOWSTER_SJERP(Slot.BODY, "outfit_sash"),
    SHOWSTER_STRIK(Slot.NECK, "outfit_glitter_bow"),
    // </beauty>
    // <race> (clothes of the race feature: put them between these lines, each ending with a comma)
    // the jockey outfit: only bought with raceprijsjes from the Raceguh (guh racebaan)
    JOCKEY_PET(Slot.HEAD, "outfit_cap", "outfit_jockey_pom"),
    JOCKEY_JASJE(Slot.BODY, "outfit_suit"),
    RACEBRIL(Slot.EYES, "outfit_racebril"),
    // </race>
    // <meppen> (clothes of the meppen feature: put them between these lines, each ending with a comma)
    MIKAJAGER_HOED(Slot.HEAD, "outfit_jagershoed"),
    MIKAJAGER_VEST(Slot.BODY, "outfit_suit", "outfit_mepvest"),
    MIKAMEPPER_MEDAILLE(Slot.NECK, "outfit_mepmedaille"),
    // </meppen>
    // <disco> (clothes of the disco feature: put them between these lines, each ending with a comma)
    DISCO_GLITTERPAK(Slot.BODY, "outfit_suit"),
    DISCO_AFRO(Slot.HEAD, "outfit_disco_afro"),
    DISCO_BRIL(Slot.EYES, "outfit_disco_bril"),
    // </disco>
    // <golf> (clothes of the golf feature: put them between these lines, each ending with a comma)
    GOLF_PET(Slot.HEAD, "outfit_flat_cap"),
    GOLF_ZONNEKLEP(Slot.EYES, "outfit_visor"),
    GOLF_TRUI(Slot.BODY, "outfit_suit"),
    // </golf>
    // <smul> (clothes of the smul feature: put them between these lines, each ending with a comma)
    SMUL_SLABBETJE(Slot.NECK, "outfit_smul_bib"),
    SMUL_BAKKERSMUTS(Slot.HEAD, "outfit_smul_baker_hat"),
    SMUL_SCHORT(Slot.BODY, "outfit_smul_apron"),
    // </smul>
    // <vissen> (clothes of the vissen feature: put them between these lines, each ending with a comma)
    // the angler outfit: bought with visbonnen from the Visguh (guhvis pond)
    VISSERSHOEDJE(Slot.HEAD, "outfit_vishoed"),
    VISVEST(Slot.BODY, "outfit_suit"),
    VIS_AAN_DE_HAAK(Slot.BACK, "outfit_vishengel"),
    // </vissen>
    // <eilanden> (clothes of the eilanden feature: put them between these lines, each ending with a comma)
    /** The Wolkguh's own outfit (the floating guh islands): a puffy cloud hat and a fluffy cloud collar. */
    WOLKENMUTS(Slot.HEAD, "outfit_wolkenmuts"),
    WOLKENKRAAG(Slot.NECK, "outfit_wolkenkraag"),
    // </eilanden>
    // <kaasmijn> (clothes of the kaasmijn feature: put them between these lines, each ending with a comma)
    KAASMIJN_HELM(Slot.HEAD, "outfit_helmet", "outfit_kaasmijn_lamp"),
    KAASMIJN_OVERALL(Slot.BODY, "outfit_suit"),
    KAASMIJN_ZAKDOEK(Slot.NECK, "outfit_scarf"),
    // </kaasmijn>
    // <bibliotheek> (clothes of the bibliotheek feature: put them between these lines, each ending with a comma)
    // the scholar's outfit (little reading glasses, cardigan, beret): only sold by the Bibliothecaris in the guh library
    BIEB_LEESBRIL(Slot.EYES, "outfit_glasses"),
    BIEB_VEST(Slot.BODY, "outfit_suit"),
    BIEB_HOED(Slot.HEAD, "outfit_bieb_hoed", "outfit_bieb_steeltje"),
    // </bibliotheek>
    // <onderwater> (clothes of the onderwater feature)
    // the duikpakje: only sold by the Zeemeerguh in the Guhbubbel (for pearls)
    DUIKBRIL(Slot.EYES, "outfit_duikbril"),
    SNORKEL(Slot.HEAD, "outfit_snorkel"),
    ZWEMBAND(Slot.BODY, "outfit_zwemband"),
    // </onderwater>
    // <evenementen> (clothes of the evenementen feature)
    // the parade outfit (a drum major's shako, a parade jacket with golden epaulettes, a little drum): only from walking
    // along with the Vadsparade to the very end
    VADSPARADE_SJAKO(Slot.HEAD, "outfit_parade_sjako"),
    VADSPARADE_JASJE(Slot.BODY, "outfit_suit", "outfit_parade_epaulet"),
    VADSPARADE_TROMMELTJE(Slot.NECK, "outfit_parade_trommel"),
    // </evenementen>
    // <vadswoud> (clothes of the vadswoud feature: put them between these lines, each ending with a comma)
    // the ranger outfit (Boswachterguh) and the picker's outfit (Knabbelplukker), only sold in the boomhutdorp
    BOSWACHTERSHOED(Slot.HEAD, "outfit_boswachtershoed"),
    BOSWACHTERSJAS(Slot.BODY, "outfit_suit"),
    PLUKMUTS(Slot.HEAD, "outfit_plukmuts"),
    PLUKMANDJE(Slot.BACK, "outfit_plukmandje"),
    // </vadswoud>
    // <barbecuether> (clothes of the barbecuether feature: the Grillguh's chef outfit)
    GRILL_KOKSMUTS(Slot.HEAD, "outfit_grill_koksmuts", "outfit_grill_koksmuts_band"),
    GRILL_SCHORT(Slot.BODY, "outfit_grill_schort"),
    GRILL_HALSDOEK(Slot.NECK, "outfit_scarf"),
    // </barbecuether>
    // --- 2.8 (Knuffeldal): one marker block per slice; each slice puts its clothes between its own lines ---
    // <knuffeldal>
    /** The mayor's sash: only from the finale of the Grote Knusfeest (Burgemeester Vadsema). */
    BURGEMEESTERSSJERP(Slot.BODY, "outfit_sash"),
    /** Seasonal (Knuffeldal): braided from flowers in spring, a straw sun hat in summer, a knitted scarf in winter. */
    BLOESEMKRANSJE(Slot.HEAD, "outfit_bloesemkransje"),
    ZONNEHOEDJE(Slot.HEAD, "outfit_zonnehoedje"),
    KNUS_SJAALTJE(Slot.NECK, "outfit_scarf"),
    // </knuffeldal>
    //
    // <bakkerij>
    /** Bakker Korstje's outfit (only in his shop, for bakmunten): a round baker's cap with a kaasknabbel, a floury apron, a bow. */
    BAKKERSMUTSJE(Slot.HEAD, "outfit_bakkersmutsje"),
    BAKKERSSCHORTJE(Slot.BODY, "outfit_suit"),
    MEELSTRIKJE(Slot.NECK, "outfit_bowtie"),
    // </bakkerij>
    //
    // <creche>
    /** The Knuffelcreche's baby clothes (Juf Knuffel's shop, for speenmunten): a frilly bonnet, a romper, a pacifier chain. */
    BABYMUTSJE(Slot.HEAD, "outfit_babymutsje"),
    ROMPERTJE(Slot.BODY, "outfit_suit"),
    SPEENKETTINKJE(Slot.NECK, "outfit_speenkettinkje"),
    // </creche>
    //
    // <theehuis>
    /** A knitted tea cosy as a hat, with a pompom: from Mevrouw Theelepel after your first gezellige theekransje. */
    THEEMUTSJE(Slot.HEAD, "outfit_theemutsje"),
    // </theehuis>
    //
    // <kapper>
    // the hairstyles of Kapper Krulletje (Knip & Vads, Knuffeldal): the HAAR slot, permanent (see feature.kapper.Kapsel).
    // Each one also names "pluis_kuif", so a Pluisguh's own tuft steps aside (its swatch is see-through in the hair's texture);
    // the bones ending in "_kruin" (the high part on top) hide under a hat (client.GuhClothesLayer)
    KAPSEL_KRULLEN(Slot.HAAR, "outfit_haar_krullen", "pluis_kuif"),
    KAPSEL_KUIFJE(Slot.HAAR, "outfit_haar_kuifje", "pluis_kuif"),
    KAPSEL_KNOTJES(Slot.HAAR, "outfit_haar_knotjes", "pluis_kuif"),
    KAPSEL_STRIKJES(Slot.HAAR, "outfit_haar_strikjes", "pluis_kuif"),
    KAPSEL_PLUISBOL(Slot.HAAR, "outfit_haar_pluisbol", "pluis_kuif"),
    KAPSEL_VLECHTJES(Slot.HAAR, "outfit_haar_vlechtjes", "pluis_kuif"),
    KAPSEL_HANENKAM(Slot.HAAR, "outfit_haar_hanenkam", "pluis_kuif"),
    KAPSEL_MATJE(Slot.HAAR, "outfit_haar_matje", "pluis_kuif"),
    /** Krulletje's own hairdresser's cape (sold for krulmunten). */
    KAPPERSCAPE(Slot.BODY, "outfit_cape"),
    // </kapper>
    //
    // <boerderij>
    /** Boerin Hooibaal's straw farmer's hat and her red neckerchief with white dots (her shop, and the Knus milestones). */
    BOERDERIJ_HOEDJE(Slot.HEAD, "outfit_boerderij_hoedje"),
    BOERDERIJ_ZAKDOEK(Slot.NECK, "outfit_scarf"),
    // </boerderij>
    //
    // <tuintjes>
    /** A floppy garden hat with a guhbloemetje, and a green garden apron with a pocket (Hooibaal's shop, the Knus milestones). */
    TUINHOEDJE(Slot.HEAD, "outfit_tuinhoedje"),
    TUINSCHORTJE(Slot.BODY, "outfit_suit"),
    // </tuintjes>
    //
    // <sterrenwacht>
    /** Professor Sterretje's outfit (Guh-Sterrenwacht, for wenssterren): a tall starry hat and a night-blue cape. */
    STERRENKIJKERSMUTS(Slot.HEAD, "outfit_tall_hat"),
    STERRENCAPE(Slot.BODY, "outfit_cape"),
    // </sterrenwacht>
    //
    // <ballon>
    /** Kapitein Wolkje's outfit (Ballonfestival, for ballonmunten): a leather pilot's cap and round flying goggles. */
    BALLONPET(Slot.HEAD, "outfit_ballonpet"),
    BALLONBRIL(Slot.EYES, "outfit_ballonbril"),
    // </ballon>
    //
    // <kamperen>
    /** Pyjamas (Opa Guh's campfire stories): guhs wear them by the campfire at night by themselves, too. */
    PYJAMA_PAKJE(Slot.BODY, "outfit_suit"),
    SLAAPMUTSJE(Slot.HEAD, "outfit_slaapmutsje"),
    // </kamperen>
    //
    // <knuffelbad>
    /** Badmeester Bubbel's shop (Knuffelbad): a swim cap with a flower, and a fluffy pink bathrobe. */
    BADMUTSJE(Slot.HEAD, "outfit_badmutsje"),
    BADJASJE(Slot.BODY, "outfit_suit"),
    // </knuffelbad>
    //
    // <wereldleven>
    /** The choir bow tie (all six songs of the liedjesboekje) and IJscoguh Tingeling's cap (sold by him). */
    KOORSTRIKJE(Slot.NECK, "outfit_bowtie"),
    IJSCOPETJE(Slot.HEAD, "outfit_cap"),
    // </wereldleven>
    //
    // --- 2.9 (De Grote Guhspelen): one marker block per slice, each filled in by its own slice ---
    // <kleding>
    /** The kleermaker's ear bows (2.9, the OREN slot): a little satin bow on each ear, in pink, mint and yellow. */
    OORSTRIKJE_ROZE(Slot.OREN, "outfit_oren_strik"),
    OORSTRIKJE_MINT(Slot.OREN, "outfit_oren_strik"),
    OORSTRIKJE_GEEL(Slot.OREN, "outfit_oren_strik"),
    // </kleding>
    //
    // <sjoelen>
    /** Opoe Njegschuif's shop (Sjoelhuisje, sjoelschijfjes): a flat puck-shaped cap, a knitted cardigan and a puck brooch. */
    SJOELEN_PETJE(Slot.HEAD, "outfit_sjoelpetje"),
    SJOELEN_VESTJE(Slot.BODY, "outfit_suit", "outfit_sjoelvest_knoop"),
    SJOELEN_BROCHE(Slot.NECK, "outfit_sjoelbroche"),
    // </sjoelen>
    //
    // <doolhof>
    /** Het Guhdoolhof's explorer outfit (Meneer Vadskronkel's shop, doolhofknabbels): hat, compass on a cord, little backpack (no storage). */
    DOOLHOF_HOEDJE(Slot.HEAD, "outfit_doolhof_hoedje"),
    DOOLHOF_RUGZAKJE(Slot.BACK, "outfit_doolhof_rugzakje"),
    DOOLHOF_KOMPAS(Slot.NECK, "outfit_doolhof_kompas"),
    // </doolhof>
    //
    // <katapult>
    /** Kapitein Floepguh's shop (Knabbelkatapult, katapultsterren): a helmet with a feather, a belt with a pluisbal pouch, pluisbal earrings. */
    KATAPULT_HELMPJE(Slot.HEAD, "outfit_katapulthelm"),
    KATAPULT_RIEM(Slot.BODY, "outfit_katapultriem"),
    KATAPULT_OORBELLETJES(Slot.OREN, "outfit_oren_oorbel"),
    // </katapult>
    //
    // <knabbelspelen>
    /** De Knabbelspelen's sports outfit (Juf Vahoegsakee's shop, spelenlintjes): sweatband, sports shirt, whistle on a cord. */
    SPELEN_ZWEETBANDJE(Slot.HEAD, "outfit_spelen_zweetband"),
    SPELEN_SPORTSHIRTJE(Slot.BODY, "outfit_suit"),
    SPELEN_FLUITJE(Slot.NECK, "outfit_spelen_fluitje"),
    // </knabbelspelen>
    //
    // <elftocht>
    /** De Elf-Guhjestocht (Schaatsmeester Guhglij's shop, elfstempels): pompom hat, woollen jumper, orange scarf, fluffy ear warmers. */
    ELFTOCHT_SCHAATSMUTS(Slot.HEAD, "outfit_elftocht_muts"),
    ELFTOCHT_TRUITJE(Slot.BODY, "outfit_suit"),
    ELFTOCHT_SJAAL(Slot.NECK, "outfit_scarf"),
    ELFTOCHT_OORWARMERS(Slot.OREN, "outfit_oren_warmer"),
    // </elftocht>
    //
    // <circuit>
    /** The Guh-Circuit outfit (Coach Vahoegvroem's shop, circuitbekers): a round racing helmet with a visor, a racing suit, a chequered flag cape. */
    CIRCUIT_HELMPJE(Slot.HEAD, "outfit_circuit_helm"),
    CIRCUIT_RACEPAK(Slot.BODY, "outfit_suit"),
    CIRCUIT_VLAGCAPE(Slot.BACK, "outfit_circuit_vlagcape"),
    // </circuit>
    //
    // <disco29>
    /** The DJ-guh's little headphones (2.9): pink cups with a golden note on both guh ears. */
    DISCO_KOPTELEFOONTJE(Slot.OREN, "outfit_oren_koptelefoon"),
    // </disco29>
    //
    // <samen>
    /** Hartjes level 1 ("lieve vadsjes van elkaar"): a golden hairpin with a pink heart on the left guh ear. */
    SAMEN_HARTJESSPELDJE(Slot.OREN, "outfit_oren_hartjesspeld"),
    /** Hartjes level 2 ("mega lieve vadsjes"): a chunky knitted pink sweater with hearts, a rolled collar and a heart patch on the back. */
    SAMEN_KNUFFELTRUITJE(Slot.BODY, "outfit_suit", "outfit_samen_truitje"),
    /** Hartjes level 3 ("zielsguh bff 5evr &lt;3"): a little golden crown with pink heart points. */
    SAMEN_ZIELSKROONTJE(Slot.HEAD, "outfit_samen_kroontje"),
    /** Hartjes level 3, exclusive: the gouden hartjes-halsbandje (a golden collar with a sparkly heart locket). */
    GOUDEN_HARTJESHALSBANDJE(Slot.NECK, "outfit_samen_halsbandje"),
    // </samen>
    //
    // <timmerguh>
    /** 3.0 Timmerguh ("Samen een huisje bouwen"): a cream timmermanshelmpje with a pink ridge, a klep and a heart sticker. */
    TIMMER_HELMPJE(Slot.HEAD, "outfit_timmerhelm"),
    /** 3.0 Timmerguh (the optional step): a leather gereedschapsriem with a hamertje, a yellow duimstok and a spijkerzakje. */
    TIMMER_GEREEDSCHAPSRIEM(Slot.BODY, "outfit_timmerriem"),
    // </timmerguh>
    //
    // <balto>
    /** Held van Nomguh (3.0): Baltoguh's red scarf with its knot and two fluttering ends. */
    BALTO_SJAALTJE(Slot.NECK, "outfit_scarf", "outfit_balto_sjaal"),
    /** Held van Nomguh: pointy grey wolf ears (with a cream inside) on the guh ears. */
    BALTO_WOLFSOORTJES(Slot.OREN, "outfit_oren_balto"),
    /** Held van Nomguh: an ice-blue knitted snow hat with a snowflake band, a big fluffy pompon and two braided strings. */
    BALTO_SNEEUWMUTS(Slot.HEAD, "outfit_balto_muts"),
    /** Held van Nomguh: red mittens on the front paws, on a string around the neck (so they never get lost, njeg). */
    BALTO_WANTJES(Slot.BODY, "outfit_balto_want"),
    // </balto>
    //
    // <mewtwo>
    /** Het kloon-eiland (3.0): the trainerpetje with our own logo (the knabbelbal with guh ears) and a dark purple peak. */
    MEWTWO_TRAINERPETJE(Slot.HEAD, "outfit_mewtwo_petje"),
    /** A lilac trainer vest (the body suit) with a little backpack and a knabbelbal clipped to it. */
    MEWTWO_TRAINERPAKJE(Slot.BODY, "outfit_suit", "outfit_mewtwo_rugzakje", "outfit_mewtwo_knabbelbal"),
    /** A soft purple costume tail with a bulb and a pink bow, and a neck tube on a little collar. */
    MEWTWO_STAARTJE(Slot.NECK, "outfit_mewtwo_nepstaart", "outfit_mewtwo_nekbuis", "outfit_mewtwo_kraagje"),
    /** A pink balloon that looks like Mieuwguh, on a string tied to the back. */
    MEW_BALLONNETJE(Slot.BACK, "outfit_mewtwo_ballon"),
    // </mewtwo>
    //
    // <hemel>
    /** Het Hemelkapelletje (3.0, the wolkenhoeder's thank-you, source "hemel"): a golden halo floating above the head, little cloud wings on the back. */
    HEMEL_AUREOOLTJE(Slot.HEAD, "outfit_hemel_aureool"),
    HEMEL_WOLKENVLEUGELTJES(Slot.BACK, "outfit_hemel_vleugel"),
    // </hemel>
    //
    // <guhwaii>
    /** Ohana op Guhwai'i: a swishy hula skirt of grass blades round the belly, with a band of little pink flowers. */
    GUHWAII_HULAROKJE(Slot.BODY, "outfit_guhwaii_rokje"),
    /** Ohana op Guhwai'i: a bloemenkrans (lei) of pink, white and yellow flowers round the neck. */
    GUHWAII_BLOEMENKRANS(Slot.NECK, "outfit_guhwaii_krans"),
    /** Ohana op Guhwai'i: big blue 626 ears over the guh ears, with two wobbly antennes on top (OREN). */
    GUHWAII_STITCHOREN(Slot.OREN, "outfit_oren_guhwaii"),
    /** Ohana op Guhwai'i: a little surfplankje on the back (turquoise with a pink hibiscus). */
    GUHWAII_SURFPLANKJE(Slot.BACK, "outfit_guhwaii_surfplank"),
    // </guhwaii>
    //
    // --- bbq2 (CONTRACT_130 5.4): the slices put their pieces between their own markers, each line ending with a comma ---
    // <paleizen>
    // </paleizen>
    //
    // <bestaand>
    // </bestaand>
    //
    // <camping_markt>
    // </camping_markt>
    //
    // <toren_peper>
    // </toren_peper>
    //
    // <ring>
    // </ring>
    //
    // <guhrio_beloning>
    // </guhrio_beloning>
    //
    ;

    /**
     * The slots (2.9: OREN is the 7th, after HAAR, and a normal wardrobe slot). HAAR (2.8, the kapper feature: hairstyles, bones {@code outfit_haar_*}) is special: it is saved, synced
     * and drawn like the others, but it isn't in the wardrobe and nothing takes it off except the kapper (not
     * GuhEntity.takeOffClothes, the beauty show or the Koningstroon): use {@link #kleding()} for "the clothes".
     */
    public enum Slot {
        HEAD, EYES, BODY, NECK, BACK, HAAR,
        /** 2.9: on the guh ears (earrings, ear warmers, ear bows, headphones; bones {@code outfit_oren_*} on the head). */
        OREN;

        private static final List<Slot> KLEDING = List.of(HEAD, EYES, BODY, NECK, BACK, OREN);

        /** The six wardrobe slots (everything but HAAR), in order. */
        public static List<Slot> kleding() {
            return KLEDING;
        }
    }

    /** Outfits wild Guhmension guhs sometimes already wear (GuhEntity.OUTFIT_CHANCE). Nothing else spawns on wild guhs. */
    public static final List<List<GuhClothes>> WILD_OUTFITS = List.of(
            List.of(STRIPED_SWEATER),
            List.of(RAINCOAT, RAIN_HAT),
            List.of(PARTY_HAT, RED_BOWTIE),
            List.of(CHEF_JACKET, CHEF_HAT, BLACK_BOWTIE));

    public final Slot slot;
    /** Bone name prefixes this piece shows. */
    public final List<String> bones;

    GuhClothes(Slot slot, String... bones) {
        this.slot = slot;
        this.bones = List.of(bones);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Identifier texture() {
        return Guhs.id("textures/entity/guh_clothes/" + id() + ".png");
    }

    public boolean shows(String bone) {
        return bones.stream().anyMatch(bone::startsWith);
    }

    /** Every bone prefix any piece in this slot can show (so a worn piece can hide a variant's own bones there). */
    public static List<String> slotBones(Slot slot) {
        return java.util.Arrays.stream(values()).filter(c -> c.slot == slot).flatMap(c -> c.bones.stream()).distinct().toList();
    }

    @Nullable
    public static GuhClothes byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : null;
    }

    @Nullable
    public static GuhClothes byId(String id) {
        for (GuhClothes c : values()) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return null;
    }
}
