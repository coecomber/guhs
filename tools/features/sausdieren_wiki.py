"""
Wiki texts of bbq2 (sausdieren): the Sausloper-stal and its questline, the Sausloper, the Sausblubje, blubroom and the
Stuiterdrankje. Not a feature module (not in FEATURES): the docs step of the merge reads WIKI and turns it into pages
(CONTRACT_130 2.4). The items, the structure, the NPC and the two creatures get their own wiki pages from the game data.
"""
from features import wereld

WIKI = {
    "verhalen": dict([wereld.wiki_questlijn(
        "sausloper", "De Sausloper-stal",
        "Langs de frituursauszee van de Guhbarbecuether staat een stal met een koperen dak en roze guh-oren. De Verzorger-guh leert je "
        "daar hoe je vrienden wordt met een Sausloper: lokken met pindasaus aan een stok, voeren met pindascheutjes en een proefrondje "
        "door de sausbak. Daarna krijg je een zadel en mag je zelf een wilde Sausloper temmen.",
        "sausloper_stal", ["verzorgerguh"], [("guhs_barbecuether", "sausdieren_stal")], related=["systemen/sausdieren"])]),
    "systemen": {
        "sausdieren": ("Sauslopers en Sausblubjes", "entity_sausloper",
                       "Twee lieve dieren van de frituursauszee. De Sausloper stapt op lange poten over de saus en draagt je naar de "
                       "overkant. Het Sausblubje stuitert rond en laat blubroom achter als je het knuffelt.",
                       ["verhalen/sausloper", "dimensies/barbecuether"]),
    },
    "npc_home": {"verzorgerguh": "bouwwerken/sausloper_stal"},
    "entity_home": {"sausloper": "bouwwerken/sausloper_stal", "sausblubje": "dimensies/barbecuether"},
    "tekst": [
        ("verhalen/sausloper", "Zo gaat het",
         "Praat met de Verzorger-guh: je krijgt pindasaus aan een stok. Houd de stok vast en lok een Sausloper uit de sausbak naar hem toe. "
         "Voer daarna een Sausloper van de stal drie pindascheutjes (de eerste drie krijg je). Vraag dan om je proefrit: er komt een "
         "gezadelde Sausloper naar de steiger, speciaal voor jou. Stap op en rijd door de vier poortjes; het volgende poortje glinstert. "
         "Terug bij de Verzorger-guh krijg je een zadel."),
        ("verhalen/sausloper", "Met zoveel als je wilt",
         "Iedereen doet de les voor zichzelf. De Sauslopers van de stal zijn van niemand: je mag ze voeren en aaien, maar meenemen kan "
         "niet. Bij de proefrit krijgt elke speler een eigen Sausloper, dus je hoeft nooit op elkaar te wachten. Mislukken kan niet: er "
         "loopt geen klok en als je verdwaalt sta je zo weer bij de start."),
        ("verhalen/sausloper", "Nog een rondje",
         "Na de les klokt de Verzorger-guh elk rondje. Je snelste tijd wordt onthouden. Haal je de finish binnen dertig seconden, dan ben "
         "je een echte Sausracer. Tip: rechtsklik met de stok voor een sprintje."),
        ("systemen/sausdieren", "De Sausloper",
         "Een ronde guh op twee heel lange poten, met een kuif van frietjes. Hij loopt over kaasfrituursaus alsof het een stoep is en "
         "brandt nooit. Op het droge bibbert hij en loopt hij traag, behalve op iets warms zoals gloeikool. Wilde Sauslopers komen uit de "
         "sauszee omhoog. Na de les in de Sausloper-stal tem je er een met pindascheutjes of een pindasausplasje (één op drie per hapje). "
         "Leg een zadel op je eigen Sausloper, klik om op te stappen en houd pindasaus aan een stok vast: hij loopt waar jij heen kijkt. "
         "Zolang je rijdt kan de saus je niks doen. Sluip en klik om hem te laten wachten."),
        ("systemen/sausdieren", "Pindasaus aan een stok",
         "Je krijgt hem van de Verzorger-guh. Kwijt of op? Maak een nieuwe van een hengel en een pindasausplasje. Rechtsklik tijdens het "
         "rijden en je Sausloper zet een sprintje in; daar gaat wel een likje pindasaus vanaf."),
        ("systemen/sausdieren", "Het Sausblubje",
         "Een stuiterend blubje saus met een korstje, in drie maten. Het doet niemand kwaad en je kunt het geen pijn doen. Geef een groot "
         "of middelgroot blubje een knuffel (rechtsklik met een lege hand) of een kaasknabbel en het splitst in twee kleinere blubjes. "
         "Elke keer blijft er een klodder blubroom liggen. Een klein blubje groeit van drie knabbels weer een maatje. Zo blijft een hokje "
         "blubjes blubben zolang jij voert en knuffelt."),
        ("systemen/sausdieren", "Blubje in een potje",
         "Een klein Sausblubje past in een glazen flesje: rechtsklik en je hebt een Sausblubje in een potje. Klik met het potje op een "
         "blok om het weer vrij te laten. De uitvinder-guh gebruikt zo'n potje voor zijn Blubkacheltje."),
        ("systemen/sausdieren", "Blubroom en het Stuiterdrankje",
         "Roer blubroom door kaasbouillon in de Guhbrouwketel en tap een Stuiterdrankje. Drie minuten lang doet vallen geen pijn: je "
         "stuitert weer omhoog, elke keer wat minder hoog. Wie sluipt, landt gewoon. Blubroom zit ook in recepten van Guh-technologie."),
    ],
}
