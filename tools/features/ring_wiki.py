"""
bbq2 (ring-kern) - the wiki entries of the core of "In de ban van de Knabbelring" (CONTRACT_130 2.4; the docs step of phase 3
turns this into pages; this module is not in FEATURES and builds nothing). The chapters add their own pages.
"""
WIKI = {
    "verhalen": {
        "knabbelring": dict(
            nl="In de ban van de Knabbelring", img="reiskaart_knabbelring",
            lead_nl="Het grote verhaal van de Guhbarbecuether: een ringvormige knabbel die iedereen hebberig maakt moet naar de Frituurberg, "
                    "niet om hem te vernietigen maar om hem te frituren en te delen. Zes hoofdstukken, een extra toren, en Sam-guh die de hele "
                    "weg met je meeloopt.",
            ftb=[("guhs_knabbelring", "ring")], structure="ring_rustpunt",
            npcs=["guhdalf", "smikagol", "araguh", "leguhlas", "gimguh", "boromika", "merrie", "pippguh", "guhrond", "guhladriel"],
            related=["systemen/knabbelring", "systemen/rustpunten", "systemen/gaven-van-guhladriel"]),
    },
    "systemen": {
        "knabbelring": ("De Knabbelring", "item_knabbelring",
                        "Wat de ring doet zolang je hem draagt: hij fluistert dat je hem moet opeten, wilde guhs lopen kwijlend achter je aan, "
                        "omdoen maakt je onzichtbaar voor Mika's maar het Oog ziet je en de Negen komen, en bij de berg wordt hij zwaar.",
                        ["verhalen/knabbelring", "systemen/rustpunten"]),
        "rustpunten": ("Rustpunten", "structure_ring_rustpunt",
                       "Kampjes met een Rustvuurtje, overal in de Guhbarbecuether. Het laatste rustpunt waar je was is de plek waar je terugkomt "
                       "als je gezien of gepakt wordt. Sam-guh kookt er één stoofpotje per dag.", ["verhalen/knabbelring"]),
        "gaven-van-guhladriel": ("De gaven van Guhladriel", "item_lichtflesje",
                                 "Het Lichtflesje, het Elfenmanteltje en het Elfentouw: wat ze in het verhaal doen en wat je er daarna nog aan hebt.",
                                 ["verhalen/knabbelring"]),
    },
    "npc_home": {},
    "entity_home": {"smikagol": "verhalen/knabbelring", "knekel_ruiter": "verhalen/knabbelring"},
    "tekst": [
        ("verhalen/knabbelring", "Hoe begin je?",
         "Volg eerst de grote verhalen van de Guhmensie: Baltoguh en Nomguh, Guhtwo en het kloon-eiland, Het Hemelkapelletje, Ohana op "
         "Guhwai'i en Het Snuffeleiland. Eerder begint Guhdalf er niet aan, en hij zegt je welke je nog mist (zie Het Guhpad). Steek ook het "
         "grillportaal aan (de quest van de Grillguh). Daarna staat Guhdalf met zijn kar bij de grote barbecueput in de Guhmensie. "
         "Het grillportaal naar de Guhbarbecuether werkt voor niemand tot hij het hele eerste hoofdstuk heeft gedaan; terug kan altijd."),
        ("verhalen/knabbelring", "Zo volg je het verhaal",
         "Linksboven in beeld staat wat je nu moet doen (uit te zetten in de Guhdex). In de Guhdex, tab Verhalen, staat de reiskaart met een vinkje "
         "per stap en 'Je bent hier'. Het Superkompas wijst naar 'Mijn verhaal'. Sam-guh zegt het ook als je op hem klikt. In het questboek "
         "staat het verhaal in de groep Het Guhpad, hoofdstuk Verhalen van de Guhbarbecuether: de quests gaan één voor één open."),
        ("verhalen/knabbelring", "Niemand doet je pijn",
         "Het Oog, de Negen en alles wat je onderweg tegenkomt duwt alleen: je staat dan weer bij je laatste rustpunt. Je verliest nooit spullen."),
        ("verhalen/knabbelring", "Samen spelen",
         "Iedereen heeft zijn eigen voortgang, zijn eigen ring en zijn eigen Sam-guh. Wie al verder is mag met een vriend meelopen door alles wat "
         "zijn eigen verhaal al bereikt heeft, maar lost de puzzels van die vriend niet voor hem op."),
        ("verhalen/knabbelring", "Na het verhaal",
         "Je krijgt de titel Ringdrager, vier outfits, het beeldje van het Oog, Sam-guh om mee naar huis te nemen (klik op hem, eenmalig) en "
         "Smikagol als maatje die bij water vis voor je vangt. In de Knabbelgouw is er elke dag een Feestknabbel."),
        ("systemen/gaven-van-guhladriel", "Lichtflesje",
         "Rechtsklik: een flits die de Knekel-Mika-ruiters om je heen acht seconden verblindt en rook wegblaast. In je hand geeft het licht dat "
         "met je meeloopt."),
        ("systemen/gaven-van-guhladriel", "Elfenmanteltje",
         "Heb je het bij je, buk dan en sta stil: na een kleine seconde lijk je op een rots en kijken het Oog en de ruiters langs je heen. "
         "Bewegen of opstaan en je bent weer jezelf."),
        ("systemen/gaven-van-guhladriel", "Elfentouw",
         "Kijk naar een Elfentouwhaak binnen 24 blokken en rechtsklik: het touw trekt je erheen. Buk om los te laten. Na het verhaal maak je zelf "
         "haken (twee ijzer en een draadje)."),
    ],
}
