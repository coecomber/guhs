"""
einde2 (biomeguhs): what the wiki must say about the six guhs of the outer islands. Not a feature module (not in FEATURES):
the docs step reads WIKI and turns it into pages. The six variants get their own guh page from the game data (Guhdex texts).

TO DO in tools/wiki_renders.py (reserved for the docs step, so not touched here): geo_quads() hides the variant bones by a
fixed list of prefixes; add "korstguh", "duinguh", "kruimelguh", "schimmelguh", "kristalguh", "koorguh" there, or every
guh in a wiki picture wears all six sets of bones at once. A picture of one biome guh: show_only_variant_bones=("<id>",).
The same list feeds tools/make_ftbquests_art.py ("guh:<variant>" sprites; its VARIANT_BONES wants "<id>": ("<id>",) for the
six). And make_ftbquests.py gives a section it makes for a module ("extra_<module>") the portrait "guh:normal" whatever
FTB_PORTRAIT says. Until those three know better, the heading of this slice's FTB section (kop_extra_einde2_biomeguhs.png)
shows a plain guh wearing the bones of all six, and so does every other guh sprite that is drawn again.
"""
WIKI = {
    "verhalen": {},
    "systemen": {
        "guhs-van-de-buiteneilanden": (
            "Guhs van de buiteneilanden", "entity:guhs:guh:korstguh",
            "Op de buitenste eilanden van het Guheinde heeft elke streek zijn eigen guh. Je temt ze met kaasknabbels, net als elke "
            "guh, en getemd doet elk zijn eigen trucje.",
            ["korstguh", "duinguh", "kruimelguh", "schimmelguh", "kristalguh", "koorguh", "guhdex", "guheinde"]),
    },
    "npc_home": {},
    "entity_home": {},
    "tekst": [
        ("guhs-van-de-buiteneilanden", "Waar vind je ze?",
         "De Korstguh woont in de Gatenkaasheuvels, de Duinguh in de Korstwoestijn, de Kruimelguh in de Kruimelwolken, de Schimmelguh "
         "op de Schimmelkaasvlakte, de Kristalguh op de Kristalkaasklippen en de Koorguh in het Knabbelkoorbos. Ze komen en gaan: er "
         "lopen er nooit veel tegelijk rond, en op het hoofdeiland en op stukken van het Guheinde die al verkend waren voordat de "
         "streken er kwamen, vind je ze niet. Loop dus een eindje verder de leegte in."),
        ("guhs-van-de-buiteneilanden", "Temmen en de Guhdex",
         "Voer een wilde biome-guh kaasknabbels tot er hartjes komen. Elke soort heeft een eigen bladzijde in de Guhdex en telt mee "
         "voor een volle Guhdex. Wie zijn Guhdex al vol had, houdt wat hij daarmee verdiend heeft."),
        ("guhs-van-de-buiteneilanden", "De trucjes",
         "Korstguh: buk naast hem en hij rolt zich op tot een balletje. Loop ertegenaan of geef het een tikje en het rolt weg; hij "
         "voelt er niks van en rolt nooit van zijn eiland af. Zijn dutjes doet hij in een kaasgat: een kuiltje van een blok diep.\n"
         "Duinguh: zakt nooit weg in kruimelzand. Staat hij er even stil op, dan graaft hij zich in tot je alleen zijn oortjes ziet. "
         "Loop je weg, dan plopt hij weer omhoog.\n"
         "Kruimelguh: zweeft heel langzaam omlaag en blijft op kruimelwolken staan.\n"
         "Schimmelguh: gloeit als een lamp die met je meeloopt. Slaapt hij, dan is het lampje uit.\n"
         "Kristalguh: snuffelt af en toe een kristalkaasbrokje op als jij in de buurt bent, hooguit één per dag per guh.\n"
         "Koorguh: zingt mee zodra er dichtbij een muziekplaat draait."),
    ],
}
