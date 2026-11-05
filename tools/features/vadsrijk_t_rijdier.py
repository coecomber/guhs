"""
1.10.0 "Het Vadsrijk", T-RIJDIER (night round T): mount looks, three new land mounts, pet roles. Java: feature/vadsrijk/rijdier
(+ small hooks in economie: RijdierSoort / EcoSpul / Rijdieren / Stal / RijdierScherm / EconomieClient, and huisdieren).

  * three new land mounts (GeckoLib models + animations + a texture per coat): the Duinhupper (a springy beach hare), the
    Wolkwalkalfje (a cloud-whale calf that floats) and the Zwamzwijntje (a piglet with a mushroom-cap saddle);
  * coats ("vacht") of the existing mounts: recoloured textures vr_eco_<soort>_<vacht>.png (the Wolkenschaapje is re-painted from
    its own model; the render-copied Knuffelhert/Guhwolf/Slakkie are recoloured pixel by pixel on their fur colours);
  * two new Pluimguh colours (kristal, lampion) recoloured from the pink one, with their egg icons;
  * pattern overlays per species: vr_rijpatroon_<soort>_<patroon>.png (only fur pixels; white shapes, tinted in game);
  * the tack items vr_rijtuig_<id> (3D item models: drawn on the mount's bones, and the reward tokens);
  * saddle icons of the new mounts; every text (nl + en).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_rijdier.py
"""
import colorsys
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

try:
    import vadsrijk_wezens as W
    import vadsrijk_rijdieren as RD
    from vadsrijk_economie import zadel
except ImportError:  # (inside make_resources: the package import)
    from features import vadsrijk_wezens as W
    from features import vadsrijk_rijdieren as RD
    from features.vadsrijk_economie import zadel

kf, anim = W.kf, W.anim
ITEM_TEX = "vadsrijk/rijdier"
S = 4   # every mount texture is 4 px per model pixel


# =====================================================================================================================
# the three new mounts
# =====================================================================================================================
def model_duinhupper():
    """A springy beach hare: big hind legs, long ears, a fluffy tail. Saddle top y 19 (zit 1.15)."""
    m = W.Model("eco_duinhupper", px=S, breedte=96, bounds=(2.2, 2.6, 0.9))
    m.bone("root")
    m.bone("body", "root", (0, 12, 0))
    m.cube("body", "vacht", (-6, 8, -6), (12, 10, 13))
    m.cube("body", "buik", (-4.5, 8.5, -6.6), (9, 7, 0.6))
    m.bone("zadel", "body", (0, 18, 0))
    m.cube("zadel", "zadel", (-5, 18, -3), (10, 1, 7))
    m.cube("zadel", "zadelrand", (-5.6, 15, -3), (0.6, 3.5, 7))
    m.spiegel("zadel", "zadelrand", (-5.6, 15, -3), (0.6, 3.5, 7))
    m.bone("hoofd", "body", (0, 16, -6))
    m.cube("hoofd", "gezicht", (-4.5, 14, -12), (9, 8, 7), gezicht={"ey": 0.42})
    m.cube("hoofd", "neus", (-1, 16.2, -12.5), (2, 1.4, 0.5))
    for kant, sgn in (("l", -1), ("r", 1)):
        m.bone("oor_" + kant, "hoofd", (2.5 * sgn, 22, -9), (0, 0, 10 * -sgn))
        if sgn < 0:
            m.cube("oor_" + kant, "oor", (-4, 22, -10), (3, 11, 2))
            m.cube("oor_" + kant, "oorbinnen", (-3.5, 23, -10.3), (2, 9, 0.4))
        else:
            m.spiegel("oor_" + kant, "oor", (-4, 22, -10), (3, 11, 2))
            m.spiegel("oor_" + kant, "oorbinnen", (-3.5, 23, -10.3), (2, 9, 0.4))
    m.bone("tail", "body", (0, 14, 7))
    m.cube("tail", "staart", (-2.5, 12, 7), (5, 5, 4))
    for naam, x, z, w, h, d in (("leg_fl", -3, -4.5, 2.5, 8, 2.5), ("leg_fr", 3, -4.5, 2.5, 8, 2.5),
                                ("leg_bl", -4, 4, 4, 8, 6), ("leg_br", 4, 4, 4, 8, 6)):
        m.bone(naam, "root", (x, 8, z))
        m.cube(naam, "poot", (x - w / 2, 0, z - d / 2), (w, h, d))
    return m


PALET_HUPPER = {
    "vacht": ((234, 206, 152), "plekjes", (244, 222, 176)),
    "gezicht": ((234, 206, 152), None, None),
    "oor": ((226, 196, 140), None, None),
    "poot": ((214, 184, 132), None, None),
    "buik": ((252, 244, 226), None, None),
    "staart": ((255, 252, 248), "plekjes", (244, 240, 236)),
    "neus": ((255, 140, 170), None, None),
    "oorbinnen": ((255, 186, 206), None, None),
    "zadel": ((126, 210, 226), "strepen", (250, 250, 255)),
    "zadelrand": ((255, 150, 196), None, None),
}


def model_wolkwalkalfje():
    """A cloud-whale calf: a round body with a face, little fins, a tail fluke, a cloud under its belly. Saddle top 19 (+float)."""
    m = W.Model("eco_wolkwalkalfje", px=S, breedte=112, bounds=(2.6, 2.4, 0.9))
    m.bone("root")
    m.bone("body", "root", (0, 12, 0))
    m.cube("body", "huid", (-7, 6, -10), (14, 12, 20))
    m.cube("body", "gezicht", (-6.5, 7, -11), (13, 10, 1), gezicht={"ey": 0.4, "schaal": 0.8})
    m.cube("body", "buik", (-6, 5.5, -9), (12, 0.5, 17))
    m.bone("zadel", "body", (0, 18, 0))
    m.cube("zadel", "zadel", (-5, 18, -3), (10, 1, 8))
    m.cube("zadel", "zadelrand", (-5.6, 15.5, -3), (0.6, 3, 8))
    m.spiegel("zadel", "zadelrand", (-5.6, 15.5, -3), (0.6, 3, 8))
    for kant, sgn in (("l", -1), ("r", 1)):
        m.bone("vin_" + kant, "body", (7 * sgn, 9, -3))
        if sgn < 0:
            m.cube("vin_" + kant, "vin", (-11, 8.5, -5), (4, 1, 5))
        else:
            m.spiegel("vin_" + kant, "vin", (-11, 8.5, -5), (4, 1, 5))
    m.bone("staart", "body", (0, 12, 10))
    m.cube("staart", "huid", (-2, 10.5, 10), (4, 4, 5))
    m.cube("staart", "vin", (-5.5, 12, 14), (11, 1, 4))
    m.bone("wolkje", "root", (0, 2, 0))
    m.cube("wolkje", "wolk", (-6, 1, -8), (12, 2.5, 15))
    m.cube("wolkje", "wolk", (-7.5, 1.5, -4), (15, 1.5, 8))
    return m


PALET_WALVIS = {
    "huid": ((174, 216, 246), "plekjes", (196, 228, 252)),
    "gezicht": ((174, 216, 246), None, None),
    "buik": ((240, 248, 255), None, None),
    "vin": ((150, 196, 236), None, None),
    "wolk": ((246, 250, 255), "plekjes", (226, 238, 252)),
    "zadel": ((255, 170, 206), "strepen", (255, 236, 246)),
    "zadelrand": ((255, 214, 120), None, None),
}


def model_zwamzwijntje():
    """A piglet with a red mushroom cap as its saddle. Cap top y 17 (zit 1.1)."""
    m = W.Model("eco_zwamzwijntje", px=S, breedte=96, bounds=(2.2, 2.0, 0.7))
    m.bone("root")
    m.bone("body", "root", (0, 9, 0))
    m.cube("body", "huid", (-6, 5, -7), (12, 9, 14))
    m.bone("zadel", "body", (0, 14, 0))
    m.cube("zadel", "hoedrand", (-7, 14, -5.5), (14, 1, 11))
    m.cube("zadel", "hoed", (-6.5, 15, -5), (13, 2, 10))
    m.bone("hoofd", "body", (0, 11, -7))
    m.cube("hoofd", "gezicht", (-4.5, 7, -12), (9, 8, 5), gezicht={"ey": 0.36})
    m.cube("hoofd", "snuit", (-2.5, 8, -13), (5, 3.5, 1))
    for kant, sgn in (("l", -1), ("r", 1)):
        m.bone("oor_" + kant, "hoofd", (3 * sgn, 15, -9), (0, 0, 25 * -sgn))
        if sgn < 0:
            m.cube("oor_" + kant, "oor", (-5, 14.5, -10), (3, 3, 1.5))
        else:
            m.spiegel("oor_" + kant, "oor", (-5, 14.5, -10), (3, 3, 1.5))
    m.bone("tail", "body", (0, 11, 7))
    m.cube("tail", "staart", (-0.75, 11, 7), (1.5, 1.5, 2.5))
    for naam, x, z in (("leg_fl", -3.5, -4.5), ("leg_fr", 3.5, -4.5), ("leg_bl", -3.5, 4.5), ("leg_br", 3.5, 4.5)):
        m.bone(naam, "root", (x, 5, z))
        m.cube(naam, "poot", (x - 1.25, 0, z - 1.25), (2.5, 5, 2.5))
    return m


PALET_ZWIJN = {
    "huid": ((246, 182, 196), None, None),
    "gezicht": ((246, 182, 196), None, None),
    "snuit": ((255, 146, 170), "stippen", (214, 96, 128)),
    "oor": ((236, 160, 180), None, None),
    "poot": ((230, 156, 174), None, None),
    "staart": ((240, 160, 180), None, None),
    "hoed": ((230, 62, 76), "stippen", (255, 250, 240)),
    "hoedrand": ((250, 236, 220), None, None),
}

NIEUW = {
    "duinhupper": (model_duinhupper, PALET_HUPPER, 4101, ("vacht", "gezicht", "oor", "poot")),
    "wolkwalkalfje": (model_wolkwalkalfje, PALET_WALVIS, 4102, ("huid", "gezicht", "vin")),
    "zwamzwijntje": (model_zwamzwijntje, PALET_ZWIJN, 4103, ("huid", "gezicht", "oor", "poot", "staart")),
}
# the rolls that carry patterns (no faces, no saddles)
PATROON_ROLLEN = {
    "duinhupper": ("vacht", "oor", "poot"),
    "wolkwalkalfje": ("huid", "vin"),
    "zwamzwijntje": ("huid", "poot", "oor"),
    "wolkenschaapje": ("wol",),
}


def _mix(c, d, f):
    return tuple(int(a * (1 - f) + b * f) for a, b in zip(c, d))


def _schaal(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


# coat colours of the new mounts (main colour; the palette is derived)
NIEUW_VACHT = {
    "duinhupper": {"sneeuw": (244, 242, 250), "roze": (246, 176, 206), "nacht": (82, 80, 142)},
    "wolkwalkalfje": {"zonsopkomst": (255, 192, 160), "sterrenhemel": (62, 60, 128), "goud": (255, 216, 106)},
    "zwamzwijntje": {"blauwzwam": (140, 180, 240), "dooier": (246, 214, 106), "glimzwam": (106, 74, 166)},
}


def palet_vacht(naam, palet, kleur):
    p = dict(palet)
    if naam == "zwamzwijntje":   # (the coat of the Zwamzwijntje = its mushroom cap; glimzwam also makes the piglet a bit lilac)
        p["hoed"] = (kleur, "stippen", (255, 250, 240))
        if kleur == (106, 74, 166):
            p["huid"] = ((222, 186, 232), None, None)
            p["gezicht"] = ((222, 186, 232), None, None)
        return p
    hoofd = PATROON_ROLLEN[naam][0]
    basis = palet[hoofd][0]
    for rol in NIEUW[naam][3]:
        oud = palet[rol]
        f = sum(oud[0]) / max(1, sum(basis))
        p[rol] = (_schaal(kleur, f), oud[1], _schaal(_mix(kleur, (255, 255, 255), 0.25), f) if oud[2] else None)
    for rol in ("buik", "staart"):
        if rol in p and rol not in NIEUW[naam][3]:
            p[rol] = (_mix(kleur, (255, 255, 255), 0.75), p[rol][1], _mix(kleur, (255, 255, 255), 0.85) if p[rol][2] else None)
    return p


def anims_wolkwalkalfje():
    def zweef(hoog, laag, lengte):
        return {"position": kf([(0, (0, laag, 0)), (lengte / 2, (0, hoog, 0)), (lengte, (0, laag, 0))])}

    def vinnen(hoek, lengte):
        return {"vin_l": {"rotation": kf([(0, (0, 0, -hoek)), (lengte / 2, (0, 0, hoek)), (lengte, (0, 0, -hoek))])},
                "vin_r": {"rotation": kf([(0, (0, 0, hoek)), (lengte / 2, (0, 0, -hoek)), (lengte, (0, 0, hoek))])},
                "staart": {"rotation": kf([(0, (-hoek * 0.6, 0, 0)), (lengte / 2, (hoek * 0.6, 0, 0)), (lengte, (-hoek * 0.6, 0, 0))])}}

    idle = {"root": zweef(3.0, 2.0, 3.0), "wolkje": {"scale": kf([(0, (1, 1, 1)), (1.5, (1.08, 1, 1.08)), (3.0, (1, 1, 1))])},
            "body": {"rotation": kf([(0, (0, 0, -2)), (1.5, (0, 0, 2)), (3.0, (0, 0, -2))])}}
    idle.update(vinnen(12, 3.0))
    hop = {"root": zweef(3.5, 2.0, 0.8), "body": {"rotation": kf([(0, (-3, 0, 0)), (0.4, (3, 0, 0)), (0.8, (-3, 0, 0))])}}
    hop.update(vinnen(28, 0.8))
    ren = {"root": zweef(4.0, 2.5, 0.5), "body": {"rotation": kf([(0, (4, 0, 0)), (0.25, (8, 0, 0)), (0.5, (4, 0, 0))])}}
    ren.update(vinnen(40, 0.5))
    sprong = {"root": {"position": kf([(0, (0, 3, 0))])}, "wolkje": {"scale": kf([(0, (1.25, 1, 1.25))])},
              "vin_l": {"rotation": kf([(0, (0, 0, -35))])}, "vin_r": {"rotation": kf([(0, (0, 0, 35))])},
              "staart": {"rotation": kf([(0, (25, 0, 0))])}}
    return {"idle": anim(3.0, idle), "hop": anim(0.8, hop), "ren": anim(0.5, ren), "sprong": anim(0.5, sprong)}


def anims_huppel():
    """The Duinhupper: the quadruped gait, but every step is a little hop (body bounces higher)."""
    a = RD.anims_viervoeter(False)
    for naam, hoog, lengte in (("hop", 2.4, 0.7), ("ren", 3.2, 0.4)):
        q = lengte / 2
        a[naam]["bones"]["body"]["position"] = kf([(0, (0, 0, 0)), (q * 0.5, (0, hoog, 0)), (q, (0, 0, 0)), (q * 1.5, (0, hoog, 0)), (lengte, (0, 0, 0))])
    return a


# =====================================================================================================================
# recolouring the render-copied textures (fur pixels only)
# =====================================================================================================================
def _hsv(arr):
    rgb = arr[..., :3].astype(float) / 255.0
    mx, mn = rgb.max(-1), rgb.min(-1)
    v = mx
    s = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    d = np.maximum(mx - mn, 1e-6)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    h = np.where(mx == r, ((g - b) / d) % 6, np.where(mx == g, (b - r) / d + 2, (r - g) / d + 4)) * 60
    h = np.where(mx - mn < 1e-6, 0, h)
    return h, s, v


def masker(soort, arr):
    """The fur pixels of a render-copied texture (soort: knuffelhert, guhwolf, slakkie, pluimguh)."""
    h, s, v = _hsv(arr)
    a = arr[..., 3] > 0
    if soort == "knuffelhert":
        return a & (h >= 5) & (h <= 38) & (s >= 0.12) & (s <= 0.55) & (v >= 0.55)
    if soort == "guhwolf":
        return a & (s <= 0.3) & (v >= 0.58)
    if soort == "slakkie":   # (the shell: orange, with its darker spiral)
        return a & (h >= 12) & (h <= 34) & (s >= 0.35) & (s <= 0.66) & (v >= 0.74)
    if soort == "pluimguh":
        return a & ((h >= 295) | (h <= 12)) & (s >= 0.08) & (v >= 0.45)
    raise KeyError(soort)


def herkleur(arr, m, doel):
    """Fur pixels -> the target colour, keeping each pixel's brightness relative to the fur's average."""
    out = arr.copy()
    lum = arr[..., :3].astype(float).mean(-1)
    ref = max(1.0, float(lum[m].mean()))
    f = (lum / ref)[..., None]
    nieuw = np.clip(np.array(doel, float)[None, None, :] * f, 0, 255)
    out[..., :3] = np.where(m[..., None], nieuw, arr[..., :3]).astype(np.uint8)
    return out


# coats of the existing mounts (must match rijdier/Uiterlijk.java)
VACHTEN = {
    "knuffelhert": {"sneeuw": (242, 240, 248), "roze": (244, 170, 202), "honing": (240, 190, 90), "nacht": (90, 80, 150)},
    "guhwolf": {"zilver": (174, 180, 194), "zon": (242, 200, 120), "middernacht": (70, 70, 122), "roze": (246, 180, 210)},
    "slakkie": {"mint": (140, 224, 190), "lavendel": (184, 160, 240), "honing": (240, 192, 96), "nacht": (74, 74, 138)},
    "wolkenschaapje": {"suikerspin": (255, 184, 218), "lavendel": (200, 180, 244), "onweer": (138, 138, 166), "karamel": (230, 176, 122)},
}
PLUIM_NIEUW = {"kristal": (184, 232, 255), "lampion": (255, 168, 96)}
EI_NIEUW = {"kristal": ((196, 236, 255), (255, 255, 255), (255, 170, 210)), "lampion": ((255, 176, 104), (255, 230, 120), (255, 150, 170))}


# =====================================================================================================================
# patterns (white shapes on the fur pixels; tinted in game)
# =====================================================================================================================
PATRONEN = ("vlekjes", "streepjes", "sterretjes", "hartjes", "glitter")


def _stempel(vorm, n):
    img = Image.new("L", (n, n), 0)
    d = ImageDraw.Draw(img)
    c = (n - 1) / 2
    if vorm == "ster":
        pts = []
        for i in range(10):
            r = c if i % 2 == 0 else c * 0.45
            a = -math.pi / 2 + i * math.pi / 5
            pts.append((c + r * math.cos(a), c + r * math.sin(a)))
        d.polygon(pts, fill=255)
    else:   # hart
        r = n * 0.27
        d.ellipse((c - 2 * r + 0.5, c - r * 1.4, c + 0.5, c + r * 0.6), fill=255)
        d.ellipse((c - 0.5, c - r * 1.4, c + 2 * r - 0.5, c + r * 0.6), fill=255)
        d.polygon([(c - 2 * r + 0.6, c), (c + 2 * r - 0.6, c), (c, c + 2.1 * r)], fill=255)
    return np.asarray(img) > 100


def patroon(m, soort_naam, kind, seed):
    """An RGBA overlay: white shapes where the mask is true."""
    h, w = m.shape
    r = np.random.default_rng(seed)
    vorm = np.zeros((h, w), bool)
    opp = int(m.sum())
    if kind == "vlekjes":
        yy, xx = np.mgrid[0:h, 0:w]
        for _ in range(max(6, opp // 700)):
            cy, cx = r.integers(0, h), r.integers(0, w)
            rr = r.uniform(5, 9)
            y0, y1, x0, x1 = max(0, int(cy - rr)), min(h, int(cy + rr) + 1), max(0, int(cx - rr)), min(w, int(cx + rr) + 1)
            vorm[y0:y1, x0:x1] |= np.hypot(xx[y0:y1, x0:x1] - cx, yy[y0:y1, x0:x1] - cy) <= rr
    elif kind == "streepjes":
        rij = (np.arange(h) // 6) % 3 == 0
        vorm[rij, :] = True
    elif kind in ("sterretjes", "hartjes"):
        st = _stempel("ster" if kind == "sterretjes" else "hart", 11)
        n = st.shape[0]
        for _ in range(max(6, opp // 1100)):
            cy, cx = r.integers(0, h - n), r.integers(0, w - n)
            vorm[cy:cy + n, cx:cx + n] |= st
    elif kind == "glitter":
        for _ in range(max(10, opp // 70)):
            cy, cx = r.integers(1, h - 2), r.integers(1, w - 2)
            vorm[cy:cy + 2, cx:cx + 2] = True
        for _ in range(max(4, opp // 900)):
            cy, cx = r.integers(3, h - 4), r.integers(3, w - 4)
            vorm[cy - 3:cy + 4, cx] = True
            vorm[cy, cx - 3:cx + 4] = True
    vorm &= m
    out = np.zeros((h, w, 4), np.uint8)
    out[vorm] = (255, 255, 255, 255)
    if kind == "glitter":   # (the eyes layer is additive: a soft white glow; black = nothing)
        out[..., 3] = 255
        out[~vorm, :3] = 0
        out[vorm, :3] = (230, 230, 255)
    return out


def model_masker(m, rollen):
    """The fur mask of a W.Model texture: the regions of these rolls."""
    mask = np.zeros((m.hoogte() * S, m.W * S), bool)
    for reg in m.regios:
        if reg["rol"] in rollen:
            x, y, w, h = reg["r"]
            mask[y * S:(y + h) * S, x * S:(x + w) * S] = True
    return mask


# =====================================================================================================================
# tack: 3D item models (attached at the anchor = model point (8, 8, 8); front = low z)
# =====================================================================================================================
TUIG_KLEUREN = {   # palette cells (2x2 px each in a 16x16 texture)
    "roze": (255, 140, 190), "wit": (255, 255, 255), "goud": (255, 208, 64), "groen": (110, 196, 110), "geel": (255, 232, 110),
    "lila": (196, 164, 244), "paars": (90, 58, 120), "donker": (60, 40, 50), "oranje": (255, 140, 60), "rood": (200, 50, 60),
    "licht": (255, 236, 150), "zalm": (255, 190, 210), "touw": (230, 200, 140), "blauw": (130, 200, 240), "roze2": (255, 96, 160),
}
CELLEN = {k: ((i % 8) * 2, (i // 8) * 2) for i, k in enumerate(TUIG_KLEUREN)}


def _blok(van, tot, kleur):
    u, v = CELLEN[kleur]
    faces = {f: {"uv": [u + 0.3, v + 0.3, u + 1.7, v + 1.7], "texture": "#t"} for f in ("north", "south", "east", "west", "up", "down")}
    return {"from": [round(x, 3) for x in van], "to": [round(x, 3) for x in tot], "faces": faces}


def tuig_elementen(naam):
    e = []
    if naam == "dekje":
        e.append(_blok((2, 8, 2.5), (14, 8.6, 13.5), "roze"))
        for x0 in (1.5, 14):
            e.append(_blok((x0, 3.8, 3), (x0 + 0.5, 8.6, 13), "roze"))
            e.append(_blok((x0 - 0.1, 3.4, 3), (x0 + 0.6, 4.0, 13), "goud"))
        for x, z in ((4, 4.5), (7.5, 6), (11, 4.5), (5.5, 9), (10, 9.5), (8, 12)):
            e.append(_blok((x, 8.6, z), (x + 1, 8.7, z + 1), "wit"))
        e.append(_blok((2, 8.05, 2.3), (14, 8.65, 2.6), "goud"))
        e.append(_blok((2, 8.05, 13.4), (14, 8.65, 13.7), "goud"))
    elif naam == "bloemenkrans":
        kleuren = ("roze", "geel", "wit", "lila", "roze2", "geel", "zalm", "lila")
        for i, k in enumerate(kleuren):
            a = i * math.pi / 4
            x, z = 8 + 4.6 * math.cos(a), 8 + 4.6 * math.sin(a)
            e.append(_blok((x - 0.9, 7.6, z - 0.9), (x + 0.9, 9.0, z + 0.9), k))
            e.append(_blok((x - 0.35, 8.95, z - 0.35), (x + 0.35, 9.25, z + 0.35), "geel" if k != "geel" else "oranje"))
            a2 = a + math.pi / 8
            x2, z2 = 8 + 4.6 * math.cos(a2), 8 + 4.6 * math.sin(a2)
            e.append(_blok((x2 - 0.6, 7.7, z2 - 0.6), (x2 + 0.6, 8.3, z2 + 0.6), "groen"))
    elif naam == "belletjes":
        e.append(_blok((3.5, 8.3, 7.5), (12.5, 9.1, 8.5), "roze"))
        for x in (5, 9.5):
            e.append(_blok((x, 6.0, 7), (x + 1.8, 8.3, 9), "goud"))
            e.append(_blok((x - 0.25, 5.8, 6.75), (x + 2.05, 6.3, 9.25), "goud"))
            e.append(_blok((x + 0.55, 5.2, 7.65), (x + 1.25, 5.8, 8.35), "donker"))
            e.append(_blok((x + 0.6, 8.2, 7.8), (x + 1.2, 8.6, 8.2), "licht"))
    elif naam == "hoedje":
        e.append(_blok((4.5, 8, 4.5), (11.5, 8.5, 11.5), "paars"))
        e.append(_blok((5.5, 8.5, 5.5), (10.5, 12.5, 10.5), "paars"))
        e.append(_blok((5.4, 9, 5.4), (10.6, 10, 10.6), "roze"))
        e.append(_blok((4.6, 9.2, 7.2), (5.4, 10.2, 8.2), "geel"))
    elif naam == "lampionnetjes":
        e.append(_blok((-0.5, 8.4, 7.8), (16.5, 8.8, 8.2), "touw"))
        for x0 in (-1.5, 14.5):
            e.append(_blok((x0 + 1.3, 6.0, 7.8), (x0 + 1.7, 8.5, 8.2), "touw"))
            e.append(_blok((x0, 2.0, 6.5), (x0 + 3, 6.0, 9.5), "oranje"))
            e.append(_blok((x0 + 0.3, 2.4, 6.3), (x0 + 2.7, 5.6, 9.7), "licht"))
            e.append(_blok((x0 + 0.5, 1.4, 7.0), (x0 + 2.5, 2.0, 9.0), "rood"))
            e.append(_blok((x0 + 0.5, 6.0, 7.0), (x0 + 2.5, 6.6, 9.0), "rood"))
    elif naam == "kroontje":
        e.append(_blok((5.5, 8, 5.5), (10.5, 9.4, 10.5), "goud"))
        for x, z in ((5.5, 5.5), (9.5, 5.5), (5.5, 9.5), (9.5, 9.5)):
            e.append(_blok((x, 9.4, z), (x + 1, 11.2, z + 1), "goud"))
            e.append(_blok((x + 0.25, 11.2, z + 0.25), (x + 0.75, 11.6, z + 0.75), "roze2"))
        e.append(_blok((7.4, 8.3, 5.2), (8.6, 9.3, 5.5), "roze2"))
    return e


TUIGEN = ("dekje", "bloemenkrans", "belletjes", "hoedje", "lampionnetjes", "kroontje")


def tuig_textuur():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for k, (u, v) in CELLEN.items():
        for dx in range(2):
            for dy in range(2):
                img.putpixel((u + dx, v + dy), TUIG_KLEUREN[k] + (255,))
    return img


def tuig_model(naam):
    els = tuig_elementen(naam)
    lo = [min(el["from"][i] for el in els) for i in range(3)]
    hi = [max(el["to"][i] for el in els) for i in range(3)]
    groot = max(hi[i] - lo[i] for i in range(3))
    sch = round(min(1.6, 14 / max(1, groot)) * 0.625, 3)
    mid = [(lo[i] + hi[i]) / 2 for i in range(3)]
    return {
        "textures": {"t": f"guhs:item/{ITEM_TEX}/tuig", "particle": f"guhs:item/{ITEM_TEX}/tuig"},
        "elements": els,
        "display": {
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "gui": {"rotation": [25, -135, 0], "translation": [round((8 - mid[0]) * sch, 2), round((8 - mid[1]) * sch, 2), 0], "scale": [sch] * 3},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.4] * 3},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375] * 3},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4] * 3},
        },
    }


# =====================================================================================================================
# writing
# =====================================================================================================================
def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _png(pad, img):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    if isinstance(img, np.ndarray):
        img = Image.fromarray(img)
    img.save(pad, optimize=True)


def _item(a, w, naam, model=None):
    if model is None:
        model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{ITEM_TEX}/{naam}"}}
    w(os.path.join(a, "models", "item", f"{naam}.json"), model)
    w(os.path.join(a, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})


def assets(a, w=_schrijf_json):
    geo = os.path.join(a, "geckolib", "models", "entity")
    ani = os.path.join(a, "geckolib", "animations", "entity")
    tex = os.path.join(a, "textures", "entity")
    n = 0
    # ---- the new mounts ----------------------------------------------------------------------------------------------------
    for naam, (maak, palet, seed, _rollen) in NIEUW.items():
        m = maak()
        w(os.path.join(geo, f"vr_eco_{naam}.geo.json"), m.geo())
        anims = anims_wolkwalkalfje() if naam == "wolkwalkalfje" else anims_huppel() if naam == "duinhupper" else RD.anims_viervoeter(False)
        w(os.path.join(ani, f"vr_eco_{naam}.animation.json"), W.anim_bestand(anims))
        _png(os.path.join(tex, f"vr_eco_{naam}.png"), W.schilder(m, palet, "blij", seed, False))
        for vacht, kleur in NIEUW_VACHT[naam].items():
            _png(os.path.join(tex, f"vr_eco_{naam}_{vacht}.png"), W.schilder(m, palet_vacht(naam, palet, kleur), "blij", seed, False))
            n += 1
        mask = model_masker(m, PATROON_ROLLEN[naam])
        for i, p in enumerate(PATRONEN):
            _png(os.path.join(tex, f"vr_rijpatroon_{naam}_{p}.png"), patroon(mask, naam, p, seed + i))
    # ---- the Wolkenschaapje: re-painted from its own model --------------------------------------------------------------------
    m = RD.model_wolkenschaapje()
    for vacht, kleur in VACHTEN["wolkenschaapje"].items():
        p = dict(RD.PALET_SCHAAP)
        p["wol"] = (kleur, "plekjes", _mix(kleur, (255, 255, 255), 0.35))
        p["gezicht"] = (_mix(kleur, (255, 220, 230), 0.5), None, None)
        if vacht == "onweer":
            p["wolk"] = ((150, 150, 176), "plekjes", (176, 176, 200))
        _png(os.path.join(tex, f"vr_eco_wolkenschaapje_{vacht}.png"), W.schilder(m, p, "blij", 3200, False))
        n += 1
    mask = model_masker(m, PATROON_ROLLEN["wolkenschaapje"])
    for i, p in enumerate(PATRONEN):
        _png(os.path.join(tex, f"vr_rijpatroon_wolkenschaapje_{p}.png"), patroon(mask, "wolkenschaapje", p, 3300 + i))
    # ---- the render-copied textures: recoloured + their pattern masks -----------------------------------------------------------
    for naam, bestand in (("knuffelhert", "vr_eco_knuffelhert.png"), ("guhwolf", "vr_eco_guhwolf.png"), ("slakkie", "vr_eco_slakkie.png"),
                          ("pluimguh", "vr_eco_pluimguh_roze.png")):
        bron = os.path.join(tex, bestand)
        if not os.path.exists(bron):
            print(f"vadsrijk_t_rijdier: {bestand} missing, skipped")
            continue
        arr = np.asarray(Image.open(bron).convert("RGBA"))
        mk = masker(naam, arr)
        doelen = PLUIM_NIEUW if naam == "pluimguh" else VACHTEN[naam]
        for vacht, kleur in doelen.items():
            _png(os.path.join(tex, f"vr_eco_{naam}_{vacht}.png"), herkleur(arr, mk, kleur))
            n += 1
        for i, p in enumerate(PATRONEN):
            _png(os.path.join(tex, f"vr_rijpatroon_{naam}_{p}.png"), patroon(mk, naam, p, 5100 + i))
    # ---- icons ------------------------------------------------------------------------------------------------------------------
    it = os.path.join(a, "textures", "item", ITEM_TEX)
    tuig_textuur().save(os.path.join(it, "tuig.png")) if os.path.isdir(it) else (os.makedirs(it, exist_ok=True), tuig_textuur().save(os.path.join(it, "tuig.png")))
    for t in TUIGEN:
        _item(a, w, f"vr_rijtuig_{t}", tuig_model(t))
    zadels = {"duinhupper": ((234, 206, 152), [(5, 6), (10, 5)], (126, 210, 226)),
              "wolkwalkalfje": ((174, 216, 246), [(6, 6), (10, 5)], (255, 170, 206)),
              "zwamzwijntje": ((230, 62, 76), [(5, 6), (8, 5), (10, 7)], (250, 236, 220))}
    for naam, (kap, stip, rug) in zadels.items():
        zadel(kap, stip, rug).png(os.path.join(a, "textures", "item", RD.ITEM_TEX, f"vr_item_zadel_{naam}.png"))
        w(os.path.join(a, "models", "item", f"vr_item_zadel_{naam}.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{RD.ITEM_TEX}/vr_item_zadel_{naam}"}})
        w(os.path.join(a, "items", f"vr_item_zadel_{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/vr_item_zadel_{naam}"}})
    for k, v in EI_NIEUW.items():
        RD.ei(*v).png(os.path.join(a, "textures", "item", RD.ITEM_TEX, f"vr_item_pluimei_{k}.png"))
        w(os.path.join(a, "models", "item", f"vr_item_pluimei_{k}.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{RD.ITEM_TEX}/vr_item_pluimei_{k}"}})
        w(os.path.join(a, "items", f"vr_item_pluimei_{k}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/vr_item_pluimei_{k}"}})
    return n


# =====================================================================================================================
# texts (nl, en)
# =====================================================================================================================
E = "vadsrijk.economie."
R = "vadsrijk.rijdier."
H = "vadsrijk.huisdier."


def teksten():
    t = {
        # the new mounts
        "entity.guhs.vr_eco_duinhupper": ("Duinhupper", "Dune Hopper"),
        "entity.guhs.vr_eco_wolkwalkalfje": ("Wolkwalkalfje", "Cloud Whale Calf"),
        "entity.guhs.vr_eco_zwamzwijntje": ("Zwamzwijntje", "Mushroom Piglet"),
        E + "rijdier.duinhupper": ("Duinhupper", "Dune Hopper"),
        E + "rijdier.wolkwalkalfje": ("Wolkwalkalfje", "Cloud Whale Calf"),
        E + "rijdier.zwamzwijntje": ("Zwamzwijntje", "Mushroom Piglet"),
        "item.guhs.vr_item_zadel_duinhupper": ("Zadel van de Duinhupper", "Dune Hopper Saddle"),
        "item.guhs.vr_item_zadel_wolkwalkalfje": ("Zadel van het Wolkwalkalfje", "Cloud Whale Calf Saddle"),
        "item.guhs.vr_item_zadel_zwamzwijntje": ("Zadel van het Zwamzwijntje", "Mushroom Piglet Saddle"),
        E + "rijdier.eigen.duinhupper": ("Een springerige strandhaas. Springt het allerhoogst (bijna 6 blokken), maar loopt wat rustiger.",
                                         "A springy beach hare. Jumps the highest of all (almost 6 blocks), but runs a bit slower."),
        E + "rijdier.eigen.wolkwalkalfje": ("Een jong wolkwalvisje op een eigen wolkje. Zweeft: valt heel zacht en springt in de lucht nog een keer.",
                                            "A young cloud whale on its own little cloud. Floats: falls gently and can jump once more in the air."),
        E + "rijdier.eigen.zwamzwijntje": ("Een stevig biggetje met een paddenstoelzadel. Schrikt niet van de eerste tik.",
                                           "A sturdy piglet with a mushroom saddle. The first hit doesn't scare it."),
        E + "rijdier.eigen.knuffelhert": ("Zacht als een knuffel, met een sierlijk geweitje. Springt heel hoog.",
                                          "Soft as a plushie, with dainty antlers. Jumps very high."),
        E + "stal.nog_niet.duinhupper": ("Geheim: vind 10 verstopte knuffels in het Vadsrijk. Dan komt er een duinhupper kijken.",
                                         "Secret: find 10 hidden plushies in the Vadsrijk. Then a dune hopper comes to see you."),
        E + "stal.nog_niet.wolkwalkalfje": ("Ruil 20 Gouden Veren (van de Vahoeg-bazen en de weekkist).",
                                            "Trade 20 Golden Feathers (from the Vahoeg bosses and the weekly chest)."),
        E + "stal.nog_niet.zwamzwijntje": ("Versla de Opperzwam in de Zwamtempel. Er scharrelt dan een biggetje uit de tempel...",
                                           "Defeat the Opperzwam in the Mushroom Temple. A piglet comes trotting out of the temple..."),
        E + "pluimkleur.kristal": ("Kristal", "Crystal"),
        E + "pluimkleur.lampion": ("Lampion", "Lantern"),
        "item.guhs.vr_item_pluimei_kristal": ("Pluimguh-ei (Kristal)", "Pluimguh Egg (Crystal)"),
        "item.guhs.vr_item_pluimei_lampion": ("Pluimguh-ei (Lampion)", "Pluimguh Egg (Lantern)"),
        E + "pluimkleur.bron.kristal": ("Versla Spiegelzucht in De Spiegelburcht (zone 2).", "Defeat Spiegelzucht in the Mirror Castle (zone 2)."),
        E + "pluimkleur.bron.lampion": ("Ruil 12 Gouden Veren (later ook in de Lampionkust).", "Trade 12 Golden Feathers (later also on the Lantern Coast)."),
        "vadsrijk.rijdier.stevig": ("Je Zwamzwijntje schudt de tik van zich af! (de volgende keer schrikt het wel)",
                                    "Your Mushroom Piglet shrugs off the hit! (the next one will scare it)"),
        # the dress-up panel
        R + "tab.info": ("Info", "Info"),
        R + "tab.aankleden": ("Aankleden", "Dress up"),
        R + "as.vacht": ("Vacht", "Coat"),
        R + "as.patroon": ("Patroon", "Pattern"),
        R + "as.tuig": ("Tuig", "Tack"),
        R + "geen": ("Niets (uittrekken)", "Nothing (take off)"),
        R + "aantrekken": ("Aantrekken", "Put on"),
        R + "draagt": ("Draagt dit", "Wearing this"),
        R + "aangekleed": ("Staat je rijdier goed!", "Looks good on your mount!"),
        R + "gekocht": ("Gekocht! Kies het bij Aankleden.", "Bought! Pick it under Dress up."),
        R + "geruild": ("Geruild voor Gouden Veren!", "Traded for Golden Feathers!"),
        R + "te_weinig_veren": ("Te weinig Gouden Veren.", "Not enough Golden Feathers."),
        R + "niet_te_koop": ("Dat is niet te koop.", "That's not for sale."),
        R + "alleen_stal": ("Kopen kan bij de stal.", "You can buy that at the stable."),
        R + "ruil_knop": ("Ruilen (%s veren)", "Trade (%s feathers)"),
        R + "veren": ("Gouden Veren: %s", "Golden Feathers: %s"),
        R + "voortgang": ("Al gevonden: %s / %s", "Found so far: %s / %s"),
        R + "voortgang_kort": (" (%s/%s)", " (%s/%s)"),
        R + "paneel_uitleg": ("Klik een vakje om het te passen. Vacht en patroon veranderen alleen het uiterlijk, nooit de snelheid.",
                              "Click a square to try it on. Coats and patterns only change the look, never the speed."),
        R + "nieuw.patroon": ("Nieuw patroon voor je rijdieren: %1$s! Kies het met %3$s, Aankleden.", "New pattern for your mounts: %1$s! Pick it with %3$s, Dress up."),
        R + "nieuw.tuig": ("Nieuw tuig voor je rijdieren: %1$s! Kies het met %3$s, Aankleden.", "New tack for your mounts: %1$s! Pick it with %3$s, Dress up."),
        R + "nieuw.vacht_van": ("Nieuwe vacht voor je %2$s: %1$s! Kies hem met %3$s, Aankleden.", "New coat for your %2$s: %1$s! Pick it with %3$s, Dress up."),
        R + "tuig_item.uitleg": ("Rechtsklik: dit tuig komt in de kast van al je rijdieren (N, Aankleden).",
                                 "Right-click: this tack goes into all your mounts' wardrobe (N, Dress up)."),
        # sources
        R + "bron.start": ("Hoort bij het rijdier.", "Comes with the mount."),
        R + "bron.stal": ("Te koop bij de stal: %s", "For sale at the stable: %s"),
        R + "bron.veren": ("Ruil %s Gouden Veren (je hebt er %s).", "Trade %s Golden Feathers (you have %s)."),
        R + "bron.baas": ("Versla %s voor het eerst.", "Defeat %s for the first time."),
        R + "bron.prestatie.rijdiermeester": ("Prestatie: krijg een rijdier op level 10.", "Achievement: get a mount to level 10."),
        R + "bron.prestatie.lv10_knuffelhert": ("Prestatie: je Knuffelhert op level 10.", "Achievement: your Knuffelhert at level 10."),
        R + "bron.prestatie.lv10_slakkie": ("Prestatie: Slakkie op level 10 (dat duurt even...).", "Achievement: Slakkie at level 10 (that takes a while...)."),
        R + "bron.prestatie.lv10_duinhupper": ("Prestatie: je Duinhupper op level 10.", "Achievement: your Dune Hopper at level 10."),
        R + "bron.prestatie.huisdieren_4": ("Prestatie: verzamel 4 huisdieren.", "Achievement: collect 4 pets."),
        R + "bron.prestatie.huisdieren_6": ("Prestatie: verzamel 6 huisdieren.", "Achievement: collect 6 pets."),
        R + "bron.prestatie.verkenner_zone1_75": ("Prestatie: het Verkenningsboek van zone 1 op 75%.", "Achievement: the zone 1 Exploration Book at 75%."),
        R + "bron.prestatie.verkenner_zone2_75": ("Prestatie: het Verkenningsboek van zone 2 op 75%.", "Achievement: the zone 2 Exploration Book at 75%."),
        R + "bron.geheim.knuffels_20": ("Geheim: vind 20 verstopte knuffels.", "Secret: find 20 hidden plushies."),
        R + "bron.geheim.knuffels_10": ("Geheim: vind 10 verstopte knuffels.", "Secret: find 10 hidden plushies."),
        R + "bron.geheim.schatten_8": ("Geheim: vind 8 verstopte schatten.", "Secret: find 8 hidden treasures."),
        R + "bron.geheim.schatten_15": ("Geheim: vind 15 verstopte schatten.", "Secret: find 15 hidden treasures."),
        R + "bron.geheim.aanwijzingen_10": ("Geheim: vind 10 aanwijzingen.", "Secret: find 10 clues."),
        # patterns + tack
        R + "patroon.vlekjes": ("Vlekjes", "Spots"),
        R + "patroon.streepjes": ("Streepjes", "Stripes"),
        R + "patroon.hartjes": ("Hartjes", "Hearts"),
        R + "patroon.sterretjes": ("Sterretjes", "Stars"),
        R + "patroon.glitter": ("Glitter", "Glitter"),
        R + "tuig.dekje": ("Zadeldekje", "Saddle blanket"),
        R + "tuig.bloemenkrans": ("Bloemenkrans", "Flower crown"),
        R + "tuig.belletjes": ("Belletjes", "Little bells"),
        R + "tuig.hoedje": ("Hoge hoed", "Top hat"),
        R + "tuig.lampionnetjes": ("Lampionnetjes (gloeien)", "Little lanterns (glowing)"),
        R + "tuig.kroontje": ("Kroontje", "Little crown"),
    }
    for tuig, (nl, en) in (("dekje", ("Zadeldekje", "Saddle Blanket")), ("bloemenkrans", ("Bloemenkrans", "Flower Crown")),
                           ("belletjes", ("Belletjes", "Little Bells")), ("hoedje", ("Hoge hoed", "Top Hat")),
                           ("lampionnetjes", ("Lampionnetjes", "Little Lanterns")), ("kroontje", ("Kroontje", "Little Crown"))):
        t[f"item.guhs.vr_rijtuig_{tuig}"] = (nl + " (rijdiertuig)", en + " (mount tack)")
    vacht = {
        "knuffelhert": {"naturel": ("Naturel", "Natural"), "sneeuw": ("Sneeuwhert", "Snow Deer"), "roze": ("Roze", "Pink"),
                        "honing": ("Honing", "Honey"), "nacht": ("Nachthert", "Night Deer")},
        "guhwolf": {"naturel": ("Sneeuwwit", "Snow White"), "zilver": ("Zilverwolf", "Silver Wolf"), "zon": ("Zonnewolf", "Sun Wolf"),
                    "middernacht": ("Middernacht", "Midnight"), "roze": ("Roze", "Pink")},
        "wolkenschaapje": {"naturel": ("Wolkwit", "Cloud White"), "suikerspin": ("Suikerspin", "Candy Floss"), "lavendel": ("Lavendel", "Lavender"),
                           "onweer": ("Onweerswolkje", "Thundercloud"), "karamel": ("Karamel", "Caramel")},
        "slakkie": {"naturel": ("Oranje huisje", "Orange Shell"), "mint": ("Munthuisje", "Mint Shell"), "lavendel": ("Lavendelhuisje", "Lavender Shell"),
                    "honing": ("Honinghuisje", "Honey Shell"), "nacht": ("Sterrenhuisje", "Starry Shell")},
        "duinhupper": {"naturel": ("Duinzand", "Dune Sand"), "sneeuw": ("Sneeuwhaas", "Snow Hare"), "roze": ("Roze", "Pink"),
                       "nacht": ("Nachthaas", "Night Hare")},
        "wolkwalkalfje": {"naturel": ("Hemelsblauw", "Sky Blue"), "zonsopkomst": ("Zonsopkomst", "Sunrise"),
                          "sterrenhemel": ("Sterrenhemel", "Starry Sky"), "goud": ("Goudwolk", "Gold Cloud")},
        "zwamzwijntje": {"naturel": ("Rode zwam", "Red Mushroom"), "blauwzwam": ("Blauwe zwam", "Blue Mushroom"),
                         "dooier": ("Dooierzwam", "Yolk Mushroom"), "glimzwam": ("Glimzwam", "Glow Mushroom")},
    }
    for soort, d in vacht.items():
        for k, v in d.items():
            t[f"{R}vacht.{soort}.{k}"] = v
    # pets
    t.update({
        H + "rol.steuner": ("Steuner", "Supporter"),
        H + "rol.snuffelaar": ("Snuffelaar", "Sniffer"),
        H + "kracht.klodderschild.naam": ("Klodderschild", "Blob Shield"),
        H + "kracht.klodderschild.tekst": ("Zakt je Vads in een gevecht onder de helft, dan legt hij een plakkerig schildje om je heen (elke 30 s).",
                                           "When your Vads drops below half in a fight, it wraps you in a sticky little shield (every 30 s)."),
        H + "kracht.klodderschild.doet": ("%s geeft je een klodderschildje!", "%s gives you a blob shield!"),
        H + "kracht.koninklijk_kussentje.naam": ("Koninklijk kussentje", "Royal Cushion"),
        H + "kracht.koninklijk_kussentje.tekst": ("Buiten een gevecht krijg je elke seconde wat Vads terug.",
                                                  "Out of a fight you get a bit of Vads back every second."),
        H + "kracht.galmlied.naam": ("Galmlied", "Echo Song"),
        H + "kracht.galmlied.tekst": ("In een gevecht zingt hij je elke 10 s wat energie toe.", "In a fight it sings you some energy every 10 s."),
        H + "kracht.kruimeltjes_rapen.naam": ("Kruimeltjes rapen", "Crumb Collector"),
        H + "kracht.kruimeltjes_rapen.tekst": ("Raapt jouw buit op die om je heen ligt, en vindt soms een extra muntje bij een overwinning.",
                                               "Picks up your loot lying around you, and sometimes finds a bonus coin when you win a fight."),
        H + "kracht.scherp_oog.naam": ("Scherp oog", "Sharp Eye"),
        H + "kracht.scherp_oog.tekst": ("Ruikt geheimen in de buurt (knuffels, schatten, aanwijzingen) en wijst ze aan met een gouden spoor. Vindt soms een extra muntje.",
                                        "Sniffs out secrets nearby (plushies, treasures, clues) and points them out with a golden trail. Sometimes finds a bonus coin."),
        H + "kracht.drakenwarmte.naam": ("Drakenwarmte", "Dragon Warmth"),
        H + "kracht.drakenwarmte.tekst": ("Warmt je op buiten een gevecht. Zakt je Vads onder een derde, dan blaast ze een roze vlammetje dat je geneest. Na Hoogvads (zone 5) groeit ze: sterker vlammetje plus een schildje.",
                                          "Warms you up out of a fight. When your Vads drops below a third, she breathes a pink flame that heals you. After Hoogvads (zone 5) she grows: a stronger flame plus a shield."),
        H + "kracht.drakenwarmte.doet": ("%s blaast een roze vlammetje: je voelt je beter!", "%s breathes a pink flame: you feel better!"),
        H + "kracht.drakenwarmte.doet_groot": ("%s blaast een grote roze vlam: genezen en beschermd!", "%s breathes a big pink flame: healed and shielded!"),
        H + "kracht.snuffeltje.naam": ("Snuffeltje", "Little Sniffer"),
        H + "kracht.snuffeltje.tekst": ("Raapt jouw buit op die vlakbij ligt.", "Picks up your loot lying close by."),
        H + "level_op": ("%s is nu level %s! Zijn kracht wordt een beetje sterker.", "%s is now level %s! Its ability gets a little stronger."),
        H + "bonus": ("%s vond nog een muntje: %s!", "%s found a bonus coin: %s!"),
        H + "snuffel": ("%s ruikt iets! Volg het gouden spoor...", "%s smells something! Follow the golden trail..."),
        "vadsrijk.huisdieren.rol": ("Rol: %s", "Role: %s"),
        "vadsrijk.huisdieren.kracht": ("Kracht: %s", "Ability: %s"),
        "vadsrijk.huisdieren.level": ("Level %s / %s", "Level %s / %s"),
        "vadsrijk.huisdieren.passief": ("Altijd: %s", "Always: %s"),
        "vadsrijk.huisdieren.stadium.0": ("Klein draakje", "Little dragon"),
        "vadsrijk.huisdieren.stadium.1": ("Jonge draak (gegroeid na Hoogvads)", "Young dragon (grown after Hoogvads)"),
        "vadsrijk.huisdieren.geen_schade": ("Huisdieren vechten niet mee en worden nooit geraakt: ze helpen je.",
                                            "Pets never fight and are never hit: they help you."),
    })
    return t


# the passive lines in the P screen (stat names must be clear: the user's rule)
PASSIEF = {
    "klodderschild": ("Elementweerstand +2%", "Element resistance +2%"),
    "koninklijk_kussentje": ("Genezen +3%", "Healing +3%"),
    "galmlied": ("Loopsnelheid +2%", "Walking speed +2%"),
    "kruimeltjes_rapen": ("Loopsnelheid +2%", "Walking speed +2%"),
    "scherp_oog": ("Loopsnelheid +2%", "Walking speed +2%"),
    "drakenwarmte": ("Genezen +2% (gegroeid: Genezen +4%, Loopsnelheid +2%)", "Healing +2% (grown: Healing +4%, Walking speed +2%)"),
    "snuffeltje": ("niets", "nothing"),
}


def alle_teksten():
    t = teksten()
    for k, v in PASSIEF.items():
        t[f"{H}kracht.{k}.passief"] = v
    return t


def build(h):
    assets(h.A, h.w)
    for k, (nl, en) in alle_teksten().items():
        h.lang(k, en, nl)   # (make_v2.lang(key, en, nl))


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    n = assets(a)
    t = alle_teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in t.items()})
        _schrijf_json(p, data)
    print(f"vadsrijk_t_rijdier: 3 new mounts, {n} coat textures, {len(TUIGEN)} tack items, {len(t)} texts")


if __name__ == "__main__":
    _direct()
