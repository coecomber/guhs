"""
De Guhvilla (1.8.0): the fourth, super big Guhhuisje next to klein, medium and groot (tools/features/huisje.py).

  - guhhuisje_villa: 4x4 blocks and five tall, twenty residents (Java: HuisjeMaat.VILLA). The same guh head as the other
    huisjes, but a villa: two storeys (the big eye windows upstairs, little windows and the snoet door downstairs), a porch
    with two pillars and a balcony with flower boxes, steps, two chimneys, little side windows, and on the roof the big ears
    with a bow and a golden crown. The body is guhhuisje_villa_model (32 model pixels high), the roof a second model
    guhhuisje_villa_dak_model that HuisjeRenderer draws 32 model pixels higher (a block model can't reach past 32);
  - the recipe (a groot huisje, two guhkristallen, a Knuffelnest, two blocks of kaasknabbels, pink wool and the
    Timmerguh's bouwboekje), loot, the axe tag, the advancement lieve_vadsjes/huisje_villa, the texts;
  - the inside (Huisje betreden): a big room guhs:huisje_binnen/villa with the new beds of 1.8.0 (two Knuffelnesten, the
    Guh-hoofd-bank, two stapelbedjes, the Guh-en-Blub-mandje, two luciferdoosjes, a schelpbedje) and the Droombed as the
    logeerbedje. Every vakje of those beds is a "bed" of the room in kamers.json, with its exact spot (punt), which way the
    sleeper looks and who sleeps there (soort): Java puts each resident in a vakje of its own kind (BinnenInrichting.verdeel).
    The vakjes of the family beds come from FamilieVormen.java (the bed renders), so they always match the real beds.

Run: PYTHONPATH=tools python tools/features/guhvilla.py (after huisje.py / huisje_binnen.py: it adds the villa to kamers.json).
"""
import json
import math
import os
import re
import sys

from PIL import Image, ImageDraw

from features import huisje

ROOM_H = 3   # (the villa's hall is three blocks high inside: room for the stapelbedjes)

TEKSTEN = {   # key: (nl, en)
    "block.guhs.guhhuisje_villa": ("Guhvilla", "Guh Villa"),
    "block.guhs.guhhuisje_villa.lore": ("Twee verdiepingen, een balkonnetje, een kroontje op het dak en plek voor twintig vadsjes. Villa Vadsvorst!",
                                        "Two floors, a little balcony, a crown on the roof and room for twenty vadsjes. Villa Vadsvorst!"),
    "gui.guhs.huisje.bereik": ("Klus-area: %s blokken", "Chore area: %s blocks"),
    "gui.guhs.huisje.bereik.tooltip": ("Hoe ver je bewoners rondlopen, klusjes doen en spelen: standaard %1$s, van %2$s tot %3$s blokken. "
                                       "Schuif en kijk naar de blauwe koepel. Groter is niet altijd vadsiger, njeg!",
                                       "How far your residents wander, do chores and play: %1$s by default, from %2$s to %3$s blocks. "
                                       "Slide and watch the blue dome. Bigger isn't always vadsier, njeg!"),
    "advancements.guhs.lieve_vadsjes.huisje_villa.title": ("Villa Vadsvorst", "Villa Vadsvorst"),
    "advancements.guhs.lieve_vadsjes.huisje_villa.description": ("Bouw een Guhvilla: twee verdiepingen, een kroontje en plek voor twintig vadsjes",
                                                                 "Build a Guh Villa: two floors, a crown and room for twenty vadsjes"),
}


# =====================================================================================================================
# textures
# =====================================================================================================================
def _pilaar():
    img = Image.new("RGBA", (16, 16), (255, 236, 244, 255))
    d = ImageDraw.Draw(img)
    for x in (2, 7, 12):
        d.line([(x, 0), (x, 15)], fill=(244, 190, 214, 255))
    d.rectangle([0, 0, 15, 1], fill=(246, 168, 200, 255))
    d.rectangle([0, 14, 15, 15], fill=(246, 168, 200, 255))
    return img


def _kroon():
    img = Image.new("RGBA", (16, 16), (246, 196, 72, 255))
    d = ImageDraw.Draw(img)
    for x in range(16):
        for y in range(16):
            if (x + y) % 5 == 0:
                img.putpixel((x, y), (255, 226, 120, 255))
    for c, (x, y) in zip([(236, 70, 120, 255), (120, 200, 255, 255), (150, 230, 140, 255)], [(3, 7), (8, 6), (12, 8)]):
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=c)
    d.rectangle([0, 15, 15, 15], fill=(196, 140, 40, 255))
    return img


def _icoon():
    """The item / quest picture: a big pink guh head with two rows of windows, a porch and a golden crown (16x16)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rand = (150, 60, 100, 255)
    v = huisje.VACHT + (255,)
    for x0 in (0, 11):                                   # ears
        d.ellipse([x0, 1, x0 + 4, 6], fill=rand)
        d.ellipse([x0 + 1, 2, x0 + 3, 5], fill=v)
    d.rectangle([6, 0, 9, 2], fill=(246, 196, 72, 255))   # crown
    img.putpixel((6, 0), (0, 0, 0, 0))
    img.putpixel((9, 0), (0, 0, 0, 0))
    img.putpixel((7, 1), (236, 70, 120, 255))
    d.rectangle([0, 3, 15, 15], fill=rand)               # the two storeys
    d.rectangle([1, 4, 14, 14], fill=v)
    d.line([(1, 9), (14, 9)], fill=huisje.VACHT_DONKER + (255,))
    for x0 in (3, 10):                                   # big eyes upstairs
        d.rectangle([x0, 5, x0 + 2, 7], fill=(52, 26, 52, 255))
        img.putpixel((x0, 5), (255, 255, 255, 255))
    for x0 in (2, 12):                                   # little windows downstairs
        img.putpixel((x0, 11), (52, 26, 52, 255))
    d.rectangle([5, 10, 10, 15], fill=huisje.LICHT + (255,))   # porch + door
    d.rectangle([6, 11, 9, 15], fill=(200, 116, 132, 255))
    img.putpixel((9, 13), (255, 226, 120, 255))
    for x in (2, 3, 12, 13):                             # flower boxes
        img.putpixel((x, 8), (98, 176, 96, 255))
    img.putpixel((2, 8), (255, 150, 200, 255))
    img.putpixel((13, 8), (255, 236, 120, 255))
    return img


# =====================================================================================================================
# the models (model pixels; the front with the porch and the snoet door faces north = low z)
# =====================================================================================================================
_el = huisje._el


def _romp():
    E = [
        # the ground floor: two crossed cores, a bit wider than the groot huisje
        _el([-7, 0, -3], [23, 16, 21], "vacht"),
        _el([-5, 0, -5], [21, 16, 23], "vacht"),
        # the upper floor (the guh head), rounded off on top
        _el([-6, 16, -2], [22, 30, 20], "vacht"),
        _el([-4, 16, -4], [20, 30, 22], "vacht", ["north", "south", "east", "west", "up"]),
        _el([-3, 30, 0], [19, 32, 18], "vacht"),
        # upstairs: the big shiny eyes, blush, flower boxes under the eyes
        _el([-2, 19, -4.6], [6, 28, -4], "oog", ["north"]),
        _el([10, 19, -4.6], [18, 28, -4], "oog", ["north"]),
        _el([-4.5, 17, -4.3], [-0.5, 19, -4], "blos", ["north"]),
        _el([16.5, 17, -4.3], [20.5, 19, -4], "blos", ["north"]),
        _el([-3, 16.5, -6.5], [7, 18.5, -4], "bloembak", ["north", "east", "west", "up"]),
        _el([9, 16.5, -6.5], [19, 18.5, -4], "bloembak", ["north", "east", "west", "up"]),
        # downstairs: two little windows beside the porch
        _el([-5, 5, -5.6], [0, 11, -5], "oog", ["north"]),
        _el([16, 5, -5.6], [21, 11, -5], "oog", ["north"]),
        # the porch: the snoet with the door, the nose, two pillars, the roof with the balcony and its railing, the steps
        _el([3, 0, -9], [13, 9, -5], "vacht_licht", ["east", "west", "up", "north"], front="deur"),
        _el([6, 9, -8], [10, 11.5, -5], "neus", ["north", "east", "west", "up"]),
        _el([-1, 0, -11], [1.5, 12, -8.5], "pilaar"),
        _el([14.5, 0, -11], [17, 12, -8.5], "pilaar"),
        _el([-2, 12, -12], [18, 13.5, -5], "pluis"),
        _el([-2, 13.5, -12], [18, 15.5, -11.5], "bloembak", ["north", "south", "east", "west", "up"]),
        _el([2, 0, -14], [14, 1.5, -11], "vacht_licht", ["north", "east", "west", "up"]),
        # little windows in the sides (both floors)
        _el([23, 5, 4], [23.6, 11, 10], "oog", ["east"]),
        _el([-7.6, 5, 4], [-7, 11, 10], "oog", ["west"]),
        _el([22, 20, 6], [22.6, 26, 12], "oog", ["east"]),
        _el([-6.6, 20, 6], [-6, 26, 12], "oog", ["west"]),
        # two chimneys at the back
        _el([14, 24, 16], [19, 32, 21], "schoorsteen"),
        _el([-3, 24, 16], [2, 32, 21], "schoorsteen"),
    ]
    return E


def _dak():
    """The roof, drawn 32 model pixels up: the big ears, the pluis, the golden crown, a bow, the chimney tops."""
    return [
        _el([-8, -4, 4], [4, 12, 13], "vacht"),
        _el([-7, 12, 5], [3, 15, 12], "vacht"),
        _el([-6.5, -1, 3.5], [2.5, 11.5, 4], "oor", ["north"]),
        _el([12, -4, 4], [24, 12, 13], "vacht"),
        _el([13, 12, 5], [23, 15, 12], "vacht"),
        _el([13.5, -1, 3.5], [22.5, 11.5, 4], "oor", ["north"]),
        _el([4, 0, 3], [12, 3, 13], "pluis"),
        _el([6, 3, 5], [10, 4.5, 11], "pluis"),
        _el([5, 4.5, 6], [11, 8, 10], "kroon"),
        _el([5, 8, 6], [6.5, 10, 7.5], "kroon"),
        _el([7.25, 8, 6], [8.75, 11, 7.5], "kroon"),
        _el([9.5, 8, 6], [11, 10, 7.5], "kroon"),
        _el([-9, 6, 2.5], [5, 9, 3.5], "strik", ["north", "south", "east", "west", "up"]),
        _el([13.5, 0, 15.5], [19.5, 1.5, 21.5], "schoorsteen"),
        _el([-3.5, 0, 15.5], [2.5, 1.5, 21.5], "schoorsteen"),
    ]


def _model(elements):
    tex = {k: f"guhs:block/guhhuisje_{k}" for k in list(huisje.TEXTURES) + ["pilaar", "kroon"]}
    tex["particle"] = "guhs:block/guhhuisje_vacht"
    m = huisje._model("groot")   # (the same display transforms as the other huisjes)
    m.pop("render_type", None)   # (26.1: the layer comes from the textures)
    m["textures"] = tex
    m["elements"] = elements
    return m


def blocks_and_items(h):
    A = h.A
    h.save(_pilaar(), "block", "guhhuisje_pilaar.png")
    h.save(_kroon(), "block", "guhhuisje_kroon.png")
    h.save(_icoon(), "item", "guhhuisje_villa.png")
    name = "guhhuisje_villa"
    h.w(f"{A}/models/block/{name}_model.json", _model(_romp()))
    h.w(f"{A}/models/block/{name}_dak_model.json", _model(_dak()))
    h.w(f"{A}/models/block/{name}.json", {"textures": {"particle": "guhs:block/guhhuisje_vacht"}})
    h.w(f"{A}/blockstates/{name}.json", {"variants": {f"facing={f}": {"model": f"guhs:block/{name}"} for f in huisje.ROT}})
    # (26.1: no render_type, no separate_transforms: a 3D model in the hand and a gui model, chosen by items/<name>.json)
    h.w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}_model"})
    h.w(f"{A}/models/item/{name}_gui.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{name}"}})
    h.self_drop(name)
    h.shaped(name, ["KTK", "WHW", "BNB"], {"K": "guhs:guh_kristal", "T": "guhs:timmerguh_bouwboekje", "W": "minecraft:pink_wool",
                                           "H": "guhs:guhhuisje_groot", "B": "guhs:block_of_kaasknabbels", "N": "guhs:knuffelnest"},
             f"guhs:{name}")
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:{name}"])
    from features import band
    band.visible(h, "huisje_villa", "huisje_groot", f"guhs:{name}", "challenge",
                 TEKSTEN["advancements.guhs.lieve_vadsjes.huisje_villa.title"][0],
                 TEKSTEN["advancements.guhs.lieve_vadsjes.huisje_villa.description"][0])


# =====================================================================================================================
# the inside: the villa's hall with the new beds
# =====================================================================================================================
def _vormen():
    """The family beds from FamilieVormen.java: id -> (sx, sy, sz, cells, vakjes [(u, v, w, soort)])."""
    pad = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "bedden", "FamilieVormen.java")
    tekst = open(pad, encoding="utf-8").read()
    uit = {}
    for m in re.finditer(r'VORMEN\.put\("([a-z_]+)", new Vorm\((\d+), (\d+), (\d+), new int\[\] \{([^}]*)\}, b, List\.of\((.*?)\)\)\);', tekst):
        vak = [(float(a), float(b), float(c), s.lower()) for a, b, c, s in
               re.findall(r"new Vakjes\.Vakje\(([-\d.]+), ([-\d.]+), ([-\d.]+), Vakjes\.Soort\.([A-Z]+)\)", m.group(6))]
        uit[m.group(1)] = (int(m.group(2)), int(m.group(3)), int(m.group(4)), [int(x) for x in m.group(5).split(",")], vak)
    return uit


def _draai(f, a, b):
    return {"east": (-b, a), "south": (-a, -b), "west": (b, -a)}.get(f, (a, b))


def _cel(anker, f, sz, lx, ly, lz):
    d = _draai(f, lx, lz - (sz - 1))
    return (anker[0] + int(round(d[0])), anker[1] + ly, anker[2] + int(round(d[1])))


def _punt(anker, f, sz, u, v, w):
    lx, lz = math.floor(u), math.floor(w)
    c = _cel(anker, f, sz, lx, 0, lz)
    o = _draai(f, u - lx - 0.5, w - lz - 0.5)
    return (c[0] + 0.5 + o[0], anker[1] + v, c[2] + 0.5 + o[1])


_YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}
_KIJK_VAN = {"north": "south", "south": "north", "east": "west", "west": "east"}   # (a bed facing north: the sleepers look south)


def _yaw_naar(van, naar):
    dx, dz = naar[0] - van[0], naar[2] - van[2]
    return round(-math.degrees(math.atan2(dx, dz)), 1)


def kamer(h):
    """The villa's hall: (template, layout) with every vakje as a bed of kamers.json."""
    vormen = _vormen()
    w, d = 20, 16
    X, Y, Z = w + 2, ROOM_H + 2, d + 3
    s = h.Structure((X, Y, Z))
    vloer, muur, plafond = "minecraft:cherry_planks", "minecraft:pink_wool", "minecraft:white_wool"
    for x in range(X):
        for z in range(d + 2):
            s.set(x, 0, z, vloer)
            s.set(x, ROOM_H + 1, z, plafond)
            rand = x in (0, X - 1) or z in (0, d + 1)
            for y in range(1, ROOM_H + 1):
                s.set(x, y, z, muur if rand else "minecraft:air")
    deur_x = (w - 1) // 2 + 1
    for y in range(0, ROOM_H + 2):
        for x in (deur_x - 1, deur_x, deur_x + 1):
            s.set(x, y, d + 2, muur)
    bezet = set()
    bedden = []

    def vrij(p):
        assert p not in bezet and s.get(*p) == "minecraft:air", ("villa: bed on bed", p)
        bezet.add(p)

    def familie(bid, anker, f, yaw_fn=None):
        sx, sy, sz, cellen, vakjes = vormen[bid]
        for deel in cellen:
            lx, rest = deel % sx, deel // sx
            p = _cel(anker, f, sz, lx, rest // sz, rest % sz)
            vrij(p)
            s.set(*p, f"guhs:{bid}", {"facing": f, "deel": str(deel)})
        midden = _punt(anker, f, sz, sx / 2, 0, sz / 2)
        for (u, v, ww, soort) in vakjes:
            pt = _punt(anker, f, sz, u, v, ww)
            yaw = yaw_fn(pt, midden) if yaw_fn else _YAW[_KIJK_VAN[f]]
            bedden.append({"bed": [math.floor(pt[0]), anker[1], math.floor(pt[2])], "kijk": _KIJK_VAN[f],
                           "punt": [round(c, 3) for c in pt], "yaw": yaw, "soort": soort})

    def enkel(bid, p, f, vakjes, props=None):
        vrij(p)
        s.set(*p, f"guhs:{bid}", dict({"facing": f}, **(props or {})))
        for (u, v, ww, soort) in vakjes:
            pt = _punt(p, f, 1, u, v, ww)
            bedden.append({"bed": list(p), "kijk": _KIJK_VAN[f], "punt": [round(c, 3) for c in pt], "yaw": _YAW[_KIJK_VAN[f]],
                           "soort": soort})

    # two Knuffelnesten in the north corners (eight guhs each, heads to the middle of the nest)
    naar_midden = lambda pt, m: _yaw_naar(pt, m)   # noqa: E731
    familie("knuffelnest", (1, 1, 4), "north", naar_midden)
    familie("knuffelnest", (17, 1, 4), "north", naar_midden)
    # the Guh-hoofd-bank against the north wall in the middle (a guh, a Blub, a muisje, a Schilly)
    familie("guh_hoofdbank", (8, 1, 2), "north")
    # two stapelbedjes beside it (two guhs each)
    for x in (6, 14):
        vrij((x, 1, 1))
        vrij((x, 2, 1))
        s.set(x, 1, 1, "guhs:stapelbedje", {"facing": "south", "half": "lower"})
        s.set(x, 2, 1, "guhs:stapelbedje", {"facing": "south", "half": "upper"})
        for y in (1, 2):
            bedden.append({"bed": [x, y, 1], "kijk": "south", "punt": [x + 0.5, y + 0.19, 1.5], "yaw": 0.0, "soort": "guh"})
    # along the west wall: the Guh-en-Blub-mandje and a luciferdoosje; along the east wall: a schelpbedje and a luciferdoosje
    enkel("guh_blub_mandje", (1, 1, 7), "east", [(0.32, 0.18, 0.5, "guh"), (0.72, 0.125, 0.4, "blub")])
    enkel("luciferdoosje", (1, 1, 9), "east", [(0.5, 0.19, 0.6, "muis")])
    enkel("schelpbedje", (20, 1, 7), "west", [(0.36, 0.13, 0.62, "schilly"), (0.66, 0.13, 0.66, "schilly")])
    enkel("luciferdoosje", (20, 1, 9), "west", [(0.5, 0.19, 0.6, "muis")])
    # the door with the doormat, the Droombed as the logeerbedje, the prikbord, the knabbelbak and cushions
    for y, half in ((1, "lower"), (2, "upper")):
        s.set(deur_x, y, d + 1, "minecraft:cherry_door", {"facing": "south", "half": half, "hinge": "left", "open": "false", "powered": "false"})
    s.set(deur_x, 3, d + 1, muur)
    mat = (deur_x, 1, d)
    s.set(*mat, "minecraft:magenta_carpet")
    s.set(1, 1, d, "guhs:droombed", {"facing": "west", "part": "head", "occupied": "false"})
    s.set(2, 1, d, "guhs:droombed", {"facing": "west", "part": "foot", "occupied": "false"})
    import sign_text
    tekst = {"messages": sign_text.messages(huisje_binnen().SIGN, ["~ Prikbord ~", "Wie doet", "welk klusje?", "(klik, njeg)"]),
             "color": "black", "has_glowing_text": h.ms.Byte(0)}
    leeg = {"messages": sign_text.messages(huisje_binnen().SIGN, ["", "", "", ""]), "color": "black", "has_glowing_text": h.ms.Byte(0)}
    prikbord = (w - 1, 2, d)
    s.set(*prikbord, "minecraft:cherry_wall_sign", {"facing": "north", "waterlogged": "false"},
          {"id": "minecraft:sign", "is_waxed": h.ms.Byte(1), "front_text": tekst, "back_text": leeg})
    bak = (w, 1, d)
    s.set(*bak, "guhs:knabbelbak", {"facing": "north"})
    for p, f in (((w - 1, 1, d), "north"), ((4, 1, d), "north"), ((15, 1, d), "north")):
        s.set(*p, "guhs:pink_kussen", {"facing": f})
    # a big soft rug in the middle (not under a bed), maanlampjes and a few plants
    for x in range(5, 17):
        for z in range(6, 14):
            if (x, 1, z) not in bezet and s.get(x, 1, z) == "minecraft:air":
                s.set(x, 1, z, "minecraft:pink_carpet" if (x + z) % 2 else "minecraft:white_carpet")
    for p in ((5, 1, 5), (16, 1, 5)):
        if p not in bezet:
            s.set(*p, "guhs:maanlampje_maan", _lampje())
    # little windows with a painted view in every wall
    ramen = [(0, 2, z) for z in (6, 11, 14)] + [(X - 1, 2, z) for z in (6, 11, 14)] + [(x, 2, d + 1) for x in (4, 7, 13, 16)]
    for r in ramen:
        if s.get(*r) == muur and r != (prikbord[0], 2, d + 1):
            s.set(*r, "guhs:huisje_raam")
    # pink lamps in the ceiling
    for x in (4, 10, 17):
        for z in (4, 10, 14):
            s.set(x, ROOM_H + 1, z, "minecraft:pearlescent_froglight", {"axis": "y"})
    s.save("huisje_binnen/villa")
    assert sum(1 for b in bedden if b["soort"] == "guh") >= 20, "the villa sleeps twenty guhs"
    return {
        "maat": [X, Y, Z], "mat": list(mat), "yaw": 180.0, "deur": [[deur_x, 1, d + 1], [deur_x, 2, d + 1]],
        "prikbord": list(prikbord), "logeerbed": {"hoofd": [1, 1, d], "voet": [2, 1, d]},
        "bedden": bedden,
    }


def _lampje():
    """The state of a lit maanlampje (whatever properties slaapzorg gave it: facing south, switched on)."""
    p = os.path.join("src", "main", "resources", "assets", "guhs", "blockstates", "maanlampje_maan.json")
    sleutels = list(json.load(open(p, encoding="utf-8"))["variants"])
    kies = next((k for k in sleutels if "facing=south" in k and ("aan=true" in k or "lit=true" in k)), sleutels[0])
    return dict(kv.split("=") for kv in kies.split(",") if "=" in kv)


def huisje_binnen():
    from features import huisje_binnen as hb
    return hb


def binnen(h):
    """Adds the villa to kamers.json (written by huisje_binnen.py for klein, medium and groot)."""
    pad = f"{h.D}/huisje_binnen/kamers.json"
    data = json.load(open(pad, encoding="utf-8"))
    data["kamers"]["villa"] = kamer(h)
    h.w(pad, data)


def build(h):
    blocks_and_items(h)
    binnen(h)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c216_guhvilla.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    with open(os.path.join(a, "guhhuisje_villa.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"model": {"type": "minecraft:select", "property": "minecraft:display_context",
                             "cases": [{"when": "gui", "model": {"type": "minecraft:model", "model": "guhs:item/guhhuisje_villa_gui"}}],
                             "fallback": {"type": "minecraft:model", "model": "guhs:item/guhhuisje_villa"}}}, f, indent=2)
        f.write("\n")
    print("guhvilla: model + roof, recipe, room with",
          len(json.load(open(os.path.join(h.D, "huisje_binnen", "kamers.json"), encoding="utf-8"))["kamers"]["villa"]["bedden"]), "vakjes")


if __name__ == "__main__":
    _direct()
