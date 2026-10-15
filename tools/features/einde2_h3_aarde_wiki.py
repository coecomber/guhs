"""
einde2 phase B, slice h3_aarde: what the wiki says about chapter 3 of the Guhvatar story and the things it gives. Not a
feature module (not in FEATURES): the docs step reads WIKI (build contract 2.4). The blocks get their pages from the game data.
No big spoilers: the page names the places and the games, not who betrays whom.
"""
WIKI = {
    "systemen": {
        "guhvatar_aarde": (
            "Guhvatar, hoofdstuk 3: Ba Sing Guh", "block:guhs:pai_sho_tafel",
            "Het derde hoofdstuk van De Legende van Guh speelt in Ba Sing Guh, de ommuurde stad van het Aarderijk. Je leert er "
            "aardesturen van De Blinde Bandiet, drinkt thee in De Jasmijn Draak en ontdekt dat er in de stad heel veel 'niet aan de "
            "hand' is.",
            ["pai_sho_tafel", "theeset", "einde2_h3_aarde_koolkraam", "aarderijk_hoed", "blinde_bandiet_riem", "einde2_bouw_aarde_aardemunt"]),
        "pai_sho": (
            "Guh Pai Sho", "block:guhs:pai_sho_tafel",
            "Het bordspel van Oom Miroh, in de eenvoudige guh-versie: om de beurt leg je een tegel op een bord van vijf bij vijf. "
            "Wie eerst een lotus heeft, wint: een eigen tegel met vier eigen tegels erboven, eronder, links en rechts.",
            ["pai_sho_tafel"]),
    },
    "entity_home": {},
    "tekst": [
        ("guhvatar_aarde", "Het hoofdstuk",
         "Hoofdstuk 3 begint zodra je het verhaal van de Noordelijke Waterstam hebt afgemaakt: Appa-guh brengt je naar de landingslob "
         "van Ba Sing Guh. Je haalt een stempel bij het paspoortloket, kijkt naar de Aardrommel, leert van Toph-guh blind aardesturen "
         "(de grond trilt en je ziet met je voeten), duwt de monorail naar de middenring en zet thee met Oom Miroh. Daarna wordt het "
         "spannend: Appa-guh verdwijnt, je sluipt langs de Dai Li-Mika's onder Meer Laogai, en onder het paleis valt de stad. Aan het "
         "eind gaat Toph-guh met je mee naar de Vuurnatie. Alles is per speler; niemand raakt gewond en mislukken kost nooit iets."),
        ("guhvatar_aarde", "Minispellen",
         "De blinddoekbaan: zes aardemunten in de vloer, te vinden met trillingen (bukken is stampen). De monorail: elke sprong is een duw. "
         "Thee zetten: drie keer klikken als het meterje in het groen staat (water, blaadjes, schenken). Pai Sho met Oom Miroh. En het "
         "sluipstuk onder Meer Laogai: elke wachter kijkt langzaam heen en weer; bukken maakt je stil, en gezien worden zet je alleen "
         "terug in de hal."),
        ("guhvatar_aarde", "Wat je overhoudt",
         "De theeset van Oom Miroh (een kopje jasmijnthee geeft een klein bonusje, en staat de set in je guhhuisje, dan komt het team op "
         "de thee), de speelbare Pai Sho-tafel (tegen een guh van het huis of tegen een vriend), de Aarderijk-hoed en de riem van De "
         "Blinde Bandiet voor je guhs, en, als je hem drie keer hebt zien breken, de kraam van de koolman."),
        ("pai_sho", "Spelen aan de tafel",
         "Klik op de tafel: je gaat zitten als eerste speler. Klikt binnen tien tellen iemand anders op de tafel, dan speel je tegen "
         "die speler; anders speelt een guh van het huis mee (nog eens klikken: meteen tegen een guh). Anderen die klikken, kijken mee."),
    ],
}
