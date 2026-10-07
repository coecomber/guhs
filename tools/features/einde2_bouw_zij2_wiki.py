"""
einde2 (bouw_zij2): what the wiki must say about the Kyoshi-dorp, the Bibliotheek van Wan Shi Guh, the
Leeuwschildpad-eiland, the Leegtehaven and the Guhvleugel-ringenrace. Not a feature module (not in FEATURES): the docs step
reads WIKI and turns it into pages (build contract 2.4). The pages of the structures themselves and of the ring block come
from the game data. Only what is there in this phase is described: the buildings. Residents and quests follow later.
"""
WIKI = {
    "systemen": {
        "plekken_buiteneilanden_zij2": (
            "Haven, dorp en bibliotheek", "block:guhs:ringenrace_ring",
            "Vijf grote plekken zweven hoog boven de buiteneilanden van het Guheinde, elk op zijn eigen stuk grond. Ze zijn niet "
            "verstopt: het Superkompas (tabblad einde) wijst ze aan. Je kunt er alvast rondkijken; de bewoners komen nog.",
            ["leegtehaven", "kyoshi_dorp", "bibliotheek_wan_shi_guh", "leeuwschildpad_eiland", "guhvleugel_ringenrace", "ringenrace_ring",
             "schimmelkaasvlakte", "korstwoestijn", "kruimelwolken"]),
    },
    "entity_home": {},
    "tekst": [
        ("plekken_buiteneilanden_zij2", "Zo kom je er",
         "De plekken liggen ver van de Knabbelberg, een heel stuk hoger dan de eilanden zelf. Lopen kan dus niet: je vliegt erheen, met "
         "Guhvleugels of op een Enderguh. Elke plek heeft een landingsplek met een roze neusje in het midden. Alles is beschermd: je kunt "
         "er niets afbreken en niets bijbouwen, zodat het voor iedere guh na jou heel blijft. Deuren, kisten en bankjes werken gewoon."),
        ("plekken_buiteneilanden_zij2", "Leegtehaven",
         "Een zwevende haven op een brok kruimelrots, er is er maar een van. Aan de twee steigers in het noorden liggen vetschepen van de "
         "Mika's; je mag aan boord, en via het luik kom je in het ruim. Tussen de steigers staat de markt: vier kramen met achter elke "
         "toonbank een Ender-Mika die handelaar is. In het oosten ligt de veerpier met veerboot De Vadsige Golf, in het westen de "
         "scheepswerf met de helling, in het zuiden de kroeg De Vadsige Walvis, de vuurtoren met guh-oortjes en de keuken van De "
         "Spijtige Spatel, waar nog geen kok staat."),
        ("plekken_buiteneilanden_zij2", "Kyoshi-dorp",
         "Een dorp op een eiland van schimmelkaas, boven de Schimmelkaasvlakte. Je loopt door de poort de dorpsstraat in, langs vier "
         "huizen met zeegroene daken, naar het plein. Daarachter staat het reuzenbeeld van Kyoshi-guh: een guh in een groen gewaad, wit "
         "geschminkt, met een gouden hoofdtooi en twee gouden waaiers. Links van het plein woont de dorpsoudste, rechts staat de dojo met "
         "de waaierdansvloer en de schminktafel. Bij de vijver wonen twee otter-pinguinguhs. In de vijver woont iets anders."),
        ("plekken_buiteneilanden_zij2", "Bibliotheek van Wan Shi Guh",
         "In de Korstwoestijn zweeft een plak woestijn waar alleen een groene torenspits, vier torentjes en een koepel bovenuit steken. "
         "De bibliotheek zelf hangt onder het zand: een toren van zes verdiepingen diep. Je klimt door het raam van de spits naar binnen "
         "en loopt via de boekentrappen omlaag door vijf zalen, elk rond de grote schacht in het midden. Helemaal onderin ligt de "
         "uilenzaal. Vanaf de bovenste zaal loop je door naar het planetarium, de koepel die je buiten al zag."),
        ("plekken_buiteneilanden_zij2", "Leeuwschildpad-eiland",
         "Dit eiland is een dier: een reusachtige Leeuwschildpad-guh die door de leegte zweeft. Op zijn schild groeit een bos van "
         "guhbloesem en eik. Je landt op de open plek achter op het schild, loopt over de top langs de steenkring en dan het pad af "
         "langs zijn linkerflank. Zijn linkerpoot houdt hij voor zijn gezicht: over de arm loop je naar zijn handpalm, pal voor zijn "
         "snuit. Er is er maar een."),
        ("plekken_buiteneilanden_zij2", "Guhvleugel-ringenrace",
         "Boven de Kruimelwolken hangt een parcours van achttien gloeiende ringen. Start en finish liggen op hetzelfde eilandje: je "
         "vertrekt van de Starttoren, vliegt de noordkant langs, neemt de bocht en komt langs de zuidkant terug op de Landingsbaan. De "
         "ringen zijn van ringenrace-ringblokken en groot genoeg voor een Enderguh. Tijden worden nog niet bijgehouden."),
    ],
}
