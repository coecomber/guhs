"""
The English of the 1.6.0 wiki pages: Safari Park Vadse Bergen, the Eternal Nibble Fields and Building Help (tools/wiki_bbq2/en.py
takes these two tables into its own; the Dutch is in tools/features/safari_wiki.py, knabbelvelden_wiki.py and bouwhulp_wiki.py).

PAGINA: page -> (English title, English lead).
TEKST:  (page, Dutch heading) -> (English heading, English text).

Names as in en_us.json (Site Shed, Shape Wand, Design Desk, Enclosure Sign, Nibble Portal, Beaver Brook...). The species lists are
read from the game data like the Dutch ones, with the English names of en_us.json.
"""
import json
import os

_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
_R = os.path.join(_ROOT, "src", "main", "resources")


def _lees(*pad):
    try:
        with open(os.path.join(_R, *pad), encoding="utf-8") as f:
            return json.load(f)
    except OSError:
        return {}


_SOORTEN = _lees("data", "guhs", "safari", "soorten.json")
_EN = _lees("assets", "guhs", "lang", "en_us.json")
_ZONES = [("savanne", "Savanna"), ("delta", "River Delta"), ("regenwoud", "Rainforest"), ("bamboe", "Bamboo Mountains"),
          ("ijskust", "Ice Coast"), ("oertijd", "Ice Age Valley"), ("europa", "European Forest"), ("australie", "Australia")]
_AFDELINGEN = [("terrarium", "Terrarium"), ("kriebelzaal", "Creepy-Crawly Hall"), ("nachtzaal", "Night Hall"), ("aquarium", "Deep Sea Hall")]


def _naam(soort):
    return _EN.get(f"entity.guhs.safari.{soort}", soort.replace("_", " ")).lower()


def _lijst(zone, sectie=None):
    namen = [_naam(s) for s, d in _SOORTEN.items() if d.get("zone") == zone and (sectie is None or d.get("sectie") == sectie)]
    return ", ".join(namen[:-1]) + " and " + namen[-1] if len(namen) > 1 else "".join(namen)


def _per_zone():
    return "\n\n".join(f"{t}: {_lijst(z)}." for z, t in _ZONES if _lijst(z))


def _per_afdeling():
    return "\n\n".join(f"{t}: {_lijst('kriebelhuis', a)}." for a, t in _AFDELINGEN if _lijst("kriebelhuis", a))


PAGINA = {
    "systemen/safaripark": (
        "Safari Park Vadse Bergen",
        "A big, fixed safari park in a dimension of its own, full of cute chibi animals in guh style. You spot and photograph them, "
        "ride along on the safari truck, earn Safari Coins and take an animal family home to your own base."),
    "systemen/safari-lobby": (
        "The entrance village",
        "The village at the entrance of the safari park: the gift shop, the ranger station, the lodge, the ice cream stand, the "
        "playground, the campfire, the petting corner, the experts' pavilion and the stop of the safari bus."),
    "systemen/safari-dieren": (
        "The safari animals",
        "Lions, elephants, penguins, beavers, red pandas, a narwhal and lots more: all the animals of the safari park, zone by zone. "
        "They are passive and can't be hurt, and they always stay in their own area."),
    "systemen/safari-vervoer": (
        "Getting around the safari park",
        "The safari bus takes you there, and in the park you ride along on the safari truck, paddle down the river in a canoe, float "
        "over the Ice Coast on the cable car and sail past the jetties on the glass-bottom boat."),
    "systemen/safaricamera": (
        "The Safari Camera and the Safari Dex",
        "Photograph the animals with the Safari Camera. Every species you photograph goes into the Safari tab of your Guhdex, with "
        "your best photo and up to three stars. Photos earn Safari Coins."),
    "systemen/safari-quests": (
        "Quests, secrets and badges",
        "Two short side quests, a secret in every zone, a zone expert per zone with a badge, three titles and a reward for the full dex."),
    "systemen/verblijfsborden": (
        "Enclosure Signs: animals in your own base",
        "Buy an Enclosure Sign in the gift shop, put it down and an animal family comes to live with you: a male, a female and two "
        "little ones. Petting, feeding, toys and presents; and some animals even build for you."),
    "systemen/kriebelhuis": (
        "The Creepy-Crawly House",
        "A big building in the entrance village full of little critters: a terrarium under a glass dome, the Creepy-Crawly Hall, a dark "
        "Night Hall, a Deep Sea Hall and a pink glass tunnel under the pond."),
    "systemen/knabbelvelden": (
        "The Eternal Nibble Fields",
        "A pink cheese plain where your guhs go into retirement. With the Nibble Portal you send them there and bring them back: "
        "not dead, just retired, nyeg!"),
    "systemen/bouwhulp": (
        "Building Help",
        "Build faster and prettier: everything you build first stands as a ghost, the Site Shed builds it for you with material from "
        "your chests, and with the Shape Wand, the brushes and blueprints you make a dome, a roof or a whole little house in a moment."),
    "systemen/spookbouw": (
        "Ghost building",
        "Everything of the building help first appears as a ghost: a see-through preview you can move, turn and mirror. Only when you "
        "press Build! do the real blocks come."),
    "systemen/bouwkeet-niveaus": (
        "The Site Shed and its levels",
        "A yellow and pink shed of 3 by 3 by 3 with a little crane on the roof. That is where building starts. With chonk power and "
        "four upgrades it grows from Handiwork to Glow Shed."),
    "systemen/vormstaf": (
        "The Shape Wand: shapes and roofs",
        "A sphere, dome, arch, spiral staircase, heart or even a guh: pick a shape, put it down as a ghost and let it be built. Roofs "
        "too, with the right corners."),
    "systemen/kwasten": (
        "The brushes",
        "Four brushes to paint what is already there: swapping, mixing, patterns (animal patterns from the safari park too) and "
        "weathering with moss and cracks."),
    "systemen/blauwdrukken": (
        "Blueprints and the Design Desk",
        "Capture what you built and build it again somewhere else, pick from the catalogue, or build what other players have put in "
        "the server's library."),
    "systemen/bouwgereedschap": (
        "Leveller, Paint Machine and Tunnel Drill",
        "Three tools for the higher levels of the Site Shed: flattening land, recolouring everything at once and drilling a pretty "
        "tunnel."),
    "systemen/bouwblokken": (
        "The new building blocks",
        "More than 400 new blocks to build with, in a creative tab of their own, Guhs: Building: pastel bricks, roof parts, vertical "
        "slabs, beams, window frames, wallpaper, plush blocks and lots more."),
    "systemen/helperdieren": (
        "Helper animals: animals that build",
        "Your animal family at home can build for you: beavers build a dam with the water rising behind it, penguins an igloo, "
        "orangutans a tree house. They look at the real land and make something that fits exactly."),
}

TEKST = {
    # --- the park ---
    ("systemen/safaripark", "Erheen"): (
        "Getting there",
        "The park lies in a dimension of its own. You get there on the Guh Safari Bus, which leaves from a bus station in the "
        "Guhmension: a thatched lodge with guh ears on the ridge and a gate of two elephants. Click the bus and the driver guh takes "
        "you there. The bus also brings you back, to the station where you got on. Your own guhs that follow you simply come along "
        "and get a safari hat in the park."),
    ("systemen/safaripark", "De zones"): (
        "The zones",
        "The park is about 512 by 512 blocks, with one river winding through all the zones: the Savanna (the biggest, with Proud "
        "Rock), the River Delta, the Rainforest, the Bamboo Mountains, the Ice Coast, the Ice Age Valley, the European Forest and "
        "Australia. Every zone has a lookout tower, a viewing hut, a truck stop and a secret. Between the zones the truck drives "
        "through big safari gates."),
    ("systemen/safaripark", "Veilig"): (
        "Safe",
        "In the park you take no fall damage, you don't get hungry and you can't get hurt. You can't break or build there; doors, "
        "gates, buttons and the guhs do work. The animals never escape."),
    ("systemen/safaripark", "De dag in het park"): (
        "A day in the park",
        "At set times of the day there are feeding times (the hiders come out then too), a ranger talk at a truck stop (keep "
        "listening for a few coins) and now and then a night safari at night with lantern trucks: animals that are awake then have "
        "glowing eyes. Every real day another species has its birthday, with garlands and a little cake: photos of that species give "
        "double coins. The schedule board at the ranger station says what happens when."),
    # --- the lobby ---
    ("systemen/safari-lobby", "Wie woont er?"): (
        "Who lives there?",
        "Head Ranger Guhmbo welcomes you, gives you a Safari Camera on your first visit and a tip every day. Photographer Flitsguh "
        "gives photo tips, and a new camera if you have none. Keeper Knuffelien looks after the petting corner. Gift Shop Guh Kassie "
        "sells all sorts of things for Safari Coins. Lodge Keeper Snurkguh gives you a hut. Ice Cream Seller Melty sells animal ice "
        "creams, and in the evening Storyteller Grandpa Snort tells stories at the campfire. Nine zone experts sit in the experts' "
        "pavilion."),
    ("systemen/safari-lobby", "De lodge"): (
        "The lodge",
        "The lodge has huts on stilts in the pond. Sleep there and you wake up there too. Whoever wakes up in the lodge gets double "
        "coins for the first photo of the day."),
    ("systemen/safari-lobby", "De aaihoek"): (
        "The petting corner",
        "A goat kid, an alpaca, a guinea pig and a little donkey live in the petting corner, each with a young one. You may feed, "
        "brush and pet them, and walk the alpaca on a lead. Every animal has a different favourite food every day; Knuffelien gives a "
        "hint. Every chore gives coins, the right food an extra coin."),
    ("systemen/safari-lobby", "Het IJskraam, de speeltuin en het kampvuur"): (
        "The ice cream stand, the playground and the campfire",
        "At the ice cream stand you buy a panda ice cream (walk faster), a zebra stripe (jump higher), a penguin ice cream (swim "
        "better) or a flamingo sorbet (see in the dark). Visitor guhs play in the playground, and at the campfire you roast "
        "marshmallows. Every day there is also a penguin parade through the village."),
    ("systemen/safari-lobby", "De giftshop"): (
        "The gift shop",
        "For Safari Coins you buy Enclosure Signs (100, 175 or 300 coins), safari feed, safari clothes for you and your guhs, safari "
        "building blocks, toys for your animals at home, posters and paintings, and the park buildings as blueprints. The safari fence "
        "opens up when there are 10 species in your dex, the rock walls at 20."),
    # --- the animals ---
    ("systemen/safari-dieren", "Per zone"): ("By zone", _per_zone()),
    ("systemen/safari-dieren", "Spotten"): (
        "Spotting",
        "Some animals are hiders: they dive into a burrow, a bush or the water if you come too fast or too close. Sneaking helps. The "
        "okapi, the sabre-tooth tiger, the beaver, the fire salamander, the badger and the quokka live in one hard-to-find spot; "
        "paw prints lead there and light up through binoculars. The narwhal is the rarest animal of all: it only shows itself now "
        "and then."),
    ("systemen/safari-dieren", "Wat ze doen"): (
        "What they do",
        "The animals walk around, eat, play, sleep at night (eyes shut and zzz), and each have their own tricks: penguins slide down "
        "the slide on their bellies, elephants spray themselves wet, beavers drag twigs to their dam and badgers go back and forth "
        "through their tunnel. From every viewing hut you see them come by regularly. A watching guh with binoculars sits at every "
        "lookout."),
    ("systemen/safari-dieren", "De aaihoek en de Gouden dikdik"): (
        "The petting corner and the Golden Dik-dik",
        "The four petting corner animals live in the entrance village. The Golden Dik-dik is a secret: you only get it with a full dex."),
    # --- getting around ---
    ("systemen/safari-vervoer", "De safaritruck"): (
        "The safari truck",
        "An open truck drives a fixed route through all the zones, with stops and big safari gates between the zones. Get on where "
        "you like and get off at any stop: about every minute another one comes by. A guide guh tells a fun fact at every stop, and "
        "you may take photos while riding."),
    ("systemen/safari-vervoer", "Kano, kabelbaan en boot"): (
        "Canoe, cable car and boat",
        "At the village you rent a canoe and paddle down the river, past hippos, otters and crocodiles. The cable car floats in a loop "
        "over the Ice Coast and the Bamboo Mountains. The glass-bottom boat sails from the main jetty at the village past jetties in "
        "the Savanna, the Delta, the Rainforest and Europe: you can get on and off at every jetty."),
    ("systemen/safari-vervoer", "De struisvogelrace"): (
        "The ostrich race",
        "On the race track you race on an ostrich. The best times are on the scoreboard."),
    # --- the camera ---
    ("systemen/safaricamera", "Een foto maken"): (
        "Taking a photo",
        "Hold right-click to zoom in and let go to take the photo. The bigger the animal in the picture the better, and an animal "
        "doing something special (sleeping, eating, playing, belly sliding) is extra nice. A photo gets one to three stars, and the "
        "card says why."),
    ("systemen/safaricamera", "De Safari-dex"): (
        "The Safari Dex",
        "Every species you have photographed goes into the Safari tab of the Guhdex, with your best photo. There you can print a photo "
        "as a photo card, or as a little 1 by 1 painting (two sheets of paper). Photos of your own animals at home count too."),
    ("systemen/safaricamera", "Safarimunten"): (
        "Safari Coins",
        "The first photo of a species gives 10, 20 or 40 coins (a better photo later gives the difference). Every day there are three "
        "photo missions of 40 coins. You also get coins for secrets, complete zones, the side quests and the petting corner. Photos "
        "taken at night of awake animals with glowing eyes get an extra point."),
    ("systemen/safaricamera", "De gouden safaricamera"): (
        "The Golden Safari Camera",
        "With a full dex you get the Golden Safari Camera: every photo taken with it gets a golden frame."),
    # --- quests ---
    ("systemen/safari-quests", "De vermomde guh"): (
        "The disguised guh",
        "A guh has slipped into the park in a home-made zebra suit (\"nyeg, I really am a zebra\"). Find him three times, each time "
        "in a different animal suit. Then he comes home with you, and his zebra suit becomes clothing for all your guhs."),
    ("systemen/safari-quests", "De te vahe olifant"): (
        "The far too chonky elephant",
        "You can't be too chonky... but this elephant is a bit too vah: he no longer fits through his little gate. Help him with a "
        "funny diet and the elephant treadmill. You get the treadmill as a block and a photo frame of the slim elephant."),
    ("systemen/safari-quests", "Geheimpjes"): (
        "Secrets",
        "A secret is hidden in every zone, like a crocodile's lost sunglasses or an old teapot behind the pagoda. Every secret gives "
        "25 coins; find them all and you get a trophy and the title Sniffer. In Australia the post guh is still looking for a lost "
        "postcard."),
    ("systemen/safari-quests", "Zone-experts en badges"): (
        "Zone experts and badges",
        "Nine experts sit in the experts' pavilion, one for every zone and one for the Creepy-Crawly House (Professor Kriebelguh). When "
        "every animal of a zone is in your dex, his counter lights up: you get the zone badge, a zone painting and 100 coins."),
    ("systemen/safari-quests", "Titels en de volle dex"): (
        "Titles and the full dex",
        "You get Sniffer for all the secrets, Beaver Boss when your beavers have built five beaver dams (see Helper animals) and "
        "Safari King with a full dex. The full dex also brings the Golden Safari Camera, a golden animal statue and the Enclosure Sign "
        "of the Golden Dik-dik."),
    ("systemen/safari-quests", "Bekende bewoners"): (
        "Famous residents",
        "Sixteen animals in the park have a name of their own, like Flappie the elephant and Dikkie. You see their name above their "
        "head, they are in your Guhdex, and next to their home is a little sign with a fun fact."),
    # --- enclosure signs ---
    ("systemen/verblijfsborden", "Kopen"): (
        "Buying",
        "You buy an Enclosure Sign in the gift shop once the species is in your dex: 100, 175 or 300 Safari Coins, depending on the "
        "animal. You can buy a sign of each species once every seven days. Little critters from the Creepy-Crawly House get a terrarium "
        "sign: a little glass tank appears for them to live in."),
    ("systemen/verblijfsborden", "De familie"): (
        "The family",
        "Exactly one male, one female and two little ones come to live on every sign. They stay inside their area (8 to 32 blocks, "
        "round or rectangular), even without a fence. Break the sign and they go back to the park with a puff and you get the sign "
        "back: that way you can move them."),
    ("systemen/verblijfsborden", "Thuis met je dieren"): (
        "At home with your animals",
        "Pet them, feed them with food that suits them (safari feed counts double) and give them a toy: a play ball, scratching tree, "
        "tyre swing or little slide. Happy animals give a present now and then. Friends may pet and feed your animals too."),
    ("systemen/verblijfsborden", "Het menu"): (
        "The menu",
        "Click the sign for the menu: Family (naming, calling back), Enclosure (size, shape, sleeping spot), Happiness (what the "
        "animal still misses, and the button Build a fitting enclosure), Presents, Info (a fun fact, your best photo, when you may buy "
        "a sign again) and, for helper animals, the Building job tab."),
    # --- the Kriebelhuis ---
    ("systemen/kriebelhuis", "Het gebouw"): (
        "The building",
        "Ticket Guh Piepje sits at the counter (tickets are free). Then you walk through the Terrarium under a glass dome, the Creepy-Crawly "
        "Hall with a winding path, little cages, an ant farm and a butterfly garden, the quiet Night Hall under a starry ceiling and "
        "the Deep Sea Hall. A pink glass tunnel runs under the pond to a viewing cave in the kelp forest. At the exit Shop Guh "
        "Spinneke has a little shop, and there is a petting lagoon."),
    ("systemen/kriebelhuis", "De diertjes"): ("The critters", _per_afdeling()),
    ("systemen/kriebelhuis", "Kijken mag"): (
        "Look, don't take",
        "Guhs critters like koi, frog guhs, butterflies and ladybirds live there too. You may look at them but not take them. Info "
        "signs stand at the homes: click one for a fun fact about the animal."),
    # --- the Knabbelvelden ---
    ("systemen/knabbelvelden", "Het Knabbelportaal"): (
        "The Nibble Portal",
        "Make a Nibble Portal from 4 blocks of kaasknabbels, 4 kaasknabbels and a pink dye and put it down: a standing frame of 2 by 2 "
        "with a little cloud of pink cheese. You don't walk through it: right-click opens the screen."),
    ("systemen/knabbelvelden", "Het scherm"): (
        "The screen",
        "On the left are your guhs from the Guhdex, on the right the guhs that have retired. Search by name, sort by number, name, "
        "fur or date, and pick one or more. With the arrows you send them to the Nibble Fields or bring them back. Click a guh to see "
        "everything about it: its personality, its clothes and how long it has been yours."),
    ("systemen/knabbelvelden", "Wie mag er met pensioen?"): (
        "Who may retire?",
        "Every tame guh of yours, even one that is far away: it is fetched first and then goes with a poof. If a guh is busy (it lives "
        "in a Guh House, runs in a Guh Wheel, stays in the Guh Room or is riding something), the screen asks first whether it really "
        "may. A picked-up guh, a guh on holiday or at the office, and a guh in the clouds can't go."),
    ("systemen/knabbelvelden", "Weer naar huis"): (
        "Back home",
        "A guh you bring back comes into your pockets as a picked-up guh, with everything on it. If your pockets are full, it waits "
        "for you at the portal."),
    ("systemen/knabbelvelden", "Op bezoek"): (
        "Visiting",
        "Every player has their own bit of Nibble Field. Go there through the screen: your retired guhs nibble, nap and play around "
        "you, see-through and with a little halo. Click a guh to pet it; sneak + click asks whether it wants to come home. It is always "
        "day, nobody gets hurt, and you can't build or break there."),
    # --- Building Help ---
    ("systemen/bouwhulp", "Zo begin je"): (
        "How to start",
        "Make a Site Shed (yellow and pink wool, a crafting table, logs and kaasknabbels) and a Shape Wand (two sticks and an amethyst "
        "shard). Click the shed for the menu, or click a block with the Shape Wand: a ghost of your shape appears. Press Build! and it "
        "is built. All the building help things are in the creative tab Guhs: Building."),
    ("systemen/spookbouw", "Een spook neerzetten"): (
        "Putting down a ghost",
        "A ghost is the real block, but see-through and softly glowing. Everybody sees it, the owner a little stronger. With the "
        "arrows in the panel you push it forward, sideways, up or down (from where you look), and you can turn and mirror it. Through "
        "a name box a friend may help."),
    ("systemen/spookbouw", "Materiaal"): (
        "Material",
        "The blocks come first from your inventory, then from a linked Bank Guh (click the shed with a Bank Key) and, from a Guh Wheel "
        "Shed on, from every chest connected to the shed's chonk net. If something is missing you see exactly what. Build anyway "
        "builds what is there, and the rest stays a ghost until you have it. In creative you build for free."),
    ("systemen/spookbouw", "Bouwen en terugdraaien"): (
        "Building and undoing",
        "It is built from the bottom up, a few blocks per tick. Blocks that are already right are skipped, and chests, machines and "
        "protected buildings are never touched. You can undo the last five building actions; whatever was in the way comes back "
        "exactly. You can have up to ten ghosts open at a time."),
    ("systemen/bouwkeet-niveaus", "De niveaus"): (
        "The levels",
        "Handiwork (without chonk power): Shape Wand and brushes, shapes up to 16 blocks, roofs, your own blueprints and the library.\n\n"
        "Guh Wheel Shed (10 VK): builder guhs, shapes up to 24, and material from every chest on the chonk net.\n\n"
        "Salt Shed (30 VK): nature shapes, block text, the Leveller and blueprints up to 32.\n\n"
        "Sauce Shed (80 VK): the Paint Machine, the Tunnel Drill and publishing in the library.\n\n"
        "Glow Shed (200 VK): shapes and blueprints up to 64, and Poof!"),
    ("systemen/bouwkeet-niveaus", "Upgraden"): (
        "Upgrading",
        "Click the shed with an upgrade, one level at a time: the Guh Wheel Cog, the Salt Crystal Spirit Level, the Sauce Crane Hook "
        "and the Glow Work Lamp. From the Guh Wheel Shed on, the shed draws chonk power while it is switched on. Without chonk power it "
        "simply works as Handiwork; it keeps its level."),
    ("systemen/bouwkeet-niveaus", "Bouwguhs"): (
        "Builder guhs",
        "In the Builder guhs tab you pick up to four of your own guhs. They put on a builder's helmet (they get their own hat back), "
        "walk along the building site and whistle while they work. Every builder guh nearby makes building faster. The crane on the "
        "shed turns along, and a bar above the shed shows how far along it is. When it is done there is a little party."),
    ("systemen/bouwkeet-niveaus", "Pof!"): (
        "Poof!",
        "A Glow Shed with all the material at hand builds the whole project at once: a pink cloud, POOF!, and there it is."),
    ("systemen/vormstaf", "Vormen"): (
        "Shapes",
        "Sphere, dome, cylinder, half cylinder, arch, tunnel, bridge, pyramid, cone, ring, spiral staircase, helix, heart, guh, egg and "
        "castle wall, hollow or solid, with a wall thickness of your choice. From a Salt Shed on also four nature shapes (a rock, a "
        "hill, a tree crown and a floating island) and block text. How big a shape may be depends on the level of your shed."),
    ("systemen/vormstaf", "Daken"): (
        "Roofs",
        "A gable roof, hip roof, tent roof or ear roof, with the stairs in the corners worked out properly."),
    ("systemen/vormstaf", "Bediening"): (
        "Controls",
        "Click a block: your shape stands there as a ghost. Sneak-click a block: it becomes your material (up to four blocks mixed). "
        "Hold the Shape Wand key (left Alt by default) for the round menu, with the shapes, roofs, the settings screen and capturing. "
        "You always see a preview where you look, only for you. Sneak-click a Site Shed with a tool to link it to that shed: then it "
        "works at its level."),
    ("systemen/kwasten", "Vier kwasten"): (
        "Four brushes",
        "The Swap Brush paints blocks over to your block, in the same shape (a stair stays a stair). The Mix Brush paints a mix of up "
        "to four blocks. The Pattern Brush paints stripes, a checkerboard, a gradient or guh faces. The Weathering Brush makes "
        "everything a bit old and cosy, with moss, cracks and ivy. Every click paints a little ball of blocks, and is built right away."),
    ("systemen/kwasten", "Dierpatronen"): (
        "Animal patterns",
        "Zebra, giraffe, leopard, cheetah, penguin, panda, red panda and sabre-tooth tiger: every animal pattern opens up as soon as "
        "that animal is in your Safari Dex. They colour wool, concrete and terracotta."),
    ("systemen/blauwdrukken", "Vastleggen"): (
        "Capturing",
        "Choose capture in the Shape Wand menu and click two corners, or sneak-click a building once: then it finds the building "
        "itself. Pink sparkles show the frame, and you can still move every side. You can keep up to thirty blueprints of your own."),
    ("systemen/blauwdrukken", "Het Ontwerpbureau"): (
        "The Design Desk",
        "At the Design Desk you browse your own blueprints, the catalogue and the library. Print one (a sheet of paper) and you have a "
        "blueprint in your hand: click a block with it and the design stands there as a ghost. It never runs out."),
    ("systemen/blauwdrukken", "De catalogus"): (
        "The catalogue",
        "Twenty-five designs to build right away, in four groups: houses (from a little guh house to a villa), garden (a greenhouse "
        "with ears, a flower arbour, a little playground), towers (a lighthouse, a windmill, a tree house) and safari (a lodge hut, a "
        "lookout tower, a safari gate). In the safari gift shop you can also buy the park's buildings as blueprints."),
    ("systemen/blauwdrukken", "De bibliotheek"): (
        "The library",
        "From a Sauce Shed on you can publish a blueprint in the server's library, with your name on it. Everybody can then build it "
        "and give it a thumbs up. Every player can publish ten."),
    ("systemen/bouwgereedschap", "De egaliseerder"): (
        "The Leveller",
        "From a Salt Shed on: flatten a piece of land, or make a slope, a hollow, a pond or a hill. First it stands as a ghost with a "
        "panel, then it is built."),
    ("systemen/bouwgereedschap", "De verfmachine"): (
        "The Paint Machine",
        "From a Sauce Shed on: recolours wool, concrete and terracotta in one go, as a rainbow gradient too."),
    ("systemen/bouwgereedschap", "De tunnelboor"): (
        "The Tunnel Drill",
        "From a Sauce Shed on: drills a round, square or vaulted tunnel, with lining and little lamps. No mining mode: just a pretty "
        "tunnel."),
    ("systemen/bouwblokken", "Wat er is"): (
        "What there is",
        "Pastel bricks and plaster in 16 colours (with stairs, slab and wall), roof tiles in 6 colours, guh tiles, wallpaper with a "
        "skirting board that appears at the bottom by itself, plush blocks that bounce a little and squeak, stained glass and "
        "portholes, guh window frames and shutters with a heart, gutters, chimneys that smoke and dormers."),
    ("systemen/bouwblokken", "Vormhulp"): (
        "Shape helpers",
        "Vertical slabs and corners of wood, stone and the new stones; posts and beams that connect by themselves; and sloped roof "
        "parts of a half and a quarter block. Build up a slope and the roof part picks the next piece by itself; right-click gives the "
        "next one."),
    ("systemen/bouwblokken", "Tuin en straat"): (
        "Garden and street",
        "Guh fences with ears and little gates, a lamp post with a guh cap, a doormat \"WELKOM GUH\", benches to sit on and a "
        "birdhouse where a little bird sings now and then."),
    ("systemen/helperdieren", "Zo werkt het"): (
        "How it works",
        "Click the Enclosure Sign of a helper animal and open the Building job tab. Pick a job, and choose Find a spot or point at a "
        "spot yourself with the Shape Wand. The animal works out a ghost that fits the real land. Move or turn it, let it work it out "
        "again, and confirm. The family then goes on a work outing (up to 48 blocks from the sign), builds it with their own work "
        "animations, and walks back happily afterwards."),
    ("systemen/helperdieren", "Regels"): (
        "Rules",
        "One job per sign at a time. The animals are free, only the blocks cost something: from your inventory or a Bank Guh you link "
        "to the sign with a Bank Key. The happier the family, the bigger and prettier they build."),
    ("systemen/helperdieren", "Wie kan wat?"): (
        "Who can do what?",
        "Beaver: a dam with a beaver pool, a lodge, bridge, jetty, boardwalk or felling trees. Meerkat, badger and wombat: a cellar, "
        "tunnel, pond, burrow network or brook. Elephant: carrying for a Site Shed nearby, clearing fallen trees, stamping land flat "
        "or a mud pool. Orangutan: a tree house, rope bridge or suspension bridge. Penguin: an igloo, ice slide, ice sculpture or ice "
        "floor. Capybara: a warm bath, little pond or hot tub. Zebra: zebra paths. Koala, toucan and bumblebee: planting. Rhino and "
        "Cape buffalo: demolishing. Otter: stepping stones or a mud slide. Flamingo: a bank with pink flowers, reeds and water lilies. "
        "Kangaroo: pouch transport between two chests. Mammoth: clearing snow or laying a snow layer."),
    ("systemen/helperdieren", "Bouwdieren"): (
        "Builder animals",
        "Woodpecker: wood carving in walls and beams. Octopus: furnishing a room. Gibbon: hanging up garlands and lanterns. Gecko: "
        "ivy, flower boxes and shutters on a facade. Glow-worms: soft blue light in a tunnel or cellar. Bowerbird: little decorations "
        "in a colour theme."),
    ("systemen/helperdieren", "De beverdam"): (
        "The beaver dam",
        "The beavers look for the narrowest spot of flowing water nearby. The ghost shows the dam and the new waterline: the water "
        "behind it rises one to three blocks and becomes a beaver pool. If nothing flows, they say \"Nothing flows here, nyeg\": let "
        "the digging crew dig a brook first. After five dams you get the title Beaver Boss."),
    ("systemen/helperdieren", "Bouw passend verblijf"): (
        "Build a fitting enclosure",
        "In the Happiness tab of every Enclosure Sign, the button Build a fitting enclosure puts down a ghost of an enclosure with "
        "everything the animal needs, its toy and a guh fence with a gate. The fence is always high enough: no animal jumps over it."),
    ("biomen/beverbeek", "Het beekjesbos"): (
        "The brook forest",
        "A rare forest of willows, alders and birches in the Guhmension, with winding brooks and little waterfalls. Behind wild dams "
        "lie beaver pools with a lodge, and everywhere you see gnawed stumps, leaf piles and little paw prints. No wild beavers walk "
        "around. Beaver Brook only grows in new parts of the world."),
    ("biomen/beverbeek", "De rangerpost"): (
        "The ranger post",
        "Very rarely there is a log cabin with a porch: the ranger post. The Beaver Brook Ranger tells you about beavers and points "
        "you the way to the safari bus. Spaander lives at the post, a beaver you can't tame or take along, but can pet lovingly."),
}

# test round 5 (FX-C): the verblijfsbord and the Bouwkeet explained
TEKST.update({
    ('systemen/verblijfsborden', 'Thuis met je dieren'): ('At home with your animals',
        'Pet them (right-click with an empty hand) and give them a toy: a play ball, scratching post, swing tyre or little slide. They find their own food, so feeding is not needed: a treat that suits them (safari feed counts double) does give hearts. Happy animals now and then leave a gift. Friends may pet and feed your animals too.'),
    ('systemen/verblijfsborden', 'Het menu'): ('The menu',
        'Click the sign for the menu, with the tabs Family (give names, call back, hide for a bit), Enclosure (size, shape, show the edge, the sleeping spot), Happiness (what the animal still misses, and Build a fitting enclosure), Gifts, Info (a fact, your best photo, when you may buy a sign again) and, for helper animals, Build Job. Every button explains itself when you hover over it; a grey button says why it is grey.'),
    ('systemen/verblijfsborden', 'Verstoppen en terugroepen'): ('Hiding and calling back',
        'Hide for a bit makes the animals invisible for twenty seconds, handy when you want to build in their enclosure. Then they come back by themselves with a puff. Call back brings them straight to the sign, also when they are hiding.'),
    ('systemen/verblijfsborden', 'Rand en slaapplek'): ('Edge and sleeping spot',
        "Show edge shows the enclosure's edge with pink dust on the ground, for two minutes or until you click Hide edge. When you change the size, you see the new edge straight away. For the sleeping spot, click Point at sleeping spot: the menu closes, and your next right-click on a block inside the enclosure becomes the spot where the family sleeps at night."),
    ('systemen/verblijfsborden', 'Tevredenheid'): ('Happiness',
        'The bar shows how happy the family is. What the animal needs (water, sand or grass, for example) counts most, then enough room, petted today, its toy nearby and a friend living within 24 blocks. Hover over a line: it says exactly what to do, with example blocks. Everything inside the edge counts, from six blocks below to ten above the sign.'),
    ('systemen/verblijfsborden', 'Bouw passend verblijf'): ('Build a fitting enclosure',
        'This button first only puts down a proposal: a ghost of an enclosure with everything the animal loves, with a fence around it. Nothing is built yet. Like it? Choose Approved, let them build: the family goes on a work outing and builds it themselves, with your blocks from your inventory and the linked Bank Guh. If something is missing, they build what is there and the rest stays a ghost. Below the proposal you already see what you still miss (picture, name and count), and the List button gives you a booklet of it. Cancel removes the proposal. A sign does one job at a time.'),
    ('systemen/spookbouw', 'Materiaal'): ('Material',
        "The blocks come from your inventory first, then from a linked Bank Guh (click the shed with a Bank Key) and, from a Guh Wheel Shed on, from every chest connected to the shed's vadsnet. If something is missing you see exactly what: hover over a block for its name. Build anyway builds what is there, and the rest stays a ghost until you have it. In creative you build for free."),
    ('systemen/spookbouw', 'Boodschappenlijstje'): ('Shopping list',
        'Something missing? Click List (in the shed or the ghost panel): you get a plain book with every block that is still missing, with its exact name and count (and how many stacks that is). Handy next to the crafting table.'),
    ('systemen/bouwkeet-niveaus', 'De niveaus'): ('The levels',
        'Handwork (no vadskracht): shape wand and brushes, shapes up to 16 blocks, roofs, own blueprints and the library. Builds up to 64 blocks from the shed.\n\nGuh Wheel Shed (10 VK): builder guhs, shapes up to 24, and material from every chest on the vadsnet. Builds up to 96 blocks away.\n\nSalt Shed (30 VK): nature shapes, block text, the leveller and blueprints up to 32. Builds up to 128 blocks away.\n\nSauce Shed (80 VK): the paint machine, the tunnel drill and publishing in the library. Builds up to 160 blocks away.\n\nGlow Shed (200 VK): shapes and blueprints up to 64, and Poof! Builds up to 192 blocks away.'),
    ('systemen/bouwkeet-niveaus', 'Bereik'): ('Reach',
        'A shed builds its project up to a certain distance: 64 blocks at Handwork, and 32 more per level, up to 192 at the Glow Shed. If you move the ghost (with the arrows in the shed or the ghost panel) so far that part of it is out of reach, that part turns red and you see a warning. That part is not built until you move it closer or raise the shed a level. While moving, you see the edge of the reach as red dust on the ground.'),
    ('systemen/bouwkeet-niveaus', 'Hoe lang duurt het?'): ('How long does it take?',
        'The shed builds five blocks a second, and every builder guh nearby adds five. A house of seven hundred blocks takes a few minutes. The shed shows how long it still takes, and a little bar floats above it. A ghost stays when you close the menu: you can walk around, come back and move it further until you say Build!'),
    ('systemen/vormstaf', 'Bediening'): ('Using it',
        'Click a block: your shape stands there as a ghost. Sneak-click a block: that becomes your material (up to four blocks mixed). Hold the shape wand key (left Alt by default) for the round menu, with the shapes, roofs, the settings screen and capture. Click the shape in the settings screen or in the shed and you get a grid of every shape, each with a picture and its name; grey ones are still locked. You always see a preview where you look, only for you. Sneak-click a Site Shed with a tool to link it to that shed: it then works at its level.'),
    ('systemen/helperdieren', 'Bouw passend verblijf'): ('Build a fitting enclosure',
        'In the Happiness tab of every Enclosure Sign, the button Build a fitting enclosure first puts down a proposal as a ghost: an enclosure with everything the animal needs, its toy and a guh fence with a gate. Choose Approved, let them build and the animals build it themselves with your blocks, or Cancel. The fence is always high enough: no animal jumps over it.'),
    ('systemen/helperdieren', 'Grijze knoppen'): ('Grey buttons',
        'Is a button grey? Hover over it: it always says why. Usually the sign already has a job (a proposal of Build a fitting enclosure counts too): stop or cancel that first.'),
})
