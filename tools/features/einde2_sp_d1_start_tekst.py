"""
einde2 "Sponsguh", slice sp_d1_start: every Dutch text of part 1 steps 0-4 (the source; the English overlay is
tools/lang/en/c112_e2_sp_d1_start.json). Read by einde2_sp_d1_start.build(h).

  KAART    the narrator card of part 1 (verhaal_motor.vertelkaart; Java Verteller.registreer("einde2_sp_d1_start", 4, ...))
  SCENES   the six scenes {naam: (titel, {key: tekst}, {acteur: naam})} (Java D1Scenes; scene.guhs.einde2_sp_d1_start_<naam>.*)
  KIJK     the look scenes of the step hints {naam: (titel, regel)}
  TEXTS    everything else: the hints, the bubble letter, the grill game, the small talk of the town, the reward, the source
  FTB      the five quests (key, titel, tekst, icon, stap)

Tone (DESIGN 5b "Stijl"): pure comedy, warm and silly, a narrator with a French touch, guh details (oortjes, njeg). Every line
is in own words: no quote or lyric of the show.
"""

P = "gui.guhs.einde2_sp_d1_start"

KAART = ("Sponsguh Kaasbroek: Het geheime recept", [
    "Ah, Bikinibodem. Diep onder de buiteneilanden van het Guheinde ligt een schotel van kaaskorst, onder een zeepbel zo groot "
    "als een berg.",
    "Daar wonen Sponsguh Kaasbroek, zijn allerbeste vriend Patguh Ster en slak Gerrit, die alleen maar \"njeg\" zegt. Maar wel "
    "met gevoel.",
    "En daar staat De Krokante Knabbel, waar Meneer Krabguh de lekkerste burger van het Guheinde verkoopt. Het recept? Geheim, "
    "natuurlijk.",
    "Vandaag begint daar iemand aan een nieuw baantje. Raad eens wie, mon ami.",
])

V = {"verteller": "De Verteller"}

SCENES = {
    "brief": ("De bubbelbrief", {
        "plop": "Plop! De zeepbel knapt. Er dwarrelt een briefje uit, nog een beetje nat van de zeep.",
        "vacature": "Hallo, hallo, HALLO! Hier is Sponsguh Kaasbroek, met een VACATURE! De Krokante Knabbel in Bikinibodem "
                    "zoekt een nieuwe knabbelbakker!",
        "eisen": "Wat je nodig hebt: twee handen, twee oortjes en een hart vol knabbels. Guhvatars mogen ook! Ik heb al een "
                 "schortje voor je klaargelegd. Njeg!",
        "bikinibodem": "Ah, Bikinibodem... Een stadje diep onder de buiteneilanden, onder een zeepbel zo groot als een berg. "
                       "Daar bakken ze de lekkerste burger van het hele Guheinde. Allez, op weg!",
    }, dict(V, sponsguh="Sponsguh (in de brief)")),
    "aankomst": ("Bikinibodem", {
        "eiland": "Ah, daar is het: een eilandje bovenop een reusachtige zeepbel. En wat eronder zit? Kijk maar, mon ami.",
        "stad": "Bikinibodem! Paarse mist als zeewater, bloemwolken als lampjes en een bodem van knapperige kaaskorst. "
                "Iedereen kent er iedereen. En bijna iedereen is er lief.",
        "knabbel": "En daar, met het ronde dak: De Krokante Knabbel, het restaurant van Meneer Krabguh. Daar zoeken ze een "
                   "knabbelbakker... en dat ben jij.",
        "bubbel": "Die glinsterende kolom midden op het eilandje is de bubbelstroom. Stap erin en hij zet je zachtjes af op het "
                  "plein. Geen lift ter wereld is zo zacht, njeg.",
    }, V),
    "welkom": ("Welkom in Bikinibodem", {
        "hallo": "HALLO! Ben jij het? Jij bent het! Jij bent het echt! De nieuwe knabbelbakker!",
        "ik_ben": "Ik ben Sponsguh Kaasbroek! Vierkant, vol gaatjes, en al negenhonderd weken op rij Medewerker van de Week!",
        "njeg": "Njeg.",
        "gerrit": "Dat is Gerrit, mijn slak. Hij zegt dat je er heel bekwaam uitziet. En dat hij honger heeft. Dat zegt hij altijd.",
        "patguh": "Hoi! Ik ben Patguh. Ik woon onder een rots. Ik heb ook een baan, hoor: ik lig. Heel goed, zelfs.",
        "mee": "Kom mee, kom mee! Meneer Krabguh wacht in De Krokante Knabbel, hier oostelijk aan de Hoofdstraat! Hij houdt van "
               "nieuw personeel. En van geld. Vooral van geld.",
    }, {"sponsguh": "Sponsguh", "gerrit": "Gerrit", "patguh": "Patguh"}),
    "sollicitatie": ("De sollicitatie", {
        "tellen": "Achthonderdachtennegentig... achthonderdnegenennegentig... NEGENHONDERD knabbels! Kom maar bij papa, "
                  "mijn glimmende schatjes.",
        "sollicitant": "Huh?! Wie staat daar? Een klant? Nee... een SOLLICITANT! Ga zitten. Nee, staan. Zitten kost extra.",
        "vragen": "Eerste vraag: weet je wat een knabbel is? Precies: geld! Tweede vraag: werk je graag heel hard voor heel, "
                  "heel weinig knabbels?",
        "knik": "Je knikt. Je weet zelf niet precies waarom.",
        "aangenomen": "AANGENOMEN! Hier is je Krokante Knabbel-petje. Het petje gaat van je eerste loon af. Grapje! ...Half grapje.",
        "zucht": "Zucht. Nog een vrolijke. Precies wat deze zaak nodig had: nog meer vrolijkheid.",
        "grill": "Sponsguh staat in de keuken, links achterin. Hij leert je het vak. Hup, aan de grill! De klanten hebben honger "
                 "en hun knabbels branden in hun zak!",
    }, dict(V, krabguh="Meneer Krabguh", octo="Octo")),
    "bakken_uitleg": ("De grill-les", {
        "keuken": "Welkom in de keuken! En dit... is de grill. Zeg maar dag tegen de grill. Hij is mijn beste vriend. Na Patguh. "
                  "En Gerrit.",
        "bonnetje": "Een klant belt aan een tafel, en dan zweeft er een bonnetje boven de grill. Daarop staat de burger, van "
                    "onder naar boven. Het pijltje wijst wat er nu op moet.",
        "kratten": "Rond de grill staan vijf kratten: broodje, burger, kaas, zeesla en augurk. Klik op een krat en dat stukje gaat "
                   "op de burger. Precies in de volgorde van het bonnetje!",
        "fout": "Iets verkeerds erop? Plof, dan valt de burger plat. Geeft niks, gewoon opnieuw! Er is geen haast, alleen liefde. "
                "Vijf bestellingen en je bent een echte knabbelbakker.",
        "trots": "Ik ben zo trots. Ik moet even huilen. Nee, later. Eerst bakken!",
    }, {"sponsguh": "Sponsguh"}),
    "bakken_klaar": ("Een natuurtalent", {
        "natuurtalent": "VIJF! Vijf perfecte Knabbelburgers! Je bent een natuurtalent! Een knabbelwonder! Een... ik ben door "
                        "mijn woorden heen!",
        "twee": "Zucht. Fantastisch. Nu zijn er twee van.",
        "kwallen": "Hé! Hé! Ben je klaar met bakken? Ik heb netjes! Kom KWALLEN VANGEN! Kom dan, kom dan, kom dan!",
    }, {"sponsguh": "Sponsguh", "octo": "Octo", "patguh": "Patguh"}),
}

KIJK = {
    "bubbel": ("De bubbelstroom", "De bubbelstroom: stap erin, de ingang is aan de zuidkant."),
    "knabbel": ("De Krokante Knabbel", "De Krokante Knabbel, aan de Hoofdstraat."),
}

# small talk of the town while this slice's steps are on: "voor" = before and during the job hunt, "bakken" = while you bake
PRAAT_VOOR = {
    "sponsguh": "Meneer Krabguh zit in zijn kantoor, achterin rechts! Niet kloppen hoor, dat kost een knabbel.",
    "patguh": "Werken? Ik heb ook eens gewerkt. Het was vreselijk. Ik heb een kwartier wakker gelegen.",
    "gerrit": "Njeg. (Gerrit kijkt je aan alsof hij alles al weet.)",
    "meneer_krabguh": "Een klant! Of... nee? Kom je voor het baantje? Dan ben je te laat! Nee, wacht, je bent precies op tijd!",
    "parel_guh": "Papa zoekt altijd personeel. Hij betaalt in knabbels. Hele kleine knabbels.",
    "octo_mika": "Nee, ik ben niet de baas. Ja, ik ben wel ongelukkig. Het kantoor is achterin.",
    "octoliam": "Octoliam, aangenaam. Rijk, knap, en ik speel beter klarinet dan Octo. Zeg hem dat gerust.",
    "plankmika": "Het geheime recept... op een dag is het VAN MIJ! Muhahaha. Ahum. Goedemiddag, nieuwe burger.",
    "juf_pufguh": "Een nieuwe in de stad? Kun je varen? Nee? Gelukkig maar, njeg.",
    "sandy_guh": "Hallo daar! Ik ben Sandy-guh. Ik woon hier in een luchtkoepel. Lang verhaal. Heel lang verhaal.",
}
PRAAT_GUH = [
    "Welkom in Bikinibodem! De mist is paars, maar heel gezond.",
    "Heb je de Knabbelburger al geproefd? Ik eet er elke dag drie. Of vijf. Wie telt dat?",
    "Pas op voor Plankmika, die kleine. Hij wil altijd het geheime recept hebben, njeg.",
]
PRAAT_BAKKEN = {
    "sponsguh": "Kijk op het bonnetje boven de grill! Het pijltje wijst wat er nu op moet. Broodje eerst!",
    "patguh": "Is het al kwallentijd? Nee? ...En nu?",
    "meneer_krabguh": "Bakken! Elke seconde dat je kletst, kost mij een knabbel!",
    "parel_guh": "Papa zegt dat je heel snel bakt. Nou ja, hij zei: niet te traag.",
    "octo_mika": "Ik hoor de bel. Ik negeer de bel. Zo werkt dat hier.",
}

TEXTS = {
    # the hints of the five steps (StapHint; %s = the direction)
    f"{P}.hint.brief": "Er zweeft een zeepbel met een briefje naar je toe, ergens in het Guheinde. Klik erop als je hem ziet!",
    f"{P}.hint.reis": "Op naar Bikinibodem (%s)! Fluit Appa met je Appa-fluitje en kies Bikinibodem in zijn lijst, of volg het "
                      "Superkompas (tabblad einde). Land op het eilandje bovenop de grote zeepbel.",
    f"{P}.hint.bubbel": "Stap in de bubbelstroom: de glinsterende kolom in het buisje midden op het eilandje (%s). De ingang is "
                        "aan de zuidkant. Hij zet je zachtjes af op het plein van Bikinibodem.",
    f"{P}.hint.knabbel": "Loop over de Hoofdstraat naar De Krokante Knabbel (%s): het gebouw met het ronde dak. Ga naar binnen, "
                         "door de deur rechts achterin het kantoor in, en klik op Meneer Krabguh.",
    f"{P}.hint.grill": "Loop het kantoor uit en neem in de achterwand van de zaak de deur helemaal links: de keuken. Ga achter de "
                       "grill staan (%s), in de lichtstraal. Sponsguh wacht op je.",
    # the bubble letter
    f"{P}.brief.komt": "Er zweeft een zeepbel naar je toe... met een briefje erin!",
    f"{P}.brief.melding": "Een glinsterende zeepbel met een briefje erin zweeft op je af. Klik erop om hem te laten knappen.",
    f"{P}.brief.naam": "✉ Bubbelbrief",
    f"{P}.brief.weg": "De bubbelbrief drijft even weg... hij komt straks terug, njeg.",
    # the interview
    f"{P}.sollicitatie.klik": "Klik op Meneer Krabguh (rechtsklik) voor je sollicitatie",
    f"{P}.beloning.petje": "Je kreeg het Krokante Knabbel-petje! Kleding voor je guhs, njeg.",
    # the grill game
    f"{P}.bakken.titel": "Knabbelburgers bakken!",
    f"{P}.bakken.regel": "Klik de kratten aan in de volgorde van het bonnetje",
    f"{P}.bakken.bel": "Ding! Bestelling %1$s van %2$s: kijk op het bonnetje boven de grill",
    f"{P}.bakken.teller": "Bestelling %1$s/%2$s",
    f"{P}.bakken.volgende": "Nu: %s",
    f"{P}.bakken.bon.kop": "Bonnetje %1$s/%2$s",
    f"{P}.bakken.bon.onder": "van onder naar boven:",
    f"{P}.bakken.klant.0": "Tafel 1: een hongerige Bikinibodemguh",
    f"{P}.bakken.klant.1": "Tafel 2: een guh met een hoed (extra kaas, graag)",
    f"{P}.bakken.klant.2": "Tafel 3: een guh die \"iets groens\" wil",
    f"{P}.bakken.klant.3": "Tafel 1: een guh die van zuur houdt",
    f"{P}.bakken.klant.4": "Patguh, door het raam: DUBBEL! Dubbel dubbel!",
    f"{P}.deel.broodje": "Broodje",
    f"{P}.deel.burger": "Burger",
    f"{P}.deel.kaas": "Kaas",
    f"{P}.deel.zeesla": "Zeesla",
    f"{P}.deel.augurk": "Augurk",
    f"{P}.bakken.plof": "Plof! Dat stond niet op het bonnetje. Geeft niks: deze bestelling opnieuw.",
    f"{P}.bakken.plof_tip": "Tip: het pijltje op het bonnetje wijst het stukje dat er nu op moet. Je verliest nooit iets.",
    f"{P}.bakken.bestelling_klaar": "Bestelling %1$s/%2$s klaar!",
    f"{P}.bakken.wacht": "Even wachten op de volgende bel...",
    f"{P}.bakken.gestopt": "Het bakken is gestopt. Ga terug achter de grill om verder te bakken (je bestellingen blijven tellen).",
    f"{P}.bakken.naar_grill": "Ga achter de grill staan, in de lichtstraal",
    f"{P}.bakken.verder": "Je had al %1$s van de %2$s bestellingen klaar. Verder bakken!",
    f"{P}.bakken.klaar.titel": "Vijf Knabbelburgers!",
    f"{P}.bakken.klaar.onder": "Je bent een echte knabbelbakker",
    f"{P}.bakken.tijd": "Je tijd: %1$s (je beste: %2$s). Gewoon voor de lol: er was geen haast.",
    f"{P}.bakken.tijd.record": "Je tijd: %1$s. Dat is je beste tijd tot nu toe!",
    # the clothes source of the cap
    "gui.guhs.kledingbron.einde2_sp_d1_start": "Sponsguh-verhaal: je sollicitatie bij De Krokante Knabbel",
}
for _k, _v in PRAAT_VOOR.items():
    TEXTS[f"{P}.praat.voor.{_k}"] = _v
for _i, _v in enumerate(PRAAT_GUH):
    TEXTS[f"{P}.praat.voor.bikinibodem_guh.{_i}"] = _v
for _k, _v in PRAAT_BAKKEN.items():
    TEXTS[f"{P}.praat.bakken.{_k}"] = _v

FTB = [
    ("einde2_sp_d1_start_r0", "De bubbelbrief", "Wie het Guhvatar-verhaal heeft uitgespeeld, krijgt in het Guheinde post: een "
     "&bzeepbel&r met een briefje erin zweeft op je af. Klik erop!", "minecraft:paper", 0),
    ("einde2_sp_d1_start_r1", "Naar Bikinibodem", "Fluit &dAppa&r en vlieg naar &bBikinibodem&r. Land op het eilandje bovenop "
     "de grote zeepbel.", "guhs:zeepbel", 1),
    ("einde2_sp_d1_start_r2", "De bubbelstroom", "Stap in de &bbubbelstroom&r: hij zet je zachtjes af op het plein van "
     "Bikinibodem, waar iemand heel vierkants op je wacht.", "guhs:bubbelstroom", 2),
    ("einde2_sp_d1_start_r3", "Een baantje", "Solliciteer bij &6Meneer Krabguh&r in &6De Krokante Knabbel&r. Neem geen geld "
     "mee: dat houdt hij.", "guhs:meneer_krabguh_spawn_egg", 3),
    ("einde2_sp_d1_start_r4", "Knabbelburgers bakken", "Sponsguh leert je het vak. Bak &6vijf Knabbelburgers&r in de volgorde "
     "van het bonnetje. Niemand haast je.", "guhs:knabbelburger", 4),
]
