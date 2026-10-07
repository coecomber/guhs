"""
einde2 (zwevers): what the wiki must say about the Kruimelkwallen, the Leegtewalvisguh and the four element animals. Not a
feature module (not in FEATURES): the docs step reads WIKI and turns it into pages (build contract 2.4). The creature pages
of the six, and the item / block pages of guhs:kruimelkwal_pot and guhs:leegteparel, come from the game data by themselves.

For the docs step: guhs:leegtewalvisguh_rug is no creature (the boxes of the whale's back): it gets no page.
"""

WIKI = {
    "verhalen": {},
    "systemen": {
        "kruimelkwallen": (
            "Kruimelkwallen", "entity:guhs:kruimelkwal",
            "Gloeiende kwalletjes van kaaskruimels die in zwermen door de Kruimelwolken dobberen. Je stuitert erop, en met een glazen "
            "fles vang je er een als lamp.",
            ["kruimelkwal", "kruimelkwal_pot", "kruimelwolken"]),
        "leegtewalvisguh": (
            "De Leegtewalvisguh", "entity:guhs:leegtewalvisguh",
            "Een reusachtige, zingende walvisguh die tussen de buiteneilanden zweeft. Je kunt op zijn rug landen. Je ziet hem pas "
            "als je het verhaal van de oude kapitein in de havenkroeg hebt aangenomen.",
            ["leegtewalvisguh", "leegteparel", "knabbelkoorvrucht", "leegtehaven"]),
        "elementdieren": (
            "De vier elementdieren", "entity:guhs:otter_pinguinguh",
            "Vleermuis-lemurguh (lucht), otter-pinguïnguh (water), das-molguh (aarde) en schildpad-eendguh (vuur): vier lieve "
            "mengeldieren die rond de plekken van hun element wonen.",
            ["vleermuis_lemurguh", "otter_pinguinguh", "das_molguh", "schildpad_eendguh"]),
    },
    "npc_home": {},
    "entity_home": {
        "vleermuis_lemurguh": "bouwwerken/westelijke_luchttempel",
        "otter_pinguinguh": "bouwwerken/noordelijke_waterstam",
        "das_molguh": "bouwwerken/omashguh",
        "schildpad_eendguh": "bouwwerken/vuurnatie_hoofdstad",
    },
    "tekst": [
        ("kruimelkwallen", "Boing!",
         "Kruimelkwallen wegen niks en je loopt er zo doorheen. Maar laat je er van boven op vallen en je stuitert een blok of vijf "
         "omhoog. Na een stuiter doet de volgende landing geen pijn, dus je kunt van kwal naar kwal springen. Sluip je, dan zak je er "
         "zachtjes doorheen. Ook guhs en andere dieren stuiteren."),
        ("kruimelkwallen", "Waar vind je ze?",
         "In zwermen van vier tot zeven in de Kruimelwolken, een zeldzame streek op de buiteneilanden van het Guheinde. Er drijven "
         "nooit meer dan twee zwermen bij je in de buurt. Loop je weg, dan drijven ze verder; later komen er nieuwe."),
        ("kruimelkwallen", "De kruimelkwalpot",
         "Klik met een glazen fles op een Kruimelkwal en je krijgt een kruimelkwalpot: een lamp (licht 12) die je op een blok zet of "
         "eronder hangt. Breek je de pot, dan zweeft de kwal weer vrij rond en krijg je je fles terug. Een vrijgelaten kwal blijft "
         "voorgoed op die plek hangen. Met zijdezacht houd je kwal en pot bij elkaar."),
        ("leegtewalvisguh", "Alleen voor wie hem gelooft",
         "De Leegtewalvisguh is er alleen voor spelers die het verhaal van de oude kapitein-guh in de havenkroeg van de Leegtehaven "
         "hebben aangenomen. Wie dat niet deed ziet niks, hoort niks en vliegt er dwars doorheen. Iedere speler apart: je vriend kan "
         "op zijn rug staan terwijl jij alleen lucht ziet."),
        ("leegtewalvisguh", "Op zijn rug",
         "Hij zweeft langzaam en altijd op dezelfde hoogte tussen de buiteneilanden. Zijn rug is plat en korstig, er groeit knabbelkoor "
         "op, en je kunt erop landen en rondlopen terwijl hij je meeneemt. Ook wat je berijdt en je eigen dieren blijven erop staan. "
         "Af en toe zingt hij een lied van vier lange noten."),
        ("leegtewalvisguh", "De leegteparel",
         "Geef hem een knabbelkoorvrucht (klik ermee op zijn rug) en hij bedankt je met een leegteparel. Dat doet hij voor elke speler "
         "een keer per Minecraft-dag; daarna eet hij wel, maar zingt hij alleen dankjewel. Temmen, duwen of pijn doen kan niet."),
        ("elementdieren", "Vleermuis-lemurguh (lucht)",
         "Fladdert in korte hupjes rond en hangt ondersteboven aan plafonds te dutten. Loop je er vlak langs, dan wordt hij wakker; "
         "sluip en hij slaapt door. Lust appels, bessen en knabbelkoorvruchten."),
        ("elementdieren", "Otter-pinguïnguh (water)",
         "Waggelt, en glijdt op ijs en sneeuw op zijn buik. Bij de ijsmuur van de Noordelijke Waterstam zijn het sleeën: ga erop "
         "zitten met een lege hand, kijk waar je heen wilt, zet af met vooruit en rem met achteruit. Op ijs en sneeuw houdt hij zijn "
         "vaart, bergaf gaat hij harder. Lust vis."),
        ("elementdieren", "Das-molguh (aarde)",
         "Blind, maar hij voelt je stappen en snuffelt jouw kant op (sluipen merkt hij niet). In zachte grond graaft hij zich in en "
         "tunnelt hij rond zonder een blok te veranderen. Lust kaasknabbels en bieten."),
        ("elementdieren", "Schildpad-eendguh (vuur)",
         "Dobbert op water en zoekt een vijver op als hij op het droge staat. Volgt iedereen die brood vasthoudt en eet het uit je "
         "hand. Bij schrik kruipt hij even in zijn schild."),
        ("elementdieren", "Waar vind je ze?",
         "Vooral in en rond de grote plekken van hun element. Heel af en toe loopt er een wilde rond in de streek van zijn element: "
         "de Kruimelwolken (lucht), de Schimmelkaasvlakte (water), de Kristalkaasklippen (aarde) en het Knabbelkoorbos (vuur). "
         "Temmen kan niet, aaien en voeren wel."),
    ],
}
