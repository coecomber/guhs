"""
The docs step's own wiki texts of bbq2: what no single slice could write because it is about the whole update (the grill
portal's lock, where the new buildings stand, the overview of the buildings and their questlines, the new creatures and their
Guhdex pages, "Mijn verhaal" on the Superkompas), and of the second part: the seven big stories of Het Guhpad in one table, the
path map in the Guhdex, how rare the Super Guhrio castle is, and the pictures of Het Snuffeleiland (the breeds, the buddies,
the ranks, the residents, the dock's family) next to the paragraphs of the slices' notes.

Same shape as a slice's WIKI["tekst"], but with the English next to the Dutch: (page, "Kopje", Dutch, "Heading", English).
A text is plain paragraphs (a blank line starts a new one), or dict(tabel=(head, rows)): head = [(English, Dutch)...] and
every cell of a row a pair (English, Dutch) too, or dict(galerij=[(picture, (English, Dutch) name, (English, Dutch) text or
None)...]): a row of pictures. Names of things that have a wiki page become links by themselves.
A sixth item names the heading of a NOTE's paragraph on that page: the text then comes right after it (under the same heading
when it has the same "Kopje") instead of at the end of the page.
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
         "Het grillportaal van de Guhmensie naar de Guhbarbecuether werkt alleen voor wie twee dingen heeft gedaan. Eén: de grote "
         "verhalen van de Guhmensie gevolgd (Baltoguh en Nomguh, Guhtwo en het kloon-eiland, Het Hemelkapelletje, Ohana op Guhwai'i en Het "
         "Snuffeleiland). Twee: het eerste hoofdstuk van In de ban van de Knabbelring helemaal gedaan: Een langverwacht knabbelfeest, "
         "ongeveer tien gezellige minuten bij Guhdalf naast de grote barbecueput. Dat geldt voor iedereen, ook voor wie al eerder in de "
         "Guhbarbecuether was; wie er nog staat, mag er blijven zolang hij wil. Wie nog niet mag, leest in de chat welke verhalen hij nog "
         "mist. De weg terug, van de Guhbarbecuether naar de Guhmensie, is nooit dicht. Zie Het Guhpad.",
         "The grill portal is locked at first",
         "The grill portal from the Guhmension to the Guh Barbecuether only works for whoever has done two things. One: followed the big "
         "stories of the Guhmension (Baltoguh and Nomguh, Guhtwo and Clone Island, The Cloud Chapel, Ohana on Guhwai'i and Sniff Island). "
         "Two: done the whole first chapter of The Lord of the Nibble Ring: A Long-Expected Nibble Party, about ten cozy minutes with "
         "Guhdalf next to the big barbecue pit. That goes for everyone, also for whoever was in the Guh Barbecuether before; whoever is "
         "still standing there may stay as long as they like. Whoever may not pass yet reads in the chat which stories they are still "
         "missing. The way back, from the Guh Barbecuether to the Guhmension, is never shut. See The Guh Path."),
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
         "met de Uitvinder-guh, grillspiesen, blubroom en de gloeister) moet je door het grillportaal, en dat gaat pas open na de grote "
         "verhalen van de Guhmensie en het eerste hoofdstuk van In de ban van de Knabbelring (zie Het Guhpad).",
         "First through the grill portal",
         "The first step, Tinkering, needs nothing from the Guh Barbecuether. For everything after it (salt crystal, the Old Guh Wheel "
         "Power Plant with the Inventor Guh, grill skewers, blub cream and the glowstar) you have to go through the grill portal, and it "
         "only opens after the big stories of the Guhmension and the first chapter of The Lord of the Nibble Ring (see The Guh Path)."),
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
         "Mijn verhaal is de eerste keuze van elke tab van het Superkompas, net zo'n vakje als een plek. Volg je een verhaal (Guhdex, tab "
         "Verhalen, 'Volg dit verhaal'), dan wijst het naar de plek van je volgende stap, ook als die nog ver weg is. Volg je er geen, dan "
         "wijst het naar het dichtstbijzijnde grote verhaal dat je nog niet hebt gedaan: zo vind je bijvoorbeeld een steigerhuisje. Is dat "
         "in een andere wereld, dan wijst het naar het portaal waar je het laatst doorheen kwam. Plekken van een hoofdstuk waar je nog niet "
         "bent, staan niet in het kompas.",
         "My Story",
         "My Story is the first choice of every tab of the Super Compass, an entry just like a place. If you follow a story (Guhdex, Tales "
         "tab, 'Follow this story'), it points to the place of your next step, also when that is still far away. If you follow none, it "
         "points to the nearest big story you have not done yet: that is how you find a dock cottage, for example. If that is in another "
         "world, it points to the portal you last came through. Places of a chapter you have not reached yet are not in the compass."),
        # ===== the second part: Het Guhpad =====
        ("systemen/guhdex", "De padkaart van het Guhpad",
         "Bovenaan de tab Verhalen staat de padkaart van het hele Guhpad. Vier haltes: de Guhmensie, de Guhbarbecuether, het Guheinde en "
         "Het echte Guheinde, elk met een vinkje als je er klaar bent of een slotje als je er nog niet in mag, een stipje per groot verhaal "
         "en de teller 'Verhalen gevolgd'. Wijs een halte aan en je ziet wat hij nog vraagt; klik erop en je springt naar zijn verhalen. "
         "Daaronder staan alle verhalen per wereld: elke wereld kun je in- en uitklappen, en een groot verhaal heeft een gouden sterretje. "
         "Onder Het Snuffeleiland staat ook je snuffelrang, bijvoorbeeld 'Snuffelpup (rang 1 (laagste) van 5 (hoogste))', met alle vijf de "
         "rangen eronder.",
         "The path map of the Guh Path",
         "At the top of the Tales tab is the path map of the whole Guh Path. Four stops: the Guhmension, the Guh Barbecuether, the Guh End "
         "and The Real Guh End, each with a check mark when you are done there or a lock when you may not go in yet, a pip per big story "
         "and the counter 'Stories followed'. Point at a stop and you see what it still asks; click it and you jump to its stories. Below "
         "it are all the stories per world: you can fold every world open and shut, and a big story has a little gold star. Under Sniff "
         "Island you also find your sniffing rank, for example 'Sniff Pup (rank 1 (lowest) of 5 (highest))', with all five ranks below "
         "it."),
        ("systemen/het-guhpad", "De grote verhalen",
         dict(tabel=([("Story", "Verhaal"), ("World", "Wereld"), ("Starts at", "Begint bij"), ("Counts as followed", "Telt als gevolgd")], [
             (("Baltoguh and Nomguh", "Baltoguh en Nomguh"), ("Guhmension", "Guhmensie"),
              ("Nomguh, in the Snowguh Tundra", "Nomguh, in de Sneeuwguhtoendra"),
              ("when you are the Hero of Nomguh", "als je de Held van Nomguh bent")),
             (("Guhtwo and Clone Island", "Guhtwo en het kloon-eiland"), ("Guhmension", "Guhmensie"),
              ("Clone Island, in a Deep Guh Sea", "het kloon-eiland, in een Diepe Guhzee"),
              ("when you are the Friend of Guhtwo", "als je de Vriend van Guhtwo bent")),
             (("The Cloud Chapel", "Het Hemelkapelletje"), ("Guhmension", "Guhmensie"),
              ("the Cloud Chapel, high in the clouds", "het Hemelkapelletje, hoog in de wolken"),
              ("when the Snuggleheart beats for you", "als het Knuffelhart voor jou klopt")),
             (("Ohana on Guhwai'i", "Ohana op Guhwai'i"), ("Guhmension", "Guhmensie"),
              ("the stilt house on Guhwai'i", "het paalhuisje op Guhwai'i"),
              ("when you are an Ohana Guh", "als je Ohana-guh bent")),
             (("Sniff Island", "Het Snuffeleiland"), ("Guhmension, then the island", "Guhmensie, daarna het eiland"),
              ("a dock cottage at a Deep Guh Sea", "een steigerhuisje aan een Diepe Guhzee"),
              ("when the first sniffing series is finished (you then get the Guhstation)", "als de eerste snuffelreeks uit is (je krijgt dan het Guhstation)")),
             (("The Lord of the Nibble Ring", "In de ban van de Knabbelring"), ("Guhmension, then the Guh Barbecuether", "Guhmensie, daarna de Guhbarbecuether"),
              ("Guhdalf at the big barbecue pit", "Guhdalf bij de grote barbecueput"),
              ("when you are a Ring-Bearer", "als je Ringdrager bent")),
             (("Super Guhrio", "Super Guhrio"), ("Guh Barbecuether", "Guhbarbecuether"),
              ("Big Nether Mika's Castle, in the sauce sea", "het Kasteel van de Grote Nether-Mika, in de sauszee"),
              ("when you have played the castle to the end", "als je het kasteel hebt uitgespeeld")),
         ])),
         "The big stories", None, "De grote verhalen"),
        # ===== the castle of Super Guhrio is rare =====
        ("bouwwerken/guhrio_kasteel", "Hoe zeldzaam is het kasteel?",
         "Er staat één kasteel op redelijke afstand: 400 tot 1100 blokken van het midden van de Guhbarbecuether, in land dat nog niemand "
         "heeft verkend. Daarbuiten is het kasteel heel zeldzaam: een tweede staat meestal ver meer dan duizend blokken verder, en in "
         "sommige werelden is er binnen een paar duizend blokken geen tweede. Zoek dus niet op goed geluk de sauszee af: het Superkompas "
         "(tab Barbecue) wijst het dichtstbijzijnde kasteel aan.",
         "How rare is the castle?",
         "One castle stands at a reasonable distance: 400 to 1100 blocks from the middle of the Guh Barbecuether, in land nobody has "
         "explored yet. Beyond that the castle is very rare: a second one usually stands well over a thousand blocks further on, and in "
         "some worlds there is no second one within a few thousand blocks. So do not search the sauce sea on the off chance: the Super "
         "Compass (tab Barbecue) points to the nearest castle."),
        ("verhalen/super-guhrio", "Het kasteel",
         "Het kasteel is zeldzaam: er staat er één op redelijke afstand in nieuw land, en heel soms ver weg nog een. Het Superkompas wijst "
         "het aan.",
         "The castle",
         "The castle is rare: one stands at a reasonable distance in new land, and very rarely another one far away. The Super Compass "
         "points to it.", "Het kasteel"),
        # ===== Het Snuffeleiland: the pictures next to the notes' paragraphs =====
        ("verhalen/snuffeleiland", "Je hond en je maatje kiezen",
         dict(galerij=[
             ("snuffel_hond", ("The six breeds", "De zes rassen"),
              ("Shiba, Jack Russell, Dachshund, Corgi, Golden Retriever and Pug, each in three coats.",
               "Shiba, jack russell, teckel, corgi, golden retriever en mopshond, elk in drie vachten.")),
             ("snuffel_maatje", ("The three buddies", "De drie maatjes"),
              ("Driftseed, Moss Acorn and Sunfluff: a forest sprite that only you can see.",
               "Zweefzaadje, Mos-eikeltje en Zonnepluisje: een bosgeestje dat alleen jij kunt zien.")),
         ]),
         "Choosing your dog and your buddy", None, "Je hond en je maatje kiezen"),
        ("systemen/snuffelen", "Je eigen hond",
         dict(galerij=[
             ("snuffel_hond_shiba", ("Shiba", "Shiba"), ("Red, Black and Tan, Cream", "Rood, zwart-tan, crème")),
             ("snuffel_hond_jackrussell", ("Jack Russell", "Jack russell"), ("White with Brown, White with Black, Tricolor", "Wit met bruin, wit met zwart, driekleur")),
             ("snuffel_hond_teckel", ("Dachshund", "Teckel"), ("Red, Black and Tan, Chocolate and Tan", "Rood, zwart-tan, chocola-tan")),
             ("snuffel_hond_corgi", ("Corgi", "Corgi"), ("Red and White, Sable, Tricolor", "Rood-wit, sable, driekleur")),
             ("snuffel_hond_golden", ("Golden Retriever", "Golden retriever"), ("Gold, Light Cream, Red Gold", "Goud, licht crème, roodgoud")),
             ("snuffel_hond_mops", ("Pug", "Mopshond"), ("Beige, Apricot, Black", "Beige, abrikoos, zwart")),
         ]),
         "Your own dog", None, "Je eigen hond"),
        ("systemen/snuffelen", "Je maatje",
         dict(galerij=[
             ("snuffel_maatje_a", ("Driftseed", "Zweefzaadje"),
              ("A little ball of seed fluff with a leaf for a sail. Left its happy face, right its naughty one.",
               "Een bolletje zaadpluis met een blad als zeil. Links zijn blije gezicht, rechts zijn ondeugende.")),
             ("snuffel_maatje_b", ("Moss Acorn", "Mos-eikeltje"),
              ("An acorn with its cap pulled low over its eyes, a tuft of moss and two twig arms to point with.",
               "Een eikeltje met de dop diep over de ogen, een pluk mos en twee takarmpjes om mee te wijzen.")),
             ("snuffel_maatje_c", ("Sunfluff", "Zonnepluisje"),
              ("A glowing dandelion head with petals all around and a curly tail.",
               "Een gloeiend paardenbloemkopje met kroonblaadjes rondom en een krulstaartje.")),
         ]),
         "Your buddy", None, "Je maatje"),
        ("systemen/snuffelen", "Snuffelen en graven",
         dict(galerij=[
             ("snuffel_snuffelt", ("Nose down, tail up", "Neus omlaag, staart omhoog"),
              ("Hold the sniff key: your dog puts its nose to the ground, and your buddy floats ahead in the direction of the scent.",
               "Houd de snuffeltoets ingedrukt: je hond gaat met zijn neus over de grond, en je maatje zweeft vooruit in de richting van de geur.")),
         ]),
         "Sniffing and digging", None, "Snuffelen en graven"),
        ("systemen/snuffelen", "Snuffelrangen",
         dict(tabel=([("Rank", "Rang"), ("Name", "Naam"), ("Scents you know", "Geuren die je kent"), ("When", "Wanneer")], [
             (("rank 1 of 5 (the lowest)", "rang 1 van 5 (de laagste)"), ("Sniff Pup", "Snuffelpup"), "0", ("in this story: you earn your diploma", "in dit verhaal: je haalt je diploma")),
             (("rank 2 of 5", "rang 2 van 5"), ("Sniff Nose", "Snuffelneus"), "20", ("in a later story", "in een later verhaal")),
             (("rank 3 of 5", "rang 3 van 5"), ("Sniff Sleuth", "Snuffelspeurder"), "50", ("in a later story", "in een later verhaal")),
             (("rank 4 of 5", "rang 4 van 5"), ("Sniff Maestro", "Snuffelmeester"), "100", ("in a later story", "in een later verhaal")),
             (("rank 5 of 5 (the highest)", "rang 5 van 5 (de hoogste)"), ("Grand Sniff Maestro", "Opper-Snuffelmeester"), "150", ("in a later story", "in een later verhaal")),
         ])),
         "Sniffing ranks", None, "Snuffelrangen"),
        ("systemen/snuffelen", "Het boompje",
         dict(galerij=[
             ("snuffel_boompje", ("From sprout to young tree", "Van kiem tot jong boompje"),
              ("The four steps: sprout, shoot, little bush, young tree.", "De vier stappen: kiem, scheutje, struikje, jong boompje.")),
         ]),
         "The little tree", None, "Het boompje"),
        ("systemen/snuffeldorp-bewoners", "Wie is wie",
         dict(galerij=[
             ("snuffel_bewoner_redder", ("Wendy Wagtail", "Jutje Kwispel"), ("The beachcomber. She finds you on the beach.", "De strandjutter. Ze vindt je op het strand.")),
             ("snuffel_bewoner_dokter", ("Doctor Plasterpaw", "Dokter Pleisterpoot"), ("The village doctor, an old dachshund in a doctor's coat.", "De dorpsdokter, een oude teckel met een doktersjas.")),
             ("snuffel_bewoner_trainer", ("Master Trufflenose", "Meester Truffelneus"), ("Gives sniffing lessons in the meadow: a big sleuth hound with a green cape.", "Geeft snuffelles in de wei: een grote speurhond met een groene cape.")),
             ("snuffel_bewoner_kapitein", ("Captain Saltsnout", "Kapitein Zoutsnoet"), ("The captain of The Wet Nose, a pug with a bone for a pipe.", "De kapitein van De Natte Neus, een mopshond met een botje als pijp.")),
             ("snuffel_bewoner_bakker", ("Baker Crumbsnout", "Bakker Kruimelsnuit"), ("The baker, at his stall on the square.", "De bakker, bij zijn kraam op het plein.")),
             ("snuffel_bewoner_visser", ("Fisher Wetnose", "Visser Natneus"), ("The fisher, on the jetty.", "De visser, op de steiger.")),
             ("snuffel_bewoner_juf", ("Miss Barkley", "Juf Blaffetje"), ("The teacher of the little school.", "De juf van het schooltje.")),
             ("snuffel_bewoner_oma", ("Granny Woolly", "Oma Wolletje"), ("Knits for the whole village, on her porch.", "Breit voor het hele dorp, op haar veranda.")),
             ("snuffel_bewoner_tuinder", ("Gardener Turnip", "Tuinder Knolletje"), ("In his vegetable patch, with his straw hat.", "In zijn moestuin, met zijn strohoed.")),
             ("snuffel_bewoner_pup", ("Little Droolball", "Kleine Kwijlebal"), ("The puppy of the square.", "De puppy van het plein.")),
         ]),
         "Who is who", None, "Wie is wie"),
        ("bouwwerken/steigerhuisje", "Wie wonen er?",
         dict(galerij=[
             ("snuffel_pup", ("Little Wobble", "Kleine Wiebel"),
              ("Your little brother or sister: always a puppy of your own breed and coat. These are the six puppies.",
               "Je broertje of zusje: altijd een puppy van jouw eigen ras en vacht. Dit zijn de zes puppy's.")),
             ("steiger_buurvrouw", ("Mrs. Basket", "Buurvrouw Mandje"), ("The neighbor who keeps watch by the sickbed.", "De buurvrouw die bij het ziekbed oppast.")),
             ("steiger_boot", ("The Wet Nose", "De Natte Neus"), ("Captain Saltsnout's boat, at the end of the pier.", "De boot van Kapitein Zoutsnoet, aan het eind van de steiger.")),
         ]),
         "Who lives there?", None, "Wie wonen er?"),
        ("dimensies/snuffeleiland", "Erheen en weer naar huis",
         "De eerste keer kom je er met het verhaal: je vaart uit vanaf een steigerhuisje in de Guhmensie en spoelt aan op het strand. Daarna "
         "vaart Kapitein Zoutsnoet je er vanaf een steigerhuisje zo vaak heen als je wilt, en als je de eerste snuffelreeks uit hebt, brengt "
         "je Guhstation je naar de plek waar je de vorige keer was. Naar huis kan altijd: gebruik de geheugenkaart in je balk en kies "
         "'Opslaan en naar huis', of laat je door Kapitein Zoutsnoet in de haven van het eiland terugvaren. Je staat dan precies waar je "
         "vertrok, weer als jezelf en met al je eigen spullen. Ook als je uitlogt of als er iets misgaat, blijf je nooit als hond buiten "
         "het eiland steken en raak je niets kwijt.",
         "Getting there and home again",
         "The first time you get there with the story: you sail out from a dock cottage in the Guhmension and wash ashore on the beach. "
         "After that Captain Saltsnout sails you there from a dock cottage as often as you like, and once you have finished the first "
         "sniffing series your Guhstation takes you to the spot where you were last time. You can always go home: use the memory card in "
         "your hotbar and choose 'Save and go home', or let Captain Saltsnout in the island's harbor sail you back. You then stand exactly "
         "where you left, yourself again and with all your own things. Also when you log out or when something goes wrong, you never get "
         "stuck as a dog outside the island and you lose nothing."),
        ("dimensies/snuffeleiland", "Iedereen op één eiland",
         "Er is één eiland voor de hele server. Andere spelers zie je er als hond rondlopen, met hun naam erboven. Je voortgang, je geuren, "
         "je goede daden, je boompje en je maatje zijn van jou alleen: iedereen ziet zijn eigen boompje groeien, en je maatje kun alleen "
         "jij zien.",
         "Everybody on one island",
         "There is one island for the whole server. You see other players walking around there as dogs, with their names above them. Your "
         "progress, your scents, your good deeds, your little tree and your buddy are yours alone: everybody sees their own little tree "
         "grow, and only you can see your buddy."),
    ],
}
