"""
The English of the bbq2 wiki pages (tools/wiki_bbq2/__init__.py reads this; the Dutch is in tools/features/*_wiki.py).

PAGINA: page -> (English title, English lead).
TEKST:  (page, Dutch heading) -> (English heading, English text). A blank line starts a new paragraph, as in the Dutch.

Hand-written, with the names of tools/lang/GLOSSARY.md section 24 (vadskracht = chonk power, Knabbelbuis = Nibble Tube,
Hapluikje = Nom Hatch, In de ban van de Knabbelring = The Lord of the Nibble Ring, Frituurberg = Mount Fry, ...), American
spelling, and the rule that nobody gets hurt: a Mika, the Eye, the Nine or a falling ember only "shove" or "send you back".
The numbers are the game's numbers of the day this was written; when a Dutch text changes (a number too), the build says so
(`python tools/wiki_bbq2/check.py`), and after the English is brought up to date `--stamp` records it.
"""

PAGINA = {
    # --- Guh Technology ---
    "systemen/vadskracht": (
        "Chonk power",
        "Chonk power is what every guh machine runs on. Guhs run it together in a Guh Wheel, Guh Wire carries it to your machines, and "
        "when you ask for more than your guhs give, everything stops for a moment."),
    "systemen/vadskrachtbronnen": (
        "Sources of chonk power",
        "Chonk power comes from guhs. They run it together in a Guh Wheel, cuddle it together on a Cuddle Generator or dance it together "
        "on a Disco Dynamo. Two sources work without a guh: the Blub Stove and the Glowstar Core."),
    "systemen/knabbelbuizen": (
        "Nibble Tubes",
        "See-through tubes in which you watch your things roll by: from chest to machine, from machine to the Bank Guh. A Direction Piece "
        "noms the things out, a Filter Piece sorts them, and the Slurper picks up what lies on the ground."),
    "systemen/sensoren": (
        "Sensors and the Guh Clock",
        "Four little guh machines that give a redstone signal: the Stock Meter counts what is in a chest, the Sniff Sensor smells who is "
        "nearby, the Guh Clock ticks and the Guh Counter counts the ticks."),
    "systemen/guhmachines": (
        "Guh machines",
        "Machines that do the work for you: harvesting, nibbling, placing, tinkering, milling and growing trees. They all run on chonk "
        "power and they are all little guhs: a snoot, two ears and something that moves while they are busy."),
    "systemen/saus": (
        "Sauce through hoses",
        "With a Sauce Pump, Sauce Hoses and a Sauce Vat you let cheese sauce, cheese frying sauce, water and milk run to your machines by "
        "themselves. The Auto Brewer, the Auto Fryer and the Grill Coal Press do the rest with it, on chonk power."),
    "systemen/bezorgguhtje": (
        "The Delivery Guhling",
        "A mini guh on a scooter with a backpack that is far too big. It lives in a Scooter Station and brings things from Stop Post to "
        "Stop Post. Toot toot, nyeg!"),
    "systemen/bank-guh-buikje": (
        "The Bank Guh's tummy",
        "A Bank Guh's tummy holds at most 256 of each kind. With the Inventor Guh's Bottomless Nibble Belly it holds as much as you like, "
        "and a Nom Hatch puts things into your bank from very far away."),
    "systemen/guh-technologie": (
        "Guh Technology: from wheel to factory",
        "Guh Technology comes in four steps: Tinkering (right away), Salt (salt crystal), Sauce (the Inventor Guh's recipe cards) and "
        "Glowstar (after the Aangebrande Mika). The quest book chapter Guh Technology is built from projects: after every project "
        "something works by itself in your own world."),
    "verhalen/techniek": (
        "The Inventor Guh's practice hall",
        "In the Old Guh Wheel Power Plant, deep in the Guh Barbecuether, five old guhs are still running their laps. The Inventor Guh has a "
        "practice hall there with five setups, and they are all broken. You fix them one by one: laying Guh Wire in a gap, making a setup "
        "that is too heavy lighter, turning a Direction Piece around, setting a Filter Piece right and connecting a Sauce Hose. In between "
        "he wants four salt crystals. When everything works again you get the Bottomless Nibble Belly for your Bank Guh and his three "
        "recipe cards."),
    "verhalen/knabbelmachine": (
        "The Great Nibble Machine",
        "Whoever finished the practice hall and beat the Aangebrande Mika may build The Great Nibble Machine with the Inventor Guh: a guh "
        "of copper and pink enamel, nine blocks tall. It is a building project in five stages for which you bring mountains of things a "
        "factory makes. Every player builds a machine of their own and only sees their own grow. When it stands, there is one perfect "
        "nibble a day in the little tray under its mouth, and you are a Nibble Machinist."),
    # --- the buildings ---
    "systemen/zoutkristal": (
        "Salt crystal",
        "Salt crystal is the raw material for the second step of Guh Technology (tubes, sensors, the Nom Hatch, the Nibble Battery). It "
        "comes from the crystal vein of the Salt Crystal Mine, from salt crystal ore and now and then from bone sand."),
    "verhalen/archeoloog": (
        "The Tyrannoguhrus Nyex",
        "In the Guh Barbecuether the Archaeologist Guh is digging up the skeleton of a giant guh. The big skeleton stays where it is, but "
        "in the bone sand of the pit there is a small one in pieces. You get a Guh Dusting Brush, dust five bones free and put the "
        "Tyrannoguhrus Nyex together bone by bone on the rack under the little roof. Every player finds their own bones and sees their "
        "own skeleton: the sand and the rack change for nobody else."),
    "verhalen/mijnwerker": (
        "Salt on Your Sandwich",
        "The roof of the Salt Crystal Mine has come down and the cart track is full of rubble. Chop five chunks off the track, follow it "
        "into the crystal cave and chop at the pink crystal vein. The Mineworker Guh wants three salt crystals on his sandwich and gives "
        "you his Salt Crystal Pickaxe in return."),
    "verhalen/wachter": (
        "The Watch at the Skewer Keep",
        "In the hall of every Skewer Keep the Guard Guh stands in his sentry box, next to the statue. The Vonk-Mika's blew out the four "
        "bridge fires and his little peanut sauce garden is full of Mika Weed. Light the brazier on every bridge with his lighting skewer "
        "and weed the six tufts of Mika Weed, and you get the recipe of the Sad Little Lantern, two lanterns and the guard outfit."),
    "verhalen/knuffelmaker": (
        "The Stolen Plushies",
        "On the first floor of every Mika grill palace the captive Plushie Maker Guh sits in his sewing corner. The Mikas make him darn "
        "socks and have put his plush guhs in three cages. Free them with one vahoege vads per cage, or sneak and pick the lock while no "
        "Nether-Mika is looking. Then bring four string: you get the plushie pattern and three plushies."),
    "verhalen/mika-oma": (
        "Granny Mika's Soup",
        "Granny Mika lives on the top walkway of the Mika Apartments and takes no part in stealing nibbles: she knits and cooks sausage "
        "soup. Bring her soup to the three grumps of the block (Grumble Mika, Whiny Mika and Snore Mika) and look for her knitting, which "
        "blew away, on the roof garden. You get a Knitted Mika Cap, and from then on every third barter with a Nether-Mika is free."),
    "verhalen/stalknecht": (
        "The Restless Sausage Piglets",
        "In the Mika Stable the Stable Hand Guh secretly takes good care of the Mikas' Sausage Piglets. Pet three of them calm, fill the "
        "feeding trough and catch Oinky, who has escaped (sneak, or he runs off). As thanks you get two Sausage Piglets in a basket, to "
        "take home."),
    "verhalen/tolwachter": (
        "The Toll Bridge of the Mika Bridge Palace",
        "The Mika Bridge Palace is a toll bridge high above the sauce: you walk in through the mouth of a gigantic Mika. The Toll Keeper "
        "Mika only lets you through when you pay 8 kaasknabbels or solve three riddles. After that you put the five rows of planks back in "
        "the gap in the bridge and ring the toll bell. From then on you may always pass for free, and you get the blueprint of the bridge."),
    "verhalen/sausloper": (
        "The Sauce Strider Stable",
        "Along the frying sauce sea of the Guh Barbecuether stands a stable with a copper roof and pink guh ears. There the Caretaker Guh "
        "teaches you how to make friends with a Sauce Strider: luring with peanut sauce on a stick, feeding with peanut sprouts and a test "
        "lap through the sauce basin. After that you get a saddle and may tame a wild Sauce Strider yourself."),
    "systemen/sausdieren": (
        "Sauce Striders and Sauce Blubbies",
        "Two sweet animals of the frying sauce sea. The Sauce Strider steps over the sauce on long legs and carries you to the other side. "
        "The Sauce Blubby bounces around and leaves blub cream behind when you cuddle it."),
    "verhalen/camping": (
        "Camping at The Glowing Guh",
        "On a cave floor in the Guh Barbecuether lies the Grill Campground: a little village of tents around a big campfire, with a "
        "reception, a wood shed and five camper guhs. The Camp Boss Guh gives you a tent bag. You pitch your own tent on a free spot and "
        "hammer in the four stakes, chop six blocks of firewood at the Lumberjack Guh's, light the campfire with it (the camper guhs come "
        "and sit by it and dance) and roast three marshmallows golden brown with the Roasting Stick. As thanks you get the recipe card of "
        "the Plantation Box and the camping outfit for your guh."),
    "verhalen/ruilmarkt": (
        "The Fake Chonk of the Barter Market",
        "On the Nether Mika Barter Market striped stalls stand around the Weigh House. Somebody is paying with fake chonk there, and the "
        "Market Master Mika is looking for help. First you learn to haggle: read what he says and pick the answer that fits. Then you weigh "
        "the five stacks of chonk in the Weigh House two by two and put the Seal of Approval on the stack that is lighter. As thanks you "
        "get the scale, and from then on every Nether-Mika gives you a little extra when you barter."),
    "systemen/tentdoek": (
        "Tent Canvas",
        "Tent Canvas is a building block in five colors (red, yellow, green, blue and cream), as a block, as a sloped piece and as a slab. "
        "The tents of the Grill Campground and the awnings of the barter market are made of it."),
    "verhalen/vuurtoren": (
        "The Smoke Guh Lighthouse",
        "In the smoke of the Guh Barbecuether stands a red and white lighthouse with two pink guh ears on its copper roof. The lamp is out, "
        "and without light the Rookguhs cannot find their way home. The Lighthouse Keeper Guh asks you to light the lamp and to bring three "
        "lost Rookguhs to the light. You get a Delivery Guhling Whistle and a keeper's coat for your guh."),
    "verhalen/pepertuin": (
        "The Pepper Garden",
        "A glass greenhouse with a garden full of peppers beside it. The Pepper Grower Guh teaches you to grow peppers: one plant, three "
        "peppers, and the soil decides which. In his greenhouse you grow all three in grow boxes of your own, and then you brew your first "
        "pepper potion. You get seeds to take home, the other potion and a pepper garland for your guh."),
    "systemen/pepers": (
        "Peppers and pepper potions",
        "The pepper plant is the only plant that feels at home in the smoke of the Guh Barbecuether. One plant gives three kinds of "
        "peppers, depending on the soil. Two of them you brew into a Guh Potion."),
    # --- The Lord of the Nibble Ring ---
    "verhalen/knabbelring": (
        "The Lord of the Nibble Ring",
        "The great story of the Guh Barbecuether: a ring-shaped nibble that makes everyone greedy has to go to Mount Fry, not to destroy it "
        "but to fry it and share it. Six chapters, an extra tower, and Sam-guh who walks with you the whole way."),
    "systemen/knabbelring": (
        "The Nibble Ring",
        "What the ring does as long as you carry it: it whispers that you should eat it, wild guhs follow you drooling, putting it on makes "
        "you invisible to Mikas but the Eye sees you and the Nine come, and near the mountain it gets heavy."),
    "systemen/rustpunten": (
        "Rest Stops",
        "Little camps with a Rest Fire, all over the Guh Barbecuether. The last rest stop you visited is the place you come back to when "
        "you are seen or caught. Sam-guh cooks one stew a day there."),
    "systemen/gaven-van-guhladriel": (
        "The Gifts of Guhladriel",
        "The Light Phial, the Little Elven Cloak and the Elven Rope: what they do in the story and what use they are afterwards."),
    "verhalen/ring-h1": (
        "A Long-Expected Nibble Party",
        "Chapter 1 of the Nibble Ring. Guhdalf stands with his cart full of fireworks at the big barbecue pit and has switched the grill "
        "portal off. Help him with the farewell party, get the Nibble Ring, pack provisions with Sam-guh and walk to the portal together: "
        "then the portal works again. About ten cozy minutes, and nothing goes wrong."),
    "verhalen/ring-h2": (
        "The Council of Guhrond",
        "Chapter 2 of The Lord of the Nibble Ring. In Guhvendell, the elf house of Guhrond under three waterfalls of cheese sauce, a "
        "council gathers about the ring. Everyone wants to eat it, Gimguh even tries. You volunteer to take it to Mount Fry and set out "
        "with eight companions: the Fellowship of the Nibble Ring."),
    "verhalen/ring-h3": (
        "The Mines of Nibblemoria",
        "Chapter 3 of The Lord of the Nibble Ring. Under the Houtskoolvlakte lies the old mine of the dwarf guhs. You only get in with the "
        "right word, and you only get through with four levers, a secret door and a bridge over a deep chasm. With the whole company, in "
        "the dark."),
    "verhalen/ring-h4": (
        "The Mirror of Guhladriel",
        "Chapter 4 of The Lord of the Nibble Ring. After the mine you rest in Caras Guhladhon, the tree city of Lady Guhladriel in the "
        "golden wood Guhlórien. You look into her mirror, receive three gifts and sail the elven boats down the Guhduin, between the two "
        "giant statues of the Arguhnath. Nothing in this chapter does anything to you: it is the rest before the Black Grill Gate."),
    "verhalen/ring-h5": (
        "The Black Grill Gate",
        "Chapter 5 of The Lord of the Nibble Ring. The gate to the land of Sauceron is a grate as tall as a house, and it is shut. From his "
        "tower the Eye of Sauceron stares into the valley. You sneak around it: across the Ash Field, the Bare Plain and through Shadow "
        "Lane to the Little Grill Door. Whoever is seen stands at the last rest fire again."),
    "systemen/oog-van-sausron": (
        "The Eye of Sauceron",
        "The one burning Mika eye on the tower behind the Black Grill Gate. Its gaze is a patch of light on the ground that you can see "
        "coming. He is simply hungry."),
    "verhalen/ring-h6": (
        "Mount Fry",
        "The last chapter of The Lord of the Nibble Ring. In the Rookdelta stands Mount Fry: a volcano with a crust of fried batter. You "
        "climb the Winding Path while the mountain throws embers, free three Smoke Guhlings that the Mikas use as a range hood, pull "
        "yourself up the west face with the Elven Rope and let Sam-guh carry you the last stretch. What happens at the top you have to see "
        "for yourself."),
    "verhalen/ring-sausuman": (
        "The Tower of Sauceruman",
        "The extra stop of The Lord of the Nibble Ring. In a black tower full of sputtering machines lives Sauceruman of Many Sauces, a "
        "wizard Mika who also wants a bite of the ring. He does not get one, so he wants to bake one himself. You may fetch the "
        "ingredients."),
    # --- Super Guhrio ---
    "verhalen/super-guhrio": (
        "Super Guhrio",
        "The Big Nether Mika has taken Princess Peachguh 'for a piece of cake'. In his castle in the frying sauce sea you play six levels "
        "in side view, and then a duel. Nobody hurts you: whoever falls or is shoved stands at their last flag again. Nyeg!"),
    "systemen/super-guhrio-spelen": (
        "Playing Super Guhrio",
        "How a level in side view works: the keys, the flags, the Super Nibble and the Fire Pepper, the big chonk coins, the pipes and "
        "Guhshi."),
    "verhalen/super-guhrio-duel": (
        "The duel with the Big Nether Mika",
        "Behind the great gate of the level hall the duel with the Big Nether Mika awaits: three rounds on his grate bridge above the "
        "frying sauce. He only shoves, nyeg: whoever is hit stands at their flag again."),
    "verhalen/pad-guhs-kraam": (
        "Toad-guh's Stall and the rewards",
        "On the forecourt of Big Nether Mika's Castle Toad-guh sits on the counter of his mushroom stall. He tells you that Princess "
        "Peachguh was taken 'for a piece of cake', sells outfits and building blocks for the coins from the levels, and has a golden chonk "
        "cap for whoever finds all eighteen big chonk coins. After the duel Guhshi waits for you in the tower room: one per player. Nyeg!"),
    "systemen/groene-reispijp": (
        "The Green Travel Pipe",
        "The green pipe that really works: stand on it, sneak, and you come out of the nearest pipe of the same color."),
}

TEKST = {
    # ================================================= chonk power =================================================
    ("systemen/vadskracht", "Hoe het werkt"): (
        "How it works",
        "Everything joined by Guh Wire is one setup together. Sources give chonk power, machines use it. A Guh Wheel with a guh in it gives "
        "10 chonk power, a happy guhling 15; a Guh Oven uses 5. A machine may also stand right next to a source: then you need no wire."),
    ("systemen/vadskracht", "Te zwaar? Dan staat alles stil"): (
        "Too heavy? Then everything stops",
        "When your machines together ask for more chonk power than your sources give, the WHOLE setup stops. Nothing goes slower and "
        "nothing gets priority: it is all or nothing. Add a source, or take a machine away. A machine counts as soon as it is switched on, "
        "also when it has nothing to do for a moment, so your setup does not flicker."),
    ("systemen/vadskracht", "Kijken is weten"): (
        "Looking is knowing",
        "You don't need a meter. Look at a Guh Wheel, a piece of Guh Wire or a machine and you read it at once: 'This setup uses 16/20 "
        "chonk power'. When everything has stopped you also read why. Look at a source and you see what it gives; at a machine, what it "
        "uses."),
    ("systemen/vadskracht", "Niet te veel van hetzelfde"): (
        "Not too many of the same",
        "Per setup at most 4 Guh Wheels, 2 Cuddle Generators, 2 Disco Dynamos, 4 Blub Stoves and 1 Glowstar Core count. Put down more "
        "and the extras do nothing ('doesn't count'). The strongest count first."),
    ("systemen/vadskracht", "De snoet van een machine"): (
        "A machine's snoot",
        "Every guh machine has a snoot. When it sleeps, it has no chonk power. When it looks happy, it has chonk power. When it looks "
        "surprised with a round little mouth, it is full: take out what it made."),
    ("systemen/vadskracht", "Guhdraad en redstone"): (
        "Guh Wire and redstone",
        "Guh Wire still gives a redstone signal as long as the setup runs, so a lamp at the end of your wire simply burns. The other way "
        "around no longer works: a lever or a redstone block gives no chonk power. For that you really need a guh, nyeg!"),
    # ================================================= sources =================================================
    ("systemen/vadskrachtbronnen", "Het Guhrad"): (
        "The Guh Wheel",
        "A tamed guh in a Guh Wheel gives 10 chonk power, a happy guhling 15. A guh never gets tired and does not need to eat. The guhs "
        "from the stories each do it their own way: Baltoguh runs harder than anyone, Guhtwo does not run at all (he floats and lets the "
        "wheel turn by itself), the 626 guh counts double, Sam-guh lugs the wheel around backpack and all, and Guhshi flutter-kicks. They "
        "give 20 or 25 chonk power. At most 4 Guh Wheels count per setup."),
    ("systemen/vadskrachtbronnen", "Een blij guhtje rent harder"): (
        "A happy guhling runs harder",
        "Whether a guh is happy is looked at the moment you put it in the wheel, and it stays that way as long as it runs. A guh that "
        "just lay on a Cuddle Generator is happy for another 60 seconds: pick it up from the cushion and put it straight into the wheel. "
        "Look at the wheel and you read whether it runs happily."),
    ("systemen/vadskrachtbronnen", "De Knuffelgenerator"): (
        "The Cuddle Generator",
        "A big pink cushion of 2 by 2 blocks. Tamed guhs within 8 blocks come and lie on it by themselves. Every guh that lies cuddling "
        "gives 3 chonk power, and 8 fit on it (24 chonk power). The embroidered little snoot sleeps when the cushion is empty and looks "
        "surprised when it is full. At most 2 Cuddle Generators count per setup."),
    ("systemen/vadskrachtbronnen", "De Disco-dynamo"): (
        "The Disco Dynamo",
        "A dance floor of 3 by 3 blocks with a turntable. Put a music disc on it (click the floor with the disc) and it keeps playing, "
        "over and over. Tamed guhs within 8 blocks come to dance: every dancer gives 5 chonk power, at most 4 guhs (20 chonk power). "
        "Every disc has its own light show on the floor. With the rare disc 'Ze hangen aan me veh' every dancer gives 1 chonk power more, "
        "and a big heart beats on the floor. An empty hand takes the disc off again. At most 2 Disco Dynamos count per setup."),
    ("systemen/vadskrachtbronnen", "Welke guhs komen er?"): (
        "Which guhs come?",
        "Tamed guhs that are free. Switch 'Wandering' off in the Guh menu and your guh stays on the cushion or the dance floor when you "
        "walk away: that keeps your factory running. A guh that follows you only joins in while you are nearby, and then walks along with "
        "you again. A guh that has to sit only counts when it sits ON the cushion or the floor. Guhs that live in a guh house do not "
        "come: they have their chores and their own little bed. Wild guhs don't either."),
    ("systemen/vadskrachtbronnen", "Het Blubkacheltje"): (
        "The Blub Stove",
        "A little stove with a glass jar on top. Put a Sauce Blubby in a Jar in it and give it a kaasknabbel now and then: then it blubs "
        "6 chonk power together. One nibble keeps it warm for 10 minutes and the little tray holds 16 nibbles. A Nibble Tube or a hopper "
        "may bring the nibbles too. When the blubby is hungry, it gives nothing. Sneaking and clicking with an empty hand takes the blubby "
        "out again, jar and all. At most 4 Blub Stoves count per setup."),
    ("systemen/vadskrachtbronnen", "De Gloeisterkern"): (
        "The Glowstar Core",
        "The glowstar of the Aangebrande Mika in a little cage of salt crystal. It costs one glowstar and then gives 200 chonk power, "
        "forever: no guh, no nibbles, it never runs out. Only 1 counts per setup; a second one falls asleep."),
    ("systemen/vadskrachtbronnen", "De Knabbelbatterij"): (
        "The Nibble Battery",
        "Stores what your setup has left over, up to 18000 chonk power (about 30 minutes of one Guh Wheel), and tops up when your machines "
        "ask for more than your sources give for a moment. Break it and it keeps its charge: you can take a full battery along to another "
        "setup. The item tells you how much is in it."),
    # ================================================= tubes and sensors =================================================
    ("systemen/knabbelbuizen", "Hoe het werkt"): (
        "How it works",
        "A Nibble Tube sticks by itself to every tube next to it and to everything that can hold things: a chest, a furnace, a guh "
        "machine, the Bank Guh, a Nom Hatch. A tube does nothing by itself. The work is done by the Direction Piece: put it with its back "
        "against a chest and it noms one thing out each time and sends it into the tube, the way the arrow points. Every thing picks at "
        "once where it goes: the nearest place where it fits. If it fits nowhere, it simply stays in the chest."),
    ("systemen/knabbelbuizen", "Het Richtingstuk"): (
        "The Direction Piece",
        "Click a chest with the Direction Piece and the arrow points away from the chest: it empties the chest. If you sneak while placing "
        "it, the arrow points into the chest instead. Is it the wrong way around? Sneak and click it with an empty hand and it turns "
        "around. Between two tubes a Direction Piece is a one-way street: nothing gets through against the arrow. Click it with an empty "
        "hand and you read what it is doing. A Direction Piece needs no chonk power. A hopper may also push its things into the back of a "
        "Direction Piece."),
    ("systemen/knabbelbuizen", "Het Filterstuk: sorteren"): (
        "The Filter Piece: sorting",
        "The Filter Piece is a Direction Piece with a picky little guh inside. It wants 2 chonk power and noms with big bites (4 things at "
        "a time instead of 1). Click it and you see its list: nine slots. Click a slot with a thing and that thing is on the list (you keep "
        "it yourself). The Filter Piece then only lets those things through, or everything except those. Put a Filter Piece right in front "
        "of a chest and that chest gets those things first, even when another chest is closer. That is how you sort: a chest per kind with "
        "a Filter Piece in front, and at the end a chest without one, for the rest. Without chonk power the little guh sleeps and lets "
        "nothing through."),
    ("systemen/knabbelbuizen", "Laat er een paar liggen"): (
        "Leave a few behind",
        "A Filter Piece that noms from a chest or the Bank Guh can leave a number of each thing behind: 'Leave behind: 16' means at least "
        "sixteen of them always stay. Handy when you want to feed your machines without your stock running out. Only a Filter Piece can "
        "nom out of a Bank Guh, and only when the bank has the Bottomless Nibble Belly: a plain Direction Piece or a hopper never gets "
        "anything out of it. Putting things in is always allowed."),
    ("systemen/knabbelbuizen", "Op slot met redstone"): (
        "Locked with redstone",
        "A redstone signal locks a Direction Piece or a Filter Piece, just like a hopper. That way a sensor can switch the tube on and "
        "off. Guh Wire does not count: it gives a redstone signal too, but your Filter Piece is simply connected to it."),
    ("systemen/knabbelbuizen", "De Opzuiger"): (
        "The Slurper",
        "The Slurper slurps every loose thing within four blocks to its snoot and keeps it in nine slots. It wants 3 chonk power. Attach a "
        "Direction Piece or a hopper to empty it. When it is full, it looks surprised. Borrowed things and things in the area of somebody "
        "else's guh house it politely leaves where they are."),
    ("systemen/knabbelbuizen", "Niks raakt kwijt"): (
        "Nothing gets lost",
        "A thing that is on its way and no longer fits where it was going comes back and looks for another place, or goes back into the "
        "chest it came from. The same happens when you break a tube while something rolls through it. If you break the Direction Piece "
        "itself, everything that was still on its way drops out onto the ground."),
    ("systemen/sensoren", "De Voorraadmeter"): (
        "The Stock Meter",
        "Put the Stock Meter with its back against a chest, a machine or the Bank Guh (on top or underneath works too). Click it, give it "
        "an example and a number, and it gives a redstone signal as soon as at least that many are inside. Turn it around and it gives a "
        "signal when there are fewer instead. Without an example it counts everything. A comparator reads how far along it is."),
    ("systemen/sensoren", "De Snuffelsensor"): (
        "The Sniff Sensor",
        "A nose that smells who is nearby. Click to choose: guhs, Mikas, players or all of them. Sneak and click to choose how far it "
        "smells: two, four or eight blocks. When it smells someone it gives a signal; a comparator reads how many there are."),
    ("systemen/sensoren", "De Guhklok en de Guhteller"): (
        "The Guh Clock and the Guh Counter",
        "The Guh Clock gives a short redstone tick every so often: every second, every two, five, ten or thirty seconds, every minute or "
        "every five minutes. It can also give a signal as long as it is day, or as long as it is night. The Guh Counter counts the ticks "
        "that come in at its back and gives a tick itself at every so-manieth: put it behind a Guh Clock and you have 'every tenth time'. "
        "Click to choose how far it counts."),
    ("systemen/sensoren", "Vadskracht en uitlezen"): (
        "Chonk power and reading it",
        "Every sensor wants 1 chonk power and has a snoot: it sleeps without chonk power and looks surprised as long as it gives its "
        "signal (then its little lamp is on too). Look at a sensor and you read what it counts, smells or ticks, just like with chonk "
        "power."),
    # ================================================= machines =================================================
    ("systemen/guhmachines", "Zo werkt een guhmachine"): (
        "How a guh machine works",
        "Put the machine down with its snoot toward where it has to work, and give it chonk power: put it next to a Guh Wheel or connect it "
        "with Guh Wire. When its snoot sleeps, it has no chonk power. When it looks happy, it works. When it looks surprised, it is full "
        "or cannot go on: look at it and you read why. Right-click opens its tummy. Nibble Tubes, hoppers and chore guhs can reach it too: "
        "in goes what it needs, out comes what it made."),
    ("systemen/guhmachines", "Vadsmolen"): (
        "Chonk Mill",
        "The big sister of the little guh mill, two blocks tall. Her sails turn on chonk power (5) instead of on wind, so she always mills "
        "just as fast: one nibble grain per second. She mills more than grain: nibble grain becomes 1 nibble flour; a bone becomes 4 bone "
        "meal; sugar cane becomes 2 sugar; a grill skewer becomes 3 grill skewer powder; a blaze rod becomes 3 blaze powder; cobblestone "
        "becomes 1 gravel; gravel becomes 1 sand."),
    ("systemen/guhmachines", "Oogster"): (
        "Harvester",
        "Mows the ripe crop on the field of 5 by 5 in front of its snoot and plants it again at once (8 chonk power). Wheat, carrots, "
        "potatoes, beetroots, kaasknabbel plants, nether wart and cocoa simply grow again; from guh gardens, berries and nibble berries it "
        "only picks the harvest; pumpkins and melons it takes away and the stem stays; sugar cane, cactus and bamboo it cuts down to the "
        "bottom piece. The harvest is in its tummy. When it is full, it leaves the crop standing."),
    ("systemen/guhmachines", "Knabbelaar"): (
        "Nibbler",
        "Nibbles away the block in front of its snoot, about one every two seconds (10 chonk power). It bites as hard as an iron pickaxe: "
        "stone, ore, wood and dirt go, obsidian does not. What it gets loose is in its tummy. It keeps its teeth off chests, machines and "
        "anything with something in it, and also off protected buildings and somebody else's guh house. Nyeg, well brought up."),
    ("systemen/guhmachines", "Neerzetter"): (
        "Placer",
        "Puts the blocks from its tummy down one by one in front of its snoot (5 chonk power). Seeds it only puts on farmland and saplings "
        "only on dirt, just like you. If something is there already, it waits. Together with a Nibbler or a Harvester it makes a little "
        "factory."),
    ("systemen/guhmachines", "Tekentafel en Bouwtekening"): (
        "Drawing Table and Blueprint",
        "A Tinker Machine has to know what to make. You draw that once on a Drawing Table: lay the recipe on the grid, add an Empty "
        "Blueprint (paper with blue dye) and take the drawing out. Drawing only costs the sheet: you get back what lies on the grid. A "
        "drawing is exact: drawn with oak planks means tinkered with oak planks. You can draw over an old drawing. The table needs no "
        "chonk power."),
    ("systemen/guhmachines", "Knutselmachine"): (
        "Tinker Machine",
        "Put a Blueprint on the left and it tinkers what is on it, one every two seconds (10 chonk power). The nine slots in the middle "
        "hold the stock: tubes and chore guhs may only put in what is on the drawing, and not too many of each thing, so that the one never "
        "gets in the way of the other. What is finished comes out on the right, with the empty buckets."),
    ("systemen/guhmachines", "Plantagebak"): (
        "Plantation Box",
        "A box of 3 by 3 full of dirt (6 chonk power). Put a sapling in and within a minute a real tree stands in the middle of the box. "
        "That works with every sapling: ordinary trees, guh blossom, vads wood, palewood, snow guh spruces, the satay and sausage "
        "mushrooms and even plain mushrooms. Kinds that only grow four together (dark oak) get three more by themselves when you put four "
        "in. Simply chop the tree down, or click the box with an axe: then the whole tree comes down in one go, leaves and all, and the "
        "saplings that drop go straight back into the box. Chore guhs from a guh house can do it too. If you chop the bottom log away by "
        "hand first, the box waits with planting until the rest of the trunk is gone too. A bee nest that grew with the tree comes with "
        "it, bees and honey and all."),
    ("systemen/guhmachines", "Bewoners van een Guhhuisje helpen mee"): (
        "The residents of a guh house help out",
        "Put your machines in the chore area of a guh house and give a resident the chore Refill & Empty Machines: then you don't need "
        "Nibble Tubes yet. Put into the machine yourself once what belongs in it and put the stock in the chest next to the house. For a "
        "Plantation Box there is the chore Plantation: the resident chops the tree and takes care of saplings."),
    # ================================================= sauce =================================================
    ("systemen/saus", "Pompen"): (
        "Pumping",
        "Put a Sauce Pump on top of a source: a block of cheese sauce, cheese frying sauce or water that does not flow. Give it 6 chonk "
        "power and it slurps up a bucket every 2 seconds. The source never runs out: a guh pump is polite and never drinks a lake dry. "
        "When its little tank is full and nobody takes it, it looks surprised."),
    ("systemen/saus", "Slangen leggen"): (
        "Laying hoses",
        "A Sauce Hose connects by itself to the hoses next to it and to everything that can hold sauce: a pump, a vat or a machine. It may "
        "go every way, also up and down, and at most 256 hoses long. Nothing stays in the hose itself. A pump pushes its sauce to "
        "everything that hangs on the hose and shares fairly. A machine slurps by itself from the vats and pumps it is connected to, but "
        "only the sauce it needs: so two sauces may happily run through one hose. A Sauce Hose needs no chonk power."),
    ("systemen/saus", "Het Sausvat"): (
        "The Sauce Vat",
        "A Sauce Vat holds 16 buckets of one sauce at a time. Through the little window in its side you see which sauce it is and how "
        "much. Tap from it with an empty bucket, or pour a full bucket in. Milk does not come from a source: you pour it in with a "
        "bucket. When the vat sleeps it is empty; when it looks surprised it is full to the brim. Pick the vat up and the sauce simply "
        "comes along."),
    ("systemen/saus", "De Brouwautomaat"): (
        "The Auto Brewer",
        "The guh brewing kettle that stirs by itself. It wants a bucket of cheese sauce (through a hose or from a bucket), one ingredient "
        "and 3 glass bottles, and makes 3 Guh Potions out of them. Grill skewer powder is not needed: the fire is chonk power (8). It only "
        "starts when everything is there AND the potions fit. The ordinary guh brewing kettle keeps working as always."),
    ("systemen/saus", "De Frituurautomaat"): (
        "The Auto Fryer",
        "The frying pan that fries by itself, in cheese frying sauce and on 8 chonk power. Kaasknabbels become fried kaasknabbels, a guh "
        "fish becomes a fried guh fish. A bucket of sauce is good for 100 pieces. The sauce stays neatly inside: nobody burns themselves. "
        "The ordinary frying pan with Mika's fat keeps working."),
    ("systemen/saus", "De Grillkoolpers"): (
        "The Grill Coal Press",
        "Two blocks tall. On 8 chonk power it presses a bucket of cheese frying sauce and a bucket of water together into one block of "
        "grill coal: the same thing that happens in the Guh Barbecuether when water touches the sauce, but without burnt paws. While it "
        "presses you see the stamp come down."),
    ("systemen/saus", "Met de hand"): (
        "By hand",
        "None of the machines has a screen. Click with an ingredient, bottles or nibbles to put them in, with a bucket to pour sauce in or "
        "scoop it out, and with an empty hand to take what is ready. When nothing is ready, you read above your hotbar how things stand; "
        "sneaking, you take the ingredients back. Look at a machine and you read under your crosshair what is in the tanks and what it is "
        "waiting for. Tubes, hoppers and chore guhs can reach it too."),
    # ================================================= delivery =================================================
    ("systemen/bezorgguhtje", "Zo werkt het"): (
        "How it works",
        "Put down a Scooter Station and give it 4 chonk power (a Guh Wheel with a guh in it is enough). A Delivery Guhling moves in at "
        "once: it parks its little scooter in front of the station. Without chonk power it scoots home and goes to sleep.\n\n"
        "Then put Stop Posts against your chests and machines, up to 96 blocks from the station. A post belongs by itself to the nearest "
        "Scooter Station (at most 8 stops per station). The first post picks up (green sign, arrow up), the second drops off (orange sign, "
        "arrow down). Click a post to switch that, to link it to another station and to set what may come along: put up to nine kinds of "
        "things in the filter, or leave it empty for everything.\n\n"
        "The Delivery Guhling rides the stops in the order of the Scooter Station's screen (the little arrows put a stop earlier or later "
        "in the round). Its backpack holds 9 stacks. At a pick-up stop it only takes what a drop-off stop can really take, so its backpack "
        "does not fill up with things nobody wants."),
    ("systemen/bezorgguhtje", "Ophalen en afleveren"): (
        "Pick Up and Drop Off",
        "Dropping off goes into the chest on the side where the post stands, just like a hopper. So put the post on top of a furnace to "
        "fill it. Picking up works like a hopper under the chest: at a furnace you get what is ready, and the fuel stays. It works with "
        "everything that can hold things: chests, barrels, guh machines, the Bank Guh and the Nom Hatch. A post never picks anything up "
        "out of a Bank Guh (dropping off there works): only a Filter Piece on a Nibble Tube can take things out of your bank. Just like "
        "the machines, a post "
        "does not work at a chest in a protected building or in the chore area of somebody else's guh house: then the Delivery Guhling "
        "skips it."),
    ("systemen/bezorgguhtje", "Nooit iets kwijt"): (
        "Never lose a thing",
        "The things in the backpack are kept by the Scooter Station. Whatever happens to the Delivery Guhling: nothing gets lost. When the "
        "way is blocked or far too long, it hops to the stop with a little poof. If it really goes missing, a new Delivery Guhling stands "
        "at the station a moment later, with the same backpack. Break the Scooter Station and everything in the backpack drops on the "
        "ground. A Delivery Guhling cannot be hurt and hurts nobody. It only works in loaded chunks: a stop that is too far away or not "
        "loaded it skips."),
    ("systemen/bezorgguhtje", "Het fluitje"): (
        "The whistle",
        "The Lighthouse Keeper Guh of the Smoke Guh Lighthouse gives you a Delivery Guhling Whistle. Blow it and the nearest Delivery "
        "Guhling of one of your own Scooter Stations (within 96 blocks) drops everything and comes to you; its backpack opens for you. "
        "Whistle while you sneak and all your Delivery Guhlings go home and start their round again. Give it a kaasknabbel and it scoots "
        "extra fast for a minute. Wahoog!"),
    # ================================================= bank =================================================
    ("systemen/bank-guh-buikje", "Vol is vol"): (
        "Full Is Full",
        "A Bank Guh is chonky, but not bottomless: of each kind (a thing with the same name, enchantments and wear) at most 256 fit in its "
        "tummy. Its screen shows it: under the things it says \"At most 256 of each kind\", a kind that is full gets a red number and "
        "\"256/256\" in the tooltip. What no longer fits you simply keep yourself: it stays on your cursor, in your inventory or in the "
        "crafting grid. Nothing ever gets lost. How many kinds go in has no limit."),
    ("systemen/bank-guh-buikje", "Had je al meer?"): (
        "Did you have more already?",
        "A bank that already held more than 256 of something (from before this update) keeps everything. You can always take it out. "
        "Adding more of it only works again when fewer than 256 are in it, or when you upgrade the bank."),
    ("systemen/bank-guh-buikje", "Het Bodemloos Knabbelmaagje"): (
        "The Bottomless Nibble Belly",
        "The Inventor Guh in the Old Guh Wheel Power Plant (Guh Barbecuether) gives you the Bottomless Nibble Belly after his chores. "
        "Click a placed Bank Guh with it and it holds as much of everything as you like. It stays with that bank forever, also when you "
        "pick it up and put it down somewhere else; an upgraded bank glitters a little. You cannot make it yourself."),
    ("systemen/bank-guh-buikje", "Het Hapluikje en de Banksleutel"): (
        "The Nom Hatch and the Bank Key",
        "The Nom Hatch is a little machine with a little mouth that needs 2 chonk power. Click with a Bank Key on your Bank Guh first "
        "(the key knows it now) and then on the hatch. Everything you put in, by hand, with a hopper, a Nibble Tube, a chore guh or the "
        "Delivery Guhling, lies in that bank at once. However far away it stands, even in another dimension. A hatch belongs to one bank, a "
        "bank may have as many hatches as you like, and the key never wears out. The hatch only noms: nothing ever comes out. It refuses "
        "(and you keep your things) when it has no chonk power, when the bank stands nowhere (you picked it up) or when the bank is full "
        "of that thing. Pick the bank up and put it down somewhere else, and its hatches find it again by themselves."),
    ("systemen/bank-guh-buikje", "Buizen en trechters"): (
        "Tubes and hoppers",
        "Nibble Tubes and hoppers can always put things INTO a Bank Guh (up to 256 per kind). Taking things OUT works in one way only: "
        "with a Filter Piece on a Nibble Tube, against a bank with the Bottomless Nibble Belly. On the Filter Piece you set what may come "
        "out, and with 'leave behind' how many of each thing have to stay in the bank. A hopper underneath, a plain Direction Piece or a "
        "pick-up Stop Post never gets anything out of a Bank Guh, not even out of a bank with the belly: so your stock never runs empty "
        "by accident. A Stock Meter can always count what is inside."),
    ("systemen/bank-guh-buikje", "Klusjes"): (
        "Chores",
        "When a Bank Guh stands in the chore area of a guh house, the residents sort everything into it. When the bank is full of "
        "something, the rest goes to a second Bank Guh in the chore area, else to the chest, else to the door. With Tidy Up, what the bank "
        "has no room for stays in the chest."),
    ("systemen/bank-guh-buikje", "Het Hapluikje in de klus-area van een Guhhuisje"): (
        "The Nom Hatch in the chore area of a guh house",
        "A working Nom Hatch in the chore area of a guh house is a place where the residents bring the things from their chores, when "
        "there is no Bank Guh in the chore area or when it is full."),
    # ================================================= chores =================================================
    ("systemen/guhhuisje", "Klusje: machines bijvullen & leeghalen"): (
        "Chore: Refill & Empty Machines",
        "Residents with this chore keep your guh machines going. They fetch what is ready (ingots from the Guh Oven, Guh Potions from the "
        "Auto Brewer, what the Tinker Machine tinkered, the harvest of the Harvester...) and bring it to the Bank Guh, the Nom Hatch or "
        "the chest, as with every other chore. And they refill the machines from the chest next to the house or from the Bank Guh. They "
        "never refill just like that: a Guh Oven eats anything, and nobody wants a whole chest of oak wood to secretly turn into charcoal. "
        "They only bring what you showed them. Put a little stack in the machine yourself once: the residents remember per slot what "
        "belongs there and keep that slot filled with the same from then on. Put something else in and they remember that. Break the "
        "machine and they have forgotten it. A Tinker Machine needs no showing: it gets exactly what is on its Blueprint. Residents don't "
        "walk back and forth for a single nibble: a slot is refilled when it is empty, at most half full, or when at least eight more fit. "
        "The chore works at the Guh Oven, Tinker Machine, Auto Brewer, Auto Fryer, Chonk Mill, Grill Coal Press, Harvester, Nibbler, "
        "Placer and Slurper, and only at machines the owner of the house put down themselves: the neighbors' ingots stay the neighbors'. "
        "(Guhs)"),
    ("systemen/guhhuisje", "Klusje: plantage"): (
        "Chore: Plantation",
        "When a Plantation Box stands in the chore area, a resident with this chore chops the tree down as soon as it stands: a swing of "
        "its little axe and the whole tree lies flat in one go. The wood, the sticks and the apples go to the chest; a few saplings stay "
        "in the box, which plants the next tree with them by itself. When a box is completely empty, the resident fetches saplings from "
        "the chest (also satay and sausage mushrooms, plain mushrooms and azalea). Trees that grow anywhere else it always leaves "
        "standing: it only chops in Plantation Boxes. The box itself needs chonk power to grow, the chopping does not. (Guhs)"),
    ("systemen/guhhuisje", "Klusje farmen: nog meer oogsten"): (
        "The Farming chore: even more to harvest",
        "The Farming chore harvests more than it used to. Besides every crop and the guh gardens, residents now also pick pumpkins and "
        "melons that hang on their stem (the stem stays; a pumpkin you put down somewhere is not a harvest and stays where it is), sugar "
        "cane (the bottom piece stays), ripe cocoa they can reach (at most two blocks above the ground), nether wart and peppers. From "
        "every peanut and mustard sprout plant they pick one sprout; the plant stays and then rests for five minutes, just like a flower. "
        "In a protected building (the Pepper Garden, the Grill Campground, the Old Guh Wheel Power Plant...) residents never do a chore, "
        "not even when it lies in the chore area of your house: what grows or lies there is not yours."),
    ("systemen/guhhuisje", "Waar gaan de spulletjes heen?"): (
        "Where do the things go?",
        "What your residents gather with their chores they bring to a Bank Guh in the chore area (it sorts, up to 256 of a kind). When "
        "there is no Bank Guh in the chore area, or it is full of something, they put it in a Nom Hatch that stands in the chore area: it "
        "noms everything on to the Bank Guh it is linked to with the Bank Key, however far away that stands. That way your bank can simply "
        "stay at home while your residents work on a field far away. The hatch does have to work: chonk power, linked, and its bank has "
        "to stand somewhere. When that bank is full too, the rest goes into the chest next to the house, and without a chest it ends up "
        "in front of the door. Nothing is ever lost. The house screen says where the things go now, and 'What can be done here?' counts "
        "the working Nom Hatches. Borrowed things never go into a bank. A guh machine never counts as a chest, not even when it stands "
        "right next to the house."),
    # ================================================= the tiers and the practice hall =================================================
    ("systemen/guh-technologie", "De vier stappen"): (
        "The four steps",
        "Tinkering: the Guh Wheel, Guh Wire, the Guh Oven, the Chonk Mill, the Cuddle Generator and the Disco Dynamo you make right away. "
        "Salt: Nibble Tubes, the Filter Piece, the Nom Hatch, the Slurper, the Harvester, the sensors and the Nibble Battery ask for salt "
        "crystal from the Guh Barbecuether. Sauce: the pump, the Sauce Vat, the Auto Brewer, the Auto Fryer, the Grill Coal Press, the "
        "Blub Stove, the Nibbler, the Placer, the Tinker Machine, the Drawing Table and the Scooter Station ask for a grill skewer or blub "
        "cream AND a recipe card from the Inventor Guh (the Plantation Box: the card from the Grill Campground). Glowstar: the Glowstar "
        "Core and The Great Nibble Machine ask for a glowstar from the Aangebrande Mika."),
    ("systemen/guh-technologie", "De projecten uit het questboek"): (
        "The projects from the quest book",
        "Never bake yourself again: a Guh Oven on a Guh Wheel, fed by hoppers or tubes. Everything into the bank by itself: a Nom Hatch at "
        "your field, your mine and your oven, with Nibble Tubes and a Harvester in front. A storage that sorts itself: a tube along your "
        "chests with a Filter Piece in front of every chest. Sauce on tap: a Sauce Pump, hoses and a Sauce Vat at home. Potions that brew "
        "themselves, fries on a conveyor belt and a grill coal line: the three sauce machines. Stone without a pickaxe: a Nibbler in "
        "front of a cobblestone generator. A forest in a box: the Plantation Box. Tinkering without hands: the Tinker Machine with a "
        "Blueprint. And the Delivery Guhling's round for everything that is too far for a tube."),
    ("verhalen/techniek", "De vijf opstellingen"): (
        "The five setups",
        "Setup 1, the loose wire: lay Guh Wire on the yellow tile between the wheel and the Guh Oven. Setup 2, too heavy: two ovens and a "
        "Chonk Mill ask for more than one wheel gives, so everything has stopped; cut a wire at the red wool. Setup 3, back to front: "
        "sneak and click the Direction Piece with an empty hand. Setup 4, the filter: the Filter Piece is set to 'everything except' the "
        "nibble, so only paper rolls through; set it to 'only these'. Setup 5, the hose: lay the Inventor Guh's Sauce Hose in the gap "
        "between the pump and the Sauce Vat."),
    ("verhalen/techniek", "Voor iedereen opnieuw"): (
        "Again for everyone",
        "The five old guhs in the wheels belong to nobody: you cannot take them out. A setup counts for every player who stands by it and "
        "is at that step, so you can do it together. When everyone walks away, the setup breaks again by itself after a few seconds for "
        "the next player. The building itself is protected: you can only take away the wires that are pointed out and the piece you laid "
        "in a gap."),
    ("verhalen/techniek", "De receptkaarten"): (
        "The recipe cards",
        "There are three recipe cards: sauce and hoses, nibbling machines and the Delivery Guhling. You put a card in the top left of the "
        "crafting grid and it stays there. Whoever lost a card sneaks and clicks the Inventor Guh: he sells them again for a few "
        "kaasknabbels."),
    ("verhalen/knabbelmachine", "Wat de Uitvinder-guh nodig heeft"): (
        "What the Inventor Guh needs",
        "The foundation: 256 stones (cobblestone, blackstone or cobbled deepslate) and 128 logs. The boiler: 96 grill coal and 128 fried "
        "kaasknabbels. The stomach: 128 nibble flour and 64 Guh Wire. The snoot: 12 Guh Potions (whichever you like) and 128 glass. The "
        "glowstar heart: 1 glowstar and 256 kaasknabbels. You may bring everything in portions: the Inventor Guh takes what he needs from "
        "your pockets and counts it for you alone. In the Guhdex (tab Tales) you see how much is still to come."),
    ("verhalen/knabbelmachine", "Elke dag een perfecte knabbel"): (
        "A perfect nibble every day",
        "Click the little tray under the mouth of your own machine: one perfect nibble a day. It fills you up completely and gives "
        "regeneration and speed for a moment. The Nibble Machine Statuette you can put down at home; click it and it nibbles."),
    # ================================================= salt, fossil =================================================
    ("systemen/zoutkristal", "De kristalader"): (
        "The crystal vein",
        "The crystal vein in the cave of the Salt Crystal Mine never runs out. Every player has a stock of their own in it: at most 24 "
        "crystals, and every half minute one grows back. A pickaxe chops one crystal out at a time, the Salt Crystal Pickaxe two. The "
        "block itself always stays, so it does not matter who was there before you. The vein only gives salt once you have cleared the "
        "track for the Mineworker Guh."),
    ("systemen/zoutkristal", "Zoutkristalerts"): (
        "Salt crystal ore",
        "In the rocks of the Guh Barbecuether there is salt crystal ore: charcoal stone with crystals in it. That ore only appears in "
        "pieces of world where nobody has ever been. Whoever lives in a Guh Barbecuether that was already explored therefore gets their "
        "salt from the crystal vein."),
    ("systemen/zoutkristal", "Wat maak je ervan"): (
        "What you make of it",
        "Four salt crystals make a salt crystal block (it gives a soft light), two make two tufts of little salt crystals. And every "
        "recipe of the Salt step of Guh Technology needs salt crystal."),
    ("verhalen/archeoloog", "Elke dag een vondst"): (
        "A find every day",
        "From the moment you have all five bones, every spot of bone sand holds something small for you once a day: a few kaasknabbels, a "
        "little bone, a charred guh bone, sometimes a salt crystal or some gold nuggets. The Guh Dusting Brush never wears out."),
    ("verhalen/mijnwerker", "Puin dat terugvalt"): (
        "Rubble that falls back",
        "The rubble you chop off the track falls back by itself after a minute, so that the next player has something to clear too. Your "
        "five chunks simply stay counted."),
    # ================================================= Skewer Keep, grill palace =================================================
    ("bouwwerken/spiesburcht", "De Wachter-guh"): (
        "The Guard Guh",
        "Next to the statue in the hall stands a sentry box with the Guard Guh. He and his box also appear in Skewer Keeps that already "
        "existed before he was there: as soon as you come near, he stands there. On every bridge a bridge brazier stands on a pedestal. A "
        "fire that you light only burns for you: another player still sees it cold and can light it themselves. That way everyone on the "
        "server can keep the watch at the Skewer Keep, as often as there are players."),
    ("bouwwerken/spiesburcht", "Mikakruid in het tuintje"): (
        "Mika Weed in the little garden",
        "Mika Weed is a purple, prickly weed that the Mikas sow in the peanut sauce garden. It only stands there for whoever has to weed "
        "it for the Guard Guh: click a tuft to pull it out. There are six."),
    ("bouwwerken/mika_grillpaleis", "De Knuffelmaker-guh"): (
        "The Plushie Maker Guh",
        "Next to the three cages with stolen plush guhs on the first floor is the sewing corner of the Plushie Maker Guh, with a loom and "
        "a little sign from the Mikas. It also appears in palaces that already existed. A plushie that you free is out of its cage for "
        "you alone: after that you see it sitting with the Plushie Maker Guh. Being caught while sneaking costs nothing: a Nether-Mika "
        "only gives you a shove. A Sluipknabbeldrankje makes you invisible to their gaze, and a Mika that sniffs at a vads ingot is not "
        "paying attention for a moment."),
    ("bouwwerken/mika_grillpaleis", "De lege troon"): (
        "The empty throne",
        "The throne of the Big Nether Mika stays empty. According to the Plushie Maker Guh he lives in a castle of his own, somewhere in "
        "the sauce sea."),
    # ================================================= the three Mika palaces =================================================
    ("bouwwerken/mika_woonblokken", "Wat is het?"): (
        "What is it?",
        "The apartments of the Nether-Mika's: three apartment blocks of grill iron around a courtyard, on a base in the frying sauce sea. "
        "There are walkways with doormats, balconies with striped awnings, clotheslines, a smoking boiler house and a sauce tower. In the "
        "apartments live Mikas that harm nobody: no monsters spawn here and you cannot break anything."),
    ("bouwwerken/mika_woonblokken", "De weg vinden"): (
        "Finding your way",
        "You come in through the gate at the front. In the two corners of the courtyard stands a stairwell with a red pointed roof: the "
        "western one goes all the way to the top walkway (Granny Mika sits there), the eastern one to the roof garden of the low block."),
    ("bouwwerken/mika_stal", "Wat is het?"): (
        "What is it?",
        "A red barn with a big door under a Sausage Piglet sign, a hayloft, six stalls and a muddy meadow with a little shelter, hay "
        "bales and a cart. The five Sausage Piglets of the stable stay with the stable: petting is always fine, taking them along is not."),
    ("bouwwerken/mika_brugpaleis", "Wat is het?"): (
        "What is it?",
        "A long bridge on piers, with the toll house halfway: a tower whose front is one big Mika face. The open mouth is the gate. Past "
        "the toll house five rows of planks are missing (under them hangs a scaffold with a ladder, so you never fall far) and at the end "
        "stands the bell tower with the toll bell."),
    ("bouwwerken/mika_brugpaleis", "Mika-kwaliteit"): (
        "Mika Quality",
        "The planks you lay drop out again after a minute. That is how it should be: then the next player finds the gap again too. If you "
        "fixed the bridge together, it counts for everyone who laid a row."),
    ("diertjes/worstzwijntje", "Als boerderijdiertje"): (
        "As a farm animal",
        "A Sausage Piglet from the Stable Hand Guh's basket is a farm animal, just like the guh sheep and the guh cow. Pet it (empty "
        "hand), brush it (guh brush) and feed it (nibble feed, or a filled guh feeding trough). Two of the three on one day and it is "
        "happy: then it sniffs something up once a day, mostly a few kaasknabbels, sometimes a little mushroom or something else from the "
        "Barbecuether. With nibble feed two Sausage Piglets get a baby."),
    # ================================================= sauce animals =================================================
    ("verhalen/sausloper", "Zo gaat het"): (
        "How it goes",
        "Talk to the Caretaker Guh: you get peanut sauce on a stick. Hold the stick and lure a Sauce Strider from the sauce basin to him. "
        "Then feed a Sauce Strider of the stable three peanut sprouts (you get the first three). Then ask for your test ride: a saddled "
        "Sauce Strider comes to the jetty, specially for you. Get on and ride through the four little gates; the next gate glitters. Back "
        "at the Caretaker Guh you get a saddle."),
    ("verhalen/sausloper", "Met zoveel als je wilt"): (
        "With as many as you like",
        "Everyone does the lesson for themselves. The Sauce Striders of the stable belong to nobody: you may feed and pet them, but you "
        "cannot take them along. For the test ride every player gets a Sauce Strider of their own, so you never have to wait for each "
        "other. You cannot fail: no clock is running and if you get lost you are back at the start in no time."),
    ("verhalen/sausloper", "Nog een rondje"): (
        "One more lap",
        "After the lesson the Caretaker Guh times every lap. Your fastest time is remembered. Reach the finish within thirty seconds and "
        "you are a real Sauce Racer. Tip: right-click with the stick for a little sprint."),
    ("systemen/sausdieren", "De Sausloper"): (
        "The Sauce Strider",
        "A round guh on two very long legs, with a quiff of fries. It walks on cheese frying sauce as if it were a sidewalk and never "
        "burns. On dry land it shivers and walks slowly, except on something warm like glowing coal. Wild Sauce Striders come up out of "
        "the sauce sea. After the lesson in the Sauce Strider Stable you tame one with peanut sprouts or a peanut sauce puddle (one in "
        "three per bite). Put a saddle on your own Sauce Strider, click to get on and hold peanut sauce on a stick: it walks where you "
        "look. As long as you ride, the sauce can do nothing to you. Sneak and click to make it wait."),
    ("systemen/sausdieren", "Pindasaus aan een stok"): (
        "Peanut Sauce on a Stick",
        "You get it from the Caretaker Guh. Lost it or used it up? Make a new one from a fishing rod and a peanut sauce puddle. Right-click "
        "while riding and your Sauce Strider breaks into a little sprint; that does cost a lick of peanut sauce."),
    ("systemen/sausdieren", "Het Sausblubje"): (
        "The Sauce Blubby",
        "A bouncing blub of sauce with a little crust, in three sizes. It harms nobody and you cannot hurt it. Give a big or medium blubby "
        "a cuddle (right-click with an empty hand) or a kaasknabbel and it splits into two smaller blubbies. Every time a dollop of blub "
        "cream is left behind. A small blubby grows a size again from three nibbles. That way a pen of blubbies keeps blubbing as long as "
        "you feed and cuddle."),
    ("systemen/sausdieren", "Blubje in een potje"): (
        "Blubby in a jar",
        "A small Sauce Blubby fits in a glass bottle: right-click and you have a Sauce Blubby in a Jar. Click a block with the jar to set "
        "it free again. The Inventor Guh uses such a jar for his Blub Stove. No wild blubby to be found? Whoever rode the test ride of the "
        "Sauce Strider Stable gets a Sauce Blubby in a Jar from the Caretaker Guh every day (ask for it, as long as you have none on "
        "you)."),
    ("systemen/sausdieren", "Blubroom en het Stuiterdrankje"): (
        "Blub cream and the Bouncy Potion",
        "Stir blub cream through cheese broth in the guh brewing kettle and tap a Bouncy Potion. For three minutes falling does not hurt: "
        "you bounce up again, a little less high each time. Whoever sneaks simply lands. Blub cream is also in recipes of Guh Technology."),
    # ================================================= campground, market =================================================
    ("verhalen/camping", "Je eigen tent"): (
        "Your own tent",
        "At the gate of the campground lie four campsites with a sign saying FREE. Click one with the tent bag and your tent stands. Then "
        "hammer in the four stakes by clicking the iron pegs. After a few minutes every tent goes back into its bag by itself, so that the "
        "spot is free for the next player. What you already did simply stays counted."),
    ("verhalen/camping", "Marshmallows roosteren"): (
        "Roasting marshmallows",
        "Roasting is a little game of letting go in time. Hold right-click with the Roasting Stick on a burning campfire: the marshmallow "
        "goes from cold to warm to golden brown to black. Let go when it says GOLDEN BROWN (you also hear a little bell). Too early or too "
        "late costs nothing: you simply try again. The stick works above every burning campfire, at home and by day too, and you need "
        "marshmallow nibbles for it."),
    ("verhalen/camping", "Het kampvuur"): (
        "The campfire",
        "The big campfire burns for two minutes after wood was thrown on it and then goes out by itself. Whoever already celebrated the "
        "campfire party pokes it up again with a click. At home it is a decoration block: light it with a flint, put it out with a "
        "shovel."),
    ("verhalen/camping", "De kampeerguhs"): (
        "The camper guhs",
        "The five guhs of the campground belong to the campground. They love to talk, but you cannot tame them or take them along. When "
        "the campfire burns they come and sit by it."),
    ("verhalen/ruilmarkt", "Afdingen"): (
        "Haggling",
        "The Market Master Mika starts at 30 nibbles and has to come down to 10. Every round he does one of three things. If he brags "
        "about his market or his hat, pay him a compliment. If he sighs that it is so quiet, make a low offer. If he growls that this is "
        "his last offer, pretend to walk away. A right answer takes 7 nibbles off the price, a wrong answer costs him patience. After "
        "three mistakes he sends you away with a shove and you start again. It never costs you real nibbles."),
    ("verhalen/ruilmarkt", "De nepvads vinden"): (
        "Finding the fake chonk",
        "Of the five stacks of chonk in the Weigh House one is fake, and fake chonk is lighter. Click a stack to put it on the left pan "
        "and a second one for the right pan: the scale dips to the heavy side. If it stays level, both stacks are real. Click the stack "
        "that is fake with the Seal of Approval. Which stack is fake differs per player. Stamp the wrong one and the swindler swaps the "
        "stacks around and you have to weigh again."),
    ("verhalen/ruilmarkt", "Ruilen op de markt"): (
        "Bartering at the market",
        "The three Stall Mikas barter at once: give an ingot of vahoege vads and you get a surprise from the same bag as with the wild "
        "Nether-Mika's. They do not come at you, not even without vads gear. Whoever finished the questline gets a second little present "
        "with every barter, at the market and with wild Nether-Mika's. Every day you can haggle with the Market Master for the deal of "
        "the day."),
    ("systemen/tentdoek", "Maken en bouwen"): (
        "Making and building",
        "Three blocks of wool and a string give four blocks of Tent Canvas in the color of the wool (white wool gives cream, light blue "
        "wool blue). From Tent Canvas you make sloped pieces and slabs, just like from stone. Put the sloped pieces against each other "
        "for a pointed roof and lay a slab on the ridge. The Camp Boss Guh and the Market Master Mika sell it too."),
    # ================================================= lighthouse, peppers =================================================
    ("verhalen/vuurtoren", "Zo gaat het"): (
        "How it goes",
        "Talk to the Lighthouse Keeper Guh in front of the tower. Bring him four glowing coal grit (break a chunk of glowing coal): he "
        "presses a Lamp Ember from it and lends you his Signal Lantern. Go into the tower and climb the spiral stairs, round after round, "
        "up into the glass lamp room. Click the lamp with the Lamp Ember: floop, it burns, and the foghorn toots."),
    ("verhalen/vuurtoren", "De verdwaalde Rookguhs"): (
        "The Lost Smoke Guhs",
        "As soon as your lamp burns, three thin, small Rookguhs float around the tower. They are your Rookguhs: above each of them you see "
        "a little star. Walk up to them with the Signal Lantern in your paw and they float after you. If one falls behind or gets stuck "
        "behind a rock, it stands next to you again with a little puff of smoke. Close to the tower it sees the light: it eats itself "
        "round and pink and floats home. When you have brought three home, go back to the Lighthouse Keeper Guh. Feeding six kaasknabbels "
        "works too: then it flies home as well."),
    ("verhalen/vuurtoren", "Met zoveel als je wilt"): (
        "With as many as you like",
        "Everyone does it for themselves. The lamp burns as long as someone who lit it is nearby, and goes out again when they are gone. "
        "That way every new player finds a dark tower. Every player has lost Rookguhs of their own; somebody else's do not follow you. You "
        "cannot fail, and a Lamp Ember that went missing or a lost Signal Lantern you simply get again."),
    ("verhalen/vuurtoren", "Daarna"): (
        "Afterwards",
        "The lamp floops on as soon as you come near. With grill iron bars, glass and glowing coal you make a Lighthouse Lamp of your own "
        "for at home, which does the same. If you lost your whistle, the Lighthouse Keeper Guh carves a new one for you every day."),
    ("verhalen/pepertuin", "Zo gaat het"): (
        "How it goes",
        "Talk to the Pepper Grower Guh in front of the greenhouse: you get three pepper seeds. In the greenhouse, left of the path, stand "
        "three grow boxes, with ash dirt, glowing coal and peanut sauce nylium. Plant a seed in every box. After 45 seconds they are ripe, "
        "also when you walk away for a moment. Click the box to pick; you get two peppers and your seed back. When you have picked all "
        "three kinds, the Pepper Grower Guh gives you grill skewer powder, a bucket of cheese sauce and three bottles. At the back of the "
        "greenhouse stands his guh brewing kettle: fire it up, pour the sauce in, stir a red or a pink pepper through it and fill a "
        "bottle. Then let him taste."),
    ("verhalen/pepertuin", "Met zoveel als je wilt"): (
        "With as many as you like",
        "The plants in the grow boxes are yours alone. You see your own plant grow, somebody else sees theirs, in the same box. Nobody can "
        "pick your peppers and you never have to wait for each other. The show beds on the other side of the path are everyone's: you "
        "pick a ripe plant with a right-click and it grows back by itself."),
    ("systemen/pepers", "Drie pepers van één plant"): (
        "Three peppers from one plant",
        "Plant pepper seeds on ash dirt and you get the green Nyeg Pepper: mild, to nibble on. On glowing coal grows the red Wahoog "
        "Pepper: hot! On peanut sauce nylium grows the pink Candy Pepper: sweet. The plant needs no light and no water. You pick a ripe "
        "plant with a right-click (two or three peppers), and then it grows on. Dig up a ripe plant and you get the peppers and one or two "
        "seeds."),
    ("systemen/pepers", "Onder glas"): (
        "Under glass",
        "Outside every growth step takes about a minute. When there is glass somewhere above the plant, it goes three times as fast. So "
        "building a greenhouse pays off. Nothing solid may sit between the plant and the glass, and the glass may be at most eight blocks "
        "higher."),
    ("systemen/pepers", "De peperdrankjes"): (
        "The pepper potions",
        "Stir a Wahoog Pepper through cheese broth in the guh brewing kettle for the Pepperfire Potion: for three minutes you chop and dig "
        "much faster, with little flames from your snoot (Pepper Breath: you cannot freeze). A Candy Pepper gives the Peppersweet Potion: "
        "you heal for half a minute and get two extra hearts for two minutes. Eating a raw Wahoog Pepper works too: it makes you run very "
        "fast for ten seconds. With smoke from your ears."),
    # ================================================= the Nibble Ring: the core =================================================
    ("verhalen/knabbelring", "Hoe begin je?"): (
        "How do you start?",
        "First light the grill portal (the Grillguh's quest). After that Guhdalf stands with his cart at the big barbecue pit in the "
        "Guhmension. The grill portal to the Guh Barbecuether works for nobody any more until you have done the whole first chapter; "
        "going back is always possible."),
    ("verhalen/knabbelring", "Zo volg je het verhaal"): (
        "How to follow the story",
        "In the top left of the screen it says what you have to do now (you can switch it off in the Guhdex). In the Guhdex, tab Tales, is "
        "the Journey Map with a check mark per step and 'You are here'. The Super Compass points to 'My Story'. Sam-guh says it too when "
        "you click him. The chapters in the quest book open one by one."),
    ("verhalen/knabbelring", "Niemand doet je pijn"): (
        "Nobody hurts you",
        "The Eye, the Nine and everything you meet on the way only shoves: you then stand at your last rest stop again. You never lose "
        "things."),
    ("verhalen/knabbelring", "Samen spelen"): (
        "Playing together",
        "Everyone has their own progress, their own ring and their own Sam-guh. Whoever is further along may walk with a friend through "
        "everything their own story has already reached, but does not solve that friend's puzzles for them."),
    ("verhalen/knabbelring", "Na het verhaal"): (
        "After the story",
        "You get the title Ring-Bearer, four outfits, the statuette of the Eye, Sam-guh to take home (click him, once) and Smikagol as a "
        "buddy who catches fish for you near water. In the Nibble Shire there is a Party Nibble every day."),
    ("verhalen/knabbelring", "Bekende gezichten"): (
        "Familiar faces",
        "On the way, seven times somebody from an older story walks through the picture: in Guhvendell, at the mine, at the mirror, in "
        "the tower, on the mountain, at the party and in the castle of Super Guhrio. They are short scenes of a few seconds. You see each "
        "of them once, also when you have not done that older story yet, and you can watch them again in the Guhdex (tab Tales, Watch "
        "Again). And in the great hall of the mine, do watch who trudges onto the bridge there. Very. Slowly."),
    ("systemen/gaven-van-guhladriel", "Lichtflesje"): (
        "Light Phial",
        "Right-click: a flash that blinds the Skelly Mika Riders around you for eight seconds and blows smoke away. In your hand it gives "
        "light that walks along with you."),
    ("systemen/gaven-van-guhladriel", "Elfenmanteltje"): (
        "Little Elven Cloak",
        "When you have it with you, duck and stand still: after a short second you look like a rock and the Eye and the riders look past "
        "you. Move or stand up and you are yourself again."),
    ("systemen/gaven-van-guhladriel", "Elfentouw"): (
        "Elven Rope",
        "Look at an Elven Rope Hook within 24 blocks and right-click: the rope pulls you to it. Duck to let go. After the story you make "
        "hooks yourself (two iron and a string)."),
    # ================================================= chapter 1 =================================================
    ("verhalen/ring-h1", "Waar is het?"): (
        "Where is it?",
        "In new land of the Guhmension every big barbecue pit comes with a Nibble Shire: hill holes with round doors, a party meadow with "
        "a party tree, Sam-guh's vegetable patch and Guhdalf's camp. At a big barbecue pit that already stood there before this update "
        "came, only Guhdalf's camp stands (his tent, his cart and the party table) on a free spot next to the pit. The story is the same "
        "in both places. The Super Compass points with 'My Story' to the nearest Guhdalf."),
    ("verhalen/ring-h1", "De stappen"): (
        "The steps",
        "1. Talk to Guhdalf (the Grillguh's barbecue has to burn first). 2. Three chores: set off a rocket from the crate on the cart, set "
        "the party table and invite Sam-guh. 3. Talk to Guhdalf again: the farewell party, with the Nibble Ring as a present. 4. Ask "
        "Sam-guh to come along. 5. Click the three provisions crates (sausage, cheese, nibbles). 6. Walk with Sam-guh to the grill portal."),
    ("verhalen/ring-h1", "Met zoveel spelers als je wilt"): (
        "With as many players as you like",
        "Everything is per player: the crate, the table and the provisions crates never run out, everyone sees their own Sam-guh, and the "
        "portal only opens for whoever did the chapter themselves. The way back out of the Guh Barbecuether is never shut."),
    ("verhalen/ring-h1", "Daarna"): (
        "Afterwards",
        "After the whole story Guhdalf and the party table give one Party Nibble every day. The fireworks crate always works, for "
        "everyone. In the Nibble Shire live four Shire Guhs who love to talk about second breakfast."),
    # ================================================= chapter 2 =================================================
    ("verhalen/ring-h2", "Hoe kom je er?"): (
        "How do you get there?",
        "The chapter begins as soon as you stand in the Guh Barbecuether, through the grill portal, after chapter 1: first you get to see "
        "the story card with the little map. Guhvendell stands exactly once in every world, in the Worstenwoud, a few hundred blocks from "
        "the middle of the Guh Barbecuether. The Super Compass (My Story) and Sam-guh point the way. Until you have finished chapter 1 you "
        "only see a wall of smoke: Guhdalf's Veil."),
    ("verhalen/ring-h2", "De stappen"): (
        "The steps",
        "1. Walk into Guhvendell through the gate in the south. 2. Talk to Guhrond on the steps of the hall. 3. Get to know Araguh (at "
        "the gate: duck and click him once more, he wants to see whether you can sneak), Leguhlas and Gimguh (in the garden, at their "
        "pile of nibbles), Boromika (in the hall, at the shards of Nibsil) and Merry and Pippguh (in the kitchen; they snack one "
        "kaasknabbel from your bag if you have one). 4. Ring the bell in the council ring: the council begins. 5. Tell Guhrond that you "
        "will take the ring. 6. Talk to Guhdalf when you are ready to leave."),
    ("verhalen/ring-h2", "Wat krijg je?"): (
        "What do you get?",
        "Provisions from Guhrond (six kaasknabbels and two stews, once per player) and the advancements The Council of Guhrond and The "
        "Fellowship. At the house stands a Rest Fire: walk past it and Guhvendell is your rest stop."),
    ("verhalen/ring-h2", "Samen spelen"): (
        "Playing together",
        "Everything is per player. The bell only starts the council for whoever is at that step; a friend who is further along or less "
        "far may simply walk along. You only see the company at the moments of your own story: first all over the house, after the bell "
        "in the council ring, and after the chapter only Guhrond still lives there. Nobody in Guhvendell does anything to you and nothing "
        "can be broken."),
    ("bouwwerken/guhvendel", "Het Laatste Knusse Huis"): (
        "The Last Cozy House",
        "A horseshoe of rock with three waterfalls of cheese sauce. On the west bank the hall: white, with pointed windows, a steep roof "
        "of green copper and a round tower. On the east bank the council ring: a round terrace in a wreath of columns, with the stone "
        "table on which the ring lies and Guhrond's high seat. Between them the narrow arched bridge without a railing. In the southeast "
        "the kitchen with the buffet and the Rest Fire, in the southwest the garden with guh blossom trees and the nibble pile of Leguhlas "
        "and Gimguh."),
    # ================================================= chapter 3 =================================================
    ("verhalen/ring-h3", "De poort"): (
        "The gate",
        "The west gate only opens for whoever solves the riddle. Above the door it says in glowing runes: Say Nyeg and Enter. Type nyeg "
        "in the chat near the gate, or click the runes and pick the right answer. Guhdalf stands by and gives clearer and "
        "clearer hints."),
    ("verhalen/ring-h3", "De Hal van de Hefbomen"): (
        "The Hall of Levers",
        "Four levers under four signs: cheese, sausage, sauce and nibble. The rhyme on the stone in the middle gives the order: first the "
        "sausage, then the cheese, the sauce on top and the nibble for dessert. One wrong lever and you start again. Every player solves "
        "it themselves; the portcullis then closes again by itself for the next one."),
    ("verhalen/ring-h3", "De put en de geheime doorgang"): (
        "The well and the secret passage",
        "In the guard room Pippguh drops a little bucket into the well. After that you hear drums. Gimguh knows a dwarf door: knock three "
        "times on the rune of what Durguh liked best. It says so on his tomb: cheese."),
    ("verhalen/ring-h3", "De Barbecuerog"): (
        "The Barbecuerog",
        "In the Hall of Pillars the Barbecuerog wakes up: a horned demon of charcoal and glowing embers, ten blocks tall, with a burning "
        "mane, wings of smoke, a sword of fire and a whip of bratwursts knotted together. He does not hurt you: his stomp shoves you away "
        "and his whip puts you back at your last rest fire. You never lose anything. Every player has a Barbecuerog of their own."),
    ("verhalen/ring-h3", "Het Brokkelpad en de brug"): (
        "The Crumble Path and the bridge",
        "Across the Chasm first runs the Crumble Path: stone that drops away a second after you step on it, between pillars that stay. "
        "After a few seconds it grows back. If you fall, you stand at the rest fire again. Then comes the Bridge of Nibble-dûm. On the "
        "other side the big scene begins; you can watch it again later in the Guhdex."),
    # ================================================= chapter 4 =================================================
    ("verhalen/ring-h4", "Stap voor stap"): (
        "Step by step",
        "1. Walk to the tree city (the Super Compass points to 'My Story'); at the gate you get the story card of the chapter. 2. Talk to "
        "Leguhlas at the gate. 3. Climb the spiral stairs around the Great Skewer and talk to Guhladriel in her hall. 4. Rest at the Rest "
        "Fire on the guest platform (the tree left of the gate). 5. Right-click the Mirror in the green dell. 6. Talk to Guhladriel at the "
        "mirror: the three gifts. 7. Step into the elven boat at the jetty and stay seated until the other side."),
    ("verhalen/ring-h4", "De boomstad"): (
        "The tree city",
        "Caras Guhladhon lies in a clearing with white paths. The Great Skewer in the middle carries Guhladriel's hall; three other trees "
        "carry platforms (the guest platform, the storage platform and the lookout) with rope bridges between them. In the whole city you "
        "take no fall damage: Guhladriel's blessing."),
    ("verhalen/ring-h4", "De Guhduin en de Arguhnath"): (
        "The Guhduin and the Arguhnath",
        "The elven boat sails by itself; you cannot steer and you cannot get out. Two players fit in it; whoever clicks the boat within "
        "three seconds sails along. The river is cheese sauce and does nothing. Halfway stand the two Guh Kings with their little paw "
        "raised. After the chapter you may sail as often as you like, and at the landing lies a boat that sails back to the city."),
    ("verhalen/ring-h4", "Een gave kwijt?"): (
        "Lost a gift?",
        "Talk to Guhladriel (after the chapter she sits in her hall again): she gives every gift you no longer have on you again."),
    # ================================================= chapter 5 =================================================
    ("verhalen/ring-h5", "Waar is het?"): (
        "Where is it?",
        "In the Asdal of the Guh Barbecuether, a few hundred blocks' walk from Guhladriel's tree city. There is only one per world. Until "
        "you have finished chapter 4, Guhdalf's Veil hangs around it. Your Super Compass ('My Story') and Sam-guh point the way; the "
        "entrance is the cave mouth at the camp."),
    ("verhalen/ring-h5", "Boromika"): (
        "Boromika",
        "At the campfire waits Boromika, a Mika who truly wants to help. Once the ring gets the better of him: he grabs for it, Sam-guh "
        "jumps in between, and he is deeply ashamed. After that he stays at the camp to keep watch."),
    ("verhalen/ring-h5", "Smikagol"): (
        "Smikagol",
        "The thief at the provisions is Smikagol. He swears on his 'chonkie' that he will show the way. From now on he runs ahead of you "
        "and waits at every hiding place. Click him and he says what you have to do here."),
    ("verhalen/ring-h5", "Het Asveld"): (
        "The Ash Field",
        "The light of the Eye moves back and forth across the field. Hide behind a little wall, under a fallen grate or in a split rock "
        "(something has to stand between you and the Eye) until it has passed, and then run to the next hiding place. Half a second in the "
        "light and you stand at your last rest fire again."),
    ("verhalen/ring-h5", "De Kale Vlakte"): (
        "The Bare Plain",
        "Nothing to sit behind. If you have the Little Elven Cloak with you, duck and stand still when the light comes: as a rock the Eye "
        "does not see you."),
    ("verhalen/ring-h5", "De Schaduwlaan"): (
        "Shadow Lane",
        "Under the wall the Eye cannot look. First there is smoke: let the Light Phial flash and it blows away (for you, forever). Then "
        "two riders of the Nine ride back and forth: a flash blinds them for eight seconds, and they ride past a rock."),
    ("verhalen/ring-h5", "Het Wachthek"): (
        "The Guard Fence",
        "Three Grill Guards look down the lane. They are Mikas, and Mikas see nobody who wears the Nibble Ring. Put it on at the skull "
        "post, walk through the fence and take it off again at once: after four seconds the Eye has you, and if you put it on too early "
        "the riders smell it."),
    ("verhalen/ring-h5", "Het Roosterpoortje"): (
        "The Little Grill Door",
        "The little door is locked and the Nine are coming. Then Guhdalf the White stands on the wall: he blinds the riders and draws the "
        "gaze of the Eye to himself. The door opens (only for you: a friend who is not that far yet sees bars) and you run through the "
        "passage to the fire behind the wall."),
    ("systemen/oog-van-sausron", "Zo werkt zijn blik"): (
        "How his gaze works",
        "The Eye looks at one place at a time: the patch of light with the eye in it and little flames around it. When somebody stands on "
        "the Ash Field or the Bare Plain, the patch moves back and forth along the route there. The Eye only sees you when you stand in "
        "the patch AND nothing stands between you and the tower. A rock under the Little Elven Cloak he does not see. The Nibble Ring he "
        "does: he feels it everywhere in the valley."),
    ("systemen/oog-van-sausron", "Na het verhaal"): (
        "After the story",
        "Whoever finished the story he leaves alone: he has had his piece of the ring. When nobody else is around, he closes his eye and "
        "sleeps. The statuette you get blinks, gives a little light and has thoughts of its own when you click it."),
    # ================================================= chapter 6 =================================================
    ("verhalen/ring-h6", "Waar staat de berg?"): (
        "Where is the mountain?",
        "One Mount Fry per world, in the Rookdelta, a few hundred blocks beyond the Black Grill Gate. Until your story is there you only "
        "see Guhdalf's Veil. The mountain stands on an ash plain just above the frying sea; on its four sides the plain runs to the edge, "
        "and you can walk there or build a little bridge. The base camp with the Rest Fire lies on the south side."),
    ("verhalen/ring-h6", "De klim"): (
        "The climb",
        "Four stretches, each with a Rest Fire: the Winding Path (walking, dodging embers), the west face (two hooks with the Elven Rope: "
        "under every hook hangs a little blue lamp, and you throw at the post with the little blue lamp), the narrow path to the east side "
        "with the last hook, and the last stretch to the Crack of Fry. On every ledge stands a cage with a Smoke Guhling: click the lock. "
        "Every player frees their own Smoke Guhlings; a friend cannot do that for you."),
    ("verhalen/ring-h6", "Niets doet pijn"): (
        "Nothing hurts",
        "Embers only shove. Falling and ending up in the fryer does no damage: after a long fall or a dive you stand at your last Rest "
        "Fire again. On the whole mountain fire does not bother you."),
    ("verhalen/ring-h6", "De ring wordt zwaar"): (
        "The ring gets heavy",
        "The higher you get, the slower you walk. After the third Smoke Guhling the ring is so heavy that you almost stand still: click "
        "Sam-guh and he carries you up. Walking is allowed too, it just takes a while."),
    ("verhalen/ring-h6", "Smikagol"): (
        "Smikagol",
        "On the hook stretches Smikagol now and then sneaks up to you and grabs for the ring. He warns first; step away or use the Light "
        "Phial, and he keeps his little paws to himself for a minute. If he touches you, you only get a shove."),
    ("verhalen/ring-h6", "Het einde"): (
        "The end",
        "On the Frying Edge above the fryer the finale plays (it cannot be skipped; you can watch it again in the Guhdex). After that the "
        "Rookguhs fly you to the Guhmension, to the grill portal where you set out, and there is the party: Araguh is crowned with a crown "
        "of nibbles. You get a piece of the fried Nibble Ring and all the rewards of the story (see The Lord of the Nibble Ring)."),
    # ================================================= Sauceruman =================================================
    ("verhalen/ring-sausuman", "Hoe kom je er?"): (
        "How do you get there?",
        "The tower opens as soon as you have finished chapter 4 (The Mirror of Guhladriel). It stands a few hundred blocks from the tree "
        "city and then appears in your Super Compass (tab Barbecue). You do not need it for the journey to Mount Fry: it is an outing. You "
        "can still go there after the whole story; Sauceruman is then mostly angry that the ring is already gone."),
    ("verhalen/ring-sausuman", "De questlijn"): (
        "The questline",
        "Talk to Sauceruman in the hall. Then fetch the three ingredients for his Ring Baker: ring dough from the Dough Kneader (first "
        "floor), hot frying sauce from the Sauce Tap (second) and cheese from the Cheese Cupboard (third). In the Cheese Cupboard there is "
        "only an onion left. Pull the lever of the Ring Baker downstairs: after the scene you have an onion ring and Sauceruman sits in "
        "his Sulking Corner. Offer him a bite (or admit that you ate it yourself) and it is done."),
    ("verhalen/ring-sausuman", "Wat krijg je?"): (
        "What do you get?",
        "The Pannantír (a pan in which you see things, to put down at home), four onion rings, and from then on the Ring Baker bakes one "
        "onion ring a day for you. Everyone does the questline for themselves: the supplies never run out."),
    ("verhalen/ring-sausuman", "Waarom doen zijn machines het niet?"): (
        "Why don't his machines work?",
        "The machines in the tower are real machines of Guh Technology, with real Guh Wire. Sauceruman has hung five of them on one Mika "
        "Wheel, and that only gives 10 chonk power. Look at a machine and you read it yourself: too heavy, so everything has stopped. "
        "Upstairs the wire leads nowhere at all. A Guh Wheel with a guh in it works better."),
    ("verhalen/ring-sausuman", "Sputterpijpen"): (
        "Sputter Pipes",
        "The pipes that make the tower smoke you can make yourself (polished grill iron around a glowing coal). They puff and sputter by "
        "themselves, as long as there is air above them. They hurt nothing and nobody."),
    # ================================================= Super Guhrio =================================================
    ("verhalen/super-guhrio", "Het kasteel"): (
        "The castle",
        "Big Nether Mika's Castle stands in the frying sauce sea of the Guh Barbecuether (Super Compass: Barbecue). You arrive on the "
        "forecourt, walk through the gatehouse and across the courtyard to the level hall. Seven gates hang there: six levels and, at the "
        "end of the red carpet, the great gate to the duel. A gate opens when the level before it has been beaten."),
    ("systemen/super-guhrio-spelen", "De knoppen"): (
        "The keys",
        "In a level you look from the side and you are fixed to the lane. A and D walk left and right, space jumps (holding it is higher, "
        "a quick tap is a little hop), S dives into a pipe, W goes through a door, sprinting is running. A mouse button throws a nibble "
        "when you have the Fire Pepper, or shoots out Guhshi's tongue. Q twice steps out of the level."),
    ("systemen/super-guhrio-spelen", "Wat je tegenkomt"): (
        "What you run into",
        "Guhmbas (jump on them: flat, nyeg!), Shell Mikas (jump on one and kick the shell away: it sweeps a whole row of Guhmbas over and "
        "flips switches), Flomp Mikas (they drop when you walk underneath), spinning grill skewers, Chomp Flowers in pipes (a wet kiss, "
        "and back to your flag). Nothing hurts: a touch costs you your Super Nibble or Fire Pepper, or puts you back at your last flag."),
    ("systemen/super-guhrio-spelen", "Munten, vadsmunten en records"): (
        "Coins, chonk coins and records",
        "Ordinary coins count once for your pouch (playing again earns no extra coins); you buy things with them at Toad-guh's. In every "
        "level lie three big chonk coins; the Guhdex (Tales > Super Guhrio) shows per level which ones you have. Your fastest time per "
        "level and for the whole castle in one go (from 1-1 through 3-2) is with the high scores, next to the server's record."),
    ("systemen/super-guhrio-spelen", "Guhshi"): (
        "Guhshi",
        "In the cellars (world 2) lies Guhshi's egg. Whoever found it may ride on his back in the keep (world 3): hold space to flutter "
        "over big gaps, and chomp Guhmbas and coins away with a mouse button. If you are hit, Guhshi runs back to his spot and you still "
        "simply stand."),
    ("verhalen/super-guhrio", "Wereld 1: de binnentuin"): (
        "World 1: The Courtyard",
        "The first two gates of the level hall (the little paintings with the green hill) lead to the courtyard: a painted lawn inside "
        "the castle walls, with hills that watch you go by and little clouds with a snoot. Here you learn the game. Above the panels tips "
        "appear as long as you have never beaten a level: walking and jumping, bumping a question block, landing on top of a Guhmba, "
        "diving into a pipe, going through a door. Every level is two floors long: first the garden itself, then up through a pipe or a "
        "door, where the flagpole stands."),
    ("verhalen/super-guhrio", "Level 1-1: de binnentuin"): (
        "Level 1-1: The Courtyard",
        "Across the lawn, past the first Guhmbas (they walk back and forth between two little hedges: watch from the hedge and jump on "
        "them), under the bricks with the first Super Nibble, over the cheese pond and the little stairs. At the end stands the big green "
        "pipe: stand on it and press S. Upstairs you walk across the clouds and the treetops to the flagpole on the tower. The three big "
        "chonk coins: on top of the bricks (climb the double hedge behind them and jump back), on the loose little cloud (under the "
        "floating coin there is a hidden block) and in this level's secret."),
    ("verhalen/super-guhrio", "Level 1-2: de heggentuin"): (
        "Level 1-2: The Hedge Garden",
        "A hedge to climb with a Guhmba on it, two cheese ponds with a little island, and then a Shell Mika with three Guhmbas in front of "
        "it: jump on him, kick his shell away and it sweeps the whole row into the pond. Through the hedge arch, with a run-up over the "
        "wide pond, and through the tower door (W) upstairs. There you walk along the garden wall, between the battlements, to the "
        "flagpole. The three big chonk coins: in the little box of bricks (you only break in when you are big; the Super Nibble is in the "
        "block before it), high above the gap behind the little wall tower (jump far off the tower) and in this level's secret."),
    ("verhalen/super-guhrio", "De geheimen van de binnentuin"): (
        "The secrets of the courtyard",
        "Every level has a secret room. In 1-1 it is the Mole Hole: above one of the pipes on the lawn floats a little coin, and you can "
        "dive into that pipe. You come out again further on. In 1-2 it is the gardener's Coin Greenhouse: the door stands on top of the "
        "hedge arch. Next to the low hedge before it two coins float one above the other; jump under them and a block appears that you "
        "can climb on. Whoever has both secrets and all six big chonk coins gets the advancement 'Geen tuingeheimen meer'."),
    ("verhalen/super-guhrio", "De prinses is in een ander kasteeldeel"): (
        "The princess is in another part of the castle",
        "Behind the flagpole of level 1-2 Toad-guh stands waving in front of a little gate. 'Thank you! But the princess is in another "
        "part of the castle, nyeg.' He says that every time you beat the level, and every time he has something else to add. After the "
        "first time the gate to the cellars (world 2) is open."),
    ("verhalen/super-guhrio", "Wereld 2: de kelders"): (
        "World 2: The Cellars",
        "Under the courtyard lie the cellars of the castle: dark, bricked in blue-green and full of green pipes. Level 2-1 is called The "
        "Pipe Cellar. Chomp Flowers live in the pipes: as long as such a flower sticks out of her pipe you get a wet kiss and stand at "
        "your flag again, but when she is inside you can stand on the pipe and dive into it (S). The first pipe leads to a hidden coin "
        "cellar, and the pipe on the little island in the cheese sauce is a secret shortcut. Platforms glide back and forth over the "
        "sauce, further on the blocks drop away under your feet and a lift takes you to a high ledge."),
    ("verhalen/super-guhrio", "De vadsmunt in de rode kooi"): (
        "The chonk coin in the red cage",
        "In level 2-1 a big chonk coin hangs in a cage of red blocks. The exclamation switch that opens the cage is at the end of a little "
        "tunnel that you do not fit into. The shell of a Shell Mika does: jump on the Mika that walks in front of it, give his shell a "
        "push toward the tunnel and then jump up from the ledge, into the cage. Mind you: the shell comes back too, nyeg."),
    ("verhalen/super-guhrio", "Het ei van Guhshi"): (
        "Guhshi's Egg",
        "Level 2-2 is called Guhshi's Nest. Halfway stands a fence with an egg lock: it only opens for whoever carries Guhshi's egg. The "
        "egg lies in the little egg room, behind the door under the painted egg (stand in it and press W). With the egg you walk through "
        "the fence to the hatching nest, a warm little stove with straw on it. There Guhshi crawls out of his egg; he thinks you are his "
        "mama. From that moment on he waits for you on his nest and you may also ride on his back in the keep (world 3). The scene of the "
        "hatching you can watch once more in the Guhdex (Tales > Super Guhrio)."),
    ("verhalen/super-guhrio", "De warpkamer"): (
        "The warp room",
        "Not every pipe comes out where you think. In level 2-2, right before the big stairs, a Chomp Flower lives in a pipe that leads "
        "to the warp room: a pitch-black room with three golden warp pipes, each under a number: 3-1, 3-2 and 1-1. Dive into such a pipe "
        "and you stand in that level at once, and the gate of that level in the level hall stays open for you from then on. The level you "
        "left from then does not count as beaten, and a warp does not count for the time of the whole castle in one go either. In the "
        "warp room there is also the third big chonk coin of level 2-2; the green pipe at the end simply takes you back."),
    ("verhalen/super-guhrio", "Wereld 3: de burcht"): (
        "World 3: The Keep",
        "The top floor of the castle: dark brickwork above the frying sauce. In level 3-1, The Grill Corridor, Guhshi waits for whoever "
        "found his egg. You jump across the sauce from hub to hub of the spinning grill skewers, lure Flomp Mikas down, and then do a "
        "stretch on foot: Guhshi stays at the hitching post (fire peppers make him sneeze), the Fire Pepper Bush gives you the Fire Pepper "
        "as often as you like, and a nibble through the slit flips the switch that opens the red wall. On top of that block stands a pipe "
        "with a Chomp Flower: it leads to the treasure room."),
    ("verhalen/super-guhrio", "Level 3-2: De Sauskelder"): (
        "Level 3-2: The Sauce Cellar",
        "Guhshi's level. Two gaps are so wide that only his flutter jump gets across; whoever is on foot hits the timer switch and runs "
        "across the bridge that then lies there for eight seconds. In between falling blocks under fast skewers, a platform that glides "
        "under two Flomp Mikas, and behind a door the Pepper Room, where a nibble through a slit opens the cage with the third chonk "
        "coin. Every level of the keep can also be beaten without Guhshi."),
    ("verhalen/super-guhrio-duel", "Ronde 1: de hendel"): (
        "Round 1: the lever",
        "The Big Nether Mika walks across his bridge, throws slow glowing embers (jump over them) and jumps. When he lands, the thud "
        "shoves everyone away who stands on the ground: jump at the right moment. Run under him while he hangs in the air and walk into "
        "the lever on the other side: the far half of the bridge plops into the sauce. He jumps just in time to the half that stays. The "
        "green pipe takes you back to his side."),
    ("verhalen/super-guhrio-duel", "Ronde 2 en 3: het schild"): (
        "Rounds 2 and 3: the shell",
        "On the short bridge he crawls into his shell and rolls back and forth between the wall and the broken end. Jump over it. After "
        "three bumps against the wall the ?-block at the start is full again: the Fire Pepper. Throw a nibble at the shell while it rolls "
        "at you and it bounces back. The third time he cannot brake any more. If you lose the Fire Pepper, the ?-block fills up again."),
    ("verhalen/super-guhrio-duel", "Hoe het afloopt"): (
        "How it ends",
        "He splashes into the sauce, climbs out again on the other side (wet, sticky, offended, otherwise nothing wrong) and sits down to "
        "sulk. The bridge comes back, and then Princess Peachguh comes out of her door, with the cake. Everyone who stands in the arena "
        "has won; whoever walks in after that finds the Big Nether Mika ready for a new fight. The Guhdex (Tales > Super Guhrio) plays "
        "the scene once more."),
    ("verhalen/pad-guhs-kraam", "De kraam"): (
        "The stall",
        "Toad-guh counts in the coins of Super Guhrio: every coin in a level counts for your pouch the first time you take it, and at "
        "every flagpole you get a little tip (one coin per five on your panel). He sells three outfits (the red Guhrio cap with mustache "
        "and the green Luiguh cap for 30 coins, the Shell Mika shell for 40) and three building blocks (the Nibble Question Block for 20, "
        "four castle flagpoles for 10, two Green Travel Pipes for 25). Every player has a pouch of their own."),
    ("verhalen/pad-guhs-kraam", "Bedankt! Maar de prinses..."): (
        "Thank you! But the princess...",
        "Every time you have played a world all the way through (the courtyard, the cellars, the keep) Toad-guh thanks you and tells you "
        "that the princess is in another part of the castle. All that time she is simply sitting in the tower room eating cake: you can "
        "drop by through the stairs in the level hall."),
    ("verhalen/pad-guhs-kraam", "Guhshi is van jou"): (
        "Guhshi is yours",
        "Whoever wins the duel with the Big Nether Mika may take Guhshi along. He stands in the tower room next to Princess Peachguh: "
        "click him and say yes. You get a Guhshi of your own, tamed and with his red saddle already on. The Guhshi in the tower room stays "
        "there for the next player: everyone gets one. From the princess you get her little crown and a cake."),
    ("verhalen/pad-guhs-kraam", "Wat Guhshi kan"): (
        "What Guhshi can do",
        "On his back you jump with space. Hold space in the air and he flutters: for a moment he does not fall and even climbs a little, "
        "once per jump, good for gaps of about seven blocks. When a kaasknabbel lies on the ground within five blocks, he chomps it away "
        "with his long tongue (one every two seconds; nibbles that were just thrown down or are meant for somebody he leaves alone). In "
        "his guh menu you switch the tongue off."),
    ("verhalen/pad-guhs-kraam", "De grote vadsmunten en de gouden vadspet"): (
        "The big chonk coins and the golden chonk cap",
        "In every level lie three big chonk coins. The Guhdex (Tales > Toad-guh's Stall and the rewards) shows all eighteen, per level, "
        "with a check mark at what you have; Toad-guh shows it too when you ask him. When you have them all, he gives you the golden "
        "chonk cap."),
    ("verhalen/pad-guhs-kraam", "Het highscorebord"): (
        "The high score board",
        "On the right of the forecourt stands the high score board. Above the golden block float the fastest times of the server: the top "
        "three of the whole castle in one go and the record of every level. Click the block and you read your own best times next to "
        "them in the chat."),
    ("systemen/groene-reispijp", "Zo werkt hij"): (
        "How it works",
        "Put down two Green Travel Pipes, at most 50 blocks apart. Stack more on top of each other and the pipe gets taller: the top one "
        "is the mouth. Stand on the mouth and sneak (or click the pipe you stand on): you slide in and come out of the nearest other pipe "
        "of the same color. Above that pipe two blocks have to be free. Let go of the sneak key for a moment before you go back. Riding a "
        "guh, you do not fit in."),
    ("systemen/groene-reispijp", "Meer paren"): (
        "More pairs",
        "Click a pipe with dye and the whole pipe gets that color. A red pipe belongs to the nearest red one, a blue one to a blue one: "
        "that is how you put several pairs close together. On the way nothing can touch you, and you lose nothing."),
    ("bouwwerken/guhrio_kasteel", "Het voorplein"): (
        "The forecourt",
        "Left of the path stands Toad-guh's mushroom stall, with a little mushroom next to it and a floating Nibble Question Block between "
        "two bricks (jump against it: one kaasknabbel a day). On the right stand the high score board and a flagpole. On each side stands "
        "a Green Travel Pipe: together they are a pair, so you can try it right away."),
    ("bouwwerken/guhrio_kasteel", "De torenkamer"): (
        "The tower room",
        "Behind the duel hall lies the room of Princess Peachguh: a pink carpet to her cake table, a peach of wool in a golden frame, her "
        "four-poster bed, a little tea corner, a piano and Guhshi's nest."),
}
