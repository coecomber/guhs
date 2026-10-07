"""
einde2 (bouw_vuur): what the wiki may say about the Vuurnatie-hoofdstad. Not a feature module (not in FEATURES): the docs
step reads WIKI and turns it into pages (build contract 2.4). The pages of the six decoration blocks come from the game
data by themselves.

The capital is a hidden story structure (in no compass, behind the veil until the story reaches it): the wiki gets no page
that says where it lies or what happens there. Only the blocks, which you can also find in its chests, are described; the
merge step decides whether the place itself gets a page once the story of chapter 4 is written.
"""
WIKI = {
    "systemen": {
        "lauwe_lava": (
            "Lauwe lava en lauwe vlammen", "block:guhs:einde2_bouw_vuur_lauwe_lava",
            "Lava die is afgekoeld tot hij lauw werd, en vlammen die alleen nog gloeien. Ze zien er gevaarlijk uit, maar je kunt er "
            "gewoon op lopen en doorheen wandelen, njeg.",
            ["einde2_bouw_vuur_lauwe_lava", "einde2_bouw_vuur_lauwe_vlam", "einde2_bouw_vuur_embleem", "einde2_bouw_vuur_portret_mikai",
             "einde2_bouw_vuur_zonnesteen", "einde2_bouw_vuur_lotustegel"]),
    },
    "entity_home": {},
    "tekst": [
        ("lauwe_lava", "Niets doet pijn",
         "Lauwe lava is een vast blok dat licht geeft als echte lava, met een velletje gesmolten kaas erop. Een lauwe vlam is een "
         "vlam waar je je poot in kunt steken: hij brandt niet en steekt niets aan. Samen maken ze van elke bouw een vulkaan "
         "zonder dat er iemand iets van krijgt."),
        ("lauwe_lava", "De andere vier",
         "Het Vuurnatie-embleem is een gouden vlam met een boze blik en één tandje. Het portret van Vuurheer Mikai kijkt je van "
         "alle vier de kanten aan. De Zonnekrijgersteen is zandsteen met een uitgehakte zon, en de Witte Lotus-tegel is een ronde "
         "witte steen met een bloem erop."),
    ],
}
