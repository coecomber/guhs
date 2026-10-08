"""
The English of the wiki pages of the second part of bbq2: Het Guhpad, the scenes of the older stories and Het Snuffeleiland
(tools/wiki_bbq2/en.py takes these two tables into its own; the Dutch is in tools/features/guhpad_wiki.py, oude_scenes_wiki.py,
snuffel_wiki.py, snuffel_steiger_wiki.py and snuffel_dorp_wiki.py).

PAGINA: page -> (English title, English lead).
TEKST:  (page, Dutch heading) -> (English heading, English text).

Hand-written, with the names of tools/lang/GLOSSARY.md section 25 (Het Guhpad = The Guh Path, Het Snuffeleiland = Sniff Island
without an article, Snuffeldorp = Sniffville, Steigerhuisje = Dock Cottage, maatje = buddy, Snuffelmeester = Sniff Maestro and
never "Sniff Master", geneesbloem = healing flower, the island's dogs by their English names) and the rules of that section:
the four colors of the scent meter are fixed words, a tale you follow is a story, nobody gets hurt and nobody fails.
"""

PAGINA = {
    "systemen/het-guhpad": (
        "The Guh Path",
        "The big stories open the worlds. First the stories of the Guhmension; only then does Guhdalf begin with the Nibble Ring and does "
        "the grill portal let you through to the Guh Barbecuether; after that the Nibble Ring, Super Guhrio and the Burnt Mika open the "
        "Guh End. At the very end lies The Real Guh End: nobody knows anything about that yet."),
    "systemen/oude-scenes": (
        "Scenes for the older stories",
        "Six older stories each got a short scene at their biggest moment: Baltoguh, Guhtwo, 626-guh, the Cloud Chapel, the Grill Guh and "
        "the Carpenter Guh. You see it once, exactly at that moment in the story, and can watch it again in the Guhdex afterwards."),
    "verhalen/snuffeleiland": (
        "Sniff Island",
        "Your little brother or sister is ill and only a healing flower can help. Papa has been looking for it for weeks. You sail after "
        "him, wash ashore on an island... and wake up as a dog. That is where you learn to sniff, nyeg."),
    "verhalen/snuffeldorp": (
        "Sniffville and the first sniffing series",
        "You wash ashore on the beach of Sniff Island and wake up as a dog. In Sniffville you learn to sniff, get a naughty buddy, help "
        "the villagers and earn your sniffing diploma. How it ends is for your own nose to find out, nyeg."),
    "systemen/snuffelen": (
        "Being a dog on Sniff Island",
        "On Sniff Island you are a dog. You walk on pawsies, you see the world from low to the ground and your nose can do things your "
        "eyes cannot."),
    "systemen/guhstation": (
        "The Guhstation",
        "A gray-black game console with a guh snoot on it. You get it when you have played through the first series of Sniff Island. "
        "With it you go back to the island whenever you want."),
    "systemen/snuffeldorp-bewoners": (
        "The dogs of Sniffville",
        "On Sniff Island everybody lives on four paws. You meet ten dogs in the first story. Six of them have lost something."),
}

TEKST = {
    # --- The Guh Path ---
    ("systemen/het-guhpad", "De grote verhalen"): (
        "The big stories",
        "Only the big stories count for the Guh Path: Baltoguh and Nomguh, Guhtwo and Clone Island, The Cloud Chapel, Ohana on Guhwai'i, "
        "Sniff Island, The Lord of the Nibble Ring, Super Guhrio. The Carpenter Guh, the Grill Guh, the buildings of the Guh Barbecuether, "
        "the jobs and Guh Technology are simply there for the fun of it: you do not have to do them for it."),
    ("systemen/het-guhpad", "Wat gaat wanneer open?"): (
        "What opens when?",
        "The Guhmension: right away, for everyone. The Nibble Ring: Guhdalf only begins once you have followed all the big stories of the "
        "Guhmension (and the Grill Guh's barbecue burns again). The Guh Barbecuether: the grill portal lets you through after those "
        "stories and after Guhdalf's nibble party (chapter 1 of the Nibble Ring). The Guh End: the portal in the Nibble Cellar lets you "
        "through when you have followed the Nibble Ring and Super Guhrio and have beaten the Burnt Mika."),
    ("systemen/het-guhpad", "Wat mis ik nog?"): (
        "What am I still missing?",
        "Guhdalf and the two portals tell you: you get a list of exactly the stories you are still missing. In your Guhdex, Tales tab, the "
        "path map of the whole Guh Path is at the top, with a check mark where you are done and a lock where you may not go yet; point at "
        "a stop and you see what it still asks. Below it are all the stories per world, and you can fold every world open and shut. You "
        "recognize a big story by its little gold star."),
    ("systemen/het-guhpad", "Was je al verder?"): (
        "Were you further already?",
        "The Guh Path holds for everyone, also if you were in the Guh Barbecuether or the Guh End before the update. If you are still "
        "standing there, you may stay as long as you like: nobody is taken away, and you can always get out. But once you are out, you only "
        "get back in when you have followed the stories. Everything you had already done is kept."),
    ("systemen/het-guhpad", "Mijn verhaal"): (
        "My Story",
        "In your Guhmension Super Compass pick the choice My Story (the first choice of every tab, next to the places). If you follow a "
        "story (Guhdex, Tales tab, 'Follow this story'), the compass points to its next step. If you follow none, it points to the nearest "
        "big story you have not done yet. If that is in another world, it points to the portal you last came through."),
    ("systemen/het-guhpad", "In het questboek"): (
        "In the quest book",
        "At the bottom of the quest book's sidebar is the group The Guh Path with four chapters: Tales of the Guhmension, Tales of the "
        "Guhbarbecuether, Tales of the Guh End and The Real Guh End. Every chapter starts with a lock quest: with a check mark per story "
        "it shows what you still have to do first."),
    ("systemen/het-guhpad", "Het echte Guheinde"): (
        "The Real Guh End",
        "A chapter full of black shadows and question marks. The only thing that is known: you only get there by following all the "
        "stories of the Guhmension, the Guh Barbecuether and the Guh End. One counter keeps track: 'Stories followed'. At the bottom is a "
        "row of question marks for tier 6 of Guh Technology: it only opens in the real Guh End."),
    ("dimensies/guheinde", "Op slot"): (
        "Locked",
        "The portal in the Nibble Cellar only lets you through when you have followed the Nibble Ring and Super Guhrio and have beaten the "
        "Burnt Mika. Coming back from the Guh End is always possible. See The Guh Path."),
    # --- the scenes of the older stories ---
    ("systemen/oude-scenes", "Hoe werkt het?"): (
        "How does it work?",
        "A scene lasts 15 to 25 seconds. The camera takes over for a moment: black bars come into view, you cannot walk and nothing can "
        "happen to you. After that the story simply goes on where it was. Every scene plays once per player; when you play together, each "
        "of you gets their own scene at their own moment and the other notices nothing. The snowstorm, the thunderstorm and the night in a "
        "scene are also only for whoever is watching: for everyone else the weather stays as it was."),
    ("systemen/oude-scenes", "Terugkijken"): (
        "Watching again",
        "Open the Guhdex, go to the Tales tab and click the story. At the bottom is Watch Again. If you are standing at the story's "
        "building, the scene plays again there. If you are somewhere else, you get it as a picture book: a drawing with the lines of the "
        "scene."),
    ("systemen/oude-scenes", "Was je al verder?"): (
        "Were you further already?",
        "Had you (almost) finished a story when the scenes were added? Then you do not have to do anything again. The scene is simply ready "
        "to watch in your Guhdex, nyeg."),
    ("systemen/oude-scenes", "De zes filmpjes"): (
        "The six scenes",
        "The White Wolf Guh (Baltoguh and Nomguh): at the lowest point of the trip back it suddenly snows so hard that you cannot see three "
        "blocks ahead, and then she stands on Wolf Rock. Day 45: The Night of the Bang (Clone Island): what Professor Nibbleclone "
        "remembers when you bring him all six lab notes. Ohana, by the Campfire (Guhwai'i): 626-guh with the picture book, and Lilo-guh "
        "who comes to sit with him. The Snuggleheart Beats (the Cloud Chapel): the clouds slide open as soon as the Cloud Shepherd has all "
        "three things. It's Burning Again! (the Grill Guh): the little flame runs around the grill coal frame, and in the distance somebody "
        "with a pointy hat is watching. The Roof Is On (the Carpenter Guh): the flag at the top, and the first little resident trots "
        "inside."),
    ("verhalen/nomguh", "Het filmpje"): (
        "The scene",
        "At the lowest point of the trip back a scene plays the first time: the storm, the white wolf guh on Wolf Rock and the howling. "
        "After that the storm clears and you ride on. If you have to do the trip once more, you get the conversation of before at that "
        "spot."),
    ("verhalen/kloon-eiland", "Het filmpje"): (
        "The scene",
        "When you bring the professor all six lab notes, you first see what he remembers: the night of day 45."),
    ("verhalen/ohana", "Het filmpje"): (
        "The scene",
        "When you go back to Lilo-guh after the three kind things, you first see the scene by the campfire next to the stilt house."),
    ("verhalen/hemelkapelletje", "Het filmpje"): (
        "The scene",
        "When the Cloud Shepherd has the guh crystal, the golden cheese nibble and the fluff feather, you see the Snuggleheart begin to "
        "beat. Only after the scene does it really beat for you."),
    ("verhalen/grillguh", "Het filmpje"): (
        "The scene",
        "When you light the Grill Guh's pit, you see the fire run through the frame. Whoever is watching in the distance, you will meet "
        "later in The Lord of the Nibble Ring."),
    ("verhalen/timmerguh", "Het filmpje"): (
        "The scene",
        "When the last roof fluff lies on the roof, you see the flag go to the top and the first little resident trot inside. After that "
        "you get your own small Guh House."),
    # --- Sniff Island: being a dog ---
    ("systemen/snuffelen", "Je eigen hond"): (
        "Your own dog",
        "At the dock you choose who you are: a Shiba, Jack Russell, Dachshund, Corgi, Golden Retriever or Pug, each in three coats, and you "
        "give your dog a name. Other players see your dog walking, with your name above it. A big dog looks from higher up than a "
        "dachshund."),
    ("systemen/snuffelen", "Je maatje"): (
        "Your buddy",
        "With your dog you also choose a buddy: Driftseed, Moss Acorn or Sunfluff. It is a naughty forest sprite that turns up later in the "
        "story. Only you can see your own buddy. When you sniff and smell something, it floats a little way ahead and points the way."),
    ("systemen/snuffelen", "Je spullen zijn veilig"): (
        "Your things are safe",
        "A dog has no pockets. As soon as you are on the island, all your things wait for you safely, exactly as you had them. When you go "
        "home, you have everything back in the same place. That also holds when you log out or when something goes wrong. What you get on "
        "the island travels home with you. On the island you cannot build or break, and nothing can hurt you."),
    ("systemen/snuffelen", "Snuffelen en graven"): (
        "Sniffing and digging",
        "Hold the sniff key (R by default). Your dog walks with its nose over the ground and the scent meter appears above your hotbar. "
        "The closer you get to a scent, the further the meter swings and the harder it wags. The color says what you smell: orange is "
        "something tasty, blue a thing, green an animal and purple something strange. There are no trails on the ground: you only follow "
        "your nose. When you are on the spot, you dig with the left mouse button. Every scent you find goes into your sniff book (N by "
        "default)."),
    ("systemen/snuffelen", "Wat een hond nog meer kan"): (
        "What else a dog can do",
        "Sit (Z), wag (V) and bark (B). Nyeg! You can change the keys in the controls, under Guhs."),
    ("systemen/snuffelen", "Snuffelrangen"): (
        "Sniffing ranks",
        "The more scents you know, the higher your rank. There are five: Sniff Pup (from the start), Sniff Nose (20 scents), Sniff Sleuth "
        "(50), Sniff Maestro (100) and Grand Sniff Maestro (150). Your rank decides what your nose can smell. In this first part of the "
        "story you become a Sniff Pup, rank 1 of 5. The other ranks come in a later story. Your rank is in the Guhdex under the story, in "
        "your sniff book and on the Guhstation."),
    ("systemen/snuffelen", "Het boompje"): (
        "The little tree",
        "By the village lies a ring of stones. That is where your buddy's little tree grows: with every good deed for a villager it grows "
        "a step, from sprout to shoot, little bush and young tree. Everybody sees their own little tree. At the end you get a blossom twig "
        "from it."),
    ("systemen/guhstation", "Zo werkt het"): (
        "How it works",
        "Put the Guhstation down and click it. A small screen opens: on a white field your dog and your little brother or sister come "
        "running and play. Click 'Press start' and you are back on the island, at the spot where you were last time. Don't feel like it? "
        "Click 'No, I'd rather not sniff right now, nyeg'. If you do not have a Guhstation yet, the captain at a dock cottage sails you to "
        "the island."),
    ("systemen/guhstation", "Weer naar huis"): (
        "Home again",
        "On the island you have a memory card in your hotbar. Use it and choose 'Save and go home': you stand exactly where you were before "
        "you left, with all your own things. Captain Saltsnout in the island's harbor sails you back too."),
    # --- Sniff Island: the dock ---
    ("bouwwerken/steigerhuisje", "Waar vind je het?"): (
        "Where do you find it?",
        "A dock cottage always stands at the water of a Deep Guh Sea in the Guhmension: a white cottage with a red tiled roof on a stone "
        "quay, with a long pier into the sea. Your Super Compass shows the way: pick the dock cottage under Tales, or pick 'My Story'. "
        "Dock cottages only stand in pieces of world that were discovered after this update. On a world that has existed for a long time "
        "you therefore have to travel quite a way for one."),
    ("bouwwerken/steigerhuisje", "Wie wonen er?"): (
        "Who lives there?",
        "In the cottage Little Wobble lies ill in bed, your little brother or sister. Mrs. Basket keeps watch. At the end of the pier "
        "Captain Saltsnout stands by his boat, The Wet Nose. In the garden hang the paper lanterns of the Lantern Feast. Papa's bed is "
        "empty: his striped scarf still hangs above it."),
    ("verhalen/snuffeleiland", "Zo begint het"): (
        "How it begins",
        "As soon as you step onto a dock for the first time, the story begins: it is the evening of the Lantern Feast. Little Wobble sneaks "
        "out of bed to watch and collapses on the pier. Everybody on the server lives this at their own moment: whoever comes later simply "
        "sees the feast too."),
    ("verhalen/snuffeleiland", "Het ziekbed"): (
        "The sickbed",
        "After that go into the cottage and talk to Mrs. Basket (or to Wobble). Wobble has the sniffle fever. Only the healing flower of "
        "Sniff Island helps against it, and Papa has been looking for it for weeks. You decide to go after him."),
    ("verhalen/snuffeleiland", "Je hond en je maatje kiezen"): (
        "Choosing your dog and your buddy",
        "Captain Saltsnout asks which dog you are. On the island there are only dogs, you see: in the Guhmension you look the way you "
        "always do, on the island you are the dog you choose here. You choose a breed, a coat and a name, and the buddy that comes with "
        "you. You may choose again at the captain as often as you like. Little Wobble is always a puppy of your breed and your color."),
    ("verhalen/snuffeleiland", "De overtocht"): (
        "The crossing",
        "Then you sail out on The Wet Nose. On the way the sky clouds over and the waves get higher and higher. The captain wants to turn "
        "back, but you jump overboard and swim on. Then everything goes black... and you wake up on the beach of Sniff Island. Your own "
        "things stay behind safely and are there again when you come home."),
    ("verhalen/snuffeleiland", "Heen en weer"): (
        "There and back",
        "As long as you do not have a Guhstation yet, the dock is your way to the island: the captain sails you there as often as you "
        "like. When you go home from the island, you arrive exactly where you left. If that was the dock, you see The Wet Nose come in "
        "again."),
    # --- Sniff Island: the village and the first series ---
    ("verhalen/snuffeldorp", "Het eiland"): (
        "The island",
        "Sniff Island lies in a sea of its own. In the southwest is the wide beach where you wash ashore, with palm trees and a sandcastle. "
        "A boardwalk leads to the beach gate of Sniffville. In the middle of the village is the square with the well. Around it stand the "
        "doctor's office (the white house with the red cross), the bakery, the little school, Granny Woolly's cottage and the house with "
        "the vegetable patch of Gardener Turnip. On the east side is the harbor with the jetty and Captain Saltsnout's boat. Through the "
        "meadow gate on the north side you come into the meadow: there stands the sniff school of Master Trufflenose and, on a little "
        "knoll, the ring of stones where your buddy's little tree grows."),
    ("verhalen/snuffeldorp", "De wegversperring"): (
        "The roadblock",
        "Behind the meadow the road goes on, between the rocks. There stands a friendly roadblock with a sign: 'You may only pass here as "
        "a Sniff Nose'. Behind it you see forest, a hill with standing stones and a lighthouse. A Sniff Pup does not get past it, not by "
        "swimming either: you are simply put back. That part of the island belongs to a later story."),
    ("verhalen/snuffeldorp", "1. Aangespoeld"): (
        "1. Washed ashore",
        "Waves, a gull, and a wet snout above you: Wendy Wagtail has found you on the beach. You look down and see... pawsies. You are a "
        "dog! Follow the boardwalk and talk to Wendy at the beach gate."),
    ("verhalen/snuffeldorp", "2. Naar de dokter"): (
        "2. To the doctor",
        "Doctor Plasterpaw hears your story. The healing flower is real and grows deep on the island, but you cannot see it, only smell "
        "it. For that you need a real sniff nose."),
    ("verhalen/snuffeldorp", "3. Snuffelles"): (
        "3. Sniffing lessons",
        "Master Trufflenose gives three lessons in the meadow, each with a real scent. Lesson 1: a buried chew bone close by (orange: "
        "something tasty). Lesson 2: his whistle, further away (blue: a thing). Lesson 3: the bees in the big oak (green: an animal), "
        "which you do not dig up but sniff from up close. After every lesson you go back to the master."),
    ("verhalen/snuffeldorp", "4. Er rommelt iets"): (
        "4. Something's rattling",
        "On the square a bucket rattles and falls off the well, all by itself. Nobody sees who does it. Only you see it: a naughty forest "
        "sprite. It is startled that you can see it, and stays with you. That is your buddy. It smells a little strange (purple)."),
    ("verhalen/snuffeldorp", "5. Goede daden"): (
        "5. Good deeds",
        "Your buddy has hidden all sorts of things and wants to make up for it. Talk to the villagers: the baker misses his rolling pin, "
        "the fisher his bobber, the teacher the school bell, granny her ball of wool, the gardener his little watering can and Little "
        "Droolball his bouncy ball. Ask about it, sniff it out, dig it up and bring it back. With every good deed the little tree grows a "
        "step, and you watch it grow. Four good deeds are enough; you can always do the other two later."),
    ("verhalen/snuffeldorp", "6. Het snuffelexamen"): (
        "6. The sniffing exam",
        "When the little tree is a young tree, you may take the exam. Master Trufflenose has hidden four scents, one of each color: two "
        "lie buried, two hang somewhere. There's no such thing as failing and there is no clock. After that you get your diploma: you are "
        "a Sniff Pup, rank 1 (the lowest) of 5 (the highest)."),
    ("verhalen/snuffeldorp", "7. Een spoor van papa"): (
        "7. A trace of Papa",
        "At the little tree your buddy gives you a blossom twig. And between the stones you smell something familiar. You dig: a blue "
        "scarf with stripes. Papa has been here! His trail goes further onto the island, past the roadblock. With this the story is "
        "finished and you get the Guhstation. To be continued."),
    ("verhalen/snuffeldorp", "Naar huis en terug"): (
        "Home and back",
        "Captain Saltsnout in the harbor sails you home whenever you want, and with the memory card you can do that too. Your story is "
        "kept. If you already have the Guhstation, you go back to the island with it. If you have lost it, the captain has another one."),
    ("systemen/snuffeldorp-bewoners", "Wie is wie"): (
        "Who is who",
        "Wendy Wagtail is the beachcomber who finds you; she lives in the beachcomber's hut by the beach gate. Doctor Plasterpaw is the old "
        "dachshund in the doctor's coat. Master Trufflenose, the big sleuth hound with the green cape, gives sniffing lessons in the "
        "meadow. Captain Saltsnout, a pug with a bone for a pipe, stands by his boat. Baker Crumbsnout stands at his stall on the square, "
        "Fisher Wetnose sits on the jetty, Miss Barkley stands in the schoolyard, Granny Woolly sits on her porch, Gardener Turnip stands "
        "in his vegetable patch and Little Droolball runs around by the well."),
    ("systemen/snuffeldorp-bewoners", "Iedereen ziet zijn eigen verhaal"): (
        "Everybody sees their own story",
        "The dogs are there for everyone, but what they say to you depends on how far you are. What a villager has lost, every player can "
        "find back for themselves. You also see the little tree in your own step, and only you can see your buddy."),
}
