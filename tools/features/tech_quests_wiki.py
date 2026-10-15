"""
bbq2 (tech-quests): what the wiki step needs to know about features/tech_quests.py (CONTRACT_130 2.4). Not in FEATURES: the
docs step of the merge reads WIKI. Items, blocks, the structure and the NPC get their own page from the game data; the
machines themselves are described by the wiki dicts of the other tech modules (systemen/guhmachines, knabbelbuizen, saus...).
"""
from features import tech_quests_ftb, wereld

_F = tech_quests_ftb.LEVERINGEN


def _fase(i):
    (_, a, na), (_, b, nb) = _F[i]
    return f"{na} {a} en {nb} {b}"


WIKI = {
    "verhalen": dict([
        wereld.wiki_questlijn(
            "techniek", "De oefenhal van de Uitvinder-guh",
            "In de Oude Guhrad-centrale, diep in de Guhbarbecuether, rennen vijf oude guhs nog altijd hun rondjes. De Uitvinder-guh "
            "heeft er een oefenhal met vijf opstellingen, en ze zijn allemaal kapot. Je maakt ze een voor een: Guhdraad in een gat "
            "leggen, een te zware opstelling lichter maken, een Richtingstuk omdraaien, een Filterstuk goed zetten en "
            "een Sausslang aansluiten. Tussendoor wil hij vier zoutkristallen. Als alles weer werkt krijg je het Bodemloos "
            "Knabbelmaagje voor je Bank Guh en zijn drie receptkaarten.",
            "oude_guhrad_centrale", ["uitvinderguh"], [("guhs_techniek", "tech_quests_centrale")], related=["knabbelmachine", "mijnwerker"]),
        wereld.wiki_questlijn(
            "knabbelmachine", "De Grote Knabbelmachine",
            "Wie de oefenhal af heeft en de Aangebrande Mika heeft verslagen, mag met de Uitvinder-guh De Grote Knabbelmachine bouwen: "
            "een guh van koper en roze email, negen blokken hoog. Het is een bouwproject in vijf stappen waarvoor je bergen spullen "
            "brengt die een fabriek maakt. Iedere speler bouwt zijn eigen machine en ziet alleen die van zichzelf groeien. Als hij "
            "staat, ligt er elke dag één perfecte knabbel in het bakje onder zijn bek, en ben je Knabbelmachinist.",
            "oude_guhrad_centrale", ["uitvinderguh"], [("guhs_techniek", "tech_quests_knabbelmachine")], related=["techniek"]),
    ]),
    "systemen": {
        "guh_technologie": ("Guh-technologie: van rad tot fabriek", "structure_oude_guhrad_centrale",
                            "Guh-technologie gaat in vier stappen: Knutselen (meteen), Zout (zoutkristal), Saus (de receptkaarten van de "
                            "Uitvinder-guh) en Gloeister (na de Aangebrande Mika). Het questboek-hoofdstuk Guh-technologie is opgebouwd uit "
                            "projecten: na elk project werkt er iets vanzelf in je eigen wereld.", ["techniek", "knabbelmachine"]),
    },
    "npc_home": {"uitvinderguh": "bouwwerken/oude_guhrad_centrale"},
    "entity_home": {},
    "tekst": [
        ("guh_technologie", "De vier stappen",
         "Knutselen: het Guhrad, Guhdraad, de Guhoven, de Vadsmolen, de Knuffelgenerator en de Disco-dynamo maak je meteen. "
         "Zout: Knabbelbuizen, het Filterstuk, het Hapluikje, de Opzuiger, de Oogster, de sensoren en de Knabbelbatterij vragen "
         "zoutkristal uit de Guhbarbecuether. Saus: de pomp, het Sausvat, de Brouwautomaat, de Frituurautomaat, de Grillkoolpers, het "
         "Blubkacheltje, de Knabbelaar, de Neerzetter, de Knutselmachine, de Tekentafel en het Stepstation vragen een grillspies of "
         "blubroom én een receptkaart van de Uitvinder-guh (de Plantagebak: de kaart van de Grillcamping). Gloeister: de "
         "Gloeisterkern en De Grote Knabbelmachine vragen een gloeister van de Aangebrande Mika."),
        ("guh_technologie", "De projecten uit het questboek",
         "Nooit meer zelf bakken: een Guhoven aan een Guhrad, gevoerd door trechters of buizen. Alles vanzelf in de bank: een "
         "Hapluikje bij je akker, je mijn en je oven, met Knabbelbuizen en een Oogster ervoor. Een opslag die zichzelf sorteert: een "
         "buis langs je kisten met voor elke kist een Filterstuk. Saus uit de kraan: een Sauspomp, slangen en een Sausvat thuis. "
         "Drankjes die zichzelf brouwen, frituur aan de lopende band en een grillkoollijn: de drie sausmachines. Steen zonder houweel: "
         "een Knabbelaar voor een keisteenmaker. Een bos in een bak: de Plantagebak. Knutselen zonder handen: de Knutselmachine met een "
         "Bouwtekening. En de bezorgronde van het Bezorgguhtje voor alles wat te ver is voor een buis."),
        ("techniek", "De vijf opstellingen",
         "Opstelling 1, de losse draad: leg Guhdraad op de gele tegel tussen het rad en de Guhoven. Opstelling 2, te zwaar: twee "
         "ovens en een Vadsmolen vragen meer dan één rad geeft, dus alles staat stil; knip een draad door bij de rode wol. "
         "Opstelling 3, achterstevoren: sluip en klik met een lege hand op het Richtingstuk. Opstelling 4, het filter: het Filterstuk staat op "
         "'alles behalve' de knabbel, dus er rolt alleen papier door; zet het op 'alleen deze'. Opstelling 5, de slang: leg de Sausslang van de Uitvinder-guh "
         "in het gat tussen de pomp en het Sausvat."),
        ("techniek", "Voor iedereen opnieuw",
         "De vijf oude guhs in de raderen zijn van niemand: je kunt ze er niet uit halen. Een opstelling telt voor iedere speler die "
         "erbij staat en bij die stap is, dus je kunt het samen doen. Loopt iedereen weg, dan gaat de opstelling na een paar tellen "
         "vanzelf weer stuk voor de volgende speler. Het gebouw zelf is beschermd: alleen de aangewezen draden en het stukje dat je "
         "in een gat legde kun je weghalen."),
        ("techniek", "De receptkaarten",
         "Er zijn drie receptkaarten: saus en slangen, knabbelende machines en het Bezorgguhtje. Een kaart leg je linksboven in het "
         "werkbankrooster en hij blijft liggen. Wie zijn kaart kwijt is, sluipt en klikt op de Uitvinder-guh: hij verkoopt ze opnieuw "
         "voor een paar kaasknabbels."),
        ("knabbelmachine", "Wat de Uitvinder-guh nodig heeft",
         f"De fundering: {_fase(0)}. De ketel: {_fase(1)}. De maag: {_fase(2)}. De snoet: {_fase(3)}. Het gloeisterhart: {_fase(4)}. "
         "Je mag alles in porties brengen: de Uitvinder-guh pakt wat hij nodig heeft uit je zakken en telt het voor jou alleen. "
         "In de Guhdex (tab Verhalen) zie je hoeveel er nog moet komen."),
        ("knabbelmachine", "Elke dag een perfecte knabbel",
         "Klik op het bakje onder de bek van je eigen machine: één perfecte knabbel per dag. Hij vult je helemaal en geeft even "
         "regeneratie en snelheid. Het Knabbelmachine-beeldje kun je thuis neerzetten; klik erop en het knabbelt."),
    ],
}
