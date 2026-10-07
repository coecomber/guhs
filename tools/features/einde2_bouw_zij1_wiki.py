"""
einde2 (bouw_zij1): what the wiki must say about Omashguh, the Westelijke Luchttempel and the Vuurtempel van Roku-guh.
Not a feature module (not in FEATURES): the docs step reads WIKI and turns it into pages (build contract 2.4). The pages of
the structures and of the postgoot themselves come from the game data.
"""
WIKI = {
    "systemen": {
        "postglijbaan": (
            "De postglijbanen van Omashguh", "block:guhs:einde2_bouw_zij1_postgoot",
            "In Omashguh loopt de post niet, de post glijdt. Drie goten van postgoot lopen van het paleis van koning Bumi-guh "
            "naar het postkantoor, en jij past er ook in.",
            ["omashguh", "einde2_bouw_zij1_postgoot", "kristalkaasklippen"]),
        "grote_plekken_buiteneilanden": (
            "De grote plekken van de buiteneilanden", "block:guhs:kristalkaas_blok",
            "Boven sommige zeldzame streken van de buiteneilanden zweeft een eiland met een hele plek erop: een bergstadje, een "
            "tempel die ondersteboven hangt, een pagode met vijf dichte deuren. Je vindt ze met het Superkompas (tabblad Einde).",
            ["omashguh", "westelijke_luchttempel", "vuurtempel_roku", "kristalkaasklippen", "kruimelwolken", "knabbelkoorbos"]),
    },
    "entity_home": {},
    "tekst": [
        ("grote_plekken_buiteneilanden", "Hoe vind je ze?",
         "Elke plek hoort bij een streek: Omashguh bij de Kristalkaasklippen, de Westelijke Luchttempel bij de Kruimelwolken en de "
         "Vuurtempel van Roku-guh bij het Knabbelkoorbos. In elke wereld staat er van elk precies een op redelijke afstand van het "
         "midden, en heel soms kom je er verder weg nog een tegen. Ze zweven hoog boven de eilanden, op hun eigen stuk rots: kijk "
         "omhoog. Het Superkompas wijst ze aan in het tabblad Einde. De plekken zijn beschermd: je kunt er niets afbreken of "
         "bijbouwen, zodat iedere guh die na jou komt ze net zo vindt."),
        ("grote_plekken_buiteneilanden", "Omashguh",
         "Een ommuurd bergstadje van klipkaas in drie lagen. Beneden de huizen en het postplein, daarboven het terras, en "
         "helemaal bovenop het paleis van koning Bumi-guh met zijn veel te grote huisdier Flopsie. In de berg zitten drie "
         "proefkamers, het postkantoor en het hol van de das-molguhs. De koning en de bewoners zijn er nog niet: zij komen met "
         "het verhaal van de Guhvatar."),
        ("grote_plekken_buiteneilanden", "Westelijke Luchttempel",
         "Bovenop het eiland staat bijna niks: gras, een fontein en een paviljoentje boven de keldertrap. De tempel hangt eronder, "
         "ondersteboven. De trap komt uit in de hangende hal; van daaruit lopen vier windgangen naar vier pagodes die met hun punt "
         "omlaag hangen, en onder de hal hangt de grote middenpagode met de orgelpijp, een schacht recht omlaag. In het eiland zelf "
         "zit de stal van de luchtbizon-guhs: vijf hokken die uitkomen op de leegte. Ze zijn leeg."),
        ("grote_plekken_buiteneilanden", "Vuurtempel van Roku-guh",
         "Een eiland in de vorm van een halve maan. Op de ene punt land je, op de andere staat een vulkaantje met een deksel erop, "
         "en in het midden staat de tempel: een rode pagode van vijf verdiepingen. Helemaal bovenin staan vijf vuurdeuren naast "
         "elkaar. Erachter ligt het heiligdom met het beeld van Guhvatar Roku-guh; dat blijft dicht tot iemand ze alle vijf "
         "tegelijk opent. Ten westen van de tempel is een rots waar echt helemaal niks is. Zegt het bordje."),
        ("postglijbaan", "Zo glijd je",
         "Ga op het platform voor het paleis op een van de drie goten staan en je bent weg. Linksom en rechtsom lopen om de berg "
         "heen; de middelste gaat rechtdoor de kurkentrekker in, de spiraaltoren op het postplein. Alle drie komen uit bij de "
         "postbak voor het postkantoor. Bukken is remmen: wie bukt staat stil en kan uitstappen. Vallen doet geen pijn, de goot "
         "gaat steeds maar een blok omlaag."),
        ("postglijbaan", "Zelf bouwen",
         "De postgoot is een blok: wie erop staat glijdt mee in de richting van de pijltjes. Je plaatst hem van je af. In de "
         "gewone wereld is hij niet te maken; je vindt hem alleen in Omashguh, en daar blijft hij liggen."),
    ],
}
