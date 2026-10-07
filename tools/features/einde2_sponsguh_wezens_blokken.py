"""
einde2, slice sponsguh_wezens - the blocks and items of the Sponsguh story: textures (painted here, 16 x 16 pixel art),
block models, blockstates, item models, loot, recipes and tags.

  Karen                 a monitor on a wheeled stand, a face of lines per block state gezicht 0-3 (neutraal, blij, boos, zucht)
  krokante_grill        the grill of De Krokante Knabbel (a second model while a burger bakes: block state bakt)
  besteltafel           a little table with a checked cloth and a bell
  vruchthuis_*          the knabbelkoorvrucht house as building blocks: the fruit shell wall, the leafy crown as a roof, a round
                        window and a round door (vanilla's door models with our textures)
  bellenblaas           a pot of soapy water with a bubble wand
  spooklantaarn         a ghost-green lantern (vanilla's lantern models)
  knabbelburger, kwallenjam   the food

build(h) writes everything; the Java side is feature/einde2/sponsguh/SponsguhBlokken + E2SponsguhFeature.
"""
import json
import math
import os
import zipfile

from PIL import Image

from features import einde2_kit as kit

FRUIT = (232, 140, 66)          # the fruit shell: orange...
FRUIT_PAARS = (150, 92, 190)    # ...going purple (knabbelkoor)
BLAD = (128, 84, 176)
BLAD_LICHT = (186, 140, 226)
STAAL = (170, 174, 186)
STAAL_DONKER = (110, 114, 128)
SCHERM = (16, 48, 44)
LIJN = (120, 255, 190)
KAST = (70, 130, 138)
HOUT = (170, 120, 70)


def _img(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def _ruis(img, rng, var=8):
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a:
                v = rng.randint(-var, var)
                px[x, y] = (max(0, min(255, r + v)), max(0, min(255, g + v)), max(0, min(255, b + v)), a)
    return img


def _vul(img, x0, y0, x1, y1, c):
    for y in range(max(0, y0), min(img.height, y1)):
        for x in range(max(0, x0), min(img.width, x1)):
            img.putpixel((x, y), tuple(c) + (255,) if len(c) == 3 else tuple(c))


def _rng(naam):
    import random
    return random.Random(kit.seed("sponsguh_wezens/blok/" + naam))


# =====================================================================================================================
# textures
# =====================================================================================================================
def schil(rng, raam=False, glas=(190, 232, 244, 110)):
    """The fruit shell: orange at the top going purple at the bottom, little seed dots; with a round window when raam."""
    img = _img()
    for y in range(16):
        t = y / 15
        c = tuple(int(FRUIT[i] * (1 - t * 0.75) + FRUIT_PAARS[i] * t * 0.75) for i in range(3))
        for x in range(16):
            img.putpixel((x, y), c + (255,))
    for _ in range(9):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        img.putpixel((x, y), (255, 214, 150, 255))
    for y in range(16):                                  # darker seams left and right: the segments of the fruit
        for x in (0, 15):
            r, g, b, a = img.getpixel((x, y))
            img.putpixel((x, y), (int(r * 0.82), int(g * 0.82), int(b * 0.82), 255))
    _ruis(img, rng, 6)
    if raam:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 4.6:
                    img.putpixel((x, y), glas)
                elif d < 6.0:
                    img.putpixel((x, y), (96, 56, 130, 255))            # the round frame
        for i in range(3, 13):                                             # the cross bars
            img.putpixel((i, 7), (96, 56, 130, 255))
            img.putpixel((7, i), (96, 56, 130, 255))
        img.putpixel((5, 5), (255, 255, 255, 170))
        img.putpixel((6, 4), (255, 255, 255, 170))
    return img


def blad(rng, top=False):
    """The leafy crown of the fruit: overlapping purple leaves."""
    img = _img()
    _vul(img, 0, 0, 16, 16, BLAD)
    for j, y in enumerate(range(-1, 17, 4)):
        for x in range(-2 + 2 * (j % 2), 18, 4):
            for dy in range(4):
                for dx in range(-1 - (dy < 2), 2 + (dy < 2)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < 16 and 0 <= yy < 16:
                        img.putpixel((xx, yy), (BLAD_LICHT if dy == 0 else BLAD if dy < 3 else (96, 60, 136)) + (255,))
    if top:
        for y in range(6, 10):
            for x in range(6, 10):
                img.putpixel((x, y), (110, 170, 90, 255))                   # the stalk
    return _ruis(img, rng, 7)


def deur(rng):
    """The round door of the fruit house, as vanilla's two door halves (top with a porthole) and its item icon."""
    boven, onder = _img(), _img()
    paars, rand, knop = (150, 100, 196), (96, 56, 130), (250, 214, 90)
    for img, top in ((boven, True), (onder, False)):
        _vul(img, 0, 0, 16, 16, paars)
        for x in range(16):
            for y in range(16):
                if x in (0, 15) or (top and y == 0) or (not top and y == 15):
                    img.putpixel((x, y), rand + (255,))
        for x in (5, 10):
            _vul(img, x, 0, x + 1, 16, (128, 82, 172))
    for y in range(16):                                    # the arch: round shoulders at the top
        for x in range(16):
            if math.hypot(x - 7.5, y - 8) > 8.4 and y < 6:
                boven.putpixel((x, y), (0, 0, 0, 0))
            elif 7.2 < math.hypot(x - 7.5, y - 8) <= 8.4 and y < 6:
                boven.putpixel((x, y), rand + (255,))
            d = math.hypot(x - 7.5, y - 8.5)
            if d < 2.6:
                boven.putpixel((x, y), (0, 0, 0, 0))                        # the porthole
            elif d < 3.7:
                boven.putpixel((x, y), rand + (255,))
    _vul(onder, 12, 2, 14, 4, knop)
    _ruis(boven, rng, 6)
    _ruis(onder, rng, 6)
    icoon = _img()
    for y in range(16):
        for x in range(4, 12):
            bron = boven if y < 8 else onder
            icoon.putpixel((x, y), bron.getpixel(((x - 4) * 2, (y % 8) * 2)))
    return boven, onder, icoon


def karen_scherm(gezicht):
    """Karen's face of lines on a dark green screen: 0 neutraal, 1 blij, 2 boos, 3 zucht."""
    img = _img()
    _vul(img, 0, 0, 16, 16, KAST)
    _vul(img, 1, 1, 15, 13, (40, 76, 82))
    _vul(img, 2, 2, 14, 12, SCHERM)
    for y in range(3, 12, 2):                              # scan lines
        _vul(img, 2, y, 14, y + 1, (20, 58, 52))
    L = LIJN
    if gezicht == 0:
        _vul(img, 4, 5, 6, 6, L), _vul(img, 10, 5, 12, 6, L), _vul(img, 5, 9, 11, 10, L)
    elif gezicht == 1:
        _vul(img, 4, 4, 6, 6, L), _vul(img, 10, 4, 12, 6, L), _vul(img, 5, 9, 11, 10, L)
        img.putpixel((4, 8), L + (255,)), img.putpixel((11, 8), L + (255,))
    elif gezicht == 2:
        L = (255, 110, 110)
        for i in range(3):
            img.putpixel((4 + i, 4 + i), L + (255,)), img.putpixel((11 - i, 4 + i), L + (255,))
        _vul(img, 5, 9, 11, 10, L)
        img.putpixel((4, 10), L + (255,)), img.putpixel((11, 10), L + (255,))
    else:
        _vul(img, 4, 6, 7, 7, L), _vul(img, 9, 6, 12, 7, L), _vul(img, 7, 9, 10, 11, L)
        img.putpixel((8, 10), SCHERM + (255,))
    _vul(img, 12, 14, 14, 15, (255, 80, 80) if gezicht == 2 else (120, 255, 150))    # the power light
    _vul(img, 2, 14, 6, 15, (40, 76, 82))
    return img


def kast(rng):
    img = _img()
    _vul(img, 0, 0, 16, 16, KAST)
    for y in (4, 8, 12):
        _vul(img, 3, y, 13, y + 1, (40, 76, 82))                            # air slits
    return _ruis(img, rng, 5)


def staal(rng, wielen=False):
    img = _img()
    _vul(img, 0, 0, 16, 16, STAAL)
    _vul(img, 0, 0, 16, 1, (214, 218, 228)), _vul(img, 0, 15, 16, 16, STAAL_DONKER)
    if wielen:
        for x in (1, 12):
            _vul(img, x, 12, x + 3, 16, (40, 40, 48))
    return _ruis(img, rng, 5)


def grill_voor(rng):
    img = staal(rng)
    _vul(img, 1, 2, 15, 6, STAAL_DONKER)
    for x in (3, 7, 11):
        _vul(img, x, 3, x + 2, 5, (220, 60, 50))                            # knobs
    _vul(img, 2, 8, 14, 14, (60, 62, 72))
    _vul(img, 3, 9, 13, 13, (250, 150, 50))                                 # the oven window, a warm glow
    _vul(img, 3, 9, 13, 10, (255, 214, 120))
    return img


def grill_boven(rng, bakt):
    img = _img()
    _vul(img, 0, 0, 16, 16, (56, 58, 66))
    for y in range(1, 15, 2):
        _vul(img, 1, y, 15, y + 1, (250, 120, 40) if bakt else (96, 98, 108))    # the bars (hot while it bakes)
    _vul(img, 0, 0, 16, 1, STAAL), _vul(img, 0, 15, 16, 16, STAAL), _vul(img, 0, 0, 1, 16, STAAL), _vul(img, 15, 0, 16, 16, STAAL)
    if bakt:
        for cx, cy in ((5, 5), (11, 10)):                                   # two patties
            for y in range(16):
                for x in range(16):
                    if math.hypot(x - cx, y - cy) < 3:
                        img.putpixel((x, y), (112, 64, 34, 255))
            img.putpixel((cx, cy), (70, 36, 20, 255))
    return _ruis(img, rng, 4)


def tafelkleed(rng):
    img = _img()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), ((214, 50, 56) if (x // 4 + y // 4) % 2 else (250, 246, 240)) + (255,))
    return _ruis(img, rng, 4)


def hout(rng, donker=False):
    img = _img()
    base = tuple(int(c * 0.78) for c in HOUT) if donker else HOUT
    _vul(img, 0, 0, 16, 16, base)
    for y in (3, 7, 11, 15):
        _vul(img, 0, y, 16, y + 1, tuple(int(c * 0.86) for c in base))
    return _ruis(img, rng, 6)


def goud(rng):
    img = _img()
    _vul(img, 0, 0, 16, 16, (250, 204, 70))
    _vul(img, 2, 2, 6, 5, (255, 244, 170))
    _vul(img, 0, 13, 16, 16, (200, 150, 40))
    return _ruis(img, rng, 5)


def bellenpot(rng):
    """The side of the bubble blower's pot: pink with a soap bubble on it."""
    img = _img()
    _vul(img, 0, 0, 16, 16, (250, 150, 196))
    _vul(img, 0, 0, 16, 2, (255, 200, 226)), _vul(img, 0, 14, 16, 16, (214, 110, 160))
    for cx, cy, r in ((5, 7, 2.6), (11, 10, 1.8)):
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if r - 0.9 < d <= r:
                    img.putpixel((x, y), (255, 255, 255, 255))
        img.putpixel((int(cx - 1), int(cy - 1)), (255, 255, 255, 255))
    return _ruis(img, rng, 4)


def sop(rng):
    """The top of the pot: soapy water full of little bubbles."""
    img = _img()
    _vul(img, 0, 0, 16, 16, (250, 150, 196))
    _vul(img, 2, 2, 14, 14, (170, 220, 250))
    for _ in range(12):
        x, y = rng.randint(3, 12), rng.randint(3, 12)
        img.putpixel((x, y), (255, 255, 255, 255))
        img.putpixel((x + 1, y), (214, 240, 255, 255))
    return img


def bellenring():
    """The wand: a stick with a ring (see-through middle, a soap film glint)."""
    img = _img()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 5.5)
            if 3.2 < d <= 5.2:
                img.putpixel((x, y), (120, 200, 250, 255))
            elif d <= 3.2 and (x + y) % 5 == 0:
                img.putpixel((x, y), (255, 255, 255, 150))
    _vul(img, 7, 10, 9, 16, (120, 200, 250))
    return img


def burger():
    rows = ["................", "....bbbbbbbb....", "...bBBsBBsBBb...", "..bBsBBBBBBsBb..", "..bBBBBsBBBBBb..", "..bbbbbbbbbbbb..",
            ".gGgGGgGGgGGgGg.", ".kkkkkkkkkkkkkk.", "..kyyykkkkyyyk..", ".rrrrrrrrrrrrrr.", "..mmmmmmmmmmmm..", "..mMmmMmmmMmMm..",
            "..cccccccccccc..", "..cCCCCCCCCCCc..", "...cccccccccc...", "................"]
    pal = {"b": (190, 126, 52), "B": (236, 172, 88), "s": (255, 244, 206), "g": (96, 170, 60), "G": (140, 214, 90), "k": (250, 210, 70),
           "y": (214, 160, 38), "r": (214, 60, 50), "m": (112, 64, 34), "M": (70, 36, 20), "c": (190, 126, 52), "C": (232, 170, 86)}
    img = _img()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch] + (255,))
    return img


def jam():
    rows = ["................", "....wwwwwwww....", "....wWWWWWWw....", "....dddddddd....", "...gggggggggg...", "..gjjjjjjjjjjg..", "..gjJjjjjjjJjg..",
            "..gjjjjLjjjjjg..", "..gjjjLlLjjjjg..", "..gjjjjLjjjJjg..", "..gjJjjjjjjjjg..", "..gjjjjjjJjjjg..", "..gjjjjjjjjjjg..", "...gggggggggg...",
            "................", "................"]
    pal = {"w": (236, 226, 240), "W": (255, 255, 255), "d": (180, 160, 190), "g": (190, 220, 236), "j": (240, 110, 196), "J": (255, 170, 226),
           "L": (255, 236, 250), "l": (255, 255, 255)}
    img = _img()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch] + ((200,) if ch == "g" else (255,)))
    return img


def textures(h):
    s = h.save
    s(schil(_rng("wand")), "block", "vruchthuis_wand.png")
    s(schil(_rng("wand"), raam=True), "block", "vruchthuis_raam.png")
    s(blad(_rng("dak")), "block", "vruchthuis_dak.png")
    s(blad(_rng("dak_top"), top=True), "block", "vruchthuis_dak_top.png")
    boven, onder, icoon = deur(_rng("deur"))
    s(boven, "block", "vruchthuis_deur_top.png")
    s(onder, "block", "vruchthuis_deur_bottom.png")
    s(icoon, "item", "vruchthuis_deur.png")
    for g in range(4):
        s(karen_scherm(g), "block", f"karen_gezicht_{g}.png")
    s(kast(_rng("karen")), "block", "karen.png")
    s(staal(_rng("staal")), "block", "krokante_grill_zij.png")
    s(staal(_rng("voet"), wielen=True), "block", "karen_voet.png")
    s(grill_voor(_rng("grill")), "block", "krokante_grill_voor.png")
    s(grill_boven(_rng("grill_top"), False), "block", "krokante_grill_boven.png")
    s(grill_boven(_rng("grill_top"), True), "block", "krokante_grill_boven_bakt.png")
    s(tafelkleed(_rng("kleed")), "block", "besteltafel_kleed.png")
    s(hout(_rng("hout")), "block", "besteltafel.png")
    s(goud(_rng("bel")), "block", "besteltafel_bel.png")
    s(bellenpot(_rng("pot")), "block", "bellenblaas.png")
    s(sop(_rng("sop")), "block", "bellenblaas_sop.png")
    s(bellenring(), "block", "bellenblaas_ring.png")
    # the ghost lantern: vanilla's soul lantern, turned ghost green
    vlam = lambda hue, sat, val: sat > 0.25                 # noqa: E731  (only the flame and its glow, not the grey iron)
    s(h.recolour(h.vanilla("block/soul_lantern"), hue=0.37, sat=0.85, val=1.08, only=vlam), "block", "spooklantaarn.png")
    s(h.recolour(h.vanilla("item/soul_lantern"), hue=0.37, sat=0.85, val=1.08, only=vlam), "item", "spooklantaarn.png")
    s(burger(), "item", "knabbelburger.png")
    s(jam(), "item", "kwallenjam.png")


# =====================================================================================================================
# models
# =====================================================================================================================
def el(frm, to, faces):
    """A model element; faces: {side: texture key} or {side: (texture key, [u0, v0, u1, v1])}."""
    out = {}
    for side, f in faces.items():
        key, uv = (f, None) if isinstance(f, str) else f
        out[side] = {"texture": "#" + key, **({"uv": uv} if uv else {})}
    return {"from": list(frm), "to": list(to), "faces": out}


ALLE = ("north", "south", "east", "west", "up", "down")


def rondom(key, **anders):
    f = {s: key for s in ALLE}
    f.update(anders)
    return f


def models(h):
    A, w = h.A, h.w
    # --- Karen: four models (one per face), turned by facing
    for g in range(4):
        w(f"{A}/models/block/karen_{g}.json", {"parent": "minecraft:block/block", "textures": {
            "particle": "guhs:block/karen", "kast": "guhs:block/karen", "scherm": f"guhs:block/karen_gezicht_{g}", "voet": "guhs:block/karen_voet",
            "staal": "guhs:block/krokante_grill_zij"}, "elements": [
            el((3, 0, 3), (13, 2, 13), rondom("voet", up="staal", down="staal")),
            el((7, 2, 7), (9, 6, 9), rondom("staal")),
            el((2, 6, 3), (14, 16, 13), rondom("kast", north=("scherm", [0, 0, 16, 16]))),
            el((6.5, 16, 7), (7.5, 18, 8), rondom("staal")), el((8.5, 16, 7), (9.5, 19, 8), rondom("staal"))]})     # two little antennas
    w(f"{A}/blockstates/karen.json", {"variants": {k.replace("facing=", f"gezicht={g},facing="): dict(v, model=f"guhs:block/karen_{g}")
                                                   for g in range(4) for k, v in h.facing_states("karen").items()}})
    w(f"{A}/models/item/karen.json", {"parent": "guhs:block/karen_1"})
    # --- the grill
    for naam, boven in (("krokante_grill", "krokante_grill_boven"), ("krokante_grill_bakt", "krokante_grill_boven_bakt")):
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "textures": {
            "particle": "guhs:block/krokante_grill_zij", "zij": "guhs:block/krokante_grill_zij", "voor": "guhs:block/krokante_grill_voor",
            "boven": f"guhs:block/{boven}"}, "elements": [
            el((0, 0, 0), (16, 12, 16), rondom("zij", north="voor")),
            el((0, 12, 0), (16, 14, 16), rondom("zij", up="boven")),
            el((0, 14, 14), (16, 16, 16), rondom("zij"))]})                                                        # the back splash
    w(f"{A}/blockstates/krokante_grill.json", {"variants": {
        **{k.replace("facing=", "bakt=false,facing="): v for k, v in h.facing_states("krokante_grill").items()},
        **{k.replace("facing=", "bakt=true,facing="): v for k, v in h.facing_states("krokante_grill", "guhs:block/krokante_grill_bakt").items()}}})
    w(f"{A}/models/item/krokante_grill.json", {"parent": "guhs:block/krokante_grill"})
    # --- the order table
    w(f"{A}/models/block/besteltafel.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/besteltafel", "hout": "guhs:block/besteltafel", "kleed": "guhs:block/besteltafel_kleed", "bel": "guhs:block/besteltafel_bel"},
        "elements": [
            el((0, 12, 0), (16, 14, 16), rondom("kleed", down="hout")),
            el((6, 1, 6), (10, 12, 10), rondom("hout")), el((3, 0, 3), (13, 1, 13), rondom("hout")),
            el((6, 14, 6), (10, 16, 10), rondom("bel")), el((7.5, 16, 7.5), (8.5, 16.6, 8.5), rondom("hout"))]})
    w(f"{A}/blockstates/besteltafel.json", {"variants": h.facing_states("besteltafel")})
    w(f"{A}/models/item/besteltafel.json", {"parent": "guhs:block/besteltafel"})
    # --- the fruit house: wall, roof, window
    h.simple_block("vruchthuis_wand")
    w(f"{A}/models/block/vruchthuis_dak.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/vruchthuis_dak_top", "bottom": "guhs:block/vruchthuis_dak", "side": "guhs:block/vruchthuis_dak"}})
    w(f"{A}/blockstates/vruchthuis_dak.json", {"variants": {"": {"model": "guhs:block/vruchthuis_dak"}}})
    w(f"{A}/models/item/vruchthuis_dak.json", {"parent": "guhs:block/vruchthuis_dak"})
    h.simple_block("vruchthuis_raam", render_type="minecraft:translucent")
    # --- the round door: vanilla oak's blockstate and models with our two textures
    tex_ = {"minecraft:block/oak_door_top": "guhs:block/vruchthuis_deur_top", "minecraft:block/oak_door_bottom": "guhs:block/vruchthuis_deur_bottom"}
    with zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar")) as z:
        state = json.dumps(json.loads(z.read("assets/minecraft/blockstates/oak_door.json")))
        w(f"{A}/blockstates/vruchthuis_deur.json", json.loads(state.replace("minecraft:block/oak_door", "guhs:block/vruchthuis_deur")))
        for path in z.namelist():
            if path.startswith("assets/minecraft/models/block/oak_door") and path.endswith(".json"):
                model = json.loads(z.read(path))
                model["textures"] = {k: tex_.get(v, v) for k, v in model.get("textures", {}).items()}
                model["render_type"] = "minecraft:cutout"
                w(f"{A}/models/block/vruchthuis_deur{path[len('assets/minecraft/models/block/oak_door'):-5]}.json", model)
        loot = z.read("data/minecraft/loot_table/blocks/oak_door.json").decode("utf-8")
        w(f"{h.D}/loot_table/blocks/vruchthuis_deur.json", json.loads(loot.replace("minecraft:oak_door", "guhs:vruchthuis_deur")))
    h.item_model("vruchthuis_deur")
    # --- the bubble blower
    w(f"{A}/models/block/bellenblaas.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
        "particle": "guhs:block/bellenblaas", "pot": "guhs:block/bellenblaas", "sop": "guhs:block/bellenblaas_sop", "ring": "guhs:block/bellenblaas_ring"},
        "elements": [
            el((4, 0, 4), (12, 7, 12), rondom("pot", up="sop")),
            {"from": [3, 6, 7.9], "to": [13, 16, 8.1], "rotation": {"origin": [8, 7, 8], "axis": "z", "angle": -22.5},
             "faces": {"north": {"texture": "#ring", "uv": [0, 0, 16, 16]}, "south": {"texture": "#ring", "uv": [16, 0, 0, 16]}}}]})
    w(f"{A}/blockstates/bellenblaas.json", {"variants": h.facing_states("bellenblaas")})
    w(f"{A}/models/item/bellenblaas.json", {"parent": "guhs:block/bellenblaas"})
    # --- the ghost lantern: vanilla's two lantern templates
    w(f"{A}/models/block/spooklantaarn.json", {"parent": "minecraft:block/template_lantern", "render_type": "minecraft:cutout",
                                                "textures": {"lantern": "guhs:block/spooklantaarn"}})
    w(f"{A}/models/block/spooklantaarn_hanging.json", {"parent": "minecraft:block/template_hanging_lantern", "render_type": "minecraft:cutout",
                                                        "textures": {"lantern": "guhs:block/spooklantaarn"}})
    w(f"{A}/blockstates/spooklantaarn.json", {"variants": {"hanging=false": {"model": "guhs:block/spooklantaarn"},
                                                             "hanging=true": {"model": "guhs:block/spooklantaarn_hanging"}}})
    h.item_model("spooklantaarn")
    h.item_model("knabbelburger")
    h.item_model("kwallenjam")


# =====================================================================================================================
# loot, recipes, tags
# =====================================================================================================================
ZELF = ("karen", "krokante_grill", "besteltafel", "vruchthuis_wand", "vruchthuis_dak", "vruchthuis_raam", "bellenblaas", "spooklantaarn")
VRUCHT = "guhs:gepofte_knabbelkoorvrucht"


def data(h):
    for blok in ZELF:
        h.self_drop(blok)
    # the fruit house set: the wall from popped knabbelkoor fruit, the rest from the wall
    h.shaped("vruchthuis_wand", ["FF", "FF"], {"F": VRUCHT}, "guhs:vruchthuis_wand", 4)
    h.shaped("vruchthuis_deur", ["WW", "WW", "WW"], {"W": "guhs:vruchthuis_wand"}, "guhs:vruchthuis_deur", 3)
    h.shaped("vruchthuis_raam", ["WGW"], {"W": "guhs:vruchthuis_wand", "G": "minecraft:glass_pane"}, "guhs:vruchthuis_raam", 2)
    h.shaped("vruchthuis_dak", ["F", "W"], {"W": "guhs:vruchthuis_wand", "F": VRUCHT}, "guhs:vruchthuis_dak", 2)
    for uit in ("vruchthuis_dak", "vruchthuis_raam"):
        h.w(f"{h.D}/recipe/{uit}_van_vruchthuis_wand_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": {"item": "guhs:vruchthuis_wand"},
                                                                         "result": {"id": f"guhs:{uit}", "count": 1}})
    h.shaped("bellenblaas", [" R ", " S ", "ZBZ"], {"R": "minecraft:pink_dye", "S": "minecraft:stick", "Z": "minecraft:slime_ball",
                                                    "B": "minecraft:bowl"}, "guhs:bellenblaas")
    h.shaped("spooklantaarn", ["NDN", "NLN", "NNN"], {"N": "minecraft:iron_nugget", "D": "minecraft:lime_dye", "L": "guhs:lichtstaafje"},
             "guhs:spooklantaarn")
    h.shaped("krokante_grill", ["III", "ICI", "SSS"], {"I": "minecraft:iron_ingot", "C": "minecraft:campfire", "S": "minecraft:smooth_stone"},
             "guhs:krokante_grill")
    h.shaped("besteltafel", ["RNR", "PPP", " S "], {"R": "minecraft:red_carpet", "N": "minecraft:gold_nugget", "P": "#minecraft:planks",
                                                    "S": "minecraft:stick"}, "guhs:besteltafel")
    add = h.add_tag
    add("minecraft/tags/block/mineable/pickaxe", ["guhs:karen", "guhs:krokante_grill", "guhs:spooklantaarn"])
    add("minecraft/tags/block/mineable/axe", ["guhs:besteltafel", "guhs:vruchthuis_wand", "guhs:vruchthuis_raam", "guhs:vruchthuis_deur", "guhs:bellenblaas"])
    add("minecraft/tags/block/mineable/hoe", ["guhs:vruchthuis_dak"])
    add("minecraft/tags/block/doors", ["guhs:vruchthuis_deur"])
    add("minecraft/tags/item/doors", ["guhs:vruchthuis_deur"])


def build(h):
    textures(h)
    models(h)
    data(h)


def check(h):
    problems = []
    for blok in ZELF + ("vruchthuis_deur",):
        for pad in (f"{h.A}/blockstates/{blok}.json", f"{h.D}/loot_table/blocks/{blok}.json", f"{h.A}/models/item/{blok}.json"):
            if not os.path.exists(pad):
                problems.append(f"missing {pad}")
    for item in ("knabbelburger", "kwallenjam"):
        if not os.path.exists(f"{h.A}/textures/item/{item}.png"):
            problems.append(f"missing the icon of {item}")
    for recept in ("vruchthuis_wand", "vruchthuis_dak", "vruchthuis_raam", "vruchthuis_deur", "bellenblaas", "spooklantaarn", "krokante_grill", "besteltafel"):
        if not os.path.exists(f"{h.D}/recipe/{recept}.json"):
            problems.append(f"no recipe for {recept}")
    for geen in ("knabbelburger", "kwallenjam"):                          # (baked on the grill / phase B)
        if os.path.exists(f"{h.D}/recipe/{geen}.json"):
            problems.append(f"{geen} must not have a crafting recipe")
    return problems
