"""
bbq2 (ring-h3): what the wiki step needs to know about chapter 3 (CONTRACT_130 2.4; not in FEATURES, nothing is built here).
"""
WIKI = {
    "verhalen": {
        "ring_h3": dict(
            nl="De Mijnen van Knabbelmoria", img="structure_knabbelmoria",
            lead_nl="Hoofdstuk 3 van In de ban van de Knabbelring. Onder de Houtskoolvlakte ligt de oude mijn van de dwerg-guhs. Je komt er "
                    "alleen in met het goede woord, je komt er alleen door met vier hefbomen en een geheime deur, en je komt er alleen uit over "
                    "een brug waar iemand anders op achterblijft.",
            ftb=[("guhs_knabbelring", "ring_h3")], structure="knabbelmoria",
            npcs=["guhdalf", "gimguh", "pippguh", "merrie", "araguh", "leguhlas", "boromika"], related=["ring_h2", "ring_h4"]),
    },
    "npc_home": {},
    "entity_home": {"barbecuerog": "bouwwerken/knabbelmoria"},
    "tekst": [
        ("verhalen/ring_h3", "De poort", "De westpoort gaat alleen open voor wie het raadsel oplost. Boven de deur staat in gloeiende runen: Zeg njeg en "
                                         "treed binnen. Typ njeg in de chat (in de buurt van de poort), of klik op de runen en kies het goede antwoord. "
                                         "Guhdalf staat erbij en geeft steeds duidelijkere hints."),
        ("verhalen/ring_h3", "De Hal van de Hefbomen", "Vier hefbomen onder vier tekens: kaas, worst, saus en knabbel. Het rijmpje op de steen in het "
                                                      "midden zegt de volgorde: eerst de worst, dan de kaas, de saus erover en de knabbel toe. Een "
                                                      "foute hefboom en je begint opnieuw. Iedere speler lost het zelf op; het valhek gaat daarna vanzelf "
                                                      "weer dicht voor de volgende."),
        ("verhalen/ring_h3", "De put en de geheime doorgang", "In de wachtkamer laat Pippguh een emmertje in de put vallen. Daarna hoor je trommels. Gimguh "
                                                             "weet een dwergendeur: klop drie keer op de rune van wat Durguh het lekkerst vond. Op zijn "
                                                             "tombe staat het: kaas."),
        ("verhalen/ring_h3", "De Barbecuerog", "In de Zuilenhal wordt de Barbecuerog wakker: een gehoornde demon van houtskool en gloeiende kooltjes, tien "
                                              "blokken hoog, met een brandende manen, vleugels van rook, een zwaard van vuur en een zweep van aan elkaar "
                                              "geknoopte braadworstjes. Hij doet je geen pijn: zijn stamp duwt je weg en zijn zweep zet je terug bij je "
                                              "laatste rustvuurtje. Je verliest nooit iets. Iedere speler heeft zijn eigen Barbecuerog."),
        ("verhalen/ring_h3", "Het Brokkelpad en de brug", "Over de Kloof loopt eerst het Brokkelpad: steen dat een tel nadat je erop stapt wegvalt, tussen "
                                                         "pilaren die blijven staan. Na een paar tellen groeit het weer aan. Val je, dan sta je weer bij het "
                                                         "rustvuurtje. Daarna komt de Brug van Knabbel-dûm. Aan de overkant begint de grote scène; die kun je "
                                                         "later in de Guhdex opnieuw bekijken."),
    ],
}
