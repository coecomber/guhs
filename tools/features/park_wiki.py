"""
1.4.2 Het Knabbelpark: the wiki entries (CONTRACT_130 2.4; not in FEATURES: the docs step reads WIKI). The blocks, the
Hertguhtje's Guhdex page, the NPCs and the structure get their own pages from the game data; this adds the questline,
where the NPCs live and the paragraphs of the park's page. (The English of these texts belongs in tools/wiki_bbq2/en.py:
the merge step's wiki pass.)
"""
from features import wereld

WIKI = {
    "verhalen": dict([
        wereld.wiki_questlijn(
            "knabbelpark", "Een dagje in het Knabbelpark",
            "In het huisje van elk Knabbelpark zit de parkwachter-guh achter zijn toonbank. Help hem een dagje: lees de drie "
            "plaquettes in de beeldentuin, geef elk Hertguhtje in de hertenkooi een kaasknabbel en breng de kooi-verzorger-guh zes "
            "tarwe voor de ruif. Dan krijg je je eigen Hertguhtje en een beeldje van je guh.",
            "knabbelpark", ["parkwachterguh", "kooiverzorgerguh"], [("guhs_minigames", "park")]),
    ]),
    "systemen": {},
    "npc_home": {"parkwachterguh": "bouwwerken/knabbelpark", "kooiverzorgerguh": "bouwwerken/knabbelpark"},
    "entity_home": {},
    "tekst": [
        ("bouwwerken/knabbelpark", "Een park dat groeit als een dorp",
         "Geen Knabbelpark is hetzelfde. In het midden ligt altijd het fonteinplein, met op het eilandje in de fontein de Reisguh "
         "'Knabbelpark'. Zij hoort bij het plein: je kunt haar niet oppakken en haar naam blijft zoals hij is. Vanaf het plein "
         "lopen wandelpaadjes over de heuvels, met heggen, bloemen, guhlantaarns, parkbankjes en hier en daar een guhprullenbak. "
         "Langs de paadjes liggen plekjes: een bloemenweide, een rustplekje, een bosje, een grote bloesemboom."),
        ("bouwwerken/knabbelpark", "Wat er altijd is",
         "Aan het plein vast liggen altijd drie dingen, zodat de parkwachter-guh altijd geholpen kan worden: de beeldentuin met de "
         "beelden van Joostvadsje, trapguh en Suusvadsje, Liekguh en Lunde, en Opa Kaasguh (met een plaquette om te lezen), de hertenkooi met drie "
         "Hertguhtjes en de kooi-verzorger-guh, en het huisje van de parkwachter."),
        ("bouwwerken/knabbelpark", "Waar vind je het?",
         "Het Knabbelpark staat alleen in de Guhmensie, op droog en zacht glooiend land. Er is er altijd één binnen een flink stukje "
         "lopen in land waar nog niemand was, en verder heel af en toe eentje. Het Superkompas wijst de weg (tabblad Knus)."),
        ("bouwwerken/knabbelpark", "De guhprullenbak",
         "Gooi iets in een guhprullenbak (rechtsklik met iets in je hand) en hij eet er één van op: njeg! Gereedschap en andere mooie "
         "spullen spuugt hij gewoon weer uit. Je kunt ze ook zelf maken, net als het parkbankje en de guhlantaarn."),
    ],
}
