"""
bbq2 (guhrio-w3) - what the docs step (phase 3) needs for the wiki pages of world 3 of Super Guhrio and the duel. Not in
FEATURES: nothing here is built by the generators. CONTRACT_130 2.4 gives the shape of WIKI. The castle's own page and the
page "Super Guhrio spelen" are the engine's (features/guhrio_wiki.py): this file only adds paragraphs to them, and a page
for the duel.
"""
WIKI = {
    "verhalen": {
        "super-guhrio-duel": dict(
            nl="Het duel met de Grote Nether-Mika", img="struct_guhrio_kasteel",
            lead_nl="Achter de grote poort van de levelhal staat de Grote Nether-Mika op zijn roosterbrug: hoorns, rode manen, een "
                    "schild vol stekels. Drie rondes, en dan blijkt waar het allemaal om ging: een stukje taart. Hij duwt alleen "
                    "maar, njeg.",
            ftb=[("guhs_guhrio", "guhrio_w3")], structure="guhrio_kasteel", npcs=["perzikguh"], related=["verhalen/super-guhrio",
                                                                                                          "systemen/super-guhrio-spelen"]),
    },
    "systemen": {},
    "npc_home": {},
    "entity_home": {"grote_nether_mika": "bouwwerken/guhrio_kasteel"},
    "tekst": [
        ("verhalen/super-guhrio", "Wereld 3: de burcht",
         "De bovenste verdieping van het kasteel: donker metselwerk boven de frituursaus. In level 3-1, De Grillgang, wacht Guhshi op "
         "wie zijn ei gevonden heeft. Je springt over de saus van naaf naar naaf van de draaiende grillspiesen, lokt Plof-Mika's naar "
         "beneden, en doet daarna een stuk te voet: Guhshi blijft bij de parkeerpaal (van vuurpepers moet hij niezen), de "
         "Vuurpeperstruik geeft je de Vuurpeper zo vaak je wilt, en een knabbel door de spleet zet de schakelaar om die de rode muur "
         "opent. Bovenop dat blok staat een pijp met een Hapbloem: die leidt naar de schatkamer."),
        ("verhalen/super-guhrio", "Level 3-2: De Sauskelder",
         "Het level van Guhshi. Twee gaten zijn zo breed dat alleen zijn fladdersprong eroverheen komt; wie te voet is, slaat tegen de "
         "klokschakelaar en rent over de brug die er dan acht tellen ligt. Daartussen valblokken onder snelle spiesen, een platform "
         "dat onder twee Plof-Mika's door glijdt, en achter een deur de Peperkamer, waar een knabbel door een spleet de kooi met de "
         "derde vadsmunt opent. Elk level van de burcht is ook zonder Guhshi te halen."),
        ("verhalen/super-guhrio-duel", "Ronde 1: de hendel",
         "De Grote Nether-Mika loopt over zijn brug, gooit trage gloeiende kooltjes (spring eroverheen) en springt. Als hij landt, "
         "duwt de dreun iedereen weg die op de grond staat: spring op het goede moment. Ren onder hem door terwijl hij in de lucht "
         "hangt en loop tegen de hendel op de overkant: de verste helft van de brug ploft in de saus. Hij springt net op tijd naar de "
         "helft die blijft staan. De groene pijp brengt je terug naar zijn kant."),
        ("verhalen/super-guhrio-duel", "Ronde 2 en 3: het schild",
         "Op de korte brug kruipt hij in zijn schild en rolt heen en weer tussen de muur en het afgebroken eind. Spring eroverheen. "
         "Na drie bonken tegen de muur zit het ?-blok bij de start weer vol: de Vuurpeper. Gooi een knabbel tegen het schild terwijl "
         "het op je af rolt en het stuitert terug. De derde keer kan hij niet meer remmen. Raak je de Vuurpeper kwijt, dan vult het "
         "?-blok zich opnieuw."),
        ("verhalen/super-guhrio-duel", "Hoe het afloopt",
         "Hij plonst in de saus, klimt er aan de overkant weer uit (nat, plakkerig, beledigd, verder niks aan de hand) en gaat zitten "
         "mokken. De brug komt terug, en dan komt Prinses Perzikguh uit haar deur, met de taart. Iedereen die in de arena staat, "
         "heeft gewonnen; wie daarna binnenloopt, vindt de Grote Nether-Mika weer klaar voor een nieuw gevecht. De Guhdex "
         "(Verhalen > Super Guhrio) speelt het filmpje nog eens af."),
    ],
}
