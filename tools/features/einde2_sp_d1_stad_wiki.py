"""
einde2 "Sponsguh" (sp_d1_stad): what the wiki may say about part 1 steps 5-6 of the Sponsguh story. Not a feature module (not
in FEATURES): the docs step reads WIKI and turns it into pages (build contract C-A 2.4). No spoilers: what the two games are
and that failing costs nothing; the story itself is told in the game.
"""
WIKI = {
    "systemen": {
        "sponsguh_kwallen": (
            "Kwallen vangen met Patguh", "item:guhs:einde2_sp_d1_stad_kwallennet",
            "Een stukje van het verhaal Sponsguh Kaasbroek in Bikinibodem. Patguh geeft je een kwallennetje, en in het Kwallenveld "
            "aan het westeinde van de Schelpstraat vang je er acht kruimelkwallen mee: zachtjes tikken, dan floep, in de pot.",
            ["kruimelkwal", "kwallenjam", "bikinibodem"]),
        "sponsguh_examen": (
            "Het bootrijexamen", "item:guhs:einde2_bouw_bikinibodem_pylon",
            "Sponsguh doet zijn bootrijexamen bij juf Pufguh, en jij bent zijn rij-maatje. Je stuurt het lesbootje over het "
            "pylonenparcours: volg de lichtjes, over het bruggetje en terug naar de finish.",
            ["bikinibodem"]),
    },
    "tekst": [
        ("sponsguh_kwallen", "Hoe werkt het?",
         "Tik een kwal aan met het netje in je hand (links- of rechtsklik). Boven je hotbar staat hoeveel kwallen er al in je pot "
         "zitten. Loop je even weg, dan blijft je pot gewoon staan. Er is geen tijdslimiet."),
        ("sponsguh_kwallen", "Wat houd je eraan over?",
         "Het kwallennetje zelf, drie potjes kwallenjam en Patguh's bloemetjesbroek voor je guh."),
        ("sponsguh_examen", "Botsen mag",
         "Je mag tot acht keer ergens tegenaan botsen (Sponsguh gilt dan wel). Vaker? Dan staat het bootje meteen weer bij de start "
         "voor een nieuwe poging. Er is geen klok en je verliest nooit iets."),
    ],
}
