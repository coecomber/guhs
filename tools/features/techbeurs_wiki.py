"""
1.7.0 (ronde 45-47): the wiki entries of the Guhtechniek-beurs (not in FEATURES: the docs step reads WIKI). The structure,
its NPCs and the Vadslamp / Vadsschakelaar get their own pages from the game data; this adds where the beursguhs live and
the paragraphs of the beurs' page. The English: tools/wiki_bbq2/en_techbeurs.py.
"""

WIKI = {
    "systemen": {},
    "npc_home": {},
    "entity_home": {},
    "tekst": [
        ("bouwwerken/guhtechniek_beurs", "Een beurs waar alles echt werkt",
         "Een grote glazen hal op het land van de Guhmensie. Elk onderdeel van de Guh-technologie heeft er een eigen stand, en "
         "alles draait echt: de Guhrad, de Knuffelgenerator en de Disco-dynamo met hun guhs, het Blubkacheltje met de "
         "Knabbelbatterij, de Gloeisterkern in zijn kooitje, de Vadsmolen en de Guhoven, de Oogster op zijn akkertje, Knabbelbuizen "
         "met een Filterstuk en een Voorraadmeter, de Opzuiger met een Guhklok, de Knabbelaar en de Neerzetter, twee "
         "Knutselmachines met een Tekentafel, de Plantagebak, de Sauspomp met een Sausvat, de Brouwautomaat, de "
         "Frituurautomaat, de Grillkoolpers en een Stepstation met zijn Bezorgguhtje."),
        ("bouwwerken/guhtechniek_beurs", "Bordjes, schakelaars en beursguhs",
         "Bij elke stand staat een bordje met hoeveel vadskracht het onderdeel gebruikt of geeft, een Vadsschakelaar om de stand "
         "uit en weer aan te zetten, en een beursguh met een grapje en een tip: welk quest je leert hoe je het zelf bouwt. "
         "Vadslampen zorgen voor licht. Kijken mag, pakken niet: de kisten en machines van de stands kun je niet openen, en de hal "
         "is beschermd."),
        ("bouwwerken/guhtechniek_beurs", "De Droomfabriek",
         "In het midden staat de Droomfabriek, op een eigen Gloeisterkern: een Oogster oogst kaasknabbelplantjes, Knabbelbuizen "
         "brengen de knabbels door een Filterstuk naar de Frituurautomaat, en de gefrituurde knabbels gaan naar de "
         "Brouwautomaat, die er vahoege drankjes van brouwt. Zet de grote schakelaar bij de kern uit en alles valt in slaap."),
        ("bouwwerken/guhtechniek_beurs", "Waar vind je hem?",
         "Er staat altijd één Guhtechniek-beurs op 800 tot 1500 blokken van het begin van je wereld, in land waar nog niemand "
         "was, en verder heel af en toe eentje. Het Superkompas wijst de weg (tabblad Knus)."),
    ],
}
