"""
1.10 "Het Vadsrijk", zone 6 "Het Zijdeland", slice U-Z6RAID (U_BRIEF.md): the raid "De Sterrendovende Toren".
Java: feature/vadsrijk/zone6/raid. The inside of the tower (blocks): tools/vadsrijk/zone6/raid/binnentoren.py.

Everything generated, on the zone-1/2 helpers (vadsrijk_wezens.Model / schilder, vadsrijk_z2_wezens Mika painter), kit v2 when
available:
  * the bosses (geckolib/.../entity/vr_z6raid_baas_*): zandwachter (rose-sand golem with an hourglass in his chest), spiegelvizier
    (Meester Glans, a Mika illusionist in a turban with a mirror staff; the same model for his images and clones), the three
    Gedoofde Sterrenbeelden (ster_knuffel, ster_bultguh, ster_pluis: star knots + dotted star lines, grey / shining), ringeling
    (Hertogin Ringeling, a Mika duchess with ten toe rings and floating rings); + T-VFX's generic vfx_* moves merged in;
  * the mechanic objects (vr_z6raid_*): zandpilaar, draaispiegel, sterrenbaken (+ lit), sterrenscherf;
  * textures in textures/vadsrijk/z6raid/ (+ _blij, + _glowmask for the AutoGlowing layer);
  * items: the Sterrenzijde set (4), Ringbreker, Komeetboog, Sterrenstaf, Ringelings Teenring, Ringreplica, Sterrennacht-halster;
    the worn look (equipment vr_z6raid_sterrenzijde, tinted from the vanilla gold layer);
  * the wings "Sterrenring" for t-kleding's Kledingkast (3 posed item models + texture, via vadsrijk_t_kleding's helpers);
  * all texts (Dutch; en_us real English where cheap).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_z6raid.py
"""
import contextlib
import json
import math
import os
import sys
import zipfile
from io import BytesIO

import numpy as np
from PIL import Image, ImageDraw

try:
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
    from features import vadsrijk_z3_wezens as z3
    from features.vadsrijk_economie import Doek, zwaard, boog, staf
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
    from features import vadsrijk_z3_wezens as z3
    from features.vadsrijk_economie import Doek, zwaard, boog, staf

Model, kf, anim, anim_bestand = vw.Model, vw.kf, vw.anim, vw.anim_bestand
samen, adem, een = z2.samen, z2.adem, z2.een
SUB = "vadsrijk/z6raid"


def _v2():
    return vw.stijl(2) if hasattr(vw, "stijl") else contextlib.nullcontext()


def draai(bone, a, t=0.8, as_="z"):
    i = {"x": 0, "y": 1, "z": 2}[as_]
    v = lambda s: tuple(s if j == i else 0 for j in range(3))
    return {bone: {"rotation": kf([(0, v(a)), (t / 2, v(-a)), (t, v(a))])}}


def bob(bone, t=2.0, h=0.8):
    return {bone: {"position": kf([(0, (0, 0, 0)), (t / 2, (0, h, 0)), (t, (0, 0, 0))])}}


def rond(bone, t=2.0, as_="y"):
    i = {"x": 0, "y": 1, "z": 2}[as_]
    return {bone: {"rotation": kf([(0, (0, 0, 0)), (t, tuple(360 if j == i else 0 for j in range(3)))])}}


def puls(bone, t=1.0, f=1.15):
    return {bone: {"scale": kf([(0, (1, 1, 1)), (t / 2, (f, f, f)), (t, (1, 1, 1))])}}


# =====================================================================================================================
# the bosses
# =====================================================================================================================
def m_zandwachter():
    m = Model("z6raid_zandwachter", px=2, breedte=256, bounds=(4, 5, 2))
    m.bone("root")
    for kant, sx in (("l", -1), ("r", 1)):
        b = "been_" + kant
        m.bone(b, "root", (sx * 8, 20, 0))
        m.cube(b, "zand", (sx * 8 - 5, 4, -5), (10, 16, 10))
        m.cube(b, "steen", (sx * 8 - 6, 0, -7), (12, 5, 13))
    m.bone("body", "root", (0, 22, 0))
    m.cube("body", "zand", (-16, 20, -10), (32, 26, 20))
    m.cube("body", "steen", (-17, 22, -11), (34, 3, 22))
    m.cube("body", "steen", (-17, 40, -11), (34, 3, 22))
    m.cube("body", "steen", (-12, 44, -8), (24, 6, 16))
    m.bone("rug", "body", (0, 32, 10))
    m.cube("rug", "rugplaat", (-11, 25, 9), (22, 16, 3))
    m.cube("rug", "goud", (-2, 28, 11.5), (4, 10, 1))
    m.bone("zandloper", "body", (0, 33, -10))
    m.cube("zandloper", "goud", (-6, 24, -12.5), (12, 2, 4))
    m.cube("zandloper", "goud", (-6, 40, -12.5), (12, 2, 4))
    m.cube("zandloper", "glas", (-4.5, 33, -12), (9, 7, 3))
    m.cube("zandloper", "glas", (-4.5, 26, -12), (9, 7, 3))
    m.cube("zandloper", "zandkorrel", (-1, 30, -12.6), (2, 6, 1))
    for sx in (-1, 1):
        m.cube("zandloper", "goud", (sx * 5.5 - 0.75, 26, -12.4), (1.5, 14, 1.5))
    m.bone("head", "body", (0, 50, 0))
    m.cube("head", "zand", (-8, 50, -9), (16, 13, 15), gezicht={"mood": "boos", "ey": 0.45, "schaal": 1.1})
    m.cube("head", "steen", (-9, 61, -10), (18, 3, 17))
    for sx in (-1, 1):
        m.cube("head", "zand", (sx * 6 - 2, 63, -3), (4, 4, 3))
        m.cube("head", "goud", (sx * 6 - 1, 64, -3.4), (2, 2, 0.5))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "arm_" + kant
        m.bone(b, "body", (sx * 17, 43, 0))
        x0 = -27 if sx < 0 else 17
        m.cube(b, "steen", (x0 - 1, 38, -7), (12, 8, 14))
        m.cube(b, "zand", (x0, 20, -5), (10, 18, 10))
        m.cube(b, "steen", (x0 - 1, 10, -6), (12, 11, 12))
        m.cube(b, "goud", (x0 - 1.2, 21, -6.2), (12.4, 2, 12.4))
    a = {
        "idle": anim(3.0, samen(adem("body", 3.0, 1.015), draai("arm_l", 4, 3.0, "z"), draai("arm_r", -4, 3.0, "z"), draai("head", 5, 3.0, "y"))),
        "walk": anim(1.6, samen(draai("been_l", 22, 1.6, "x"), draai("been_r", -22, 1.6, "x"), draai("arm_l", -16, 1.6, "x"), draai("arm_r", 16, 1.6, "x"),
                                bob("root", 0.8, 1.0))),
        "slag": een(1.0, {"arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-160, 0, 0)), (0.6, (10, 0, 0)), (1.0, (0, 0, 0))])},
                          "arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-160, 0, 0)), (0.6, (10, 0, 0)), (1.0, (0, 0, 0))])},
                          "body": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-10, 0, 0)), (0.6, (18, 0, 0)), (1.0, (0, 0, 0))])}}),
        "golf": een(1.2, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-60, 0, 80)), (0.8, (-60, 0, -40)), (1.2, (0, 0, 0))])},
                          "body": {"rotation": kf([(0, (0, 0, 0)), (0.5, (0, 30, 0)), (0.8, (0, -30, 0)), (1.2, (0, 0, 0))])}}),
        "draai": een(1.5, {"zandloper": {"rotation": kf([(0, (0, 0, 0)), (1.5, (0, 0, 180))])}}),
        "storm": een(2.0, {"arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.5, (0, 0, -90)), (1.5, (0, 0, -90)), (2.0, (0, 0, 0))])},
                           "arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.5, (0, 0, 90)), (1.5, (0, 0, 90)), (2.0, (0, 0, 0))])},
                           "body": {"rotation": kf([(0, (0, 0, 0)), (2.0, (0, 360, 0))])}}),
    }
    pal = {"zand": ((236, 168, 150), "plekjes", (220, 146, 132)), "steen": ((200, 120, 112), "strepen", (176, 100, 96)),
           "goud": ((248, 206, 96), "glans", None), "glas": ((236, 240, 255), "glans", None), "zandkorrel": ((255, 214, 150), None, None),
           "rugplaat": ((170, 96, 110), "facet", (140, 80, 96))}
    glow = {"glas": (150, 120, 70), "zandkorrel": (255, 200, 110)}
    return m, a, pal, glow


def mika_lijf(naam, breedte=128):
    """A Mika body without the mining gear (the zone-2 Mika: pink fur, round ears, a thin tail), for the raid's two Mika's."""
    m = Model(naam, px=4, breedte=breedte, bounds=(3, 3.5, 1.5))
    m.bone("root")
    for kant, sx in (("l", -1), ("r", 1)):
        b = "been_" + kant
        m.bone(b, "root", (sx * 2, 9, 0))
        m.cube(b, "broek", (sx * 2 - 1.5, 2, -1.5), (3, 7, 3))
        m.cube(b, "voet", (sx * 2 - 1.8, 0, -2.6), (3.6, 2.2, 4.2))
    m.bone("body", "root", (0, 9, 0))
    m.cube("body", "vacht", (-3.5, 9, -2.2), (7, 9, 4.4))
    m.bone("staart", "body", (0, 10, 2.2), (-40, 0, 0))
    m.cube("staart", "vacht", (-0.6, 9.5, 2.2), (1.2, 1.2, 9))
    m.bone("head", "body", (0, 18, 0))
    m.cube("head", "vacht", (-4.5, 18, -4.5), (9, 8, 8), gezicht={"mood": "mika", "ey": 0.5})
    m.cube("head", "snuit", (-2, 18.5, -5.3), (4, 2.5, 1))
    for sx in (-1, 1):
        m.cube("head", "vacht", (sx * 3.3 - 1.7, 25, -1.5), (3.4, 3.4, 1.2))
        m.cube("head", "oor", (sx * 3.3 - 1.1, 25.5, -1.7), (2.2, 2.2, 0.4))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "arm_" + kant
        m.bone(b, "body", (sx * 4.5, 17, 0))
        ox = -6 if sx < 0 else 3.5
        m.cube(b, "mouw", (ox, 9.5, -1.2), (2.5, 8, 2.5))
        m.cube(b, "poot", (ox, 8.5, -1.3), (2.5, 1.5, 2.6))
    return m


def mika_anims():
    lk = lambda a, t=0.6: {"rotation": kf([(0, (a, 0, 0)), (t / 2, (-a, 0, 0)), (t, (a, 0, 0))])}
    return {
        "idle": anim(2.0, samen(adem("body", 2.0, 1.02), {"staart": {"rotation": kf([(0, (-40, -15, 0)), (1, (-40, 15, 0)), (2, (-40, -15, 0))])}})),
        "walk": anim(0.6, {"been_l": lk(30), "been_r": lk(-30), "arm_l": lk(-25), "arm_r": lk(25),
                           "staart": {"rotation": kf([(0, (-30, -20, 0)), (0.3, (-30, 20, 0)), (0.6, (-30, -20, 0))])}}),
    }


def m_spiegelvizier():
    m = mika_lijf("z6raid_spiegelvizier")
    m.cube("body", "gewaad", (-4, 6, -2.7), (8, 12, 5.4))
    m.cube("body", "sjerp", (-4.2, 12, -2.9), (8.4, 1.6, 5.8))
    m.cube("body", "goud", (-0.8, 12.1, -3.1), (1.6, 1.4, 0.4))
    m.cube("head", "tulband", (-5, 25, -5), (10, 3.5, 9))
    m.cube("head", "tulband", (-4, 28.5, -4), (8, 2.5, 7))
    m.cube("head", "juweel", (-1, 26, -5.6), (2, 2, 0.8))
    m.cube("head", "veer", (-0.5, 28, -5), (1, 5, 1))
    m.cube("head", "snor", (-3, 18.2, -5.6), (6, 1, 0.6))
    m.bone("wapen", "arm_r", (4.7, 9.5, 0))
    m.cube("wapen", "steel", (4.2, 2, -0.6), (1.2, 18, 1.2))
    m.cube("wapen", "goud", (3.2, 19.5, -1.6), (3.2, 1.2, 3.2))
    m.cube("wapen", "spiegel", (2.3, 20.5, -0.4), (5, 5, 0.8))
    a = mika_anims()
    a["poets"] = een(1.2, {"arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-80, 30, 0)), (0.4, (-80, -10, 0)), (0.6, (-80, 30, 0)), (0.8, (-80, -10, 0)),
                                                       (1.2, (0, 0, 0))])}})
    a["straal"] = een(0.8, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-90, 0, 0)), (0.6, (-90, 0, 0)), (0.8, (0, 0, 0))])}})
    a["wissel"] = een(1.0, {"root": {"rotation": kf([(0, (0, 0, 0)), (1.0, (0, 720, 0))])}})
    a["verstop"] = een(1.0, {"root": {"rotation": kf([(0, (0, 0, 0)), (0.5, (0, 360, 0)), (1.0, (0, 360, 0))]),
                                      "scale": kf([(0, (1, 1, 1)), (0.4, (0.6, 1.3, 0.6)), (1.0, (1, 1, 1))])}})
    pal = {"vacht": ((186, 134, 160), "plekjes", (170, 118, 146)), "snuit": ((226, 168, 184), None, None), "oor": ((236, 146, 166), None, None),
           "poot": ((236, 166, 182), None, None), "voet": ((90, 60, 110), None, None), "broek": ((110, 80, 150), None, None),
           "mouw": ((120, 170, 220), "strepen", (220, 236, 255)), "gewaad": ((96, 150, 214), "strepen", (232, 240, 255)),
           "sjerp": ((230, 190, 80), None, None), "goud": ((250, 210, 90), "glans", None), "tulband": ((240, 244, 252), "strepen", (180, 210, 240)),
           "juweel": ((120, 230, 255), "glans", None), "veer": ((255, 255, 255), None, None), "snor": ((70, 44, 60), None, None),
           "steel": ((210, 210, 226), None, None), "spiegel": ((226, 240, 255), "glans", None)}
    glow = {"spiegel": (140, 170, 200), "juweel": (60, 160, 200)}
    return m, a, pal, glow


def m_ringeling():
    m = mika_lijf("z6raid_ringeling")
    m.cube("body", "jurk", (-4.2, 9, -2.6), (8.4, 9, 5.2))
    m.bone("rok", "body", (0, 9, 0))
    m.cube("rok", "jurk", (-6, 2.5, -4.5), (12, 7, 9))
    m.cube("rok", "zoom", (-6.4, 2, -4.9), (12.8, 1.2, 9.8))
    m.cube("body", "goud", (-4.4, 16.4, -2.8), (8.8, 1.2, 5.6))
    m.cube("body", "parels", (-2.5, 15, -2.9), (5, 1, 0.5))
    m.cube("head", "kroon", (-3.5, 26, -3.5), (7, 2, 6))
    for i, x in enumerate((-3, -1, 1, 3)):
        m.cube("head", "kroon", (x - 0.5, 28, -3.5), (1, 1.6 + (i % 2), 1))
    m.cube("head", "juweel", (-0.7, 28.2, -3.9), (1.4, 1.4, 0.6))
    for sx in (-1, 1):
        m.cube("head", "goud", (sx * 4.6 - 0.4, 19, -0.5), (0.8, 2, 0.8))
    # ten toe rings (5 per foot, on the front of each foot)
    for kant, sx in (("l", -1), ("r", 1)):
        for i in range(5):
            m.cube("been_" + kant, "ring", (sx * 2 - 1.9 + i * 0.85, 0.2, -2.9), (0.6, 0.9, 0.6))
    # her own rings floating round her
    m.bone("ringen", "root", (0, 16, 0))
    for i in range(4):
        hoek = i * math.pi / 2
        x, z = math.cos(hoek) * 9, math.sin(hoek) * 9
        m.cube("ringen", "ring", (x - 1.5, 15 + (i % 2) * 3, z - 0.3), (3, 3, 0.6))
    a = mika_anims()
    a["idle"] = anim(2.0, samen(adem("body", 2.0, 1.02), rond("ringen", 4.0), {"staart": {"rotation": kf([(0, (-40, -15, 0)), (1, (-40, 15, 0)),
                                                                                                         (2, (-40, -15, 0))])}}))
    a["walk"] = anim(0.6, samen(a["walk"]["bones"], rond("ringen", 2.0)))
    a["schop"] = een(0.7, {"been_r": {"rotation": kf([(0, (0, 0, 0)), (0.25, (40, 0, 0)), (0.4, (-80, 0, 0)), (0.7, (0, 0, 0))])}})
    a["trap"] = een(0.8, {"been_l": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-50, 0, 0)), (0.45, (5, 0, 0)), (0.8, (0, 0, 0))])},
                          "root": {"position": kf([(0, (0, 0, 0)), (0.3, (0, 2, 0)), (0.45, (0, 0, 0))])}})
    a["zweef"] = anim(2.0, samen({"arm_l": {"rotation": kf([(0, (0, 0, -70))])}, "arm_r": {"rotation": kf([(0, (0, 0, 70))])}}, rond("ringen", 1.0),
                                 bob("root", 2.0, 1.5)))
    a["val"] = een(1.0, {"root": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-30, 0, 20)), (1.0, (0, 0, 0))])}})
    a["huil"] = een(2.0, {"arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-120, 30, 0)), (1.7, (-120, 30, 0)), (2.0, (0, 0, 0))])},
                          "arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-120, -30, 0)), (1.7, (-120, -30, 0)), (2.0, (0, 0, 0))])}})
    a["blij"] = anim(1.0, samen(bob("root", 0.5, 1.5), draai("arm_l", 30, 0.5, "z"), draai("arm_r", -30, 0.5, "z")))
    pal = {"vacht": ((200, 140, 168), "plekjes", (184, 124, 152)), "snuit": ((236, 176, 192), None, None), "oor": ((246, 150, 176), None, None),
           "poot": ((246, 176, 192), None, None), "voet": ((246, 176, 192), None, None), "broek": ((150, 70, 130), None, None),
           "mouw": ((170, 60, 150), "glans", None), "jurk": ((176, 64, 156), "strepen", (220, 120, 200)), "zoom": ((250, 210, 90), None, None),
           "goud": ((250, 210, 90), "glans", None), "parels": ((250, 246, 236), None, None), "kroon": ((250, 214, 90), "glans", None),
           "juweel": ((190, 90, 255), "glans", None), "ring": ((255, 216, 80), "glans", None)}
    glow = {"ring": (255, 200, 90), "juweel": (150, 70, 220)}
    return m, a, pal, glow


# ---- the constellations: star knots + dotted lines in the front plane (no rotated cubes) ---------------------------------
def sterrenbeeld(naam, knopen, lijnen, kop, bounds, extra=None):
    """knopen: {name: (x, y, size)}; lijnen: [(a, b)]; kop: (x, y, w, h) the head knot with a face. Bones per part (key of
    knopen -> its bone via extra['bones'])."""
    m = Model(naam, px=4, breedte=128, bounds=bounds)
    m.bone("root")
    m.bone("body", "root", (0, 0, 0))
    bones = (extra or {}).get("bones", {})
    gemaakt = {"root", "body"}
    for b, (ouder, piv) in bones.items():
        m.bone(b, ouder, piv)
        gemaakt.add(b)
    hx, hy, hw, hh = kop
    m.bone("head", "body", (hx, hy, 0))
    m.cube("head", "kop", (hx - hw / 2, hy - hh / 2, -2), (hw, hh, 4), gezicht={"mood": "blij", "ey": 0.45}, rond=0)
    for k, (x, y, s) in knopen.items():
        b = bones_van(k, bones)
        m.cube(b, "ster", (x - s / 2, y - s / 2, -s / 2), (s, s, s), rond=0)
    for a_, b_ in lijnen:
        ax, ay, _ = knopen[a_] if a_ in knopen else (hx, hy, 0)
        bx, by, _ = knopen[b_] if b_ in knopen else (hx, hy, 0)
        bone = bones_van(a_, bones)
        L = math.hypot(bx - ax, by - ay)
        n = max(1, int(L / 2.6))
        for i in range(1, n):
            t = i / n
            x, y = ax + (bx - ax) * t, ay + (by - ay) * t
            m.cube(bone, "lijn", (x - 0.6, y - 0.6, -0.6), (1.2, 1.2, 1.2), rond=0)
    return m


def bones_van(knoop, bones):
    for b in bones:
        if knoop.startswith(b):
            return b
    return "body"


STER_PAL = {"ster": ((255, 236, 170), "glans", None), "lijn": ((214, 226, 255), None, None), "kop": ((255, 240, 200), "glans", None)}
STER_GLOW = {"ster": (255, 220, 140), "lijn": (150, 170, 230), "kop": (200, 180, 120)}


def m_ster_knuffel():
    kn = {"body_borst": (0, 30, 5), "body_buik": (0, 18, 6), "body_heup_l": (-7, 12, 4), "body_heup_r": (7, 12, 4), "body_schouder_l": (-10, 33, 4),
          "body_schouder_r": (10, 33, 4), "arm_l_poot": (-16, 20, 5), "arm_r_poot": (16, 20, 5), "been_l_voet": (-8, 2, 5), "been_r_voet": (8, 2, 5),
          "head_oor_l": (-6, 47, 4), "head_oor_r": (6, 47, 4)}
    li = [("body_borst", "body_buik"), ("body_buik", "body_heup_l"), ("body_buik", "body_heup_r"), ("body_borst", "body_schouder_l"),
          ("body_borst", "body_schouder_r"), ("arm_l_poot", "body_schouder_l"), ("arm_r_poot", "body_schouder_r"), ("been_l_voet", "body_heup_l"),
          ("been_r_voet", "body_heup_r"), ("head_oor_l", "kop"), ("head_oor_r", "kop"), ("body_borst", "kop"), ("body_schouder_l", "body_heup_l"),
          ("body_schouder_r", "body_heup_r")]
    bones = {"arm_l": ("body", (-10, 33, 0)), "arm_r": ("body", (10, 33, 0)), "been_l": ("body", (-7, 12, 0)), "been_r": ("body", (7, 12, 0))}
    m = sterrenbeeld("z6raid_ster_knuffel", kn, li, (0, 41, 10, 9), (3, 3.5, 1), {"bones": bones})
    a = {"idle": anim(3.0, samen(adem("body", 3.0, 1.03), draai("arm_l", 8, 3.0, "z"), draai("arm_r", -8, 3.0, "z"))),
         "walk": anim(1.2, samen(draai("been_l", 20, 1.2, "x"), draai("been_r", -20, 1.2, "x"), draai("arm_l", -15, 1.2, "x"), draai("arm_r", 15, 1.2, "x"))),
         "klem": een(1.0, {"arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.4, (0, 0, -60)), (0.6, (0, 0, 30)), (1.0, (0, 0, 0))])},
                           "arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.4, (0, 0, 60)), (0.6, (0, 0, -30)), (1.0, (0, 0, 0))])}}),
         "brul": een(1.5, samen(puls("head", 1.5, 1.3), {"body": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-15, 0, 0)), (1.5, (0, 0, 0))])}})),
         "storm": een(0.5, puls("body", 0.5)), "lied": een(0.5, puls("body", 0.5)),
         "uit": anim(2.0, samen(puls("body", 2.0, 1.08), rond("root", 6.0)))}
    return m, a


def m_ster_bultguh():
    kn = {"body_bult1": (-5, 26, 5), "body_bult2": (7, 26, 5), "body_rug": (1, 20, 4), "body_staart": (14, 18, 3), "nek_1": (-12, 22, 4),
          "been_l_voor": (-8, 2, 4), "been_r_voor": (-4, 2, 4), "been_l_achter": (6, 2, 4), "been_r_achter": (10, 2, 4), "body_buik": (1, 13, 4)}
    li = [("body_bult1", "body_rug"), ("body_bult2", "body_rug"), ("body_bult2", "body_staart"), ("body_rug", "body_buik"), ("nek_1", "body_bult1"),
          ("nek_1", "kop"), ("been_l_voor", "body_buik"), ("been_r_voor", "body_buik"), ("been_l_achter", "body_buik"), ("been_r_achter", "body_buik")]
    bones = {"nek": ("body", (-10, 22, 0)), "been_l": ("body", (-6, 12, 0)), "been_r": ("body", (8, 12, 0))}
    m = sterrenbeeld("z6raid_ster_bultguh", kn, li, (-17, 30, 8, 7), (3, 2.5, 1), {"bones": bones})
    a = {"idle": anim(2.0, samen(bob("root", 2.0, 2.0), draai("nek", 6, 2.0, "z"))),
         "walk": anim(1.0, samen(bob("root", 1.0, 2.0), draai("been_l", 25, 1.0, "z"), draai("been_r", -25, 1.0, "z"))),
         "storm": een(0.8, {"body": {"rotation": kf([(0, (0, 0, 0)), (0.3, (0, 0, 20)), (0.8, (0, 0, 0))])}}),
         "klem": een(0.5, puls("body", 0.5)), "brul": een(0.5, puls("body", 0.5)), "lied": een(0.5, puls("body", 0.5)),
         "uit": anim(2.0, samen(puls("body", 2.0, 1.08), bob("root", 2.0, 1.0)))}
    return m, a


def m_ster_pluis():
    kn = {}
    for i in range(8):
        h = i * math.pi / 4
        kn[f"body_p{i}"] = (math.cos(h) * 7, 9 + math.sin(h) * 7, 2.6)
    li = [(f"body_p{i}", f"body_p{(i + 1) % 8}") for i in range(8)]
    m = sterrenbeeld("z6raid_ster_pluis", kn, li, (0, 9, 7, 6), (1.5, 1.5, 0.6))
    a = {"idle": anim(2.0, samen(bob("root", 2.0, 1.5), rond("body", 8.0, "z"))),
         "walk": anim(0.6, bob("root", 0.6, 2.0)),
         "lied": een(3.0, samen(puls("body", 0.6, 1.2), bob("root", 3.0, 3.0))),
         "klem": een(0.5, puls("body", 0.5)), "brul": een(0.5, puls("body", 0.5)), "storm": een(0.5, puls("body", 0.5)),
         "uit": anim(2.0, samen(puls("body", 2.0, 1.1), rond("body", 3.0, "z"))),
         "hop": anim(0.6, bob("root", 0.6, 4.0)), "sprong": anim(1.0, rond("body", 1.0, "z"))}
    return m, a


# ---- the objects ------------------------------------------------------------------------------------------------------------
def m_zandpilaar():
    m = Model("z6raid_zandpilaar", px=4, breedte=96, bounds=(2, 4, 0.5))
    m.bone("root")
    m.cube("root", "steen", (-9, 0, -9), (18, 4, 18))
    m.bone("zuil", "root", (0, 4, 0))
    for i in range(4):
        w = 13 - i * 1.5
        m.cube("zuil", "zand" if i % 2 == 0 else "steen", (-w / 2, 4 + i * 11, -w / 2), (w, 11, w))
    m.cube("zuil", "goud", (-6, 48, -6), (12, 3, 12))
    a = {"idle": anim(2.0, {"zuil": {"scale": kf([(0, (1, 1, 1)), (1.0, (1.02, 0.99, 1.02)), (2.0, (1, 1, 1))])}})}
    pal = {"zand": ((236, 168, 150), "plekjes", (220, 146, 132)), "steen": ((200, 120, 112), "strepen", (176, 100, 96)), "goud": ((248, 206, 96), "glans", None)}
    return m, a, pal, {}


def m_draaispiegel():
    m = Model("z6raid_draaispiegel", px=4, breedte=96, bounds=(2, 3, 0.5))
    m.bone("root")
    m.cube("root", "voet", (-6, 0, -6), (12, 3, 12))
    m.cube("root", "goud", (-1.5, 3, -1.5), (3, 14, 3))
    m.bone("spiegel", "root", (0, 24, 0))
    m.cube("spiegel", "goud", (-8, 16, -1.5), (16, 16, 3))
    m.cube("spiegel", "spiegel", (-7, 17, -2), (14, 14, 1))
    m.cube("spiegel", "goud", (-1, 32, -1), (2, 3, 2))
    a = {"idle": anim(3.0, {"spiegel": {"rotation": kf([(0, (0, -4, 0)), (1.5, (0, 4, 0)), (3.0, (0, -4, 0))])}}),
         "open": een(0.5, puls("spiegel", 0.5, 1.1)), "klop": een(0.5, puls("spiegel", 0.5, 1.1)), "schiet": een(0.5, puls("spiegel", 0.5, 1.1)),
         "pomp": een(0.5, puls("spiegel", 0.5, 1.1)), "grijp": een(0.5, puls("spiegel", 0.5, 1.1))}
    pal = {"voet": ((150, 150, 176), None, None), "goud": ((248, 206, 96), "glans", None), "spiegel": ((226, 240, 255), "glans", None)}
    return m, a, pal, {"spiegel": (150, 180, 210)}


def m_sterrenbaken():
    m = Model("z6raid_sterrenbaken", px=4, breedte=96, bounds=(2, 3, 0.5))
    m.bone("root")
    m.cube("root", "steen", (-7, 0, -7), (14, 3, 14))
    m.cube("root", "steen", (-4, 3, -4), (8, 14, 8))
    m.cube("root", "goud", (-6, 17, -6), (12, 2, 12))
    m.bone("ster", "root", (0, 24, 0))
    m.cube("ster", "ster", (-3, 20, -3), (6, 6, 6), rond=0)
    m.cube("ster", "ster", (-1, 18.5, -1), (2, 9, 2), rond=0)
    m.cube("ster", "ster", (-4.5, 22, -1), (9, 2, 2), rond=0)
    a = {"idle": anim(3.0, samen(rond("ster", 3.0), bob("ster", 3.0, 1.0))),
         "open": een(0.5, puls("ster", 0.5)), "klop": een(0.5, puls("ster", 0.5)), "schiet": een(0.5, puls("ster", 0.5)),
         "pomp": een(0.5, puls("ster", 0.5)), "grijp": een(0.5, puls("ster", 0.5))}
    pal = {"steen": ((120, 120, 136), "strepen", (100, 100, 116)), "goud": ((248, 206, 96), "glans", None), "ster": ((150, 150, 170), None, None)}
    pal_aan = dict(pal, ster=((255, 236, 160), "glans", None))
    return m, a, pal, pal_aan


def m_sterrenscherf():
    m = Model("z6raid_sterrenscherf", px=4, breedte=64, bounds=(1, 1, 0.5))
    m.bone("root")
    m.bone("scherf", "root", (0, 7, 0))
    m.cube("scherf", "ster", (-2, 4, -2), (4, 6, 4), rot=(0, 45, 0), pivot=(0, 7, 0), rond=0)
    m.cube("scherf", "ster", (-1, 2.5, -1), (2, 9, 2), rot=(0, 45, 0), pivot=(0, 7, 0), rond=0)
    a = {"idle": anim(2.0, samen(rond("scherf", 2.0), bob("scherf", 2.0, 1.5))),
         "open": een(0.5, puls("scherf", 0.5)), "klop": een(0.5, puls("scherf", 0.5)), "schiet": een(0.5, puls("scherf", 0.5)),
         "pomp": een(0.5, puls("scherf", 0.5)), "grijp": een(0.5, puls("scherf", 0.5))}
    pal = {"ster": ((255, 240, 180), "glans", None)}
    return m, a, pal, {"ster": (255, 220, 140)}


# =====================================================================================================================
# texts
# =====================================================================================================================
BAZEN = {
    "zandwachter": ("De Zandwachter", "The Sand Warden", "Een golem van rozenzand met een zandloper in zijn borst"),
    "spiegelvizier": ("De Spiegelvizier", "The Mirror Vizier", "Meester Glans, spiegelpoetser en illusionist"),
    "sterrenbeelden": ("De Gedoofde Sterrenbeelden", "The Snuffed Constellations", "Drie sterrenbeelden, één venster"),
    "ringeling": ("Hertogin Ringeling", "Duchess Ringeling", "de Ringvinger, met tien ringen om haar tenen"),
}
ENTITEITEN = {
    "vr_z6raid_baas_zandwachter": ("De Zandwachter", "The Sand Warden"),
    "vr_z6raid_baas_spiegelvizier": ("De Spiegelvizier", "The Mirror Vizier"),
    "vr_z6raid_baas_ster_knuffel": ("De Grote Knuffel", "The Great Cuddle"),
    "vr_z6raid_baas_ster_bultguh": ("De Vliegende Bultguh", "The Flying Humpguh"),
    "vr_z6raid_baas_ster_pluis": ("De Kleine Pluis", "The Little Fluff"),
    "vr_z6raid_baas_ringeling": ("Hertogin Ringeling", "Duchess Ringeling"),
    "vr_z6raid_zandpilaar": ("Zandpilaar", "Sand pillar"),
    "vr_z6raid_draaispiegel": ("Draaispiegel (rechtsklik: draai)", "Turning mirror (right-click: turn)"),
    "vr_z6raid_sterrenbaken": ("Sterrenbaken", "Star beacon"),
    "vr_z6raid_sterrenscherf": ("Sterrenscherf (rechtsklik: oppakken)", "Star shard (right-click: pick up)"),
}
TEKSTEN = {
    "vadsrijk.z6raid.titel": "De Sterrendovende Toren",
    "vadsrijk.z6raid.start.onder": "%s spelers. Vier verdiepingen, vier bazen. Zet 'm op!",
    "vadsrijk.z6raid.start.alleen": "De toren laat je niet alleen naar binnen: neem minstens één vriendje mee (2-10 spelers).",
    "vadsrijk.z6raid.start.tel": "Blijf staan... %s spelers op de startcirkel.",
    "vadsrijk.z6raid.rij.erin": "De toren is bezet. Jullie staan in de rij: plek %s.",
    "vadsrijk.z6raid.rij.beurt": "Jullie zijn aan de beurt! Kom binnen 10 tellen naar De Wachtzaal.",
    "vadsrijk.z6raid.wachtzaal.welkom": "De Wachtzaal van De Sterrendovende Toren. Ga met 2-10 spelers op de gouden startcirkel staan. De uitgang is de paarse boog in het westen.",
    "vadsrijk.z6raid.wachtzaal.vrij": "De toren is vrij: ga met 2-10 spelers op de gouden startcirkel staan.",
    "vadsrijk.z6raid.wachtzaal.plek": "Jullie staan in de rij: plek %s (de groep binnen is op verdieping %s).",
    "vadsrijk.z6raid.wachtzaal.bezet": "Bezet: een groep van %s is op verdieping %s (%s groep(en) in de rij). Op de startcirkel kom je in de rij.",
    "vadsrijk.z6raid.verdieping.op": "Het sterrenvlies smelt!",
    "vadsrijk.z6raid.verdieping.2": "Op naar verdieping 2: De Spiegelvizier",
    "vadsrijk.z6raid.verdieping.3": "Op naar verdieping 3: De Gedoofde Sterrenbeelden",
    "vadsrijk.z6raid.verdieping.4": "Op naar het dak: Hertogin Ringeling",
    "vadsrijk.z6raid.wipe": "Wipe!",
    "vadsrijk.z6raid.wipe.onder": "De toren zet de verdieping terug. Wipes: %s. Adem in, en nog een keer!",
    "vadsrijk.z6raid.vrij": "De toren is weer vrij.",
    "vadsrijk.z6raid.klaar.chat": "De Sterrendovende Toren is uit! Tijd %s, wipes %s. Je kist staat op het dak; de lichtzuil in het midden brengt je naar buiten.",
    "vadsrijk.z6raid.enrage": "De toren raakt zijn geduld kwijt!",
    "vadsrijk.z6raid.onderbroken": "Onderbroken!",
    "vadsrijk.z6raid.spiegelkloon": "Spiegelkloon",
    # hud
    "vadsrijk.z6raid.hud.verdieping": "%s: %s",
    "vadsrijk.z6raid.hud.klaar": "De toren is uit!",
    "vadsrijk.z6raid.hud.klok": "Gevecht %s",
    "vadsrijk.z6raid.hud.rust": "rust",
    "vadsrijk.z6raid.hud.wipes": "Wipes %s  Tijd %s  (%s sp.)",
    "vadsrijk.z6raid.hud.weg": "weg",
    "vadsrijk.z6raid.hud.flauw": "flauw",
    "vadsrijk.z6raid.hud.buiten": "buiten",
    # zandwachter
    "vadsrijk.z6raid.zandwachter.zandloper": "De zandloper loopt leeg! Sta TEGELIJK op beide roze platen (%s per plaat).",
    "vadsrijk.z6raid.zandwachter.omgedraaid": "Omgedraaid! Zijn rug is kwetsbaar",
    "vadsrijk.z6raid.zandwachter.begraven": "Begraven onder het zand!",
    "vadsrijk.z6raid.zandwachter.pilaren": "%s zandpilaren rijzen op: sla ze kapot binnen 20 tellen!",
    "vadsrijk.z6raid.zandwachter.pilaren_weg": "Alle pilaren zijn weg!",
    "vadsrijk.z6raid.zandwachter.muur": "Een zandmuur! Hij is even taai",
    "vadsrijk.z6raid.zandwachter.huid": "Zijn zandhuid sluit zich weer.",
    "vadsrijk.z6raid.zandwachter.vuist": "Zandvuist op %s: ga er SAMEN in staan!",
    "vadsrijk.z6raid.zandwachter.spetters": "Zandspetters: ga uit elkaar!",
    "vadsrijk.z6raid.zandwachter.fase2": "Het zand raakt op: meer pilaren!",
    "vadsrijk.z6raid.zandwachter.fase3": "De laatste korrels!",
    # spiegelvizier
    "vadsrijk.z6raid.spiegelvizier.verstop": "Waar is de echte?",
    "vadsrijk.z6raid.spiegelvizier.immuun": "Dat is maar een spiegelbeeld... of hij verstopt zich nog.",
    "vadsrijk.z6raid.spiegelvizier.nep": "Nep! Er stapt een spiegelkloon uit.",
    "vadsrijk.z6raid.spiegelvizier.draai": "De spiegel wijst nu naar beeld %s van %s.",
    "vadsrijk.z6raid.spiegelvizier.gevonden": "Gevonden!",
    "vadsrijk.z6raid.spiegelvizier.fout": "Fout beeld! Een spiegelkloon stapt eruit.",
    "vadsrijk.z6raid.spiegelvizier.flits": "Spiegelflits!",
    "vadsrijk.z6raid.spiegelvizier.gewisseld": "Wie is wie? Je bent gewisseld met %s!",
    "vadsrijk.z6raid.spiegelvizier.breekt": "De spiegelzaal breekt!",
    "vadsrijk.z6raid.spiegelvizier.zeg.verstop": "Raad maar welke ik ben. Ik poets mezelf zó mooi, je ziet het verschil nooit!",
    "vadsrijk.z6raid.spiegelvizier.zeg.gevonden": "Huh?! Wie heeft mijn spiegels gedraaid?",
    "vadsrijk.z6raid.spiegelvizier.zeg.wissel": "Wie is wie? Hihihi!",
    "vadsrijk.z6raid.spiegelvizier.zeg.breekt": "Mijn spiegels! Weet je wel hoe lang ik die gepoetst heb?",
    # sterrenbeelden
    "vadsrijk.z6raid.ster_bultguh": "De Vliegende Bultguh",
    "vadsrijk.z6raid.ster_pluis": "De Kleine Pluis",
    "vadsrijk.z6raid.sterrenbeelden.venster": "Eén is uit! 10 tellen voor de andere twee",
    "vadsrijk.z6raid.sterrenbeelden.venster_hud": "Steek-aan-venster",
    "vadsrijk.z6raid.sterrenbeelden.een_uit": "%s straalt weer (%s/%s)!",
    "vadsrijk.z6raid.sterrenbeelden.samen": "Alle drie stralen weer!",
    "vadsrijk.z6raid.sterrenbeelden.aangestoken": "Te laat: ze steken elkaar weer aan!",
    "vadsrijk.z6raid.sterrenbeelden.uit_al": "Deze straalt al: help de anderen!",
    "vadsrijk.z6raid.sterrenbeelden.klem": "Knuffelklem op %s: de rest weg ervan!",
    "vadsrijk.z6raid.sterrenbeelden.brul": "Berenbrul! Naar BINNEN, dicht bij de Knuffel!",
    "vadsrijk.z6raid.sterrenbeelden.lied": "Het Sterrenlied heelt de anderen!",
    "vadsrijk.z6raid.sterrenbeelden.zeg.aan": "Grrr... we laten elkaar niet uitgaan!",
    # ringeling
    "vadsrijk.z6raid.ringeling.fase2": "De Lege Lens zuigt het licht weg",
    "vadsrijk.z6raid.ringeling.fase3": "Ringdans!",
    "vadsrijk.z6raid.ringeling.lens": "De Lens beschermt haar: steek eerst alle bakens aan!",
    "vadsrijk.z6raid.ringeling.ring_komt": "Een %s komt op je af: SPRING in de roze cirkel om hem te vangen!",
    "vadsrijk.z6raid.ringeling.gevangen": "Gevangen! Terug naar de Hertogin!",
    "vadsrijk.z6raid.ringeling.geen_ringen": "Ze heeft geen ringen meer om te schoppen!",
    "vadsrijk.z6raid.ringeling.al_scherf": "Je draagt al een sterrenscherf.",
    "vadsrijk.z6raid.ringeling.scherf_op": "Je draagt een sterrenscherf: breng hem naar een gedoofd baken!",
    "vadsrijk.z6raid.ringeling.geen_scherf": "Pak eerst een sterrenscherf.",
    "vadsrijk.z6raid.ringeling.baken": "Een baken brandt weer (%s/%s)!",
    "vadsrijk.z6raid.ringeling.licht": "Het licht is terug!",
    "vadsrijk.z6raid.ringeling.ring_laag": "Lage ring: SPRING!",
    "vadsrijk.z6raid.ringeling.ring_hoog": "Hoge ring: DUIK (sluip)!",
    "vadsrijk.z6raid.ringeling.val": "De Lens barst: ze valt!",
    "vadsrijk.z6raid.ringeling.zeg.schop": "Vang dan, als je kan!",
    "vadsrijk.z6raid.ringeling.zeg.lens": "Lens, drink het licht! Alles, alles, ALLES voor de Keizer!",
    "vadsrijk.z6raid.ringeling.zeg.licht": "Nee! Mijn duisternis! Wie steekt hier zomaar sterren aan?",
    "vadsrijk.z6raid.ringeling.zeg.dans": "Dansen jullie mee? Mijn ringen zijn groter dan jullie!",
    "vadsrijk.z6raid.ringeling.zeg.einde1": "Mijn... tenen... Niemand heeft ooit gevraagd of ík wel blij was.",
    "vadsrijk.z6raid.ringeling.zeg.einde2": "Kijk... de sterren. Ze branden weer. Ik was vergeten hoe mooi ze zijn.",
    "vadsrijk.z6raid.ringeling.zeg.einde3": "De Keizer krijgt geen ring meer van mij. Ik help jullie, ridders. Kom me opzoeken in Saffraanpoort!",
    "vadsrijk.z6raid.ring.vuur": "vuurring",
    "vadsrijk.z6raid.ring.ijs": "ijsring",
    "vadsrijk.z6raid.ring.lichtdief": "lichtdiefring",
    "vadsrijk.z6raid.ring.duw": "duwring",
    "vadsrijk.z6raid.ring.wissel": "wisselring",
    # loot
    "vadsrijk.z6raid.loot.teenring": "%s vond Ringelings Teenring!",
    "vadsrijk.z6raid.loot.vleugels": "%s kreeg de Sterrenring-vleugels uit De Sterrendovende Toren!",
    "vadsrijk.z6raid.halster.ok": "De Sterrennacht-bultguh is van jou!",
    "vadsrijk.z6raid.halster.later": "De bultguh-stal is nog niet open in deze versie: bewaar dit halster goed.",
    "item.guhs.vr_z6raid_halster.uitleg": "Rechtsklik: de Sterrennacht-bultguh (rijdier).",
    # titles
    "gui.guhs.titels.naam.z6raid_ringbreker": "Ringbreker",
    "gui.guhs.titels.hint.z6raid_ringbreker": "Haal De Sterrendovende Toren uit.",
    "gui.guhs.titels.naam.z6raid_sterrenaansteker": "Sterrenaansteker",
    "gui.guhs.titels.hint.z6raid_sterrenaansteker": "Haal De Sterrendovende Toren uit zonder één keer flauw te vallen.",
    # wings (t-kleding)
    "vadsrijk.kleding.ding.vleugel.sterrenring": "Sterrenring-vleugels",
    "vadsrijk.kleding.ding.vleugel.sterrenring.bron": "Een zeldzame vondst in de kist van De Sterrendovende Toren.",
    # pet
    "vadsrijk.huisdier.sterrenpluisje.naam": "Sterrenpluisje",
    "vadsrijk.huisdier.sterrenpluisje.tekst": "De Kleine Pluis, opgevrolijkt en piepklein. Gloeit 's nachts.",
    "vadsrijk.huisdier.sterrenpluisje.bron": "Zeldzaam uit de kist van De Sterrendovende Toren.",
    # attack names (cast bar)
    "vadsrijk.vfx.aanval.zandloper": "De zandloper loopt leeg",
    "vadsrijk.vfx.aanval.zandstorm": "Zandstorm",
    "vadsrijk.vfx.aanval.verblindende_glans": "Verblindende Glans",
    "vadsrijk.vfx.aanval.sterrenlied": "Sterrenlied",
    "vadsrijk.vfx.aanval.ring_laag": "Lage ring",
    "vadsrijk.vfx.aanval.ring_hoog": "Hoge ring",
}
EN = {
    "vadsrijk.z6raid.titel": "The Star-Snuffing Tower",
    "vadsrijk.z6raid.start.alleen": "The tower won't let you in alone: bring at least one friend (2-10 players).",
    "vadsrijk.z6raid.wipe": "Wipe!",
    "vadsrijk.z6raid.onderbroken": "Interrupted!",
    "vadsrijk.z6raid.spiegelkloon": "Mirror clone",
    "gui.guhs.titels.naam.z6raid_ringbreker": "Ringbreaker",
    "gui.guhs.titels.naam.z6raid_sterrenaansteker": "Starlighter",
    "vadsrijk.kleding.ding.vleugel.sterrenring": "Star-ring wings",
    "vadsrijk.huisdier.sterrenpluisje.naam": "Starfluff",
}
TIPS = {
    "zandwachter": "Raid-tip: de Zandwachter neemt pas echt schade als de zandloper is omgedraaid. Twee groepjes op de twee roze platen, tegelijk! Daarna: sla hem in zijn RUG.",
    "spiegelvizier": "Raid-tip: alleen de echte Spiegelvizier fonkelt af en toe. Draai (rechtsklik) twee spiegels naar hetzelfde beeld om hem te ontmaskeren.",
    "sterrenbeelden": "Raid-tip: de drie sterrenbeelden moeten binnen 10 tellen na elkaar uit. Breng ze eerst alle drie laag, en onderbreek het Sterrenlied van de Pluis!",
    "ster_bultguh": "Raid-tip: de Bultguh stormt over een rode baan: stap eruit.",
    "ster_pluis": "Raid-tip: de Kleine Pluis heelt de anderen: onderbreek haar lied.",
    "ringeling": "Raid-tip: SPRING in de roze cirkel om een schoppende ring te vangen. In het donker: pak sterrenscherven en breng ze naar de bakens. Lage ring = springen, hoge ring = sluipen.",
}
RAID_TIP = "Raid: iedereen doet schade, rollen zijn vrij. Flauwgevallen? Je wordt wakker op de trap van deze verdieping en mag weer meedoen. Valt de hele groep, dan begint de verdieping opnieuw."
FASES = {"zandwachter": {2: "Meer pilaren!", 3: "De laatste korrels"}, "spiegelvizier": {2: "De zaal breekt"},
         "ringeling": {2: "De Lege Lens", 3: "Ringdans"}}

SET = {"sterrenzijde_tulband": ("hoofd", "Sterrenzijde-tulband", "Starsilk Turban"),
       "sterrenzijde_mantel": ("borst", "Sterrenzijde-mantel", "Starsilk Mantle"),
       "sterrenzijde_pofbroek": ("benen", "Sterrenzijde-pofbroek", "Starsilk Trousers"),
       "sterrenzijde_teenslofjes": ("voeten", "Sterrenzijde-teenslofjes", "Starsilk Toe Slippers")}
SET_EFFECT = "Sterrenzijde-set: 2 stuks = +15 Verdediging en +150 Vads; 4 stuks = Schade tegen bazen +10%"
WAPENS = {"ringbreker": ("Ringbreker", "Ringbreaker", "Zwaard (Ridder) uit De Sterrendovende Toren", "Een ring breekt, een vriendschap begint."),
          "komeetboog": ("Komeetboog", "Comet Bow", "Boog (Jager) uit De Sterrendovende Toren", "Elke pijl een vallende ster. Doe een wens!"),
          "sterrenstaf": ("Sterrenstaf", "Star Staff", "Staf (Magier) uit De Sterrendovende Toren", "Er zit een stukje van de Lege Lens in, nu vol licht.")}


def teksten():
    t = {k: (v, EN.get(k, v)) for k, v in TEKSTEN.items()}
    for k, (nl, en) in ENTITEITEN.items():
        t[f"entity.guhs.{k}"] = (nl, en)
    for b, (nl, en, onder) in BAZEN.items():
        t[f"vadsrijk.z6raid.{b}"] = (nl, en)
        t[f"vadsrijk.z6raid.{b}.onder"] = (onder, onder)
    for b, tip in TIPS.items():
        t[f"vadsrijk.z6raid.tip.{b}"] = (tip, tip)
        t[f"vadsrijk.z2bazen.tip.{b}"] = (tip, tip)
        t[f"vadsrijk.z3bazen.tip.{b}"] = (RAID_TIP, RAID_TIP)
    for b, f in FASES.items():
        for n, regel in f.items():
            t[f"vadsrijk.kern.beleving.fase.vr_z6raid_baas_{b if b != 'sterrenbeelden' else 'ster_knuffel'}.{n}"] = (f"Fase {n}: {regel}",) * 2
    for k, (_slot, nl, en) in SET.items():
        t[f"item.guhs.vr_z6raid_{k}"] = (nl, en)
        t[f"vadsrijk.loot.z6raid_{k}"] = (nl, en)
        t[f"vadsrijk.loot.z6raid_{k}.flavour.0"] = ("\"Glanzende zijde met sterretjes. Er zitten gouden teenringetjes aan.\"",) * 2
        t[f"vadsrijk.bazen.effect.z6raid_{k}"] = (SET_EFFECT, SET_EFFECT)
    for k, (nl, en, effect, fl) in WAPENS.items():
        t[f"item.guhs.vr_z6raid_{k}"] = (nl, en)
        t[f"vadsrijk.loot.z6raid_{k}"] = (nl, en)
        t[f"vadsrijk.loot.z6raid_{k}.flavour.0"] = (f"\"{fl}\"",) * 2
        t[f"vadsrijk.bazen.effect.z6raid_{k}"] = (effect, effect)
    t["item.guhs.vr_z6raid_teenring"] = ("Ringelings Teenring", "Ringeling's Toe Ring")
    t["vadsrijk.loot.z6raid_teenring"] = ("Ringelings Teenring", "Ringeling's Toe Ring")
    t["vadsrijk.loot.z6raid_teenring.flavour.0"] = ("\"Eén van de tien. Ze mist hem niet, zegt ze.\"",) * 2
    t["vadsrijk.bazen.effect.z6raid_teenring"] = ("Guhddelijke ring uit De Sterrendovende Toren",) * 2
    t["item.guhs.vr_z6raid_ringreplica"] = ("Ringreplica (gildetrofee)", "Ring Replica (guild trophy)")
    t["item.guhs.vr_z6raid_halster"] = ("Sterrennacht-halster", "Starnight Halter")
    return t


# =====================================================================================================================
# writing
# =====================================================================================================================
def controleer(naam, m, anims, nodig):
    bones = {b["name"] for b in m.bones}
    for an, a in anims.items():
        for b in a["bones"]:
            assert b in bones, f"{naam}: anim {an} moves unknown bone {b}"
    for n in nodig:
        assert n in anims, f"{naam}: missing anim {n}"


def _alpha(img):
    a = np.asarray(img).copy()
    zwart = a[..., :3].sum(axis=2) < 12
    a[zwart, 3] = 0
    return Image.fromarray(a)


def _vfx_anims(m, anims):
    """T-VFX's generic moves (vfx_windup/treffer/geraakt/fase/woede/juich) on this model's bones."""
    try:
        from features import vadsrijk_t_vfx as tv
    except ImportError:
        return anims
    out = dict(anims)
    out.update(tv.generiek({b["name"] for b in m.bones}))
    return out


def _gezicht_mood(m, mood):
    """Paint variant: every face region gets this mood (Mika -> blij for the cheered-up texture)."""
    oud = []
    for reg in m.regios:
        if reg["gezicht"]:
            oud.append((reg, dict(reg["gezicht"])))
            reg["gezicht"] = dict(reg["gezicht"], mood=mood)
    return oud


def _herstel(oud):
    for reg, g in oud:
        reg["gezicht"] = g


def _item(a, w, item, tex, img, handheld=False):
    pad = os.path.join(a, "textures", "item", *tex.split("/")) + ".png"
    if not hasattr(img, "save"):
        os.makedirs(os.path.dirname(pad), exist_ok=True)
        img.png(pad)
    else:
        vw._png(pad, img)
    parent = "minecraft:item/handheld" if handheld else "minecraft:item/generated"
    w(os.path.join(a, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
    w(os.path.join(a, "models", "item", item + ".json"), {"parent": parent, "textures": {"layer0": "guhs:item/" + tex}})


def _komeet(d):
    d.disc(11.5, 4, 2.6, (255, 236, 150))
    d.disc(11, 3.4, 1, (255, 255, 230))
    for i in range(4):
        d.px(8 - i, 7 + i, (200, 180, 255))


def _ster_top(d):
    d.disc(11, 5, 2.6, (255, 230, 140))
    for (x, y) in ((11, 1), (15, 5), (11, 9), (7, 5)):
        d.lijn(11, 5, x, y, (255, 246, 200))


def halster_icoon():
    im = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(im)
    d.ellipse((2, 2, 13, 13), outline=(40, 30, 90, 255), width=2)
    d.ellipse((4, 4, 11, 11), outline=(255, 214, 90, 255), width=1)
    for (x, y) in ((7, 1), (13, 7), (2, 9)):
        im.putpixel((x, y), (255, 250, 210, 255))
    return im


def assets(a, w=vw._schrijf_json):
    geo = os.path.join(a, "geckolib", "models", "entity")
    ani = os.path.join(a, "geckolib", "animations", "entity")
    td = os.path.join(a, "textures", *SUB.split("/"))
    n = 0

    def schrijf_model(mid, m, anims, nodig, vfx=False):
        nonlocal n
        anims = _vfx_anims(m, anims) if vfx else anims
        controleer(mid, m, anims, nodig)
        w(os.path.join(geo, mid + ".geo.json"), m.geo())
        w(os.path.join(ani, mid + ".animation.json"), anim_bestand(anims))
        n += 1

    # the golem
    with _v2():
        m, an, pal, glow = m_zandwachter()
    schrijf_model("vr_z6raid_baas_zandwachter", m, an, ["idle", "walk", "slag", "golf", "draai", "storm"], vfx=True)
    vw._png(os.path.join(td, "zandwachter.png"), vw.schilder(m, pal, "boos", 6100, False))
    oud = _gezicht_mood(m, "blij")
    vw._png(os.path.join(td, "zandwachter_blij.png"), vw.schilder(m, pal, "blij", 6100, False))
    _herstel(oud)
    g = _alpha(vw.gloed(m, glow, 6101))
    vw._png(os.path.join(td, "zandwachter_glowmask.png"), g)
    vw._png(os.path.join(td, "zandwachter_blij_glowmask.png"), g)
    # the two Mika's
    for naam, maak, nodig, seed in (("spiegelvizier", m_spiegelvizier, ["idle", "walk", "poets", "straal", "wissel", "verstop"], 6200),
                                    ("ringeling", m_ringeling, ["idle", "walk", "schop", "trap", "zweef", "val", "huil", "blij"], 6300)):
        with _v2():
            m, an, pal, glow = maak()
        schrijf_model("vr_z6raid_baas_" + naam, m, an, nodig, vfx=True)
        vw._png(os.path.join(td, naam + ".png"), z2.schilder(m, pal, seed))
        oud = _gezicht_mood(m, "blij")
        vw._png(os.path.join(td, naam + "_blij.png"), z2.schilder(m, pal, seed))
        _herstel(oud)
        gm = _alpha(z2.schilder(m, pal, seed + 1, glow, glow=True))
        vw._png(os.path.join(td, naam + "_glowmask.png"), gm)
        vw._png(os.path.join(td, naam + "_blij_glowmask.png"), gm)
        if naam == "spiegelvizier":   # the clone: pale glassy blue
            bleek = {k: (tuple(int(200 + c * 0.2) for c in v[0]), v[1], v[2]) for k, v in pal.items()}
            vw._png(os.path.join(td, "spiegelkloon.png"), z2.schilder(m, bleek, seed + 7))
            vw._png(os.path.join(td, "spiegelkloon_glowmask.png"), gm)
    # the constellations: grey (gedoofd) / bright (blij)
    for naam, maak, seed in (("ster_knuffel", m_ster_knuffel, 6400), ("ster_bultguh", m_ster_bultguh, 6410), ("ster_pluis", m_ster_pluis, 6420)):
        m, an = maak()
        nodig = ["idle", "walk", "uit", "klem", "brul", "storm", "lied"]
        schrijf_model("vr_z6raid_baas_" + naam, m, an, nodig, vfx=True)
        vw._png(os.path.join(td, naam + ".png"), vw.schilder(m, STER_PAL, "boos", seed, True))
        vw._png(os.path.join(td, naam + "_blij.png"), vw.schilder(m, STER_PAL, "blij", seed, False))
        grijs = {k: tuple(int(c * 0.35) for c in v) for k, v in STER_GLOW.items()}
        vw._png(os.path.join(td, naam + "_glowmask.png"), _alpha(vw.gloed(m, grijs, seed + 1)))
        vw._png(os.path.join(td, naam + "_blij_glowmask.png"), _alpha(vw.gloed(m, STER_GLOW, seed + 2)))
    # the objects
    for naam, maak, seed in (("zandpilaar", m_zandpilaar, 6500), ("draaispiegel", m_draaispiegel, 6510), ("sterrenscherf", m_sterrenscherf, 6520)):
        with _v2():
            m, an, pal, glow = maak()
        schrijf_model("vr_z6raid_" + naam, m, an, ["idle"])
        vw._png(os.path.join(td, naam + ".png"), vw.schilder(m, pal, "blij", seed, False))
        if glow:
            vw._png(os.path.join(td, naam + "_glowmask.png"), _alpha(vw.gloed(m, glow, seed + 1)))
    with _v2():
        m, an, pal, pal_aan = m_sterrenbaken()
    schrijf_model("vr_z6raid_sterrenbaken", m, an, ["idle"])
    vw._png(os.path.join(td, "sterrenbaken.png"), vw.schilder(m, pal, "blij", 6530, False))
    vw._png(os.path.join(td, "sterrenbaken_aan.png"), vw.schilder(m, pal_aan, "blij", 6530, False))
    vw._png(os.path.join(td, "sterrenbaken_glowmask.png"), _alpha(vw.gloed(m, {"ster": (40, 40, 50)}, 6531)))
    vw._png(os.path.join(td, "sterrenbaken_aan_glowmask.png"), _alpha(vw.gloed(m, {"ster": (255, 220, 140), "goud": (120, 90, 40)}, 6532)))
    # items
    for i, (k, (slot, nl, en)) in enumerate(SET.items()):
        _item(a, w, "vr_z6raid_" + k, f"{SUB}/{k}", vw.icoon(slot, (236, 150, 210), (255, 220, 110), None, 6600 + i))
    _item(a, w, "vr_z6raid_ringbreker", f"{SUB}/ringbreker", zwaard((255, 214, 120), (190, 90, 255)), handheld=True)
    _item(a, w, "vr_z6raid_komeetboog", f"{SUB}/komeetboog", boog((120, 90, 200), (255, 230, 150)), handheld=True)
    _item(a, w, "vr_z6raid_sterrenstaf", f"{SUB}/sterrenstaf", staf(_ster_top), handheld=True)
    _item(a, w, "vr_z6raid_teenring", f"{SUB}/teenring", z2.sieraad_icoon("ring", (255, 214, 80), (200, 110, 255), 6610))
    _item(a, w, "vr_z6raid_ringreplica", f"{SUB}/ringreplica", z3.trofee_icoon((255, 214, 80), 6611))
    _item(a, w, "vr_z6raid_halster", f"{SUB}/halster", halster_icoon())
    vleugels(a)
    return n


def vleugels(a):
    """The Sterrenring wings for t-kleding (three posed item models + texture), with t-kleding's own helpers."""
    try:
        from features import vadsrijk_t_kleding as K
    except ImportError:
        print("vadsrijk_u_z6raid: no vadsrijk_t_kleding (t-kleding): wings skipped")
        return
    oud = K.vleugel_elementen

    def elementen(naam):
        if naam != "sterrenring":
            return oud(naam)
        el = [K._plaat(0, 12, 7.5, 9, d=0.6)]
        for i, (ln, up) in enumerate([(4, 4), (6, 5), (7, 6), (5, 4), (3, 3)]):
            x = i * 2.4
            el.append(K._plaat(x, x + 2.0, 8 - ln, 8 + up, d=0.4))
        el.append(K._box([10, 13, 7.6], [12.5, 15.5, 8.4]))   # a little gold ring at the tip
        return el

    K.vleugel_elementen = elementen
    try:
        for pose in K.VLEUGEL_POSES:
            K._model("vleugel", f"sterrenring_{pose}", K.vleugel_paar("sterrenring", pose), a, tex="vleugel_sterrenring")
    finally:
        K.vleugel_elementen = oud
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            c = (232, 214, 255) if (x * 3 + y) % 7 else (255, 240, 180)
            if (x + y * 5) % 23 == 0:
                c = (255, 255, 240)
            px[x, y] = c + (255,)
    td = os.path.join(a, "textures", "item", "vr_t_kleding")
    os.makedirs(td, exist_ok=True)
    im.save(os.path.join(td, "vleugel_sterrenring.png"))


def uiterlijken(a, w=vw._schrijf_json):
    """The worn look of the Sterrenzijde set: the vanilla gold layers tinted rose-gold with a lilac shimmer."""
    uid = "vr_z6raid_sterrenzijde"
    w(os.path.join(a, "equipment", uid + ".json"), {"layers": {"humanoid": [{"texture": "guhs:" + uid}], "humanoid_leggings": [{"texture": "guhs:" + uid}]}})
    jar = os.path.join("build", "moddev", "artifacts", "minecraft-patched-26.1.2.112.jar")
    if not os.path.exists(jar):
        print("vadsrijk_u_z6raid: no minecraft jar in build/ (run a gradle build first): worn look textures skipped")
        return
    with zipfile.ZipFile(jar) as z:
        for laag in ("humanoid", "humanoid_leggings"):
            img = Image.open(BytesIO(z.read(f"assets/minecraft/textures/entity/equipment/{laag}/gold.png"))).convert("RGBA")
            px = img.load()
            for y in range(img.height):
                for x in range(img.width):
                    r, g, b, al = px[x, y]
                    l = min(1.0, (0.3 * r + 0.59 * g + 0.11 * b) / 255 * 1.2)
                    k = (250, 168, 220) if (x + y) % 9 else (210, 180, 255)
                    px[x, y] = (int(k[0] * l), int(k[1] * l), int(k[2] * l), al)
            doel = os.path.join(a, "textures", "entity", "equipment", laag, uid + ".png")
            os.makedirs(os.path.dirname(doel), exist_ok=True)
            img.save(doel)


def build(h):
    assets(h.A, h.w)
    uiterlijken(h.A, h.w)
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    n = assets(a)
    uiterlijken(a)
    t = teksten()
    for i, taal in enumerate(("nl_nl", "en_us")):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in t.items()})
        vw._schrijf_json(p, data)
    print(f"vadsrijk_u_z6raid: {n} models, {len(t)} texts")


if __name__ == "__main__":
    _direct()
