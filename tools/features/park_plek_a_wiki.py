"""
1.4.2 Het Knabbelpark, plekjes A: the wiki entries (CONTRACT_130 2.4; not in FEATURES: the docs step reads WIKI). The
blocks, the Knabbelkoi's Guhdex page, the kraampjesguh and the park get their own pages from the game data; this adds where
the kraampjesguh lives and the paragraphs about these plekjes on the park's page. (The English of these texts belongs in
tools/wiki_bbq2/en.py: the merge step's wiki pass.)
"""

WIKI = {
    "verhalen": {},
    "systemen": {},
    "npc_home": {"kraamguh": "bouwwerken/knabbelpark"},
    "entity_home": {"knabbelkoi": "bouwwerken/knabbelpark"},
    "tekst": [
        ("bouwwerken/knabbelpark", "De eendenvijver",
         "In de eendenvijver zwemmen twee guh-eendjes met een rijtje kuikentjes achter zich aan. Geef ze brood, zaadjes of een "
         "kaasknabbel: kwak-njeg! Aan het steigertje ligt altijd een roeibootje klaar, ook als iemand anders er net mee weg is."),
        ("bouwwerken/knabbelpark", "De speeltuin",
         "Speeltuin De Vrolijke Vads heeft een glijbaan (klim de ladder op en stap erin), twee schommels en een wip voor twee "
         "(rechtsklik om te gaan zitten). In de zandbak kun je graven: één keer per dag vind je er een kaasknabbel. De glijbaan, "
         "de schommel en de wip kun je ook zelf maken of kopen, voor een speeltuin bij je eigen huis."),
        ("bouwwerken/knabbelpark", "De picknickweide",
         "Op de picknickweide liggen geruite picknickkleden met thee en kussens. De kraampjesguh achter zijn kraampje verkoopt "
         "kaasknabbels, koekjes, taart, picknickkleden, parkbankjes en speeltuinspullen. Betalen doe je met vahoege vads."),
        ("bouwwerken/knabbelpark", "De muziektuin",
         "Tussen de bloembedden en een kleine kas staat de muziektent. Klik op de jukebox: de platenkast van het park kiest een "
         "plaatje en de drie guhs in de tent gaan dansen. Nog een klik zet de muziek weer uit. Een eigen plaatje mag ook."),
        ("bouwwerken/knabbelpark", "De japanse tuin",
         "Een beekje stroomt onder een rood boogbruggetje door naar een koivijver met Knabbelkoi's: kois in kaasknabbelkleuren, "
         "een knipoog naar het Klaterdal. Geef ze een kaasknabbel of koivoer en ze springen van blijdschap uit het water. Over het "
         "bruggetje ligt een zenhoekje met geharkt zand, een toro en bamboe."),
    ],
}
