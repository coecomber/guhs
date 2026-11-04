"""
1.9.0 "Een opknapper!" (De Erfenis): what the wiki says about the update (design: MineScapeStory/ontwerp-guhs190-erfenis.md).
Not a feature module (not in FEATURES): the docs step (tools/wiki_bbq2) reads WIKI; the English is in tools/wiki_bbq2/en_guhs190.py.
Spoiler-free like the Discord post: the Sherlock page tells how the story starts, never who, what or how it ends.
"""
WIKI = {
    "verhalen": {},
    "systemen": {
        "een-opknapper": (
            "Een opknapper!", "item:guhs:makelaarsbrief",
            "Makelaar Guhrrit heeft een buitenkansje: een overwoekerd boerderijtje met een eigen stukje grond. Bouwkundig rapport: "
            "vads. Vraagprijs: één kaasknabbel. Ruim het op en bouw er je eigen knusse guhdorpje van.",
            ["je-dorp", "buren-en-bestellingen", "leverkist", "sherlock-guh", "nieuw-in-1-9"]),
        "je-dorp": (
            "Je dorp", "block:guhs:erfhekje",
            "Achter je Erfhekje ligt je eigen dorp: 33 gebouwen die tot level 10 groeien, vijf paletten, paadjes, hekjes en deco. "
            "Druk op F9 voor het bovenaanzicht en bouw alles van bovenaf.",
            ["een-opknapper", "buren-en-bestellingen", "leverkist"]),
        "buren-en-bestellingen": (
            "Buren en bestellingen", "item:guhs:guhldenzakje",
            "Druk op F10 voor de wereldkaart met de dorpjes van andere spelers en een heleboel buurdorpjes. De buren plaatsen "
            "bestellingen, jij levert met je karretjes en krijgt er Guhlden voor.",
            ["je-dorp", "een-opknapper"]),
        "leverkist": (
            "De Leverkist", "block:guhs:leverkist",
            "Wat je dorp maakt, stuur je via de Leverkist naar je basis: wol, stammen, klei, zand, glasscherven en zelfs "
            "kaasknabbels. Niet te veel tegelijk: de bezorg-guh heeft ook maar twee pootjes.",
            ["je-dorp", "een-opknapper"]),
        "sherlock-guh": (
            "Sherlock Guh", "item:guhs:sherlock_vergrootglas",
            "Waarom kostte die grond nou maar één knabbel? Bouw een Detectivebureau, en een bekende speurneus neemt samen met de "
            "trouwe Dokter Vadson de zaak aan. Jij bent de detective.",
            ["een-opknapper", "je-dorp"]),
        "nieuw-in-1-9": (
            "Nieuw in 1.9: Een opknapper!", "item:guhs:makelaarsbrief",
            "Wat er nieuw is in Guhs 1.9: de brief van de makelaar, je eigen dorp in een eigen dimensie, buren en bestellingen, "
            "de Leverkist en een speurneus die vindt dat er iets niet klopt. Er is niks gereset. De volledige lijst staat in het "
            "CHANGELOG van de mod.",
            ["een-opknapper", "je-dorp", "buren-en-bestellingen", "leverkist", "sherlock-guh"]),
    },
    "tekst": [
        ("systemen/een-opknapper", "Je hebt post",
         "Ben je een half uurtje in de Guhmension geweest (speeltijd, vanaf je eerste bezoek), dan komt de Postguh met een dikke "
         "envelop. Lees de brief en koop de opknapper voor één kaasknabbel: je krijgt een Erfhekje en een Leverkist."),
        ("systemen/een-opknapper", "Naar je erf",
         "Zet het Erfhekje in je basis en loop erdoor (of rechtsklik erop). Je staat meteen op je eigen erf, in een rustige, "
         "zonnige plek zonder weer en zonder monsters. Ga je terug, dan kom je uit voor het hekje dat je gebruikte. Makelaar "
         "Guhrrit leidt je rond tot je Hoeve level 2 is en stuurt je daarna ansichtkaartjes van vakantie."),
        ("systemen/een-opknapper", "Het FTB-hoofdstuk",
         "In het FTB-boek staat het hoofdstuk \"Een opknapper!\" (na Guhdromenland). De quests over je dorp vinken zichzelf af "
         "zodra het in je dorp zo ver is. Niks staat op slot."),
        ("systemen/je-dorp", "Gebouwen en levels",
         "Er zijn 33 gebouwen: de Hoeve, kippenhokken, schaapskooien, de eendenvijver, de molen, de bakkerij, de kaasmakerij, "
         "de weverij, de houtzagerij, de glasblazerij, het dorpscafé, de muziektent, het Guhpension en nog veel meer. De meeste "
         "groeien tot level 10 (de Mammoetwei tot 3), maar nooit hoger dan je Hoeve. De bouwers in de Timmerschuur doen het "
         "werk: eerst één, later twee en drie."),
        ("systemen/je-dorp", "Productie",
         "Je dorp werkt in echte tijd door, ook als je er niet bent. Bronnen maken grondstoffen, werkplaatsen maken daar "
         "halffabricaten en spullen van. Bewoners trekken in de Guhhutjes en werken in je gebouwen. Een vol mandje stopt een "
         "gebouw tot je het leeghaalt."),
        ("systemen/je-dorp", "Paletten",
         "Elk gebouw kan in vijf paletten: hout & stro, baksteen & roze daken, vakwerk & knuffeldetails, vads & goud, en "
         "pastel-kristal & lichtjes. Verven is gratis, per gebouw of het hele dorp in één keer; een hoger palet komt vrij met "
         "een hogere Hoeve."),
        ("systemen/je-dorp", "Het bovenaanzicht",
         "Druk op F9 en je kijkt van bovenaf op je hele dorp: gebouwen neerzetten, verplaatsen, draaien, verbeteren en verven, "
         "met een timer boven elke klus. Je kunt er ook gewoon zelf rondlopen tussen je dorpsguhs."),
        ("systemen/je-dorp", "Deco, paden en Knusheid",
         "In de decowinkel koop je paadjes, hekjes, bloemen, bankjes, lichtjes, fonteinen, beelden en boerderijspul. Paden en "
         "hekjes trek je tegel voor tegel; bochten en kruisingen gaan vanzelf. Wat je opbergt gaat naar het decoschuurtje. Hoe "
         "knusser je dorp, hoe meer Knusheid, met mijlpalen en drie titels."),
        ("systemen/buren-en-bestellingen", "Bestellingen",
         "Open de bestellingen bij het Marktplein of via een buurdorpje op de kaart. Lever de gevraagde goederen met je "
         "karretjes en verdien Guhlden; soms sturen de buren een bedankje voor je dorp mee."),
        ("systemen/buren-en-bestellingen", "Handje helpen",
         "Zie je het dorp van een vriend op de kaart, kies dan Handje helpen: de klus die het eerst klaar is gaat sneller. Je "
         "vriend hoort meteen (of bij het inloggen) wie er hielp."),
        ("systemen/leverkist", "Naar huis sturen",
         "Kies in je dorp wat er naar huis mag. Het komt vanzelf in je Leverkist zodra die geladen is, ook als je er niet bent; "
         "het vlaggetje gaat omhoog als er post in zit. Trechters en buizen kunnen de kist leeghalen. Heb je er meer, dan vullen "
         "ze zich op volgorde."),
        ("systemen/leverkist", "Halffabricaten en plafonds",
         "Bouwgrondstoffen komen als halffabricaat: stammen, klei, zand en glasscherven (smelt ze thuis tot glas). Wol en de "
         "Bouwkeet-blokken komen kant-en-klaar. Per materiaal mag er een plafond per uur mee, dat tot 8 uur spaart; wat niet "
         "past wacht netjes op de volgende keer."),
        ("systemen/leverkist", "De Dorpszender",
         "Een Vadszender met een dorp-upgrade zendt vadskracht van je net naar de Vadsmast in je dorp. Daarop draaien de "
         "hoogste werkplaatsen en de Mammoetwei. Een kwart waait onderweg weg."),
        ("systemen/sherlock-guh", "Hoe het begint",
         "Bij het opruimen van je erf vind je misschien al iets vreemds. Bouw een Detectivebureau: boven het bureau verschijnt "
         "een vergrootglas als er een zaak klaarligt. Daar wacht het Speurboekje met de zaken. Zoek aanwijzingen in je dorp en "
         "los ze zelf op. Opgeloste zaken geven echte beloningen voor je dorp. Elementair, mijn beste vads."),
    ],
}
