"""
einde2 (phase B), slice h2_water: what the wiki must say about chapter 2 of the Guhvatar story, "De Noordelijke Waterstam".
Not a feature module (not in FEATURES): the docs step reads WIKI and turns it into pages (contract C-A 2.4). The pages of the
items (the boemerang), the clothes and the place itself come from the game data. Spoilers are kept light: the page tells
what you do, not every turn of the story.
"""
WIKI = {
    "systemen": {
        "guhvatar_water": (
            "De Guhvatar, hoofdstuk 2: de Noordelijke Waterstam", "item:guhs:boemerang",
            "Het tweede hoofdstuk van het Guhvatar-verhaal. Appa brengt je naar een stad van ijs, waar je leert watersturen van "
            "Meester Pakguh (die het alleen aan guhs met een snor leert), de Blauwe Geest ontmoet, naar de Geestenwereld reist en "
            "een golf zo hoog als een muur maakt. Niemand raakt ooit gewond: de Mika's duwen alleen.",
            ["noordelijke_waterstam", "boemerang", "waterstam_parka", "blauwe_geest_masker"]),
    },
    "entity_home": {},
    "tekst": [
        ("guhvatar_water", "Hoe begint het?",
         "Het hoofdstuk begint zodra je hoofdstuk 1 in de Zuidelijke Luchttempel hebt afgemaakt: Appa vliegt je naar de Noordelijke "
         "Waterstam, en de rooksluier om de stad gaat voor je open. Wie met je meereist op Appa mag als gast mee naar binnen, maar "
         "ieder houdt zijn eigen voortgang."),
        ("guhvatar_water", "De training",
         "Meester Pakguh wil eerst niet, maar Katara-guh daagt hem uit. Daarna leer je drie dingen op het trainingsplein: de "
         "waterzweep (raak vijf doelen), de golf (drie golven in het golfbassin) en de ijsbrug (bevries een brug naar het eilandje "
         "en loop eroverheen). Je gebruikt je krachten met de krachtentoets; ze werken in het verhaal alleen in de Waterstam. "
         "Daarna ga je otter-surfen van de ijsmuur en duw je een gondel met waterstroom langs drie haltes in de grachten."),
        ("guhvatar_water", "Sluipen",
         "Twee keer moet je op het schip van admiraal Zhao-Mika sluipen. De wachters kijken rond in een kegel van licht (je ziet "
         "kleine vlammetjes aan de randen). Zien ze je, dan word je teruggezet naar je laatste rustpunt. Sluipen (bukken) helpt: "
         "dan zien ze je minder ver. Het kost nooit iets."),
        ("guhvatar_water", "De Geestenwereld",
         "Mediteer bij de vijver in de geestenoase (blijf even sluipend stil zitten) en je reist met Aang-guh naar de Geestenwereld. "
         "Daar wacht Koh de Gezichtenrover: als hij je aankijkt mag je niet bewegen, niet springen en nergens op klikken. Lukt het "
         "niet, dan leent hij even je gezicht en sta je weer bij de aankomst. Probeer het gewoon opnieuw."),
        ("guhvatar_water", "Wat je krijgt",
         "Een Blauwe Geest-masker en een Waterstam-parka voor je guh, en Sokka-guh's boemerang. Gooi hem: hij komt altijd terug, "
         "met alles wat hij onderweg tegenkwam. Katara-guh en Sokka-guh reizen daarna met je mee naar Ba Sing Guh."),
        ("guhvatar_water", "Herbekijken",
         "Alle cutscenes van het hoofdstuk staan in de Guhdex, tab Verhalen, bij De Noordelijke Waterstam."),
    ],
}
