"""
einde2 zij: every Dutch text of the slice (CONTRACT_E2_B 7.6). English: tools/lang/en/c96_e2_zij.json.

  LIJNEN   the eight side lines (verhaal_motor.verhaallijn): name, explanation, steps, done, extra text variants
  SCENES   the five cutscenes (verhaal_motor.scene): title, lines, speaker names
  BOEKEN   the six books of the Bibliotheek van Wan Shi Guh: title and pages
  TEKST    everything else: quest dialogue, messages, items, the block, titles, the key, the powers

All in own words: no line of the series, no line of the poem at the end of another game; the wisdoms are our own.
Texts of a talking screen (Praat) stay under about 170 characters (six lines of the screen).
"""

Q = "quest.guhs.einde2_zij."
G = "gui.guhs.einde2_zij."

# =====================================================================================================================
# the eight side lines
# =====================================================================================================================
LIJNEN = {
    "omashguh": dict(
        naam="De drie proeven van Omashguh",
        uitleg="Koning Bumi-guh van Omashguh is meer dan honderd jaar oud en nog steeds heerlijk gek. Zijn proeven werken anders "
               "dan je denkt.",
        stappen=[
            ("Een gekke oude koning", "Praat met koning Bumi-guh in de troonzaal boven in het paleis van Omashguh.", "Omashguh, de troonzaal"),
            ("De sleutel in de waterval", "Haal de sleutel uit de waterval van glas (proef 1, aan de westkant onder de troonzaal). "
                                          "Klimmen lukt niet: gebruik je aardekracht in de kamer.", "Omashguh, proef 1 (west, onder de troonzaal)"),
            ("Waar is Flopsie?", "Zoek Flopsie, het huisdier van de koning. Het hokje van proef 2 (aan de oostkant onder de troonzaal) "
                                 "is wel erg klein...", "Omashguh, proef 2 (oost, onder de troonzaal)"),
            ("Kies je tegenstander", "Ga naar de arena van proef 3 (in het midden onder de troonzaal), kies je tegenstander en "
                                     "beantwoord de laatste vraag van de koning.", "Omashguh, proef 3 (midden, onder de troonzaal)"),
            ("Sneller dan de post", "Spring op het laadplatform voor het paleis in een postgoot en glij binnen 40 seconden naar het "
                                    "postkantoor.", "Omashguh, de postglijbanen"),
        ],
        klaar=("Bumi-guh lacht nog steeds. Lik eens aan je kristalsnoep-ring.", "Omashguh"),
        extra={"0_element": ("Koning Bumi-guh wacht op iemand die kan aardesturen. Dat leer je in het verhaal van de Guhvatar.", "Omashguh")},
    ),
    "westelijke_luchttempel": dict(
        naam="De stille tempel",
        uitleg="De Westelijke Luchttempel hangt ondersteboven onder een eiland. Vroeger floot de wind door de gangen en woonde er "
               "een kudde luchtbizon-guhs. Nu is het er stil.",
        stappen=[
            ("De luchtnon", "Praat met de luchtnon-guh bij de trap in de hangende hal: de tempel hangt ondersteboven onder het eiland.",
             "Westelijke Luchttempel, de hangende hal"),
            ("Laat de tempel zingen", "Ga op het balkon aan het eind van elk van de vijf windgangen staan en gebruik daar je luchtkracht. "
                                      "Vier gangen lopen van de hal naar de vier kanten, de vijfde is de orgelpijp onderin.",
             "Westelijke Luchttempel, de windgangen"),
            ("De verdwaalde kudde", "Vijf luchtbizon-guhs zweven verdwaald rond het eiland. Vlieg naar ze toe (met Appa of je vleugels) "
                                    "en klik ze, of richt van een afstand je Luchtbal op ze: ze vliegen naar hun eigen hok.",
             "Rond de Westelijke Luchttempel"),
            ("Terug naar de luchtnon", "De kudde is thuis en de tempel zingt. Vertel het de luchtnon-guh.", "Westelijke Luchttempel"),
        ],
        klaar=("De tempel zingt weer en de kudde is thuis.", "Westelijke Luchttempel"),
        extra={"0_element": ("De luchtnon-guh wacht op iemand die kan luchtsturen. Dat leer je in het verhaal van de Guhvatar.",
                             "Westelijke Luchttempel")},
    ),
    "vuurtempel_roku": dict(
        naam="Het vuur van Roku-guh",
        uitleg="Op een halvemaaneiland staat de tempel van Guhvatar Roku-guh. Achter vijf vuurdeuren wacht zijn beeld, en "
               "misschien meer dan dat.",
        stappen=[
            ("De vriendelijke Vuurwijze", "Praat met Shyu, de vriendelijke Vuurwijze bij het luik achter het altaar, op de begane grond "
                                          "van de toren.", "Vuurtempel van Roku-guh, de begane grond"),
            ("Vijf vuurdeuren", "Ga boven in de toren voor elke vuurdeur staan en gebruik je vuurkracht: steek alle vijf de sloten aan.",
             "Vuurtempel, de bovenste verdieping"),
            ("Het heiligdom", "De deuren zijn open, alleen voor jou. Loop recht op een vuurdeur af: je stapt erdoor, het heiligdom in.",
             "Vuurtempel, achter de vuurdeuren boven in de toren"),
            ("Roku-guh", "Praat met de geest van Guhvatar Roku-guh in het heiligdom.", "Vuurtempel, het heiligdom"),
        ],
        klaar=("Roku-guh heeft je zijn verhaal verteld. Het eeuwige vlammetje is van jou.", "Vuurtempel van Roku-guh"),
        extra={"0_element": ("Shyu de Vuurwijze wacht op iemand die kan vuursturen. Dat leer je in het verhaal van de Guhvatar.",
                             "Vuurtempel van Roku-guh")},
    ),
    "kyoshi_dorp": dict(
        naam="De waaiers van Kyoshi",
        uitleg="In het Kyoshi-dorp trainen de Kyoshi-krijgsterguhs met waaiers onder het reuzenbeeld van Guhvatar Kyoshi-guh. En "
               "bij de poort staat haar grootste fan.",
        stappen=[
            ("De leidster", "Praat met de leidster van de Kyoshi-krijgsterguhs in de dojo, voor de wand met waaiers.",
             "Kyoshi-dorp, de dojo aan de oostkant"),
            ("De waaierdans", "Ga op de gouden tegel midden op de dansvloer staan en volg de roep op de maat: BUK (shift), SPRING of "
                              "DRAAI (kijk opzij). Tien goed is genoeg.", "Kyoshi-dorp, de dansvloer in de dojo"),
            ("Geschminkt als een krijgster", "Ga naar de schminktafel onder de spiegel in de zuidwesthoek van de dojo (klik erop, of "
                                             "buk ervoor).", "Kyoshi-dorp, de schminktafel"),
            ("De grootste fan", "Laat je zien aan de schuimbekkende fan-guh bij de poort: klik op hem.", "Kyoshi-dorp, de poort bij de landingsplek"),
        ],
        klaar=("De fan is nog steeds van zijn stokje. Je Kyoshi-schmink staat je guh prachtig.", "Kyoshi-dorp"),
        extra={"0_element": ("De leidster wacht op iemand die kan watersturen. Dat leer je in het verhaal van de Guhvatar.", "Kyoshi-dorp")},
    ),
    "bibliotheek_wan_shi_guh": dict(
        naam="Kennis voor kennis",
        uitleg="Half onder het korstzand ligt de bibliotheek van Wan Shi Guh, de uil die alles weet. Wie er iets leert, brengt er "
               "ook iets mee.",
        stappen=[
            ("De regels van de uil", "Praat met Wan Shi Guh in de uilenzaal, helemaal onderin de bibliotheek. Je komt binnen door het "
                                     "raam in de torenspits en gaat dan steeds verder naar beneden.", "Bibliotheek, de uilenzaal"),
            ("Een stukje kennis", "Breng Wan Shi Guh drie stukjes kennis: een kaart, een kompas, een klok, een verrekijker of een boek. "
                                  "Houd het vast en klik op hem.", "Bibliotheek, de uilenzaal"),
            ("Een boek voor jou", "Een vos-guh brengt het boek dat bij jou past. Zoek er een en klik erop.", "Bibliotheek van Wan Shi Guh"),
            ("Het planetarium", "Draai aan het wiel van het planetarium, bij professor Zei-guh: de zijzaal aan de westkant, op de "
                                "hoogte van zaal 1.", "Bibliotheek, het planetarium"),
        ],
        klaar=("Wan Shi Guh ruilt elke dag een stukje kennis voor een boek.", "Bibliotheek van Wan Shi Guh"),
    ),
    "leeuwschildpad": dict(
        naam="De zegen van de Leeuwschildpad",
        uitleg="Het eiland is geen eiland: het is de oude Leeuwschildpad-guh, die al eeuwen door de leegte zwemt. Hij geeft één "
               "zegen per guh. Of per speler.",
        stappen=[
            ("Over het schild", "Wandel over het schild van het eiland naar de steenkring helemaal bovenop.", "Leeuwschildpad-eiland"),
            ("De opgeheven poot", "Loop langs zijn flank naar beneden, naar de voorkant onder zijn kop, en ga op de handpalm van zijn "
                                  "opgeheven poot staan.", "Leeuwschildpad-eiland, de poot"),
            ("Een knabbel als offer", "Houd een kaasknabbel vast en klik op zijn snuit, terwijl je op zijn handpalm staat.",
             "Leeuwschildpad-eiland, de poot"),
        ],
        klaar=("Je draagt de schub van de Leeuwschildpad. Val je in de leegte, dan vangt hij je op.", "Leeuwschildpad-eiland"),
    ),
    "mika_kok": dict(
        naam="De spijtige Mika-kok",
        uitleg="In de keuken van elke Mika-vesting staat een kok met een slappe muts. Hij kookt stiekem voor de gevangen guhs, en "
               "hij heeft overal spijt van.",
        stappen=[
            ("Een zuchtende kok", "Praat met de Mika-kok in de keuken van een Mika-vesting.", "Een Mika-vesting, de keuken"),
            ("Ingrediënten", "Breng de kok 2 schimmelpaddenstoelen, 2 kristalkaasbrokjes, 2 knabbelkoorvruchten en 1 kruimelwolk "
                             "uit de buiteneilanden.", "De buiteneilanden"),
            ("Leid de kapitein af", "Praat met de vesting-kapitein achter zijn bureau op de begane grond en verzin iets dat werkt.",
             "Een Mika-vesting, de begane grond"),
            ("De geheime soep", "Terug naar de kok in de keuken.", "Een Mika-vesting, de keuken"),
            ("Een eigen keukentje", "Zoek de kok op in zijn nieuwe keuken in de Leegtehaven: De Spijtige Spatel, aan de kade.",
             "Leegtehaven, de kade"),
        ],
        klaar=("De kok heeft een eigen keukentje en jij het recept van de Spijtsoep.", "Leegtehaven, De Spijtige Spatel"),
    ),
    "vetbootje": dict(
        naam="Een eigen vetbootje",
        uitleg="Aan de steiger van elke Mika-vesting hangt een havenbord. Er hangt een briefje op voor wie een eigen schip wil.",
        stappen=[
            ("Het briefje", "Lees het briefje op het havenbord aan de steiger van een Mika-vesting (klik erop).", "Een Mika-vesting, de steiger"),
            ("Naar de werf", "Breng de scheepsbouwer-guh op de scheepswerf aan de westkant van de Leegtehaven: 24 planken, 8 wol, "
                             "4 Mika's vet en 2 flitskruimels. Daarna bouwt hij je bootje.", "Leegtehaven, de scheepswerf (west)"),
            ("Doop je bootje", "Je vetbootje is klaar. Praat met de scheepsbouwer-guh en geef het een kleur, een zeil en een naam.",
             "Leegtehaven, de scheepswerf"),
        ],
        klaar=("Je hebt een eigen vetbootje. Het vetfluitje roept het altijd terug.", "Leegtehaven"),
    ),
}

# =====================================================================================================================
# fix round 1 (fix2-geestzij): the step hints (gui.guhs.einde2_zij.hint.*, %s = the direction) and the look scenes
# =====================================================================================================================
HINTS = {
    "omash.0": "Koning Bumi-guh zit %s, op zijn troon in de troonzaal boven in het paleis van Omashguh.",
    "omash.1": "Proef 1 begint %s: de kamer met de waterval van glas, aan de westkant onder de troonzaal. Ga erin en gebruik daar "
               "je aardekracht (je krachttoets, standaard H).",
    "omash.2": "Proef 2 begint %s, aan de oostkant onder de troonzaal: het hokje van Flopsie. Kijk eens goed hoe groot die "
               "pootafdrukken zijn...",
    "omash.3": "De arena van proef 3 ligt %s, in het midden onder de troonzaal. Loop erin en kies je tegenstander.",
    "omash.4": "Het laadplatform van de postglijbanen ligt %s, voor het paleis. Stap in een postgoot: de klok loopt vanaf het eerste "
               "stukje glijbaan. Te laat? Dan brengt de postlift je terug naar boven.",
    "lucht.0": "De luchtnon-guh staat %s, bij de trap in de hangende hal: de tempel hangt ondersteboven onder het eiland.",
    "lucht.1": "Het balkon van een stille windgang ligt %s (de lichtzuil wijst het aan). Ga op het balkon staan en gebruik daar je "
               "luchtkracht. Vier gangen lopen van de hal naar de vier kanten, de vijfde is de orgelpijp onderin.",
    "lucht.2": "Een verdwaalde luchtbizon-guh zweeft %s (de lichtzuil wijst hem aan). Vlieg erheen en klik hem, of richt van een "
               "afstand je Luchtbal op hem: dan vliegt hij naar zijn hok.",
    "lucht.3": "De kudde is thuis. De luchtnon-guh wacht %s, bij de trap in de hangende hal.",
    "vuur.0": "Shyu staat %s, bij het luik achter het altaar op de begane grond van de toren.",
    "vuur.1": "De vijf vuurdeuren staan %s, op een rij boven in de toren. Ga recht voor een deur staan en gebruik je vuurkracht: het "
              "slot gaat branden. De lichtzuil wijst de volgende deur aan.",
    "vuur.2": "De vuurdeuren staan open, alleen voor jou (%s). Loop recht op een deur af: je stapt erdoor, het heiligdom in.",
    "vuur.3": "De geest van Roku-guh staat %s, voor zijn beeld in het heiligdom. Klik op hem.",
    "kyoshi.0": "De leidster van de Kyoshi-krijgsterguhs staat %s, in de dojo aan de oostkant van het dorp, voor de wand met waaiers.",
    "kyoshi.1": "De gouden tegel ligt %s, midden op de dansvloer in de dojo. Ga erop staan, dan begint de dans vanzelf. Doe wat er "
                "groot in beeld staat: BUK (shift), SPRING of DRAAI (kijk opzij). Tien van de zestien goed is genoeg.",
    "kyoshi.2": "De schminktafel staat %s, onder de spiegel in de zuidwesthoek van de dojo. Klik erop.",
    "kyoshi.3": "De schuimbekkende fan-guh staat %s, naast zijn fanclubkraam bij de poort, vlak bij de landingsplek. Klik op hem.",
    "bieb.0": "Wan Shi Guh zit %s, helemaal onderin de bibliotheek, in de uilenzaal. Je komt binnen door het raam in de torenspits "
              "en gaat dan steeds verder naar beneden.",
    "bieb.1": "Wan Shi Guh zit %s, in de uilenzaal. Houd een kaart, kompas, klok, verrekijker of boek in je hand en klik op hem.",
    "bieb.2": "Een vos-guh met een boek staat %s, naast de verhoging van de uil. Klik op hem.",
    "bieb.3": "Het planetarium ligt %s: de zijzaal aan de westkant, op de hoogte van zaal 1. Klik op de magneetsteen van het wiel, "
              "naast professor Zei-guh.",
    "schildpad.0": "Het hoogste punt van zijn schild ligt %s: loop door het bos op zijn rug naar de steenkring.",
    "schildpad.1": "Zijn opgeheven poot ligt %s, aan de voorkant onder zijn kop. Loop langs zijn flank naar beneden en ga op het "
                   "middelste kussentje van zijn handpalm staan.",
    "schildpad.2": "Zijn snuit is %s. Houd een kaasknabbel in je hand en klik erop, terwijl je op zijn handpalm staat.",
    "kok.0": "De Mika-kok staat %s, bij zijn soepketel in de keuken van de Mika-vesting.",
    "kok.2": "De vesting-kapitein zit %s, achter zijn bureau op de begane grond van de vesting. Praat met hem.",
    "kok.4": "De Spijtige Spatel, het nieuwe keukentje van de kok, ligt %s, aan de kade van de Leegtehaven.",
    "bootje.1": "De scheepsbouwer-guh staat %s, op de scheepswerf aan de westkant van de Leegtehaven, bij de helling.",
}
# (naam: (titel, caption)) -> scene einde2_zij_kijk_<naam> (StapHint.kijkScene, 3.2 s, one run-on caption)
KIJK = {
    "omash_proef1": ("Proef 1: de waterval van glas", "Ga naar binnen en gebruik je aardekracht."),
    "omash_proef2": ("Proef 2: waar is Flopsie?", "Dat hokje is wel erg klein..."),
    "omash_proef3": ("Proef 3: de arena", "Hier kies je je tegenstander."),
    "omash_glijbaan": ("De postglijbanen", "Stap in een postgoot en glij naar beneden!"),
    "lucht_windfluit": ("Een stille windgang", "Ga op het balkon staan en laat de wind zingen."),
    "lucht_kudde": ("Een verdwaalde luchtbizon", "Klik hem, of blaas er met Luchtbal naartoe."),
    "vuur_deuren": ("De vijf vuurdeuren", "Sta voor een deur en gebruik je vuurkracht."),
    "vuur_heiligdom": ("Het heiligdom", "De deuren staan open. Alleen voor jou."),
    "kyoshi_dans": ("De gouden tegel", "Sta hier, en volg de roep op de maat."),
    "kyoshi_schmink": ("De schminktafel", "Onder de spiegel in de dojo."),
    "kyoshi_fan": ("De grootste fan", "Hij wacht al jaren op een echte krijgster."),
    "bieb_vos": ("Een vos-guh", "Hij heeft een boek voor je."),
    "bieb_planetarium": ("Het planetarium", "Draai aan het wiel bij professor Zei-guh."),
    "schildpad_top": ("De steenkring", "Het hoogste punt van zijn schild."),
    "schildpad_poot": ("De opgeheven poot", "Ga op zijn handpalm staan."),
    "schildpad_kop": ("De snuit", "Een kaasknabbel in je hand, en klik."),
    "haven_kok": ("De Spijtige Spatel", "Zijn eigen keukentje, aan de kade."),
    "haven_werf": ("De scheepswerf", "De scheepsbouwer-guh wacht bij de helling."),
}

# =====================================================================================================================
# the five scenes
# =====================================================================================================================
SCENES = {
    "einde2_zij_opper_mika_opkomst": dict(
        titel="De Opper-Mika verschijnt",
        regels={
            "wie": "WIE staat er op MIJN Knabbelberg?!",
            "alles": "Elke knabbel hier is van MIJ. Elk kristal. Elke kruimel!",
            "duw": "Ik duw jullie allemaal terug naar waar je vandaan komt!",
            "titel": "DE OPPER-MIKA - heerser over het Guheinde",
        },
        namen={"opper": "Opper-Mika"},
    ),
    "einde2_zij_einde_gedicht": dict(
        titel="Het gedicht van het Guheinde",
        regels={
            "regel.1": "Kijk. Daar is er weer een.",
            "regel.2": "Een speler? Met knabbels in elke zak?",
            "regel.3": "Die. Die heeft net de Opper-Mika verslagen.",
            "regel.4": "Verslagen? Of heel hard teruggeduwd?",
            "regel.5": "In het Guheinde is dat hetzelfde.",
            "regel.6": "Weet de speler dat wij kijken?",
            "regel.7": "Soms. Als het stil is. Als de guhs slapen en het kampvuur knispert.",
            "regel.8": "Wat zullen we de speler vertellen?",
            "regel.9": "Dat de wereld groter is dan de rand van het scherm.",
            "regel.10": "Dat een knabbel die je deelt, twee knabbels waard is.",
            "regel.11": "Dat je een guh niet bezit. Een guh kiest jou.",
            "regel.12": "En dat het Guheinde geen einde is. Alleen een plek met een mooie naam.",
            "regel.13": "Er komt nog meer. Er komt altijd nog meer.",
            "regel.14": "Sst. De speler wordt wakker.",
            "regel.15": "Word wakker, speler. Er wacht een kaasbol op je.",
            "regel.16": "...en de speler ging vadsen.",
            "credits.1": "GUHS",
            "credits.2": "Met de guhs van de Guhmensie, de Barbecuether en het Guheinde",
            "credits.3": "Met de Mika's (die alleen maar duwen)",
            "credits.4": "Met Team Guhvatar, dat nog op je wacht",
            "credits.5": "Met elke guh die ooit is geaaid",
            "credits.6": "En met jou. Bedankt voor het spelen, njeg.",
        },
        namen={"roze": "De Roze Stem", "goud": "De Gouden Stem"},
    ),
    "einde2_zij_roku_zonnestraal": dict(
        titel="De zonnestraal",
        regels={
            "zon": "Eén keer per dag valt de zon precies door het venster van het heiligdom.",
            "ogen": "De ogen van het beeld beginnen te gloeien.",
            "hallo": "Ik heb op je gewacht, jonge Guhvatar. Heel lang. Ik ben nogal geduldig geworden.",
            "wie": "Ik ben Roku-guh. Vóór Aang-guh was ik de Guhvatar. Dus in zekere zin was ik jij. Verwarrend, njeg.",
            "praten": "Kom dichterbij. Ik wil je iets vertellen over vriendschap, en over een vriend die altijd meer wilde.",
        },
        namen={"roku": "Roku-guh"},
    ),
    "einde2_zij_leeuwschildpad_zegen": dict(
        titel="De zegen",
        regels={
            "gegeven": "Een knabbel. Zomaar gegeven. Weet je hoe zeldzaam dat is, kleintje?",
            "zegen1": "Ik geef je geen kracht. Kracht heb je al genoeg, met je elementen en je vrienden.",
            "zegen2": "Ik geef je iets anders. Een plek om op te vallen.",
            "zegen3": "Val je ooit in de leegte, dan vangt een oude schildpad je op. Zo groot is de leegte niet, njeg.",
        },
        namen={"schildpad": "De Leeuwschildpad-guh"},
    ),
    "einde2_zij_knipoog_mew": dict(
        titel="Knipoog: Mew-guh in de bibliotheek",
        regels={
            "regels": "Regel één: geen Mika's. Regel twee: geen vuur. Regel drie: alles blijft netjes op zijn plank.",
            "niks": "...Hm. Niks te zien.",
        },
        namen={"uil": "Wan Shi Guh"},
    ),
}

# =====================================================================================================================
# the six books of the library (pages: at most ~250 characters, a book page holds about that)
# =====================================================================================================================
BOEKEN = {
    "vier_volkeren": ("Over de vier volkeren", [
        "Lang geleden woonden er in het Guheinde vier volkeren. Ze deelden de wind, het water, de grond en het vuur. En de "
        "knabbels. Vooral de knabbels.",
        "De luchtnomaden-guhs leefden in tempels die zweefden. Ze aten weinig, lachten veel en lieten elke deur open voor wie "
        "langskwam.",
        "De waterstam-guhs woonden in het ijs en de schimmelkaas. Ze konden met water praten. Het water praatte niet terug, "
        "maar luisterde wel.",
        "De aarde-guhs bouwden steden met muren, en de vuur-guhs bouwden smidsen. Allebei vonden ze dat de ander te hard "
        "bouwde. Toen kwam er een Guhvatar.",
    ]),
    "zon_ging_vadsen": ("De dag dat de zon even ging vadsen", [
        "Professor Zei-guh noteerde: soms schuift de maan voor de zon. Dan wordt het midden op de dag even donker.",
        "Wie op zo'n dag vuurstuurt, merkt dat zijn vuur ook even gaat liggen. Alsof het een dutje doet. Dit is geen geheim. "
        "Het is gewoon sterrenkunde.",
        "Opmerking in de kantlijn, in een ander handschrift: 'Wie dit leest: zorg dat je die dag niet net iets belangrijks "
        "moet doen, njeg.'",
    ]),
    "luchtbizons": ("De luchtbizon-guh: een veldgids", [
        "De luchtbizon-guh heeft zes poten, een platte staart en de lieve oren van een guh. Hij vliegt zonder vleugels. "
        "Niemand weet hoe. Hij ook niet.",
        "Een luchtbizon-guh eet hooi, appels en, als niemand kijkt, knabbels. Hij verliest nooit de weg naar huis, behalve "
        "als de wind niet meer fluit.",
        "Wie een luchtbizon-guh wil roepen, fluit. Wie niet kan fluiten, zegt heel hard 'jip jip'. Dat werkt ook.",
    ]),
    "knabbelkoor": ("Waarom het knabbelkoor zingt", [
        "In het Knabbelkoorbos groeien paarse planten die zachtjes zingen. Ze zingen geen woorden, alleen een lange, lage "
        "toon. Een beetje zoals een guh die heel tevreden is.",
        "Volgens de oude verhalen zingen ze omdat ze anders in slaap vallen. Volgens de wetenschap zingen ze omdat de wind "
        "door hun holle stengels waait.",
        "Wie een vrucht eet, flitst een stukje weg. Soms springt er een kaasmijtje uit. Dat heeft niets met zingen te maken, "
        "maar het staat ook in dit boek.",
    ]),
    "grote_zingende": ("Van grote, zingende dingen", [
        "Zeevaarders van de leegte vertellen over iets reusachtigs dat tussen de eilanden zweeft en zingt. De meeste mensen "
        "lachen erom.",
        "Een oude kapitein in de Leegtehaven lacht er niet om. Hij zegt dat je het alleen ziet als je erin gelooft.",
        "Dit boek doet geen uitspraak. Het noteert alleen dat er in de leegte soms een lied klinkt dat niemand kan verklaren.",
    ]),
    "de_eerste": ("Wie was de eerste?", [
        "Voor Aang-guh was er Roku-guh. Voor Roku-guh was er Kyoshi-guh. En daarvoor nog veel meer. Maar wie was de allereerste "
        "Guhvatar?",
        "Niemand weet het zeker. Sommigen zeggen dat het een guh met een kap was, die ver weg woonde, voorbij de mist.",
        "Anderen zeggen dat de eerste Guhvatar een gewone guh was, die op een dag iets deed wat niemand anders durfde: "
        "luisteren naar alle vier de elementen tegelijk.",
        "De bibliotheek bewaart dit boek op de onderste plank. Wan Shi Guh zegt dat sommige vragen beter een tijdje kunnen "
        "blijven liggen.",
    ]),
}

# =====================================================================================================================
# everything else
# =====================================================================================================================
TEKST = {
    # --- the element gate of the four places ---
    Q + "element.lucht": "Kom terug als je kunt luchtsturen, njeg.",
    Q + "element.water": "Kom terug als je kunt watersturen, njeg.",
    Q + "element.aarde": "Kom terug als je kunt aardesturen, njeg! Hehehe.",
    Q + "element.vuur": "Kom terug als je kunt vuursturen, njeg.",

    # --- Omashguh ---
    Q + "omash.naam.bumi": "Koning Bumi-guh",
    Q + "omash.welkom": "Hehehe! Een Guhvatar! Bij MIJ! Weet je wat jij nodig hebt? Proeven! Drie stuks. En ze werken anders dan je denkt, njeg.",
    Q + "omash.optie.doe_mee": "Ik doe mee!",
    Q + "omash.optie.waarom": "Waarom proeven?",
    Q + "omash.uitleg": "Om te leren denken als een gekke oude koning! Kijk niet naar wat iets lijkt. Kijk naar wat het is. *snurk-giechel*",
    Q + "omash.proef1": "Proef één! In de waterval van glas hangt een sleutel. Haal hem eruit. Klimmen? Hehehe. Probeer maar!",
    Q + "omash.proef1_nog": "De sleutel hangt nog in de waterval! Klimmen lukt niet, hè? Wat kan een aardesturder dat een klimmer niet kan?",
    Q + "omash.proef1_klaar": "JA! De sleutel! ...Hij past trouwens nergens op. HAHAHA! Ik wilde zien hoe je denkt. Proef twee!",
    Q + "omash.proef2_nog": "Zoek Flopsie! Mijn huisdiertje. Klein. Lief. Zo'n beetje. Hehe.",
    Q + "omash.proef2_klaar": "Je hebt Flopsie gevonden! Dat hokje was niet voor hem, dat was voor zijn knuffel. Proef drie!",
    Q + "omash.proef3_nog": "Proef drie wacht in de arena. Kies je tegenstander. Kies wijs. Of gek. Dat is hetzelfde, njeg.",
    Q + "omash.proef3_klaar": "Bumi-guh! Altijd al zo geheten! Nog één ding: de postglijbaan. Race tegen de post!",
    Q + "omash.race": "Spring op het laadplatform in een goot. Ben je binnen veertig tellen bij het postkantoor, dan ben je sneller dan de post!",
    Q + "omash.race_klaar": "Spring in een postgoot! De tijd loopt zodra je glijdt.",
    Q + "omash.race_titel": "GLIJDEN!",
    Q + "omash.race_tijd": "Tijd: %s",
    Q + "omash.race_te_laat": "Hehe, %s! De post was eerder. Nog een keer: de postlift brengt je zo naar boven!",
    Q + "omash.race_kwijt": "Je bent van de glijbaan geraakt. Geeft niks: de postlift brengt je zo weer naar boven.",
    Q + "omash.lift": "De postlift zet je terug op het laadplatform. Spring in een goot en probeer het nog eens!",
    Q + "omash.proef_titel": "Proef %s gehaald!",
    Q + "omash.proef_sub.1": "De sleutel valt uit de waterval",
    Q + "omash.proef_sub.2": "Flopsie is gevonden",
    Q + "omash.proef_sub.3": "Jij koos de gekste tegenstander",
    Q + "omash.race_gewonnen": "Sneller dan de post!",
    Q + "omash.klaar": "HAHAHA! Sneller dan de post! Hier, mijn kristalsnoep-ring. Lik er maar aan. Niet alles tegelijk, njeg.",
    Q + "omash.dank.0": "Hehehe! Kom je weer eens kristalsnoep eten?",
    Q + "omash.dank.1": "Flopsie vindt je lief. Dat zie ik aan zijn tong.",
    Q + "omash.dank.2": "Wie heeft de beste glijbanen van het Guheinde? IK!",
    Q + "omash.dank.3": "Ik ben honderdtwaalf. Ongeveer. Ik tel de verjaardagen met kristalsnoepjes en die eet ik op. Hehe.",
    Q + "omash.zone.1": "Een sleutel, hoog in een waterval van glas. Klimmen lukt nooit... maar de grond onder je luistert misschien wel.",
    Q + "omash.zone.2": "Een piepklein hokje met reuzenpootafdrukken ervoor. Hier woont Flopsie niet. Of is Flopsie juist heel groot?",
    Q + "omash.kies": "Proef drie! Kies je tegenstander. Wie durf jij aan?",
    Q + "omash.kies.1": "De Kei-kolos",
    Q + "omash.kies.2": "Het Kristalbeest",
    Q + "omash.kies.3": "Koning Bumi-guh zelf",
    Q + "omash.kies_fout.1": "De Kei-kolos? Die slaapt al honderd jaar. Saai! Kies opnieuw.",
    Q + "omash.kies_fout.2": "Het Kristalbeest is een standbeeld, njeg. Kies opnieuw!",
    Q + "omash.kies_goed": "MIJ?! HAHAHA! Goed zo! De gevaarlijkste tegenstander staat altijd naast je en lacht. Ik geef me over!",
    Q + "omash.naam_vraag": "Laatste vraag. Wat... is... mijn... naam?",
    Q + "omash.naam_optie.1": "Koning Kristalsnoep",
    Q + "omash.naam_optie.2": "Flopsie",
    Q + "omash.naam_optie.3": "Bumi-guh",
    Q + "omash.naam_fout.1": "Kristalsnoep? Lekker, maar nee. Denk eenvoudig!",
    Q + "omash.naam_fout.2": "Flopsie is het huisdier! Ik ben de koning. Nog een keer!",
    Q + "omash.hint.proef1": "Ga naar proef 1 (de waterval van glas) onder de troonzaal en gebruik daar je aardekracht.",
    Q + "omash.hint.proef2": "Zoek Flopsie. Niet in het hokje van proef 2, maar ergens... groter.",
    Q + "omash.hint.proef3": "Ga naar de arena van proef 3 en kies je tegenstander.",
    Q + "omash.hint.race": "Glij vanaf het laadplatform voor het paleis binnen 40 seconden naar het postkantoor.",

    # --- the Westelijke Luchttempel ---
    Q + "lucht.naam.luchtnon": "De luchtnon-guh",
    Q + "lucht.welkom": "Hoor je dat? Niets. De wind fluit niet meer door de gangen, en onze kudde is verdwaald. Een luchtsturder kan ons helpen.",
    Q + "lucht.optie.help": "Ik help je!",
    Q + "lucht.optie.wat": "Hoe kan een tempel zingen?",
    Q + "lucht.uitleg": "Vijf windgangen, elk met een balkon aan het eind. Wie daar luchtstuurt, laat de gang fluiten. Samen zingen ze de kudde naar huis.",
    Q + "lucht.fluit": "Ga naar het balkon aan het eind van elke windgang en gebruik daar je luchtkracht. Vijf gangen, vijf tonen.",
    Q + "lucht.fluit_nog": "Nog %s windgangen zijn stil.",
    Q + "lucht.fluit_al": "Deze windgang zingt al.",
    Q + "lucht.fluit_een": "Een windgang zingt! (%s van de %s)",
    Q + "lucht.fluit_klaar": "Hoor je dat?! De hele tempel zingt weer! Nu hoort de kudde de weg naar huis. Help ze nog even.",
    Q + "lucht.kudde_nog": "Nog %s luchtbizon-guhs zijn verdwaald. Ze zweven rond het eiland.",
    Q + "lucht.kudde_een": "Een luchtbizon-guh vliegt naar huis! (%s van de %s)",
    Q + "lucht.kudde_klaar": "Ze zijn allemaal thuis! Vijf hokken, vijf blije bizons. Kom, ik moet je iets geven.",
    Q + "lucht.klaar": "Het is lang geleden dat deze tempel zo vrolijk klonk. Hier, twee windgongen. Hang ze ergens op waar het waait.",
    Q + "lucht.optie.dank": "Graag gedaan!",
    Q + "lucht.dank.0": "Luister. Hoor je de tempel? Hij neuriet.",
    Q + "lucht.dank.1": "De kudde eet weer goed. Vooral de kleinste. Die eet voor drie.",
    Q + "lucht.dank.2": "Een luchtnomade bezit niets, behalve een goede wind. En nu dus ook die van jou, njeg.",
    Q + "lucht.hint.fluit": "Gebruik je luchtkracht op het balkon aan het eind van elke windgang (5).",
    Q + "lucht.hint.kudde": "Vlieg naar de verdwaalde luchtbizon-guhs rond het eiland en klik ze, of richt van een afstand je Luchtbal op ze (5).",
    Q + "lucht.fluit_titel": "De tempel zingt!",
    Q + "lucht.kudde_titel": "De kudde is thuis!",
    Q + "lucht.hint.terug": "Ga terug naar de luchtnon-guh in de hangende hal.",

    # --- the Vuurtempel van Roku-guh ---
    Q + "vuur.naam.shyu": "Shyu",
    Q + "vuur.naam.roku": "Roku-guh",
    Q + "vuur.welkom": "Sst. De andere Vuurwijzen mogen het niet horen. De vijf deuren boven gaan alleen open voor het vuur van een Guhvatar.",
    Q + "vuur.optie.ja": "Ik steek ze aan.",
    Q + "vuur.optie.wie": "Wat is er achter de deuren?",
    Q + "vuur.uitleg": "Het heiligdom, met het beeld van Roku-guh. Mijn opa was de laatste die hem daar zag. Ik geloof dat hij nog wacht.",
    Q + "vuur.deuren": "Ga boven voor elke vuurdeur staan en gebruik je vuurkracht. Vijf sloten, vijf vlammen. En wees stil, njeg.",
    Q + "vuur.deur_nog": "Nog %s vuurdeuren zijn koud.",
    Q + "vuur.deur_al": "Dit slot brandt al.",
    Q + "vuur.deur_een": "Het slot gloeit! (%s van de %s)",
    Q + "vuur.deuren_open": "Ze zijn open! Alle vijf! Ga naar binnen, Guhvatar. Ik houd de anderen wel bezig met thee.",
    Q + "vuur.deuren_titel": "De vuurdeuren gaan open!",
    Q + "vuur.deuren_sub": "Loop recht op een deur af",
    Q + "vuur.open": "De deuren zijn open, voor jou. Ga het heiligdom in.",
    Q + "vuur.roku_wacht": "Hij is er echt? Roku-guh? Ga, ga! Laat hem niet wachten.",
    Q + "vuur.shyu_dank.0": "Mijn opa zou zo trots zijn. En ook een beetje jaloers.",
    Q + "vuur.shyu_dank.1": "De andere Vuurwijzen denken dat er tocht in de toren zat. Ik zeg niks.",
    Q + "vuur.shyu_dank.2": "Vuur is als thee: te heet en je brandt je, te koud en je hebt er niks aan.",
    Q + "vuur.roku.1": "Lang geleden had ik een vriend. Hij was de prins van het vuur, en ik was de Guhvatar. We deelden alles. Ook onze knabbels.",
    Q + "vuur.roku.2": "Toen hij Vuurheer werd, wilde hij meer. Meer land. Meer knabbels. Ik zei nee, en hij hoorde me niet.",
    Q + "vuur.roku.3": "Ik had hem eerder moeten tegenhouden. Niet met vuur, maar met vriendschap. Ik wachtte te lang.",
    Q + "vuur.roku.4": "Zijn kleinzoon is Vuurheer Mikai. De honger is dezelfde gebleven. Maar jij hebt iets wat ik toen niet had.",
    Q + "vuur.roku.5": "Je hebt vrienden die je de waarheid durven te zeggen. Luister naar ze. Dat is het hele geheim, njeg.",
    Q + "vuur.optie.dank_roku": "Dank je, Roku-guh.",
    Q + "vuur.roku_klaar": "Neem dit vlammetje mee. Het gaat nooit uit, zoals een goede vriendschap. Ik zie je weer, Guhvatar.",
    Q + "vuur.roku_later.0": "Een vlam die je deelt, wordt niet kleiner.",
    Q + "vuur.roku_later.1": "Kyoshi-guh zegt dat ik te veel praat. Kyoshi-guh praat met haar voeten.",
    Q + "vuur.roku_later.2": "Ga maar. De wereld wacht. Ik wacht hier wel, daar ben ik goed in.",
    Q + "vuur.roku_vroeg": "...",
    Q + "vuur.hint.deuren": "Gebruik je vuurkracht voor elk van de vijf vuurdeuren boven in de toren.",
    Q + "vuur.hint.heiligdom": "Loop door een van de open vuurdeuren het heiligdom in.",
    Q + "vuur.hint.roku": "Praat met Roku-guh in het heiligdom.",

    # --- the Kyoshi-dorp ---
    Q + "kyoshi.naam.leider": "De leidster van de Kyoshi-krijgsters",
    Q + "kyoshi.welkom": "Een waterstuurder bij de Kyoshi-krijgsters? Wij vechten met waaiers, maar eerst dansen we. Durf je mee te dansen?",
    Q + "kyoshi.optie.dans": "Ik dans mee!",
    Q + "kyoshi.optie.wie": "Wie was Kyoshi-guh?",
    Q + "kyoshi.uitleg": "De Guhvatar met de grootste voeten van het Guheinde. Ze stichtte dit dorp. Haar beeld kijkt nog steeds of we goed oefenen.",
    Q + "kyoshi.dans": "Ga op de gouden tegel midden op de dansvloer staan. Roep ik BUK, dan buk je. SPRING, dan spring je. DRAAI, dan draai je om.",
    Q + "kyoshi.dans_nog": "De gouden tegel wacht. Tien van de zestien goed, dan ben je een van ons.",
    Q + "kyoshi.dans_titel": "De waaierdans",
    Q + "kyoshi.dans_sub": "Luister naar de maat...",
    Q + "kyoshi.zet.buk": "BUK!",
    Q + "kyoshi.zet.spring": "SPRING!",
    Q + "kyoshi.zet.draai": "DRAAI!",
    Q + "kyoshi.zet_teller": "%s / %s",
    Q + "kyoshi.zet_goed": "Mooi! (%s goed)",
    Q + "kyoshi.zet_mis": "Mis... volgende!",
    Q + "kyoshi.dans_goed": "Prachtig gedanst!",
    Q + "kyoshi.dans_bijna": "Bijna!",
    Q + "kyoshi.dans_score": "%s van de %s goed",
    Q + "kyoshi.dans_weg": "Je liep van de dansvloer af. Ga weer op de gouden tegel staan om opnieuw te beginnen.",
    Q + "kyoshi.dans_klaar": "Je danst als een echte krijgster! Kom, aan de schminktafel. Een krijgster laat zich zien.",
    Q + "kyoshi.dans_opnieuw": "Niet gek, maar het moeten er %s zijn. Nog een keer: ga weer op de gouden tegel staan.",
    Q + "kyoshi.schmink": "De schminktafel staat onder de spiegel. Wit, rood en een beetje goud.",
    Q + "kyoshi.geschminkt": "Geschminkt!",
    Q + "kyoshi.geschminkt_sub": "Wit, rood en een beetje goud",
    Q + "kyoshi.schmink_klaar": "Je ziet eruit als Kyoshi-guh zelf! Ga je eens laten zien bij de poort. Er staat daar iemand die... nou ja. Je ziet het wel.",
    Q + "kyoshi.fan": "De fan-guh bij de poort heeft nog nooit een echte krijgster van dichtbij gezien. Ga maar. En houd een emmer bij de hand.",
    Q + "kyoshi.fan_schuim": "Een... een... ECHTE KYOSHI-KRIJGSTER! Hier! Bij MIJ! *schuim* *plof*",
    Q + "kyoshi.klaar": "Hij is weer flauwgevallen. Dat is de vierde keer deze week. Je hoort nu bij ons, krijgster. Je schmink staat je guh vast ook.",
    Q + "kyoshi.fan_later.0": "Mag ik je handtekening? Op mijn waaier? Op mijn gezicht?",
    Q + "kyoshi.fan_later.1": "Ik heb vandaag maar één keer geschuimd. Ik word sterker.",
    Q + "kyoshi.fan_later.2": "Ik heb een poster van jou. Ik heb hem zelf getekend. Je oren zijn een beetje groot geworden.",
    Q + "kyoshi.dank.0": "Een waaier is geen wapen. Het is een dansje dat toevallig ook verdedigt.",
    Q + "kyoshi.dank.1": "Kyoshi-guh zei altijd: sta stevig, en draag goede schoenen. Vooral dat laatste.",
    Q + "kyoshi.dank.2": "Je bent altijd welkom op de dansvloer, krijgster.",
    Q + "kyoshi.hint.dans": "Ga op de gouden tegel midden op de dansvloer in de dojo staan.",
    Q + "kyoshi.hint.schmink": "Ga naar de schminktafel onder de spiegel in de dojo.",
    Q + "kyoshi.hint.fan": "Laat je zien aan de schuimbekkende fan-guh bij de poort.",

    # --- the Bibliotheek van Wan Shi Guh ---
    Q + "bieb.naam.uil": "Wan Shi Guh",
    Q + "bieb.naam.professor": "Professor Zei-guh",
    Q + "bieb.regels.1": "Welkom in mijn bibliotheek. Ik ben Wan Shi Guh, hij-die-tienduizend-dingen-weet. Ongeveer tienduizend. Ik tel nog.",
    Q + "bieb.regels.2": "Er zijn drie regels. Geen Mika's. Geen vuur. En wie hier kennis haalt, brengt ook kennis mee.",
    Q + "bieb.regels.3": "Wat weet jij, kleine Guhvatar? Breng me drie stukjes kennis, en mijn vos-guhs zoeken een boek voor je.",
    Q + "bieb.optie.kennis": "Ik breng je kennis.",
    Q + "bieb.kennis_wat": "Kennis is een kaart, een kompas, een klok, een verrekijker, een boek. Nog %s stukjes. Houd het vast en geef het me.",
    Q + "bieb.kennis_een.0": "Hm. Een goed begin. Nog %s.",
    Q + "bieb.kennis_een.1": "Interessant. Heel interessant. Nog %s.",
    Q + "bieb.kennis_een.2": "Ah, dit had ik nog niet. Nog %s.",
    Q + "bieb.kennis_klaar": "Drie stukjes kennis. Een eerlijke ruil. Mijn vos-guhs zoeken nu het boek dat bij jou past. Zoek er een.",
    Q + "bieb.vos": "*Ruikt aan je oren, legt een boek in je pootjes en rent weg.*",
    Q + "bieb.vos_zoek": "Een vos-guh brengt je boek. Ze rennen overal rond in de zalen.",
    Q + "bieb.planetarium": "In het planetarium draait professor Zei-guh aan de hemel. Ga eens kijken. Wat je daar ziet, is belangrijk.",
    Q + "bieb.prof_wiel": "Ah, een bezoeker! Draai eens aan het wiel. Kijk wat de zon en de maan doen.",
    Q + "bieb.prof_later.0": "Ik woon hier nu. Ik heb nog vierduizend boeken te gaan. Ongeveer.",
    Q + "bieb.prof_later.1": "Weet jij hoe je uit een bibliotheek komt die half onder het zand ligt? Ik ook niet. Maar ik vind het niet erg.",
    Q + "bieb.prof_later.2": "Pas op de trappen. Het zijn boekenkasten.",
    Q + "bieb.planetarium_klaar.1": "Kijk! De maan schuift voor de zon. Midden op de dag wordt het even donker. Zelfs vuur gaat dan even liggen.",
    Q + "bieb.planetarium_klaar.2": "De dag dat de zon even ging vadsen. Schrijf het maar op, Guhvatar. Ooit kan het belangrijk zijn.",
    Q + "bieb.klaar": "Je hebt iets geleerd en iets gebracht. Kom terug wanneer je wilt: elke dag ruil ik een stukje kennis voor een boek.",
    Q + "bieb.later.0": "Breng me kennis, en ik geef je een boek. Eén per dag. Ik ben een uil, geen drukkerij.",
    Q + "bieb.later.1": "Mijn vos-guhs zijn snel, maar soms leggen ze boeken op de verkeerde plank. Ik vermoed dat ze het expres doen.",
    Q + "bieb.later.2": "Alles wat hier staat, is waar. Behalve de kookboeken.",
    Q + "bieb.ruil_morgen": "Vandaag heb je al geruild. Kom morgen terug.",
    Q + "bieb.ruil": "Een eerlijke ruil. Lees het goed. En zet het later netjes terug in je eigen kast.",
    Q + "bieb.hint.kennis": "Geef Wan Shi Guh drie stukjes kennis (kaart, kompas, klok, verrekijker, boek): houd het vast en klik op hem.",
    Q + "bieb.hint.vos": "Zoek een vos-guh in de bibliotheek en klik erop.",
    Q + "bieb.hint.planetarium": "Draai aan het wiel van het planetarium, bij professor Zei-guh.",

    # --- the Leeuwschildpad-eiland ---
    Q + "schildpad.naam": "De Leeuwschildpad-guh",
    Q + "schildpad.kietel": "Hmmmmm? Wie kietelt er over mijn schild? Kom eens naar beneden, kleintje. Naar mijn poot.",
    Q + "schildpad.slaap": "*Zzzzzz... een diep, warm gesnurk, zo groot als een eiland.*",
    Q + "schildpad.wakker.1": "Ah. Een Guhvatar. Ik heb er al heel wat gezien. Ze komen, ze gaan. Ik zwem.",
    Q + "schildpad.wakker.2": "Ik ben ouder dan de eilanden, en bijna net zo vadsig. Dat is een compliment, njeg.",
    Q + "schildpad.vraag": "Zeg eens, kleintje. Wat zoek je eigenlijk?",
    Q + "schildpad.antwoord.1": "Kracht.",
    Q + "schildpad.antwoord.2": "Rust.",
    Q + "schildpad.antwoord.3": "Eerlijk gezegd: knabbels.",
    Q + "schildpad.wijs.1": "Kracht. Hm. Wie sterk is, kan dragen. Wie wijs is, weet wanneer hij iets moet neerleggen.",
    Q + "schildpad.wijs.2": "Rust. Goed antwoord. Rust is geen stilstand. Rust is zwemmen zonder haast.",
    Q + "schildpad.wijs.3": "HOHOHO. Eindelijk iemand die de waarheid zegt. Knabbels zijn kracht en rust tegelijk.",
    Q + "schildpad.offer": "Weet je wat ik al eeuwen niet heb gehad? Een knabbel. Leg er eens eentje in mijn poot.",
    Q + "schildpad.offer_nog": "Een knabbel, kleintje. Houd hem vast en geef hem aan mijn snuit.",
    Q + "schildpad.kom_op_poot": "Kom op mijn poot staan, kleintje. Mijn ogen zijn niet meer zo goed.",
    Q + "schildpad.klaar": "Draag die schub goed. Hij weet waar ik ben, en ik weet waar jij bent.",
    Q + "schildpad.morgen": "Eén wijsheid per dag, kleintje. Meer is te zwaar voor een kleine buik.",
    Q + "schildpad.dagwijsheid.0": "Wie haast heeft, mist het uitzicht. Wie geen haast heeft, mist soms de boot. Allebei is goed.",
    Q + "schildpad.dagwijsheid.1": "Een eiland dat zwemt, is nooit verdwaald. Het is gewoon onderweg.",
    Q + "schildpad.dagwijsheid.2": "De leegte lijkt leeg. Maar er zingt soms iets in. Luister maar.",
    Q + "schildpad.dagwijsheid.3": "Je hoeft niet alles te kunnen. Je hoeft alleen te weten wie het wel kan.",
    Q + "schildpad.dagwijsheid.4": "Een knabbel delen is twee keer eten. Eén keer met je mond, één keer met je hart.",
    Q + "schildpad.dagwijsheid.5": "Ook een oude schildpad leert nog iets. Vandaag leerde ik dat jij me kwam opzoeken.",
    Q + "schildpad.dagwijsheid.6": "Water stroomt om de steen heen. De steen vindt dat prima.",
    Q + "schildpad.dagwijsheid.7": "Slaap goed, eet goed, vads goed. De rest volgt vanzelf.",
    Q + "schildpad.gevangen": "De Leeuwschildpad-guh vangt je op!",
    Q + "schildpad.hint.poot": "Loop naar de opgeheven poot van de Leeuwschildpad en ga op zijn handpalm staan.",
    Q + "schildpad.hint.knabbel": "Houd een kaasknabbel vast en klik op de snuit van de Leeuwschildpad, op zijn handpalm.",

    # --- the sorry Mika cook ---
    Q + "kok.welkom": "O! Schrik niet. Ik ben maar de kok. Ik kook stiekem voor de gevangen guhs. Elke dag. En ik heb er zo'n spijt van dat ik een Mika ben.",
    Q + "kok.optie.help": "Kan ik je helpen?",
    Q + "kok.optie.waarom": "Waarom doe je dat stiekem?",
    Q + "kok.uitleg": "Mika's mogen niet lief zijn. Dat staat in het Mika-boek, op bladzijde één. Ik heb bladzijde één eruit gescheurd. Sorry.",
    Q + "kok.ingredienten": "Ik wil echte soep maken. Met 2 schimmelpaddenstoelen, 2 kristalkaasbrokjes, 2 knabbelkoorvruchten en 1 kruimelwolk.",
    Q + "kok.ingredienten_nog": "Ik heb nog niet alles. Schimmelpaddenstoelen, kristalkaasbrokjes, knabbelkoorvruchten, een kruimelwolk. Sorry dat ik zo veel vraag.",
    Q + "kok.ingredienten_klaar": "O, dit ruikt naar het hele Guheinde! Maar de kapitein mag me niet zien koken. Kun je hem even... afleiden?",
    Q + "kok.kapitein_nog": "De kapitein staat beneden. Verzin iets. Iets wat hij gelooft. Sorry.",
    Q + "kok.soep": "Hij is weg! De soep is klaar en de cellen krijgen een kom. Weet je wat? Ik ga weg. Ik wil een eigen keukentje. Met een raam.",
    Q + "kok.optie.ga": "Ga maar! Ik zie je in de Leegtehaven.",
    Q + "kok.vertrek": "Sorry dat ik zo snel ga. Sorry voor de rommel. Tot in de Leegtehaven!",
    Q + "kok.weg": "...",
    Q + "kok.kapitein": "WAT MOET JE? Dit is MIJN vesting. Zeg het snel, ik heb het druk met... dingen.",
    Q + "kok.afleiding.1": "Kijk! Een vetschip zonder kapitein!",
    Q + "kok.afleiding.2": "Uw hoed staat scheef.",
    Q + "kok.afleiding.3": "Mag ik even door naar de keuken?",
    Q + "kok.afgeleid": "WAT?! Een schip zonder kapitein?! Dat kan niet! Dat MAG niet! Ik ga kijken! Niemand doet iets tot ik terug ben!",
    Q + "kok.hoed": "...Echt? *zet hoed recht* ...Bedankt. En nu WEG.",
    Q + "kok.nee": "NEE.",
    Q + "kok.haven": "Je bent gekomen! Kijk, mijn eigen keukentje. Met een raam! Naar de leegte! Hier, het recept van mijn Spijtsoep. Sorry dat hij zo lekker is.",
    Q + "kok.optie.smakelijk": "Smakelijk!",
    Q + "kok.haven_vreemd": "Hallo? Kennen wij elkaar? Sorry. Ik ben hier nieuw. Ik heb spijt van van alles.",
    Q + "kok.dagsoep": "Hier, een kom Spijtsoep. Nog warm. Sorry dat het er maar één is.",
    Q + "kok.later.0": "Ik heb een raam. Ik kijk elke dag naar de leegte. Sorry, ik ben gewoon blij.",
    Q + "kok.later.1": "De kapitein heeft dat schip nooit gevonden. Hij zoekt nog. Sorry, kapitein.",
    Q + "kok.later.2": "Morgen weer soep. Beloofd. Sorry dat het pas morgen is.",
    Q + "kok.hint.ingredienten": "Haal de ingrediënten in de buiteneilanden: schimmelpaddenstoelen, kristalkaasbrokjes, knabbelkoorvruchten, een kruimelwolk.",
    Q + "kok.hint.kapitein": "Praat met de vesting-kapitein op de begane grond en leid hem af.",
    Q + "kok.hint.terug": "Ga terug naar de kok in de keuken van de vesting.",
    Q + "kok.hint.haven": "Zoek de kok op in zijn nieuwe keuken in de Leegtehaven: De Spijtige Spatel.",

    # --- the vetbootje commission ---
    Q + "bootje.naam": "De scheepsbouwer-guh",
    Q + "bootje.briefje.1": "~ GEZOCHT: een dappere vaarder ~",
    Q + "bootje.briefje.2": "Wil je een eigen vetbootje? Klein, traag, vliegt toch. Met een kist en plekjes voor je guhs.",
    Q + "bootje.briefje.3": "Breng 24 planken, 8 wol, 4 Mika's vet en 2 flitskruimels naar de scheepswerf in de Leegtehaven.",
    Q + "bootje.briefje.4": "(Iemand heeft er onder geschreven: 'niet aan de kapitein vertellen'.)",
    Q + "bootje.werf": "Ah, mijn favoriete schipper! Welkom op de werf. Een nieuwe kleur? Een ander zeil?",
    Q + "bootje.geen_opdracht": "Een eigen bootje? Dan heb je een opdracht nodig. Kijk eens op het havenbord van een Mika-vesting.",
    Q + "bootje.materiaal": "Voor een vetbootje heb ik nodig: 24 planken, 8 wol, 4 Mika's vet en 2 flitskruimels. Dan begin ik meteen!",
    Q + "bootje.optie.ok": "Ik ga het halen.",
    Q + "bootje.begint": "Alles is er! Kijk maar op de helling. Even geduld: hamer hamer, zaag zaag, njeg.",
    Q + "bootje.bezig": "Ik ben bezig! Hamer hamer. Nog even.",
    Q + "bootje.af": "Je vetbootje is af!",
    Q + "bootje.af_sub": "Ga terug naar de scheepsbouwer-guh",
    Q + "bootje.af_zeg": "Klaar! Het mooiste bootje dat ik ooit heb gebouwd. Kom het maar dopen.",
    Q + "bootje.doop": "Hier is hij. Een eigen vetbootje. Geef hem een kleur, een zeil en een naam. Breek er geen fles op, alsjeblieft.",
    Q + "bootje.optie.doop": "Naar de werf!",
    Q + "bootje.hint.briefje": "Lees het briefje op het havenbord aan de steiger van een Mika-vesting.",
    Q + "bootje.hint.werf": "Breng de materialen naar de scheepsbouwer-guh in de Leegtehaven.",
    Q + "bootje.hint.materiaal": "Verzamel 24 planken, 8 wol, 4 Mika's vet en 2 flitskruimels.",
    Q + "bootje.hint.doop": "Praat met de scheepsbouwer-guh om je vetbootje te dopen.",

    # --- the Leegtehaven ---
    Q + "haven.kraam": "Kraam van een Ender-Mika",
    Q + "haven.veer.welkom": "De Vadsige Golf vaart naar elke plek die je kent. Gratis! Waar wil je heen?",
    Q + "haven.veer.verder": "En verder kan ik je hierheen brengen:",
    Q + "haven.veer.meer": "Andere plekken...",
    Q + "haven.veer.hoofdeiland": "Het hoofdeiland (de Knabbelberg)",
    Q + "haven.veer.mist": "Daar kan de veerboot niet komen: die plek is nog niet te vinden.",
    Q + "haven.veer.aan": "De Vadsige Golf zet je af bij: %s",

    # --- the ring race ---
    Q + "race.naam": "De racemeester-guh",
    Q + "race.welkom": "Welkom bij de Guhvleugel-ringenrace! Achttien ringen, één landingsbaan, en de klok loopt. Klaar?",
    Q + "race.welkom_terug": "Daar is mijn racer! Je beste tijd is %s. Kun je sneller?",
    Q + "race.optie.start": "Start!",
    Q + "race.optie.uitleg": "Hoe werkt het?",
    Q + "race.uitleg": "Vlieg door alle achttien ringen, op volgorde, en land op de landingsbaan. Onder de %s krijg je een titel!",
    Q + "race.niet_op_start": "Ga eerst op het startplatform staan.",
    Q + "race.klaar": "Op je plaatsen...",
    Q + "race.vlieg": "VLIEG!",
    Q + "race.ring": "Ring %s / %s  -  %s",
    Q + "race.mis": "Uit de race! De wind zet je zo terug op het startplatform; praat dan met de racemeester-guh om opnieuw te beginnen.",
    Q + "race.terug": "Terug op het startplatform. Praat met de racemeester-guh voor een nieuwe ronde!",
    Q + "race.finish": "Geland!",
    Q + "race.finish_record": "Nieuw eigen record: %s",
    Q + "race.finish_tijd": "Tijd: %s",
    Q + "race.titel": "Onder de %s! Jij bent een echte Ringenracer!",
    Q + "race.bord": "Ringenrace",

    # --- the powers ---
    G + "kracht.waterloop": "Je loopt over het water!",
    G + "kracht.geen": "Je kent hier nog geen vadskunst.",
    G + "kracht.wacht": "%s moet nog even afkoelen.",
    G + "kracht.wiel": "Elementenwiel",
    G + "kracht.kies": "Kies...",
    "gui.guhs.einde2_kracht.zij_luchtsprong": "Luchtsprong",
    "gui.guhs.einde2_kracht.zij_waterloop": "Over water lopen",
    "gui.guhs.einde2_kracht.zij_aardzuil": "Aardzuiltje",
    "gui.guhs.einde2_kracht.zij_handvlam": "Handvlam",
    "key.guhs.einde2_kracht": "Vadskunst (tikken: gebruiken, vasthouden: elementenwiel)",

    # --- the Guhdex lists (needs, rewards) ---
    G + "nodig.kennis": "Stukjes kennis",
    G + "nodig.planken": "Planken",
    G + "nodig.wol": "Wol",
    G + "nodig.vet": "Mika's vet",
    G + "nodig.flitskruimel": "Flitskruimels",
    G + "beloning.windgongen": "Twee windgongen",
    G + "beloning.vlam": "Het eeuwige vlammetje",
    G + "beloning.schmink": "Kyoshi-schmink (voor je guh)",
    G + "beloning.boeken": "Boeken uit de bibliotheek",
    G + "beloning.recept": "Het recept van de Spijtsoep",
    G + "beloning.vetbootje": "Een eigen vetbootje",
    G + "boek.door": "Uit de bibliotheek van Wan Shi Guh",

    # --- items, the block, the clothes ---
    "item.guhs.einde2_zij_kristalsnoep_ring": "Kristalsnoep-ring",
    "item.guhs.einde2_zij_kristalsnoep_ring.lore": "Van koning Bumi-guh. Lik eraan: je graaft een tijdje extra snel.",
    "item.guhs.einde2_zij_kristalsnoep_ring.lik": "*lik* Kristalsnoep! Je graaft extra snel.",
    "item.guhs.einde2_zij_zegenschub": "Schub van de Leeuwschildpad",
    "item.guhs.einde2_zij_zegenschub.lore": "Val je in de leegte van het Guheinde, dan vangt de oude schildpad je op.",
    "item.guhs.einde2_zij_spijtsoep": "Spijtsoep",
    "item.guhs.einde2_zij_spijtsoep.lore": "Van de spijtige Mika-kok. Sorry dat hij zo lekker is.",
    "block.guhs.einde2_zij_aardzuil": "Aardzuiltje",
    "item.guhs.einde2_zij_kyoshi_schmink": "Kyoshi-schmink",
    "gui.guhs.kledingbron.einde2_zij": "Kyoshi-dorp",

    # --- titles ---
    "gui.guhs.titel.vestingbevrijder": "Vestingbevrijder",
    "gui.guhs.titels.hint.vestingbevrijder": "Bevrijd zes magere guhs uit de cellen van de Mika-vestingen.",
    "gui.guhs.titel.ringenracer": "Ringenracer",
    "gui.guhs.titels.hint.ringenracer": "Vlieg de Guhvleugel-ringenrace in minder dan 45 seconden.",
}

# the FTB quests (key -> (title, text)); the step quests come from LIJNEN through wereld.ftb_questlijn
FTB_EXTRA = {
    "krachten": ("Vier blijvende vadskunsten", "Elk element dat je in het verhaal van de Guhvatar leert, laat iets blijvends achter: een "
                 "&6luchtsprong&r (ook in de lucht), &6over water lopen&r, een &6aardzuiltje&r en een &6handvlam&r. Eén toets (standaard "
                 "&dH&r): tikken = gebruiken, vasthouden = het elementenwiel."),
    "handel": ("Ruilen met Ender-Mika's", "Op de markt van de Leegtehaven staan Ender-Mika's achter hun kraam. Hun aanbod wisselt elke "
               "dag. Klik er een aan om te ruilen."),
    "veer": ("De Vadsige Golf", "De veerman-guh in de Leegtehaven vaart je gratis naar elke plek van het Guheinde die je kent, en naar "
             "het hoofdeiland."),
    "ringenrace": ("De Guhvleugel-ringenrace", "Praat met de racemeester-guh op het startplatform, vlieg door alle achttien ringen en "
                   "land op de landingsbaan. Onder de 45 seconden word je &dRingenracer&r."),
    "vestingbevrijder": ("Vestingbevrijder", "Bevrijd zes magere guhs uit de cellen van de Mika-vestingen (geef ze een knabbel). Dat "
                         "levert de titel &dVestingbevrijder&r op."),
}
