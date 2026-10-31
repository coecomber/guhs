"""
1.8.1 "De Maagwand": what the wiki says about the update (design: MineScapeStory/ontwerp-guhs181-maagwand.md). Not a feature
module (not in FEATURES): the docs step (tools/wiki_bbq2) reads WIKI; the English is in tools/wiki_bbq2/en_guhs181.py.
"""
WIKI = {
    "verhalen": {},
    "systemen": {
        "maagwand": (
            "De maagwand", "item:guhs:gefrituurde_kaasknabbels",
            "Goed guh-eten zit achter je maagwand. Is vads te vadsig, dan volgt een BOERP en houd je het item. Eet trainvoer om "
            "van Melkmaagje naar Onverwoestbare Vadsmaag te groeien: elke laag opent nieuw eten en geeft een klein bonusje.",
            ["frituurlijn", "nieuw-in-1-8-1"]),
        "frituurlijn": (
            "De frituurlijn en de Guhsnackbar", "block:guhs:frying_pan",
            "De frituurpan kan meer: dubbel en driedubbel gefrituurd, patat en snacks. Recepten leer je bij de Snackbar-guh in "
            "de zeldzame Guhsnackbar De Patatzak, te vinden met het Snackbarkompas. Daarna frituur je ze thuis zelf.",
            ["maagwand", "nieuw-in-1-8-1"]),
        "nieuw-in-1-8-1": (
            "Nieuw in 1.8.1: De Maagwand", "item:guhs:gefrituurde_kaasknabbels",
            "Wat er nieuw is in Guhs 1.8.1: de maagwand, de frituurlijn met snackbarvads, de Guhsnackbar en een veel grotere "
            "Guhkermis. De volledige lijst staat in het CHANGELOG van de mod.",
            ["maagwand", "frituurlijn", "bouwwerken/guh_kermis"]),
    },
    "tekst": [
        ("systemen/maagwand", "De lagen",
         "Melkmaagje is de start. Daarna eet je 100 keer Kaas Knabbels, sate of kaasbrok voor de Knabbelmaag, 64 keer gefrituurd, "
         "gebakken guh-vis of kaasbolletje voor de Frituurmaag, 48 keer dubbel gefrituurd, gegrilde sate of patat voor de Saustaaie "
         "Maag en 32 keer driedubbel gefrituurd, kaaskroket of Frikandel Guhspeciaal voor de Onverwoestbare Vadsmaag."),
        ("systemen/maagwand", "De Guhtong",
         "Links in je inventaris hangt de Guhtong met zwemmende guhlichaampjes. Hover erover voor je laag, je teller, je "
         "trainvoer en alle items die nog op slot zitten."),
        ("systemen/frituurlijn", "Snacks",
         "Vrij te maken in de pan: dubbel en driedubbel gefrituurde kaasknabbels en Patatje Kaassaus. Te leren: kaaskroket, "
         "Frikandel Guhspeciaal, bitterguhllen, kaassoufflguh, Patatje Vads, Patatje Oorlog, Patatje Guhspeciaal en De Vetste Hap."),
        ("systemen/nieuw-in-1-8-1", "De Grote Guhkermis",
         "Elke Guhkermis groeit vanzelf uit met eetkraampjes, spelkraampjes, de Zweefmolen, Botsguhs, het Reuzenrad, het "
         "Spookguhhuis, de Guhtwister en de Knabbelbooster. Rijden is gratis en levert kermisbonnen op."),
    ],
}
