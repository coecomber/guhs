"""
Op Safari! (1.6.0) deel 2, slice F11: the new building blocks (design ontwerp-safaripark-en-bouwen.md 2.7, contract
SAFARI_CONTRACT.md "F11 blokken"; Java: feature/bouwhulp/blokken/). Blocks that help with base building:

  vormhulp     verticale platen + hoekjes (vanilla wood/stone and the guh blocks), palen + balken that connect by
               themselves, schuine daken (half and quarter slope, in parts that you stack)
  muren        pastelbakstenen + pleister in 16 colours (block, trap, plaat, muur), guh-tegels, behang with a plint,
               knuffelblokken (bouncy, squeaky; four with a safari animal pattern from safari_patronen/)
  details      guh-kozijnen (oortjes / bloembak / gordijntjes, right-click to switch), guh-luiken, glas-in-lood +
               ruiten, patrijspoorten, dakgoot, regenpijp, guh-schoorsteen (smokes), dakkapel, dakpannen
  tuin/straat  hekjes with ears + poortjes, guh-lantaarnpaal, deurmat "Welkom guh", bankjes, vogelhuisje

Writes textures (assets/guhs/textures/block/bouwhulp/), models, blockstates, client items, loot, recipes, tags, the
names (NL + EN, English also in tools/lang/en/c171_bouwhulp_blokken.json) and the block list the Java side registers
(feature/bouwhulp/blokken/BouwLijst.java, generated). Run on its own from the project root:
    python tools/features/bouwhulp_blokken.py
"""
import json
import os
import sys

import numpy as np
from PIL import Image

HIER = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join("src", "main", "resources")
A = os.path.join(RES, "assets", "guhs")
D = os.path.join(RES, "data", "guhs")
TEXDIR = os.path.join(A, "textures", "block", "bouwhulp")
T = "guhs:block/bouwhulp/"
EN_BESTAND = os.path.join("tools", "lang", "en", "c171_bouwhulp_blokken.json")
JAVA_LIJST = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "bouwhulp", "blokken", "BouwLijst.java")

rng = np.random.default_rng(1611)

# =====================================================================================================================
# the block list (also written to BouwLijst.java)
# =====================================================================================================================
# id, NL adjective, EN name, planks, stripped log (side), stripped log (top)
HOUT = [
    ("eiken", "Eiken", "Oak", "oak_planks", "stripped_oak_log"),
    ("sparren", "Sparren", "Spruce", "spruce_planks", "stripped_spruce_log"),
    ("berken", "Berken", "Birch", "birch_planks", "stripped_birch_log"),
    ("jungle", "Jungle", "Jungle", "jungle_planks", "stripped_jungle_log"),
    ("acacia", "Acacia", "Acacia", "acacia_planks", "stripped_acacia_log"),
    ("donkereiken", "Donkereiken", "Dark Oak", "dark_oak_planks", "stripped_dark_oak_log"),
    ("mangrove", "Mangrove", "Mangrove", "mangrove_planks", "stripped_mangrove_log"),
    ("kersen", "Kersen", "Cherry", "cherry_planks", "stripped_cherry_log"),
    ("bleekeiken", "Bleekeiken", "Pale Oak", "pale_oak_planks", "stripped_pale_oak_log"),
    ("bamboe", "Bamboe", "Bamboo", "bamboo_planks", "stripped_bamboo_block"),
    ("karmozijn", "Karmozijnen", "Crimson", "crimson_planks", "stripped_crimson_stem"),
    ("verwrongen", "Verwrongen", "Warped", "warped_planks", "stripped_warped_stem"),
]
# the logs you put in the stonecutter for palen / balken
HOUT_LOG = {"eiken": "oak_log", "sparren": "spruce_log", "berken": "birch_log", "jungle": "jungle_log", "acacia": "acacia_log",
            "donkereiken": "dark_oak_log", "mangrove": "mangrove_log", "kersen": "cherry_log", "bleekeiken": "pale_oak_log",
            "bamboe": "bamboo_block", "karmozijn": "crimson_stem", "verwrongen": "warped_stem"}
# id, NL, EN, vanilla block, side texture, top texture
STEEN = [
    ("steen", "Stenen", "Stone", "stone", "stone", "stone"),
    ("keien", "Keien", "Cobblestone", "cobblestone", "cobblestone", "cobblestone"),
    ("steenbaksteen", "Stenen bakstenen", "Stone Brick", "stone_bricks", "stone_bricks", "stone_bricks"),
    ("bemoste_steenbaksteen", "Bemoste stenen bakstenen", "Mossy Stone Brick", "mossy_stone_bricks", "mossy_stone_bricks", "mossy_stone_bricks"),
    ("gladde_steen", "Gladde stenen", "Smooth Stone", "smooth_stone", "smooth_stone_slab_side", "smooth_stone"),
    ("zandsteen", "Zandstenen", "Sandstone", "sandstone", "sandstone", "sandstone_top"),
    ("baksteen", "Bakstenen", "Brick", "bricks", "bricks", "bricks"),
    ("andesiet", "Gepolijst andesiet", "Polished Andesite", "polished_andesite", "polished_andesite", "polished_andesite"),
    ("dioriet", "Gepolijst dioriet", "Polished Diorite", "polished_diorite", "polished_diorite", "polished_diorite"),
    ("graniet", "Gepolijst graniet", "Polished Granite", "polished_granite", "polished_granite", "polished_granite"),
    ("diepsteenbaksteen", "Diepsteenbakstenen", "Deepslate Brick", "deepslate_bricks", "deepslate_bricks", "deepslate_bricks"),
    ("kwarts", "Kwarts", "Quartz", "quartz_block", "quartz_block_side", "quartz_block_top"),
    ("modderbaksteen", "Modderbakstenen", "Mud Brick", "mud_bricks", "mud_bricks", "mud_bricks"),
]
DAK_STEEN = ["baksteen", "steenbaksteen", "diepsteenbaksteen", "modderbaksteen"]
# the 16 pastels, one per vanilla dye (that dye makes it): id, NL, EN, dye, colour
KLEUREN = [
    ("vanille", "Vanille", "Vanilla", "white", (246, 238, 222)),
    ("wolkje", "Wolkjesgrijs", "Cloud Grey", "light_gray", (214, 214, 226)),
    ("muisgrijs", "Muisgrijs", "Mouse Grey", "gray", (170, 166, 184)),
    ("drop", "Drop", "Licorice", "black", (104, 96, 118)),
    ("mokka", "Mokka", "Mocha", "brown", (196, 156, 128)),
    ("framboos", "Framboos", "Raspberry", "red", (236, 136, 150)),
    ("perzik", "Perzik", "Peach", "orange", (250, 190, 150)),
    ("boter", "Boter", "Butter", "yellow", (250, 230, 150)),
    ("pistache", "Pistache", "Pistachio", "lime", (204, 232, 152)),
    ("salie", "Salie", "Sage", "green", (166, 200, 160)),
    ("aqua", "Aqua", "Aqua", "cyan", (150, 218, 214)),
    ("hemelsblauw", "Hemelsblauw", "Sky Blue", "light_blue", (172, 210, 246)),
    ("korenbloem", "Korenbloem", "Cornflower", "blue", (150, 168, 234)),
    ("lavendel", "Lavendel", "Lavender", "purple", (192, 170, 236)),
    ("orchidee", "Orchidee", "Orchid", "magenta", (230, 168, 228)),
    ("roze", "Roze", "Pink", "pink", (250, 182, 206)),
]
KLEUR = {k[0]: k for k in KLEUREN}
# roof tiles: id, NL, EN, terracotta (recipe), colour
DAKPANNEN = [
    ("roze", "Roze", "Pink", "pink_terracotta", (236, 140, 168)),
    ("kersrood", "Kersrode", "Cherry Red", "red_terracotta", (200, 82, 92)),
    ("hemelsblauw", "Hemelsblauwe", "Sky Blue", "light_blue_terracotta", (120, 170, 222)),
    ("mint", "Mint", "Mint", "lime_terracotta", (130, 200, 160)),
    ("lavendel", "Lavendel", "Lavender", "purple_terracotta", (160, 130, 210)),
    ("perzik", "Perzik", "Peach", "orange_terracotta", (238, 156, 108)),
]
TEGELS = [("guhgezicht", "Guhgezichttegels", "Guh Face Tiles", "Guhgezichttegelplaat", "Guh Face Tile Slab"),
          ("hartjes", "Hartjestegels", "Heart Tiles", "Hartjestegelplaat", "Heart Tile Slab"),
          ("pootjes", "Pootjestegels", "Paw Print Tiles", "Pootjestegelplaat", "Paw Print Tile Slab"),
          ("dambord", "Dambordtegels", "Checkerboard Tiles", "Dambordtegelplaat", "Checkerboard Tile Slab"),
          ("visgraat", "Visgraatvloer", "Herringbone Floor", "Visgraatvloerplaat", "Herringbone Floor Slab")]
BEHANG_PATRONEN = [("bloemetjes", "Bloemetjesbehang", "Flowery Wallpaper"), ("streepjes", "Streepjesbehang", "Striped Wallpaper"),
                   ("wolkjes", "Wolkjesbehang", "Cloudy Wallpaper"), ("guhtjes", "Guhtjesbehang", "Little Guhs Wallpaper")]
BEHANG_KLEUREN = [("roze", "roze", "Pink", (250, 214, 226), "pink_wool"), ("mint", "mint", "Mint", (206, 238, 220), "lime_wool")]
KNUFFEL_KLEUREN = ["roze", "lavendel", "aqua", "hemelsblauw", "boter", "perzik", "vanille", "wolkje"]
KNUFFEL_WOL = {"roze": "pink_wool", "lavendel": "purple_wool", "aqua": "cyan_wool", "hemelsblauw": "light_blue_wool", "boter": "yellow_wool",
               "perzik": "orange_wool", "vanille": "white_wool", "wolkje": "light_gray_wool"}
# animal patterns (tools/features/safari_patronen): id, pattern file, NL, EN, what you add to a vanilla snuggle block
KNUFFEL_PATRONEN = [("zebra", "patroon_zebra", "Zebraknuffelblok", "Zebra Snuggle Block", "black_dye"),
                    ("giraffe", "patroon_giraffe", "Giraffeknuffelblok", "Giraffe Snuggle Block", "brown_dye"),
                    ("tijger", "patroon_tijgerstreep", "Tijgerknuffelblok", "Tiger Snuggle Block", "orange_dye"),
                    ("panda", "patroon_panda", "Pandaknuffelblok", "Panda Snuggle Block", "bamboo")]
# painted wood colours (kozijnen, luiken, hekjes, poortjes): id, NL (het-word), NL (de-word), EN, dye, colour
VERF = [("wit", "Wit", "Witte", "White", "white_dye", (246, 244, 240)), ("roze", "Roze", "Roze", "Pink", "pink_dye", (246, 172, 198)),
        ("mint", "Mint", "Mint", "Mint", "lime_dye", (176, 226, 196)), ("hemelsblauw", "Hemelsblauw", "Hemelsblauwe", "Sky Blue", "light_blue_dye", (168, 206, 242))]
GLAS = [("guh", "Guh-glas-in-lood", "Guh Stained Glass", "pink_dye"), ("hartje", "Hartjesglas-in-lood", "Heart Stained Glass", "red_dye"),
        ("bloem", "Bloemetjesglas-in-lood", "Flower Stained Glass", "yellow_dye"),
        ("regenboog", "Regenboogglas-in-lood", "Rainbow Stained Glass", "amethyst_shard")]
PATRIJS = [("koper", "Koperen patrijspoort", "Copper Porthole", "copper_ingot", (214, 128, 92)),
           ("zilver", "Zilveren patrijspoort", "Silver Porthole", "iron_ingot", (212, 214, 222)),
           ("goud", "Gouden patrijspoort", "Golden Porthole", "gold_ingot", (246, 206, 92))]
DAKKAPEL = [("roze", "Roze dakkapel", "Pink Dormer", "pink_dye"), ("wit", "Witte dakkapel", "White Dormer", "white_dye")]
BANKJES = [("hout", "Houten guhbankje", "Wooden Guh Bench"), ("roze", "Roze guhbankje", "Pink Guh Bench")]

# =====================================================================================================================
# writing
# =====================================================================================================================
NL, EN = {}, {}
IDS = []           # every block id, in creative order
PICKAXE, AXE, HOE = [], [], []
TAGS = {"stairs": [], "slabs": [], "walls": [], "wooden_fences": [], "fence_gates": []}


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def naam(bid, nl, en):
    NL[f"block.guhs.{bid}"] = nl
    EN[f"block.guhs.{bid}"] = en
    IDS.append(bid)


def model(n, obj):
    w(os.path.join(A, "models", "block", n + ".json"), obj)


def blockstate(n, obj):
    w(os.path.join(A, "blockstates", n + ".json"), obj)


def item(n, m=None, sprite=None):
    """The client item: a block model (default: the block's own), or a flat sprite."""
    if sprite:
        w(os.path.join(A, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": sprite}})
    else:
        w(os.path.join(A, "models", "item", n + ".json"), {"parent": m or f"guhs:block/{n}"})
    w(os.path.join(A, "items", n + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}})


def loot(n, plaat=False, conditie=None):
    entry = {"type": "minecraft:item", "name": f"guhs:{n}"}
    if plaat:
        entry["functions"] = [{"function": "minecraft:set_count", "count": 2, "add": False,
                               "conditions": [{"condition": "minecraft:block_state_property", "block": f"guhs:{n}",
                                               "properties": {"type": "double"}}]},
                              {"function": "minecraft:explosion_decay"}]
    pool = {"rolls": 1, "bonus_rolls": 0, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}
    if conditie:
        pool["conditions"].append({"condition": "minecraft:block_state_property", "block": f"guhs:{n}", "properties": conditie})
    w(os.path.join(D, "loot_table", "blocks", n + ".json"), {"type": "minecraft:block", "pools": [pool]})


def shaped(n, pattern, key, count=1, uit=None):
    res = {"id": f"guhs:{n}"}
    if count != 1:
        res["count"] = count
    key = {k: (v if ":" in v or v.startswith("#") else "minecraft:" + v) for k, v in key.items()}
    w(os.path.join(D, "recipe", (uit or n) + ".json"),
      {"type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern, "key": key, "result": res})


def shapeless(n, ingr, count=1, uit=None):
    res = {"id": f"guhs:{n}"}
    if count != 1:
        res["count"] = count
    ingr = [v if ":" in v or v.startswith("#") else "minecraft:" + v for v in ingr]
    w(os.path.join(D, "recipe", (uit or n) + ".json"),
      {"type": "minecraft:crafting_shapeless", "category": "building", "ingredients": ingr, "result": res})


def steenzaag(van, n, count=1):
    """A stonecutter recipe (the cheap way to all the shapes of a material)."""
    van = van if ":" in van else "minecraft:" + van
    res = {"id": f"guhs:{n}"}
    if count != 1:
        res["count"] = count
    w(os.path.join(D, "recipe", f"{n}_van_{van.split(':')[1]}_zagen.json"),
      {"type": "minecraft:stonecutting", "ingredient": van, "result": res})


def tag(pad, namen):
    full = os.path.join(RES, "data", "minecraft", "tags", "block", *pad) + ".json"
    d = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
    for n in namen:
        if f"guhs:{n}" not in d["values"]:
            d["values"].append(f"guhs:{n}")
    w(full, d)


def item_tag(pad, namen):
    full = os.path.join(RES, "data", "minecraft", "tags", "item", *pad) + ".json"
    d = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
    for n in namen:
        if f"guhs:{n}" not in d["values"]:
            d["values"].append(f"guhs:{n}")
    w(full, d)


ALLE6 = ("north", "south", "east", "west", "up", "down")


def el(frm, to, faces, rot=None, licht=None):
    """An element; faces: {face: texture ref or a dict (texture, uv, rotation, cullface)}."""
    e = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": {}}
    for f, t in faces.items():
        e["faces"][f] = {"texture": t} if isinstance(t, str) else dict(t)
    if rot:
        e["rotation"] = rot
    if licht:
        e["light_emission"] = licht
    return e


def doos(frm, to, tex, cull=(), **kw):
    """A box with the same texture on all six faces (cull: faces against the block's edge)."""
    faces = {}
    for f in ALLE6:
        faces[f] = {"texture": tex, "cullface": f} if f in cull else tex
    return el(frm, to, faces, **kw)


def facing_states(m, extra="", y0=0):
    """variants facing=north/east/south/west -> the north model turned (m may be a function of the facing)."""
    out = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        v = {"model": m}
        if (y + y0) % 360:
            v["y"] = (y + y0) % 360
        out[f"facing={f}" + extra] = v
    return out


def ref(t):
    return t if ":" in t else "minecraft:block/" + t


def translucent(textures):
    return {k: {"sprite": v, "force_translucent": True} for k, v in textures.items()}


# =====================================================================================================================
# textures (16x16)
# =====================================================================================================================
def img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def vlak(c, var=5, alpha=255, h=16, b=16):
    a = np.zeros((h, b, 4), float)
    a[..., :3] = np.array(c, float) + rng.normal(0, var, (h, b, 1))
    a[..., 3] = alpha
    return a


def zacht_ruis(sterkte=8, cel=4, h=16):
    """Low-frequency noise (blotches): a coarse random grid blown up smoothly, tiling."""
    n = h // cel
    g = rng.normal(0, 1, (n, n))
    big = np.array(Image.fromarray(((g - g.min()) / (np.ptp(g) + 1e-6) * 255).astype(np.uint8)).resize((h, h), Image.BICUBIC), float)
    return (big / 255 - 0.5) * 2 * sterkte


def mix(c, d, t):
    return tuple(int(round(c[i] * (1 - t) + d[i] * t)) for i in range(3))


def put(a, x, y, c, alpha=255):
    if 0 <= x < a.shape[1] and 0 <= y < a.shape[0]:
        a[y, x, :3] = c
        a[y, x, 3] = alpha


def save(a, n, frametime=None):
    pad = os.path.join(TEXDIR, n + ".png")
    os.makedirs(TEXDIR, exist_ok=True)
    (a if isinstance(a, Image.Image) else img(a)).save(pad)
    if frametime:
        w(pad + ".mcmeta", {"animation": {"frametime": frametime}})
    return T + n


HART = ["01010", "11111", "11111", "01110", "00100"]
HART_KLEIN = ["101", "111", "010"]
POOT = [".0.0.", "0...0", "..1..", ".111.", ".111."]
GUH = [".1...1.", "11...11", "1111111", "1101011", "1111111", "1110111", ".11111."]   # ears, eyes (0), mouth


def stempel(a, x0, y0, patroon, c, c0=None):
    for dy, rij in enumerate(patroon):
        for dx, ch in enumerate(rij):
            if ch == "1":
                put(a, x0 + dx, y0 + dy, c)
            elif ch == "0" and c0 is not None:
                put(a, x0 + dx, y0 + dy, c0)


def tex_baksteen(c):
    a = vlak(c, 4)
    voeg = mix(c, (255, 252, 246), 0.62)
    for rij in range(4):
        off = 0 if rij % 2 == 0 else 4
        for steen in range(3):
            tint = rng.normal(0, 7)
            for y in range(rij * 4, rij * 4 + 3):
                for xx in range(8):
                    x = (steen * 8 + xx - off) % 16
                    if xx == 7:
                        continue
                    a[y, x, :3] = np.array(c, float) + tint + rng.normal(0, 3) + (10 if y == rij * 4 else -8 if y == rij * 4 + 2 else 0)
        for x in range(16):
            a[rij * 4 + 3, x, :3] = np.array(voeg, float) + rng.normal(0, 2)
        for y in range(rij * 4, rij * 4 + 3):
            for k in range(2):
                a[y, (k * 8 + 7 + off) % 16, :3] = np.array(voeg, float) + rng.normal(0, 2)
    return a


def tex_pleister(c):
    a = vlak(c, 2)
    a[..., :3] += zacht_ruis(6)[..., None]
    for _ in range(9):
        x, y = rng.integers(0, 16, 2)
        put(a, x, y, mix(c, (255, 255, 255), 0.35))
    for _ in range(5):
        x, y = rng.integers(0, 16, 2)
        put(a, x, y, mix(c, (90, 70, 80), 0.12))
    return a


def tex_dakpan(c):
    a = vlak(c, 4)
    for rij in range(4):
        off = 0 if rij % 2 == 0 else 4
        for x in range(16):
            lx = (x + off) % 8
            y = rij * 4 + 3
            a[y, x, :3] = np.array(mix(c, (40, 20, 30), 0.35), float)          # the shadow under each row
            if lx in (0, 7):
                a[y - 1, x, :3] = np.array(mix(c, (40, 20, 30), 0.25), float)  # rounded corners
            if lx == 0:
                a[rij * 4:rij * 4 + 3, x, :3] = np.array(mix(c, (40, 20, 30), 0.18), float)
            if lx in (2, 3):
                a[rij * 4, x, :3] = np.array(mix(c, (255, 255, 255), 0.3), float)  # a glint on every tile
    return a


def tex_tegel(soort):
    if soort == "guhgezicht":
        a = vlak((250, 244, 236), 2)
        for (x0, y0) in ((0, 0), (8, 8)):
            stempel(a, x0 + 0, y0 + 1, GUH, (246, 178, 204), (70, 50, 60))
        for (x0, y0) in ((8, 0), (0, 8)):
            stempel(a, x0 + 2, y0 + 2, HART_KLEIN, (250, 206, 220))
    elif soort == "hartjes":
        a = vlak((252, 214, 226), 2)
        for (x0, y0) in ((1, 1), (9, 9)):
            stempel(a, x0, y0, HART, (236, 110, 146))
        for (x0, y0) in ((10, 2), (2, 10)):
            stempel(a, x0, y0, HART_KLEIN, (255, 246, 250))
    elif soort == "pootjes":
        a = vlak((240, 226, 204), 2)
        for (x0, y0) in ((1, 1), (9, 9)):
            stempel(a, x0, y0, POOT, (176, 128, 104), (176, 128, 104))
        for (x0, y0) in ((10, 2), (2, 10)):
            stempel(a, x0, y0, ["1.1", ".1."], (206, 170, 150))
    elif soort == "dambord":
        a = vlak((250, 246, 240), 2)
        for y in range(16):
            for x in range(16):
                if (x // 4 + y // 4) % 2:
                    a[y, x, :3] = np.array((246, 176, 200), float) + rng.normal(0, 2)
    else:   # visgraat: herringbone in pastel wood
        a = vlak((236, 206, 170), 3)
        for y in range(16):
            for x in range(16):
                blok = (x // 4)
                d = (y + (x % 4) * (1 if blok % 2 == 0 else -1)) % 8
                if d == 0:
                    a[y, x, :3] = np.array((196, 156, 120), float)
                elif d < 4:
                    a[y, x, :3] += 8
    # grout: the 8x8 tiles (not for the herringbone)
    if soort != "visgraat":
        for i in range(16):
            for j in (0, 8):
                a[j, i, :3] = a[j, i, :3] * 0.88 + 255 * 0.12 - 18
                a[i, j, :3] = a[i, j, :3] * 0.88 + 255 * 0.12 - 18
    return a


def tex_behang(patroon, c, plint=False):
    a = vlak(c, 2)
    accent = mix(c, (220, 80, 130), 0.45) if c[0] > c[1] else mix(c, (60, 150, 110), 0.5)
    wit = (255, 252, 250)
    if patroon == "bloemetjes":
        for (x0, y0) in ((2, 2), (10, 6), (5, 10), (13, 13)):
            for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                put(a, (x0 + dx) % 16, (y0 + dy) % 16, accent)
            put(a, x0, y0, (250, 220, 110))
        for (x0, y0) in ((7, 3), (1, 13), (14, 9)):
            put(a, x0, y0, wit)
    elif patroon == "streepjes":
        for x in range(16):
            if x % 4 == 0:
                a[:, x, :3] = np.array(accent, float)
            elif x % 4 == 2:
                a[:, x, :3] = np.array(mix(c, wit, 0.5), float)
    elif patroon == "wolkjes":
        for (x0, y0) in ((1, 2), (9, 9)):
            stempel(a, x0, y0, [".11..", "11111", ".111."], wit)
        for (x0, y0) in ((11, 2), (3, 12)):
            put(a, x0, y0, accent)
            put(a, x0 + 1, y0, accent)
    else:   # guhtjes
        for (x0, y0) in ((1, 1), (9, 8)):
            stempel(a, x0, y0, GUH, accent, (80, 60, 70))
        for (x0, y0) in ((11, 2), (3, 11)):
            stempel(a, x0, y0, HART_KLEIN, mix(accent, wit, 0.4))
    if plint:
        for y in range(12, 16):
            a[y, :, :3] = np.array((250, 248, 244), float) + rng.normal(0, 2, (16, 1))
        a[12, :, :3] = np.array((226, 220, 214), float)
        a[15, :, :3] = np.array((214, 206, 200), float)
    return a


def steekjes(a, c):
    """The stitched seam of a plush: dashed, one pixel in from the edge."""
    for i in range(1, 15):
        if i % 2:
            for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
                put(a, x, y, c)
    return a


def tex_knuffel(c):
    a = vlak(c, 5)
    a[..., :3] += zacht_ruis(5, 8)[..., None]
    for _ in range(30):
        x, y = rng.integers(0, 16, 2)
        a[y, x, :3] += 10
    steekjes(a, mix(c, (90, 60, 80), 0.25))
    stempel(a, 6, 6, HART_KLEIN, mix(c, (255, 255, 255), 0.45))
    return a


def tex_knuffel_patroon(bestand):
    p = np.array(Image.open(os.path.join(HIER, "safari_patronen", bestand + ".png")).convert("RGBA"), float)
    a = p.copy()
    a[..., :3] = a[..., :3] * 0.8 + 255 * 0.2 + rng.normal(0, 4, (16, 16, 1))   # softer, fluffy
    a[..., 3] = 255
    steekjes(a, (120, 100, 110))
    return a


def tex_verf(c):
    """Painted wood: soft grain lines."""
    a = vlak(c, 2)
    for y in range(16):
        if y % 4 == 3:
            a[y, :, :3] -= 14
        a[y, :, :3] += rng.normal(0, 2)
    return a


def tex_luik(c):
    a = np.zeros((16, 16, 4), float)
    a[..., :3] = c
    a[..., 3] = 255
    for y in range(16):
        if y % 3 == 2:
            a[y, 1:15, :3] = np.array(mix(c, (60, 40, 50), 0.22), float)
        elif y % 3 == 0:
            a[y, 1:15, :3] = np.array(mix(c, (255, 255, 255), 0.3), float)
    rand = mix(c, (60, 40, 50), 0.3)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(a, x, y, rand)
    for dy, rij in enumerate(HART):                # a heart cut out (you see through it)
        for dx, ch in enumerate(rij):
            if ch == "1":
                a[5 + dy, 5 + dx] = 0
    return a


def tex_kozijn_glas():
    a = np.zeros((16, 16, 4), float)
    for i in range(4):
        put(a, 3 + i, 9 - i, (255, 255, 255), 255)
        put(a, 9 + i, 13 - i, (240, 248, 255), 255)
    return a


def tex_glasinlood(soort):
    lood = (74, 70, 86)
    a = np.zeros((16, 16, 4), float)
    kleuren = {"guh": [(250, 170, 200), (255, 230, 240)], "hartje": [(236, 90, 120), (255, 214, 226)],
               "bloem": [(250, 220, 100), (180, 230, 160)], "regenboog": [(240, 110, 120), (250, 190, 100), (250, 240, 120),
                                                                        (150, 220, 150), (130, 180, 240), (180, 140, 230)]}[soort]
    for y in range(16):
        for x in range(16):
            if soort == "regenboog":
                c = kleuren[min(5, y * 6 // 16)]
            else:
                c = kleuren[1]
            a[y, x, :3] = c
            a[y, x, 3] = 150
    if soort == "guh":
        stempel(a, 4, 4, GUH, (250, 150, 190), (70, 60, 80))
        for y in range(16):
            for x in range(16):
                if a[y, x, 0] == 250 and a[y, x, 1] == 150:
                    a[y, x, 3] = 190
    elif soort == "hartje":
        for dy, rij in enumerate(["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]):
            for dx, ch in enumerate(rij):
                if ch == "1":
                    a[5 + dy, 4 + dx, :3] = kleuren[0]
                    a[5 + dy, 4 + dx, 3] = 190
    elif soort == "bloem":
        for (dx, dy) in ((0, -2), (-2, 0), (2, 0), (0, 2), (-1, -1), (1, -1), (-1, 1), (1, 1)):
            put(a, 8 + dx, 7 + dy, (246, 160, 190), 190)
        put(a, 8, 7, kleuren[0], 210)
        for y in range(10, 15):
            put(a, 8, y, (110, 190, 120), 200)
    # the lead: the edge and a few lines
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(a, x, y, lood)
    if soort in ("guh", "bloem", "hartje"):
        for i in range(16):
            if i not in range(3, 13):
                put(a, i, 8, lood)
                put(a, 8, i, lood)
    else:
        for y in (3, 6, 9, 12):
            for x in range(16):
                put(a, x, y, lood)
    return a


def tex_lood():
    a = vlak((74, 70, 86), 3)
    return a


def tex_patrijspoort(c):
    a = vlak(c, 4)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 5.2:
                a[y, x, :3] = (190, 226, 246)
                a[y, x, 3] = 110
                if 3 <= x <= 6 and 3 <= y <= 6 and x + y in (8, 9):
                    a[y, x, :3] = (255, 255, 255)
                    a[y, x, 3] = 200
            elif d < 6.3:
                a[y, x, :3] = np.array(mix(c, (40, 30, 30), 0.3), float)
            elif d < 7.2:
                a[y, x, :3] = np.array(mix(c, (255, 255, 255), 0.25), float)
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14), (7, 0), (0, 7), (15, 8), (8, 15)):   # rivets
        put(a, x, y, mix(c, (255, 255, 255), 0.5))
    return a


def tex_zink():
    a = vlak((182, 190, 200), 3)
    for x in range(16):
        if x % 4 == 0:
            a[:, x, :3] += 10
    return a


def tex_raam():
    """The dormer's front: a white frame, a window with a pink curtain at the top, cut-out glass."""
    a = vlak((248, 246, 242), 2)
    for y in range(3, 13):
        for x in range(3, 13):
            a[y, x] = (150, 196, 230, 255)
    for x in range(3, 13):
        put(a, x, 3, (246, 170, 196))
        put(a, x, 4, (246, 170, 196) if x % 2 else (236, 150, 180))
    for y in range(3, 13):
        put(a, 7, y, (248, 246, 242))
        put(a, 8, y, (248, 246, 242))
    for x in range(3, 13):
        put(a, x, 8, (248, 246, 242))
    for i in range(3):
        put(a, 4 + i, 11 - i, (230, 244, 255))
    return a


def tex_deurmat():
    """32x32: a coir mat with WELKOM over GUH, a border and a heart."""
    a = vlak((196, 156, 104), 6, h=32, b=32)
    for y in range(32):
        for x in range(32):
            if (x + y) % 3 == 0:
                a[y, x, :3] -= 10
    rand = (150, 104, 70)
    for i in range(32):
        for (x, y) in ((i, 1), (i, 30), (1, i), (30, i)):
            put(a, x, y, rand)
    font = {"W": ["1...1", "1...1", "1.1.1", "11.11", "1...1"], "E": ["111", "1..", "11.", "1..", "111"],
            "L": ["1..", "1..", "1..", "1..", "111"], "K": ["1..1", "1.1.", "11..", "1.1.", "1..1"],
            "O": [".11.", "1..1", "1..1", "1..1", ".11."], "M": ["1...1", "11.11", "1.1.1", "1...1", "1...1"],
            "G": [".111", "1...", "1.11", "1..1", ".111"], "U": ["1..1", "1..1", "1..1", "1..1", ".11."],
            "H": ["1..1", "1..1", "1111", "1..1", "1..1"]}
    pink = (226, 92, 132)

    def schrijf(tekst, y0):
        breed = sum(len(font[ch][0]) + 1 for ch in tekst) - 1
        x = (32 - breed) // 2
        for ch in tekst:
            stempel(a, x, y0, font[ch], (92, 56, 40))
            x += len(font[ch][0]) + 1
    schrijf("WELKOM", 7)
    schrijf("GUH", 15)
    stempel(a, 14, 23, HART_KLEIN, pink)
    return a


def tex_lamp():
    a = vlak((255, 236, 160), 3)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(a, x, y, (120, 170, 150))
    a[6:10, 6:10, :3] = (255, 252, 230)
    return a


def tex_metaal(c):
    a = vlak(c, 3)
    a[0, :, :3] += 16
    return a


def tex_vogelhuis_voor():
    a = tex_pleister((250, 230, 170))
    for y in range(16):
        for x in range(16):
            if ((x - 7.5) ** 2 + (y - 6.5) ** 2) ** 0.5 < 2.6:
                a[y, x, :3] = (60, 44, 40)
    return a


def tex_oor():
    a = vlak((250, 182, 206), 3)
    a[4:12, 4:12, :3] = (255, 214, 228)
    return a


def tex_bloemen():
    """A cross texture: little flowers in a window box."""
    a = np.zeros((16, 16, 4), float)
    for x in range(1, 15, 3):
        h = int(rng.integers(6, 11))
        for y in range(16 - h, 16):
            put(a, x, y, (96, 170, 96))
        c = [(250, 150, 190), (250, 220, 110), (190, 160, 240), (255, 255, 255)][x % 4]
        for (dx, dy) in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
            put(a, x + dx, 16 - h + dy, c)
        put(a, x, 16 - h, (250, 210, 90))
    return a


def tex_gordijn():
    a = vlak((248, 184, 206), 3)
    for x in range(16):
        if x % 3 == 0:
            a[:, x, :3] -= 18
    for y in range(0, 16, 4):
        put(a, 8, y, (255, 255, 255))
    return a


# =====================================================================================================================
# the blocks
# =====================================================================================================================
def vorm_vplaat(bid, zij, boven, onder=None):
    onder = onder or boven
    tx = {"particle": zij, "zij": zij, "boven": boven, "onder": onder}
    model(f"{bid}_vplaat", {"parent": "minecraft:block/block", "textures": tx, "elements": [el([0, 0, 0], [16, 16, 8], {
        "north": {"texture": "#zij", "cullface": "north"}, "south": "#zij", "east": {"texture": "#zij", "cullface": "east"},
        "west": {"texture": "#zij", "cullface": "west"}, "up": {"texture": "#boven", "cullface": "up"},
        "down": {"texture": "#onder", "cullface": "down"}})]})
    blockstate(f"{bid}_vplaat", {"variants": {k: dict(v, uvlock=True) if "y" in v else v
                                              for k, v in facing_states(f"guhs:block/{bid}_vplaat").items()}})
    item(f"{bid}_vplaat")
    loot(f"{bid}_vplaat")
    model(f"{bid}_hoekje", {"parent": "minecraft:block/block", "textures": tx, "elements": [el([0, 0, 0], [8, 16, 8], {
        "north": {"texture": "#zij", "cullface": "north"}, "south": "#zij", "east": "#zij",
        "west": {"texture": "#zij", "cullface": "west"}, "up": {"texture": "#boven", "cullface": "up"},
        "down": {"texture": "#onder", "cullface": "down"}})]})
    blockstate(f"{bid}_hoekje", {"variants": {k: dict(v, uvlock=True) if "y" in v else v
                                              for k, v in facing_states(f"guhs:block/{bid}_hoekje").items()}})
    item(f"{bid}_hoekje")
    loot(f"{bid}_hoekje")


def dak_stappen(deel, delen):
    """The stepped slope (high side north): four steps across z, each 4 deep."""
    els = []
    per = 16 // delen
    for i in range(4):
        z1 = 16 - 4 * i
        h = deel * per + per * (i + 1) / 4
        els.append(el([0, 0, z1 - 4], [16, h, z1], {"north": "#zij", "south": "#zij", "east": "#zij", "west": "#zij",
                                                     "up": "#boven", "down": {"texture": "#onder", "cullface": "down"}}))
    return els


def vorm_dak(bid, zij, boven):
    tx = {"particle": boven, "zij": zij, "boven": boven, "onder": zij}
    for soort, delen in (("half", 2), ("kwart", 4)):
        n = f"{bid}_dak_{soort}"
        variants = {}
        for d in range(delen):
            model(f"{n}_{d}", {"parent": "minecraft:block/block", "textures": tx, "elements": dak_stappen(d, delen)})
            for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                v = {"model": f"guhs:block/{n}_{d}"}
                if y:
                    v["y"] = y
                    v["uvlock"] = True
                variants[f"deel={d},facing={f}"] = v
        blockstate(n, {"variants": variants})
        item(n, f"guhs:block/{n}_{delen - 1}")
        loot(n)


ARM_LOG_ROT = {"north": {"east": 90, "west": 90}, "south": {"east": 90, "west": 90},
               "east": {"up": 90, "down": 90, "north": 90, "south": 90}, "west": {"up": 90, "down": 90, "north": 90, "south": 90}}


def vorm_paalbalk(bid, zij, eind):
    for soort, t in (("paal", 6), ("balk", 10)):
        n = f"{bid}_{soort}"
        lo, hi = 8 - t / 2, 8 + t / 2
        tx = {"particle": zij, "zij": zij, "eind": eind}
        model(f"{n}_kern", {"textures": tx, "elements": [el([lo, lo, lo], [hi, hi, hi], {f: ("#eind" if f in ("up", "down") else "#zij") for f in ALLE6})]})
        armen = {"north": ([lo, lo, 0], [hi, hi, lo]), "south": ([lo, lo, hi], [hi, hi, 16]), "east": ([hi, lo, lo], [16, hi, hi]),
                 "west": ([0, lo, lo], [lo, hi, hi]), "up": ([lo, hi, lo], [hi, 16, hi]), "down": ([lo, 0, lo], [hi, lo, hi])}
        parts = [{"apply": {"model": f"guhs:block/{n}_kern"}}]
        for d, (a, b) in armen.items():
            faces = {}
            for f in ALLE6:
                if f == d:
                    faces[f] = {"texture": "#eind", "cullface": f}
                elif (d in ("up", "down") and f in ("up", "down")) or (f == {"north": "south", "south": "north", "east": "west",
                                                                                "west": "east", "up": "down", "down": "up"}[d]):
                    continue
                else:
                    r = ARM_LOG_ROT.get(d, {}).get(f)
                    faces[f] = {"texture": "#zij", "rotation": r} if r else "#zij"
            model(f"{n}_{d}", {"textures": tx, "elements": [el(a, b, faces)]})
            parts.append({"when": {d: "true"}, "apply": {"model": f"guhs:block/{n}_{d}"}})
        blockstate(n, {"multipart": parts})
        model(f"{n}_inventory", {"parent": "minecraft:block/block", "textures": tx, "elements": [
            el([lo, 0, lo], [hi, 16, hi], {f: ("#eind" if f in ("up", "down") else "#zij") for f in ALLE6})]})
        item(n, f"guhs:block/{n}_inventory")
        loot(n)


def volblok(n, tex_zij, tex_boven=None):
    model(n, {"parent": "minecraft:block/cube_bottom_top", "textures": {"side": tex_zij, "top": tex_boven or tex_zij, "bottom": tex_boven or tex_zij}})
    blockstate(n, {"variants": {"": {"model": f"guhs:block/{n}"}}})
    item(n)
    loot(n)


def trap_plaat_muur(n, t, muur=True, trap=True):
    if trap:
        for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
            model(f"{n}_trap{suffix}", {"parent": f"minecraft:block/{parent}", "textures": {"bottom": t, "top": t, "side": t}})
        blockstate(f"{n}_trap", {"variants": stair_states(f"guhs:block/{n}_trap")})
        item(f"{n}_trap")
        loot(f"{n}_trap")
        TAGS["stairs"].append(f"{n}_trap")
    model(f"{n}_plaat", {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
    model(f"{n}_plaat_top", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
    blockstate(f"{n}_plaat", {"variants": {"type=bottom": {"model": f"guhs:block/{n}_plaat"}, "type=top": {"model": f"guhs:block/{n}_plaat_top"},
                                           "type=double": {"model": f"guhs:block/{n}"}}})
    item(f"{n}_plaat")
    loot(f"{n}_plaat", plaat=True)
    TAGS["slabs"].append(f"{n}_plaat")
    if muur:
        for part, parent in (("post", "template_wall_post"), ("side", "template_wall_side"), ("side_tall", "template_wall_side_tall")):
            model(f"{n}_muur_{part}", {"parent": f"minecraft:block/{parent}", "textures": {"wall": t}})
        model(f"{n}_muur_inventory", {"parent": "minecraft:block/wall_inventory", "textures": {"wall": t}})
        blockstate(f"{n}_muur", {"multipart": wall_multipart(f"guhs:block/{n}_muur")})
        item(f"{n}_muur", f"guhs:block/{n}_muur_inventory")
        loot(f"{n}_muur")
        TAGS["walls"].append(f"{n}_muur")


def stair_states(m):
    out = {}
    for facing, rot in (("east", 0), ("south", 90), ("west", 180), ("north", 270)):
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model_ = m + ("" if shape == "straight" else "_inner" if shape.startswith("inner") else "_outer")
                y = rot
                if shape in ("inner_left", "outer_left"):
                    y = (rot + 270) % 360
                if half == "top":
                    if shape in ("inner_right", "outer_right"):
                        y = (rot + 90) % 360
                    elif shape in ("inner_left", "outer_left"):
                        y = rot
                v = {"model": model_}
                if half == "top":
                    v["x"] = 180
                if y:
                    v["y"] = y
                if half == "top" or y:
                    v["uvlock"] = True
                out[f"facing={facing},half={half},shape={shape}"] = v
    return out


def wall_multipart(m):
    parts = [{"when": {"up": "true"}, "apply": {"model": m + "_post"}}]
    for d, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for hoog, sfx in (("low", "_side"), ("tall", "_side_tall")):
            ap = {"model": m + sfx, "uvlock": True}
            if r:
                ap["y"] = r
            parts.append({"when": {d: hoog}, "apply": ap})
    return parts


# --- groups ---------------------------------------------------------------------------------------------------------
def groep_vormhulp_vanilla():
    for bid, nl, en, planks, log in HOUT:
        p = ref(planks)
        vorm_vplaat(bid, p, p)
        naam(f"{bid}_vplaat", f"{nl} verticale plaat", f"{en} Vertical Slab")
        naam(f"{bid}_hoekje", f"{nl} hoekje", f"{en} Corner")
        steenzaag(planks, f"{bid}_vplaat", 2)
        steenzaag(planks, f"{bid}_hoekje", 4)
        AXE.extend([f"{bid}_vplaat", f"{bid}_hoekje"])
    for bid, nl, en, blok, zij, top in STEEN:
        vorm_vplaat(bid, ref(zij), ref(top))
        naam(f"{bid}_vplaat", f"{nl} verticale plaat", f"{en} Vertical Slab")
        naam(f"{bid}_hoekje", f"{nl} hoekje", f"{en} Corner")
        steenzaag(blok, f"{bid}_vplaat", 2)
        steenzaag(blok, f"{bid}_hoekje", 4)
        PICKAXE.extend([f"{bid}_vplaat", f"{bid}_hoekje"])


def groep_paalbalk():
    for bid, nl, en, planks, log in HOUT:
        vorm_paalbalk(bid, ref(log), ref(log + "_top"))
        naam(f"{bid}_paal", f"{nl} paal", f"{en} Post")
        naam(f"{bid}_balk", f"{nl} balk", f"{en} Beam")
        steenzaag(HOUT_LOG[bid], f"{bid}_paal", 4)
        steenzaag(HOUT_LOG[bid], f"{bid}_balk", 2)
        steenzaag(log, f"{bid}_paal", 4)
        steenzaag(log, f"{bid}_balk", 2)
        AXE.extend([f"{bid}_paal", f"{bid}_balk"])


def groep_daken_vanilla():
    for bid, nl, en, planks, log in HOUT:
        vorm_dak(bid, ref(planks), ref(planks))
        naam(f"{bid}_dak_half", f"{nl} dak (halve helling)", f"{en} Roof (Half Slope)")
        naam(f"{bid}_dak_kwart", f"{nl} dak (kwart helling)", f"{en} Roof (Quarter Slope)")
        steenzaag(planks, f"{bid}_dak_half", 2)
        steenzaag(planks, f"{bid}_dak_kwart", 2)
        AXE.extend([f"{bid}_dak_half", f"{bid}_dak_kwart"])
    steen = {s[0]: s for s in STEEN}
    for bid in DAK_STEEN:
        _, nl, en, blok, zij, top = steen[bid]
        vorm_dak(bid, ref(zij), ref(top))
        naam(f"{bid}_dak_half", f"{nl} dak (halve helling)", f"{en} Roof (Half Slope)")
        naam(f"{bid}_dak_kwart", f"{nl} dak (kwart helling)", f"{en} Roof (Quarter Slope)")
        steenzaag(blok, f"{bid}_dak_half", 2)
        steenzaag(blok, f"{bid}_dak_kwart", 2)
        PICKAXE.extend([f"{bid}_dak_half", f"{bid}_dak_kwart"])


def groep_pastel():
    for soort, nl_s, en_s, en_s1 in (("pastelbaksteen", "pastelbakstenen", "Pastel Bricks", "Pastel Brick"),
                                     ("pleister", "pleister", "Plaster", "Plaster")):
        for kid, nl, en, dye, c in KLEUREN:
            n = f"{soort}_{kid}"
            t = save(tex_baksteen(c) if soort == "pastelbaksteen" else tex_pleister(c), n)
            volblok(n, t)
            naam(n, f"{nl} {nl_s}", f"{en} {en_s}")
            trap_plaat_muur(n, t)
            for sfx, snl, sen in (("trap", "trap", "Stairs"), ("plaat", "plaat", "Slab"), ("muur", "muur", "Wall")):
                naam(f"{n}_{sfx}", f"{nl} {nl_s}{'' if soort == 'pleister' else ''} {snl}", f"{en} {en_s1} {sen}")
            vorm_vplaat(n, t, t)
            naam(f"{n}_vplaat", f"{nl} {nl_s} verticale plaat", f"{en} {en_s1} Vertical Slab")
            naam(f"{n}_hoekje", f"{nl} {nl_s} hoekje", f"{en} {en_s1} Corner")
            # recipes: 8 bricks (or white terracotta) around a dye, then the stonecutter for every shape
            shaped(n, ["XXX", "XDX", "XXX"], {"X": "bricks" if soort == "pastelbaksteen" else "white_terracotta", "D": f"{dye}_dye"}, 8)
            shaped(f"{n}_trap", ["X  ", "XX ", "XXX"], {"X": f"guhs:{n}"}, 4)
            shaped(f"{n}_plaat", ["XXX"], {"X": f"guhs:{n}"}, 6)
            shaped(f"{n}_muur", ["XXX", "XXX"], {"X": f"guhs:{n}"}, 6)
            for sfx, cnt in (("trap", 1), ("plaat", 2), ("muur", 1), ("vplaat", 2), ("hoekje", 4)):
                steenzaag(f"guhs:{n}", f"{n}_{sfx}", cnt)
            for sfx in ("", "_trap", "_plaat", "_muur", "_vplaat", "_hoekje"):
                PICKAXE.append(n + sfx)


def groep_dakpannen():
    for kid, nl, en, terra, c in DAKPANNEN:
        n = f"dakpan_{kid}"
        t = save(tex_dakpan(c), n)
        volblok(n, t)
        naam(n, f"{nl} dakpannen", f"{en} Roof Tiles")
        trap_plaat_muur(n, t, muur=False)
        naam(f"{n}_trap", f"{nl} dakpannentrap", f"{en} Roof Tile Stairs")
        naam(f"{n}_plaat", f"{nl} dakpannenplaat", f"{en} Roof Tile Slab")
        vorm_dak(n, t, t)
        naam(f"{n}_dak_half", f"{nl} dakpannendak (halve helling)", f"{en} Tiled Roof (Half Slope)")
        naam(f"{n}_dak_kwart", f"{nl} dakpannendak (kwart helling)", f"{en} Tiled Roof (Quarter Slope)")
        vorm_vplaat(n, t, t)
        naam(f"{n}_vplaat", f"{nl} dakpannen verticale plaat", f"{en} Roof Tile Vertical Slab")
        naam(f"{n}_hoekje", f"{nl} dakpannen hoekje", f"{en} Roof Tile Corner")
        shaped(n, ["XX", "XX"], {"X": terra}, 4)
        shaped(f"{n}_trap", ["X  ", "XX ", "XXX"], {"X": f"guhs:{n}"}, 4)
        shaped(f"{n}_plaat", ["XXX"], {"X": f"guhs:{n}"}, 6)
        for sfx, cnt in (("trap", 1), ("plaat", 2), ("dak_half", 2), ("dak_kwart", 2), ("vplaat", 2), ("hoekje", 4)):
            steenzaag(f"guhs:{n}", f"{n}_{sfx}", cnt)
        for sfx in ("", "_trap", "_plaat", "_dak_half", "_dak_kwart", "_vplaat", "_hoekje"):
            PICKAXE.append(n + sfx)


def groep_tegels():
    for tid, nl, en, pnl, pen in TEGELS:
        n = f"tegel_{tid}"
        t = save(tex_tegel(tid), n)
        volblok(n, t)
        naam(n, nl, en)
        trap_plaat_muur(n, t, muur=False, trap=False)
        naam(f"{n}_plaat", pnl, pen)
        steenzaag("white_concrete", n, 1)
        steenzaag(f"guhs:{n}", f"{n}_plaat", 2)
        shaped(f"{n}_plaat", ["XXX"], {"X": f"guhs:{n}"}, 6)
        PICKAXE.extend([n, f"{n}_plaat"])


def groep_behang():
    for kid, knl, ken, c, wol in BEHANG_KLEUREN:
        ids = []
        for pid, nl, en in BEHANG_PATRONEN:
            n = f"behang_{pid}_{kid}"
            t = save(tex_behang(pid, c), n)
            tp = save(tex_behang(pid, c, plint=True), n + "_plint")
            model(n, {"parent": "minecraft:block/cube_all", "textures": {"all": t}})
            model(n + "_plint", {"parent": "minecraft:block/cube_bottom_top", "textures": {"side": tp, "top": t, "bottom": t}})
            blockstate(n, {"variants": {"plint=false": {"model": f"guhs:block/{n}"}, "plint=true": {"model": f"guhs:block/{n}_plint"}}})
            item(n, f"guhs:block/{n}_plint")
            loot(n)
            naam(n, f"{nl} ({knl})", f"{en} ({ken})")
            AXE.append(n)
            ids.append(n)
        shaped(ids[0], ["PPP", "PWP", "PPP"], {"P": "paper", "W": wol}, 8)
        for a in ids:
            for b in ids:
                if a != b:
                    steenzaag(f"guhs:{a}", b)


def groep_knuffel():
    for kid in KNUFFEL_KLEUREN:
        _, nl, en, dye, c = KLEUR[kid]
        n = f"knuffelblok_{kid}"
        t = save(tex_knuffel(c), n)
        model(n, {"parent": "minecraft:block/cube_all", "textures": {"all": t}})
        blockstate(n, {"variants": {"": {"model": f"guhs:block/{n}"}}})
        item(n)
        loot(n)
        naam(n, f"{nl} knuffelblok", f"{en} Snuggle Block")
        shaped(n, [" W ", "WSW", " W "], {"W": KNUFFEL_WOL[kid], "S": "slime_ball"}, 4)
    for pid, bestand, nl, en, extra in KNUFFEL_PATRONEN:
        n = f"knuffelblok_{pid}"
        t = save(tex_knuffel_patroon(bestand), n)
        model(n, {"parent": "minecraft:block/cube_all", "textures": {"all": t}})
        blockstate(n, {"variants": {"": {"model": f"guhs:block/{n}"}}})
        item(n)
        loot(n)
        naam(n, nl, en)
        shapeless(n, ["guhs:knuffelblok_vanille", extra])


def groep_kozijnen():
    glas = save(tex_kozijn_glas(), "kozijn_glas")
    oor = save(tex_oor(), "oor")
    bloemen = save(tex_bloemen(), "bloembak_bloemen")
    gordijn = save(tex_gordijn(), "gordijn")
    bak = save(tex_pleister((222, 138, 110)), "bloembak")
    for vid, nl, nlde, en, dye, c in VERF:
        hout = save(tex_verf(c), f"verf_{vid}")
        n = f"guh_kozijn_{vid}"
        tx = {"particle": hout, "hout": hout, "glas": glas, "oor": oor, "bloem": bloemen, "gordijn": gordijn, "bak": bak}
        kaal = [doos([0, 14, 6], [16, 16, 10], "#hout", cull=("up", "east", "west")),
                doos([0, 0, 6], [16, 2, 10], "#hout", cull=("down", "east", "west")),
                doos([0, 2, 6], [2, 14, 10], "#hout", cull=("west",)), doos([14, 2, 6], [16, 14, 10], "#hout", cull=("east",)),
                doos([7.5, 2, 7.25], [8.5, 14, 8.75], "#hout"), doos([2, 7.5, 7.25], [14, 8.5, 8.75], "#hout"),
                el([2, 2, 8], [14, 14, 8], {"north": "#glas", "south": "#glas"}),
                doos([0, 0, 3.5], [16, 1.5, 6], "#hout")]
        oortjes = [doos([2, 16, 7], [5, 19, 9], "#hout"), doos([11, 16, 7], [14, 19, 9], "#hout"),
                   el([2.75, 16, 6.9], [4.25, 18.25, 6.9], {"north": "#oor"}), el([11.75, 16, 6.9], [13.25, 18.25, 6.9], {"north": "#oor"})]
        bloembak = [doos([1, 1.5, 1], [15, 5, 5.5], "#bak"),
                    el([1.5, 5, 3.25], [14.5, 11, 3.25], {"north": "#bloem", "south": "#bloem"})]
        gordijnen = [doos([2, 2, 10], [5, 14, 10.5], "#gordijn"), doos([11, 2, 10], [14, 14, 10.5], "#gordijn"),
                     doos([2, 12, 10], [14, 14, 10.75], "#gordijn")]
        variants = {}
        for decor, extra in (("kaal", []), ("oortjes", oortjes), ("bloembak", bloembak), ("gordijn", gordijnen)):
            model(f"{n}_{decor}", {"textures": tx, "elements": kaal + extra})
            for k, v in facing_states(f"guhs:block/{n}_{decor}").items():
                variants[f"decor={decor},{k}"] = v
        blockstate(n, {"variants": variants})
        item(n, f"guhs:block/{n}_oortjes")
        loot(n)
        naam(n, f"{nl} guhkozijn", f"{en} Guh Window Frame")
        shaped(n, [" D ", "SGS", " S "], {"D": dye, "S": "stick", "G": "glass_pane"}, 2)
        AXE.append(n)

        # the shutter: a panel with a heart, door-like (right-click: open)
        luik = save(tex_luik(c), f"luik_{vid}")
        ln = f"guh_luik_{vid}"
        tl = {"particle": luik, "luik": luik, "hout": hout}
        model(f"{ln}_dicht", {"textures": tl, "elements": [el([0, 0, 13], [16, 16, 16], {"north": "#luik", "south": "#luik",
                                                                                          "east": "#hout", "west": "#hout", "up": "#hout", "down": "#hout"})]})
        model(f"{ln}_open_rechts", {"textures": tl, "elements": [el([13, 0, 0], [16, 16, 16], {"east": "#luik", "west": "#luik",
                                                                                                "north": "#hout", "south": "#hout", "up": "#hout", "down": "#hout"})]})
        model(f"{ln}_open_links", {"textures": tl, "elements": [el([0, 0, 0], [3, 16, 16], {"east": "#luik", "west": "#luik",
                                                                                             "north": "#hout", "south": "#hout", "up": "#hout", "down": "#hout"})]})
        variants = {}
        for open_ in ("false", "true"):
            for kant in ("links", "rechts"):
                m = f"guhs:block/{ln}_dicht" if open_ == "false" else f"guhs:block/{ln}_open_{kant}"
                for k, v in facing_states(m).items():
                    variants[f"{k},kant={kant},open={open_}"] = v
        blockstate(ln, {"variants": variants})
        item(ln, f"guhs:block/{ln}_dicht")
        loot(ln)
        naam(ln, f"{nl} guhluik", f"{en} Guh Shutter")
        shaped(ln, ["PDP", "PPP"], {"P": "#minecraft:planks", "D": dye}, 4)
        AXE.append(ln)


def groep_glas():
    lood = save(tex_lood(), "glasinlood_rand")
    for gid, nl, en, extra in GLAS:
        n = f"glasinlood_{gid}"
        t = save(tex_glasinlood(gid), n)
        model(n, {"parent": "minecraft:block/cube_all", "textures": translucent({"all": t})})
        blockstate(n, {"variants": {"": {"model": f"guhs:block/{n}"}}})
        item(n)
        loot(n)
        naam(n, nl, en)
        shaped(n, ["GNG", "GDG", "GNG"], {"G": "glass", "N": "iron_nugget", "D": extra}, 6)
        r = f"{n}_ruit"
        tx = translucent({"pane": t, "edge": lood})
        for part in ("post", "side", "side_alt", "noside", "noside_alt"):
            model(f"{r}_{part}", {"parent": f"minecraft:block/template_glass_pane_{part}", "textures": tx})
        m = f"guhs:block/{r}"
        blockstate(r, {"multipart": [
            {"apply": {"model": m + "_post"}},
            {"when": {"north": "true"}, "apply": {"model": m + "_side"}},
            {"when": {"east": "true"}, "apply": {"model": m + "_side", "y": 90}},
            {"when": {"south": "true"}, "apply": {"model": m + "_side_alt"}},
            {"when": {"west": "true"}, "apply": {"model": m + "_side_alt", "y": 90}},
            {"when": {"north": "false"}, "apply": {"model": m + "_noside"}},
            {"when": {"east": "false"}, "apply": {"model": m + "_noside_alt"}},
            {"when": {"south": "false"}, "apply": {"model": m + "_noside_alt", "y": 90}},
            {"when": {"west": "false"}, "apply": {"model": m + "_noside", "y": 270}}]})
        item(r, sprite=t)
        loot(r)
        naam(r, f"{nl}ruit", f"{en} Pane")
        shaped(r, ["XXX", "XXX"], {"X": f"guhs:{n}"}, 16)
    for pid, nl, en, ingot, c in PATRIJS:
        n = f"patrijspoort_{pid}"
        t = save(tex_patrijspoort(c), n)
        model(n, {"parent": "minecraft:block/cube_all", "textures": translucent({"all": t})})
        blockstate(n, {"variants": {"": {"model": f"guhs:block/{n}"}}})
        item(n)
        loot(n)
        naam(n, nl, en)
        shaped(n, [" I ", "IGI", " I "], {"I": ingot, "G": "glass"}, 2)
        PICKAXE.append(n)


def groep_dakdetails():
    zink = save(tex_zink(), "zink")
    # dakgoot (the gutter hangs against the wall: model facing north = outward north, wall at the south)
    model("dakgoot", {"textures": {"particle": zink, "z": zink}, "elements": [
        doos([0, 11, 10], [16, 12, 16], "#z", cull=("east", "west")), doos([0, 12, 10], [16, 15, 11], "#z", cull=("east", "west")),
        doos([0, 12, 15], [16, 16, 16], "#z", cull=("east", "west", "south"))]})
    blockstate("dakgoot", {"variants": facing_states("guhs:block/dakgoot")})
    item("dakgoot")
    loot("dakgoot")
    naam("dakgoot", "Dakgoot", "Gutter")
    shaped("dakgoot", ["N N", "NNN"], {"N": "iron_nugget"}, 4)
    model("regenpijp", {"textures": {"particle": zink, "z": zink}, "elements": [
        doos([6, 0, 11], [10, 16, 15], "#z", cull=("up", "down")), doos([5, 10, 14], [11, 12, 16], "#z", cull=("south",)),
        doos([5, 2, 14], [11, 4, 16], "#z", cull=("south",))]})
    blockstate("regenpijp", {"variants": facing_states("guhs:block/regenpijp")})
    item("regenpijp")
    loot("regenpijp")
    naam("regenpijp", "Regenpijp", "Drainpipe")
    shaped("regenpijp", ["N", "N", "N"], {"N": "iron_nugget"}, 4)
    PICKAXE.extend(["dakgoot", "regenpijp"])
    # the guh chimney: pink bricks with ears; it smokes (right-click: fire out / on)
    bak = T + "pastelbaksteen_roze"
    oor = T + "oor"
    roet = save(vlak((52, 44, 48), 4), "roet")
    model("guh_schoorsteen", {"textures": {"particle": bak, "b": bak, "oor": oor, "roet": roet}, "elements": [
        el([2, 0, 2], [14, 13, 14], {"north": "#b", "south": "#b", "east": "#b", "west": "#b", "down": {"texture": "#b", "cullface": "down"}}),
        el([1, 13, 1], [15, 16, 15], {"north": "#b", "south": "#b", "east": "#b", "west": "#b", "down": "#b"}),
        el([1, 16, 1], [3, 16, 15], {"up": "#b"}), el([13, 16, 1], [15, 16, 15], {"up": "#b"}),
        el([3, 16, 1], [13, 16, 3], {"up": "#b"}), el([3, 16, 13], [13, 16, 15], {"up": "#b"}),
        el([3, 14, 3], [13, 14, 13], {"up": "#roet"}),
        el([3, 13, 3], [13, 16, 3], {"south": "#b"}), el([3, 13, 13], [13, 16, 13], {"north": "#b"}),
        el([3, 13, 3], [3, 16, 13], {"east": "#b"}), el([13, 13, 3], [13, 16, 13], {"west": "#b"}),
        doos([2, 16, 7], [5, 19.5, 9], "#b"), doos([11, 16, 7], [14, 19.5, 9], "#b"),
        el([2.75, 16.5, 6.9], [4.25, 18.75, 6.9], {"north": "#oor"}), el([11.75, 16.5, 6.9], [13.25, 18.75, 6.9], {"north": "#oor"})]})
    blockstate("guh_schoorsteen", {"variants": {"rookt=true": {"model": "guhs:block/guh_schoorsteen"},
                                                "rookt=false": {"model": "guhs:block/guh_schoorsteen"}}})
    item("guh_schoorsteen")
    loot("guh_schoorsteen")
    naam("guh_schoorsteen", "Guhschoorsteen", "Guh Chimney")
    shaped("guh_schoorsteen", ["BCB", "B B"], {"B": "brick", "C": "campfire"}, 2)
    PICKAXE.append("guh_schoorsteen")
    # the dormer (front north)
    raam = save(tex_raam(), "dakkapel_raam")
    for did, nl, en, dye in DAKKAPEL:
        n = f"dakkapel_{did}"
        dak = T + ("dakpan_roze" if did == "roze" else "dakpan_hemelsblauw")
        muur = T + ("pleister_vanille" if did == "roze" else "pleister_wolkje")
        els = [el([2, 0, 4], [14, 10, 16], {"north": "#raam", "east": "#muur", "west": "#muur", "south": {"texture": "#muur", "cullface": "south"},
                                            "down": {"texture": "#muur", "cullface": "down"}})]
        for i in range(4):
            h = 10 + 1.5 * (i + 1)
            els.append(el([1 + 1.75 * i, 9, 2], [1 + 1.75 * (i + 1), h, 16], {"north": "#dak", "up": "#dak", "west": "#dak", "down": "#dak",
                                                                               "south": {"texture": "#dak", "cullface": "south"}}))
            els.append(el([15 - 1.75 * (i + 1), 9, 2], [15 - 1.75 * i, h, 16], {"north": "#dak", "up": "#dak", "east": "#dak", "down": "#dak",
                                                                                 "south": {"texture": "#dak", "cullface": "south"}}))
        els += [doos([4.5, 15, 3], [6.5, 17.5, 4.5], "#oor"), doos([9.5, 15, 3], [11.5, 17.5, 4.5], "#oor")]
        model(n, {"textures": {"particle": dak, "dak": dak, "muur": muur, "raam": raam, "oor": T + "oor"}, "elements": els})
        blockstate(n, {"variants": facing_states(f"guhs:block/{n}")})
        item(n)
        loot(n)
        naam(n, nl, en)
        shaped(n, [" D ", "PGP", " P "], {"D": dye, "P": "#minecraft:planks", "G": "glass_pane"}, 1)
        AXE.append(n)


def groep_tuin():
    oor = T + "oor"
    for vid, nl, nlde, en, dye, c in VERF:
        hout = T + f"verf_{vid}"
        n = f"guh_hekje_{vid}"
        tx = {"particle": hout, "texture": hout, "oor": oor}
        post = [doos([6, 0, 6], [10, 16, 10], "#texture", cull=("down",)),
                doos([6, 16, 7.5], [7.5, 18.5, 8.5], "#texture"), doos([8.5, 16, 7.5], [10, 18.5, 8.5], "#texture")]
        model(f"{n}_post", {"textures": tx, "elements": post})
        side = [el([7, 12, 0], [9, 15, 9], {"north": {"texture": "#texture", "cullface": "north"}, "up": "#texture", "down": "#texture",
                                             "east": "#texture", "west": "#texture"}),
                el([7, 6, 0], [9, 9, 9], {"north": {"texture": "#texture", "cullface": "north"}, "up": "#texture", "down": "#texture",
                                           "east": "#texture", "west": "#texture"})]
        model(f"{n}_side", {"textures": tx, "elements": side})
        inv = [doos([6, 0, 0], [10, 16, 4], "#texture"), doos([6, 0, 12], [10, 16, 16], "#texture"),
               doos([7, 13, -2], [9, 15, 18], "#texture"), doos([7, 5, -2], [9, 7, 18], "#texture"),
               doos([6, 16, 0.5], [7.5, 18.5, 1.5], "#texture"), doos([8.5, 16, 0.5], [10, 18.5, 1.5], "#texture"),
               doos([6, 16, 14.5], [7.5, 18.5, 15.5], "#texture"), doos([8.5, 16, 14.5], [10, 18.5, 15.5], "#texture")]
        model(f"{n}_inventory", {"parent": "minecraft:block/block", "textures": tx, "elements": inv,
                                 "display": {"gui": {"rotation": [30, 135, 0], "scale": [0.625, 0.625, 0.625]}}})
        parts = [{"apply": {"model": f"guhs:block/{n}_post"}}]
        for d, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            ap = {"model": f"guhs:block/{n}_side", "uvlock": True}
            if r:
                ap["y"] = r
            parts.append({"when": {d: "true"}, "apply": ap})
        blockstate(n, {"multipart": parts})
        item(n, f"guhs:block/{n}_inventory")
        loot(n)
        naam(n, f"{nl} guhhekje", f"{en} Guh Fence")
        shaped(n, ["PDP", "PSP"], {"P": "#minecraft:planks", "S": "stick", "D": dye}, 3)
        TAGS["wooden_fences"].append(n)
        AXE.append(n)
        g = f"guh_poortje_{vid}"
        for part, parent in (("", "template_fence_gate"), ("_open", "template_fence_gate_open"), ("_wall", "template_fence_gate_wall"),
                             ("_wall_open", "template_fence_gate_wall_open")):
            model(f"{g}{part}", {"parent": f"minecraft:block/{parent}", "textures": {"texture": hout}})
        variants = {}
        for f, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
            for in_wall in ("false", "true"):
                for open_ in ("false", "true"):
                    m = f"guhs:block/{g}" + ("_wall" if in_wall == "true" else "") + ("_open" if open_ == "true" else "")
                    v = {"model": m, "uvlock": True}
                    if y:
                        v["y"] = y
                    variants[f"facing={f},in_wall={in_wall},open={open_}"] = v
        blockstate(g, {"variants": variants})
        item(g)
        loot(g)
        naam(g, f"{nl} guhpoortje", f"{en} Guh Fence Gate")
        shaped(g, ["SDS", "SPS"], {"P": "#minecraft:planks", "S": "stick", "D": dye}, 1)
        TAGS["fence_gates"].append(g)
        AXE.append(g)

    # the lamppost: two blocks high, a guh cap with ears on the lamp
    mint = save(tex_metaal((150, 200, 180)), "lantaarn_paal")
    lamp = save(tex_lamp(), "lantaarn_glas")
    kap = T + "dakpan_roze"
    tx = {"particle": mint, "p": mint, "lamp": lamp, "kap": kap, "oor": oor}
    model("guh_lantaarnpaal_onder", {"textures": tx, "elements": [doos([6.5, 0, 6.5], [9.5, 16, 9.5], "#p", cull=("up",)),
                                                                  doos([4.5, 0, 4.5], [11.5, 2, 11.5], "#p", cull=("down",)),
                                                                  doos([5.5, 2, 5.5], [10.5, 3.5, 10.5], "#p")]})
    model("guh_lantaarnpaal_boven", {"textures": tx, "elements": [
        doos([6.5, 0, 6.5], [9.5, 6, 9.5], "#p", cull=("down",)), doos([4, 6, 4], [12, 7, 12], "#p"),
        doos([4.5, 7, 4.5], [11.5, 13, 11.5], "#lamp", licht=15), doos([3, 13, 3], [13, 14.5, 13], "#kap"),
        doos([4.5, 14.5, 4.5], [11.5, 16, 11.5], "#kap"),
        doos([4.5, 16, 7], [7, 18.5, 9], "#kap"), doos([9, 16, 7], [11.5, 18.5, 9], "#kap"),
        el([5.1, 16.4, 6.9], [6.4, 18, 6.9], {"north": "#oor"}), el([9.6, 16.4, 6.9], [10.9, 18, 6.9], {"north": "#oor"})]})
    blockstate("guh_lantaarnpaal", {"variants": {"half=lower": {"model": "guhs:block/guh_lantaarnpaal_onder"},
                                                 "half=upper": {"model": "guhs:block/guh_lantaarnpaal_boven"}}})
    icoon = np.zeros((16, 16, 4), float)
    for y in range(5, 16):
        put(icoon, 7, y, (150, 200, 180))
        put(icoon, 8, y, (120, 170, 150))
    for x in range(6, 10):
        put(icoon, x, 15, (120, 170, 150))
    for y in range(2, 5):
        for x in range(5, 11):
            put(icoon, x, y, (255, 236, 160))
    for x in range(4, 12):
        put(icoon, x, 1, (236, 140, 168))
    for (x, y) in ((5, 0), (10, 0)):
        put(icoon, x, y, (236, 140, 168))
    item("guh_lantaarnpaal", sprite=save(icoon, "guh_lantaarnpaal_icoon"))
    loot("guh_lantaarnpaal", conditie={"half": "lower"})
    naam("guh_lantaarnpaal", "Guhlantaarnpaal", "Guh Lamppost")
    shaped("guh_lantaarnpaal", ["DLD", " I ", " I "], {"D": "pink_dye", "L": "lantern", "I": "iron_bars"}, 1)
    PICKAXE.append("guh_lantaarnpaal")

    # the doormat (32x32 texture, readable for whoever placed it)
    mat = save(tex_deurmat(), "deurmat_welkom")
    model("deurmat_welkom", {"textures": {"particle": mat, "m": mat}, "elements": [el([0, 0, 0], [16, 1, 16], {
        "up": {"texture": "#m", "rotation": 180}, "down": {"texture": "#m", "cullface": "down"},
        "north": {"texture": "#m", "uv": [0, 15, 16, 16]}, "south": {"texture": "#m", "uv": [0, 15, 16, 16]},
        "east": {"texture": "#m", "uv": [0, 15, 16, 16]}, "west": {"texture": "#m", "uv": [0, 15, 16, 16]}})]})
    blockstate("deurmat_welkom", {"variants": facing_states("guhs:block/deurmat_welkom")})
    item("deurmat_welkom")
    loot("deurmat_welkom")
    naam("deurmat_welkom", "Deurmat \"Welkom guh\"", "\"Welkom Guh\" Doormat")
    shaped("deurmat_welkom", ["CDC"], {"C": "brown_carpet", "D": "pink_dye"}, 2)

    # the benches (a seat: GuhFurnitureBlock)
    poot_metaal = save(tex_metaal((236, 150, 180)), "bank_poot_roze")
    for bid, nl, en in BANKJES:
        hout = ref("oak_planks") if bid == "hout" else T + "verf_roze"
        poot = ref("dark_oak_planks") if bid == "hout" else poot_metaal
        tx = {"particle": hout, "h": hout, "p": poot, "oor": oor}
        els = [doos([0, 6, 3], [16, 7.5, 6.5], "#h"), doos([0, 6, 7], [16, 7.5, 10.5], "#h"), doos([0, 6, 11], [16, 7.5, 13.5], "#h"),
               doos([0, 9, 12.5], [16, 11.5, 14], "#h"), doos([0, 12.5, 12.5], [16, 15, 14], "#h"),
               doos([1, 0, 4], [3, 6, 6], "#p"), doos([13, 0, 4], [15, 6, 6], "#p"),
               doos([1, 0, 11.5], [3, 15, 13], "#p"), doos([13, 0, 11.5], [15, 15, 13], "#p"),
               doos([1, 15, 12], [3.5, 17.5, 13.5], "#p"), doos([12.5, 15, 12], [15, 17.5, 13.5], "#p")]
        n = f"guh_bankje_{bid}"
        model(n, {"textures": tx, "elements": els})
        blockstate(n, {"variants": facing_states(f"guhs:block/{n}")})
        item(n)
        loot(n)
        naam(n, nl, en)
        if bid == "hout":
            shaped(n, ["  P", "PPP", "S S"], {"P": "#minecraft:planks", "S": "stick"}, 1)
        else:
            shaped(n, ["D P", "PPP", "S S"], {"P": "#minecraft:planks", "S": "stick", "D": "pink_dye"}, 1)
        AXE.append(n)

    # the birdhouse on its little post (now and then a bird sings in it)
    voor = save(tex_vogelhuis_voor(), "vogelhuisje_voor")
    muur = T + "pleister_boter"
    dak = T + "dakpan_roze"
    model("vogelhuisje", {"textures": {"particle": muur, "v": voor, "m": muur, "d": dak, "p": ref("oak_planks")}, "elements": [
        doos([7, 0, 7], [9, 6, 9], "#p", cull=("down",)),
        el([4, 6, 4], [12, 13, 12], {"north": "#v", "south": "#m", "east": "#m", "west": "#m", "up": "#m", "down": "#m"}),
        doos([3, 13, 3], [13, 14.5, 13], "#d"), doos([4.5, 14.5, 4.5], [11.5, 16, 11.5], "#d"),
        doos([7.5, 8, 2.5], [8.5, 8.75, 4], "#p")]})
    blockstate("vogelhuisje", {"variants": facing_states("guhs:block/vogelhuisje")})
    item("vogelhuisje")
    loot("vogelhuisje")
    naam("vogelhuisje", "Vogelhuisje", "Birdhouse")
    shaped("vogelhuisje", [" P ", "PFP", " S "], {"P": "#minecraft:planks", "F": "feather", "S": "stick"}, 1)
    AXE.append("vogelhuisje")


# =====================================================================================================================
def java_lijst():
    def arr(rijen):
        return "{" + ", ".join("{" + ", ".join(f'"{v}"' for v in r) + "}" for r in rijen) + "}"

    def lst(v):
        return "{" + ", ".join(f'"{x}"' for x in v) + "}"
    src = f"""package nl.juiced.guhs.feature.bouwhulp.blokken;

/** GENERATED by tools/features/bouwhulp_blokken.py (the same lists as the resources): do not edit by hand. */
final class BouwLijst {{
    /** id, planks (vanilla), stripped log (vanilla): verticale platen, hoekjes, palen, balken, daken. */
    static final String[][] HOUT = {arr([(h[0], h[3], h[4]) for h in HOUT])};
    /** id, vanilla block: verticale platen, hoekjes. */
    static final String[][] STEEN = {arr([(s[0], s[3]) for s in STEEN])};
    /** The stones that also make roofs. */
    static final String[] DAK_STEEN = {lst(DAK_STEEN)};
    /** id, dye (its map colour). */
    static final String[][] KLEUREN = {arr([(k[0], k[3]) for k in KLEUREN])};
    /** id, the terracotta it is made of (its properties). */
    static final String[][] DAKPANNEN = {arr([(k[0], k[3]) for k in DAKPANNEN])};
    static final String[] TEGELS = {lst([t[0] for t in TEGELS])};
    static final String[] BEHANG = {lst([f"{p[0]}_{k[0]}" for k in BEHANG_KLEUREN for p in BEHANG_PATRONEN])};
    static final String[] KNUFFEL = {lst(KNUFFEL_KLEUREN + [p[0] for p in KNUFFEL_PATRONEN])};
    static final String[] VERF = {lst([v[0] for v in VERF])};
    static final String[] GLAS = {lst([g[0] for g in GLAS])};
    static final String[] PATRIJS = {lst([p[0] for p in PATRIJS])};
    static final String[] DAKKAPEL = {lst([d[0] for d in DAKKAPEL])};
    static final String[] BANKJES = {lst([b[0] for b in BANKJES])};
    /** Every block id, in creative order ({len(IDS)} blocks). */
    static final String[] ALLE = {lst(IDS)};

    private BouwLijst() {{
    }}
}}
"""
    os.makedirs(os.path.dirname(JAVA_LIJST), exist_ok=True)
    with open(JAVA_LIJST, "w", encoding="utf-8", newline="\n") as f:
        f.write(src)


def af():
    NL["itemGroup.guhs.bouwen"] = "Guhs: Bouwen"
    EN["itemGroup.guhs.bouwen"] = "Guhs: Building"
    for taal, nieuw in (("nl_nl", NL), ("en_us", EN)):
        pad = os.path.join(A, "lang", f"{taal}.json")
        d = json.load(open(pad, encoding="utf-8"))
        d.update(nieuw)
        with open(pad, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    w(EN_BESTAND, EN)
    tag(["mineable", "pickaxe"], PICKAXE)
    tag(["mineable", "axe"], AXE)
    for t, namen in TAGS.items():
        tag([t], namen)
        item_tag([t], namen)


def blokken():
    # (the order = the creative order; groups refer to each other's textures by name only)
    groep_vormhulp_vanilla()
    groep_paalbalk()
    groep_daken_vanilla()
    groep_pastel()
    groep_dakpannen()
    groep_tegels()
    groep_behang()
    groep_knuffel()
    groep_kozijnen()
    groep_glas()
    groep_dakdetails()
    groep_tuin()


if __name__ == "__main__":
    blokken()
    java_lijst()
    af()
    print(f"bouwhulp_blokken: {len(IDS)} blocks, {len(NL)} texts")
