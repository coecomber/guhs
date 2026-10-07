"""
einde2 (vesting): what the wiki must say about the modular Mika-vesting.
Not a feature module (not in FEATURES): the docs step reads WIKI and turns it into pages (build contract 2.4). The page of
the structure itself comes from the game data.
"""
WIKI = {
    "systemen": {
        "mika_vesting_stukken": (
            "De Mika-vesting van binnen", "block:guhs:mika_steen_pilaar",
            "De Mika-vestingen van de buiteneilanden zijn geen losse toren meer. Elke nieuwe vesting is in elkaar gezet uit "
            "stukken, dus geen twee zijn hetzelfde. Wat er altijd is: de burcht, het cellenblok en een vetschip aan de steiger.",
            ["mika_vesting", "guhvleugels", "mika_steen", "schuilguh", "guheinde_terugpoort"]),
    },
    "entity_home": {"schuilguh": "bouwwerken/mika_vesting"},
    "tekst": [
        ("mika_vesting_stukken", "De burcht",
         "In het midden staat de burcht, vier verdiepingen hoog, met een wenteltrap in de hoek die van onder tot boven doorloopt. "
         "Beneden is het wachtlokaal met het bureau van de vesting-kapitein. Daarboven de keuken, waar het naar knabbelsoep "
         "(zonder knabbel) ruikt. Op de tweede verdieping de slaapzaal of het knabbeldepot. Bovenin de troonzaal, met achter "
         "tralies de schatkamer. En op het dak wappert de vlag met Mika's gezicht."),
        ("mika_vesting_stukken", "Het cellenblok",
         "Achter de burcht, door de achterdeur van het wachtlokaal, staat het cellenblok: zes cellen met in elk een magere guh. "
         "Geef zo'n guh een kaasknabbel en er springt een kleurige guh uit die vrolijk wegrent. De grijze guh blijft zitten voor "
         "de volgende speler: iedereen op de server kan elke guh een keer bevrijden. In een kerkertoren zitten er nog twee. "
         "Een lege cel krijgt na een tijdje vanzelf weer een bewoner."),
        ("mika_vesting_stukken", "Bruggen en zijtorens",
         "Vanuit de keuken en vanaf de tweede verdieping lopen bruggen naar zijtorens: korte, lange en overdekte. De zijtorens "
         "staan niet op de grond maar zweven op een puntige voet. Er zijn wachttorens, kerkertorens en ronde uitkijktorens met "
         "een spits. Een deur waar geen brug meer bij paste komt uit op een balkon; een brug zonder toren eindigt bij een bordje "
         "met een belofte."),
        ("mika_vesting_stukken", "Het vetschip",
         "Aan de steiger ligt altijd een vetschip, een van de drie: het grote met twee masten, de sloep, of de vetblaas, een "
         "romp die onder een reusachtige bel vet hangt. In de kajuit hangen de Guhvleugels. Die blijven hangen: iedere speler "
         "krijgt er zijn eigen paar van, een keer per schip."),
        ("mika_vesting_stukken", "Schuilguhs",
         "Aan de muren, op de bruggen en op de daken zitten Schuilguhs in hun kaasdoos. Die horen niet bij de Mika's; ze "
         "schuilen er gewoon. Een kusje en je zweeft even."),
        ("mika_vesting_stukken", "Oude en nieuwe vestingen",
         "Een vesting die er al stond blijft de toren die hij was. Alleen in stukken van de buiteneilanden waar nog niemand "
         "geweest is staan de nieuwe. In een nieuwe vesting kun je niets afbreken of bijbouwen; deuren, kisten en tonnen "
         "werken gewoon."),
    ],
}
