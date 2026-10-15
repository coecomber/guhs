"""
Het Guhpad - what the wiki must say (CONTRACT_130 2.4; the docs step turns this into pages; this module is not in FEATURES
and builds nothing). One system page, and lines for pages that exist: the Knabbelring, the Guheinde, the Superkompas, the
Guhdex and the quest book.

FOR THE DOCS STEP: the FTB chapters guhs_knabbelring and guhs_guhrio are gone: their sections (the same section ids) are in
guhs_pad_barbecuether now (tools/make_ftbquests.py VERHUISD). The wiki notes of the ring and guhrio slices still name the old
chapters in ftb=[(chapter, sid)]: map them with VERHUISD. "Guhverhalen" is called "Verhalen van de Guhmensie" and "Het
Guheinde" (the chapter) "Verhalen van het Guheinde"; both kept their file names (guhs_verhalen, guhs_guheinde).
"""
from features import guhpad

GROOT = ", ".join(guhpad.VERHALEN[v][2] for v in guhpad.VERHALEN)

WIKI = {
    "verhalen": {},
    "systemen": {
        "het-guhpad": (
            "Het Guhpad", "gui_guhpad_padkaart",
            "De grote verhalen openen de werelden. Eerst de verhalen van de Guhmensie, dan pas begint Guhdalf met de Knabbelring en "
            "laat het grillportaal je door naar de Guhbarbecuether; daarna openen de Knabbelring, Super Guhrio en de Aangebrande Mika "
            "het Guheinde. Helemaal aan het eind ligt Het echte Guheinde: daar weet nog niemand iets van.",
            ["verhalen/knabbelring", "verhalen/super-guhrio", "guhdex", "guhmensie_superkompas"]),
    },
    "npc_home": {},
    "entity_home": {},
    "tekst": [
        ("systemen/het-guhpad", "De grote verhalen",
         f"Alleen de grote verhalen tellen mee voor het Guhpad: {GROOT}. De Timmerguh, de Grillguh, de bouwwerken van de "
         "Guhbarbecuether, de beroepen en de Guh-technologie zijn er gewoon bij voor de gezelligheid: die hoef je er niet voor te doen."),
        ("systemen/het-guhpad", "Wat gaat wanneer open?",
         "De Guhmensie: meteen, voor iedereen. De Knabbelring: Guhdalf begint er pas aan als je alle grote verhalen van de Guhmensie hebt "
         "gevolgd (en de barbecue van de Grillguh weer brandt). De Guhbarbecuether: het grillportaal laat je door na die verhalen en na "
         "het knabbelfeest van Guhdalf (hoofdstuk 1 van de Knabbelring). Het Guheinde: het portaal in de Knabbelkelder laat je door als je "
         "de Knabbelring en Super Guhrio hebt gevolgd en de Aangebrande Mika hebt verslagen."),
        ("systemen/het-guhpad", "Wat mis ik nog?",
         "Guhdalf en de twee portalen zeggen het je: je krijgt een lijstje met precies de verhalen die je nog mist. In je Guhdex, tab "
         "Verhalen, staat bovenaan de padkaart van het hele Guhpad, met een vinkje waar je klaar bent en een slotje waar je nog niet mag "
         "komen; wijs een halte aan en je ziet wat hij nog vraagt. Daaronder staan alle verhalen per wereld, en elke wereld kun je in- en "
         "uitklappen. Een groot verhaal herken je aan het gouden sterretje."),
        ("systemen/het-guhpad", "Was je al verder?",
         "Het Guhpad geldt voor iedereen, ook als je voor de update al in de Guhbarbecuether of het Guheinde was. Sta je er nog, dan mag "
         "je er blijven zolang je wilt: niemand wordt weggehaald, en eruit kun je altijd. Maar ben je er eenmaal uit, dan kom je er pas "
         "weer in als je de verhalen hebt gevolgd. Alles wat je al gedaan had blijft bewaard."),
        ("systemen/het-guhpad", "Mijn verhaal",
         "Kies in je Guhmensie-superkompas de keuze Mijn verhaal (de eerste keuze van elke tab, naast de plekken). Volg je een verhaal "
         "(Guhdex, tab Verhalen, 'Volg dit verhaal'), dan wijst het kompas naar de volgende stap daarvan. Volg je er geen, dan wijst het "
         "naar het dichtstbijzijnde grote verhaal dat je nog niet hebt gedaan. Is dat in een andere wereld, dan wijst het naar het "
         "portaal waar je het laatst doorheen kwam."),
        ("systemen/het-guhpad", "In het questboek",
         "Onderaan de zijbalk van het questboek staat de groep Het Guhpad met vier hoofdstukken: Verhalen van de Guhmensie, Verhalen van "
         "de Guhbarbecuether, Verhalen van het Guheinde en Het echte Guheinde. Elk hoofdstuk begint met een slotquest: die laat met een "
         "vinkje per verhaal zien wat je eerst nog moet doen."),
        ("systemen/het-guhpad", "Het echte Guheinde",
         "Een hoofdstuk vol zwarte schimmen en vraagtekens. Het enige dat bekend is: je komt er alleen door alle verhalen van de "
         "Guhmensie, de Guhbarbecuether en het Guheinde te volgen. Eén teller houdt het bij: 'Verhalen gevolgd'. Onderaan staat een rijtje "
         "vraagtekens voor laag 6 van de Guh-technologie: die gaat pas open in het echte Guheinde."),
        ("verhalen/knabbelring", "Eerst de Guhmensie",
         "Guhdalf begint pas met de Knabbelring als je alle grote verhalen van de Guhmensie hebt gevolgd. Mis je er nog een, dan zegt hij "
         "welke. Zie Het Guhpad."),
        ("guheinde_portaal", "Op slot",
         "Het portaal in de Knabbelkelder laat je pas door als je de Knabbelring en Super Guhrio hebt gevolgd en de Aangebrande Mika hebt "
         "verslagen. Terug uit het Guheinde kan altijd. Zie Het Guhpad."),
        ("barbecuether_portaal", "Op slot",
         "Het grillportaal laat je vanuit de Guhmensie pas door als je de grote verhalen van de Guhmensie en het knabbelfeest van Guhdalf "
         "hebt gedaan. Terug uit de Guhbarbecuether kan altijd. Zie Het Guhpad."),
    ],
}
