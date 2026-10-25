"""
The English of the 1.7.0 "Onder de Wol" (Under the Wool) wiki pages (tools/wiki_bbq2/en.py takes these two tables into its
own; the Dutch is in tools/features/onderwol_wiki.py).

PAGINA: page -> (English title, English lead).
TEKST:  (page, Dutch heading) -> (English heading, English text).

Names as in en_us.json (Felt Stone, Snugglestone, Guh Crystal Stone, Plush Seam, Guh Clay, Vahoegite, Glitter Altar, Cuddle
Bookshelf, Little Spell Book, Vahoege Vadscracker, Cloud Lift, Fluff Ladder, Little Mine Lamp, Saddle Bag, Mine Guh House...).
"""

PAGINA = {
    "systemen/onder-de-wol": (
        "Under the Wool: the underground",
        "Under the pink wool of the Guhmension there is now a whole plush underground: stone layers that get harder the deeper "
        "you go, ores per layer, caves with six cave biomes of their own, sauce lakes, more than twenty buildings under the "
        "ground, an underground villager town and five cave minigames. Only in new chunks."),
    "systemen/steenlagen-en-ertsen": (
        "Stone layers and ores",
        "The deeper you dig, the stonier it gets: pink wool, Felt Stone, Snugglestone, Guh Crystal Stone and, at the very bottom, "
        "the unbreakable Plush Seam. Every layer has its own ores, and deeper is richer."),
    "systemen/grotten": (
        "Caves and cave biomes",
        "Winding tunnels, big cheese-hole halls and now and then a gigantic hall, sauce lakes and sauce falls, plush heart geodes "
        "and six cave biomes, each with its own plants, light and cheerful music. Nothing hostile: only sweet cave dwellers."),
    "systemen/ondergrondse-bouwwerken": (
        "Buildings under the ground",
        "From an abandoned guh mine to a bathhouse full of warm sauce: all over the underground of the Guhmension buildings stand "
        "in a cave of their own, with signs, residents, loot and sometimes guh clothes. The Mijnguh Village is always close to the "
        "start of your world."),
    "systemen/guh-villagers": (
        "Guh villagers and the Vads Mine Town",
        "Real villagers in guh style: a new cute model, four new professions, a pink Iron Guhlem of their own and an underground "
        "town full of guh villagers. And now you can pick up any villager."),
    "systemen/glinsteraltaar": (
        "The Glitter Altar and the spells",
        "The guh version of the enchanting table: a glittering crystal altar with a floating book, powered by Cuddle Bookshelves. "
        "With the useful vanilla enchantments and eight guh spells of its own, and Little Spell Books to put them on with an anvil."),
    "systemen/mijnhulpjes": (
        "Mining helpers: tools and handy things",
        "The Vahoege Vadscracker (five tools in one), super vahoege vads, the Cloud Lift for your mine shaft, the Fluff Ladder that "
        "unrolls by itself and a Little Mine Lamp for your guh."),
    "systemen/mijn-guhhuisje": (
        "The Mine Guh House and the Saddle Bag",
        "Help the Miner Guh and you get a Mine Guh House: up to four of your own guhs move in and slowly mine out an area under the "
        "house. With a Saddle Bag a guh carries an endless amount of plain ground blocks."),
    "systemen/voetjes": (
        "Feet: the seventh clothing slot",
        "Your guh's wardrobe has a slot for the feet: slippers, boots, golf shoes and two pairs of striders that let your tamed "
        "guhs walk over cheese sauce and even over lava."),
    "systemen/vrolijk-oppervlak": (
        "A more cheerful Guhmension",
        "Above ground the Guhmension got prettier too: a patchwork of pastel wool, flower meadows, brooks from spring to spring, "
        "pink beaches, little islands, heart lakes, rainbow hills and the rare Flower Valley."),
    "systemen/nieuw-in-1-7": (
        "New in 1.7.0: Under the Wool",
        "What's new in Guhs 1.7.0 \"Onder de Wol\" (Under the Wool): a whole underground below the Guhmension, buildings, villagers, "
        "enchanting, mining helpers, five cave minigames, 18 holes of golf, feet clothes and a more cheerful surface. The full list is "
        "in the mod's CHANGELOG."),
    "systemen/grot-minigames": (
        "The five cave minigames",
        "Under pink huts in the Guhmension lie five game halls: Treasure Dig, Glowworm Catching, the Little Lights Puzzle, the cart "
        "switches in the Switch Cave and guh bowling in the Bowling Cave. Each with its own coin, a shop, highscores and guh clothes."),
}

TEKST = {
    # --- what's new ---
    ("systemen/nieuw-in-1-7", "Onder de grond"): (
        "Under the ground",
        "Stone layers (Felt Stone, Snugglestone, Guh Crystal Stone and the Plush Seam), ores per layer, guh clay, pigment and "
        "Vahoegite, caves with six cave biomes and cheerful cave music, a sauce sea, more than twenty buildings under the ground, the "
        "Mijnguh Village and the Vads Mine Town. Nothing hostile under the ground: but the Cave Guh, the Badger-Mole Guh and little "
        "glow worms. Only in new chunks."),
    ("systemen/nieuw-in-1-7", "Spullen en systemen"): (
        "Things and systems",
        "The Glitter Altar with Cuddle Bookshelves, eight guh spells and Little Spell Books; guh villagers with a new model, four new "
        "professions, the Iron Guhlem and picking up villagers; the Vahoege Vadscracker, super vahoege vads, the Cloud Lift, the Fluff "
        "Ladder and the Little Mine Lamp; the Mine Guh House with the Miner Guh's helper quest and the Saddle Bag; the feet clothing "
        "slot with Cheese Sauce Striders and Lava Striders; bigger terraria; a Smoke Guh Platform of your own."),
    ("systemen/nieuw-in-1-7", "Spelletjes"): (
        "Games",
        "Five cave minigames (Treasure Dig, Glowworm Catching, the Little Lights Puzzle, cart switches and the Bowling Cave) with their "
        "own coins and shops, and new golf courses with 18 holes, a golf shop and the golf trophy."),
    ("systemen/nieuw-in-1-7", "Bovengronds"): (
        "Above ground",
        "A more cheerful Guhmension (patchwork, flower meadows, brooks, pink beaches, little islands, heart lakes, rainbow hills and "
        "the Flower Valley) and a Barbecuether without lava: everything is harmless Cheese Frying Sauce."),
    # --- overview ---
    ("systemen/onder-de-wol", "Waarom naar beneden?"): (
        "Why go down?",
        "Everything under the Guhmension used to be pink wool, so mining wasn't all that exciting. Now you find stone to build with "
        "under the wool (Felt Stone and Snugglestone in bulk), ores, guh clay and pigment for the Building Help blocks, Vahoegite "
        "for the best tools, caves to explore and buildings with loot."),
    ("systemen/onder-de-wol", "Nieuwe chunks"): (
        "New chunks",
        "The underground, the caves, the buildings and the prettier surface only come in new chunks. What was generated already "
        "stays as it was: so go exploring a little further out."),
    # --- layers and ores ---
    ("systemen/steenlagen-en-ertsen", "De lagen"): (
        "The layers",
        "From top to bottom:\n\n"
        "- Pink wool: as always, with a thick layer on top.\n"
        "- Felt Stone (about y 45 to 64): soft and quick to mine, with a whole family (polished, bricks, tiles, chiseled, stairs, "
        "slabs and walls).\n"
        "- Snugglestone (about y 20 to 45): finally in bulk, with veins of pink plush stone, pockets of wool in the stone, pink "
        "crystal points and pastel stripes.\n"
        "- Guh Crystal Stone (about y 5 to 20): the deepest layer, with the richest ores.\n"
        "- Plush Seam (y 0 to 4): the unbreakable bottom.\n\n"
        "The borders wave a few blocks up and down and follow the land: higher under mountains, lower under valleys and the sea. "
        "There is always a layer of biome on the surface, with a bare patch of Felt Stone here and there."),
    ("systemen/steenlagen-en-ertsen", "Ertsen per laag"): (
        "Ores per layer",
        "Every vanilla ore (coal, copper, iron, gold, lapis, redstone, diamond and emerald) and the guh ores (vads ore, cheese "
        "nibble ore, cheese grain ore and pigment ore) have a version in Felt Stone, Snugglestone and Guh Crystal Stone. They drop "
        "the same as in vanilla (fortune and smelting work as usual). Coal and cheese nibbles are mostly near the top, iron in the "
        "middle, gold, vads and cheese grains deeper, redstone and diamond only in the Guh Crystal Stone. Emerald is only found under "
        "the Guh Peaks and the Chonk Cliffs. The pink wool only holds the familiar clumps of cheese nibbles."),
    ("systemen/steenlagen-en-ertsen", "Guhklei en pigment"): (
        "Guh clay and pigment",
        "Guh Clay lies in big clumps in the Felt Stone and the Snugglestone. It gives clay balls (bake them into bricks), and the "
        "block itself bakes into pink terracotta: the raw material for the roof tiles and plaster of the Building Help. Pigment ore "
        "lies in the Snugglestone and the Guh Crystal Stone and drops pastel dye: the common colours near the top, the rarer ones deeper."),
    ("systemen/steenlagen-en-ertsen", "Vahoegiet"): (
        "Vahoegite",
        "The rarest ore: only in the Guh Crystal Stone, between y 5 and 18, and in the plush heart geodes. You need Vahoegite for the "
        "Glitter Altar, the Saddle Bag, the Miner Guh's helper quest and the Vahoegite Ingot of super vahoege vads."),
    # --- caves ---
    ("systemen/grotten", "Grotten per laag"): (
        "Caves per layer",
        "The pink wool has few caves: narrow winding tunnels and cave mouths that lead down. In the Felt Stone they become winding "
        "tunnels, in the Snugglestone big cheese-hole halls with little sauce lakes and sauce falls, and in the Guh Crystal Stone "
        "gigantic halls, plush heart geodes (hollow crystal balls) and big sauce lakes."),
    ("systemen/grotten", "De sauszee"): (
        "The sauce sea",
        "In about a quarter of the underground, in big patches, the deepest caves fill with warm cheese sauce up to a fixed level "
        "(around y 10): an underground sauce sea. Higher up lie separate little sauce lakes, and sauce falls pour out of the walls."),
    ("systemen/grotten", "Water onder de grond"): (
        "Water under the ground",
        "Here and there lies a still cave pond with a glowing rim of glow moss and sparkle mushrooms, and a little waterfall comes "
        "out of some walls. Less often than in an ordinary world, but extra sweet."),
    ("systemen/grotten", "Zes grotbiomen"): (
        "Six cave biomes",
        "Every cave biome lies in its own layer and has its own plants, light and a cheerful little song:\n\n"
        "- Fluff Cave (Felt Stone): fluff wool, fluff puffs and soft fluff cushions.\n"
        "- Sleepy Cave (Felt Stone): wild guhs lie asleep among night lights.\n"
        "- Plush Root Cave (Felt Stone, only under forests): hanging plush roots and moss.\n"
        "- Glitter Cavern (Snugglestone): glitter vines, glow moss, sparkle mushrooms and little glow worms.\n"
        "- Little Bells Cave (Snugglestone): crystals that play a note.\n"
        "- Crystal Icicle Cave (Snugglestone and deeper): pink crystal icicles on the ceiling and the floor.\n\n"
        "The Holey Cheese Caves and the Guh Crystal Mine are still there too, fitted to the new layers. You find a special cave every "
        "few hundred blocks; ordinary caves are the most common."),
    ("systemen/grotten", "Vrolijke grotmuziek"): (
        "Cheerful cave music",
        "Under the ground of the Guhmension you no longer hear scary cave sounds. Every cave biome has a cheerful, cute song of its "
        "own, and instead of the rumbling in the dark you hear soft drops and little bells. Other dimensions sound as always."),
    ("systemen/grotten", "Wie woont er onder de grond?"): (
        "Who lives under the ground?",
        "Nothing hostile: in the Guhmension monsters don't spawn by themselves under the ground (except in their own buildings). "
        "The caves are mostly home to ordinary wild guhs, and very now and then (as rare as a chocolate guh) a Cave Guh with deep "
        "pink fur full of crystal specks that softly glow in the dark. The Badger-Mole Guh pokes its head out of the Felt Stone and "
        "the Snugglestone, digs back in and sometimes leaves guh clay, dye or (very rarely) Vahoegite behind. In the Glitter Cavern "
        "little glow worms hang from the ceiling on a thread."),
    ("systemen/grotten", "Een grot zoeken"): (
        "Finding a cave",
        "For operators (and in creative): /guhs grot <name> finds the nearest cave biome of that kind and takes you there. The "
        "ordinary /locate biome only looks at a few heights and often misses the thin cave layers."),
    # --- the cave biomes' own pages ---
    ("biomen/glitterspelonk", "In de Glitterspelonk"): (
        "In the Glitter Cavern",
        "A cave biome in the Snugglestone. Glowing glitter vines hang from the ceiling (some with glitter berries to pick and eat), "
        "the floor is covered in pink glow moss and sparkle mushrooms, and little glow worms hang on their threads in small groups. "
        "Cave Guhs are a bit more common here."),
    ("biomen/pluisgrot", "In de Pluisgrot"): (
        "In the Fluff Cave",
        "A cave biome in the Felt Stone, lined with fluff wool and fluff grass. Walk through the fluff puffs and fluff blows up. On "
        "a fluff cushion you land softly, with a little bounce."),
    ("biomen/slaapgrot", "In de Slaapgrot"): (
        "In the Sleepy Cave",
        "A cave biome in the Felt Stone with a floor of fluff cushions and wool. Wild guhs lie curled up asleep here (zzz) and snore "
        "now and then. Pet one (right-click with an empty hand) or give it a cheese nibble: it yawns, stretches and is an ordinary "
        "wild guh after that. Sparkle mushrooms are the night lights."),
    ("biomen/knuffelwortelgrot", "In de Knuffelwortelgrot"): (
        "In the Plush Root Cave",
        "A cave biome in the Felt Stone, only under forests. Soft plush roots hang from the ceiling, and moss grows on the plush root "
        "soil. Bone meal on plush root soil grows a new plush root under it."),
    ("biomen/belletjesgrot", "In de Belletjesgrot"): (
        "In the Little Bells Cave",
        "A cave biome in the Snugglestone full of bell crystals and little bell clusters. Step on one, hit it or click a cluster and "
        "it plays a note, with a soft echo. Every spot has its own note: a row of crystals is a scale, so you can tap tunes on them."),
    ("biomen/kristalpegelgrot", "In de Kristalpegelgrot"): (
        "In the Crystal Icicle Cave",
        "A cave biome in the Snugglestone and deeper, like a dripstone cave but pink: crystal icicles hang from the ceiling and stand "
        "on the floor, their tips glow softly, and now and then a drop of cheese sauce drips off. Watch out: an icicle that comes "
        "loose falls down and hurts, and so does falling onto an icicle tip."),
    # --- buildings ---
    ("systemen/ondergrondse-bouwwerken", "Altijd in de buurt: het Mijnguhdorp"): (
        "Always nearby: the Mijnguh Village",
        "Every world has one Mijnguh Village between about 300 and 1000 blocks from the middle (in new land), and now and then "
        "another one further out. It lies around a warm sauce lake, with houses on both shores, terraces, the Miner Guh's stall and "
        "side tunnels to attached buildings (such as the Cuddle Dormitory, the Dairy, the Wool Spinnery and the Crystal Nursery). A "
        "Mine Entrance Hut above ground takes you down with a cloud lift."),
    ("systemen/ondergrondse-bouwwerken", "Vaak, af en toe en zeldzaam"): (
        "Common, now and then, and rare",
        "Common: the Abandoned Guh Mine (a random network of tunnels with special rooms and chest sleds), Cave Paintings and the "
        "Sauce Lake Fishing Hut.\n\n"
        "Now and then: the Abandoned Sled Station, The Golden Sauce Dairy, the Sunken Guh House, the Giant Guh Fossil, the Crystal "
        "Nursery, the Lost Building Crew, the Cave Guh Burrows, The Warm Nibble Bathhouse, the Cave Disco, the Old Wool Spinnery, "
        "the Star Cave, the Cuddle Dormitory, the Mushroom Village, the Plush Pit, the Surprise Party and the Mine Entrance Hut.\n\n"
        "Rare: the Old Guh Temple (a huge temple complex) and the Old Bank Vault. Every so many hundred blocks there is also a Vads "
        "Mine Town and one of the cave minigames."),
    ("systemen/ondergrondse-bouwwerken", "Wat vind je er?"): (
        "What do you find there?",
        "Signs with guh jokes and residents who tell you what's what, building materials, cheese nibbles and vads, and a few deco "
        "blocks of each place's own. A few buildings that got extra care hide guh clothes too. Very rarely a Super Vads Template "
        "lies in the chests of the deepest buildings, and here and there a Little Spell Book. The old buildings from before (such as "
        "the guh caves and the cheese mine) now stand in the stone of their depth."),
    ("bouwwerken/mijnguhdorp", "De Mijnguh"): (
        "The Miner Guh",
        "The Miner Guh sits in his stall by the sauce lake. He asks you for help (the quest Mijnguh-hulpje) and then gives you a "
        "Mine Guh House."),
    ("bouwwerken/vadsmijnstadje", "Een stadje van echte villagers"): (
        "A town of real villagers",
        "A big cave hall in the Snugglestone, about 60 by 60 blocks, with ten houses of Felt Stone and pastel bricks around a square. "
        "On the square the Guh Wheel plant turns: a guh in a Guh Wheel that powers the town with vadskracht. Ten guh villagers live "
        "there (with the four new professions and a few of the old guh jobs), two children and two Iron Guhlems. A Mine Entrance Hut "
        "above ground takes you down with the cloud lift. There is one about every 1500 blocks."),
    # --- villagers ---
    ("systemen/guh-villagers", "Een nieuw model"): (
        "A new model",
        "Every guh villager (also those in the guh villages above ground) has a new, cuter guh model: a round guh head for the "
        "grown-ups and a little plush villager for the children. Their trades stay the same."),
    ("systemen/guh-villagers", "Vier nieuwe beroepen"): (
        "Four new professions",
        "- Wizard Guh (workstation: the Wizard Guh Lectern): sells Little Spell Books, also with the spells you don't get from the "
        "Glitter Altar.\n"
        "- Chonk Mechanic (the Guh Wheel Workbench): Guh Wheels, Guh Wire, Guh Ovens and more machines for vadskracht; he buys "
        "vahoege vads.\n"
        "- Miner (the Ore Chest): layer stones, guh clay, dye, felt gravel, glow moss and lamps; he buys your ores and Vahoegite.\n"
        "- Crystal Cutter (the Crystal Grindstone): cuts guh crystal and Vahoegite into crystal lamps, crystal icicles, bell crystals "
        "and more glittering blocks."),
    ("systemen/guh-villagers", "De IJzerguhlem"): (
        "The Iron Guhlem",
        "Guh villagers don't summon an iron golem but an Iron Guhlem: lavender iron, a round guh head, a fluffy pink collar and a "
        "flower crown. It does exactly what an iron golem does and appears by the same rules, so an iron farm with guh villagers "
        "works as usual. It drops iron ingots and fluff grass instead of poppies."),
    ("systemen/guh-villagers", "Villagers oppakken"): (
        "Picking up villagers",
        "Sneak and right-click a villager with an empty hand (an ordinary one or a guh villager, grown-up or child) and you pick it "
        "up, like a tamed guh. It keeps its profession, level, trades and name. Right-click the ground to put it back. Not while it "
        "is trading, and not the villagers that belong to a building or a minigame."),
    # --- the altar ---
    ("systemen/glinsteraltaar", "Zo werkt het"): (
        "How it works",
        "The Glitter Altar works like an enchanting table: put a tool, a weapon, a piece of armour or a book on it, with lapis, and "
        "choose one of the three spells. It costs lapis and levels, like in vanilla. How strong the spells are depends on the Cuddle "
        "Bookshelves around the altar (like bookshelves around an enchanting table, up to 15); ordinary bookshelves don't count here. "
        "Hearts fly from the shelves to the altar."),
    ("systemen/glinsteraltaar", "Wat zit erin?"): (
        "What's in it?",
        "The useful vanilla enchantments: sharpness, protection, efficiency, unbreaking, power, fortune, looting, silk touch, feather "
        "falling, respiration, depth strider, swift sneak and (rarely) mending. Plus five guh spells: Mika Whacker, Fluff Fall, "
        "Glitter Sight, Vads Diligence and Baking Vads. Unbreaking and mending never go on things that don't break anyway (like "
        "vahoege vads and the Mika-mepper)."),
    ("systemen/glinsteraltaar", "De acht guh-spreukjes"): (
        "The eight guh spells",
        "- Mika Whacker (weapons): double damage against Mikas. Not on the Mika-mepper itself.\n"
        "- Fluff Fall (boots): no fall damage when you land on wool, felt or fluff. Poof!\n"
        "- Glitter Sight (helmet): a soft glittering night vision, only under the ground.\n"
        "- Vads Diligence (tools, up to level 3): much faster through wool, felt and cheese nibble blocks.\n"
        "- Baking Vads (mining tools): what you mine comes out of the ground already smelted.\n"
        "- Piggy Guh (mining tools): what you mine and pick up goes straight to the Bank Guh of the Bank Key in your inventory. Kaching!\n"
        "- Gentle Pat (weapons): your hits never hurt your own pets or villagers.\n"
        "- Cheese Nibble Scent (any piece of armour): every wild guh nearby comes to you and follows you around.\n\n"
        "You never get Piggy Guh, Gentle Pat and Cheese Nibble Scent from the altar: only as a Little Spell Book, from the Wizard Guh "
        "or from a chest in a building under the ground."),
    ("systemen/glinsteraltaar", "Spreukjesboekjes"): (
        "Little Spell Books",
        "A Little Spell Book is the guh enchanted book: a lilac book with hearts. You get one when you enchant a book on the altar "
        "with a guh spell, from the Wizard Guh or from chests. On an anvil you put the spell on your things, like an enchanted book."),
    # --- mining helpers ---
    ("systemen/mijnhulpjes", "De Vahoege Vadsknakker"): (
        "The Vahoege Vadscracker",
        "Pickaxe, axe, shovel, hoe and shears in one. You make it from a vads paxel, vahoege vads shears and a vahoege vads hoe; "
        "their enchantments come along. Right-click does whatever fits the block: a log is stripped, grass and dirt become farmland, "
        "sneaking on grass makes a path, and a sheep gets shorn. Guhs keep their wool. It never breaks."),
    ("systemen/mijnhulpjes", "Super vahoege vads"): (
        "Super vahoege vads",
        "The strongest version of the vads tools and armour (as strong as netherite). Make a Vahoegite Ingot from 4 Vahoegite and 4 "
        "vahoege vads ingots, and put the Super Vads Template, a vahoege vads item and the ingot on the smithing table. The template "
        "lies very rarely in the chests of the deepest buildings under the ground (such as the Old Guh Temple and the Old Bank Vault); "
        "copy it with 7 Vahoegite and Guh Crystal Stone. Everything can be done in the Guhmension: you don't have to go to the "
        "Barbecuether for it."),
    ("systemen/mijnhulpjes", "Het Wolkenliftje"): (
        "The Cloud Lift",
        "An elevator for your mine: put a Cloud Lift at the bottom and one up on the ground, straight above each other. Stand on it "
        "and jump: you float up in a pink cloud to the next Cloud Lift above (up to 256 blocks, with two free blocks above it). "
        "Sneaking takes you to the next one below. It works right through the stone."),
    ("systemen/mijnhulpjes", "De Pluisladder"): (
        "The Fluff Ladder",
        "A soft pink rope ladder on a roll. Put it against a wall at the edge of a hole and it unrolls down to the floor by itself. "
        "Break a piece and the whole ladder rolls up again and you get your roll back."),
    ("systemen/mijnhulpjes", "Het Mijnlampje"): (
        "The Little Mine Lamp",
        "A pink helmet with a lamp for your tamed guh (guh clothes for the head). Wherever it walks, it gives light in the dark: "
        "handy when it follows you down the mine."),
    # --- the mine house ---
    ("systemen/mijn-guhhuisje", "De quest Mijnguh-hulpje"): (
        "The quest Miner Guh's helper",
        "Talk to the Miner Guh (in the Mijnguh Village under the ground, or in the cheese mine) and he asks for 16 Felt Stone, a guh "
        "clay and a Vahoegite. Bring them and you get the Mine Guh House and a Little Mine Lamp for your guh. Want another house? "
        "Bring him the same again, with the Vahoegite in your hand."),
    ("systemen/mijn-guhhuisje", "Zo werkt het huisje"): (
        "How the house works",
        "Click the house with a picked-up guh and it moves in: at most four of your own tamed guhs. In the menu you set the area with "
        "sliders (up to 20 blocks to each side and as deep as you like, never below y 10); pink sparkles show the edge. For free each "
        "guh mines a block every 20 seconds, with cheese nibbles four times as fast. What they mine goes into a chest next to the "
        "house, or into the Bank Guh of a Bank Key. They leave buildings, water and lava alone."),
    ("systemen/mijn-guhhuisje", "Het Rugzakzadel"): (
        "The Saddle Bag",
        "Guh clothes for the back, made with a saddle, pink wool, a chest and a Vahoegite. In the bag sits a little guh that keeps "
        "everything in its tummy, so there is no end to what fits in: plain ground blocks such as wool, the layer stones, dirt, stone, "
        "sand, gravel and guh clay (no ores or rare things). When a guh with a Saddle Bag lives in a Mine Guh House, it puts the plain "
        "ground in its bag and brings it to the Bank Guh (as much as fits; the rest stays in the bag)."),
    # --- feet ---
    ("systemen/voetjes", "Wat er is"): (
        "What there is",
        "- Fluffy Slippers and Guh Rain Boots: from the tailor, for 6 and 8 emeralds.\n"
        "- Golf Shoes: in the Golfguh's golf shop, for 12 golf balls.\n"
        "- Miner's Boots: rare in the chests of the Old Cheese Mineshaft.\n"
        "- Cheese Sauce Striders: rare in the chests of the Mika grill palace and the Skewer Keep.\n"
        "- Lava Striders: let the tailor upgrade your Cheese Sauce Striders with 4 magma cream and a vahoege vads."),
    ("systemen/voetjes", "Lopers"): (
        "Striders",
        "A tamed guh with Cheese Sauce Striders walks on top of every cheese sauce, also on the hot Cheese Frying Sauce of the "
        "Barbecuether, as if it were a floor. It doesn't sink in, doesn't burn and simply follows you across. With Lava Striders it "
        "can do that on real lava too."),
    ("systemen/kleding", "Voetjes"): (
        "Feet",
        "Since 1.7.0 the wardrobe has a seventh slot: the feet, for slippers, boots, golf shoes and striders. See Feet: the seventh "
        "clothing slot."),
    # --- surface ---
    ("systemen/vrolijk-oppervlak", "Bloemen, keien en vijvertjes"): (
        "Flowers, boulders and ponds",
        "A patchwork of lilac, white and peach wool, flower meadows full of guh flowers and butterflies, wool boulders with moss, "
        "small ponds with water lilies and reeds, little waterfalls from the hillsides, fairy rings that glow at night (sometimes a "
        "guh dances inside), fallen logs with mushrooms, groups of trees, flowering bushes, hedges and fluff in the air. Felt gravel "
        "lies along the water: soft gravel of felt balls."),
    ("systemen/vrolijk-oppervlak", "Beekjes en strandjes"): (
        "Brooks and beaches",
        "Winding brooks and brooks that run down from a little spring on a hill, with a step here and there, pink glitter above the "
        "water, glitter reed and water lilies. Pink beaches (with little shells) lie next to the ordinary ones along lakes and seas, "
        "and little islands with a tree lie in bigger lakes and off the coast."),
    ("systemen/vrolijk-oppervlak", "Pareltjes"): (
        "Little gems",
        "Very now and then: a heart lake (a heart from above, with a flower rim and heart lily pads), rainbow hills with pastel "
        "stripes in the slopes, and in the Cheese Flats and the Nibble Crumbs a cheese patchwork of holey cheese, cheese moss, cheese "
        "flowers and puddles of cheese sauce. And the Flower Valley: a small, rare biome full of flowers in every guh colour, with "
        "bees, butterflies and fluff. In the evening the sky is a softer pink, and at night little glow guhs fly over the fields and "
        "the water."),
    ("biomen/bloemenvallei", "De Bloemenvallei"): (
        "The Flower Valley",
        "A small, rare biome of the Guhmension (since 1.7.0), full of flowers in every guh colour, with bees, butterflies and fluff "
        "in the air. Sometimes a heart lake lies in it."),
    # --- the cave minigames ---
    ("systemen/grot-minigames", "Hoe kom je er?"): (
        "How do you get there?",
        "Every game hall lies deep under a pink hut somewhere in the Guhmension, about every 500 blocks. Stand on the Cloud Lift in the "
        "hut and sneak: you float down; jumping on the lift in the hall takes you back up. Every hall also has two or three extra "
        "entrances: a little tunnel with a door and an \"Entrance\" sign, so if you dig towards a hall from a cave you get in too. "
        "Only the hall itself is protected, not the rock around it. The Super Compass points to the nearest one."),
    ("systemen/grot-minigames", "Munten en winkeltjes"): (
        "Coins and shops",
        "Every game has its own coin: treasure coins, glimmer coins, lamp coins, ore tickets and pin buttons. The better you play, "
        "the more coins, and a new personal record adds a few more. In the host's shop you buy guh clothes, building blocks (four "
        "for one coin) and deco with them. All five coins also work in the claw machine. Your very first game brings a present: 16 "
        "cheese nibbles and something nice for your home."),
    ("minigames/schatgraven", "Zo speel je"): (
        "How to play",
        "Captain Shovelsnout lends you his soft shovel. You have 90 seconds to dig up the treasure under the fluff field (right- or "
        "left-click the field). Sniffy sniffs along: COLD, warm or HOT (then he jumps!). Every treasure is 10 points, the golden "
        "cheese 25, and a squeaky cheese costs you 10 seconds. Found everything? Then the seconds left count too. Easy is a small "
        "field with lots of help from Sniffy, hard the whole field, and Sniffy sniffs less often."),
    ("minigames/schatgraven", "Het winkeltje"): (
        "The shop",
        "The eye patch (40 treasure coins) and the pirate hat (60), treasure sandstone, treasure chest planks, fluff wool and felt "
        "gravel, and as deco the golden cheese (24) and the little treasure chest (40)."),
    ("minigames/glimwormvangen", "Zo speel je"): (
        "How to play",
        "Miss Glimmy gives you a net and a jar on your back. You have 60 seconds: swing your net (right-click) at the glowworms right "
        "in front of you. Some are quick, some hide in the glow bushes, and a golden glowworm is 5 points. The more you catch, the "
        "brighter your jar and the hall. Hard: more and quicker glowworms, and it starts darker."),
    ("minigames/glimwormvangen", "Het winkeltje"): (
        "The shop",
        "The butterfly net (50 glimmer coins) and the glowworm feelers (60), night felt and glow moss, and as deco the glow bush (20) "
        "and the glowworm jar (24)."),
    ("minigames/lichtjespuzzel", "Zo speel je"): (
        "How to play",
        "Master Blinky has a big raster of 7 by 7 toadstool lamps. Jump on a lamp (or crouch on it): it and its four neighbours "
        "switch, on becomes off and off becomes on. Switch them all on before the time is up. Every puzzle can be done, and he knows "
        "the fewest jumps it takes: the closer you get to that and the faster you are, the more lamp coins. Hard: a trickier puzzle "
        "and less time."),
    ("minigames/lichtjespuzzel", "Het winkeltje"): (
        "The shop",
        "The glowing toadstool hat (70 lamp coins), toadstool stone and mushroom stem bricks, and as deco the toadstool lamp (28), "
        "the toadstool stool (32) and the lamp tile (40)."),
    ("minigames/karretjes", "Zo speel je"): (
        "How to play",
        "You stand with Conducteur Piepwiel on the balcony above a network of rails. The carts roll out of the tunnel, each full of "
        "one ore: the colour of the load says which bin it belongs in. Above every switch there is a lever on the railing (on is "
        "right, off is left). A cart in the wrong bin is gone; three gone and your shift is over. They come ever faster. There are "
        "three tracks: 2, 3 or 5 switches."),
    ("minigames/karretjes", "Het winkeltje"): (
        "The shop",
        "The conductor's cap (50 ore tickets), sleeper wood, ore gravel stone and ore gravel bricks, and as deco the ore cart (64, you "
        "can sit in it), the station clock (24) and the signal lamp (20)."),
    ("minigames/kegelen", "Zo speel je"): (
        "How to play",
        "Real bowling with De Guhde: ten frames, two balls per frame, at ten plush guh pins that squeak. A strike counts 10 plus your "
        "next two balls, a spare 10 plus the next one, and in the tenth frame you get bonus balls. Look where the wool ball should "
        "go, hold right-click for power and let go. Stand left or right on the approach for a hook, or sneak for a straight ball. "
        "Three lanes: straight, bendy and one with obstacles."),
    ("minigames/kegelen", "Het winkeltje"): (
        "The shop",
        "The bowling shirt (70 pin buttons) and the sweatband (40), lane wood and retro tiles, and as deco the plush pin (20), the "
        "wool ball rack (40) and the neon bowling sign (32)."),
    # --- golf ---
    ("minigames/golf", "De achterste 9"): (
        "The back 9",
        "New golf courses (since 1.7.0) have 18 holes; courses that already existed keep 9. The back 9 are a Tour of the Guhmension: "
        "every hole is a mini version of a place in the Guhmension, with moving obstacles, ramps, loopings, bumpers, slippery ice "
        "felt and sticky cheese mud, and the Big Guh Mouth as the finale. They open once you have played the front 9 at or under par. "
        "Par: 25 on easy, 38 on medium and 47 on hard. At the Golfguh choose the front 9, the back 9 or all 18."),
    ("minigames/golf", "Meer golfballetjes"): (
        "More golf balls",
        "A hole of the back 9 gives more golf balls than one of the front: 8 for a hole-in-one, 6 for an eagle, 5 for a birdie, 4 for "
        "par, 2 for a bogey and otherwise 1. After all 18 holes you get 5 more, another 8 if you were at or under par and another 4 "
        "for a new 18-hole record."),
    ("minigames/golf", "Het golfwinkeltje"): (
        "The golf shop",
        "Next to the golf outfit the Golfguh now sells golf shoes (12), golf course felt and checkered felt in three colours (four "
        "blocks for one golf ball), and deco: golf balls on a tee (3), a hole with a flag (6), a golf rack (10) and a golf cart to sit "
        "in (64)."),
    ("minigames/golf", "De golftrofee"): (
        "The golf trophy",
        "Not for sale: play all 18 holes at or under par and the Golfguh gives you the golf trophy once, with your name and record on it."),
    # --- elsewhere ---
    ("dimensies/barbecuether", "Geen lava meer"): (
        "No more lava",
        "Since 1.7.0 there is no real lava in the Barbecuether any more: it is all Cheese Frying Sauce, and that is harmless (no "
        "burns, no fires). Lava in chunks that already existed turns into sauce by itself when they load; your world is not made "
        "again. With Cheese Sauce Striders your tamed guhs walk over the sauce."),
    ("verhalen/knabbelring", "Je eigen Rookguh-platform"): (
        "Your own Smoke Guh Platform",
        "Finish the whole journey of the Nibble Ring and the Smoke Guh gives you a Smoke Guh Platform of your own (whoever had done "
        "that already got it at their first login after 1.7.0). Put it down wherever you like, in any world: the Smoke Guh always "
        "flies you there, and from your platform he flies you to any place of your journey, or to the Super Guhrio castle once you "
        "have finished world 1-1. You have one; put it down somewhere else and your landing place moves along."),
    ("systemen/verblijfsborden", "Grotere terraria"): (
        "Bigger terraria",
        "Since 1.7.0 a terrarium may have any shape: build terrarium glass or terrarium floor onto the glass tank, and everything "
        "joined to it like that (a big glass tank, tunnels one block wide, up and down) is one living space. The critters walk "
        "around in all of it, but never out of it. Two terraria joined by glass don't share: one of them gets the whole space."),
    ("systemen/commandos", "Grotten zoeken (1.7.0)"): (
        "Finding caves (1.7.0)",
        "/guhs grot <name> (operators): finds the nearest cave biome of that kind in the Guhmension and takes you there. Names: "
        "glitterspelonk, pluisgrot, slaapgrot, knuffelwortelgrot, belletjesgrot, kristalpegelgrot, gatenkaasgrotten and guh_kristalmijn."),
    ("dimensies/guhmension", "Onder de Wol (1.7.0)"): (
        "Under the Wool (1.7.0)",
        "Under the wool of the Guhmension there are now stone layers, ores, caves with six cave biomes and more than twenty "
        "buildings under the ground. See Under the Wool: the underground."),
}
