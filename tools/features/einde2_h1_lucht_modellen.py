"""
einde2 phase B, slice h1_lucht: the looks of the chapter.

  kaasbol          geckolib/models/entity/kaasbol.geo.json (+ animation, textures/entity/kaasbol.png and kaasbol_gloed.png):
                   a round cheese ball (28 px across) of two halves; the top half (bone "boven") is hinged at the back and
                   tips open in the animation "open"; inside, a glowing core (bone "kern"). Animations: idle (bobbing,
                   breathing glow), open, dicht.
  einde2_h1_lucht_luchtbal   the ball of the luchtbal match: a swirl of air (10 px), always glowing a little; animation draai.
  luchtscooter     the toy block: a spinning ball of air on a little wooden base (block model + textures).
  the clothes      the Luchtnomadengewaad (orange and yellow robe; the blue arrow on the head: bone outfit_luchtnomade_pijl)
  the pictures     the narrator card of the chapter and the travel map of the whole story (verhaal_motor.Kaart).

The geo files sample a few painted regions of their texture (many cubes share one region), so a round shape of many cubes
does not need an atlas of its own per face.
"""
import math

import numpy as np
from PIL import Image

import features.einde2_kit as kit

KAAS = (246, 196, 72)
KAAS_DONKER = (214, 150, 46)
KORST = (226, 146, 50)
GAT = (190, 120, 36)
KERN = (214, 246, 255)
WIND = (226, 240, 255)
WIND_BLAUW = (140, 196, 240)
TEX = 128


# =====================================================================================================================
# painting helpers
# =====================================================================================================================
def _vlak(rng, w, h, kleur, var=10):
    a = np.zeros((h, w, 4), np.float32)
    a[..., :3] = kleur
    a[..., :3] += rng.normal(0, var / 2, (h, w, 1))
    a[..., 3] = 255
    return a


def _gat(a, cx, cy, r, kleur, rand=None):
    h, w = a.shape[:2]
    for y in range(max(0, int(cy - r - 1)), min(h, int(cy + r + 2))):
        for x in range(max(0, int(cx - r - 1)), min(w, int(cx + r + 2))):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r:
                a[y, x, :3] = kleur
            elif rand and d <= r + 1:
                a[y, x, :3] = rand


def kaas_textuur():
    """kaasbol.png (128 x 128): (0,0) the cheese with holes, (64,0) the rind, (0,64) the glowing core, (64,64) the inside of
    the shell. Returns (texture, glow mask)."""
    rng = kit.rng("h1_lucht/kaasbol")
    img = np.zeros((TEX, TEX, 4), np.float32)
    kaas = _vlak(rng, 64, 64, KAAS, 12)
    for _ in range(26):
        _gat(kaas, rng.uniform(2, 62), rng.uniform(2, 62), rng.uniform(1.2, 3.6), GAT, KAAS_DONKER)
    img[0:64, 0:64] = kaas
    korst = _vlak(rng, 64, 64, KORST, 14)
    for _ in range(40):
        x, y = rng.integers(0, 64, 2)
        korst[y, x, :3] = KAAS_DONKER
    img[0:64, 64:128] = korst
    kern = _vlak(rng, 64, 64, KERN, 8)
    for i in range(64):                                     # soft rings of light
        for j in range(64):
            d = math.hypot(i - 31.5, j - 31.5)
            kern[j, i, :3] = np.clip(kern[j, i, :3] + 20 * math.sin(d * 0.5), 0, 255)
    img[64:128, 0:64] = kern
    img[64:128, 64:128] = _vlak(rng, 64, 64, (232, 176, 70), 8)
    gloed = np.zeros_like(img)
    # the glow: the whole core, the holes of the cheese a little (light shines through), and the seam
    gloed[64:128, 0:64] = img[64:128, 0:64]
    gaten = (np.abs(img[0:64, 0:64, :3] - np.array(GAT)).sum(-1) < 30)
    gloed[0:64, 0:64][gaten] = (170, 230, 255, 255)
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)), Image.fromarray(np.clip(gloed, 0, 255).astype(np.uint8))


def wind_textuur():
    """einde2_h1_lucht_luchtbal.png (64 x 64): a swirl of white and light blue (0,0, 32 x 32), and its glow mask."""
    img = np.zeros((64, 64, 4), np.float32)
    for y in range(32):
        for x in range(32):
            hoek = math.atan2(y - 15.5, x - 15.5)
            d = math.hypot(y - 15.5, x - 15.5)
            t = (math.sin(hoek * 3 + d * 0.45) + 1) / 2
            img[y, x, :3] = np.array(WIND) * t + np.array(WIND_BLAUW) * (1 - t)
            img[y, x, 3] = 255
    gloed = img.copy()
    gloed[..., :3] *= 0.75
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)), Image.fromarray(np.clip(gloed, 0, 255).astype(np.uint8))


# =====================================================================================================================
# geometry
# =====================================================================================================================
def _uv(regio, w, h, rng):
    """A face that samples a w x h window of a 64 x 64 region (at a random spot, so the cubes don't all look the same)."""
    u0, v0 = regio
    w, h = max(1.0, min(63.0, w)), max(1.0, min(63.0, h))
    du, dv = rng.uniform(0, 64 - w), rng.uniform(0, 64 - h)
    return {"uv": [round(u0 + du, 2), round(v0 + dv, 2)], "uv_size": [round(w, 2), round(h, 2)]}


def _kubus(origin, size, regio, rng, boven=None):
    sx, sy, sz = size
    maten = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}
    uv = {f: _uv(boven if boven and f in ("up", "down") else regio, *maten[f], rng) for f in maten}
    return {"origin": [round(o, 3) for o in origin], "size": [round(s, 3) for s in size], "uv": uv}


def bol(r, y0, y1, cy, regio, rng, stap=2.0, boven=None):
    """The layers of a ball of radius r (centre height cy) between heights y0 and y1: per layer a cross of two boxes and a
    square, so it looks round from every side."""
    cubes = []
    y = y0
    while y < y1 - 1e-6:
        h = min(stap, y1 - y)
        m = y + h / 2
        rr = math.sqrt(max(0.0, r * r - (m - cy) ** 2))
        if rr >= 0.8:
            a, b = rr, rr * 0.62
            cubes.append(_kubus([-a, y, -b], [2 * a, h, 2 * b], regio, rng, boven))
            cubes.append(_kubus([-b, y, -a], [2 * b, h, 2 * a], regio, rng, boven))
            c = rr * 0.86
            cubes.append(_kubus([-c, y, -c], [2 * c, h, 2 * c], regio, rng, boven))
        y += h
    return cubes


def kaasbol_geo():
    rng = np.random.default_rng(kit.seed("h1_lucht/kaasbol_geo"))
    R, CY = 14.0, 14.0
    kaas, korst, kern = (0, 0), (64, 0), (0, 64)
    bones = [
        {"name": "kaasbol", "pivot": [0, 0, 0], "cubes": []},
        {"name": "onder", "parent": "kaasbol", "pivot": [0, CY, 0], "cubes": bol(R, 0, CY, CY, kaas, rng, boven=korst)},
        {"name": "boven", "parent": "kaasbol", "pivot": [0, CY, R - 2], "cubes": bol(R, CY, 2 * CY, CY, kaas, rng, boven=korst)},
        {"name": "kern", "parent": "kaasbol", "pivot": [0, CY, 0], "cubes": [_kubus([-7, 7, -7], [14, 14, 14], kern, rng)]},
    ]
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.kaasbol", "texture_width": TEX, "texture_height": TEX,
                        "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1.0, 0]},
        "bones": bones}]}


def kaasbol_anim():
    def kf(pairs):
        return {str(round(t, 2)): v for t, v in pairs}
    idle = {"kaasbol": {"position": kf([(0, [0, 0, 0]), (1.5, [0, 0.6, 0]), (3, [0, 0, 0])]),
                        "rotation": kf([(0, [0, 0, -2]), (1.5, [0, 0, 2]), (3, [0, 0, -2])])},
            "kern": {"scale": kf([(0, [1, 1, 1]), (1.5, [1.06, 1.06, 1.06]), (3, [1, 1, 1])])}}
    open_ = {"boven": {"rotation": kf([(0, [0, 0, 0]), (0.3, [6, 0, 0]), (0.5, [-12, 0, 0]), (1.4, [-104, 0, 0]), (1.6, [-96, 0, 0]),
                                       (1.8, [-100, 0, 0])])},
             "kaasbol": {"rotation": kf([(0, [0, 0, 0]), (0.15, [0, 0, 4]), (0.3, [0, 0, -4]), (0.45, [0, 0, 2]), (0.6, [0, 0, 0])])},
             "kern": {"scale": kf([(0, [1, 1, 1]), (0.5, [1.3, 1.3, 1.3]), (1.0, [0.9, 0.9, 0.9]), (1.8, [1, 1, 1])])}}
    dicht = {"boven": {"rotation": kf([(0, [-100, 0, 0]), (1.0, [0, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": {
        "animation.kaasbol.idle": {"loop": True, "animation_length": 3.0, "bones": idle},
        "animation.kaasbol.open": {"loop": "hold_on_last_frame", "animation_length": 1.8, "bones": open_},
        "animation.kaasbol.dicht": {"loop": "hold_on_last_frame", "animation_length": 1.0, "bones": dicht}}}


def luchtbal_geo():
    rng = np.random.default_rng(kit.seed("h1_lucht/luchtbal_geo"))
    bones = [{"name": "bal", "pivot": [0, 5, 0], "cubes": bol(5.0, 0, 10, 5, (0, 0), rng, stap=2.5)}]
    # a ring of wind round its waist
    bones.append({"name": "ring", "parent": "bal", "pivot": [0, 5, 0],
                  "cubes": [_kubus([-6.5, 4.6, -0.4], [13, 0.8, 0.8], (0, 0), rng), _kubus([-0.4, 4.6, -6.5], [0.8, 0.8, 13], (0, 0), rng)]})
    for b in bones:
        for c in b["cubes"]:
            for f in c["uv"].values():                      # (every face shows the whole swirl: the 32 x 32 corner)
                f["uv"], f["uv_size"] = [0, 0], [32, 32]
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.einde2_h1_lucht_luchtbal", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 1.5, "visible_bounds_height": 1.5, "visible_bounds_offset": [0, 0.4, 0]},
        "bones": bones}]}


def luchtbal_anim():
    return {"format_version": "1.8.0", "animations": {
        "animation.einde2_h1_lucht_luchtbal.draai": {"loop": True, "animation_length": 1.0, "bones": {
            "bal": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, -360, 0]}},
            "ring": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 720, 0]}, "scale": {"0.0": [1, 1, 1], "0.5": [1.15, 1, 1.15], "1.0": [1, 1, 1]}}}}}}


# =====================================================================================================================
# the toy block: a ball of air on a little base
# =====================================================================================================================
def scooter_textures():
    rng = kit.rng("h1_lucht/luchtscooter")
    hout = _vlak(rng, 16, 16, (176, 128, 80), 12)
    for y in range(0, 16, 4):
        hout[y, :, :3] = (140, 96, 56)
    wind, _ = wind_textuur()
    wind = wind.crop((8, 8, 24, 24)).convert("RGBA")
    a = np.array(wind).astype(np.float32)
    a[..., 3] = 210                                         # (a little see-through: it is air)
    return Image.fromarray(np.clip(hout, 0, 255).astype(np.uint8)), Image.fromarray(a.astype(np.uint8))


def scooter_model():
    def rondom(key):
        return {s: {"texture": "#" + key} for s in ("north", "south", "east", "west", "up", "down")}
    return {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
            "textures": {"particle": "guhs:block/luchtscooter_voet", "voet": "guhs:block/luchtscooter_voet", "wind": "guhs:block/luchtscooter"},
            "elements": [
                {"from": [3, 0, 3], "to": [13, 2, 13], "faces": rondom("voet")},
                {"from": [7, 2, 7], "to": [9, 5, 9], "faces": rondom("voet")},
                {"from": [4, 5, 4], "to": [12, 13, 12], "faces": rondom("wind")},
                {"from": [5, 6, 3], "to": [11, 12, 4], "faces": rondom("wind")},
                {"from": [5, 6, 12], "to": [11, 12, 13], "faces": rondom("wind")},
                {"from": [3, 6, 5], "to": [4, 12, 11], "faces": rondom("wind")},
                {"from": [12, 6, 5], "to": [13, 12, 11], "faces": rondom("wind")}]}


# =====================================================================================================================
# the clothes: the Luchtnomadengewaad and its arrow
# =====================================================================================================================
_H = [0, 6, -2]
BONES = {
    # the blue arrow of the air nomads: from the back of the head over the top and down the forehead, ending in a point
    "outfit_luchtnomade_pijl": ("head", _H, "luchtnomade_pijl", [
        ([-1.0, 15.0, -9.6], [2.0, 0.4, 7.4], 0),
        ([-1.0, 13.0, -10.3], [2.0, 2.2, 0.4], 0),
        ([-0.8, 11.0, -12.35], [1.6, 2.0, 0.4], 0),
        ([-2.2, 10.2, -12.4], [4.4, 0.8, 0.4], 0),
        ([-1.4, 9.6, -12.4], [2.8, 0.6, 0.4], 0),
        ([-0.6, 9.1, -12.4], [1.2, 0.5, 0.4], 0)]),
}
ORANJE, GEEL, PIJL = (236, 132, 40), (250, 204, 70), (70, 150, 226)


def clothes(rng, v):
    r = kit.rng("h1_lucht/kleding")

    def gewaad():
        a = v.fabric(GEEL, r, 6)
        a[0:6, :] = v.fabric(ORANJE, r, 6)[0:6, :]                 # the orange cape over the shoulders
        a[20:32, :] = v.fabric(ORANJE, r, 6)[20:32, :]             # the orange robe below the yellow sash
        a[18:20, :] = (196, 96, 30)
        return np.clip(a, 0, 255)
    return {"guhvatar_luchtnomade": {"suit": gewaad, "luchtnomade_pijl": (lambda: v.fabric(PIJL, r, 4))}}


ICOON = ["................", "......bbbb......", ".....bbbbbb.....", "......bbbb......", ".......bb.......", "....oooooooo....",
         "...oooooooooo...", "..ooooyyyyoooo..", "..oooyyyyyyooo..", "..ooooyyyyoooo..", "..ooooooooooo...", "...oooooooooo...",
         "...oooooooooo...", "....oooooooo....", "................", "................"]


def icons(ic):
    return {"guhvatar_luchtnomade": ic.icon(ICOON, {"b": PIJL, "o": ORANJE, "y": GEEL})}


# =====================================================================================================================
# the pictures
# =====================================================================================================================
def kaart_hoofdstuk(vm):
    """The narrator card: the floating temple above the void, the paths of the wind."""
    k = vm.Kaart(921)
    k.water([(0, 0), (256, 0), (256, 160), (0, 160)], kleur=(196, 206, 232, 255))      # (the sky)
    k.land([(70, 92), (96, 70), (160, 66), (196, 88), (178, 112), (120, 120), (82, 110)], kleur=(232, 212, 150, 255))
    k.land([(96, 112), (128, 140), (162, 112)], kleur=(170, 134, 96, 255))              # the rock under the island
    k.toren(128, 82, 24, kleur=(236, 236, 240, 255))
    k.toren(104, 88, 16, kleur=(236, 236, 240, 255))
    k.toren(152, 88, 16, kleur=(236, 236, 240, 255))
    k.pad([(30, 40), (70, 56), (104, 60), (128, 56)], kleur=(90, 130, 200, 255))
    k.pad([(220, 30), (190, 50), (160, 58)], kleur=(90, 130, 200, 255))
    k.kruis(128, 92)
    k.kompasroos()
    return k


# the travel map: the main island in the middle, the four story structures around it, in playing order
HALTES = {"guhvatar_lucht": (60, 38), "guhvatar_water": (204, 34), "guhvatar_aarde": (206, 124), "guhvatar_vuur": (54, 128),
          "guhvatar_finale": (78, 112)}


def kaart_reis(vm):
    k = vm.Kaart(922)
    k.water([(0, 0), (256, 0), (256, 160), (0, 160)], kleur=(88, 70, 112, 255))         # the void
    k.land([(108, 64), (128, 50), (150, 62), (146, 92), (122, 98), (104, 86)], kleur=(232, 206, 120, 255))   # the Knabbelberg
    k.berg(128, 80, 16, kleur=(214, 170, 90, 255))
    for x, y in ((60, 38), (204, 34), (206, 124), (54, 128)):
        k.land([(x - 16, y + 2), (x - 6, y - 10), (x + 12, y - 8), (x + 16, y + 4), (x + 2, y + 12)], kleur=(220, 200, 150, 255))
    k.toren(60, 38, 14, kleur=(236, 236, 240, 255))                                     # the air temple
    k.toren(204, 34, 12, kleur=(170, 214, 240, 255))                                    # the ice city
    k.huisje(206, 124, kleur=(120, 150, 80, 255))                                        # the walled city
    k.vulkaan(54, 128, 18)                                                              # the capital in its crater
    k.pad([(128, 74), (96, 56), (60, 38), (204, 34), (206, 124), (54, 128)], kleur=(232, 232, 250, 255))
    k.kompasroos(232, 80)
    return k
