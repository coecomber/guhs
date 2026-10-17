"""
einde2 (phase B), slice h2_water: every Dutch text of chapter 2 of the Guhvatar story, "De Noordelijke Waterstam".
Read by einde2_h2_water.py (which writes them and checks them against the Java of the slice); the English overlay is
tools/lang/en/c93_e2_h2_water.json.

Copyright: every line is our own. The show is parodied, never quoted: well-known moments are told again in our own words
and guh-ified (CONTRACT_E2_B 6.3). Guhs are always kind, Mika's only shove, nobody dies (Yue-guh does not die: she gives the
moon its light back and BECOMES the moon spirit; she visits).
"""
Q = "quest.guhs.einde2_h2_water."

# --- the questline (verhaal_motor.verhaallijn): name, explanation, the steps (stapnaam, nu, waar), done --------------------------------
LIJN_NAAM = "Hoofdstuk 2: De Noordelijke Waterstam"
LIJN_UITLEG = ("Aan de rand van het Guheinde drijft een stad van ijs. Daar leer je watersturen van de strengste meester met de "
               "langste snor, ontmoet je de Blauwe Geest, reis je naar de Geestenwereld en maak je een golf zo hoog als een muur.")
STAPPEN = [
    # fix2-h2 (playtest 1): every "nu" text says WHAT to do and WHERE, with a direction and a landmark
    ("Aankomst", "Vlieg met Appa naar de Noordelijke Waterstam. Zit je niet op Appa? Klik op hem en kies de gouden ster.", "De Noordelijke Waterstam"),
    ("Meester Pakguh", "Praat met Meester Pakguh op het trainingsplein: door de poort in de ijsmuur, langs de grote gracht naar het noorden en de grote trap op.",
     "Het trainingsplein (boven de grote trap)"),
    ("De waterzweep", "Raak de vijf doelen aan de oostrand van het trainingsplein met je kracht Waterzweep (houd de krachtentoets ingedrukt en kies hem).",
     "Het trainingsplein"),
    ("De golf", "Maak drie golven met Watergolf in het golfbassin: het verzonken bad met de steiger, net oost van het trainingsplein.",
     "Het golfbassin (oost van het plein)"),
    ("De ijsbrug", "Ga op het eind van de steiger in het golfbassin staan, kijk naar het eilandje en gebruik IJsbrug. Loop dan over de brug naar het eilandje.",
     "Het golfbassin"),
    ("Otter-surfen", "Klim op de ijsmuur west van de poort naar de Glijkansel, klik met een lege hand op een otter-pinguinguh en glijd naar de sneeuwhoop beneden.",
     "De Glijkansel (op de ijsmuur, west van de poort)"),
    ("De gondel", "Vanavond is het Maanfeest. Stap in de gondel in het bekken onder de grote trap en breng met Waterstroom de maanlantaarns naar de "
     "drie lichtende haltes: bij de poort, aan de westgracht en aan de oostgracht.", "De grachten van de benedenstad"),
    ("Prinses Yue-guh", "Prinses Yue-guh heeft je uitgenodigd. Ga helemaal naar boven, naar het paleis in het noorden van de stad, door de grote deur, en "
     "praat met haar in de troonzaal.", "Het paleis (boven in de stad)"),
    ("De haven", "Hoofdman Arnook-guh stuurt je op verkenning: ga naar de haven buiten de poort, aan de oostkant, waar het zwarte schip van Zhao-Mika "
     "ligt, en kijk wat hij van plan is.", "De haven (buiten de poort, oost)"),
    ("De Blauwe Geest", "Sluip van het schip van Zhao-Mika af, terug naar de tonnen op de haven. Blijf uit de vlammetjes-kegels van de wachters; gebukt zien "
     "ze je minder ver.", "De VNS Schroeiplek"),
    ("De maanvis", "Zhao-Mika liep met soldaten naar de geestenoase, waar de maan en de oceaan wonen. Ga er snel heen: helemaal boven, achter het "
     "paleis, door het kleine deurtje in het ijs.", "De geestenoase (achter het paleis)"),
    ("Mediteren", "Ga in de geestenoase bij de vijver staan, houd sluipen ingedrukt en blijf drie tellen stil: dan mediteer je met Aang-guh.",
     "De geestenoase"),
    ("Koh", "Neem in de Geestenwereld het enige pad zonder roze nevel: naar het westen, door het Gefluisterwoud, naar Koh's grot onder de oeroude "
     "boom. Als Koh je aankijkt: niet bewegen, niet springen, nergens op klikken. Weer in je lijf? Mediteer opnieuw bij de vijver in de geestenoase.",
     "Koh's grot (Geestenwereld)"),
    ("De Hemelvijver", "De roze nevel boven het pad naar het oosten is weg: loop terug naar de Meditatiecirkel en volg dat pad naar de Hemelvijver, "
     "waar Tui en La horen te zwemmen. Weer in je lijf? Mediteer opnieuw bij de vijver in de geestenoase.", "De Hemelvijver (Geestenwereld)"),
    ("Tui terughalen", "Tui, de maanvis, zit in een kooi aan boord van het schip van Zhao-Mika. Steel hem terug: ga naar de haven buiten de poort (oost), "
     "sluip het schip op en haal hem uit de kooi in de cel achter in het ruim.", "De VNS Schroeiplek (in de haven)"),
    ("De maan", "Breng de maanvis terug naar het eilandje in de vijver van de geestenoase, achter het paleis.", "De geestenoase"),
    ("De grote golf", "Klim via de trap aan de binnenkant van de ijsmuur naar de Golfkansel, het balkonnetje midden boven de poort, en ga op het lichtje staan.",
     "De Golfkansel (boven de poort)"),
    ("De sneeuwstorm", "Loop door de sneeuwstorm op de gletsjer naar het lichtje en luister naar Appa's brul. Loop je de storm uit, dan haalt Zuka je terug.",
     "De gletsjer"),
]
LIJN_KLAAR = ("Klaar! Je kunt watersturen, njeg. Katara-guh en Sokka-guh reizen met je mee naar Ba Sing Guh.", "Op weg naar Ba Sing Guh")
LIJN_KORT = {
    "0": "Naar de Waterstam", "1": "Praat met Pakguh", "2": "Raak de vijf doelen", "3": "Drie golven in het bassin",
    "4": "Een ijsbrug naar het eilandje", "5": "Otter-surfen van de muur", "6": "Maanlantaarns met de gondel", "7": "Naar Yue-guh",
    "8": "Naar de haven", "9": "Sluip van het schip", "10": "Naar de geestenoase", "11": "Mediteer bij de vijver", "12": "Naar Koh",
    "13": "Naar de Hemelvijver", "14": "Steel de maanvis terug", "15": "Breng Tui naar de vijver", "16": "Naar de Golfkansel",
    "17": "Volg het licht",
}

# --- fix2-h2: the step hints (H2Hints) --------------------------------------------------------------------------------------------------
# HINTS: the chat line of a step (gui.guhs.einde2_h2_water.hint.<naam>, %s = the direction, "in het noorden, een eindje verder"); it says
# WHAT and WHERE with a landmark. KIJK: the look scenes einde2_h2_water_kijk_<naam> (titel, the one run-on caption), the same list as
# H2Hints.KIJK. TITELS: the big title when a part is done (gui.guhs.einde2_h2_water.titel.<naam> + .onder). HALTES: the gondola stops.
HINTS = {
    "pakguh": "Meester Pakguh staat %s, op het trainingsplein: door de poort in de ijsmuur, langs de grote gracht naar het noorden en de grote trap op. "
              "Praat met hem (rechtsklik).",
    "zweep": "De vijf doelen staan %s, op een rij aan de oostrand van het plein. Kies Waterzweep, kijk recht naar een doel (van dichtbij) en gebruik je "
             "kracht. Elk doel telt een keer.",
    "golf": "Het golfbassin ligt %s: het verzonken bad met de steiger, net oost van de doelen. Ga aan de rand of op de steiger staan, kies Watergolf en "
            "maak drie golven.",
    "ijsbrug": "Het eilandje met het paviljoen ligt %s. Loop over de steiger tot het eind, kijk naar het eilandje, kies IJsbrug en gebruik hem. Loop dan "
               "over de brug: hij blijft een minuut staan.",
    "slee": "De Glijkansel is %s, boven op de ijsmuur, west van de poort. Neem de trap aan de binnenkant van de muur omhoog, klik met een lege hand op een "
            "otter-pinguinguh en stuur met kijken (W duwt, S remt). Val je eraf, dan sta je zo weer boven.",
    "gondel": "De gondel ligt %s, in het bekken onder aan de grote trap. Stap in, kies Waterstroom en duw hem waar je kijkt, langs de drie haltes: "
              "daar wachten ze op de maanlantaarns. De volgende halte licht op. Peddelen mag ook.",
    "yue": "De grote deur van het paleis is %s, helemaal boven in de stad. Ga naar binnen: prinses Yue-guh staat in de troonzaal, bij de zetel van haar "
           "vader. Praat met haar.",
    "haven": "De haven is %s: door de poort naar buiten en dan naar het oosten, waar het grote zwarte schip van Zhao-Mika ligt. Loop de haven op.",
    "ontsnap": "Je zit op het schip. De haven is %s: de trap achter de toren op, achter de tonnen langs om de schoorsteen, en de ladder af naar de "
               "steiger. Blijf uit de vlammetjes-kegels van de wachters en sluip.",
    "oase": "De geestenoase is %s: helemaal boven, achter het paleis. Loop om het paleis heen en ga door het kleine deurtje in het ijs.",
    "mediteer": "De vijver is %s. Ga ergens in de oase staan, houd sluipen ingedrukt en blijf drie tellen stil. Boven je hotbar tellen de tellen af.",
    "koh": "Koh's grot is %s: neem het pad zonder roze nevel, naar het westen door het Gefluisterwoud, naar de holle wortels van de oeroude boom, "
           "en loop de Gang der Gezichten in. Als Koh je aankijkt: niet bewegen, niet springen, nergens op klikken. Weer in je lijf? Mediteer opnieuw "
           "bij de vijver in de geestenoase.",
    "hemelvijver": "De Hemelvijver is %s: terug naar de Meditatiecirkel en dan het pad naar het oosten op, waar de roze nevel weg is. Weer in je lijf? "
                   "Mediteer opnieuw bij de vijver in de geestenoase.",
    "vis_halen": "Tui, de maanvis, zit aan boord van het schip van Zhao-Mika: steel hem terug! De haven is %s, buiten de poort aan de oostkant. "
                 "Begin achter de tonnen, klim de ladder aan de zijkant van het schip op, sluip om de schoorsteen en de trap achter de toren af naar "
                 "de cel achter in het ruim. Daar hangt Tui in een kooi.",
    "vis_terug": "Breng Tui snel terug: het eilandje in de vijver van de geestenoase is %s, achter het paleis. Loop er gewoon heen.",
    "golf_kansel": "De Golfkansel is %s: het balkonnetje midden boven de poort in de ijsmuur. Neem de trap aan de binnenkant van de muur omhoog en ga op "
                   "het lichtje staan.",
    "storm": "Het lichtje is %s. Loop er recht op af door de sneeuw en luister naar Appa's brul. Loop je de storm uit, dan haalt Zuka je terug.",
    "geest_balk": "➜ %s: %s",
    "halte_balk": "Volgende halte %s: %s",
}
HALTES = {"1": "bij de poort", "2": "aan de westgracht", "3": "aan de oostgracht"}
KIJK = {
    "pakguh": ("Meester Pakguh", "Meester Pakguh. De langste snor van het Guheinde."),
    "zweep": ("De vijf doelen", "Vijf doelen. Een waterzweep per doel."),
    "golf": ("Het golfbassin", "Het golfbassin. Golven, geen gespetter."),
    "slee": ("De Glijkansel", "De Glijkansel. De otters staan al klaar."),
    "gondel": ("De gondel", "De gondel, in het bekken onder de grote trap. De maanlantaarns liggen klaar."),
    "yue": ("Het paleis", "Het paleis. Yue-guh wacht binnen."),
    "haven": ("De haven", "De haven. Daar ligt het schip van Zhao-Mika."),
    "ontsnap": ("De weg naar buiten", "Daar is de haven. Sluip erheen."),
    "oase": ("De geestenoase", "Het deurtje naar de geestenoase."),
    "mediteer": ("De vijver", "De vijver van de maan en de oceaan."),
    "vis_halen": ("De haven", "De haven. Begin achter de tonnen."),
    "vis_terug": ("De vijver", "Terug naar de vijver. Tui heeft haast."),
    "golf_kansel": ("De Golfkansel", "De Golfkansel. Hier wacht de oceaan op je."),
    "storm": ("Een lichtje", "Daar! Een lichtje in de storm."),
}
TITELS = {
    "zweep": ("Vijf van de vijf!", "Pakguh's snor trilt van trots"),
    "golf": ("Drie golven!", "Golven, geen gespetter"),
    "ijsbrug": ("Over het ijs!", "De lessen van Pakguh zijn klaar"),
    "slee": ("WAAAHOE!", "Otter-surfen gelukt"),
    "gondel": ("Alle lantaarns!", "Het Maanfeest kan beginnen"),
    "ontsnap": ("Ontsnapt!", "Niemand heeft je gezien"),
    "koh": ("Geen enkele rimpel!", "Koh laat je gaan"),
    "vis": ("De maanvis!", "Snel, terug naar de oase"),
}

# --- the narrator card ----------------------------------------------------------------------------------------------------------------
KAART_TITEL = "Hoofdstuk 2: De Noordelijke Waterstam"
KAART_REGELS = [
    "Met de wind in je oortjes vlieg je op Appa's rug door de leegte.",
    "Ver weg, boven een zee die hier niet hoort, drijft een berg van ijs.",
    "Daarop staat een stad van grachten en sneeuw, achter een muur zo hoog als een wolk.",
    "De Noordelijke Waterstam. Hier woont de beste watersturer van het Guheinde.",
    "Hij heeft een snor. Dat blijkt belangrijk.",
]

# --- the scenes: titel, {key: tekst}, {acteur: naam} ------------------------------------------------------------------------------------
SCENES = {
    "aankomst": ("Aankomst bij de Waterstam", {
        "intro": "De Noordelijke Waterstam. Een stad van ijs aan de rand van de wereld. Nou ja: aan de rand van het Guheinde.",
        "halt": "Halt! Wie komt daar aanvliegen op een... zespotige knuffel?",
        "aang": "Ik ben Aang-guh! En dit is de nieuwe Guhvatar. We komen watersturen leren!",
        "open": "De Guhvatar? Hier? ...Doe de poort open! En iemand moet Meester Pakguh wakker maken.",
        "pakguh": "Pakguh is de beste watersturer van de stad. Kom, njeg!",
    }, {"wachter": "Poortwachter-guh"}),
    "pakguh": ("Meester Pakguh", {
        "dus": "Dus jij bent de nieuwe Guhvatar. Mmm. En je wilt watersturen leren.",
        "graag": "Heel graag! Wij allebei!",
        "snor": "Ik leer het alleen aan guhs met een snor. Zo is het altijd geweest en zo blijft het. Kijk naar mijn snor. Dat is een snor.",
        "domste": "Een SNOR? Dat is de domste regel van de hele Waterstam!",
        "oud": "De domste regels zijn vaak de oudste, jongedame.",
        "uitdaging": "Dan daag ik je uit. Hier, op het plein. Win ik, dan train je de Guhvatar. En mij.",
        "zweep": "...Je hebt een sterke zweep, Katara-guh. En een nog sterkere wil.",
        "goed": "Goed dan. Ik train jullie allebei. Snor of geen snor.",
        "kom_op": "Kom op, Guhvatar. Laat hem zien wat een guh zonder snor kan!",
    }, {}),
    "blauwe_geest": ("De Blauwe Geest", {
        "cel": "Het ruim van de VNS Schroeiplek. Je zit vastgebonden aan een paal. Donker. Vochtig. Het ruikt naar houtskool en verloren eer.",
        "wacht": "Een veertje onder de neus. Een geeuw. En de wacht slaapt als een baby-Mika.",
        "wie": "...Wie ben jij?",
        "stil": "De Blauwe Geest zegt niets. Hij legt een vinger op zijn masker: stil.",
        "weg": "Hij maakt je touwen los en wijst naar de trap. Naar buiten. En dan is hij weg, net zo stil als hij kwam.",
        "dank": "...Dank je?",
        "uitleg": "Sluip van het schip. Blijf uit het licht van de wachters. Als je sluipt, zien ze je minder ver.",
    }, {"geest": "De Blauwe Geest"}),
    "maanvis": ("De maanvis", {
        "oase": "De geestenoase: het enige warme plekje van de Waterstam. Hier zwemmen Tui en La, de maan en de oceaan, al duizend jaar rondjes om elkaar.",
        "geschiedenis": "Admiraal Zhao-Mika schrijft geschiedenis! Wie de maan heeft, heeft de Waterstam. Pak die vis!",
        "nee": "Nee! Zonder de maanvis is er geen maan. En zonder maan geen watersturen!",
        "lach": "Hahaha! Nu is de maan van MIJ. ...Ik bedoel: van de Vuurnatie. Maar vooral van mij.",
        "grijs": "Tui is weg. La zwemt nog alleen haar rondjes. En dan trekt de kleur uit alles weg.",
        "koh": "We moeten naar de Geestenwereld. Naar Koh. Hij weet hoe je de maan redt.",
    }, {}),
    "geest_reis": ("De reis naar de Geestenwereld", {
        "zitten": "Ogen dicht. Adem in... adem uit... denk aan niets. Behalve aan knabbels. Nee, ook niet aan knabbels.",
        "gloed": "Dan beginnen jullie tatoeages te gloeien. Eerst zacht blauw. Dan feller. Dan ZO wit dat zelfs de vijver ervan schrikt.",
        "jaaa": "Daar gaan we! VAHOEEEG!",
        "gefeli": "GEFELICIVADS MET JULLIE 10 CARPACCIO STREAK BIJ LA CUBANITA JOOSTVADSJE EN SUUSVADSJE!",
        "stil1": "...",
        "stil2": "...",
        "sorry": "... ummm. sorry voor het storen...",
        "stil3": "...",
        "dimensie": "ik denk... dat we in de verkeerde parallelle dimensie zijn gekomen.....",
        "vadsjes": "... emmmm... Dat zijn niet de vadsjes die we dachten.....",
        "taart": "De taart zakt in.",
        "njeg": "njeg... denk het ook...",
        "fout": "Oh wacht.. mijn fout... we moesten hierheen hihi",
        "lijf": "Jullie lijven blijven stil bij de vijver zitten. Jullie zielen snuffelen verder, njeg. Deze keer naar de goede wereld.",
        "echt": "Deze keer echt. Beloofd. Hihi.",
        "spandoek": "GEFELICIVADS!",
    }, {"iedereen": "Iedereen"}),
    "koh": ("Koh de Gezichtenrover", {
        "grot": "Koh's grot. Honderden gezichten kijken je aan vanaf de wanden. En dan beweegt er iets in het donker.",
        "waarschuw": "Psst! Wat er ook gebeurt: toon GEEN emotie. Niet bewegen, niet springen, nergens op klikken. Anders pakt hij je gezicht.",
        "welkom": "Ahhh... de Guhvatar. Lang geleden. Je vorige gezicht vond ik mooier, njeg.",
        "vraag": "Je wilt iets weten over de maan en de oceaan. Dan moet je eerst iets voor mij doen. Sta stil. Kijk mij aan. En voel... niets.",
        "succes": "Succes. Niet lachen. Ook niet om zijn snor. Hij heeft geen snor. Juist daarom.",
    }, {"koh": "Koh"}),
    "la_woede": ("De woede van La", {
        "vijver": "De Hemelvijver. Tui is weg. La zwemt alleen... en La is BOOS.",
        "visioen": "Een visioen: een reusachtige oceaangeest, rood van woede, die de hele vloot van Zhao-Mika opslokt. En de Waterstam erbij.",
        "maan": "Zonder de maan raakt de oceaan uit balans. We moeten Tui terugbrengen. Snel, terug naar ons lijf!",
    }, {"la": "La"}),
    "yue_maan": ("Yue-guh wordt de maan", {
        "terug": "Je legt de maanvis terug in de vijver. Maar Tui beweegt nauwelijks. Hij is te zwak. De maan aan de hemel blijft grijs.",
        "nee": "Nee... hij leeft nog, maar hij is zo zwak...",
        "ik": "Het is niet te laat. Toen ik klein was, gaf de maan mij een stukje van haar licht. Ik heb het al die tijd bij me gedragen.",
        "niet": "Yue... wat ga je doen? Je gaat toch niets doms doen? Dat is MIJN taak.",
        "geven": "Ik geef het terug. Ik word niet minder, Sokka-guh. Ik word... meer. Ik word de maan. En ik kijk elke nacht naar je.",
        "beloof": "Beloof je dat? ...Elke nacht?",
        "belofte": "Elke nacht. Ook als het bewolkt is. Dan kijk ik gewoon harder.",
        "licht": "Een zacht licht schiet omhoog, door het gat in de gletsjer, recht naar de hemel. De maan gloeit weer. Alle kleur stroomt terug in de wereld.",
        "dank": "Ik ben er nog. Kijk maar omhoog. Ik ben er altijd, Guhvatar.",
        "dankje": "Dank je, Yue-guh.",
        "zwaai": "...Ik zwaai straks naar je. Niet lachen. Iedereen gaat vanaf nu naar de maan zwaaien.",
        "maan": "Boven de Noordelijke Waterstam schijnt de maan helderder dan ooit. Een tikje roze. Met twee kleine oortjes, als je goed kijkt.",
        "nu": "Tui leeft. Maar de vloot van Zhao-Mika ligt nog voor de muur. Kom. De oceaan wacht op ons.",
    }, {"yue_geest": "Yue-guh, de maangeest"}),
    "golf": ("De grote golf", {
        "vloot": "De vloot van Zhao-Mika ligt voor de ijsmuur. Tientallen kanonnen. Honderden Mika's. En een Guhvatar op een richeltje.",
        "kun": "Kun je dit? De oceaan is groot. Echt heel groot.",
        "samen": "Je doet het niet alleen. La helpt je. Laat de oceaan door je heen stromen... en vergeet niet: zachtjes. Niemand mag pijn krijgen.",
        "gloed": "Je ogen gloeien. Je voelt de oceaan. Hij is boos, maar hij luistert naar jou.",
        "la": "Uit de zee rijst een reusachtige gloeiende koi op. La, de oceaangeest. Ze kijkt naar jou. Ze wacht.",
        "zachtjes": "...Samen dan. ZACHTJES!",
        "golf": "Een golf zo hoog als de ijsmuur rolt de zee op. Hij tilt de schepen op, heel voorzichtig, en draagt ze weg. Ver weg. De leegte in.",
        "nee": "Nee! Mijn vloot! Mijn GESCHIEDENIS! ...Is dit een ton? Ik zit op een ton.",
        "niemand": "Niemand is gewond. De Mika's dobberen op wrakhout en tonnen naar huis. Een paar zwaaien zelfs. Zachtjes, dus.",
        "jij": "Je hebt het gedaan! Je hebt de oceaan gestuurd! Nou ja, de oceaan heeft jou gestuurd. Samen!",
        "trots": "Roku-guh zou trots op je zijn. Ik ben het ook. Heel erg.",
        "rust": "De zee wordt stil. De maan schijnt. En voor het eerst in dagen kun je even ademhalen...",
        "maar": "...Maar iemand anders heeft ook gewacht. Achter je, in de schaduw van de toren.",
    }, {}),
    "sneeuwstorm": ("De sneeuwstorm", {
        "wit": "Alles is wit. Er zit sneeuw in je oortjes, in je neus en in je knabbelzakje. Je ligt midden in een sneeuwstorm.",
        "eindelijk": "Eindelijk, Guhvatar. Je dacht dat je van me af was. Niemand is ooit van mij af.",
        "waar": "...Waar zijn we?",
        "wachten": "Ergens op de gletsjer. We wachten tot de storm gaat liggen. Dan neem ik je mee naar huis. Naar MIJN huis. Dan krijg ik mijn eer terug.",
        "eer": "MIJN EER!",
        "koud": "Zuka doet stoer, maar zijn oortjes trillen. Het is ijskoud. En in de verte, door de witte storm heen... zie je een lichtje.",
        "ga": "Volg het licht. Loop door de storm. Zuka is te koud om je tegen te houden.",
    }, {}),
    "team": ("Katara-guh en Sokka-guh", {
        "daar": "DAAR! Ik zie je! Guhvatar!",
        "gevonden": "Appa heeft je geroken. Niet beledigend bedoeld. Je ruikt naar Zuka. Dat is wel beledigend bedoeld.",
        "zuka": "Zuka ligt daar nog... in de sneeuw. Hij heeft het koud.",
        "niemand_achter": "...Dan nemen we hem mee tot aan de stad, met Appa's deken om. We laten niemand achter in de sneeuw. Ook hem niet.",
        "team": "Katara-guh, Sokka-guh... gaan jullie met ons mee? Naar Ba Sing Guh?",
        "ja": "Natuurlijk. Je hebt een waterleraar nodig. En Sokka heeft... ons nodig.",
        "plan": "Ik heb al een plan! ...Stap een: Appa.",
    }, {}),
    # fix3-h2 (playtest 2, item 1): the story thread - a scene with its reason before every task, the Water Tribe, the threat
    "welkom": ("Katara-guh en Sokka-guh", {
        "intro": "Uit de poort komen twee guhs in dikke blauwe jassen aanlopen. Een met een strenge blik. Een met een boemerang en een hongerige blik.",
        "wie": "Dus jij bent de nieuwe Guhvatar! Ik ben Katara-guh. En dit is mijn broer, Sokka-guh. Welkom in de Noordelijke Waterstam!",
        "krijger": "Hoi. Ik ben de krijger van de familie. En de grappige. En de slimme. Katara is... de rest.",
        "leren": "Wij komen watersturen leren! Een Guhvatar moet alle vier de elementen kunnen. Lucht heb ik al. Nou ja: wij.",
        "maan": "Dan zit je hier goed. Wij sturen het water met de kracht van de maan. Boven in de stad, in de geestenoase, zwemmen de maan en de "
                "oceaan zelf rond: twee geestvisjes, Tui en La.",
        "druk": "Twee visjes die de hele stad beschermen. Geen druk, visjes.",
        "rook": "Maar er is iets. Vanochtend zag een visser zwarte rook boven de zee. De Vuurnatie zoekt de Guhvatar... en ik denk dat ze hierheen komen.",
        "pakguh": "Dus je moet snel leren. De beste leraar is Meester Pakguh, op het trainingsplein boven de grote trap. Alleen heeft hij een hele "
                  "domme regel.",
        "snor": "Het gaat over snorren. Meer zeg ik niet. Ik heb ook geen snor. Nog niet. Hij komt eraan, ik voel het in mijn oortjes.",
    }, {"wachter": "Poortwachter-guh"}),
    "les_zweep": ("De eerste les", {
        "geduld": "Eerste les. Water is geduldig. Het duwt niet, het stroomt. Maar als het moet... dan tikt het.",
        "doelen": "Vijf doelen. Raak ze alle vijf met een waterzweep. Ik kijk. Mijn snor kijkt ook.",
        "waarom": "Weet je waarom we op doelen oefenen? Zodat je straks iemand kunt wegduwen zonder hem pijn te doen. Een zweep van water maakt "
                  "alleen maar nat.",
        "vuurnatie": "Precies. Als de Vuurnatie komt, wil ik geen gewonden. Niet bij ons en niet bij hen. Een goede watersturer wint zonder dat er "
                     "iemand huilt.",
    }, {}),
    "les_golf": ("De tweede les", {
        "slecht": "Niet slecht. Helemaal niet slecht. Voor een guh zonder snor.",
        "zee": "Tweede les. Een stad op het ijs leeft van de zee. Als de zee wild wordt, maakt een watersturer haar weer rustig. Met golven.",
        "bassin": "Daar, in het golfbassin. Drie golven. Mooie, ronde, rustige golven.",
        "dweilt": "Golven, geen gespetter. Wie spettert, dweilt.",
    }, {}),
    "les_ijsbrug": ("De laatste les", {
        "mooier": "Je golven worden mooier dan de mijne. Zeg dat niet tegen Pakguh.",
        "hoorde": "Ik hoorde dat. ...Laatste les voor vandaag. Water kan ook stil staan.",
        "brug": "Als de Vuurnatie een brug van de stad breekt, bevriest een watersturer gewoon een nieuwe. Zie je dat eilandje? Daar wil ik je zien "
                "staan. Met droge poten.",
        "glad": "Een brug van ijs! Hij is toch niet glad? ...Hij is glad, he. Njeg.",
    }, {}),
    "otters": ("Otter-surfen", {
        "voorbij": "Water kan stromen en water kan stil staan. Jij kunt nu allebei. Mijn lessen zijn voor vandaag voorbij.",
        "pinguin": "Morgen werken we aan je evenwicht. Je staat als een pinguin op een ijsschots.",
        "surfen": "Evenwicht? Daar heb ik iets voor! OTTER-SURFEN! Zo leren alle guhkinderen van de Waterstam op het ijs te staan.",
        "waar": "Het is echt waar. Wie zonder omvallen van de ijsmuur glijdt, valt ook in een gevecht niet om. ...Ik doe niet mee. ...Oke, ik doe mee.",
        "muur": "Boven op de ijsmuur, west van de poort, bij de Glijkansel. De otters wachten al. Lege poten mee!",
    }, {}),
    "maanfeest": ("Het Maanfeest", {
        "nog": "WAAAHOE! Nog een keer! ...Ik bedoel: heel volwassen en nuttig was dat.",
        "feest": "Kijk eens omhoog. Vanavond is het volle maan. Dan viert de hele Waterstam het Maanfeest.",
        "maan": "Boven de ijsmuur komt een ronde, zilveren maan op. Ergens in de stad begint iemand al te zingen. Vals, maar met heel veel liefde.",
        "lantaarns": "Prinses Yue-guh heeft de Guhvatar uitgenodigd in het paleis. Maar eerst moeten de maanlantaarns naar de drie haltes langs de "
                     "grachten. Dat doen watersturers zelf: met een gondel, geduwd met water.",
        "yue": "Yue-guh is heel... aardig. Om mee te praten. Over boemerangs. Ik ga mee. Voor het paleis. Vooral de muren.",
        "gondel": "Een gondel duwen met watersturen? Dat wordt mijn lievelingsles!",
    }, {}),
    "paleis": ("In het paleis", {
        "welkom": "Welkom, Guhvatar. Ik heb veel over je gehoord. Vooral van Sokka-guh. Hij praat heel veel.",
        "nauwelijks": "Ik? Veel praten? Nee hoor. Nauwelijks. Bijna nooit. Mooi paleis trouwens. Mooie... muren.",
        "licht": "Weet je waarom mijn haar wit is? Toen ik klein was, was ik heel ziek. Mijn vader bracht me naar de oase, en de maangeest gaf me een "
                 "stukje van haar licht. Sindsdien voel ik de maan.",
        "onrustig": "En vandaag is ze onrustig. Onze verkenners zagen rook boven de zee.",
        "kampvuur": "Rook? Zoals van een kampvuur? Een gezellig, klein, onschuldig kampvuur?",
        "vloot": "Zoals van een vloot. De Vuurnatie komt eraan. Admiraal Zhao-Mika ligt al met zijn schip in de haven.",
        "post": "Iedereen naar zijn post. Guhvatar: ga naar de haven en kijk wat Zhao-Mika van plan is. Hij noemt zichzelf een schrijver van "
                "geschiedenis. Dat bevalt me niets.",
        "voorzichtig": "Wees voorzichtig. Een admiraal die midden in het Maanfeest aanmeert, heeft iets groots in gedachten.",
    }, {}),
    "kade": ("Op de kade", {
        "daar": "Daar ben je! We zochten je overal! Je was opeens weg. Wie heeft je bevrijd?",
        "masker": "Iemand met een blauw masker. Hij zei niks. Hij deed alleen... stil.",
        "typisch": "Een geheimzinnige redder met een masker. Typisch. Ik wil ook een masker.",
        "zhao": "Luister. Zhao-Mika is niet meer op zijn schip. Hij liep met soldaten de stad in, de grote trap op. Langs het paleis. Erachter.",
        "oase": "Achter het paleis ligt de geestenoase. De maan en de oceaan! Ik voel het tot in mijn oortjes: er gaat iets mis. Snel!",
    }, {}),
    "vis_aan_boord": ("De maanvis aan boord", {
        "zak": "Psst! Hier, achter de tonnen. Ik heb ze gezien: twee soldaten met een natte zak. Die zak zwom.",
        "kooi": "Tui, de maanvis. Zhao-Mika heeft hem aan boord van zijn schip gebracht, de VNS Schroeiplek. In een kooi, achter in het ruim.",
        "la": "Zonder Tui geen maan. En zonder maan wordt La steeds bozer. We zagen het in de Geestenwereld.",
        "plan": "Dan stelen we hem terug. Ik lok de wachters op het dek weg. Jij sluipt aan boord: de ladder op, om de schoorsteen, de trap af naar de cel.",
        "opnieuw": "Blijf uit hun vlammetjes. En als ze je zien: gewoon opnieuw. Zo doe ik het ook altijd.",
    }, {}),
    "geest_aankomst": ("Roze nevel", {
        "echt": "We zijn er! De echte Geestenwereld deze keer. Kijk niet te lang naar de lucht, die doet raar.",
        "nevel": "Over drie van de paden hangt een dikke roze nevel: naar het noorden, naar het oosten en naar het zuidoosten. Alleen het pad naar "
                 "het westen ligt open.",
        "bazig": "Geestnevel. De geesten laten ons alleen door waar we nu moeten zijn. Dat is lief van ze. En een beetje bazig.",
        "koh": "Koh woont onder de oeroude zwarte boom, voorbij het Gefluisterwoud. Hij weet alles van de maan. Maar hij pakt je gezicht als je ook "
               "maar een spiertje vertrekt. Kom!",
    }, {}),
    "koh_raad": ("De raad van Koh", {
        "rimpel": "Mmmm. Geen enkele rimpel. Jammer. Je hebt een mooi gezicht.",
        "evenwicht": "De maan en de oceaan houden elkaar vast. Duw er een weg, en de ander verliest zich in woede.",
        "vijver": "Ga naar de Hemelvijver, Guhvatar. Kijk wat er gebeurt als het evenwicht verdwijnt.",
        "gedaan": "Je deed het! Je trok helemaal geen gezicht! Ik had bijna zelf gelachen.",
        "nevel": "Ver weg, bij de Meditatiecirkel, lost de roze nevel boven het pad naar het oosten op. De weg naar de Hemelvijver is vrij.",
    }, {"koh": "Koh"}),
    "knipoog_balto": ("Baltoguh bij de Waterstam", {
        "sneeuw": "Eindelijk sneeuw! Hier moet Nomguh zijn...",
        "verkeerd": "Hmm. Weer verkeerd afgevadst.",
        "snor": "Had die wel een snor?",
    }, {"balto": "Baltoguh"}),
    "knipoog_sjokkel": ("Sjokkel op de rand", {
        "sjokkel": "Op de rand van het paleisterras kruipt iemand voorbij. Heel. Langzaam.",
        "bijna": "Sjokkel. Op weg naar de finish. Hij is er bijna. Nou ja... bijna.",
    }, {}),
}

# --- everything else: chat lines of the story, the characters' small talk, items, names --------------------------------------------------
TEKSTEN = {
    # the story in the chat
    Q + "kracht.uitleg": "Nieuwe kracht: %1$s! Houd %2$s ingedrukt en kies hem in je krachtwiel. Tik daarna op %2$s om hem te gebruiken.",
    Q + "zweep.teller": "Raak! %s van de %s doelen.",
    Q + "golf.teller": "Een golf! %s van de %s.",
    Q + "ijsbrug.staat": "Een brug van ijs, alleen voor jou. Snel, hij blijft een minuut staan!",
    Q + "slee.op": "Hou je oortjes vast!",
    Q + "slee.opnieuw": "Bijna! De otter wacht weer boven op je. Nog een keer!",
    Q + "gondel.teller": "Een maanlantaarn bij de halte! %s van de %s.",
    Q + "gondel.klaar": "Alle maanlantaarns hangen! Nu naar het paleis: Yue-guh wacht op ons. Ik ga mee. Om... het paleis te zien.",
    Q + "haven.net": "Er valt een net over je heen. Vuurnatie-Mika's springen achter de tonnen vandaan. Alles wordt donker...",
    Q + "sluip.uitleg": "De wachters zien alles in hun kegel (de vlammetjes). Sluip erlangs: gebukt zien ze je minder ver. Gezien? Dan begin je weer bij je laatste rustpunt.",
    Q + "sluip.gezien.0": "HE! Een guh! Terug jij!",
    Q + "sluip.gezien.1": "Wie loopt daar? ...Terug naar je plek, kleintje!",
    Q + "sluip.gezien.2": "Halt! Hier wordt niet gesnuffeld!",
    Q + "sluip.terug": "Gezien! Je wordt teruggezet. Probeer het nog eens.",
    Q + "ontsnap.klaar": "Je bent van het schip af! Niemand heeft je gezien. Van de Blauwe Geest is geen spoor meer.",
    Q + "ontsnap.masker": "Op de steiger ligt een blauw maskertje. Precies op maat voor een guh. Raar.",
    Q + "maanvis.aang": "Ik voel de maan zwakker worden. Kom zitten bij de vijver. We gaan mediteren.",
    Q + "mediteer.weg": "Je concentratie is weg. Ga weer rustig zitten.",
    Q + "mediteer.bezig": "Mediteren... nog %s tel.",
    Q + "mediteer.hoe": "Houd sluipen ingedrukt en blijf stil staan om te mediteren.",
    Q + "geest.geen_wereld": "De Geestenwereld is er in deze wereld niet. Het verhaal gaat verder zonder.",
    Q + "geest.weer": "Je ziel snuffelt weer verder in de Geestenwereld.",
    Q + "koh.titel": "Toon geen emotie",
    Q + "koh.ondertitel": "Niet bewegen. Niet springen. Nergens op klikken.",
    Q + "koh.gezicht.0": "Koh kijkt je aan met het gezicht van een guh...",
    Q + "koh.gezicht.1": "...nu met het gezicht van een Mika. Hij grijnst.",
    Q + "koh.gezicht.2": "...nu lacht hij. Heel hard. Niet meelachen.",
    Q + "koh.gezicht.3": "...nu kijkt hij heel verdrietig. Niet meehuilen.",
    Q + "koh.gezicht.4": "...weer een guh. Hij snuffelt aan je oortje.",
    Q + "koh.gezicht.5": "...nog heel even...",
    Q + "naam.koh": "Koh",
    Q + "koh.weer": "Ahhh. Daar ben je weer. Laten we het nog eens proberen. Kijk mij aan.",
    Q + "koh.fout.beweeg": "Ah! Je bewoog. Dat was een gevoel. Mag ik dat gezichtje even lenen? Ik geef het straks terug, njeg.",
    Q + "koh.fout.spring": "Een sprongetje! Van schrik, of van plezier? Allebei mooi. Allebei van mij.",
    Q + "koh.fout.klik": "Je hand schoot uit. Wat een mooie zenuwen. Wat een mooi gezicht.",
    Q + "koh.fout": "Koh heeft je gezicht geleend. Het komt vanzelf terug. Je staat weer bij de ingang van zijn grot: loop naar binnen en probeer het nog eens.",
    Q + "la.aang": "Snel! Terug naar ons lijf. We moeten Tui terugbrengen voordat La de hele zee boos maakt.",
    Q + "vis.gepakt": "Je haalt de zak met de maanvis voorzichtig uit de kooi. Tui is slap en grijs. Snel!",
    Q + "vis.katara": "Je hebt hem! Ik houd de wachters nog even bezig. Ga, breng Tui naar de vijver!",
    Q + "yue_maan.klaar": "De maan gloeit weer. De kleuren zijn terug. Maar de vloot ligt nog voor de muur.",
    Q + "storm.gegrepen": "Terwijl iedereen juicht, grijpt iemand je in je nek. Een hete adem. Dan alleen nog sneeuw...",
    Q + "storm.terug": "De sneeuwstorm laat je niet gaan. Je bent weer bij Zuka.",
    Q + "klaar.1": "Hoofdstuk 2 is klaar! Je kunt watersturen. Katara-guh en Sokka-guh reizen voortaan met je mee.",
    Q + "klaar.boemerang": "Hier, neem mijn boemerang. Ik heb er nog een. Hij komt altijd terug. Net als ik. En net als jij.",
    Q + "klaar.2": "Je krijgt een Waterstam-parka voor je guh en Sokka-guh's boemerang. Appa brengt jullie naar Ba Sing Guh.",
    Q + "klaar.geen_vlucht": "Appa weet de weg naar Ba Sing Guh nog niet. Fluit hem later: hij brengt je erheen zodra hij de stad kent.",
    Q + "ijsbrug.hier_niet": "Hier is niets om te bevriezen. Probeer het op het eind van de steiger in het golfbassin.",
    Q + "ijsbrug.kijk": "Kijk naar het eilandje als je de brug bevriest.",
    Q + "stroom.geen_water": "Waterstroom werkt alleen in het water, of in je gondel.",
    Q + "doel.koh": "Koh's grot",
    Q + "doel.hemelvijver": "De Hemelvijver",
    # the characters (E2H2WaterRol)
    Q + "rol.pakguh.0": "Weer een toerist. Ga maar naar de markt, daar verkopen ze plaksnorren.",
    Q + "rol.pakguh.zweep": "Vijf doelen. Een zweep van water. Raak ze allemaal. Snor of geen snor.",
    Q + "rol.pakguh.golf": "Ga bij het golfbassin staan en maak drie golven. Niet spetteren. Golven.",
    Q + "rol.pakguh.ijsbrug": "Water kan ook stil staan. Ga op het eind van de steiger staan, kijk naar het eilandje en bevries de weg ernaartoe.",
    Q + "rol.pakguh.vrij": "Mijn lessen zijn voor vandaag voorbij. Ga maar spelen met die snorloze vrienden van je.",
    Q + "rol.pakguh.beleg": "De Vuurnatie staat voor de muur. Een goede watersturer blijft kalm als het ijs. ...Ik ben heel kalm. Heel. Kalm.",
    Q + "rol.pakguh.klaar": "Je bent een echte watersturer geworden, Guhvatar. Zonder snor. Misschien moet ik mijn regels eens herschrijven.",
    Q + "rol.katara.0": "Wil Pakguh jou ook niks leren? Wacht maar. Ik heb een plan. Het heet: hem uitdagen.",
    Q + "rol.katara.training": "Water stroomt, het duwt niet. Beweeg mee, niet ertegenin. Dat zegt Pakguh. Ik zeg: raak gewoon dat doel.",
    Q + "rol.katara.slee": "Sokka wil otter-surfen. Ik zou het niet doen. ...Ik ga het wel doen.",
    Q + "rol.katara.gondel": "Duw de gondel met watersturen door de grachten. De haltes zijn bij de poort, aan de westgracht en aan de oostgracht.",
    Q + "rol.katara.yue": "Sokka is verliefd. Hij doet heel raar. Nog rarer dan anders.",
    Q + "rol.katara.beleg": "Alles is grijs zonder de maan. Zelfs Sokka's grappen. Al waren die altijd al een beetje grijs.",
    Q + "rol.katara.golf": "Ga naar de Golfkansel boven de poort. Ik voel het: de oceaan wacht op je.",
    Q + "rol.katara.klaar": "Ik ga met je mee, Guhvatar. Iemand moet je leren hoe je je eigen sokken wast. Met water.",
    Q + "rol.sokka.0": "Watersturen is niks voor mij. Ik heb een boemerang. Een boemerang komt altijd terug. Een boemerang laat je nooit in de steek.",
    Q + "rol.sokka.slee": "Otter-surfen! Klim op de ijsmuur, west van de poort. Bovenaan wacht een otter-pinguinguh. Lege poten erop, en GAAN!",
    Q + "rol.sokka.gondel": "Gondelen is net als otter-surfen. Maar dan natter. En langzamer. En zonder otter.",
    Q + "rol.sokka.yue": "Heb je prinses Yue-guh gezien? In het paleis? Zei ze iets over mij? Zeg dat ik grappig ben. En lang.",
    Q + "rol.sokka.beleg": "Ik heb een plan voor de belegering. Stap een: niet gevangen worden. Stap twee: ...daar werk ik nog aan.",
    Q + "rol.sokka.klaar": "Ik ga ook mee! Wie moet er anders de plannen maken? En de grappen. Vooral de grappen.",
    Q + "rol.yue.0": "Welkom in het paleis, Guhvatar. Mijn vader zegt dat de maan hier mooier schijnt dan waar ook. Hij heeft gelijk.",
    Q + "rol.yue.beleg": "Ze zeggen dat ik met de kracht van de maan geboren ben. Ik voel haar nu zwakker worden. Wees voorzichtig.",
    Q + "rol.hoofdman.0": "Welkom in de Noordelijke Waterstam, Guhvatar. Mijn ijs is jouw ijs. Niet uitglijden.",
    Q + "rol.hoofdman.beleg": "De vloot van Zhao-Mika ligt voor de muur. Mijn krijgers houden de poort. Jij... jij houdt de hoop.",
    Q + "rol.hoofdman.na": "Mijn dochter schijnt nu elke nacht over de stad. Ik ben trots op haar. En op jou.",
    Q + "rol.poortwachter.0": "Halt! ...O, jij weer. Loop maar door. Veeg je poten.",
    Q + "rol.poortwachter.klaar": "De Guhvatar! Iedereen heeft het over die golf. Ik stond erbij. Ik werd heel nat.",
    Q + "rol.zhao.0": "Ik ben admiraal Zhao-Mika! Ik ga geschiedenis schrijven! ...Wat kijk je? Hup, weg, guh.",
    Q + "rol.zhao.1": "Heb je mijn schip gezien? Indrukwekkend, he? Raak niks aan. Vooral de knoppen niet.",
    Q + "rol.wacht.0": "Doorlopen. Dit is een schip van de Vuurnatie. Hier wordt niet geknuffeld.",
    Q + "rol.bewoner.0": "IJs, ijs en nog eens ijs. En zeepruimen. Wil je een zeepruim? Niemand wil ooit een zeepruim.",
    Q + "rol.bewoner.1": "Heb je de Golfkansel al gezien, boven de poort? Daar staat nooit iemand op. Te hoog. Te koud. Te episch.",
    Q + "rol.bewoner.beleg": "De Vuurnatie! Alle guhs naar binnen! ...Ik bedoel: alle watersturende guhs naar BUITEN!",
    Q + "rol.bewoner.na": "Die golf! Zag je hem? De hele vloot dreef zachtjes de leegte in. Niemand gewond. Alleen Zhao's trots.",
    Q + "rol.bewoner.na2": "Ik zwaai elke nacht naar de maan. Sokka-guh doet het ook. Hij zwaait heel lang.",
    Q + "rol.storm_zuka.0": "Blijf daar. Als de storm gaat liggen, neem ik je mee. Dan krijg ik mijn eer terug.",
    Q + "rol.storm_zuka.1": "Het is... best koud. Niet dat ik het koud heb. Ik heb het nooit koud. Ik ben van de Vuurnatie.",
    Q + "rol.yue_maan.0": "Ik ben er nog, Guhvatar. Elke nacht. Zeg tegen Sokka dat hij mag ophouden met zwaaien. ...Of eigenlijk: niet ophouden.",
    Q + "rol.yue_maan.1": "De maan en de oceaan houden elkaar in evenwicht. Net als jij, straks, met alle vier de elementen. Op naar Ba Sing Guh.",
    # items, entities, clothes, the powers
    "item.guhs.boemerang": "Boemerang",
    "item.guhs.boemerang.lore": "Van Sokka-guh. Gooi hem: hij komt altijd terug, met alles wat hij onderweg tegenkwam",
    "entity.guhs.einde2_h2_water_boemerang": "Boemerang",
    "entity.guhs.einde2_h2_water_gondel": "Gondel",
    "entity.guhs.einde2_h2_water_slee_otter": "Slee-otter",
    "item.guhs.waterstam_parka": "Waterstam-parka",
    "item.guhs.blauwe_geest_masker": "Blauwe Geest-masker",
    "gui.guhs.kledingbron.einde2_h2_water": "De Guhvatar, hoofdstuk 2: de Noordelijke Waterstam",
    "gui.guhs.einde2_h2_water.kracht.einde2_h2_water_waterzweep": "Waterzweep",
    "gui.guhs.einde2_h2_water.kracht.einde2_h2_water_watergolf": "Watergolf",
    "gui.guhs.einde2_h2_water.kracht.einde2_h2_water_ijsbrug": "IJsbrug",
    "gui.guhs.einde2_h2_water.kracht.einde2_h2_water_waterstroom": "Waterstroom",
    # fix3-h2: the pink spirit mist over the paths of the Geestenwereld that are not for now (Geestnevel)
    "gui.guhs.einde2_h2_water.nevel": "Een roze geestnevel duwt je zachtjes terug. Dit pad is nog niet voor jou, njeg.",
}
