"""
Guh-sjoelen (2.9, De Grote Guhspelen; slice sjoelkatapult): het Sjoelhuisje in de Guhweides met Opoe Njegschuif.

  build(h)    the blocks of the sjoelbak (bakplank, kop, poort 2-3-4-1, vak) and the sjoelschijvenstapel, the puck entity
              textures, the sjoelschijfje coin and the loaned pucks, Opoe Njegschuif's model and texture, all texts
              (Dutch in both languages), the advancements (tab De Grote Guhspelen), the sjoelhuisje structure (with a
              geometry self-check, sjoelen_bouw.py)
  ftb(fq)     the sjoel quests (section "sjoelen" of guhs_minigames)
  BONES / clothes / CLOTHES / icons: Opoe's knitted outfit: het sjoelpetje (a flat puck-shaped cap), het sjoelvestje (a
              knitted cardigan with wooden buttons) and de sjoelbroche (a puck brooch on a ribbon)

Java side: nl.juiced.guhs.feature.sjoelen (SjoelGame, SjoelBak, SjoelSchijfEntity, ...).
"""
import math
import random

import numpy as np
from PIL import Image

from features import sjoelen_bouw as bouw
from features import sterrenwacht_hulp as hulp

NAME = "sjoelhuisje"
WOOD = (226, 200, 156)
DARK = (98, 64, 40)
PUCK = (196, 148, 96)
PINK = (242, 150, 192)

# ---------------------------------------------------------------------------------------------------------------------
# the outfit (Opoe's knitting)
# ---------------------------------------------------------------------------------------------------------------------
_H = [0, 6, -2]
_B = [0, 6, 6]
BONES = {
    # a flat round cap like a big sjoelschijf on the head, with a little knob
    "outfit_sjoelpetje": ("head", _H, "sjoelpetje", [([-4.8, 15, -10.6], [9.6, 1.6, 8.6], 0), ([-3.8, 15, -11.6], [7.6, 1.6, 10.6], 0),
                                                   ([-5.6, 15.1, -9.6], [11.2, 1.2, 6.6], 0)]),
    "outfit_sjoelpetje_knop": ("head", _H, "sjoelpetje_knop", [([-0.8, 16.6, -7.1], [1.6, 0.8, 1.6], 0)]),
    # the cardigan is the body suit (outfit_suit, knitted texture) plus three wooden buttons on the chest
    "outfit_sjoelvest_knoop": ("body", _B, "sjoelvest_knoop", [([-0.7, y, -3.2], [1.4, 1.2, 0.6], 0) for y in (3.2, 5.6, 8.0)]),
    # a ribbon round the neck with a round wooden puck brooch at the front
    "outfit_sjoelbroche_lint": ("head", _H, "sjoelbroche_lint", [([-4.6, 1.5, -11.2], [9.2, 1.0, 0.5], 0)]),
    "outfit_sjoelbroche": ("head", _H, "sjoelbroche", [([-1.5, -0.8, -11.7], [3.0, 3.0, 0.6], 0), ([-1.1, -0.4, -11.9], [2.2, 2.2, 0.3], 0)]),
}
CLOTHES = ["sjoelen_petje", "sjoelen_vestje", "sjoelen_broche"]


def _knit(rng, v, base, cable):
    """Cable knitting: rows of little V stitches and two twisted cables."""
    a = v.fabric(base, rng, 5)
    px = v.SWATCH * 4
    for y in range(px):
        for x in range(px):
            if (x + (y // 2) % 2) % 4 == 0:
                a[y, x] = tuple(max(0, c - 22) for c in base)
            cx = x % 16
            if cx in (5, 6, 9, 10):
                twist = (y // 4) % 2
                if (cx in (5, 6)) != bool(twist):
                    a[y, x] = cable
    return a


def _puck_face(rng, v, wood, face=True):
    a = v.fabric(wood, rng, 6)
    px = v.SWATCH * 4
    for y in range(px):
        for x in range(px):
            d = math.hypot(x - 15.5, y - 15.5)
            if 12 < d < 15:
                a[y, x] = tuple(max(0, c - 40) for c in wood)
    if face:
        for ex in (11, 20):
            a[12:16, ex:ex + 2] = (40, 28, 50)
            a[12, ex] = (255, 255, 255)
        a[18:20, 8:10] = PINK
        a[18:20, 22:24] = PINK
        for x, y in ((14, 19), (15, 20), (16, 20), (17, 19)):
            a[y, x] = (150, 72, 116)
    return a


def clothes(rng, v):
    return {
        "sjoelen_petje": {"sjoelpetje": lambda: _puck_face(rng, v, (206, 160, 104), face=False),
                          "sjoelpetje_knop": lambda: v.fabric(PINK, rng, 8)},
        "sjoelen_vestje": {"suit": lambda: _knit(rng, v, (232, 190, 96), (250, 226, 150)),
                           "sjoelvest_knoop": lambda: v.fabric((150, 98, 58), rng, 10)},
        "sjoelen_broche": {"sjoelbroche": lambda: _puck_face(rng, v, PUCK),
                           "sjoelbroche_lint": lambda: v.stripes((238, 120, 170), (250, 200, 222), rng, 3)},
    }


PETJE_ICON = ["................", "................", "................", "................", "................",
              "......cc........", "...aaaaaaaaa....", "..abbbbbbbbba...", ".abbwbbbbbwbba..", ".abbbbbbbbbbba..",
              "..aabbbbbbbaa...", "....aaaaaaa.....", "................", "................", "................", "................"]
BROCHE_ICON = ["................", "................", "..llllllllllll..", "..l..........l..", "...l........l...", "....l......l....",
               ".....aaaaaa.....", "....abbbbbba....", "...abkbbbbkba...", "...abbbbbbbba...", "...abpbbbbpba...",
               "...abbbmmbbba...", "....abbbbbba....", ".....aaaaaa.....", "................", "................"]


def icons(ic):
    return {
        "sjoelen_petje": ic.icon(PETJE_ICON, {"a": (120, 80, 45), "b": (206, 160, 104), "w": (240, 222, 190), "c": PINK}),
        "sjoelen_vestje": ic.shirt((232, 190, 96), (150, 110, 50), (150, 98, 58), pattern="buttons"),
        "sjoelen_broche": ic.icon(BROCHE_ICON, {"l": (238, 120, 170), "a": (120, 80, 45), "b": PUCK, "k": (40, 28, 50),
                                                "p": PINK, "m": (150, 72, 116)}),
    }


# ---------------------------------------------------------------------------------------------------------------------
# textures
# ---------------------------------------------------------------------------------------------------------------------
def _grain(base, seed, size=16, var=6):
    """Wood with its grain running up the texture (along the sjoelbak)."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    cols = [rng.randint(-var, var) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            v = cols[x] + rng.randint(-3, 3)
            if rng.random() < 0.015:
                v -= 14
            img.putpixel((x, y), tuple(max(0, min(255, b + v)) for b in base) + (255,))
    return img


DIGITS = {"1": ["010", "110", "010", "010", "111"], "2": ["111", "001", "111", "100", "111"], "3": ["111", "001", "111", "001", "111"],
          "4": ["101", "101", "111", "001", "001"]}


def textures(h):
    plank = _grain(WOOD, 2901)
    px = plank.load()
    for x in range(16):                                      # a waxed shine
        px[x, 4] = tuple(min(255, c + 14) for c in px[x, 4][:3]) + (255,)
    h.save(plank, "block", "sjoelen_bakplank.png")
    h.save(_grain(DARK, 2902), "block", "sjoelen_rand.png")
    # the head: the planks with a dark start line where you stand (south = the texture's bottom), an arrow in the middle
    kop = plank.copy()
    k = kop.load()
    for x in range(16):
        for y in (12, 13):
            k[x, y] = DARK + (255,)
    h.save(kop, "block", "sjoelen_kop_top.png")
    pijl = kop.copy()
    p = pijl.load()
    for y in range(3, 11):
        p[7, y] = (238, 110, 160, 255)
        p[8, y] = (238, 110, 160, 255)
    for i in range(3):
        for x in (6 - i, 9 + i):
            p[x, 4 + i] = (238, 110, 160, 255)
    h.save(pijl, "block", "sjoelen_kop_pijl.png")
    # the front of the gate bar: 80 px wide over the five blocks, the numbers 2 3 4 1 over the openings
    strip = Image.new("RGBA", (80, 16))
    s = strip.load()
    rng = random.Random(2903)
    for x in range(80):
        for y in range(16):
            s[x, y] = tuple(max(0, min(255, c + rng.randint(-5, 5))) for c in DARK) + (255,)
    colours = {"2": (250, 220, 90), "3": (140, 220, 170), "4": (250, 150, 200), "1": (170, 200, 250)}
    for centre, digit in zip((10, 30, 50, 70), "2341"):
        for row, line in enumerate(DIGITS[digit]):
            for i, ch in enumerate(line):
                if ch == "1":
                    s[centre - 1 + i, 5 + row] = colours[digit] + (255,)
    for x in range(80):
        s[x, 4] = (140, 96, 60, 255)
    for d in range(5):
        h.save(strip.crop((16 * d, 0, 16 * d + 16, 16)), "block", f"sjoelen_poort_voor_{d}.png")
    # the side of a stack of pucks and its top (a guh face)
    side = Image.new("RGBA", (16, 16))
    sp = side.load()
    for y in range(16):
        for x in range(16):
            band = y % 4 == 3
            c = tuple(max(0, v - 36) for v in PUCK) if band else PUCK
            if (y // 4) == 1 and not band:
                c = PINK
            sp[x, y] = tuple(max(0, min(255, v + rng.randint(-5, 5))) for v in c) + (255,)
    h.save(side, "block", "sjoelen_stapel_zij.png")
    top = Image.new("RGBA", (16, 16), PUCK + (255,))
    hulp.paint_face(top, (0, 0, 16, 16), 0)
    h.save(top, "block", "sjoelen_stapel_top.png")
    # the pucks (entity texture 32x16: box a 7x2x5 at (0,0), box b 5x1.8x7 at (0,8)); the pink one is the 20th
    for name, wood in (("sjoelschijf", PUCK), ("sjoelschijf_roze", PINK)):
        img = Image.new("RGBA", (32, 16))
        q = img.load()
        for x in range(32):
            for y in range(16):
                q[x, y] = tuple(max(0, min(255, c + rng.randint(-6, 6))) for c in wood) + (255,)
        edge = tuple(max(0, c - 40) for c in wood)
        for x in range(24):                                # the sides: a darker edge
            for y in (5, 6, 15):
                q[x, y] = edge + (255,)
        for (x, y) in ((6, 1), (10, 1)):                   # a tiny guh face on the top of box a
            q[x, y] = (40, 28, 50, 255)
            q[x, y + 1] = (40, 28, 50, 255)
        for (x, y) in ((5, 3), (11, 3)):
            q[x, y] = (255, 150, 188, 255)
        q[8, 3] = (150, 72, 116, 255)
        h.save(img, "entity", f"{name}.png")
    # items: the sjoelschijfje (a wooden puck coin with a guh face) and the loaned stack of pucks
    h.save(h.grid(["................", "......dddd......", "....ddwwwwdd....", "...dwwwwwwwwd...", "..dwwkwwwwkwwd..",
                   "..dwwkwwwwkwwd..", ".dwwwwwwwwwwwwd.", ".dwpwwwwwwwwpwd.", ".dwwwwmmmmwwwwd.", "..dwwwwwwwwwwd..",
                   "..dwwwwwwwwwwd..", "...dwwwwwwwwd...", "....ddwwwwdd....", "......dddd......", "................",
                   "................"], {"d": (120, 80, 45, 255), "w": PUCK + (255,), "k": (40, 28, 50, 255), "p": PINK + (255,),
                                          "m": (150, 72, 116, 255)}), "item", "sjoelschijfje.png")
    h.save(h.grid(["................", "................", "....aaaaaaaa....", "...awwwwwwwwa...", "...aaaaaaaaaa...",
                   "....aaaaaaaa....", "...awwwwwwwwa...", "...aaaaaaaaaa...", "....aaaaaaaa....", "...apppppppa....",
                   "...aaaaaaaaaa...", "....aaaaaaaa....", "...awwwwwwwwa...", "...aaaaaaaaaa...", "................",
                   "................"], {"a": (120, 80, 45, 255), "w": PUCK + (255,), "p": PINK + (255,)}), "item", "sjoelen_schijven.png")


# ---------------------------------------------------------------------------------------------------------------------
# block models
# ---------------------------------------------------------------------------------------------------------------------
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
DIVIDERS_PX = [(0, 4), (16, 24), (36, 44), (56, 64), (76, 80)]


def _clip(d):
    out = []
    for a, b in DIVIDERS_PX:
        lo, hi = max(a, 16 * d), min(b, 16 * d + 16)
        if lo < hi:
            out.append((lo - 16 * d, hi - 16 * d))
    return out


def _cube(frm, to, tex, faces=("down", "up", "north", "south", "west", "east")):
    return {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in faces}}


def models(h):
    A = h.A
    h.simple_block("sjoelen_bakplank")
    h.self_drop("sjoelen_bakplank")
    h.shaped("sjoelen_bakplank", ["SB", "BS"], {"S": "minecraft:birch_slab", "B": "minecraft:honeycomb"}, "guhs:sjoelen_bakplank", 4)
    # the head
    for name, tex in (("sjoelen_kop", "sjoelen_kop_top"), ("sjoelen_kop_pijl", "sjoelen_kop_pijl")):
        h.w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"guhs:block/{tex}", "side": "guhs:block/sjoelen_rand", "bottom": "guhs:block/sjoelen_bakplank"}})
    h.w(f"{A}/blockstates/sjoelen_kop.json", {"variants": {
        f"deel={d},facing={f}": {"model": "guhs:block/" + ("sjoelen_kop_pijl" if d == 2 else "sjoelen_kop"), **({"y": r} if r else {})}
        for f, r in ROT.items() for d in range(5)}})
    h.w(f"{A}/models/item/sjoelen_kop.json", {"parent": "guhs:block/sjoelen_kop"})
    # the gate bar and the lanes: dividers above the surface, drawn over the five blocks
    for d in range(5):
        def base():
            b = _cube([0, 0, 0], [16, 16, 16], "#rand")
            b["faces"]["up"]["texture"] = "#plank"
            return b
        els = [base()]
        for a, b in _clip(d):
            els.append(_cube([a, 16, 0], [b, 26, 16], "#rand"))
        bridge = _cube([0, 20, 10], [16, 26, 16], "#rand")
        bridge["faces"]["south"] = {"texture": "#voor", "uv": [0, 5, 16, 11]}
        els.append(bridge)
        h.w(f"{A}/models/block/sjoelen_poort_{d}.json", {"parent": "minecraft:block/block", "textures": {
            "particle": "guhs:block/sjoelen_rand", "plank": "guhs:block/sjoelen_bakplank", "rand": "guhs:block/sjoelen_rand",
            "voor": f"guhs:block/sjoelen_poort_voor_{d}"}, "elements": els})
        els = [base()]
        for a, b in _clip(d):
            els.append(_cube([a, 16, 0], [b, 20, 16], "#rand"))
        h.w(f"{A}/models/block/sjoelen_vak_{d}.json", {"parent": "minecraft:block/block", "textures": {
            "particle": "guhs:block/sjoelen_rand", "plank": "guhs:block/sjoelen_bakplank", "rand": "guhs:block/sjoelen_rand"}, "elements": els})
    for name in ("sjoelen_poort", "sjoelen_vak"):
        h.w(f"{A}/blockstates/{name}.json", {"variants": {
            f"deel={d},facing={f}": {"model": f"guhs:block/{name}_{d}", **({"y": r} if r else {})} for f, r in ROT.items() for d in range(5)}})
        h.w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}_2"})
    # a stack of four pucks (each two crossed boxes), a little crooked
    els = []
    for i, (dx, dz) in enumerate(((0, 0), (0.6, -0.3), (-0.4, 0.4), (0.3, 0.2))):
        y0 = i * 2.6
        side = {"texture": "#zij", "uv": [0, 4 * i, 9, 4 * i + 3]}
        for frm, to in (([3.5, y0, 5], [12.5, y0 + 2.4, 11]), ([5, y0, 3.5], [11, y0 + 2.4, 12.5])):
            els.append({"from": [frm[0] + dx, frm[1], frm[2] + dz], "to": [to[0] + dx, to[1], to[2] + dz], "faces": {
                "north": dict(side), "south": dict(side), "east": dict(side), "west": dict(side),
                "up": {"texture": "#top"}, "down": {"texture": "#top"}}})
    h.w(f"{A}/models/block/sjoelen_stapel.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/sjoelen_stapel_zij", "zij": "guhs:block/sjoelen_stapel_zij", "top": "guhs:block/sjoelen_stapel_top"},
        "elements": els})
    h.w(f"{A}/blockstates/sjoelen_stapel.json", {"variants": {f"facing={f}": {"model": "guhs:block/sjoelen_stapel", **({"y": r} if r else {})}
                                                             for f, r in ROT.items()}})
    h.w(f"{A}/models/item/sjoelen_stapel.json", {"parent": "guhs:block/sjoelen_stapel"})
    h.self_drop("sjoelen_stapel")
    h.shaped("sjoelen_stapel", ["S", "S", "S"], {"S": "guhs:sjoelschijfje"}, "guhs:sjoelen_stapel", 1)
    h.item_model("sjoelschijfje")
    h.item_model("sjoelen_schijven")
    h.add_tag("guhs/tags/item/loaned", ["guhs:sjoelen_schijven"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:sjoelen_bakplank", "guhs:sjoelen_stapel"])


# ---------------------------------------------------------------------------------------------------------------------
# Opoe Njegschuif: a soft lilac grandma guh with round golden glasses, a white bun with two knitting needles, a knitted
# pink shawl, and a string of pearls
# ---------------------------------------------------------------------------------------------------------------------
def npc(h):
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_sjoelguh")
    sw = hulp.swatches(geo, ["bril", "haar", "naald", "sjaal", "parel"])
    c = hulp.cube
    geo["bones"].append({"name": "opoe_bril", "parent": "head", "pivot": [0, 20, -7], "cubes": [
        c([1.0, 18.2, -7.7], [4.6, 3.8, 0.5], sw["bril"]), c([-5.6, 18.2, -7.7], [4.6, 3.8, 0.5], sw["bril"]),
        c([-1.0, 20.1, -7.7], [2.0, 0.5, 0.5], sw["naald"]), c([5.5, 20.1, -7.5], [2.2, 0.4, 0.4], sw["naald"]),
        c([-7.7, 20.1, -7.5], [2.2, 0.4, 0.4], sw["naald"])]})
    geo["bones"].append({"name": "opoe_knot", "parent": "head", "pivot": [0, 26, 0], "cubes": [
        c([-3.0, 25.6, -2.2], [6.0, 3.0, 5.0], sw["haar"]), c([-2.2, 28.4, -1.6], [4.4, 1.6, 3.8], sw["haar"]),
        c([-5.0, 27.4, 0.0], [10.0, 0.5, 0.5], sw["naald"], rotation=[0, 0, 18], pivot=[0, 27.6, 0.2]),
        c([-5.0, 27.4, -0.8], [10.0, 0.5, 0.5], sw["naald"], rotation=[0, 0, -22], pivot=[0, 27.6, -0.6])]})
    geo["bones"].append({"name": "opoe_sjaal", "parent": "body", "pivot": [0, 12, 0], "cubes": [
        c([-5.6, 10.4, -4.4], [11.2, 2.4, 8.6], sw["sjaal"], inflate=0.1), c([-5.0, 6.0, 3.6], [10.0, 4.6, 0.8], sw["sjaal"]),
        c([-4.4, 7.2, -4.9], [2.2, 3.4, 0.6], sw["sjaal"]), c([2.2, 7.2, -4.9], [2.2, 3.4, 0.6], sw["sjaal"])]})
    geo["bones"].append({"name": "opoe_parels", "parent": "body", "pivot": [0, 12, -4], "cubes": [
        c([-3.2 + 1.3 * i, 9.2 - (0.8 if 1 <= i <= 4 else 0), -5.0], [0.9, 0.9, 0.9], sw["parel"]) for i in range(6)]})
    hulp.save_geo(h, "guh_npc_sjoelguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.80, sat=0.42, val=1.03)
    rng = np.random.default_rng(29102)

    def bril(block):
        block[..., :3] = (236, 196, 90)
        block[5:27, 5:27, :3] = (206, 232, 246)
        block[8:12, 9:13, :3] = (255, 255, 255)
    hulp.paint_swatch(a, sw["bril"], (236, 196, 90), rng, 4, bril)
    hulp.paint_swatch(a, sw["haar"], (244, 240, 246), rng, 10)
    hulp.paint_swatch(a, sw["naald"], (238, 110, 160), rng, 6)

    def gebreid(block):
        for y in range(32):
            for x in range(32):
                if (x + (y // 2) % 2) % 4 == 0:
                    block[y, x, :3] = (200, 110, 150)
    hulp.paint_swatch(a, sw["sjaal"], (236, 140, 180), rng, 6, gebreid)
    hulp.paint_swatch(a, sw["parel"], (250, 246, 240), rng, 4)
    h.save(Image.fromarray(a), "entity", "npc_sjoelguh.png")


# ---------------------------------------------------------------------------------------------------------------------
# advancements (tab De Grote Guhspelen) and texts
# ---------------------------------------------------------------------------------------------------------------------
ADVANCEMENTS = [  # name, parent, icon, frame, criteria, title, description
    ("sjoelen_gevonden", "root", "guhs:sjoelen_stapel", "task",
     {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": "guhs:sjoelhuisje"}}}}},
     "Komt dat zien!", "Vind het Sjoelhuisje in de Guhweides: het huisje met een reuzensjoelbak als dak"),
    ("sjoelen_gespeeld", "sjoelen_gevonden", "guhs:sjoelschijfje", "task", {"done": {"trigger": "minecraft:impossible"}},
     "Twintig schijfjes", "Sjoel een hele beurt van twintig schijfjes bij Opoe Njegschuif"),
    ("sjoelen_zestig", "sjoelen_gespeeld", "guhs:sjoelen_bakplank", "goal", {"done": {"trigger": "minecraft:impossible"}},
     "Opoe is trots", "Sjoel 60 punten of meer in één beurt"),
    ("sjoelen_honderd", "sjoelen_zestig", "guhs:sjoelen_stapel", "challenge", {"done": {"trigger": "minecraft:impossible"}},
     "Honderd! VAHOEG!", "Sjoel de volle 100 punten: vijf schijfjes in elk poortje"),
    ("sjoelen_kleding", "sjoelen_gespeeld", "guhs:sjoelen_vestje", "goal",
     {"items": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
         {"items": "guhs:sjoelen_petje"}, {"items": "guhs:sjoelen_vestje"}, {"items": "guhs:sjoelen_broche"}]}},
      "code": {"trigger": "minecraft:impossible"}},
     "Gebreid door Opoe", "Verzamel het hele sjoelpakje: het sjoelpetje, het sjoelvestje en de sjoelbroche"),
]


def advancements(h):
    for name, parent, icon, frame, crit, title, desc in ADVANCEMENTS:
        adv = {"parent": f"guhs:grote_guhspelen/{parent}",
               "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.grote_guhspelen.{name}.title"},
                           "description": {"translate": f"advancements.guhs.grote_guhspelen.{name}.description"},
                           "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
               "criteria": crit}
        if len(crit) > 1:
            adv["requirements"] = [list(crit)]
        h.w(f"{h.D}/advancement/grote_guhspelen/{name}.json", adv)
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.title", title, title)
        h.lang(f"advancements.guhs.grote_guhspelen.{name}.description", desc, desc)
    hulp.quest_advancements(h, ["sjoelen_gespeeld", "sjoelen_zestig", "sjoelen_honderd", "sjoelen_kleding", "seen_sjoelguh"])


TEXTS = {
    "block.guhs.sjoelen_bakplank": "Sjoelbakplank",
    "block.guhs.sjoelen_kop": "Kop van de sjoelbak",
    "block.guhs.sjoelen_poort": "Sjoelpoortjes (2-3-4-1)",
    "block.guhs.sjoelen_vak": "Sjoelvakjes",
    "block.guhs.sjoelen_stapel": "Stapeltje sjoelschijven",
    "item.guhs.sjoelschijfje": "Sjoelschijfje",
    "item.guhs.sjoelen_schijven": "Sjoelschijven (geleend)",
    "item.guhs.sjoelen_schijven.lore": "Sta aan de kop van de sjoelbak, houd rechtsklik ingedrukt (de krachtbalk gaat op en neer), kijk waar hij heen moet en laat los. Schuiven maar!",
    "item.guhs.sjoelen_schijven.loan": "Van Opoe Njegschuif geleend: na je beurt gaan ze terug in haar doosje.",
    "item.guhs.sjoelen_petje": "Sjoelpetje",
    "item.guhs.sjoelen_vestje": "Sjoelvestje",
    "item.guhs.sjoelen_broche": "Sjoelbroche",
    "entity.guhs.sjoelschijf": "Sjoelschijf",
    "entity.guhs.guh_npc.sjoelguh": "Opoe Njegschuif",
    "gui.guhs.guhdex.rarity.sjoelguh": "Zeldzaamheid: uniek (in het Sjoelhuisje in de Guhweides)",
    "gui.guhs.guhdex.info.sjoelguh": "Opoe Njegschuif sjoelt al sinds ze een klein guhtje was. Ze breit haar eigen vestjes, schenkt thee voor iedereen die komt kijken en weet precies hoe hard een schijfje moet: \"Niet te zacht, niet te hard, gewoon vahoeg, liefje!\" Ze wordt nooit boos, ook niet als je alle schijfjes in poortje 1 schuift. Njeg!",
    "structure.guhs.sjoelhuisje": "Het Sjoelhuisje",
    "structure.guhs.sjoelhuisje.tooltip": "Minigame: guh-sjoelen bij Opoe Njegschuif, sjoelschijfjes en een gebreid sjoelpakje (Guhweides)",
    # Opoe talks
    "quest.guhs.sjoelen.hello1": "Ha, dag liefje! Kom je sjoelen? Twintig schijfjes, vier poortjes, en een kopje thee na afloop. VAHOEG!",
    "quest.guhs.sjoelen.hello2": "Njeg njeg, daar is een nieuw guhtje! Weet je hoe het moet? In elk poortje één schijfje: dat is een setje, twintig punten!",
    "quest.guhs.sjoelen.hello3": "Ik heb vanochtend de bak weer in de was gezet. Zo glad als een kaasknabbel in de boter! Probeer maar.",
    "quest.guhs.sjoelen.hello4": "Vroeger sjoelde ik met mijn opa tot diep in de nacht. Hij schoof alles in poortje 1... maar ik hield toch van hem. Speel je een potje?",
    "quest.guhs.sjoelen.playing": "Je bent aan het sjoelen, liefje! Ga maar lekker aan de kop van de bak staan. Wil je stoppen? Dat mag ook.",
    "quest.guhs.sjoelen.busy": "Even geduld, liefje: %s is nu aan het sjoelen. Kijk maar mee vanaf het bankje!",
    "quest.guhs.sjoelen.elsewhere": "Je bent al ergens anders aan het sjoelen!",
    "quest.guhs.sjoelen.broken": "Njeg... waar is mijn sjoelbak gebleven? Ik kan de kop niet meer vinden.",
    "quest.guhs.sjoelen.full": "Je zakken zitten propvol! Maak één plekje vrij voor de schijfjes, dan leen ik ze je.",
    "quest.guhs.sjoelen.start": "Hier zijn je twintig schijfjes. Ga aan de kop van de bak staan... en klaar? 3... 2... 1...",
    "quest.guhs.sjoelen.how": "Houd rechtsklik ingedrukt met de schijfjes: de krachtbalk gaat op en neer. Tussen Opoe's twee streepjes is het goed. Kijk waar hij heen moet en laat los. Loop opzij om van links of rechts te schuiven. Poortjes: 2-3-4-1. Elk setje (één in elk poortje) = 20 punten!",
    "quest.guhs.sjoelen.stopped": "Je stopt met sjoelen. De schijfjes gaan terug in Opoe's doosje. Tot gauw, liefje!",
    "quest.guhs.sjoelen.walked_away": "Je liep weg van de sjoelbak. Opoe Njegschuif pakt haar schijfjes weer in.",
    "quest.guhs.sjoelen.idle": "Njeg, ben je in slaap gevallen? Opoe ruimt de schijfjes maar weer op.",
    "quest.guhs.sjoelen.count": "=== Opoe telt de schijfjes ===",
    "quest.guhs.sjoelen.count_gate": "Poortje %s: %s schijfjes",
    "quest.guhs.sjoelen.total": "%s setjes (%s punten) + de rest (%s punten) = %s punten",
    "quest.guhs.sjoelen.record": "Nieuw record: %s punten! (was %s)",
    "quest.guhs.sjoelen.first_record": "Je eerste beurt: %s punten. Dat is meteen je record!",
    "quest.guhs.sjoelen.no_record": "Je record blijft %s punten. Nog een potje?",
    "quest.guhs.sjoelen.munten": "+%s sjoelschijfjes (1 voor je beurt, 1 per setje, 1 voor een nieuw record)",
    "quest.guhs.sjoelen.first": "Je allereerste beurt! Een cadeautje van Opoe: kaasknabbels en een stapeltje sjoelschijven voor thuis. VAHOEG!",
    "quest.guhs.sjoelen.huis_record": "HUISRECORD! Jouw naam zweeft nu bovenaan in het Sjoelhuisje. Opoe breit er een sjaaltje voor!",
    "quest.guhs.sjoelen.opoe_100": "HONDERD! Vijf in elk poortje! Ik heb in honderd jaar nog nooit zoiets vahoegs gezien. Kom hier, dan krijg je een knuffel!",
    "quest.guhs.sjoelen.opoe_goed": "Knap gedaan, liefje! Jij kunt echt sjoelen. Opoe is trots op je.",
    "quest.guhs.sjoelen.opoe_aardig": "Dat ging al heel aardig! Probeer elk poortje evenveel te geven, dan krijg je meer setjes.",
    "quest.guhs.sjoelen.opoe_oefenen": "Njeg, geeft niks hoor! Oefening baart kunst. Zachtjes schuiven, tussen de twee streepjes.",
    # on screen
    "gui.guhs.sjoelen.countdown_sub": "Ga aan de kop van de sjoelbak staan",
    "gui.guhs.sjoelen.go": "SCHUIVEN!",
    "gui.guhs.sjoelen.go_sub": "Twintig schijfjes, vier poortjes",
    "gui.guhs.sjoelen.bar": "Schijfjes over: %s | 2: %s  3: %s  4: %s  1: %s | %s punten",
    "gui.guhs.sjoelen.look": "Kijk naar de poortjes aan het eind van de bak!",
    "gui.guhs.sjoelen.stand": "Ga aan de kop van de sjoelbak staan om te schuiven",
    "gui.guhs.sjoelen.wait_countdown": "Nog even wachten... 3, 2, 1!",
    "gui.guhs.sjoelen.all_gone": "Alle schijfjes zijn geschoven! Opoe telt zo.",
    "gui.guhs.sjoelen.wait_puck": "Wacht even tot je vorige schijfje weg is!",
    "gui.guhs.sjoelen.good": "Mooi zo! Precies tussen Opoe's streepjes.",
    "gui.guhs.sjoelen.power": "Kracht: %s%%",
    "gui.guhs.sjoelen.done": "%s punten",
    "gui.guhs.sjoelen.done_good": "%s punten! Knap!",
    "gui.guhs.sjoelen.done_100": "%s PUNTEN! VAHOEG!",
    "gui.guhs.sjoelen.done_sub": "%s setjes, +%s sjoelschijfjes",
    "gui.guhs.scorebord.sjoelen": "Sjoelhuisje top 3",
    "gui.guhs.sjoelen.scorebord_heading": "20 schijfjes, hoogstens 100",
    "gui.guhs.sjoelen.no_build": "Njeg! Het Sjoelhuisje is van Opoe: hier mag je niks slopen of bouwen.",
    "gui.guhs.sjoelen.question": "Een beurtje sjoelen? Twintig schijfjes over de grote sjoelbak, in de poortjes 2-3-4-1. Hoe meer setjes, hoe meer sjoelschijfjes!",
    "gui.guhs.sjoelen.mine": "Je bent aan het sjoelen (nog %s schijfjes). Stoppen mag altijd.",
    "gui.guhs.sjoelen.busy": "%s is aan het sjoelen. Kijk mee vanaf het bankje of wacht even!",
    "gui.guhs.sjoelen.play": "Sjoelen! (20 schijfjes)",
    "gui.guhs.sjoelen.play.tooltip": "Je krijgt twintig schijfjes en gaat aan de kop van de bak staan",
    "gui.guhs.sjoelen.stop": "Stoppen",
    "gui.guhs.sjoelen.stop.tooltip": "Je beurt stopt; wat al in de poortjes ligt telt niet meer",
    "gui.guhs.sjoelen.shop": "Winkeltje",
    "gui.guhs.sjoelen.shop.tooltip": "Opoe's gebreide sjoelpakje voor je guh, voor sjoelschijfjes",
    "gui.guhs.sjoelen.rules": "Elk setje (één in 2, 3, 4 én 1) is 20 punten. Wat over is telt per poortje: 2, 3, 4 of 1. Hoogstens 100!",
    "gui.guhs.sjoelen.records": "Jouw records:",
    "gui.guhs.sjoelen.best": "Beste beurt: %s punten   Beurten: %s",
    "gui.guhs.sjoelen.huis": "Huisrecord: %s met %s punten",
    "gui.guhs.sjoelen.huis_none": "Huisrecord: nog niemand!",
}


def texts(h):
    for key, text in TEXTS.items():
        h.lang(key, text, text)


# ---------------------------------------------------------------------------------------------------------------------
def build(h):
    textures(h)
    models(h)
    npc(h)
    advancements(h)
    texts(h)
    s, info = bouw.build(h)
    n = bouw.check(s)
    h.TEMPLATE_SIZES[NAME] = 48
    h.FLATNESS[NAME] = 22                      # (the hall sits on a 4-deep foundation; beard_box does the rest)
    none = {"bounding_box": "piece", "spawns": []}
    h.structure(NAME, ["guh_meadows"], spacing=32, separation=11, salt=20290101, start_y=-bouw.G, reach=64, centre=bouw.ANCHOR,
                spawn_overrides={"monster": none, "ambient": none})
    s.save(NAME)
    selfcheck(h)
    print(f"sjoelen: sjoelhuisje geometry check ok ({n} walkable spots, {info['gezichten']} guh faces)")


def selfcheck(h):
    import os
    missing = [f for f in ("blockstates/sjoelen_poort.json", "models/block/sjoelen_vak_4.json", "geo/entity/guh_npc_sjoelguh.geo.json",
                           "textures/entity/sjoelschijf.png", "textures/entity/npc_sjoelguh.png", "textures/item/sjoelschijfje.png")
               if not os.path.exists(f"{h.A}/{f}")]
    for key in ("quest.guhs.sjoelen.hello4", "gui.guhs.sjoelen.bar", "entity.guhs.guh_npc.sjoelguh"):
        if key not in h.NL:
            missing.append(key)
    if missing:
        raise SystemExit(f"sjoelen assets missing: {missing}")


# ---------------------------------------------------------------------------------------------------------------------
def ftb(fq):
    q = fq.q
    q("sjoelen_huisje", "Het Sjoelhuisje", "In de &dGuhweides&r staat soms een houten huisje met een reuzensjoelbak als dak en een guhgezicht "
      "op de gevel: het &6Sjoelhuisje&r (superkompas: Minigames). Binnen woont &6Opoe Njegschuif&r.", "guhs:sjoelen_stapel",
      [fq.structure(NAME)], rewards=(("guhs:kaas_knabbels", 8),), x=-8, y=0, shape="circle", xp=100)
    q("sjoelen_beurt", "Twintig schijfjes", "Praat met Opoe Njegschuif en sjoel een hele beurt. Houd rechtsklik ingedrukt: de krachtbalk "
      "gaat op en neer, tussen Opoe's twee streepjes is het goed. Kijk waar het schijfje heen moet en laat los!", "guhs:sjoelschijfje",
      [fq.adv("sjoelen_gespeeld")], rewards=(("guhs:sjoelschijfje", 2),), x=-6.5, y=0, xp=150)
    q("sjoelen_zestig", "Opoe is trots", "Sjoel 60 punten of meer. Tip: probeer in elk poortje evenveel schijfjes te krijgen, want elk "
      "setje (2-3-4-1) is twintig punten!", "guhs:sjoelen_bakplank", [fq.adv("sjoelen_zestig")], rewards=(("guhs:sjoelschijfje", 3),),
      x=-5, y=0, xp=200)
    q("sjoelen_honderd", "Honderd! VAHOEG!", "De volle 100 punten: vijf schijfjes in elk poortje. Opoe Njegschuif heeft dat maar één keer "
      "eerder gezien...", "guhs:sjoelen_stapel", [fq.adv("sjoelen_honderd")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),),
      x=-3.5, y=0, shape="hexagon", xp=400)
    q("sjoelen_petje", "Petje op!", "Koop het sjoelpetje bij Opoe Njegschuif (4 sjoelschijfjes): een plat petje, net een sjoelschijf.",
      "guhs:sjoelen_petje", [fq.item("guhs:sjoelen_petje")], x=-2, y=0, xp=100)
    q("sjoelen_pakje", "Gebreid door Opoe", "Verzamel het hele sjoelpakje: het sjoelpetje, het warme sjoelvestje en de sjoelbroche.",
      "guhs:sjoelen_vestje", [fq.adv("sjoelen_kleding")], rewards=(("guhs:sjoelschijfje", 3),), x=-0.5, y=0, shape="gear", xp=300)
    q("sjoelen_opoe", "Een kopje thee bij Opoe", "Zet Opoe Njegschuif in je Guhdex (kom dichtbij genoeg).", "guhs:guhdex",
      [fq.adv("seen_sjoelguh")], rewards=(("guhs:kaas_knabbels", 8),), x=1, y=0, shape="rsquare", xp=100)
