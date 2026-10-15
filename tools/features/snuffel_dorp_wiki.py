"""
Wiki texts of Het Snuffeleiland, slice snuffel-dorp: the island, Snuffeldorp and its residents, and the first series of
the story step by step. Not a feature module (not in FEATURES): the docs step of the merge reads WIKI and turns it into
pages (CONTRACT_130 2.4). How being a dog works (sniffing, ranks, the Guhstation) is the kern's snuffel_wiki.py; the dock
and the way to the island are the dock slice's.
"""

WIKI = {
    "verhalen": {
        "snuffeldorp": dict(
            nl="Snuffeldorp en de eerste snuffelreeks", img="snuffel_dorp",
            lead_nl="Je spoelt aan op het strand van het Snuffeleiland en wordt wakker als hond. In Snuffeldorp leer je snuffelen, krijg je een "
                    "ondeugend maatje, help je de dorpelingen en haal je je snuffeldiploma. Helemaal aan het eind vind je een spoor van papa.",
            ftb=[("guhs_verhalen", "snuffel"), ("guhs_verhalen", "snuffel_dorp")], structure="steigerhuisje", npcs=[],
            related=["verhalen/snuffeleiland", "systemen/snuffelen", "systemen/guhstation", "systemen/snuffeldorp_bewoners"]),
    },
    "systemen": {
        "snuffeldorp_bewoners": ("De honden van Snuffeldorp", "snuffel_bewoners",
                                 "Op het Snuffeleiland woont iedereen op vier poten. Tien honden kom je in het eerste verhaal tegen. "
                                 "Zes van hen zijn iets kwijt.",
                                 ["verhalen/snuffeldorp", "systemen/snuffelen"]),
    },
    "tekst": [
        ("verhalen/snuffeldorp", "Het eiland",
         "Het Snuffeleiland ligt in zijn eigen zee. In het zuidwesten ligt het brede strand waar je aanspoelt, met palmen en een zandkasteel. "
         "Een plankenpad loopt naar het strandpoortje van Snuffeldorp. Midden in het dorp ligt het plein met de put. Daaromheen staan de "
         "dokterspraktijk (het witte huis met het rode kruis), de bakkerij, het schooltje, het huisje van Oma Wolletje en het huis met de "
         "moestuin van Tuinder Knolletje. Aan de oostkant ligt de haven met de steiger en de boot van Kapitein Zoutsnoet. Door het weipoortje "
         "aan de noordkant kom je in de wei: daar staat het snuffelschooltje van Meester Truffelneus en, op een heuveltje, de stenenkrans "
         "waar het boompje van je maatje groeit."),
        ("verhalen/snuffeldorp", "De wegversperring",
         "Achter de wei loopt de weg verder, tussen de rotsen door. Daar staat een vriendelijke wegversperring met een bordje: 'Hier mag je "
         "pas door als Snuffelneus'. Erachter zie je bos, een heuvel met staande stenen en een vuurtoren. Een Snuffelpup komt er niet "
         "langs, ook niet zwemmend: je wordt gewoon weer teruggezet. Dat deel van het eiland hoort bij een later verhaal."),
        ("verhalen/snuffeldorp", "1. Aangespoeld",
         "Golven, een meeuw, en een natte snuit boven je: Jutje Kwispel heeft je op het strand gevonden. Je kijkt naar beneden en ziet... "
         "pootjes. Je bent een hond! Volg het plankenpad en praat met Jutje bij het strandpoortje."),
        ("verhalen/snuffeldorp", "2. Naar de dokter",
         "Dokter Pleisterpoot hoort je verhaal. De geneesbloem bestaat echt en groeit diep op het eiland, maar je kunt hem niet zien, "
         "alleen ruiken. Daarvoor heb je een echte snuffelneus nodig."),
        ("verhalen/snuffeldorp", "3. Snuffelles",
         "Meester Truffelneus geeft drie lessen in de wei, steeds met een echte geur. Les 1: een begraven kluifje vlakbij (oranje: iets "
         "lekkers). Les 2: zijn fluitje, verder weg (blauw: een ding). Les 3: de bijen in de grote eik (groen: een dier), die je niet "
         "opgraaft maar van dichtbij besnuffelt. Na elke les ga je terug naar de meester."),
        ("verhalen/snuffeldorp", "4. Er rommelt iets",
         "Op het plein rammelt een emmer en valt van de put, helemaal vanzelf. Niemand ziet wie het doet. Alleen jij ziet het: een "
         "ondeugend bosgeestje. Het schrikt ervan dat jij het kunt zien, en blijft bij je. Dat is je maatje. Het ruikt een beetje vreemd "
         "(paars)."),
        ("verhalen/snuffeldorp", "5. Goede daden",
         "Je maatje heeft van alles verstopt en wil het goedmaken. Praat met de dorpelingen: de bakker mist zijn deegroller, de visser zijn "
         "dobber, de juf de schoolbel, oma haar bol wol, de tuinder zijn gietertje en Kleine Kwijlebal zijn stuiterbal. Vraag ernaar, "
         "snuffel het op, graaf het uit en breng het terug. Van elke goede daad groeit het boompje een stap, en je ziet het groeien. "
         "Vier goede daden zijn genoeg; de andere twee kun je altijd nog doen."),
        ("verhalen/snuffeldorp", "6. Het snuffelexamen",
         "Is het boompje een jong boompje, dan mag je examen doen. Meester Truffelneus heeft vier geuren verstopt, van elke kleur één: "
         "twee liggen begraven, twee hangen ergens. Zakken bestaat niet en er is geen klok. Daarna krijg je je diploma: je bent "
         "Snuffelpup, rang 1 (de laagste) van 5 (de hoogste)."),
        ("verhalen/snuffeldorp", "7. Een spoor van papa",
         "Bij het boompje geeft je maatje je een bloesemtakje. En tussen de stenen ruik je iets bekends. Je graaft: een blauwe sjaal met "
         "strepen. Papa is hier geweest! Zijn spoor loopt verder het eiland op, voorbij de wegversperring. Het verhaal is hiermee klaar "
         "en je krijgt het Guhstation en de muziekplaat van het eiland. Wordt vervolgd."),
        ("verhalen/snuffeldorp", "Naar huis en terug",
         "Kapitein Zoutsnoet in de haven vaart je naar huis wanneer je wilt, en met de geheugenkaart kan dat ook. Je verhaal blijft "
         "bewaard. Heb je het Guhstation al, dan ga je daarmee terug naar het eiland. Ben je het kwijt, dan heeft de kapitein er nog een. Had je het verhaal al uit "
         "voordat de muziekplaat bestond? Praat dan met de kapitein: je krijgt hem alsnog, één keer."),
        ("systemen/snuffeldorp_bewoners", "Wie is wie",
         "Jutje Kwispel is de strandjutter die je vindt; ze woont in de juttershut bij het strandpoortje. Dokter Pleisterpoot is de oude "
         "teckel met de doktersjas. Meester Truffelneus, de grote speurhond met de groene cape, geeft snuffelles in de wei. Kapitein "
         "Zoutsnoet, een mopshond met een botje als pijp, staat bij zijn boot. Bakker Kruimelsnuit staat bij zijn kraam op het plein, "
         "Visser Natneus zit op de steiger, Juf Blaffetje staat op het schoolplein, Oma Wolletje zit op haar veranda, Tuinder Knolletje "
         "staat in zijn moestuin en Kleine Kwijlebal rent rond bij de put."),
        ("systemen/snuffeldorp_bewoners", "Iedereen ziet zijn eigen verhaal",
         "De honden staan er voor iedereen, maar wat ze tegen je zeggen hangt af van hoe ver jij bent. Wat een dorpeling kwijt is, kan "
         "elke speler voor zichzelf terugvinden. Ook het boompje zie je in je eigen stap, en je maatje kan alleen jij zien."),
    ],
}
