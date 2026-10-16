"""
einde2 phase B, slice h1_lucht: every Dutch text of chapter 1 of the Guhvatar story (the source; the English overlay is
tools/lang/en/c92_e2_h1_lucht.json). Read by einde2_h1_lucht.build(h).

  LIJN        the questline guhvatar_lucht (verhaal_motor.verhaallijn: name, explanation, the ten steps, the variant 9_vlucht)
  KAART       the narrator card of the chapter (verhaal_motor.vertelkaart)
  SCENES      the eight cutscenes: {naam: (titel, {key: tekst}, {acteur: naam})} (Java: H1Scenes; scene.guhs.einde2_h1_lucht_<naam>.*)
  HINTS/KIJK  fix round 1: the step hints and their look scenes (H1Hints); LES: the airbending lesson
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
        # fix round 1 (playtest 1): every step says WHAT to do and WHERE, with a direction and a landmark; the spot of the step
        # also shines (a column of light) and the compass points at it
        ("De Hal van de Guhvatars", "Loop vanaf de landingsplek de grote trap op naar het noorden, door de tempelpoort en over het "
         "plein. Ga de grote witte Hal met de hoge toren erop binnen. Rechts (oost) staan de geesten van Roku-guh en Kyoshi-guh "
         "voor hun beelden, met een lichtstraal boven hun hoofd: praat met een van hen.",
         "De grote witte Hal met de hoge toren erop, noord van de tempelpoort"),
        ("Luchtbal", "Loop de Hal uit en ga naar het westen. Monnik Guhatso staat bij de taarten op het terras boven het luchtbalveld "
         "(het veld vol palen). Praat met hem en speel luchtbal tegen Aang-guh: spring van paal naar paal en sla de gloeiende bal "
         "met linksklik naar het doel van Aang-guh. Eerst tot drie!",
         "Het taartenterras boven het luchtbalveld, west van de Hal"),
        ("De luchtscooter-baan", "Loop vanaf het luchtbalveld naar het zuiden, tot aan de rand van de tempel. Daar staat de "
         "oranje-gele startboog met een lichtstraal en dwarrelende wolkjes. Stap op de zwart-witte streep onder de boog: je krijgt "
         "een luchtscooter en na 3-2-1 rij je weg. Kijk waar je heen wilt, W gaat sneller. Rij door alle poortjes naar de finish.",
         "De oranje-gele startboog, zuid van het luchtbalveld (de zuidwesthoek van de tempel)"),
        ("De zweefringen", "De wind brengt je naar het zuidbalkon boven in de hoge toren op het dak van de Hal (daarna kan het ook "
         "met de windzuil midden op het plein voor de Hal). Spring van het balkon: je zweeft en zakt heel langzaam. Kijk waar je "
         "heen wilt en vlieg door de zes ringen; de volgende ring licht blauw op. Eentje missen mag.",
         "Het zuidbalkon boven in de hoge toren op de Hal (windzuil: midden op het plein voor de Hal)"),
        ("Achter Momo aan!", "Een vleermuis-lemurguhtje heeft je knabbel gegrist! Hij fladdert gloeiend vlak voor je en laat een "
         "spoor van wolkjes achter. Loop hem achterna: naar de monnikentoren in het noordwesten, de toren in en omhoog, en over de "
         "daken. Blijf je achter, dan wacht hij op je.",
         "Volg het gloeiende lemurguhtje (eerst naar de monnikentoren, noordwest)"),
        ("Appa's zadel", "Ga naar Appa's stal: het grote witte gebouw met het groene dak tussen de twee torens in het noorden, "
         "achter de Hal (de deur is aan de zuidkant). Pak rechts van de deur het gloeiende zadel van de houten bok (rechtsklik), klik er daarna mee op "
         "Appa, en geef hem drie appels uit de ton links van de deur (rechtsklik op Appa met een appel).",
         "Appa's stal, tussen de twee noordelijke torens (achter de Hal)"),
        ("De dichte deur", "Er tikt iets achter een dichte deur. Loop naar het lange lage gebouw ten oosten van de Hal: de "
         "werkplaats. De grote dichte deur zit in de westmuur, onder de lantaarn en de lichtstraal. Loop ertegenaan of klik erop.",
         "De deur in de westmuur van de werkplaats, ten oosten van de Hal"),
        ("Zuka", "De windgong luidt: er ligt iets aan de steiger! Loop naar het zuiden, door de tempelpoort de grote trap af naar "
         "het landingsterras waar je met Appa aankwam. De houten steiger steekt daar aan de oostkant de leegte in (lichtstraal).",
         "De houten steiger aan de oostkant van het landingsterras (zuid, onder aan de trap)"),
    ],
    klaar=("Luchtsturen geleerd! Appa vliegt je naar de Noordelijke Waterstam.", "Op weg naar de Noordelijke Waterstam"),
    extra={"9_vlucht": ("Ontsnap! Ren over het landingsterras naar het westen, naar Appa bij het kampvuur in het kamp "
                        "(lichtstraal). Ontwijk Zuka's vuurballen en zijn mannen.",
                        "Het kamp met het kampvuur, west op het landingsterras")},
    kort={"0": "Open de gloeiende kaasbol", "1": "Vlieg met Appa naar de Luchttempel", "2": "Naar de Hal, praat met een geest",
          "3": "Luchtbal: Guhatso, west van de Hal", "4": "Startboog zuid van het luchtbalveld", "5": "Spring van het balkon, 6 ringen",
          "6": "Volg het gloeiende lemurguhtje", "7": "Zadel op Appa, drie appels", "8": "De deur van de werkplaats (oost)",
          "9": "Naar de steiger (zuid, beneden)", "9_vlucht": "Ren naar Appa in het kamp (west)!",
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
    # (fix round 1: the compass points at the spot of each step)
    f"{P}.doel.stap.2": "De geesten in de Hal van de Guhvatars",
    f"{P}.doel.stap.3": "Monnik Guhatso op het taartenterras",
    f"{P}.doel.stap.4": "De startboog van de luchtscooter-baan",
    f"{P}.doel.stap.5": "De windzuil naar het balkon",
    f"{P}.doel.stap.6": "De monnikentoren",
    f"{P}.doel.stap.7": "Appa's zadel in de stal",
    f"{P}.doel.stap.8": "De dichte deur van de werkplaats",
    f"{P}.doel.stap.9": "De steiger",
    f"{P}.doel.stap.9_vlucht": "Appa in het kamp",
    # --- the rewards --------------------------------------------------------------------------------------------------------
    f"{P}.beloning.luchtsturen": "Luchtsturen (in de tempel: luchtbal en luchtsprong)",
    f"{P}.nodig.appels": "Appels voor Appa",
    f"{P}.beloning.kreeg": "Je krijgt het luchtnomadengewaad met de pijl en twee luchtscooters voor je guhs!",
    f"{P}.kracht.luchtbal": "Luchtbal",
    f"{P}.kracht.luchtsprong": "Luchtsprong",
    f"{P}.kracht.waai": "Woesj!",
    # --- the roles at the temple ------------------------------------------------------------------------------------------
    f"{Q}.aang.welkom": "Daar ben je! De Hal van de Guhvatars is recht vooruit: door de poort, over het plein, de grote witte hal met de toren erop.",
    f"{Q}.aang.hal": "De Hal is recht vooruit, het grote witte gebouw met de toren erop. Binnen rechts wachten Roku-guh en Kyoshi-guh. Nou ja, hun geesten. Niet schrikken als ze praten.",
    f"{Q}.aang.luchtbal": "Luchtbal! Guhatso staat bij de taarten op het terras, west van de Hal, naast het veld met de palen. Ik ga alvast op mijn paal staan. Ik waarschuw je: ik ben er goed in, njeg.",
    f"{Q}.aang.scooter": "De luchtscooter-baan begint bij de oranje-gele boog, zuid van het luchtbalveld, aan de rand. Stap op de streep en je staat op een bal van lucht: het allerleukste wat er is!",
    f"{Q}.aang.zweef": "Naar het balkon boven in de hoge toren? Stap in de windzuil midden op het plein voor de Hal: de wind tilt je erheen. Spring, en vertrouw de wind.",
    f"{Q}.aang.momo": "Een lemurguhtje heeft je knabbel gegrist? Haha! Die wonen hier overal. Kijk, daar fladdert hij, dat gloeiende ding! Loop hem maar achterna.",
    f"{Q}.aang.stal": "Appa's oude stal is het witte gebouw met het groene dak tussen de twee noordelijke torens. Het zadel ligt rechts van de deur, de appels staan links. Hij heeft honderd jaar geen appel gehad!",
    f"{Q}.aang.deur": "Hoor je dat getik? Het komt uit de werkplaats, het lange gebouw ten oosten van de Hal. De deur zit in de westmuur. Ga eens kijken!",
    f"{Q}.aang.steiger": "Er ligt iets aan de steiger, beneden op het landingsterras aan de oostkant. Een schip? Hier komen nooit schepen...",
    f"{Q}.aang.vlucht": "Niet praten: rennen! Appa wacht in het kamp!",
    f"{Q}.aang.na.0": "Luchtsturen is net als vadsen: je moet het niet te hard proberen.",
    f"{Q}.aang.na.1": "Kom je straks nog luchtballen? Ik heb stiekem geoefend.",
    f"{Q}.aang.na.2": "Het is hier minder stil sinds jij er was. Fijn, njeg.",
    f"{Q}.guhatso.vroeg": "Ha! Een nieuw gezicht. Of een oud gezicht in een nieuw lijf? Kom terug als je de Hal hebt gezien.",
    f"{Q}.guhatso.luchtbal": "Ik ben monnik Guhatso. Nou ja: de herinnering aan hem. Aang-guh was mijn beste leerling. Luchtbal is simpel: "
                            "de gloeiende bal zweeft boven de palen en blijft altijd boven een paal hangen. Spring naar een paal ernaast en sla hem "
                            "met linksklik naar het doel van Aang-guh, aan de noordkant van het veld. Wie in de wolk valt, zet de wind meteen terug "
                            "op een paal. Eerst tot drie!",
    f"{Q}.guhatso.scooter": "Mooi gespeeld! Nu de luchtscooter-baan. Loop naar het zuiden, naar de oranje-gele boog aan de rand van de tempel, en stap op de streep. "
                            "Je rijdt op een bal van lucht: kijk waar je heen wilt, W gaat sneller, spatie springt.",
    f"{Q}.guhatso.zweef": "En nu het mooiste: zweven. Spring van het zuidbalkon boven in de hoge toren en vlieg door de zes ringen. De windzuil midden op het plein "
                          "voor de Hal brengt je naar boven.",
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
    f"{P}.luchtbal.start": "Luchtbal! De gloeiende bal zweeft boven de palen. Spring naar een paal ernaast en sla hem (linksklik) naar het doel van Aang-guh, aan de overkant. Eerst tot drie!",
    f"{P}.luchtbal.titel": "Luchtbal!",
    f"{P}.luchtbal.titel_onder": "Sla de bal (linksklik) naar het doel aan de overkant",
    f"{P}.luchtbal.terug_op_paal": "Floep! De wind zet je terug op een paal.",
    f"{P}.luchtbal.bal_terug": "De wind brengt de bal terug, vlak bij jou.",
    f"{P}.luchtbal.schiet": "Aang-guh schiet op jouw doel! Hou hem tegen!",
    f"{P}.luchtbal.gewonnen": "Gewonnen!",
    f"{P}.luchtbal.stand": "Jij %s - %s Aang-guh",
    f"{P}.luchtbal.jij": "GOAL! De bal zoeft door het doel van Aang-guh!",
    f"{P}.luchtbal.aang": "Aang-guh scoort! \"Hihi, sorry!\"",
    f"{P}.luchtbal.blaast": "Aang-guh blaast de bal terug naar jou!",
    f"{P}.luchtbal.verloren": "Aang-guh wint deze keer. Praat met Guhatso voor een nieuwe wedstrijd, njeg.",
    f"{P}.luchtbal.weg": "Je liep van het veld: de wedstrijd stopt. Praat met Guhatso om opnieuw te beginnen.",
    # --- the scooter run ------------------------------------------------------------------------------------------------------
    f"{P}.scooter.klaar": "Je staat op een luchtscooter! Kijk waar je heen wilt, W gaat sneller, S remt, spatie springt. Rij door alle poortjes naar de finish.",
    f"{P}.scooter.stap_op": "Hier start de luchtscooter-baan: stap op de zwart-witte streep onder de boog!",
    f"{P}.scooter.uitleg": "Kijk waar je heen wilt - W is sneller",
    f"{P}.scooter.zoef": "ZOEEEF!",
    f"{P}.scooter.stand": "Poortje %s van %s - nog %s seconden",
    f"{P}.scooter.te_laat": "De tijd is om! Geen nood: stap weer op de streep onder de startboog en probeer het nog eens.",
    f"{P}.scooter.afgestapt": "Je stapte van de luchtscooter. Stap weer op de streep onder de startboog om opnieuw te rijden.",
    f"{P}.scooter.terug_op_baan": "Woesj! De wind zet je terug op de baan.",
    f"{P}.scooter.finish": "FINISH!",
    f"{P}.scooter.tijd": "Gehaald in %s seconden",
    f"{P}.scooter.gehaald": "Gehaald in %s seconden! Guhatso klapt in zijn doorzichtige handjes.",
    # --- the glide rings ------------------------------------------------------------------------------------------------------
    f"{P}.zweef.balkon": "Spring van het balkon: je zweeft en zakt langzaam. Kijk naar de blauwe ring!",
    f"{P}.zweef.je_zweeft": "Je zweeft! Kijk waar je heen wilt. Omlaag kijken gaat sneller.",
    f"{P}.zweef.ring": "Ring %s van %s!",
    f"{P}.zweef.gemist": "Net ernaast! Geen nood: op naar de volgende blauwe ring.",
    f"{P}.zweef.te_weinig": "Je zweefde door %s ringen. Geen nood: de wind zet je zo terug op het balkon, dan probeer je het nog eens.",
    f"{P}.zweef.goed": "VAHOEG! %s ringen! Jij kunt zweven.",
    f"{P}.zweef.gehaald": "Gehaald!",
    f"{P}.zweef.gehaald_onder": "%s ringen. Jij kunt zweven!",
    f"{P}.zweef.wind_komt": "Guhatso: \"En nu: zweven!\" De wind tilt je zo op naar het balkon van de hoge toren...",
    f"{P}.zweef.op_balkon": "Je staat op het zuidbalkon boven in de hoge toren. Spring eraf: je zweeft en zakt heel langzaam. Kijk waar je heen wilt en vlieg door de zes ringen; de volgende licht blauw op.",
    f"{P}.zweef.titel": "Spring en zweef!",
    f"{P}.zweef.titel_onder": "Vlieg door de blauwe ringen",
    f"{P}.zweef.zuil": "Woesj! De windzuil tilt je op naar het balkon.",
    f"{P}.zweef.neergezet": "De wind zet je zachtjes neer op het plein.",
    # --- Momo -----------------------------------------------------------------------------------------------------------------
    f"{P}.momo.jat": "Hé! Een klein vleermuis-lemurguhtje grist je knabbel uit je hand en fladdert weg!",
    f"{P}.momo.volg": "Volg het gloeiende lemurguhtje! Hij laat een spoor van wolkjes achter en wacht op je als je achterblijft.",
    f"{P}.momo.daar": "Daar is hij! Het gloeiende lemurguhtje, vlak voor je. Loop hem achterna!",
    f"{P}.momo.toren": "Hij vliegt de monnikentoren in. Erachteraan, de trap op!",
    f"{P}.momo.wacht": "Kwiek! Het lemurguhtje wacht op je. Kom maar, volg het spoor van wolkjes.",
    f"{P}.momo.weg": "Zoef! Daar gaat hij weer!",
    # --- Appa's stable --------------------------------------------------------------------------------------------------------
    f"{P}.stal.appa_komt": "Appa sjokt zijn oude stal binnen en snuffelt aan alles.",
    f"{P}.stal.zadel_naam": "Appa's zadel",
    f"{P}.stal.zadel_gepakt": "Je pakt Appa's zadel van de bok. Klik er nu mee op Appa!",
    f"{P}.stal.zadel_klaar": "Het zadel zit! Appa kijkt er goedkeurend naar.",
    f"{P}.stal.zadel_zit_al": "Het zadel zit al op Appa.",
    f"{P}.stal.doe_zadel": "Pak het gloeiende zadel van de houten bok, rechts van de deur (rechtsklik).",
    f"{P}.stal.doe_op_appa": "Klik met het zadel in je hand op Appa.",
    f"{P}.stal.doe_appels": "Pak appels uit de ton links van de deur (rechtsklik).",
    f"{P}.stal.doe_voeren": "Klik met een appel in je hand op Appa.",
    f"{P}.stal.vol": "Appa zit vol. Drie appels is genoeg.",
    f"{P}.stal.titel": "Appa is blij!",
    f"{P}.stal.titel_onder": "Zadel op, buik vol",
    f"{P}.stal.appels": "Je pakt appels uit de ton. Klik er nu mee op Appa!",
    f"{P}.stal.voer": "Appa smikkelt een appel op (%s/3). Grommm!",
    f"{P}.stal.klaar": "Appa is blij: een heel zadel en een volle buik. Hij likt je hele hoofd af. Bah. En: aww.",
    # --- the closed door ------------------------------------------------------------------------------------------------------
    f"{P}.deur.hier": "De werkplaats! De dichte deur zit in de westmuur, onder de lantaarn. Loop ertegenaan.",
    f"{P}.deur.titel": "Nog niet af, njeg!",
    f"{P}.deur.titel_onder": "Er ligt iets aan de steiger...",
    f"{P}.deur.klop": "Je klopt op de dichte deur...",
    f"{P}.deur.werktuigguh": "Een stem achter de deur: \"Nog niet af, njeg! Kom later maar terug. Veel later!\"",
    f"{P}.deur.gong": "In de verte slaat de windgong. Er ligt iets aan de steiger: beneden op het landingsterras, aan de oostkant!",
    # --- the escape -----------------------------------------------------------------------------------------------------------
    f"{P}.ontsnap.start": "Ren naar het westen, naar Appa in het kamp bij het kampvuur! Ontwijk de vuurballen en Zuka's mannen.",
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
# fix round 1 (playtest 1: "ietsjes meer hints"; kern's StapHint, merge_fix1): one hint per step inside the temple (H1Hints).
# HINTS: the chat line after the look scene (%s = the direction, "in het noorden, een eindje verder"); it says WHERE, the
# step's own nu text and fix-h1's lines on the spot say WHAT. KIJK: the look scenes einde2_h1_lucht_kijk_<naam>, (titel,
# the one run-on caption). LES: Aang-guh's airbending lesson after the luchtbal match.
# =====================================================================================================================
HINTS = {
    "hal": "De geesten van Roku-guh en Kyoshi-guh wachten %s, in de grote witte Hal: de trap op, door de poort, over het plein.",
    "luchtbal": "Monnik Guhatso staat %s, bij de taarten boven het luchtbalveld. Praat met hem.",
    "scooter": "De oranje-gele startboog staat %s, aan de zuidrand van de tempel. Stap op de streep eronder.",
    "zweven": "De eerste ring hangt %s. Daarna licht steeds de volgende ring blauw op.",
    "momo": "Het lemurguhtje vlucht naar de monnikentoren, %s. Kwijt? De deur zit aan de zuidkant van de toren.",
    "stal": "Appa's stal met het groene dak staat %s. De deur zit aan de zuidkant.",
    "deur": "De dichte deur van de werkplaats is %s, in de westmuur van het lange gebouw oost van de Hal.",
    "zuka": "De steiger is %s: de grote trap af, aan de oostkant van het landingsterras.",
}
KIJK = {
    "hal": ("De Hal van de Guhvatars", "De Hal van de Guhvatars. Twee geesten wachten op je."),
    "luchtbal": ("Monnik Guhatso", "Monnik Guhatso. Hij heeft een spelletje voor je, en taart."),
    "scooter": ("De start van de baan", "Hier begint de luchtscooter-baan."),
    "zweven": ("De eerste ring", "De eerste ring. Spring, en vertrouw de wind."),
    "momo": ("De monnikentoren", "De monnikentoren. Daar wil dat lemurguhtje heen!"),
    "stal": ("Appa's stal", "Appa's oude stal. Hij heeft honger, njeg."),
    "deur": ("De dichte deur", "Daar tikt iets... achter die deur."),
    "zuka": ("De steiger", "De steiger. Daar ligt iets... iets groots."),
}
LES = {
    "1": "Zag je dat? Die bal vloog niet door je hand, maar door de wind die je hem meegaf. Dat was luchtsturen! Een klein beetje, njeg.",
    "2": "Nu zonder bal. Druk op %s, kies Luchtsprong in je krachtwiel en spring op de wind!",
    "3": "Luchtsprong en Luchtbal werken overal in de tempel. Probeer ze maar!",
    "sprong": "Whoa, zag je hoe hoog? Lucht duwt niet, lucht tilt je op. Probeer nu Luchtbal: een windstoot waar je kijkt.",
    "bal": "Woesj! Dat is het! Lucht is licht en vrij. De rest leer je onderweg: rennen, zweven... en ooit misschien een vuurbal wegblazen.",
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
    ("einde2_h1_lucht_scooter", "De luchtscooter-baan", "Stap op de streep onder de startboog en race op een luchtscooter de hele baan rond voordat de tijd om is. Zoeeef!",
     "guhs:luchtscooter", 4),
    ("einde2_h1_lucht_zweef", "Zweven", "Spring van het zuidbalkon boven in de grote toren en zweef door de zes ringen. Eentje missen mag.",
     "minecraft:feather", 5),
    ("einde2_h1_lucht_momo", "Achter Momo aan", "Een gloeiend lemurguhtje grist je knabbel. Volg hem en zijn spoor van wolkjes door de tempel, de torens op en over de bruggen.",
     "guhs:kaas_knabbels", 6),
    ("einde2_h1_lucht_stal", "Appa's stal", "Pak het zadel in Appa's stal, leg het op Appa en voer hem drie appels. Een blije Appa is een vliegende Appa.",
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
