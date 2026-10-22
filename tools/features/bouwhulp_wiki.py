"""
1.6.0 deel 2 Bouwhulp (F13): what the wiki says about building help: the Bouwkeet and its levels, ghost building, the
vormstaf, the kwasten, blueprints and the Ontwerpbureau, the tools of the higher levels, the new building blocks, the
helper animals and the Beverbeek. Not a feature module (not in FEATURES): the docs step (tools/wiki_bbq2) reads WIKI; the
English is in tools/wiki_bbq2/en_safari.py.
"""
WIKI = {
    "verhalen": {},
    "systemen": {
        "bouwhulp": (
            "Bouwhulp", "block:guhs:bouwkeet",
            "Sneller en mooier bouwen: alles wat je bouwt staat eerst als spook neer, de Bouwkeet bouwt het voor je met materiaal uit "
            "je kisten, en met de vormstaf, de kwasten en blauwdrukken maak je in een tel een koepel, een dak of een heel huisje.",
            ["bouwkeet-niveaus", "spookbouw", "vormstaf", "kwasten", "blauwdrukken", "bouwgereedschap", "bouwblokken", "helperdieren"]),
        "spookbouw": (
            "Spookbouw", "block:guhs:bouwkeet",
            "Alles van de bouwhulp verschijnt eerst als spook: een doorzichtig voorbeeld dat je kunt verschuiven, draaien en spiegelen. "
            "Pas als je op Bouw! drukt komen de echte blokken.",
            ["bouwhulp", "bouwkeet-niveaus", "bank_guh"]),
        "bouwkeet-niveaus": (
            "De Bouwkeet en zijn niveaus", "block:guhs:bouwkeet",
            "Een geel-roze keetje van 3 bij 3 bij 3 met een kraantje op het dak. Daar begint het bouwen. Met vadskracht en vier "
            "upgrades groeit hij van Handwerk tot Gloeikeet.",
            ["bouwkeet", "bouwhulp", "spookbouw", "vadskracht"]),
        "vormstaf": (
            "De vormstaf: vormen en daken", "vormstaf",
            "Een bol, koepel, boog, wenteltrap, hart of zelfs een guh: kies een vorm, zet hem als spook neer en laat hem bouwen. Ook "
            "daken met de juiste hoekjes.",
            ["vormstaf", "bouwhulp", "spookbouw", "bouwkeet-niveaus"]),
        "kwasten": (
            "De kwasten", "patroonkwast",
            "Vier kwasten om te verven wat er al staat: omwisselen, mengen, patronen (ook dierpatronen uit het safaripark) en "
            "verweren met mos en barstjes.",
            ["vervangkwast", "mengkwast", "patroonkwast", "verweerkwast", "bouwhulp"]),
        "blauwdrukken": (
            "Blauwdrukken en het Ontwerpbureau", "block:guhs:ontwerpbureau",
            "Leg vast wat je gebouwd hebt en bouw het ergens anders opnieuw, kies uit de catalogus, of bouw na wat andere spelers in de "
            "bibliotheek van de server hebben gezet.",
            ["ontwerpbureau", "blauwdruk", "bouwhulp", "spookbouw"]),
        "bouwgereedschap": (
            "Egaliseerder, verfmachine en tunnelboor", "egaliseerder",
            "Drie stukken gereedschap voor de hogere niveaus van de Bouwkeet: terrein vlak maken, alles in één keer omkleuren en een "
            "mooie tunnel boren.",
            ["egaliseerder", "verfmachine", "tunnelboor", "bouwkeet-niveaus"]),
        "bouwblokken": (
            "De nieuwe bouwblokken", "block:guhs:pastelbaksteen_roze",
            "Meer dan 400 nieuwe blokken om mee te bouwen, in een eigen creatief tabblad Guhs: Bouwen: pastelbakstenen, dakdelen, "
            "verticale platen, balken, kozijnen, behang, knuffelblokken en nog veel meer.",
            ["bouwhulp"]),
        "helperdieren": (
            "Helperdieren: dieren die bouwen", "block:guhs:safari_verblijfsbord",
            "Je dierenfamilie thuis kan voor je bouwen: bevers bouwen een dam waarachter het water stijgt, pinguïns een iglo, "
            "orang-oetans een boomhut. Ze kijken naar het echte terrein en maken iets dat precies past.",
            ["verblijfsborden", "spookbouw", "bouwhulp", "biomen/beverbeek"]),
    },
    "npc_home": {"beverbeek_ranger": "biomen/beverbeek"},
    "entity_home": {},
    "tekst": [
        # --- overview ---
        ("bouwhulp", "Zo begin je",
         "Maak een Bouwkeet (gele en roze wol, een werkbank, stammen en kaasknabbels) en een vormstaf (twee stokjes en een "
         "amethistscherf). Klik op de keet voor het menu, of klik met de vormstaf op een blok: er verschijnt een spook van je vorm. "
         "Druk op Bouw! en het wordt gebouwd. Alle bouwhulp-spullen staan in het creatieve tabblad Guhs: Bouwen."),
        # --- ghosts ---
        ("spookbouw", "Een spook neerzetten",
         "Een spook is het echte blok, maar doorzichtig en zacht lichtgevend. Iedereen ziet het, de eigenaar net iets sterker. Met "
         "de pijltjes in het paneel schuif je het vooruit, opzij, omhoog of omlaag (vanuit waar je kijkt), en je kunt het draaien en "
         "spiegelen. Via een naamvak mag een vriend meehelpen."),
        ("spookbouw", "Materiaal",
         "De blokken komen eerst uit je inventaris, dan uit een gekoppelde Bank Guh (klik met een Banksleutel op de "
         "keet) en, vanaf een Guhrad-keet, uit alle kisten die aan het vadsnet van de keet zitten. Ontbreekt er iets, "
         "dan zie je precies wat: ga met je muis op een blokje staan voor de naam. Met Alsnog bouwen wordt gebouwd wat "
         "er is, en de rest blijft spook tot je het hebt. In creatief bouw je gratis."),
        ("spookbouw", "Bouwen en terugdraaien",
         "Er wordt van onder naar boven gebouwd, een paar blokjes per tik. Blokken die al kloppen worden overgeslagen, en kisten, "
         "machines en beschermde gebouwen worden nooit aangeraakt. De laatste vijf bouwacties kun je terugdraaien; wat in de weg "
         "stond komt dan precies terug. Je kunt tot tien spoken tegelijk open hebben staan."),
        ("spookbouw", "Boodschappenlijstje",
         "Mist er iets? Klik op Lijstje (in de keet of in het spookpaneel): je krijgt een gewoon boek met alle blokken "
         "die nog ontbreken, met de precieze naam en het aantal (en hoeveel stapels dat is). Handig naast de werkbank."),
        # --- the keet ---
        ("bouwkeet-niveaus", "De niveaus",
         "Handwerk (zonder vadskracht): vormstaf en kwasten, vormen tot 16 blokken, daken, eigen blauwdrukken en de "
         "bibliotheek. Bouwt tot 64 blokken van de keet.\n\nGuhrad-keet (10 VK): bouwguhs, vormen tot 24, en materiaal "
         "uit alle kisten op het vadsnet. Bouwt tot 96 blokken ver.\n\nZoutkeet (30 VK): natuurvormen, bloktekst, de "
         "egaliseerder en blauwdrukken tot 32. Bouwt tot 128 blokken ver.\n\nSauskeet (80 VK): de verfmachine, de "
         "tunnelboor en publiceren in de bibliotheek. Bouwt tot 160 blokken ver.\n\nGloeikeet (200 VK): vormen en "
         "blauwdrukken tot 64, en Pof! Bouwt tot 192 blokken ver."),
        ("bouwkeet-niveaus", "Upgraden",
         "Klik met een upgrade op de keet, steeds één niveau tegelijk: het Guhrad-tandwieltje, de Zoutkristal-waterpas, het "
         "Saus-kraanhaakje en de Gloei-bouwlamp. Vanaf de Guhrad-keet trekt de keet vadskracht zolang hij aan staat. Zonder "
         "vadskracht werkt hij gewoon als Handwerk; zijn niveau houdt hij."),
        ("bouwkeet-niveaus", "Bouwguhs",
         "In de tab Bouwguhs wijs je tot vier van je eigen guhs aan. Ze zetten een bouwhelmpje op (hun eigen hoed krijgen ze terug), "
         "lopen langs de bouwplek en fluiten erbij. Elke bouwguh in de buurt maakt het bouwen sneller. Het kraantje op de keet draait "
         "mee, en een balkje boven de keet laat zien hoe ver het is. Als het af is, is er een klein feestje."),
        ("bouwkeet-niveaus", "Pof!",
         "Een Gloeikeet met al het materiaal bij de hand bouwt het hele project in één keer: een roze wolk, POF!, en het staat er."),
        ("bouwkeet-niveaus", "Bereik",
         "Een keet bouwt zijn project tot een bepaalde afstand: 64 blokken op Handwerk, en 32 meer per niveau, tot 192 "
         "op de Gloeikeet. Schuif je het spook (met de pijltjes in de keet of het spookpaneel) zo ver dat een stuk "
         "buiten het bereik valt, dan wordt dat stuk rood en zie je een waarschuwing. Dat deel wordt niet gebouwd tot "
         "je het dichterbij schuift of de keet een niveau hoger maakt. Tijdens het verschuiven zie je de grens van het "
         "bereik als rode stofjes op de grond."),
        ("bouwkeet-niveaus", "Hoe lang duurt het?",
         "De keet bouwt vijf blokken per seconde, en elke bouwguh in de buurt doet er vijf bij. Een huis van "
         "zevenhonderd blokken duurt dus een paar minuten. In de keet zie je hoe lang het nog duurt, en boven de keet "
         "zweeft een balkje. Een spook blijft staan als je het menu sluit: je kunt rondlopen, terugkomen en verder "
         "schuiven tot je Bouw! zegt."),
        # --- vormstaf ---
        ("vormstaf", "Vormen",
         "Bol, koepel, cilinder, halve cilinder, boog, tunnel, brug, piramide, kegel, ring, wenteltrap, helix, hart, guh, ei en "
         "kasteelmuur, hol of vol, met een wanddikte naar keuze. Vanaf een Zoutkeet ook vier natuurvormen (een rots, een heuvel, een "
         "boomkruin en een zweefeiland) en bloktekst. Hoe groot een vorm mag zijn hangt af van het niveau van je keet."),
        ("vormstaf", "Daken",
         "Een zadeldak, schilddak, tentdak of oortjesdak, met de trapjes in de hoeken goed uitgerekend."),
        ("vormstaf", "Bediening",
         "Klik op een blok: je vorm staat er als spook. Sluip-klik op een blok: dat wordt je materiaal (tot vier "
         "blokken gemengd). Houd de vormstaf-toets in (standaard linker Alt) voor het ronde menu, met de vormen, daken, "
         "het instellingenscherm en vastleggen. Klik je in het instellingenscherm of in de keet op de vorm, dan krijg "
         "je een raster met alle vormen, elk met een plaatje en de naam; grijs is nog op slot. Je ziet steeds een "
         "voorbeeld waar je kijkt, alleen voor jou. Sluip-klik met gereedschap op een Bouwkeet om het aan die keet te "
         "koppelen: dan werkt het op zijn niveau."),
        # --- kwasten ---
        ("kwasten", "Vier kwasten",
         "De vervangkwast verft blokken om naar jouw blok, in dezelfde vorm (een trap blijft een trap). De mengkwast verft een mix van "
         "tot vier blokken. De patroonkwast verft strepen, een dambord, een verloop of guh-gezichtjes. De verweerkwast maakt alles "
         "een beetje oud en knus, met mos, barstjes en klimop. Elke klik verft een bolletje blokken, en wordt meteen gebouwd."),
        ("kwasten", "Dierpatronen",
         "Zebra, giraffe, luipaard, jachtluipaard, pinguïn, panda, rode panda en sabeltandtijger: elk dierpatroon gaat open zodra "
         "dat dier in je Safari-dex staat. Ze kleuren wol, beton en terracotta."),
        # --- blueprints ---
        ("blauwdrukken", "Vastleggen",
         "Kies vastleggen in het menu van de vormstaf en klik twee hoeken aan, of sluip-klik één keer op een gebouw: dan zoekt hij "
         "het gebouw zelf. Roze glinsters laten het kader zien, en elke kant kun je nog verschuiven. Je kunt tot dertig eigen "
         "blauwdrukken bewaren."),
        ("blauwdrukken", "Het Ontwerpbureau",
         "Aan het Ontwerpbureau blader je door je eigen blauwdrukken, de catalogus en de bibliotheek. Druk er een af (een vel papier) "
         "en je hebt een blauwdruk in je hand: klik ermee op een blok en het ontwerp staat er als spook. Hij raakt nooit op."),
        ("blauwdrukken", "De catalogus",
         "Vijfentwintig ontwerpen om meteen te bouwen, in vier groepen: huizen (van een klein guh-huisje tot een villa), tuin "
         "(een kas met oortjes, een bloemenprieel, een speeltuintje), torens (een vuurtoren, een windmolen, een boomhut) en safari "
         "(een lodgehutje, een uitkijktoren, een safaripoort). In de safari-giftshop koop je de gebouwen van het park ook als "
         "blauwdruk."),
        ("blauwdrukken", "De bibliotheek",
         "Vanaf een Sauskeet kun je een blauwdruk publiceren in de bibliotheek van de server, met jouw naam erbij. Iedereen kan hem "
         "dan bouwen en een duimpje geven. Iedere speler kan er tien publiceren."),
        # --- tools ---
        ("bouwgereedschap", "De egaliseerder",
         "Vanaf een Zoutkeet: maak een stuk terrein vlak, of maak een helling, kuil, vijver of heuvel. Eerst staat het als spook "
         "met een paneel, daarna wordt het gebouwd."),
        ("bouwgereedschap", "De verfmachine",
         "Vanaf een Sauskeet: kleurt wol, beton en terracotta in één keer om, ook als regenboogverloop."),
        ("bouwgereedschap", "De tunnelboor",
         "Vanaf een Sauskeet: boort een ronde, vierkante of gewelfde tunnel, met bekleding en lampjes. Geen mijnmodus: gewoon een "
         "mooie tunnel."),
        # --- blocks ---
        ("bouwblokken", "Wat er is",
         "Pastelbakstenen en pleister in 16 kleuren (met trap, plaat en muurtje), dakpannen in 6 kleuren, guh-tegels, behang met een "
         "plint die vanzelf onderaan komt, knuffelblokken die een beetje veren en piepen, glas-in-lood en patrijspoorten, "
         "guh-kozijnen en luikjes met een hartje, dakgoten, schoorstenen die roken en dakkapellen."),
        ("bouwblokken", "Vormhulp",
         "Verticale platen en hoekjes van hout, steen en de nieuwe stenen; palen en balken die vanzelf aan elkaar vastzitten; en "
         "schuine dakdelen van een half en een kwart blok. Bouw je een helling op, dan kiest het dakdeel vanzelf het volgende stukje; "
         "rechtsklik geeft het volgende."),
        ("bouwblokken", "Tuin en straat",
         "Guh-hekjes met oortjes en poortjes, een lantaarnpaal met een guh-petje, een deurmat \"WELKOM GUH\", bankjes om op te zitten "
         "en een vogelhuisje waar af en toe een vogeltje in zingt."),
        # --- helpers ---
        ("helperdieren", "Zo werkt het",
         "Klik op het verblijfsbord van een helperdier en open de tab Bouwopdracht. Kies een taak, en kies Zoek plek of wijs zelf een "
         "plek aan met de vormstaf. Het dier rekent een spook uit dat bij het echte terrein past. Verschuif of draai het, laat het "
         "opnieuw uitrekenen, en bevestig. De familie gaat dan op werkuitje (tot 48 blokken van het bord), bouwt het met eigen "
         "werkanimaties, en loopt daarna blij terug."),
        ("helperdieren", "Regels",
         "Eén klus per bord tegelijk. De dieren zijn gratis, alleen de blokken kosten iets: uit je inventaris of een Bank Guh die je "
         "aan het bord koppelt met een Banksleutel. Hoe blijer de familie, hoe groter en mooier ze bouwen."),
        ("helperdieren", "Wie kan wat?",
         "Bever: een dam met een beverpoel, een burcht, brug, steiger, vlonder of bomen kappen. Stokstaartje, das en wombat: een "
         "kelder, tunnel, vijver, holletjesnet of beekje. Olifant: sjouwen voor een Bouwkeet in de buurt, omgevallen bomen opruimen, "
         "terrein vlak stampen of een modderpoel. Orang-oetan: een boomhut, touwbrug of hangbrug. Pinguïn: een iglo, ijsglijbaan, "
         "ijssculptuur of ijsvloer. Capibara: een warm badje, vijvertje of hottub. Zebra: zebrapaden. Koala, toekan en hommel: "
         "planten. Neushoorn en kafferbuffel: slopen. Otter: stapstenen of een modderglijbaan. Flamingo: een oever met roze bloemen, "
         "riet en waterlelies. Kangoeroe: buideltransport tussen twee kisten. Mammoet: sneeuw wegschuiven of een sneeuwlaag."),
        ("helperdieren", "Bouwdieren",
         "Specht: houtsnijwerk in muren en balken. Octopus: een kamer inrichten. Gibbon: slingers en lampionnen ophangen. Gekko: "
         "klimop, bloembakjes en luikjes op een gevel. Glimwormpjes: zacht blauw licht in een tunnel of kelder. Prieelvogel: kleine "
         "deco in een kleurthema."),
        ("helperdieren", "De beverdam",
         "De bevers zoeken de smalste plek van stromend water in de buurt. Het spook laat de dam zien én de nieuwe waterlijn: het "
         "water erachter stijgt een tot drie blokken en wordt een beverpoel. Stroomt er niks, dan zeggen ze \"Hier stroomt niks, "
         "njeg\": laat dan eerst de graafploeg een beekje graven. Na vijf dammen krijg je de titel Beverbaas."),
        ("helperdieren", "Bouw passend verblijf",
         "In de tab Tevredenheid van elk verblijfsbord zet de knop Bouw passend verblijf eerst een voorstel neer als "
         "spook: een verblijf met alles wat het dier nodig heeft, zijn speeltje en een guh-hek met poortje. Kies "
         "Goedgekeurd, laat bouwen en de dieren bouwen het zelf met jouw blokken, of Annuleren. Het hek staat altijd "
         "hoog genoeg: geen dier springt eroverheen."),
        ("helperdieren", "Grijze knoppen",
         "Is een knop grijs, ga er dan met je muis op staan: er staat altijd waarom. Meestal heeft het bord al een klus "
         "(ook een voorstel van Bouw passend verblijf telt): stop of annuleer die eerst."),
        # --- the Beverbeek ---
        ("biomen/beverbeek", "Het beekjesbos",
         "Een zeldzaam bos van wilgen, elzen en berken in de Guhmensie, met slingerende beekjes en kleine watervalletjes. Achter "
         "wilde dammen liggen beverpoelen met een burcht, en overal zie je knaagstronkjes, bladhoopjes en pootafdrukjes. Er lopen geen "
         "wilde bevers rond. De Beverbeek groeit alleen in nieuwe stukken wereld."),
        ("biomen/beverbeek", "De rangerpost",
         "Heel soms staat er een blokhut met een veranda: de rangerpost. De Beverbeek-ranger vertelt over bevers en wijst je de weg "
         "naar de safaribus. Bij de post woont Spaander, een bever die je niet kunt temmen of meenemen, maar wel lief kunt aaien."),
    ],
}
