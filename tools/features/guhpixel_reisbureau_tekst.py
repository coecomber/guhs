"""
Reisbureau "De Vadsvakantie": the twenty destinations (+ the proefreisje) and every Dutch text of the slice.
(biomes3 added the last one of each duration: bloesemmeertje, klaterdal, wolkenweide and japan; sixteen before.)
English: tools/lang/en/c38_px_reisbureau.json. The Java side (feature/guhpixel/reisbureau/Bestemming.java) has the same
ids, durations and chances: selfcheck() of guhpixel_reisbureau.py compares them.

A destination: (id, minutes, chance on the rare souvenir in %, name, "plek" (fits after "Op vakantie in ..."), the hover
explanation, the ansichtkaart in the guh's voice, (souvenir name, lore), (rare souvenir name, lore)).
The first one is the user's inside joke: its name and the picture's name are spelled exactly like this.
"""

UUR = 60
BESTEMMINGEN = [
    # ---- 1 hour (5%) ----------------------------------------------------------------------------------------------------
    ("lingsesdijk", 1 * UUR, 5, "Vadsen bij huize Lingsesdijk 86", "huize Lingsesdijk 86",
     "Een uurtje vadsen in de tuin van een huisje aan de dijk. Er gebeurt niks. Dat is het hele idee, njeg.",
     "Lieve baas! Ik lig in de tuin van huize Lingsesdijk 86. Er kwam een eend langs. Verder niks. Op dit gras kun je "
     "heel goed vadsen. Ik heb al drie dutjes gedaan en het is pas half twee. Njeg!",
     ("Schilderij \"Huize Lingsesdijk 86\"", "Een huisje aan de dijk, met een guh die slaapt in de tuin. Hang het aan de muur."),
     ("\"Huize Lingsesdijk 86\" in gouden lijst", "Hetzelfde schilderij, maar dan chic. De guh in de tuin slaapt er even hard om.")),
    ("kaasmarkt", 1 * UUR, 5, "Dagje Kaasmarkt", "het kaasmarktstadje",
     "Kaas kijken, kaas ruiken, kaas dragen en vooral: kaas proeven. Je guh komt rond terug.",
     "Hoi baas! Op de Kaasmarkt mag je alles proeven. Dat heb ik gedaan. Alles. De kaasdragers moesten mij daarna ook "
     "dragen. Ik neem een stapeltje kaaswielen mee, als ik ze onderweg niet opeet. Njeg!",
     ("Stapeltje kaaswielen", "Drie kaaswielen op elkaar. Niet opeten, het is een souvenir. Echt niet. Njeg."),
     ("Gouden kaasje", "De hoofdprijs van de Kaasmarkt. Blinkt als goud, ruikt als kaas.")),
    ("vadswoud", 1 * UUR, 5, "Middagdutje in het Vadswoud", "het Vadswoud",
     "Een bos waar zelfs de bomen gapen. Zacht mos, geen wekker.",
     "Dag baas! In het Vadswoud ligt overal mos. Ik ging er even op zitten en toen was het avond. De glimguhtjes deden het "
     "licht voor mij aan. Dit kussen van mos mocht mee naar huis. Vahoeg!",
     ("Moskussen", "Een kussen van echt Vadswoud-mos. Ruikt naar bos en naar dutjes."),
     ("Glimguhtjes-lantaarn", "Een lantaarntje vol glimguhtjes. Ze geven licht en doen soms ook een dutje.")),
    ("knuffeldal", 1 * UUR, 5, "Kinderboerderij Knuffeldal", "de kinderboerderij van Knuffeldal",
     "Schaapjes aaien in Knuffeldal. De schaapjes aaien terug.",
     "Hoi baas! Ik heb een schaapje geaaid. Toen aaide het schaapje mij. Toen vielen we samen in slaap in het hooi. De boer "
     "zei dat dat hier elke dag gebeurt. Njeg, wat een fijne plek!",
     ("Knuffelschaapje", "Een wollig schaapje uit Knuffeldal. Zegt geen mèh, maar is wel heel zacht."),
     ("Gouden bel", "De gouden bel van het liefste schaap van Knuffeldal. Tingeling, njeg.")),
    # biomes3
    ("bloesemmeertje", 1 * UUR, 5, "Hanami bij het Bloesemmeertje", "het Bloesemmeertje",
     "Een kleedje onder de bloesembomen aan het water. Kijken hoe de blaadjes vallen. Meer is het niet, njeg.",
     "Hoi baas! Ik lig op een kleedje onder een bloesemboom. Er vallen de hele tijd roze blaadjes op mijn neus. De koi "
     "kwamen kijken of ik koivoer was. De visser-guh zei: stilzitten is ook vissen. Dat kan ik heel goed. Njeg!",
     ("Bloesemtakje in een vaasje", "Een takje guhbloesem van het Bloesemmeertje. Het bloeit altijd, ook als het sneeuwt."),
     ("Koikommetje", "Een glazen kom met een piepklein gouden koitje erin. Het glinstert in de zon. Voeren hoeft niet.")),
    # ---- 2 hours (8%) ---------------------------------------------------------------------------------------------------
    ("guhwaii", 2 * UUR, 8, "Strandmiddag op Guhwai'i", "Guhwai'i",
     "Zon, zand en een handdoek. Surfen mag, liggen ook.",
     "Aloha baas! Ik wou gaan surfen, maar de handdoek lag zo lekker. Ik heb wel een zandkasteel gebouwd. Het heeft één "
     "kamer: een slaapkamer. De zee kwam kijken en vond het ook mooi. Njeg!",
     ("Zandkasteeltje", "Een kasteeltje van Guhwai'i-zand, met een vlaggetje erop. Eén kamer: een slaapkamer."),
     ("Gouden surfplankje", "Voor de beste surfer van de middag. Je guh lag er vooral op te zonnen.")),
    ("barbecuether", 2 * UUR, 8, "Wellness in de Barbecuether", "de Barbecuether",
     "Lekker warm stomen in de sauna van de Barbecuether. Je guh komt glanzend terug.",
     "Hallo baas! Hier is het overal lekker warm. Ik zat in de sauna tot ik helemaal zacht was. Daarna een bad met "
     "bubbels. Ik ruik nu een beetje naar pindasaus, maar dat hoort erbij. Vahoeg!",
     ("Sauna-emmertje", "Een houten emmertje met een lepel. Giet water op de stenen: tsss. Fijn, hoor."),
     ("Stomend badkuipje", "Een badkuipje dat altijd warm blijft. Er komt echte stoom vanaf.")),
    ("efteguh", 2 * UUR, 8, "Pretpark de Efteguh", "de Efteguh",
     "Sprookjes, achtbanen en een prullenbak die praat. Je guh gaat vooral in het bootje dat langzaam vaart.",
     "Hoi baas! In de Efteguh staat Holle Bolle Guh. Hij roept: papier hier, njeg! Ik heb hem al mijn papiertjes gegeven. "
     "De achtbaan heb ik overgeslagen, het bootje ging lekker langzaam. Ik viel in slaap bij de kabouters.",
     ("Holle Bolle Guh-prullenbak", "\"Papier hier, njeg!\" Hij heeft altijd honger, maar alleen naar papier."),
     ("Sprookjespaddenstoel", "Een rode paddenstoel met witte stippen. Als je goed luistert hoor je een muziekje.")),
    ("guhkenhof", 2 * UUR, 8, "Guhkenhof", "de Guhkenhof",
     "Bloemen zover je kunt kijken. Niet in de bloemen liggen. (Je guh ligt in de bloemen.)",
     "Dag baas! Zoveel bloemen! Op het bordje stond: niet in de tulpen liggen. Ik lag er dus naast. Mijn neus is geel van "
     "het snuffelen. Ik breng een vaasje mee voor op tafel. Njeg!",
     ("Tulpenvaas", "Een vaasje met roze tulpen van de Guhkenhof. Ze blijven altijd mooi."),
     ("Regenboogtulpen", "Tulpen in alle kleuren tegelijk. Niemand weet hoe ze het doen.")),
    # biomes3
    ("klaterdal", 2 * UUR, 8, "Theepauze in het Klaterdal", "het Klaterdal",
     "Thee drinken in het theehuisje bij de grote waterval. Het water klatert, je guh knikkebolt.",
     "Dag baas! In het Klaterdal klatert alles: de waterval, de rivier en mijn buik. Ik kreeg thee in het theehuisje op de "
     "rots en viel in slaap op de tatami. Een tanukiguh heeft mijn koekje opgegeten. Ik vond het goed. Vahoeg!",
     ("Bamboe-klatertje", "Een bamboebuisje dat volloopt, omkiept en tok zegt. Daar word je heel rustig van."),
     ("Mini-watervalletje", "Een watervalletje op een rots, klein genoeg voor op de kast. De nevel is echt.")),
    # ---- 8 hours (15%) --------------------------------------------------------------------------------------------------
    ("nomguh", 8 * UUR, 15, "Wintersport in Nomguh", "Nomguh",
     "Sleeën, sneeuw en warme chocolademelk. Je guh rolt meestal gewoon de berg af.",
     "Brrr, hoi baas! Nomguh is wit en koud en prachtig. Skiën lukte niet, dus ik ben de berg af gerold. Dat ging eigenlijk "
     "veel sneller. Daarna warme chocolademelk bij de kachel en een dutje van vier uur. Njeg!",
     ("Sneeuwbol", "Een sneeuwbol uit Nomguh met een klein huisje erin."),
     ("Sneeuwbol waarin het echt sneeuwt", "In deze sneeuwbol sneeuwt het altijd. Schudden hoeft niet, dat is fijn.")),
    ("guhrijs", 8 * UUR, 15, "Stedentrip Guhrijs", "Guhrijs",
     "De stad van de liefde, de stokbroodjes en de Eiffelknabbeltoren.",
     "Bonjour baas! Guhrijs is heel chic. Ik at een stokbrood dat langer was dan ik. De Eiffelknabbeltoren is helemaal van "
     "knabbels, maar je mag er niet van eten. Ik heb het gevraagd. Twee keer. Njeg!",
     ("Eiffelknabbeltorentje", "Een kleine Eiffelknabbeltoren. Ook hier mag je niet van eten."),
     ("Verlicht Eiffelknabbeltorentje", "Dit torentje geeft licht, net als de echte 's avonds. Très vads.")),
    ("camping", 8 * UUR, 15, "Camping De Vadsige Tent", "een tentje op Camping De Vadsige Tent",
     "Kamperen zoals het hoort: tent opzetten, erin gaan liggen, klaar.",
     "Hoi baas! De tent opzetten duurde een uur. Daarna was ik moe, dus ik ben erin gaan liggen. 's Avonds marshmallows "
     "boven het vuur. Er zat er één in mijn vacht. Die heb ik later gevonden. Lekker! Njeg!",
     ("Mini-tentje", "Een klein tentje. Er past precies één dutje in."),
     ("Kampvuurtje met marshmallow", "Een vuurtje dat nooit uitgaat, met een marshmallow die nooit aanbrandt.")),
    ("guhnetie", 8 * UUR, 15, "Guhnetië", "Guhnetië",
     "Een stad met water in plaats van straten. Je guh laat zich de hele dag rondvaren.",
     "Ciao baas! In Guhnetië hoef je nergens heen te lopen. Je gaat in een gondeltje liggen en iemand duwt. De gondelier "
     "zong een liedje. Ik heb meegezongen: njeeeeg! Hij vond het prachtig, denk ik.",
     ("Gondeltje", "Een gondeltje uit Guhnetië. Liggen maar, iemand anders duwt."),
     ("Carnavalsmasker", "Een deftig masker met veren en goud. Hang het aan de muur.")),
    # biomes3
    ("wolkenweide", 8 * UUR, 15, "Wolkjes kijken op de Wolkenweide", "de Wolkenweide",
     "Hoog boven alles op een zachte wolk liggen. Schaapjes tellen gaat hier vanzelf.",
     "Hoi baas, hier boven! Ik lig op een wolk. Hij is zachter dan mijn mandje, sorry. De wolkenschaapjes zweven voorbij "
     "en ik heb ze geteld: bij zeven sliep ik al. Ergens snurkt iets heel groots. Ik snurk gezellig mee. Njeg!",
     ("Wolkje in een potje", "Een echt wolkje van de Wolkenweide, in een glazen potje met een kurk. Niet openmaken, dan waait het weg."),
     ("Regenboogje in een potje", "Het eindje van een regenboog, gevangen in een potje. Het geeft zacht licht in alle kleuren.")),
    # ---- 24 hours (30%) -------------------------------------------------------------------------------------------------
    ("kaasmaan", 24 * UUR, 30, "Reis naar de Kaasmaan", "een raket naar de Kaasmaan",
     "Met de raket naar de maan. Die is echt van kaas. Je guh gaat dat heel goed controleren.",
     "Hallo baas, hier de maan! Hij is echt van kaas, ik heb het gecontroleerd. Heel vaak gecontroleerd. Er is nu een "
     "krater bij. Zweven is fijn: je ligt en toch ook weer niet. Tot morgen, njeg!",
     ("Maansteen van kaas", "Een echt stukje Kaasmaan, met gaatjes. Wetenschappelijk bewijs."),
     ("Mini-raket", "Een kleine raket, klaar voor vertrek. Drie, twee, één... dutje.")),
    ("wereldreis", 24 * UUR, 30, "Wereldreis in 80 dutjes", "de hele wereld",
     "De hele wereld rond in één dag. Dat kan alleen als je onderweg veel slaapt.",
     "Hoi baas! Ik ben de hele wereld rond geweest. Ik heb er weinig van gezien, want in de trein, de boot, de ballon en "
     "op de kameel kon je zo fijn slapen. Tachtig dutjes precies. Ik heb ze geteld. Njeg!",
     ("Wereldbol", "De hele wereld op een standaard. Je guh heeft overal een dutje gedaan."),
     ("Draaiende gouden wereldbol", "Een gouden wereldbol die uit zichzelf draait. Zo reis je zonder op te staan.")),
    ("cruise", 24 * UUR, 30, "Cruise over de Guhzee", "een hut op de Guhzee",
     "Een dag en een nacht op een groot schip met een buffet dat nooit sluit.",
     "Ahoi baas! Op het schip is een buffet dat nooit dichtgaat. Ik heb een ligstoel naast het buffet gezet. De kapitein "
     "zei dat ik de beste passagier ben, want ik val nooit overboord. Ik lig alleen maar. Njeg!",
     ("Scheepje in een fles", "Hoe komt dat scheepje erin? Je guh weet het ook niet."),
     ("Reddingsboei met gouden anker", "De reddingsboei van het cruiseschip, met een gouden anker. Hang hem aan de muur.")),
    ("balkonie", 24 * UUR, 30, "Thuisblijfvakantie \"Balkonië\"", "Balkonië",
     "De goedkoopste reis: 24 uur op je eigen bank. Je guh is wel echt even weg, hoor. Min of meer.",
     "Hoi baas! Ik ben op vakantie in Balkonië. Het lijkt heel erg op thuis. De bank is hetzelfde. De koelkast ook. Ik heb "
     "jou zien langslopen en gezwaaid, maar je zag me niet. Thuis is het ook vads. Njeg!",
     ("Schilderij \"Thuis is het ook vads\"", "Een guh op de bank, 24 uur lang. Hang het aan de muur."),
     ("Bordje \"Balkonië\"", "Een bordje voor naast de voordeur. Nu is je huis een vakantieland.")),
    # biomes3: the guh tags along with the two weebs of the weebhuisje (their own twelve-piece collection is another one)
    ("japan", 24 * UUR, 30, "Japan, met Evivads en Nielsvads", "Japan (met Evivads en Nielsvads)",
     "Je guh mag mee met de twee weebs uit het weebhuisje. Evivads: \"Kawaii, een reisgenootje!\" Nielsvads: \"We gaan met "
     "de trein. Een échte trein.\"",
     "Konnichiwa baas! Ik ben in Japan met Evivads en Nielsvads. Evivads riep bij elke bloesemboom: \"Kawaii!\" Nielsvads "
     "wilde alleen maar met de trein: \"Sugoi, dít is pas echt openbaar vervoer.\" We reden zeven keer hetzelfde rondje. "
     "Ik sliep in het bagagerek. Mata ne, tot morgen! Njeg!",
     ("Kogeltreintje", "Een wit kogeltreintje met een guhneus. Nielsvads zegt dat het precies op tijd rijdt, ook als het stilstaat."),
     ("Gouden Fuji-guh", "De beroemde berg, maar dan met guhoortjes en een mutsje van sneeuw. Evivads vond hem \"sugoi kawaii\".")),
]

# the proefreisje of the questline: five minutes, a postcard, no souvenir
PROEF = ("om_de_hoek", 5, 0, "Proefreisje om de hoek", "het straatje om de hoek",
         "Vijf minuutjes: tot aan de hoek en weer terug. Om te oefenen met weggaan en terugkomen.",
         "Hoi baas! Ik ben om de hoek. Het is hier bijna net als voor de hoek, maar dan iets verder. Ik heb een steentje "
         "gezien. Ik kom zo weer terug. Njeg!")

IDS = [b[0] for b in BESTEMMINGEN]

N = "reisbureau"
G = f"gui.guhs.{N}."
Q = f"quest.guhs.{N}."

TEXTS = {
    f"structure.guhs.{N}": "Reisbureau \"De Vadsvakantie\"",
    f"structure.guhs.{N}.tooltip": "Stuur je guh op vakantie! Hij komt terug met een ansichtkaart en een souvenir.",
    f"entity.guhs.guh_npc.{N}_agent": "Reisagent-guh",

    # ---- blocks and items -----------------------------------------------------------------------------------------------
    f"block.guhs.{N}_balie": "Reisbalie",
    f"block.guhs.{N}_balie.lore": "Stuur hier één van je guhs op vakantie. Hij komt terug met een ansichtkaart en een souvenir, "
                                 "en hij kan nooit kwijtraken. Njeg!",
    f"item.guhs.{N}_stempel": "Reisstempel",
    f"item.guhs.{N}_stempel.lore": "Van de Reisagent-guh. Hiermee maak je thuis een Reisbalie. Kwijt? De Reisagent heeft er nog één.",

    # ---- the trip screen ------------------------------------------------------------------------------------------------
    G + "titel": "Reisbureau \"De Vadsvakantie\"",
    G + "vandaag": "De reizen van vandaag",
    G + "nieuw_over": "Nieuwe reizen over %s",
    G + "duur": "Duur: %s",
    G + "waarschijnlijk": "Waarschijnlijk",
    G + "met_geluk": "Met geluk (%s%%)",
    G + "tip.waarschijnlijk": "Dit neemt je guh bijna zeker mee, samen met een ansichtkaart.",
    G + "tip.met_geluk": "Met een beetje geluk neemt je guh dit zeldzame souvenir mee, in plaats van het gewone.",
    G + "tip.heb_je": "Deze heb je al: een dubbele wordt een stempel op je reispas.",
    G + "tip.nieuw": "Deze heb je nog niet!",
    G + "tip.echte_tijd": "Reizen duren echte tijd. De reis loopt gewoon door als jij niet speelt.",
    G + "tip.proef": "Je eerste reisje. Je guh neemt alleen een ansichtkaart mee.",
    G + "kies_reis": "Kies een reis.",
    G + "kies_guh": "Wie mag er op reis?",
    G + "knop.boek": "Goede reis!",
    G + "knop.ophalen": "Ophalen",
    G + "knop.eerder": "Eerder terugroepen",
    G + "knop.eerder_zeker": "Zeker weten?",
    G + "tip.eerder": "Je guh komt meteen terug, maar zonder ansichtkaart en zonder souvenir. Hij vindt het niet erg.",
    G + "weg.kop": "Op vakantie",
    G + "weg.vertrekt": "%s pakt zijn koffertje...",
    G + "weg.onderweg": "%1$s is op vakantie in %2$s.",
    G + "weg.terug_over": "Terug over %s",
    G + "weg.klaar": "%s is terug en wacht bij de balie!",
    G + "weg.een_tegelijk": "Er mag één guh tegelijk op reis.",
    G + "reispas": "Reispas",
    G + "reispas.stempels": "Stempels: %1$s/%2$s",
    G + "reispas.uitleg": "Een dubbel souvenir wordt een stempel. Tien stempels: een Gouden koffertje!",
    G + "reispas.reizen": "Reizen: %s",
    G + "reispas.souvenirs": "Souvenirs: %1$s/%2$s",
    G + "slot.kop": "Nog even geduld, njeg",
    G + "slot.0": "Praat eerst met de Reisagent-guh in het Reisbureau \"De Vadsvakantie\" in de Guhmensie. Het Superkompas wijst de weg.",
    G + "slot.1": "De Reisagent-guh wil eerst dat het koffertje ingepakt is: breng hem de drie spulletjes.",
    G + "slot.2": "Eerst het proefreisje om de hoek, njeg. Daarna gaan alle reizen open.",
    G + "tijd.um": "%1$s u %2$s min",
    G + "tijd.u": "%s uur",
    G + "tijd.m": "%s min",
    G + "tijd.bijna": "nog heel even",
    G + "uit.ver": "Niet in de buurt. Zet je guh naast de balie.",

    # ---- messages -------------------------------------------------------------------------------------------------------
    G + "melding.geboekt": "%1$s gaat op reis: %2$s. Goede reis, njeg!",
    G + "melding.bezig": "Er is al een guh op reis. Eén tegelijk, njeg.",
    G + "melding.niet_vandaag": "Die reis is er vandaag niet meer. Kijk even opnieuw.",
    G + "melding.geen_guh": "Die guh is niet in de buurt van de balie.",
    G + "melding.guh_bezet": "Die guh kan nu niet weg: %s",
    G + "melding.eerst_agent": "Praat eerst met de Reisagent-guh.",
    G + "melding.nog_niet": "Je guh is nog onderweg. Nog even geduld!",
    G + "melding.niemand": "Er is niemand op reis.",
    G + "melding.al_thuis": "Je guh is al thuis, njeg.",
    G + "melding.geen_plek": "Hier is geen plekje vrij. Maak wat ruimte bij de balie.",
    G + "melding.terug": "%1$s is terug uit %2$s! Hij heeft een zonnebril op en een ansichtkaart voor je.",
    G + "melding.eerder": "%s is weer thuis. Zonder souvenir, maar wel blij je te zien.",
    G + "melding.geannuleerd": "De reis ging niet door: je guh was al weg voordat hij kon vertrekken. Hij is gewoon thuis.",
    G + "melding.souvenir": "Souvenir: %s",
    G + "melding.zeldzaam": "Wauw, een zeldzaam souvenir: %s!",
    G + "melding.stempel": "Die had je al: een stempel op je reispas! (%1$s/%2$s)",
    G + "melding.koffertje": "Je reispas is vol: je krijgt een Gouden koffertje! Vahoeg!",
    G + "melding.vast": "Deze balie hoort bij het Reisbureau. Maak er thuis zelf één met een Reisstempel.",
    G + "melding.staat_klaar": "%s is terug van vakantie en wacht bij een Reisbalie!",

    # ---- "Waar is mijn guh?" --------------------------------------------------------------------------------------------
    G + "plek.onderweg": "%1$s (terug over %2$s)",
    G + "plek.klaar": "%1$s (staat klaar bij een Reisbalie)",
    G + "dagboek.vakantie": "Ik ben op vakantie geweest: %s. Ik had een zonnebril op.",

    # ---- the ansichtkaart -----------------------------------------------------------------------------------------------
    G + "kaart.titel": "Ansichtkaart",
    G + "kaart.van": "Van: %s",
    G + "kaart.uit": "Uit: %s",
    G + "kaart.groetjes": "Veel vadsjes van",
    G + "kaart.lezen": "Rechtsklik om te lezen",
    G + "kaart.aan": "Aan mijn baas",
    G + "kaart.postzegel": "1 knabbel",
    G + "kaart.je_guh": "je guh",

    # ---- the Guhdex section ---------------------------------------------------------------------------------------------
    G + "gids.kop": "Reisbureau \"De Vadsvakantie\"",
    G + "gids.uitleg": "Stuur één guh tegelijk op vakantie bij een Reisbalie. Reizen duren echte tijd; je guh is veilig en "
                       "komt altijd terug, met een ansichtkaart en een souvenir.",
    G + "gids.zoek": "Zoek het Reisbureau \"De Vadsvakantie\" in de Guhmensie (Superkompas) en praat met de Reisagent-guh.",
    G + "gids.stap": "Reisagent-guh",
    G + "gids.stap.0": "Nog niet ontmoet",
    G + "gids.stap.1": "Koffertje inpakken: breng de drie spulletjes",
    G + "gids.stap.2": "Stuur een guh op het proefreisje",
    G + "gids.stap.3": "Klaar: je hebt de Reisstempel",
    G + "gids.nu": "Nu op reis",
    G + "gids.niemand": "Niemand",
    G + "gids.nu.onderweg": "%1$s: %2$s (terug over %3$s)",
    G + "gids.reizen": "Reizen gemaakt",
    G + "gids.stempels": "Stempels op de reispas",
    G + "gids.koffertjes": "Gouden koffertjes",
    G + "gids.kaarten": "Ansichtkaarten",
    G + "gids.souvenirs": "Souvenirs",
    G + "gids.album": "Het album",
    G + "gids.tip.gewoon": "Souvenir van: %s",
    G + "gids.tip.zeldzaam": "Zeldzaam souvenir van: %s",

    # ---- the Reisagent-guh ----------------------------------------------------------------------------------------------
    Q + "hallo": "Welkom bij Reisbureau \"De Vadsvakantie\", njeg! Ik ben de Reisagent-guh. Ik stuur guhs op vakantie. "
                 "Ze komen terug met een ansichtkaart, een souvenir en een zonnebril op. Wil jouw guh ook eens weg?",
    Q + "optie.ja": "Ja, graag!",
    Q + "optie.wat": "Hoe werkt dat?",
    Q + "optie.oke": "Oké, njeg!",
    Q + "optie.stempel": "Mijn Reisstempel is kwijt, njeg",
    Q + "uitleg": "Heel simpel! Je kiest een reis, je guh pakt zijn koffertje en weg is hij. Een reis duurt 1, 2, 8 of 24 "
                  "echte uren. Ook als jij niet speelt gaat de tijd door. Je guh is bij mij veilig: hij kan nooit kwijtraken. "
                  "Elke dag zijn er vier nieuwe reizen.",
    Q + "koffer": "Mooi! Maar eerst moet het koffertje ingepakt. Een guh gaat niet op reis zonder: %1$s kaasknabbels voor "
                  "onderweg, %2$s stuk wol als kussentje en %3$s papiertje voor de ansichtkaart. Breng je ze even?",
    Q + "koffer_nog": "Het koffertje is nog niet vol, njeg. Ik heb nodig: %1$s kaasknabbels, %2$s stuk wol (maakt niet uit "
                      "welke kleur) en %3$s papiertje.",
    Q + "koffer_klaar": "Knabbels, kussentje, papiertje... het koffertje is ingepakt! Nu een proefreisje: zet één van je "
                        "guhs hier bij de balie en stuur hem \"om de hoek\". Dat duurt maar vijf minuutjes.",
    Q + "proef_nog": "Het proefreisje staat klaar bij de balie hiernaast. Zet één van je guhs erbij en kies \"Proefreisje "
                     "om de hoek\". Vijf minuutjes, njeg!",
    Q + "proef_weg": "Je guh is nu om de hoek. Spannend, hè? Haal hem zo op bij de balie.",
    Q + "klaar": "Hij is terug! En? Hij heeft vast een steentje gezien. Hier is je Reisstempel: daarmee maak je thuis je "
                 "eigen Reisbalie. Dan hoef je niet steeds helemaal hierheen. Goede reis, njeg!",
    Q + "praatje.0": "Vandaag in de aanbieding: vier reizen! Morgen weer vier andere. Kijk maar bij de balie.",
    Q + "praatje.1": "Wist je dat een guh in Balkonië gewoon op je eigen bank ligt? Toch komt hij uitgerust terug.",
    Q + "praatje.2": "Een dubbel souvenir is niet erg: dat wordt een stempel op je reispas. Tien stempels en je krijgt een "
                     "Gouden koffertje van mij.",
    Q + "praatje.3": "De populairste reis? Vadsen bij huize Lingsesdijk 86. Er is daar niks te doen. Guhs vinden het prachtig.",
    Q + "stempel_nieuw": "Kwijt? Geeft niks, ik heb een hele la vol. Alsjeblieft, een nieuwe Reisstempel!",
    Q + "stempel_heb_je": "Je hebt je Reisstempel nog, njeg. Kijk maar in je zakken.",

    # ---- commands (dev) and sounds --------------------------------------------------------------------------------------
    f"subtitles.guhs.{N}.vertrek": "Guh gaat op reis",
    f"subtitles.guhs.{N}.terug": "Guh komt terug van vakantie",
    f"subtitles.guhs.{N}.stempel": "Stempel op de reispas",
    f"subtitles.guhs.{N}.balie": "Reisbalie-belletje",
}

EXTRA_BLOKKEN = {
    f"{N}_gouden_koffertje": ("Gouden koffertje", "Voor de echte wereldreiziger: tien stempels op je reispas. Er zit niks in, "
                                                  "maar hij blinkt prachtig."),
    f"{N}_koffertje": ("Koffertje", "Een klein koffertje vol stickers van verre landen. Staat gezellig in de gang."),
}


def kaart_naam(naam):
    return f"Ansichtkaart: {naam}"


def alle(texts=None):
    """Every Dutch text of the slice, including the per-destination ones."""
    t = dict(TEXTS if texts is None else texts)
    for (bid, _min, _kans, naam, plek, uitleg, kaart, souvenir, zeldzaam) in BESTEMMINGEN:
        t[G + f"bestemming.{bid}"] = naam
        t[G + f"bestemming.{bid}.plek"] = plek
        t[G + f"bestemming.{bid}.uitleg"] = uitleg
        t[f"book.guhs.{N}.kaart.{bid}"] = kaart
        t[f"item.guhs.{N}_kaart_{bid}"] = kaart_naam(naam)
        t[f"block.guhs.{N}_souvenir_{bid}"] = souvenir[0]
        t[f"block.guhs.{N}_souvenir_{bid}.lore"] = souvenir[1]
        t[f"block.guhs.{N}_zeldzaam_{bid}"] = zeldzaam[0]
        t[f"block.guhs.{N}_zeldzaam_{bid}.lore"] = zeldzaam[1]
    bid, _min, _kans, naam, plek, uitleg, kaart = PROEF
    t[G + f"bestemming.{bid}"] = naam
    t[G + f"bestemming.{bid}.plek"] = plek
    t[G + f"bestemming.{bid}.uitleg"] = uitleg
    t[f"book.guhs.{N}.kaart.{bid}"] = kaart
    t[f"item.guhs.{N}_kaart_{bid}"] = kaart_naam(naam)
    for blok, (naam, lore) in EXTRA_BLOKKEN.items():
        t[f"block.guhs.{blok}"] = naam
        t[f"block.guhs.{blok}.lore"] = lore
    return t
