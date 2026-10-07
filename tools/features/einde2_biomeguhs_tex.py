"""
einde2, slice biomeguhs: the looks of the six guhs of the outer islands.

  - BONES: their variant bones on the guh model (tools/make_guh_variants.py adds them; GuhVariant shows the bones whose
    name starts with the variant id). The guh sheet has no room for new swatches, so every bone samples the swatch PLACE of
    an older variant (pluis_kuif, koning_goud, ...) and each of our textures paints its own colours there.
  - variants(v): fur colour + swatch painters per variant (the hook of make_guh_variants.py).
  - vacht(h): the patterns ON the fur (cheese holes, cracks, crumbs, blue veins, golden freckles, the chorus rings), painted
    from guh.png every time (so a second run changes nothing), then the sleeping eyes again and the glow masks.
  - preview(h, out): pictures of all six for a look (python tools/features/einde2_biomeguhs_tex.py from the project root).

Every painter draws with kit.rng(<name>): the rng make_guh_variants hands a module depends on its place in FEATURES.
"""
import json
import math
import os

import numpy as np
from PIL import Image

import features.einde2_kit as kit

PX = 32                                    # a swatch is 8 x 8 UV units = 32 x 32 texture pixels
_H, _B, _T = [0, 6, -2], [0, 6, 6], [0, 1.5, 12]          # head, body and tail pivots of the guh model
_EL, _ER = [7.5, 11, -5.5], [-7.5, 11, -5.5]              # the ear pivots

# variant id: fur colour
VACHT = {
    "korstguh": (238, 198, 88),            # gatenkaas yellow
    "duinguh": (228, 198, 146),            # dry crust sand
    "kruimelguh": (240, 220, 180),         # a fresh bun
    "schimmelguh": (220, 226, 212),        # blue cheese, the pale part
    "kristalguh": (200, 186, 166),         # klipkaas grey
    "koorguh": (178, 122, 200),            # knabbelkoor purple
}


def _spiegel(cubes):
    return [([round(-(o[0] + s[0]), 2), o[1], o[2]], list(s), i) for o, s, i in cubes]


def _ring(cx, cy, rx, ry, z, maat, diep, stap, weg=(), inflate=0.0):
    """Little cubes on an ellipse in the x-y plane (a collar); angles in `weg` ranges are left out."""
    out = []
    for a in range(0, 360, stap):
        if any(lo < a < hi for lo, hi in weg):
            continue
        x, y = cx + rx * math.cos(math.radians(a)), cy + ry * math.sin(math.radians(a))
        out.append(([round(x - maat / 2, 2), round(y - maat / 2, 2), z], [maat, maat, diep], inflate))
    return out


# --- Korstguh: a crust shield in bands over its back (it rolls up inside it), a crust cap and a crust tail nub ---------
_BANDEN = []
for _i, _z in enumerate((-2.7, 0.9, 4.5, 8.1)):
    _inf = 0.3 if _i % 2 == 0 else 0.0     # every other band lies over its neighbours
    _BANDEN += [([-5.4, 10.7, _z], [10.8, 1.0, 3.6], _inf), ([-7.2, 5.0, _z], [1.0, 5.2, 3.6], _inf), ([6.2, 5.0, _z], [1.0, 5.2, 3.6], _inf),
                ([-6.6, 9.6, _z], [1.6, 1.5, 3.6], _inf), ([5.0, 9.6, _z], [1.6, 1.5, 3.6], _inf)]
_KORST = {
    "korstguh_schild": ("body", _B, "koning_goud", _BANDEN),
    "korstguh_kap": ("head", _H, "koning_goud", [([-5.2, 15.1, -9.6], [10.4, 0.9, 8.2], 0), ([-3.6, 15.9, -8.6], [7.2, 0.7, 6.2], 0)]),
    "korstguh_staart": ("tail", _T, "koning_goud", [([-1.5, 2.3, 12.0], [3.0, 2.4, 3.4], 0), ([-1.0, 2.7, 15.2], [2.0, 1.6, 1.6], 0)]),
}

# --- Duinguh: tall desert ears (all you see of it when it has dug in), cheek tufts and a bushy tail with a dark tip -----
_OOR = [([4.6, 16.4, -6.5], [6.8, 3.4, 1.8], 0), ([5.4, 19.6, -6.4], [5.4, 3.0, 1.6], 0), ([6.2, 22.4, -6.3], [3.8, 2.6, 1.4], 0)]
_OOR_BINNEN = [([5.9, 11.4, -6.8], [4.4, 8.6, 0.3], 0), ([6.5, 19.8, -6.7], [3.2, 3.4, 0.3], 0)]
_OOR_PUNT = [([7.2, 25.0, -6.2], [1.8, 1.3, 1.2], 0)]
_DUIN = {
    "duinguh_oor_links": ("ear_left", _EL, "pluis_kuif", _OOR), "duinguh_oor_rechts": ("ear_right", _ER, "pluis_kuif", _spiegel(_OOR)),
    "duinguh_oor_links_binnen": ("ear_left", _EL, "pluis_wang", _OOR_BINNEN),
    "duinguh_oor_rechts_binnen": ("ear_right", _ER, "pluis_wang", _spiegel(_OOR_BINNEN)),
    "duinguh_oor_links_punt": ("ear_left", _EL, "ender_bone", _OOR_PUNT), "duinguh_oor_rechts_punt": ("ear_right", _ER, "ender_bone", _spiegel(_OOR_PUNT)),
    "duinguh_wangpluis": ("head", _H, "pluis_kuif", [([6.9, 3.4, -11.0], [1.6, 3.4, 3.2], 0), ([-8.5, 3.4, -11.0], [1.6, 3.4, 3.2], 0),
                                                    ([7.6, 2.6, -10.2], [1.2, 1.6, 1.8], 0), ([-8.8, 2.6, -10.2], [1.2, 1.6, 1.8], 0)]),
    "duinguh_staart": ("tail", _T, "pluis_kuif", [([-1.6, 2.2, 12.2], [3.2, 2.8, 3.8], 0), ([-2.1, 1.9, 15.6], [4.2, 3.4, 3.2], 0)]),
    "duinguh_staart_punt": ("tail", _T, "ender_bone", [([-1.6, 2.3, 18.6], [3.2, 2.6, 1.8], 0)]),
}

# --- Kruimelguh: a little cloud on its back, puffy floaters at its sides, cloud socks and tail, crumbs in its fur --------
_VLEUGEL = [([6.3, 6.0, 1.6], [2.2, 3.4, 5.0], 0.1), ([8.1, 6.6, 2.3], [1.7, 2.4, 3.6], 0)]
_KRUIMEL = {
    "kruimelguh_wolk": ("body", _B, "wolk_pluis", [([-4.0, 11.2, 1.0], [8.0, 2.4, 7.0], 0.2), ([-5.6, 10.8, 3.0], [3.0, 2.0, 4.0], 0.1),
                                                  ([2.6, 10.8, 3.0], [3.0, 2.0, 4.0], 0.1), ([-2.5, 13.4, 2.5], [5.0, 1.6, 4.4], 0)]),
    "kruimelguh_vleugel_links": ("body", [6.6, 8, 4], "wolk_pluis", _VLEUGEL),
    "kruimelguh_vleugel_rechts": ("body", [-6.6, 8, 4], "wolk_pluis", _spiegel(_VLEUGEL)),
    "kruimelguh_staart": ("tail", _T, "wolk_pluis", [([-2.4, 1.6, 11.0], [4.8, 3.6, 3.4], 0.2), ([-1.6, 4.2, 12.4], [3.2, 2.4, 2.8], 0.1),
                                                    ([-1.2, 2.0, 13.8], [2.4, 2.4, 2.2], 0)]),
    "kruimelguh_sok_fl": ("leg_front_left", [2, 1, 0], "wolk_pluis", [([4.25, 0, -2.5], [3.5, 1.6, 4.5], 0.5)]),
    "kruimelguh_sok_fr": ("leg_front_right", [-6, 1, 0], "wolk_pluis", [([-7.75, 0, -2.5], [3.5, 1.6, 4.5], 0.5)]),
    "kruimelguh_sok_bl": ("leg_back_left", [6, 1, 9], "wolk_pluis", [([4.5, 0, 7], [3.5, 1.6, 4.5], 0.5)]),
    "kruimelguh_sok_br": ("leg_back_right", [-6, 1, 9], "wolk_pluis", [([-8, 0, 7], [3.5, 1.6, 4.5], 0.5)]),
    "kruimelguh_kruimels": ("head", _H, "koning_goud", [([-4.6, 15.3, -8.0], [1.4, 1.0, 1.4], 0), ([2.6, 15.3, -5.4], [1.2, 0.9, 1.2], 0),
                                                       ([-0.6, 15.3, -10.2], [1.0, 0.8, 1.0], 0), ([3.6, 15.2, -9.6], [0.9, 0.7, 0.9], 0),
                                                       ([-2.4, 15.2, -4.6], [0.8, 0.6, 0.8], 0)]),
    "kruimelguh_kruimels_rug": ("body", _B, "koning_goud", [([-5.2, 10.5, 8.6], [1.3, 0.9, 1.3], 0), ([3.6, 10.5, 9.2], [1.1, 0.8, 1.1], 0),
                                                          ([0.4, 11.0, 9.9], [1.0, 0.8, 1.0], 0), ([-1.6, 15.0, 4.0], [0.9, 0.6, 0.9], 0)]),
}

# --- Schimmelguh: glowing little mushrooms on its head, back and tail, glowing cheeks -----------------------------------
_SCHIMMEL = {
    "schimmelguh_steel": ("head", _H, "koning_manen", [([-4.7, 15.2, -6.7], [1.3, 2.4, 1.3], 0), ([3.2, 15.2, -4.5], [1.0, 1.6, 1.0], 0)]),
    "schimmelguh_hoed": ("head", _H, "zeemeer_vin", [([-5.9, 17.4, -7.9], [3.7, 1.2, 3.7], 0), ([-5.25, 18.6, -7.25], [2.4, 0.8, 2.4], 0),
                                                    ([2.25, 16.6, -5.45], [2.9, 1.0, 2.9], 0), ([2.85, 17.6, -4.85], [1.7, 0.6, 1.7], 0)]),
    "schimmelguh_steel_rug": ("body", _B, "koning_manen", [([-2.4, 11.2, 4.5], [1.3, 2.8, 1.3], 0), ([2.6, 11.0, 7.5], [1.0, 1.8, 1.0], 0),
                                                          ([-4.2, 10.6, 8.3], [0.9, 1.6, 0.9], 0)]),
    "schimmelguh_hoed_rug": ("body", _B, "zeemeer_vin", [([-3.75, 13.8, 3.15], [4.0, 1.3, 4.0], 0), ([-3.05, 15.1, 3.85], [2.6, 0.8, 2.6], 0),
                                                        ([1.5, 12.6, 6.4], [3.2, 1.1, 3.2], 0), ([2.1, 13.7, 7.0], [2.0, 0.6, 2.0], 0),
                                                        ([-5.1, 12.0, 7.4], [2.7, 0.9, 2.7], 0), ([-4.55, 12.9, 7.95], [1.6, 0.5, 1.6], 0)]),
    "schimmelguh_staart": ("tail", _T, "zeemeer_vin", [([-1.6, 2.4, 17.2], [3.2, 3.0, 1.3], 0), ([-1.0, 3.0, 18.5], [2.0, 1.8, 0.8], 0)]),
    "schimmelguh_wangen": ("head", _H, "asguh_wang", [([4.4, 5.0, -12.4], [3.0, 1.9, 0.45], 0), ([-7.4, 5.0, -12.4], [3.0, 1.9, 0.45], 0)]),
}

# --- Kristalguh: golden kaaskristallen growing out of its back, a little tiara of them, one on its tail ------------------
# (three back bones: the outer two lean outwards, client side: E2BiomeguhsClient)
_KRISTAL = {
    "kristalguh_kristal": ("body", [0, 11, 4], "koning_goud", [([-1.3, 11.0, 2.0], [2.6, 5.2, 2.6], 0), ([-0.9, 10.9, 7.4], [1.8, 3.0, 1.8], 0)]),
    "kristalguh_kristal_punt": ("body", [0, 11, 4], "koning_manen", [([-0.7, 16.2, 2.6], [1.4, 1.3, 1.4], 0), ([-0.45, 13.9, 7.85], [0.9, 0.9, 0.9], 0)]),
    "kristalguh_kristal_links": ("body", [3, 10.5, 6], "koning_goud", [([1.9, 10.6, 4.4], [2.0, 4.0, 2.0], 0), ([3.2, 10.3, 8.6], [1.4, 2.2, 1.4], 0)]),
    "kristalguh_kristal_links_punt": ("kristalguh_kristal_links", [3, 10.5, 6], "koning_manen", [([2.4, 14.6, 4.9], [1.0, 1.0, 1.0], 0)]),
    "kristalguh_kristal_rechts": ("body", [-3, 10.5, 6], "koning_goud", [([-3.9, 10.6, 3.4], [1.8, 3.4, 1.8], 0), ([-4.6, 10.3, 7.8], [1.5, 2.4, 1.5], 0)]),
    "kristalguh_kristal_rechts_punt": ("kristalguh_kristal_rechts", [-3, 10.5, 6], "koning_manen", [([-3.5, 14.0, 3.8], [1.0, 0.9, 1.0], 0)]),
    "kristalguh_tiara": ("head", _H, "koning_goud", [([-1.0, 15.2, -8.8], [2.0, 3.0, 2.0], 0), ([-4.3, 15.1, -7.8], [1.5, 1.9, 1.5], 0),
                                                    ([2.8, 15.1, -7.8], [1.5, 1.9, 1.5], 0)]),
    "kristalguh_tiara_punt": ("head", _H, "koning_manen", [([-0.5, 18.2, -8.3], [1.0, 1.0, 1.0], 0)]),
    "kristalguh_staart": ("tail", _T, "koning_goud", [([-1.3, 2.3, 16.6], [2.6, 2.6, 2.6], 0)]),
    "kristalguh_staart_punt": ("tail", _T, "koning_manen", [([-0.7, 2.9, 19.2], [1.4, 1.4, 1.2], 0)]),
}

# --- Koorguh: a knabbelkoor flower on its head, a white choir collar ("koor"), a knabbelkoor twig for a tail --------------
_KOOR = {
    "koorguh_kraag": ("body", _B, "koning_manen", _ring(0, 6.4, 7.3, 5.6, -1.6, 3.0, 2.4, 30, weg=((225, 315),), inflate=0.1)),
    "koorguh_steel": ("head", _H, "pluis_wang", [([-0.7, 15.2, -6.5], [1.4, 1.8, 1.4], 0)]),
    "koorguh_bloem": ("head", _H, "pluis_kuif", [([-2.2, 16.9, -8.0], [4.4, 3.6, 4.4], 0)]),
    "koorguh_bloem_knop": ("head", _H, "koning_manen", [([-0.9, 20.5, -6.7], [1.8, 0.7, 1.8], 0), ([-2.9, 17.9, -6.7], [0.7, 1.6, 1.8], 0),
                                                       ([2.2, 17.9, -6.7], [0.7, 1.6, 1.8], 0), ([-0.9, 17.9, -8.7], [1.8, 1.6, 0.7], 0),
                                                       ([-0.9, 17.9, -4.3], [1.8, 1.6, 0.7], 0)]),
    "koorguh_staart": ("tail", _T, "pluis_wang", [([-0.9, 2.6, 12.0], [1.8, 1.8, 5.6], 0), ([-0.9, 4.4, 15.8], [1.8, 3.8, 1.8], 0)]),
    "koorguh_staart_bloem": ("tail", _T, "pluis_kuif", [([-1.7, 8.0, 15.0], [3.4, 3.0, 3.4], 0)]),
    "koorguh_staart_knop": ("tail", _T, "koning_manen", [([-0.7, 11.0, 16.0], [1.4, 0.6, 1.4], 0)]),
}

BONES = {**_KORST, **_DUIN, **_KRUIMEL, **_SCHIMMEL, **_KRISTAL, **_KOOR}
# the swatches that glow in the dark, per variant (the glow masks)
GLOEIT = {"schimmelguh": ("zeemeer_vin", "asguh_wang"), "kristalguh": ("koning_goud", "koning_manen")}


# =====================================================================================================================
# swatch painters (32 x 32 float RGB, like make_guh_variants.fabric)
# =====================================================================================================================
def _stof(colour, rng, noise=10):
    base = np.array(colour, np.float32)
    return np.clip(base + rng.normal(0, noise / 2, (PX, PX, 1)) + rng.normal(0, noise / 4, (PX, PX, 3)), 0, 255)


def _vlek(a, cx, cy, rx, ry, colour, mix=1.0):
    yy, xx = np.mgrid[0:a.shape[0], 0:a.shape[1]]
    m = ((xx - cx) / max(0.5, rx)) ** 2 + ((yy - cy) / max(0.5, ry)) ** 2 <= 1
    a[m] = a[m] * (1 - mix) + np.array(colour, np.float32) * mix
    return m


def korst(rng):
    """The Korstguh's shield: baked crust, darker cracks, a light baked edge and a few cheese holes."""
    a = _stof((184, 124, 50), rng, 14)
    a[:3] = a[:3] * 0.55 + np.array((226, 176, 92)) * 0.45
    a[-3:] *= 0.8
    for _ in range(5):
        x, y = rng.integers(2, PX - 2), rng.integers(4, PX - 4)
        for _ in range(int(rng.integers(5, 12))):
            a[y, x] = (132, 84, 34)
            x, y = int(np.clip(x + rng.integers(-1, 2), 0, PX - 1)), int(np.clip(y + rng.integers(0, 2), 0, PX - 1))
    for cx, cy, r in ((8, 11, 3.2), (23, 20, 2.6), (17, 7, 1.8)):
        _vlek(a, cx, cy, r, r, (128, 80, 30))
        _vlek(a, cx, cy - 0.8, r * 0.7, r * 0.6, (104, 62, 24))
    return np.clip(a, 0, 255)


def duin_vacht(rng):
    a = _stof(VACHT["duinguh"], rng, 12)
    a[:6] = a[:6] * 0.86                                   # a little darker towards the ear tips
    return a


def duin_binnen(rng):
    a = _stof((238, 178, 156), rng, 8)
    a[:, :3] = a[:, :3] * 0.9
    a[:, -3:] = a[:, -3:] * 0.9
    return a


def duin_punt(rng):
    return _stof((112, 80, 56), rng, 10)


def wolk(rng):
    """The Kruimelguh's cloud fluff: soft cream with lighter puffs and a few toasted crumbs caught in it."""
    a = _stof((252, 246, 232), rng, 6)
    for _ in range(7):
        _vlek(a, rng.integers(2, PX - 2), rng.integers(2, PX - 2), rng.integers(3, 6), rng.integers(2, 5), (255, 253, 246), 0.7)
    a[-4:] = a[-4:] * 0.93
    for _ in range(9):
        x, y = rng.integers(1, PX - 2, 2)
        a[y:y + 2, x:x + 2] = (214, 168, 104) if rng.random() < 0.6 else (236, 204, 150)
    return np.clip(a, 0, 255)


def kruimel(rng):
    a = _stof((208, 156, 88), rng, 16)
    for _ in range(14):
        x, y = rng.integers(0, PX - 2, 2)
        a[y:y + 2, x:x + 2] = (168, 116, 58)
    a[:4] = a[:4] * 0.6 + np.array((244, 210, 150)) * 0.4
    return np.clip(a, 0, 255)


def steel(rng):
    a = _stof((234, 238, 224), rng, 6)
    a[:, ::5] = a[:, ::5] * 0.93
    return a


def hoed(rng):
    """A glowing mushroom cap: teal, lighter in the middle, with pale spots."""
    a = _stof((58, 184, 180), rng, 8)
    yy, xx = np.mgrid[0:PX, 0:PX]
    d = np.sqrt((xx - PX / 2 + 0.5) ** 2 + (yy - PX / 2 + 0.5) ** 2) / (PX / 2)
    a = a * np.clip(1.12 - 0.3 * d, 0.7, 1.12)[..., None]
    for cx, cy, r in ((9, 9, 3), (22, 12, 2.4), (14, 22, 2.8), (25, 25, 1.8), (5, 24, 1.6)):
        _vlek(a, cx, cy, r, r, (206, 255, 240))
    a[-3:] = a[-3:] * 0.7 + np.array((30, 110, 124)) * 0.3
    return np.clip(a, 0, 255)


def wang(rng):
    a = _stof((70, 196, 190), rng, 10)
    _vlek(a, PX / 2, PX / 2, PX * 0.3, PX * 0.3, (196, 255, 240), 0.85)
    return np.clip(a, 0, 255)


def kristal(rng):
    """Golden kaaskristal: light at the top, deep gold at the foot, slanted facets."""
    a = np.zeros((PX, PX, 3), np.float32)
    top, voet = np.array((255, 240, 160), np.float32), np.array((232, 162, 44), np.float32)
    for y in range(PX):
        a[y, :] = top + (voet - top) * (y / (PX - 1)) ** 0.8
    for x0 in (-6, 10, 22):                                 # facets: a light stripe with a dark one beside it
        for y in range(PX):
            x = x0 + y // 2
            if 0 <= x < PX:
                a[y, x] = np.minimum(a[y, x] + 26, 255)
            if 0 <= x + 5 < PX:
                a[y, x + 5] = a[y, x + 5] * 0.86
    a[:, :2] *= 0.88
    a[:, -2:] *= 0.9
    return np.clip(a + rng.normal(0, 3, (PX, PX, 1)), 0, 255)


def kristal_punt(rng):
    a = _stof((255, 250, 206), rng, 4)
    a[-6:] = a[-6:] * 0.5 + np.array((250, 220, 120)) * 0.5
    return np.clip(a, 0, 255)


def bloem(rng):
    """A knabbelkoor flower: purple, darker at the edges, a paler heart."""
    a = _stof((164, 100, 194), rng, 10)
    yy, xx = np.mgrid[0:PX, 0:PX]
    rand = np.minimum(np.minimum(xx, PX - 1 - xx), np.minimum(yy, PX - 1 - yy))
    a = a * np.clip(0.78 + rand * 0.045, 0.78, 1.06)[..., None]
    _vlek(a, PX / 2, PX / 2, 6, 6, (214, 170, 232), 0.55)
    return np.clip(a, 0, 255)


def koor_steel(rng):
    a = _stof((112, 68, 138), rng, 10)
    for y in range(4, PX, 9):                               # the knots of the stalk
        a[y:y + 2] = a[y:y + 2] * 0.74
    a[:, :3] = a[:, :3] * 1.1
    return np.clip(a, 0, 255)


def kraag(rng):
    """The choir collar and the flower's buds: cream with fine pleats."""
    a = _stof((248, 240, 218), rng, 5)
    a[:, ::4] = a[:, ::4] * 0.9
    a[-4:] = a[-4:] * 0.92
    return np.clip(a, 0, 255)


def variants(v):
    """fur colour + {swatch: painter} per variant (called through einde2_biomeguhs.variants)."""
    def met(naam, **painters):
        return VACHT[naam], {swatch: (lambda p=p, s=swatch: p(kit.rng(f"{naam}/{s}"))) for swatch, p in painters.items()}
    return {
        "korstguh": met("korstguh", koning_goud=korst),
        "duinguh": met("duinguh", pluis_kuif=duin_vacht, pluis_wang=duin_binnen, ender_bone=duin_punt),
        "kruimelguh": met("kruimelguh", wolk_pluis=wolk, koning_goud=kruimel),
        "schimmelguh": met("schimmelguh", koning_manen=steel, zeemeer_vin=hoed, asguh_wang=wang),
        "kristalguh": met("kristalguh", koning_goud=kristal, koning_manen=kristal_punt),
        "koorguh": met("koorguh", pluis_kuif=bloem, pluis_wang=koor_steel, koning_manen=kraag),
    }


# =====================================================================================================================
# the patterns on the fur
# =====================================================================================================================
def _vlakken(geo, bone, faces=("up", "east", "west", "south", "north", "down")):
    from features import guhpolder_tex as gt
    return [cube[f] for cube in gt._faces(geo, bone) for f in faces if f in cube]


def _masker(shape):
    return np.zeros(shape, bool)


def _stippen(m, r, rng, n, r0, r1, rond=True):
    """n random dots (ellipses) inside a rectangle."""
    x0, y0, x1, y1 = r
    if x1 - x0 < 3 or y1 - y0 < 3:
        return
    yy, xx = np.mgrid[y0:y1, x0:x1]
    for _ in range(n):
        cx, cy = rng.uniform(x0, x1), rng.uniform(y0, y1)
        rx = rng.uniform(r0, r1)
        ry = rx if rond else rng.uniform(r0, r1)
        m[y0:y1, x0:x1] |= ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2 <= 1


def _lijnen(m, r, rng, n, lengte, dik=1, recht=0.6):
    """n wandering lines inside a rectangle (cracks, veins); `recht` = how much a line keeps its direction."""
    x0, y0, x1, y1 = r
    if x1 - x0 < 4 or y1 - y0 < 4:
        return
    for _ in range(n):
        x, y = rng.uniform(x0, x1), rng.uniform(y0, y1)
        a = rng.uniform(0, 2 * math.pi)
        for stap in range(int(lengte)):
            xi, yi = int(x), int(y)
            if not (x0 <= xi < x1 and y0 <= yi < y1):
                break
            m[yi:min(y1, yi + dik), xi:min(x1, xi + dik)] = True
            a += rng.normal(0, 1 - recht)
            x, y = x + math.cos(a), y + math.sin(a)
            if stap and rng.random() < 0.06:                # a side branch
                _lijnen(m, (max(x0, xi - 1), max(y0, yi - 1), x1, y1), rng, 1, lengte / 3, dik, recht)


def _aantal(r, per=260.0):
    return max(1, int((r[2] - r[0]) * (r[3] - r[1]) / per))


def _buik(p, geo, colour):
    """A lighter belly, chest and chin (keeps the fur's own shading)."""
    for cube_faces in (_vlakken(geo, "body", ("down",)), _vlakken(geo, "head", ("down",))):
        for r in cube_faces:
            p.rect(r, colour)
    from features import guhpolder_tex as gt
    for cube in gt._faces(geo, "body"):
        for side in ("east", "west"):
            p.rect(cube[side], colour, rows=(0.78, 1.0))


def _korstguh(p, geo, rng):
    """Gatenkaas: round holes all over, darker in the middle; a creamy belly."""
    _buik(p, geo, (250, 226, 150))
    gat, diep = _masker(p.fur.shape), _masker(p.fur.shape)
    for bone, faces in (("body", ("up", "east", "west", "south")), ("head", ("up", "east", "west", "south")), ("tail", ("up", "east", "west"))):
        for r in _vlakken(geo, bone, faces):
            sub = kit.rng(f"korstguh/gat/{r}")
            n = _aantal(r, 330.0)
            stand = sub.bit_generator.state
            _stippen(gat, r, sub, n, 2.4, 4.6)
            sub.bit_generator.state = stand               # the same spots again, smaller: the dark inside of a hole
            _stippen(diep, r, sub, n, 1.2, 2.6)
    p.recolour(gat, (214, 160, 56))
    p.recolour(diep & gat, (186, 128, 40))


def _duinguh(p, geo, rng):
    """Dry cracked crust on its back, a pale belly, darker little socks."""
    _buik(p, geo, (246, 230, 194))
    barst = _masker(p.fur.shape)
    for bone, faces in (("body", ("up", "east", "west", "south")), ("head", ("up", "east", "west"))):
        for r in _vlakken(geo, bone, faces):
            _lijnen(barst, r, kit.rng(f"duinguh/barst/{r}"), _aantal(r, 420.0), 16, 1, 0.55)
    p.recolour(barst, (186, 150, 100))
    for leg in ("leg_back_left", "leg_back_right", "leg_front_left", "leg_front_right"):
        for r in _vlakken(geo, leg):
            p.rect(r, (206, 170, 118), rows=(0.55, 1.0))


def _kruimelguh(p, geo, rng):
    """Crumbs all through its fur: toasted and golden specks."""
    _buik(p, geo, (255, 250, 238))
    for kleur, per, naam in (((212, 166, 100), 150.0, "a"), ((236, 204, 148), 110.0, "b"), ((184, 132, 72), 420.0, "c")):
        m = _masker(p.fur.shape)
        for bone in ("body", "head", "tail", "ear_left", "ear_right"):
            for r in _vlakken(geo, bone, ("up", "east", "west", "south")):
                _stippen(m, r, kit.rng(f"kruimelguh/{naam}/{r}"), _aantal(r, per), 0.8, 1.5)
        p.recolour(m, kleur)


def _schimmelguh(p, geo, rng):
    """Blue cheese: blue-green veins wandering over its back and head. Returns the vein mask (it glows)."""
    _buik(p, geo, (238, 242, 230))
    ader, licht = _masker(p.fur.shape), _masker(p.fur.shape)
    for bone, faces in (("body", ("up", "east", "west", "south")), ("head", ("up", "east", "west", "south")), ("tail", ("up",))):
        for r in _vlakken(geo, bone, faces):
            _lijnen(ader, r, kit.rng(f"schimmelguh/ader/{r}"), _aantal(r, 300.0), 22, 2, 0.5)
            _stippen(licht, r, kit.rng(f"schimmelguh/vlek/{r}"), _aantal(r, 700.0), 1.5, 2.6)
    p.recolour(licht, (150, 196, 196))
    p.recolour(ader, (62, 132, 152))
    return (ader | licht) & p.fur


def _kristalguh(p, geo, rng):
    """Klipkaas grey with golden freckles (kristalkaas dust) and a pale belly."""
    _buik(p, geo, (226, 216, 200))
    goud, glim = _masker(p.fur.shape), _masker(p.fur.shape)
    for bone in ("body", "head", "tail"):
        for r in _vlakken(geo, bone, ("up", "east", "west", "south")):
            _stippen(goud, r, kit.rng(f"kristalguh/goud/{r}"), _aantal(r, 170.0), 0.9, 1.7)
            _stippen(glim, r, kit.rng(f"kristalguh/glim/{r}"), _aantal(r, 600.0), 0.6, 1.0)
    p.recolour(goud, (240, 194, 84))
    p.recolour(glim, (255, 240, 170))
    for leg in ("leg_back_left", "leg_back_right", "leg_front_left", "leg_front_right"):
        for r in _vlakken(geo, leg):
            p.rect(r, (170, 154, 134), rows=(0.5, 1.0))


def _koorguh(p, geo, rng):
    """Knabbelkoor purple: a lilac belly and chest, darker rings over its back like the knots of the plant."""
    _buik(p, geo, (226, 196, 236))
    from features import guhpolder_tex as gt
    for cube in gt._faces(geo, "body"):
        p.rect(cube["north"], (226, 196, 236), rows=(0.3, 1.0))
        for a, b in ((0.16, 0.24), (0.5, 0.58), (0.84, 0.92)):          # rings: bands across the back and down the sides
            x0, y0, x1, y1 = cube["up"]
            m = _masker(p.fur.shape)
            m[y0 + int(a * (y1 - y0)):y0 + int(b * (y1 - y0)) + 1, x0:x1] = True
            for side in ("east", "west"):
                sx0, sy0, sx1, sy1 = cube[side]
                lo, hi = (a, b) if side == "west" else (1 - b, 1 - a)
                m[sy0:sy0 + int(0.7 * (sy1 - sy0)), sx0 + int(lo * (sx1 - sx0)):sx0 + int(hi * (sx1 - sx0)) + 1] = True
            p.recolour(m, (146, 92, 172))
    stip = _masker(p.fur.shape)
    for r in _vlakken(geo, "head", ("up", "east", "west", "south")):
        _stippen(stip, r, kit.rng(f"koorguh/stip/{r}"), _aantal(r, 520.0), 1.0, 1.6)
    p.recolour(stip, (232, 206, 240))


def _oorbinnen(p, geo, colour):
    """The inside of the ears (pink on guh.png, not fur) in this variant's own colour, keeping its shading. Returns the mask."""
    m = _masker(p.fur.shape)
    for bone in ("ear_left", "ear_right"):
        for x0, y0, x1, y1 in _vlakken(geo, bone):
            m[y0:y1, x0:x1] = True
    m &= ~p.fur & (p.a[..., 3] > 0)
    if m.any():
        gemiddeld = p.a[m][:, :3].mean(0)
        p.a[m, :3] = np.clip(np.array(colour) + (p.a[m][:, :3] - gemiddeld), 0, 255)
    return m


BASIS = ("body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right", "leg_front_left", "leg_front_right")
# the inside of the ears, per variant
OOR_BINNEN = {"korstguh": (206, 150, 60), "duinguh": (238, 178, 156), "kruimelguh": (236, 204, 150), "schimmelguh": (110, 170, 180),
              "kristalguh": (240, 194, 84), "koorguh": (226, 196, 236)}
PATROON = {"korstguh": _korstguh, "duinguh": _duinguh, "kruimelguh": _kruimelguh, "schimmelguh": _schimmelguh,
           "kristalguh": _kristalguh, "koorguh": _koorguh}


def _swatch_rects(geo, swatches_of):
    """The texture rectangles (pixels) of the swatches of these bones: {bone name prefix: [rect]}."""
    from features import guhpolder_tex as gt
    out = {}
    for name, (_parent, _pivot, swatch, _cubes) in BONES.items():
        if swatch in swatches_of.get(name.split("_")[0], ()):
            faces = gt._faces(geo, name)
            if faces:
                out.setdefault(name.split("_")[0], set()).add(faces[0]["north"])
    return out


def vacht(h):
    """Paints the fur patterns on guh_<id>.png (made by make_guh_variants.py: fur colour + swatches), makes the sleeping eyes
    again and writes the glow masks. Everything is worked out from guh.png, so running it twice changes nothing."""
    from features import guhpolder_tex as gt
    geo_pad = os.path.join(h.A, "geckolib", "models", "entity", "guh.geo.json")
    geo = json.load(open(geo_pad, encoding="utf-8"))["minecraft:geometry"][0]
    if not any(b["name"] == "korstguh_schild" for b in geo["bones"]):
        raise SystemExit("einde2_biomeguhs: run tools/make_guh_variants.py first (the biome guhs' bones are not in guh.geo.json)")
    base = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    gloed = _swatch_rects(geo, GLOEIT)
    import make_sleep_eyes
    eigen = np.zeros((base.height, base.width), bool)       # the faces of the guh's own bones
    for bone in BASIS:
        for x0, y0, x1, y1 in _vlakken(geo, bone):
            eigen[y0:y1, x0:x1] = True
    for naam, patroon in PATROON.items():
        p = gt.Painter(base, VACHT[naam])
        oren = _oorbinnen(p, geo, OOR_BINNEN[naam])
        extra = patroon(p, geo, kit.rng(f"{naam}/vacht"))
        pad = os.path.join(h.TEX, "entity", f"guh_{naam}.png")
        doel = np.asarray(Image.open(pad).convert("RGBA")).copy()
        m = (p.fur | oren) & eigen                         # (only the guh's own faces: the swatches keep their own paint)
        doel[m, :3] = p.a[m, :3].astype(np.uint8)
        h.save(Image.fromarray(doel), "entity", f"guh_{naam}.png")
        make_sleep_eyes.sleepy(f"guh_{naam}")
        if naam in GLOEIT:
            mask = np.zeros(doel.shape[:2], bool)
            for x0, y0, x1, y1 in gloed.get(naam, ()):
                mask[y0:y1, x0:x1] = True
            glow = np.zeros_like(doel)
            glow[mask] = doel[mask]
            h.save(Image.fromarray(glow), "entity", f"guh_{naam}_gloed.png")          # the mushrooms / the crystals: always
            if extra is not None:                                                     # the lamp: the veins light up too
                lamp = glow.copy()
                lamp[extra] = doel[extra]
                lamp[extra, :3] = np.clip(lamp[extra, :3].astype(np.int32) + (30, 70, 60), 0, 255).astype(np.uint8)
                h.save(Image.fromarray(lamp), "entity", f"guh_{naam}_lamp.png")


# =====================================================================================================================
# a look at them (not part of the build)
# =====================================================================================================================
def preview(h, out):
    """Renders every biome guh from three sides to <out>/<id>.png (the guh's own bones + this variant's)."""
    import wiki_renders as wr
    geo = os.path.join(h.A, "geckolib", "models", "entity", "guh.geo.json")
    alle = tuple(VACHT)
    os.makedirs(out, exist_ok=True)
    for naam in alle:
        verberg = [b for b in BONES if not b.startswith(naam)] + ["saddle"] + [f"armor_{t}{s}" for t in ("iron", "diamond", "netherite") for s in ("", "_body")]
        quads = wr.geo_quads(geo, f"guhs:entity/guh_{naam}", hide=verberg)
        beelden = [wr.render(quads, yaw=yaw, pitch=pitch, size=320, ss=1) for yaw, pitch in ((35, -22), (150, -28))]
        vel = Image.new("RGBA", (320 * len(beelden), 320), (58, 44, 70, 255))
        for i, b in enumerate(beelden):
            vel.alpha_composite(b, (320 * i, 0))
        vel.save(os.path.join(out, f"{naam}.png"))
