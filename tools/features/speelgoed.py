"""
Het speelgoed (2.10 "Lieve vadsjes van elkaar", slice speelgoed; see guhs_work210/CONTRACT_210.md par. 5.5).

Four toys in GUH style (pink fur, round guh ears, guh eyes and snoet; NOT hamster toys):
  - knabbelbal        a fluffy pink ball with guh ears, a face and a window with a kaasknabbel inside (entity + item)
  - guh_glijbaantje   klimrek with guh ears, a platform on a guh face, a glossy slide (1 x 2 x 3, block model + invisible parts)
  - pluizige_tunnel   pink fur tunnel pieces that join up; every entrance is a guh face with ears (multipart)
  - guh_wip           a plank on a little guh-head stand, cushions and guh-ear handles (the plank swings: extra model)
  - guh_schommel      a pink A-frame with a guh head on the beam, a seat on ropes (the seat swings: extra model)
Plus textures, icons, recipes, loot, sounds, texts, advancements, the game test rooms and the FTB section "Speelgoed".
Models are in local pixels as if the toy faces north (front = low z); the blockstates turn them.
"""
import os
import random

import numpy as np
from PIL import Image, ImageDraw

TOYS = ["guh_glijbaantje", "guh_wip", "guh_schommel", "pluizige_tunnel"]
SPEELTJES = ["knabbelbal", "glijbaantje", "tunnel", "wip_schommel"]
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

VACHT = (246, 168, 200)
VACHT_DONKER = (226, 138, 176)
LICHT = (255, 214, 230)
OOR = (255, 150, 196)
NEUS = (214, 86, 138)
OOG = (52, 26, 52)
HOUT = (238, 196, 160)
HOUT_DONKER = (206, 158, 124)
MINT = (168, 232, 206)
GEEL = (255, 214, 92)


# =====================================================================================================================
# textures
# =====================================================================================================================
def _ruis(size, basis, var, seed, strepen=None, w=None):
    rng = np.random.default_rng(seed)
    w = w or size
    a = np.zeros((size, w, 4), np.uint8)
    base = np.array(basis, float)
    n = rng.normal(0, var, (size, w))
    for c in range(3):
        a[..., c] = np.clip(base[c] + n, 0, 255)
    a[..., 3] = 255
    if strepen:
        kleur, kans = strepen
        for _ in range(int(size * w * kans)):
            x, y = rng.integers(0, w), rng.integers(0, size - 2)
            for d in range(rng.integers(2, 4)):
                if y + d < size:
                    a[y + d, (x + d // 2) % w, :3] = kleur
    return a


def _vacht(seed=4101):
    return Image.fromarray(_ruis(16, VACHT, 7, seed, (VACHT_DONKER, 0.08)))


def _vacht_licht():
    return Image.fromarray(_ruis(16, LICHT, 5, 4102, ((244, 196, 214), 0.05)))


def _pluis():
    """Extra fluffy fur for the tunnel: pink with lighter tufts."""
    a = _ruis(16, (248, 178, 208), 6, 4103, ((255, 214, 232), 0.08))
    return Image.fromarray(a)


def _binnen():
    """The inside of the tunnel: a darker, soft pink."""
    return Image.fromarray(_ruis(16, (214, 128, 166), 6, 4104, ((196, 108, 148), 0.1)))


def _vloer():
    """The tunnel's carpet: soft light pink with little hearts."""
    img = Image.fromarray(_ruis(16, (250, 204, 222), 4, 4105))
    for (x, y) in ((3, 3), (11, 10)):
        for dx, dy in ((0, 0), (2, 0), (0, 1), (1, 1), (2, 1), (1, 2), (-1, 0), (3, 0)):
            if 0 <= x + dx < 16 and 0 <= y + dy < 16 and not (dx in (-1, 3)):
                img.putpixel((x + dx, y + dy), (240, 130, 176, 255))
    return img


def _oor():
    a = _ruis(16, OOR, 4, 4106)
    ys = np.arange(16)[:, None]
    a[..., :3] = np.clip(a[..., :3].astype(int) - (15 - ys) * 1.2, 0, 255).astype(np.uint8)
    return Image.fromarray(a)


def _gezicht():
    """A guh face on light pink fur: two big shiny eyes, a pink snoet with a nose, blush."""
    img = Image.fromarray(_ruis(16, VACHT, 5, 4107))
    d = ImageDraw.Draw(img)
    for x0 in (3, 10):
        d.rectangle([x0, 4, x0 + 2, 7], fill=OOG + (255,))
        img.putpixel((x0, 4), (255, 255, 255, 255))
        img.putpixel((x0 + 1, 7), (120, 80, 120, 255))
    d.rectangle([6, 8, 9, 11], fill=LICHT + (255,))
    d.rectangle([7, 8, 8, 9], fill=NEUS + (255,))
    img.putpixel((7, 11), (170, 70, 110, 255))
    img.putpixel((8, 11), (170, 70, 110, 255))
    for x in (1, 2, 13, 14):
        img.putpixel((x, 9), (250, 120, 166, 255))
    return img


def _tunnel_gezicht():
    """Above a tunnel entrance: the eyes (and blush) of the guh whose mouth is the hole."""
    img = _pluis()
    d = ImageDraw.Draw(img)
    for x0 in (3, 10):
        d.rectangle([x0, 5, x0 + 2, 9], fill=OOG + (255,))
        img.putpixel((x0, 5), (255, 255, 255, 255))
        img.putpixel((x0 + 1, 6), (255, 255, 255, 255))
    for x in (1, 2, 13, 14):
        img.putpixel((x, 11), (250, 120, 166, 255))
    d.rectangle([7, 11, 8, 12], fill=NEUS + (255,))
    return img


def _hout():
    """Warm light wood, soft and rounded (the frames, the platform, the plank)."""
    a = _ruis(16, HOUT, 5, 4108)
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    for y in (0, 5, 10, 15):
        d.line([(0, y), (15, y)], fill=HOUT_DONKER + (255,))
    rng = random.Random(4108)
    for _ in range(5):
        x, y = rng.randrange(16), rng.randrange(16)
        img.putpixel((x, y), (222, 176, 140, 255))
    return img


def _glij():
    """The slide: glossy pink-lilac with white shine stripes along it."""
    a = _ruis(16, (236, 150, 214), 3, 4109)
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    for x in (3, 4, 11):
        d.line([(x, 0), (x, 15)], fill=(255, 226, 246, 255))
    d.line([(0, 0), (0, 15)], fill=(206, 110, 186, 255))
    d.line([(15, 0), (15, 15)], fill=(206, 110, 186, 255))
    return img


def _sport():
    """The klimrek's rungs: pastel mint."""
    a = _ruis(16, MINT, 4, 4110)
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    d.line([(0, 2), (15, 2)], fill=(214, 250, 234, 255))
    return img


def _touw():
    a = _ruis(16, (246, 232, 214), 5, 4111)
    img = Image.fromarray(a)
    for y in range(0, 16, 3):
        for x in range(16):
            if (x + y) % 4 == 0:
                img.putpixel((x, y), (214, 196, 170, 255))
    return img


def _kussen():
    """A pink cushion with a heart."""
    a = _ruis(16, (250, 150, 190), 4, 4112)
    img = Image.fromarray(a)
    heart = [(5, 5), (6, 5), (9, 5), (10, 5), (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (11, 6), (4, 7), (5, 7), (6, 7), (7, 7),
             (8, 7), (9, 7), (10, 7), (11, 7), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (6, 9), (7, 9), (8, 9), (9, 9), (7, 10), (8, 10)]
    for p in heart:
        img.putpixel(p, (255, 236, 244, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(226, 120, 164, 255))
    return img


def _pixels(rows, pal):
    img = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img


PAL = {
    "k": (122, 48, 88, 255),     # outline
    "p": VACHT + (255,), "d": VACHT_DONKER + (255,), "l": LICHT + (255,), "o": OOR + (255,), "e": OOG + (255,), "w": (255, 255, 255, 255),
    "n": NEUS + (255,), "b": (250, 120, 166, 255), "y": GEEL + (255,), "Y": (228, 170, 50, 255), "h": HOUT + (255,), "H": HOUT_DONKER + (255,),
    "m": MINT + (255,), "g": (236, 150, 214, 255), "G": (255, 226, 246, 255), "t": (246, 232, 214, 255), "c": (250, 150, 190, 255),
    "x": (70, 34, 60, 255),
}

ICONS = {
    "knabbelbal": [
        "................",
        "...kk......kk...",
        "..kpok....kopk..",
        "..kpokkkkkkopk..",
        "...kppppppppk...",
        "..kpplppppplpk..",
        ".kppewppppewppk.",
        ".kppeepppeeppbk.",
        ".kbppplnnlpppbk.",
        ".kpppppllppppdk.",
        ".kppkkkyyykkpdk.",
        ".kpkyyYyyyYykdk.",
        "..kpkyyyYyykdk..",
        "...kpkkkkkkdk...",
        "....kkddddkk....",
        "......kkkk......"],
    "guh_glijbaantje": [
        "................",
        "..........k...k.",
        ".........kok.kok",
        ".........kpk.kpk",
        ".........khmmmhk",
        ".........kh...hk",
        "....kkkkkkhkkkhk",
        "...kghhhhhhmmmhk",
        "..kgGkkkkkh...hk",
        ".kgGk.kHk.hmmmhk",
        "kgGk..kHk.h...hk",
        "kGk...kHk.hmmmhk",
        "kk....kHk.h...hk",
        "......kHk.h...hk",
        "......kkk.kk..kk",
        "................"],
    "guh_wip": [
        "................",
        "................",
        "..k.k......k.k..",
        ".kokok....kokok.",
        ".kkhkk....kkhkk.",
        "kccchhhhhhhhccck",
        "kHHHHHHHHHHHHHHk",
        ".kkkkkkkkkkkkkk.",
        ".....kpk.kpk....",
        ".....kpppppk....",
        "....kpewpewpk...",
        "....kpeepeepk...",
        "....kbpnnnpbk...",
        "....kpppppppk...",
        "...kHHHHHHHHHk..",
        "...kkkkkkkkkkk.."],
    "guh_schommel": [
        "...kk......kk...",
        "..kpokkkkkkopk..",
        ".khhhhkpkphhhhk.",
        ".kh.kpewpewpkhk.",
        ".kh..kpnnpk..hk.",
        ".kh...kttk...hk.",
        "kh....t..t....hk",
        "kh....t..t....hk",
        "kh....t..t....hk",
        "kh....t..t....hk",
        "kh...kcccck...hk",
        "kh...khhhhk...hk",
        "kh....kkkk....hk",
        "kh............hk",
        "kk............kk",
        "................"],
    "pluizige_tunnel": [
        "................",
        "..kk........kk..",
        ".kpok......kopk.",
        ".kpokkkkkkkkopk.",
        "kpppppppppppppk.",
        "kpewppppppppewk.",
        "kpeebpppppppeebk",
        "kpxxxxpppppxxxxk",
        "kxxxxxxpppxxxxxk",
        "kxxxxxxpppxxxxxk",
        "kxxxxxxpdpxxxxxk",
        "kxxxxxxpdpxxxxxk",
        "kpxxxxppdpxxxxpk",
        "kddddddddddddddk",
        ".kkkkkkkkkkkkkk.",
        "................"],
}


def textures(h):
    tex = {"vacht": _vacht, "vacht_licht": _vacht_licht, "pluis": _pluis, "binnen": _binnen, "vloer": _vloer, "oor": _oor, "gezicht": _gezicht,
           "tunnel_gezicht": _tunnel_gezicht, "hout": _hout, "glij": _glij, "sport": _sport, "touw": _touw, "kussen": _kussen}
    for name, paint in tex.items():
        h.save(paint(), "block", f"speelgoed_{name}.png")
    for name, rows in ICONS.items():
        h.save(_pixels(rows, PAL), "item", f"{name}.png")
    for vol in (True, False):
        h.save(_bal_textuur(vol), "entity", "knabbelbal.png" if vol else "knabbelbal_leeg.png")


def _bal_textuur(vol):
    """The knabbelbal entity texture (64x64, see client.KnabbelbalRenderer for the box layout)."""
    a = _ruis(64, VACHT, 8, 4120, (VACHT_DONKER, 0.07), w=64)
    img = Image.fromarray(a)
    d = ImageDraw.Draw(img)
    # slab A (0,16) 6x6x9: north face at (9,25) 6x6 = the face; south face at (24,25) 6x6 = the window
    fx, fy = 9, 25
    d.rectangle([fx, fy, fx + 5, fy + 5], fill=LICHT + (255,))
    for x in (fx + 1, fx + 4):
        img.putpixel((x, fy + 1), OOG + (255,))
        img.putpixel((x, fy + 2), OOG + (255,))
    img.putpixel((fx + 1, fy + 1), (255, 255, 255, 255))
    img.putpixel((fx + 4, fy + 1), (255, 255, 255, 255))
    img.putpixel((fx + 2, fy + 3), NEUS + (255,))
    img.putpixel((fx + 3, fy + 3), NEUS + (255,))
    img.putpixel((fx, fy + 3), (250, 120, 166, 255))
    img.putpixel((fx + 5, fy + 3), (250, 120, 166, 255))
    wx, wy = 24, 25
    d.rectangle([wx, wy, wx + 5, wy + 5], fill=(200, 110, 150, 255))
    if vol:
        d.rectangle([wx + 1, wy + 1, wx + 4, wy + 4], fill=GEEL + (255,))
        img.putpixel((wx + 2, wy + 2), (228, 170, 50, 255))
        img.putpixel((wx + 3, wy + 3), (228, 170, 50, 255))
        img.putpixel((wx + 1, wy + 1), (255, 240, 170, 255))
    else:
        d.rectangle([wx + 1, wy + 1, wx + 4, wy + 4], fill=(96, 40, 70, 255))
    # the core (0,0) 8x8x8: a face on its front (8,8) as well, in case you see its edge
    d.rectangle([8, 8, 15, 15], fill=VACHT + (255,))
    # ears (48,0) and (56,0): pink insides at the front (north = (u+2, v+2) 2x2)
    for u in (48, 56):
        d.rectangle([u + 2, 2, u + 3, 3], fill=(255, 118, 176, 255))
    return img


# =====================================================================================================================
# models (model pixels, facing north)
# =====================================================================================================================
def _el(frm, to, tex, faces="all", front=None, rot=None, uv=None):
    names = ["north", "east", "south", "west", "up", "down"] if faces == "all" else faces
    f = {}
    for n in names:
        f[n] = {"uv": list(uv) if uv else [0, 0, 16, 16], "texture": f"#{front if (front and n == 'north') else tex}"}
    e = {"from": [float(v) for v in frm], "to": [float(v) for v in to], "faces": f}
    if rot:
        e["rotation"] = {"origin": [float(v) for v in rot[0]], "axis": rot[1], "angle": rot[2]}
    return e


TEX = {k: f"guhs:block/speelgoed_{k}" for k in ["vacht", "vacht_licht", "pluis", "binnen", "vloer", "oor", "gezicht", "tunnel_gezicht", "hout",
                                                 "glij", "sport", "touw", "kussen"]}


def _model(elements, particle="vacht", display=None):
    m = {"render_type": "minecraft:cutout", "ambientocclusion": False, "textures": dict(TEX, particle=TEX[particle]), "elements": elements}
    if display:
        m["display"] = display
    return m


def _oortje(x0, y0, z0, x1, y1, z1, binnen_z=None, achter=True):
    """A round guh ear (fur: a body and a narrower cap) with its pink inside at the front (and the back)."""
    cap = min(1.5, (y1 - y0) * 0.35)
    out = [_el([x0, y0, z0], [x1, y1 - cap, z1], "vacht"), _el([x0 + 0.7, y1 - cap, z0 + 0.2], [x1 - 0.7, y1, z1 - 0.2], "vacht")]
    bz = z0 - 0.1 if binnen_z is None else binnen_z
    out.append(_el([x0 + 0.8, y0 + 0.8, bz], [x1 - 0.8, y1 - cap, bz + 0.1], "oor", ["north"]))
    if achter:
        out.append(_el([x0 + 0.8, y0 + 0.8, z1], [x1 - 0.8, y1 - cap, z1 + 0.1], "oor", ["south"]))
    return out


def glijbaan_model():
    e = []
    e += [_el([2, 0, 2], [4, 20, 4], "hout"), _el([12, 0, 2], [14, 20, 4], "hout")]                    # posts under the platform
    e.append(_el([1, 4, 0], [15, 20, 2], "vacht"))                                                   # the fur panel behind the slide
    e.append(_el([0.5, 7, 4], [1.5, 19, 18], "vacht", ["west", "up", "north", "south"], uv=(0, 0, 16, 16)))   # two guh faces carry
    e[-1]["faces"]["west"]["texture"] = "#gezicht"                                                  # the platform, one each side
    e.append(_el([14.5, 7, 4], [15.5, 19, 18], "vacht", ["east", "up", "north", "south"]))
    e[-1]["faces"]["east"]["texture"] = "#gezicht"
    e += [_el([2, 0, 21], [4, 30, 24], "hout"), _el([12, 0, 21], [14, 30, 24], "hout")]               # the klimrek's rails
    for y in (4, 9, 14, 19, 24):
        e.append(_el([4, y, 22], [12, y + 1.5, 23.5], "sport", ["north", "south", "up", "down"]))   # its rungs
    e.append(_el([1, 20, 1], [15, 22, 21], "hout"))                                                 # the platform
    e += [_el([1, 22, 2], [2, 27, 20], "vacht_licht"), _el([14, 22, 2], [15, 27, 20], "vacht_licht")]  # railings
    e += _oortje(0.5, 26, 19.5, 5.5, 32, 23.5) + _oortje(10.5, 26, 19.5, 15.5, 32, 23.5)             # guh ears on the klimrek
    rot = ([8, 15, -5], "x", -45)                                                                   # the slide, 45 degrees
    e.append(_el([3, 14, -15], [13, 15.5, 5], "glij", rot=rot))
    e.append(_el([2, 14, -15], [3, 17.5, 5], "glij", rot=rot))
    e.append(_el([13, 14, -15], [14, 17.5, 5], "glij", rot=rot))
    e.append(_el([3, 6.5, -16], [13, 8, -11], "glij"))                                              # the run-out at the bottom
    e += [_el([2, 6.5, -16], [3, 9.5, -11], "glij"), _el([13, 6.5, -16], [14, 9.5, -11], "glij")]
    e.append(_el([7, 0, -9], [9, 11, -7], "hout"))                                                  # a leg under the slide
    e += [_el([7, 0, -15], [9, 6.5, -13], "hout")]
    return _model(e, "vacht", display=_display(0.3))


def wip_voet_model():
    e = [_el([4, 0, 5], [12, 6, 11], "vacht", front="gezicht")]
    e[0]["faces"]["south"]["texture"] = "#gezicht"
    e[0]["faces"]["north"]["uv"] = e[0]["faces"]["south"]["uv"] = [0, 3, 16, 15]
    e.append(_el([6.5, 6, 6.5], [9.5, 8.5, 9.5], "hout"))                                           # the axle post
    e += _oortje(4, 6, 7, 6, 8.4, 9) + _oortje(10, 6, 7, 12, 8.4, 9)                                  # little ears on the stand
    e += [_el([3.5, 0, 4.5], [12.5, 1, 11.5], "hout")]                                              # a base plate
    return _model(e, "vacht", display=_display(0.45))


def wip_plank_model():
    e = [_el([5, 8.5, -12], [11, 10, 28], "hout")]
    e += [_el([5.5, 10, -12], [10.5, 11, -6], "kussen"), _el([5.5, 10, 22], [10.5, 11, 28], "kussen")]
    for z0 in (-5, 20):     # handles: a little bar with two guh ears on top
        e.append(_el([5.5, 10, z0], [10.5, 13, z0 + 1], "hout"))
        e += [_el([5, 13, z0 - 0.25], [7.5, 15.5, z0 + 1.25], "vacht"), _el([8.5, 13, z0 - 0.25], [11, 15.5, z0 + 1.25], "vacht")]
        e += [_el([5.6, 13.6, z0 - 0.35], [6.9, 15, z0 - 0.25], "oor", ["north"]), _el([9.1, 13.6, z0 - 0.35], [10.4, 15, z0 - 0.25], "oor", ["north"])]
    return _model(e, "hout")


def schommel_model():
    e = []
    for x0 in (-14, 28):                         # the two A-frames
        for ang in (22.5, -22.5):
            e.append(_el([x0, 0, 7], [x0 + 2, 30, 9], "hout", rot=([x0 + 1, 28, 8], "x", ang)))
        e.append(_el([x0 - 0.5, 0, 1], [x0 + 2.5, 1, 15], "hout"))                                  # feet
    e.append(_el([-15, 27, 6.5], [31, 30, 9.5], "hout"))                                              # the beam
    e.append(_el([3, 24.5, 5], [13, 31, 11], "vacht", front="gezicht"))                               # a guh head on the beam
    e[-1]["faces"]["south"]["texture"] = "#gezicht"
    e += _oortje(3, 29, 7, 6.5, 32, 9) + _oortje(9.5, 29, 7, 13, 32, 9)
    for x0 in (-15, 28.5):                       # fluffy pompons on the beam ends
        e.append(_el([x0, 29, 6], [x0 + 2.5, 31.5, 10], "pluis"))
    return _model(e, "hout", display=_display(0.3))


def schommel_zitje_model():
    e = [_el([4, 10, 7.5], [5, 28, 8.5], "touw"), _el([11, 10, 7.5], [12, 28, 8.5], "touw")]
    e.append(_el([3, 8, 5], [13, 10, 11], "hout"))
    e.append(_el([3.5, 10, 5.5], [12.5, 11, 10.5], "kussen"))
    return _model(e, "hout")


def _display(scale):
    s = [scale, scale, scale]
    return {
        "gui": {"rotation": [25, 210, 0], "translation": [0, -1, 0], "scale": s},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [scale * 0.6] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, -1, 0], "scale": s},
        "thirdperson_righthand": {"rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": [scale * 0.6] * 3},
        "thirdperson_lefthand": {"rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": [scale * 0.6] * 3},
        "firstperson_righthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [scale * 0.8] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [scale * 0.8] * 3},
    }


def tunnel_dak_model():
    e = [_el([0, 13, 0], [16, 16, 16], "pluis", ["up", "north", "south", "east", "west"]),
         _el([0, 12.99, 0], [16, 13, 16], "binnen", ["down"]),
         _el([0, 0, 0], [16, 0.5, 16], "vloer", ["up"])]
    return _model(e, "pluis")


def tunnel_wand_model():
    e = [_el([0, 0, 0], [16, 13, 1.5], "pluis", ["north", "east", "west"]),
         _el([0, 0, 1.49], [16, 13, 1.5], "binnen", ["south"]),
         _el([0, 10.5, 1.5], [16, 13, 3], "binnen", ["south", "down"])]
    return _model(e, "pluis")


def tunnel_ingang_model():
    e = [_el([0, 0, -1], [3, 13, 1], "pluis"), _el([13, 0, -1], [16, 13, 1], "pluis"),
         _el([0, 10, -1], [16, 16, 1], "pluis", front="tunnel_gezicht"),
         _el([3, 8, -1], [5, 10, 1], "pluis"), _el([11, 8, -1], [13, 10, 1], "pluis")]   # (rounds the hole a bit)
    e += _oortje(1, 16, -1, 5.5, 20, 1.5, binnen_z=-1.1) + _oortje(10.5, 16, -1, 15, 20, 1.5, binnen_z=-1.1)
    e.append(_el([6.5, 9, -1.3], [9.5, 10.5, -1], "gezicht", ["north", "up"]))      # the little nose above the mouth
    return _model(e, "pluis")


def blocks_and_items(h):
    A = h.A
    h.w(f"{A}/models/block/guh_glijbaantje.json", glijbaan_model())
    h.w(f"{A}/models/block/guh_wip.json", wip_voet_model())
    h.w(f"{A}/models/block/guh_wip_plank.json", wip_plank_model())
    h.w(f"{A}/models/block/guh_schommel.json", schommel_model())
    h.w(f"{A}/models/block/guh_schommel_zitje.json", schommel_zitje_model())
    for name in ("guh_glijbaantje", "guh_wip", "guh_schommel"):
        h.w(f"{A}/blockstates/{name}.json", {"variants": {f"facing={f}": ({"model": f"guhs:block/{name}", "y": r} if r else
                                                                         {"model": f"guhs:block/{name}"}) for f, r in ROT.items()}})
        h.w(f"{A}/models/item/{name}.json", {"loader": "neoforge:separate_transforms", "base": {"parent": f"guhs:block/{name}"},
                                             "perspectives": {"gui": {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{name}"}}}})
        h.self_drop(name)
    # the invisible parts
    h.w(f"{A}/models/block/speelgoed_deel.json", {"textures": {"particle": TEX["hout"]}})
    h.w(f"{A}/blockstates/speelgoed_deel.json", {"variants": {"": {"model": "guhs:block/speelgoed_deel"}}})
    # the tunnel: roof + a fur wall on every closed side + a guh-face entrance on every end
    h.w(f"{A}/models/block/pluizige_tunnel_dak.json", tunnel_dak_model())
    h.w(f"{A}/models/block/pluizige_tunnel_wand.json", tunnel_wand_model())
    h.w(f"{A}/models/block/pluizige_tunnel_ingang.json", tunnel_ingang_model())
    parts = [{"apply": {"model": "guhs:block/pluizige_tunnel_dak"}}]
    for side, r in ROT.items():
        for kant, model in (("dicht", "pluizige_tunnel_wand"), ("ingang", "pluizige_tunnel_ingang")):
            apply = {"model": f"guhs:block/{model}"}
            if r:
                apply["y"] = r
            parts.append({"when": {side: kant}, "apply": apply})
    h.w(f"{A}/blockstates/pluizige_tunnel.json", {"multipart": parts})
    h.item_model("pluizige_tunnel")
    h.self_drop("pluizige_tunnel")
    h.item_model("knabbelbal")
    # recipes
    h.shaped("knabbelbal", [" W ", "WKW", " W "], {"W": "minecraft:pink_wool", "K": "guhs:kaas_knabbels"}, "guhs:knabbelbal")
    h.shaped("guh_glijbaantje", ["LWW", "LPS", "L S"], {"L": "minecraft:ladder", "W": "minecraft:pink_wool", "P": "#minecraft:planks",
                                                        "S": "#minecraft:wooden_slabs"}, "guhs:guh_glijbaantje")
    h.shaped("pluizige_tunnel", ["WWW", "W W"], {"W": "minecraft:pink_wool"}, "guhs:pluizige_tunnel", 4)
    h.shaped("guh_wip", ["WSW", " F "], {"W": "minecraft:pink_wool", "S": "#minecraft:wooden_slabs", "F": "#minecraft:wooden_fences"}, "guhs:guh_wip")
    h.shaped("guh_schommel", ["FWF", "S S", "FCF"], {"F": "#minecraft:wooden_fences", "W": "minecraft:pink_wool", "S": "minecraft:string",
                                                     "C": "minecraft:pink_carpet"}, "guhs:guh_schommel")
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:guh_glijbaantje", "guhs:guh_wip", "guhs:guh_schommel", "guhs:speelgoed_deel"])


# =====================================================================================================================
# sounds, advancements, texts
# =====================================================================================================================
SOUNDS = {
    "speelgoed.wieee": [{"name": "guhs:guh_ambient5", "pitch": 1.5}, {"name": "guhs:guh_ambient9", "pitch": 1.6},
                        {"name": "guhs:guh_ambient12", "pitch": 1.45}],
    "speelgoed.duw": [{"name": "minecraft:block.wool.place", "type": "event", "pitch": 0.9},
                      {"name": "minecraft:entity.item_frame.rotate_item", "type": "event", "pitch": 0.8}],
    "speelgoed.bal": [{"name": "minecraft:block.wool.step", "type": "event", "pitch": 1.3},
                      {"name": "minecraft:entity.slime.jump_small", "type": "event", "pitch": 1.7, "volume": 0.5}],
    "speelgoed.plop": [{"name": "minecraft:entity.chicken.egg", "type": "event", "pitch": 1.4},
                       {"name": "minecraft:block.bubble_column.bubble_pop", "type": "event", "pitch": 1.2}],
    "speelgoed.gevonden": [{"name": "guhs:guh_ambient3", "pitch": 1.45}, {"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 1.5}],
    "speelgoed.klim": [{"name": "minecraft:block.ladder.step", "type": "event", "pitch": 1.4, "volume": 0.6}],
}
IMPOSSIBLE = {"done": {"trigger": "minecraft:impossible"}}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def advancements(h):
    from features import band
    band.visible(h, "speelgoed_eerste", "root", "guhs:knabbelbal", "task", "Speelkwartiertje!",
                 "Laat een guh met speelgoed spelen: een knabbelbal, een glijbaantje, een tunnel, een wip of een schommel")
    band.visible(h, "speelgoed_alle", "speelgoed_eerste", "guhs:guh_schommel", "goal", "Speeltuin Vahoeg",
                 "Je guhs hebben met alle vier soorten speelgoed gespeeld")
    band.visible(h, "speelgoed_schop", "speelgoed_eerste", "guhs:knabbelbal", "task", "Balletje trappen",
                 "Schop een knabbelbal (linksklik). Wie haalt hem? Njeg!")
    band.visible(h, "speelgoed_duw", "speelgoed_eerste", "guhs:guh_schommel", "task", "Hoger, hoger!",
                 "Geef een guh op de schommel of de wip een duwtje")
    band.visible(h, "speelgoed_verstop", "speelgoed_eerste", "guhs:pluizige_tunnel", "task", "Gevonden!",
                 "Klop op een pluizige tunnel waar een guh zich in verstopt")
    band.visible(h, "speelgoed_zelf", "speelgoed_eerste", "guhs:guh_glijbaantje", "task", "Ook een beetje guh",
                 "Ga zelf van het guh-glijbaantje, of schommel of wip zelf mee (sluip + rechtsklik)")


TEXTS = {
    # blocks, items, entities
    "block.guhs.guh_glijbaantje": "Guh-glijbaantje",
    "block.guhs.guh_glijbaantje.lore": "Een klimrek met guhoortjes, een platform op een guhsnoet en een glimmende glijbaan. Wieee!",
    "block.guhs.guh_glijbaantje.uitleg": "Guhs klimmen erop en glijden eraf. Rechtsklik om zelf te glijden.",
    "block.guhs.guh_wip": "Guh-wip",
    "block.guhs.guh_wip.lore": "Een wip op een klein guhhoofdje, met kussentjes en guhoor-handvatjes.",
    "block.guhs.guh_wip.uitleg": "Twee guhs wippen samen op en neer. Rechtsklik: duwtje. Sluip + rechtsklik: zelf wippen.",
    "block.guhs.guh_schommel": "Guh-schommel",
    "block.guhs.guh_schommel.lore": "Een roze schommel met een guhhoofd op de balk en een zacht kussentje.",
    "block.guhs.guh_schommel.uitleg": "Een guh schommelt er zelf op. Rechtsklik: duwen, hoger! Sluip + rechtsklik: zelf schommelen.",
    "block.guhs.pluizige_tunnel": "Pluizige tunnel",
    "block.guhs.pluizige_tunnel.lore": "Zachte roze vachttunnel. Leg de stukjes aan elkaar voor een kronkeltunnel!",
    "block.guhs.pluizige_tunnel.uitleg": "Guhs rennen erdoor en verstoppen zich erin. Klop erop (rechtsklik) om ze te vinden.",
    "block.guhs.speelgoed_deel": "Speelgoed",
    "item.guhs.knabbelbal": "Knabbelbal",
    "item.guhs.knabbelbal.vol": "Met een kaasknabbel erin",
    "item.guhs.knabbelbal.leeg": "Leeg: stop er een kaasknabbel in (rechtsklik)",
    "item.guhs.knabbelbal.uitleg": "Leg hem neer: guhs duwen hem rond tot de knabbel eruit rolt. Linksklik om te schoppen!",
    "entity.guhs.knabbelbal": "Knabbelbal",
    "entity.guhs.speelgoed_zitje": "Speelgoed",
    # messages
    "gui.guhs.speelgoed.glijbaan.bezet": "Er glijdt al iemand, njeg! Even op je beurt wachten.",
    "gui.guhs.speelgoed.glijbaan.klim": "Klimmen, klimmen... en dan: wieee!",
    "gui.guhs.speelgoed.vol": "Alle plekjes zijn bezet!",
    "gui.guhs.speelgoed.zit": "Lekker zitten! (Sluip om eraf te gaan)",
    "gui.guhs.speelgoed.duw": "Duw!",
    "gui.guhs.speelgoed.duw.guh": "Duw! Hoger, hoger! VAHOEG!",
    "gui.guhs.speelgoed.tunnel.leeg": "Klop klop... er zit niemand in. Nog niet, njeg.",
    "gui.guhs.speelgoed.tunnel.gevonden": "Gevonden! %s giechelt en komt de tunnel uit.",
    "gui.guhs.speelgoed.tunnel.muisje": "%s heeft %s gevonden in de tunnel! Piep piep!",
    "gui.guhs.speelgoed.bal.gevuld": "Er zit weer een kaasknabbel in de knabbelbal. Wie gaat hem eruit duwen?",
    # sounds
    "subtitles.guhs.speelgoed.wieee": "Guh: wieee!",
    "subtitles.guhs.speelgoed.duw": "Duwtje",
    "subtitles.guhs.speelgoed.bal": "Knabbelbal rolt",
    "subtitles.guhs.speelgoed.plop": "Kaasknabbel: plop!",
    "subtitles.guhs.speelgoed.gevonden": "Gevonden! Guh giechelt",
    "subtitles.guhs.speelgoed.klim": "Guh klimt",
    # the dagboekje: wist-je-datjes the guh writes
    "gui.guhs.wistjedat.speelgoed.eerste_knabbelbal": "Vandaag heb ik voor het eerst een knabbelbal rondgeduwd tot de knabbel eruit rolde. Njam! Ik ben een balkampioen, njeg.",
    "gui.guhs.wistjedat.speelgoed.eerste_glijbaantje": "Vandaag ben ik voor het eerst van het glijbaantje gegaan. Wieee! Mijn oortjes wapperden helemaal.",
    "gui.guhs.wistjedat.speelgoed.eerste_tunnel": "Vandaag heb ik me voor het eerst verstopt in de pluizige tunnel. Niemand zag me. Nou ja, bijna niemand.",
    "gui.guhs.wistjedat.speelgoed.eerste_wip_schommel": "Vandaag heb ik voor het eerst geschommeld. Zo hoog! Ik was zo VAHOEG, njeg.",
    "gui.guhs.wistjedat.speelgoed.knabbelbal_1": "Ik heb de knabbelbal wel tien keer met mijn snoet geduwd. Hij rolt zo lekker!",
    "gui.guhs.wistjedat.speelgoed.knabbelbal_2": "Er zat een kaasknabbel in de bal. Nu zit hij in mijn buikje. Vadsig!",
    "gui.guhs.wistjedat.speelgoed.glijbaantje_1": "Ik ben drie keer achter elkaar van de glijbaan gegaan. Nog een keer? Nog een keer!",
    "gui.guhs.wistjedat.speelgoed.glijbaantje_2": "Het klimrek heeft oortjes, net als ik. Daarom vind ik hem zo lief.",
    "gui.guhs.wistjedat.speelgoed.tunnel_1": "In de tunnel is het zacht en donker. Het perfecte verstopplekje, njeg.",
    "gui.guhs.wistjedat.speelgoed.tunnel_2": "Ik moest heel hard giechelen toen ik gevonden werd. Dat verraadde me!",
    "gui.guhs.wistjedat.speelgoed.wip_schommel_1": "Op de wip ging ik op en neer, op en neer. Mijn buikje kriebelde ervan.",
    "gui.guhs.wistjedat.speelgoed.wip_schommel_2": "Iemand gaf me een duwtje op de schommel. Ik vloog bijna tot de wolken!",
}


def texts(h):
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)


# =====================================================================================================================
# the game test rooms, the self-check
# =====================================================================================================================
def test_templates(h):
    t = h.Structure((20, 8, 20))
    for x in range(20):
        for z in range(20):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("speelgoed_test_tuin")
    # a walled play room (so a rolling ball stays in)
    t = h.Structure((14, 6, 14))
    for x in range(14):
        for z in range(14):
            t.set(x, 0, z, "minecraft:smooth_stone")
            if x in (0, 13) or z in (0, 13):
                t.set(x, 1, z, "minecraft:pink_wool")
                t.set(x, 2, z, "minecraft:pink_wool")
    t.save("speelgoed_test_speelkamer")


def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    for name in TOYS:
        for p in (f"{A}/blockstates/{name}.json", f"{A}/models/item/{name}.json", f"{D}/loot_table/blocks/{name}.json", f"{D}/recipe/{name}.json",
                  os.path.join(h.TEX, "item", f"{name}.png")):
            if not os.path.exists(p):
                missing.append(p)
        if f"block.guhs.{name}" not in h.NL:
            missing.append(f"lang block.guhs.{name}")
    for p in (f"{A}/models/block/guh_wip_plank.json", f"{A}/models/block/guh_schommel_zitje.json", os.path.join(h.TEX, "entity", "knabbelbal.png"),
              os.path.join(h.TEX, "entity", "knabbelbal_leeg.png"), f"{A}/models/item/knabbelbal.json", f"{D}/recipe/knabbelbal.json"):
        if not os.path.exists(p):
            missing.append(p)
    for model in (glijbaan_model(), wip_voet_model(), wip_plank_model(), schommel_model(), schommel_zitje_model(), tunnel_dak_model(),
                  tunnel_wand_model(), tunnel_ingang_model()):
        for e in model["elements"]:
            if min(e["from"]) < -16 or max(e["to"]) > 32:
                missing.append(f"element out of range: {e['from']} {e['to']}")
            if "rotation" in e and e["rotation"]["angle"] not in (-45, -22.5, 0, 22.5, 45):
                missing.append(f"bad rotation {e['rotation']}")
    for s in SPEELTJES:
        for k in (f"gui.guhs.wistjedat.speelgoed.eerste_{s}", f"gui.guhs.wistjedat.speelgoed.{s}_1", f"gui.guhs.wistjedat.speelgoed.{s}_2"):
            if k not in h.NL:
                missing.append(k)
    if missing:
        raise SystemExit(f"speelgoed assets missing: {missing}")


def build(h):
    textures(h)
    blocks_and_items(h)
    sounds(h)
    advancements(h)
    texts(h)
    test_templates(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (section "Speelgoed")
# =====================================================================================================================
def ftb(fq):
    q, item, adv = fq.q, fq.item, fq.adv
    q("speelgoed_knabbelbal", "De knabbelbal", "Maak een &dknabbelbal&r: roze wol met een kaasknabbel erin. Leg hem neer en kijk maar: je guhs "
      "duwen hem met hun snoet rond tot de knabbel eruit rolt... en dan: smikkelen! Leeg? Rechtsklik met een kaasknabbel om hem weer te vullen.",
      "guhs:knabbelbal", [item("guhs:knabbelbal")], rewards=(("guhs:kaas_knabbels", 8),), shape="circle")
    q("speelgoed_schop", "Balletje trappen", "Schop de knabbelbal weg met een &dlinksklik&r. Je guhs in de buurt rennen er meteen achteraan. "
      "Wie het eerst de knabbel eruit duwt!", "guhs:knabbelbal", [adv("guhs:lieve_vadsjes/speelgoed_schop")], rewards=(("guhs:kaas_knabbels", 6),))
    q("speelgoed_glijbaantje", "Wieee!", "Bouw een &dguh-glijbaantje&r (een klimrek met guhoortjes en een glimmende glijbaan). Guhs klimmen "
      "erop, glijden eraf en rennen terug voor nog een keer. Rechtsklik om zelf te glijden!",
      "guhs:guh_glijbaantje", [item("guhs:guh_glijbaantje")], rewards=(("guhs:kaas_knabbels", 12),))
    q("speelgoed_tunnel", "Pluizige tunnel", "Maak &dpluizige tunnels&r en leg ze aan elkaar: rechte stukken, bochten, kruisingen. Elke ingang "
      "is een guhsnoet met oortjes. Guhs rennen erdoorheen en verstoppen zich erin!", "guhs:pluizige_tunnel", [item("guhs:pluizige_tunnel", 4)],
      rewards=(("guhs:kaas_knabbels", 8),))
    q("speelgoed_verstop", "Verstoppertje", "Zit er een guh verstopt in de tunnel (je hoort hem giechelen)? &dKlop&r erop met een rechtsklik: "
      "gevonden! Een tam muisje van jou zoekt ook mee, piep piep.", "guhs:pluizige_tunnel", [adv("guhs:lieve_vadsjes/speelgoed_verstop")],
      rewards=(("guhs:kaas_knabbels", 8),))
    q("speelgoed_wip", "Op en neer", "Bouw een &dguh-wip&r. Twee guhs samen wippen het leukst; eentje in zijn eentje wipt een beetje. "
      "Sluip en rechtsklik om zelf mee te wippen.", "guhs:guh_wip", [item("guhs:guh_wip")], rewards=(("guhs:kaas_knabbels", 10),))
    q("speelgoed_schommel", "Schommelen", "Bouw een &dguh-schommel&r, met een guhhoofd op de balk. Een guh schommelt er vanzelf op.",
      "guhs:guh_schommel", [item("guhs:guh_schommel")], rewards=(("guhs:kaas_knabbels", 10),))
    q("speelgoed_duw", "Hoger, hoger!", "Zit er een guh op de schommel of de wip? Geef hem een &dduwtje&r (rechtsklik): hoger! Je eigen guh "
      "krijgt er hartjes van.", "guhs:guh_schommel", [adv("guhs:lieve_vadsjes/speelgoed_duw")], rewards=(("guhs:marshmallow_knabbel", 2),))
    q("speelgoed_eerste", "Speelkwartiertje!", "Zet speelgoed neer bij je guhs. Is er een speler in de buurt, dan gaan ze vanzelf spelen; bewoners "
      "van een &dGuhhuisje&r spelen ook met speelgoed rond hun huisje. Spelen geeft &dhartjes&r!", "guhs:knabbelbal",
      [adv("guhs:lieve_vadsjes/speelgoed_eerste")], rewards=(("guhs:kaas_knabbels", 8),), xp=50)
    q("speelgoed_zelf", "Ook een beetje guh", "Ga zelf van het glijbaantje, of sluip en rechtsklik op de schommel of wip. Wieee!",
      "guhs:guh_glijbaantje", [adv("guhs:lieve_vadsjes/speelgoed_zelf")], rewards=(("guhs:kaas_knabbels", 6),))
    q("speelgoed_alle", "Speeltuin Vahoeg", "Laat je guhs met &dalle vier&r soorten speelgoed spelen: de knabbelbal, het glijbaantje, de tunnel "
      "en de wip of schommel. Misschien ontdek je zo ook hun &dfavoriete speeltje&r!", "guhs:guh_schommel",
      [adv("guhs:lieve_vadsjes/speelgoed_alle")], rewards=(("guhs:gefrituurde_kaasknabbels", 3),), shape="gear", xp=200)
