"""
einde2 phase B (h1_lucht): what the wiki may say about chapter 1 of the Guhvatar story. Not a feature module (not in
FEATURES): the docs step reads WIKI and turns it into pages (build contract C-A 2.4). No spoilers: what you need to start and
what the toys are; the story itself is told in the game. The pages of the kaasbol, the toy and the clothes come from the game
data.
"""
WIKI = {
    "systemen": {
        "guhvatar_lucht": (
            "De Guhvatar: Lucht", "item:guhs:luchtscooter",
            "Het eerste hoofdstuk van De Legende van Guh. Wie Opper-Mika heeft verslagen, ziet bij de Knabbelberg een gloeiende "
            "kaasbol aanspoelen. Wat erin zit, verandert alles... en daarna leer je luchtsturen in de Zuidelijke Luchttempel.",
            ["luchtscooter", "kaasbol", "zuidelijke_luchttempel"]),
    },
    "entity_home": {"kaasbol": "systemen/guhvatar_lucht"},
    "tekst": [
        ("guhvatar_lucht", "Hoe begint het?",
         "Versla eerst Opper-Mika op de Knabbelberg. Kom je daarna terug in het Guheinde, dan drijft er bij de plek waar het "
         "portaal je neerzet iets gloeiends aan. Alleen jij ziet jouw kaasbol: iedere speler heeft er een eigen. Tik hem open."),
        ("guhvatar_lucht", "Spelen in de Luchttempel",
         "In de tempel speel je luchtbal op palen, race je over de luchtscooter-baan en zweef je door ringen rond de torens. "
         "Verlies je, dan probeer je het gewoon nog een keer: falen kost nooit iets, en vallen doet in de tempel geen pijn."),
        ("guhvatar_lucht", "Wat houd je eraan over?",
         "Een luchtnomadengewaad voor je guh en twee luchtscooters: speelgoed waar je guhs zoeeef rondjes op rijden, ook in hun "
         "guhhuisje. En natuurlijk de reis zelf: die gaat verder, naar het water."),
    ],
}
