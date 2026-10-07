"""
einde2 (bouw_bikinibodem): what the wiki must say about Bikinibodem onder de leegte, the town itself (not its story: that is
a later phase). Not a feature module (not in FEATURES): the docs step reads WIKI and turns it into pages (build contract
2.4). The pages of the blocks and of the structure itself come from the game data.
"""
WIKI = {
    "systemen": {
        "bikinibodem": (
            "Bikinibodem onder de leegte", "block:guhs:zeepbel",
            "Een stadje op een schotel van kaaskorst, diep tussen de buiteneilanden van het Guheinde, onder een reusachtige zeepbel. "
            "Je komt er met de bubbelstroom vanaf het eilandje erboven, of door de stadspoort.",
            ["zeepbel", "bubbelstroom", "bloemwolk", "einde2_bouw_bikinibodem_koraal", "kruimelkwal", "schelpenstad"]),
    },
    "entity_home": {},
    "tekst": [
        ("bikinibodem", "Hoe kom je er?",
         "Het Superkompas (tabblad einde) wijst naar Bikinibodem. Het ligt laag: de eilanden zweven eromheen en erboven. Wie komt "
         "aanvliegen landt op het kleine eilandje boven de zeepbel. Daar staan twee kolommen van bubbels. Stap in de ene en je zakt "
         "rustig door de bel naar het Bellenplein; de andere brengt je weer omhoog. De zeepbel zelf is dicht. De enige andere weg "
         "naar binnen is de stadspoort aan de oostkant, waar de weg naar Schelpenstad begint."),
        ("bikinibodem", "De leegtemist",
         "Onder de zeepbel hangt paarse leegtemist. Die doet alsof hij zeewater is, maar je kunt er gewoon in ademen. In de hemel "
         "van de bel hangen bloemwolken die zachtjes licht geven, en op straat groeit gloeikoraal: zo is het er nooit donker."),
        ("bikinibodem", "Wat staat er allemaal?",
         "Rond het Bellenplein liggen De Krokante Knabbel (met de grill, de kassa en het kantoor met de kluis) en recht daartegenover "
         "De Vetemmer, een emmer met een lab erachter. Verder de Guhfy Guhber-ijssalon met een ijscoupe op het dak, de vaarschool "
         "van juf Pufguh met haar pylonenparcours, het stadion De Bellenkom, de boomkoepel van Sandy-guh met een echte boom uit de "
         "Guhmensie, en het rusthuis De Luie Schelp. Aan de Schelpstraat staan drie beroemde huizen naast elkaar: een "
         "knabbelkoorvrucht, een hoofd van steen vol zelfportretten en een kruimelrots. In de vier hoeken liggen de "
         "Kwallenvelden, waar zwermen Kruimelkwallen komen."),
        ("bikinibodem", "Mag je er bouwen?",
         "Nee. De stad is beschermd, net als de andere grote plekken van het Guheinde: je kunt er niets breken of plaatsen. Deuren, "
         "kisten en de lift werken gewoon."),
    ],
}
