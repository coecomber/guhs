"""
einde2 phase B, slice h4_vuur: every Dutch text of chapter 4 and the finale of the Guhvatar story (the English overlay is
tools/lang/en/c95_e2_h4_vuur.json). Not a feature module: einde2_h4_vuur.py writes these.

Copyright (CONTRACT_E2_B 6.3, DESIGN 0): every line is our own; the well-known moments of the series are told in our own
words and guh-ified. Miroh's lines of DESIGN 5 ("Ik was nooit boos op je..." and "Het lot is een grappig ding,
Guhvatar." and "De mooiste dag van mijn leven... na die keer met de perfecte jasmijnthee.") are approved as written.
"""

Q = "quest.guhs.einde2_h4_vuur."
G = "gui.guhs.einde2_h4_vuur."

# =====================================================================================================================
# the two questlines (verhaal_motor.verhaallijn): (stapnaam, nu, waar) per step
# =====================================================================================================================
VUUR_NAAM = "Hoofdstuk 4: De Vuurnatie"
VUUR_UITLEG = ("De Vuurnatie-hoofdstad, in de krater van een vulkaan van aangebrande kaaskorst. Je komt vermomd binnen, je "
               "leert de Vuurnatie kennen van binnenuit, en op de Dag van de Zwarte Zon kiest Zuka eindelijk zijn eigen weg. "
               "Daarna: goedmaken, vuur leren bij de draken, een heel slecht toneelstuk en een tent vol thee.")
VUUR_STAPPEN = [
    ("Aankomst in de Vuurnatie", "Land in de Vuurnatie-hoofdstad", "Het landingsveld bij de haven"),
    ("Het schooltje", "Ga vermomd naar het Vuurnatie-schooltje en schuif aan in de klas", "Het schooltje in de krater, ten westen van het paleis"),
    ("Het geheime dansfeest", "Dans op het verboden feest: sta op het oplichtende vak als de maat ronddraait",
     "De dansgrot, aan het eind van het steegje naast de school"),
    ("De schildpad-eendguhs", "Voer de schildpad-eendguhs in de paleisvijver (klik op de broodmand)", "De broodmand bij de paleisvijver"),
    ("De Dag van de Zwarte Zon", "Ga naar het strand van de invasie: vandaag gaat de zon uit", "Het strand naast de haven"),
    ("De cel onder het paleis", "Volg Zuka door de bunker naar de cel van Oom Miroh", "De cel naast de hal van de bunker"),
    ("Een gast in het kamp", "Ga terug naar het kamp van het team", "Het kamp naast het landingsveld"),
    ("Goedmaken", "Help Zuka het goed te maken met Toph-guh en Katara-guh", "Het kamp naast het landingsveld"),
    ("Het Kokend Rotsblok", "Neem de gondel naar Het Kokend Rotsblok en bevrijd Suki-guh", "Het bovenstation van de gondel op de kraterrand"),
    ("De Drakendans", "Dans de Drakendans met Zuka bij de Zonnekrijgers", "Het ronde terras boven de ruïne, bovenaan de lange trap"),
    ("Het toneel van Sintelstrand", "Kijk het toneelstuk op Sintelstrand: neem plaats op de derde rij", "Het openluchttheater van Sintelstrand"),
    ("De tent van de Witte Lotus", "Zoek Zuka en Oom Miroh in het kamp van de Witte Lotus", "De grote tent in het kamp van de Witte Lotus"),
]
VUUR_KLAAR = ("Hoofdstuk 4 is af! De komeet komt eraan. Praat met Oom Miroh in zijn tent.", "De tent van de Witte Lotus")
VUUR_EXTRA = {
    "7_katara": ("Toph-guh is om. Nu Katara-guh nog: help Zuka de goede woorden te vinden", "Het kamp naast het landingsveld"),
    "8_binnen": ("Sluip over de binnenplaats naar de cel van Suki-guh. Bukken helpt!", "Het Kokend Rotsblok, de gevangenis op het eiland"),
}
VUUR_KORT = {
    "0": "Land in de Vuurnatie-hoofdstad", "1": "Naar het schooltje (vermomd)", "2": "Dans op het geheime feest", "3": "Voer de schildpad-eendguhs",
    "4": "Naar het invasiestrand", "5": "Volg Zuka naar de cel", "6": "Terug naar het kamp", "7": "Help Zuka met Toph-guh",
    "7_katara": "Help Zuka met Katara-guh", "8": "Naar de gondel van Het Kokend Rotsblok", "8_binnen": "Sluip naar de cel van Suki-guh",
    "9": "Dans de Drakendans", "10": "Naar het theater van Sintelstrand", "11": "Naar de tent van de Witte Lotus", "klaar": "Praat met Oom Miroh",
}

FINALE_NAAM = "De komeet van Mikai"
FINALE_UITLEG = ("De knabbelkomeet komt. Vuurheer Mikai wil met zijn kracht alles opeten wat er te eten valt, voor altijd. "
                 "De vorige Guhvatars geven raad, de Leeuwschildpad-guh leert je iets wat niemand meer kent, en dan: de "
                 "luchtvloot, de Agni Kai, het duel op de rotspilaren en een kroning.")
FINALE_STAPPEN = [
    ("Raad van de voorgangers", "Praat met Oom Miroh in zijn tent", "De tent van de Witte Lotus"),
    ("De Leeuwschildpad-guh", "Rust even uit in de hoofdstad. Er gebeurt iets in de nacht...", "De Vuurnatie-hoofdstad"),
    ("De luchtvloot", "Vlieg op Appa-guh door de brandende luchtvloot van Mikai", "De kade bij de hap uit de kraterrand"),
    ("Agni Kai", "Vlieg naar het voorplein van het paleis", "Het voorplein van het paleis"),
    ("Het duel", "Stop Vuurheer Mikai op de rotspilaren, zonder hem pijn te doen", "De duelarena tussen de rotspilaren"),
    ("De kroning", "Ga naar de kroning op het voorplein", "Het voorplein van het paleis"),
    ("De aftiteling", "Kijk hoe het verhaal afloopt", "Het voorplein van het paleis"),
]
FINALE_KLAAR = ("Je bent de Guhvatar! Haal elke dag een feestje op bij de bakker van de hoofdstad.", "De Vuurnatie-hoofdstad")
FINALE_EXTRA = {
    "0_geest": ("Vlieg met Fang naar de Top der Voorgangers in de Geestenwereld", "Fang's Hemelpad in de Geestenwereld"),
}
FINALE_KORT = {
    "0": "Praat met Oom Miroh", "0_geest": "Vlieg met Fang naar de Top", "1": "Rust uit in de hoofdstad", "2": "Vlieg door de luchtvloot",
    "3": "Naar het voorplein", "4": "Het duel met Vuurheer Mikai", "5": "Naar de kroning", "6": "De aftiteling", "klaar": "Je bent de Guhvatar!",
}

# the narrator card of the chapter (Verteller, 5 lines; its map: einde2_h4_vuur.kaart)
KAART_TITEL = "Hoofdstuk 4: De Vuurnatie"
KAART_REGELS = [
    "Ba Sing Guh is gevallen. Oom Miroh is gevangen. En jij bent nog moe.",
    "Appa-guh brengt het team naar het hart van de Vuurnatie: een hoofdstad in een krater.",
    "Hier heeft Vuurheer Mikai zijn troon, zijn leger en zijn honger.",
    "Vermomd tussen de Mika's wacht je op de Dag van de Zwarte Zon.",
    "Het vuur is het laatste element. Het is ook het moeilijkste. Njeg.",
]

# =====================================================================================================================
# the cutscenes: {naam: (titel, {regel: tekst}, {acteur: naam})}
# =====================================================================================================================
TEAM = {"aang": "Aang-guh", "katara": "Katara-guh", "sokka": "Sokka-guh", "toph": "Toph-guh", "zuka": "Zuka", "miroh": "Oom Miroh",
        "suki": "Suki-guh", "momo": "Momo-guh", "appa": "Appa-guh"}

SCENES = {
    "aankomst": ("Vermomd in de Vuurnatie", {
        "verteller": "De Vuurnatie-hoofdstad. Een krater vol rook, vlaggen en heel veel boze vlammetjes.",
        "plan": "Plan Vermomming! Rode kleren, gouden randjes, en niemand zegt het woord 'Guhvatar'. Niemand. Njeg.",
        "moe": "Jij bent nog moe van Ba Sing Guh. Doe het rustig aan, oké? Wij letten op je.",
        "schoenen": "Rood? Ik zie toch niks. Ik voel alleen dat deze schoenen knellen.",
        "welkom": "Welkom in de Vuurnatie! Ze schijnen hier de lekkerste vuurvlokken te hebben. Denk ik. Hoop ik.",
    }, {k: TEAM[k] for k in ("aang", "sokka", "katara", "toph")}),
    "school": ("Het Vuurnatie-schooltje", {
        "beste": "Goedemorgen klas! Wie weet wie de allerbeste Vuurheer is van de hele geschiedenis?",
        "mikai": "Vuurheer Mikai, juf!",
        "knabbels": "Heel goed. En wie heeft de grootste voorraad knabbels van de hele wereld?",
        "opgegeten": "Ook Vuurheer Mikai, juf! Want hij heeft alle andere al opgegeten.",
        "oren": "Jij daar, nieuwe. Je hoofdband zit scheef. Het lijken wel... oortjes.",
        "fluister": "Psst. Vanavond in de grot. Dansen is verboden. Kom!",
    }, {"juf": "De juf", "leerling_1": "Leerling", "leerling_4": "Leerling", "leerling_5": "Een leerling"}),
    "zwarte_zon": ("De Dag van de Zwarte Zon", {
        "zon": "De Dag van de Zwarte Zon. Midden op de dag schuift de maan voor de zon.",
        "acht_minuten": "Acht minuten lang kan geen enkele Mika vuur maken. Acht minuten. Dat is de kans.",
        "plan": "Iedereen klaar? Dit is het, njeg. Het plan van mijn leven. Ik heb het drie keer opgeschreven.",
        "nu": "Nu of nooit. Naar het paleis!",
        "leeg": "De troonzaal is leeg. Geen wachters. Geen Vuurheer. Alleen het vlammengordijn, gedoofd.",
        "waar": "Waar is iedereen...?",
        "wist": "Op zoek naar mijn vader? Wat schattig. Wij wisten het al weken, Guhvatartje. Hij zit veilig, ver weg. En de zon komt zo terug.",
        "val": "Ze wist het... Het was een val. Iedereen terug naar het strand. Nu!",
        "luister": "Vader. Ik heb je iets te zeggen. En deze keer ga jij luisteren.",
        "durft": "Je durft. Op de enige dag dat ik geen vuur heb, kom jij praten.",
        "weggestuurd": "Je hebt me weggestuurd omdat ik opkwam voor anderen. Dat was niet zwak. Dat was het beste wat ik ooit gedaan heb.",
        "bang": "Ik ben niet meer bang voor je. Ik ga de Guhvatar helpen. En jij... jij gaat leren delen.",
        "les": "De zon is terug, prins. Dan krijg je nu je les.",
        "bliksem": "Mikai schiet een bliksemflits. Zuka vangt hem op, leidt hem door zijn buik, zijn arm uit, en stuurt hem weg: het plafond in.",
        "dag": "Oom Miroh heeft me dat geleerd. Iets wat jij nooit hebt gedaan. Dag, vader.",
        "weg": "Zuka loopt weg. Voor het eerst kijkt hij niet om.",
        "terug": "Terug naar het kamp. Allemaal. We komen terug, dat beloof ik.",
        "plan_b": "Ik had een plan B. Plan B was eigenlijk ook plan A, maar dan zachter.",
        "oom": "En ergens diep onder het paleis gaat Zuka naar de enige die hij nog wil zien.",
    }, {"aang": "Aang-guh", "sokka": "Sokka-guh", "katara": "Katara-guh", "azula": "Azula-Mika", "zuka": "Zuka", "mikai": "Vuurheer Mikai"}),
    "gevangenis": ("De cel onder het paleis", {
        "nee_gezegd": "Oom. Ik heb het gedaan. Ik heb nee gezegd tegen vader. Ik heb gezegd wat ik vond.",
        "draait": "Oom Miroh zegt niets. Hij draait zich langzaam om, naar de muur.",
        "goedmaken": "Ik weet dat ik je pijn heb gedaan in Ba Sing Guh. Ik kom het goedmaken. Echt. Ooit.",
        "stilte": "Stilte. Alleen het tikken van een druppel.",
        "ga": "...Ik ga. Pas goed op jezelf, oom.",
        "opdrukjes": "Als Zuka weg is, doet Miroh vijftig opdrukjes. Dan nog vijftig. Dan nog eens vijftig. De tralies kraken.",
        "sterk": "Een oude guh moet sterk zijn... voor als zijn neefje hem weer nodig heeft.",
    }, {"zuka": "Zuka", "miroh": "Oom Miroh"}),
    "kamp": ("Een gast in het kamp", {
        "avond": "Die avond, bij het kampvuur. Er staat iemand aan de rand van het licht.",
        "hoi": "Hallo. Zuka hier. Ik ben... aan jullie kant nu. Hoi.",
        "eruit": "Jij? Na alles wat je gedaan hebt? Eruit. Nu.",
        "eer": "Hij heeft wel héél lang zijn eer gezocht. Misschien heeft hij hem gevonden. In de prullenbak.",
        "leraar": "De Guhvatar moet vuur leren. Niemand anders hier kan dat. Ik wel. Een beetje.",
        "voeten": "Hij spreekt de waarheid. Ik voel het aan zijn hartslag. En aan mijn voeten. Die heeft hij namelijk ooit geschroeid.",
        "sorry": "Dat was... per ongeluk. Sorry, Toph.",
        "tweede": "Laten we het proberen. Iedereen verdient een tweede knabbel.",
        "eten": "Zuka mag blijven. Maar niemand zit naast hem bij het eten.",
    }, {k: TEAM[k] for k in ("aang", "katara", "sokka", "toph", "zuka")}),
    "kokend_rotsblok": ("Ontsnapping uit Het Kokend Rotsblok", {
        "suki": "Suki! Ik wist dat ik je zou vinden. Ik had er een plan voor. Eigenlijk zeven plannen.",
        "rennen": "Laat die plannen maar. Rennen!",
        "kabel": "Snij de kabel door! Laat ze in de kaassaus vallen!",
        "mee": "Joehoe! Mogen wij ook mee? Wij komen toch al.",
        "broertje": "Broertje. Wat zie je er... volgzaam uit. Staat je niet.",
        "eenzaam": "En jij ziet er eenzaam uit, Azula.",
        "kabel_zakt": "De gondel schiet het station binnen. Achter jullie zakt de kabel kreunend in de kaassaus. Niemand valt erin. Alleen Azula's goede humeur.",
        "dank": "Dank je, Zuka. Sokka zei al dat je veranderd was. Hij had gelijk. Zeg het hem niet.",
    }, {"sokka": "Sokka-guh", "suki": "Suki-guh", "zuka": "Zuka", "directeur": "De directeur", "ty_lee": "Ty Lee-Mika", "azula": "Azula-Mika"}),
    "draken": ("De meesters van het vuur", {
        "laatste_stap": "De laatste stap van de Drakendans. Dan is het stil op het terras.",
        "oordeel": "Nu oordelen de meesters. Laat zien wat er in jullie hart brandt.",
        "menu": "Ze gaan ons opeten. Ik dacht dat dit een dans was, geen menu!",
        "cirkel": "Twee draakguhs, een rode en een blauwe, cirkelen om het terras. Hun oortjes staan recht overeind.",
        "regenboog": "Ze spuwen vuur. Het brandt niet. Rood, goud, blauw, groen, roze: alle kleuren van de regenboog, en een paar die nog geen naam hebben.",
        "leven": "Vuur is geen woede... Vuur is leven. Het is energie. Het is... warm. Daar was ik het vergeten.",
        "jij": "Jij voelt het ook: vuur dat niet brandt maar verwarmt. Je leert vuursturen.",
        "gezien": "De meesters hebben jullie gezien. Het vuur in jullie is goed. Ga, en vergeet het niet.",
        "dank": "Guhvatar... dank je. Voor het eerst voelt mijn vuur als iets van mij. Ik hoor erbij, hè? Bij het team.",
        "vuur_van_jou": "Zuka hoort bij het team. En vanaf nu brandt jouw vuur. Het verwarmt.",
    }, {"zuka": "Zuka", "zonnekrijger": "Hoofd van de Zonnekrijgers"}),
    "toneel": ("De jongen in de kaasbol", {
        "vanavond": "Sintelstrand. Het openluchttheater speelt vanavond 'De jongen in de kaasbol'. Jullie zitten op de derde rij.",
        "guhvatar": "Ik ben de Guhvatar! Ik eet alleen vegetarische knabbels en ik lach heel hard! HAHAHAHA!",
        "lach": "...Zo lach ik toch niet? Zo lach ik toch niet, njeg?",
        "hoop": "Hoop! Hoop! Ik praat alleen maar over hoop! En ik huil heel veel. Snik! Hoop!",
        "soms": "Ik praat helemaal niet alleen maar over... oké. Soms.",
        "honger": "Ik ben grappig! Ik heb honger! Ik ben grappig omdat ik honger heb!",
        "goed": "Dat... is eigenlijk best goed.",
        "reus": "IK BEN EEN REUS! IK ZIE ALLES MET MIJN OREN!",
        "tien": "Eindelijk een acteur die mij snapt. Prachtig. Tien knabbels.",
        "eer": "Mijn eer! Mijn eeeer! Waar is mijn eer?! Heeft iemand mijn eer gezien?!",
        "kant": "Het litteken zit aan de verkeerde kant. En zo vaak praat ik niet over mijn eer.",
        "iedereen_kijkt": "Iedereen kijkt naar Zuka.",
        "vaak": "...Oké. Wél zo vaak.",
        "doek": "Het doek valt. Het publiek klapt. Jullie klappen ook. Een beetje.",
        "samen": "Weet je wat? Het verhaal klopt niet. Maar we zitten hier wél samen. En dat klopt helemaal.",
    }, {"aang": "Aang-guh", "katara": "Katara-guh", "sokka": "Sokka-guh", "toph": "Toph-guh", "zuka": "Zuka",
        "acteur_1": "Acteur (als Aang-guh)", "acteur_2": "Acteur (als Katara-guh)", "acteur_3": "Acteur (als Sokka-guh)",
        "acteur_4": "Acteur (als Toph-guh)", "acteur_5": "Acteur (als Zuka)"}),
    "tent": ("De tent van de Witte Lotus", {
        "kamp": "Het kamp van de Witte Lotus. Oude meesters rond een vuur, en één tent met licht erin.",
        "nooit": "Hij gaat me nooit vergeven. Waarom zou hij ook?",
        "spijt": "Oom... Het spijt me. Ik was zo dom. Zo verloren. Ik heb je verraden, terwijl jij altijd... altijd...",
        "opstaan": "Oom Miroh staat op. Langzaam. Hij legt zijn poten om zijn neefje heen.",
        "nooit_boos": "Ik was nooit boos op je. Ik was verdrietig, omdat ik bang was dat je de weg kwijt was.",
        "haten": "Ik dacht dat je me zou haten.",
        "neefje": "Haten? Je bent mijn neefje. Dat stopt niet omdat je een keer de verkeerde afslag neemt. Ook niet twee keer, njeg.",
        "thee": "Kom. Er is thee. Er is altijd thee.",
        "thee_drinken": "Ze drinken thee. Er wordt niet veel gezegd. Dat hoeft ook niet.",
        "lot": "Het lot is een grappig ding, Guhvatar. Morgen komt de komeet. Vanavond drinken we thee.",
        "kopje": "Ga zitten. Er zit nog een kopje in de pot. En er staat er altijd een voor jou klaar.",
    }, {"zuka": "Zuka", "miroh": "Oom Miroh"}),
    "top": ("De Top der Voorgangers", {
        "top": "De Top der Voorgangers. Boven de nevel van de Geestenwereld wachten de Guhvatars die er voor jou waren.",
        "komeet": "Morgen komt de knabbelkomeet. Mikai wil zijn kracht gebruiken om alles op te eten wat er te eten valt. Alles. Voor altijd.",
        "twijfel": "Ik heb ooit getwijfeld toen ik had moeten handelen. Daardoor kon zijn familie hongerig blijven. Twijfel niet zoals ik, jonge Guhvatar.",
        "hard": "Ik twijfelde nooit. Ik was wel eens te hard. Wees vastberaden, maar laat je hart niet verharden.",
        "geluk": "Een Guhvatar zet de wereld voorop. Maar vergeet niet dat jij óók bij die wereld hoort, njeg.",
        "vissen": "En neem rust. Ik heb ooit drie dagen achter elkaar gevist. Dat hielp ook.",
        "papier": "Schrijf alles op. Plannen, lijstjes, recepten. Ik had de mooiste bureaus van alle Guhvatars.",
        "alleen": "Je staat er niet alleen voor. Wij zijn altijd bij je, ook als je ons niet ziet. Ga terug naar je lijf. Het is tijd.",
    }, {"roku": "Roku-guh", "kyoshi": "Kyoshi-guh", "yangchen": "Yangchen-guh", "kuruk": "Kuruk-guh", "szeto": "Szeto-guh"}),
    "leeuwschildpad": ("De Leeuwschildpad-guh", {
        "wakker": "Je wordt wakker. Het is nacht. De grond onder je ademt.",
        "schildpad": "Je ligt op de rug van een eiland... van een schildpad... van een LEEUWSCHILDPAD. Met oortjes.",
        "bang": "Wees niet bang, kleine Guhvatar. Ik ben ouder dan de oudste knabbel.",
        "hart": "Je wilt Mikai stoppen zonder hem pijn te doen. Dat is een goed hart. Het is ook een moeilijk hart.",
        "vroeger": "Lang voor de Guhvatars stuurden guhs niet het vuur of het water. Ze stuurden het licht in zichzelf. De energie.",
        "honger": "Wie zijn eigen licht sterk houdt, kan de honger van een ander stillen. Neem hem niets af wat hij nodig heeft. Geef hem rust.",
        "licht": "Een warm licht stroomt door je heen, van je voorhoofd tot in je poten. Je weet nu wat je moet doen.",
        "eten": "Ga. De komeet komt. En vergeet niet te eten, njeg.",
        "zakt": "De Leeuwschildpad-guh zakt langzaam terug in de nevel. Het eiland is weg. Jij staat op de kade.",
    }, {"schildpad": "Leeuwschildpad-guh"}),
    "agni_kai": ("Agni Kai", {
        "komeet": "Het voorplein van het paleis. De komeet kleurt de hele lucht rood.",
        "agni_kai": "Een Agni Kai, broertje. Een vuurduel. Alleen jij en ik. Op de dag dat ik Vuurheerin word.",
        "niet_in_orde": "Ik ga met je vechten, Azula. Maar je bent niet in orde. Laat het gaan. Alsjeblieft.",
        "perfect": "Niet in orde? Ik ben PERFECT.",
        "vlammen": "Blauwe vlammen tegen rode. Ze lijken op elkaar. Ze zijn het niet.",
        "waterguh": "Kijk eens wie er meedoet. De waterguh. Even zien hoe goed jij kunt vangen.",
        "katara": "KATARA!",
        "zuka_valt": "Zuka springt ervoor. De bliksem duwt hem tegen de grond. Hij ademt. Hij ademt nog.",
        "hou_vol": "Zuka! Hou vol... Ik ben hier. Ik ben er.",
        "ketting": "Azula jaagt Katara-guh over het plein. Katara lokt haar boven een rooster met water en bevriest alles in één keer. Een ketting van ijs houdt Azula vast.",
        "nee": "Nee... nee... NEE!",
        "ijs": "Azula zit vast in het ijs en snikt. Niemand lacht. Katara blijft even bij haar staan.",
        "dank": "Je sprong voor mij. Dank je, Zuka. Stil nu, ik lap je op.",
        "ga": "Ik ben blij dat jij het bent die me oplapt. Ga, Guhvatar! Mikai is bij de rotspilaren!",
    }, {"zuka": "Zuka", "azula": "Azula-Mika", "katara": "Katara-guh"}),
    "omslag": ("De Guhvatar-staat", {
        "litteken": "Mikai's vuur gooit je tegen een rotspilaar. Een scherpe punt raakt je rug, precies op het oude litteken van Azula's bliksem.",
        "open": "En dan... gaat er iets open.",
        "bij_je": "Wij zijn bij je.",
        "alle": "Alle Guhvatars. Allemaal.",
        "ga": "Hihi, ik ook! Ga ervoor, njeg!",
        "ogen": "Je ogen gloeien. Lucht, water, aarde en vuur draaien om je heen als een ring van licht. De Guhvatar-staat.",
        "wat_ben_jij": "Wat... wat ben jij?",
        "laatste_ronde": "De laatste ronde. Alle vier de elementen. Tegelijk.",
    }, {"roku": "Roku-guh", "kyoshi": "Kyoshi-guh", "aang": "Aang-guh", "mikai": "Vuurheer Mikai"}),
    "trek_weg": ("Genoeg", {
        "ademt": "Mikai ligt op een pilaar. Hij ademt zwaar en kijkt naar de komeet. Hij heeft nog steeds trek. Altijd trek.",
        "maak_af": "Toe dan. Maak het af.",
        "poten": "Jij legt je poot op zijn voorhoofd en je andere op zijn hart. Zoals de Leeuwschildpad-guh deed.",
        "licht": "Jouw licht duwt tegen een donkere, knagende honger. Heel lang. Dan wint het jouwe.",
        "vol": "Wat... wat heb je gedaan? Ik... ik zit vol. Ik zit VOL.",
        "tevreden": "Je hebt hem zijn onstilbare trek afgenomen. Wat overblijft is een ronde, tevreden Mika die voor het eerst in zijn leven genoeg heeft.",
        "dutje": "Mag ik... mag ik misschien een dutje doen?",
        "snurkt": "Hij valt in slaap. Hij snurkt. Het klinkt bijna lief.",
    }, {"mikai": "Vuurheer Mikai", "mikai_vadsig": "Mikai"}),
    "kroning": ("De kroning", {
        "ochtend": "De volgende ochtend. Het voorplein staat vol: Mika's, guhs, en een paar verbaasde schildpad-eendguhs.",
        "kroon": "Oom Miroh zet zijn neefje de kroon op. Hij past. Natuurlijk past hij.",
        "voorbij": "De oorlog is voorbij. Vanaf vandaag delen we. Iedere knabbel, iedere vlam, iedere dag.",
        "proberen": "Ik beloof niet dat ik alles goed doe. Wel dat ik het probeer. Met hulp van mijn vrienden.",
        "oom": "En dat ik voortaan luister naar mijn oom. Meestal.",
        "plan": "Dit is het beste plan dat ooit gelukt is. En ik had er niet eens een plan voor!",
        "voeten": "Ik voel heel veel blije voeten.",
        "samen": "We hebben het gedaan. Samen.",
        "juiste_guh": "En jij, Guhvatar... jij was precies de juiste guh op de juiste plek. Ik had het stokje aan niemand beter kunnen geven.",
        "dank": "Guhvatar. Jij liet me zien wie ik kon zijn. Dank je. De Vuurnatie is ook jouw thuis.",
        "mooiste_dag": "De mooiste dag van mijn leven... na die keer met de perfecte jasmijnthee.",
        "theehuis": "Ik ga De Jasmijn Draak weer openen. Kom langs. Er staat altijd een kopje voor je klaar.",
        "zo": "En zo kwam er vrede in de vier volken. Met vuurwerk. Heel veel vuurwerk.",
    }, {"zuka": "Vuurheer Zuka", "miroh": "Oom Miroh", "aang": "Aang-guh", "katara": "Katara-guh", "sokka": "Sokka-guh", "toph": "Toph-guh"}),
    "aftiteling": ("De Legende van Guh", {
        "titel": "~ De Legende van Guh ~",
        "jij": "Met jou als de Guhvatar",
        "aang": "Aang-guh, de vorige Guhvatar (die even ging vadsen)",
        "waterstam": "Katara-guh en Sokka-guh, van de Waterstam",
        "toph": "Toph-guh, de Blinde Bandiet",
        "zuka_miroh": "Vuurheer Zuka, en Oom Miroh met zijn thee",
        "appa_momo": "Appa-guh en Momo-guh",
        "suki": "Suki-guh en de Kyoshi-krijgsters",
        "mikai": "Vuurheer Mikai, eindelijk vol",
        "azula": "Azula-Mika, die nog even nadenkt",
        "voorgangers": "Roku-guh, Kyoshi-guh en alle Guhvatars voor jou",
        "wezens": "De Leeuwschildpad-guh, de draakguhs en Fang",
        "muziek": "De muziek: kijk in het Guhboek bij de credits",
        "knipoog": "Een guh-knipoog naar Avatar: De Legende van Aang",
        "iedereen": "En iedere guh die ooit een knabbel deelde",
        "einde": "Het verhaal van de Guhvatar is verteld. Maar de wereld is groot, de thee is nog warm, en er is nog zoveel te ontdekken. Wordt vervolgd, njeg.",
    }, {}),
    "knipoog_stitch": ("Aloha in Het Kokend Rotsblok", {
        "aloha": "Aloha, njeg!",
        "wie": "WIE laat dat ding steeds binnen?!",
    }, {"stitch": "626-guh", "bewaker": "Bewaker-Mika"}),
    "knipoog_sjokkel_finish": ("Sjokkel finisht", {
        "sjokkel": "En helemaal achteraan, na vier hoofdstukken: Sjokkel komt over de finish van de luchtscooter-baan.",
        "gehaald": "Hij heeft het gehaald! Sjokkel, je bent een held!",
    }, {"aang": "Aang-guh", "sjokkel": "Sjokkel"}),
}

# =====================================================================================================================
# chat, the minigames, the talks, names and buttons
# =====================================================================================================================
TEKSTEN = {
    # the arrival, the school, the dance, the turtle-ducks
    Q + "aankomst.vermomd": "Je bent vermomd! Zolang je in de Vuurnatie-kleren loopt, denkt iedereen dat je een gewone Mika-scholier bent. Ga naar het schooltje.",
    Q + "dans.uitnodiging": "Een leerling fluistert: 'Ga op de vloer staan! Als de muziek begint: spring op het vak dat oplicht!'",
    Q + "dans.start": "De muziek begint! Sta op het oplichtende vak als de maat rond is.",
    Q + "dans.tel": "Nog %s...",
    Q + "dans.ronde": "Maat %s van %s, goed gedanst: %s (nodig: %s)",
    Q + "dans.weg": "Je stapt van de vloer. Het feest gaat door: kom terug om opnieuw te dansen.",
    Q + "dans.bijna": "Bijna! %s goed van de %s nodig. Stap opnieuw op de vloer om nog een keer te dansen.",
    Q + "dans.gewonnen": "Wauw! De hele grot juicht. Zo heeft hier nog nooit iemand gedanst!",
    Q + "dans.juf": "Dan staat de juf in de ingang. Iedereen rent weg. Jij ook: naar de paleisvijver, waar niemand zoekt. De schildpad-eendguhs hebben vast honger.",
    Q + "eendjes.gevoerd": "Plons! De schildpad-eendguhs komen aangezwommen. Ze kwaken tevreden. Eentje bijt zachtjes in je poot. Uit liefde.",
    Q + "eendjes.zwarte_zon": "Morgen is het zover: de Dag van de Zwarte Zon. Het team verzamelt zich op het strand naast de haven.",
    # the Black Sun, the prison, the camp
    Q + "gevangenis.volg": "Zuka glipt de gang in, naar de cel naast de hal. Volg hem, stil.",
    Q + "gevangenis.deur": "De ijzeren deur kraakt open. Iemand heeft hem op een kier laten staan... Zuka?",
    Q + "kamp.vlucht": "Je glipt de bunker uit en rent naar het kamp naast het landingsveld. Daar wacht het team.",
    # making up
    Q + "goedmaken.toph.1": "Zo, Vuurhoofd. Je wilt sorry zeggen. Leuk. Ik luister met mijn voeten, dus doe je best.",
    Q + "goedmaken.toph.2": "Ik... eh... Guhvatar? Help me even. Wat moet ik zeggen?",
    Q + "goedmaken.toph.3": "Kies goed, Guhvatar. Ik voel het als hij liegt.",
    Q + "goedmaken.toph.goed": "Toph: 'Hm. Eerlijk. Ik voel geen leugen. Oké, Vuurhoofd, je mag naast me zitten. Niet te dichtbij.' Nu Katara-guh nog...",
    Q + "goedmaken.toph.fout2": "Toph stampt. Zuka vliegt een meter de lucht in. 'Warm? WARM?' Probeer het nog eens.",
    Q + "goedmaken.toph.fout3": "'Je eer, je eer...' Toph gaapt heel hard. 'Probeer het nog eens, Vuurhoofd.'",
    Q + "goedmaken.katara.1": "Ik vertrouw hem niet. Hij heeft ons al te vaak verraden. Woorden zijn makkelijk.",
    Q + "goedmaken.katara.2": "Ze heeft gelijk. Guhvatar... wat kan ik doen?",
    Q + "goedmaken.katara.3": "Nou, Guhvatar? Zeg het maar.",
    Q + "goedmaken.katara.goed": "Zuka bakt voor iedereen knabbelpannenkoekjes. Ze zijn aangebrand. Katara eet er toch een. 'Hm. Niet slecht. Voor een prins.'",
    Q + "goedmaken.katara.fout2": "Katara: 'Een knuffel? Van mij? Aan hem? Nee.' Probeer het nog eens.",
    Q + "goedmaken.katara.fout3": "Katara: '...Wat?' Zuka bloost tot aan zijn knot. Probeer het nog eens.",
    Q + "goedmaken.sokka": "Sokka: 'Mijn beurt! Ik heb een plan. Een héél gevaarlijk plan. Het Kokend Rotsblok. Suki zit daar. Ga naar de gondel op de kraterrand!'",
    # Het Kokend Rotsblok
    Q + "rotsblok.plan": "Sokka: 'Daar is de gondel. Stap in, dan zijn we zo op het dak van de gevangenis. En dan: sluipen. Bukken! Bewakers zien je dan minder snel.'",
    Q + "rotsblok.gondel": "De gondel kraakt over het kokende meer van kaassaus...",
    Q + "rotsblok.binnen": "Je staat op het dak van Het Kokend Rotsblok. Sluip naar beneden en over de binnenplaats naar de cel van Suki-guh.",
    Q + "rotsblok.sluip": "Twee bewakers lopen over de binnenplaats. Blijf uit hun zicht, of buk.",
    Q + "rotsblok.betrapt": "Een bewaker-Mika ziet je! Hij duwt je zachtjes terug naar de hal. Opnieuw, en stiller.",
    Q + "rotsblok.gelukt": "Jullie zijn eruit! Suki-guh is vrij, Sokka-guh glimt, en Zuka hoort er een beetje meer bij.",
    # the Drakendans
    Q + "drakendans.hint": "Zuka's vuur is zwak geworden. 'Ik moet terug naar de bron', zegt hij. De ruïne van de Zonnekrijgers ligt ten westen van de krater.",
    Q + "drakendans.zonnekrijger": "Het hoofd van de Zonnekrijgers: 'Jullie willen het vuur begrijpen? Klim de lange trap op. Daar dansen jullie voor de meesters.'",
    Q + "drakendans.start": "De Drakendans! Stap op elke tegel die oplicht, één voor één. Zuka danst de andere helft.",
    Q + "drakendans.stap": "Stap %s van %s",
    Q + "drakendans.opnieuw": "De vlammen doven. Opnieuw. Langzaam, en samen.",
    Q + "drakendans.weg": "Je verlaat het terras. Kom terug om de dans opnieuw te beginnen.",
    Q + "drakendans.geleerd": "Je kunt vuursturen! (Vuur is het laatste element: je kent er nu vier.)",
    Q + "drakendans.toneel": "Het team gaat even uitrusten op Sintelstrand. Er is vanavond toneel. Over... jullie?",
    # the finale
    Q + "finale.miroh.1": "Morgen komt de komeet. Mikai zal hem gebruiken om alles op te eten wat er te eten valt. Je hebt raad nodig, Guhvatar.",
    Q + "finale.aang.1": "Ga mediteren! De vorige Guhvatars weten meer dan wij. Fang, de draak van Roku-guh, vliegt je naar de Top der Voorgangers.",
    Q + "finale.miroh.2": "Een kop thee voor onderweg? Nee? Dan ga je met een warm hart. Dat is ook goed.",
    Q + "finale.mediteer": "Je sluit je ogen. De Witte Lotus gaat voor je open... en je ziel gaat op reis.",
    Q + "finale.geen_geest": "Je kunt nu even niet mediteren (de Geestenwereld is er niet). Probeer het straks nog eens bij Oom Miroh.",
    Q + "finale.terug": "Je opent je ogen in de tent. De thee is nog warm. Buiten valt de nacht.",
    Q + "miroh.komeet": "De komeet komt dichterbij. Ik zet nog even thee. Thee helpt altijd een beetje.",
    Q + "miroh.thee.0": "Thee? Natuurlijk thee. Er is altijd thee voor de Guhvatar.",
    Q + "miroh.thee.1": "Een goede thee laat je even trekken. Net als een goede neef.",
    Q + "miroh.thee.2": "Ik heb de pot weer gevuld. Ga zitten, njeg. Vertel eens wat je gedaan hebt.",
    Q + "schildpad.slaap": "Je ogen worden zwaar...",
    Q + "schildpad.wakker": "Je staat op de kade. Was het een droom? Nee: het licht zit nog in je. Stap op Appa-guh: de luchtvloot komt!",
    Q + "vloot.geen_appa": "Appa-guh kan nu niet komen. Probeer het zo nog eens.",
    Q + "vloot.start": "Appa-guh stijgt op. Voor je hangt de luchtvloot van Mikai, rood en brandend. Vlieg erlangs: het team springt over en haalt ze neer!",
    Q + "vloot.op_appa": "Stap weer op Appa-guh!",
    Q + "vloot.van_appa": "Je bent van Appa-guh af.",
    Q + "vloot.te_laat": "De vloot heeft zich hergroepeerd.",
    Q + "vloot.botsing": "Bonk! Appa-guh botst tegen een luchtschip (niemand gewond, alleen een deuk in je trots).",
    Q + "vloot.neer.sokka": "Sokka-guh: 'Boemerang! Altijd de boemerang!' Het schip zakt rokend weg.",
    Q + "vloot.neer.toph": "Toph-guh: 'Metaal is óók aarde, njeg!' Ze vouwt de romp dubbel.",
    Q + "vloot.neer.suki": "Suki-guh: 'Kyoshi-krijgsters, vooruit!' Het schip draait weg en zakt in de wolken.",
    Q + "vloot.stand": "Neergehaald: %s van de %s",
    Q + "vloot.au": "Een vuurstoot duwt Appa opzij! (nog %s)",
    Q + "vloot.geraakt": "Te vaak geraakt: de vloot drijft je terug.",
    Q + "vloot.nog_niet": "Er vliegen er nog te veel! Neergehaald: %s van de %s. Keer om en help het team.",
    Q + "vloot.opnieuw": "Terug naar het begin van de vlucht. Nog een keer!",
    Q + "vloot.gelukt": "De luchtvloot is neergehaald! Iedereen veilig aan parachuutjes. Appa-guh landt bij het paleis...",
    Q + "duel.naar": "Ga naar de duelarena: Vuurheer Mikai wacht op de rotspilaren.",
    Q + "duel.ronde.1": "Ronde 1: lucht. Ontwijk zijn vuurstoten. Springen is een luchtsprong: hoog en ver, van pilaar naar pilaar.",
    Q + "duel.ronde.2": "Ronde 2: water. Mikai steekt vier pilaren in brand. Blus de vlammen (rechtsklik op een vlam).",
    Q + "duel.ronde.3": "Ronde 3: aarde. Er regenen vonken van de komeet. Klim naar de hoogste pilaar. Bukken is een aardesprong: een rotszuil gooit je omhoog.",
    Q + "duel.ronde.4": "Ronde 4: vuur. Sla zijn vuurstoten terug (linksklik op een stoot die dichtbij komt).",
    Q + "duel.ronde.5": "De Guhvatar-staat! Sla drie stoten terug en blus twee vlammen. Alle elementen zijn van jou.",
    Q + "duel.gewonnen.1": "Hij raakt je niet meer. Mikai brult.",
    Q + "duel.gewonnen.2": "Alle vlammen gedoofd. Er stijgt stoom op uit de arena.",
    Q + "duel.gewonnen.3": "Je staat bovenaan. De komeet is vlak boven je.",
    Q + "duel.gewonnen.4": "Zijn eigen vuur slaat terug in zijn gezicht. Maar dan...",
    Q + "duel.plons": "Plons! Het water is lauw. Terug op de pilaar.",
    Q + "duel.tijd": "Mikai hergroepeert. Deze ronde begint opnieuw.",
    Q + "duel.opnieuw": "Te vaak geraakt (niets aan de hand, alleen een duwtje). Deze ronde begint opnieuw.",
    Q + "duel.duw": "Een duwtje! Je staat nog.",
    Q + "duel.terug": "Teruggeslagen: %s van %s",
    Q + "duel.geblust": "Geblust: %s van %s",
    Q + "duel.klaar": "Het is voorbij. Ga naar het voorplein van het paleis: daar wacht iedereen.",
    # the baker, the rewards, the hints
    Q + "bakker.vuurvlokken": "Vuurvlokken! Vers! Ze knetteren in je mond. Twee voor de prijs van drie!",
    Q + "bakker.spannend": "Wat een spannende tijden. Ik bak maar wat. Bakken helpt.",
    Q + "bakker.feest": "Feest! De oorlog is voorbij en de Guhvatar staat in mijn winkel! Hier, van de zaak.",
    Q + "bakker.morgen": "Kom morgen terug, Guhvatar: dan heb ik weer iets lekkers voor je.",
    Q + "beloning.vuur": "Hoofdstuk 4 is af! Je krijgt de Vuurnatie-vermomming voor je guhs (in de kast van de Guhdex) en een zak knabbels.",
    Q + "beloning.finale": "Je bent de Guhvatar! Je krijgt je eigen standbeeld, de titel Guhvatar, Zuka's litteken voor je guhs en een flinke berg knabbels.",
    Q + "hint.finale": "De komeet komt eraan. Oom Miroh wacht in de tent van de Witte Lotus.",
    Q + "hint.daarna": "Het verhaal is verteld, maar de wereld is groot: de Geestenwereld is helemaal open, de bakker van de hoofdstad heeft elke dag iets lekkers, en ergens in de Luchttempel staat een leeg voetstuk... dat is nu niet meer leeg.",
    # names and buttons of the talking screens
    G + "naam.aang": "Aang-guh",
    G + "naam.toph": "Toph-guh",
    G + "naam.katara": "Katara-guh",
    G + "naam.zuka": "Zuka",
    G + "optie.mediteer": "Ik ga mediteren",
    G + "optie.straks": "Straks",
    G + "optie.toph.eerlijk": "\"Sorry dat ik je voeten heb geschroeid. Dat had nooit mogen gebeuren.\"",
    G + "optie.toph.warm": "\"Je voeten waren toch al warm, njeg?\"",
    G + "optie.toph.eer": "\"Mijn eer was in gevaar!\"",
    G + "optie.katara.doen": "\"Laat hem iets dóén in plaats van iets zeggen.\"",
    G + "optie.katara.knuffel": "\"Geef hem een knuffel.\"",
    G + "optie.katara.staart": "\"Hij heeft een hele mooie staart.\"",
    G + "beloning.vermomming": "Vuurnatie-vermomming (kleding voor je guhs)",
    G + "beloning.knabbels": "Een zak knabbels",
    G + "beloning.titel": "De titel Guhvatar",
    G + "beloning.litteken": "Zuka's litteken (kleding voor je guhs)",
    # the statue, the entities, the title, the clothes
    "block.guhs.guhvatar_beeld": "Guhvatar-standbeeld",
    "item.guhs.guhvatar_beeld.van": "Het beeld van %s",
    "item.guhs.guhvatar_beeld.lore": "Met gloeiende ogen. Zet het in je huisje, njeg",
    "entity.guhs.einde2_h4_vuur_luchtschip": "Luchtschip van de Vuurnatie",
    "entity.guhs.einde2_h4_vuur_vuurbal": "Vuurstoot",
    "gui.guhs.titel.guhvatar": "Guhvatar",
    "gui.guhs.titels.hint.guhvatar": "Maak het verhaal van de Guhvatar af, in het Guheinde",
    "item.guhs.vuurnatie_vermomming": "Vuurnatie-vermomming",
    "item.guhs.zuka_litteken": "Zuka's litteken",
    "gui.guhs.kledingbron.einde2_h4_vuur": "De Legende van Guh: hoofdstuk 4 en de finale",
    "advancements.guhs.guheinde.einde2_h4_vuur_guhvatar.title": "De Guhvatar",
    "advancements.guhs.guheinde.einde2_h4_vuur_guhvatar.description": "Maak het verhaal van de Guhvatar af: van de kaasbol tot de kroning",
}

# =====================================================================================================================
# FTB: the section "De Vuurnatie" and "De komeet van Mikai" (chapter guhs_guheinde): (key, titel, tekst, icon, adv)
# =====================================================================================================================
FTB_VUUR = [
    ("einde2_h4_vuur_aankomst", "Vermomd in de Vuurnatie", "Appa-guh brengt het team naar de Vuurnatie-hoofdstad. Rode kleren aan, oortjes onder een hoofdband: niemand mag weten wie je bent.", "guhs:einde2_bouw_vuur_embleem", "guhvatar_vuur_stap_1"),
    ("einde2_h4_vuur_school", "Het schooltje", "Schuif aan in de klas van de Vuurnatie. Wie is de beste Vuurheer? Je weet het antwoord al, njeg.", "minecraft:book", "guhvatar_vuur_stap_2"),
    ("einde2_h4_vuur_dans", "Het geheime dansfeest", "Dansen is verboden in de Vuurnatie. In de grot achter het schooltje doen ze het toch. Sta op het oplichtende vak!", "minecraft:note_block", "guhvatar_vuur_stap_3"),
    ("einde2_h4_vuur_eendjes", "De schildpad-eendguhs", "Voer de schildpad-eendguhs in de paleisvijver. Ze bijten zachtjes. Uit liefde.", "minecraft:bread", "guhvatar_vuur_stap_4"),
    ("einde2_h4_vuur_zwarte_zon", "De Dag van de Zwarte Zon", "Acht minuten zonder vuur. De troonzaal is leeg, en diep onder het paleis zegt Zuka eindelijk wat hij vindt.", "minecraft:black_concrete", "guhvatar_vuur_stap_5"),
    ("einde2_h4_vuur_cel", "De cel onder het paleis", "Volg Zuka naar zijn oom. Miroh zegt niets. Maar hij doet wel opdrukjes.", "minecraft:iron_bars", "guhvatar_vuur_stap_6"),
    ("einde2_h4_vuur_kamp", "Een gast in het kamp", "Zuka staat aan de rand van het kampvuur. Niemand vertrouwt hem. Toph-guh al helemaal niet.", "minecraft:campfire", "guhvatar_vuur_stap_7"),
    ("einde2_h4_vuur_goedmaken", "Goedmaken", "Help Zuka de goede woorden te vinden, voor Toph-guh en voor Katara-guh.", "guhs:kaas_knabbels", "guhvatar_vuur_stap_8"),
    ("einde2_h4_vuur_rotsblok", "Het Kokend Rotsblok", "Met Sokka-guh en Zuka naar de gevangenis in het kokende meer. Sluip langs de bewakers en bevrijd Suki-guh.", "minecraft:iron_chain", "guhvatar_vuur_stap_9"),
    ("einde2_h4_vuur_drakendans", "De Drakendans", "Dans voor de draakguhs van de Zonnekrijgers, stap voor stap. Dan leer je wat vuur echt is.", "guhs:einde2_bouw_vuur_zonnesteen", "guhvatar_vuur_stap_10"),
    ("einde2_h4_vuur_toneel", "De jongen in de kaasbol", "Een toneelstuk over jullie avontuur. Het klopt van geen kant. Kijk het toch.", "minecraft:painting", "guhvatar_vuur_stap_11"),
    ("einde2_h4_vuur_tent", "De tent van de Witte Lotus", "Zuka en Oom Miroh. Een tent, een pot thee, en een zin waar iedereen op heeft gewacht.", "guhs:einde2_bouw_vuur_lotustegel", "guhvatar_vuur_stap_12"),
]
FTB_FINALE = [
    ("einde2_h4_vuur_top", "De Top der Voorgangers", "Fang vliegt je door de Geestenwereld naar de vorige Guhvatars. Ze hebben raad. En een paar grapjes.", "minecraft:feather", "guhvatar_finale_stap_1"),
    ("einde2_h4_vuur_leeuwschildpad", "De Leeuwschildpad-guh", "Je wordt 's nachts wakker op de rug van een eiland. Dat eiland heeft oortjes.", "minecraft:turtle_scute", "guhvatar_finale_stap_2"),
    ("einde2_h4_vuur_luchtvloot", "De luchtvloot", "Vlieg op Appa-guh door de brandende luchtschepen van Mikai. Het team haalt ze neer, jij stuurt.", "minecraft:fire_charge", "guhvatar_finale_stap_3"),
    ("einde2_h4_vuur_agni_kai", "Agni Kai", "Zuka en Azula-Mika op het voorplein van het paleis. Blauw vuur tegen rood.", "minecraft:blue_ice", "guhvatar_finale_stap_4"),
    ("einde2_h4_vuur_duel", "Het duel", "Vier rondes op de rotspilaren, één voor elk element. En dan raakt de rots je oude litteken...", "minecraft:nether_star", "guhvatar_finale_stap_5"),
    ("einde2_h4_vuur_kroning", "De kroning", "Een nieuwe Vuurheer. Vuurwerk. Thee.", "minecraft:golden_helmet", "guhvatar_finale_stap_6"),
    ("einde2_h4_vuur_guhvatar", "De Guhvatar", "Het verhaal is verteld. Je krijgt je eigen standbeeld, met gloeiende ogen.", "guhs:guhvatar_beeld", "guhvatar_finale_stap_7"),
]
