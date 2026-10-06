"""
Wiki texts of the guh machines (bbq2, tech-machines). Not a feature module (not in FEATURES): the docs step of the merge
reads WIKI and turns it into pages (CONTRACT_130 2.4). The numbers come from VadsGetallen.java and from this slice's own
tables (tech_machines.MALEN), so the wiki never disagrees with the game.
"""
from features import tech_machines as tm, vadskracht as v

NAAM = {"minecraft:bone": "bot", "minecraft:bone_meal": "beendermeel", "minecraft:sugar_cane": "suikerriet", "minecraft:sugar": "suiker",
        "minecraft:blaze_rod": "blazestaf", "minecraft:blaze_powder": "blazepoeder", "minecraft:cobblestone": "keisteen",
        "minecraft:gravel": "grind", "minecraft:sand": "zand", "#guhs:knus/knabbelgraan": "knabbelgraan", "guhs:knabbelmeel": "knabbelmeel",
        "guhs:grillspies": "grillspies", "guhs:grillspiespoeder": "grillspiespoeder"}


def _malen():
    return "; ".join(f"{NAAM.get(i, i)} wordt {n} {NAAM.get(u, u)}" for i, u, n, _ in tm.MALEN)


WIKI = {
    "systemen": {
        "guhmachines": ("Guhmachines", "knabbelaar",
                        "Machines die het werk voor je doen: oogsten, knabbelen, neerzetten, knutselen, malen en bomen kweken. Ze lopen "
                        "allemaal op vadskracht en het zijn allemaal kleine guhs: een snoet, twee oortjes en iets dat beweegt als ze bezig zijn.",
                        ["systemen/vadskracht", "systemen/guhhuisje"]),
    },
    "tekst": [
        ("systemen/guhmachines", "Zo werkt een guhmachine",
         "Zet de machine neer met zijn snoet naar waar hij moet werken, en geef hem vadskracht: zet hem naast een Guhrad of sluit hem aan met "
         "Guhdraad. Slaapt zijn snoet, dan heeft hij geen vadskracht. Kijkt hij blij, dan werkt hij. Kijkt hij verbaasd, dan zit hij vol of "
         "kan hij niet verder: kijk naar hem en je leest waarom. Rechtsklik opent zijn buikje. Knabbelbuizen, trechters en klusguhs kunnen "
         "er ook bij: erin wat hij nodig heeft, eruit wat hij gemaakt heeft."),
        ("systemen/guhmachines", "Vadsmolen",
         f"Het grote zusje van het guh-molentje, twee blokken hoog. Haar wieken draaien op vadskracht ({v.getal('MOLEN')}) in plaats van op "
         f"wind, dus ze maalt altijd even hard: een knabbelgraan per tel. Ze maalt meer dan graan: {_malen()}."),
        ("systemen/guhmachines", "Oogster",
         f"Maait het rijpe gewas op het veld van 5 bij 5 voor zijn snoet en plant het meteen weer in ({v.getal('OOGSTER')} vadskracht). "
         "Tarwe, wortels, aardappels, bieten, kaasknabbelplantjes, netherwrat en cacao groeien gewoon opnieuw; van guhtuintjes, bessen en "
         "knabbelbessen plukt hij alleen de oogst; pompoenen en meloenen haalt hij weg en de stengel blijft; suikerriet, cactus en bamboe "
         "knipt hij af tot het onderste stuk. De oogst zit in zijn buikje. Zit hij vol, dan laat hij het gewas staan."),
        ("systemen/guhmachines", "Knabbelaar",
         f"Knabbelt het blok voor zijn snoet weg, ongeveer één per twee tellen ({v.getal('KNABBELAAR')} vadskracht). Hij bijt zo hard als een "
         "ijzeren houweel: steen, erts, hout en aarde gaan erdoor, obsidiaan niet. Wat hij loskrijgt zit in zijn buikje. Hij blijft van kisten, "
         "machines en alles met iets erin af, en ook van beschermde gebouwen en van het huisje van een ander. Njeg, netjes opgevoed."),
        ("systemen/guhmachines", "Neerzetter",
         f"Zet de blokken uit zijn buikje één voor één neer voor zijn snoet ({v.getal('NEERZETTER')} vadskracht). Zaadjes zet hij alleen op "
         "akkerland en zaailingen alleen op aarde, net als jij. Staat er al iets, dan wacht hij. Samen met een Knabbelaar of een Oogster maak je "
         "er een fabriekje van."),
        ("systemen/guhmachines", "Tekentafel en Bouwtekening",
         "Een Knutselmachine moet weten wat hij moet maken. Dat teken je één keer op een Tekentafel: leg het recept op het rooster, leg er een "
         "lege Bouwtekening bij (papier met blauwe kleurstof) en pak de tekening eruit. Tekenen kost alleen het vel: wat op het rooster ligt "
         "krijg je terug. Een tekening is precies: getekend met eikenplanken is geknutseld met eikenplanken. Een oude tekening kun je "
         "overtekenen. De tafel heeft geen vadskracht nodig."),
        ("systemen/guhmachines", "Knutselmachine",
         f"Leg links een Bouwtekening en hij knutselt wat erop staat, om de twee tellen één ({v.getal('KNUTSELMACHINE')} vadskracht). In de "
         "negen vakjes in het midden komt de voorraad: buizen en klusguhs mogen er alleen in stoppen wat op de tekening staat, en van elk "
         "ding niet te veel, zodat het ene het andere nooit in de weg zit. Rechts komt eruit wat klaar is, met de lege emmers erbij."),
        ("systemen/guhmachines", "Plantagebak",
         f"Een bak van 3 bij 3 vol aarde ({v.getal('PLANTAGEBAK')} vadskracht). Stop er een zaailing in en binnen een minuut staat er midden "
         "in de bak een echte boom. Dat werkt met elke zaailing: gewone bomen, guhbloesem, vadshout, bleekhout, sneeuwguhsparren, de sate- en "
         "worstzwammetjes en zelfs paddenstoelen. Soorten die alleen met z'n vieren groeien (donkere eik) krijgen er vanzelf drie bij als je er "
         "vier in stopt. Hak de boom gewoon om, of klik met een bijl op de bak: dan gaat de hele boom in één keer om, blaadjes en al, en de "
         "zaailingen die eruit vallen gaan meteen terug de bak in. Klusguhs uit een Guhhuisje kunnen het ook."),
    ],
}
