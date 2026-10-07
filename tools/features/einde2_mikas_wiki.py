"""
einde2, slice mikas: the wiki texts of this slice (not a feature module: the wiki tools belong to another session, the merge
step feeds them this dict). Items, blocks and creatures get their own wiki page from the game data; this adds one system
page, where the Schuilguhs live, and a few paragraphs under existing pages.
"""

WIKI = {
    "systemen": {
        "knabbeljatten": (
            "De Ender-Mika en je knabbels", "entity/ender_mika",
            "Op de buiteneilanden van het Guheinde jat de Ender-Mika een kaasknabbel uit je hand. Hij geeft hem altijd terug: "
            "je moet alleen weten hoe.",
            ["wezens/ender_mika", "wezens/kaasmijt", "items/flitskruimel"]),
        "schuildoos": (
            "De Schuildoos", "block/schuildoos",
            "Een kaasdoos die zijn inhoud vasthoudt als je hem oppakt. Je maakt hem van twee doosschillen van een Schuilguh en een kist.",
            ["wezens/schuilguh", "items/doosschil", "blokken/schuildoos"]),
    },
    "entity_home": {
        "schuilguh": "bouwwerken/mika_vesting",
    },
    "tekst": [
        ("systemen/knabbeljatten", "Zo jat hij",
         "Een Ender-Mika die een kaasknabbel (gewoon of gefrituurd) in je hand ziet, komt alleen dichterbij als je NIET naar hem kijkt. "
         "Hij flitst achter je rug, pakt precies één knabbel en flitst weer weg. Meer dan één pakt hij nooit, en iets anders dan een "
         "knabbel ook niet. Daarna laat hij jou (en iedereen) een minuut met rust."),
        ("systemen/knabbeljatten", "Zo krijg je hem terug",
         "Kijk hem twee tellen strak aan: hij verstijft, begint te trillen en laat de knabbel vallen. Een mep of een duw werkt ook "
         "(hij duwt wel terug, hij blijft een Mika). Doe je niets, dan geeft hij het na twintig tellen zelf op. Loop je weg of ga je "
         "naar een andere wereld, dan zit de knabbel vanzelf weer in je zak. Kwijt raak je hem dus nooit."),
        ("systemen/knabbeljatten", "Een Kaasmijt als oppas",
         "Ender-Mika's zijn dol op Kaasmijten. Loopt er eentje rond, dan sjokken ze er verliefd achteraan en vergeten ze je knabbels. "
         "Een Kaasmijt springt soms uit een knabbelkoorvrucht waar je een hap van neemt. Hij kietelt wel je tenen: daar krijg je "
         "de hik van (onschuldige sprongetjes, acht tellen lang)."),
        ("systemen/schuildoos", "Doosschillen verzamelen",
         "Schuilguhs wonen in ronde kaasdoosjes aan de muren van een Mika-vesting. Geef er een een kaasknabbel en hij schuift een "
         "doosschil naar buiten, één keer per Minecraft-dag per Schuilguh. Met een lege hand krijg je een kusje: je zweeft drie "
         "tellen omhoog en landt zacht. Een Schuilguh kan nooit pijn hebben; schrikt hij, dan klapt zijn deksel even dicht."),
        ("systemen/schuildoos", "De doos",
         "Doosschil, kist, doosschil onder elkaar op de werkbank geeft een Schuildoos met 27 vakjes. Breek je hem af, dan blijft "
         "alles erin zitten en neem je de volle doos mee in één vakje. Een Schuildoos past niet in een andere Schuildoos."),
    ],
}
