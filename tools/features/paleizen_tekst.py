"""
bbq2 (paleizen) - every Dutch text of the three Mika palaces: names, the three questlines (Guhdex tab Verhalen), what the
characters say, the messages, the signs in the templates and the advancements. English comes in phase 3 (tools/lang/en).

  TEXTS      {lang key: Dutch}
  LIJNEN     the arguments of verhaal_motor.verhaallijn for the three questlines (ids mika_oma, stalknecht, tolwachter)
  RAADSELS   the Tolwachter's riddles: (question, [three answers], index of the right one); Java: TolQuest.GOED
  NAMEN      the names of the neighbours (entity.guhs.paleizen_mopper_mika.<nr>)
"""

OMA = "quest.guhs.paleizen.oma."
MOP = "quest.guhs.paleizen.mopper."
STAL = "quest.guhs.paleizen.stal."
TOL = "quest.guhs.paleizen.tol."
GUI = "gui.guhs.paleizen."
SIGN = "sign.guhs.paleizen."

NAMEN = ["Brom-Mika", "Zeur-Mika", "Snurk-Mika", "Buur-Mika", "Barbecue-Mika", "Zonnebad-Mika"]

RAADSELS = [
    ("Het is geel, het kraakt en het is altijd te snel op. Wat is het?", ["Een kaasknabbel", "Een Rookguh", "De zon"], 0),
    ("Ik heb een krulstaart met een knoopje, vier pootjes en eigenlijk ben ik een worst. Wie ben ik?",
     ["Een Sausloper", "Een Worstzwijntje", "De Tolwachter"], 1),
    ("Wat wordt natter naarmate het meer afdroogt?", ["Kaasfrituursaus", "Een Sausblubje", "Een handdoek"], 2),
    ("Wat heeft een bek maar bijt niet, en je loopt er zo doorheen?", ["Mijn tolhuis", "Een boze guh", "Een Hapbloem"], 0),
    ("Wat kun je breken zonder het aan te raken?", ["Een brugplank", "Een belofte", "Een knabbel"], 1),
    ("Wat zegt een guh die het helemaal met je eens is?", ["Miauw", "Tol betalen!", "Njeg"], 2),
]

LIJNEN = {
    "mika_oma": dict(
        naam="Soep van Mika-oma",
        uitleg="Mika-oma doet niet mee aan het knabbels jatten: ze breit en kookt worstsoep voor de hele flat. Maar haar buren "
               "mopperen en haar breiwerk is weggewaaid. Njeg!",
        stappen=[
            ("Zoek Mika-oma", "Klim naar de bovenste galerij van de hoge flat en praat met Mika-oma.",
             "De Mika-woonblokken in de Guhbarbecuether (superkompas: Barbecue)"),
            ("Breng soep naar de mopperaars", "Breng een kommetje worstsoep naar Brom-Mika, Zeur-Mika en Snurk-Mika (rechtsklik ze "
             "met de soep in je hand).", "Brom-Mika: westflat, begane grond. Zeur-Mika: oostflat, tweede verdieping. Snurk-Mika: "
             "hoge flat, tweede verdieping"),
            ("Zoek het breiwerk", "Het breiwerk van Mika-oma is van de galerij gewaaid. Zoek het breimandje en pak het breiwerk "
             "(rechtsklik).", "De daktuin van de lage oostflat (neem de trap in de oostelijke toren)"),
            ("Breng het breiwerk terug", "Breng het breiwerk naar Mika-oma.", "De bovenste galerij van de hoge flat"),
        ],
        klaar=("Mika-oma breit weer en de hele flat ruikt naar soep. Vahoeg!", "De Mika-woonblokken"),
        extra={"1_klaar": ("Alle drie de mopperaars hebben soep en het is opeens heel stil in de flat. Ga het Mika-oma vertellen!",
                           "De bovenste galerij van de hoge flat")},
        kort={"0": "Praat met Mika-oma op de bovenste galerij", "1": "Breng soep naar de drie mopperaars",
              "1_klaar": "Vertel Mika-oma dat de soep rond is", "2": "Zoek het breiwerk op de daktuin",
              "3": "Breng het breiwerk naar Mika-oma"}),
    "stalknecht": dict(
        naam="De onrustige Worstzwijntjes",
        uitleg="De Mika's laten hun Worstzwijntjes de hele dag knabbels opsnuffelen en zijn veel te ruw. De Stalknecht-guh "
               "zorgt stiekem goed voor ze, maar nu stuiteren ze in hun box en eentje is ontsnapt.",
        stappen=[
            ("Praat met de Stalknecht-guh", "Praat met de Stalknecht-guh voor de grote staldeur.",
             "De Mika-stal in de Guhbarbecuether (superkompas: Barbecue)"),
            ("Kalmeer drie Worstzwijntjes", "Aai drie onrustige Worstzwijntjes tot ze rustig knorren: rechtsklik met een lege "
             "hand, drie aaitjes per zwijntje.", "In de boxen van de stal en in de wei"),
            ("Vul de voerbak", "Schep de zak zwijnenvoer van de Stalknecht-guh leeg in de voerbak (rechtsklik de voerbak met de zak).",
             "Aan het eind van het gangpad in de stal"),
            ("Vang Knorretje", "Knorretje is ontsnapt! Zoek hem bij de stal, sluip naar hem toe (anders schrikt hij) en til hem op "
             "(rechtsklik).", "Achter de hooibalen in de wei, op de hooizolder of bij de kar"),
        ],
        klaar=("Alle Worstzwijntjes knorren tevreden, en jij hebt er zelf twee. Knor, njeg!", "De Mika-stal"),
        extra={"1_klaar": ("Drie rustige Worstzwijntjes! Ga terug naar de Stalknecht-guh.", "Voor de grote staldeur"),
               "2_klaar": ("De Worstzwijntjes smakken tevreden. Ga terug naar de Stalknecht-guh.", "Voor de grote staldeur"),
               "3_gevangen": ("Je hebt Knorretje in je armen! Breng hem naar de Stalknecht-guh.", "Voor de grote staldeur")},
        kort={"0": "Praat met de Stalknecht-guh", "1": "Aai drie Worstzwijntjes kalm", "1_klaar": "Ga terug naar de Stalknecht-guh",
              "2": "Vul de voerbak in de stal", "2_klaar": "Ga terug naar de Stalknecht-guh", "3": "Vang Knorretje (sluipen!)",
              "3_gevangen": "Breng Knorretje naar de Stalknecht-guh"}),
    "tolwachter": dict(
        naam="De tolbrug van het Mika-brugpaleis",
        uitleg="Niemand komt over de brug zonder drie raadsels van de Tolwachter te raden. En de brug zelf? Die is alweer kapot. "
               "Mika-kwaliteit, njeg.",
        stappen=[
            ("Praat met de Tolwachter-Mika", "Loop naar de reusachtige Mikabek en praat met de Tolwachter-Mika.",
             "Het Mika-brugpaleis in de Guhbarbecuether (superkompas: Barbecue)"),
            ("Raad drie raadsels", "Raad drie raadsels van de Tolwachter-Mika goed. Fout geraden? Dan krijg je gewoon een ander raadsel.",
             "Bij de Tolwachter-Mika in de poort"),
            ("Repareer de brug", "Leg de vijf brugplanken van de Tolwachter in het gat (rechtsklik met een plank, vlak bij het gat).",
             "Op de brug, voorbij het tolhuis"),
            ("Luid de tolbel", "Loop over de gemaakte brug naar de klokkentoren en luid de tolbel (rechtsklik de bel).",
             "In de klokkentoren aan de overkant"),
            ("Terug naar de Tolwachter", "Ga terug naar de Tolwachter-Mika voor je beloning.", "Bij de Tolwachter-Mika in de poort"),
        ],
        klaar=("De brug is heel (voor even), de bel luidt en jij mag altijd gratis door. Vahoeg!", "Het Mika-brugpaleis"),
        kort={"0": "Praat met de Tolwachter-Mika", "1": "Raad drie raadsels van de Tolwachter", "2": "Leg de vijf planken in het gat",
              "3": "Luid de tolbel aan de overkant", "4": "Ga terug naar de Tolwachter-Mika"}),
}

TEXTS = {
    # --- names -----------------------------------------------------------------------------------------------------------
    "entity.guhs.guh_npc.mika_oma": "Mika-oma",
    "entity.guhs.guh_npc.stalknechtguh": "Stalknecht-guh",
    "entity.guhs.guh_npc.tolwachter_mika": "Tolwachter-Mika",
    "entity.guhs.paleizen_mopper_mika": "Mopper-Mika",
    "block.guhs.paleizen_brugplank": "Mika-brugplanken",
    "block.guhs.paleizen_brugleuning": "Touwleuning",
    "block.guhs.paleizen_breiwerk": "Breimandje van Mika-oma",
    "item.guhs.worstzwijntje_spawn_egg": "Worstzwijntje-spawnei",
    "item.guhs.paleizen_omasoep": "Oma's worstsoep",
    "item.guhs.paleizen_omasoep.lore": "Van Mika-oma, voor de mopperaars van de flat. Niet zelf opeten, njeg!",
    "item.guhs.paleizen_breiwerkje": "Breiwerk van Mika-oma",
    "item.guhs.paleizen_breiwerkje.lore": "Een half mutsje aan twee naalden. Breng het terug naar oma.",
    "item.guhs.paleizen_zwijnenvoer": "Zak zwijnenvoer",
    "item.guhs.paleizen_zwijnenvoer.lore": "Voor de voerbak in de Mika-stal. (Smaakt naar karton.)",
    "item.guhs.paleizen_gevangen_zwijntje": "Knorretje (in je armen)",
    "item.guhs.paleizen_gevangen_zwijntje.lore": "Het ontsnapte Worstzwijntje. Breng hem naar de Stalknecht-guh.",
    "item.guhs.paleizen_worstzwijntje_mandje": "Worstzwijntje in een mandje",
    "item.guhs.paleizen_worstzwijntje_mandje.lore": "Zet het mandje neer (rechtsklik op de grond): er springt een Worstzwijntje uit dat bij jou wil wonen.",
    "item.guhs.paleizen_worstzwijntje_mandje.lore2": "Aaien, borstelen en knabbelvoer: een blij Worstzwijntje snuffelt elke dag iets lekkers op.",
    "item.guhs.paleizen_losse_plank": "Brugplank van de Tolwachter",
    "item.guhs.paleizen_losse_plank.lore": "Rechtsklik vlak bij het gat in de tolbrug: je legt een hele rij planken.",
    "item.guhs.paleizen_recept_brug": "Bouwtekening: Mika-brug",
    "item.guhs.paleizen_recept_brug.lore": "Met deze tekening in je werkbank maak je Mika-brugplanken en touwleuning.",
    "item.guhs.paleizen_recept_brug.lore2": "Blijft gewoon in je werkbank liggen. (De brug staat er scheef op.)",
    "item.guhs.paleizen_mikamuts": "Gebreide Mika-muts",
    "gui.guhs.kledingbron.paleizen": "Soep van Mika-oma (Mika-woonblokken)",
    # the Worstzwijntje as a farm animal (BoerderijDier's message, the sniffing)
    "gui.guhs.boerderij.blij.worstzwijntje": "Je Worstzwijntje is helemaal blij! Het gaat iets lekkers voor je opsnuffelen.",
    GUI + "snuffel": "Snuf snuf... je Worstzwijntje heeft iets opgesnuffeld!",
    GUI + "mandje.los": "Knor! Je Worstzwijntje springt uit het mandje.",
    GUI + "mandje.geen_plek": "Hier is geen plek voor het mandje. Zoek een stukje vrije grond.",
    "subtitles.guhs.paleizen.worstzwijntje_knor": "Worstzwijntje knort",
    "subtitles.guhs.paleizen.worstzwijntje_gil": "Worstzwijntje gilt",
    "subtitles.guhs.paleizen.mopper": "Mika moppert",
    "subtitles.guhs.paleizen.plank": "Plank wordt vastgetimmerd",
    # --- Mika-oma ----------------------------------------------------------------------------------------------------------
    OMA + "hallo": "Ach, bezoek! Kom binnen, kind... of nee, blijf maar buiten, binnen ligt overal wol. Ik ben Mika-oma. Die jonge "
                   "Mika's jatten de hele dag knabbels. Ik niet hoor. Ik brei. En ik kook worstsoep. Njeg!",
    OMA + "vraag": "Wil je iets voor me doen? Mijn buren mopperen de hele dag, en daar helpt maar één ding tegen: een kommetje van "
                   "mijn worstsoep. Maar mijn pootjes willen al die trappen niet meer af.",
    OMA + "uitleg": "Brom-Mika woont beneden in de westflat en bromt over herrie. Zeur-Mika woont in de oostflat, tweede "
                    "verdieping, en zeurt over de geur. En Snurk-Mika, twee verdiepingen onder mij, snurkt zo hard dat hij er zelf "
                    "wakker van wordt. En dan moppert hij. Njeg.",
    OMA + "soep": "Dank je, kind! Hier zijn drie kommetjes: eentje voor Brom-Mika, eentje voor Zeur-Mika en eentje voor "
                  "Snurk-Mika. Niet zelf opeten, hoor! ...Nou ja. Ik heb nog een hele pan.",
    OMA + "soep_nog": "Is de soep al rond? Nog %s mopperaars te gaan. Brom-Mika woont beneden in de westflat, Zeur-Mika op de "
                      "tweede verdieping van de oostflat en Snurk-Mika twee verdiepingen onder mij.",
    OMA + "soep_kwijt": "Is de soep op? Of... heb je hem zelf opgegeten? Geeft niks, kind. Hier, een nieuw kommetje.",
    OMA + "breiwerk": "Ze hebben allemaal gegeten en niemand moppert? Wat een rust! Dan kan ik eindelijk verder met brei... MIJN "
                      "BREIWERK! Het lag op het randje van de galerij. Het is weggewaaid: ik zag het nog net op het dak van de "
                      "lage flat landen. Wil jij het voor me pakken?",
    OMA + "breiwerk_nog": "Mijn breiwerk ligt vast nog op de daktuin van de lage flat, daar aan de overkant. Neem de trap in de "
                          "oostelijke toren en loop over de galerij, kind.",
    OMA + "klaar": "Mijn breiwerk! En er is geen steekje los. Weet je wat? Dit mutsje was toch bijna af... tik tik tik... zo! Een "
                   "gebreide Mika-muts voor je guh. Met hoorntjes! En ik zeg tegen de jonge Mika's dat ze jou korting geven bij "
                   "het ruilen. Anders krijgen ze geen soep meer. Njeg!",
    OMA + "dank0": "Kom je nog eens langs? Er staat altijd soep op.",
    OMA + "dank1": "Die muts staat je guh vast beeldig. Met hoorntjes zie je er meteen uit als een echte Mika. Maar dan lief.",
    OMA + "dank2": "Geeft een Nether-Mika je geen korting? Zeg dan maar: 'Dat hoort oma!' Njeg.",
    OMA + "optie.ja": "Ik breng de soep rond!",
    OMA + "optie.wie": "Wie mopperen er dan?",
    OMA + "optie.zoek": "Ik ga het zoeken!",
    OMA + "optie.dank": "Dankjewel, oma!",
    OMA + "hint.soep": "breng de soep naar Brom-Mika (westflat, begane grond), Zeur-Mika (oostflat, 2e verdieping) en Snurk-Mika (hoge flat, 2e verdieping)",
    OMA + "hint.breiwerk": "zoek het breimandje op de daktuin van de lage oostflat (de trap in de oostelijke toren)",
    OMA + "hint.terug": "breng het breiwerk naar Mika-oma op de bovenste galerij",
    MOP + "0.mopper": "Brom! Wat een herrie hier altijd. Stampende poten op de galerij, bellen die luiden, zwijnen die knorren. BROM!",
    MOP + "0.soep": "Brom... is dat de soep van Mika-oma? ...Slurp. Hm. Nou. Die is wel lekker. Maar zeg het tegen niemand. Brom.",
    MOP + "0.tevreden": "Brom. Stil eens, ik geniet nog na van de soep.",
    MOP + "1.mopper": "Het STINKT hier! Naar aangebrande knabbels, naar natte Mika, naar alles. En niemand doet er wat aan!",
    MOP + "1.soep": "Wat ruik ik nou... worstsoep? Van oma? Geef hier! ...Mmm. Oké. DIT mag wel ruiken.",
    MOP + "1.tevreden": "Ik zeur even niet. Mijn snuit zit nog vol soepgeur. Heerlijk.",
    MOP + "2.mopper": "ZZZ... snurk... HÈ?! Wie maakt mij wakker? Ik sliep net! Nou ja, ik werd wakker van mijn eigen gesnurk. Maar TOCH.",
    MOP + "2.soep": "Soep op bed? Van oma? ...Slurp slurp. Daar word je zo lekker slaperig van. Welterusten. ZZZ...",
    MOP + "2.tevreden": "ZZZ... soep... zzz... njeg... zzz...",
    MOP + "3.a": "Mooi weer voor de was, hè? Nou ja. Het is hier altijd rokerig. Maar de was hangt.",
    MOP + "3.b": "Ken je Mika-oma van de bovenste galerij? Die breit sokken voor de hele flat. Ze kriebelen wel.",
    MOP + "4.a": "Barbecueën doe je met geduld. En met gejatte knabbels. Maar dat laatste heb je niet van mij.",
    MOP + "4.b": "Worstje? Nee, grapje. Die zijn van mij.",
    MOP + "5.a": "Ssst. Ik lig te zonnen. Ja, ik weet dat er hier geen zon is. Je moet er gewoon in geloven.",
    MOP + "5.b": "Dit is het mooiste dak van de hele Barbecuether. Vooral als niemand tegen me praat.",
    MOP + "geen_soep": "Zonder soep hoef je hier niet aan te kloppen. Mopper mopper.",
    MOP + "vreemde": "Soep? Van wie? Ik neem niks aan van vreemden. Praat eerst maar met Mika-oma. Mopper.",
    GUI + "soep.nog": "Slurp! Nog %s mopperaars te gaan.",
    GUI + "soep.klaar": "Alle drie de mopperaars hebben soep. Het is opeens heel stil in de flat... Ga het Mika-oma vertellen!",
    GUI + "breiwerk.niet_nu": "Een mandje met breiwerk. Van wie zou dat zijn?",
    GUI + "breiwerk.gevonden": "Je hebt het breiwerk van Mika-oma gevonden! Breng het snel terug.",
    GUI + "breiwerk.al": "Je hebt het breiwerk al. Mika-oma wacht op de bovenste galerij.",
    GUI + "breiwerk.klaar": "Oma heeft hier een reservebreiwerkje neergelegd. 'Voor als het weer waait', zegt ze.",
    GUI + "korting": "Korting van Mika-oma! De Mika gooit je staaf terug. 'Dat hoort oma!'",
    GUI + "beloning.korting": "Korting bij de Nether-Mika's: elke derde ruil is gratis",
    # --- the Stalknecht-guh ------------------------------------------------------------------------------------------------
    STAL + "hallo": "Psst! Jij daar! Ik ben de Stalknecht-guh. Ja, een guh, bij de Mika's. Iemand moet toch voor de Worstzwijntjes "
                    "zorgen? De Mika's laten ze de hele dag knabbels opsnuffelen en schreeuwen maar. Daar worden ze zo onrustig "
                    "van, njeg.",
    STAL + "vraag": "Kijk nou: ze stuiteren in hun box. Wil je me helpen ze te kalmeren? Je hoeft alleen maar te aaien. Dat kunnen "
                    "Mika's niet, die hebben er het geduld niet voor.",
    STAL + "uitleg": "Een Worstzwijntje is een worstje op pootjes: een snuitje, twee slagtandjes, een streep mosterd over zijn rug "
                     "en een krulstaart met een knoopje erin. Ze ruiken een knabbel op honderd blokken afstand. En ze zijn dol op "
                     "aaien. Njeg!",
    STAL + "aai": "Vahoeg! Aai er drie tot ze rustig knorren: drie aaitjes per zwijntje, dan zakt het gestuiter vanzelf. Lege hand, "
                  "zachte pootjes.",
    STAL + "aai_nog": "Nog %s Worstzwijntjes kalmeren. Rustig aan: drie aaitjes per zwijntje, met een lege hand.",
    STAL + "voer": "Hoor je dat? Tevreden geknor! Nu hebben ze trek. Hier, een zak zwijnenvoer. Schep hem leeg in de voerbak, "
                   "helemaal achter in het gangpad.",
    STAL + "voer_nog": "De voerbak staat aan het eind van het gangpad. Rechtsklik hem met de zak zwijnenvoer.",
    STAL + "voer_kwijt": "Zak kwijt? Hier, ik heb er nog eentje. Niet zelf opeten: het smaakt naar karton. Vraag maar niet hoe ik dat weet.",
    STAL + "ontsnapt": "Smakken maar! Eén, twee, drie, vier, vijf... wacht. Waar is Knorretje? Zijn box staat open! KNORRETJE IS "
                       "ONTSNAPT! Hij zit vast ergens bij de wei. Sluip naar hem toe, anders schrikt hij en rent hij weg. En til "
                       "hem dan op, njeg!",
    STAL + "zoek_nog": "Knorretje verstopt zich graag achter de hooibalen in de wei, bij de kar of op de hooizolder. Sluipen, hè! "
                       "Hij schrikt van stampende poten.",
    STAL + "klaar": "KNORRETJE! Kom maar bij de knecht. Je bent een held, njeg! Weet je wat? De Mika's tellen hun zwijntjes toch "
                    "nooit. Hier: twee kleintjes, elk in een mandje. Zorg goed voor ze: aaien, borstelen en knabbelvoer. Dan "
                    "snuffelen ze elke dag iets lekkers voor je op.",
    STAL + "dank0": "Hoe gaat het met je Worstzwijntjes? Krijgen ze genoeg aaitjes?",
    STAL + "dank1": "Een blij Worstzwijntje snuffelt elke dag iets op. Soms een knabbel, soms iets van de Barbecuether. Njeg!",
    STAL + "dank2": "Niet tegen de Mika's zeggen dat ik de zwijntjes extra voer geef. Straks ben ik de stalknecht van... niks.",
    STAL + "optie.ja": "Ik help je!",
    STAL + "optie.wat": "Wat is een Worstzwijntje?",
    STAL + "optie.zoek": "Ik ga hem zoeken!",
    STAL + "optie.dank": "Dankjewel!",
    STAL + "hint.aai": "aai drie Worstzwijntjes in de stal (lege hand, drie aaitjes per zwijntje)",
    STAL + "hint.voer": "rechtsklik de voerbak achter in het gangpad met de zak zwijnenvoer",
    STAL + "hint.zoek": "zoek Knorretje bij de wei, sluip naar hem toe en til hem op",
    GUI + "aai.1": "Knor? Het Worstzwijntje stuitert al iets minder.",
    GUI + "aai.2": "Knorrr... Bijna rustig. Nog één aaitje!",
    GUI + "aai.3": "Knorrr! Dit Worstzwijntje is helemaal rustig. (%s van de 3)",
    GUI + "aai.al": "Dit Worstzwijntje heb jij al gekalmeerd. Het knort tevreden.",
    GUI + "aai.klaar": "Drie rustige Worstzwijntjes! Ga terug naar de Stalknecht-guh.",
    GUI + "voerbak": "Smak smak smak! De Worstzwijntjes storten zich op het voer. Ga terug naar de Stalknecht-guh.",
    GUI + "schrik": "Knorretje schrikt van je gestamp en rent weg! Sluip er de volgende keer heen.",
    GUI + "moe": "Knorretje is moe van het rennen. Nu kun je hem zo oppakken.",
    GUI + "gevangen": "Je hebt Knorretje! Hij knort zachtjes in je armen. Breng hem naar de Stalknecht-guh.",
    GUI + "niet_van_jou": "Dit is het ontsnapte zwijntje van %s. Dat van jou zit ergens anders.",
    GUI + "knorretje": "Knorretje",
    # --- the Tolwachter-Mika -----------------------------------------------------------------------------------------------
    TOL + "hallo": "HALT! Dit is de tolbrug van het Mika-brugpaleis en ik ben de Tolwachter. Niemand komt door mijn bek... eh, door "
                   "DE bek, zonder tol. En de tol is: drie raadsels. Ik had liever knabbels gehad, maar de baas zegt raadsels. Njeg njeg njeg.",
    TOL + "raadsel_goed": "Grr. Goed. Dat is er %s van de 3.",
    TOL + "raadsel_fout": "FOUT! Njeg njeg njeg! Denk nog maar eens goed na. Ik heb er nog meer.",
    TOL + "raadsels_klaar": "Drie goed?! Wie heeft jou de antwoorden verteld... Nou ja. Afspraak is afspraak. Je mag door.",
    TOL + "brug": "Maar eh... er is één dingetje. De brug is kapot. Alweer. De planken vallen er steeds uit, snap jij het? Hier: "
                  "vijf nieuwe planken. Leg jij ze er even in? Ik heb het te druk met tol wachten.",
    TOL + "brug_nog": "De planken moeten in het gat, voorbij mijn tolhuis. Nog %s rijen. Val je erin? Onder het gat hangt een "
                      "steiger, met een ladder terug omhoog.",
    TOL + "brug_kwijt": "Planken kwijt? In de saus laten vallen zeker. Hier, ik heb er nog. Ik heb er ALTIJD nog.",
    TOL + "brug_wacht": "Iemand anders heeft de brug net gemaakt. Wacht maar even: hij stort zo weer in. Mika-kwaliteit!",
    TOL + "bel_nog": "De brug is heel? Luid dan de tolbel in de klokkentoren aan de overkant. Dan weet iedereen dat de brug weer "
                     "open is. En dat er weer raadsels te raden vallen!",
    TOL + "klaar": "DONG! Ik hoorde hem tot hier. Mooi werk. Vooruit: jij hoeft nooit meer een raadsel te raden op mijn brug. En hier, de "
                   "bouwtekening van de brug, met wat planken en touwleuning. Kun je thuis je eigen brug bouwen. Zonder tol. "
                   "Zonde, maar goed.",
    TOL + "dank0": "Vrije doorgang voor jou. Maar zeg het niet tegen de anderen, straks wil iedereen het.",
    TOL + "dank1": "De brug is alweer ingestort? Ja. Dat doet hij. Daar is het een Mika-brug voor.",
    TOL + "dank2": "Wil je nog een raadsel? Wat is geel en kost acht knabbels? ...Niks. Ik wil gewoon acht knabbels.",
    TOL + "tekening_kwijt": "Bouwtekening kwijt? Hier is een nieuwe. Ik teken ze zelf. Daarom staat de brug er scheef op.",
    TOL + "optie.raadsels": "Kom maar op met die raadsels!",
    TOL + "optie.later": "Ik kom later terug",
    TOL + "optie.ok": "Komt goed!",
    TOL + "optie.dank": "Dankjewel!",
    TOL + "hint.brug": "leg de vijf planken in het gat in de brug (rechtsklik met een plank, vlak bij het gat)",
    TOL + "hint.bel": "loop over de brug naar de klokkentoren en luid de tolbel",
    TOL + "hint.terug": "ga terug naar de Tolwachter-Mika voor je beloning",
    GUI + "tol.halt": "Ho ho! Eerst drie raadsels raden, njeg!",
    GUI + "tol.plank": "Tik tik tik! Nog %s rijen planken.",
    GUI + "tol.plank.op": "Je planken zijn op, maar er missen nog %s rijen. Haal nieuwe planken bij de Tolwachter-Mika.",
    GUI + "tol.plank.ver": "Loop naar het gat in de tolbrug om deze plank te leggen.",
    GUI + "tol.plank.heel": "De brug is al heel. Wacht tot hij weer instort (Mika-kwaliteit).",
    GUI + "tol.plank.niet_nu": "Deze planken zijn voor de tolbrug. Praat eerst met de Tolwachter-Mika.",
    GUI + "tol.brug_heel": "De brug is heel! Loop snel naar de klokkentoren en luid de tolbel, voordat hij weer instort.",
    GUI + "tol.bel": "DONG! De tolbel galmt over de hele brug. Ga terug naar de Tolwachter-Mika.",
    GUI + "tol.bel.niet_nu": "Dong. Mooi geluid. Maar de Tolwachter weet van niks: praat eerst met hem.",
    GUI + "beloning.doorgang": "Vrije doorgang over de tolbrug, voor altijd",
    GUI + "beloning.zwijntjes": "Twee Worstzwijntjes in een mandje (voor thuis)",
    GUI + "nodig.raadsels": "Raadsels van de Tolwachter goed geraden",
    # --- the signs in the templates ----------------------------------------------------------------------------------------
    SIGN + "brug.welkom1": "TOLBRUG", SIGN + "brug.welkom2": "Mika-brugpaleis", SIGN + "brug.welkom3": "Tol: 3 raadsels",
    SIGN + "brug.tol1": "HALT! TOL!", SIGN + "brug.tol2": "3 raadsels raden", SIGN + "brug.tol3": "Knabbels? Nee.",
    SIGN + "brug.tol4": "(helaas, njeg)",
    SIGN + "brug.dag1": "Tot ziens!", SIGN + "brug.dag2": "Kom nog eens", SIGN + "brug.dag3": "raden, njeg",
    SIGN + "brug.kantoor1": "Tolkantoor", SIGN + "brug.kantoor2": "Klachten?", SIGN + "brug.kantoor3": "Kosten ook tol.",
    SIGN + "brug.wacht1": "Wachtkamer", SIGN + "brug.wacht2": "Wachten is", SIGN + "brug.wacht3": "gratis. Nog wel.",
    SIGN + "brug.spaar1": "Spaarpotje van", SIGN + "brug.spaar2": "de Tolwachter", SIGN + "brug.spaar3": "AFBLIJVEN!",
    SIGN + "brug.gat1": "PAS OP!", SIGN + "brug.gat2": "Brug kapot.", SIGN + "brug.gat3": "(alweer, njeg)",
    SIGN + "brug.steiger1": "Gevallen?", SIGN + "brug.steiger2": "De ladder zit", SIGN + "brug.steiger3": "aan het eind.",
    SIGN + "brug.bel1": "TOLBEL", SIGN + "brug.bel2": "Luiden als de", SIGN + "brug.bel3": "brug heel is!",
    SIGN + "stal.deur1": "MIKA-STAL", SIGN + "stal.deur2": "Worstzwijntjes", SIGN + "stal.deur3": "Niet plagen!",
    SIGN + "stal.naam0": "Knir", SIGN + "stal.naam1": "Snuffel", SIGN + "stal.naam2": "Mosterdje", SIGN + "stal.naam3": "Truffel",
    SIGN + "stal.leeg1": "Knorretje", SIGN + "stal.leeg2": "(het hekje stond", SIGN + "stal.leeg3": "weer eens open)",
    SIGN + "stal.baas1": "Opgesnuffeld:", SIGN + "stal.baas2": "voor de BAAS", SIGN + "stal.baas3": "(niet de knecht)",
    SIGN + "stal.bord1": "Mika-stal", SIGN + "stal.bord2": "Zwijntjes aaien", SIGN + "stal.bord3": "mag (stiekem)",
    SIGN + "woon.poort1": "MIKA-WOONBLOKKEN", SIGN + "woon.poort2": "Hier wonen wij.", SIGN + "woon.poort3": "Niet aanbellen!",
    SIGN + "woon.post1": "Brievenbussen", SIGN + "woon.post2": "Reclame? NEE.", SIGN + "woon.post3": "Knabbels? JA.",
    SIGN + "woon.regels1": "HUISREGELS", SIGN + "woon.regels2": "1. Niet mopperen", SIGN + "woon.regels3": "na bedtijd",
    SIGN + "woon.regels4": "2. Zie regel 1",
    SIGN + "woon.vuil1": "Grofvuil", SIGN + "woon.vuil2": "(geen gejatte", SIGN + "woon.vuil3": "knabbelzakken)",
    SIGN + "woon.ketel1": "Ketelhuis", SIGN + "woon.ketel2": "Verwarming voor", SIGN + "woon.ketel3": "de hele flat",
    SIGN + "woon.saus1": "Saustoren", SIGN + "woon.saus2": "Warme saus uit", SIGN + "woon.saus3": "elke kraan",
    SIGN + "woon.dak1": "Daktuin", SIGN + "woon.dak2": "Pindascheutjes", SIGN + "woon.dak3": "van de buren",
    SIGN + "woon.oma1": "Mika-oma", SIGN + "woon.oma2": "Soep & breiwerk", SIGN + "woon.oma3": "Poten vegen!",
    SIGN + "woon.mand1": "Breimandje", SIGN + "woon.mand2": "LEEG?! Waar is", SIGN + "woon.mand3": "mijn breiwerk?",
}

for _nr, _naam in enumerate(NAMEN):
    TEXTS[f"entity.guhs.paleizen_mopper_mika.{_nr}"] = _naam
for _i, (_vraag, _antwoorden, _goed) in enumerate(RAADSELS):
    TEXTS[f"{TOL}raadsel.{_i}"] = _vraag
    for _j, _a in enumerate(_antwoorden):
        TEXTS[f"{TOL}raadsel.{_i}.{'abc'[_j]}"] = _a

# the Guhdex page of the Worstzwijntje (bbq2.pagina: name, rarity, info)
PAGINA = ("Worstzwijntje", "Ongewoon (in de Mika-stal van de Guhbarbecuether)",
          "Een braadworstje op vier pootjes, met een snuitje, twee slagtandjes, een streep mosterd over zijn rug en een krulstaart "
          "met een knoopje erin. De Mika's laten het knabbels opsnuffelen. Help de Stalknecht-guh, dan krijg je er zelf twee: aai, "
          "borstel en voer ze, en ze snuffelen elke dag iets lekkers voor je op. Knor, njeg!")

STRUCTUREN = {
    "mika_woonblokken": ("Mika-woonblokken", "De flats van de Nether-Mika's: galerijen, waslijnen en Mika-oma met haar soep (Guhbarbecuether)"),
    "mika_stal": ("Mika-stal", "De stal van de Worstzwijntjes, met de Stalknecht-guh voor de grote deur (Guhbarbecuether)"),
    "mika_brugpaleis": ("Mika-brugpaleis", "Een tolbrug door de bek van een reusachtige Mika. De Tolwachter wil drie raadsels van je horen (Guhbarbecuether)"),
}

# visible advancements (tab guhs:barbecuether/): name -> (parent, icon, frame, title, text, structure or None)
ADVANCEMENTS = {
    "paleizen_woonblokken": ("binnen", "minecraft:orange_stained_glass_pane", "task", "Bij de Mika's thuis", "Vind de Mika-woonblokken", "mika_woonblokken"),
    "paleizen_mika_oma": ("paleizen_woonblokken", "guhs:paleizen_omasoep", "goal", "Soep van oma",
                          "Breng de soep van Mika-oma naar de mopperaars en vind haar breiwerk terug", None),
    "paleizen_stal": ("binnen", "minecraft:hay_block", "task", "Knor!", "Vind de Mika-stal", "mika_stal"),
    "paleizen_stalknecht": ("paleizen_stal", "guhs:paleizen_worstzwijntje_mandje", "goal", "Zwijntjesfluisteraar",
                            "Kalmeer de Worstzwijntjes, vul de voerbak en vang Knorretje", None),
    "paleizen_brugpaleis": ("binnen", "guhs:paleizen_brugplank", "task", "Door de bek van de Mika", "Vind het Mika-brugpaleis", "mika_brugpaleis"),
    "paleizen_tolwachter": ("paleizen_brugpaleis", "minecraft:bell", "goal", "Tolvrij!",
                            "Raad de raadsels, repareer de brug en luid de tolbel", None),
}
# hidden ones for FTB tasks (besides quest/<lijn>_stap_<i>)
VERBORGEN = ["paleizen_raadsels", "paleizen_snuffel", "paleizen_korting"]
