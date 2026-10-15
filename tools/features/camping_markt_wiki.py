"""
bbq2 (camping-markt): what the wiki step needs to know about features/camping_markt.py (CONTRACT_130 2.4). Not in FEATURES:
the docs step of the merge reads WIKI. Items, blocks, structures and the three NPCs get their own page from the game data.
"""
from features import wereld

WIKI = {
    "verhalen": dict([
        wereld.wiki_questlijn(
            "camping", "Kamperen bij De Gloeiende Guh",
            "Op een grotbodem in de Guhbarbecuether ligt de Grillcamping: een dorpje van tenten rond een groot kampvuur, met een "
            "receptie, een houtschuur en vijf kampeerguhs. De Kampbaas-guh geeft je een tentzak. Je zet je eigen tent op een vrij "
            "plekje en slaat de vier haringen vast, hakt bij de Houthakker-guh zes blokken brandhout, steekt daarmee het kampvuur "
            "aan (de kampeerguhs komen erbij zitten en dansen) en roostert met de Roosterstok drie marshmallows goudbruin. Als dank "
            "krijg je de receptkaart van de Plantagebak en het kampeerpakje voor je guh.",
            "grillcamping", ["kampbaasguh", "houthakkerguh"], [("guhs_barbecuether", "camping_markt_camping")], related=["ruilmarkt"]),
        wereld.wiki_questlijn(
            "ruilmarkt", "De nepvads van de ruilmarkt",
            "Op de Nether-Mika-ruilmarkt staan gestreepte kraampjes rond de Waag. Iemand betaalt er met nepvads, en de "
            "Marktmeester-Mika zoekt hulp. Eerst leer je afdingen: lees wat hij zegt en kies het antwoord dat past. Daarna weeg je "
            "de vijf stapels vads in de Waag twee aan twee en zet je de Keurstempel op de stapel die lichter is. Als dank krijg je "
            "de weegschaal, en voortaan geeft elke Nether-Mika je een extraatje bij het ruilen.",
            "mika_ruilmarkt", ["marktmeester_mika"], [("guhs_barbecuether", "camping_markt_markt")], related=["camping"]),
    ]),
    "systemen": {
        "tentdoek": ("Tentdoek", "campingmarkt_tentdoek_geel_trap",
                     "Tentdoek is een bouwblok in vijf kleuren (rood, geel, groen, blauw en crème), als blok, als schuin stuk en als "
                     "plaat. De tenten van de Grillcamping en de luifels van de ruilmarkt zijn ervan gemaakt.", ["camping", "ruilmarkt"]),
    },
    "npc_home": {"kampbaasguh": "bouwwerken/grillcamping", "houthakkerguh": "bouwwerken/grillcamping",
                 "marktmeester_mika": "bouwwerken/mika_ruilmarkt"},
    "entity_home": {"campingmarkt_kraam_mika": "bouwwerken/mika_ruilmarkt"},
    "tekst": [
        ("camping", "Je eigen tent",
         "Bij de poort van de camping liggen vier kampeerplekjes met een bordje VRIJ. Klik er met de tentzak op en je tent staat. Sla "
         "daarna de vier haringen vast door op de ijzeren pennen te klikken. Na een paar minuten gaat elke tent vanzelf weer de zak "
         "in, zodat het plekje vrij is voor de volgende speler. Wat jij al gedaan hebt, blijft gewoon geteld."),
        ("camping", "Marshmallows roosteren",
         "Roosteren is een spelletje van op tijd loslaten. Houd met de Roosterstok rechtsklik ingedrukt op een brandend kampvuur: de "
         "marshmallow gaat van koud naar warm naar goudbruin naar zwart. Laat los als er GOUDBRUIN staat (je hoort ook een belletje). "
         "Te vroeg of te laat kost niks: je probeert het gewoon opnieuw. De stok werkt boven elk brandend kampvuur, ook thuis en ook "
         "overdag, en je hebt er marshmallowknabbels voor nodig."),
        ("camping", "Het kampvuur",
         "Het grote kampvuur brandt twee minuten nadat er hout op is gegooid en gaat dan vanzelf uit. Wie het kampvuurfeest al heeft "
         "gevierd, pookt het met een klik weer op. Thuis is het een sierblok: aansteken met een vuursteen, doven met een schep."),
        ("camping", "De kampeerguhs",
         "De vijf guhs van de camping horen bij de camping. Ze praten graag, maar je kunt ze niet temmen of meenemen. Brandt het "
         "kampvuur, dan komen ze erbij zitten."),
        ("ruilmarkt", "Afdingen",
         "De Marktmeester-Mika begint bij 30 knabbels en moet naar 10. Elke ronde doet hij één van drie dingen. Schept hij op over "
         "zijn markt of zijn hoed, geef dan een compliment. Zucht hij dat het zo stil is, bied dan laag. Gromt hij dat dit zijn "
         "laatste bod is, doe dan alsof je wegloopt. Een goed antwoord haalt 7 knabbels van de prijs, een fout antwoord kost hem "
         "geduld. Na drie fouten stuurt hij je weg met een duwtje en begin je opnieuw. Het kost je nooit echte knabbels."),
        ("ruilmarkt", "De nepvads vinden",
         "Van de vijf stapels vads in de Waag is er één nep, en nepvads is lichter. Klik op een stapel om hem op de linkerschaal te "
         "leggen en op een tweede voor de rechterschaal: de weegschaal zakt naar de zware kant. Blijft hij recht, dan zijn beide "
         "stapels echt. Klik met de Keurstempel op de stapel die nep is. Welke stapel nep is, verschilt per speler. Stempel je "
         "verkeerd, dan wisselt de oplichter de stapels om en moet je opnieuw wegen."),
        ("ruilmarkt", "Ruilen op de markt",
         "De drie kraam-Mika's ruilen meteen: geef een staaf vahoege vads en je krijgt een verrassing uit dezelfde zak als bij de "
         "wilde Nether-Mika's. Ze vallen je niet aan, ook niet zonder vadsuitrusting. Wie de questlijn af heeft, krijgt bij elke "
         "ruil een tweede cadeautje, op de markt en bij wilde Nether-Mika's. Elke dag kun je bij de Marktmeester afdingen om het "
         "koopje van de dag."),
        ("tentdoek", "Maken en bouwen",
         "Drie blokken wol en een draad geven vier blokken tentdoek in de kleur van de wol (witte wol geeft crème, lichtblauwe wol "
         "blauw). Van tentdoek maak je schuine stukken en platen, net als van steen. Zet de schuine stukken tegen elkaar voor een "
         "puntdak en leg een plaat op de nok. De Kampbaas-guh en de Marktmeester-Mika verkopen het ook."),
    ],
}
