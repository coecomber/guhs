"""
bbq2 (bank): what the wiki must say about the Bank Guh's cap, its upgrade, the Hapluikje and the Banksleutel. Not a
feature module (not in FEATURES): the docs step (phase 3) reads WIKI and turns it into pages (CONTRACT_130 2.4).
The item / block pages of guhs:hapluikje, guhs:bank_sleutel and guhs:bank_upgrade come from the game data by themselves.

TO CORRECT in tools/make_wiki.py (reserved for phase 3, so not touched here): the Bank Guh's line still says
"Zo vadsig dat er oneindig veel spullen in zijn buikje passen" (around line 4847). It must become the text of
"bank-guh" below (256 of each kind; infinite only with the Bodemloos Knabbelmaagje).
"""
from features import bank, vadskracht

CAP = bank.cap()
VK = vadskracht.getal("HAPLUIKJE")

WIKI = {
    "verhalen": {},
    "systemen": {
        "bank-guh-buikje": (
            "Het buikje van de Bank Guh", "block:guhs:hapluikje",
            f"In het buikje van een Bank Guh past van elke soort hooguit {CAP}. Met het Bodemloos Knabbelmaagje van de uitvinder-guh "
            "past er oneindig veel in, en een Hapluikje stopt spullen van heel ver weg in je bank.",
            ["bank_guh", "hapluikje", "bank_sleutel", "bank_upgrade", "vadskracht", "knabbelbuizen"]),
    },
    "npc_home": {},
    "entity_home": {},
    "tekst": [
        ("bank-guh-buikje", "Vol is vol",
         f"Een Bank Guh is vadsig, maar niet bodemloos: van elke soort (een ding met dezelfde naam, betoveringen en slijtage) passen er "
         f"hooguit {CAP} in zijn buikje. Zijn scherm laat het zien: onder de spullen staat \"Hooguit {CAP} van elke soort\", een soort "
         f"die vol zit krijgt een rood getal en \"{CAP}/{CAP}\" in de tooltip. Wat er niet meer bij past houd je gewoon zelf: het blijft "
         "aan je muis, in je inventaris of in het werkbankrooster. Er raakt nooit iets kwijt. Hoeveel soorten erin gaan is niet begrensd."),
        ("bank-guh-buikje", "Had je al meer?",
         f"Een bank die al meer dan {CAP} van iets had (van voor deze update) houdt alles. Je kunt het er altijd uit halen. Er iets van "
         f"bij stoppen kan pas weer als er minder dan {CAP} in zit, of als je de bank opwaardeert."),
        ("bank-guh-buikje", "Het Bodemloos Knabbelmaagje",
         "De uitvinder-guh in de Oude Guhrad-centrale (Guhbarbecuether) geeft je na zijn klusjes het Bodemloos Knabbelmaagje. Klik ermee "
         "op een neergezette Bank Guh en van alles past er oneindig veel in. Het blijft voor altijd bij die bank, ook als je hem oppakt "
         "en ergens anders neerzet; een opgewaardeerde bank glinstert een beetje. Je kunt het niet zelf maken."),
        ("bank-guh-buikje", "Het Hapluikje en de Banksleutel",
         f"Het Hapluikje is een machientje met een bekje dat {VK} vadskracht nodig heeft. Klik met een Banksleutel eerst op je Bank Guh "
         "(de sleutel kent hem nu) en dan op het luikje. Alles wat je erin stopt, met de hand, een trechter, een Knabbelbuis, een "
         "klusjesguh of het Bezorgguhtje, ligt meteen in die bank. Hoe ver weg die ook staat, zelfs in een andere dimensie. Een luikje "
         "hoort bij een bank, een bank mag zoveel luikjes hebben als je wilt, en de sleutel raakt nooit op. Het luikje hapt alleen: er "
         "komt nooit iets uit. Het weigert (en jij houdt je spullen) als het geen vadskracht heeft, als de bank nergens staat (je hebt "
         "hem opgepakt) of als de bank vol zit met dat ding. Pak je de bank op en zet je hem ergens anders neer, dan vinden zijn "
         "luikjes hem vanzelf terug."),
        ("bank-guh-buikje", "Buizen en trechters",
         f"Knabbelbuizen en trechters kunnen altijd spullen IN een Bank Guh stoppen (tot {CAP} per soort). Er spullen UIT halen kan maar "
         "op één manier: met een Filterstuk aan een Knabbelbuis, tegen een bank met het Bodemloos Knabbelmaagje. Op het Filterstuk zet je "
         "wat eruit mag, en met 'laat liggen' hoeveel er van elk ding in de bank moet blijven. Een trechter eronder, een gewoon "
         "Richtingstuk of een ophaal-Haltepaaltje krijgt nooit iets uit een Bank Guh, ook niet uit een bank met het maagje: je voorraad "
         "loopt dus nooit per ongeluk leeg. Een Voorraadmeter kan altijd tellen wat erin zit."),
        ("bank-guh-buikje", "Klusjes",
         "Staat er een Bank Guh in de klus-area van een Guhhuisje, dan sorteren de bewoners alles erin. Zit de bank vol met iets, dan "
         "gaat de rest naar een tweede Bank Guh in de klus-area, anders naar de kist, anders naar de deur. Bij Opruimen blijft in de "
         "kist liggen waar de bank geen plek meer voor heeft."),
    ],
}
