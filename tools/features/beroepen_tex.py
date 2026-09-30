"""
De beroepen (2.9) - textures and block/item models: the marshmallow campfire pit (with its flames), Mika paw prints, the
stolen knabbel sack, the snotkruid plant (4 stages), the mengketel, Bob's ghost roof spot and roof tile, the loaned
guh-brandslang, the kaasmelkdrankje, the snotkruidje, and the four characters' looks (sitting guh models with a fire helmet,
a police cap, a doctor's head mirror and a builder's helmet) plus the Knabbeldief-Mika (a Mika with a burglar's mask, a
striped shirt and a loot sack).
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw

from features import knuffeldal_npcs as npcs

T = 16


def rgba(c, a=255):
    return tuple(c[:3]) + (a,)


def noisy(base, var, seed, size=T):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = rng.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base[:3]) + (255,))
    return img


def put(img, pts, col):
    for (x, y) in pts:
        if 0 <= x < img.width and 0 <= y < img.height:
            img.putpixel((x, y), rgba(col))


# =====================================================================================================================
# block textures
# =====================================================================================================================
def vuurkuil(h):
    """The pit: grey guh-stones in a ring round warm ash, and its side."""
    rng = random.Random(2901)
    top = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            d = math.dist((x + 0.5, y + 0.5), (8, 8))
            if d > 5.2:
                g = 150 + rng.randint(-18, 18)
                top.putpixel((x, y), (g, g - 6, g + 4, 255))
                if (x + y) % 5 == 0:
                    top.putpixel((x, y), (120, 116, 128, 255))
            else:
                a = rng.randint(0, 40)
                glow = d < 2.5 and rng.random() < 0.5
                top.putpixel((x, y), (200, 90 + a, 40, 255) if glow else (60 + a, 50 + a // 2, 50, 255))
    h.save(top, "block", "beroepen_vuurkuil_top.png")
    side = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            stone = ((x // 4 + (y // 3) % 2) % 2 == 0)
            g = (158 if stone else 138) + rng.randint(-12, 12)
            side.putpixel((x, y), (g, g - 4, g + 6, 255))
            if y % 3 == 0 or (x + (y // 3) * 2) % 4 == 0:
                side.putpixel((x, y), (100, 96, 110, 255))
    h.save(side, "block", "beroepen_vuurkuil_zijkant.png")
    # a marshmallow on a stick: pink/white swirl
    mm = Image.new("RGBA", (T, T), (255, 244, 248, 255))
    for y in range(T):
        for x in range(T):
            if (x + y) % 6 in (0, 1):
                mm.putpixel((x, y), (250, 190, 214, 255))
            if (x + y) % 6 == 3 and y > 10:
                mm.putpixel((x, y), (226, 170, 120, 255))    # lightly toasted
    h.save(mm, "block", "beroepen_marshmallow.png")


def pootafdruk(h):
    """Pink Mika paw prints (two, one step apart) on a transparent block top."""
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    col, dark = (236, 104, 160), (190, 60, 120)

    def paw(cx, cy):
        for (x, y) in ((cx - 1, cy), (cx, cy), (cx + 1, cy), (cx - 1, cy + 1), (cx, cy + 1), (cx + 1, cy + 1), (cx, cy + 2)):
            img.putpixel((x, y), rgba(col))
        for (x, y) in ((cx - 2, cy - 2), (cx, cy - 3), (cx + 2, cy - 2)):
            img.putpixel((x, y), rgba(dark))
    paw(4, 11)
    paw(11, 5)
    h.save(img, "block", "beroepen_pootafdruk.png")


def knabbelbuit(h):
    """A burlap sack full of kaasknabbels with a pink Mika paw on it; its top, and a knabbel texture."""
    rng = random.Random(2903)
    sack = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            b = (176, 138, 92) if (x + y * 3) % 4 else (160, 124, 80)
            v = rng.randint(-10, 10)
            sack.putpixel((x, y), (b[0] + v, b[1] + v, b[2] + v, 255))
    for (x, y) in ((6, 7), (7, 7), (8, 7), (9, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9), (5, 5), (7, 4), (8, 4), (10, 5)):
        sack.putpixel((x, y), (236, 104, 160, 255))
    h.save(sack, "block", "beroepen_knabbelbuit.png")
    top = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            v = rng.randint(-14, 14)
            c = (246, 190, 60) if (x * 3 + y * 5) % 7 else (220, 140, 40)
            top.putpixel((x, y), (min(255, c[0] + v), c[1] + v, c[2], 255))
    h.save(top, "block", "beroepen_knabbelbuit_top.png")


def snotkruid(h):
    """The snotkruid plant: four stages, from a sprout to a bushy plant with pale green, dewy (snotty!) droplets."""
    for age in range(4):
        img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
        rng = random.Random(2904 + age)
        height = 4 + age * 3
        stems = [(5, 0), (8, 1), (11, 0)] if age >= 2 else [(8, 0)] if age == 0 else [(6, 0), (10, 0)]
        for (sx, lean) in stems:
            for i in range(height):
                x = sx + (lean if i > height // 2 else 0)
                y = T - 1 - i
                img.putpixel((x, y), (70, 150, 70, 255))
                if i > 1 and i % 2 == 0:
                    for dx in (-1, 1):
                        if 0 <= x + dx < T:
                            img.putpixel((x + dx, y), (110, 200, 100, 255) if rng.random() < 0.8 else (150, 220, 120, 255))
            if age >= 1:
                top = T - height
                for (dx, dy) in ((-1, 0), (1, 0), (0, -1), (-1, -1), (1, -1), (0, 0)):
                    x, y = sx + (lean if height > 2 else 0) + dx, top + dy
                    if 0 <= x < T and 0 <= y < T:
                        img.putpixel((x, y), (120, 214, 110, 255))
            if age == 3:     # the dewy "snot" droplets: pale, shiny green-yellow
                x, y = sx + lean, T - height - 1
                if 0 <= y < T:
                    img.putpixel((x, y), (214, 246, 150, 255))
                    img.putpixel((x, y + 1), (190, 236, 130, 255))
        h.save(img, "block", f"beroepen_snotkruid_{age}.png")


def mengketel(h):
    """The mengketel: a round copper-pink pot with a little guh face on its front, and its bubbly top."""
    rng = random.Random(2905)
    side = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            v = rng.randint(-8, 8)
            band = y in (1, 2) or y in (13, 14)
            c = (200, 110, 110) if band else (236, 150, 160)
            side.putpixel((x, y), (c[0] + v, c[1] + v, c[2] + v, 255))
    h.save(side, "block", "beroepen_mengketel.png")
    face = side.copy()
    put(face, [(5, 6), (5, 7), (10, 6), (10, 7)], (30, 20, 40))
    put(face, [(5, 5), (10, 5)], (255, 255, 255))
    put(face, [(3, 9), (12, 9)], (246, 110, 150))
    put(face, [(7, 10), (8, 10), (6, 9), (9, 9)], (130, 50, 90))
    h.save(face, "block", "beroepen_mengketel_voor.png")
    top = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            rim = x in (0, 15) or y in (0, 15)
            if rim:
                top.putpixel((x, y), (200, 110, 110, 255))
            else:
                v = rng.randint(-10, 10)
                top.putpixel((x, y), (214 + v, 236 + v // 2, 150, 255))
    for (x, y) in ((4, 5), (10, 4), (7, 10), (12, 11), (3, 12)):
        put(top, [(x, y), (x + 1, y), (x, y + 1), (x + 1, y + 1)], (246, 255, 214))
    h.save(top, "block", "beroepen_mengketel_top.png")


def dak(h):
    """Bob's roof: the tile (orange-pink pantiles, each with a tiny guh smile) and the ghost spot where one still goes."""
    rng = random.Random(2906)
    tile = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            row = y // 4
            wave = (x + row * 2) % 8
            shade = 30 if wave in (0, 7) else 0
            v = rng.randint(-8, 8)
            tile.putpixel((x, y), (228 - shade + v, 112 - shade // 2 + v, 86 + v, 255))
            if y % 4 == 3:
                tile.putpixel((x, y), (170, 70, 60, 255))
    for (cx, cy) in ((3, 1), (11, 5), (3, 9), (11, 13)):
        put(tile, [(cx, cy), (cx + 2, cy)], (120, 40, 50))
        put(tile, [(cx + 1, cy + 1)], (120, 40, 50))
    h.save(tile, "block", "beroepen_dakpan.png")
    ghost = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    for y in range(T):
        for x in range(T):
            edge = x in (0, 15) or y in (0, 15)
            if edge or (x + y) % 8 == 0:
                ghost.putpixel((x, y), (255, 170, 120, 200 if edge else 110))
            else:
                ghost.putpixel((x, y), (255, 190, 150, 45))
    for i in range(5, 11):                                       # a "+" in the middle: a tile goes here
        ghost.putpixel((i, 7), (255, 255, 255, 220))
        ghost.putpixel((i, 8), (255, 255, 255, 220))
        ghost.putpixel((7, i), (255, 255, 255, 220))
        ghost.putpixel((8, i), (255, 255, 255, 220))
    h.save(ghost, "block", "beroepen_dakplek.png")


# =====================================================================================================================
# item textures
# =====================================================================================================================
def items(h):
    # the guh-brandslang: a coiled red hose with a brass nozzle and a little guh face on the reel
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((1, 4, 11, 14), outline=(200, 30, 40, 255), width=2)
    d.ellipse((3, 6, 9, 12), outline=(160, 20, 30, 255), width=1)
    d.ellipse((5, 8, 7, 10), fill=(250, 200, 70, 255))
    d.line((10, 8, 14, 3), fill=(200, 30, 40, 255), width=2)
    put(img, [(14, 2), (15, 1), (15, 2), (14, 1)], (240, 190, 60))
    put(img, [(13, 3)], (250, 220, 110))
    put(img, [(15, 0)], (120, 200, 255))
    h.save(img, "item", "guh_brandslang.png")
    # the kaasmelkdrankje: a round bottle of pale green-yellow drink with a pink cork and a guh face label
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((3, 5, 12, 15), fill=(214, 240, 150, 255), outline=(150, 190, 200, 255))
    d.rectangle((6, 1, 9, 5), fill=(210, 236, 240, 255), outline=(150, 190, 200, 255))
    d.rectangle((6, 0, 9, 1), fill=(240, 120, 160, 255))
    put(img, [(6, 9), (9, 9)], (30, 20, 40))
    put(img, [(7, 11), (8, 11)], (200, 70, 110))
    put(img, [(4, 7), (5, 6)], (255, 255, 255))
    h.save(img, "item", "kaasmelkdrankje.png")
    # a snotkruidje: a sprig of pale green leaves with a dewy droplet
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.line((4, 15, 9, 4), fill=(70, 140, 60, 255), width=1)
    for (x, y, r) in ((6, 10, 2), (9, 7, 2), (10, 4, 2), (5, 12, 2), (11, 9, 1)):
        d.ellipse((x - r, y - r, x + r, y + r), fill=(120, 210, 110, 255), outline=(80, 160, 70, 255))
    put(img, [(12, 2), (12, 3), (13, 3)], (220, 250, 170))
    h.save(img, "item", "snotkruidje.png")


# =====================================================================================================================
# block + item models, blockstates
# =====================================================================================================================
def _el(frm, to, faces, rot=None, shade=True):
    e = {"from": frm, "to": to, "faces": faces}
    if rot:
        e["rotation"] = rot
    if not shade:
        e["shade"] = False
    return e


def _all(tex, uv=None, dirs=("north", "south", "east", "west", "up", "down")):
    return {d: ({"texture": tex, "uv": uv} if uv else {"texture": tex}) for d in dirs}


def models(h):
    A, w = h.A, h.w
    # --- the marshmallow campfire pit: out, or burning (1..3, the flames grow) ---
    pit = [
        _el([0, 0, 0], [16, 3, 16], {**_all("#zij", [0, 13, 16, 16], ("north", "south", "east", "west")), "up": {"texture": "#top"},
                                      "down": {"texture": "#zij"}}),
        _el([1, 3, 6], [15, 6, 10], {**_all("#log", [0, 4, 16, 8], ("north", "south", "up", "down")),
                                      **_all("#logtop", [4, 4, 8, 8], ("east", "west"))}),
        _el([6, 3, 1], [10, 6, 15], {**_all("#log", [0, 4, 16, 8], ("east", "west", "up", "down")),
                                      **_all("#logtop", [4, 4, 8, 8], ("north", "south"))}),
        # two marshmallow sticks leaning over the pit
        _el([2.5, 3, 12.5], [3.5, 14, 13.5], _all("#stok"), rot={"origin": [3, 3, 13], "axis": "x", "angle": 22.5}),
        _el([1.5, 13, 11.5], [4.5, 16, 14.5], _all("#mm"), rot={"origin": [3, 3, 13], "axis": "x", "angle": 22.5}),
        _el([12.5, 3, 2.5], [13.5, 14, 3.5], _all("#stok"), rot={"origin": [13, 3, 3], "axis": "z", "angle": -22.5}),
        _el([11.5, 13, 1.5], [14.5, 16, 4.5], _all("#mm"), rot={"origin": [13, 3, 3], "axis": "z", "angle": -22.5}),
    ]
    tex = {"zij": "guhs:block/beroepen_vuurkuil_zijkant", "top": "guhs:block/beroepen_vuurkuil_top", "log": "minecraft:block/oak_log",
           "logtop": "minecraft:block/oak_log_top", "stok": "minecraft:block/stripped_birch_log", "mm": "guhs:block/beroepen_marshmallow",
           "particle": "guhs:block/beroepen_vuurkuil_zijkant"}
    w(f"{A}/models/block/beroepen_marshmallowvuur_uit.json", {"textures": tex, "elements": pit, "render_type": "minecraft:cutout"})
    for n, hgt in ((1, 9), (2, 16), (3, 26)):
        top = min(32, 4 + hgt)
        flames = [
            _el([0, 3, 8], [16, top, 8], {"north": {"texture": "#vuur", "uv": [0, 0, 16, 16]}, "south": {"texture": "#vuur", "uv": [0, 0, 16, 16]}},
                shade=False),
            _el([8, 3, 0], [8, top, 16], {"east": {"texture": "#vuur", "uv": [0, 0, 16, 16]}, "west": {"texture": "#vuur", "uv": [0, 0, 16, 16]}},
                shade=False),
        ]
        w(f"{A}/models/block/beroepen_marshmallowvuur_{n}.json", {"textures": {**tex, "vuur": "minecraft:block/fire_0"},
                                                                   "elements": pit + flames, "render_type": "minecraft:cutout"})
    w(f"{A}/blockstates/beroepen_marshmallowvuur.json", {"variants": {
        f"vuur={n}": {"model": f"guhs:block/beroepen_marshmallowvuur_{'uit' if n == 0 else n}"} for n in range(4)}})
    w(f"{A}/models/item/beroepen_marshmallowvuur.json", {"parent": "guhs:block/beroepen_marshmallowvuur_uit"})

    # --- the paw prints (flat, turned the way the Mika walked) ---
    w(f"{A}/models/block/beroepen_pootafdruk.json", {"textures": {"poot": "guhs:block/beroepen_pootafdruk", "particle": "guhs:block/beroepen_pootafdruk"},
                                                     "render_type": "minecraft:cutout",
                                                     "elements": [_el([0, 0, 0], [16, 0.25, 16], {"up": {"texture": "#poot"}})]})
    w(f"{A}/blockstates/beroepen_pootafdruk.json", {"variants": {
        f"facing={f}": {"model": "guhs:block/beroepen_pootafdruk", **({"y": y} if y else {})}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})

    # --- the knabbel loot: a sack, tied up, knabbels spilling out ---
    w(f"{A}/models/block/beroepen_knabbelbuit.json", {
        "textures": {"zak": "guhs:block/beroepen_knabbelbuit", "top": "guhs:block/beroepen_knabbelbuit_top",
                     "touw": "minecraft:block/stripped_oak_log", "particle": "guhs:block/beroepen_knabbelbuit"},
        "elements": [
            _el([3, 0, 3], [13, 10, 13], {**_all("#zak", None, ("north", "south", "east", "west", "down")), "up": {"texture": "#top"}}),
            _el([5, 10, 5], [11, 11, 11], _all("#zak")),
            _el([6.5, 11, 6.5], [9.5, 13, 9.5], _all("#touw")),
            _el([0.5, 0, 11], [3.5, 2, 14], _all("#top")),
            _el([12, 0, 1], [15, 2, 4], _all("#top")),
            _el([13, 0, 9], [15.5, 1.5, 11.5], _all("#top")),
        ]})
    w(f"{A}/blockstates/beroepen_knabbelbuit.json", {"variants": {"": {"model": "guhs:block/beroepen_knabbelbuit"}}})
    w(f"{A}/models/item/beroepen_knabbelbuit.json", {"parent": "guhs:block/beroepen_knabbelbuit"})

    # --- the snotkruid plant ---
    for age in range(4):
        w(f"{A}/models/block/beroepen_snotkruid_{age}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                               "textures": {"cross": f"guhs:block/beroepen_snotkruid_{age}"}})
    w(f"{A}/blockstates/beroepen_snotkruid.json", {"variants": {f"age={a}": {"model": f"guhs:block/beroepen_snotkruid_{a}"} for a in range(4)}})

    # --- the mengketel (its face to the front) ---
    w(f"{A}/models/block/beroepen_mengketel.json", {
        "textures": {"zij": "guhs:block/beroepen_mengketel", "voor": "guhs:block/beroepen_mengketel_voor", "top": "guhs:block/beroepen_mengketel_top",
                     "poot": "minecraft:block/pink_terracotta", "lepel": "minecraft:block/stripped_oak_log",
                     "particle": "guhs:block/beroepen_mengketel"},
        "elements": [
            _el([2, 2, 2], [14, 13, 14], {"north": {"texture": "#voor"}, "south": {"texture": "#zij"}, "east": {"texture": "#zij"},
                                          "west": {"texture": "#zij"}, "up": {"texture": "#top"}, "down": {"texture": "#zij"}}),
            _el([1, 11, 1], [15, 13, 15], {**_all("#zij", [0, 13, 16, 15], ("north", "south", "east", "west")), "down": {"texture": "#zij"}}),
            _el([3, 0, 3], [5, 2, 5], _all("#poot")), _el([11, 0, 3], [13, 2, 5], _all("#poot")),
            _el([3, 0, 11], [5, 2, 13], _all("#poot")), _el([11, 0, 11], [13, 2, 13], _all("#poot")),
            # the wooden stirring spoon
            _el([10, 10, 9], [11, 18, 10], _all("#lepel"),
                rot={"origin": [10, 10, 9], "axis": "z", "angle": 22.5}),
        ]})
    w(f"{A}/blockstates/beroepen_mengketel.json", {"variants": {
        f"facing={f}": {"model": "guhs:block/beroepen_mengketel", **({"y": y} if y else {})}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    w(f"{A}/models/item/beroepen_mengketel.json", {"parent": "guhs:block/beroepen_mengketel"})

    # --- Bob's roof: the ghost spot (see-through) and the tile ---
    w(f"{A}/models/block/beroepen_dakplek.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                                                  "textures": {"all": "guhs:block/beroepen_dakplek"}})
    w(f"{A}/blockstates/beroepen_dakplek.json", {"variants": {"": {"model": "guhs:block/beroepen_dakplek"}}})
    w(f"{A}/models/block/beroepen_dakpan.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/beroepen_dakpan"}})
    w(f"{A}/blockstates/beroepen_dakpan.json", {"variants": {"": {"model": "guhs:block/beroepen_dakpan"}}})
    w(f"{A}/models/item/beroepen_dakpan.json", {"parent": "guhs:block/beroepen_dakpan"})

    # --- the invisible markers (a particle texture only) ---
    for marker in ("beroepen_guhtjeplek", "beroepen_kluisplek", "beroepen_verstopplek"):
        w(f"{A}/models/block/{marker}.json", {"textures": {"particle": "minecraft:block/pink_stained_glass"}})
        w(f"{A}/blockstates/{marker}.json", {"variants": {"": {"model": f"guhs:block/{marker}"}}})

    # --- items ---
    for item in ("guh_brandslang", "kaasmelkdrankje", "snotkruidje"):
        h.item_model(item)
    # the hose held like a tool
    w(f"{A}/models/item/guh_brandslang.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": "guhs:item/guh_brandslang"}})


# =====================================================================================================================
# the characters
# =====================================================================================================================
def _npc(h, kind, hue, sat, val, bones, paint, seed):
    geo_file = npcs._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = f"geometry.guh_npc_{kind}"
    names = sorted({c[2] for cubes in bones.values() for c in cubes[1]})
    sw = npcs._swatches(geo, names)
    for name, (parent_pivot, cubes) in bones.items():
        parent, pivot = parent_pivot
        geo["bones"].append({"name": name, "parent": parent, "pivot": pivot,
                             "cubes": [npcs._cube(o, s, sw[n], inflate) for (o, s, n, inflate) in cubes]})
    npcs._save_geo(h, f"guh_npc_{kind}.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=hue, sat=sat, val=val, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(seed)
    for name, (colour, var, pattern) in paint.items():
        npcs._paint(a, sw[name], colour, rng, var, pattern)
    h.save(Image.fromarray(a), "entity", f"npc_{kind}.png")


def _stripe(colour, rows):
    def f(block):
        for r in rows:
            block[r * 4:(r + 1) * 4, :, :3] = colour
    return f


def _badge(colour):
    def f(block):
        for yy in range(32):
            for xx in range(32):
                if abs(xx - 15.5) + abs(yy - 15.5) < 11:
                    block[yy, xx, :3] = colour
                if abs(xx - 15.5) + abs(yy - 15.5) < 5:
                    block[yy, xx, :3] = (255, 255, 255)
    return f


def _checks(a, b):
    def f(block):
        for yy in range(32):
            for xx in range(32):
                block[yy, xx, :3] = a if ((xx // 8) + (yy // 8)) % 2 else b
    return f


def _kruis(block):
    block[..., :3] = (255, 255, 255)
    block[10:22, 4:28, :3] = (60, 180, 80)
    block[4:28, 10:22, :3] = (60, 180, 80)


def characters(h):
    H = [0, 13, 0]
    B = [0, 12, -4]
    # Brandweercommandant Blusguh: a red fire helmet with a brim, a comb and a golden badge; a navy jacket with yellow-grey
    # reflective stripes; a moustache of soot (he's been blowing out marshmallow fires all day)
    _npc(h, "brandweerguh", 0.97, 0.5, 1.0, {
        "blus_helm": (("head", H), [([-7.5, 25.2, -8.0], [15, 1.0, 14], "helm", 0), ([-6.0, 26.0, -6.0], [12, 4.5, 11], "helm", 0),
                                    ([-0.8, 26.0, -6.6], [1.6, 6.0, 12], "kam", 0), ([-1.6, 27.2, -7.0], [3.2, 2.6, 0.6], "goud", 0)]),
        "blus_jas": (("body", B), [([-5.2, 2.2, -4.4], [10.4, 9.4, 1.0], "jas", 0), ([-5.2, 5.0, -4.6], [10.4, 1.2, 0.4], "streep", 0),
                                   ([-5.2, 8.0, -4.6], [10.4, 1.2, 0.4], "streep", 0)]),
        "blus_snor": (("head", H), [([-3.0, 16.2, -8.0], [6.0, 1.0, 0.6], "roet", 0)]),
    }, {"helm": ((214, 36, 44), 8, None), "kam": ((180, 20, 30), 6, None), "goud": ((246, 200, 70), 6, _badge((236, 170, 40))),
        "jas": ((40, 48, 82), 6, None), "streep": ((236, 236, 150), 4, None), "roet": ((70, 60, 66), 4, None)}, 2911)
    # Inspecteur Vahoegsma: a navy police cap with a peak, a red-white-blue band and a golden badge; a navy uniform with a
    # blue-orange stripe; a big magnifying glass on a cord
    _npc(h, "politieguh", 0.62, 0.35, 1.0, {
        "politie_pet": (("head", H), [([-6.5, 25.0, -6.0], [13, 2.4, 11], "band", 0), ([-7.0, 27.2, -6.8], [14, 3.2, 12.6], "pet", 0),
                                      ([-5.0, 25.0, -9.2], [10, 0.8, 3.4], "klep", 0), ([-1.3, 27.6, -7.2], [2.6, 2.4, 0.6], "goud", 0)]),
        "politie_uniform": (("body", B), [([-5.2, 2.2, -4.4], [10.4, 9.4, 1.0], "pet", 0), ([-5.2, 6.4, -4.6], [10.4, 1.4, 0.4], "striping", 0)]),
        "politie_loep": (("body", B), [([2.8, 3.4, -5.4], [3.6, 3.6, 0.6], "loep", 0), ([4.2, 1.2, -5.2], [0.8, 2.4, 0.5], "handvat", 0)]),
    }, {"pet": ((30, 42, 90), 6, None), "band": ((220, 220, 230), 4, _checks((40, 60, 160), (230, 230, 240))), "klep": ((20, 24, 40), 4, None),
        "goud": ((246, 200, 70), 6, _badge((236, 170, 40))), "striping": ((40, 90, 200), 4, _stripe((240, 120, 40), (3, 4))),
        "loep": ((190, 230, 250), 6, None), "handvat": ((120, 70, 40), 6, None)}, 2912)
    # Dokter Snotneus-guh: a round head mirror on a band, a white coat with a green cross, a stethoscope, and a big red
    # (always a bit snotty) nose
    _npc(h, "apothekerguh", 0.45, 0.25, 1.05, {
        "dokter_spiegel": (("head", H), [([-6.8, 25.6, -2.0], [13.6, 1.2, 1.4], "band", 0), ([-2.5, 25.0, -7.4], [5.0, 5.0, 0.6], "spiegel", 0),
                                         ([-0.6, 25.8, -6.8], [1.2, 1.2, 4.8], "band", 0)]),
        "dokter_jas": (("body", B), [([-5.3, 2.0, -4.5], [10.6, 9.8, 1.0], "jas", 0), ([1.2, 7.2, -4.8], [3.0, 3.0, 0.4], "kruis", 0)]),
        "dokter_stetho": (("body", B), [([-3.4, 10.6, -4.9], [6.8, 0.6, 0.6], "stetho", 0), ([-3.4, 6.0, -4.9], [0.6, 4.6, 0.6], "stetho", 0),
                                        ([-3.9, 5.2, -5.1], [1.6, 1.2, 0.6], "zilver", 0)]),
        "dokter_neus": (("head", H), [([-1.4, 15.8, -8.6], [2.8, 2.2, 1.6], "neus", 0)]),
    }, {"band": ((230, 230, 236), 4, None), "spiegel": ((210, 230, 240), 8, None), "jas": ((248, 250, 250), 3, None), "kruis": ((60, 180, 80), 2, _kruis),
        "stetho": ((40, 40, 50), 4, None), "zilver": ((200, 206, 214), 6, None), "neus": ((236, 70, 80), 8, None)}, 2913)
    # Bob de Guhbouwer: a yellow builder's helmet, an orange safety vest with reflective stripes, a pencil behind his ear,
    # a tool belt with a hammer
    _npc(h, "bouwvakkerguh", 0.08, 0.45, 1.0, {
        "bob_helm": (("head", H), [([-7.0, 25.2, -7.6], [14, 0.8, 13.4], "helm", 0), ([-6.0, 26.0, -6.0], [12, 4.2, 11], "helm", 0),
                                   ([-0.7, 29.8, -6.2], [1.4, 0.8, 11.4], "helm", 0), ([-1.6, 26.8, -6.4], [3.2, 2.0, 0.5], "sticker", 0)]),
        "bob_potlood": (("head", H), [([5.0, 22.0, -3.0], [0.8, 0.8, 5.0], "potlood", 0)]),
        "bob_hesje": (("body", B), [([-5.2, 2.2, -4.4], [10.4, 9.4, 1.0], "hesje", 0), ([-5.2, 4.2, -4.6], [10.4, 1.0, 0.4], "reflex", 0),
                                    ([-5.2, 7.6, -4.6], [10.4, 1.0, 0.4], "reflex", 0)]),
        "bob_riem": (("body", B), [([-5.6, 1.8, -4.8], [11.2, 1.2, 9.6], "riem", 0), ([3.4, 0.6, -5.4], [1.0, 2.8, 1.0], "hamersteel", 0),
                                   ([2.6, 3.2, -5.6], [2.6, 1.2, 1.4], "hamerkop", 0)]),
    }, {"helm": ((250, 206, 40), 8, None), "sticker": ((240, 110, 160), 4, None), "potlood": ((240, 180, 60), 6, None),
        "hesje": ((250, 130, 30), 8, None), "reflex": ((230, 234, 230), 3, None), "riem": ((110, 70, 40), 6, None),
        "hamersteel": ((150, 100, 60), 6, None), "hamerkop": ((120, 124, 134), 6, None)}, 2914)


def knabbeldief(h):
    """The Knabbeldief-Mika: the Mika's model with a black burglar's mask round its eyes, a black-and-white striped shirt
    (painted), and a little loot sack on its back with knabbels peeking out."""
    geo_file = npcs._load(h, "mika.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.knabbeldief_mika"
    geo["bones"] = [b for b in geo["bones"] if not b["name"].startswith("dief_")]
    sw = npcs._swatches(geo, ["masker", "zak", "knabbel"])
    geo["bones"].append({"name": "dief_masker", "parent": "head", "pivot": [0, 5, -2], "cubes": [
        npcs._cube([-7.2, 5.4, -12.4], [14.4, 2.6, 0.6], sw["masker"]),
        npcs._cube([-7.2, 5.4, -12.4], [0.6, 2.6, 9.0], sw["masker"]), npcs._cube([6.6, 5.4, -12.4], [0.6, 2.6, 9.0], sw["masker"])]})
    geo["bones"].append({"name": "dief_zak", "parent": "body", "pivot": [0, 9, 6], "cubes": [
        npcs._cube([-3.0, 9.0, 2.5], [6.0, 5.0, 6.0], sw["zak"]), npcs._cube([-1.0, 14.0, 4.5], [2.0, 1.0, 2.0], sw["zak"]),
        npcs._cube([1.2, 13.4, 3.0], [1.6, 1.6, 1.6], sw["knabbel"]), npcs._cube([-2.6, 13.2, 5.6], [1.4, 1.4, 1.4], sw["knabbel"])]})
    npcs._save_geo(h, "knabbeldief_mika.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")
    a = np.asarray(src).copy()
    # stripes on the body: every other few rows of the body's fur darker (a striped burglar's shirt)
    body = [b for b in geo["bones"] if b["name"] == "body"][0]
    mask = np.zeros(a.shape[:2], bool)
    for cube in body.get("cubes", []):
        uv = cube.get("uv")
        if isinstance(uv, list):
            u, v = uv
            sx, sy, sz = cube["size"]
            mask[int(v):int(v + sz + sy), int(u):int(u + 2 * (sx + sz))] = True
    for y in range(a.shape[0]):
        if (y // 3) % 2 == 0:
            row = mask[y] & (a[y, :, 3] > 0)
            a[y, row, :3] = (a[y, row, :3] * 0.35).astype(np.uint8)
    rng = np.random.default_rng(2915)
    npcs._paint(a, sw["masker"], (24, 20, 30), rng, 4)
    npcs._paint(a, sw["zak"], (176, 138, 92), rng, 10)
    npcs._paint(a, sw["knabbel"], (246, 190, 60), rng, 10)
    h.save(Image.fromarray(a), "entity", "knabbeldief_mika.png")


def build(h):
    vuurkuil(h)
    pootafdruk(h)
    knabbelbuit(h)
    snotkruid(h)
    mengketel(h)
    dak(h)
    items(h)
    models(h)
    characters(h)
    knabbeldief(h)
