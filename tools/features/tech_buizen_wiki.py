"""
Wiki texts of the Knabbelbuizen, the Opzuiger and the sensors (bbq2, tech-buizen). Not a feature module (not in FEATURES):
the docs step of the merge reads WIKI and turns it into pages (CONTRACT_130 2.4). The numbers come from VadsGetallen.java
(vadskracht.getal) and from feature/techbuis/Buizen.java.
"""
import os
import re

from features import vadskracht as v


def _buis(naam):
    """A number of feature/techbuis/Buizen.java (so the wiki never disagrees with the code)."""
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "techbuis", "Buizen.java"), encoding="utf-8").read()
    m = re.search(r"\b" + naam + r"\s*=\s*([0-9]+)", src)
    if not m:
        raise SystemExit(f"tech_buizen_wiki: Buizen heeft geen {naam}")
    return int(m.group(1))


P = "systemen/knabbelbuizen"
S = "systemen/sensoren"

WIKI = {
    "systemen": {
        "knabbelbuizen": ("Knabbelbuizen", "knabbelbuis",
                          "Doorzichtige buizen waar je je spullen doorheen ziet rollen: van kist naar machine, van machine naar de "
                          "Bank Guh. Een Richtingstuk hapt de spullen eruit, een Filterstuk sorteert ze, en de Opzuiger raapt op wat "
                          "er op de grond ligt.",
                          ["systemen/vadskracht", S]),
        "sensoren": ("Sensoren en de Guhklok", "snuffelsensor",
                     "Vier kleine guhmachientjes die een redstonesignaal geven: de Voorraadmeter telt wat er in een kist zit, de "
                     "Snuffelsensor ruikt wie er in de buurt is, de Guhklok tikt en de Guhteller telt de tikken.",
                     ["systemen/vadskracht", P]),
    },
    "tekst": [
        (P, "Hoe het werkt",
         "Een Knabbelbuis plakt vanzelf vast aan elke buis ernaast en aan alles waar spullen in kunnen: een kist, een oven, een "
         "guhmachine, de Bank Guh, een Hapluikje. Zelf doet een buis niks. Het werk doet het Richtingstuk: zet het met zijn "
         "achterkant tegen een kist en het hapt er elke keer één ding uit en stuurt dat de buis in, de kant van de pijl op. "
         "Elk ding kiest meteen waar het heen gaat: de dichtstbijzijnde plek waar het in past. Past het nergens, dan blijft het "
         "gewoon in de kist liggen."),
        (P, "Het Richtingstuk",
         "Klik met het Richtingstuk op een kist en de pijl wijst van de kist af: hij haalt de kist leeg. Sluip je tijdens het "
         "plaatsen, dan wijst de pijl juist de kist in. Staat hij verkeerd om? Sluip en klik erop met een lege hand, dan draait hij "
         "om. Tussen twee buizen is een Richtingstuk eenrichtingsverkeer: tegen de pijl in komt er niks door. Klik erop met een "
         "lege hand en je leest wat hij aan het doen is. Een Richtingstuk heeft geen vadskracht nodig. Een trechter mag zijn "
         "spullen ook in de achterkant van een Richtingstuk duwen."),
        (P, "Het Filterstuk: sorteren",
         f"Het Filterstuk is een Richtingstuk met een kieskeurig guhtje erin. Het wil {v.getal('BUISFILTER')} vadskracht en hapt "
         f"met grote happen ({_buis('HAP_FILTER')} dingen per keer in plaats van {_buis('HAP_RICHTING')}). Klik erop en je ziet zijn "
         "lijstje: negen vakjes. Klik met een ding op een vakje en dat ding staat erop (je houdt het zelf). Het Filterstuk laat "
         "dan alleen die dingen door, of juist alles behalve die. Zet een Filterstuk vlak voor een kist en die kist krijgt die "
         "dingen het eerst, ook als een andere kist dichterbij staat. Zo sorteer je: per soort een kist met een Filterstuk ervoor, "
         "en aan het eind een kist zonder, voor de rest. Zonder vadskracht slaapt het guhtje en laat het niks door."),
        (P, "Laat er een paar liggen",
         "Een Filterstuk dat uit een kist of de Bank Guh hapt kan van elk ding een aantal laten liggen: 'Laat liggen: 16' betekent "
         "dat er altijd minstens zestien van achterblijven. Handig als je je machines wilt voeren zonder dat je voorraad opraakt. "
         "Uit een Bank Guh happen kan alleen een Filterstuk, en alleen als de bank het Bodemloos Knabbelmaagje heeft: een gewoon "
         "Richtingstuk of een trechter krijgt er nooit iets uit. Erin stoppen mag altijd."),
        (P, "Op slot met redstone",
         "Een redstonesignaal zet een Richtingstuk of Filterstuk op slot, net als bij een trechter. Zo laat je een sensor de buis "
         "aan- en uitzetten. Guhdraad telt niet mee: dat geeft ook een redstonesignaal, maar daar zit je Filterstuk gewoon aan vast."),
        (P, "De Opzuiger",
         f"De Opzuiger slurpt alle losse spullen binnen vier blokken naar zijn snoet en bewaart ze in negen vakjes. Hij wil "
         f"{v.getal('OPZUIGER')} vadskracht. Zet er een Richtingstuk of een trechter aan om hem leeg te halen. Zit hij vol, dan kijkt "
         "hij verbaasd. Geleende spullen en spullen in het gebied van andermans Guhhuisje laat hij netjes liggen."),
        (P, "Niks raakt kwijt",
         "Een ding dat onderweg is en niet meer past waar het heen ging, komt terug en zoekt een andere plek, of gaat terug in de "
         "kist waar het uit kwam. Breek je een buis terwijl er iets doorheen rolt, dan gebeurt hetzelfde. Breek je het Richtingstuk "
         "zelf, dan valt alles wat nog onderweg was eruit op de grond."),
        (S, "De Voorraadmeter",
         "Zet de Voorraadmeter met zijn rug tegen een kist, een machine of de Bank Guh (erop of eronder mag ook). Klik erop, geef "
         "hem een voorbeeld en een aantal, en hij geeft een redstonesignaal zodra er minstens zoveel in zit. Draai het om en hij "
         "geeft juist een signaal als er minder is. Zonder voorbeeld telt hij alles. Een comparator leest hoe ver hij is."),
        (S, "De Snuffelsensor",
         "Een neus die ruikt wie er in de buurt is. Klik om te kiezen: guhs, Mika's, spelers of allemaal. Sluip en klik om te "
         "kiezen hoe ver hij ruikt: twee, vier of acht blokken. Ruikt hij iemand, dan geeft hij een signaal; een comparator leest "
         "hoeveel het er zijn."),
        (S, "De Guhklok en de Guhteller",
         "De Guhklok geeft om de zoveel tijd een kort redstonetikje: elke tel, elke twee, vijf, tien of dertig tellen, elke minuut "
         "of elke vijf minuten. Hij kan ook een signaal geven zolang het dag is, of zolang het nacht is. De Guhteller telt de "
         "tikjes die aan zijn achterkant binnenkomen en geeft bij de zoveelste zelf een tikje: zet hem achter een Guhklok en je "
         "hebt 'elke tiende keer'. Klik om te kiezen tot hoeveel hij telt."),
        (S, "Vadskracht en uitlezen",
         f"Elke sensor wil {v.getal('SENSOR')} vadskracht en heeft een snoet: hij slaapt zonder vadskracht en kijkt verbaasd zolang "
         "hij zijn signaal geeft (dan brandt ook zijn lampje). Kijk naar een sensor en je leest wat hij telt, ruikt of tikt, net "
         "als bij de vadskracht."),
    ],
}
