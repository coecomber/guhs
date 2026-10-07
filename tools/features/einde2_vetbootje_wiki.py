"""
einde2 (vetbootje): what the wiki must say about the player's own vetbootje and the personal heteluchtballon. Not a
feature module (not in FEATURES): the docs step reads WIKI and turns it into pages (CONTRACT_E2 2.4).
The item pages of guhs:vetbootje_fluitje, guhs:vetbootje_werfboekje and guhs:heteluchtballon come from the game data by
themselves. HOW a player gets the boat (the questline at the Mika-vesting, the shipyard of the Leegtehaven) and the balloon
(the Vredesballon) is written by phase B and C: the texts below only say what the two vehicles do.
"""

WIKI = {
    "systemen": {
        "vetbootje": (
            "Je eigen vetbootje", "item:guhs:vetbootje_fluitje",
            "Een klein, langzaam vliegend vetschip van jezelf: met een Mika op het zeil, een kist in het ruim en twee kussentjes voor "
            "je guhs. Je raakt het nooit kwijt: fluit op je Vetfluitje en het komt aangegleden.",
            ["vetbootje_fluitje", "vetbootje_werfboekje", "mika_vesting", "leegtehaven"]),
        "heteluchtballon": (
            "Je eigen heteluchtballon", "item:guhs:heteluchtballon",
            "Een kleine ballon met een slapend guh-snoetje om rustig mee te zweven. Jij doet de brander, de wind doet de rest.",
            ["heteluchtballon"]),
    },
    "entity_home": {},
    "tekst": [
        ("vetbootje", "Varen",
         "Klik op je vetbootje om aan boord te stappen: jij staat aan het stuurwiel. Vooruit en achteruit varen en links en rechts "
         "sturen gaat met de gewone looptoetsen, springen is omhoog en sprinten is omlaag. Het is een vetbootje en geen raceguh: het "
         "gaat rustig. Laat je alles los, dan blijft het hangen waar het is. Het valt nooit, ook niet als er niemand op staat, en het "
         "zakt niet onder de wereld. Stap je hoog in de lucht uit, dan zweef je zachtjes naar beneden."),
        ("vetbootje", "De kist en de guhs",
         "Sluipen + klikken (of een klik op het dek terwijl je vaart) opent het ruim: een kist met 27 vakjes die alleen jij open "
         "krijgt. Klik met een kaasknabbel op het bootje en je eigen guhs in de buurt springen op de twee kussentjes voor de mast; "
         "nog een klik en ze stappen weer uit. Een heel vadsige guh past niet op een kussentje. Een vriend mag ook op een kussentje "
         "meevaren, maar het stuur is van jou."),
        ("vetbootje", "Nooit kwijt",
         "Het Vetfluitje roept je vetbootje, waar het ook is: is het in de buurt, dan komt het zelf (met je guhs er nog op); is het "
         "ver weg of in een andere wereld, dan staat er een nieuw voor je klaar, met dezelfde verf, dezelfde naam en alles wat in "
         "de kist zat. Staat het in de weg? Sluipen + een tikje en het dobbert naar huis tot je weer fluit."),
        ("vetbootje", "De scheepswerf",
         "Met het Werfboekje verf je je vetbootje: zestien kleuren voor het zeil, de wimpel en de streep op de romp, zes zeilen "
         "(Mika-snoet, Knabbelzak, Vetstrepen, Hartje, Vahoeg-ster, Kruimels) en een eigen naam. Geen inspiratie? De werf verzint "
         "er een, zoals De Dobberende Vadsbaal. De naam lees je als je naar het bootje kijkt."),
        ("heteluchtballon", "Zweven",
         "Zet de opgevouwen ballon op de grond en hij vouwt zich uit. Stap in het mandje en houd springen ingedrukt: de brander "
         "loeit en je stijgt. Laat los en je zakt heel zachtjes. Sturen kan niet: de wind neemt je mee. Elke wereld heeft zijn eigen "
         "trage wind, en op elke hoogte waait hij een andere kant op. Kies dus je hoogte en je kiest je richting; het vaantje op "
         "het mandje wijst de wind aan. Op de grond staat de ballon stil."),
        ("heteluchtballon", "Meenemen",
         "Er past nog iemand bij in het mandje: een vriend, of je eigen guh (klik met een kaasknabbel). Sluipen + klikken vouwt de "
         "ballon weer op, zo je zak in. Stap je hoog in de lucht uit, dan zweef je zachtjes naar beneden."),
    ],
}
