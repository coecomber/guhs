"""
Guh-parkour: the textures and block models of the posts, the six obstacles and the scorebord (helper of guhpixel_parkour.py).

Everything is in model pixels as if the block faces north (its front at low z, a guh runs in at the front and out at the
back); the blockstates turn the models. The three long obstacles (kruiptunnel, slalompaaltjes, evenwichtsbalk) span
z -16..32: their controller block is the middle one (Java: Obstakel, the same boxes as the collision shapes).
Fur, wood, ears and faces are the speelgoed textures (guhs:block/speelgoed_*); own textures are guhparkour_*.
"""
import numpy as np
from PIL import Image, ImageDraw

ROZE = (246, 168, 200)
ROZE_DONKER = (226, 138, 176)
WIT = (255, 244, 248)
MINT = (168, 232, 206)
MINT_DONKER = (128, 200, 172)
GEEL = (255, 214, 92)
GEEL_DONKER = (228, 170, 50)
BORD = (62, 34, 54)
GROEN = (126, 214, 134)

EIGEN = ["streep", "streep_mint", "vlag_start", "vlag_finish", "bord", "doek", "veer", "knabbels", "mat"]
SPEELGOED = ["vacht", "vacht_licht", "pluis", "oor", "gezicht", "hout", "kussen", "sport"]
TEX = {**{k: f"guhs:block/guhparkour_{k}" for k in EIGEN}, **{k: f"guhs:block/speelgoed_{k}" for k in SPEELGOED}}


# =====================================================================================================================
# textures (16 x 16, fixed seeds)
# =====================================================================================================================
def _ruis(basis, var, seed):
    rng = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.uint8)
    n = rng.normal(0, var, (16, 16))
    for c in range(3):
        a[..., c] = np.clip(basis[c] + n, 0, 255)
    a[..., 3] = 255
    return Image.fromarray(a)


def _strepen(a, b, seed):
    """Candy stripes going round a pole: two colours in slanted bands."""
    img = _ruis(a, 3, seed)
    ruis_b = _ruis(b, 3, seed + 1)
    for y in range(16):
        for x in range(16):
            if ((x + y) // 4) % 2:
                img.putpixel((x, y), ruis_b.getpixel((x, y)))
    return img


def _vlag_start():
    """A green flag with a white guh head on it (two round ears, eyes): go!"""
    img = _ruis(GROEN, 4, 5201)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 0], fill=(92, 176, 104, 255))
    d.rectangle([0, 15, 15, 15], fill=(92, 176, 104, 255))
    d.rectangle([4, 6, 11, 12], fill=WIT + (255,))
    d.rectangle([3, 3, 5, 6], fill=WIT + (255,))
    d.rectangle([10, 3, 12, 6], fill=WIT + (255,))
    for x in (5, 9):
        d.rectangle([x, 8, x + 1, 9], fill=(52, 26, 52, 255))
    d.rectangle([7, 10, 8, 11], fill=(214, 86, 138, 255))
    return img


def _vlag_finish():
    """A finish flag in guh colours: pink and white checks."""
    img = Image.new("RGBA", (16, 16))
    a, b = _ruis(ROZE, 3, 5202), _ruis(WIT, 2, 5203)
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), (a if ((x // 4) + (y // 4)) % 2 else b).getpixel((x, y)))
    return img


def _bord():
    """The dark face of the scorebord, with a thin pink line round it."""
    img = _ruis(BORD, 3, 5204)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=ROZE_DONKER + (255,))
    return img


def _doek():
    """The cloth of the kruiptunnel: mint with darker seams and little stitches."""
    img = _ruis(MINT, 4, 5205)
    d = ImageDraw.Draw(img)
    for x in (0, 8):
        d.line([(x, 0), (x, 15)], fill=MINT_DONKER + (255,))
    for y in range(1, 16, 4):
        for x in (4, 12):
            img.putpixel((x, y), (236, 255, 246, 255))
    return img


def _veer():
    """The spring under the springplank: grey coils (cutout between them)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(0, 16, 4):
        d.rectangle([1, y, 14, y + 1], fill=(176, 176, 190, 255))
        d.rectangle([1, y, 14, y], fill=(226, 226, 236, 255))
        d.line([(13, y + 2), (2, y + 3)], fill=(132, 132, 150, 255))
    return img


def _knabbels():
    """A bowl full of kaasknabbels seen from above."""
    img = _ruis((250, 226, 236), 3, 5206)
    rng = np.random.default_rng(5207)
    d = ImageDraw.Draw(img)
    d.ellipse([1, 1, 14, 14], fill=(255, 250, 252, 255), outline=ROZE_DONKER + (255,))
    for _ in range(22):
        x, y = int(rng.integers(3, 12)), int(rng.integers(3, 12))
        d.rectangle([x, y, x + 1, y + 1], fill=(GEEL if rng.random() > 0.35 else GEEL_DONKER) + (255,))
    return img


def _mat():
    """The pad on the springplank: pink with a white arrow pointing up the ramp (to the back = the bottom of the texture)."""
    img = _ruis(ROZE, 4, 5208)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=ROZE_DONKER + (255,))
    d.rectangle([7, 3, 8, 10], fill=WIT + (255,))
    for i in range(4):
        d.line([(7 - i, 10 - i + 3), (8 + i, 10 - i + 3)], fill=WIT + (255,))
    return img


def textures(h):
    paint = {"streep": lambda: _strepen(ROZE, WIT, 5210), "streep_mint": lambda: _strepen(MINT, WIT, 5212), "vlag_start": _vlag_start,
             "vlag_finish": _vlag_finish, "bord": _bord, "doek": _doek, "veer": _veer, "knabbels": _knabbels, "mat": _mat}
    for name in EIGEN:
        h.save(paint[name](), "block", f"guhparkour_{name}.png")


# =====================================================================================================================
# models
# =====================================================================================================================
def _el(frm, to, tex, faces="all", rot=None, uv=None, **anders):
    """One box; anders: another texture for single faces (north="gezicht")."""
    names = ["north", "east", "south", "west", "up", "down"] if faces == "all" else faces
    f = {n: {"uv": list(uv) if uv else [0, 0, 16, 16], "texture": f"#{anders.get(n, tex)}"} for n in names}
    e = {"from": [float(v) for v in frm], "to": [float(v) for v in to], "faces": f}
    if rot:
        e["rotation"] = {"origin": [float(v) for v in rot[0]], "axis": rot[1], "angle": rot[2]}
    return e


def _oortje(x0, y0, z0, x1, y1, z1):
    """A round guh ear: a fur body with a narrower cap, pink inside at the front and the back."""
    cap = min(1.5, (y1 - y0) * 0.35)
    return [_el([x0, y0, z0], [x1, y1 - cap, z1], "vacht"), _el([x0 + 0.6, y1 - cap, z0 + 0.2], [x1 - 0.6, y1, z1 - 0.2], "vacht"),
            _el([x0 + 0.7, y0 + 0.6, z0 - 0.1], [x1 - 0.7, y1 - cap, z0], "oor", ["north"]),
            _el([x0 + 0.7, y0 + 0.6, z1], [x1 - 0.7, y1 - cap, z1 + 0.1], "oor", ["south"])]


def _display(scale, omhoog=0.0):
    s = [scale, scale, scale]
    k = [scale * 0.6] * 3
    return {
        "gui": {"rotation": [25, 210, 0], "translation": [0, omhoog, 0], "scale": s},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": k},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, omhoog, 0], "scale": s},
        "thirdperson_righthand": {"rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": k},
        "thirdperson_lefthand": {"rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": k},
        "firstperson_righthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [scale * 0.8] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [scale * 0.8] * 3},
    }


def _model(elements, particle, display):
    return {"render_type": "minecraft:cutout", "ambientocclusion": False, "textures": dict(TEX, particle=TEX[particle]),
            "elements": elements, "display": display}


def _paal(vlag):
    """A post: a wooden foot, a candy-striped pole with two guh ears on top and a flag to the right."""
    e = [_el([5, 0, 5], [11, 2, 11], "hout"), _el([6.5, 2, 6.5], [9.5, 16, 9.5], "streep")]
    e += _oortje(5.5, 16, 7, 7.75, 19, 9) + _oortje(8.25, 16, 7, 10.5, 19, 9)
    e.append(_el([9.5, 9.5, 7.6], [15.5, 15, 8.4], vlag))
    return e


def startpaaltje():
    return _model(_paal("vlag_start"), "streep", _display(0.62, -1.5))


def finishpaaltje():
    e = _paal("vlag_finish")
    e.append(_el([9.5, 4, 7.6], [15.5, 9.5, 8.4], "vlag_finish"))      # (a longer flag: the finish stands out)
    return _model(e, "streep", _display(0.62, -1.5))


def horde():
    e = [_el([0.5, 0, 5.5], [3.5, 1, 10.5], "hout"), _el([12.5, 0, 5.5], [15.5, 1, 10.5], "hout"),      # feet
         _el([1, 1, 7], [3, 9, 9], "streep"), _el([13, 1, 7], [15, 9, 9], "streep"),                    # posts
         _el([3, 6, 7.5], [13, 8, 8.5], "streep_mint")]                                                 # the bar
    e += _oortje(0.75, 9, 7.25, 3.25, 11.5, 8.75) + _oortje(12.75, 9, 7.25, 15.25, 11.5, 8.75)
    return _model(e, "streep", _display(0.72))


def springplank():
    e = [_el([2, 0, 0], [14, 1, 16], "hout"),                                                           # the frame on the ground
         _el([2, 1, 0], [14, 2.5, 17], "hout", rot=([8, 1, 0], "x", -22.5), up="mat"),                  # the board, up to the back
         _el([5.5, 1, 10.5], [10.5, 6, 14.5], "veer", ["north", "south", "east", "west"]),              # the spring
         _el([3, 1, 13.5], [4.5, 7, 15], "hout"), _el([11.5, 1, 13.5], [13, 7, 15], "hout")]            # two little posts
    e += _oortje(2.25, 7, 13.25, 5.25, 10, 15.25) + _oortje(10.75, 7, 13.25, 13.75, 10, 15.25)
    return _model(e, "mat", _display(0.6))


LANG = (-16, 32)       # (z of a three-block obstacle)


def kruiptunnel():
    z0, z1 = LANG
    e = [_el([1, 0, z0], [2.5, 9, z1], "doek", ["east", "west", "north", "south"]),
         _el([13.5, 0, z0], [15, 9, z1], "doek", ["east", "west", "north", "south"]),
         _el([1, 9, z0], [15, 10.5, z1], "doek", ["up", "down", "north", "south", "east", "west"])]
    for z in (z0, 7.5, z1 - 1):       # three hoops
        e += [_el([0.5, 0, z], [1.5, 11, z + 1], "streep"), _el([14.5, 0, z], [15.5, 11, z + 1], "streep"),
              _el([0.5, 10.5, z], [15.5, 11.5, z + 1], "streep")]
    for z in (z0 + 0.25, z1 - 2.25):  # guh ears over both holes
        e += _oortje(2, 11.5, z, 5.5, 15, z + 2) + _oortje(10.5, 11.5, z, 14, 15, z + 2)
    return _model(e, "doek", _display(0.4))


def kruiptunnel_bobbel(n):
    """The bump of the guh inside, at place n (1..5 from the front to the back)."""
    zc = LANG[0] + (n - 0.5) * 9.6
    e = [_el([2.5, 10.5, zc - 4.5], [13.5, 13, zc + 4.5], "doek", ["up", "north", "south", "east", "west"]),
         _el([4, 13, zc - 3.5], [12, 14.5, zc + 3.5], "doek", ["up", "north", "south", "east", "west"])]
    return {"render_type": "minecraft:cutout", "ambientocclusion": False, "textures": {"doek": TEX["doek"], "particle": TEX["doek"]}, "elements": e}


def slalompaaltjes():
    e = [_el([7.4, 0, LANG[0] + 2], [8.6, 0.5, LANG[1] - 2], "hout")]                                   # a lath that ties them together
    for i, zc in enumerate((-8, 8, 24)):
        e += [_el([5, 0, zc - 3], [11, 1, zc + 3], "hout"),
              _el([6.5, 1, zc - 1.5], [9.5, 15, zc + 1.5], "streep" if i % 2 == 0 else "streep_mint"),
              _el([6, 15, zc - 2], [10, 17.5, zc + 2], "pluis")]                                        # a pompon on top
    return _model(e, "streep", _display(0.42))


def evenwichtsbalk():
    e = [_el([6, 6, LANG[0]], [10, 8, LANG[1]], "hout")]
    for z0, voor in ((-14, "north"), (27, "south")):
        e.append(_el([5, 0, z0], [11, 6, z0 + 3], "vacht", **{voor: "gezicht"}))                        # a guh head carries each end
        e += _oortje(4.5, 6, z0 + 0.5, 6, 8.5, z0 + 2.5) + _oortje(10, 6, z0 + 0.5, 11.5, 8.5, z0 + 2.5)
    return _model(e, "hout", _display(0.42))


def knabbeltafeltje():
    e = [_el([x, 0, z], [x + 1.5, 5, z + 1.5], "hout") for x in (3.5, 11) for z in (3.5, 11)]
    e.append(_el([3, 5, 3], [13, 7, 13], "hout", up="kussen"))                                          # the top with its cloth
    e.append(_el([5, 7, 5], [11, 9, 11], "vacht_licht", up="knabbels"))                                 # the bowl
    e.append(_el([5.5, 9, 5.5], [10.5, 10, 10.5], "knabbels", ["up", "north", "south", "east", "west"]))  # heaped full
    return _model(e, "hout", _display(0.72))


def scorebord():
    e = [_el([-3, 0, 7.5], [-1, 6, 8.5], "hout"), _el([17, 0, 7.5], [19, 6, 8.5], "hout"),              # legs
         _el([-4.5, 0, 6], [0.5, 1, 10], "hout"), _el([15.5, 0, 6], [20.5, 1, 10], "hout"),             # feet
         _el([-5, 6, 7], [21, 23, 9], "hout", north="bord"),                                            # the board, its dark face in front
         _el([-6, 5, 6.75], [22, 6, 9.25], "streep"), _el([-6, 23, 6.75], [22, 24, 9.25], "streep")]    # a striped frame above and below
    e += _oortje(-4, 24, 7, 0.5, 27.5, 9) + _oortje(15.5, 24, 7, 20, 27.5, 9)
    return _model(e, "bord", _display(0.36, -3))


MODELLEN = {"guhparkour_startpaaltje": startpaaltje, "guhparkour_finishpaaltje": finishpaaltje, "guhparkour_horde": horde,
            "guhparkour_springplank": springplank, "guhparkour_kruiptunnel": kruiptunnel, "guhparkour_slalompaaltjes": slalompaaltjes,
            "guhparkour_evenwichtsbalk": evenwichtsbalk, "guhparkour_knabbeltafeltje": knabbeltafeltje, "guhparkour_scorebord": scorebord}
