"""
1.8.0 "Dromen & Vadskracht": what the wiki says about the update (design: MineScapeStory/ontwerp-guhs180-dromen.md and
ontwerp-guhs180-vadskracht.md). Not a feature module (not in FEATURES): the docs step (tools/wiki_bbq2) reads WIKI; the
English is in tools/wiki_bbq2/en_dromen180.py. No story spoilers: the wiki teases Snoes' dream, it does not walk through it.
"""
WIKI = {
    "verhalen": {},
    "systemen": {
        "guhdromenland": (
            "Guhdromenland: de droomwereld", "block:guhs:droomzandblok",
            "Stop je guh 's avonds in, ga naast hem slapen en word wakker in Guhdromenland: zwevende eilanden onder een eeuwige "
            "paarsroze schemer met een slaperige guh-maan. Er is daar een guhtje dat je hulp heel goed kan gebruiken.",
            ["slaapzorg-en-bedden", "blub-guh", "nieuw-in-1-8"]),
        "blub-guh": (
            "De Blub-guh", "block:guhs:blub_knuffel",
            "Een zacht, huppelend, piepend wezentje met een eigen Blub-Guh-Meter: van 100% Blub tot helemaal guh. Vijf soorten, "
            "pastelkleurtjes, een eigen grootte, kleding, trucjes en eigen klusjes in je guhhuisje.",
            ["zingende-weide", "guhdromenland", "nieuw-in-1-8"]),
        "zingende-weide": (
            "De Zingende Weide", "block:guhs:zingbloemetje_roze",
            "Een zeldzaam nieuw biome in de Guhmensie met roze kliffen, reuzen-wolkjesbomen, bubbelsap, de Guhwand en 's nachts "
            "lichtzuilen. Hier wonen de Blubs, en bij schemer zingen ze in de Koorkom. Alleen in nieuwe chunks.",
            ["blub-guh", "nieuw-in-1-8"]),
        "slaapzorg-en-bedden": (
            "Slapen als een guh", "block:guhs:muziekdoosje",
            "Guhs worden 's avonds slaperig en zoeken zelf een slaapplekje. Met nieuwe bedden voor jou en voor guhs, Blubs, "
            "muisjes en Schilly's, familiebedden, het Knuffelnest, slaapkleding, deco en de Guhvilla.",
            ["guhdromenland", "guhvilla", "nieuw-in-1-8"]),
        "guhvilla": (
            "De Guhvilla", "block:guhs:guhhuisje_villa",
            "Het grootste guhhuisje: plek voor 20 bewonertjes, een ruime binnenkant vol nieuwe bedden en een groter klusgebied. "
            "Alle guhhuisjes hebben nu een overzichtelijker menu en een schuifje voor het klusgebied.",
            ["slaapzorg-en-bedden", "systemen/guhhuisje", "nieuw-in-1-8"]),
        "weer-vadser": (
            "De Weer vadser", "block:guhs:weer_vadser",
            "Maak je eigen weertje in een zone rond het blok: zon, regen, onweer en negen guh-weertjes die je met weerpotjes "
            "ontgrendelt. Groter met Wolkenantennes of het Knabbel-weerstation. Kost vadskracht.",
            ["vadskracht-2", "nieuw-in-1-8"]),
        "vadskracht-2": (
            "Vadskracht: de Fabriekshal en de Koepel", "block:guhs:knabbelketel",
            "Nieuwe bronnen, fabrieken en grote projecten: de Knabbelketel, de Bouwblokken-fabriek, de Wolfabriek, de Kaaskas, "
            "Kleurmolen en Regenboogpers, de Mega-knuffelcentrale, het Hamsterbuis-net, de Fossiel-broedmachine en de Kaasraket. "
            "Allemaal te zien in de vernieuwde Guhtechniek-beurs.",
            ["weer-vadser", "bouwwerken/guhtechniek_beurs", "nieuw-in-1-8"]),
        "gezelschapshoek": (
            "De Gezelschapshoek in Guhpixel", "opeenrij_schijfje",
            "Drie nieuwe spelletjes in de lobby van Guhpixel: Kussengevecht op een reuzenbed, Wie is het met twintig guhs en Guh op "
            "een rij tegen een bot of een vriend.",
            ["nieuw-in-1-8"]),
        "nieuw-in-1-8": (
            "Nieuw in 1.8.0: Dromen & Vadskracht", "block:guhs:droomzandblok",
            "Wat er nieuw is in Guhs 1.8.0: Guhdromenland, de Blub-guh, de Zingende Weide, slapen als een guh met nieuwe bedden en "
            "de Guhvilla, de Weer vadser, een tweede golf vadskracht met de Fabriekshal en de Koepel, en de Gezelschapshoek. De "
            "volledige lijst staat in het CHANGELOG van de mod.",
            ["guhdromenland", "blub-guh", "zingende-weide", "slaapzorg-en-bedden", "guhvilla", "weer-vadser", "vadskracht-2",
             "gezelschapshoek", "bouwwerken/guhtechniek_beurs"]),
    },
    "tekst": [
        ("systemen/nieuw-in-1-8", "Dromen",
         "Guhdromenland met zes droomeilanden, een droomplein met droomwinkeltje, een eigen droomwolkje om op te bouwen, "
         "droomgasten en een lief verhaal voor wie gaat slapen. Nieuwe droomblokken, droomwezens en een slaperige guh-maan."),
        ("systemen/nieuw-in-1-8", "Wezens en plekken",
         "De Blub-guh met de Blub-Guh-Meter, kleding, trucjes, klusjes en de meterkaart in de Guhdex; het biome De Zingende Weide "
         "met wolkjeshout, bubbelsap, de Koorkom en de Dirigent; roze graan; Wolkenschaapjes die je kunt scheren en kweken."),
        ("systemen/nieuw-in-1-8", "Thuis",
         "Slaperige guhs, slaapplekjes, ontbijt op bed, muziekdoosjes en maanlampjes; vier spelerbedden, vier diertjesbedden, "
         "zeven familiebedden en het Knuffelnest; slaapkleding en een droompyjama; de Guhvilla en een klus-schuifje voor alle huisjes."),
        ("systemen/nieuw-in-1-8", "Vadskracht",
         "De Weer vadser, de Knabbelketel, de Knuffelsteen-generator, saus-buizen en -tanks, de Bouwblokken-fabriek, de Wolfabriek, "
         "Kleurmolen en Regenboogpers, de Kaaskas, de Sorteerband, de Mega-knuffelcentrale, het Hamsterbuis-net, de "
         "Fossiel-broedmachine met vier oerguhs, de Kaasraket naar de Kaasmaan en export bij de guh-villagers. Guhraden geven na "
         "de eerste vier in een net steeds minder. De Guhtechniek-beurs kreeg er een aanbouw, de Fabriekshal en de Koepel bij."),
        ("systemen/nieuw-in-1-8", "En verder",
         "De Gezelschapshoek in Guhpixel, tooltips bij alle effecten, een nieuw techniek-hoofdstuk in de questboek en fixes, "
         "zoals grote guhs die niet meer stikken bij het spawnen."),
    ],
}
