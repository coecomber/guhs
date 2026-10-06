"""
bbq2 (ring-h6) - the wiki entry of chapter 6 of the Knabbelring, "De Frituurberg" (CONTRACT_130 2.4; the docs step of phase 3
turns this into pages; this module is not in FEATURES and builds nothing).
"""
from features import wereld

WIKI = {
    "verhalen": dict([wereld.wiki_questlijn(
        "ring_h6", "De Frituurberg",
        "Het laatste hoofdstuk van In de ban van de Knabbelring. In de Rookdelta staat de Frituurberg: een vulkaan met een korst van "
        "gefrituurd beslag en een reusachtig frituurmandje boven de krater. Je klimt het Kronkelpad op terwijl de berg met kooltjes gooit, "
        "bevrijdt drie Rookguhjes die de Mika's als afzuigkap gebruiken, trekt jezelf met het Elfentouw langs de westwand omhoog en laat je "
        "het laatste stuk door Sam-guh dragen. Boven grijpt Smikagol de ring, valt in de frituur en komt er goudbruin weer uit: de ring "
        "wordt niet vernietigd maar gedeeld. Zelfs het Oog van Sausron krijgt een stukje.",
        "frituurberg", ["smikagol", "araguh", "guhdalf"], [("guhs_knabbelring", "ring_h6")], related=["verhalen/knabbelring"])]),
    "systemen": {},
    "npc_home": {},
    "entity_home": {"ringh6_rookguh": "verhalen/ring_h6", "ringh6_krokante_smikagol": "verhalen/ring_h6", "ringh6_valkool": "verhalen/ring_h6"},
    "tekst": [
        ("verhalen/ring_h6", "Waar staat de berg?",
         "Eén Frituurberg per wereld, in de Rookdelta, een paar honderd blokken voorbij de Zwarte Roosterpoort. Tot je verhaal daar is zie je "
         "alleen Guhdalfs sluier. De berg staat op een asvlakte net boven de frituurzee; aan de vier kanten loopt de vlakte tot de rand, daar "
         "kun je naartoe lopen of een bruggetje bouwen. Het basiskamp met het Rustvuurtje ligt aan de zuidkant."),
        ("verhalen/ring_h6", "De klim",
         "Vier stukken, elk met een Rustvuurtje: het Kronkelpad (lopen, kooltjes ontwijken), de westwand (twee haken met het Elfentouw: onder "
         "elke haak hangt een blauw lampje, bij de paal met het blauwe lampje gooi je), het smalle pad naar de oostkant met de laatste haak, en "
         "het laatste stuk naar de Frituurspleet. Op elke richel staat een kooi met een Rookguhje: klik op het slot. Iedere speler bevrijdt "
         "zijn eigen Rookguhjes; een vriend kan dat niet voor je doen."),
        ("verhalen/ring_h6", "Niets doet pijn",
         "Kooltjes duwen alleen. Vallen en in de frituur terechtkomen doet geen schade: na een lange val of een duik sta je weer bij je "
         "laatste Rustvuurtje. Op de hele berg heb je geen last van vuur."),
        ("verhalen/ring_h6", "De ring wordt zwaar",
         "Hoe hoger je komt, hoe langzamer je loopt. Na het derde Rookguhje is de ring zo zwaar dat je bijna stilstaat: klik op Sam-guh en hij "
         "draagt je naar boven. Lopen mag ook, het duurt alleen even."),
        ("verhalen/ring_h6", "Smikagol",
         "Op de haak-stukken sluipt Smikagol af en toe naar je toe en graait naar de ring. Hij waarschuwt eerst; stap weg of gebruik het "
         "Lichtflesje, dan houdt hij een minuut zijn pootjes thuis. Raakt hij je, dan krijg je alleen een duwtje."),
        ("verhalen/ring_h6", "Het einde",
         "Op het Bakrandje boven de frituur speelt de finale (niet over te slaan, terug te kijken in de Guhdex). Daarna vliegen de Rookguhs je "
         "naar de Guhmensie, bij het grillportaal waar je vertrok, en daar is het feest: Araguh wordt gekroond met een kroon van knabbels. Je "
         "krijgt een stukje gefrituurde Knabbelring en alle beloningen van het verhaal (zie In de ban van de Knabbelring)."),
    ],
}
