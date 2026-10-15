"""
Super Guhrio (bbq2, slice guhrio-engine) - what the docs step (phase 3) needs to write the wiki pages of the castle and the
side-view game. Not in FEATURES: nothing here is built by the generators. CONTRACT_130 2.4 gives the shape of WIKI.
The levels themselves (guhrio_w1 .. guhrio_w3), Pad-guh, the shop and the rewards (guhrio_beloning) add their own files.
"""
WIKI = {
    "verhalen": {
        "super-guhrio": dict(
            nl="Super Guhrio", img="struct_guhrio_kasteel",
            lead_nl="De Grote Nether-Mika heeft Prinses Perzikguh meegenomen 'voor een stukje taart'. In zijn kasteel in de frituursauszee "
                    "speel je zes levels van opzij, en dan een duel. Niemand doet je pijn: wie valt of geduwd wordt, staat weer bij zijn "
                    "laatste vlaggetje. Njeg!",
            ftb=[("guhs_guhrio", "guhrio")], structure="guhrio_kasteel", npcs=[], related=["systemen/super-guhrio-spelen"]),
    },
    "systemen": {
        "super-guhrio-spelen": ("Super Guhrio spelen", "struct_guhrio_kasteel",
                                "Hoe een level van opzij werkt: de knoppen, de vlaggetjes, de Superknabbel en de Vuurpeper, de grote "
                                "vadsmunten, de pijpen en Guhshi.", ["verhalen/super-guhrio"]),
    },
    "npc_home": {},
    "entity_home": {"guhmba": "bouwwerken/guhrio_kasteel", "schild_mika": "bouwwerken/guhrio_kasteel", "plof_mika": "bouwwerken/guhrio_kasteel",
                    "hapbloem": "bouwwerken/guhrio_kasteel"},
    "tekst": [
        ("verhalen/super-guhrio", "Het kasteel",
         "Het Kasteel van de Grote Nether-Mika staat in de frituursauszee van de Guhbarbecuether (superkompas: Barbecue). Je komt aan op "
         "het voorplein, loopt door het poortgebouw en over de binnenplaats naar de levelhal. Daar hangen zeven poorten: zes levels en "
         "aan het eind van de rode loper de grote poort naar het duel. Een poort gaat open als het level ervoor gehaald is."),
        ("systemen/super-guhrio-spelen", "De knoppen",
         "In een level kijk je van opzij en zit je vast op de baan. A en D lopen naar links en rechts, spatie springt (ingedrukt houden "
         "is hoger, even tikken is een hupje), S duikt in een pijp, W gaat door een deur, sprinten is rennen. Een muisknop gooit een "
         "knabbel als je de Vuurpeper hebt, of laat de tong van Guhshi uitschieten. Twee keer Q stapt uit het level."),
        ("systemen/super-guhrio-spelen", "Wat je tegenkomt",
         "Guhmba's (spring erop: plat, njeg!), Schild-Mika's (spring erop en schop het schild weg: het veegt een hele rij Guhmba's om en "
         "zet schakelaars om), Plof-Mika's (vallen als je eronderdoor loopt), draaiende grillspiesen, Hapbloemen in pijpen (een natte "
         "zoen, en terug naar je vlaggetje). Niets doet pijn: een aanraking kost je Superknabbel of Vuurpeper, of zet je terug bij je "
         "laatste vlaggetje."),
        ("systemen/super-guhrio-spelen", "Munten, vadsmunten en records",
         "Gewone munten tellen één keer mee voor je buidel (opnieuw spelen levert geen extra munten op); bij Pad-guh koop je er dingen "
         "voor. In elk level liggen drie grote vadsmunten; de Guhdex (Verhalen > Super Guhrio) laat per level zien welke je hebt. Je "
         "snelste tijd per level en voor het hele kasteel in één keer (van 1-1 tot en met 3-2) staan bij de highscores, naast het record "
         "van de server."),
        ("systemen/super-guhrio-spelen", "Guhshi",
         "In de kelders (wereld 2) ligt het ei van Guhshi. Wie het gevonden heeft, mag in de burcht (wereld 3) op zijn rug: houd spatie "
         "ingedrukt om te fladderen over grote gaten, en hap met een muisknop Guhmba's en munten weg. Word je geraakt, dan rent Guhshi "
         "terug naar zijn plekje en sta jij nog gewoon."),
    ],
}
