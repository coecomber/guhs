"""
einde2 (appa): the wiki entries of the slice (build contract 2.4; not in FEATURES: the docs step of the merge reads WIKI).
The Appa-guh, the Momo-guh and the Appa-fluitje get their own wiki pages from the game data; this adds one system page
about travelling with the two and a few paragraphs for it.
"""

WIKI = {
    "systemen": {
        "appa-en-momo": (
            "Appa-guh en Momo-guh", "entity:guhs:appa_guh",
            "Je eigen luchtbizon met een guh-snoet en je eigen vliegende lemur: je krijgt ze in het Guhvatar-verhaal en je "
            "raakt ze nooit meer kwijt. De Appa-guh vliegt je overal heen, de Momo-guh raapt alles voor je op.",
            ["appa_guh", "momo_guh", "appa_fluitje", "guhvleugels", "enderguh"]),
    },
    "entity_home": {},
    "tekst": [
        ("appa-en-momo", "Vliegen op de Appa-guh",
         "Klik op je Appa-guh en je zit op zijn kop. Op de grond sjokt hij; druk op springen en hij roept jip jip en stijgt op. "
         "In de lucht vliegt hij waar jij kijkt: springen is omhoog, sluipen is omlaag, en als je loslaat blijft hij hangen. "
         "Raakt hij de grond, dan is hij geland. Afstappen kan alleen op de grond. Hij is langzamer dan Guhvleugels, maar hij "
         "wordt nooit moe en hij vliegt in elke dimensie."),
        ("appa-en-momo", "De zadelmand en de zadelkist",
         "In de zadelmand passen drie vrienden. Een ander mensje klikt gewoon op je Appa-guh. Je eigen guhs gaan mee als je "
         "klikt met een opgepakte guh in je hand of met guhs aan de lijn (de lijnen krijg je terug). Een tik tegen zijn flank "
         "en je guhs rollen er weer uit. Sluip + klik opent de zadelkist met 27 vakjes: alleen jij kunt erin."),
        ("appa-en-momo", "Het Appa-fluitje",
         "Blaas op het Appa-fluitje en je Appa-guh komt naar je toe, ook als hij heel ver weg is of in een andere dimensie "
         "staat. Zijn zadelkist en zijn naam komen altijd mee. Het fluitje kun je namaken van twee bamboe en een kaasknabbel, "
         "dus ook dat raak je niet kwijt."),
        ("appa-en-momo", "De Momo-guh",
         "De Momo-guh zit op je rechterschouder en raapt alles op wat binnen zes blokken ligt. Wat je zelf weggooit laat hij "
         "liggen. Ongeveer eens in de twintig minuten leent hij een kaasknabbel uit je tas; die geeft hij even later terug. "
         "Sluip + klik met een lege hand op een blok en hij blijft daar thuis zitten. Sluip + klik op hem, of sluip en blaas "
         "op het Appa-fluitje, en hij fladdert terug naar je schouder."),
    ],
}
