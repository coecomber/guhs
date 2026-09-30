"""
Het Guh-Circuit (2.9) - textures and models: the rainbow road (seven glowing colours), the cheese road, slippery kaassaus,
glittering bergijs, the shimmering boost ring skin, the stuiterpaddenstoel (a pink guh mushroom cap with a little face),
the circuitbeker (a golden cup with guh-ear handles), Coach Vahoegvroem (the sitting guh in sky blue with a chequered cap,
a headset with a microphone and a golden stopwatch), the Mika-pikkers (the Mika in a chequered bandana) and the models /
blockstates of all the circuit's blocks.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image

from features import knuffeldal_npcs as npcs

REGENBOOG = [(255, 90, 110), (255, 160, 70), (255, 225, 80), (110, 220, 120), (90, 190, 255), (120, 120, 245), (200, 110, 240)]
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def _img(size=16):
    return Image.new("RGBA", (size, size))


def regenboogweg(k):
    """A glowing rainbow tile: a soft sheen towards the middle, a lighter rim, a little star now and then."""
    base = np.array(REGENBOOG[k], np.float32)
    rng = random.Random(900 + k)
    img = _img()
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            c = base * (0.86 + 0.02 * edge) + rng.uniform(-6, 6)
            if edge == 0:
                c = base * 0.75 + 255 * 0.25
            if (x + y) % 11 == 0 and 3 < x < 12:
                c = c * 0.7 + 255 * 0.3                         # a diagonal glint
            img.putpixel((x, y), tuple(int(max(0, min(255, v))) for v in c) + (255,))
    for sx, sy in ((4, 5), (11, 10)):
        if rng.random() < 0.8:
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                img.putpixel((sx + dx, sy + dy), (255, 255, 240, 255))
    return img


def kaasweg():
    """A cheese road: warm yellow, gatenkaas holes, a darker crust along the edges."""
    rng = random.Random(911)
    img = _img()
    holes = [(4, 4, 1.8), (11, 6, 1.3), (7, 11, 2.0), (13, 13, 1.0), (2, 12, 0.9)]
    for y in range(16):
        for x in range(16):
            c = np.array((236, 196, 70), np.float32) + rng.uniform(-10, 10)
            for hx, hy, r in holes:
                d = math.hypot(x - hx, y - hy)
                if d < r:
                    c = np.array((190, 145, 40), np.float32)
                elif d < r + 0.8:
                    c = c * 0.92
            img.putpixel((x, y), tuple(int(max(0, min(255, v))) for v in c) + (255,))
    return img


def kaassaus():
    """Glossy orange kaassaus: swirls and bright highlights (it's slippery!)."""
    rng = random.Random(913)
    img = _img()
    for y in range(16):
        for x in range(16):
            swirl = math.sin((x + y * 0.6) * 0.8) * 18 + math.cos(y * 0.9 - x * 0.3) * 12
            c = np.array((255, 160, 40), np.float32) + swirl + rng.uniform(-5, 5)
            if (x - 5) ** 2 + (y - 4) ** 2 < 3 or (x - 12) ** 2 + (y - 11) ** 2 < 2:
                c = np.array((255, 240, 190), np.float32)
            img.putpixel((x, y), tuple(int(max(0, min(255, v))) for v in c) + (255,))
    return img


def bergijs():
    """Pale blue ice with glitter and a few cracks."""
    rng = random.Random(917)
    img = _img()
    for y in range(16):
        for x in range(16):
            c = np.array((175, 215, 245), np.float32) + rng.uniform(-8, 8)
            if abs((x - y) - 3) < 1 and x > 5 or abs((x + y) - 20) < 1 and y > 8:
                c = np.array((140, 180, 225), np.float32)
            if rng.random() < 0.05:
                c = np.array((255, 255, 255), np.float32)
            img.putpixel((x, y), tuple(int(max(0, min(255, v))) for v in c) + (255,))
    return img


def boostring():
    """The boost ring's skin: a see-through rainbow swirl."""
    img = _img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            a = math.atan2(y - 7.5, x - 7.5)
            hue = ((a / (2 * math.pi)) + d / 14) % 1.0
            r, g, b = [int(v * 255) for v in _hsv(hue, 0.55, 1.0)]
            img.putpixel((x, y), (r, g, b, 150))
    return img


def _hsv(h, s, v):
    import colorsys
    return colorsys.hsv_to_rgb(h, s, v)


def paddenstoel_top():
    """A pink guh mushroom cap from above: white spots and a tiny guh face in the middle."""
    img = _img()
    rng = random.Random(921)
    for y in range(16):
        for x in range(16):
            c = np.array((245, 120, 175), np.float32) + rng.uniform(-8, 8)
            img.putpixel((x, y), tuple(int(max(0, min(255, v))) for v in c) + (255,))
    for sx, sy, r in ((3, 3, 1.3), (12, 3, 1.1), (3, 12, 1.0), (12, 12, 1.4)):
        for y in range(16):
            for x in range(16):
                if math.hypot(x - sx, y - sy) <= r:
                    img.putpixel((x, y), (255, 245, 250, 255))
    for x, y, c in ((6, 7, (40, 25, 45)), (9, 7, (40, 25, 45)), (6, 6, (255, 255, 255)), (9, 6, (255, 255, 255)),
                    (5, 9, (255, 150, 190)), (10, 9, (255, 150, 190)), (7, 9, (180, 60, 110)), (8, 9, (180, 60, 110))):
        img.putpixel((x, y), c + (255,))
    return img


def paddenstoel_side():
    img = _img()
    for y in range(16):
        for x in range(16):
            if y < 11:
                c = (245 - y * 3, 120, 175) if (x + y // 3) % 5 else (255, 240, 248)
            else:
                c = (250, 235, 215)
            img.putpixel((x, y), c + (255,))
    return img


def beker_icon():
    """The circuitbeker: a golden cup with two guh-ear handles and a pink ribbon."""
    rows = ["................", "..e..........e..", ".eE..........Ee.", ".eE.gggggggg.Ee.", "..eggGyyyyGgge..", "...gGyyyyyyGg...",
            "...gGyyWyyyGg...", "....gGyyyyGg....", ".....gGyyGg.....", "......gggg......", ".......gg.......", "......pppp......",
            ".....pPPPPp.....", ".....gggggg.....", "....gggggggg....", "................"]
    pal = {"g": (200, 150, 40, 255), "G": (240, 190, 60, 255), "y": (255, 222, 90, 255), "W": (255, 255, 235, 255),
           "e": (220, 160, 50, 255), "E": (255, 150, 190, 255), "p": (225, 80, 150, 255), "P": (255, 140, 190, 255)}
    img = _img()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img


def textures(h):
    for k in range(7):
        h.save(regenboogweg(k), "block", f"circuit_regenboogweg_{k}.png")
    h.save(kaasweg(), "block", "circuit_kaasweg.png")
    h.save(kaassaus(), "block", "circuit_kaassaus.png")
    h.save(bergijs(), "block", "circuit_bergijs.png")
    h.save(boostring(), "block", "circuit_boostring.png")
    h.save(paddenstoel_top(), "block", "circuit_stuiterpaddenstoel_top.png")
    h.save(paddenstoel_side(), "block", "circuit_stuiterpaddenstoel_side.png")
    h.save(beker_icon(), "item", "circuitbeker.png")
    h.item_model("circuitbeker")
    mika_textures(h)


# ======================================================================================================================
# models and blockstates
# ======================================================================================================================
def models(h):
    A = h.A
    w = h.w
    # the rainbow road: a model per colour, the slab too
    for k in range(7):
        w(f"{A}/models/block/circuit_regenboogweg_{k}.json", {"parent": "minecraft:block/cube_all",
                                                             "textures": {"all": f"guhs:block/circuit_regenboogweg_{k}"}})
        for kind, parent in (("", "minecraft:block/slab"), ("_top", "minecraft:block/slab_top")):
            t = f"guhs:block/circuit_regenboogweg_{k}"
            w(f"{A}/models/block/circuit_regenboogweg_plaat_{k}{kind}.json", {"parent": parent, "textures": {"bottom": t, "top": t, "side": t}})
    w(f"{A}/blockstates/circuit_regenboogweg.json", {"variants": {f"kleur={k}": {"model": f"guhs:block/circuit_regenboogweg_{k}"} for k in range(7)}})
    variants = {}
    for k in range(7):
        variants[f"kleur={k},type=bottom"] = {"model": f"guhs:block/circuit_regenboogweg_plaat_{k}"}
        variants[f"kleur={k},type=top"] = {"model": f"guhs:block/circuit_regenboogweg_plaat_{k}_top"}
        variants[f"kleur={k},type=double"] = {"model": f"guhs:block/circuit_regenboogweg_{k}"}
    w(f"{A}/blockstates/circuit_regenboogweg_plaat.json", {"variants": variants})
    w(f"{A}/models/item/circuit_regenboogweg.json", {"parent": "guhs:block/circuit_regenboogweg_0"})
    w(f"{A}/models/item/circuit_regenboogweg_plaat.json", {"parent": "guhs:block/circuit_regenboogweg_plaat_4"})
    # the cheese road and its slab
    h.simple_block("circuit_kaasweg")
    t = "guhs:block/circuit_kaasweg"
    w(f"{A}/models/block/circuit_kaasweg_plaat.json", {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
    w(f"{A}/models/block/circuit_kaasweg_plaat_top.json", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
    w(f"{A}/blockstates/circuit_kaasweg_plaat.json", {"variants": {"type=bottom": {"model": "guhs:block/circuit_kaasweg_plaat"},
                                                                  "type=top": {"model": "guhs:block/circuit_kaasweg_plaat_top"},
                                                                  "type=double": {"model": "guhs:block/circuit_kaasweg"}}})
    w(f"{A}/models/item/circuit_kaasweg_plaat.json", {"parent": "guhs:block/circuit_kaasweg_plaat"})
    h.simple_block("circuit_kaassaus")
    h.simple_block("circuit_bergijs")
    # the stuiterpaddenstoel
    w(f"{A}/models/block/circuit_stuiterpaddenstoel.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/circuit_stuiterpaddenstoel_top", "side": "guhs:block/circuit_stuiterpaddenstoel_side",
        "bottom": "guhs:block/circuit_stuiterpaddenstoel_side"}})
    w(f"{A}/blockstates/circuit_stuiterpaddenstoel.json", {"variants": {"": {"model": "guhs:block/circuit_stuiterpaddenstoel"}}})
    w(f"{A}/models/item/circuit_stuiterpaddenstoel.json", {"parent": "guhs:block/circuit_stuiterpaddenstoel"})
    # the boost ring skin: a thin see-through plane (axis x: the track runs along x through it)
    w(f"{A}/models/block/circuit_boostring.json", {"parent": "minecraft:block/block", "render_type": "translucent",
                                                   "textures": {"particle": "guhs:block/circuit_boostring", "skin": "guhs:block/circuit_boostring"},
                                                   "elements": [{"from": [7, 0, 0], "to": [9, 16, 16], "shade": False, "faces": {
                                                       f: {"texture": "#skin"} for f in ("north", "south", "east", "west", "up", "down")}}]})
    w(f"{A}/blockstates/circuit_boostring.json", {"variants": {"axis=x": {"model": "guhs:block/circuit_boostring"},
                                                               "axis=z": {"model": "guhs:block/circuit_boostring", "y": 90}}})
    w(f"{A}/models/item/circuit_boostring.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:block/circuit_boostring"}})
    # the invisible markers
    w(f"{A}/models/block/circuit_marker.json", {"textures": {"particle": "minecraft:block/pink_stained_glass"}})
    for name in ("circuit_mikaplek", "circuit_rolplek", "circuit_looping"):
        w(f"{A}/blockstates/{name}.json", {"variants": {f"facing={f},vanaf={v}": {"model": "guhs:block/circuit_marker"}
                                                        for f in ROT for v in range(3)}})
    # loot: the road blocks drop themselves
    for name in ("circuit_regenboogweg", "circuit_regenboogweg_plaat", "circuit_kaasweg", "circuit_kaasweg_plaat", "circuit_kaassaus",
                 "circuit_bergijs", "circuit_stuiterpaddenstoel"):
        h.self_drop(name)


# ======================================================================================================================
# the Mika-pikkers: the Mika in a chequered bandana (pikker: a bit lilac; pusher: a bit orange)
# ======================================================================================================================
MIKA_TOPS = [((46, 35), (16, 7)), ((116, 35), (11, 8)), ((63, 49), (14, 12))]


def mika_textures(h):
    src = Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")
    for name, hue, sat, band in (("circuit_mikapikker", 0.82, 0.8, ((40, 30, 50), (255, 255, 255))),
                                 ("circuit_mikaduwer", 0.06, 1.1, ((200, 70, 20), (255, 230, 120)))):
        img = h.recolour(src, hue=hue, sat=sat, val=1.0, only=h.pinkish).convert("RGBA")
        a = np.asarray(img).copy()
        for (u, v), (su, sv) in MIKA_TOPS:
            x0, y0, x1, y1 = int(u * 4), int(v * 4), int((u + su) * 4), int((v + sv) * 4)
            for y in range(y0, y1):
                for x in range(x0, x1):
                    if a[y, x, 3] == 0:
                        continue
                    a[y, x, :3] = band[((x - x0) // 8 + (y - y0) // 8) % 2]
        h.save(Image.fromarray(a), "entity", f"{name}.png")


# ======================================================================================================================
# Coach Vahoegvroem: the sitting guh in sky blue, a chequered cap, a headset with a microphone, a golden stopwatch
# ======================================================================================================================
def coach(h):
    geo_file = npcs._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_circuitguh"
    sw = npcs._swatches(geo, ["pet", "klep", "band", "kap", "micro", "goud", "wijzer"])
    c = npcs._cube
    geo["bones"].append({"name": "coach_pet", "parent": "head", "pivot": [0, 26, 0], "cubes": [
        c([-5.2, 25.6, -5.2], [10.4, 2.4, 9.4], sw["pet"]),
        c([-4.2, 28.0, -4.2], [8.4, 1.0, 7.4], sw["pet"]),
        c([-4.6, 25.4, -9.4], [9.2, 0.6, 4.4], sw["klep"]),
        c([-0.8, 29.0, -1.8], [1.6, 0.8, 1.6], sw["band"])]})
    geo["bones"].append({"name": "coach_headset", "parent": "head", "pivot": [0, 24, 0], "cubes": [
        c([-7.4, 17.0, -2.4], [1.2, 4.2, 4.2], sw["kap"]),
        c([6.2, 17.0, -2.4], [1.2, 4.2, 4.2], sw["kap"]),
        c([-7.0, 21.2, -0.6], [0.8, 4.6, 0.9], sw["band"]),
        c([6.2, 21.2, -0.6], [0.8, 4.6, 0.9], sw["band"]),
        c([-7.8, 18.2, -6.8], [0.6, 0.6, 4.6], sw["micro"]),
        c([-7.9, 17.7, -7.7], [1.2, 1.2, 1.2], sw["kap"])]})
    geo["bones"].append({"name": "coach_stopwatch", "parent": "body", "pivot": [0, 12, -4], "cubes": [
        c([-3.4, 11.3, -4.7], [6.8, 0.5, 0.5], sw["band"]),
        c([-3.4, 9.4, -4.7], [0.5, 1.9, 0.5], sw["band"]),
        c([2.9, 9.4, -4.7], [0.5, 1.9, 0.5], sw["band"]),
        c([-1.6, 6.6, -5.1], [3.2, 3.2, 0.8], sw["goud"]),
        c([-0.4, 9.8, -5.0], [0.8, 0.8, 0.6], sw["goud"]),
        c([-1.1, 7.1, -5.25], [2.2, 2.2, 0.2], sw["wijzer"])]})
    npcs._save_geo(h, "guh_npc_circuitguh.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.55, sat=0.65, val=1.02, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(2905)

    def chequer(block):
        for yy in range(32):
            for xx in range(32):
                block[yy, xx, :3] = (250, 250, 250) if ((xx // 8) + (yy // 8)) % 2 else (235, 80, 150)

    def watch(block):
        block[..., :3] = (250, 248, 238)
        for yy in range(32):
            for xx in range(32):
                if abs(xx - 16) < 2 and 6 < yy < 17 or abs(yy - 16) < 2 and 15 < xx < 25:
                    block[yy, xx, :3] = (40, 30, 50)
    npcs._paint(a, sw["pet"], (240, 240, 240), rng, 4, chequer)
    npcs._paint(a, sw["klep"], (235, 80, 150), rng, 6)
    npcs._paint(a, sw["band"], (60, 50, 70), rng, 6)
    npcs._paint(a, sw["kap"], (235, 80, 150), rng, 8)
    npcs._paint(a, sw["micro"], (50, 45, 60), rng, 4)
    npcs._paint(a, sw["goud"], (246, 200, 70), rng, 10)
    npcs._paint(a, sw["wijzer"], (250, 248, 238), rng, 2, watch)
    h.save(Image.fromarray(a), "entity", "npc_circuitguh.png")
