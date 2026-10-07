"""
Wiki texts of Het Snuffeleiland, the dock: the steigerhuisje, who lives there and the opening of the story. Not a feature
module (not in FEATURES): the docs step of the merge reads WIKI and turns it into pages (CONTRACT_130 2.4). The story's own
page ("verhalen/snuffeleiland") is the kern's (snuffel_wiki.py): this module adds paragraphs to it. The structure gets its
page from the game data by itself.
"""

WIKI = {
    "verhalen": {},
    "systemen": {},
    "entity_home": {"steiger_bewoner": "bouwwerken/steigerhuisje", "steiger_boot": "bouwwerken/steigerhuisje"},
    "tekst": [
        ("bouwwerken/steigerhuisje", "Waar vind je het?",
         "Een steigerhuisje staat altijd aan het water van een Diepe Guhzee in de Guhmensie: een wit huisje met een rood pannendak op "
         "een stenen kade, met een lange steiger de zee in. Je superkompas wijst de weg: kies het steigerhuisje bij de Verhalen, of "
         "kies 'Mijn verhaal'. Steigerhuisjes staan alleen in stukken wereld die na deze update zijn ontdekt. Op een wereld die al "
         "lang bestaat, moet je er dus een flink eind voor reizen."),
        ("bouwwerken/steigerhuisje", "Wie wonen er?",
         "In het huisje ligt Kleine Wiebel ziek in bed, je kleine broertje of zusje. Buurvrouw Mandje past op. Aan het eind van de "
         "steiger staat Kapitein Zoutsnoet bij zijn boot, De Natte Neus. In de tuin hangen de lampions van het lantaarnfeest. Het "
         "bed van papa is leeg: zijn gestreepte sjaal hangt er nog boven."),
        ("verhalen/snuffeleiland", "Zo begint het",
         "Zodra je voor het eerst op een steiger stapt, begint het verhaal: het is de avond van het lantaarnfeest. Kleine Wiebel sluipt "
         "uit bed om te kijken en zakt op de steiger in elkaar. Iedereen op de server beleeft dit op zijn eigen moment: wie later "
         "komt, ziet het feest gewoon ook."),
        ("verhalen/snuffeleiland", "Het ziekbed",
         "Ga daarna het huisje in en praat met Buurvrouw Mandje (of met Wiebel). Wiebel heeft de snuffelkoorts. Daar helpt alleen de "
         "geneesbloem van het Snuffeleiland tegen, en papa is die al weken aan het zoeken. Jij besluit hem achterna te gaan."),
        ("verhalen/snuffeleiland", "Je hond en je maatje kiezen",
         "Kapitein Zoutsnoet vraagt welke hond jij bent. Op het eiland lopen namelijk alleen honden rond: in de Guhmensie zie je "
         "eruit zoals altijd, op het eiland ben je de hond die je hier kiest. Je kiest een ras, een vacht en een naam, en het "
         "maatje dat met je meegaat. Je mag bij de kapitein zo vaak opnieuw kiezen als je wilt. Kleine Wiebel is altijd een puppy "
         "van jouw ras en jouw kleur."),
        ("verhalen/snuffeleiland", "De overtocht",
         "Dan vaar je uit met De Natte Neus. Onderweg betrekt de lucht en worden de golven steeds hoger. De kapitein wil omkeren, "
         "maar jij springt overboord en zwemt door. Dan wordt alles zwart... en je wordt wakker op het strand van het "
         "Snuffeleiland. Je eigen spullen blijven veilig achter en zijn er weer als je thuiskomt."),
        ("verhalen/snuffeleiland", "Heen en weer",
         "Zolang je nog geen Guhstation hebt, is de steiger je weg naar het eiland: de kapitein vaart je er zo vaak naartoe als je "
         "wilt. Ga je vanaf het eiland naar huis, dan kom je precies uit waar je vertrok. Was dat de steiger, dan zie je De Natte "
         "Neus weer aanleggen."),
    ],
}
