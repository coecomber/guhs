"""
De Gatenkaasgrotten (2.7, slice 3): the underground of the Guhmension (below about y 40) turns, in big patches, into one
enormous cheese with holes.

  - biome guhs:gatenkaasgrotten: selected by our own noise ("weirdness", only below y ~40) and a deeper "depth" there,
    like the guh_kristalmijn (see patch_worldgen). Its rock is gatenkaas (a surface rule), the guhs:gatenkaas_holte
    feature (GatenkaasHolteFeature.java) blows big round cheese holes in it, with kaas stalactites, glowing kaasmos,
    kaassaus pools and drips, and kaaskorrel ore (+ kaasknabbel veins and vads ore in the cheese)
  - blocks: gatenkaas (+ stenen, trap, plaat, muur), belegen kaas stenen / tegels (+ trap, plaat, muur), kaas_stalactiet,
    kaasmos (+ tapijt), kaaskorrelerts, knabbelsensor, knabbelschreeuwer; items: kaaskorrel, stille_knabbel
  - mob effect guhs:stil; the Vadswaker (a big blind Mika: geo/animations/textures made here)
  - structures: gatenkaas_mijnschacht (an abandoned cheese mine shaft, small) and stille_voorraadkelder (the Mika's
    secret larder, an Ancient City parody, 96 x 30 x 96), each with a geometry self-check

build(h) makes everything (h = make_v2); ftb(fq) adds the quests.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw

CHEESE = (240, 196, 84)
CHEESE_LIGHT = (255, 228, 140)
CHEESE_DARK = (198, 146, 50)
HOLE = (168, 116, 34)
AGED = (168, 104, 44)
AGED_DARK = (112, 64, 26)
AGED_LIGHT = (204, 140, 66)
PINK = (238, 141, 173)

BOOK = "voorraadkelder"          # the lore book (tools/features/bibliotheek.py BOOKS, Guhboek.VOORRAADKELDER)

# =====================================================================================================================
# textures
# =====================================================================================================================


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def noisy(base, var, seed, size=16):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = rng.randint(-var, var)
            img.putpixel((x, y), clamp((base[0] + v, base[1] + v, base[2] + v)) + (255,))
    return img


def holes_on(img, holes, rim=CHEESE_LIGHT, inside=HOLE, shade=None):
    """Round cheese holes (seamless: they wrap around the edges). A hole is lit from the bottom right inside."""
    px = img.load()
    size = img.size[0]
    shade = shade or tuple(int(c * 0.78) for c in inside)
    for (cx, cy, r) in holes:
        for y in range(size):
            for x in range(size):
                dx = min(abs(x + 0.5 - cx), size - abs(x + 0.5 - cx))
                dy = min(abs(y + 0.5 - cy), size - abs(y + 0.5 - cy))
                sx = (x + 0.5 - cx + size / 2) % size - size / 2
                sy = (y + 0.5 - cy + size / 2) % size - size / 2
                d = math.hypot(dx, dy)
                if d <= r:
                    # the far (bottom right) inside of the hole catches light, the near side is in shadow
                    t = (sx + sy) / (2 * r)
                    px[x, y] = clamp(tuple(s + (i - s) * (0.5 + t) for s, i in zip(shade, inside))) + (255,)
                elif d <= r + 0.8 and sx + sy < 0:
                    px[x, y] = rim + (255,)
    return img


def gatenkaas_tex():
    img = noisy(CHEESE, 7, 101)
    return holes_on(img, [(4, 4, 2.3), (12, 6, 1.6), (7, 12, 2.0), (14, 14, 1.2), (1.5, 9.5, 1.0)])


def bricks(base, mortar, seed, hole_list=(), light=None):
    img = noisy(base, 6, seed)
    px = img.load()
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 4 if row % 2 else 0
            if y % 4 == 3 or (x + off) % 8 == 7:
                px[x, y] = mortar + (255,)
            elif light and (y % 4 == 0 or (x + off) % 8 == 0):
                px[x, y] = light + (255,)
    if hole_list:
        holes_on(img, hole_list, rim=light or CHEESE_LIGHT, inside=tuple(int(c * 0.72) for c in base))
    return img


def tiles(base, mortar, seed, light):
    img = noisy(base, 5, seed)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x % 8 == 7 or y % 8 == 7:
                px[x, y] = mortar + (255,)
            elif x % 8 == 0 or y % 8 == 0:
                px[x, y] = light + (255,)
    return img


def kaaskorrel_ore():
    img = gatenkaas_tex()
    px = img.load()
    for (x, y) in ((3, 9), (4, 9), (3, 10), (10, 3), (11, 3), (11, 4), (9, 11), (10, 11), (13, 9), (6, 2)):
        px[x, y] = (255, 252, 232, 255)
    for (x, y) in ((4, 10), (10, 4), (10, 12), (12, 4)):
        px[x, y] = (236, 222, 170, 255)
    for (x, y) in ((3, 8), (11, 2), (9, 10)):
        px[x, y] = (255, 255, 255, 255)
    return img


def sensor_textures(h):
    side = noisy(AGED, 6, 201)
    px = side.load()
    for x in range(16):
        for y in range(8, 16):
            if y in (8, 9):
                px[x, y] = AGED_DARK + (255,)
    holes_on(side, [(4, 12.5, 1.3), (11, 13, 1.1)], rim=AGED_LIGHT, inside=AGED_DARK)
    h.save(side, "block", "knabbelsensor_side.png")
    top = gatenkaas_tex()
    h.save(top, "block", "knabbelsensor_top.png")
    h.save(noisy(AGED_DARK, 5, 202), "block", "knabbelsensor_bottom.png")
    for active in (False, True):
        ear = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        epx = ear.load()
        outer = (255, 214, 90) if active else (214, 140, 160)
        inner = (255, 250, 200) if active else (170, 90, 120)
        for y in range(16):
            for x in range(16):
                d = math.hypot((x + 0.5 - 8) / 7.5, (y + 0.5 - 8) / 7.5)
                if d <= 1:
                    epx[x, y] = (inner if d < 0.6 else outer) + (255,)
        h.save(ear, "block", "knabbelsensor_ear" + ("_active" if active else "") + ".png")


def shrieker_textures(h):
    fur = (150, 74, 112)
    side = noisy(fur, 8, 301)
    px = side.load()
    for x in range(16):                                   # angry little eyes and a grin all around
        for y in range(16):
            if y >= 8:
                px[x, y] = clamp((fur[0] * 0.7, fur[1] * 0.7, fur[2] * 0.7)) + (255,)
    for (x, y) in ((4, 3), (11, 3)):
        px[x, y] = (230, 30, 40, 255)
        px[x - 1, y - 1] = (20, 10, 20, 255)
        px[x + (1 if x < 8 else -1), y - 1] = (20, 10, 20, 255)
    for x in range(5, 11):
        px[x, 5 + (1 if 6 <= x <= 9 else 0)] = (30, 10, 20, 255)
    h.save(side, "block", "knabbelschreeuwer_side.png")
    h.save(noisy((90, 40, 60), 6, 302), "block", "knabbelschreeuwer_bottom.png")
    for name, glow in (("knabbelschreeuwer_top", False), ("knabbelschreeuwer_inner_top", False), ("knabbelschreeuwer_can_summon_inner_top", True)):
        img = Image.new("RGBA", (16, 16), fur + (255,))
        d = ImageDraw.Draw(img)
        if name.endswith("_top") and "inner" not in name:
            # the lips of the big mouth, with teeth
            d.rectangle((0, 0, 15, 15), fill=fur + (255,))
            d.ellipse((2, 2, 13, 13), fill=(60, 10, 25, 255))
            for (x, y) in ((4, 3), (7, 2), (10, 3), (3, 7), (12, 7), (4, 11), (7, 12), (10, 11)):
                d.rectangle((x, y, x + 1, y + 1), fill=(250, 246, 236, 255))
            img.putpixel((3, 3), (255, 255, 255, 255))
        else:
            d.rectangle((0, 0, 15, 15), fill=(70, 12, 28, 255))
            d.ellipse((4, 4, 11, 11), fill=(120, 20, 40, 255))
            if glow:                                      # a Mika-built one has a red glow deep in its throat
                d.ellipse((6, 6, 9, 9), fill=(255, 60, 60, 255))
        h.save(img, "block", f"{name}.png")


ITEM_PAL = {
    ".": (0, 0, 0, 0), "k": (70, 45, 20, 255), "w": (255, 255, 255, 255), "c": (255, 250, 230, 255), "C": (236, 222, 170, 255),
    "y": CHEESE + (255,), "Y": CHEESE_LIGHT + (255,), "d": CHEESE_DARK + (255,), "b": (120, 130, 200, 255), "B": (80, 90, 160, 255),
    "l": (190, 180, 230, 255), "p": PINK + (255,), "P": (200, 90, 140, 255),
}
ICONS = {
    "kaaskorrel": ["................", "................", "......kk........", ".....kcck.......", "....kcwcCk......", "....kccCCk..kk..",
                   "...kccwcCCkkcck.", "...kcccCCCkcCCk.", "..kcwccCCCCkCCk.", "..kccccCCCCkkk..", "..kccCCCCCCk....", "...kcCCCCCk.....",
                   "....kCCCCk......", ".....kkkk.......", "................", "................"],
    "stille_knabbel": ["................", ".........b......", "........bB..b...", "........Bbb.bB..", "..........B.bb..", "....kkkk....B...",
                       "..kkYYYYkk......", ".kYYyyyyyyk.....", ".kYyyydyyyyk....", "kyyyydddyyyk....", "kyyyyydyyydk....", "kyydyyyyyddk....",
                       ".kyyyyyyddk.....", "..kkddddkk......", "....kkkk........", "................"],
}


def stil_icon():
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((1, 3, 16, 16), fill=PINK + (255,), outline=(180, 80, 120, 255))
    d.ellipse((1, 0, 6, 6), fill=PINK + (255,), outline=(180, 80, 120, 255))
    d.ellipse((11, 0, 16, 6), fill=PINK + (255,), outline=(180, 80, 120, 255))
    for x0 in (4, 10):                                   # sleepy closed eyes
        d.line((x0, 8, x0 + 2, 9), fill=(40, 20, 30, 255))
    d.ellipse((6, 10, 11, 14), fill=(255, 244, 248, 255))
    d.line((8, 8, 8, 14), fill=(250, 200, 80, 255), width=2)   # a finger on the lips: sst!
    return img


def recolour_dripstone(h):
    for tip in ("up", "down"):
        for t in ("tip", "tip_merge", "frustum", "middle", "base"):
            img = h.vanilla(f"block/pointed_dripstone_{tip}_{t}")
            h.save(h.ramp(img, (150, 100, 30), (255, 228, 130)), "block", f"kaas_stalactiet_{tip}_{t}.png")
    h.save(h.ramp(h.vanilla("item/pointed_dripstone"), (150, 100, 30), (255, 228, 130)), "item", "kaas_stalactiet.png")


# =====================================================================================================================
# block models and states
# =====================================================================================================================
_VJAR = None


def vanilla_json(path):
    import zipfile
    global _VJAR
    if _VJAR is None:
        _VJAR = zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))
    return json.loads(_VJAR.read(f"assets/minecraft/{path}.json"))


def copy_family(h, vanilla_prefix, ours, tex):
    """Stairs, slab and wall like vanilla's tuff bricks: the same models and states, with our names and texture."""
    A = h.A
    for kind in ("stairs", "slab", "wall"):
        v = f"{vanilla_prefix}_{kind}"
        us = {"stairs": f"{ours}_trap", "slab": f"{ours}_plaat", "wall": f"{ours}_muur"}[kind]
        state = json.dumps(vanilla_json(f"blockstates/{v}"))
        state = state.replace(f"minecraft:block/{v}", f"guhs:block/{us}").replace(f"minecraft:block/{vanilla_prefix}s", f"guhs:block/{tex}")
        h.w(f"{A}/blockstates/{us}.json", json.loads(state))
        suffixes = {"stairs": ["", "_inner", "_outer"], "slab": ["", "_top"], "wall": ["_post", "_side", "_side_tall", "_inventory"]}[kind]
        for sfx in suffixes:
            model = vanilla_json(f"models/block/{v}{sfx}")
            model["textures"] = {k: f"guhs:block/{tex}" for k in model["textures"]}
            h.w(f"{A}/models/block/{us}{sfx}.json", model)
        h.w(f"{A}/models/item/{us}.json", {"parent": f"guhs:block/{us}" + ("_inventory" if kind == "wall" else "")})
        if kind == "slab":
            h.w(f"{h.D}/loot_table/blocks/{us}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
                {"type": "minecraft:item", "name": f"guhs:{us}", "functions": [
                    {"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [
                        {"condition": "minecraft:block_state_property", "block": f"guhs:{us}", "properties": {"type": "double"}}]},
                    {"function": "minecraft:explosion_decay"}]}]}]})
        else:
            h.self_drop(us)


def stalactite_assets(h):
    A = h.A
    variants = {}
    for tip in ("up", "down"):
        for t in ("tip", "tip_merge", "frustum", "middle", "base"):
            h.w(f"{A}/models/block/kaas_stalactiet_{tip}_{t}.json", {"parent": "minecraft:block/pointed_dripstone", "render_type": "minecraft:cutout",
                                                                        "textures": {"cross": f"guhs:block/kaas_stalactiet_{tip}_{t}"}})
            variants[f"thickness={t},vertical_direction={tip}"] = {"model": f"guhs:block/kaas_stalactiet_{tip}_{t}"}
    h.w(f"{A}/blockstates/kaas_stalactiet.json", {"variants": variants})
    h.item_model("kaas_stalactiet")
    h.self_drop("kaas_stalactiet")


def sensor_assets(h):
    A = h.A
    for active in (False, True):
        sfx = "_active" if active else ""
        ear = f"#ear"
        h.w(f"{A}/models/block/knabbelsensor{sfx}.json", {
            "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"particle": "guhs:block/knabbelsensor_side", "side": "guhs:block/knabbelsensor_side", "top": "guhs:block/knabbelsensor_top",
                         "bottom": "guhs:block/knabbelsensor_bottom", "ear": f"guhs:block/knabbelsensor_ear{sfx}"},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 8, 16], "faces": {
                    "north": {"uv": [0, 8, 16, 16], "texture": "#side", "cullface": "north"},
                    "south": {"uv": [0, 8, 16, 16], "texture": "#side", "cullface": "south"},
                    "east": {"uv": [0, 8, 16, 16], "texture": "#side", "cullface": "east"},
                    "west": {"uv": [0, 8, 16, 16], "texture": "#side", "cullface": "west"},
                    "up": {"uv": [0, 0, 16, 16], "texture": "#top"}, "down": {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "down"}}},
                # two round listening guh ears, a bit tilted outwards (they're what light up)
                {"from": [2, 8, 7.5], "to": [8, 15, 8.5], "rotation": {"origin": [5, 8, 8], "axis": "z", "angle": 22.5},
                 "faces": {f: {"uv": [0, 0, 16, 16], "texture": ear} for f in ("north", "south")}},
                {"from": [8, 8, 7.5], "to": [14, 15, 8.5], "rotation": {"origin": [11, 8, 8], "axis": "z", "angle": -22.5},
                 "faces": {f: {"uv": [0, 0, 16, 16], "texture": ear} for f in ("north", "south")}},
                {"from": [2, 8, 7.5], "to": [8, 15, 8.5], "rotation": {"origin": [5, 8, 8], "axis": "z", "angle": 22.5},
                 "faces": {f: {"uv": [0, 0, 1, 16], "texture": ear} for f in ("east", "west", "up")}},
                {"from": [8, 8, 7.5], "to": [14, 15, 8.5], "rotation": {"origin": [11, 8, 8], "axis": "z", "angle": -22.5},
                 "faces": {f: {"uv": [0, 0, 1, 16], "texture": ear} for f in ("east", "west", "up")}},
            ]})
    h.w(f"{A}/blockstates/knabbelsensor.json", {"variants": {
        "active=false": {"model": "guhs:block/knabbelsensor"}, "active=true": {"model": "guhs:block/knabbelsensor_active"}}})
    h.w(f"{A}/models/item/knabbelsensor.json", {"parent": "guhs:block/knabbelsensor"})
    for summon in (False, True):
        name = "knabbelschreeuwer" + ("_can_summon" if summon else "")
        h.w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/template_sculk_shrieker", "textures": {
            "bottom": "guhs:block/knabbelschreeuwer_bottom", "side": "guhs:block/knabbelschreeuwer_side", "top": "guhs:block/knabbelschreeuwer_top",
            "inner_top": "guhs:block/knabbelschreeuwer" + ("_can_summon" if summon else "") + "_inner_top",
            "particle": "guhs:block/knabbelschreeuwer_bottom"}})
    h.w(f"{A}/blockstates/knabbelschreeuwer.json", {"variants": {
        f"can_summon={c},shrieking={s}": {"model": "guhs:block/knabbelschreeuwer" + ("_can_summon" if c == "true" else "")}
        for c in ("false", "true") for s in ("false", "true")}})
    h.w(f"{A}/models/item/knabbelschreeuwer.json", {"parent": "guhs:block/knabbelschreeuwer"})
    # (like sculk: only with silk touch; otherwise just a bit of xp from the block itself... here: nothing)
    for name in ("knabbelsensor", "knabbelschreeuwer"):
        h.w(f"{h.D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"guhs:{name}"}], "conditions": [
            {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
                {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}]})


# =====================================================================================================================
# the Vadswaker: a big blind Mika (the Mika model with a sleep mask and huge listening ears)
# =====================================================================================================================
MASK = (44, 46, 96)
MASK_LIGHT = (210, 205, 250)
EAR_RIM = (150, 96, 40)
EAR_INNER = (244, 150, 180)


def vadswaker_model(h):
    A = h.A
    geo = json.load(open(f"{A}/geckolib/models/entity/mika.geo.json", encoding="utf-8"))
    g = geo["minecraft:geometry"][0]
    g["description"]["identifier"] = "geometry.vadswaker"
    g["description"]["visible_bounds_width"] = 4
    g["description"]["visible_bounds_height"] = 3

    def faces(u, v, sx, sy, sz):
        """Box-like per-face uv from a corner (u, v)."""
        return {"north": {"uv": [u + sz, v + sz], "uv_size": [sx, sy]}, "south": {"uv": [u + 2 * sz + sx, v + sz], "uv_size": [sx, sy]},
                "east": {"uv": [u, v + sz], "uv_size": [sz, sy]}, "west": {"uv": [u + sz + sx, v + sz], "uv_size": [sz, sy]},
                "up": {"uv": [u + sz, v], "uv_size": [sx, sz]}, "down": {"uv": [u + sz + sx, v], "uv_size": [sx, sz]}}
    extra = {
        # the sleep mask over his eyes (he's blind: he only hears)
        "head": [{"origin": [-7.3, 7.2, -12.45], "size": [14.6, 3.4, 0.6], "uv": faces(0, 84, 15, 4, 1)},
                 {"origin": [-7.35, 7.4, -12.2], "size": [0.4, 3.0, 3.0], "uv": faces(0, 92, 1, 3, 3)},
                 {"origin": [6.95, 7.4, -12.2], "size": [0.4, 3.0, 3.0], "uv": faces(0, 92, 1, 3, 3)}],
        # huge dish ears that hear every crumb
        "ear_left": [{"origin": [4.5, 11.0, -8.9], "size": [10, 9, 0.8], "uv": faces(40, 84, 10, 9, 1)}],
        "ear_right": [{"origin": [-14.5, 11.0, -8.9], "size": [10, 9, 0.8], "uv": faces(40, 84, 10, 9, 1)}],
    }
    for bone in g["bones"]:
        bone.setdefault("cubes", []).extend(extra.get(bone["name"], []))
    h.w(f"{A}/geckolib/models/entity/vadswaker.geo.json", geo)

    # --- the texture: the Mika, gone the colour of old cheese from all the stolen knabbels, with holes in its fur ---
    src = Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")
    tex = h.recolour(src, hue=0.085, sat=1.25, val=0.78, only=h.pinkish)
    a = np.asarray(tex).copy()
    glow = np.zeros_like(a)
    rng = random.Random(77)
    hh, ww = a.shape[:2]
    mika = np.asarray(src)
    fur = [(x, y) for y in range(hh) for x in range(ww) if mika[y, x, 3] > 0 and y < 82]
    for _ in range(90):                                   # cheese holes in the fur: dark, with a glowing rim
        x, y = rng.choice(fur)
        a[y, x] = [92, 52, 18, 255]
        for (dx, dy) in ((1, 0), (0, 1)):
            if x + dx < ww and y + dy < hh and mika[y + dy, x + dx, 3] > 0:
                a[y + dy, x + dx] = [255, 214, 90, 255]
                glow[y + dy, x + dx] = [255, 214, 90, 255]
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    # the mask (uv 0,84: 15 x 4 front at (1, 85))
    d.rectangle((0, 84, 39, 91), fill=MASK + (255,))
    for x0 in (3, 10):                                    # stitched sleeping eyes
        for i, dy in enumerate((0, 1, 1, 1, 0)):
            img.putpixel((x0 + i, 86 + dy), MASK_LIGHT + (255,))
    img.putpixel((8, 85), (255, 170, 205, 255))           # a tiny pink heart in the middle, gift of a guh long ago
    d.rectangle((0, 92, 12, 99), fill=MASK + (255,))
    # the ears: rim and pink inside (front face at 41,85 size 10x9)
    d.rectangle((40, 84, 63, 94), fill=EAR_RIM + (255,))
    d.ellipse((42, 86, 50, 93), fill=EAR_INNER + (255,))
    img.putpixel((44, 88), (255, 214, 230, 255))
    h.save(img, "entity", "vadswaker.png")
    g2 = Image.fromarray(glow)
    h.save(g2, "entity", "vadswaker_glowmask.png")

    # --- animations: the guh idle/walk, plus climbing out of / into the ground, roaring, sniffing and a paw swipe ---
    guh = json.load(open(f"{A}/geckolib/animations/entity/guh.animation.json", encoding="utf-8"))["animations"]
    anims = {"animation.vadswaker.idle": guh["animation.guh.idle"], "animation.vadswaker.walk": json.loads(json.dumps(guh["animation.guh.walk"]))}
    anims["animation.vadswaker.walk"]["animation_length"] = 0.9
    for bone in anims["animation.vadswaker.walk"]["bones"].values():      # (a slower, heavier walk)
        for name, chan in list(bone.items()):
            if isinstance(chan, dict):
                bone[name] = {str(round(float(k) * 1.5, 3)): v for k, v in chan.items()}
    anims["animation.vadswaker.emerge"] = {"loop": "hold_on_last_frame", "animation_length": 3.0, "bones": {
        "root": {"position": {"0.0": [0, -34, 0], "0.6": [0, -26, 0], "1.4": [0, -16, 0], "2.2": [0, -6, 0], "2.8": [0, 1, 0], "3.0": [0, 0, 0]},
                 "rotation": {"0.0": [-20, 0, 0], "1.0": [-10, 0, 8], "1.8": [-12, 0, -8], "2.6": [-6, 0, 4], "3.0": [0, 0, 0]}},
        "head": {"rotation": {"0.0": [-30, 0, 0], "1.5": [-20, 10, 0], "2.4": [-25, -10, 0], "3.0": [0, 0, 0]}},
        "ear_left": {"rotation": {"0.0": [0, 0, -30], "2.8": [0, 0, -30], "3.0": [0, 0, 0]}},
        "ear_right": {"rotation": {"0.0": [0, 0, 30], "2.8": [0, 0, 30], "3.0": [0, 0, 0]}}}}
    anims["animation.vadswaker.dig"] = {"loop": "hold_on_last_frame", "animation_length": 3.0, "bones": {
        "root": {"position": {"0.0": [0, 0, 0], "0.5": [0, 1, 0], "1.2": [0, -8, 0], "2.2": [0, -20, 0], "3.0": [0, -36, 0]},
                 "rotation": {"0.0": [0, 0, 0], "0.8": [15, 0, 6], "1.6": [18, 0, -6], "2.4": [20, 0, 4], "3.0": [20, 0, 0]}},
        "head": {"rotation": {"0.0": [0, 0, 0], "1.0": [25, 0, 0], "3.0": [35, 0, 0]}},
        "leg_front_left": {"rotation": {"0.0": [0, 0, 0], "0.3": [-50, 0, 0], "0.6": [30, 0, 0], "0.9": [-50, 0, 0], "1.2": [30, 0, 0], "1.5": [-50, 0, 0], "1.8": [0, 0, 0]}},
        "leg_front_right": {"rotation": {"0.0": [0, 0, 0], "0.3": [30, 0, 0], "0.6": [-50, 0, 0], "0.9": [30, 0, 0], "1.2": [-50, 0, 0], "1.5": [30, 0, 0], "1.8": [0, 0, 0]}}}}
    anims["animation.vadswaker.roar"] = {"loop": False, "animation_length": 1.7, "bones": {
        "head": {"rotation": {"0.0": [0, 0, 0], "0.5": [-35, 0, 0], "0.9": [-40, 0, 0], "1.1": [10, 0, 0], "1.7": [0, 0, 0]}},
        "body": {"scale": {"0.0": [1, 1, 1], "0.8": [1.12, 1.12, 1.05], "1.0": [0.94, 0.94, 1], "1.7": [1, 1, 1]}},
        "ear_left": {"rotation": {"0.0": [0, 0, 0], "0.8": [0, 0, -40], "1.7": [0, 0, 0]}},
        "ear_right": {"rotation": {"0.0": [0, 0, 0], "0.8": [0, 0, 40], "1.7": [0, 0, 0]}}}}
    anims["animation.vadswaker.sniff"] = {"loop": False, "animation_length": 1.8, "bones": {
        "head": {"rotation": {"0.0": [0, 0, 0], "0.3": [-12, 20, 0], "0.6": [-8, 20, 0], "0.9": [-12, -20, 0], "1.2": [-8, -20, 0], "1.5": [-15, 0, 0], "1.8": [0, 0, 0]}},
        "ear_left": {"rotation": {"0.0": [0, 0, 0], "0.4": [0, 25, -10], "1.0": [0, -10, 10], "1.8": [0, 0, 0]}},
        "ear_right": {"rotation": {"0.0": [0, 0, 0], "0.4": [0, -25, 10], "1.0": [0, 10, -10], "1.8": [0, 0, 0]}}}}
    anims["animation.vadswaker.attack"] = {"loop": False, "animation_length": 0.6, "bones": {
        "root": {"position": {"0.0": [0, 0, 0], "0.15": [0, 2, -2], "0.3": [0, 0, 1], "0.6": [0, 0, 0]}},
        "head": {"rotation": {"0.0": [0, 0, 0], "0.15": [-20, 0, 0], "0.3": [25, 0, 0], "0.6": [0, 0, 0]}},
        "leg_front_left": {"rotation": {"0.0": [0, 0, 0], "0.15": [-70, 0, 0], "0.3": [20, 0, 0], "0.6": [0, 0, 0]}},
        "leg_front_right": {"rotation": {"0.0": [0, 0, 0], "0.15": [-70, 0, 0], "0.3": [20, 0, 0], "0.6": [0, 0, 0]}}}}
    h.w(f"{A}/geckolib/animations/entity/vadswaker.animation.json", {"format_version": "1.8.0", "animations": anims})
    h.w(f"{A}/models/item/vadswaker_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    h.w(f"{h.D}/loot_table/entities/vadswaker.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaaskorrel", "functions": h.count_fn(6, 10) + [
            {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "functions": h.count_fn(2, 3)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaas_knabbels", "functions": h.count_fn(16, 32)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:stille_knabbel", "functions": h.count_fn(2, 4)}]}]})


# =====================================================================================================================
# worldgen: the biome, its noise, the features
# =====================================================================================================================
# The biome is chosen like the crystal mine (make_v2.guh_sea_and_crystals): multi_noise with a "depth" that is 1 below
# y58 and -1 above y62. We add +1 to the depth where it is "deep": below y 36 (fading out by y 44) AND well inside the
# rock (the terrain density at least 0.28-0.45, some 15-25 blocks under the surface: the Guhmension has valleys down to
# y ~30), and put our own patchy noise in "weirdness" there (everyone else has weirdness 0). There, everything else is
# one whole step away; the gatenkaas biome wants depth [1.6, 2] and weirdness [1.22, 2]: it wins where our noise is
# above ~0.15 (about 30% of the underground), and anywhere else it is at least 0.36 + 1.49 away, more than any other
# biome ever is, so it never reaches the surface (checked on a dev server: see NOTES_gatenkaas.md).
DEPTH_FROM, DEPTH_TO = 36, 44
BIOME_POINT = {"temperature": [-1.0, 1.0], "humidity": [-1.0, 1.0], "continentalness": [-1.0, 1.0], "erosion": [-1.0, 1.0],
               "weirdness": [1.22, 2.0], "depth": [1.6, 2.0], "offset": 0.0}


def router_deep(d):
    """The "deep" part of the Guhmension's noise router (noise_settings d): the depth and ridges (weirdness) slots.
    "deep": 1 where we are well inside the rock (the terrain density, without the floor of the valleys, is at least
    0.28-0.45: some 15-25 blocks under the surface) AND below y 36-44; else 0. Under deep valleys it stays 0, so the
    biome never gets near the surface anywhere (the Guhmension has valleys down to y ~30). Made from the terrain in
    final_density, so it is run again when the terrain changes (tools/features/diepzee.py: the deep seas)."""
    r = d["noise_router"]
    final = r["final_density"]
    terrain = final["argument1"] if final.get("type") == "minecraft:max" else final
    solid = {"type": "minecraft:clamp", "min": 0.0, "max": 1.0, "input": {"type": "minecraft:mul", "argument2": 6.0,
             "argument1": {"type": "minecraft:add", "argument1": json.loads(json.dumps(terrain)), "argument2": -0.28}}}
    low = {"type": "minecraft:y_clamped_gradient", "from_y": DEPTH_FROM, "to_y": DEPTH_TO, "from_value": 1.0, "to_value": 0.0}
    deep = {"type": "minecraft:mul", "argument1": low, "argument2": solid}
    base = r["depth"]["argument1"] if r["depth"].get("type") == "minecraft:add" else r["depth"]
    r["depth"] = {"type": "minecraft:add", "argument1": base, "argument2": deep}
    r["ridges"] = {"type": "minecraft:mul",
                   "argument1": {"type": "minecraft:noise", "noise": "guhs:gatenkaasgrotten", "xz_scale": 1.0, "y_scale": 0.0},
                   "argument2": deep}


def patch_worldgen(h):
    D = h.D
    h.w(f"{D}/worldgen/noise/gatenkaasgrotten.json", {"firstOctave": -7, "amplitudes": [1.0, 0.6, 0.3]})

    def biome_source(d):
        entries = [e for e in d["generator"]["biome_source"]["biomes"] if e["biome"] != "guhs:gatenkaasgrotten"]
        entries.append({"biome": "guhs:gatenkaasgrotten", "parameters": dict(BIOME_POINT)})
        d["generator"]["biome_source"]["biomes"] = entries
    h.patch_json(f"{D}/dimension/guhmension.json", biome_source)

    def router(d):
        router_deep(d)
        # the rock of the biome is gatenkaas (there is nothing but "default block" down there)
        rule = {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": ["guhs:gatenkaasgrotten"]},
                "then_run": {"type": "minecraft:block", "result_state": {"Name": "guhs:gatenkaas"}}}
        rules = d["surface_rule"]["sequence"]
        if rule not in rules:
            rules.append(rule)
    h.patch_json(f"{D}/worldgen/noise_settings/guhmension.json", router)

    # --- the features ---
    h.w(f"{D}/worldgen/configured_feature/gatenkaas_holte.json", {"type": "guhs:gatenkaas_holte", "config": {}})
    h.w(f"{D}/worldgen/placed_feature/gatenkaas_holte.json", {"feature": "guhs:gatenkaas_holte", "placement": [
        {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list", "distribution": [
            {"weight": 2, "data": 0}, {"weight": 5, "data": 1}, {"weight": 2, "data": 2}]}},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 10},
                                                      "max_inclusive": {"absolute": 32}}},
        {"type": "minecraft:biome"}]})

    def ore(name, state, size, count, lo, hi, air=0.0):
        h.w(f"{D}/worldgen/configured_feature/{name}.json", {"type": "minecraft:ore", "config": {
            "size": size, "discard_chance_on_air_exposure": air, "targets": [
                {"target": {"predicate_type": "minecraft:block_match", "block": "guhs:gatenkaas"}, "state": {"Name": state}}]}})
        h.w(f"{D}/worldgen/placed_feature/{name}.json", {"feature": f"guhs:{name}", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo},
                                                          "max_inclusive": {"absolute": hi}}},
            {"type": "minecraft:biome"}]})
    ore("gatenkaas_kaaskorrelerts", "guhs:kaaskorrelerts", 5, 6, 1, 42)
    ore("gatenkaas_knabbeladers", "guhs:block_of_kaasknabbels", 14, 3, 1, 42)
    ore("gatenkaas_vadserts", "guhs:compressed_super_vahoege_vads", 4, 3, 1, 30, air=1.0)

    # --- the biome: a copy of the crystal mine's, cheese-yellow and quiet ---
    base = json.load(open(f"{D}/worldgen/biome/guh_kristalmijn.json", encoding="utf-8"))
    b = json.loads(json.dumps(base))
    b["effects"].update({"fog_color": 0xE8C060, "sky_color": 0xF6D98A, "water_color": 0xF2C94C, "water_fog_color": 0xC89A30,
                         "ambient_sound": "minecraft:ambient.cave",
                         "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 3000, "block_search_extent": 8, "offset": 2.0},
                         "particle": {"options": {"type": "minecraft:dust", "color": [1.0, 0.82, 0.3], "scale": 0.7}, "probability": 0.004}})
    b["spawners"] = {k: [] for k in base["spawners"]}
    b["features"] = [["guhs:gatenkaas_holte"], [], [], [], [], [], ["guhs:guhmension_kaasknabbel_veins", "guhs:vahoege_vads_ore",
                     "guhs:gatenkaas_kaaskorrelerts", "guhs:gatenkaas_knabbeladers", "guhs:gatenkaas_vadserts"], [], [], [], []]
    h.w(f"{D}/worldgen/biome/gatenkaasgrotten.json", b)
    # the guh caves (y 5-14) still come in our biome; guh villagers too
    for s in ("guh_caves", "challenging_guh_caves"):
        h.add_tag(f"guhs/tags/worldgen/biome/has_structure/{s}", ["guhs:gatenkaasgrotten"])
    h.patch_json(f"{h.R}/data/neoforge/data_maps/worldgen/biome/villager_types.json",
                 lambda d: d["values"].update({"guhs:gatenkaasgrotten": {"villager_type": "guhs:guh"}}))


def underground_structure(h, name, spacing, separation, salt, height, anchor):
    """An underground jigsaw structure (one template, placed around its anchor jigsaw), only in the gatenkaas caves."""
    D = h.D
    none = {"bounding_box": "full", "spawns": []}
    h.w(f"{D}/worldgen/structure/{name}.json", {
        "type": "minecraft:jigsaw", "biomes": f"#guhs:has_structure/{name}", "step": "underground_structures",
        "spawn_overrides": {"monster": none, "creature": none, "ambient": none, "underground_water_creature": none, "axolotls": none},
        "terrain_adaptation": "none", "start_pool": f"guhs:{name}/start", "size": 1, "start_height": height,
        "start_jigsaw_name": anchor, "max_distance_from_center": 80, "use_expansion_hack": False})
    h.w(f"{D}/worldgen/template_pool/{name}/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{name}",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    h.w(f"{D}/worldgen/structure_set/{name}.json", {
        "structures": [{"structure": f"guhs:{name}", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt,
                      # (the Voorraadkelder stays clear of the Knabbelkelders of the Guheinde, which lie at the same depth)
                      **({"exclusion_zone": {"other_set": "guhs:knabbelkelder", "chunk_count": 8}}
                         if name == "stille_voorraadkelder" else {})}})
    h.w(f"{D}/tags/worldgen/biome/has_structure/{name}.json", {"values": ["guhs:gatenkaasgrotten"]})


# =====================================================================================================================
# structures: shared bits
# =====================================================================================================================
def mc(n):
    return n if ":" in n else f"minecraft:{n}"


# blocks you can walk through / stand in, and ones you can't stand on
THIN = ("rail", "lantern", "chain", "sign", "carpet", "tapijt", "torch", "candle", "cobweb", "ladder", "kaas_stalactiet", "button",
        "pressure_plate")
FENCY = ("fence", "_wall", "muur", "pane", "bars")
LIGHT = {"lantern": 15, "soul_lantern": 10, "shroomlight": 15, "ochre_froglight": 15, "pearlescent_froglight": 15, "glowstone": 15,
         "sea_lantern": 15, "guhs:kaasmos": 9, "guhs:kaasmos_tapijt": 6, "guhs:kaaskorrelerts": 3, "guhs:knabbelsensor": 2,
         "guhs:lampion_geel": 15, "guhs:lampion_roze": 15, "guhs:lampion_mint": 15, "wall_torch": 14, "torch": 14}


class Build:
    """A template being built, plus what the self-check needs."""

    def __init__(self, h, size, seed):
        self.h = h
        self.W, self.H, self.D = size
        self.s = h.Structure(size)
        self.rng = random.Random(seed)
        self.hollow = set()          # the dug-out air
        self.entities = []           # (x, y, z, kind)
        self.targets = {}            # label -> a spot that must be reachable

    def inside(self, x, y, z):
        return 0 <= x < self.W and 0 <= y < self.H and 0 <= z < self.D

    def set(self, x, y, z, name, props=None, nbt=None):
        if self.inside(x, y, z):
            self.s.set(x, y, z, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def dig(self, x, y, z):
        if self.inside(x, y, z):
            self.hollow.add((x, y, z))
            self.s.set(x, y, z, "minecraft:air")

    def air(self, x, y, z):
        return self.get(x, y, z) in (None, "minecraft:air")


def sign(b, x, y, z, facing, keys, wall=True, wood="dark_oak"):
    """A waxed sign with lang keys. Wall signs hang on the block behind them."""
    h = b.h
    blank = json.dumps("")
    msgs = [json.dumps({"translate": f"sign.guhs.{k}"}) if k else blank for k in keys] + [blank] * (4 - len(keys))
    text = {"messages": h.ms.NbtList(8, msgs), "color": "orange", "has_glowing_text": h.Byte(1)}
    empty = {"messages": h.ms.NbtList(8, [blank] * 4), "color": "black", "has_glowing_text": h.Byte(0)}
    nbt = {"id": "minecraft:sign", "is_waxed": h.Byte(1), "front_text": text, "back_text": empty}
    if wall:
        b.set(x, y, z, f"{wood}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
    else:
        rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
        b.set(x, y, z, f"{wood}_sign", {"rotation": rot, "waterlogged": "false"}, nbt)


def lantern(b, x, y, z, hanging=True, soul=False):
    b.set(x, y, z, "soul_lantern" if soul else "lantern", {"hanging": "true" if hanging else "false", "waterlogged": "false"})


def candle(b, x, y, z, n=None, colour=None):
    n = n or b.rng.randint(2, 4)
    colour = colour or b.rng.choice(["yellow", "orange", "white", "brown"])
    b.set(x, y, z, f"{colour}_candle", {"candles": str(n), "lit": "true", "waterlogged": "false"})


def barrel(b, x, y, z, loot=None):
    nbt = {"id": "minecraft:barrel", "LootTable": loot} if loot else None
    b.set(x, y, z, "barrel", {"facing": "up", "open": "false"}, nbt)


def connect(b):
    """Fences, walls and panes connect to their neighbours (like when you place them)."""
    def solid(n):
        return n not in (None, "minecraft:air", "guhs:kaas_saus") and not any(t in n for t in THIN)
    joins = ("_fence", "_pane", "_bars", "_wall", "_muur")
    for (x, y, z), (name, props, nbt) in list(b.s.blocks.items()):
        fence = name.endswith("_fence") or name.endswith("_pane") or name.endswith("_bars")
        wall = name.endswith("_wall") or name.endswith("_muur")
        if not (fence or wall):
            continue
        p = {"waterlogged": "false"}
        for d, (dx, dz) in {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}.items():
            n = b.get(x + dx, y, z + dz)
            ok = bool(n) and (any(n.endswith(s) for s in joins) or (solid(n) and "sign" not in n and "slab" not in n and "plaat" not in n))
            if fence:
                p[d] = "true" if ok else "false"
            else:
                p[d] = "low" if ok else "none"
        if wall:
            straight = (p["north"] != "none" and p["south"] != "none" and p["east"] == "none" and p["west"] == "none") or \
                       (p["east"] != "none" and p["west"] != "none" and p["north"] == "none" and p["south"] == "none")
            above = b.get(x, y + 1, z)
            p["up"] = "false" if straight and (above in (None, "minecraft:air")) else "true"
        b.s.blocks[(x, y, z)] = (name, p, nbt)


def jigsaw_anchor(b, x, y, z, name):
    floor = b.s.blocks[(x, y, z)]
    final = floor[0] + ("[" + ",".join(f"{k}={v}" for k, v in floor[1].items()) + "]" if floor[1] else "")
    b.set(x, y, z, "jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": name, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": final,
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


N6 = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))


def check(b, starts, min_light=0, need_back=None):
    """The geometry self-check: walking like a player from the starts, every target is reachable (and the way back),
    no holes to fall into, everything that hangs or stands has something to hang on / stand on, no floating blocks,
    no kaassaus that can run out, and no dark places to stand (below min_light). Returns a list of problems."""
    blocks = b.s.blocks
    problems = []

    def name(c):
        bl = blocks.get(c)
        return "natural" if bl is None else bl[0]

    def passable(c):
        n = name(c)
        if n == "natural":
            return False
        if n in ("minecraft:air", "guhs:kaas_saus"):
            return True
        if n.endswith("_door"):
            return blocks[c][1].get("open") == "true"
        return any(t in n for t in THIN)

    def fluid(c):
        return name(c) == "guhs:kaas_saus"

    def support(c):
        n = name(c)
        return not passable(c) and not any(t in n for t in FENCY)

    def inside(c):
        return b.inside(*c)

    holes = set()

    def ladder(c):
        return name(c).endswith("ladder")

    def moves(c):
        x, y, z = c
        out = []
        if fluid(c) or ladder(c):
            for dy in (1, -1):
                n = (x, y + dy, z)
                if inside(n) and passable(n):
                    out.append(n)
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            up = (nx, y + 1, nz)
            if inside(up) and passable(up) and passable((nx, y + 2, nz)) and passable((x, y + 2, z)) and support((nx, y, nz)):
                out.append(up)
            n = (nx, y, nz)
            if not (inside(n) and passable(n) and passable((nx, y + 1, nz))):
                continue
            yy = y
            while yy > 0 and not support((nx, yy - 1, nz)) and not fluid((nx, yy, nz)) and not ladder((nx, yy, nz)):
                yy -= 1
            if y - yy > 3:
                holes.add((n, y - yy))
                continue
            out.append((nx, yy, nz))
        return out

    def bfs(froms):
        seen = set(froms)
        todo = list(froms)
        while todo:
            c = todo.pop()
            for n in moves(c):
                if n not in seen:
                    seen.add(n)
                    todo.append(n)
        return seen

    for s in starts:
        if not (passable(s) and passable((s[0], s[1] + 1, s[2])) and support((s[0], s[1] - 1, s[2]))):
            problems.append(f"start {s} isn't a place to stand ({name(s)} on {name((s[0], s[1] - 1, s[2]))})")
    reach = bfs(starts)
    for label, c in b.targets.items():
        if c not in reach:
            problems.append(f"can't reach {label} at {c}")
    if need_back:
        back = bfs([b.targets[need_back]])
        if not any(s in back for s in starts):
            problems.append(f"can't walk back from {need_back} to the way in")
    for (n, fall) in sorted(holes)[:20]:
        problems.append(f"hole: stepping into {n} drops {fall} blocks")

    for (x, y, z, kind) in b.entities:
        if kind == "cart" and "rail" not in name((x, y, z)):
            problems.append(f"cart at {(x, y, z)} is not on a rail")

    for (x, y, z), (n, props, _) in blocks.items():
        below, above = (x, y - 1, z), (x, y + 1, z)
        if n.endswith("lantern"):
            other = above if props.get("hanging") == "true" else below
            if not (support(other) or name(other).endswith("chain") or any(t in name(other) for t in FENCY)):
                problems.append(f"{n} at {(x, y, z)} hangs/stands on nothing")
        if n.endswith("chain") and not (support(above) or name(above).endswith("chain")):
            problems.append(f"chain at {(x, y, z)} hangs on nothing")
        stands = n.endswith("candle") or "carpet" in n or "tapijt" in n or n.endswith("rail") or \
            n in ("guhs:knabbelsensor", "guhs:knabbelschreeuwer", "minecraft:lectern") or (n.endswith("_sign") and "wall" not in n)
        if stands and not support(below):
            problems.append(f"{n} at {(x, y, z)} stands on nothing ({name(below)})")
        if n == "guhs:kaas_stalactiet":
            hold = above if props["vertical_direction"] == "down" else below
            if not (support(hold) or name(hold) == "guhs:kaas_stalactiet"):
                problems.append(f"stalactite at {(x, y, z)} holds on to nothing")
        if n == "guhs:kaas_saus":
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
                o = (x + dx, y + dy, z + dz)
                if name(o) != "guhs:kaas_saus" and passable(o):
                    problems.append(f"kaassaus at {(x, y, z)} runs out into {o}")
                    break
        if (n.endswith("gravel") or n.endswith("sand")) and not support(below):
            problems.append(f"{n} at {(x, y, z)} would fall")

    # floating: every block must hang together with the rock around the template (unset cells, the template's edge)
    solid = {c for c, bl in blocks.items() if bl[0] != "minecraft:air"}
    anchored = set()
    todo = []
    for c in solid:
        x, y, z = c
        if any(not b.inside(x + dx, y + dy, z + dz) or (x + dx, y + dy, z + dz) not in blocks for dx, dy, dz in N6):
            anchored.add(c)
            todo.append(c)
    while todo:
        x, y, z = todo.pop()
        for dx, dy, dz in N6:
            n = (x + dx, y + dy, z + dz)
            if n in solid and n not in anchored:
                anchored.add(n)
                todo.append(n)
    floating = solid - anchored
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. {sorted(floating)[:6]}")

    # light (block light only: there's no sky down here)
    light = {}
    todo = []
    for c, bl in blocks.items():
        n = bl[0]
        lvl = LIGHT.get(n, LIGHT.get(n.split(":")[1], 0)) if n != "minecraft:air" else 0
        if n.endswith("candle") and bl[1].get("lit") == "true":
            lvl = 3 * int(bl[1]["candles"])
        if lvl:
            light[c] = lvl
            todo.append(c)

    def opaque(c):
        n = name(c)
        return not passable(c) and not any(t in n for t in FENCY + ("glass", "stairs", "trap", "slab", "plaat", "chest", "lectern",
                                                                     "sensor", "schreeuwer", "leaves", "ladder"))
    i = 0
    while i < len(todo):
        c = todo[i]
        i += 1
        l = light[c] - 1
        if l <= 0:
            continue
        x, y, z = c
        for dx, dy, dz in N6:
            n = (x + dx, y + dy, z + dz)
            if inside(n) and light.get(n, 0) < l and not opaque(n):
                light[n] = l
                todo.append(n)
    dark = [c for c in reach if light.get(c, 0) < min_light and not fluid(c)]
    if dark:
        problems.append(f"{len(dark)} dark places to stand (light < {min_light}), e.g. {sorted(dark)[:20]}")
    b.stats = {"reachable": len(reach), "blocks": len(blocks), "dark<5": sum(1 for c in reach if light.get(c, 0) < 5)}
    b.reach = reach
    b.dark = dark
    return problems


# =====================================================================================================================
# de verlaten kaasmijnschacht: two crossing tunnels with an old rail track, a room with a chest, a caved-in shaft
# =====================================================================================================================
SW, SH, SD = 35, 13, 35
SF = 2                  # floor level in the template


def build_mijnschacht(h):
    b = Build(h, (SW, SH, SD), 20270301)
    rng = b.rng
    # --- dig: two tunnels (3 wide, 3 high) and the room where they cross (5 high), a shaft up from its corner ---
    for x in range(SW):
        for z in (16, 17, 18):
            for y in range(SF + 1, SF + 4):
                b.dig(x, y, z)
                b.dig(z, y, x)
    for x in range(12, 23):
        for z in range(12, 23):
            for y in range(SF + 1, SF + 6):
                b.dig(x, y, z)
    for y in range(SF + 6, SH - 1):                 # (just wide enough for the ladder: nobody falls off)
        b.dig(21, y, 12)
    # --- the rock around it: gatenkaas with cheese veins (the old miners never finished) ---
    for (x, y, z) in sorted(b.hollow):
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    c = (x + dx, y + dy, z + dz)
                    if c not in b.hollow and b.inside(*c) and b.get(*c) is None:
                        r = rng.random()
                        b.set(*c, "guhs:kaasader" if r < 0.05 else "guhs:uitgemijnde_kaasader" if r < 0.08
                              else "guhs:kaaskorrelerts" if r < 0.09 else "guhs:gatenkaas")
    # the floor: old planks (some rotted away) in the tunnels, cheese bricks in the room
    for (x, y, z) in sorted(b.hollow):
        if y == SF + 1:
            room = 12 <= x <= 22 and 12 <= z <= 22
            b.set(x, SF, z, "guhs:gatenkaas_stenen" if room else ("spruce_planks" if rng.random() > 0.15 else "guhs:gatenkaas"))
    # --- supports every 5 blocks, some of them broken; old wall torches (some burned out) ---
    for a in range(2, SW - 1, 5):
        if 11 <= a <= 23:
            continue
        for along_x in (True, False):
            broken = rng.random() < 0.3
            for s in (16, 18):
                if broken and s == 18:
                    continue
                for y in (SF + 1, SF + 2):
                    b.set(*((a, y, s) if along_x else (s, y, a)), "spruce_fence")
            if not broken:
                for s in (16, 17, 18):
                    b.set(*((a, SF + 3, s) if along_x else (s, SF + 3, a)), "spruce_planks")
                if rng.random() < 0.6:
                    if along_x:
                        b.set(a + 1, SF + 2, 16, "wall_torch", {"facing": "south"})
                    else:
                        b.set(16, SF + 2, a + 1, "wall_torch", {"facing": "east"})
            else:
                b.set(*((a, SF + 3, 16) if along_x else (16, SF + 3, a)), "spruce_slab", {"type": "top", "waterlogged": "false"})
    # --- the rail track along the x tunnel, with gaps; a chest cart and an empty cart ---
    for x in range(1, SW - 1):
        if 12 <= x <= 22 or rng.random() < 0.12 or not b.air(x, SF + 1, 17):
            continue
        b.set(x, SF + 1, 17, "rail", {"shape": "east_west", "waterlogged": "false"})
    for (x, kind) in ((7, "chest"), (28, "plain")):
        b.set(x, SF + 1, 17, "rail", {"shape": "east_west", "waterlogged": "false"})
        nbt = {"id": "minecraft:chest_minecart" if kind == "chest" else "minecraft:minecart", "Rotation": h.floats(90.0, 0.0)}
        if kind == "chest":
            nbt["LootTable"] = "guhs:chests/gatenkaas_mijnschacht"
        b.s.entity(x + 0.5, SF + 1 + 0.0625, 17.5, nbt)
        b.entities.append((x, SF + 1, 17, "cart"))
    # --- cobwebs in the corners, cheese stalactites ---
    for (x, y, z) in sorted(b.hollow):
        if y == SF + 3 and b.air(x, y, z) and not (12 <= x <= 22 and 12 <= z <= 22):
            walls = sum(1 for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) if (x + dx, y, z + dz) not in b.hollow)
            if walls >= 1 and rng.random() < 0.07:
                b.set(x, y, z, "cobweb")
        if (x, y + 1, z) not in b.hollow and b.air(x, y, z) and y >= SF + 4 and (x, z) != (21, 12) and rng.random() < 0.05:
            b.set(x, y, z, "guhs:kaas_stalactiet", {"vertical_direction": "down", "thickness": "tip"})
    # --- the room: the chest of the last mine guh, barrels, a table, a sign, lamp posts, a ladder up the shaft ---
    h.chest(b.s, 13, SF + 1, 21, "east", "guhs:chests/gatenkaas_mijnschacht")
    b.set(14, SF + 1, 22, "guhs:guh_tafel", {"facing": "south"})
    candle(b, 14, SF + 2, 22, 3, "yellow")
    for (x, z) in ((12, 12), (13, 12), (12, 13), (22, 22)):
        barrel(b, x, SF + 1, z)
    b.set(12, SF + 2, 12, "guhs:kaasader")
    for (x, z) in ((15, 13), (19, 21), (13, 16)):
        b.set(x, SF + 1, z, "spruce_fence")
        lantern(b, x, SF + 2, z, hanging=False)
    b.set(17, SF + 5, 17, "chain", {"axis": "y", "waterlogged": "false"})
    lantern(b, 17, SF + 4, 17)
    for y in range(SF + 1, SH - 1):
        b.set(21, y, 12, "ladder", {"facing": "south", "waterlogged": "false"})
    sign(b, 12, SF + 3, 20, "east", ["mijnschacht.bord1", "mijnschacht.bord2", "mijnschacht.bord3"], wood="spruce")
    b.set(17, SF + 1, 13, "rail", {"shape": "east_west", "waterlogged": "false"})     # an old cart in the room
    b.s.entity(17.5, SF + 1.0625, 13.5, {"id": "minecraft:minecart", "Rotation": h.floats(90.0, 0.0)})
    b.entities.append((17, SF + 1, 13, "cart"))
    connect(b)
    jigsaw_anchor(b, 17, SF, 17, "guhs:gatenkaas_mijnschacht_midden")
    b.targets = {"west end": (1, SF + 1, 17), "east end": (SW - 2, SF + 1, 17), "north end": (17, SF + 1, 1), "south end": (17, SF + 1, SD - 2),
                 "the chest": (14, SF + 1, 21), "top of the shaft": (21, SH - 2, 12)}
    problems = check(b, [(1, SF + 1, 16)], need_back="the chest")
    b.s.save("gatenkaas_mijnschacht")
    print(f"gatenkaas_mijnschacht: {b.stats}")
    if problems:
        raise SystemExit("gatenkaas_mijnschacht self-check failed:\n  " + "\n  ".join(problems[:40]))
    print("gatenkaas_mijnschacht: geometry check ok")
    return b


# =====================================================================================================================
# de Stille Voorraadkelder: the secret larder of the Mika's (an Ancient City parody)
#
# A huge round cheese cavern (a dome with cheese holes in its ceiling). In the middle, on a plaza inside a moat of
# kaassaus, the Mika-voorraadschuur: a giant Mika head (two-faced: an angry face at the front, where its open mouth is
# the door, and one at the back), with ears and a devil's tail, full of stolen kaasknabbels, the lore book on a lectern
# and the big chest. Four avenues cross the moat on bridges; around the ring street stand eight little storage huts
# with a stolen guh portrait above the door, four stolen guh statues, lamp posts, and the Vadswaker's sleeping pit.
# Knabbelsensoren everywhere along the streets, knabbelschreeuwers at the crossings. Two tunnels lead out, east/west.
# =====================================================================================================================
KW, KH, KD = 96, 30, 96
KF = 4                  # floor level in the template (the walkable air is KF + 1)
KX, KZ = 48, 48
MOAT0, MOAT1 = 17.0, 19.6
RING0, RING1 = 23.5, 26.5
HUT_R, STATUE_R = 35, 30


def cavern_radius(a):
    return 42.5 + 1.5 * math.sin(3 * a + 1) + 1.0 * math.sin(5 * a + 2)


def cavern_top(x, z):
    d = math.hypot(x - KX, z - KZ)
    r = cavern_radius(math.atan2(z - KZ, x - KX))
    if d > r:
        return None
    return KF + 7 + int(round(15 * math.sqrt(max(0.0, 1 - (d / r) ** 2))))


def mossy(x, z):
    return math.sin(x * 0.35 + 1.3) + math.cos(z * 0.31 - x * 0.12) > 0.1


def street(x, z):
    """Is this floor cell a street (avenue, ring, plaza, bridge)? Returns its kind or None."""
    d = math.hypot(x - KX, z - KZ)
    ax, az = abs(x - KX), abs(z - KZ)
    if d <= MOAT0 - 1:
        return "plaza"
    if ax <= 2 or az <= 2:
        return "avenue"
    if RING0 <= d <= RING1:
        return "ring"
    if ax == 3 or az == 3:
        return "edge"
    if RING0 - 1 <= d <= RING1 + 1:
        return "edge"
    return None


def in_moat(x, z):
    d = math.hypot(x - KX, z - KZ)
    return MOAT0 <= d <= MOAT1 and abs(x - KX) > 3 and abs(z - KZ) > 3


def dig_kelder(b):
    rng = b.rng
    tops = {}
    for x in range(KW):
        for z in range(KD):
            t = cavern_top(x, z)
            if t is not None:
                tops[(x, z)] = t
                for y in range(KF + 1, t + 1):
                    b.dig(x, y, z)
    # round cheese holes bubbling up into the ceiling, and a few round alcoves in the wall
    for _ in range(34):
        a = rng.uniform(0, 2 * math.pi)
        d = rng.uniform(0, 0.8) * cavern_radius(a)
        cx, cz = KX + d * math.cos(a), KZ + d * math.sin(a)
        t = tops.get((int(cx), int(cz)))
        if t is None:
            continue
        r = rng.uniform(2.2, 4.6)
        cy = t + rng.uniform(-0.5, 1.5)
        sphere(b, cx, cy, cz, r, max_y=KH - 3)
    for k in range(12):
        a = k * math.pi / 6 + rng.uniform(-0.2, 0.2)
        if min(abs(math.sin(a)), abs(math.cos(a))) < 0.2:
            continue                                   # (not where the avenues meet the wall)
        r = rng.uniform(3, 4.5)
        d = cavern_radius(a) - 1
        d = min(d, 45 - r)
        sphere(b, KX + d * math.cos(a), KF + 3, KZ + d * math.sin(a), r, min_y=KF + 1)
    # the two ways out, east and west
    for x in list(range(0, 12)) + list(range(KW - 12, KW)):
        for z in range(KZ - 1, KZ + 2):
            for y in range(KF + 1, KF + 5):
                if (x, y, z) not in b.hollow:
                    b.dig(x, y, z)
    b.tops = tops


def sphere(b, cx, cy, cz, r, min_y=KF + 1, max_y=KH - 3):
    for x in range(int(cx - r) - 1, int(cx + r) + 2):
        for z in range(int(cz - r) - 1, int(cz + r) + 2):
            for y in range(max(min_y, int(cy - r) - 1), min(max_y, int(cy + r) + 1) + 1):
                if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 + (z + 0.5 - cz) ** 2 <= r * r and b.inside(x, y, z) \
                        and 2 <= x < KW - 2 and 2 <= z < KD - 2:
                    b.dig(x, y, z)


def shell_kelder(b):
    """Two blocks of cheese around everything (so no wool or foreign caves show through), solid cheese under the floor."""
    rng = b.rng
    for (x, y, z) in list(b.hollow):
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                for dz in range(-2, 3):
                    c = (x + dx, y + dy, z + dz)
                    if c not in b.hollow and b.inside(*c) and b.get(*c) is None:
                        aged = math.sin(c[0] * 0.21 + c[1] * 0.4) + math.cos(c[2] * 0.19 - c[1] * 0.3) > 1.1
                        r = rng.random()
                        b.set(*c, "guhs:kaaskorrelerts" if r < 0.012 else "guhs:belegen_kaas_stenen" if aged else "guhs:gatenkaas")
    for x in range(KW):
        for z in range(KD):
            if (x, KF + 1, z) in b.hollow:
                for y in range(0, KF):
                    if b.get(x, y, z) is None:
                        b.set(x, y, z, "guhs:gatenkaas")


def floor_kelder(b):
    for (x, y, z) in list(b.hollow):
        if y != KF + 1:
            continue
        kind = street(x, z)
        d = math.hypot(x - KX, z - KZ)
        if kind == "plaza":
            name = "guhs:belegen_kaas_tegels" if int(d) % 4 else "guhs:gatenkaas_stenen"
        elif kind in ("avenue", "ring"):
            name = "guhs:belegen_kaas_tegels"
        elif kind == "edge":
            name = "guhs:gatenkaas_stenen"
        else:
            name = "guhs:kaasmos" if mossy(x, z) else "guhs:gatenkaas"
        if x < 12 or x >= KW - 12:
            name = "guhs:belegen_kaas_tegels" if abs(z - KZ) <= 1 else name
        b.set(x, KF, z, name)
        if in_moat(x, z):
            b.set(x, KF, z, "guhs:kaas_saus", {"level": "0"})
            b.set(x, KF - 1, z, "guhs:kaas_saus", {"level": "0"})
            b.set(x, KF - 2, z, "guhs:belegen_kaas_stenen")
    # the bridges: railings on both sides where they cross the moat
    for x in range(KW):
        for z in range(KD):
            d = math.hypot(x - KX, z - KZ)
            if MOAT0 - 0.5 <= d <= MOAT1 + 0.5 and (abs(x - KX) == 3 and abs(z - KZ) > 3 or abs(z - KZ) == 3 and abs(x - KX) > 3):
                b.set(x, KF, z, "guhs:gatenkaas_stenen")
                b.set(x, KF + 1, z, "guhs:belegen_kaas_muur")


def mika_face(u, v, door):
    """The face of the Mika head: u = sideways from the middle, v = height above the floor."""
    au = abs(u)
    if door and au <= 1 and 1 <= v <= 4:
        return "air"
    if round(10.6 + (au - 2) * 0.45) == v and 2 <= au <= 7 or 3 <= au <= 6 and round(10.6 + (au - 2) * 0.45) + 1 == v:
        return "black_concrete"                                            # angry eyebrows
    if ((au - 4.5) / 1.8) ** 2 + ((v - 8) / 1.7) ** 2 <= 1:
        if (au, v) in ((4, 8), (5, 8)):
            return "black_concrete"
        if (au, v) == (4, 9) and u < 0 or (au, v) == (5, 9) and u > 0:
            return "white_concrete"
        return "red_concrete"
    if ((au - 8.3) / 1.6) ** 2 + ((v - 5) / 1.1) ** 2 <= 1:
        return "magenta_concrete"                                         # cheeks
    if au <= 1 and v == 7:
        return "magenta_terracotta"                                       # nose
    if (u / 3.2) ** 2 + ((v - 2.8) / 2.6) ** 2 <= 1:
        if au == 2 and v == 4:
            return "white_concrete"                                       # fangs
        return "black_concrete"                                           # the open mouth
    if (u / 5.6) ** 2 + ((v - 3.4) / 3.9) ** 2 <= 1:
        return "white_concrete"                                           # snout
    return None


BARN_R = (11.5, 12.5, 10.5)


def barn_inside(x, y, z, grow=0.0):
    rx, ry, rz = BARN_R
    return ((x - KX) / (rx + grow)) ** 2 + ((y - (KF + 0.5)) / (ry + grow)) ** 2 + ((z - KZ) / (rz + grow)) ** 2 <= 1


def barn(b, h):
    rng = b.rng
    rx, ry, rz = BARN_R
    inner = {(x, y, z) for x in range(KX - 13, KX + 14) for z in range(KZ - 12, KZ + 13) for y in range(KF + 1, KF + 15)
             if barn_inside(x, y, z)}
    shellc = {c for c in inner if any((c[0] + dx, c[1] + dy, c[2] + dz) not in inner
                                     for dx in (-1, 0, 1) for dy in (-1, 0, 1) for dz in (-1, 0, 1) if c[1] + dy > KF)}
    for c in inner - shellc:
        b.set(*c, "air")
    for (x, y, z) in shellc:
        u, v = x - KX, y - KF
        depth = math.sqrt(max(0.0, 1 - (u / rx) ** 2 - ((y - KF - 0.5) / ry) ** 2))
        name = "pink_concrete" if (x * 3 + y * 5 + z) % 9 else "pink_wool"
        if z - KZ > 0.55 * rz * depth:
            name = mika_face(u, v, door=True) or name
        elif z - KZ < -0.55 * rz * depth:
            name = mika_face(-u, v, door=False) or name                # the second face at the back
        b.set(x, y, z, name)
    # the ears (with a stolen knabbel stuck to one of them) and the devil's tail
    for side in (-1, 1):
        ex, ey = KX + side * 7, KF + 12
        for x in range(ex - 5, ex + 6):
            for y in range(ey - 5, ey + 6):
                d = math.hypot(x - ex, (y - ey) * 1.1)
                if d <= 3.8 and y > KF + 8:
                    for z in (KZ - 1, KZ, KZ + 1):
                        if (x, y, z) in inner and (x, y, z) not in shellc:
                            continue
                        b.set(x, y, z, "pink_terracotta" if d <= 2.2 and z == KZ + 1 else "pink_concrete")
    tail = [(KX, KF + 3, KZ - int(rz) - k) for k in range(1, 5)] + [(KX, KF + 3 + k, KZ - int(rz) - 4) for k in range(1, 5)]
    for c in tail:
        b.set(*c, "magenta_concrete")
    tx, ty, tz = tail[-1]
    for dx in (-1, 0, 1):
        b.set(tx + dx, ty + 1, tz, "magenta_concrete")
    b.set(tx, ty + 2, tz, "magenta_concrete")
    b.barn_inner = inner
    b.barn_shell = shellc

    # --- inside: the lectern with the book, the big chest, heaps of stolen knabbels, lamps ---
    F = KF
    idx = [bk[0] for bk in b.bieb.BOOKS].index(BOOK)
    for dx in (-1, 0, 1):
        for dz in (-8, -7, -6):
            b.set(KX + dx, F, KZ + dz, "gold_block")
    b.set(KX, F + 1, KZ - 7, "lectern", {"facing": "south", "has_book": "true", "powered": "false"}, b.bieb.lectern_book(idx))
    h.chest(b.s, KX + 2, F + 1, KZ - 7, "south", "guhs:chests/voorraadschuur")
    h.chest(b.s, KX - 2, F + 1, KZ - 7, "south", "guhs:chests/stille_voorraadkelder")
    for (x, z, facing) in ((KX - 8, KZ, "east"), (KX + 8, KZ, "west")):
        h.chest(b.s, x, F + 1, z, facing, "guhs:chests/stille_voorraadkelder")
    for (hx, hz, r) in ((KX - 6, KZ - 4, 2.6), (KX + 6, KZ - 4, 2.6), (KX - 6, KZ + 4, 2.0), (KX + 6, KZ + 4, 2.0)):
        for x in range(int(hx - r) - 1, int(hx + r) + 2):
            for z in range(int(hz - r) - 1, int(hz + r) + 2):
                top = int(round((r - math.hypot(x - hx, z - hz)) * 1.1))
                for y in range(F + 1, F + 1 + max(0, top)):
                    if (x, y, z) in inner and (x, y, z) not in shellc and b.get(x, y, z) == "minecraft:air":
                        b.set(x, y, z, "guhs:block_of_kaasknabbels" if rng.random() < 0.85 else "yellow_terracotta")
    for (x, z) in ((KX - 4, KZ - 8), (KX + 4, KZ - 8), (KX - 9, KZ - 3), (KX + 9, KZ - 3)):
        if (x, F + 1, z) in inner and (x, F + 1, z) not in shellc:
            barrel(b, x, F + 1, z)
            candle(b, x, F + 2, z)
    for (x, z) in ((KX - 5, KZ), (KX + 5, KZ), (KX, KZ - 3), (KX, KZ + 4)):
        top = max(y for y in range(F + 1, F + 15) if (x, y, z) in inner and (x, y, z) not in shellc)
        for y in range(F + 7, top + 1):
            b.set(x, y, z, "chain", {"axis": "y", "waterlogged": "false"})
        lantern(b, x, F + 6, z)
    # guards: two knabbelsensoren just inside the mouth, a schreeuwer behind the lectern
    for x in (KX - 3, KX + 3):
        b.set(x, F + 1, KZ + 6, "guhs:knabbelsensor", {"active": "false"})
    b.set(KX, F + 1, KZ - 9, "guhs:knabbelschreeuwer", {"can_summon": "true", "shrieking": "false"})
    b.targets["the lectern (voorraadboek)"] = (KX, F + 1, KZ - 5)
    b.targets["the big chest"] = (KX + 2, F + 1, KZ - 6)
    sign(b, KX + 3, F + 1, KZ + 11, "south", ["voorraad.schuur1", "voorraad.schuur2", "voorraad.schuur3", "voorraad.schuur4"], wall=False)


def guh_statue(b, cx, cz, face):
    """A stolen guh statue: a round pink guh on a cheese pedestal, looking at the barn (face: its facing direction)."""
    fx, fz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[face]
    base = KF + 1
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            b.set(x, base, z, "guhs:gatenkaas_stenen" if max(abs(x - cx), abs(z - cz)) < 3 else "guhs:gatenkaas_stenen_plaat",
                  None if max(abs(x - cx), abs(z - cz)) < 3 else {"type": "bottom", "waterlogged": "false"})
    cy = base + 3
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            for y in range(base + 1, base + 6):
                # along the facing axis the guh is longer (3.1), across it 2.5 wide, 2.3 high
                along = (x - cx) * fx + (z - cz) * fz
                across = (x - cx) * fz - (z - cz) * fx
                if (along / 3.1) ** 2 + (across / 2.5) ** 2 + ((y + 0.5 - cy) / 2.3) ** 2 > 1:
                    continue
                name = "pink_wool"
                front = along >= 2
                if front:
                    if abs(across) == 1 and y == cy:
                        name = "black_concrete"                              # eyes
                    elif abs(across) == 2 and y == cy - 1:
                        name = "magenta_concrete"                            # cheeks
                    elif abs(across) <= 1 and y <= cy - 1:
                        name = "white_concrete"                              # snout
                b.set(x, y, z, name)
    for s in (-1, 1):                                                    # ears
        ex, ez = cx + fz * s, cz - fx * s
        b.set(ex, cy + 2, ez, "pink_terracotta")
        b.set(ex, cy + 3, ez, "pink_wool")
    sx, sz = cx + fx * 4, cz + fz * 4
    sign(b, sx, base, sz, face, ["voorraad.beeld1", "voorraad.beeld2", "voorraad.beeld3"], wall=False)
    candle(b, cx + fz * 3 + fx * 3, base + 1, cz - fx * 3 + fz * 3, 3, "yellow")
    candle(b, cx - fz * 3 + fx * 3, base + 1, cz + fx * 3 + fz * 3, 2, "orange")
    b.targets[f"statue at {cx},{cz}"] = (sx + fx, base, sz + fz)


def hut(b, h, n, hx, hz):
    """A little storage hut of aged cheese with a flat roof, a stolen guh portrait above the door, a chest inside."""
    F = KF
    dx, dz = KX - hx, KZ - hz
    if abs(dx) > abs(dz):
        door = (hx + (3 if dx > 0 else -3), hz)
        face = "east" if dx > 0 else "west"
    else:
        door = (hx, hz + (3 if dz > 0 else -3))
        face = "south" if dz > 0 else "north"
    fx, fz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[face]
    for x in range(hx - 3, hx + 4):
        for z in range(hz - 3, hz + 4):
            edge = max(abs(x - hx), abs(z - hz)) == 3
            corner = abs(x - hx) == 3 and abs(z - hz) == 3
            b.set(x, F, z, "guhs:belegen_kaas_tegels")
            for y in range(F + 1, F + 5):
                if edge:
                    b.set(x, y, z, "guhs:gatenkaas_stenen" if corner else "guhs:belegen_kaas_stenen")
                else:
                    b.set(x, y, z, "air")
            b.set(x, F + 5, z, "guhs:belegen_kaas_tegels")
    for x in range(hx - 4, hx + 5):
        for z in range(hz - 4, hz + 5):
            if max(abs(x - hx), abs(z - hz)) == 4:
                b.set(x, F + 5, z, "guhs:belegen_kaas_plaat", {"type": "top", "waterlogged": "false"})
    for x in range(hx - 2, hx + 3):
        for z in range(hz - 2, hz + 3):
            b.set(x, F + 6, z, "guhs:belegen_kaas_plaat", {"type": "bottom", "waterlogged": "false"})
    # the door gap, and the stolen guh portrait above it (a tiny guh face)
    ox, oz = door
    for y in (F + 1, F + 2):
        b.set(ox, y, oz, "air")
    px, pz = fz, -fx                                  # sideways along the wall
    face_rows = {F + 4: ["pink_terracotta", "black_concrete", "pink_terracotta", "black_concrete", "pink_terracotta"],
                 F + 3: ["magenta_concrete", "pink_terracotta", "white_concrete", "pink_terracotta", "magenta_concrete"]}
    for y, row in face_rows.items():
        for i, name in enumerate(row):
            b.set(ox + px * (i - 2), y, oz + pz * (i - 2), name)
    for s in (-2, 2):                                 # its ears on the roof edge
        b.set(ox + px * s, F + 6, oz + pz * s, "pink_terracotta")
    # inside: a chest against the back wall, barrels with candles, a pile of knabbels, a lamp
    bx, bz = hx - fx * 2, hz - fz * 2
    chest_facing = face
    h.chest(b.s, bx, F + 1, bz, chest_facing, "guhs:chests/stille_voorraadkelder")
    b.targets[f"hut {n} chest"] = (bx + fx, F + 1, bz + fz)
    for s in (-2, 2):
        barrel(b, bx + px * s, F + 1, bz + pz * s)
        candle(b, bx + px * s, F + 2, bz + pz * s)
    b.set(hx + px * 2 + fx, F + 1, hz + pz * 2 + fz, "guhs:block_of_kaasknabbels")
    b.set(hx - px * 2 + fx, F + 1, hz - pz * 2 + fz, "brown_carpet")
    lantern(b, hx, F + 4, hz)
    sign(b, ox + px * 2 + fx, F + 2, oz + pz * 2 + fz, face, [f"voorraad.hut{n}a", f"voorraad.hut{n}b", "voorraad.hutc"])
    return (ox + fx, oz + fz)


def lamp_post(b, x, z, soul=False):
    b.set(x, KF + 1, z, "guhs:belegen_kaas_muur")
    b.set(x, KF + 2, z, "guhs:belegen_kaas_muur")
    lantern(b, x, KF + 3, z, hanging=False, soul=soul)


def free_floor(b, x, z):
    """A floor cell with nothing on it yet (and air above)."""
    return (x, KF + 1, z) in b.hollow and b.get(x, KF + 1, z) == "minecraft:air" and b.get(x, KF + 2, z) == "minecraft:air" \
        and b.get(x, KF, z) not in (None, "minecraft:air", "guhs:kaas_saus")


def streets_and_guards(b, h):
    rng = b.rng
    # lamp posts along the avenues and around the ring
    for r in range(22, 44, 7):
        for (ux, uz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for s in (-4, 4):
                x, z = KX + ux * r + uz * s, KZ + uz * r + ux * s
                if free_floor(b, x, z) and b.tops.get((x, z), 0) >= KF + 5:
                    lamp_post(b, x, z)
    for k in range(12):
        a = k * math.pi / 6 + math.pi / 12
        x, z = int(round(KX + (RING1 + 1.5) * math.cos(a))), int(round(KZ + (RING1 + 1.5) * math.sin(a)))
        if free_floor(b, x, z):
            lamp_post(b, x, z, soul=k % 2 == 0)
    # knabbelsensoren: along the avenues (just off the street) and round the ring, a few on the plaza
    sensors = 0
    for r in range(21, 44, 6):
        for (ux, uz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for s in (-5, 5):
                x, z = KX + ux * r + uz * s, KZ + uz * r + ux * s
                if free_floor(b, x, z):
                    b.set(x, KF + 1, z, "guhs:knabbelsensor", {"active": "false"})
                    sensors += 1
    for k in range(16):
        a = k * math.pi / 8 + 0.2
        x, z = int(round(KX + (RING0 - 1.5) * math.cos(a))), int(round(KZ + (RING0 - 1.5) * math.sin(a)))
        if free_floor(b, x, z):
            b.set(x, KF + 1, z, "guhs:knabbelsensor", {"active": "false"})
            sensors += 1
    for (x, z) in ((KX - 13, KZ + 3), (KX + 13, KZ - 3), (KX + 3, KZ - 13), (KX - 3, KZ + 13)):
        if free_floor(b, x, z):
            b.set(x, KF + 1, z, "guhs:knabbelsensor", {"active": "false"})
            sensors += 1
    # knabbelschreeuwers: at the bridge heads and between the huts
    shriekers = 0
    for (ux, uz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for s in (-7, 7):
            x, z = KX + ux * 21 + uz * s, KZ + uz * 21 + ux * s
            if free_floor(b, x, z):
                b.set(x, KF + 1, z, "guhs:knabbelschreeuwer", {"can_summon": "true", "shrieking": "false"})
                shriekers += 1
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        x, z = int(round(KX + 31 * math.cos(a))), int(round(KZ + 31 * math.sin(a)))
        for (ddx, ddz) in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1), (2, 0), (0, 2)):
            if free_floor(b, x + ddx, z + ddz):
                b.set(x + ddx, KF + 1, z + ddz, "guhs:knabbelschreeuwer", {"can_summon": "true", "shrieking": "false"})
                shriekers += 1
                break
    b.sensors, b.shriekers = sensors, shriekers + 1


def sleeping_pit(b):
    """Where the Vadswaker sleeps: a round pit with a giant pillow, at the north end of the north avenue."""
    cx, cz = KX, KZ - 37
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            d = math.hypot(x - cx, z - cz)
            if d <= 3.6 and (x, KF + 1, z) in b.hollow:
                b.set(x, KF, z, "air")
                b.hollow.add((x, KF, z))
                b.set(x, KF - 1, z, "white_wool" if (x + z) % 3 == 0 else "pink_wool")
                if d <= 2 and (x - cx) % 2 == 0:
                    b.set(x, KF, z, "pink_carpet")
    sign(b, cx + 4, KF + 1, cz + 3, "south", ["voorraad.kuil1", "voorraad.kuil2", "voorraad.kuil3"], wall=False)
    b.targets["the sleeping pit"] = (cx, KF, cz + 1)


def decorate_kelder(b):
    rng = b.rng
    for (x, y, z) in sorted(b.hollow):
        if y == KF + 1 and b.get(x, KF, z) == "guhs:kaasmos" and free_floor(b, x, z):
            near_street = any(street(x + dx, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1))
            r = rng.random()
            if r < 0.18:
                b.set(x, y, z, "guhs:kaasmos_tapijt")
            elif r < 0.035 + 0.18 and not near_street:
                n = 1 + rng.randint(0, 2)
                for i in range(n):
                    t = "tip" if i == n - 1 else "frustum" if i == n - 2 else "base"
                    if b.get(x, y + i, z) == "minecraft:air":
                        b.set(x, y + i, z, "guhs:kaas_stalactiet", {"vertical_direction": "up", "thickness": t})
    # stalactites hang from the ceiling (only where it's high: they must not hit anyone's head)
    for (x, z), t in b.tops.items():
        top = max((y for y in range(KF + 1, KH) if (x, y, z) in b.hollow), default=None)
        if top is None or top < KF + 9 or b.get(x, top, z) != "minecraft:air" or b.get(x, top + 1, z) in (None, "minecraft:air"):
            continue
        if rng.random() < 0.035:
            want, n = 1 + rng.randint(0, 3), 0
            while n < want and b.get(x, top - n, z) == "minecraft:air" and top - n > KF + 6:
                n += 1
            for i in range(n):
                th = "tip" if i == n - 1 else "frustum" if i == n - 2 else "middle" if i > 0 else "base"
                b.set(x, top - i, z, "guhs:kaas_stalactiet", {"vertical_direction": "down", "thickness": th})
    # lamps in the tunnels out
    for x in list(range(2, 12, 5)) + list(range(KW - 10, KW - 1, 5)):
        if b.get(x, KF + 5, KZ) not in (None, "minecraft:air"):
            lantern(b, x, KF + 4, KZ)


def light_up(b):
    """Dark corners get a light in the floor: glowing kaasmos off the streets, a froglight tile in a street."""
    done = []
    for (x, y, z) in sorted(b.dark):
        if any(abs(x - dx) + abs(z - dz) <= 6 for dx, dz in done):
            continue
        for (fx, fz) in ((x, z), (x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            floor = b.get(fx, y - 1, fz)
            if floor in ("guhs:gatenkaas", "guhs:kaasmos"):
                b.set(fx, y - 1, fz, "guhs:kaasmos")
                break
            if floor in ("guhs:belegen_kaas_tegels", "guhs:gatenkaas_stenen"):
                b.set(fx, y - 1, fz, "ochre_froglight")
                break
        else:
            continue
        done.append((x, z))


def build_kelder(h):
    from features import bibliotheek
    b = Build(h, (KW, KH, KD), 20270303)
    b.bieb = bibliotheek
    dig_kelder(b)
    shell_kelder(b)
    floor_kelder(b)
    barn(b, h)
    for k in range(8):
        a = math.pi / 8 + k * math.pi / 4
        hut(b, h, k + 1, int(round(KX + HUT_R * math.cos(a))), int(round(KZ + HUT_R * math.sin(a))))
    for k, face in enumerate(("north", "west", "south", "east")):
        # statues on the diagonals, looking at the barn
        a = math.pi / 4 + k * math.pi / 2
        cx, cz = int(round(KX + STATUE_R * math.cos(a))), int(round(KZ + STATUE_R * math.sin(a)))
        dx, dz = KX - cx, KZ - cz
        guh_statue(b, cx, cz, ("east" if dx > 0 else "west") if k % 2 == 0 else ("south" if dz > 0 else "north"))
    sleeping_pit(b)
    streets_and_guards(b, h)
    decorate_kelder(b)
    connect(b)
    jigsaw_anchor(b, KX, KF, KZ + 2, "guhs:voorraadkelder_midden")
    for (ux, uz, label) in ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")):
        b.targets[f"{label} bridge"] = (KX + ux * 18, KF + 1, KZ + uz * 18)
    starts = [(1, KF + 1, KZ), (KW - 2, KF + 1, KZ)]
    problems = check(b, starts, min_light=1, need_back="the lectern (voorraadboek)")
    for _ in range(4):
        if not b.dark:
            break
        light_up(b)
        problems = check(b, starts, min_light=1, need_back="the lectern (voorraadboek)")
    if b.sensors < 30 or b.shriekers < 8:
        problems.append(f"too few guards: {b.sensors} sensors, {b.shriekers} schreeuwers")
    b.s.save("stille_voorraadkelder")
    print(f"stille_voorraadkelder: {b.stats}, {b.sensors} knabbelsensoren, {b.shriekers} knabbelschreeuwers")
    if problems:
        raise SystemExit("stille_voorraadkelder self-check failed:\n  " + "\n  ".join(problems[:40]))
    print("stille_voorraadkelder: geometry check ok")
    return b


# =====================================================================================================================
# names and texts (Dutch, in every language)
# =====================================================================================================================
HUT_WARES = ["Kaasknabbels", "Gefrituurd!", "Kaassaus", "Goudkaas", "Van Guhdorp", "Van de kermis", "Van oma guh", "Alles van ons!"]
LANG = {
    "block.guhs.gatenkaas": "Gatenkaas",
    "block.guhs.gatenkaas_stenen": "Gatenkaasstenen",
    "block.guhs.gatenkaas_stenen_trap": "Gatenkaasstenen trap",
    "block.guhs.gatenkaas_stenen_plaat": "Gatenkaasstenen plaat",
    "block.guhs.gatenkaas_stenen_muur": "Gatenkaasstenen muur",
    "block.guhs.belegen_kaas_stenen": "Belegen kaasstenen",
    "block.guhs.belegen_kaas_tegels": "Belegen kaastegels",
    "block.guhs.belegen_kaas_trap": "Belegen kaastrap",
    "block.guhs.belegen_kaas_plaat": "Belegen kaasplaat",
    "block.guhs.belegen_kaas_muur": "Belegen kaasmuur",
    "block.guhs.kaas_stalactiet": "Kaasstalactiet",
    "block.guhs.kaasmos": "Gloeiend kaasmos",
    "block.guhs.kaasmos_tapijt": "Kaasmostapijt",
    "block.guhs.kaaskorrelerts": "Kaaskorrelerts",
    "block.guhs.knabbelsensor": "Knabbelsensor",
    "block.guhs.knabbelschreeuwer": "Knabbelschreeuwer",
    "item.guhs.kaaskorrel": "Kaaskorrel",
    "item.guhs.stille_knabbel": "Stille knabbel",
    "item.guhs.vadswaker_spawn_egg": "Vadswaker-spawnei",
    "entity.guhs.vadswaker": "Vadswaker",
    "effect.guhs.stil": "Stil",
    "biome.guhs.gatenkaasgrotten": "Gatenkaasgrotten",
    "structure.guhs.stille_voorraadkelder": "Stille Voorraadkelder",
    "structure.guhs.stille_voorraadkelder.tooltip": "Diep in de Gatenkaasgrotten: de geheime voorraadkelder van de Mika's. SSST, niet knabbelen!",
    "structure.guhs.gatenkaas_mijnschacht": "Verlaten kaasmijnschacht",
    "structure.guhs.gatenkaas_mijnschacht.tooltip": "Een oude mijnschacht in de Gatenkaasgrotten, met een karretje vol buit",
    "gui.guhs.superkompas.ondergrond": "Ondergrond",
    "gui.guhs.superkompas.ondergrond.tooltip": "Diep onder de Guhmensie, in de gatenkaas. Sssst!",
    "quest.guhs.gatenkaas.warning.1": "NJEEEG! Diep onder de kaas spitst iemand zijn oren... (1/3)",
    "quest.guhs.gatenkaas.warning.2": "NJEEEG! Er klinkt gekauw, steeds dichterbij... Sluip, guh! (2/3)",
    "quest.guhs.gatenkaas.warning.3": "NJEEEEEEG! De Vadswaker komt eraan! (3/3)",
    "quest.guhs.gatenkaas.vadswaker_wakker": "De Vadswaker kruipt uit de kaas! Hij is blind, maar hij hoort je stappen en vooral je kauwen. Wegrennen of sluipen, VAHOEG!",
    "subtitles.guhs.vadswaker": "Vadswaker knabbelt",
    # signs
    "sign.guhs.mijnschacht.bord1": "SCHACHT 7", "sign.guhs.mijnschacht.bord2": "DICHT! Te veel", "sign.guhs.mijnschacht.bord3": "gaten, njeg!",
    "sign.guhs.voorraad.schuur1": "MIKA-VOORRAAD", "sign.guhs.voorraad.schuur2": "Verboden te", "sign.guhs.voorraad.schuur3": "knabbelen!",
    "sign.guhs.voorraad.schuur4": "- de Mika's",
    "sign.guhs.voorraad.beeld1": "GEJAT!", "sign.guhs.voorraad.beeld2": "uit Guhdorp", "sign.guhs.voorraad.beeld3": "(schattig, njeg)",
    "sign.guhs.voorraad.kuil1": "Hier slaapt de", "sign.guhs.voorraad.kuil2": "VADSWAKER", "sign.guhs.voorraad.kuil3": "SSSSST!!!",
    "sign.guhs.voorraad.hutc": "Niet aankomen!",
    # advancements
    "advancements.guhs.guhmension.find_gatenkaasgrotten.title": "Vol gaten!",
    "advancements.guhs.guhmension.find_gatenkaasgrotten.description": "Daal af in de Gatenkaasgrotten, diep onder de Guhmensie",
    "advancements.guhs.guhmension.gatenkaas_kaaskorrel.title": "Knapperig!",
    "advancements.guhs.guhmension.gatenkaas_kaaskorrel.description": "Hak een kaaskorrel uit de gatenkaas",
    "advancements.guhs.guhmension.find_stille_voorraadkelder.title": "Sssst... niet knabbelen!",
    "advancements.guhs.guhmension.find_stille_voorraadkelder.description": "Vind de Stille Voorraadkelder van de Mika's",
    "advancements.guhs.guhmension.gatenkaas_voorraadboek.title": "Het geheim van de kelder",
    "advancements.guhs.guhmension.gatenkaas_voorraadboek.description": "Vind het dagboek van de Voorraadmika",
    "advancements.guhs.guhmension.vadswaker_gewekt.title": "Wie knabbelt daar?",
    "advancements.guhs.guhmension.vadswaker_gewekt.description": "Maak de Vadswaker wakker (oeps)",
    "advancements.guhs.guhmension.gatenkaas_stille_knabbel.title": "Knabbelen zonder kraken",
    "advancements.guhs.guhmension.gatenkaas_stille_knabbel.description": "Eet een stille knabbel: nu hoort niemand je",
    "advancements.guhs.guhmension.vadswaker_verslagen.title": "Vadswaker? Vadsslaper!",
    "advancements.guhs.guhmension.vadswaker_verslagen.description": "Versla de Vadswaker (niet verplicht, echt niet!)",
}
for _n, _w in enumerate(HUT_WARES, 1):
    LANG[f"sign.guhs.voorraad.hut{_n}a"] = f"VOORRAAD {_n}"
    LANG[f"sign.guhs.voorraad.hut{_n}b"] = _w

BLOCKS = ["gatenkaas", "gatenkaas_stenen", "gatenkaas_stenen_trap", "gatenkaas_stenen_plaat", "gatenkaas_stenen_muur", "belegen_kaas_stenen",
          "belegen_kaas_tegels", "belegen_kaas_trap", "belegen_kaas_plaat", "belegen_kaas_muur", "kaas_stalactiet", "kaasmos", "kaasmos_tapijt",
          "kaaskorrelerts", "knabbelsensor", "knabbelschreeuwer"]
ITEMS = ["kaaskorrel", "stille_knabbel", "vadswaker_spawn_egg"]


def loot_tables(h):
    D = h.D
    from features import bibliotheek
    idx = [bk[0] for bk in bibliotheek.BOOKS].index(BOOK)
    it = lambda name, w, lo=1, hi=1, **kw: {"type": "minecraft:item", "name": name, "weight": w, **({"functions": h.count_fn(lo, hi)} if hi > 1 else {}), **kw}
    swift = {"type": "minecraft:item", "name": "minecraft:book", "weight": 2,
             "functions": [{"function": "minecraft:enchant_randomly", "options": "minecraft:swift_sneak"}]}
    h.w(f"{D}/loot_table/chests/stille_voorraadkelder.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 5, "max": 8}, "entries": [
            it("guhs:kaas_knabbels", 8, 8, 24), it("guhs:gefrituurde_kaasknabbels", 5, 4, 12), it("guhs:kaaskorrel", 4, 1, 4),
            it("guhs:stille_knabbel", 4, 1, 3), it("guhs:goudkaas", 2, 1, 2), it("guhs:kaasbrok", 4, 4, 12), it("guhs:vahoege_vads_ingot", 2, 1, 2),
            swift, it("minecraft:diamond", 1, 1, 2), it("guhs:kaashoning", 2, 1, 3), it("guhs:guh_kristal", 2, 2, 6),
            it("guhs:music_disc_ze_hangen", 1), it("minecraft:enchanted_golden_apple", 1)]}]})
    h.w(f"{D}/loot_table/chests/voorraadschuur.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [bibliotheek.lore_book_entry(idx)]},
        {"rolls": 1, "entries": [it("guhs:stille_knabbel", 1, 3, 5)]},
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 6}, "entries": [
            it("guhs:block_of_kaasknabbels", 4, 2, 6), it("guhs:kaaskorrel", 4, 3, 6), it("guhs:goudkaas", 3, 1, 3),
            it("guhs:vahoege_vads_ingot", 2, 1, 3), swift, it("minecraft:diamond", 2, 1, 3), it("minecraft:enchanted_golden_apple", 1)]}]})
    h.w(f"{D}/loot_table/chests/gatenkaas_mijnschacht.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            it("guhs:kaasbrok", 6, 3, 10), it("guhs:kaaskorrel", 3, 1, 3), it("minecraft:rail", 4, 4, 12), it("minecraft:torch", 4, 4, 12),
            it("guhs:gefrituurde_kaasknabbels", 3, 2, 6), it("guhs:stille_knabbel", 2, 1, 2), it("guhs:goudkaas", 1),
            it("minecraft:name_tag", 1), it("minecraft:iron_pickaxe", 1), it("guhs:gatenkaas_stenen", 2, 4, 12)]}]})
    # blocks
    for name in ("gatenkaas", "gatenkaas_stenen", "belegen_kaas_stenen", "belegen_kaas_tegels", "kaas_stalactiet", "kaasmos", "kaasmos_tapijt"):
        h.self_drop(name)
    h.w(f"{D}/loot_table/blocks/kaaskorrelerts.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:kaaskorrelerts", "conditions": [
                {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
                    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
            {"type": "minecraft:item", "name": "guhs:kaaskorrel", "functions": h.count_fn(1, 2) + [
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}]}]}]})


def recipes(h):
    D = h.D

    def cut(src, dst, n=1):
        h.w(f"{D}/recipe/{dst}_van_{src}_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": {"item": f"guhs:{src}"},
                                                             "result": {"id": f"guhs:{dst}", "count": n}})

    def cook(src, dst, xp=0.1):
        h.w(f"{D}/recipe/{dst}_van_{src}_smelten.json", {"type": "minecraft:smelting", "category": "blocks", "ingredient": {"item": f"guhs:{src}"},
                                                          "result": {"id": f"guhs:{dst}"}, "experience": xp, "cookingtime": 200})
    h.shaped("gatenkaas_stenen", ["GG", "GG"], {"G": "guhs:gatenkaas"}, "guhs:gatenkaas_stenen", 4)
    h.shaped("belegen_kaas_tegels", ["BB", "BB"], {"B": "guhs:belegen_kaas_stenen"}, "guhs:belegen_kaas_tegels", 4)
    cook("gatenkaas_stenen", "belegen_kaas_stenen")
    for base, fam in (("gatenkaas_stenen", "gatenkaas_stenen"), ("belegen_kaas_stenen", "belegen_kaas")):
        h.shaped(f"{fam}_trap", ["S  ", "SS ", "SSS"], {"S": f"guhs:{base}"}, f"guhs:{fam}_trap", 4)
        h.shaped(f"{fam}_plaat", ["SSS"], {"S": f"guhs:{base}"}, f"guhs:{fam}_plaat", 6)
        h.shaped(f"{fam}_muur", ["SSS", "SSS"], {"S": f"guhs:{base}"}, f"guhs:{fam}_muur", 6)
        cut(base, f"{fam}_trap")
        cut(base, f"{fam}_plaat", 2)
        cut(base, f"{fam}_muur")
    cut("gatenkaas", "gatenkaas_stenen")
    cut("belegen_kaas_stenen", "belegen_kaas_tegels")
    h.shaped("kaasmos_tapijt", ["MM"], {"M": "guhs:kaasmos"}, "guhs:kaasmos_tapijt", 3)
    h.shapeless("kaasmos", ["minecraft:moss_block", "guhs:kaas_knabbels"], "guhs:kaasmos", 1)
    h.shapeless("stille_knabbel", ["guhs:kaaskorrel", "guhs:kaas_knabbels", "guhs:kaasmos"], "guhs:stille_knabbel", 2)
    cook("kaaskorrelerts", "kaaskorrel", 0.7)
    h.w(f"{D}/recipe/kaaskorrel_van_kaaskorrelerts_blast.json", {"type": "minecraft:blasting", "category": "misc",
                                                                   "ingredient": {"item": "guhs:kaaskorrelerts"}, "result": {"id": "guhs:kaaskorrel"},
                                                                   "experience": 0.7, "cookingtime": 100})


def tags(h):
    pick = ["gatenkaas", "gatenkaas_stenen", "gatenkaas_stenen_trap", "gatenkaas_stenen_plaat", "gatenkaas_stenen_muur", "belegen_kaas_stenen",
            "belegen_kaas_tegels", "belegen_kaas_trap", "belegen_kaas_plaat", "belegen_kaas_muur", "kaas_stalactiet", "kaaskorrelerts",
            "knabbelschreeuwer"]
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{n}" for n in pick])
    h.add_tag("minecraft/tags/block/mineable/hoe", ["guhs:kaasmos", "guhs:kaasmos_tapijt", "guhs:knabbelsensor"])
    h.add_tag("minecraft/tags/block/walls", ["guhs:gatenkaas_stenen_muur", "guhs:belegen_kaas_muur"])
    h.add_tag("minecraft/tags/item/walls", ["guhs:gatenkaas_stenen_muur", "guhs:belegen_kaas_muur"])
    h.add_tag("minecraft/tags/block/stairs", ["guhs:gatenkaas_stenen_trap", "guhs:belegen_kaas_trap"])
    h.add_tag("minecraft/tags/item/stairs", ["guhs:gatenkaas_stenen_trap", "guhs:belegen_kaas_trap"])
    h.add_tag("minecraft/tags/block/slabs", ["guhs:gatenkaas_stenen_plaat", "guhs:belegen_kaas_plaat"])
    h.add_tag("minecraft/tags/item/slabs", ["guhs:gatenkaas_stenen_plaat", "guhs:belegen_kaas_plaat"])
    h.add_tag("minecraft/tags/block/dampens_vibrations", ["guhs:kaasmos_tapijt"])


def advancements(h):
    D = h.D
    for name, parent, icon, frame, crit in [
        ("find_gatenkaasgrotten", "enter_guhmension", "guhs:gatenkaas", "task",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"biomes": ["guhs:gatenkaasgrotten"]}}}}),
        ("gatenkaas_kaaskorrel", "find_gatenkaasgrotten", "guhs:kaaskorrel", "task",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:kaaskorrel"}]}}),
        ("gatenkaas_stille_knabbel", "gatenkaas_kaaskorrel", "guhs:stille_knabbel", "task",
         {"trigger": "minecraft:consume_item", "conditions": {"item": {"items": "guhs:stille_knabbel"}}}),
        ("find_stille_voorraadkelder", "find_gatenkaasgrotten", "guhs:knabbelsensor", "goal",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:stille_voorraadkelder"]}}}}),
        ("gatenkaas_voorraadboek", "find_stille_voorraadkelder", "minecraft:written_book", "goal",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
             {"items": "minecraft:written_book", "predicates": {"minecraft:custom_data": "{GuhsBoek:\"%s\"}" % BOOK}}]}}),
        ("vadswaker_gewekt", "find_stille_voorraadkelder", "guhs:knabbelschreeuwer", "task", {"trigger": "minecraft:impossible"}),
        ("vadswaker_verslagen", "vadswaker_gewekt", "guhs:vadswaker_spawn_egg", "challenge",
         {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {"type": "guhs:vadswaker"}}}),
    ]:
        h.w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": {"done": crit}})
    for name in ("found_vadswaker", "gatenkaas_geschreeuw", "gatenkaas_vadswaker_gewekt", "gatenkaas_vadswaker_verslagen"):
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


# =====================================================================================================================
# build
# =====================================================================================================================
def build(h):
    h.ms = __import__("make_structures")
    A, D = h.A, h.D

    # --- textures and models of the blocks ---
    h.save(gatenkaas_tex(), "block", "gatenkaas.png")
    h.save(bricks(CHEESE, CHEESE_DARK, 111, [(12, 2, 1.1), (4, 10, 1.3)], light=CHEESE_LIGHT), "block", "gatenkaas_stenen.png")
    h.save(bricks(AGED, AGED_DARK, 121, [(11, 5, 1.0)], light=AGED_LIGHT), "block", "belegen_kaas_stenen.png")
    h.save(tiles(AGED, AGED_DARK, 131, AGED_LIGHT), "block", "belegen_kaas_tegels.png")
    h.save(h.ramp(h.vanilla("block/moss_block"), (196, 150, 40), (255, 238, 130)), "block", "kaasmos.png")
    h.save(kaaskorrel_ore(), "block", "kaaskorrelerts.png")
    for name in ("gatenkaas", "gatenkaas_stenen", "belegen_kaas_stenen", "belegen_kaas_tegels", "kaasmos", "kaaskorrelerts"):
        h.simple_block(name)
    copy_family(h, "tuff_brick", "gatenkaas_stenen", "gatenkaas_stenen")
    copy_family(h, "tuff_brick", "belegen_kaas", "belegen_kaas_stenen")
    carpet = vanilla_json("models/block/moss_carpet")
    h.w(f"{A}/models/block/kaasmos_tapijt.json", {"parent": carpet["parent"], "textures": {"wool": "guhs:block/kaasmos"}})
    h.w(f"{A}/blockstates/kaasmos_tapijt.json", {"variants": {"": {"model": "guhs:block/kaasmos_tapijt"}}})
    h.w(f"{A}/models/item/kaasmos_tapijt.json", {"parent": "guhs:block/kaasmos_tapijt"})
    recolour_dripstone(h)
    stalactite_assets(h)
    sensor_textures(h)
    shrieker_textures(h)
    sensor_assets(h)

    # --- items, the effect, the Vadswaker ---
    for name, rows in ICONS.items():
        h.save(h.grid(rows, ITEM_PAL), "item", f"{name}.png")
        h.item_model(name)
    h.save(stil_icon(), "mob_effect", "stil.png")
    vadswaker_model(h)

    loot_tables(h)
    recipes(h)
    tags(h)
    advancements(h)
    patch_worldgen(h)

    # --- the structures: the abandoned mine shaft (common in the caves) and the larder (rare) ---
    underground_structure(h, "gatenkaas_mijnschacht", spacing=14, separation=5, salt=20270301,
                          height={"type": "minecraft:uniform", "min_inclusive": {"absolute": 10}, "max_inclusive": {"absolute": 26}},
                          anchor="guhs:gatenkaas_mijnschacht_midden")
    # (y 8: the larder's floor; the cave dome reaches up to y ~30. TODO(merge): exclusion_zone from the Knabbelkelders)
    underground_structure(h, "stille_voorraadkelder", spacing=32, separation=12, salt=20270303, height={"absolute": 8},
                          anchor="guhs:voorraadkelder_midden")
    build_mijnschacht(h)
    build_kelder(h)

    for key, nl in LANG.items():
        h.lang(key, nl, nl)
    selfcheck_assets(h)


def selfcheck_assets(h):
    """check_assets.py only knows the registry classes: this checks our own blocks, items and the Vadswaker."""
    A = h.A
    missing = []
    for b in BLOCKS:
        for p in (f"{A}/blockstates/{b}.json", f"{A}/models/item/{b}.json", f"{h.D}/loot_table/blocks/{b}.json"):
            if not os.path.exists(p):
                missing.append(p)
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ITEMS:
        if not os.path.exists(f"{A}/models/item/{i}.json") or f"item.guhs.{i}" not in h.NL:
            missing.append(f"item {i}")
    for p in (f"{A}/geckolib/models/entity/vadswaker.geo.json", f"{A}/geckolib/animations/entity/vadswaker.animation.json", f"{A}/textures/entity/vadswaker.png",
              f"{A}/textures/entity/vadswaker_glowmask.png", f"{A}/textures/mob_effect/stil.png"):
        if not os.path.exists(p):
            missing.append(p)
    for path in [f"{A}/blockstates/{b}.json" for b in BLOCKS]:
        for v in json.load(open(path, encoding="utf-8")).get("variants", {}).values():
            for m in (v if isinstance(v, list) else [v]):
                ref = m["model"].split(":", 1)
                if ref[0] == "guhs" and not os.path.exists(f"{A}/models/{ref[1]}.json"):
                    missing.append(f"model {m['model']}")
        for part in json.load(open(path, encoding="utf-8")).get("multipart", []):
            for m in (part["apply"] if isinstance(part["apply"], list) else [part["apply"]]):
                ref = m["model"].split(":", 1)
                if ref[0] == "guhs" and not os.path.exists(f"{A}/models/{ref[1]}.json"):
                    missing.append(f"model {m['model']}")
    if missing:
        raise SystemExit(f"gatenkaas assets missing: {sorted(set(missing))[:30]}")


# =====================================================================================================================
# FTB quests (row y = 72, no dependencies)
# =====================================================================================================================
def ftb(fq):
    q, item, adv, structure, biome = fq.q, fq.item, fq.adv, fq.structure, fq.biome
    y = 72
    q("gatenkaas_grotten", "Vol gaten!", "Diep onder de Guhmensie (onder y 40) is de grond op sommige plekken één grote &6gatenkaas&r: ronde holen, kaasstalactieten, druppelende kaassaus en gloeiend kaasmos. Graaf naar beneden en vind de &eGatenkaasgrotten&r!",
      "guhs:gatenkaas", [biome("gatenkaasgrotten")], rewards=(("guhs:kaas_knabbels", 16),), x=-8, y=y, shape="hexagon", xp=100)
    q("gatenkaas_mos", "Gloeiend kaasmos", "Het kaasmos in de grotten gloeit zacht. Verzamel wat kaasmos: handig als lampje, en om &dstille knabbels&r te maken.",
      "guhs:kaasmos", [item("guhs:kaasmos", 8)], x=-6, y=y)
    q("gatenkaas_korrel", "Knapperig!", "In de gatenkaas zit hier en daar &fkaaskorrelerts&r: knapperige kaaskristallen, net als in oude kaas. Hak er een paar uit.",
      "guhs:kaaskorrel", [item("guhs:kaaskorrel", 3)], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-4, y=y)
    q("gatenkaas_stil", "Knabbelen zonder kraken", "Maak een &dstille knabbel&r (kaaskorrel + kaasknabbels + kaasmos) en eet hem op. Anderhalve minuut lang ben je &7Stil&r: niemand hoort je stappen, hakken of kauwen. Ook de Vadswaker niet!",
      "guhs:stille_knabbel", [adv("guhs:guhmension/gatenkaas_stille_knabbel")], x=-2, y=y)
    q("gatenkaas_schacht", "Schacht 7: dicht!", "Ergens in de gatenkaas ligt een &everlaten kaasmijnschacht&r, met een karretje vol buit. Het superkompas (Ondergrond) weet de weg.",
      "minecraft:chest_minecart", [structure("gatenkaas_mijnschacht")], rewards=(("guhs:kaasbrok", 8),), x=0, y=y)
    q("gatenkaas_kelder", "Sssst... niet knabbelen!", "Diep in de Gatenkaasgrotten verstoppen de Mika's al hun gestolen kaasknabbels: de &cStille Voorraadkelder&r. Pas op: &eknabbelsensoren&r horen je lopen, eten en hakken, en &eknabbelschreeuwers&r roepen dan NJEEEG! &6Sluipen&r is veilig.",
      "guhs:knabbelsensor", [structure("stille_voorraadkelder")], rewards=(("guhs:stille_knabbel", 2),), x=2, y=y, shape="octagon", xp=200)
    q("gatenkaas_boek", "Het geheim van de kelder", "In het midden van de kelder staat de Mika-voorraadschuur: een reuzenmikahoofd vol gestolen knabbels. Daarin ligt het &ddagboek van de Voorraadmika&r. Neem het mee (ook voor de bibliotheek!).",
      "minecraft:written_book", [adv("guhs:guhmension/gatenkaas_voorraadboek")], rewards=(("guhs:goudkaas", 1),), x=4, y=y, shape="diamond", xp=200)
    q("gatenkaas_vadswaker", "Wie knabbelt daar?", "Drie keer NJEEEG... en de &cVadswaker&r kruipt uit de kaas: een reuzenmika met een slaapmasker. Hij is blind, maar hij hoort je &6kauwen&r. Kom een keer in de buurt (van een veilige afstand!) voor je Guhdex. Wegrennen of sluipen, VAHOEG!",
      "guhs:vadswaker_spawn_egg", [adv("found_vadswaker")], rewards=(("guhs:gefrituurde_kaasknabbels", 6),), x=6, y=y, xp=150)
    q("gatenkaas_vadswaker_verslagen", "Vadswaker? Vadsslaper!", "Durf je het? Versla de Vadswaker. Hij is heel sterk, dus dit is echt niet verplicht. Een echte guh sluipt gewoon weg.",
      "minecraft:netherite_sword", [fq.kill("guhs:vadswaker")], rewards=(("guhs:vahoege_vads_ingot", 2),), x=8, y=y, shape="gear", xp=500)
