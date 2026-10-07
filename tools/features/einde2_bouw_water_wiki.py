"""
einde2 (bouw_water): what the wiki must say about the Noordelijke Waterstam and the blocks it is built of.
Not a feature module (not in FEATURES): the docs step reads WIKI and turns it into pages (build contract 2.4). The pages of
the blocks and of the structure itself come from the game data. No story here: the place is hidden until the Guhvatar's
story brings you there, and what happens in it is told by the page of that story (a later phase writes it).
"""
WIKI = {
    "systemen": {
        "waterstam_ijs": (
            "IJs van de Waterstam", "block:guhs:einde2_bouw_water_ijssteen",
            "De guhs van de Noordelijke Waterstam bouwen alles van ijs dat ze uit de gletsjer zagen: stenen waar je niet op "
            "uitglijdt, lantaarns die nooit smelten en hun eigen teken, een maan boven drie golven.",
            ["einde2_bouw_water_ijssteen", "einde2_bouw_water_ijssteen_trap", "einde2_bouw_water_ijssteen_plaat", "einde2_bouw_water_ijslantaarn",
             "einde2_bouw_water_stamteken", "einde2_bouw_water_vuurnatie_teken", "noordelijke_waterstam"]),
    },
    "entity_home": {},
    "tekst": [
        ("waterstam_ijs", "Waterstam-ijsstenen",
         "Vier blokken pakijs worden vier Waterstam-ijsstenen. Ze zijn even hard als pakijs, maar je glijdt er niet op uit en ze "
         "smelten nooit, ook niet naast een lamp. Je maakt er trappen en platen van, op de werkbank of op de steenzaag."),
        ("waterstam_ijs", "De ijslantaarn",
         "Een lichtje dat in helder ijs is ingevroren: vier ijsstenen rond een guhkristal geven vier ijslantaarns. Een ijslantaarn "
         "geeft het volle licht. De Waterstam legt ze in de straat, hangt ze aan kettingen en zet ze op palen."),
        ("waterstam_ijs", "De twee tekens",
         "Het Waterstamteken is een maan boven drie golven, met twee guh-oortjes erboven: je maakt het van ijsstenen en lapis "
         "lazuli. Het Vuurnatieteken is een zwarte vlam die boos kijkt, van houtskoolsteen-stenen en rode kleurstof. Allebei zijn "
         "ze alleen om mee te bouwen."),
        ("noordelijke_waterstam", "De plek",
         "Hoog boven de buiteneilanden van het Guheinde drijft een ijsberg met een eigen zee. Achter een reusachtige ijsmuur met een "
         "poort ligt een stad in drie lagen: beneden de grachten met bruggen, huizen en iglo's, daarboven het trainingsplein met het "
         "golfbassin, en bovenaan het paleis van de hoofdman. Achter het paleis, door een deurtje in het ijs, ligt de geestenoase: "
         "de enige plek waar het warm is en gras groeit. Buiten de muur liggen de haven en het landingsijs, en vanaf de muur glijdt "
         "een sleebaan omlaag. De Waterstam ligt altijd boven een streek met blauwgroen water (de Korstwoestijn of de "
         "Schimmelkaasvlakte), precies een keer per wereld, en niemand kan er iets afbreken."),
        ("noordelijke_waterstam", "Varen",
         "De ringgracht en de Grote Gracht zijn vijf blokken breed en twee diep, en onder elke brug is ruimte voor een guh in een "
         "bootje. Vanaf het bekken onder aan de grote trap vaar je in een rechte lijn door de poort de zee op."),
    ],
}
