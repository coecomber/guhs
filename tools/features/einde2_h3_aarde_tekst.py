"""
einde2 phase B, slice h3_aarde: every Dutch text of chapter 3 (Ba Sing Guh). English: tools/lang/en/c94_e2_h3_aarde.json.

Own words throughout (CONTRACT_E2_B 6.3): the well-known lines of the show only as a guh-ified paraphrase; Miroh's lines are
the ones the design approved (DESIGN 5 "Miroh, de start en samen reizen"); his lullaby for Lu Ten is an OWN song about a
young soldier guh coming home (no line, image or melody of the series' song).
"""

# =====================================================================================================================
# the questline (verhaal_motor.verhaallijn): (stapnaam, nu, waar) per step; index = step
# =====================================================================================================================
from features.einde2_audio import LIED as _AUDIO_LIED

LIJN_NAAM = "Hoofdstuk 3: Ba Sing Guh"
LIJN_UITLEG = ("Het Aarderijk: een stad met drie ringen muren, een strenge loketguh, een blinde kampioen, een theehuis waar het "
               "naar jasmijn ruikt... en een geheim dat niemand hardop zegt. Leer aardesturen, vind Appa-guh terug en overleef de "
               "val van Ba Sing Guh.")
STAPPEN = [
    # (fix2-h3: every nu text says WHAT and WHERE, with a direction and a landmark you can see)
    ("Aankomst in Ba Sing Guh", "Vlieg met Appa-guh naar Ba Sing Guh en land op de landingslob: het grasveld aan de westkant, voor de grote "
                                "buitenmuur.", "De landingslob, het grasveld voor de westpoort van Ba Sing Guh"),
    ("Het paspoortloket", "Niemand komt Ba Sing Guh binnen zonder stempel. Loop van de landingslob naar de grote poort in de buitenmuur "
                          "(oost). In de doorgang, links, zit de strenge loketguh achter zijn raampje: praat met hem.",
     "Het loket in de doorgang van de buitenpoort"),
    ("De Aardrommel", "Aang-guh heeft een aardeleraar nodig, en de sterkste aardesturers van de stad vechten in de Aardrommel. Ga door de poort de stad in en loop naar het zuidoosten, naar de "
                      "grote arena met de tribunes en de bel. Loop de arena in.", "De arena van de Aardrommel, zuidoost van de poort"),
    ("Zien met je voeten", "De Blinde Bandiet wil je iets leren. Ga de tribune af en praat met Toph-guh, beneden in de ring van de arena.",
     "Toph-guh, in de ring van de Aardrommel"),
    ("De monorail", "Toph-guh stuurt je naar de middenring: daar wacht je tot de Aardekoning je wil ontvangen. Neem de monorail. Loop terug naar de buitenpoort: links ervan (noord), binnen de muur, staat de "
                    "traptoren met het bordje 'Naar de muur en de monorail'. Boven wacht de wagen: ga erin staan en spring om hem met "
                    "aardesturen te duwen.", "De wagen van de monorail, boven op de buitenmuur bij de poort"),
    ("De Jasmijn Draak", "Er zou in de middenring een theehuis zijn waar de thee naar de hemel smaakt. Loop vanaf station Middenring de "
                         "trap af en dan naar het zuiden: De Jasmijn Draak heeft een groen dak en een draak boven de deur. Ga naar binnen.",
     "De Jasmijn Draak, zuid van station Middenring"),
    ("Thee met Oom Miroh", "Oom Miroh leert je thee zetten. Ga achter de toonbank de keuken in (achterin, zuidkant) en rechtsklik op de "
                           "grote ketel op het fornuis. Klik nog eens telkens als het meterje in het groen staat.",
     "De keuken van De Jasmijn Draak, achter de toonbank"),
    ("Een spelletje Pai Sho", "Speel een potje Pai Sho met Oom Miroh: praat met hem, achter de toonbank van het theehuis.",
     "Oom Miroh, in De Jasmijn Draak"),
    ("Geen knabbeltekort", "Joo Dee-guh, de gids van de stad, kan jullie misschien bij de Aardekoning brengen. Ze staat voor haar huis in het noorden van de "
                           "middenring (een huis dat links en rechts precies gelijk is). Praat met haar.",
     "Het huis van Joo Dee-guh, middenring noord"),
    ("Terug naar het kamp", "Twee Dai Li-Mika's volgen je, en Appa-guh en Momo-guh zijn alleen bij het kamp. Ga door de buitenpoort naar buiten, naar jullie kamp aan de "
                            "zuidkant van de landingslob, waar Appa landde.", "Het kamp op de landingslob"),
    ("De Aardekoning", "Appa is weg, meegenomen door de Dai Li! Alleen de Aardekoning-guh kan ze bevelen hem terug te geven. Hij zit in de troonzaal van het grote gouden paleis, midden "
                       "in de stad (de bovenring). Praat met hem.", "De troonzaal van het gouden paleis, midden in de stad"),
    ("Meer Laogai", "Bosco rook iets bij Meer Laogai, het meer in het oosten van de stad. Aan de noordkant ervan staat een houten "
                    "botenschuur met een groen dak ('Botenverhuur GESLOTEN'): ga erin en de trap af naar de basis. Daar: bukken, wachten "
                    "tot de wachters wegkijken, en gaan. Gezien = terug naar het begin, verder niets.",
     "De botenschuur aan de noordkant van Meer Laogai, oost in de stad"),
    ("De heuvel met de boom", "Oom Miroh is in gevaar: 'A' komt hem en zijn neef Zuka halen. Hij zei dat hij vandaag naar de heuvel "
                              "met de kersenboom gaat, op de landingslob buiten de buitenmuur (noord van waar Appa landt). Waarschuw hem. "
                              "Appa is terug: fluit hem, of loop.",
     "De heuvel met de kersenboom op de landingslob"),
    ("De kristalcatacomben", "De Dai Li hebben Katara-guh en Zuka meegenomen, diep onder het paleis. Oom Miroh gaat met je mee. Ga naar de oude "
                             "poort in de paleistuin, aan de oostkant van het gouden paleis (een mossig poortgebouw, 'Verboden toegang'), "
                             "en volg de gang omlaag naar de kristalcel en de kristalkamer.", "De oude poort in de paleistuin, oostkant van het paleis"),
    ("De ontsnapping", "Ba Sing Guh is gevallen. Vlucht op Appa-guh: dat gaat vanzelf, meteen na de val. Ben je er tussenuit "
                       "gegaan en sta je nog in de stad? Loop dan naar het kamp op de landingslob.", "Het kamp op de landingslob"),
]
KLAAR = ("Aardesturen geleerd, Appa-guh terug, Toph-guh in het team. Op naar de Vuurnatie!", "Op Appa's rug, op weg naar de Vuurnatie")
EXTRA = {"3_baan": ("Ogen dicht: Toph's oefenbaan is het ommuurde veld direct oost van de arena (ingang aan de westkant, bij het bordje "
                    "'Blinddoekbaan'). Ga erin: bukken is stampen. Loop naar het felste licht en sta op alle zes aardemunten.",
                    "De ommuurde oefenbaan direct oost van de arena")}
KORT = {"0": "Land bij Ba Sing Guh", "1": "Praat met de loketguh", "2": "Ga naar de Aardrommel", "3": "Praat met Toph-guh",
        "3_baan": "Vind de zes aardemunten, blind", "4": "Duw de monorail naar de middenring", "5": "Zoek De Jasmijn Draak",
        "6": "Zet thee bij de grote ketel", "7": "Speel Pai Sho met Oom Miroh", "8": "Praat met Joo Dee-guh", "9": "Terug naar het kamp",
        "10": "Naar de Aardekoning", "11": "Sluip naar Meer Laogai", "12": "Waarschuw Oom Miroh", "13": "Red Katara-guh in de catacomben",
        "14": "Ontsnap op Appa-guh"}
# the Superkompas name of the goal of each step (E2H3Aarde.doel)
DOELEN = ["de landingslob van Ba Sing Guh", "het paspoortloket", "de Aardrommel", "Toph-guh", "het monorailperron", "De Jasmijn Draak",
          "de theeketel", "Oom Miroh", "Joo Dee-guh", "het kamp", "de Aardekoning", "Meer Laogai", "de heuvel met de boom",
          "de kristalcatacomben", "het kamp"]

# =====================================================================================================================
# the narrator card
# =====================================================================================================================
KAART_TITEL = "Hoofdstuk 3: Ba Sing Guh"
KAART_REGELS = [
    "Na de Noordelijke Waterstam vloog Appa-guh dagenlang over de leegte, op weg naar het Aarderijk.",
    "Daar ligt Ba Sing Guh: een stad met muren zo hoog dat zelfs de wolken eromheen moeten.",
    "Achter die muren wonen duizenden guhs. En ze zeggen allemaal dat er niets aan de hand is.",
    "Jij moet hier aardesturen leren. Van de koppigste leraar van het hele Guheinde.",
    "En ergens in de stad schenkt een ronde oude guh thee... aan wie maar binnenkomt. Njeg.",
]

# =====================================================================================================================
# the scenes: (title, {key: line}, {actor: name})
# =====================================================================================================================
SCENES = {
    "aankomst": ("Aankomst in Ba Sing Guh", {
        "begin": "Ba Sing Guh. De ommuurde stad van het Aarderijk, op haar eigen plak klipkaas in de leegte.",
        "muur": "Wauw. Daar komt echt geen Mika doorheen. Zelfs geen hele dikke.",
        "hoop": "Hier zijn we veilig. En hier helpen ze ons vast tegen de Vuurnatie.",
        "knabbels": "En ze hebben vast de grootste knabbelmarkt van het hele Guheinde!",
        "poort": "Maar eerst: het paspoortloket. Njeg.",
    }, {}),
    "loket": ("Het paspoortloket", {
        "stempel": "Paspoort... in orde.",
        "papieren": "Formulier 3B, formulier 7Q en de verklaring van vadsigheid... ook in orde.",
        "welkom": "Welkom in Ba Sing Guh. Hier is iedereen veilig. En er is hier geen knabbeltekort.",
        "poort": "Met een zware zucht schuiven de tralies van de buitenpoort omhoog.",
    }, {}),
    "aardrommel": ("De Aardrommel", {
        "kei": "In deze hoek: de rots die nooit wijkt, de kiezel die nooit kraakt... DE KEI-GUH!",
        "rotsen": "De Kei-guh is klaar om te rotsen!",
        "bandiet": "En in die hoek: de kampioen. Klein, blind... en nog nooit verslagen: DE BLINDE BANDIET!",
        "kom": "Kom maar op, grote kiezel.",
        "verslagen": "De Kei-guh... voelt zich... nogal verslagen.",
        "wie": "Volgende! Wie durft er nog?",
        "jij": "Jij daar, op de tribune. Je voeten zijn luid, njeg. Je stampt als een luchtbizon. Kom straks maar eens naar beneden.",
    }, {}),
    "jasmijn_draak": ("De Jasmijn Draak", {
        "draak": "De Jasmijn Draak. Het ruikt er naar jasmijn, warme knabbels... en heel vaag naar Vuurnatie.",
        "welkom": "Welkom, welkom! Kom binnen, ga zitten. De thee is bijna klaar.",
        "vreemde": "Thee drinken met een vreemde guh is een van de echte vadsigheden van het leven.",
        "thee": "Thee. Voor u.",
        "zuur": "Een kop thee met een zuur gezicht smaakt ook zuur, neefje.",
        "neef": "Ik ben je neefje niet... Ik bedoel: ja, oom. Natuurlijk, oom.",
        "blik": "Zuka kijkt je heel lang aan. Dan draait hij zich om en poetst een kopje dat al schoon was.",
        "ketel": "Help je mij met een pot thee? De ketel staat in de keuken. Ik leer het je wel, njeg.",
    }, {}),
    "appa_ontvoerd": ("Appa's verloren dagen", {
        "dut": "Terwijl jij door de stad liep, lag Appa-guh bij het kamp te dutten. Momo-guh sliep op zijn kop.",
        "stenen": "Toen kwam de grond omhoog.",
        "mee": "Rustig maar, grote. Je gaat een poosje logeren.",
        "brul": "Appa brulde. Maar stenen handen houden zelfs een luchtbizon heel stil.",
        "momo": "Momo-guh vluchtte de bosjes in. Hij keek tot ze weg waren.",
        "dag1": "Dag een. Ze brachten hem onder de stad, diep onder Meer Laogai, waar het licht nooit komt.",
        "dag2": "Dag twee. Appa at niet. Hij keek alleen naar het kleine gaatje in het plafond, waar soms een wolk voorbij dreef.",
        "dag3": "Dag drie. Een Dai Li-Mika gooide hem een kool toe. Appa rook eraan... en dacht aan jouw handen, die altijd naar knabbels roken.",
        "wacht": "En elke nacht neuriede hij heel zacht. Zodat je hem zou horen, als je ooit in de buurt kwam.",
        "leeg": "Bij het kamp ligt alleen nog een plukje witte vacht.",
        "momo2": "Momo-guh komt uit de bosjes en drukt zich tegen je been. Zijn oortjes hangen helemaal plat.",
        "zoek": "Appa is weg. Iemand in deze stad weet waar hij is.",
    }, {"dai_li_1": "Dai Li-Mika"}),
    "long_feng": ("Long Feng-Mika", {
        "tralies": "Achter de tralies, in het donker: iets groots en wits. Iets dat heel zacht neuriet.",
        "knap": "Knap gedaan, Guhvatar. Helemaal tot hier, zonder een enkele Dai Li-Mika wakker te maken.",
        "niets": "Maar hier is niets te zien. Er is geen luchtbizon in Ba Sing Guh.",
        "knabbel": "En er is ook geen knabbeltekort. Begrijp je? In Ba Sing Guh is er nooit iets aan de hand.",
        "dit": "O nee? En wat is DAT dan, achter die tralies? Een heel groot kussen?",
        "liegen": "Je hebt de hele stad voorgelogen. De Aardekoning weet van niets!",
        "weg": "De Aardekoning weet precies wat ik hem vertel. ...Dai Li, wij gaan.",
    }, {}),
    "weerzien": ("Weerzien met Appa", {
        "krak": "Je stampt. De vloer trilt. De tralies buigen open alsof ze van klei zijn.",
        "appa": "Appa-guh tilt zijn grote kop op. Hij snuift. Hij snuift nog een keer.",
        "lik": "Een grote, natte, warme tong. Je bent van top tot teen nat. Het kan je helemaal niets schelen.",
        "thuis": "Kom, we gaan naar huis. Allemaal.",
        "jipjip": "Jip jip!",
    }, {}),
    "lu_ten": ("Een lied voor Lu Ten", {
        # Miroh's lullaby: the words are audio's (einde2_audio.LIED), timed to moment.lu_ten; here under the scene's own keys
        **{"lied." + k: v for k, v in _AUDIO_LIED.items()},
        "delen": "Sommige knabbels deel je, ook als er niemand meer is om ze mee te delen.",
    }, {}),
    "val": ("De val van Ba Sing Guh", {
        "kristal": "Onder het paleis gloeien de kristallen groen. Hier vonden jullie Katara-guh terug. En Zuka.",
        "reunie": "Wat gezellig. Een familiereunie, diep onder de grond.",
        "zuka": "Zuka, lieve broer. Kies maar. Kom mee naar huis... of blijf bij je theeoompje.",
        "weg": "Zuka. Is het jouw eigen weg, of een weg die een ander je oplegt?",
        "stil": "Het is heel stil in de kristalkamer. Zuka kijkt naar zijn oom. Dan naar zijn zus.",
        "zuka_naam": "...Zuka.",
        # fix3-h3 (playtest 2 item 8): the choice with more heart
        "thuis": "Vader mist je, Zuka. Echt. Kom mee, en je mag weer thuis wonen. Dan mogen je oortjes eindelijk weer omhoog.",
        "eer": "Thuis... Mijn eer terug...",
        "trots": "Je hoeft niemands trots te verdienen om goed te zijn, neefje. Ook niet die van je vader. Alleen die van jezelf.",
        "sorry": "...Het spijt me, oom.",
        "keuze": "Hij kiest de verkeerde kant.",
        "nu": "Ze zijn met te veel! Guhvatar, de Guhvatar-staat. NU!",
        "staat": "Je ogen gloeien. De lucht om je heen zoemt. Heel even voel je alle Guhvatars van voor jou.",
        "bliksem": "Daar gaat je glimmende moment.",
        "val": "De bliksem raakt je midden in de gloed. Je valt. Het wordt stil en zwart in je hoofd.",
        "water": "Het geestenwater van de Noordpool... alsjeblieft. Alsjeblieft, laat het werken.",
        "ogen": "Een koele gloed. Je ogen gaan open. Katara-guh lacht door haar tranen heen.",
        "ga": "Ga! Allemaal! Ik houd ze hier wel even bezig, njeg.",
        "appa": "Appa! Iedereen erop!",
        "miroh": "Achter jullie sluiten de Dai Li-Mika's Oom Miroh in, in een kooi van groen kristal. Hij kijkt jullie na. En knikt.",
        "gevallen": "Ba Sing Guh was gevallen. Maar de Guhvatar leefde nog, en vloog weg op de rug van een luchtbizon.",
    }, {}),
    # fix3-h3 (playtest 2 items 1, 8, 9, 10): the story between the steps (H3Hints.BRUGGEN), the crystal cell, Miroh's
    # capture, the escape with Toph and the talk in flight (H3Verloop.finale)
    "leraar": ("Een leraar voor de Guhvatar", {
        "binnen": "We zijn binnen! Lucht kan ik al, water leer ik van Katara... nu nog aarde. Maar wie leert mij dat?",
        "geduld": "Iemand die heel sterk is. En heel veel geduld heeft met een Guhvatar die overal tegenaan vliegt, njeg.",
        "affiche": "Kijk wat er bij de poort hing: 'De Aardrommel! Vanavond: de sterkste aardesturers van Ba Sing Guh!' Als er ergens een leraar zit, dan daar.",
        "kampioen": "Een kampioen als leraar? Ja! Daar wil ik heen!",
        "pauze": "En er staat: knabbels in de pauze. Dat is ook belangrijk. Voor het plan.",
        "arena": "Ergens in het zuidoosten juicht een arena. De grond dreunt zachtjes mee onder je pootjes.",
    }, {}),
    "middenring": ("Op naar de middenring", {
        "kiezel": "Niet slecht, kiezel. Voor een luchtsturder.",
        "koning": "Dank je, Toph. Nu moeten we de Aardekoning spreken. Hij moet weten wat de Vuurnatie van plan is.",
        "muren": "De koning? Die zit in de bovenring, achter drie muren en duizend formulieren. Daar komt niemand zomaar binnen.",
        "afspraak": "Dan vragen we een afspraak aan. En terwijl we wachten...",
        "monorail": "...duw jij de monorail naar de middenring. Met aardesturen. Goede oefening. Daar is een theehuis waar de thee naar de hemel smaakt, zeggen ze.",
        "mee": "Ga je niet mee, Toph?",
        "titel": "Ik heb een titel te verdedigen. En ik vind jullie heus wel terug. Jullie lopen als een kudde luchtbizons.",
    }, {}),
    "uitnodiging": ("Oren in de muren", {
        "oren": "Jullie willen de Aardekoning spreken, zeiden jullie? Zachtjes, njeg. In deze stad hebben zelfs de theekopjes oren.",
        "wie": "Oren? Van wie dan?",
        "dai_li": "Van de Dai Li. De stille guhs met de groene hoeden. Ze houden de stad rustig. Soms een beetje té rustig.",
        "gids": "Onze gids, Joo Dee-guh, zoekt ons al de hele dag. Zij kan ons vast bij de koning brengen.",
        "luister": "Ga dan maar naar haar toe. En luister goed naar wat ze niet zegt.",
        "zuka": "Achter de toonbank poetst Zuka hetzelfde kopje voor de twintigste keer. Hij heeft alles gehoord.",
    }, {}),
    "gevolgd": ("Twee schaduwen", {
        "glimlach": "Joo Dee-guh glimlacht. Ze glimlacht nog steeds. Ze heeft nog geen één keer met haar ogen geknipperd.",
        "raar": "Oké. Dit is raar. Ze gaf op geen enkele vraag antwoord. En kijk eens wie daar bij de muur staan.",
        "miroh": "Dai Li. Ze lopen al achter ons aan sinds het theehuis. Oom Miroh had gelijk.",
        "appa": "Appa en Momo zijn helemaal alleen bij het kamp... Als die Dai Li ons volgen, wie let er dan op hen?",
        "kamp": "Precies wat ik dacht. Terug naar het kamp. Nu meteen.",
    }, {}),
    "na_appa": ("Het lege kamp", {
        "schuld": "Appa... Ik had hier moeten zijn. Ik had hem nooit alleen mogen laten.",
        "stenen": "Het is niet jouw schuld. Kijk: handen van steen, in de grond gedrukt. Dit waren de Dai Li.",
        "dai_li": "De stille guhs met de groene hoeden. Die werken voor de stad. Dus eigenlijk werken ze voor...",
        "koning": "...de Aardekoning. Als iemand ze kan bevelen Appa terug te geven, is hij het.",
        "paleis": "Geen formulieren meer, geen afspraken. We gaan naar het paleis. Vandaag nog.",
        "momo": "Momo-guh klimt op je schouder en houdt zich stevig vast. Zijn oortjes staan weer een heel klein beetje rechtop.",
    }, {}),
    "waarom_miroh": ("Een brief met een rood zegel", {
        "brief": "Waar Long Feng-Mika stond, ligt een opgerolde brief. Er zit een rood zegel op, met een vlammetje.",
        "lees": "'Aan Long Feng. In theehuis De Jasmijn Draak verstoppen zich twee verraders van de Vuurnatie: de verbannen prins Zuka en zijn oom. Houd ze vast tot ik er ben. A.'",
        "zuka": "Zuka?! De ober met het litteken... dat is Zuka! De prins die ons al achternazit sinds de Luchttempel!",
        "oom": "En die ronde guh met de theepot is zijn oom. Die stond elke keer op dat schip. Met thee.",
        "aardig": "Maar Oom Miroh was aardig voor ons. Hij schonk ons thee toen niemand ons wilde zien. Als 'A' hem komt halen...",
        "heuvel": "Hij zei dat hij vandaag naar de heuvel buiten de muur ging. Naar de kersenboom. 'Voor iemand die ik mis.'",
        "waarschuwen": "Dan gaan we hem waarschuwen. Een vriend is een vriend. Ook als zijn neefje ons achternazit.",
    }, {}),
    "katara_weg": ("Katara is weg", {
        "rennen": "Beneden aan de heuvel klinkt gehijg. Sokka-guh komt naar boven rennen, helemaal buiten adem.",
        "katara": "Katara is weg! Ze ging naar het theehuis om Zuka te waarschuwen... en de Dai Li namen ze allebei mee. Diep onder het paleis!",
        "neefje": "Mijn neefje ook...",
        "waarom": "Oom Miroh... we weten wie Zuka is. Hij zit al achter ons aan sinds de Luchttempel. Waarom eigenlijk?",
        "vader": "Omdat zijn vader hem wegstuurde. Zuka zei ooit iets op het verkeerde moment, en kreeg vuur in zijn gezicht. Daar komt dat litteken vandaan. En zijn ene oortje staat sindsdien nooit meer helemaal rechtop.",
        "thuis": "Hij mag pas naar huis als hij de Guhvatar meebrengt. Hij jaagt niet op jou. Hij jaagt op een thuis.",
        "azula": "En 'A' is mijn nichtje Azula. Als zij hier is, valt deze stad. De Dai Li brengen hun gevangenen naar de oude catacomben onder het paleis. Ik ga met je mee.",
        "appa": "Ik haal Appa en houd de lucht vrij. Als jullie Katara hebben: fluiten. Hard!",
    }, {}),
    "kristalcel": ("De kristalcel", {
        "stemmen": "Uit de kristalcel komen stemmen. Jullie blijven even staan, in het donker, en luisteren.",
        "oor": "Ik dacht altijd dat jij gewoon een boze Mika was die ons achternazat. Maar je oor... en dat litteken... wat is er gebeurd?",
        "vader": "Mijn vader. Ik zei iets op de verkeerde dag, tegen de verkeerde guh. Hij noemde het een les. Daarna stuurde hij me weg.",
        "thuis": "Ik mag pas terug als ik de Guhvatar breng. Dan krijg ik mijn eer terug. Mijn thuis. En misschien... dat hij trots op me is.",
        "water": "Ik heb water van de Noordpool bij me. Geestenwater. Misschien kan het je litteken helen...",
        "dank": "...Het hoort bij me. Maar... dank je. Echt.",
        "neefje": "Neefje! Een oude generaal kent nog een paar trucjes met vuur, njeg. Dit kristal smelt als suiker in de thee.",
        "oom": "Oom? Jij... met de Guhvatar?",
        "harten": "Ik ben waar de thee warm is en de harten ook. Kom, allemaal. Hier is het niet veilig.",
        "kamer": "Verderop, in de grote kristalkamer, gloeit iets groen op. Iemand wacht daar al op jullie.",
    }, {}),
    "miroh_gevangen": ("Oom Miroh gevangen", {
        "stilte": "Diep onder de stad, in de stilte na de bliksem. Het kristal groeit verder.",
        "kooi": "Kijk eens aan. Oompje, in een kooi van kristal. Het staat je goed.",
        "thee": "Kristal of klei, nichtje. Thee smaakt overal hetzelfde, zolang je hem deelt.",
        "vader": "Dai Li, breng hem naar de Vuurnatie. Vader zal blij zijn. Een verrader minder om thee mee te drinken.",
        "oom": "Oom, ik...",
        "blik": "Oom Miroh kijkt zijn neefje lang aan. Hij zegt niets. Voor het eerst heeft hij geen wijsheid voor Zuka. Alleen een blik die meer pijn doet dan bliksem.",
        "huis": "Kom, broertje. We gaan naar huis. Dat wilde je toch?",
    }, {}),
    "ontsnapping": ("De ontsnapping", {
        "vlucht": "Appa-guh schiet uit de Koningsput omhoog, over de muren van Ba Sing Guh. Achter jullie gaan de lichten van de stad een voor een uit.",
        "wacht": "Hé! Wacht eens even, jullie!",
        "hier": "Dachten jullie dat je zonder mij kon wegvliegen? Ik voelde jullie al aankomen bij de muur.",
        "vliegen": "Toph! Maar je haat vliegen!",
        "help": "Ik háát vliegen, njeg. Maar de Guhvatar kan nog geen kiezel rechtop zetten. Ik ga mee. Ik help jullie. En de Guhvatar.",
    }, {}),
    "koers": ("Koers naar het westen", {
        "waarheen": "Oké. Ba Sing Guh is gevallen, de koning is gevlucht met Bosco. Dus... waar gaan we heen?",
        "miroh": "Ze hebben Oom Miroh. Azula neemt hem mee naar de Vuurnatie.",
        "vuur": "En de Guhvatar moet nog vuur leren. Er is maar één plek waar dat kan.",
        "vermomming": "Recht het hol van de Mika in. ...In vermomming dan. Rode kleren, nepnamen. Misschien moet de Guhvatar zelfs naar school, dan valt hij helemaal niet op.",
        "snor": "Ik doe geen nepsnor op. ...Oké. Een kleintje.",
        "school": "Naar school? Ik ben nog nooit naar school geweest! Leuk!",
        "west": "Appa-guh draait naar het westen, waar de lucht rood kleurt. Achter jullie wordt Ba Sing Guh kleiner en kleiner. Op naar de Vuurnatie.",
    }, {}),
    "knipoog_mewtwo": ("Iemand in de tank", {
        "vrijwillig": "Die zit hier vrijwillig. Echt. We krijgen hem er niet meer uit.",
        "portie": "Dubbele portie. Njeg.",
        "giechel": "Ergens giechelt iets roze. Niemand kijkt.",
    }, {"mewtwo": "Mewtwo-guh"}),
    "knipoog_sjokkel": ("Sjokkel op de muur", {
        "sjokkel": "Op de muur tussen de ringen kruipt Sjokkel. Hij is al heel ver gekomen. Denkt hij.",
    }, {"sjokkel": "Sjokkel"}),
}

# =====================================================================================================================
# quest.guhs.einde2_h3_aarde.* (the chat lines and talking screens of the roles)
# =====================================================================================================================
QUEST = {
    # the narrator, in the chat
    "klaar": "Hoofdstuk 3 is klaar: Ba Sing Guh. Toph-guh hoort bij het team. Appa-guh zet koers naar de Vuurnatie...",
    "aarde_geleerd": "Je hebt aardesturen geleerd! Je voelt de grond onder je pootjes, en de grond voelt jou.",
    # the passport desk
    "loket.wacht": "Volgende! ...O. U bent er nog niet. Eerst landen, dan in de rij, dan pas een loket. Zo werkt dat.",
    "loket.vraag.0": "Naam?",
    "loket.vraag.0.0": "%s.",
    "loket.vraag.0.1": "De Guhvatar.",
    "loket.vraag.0.2": "Guh.",
    "loket.antwoord.0.0": "Genoteerd. In drievoud.",
    "loket.antwoord.0.1": "De Guhvatar. Uh-huh. Dat zeggen ze allemaal. Genoteerd.",
    "loket.antwoord.0.2": "Guh. Met een hoofdletter? Genoteerd.",
    "loket.vraag.1": "Doel van uw bezoek?",
    "loket.vraag.1.0": "Toerisme.",
    "loket.vraag.1.1": "De Aardekoning spreken.",
    "loket.vraag.1.2": "Knabbels kopen.",
    "loket.antwoord.1.0": "Toerisme. Prima. De stad is mooi. Er is niets aan de hand.",
    "loket.antwoord.1.1": "De Aardekoning ontvangt niemand. Ik schrijf maar 'toerisme' op.",
    "loket.antwoord.1.2": "Knabbels genoeg in Ba Sing Guh. Meer dan genoeg. Ik schrijf 'toerisme' op.",
    "loket.vraag.2": "Heeft u Mika's, vuur of verboden knabbels bij u?",
    "loket.vraag.2.0": "Nee.",
    "loket.vraag.2.1": "Alleen Momo-guh.",
    "loket.vraag.2.2": "...een paar knabbels.",
    "loket.antwoord.2.0": "Mooi zo.",
    "loket.antwoord.2.1": "Een lemur is geen Mika. Mag erin. Zolang hij niets jat.",
    "loket.antwoord.2.2": "Knabbels zijn niet verboden. Gelukkig maar, njeg. Ik had er net zelf een op.",
    "loket.na.0": "Volgende!",
    "loket.na.1": "Uw stempel is nog geldig. Volgende!",
    "loket.na.2": "Ik heb vandaag al tweehonderd stempels gezet. Mijn pootje doet pijn. Volgende!",
    # Toph-guh
    "toph.vroeg": "Wat moet je? Ik vecht zo. Ga maar op de tribune zitten, dan hoor ik je tenminste niet.",
    "toph.ring": "Kijk maar goed. Nou ja, kijken. Je snapt me wel.",
    "toph.les.0": "Dus jij bent de Guhvatar. Je loopt alsof de grond je iets schuldig is.",
    "toph.les.1": "Aarde is geen water. Aarde wacht niet tot jij er klaar voor bent. Aarde blijft staan. Jij moet dat ook leren.",
    "toph.les.2": "Doe je ogen dicht. Nee, echt dicht. Daar, naast de arena, is mijn oefenbaan. Ga erin en voel.",
    "toph.les.3": "Elke paar tellen trilt de grond, en als je bukt stamp je zelf. Ergens in de vloer liggen zes aardemunten. Sta op alle zes. Met je ogen dicht, njeg.",
    "toph.bezig": "%s van de %s. Stampen, voelen, lopen. Niet spieken.",
    "toph.geleerd": "Zie je wel? Je ziet met je voeten. Je bent nog steeds een kiezel, maar wel een kiezel die luistert.",
    "toph.na.0": "Ik zie je wel aankomen, hoor. Met mijn voeten.",
    "toph.na.1": "Een aardesturer staat stevig. Ook als iedereen om hem heen omvalt.",
    "toph.na.2": "De Kei-guh wil een herkansing. De Kei-guh krijgt geen herkansing.",
    "toph.na.3": "Ik ben blind, niet dom. Ik weet heus wel dat je net naar me zwaaide.",
    "toph.mee.0": "Dus. Ba Sing Guh is gevallen, de koning is weg, en jullie gaan naar de Vuurnatie. Toch?",
    "toph.mee.1": "Toph... je gaat toch niet...",
    "toph.mee.2": "Iemand moet de Guhvatar leren dat aarde niet wijkt. Jullie zijn allemaal veel te zacht.",
    "toph.mee.3": "Je haat vliegen.",
    "toph.mee.4": "Ik háát vliegen, njeg. ...Ik ga mee. En als iemand er iets van zegt, stamp ik hem van die bizon af.",
    "toph.mee.5": "Team Guhvatar is compleet! Nou ja... bijna.",
    # the Aardrommel
    "kei.0": "De Kei-guh is niet boos. De Kei-guh is alleen een beetje... gekiezeld.",
    "kei.1": "De Kei-guh traint nu met kleinere tegenstanders. Kiezels. Grind. Zand.",
    "kei.2": "Een rots die valt, staat weer op. Dat zegt de Kei-guh.",
    "omroeper.0": "Volgende week in de Aardrommel: de Blinde Bandiet tegen... iedereen die durft!",
    "omroeper.1": "Kaartjes voor de Aardrommel! Knabbels in de pauze! Knabbels ook voor de pauze!",
    "omroeper.2": "Wie de Blinde Bandiet verslaat, krijgt een gouden riem. Niemand heeft hem ooit gekregen.",
    # Oom Miroh
    "miroh.vroeg": "Een nieuw gezicht! Kom straks eens langs in mijn theehuis, De Jasmijn Draak. Middenring, je ruikt het vanzelf.",
    "miroh.welkom": "Ga zitten, ga zitten! Niemand die De Jasmijn Draak met een lege maag verlaat.",
    "miroh.thee.uitleg": "Goede thee is geduld, njeg. Klik op de ketel in de keuken. Het water mag net niet koken, de blaadjes moeten trekken maar niet verdrinken, en schenk tot de rand... maar er niet overheen. Klik als het meterje in het groen staat.",
    "thee.vroeg.0": "Nog even. Het water moet nog wakker worden.",
    "thee.laat.0": "O jee, nu kookt het. Kokend water maakt jasmijn bitter. Nog een keer.",
    "thee.goed.0": "Precies goed! Het water zingt. Nu de blaadjes.",
    "thee.vroeg.1": "Te vroeg! De blaadjes hebben hun verhaal nog niet verteld.",
    "thee.laat.1": "Te lang. Nu zijn ze verdronken in hun eigen verhaal. Nog een keer.",
    "thee.goed.1": "Ah, ruik je dat? Nu schenken. Rustig aan.",
    "thee.vroeg.2": "Dat is een halve kop. Een halve kop is een halve vriendschap.",
    "thee.laat.2": "Over de rand! Geeft niks, de tafel had ook dorst. Nog een keer.",
    "thee.gelukt": "Perfect. Een kop thee als een warme deken. Hier, neem mijn oude theeset mee. Ik heb er toch twee... of drie... of vijf.",
    "thee.ronde.0": "Eerst het water: klik op de ketel als het meterje in het groen staat. Net niet koken!",
    "thee.ronde.1": "Dan de blaadjes: laat ze trekken, en klik in het groen.",
    "thee.ronde.2": "En nu schenken: bijna vol. Niet over de rand!",
    "miroh.pai_sho.vraag": "Speel je Pai Sho? Nee? Geeft niets. Het is heel eenvoudig: om de beurt leg je een tegel, en wie eerst een lotus legt, wint. Een tegel met vier blaadjes eromheen. Zullen we?",
    "miroh.pai_sho.ja": "Graag!",
    "miroh.pai_sho.straks": "Straks.",
    "miroh.pai_sho.gewonnen": "Je hebt gewonnen! Ha! Je bent een natuurtalent. Of ik word oud. Allebei, waarschijnlijk.",
    "miroh.pai_sho.verloren": "Ik heb gewonnen? O jee. Geeft niets. Verliezen is ook een spel, alleen met meer thee.",
    "miroh.pai_sho.gelijk": "Gelijkspel! Het mooiste soort uitslag: niemand hoeft de afwas te doen.",
    "miroh.pai_sho.hoop": "Weet je, in moeilijke tijden zoeken guhs hoop bij anderen. Maar hoop is iets wat je jezelf geeft, njeg. Dat is innerlijke kracht.",
    "miroh.pai_sho.tafel": "Neem deze tafel mee. Een spel is pas een spel als je het deelt.",
    "miroh.wijs.0": "Een knabbel die je bewaart voor later, is ook een beetje een knabbel van nu.",
    "miroh.wijs.1": "Wie altijd naar de top kijkt, ziet de bloemen langs het pad niet.",
    "miroh.wijs.2": "Trots is een zware rugzak. Thee is een lichte. Kies maar.",
    "miroh.wijs.3": "Mijn neefje is als een ketel: veel stoom, maar binnenin gewoon warm water.",
    "miroh.wijs.4": "Een guh die valt, is geen guh die verloren heeft. Pas als hij blijft liggen.",
    "miroh.wijs.5": "Pai Sho leert je dat de mooiste zet soms de zet is die je niet doet.",
    "miroh.heuvel": "...",
    "miroh.heropend": "Welkom terug in De Jasmijn Draak! Dit is de mooiste dag van mijn leven... na die keer met de perfecte jasmijnthee. Ga zitten, de pot staat altijd klaar.",
    "miroh.na.0": "Thee? Natuurlijk thee. Altijd thee.",
    "miroh.na.1": "Zuka schrijft me elke week. Hij zegt dat de knabbels in de Vuurnatie nu eerlijk verdeeld worden. Ik ben zo trots.",
    "miroh.na.2": "Dit kopje is van het huis. Dat zijn ze eigenlijk allemaal.",
    "miroh.na.3": "Ga zitten, Guhvatar. Zelfs helden mogen even vadsen.",
    # Zuka the waiter
    "zuka.vroeg": "We zijn nog niet open. ...Ik bedoel: welkom. Zegt mijn oom.",
    "zuka.ober.0": "Thee. Ja. Wat voor thee? ...Het maakt me niet uit. Ik haal gewoon thee.",
    "zuka.ober.1": "Ik ben geen ober. Ik ben... Laat maar. Ik ben een ober.",
    "zuka.ober.2": "Mijn oom zegt dat ik vaker moet glimlachen. Dit is mijn glimlach.",
    "zuka.ober.3": "Nog één klant die om 'de speciale knabbelthee van het huis' vraagt...",
    "zuka.ober.4": "Waarom kijk je zo naar me? Ik heb niets met de Vuurnatie te maken. Niets.",
    "zuka.lu_ten": "Mijn oom? Die is weg. Naar de heuvel buiten de muur, die met de boom. Hij gaat er elk jaar heen, op deze dag. ...Laat hem maar even. Of ga naar hem toe. Mij een zorg.",
    "zuka.val": "Je vriendin. Katara-guh. De Dai Li hebben haar meegenomen, naar beneden, onder het paleis. Mijn oom gaat met je mee. Ik... ik zie wel.",
    # the koolman
    "kool.hallo.0": "Verse kolen! Knapperige kolen! De mooiste kolen van de hele onderring!",
    "kool.hallo.1": "Kolen, kolen, kolen! Pas op voor mijn kraam, hij is nieuw!",
    "kool.hallo.2": "Een kool per dag houdt de Mika's weg. Dat zeg ik altijd. Niemand gelooft het.",
    "kool.kapot.1": "MIJN KOLEN!",
    "kool.kapot.2": "Ik ben verhuisd naar de middenring. Hier is het veilig, dacht ik... MIJN KOLEN!",
    "kool.kapot.3": "De bovenring! Waar niemand ooit... MIJN KOOOOLEN!",
    "kool.oorzaak.1": "Je stapt opzij voor een kar vol theekisten, struikelt over een kool... en de hele kraam zakt in elkaar.",
    "kool.oorzaak.2": "Momo-guh ziet een kool. Momo-guh wil die kool. De kraam vindt dat geen goed idee.",
    "kool.oorzaak.3": "Twee Dai Li-Mika's schieten langs, onder de grond door. De stenen gaan omhoog. De kolen ook.",
    "kool.boos.1": "Jij! Jij bent die guh van... Nou ja. Het was een ongelukje. Toch? TOCH?",
    "kool.boos.2": "Twee keer! Twee kramen! Ik ga een kraam van steen bouwen. Van aardemunt. Met een muur eromheen.",
    "kool.geef.0": "Drie kramen. Drie! Weet je wat? Neem hem maar mee. Ik bouw wel een nieuwe.",
    "kool.geef.1": "Bij jou gaat hij vast minder snel stuk. ...Toch?",
    "kool.na.0": "Ik heb ze weer opgebouwd. Alle drie. Kijk er alsjeblieft niet naar.",
    "kool.na.1": "Kolen! Verse kolen! Op veilige afstand van de Guhvatar!",
    "kool.na.2": "Mijn kolen zijn beroemd geworden. Er komen toeristen kijken. Naar de brokstukken.",
    # Joo Dee-guh
    "joo_dee.welkom": "Welkom in Ba Sing Guh! Ik ben Joo Dee-guh. Ik laat u graag alles zien. Alles wat er te zien is.",
    "joo_dee.vraag": "Wat wilt u weten? Ik weet alles over Ba Sing Guh. Alles wat ik mag weten.",
    "joo_dee.vraag.0": "Waar is de Aardekoning?",
    "joo_dee.vraag.1": "Weet de stad dat de Vuurnatie oorlog voert?",
    "joo_dee.vraag.2": "Is er genoeg te eten voor iedereen?",
    "joo_dee.antwoord.0": "De Aardekoning is heel druk. Met belangrijke koningsdingen. Hij ontvangt niemand.",
    "joo_dee.antwoord.1": "Oorlog? Hihi. In Ba Sing Guh praten we niet over... nare dingen.",
    "joo_dee.antwoord.2": "Natuurlijk! Hihi.",
    "joo_dee.knabbeltekort": "Er is geen knabbeltekort in Ba Sing Guh.",
    "joo_dee.volgers": "Achter je, bij de muur, staan opeens twee Dai Li-Mika's. Ze kijken de andere kant op. Heel opvallend. Ga maar eens terug naar het kamp.",
    "joo_dee.na.0": "Er is geen knabbeltekort in Ba Sing Guh.",
    "joo_dee.na.1": "Wat een mooie dag! Hihi. Er is geen knabbeltekort.",
    "joo_dee.na.2": "Hallo! Ik ben Joo Dee-guh. ...Kennen wij elkaar?",
    # the king, Long Feng, Bosco
    "koning.0": "Een bezoeker! Wat leuk, ik krijg nooit bezoek. Ik ben de Aardekoning-guh, en dit is Bosco. Bosco, zeg eens hallo.",
    "koning.1": "Je vertelt de koning over Appa-guh. En over de Vuurnatie, die al honderd jaar knabbels inpikt.",
    "koning.2": "Majesteit, de Guhvatar is moe van de reis. Er is geen knabbeltekort in Ba Sing Guh. En geen luchtbizon.",
    "koning.3": "Bosco snuffelt aan je Appa-fluitje. Zijn neus gaat omhoog. Dan draait hij zich om en kijkt door het raam. Naar het meer.",
    "koning.4": "Bosco? Wat zie je daar? Meer Laogai? Daar is toch niets... Long Feng?",
    "koning.5": "Grrrom.",
    "koning.6": "Long Feng-Mika kijkt ook naar het meer. Heel even maar. Net een tel te lang.",
    "koning.waarheid": "Long Feng heeft mij voorgelogen? Al die jaren? ...Dan is er dus WEL een knabbeltekort?! Bosco, haal de grote knabbelkist. We gaan delen.",
    "koning.vroeg.0": "Ben je een bezoeker? Long Feng zegt dat ik het heel druk heb. Heb ik het druk? Ik weet het niet.",
    "koning.vroeg.1": "Bosco heeft vandaag drie honingraten op. Dat is een record. Ik ben heel trots op Bosco.",
    "koning.vroeg.2": "In mijn stad is iedereen gelukkig. Dat zegt Long Feng. Long Feng weet alles.",
    "koning.na.0": "Ik leer elke dag iets nieuws over mijn eigen stad. Gisteren: er zijn ARMEN in de onderring. Wie wist dat?",
    "koning.na.1": "Bosco mist Appa. Ze waren al bijna vrienden.",
    "koning.na.2": "Ik heb een brilletje gekregen. Nu zie ik alles. Ook wat ik liever niet zag.",
    "long_feng.0": "De koning heeft geen tijd. Doorlopen.",
    "long_feng.1": "Bent u verdwaald? De uitgang is daar. Heel ver weg.",
    "long_feng.2": "Er is geen knabbeltekort in Ba Sing Guh. Ik zeg het nog één keer, en dan zeg ik het nog één keer.",
    "azula": "Kom maar dichterbij, Guhvatar. Ik bijt niet. Ik duw alleen. Heel hard.",
    "dai_li.0": "Doorlopen, burger. Er is hier niets te zien.",
    "dai_li.1": "Wij zijn de Dai Li. Wij bewaken de cultuur van Ba Sing Guh. En de stilte.",
    "dai_li.2": "Heeft u vragen? Stel ze niet.",
    "dai_li.3": "Hm.",
    "volger.0": "Wij? Wij volgen niemand. Wij lopen toevallig dezelfde kant op.",
    "volger.1": "Mooie dag, he. Heel normaal.",
    "volger.2": "...",
    "klant.0": "De thee is hier zo lekker. Ik kom elke dag. Soms twee keer.",
    "klant.1": "Die ober is zo chagrijnig. Ik geef hem altijd een extra knabbel. Misschien helpt het ooit.",
    "klant.2": "Ze zeggen dat de oude baas ooit een hele beroemde generaal was. Nee joh. Moet je hem zien met die theepot.",
    "klant.3": "Psst. Er is geen knabbeltekort. Zeggen ze.",
    # the sneak through the base
    "sluip.begin": "De basis onder Meer Laogai. Overal Dai Li-Mika's die heen en weer kijken: de stofbundel voor hun neus laat zien waar. Bukken maakt je stil (dan zien ze alleen het oranje stukje); achter hun rug langs is veilig. Appa's kamer is aan het eind van de gang met de cellen, naar het zuiden.",
    "sluip.halt.0": "Halt! Terug naar de hal, burger.",
    "sluip.halt.1": "Wie is daar? ...Terug jij. Hup.",
    "sluip.halt.2": "Hier is niets te zien. Echt niet. Terug.",
    "sluip.halt.3": "Verdwaald? De uitgang is die kant op. Doei.",
    # tea guests in your guhhuisje (the team, when the theeset stands there)
    "theebezoek.aang_guh": "Mmm! Dit smaakt precies als in De Jasmijn Draak! Mag ik nog een kopje? En nog een?",
    "theebezoek.katara_guh": "Wat gezellig hier. Je huisje is net zo warm als je thee, njeg.",
    "theebezoek.sokka_guh": "Thee is prima. Maar heb je er ook knabbels bij? Gewoon vragen. Voor het plan.",
    "theebezoek.toph_guh": "Thee is goed. Zolang we niet hoeven te vliegen.",
    "theebezoek.zuka_team": "Mijn oom zou trots op je zijn. Niet op mij. Ik heb mijn kopje laten vallen.",
    "theebezoek.zuka_vuurheer": "Ook een Vuurheer drinkt graag thee. Zeker deze. Je hebt het van de beste geleerd.",
}

# =====================================================================================================================
# gui.guhs.einde2_h3_aarde.* and the blocks
# =====================================================================================================================
GUI = {
    "verteller": "Verteller",
    "tril.gevonden": "Aardemunt gevonden! %s van de %s",
    "monorail.uitleg": "Spring (spatie) om de wagen met aardesturen te duwen. Bukken = uitstappen.",
    "monorail.stand": "Monorail: %s%% van de rit   %s   (spatie = duwen)",
    "monorail.aan": "Station! Je hebt %s keer geduwd. Vahoeg!",
    "thee.meter.0": "Water:",
    "thee.meter.1": "Blaadjes:",
    "thee.meter.2": "Schenken:",
    "theeset.wacht": "De thee moet nog even trekken. Probeer het over %s minuut opnieuw.",
    "theeset.drink.0": "Jasmijnthee. Je voelt je heel even een held. Een vadsige held.",
    "theeset.drink.1": "Een kopje van Oom Miroh's recept. Warm tot in je oortjes.",
    "theeset.drink.2": "Thee! Je pootjes worden sneller. Of je denkt dat. Allebei goed.",
    "theeset.drink.3": "Slurp. Ahhh. Precies niet te heet.",
    "pai_sho.titel": "Pai Sho",
    "pai_sho.nieuw": "Nieuw spel",
    "pai_sho.sluit": "Klaar",
    "pai_sho.jij": "Jouw beurt: klik op een leeg vakje.",
    "pai_sho.denkt": "%s denkt na...",
    "pai_sho.kijk": "Je kijkt mee.",
    "pai_sho.wacht_kort": "De tafel wacht op een tweede speler...",
    "pai_sho.wacht": "Wie speelt er mee? Laat iemand binnen tien tellen op de tafel klikken. Anders speelt een guh van het huis mee. (Nog eens klikken: meteen tegen een guh.)",
    "pai_sho.tegen": "%s speelt mee!",
    "pai_sho.gewonnen": "Je hebt een lotus! Gewonnen!",
    "pai_sho.verloren": "De ander heeft een lotus. Volgende keer beter, njeg.",
    "pai_sho.miroh_wint": "Oom Miroh heeft een lotus.",
    "pai_sho.gelijk": "Het bord is vol: gelijkspel.",
    "pai_sho.uit.a": "De witte lotus wint.",
    "pai_sho.uit.b": "De rode lotus wint.",
    "pai_sho.guh": "Een guh van het huis",
    "pai_sho.niemand": "(niemand)",
    "pai_sho.weg": "(weg)",
    "pai_sho.regels": "Om de beurt leg je een tegel op een leeg vakje. Wie eerst een lotus heeft (een tegel met vier van zijn eigen tegels boven, onder, links en rechts ervan), wint.",
}
BLOKKEN = {
    "block.guhs.pai_sho_tafel": "Pai Sho-tafel",
    "block.guhs.pai_sho_tafel.lore": "Om de beurt een tegel; wie eerst een lotus legt, wint. Tegen een guh of tegen een vriend.",
    "block.guhs.theeset": "Theeset van Oom Miroh",
    "block.guhs.theeset.lore": "Een kopje jasmijnthee geeft een klein bonusje. Zet hem in je guhhuisje, dan komt het team op de thee.",
    "block.guhs.einde2_h3_aarde_koolkraam": "Kraam van de koolman",
    "block.guhs.einde2_h3_aarde_koolkraam.lore": "Breekbaar. Heel breekbaar.",
    "block.guhs.einde2_h3_aarde_koolkraam.klik": "MIJN KOLEN!",
    "item.guhs.aarderijk_hoed": "Aarderijk-hoed",
    "item.guhs.blinde_bandiet_riem": "Riem van De Blinde Bandiet",
    "gui.guhs.kledingbron.einde2_h3_aarde": "Hoofdstuk 3 van de Guhvatar: Ba Sing Guh",
    "gui.guhs.verhalen.guhvatar_aarde.beloning.koolkraam": "De kraam van de koolman (als je hem drie keer hebt zien breken)",
}


# =====================================================================================================================
# fix round 1 stage 2 (fix2-h3, playtest 1: "ietsjes meer hints, een duidelijkere aansluiting"): the step hints (H3Hints)
# HINTS gui.guhs.einde2_h3_aarde.hint.<naam>: the chat line after the look scene (%s = the direction, "in het oosten, een
#   eindje verder"); it says WHERE, the step's own nu text says WHAT. Step 3 (Toph) has no line of its own: its nu text
#   (and 3_baan) is shown, so it follows the lesson.
# KIJK scene.guhs.einde2_h3_aarde_kijk_<naam>: the ~3 s look scenes (titel, the one run-on caption), the list of H3Hints.KIJK.
# TEAM gui.guhs.einde2_h3_aarde.team.<naam>: (wie, line) a team member says why we go there next, when the step starts;
#   wie = E2Team.Lid id, only said when that member is in the player's team.
# TITELS gui.guhs.einde2_h3_aarde.*: the start and finish titles of the games, the way through the base and the catacombs.
# =====================================================================================================================
HINTS = {
    "loket": "De strenge loketguh zit %s, in de doorgang van de grote poort, links achter zijn raampje. Praat met hem.",
    "aardrommel": "De arena van de Aardrommel ligt %s: door de poort de stad in, naar de arena met de tribunes en de bel. Loop er gewoon in.",
    "baan": "Toph's oefenbaan ligt %s: het ommuurde veld naast de arena, ingang bij het bordje 'Blinddoekbaan'. Ga erin en zoek met je "
            "voeten de zes aardemunten.",
    "monorail": "De wagen van de monorail wacht %s, boven op de buitenmuur. De traptoren omhoog staat binnen de muur, links (noord) van de "
                "poort: bordje 'Naar de muur en de monorail'.",
    "theehuis": "De Jasmijn Draak staat %s: de trap van het station af, naar het groene dak met de draak boven de deur. Loop naar binnen.",
    "thee": "De grote ketel staat %s, achterin de keuken achter de toonbank. Rechtsklik erop om te beginnen.",
    "pai_sho": "Oom Miroh staat %s, achter de toonbank. Praat met hem voor een potje Pai Sho.",
    "joo_dee": "Joo Dee-guh staat %s, voor haar huis in het noorden van de middenring. Praat met haar.",
    "kamp": "Het kamp ligt %s: door de buitenpoort naar buiten, op de landingslob, naast waar Appa landde.",
    "koning": "De Aardekoning-guh zit %s, in de troonzaal van het grote gouden paleis midden in de stad. Praat met hem.",
    "laogai": "De botenschuur staat %s, aan de noordkant van Meer Laogai: houten schuur, groen dak, bordje 'Botenverhuur GESLOTEN'. "
              "Binnen gaat de trap omlaag naar de basis.",
    "lu_ten": "De heuvel met de kersenboom ligt %s, op de landingslob buiten de buitenmuur. Loop de heuvel op.",
    "val": "De oude poort staat %s, in de paleistuin aan de oostkant van het gouden paleis: een mossig poortgebouw met het bordje "
           "'Verboden toegang'. Daarachter gaat de trap omlaag.",
    "toph": "Het kamp ligt %s, op de landingslob. Daar staat Appa-guh klaar om te ontsnappen.",
}
KIJK = {
    "loket": ("Het paspoortloket", "Het paspoortloket. Zonder stempel kom je er niet in."),
    "aardrommel": ("De Aardrommel", "De arena van de Aardrommel. Hoor je dat gejuich?"),
    "baan": ("De blinddoekbaan", "Toph's oefenbaan. Ogen dicht, voeten open."),
    "monorail": ("De monorail", "De wagen van de monorail. Stap in, en duw!"),
    "theehuis": ("De Jasmijn Draak", "De Jasmijn Draak. Het ruikt hier naar jasmijn."),
    "joo_dee": ("Joo Dee-guh", "Joo Dee-guh, de gids van de stad. Ze glimlacht al."),
    "kamp": ("Het kamp", "Jullie kamp op de landingslob. Appa en Momo wachten daar."),
    "koning": ("Het paleis", "Het paleis van de Aardekoning. Ergens daarbinnen zit hij."),
    "laogai": ("De botenschuur", "De botenschuur aan Meer Laogai. 'Er is hier geen basis.' Vast niet."),
    "lu_ten": ("De heuvel met de boom", "De heuvel met de kersenboom. Daar zit iemand."),
    "val": ("De oude poort", "De oude poort in de paleistuin. Daaronder: de catacomben."),
    "toph": ("Bij het kamp", "Het kamp. Appa-guh staat klaar om weg te vliegen."),
}
TEAM = {
    "theehuis": ("aang", "Ruik je dat? Jasmijn! Dat moet dat theehuis zijn, daar beneden."),
    "pai_sho": ("aang", "Oom Miroh heeft een spelbord klaarstaan. Pai Sho! Vraag maar of hij een potje wil spelen."),
    "laogai": ("katara", "Bosco keek naar het meer. Meer Laogai! Daar is Appa, ik weet het zeker. En dat bordje bij de botenschuur liegt."),
}
TITELS = {
    "baan.titel": "Zien met je voeten",
    "baan.onder": "Bukken = stampen · loop naar het felste licht · %s/%s",
    "baan.klaar": "Aardesturen geleerd!",
    "baan.klaar_onder": "Alle zes aardemunten gevonden",
    "monorail.titel": "De monorail",
    "monorail.onder": "Tik op spatie = duwen · bukken = uitstappen",
    "monorail.station": "Station!",
    "thee.titel": "Thee zetten",
    "thee.onder": "Klik op de ketel als het meterje in het groen staat",
    "thee.klaar": "Perfecte thee!",
    "thee.klaar_onder": "De theeset van Oom Miroh is van jou",
    "sluip.titel": "Sluip naar Appa",
    "sluip.onder": "Bukken = stil · blijf uit hun stofbundel",
    "pad.appa": "Appa's kamer: %s. Volg de gang met de cellen.",
    "pad.kristal": "Volg de gang en de lichtjes omlaag: de kristalkamer is %s.",
}
