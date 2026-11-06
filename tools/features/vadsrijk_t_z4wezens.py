"""
1.10 "Het Vadsrijk", zone 4 "De Lampionkust", slice t-z4wezens (T_BRIEF.md). Java: feature/vadsrijk/zone4/{wezens,bazen}.

Everything generated (no hand-made art), on the zone-1 helpers (vadsrijk_wezens.Model / schilder / gezicht: the "ontvadst" grey
grumpy look and the bright happy look after the pink plof) and the zone-2 Mika body (vadsrijk_z2_wezens.mika):
  * 10 creature models + animations (assets/guhs/geckolib/{models,animations}/entity/vadsrijk/zone4/wezens/):
      mopperkrab, koraalkobold, stekelpluis, zeurslak, lampionkwal, mokmeeuw, bromvlieger, pruilrob, mistmopje (ontvadst, cheered up)
      vliegerknecht (the Mika kite crew of the Wrak)
    per creature <naam>.png (grey, grumpy), <naam>_blij.png (bright, happy, blush), <naam>_gloed.png, <naam>_elite.png.
  * the bosses (geckolib/.../entity/vr_z4_baas_*): zeeslurf (sea cow + jellyfish veil, glowing lantern frills), vliegervanger
    (Kootje: a Mika captain with a war kite bone), poliep (grey polyp), lier (deck winch, rope bones touw_1..3) and the pet
    vliegertje; textures in textures/vadsrijk/z4bazen/ (+ _glowmask for the AutoGlowing layer).
  * gear: 8 armour pieces (2 sets), 3 jewellery, 6 class weapons, 2 crystals, 6 named boss items, 2 trophies (16x16 icons, item
    models, worn looks); all Dutch texts (en_us gets real English for the names where cheap).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_z4wezens.py
"""
import contextlib
import json
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
    from features import vadsrijk_z3_wezens as z3
    from features.vadsrijk_economie import Doek, _licht, zwaard, boog, staf
    from features.vadsrijk_z2_systemen import kristal, kruisboog
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
    from features import vadsrijk_z3_wezens as z3
    from features.vadsrijk_economie import Doek, _licht, zwaard, boog, staf
    from features.vadsrijk_z2_systemen import kristal, kruisboog

Model, kf, anim, anim_bestand = vw.Model, vw.kf, vw.anim, vw.anim_bestand
samen, adem, een, stil = z2.samen, z2.adem, z2.een, z2.stil
SUB = "vadsrijk/zone4/wezens"
BSUB = "vadsrijk/z4bazen"
OOG = ((40, 30, 50), None, None)


def _v2():
    """t-models' model kit v2 (rounded boxes, v2 painting, faces in model space) when this tree has it, else the old style."""
    return vw.stijl(2) if hasattr(vw, "stijl") else contextlib.nullcontext()


def _naloop(m, anims):
    """t-models' secondary motion (ears/tails lag a little) when available."""
    if hasattr(vw, "_kit"):
        try:
            vw._kit().naloop(m, anims)
        except Exception as e:                        # (a newer kit with another signature: skip the extra wiggle)
            print("vadsrijk_t_z4wezens: naloop skipped:", e)
    return anims


def bob(bone, t=2.0, h=0.8):
    return {bone: {"position": kf([(0, (0, 0, 0)), (t / 2, (0, h, 0)), (t, (0, 0, 0))])}}


def draai(bone, a, t=0.8, as_="z"):
    i = {"x": 0, "y": 1, "z": 2}[as_]
    v = lambda s: tuple(s if j == i else 0 for j in range(3))
    return {bone: {"rotation": kf([(0, v(a)), (t / 2, v(-a)), (t, v(a))])}}


def hup(bone="root", t=0.5, h=3.0):
    return {bone: {"position": kf([(0, (0, 0, 0)), (t / 2, (0, h, 0)), (t, (0, 0, 0))])}}


def rond(bone, t=1.0):
    return {bone: {"rotation": kf([(0, (0, 0, 0)), (t, (0, 360, 0))])}}


# =====================================================================================================================
# the creatures (front = -z; the face sits on the north face of the cube with "gezicht")
# =====================================================================================================================
def m_mopperkrab():
    m = Model("mopperkrab", px=4, breedte=96, bounds=(2, 1.5, 0.4))
    m.bone("root")
    m.bone("lijf", "root", (0, 4, 0))
    m.cube("lijf", "schild", (-6, 2, -5), (12, 6, 10), gezicht={"mood": "blij", "ey": 0.42})
    m.cube("lijf", "schild", (-5, 8, -4), (10, 1.5, 8))
    m.cube("lijf", "buik", (-5.5, 1.4, -4.5), (11, 1, 9))
    for sx, kant in ((-1, "l"), (1, "r")):
        m.cube("lijf", "steel", (sx * 2.5 - 0.5, 8, -4), (1, 3, 1))
        m.cube("lijf", "oog", (sx * 2.5 - 1, 11, -4.5), (2, 2, 2))
        b = "schaar_" + kant
        m.bone(b, "lijf", (sx * 6, 5, -3))
        m.cube(b, "poot", (sx * 6 + (0 if sx > 0 else -2), 4, -5), (2, 2, 3))
        m.cube(b, "schaar", (sx * 7.5 - 2.5, 3, -10), (5, 4, 5))
        m.cube(b, "schaar", (sx * 7.5 - 1.5, 7, -10), (3, 1.5, 3))
        p = "poten_" + kant
        m.bone(p, "lijf", (sx * 6, 3, 0))
        for i, z in enumerate((-2.5, 0.5, 3.5)):
            m.cube(p, "poot", (sx * 6 + (0 if sx > 0 else -4), 0, z), (4, 1.2, 1.2), rot=(0, 0, -25 * sx), pivot=(sx * 6, 2, z))
    a = {
        "idle": anim(2.0, samen(adem("lijf", 2.0, 1.03), draai("schaar_l", 8, 2.0, "x"), draai("schaar_r", -8, 2.0, "x"))),
        "loop": anim(0.5, samen({"root": {"position": kf([(0, (-0.6, 0, 0)), (0.25, (0.6, 0.4, 0)), (0.5, (-0.6, 0, 0))])}},
                                draai("poten_l", 20, 0.5), draai("poten_r", 20, 0.5))),
        "aanval": een(0.4, {"schaar_l": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-40, -20, 0)), (0.3, (10, 0, 0)), (0.4, (0, 0, 0))])},
                            "schaar_r": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-40, 20, 0)), (0.3, (10, 0, 0)), (0.4, (0, 0, 0))])}}),
        "schild": anim(1.0, {"schaar_l": {"rotation": kf([(0, (-55, -35, 0))])}, "schaar_r": {"rotation": kf([(0, (-55, 35, 0))])}}),
        "blij": anim(0.6, samen(hup("root", 0.6, 2.5), draai("schaar_l", 40, 0.6, "z"), draai("schaar_r", -40, 0.6, "z"))),
    }
    pal = {"schild": ((240, 120, 104), "plekjes", (250, 160, 140)), "buik": ((252, 214, 196), None, None), "schaar": ((236, 104, 96), None, None),
           "poot": ((230, 130, 110), None, None), "steel": ((240, 150, 130), None, None), "oog": OOG}
    return m, a, pal, {}


def m_koraalkobold():
    m = Model("koraalkobold", px=4, breedte=64, bounds=(1.5, 1.5, 0.4))
    m.bone("root")
    for sx, kant in ((-1, "l"), (1, "r")):
        b = "been_" + kant
        m.bone(b, "root", (sx * 1.5, 4, 0))
        m.cube(b, "huid", (sx * 1.5 - 1, 0, -1), (2, 4, 2))
    m.bone("lijf", "root", (0, 4, 0))
    m.cube("lijf", "huid", (-2.5, 4, -2), (5, 5, 4))
    m.bone("kop", "lijf", (0, 9, 0))
    m.cube("kop", "huid", (-3.5, 9, -3.5), (7, 6, 6), gezicht={"mood": "blij", "ey": 0.45})
    for x, z, h in ((-2.5, -1, 4), (0.2, 0.5, 5), (2, -1.5, 3.5)):
        m.cube("kop", "koraal", (x - 0.6, 15, z - 0.6), (1.2, h, 1.2))
        m.cube("kop", "koraal", (x - 1.6, 15 + h * 0.6, z - 0.5), (3.2, 1, 1))
    for sx, kant in ((-1, "l"), (1, "r")):
        b = "arm_" + kant
        m.bone(b, "lijf", (sx * 3, 8.5, 0))
        m.cube(b, "huid", (sx * 3 + (0 if sx > 0 else -1.5), 5.5, -0.8), (1.5, 3.5, 1.6))
    m.cube("arm_r", "koraal", (3.2, 4.5, -1.5), (1.6, 1.6, 1.6))
    lk = lambda a, t=0.4: {"rotation": kf([(0, (a, 0, 0)), (t / 2, (-a, 0, 0)), (t, (a, 0, 0))])}
    a = {
        "idle": anim(1.6, samen(adem("lijf", 1.6, 1.04), draai("kop", 6, 1.6, "z"))),
        "loop": anim(0.4, samen({"been_l": lk(35), "been_r": lk(-35), "arm_l": lk(-30)}, hup("lijf", 0.4, 0.6))),
        "aanval": een(0.4, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-100, 0, 0)), (0.4, (0, 0, 0))])}}),
        "spuug": een(0.5, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-160, 0, 0)), (0.3, (-40, 0, 0)), (0.5, (0, 0, 0))])}}),
        "blij": anim(0.5, samen(hup("root", 0.5, 3), draai("arm_l", 60, 0.5, "z"), draai("arm_r", -60, 0.5, "z"))),
    }
    pal = {"huid": ((120, 200, 196), "plekjes", (150, 220, 214)), "koraal": ((255, 128, 150), "plekjes", (255, 180, 190)), "oog": OOG}
    return m, a, pal, {}


def m_stekelpluis():
    m = Model("stekelpluis", px=4, breedte=96, bounds=(1.5, 1.5, 0.4))
    m.bone("root")
    m.bone("bal", "root", (0, 5.5, 0))
    m.cube("bal", "pluis", (-5, 1, -5), (10, 9, 10), gezicht={"mood": "blij", "ey": 0.45})
    m.cube("bal", "pluis", (-4, 0.4, -4), (8, 10.4, 8))
    m.bone("stekels", "bal", (0, 5.5, 0))
    for i in range(10):
        hoek = i * 36
        m.cube("stekels", "stekel", (-0.5, 9.5, -0.5), (1, 3, 1), rot=(55, hoek, 0), pivot=(0, 5.5, 0))
    for i in range(6):
        m.cube("stekels", "stekel", (-0.5, 9.5, -0.5), (1, 2.5, 1), rot=(100, i * 60 + 30, 0), pivot=(0, 5.5, 0))
    a = {
        "idle": anim(2.0, samen(adem("bal", 2.0, 1.04), {"stekels": {"rotation": kf([(0, (0, 0, 0)), (2.0, (0, 30, 0))])}})),
        "rol": anim(0.8, {"bal": {"rotation": kf([(0, (0, 0, 0)), (0.8, (-360, 0, 0))])}}),
        "aanval": een(0.3, {"stekels": {"scale": kf([(0, (1, 1, 1)), (0.15, (1.3, 1.3, 1.3)), (0.3, (1, 1, 1))])}}),
        "puf": een(0.6, {"stekels": {"scale": kf([(0, (1, 1, 1)), (0.2, (1.6, 1.6, 1.6)), (0.6, (1, 1, 1))])},
                         "bal": {"scale": kf([(0, (1, 1, 1)), (0.2, (1.15, 1.15, 1.15)), (0.6, (1, 1, 1))])}}),
        "blij": anim(0.5, samen(hup("root", 0.5, 3), {"stekels": {"scale": kf([(0, (0.6, 0.6, 0.6))])}})),
    }
    pal = {"pluis": ((150, 110, 200), "plekjes", (190, 150, 230)), "stekel": ((250, 220, 250), None, None), "oog": OOG}
    return m, a, pal, {}


def m_zeurslak():
    m = Model("zeurslak", px=4, breedte=96, bounds=(1.8, 1.5, 0.4))
    m.bone("root")
    m.bone("lijf", "root", (0, 2, 0))
    m.cube("lijf", "huid", (-3, 0, -6), (6, 3.5, 13))
    m.bone("kop", "lijf", (0, 3, -6))
    m.cube("kop", "huid", (-3, 0, -10), (6, 6, 4), gezicht={"mood": "blij", "ey": 0.4})
    for sx in (-1, 1):
        m.cube("kop", "huid", (sx * 1.6 - 0.4, 6, -9), (0.8, 3, 0.8))
        m.cube("kop", "oog", (sx * 1.6 - 0.8, 9, -9.4), (1.6, 1.6, 1.6))
    m.bone("huis", "root", (0, 3, 1))
    m.cube("huis", "huis", (-3.5, 3, -2), (7, 9, 9))
    m.cube("huis", "huis2", (-2.5, 6, -1), (5, 5, 7), inflate=0.3)
    m.cube("huis", "huis", (-1.5, 8, 0), (3, 3, 5), inflate=0.6)
    a = {
        "idle": anim(2.4, samen(adem("huis", 2.4, 1.02), draai("kop", 5, 2.4, "z"))),
        "kruip": anim(1.2, samen({"lijf": {"scale": kf([(0, (1, 1, 1)), (0.6, (0.92, 1, 1.12)), (1.2, (1, 1, 1))])}}, draai("kop", 4, 1.2, "x"))),
        "aanval": een(0.5, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-25, 0, 0)), (0.35, (15, 0, 0)), (0.5, (0, 0, 0))])}}),
        "schelp": anim(1.0, {"kop": {"scale": kf([(0, (0.1, 0.1, 0.1))])}, "lijf": {"scale": kf([(0, (0.4, 0.3, 0.3))])}}),
        "blij": anim(0.8, samen(hup("root", 0.8, 2), rond("huis", 0.8))),
    }
    pal = {"huid": ((180, 220, 150), "plekjes", (200, 236, 170)), "huis": ((250, 190, 120), "strepen", (240, 150, 110)),
           "huis2": ((255, 214, 150), "strepen", (250, 170, 130)), "oog": OOG}
    return m, a, pal, {}


def m_lampionkwal():
    m = Model("lampionkwal", px=4, breedte=96, bounds=(1.8, 2.2, 0.8))
    m.bone("root")
    m.bone("kap", "root", (0, 12, 0))
    m.cube("kap", "kap", (-6, 10, -6), (12, 8, 12), gezicht={"mood": "blij", "ey": 0.55})
    m.cube("kap", "kap", (-4.5, 18, -4.5), (9, 2, 9))
    m.cube("kap", "rand", (-6.5, 9, -6.5), (13, 1.5, 13))
    m.cube("kap", "lamp", (-2.5, 11.5, -2.5), (5, 5, 5))
    for i, (x, z) in enumerate(((-4, -4), (4, -4), (-4, 4), (4, 4), (0, 0))):
        b = f"tentakel_{i}"
        m.bone(b, "kap", (x, 9, z))
        m.cube(b, "tentakel", (x - 0.6, 1, z - 0.6), (1.2, 8, 1.2))
        m.cube(b, "lint", (x - 1, 0, z - 1), (2, 1.5, 2))
    zw = samen(*[draai(f"tentakel_{i}", 12 if i % 2 else -12, 1.6, "x") for i in range(5)])
    a = {
        "idle": anim(1.6, samen(bob("root", 1.6, 1.2), zw, {"kap": {"scale": kf([(0, (1, 1, 1)), (0.8, (1.06, 0.94, 1.06)), (1.6, (1, 1, 1))])}})),
        "zweef": anim(1.6, samen(bob("root", 1.6, 1.0), zw)),
        "aanval": een(0.4, samen(*[{f"tentakel_{i}": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-30, 0, 0)), (0.4, (0, 0, 0))])}} for i in range(5)])),
        "speciaal": een(0.6, {"kap": {"scale": kf([(0, (1, 1, 1)), (0.2, (1.25, 0.8, 1.25)), (0.6, (1, 1, 1))])}}),
        "blij": anim(1.0, samen(rond("root", 1.0), bob("kap", 1.0, 2))),
    }
    pal = {"kap": ((255, 170, 210), "glans", None), "rand": ((255, 210, 230), None, None), "lamp": ((255, 220, 120), None, None),
           "tentakel": ((250, 190, 225), None, None), "lint": ((255, 140, 190), None, None), "oog": OOG}
    return m, a, pal, {"lamp": (255, 200, 110)}


def m_mokmeeuw():
    m = Model("mokmeeuw", px=4, breedte=96, bounds=(2, 1.5, 0.4))
    m.bone("root")
    m.bone("lijf", "root", (0, 5, 0))
    m.cube("lijf", "veer", (-3, 3, -4), (6, 5, 9))
    m.cube("lijf", "staart", (-2.5, 4, 5), (5, 1.5, 4))
    m.bone("kop", "lijf", (0, 8, -3))
    m.cube("kop", "veer", (-2.8, 7, -8), (5.6, 5, 5), gezicht={"mood": "blij", "ey": 0.42})
    m.cube("kop", "snavel", (-1, 7.6, -11), (2, 1.6, 3))
    m.cube("kop", "vlek", (-0.4, 7.3, -10.8), (0.8, 0.6, 0.6))
    for sx, kant in ((-1, "l"), (1, "r")):
        b = "vleugel_" + kant
        m.bone(b, "lijf", (sx * 3, 7.5, -1))
        m.cube(b, "vleugel", (sx * 3 + (0 if sx > 0 else -9), 7, -3), (9, 0.8, 6))
        m.cube(b, "punt", (sx * 3 + (6 if sx > 0 else -9), 7.1, -2.5), (3, 0.6, 5))
        p = "poot_" + kant
        m.bone(p, "root", (sx * 1.2, 3, 0))
        m.cube(p, "poot", (sx * 1.2 - 0.4, 0, -0.4), (0.8, 3, 0.8))
        m.cube(p, "poot", (sx * 1.2 - 0.8, 0, -1.5), (1.6, 0.4, 2))
    klap = lambda t: samen(draai("vleugel_l", 35, t, "z"), draai("vleugel_r", -35, t, "z"))
    a = {
        "idle": anim(0.8, samen(klap(0.8), {"poot_l": {"rotation": kf([(0, (40, 0, 0))])}, "poot_r": {"rotation": kf([(0, (40, 0, 0))])}})),
        "vlieg": anim(0.5, samen(klap(0.5), {"poot_l": {"rotation": kf([(0, (60, 0, 0))])}, "poot_r": {"rotation": kf([(0, (60, 0, 0))])}})),
        "zit": anim(2.0, samen(adem("lijf", 2.0, 1.04), {"vleugel_l": {"rotation": kf([(0, (0, 0, -5))])}, "vleugel_r": {"rotation": kf([(0, (0, 0, 5))])}},
                               draai("kop", 10, 2.0, "y"))),
        "loop": anim(0.5, klap(0.5)),
        "aanval": een(0.5, samen({"lijf": {"rotation": kf([(0, (0, 0, 0)), (0.2, (40, 0, 0)), (0.5, (0, 0, 0))])}},
                                 {"vleugel_l": {"rotation": kf([(0, (0, 0, 60))])}, "vleugel_r": {"rotation": kf([(0, (0, 0, -60))])}})),
        "blij": anim(0.5, samen(klap(0.3), hup("root", 0.5, 3))),
    }
    pal = {"veer": ((246, 246, 250), "plekjes", (226, 230, 240)), "vleugel": ((200, 206, 220), "veer", (170, 176, 196)),
           "punt": ((70, 70, 80), None, None), "staart": ((230, 232, 240), None, None), "snavel": ((255, 200, 70), None, None),
           "vlek": ((240, 80, 70), None, None), "poot": ((255, 170, 90), None, None), "oog": OOG}
    return m, a, pal, {}


def m_bromvlieger():
    m = Model("bromvlieger", px=4, breedte=96, bounds=(2, 2.5, 0.8))
    m.bone("root")
    m.bone("vlieger", "root", (0, 14, 0))
    m.cube("vlieger", "doek", (-6, 8, -0.6), (12, 12, 1.2), rot=(0, 0, 45), pivot=(0, 14, 0))
    m.cube("vlieger", "doek2", (-5, 13.6, -0.9), (10, 0.8, 0.6))   # the cross bar
    m.cube("vlieger", "gezicht", (-3.5, 10.5, -1.2), (7, 6, 0.6), gezicht={"mood": "blij", "ey": 0.4})
    for sx in (-1, 1):
        m.cube("vlieger", "oor", (sx * 3 - 1.2, 21, -0.6), (2.4, 2.6, 1))
    m.bone("staart", "vlieger", (0, 5.5, 0))
    for i in range(4):
        y = 5.5 - i * 3
        m.cube("staart", "touw", (-0.2, y - 3, -0.2), (0.4, 3, 0.4))
        m.cube("staart", "strik", (-1.5, y - 2.5, -0.5), (3, 1.2, 1))
    a = {
        "idle": anim(2.0, samen(draai("vlieger", 8, 2.0, "z"), draai("staart", 15, 1.0, "z"))),
        "vlieg": anim(1.2, samen(draai("vlieger", 12, 1.2, "z"), draai("staart", 25, 0.6, "z"), bob("root", 1.2, 1.5))),
        "aanval": een(0.5, {"staart": {"rotation": kf([(0, (0, 0, 0)), (0.2, (0, 0, 80)), (0.35, (0, 0, -60)), (0.5, (0, 0, 0))])}}),
        "speciaal": een(0.7, {"vlieger": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-40, 0, 0)), (0.7, (0, 0, 0))])}}),
        "blij": anim(1.0, samen(rond("vlieger", 1.0), bob("root", 1.0, 3))),
    }
    pal = {"doek": ((120, 200, 255), "strepen", (255, 150, 200)), "doek2": ((180, 130, 90), None, None), "gezicht": ((255, 236, 200), None, None),
           "oor": ((255, 160, 200), None, None), "touw": ((240, 236, 220), None, None), "strik": ((255, 120, 170), None, None)}
    return m, a, pal, {}


def m_pruilrob():
    m = Model("pruilrob", px=4, breedte=96, bounds=(2, 1.5, 0.4))
    m.bone("root")
    m.bone("lijf", "root", (0, 4, 0))
    m.cube("lijf", "vacht", (-4.5, 0, -6), (9, 7.5, 13), gezicht=None)
    m.cube("lijf", "buik", (-3.5, 0, -6.3), (7, 4.5, 4))
    m.bone("kop", "lijf", (0, 6, -6))
    m.cube("kop", "vacht", (-3.5, 4, -11), (7, 6.5, 6), gezicht={"mood": "blij", "ey": 0.4})
    m.cube("kop", "snuit", (-2, 4.4, -12), (4, 2.4, 1.4))
    for sx in (-1, 1):
        m.cube("kop", "snor", (sx * 2.4 - 0.8, 5.2, -12.2), (1.6, 0.4, 0.4))
    for sx, kant in ((-1, "l"), (1, "r")):
        b = "vin_" + kant
        m.bone(b, "lijf", (sx * 4.5, 2, -3))
        m.cube(b, "vin", (sx * 4.5 + (0 if sx > 0 else -4), 0, -4.5), (4, 1, 3), rot=(0, 0, -20 * sx), pivot=(sx * 4.5, 1, -3))
    m.bone("staart", "lijf", (0, 2, 7))
    m.cube("staart", "vin", (-3, 0.5, 7), (6, 1, 3.5))
    a = {
        "idle": anim(2.4, samen(adem("lijf", 2.4, 1.03), draai("kop", 8, 2.4, "y"), draai("staart", 10, 2.4, "y"))),
        "loop": anim(0.6, samen(hup("lijf", 0.6, 1.0), draai("vin_l", 25, 0.6, "y"), draai("vin_r", -25, 0.6, "y"))),
        "glij": anim(0.4, samen({"lijf": {"rotation": kf([(0, (8, 0, 0))])}, "vin_l": {"rotation": kf([(0, (0, -50, 0))])},
                                 "vin_r": {"rotation": kf([(0, (0, 50, 0))])}}, draai("staart", 20, 0.4, "y"))),
        "speciaal": een(1.0, samen(draai("vin_l", 50, 0.25, "z"), draai("vin_r", -50, 0.25, "z"))),
        "aanval": een(0.4, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-25, 0, 0)), (0.3, (10, 0, 0)), (0.4, (0, 0, 0))])}}),
        "blij": anim(0.6, samen(hup("root", 0.6, 2.5), draai("vin_l", 60, 0.3, "z"), draai("vin_r", -60, 0.3, "z"))),
    }
    pal = {"vacht": ((176, 196, 220), "plekjes", (196, 214, 236)), "buik": ((230, 236, 246), None, None), "snuit": ((240, 230, 236), None, None),
           "snor": ((90, 90, 100), None, None), "vin": ((140, 160, 190), None, None), "oog": OOG}
    return m, a, pal, {}


def m_mistmopje():
    m = Model("mistmopje", px=4, breedte=96, bounds=(1.8, 1.8, 0.6))
    m.bone("root")
    m.bone("lijf", "root", (0, 8, 0))
    m.cube("lijf", "wolk", (-4.5, 4, -4), (9, 7, 8), gezicht={"mood": "blij", "ey": 0.45})
    m.cube("lijf", "wolk2", (-6.5, 5, -2.5), (4, 4.5, 5))
    m.cube("lijf", "wolk2", (2.5, 5.5, -2.5), (4, 4, 5))
    m.cube("lijf", "wolk2", (-2.5, 10, -2.5), (5, 3, 5))
    m.bone("pluim", "lijf", (0, 4, 2))
    for i, x in enumerate((-2, 0.5, 2.5)):
        m.cube("pluim", "wolk2", (x - 0.8, 1, 0.5 + i * 0.6), (1.6, 3, 1.6))
    a = {
        "idle": anim(2.0, samen(bob("root", 2.0, 1.2), adem("lijf", 2.0, 1.05))),
        "zweef": anim(2.0, samen(bob("root", 2.0, 1.0), draai("pluim", 20, 1.0, "x"))),
        "mist": anim(1.0, {"lijf": {"scale": kf([(0, (1.5, 0.6, 1.5))])}}),
        "speciaal": een(0.6, {"lijf": {"scale": kf([(0, (1, 1, 1)), (0.2, (1.4, 1.4, 1.4)), (0.6, (1, 1, 1))])}}),
        "aanval": een(0.4, {"lijf": {"rotation": kf([(0, (0, 0, 0)), (0.2, (25, 0, 0)), (0.4, (0, 0, 0))])}}),
        "blij": anim(1.0, samen(rond("root", 1.0), bob("lijf", 1.0, 2))),
    }
    pal = {"wolk": ((236, 230, 250), "plekjes", (250, 246, 255)), "wolk2": ((226, 220, 246), "plekjes", (244, 240, 255)), "oog": OOG}
    return m, a, pal, {}


def m_vliegerknecht():
    m, a = z2.mika("vliegerknecht", "houweel")
    m.cube("kop", "bril", (-3.6, 22.5, -5), (7.2, 1.6, 0.8))              # aviator goggles
    m.cube("lijf", "vlieger", (-3, 12, 2.5), (6, 6, 0.8), rot=(0, 0, 45), pivot=(0, 15, 2.9))   # a little kite on the back
    a["blij"] = anim(1.0, stil())
    pal = {"vacht": ((200, 140, 162), "plekjes", (184, 126, 148)), "snuit": ((230, 172, 188), None, None), "oor": ((236, 146, 166), None, None),
           "poot": ((236, 166, 182), None, None), "vest": ((60, 90, 130), "strepen", (230, 230, 240)), "broek": ((70, 64, 76), None, None),
           "laars": ((40, 34, 30), None, None), "helm": ((120, 84, 56), None, None), "lamp": ((236, 220, 120), None, None),
           "steel": ((96, 70, 48), None, None), "ijzer": ((150, 152, 164), None, None), "bril": ((80, 200, 230), None, None),
           "vlieger": ((90, 90, 100), "strepen", (40, 40, 46))}
    return m, a, pal, {"lamp": (120, 100, 30)}


WEZENS = [("mopperkrab", m_mopperkrab), ("koraalkobold", m_koraalkobold), ("stekelpluis", m_stekelpluis), ("zeurslak", m_zeurslak),
          ("lampionkwal", m_lampionkwal), ("mokmeeuw", m_mokmeeuw), ("bromvlieger", m_bromvlieger), ("pruilrob", m_pruilrob),
          ("mistmopje", m_mistmopje), ("vliegerknecht", m_vliegerknecht)]
MIKA = {"vliegerknecht"}
NODIG = {
    "mopperkrab": ["idle", "loop", "aanval", "schild", "blij"], "koraalkobold": ["idle", "loop", "aanval", "spuug", "blij"],
    "stekelpluis": ["idle", "rol", "puf", "blij"], "zeurslak": ["idle", "kruip", "aanval", "schelp", "blij"],
    "lampionkwal": ["idle", "zweef", "speciaal", "blij"], "mokmeeuw": ["idle", "vlieg", "zit", "aanval", "blij"],
    "bromvlieger": ["idle", "vlieg", "aanval", "speciaal", "blij"], "pruilrob": ["idle", "loop", "glij", "speciaal", "aanval", "blij"],
    "mistmopje": ["idle", "zweef", "mist", "speciaal", "blij"], "vliegerknecht": ["idle", "loop", "aanval", "spuug", "blij"],
}


# =====================================================================================================================
# the bosses, their objects and the pet
# =====================================================================================================================
def m_zeeslurf():
    m = Model("z4_baas_zeeslurf", px=2, breedte=256, bounds=(6, 5, 1.5))
    m.bone("root")
    m.bone("lijf", "root", (0, 22, 0))
    m.cube("lijf", "huid", (-24, 8, -20), (48, 28, 40))
    m.cube("lijf", "huid", (-20, 34, -16), (40, 6, 32))
    m.cube("lijf", "buik", (-20, 6, -18), (40, 4, 34))
    m.bone("kop", "lijf", (0, 24, -20))
    m.cube("kop", "huid", (-15, 12, -36), (30, 24, 17), gezicht={"mood": "blij", "ey": 0.42, "schaal": 1.0})
    for sx in (-1, 1):
        m.cube("kop", "oor", (sx * 10 - 3, 34, -28), (6, 6, 3))
        m.cube("kop", "binnenoor", (sx * 10 - 2, 35, -28.4), (4, 4, 0.6))
    m.bone("slurf", "kop", (0, 18, -36))
    m.cube("slurf", "huid", (-4, 14, -46), (8, 8, 10))
    m.bone("slurf2", "slurf", (0, 17, -46))
    m.cube("slurf2", "huid", (-3, 13, -54), (6, 6, 8))
    m.cube("slurf2", "snuit", (-3.5, 12.5, -56), (7, 7, 2))
    for sx, kant in ((-1, "l"), (1, "r")):
        b = "vin_" + kant
        m.bone(b, "lijf", (sx * 24, 16, -6))
        m.cube(b, "vin", (sx * 24 + (0 if sx > 0 else -16), 14, -14), (16, 4, 14))
    m.bone("staart", "lijf", (0, 20, 20))
    m.cube("staart", "huid", (-8, 14, 20), (16, 12, 10))
    m.cube("staart", "vin", (-18, 18, 28), (36, 4, 12))
    m.bone("sluier", "lijf", (0, 8, 0))
    for i in range(10):
        hoek = i * 36
        b = f"franje_{i}"
        x, z = int(round(np.cos(np.radians(hoek)) * 18)), int(round(np.sin(np.radians(hoek)) * 15))
        m.bone(b, "sluier", (x, 8, z))
        m.cube(b, "franje", (x - 2, -4, z - 2), (4, 12, 4))
        m.cube(b, "lamp", (x - 2.5, -8, z - 2.5), (5, 5, 5))
    zw = samen(*[draai(f"franje_{i}", 10 if i % 2 else -10, 2.0, "x") for i in range(10)])
    a = {
        "idle": anim(3.0, samen(bob("root", 3.0, 2), adem("lijf", 3.0, 1.02), zw, draai("slurf", 8, 3.0, "x"), draai("vin_l", 10, 3.0, "z"),
                                draai("vin_r", -10, 3.0, "z"))),
        "walk": anim(2.0, samen(bob("root", 2.0, 2), zw, draai("staart", 15, 2.0, "x"), draai("vin_l", 25, 1.0, "z"), draai("vin_r", -25, 1.0, "z"))),
        "spuit": een(1.0, {"slurf": {"rotation": kf([(0, (0, 0, 0)), (0.4, (50, 0, 0)), (0.6, (-20, 0, 0)), (1.0, (0, 0, 0))])},
                           "slurf2": {"rotation": kf([(0, (0, 0, 0)), (0.4, (30, 0, 0)), (1.0, (0, 0, 0))])}}),
        "wolk": een(1.0, samen(draai("lijf", 6, 0.25, "z"), {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.5, (15, 0, 0)), (1.0, (0, 0, 0))])}})),
        "duik": een(1.2, {"lijf": {"rotation": kf([(0, (0, 0, 0)), (0.6, (35, 0, 0)), (1.2, (0, 0, 0))])}}),
        "golf": een(1.2, {"staart": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-50, 0, 0)), (0.8, (40, 0, 0)), (1.2, (0, 0, 0))])}}),
        "giechel": een(1.5, samen(draai("lijf", 8, 0.3, "z"), draai("kop", 12, 0.3, "y"), draai("slurf", 25, 0.3, "x"))),
        # the pet (vr_huisdier_zeeslurfje uses these on the same bones)
        "hop": anim(0.6, samen(hup("root", 0.6, 6), draai("vin_l", 25, 0.6, "z"), draai("vin_r", -25, 0.6, "z"))),
        "sprong": anim(1.0, samen(draai("vin_l", 40, 0.5, "z"), draai("vin_r", -40, 0.5, "z"), draai("slurf", 20, 1.0, "x"))),
    }
    pal = {"huid": ((176, 210, 240), "plekjes", (200, 226, 250)), "buik": ((236, 240, 252), None, None), "oor": ((176, 210, 240), None, None),
           "binnenoor": ((255, 170, 200), None, None), "snuit": ((255, 190, 210), None, None), "vin": ((150, 190, 230), None, None),
           "franje": ((255, 190, 225), "glans", None), "lamp": ((255, 220, 130), None, None)}
    return m, a, pal


def m_vliegervanger():
    m, a = z2.mika("z4_baas_vliegervanger", "houweel")
    m.bounds = (5, 6, 2.5)                                                  # the war kite reaches 4 blocks up
    m.cube("kop", "hoed", (-6, 27, -6), (12, 1, 11))                       # a captain's hat
    m.cube("kop", "hoed", (-3.5, 28, -3.5), (7, 3.5, 6))
    m.cube("kop", "veer", (2, 31, -1), (1, 4, 1), rot=(0, 0, -20), pivot=(2.5, 31, -0.5))
    m.cube("kop", "bril", (-3.6, 22.5, -5), (7.2, 1.6, 0.8))
    m.cube("lijf", "cape", (-4.2, 6, 2.3), (8.4, 12, 0.8))
    m.bone("vlieger", "root", (0, 30, 2))
    m.cube("vlieger", "doek", (-13, 33, 2), (26, 26, 1.2), rot=(0, 0, 45), pivot=(0, 46, 2.6))
    m.cube("vlieger", "lat", (-17, 45.5, 1.6), (34, 1, 0.8))
    m.cube("vlieger", "lat", (-0.5, 28, 1.6), (1, 36, 0.8))
    m.cube("vlieger", "schedel", (-5, 42, 1.2), (10, 8, 0.6))           # a grim Mika face on the war kite
    for sx in (-1, 1):
        m.cube("vlieger", "doek", (sx * 9 - 2, 60, 2), (4, 4, 1))
        m.cube("vlieger", "touw", (sx * 4 - 0.2, 20, 2), (0.4, 14, 0.4), rot=(0, 0, 12 * sx), pivot=(sx * 4, 20, 2))
    a["walk"] = a.pop("loop")
    a["net"] = een(0.8, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-150, 0, 0)), (0.5, (-20, 0, 0)), (0.8, (0, 0, 0))])},
                         "arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-120, 0, 0)), (0.8, (0, 0, 0))])}})
    a["gooi"] = een(0.6, {"arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.25, (-170, 0, 0)), (0.4, (-30, 0, 0)), (0.6, (0, 0, 0))])}})
    a["opstijg"] = een(1.0, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-175, 0, 0)), (1.0, (-175, 0, 0))])},
                             "arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-175, 0, 0)), (1.0, (-175, 0, 0))])}})
    a["val"] = een(1.0, {"lijf": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-30, 0, 20)), (1.0, (0, 0, 0))])}})
    a["fluit"] = een(0.8, {"arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-140, 30, 0)), (0.6, (-140, 30, 0)), (0.8, (0, 0, 0))])}})
    pal = {"vacht": ((190, 130, 150), "plekjes", (172, 116, 136)), "snuit": ((226, 168, 184), None, None), "oor": ((236, 146, 166), None, None),
           "poot": ((236, 166, 182), None, None), "vest": ((110, 40, 46), "strepen", (230, 196, 120)), "broek": ((50, 46, 60), None, None),
           "laars": ((40, 30, 26), None, None), "helm": ((40, 36, 46), None, None), "hoed": ((36, 32, 44), "strepen", (230, 196, 90)),
           "veer": ((255, 214, 90), None, None), "lamp": ((236, 220, 120), None, None), "steel": ((96, 70, 48), None, None),
           "ijzer": ((150, 152, 164), None, None), "bril": ((80, 200, 230), None, None), "cape": ((80, 30, 40), None, None),
           "doek": ((90, 88, 100), "strepen", (60, 58, 70)), "lat": ((110, 80, 56), None, None), "schedel": ((230, 226, 214), None, None),
           "touw": ((220, 214, 196), None, None)}
    return m, a, pal


def m_poliep():
    m = Model("z4_baas_poliep", px=4, breedte=96, bounds=(2, 2, 0.6))
    m.bone("root")
    m.cube("root", "steen", (-6, 0, -6), (12, 3, 12))
    m.bone("steel", "root", (0, 3, 0))
    m.cube("steel", "poliep", (-3, 3, -3), (6, 12, 6), gezicht={"mood": "blij", "ey": 0.35})
    m.bone("kroon", "steel", (0, 15, 0))
    for i in range(8):
        m.cube("kroon", "tentakel", (-0.7, 15, -0.7), (1.4, 6, 1.4), rot=(35, i * 45, 0), pivot=(0, 15, 0))
    a = {"idle": anim(2.0, samen(draai("steel", 6, 2.0, "z"), {"kroon": {"rotation": kf([(0, (0, 0, 0)), (2.0, (0, 45, 0))])}}))}
    pal = {"steen": ((200, 140, 150), None, None), "poliep": ((150, 140, 160), "plekjes", (120, 112, 130)),
           "tentakel": ((176, 168, 186), None, None)}
    return m, a, pal


def m_lier():
    m = Model("z4_baas_lier", px=4, breedte=96, bounds=(2, 2, 0.6))
    m.bone("root")
    m.cube("root", "hout", (-7, 0, -4), (14, 2, 8))
    for sx in (-1, 1):
        m.cube("root", "hout", (sx * 6 - 1, 2, -1.5), (2, 9, 3))
    m.bone("trommel", "root", (0, 8, 0))
    m.cube("trommel", "ijzer", (-5, 6, -2), (10, 4, 4))
    for i in range(3):
        m.bone(f"touw_{i + 1}", "trommel", (0, 8, 0))
        m.cube(f"touw_{i + 1}", "touw", (-4 + i * 3, 5.6, -2.4), (2.4, 4.8, 4.8))
    m.bone("slinger", "root", (7, 8, 0))
    m.cube("slinger", "ijzer", (7, 7.5, -0.5), (1, 1, 1))
    m.cube("slinger", "ijzer", (8, 7.5, -0.5), (1, 5, 1))
    m.cube("slinger", "greep", (8, 12, -0.5), (3, 1, 1))
    m.bone("vlag", "root", (0, 11, 0))
    m.cube("vlag", "lint", (-1, 11, -0.5), (2, 3, 1))   # a purple marker flag
    a = {"idle": anim(2.0, draai("vlag", 10, 2.0, "z")),
         "pomp": een(0.5, {"slinger": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-360, 0, 0))])},
                           "trommel": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-120, 0, 0))])}})}
    pal = {"hout": ((150, 104, 70), "strepen", (130, 88, 58)), "ijzer": ((140, 140, 150), None, None), "touw": ((226, 214, 186), "strepen", (200, 186, 156)),
           "greep": ((200, 60, 60), None, None), "lint": ((176, 80, 255), None, None)}
    return m, a, pal


def m_vliegertje():
    m = Model("vr_huisdier_vliegertje", px=4, breedte=64, bounds=(1, 1.5, 0.6))
    m.bone("root")
    m.bone("vlieger", "root", (0, 8, 0))
    m.cube("vlieger", "doek", (-4, 4, -0.5), (8, 8, 1), rot=(0, 0, 45), pivot=(0, 8, 0))
    m.cube("vlieger", "gezicht", (-2.5, 5.5, -0.9), (5, 4, 0.5), gezicht={"mood": "blij", "ey": 0.4})
    for sx in (-1, 1):
        m.cube("vlieger", "oor", (sx * 2 - 1, 13, -0.5), (2, 2, 1))
    m.bone("staart", "vlieger", (0, 2.5, 0))
    for i in range(3):
        m.cube("staart", "strik", (-1, 1 - i * 2, -0.4), (2, 0.8, 0.8))
    a = {"idle": anim(2.0, samen(bob("root", 2.0, 1.0), draai("vlieger", 10, 2.0, "z"), draai("staart", 20, 1.0, "z"))),
         "hop": anim(0.6, samen(hup("root", 0.6, 3), draai("staart", 30, 0.6, "z"))),
         "sprong": anim(1.0, samen(rond("vlieger", 1.0), bob("root", 1.0, 2)))}
    pal = {"doek": ((255, 160, 210), "strepen", (255, 220, 120)), "gezicht": ((255, 240, 230), None, None), "oor": ((255, 130, 190), None, None),
           "strik": ((140, 220, 255), None, None)}
    return m, a, pal


# =====================================================================================================================
# gear
# =====================================================================================================================
HARNAS = {
    # id: (slot, name NL, name EN, colour, accent, set)
    "schelpenhelm": ("hoofd", "Schelpenhelm", "Shell Helm", (250, 214, 200), (120, 220, 210), "schelpen"),
    "schelpenpantser": ("borst", "Schelpenpantser", "Shell Armour", (246, 200, 186), (120, 220, 210), "schelpen"),
    "schelpenbroek": ("benen", "Schelpenbroek", "Shell Trousers", (236, 190, 176), (255, 150, 170), "schelpen"),
    "schelpenlaarzen": ("voeten", "Schelpenlaarzen", "Shell Boots", (226, 180, 166), (255, 150, 170), "schelpen"),
    "lampionmuts": ("hoofd", "Lampionmuts", "Lantern Hat", (255, 160, 200), (255, 220, 120), "lampion"),
    "lampionjurk": ("borst", "Lampionjurk", "Lantern Robe", (250, 150, 196), (255, 220, 120), "lampion"),
    "lampionbroek": ("benen", "Lampionbroek", "Lantern Trousers", (236, 140, 186), (160, 236, 200), "lampion"),
    "lampionslofjes": ("voeten", "Lampionslofjes", "Lantern Slippers", (255, 176, 210), (160, 236, 200), "lampion"),
}
NIEUW = {
    # t-items' new slots (Java registers them only when the slots exist): id: (kind, name NL, name EN, colour, accent)
    "schelpenwanten": ("handschoenen", "Schelpenwanten", "Shell Mittens", (246, 200, 186), (120, 220, 210)),
    "lampioncape": ("cape", "Lampioncape", "Lantern Cape", (255, 150, 200), (255, 220, 120)),
    "koraalwantjes": ("handschoenen", "Koraalwantjes", "Coral Mittens", (255, 140, 160), (255, 220, 230)),
    "vliegercape": ("cape", "Vliegercape", "Kite Cape", (130, 200, 255), (255, 150, 200)),
    "zeeglasarmband": ("armband", "Zeeglasarmband", "Sea Glass Bracelet", (122, 216, 200), (240, 250, 255)),
}
FLAVOUR_NIEUW = {
    "schelpenwanten": ["Knip-knap, net als een krab. Maar dan lief.", "Je handen ruiken naar zee.", "Hoort bij de Schelpenset."],
    "lampioncape": ["Wappert als een lampionsnoer in de wind.", "In het donker gloeit de zoom.", "Hoort bij de Lampionset."],
    "koraalwantjes": ["Roze, warm en een beetje prikkelig.", "Gebreid door een koraalkobold. Hij had hulp.", "Vissen vinden ze mooi."],
    "vliegercape": ["Bij elke windvlaag voel je je even een vlieger.", "Er zit nog een vliegertouwtje aan.", "Fladder, fladder."],
    "zeeglasarmband": ["Elk glaasje is rondgeslepen door de golven.", "Rinkelt zacht als je slaat.", "Voor snelle handen."],
}


def nieuw_icoon(soort, kleur, accent, seed):
    """16x16: mittens, a cape or a bracelet (the t-items kinds)."""
    d = Doek()
    k, a = tuple(kleur), tuple(accent)
    if soort == "handschoenen":
        for dx in (0, 7):
            d.rect(2 + dx, 5, 6 + dx, 13, k)
            d.rect(1 + dx, 7, 1 + dx, 10, k)
            d.rect(2 + dx, 12, 6 + dx, 13, a)
    elif soort == "cape":
        d.poly([(4, 2), (12, 2), (14, 14), (2, 14)], k)
        d.rect(4, 2, 12, 3, a)
        d.lijn(8, 4, 8, 13, _licht(k, 0.85))
    else:
        for i in range(10):
            import math as _m
            h = i / 10 * 2 * _m.pi
            d.disc(8 + _m.cos(h) * 4.5, 8 + _m.sin(h) * 3, 1.3, k if i % 2 else a)
    d.schaduw()
    return d.rand()


SIERADEN = {
    "parelring": ("ring", "Parelring", "Pearl Ring", (230, 220, 236), (255, 250, 255)),
    "schelpenketting": ("ketting", "Schelpenketting", "Shell Necklace", (250, 200, 180), (255, 140, 160)),
    "lampionoorbel": ("oorbel", "Lampionoorbel", "Lantern Earring", (255, 190, 110), (255, 120, 190)),
}


def _ankerknots():
    d = Doek()
    d.lijn(3, 14, 9, 8, (150, 110, 80), dik=2)
    d.lijn(9, 8, 13, 2, (130, 140, 160), dik=2)
    d.lijn(10, 2, 15, 6, (130, 140, 160))
    d.lijn(9, 4, 13, 1, (170, 180, 200))
    d.disc(12, 3, 1.4, (255, 150, 190))
    d.schaduw()
    return d.rand()


def _lampiontop(d):
    d.rect(9, 1, 14, 2, (200, 60, 90))
    d.disc(11.5, 5, 3, (255, 140, 190))
    d.disc(11.5, 5, 1.5, (255, 230, 150))
    d.rect(10, 8, 13, 9, (200, 60, 90))


def _koraaltop(d):
    for x, y in ((10, 6), (12, 3), (14, 5), (11, 1), (13, 1)):
        d.lijn(9, 8, x, y, (255, 120, 150))
    d.disc(12, 4, 1, (255, 200, 210))


WAPENS = {
    "ankerknots": ("Ankerknots", "Anchor Club", _ankerknots, ["Ooit hield hij een vissersbootje op zijn plek. Nu jou.", "Zwaar. Zout. Zalig.",
                                                             "Er zit nog een beetje zeewier aan. Laat maar zitten."]),
    "golfbreker": ("Golfbreker", "Wavebreaker", lambda: zwaard((150, 220, 240)), ["Hij snijdt door golven, ellende en grauwe buien.",
                                                                                  "Glinstert als de zee om vijf uur 's middags.", "Ruikt naar zout en avontuur."]),
    "vliegerboog": ("Vliegerboog", "Kite Bow", lambda: boog((240, 160, 200), (140, 220, 255)), ["De pees is een oud vliegertouw. Hij zingt in de wind.",
                                                                                                "Schiet een beetje met de wind mee. Handig!", "Er hangt nog een strikje aan."]),
    "harpoenkruisboog": ("Harpoenkruisboog", "Harpoon Crossbow", lambda: kruisboog((120, 100, 80), (130, 220, 240)),
                         ["Voor grote vissen en grauwe wezens.", "Tsjoeng! En dan: plons.", "Gemaakt in de werkplaats van Lampionhaven."]),
    "lampionstaf": ("Lampionstaf", "Lantern Staff", lambda: staf(_lampiontop), ["Er brandt altijd een lampionnetje in. Ook 's nachts. Vooral 's nachts.",
                                                                               "Langzaam, warm en heel erg roze.", "Het feest begint waar jij staat."]),
    "koraalstaf": ("Koraalstaf", "Coral Staff", lambda: staf(_koraaltop), ["Gegroeid in de roze lagune.", "Je heals ruiken naar zee.",
                                                                           "Er woont een klein visje in. Hij helpt mee."]),
}
KRISTALLEN = {
    "parelkristal": ("Parelkristal", "Pearl Crystal", 0xF4E8F0, "Rond, zacht en glanzend. Je wordt er wat steviger van: meer Vads."),
    "zeeglas": ("Zeeglas", "Sea Glass", 0x7AD8C8, "Een scherfje zee, rondgeslepen door de golven. Meer Elementweerstand."),
}
BAAS_ITEMS = {
    # id: (name NL, name EN, flavour, effect, icon)
    "slurfstaf": ("Slurfstaf", "Trunk Staff", "\"Proest! Een beetje zout water hoort erbij.\"", "+15 % schade op wezens in het water",
                  lambda: staf(lambda d: (d.disc(11.5, 4, 3, (176, 210, 240)), d.disc(11.5, 4, 1.4, (255, 190, 210))))),
    "zeekoeienhuid": ("Zeekoeienhuid", "Sea Cow Hide", "\"Dik, zacht en een beetje glibberig.\"", "in het water krijg je 15 % minder schade",
                      lambda: vw.icoon("borst", (176, 210, 240), (255, 190, 225), None, 4801)),
    "lagunelampion": ("Lagunelampion", "Lagoon Lantern", "\"Het lichtje dat de Zeeslurf weer liet zingen.\"", "je geeft zelf licht in het donker",
                      lambda: z2.sieraad_icoon("oorbel", (255, 210, 130), (255, 140, 200), 4802)),
    "oorlogsvliegerboog": ("Oorlogsvliegerboog", "War Kite Bow", "\"Kootje mist hem nog elke dag.\"", "+20 % schade op vliegende wezens",
                           lambda: boog((90, 88, 100), (230, 196, 120))),
    "enterhaak": ("Kootjes Enterhaak", "Kootje's Grappling Hook", "\"Hij pakte er knuffels mee. Jij pakt er Mika's mee.\"",
                  "+15 % schade op wezens die Plakkerig zijn", _ankerknots),
    "kaasdoek": ("Kaasdoek van de Vliegervanger", "The Kite Catcher's Cheesecloth", "\"Hij ruikt nog naar kootjeskaas. Heel erg.\"",
                 "onder 30 % Vads loop je 3 s sneller (30 s afkoelen)", lambda: vw.icoon("hoofd", (80, 30, 40), (230, 196, 90), None, 4803)),
}
BAZEN = {
    # id: (name NL, name EN, title, subtitle)
    "zeeslurf": ("De Grauwe Zeeslurf", "The Grey Sea-Trunk", "De Grauwe Zeeslurf", "Hij mokt al jaren op de bodem van de lagune"),
    "vliegervanger": ("Kootje de Vliegervanger", "Kootje the Kite Catcher", "Kootje de Vliegervanger", "Hij steelt knuffels. Met een vlieger."),
}
DOELEN = {"poliep": ("Grauwe poliep", "Grey Polyp"), "lier": ("Lier", "Winch")}
HUISDIEREN = {
    "zeeslurfje": ("Zeeslurfje", "Een piepklein roze zeekoetje met een slurfje. Proest bubbeltjes als hij blij is.", "Vrolijk De Grauwe Zeeslurf op in de Koraalbaai"),
    "vliegertje": ("Vliegertje", "De oorlogsvlieger van Kootje, nu roze en lief. Wappert achter je aan.", "Versla Kootje in het Wrak van de Vliegervanger"),
}
FASES = {"zeeslurf": {2: "Het grotje!", 3: "Golfslag!"}, "vliegervanger": {2: "Opstijgen!", 3: "Knuffelregen!"}}

ENTITEITEN = {
    "mopperkrab": ("Grauwe Mopperkrab", "Grey Grumpy Crab"), "koraalkobold": ("Koraalkobold", "Coral Imp"),
    "stekelpluis": ("Stekelpluis", "Spiky Puff"), "zeurslak": ("Zeurslak", "Whiny Sea-Snail"), "lampionkwal": ("Grauwe Lampionkwal", "Grey Lantern Jelly"),
    "mokmeeuw": ("Mokmeeuw", "Sulky Gull"), "bromvlieger": ("Bromvlieger", "Grumpy Kite"), "pruilrob": ("Pruilrob", "Pouting Seal"),
    "mistmopje": ("Mistmopje", "Mist Mope"), "vliegerknecht": ("Vliegerknecht", "Kite Deckhand"),
}
FLAVOUR = {
    "schelpenhelm": ["Je hoort de zee als je hem opzet.", "Een heremietkreeft woonde er eerst in. Hij vond het goed.", "Glimt roze in de zon."],
    "schelpenpantser": ["Hard van buiten, parelmoer van binnen.", "Elke schelp is een cadeautje van de golven.", "Rammelt een beetje. Gezellig."],
    "schelpenbroek": ["Met een zakje vol zand. Dat gaat er nooit meer uit.", "Waterdicht. Bijna.", "Perfect voor pootjebaden en vechten."],
    "schelpenlaarzen": ["Kraken zacht op het strand.", "Krabben vinden ze heel interessant.", "Voor wie stevig wil staan in de branding."],
    "lampionmuts": ["Er brandt een klein lampionnetje bovenop.", "Nachtvlinders vinden je nu erg leuk.", "Gevouwen van feestpapier en goede zin."],
    "lampionjurk": ["Zacht als lampionzijde, warm als een feestavond.", "Zwiert als je draait. Probeer maar.", "Genaaid in de lampionwerkplaats."],
    "lampionbroek": ["Met lampionnetjes langs de naad.", "Voor lange avonden op de kade.", "Lichtgevend als je lacht. Echt."],
    "lampionslofjes": ["Je loopt op een wolkje feest.", "Klinken als belletjes op de steiger.", "Ideaal voor een dansje bij het kampvuur."],
    "parelring": ["Uit de diepste schelp van de lagune.", "Rond, glad en een beetje magisch.", "De Zeeslurf gaf hem misschien zelf."],
    "schelpenketting": ["Rammelt vrolijk als je loopt.", "Zelf geraapt op het Knuffeltij-strand.", "Elke schelp een herinnering."],
    "lampionoorbel": ["Brandt zacht naast je oor.", "Een lampionnetje voor in het donker.", "Het feest gaat met je mee."],
}

VFX_AANVALLEN = {"golfslag": ("Golfslag", "Wave Slap"), "samen_aaien": ("Samen aaien!", "Pet Together!"),
                 "knuffelregen": ("Knuffelregen", "Plushie Rain"), "windvlaag": ("Windvlaag", "Gust")}

TEKSTEN = {
    # creatures
    "vadsrijk.z4wezens.scharen": "Scharen dicht!",
    "vadsrijk.z4wezens.verstopt": "Verstopt!",
    "vadsrijk.z4wezens.mika.weg.0": "Dit komt in mijn rapport!",
    "vadsrijk.z4wezens.mika.weg.1": "Terug naar het wrak! TERUG!",
    "vadsrijk.z4wezens.mika.weg.2": "Kootje hoort hiervan!",
    "vadsrijk.z4wezens.mika.weg.3": "Mijn vlieger! Wacht op mij!",
    "vadsrijk.z4wezens.set.schelpen": "Schelpenset",
    "vadsrijk.z4wezens.set.lampion": "Lampionset",
    "vadsrijk.z4wezens.set.stukken": "Set: %s (%s/4 gedragen; de set-wanten of -cape tellen mee)",
    "vadsrijk.z4wezens.set.schelpen.2": "(2) Schelpenhuid: +12 Verdediging",
    "vadsrijk.z4wezens.set.schelpen.4": "(4) Getijdenkracht: Schade tegen bazen +8%%",
    "vadsrijk.z4wezens.set.lampion.2": "(2) Lampionglans: +8 Wijsheid",
    "vadsrijk.z4wezens.set.lampion.4": "(4) Feestlicht: +10 Heal-kracht, en je geeft zelf licht in het donker",
    "vadsrijk.z4wezens.tip.mopperkrab.titel": "Grauwe Mopperkrab",
    "vadsrijk.z4wezens.tip.mopperkrab": "Houdt hij zijn scharen voor zijn gezicht? Dan doen klappen van voren maar de helft. Loop om hem heen!",
    "vadsrijk.z4wezens.tip.koraalkobold.titel": "Koraalkobold",
    "vadsrijk.z4wezens.tip.koraalkobold": "Hij gooit koraalscherfjes van een afstand. Ren erheen: van dichtbij is hij een makkie.",
    "vadsrijk.z4wezens.tip.stekelpluis.titel": "Stekelpluis",
    "vadsrijk.z4wezens.tip.stekelpluis": "Hij doet niks tot jij iets doet. Rode cirkel = zijn stekels: stap even weg.",
    "vadsrijk.z4wezens.tip.zeurslak.titel": "Zeurslak",
    "vadsrijk.z4wezens.tip.zeurslak": "Zijn slijmspoor maakt je Plakkerig. Kruipt hij in zijn huisje? Wacht even, dan komt hij weer naar buiten.",
    "vadsrijk.z4wezens.tip.lampionkwal.titel": "Grauwe Lampionkwal",
    "vadsrijk.z4wezens.tip.lampionkwal": "Rode cirkel onder de kwal = zijn tentakels prikken. Stap eruit en sla daarna terug.",
    "vadsrijk.z4wezens.tip.mokmeeuw.titel": "Mokmeeuw",
    "vadsrijk.z4wezens.tip.mokmeeuw": "Rode streep = zijn duikvlucht. Opzij! Daarna zit hij even op de grond: dat is je kans.",
    "vadsrijk.z4wezens.tip.bromvlieger.titel": "Bromvlieger",
    "vadsrijk.z4wezens.tip.bromvlieger": "Paarse streep = een windstoot die je wegblaast. Komt hij laag, dan zwiept zijn staart.",
    "vadsrijk.z4wezens.tip.pruilrob.titel": "Pruilrob",
    "vadsrijk.z4wezens.tip.pruilrob": "Trommelt hij met zijn flippers? Dan glijdt hij op zijn buik langs de rode streep. Stap opzij!",
    "vadsrijk.z4wezens.tip.mistmopje.titel": "Mistmopje",
    "vadsrijk.z4wezens.tip.mistmopje": "Hij smelt weg in de mist en duikt achter je op. Zie je de rode cirkel? Weg ermee, anders wordt het even donker.",
    "vadsrijk.z4wezens.tip.vliegerknecht.titel": "Vliegerknecht",
    "vadsrijk.z4wezens.tip.vliegerknecht": "Een Mika met een enterhaak. Rode streep = zijn haak: stap opzij, anders word je Plakkerig.",
    # bosses
    "vadsrijk.z4bazen.tip.zeeslurf.titel": "De Grauwe Zeeslurf",
    "vadsrijk.z4bazen.tip.zeeslurf": "Rood = zijn slurf of een mokwolk. In fase 2 duikt hij zijn grotje in: duik mee en breek de 3 grauwe poliepen. Roze bubbelringen geven lucht!",
    "vadsrijk.z4bazen.tip.vliegervanger.titel": "Kootje de Vliegervanger",
    "vadsrijk.z4bazen.tip.vliegervanger": "Hangt hij onder zijn vlieger? Rechtsklik de 3 lieren op het dek (elk 3x) om hem neer te halen. Geel = samen een knuffel vangen!",
    "vadsrijk.z3bazen.tip.zeeslurf.titel": "De Grauwe Zeeslurf",
    "vadsrijk.z3bazen.tip.zeeslurf": "Je verslaat hem niet: je vrolijkt hem op. Volg de kleuren: rood = weg, roze = veilig, paars = de stroming, geel = samen.",
    "vadsrijk.z3bazen.tip.vliegervanger.titel": "Het Wrak van de Vliegervanger",
    "vadsrijk.z3bazen.tip.vliegervanger": "Een Mika met een gestolen knuffelvoorraad. Rood = weg, paars = wind en lieren, geel = samen vangen.",
    "vadsrijk.z4bazen.zeeslurf.immuun": "Hij zit diep in zijn grotje! Breek eerst de grauwe poliepen.",
    "vadsrijk.z4bazen.zeeslurf.fase2": "HET GROTJE",
    "vadsrijk.z4bazen.zeeslurf.fase2.onder": "Duik mee en breek de grauwe poliepen!",
    "vadsrijk.z4bazen.zeeslurf.fase3": "GOLFSLAG",
    "vadsrijk.z4bazen.zeeslurf.fase3.onder": "Weg uit de rode ring, of duik eronderdoor!",
    "vadsrijk.z4bazen.zeeslurf.zeg.grotje": "Mmmmwoeeeh... laat me met rust... *bloeb*",
    "vadsrijk.z4bazen.zeeslurf.zeg.golf": "WOEEEH! *plets*",
    "vadsrijk.z4bazen.zeeslurf.duik": "De Zeeslurf zakt naar zijn grotje! Duik naar de 3 grauwe poliepen (roze ringen = lucht).",
    "vadsrijk.z4bazen.zeeslurf.poliep": "Poliep kapot! Nog %s over.",
    "vadsrijk.z4bazen.zeeslurf.adem": "Hij komt boven en snakt naar adem! Nu raken!",
    "vadsrijk.z4bazen.zeeslurf.lucht": "Lucht!",
    "vadsrijk.z4bazen.zeeslurf.golf": "Golfslag! Weg uit de rode ring!",
    "vadsrijk.z4bazen.zeeslurf.aaien": "Samen aaien! Ga met z'n allen in de gele cirkel staan!",
    "vadsrijk.z4bazen.zeeslurf.giechel": "Hihihi! Hij moet giechelen en vergeet even te zwemmen!",
    "vadsrijk.z4bazen.zeeslurf.blij": "De Zeeslurf is weer roze! Hij zingt een liedje voor de hele lagune.",
    "vadsrijk.z4bazen.vliegervanger.immuun": "Hij hangt te hoog! Draai de 3 lieren op het dek binnen (rechtsklik).",
    "vadsrijk.z4bazen.vliegervanger.fase2": "OPSTIJGEN!",
    "vadsrijk.z4bazen.vliegervanger.fase2.onder": "Draai de lieren binnen om zijn vlieger neer te halen!",
    "vadsrijk.z4bazen.vliegervanger.fase3": "KNUFFELREGEN",
    "vadsrijk.z4bazen.vliegervanger.fase3.onder": "Vang de gestolen knuffels samen op!",
    "vadsrijk.z4bazen.vliegervanger.zeg.op": "Ha! Vang me dan, landrotten! Mijn vlieger is sneller dan jullie!",
    "vadsrijk.z4bazen.vliegervanger.zeg.regen": "Nee! Mijn zak! Mijn knuffels!",
    "vadsrijk.z4bazen.vliegervanger.zeg.fluit": "*fluit* Knechten! Aan dek!",
    "vadsrijk.z4bazen.vliegervanger.zeg.weg": "Dit komt in mijn rapport! De Keizer hoort hiervan!",
    "vadsrijk.z4bazen.vliegervanger.lieren": "Hij hangt aan zijn vlieger! Rechtsklik de 3 lieren (paarse vlag) elk 3 keer.",
    "vadsrijk.z4bazen.vliegervanger.draai": "Lier: %s/%s",
    "vadsrijk.z4bazen.vliegervanger.lier": "Lier binnen! (%s/%s)",
    "vadsrijk.z4bazen.vliegervanger.neer": "KRAK! De vlieger stort neer! Kootje ligt even plat: nu raken!",
    "vadsrijk.z4bazen.vliegervanger.knuffel": "Er valt een knuffel uit zijn zak! Vang hem SAMEN in de gele cirkel!",
    "vadsrijk.z4bazen.vliegervanger.gevangen": "Knuffel gered! (%s/%s)",
    "vadsrijk.z4bazen.loot.guhddelijk": "%s vond iets Guhddelijks: %s!",
}


def teksten():
    """{key: (nl, en)}"""
    t = {k: (v, v) for k, v in TEKSTEN.items()}
    for k, (nl, en) in ENTITEITEN.items():
        t[f"entity.guhs.vr_z4_mob_{k}"] = (nl, en)
    for k, (slot, nl, en, kleur, accent, s) in HARNAS.items():
        t[f"item.guhs.vr_z4_mob_{k}"] = (nl, en)
        t[f"vadsrijk.loot.{k}"] = (nl, en)
    for k, (soort, nl, en, kleur, steen) in SIERADEN.items():
        t[f"item.guhs.vr_z4_mob_{k}"] = (nl, en)
        t[f"vadsrijk.loot.{k}"] = (nl, en)
    for k, regels in list(FLAVOUR.items()) + list(FLAVOUR_NIEUW.items()):
        for i, regel in enumerate(regels):
            t[f"vadsrijk.loot.{k}.flavour.{i}"] = ("\"" + regel + "\"",) * 2
    for k, (soort, nl, en, kleur, accent) in NIEUW.items():
        t[f"item.guhs.vr_z4_mob_{k}"] = (nl, en)
        t[f"vadsrijk.loot.{k}"] = (nl, en)
    for k, (nl, en) in VFX_AANVALLEN.items():
        t[f"vadsrijk.vfx.aanval.{k}"] = (nl, en)
    for k, (nl, en, _icoon, fl) in WAPENS.items():
        t[f"item.guhs.vr_z4_{k}"] = (nl, en)
        t[f"vadsrijk.loot.{k}"] = (nl, en)
        for i, regel in enumerate(fl):
            t[f"vadsrijk.loot.{k}.flavour.{i}"] = (regel, regel)
    for k, (nl, en, _kleur, fl) in KRISTALLEN.items():
        t[f"item.guhs.vr_z4_kristal_{k}"] = (nl, en)
        t[f"item.guhs.vr_z4_kristal_{k}.flavour"] = (fl, fl)
    for k, (nl, en, fl, effect, _i) in BAAS_ITEMS.items():
        t[f"item.guhs.vr_z4_baas_{k}"] = (nl, en)
        t[f"vadsrijk.loot.z4baas_{k}"] = (nl, en)
        t[f"vadsrijk.loot.z4baas_{k}.flavour.0"] = (fl, fl)
        t[f"vadsrijk.bazen.effect.z4baas_{k}"] = (effect, effect)
    for b, (nl, en, titel, onder) in BAZEN.items():
        t[f"entity.guhs.vr_z4_baas_{b}"] = (nl, en)
        t[f"vadsrijk.z4bazen.{b}"] = (titel, en)
        t[f"vadsrijk.z4bazen.{b}.onder"] = (onder, onder)
        t[f"item.guhs.vr_z4_baas_trofee_{b}"] = ("Trofee: " + nl, "Trophy: " + en)
        for f, regel in FASES.get(b, {}).items():
            t[f"vadsrijk.kern.beleving.fase.vr_z4_baas_{b}.{f}"] = (f"Fase {f}: {regel}",) * 2
    for k, (nl, en) in DOELEN.items():
        t[f"entity.guhs.vr_z4_baas_{k}"] = (nl, en)
    for h, (naam, tekst, bron) in HUISDIEREN.items():
        t[f"vadsrijk.huisdier.{h}.naam"] = (naam, naam)
        t[f"vadsrijk.huisdier.{h}.tekst"] = (tekst, tekst)
        t[f"vadsrijk.huisdier.{h}.bron"] = (bron, bron)
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


def _img(x):
    return x


def assets(a, w=vw._schrijf_json):
    geo = os.path.join(a, "geckolib", "models", "entity")
    ani = os.path.join(a, "geckolib", "animations", "entity")
    tex_dir = os.path.join(a, "textures", *SUB.split("/"))
    btex = os.path.join(a, "textures", *BSUB.split("/"))
    n = 0
    for i, (naam, maak) in enumerate(WEZENS):
        with _v2():
            m, anims, pal, glow = maak()
        anims = _naloop(m, z2.leg_anims(m, anims))
        controleer(naam, m, anims, NODIG[naam])
        w(os.path.join(geo, *SUB.split("/"), naam + ".geo.json"), m.geo())
        w(os.path.join(ani, *SUB.split("/"), naam + ".animation.json"), anim_bestand(anims))
        if naam in MIKA:
            tex = z2.schilder(m, pal, 4400 + i)
            vw._png(os.path.join(tex_dir, naam + ".png"), tex)
            vw._png(os.path.join(tex_dir, naam + "_gloed.png"), z2.schilder(m, pal, 4500 + i, glow, glow=True))
        else:
            tex = vw.schilder(m, pal, "blij", 4400 + i, True)            # ontvadst: grey and grumpy
            vw._png(os.path.join(tex_dir, naam + ".png"), tex)
            vw._png(os.path.join(tex_dir, naam + "_blij.png"), vw.schilder(m, pal, "blij", 4400 + i, False))
            g = {k: tuple(int(c * 0.45) for c in v) for k, v in glow.items()}   # the dead lantern glows only a little while grey
            vw._png(os.path.join(tex_dir, naam + "_gloed.png"), vw.gloed(m, g, 4500 + i))
        vw._png(os.path.join(tex_dir, naam + "_elite.png"), vw.elite_gloed(tex))
        n += 1
    # bosses + objects + pet
    with _v2():
        m, anims, pal = m_zeeslurf()
    controleer("zeeslurf", m, anims, ["idle", "walk", "spuit", "wolk", "duik", "golf", "giechel", "hop", "sprong"])
    w(os.path.join(geo, "vr_z4_baas_zeeslurf.geo.json"), m.geo())
    w(os.path.join(ani, "vr_z4_baas_zeeslurf.animation.json"), anim_bestand(anims))
    w(os.path.join(ani, "vr_huisdier_zeeslurfje.animation.json"), anim_bestand({"idle": anims["idle"], "hop": anims["hop"], "sprong": anims["sprong"]}))
    vw._png(os.path.join(btex, "zeeslurf.png"), vw.schilder(m, pal, "blij", 4600, True))
    vw._png(os.path.join(btex, "zeeslurf_blij.png"), vw.schilder(m, pal, "blij", 4600, False))
    vw._png(os.path.join(btex, "zeeslurf_glowmask.png"), _alpha(vw.gloed(m, {"lamp": (110, 96, 120)}, 4601)))
    vw._png(os.path.join(btex, "zeeslurf_blij_glowmask.png"), _alpha(vw.gloed(m, {"lamp": (255, 210, 120), "franje": (140, 60, 100)}, 4602)))
    with _v2():
        m, anims, pal = m_vliegervanger()
    controleer("vliegervanger", m, anims, ["idle", "walk", "net", "gooi", "opstijg", "val", "fluit"])
    w(os.path.join(geo, "vr_z4_baas_vliegervanger.geo.json"), m.geo())
    w(os.path.join(ani, "vr_z4_baas_vliegervanger.animation.json"), anim_bestand(anims))
    vw._png(os.path.join(btex, "vliegervanger.png"), z2.schilder(m, pal, 4610))
    for naam, maak, nodig in (("poliep", m_poliep, ["idle"]), ("lier", m_lier, ["idle", "pomp"]), ("vliegertje", m_vliegertje, ["idle", "hop", "sprong"])):
        with _v2():
            m, anims, pal = maak()
        controleer(naam, m, anims, nodig)
        mid = "vr_huisdier_vliegertje" if naam == "vliegertje" else "vr_z4_baas_" + naam
        w(os.path.join(geo, mid + ".geo.json"), m.geo())
        w(os.path.join(ani, mid + ".animation.json"), anim_bestand(anims))
        vw._png(os.path.join(btex, naam + ".png"), vw.schilder(m, pal, "blij", 4620 + len(naam), naam == "poliep"))
    # items
    for i, (k, (slot, nl, en, kleur, accent, s)) in enumerate(HARNAS.items()):
        _item(a, w, "vr_z4_mob_" + k, f"{SUB}/{k}", vw.icoon(slot, kleur, accent, None, 4700 + i))
    for i, (k, (soort, nl, en, kleur, steen)) in enumerate(SIERADEN.items()):
        _item(a, w, "vr_z4_mob_" + k, f"{SUB}/{k}", z2.sieraad_icoon(soort, kleur, steen, 4750 + i))
    for i, (k, (soort, nl, en, kleur, accent)) in enumerate(NIEUW.items()):
        _item(a, w, "vr_z4_mob_" + k, f"{SUB}/{k}", nieuw_icoon(soort, kleur, accent, 4760 + i))
    for k, (nl, en, icoon, _fl) in WAPENS.items():
        _item(a, w, "vr_z4_" + k, f"{SUB}/{k}", _img(icoon()), handheld=True)
    for i, (k, (nl, en, kleur, _fl)) in enumerate(KRISTALLEN.items()):
        _item(a, w, "vr_z4_kristal_" + k, f"{SUB}/kristal_{k}", _img(kristal(kleur, i)))
    for k, (nl, en, fl, effect, icoon) in BAAS_ITEMS.items():
        _item(a, w, "vr_z4_baas_" + k, f"{BSUB}/{k}", _img(icoon()), handheld=k in ("slurfstaf", "oorlogsvliegerboog", "enterhaak"))
    for i, (b, (nl, en, _t, _o)) in enumerate(BAZEN.items()):
        _item(a, w, "vr_z4_baas_trofee_" + b, f"{BSUB}/trofee_{b}", z3.trofee_icoon((255, 180, 210) if b == "zeeslurf" else (120, 110, 130), 4790 + i))
    return n


def _alpha(img):
    """AutoGlowing glowmasks: black = transparent (only the glowing parts are drawn)."""
    a = np.asarray(img).copy()
    zwart = a[..., :3].sum(axis=2) < 12
    a[zwart, 3] = 0
    return Image.fromarray(a)


def uiterlijken(a, w=vw._schrijf_json):
    """The worn look of every armour piece: vanilla iron tinted (needs the minecraft jar in build/, like zone 3)."""
    import zipfile
    from io import BytesIO
    jar = os.path.join("build", "moddev", "artifacts", "minecraft-patched-26.1.2.112.jar")
    bron = {}
    if os.path.exists(jar):
        with zipfile.ZipFile(jar) as z:
            for laag in ("humanoid", "humanoid_leggings"):
                bron[laag] = Image.open(BytesIO(z.read(f"assets/minecraft/textures/entity/equipment/{laag}/iron.png"))).convert("RGBA")
    for hid, (_slot, _nl, _en, kleur, _k2, _set) in HARNAS.items():
        uid = "vr_z4_mob_" + hid
        w(os.path.join(a, "equipment", uid + ".json"), {"layers": {"humanoid": [{"texture": "guhs:" + uid}], "humanoid_leggings": [{"texture": "guhs:" + uid}]}})
        for laag in ("humanoid", "humanoid_leggings"):
            doel = os.path.join(a, "textures", "entity", "equipment", laag, uid + ".png")
            if laag not in bron:
                if not os.path.exists(doel):
                    print("vadsrijk_t_z4wezens: no minecraft jar (run a gradle build first) for", doel)
                continue
            img = bron[laag].copy()
            px = img.load()
            for y in range(img.height):
                for x in range(img.width):
                    r, g, b, al = px[x, y]
                    l = min(1.0, (0.3 * r + 0.59 * g + 0.11 * b) / 255 * 1.15)
                    px[x, y] = (int(kleur[0] * l), int(kleur[1] * l), int(kleur[2] * l), al)
            os.makedirs(os.path.dirname(doel), exist_ok=True)
            img.save(doel)


def build(h):
    assets(h.A, h.w)
    uiterlijken(h.A, h.w)
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)   # (T-INT: make_v2.lang(key, en, nl))


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
    print(f"vadsrijk_t_z4wezens: {n} creature models, 5 boss/object/pet models, {len(t)} texts")


if __name__ == "__main__":
    _direct()
