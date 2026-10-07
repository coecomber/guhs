"""
einde2 (bouw_schelpenstad): what the wiki must say about the road out of Bikinibodem, De Ruige Kroeg, the loopgraaf and
Schelpenstad. Not a feature module (not in FEATURES): the docs step reads WIKI and turns it into pages (build contract 2.4).
The pages of the structure and of the eight decorative blocks come from the game data by themselves.

Phase A has no story: the text below only says what STANDS there. What happens there is the Sponsguh story's (phase B): that
module adds its own "verhalen" entry later.
"""
WIKI = {
    "systemen": {
        "weg-naar-schelpenstad": (
            "De weg naar Schelpenstad", "block:guhs:einde2_bouw_schelpenstad_schelpsteen",
            "Uit de stadspoort van Bikinibodem loopt een weg de leegte in. Wie hem helemaal uitloopt komt langs De Ruige Kroeg, "
            "moet dwars door de loopgraaf en staat dan in Schelpenstad, een badplaatsje aan de Leegtezee.",
            ["einde2_bouw_schelpenstad_schelpsteen", "einde2_bouw_schelpenstad_souvenir", "einde2_bouw_schelpenstad_kaartenrek",
             "einde2_bouw_schelpenstad_kroonkussen", "einde2_bouw_schelpenstad_drooglamp", "einde2_bouw_schelpenstad_stopcontact",
             "einde2_bouw_schelpenstad_sproeier", "einde2_bouw_schelpenstad_neon"]),
    },
    "entity_home": {},
    "tekst": [
        ("weg-naar-schelpenstad", "De weg",
         "De weg begint bij de poort in de zeepbelkoepel van Bikinibodem en is negen blokken breed, met een stoeprand en om de paar "
         "stappen een lampion in de berm. Schelpenstad staat niet in het Superkompas: de weg wijst zelf de weg. Het hele stuk, van "
         "de kroeg tot de pier, is beschermd: je kunt er niets afbreken of bijbouwen."),
        ("weg-naar-schelpenstad", "De Ruige Kroeg",
         "Het eerste wat je tegenkomt is een roestige sleepboot die op het droge ligt. In de romp zit de kroeg: een podium in de "
         "achtersteven, een tap langs de kant, tafeltjes, en in de boeg een wc met een pot zeepsop. Bellen blazen is er streng "
         "verboden. Via een trap kom je op het dek, bij het kantoortje van de barman en de stuurhut. De schoorsteen rookt nog. "
         "Voor de deur is een parkeerplaats voor ruige karretjes."),
        ("weg-naar-schelpenstad", "De loopgraaf",
         "Voorbij de kroeg klimt de weg de rotsen in en houdt dan zomaar op, aan de rand van de loopgraaf: een kloof van achttien "
         "blokken diep, dwars door de hele plaat. Er is geen brug. Langs de westwand kronkelt een pad omlaag, over de bodem loopt "
         "een spoor van platgelopen stenen, en langs de oostwand kun je weer omhoog. Het is er donker: alleen zielenlantaarns en "
         "gloeiende schimmel geven wat licht. In de wanden zitten grotten met tanden van steen. Op de bodem staat een kraampje met "
         "gratis ijs. Er staat nooit iemand achter, njeg."),
        ("weg-naar-schelpenstad", "Schelpenstad",
         "Aan de overkant gaat de weg de heuvel af, onder een poort door, naar een pleintje met een schelp in de bestrating. "
         "Schelpenstad is gebouwd van schelpsteen en parelmoer: huisjes met een koepeldak, een kiosk met ansichtkaarten, een "
         "boulevard met bankjes, en een strand van kruimelzand met omkleedhokjes, parasols en strandtent De Zoute Snoet. Achter het "
         "strand houdt de plaat op: daar is de leegte de zee. Op de hoek staat vuurtoren De Lange Njeg (je mag de ladder op), bij "
         "het water de uitkijkpost van Guhwatch, en een pier steekt een flink eind de leegte in. Wie tot op de kop van de pier "
         "loopt krijgt de advancement Groetjes uit Schelpenstad."),
        ("weg-naar-schelpenstad", "De souvenirwinkel",
         "Het grootste gebouw van de stad, met een reuzenschelp op het dak, is Schelpenstad Souvenirs & Zo van de Cycloop. De "
         "planken staan vol gedroogde souvenirs: plat, stijf en met wiebeloogjes. Midden in de winkel ligt een kroonkussen, en "
         "achterin staat de droogtafel onder de droogtelamp. De lamp zit in een stopcontact, en aan het plafond hangen "
         "sproeiers. Dat is vast nergens goed voor."),
    ],
}
