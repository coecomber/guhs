"""
Op Safari! (1.6.0) step 2, slice S5: the verblijfsbord and the animals at home (design ontwerp-safaripark-en-bouwen.md 1.9,
rounds 5-8, 21, 27, 36, 38-39; contract SAFARI_CONTRACT.md "Stap 2 API").

  build(h)   writes
             - data/guhs/safari/verblijf.json: per species with a board (soorten.json "bord" != null) its price, food,
               what makes it happy (tevreden: block counts in its home), gifts, toy, friends, specials, helper tasks
             - the board model (geckolib/models/block/safari/verblijfsbord.geo.json, from the phase-0 object) and a texture
               per species with that animal's portrait on the panel (textures/block/safari/verblijfsbord_<soort>.png)
             - the four toys (speelbal, glijbaantje, krabboom, schommelband), the ooievaarsbus, the reuzen-ei and the
               reuzen-omelet (models, textures, loot, the smelting recipe)
             - the texts (NL + EN)
  python tools/features/safari_verblijf.py [--portretten]   (from the project root; --portretten redraws every portrait)

The portraits are drawn from the shipped animal models with tools/wiki_renders.py (an existing portrait is kept unless
--portretten: drawing all 46 takes a while).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, ".."))
from features import boerderij_dieren as bd          # noqa: E402

RES = os.path.join("src", "main", "resources")
A = os.path.join(RES, "assets", "guhs")
D = os.path.join(RES, "data", "guhs")
SOORTEN_JSON = os.path.join(D, "safari", "soorten.json")
PRIJS = {"gewoon": 100, "middel": 175, "zeldzaam": 300}
OPNIEUW = "--portretten" in sys.argv

# --- what each species likes ----------------------------------------------------------------------------------------------
# an eis: (id, kind, blocks/tag, minimum) - kind "blok": the count of these blocks in the home, "water": water blocks,
# "tag": blocks of a block tag
EISEN = {
    "water": ("water", None, 20), "waterveel": ("water", None, 60), "waterbeetje": ("water", None, 6),
    "modder": ("blok", ["minecraft:mud", "minecraft:packed_mud", "minecraft:muddy_mangrove_roots"], 6),
    "gras": ("blok", ["minecraft:grass_block", "minecraft:short_grass", "minecraft:tall_grass"], 20),
    "zand": ("blok", ["minecraft:sand", "minecraft:red_sand", "minecraft:suspicious_sand"], 12),
    "acacia": ("blok", ["minecraft:acacia_log", "minecraft:acacia_leaves", "minecraft:acacia_wood"], 6),
    "rots": ("blok", ["minecraft:stone", "minecraft:andesite", "minecraft:granite", "minecraft:diorite", "minecraft:cobblestone",
                      "minecraft:mossy_cobblestone", "minecraft:tuff", "minecraft:calcite"], 10),
    "bomen": ("tag", "minecraft:logs", 6),
    "bladeren": ("tag", "minecraft:leaves", 12),
    "bamboe": ("blok", ["minecraft:bamboo", "minecraft:bamboo_block", "minecraft:bamboo_planks"], 8),
    "ijs": ("blok", ["minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice"], 8),
    "sneeuw": ("blok", ["minecraft:snow_block", "minecraft:snow", "minecraft:powder_snow"], 10),
    "aarde": ("blok", ["minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt", "minecraft:podzol"], 10),
    "holletje": ("blok", ["minecraft:rooted_dirt", "minecraft:coarse_dirt", "minecraft:mud"], 3),
    "struiken": ("blok", ["minecraft:sweet_berry_bush", "minecraft:azalea", "minecraft:flowering_azalea", "minecraft:fern",
                          "minecraft:large_fern", "minecraft:bush", "minecraft:firefly_bush", "minecraft:dead_bush"], 4),
    "varens": ("blok", ["minecraft:fern", "minecraft:large_fern"], 4),
    "mos": ("blok", ["minecraft:moss_block", "minecraft:moss_carpet"], 4),
    "zeegras": ("blok", ["minecraft:seagrass", "minecraft:tall_seagrass", "minecraft:kelp", "minecraft:kelp_plant"], 6),
    "bloemen": ("tag", "minecraft:small_flowers", 6),
    "eucalyptus": ("blok", ["minecraft:birch_log", "minecraft:birch_leaves", "minecraft:birch_wood"], 6),
    "jungle": ("blok", ["minecraft:jungle_log", "minecraft:jungle_leaves", "minecraft:vine"], 8),
    "lelies": ("blok", ["minecraft:lily_pad"], 3),
    "donker": ("blok", ["minecraft:moss_block", "minecraft:glow_lichen", "minecraft:cave_vines", "minecraft:cave_vines_plant",
                        "minecraft:hanging_roots"], 3),
}
EIS_NAMEN = {  # id: (nl, en)
    "water": ("Water (20+)", "Water (20+)"), "waterveel": ("Veel water (60+)", "Lots of water (60+)"),
    "waterbeetje": ("Een plasje water", "A little water"), "modder": ("Modder", "Mud"), "gras": ("Gras", "Grass"),
    "zand": ("Zand", "Sand"), "acacia": ("Acacia", "Acacia"), "rots": ("Rotsen en stenen", "Rocks and stones"),
    "bomen": ("Bomen", "Trees"), "bladeren": ("Bladeren", "Leaves"), "bamboe": ("Bamboe", "Bamboo"), "ijs": ("IJs", "Ice"),
    "sneeuw": ("Sneeuw", "Snow"), "aarde": ("Aarde", "Dirt"), "holletje": ("Een holletje (losse aarde)", "A burrow (loose dirt)"),
    "struiken": ("Struikjes", "Bushes"), "varens": ("Varens", "Ferns"), "mos": ("Mos", "Moss"), "zeegras": ("Zeegras", "Seagrass"),
    "bloemen": ("Bloemetjes", "Flowers"), "eucalyptus": ("Berk (eucalyptus)", "Birch (eucalyptus)"),
    "jungle": ("Junglebomen en lianen", "Jungle trees and vines"), "lelies": ("Waterlelies", "Lily pads"),
    "donker": ("Een donker, mossig hoekje", "A dark, mossy corner"),
}

VOER = {
    "gras": ["minecraft:wheat", "minecraft:hay_block", "minecraft:apple"],
    "vlees": ["minecraft:beef", "minecraft:porkchop", "minecraft:mutton", "minecraft:chicken"],
    "vis": ["minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish"],
    "bamboe": ["minecraft:bamboo", "minecraft:sweet_berries"],
    "fruit": ["minecraft:apple", "minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:melon_slice"],
    "groente": ["minecraft:carrot", "minecraft:potato", "minecraft:beetroot", "minecraft:sweet_berries"],
    "zaad": ["minecraft:wheat_seeds", "minecraft:melon_seeds", "minecraft:sweet_berries"],
    "zeegras": ["minecraft:seagrass", "minecraft:kelp", "minecraft:melon_slice"],
    "blad": ["minecraft:oak_leaves", "minecraft:birch_leaves", "minecraft:jungle_leaves", "minecraft:apple"],
}
CADEAU = {
    "vogel": ["guhs:pluisveertje", "minecraft:feather", "minecraft:dandelion"],
    "botje": ["minecraft:bone", "minecraft:dandelion", "minecraft:short_grass"],
    "zee": ["minecraft:nautilus_shell", "minecraft:kelp", "minecraft:prismarine_shard"],
    "bos": ["minecraft:stick", "minecraft:sweet_berries", "minecraft:brown_mushroom", "minecraft:poppy"],
    "bamboe": ["minecraft:bamboo", "minecraft:cherry_leaves", "minecraft:pink_petals"],
    "ijs": ["minecraft:snowball", "minecraft:blue_ice", "minecraft:cod"],
    "jungle": ["minecraft:cocoa_beans", "minecraft:vine", "minecraft:glow_berries"],
    "savanne": ["minecraft:dead_bush", "minecraft:acacia_sapling", "minecraft:dandelion"],
    "outback": ["minecraft:red_sand", "minecraft:birch_sapling", "minecraft:sweet_berries"],
    "wol": ["minecraft:brown_wool", "minecraft:white_wool", "minecraft:string"],
    "glim": ["minecraft:glow_berries", "minecraft:glow_lichen", "minecraft:glowstone_dust"],
}

# per species: (eisen, voer, cadeau, speeltje, vrienden, helper tasks)
DIER = {
    "leeuw": (["zand", "acacia", "rots"], "vlees", "botje", "krabboom", ["jachtluipaard"], []),
    "luipaard": (["acacia", "bladeren"], "vlees", "botje", "krabboom", [], []),
    "olifant": (["water", "modder", "gras"], "gras", "savanne", "bal", ["neushoorn", "giraffe"], ["sjouwen", "opruimen", "stampen", "modderpoel"]),
    "neushoorn": (["modder", "gras"], "gras", "savanne", "bal", ["olifant"], ["slopen"]),
    "kafferbuffel": (["gras", "water"], "gras", "savanne", "bal", ["ooievaar", "flamingo"], ["slopen"]),
    "dikdik": (["struiken", "gras"], "blad", "savanne", "glijbaan", [], []),
    "giraffe": (["acacia", "gras"], "blad", "savanne", "bal", ["zebra", "olifant"], []),
    "zebra": (["gras"], "gras", "savanne", "bal", ["giraffe"], ["zebrapad"]),
    "stokstaartje": (["zand", "holletje"], "zaad", "savanne", "glijbaan", [], ["kelder", "tunnel", "vijver", "holletjesnet", "beekje"]),
    "jachtluipaard": (["zand", "rots", "gras"], "vlees", "botje", "krabboom", ["leeuw"], []),
    "struisvogel": (["zand", "gras"], "zaad", "vogel", "bal", ["zebra"], []),
    "zeekoe": (["waterveel", "zeegras"], "zeegras", "zee", "bal", ["capibara"], []),
    "nijlpaard": (["water", "modder"], "gras", "zee", "bal", ["flamingo"], []),
    "flamingo": (["waterbeetje", "zand", "lelies"], "zeegras", "vogel", "schommel", ["nijlpaard", "kafferbuffel"], ["oever"]),
    "capibara": (["water", "gras"], "groente", "bos", "glijbaan", ["*"], ["badje", "vijvertje", "hottub"]),
    "krokodil": (["water", "zand"], "vlees", "zee", "bal", [], []),
    "rode_panda": (["bamboe", "bomen"], "bamboe", "bamboe", "krabboom", ["reuzenpanda"], []),
    "reuzenpanda": (["bamboe"], "bamboe", "bamboe", "bal", ["rode_panda"], []),
    "gibbon": (["bomen", "bladeren"], "fruit", "jungle", "schommel", ["orang_oetan"], ["slingers"]),
    "luiaard": (["jungle", "bladeren"], "blad", "jungle", "schommel", ["toekan"], []),
    "okapi": (["jungle", "varens"], "blad", "jungle", "bal", [], []),
    "orang_oetan": (["bomen", "jungle"], "fruit", "jungle", "schommel", ["gibbon"], ["boomhut", "touwbrug", "hangbrug"]),
    "toekan": (["jungle", "bladeren"], "fruit", "vogel", "schommel", ["luiaard"], ["planten"]),
    "gekko": (["rots", "jungle"], "fruit", "jungle", "schommel", [], ["gevel"]),
    "glimworm": (["donker", "mos"], "fruit", "glim", "schommel", [], ["licht"]),
    "pinguin": (["ijs", "sneeuw", "water"], "vis", "ijs", "glijbaan", ["zeehond"], ["iglo", "ijsglijbaan", "ijssculptuur", "ijsvloer"]),
    "ijsbeer": (["ijs", "water"], "vis", "ijs", "bal", [], []),
    "zeehond": (["rots", "water"], "vis", "zee", "glijbaan", ["pinguin"], []),
    "narwal": (["waterveel"], "vis", "zee", "bal", [], []),
    "octopus": (["waterveel", "rots"], "vis", "zee", "bal", [], ["inrichten"]),
    "sabeltandtijger": (["sneeuw", "rots"], "vlees", "ijs", "krabboom", [], []),
    "mammoet": (["sneeuw"], "gras", "wol", "bal", ["wolharige_neushoorn"], ["sneeuwschuiven", "sneeuwlaag"]),
    "wolharige_neushoorn": (["sneeuw", "gras"], "gras", "wol", "bal", ["mammoet"], []),
    "bever": (["water", "bomen"], "blad", "bos", "glijbaan", ["otter"], ["dam", "burcht", "brug", "steiger", "vlonder", "kappen"]),
    "vuursalamander": (["mos", "rots", "waterbeetje"], "groente", "bos", "glijbaan", [], []),
    "egel": (["bladeren", "struiken"], "groente", "bos", "bal", [], []),
    "otter": (["water", "zand"], "vis", "zee", "glijbaan", ["bever"], ["stapstenen", "modderglijbaan"]),
    "das": (["aarde", "bomen"], "groente", "bos", "glijbaan", [], ["kelder", "tunnel", "vijver", "holletjesnet", "beekje"]),
    "edelhert": (["gras", "bomen"], "gras", "bos", "bal", [], []),
    "ooievaar": (["waterbeetje", "gras"], "vis", "vogel", "schommel", ["kafferbuffel"], []),
    "specht": (["bomen"], "zaad", "vogel", "schommel", [], ["houtsnijwerk"]),
    "koala": (["eucalyptus"], "blad", "outback", "krabboom", ["wombat"], ["planten"]),
    "kangoeroe": (["zand", "gras"], "gras", "outback", "bal", ["quokka"], ["buideltransport"]),
    "wombat": (["aarde", "holletje"], "groente", "outback", "glijbaan", ["koala"], ["kelder", "tunnel", "vijver", "holletjesnet", "beekje"]),
    "quokka": (["struiken", "gras"], "blad", "outback", "glijbaan", ["kangoeroe"], []),
    "prieelvogel": (["bloemen", "bomen"], "fruit", "vogel", "schommel", [], ["decoreren"]),
    "gouden_dikdik": (["struiken", "gras"], "blad", "savanne", "glijbaan", [], []),
    # round 4 (L4): the Kriebelhuis animals (a "terrarium" one lives in a glass tank at home: the terrariumbord) + the pelican
    "kameleon": (["jungle", "bladeren"], "fruit", "jungle", "schommel", [], []),
    "reuzenschildpad": (["gras", "zand"], "groente", "savanne", "bal", ["*"], []),
    "slang": (["rots", "zand"], "vlees", "bos", "glijbaan", [], []),
    "hoornkikker": (["mos", "waterbeetje"], "groente", "bos", "bal", [], []),
    "kraaghagedis": (["zand", "rots"], "fruit", "outback", "glijbaan", [], []),
    "gifkikker": (["mos", "varens", "waterbeetje"], "fruit", "jungle", "schommel", ["gifkikker"], []),
    "baardagaam": (["zand", "rots"], "groente", "outback", "bal", [], []),
    "mier": (["aarde", "bladeren"], "blad", "bos", "glijbaan", [], []),
    "springspin": (["bladeren", "varens"], "fruit", "bos", "schommel", [], []),
    "wandelende_tak": (["bladeren", "bomen"], "blad", "bos", "schommel", [], []),
    "mestkever": (["aarde", "gras"], "gras", "savanne", "bal", [], []),
    "slak": (["mos", "varens"], "groente", "bos", "glijbaan", [], []),
    "vogelspin": (["aarde", "rots"], "fruit", "jungle", "krabboom", [], []),
    "hommel": (["bloemen", "gras"], "fruit", "vogel", "schommel", [], ["planten"]),
    "kiwi": (["varens", "aarde"], "groente", "vogel", "bal", [], []),
    "schubdier": (["aarde", "holletje"], "groente", "bos", "bal", [], []),
    "spookdiertje": (["jungle", "donker"], "fruit", "jungle", "schommel", [], []),
    "plompe_lori": (["jungle", "donker"], "fruit", "jungle", "schommel", ["spookdiertje", "galago"], []),
    "fruitvleermuis": (["bomen", "donker"], "fruit", "glim", "schommel", [], []),
    "galago": (["bomen", "donker"], "fruit", "jungle", "schommel", ["plompe_lori"], []),
    "suikereekhoorn": (["bomen", "bladeren"], "fruit", "bos", "schommel", [], []),
    "fennek": (["zand", "holletje"], "groente", "outback", "bal", [], []),
    "zeepaardje": (["zeegras"], "zeegras", "zee", "schommel", [], []),
    "pijlstaartrog": (["waterveel", "zand"], "vis", "zee", "bal", ["zeeschildpad"], []),
    "zeeschildpad": (["waterveel", "zeegras"], "zeegras", "zee", "bal", ["pijlstaartrog"], []),
    "clownvis": (["zeegras"], "zeegras", "zee", "bal", [], []),
    "kwal": (["donker"], "zeegras", "glim", "bal", [], []),
    "kogelvis": (["zeegras", "rots"], "vis", "zee", "bal", [], []),
    "zeester": (["zand", "rots"], "zeegras", "zee", "glijbaan", [], []),
    "babyhaai": (["waterveel"], "vis", "zee", "bal", [], []),
    "regenboogvis": (["zeegras"], "zeegras", "zee", "bal", [], []),
    "lantaarnvisje": (["donker"], "vis", "glim", "bal", [], []),
    "dumbo_octopus": (["donker", "rots"], "vis", "zee", "bal", [], []),
    "pelikaan": (["water", "zand"], "vis", "vogel", "schommel", ["flamingo"], []),
}
TAKEN = {  # helper task id: (nl, en)
    "sjouwen": ("Materiaal sjouwen", "Carry materials"), "opruimen": ("Omgevallen bomen opruimen", "Clear fallen trees"),
    "stampen": ("Terrein vlak stampen", "Stamp the ground flat"), "modderpoel": ("Een modderpoel maken", "Make a mud pool"),
    "slopen": ("Slopen (de sloopploeg)", "Demolish (the demolition crew)"), "zebrapad": ("Zebrapaden lopen", "Walk zebra paths"),
    "kelder": ("Een kelder graven", "Dig a cellar"), "tunnel": ("Een tunnel graven", "Dig a tunnel"),
    "vijver": ("Een vijver graven", "Dig a pond"), "holletjesnet": ("Een holletjesnet met kamertjes", "A burrow network with rooms"),
    "beekje": ("Een beekje graven", "Dig a stream"), "oever": ("De oever versieren", "Decorate the shore"),
    "badje": ("Een warm badje", "A warm bath"), "vijvertje": ("Een vijvertje", "A little pond"), "hottub": ("Een hottub", "A hot tub"),
    "slingers": ("Slingers en lampionnen ophangen", "Hang garlands and lanterns"),
    "boomhut": ("Een boomhut bouwen", "Build a treehouse"), "touwbrug": ("Een touwbrug", "A rope bridge"),
    "hangbrug": ("Een hangbrug", "A suspension bridge"), "planten": ("Bomen en bloemen planten", "Plant trees and flowers"),
    "gevel": ("Een gevel levendig maken", "Liven up a facade"), "licht": ("Een tunnel verlichten", "Light a tunnel"),
    "iglo": ("Een iglo bouwen", "Build an igloo"), "ijsglijbaan": ("Een ijsglijbaan", "An ice slide"),
    "ijssculptuur": ("Een ijssculptuur", "An ice sculpture"), "ijsvloer": ("Een ijsvloer over water", "An ice floor over water"),
    "inrichten": ("Een kamer inrichten", "Furnish a room"), "sneeuwschuiven": ("Sneeuw wegschuiven", "Push snow away"),
    "sneeuwlaag": ("Een sneeuwlaag leggen", "Lay a snow cover"), "dam": ("Een dam met beverpoel", "A dam with a beaver pond"),
    "burcht": ("Een beverburcht", "A beaver lodge"), "brug": ("Een brug", "A bridge"), "steiger": ("Een steiger", "A jetty"),
    "vlonder": ("Een vlonder", "A boardwalk"), "kappen": ("Bomen kappen", "Fell trees"), "stapstenen": ("Stapstenen", "Stepping stones"),
    "modderglijbaan": ("Een modderglijbaan", "A mud slide"), "houtsnijwerk": ("Houtsnijwerk", "Wood carving"),
    "buideltransport": ("Buideltransport tussen kisten", "Pouch transport between chests"),
    "decoreren": ("Decoreren in een kleurthema", "Decorate in a colour theme"),
}
SPECIAAL = {"struisvogel": {"rijdier": True, "ei": True}, "ooievaar": {"bezorger": True},
            "narwal": {"water_nodig": 60}, "zeekoe": {"water_nodig": 40}, "octopus": {"water_nodig": 40},
            "pijlstaartrog": {"water_nodig": 40}, "zeeschildpad": {"water_nodig": 40}, "babyhaai": {"water_nodig": 40}}

FEITEN = {  # species: (nl, en)
    "leeuw": ("Leeuwen slapen tot wel 20 uur per dag. Lekker vads.", "Lions sleep up to 20 hours a day. Nice and chonky."),
    "luipaard": ("Een luipaard sleept zijn eten de boom in, zodat niemand het pikt.", "A leopard drags its food up a tree, so nobody steals it."),
    "olifant": ("Olifanten herkennen elkaar nog na jaren. Ze vergeten nooit een knuffel.", "Elephants recognise each other after years. They never forget a cuddle."),
    "neushoorn": ("Een neushoornhoorn is van hetzelfde spul als jouw nagels.", "A rhino horn is made of the same stuff as your nails."),
    "kafferbuffel": ("Buffels stemmen samen af welke kant de kudde op gaat.", "Buffalo vote together which way the herd goes."),
    "dikdik": ("Een dikdik is maar zo groot als een vadsige kat.", "A dik-dik is only as big as a chonky cat."),
    "giraffe": ("Een giraffentong is blauwig en wel 45 cm lang.", "A giraffe's tongue is bluish and up to 45 cm long."),
    "zebra": ("Geen twee zebra's hebben dezelfde strepen.", "No two zebras have the same stripes."),
    "stokstaartje": ("Stokstaartjes houden om de beurt de wacht. Altijd rechtop!", "Meerkats take turns keeping watch. Always upright!"),
    "jachtluipaard": ("De jachtluipaard is het snelste landdier, maar is na een sprint superloom.", "The cheetah is the fastest land animal, but super sleepy after a sprint."),
    "struisvogel": ("Een struisvogelei is zo groot als 24 kippeneieren. Reuzen-omelet!", "An ostrich egg is as big as 24 chicken eggs. Giant omelette!"),
    "zeekoe": ("Zeekoeien zijn familie van... olifanten! Dat zie je aan de vadsheid.", "Manatees are related to... elephants! You can tell by the chonk."),
    "nijlpaard": ("Nijlpaarden kunnen niet zwemmen: ze lopen over de bodem.", "Hippos can't swim: they walk along the bottom."),
    "flamingo": ("Flamingo's zijn roze door wat ze eten.", "Flamingos are pink because of what they eat."),
    "capibara": ("Capibara's zijn met iedereen vrienden. Echt met iedereen.", "Capybaras are friends with everyone. Really everyone."),
    "krokodil": ("Een krokodil kan een uur onder water blijven, en lachen tegelijk.", "A crocodile can stay under water for an hour, and smile at the same time."),
    "rode_panda": ("Rode panda's gebruiken hun pluimstaart als dekentje.", "Red pandas use their fluffy tail as a blanket."),
    "reuzenpanda": ("Een reuzenpanda eet zo'n 12 uur per dag bamboe. Vahoeg.", "A giant panda eats bamboo for about 12 hours a day. Vahoeg."),
    "gibbon": ("Gibbons zingen 's ochtends samen een duet.", "Gibbons sing a duet together in the morning."),
    "luiaard": ("Luiaards komen maar eens per week naar beneden voor de wc.", "Sloths only come down once a week to go to the toilet."),
    "okapi": ("De okapi lijkt op een zebra, maar is familie van de giraffe.", "The okapi looks like a zebra, but is related to the giraffe."),
    "orang_oetan": ("Orang-oetans maken elke avond een nieuw bedje van bladeren.", "Orangutans build a new bed of leaves every evening."),
    "toekan": ("Een toekansnavel is groot, maar heel licht.", "A toucan's beak is big, but very light."),
    "gekko": ("Gekko's lopen tegen muren op dankzij piepkleine haartjes op hun teen.", "Geckos walk up walls thanks to tiny hairs on their toes."),
    "glimworm": ("Glimwormpjes laten hun staartje gloeien om lekkers te lokken.", "Glow-worms make their tail glow to lure snacks."),
    "pinguin": ("Keizerspinguïns knuffelen samen tegen de kou: de huddle!", "Emperor penguins cuddle together against the cold: the huddle!"),
    "ijsbeer": ("Een ijsbeer heeft zwarte huid onder zijn witte vacht.", "A polar bear has black skin under its white fur."),
    "zeehond": ("Zeehonden kunnen onder water slapen, met hun neus dicht.", "Seals can sleep under water, with their nose shut."),
    "narwal": ("De tand van een narwal kan wel 3 meter lang worden.", "A narwhal's tusk can grow up to 3 metres long."),
    "octopus": ("Een octopus heeft drie hartjes. Driedubbel lief!", "An octopus has three hearts. Triple sweet!"),
    "sabeltandtijger": ("Sabeltandtijgers leefden al in de ijstijd. Deze is gelukkig lief.", "Sabre-tooth tigers lived in the ice age. Luckily this one is sweet."),
    "mammoet": ("Een mammoet had een dikke vachtlaag én een vetlaag: extra vads.", "A mammoth had thick fur and a layer of fat: extra chonk."),
    "wolharige_neushoorn": ("De wolharige neushoorn schoof met zijn hoorn sneeuw opzij.", "The woolly rhino pushed snow aside with its horn."),
    "bever": ("Bevers hebben oranje tanden: daar zit ijzer in.", "Beavers have orange teeth: there's iron in them."),
    "vuursalamander": ("Vuursalamanders houden van regen en natte stenen.", "Fire salamanders love rain and wet stones."),
    "egel": ("Een egel heeft wel 5000 stekels. Niet knuffelen zonder handschoen.", "A hedgehog has about 5000 spines. No cuddles without a glove."),
    "otter": ("Otters houden pootjes vast als ze slapen, zodat ze niet wegdrijven.", "Otters hold paws when they sleep, so they don't drift apart."),
    "das": ("Een dassenburcht kan honderden jaren oud zijn.", "A badger sett can be hundreds of years old."),
    "edelhert": ("Een edelhert krijgt elk jaar een nieuw gewei.", "A red deer grows new antlers every year."),
    "ooievaar": ("Ooievaars klepperen met hun snavel om hallo te zeggen.", "Storks clatter their beak to say hello."),
    "specht": ("Een specht tikt wel 20 keer per seconde. Tok tok tok!", "A woodpecker taps up to 20 times a second. Tok tok tok!"),
    "koala": ("Koala's slapen tot 22 uur per dag. Nog vadser dan een leeuw.", "Koalas sleep up to 22 hours a day. Even chonkier than a lion."),
    "kangoeroe": ("Een baby-kangoeroe is bij de geboorte zo groot als een boon.", "A baby kangaroo is as big as a bean when it's born."),
    "wombat": ("Wombats maken vierkante keutels. Ja echt.", "Wombats make square droppings. Yes, really."),
    "quokka": ("De quokka heet het gelukkigste dier ter wereld.", "The quokka is called the happiest animal in the world."),
    "prieelvogel": ("Een prieelvogelmannetje versiert zijn prieel met blauwe spulletjes.", "A male bowerbird decorates his bower with blue things."),
    "gouden_dikdik": ("Een gouden dikdik? Die bestaat niet. Behalve deze.", "A golden dik-dik? That doesn't exist. Except this one."),
    # round 4 (L4)
    "kameleon": ("Een kameleon kan met zijn oogjes twee kanten tegelijk op kijken.", "A chameleon can look two ways at once with its little eyes."),
    "reuzenschildpad": ("Reuzenschildpadden kunnen ouder worden dan 150 jaar. Rustig aan, vads.", "Giant tortoises can live past 150. Slow and steady, chonk."),
    "slang": ("Een slang ruikt met zijn tong. Daarom steekt hij hem steeds uit.", "A snake smells with its tongue. That's why it keeps sticking it out."),
    "hoornkikker": ("Een hoornkikker is bijna helemaal mond. Hap!", "A horned frog is almost all mouth. Chomp!"),
    "kraaghagedis": ("Schrikt een kraaghagedis, dan klapt hij zijn kraag open en rent hij op twee pootjes weg.", "When a frilled lizard gets a fright, it opens its frill and runs off on two legs."),
    "gifkikker": ("Gifkikkertjes zijn zo fel van kleur om te zeggen: niet opeten!", "Poison dart frogs are so brightly coloured to say: don't eat me!"),
    "baardagaam": ("Een baardagaam zwaait met zijn pootje om hallo te zeggen.", "A bearded dragon waves its little paw to say hello."),
    "mier": ("Een mier kan wel 50 keer zijn eigen gewicht tillen. Sterk hoor!", "An ant can lift 50 times its own weight. So strong!"),
    "springspin": ("Een springspin heeft acht oogjes en springt wel 50 keer zijn eigen lengte.", "A jumping spider has eight eyes and jumps 50 times its own length."),
    "wandelende_tak": ("Een wandelende tak wiegt heen en weer, net als een takje in de wind.", "A stick insect sways to and fro, just like a twig in the wind."),
    "mestkever": ("Een mestkever navigeert met de sterren. Ja, met de Melkweg!", "A dung beetle navigates by the stars. Yes, by the Milky Way!"),
    "slak": ("Een slak heeft duizenden piepkleine tandjes.", "A snail has thousands of tiny teeth."),
    "vogelspin": ("Een vogelspin is pluizig als een knuffel, maar knuffelen vindt hij niks.", "A tarantula is fluffy like a plushie, but it doesn't like cuddles."),
    "hommel": ("Een hommel bromt zo hard omdat hij zijn vleugels warm trilt.", "A bumblebee buzzes so loud because it shivers its wings warm."),
    "kiwi": ("Een kiwi is de enige vogel met neusgaatjes aan het puntje van zijn snavel.", "A kiwi is the only bird with nostrils at the tip of its beak."),
    "schubdier": ("Een schubdier rolt zich op tot een bal als hij schrikt. Een levend dennenappeltje!", "A pangolin rolls into a ball when it's scared. A living pine cone!"),
    "spookdiertje": ("Elk oog van een spookdiertje is groter dan zijn hersentjes.", "Each eye of a tarsier is bigger than its brain."),
    "plompe_lori": ("De plompe lori doet alles heel, heel, heel langzaam. Vads tot op het bot.", "The slow loris does everything very, very, very slowly. Chonk to the bone."),
    "fruitvleermuis": ("Fruitvleermuizen slapen ondersteboven, ingepakt in hun vleugels als een dekentje.", "Fruit bats sleep upside down, wrapped in their wings like a blanket."),
    "galago": ("Een galago springt wel twee meter hoog. Boing!", "A bushbaby jumps up to two metres high. Boing!"),
    "suikereekhoorn": ("Een suikereekhoorn zweeft van boom naar boom met een vliesje tussen zijn pootjes.", "A sugar glider glides from tree to tree with a skin flap between its legs."),
    "fennek": ("Met zijn reuzenoren hoort een fennek zelfs een kevertje onder het zand lopen.", "With its giant ears a fennec fox can even hear a beetle walking under the sand."),
    "zeepaardje": ("Bij zeepaardjes krijgt de papa de baby's. Fijn voor mama!", "With seahorses, the dad has the babies. Nice for mum!"),
    "pijlstaartrog": ("Een pijlstaartrog 'vliegt' onder water met zijn grote vinnen.", "A stingray 'flies' under water with its big fins."),
    "zeeschildpad": ("Zeeschildpadden zwemmen duizenden kilometers en vinden altijd terug naar hun strand.", "Sea turtles swim thousands of kilometres and always find their way back to their beach."),
    "clownvis": ("Een clownvisje woont veilig tussen de prikarmpjes van een anemoon.", "A clownfish lives safely among the stinging arms of an anemone."),
    "kwal": ("Kwallen hebben geen hersenen, geen hart en geen botten. Wel heel mooi.", "Jellyfish have no brain, no heart and no bones. Very pretty though."),
    "kogelvis": ("Een kogelvis blaast zich op tot een stekelbal als hij schrikt.", "A pufferfish puffs up into a spiky ball when it's startled."),
    "zeester": ("Een zeester kan een armpje terug laten groeien. En de zee-egeltjes zijn zijn beste vriendjes.", "A starfish can grow back an arm. And the sea urchins are its best friends."),
    "babyhaai": ("Haaien hebben rijen tandjes: valt er een uit, dan schuift de volgende naar voren.", "Sharks have rows of teeth: when one falls out, the next one moves forward."),
    "regenboogvis": ("Regenboogvisjes zwemmen in een school: samen draaien ze als één grote vis.", "Rainbowfish swim in a school: together they turn like one big fish."),
    "lantaarnvisje": ("Een lantaarnvisje maakt zijn eigen licht, diep in de donkere zee.", "A lanternfish makes its own light, deep in the dark sea."),
    "dumbo_octopus": ("De Dumbo-octopus fladdert met zijn oortjes, net als de olifant uit de film.", "The Dumbo octopus flaps its little ears, just like the elephant from the film."),
    "pelikaan": ("In de keelzak van een pelikaan past meer dan in zijn maag. Slokop!", "A pelican's throat pouch holds more than its stomach. Gobble!"),
}


def soorten():
    return json.load(open(SOORTEN_JSON, encoding="utf-8"))


def data():
    uit = {}
    for soort, s in soorten().items():
        klasse = s.get("bord") or ("beloning" if soort == "gouden_dikdik" else None)   # (the full-dex reward: never sold)
        if not klasse:
            continue
        eisen, voer, cadeau, speeltje, vrienden, taken = DIER[soort]
        e = []
        for eid in eisen:
            soortEis, ids, n = EISEN[eid]
            regel = {"id": eid, "soort": soortEis, "min": n}
            if soortEis == "blok":
                regel["blokken"] = ids
            elif soortEis == "tag":
                regel["tag"] = ids
            e.append(regel)
        uit[soort] = {"klasse": klasse, "prijs": PRIJS.get(klasse, 0), "voer": VOER[voer], "eisen": e, "cadeaus": CADEAU[cadeau],
                      "speeltje": speeltje, "vrienden": vrienden, "taken": taken, **SPECIAAL.get(soort, {})}
        if s.get("terrarium"):
            uit[soort]["terrarium"] = True        # round 4 (L4): a terrariumbord (a glass tank at home)
    return {"uitleg": "S5 verblijfsbord (tools/features/safari_verblijf.py): per species with a board its price, food (vanilla "
                      "items; the tag guhs:safarivoer counts double), what makes it happy (block counts in its home), gifts, "
                      "toy, friends ('*' = everyone), specials and helper tasks (the Bouwopdracht tab, coming later).",
            "soorten": uit}


# --- the board model + per-species textures -------------------------------------------------------------------------------
HOUT = (176, 128, 80)
CREME = (250, 242, 218)


def planken(c, stap=8):
    def f(W, H, rng):
        a = bd.plain(c, 8)(W, H, rng).astype(np.float32)
        for y in range(stap - 1, H, stap):
            a[y, :, :3] *= 0.72
        return np.clip(a, 0, 255).astype(np.uint8)
    return f


def bord_model():
    """The phase-0 verblijfsbord (safari-ontwerp/objecten): a post, a board, a panel with the portrait, a little green roof."""
    bd.ATLAS, bd.S = 128, 4
    atlas = bd.Atlas(14)
    hout = planken(HOUT, 6)
    paneel = bd.cube(atlas, [-10, 15, -1.75], [20, 12, 0.5], bd.plain(CREME, 3))
    b = [bd.cube(atlas, [-1.5, 0, -1.5], [3, 22, 3], hout),
         bd.cube(atlas, [-11, 14, -1.5], [22, 14, 1.5], hout),
         paneel,
         bd.cube(atlas, [-12.5, 28, -2.5], [25, 1.5, 4], bd.plain((112, 150, 82), 5)),
         bd.cube(atlas, [-7, 29.5, -2], [14, 1.5, 3], bd.plain((112, 150, 82), 5)),
         bd.cube(atlas, [-4, 0, -4], [8, 1.5, 8], bd.plain((150, 140, 128), 6))]
    bones = [bd.bone("root", None, [0, 0, 0], []), bd.bone("bord", "root", [0, 0, 0], b)]
    g = bd.geo("safari/verblijfsbord", bones, 2, 2)
    plek = paneel["uv"]["north"]
    return g, atlas.img.copy(), plek


def font(n, bold=True):
    for naam in (("arialbd.ttf" if bold else "arial.ttf"), "DejaVuSans-Bold.ttf", "DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(naam, n)
        except OSError:
            pass
    return ImageFont.load_default()


def portret(soort, naam):
    """The animal in three-quarter view (from the shipped model), fitted on a sunny little panel, with its name."""
    import wiki_renders as wr
    geo = os.path.join(A, "geckolib", "models", "entity", "safari", f"{soort}.geo.json")
    tex = np.asarray(Image.open(os.path.join(A, "textures", "entity", "safari", f"{soort}.png")).convert("RGBA")).astype(np.float32)
    quads = wr.geo_quads(geo, tex)
    img = wr.render(quads, yaw=28, pitch=-8, size=200, margin=0.02, ss=2)
    bbox = img.getbbox()
    return img.crop(bbox) if bbox else img


def paneel(dier, naam, W, H):
    """The board's panel W x H texels: sky, grass, the animal, its name in a strip; painted mirrored (GeckoLib mirrors x)."""
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    strip = max(10, H // 4)
    for y in range(H - strip):                                  # a soft sky going into sand/grass at the bottom
        t = y / max(1, H - strip)
        c = tuple(int(a + (b - a) * t) for a, b in zip((198, 230, 248), (246, 236, 200)))
        d.line([(0, y), (W, y)], fill=c + (255,))
    d.rectangle([0, H - strip - 4, W, H - strip], fill=(150, 190, 110, 255))
    d.rectangle([0, H - strip, W, H], fill=(70, 110, 52, 255))
    vak_w, vak_h = W - 6, H - strip - 4
    s = min(vak_w / dier.width, vak_h / dier.height)
    pw, ph = max(1, int(dier.width * s)), max(1, int(dier.height * s))
    klein = dier.resize((pw, ph), Image.LANCZOS)
    img.alpha_composite(klein, ((W - pw) // 2, H - strip - 2 - ph))
    grootte = strip - 2
    f = font(grootte)
    while grootte > 6 and d.textlength(naam, font=f) > W - 4:
        grootte -= 1
        f = font(grootte)
    tw = d.textlength(naam, font=f)
    bb = d.textbbox((0, 0), naam, font=f)
    d.text(((W - tw) / 2, H - strip + (strip - (bb[3] - bb[1])) / 2 - bb[1]), naam, font=f, fill=(255, 248, 226, 255))
    d.rectangle([0, 0, W - 1, H - 1], outline=(70, 110, 52, 255), width=2)
    return img.transpose(Image.FLIP_LEFT_RIGHT)


# --- the toys, the stork's mailbox, the giant egg ------------------------------------------------------------------------
def _el(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), rot=None):
    e = {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in faces}}
    if rot:
        e["rotation"] = rot
    return e


SPEELTJES = {  # id: (nl, en, textures, elements)
    "speelbal": ("Speelbal", "Play ball", {"a": "guhs:block/safari_speelbal", "particle": "guhs:block/safari_speelbal"},
                 [_el([4, 0, 4], [12, 8, 12], "#a"), _el([5, 8, 5], [11, 9, 11], "#a"), _el([3, 1, 5], [4, 7, 11], "#a"),
                  _el([12, 1, 5], [13, 7, 11], "#a"), _el([5, 1, 3], [11, 7, 4], "#a"), _el([5, 1, 12], [11, 7, 13], "#a")]),
    "glijbaantje": ("Glijbaantje", "Little slide", {"a": "guhs:block/safari_glijbaantje", "b": "minecraft:block/stripped_oak_log",
                                                     "particle": "guhs:block/safari_glijbaantje"},
                    [_el([1, 0, 1], [3, 12, 3], "#b"), _el([13, 0, 1], [15, 12, 3], "#b"), _el([1, 11, 1], [15, 12, 5], "#b"),
                     _el([3, 0, 4], [13, 2, 15], "#a"), _el([3, 2, 4], [13, 4, 12], "#a"), _el([3, 4, 4], [13, 6, 9], "#a"),
                     _el([3, 6, 4], [13, 8, 7], "#a"), _el([3, 8, 4], [13, 11, 5], "#a")]),
    "krabboom": ("Krabboom", "Scratching tree", {"a": "guhs:block/safari_krabboom", "b": "minecraft:block/pink_wool",
                                                  "particle": "guhs:block/safari_krabboom"},
                 [_el([2, 0, 2], [14, 2, 14], "#b"), _el([6, 2, 6], [10, 14, 10], "#a"), _el([3, 14, 3], [13, 16, 13], "#b")]),
    "schommelband": ("Schommelband", "Tyre swing", {"a": "guhs:block/safari_schommelband", "b": "minecraft:block/oak_log",
                                                     "c": "minecraft:block/brown_wool", "particle": "guhs:block/safari_schommelband"},
                     [_el([0, 14, 7], [16, 16, 9], "#b"), _el([7.5, 6, 7.5], [8.5, 14, 8.5], "#c"),
                      _el([3, 1, 3], [13, 6, 5], "#a"), _el([3, 1, 11], [13, 6, 13], "#a"), _el([3, 1, 5], [5, 6, 11], "#a"),
                      _el([11, 1, 5], [13, 6, 11], "#a")]),
}


def tex_speelbal():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            band = (x + y) // 4 % 3
            img.putpixel((x, y), ((246, 120, 160), (255, 220, 90), (120, 190, 240))[band] + (255,))
    return img


def tex_effen(c, var=10, seed=1, streep=None):
    rng = np.random.default_rng(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = int(rng.integers(-var, var + 1))
            cc = c if not (streep and y % streep == 0) else tuple(max(0, k - 40) for k in c)
            img.putpixel((x, y), tuple(max(0, min(255, k + v)) for k in cc) + (255,))
    return img


def tex_band():
    img = tex_effen((40, 40, 46), 6, 3)
    d = ImageDraw.Draw(img)
    for x in range(0, 16, 4):
        d.line([(x, 0), (x + 3, 15)], fill=(60, 60, 70, 255))
    return img


def tex_bus():
    img = tex_effen((250, 250, 252), 6, 5)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 4, 13, 11], fill=(220, 60, 70, 255))
    d.rectangle([3, 5, 12, 10], fill=(248, 210, 220, 255))
    d.line([(3, 5), (7, 8), (12, 5)], fill=(220, 60, 70, 255))
    d.rectangle([0, 0, 15, 15], outline=(160, 160, 170, 255))
    return img


def icon_ei():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([2, 0, 13, 15], fill=(250, 244, 226, 255), outline=(190, 176, 150, 255))
    for x, y in ((5, 5), (9, 8), (6, 11), (10, 3)):
        img.putpixel((x, y), (214, 200, 168, 255))
    img.putpixel((5, 3), (255, 255, 255, 255))
    img.putpixel((4, 4), (255, 255, 255, 255))
    return img


def icon_omelet():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([0, 4, 15, 14], fill=(255, 214, 90, 255), outline=(220, 160, 50, 255))
    d.ellipse([5, 6, 10, 10], fill=(255, 240, 160, 255))
    for x, y in ((3, 8), (12, 9), (8, 12)):
        img.putpixel((x, y), (90, 160, 70, 255))
    return img


LANG = {  # key: (en, nl)
    "block.guhs.safari_verblijfsbord": ("Enclosure Sign", "Verblijfsbord"),
    "item.guhs.safari_verblijfsbord": ("Enclosure Sign", "Verblijfsbord"),
    "item.guhs.safari_verblijfsbord.met": ("Enclosure Sign: %s", "Verblijfsbord: %s"),
    "item.guhs.safari_verblijfsbord.lore": ("Place it and a family moves in: a male, a female and two little ones. Vahoeg!",
                                            "Zet hem neer en er komt een familie wonen: een mannetje, een vrouwtje en twee kleintjes. Vahoeg!"),
    "item.guhs.safari_verblijfsbord.leeg": ("(no animal: get one in the Safaripark gift shop)", "(geen dier: haal er een in de safari-giftshop)"),
    "entity.guhs.safari_thuisdier": ("Animal at home", "Dier thuis"),
    # round 4 (L4): the terrariumbord (small Kriebelhuis creatures live in a glass tank at home)
    "item.guhs.safari_verblijfsbord.terrarium_met": ("Terrarium Sign: %s", "Terrariumbord: %s"),
    "item.guhs.safari_verblijfsbord.terrarium_lore": ("Place it and a little glass tank appears, with the creatures living inside. Cosy indoors too!",
                                                      "Zet hem neer en er verschijnt een glazen bakje waarin de beestjes wonen. Ook knus binnen!"),
    "block.guhs.safari_terrarium": ("Terrarium", "Terrarium"),
    "gui.guhs.verblijf.terrarium.welkom": ("%s moved into the terrarium! Vahoeg!", "%s woont nu in het terrarium! Vahoeg!"),
    "item.guhs.safari_reuzen_ei": ("Giant Egg", "Reuzen-ei"),
    "item.guhs.safari_reuzen_ei.lore": ("From your ostrich! Bake it in a furnace.", "Van je struisvogel! Bak hem in een oven."),
    "item.guhs.safari_reuzen_omelet": ("Giant Omelette", "Reuzen-omelet"),
    "item.guhs.safari_reuzen_omelet.lore": ("Enough for the whole family. Njeg!", "Genoeg voor de hele familie. Njeg!"),
    "block.guhs.safari_ooievaarsbus": ("Stork Mailbox", "Ooievaarsbus"),
    "item.guhs.safari_ooievaarsbus": ("Stork Mailbox", "Ooievaarsbus"),
    "block.guhs.safari_ooievaarsbus.lore": ("Friends' storks drop their parcels in here.", "Hier leggen de ooievaars van vrienden hun pakketjes in."),
    "gui.guhs.verblijf.bus.titel": ("Stork mailbox of %s", "Ooievaarsbus van %s"),
    "gui.guhs.verblijf.bus.niet_van_jou": ("This mailbox belongs to %s.", "Deze ooievaarsbus is van %s."),
    # the board's screen
    "gui.guhs.verblijf.titel": ("Enclosure: %s", "Verblijf: %s"),
    "gui.guhs.verblijf.van": ("of %s", "van %s"),
    "gui.guhs.verblijf.alleen_kijken": ("(you can only look)", "(je mag alleen kijken)"),
    "gui.guhs.verblijf.tab.familie": ("Family", "Familie"),
    "gui.guhs.verblijf.tab.verblijf": ("Home", "Verblijf"),
    "gui.guhs.verblijf.tab.tevreden": ("Happiness", "Tevredenheid"),
    "gui.guhs.verblijf.tab.cadeautjes": ("Gifts", "Cadeautjes"),
    "gui.guhs.verblijf.tab.info": ("Info", "Info"),
    "gui.guhs.verblijf.tab.bouw": ("Building job", "Bouwopdracht"),
    "gui.guhs.verblijf.tab.bezorgen": ("Deliver", "Bezorgen"),
    "gui.guhs.verblijf.man": ("Male", "Mannetje"),
    "gui.guhs.verblijf.vrouw": ("Female", "Vrouwtje"),
    "gui.guhs.verblijf.baby": ("Little one", "Kleintje"),
    "gui.guhs.verblijf.weg": ("(away)", "(weg)"),
    "gui.guhs.verblijf.hernoem": ("Name", "Naam"),
    "gui.guhs.verblijf.terugroepen": ("Call back", "Terugroepen"),
    "gui.guhs.verblijf.verstoppen": ("Hide for a moment", "Even verstoppen"),
    "gui.guhs.verblijf.stemming.blij": ("happy", "blij"),
    "gui.guhs.verblijf.stemming.tevreden": ("content", "tevreden"),
    "gui.guhs.verblijf.stemming.mopperig": ("a bit grumpy", "beetje mopperig"),
    "gui.guhs.verblijf.stemming.slaapt": ("asleep", "slaapt"),
    "gui.guhs.verblijf.straal": ("Roaming range: %s", "Zwerfgebied: %s"),
    "gui.guhs.verblijf.vorm.cirkel": ("Shape: circle", "Vorm: cirkel"),
    "gui.guhs.verblijf.vorm.rechthoek": ("Shape: rectangle", "Vorm: rechthoek"),
    "gui.guhs.verblijf.breedte": ("Width: %s", "Breedte: %s"),
    "gui.guhs.verblijf.diepte": ("Depth: %s", "Diepte: %s"),
    "gui.guhs.verblijf.rand": ("Show the edge", "Rand tonen"),
    "gui.guhs.verblijf.thuisplek": ("Sleeping spot: here", "Slaapplek: hier"),
    "gui.guhs.verblijf.thuisplek.weg": ("No sleeping spot", "Geen slaapplek"),
    "gui.guhs.verblijf.thuisplek.is": ("Sleeping spot at %s", "Slaapplek bij %s"),
    "gui.guhs.verblijf.thuisplek.te_ver": ("Stand inside the enclosure to set the sleeping spot.", "Ga in het verblijf staan om de slaapplek te kiezen."),
    "gui.guhs.verblijf.verblijf.uitleg": ("The animals always stay inside this area, even without a fence. You may build one yourself, of course!",
                                          "De dieren blijven altijd binnen dit gebied, ook zonder hek. Een hek bouwen mag natuurlijk wel!"),
    "gui.guhs.verblijf.tevreden.balk": ("Happiness: %s%%", "Tevredenheid: %s%%"),
    "gui.guhs.verblijf.tevreden.ruimte": ("Room to roam", "Ruimte om te zwerven"),
    "gui.guhs.verblijf.tevreden.gevoerd": ("Fed today", "Vandaag gevoerd"),
    "gui.guhs.verblijf.tevreden.geaaid": ("Petted today", "Vandaag geaaid"),
    "gui.guhs.verblijf.tevreden.speeltje": ("A %s nearby", "Een %s in de buurt"),
    "gui.guhs.verblijf.tevreden.vrienden": ("Friends next door", "Vriendjes in de buurt"),
    "gui.guhs.verblijf.tevreden.bouw": ("Build a fitting enclosure", "Bouw passend verblijf"),
    "gui.guhs.verblijf.binnenkort": ("Coming soon! The Bouwkeet update brings this.", "Binnenkort! Dit komt met de Bouwkeet-update."),
    "gui.guhs.verblijf.voer": ("Likes to eat: %s (Safari food counts double)", "Eet graag: %s (safarivoer telt dubbel)"),
    "gui.guhs.verblijf.cadeautjes.uitleg": ("Happy animals sometimes leave a little gift here.", "Blije dieren laten hier soms een cadeautje achter."),
    "gui.guhs.verblijf.cadeautjes.pak": ("Take everything", "Alles pakken"),
    "gui.guhs.verblijf.cadeautjes.leeg": ("No gifts yet. Keep them happy!", "Nog geen cadeautjes. Maak ze blij!"),
    "gui.guhs.verblijf.info.sterren": ("Your best photo: %s", "Je beste foto: %s"),
    "gui.guhs.verblijf.info.geen_foto": ("No photo in your Safari-dex yet.", "Nog geen foto in je Safari-dex."),
    "gui.guhs.verblijf.info.cooldown": ("Next sign of this kind: in %s", "Volgende bord van deze soort: over %s"),
    "gui.guhs.verblijf.info.cooldown.nu": ("You may buy another sign of this kind.", "Je mag weer een bord van deze soort kopen."),
    "gui.guhs.verblijf.info.dagen": ("%s days", "%s dagen"),
    "gui.guhs.verblijf.info.uren": ("%s hours", "%s uur"),
    "gui.guhs.verblijf.bouw.uitleg": ("Later this animal can build for you. What it will be able to do:",
                                      "Straks kan dit dier voor je bouwen. Wat het gaat kunnen:"),
    "gui.guhs.verblijf.bouw.geen": ("This animal is not a builder. It is very good at being cute, though.",
                                    "Dit dier is geen bouwer. Wel heel goed in schattig zijn."),
    "gui.guhs.verblijf.bezorgen.uitleg": ("Your stork takes up to 9 stacks from the chest next to this sign to a friend's Stork Mailbox.",
                                          "Je ooievaar brengt tot 9 stapels uit de kist naast dit bord naar de Ooievaarsbus van een vriend."),
    "gui.guhs.verblijf.bezorgen.naar": ("To: %s", "Naar: %s"),
    "gui.guhs.verblijf.bezorgen.niemand": ("Nobody has a Stork Mailbox yet.", "Nog niemand heeft een Ooievaarsbus."),
    "gui.guhs.verblijf.bezorgen.stuur": ("Send the parcel!", "Pakketje versturen!"),
    "gui.guhs.verblijf.bezorgen.onderweg": ("The stork is on its way...", "De ooievaar is onderweg..."),
    "gui.guhs.verblijf.bezorgen.geen_kist": ("Put a chest right next to the sign (with something in it).", "Zet een kist vlak naast het bord (met iets erin)."),
    "gui.guhs.verblijf.bezorgen.geen_bus": ("That friend has no Stork Mailbox (any more).", "Die vriend heeft (geen) Ooievaarsbus (meer)."),
    "gui.guhs.verblijf.bezorgen.vol": ("The mailbox of %s is full: the stork brought the parcel back.", "De ooievaarsbus van %s zit vol: de ooievaar bracht het pakketje terug."),
    "gui.guhs.verblijf.bezorgen.klaar": ("Your stork delivered the parcel to %s. Klepper klepper!", "Je ooievaar heeft het pakketje bij %s bezorgd. Klepper klepper!"),
    "gui.guhs.verblijf.bezorgen.ontvangen": ("A stork from %s dropped a parcel in your Stork Mailbox!", "Een ooievaar van %s legde een pakketje in je Ooievaarsbus!"),
    "gui.guhs.verblijf.sluit": ("Done", "Klaar"),
    # messages
    "gui.guhs.verblijf.welkom": ("Welcome home, %s family! Vahoeg!", "Welkom thuis, familie %s! Vahoeg!"),
    "gui.guhs.verblijf.terug_park": ("The animals went back to the safari park. Bye bye!", "De dieren zijn terug naar het safaripark. Doei doei!"),
    "gui.guhs.verblijf.niet_jouw": ("This sign belongs to %s.", "Dit bord is van %s."),
    "gui.guhs.verblijf.water_nodig": ("A %s needs water: at least %s water blocks around the sign.", "Een %s heeft water nodig: minstens %s waterblokken rond het bord."),
    "gui.guhs.verblijf.niet_in_park": ("Signs belong at home, not in the safari park.", "Borden horen thuis, niet in het safaripark."),
    "gui.guhs.verblijf.geaaid": ("%s loves this! <3", "%s vindt dit heerlijk! <3"),
    "gui.guhs.verblijf.gevoerd": ("Nom nom! %s is happy.", "Nomnom! %s is blij."),
    "gui.guhs.verblijf.vol": ("%s has had enough for today.", "%s heeft vandaag genoeg gehad."),
    "gui.guhs.verblijf.ei": ("Your ostrich laid a Giant Egg!", "Je struisvogel heeft een reuzen-ei gelegd!"),
    "gui.guhs.verblijf.rijden": ("Sneak + right-click to ride. Hold on tight!", "Sluip + rechtsklik om te rijden. Hou je vast!"),
    # the shop API (S2 shows these)
    "gui.guhs.verblijf.koop.geen_foto": ("Photograph a %s in the park first.", "Fotografeer eerst een %s in het park."),
    "gui.guhs.verblijf.koop.cooldown": ("You can buy another %s sign in %s.", "Je kunt over %2$s weer een bord voor %1$s kopen."),
    "gui.guhs.verblijf.koop.munten": ("You need %s Safari coins.", "Je hebt %s Safarimunten nodig."),
    "gui.guhs.verblijf.koop.gekocht": ("Enclosure sign bought: %s! Place it at home.", "Verblijfsbord gekocht: %s! Zet hem thuis neer."),
    "gui.guhs.verblijf.koop.geen_bord": ("There is no sign for this animal.", "Voor dit dier is er geen bord."),
}


def build(h):
    w = h.w
    d = data()
    w(f"{D}/safari/verblijf.json", d)
    # the board: GeckoLib draws it all (the blockstate only gives the particles)
    g, atlas, plek = bord_model()
    w(f"{A}/geckolib/models/block/safari/verblijfsbord.geo.json", g)
    w(f"{A}/geckolib/animations/block/safari/verblijfsbord.animation.json", {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 1.0, "bones": {}}}})   # (GeckoLib refuses a file without animations)
    w(f"{A}/blockstates/safari_verblijfsbord.json", {"variants": {
        f"facing={f}": {"model": "guhs:block/safari_verblijfsbord"} for f in ("north", "south", "east", "west")}})
    w(f"{A}/models/block/safari_verblijfsbord.json", {"textures": {"particle": "minecraft:block/stripped_oak_log"}})
    w(f"{A}/models/item/safari_verblijfsbord.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/safari_verblijfsbord"}})
    namen = {}
    try:
        from features.safari import NAMEN as namen
    except Exception:                                                     # (safari.py always has them; just in case)
        pass
    (u, v), (pw, ph) = plek["uv"], plek["uv_size"]
    S = 4
    tex_map = os.path.join(A, "textures", "block", "safari")
    os.makedirs(tex_map, exist_ok=True)
    for soort in d["soorten"]:
        uit = os.path.join(tex_map, f"verblijfsbord_{soort}.png")
        if os.path.exists(uit) and not OPNIEUW:
            continue
        naam = namen.get(soort, soort)
        img = Image.fromarray(atlas, "RGBA")
        img.paste(paneel(portret(soort, naam), naam, int(pw * S), int(ph * S)), (int(u * S), int(v * S)))
        img.save(uit)
    # the item icon: a little sign on a post
    icoon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    di = ImageDraw.Draw(icoon)
    di.rectangle([7, 8, 8, 15], fill=(150, 104, 62, 255))
    di.rectangle([1, 2, 14, 10], fill=(176, 128, 80, 255), outline=(120, 84, 50, 255))
    di.rectangle([3, 4, 12, 8], fill=(250, 242, 218, 255))
    di.ellipse([5, 4, 9, 8], fill=(214, 128, 56, 255))
    di.rectangle([0, 1, 15, 1], fill=(112, 150, 82, 255))
    h.save(icoon, "item", "safari_verblijfsbord.png")
    # toys
    for sid, (nl, en, tex, els) in SPEELTJES.items():
        w(f"{A}/models/block/safari_{sid}.json", {"parent": "minecraft:block/block", "textures": tex, "elements": els, "render_type": "minecraft:cutout"})
        w(f"{A}/blockstates/safari_{sid}.json", {"variants": {"": {"model": f"guhs:block/safari_{sid}"}}})
        w(f"{A}/models/item/safari_{sid}.json", {"parent": f"guhs:block/safari_{sid}"})
        h.self_drop(f"safari_{sid}")
        h.lang(f"block.guhs.safari_{sid}", en, nl)
        h.lang(f"item.guhs.safari_{sid}", en, nl)
        h.lang(f"block.guhs.safari_{sid}.lore", "A toy for your animals at home: they play with it and get happier.",
               "Een speeltje voor je dieren thuis: ze spelen ermee en worden blijer.")
    h.save(tex_speelbal(), "block", "safari_speelbal.png")
    h.save(tex_effen((120, 200, 236), 6, 2, streep=4), "block", "safari_glijbaantje.png")
    h.save(tex_effen((206, 176, 130), 10, 4, streep=3), "block", "safari_krabboom.png")
    h.save(tex_band(), "block", "safari_schommelband.png")
    h.shaped("safari_speelbal", [" W ", "WSW", " W "], {"W": "#minecraft:wool", "S": "minecraft:slime_ball"}, "guhs:safari_speelbal")
    h.shaped("safari_glijbaantje", ["  P", " PS", "PSS"], {"P": "#minecraft:planks", "S": "minecraft:smooth_stone_slab"}, "guhs:safari_glijbaantje")
    h.shaped("safari_krabboom", ["W", "L", "W"], {"W": "#minecraft:wool", "L": "#minecraft:logs"}, "guhs:safari_krabboom")
    h.shaped("safari_schommelband", ["LLL", " S ", " K "], {"L": "#minecraft:logs", "S": "minecraft:string", "K": "minecraft:black_dye"}, "guhs:safari_schommelband")
    # the stork's mailbox
    w(f"{A}/models/block/safari_ooievaarsbus.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"a": "guhs:block/safari_ooievaarsbus", "b": "minecraft:block/stripped_spruce_log", "particle": "guhs:block/safari_ooievaarsbus"},
        "elements": [_el([7, 0, 7], [9, 9, 9], "#b"), _el([3, 9, 4], [13, 16, 12], "#a")]})
    w(f"{A}/blockstates/safari_ooievaarsbus.json", {"variants": {"": {"model": "guhs:block/safari_ooievaarsbus"}}})
    w(f"{A}/models/item/safari_ooievaarsbus.json", {"parent": "guhs:block/safari_ooievaarsbus"})
    h.save(tex_bus(), "block", "safari_ooievaarsbus.png")
    h.self_drop("safari_ooievaarsbus")
    h.shaped("safari_ooievaarsbus", ["PPP", "PCP", " S "], {"P": "#minecraft:planks", "C": "minecraft:chest", "S": "minecraft:stick"}, "guhs:safari_ooievaarsbus")
    # the giant egg and omelette
    h.save(icon_ei(), "item", "safari_reuzen_ei.png")
    h.save(icon_omelet(), "item", "safari_reuzen_omelet.png")
    h.item_model("safari_reuzen_ei")
    h.item_model("safari_reuzen_omelet")
    for soort_recept in ("smelting", "smoking", "campfire_cooking"):
        w(f"{D}/recipe/safari_reuzen_omelet_{soort_recept}.json", {"type": f"minecraft:{soort_recept}", "category": "food",
            "ingredient": "guhs:safari_reuzen_ei", "result": {"id": "guhs:safari_reuzen_omelet"}, "experience": 0.5,
            "cookingtime": {"smelting": 200, "smoking": 100, "campfire_cooking": 600}[soort_recept]})
    # round 4 (L4): the terrarium (a terrariumbord placed at home): a glass tank with a wooden frame, a sandy-mossy floor,
    # a little branch and a name plate; the little creature lives inside. One model for every species (cutout glass).
    terrarium_blok(h)
    # the sign drops itself with its species (Java: getDrops); the loot table stays empty
    w(f"{D}/loot_table/blocks/safari_verblijfsbord.json", {"type": "minecraft:block", "pools": []})
    w(f"{D}/loot_table/entities/safari_thuisdier.json", {"type": "minecraft:entity", "pools": []})
    for key, (en, nl) in LANG.items():
        h.lang(key, en, nl)
    for eid, (nl, en) in EIS_NAMEN.items():
        h.lang(f"gui.guhs.verblijf.eis.{eid}", en, nl)
    for tid, (nl, en) in TAKEN.items():
        h.lang(f"gui.guhs.verblijf.taak.{tid}", en, nl)
    for sid, (nl, en, _, _) in SPEELTJES.items():
        h.lang(f"gui.guhs.verblijf.speeltje.{sid}", en.lower(), nl.lower())
    for soort, (nl, en) in FEITEN.items():
        h.lang(f"gui.guhs.verblijf.feit.{soort}", en, nl)


def tex_terrarium_glas():
    """16x16: a clear pane with a few shine streaks inside a wooden frame (the frame only on the edges)."""
    img = Image.new("RGBA", (16, 16), (210, 236, 246, 46))
    d = ImageDraw.Draw(img)
    for i in range(3):
        d.line([(3 + i * 5, 12), (7 + i * 5, 4)], fill=(255, 255, 255, 110))
    d.rectangle([0, 0, 15, 1], fill=(150, 104, 62, 255))
    d.rectangle([0, 14, 15, 15], fill=(132, 90, 52, 255))
    d.rectangle([0, 0, 1, 15], fill=(150, 104, 62, 255))
    d.rectangle([14, 0, 15, 15], fill=(150, 104, 62, 255))
    return img


def tex_terrarium_bodem():
    """16x16: sand with moss patches and a few pebbles."""
    rng = np.random.default_rng(16)
    a = np.zeros((16, 16, 4), np.uint8)
    a[..., :3] = np.clip(np.array((214, 190, 140)) + rng.normal(0, 8, (16, 16, 1)), 0, 255)
    a[..., 3] = 255
    for (x, y, r) in ((4, 4, 3), (11, 10, 3), (3, 12, 2)):
        for yy in range(16):
            for xx in range(16):
                if (xx - x) ** 2 + (yy - y) ** 2 <= r * r:
                    a[yy, xx, :3] = np.clip(np.array((92, 140, 60)) + rng.normal(0, 10, 3), 0, 255)
    for (x, y) in ((8, 3), (13, 5), (7, 13)):
        a[y, x, :3] = (150, 150, 146)
    return Image.fromarray(a, "RGBA")


def terrarium_blok(h):
    w = h.w
    hout = "minecraft:block/stripped_oak_log"
    w(f"{A}/models/block/safari_terrarium.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"glas": "guhs:block/safari_terrarium_glas", "bodem": "guhs:block/safari_terrarium_bodem", "hout": hout,
                     "particle": "guhs:block/safari_terrarium_glas"},
        "elements": [
            _el([0, 0, 0], [16, 2, 16], "#hout", ("north", "south", "east", "west", "down")),
            _el([0.5, 2, 0.5], [15.5, 2.5, 15.5], "#bodem", ("up",)),
            _el([0, 2, 0], [16, 15, 0.5], "#glas", ("north", "south")),
            _el([0, 2, 15.5], [16, 15, 16], "#glas", ("north", "south")),
            _el([0, 2, 0], [0.5, 15, 16], "#glas", ("east", "west")),
            _el([15.5, 2, 0], [16, 15, 16], "#glas", ("east", "west")),
            _el([0, 15, 0], [16, 16, 16], "#hout", ("north", "south", "east", "west", "up", "down")),
            _el([3, 2.5, 9], [13, 3.5, 10], "#hout", ("north", "south", "east", "west", "up"), {"angle": 22.5, "axis": "y", "origin": [8, 3, 9.5]}),
        ]})
    w(f"{A}/blockstates/safari_terrarium.json", {"variants": {
        f"facing={f}": {"model": "guhs:block/safari_terrarium", **({"y": r} if r else {})}
        for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    h.save(tex_terrarium_glas(), "block", "safari_terrarium_glas.png")
    h.save(tex_terrarium_bodem(), "block", "safari_terrarium_bodem.png")
    # it drops its terrariumbord with the species (Java: getDrops)
    w(f"{D}/loot_table/blocks/safari_terrarium.json", {"type": "minecraft:block", "pools": []})


ITEMS = ["safari_verblijfsbord", "safari_speelbal", "safari_glijbaantje", "safari_krabboom", "safari_schommelband", "safari_ooievaarsbus",
         "safari_reuzen_ei", "safari_reuzen_omelet"]
EN_BESTAND = os.path.join("tools", "lang", "en", "c165_safari_verblijf.json")


class Los:
    """Running this file on its own (no make_resources: that needs the old 1.21.1 jars): the same helpers, writing the
    26.1 formats straight away (no render_type, client item definitions in items/, lang appended to nl_nl/en_us)."""
    def __init__(self):
        self.A, self.D = A, D
        self.NL, self.EN = {}, {}

    def w(self, path, obj):
        if path.endswith(".json") and "/models/" in path.replace(os.sep, "/") and isinstance(obj, dict):
            obj = dict(obj)
            obj.pop("render_type", None)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def save(self, img, *pad):
        full = os.path.join(A, "textures", *pad)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        img.save(full)

    def lang(self, key, en, nl):
        self.EN[key] = en
        self.NL[key] = nl

    def self_drop(self, name, item=None):
        self.w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{item or name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    def item_model(self, name, tex=None, parent="minecraft:item/generated"):
        self.w(f"{A}/models/item/{name}.json", {"parent": parent, "textures": {"layer0": tex or f"guhs:item/{name}"}})

    def shaped(self, name, pattern, key, result, count=1):
        res = {"id": result}
        if count != 1:
            res["count"] = count
        self.w(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": dict(key), "result": res})

    def af(self):
        for item in ITEMS:
            self.w(f"{A}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{item}"}})
        for taal, nieuw in (("nl_nl", self.NL), ("en_us", self.EN)):
            pad = os.path.join(A, "lang", f"{taal}.json")
            d = json.load(open(pad, encoding="utf-8"))
            d.update(nieuw)
            with open(pad, "w", encoding="utf-8", newline="\n") as f:
                json.dump(d, f, indent=2, ensure_ascii=False)
                f.write("\n")
        os.makedirs(os.path.dirname(EN_BESTAND), exist_ok=True)
        with open(EN_BESTAND, "w", encoding="utf-8", newline="\n") as f:
            json.dump(self.EN, f, indent=2, ensure_ascii=False)
            f.write("\n")


if __name__ == "__main__":
    los = Los()
    build(los)
    los.af()
    print(f"safari_verblijf: {len(data()['soorten'])} soorten, {len(los.NL)} texts")
