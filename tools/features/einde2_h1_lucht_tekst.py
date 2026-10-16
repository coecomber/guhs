"""
einde2 phase B, slice h1_lucht: every Dutch text of chapter 1 of the Guhvatar story (the source; the English overlay is
tools/lang/en/c92_e2_h1_lucht.json). Read by einde2_h1_lucht.build(h).

  LIJN        the questline guhvatar_lucht (verhaal_motor.verhaallijn: name, explanation, the ten steps, the variant 9_vlucht)
  KAART       the narrator card of the chapter (verhaal_motor.vertelkaart)
  SCENES      the eight cutscenes: {naam: (titel, {key: tekst}, {acteur: naam})} (Java: H1Scenes; scene.guhs.einde2_h1_lucht_<naam>.*)
  TEXTS       everything else: the hints, the talks of Aang-guh, Guhatso, Roku-guh and Kyoshi-guh, the minigames, the rewards
  FTB         the FTB section "De Guhvatar: Lucht": (key, titel, tekst, icon, stap)

Tone (DESIGN 5): as close to the show as possible, more emotion than usual, guh word play in names and small lines. Every
line is in own words; the opening film's first two sentences are the DESIGN's own (approved) parody lines.
"""

P = "gui.guhs.einde2_h1_lucht"
Q = "quest.guhs.einde2_h1_lucht"
LIJN_ID = "guhvatar_lucht"

# =====================================================================================================================
# the questline (step i = index i; 10 steps, done at 10)
# =====================================================================================================================
LIJN = dict(
    naam="Hoofdstuk 1: De Zuidelijke Luchttempel",
    uitleg="Een gloeiende kaasbol spoelt aan bij de Knabbelberg. Erin: Aang-guh, de vorige Guhvatar, en zijn luchtbizon "
           "Appa-guh. En jij blijkt de nieuwe Guhvatar te zijn. Eerst: luchtsturen leren in de Zuidelijke Luchttempel.",
    stappen=[
        ("De kaasbol", "Open de gloeiende kaasbol die bij de Knabbelberg is aangespoeld.",
         "Het Guheinde, bij de Knabbelberg (waar je uit het portaal komt)"),
        ("Naar de Luchttempel", "Vlieg op Appa naar de Zuidelijke Luchttempel. Ben je eraf gestapt? Fluit hem met je Appa-fluitje.",
         "De Zuidelijke Luchttempel, hoog boven de leegte"),
        ("De Hal van de Guhvatars", "Ga de grote trap op naar de Hal van de Guhvatars en praat met de geest van Roku-guh of Kyoshi-guh.",
         "De grote hal in het midden van de tempel"),
        ("Luchtbal", "Praat met de herinnering van monnik Guhatso op het taartenterras en speel luchtbal tegen Aang-guh. Eerst tot drie!",
         "Het taartenterras en het luchtbalveld, west van de hal"),
        ("De luchtscooter-baan", "Ga naar de start van de luchtscooter-baan en race de hele baan rond voordat de tijd om is.",
         "De start van de baan, zuidwest van het voorplein"),
        ("De zweefringen", "Klim naar het zuidbalkon van de grote toren, spring en zweef door minstens tien van de veertien ringen.",
         "Het zuidbalkon boven op de grote toren (via de noordelijke torens en hun bruggen)"),
        ("Achter Momo aan!", "Een vleermuis-lemurguhtje heeft je knabbel gegrist! Ga naar de keuken van de monnikentoren en volg hem.",
         "De keuken onder in de monnikentoren (noordwest)"),
        ("Appa's stal", "Maak Appa's kapotte zadel heel (klik er een paar keer op) en voer hem drie appels uit de appelton.",
         "Appa's stal tussen de twee noordelijke torens"),
        ("De dichte deur", "Er tikt iets achter de dichte deur ten oosten van de hal. Ga eens kijken wie daar knutselt.",
         "De werkplaats, oost van de hal"),
        ("Zuka", "Er ligt iets aan de steiger! Ga naar de steiger aan de oostkant van het landingsterras.",
         "De steiger aan de oostkant van het landingsterras"),
    ],
    klaar=("Luchtsturen geleerd! Appa vliegt je naar de Noordelijke Waterstam.", "Op weg naar de Noordelijke Waterstam"),
    extra={"9_vlucht": ("Ontsnap! Ren naar Appa in het kamp west van de landingsplek. Ontwijk Zuka's vuurballen en zijn mannen.",
                        "Het kamp west van de landingsplek")},
    kort={"0": "Open de gloeiende kaasbol", "1": "Vlieg met Appa naar de Luchttempel", "2": "Naar de Hal van de Guhvatars",
          "3": "Luchtbal tegen Aang-guh", "4": "Race de luchtscooter-baan", "5": "Zweef door de ringen", "6": "Volg het lemurguhtje",
          "7": "Zadel heel, Appa appels", "8": "Kijk achter de dichte deur", "9": "Naar de steiger", "9_vlucht": "Ren naar Appa!",
          "klaar": "Op naar de Waterstam"},
)

# the narrator card of the chapter (4 lines)
KAART = ("Hoofdstuk 1: Lucht", [
    "Honderd jaar lang sliep de Guhvatar in een kaasbol. Nu is de kaasbol open, en de wereld heeft honger.",
    "Hoog boven de leegte zweeft de Zuidelijke Luchttempel. Hier leerden de luchtnomaden-guhs zweven, spelen en delen.",
    "Aang-guh groeide hier op. Nu is het er stil, maar de wind weet de weg nog.",
    "Leer de lucht kennen, Guhvatar. Ze is licht, vrij... en een beetje ondeugend. Njeg.",
])

# the travel map of the whole story (Reiskaart "guhvatar", group "guheinde")
REISKAART = "De reis van de Guhvatar"

# =====================================================================================================================
# the cutscenes (Java: H1Scenes; the keys are the zeg keys there)
# =====================================================================================================================
SCENES = {
    "kaasbol": ("De kaasbol", {
        "drijft": "Hoog boven de leegte drijft iets gloeiends naar de Knabbelberg. Langzaam. Plechtig. Een beetje scheef.",
        "plof": "Plof. Het rolt nog een stukje door... en ligt stil.",
        "barst": "Er loopt een gloeiende barst over de kaas. Hij wordt breder. Het licht wordt feller.",
        "gaap": "Haaaaa... Wat een dutje. Hoe lang heb ik geslapen? Een uurtje? Twee?",
        "hallo": "O! Hallo! Ik ben Aang-guh. En dat grote harige ding achter me is Appa. Hij is een luchtbizon. Een guh-luchtbizon.",
        "brom": "Grrrooommmf.",
        "ogen": "Opeens tintelt er iets in je, van je oortjes tot je tenen. Je ogen beginnen te gloeien...",
        "guhvatar": "Whoa! Je ogen gloeien! Dan ben jij... jij bent de nieuwe Guhvatar!",
        "korra": "...Wacht even. Je heet toch niet Korra?",
        "korra2": "Korra? Nooit van gehoord, njeg. Vergeet dat ik het vroeg.",
        "stokje": "Ik was de vorige Guhvatar. Ik ging even vadsen in de kaasbol... en toen werd het honderd jaar. Het stokje is nu van jou.",
        "verhaal": "Maar eerst moet je weten wat er gebeurde terwijl ik sliep. Ga zitten. Het is een episch verhaal.",
    }, {"aang": "Aang-guh", "appa": "Appa-guh"}),
    "intro": ("Water. Aarde. Vuur. Lucht.", {
        "water": "Water.",
        "aarde": "Aarde.",
        "vuur": "Vuur.",
        "lucht": "Lucht.",
        "vroeger": "Lang geleden leefden de vier volkeren samen in vadsigheid.",
        "trek": "Maar alles veranderde toen de Vuurnatie trek kreeg...",
        "guhvatar": "Eén guh kon alle vier de elementen sturen en zorgde dat elke knabbel eerlijk gedeeld werd: de Guhvatar.",
        "slaap": "Maar juist toen de wereld hem het hardst nodig had, rolde hij zich op in een kaasbol. Voor een dutje van honderd jaar.",
        "mikai": "Vuurheer Mikai at en at. En hoe meer hij at, hoe hongeriger de wereld werd.",
        "jij": "Nu is de kaasbol open. Het stokje ligt bij een nieuwe Guhvatar. Bij jou.",
        "jipjip": "Kom! Appa brengt ons naar mijn oude thuis, de Zuidelijke Luchttempel. Daar leer je luchtsturen. Jip jip!",
    }, {"aang": "Aang-guh"}),
    "aankomst": ("Aankomst bij de Luchttempel", {
        "tempel": "De Zuidelijke Luchttempel. Wit, stil en hoger dan de wolken.",
        "thuis": "Thuis! Nou ja... mijn oude thuis. Het is hier zo stil zonder de monniken. Kom, eerst naar de Hal van de Guhvatars.",
    }, {"aang": "Aang-guh"}),
    "hal": ("De Hal van de Guhvatars", {
        "beelden": "In de hal staan de beelden van alle guhs die vóór jou de Guhvatar waren. Hun ogen lijken je te volgen.",
        "roku1": "Welkom, jonge Guhvatar. Ik ben Roku-guh. Lang geleden stond ik waar jij nu staat, met net zulke knikkende knieën.",
        "kyoshi1": "En ik ben Kyoshi-guh. Eén ding vooraf: één element is niet genoeg.",
        "roku2": "Lucht, water, aarde en vuur. Leer ze alle vier, in die volgorde. Alleen zo kun je Vuurheer Mikai stoppen.",
        "kyoshi2": "En vergeet niet te eten. Een hongerige Guhvatar redt niemand. Dat weet ik uit ervaring.",
        "voetstuk": "In het midden van de hal staat nog één voetstuk leeg. Het lijkt op iemand te wachten.",
    }, {"roku": "Roku-guh", "kyoshi": "Kyoshi-guh"}),
    "momo": ("Momo-guh", {
        "gevangen": "In zijn nestje onder het dak laat het lemurguhtje zich eindelijk vangen.",
        "kwiek": "Kwiek? ...Kwiek!",
        "terug": "Hij legt je knabbel netjes terug in je hand. Dan kruipt hij op je schouder en blijft daar zitten.",
        "naam": "Hij heeft jou gekozen. Vanaf nu heet hij Momo-guh, en hij gaat overal met je mee.",
    }, {"momo": "Lemurguhtje"}),
    "zuka_steiger": ("Zuka", {
        "boem": "BOEM! Een Vuurnatie-schip ramt de steiger. Planken vliegen in het rond.",
        "eindelijk": "Eindelijk... de Guhvatar.",
        "eer": "Drie jaar heb ik gezocht. Als ik jou meeneem naar mijn vader, krijg ik mijn eer terug!",
        "wie": "Prins Zuka. Verbannen, boos, en heel erg vastberaden. Zijn mannen springen achter hem aan op de steiger.",
        "pak": "Pak de Guhvatar! Maar zachtjes, hè. Vader wil hem heel hebben.",
        "rennen": "Appa wacht in het kamp aan de andere kant van het terras. Rennen!",
    }, {"zuka": "Zuka"}),
    "geleerd": ("Luchtsturen geleerd", {
        "eer": "MIJN EER!",
        "blaas": "Zuka's vuurbal vliegt recht op je af. Je zwaait je arm opzij, en de wind gaat mee.",
        "zag_je": "Zag je dat?! Je blies die vuurbal zo opzij! Dat is luchtsturen!",
        "geleerd": "Lucht is licht en vrij. Net als een guh na het eten. Je hebt het geleerd, Guhvatar.",
        "brom": "Grrroooom!",
        "volgende": "Op naar de Noordelijke Waterstam! Daar leert iemand ons watersturen. Jip jip!",
    }, {"aang": "Aang-guh", "appa": "Appa-guh", "zuka": "Zuka"}),
    "knipoog_sjokkel": ("Sjokkel op de baan", {
        "sjokkel": "Op de luchtscooter-baan is Sjokkel ook aan de race begonnen. Hij heeft er alle tijd voor.",
    }, {}),
}

# =====================================================================================================================
# everything else
# =====================================================================================================================
TEXTS = {
    # --- the kaasbol ------------------------------------------------------------------------------------------------------
    "entity.guhs.kaasbol": "Kaasbol",
    f"{P}.kaasbol.melding": "Bij de Knabbelberg drijft iets gloeiends aan... Een kaasbol?",
    f"{P}.kaasbol.klik": "De kaasbol is warm en zoemt zachtjes. Er klopt iets van binnen!",
    f"{P}.vlucht.mislukt": "Appa kan nog niet vertrekken: de wind weet de weg naar de Luchttempel nog niet. Probeer het zo nog eens, njeg.",
    f"{P}.vlucht.fluit": "Appa landt. Ben je eraf gestapt? Fluit hem met je Appa-fluitje, dan vliegt hij je verder.",
    f"{P}.doel.kaasbol": "De gloeiende kaasbol",
    f"{P}.doel.tempel": "De Zuidelijke Luchttempel",
    f"{P}.doel.waterstam": "De Noordelijke Waterstam",
    # --- the rewards --------------------------------------------------------------------------------------------------------
    f"{P}.beloning.luchtsturen": "Luchtsturen (in de tempel: luchtbal en luchtsprong)",
    f"{P}.nodig.appels": "Appels voor Appa",
    f"{P}.beloning.kreeg": "Je krijgt het luchtnomadengewaad met de pijl en twee luchtscooters voor je guhs!",
    f"{P}.kracht.luchtbal": "Luchtbal",
    f"{P}.kracht.luchtsprong": "Luchtsprong",
    f"{P}.kracht.waai": "Woesj!",
    # --- the roles at the temple ------------------------------------------------------------------------------------------
    f"{Q}.aang.welkom": "Daar ben je! De Hal van de Guhvatars is de grote trap op, recht vooruit.",
    f"{Q}.aang.hal": "De Hal is de grote trap op. Roku-guh en Kyoshi-guh wachten op je. Nou ja, hun geesten. Niet schrikken als ze praten.",
    f"{Q}.aang.luchtbal": "Luchtbal! Guhatso staat op het taartenterras, west van de hal. Ik ga alvast op mijn paal staan. Ik waarschuw je: ik ben er goed in, njeg.",
    f"{Q}.aang.scooter": "De luchtscooter-baan begint zuidwest van het voorplein. Rondjes rennen op een bal van lucht: het allerleukste wat er is!",
    f"{Q}.aang.zweef": "Klim via de noordelijke torens en hun bruggen naar het zuidbalkon van de grote toren. Spring, en vertrouw de wind.",
    f"{Q}.aang.momo": "Een lemurguhtje heeft je knabbel gegrist? Haha! Die wonen hier overal. Hij vloog naar de keuken van de monnikentoren.",
    f"{Q}.aang.stal": "Appa's oude stal is tussen de twee noordelijke torens. Zijn zadel is kapot, en hij heeft honderd jaar geen appel gehad.",
    f"{Q}.aang.deur": "Hoor je dat getik oost van de hal? Daar woonde vroeger niemand. Ga eens kijken!",
    f"{Q}.aang.steiger": "Er ligt iets aan de steiger. Een schip? Hier komen nooit schepen...",
    f"{Q}.aang.vlucht": "Niet praten: rennen! Appa wacht in het kamp!",
    f"{Q}.aang.na.0": "Luchtsturen is net als vadsen: je moet het niet te hard proberen.",
    f"{Q}.aang.na.1": "Kom je straks nog luchtballen? Ik heb stiekem geoefend.",
    f"{Q}.aang.na.2": "Het is hier minder stil sinds jij er was. Fijn, njeg.",
    f"{Q}.guhatso.vroeg": "Ha! Een nieuw gezicht. Of een oud gezicht in een nieuw lijf? Kom terug als je de Hal hebt gezien.",
    f"{Q}.guhatso.luchtbal": "Ik ben monnik Guhatso. Nou ja: de herinnering aan hem. Aang-guh was mijn beste leerling. Luchtbal is simpel: "
                            "sla de bal door het doel van Aang-guh, aan de overkant van het veld. Spring van paal naar paal. Wie in de wolk valt, "
                            "loopt er gewoon weer uit. Eerst tot drie!",
    f"{Q}.guhatso.scooter": "Mooi gespeeld! Nu de luchtscooter-baan. Ren de hele baan rond: de wind duwt je. De start is zuidwest van het voorplein.",
    f"{Q}.guhatso.zweef": "En nu het mooiste: zweven. Het zuidbalkon van de grote toren. Spring, en vlieg door de ringen. Minstens tien!",
    f"{Q}.guhatso.later": "De wind vergeet niets, jonge Guhvatar. Ik ook niet. Vooral niet waar de taarten staan.",
    f"{Q}.guhatso.taart": "Taart? Taart! Hier, vang!",
    f"{Q}.guhatso.gewonnen": "Ha! Drie tegen minder dan drie! Aang-guh, je bent verslagen door de nieuwe Guhvatar. Taart voor iedereen!",
    f"{Q}.roku.raad.0": "Lucht leert je loslaten. Water leert je meebewegen. Aarde leert je staan. En vuur... leert je voorzichtig zijn.",
    f"{Q}.roku.raad.1": "Ik was ooit te laat om een vuur te stoppen. Wees jij op tijd, Guhvatar.",
    f"{Q}.roku.raad.2": "Een Guhvatar is niet sterk omdat hij alles kan. Hij is sterk omdat hij niet alleen is.",
    f"{Q}.kyoshi.raad.0": "Wees aardig. Maar laat je knabbels nooit zomaar afpakken.",
    f"{Q}.kyoshi.raad.1": "Een waaier is ook een wapen. En een prima manier om je thee af te koelen.",
    f"{Q}.kyoshi.raad.2": "Ik was de langste Guhvatar ooit. Dat zegt niets. Behalve dat ik overal bij kon.",
    # --- the luchtbal match ------------------------------------------------------------------------------------------------
    f"{P}.optie.klaar": "Ik ben er klaar voor!",
    f"{P}.optie.straks": "Straks",
    f"{P}.luchtbal.start": "Luchtbal! Sla de bal (linksklik) door het doel van Aang-guh. Eerst tot drie!",
    f"{P}.luchtbal.stand": "Jij %s - %s Aang-guh",
    f"{P}.luchtbal.jij": "GOAL! De bal zoeft door het doel van Aang-guh!",
    f"{P}.luchtbal.aang": "Aang-guh scoort! \"Hihi, sorry!\"",
    f"{P}.luchtbal.blaast": "Aang-guh blaast de bal jouw kant op!",
    f"{P}.luchtbal.verloren": "Aang-guh wint deze keer. Praat met Guhatso voor een nieuwe wedstrijd, njeg.",
    f"{P}.luchtbal.weg": "Je liep van het veld: de wedstrijd stopt. Praat met Guhatso om opnieuw te beginnen.",
    # --- the scooter run ------------------------------------------------------------------------------------------------------
    f"{P}.scooter.klaar": "De luchtscooter-baan! Klaar voor de start...",
    f"{P}.scooter.tel": "%s...",
    f"{P}.scooter.zoef": "ZOEEEF!",
    f"{P}.scooter.stand": "Poortje %s van %s - nog %s seconden",
    f"{P}.scooter.te_laat": "De tijd is om! Terug naar de start, njeg.",
    f"{P}.scooter.van_de_baan": "Van de baan geblazen! Terug naar de start.",
    f"{P}.scooter.gehaald": "Gehaald in %s seconden! Guhatso klapt in zijn doorzichtige handjes.",
    # --- the glide rings ------------------------------------------------------------------------------------------------------
    f"{P}.zweef.balkon": "Spring van het balkon: je zweeft! Kijk waar je heen wilt.",
    f"{P}.zweef.ring": "Ring %s van %s!",
    f"{P}.zweef.te_weinig": "Je zweefde door %s ringen. Tien moeten het er zijn: klim nog eens naar het balkon.",
    f"{P}.zweef.goed": "VAHOEG! %s ringen! Jij kunt zweven.",
    # --- Momo -----------------------------------------------------------------------------------------------------------------
    f"{P}.momo.jat": "Hé! Een klein vleermuis-lemurguhtje grist je knabbel uit je hand en fladdert weg!",
    f"{P}.momo.keuken": "Het lemurguhtje vloog naar de keuken van de monnikentoren!",
    f"{P}.momo.wacht": "Kwiek! Het lemurguhtje wacht even op je.",
    f"{P}.momo.weg": "Zoef! Daar gaat hij weer!",
    # --- Appa's stable --------------------------------------------------------------------------------------------------------
    f"{P}.stal.appa_komt": "Appa sjokt zijn oude stal binnen en snuffelt aan alles.",
    f"{P}.stal.zadel": "Je maakt het zadel een stukje heel (%s/3).",
    f"{P}.stal.zadel_klaar": "Het zadel is weer heel! Appa kijkt er goedkeurend naar.",
    f"{P}.stal.appels": "Je pakt drie appels uit de ton.",
    f"{P}.stal.voer": "Appa smikkelt een appel op (%s/3). Grommm!",
    f"{P}.stal.klaar": "Appa is blij: een heel zadel en een volle buik. Hij likt je hele hoofd af. Bah. En: aww.",
    # --- the closed door ------------------------------------------------------------------------------------------------------
    f"{P}.deur.klop": "Je klopt op de dichte deur...",
    f"{P}.deur.werktuigguh": "Een stem achter de deur: \"Nog niet af, njeg! Kom later maar terug. Veel later!\"",
    f"{P}.deur.gong": "In de verte slaat de windgong. Er ligt iets aan de steiger!",
    # --- the escape -----------------------------------------------------------------------------------------------------------
    f"{P}.ontsnap.start": "Ren naar Appa in het kamp! Ontwijk de vuurballen en Zuka's mannen.",
    f"{P}.ontsnap.geraakt": "Woeps! Weggeduwd (%s/4)",
    f"{P}.ontsnap.terug": "Zuka's mannen zetten je terug aan het begin van de steiger. Nog een keer, njeg!",
    f"{P}.ontsnap.geblazen": "Je blaast ze opzij!",
    # --- the Koningguh, about the new way into the Guheinde ---------------------------------------------------------------
    f"{Q}.koning.oog": "O, en grillspiespoeder? Dat maal je van een grillspies van de Vonk-Mika's in de Spiesburcht, in de "
                       "Barbecuether. Eén spies, twee porties. Zo. Nu weet je alles, njeg.",
    # --- the visible advancement ----------------------------------------------------------------------------------------------
    "advancements.guhs.guheinde.einde2_h1_lucht_geleerd.title": "Luchtsturen!",
    "advancements.guhs.guheinde.einde2_h1_lucht_geleerd.description": "Leer luchtsturen in de Zuidelijke Luchttempel",
    # --- the toy and the clothes ----------------------------------------------------------------------------------------------
    "block.guhs.luchtscooter": "Luchtscooter",
    "block.guhs.luchtscooter.lore": "Speelgoed voor je guhs: een draaiende bal van lucht. Zoeeef, rondjes!",
    "gui.guhs.huisje.overzicht.ding.luchtscooter": "Guhs rijden zoeeef rondjes op de luchtscooter.",
    "item.guhs.guhvatar_luchtnomade": "Luchtnomadengewaad",
    "gui.guhs.kledingbron.einde2_h1_lucht": "De Guhvatar: Lucht",
    # --- the luchtbal ball --------------------------------------------------------------------------------------------------
    "entity.guhs.einde2_h1_lucht_luchtbal": "Luchtbal",
}

# =====================================================================================================================
# FTB: the section "De Guhvatar: Lucht" (one quest per step; quest i is done when step i + 1 is reached)
# =====================================================================================================================
FTB_TITEL = "Hoofdstuk 1: De Zuidelijke Luchttempel"
FTB = [
    ("einde2_h1_lucht_kaasbol", "De kaasbol", "Wie Opper-Mika heeft verslagen, ziet bij de Knabbelberg een &6gloeiende kaasbol&r aanspoelen. "
     "Tik hem open en kijk wie erin zat.", "guhs:oog_van_vadsig", 0),
    ("einde2_h1_lucht_vlucht", "Jip jip!", "Vlieg op &dAppa-guh&r naar de &bZuidelijke Luchttempel&r. Je hoeft niet te zoeken: hij weet de weg.",
     "guhs:appa_fluitje", 1),
    ("einde2_h1_lucht_hal", "De Hal van de Guhvatars", "In de hal staan de beelden van alle Guhvatars vóór jou. Twee van hen hebben je iets te vertellen.",
     "minecraft:chiseled_quartz_block", 2),
    ("einde2_h1_lucht_luchtbal", "Luchtbal", "Speel luchtbal tegen &dAang-guh&r op de palen. Guhatso is scheidsrechter. Eerst tot drie!",
     "minecraft:snowball", 3),
    ("einde2_h1_lucht_scooter", "De luchtscooter-baan", "Race de hele baan rond de tempel rond voordat de tijd om is. Zoeeef!",
     "guhs:luchtscooter", 4),
    ("einde2_h1_lucht_zweef", "Zweven", "Spring van de grote toren en zweef door de ringen. Minstens tien van de veertien.",
     "minecraft:feather", 5),
    ("einde2_h1_lucht_momo", "Achter Momo aan", "Een lemurguhtje grist je knabbel. Volg hem door de hele tempel, de torens op en over de bruggen.",
     "guhs:kaas_knabbels", 6),
    ("einde2_h1_lucht_stal", "Appa's stal", "Maak het oude zadel heel en voer Appa drie appels. Een blije Appa is een vliegende Appa.",
     "minecraft:apple", 7),
    ("einde2_h1_lucht_deur", "Nog niet af", "Achter een dichte deur knutselt iemand. Wie? Dat hoor je wel. Of eigenlijk: dat hoor je nog niet.",
     "guhs:werkplaatsdeur", 8),
    ("einde2_h1_lucht_zuka", "Eindelijk, de Guhvatar", "Een Vuurnatie-schip ramt de steiger. Ontsnap op Appa en leer luchtsturen: "
     "het eerste van de vier elementen!", "guhs:luchtscooter", 9),
]

# =====================================================================================================================
# the pier's signs (einde2_bouw_lucht chunk c59; the texts are written by the build, these are only for the English)
# =====================================================================================================================
BORDJES = ["Steiger", "Alleen voor", "vriendelijke", "schepen, njeg.", "Niet rammen.", "Echt niet.", "Ook niet als je", "een prins bent."]
