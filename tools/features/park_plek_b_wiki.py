"""
1.4.2 Het Knabbelpark, park-plekjes b: the wiki entries (CONTRACT_130 2.4; not in FEATURES: the docs step reads WIKI). The
blocks, the Duifguhtje's Guhdex page and the NPCs get their own pages from the game data; this adds where they live and the
paragraphs about the five plekjes on the park's page. (The English belongs in tools/wiki_bbq2/en.py: the merge step's wiki pass.)
"""

WIKI = {
    "verhalen": {},
    "systemen": {},
    "npc_home": {"omaguh": "bouwwerken/knabbelpark", "mokmika": "bouwwerken/knabbelpark", "guhkindje": "bouwwerken/knabbelpark"},
    "entity_home": {"duifguhtje": "bouwwerken/knabbelpark"},
    "tekst": [
        ("bouwwerken/knabbelpark", "Oma-guh en de Duifguhtjes",
         "Op sommige plekjes zit Oma-guh op haar bankje de Duifguhtjes te voeren: mollige grijze vogeltjes met een glimmend "
         "groen-paars nekje en echte guh-oogjes. Praat met haar en je krijgt een handje broodkruimels (één keer per dag). Strooi ze "
         "(rechtsklik) en de Duifguhtjes komen er meteen op af; houd je kruimels vast, dan fladderen ze om je heen. Roekoe!"),
        ("bouwwerken/knabbelpark", "De uitkijktoren",
         "Heeft een park een uitkijktoren, dan staat die altijd op het hoogste plekje van het park. Een wenteltrap brengt je naar "
         "het dak, en daar staan twee verrekijkers: rechtsklik en je kijkt ver weg, net als door een echte verrekijker. Sluip om "
         "weer gewoon te kijken."),
        ("bouwwerken/knabbelpark", "De poppenkast",
         "Klik op het podium van de poppenkast en de voorstelling begint: een guh legt zijn kaasknabbel neer, een Mika wil hem "
         "stiekem pakken en de guhkindjes op de bankjes roepen zo hard ze kunnen: ACHTER JE! Gelukkig delen ze aan het eind."),
        ("bouwwerken/knabbelpark", "De wensboom",
         "Hernoem een papiertje op een aambeeld: dat is je wens. Rechtsklik er de stam van de wensboom mee en je wens hangt op een "
         "bordje onder een tak, voor iedereen om te lezen. Je mag er twee per boom; sluip met een lege hand om je eigen wensen weer "
         "weg te halen."),
        ("bouwwerken/knabbelpark", "Het Mika-bankje",
         "Er staat een bordje 'Verboden voor Mika's' bij, maar er zit altijd een mokkende Mika op. Kom je te dichtbij, dan duwt hij "
         "je weg (hij doet nooit pijn). Geef hem een kaasknabbel en hij huppelt blij weg; de volgende dag zit hij er gewoon weer."),
    ],
}
