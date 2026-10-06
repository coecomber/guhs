"""
Wiki texts of the three Mika palaces (bbq2, slice paleizen). Not a feature module (not in FEATURES): the docs step of the
merge reads WIKI and turns it into pages (CONTRACT_130 2.4). Items, blocks, structures, the clothes piece, the characters and
the Worstzwijntje get their own pages from the game data.
"""
from features import wereld

BBQ = "guhs_barbecuether"

WIKI = {
    "verhalen": dict([
        wereld.wiki_questlijn(
            "mika_oma", "Soep van Mika-oma",
            "Mika-oma woont op de bovenste galerij van de Mika-woonblokken en doet niet mee aan het knabbels jatten: ze breit en kookt "
            "worstsoep. Breng haar soep naar de drie mopperaars van de flat (Brom-Mika, Zeur-Mika en Snurk-Mika) en zoek haar "
            "weggewaaide breiwerk op de daktuin. Je krijgt een gebreide Mika-muts, en voortaan is elke derde ruil met een Nether-Mika "
            "gratis.", "mika_woonblokken", ["mika_oma"], [(BBQ, "paleizen_woonblokken")], related=["verhalen/stalknecht", "verhalen/tolwachter"]),
        wereld.wiki_questlijn(
            "stalknecht", "De onrustige Worstzwijntjes",
            "In de Mika-stal zorgt de Stalknecht-guh stiekem goed voor de Worstzwijntjes van de Mika's. Aai er drie kalm, vul de "
            "voerbak en vang Knorretje, die ontsnapt is (sluipen, anders rent hij weg). Als dank krijg je twee Worstzwijntjes in een "
            "mandje, voor thuis.", "mika_stal", ["stalknechtguh"], [(BBQ, "paleizen_stal")], related=["verhalen/mika_oma", "verhalen/tolwachter"]),
        wereld.wiki_questlijn(
            "tolwachter", "De tolbrug van het Mika-brugpaleis",
            "Het Mika-brugpaleis is een tolbrug hoog boven de saus: je loopt er binnen door de bek van een reusachtige Mika. De "
            "Tolwachter-Mika laat je pas door als je 8 kaasknabbels betaalt of drie raadsels raadt. Daarna leg je de vijf rijen planken "
            "terug in het gat van de brug en luid je de tolbel. Je mag voortaan altijd gratis door en krijgt de bouwtekening van de "
            "brug.", "mika_brugpaleis", ["tolwachter_mika"], [(BBQ, "paleizen_brugpaleis")], related=["verhalen/mika_oma", "verhalen/stalknecht"]),
    ]),
    "npc_home": {"mika_oma": "bouwwerken/mika_woonblokken", "stalknechtguh": "bouwwerken/mika_stal", "tolwachter_mika": "bouwwerken/mika_brugpaleis"},
    "entity_home": {"worstzwijntje": "bouwwerken/mika_stal", "paleizen_mopper_mika": "bouwwerken/mika_woonblokken"},
    "tekst": [
        ("bouwwerken/mika_woonblokken", "Wat is het?",
         "De flats van de Nether-Mika's: drie woonblokken van roosterijzer rond een binnenplaats, op een voet in de frituursauszee. Er "
         "zijn galerijen met deurmatten, balkons met gestreepte zonneschermen, waslijnen, een rokend ketelhuis en een saustoren. In de "
         "flats wonen Mika's die niemand kwaad doen: er spawnen hier geen monsters en je kunt er niks stukmaken."),
        ("bouwwerken/mika_woonblokken", "De weg vinden",
         "Je komt binnen door de poort aan de voorkant. In de twee hoeken van de binnenplaats staat een trappenhuis met een rood puntdak: "
         "het westelijke gaat helemaal naar de bovenste galerij (daar zit Mika-oma), het oostelijke naar de daktuin van de lage flat."),
        ("bouwwerken/mika_stal", "Wat is het?",
         "Een rode schuur met een grote deur onder een Worstzwijntjesbord, een hooizolder, zes boxen en een modderige wei met een "
         "afdakje, hooibalen en een kar. De vijf Worstzwijntjes van de stal blijven van de stal: aaien mag altijd, meenemen niet."),
        ("bouwwerken/mika_brugpaleis", "Wat is het?",
         "Een lange brug op pijlers, met halverwege het tolhuis: een toren waarvan de voorkant één groot Mika-gezicht is. De open bek is "
         "de poort. Voorbij het tolhuis missen vijf rijen planken (eronder hangt een steiger met een ladder, je valt dus nooit ver) en "
         "aan het eind staat de klokkentoren met de tolbel."),
        ("bouwwerken/mika_brugpaleis", "Mika-kwaliteit",
         "De planken die je legt vallen er na een minuut weer uit. Dat hoort zo: dan vindt de volgende speler het gat ook weer. Heb je de "
         "brug samen gemaakt, dan telt het voor iedereen die een rij heeft gelegd."),
        ("dieren/worstzwijntje", "Als boerderijdiertje",
         "Een Worstzwijntje uit het mandje van de Stalknecht-guh is een boerderijdiertje, net als het guhschaapje en de guhkoe. Aai het "
         "(lege hand), borstel het (guhborstel) en voer het (knabbelvoer, of een gevulde guh-voerbak). Twee van de drie op één dag en het "
         "is blij: dan snuffelt het één keer per dag iets op, meestal een paar kaasknabbels, soms een zwammetje of iets anders uit de "
         "Barbecuether. Met knabbelvoer krijgen twee Worstzwijntjes een jong."),
    ],
}
