"""
bbq2 (tech-bezorg) - what the wiki should say about the Bezorgguhtje (CONTRACT_130 2.4: not in FEATURES; the docs step of
the merge turns this dict into pages). The Stepstation, the Haltepaaltje, the whistle and the Bezorgguhtje get their item /
block / creature pages from the game data by themselves; this adds the system page that explains how they work together.
"""
from features import tech_bezorg, vadskracht

BEREIK, HALTES, RUGZAK = tech_bezorg.getal("BEREIK"), tech_bezorg.getal("MAX_HALTES"), tech_bezorg.getal("RUGZAK")
VK = vadskracht.getal("STEPSTATION")

WIKI = {
    "systemen": {
        "bezorgguhtje": ("Het Bezorgguhtje", "entity/bezorgguhtje",
                         "Een mini-guh op een step met een veel te grote rugzak. Hij woont in een Stepstation en brengt spullen "
                         "van Haltepaaltje naar Haltepaaltje. Tuut tuut, njeg!",
                         ["vadskracht", "knabbelbuizen", "hapluikje"]),
    },
    "entity_home": {"bezorgguhtje": "blokken/stepstation"},
    "tekst": [
        ("systemen/bezorgguhtje", "Zo werkt het",
         f"Zet een Stepstation neer en geef het {VK} vadskracht (een Guhrad met een guh erin is genoeg). Er komt meteen een "
         "Bezorgguhtje wonen: hij parkeert zijn stepje voor het station. Zonder vadskracht stept hij naar huis en gaat hij slapen.\n\n"
         f"Zet daarna Haltepaaltjes tegen je kisten en machines, tot {BEREIK} blokken van het station. Een paaltje hoort vanzelf bij "
         f"het dichtstbijzijnde Stepstation (hooguit {HALTES} haltes per station). Het eerste paaltje haalt op (groen bord, pijl "
         "omhoog), het tweede levert af (oranje bord, pijl omlaag). Klik op een paaltje om dat te wisselen, om het aan een ander "
         "station te koppelen en om in te stellen wat er mee mag: leg tot negen soorten spullen in het filter, of laat het leeg "
         "voor alles.\n\n"
         f"Het Bezorgguhtje rijdt de haltes af in de volgorde van het scherm van het Stepstation (met de pijltjes zet je een halte "
         f"eerder of later in de ronde). In zijn rugzak passen {RUGZAK} stapels. Bij een ophaal-halte pakt hij alleen wat een "
         "aflever-halte ook echt kwijt kan, dus zijn rugzak loopt niet vol met spullen die niemand wil."),
        ("systemen/bezorgguhtje", "Ophalen en afleveren",
         "Afleveren gaat in de kist aan de kant waar het paaltje staat, net als bij een trechter. Zet het paaltje dus bovenop een "
         "oven om hem te vullen. Ophalen werkt als een trechter onder de kist: bij een oven krijg je wat klaar is, en de "
         "brandstof blijft liggen. Het werkt met alles waar spullen in kunnen: kisten, tonnen, guh-machines, de Bank Guh en het "
         "Hapluikje."),
        ("systemen/bezorgguhtje", "Nooit iets kwijt",
         "De spullen in de rugzak worden bewaard door het Stepstation. Wat er ook met het Bezorgguhtje gebeurt: er raakt niets "
         "weg. Is de weg dicht of veel te lang, dan hupt hij met een poefje naar de halte. Raakt hij echt zoek, dan staat er even "
         "later een nieuw Bezorgguhtje bij het station, met dezelfde rugzak. Breek je het Stepstation af, dan valt alles uit de "
         "rugzak op de grond. Een Bezorgguhtje kan geen pijn hebben en doet niemand pijn. Hij werkt alleen in geladen chunks: "
         "een halte die te ver weg of niet geladen is slaat hij over."),
        ("systemen/bezorgguhtje", "Het fluitje",
         "De torenwachter-guh van de Rookguh-vuurtoren geeft je een Bezorgguhtje-fluitje. Fluit erop en het dichtstbijzijnde "
         f"Bezorgguhtje van een van je eigen Stepstations (binnen {BEREIK} blokken) laat alles vallen en komt naar je toe; zijn "
         "rugzak gaat voor je open. Fluit je terwijl je sluipt, dan gaan al je Bezorgguhtjes naar huis en beginnen ze opnieuw aan "
         "hun ronde. Geef hem een kaasknabbel en hij stept een minuut lang extra hard. Vahoeg!"),
    ],
}
