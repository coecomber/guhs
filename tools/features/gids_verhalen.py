"""
De Guhdex-tab "Verhalen" (Java: feature/gids/VerhalenVoortgang, VerhaalStand, VerhalenPayloads, client/GidsVerhalenTab):
per questline your step, what to do now, whom to visit and where, what you need and the rewards. No registry content,
so this module only writes lang (Dutch in both files):

  gui.guhs.guhdex.tab.verhalen                      the tab name
  gui.guhs.verhalen.<id>.naam / .uitleg             the questline's name and its story in one line
  gui.guhs.verhalen.<id>.stap.<i>                   the name of every step (i = 0 .. stappen-1)
  gui.guhs.verhalen.<id>.nu.<sleutel>               what to do now (sleutel: the step, "klaar", or a variant like 3_bewoner)
  gui.guhs.verhalen.<id>.waar.<sleutel>             whom to visit and where
  gui.guhs.verhalen.*                               the tab's own texts (status, headings, rewards)

The self-check compares the step counts and sleutels with VerhalenVoortgang.java (STAPPEN / EXTRA), so no raw key shows.
"""
import os
import re

JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "gids", "VerhalenVoortgang.java")

ALGEMEEN = {
    "gui.guhs.guhdex.tab.verhalen": "Verhalen",
    "gui.guhs.verhalen.kop.nieuw": "Guhverhalen",
    "gui.guhs.verhalen.kop.oud": "Eerdere avonturen",
    "gui.guhs.verhalen.kop.nu": "Wat nu?",
    "gui.guhs.verhalen.kop.klaar": "Helemaal klaar!",
    "gui.guhs.verhalen.kop.nodig": "Wat je nodig hebt",
    "gui.guhs.verhalen.kop.stappen": "Alle stappen",
    "gui.guhs.verhalen.kop.beloningen": "Beloningen",
    "gui.guhs.verhalen.status.niet_begonnen": "Nog niet begonnen",
    "gui.guhs.verhalen.status.bezig": "Bezig",
    "gui.guhs.verhalen.status.klaar": "Klaar! Vahoeg!",
    "gui.guhs.verhalen.status.stap": "Stap %s van %s: %s",
    "gui.guhs.verhalen.stappen_telling": "%s van de %s stappen gedaan",
    "gui.guhs.verhalen.binnen": "binnen!",
    "gui.guhs.verhalen.nog_niet": "nog niet",
    "gui.guhs.verhalen.terug": "< Terug",
    "gui.guhs.verhalen.laden": "De verhalen worden opgehaald... njeg, even geduld!",
    "gui.guhs.verhalen.klik": "Klik een verhaal aan: dan zie je precies wat je nu moet doen",
    "gui.guhs.verhalen.klik_rij": "Klik voor alle stappen",
    "gui.guhs.verhalen.ding.planken": "Planken (van elk hout)",
    "gui.guhs.verhalen.ding.snacks": "Lekkere snacks",
    "gui.guhs.verhalen.ding.roze_wol": "Roze wol",
    "gui.guhs.verhalen.ding.rps": "Keer op rij gewonnen van Mika-baas",
    "gui.guhs.verhalen.beloning.kleertjes": "%s kleertjes voor je guhs",
    "gui.guhs.verhalen.beloning.held_van_nomguh": "De titel \"Held van Nomguh\"",
    "gui.guhs.verhalen.beloning.sledesprint": "Sledesprint racen tegen Steele-Mika",
    "gui.guhs.verhalen.beloning.getemd": "Je eigen %s (eenmalig temmen)",
    "gui.guhs.verhalen.beloning.wolkjes": "Guhs terughalen uit de wolkjes (al %s keer)",
    "gui.guhs.verhalen.beloning.guhbert": "Guhbert, een lief babyguhtje",
    "gui.guhs.verhalen.beloning.maag": "Je eigen guhmaag (en de Tandarts-guh)",
    "gui.guhs.verhalen.beloning.knuffelburgemeester": "De titel \"Knuffelburgemeester\"",
    "gui.guhs.verhalen.beloning.enderguh": "Een tamme Vahoege Enderguh",
    "gui.guhs.verhalen.beloning.ridder": "Geridderd door de Koningguh",
    "gui.guhs.verhalen.beloning.grillwinkel": "De winkel van de Grillguh gaat open",
    "gui.guhs.verhalen.mewtwo.alle_notities": "nergens meer: je hebt ze alle zes",
    "gui.guhs.verhalen.knusfeest.alles_bij_je": "nergens meer: je hebt alles bij je",
}

# id: (naam, uitleg, [stap-namen], {sleutel: nu}, {sleutel: waar})
VERHALEN = {
    "timmerguh": (
        "Samen een huisje bouwen",
        "De Timmerguh heeft een half guhhuisje gebouwd. Help hem met het dak en krijg zijn bouwboekje!",
        ["Maak kennis met de Timmerguh", "Planken en roze wol brengen", "Het oortjesdak leggen",
         "Een guh in je huisje", "Extra: het huisje knus maken"],
        {"0": "Praat met de Timmerguh en zeg dat je wilt helpen.",
         "1": "Breng de Timmerguh 16 planken (van elk hout) en 8 roze wol. Daar maakt hij dakpluisjes van.",
         "2": "Klim op de steiger en leg met de dakpluisjes elk spookplekje van het oortjesdak vol. Pluf!",
         "3": "Zet je kleine Guhhuisje neer bij je thuis, rechtsklik het en kies Nieuwe bewoner. Daarna vertel je het de Timmerguh.",
         "3_bewoner": "Er woont al een guh in je huisje, vahoeg! Ga het snel aan de Timmerguh vertellen.",
         "4": "Extra klusje: zet een speeltje en een guhlampje bij een van je Guhhuisjes (in het thuisgebied) en vertel het de Timmerguh.",
         "4_knus": "Je huisje is knus ingericht! Vertel het de Timmerguh voor je gereedschapsriem.",
         "klaar": "Het huisje is af en superknus. Met het bouwboekje timmer je nu zelf alle drie de Guhhuisjes!"},
        {"0": "Praat met de Timmerguh op de bouwplaats aan de rand van Knuffeldal (bioom Knuffeldal)",
         "1": "Breng het naar de Timmerguh op de bouwplaats in Knuffeldal",
         "2": "Het dak van de bouwplaats in Knuffeldal (de steiger op!)",
         "3": "Je eigen thuis, en dan de Timmerguh in Knuffeldal",
         "3_bewoner": "Praat met de Timmerguh op de bouwplaats in Knuffeldal",
         "4": "Bij je Guhhuisje thuis, en dan de Timmerguh in Knuffeldal",
         "4_knus": "Praat met de Timmerguh op de bouwplaats in Knuffeldal",
         "klaar": "Zelf timmeren aan de werkbank, met het bouwboekje"}),
    "balto": (
        "Baltoguh en de medicijnkist",
        "De guhbaby's van Nomguh zijn ziek. Alleen Baltoguh durft door de sneeuwstorm naar de berghut...",
        ["Maak kennis met Baltoguh", "Bezoek Rosy in het ziekenhuisje", "Vraag Boris om raad", "Op naar de berghut!",
         "De medicijnkist halen", "Terug door de storm", "Het medicijn naar Rosy"],
        {"0": "Praat met Baltoguh, het wolf-guhtje bij de oude boot. Niet luisteren naar Steele-Mika!",
         "1": "Ga naar Rosy in het ziekenhuisje: de guhbaby's zijn ziek. Hatsjoe-njeg!",
         "2": "Vraag Boris de gans om raad. Gak!",
         "3": "Praat met Baltoguh en kies \"Op naar de berghut!\": dan begint de sledetocht.",
         "4": "Stuur de slee over de route door de sneeuwstorm naar de berghut en pak de medicijnkist.",
         "5": "Snel terug naar Nomguh voor de tijd op is! Op het dieptepunt huil je mee met Baltoguh.",
         "6": "Breng de medicijnkist naar Rosy: de guhbaby's worden beter!",
         "klaar": "Je bent de Held van Nomguh! Race nu ook de sledesprint tegen Steele-Mika.",
         "klaar_tem": "Je bent de Held van Nomguh! Baltoguh wil met je mee: praat met hem bij de oude boot."},
        {"0": "Baltoguh bij de oude boot in de bevroren baai van Nomguh (Sneeuwguhtoendra)",
         "1": "Praat met Rosy in het ziekenhuisje van Nomguh (het witte huis met het roze hart)",
         "2": "Praat met Boris op de oude boot in de bevroren baai van Nomguh (Sneeuwguhtoendra)",
         "3": "Praat met Baltoguh bij de oude boot in Nomguh",
         "4": "De berghut op de Nomguhpieken (de slee rijdt vanzelf de route)",
         "5": "Het ziekenhuisje in Nomguh",
         "6": "Praat met Rosy in het ziekenhuisje van Nomguh",
         "klaar": "Steele-Mika bij de stal in Nomguh (sledesprint)",
         "klaar_tem": "Baltoguh bij de oude boot in Nomguh (Sneeuwguhtoendra)"}),
    "mewtwo": (
        "Het kloon-eiland",
        "Professor Knabbelkloon is zijn labnotities kwijt en de kloontank is kapot. Wie zwemt daar in de tank?",
        ["Beloof de professor te helpen", "Zes labnotities zoeken", "De kloontank repareren", "Een dubbele portie"],
        {"0": "Praat met Professor Knabbelkloon en beloof zijn zes labnotities te zoeken.",
         "1": "Zoek de labnotities op het eiland (kijk eens %s) en breng ze naar de professor.",
         "2": "Zoek de vier tankonderdelen in de onderdelenkisten (kijk eens %s) en bouw ze in bij de kloontank.",
         "2_inbouwen": "Je hebt alle onderdelen gevonden! Bouw ze in bij de kloontank in de koepelhal. Blub!",
         "3": "Vul de grote knabbelschaal in de arena: 32 kaasknabbels en 2 lekkere snacks. Dubbel lekker!",
         "klaar": "Guhtwo en Mieuwguh zijn beste vriendjes. Het kloon-eiland is gered, vahoeg!",
         "klaar_tem": "Het verhaal is af! Guhtwo wacht boven in de arena: hij wil met je mee."},
        {"0": "Professor Knabbelkloon in zijn kantoortje op het kloon-eiland (Diepe Guhzee)",
         "1": "Overal op het kloon-eiland; daarna het kantoortje van de professor",
         "2": "De onderdelenkisten op het eiland, en de kloontank in de koepelhal",
         "2_inbouwen": "De kloontank in de koepelhal van het kloon-eiland",
         "3": "De knabbelschaal in de arena boven op de toren van het kloon-eiland",
         "klaar": "Zeg eens hoi tegen Mieuwguh op het kloon-eiland (Diepe Guhzee)",
         "klaar_tem": "Guhtwo in de arena boven op het kloon-eiland"}),
    "hemel": (
        "Het Knuffelhart",
        "In het Hemelkapelletje slaapt het Knuffelhart. Maak het wakker, dan haal je guhs terug uit de wolkjes.",
        ["Praat met de wolkenhoeder", "Drie dingen voor het Knuffelhart"],
        {"0": "Zoek het Hemelkapelletje en praat met de wolkenhoeder.",
         "1": "Breng de wolkenhoeder een guhkristal, een gouden kaasknabbel en een pluisveertje (mag ook een voor een).",
         "klaar": "Het Knuffelhart klopt weer! Klik erop om een guh uit de wolkjes terug te halen."},
        {"0": "De wolkenhoeder in het Hemelkapelletje, een wolkentempeltje op een zwevend eilandje (Guhvelden, Guhweides...)",
         "1": "Praat met de wolkenhoeder in het Hemelkapelletje",
         "klaar": "Het Knuffelhart in elk Hemelkapelletje"}),
    "guhwaii": (
        "Ohana op Guhwai'i",
        "Er viel een ster op de heuvel van Guhwai'i... en daarin zat een heel stout blauw guhtje: 626-guh!",
        ["Aloha, Lilo-guh!", "De vallende ster zoeken", "Vertel het aan Lilo-guh", "Opruimen met Nani-guh",
         "Iets liefs voor 626-guh", "Ohana!"],
        {"0": "Praat met Lilo-guh in het paalhuisje op het strand.",
         "1": "Zoek de vallende ster boven op de heuvel van het eiland: de neergestorte capsule.",
         "2": "Je hebt de capsule gevonden! Vertel het aan Lilo-guh.",
         "3": "Help Nani-guh opruimen: klik op de rommeltjes van 626 in en om het paalhuisje.",
         "4": "Geef 626-guh iets liefs: een kokosnoot, een roze hibiscus en 8 kaasknabbels (rechtsklik hem ermee).",
         "5": "626-guh is lief geworden! Ga terug naar Lilo-guh.",
         "klaar": "Ohana betekent familie. En familie laat je nooit in de steek, njeg!",
         "klaar_tem": "Het verhaal is af! Geef 626-guh bij het asiel een kaasknabbel: dan gaat hij met je mee."},
        {"0": "Lilo-guh in het paalhuisje van Lilo en Nani (Guhwai'i)",
         "1": "De neergestorte capsule op de heuvel van Guhwai'i (superkompas: tab Verhalen)",
         "2": "Praat met Lilo-guh in het paalhuisje op Guhwai'i",
         "3": "In en om het paalhuisje van Lilo en Nani (Guhwai'i)",
         "4": "626-guh buiten bij het guh-asiel naast het paalhuisje",
         "5": "Praat met Lilo-guh in het paalhuisje op Guhwai'i",
         "klaar": "Surfen en hula dansen op het surfstrand van Guhwai'i",
         "klaar_tem": "626-guh bij het guh-asiel naast het paalhuisje (Guhwai'i)"}),
    "vadsig": (
        "De ontvoerde guh",
        "Moeder Vadsig mist haar babyguhtje Guhbert. De Mika's hebben hem meegenomen!",
        ["Praat met Moeder Vadsig", "Bevrijd Guhbert uit het Mika-kamp", "Zoek de verloren guhtaart", "Een feestje voor Moeder Vadsig"],
        {"0": "Praat met Moeder Vadsig in haar heiligdom.",
         "1": "Win 3 keer op rij steen-papier-schaar van Mika-baas. Psst: VADS wint altijd!",
         "2": "Zoek de verloren guhtaart bij een Guh-picknick.",
         "3": "Breng Moeder Vadsig de verloren guhtaart en 3 guh-ballonnen.",
         "klaar": "Moeder Vadsig heeft je opgeslokt, en nu heb je je eigen guhmaag! De Tandarts-guh maakt hem groter."},
        {"0": "Moeder Vadsig in het Vadsig-heiligdom (Guhmensie)",
         "1": "Mika-baas in het Mika-kamp (volg je Mika-spoorkompas)",
         "2": "Een Guh-picknick in de Guhmensie",
         "3": "Praat met Moeder Vadsig in het Vadsig-heiligdom",
         "klaar": "De Tandarts-guh in je guhmaag"}),
    "slee": (
        "De kapotte slee",
        "De oude Slee-guh wil zo graag nog een keer sleetje rijden, maar zijn slee is kapot.",
        ["Praat met de Slee-guh", "Onderdelen voor de slee"],
        {"0": "Praat met de Slee-guh in zijn sleehut.",
         "1": "Breng een sleeglijder (guhgrotten), een guh-belletje (guhdorpen) en een roze lint (kleermaker).",
         "klaar": "De slee is gemaakt en je kreeg je eigen Guhslee en het sleebouwersboek. Sjoef!"},
        {"0": "De Slee-guh in de sleehut (Guhpieken)",
         "1": "Praat met de Slee-guh in de sleehut (Guhpieken)",
         "klaar": "Sleetje rijden, overal waar sneeuw ligt"}),
    "knusfeest": (
        "Het Grote Knusfeest",
        "Burgemeester Vadsema wil het knusste feest ooit geven. Help jij met de zes feesttaakjes?",
        ["Praat met de burgemeester", "Zes feesttaakjes", "Het feest vieren"],
        {"0": "Praat met Burgemeester Vadsema op de trappen van het stadhuis: je krijgt een knusfeestlijstje.",
         "1": "Maak alle feesttaakjes van je lijstje en breng ze naar de burgemeester. Volgende: %s. Pas op voor Kruimel-Mika!",
         "2": "Alles is gebracht! Praat met de burgemeester: het feest begint!",
         "klaar": "Wat een knus feest! Jij bent nu Knuffelburgemeester. Elk seizoen is er weer een seizoensfeest."},
        {"0": "Burgemeester Vadsema bij het stadhuis van Knuffeldal (bioom Knuffeldal)",
         "1": "De spelletjes van Knuffeldal, daarna Burgemeester Vadsema bij het stadhuis",
         "2": "Burgemeester Vadsema bij het stadhuis van Knuffeldal",
         "klaar": "Burgemeester Vadsema, voor een seizoensfeest"}),
    "guheinde": (
        "Het Guheinde",
        "Opper-Mika heeft de Hongerige Enderguh gevangen in het Guheinde. De Koningguh vraagt jouw hulp!",
        ["De opdracht van de Koningguh", "Een Mika-traan", "Een Oog van Vadsig", "De Knabbelkelder vinden",
         "Twaalf ogen voor het portaal", "Het Guheinde in", "Versla Opper-Mika", "Ridder van de Koningguh"],
        {"0": "Word guhvriend bij de poortwachters (zitten of \"njeg\" zeggen) en praat met de Koningguh op zijn troon.",
         "1": "Zoek een Mika-traan: Mika's laten er soms een vallen (of win van Mika-baas).",
         "2": "Maak een Oog van Vadsig (guhkristal + kaasknabbels + Mika-traan) en gooi het in de Guhmensie.",
         "3": "Volg de ogen naar de Knabbelkelder, diep onder de grond.",
         "4": "Vul alle twaalf portaalframes met een Oog van Vadsig: het portaal gaat open!",
         "5": "Spring door het portaal naar het Guheinde.",
         "6": "Sla de kristallen stuk, voer de Hongerige Enderguh en versla Opper-Mika!",
         "7": "Vertel de Koningguh dat je gewonnen hebt: hij slaat je tot ridder.",
         "klaar": "Je bent Ridder van de Koningguh! Opper-Mika mag je altijd nog eens verslaan."},
        {"0": "De Koningguh in het Guhkasteel (heel ver weg in de Guhmensie)",
         "1": "Mika's in de Guhmensie, of Mika-baas in het Mika-kamp",
         "2": "Aan de werkbank, en daarna gooien in de Guhmensie",
         "3": "De Knabbelkelder, ondergronds in de Guhmensie",
         "4": "Het portaal in de Knabbelkelder",
         "5": "Het portaal in de Knabbelkelder",
         "6": "Het Guheinde",
         "7": "Praat met de Koningguh in het Guhkasteel",
         "klaar": "Het Guheinde, via de Knabbelkelder"}),
    "grillguh": (
        "De Grillguh helpt",
        "De Grillguh droomt van de Barbecuether. Maak samen een grillkoolportaal en steek het aan!",
        ["Praat met de Grillguh", "Een grillkoolframe bouwen", "Een aanmaakblokje halen", "Het portaal aansteken"],
        {"0": "Praat met de Grillguh bij de grote barbecueput.",
         "1": "Maak grillkool (kaassaus over een blok steenkool) en bouw een grillkoolframe bij de Grillguh. Praat dan weer met hem.",
         "2": "Haal een aanmaakblokje: de Mika's in een Mika-kamp laten er een vallen.",
         "3": "Steek het grillkoolframe aan met het aanmaakblokje. Wroemf!",
         "klaar": "Het portaal naar de Barbecuether brandt! De Grillguh gaf je zijn geheime recept, en zijn winkel is open."},
        {"0": "De Grillguh bij de grote barbecueput (Guhmensie of Barbecuether)",
         "1": "Bij de barbecueput van de Grillguh",
         "2": "Een Mika-kamp in de Guhmensie",
         "3": "Het grillkoolframe bij de Grillguh",
         "klaar": "De Grillguh en de Barbecuether"}),
    "beroep_brandweer": (
        "Beroep: brandweer",
        "Help Brandweercommandant Blusguh: de marshmallowvuren laaien op en er zit een boomguhtje vast!",
        ["Praat met Blusguh", "Blus alle vuren", "Red het boomguhtje"],
        {"0": "Praat met Brandweercommandant Blusguh: je krijgt een guh-brandslang.",
         "1": "Blus alle marshmallowvuren met de guh-brandslang. Pssssj!",
         "2": "Red het boomguhtje uit de boom (rechtsklik het).",
         "klaar": "Held van de brandweer! Je guhs dragen nu een brandweerhelm en -jasje."},
        {"0": "De brandweerkazerne in de Beroepenstraat van Knuffeldal",
         "1": "Rond de brandweerkazerne in Knuffeldal",
         "2": "De boom bij de brandweerkazerne in Knuffeldal",
         "klaar": "De Beroepenstraat van Knuffeldal"}),
    "beroep_politie": (
        "Beroep: politie",
        "Inspecteur Vahoegsma zoekt de Knabbeldief. Volg de pootafdrukjes!",
        ["Praat met Vahoegsma", "Volg de pootafdrukken", "Vertel het de inspecteur"],
        {"0": "Praat met Inspecteur Vahoegsma in het politiebureautje.",
         "1": "Volg de pootafdrukjes naar de knabbelbuit en rechtsklik de zak.",
         "2": "Gevonden! Vertel het aan Inspecteur Vahoegsma.",
         "klaar": "Zaak opgelost! Je guhs dragen nu een politiepetje en -uniform."},
        {"0": "Het politiebureautje in de Beroepenstraat van Knuffeldal",
         "1": "Het spoor vanaf het politiebureautje in Knuffeldal",
         "2": "Inspecteur Vahoegsma in het politiebureautje in Knuffeldal",
         "klaar": "De Beroepenstraat van Knuffeldal"}),
    "beroep_apotheek": (
        "Beroep: apotheek",
        "Snotje is verkouden! Dokter Snotneus-guh heeft een kaasmelkdrankje nodig.",
        ["Praat met de dokter", "Maak een kaasmelkdrankje", "Vertel het de dokter"],
        {"0": "Praat met Dokter Snotneus-guh in het apotheekje: je krijgt kaasmelk.",
         "1": "Pluk 3 snotkruidjes, maak in de mengketel een kaasmelkdrankje en geef het aan Snotje.",
         "2": "Snotje is beter! Vertel het aan Dokter Snotneus-guh.",
         "klaar": "Hatsjoe-weg! Je guhs dragen nu een doktersjas en een stethoscoop."},
        {"0": "Het apotheekje in de Beroepenstraat van Knuffeldal",
         "1": "Rond het apotheekje in Knuffeldal (de mengketel staat binnen)",
         "2": "Dokter Snotneus-guh in het apotheekje in Knuffeldal",
         "klaar": "De Beroepenstraat van Knuffeldal"}),
    "beroep_bouw": (
        "Beroep: bouw",
        "Bob de Guhbouwer bouwt een huisje, maar het dak is nog niet af. Kunnen we het maken? Njeg, dat kunnen we!",
        ["Praat met Bob", "Planken en kaasknabbels brengen", "Leg de dakpannen"],
        {"0": "Praat met Bob de Guhbouwer.",
         "1": "Breng Bob 16 planken (van elk hout) en 8 kaasknabbels.",
         "2": "Leg met de dakpannen elk spookplekje van het dak vol.",
         "klaar": "Het dak is af! Je guhs dragen nu een bouwhelm en een veiligheidsvestje."},
        {"0": "Bob de Guhbouwer in een Guhdorp (Guhmensie)",
         "1": "Praat met Bob de Guhbouwer in het Guhdorp",
         "2": "Het dak bij Bob in het Guhdorp",
         "klaar": "Bob de Guhbouwer in het Guhdorp"}),
}


def build(h):
    for key, text in ALGEMEEN.items():
        h.lang(key, text, text)
    for vid, (naam, uitleg, stappen, nu, waar) in VERHALEN.items():
        base = f"gui.guhs.verhalen.{vid}"
        h.lang(f"{base}.naam", naam, naam)
        h.lang(f"{base}.uitleg", uitleg, uitleg)
        for i, s in enumerate(stappen):
            h.lang(f"{base}.stap.{i}", s, s)
        for k, t in nu.items():
            h.lang(f"{base}.nu.{k}", t, t)
        for k, t in waar.items():
            h.lang(f"{base}.waar.{k}", t, t)
    selfcheck()
    # bbq2: the verhaal engine's own module (verhaal_motor: the registered questlines, cutscenes, narrator cards, the demo).
    # The skeleton lists it in FEATURES; until then it is built from here, so its texts are never missing.
    from features import FEATURES, verhaal_motor
    if "verhaal_motor" not in FEATURES:
        verhaal_motor.build(h)


def java_tabel():
    """STAPPEN and EXTRA from VerhalenVoortgang.java."""
    if not os.path.exists(JAVA):
        return None, None
    src = open(JAVA, encoding="utf-8").read()
    stappen = {k: int(v) for k, v in re.findall(r'entry\("([a-z_]+)", (\d+)\)', src)}
    extra_body = src[src.index("EXTRA = "):src.index("STAPPEN = ")]
    extra = {k: re.findall(r'"([a-z0-9_]+)"', v) for k, v in re.findall(r'"([a-z_]+)", List\.of\(([^)]*)\)', extra_body)}
    return stappen, extra


def selfcheck():
    problems = []
    stappen, extra = java_tabel()
    if stappen is None:
        return
    for vid, n in stappen.items():
        if vid not in VERHALEN:
            problems.append(f"no texts for {vid}")
            continue
        _, _, namen, nu, waar = VERHALEN[vid]
        if len(namen) != n:
            problems.append(f"{vid}: {len(namen)} step names, Java says {n}")
        for s in [str(i) for i in range(n)] + ["klaar"] + extra.get(vid, []):
            if s not in nu or s not in waar:
                problems.append(f"{vid}: no nu/waar for {s}")
    for vid in VERHALEN:
        if vid not in stappen:
            problems.append(f"{vid} is not in VerhalenVoortgang.STAPPEN")
    if problems:
        raise SystemExit("gids_verhalen self-check:\n  " + "\n  ".join(problems))
