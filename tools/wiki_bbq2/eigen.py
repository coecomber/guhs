"""
The docs step's own wiki texts of bbq2: what no single slice could write because it is about the whole update (the grill
portal's lock, where the new buildings stand, the overview of the buildings and their questlines, the new creatures and their
Guhdex pages, "Mijn verhaal" on the Superkompas).

Same shape as a slice's WIKI["tekst"], but with the English next to the Dutch: (page, "Kopje", Dutch, "Heading", English).
A text is plain paragraphs (a blank line starts a new one), or dict(tabel=(head, rows)): head = [(English, Dutch)...] and
every cell of a row a pair (English, Dutch) too. Names of things that have a wiki page become links by themselves.
"""

_GEBOUWEN = [
    # building, who, what you do, what you get
    (("Skewer Keep", "Spiesburcht"), ("Guard Guh", "Wachter-guh"),
     ("light the four bridge fires and weed the peanut sauce garden", "steek de vier brugvuren aan en wied het pindasaus-tuintje"),
     ("the recipe of the Sad Little Lantern, the guard outfit", "het recept van het Zielig lantaarntje, het wachterspak")),
    (("Mika grill palace", "Mika-grillpaleis"), ("Plushie Maker Guh", "Knuffelmaker-guh"),
     ("free the plushies from their three cages, with vahoege vads or by sneaking", "bevrijd de knuffels uit hun drie kooien, met vahoege vads of sluipend"),
     ("the plushie pattern and three plushies", "het knuffelpatroon en drie knuffels")),
    (("Mika Apartments", "Mika-woonblokken"), ("Granny Mika", "Mika-oma"),
     ("bring soup to three grumpy Mikas and find her knitting", "breng soep naar drie mopperende Mika's en zoek haar breiwerk"),
     ("a Knitted Mika Cap; every third barter is free", "een gebreide Mika-muts; elke derde ruil is gratis")),
    (("Mika Stable", "Mika-stal"), ("Stable Hand Guh", "Stalknecht-guh"),
     ("calm and feed the Sausage Piglets and catch the one that escaped", "kalmeer en voer de Worstzwijntjes en vang de ontsnapte"),
     ("two Sausage Piglets for your own farm", "twee Worstzwijntjes voor je eigen boerderij")),
    (("Mika Bridge Palace", "Mika-brugpaleis"), ("Toll Keeper Mika", "Tolwachter-Mika"),
     ("three riddles or pay the toll, then mend the bridge", "drie raadsels of tol betalen, en dan de brug maken"),
     ("free passage and the blueprint of the bridge", "vrije doorgang en de bouwtekening van de brug")),
    (("Fossil Dig", "Fossiel-opgraving"), ("Archaeologist Guh", "Archeoloog-guh"),
     ("dust five bones free and put the skeleton together", "kwast vijf botten los en zet het skelet in elkaar"),
     ("the Tyrannoguhrus Statuette and the Guh Dusting Brush", "het Tyrannoguhrus-beeldje en het Guhkwastje")),
    (("Salt Crystal Mine", "Zoutkristalmijn"), ("Mineworker Guh", "Mijnwerker-guh"),
     ("clear the cart track and find the crystal vein", "maak het karrenspoor vrij en vind de kristalader"),
     ("the Salt Crystal Pickaxe and salt from the vein", "de Zoutkristalhouweel en zout uit de ader")),
    (("Sauce Strider Stable", "Sausloper-stal"), ("Caretaker Guh", "Verzorger-guh"),
     ("lure, feed and ride a Sauce Strider", "lok, voer en berijd een Sausloper"),
     ("a saddle; you may tame wild Sauce Striders", "een zadel; je mag wilde Sauslopers temmen")),
    (("Old Guh Wheel Power Plant", "Oude Guhrad-centrale"), ("Inventor Guh", "Uitvinder-guh"),
     ("fix the five broken setups of the practice hall", "maak de vijf kapotte opstellingen van de oefenhal"),
     ("the Bottomless Nibble Belly for your Bank Guh and three recipe cards", "het Bodemloos Knabbelmaagje voor je Bank Guh en drie receptkaarten")),
    (("Grill Campground", "Grillcamping"), ("Camp Boss Guh, Lumberjack Guh", "Kampbaas-guh, Houthakker-guh"),
     ("pitch a tent, chop firewood, light the campfire, roast marshmallows", "zet een tent op, hak brandhout, steek het kampvuur aan, rooster marshmallows"),
     ("the recipe card of the Plantation Box and the camping outfit", "de receptkaart van de Plantagebak en het kampeerpakje")),
    (("Nether Mika Barter Market", "Nether-Mika-ruilmarkt"), ("Market Master Mika", "Marktmeester-Mika"),
     ("learn to haggle and find the fake chonk", "leer afdingen en vind de nepvads"),
     ("the Weigh House scale; a little extra with every barter", "de weegschaal van de Waag; een extraatje bij elke ruil")),
    (("Smoke Guh Lighthouse", "Rookguh-vuurtoren"), ("Lighthouse Keeper Guh", "Torenwachter-guh"),
     ("light the lamp and bring three lost Rookguhs home", "steek de lamp aan en breng drie verdwaalde Rookguhs thuis"),
     ("the Delivery Guhling Whistle and the keeper's coat", "het Bezorgguhtje-fluitje en de wachtersjas")),
    (("Pepper Garden", "Pepertuin"), ("Pepper Grower Guh", "Peperteler-guh"),
     ("grow the three peppers and brew your first pepper potion", "kweek de drie pepers en brouw je eerste peperdrankje"),
     ("pepper seeds, the other potion and a pepper garland", "peperzaadjes, het andere drankje en een peperslinger")),
]

WIKI = {
    "tekst": [
        # --- the dimension ---
        ("dimensies/barbecuether", "Het grillportaal zit eerst op slot",
         "Het grillportaal van de Guhmensie naar de Guhbarbecuether werkt alleen voor wie het eerste hoofdstuk van In de ban van de "
         "Knabbelring helemaal heeft gedaan: Een langverwacht knabbelfeest, ongeveer tien gezellige minuten bij Guhdalf naast de grote "
         "barbecueput. Dat geldt voor iedereen, ook voor wie al eerder in de Guhbarbecuether was. Wie nog niet mag, leest in de chat dat "
         "Guhdalf de teleportatiemagie heeft uitgezet. De weg terug, van de Guhbarbecuether naar de Guhmensie, is nooit dicht.",
         "The grill portal is locked at first",
         "The grill portal from the Guhmension to the Guh Barbecuether only works for whoever has done the whole first chapter of The Lord "
         "of the Nibble Ring: A Long-Expected Nibble Party, about ten cozy minutes with Guhdalf next to the big barbecue pit. That goes for "
         "everyone, also for whoever was in the Guh Barbecuether before. Whoever may not pass yet reads in the chat that Guhdalf has "
         "switched the teleportation magic off. The way back, from the Guh Barbecuether to the Guhmension, is never shut."),
        ("dimensies/barbecuether", "Dertien gebouwen met een eigen questlijn",
         "In elk van deze gebouwen woont iemand met een questlijn van een paar stappen en een bescheiden beloning. Alles is per speler: "
         "iedereen op de server kan elke questlijn doen, zo vaak er spelers zijn. Je vindt ze met het Superkompas, tab Barbecue. In de "
         "sauszee staat ook nog het Kasteel van de Grote Nether-Mika: dat is Super Guhrio.",
         "Thirteen buildings with a questline of their own",
         "In each of these buildings lives somebody with a questline of a few steps and a modest reward. Everything is per player: "
         "everyone on the server can do every questline, as often as there are players. You find them with the Super Compass, tab "
         "Barbecue. In the sauce sea there is also Big Nether Mika's Castle: that is Super Guhrio."),
        ("dimensies/barbecuether", "Dertien gebouwen met een eigen questlijn",
         dict(tabel=([("Building", "Gebouw"), ("Who", "Wie"), ("What you do", "Wat je doet"), ("What you get", "Wat je krijgt")], _GEBOUWEN)),
         "Thirteen buildings with a questline of their own", None),
        ("dimensies/barbecuether", "Nieuwe gebouwen staan in onverkend land",
         "Een nieuw gebouw verschijnt alleen in stukken wereld die nog nooit zijn gemaakt. Van elk gebouw staat er één op redelijke "
         "afstand in nieuw land, en daarna komen er verspreid nog meer. Woon je in een Guhbarbecuether die al helemaal verkend is, loop "
         "dan verder dan waar iemand ooit was: het Superkompas (tab Barbecue) wijst het dichtstbijzijnde aan. De Wachter-guh en de "
         "Knuffelmaker-guh zijn de uitzondering: zij verschijnen ook in een Spiesburcht en een Mika-grillpaleis die er al stonden.",
         "New buildings stand in unexplored land",
         "A new building only appears in pieces of world that were never made before. Of each building one stands at a reasonable "
         "distance in new land, and after that more are spread around. If you live in a Guh Barbecuether that is completely explored "
         "already, walk further than anyone ever went: the Super Compass (tab Barbecue) points to the nearest one. The Guard Guh and the "
         "Plushie Maker Guh are the exception: they also appear in a Skewer Keep and a Mika grill palace that already stood there."),
        ("dimensies/barbecuether", "Nieuwe wezens",
         "De Sausloper stapt op lange poten over de frituursaus en draagt je naar de overkant. Het Sausblubje stuitert rond en laat "
         "blubroom achter als je het knuffelt. Het Worstzwijntje is een lief boerderijdiertje uit de Mika-stal. En het Bezorgguhtje "
         "brengt op zijn stepje spullen rond tussen je machines. Geen van allen doet iemand pijn: ook de nieuwe Mika's duwen alleen.",
         "New creatures",
         "The Sauce Strider steps over the frying sauce on long legs and carries you to the other side. The Sauce Blubby bounces around "
         "and leaves blub cream behind when you cuddle it. The Sausage Piglet is a sweet farm animal from the Mika Stable. And the Delivery "
         "Guhling carries things around between your machines on its little scooter. None of them hurts anyone: the new Mikas only shove "
         "too."),
        # --- the big pit ---
        ("bouwwerken/barbecueput", "De grote barbecueput en de Knabbelgouw",
         "In nieuw land van de Guhmensie is de grote barbecueput met de Grillguh het midden van een Knabbelgouw: het Superkompas noemt "
         "hem daar Knabbelgouw. Bij een grote barbecueput die er al stond, slaat Guhdalf zijn kamp op een vrij plekje ernaast op. Op beide "
         "plekken begint, zodra de barbecue van de Grillguh brandt, het verhaal In de ban van de Knabbelring.",
         "The big barbecue pit and the Nibble Shire",
         "In new land of the Guhmension the big barbecue pit with the Grillguh is the middle of a Nibble Shire: the Super Compass calls it "
         "Nibble Shire there. At a big barbecue pit that already stood there, Guhdalf pitches his camp on a free spot next to it. In both "
         "places the story The Lord of the Nibble Ring begins as soon as the Grillguh's barbecue burns."),
        # --- Guh Technology ---
        ("systemen/guh-technologie", "Eerst door het grillportaal",
         "De eerste stap, Knutselen, heeft niets uit de Guhbarbecuether nodig. Voor alles daarna (zoutkristal, de Oude Guhrad-centrale "
         "met de Uitvinder-guh, grillspiesen, blubroom en de gloeister) moet je door het grillportaal, en dat gaat pas open na het eerste "
         "hoofdstuk van In de ban van de Knabbelring.",
         "First through the grill portal",
         "The first step, Tinkering, needs nothing from the Guh Barbecuether. For everything after it (salt crystal, the Old Guh Wheel "
         "Power Plant with the Inventor Guh, grill skewers, blub cream and the glowstar) you have to go through the grill portal, and it "
         "only opens after the first chapter of The Lord of the Nibble Ring."),
        ("systemen/guhmachines", "Het klusje staat eerst uit",
         "Machines bijvullen & leeghalen is het enige klusje dat uit staat tot de eigenaar van het huisje het aanzet: het maakt van je "
         "voorraad iets anders, en dat moet je zelf willen. Zet het bij je bewoner aan en doe elke machine één keer voor wat erin "
         "hoort.",
         "The chore is switched off at first",
         "Refill & Empty Machines is the only chore that is off until the owner of the house switches it on: it turns your stock into "
         "something else, and that has to be your own choice. Switch it on for your resident and show every machine once what belongs "
         "in it."),
        # --- the Guhdex and the Superkompas ---
        ("systemen/guhdex", "Zes nieuwe pagina's",
         "Het Sausblubje, de Sausloper, het Worstzwijntje, het Bezorgguhtje, Guhshi en Sam-guh hebben elk een pagina in de Guhdex. Ze "
         "tellen mee voor een complete Guhdex. Wie de titel van de complete Guhdex al had, houdt hem.",
         "Six new pages",
         "The Sauce Blubby, the Sauce Strider, the Sausage Piglet, the Delivery Guhling, Guhshi and Sam-guh each have a page in the Guhdex. "
         "They count for a complete Guhdex. Whoever already had the title of the complete Guhdex keeps it."),
        ("systemen/guhdex", "Een verhaal volgen",
         "De tab Verhalen heeft voor een groot verhaal een reiskaart: de hoofdstukken, een vinkje per stap, 'Je bent hier' en één zin "
         "'Dit moet je nu doen'. Latere hoofdstukken staan er als '???'. Hier zet je ook het doel linksboven in beeld aan of uit, en "
         "onder 'Opnieuw bekijken' speel je de filmpjes af die je al gezien hebt.",
         "Following a story",
         "For a big story the tab Tales has a Journey Map: the chapters, a check mark per step, 'You are here' and one sentence 'What to "
         "do now'. Later chapters are shown as '???'. Here you also switch the objective in the top left of the screen on or off, and "
         "under 'Watch Again' you play the scenes you have already seen."),
        ("systemen/superkompas", "Mijn verhaal",
         "Volg je een verhaal met een reiskaart, dan heeft het Superkompas een vakje 'Mijn verhaal': het wijst naar de plek van je "
         "volgende stap, ook als die nog ver weg is. Plekken van een hoofdstuk waar je nog niet bent, staan niet in het kompas.",
         "My Story",
         "When you follow a story with a Journey Map, the Super Compass has an entry 'My Story': it points to the place of your next "
         "step, also when that is still far away. Places of a chapter you have not reached yet are not in the compass."),
    ],
}
