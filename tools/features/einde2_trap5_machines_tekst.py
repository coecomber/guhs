"""
einde2 tier 5 (trap5_machines): every Dutch text of the slice (the English is tools/lang/en/c100_e2_trap5_machines.json).
Not a feature module: einde2_trap5_machines.py writes these. The vadskracht numbers come from T5Getallen.java (getal()).

The Werktuigguh is the parody of the inventor of the Northern Air Temple: goggles on his forehead, a monocle on a chain,
three wooden fingers, never finished ("nog niet af, njeg"), once the builder of the Fire Nation's war balloons and sorry for
it. Every line is in our own words.
"""
import os
import re

_JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "trap5", "T5Getallen.java")
_GETALLEN = None


def getal(naam):
    """A number of T5Getallen.java (the one place where they live)."""
    global _GETALLEN
    if _GETALLEN is None:
        src = open(_JAVA, encoding="utf-8").read()
        _GETALLEN = {k: int(v) for k, v in re.findall(r"\b([A-Z_]+)\s*=\s*(\d+)L?\b", src)}
    if naam not in _GETALLEN:
        raise SystemExit(f"einde2_trap5_machines_tekst: no number {naam} in T5Getallen.java")
    return _GETALLEN[naam]


G = getal
P = "gui.guhs.einde2_trap5_machines."
Q = "quest.guhs.einde2_trap5_machines.werktuigguh."
SCENE = "einde2_trap5_machines_werkplaats"
LIJN = "werktuigguh"
SECTIE = "Trap 5: de werkplaats van de Werktuigguh"

# --- the blocks: (id, name, lore) ---------------------------------------------------------------------------------------
BLOKKEN = {
    "vadszender": ("Vadszender",
                   "Zendt de vadskracht van zijn net naar Vadsontvangers in de buurt. Zijn oortjes wiebelen als hij zendt"),
    "vadsontvanger": ("Vadsontvanger",
                      f"Geeft zijn net precies wat het tekortkomt, zolang een Vadszender binnen {G('ZENDER_BEREIK')} blokken zendt"),
    "guhterminal": ("Guhterminal",
                    "Je eigen Bank Guh, van overal: koppel hem met de Banksleutel. Ook vanuit een andere dimensie"),
    "bestelbalie": ("Bestelbalie",
                    "Hier werkt de Bestelguh. Hij laat de Knutselmachines in de buurt maken wat je bestelt, uit je bank"),
    "knabbelpoort_buis": ("Knabbelpoort-buis",
                          "Een buisstuk met een piepklein Knabbelpoortje erin: wat erin gaat, komt bij zijn maatje eruit. Sluip-klik ze allebei"),
    "thuispoort": ("Thuispoort",
                   "Je eigen netwerk van poorten, zo groot als je wilt. Je guhs aan de lijn en je rijdier springen mee"),
    "enderguhrad": ("Enderguhrad",
                    "Een reuzenrad waarin een Vahoege Enderguh rondjes vliegt. Eén per opstelling telt"),
    "komeetkern": ("Komeetkern",
                   "Een scherf van de knabbelkomeet. Hij gloeit nog na, en hoe"),
    "luchtblazer": ("Luchtblazer",
                    "Blaast spullen, guhs en spelers weg. Neergezet terwijl je naar beneden kijkt blaast hij omhoog: een liftje"),
    "waterdrager": ("Waterdrager",
                    "Bevloeit de akker om zich heen, vult ketels naast zich en lege emmers"),
    "korstduwer": ("Korstduwer",
                   f"Een zuiger op vadskracht: bij een redstonepuls (of een klik) schuift hij een rij van hooguit {G('DUWER_MAX')} blokken op"),
    "vuurkorf": ("Vuurkorf",
                 "Verwarmt de grond om zich heen: wat er even ligt, wordt gebakken, en sneeuw en ijs smelten"),
}

ITEMS = {
    "einde2_trap5_machines_bouwplan": ("Bouwplan van de Werktuigguh",
                                       "Voor alle machines van trap 5. Vol vlekken, doorhalingen en een tekeningetje van een guh met een vliegtuigje"),
    "einde2_trap5_machines_diploma": ("Diploma van de Werktuigguh",
                                      "\"Hierbij verklaar ik, de Werktuigguh, dat deze Guhvatar meer heeft afgemaakt dan ik ooit. Getekend, met drie houten vingers.\""),
}

ENTITY = {"entity.guhs.bestelguh": "Bestelguh"}

# --- every message of the machines (block.guhs.<id>.<key>) ------------------------------------------------------------------
BERICHTEN = {
    "vadszender": {
        "weigert": "Deze zender zendt niet: in zijn eigen net zit een Vadsontvanger. Wat binnenkomt zend ik niet door, njeg",
        "stand": "%1$s van %2$s ontvangers, zendt %3$s vadskracht door (%4$s)",
        "regel": "Zendt %s vadskracht door naar zijn ontvangers",
    },
    "vadsontvanger": {
        "geen_zender": "Geen Vadszender gevonden binnen %s blokken",
        "stand": "Luistert naar de zender bij %1$s %2$s %3$s: geeft %4$s van %5$s vadskracht",
    },
    "guhterminal": {
        "sleutel_leeg": "Klik eerst je Bank Guh aan met de Banksleutel, dan de terminal",
        "gekoppeld": "Gekoppeld aan de bank bij %1$s %2$s %3$s (%4$s)",
        "gekoppeld.weg": "Gekoppeld, maar die bank staat nu nergens",
        "oefen": "De Werktuigguh: \"Een oefenterminal, njeg! Die van jou koppel je met de Banksleutel aan je eigen Bank Guh. Dan zie je je hele bank, waar je ook bent.\"",
        "los": "Deze terminal is nog niet gekoppeld: Banksleutel op je Bank Guh, dan op de terminal",
        "slaapt": "De terminal slaapt: hij heeft vadskracht nodig",
        "bank_weg": "Je bank is nergens te vinden. Staat hij nog?",
        "geen_balie": "Geen Bestelbalie van deze bank in de buurt (de balie moet geladen zijn)",
    },
    "bestelbalie": {
        "sleutel_leeg": "Klik eerst je Bank Guh aan met de Banksleutel, dan de balie",
        "gekoppeld": "De Bestelguh weet nu bij welke bank hij moet zijn",
        "oefen": "De Werktuigguh: \"Zet zo'n balie naast je Knutselmachines, koppel hem aan je bank, en bestel! De Bestelguh rent zich het schortje van het lijf.\"",
        "los": "Deze balie is nog niet gekoppeld: Banksleutel op je Bank Guh, dan op de balie",
        "geen_machines": f"Er staan geen Knutselmachines met een Bouwtekening binnen %s blokken",
        "al_bezig": "De Bestelguh is nog bezig met %1$s × %2$s. Sluip-klik de balie om dat te annuleren",
        "slaapt": "De Bestelbalie slaapt: hij heeft vadskracht nodig",
        "te_veel": "Dat is te veel in één keer (hooguit %s keer knutselen per bestelling)",
        "mist": "Dat lukt niet: er mist %1$s × %2$s, en geen machine hier kan het maken",
        "aangenomen": "Bestelling aangenomen: %1$s × %2$s (%3$s stappen). De Bestelguh rent al!",
        "opgegeven.mist": "De Bestelguh geeft het op: hij wachtte te lang op %s",
        "opgegeven": "De Bestelguh geeft het op: er gebeurde te lang niets",
        "machine_weg": "De Bestelguh geeft het op: een machine van de bestelling is weg of maakt nu iets anders",
        "wacht": "De Bestelguh wacht op %1$s × %2$s in de bank",
        "bonnetje": "Bonnetje van de Bestelguh: %1$s × %2$s, netjes in je bank. Dank je wel, njeg!",
        "geannuleerd": "Bestelling geannuleerd. Wat al gemaakt is ligt in je bank",
        "wacht_op_bestelling": "De Bestelguh wacht op een bestelling",
        "bezig": "Bezig met %1$s × %2$s: stap %3$s van %4$s",
    },
    "knabbelpoort_buis": {
        "oefen": "De Werktuigguh: \"Sluip-klik de ene, sluip-klik de andere, en ze zijn maatjes. Wat er hier in gaat, komt daar uit, al ligt daar een andere dimensie!\"",
        "eerste": "Onthouden! Sluip-klik nu de andere kant",
        "eerste_weg": "De eerste kant is er niet meer (of niet geladen). Begin opnieuw",
        "gekoppeld": "Gekoppeld met de buis bij %1$s %2$s %3$s (%4$s)",
        "los": "Nog geen maatje: sluip-klik deze buis en dan een andere",
        "stand": "Maatje bij %1$s %2$s %3$s (%4$s)",
    },
    "thuispoort": {
        "icoon": "Icoon van deze poort: %s",
        "naam_n": "Thuispoort %s",
        "niet_van_jou": "Deze poort is niet van jou, en de eigenaar heeft je niet toegelaten",
        "lading": "Lading: %1$s van %2$s vadskracht",
        "kost": "Een sprong kost %s vadskracht",
        "wie_niet": "Geen speler met de naam %s online",
        "te_weinig": "Te weinig lading: %1$s vadskracht, een sprong kost %2$s vadskracht. Geef het net wat tijd",
        "weg": "Die poort is er niet meer",
        "oefen": "Hup, naar de andere oefenpoort!",
    },
    "enderguhrad": {
        "bezet": "Er vliegt al een Enderguh in dit rad",
        "alleen_vahoege": "Alleen een Vahoege Enderguh vliegt zo hoog. Een gewone guh krijgt er hoogtevrees van",
        "vliegt": "Een Vahoege Enderguh vliegt zijn rondjes: %s vadskracht",
        "leeg": "Leeg rad: zet er een Vahoege Enderguh in",
    },
    "komeetkern": {
        "regel": "Komeetkern: %s vadskracht",
    },
    "waterdrager": {
        "slaapt": "De Waterdrager heeft vadskracht nodig",
    },
    "korstduwer": {
        "slaapt": "De Korstduwer heeft vadskracht nodig",
        "vast": "Vast: er zit iets in de rij dat niet mag schuiven, of de rij is langer dan %s",
    },
}

# --- gui.guhs.einde2_trap5_machines.* -----------------------------------------------------------------------------------
GUI = {
    "vraagt": "Vraagt %s vadskracht",
    "geeft": "Geeft %s vadskracht",
    "aan": "aan",
    "uit": "slaapt",
    "deur_open": "De deur van de werkplaats laat je door. De Werktuigguh wacht binnen!",
    "komeet.gepakt": "Je raapt de scherf van de knabbelkomeet op. Hij is nog warm. De Komeetkern is van jou!",
    "project.p_zender": "Project af: een Vadsontvanger voedt een machine uit de verte!",
    "project.p_terminal": "Project af: je bank, gewoon via een Guhterminal!",
    "project.p_bestel": "Project af: de Bestelguh heeft je eerste bestelling gebracht!",
    "project.p_poortbuis": "Project af: iets ging dwars door een Knabbelpoort-buis!",
    "project.p_thuispoort": "Project af: je eerste sprong met een Thuispoort!",
    "project.p_lucht": "Elementmachine: de Luchtblazer blaast!",
    "project.p_water": "Elementmachine: de Waterdrager bevloeit!",
    "project.p_aarde": "Elementmachine: de Korstduwer duwt!",
    "project.p_vuur": "Elementmachine: de Vuurkorf bakt!",
    "project.p_enderguhrad": "Nieuwe bron: een Vahoege Enderguh vliegt in je Enderguhrad!",
    "project.p_komeet": "Nieuwe bron: de Komeetkern gloeit in je net!",
    "nodig.elementen": "Elementmachines die werken",
    "nodig.enderguhrad": "Een draaiend Enderguhrad",
    "nodig.komeetkern": "Een Komeetkern in je net",
    "doel.werkplaats": "De werkplaats van de Werktuigguh",
    "doel.komeet": "De scherf van de knabbelkomeet",
    "beloning.thuispoort": "Een extra Thuispoort",
    "bestellen": "Bestellen",
    "bestellen.tooltip": "Alles wat de Bestelguhs van deze bank kunnen laten maken",
    "bestel.titel": "Bestellen bij de Bestelguh",
    "bestel.uitleg": "Wat de machines rond %s balie(s) kunnen maken:",
    "bestel.leeg": "Nog niets. Zet Knutselmachines met een Bouwtekening naast een Bestelbalie die aan deze bank vastzit.",
    "bestel.item": "%1$s (%2$s per keer), in de bank: %3$s",
    "bestel.item_onbekend": "%1$s (%2$s per keer)",
    "bestel.gekozen": "%1$s × %2$s",
    "bestel.knop": "Bestellen!",
    "poort.naam": "Naam van deze poort",
    "poort.rij": "%1$s  (%2$s, %3$s, %4$s)",
    "poort.ga": "Spring naar %1$s (%2$s)",
    "poort.wie": "Naam van een speler",
    "poort.icoon": "Icoon",
    "poort.icoon.tooltip": "Het item in je hand wordt het icoontje van deze poort",
    "poort.toelaten": "Toelaten",
    "poort.weigeren": "Weigeren",
    "poort.lading": "Lading %1$s / %2$s vadskracht, een sprong kost %3$s vadskracht",
    "poort.geen": "Nog geen andere poorten. Zet er nog een neer, waar je maar wilt: ook in een andere dimensie.",
    "poort.alleen_jij": "Alleen jij mag je poorten gebruiken",
    "poort.ook": "Ook welkom: %s",
}

# --- the Werktuigguh ---------------------------------------------------------------------------------------------------
WERKTUIGGUH = {
    "nog_niet": "Nog niet af, njeg! Kom terug als je de hele wereld weer in balans hebt gebracht. Dat duurt even, ik weet het.",
    "welkom1": "Ah! Mijn werkplaats! Kijk niet naar de rommel, kijk naar de mogelijkheden. Pas op voor die tandwielen, die bijten.",
    "welkom2": "Honderd jaar heb ik dingen half gemaakt. Een vleugel zonder veren, een kachel zonder vuur. Ik wachtte op iemand die het zou afmaken.",
    "welkom3": "Hier, mijn Bouwplan. Leg het in elk recept van mijn machines: het blijft gewoon liggen. Elk project laat iets werkends achter in JOUW wereld.",
    "hint.plan": "Het Bouwplan blijft in het werkbankrooster liggen. Kwijt? Sluip-klik de Werktuigguh voor een nieuw.",
    "project1.a": "Project één: kracht door de lucht. Een Vadszender zendt de vadskracht van zijn net naar ontvangers in de buurt.",
    "project1.b": f"Zet een Vadszender op je net en een Vadsontvanger binnen {G('ZENDER_BEREIK')} blokken, met een machine eraan. Tekort? Dan staat alles stil, zoals altijd.",
    "hint.project1": "Kijk ook naar oefenopstelling 1 in de werkplaats.",
    "project2.a": "Project twee: je bank, overal. Een Guhterminal is een raampje in je Bank Guh. Geen tablet, njeg: een blok.",
    "project2.b": "Klik je Bank Guh met de Banksleutel en dan de terminal. Geef hem vadskracht, klik hem, en kijk: je bank!",
    "hint.project2": "Een terminal werkt ook in een andere dimensie dan je bank.",
    "project3.a": "Project drie: de Bestelguh. Hij werkt aan een Bestelbalie naast je Knutselmachines, met een schortje. Een lief schortje.",
    "project3.b": "Koppel de balie aan je bank. Bestel bij de balie of via de knop Bestellen op je terminal: hij haalt alles uit de bank en laat de machines draaien.",
    "hint.project3": "Ook tussenproducten maakt hij, als er een machine voor staat.",
    "project4.a": "Project vier: post door de poort. Een Knabbelpoort-buis heeft een piepklein Knabbelpoortje. Ik heb er zes jaar over gedaan. Het waren vooral de oortjes.",
    "project4.b": "Sluip-klik de ene buis, sluip-klik de andere, en ze zijn maatjes. Stop er iets in: het komt er bij de ander uit, ook in een andere dimensie.",
    "hint.project4": "Beide kanten moeten geladen zijn en vadskracht hebben.",
    "project5.a": "Project vijf: thuis in één stap. Een Thuispoort is van jou alleen, tenzij je iemand toelaat. Zet er zoveel neer als je wilt.",
    "project5.b": "Hij laadt zich op met wat je net overhoudt. Hoe meer poorten je hebt, hoe meer een sprong kost. Je guhs aan de lijn springen mee!",
    "hint.project5": "Houd een item vast, open de poort en klik op Icoon: dat wordt zijn icoontje.",
    "project6.a": "Project zes: de vier elementen, in machines. Lucht, water, aarde, vuur. Ik heb ze ooit voor de oorlog gebouwd. Nu doen ze iets liefs.",
    "project6.b": "Een Luchtblazer, een Waterdrager, een Korstduwer en een Vuurkorf. Laat ze elk één keer hun ding doen in jouw wereld.",
    "hint.project6": "Een Luchtblazer neergezet terwijl je naar beneden kijkt wordt een liftje.",
    "project7.a": f"Project zeven: nieuwe bronnen. Een Enderguhrad: een reuzenrad waarin een Vahoege Enderguh rondvliegt. {G('ENDERGUHRAD')} vadskracht!",
    "project7.b": "En de scherf van de knabbelkomeet ligt nog bij het duelplein in de Vuurnatie-hoofdstad. Raap hem op en zet hem in je net. Hij gloeit nog steeds.",
    "hint.project7": "De scherf ligt alleen voor jou klaar. Stap erop om hem te pakken.",
    "kit": "Hier, het eerste exemplaar maak ik voor je. De rest knutsel je zelf, met mijn Bouwplan.",
    "alles_af": "Alles af? Echt alles? Kom, kom, vertel!",
    "klaar1": "Je hebt het gedaan. Alles werkt. Niets is half. Ik moet even gaan zitten.",
    "klaar2": "Vroeger bouwde ik ballonnen die vuur lieten regenen. Ik dacht dat ik nooit meer iets goeds zou maken.",
    "klaar3": "Hier: een diploma. Ik heb het helemaal afgemaakt. Mijn eerste afgemaakte ding in honderd jaar. En een extra Thuispoort, voor onderweg.",
    "hint.ballon": "Naast de werkplaats ligt een bouwplaats. De Werktuigguh heeft nog één groot plan...",
    "tip0": "Een machine met een snoet die slaapt heeft geen vadskracht. Een verbaasde snoet zit vol. Een blije snoet: alles goed!",
    "tip1": "Een Vadsontvanger geeft precies wat zijn net tekortkomt. Niet meer, niet minder. Netjes, njeg.",
    "tip2": "De Komeetkern is de sterkste bron tot nu toe. Tot nu toe! Ik heb nog plannen. Altijd.",
    "tip3": "Mijn houten vingers? Ach. Een experiment met een stoomdraaimolen. Hij draait nog steeds, ergens.",
    "tip4": "Weet je wat ik het mooiste vind van uitvinden? Dat iets wat niet bestond, ineens iemand helpt.",
}

# --- the door scene ----------------------------------------------------------------------------------------------------
SCENE_TEKST = ("De werkplaats gaat open", {
    "zin1": "Wie klopt daar zo vadsig aan mijn deur? ...Bij al mijn tandwielen. De Guhvatar zelf!",
    "zin2": "Honderd jaar heb ik aan deze deur gesleuteld. Hij was nog niet af, njeg. Vandaag wel.",
    "zin3": "Vroeger bouwde ik oorlogsballonnen voor de Vuurnatie. Daar ben ik niet trots op. Echt niet.",
    "zin4": "Nu er weer vrede is, wil ik dingen maken waar niemand bang van wordt. Kom binnen! Mijn oortjes tintelen al.",
}, {"werktuigguh": "Werktuigguh"})

# --- the questline (verhaal_motor.verhaallijn: (stapnaam, nu, waar) per step) -------------------------------------------------
LIJN_NAAM = "De Werktuigguh"
LIJN_UITLEG = ("Na de finale gaat de deur van de werkplaats in de Zuidelijke Luchttempel open. De Werktuigguh, uitvinder en ooit "
               "bouwer van oorlogsballonnen, leert je trap 5 van de Guh-technologie in projecten. Elk project laat iets werkends "
               "achter in je eigen wereld.")
STAPPEN = [
    ("De werkplaats", "Praat met de Werktuigguh in zijn werkplaats", "De werkplaats in de Zuidelijke Luchttempel"),
    ("Kracht door de lucht", "Voed een machine met een Vadszender en een Vadsontvanger", "In je eigen wereld"),
    ("Je bank, overal", "Open je bank via een Guhterminal", "In je eigen wereld"),
    ("De Bestelguh", "Laat de Bestelguh een bestelling voor je maken", "In je eigen wereld"),
    ("Post door de poort", "Stuur iets door een Knabbelpoort-buis", "In je eigen wereld"),
    ("Thuis in één stap", "Spring met een Thuispoort", "In je eigen wereld"),
    ("De vier elementen", "Laat de Luchtblazer, de Waterdrager, de Korstduwer en de Vuurkorf elk hun werk doen", "In je eigen wereld"),
    ("Nieuwe bronnen", "Laat een Vahoege Enderguh vliegen in een Enderguhrad en zet de Komeetkern in je net",
     "De scherf ligt bij het duelplein van de Vuurnatie-hoofdstad"),
    ("Alles af", "Vertel het de Werktuigguh", "De werkplaats in de Zuidelijke Luchttempel"),
]
KLAAR = ("Alles af! De Werktuigguh heeft nog één groot plan: de Vredesballon", "De bouwplaats naast de werkplaats")

# --- advancements: hidden (FTB tasks) and visible (tab Guheinde) ----------------------------------------------------------
PROJECTEN = ["p_zender", "p_terminal", "p_bestel", "p_poortbuis", "p_thuispoort", "p_lucht", "p_water", "p_aarde", "p_vuur",
             "p_enderguhrad", "p_komeet"]
VERBORGEN = [f"einde2_trap5_machines_{p}" for p in PROJECTEN] + ["einde2_trap5_machines_komeet_gepakt"]
ZICHTBAAR = [
    # (name, parent, icon, frame, title, text)
    ("einde2_trap5_machines_werkplaats", "guheinde_winst", "guhs:vadszender", "task", "Nog niet af? Nu wel!",
     "De deur van de werkplaats van de Werktuigguh gaat voor je open"),
    ("einde2_trap5_machines_halverwege", "einde2_trap5_machines_werkplaats", "guhs:thuispoort", "task", "Halverwege de werkbank",
     "Vijf projecten van de Werktuigguh werken in je eigen wereld"),
    ("einde2_trap5_machines_komeet", "einde2_trap5_machines_werkplaats", "guhs:komeetkern", "goal", "Een staartje van de komeet",
     "Raap de scherf van de knabbelkomeet op: de Komeetkern"),
    ("einde2_trap5_machines_klaar", "einde2_trap5_machines_halverwege", "guhs:einde2_trap5_machines_diploma", "challenge", "Helemaal af",
     "Maak alle projecten van de Werktuigguh af"),
]

# --- FTB (chapter guhs_techniek, its own section) ----------------------------------------------------------------------------
# (key, title, text, icon, task) - task ("adv", name)
FTB = [
    ("werkplaats", "De werkplaats gaat open",
     "Na de finale van het Guhvatar-verhaal gaat de deur van de werkplaats in de &dZuidelijke Luchttempel&r open. Praat met de "
     "&6Werktuigguh&r: hij geeft je zijn &6Bouwplan&r, voor alle machines van trap 5.", "guhs:einde2_trap5_machines_bouwplan",
     ("adv", "werktuigguh_stap_1")),
    ("zender", "Kracht door de lucht",
     f"Een &6Vadszender&r zendt de vadskracht van zijn net naar &6Vadsontvangers&r binnen {G('ZENDER_BEREIK')} blokken. Samen zijn ze één net: "
     "tekort ergens = alles stil.", "guhs:vadszender", ("adv", "werktuigguh_stap_2")),
    ("terminal", "Je bank, overal",
     "Een &6Guhterminal&r is een raampje in je Bank Guh. Koppel hem met de &6Banksleutel&r. Zoeken, sorteren, pakken, storten: "
     "met de regels van je bank.", "guhs:guhterminal", ("adv", "werktuigguh_stap_3")),
    ("bestel", "De Bestelguh",
     "De &6Bestelguh&r werkt aan een &6Bestelbalie&r naast je Knutselmachines. Bestel, en hij haalt alles uit de bank, laat de machines "
     "draaien en brengt het bonnetje.", "guhs:bestelbalie", ("adv", "werktuigguh_stap_4")),
    ("poortbuis", "Post door de poort",
     "Een &6Knabbelpoort-buis&r stuurt wat erin gaat naar zijn maatje, ook in een andere dimensie.", "guhs:knabbelpoort_buis",
     ("adv", "werktuigguh_stap_5")),
    ("thuispoort", "Thuis in één stap",
     "Je eigen netwerk van &6Thuispoorten&r. Kies waar je heen springt; je guhs aan de lijn en je rijdier gaan mee.", "guhs:thuispoort",
     ("adv", "werktuigguh_stap_6")),
    ("elementen", "De vier elementen",
     "&6Luchtblazer&r, &6Waterdrager&r, &6Korstduwer&r en &6Vuurkorf&r: laat ze elk hun ding doen.", "guhs:vuurkorf",
     ("adv", "werktuigguh_stap_7")),
    ("enderguhrad", "Het Enderguhrad",
     f"Een &5Vahoege Enderguh&r vliegt rondjes in een reuzenrad: {G('ENDERGUHRAD')} vadskracht. Eén per opstelling telt.",
     "guhs:enderguhrad", ("adv", "einde2_trap5_machines_p_enderguhrad")),
    ("komeet", "Een staartje van de komeet",
     f"De scherf van de knabbelkomeet ligt bij het duelplein van de Vuurnatie-hoofdstad, alleen voor jou. De &6Komeetkern&r: "
     f"{G('KOMEETKERN')} vadskracht, de sterkste bron tot nu toe.", "guhs:komeetkern", ("adv", "einde2_trap5_machines_komeet_gepakt")),
    ("bronnen", "Nieuwe bronnen",
     "Een draaiend Enderguhrad en een gloeiende Komeetkern, allebei in je eigen net.", "guhs:komeetkern", ("adv", "werktuigguh_stap_8")),
    ("klaar", "Helemaal af",
     "Vertel het de Werktuigguh. Voor het eerst in honderd jaar maakt hij iets af: een diploma, voor jou. En hij heeft nog één groot plan...",
     "guhs:einde2_trap5_machines_diploma", ("adv", "werktuigguh_stap_9")),
]
