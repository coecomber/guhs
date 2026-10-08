"""
Wiki texts of Het Snuffeleiland, the kern: how being a dog works (the dog form, sniffing, ranks, the companion, the tree,
the Guhstation, going home). Not a feature module (not in FEATURES): the docs step of the merge reads WIKI and turns it
into pages (CONTRACT_130 2.4). The story itself (the dock, the village, its residents) is told by the dock and village
slices in their own wiki modules; the block and the items get their pages from the game data.
"""

WIKI = {
    "verhalen": {
        "snuffeleiland": dict(
            nl="Het Snuffeleiland", img="snuffel_eiland",
            lead_nl="Je kleine broertje of zusje is ziek en alleen een geneesbloem kan helpen. Papa is die al weken aan het zoeken. "
                    "Jij vaart hem achterna, spoelt aan op een eiland... en wordt wakker als hond. Daar leer je snuffelen, njeg.",
            ftb=[("guhs_verhalen", "snuffel")], structure="steigerhuisje", npcs=[], related=["systemen/snuffelen", "systemen/guhstation"]),
    },
    "systemen": {
        "snuffelen": ("Een hond zijn op het Snuffeleiland", "snuffel_hond",
                      "Op het Snuffeleiland ben je een hond. Je loopt op pootjes, je ziet de wereld van laag bij de grond en je neus "
                      "kan dingen die je ogen niet kunnen.",
                      ["verhalen/snuffeleiland", "systemen/guhstation"]),
        "guhstation": ("Het Guhstation", "block_guhstation",
                       "Een grijszwart spelkastje met een guh-snoet erop. Je krijgt het als je de eerste reeks van het Snuffeleiland "
                       "hebt uitgespeeld, samen met de muziekplaat van het eiland. Daarmee ga je terug naar het eiland wanneer je wilt.",
                       ["verhalen/snuffeleiland", "systemen/snuffelen"]),
    },
    "tekst": [
        ("systemen/snuffelen", "Je eigen hond",
         "Bij de steiger kies je wie je bent: een shiba, jack russell, teckel, corgi, golden retriever of mopshond, elk in drie kleuren, "
         "en je geeft je hond een naam. Andere spelers zien jouw hond lopen, met jouw naam erboven. Een grote hond kijkt van hoger dan "
         "een teckel."),
        ("systemen/snuffelen", "Je maatje",
         "Bij je hond kies je ook een maatje: het Zweefzaadje, het Mos-eikeltje of het Zonnepluisje. Het is een ondeugend bosgeestje "
         "dat later in het verhaal opduikt. Alleen jij kunt je eigen maatje zien. Als je snuffelt en iets ruikt, zweeft het een "
         "stukje vooruit en wijst het de kant op."),
        ("systemen/snuffelen", "Je spullen zijn veilig",
         "Een hond heeft geen zakken. Zodra je op het eiland bent, wachten al je spullen veilig op je, precies zoals je ze had. Ga je "
         "naar huis, dan heb je alles weer terug op dezelfde plek. Dat geldt ook als je uitlogt of als er iets misgaat. Wat je op het "
         "eiland krijgt, reist met je mee naar huis. Op het eiland kun je niet bouwen of breken, en niets kan je pijn doen."),
        ("systemen/snuffelen", "Snuffelen en graven",
         "Houd de snuffeltoets ingedrukt (standaard R). Je hond loopt met zijn neus over de grond en boven je balk verschijnt de "
         "geurmeter. Hoe dichter je bij een geur komt, hoe verder de meter uitslaat en hoe harder hij zwiept. De kleur zegt wat je "
         "ruikt: oranje is iets lekkers, blauw een ding, groen een dier en paars iets vreemds. Er zijn geen sporen op de grond: je "
         "volgt alleen je neus. Sta je op de plek, dan graaf je met de linkermuisknop. Elke geur die je vindt, komt in je "
         "snuffelboekje (standaard N)."),
        ("systemen/snuffelen", "Wat een hond nog meer kan",
         "Zitten (Z), kwispelen (V) en blaffen (B). Njeg! De toetsen kun je veranderen bij de besturing, onder Guhs."),
        ("systemen/snuffelen", "Snuffelrangen",
         "Hoe meer geuren je kent, hoe hoger je rang. Er zijn er vijf: Snuffelpup (vanaf het begin), Snuffelneus (20 geuren), "
         "Snuffelspeurder (50), Snuffelmeester (100) en Opper-Snuffelmeester (150). Je rang bepaalt wat je neus kan ruiken. In dit "
         "eerste deel van het verhaal word je Snuffelpup, rang 1 van 5. De andere rangen komen in een later verhaal. Je rang staat "
         "in de Guhdex onder het verhaal, in je snuffelboekje en op het Guhstation."),
        ("systemen/snuffelen", "Het boompje",
         "Bij het dorp ligt een krans van steentjes. Daar groeit het boompje van je maatje: van elke goede daad voor een dorpeling "
         "groeit het een stap, van kiem naar scheutje, struikje en jong boompje. Iedereen ziet zijn eigen boompje. Aan het eind "
         "krijg je er een bloesemtakje van."),
        ("systemen/guhstation", "Zo werkt het",
         "Zet het Guhstation neer en klik erop. Er opent een klein schermpje: op een wit veld komen jouw hond en je broertje of "
         "zusje aanrennen en spelen. Klik op 'Druk op start' en je staat weer op het eiland, op de plek waar je de vorige keer "
         "was. Geen zin? Klik op 'Nee ik wil even niet snuffelen, njeg'. Heb je nog geen Guhstation, dan vaart de kapitein bij een "
         "steigerhuisje je naar het eiland."),
        ("systemen/guhstation", "De muziekplaat van het eiland",
         "Aan het eind van de eerste reeks krijg je ook de muziekplaat 'Het Snuffeleiland' (Guh - Pootjes in het zand), een cadeautje "
         "van Kapitein Zoutsnoet. Had je het verhaal al uit? Praat dan met de kapitein in de haven van het eiland. Iedere speler "
         "krijgt hem één keer. In een jukebox hoor je het deuntje op gewone sterkte; de Disco-dynamo telt hem als zeldzame plaat. "
         "Zolang het schermpje van het Guhstation open is, hoor je hetzelfde deuntje heel zachtjes (de schuif 'Jukebox/nootblokken' "
         "bij de geluidsinstellingen regelt het mee). Verder is het op het eiland stil: daar hoor je alleen de zee en je eigen neus."),
        ("systemen/guhstation", "Je eigen deuntje",
         "Het deuntje is één geluid met één bestand, dus je kunt het op je eigen computer vervangen met een resourcepack. Zet je eigen "
         "muziek als OGG Vorbis in het pack op assets/guhs/sounds/music_disc_snuffeleiland.ogg en zet het pack aan: de plaat en het "
         "Guhstation spelen dan jouw bestand (het geluid heet guhs:music_disc.snuffeleiland). Er verandert niets aan de mod zelf, en "
         "andere spelers horen gewoon het eigen deuntje van de mod."),
        ("systemen/guhstation", "Weer naar huis",
         "Op het eiland heb je een geheugenkaart in je balk. Gebruik hem en kies 'Opslaan en naar huis': je staat precies waar je "
         "was voordat je vertrok, met al je eigen spullen. Kapitein Zoutsnoet in de haven van het eiland vaart je ook terug."),
    ],
}
