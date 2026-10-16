"""
einde2 (bouw_lucht): what the wiki may say about the Zuidelijke Luchttempel. Not a feature module (not in FEATURES): the
docs step reads WIKI and turns it into pages (build contract 2.4). The temple is a story structure: hidden until a player
gets there in the story, so these texts tell what the place is like and give nothing of the story away. The pages of the
blocks come from the game data.
"""
WIKI = {
    "systemen": {
        "zuidelijke_luchttempel": (
            "De Zuidelijke Luchttempel", "block:guhs:einde2_bouw_lucht_tegel",
            "Een witte tempel met blauwgroene daken op een eigen zwevende rots, hoog boven de buiteneilanden van het Guheinde. "
            "Hier woonden de luchtnomaden: guhs die vonden dat je ook in de wind kunt vadsen.",
            ["einde2_bouw_lucht_tegel", "einde2_bouw_lucht_pijltegel", "einde2_bouw_lucht_taart", "einde2_bouw_lucht_tandwiel",
             "einde2_bouw_lucht_blauwdruk", "werkplaatsdeur", "vleermuis_lemurguh"]),
    },
    "entity_home": {"vleermuis_lemurguh": "bouwwerken/zuidelijke_luchttempel"},
    "tekst": [
        ("zuidelijke_luchttempel", "Hoe kom je er?",
         "De tempel staat in geen enkel kompas. Je komt er vanzelf als het zover is. Hij zweeft hoog boven alles wat er op de "
         "buiteneilanden groeit, dus je landt er: op het landingsterras aan de zuidkant, onder aan de trap naar de tempelpoort. "
         "Van de tempel breek je niets af en je zet er niets neer: hij blijft heel voor iedere guh die na jou komt."),
        ("zuidelijke_luchttempel", "Wat is er te zien?",
         "Achter de poort ligt het voorplein, en daarachter op zijn bordes de Hal van de Guhvatars: tien beelden van steen, elk met een "
         "bordje, en in het midden een leeg voetstuk. Op het dak van de hal staat de grote toren, met een tempelbel, een kamer die "
         "al honderd jaar niet is opgeruimd, een theekamer en helemaal boven de uitkijk. Ten noorden van de hal staan de "
         "monnikentoren (keuken, slaapzalen, een kamer om te mediteren, de windgong) en de windtoren (fruitkelder, bibliotheek, "
         "kaartenkamer, windfluiten). Van allebei loopt een brug naar het dak van de hal. Daartussen ligt een grote stal, open "
         "naar het noorden."),
        ("zuidelijke_luchttempel", "Oefenen als een luchtnomade",
         "In het westen ligt het luchtbalveld: een verdiept veld vol palen in een bed van kruimelwolk. Wie van een paal valt zakt "
         "zacht door de wolk en loopt via de trap weer naar boven. Erboven ligt het taartenterras. Langs de rand van de rots loopt "
         "de luchtscooter-baan, van het zuidwesten om het noorden heen naar het noordoosten. En vanaf het zuidbalkon "
         "van de grote toren hangen zes gloeiende zweefringen in een boog naar het westen, boven het plein."),
        ("zuidelijke_luchttempel", "De werkplaats",
         "In het oosten staat een werkplaats met een dichte deur. Op het bordje staat: nog niet af, njeg. Daarnaast ligt een "
         "ommuurde bouwplaats, open naar de lucht. Wat daar gebouwd wordt zie je vanzelf, als het zover is."),
    ],
}
