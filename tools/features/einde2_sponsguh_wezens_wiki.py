"""
einde2 (sponsguh_wezens): what the wiki must say about the cast of the Sponsguh story, the buddy and the pet, the
Knabbelburgermobiel and the story's blocks and food. Not a feature module (not in FEATURES): the docs step reads WIKI and
turns it into pages (build contract 2.4). The item, block and creature pages come from the game data by themselves.

Phase A has no story: the pages below only say what EXISTS (what a thing does once you have it). How you get the buddy,
the pet, the clothes and the recipe is the story's (phase B): that module adds its own "verhalen" entry later.
"""
from features import einde2_sponsguh_wezens_tekst as tekst

CAST = ", ".join(naam for naam, _, _ in tekst.NAMEN.values())
# who is at home in Schelpenstad and along the road (everybody else: Bikinibodem)
SCHELPENSTAD = ("cycloop_mika", "david_hasselguh", "mindy_guh", "dennis_mika", "loopgraafmonster")

WIKI = {
    "systemen": {
        "sponsguh-maatje": (
            "Sponsguh als maatje", "entity:guhs:sponsguh",
            "Sponsguh Kaasbroek kan je maatje worden: een vierkant blok gatenkaas dat met je meeloopt, gemorst water en kaassaus "
            "opzuigt, elke dag een Knabbelburger voor je bakt en zo aanstekelijk lacht dat je guhs ervan gaan springen.",
            ["sponsguh", "gerrit", "knabbelburger", "krokante_grill"]),
        "gerrit-slakje": (
            "Gerrit de slak", "entity:guhs:gerrit",
            "Gerrit is de slak van Sponsguh, en hij kan jouw huisdiertje worden. Hij zegt njeg waar een poes miauw zou zeggen.",
            ["gerrit", "sponsguh"]),
        "knabbelburgers-bakken": (
            "Knabbelburgers bakken", "block:guhs:krokante_grill",
            "Een Knabbelburger maak je niet op een werkbank: die bak je op de krokante grill. Maar alleen als je het geheime recept "
            "kent, njeg.",
            ["krokante_grill", "knabbelburger", "besteltafel", "kaas_knabbels"]),
        "vruchthuis": (
            "Het vruchthuis", "block:guhs:vruchthuis_wand",
            "Sponsguh woont in een knabbelkoorvrucht. Met de vier vruchthuisblokken bouw je er zelf een: de schil als wand, het "
            "bladerkroontje als dak, een rond raam en een ronde deur.",
            ["vruchthuis_wand", "vruchthuis_dak", "vruchthuis_raam", "vruchthuis_deur", "gepofte_knabbelkoorvrucht"]),
    },
    "entity_home": {id: "bouwwerken/schelpenstad" if id in SCHELPENSTAD else "bouwwerken/bikinibodem" for id in tekst.NAMEN},
    "tekst": [
        ("sponsguh-maatje", "Wat hij doet",
         "Je maatje loopt met je mee en komt vanzelf weer naast je staan als je te ver weg bent, ook in een andere dimensie. Hooguit "
         "een keer per minuut zuigt hij binnen vier blokken een geknoeid blokje water of kaassaus op: een stromend blokje, of een los "
         "bronblokje waar geen ander bronblokje naast ligt. Een vijver, een gracht of een sausmeer laat hij met rust, en in een "
         "beschermd gebouw of in het gebied van andermans Guhhuisje zuigt hij niets op."),
        ("sponsguh-maatje", "Elke dag een Knabbelburger",
         "Een keer per Minecraft-dag, als je in de buurt bent, bakt hij een Knabbelburger voor je. Daar heeft hij het geheime recept "
         "niet voor nodig: hij IS de frietkok."),
        ("sponsguh-maatje", "Aaien en thuis laten",
         "Klik op hem en hij lacht: alle guhs binnen acht blokken springen mee. Sluip en klik, en hij blijft wachten waar hij staat; "
         "nog een keer en hij komt weer mee. Je hebt er precies een, en je kunt hem niet kwijtraken."),
        ("gerrit-slakje", "Njeg",
         "Gerrit kruipt rustig achter je aan en laat een spoortje slijm achter. Aai hem voor een njeg. Sluip en klik en hij blijft waar "
         "hij is. Ook van Gerrit heb je er precies een."),
        ("knabbelburgers-bakken", "Zo werkt de grill",
         "Zorg dat je een kaasknabbel en een brood bij je hebt en klik op de krokante grill. Na ongeveer vijf seconden springt er een "
         "Knabbelburger uit. Ken je het recept niet, dan sist de grill alleen maar en kost het je niets. De besteltafel ernaast heeft "
         "een belletje: ting!"),
        ("vruchthuis", "Bouwen",
         "Vier gepofte knabbelkoorvruchten geven vier vruchthuiswanden. Van de wand maak je op de steenzaag een dak of een raam, en "
         "van zes wanden drie ronde deuren. Zet er een spooklantaarn in voor griezelig groen licht en een bellenblaas voor je guhs: "
         "die jagen op de bellen."),
        ("bikinibodem", "Wie er wonen",
         f"De bewoners van het Sponsguh-verhaal: {CAST}. Guhs en Mika's door elkaar, en niemand doet iemand kwaad: de Mika's duwen "
         "hooguit, de monsters uit de loopgraaf willen vooral zingen."),
    ],
}
