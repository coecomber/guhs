"""
einde2 (bouw_aarde): what the wiki must say about Ba Sing Guh as a place. Not a feature module (not in FEATURES): the docs
step reads WIKI and turns it into pages (build contract 2.4). The pages of the six blocks and of the structure itself come
from the game data. Nothing here tells the story: that is a later phase's text (no spoilers on the page of the place).
"""
WIKI = {
    "systemen": {
        "ba_sing_guh": (
            "Ba Sing Guh", "block:guhs:einde2_bouw_aarde_aardemunt",
            "De ommuurde stad van de drie ringen, hoog boven de buiteneilanden op haar eigen plak klipkaas. Er is hier geen "
            "knabbeltekort. Dat staat op elk bordje, dus het zal wel kloppen.",
            ["einde2_bouw_aarde_stadssteen", "einde2_bouw_aarde_aardemunt", "einde2_bouw_aarde_kool", "einde2_bouw_aarde_spoor",
             "einde2_bouw_aarde_groen_kristal", "einde2_bouw_aarde_theekist", "klipkaas", "kristalkaas_kristal", "das_molguh"]),
    },
    "entity_home": {"das_molguh": "bouwwerken/ba_sing_guh"},
    "tekst": [
        ("ba_sing_guh", "De stad",
         "Ba Sing Guh zweeft op een rond eiland van klipkaas. Je landt op de westkant, voor de buitenmuur: twintig blokken hoog, "
         "met kantelen en ronde torens. In de poort zit het paspoortloket. Wie erdoor mag, komt in de onderring: kleine lemen "
         "huisjes dicht op elkaar, een markt, kooltuintjes en waterputten. Een tweede muur verder ligt de middenring, drie blokken "
         "hoger, met witte huizen onder groene pannendaken. Binnen de derde muur ligt de bovenring met het paleis en zijn gouden "
         "daken. Vanuit het paleis lopen vier wegen door de poorten van de binnenmuren naar buiten; in elke ring loopt een weg in "
         "het rond."),
        ("ba_sing_guh", "De monorail",
         "Vanaf de buitenmuur loopt een viaduct op hoogte over de twee binnenmuren heen naar de bovenring: de monorail, met vier "
         "stations (Buitenmuur, Onderring, Middenring, Bovenring). Bij elk station staat een toren met een wenteltrap. Je mag ook "
         "gewoon over het viaduct lopen. Er rijdt een wagen. Die moet je zelf duwen."),
        ("ba_sing_guh", "Wat er te zien is",
         "In de onderring ligt de Aardrommel, een komvormige arena met een stenen ring in het midden, en ernaast de blinddoekbaan. "
         "Aan de oostkant ligt Meer Laogai, met een botenverhuur die altijd gesloten is. In de middenring staat theehuis De Jasmijn "
         "Draak, met theepotjes waar je echt thee in kunt zetten, en het huis van gids Joo Dee-guh, dat links en rechts precies "
         "hetzelfde is. In de bovenring staan het gasthuis, het paleis met de troonzaal en de kamer van beer Bosco, en in de "
         "paleistuin de Koningsput: een schacht waar het water van de vijver in valt. De stad is beschermd: je kunt er niets "
         "afbreken of bijbouwen."),
        ("ba_sing_guh", "Onder de stad",
         "Onder de paleistuin begint, achter een oude poort, een lange gang omlaag: de kristalcatacomben van Oud Ba Sing Guh. Ze "
         "gloeien groen van het catacombenkristal. Onderweg wonen das-molguhs in een hol; maak ze niet wakker. De gang eindigt in "
         "de kristalkamer, een grote grot recht onder de Koningsput. En onder Meer Laogai ligt helemaal niets. Zeker geen basis."),
        ("ba_sing_guh", "De blokken van de stad",
         "Zes sierblokken horen bij de stad en kun je ook zelf maken: de stadssteen van de muren (vier klipkaasstenen), de "
         "aardemuntsteen (stadssteen met groene kleurstof), de monorailbalk (stadssteen met ijzerklompjes), het catacombenkristal "
         "(kristalkaas, groene kleurstof en glas; het geeft licht), de theekist (planken om een blaadje) en de kool van de koolman "
         "(mosblok met limoengroene kleurstof). Ze doen niets, behalve mooi zijn."),
    ],
}
