"""
bbq2 (ring-h1) - the wiki entries of chapter 1 of the Knabbelring (CONTRACT_130 2.4; the docs step of phase 3 turns this into
pages; this module is not in FEATURES and builds nothing).
"""
WIKI = {
    "verhalen": {
        "ring_h1": dict(
            nl="Een langverwacht knabbelfeest", img="structure_knabbelgouw",
            lead_nl="Hoofdstuk 1 van de Knabbelring. Guhdalf staat met zijn kar vol vuurwerk bij de grote barbecueput en heeft het grillportaal "
                    "uitgezet. Help hem met het afscheidsfeest, krijg de Knabbelring, pak proviand in met Sam-guh en loop samen naar het portaal: "
                    "dan doet het portaal het weer. Ongeveer tien gezellige minuten, er gaat niets mis.",
            ftb=[("guhs_knabbelring", "ring_h1")], structure="knabbelgouw", npcs=["guhdalf"],
            related=["verhalen/knabbelring", "bouwwerken/barbecueput"]),
    },
    "systemen": {},
    "npc_home": {"guhdalf": "bouwwerken/knabbelgouw"},
    "entity_home": {},
    "tekst": [
        ("verhalen/ring_h1", "Waar is het?",
         "In nieuw land van de Guhmensie hoort bij elke grote barbecueput een Knabbelgouw: heuvelholletjes met ronde deuren, een feestwei met een "
         "feestboom, de moestuin van Sam-guh en het kamp van Guhdalf. Bij een grote barbecueput die er al stond voordat deze update kwam, staat "
         "alleen Guhdalfs kamp (zijn tent, zijn kar en de feesttafel) op een vrij plekje naast de put. Het verhaal is op beide plekken hetzelfde. "
         "Het Superkompas wijst met 'Mijn verhaal' naar de dichtstbijzijnde Guhdalf."),
        ("verhalen/ring_h1", "De stappen",
         "1. Praat met Guhdalf (eerst moet de barbecue van de Grillguh branden). 2. Drie klusjes: steek een vuurpijl af uit de kist op de kar, dek "
         "de feesttafel en nodig Sam-guh uit. 3. Praat weer met Guhdalf: het afscheidsfeest, met de Knabbelring als cadeau. 4. Vraag Sam-guh mee. "
         "5. Klik op de drie proviandkratten (worst, kaas, knabbels). 6. Loop met Sam-guh naar het grillportaal."),
        ("verhalen/ring_h1", "Met zoveel spelers als je wilt",
         "Alles is per speler: de kist, de tafel en de kratten raken nooit op, iedereen ziet zijn eigen Sam-guh, en het portaal gaat alleen open "
         "voor wie het hoofdstuk zelf heeft gedaan. De weg terug uit de Guhbarbecuether is nooit dicht."),
        ("verhalen/ring_h1", "Daarna",
         "Guhdalf en de feesttafel geven na het hele verhaal elke dag één Feestknabbel. De vuurwerkkist doet het altijd, voor iedereen. In de "
         "Knabbelgouw wonen vier Gouwguhs die graag over het tweede ontbijt praten."),
    ],
}
