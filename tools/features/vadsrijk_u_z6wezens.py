"""
1.10 "Het Vadsrijk", zone 6 "Het Zijdeland": the combat slice U-Z6WEZENS (ZONE6_PLAN §5-§9). Java: feature/vadsrijk/zone6/wezens
(+ zone6/bultguh).

Everything generated (no hand-made art), on the zone-1/2/4 helpers and t-models' kit v2 (vw.stijl(2) + naloop):
  * 14 creature models + animations (assets/guhs/geckolib/{models,animations}/entity/vadsrijk/zone6/wezens/):
      zandvlooi, fennekguh, springpiep, duinkruiper, dadelaapje, gierguh, spiegelslang, zoutkreeft, zandgeestje, ruinewachter,
      mika_lampendover, mika_ringdrager, gedoofd_sterretje, zoutslang (the cave elite)
    per creature <naam>.png (cheer-up ones: grey crusted and grumpy), <naam>_blij.png (bright, happy), _gloed, _elite.
  * the bosses (geckolib/.../entity/vr_z6_baas_*): wormelien, zoutkolos, astrolabiumwachter (bones ring_1..4), glimmertje,
    sterrenguh; objects zoutkorst, zingkristal, vuurschaal (bone vlam), telescoop (bone kijker); textures in
    textures/vadsrijk/zone6/bazen/ (+ _blij, + _glowmask for the AutoGlowing layer).
  * pets: bobbeltje (a baby Bultguh), wormelientje, zoutklontje, sterrenguhtje + huisdier.animation.json (root only).
  * the BULTGUH mount (geckolib entity/vr_eco_bultguh: humps, saddle, tassels, bells), its coats, pattern overlays, saddle icon,
    and the zone-6 tack item model vr_rijtuig_wormelien_zadeldeken.
  * gear icons (4 sets incl. gloves/cape/armband, 5 jewellery, 6 weapons, 2 crystals, 10 named boss items, 3 trophies,
    Sterrenstof, Sterrenkompas), the worn armour looks, and all texts (nl + en).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_z6wezens.py
(rerun after a gradle build for the worn armour textures, which need the minecraft jar in build/).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
    from features import vadsrijk_z3_wezens as z3
    from features import vadsrijk_t_z4wezens as z4
    from features import vadsrijk_t_z5wezens as z5
    from features import vadsrijk_t_rijdier as TR
    from features import vadsrijk_rijdieren as RD
    from features.vadsrijk_economie import Doek, _licht, zwaard, boog, staf, zadel
    from features.vadsrijk_z2_systemen import kristal, kruisboog
    from features.vadsrijk_z3_systemen import knots
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
    from features import vadsrijk_z3_wezens as z3
    from features import vadsrijk_t_z4wezens as z4
    from features import vadsrijk_t_z5wezens as z5
    from features import vadsrijk_t_rijdier as TR
    from features import vadsrijk_rijdieren as RD
    from features.vadsrijk_economie import Doek, _licht, zwaard, boog, staf, zadel
    from features.vadsrijk_z2_systemen import kristal, kruisboog
    from features.vadsrijk_z3_systemen import knots

Model, kf, anim, anim_bestand = vw.Model, vw.kf, vw.anim, vw.anim_bestand
samen, adem, een, stil = z2.samen, z2.adem, z2.een, z2.stil
bob, draai, hup, rond = z4.bob, z4.draai, z4.hup, z4.rond
SUB = "vadsrijk/zone6/wezens"
BSUB = "vadsrijk/zone6/bazen"
OOG = ((40, 30, 50), None, None)
ZOUT = ((236, 230, 240), "facet", (250, 220, 236))
ROZE_ZOUT = ((250, 196, 214), "facet", (255, 226, 236))
MESSING = ((222, 176, 92), "glans", None)


def lk(a, t=0.5, as_=0):
    v = lambda s: tuple(s if j == as_ else 0 for j in range(3))
    return {"rotation": kf([(0, v(a)), (t / 2, v(-a)), (t, v(a))])}


def poten4(m, ouder, y, xs, zs, w=2.0, h=None, rol="poot"):
    """Four legs as bones poot_lv/rv/la/ra hanging from y."""
    h = y if h is None else h
    for nm, x, z in (("poot_lv", -xs, -zs), ("poot_rv", xs, -zs), ("poot_la", -xs, zs), ("poot_ra", xs, zs)):
        m.bone(nm, ouder, (x, y, z))
        m.cube(nm, rol, (x - w / 2, y - h, z - w / 2), (w, h, w))


def loop4(t=0.5, a=30):
    return {"poot_lv": lk(a, t), "poot_ra": lk(a, t), "poot_rv": lk(-a, t), "poot_la": lk(-a, t)}


# =====================================================================================================================
# the creatures (front = -z; the face sits on the north face of the cube with "gezicht")
# =====================================================================================================================
def m_zandvlooi():
    m = Model("zandvlooi", px=4, breedte=64, bounds=(1, 1, 0.4))
    m.bone("root")
    m.bone("lijf", "root", (0, 2, 0))
    m.cube("lijf", "schild", (-2.5, 1, -3), (5, 4, 6), gezicht={"mood": "blij", "ey": 0.45})
    m.cube("lijf", "buik", (-2, 0.6, -2.5), (4, 0.6, 5))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "voelspriet_" + kant
        m.bone(b, "lijf", (sx * 1, 5, -2.5), (-15, 0, sx * -15))
        m.cube(b, "spriet", (sx * 1 - 0.3, 5, -2.8), (0.6, 3, 0.6))
        m.cube(b, "bol", (sx * 1 - 0.6, 8, -3.1), (1.2, 1.2, 1.2))
        p = "poten_" + kant
        m.bone(p, "root", (sx * 2.5, 1.5, 0))
        for z in (-1.8, 0, 1.8):
            m.cube(p, "poot", (sx * 2.5 + (0 if sx > 0 else -2.5), 0.5, z - 0.4), (2.5, 0.8, 0.8), rot=(0, 0, -30 * sx), pivot=(sx * 2.5, 1.5, z))
    a = {
        "idle": anim(1.2, samen(adem("lijf", 1.2, 1.05), draai("voelspriet_l", 10, 1.2, "x"), draai("voelspriet_r", -10, 1.2, "x"))),
        "hop": anim(0.4, samen(hup("root", 0.4, 2.5), draai("poten_l", 25, 0.4), draai("poten_r", -25, 0.4))),
        "verstopt": anim(1.0, samen({"root": {"position": kf([(0, (0, -4.6, 0))])}}, draai("voelspriet_l", 15, 0.5, "x"))),
        "stuiter": een(0.5, {"root": {"position": kf([(0, (0, 0, 0)), (0.25, (0, 5, 0)), (0.5, (0, 0, 0))]),
                                      "rotation": kf([(0, (0, 0, 0)), (0.5, (-360, 0, 0))])}}),
        "aanval": een(0.3, {"lijf": {"rotation": kf([(0, (0, 0, 0)), (0.12, (-25, 0, 0)), (0.3, (0, 0, 0))])}}),
        "blij": anim(0.5, samen(hup("root", 0.5, 2.5), draai("voelspriet_l", 30, 0.5), draai("voelspriet_r", -30, 0.5))),
    }
    pal = {"schild": ((244, 176, 164), "plekjes", (252, 206, 190)), "buik": ((252, 226, 210), None, None), "spriet": ((214, 150, 140), None, None),
           "bol": ((255, 150, 186), None, None), "poot": ((222, 156, 146), None, None)}
    return m, a, pal, {"bol": (120, 60, 80)}


def m_fennekguh():
    m = Model("fennekguh", px=4, breedte=96, bounds=(1.6, 1.8, 0.5))
    m.bone("root")
    poten4(m, "root", 4, 2, 3.5, 2)
    m.bone("lijf", "root", (0, 6, 0))
    m.cube("lijf", "vacht", (-3, 4, -5), (6, 5, 10))
    m.cube("lijf", "buik", (-2.5, 3.6, -4.5), (5, 0.6, 9))
    m.bone("kop", "lijf", (0, 8, -5))
    m.cube("kop", "vacht", (-3.5, 6, -10), (7, 6, 5), gezicht={"mood": "blij", "ey": 0.48})
    m.cube("kop", "snuit", (-1.5, 6.4, -11.2), (3, 2, 1.4))
    m.cube("kop", "neus", (-0.6, 7.6, -11.5), (1.2, 0.8, 0.4))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "oor_" + kant
        m.bone(b, "kop", (sx * 2.2, 12, -7.5), (0, 0, sx * -12))
        m.cube(b, "oor", (sx * 2.2 - 2, 12, -8), (4, 8, 1))
        m.cube(b, "oorbinnen", (sx * 2.2 - 1.3, 12.6, -8.3), (2.6, 6, 0.4))
    m.bone("staart", "lijf", (0, 8, 5), (-35, 0, 0))
    m.cube("staart", "vacht", (-1.6, 6.5, 5), (3.2, 3.2, 6))
    m.cube("staart", "punt", (-1.6, 6.5, 11), (3.2, 3.2, 2.2))
    a = {
        "idle": anim(2.0, samen(adem("lijf", 2.0, 1.03), draai("oor_l", 6, 2.0), draai("oor_r", -6, 2.0), draai("staart", 10, 2.0, "y"))),
        "loop": anim(0.45, samen(loop4(0.45, 35), bob("lijf", 0.45, 0.6), draai("staart", 15, 0.45, "y"))),
        "aanval": een(0.4, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.15, (25, 0, 0)), (0.4, (0, 0, 0))])}}),
        "speciaal": een(0.8, {"oor_l": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-25, 0, 10)), (0.8, (0, 0, 0))])},
                              "oor_r": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-25, 0, -10)), (0.8, (0, 0, 0))])},
                              "root": {"position": kf([(0, (0, 0, 0)), (0.3, (0, -1, 0)), (0.8, (0, 0, 0))])}}),
        "blij": anim(0.5, samen(hup("root", 0.5, 3), draai("staart", 35, 0.25, "y"))),
    }
    pal = {"vacht": ((246, 210, 160), "plekjes", (252, 228, 190)), "buik": ((255, 246, 232), None, None), "snuit": ((255, 240, 220), None, None),
           "neus": ((80, 50, 60), None, None), "oor": ((240, 196, 146), None, None), "oorbinnen": ((255, 190, 200), None, None),
           "punt": ((255, 250, 244), None, None), "poot": ((232, 190, 140), None, None)}
    return m, a, pal, {}


def m_springpiep():
    m = Model("springpiep", px=4, breedte=64, bounds=(1.4, 1.6, 0.4))
    m.bone("root")
    m.bone("lijf", "root", (0, 5, 0))
    m.cube("lijf", "vacht", (-3, 3, -3), (6, 6, 6), gezicht={"mood": "blij", "ey": 0.5})
    m.cube("lijf", "buik", (-2.4, 3.2, -3.3), (4.8, 3.5, 0.4))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "oor_" + kant
        m.bone(b, "lijf", (sx * 1.6, 9, -0.5), (0, 0, sx * -10))
        m.cube(b, "oor", (sx * 1.6 - 1, 9, -1), (2, 5, 1))
        bn = "been_" + kant
        m.bone(bn, "root", (sx * 2, 4, 1))
        m.cube(bn, "vacht", (sx * 2 - 1, 1.5, 0), (2, 3, 3))
        m.cube(bn, "poot", (sx * 2 - 0.8, 0, -1.5), (1.6, 1.5, 4))
    m.bone("staart", "lijf", (0, 4, 3), (-20, 0, 0))
    m.cube("staart", "staart", (-0.4, 3.6, 3), (0.8, 0.8, 9))
    m.cube("staart", "pluim", (-1, 3.2, 11.5), (2, 1.6, 2.5))
    a = {
        "idle": anim(1.6, samen(adem("lijf", 1.6, 1.04), draai("oor_l", 8, 1.6), draai("oor_r", -8, 1.6), draai("staart", 10, 1.6, "x"))),
        "hop": anim(0.45, samen(hup("root", 0.45, 4), {"been_l": lk(40, 0.45), "been_r": lk(40, 0.45)}, draai("staart", 20, 0.45, "x"))),
        "spuug": een(0.4, {"been_l": {"rotation": kf([(0, (0, 0, 0)), (0.15, (70, 0, 0)), (0.4, (0, 0, 0))])},
                           "been_r": {"rotation": kf([(0, (0, 0, 0)), (0.15, (70, 0, 0)), (0.4, (0, 0, 0))])}}),
        "stuiter": een(0.6, {"root": {"position": kf([(0, (0, 0, 0)), (0.3, (0, 6, 0)), (0.6, (0, 0, 0))]),
                                      "rotation": kf([(0, (0, 0, 0)), (0.6, (360, 0, 0))])}}),
        "blij": anim(0.5, samen(hup("root", 0.5, 4), draai("oor_l", 30, 0.5), draai("oor_r", -30, 0.5))),
    }
    pal = {"vacht": ((240, 214, 170), "plekjes", (250, 230, 196)), "buik": ((255, 248, 236), None, None), "oor": ((246, 196, 186), None, None),
           "poot": ((236, 200, 160), None, None), "staart": ((236, 206, 160), None, None), "pluim": ((255, 252, 250), None, None)}
    return m, a, pal, {}


def m_duinkruiper():
    m = Model("duinkruiper", px=4, breedte=96, bounds=(1.8, 1.4, 0.4))
    m.bone("root")
    m.bone("lijf", "root", (0, 2.5, 0))
    m.cube("lijf", "schild", (-3.5, 1.5, -5), (7, 3, 9), gezicht={"mood": "blij", "ey": 0.4})
    for kant, sx in (("l", -1), ("r", 1)):
        b = "schaar_" + kant
        m.bone(b, "lijf", (sx * 3, 3, -4.5))
        m.cube(b, "schild", (sx * 3 + (0 if sx > 0 else -1.5), 2.3, -7.5), (1.5, 1.5, 3))
        m.cube(b, "schaar", (sx * 4 - 1.6, 1.8, -10.5), (3.2, 2.4, 3.2))
        p = "poten_" + kant
        m.bone(p, "lijf", (sx * 3.5, 2, 0))
        for z in (-2.5, 0, 2.5):
            m.cube(p, "poot", (sx * 3.5 + (0 if sx > 0 else -3), 0.6, z - 0.4), (3, 0.8, 0.8), rot=(0, 0, -25 * sx), pivot=(sx * 3.5, 2, z))
    m.bone("staart", "lijf", (0, 3.5, 4), (55, 0, 0))
    m.cube("staart", "schild", (-1.2, 3, 4), (2.4, 2.4, 4))
    m.bone("staart2", "staart", (0, 4.2, 8), (40, 0, 0))
    m.cube("staart2", "schild", (-1, 3.4, 8), (2, 2, 3.5))
    m.bone("angel", "staart2", (0, 4.4, 11.5), (50, 0, 0))
    m.cube("angel", "angel", (-0.8, 3.8, 11.5), (1.6, 1.6, 2.5))
    m.cube("angel", "strik", (-2.2, 4.0, 11.6), (4.4, 1.2, 0.8))
    a = {
        "idle": anim(2.0, samen(adem("lijf", 2.0, 1.03), draai("staart", 6, 2.0, "x"), draai("schaar_l", 8, 2.0, "y"), draai("schaar_r", -8, 2.0, "y"))),
        "loop": anim(0.5, samen(draai("poten_l", 20, 0.5), draai("poten_r", 20, 0.5), draai("staart", 5, 0.5, "x"))),
        "speciaal": een(0.6, {"staart": {"rotation": kf([(0, (55, 0, 0)), (0.2, (95, 0, 0)), (0.35, (20, 0, 0)), (0.6, (55, 0, 0))])},
                              "staart2": {"rotation": kf([(0, (40, 0, 0)), (0.2, (60, 0, 0)), (0.35, (10, 0, 0)), (0.6, (40, 0, 0))])}}),
        "aanval": een(0.4, {"schaar_l": {"rotation": kf([(0, (0, 0, 0)), (0.15, (0, 30, 0)), (0.4, (0, 0, 0))])},
                            "schaar_r": {"rotation": kf([(0, (0, 0, 0)), (0.15, (0, -30, 0)), (0.4, (0, 0, 0))])}}),
        "blij": anim(0.6, samen(hup("root", 0.6, 2), draai("staart", 25, 0.3, "y"))),
    }
    pal = {"schild": ((250, 160, 186), "plekjes", (255, 196, 214)), "schaar": ((246, 140, 172), None, None), "poot": ((240, 150, 176), None, None),
           "angel": ((255, 220, 230), None, None), "strik": ((140, 210, 255), None, None)}
    return m, a, pal, {}


def m_dadelaapje():
    m = Model("dadelaapje", px=4, breedte=64, bounds=(1.4, 1.8, 0.4))
    m.bone("root")
    for kant, sx in (("l", -1), ("r", 1)):
        b = "been_" + kant
        m.bone(b, "root", (sx * 1.3, 4, 0))
        m.cube(b, "vacht", (sx * 1.3 - 0.9, 0, -0.9), (1.8, 4, 1.8))
    m.bone("lijf", "root", (0, 4, 0))
    m.cube("lijf", "vacht", (-2.5, 4, -2), (5, 5, 4))
    m.cube("lijf", "buik", (-1.8, 4.4, -2.3), (3.6, 3.6, 0.4))
    m.bone("kop", "lijf", (0, 9, 0))
    m.cube("kop", "vacht", (-3.5, 9, -3.5), (7, 6, 6), gezicht={"mood": "blij", "ey": 0.48})
    m.cube("kop", "snuit", (-2, 9.4, -4), (4, 2.4, 0.6))
    for kant, sx in (("l", -1), ("r", 1)):
        m.cube("kop", "oor", (sx * 3.8 - 1, 11.5, -1), (2, 2.5, 1))
        b = "arm_" + kant
        m.bone(b, "lijf", (sx * 2.8, 8.5, 0))
        m.cube(b, "vacht", (sx * 2.8 + (0 if sx > 0 else -1.4), 3.5, -0.7), (1.4, 5, 1.4))
    m.cube("arm_r", "dadel", (3.2, 2.6, -1), (1.4, 1.6, 1.4))
    m.bone("staart", "lijf", (0, 5, 2), (-40, 0, 0))
    m.cube("staart", "vacht", (-0.5, 4.5, 2), (1, 1, 7))
    m.cube("staart", "vacht", (-0.5, 4.5, 8.5), (1, 3, 1))
    a = {
        "idle": anim(1.8, samen(adem("lijf", 1.8, 1.03), draai("kop", 6, 1.8), draai("staart", 15, 1.8, "y"))),
        "loop": anim(0.45, samen({"been_l": lk(35, 0.45), "been_r": lk(-35, 0.45), "arm_l": lk(-30, 0.45), "arm_r": lk(30, 0.45)}, bob("lijf", 0.45, 0.5))),
        "aanval": een(0.4, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-110, 0, 0)), (0.4, (0, 0, 0))])}}),
        "spuug": een(0.5, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-170, 0, 0)), (0.3, (-40, 0, 0)), (0.5, (0, 0, 0))])}}),
        "blij": anim(0.5, samen(hup("root", 0.5, 3), draai("arm_l", 70, 0.5), draai("arm_r", -70, 0.5))),
    }
    pal = {"vacht": ((210, 156, 120), "plekjes", (226, 176, 140)), "buik": ((250, 222, 196), None, None), "snuit": ((252, 216, 190), None, None),
           "oor": ((240, 186, 160), None, None), "dadel": ((150, 80, 50), "glans", None)}
    return m, a, pal, {}


def m_gierguh():
    m = Model("gierguh", px=4, breedte=96, bounds=(2.4, 1.6, 0.6))
    m.bone("root")
    m.bone("lijf", "root", (0, 4, 0))
    m.cube("lijf", "veer", (-4, 3, -4), (8, 7, 8), gezicht={"mood": "blij", "ey": 0.5})
    m.cube("lijf", "kraag", (-4.6, 2.6, -2), (9.2, 2.2, 6.6))
    m.cube("lijf", "snavel", (-1, 5, -5), (2, 1.6, 1.4))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "vleugel_" + kant
        m.bone(b, "lijf", (sx * 4, 8, 0))
        m.cube(b, "vleugel", (sx * 4 + (0 if sx > 0 else -10), 7.5, -3), (10, 1, 6))
        m.cube(b, "punt", (sx * 4 + (8 if sx > 0 else -12), 7.4, -2), (4, 1.2, 4))
        p = "klauw_" + kant
        m.bone(p, "root", (sx * 1.5, 3, 0))
        m.cube(p, "klauw", (sx * 1.5 - 0.8, 0, -1.2), (1.6, 3, 2.4))
    m.bone("staart", "lijf", (0, 4, 4))
    m.cube("staart", "veer", (-2.5, 3.2, 4), (5, 1, 3))
    a = {
        "idle": anim(2.0, samen(adem("lijf", 2.0, 1.03), draai("vleugel_l", 6, 2.0), draai("vleugel_r", -6, 2.0))),
        "vlieg": anim(0.7, samen(draai("vleugel_l", 35, 0.7), draai("vleugel_r", -35, 0.7), bob("lijf", 0.7, 0.8))),
        "zit": anim(1.0, {"vleugel_l": {"rotation": kf([(0, (0, 0, 60))])}, "vleugel_r": {"rotation": kf([(0, (0, 0, -60))])}}),
        "aanval": een(0.6, {"lijf": {"rotation": kf([(0, (0, 0, 0)), (0.2, (40, 0, 0)), (0.6, (0, 0, 0))])},
                            "vleugel_l": {"rotation": kf([(0, (0, 0, 0)), (0.2, (0, 0, 70)), (0.6, (0, 0, 0))])},
                            "vleugel_r": {"rotation": kf([(0, (0, 0, 0)), (0.2, (0, 0, -70)), (0.6, (0, 0, 0))])}}),
        "blij": anim(0.6, samen(hup("root", 0.6, 3), draai("vleugel_l", 50, 0.3), draai("vleugel_r", -50, 0.3))),
    }
    pal = {"veer": ((230, 190, 170), "plekjes", (244, 210, 196)), "kraag": ((255, 244, 236), "plekjes", (250, 230, 220)),
           "snavel": ((255, 200, 120), None, None), "vleugel": ((210, 160, 146), "strepen", (232, 186, 170)), "punt": ((170, 120, 110), None, None),
           "klauw": ((255, 196, 120), None, None)}
    return m, a, pal, {}


def m_spiegelslang():
    m = Model("spiegelslang", px=4, breedte=96, bounds=(1.8, 1.2, 0.6))
    m.bone("root")
    m.bone("kop", "root", (0, 3, -7))
    m.cube("kop", "glas", (-2.5, 1, -11), (5, 4, 5), gezicht={"mood": "blij", "ey": 0.45})
    m.cube("kop", "kam", (-0.6, 5, -10), (1.2, 1.5, 3))
    vorige = "kop"
    for i, z in enumerate((-6, -2, 2, 6)):
        b = f"seg{i + 1}"
        m.bone(b, vorige if i == 0 else f"seg{i}", (0, 2.5, z))
        breed = 4 - i * 0.6
        m.cube(b, "glas" if i % 2 == 0 else "glans", (-breed / 2, 1, z), (breed, breed * 0.8, 4))
    a = {
        "idle": anim(2.0, samen(draai("seg1", 8, 2.0, "y"), draai("seg2", -10, 2.0, "y"), draai("seg3", 12, 2.0, "y"), draai("kop", 8, 2.0, "x"))),
        "loop": anim(0.8, samen(draai("seg1", 18, 0.8, "y"), draai("seg2", -22, 0.8, "y"), draai("seg3", 25, 0.8, "y"), draai("seg4", -25, 0.8, "y"))),
        "speciaal": een(0.5, {"kop": {"position": kf([(0, (0, 0, 0)), (0.2, (0, 1.5, -4)), (0.5, (0, 0, 0))])}}),
        "aanval": een(0.4, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-25, 0, 0)), (0.4, (0, 0, 0))])}}),
    }
    pal = {"glas": ((236, 240, 255), "glans", None), "glans": ((214, 226, 252), "facet", (250, 236, 250)), "kam": ((255, 200, 226), None, None)}
    return m, a, pal, {"kam": (140, 90, 120)}


def m_zoutkreeft():
    m = Model("zoutkreeft", px=4, breedte=96, bounds=(2, 1.4, 0.4))
    m.bone("root")
    m.bone("lijf", "root", (0, 3, 0))
    m.cube("lijf", "huid", (-4, 1.5, -5), (8, 4, 9), gezicht={"mood": "blij", "ey": 0.42})
    m.bone("korst", "lijf", (0, 5.5, 0))
    for x, z, h in ((-2.5, -2, 2.5), (0.5, -3, 3.2), (2.2, 0.5, 2.2), (-1, 1.5, 2.8), (1.2, 2.5, 1.8)):
        m.cube("korst", "kristal", (x - 0.9, 5.5, z - 0.9), (1.8, h, 1.8), rot=(10, 20 * x, -8 * z), pivot=(x, 5.5, z))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "schaar_" + kant
        m.bone(b, "lijf", (sx * 4, 3.5, -4))
        m.cube(b, "huid", (sx * 4 + (0 if sx > 0 else -1.5), 2.8, -7), (1.5, 1.5, 3))
        m.cube(b, "schaar", (sx * 5 - 2, 2, -11), (4, 3, 4))
        m.cube("lijf", "spriet", (sx * 1.5 - 0.25, 4.5, -11), (0.5, 0.5, 6), rot=(-20, sx * 15, 0), pivot=(sx * 1.5, 4.5, -5))
        p = "poten_" + kant
        m.bone(p, "lijf", (sx * 4, 2.5, 0))
        for z in (-2.5, 0, 2.5):
            m.cube(p, "poot", (sx * 4 + (0 if sx > 0 else -3.5), 1, z - 0.4), (3.5, 0.8, 0.8), rot=(0, 0, -30 * sx), pivot=(sx * 4, 2.5, z))
    m.bone("staart", "lijf", (0, 3, 4))
    m.cube("staart", "huid", (-2.5, 2, 4), (5, 2.5, 3))
    m.cube("staart", "waaier", (-3.5, 2.2, 7), (7, 1, 3))
    a = {
        "idle": anim(2.0, samen(adem("lijf", 2.0, 1.03), draai("schaar_l", 10, 2.0, "y"), draai("schaar_r", -10, 2.0, "y"), draai("staart", 6, 2.0, "x"))),
        "loop": anim(0.5, samen({"root": {"position": kf([(0, (-0.8, 0, 0)), (0.25, (0.8, 0.4, 0)), (0.5, (-0.8, 0, 0))])}},
                                draai("poten_l", 25, 0.5), draai("poten_r", 25, 0.5))),
        "aanval": een(0.4, {"schaar_l": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-40, -25, 0)), (0.4, (0, 0, 0))])},
                            "schaar_r": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-40, 25, 0)), (0.4, (0, 0, 0))])}}),
        "blij": anim(0.6, samen(hup("root", 0.6, 2.5), draai("schaar_l", 40, 0.6), draai("schaar_r", -40, 0.6))),
    }
    pal = {"huid": ((250, 150, 140), "plekjes", (255, 186, 170)), "schaar": ((246, 130, 126), None, None), "kristal": ZOUT,
           "spriet": ((240, 160, 150), None, None), "poot": ((236, 140, 130), None, None), "waaier": ((255, 176, 166), "strepen", (255, 210, 196))}
    return m, a, pal, {}


def m_zandgeestje():
    m = Model("zandgeestje", px=4, breedte=64, bounds=(1.4, 2, 0.6))
    m.bone("root")
    m.bone("werveling", "root", (0, 0, 0))
    for i, (y, r, h) in enumerate(((0, 1.2, 2), (2, 2.0, 2.5), (4.5, 2.8, 3), (7.5, 3.6, 3.5))):
        m.cube("werveling", "stof" if i % 2 == 0 else "stof2", (-r, y, -r), (2 * r, h, 2 * r), rot=(0, 20 * i, 0), pivot=(0, y, 0))
    m.bone("kop", "werveling", (0, 11, 0))
    m.cube("kop", "stof", (-3, 11, -3), (6, 5, 6), gezicht={"mood": "geest", "ey": 0.5})
    m.cube("kop", "sterretje", (-0.6, 16.5, -0.6), (1.2, 1.2, 1.2))
    a = {
        "idle": anim(1.0, rond("werveling", 1.0)),
        "zweef": anim(0.6, samen(rond("werveling", 0.6), bob("kop", 0.6, 0.6))),
        "speciaal": een(1.0, {"werveling": {"rotation": kf([(0, (0, 0, 0)), (1.0, (0, 1080, 0))]),
                                            "scale": kf([(0, (1, 1, 1)), (0.5, (1.4, 1, 1.4)), (1.0, (1, 1, 1))])}}),
    }
    pal = {"stof": ((246, 196, 186), "plekjes", (255, 226, 214)), "stof2": ((236, 180, 170), "plekjes", (250, 214, 200)),
           "sterretje": ((255, 246, 190), None, None)}
    return m, a, pal, {"sterretje": (255, 240, 170), "stof": (60, 40, 40)}


def m_ruinewachter():
    m = Model("ruinewachter", px=4, breedte=96, bounds=(2, 2.6, 0.6))
    m.bone("root")
    for kant, sx in (("l", -1), ("r", 1)):
        b = "been_" + kant
        m.bone(b, "root", (sx * 2.2, 5, 0))
        m.cube(b, "steen", (sx * 2.2 - 1.6, 0, -1.6), (3.2, 5, 3.2))
    m.bone("lijf", "root", (0, 5, 0))
    m.cube("lijf", "steen", (-5, 5, -4), (10, 12, 8), gezicht={"mood": "blij", "ey": 0.62})
    m.cube("lijf", "korst", (-5.3, 13, -4.3), (3.5, 2.5, 3))
    m.cube("lijf", "korst", (2.4, 7, -4.3), (3, 3, 2))
    m.cube("lijf", "band", (-5.2, 9, -4.2), (10.4, 1, 8.4))
    for kant, sx in (("l", -1), ("r", 1)):
        m.cube("lijf", "steen", (sx * 3.2 - 1.3, 17, -1), (2.6, 3, 2))
        b = "arm_" + kant
        m.bone(b, "lijf", (sx * 5.5, 15, 0))
        m.cube(b, "steen", (sx * 5.5 + (0 if sx > 0 else -2.6), 8, -1.3), (2.6, 7, 2.6))
    m.bone("hoed", "lijf", (0, 17, 0))
    m.cube("hoed", "goud", (-2.5, 17, -2.5), (5, 2.5, 5))
    m.cube("hoed", "ster", (-0.6, 19.5, -0.6), (1.2, 1.2, 1.2))
    a = {
        "idle": anim(2.4, samen(adem("lijf", 2.4, 1.02), draai("arm_l", 5, 2.4, "x"), draai("arm_r", -5, 2.4, "x"))),
        "loop": anim(0.9, samen({"been_l": lk(25, 0.9), "been_r": lk(-25, 0.9), "arm_l": lk(-20, 0.9), "arm_r": lk(20, 0.9)}, bob("lijf", 0.9, 0.4))),
        "standbeeld": anim(1.0, {"arm_l": {"rotation": kf([(0, (-30, 0, 20))])}, "arm_r": {"rotation": kf([(0, (-30, 0, -20))])}}),
        "speciaal": een(1.0, {"arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-170, 0, 0)), (0.6, (10, 0, 0)), (1.0, (0, 0, 0))])},
                              "arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-170, 0, 0)), (0.6, (10, 0, 0)), (1.0, (0, 0, 0))])},
                              "root": {"position": kf([(0, (0, 0, 0)), (0.6, (0, -1, 0)), (1.0, (0, 0, 0))])}}),
        "aanval": een(0.5, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-90, 0, 0)), (0.5, (0, 0, 0))])}}),
        "blij": anim(0.8, samen(hup("root", 0.8, 1.5), draai("arm_l", 60, 0.4), draai("arm_r", -60, 0.4))),
    }
    pal = {"steen": ((222, 196, 176), "plekjes", (236, 214, 196)), "korst": ZOUT, "band": ((120, 200, 196), "strepen", (255, 220, 120)),
           "goud": ((240, 200, 100), "glans", None), "ster": ((255, 246, 200), None, None)}
    return m, a, pal, {"ster": (255, 236, 160)}


def mika_pal(vest, **extra):
    return z5.mika_pal(vest, **extra)


def m_mika_lampendover():
    m, a = z2.mika("mika_lampendover", "houweel")
    m.cube("wapen", "steel", (4.2, 9, -0.5), (1, 16, 1), rot=(-60, 0, 0), pivot=(4.7, 9.5, 0))
    m.cube("wapen", "dover", (2.8, 23, -9), (4, 4, 4), rot=(-60, 0, 0), pivot=(4.7, 9.5, 0))
    m.cube("wapen", "dover", (3.3, 26.6, -8.5), (3, 1.5, 3), rot=(-60, 0, 0), pivot=(4.7, 9.5, 0))
    a["speciaal"] = een(0.7, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-160, 0, 0)), (0.45, (30, 0, 0)), (0.7, (0, 0, 0))])}})
    a["blij"] = anim(1.0, stil())
    pal = mika_pal(((80, 70, 110), "strepen", (200, 190, 230)), helm=((70, 64, 90), None, None), dover=((150, 150, 160), "glans", None))
    return m, a, pal, {"lamp": (100, 80, 30)}


def m_mika_ringdrager():
    m, a = z2.mika("mika_ringdrager", "houweel")
    for x in (-3.4, 0, 3.4):
        m.cube("lijf", "pot", (x - 1.1, 9.5, -3.6), (2.2, 2.6, 1.6))
        m.cube("lijf", "deksel", (x - 0.9, 12.1, -3.4), (1.8, 0.6, 1.2))
    m.cube("kop", "ring", (-5.5, 27.3, -5.5), (11, 0.6, 10))
    m.cube("wapen", "pot", (3.6, 4.8, -2.6), (2.4, 3, 2.4))
    a["speciaal"] = anim(1.0, {"arm_r": {"rotation": kf([(0, (-90, 0, 0)), (0.5, (-95, 0, 4)), (1.0, (-90, 0, 0))])}})
    a["blij"] = anim(1.0, stil())
    pal = mika_pal(((60, 52, 84), "strepen", (240, 200, 90)), helm=((50, 44, 70), None, None), pot=((190, 220, 255), "glans", None),
                   deksel=((240, 200, 90), None, None), ring=((250, 210, 100), "glans", None))
    return m, a, pal, {"pot": (120, 150, 200), "lamp": (100, 80, 30)}


def m_gedoofd_sterretje():
    m = Model("gedoofd_sterretje", px=4, breedte=64, bounds=(1.4, 1.4, 0.6))
    m.bone("root")
    m.bone("ster", "root", (0, 4, 0))
    m.cube("ster", "ster", (-3, 1, -1.5), (6, 6, 3), gezicht={"mood": "blij", "ey": 0.45})
    m.bone("punten", "ster", (0, 4, 0))
    for i in range(5):
        h = i * 72
        m.cube("punten", "punt", (-1.2, 6.5, -1), (2.4, 4, 2), rot=(0, 0, h), pivot=(0, 4, 0))
    a = {
        "idle": anim(2.0, samen(bob("ster", 2.0, 0.8), {"punten": {"rotation": kf([(0, (0, 0, 0)), (2.0, (0, 0, 72))])}})),
        "zweef": anim(1.2, samen(bob("ster", 1.2, 1.0), {"punten": {"rotation": kf([(0, (0, 0, 0)), (1.2, (0, 0, 72))])}})),
        "speciaal": een(0.6, {"ster": {"scale": kf([(0, (1, 1, 1)), (0.2, (1.3, 1.3, 1.3)), (0.6, (1, 1, 1))])}}),
        "blij": anim(0.8, samen(hup("root", 0.8, 3), rond("ster", 0.8))),
    }
    pal = {"ster": ((255, 236, 170), "glans", None), "punt": ((255, 226, 150), "glans", None)}
    return m, a, pal, {"ster": (90, 80, 50), "punt": (90, 80, 50)}


def m_zoutslang():
    m = Model("zoutslang", px=4, breedte=128, bounds=(2.4, 1.6, 0.8))
    m.bone("root")
    m.bone("kop", "root", (0, 4, -9))
    m.cube("kop", "kristal", (-3.5, 1.5, -15), (7, 5.5, 7), gezicht={"mood": "blij", "ey": 0.42})
    for x, z, h in ((-2, -12, 3), (0, -11, 4), (2, -12, 3)):
        m.cube("kop", "punt", (x - 0.7, 7, z - 0.7), (1.4, h, 1.4), rot=(-15, 0, x * 6), pivot=(x, 7, z))
    for i, z in enumerate((-8, -3, 2, 7, 12)):
        b = f"seg{i + 1}"
        m.bone(b, "kop" if i == 0 else f"seg{i}", (0, 3.5, z))
        breed = 6 - i * 0.8
        m.cube(b, "kristal" if i % 2 == 0 else "roze", (-breed / 2, 1, z), (breed, breed * 0.8, 5))
        m.cube(b, "punt", (-0.6, 1 + breed * 0.8, z + 1.5), (1.2, 2, 1.2))
    a = {
        "idle": anim(2.4, samen(draai("seg1", 8, 2.4, "y"), draai("seg2", -10, 2.4, "y"), draai("seg3", 12, 2.4, "y"), draai("kop", 6, 2.4, "x"))),
        "loop": anim(1.0, samen(draai("seg1", 18, 1.0, "y"), draai("seg2", -22, 1.0, "y"), draai("seg3", 25, 1.0, "y"), draai("seg4", -25, 1.0, "y"),
                                draai("seg5", 25, 1.0, "y"))),
        "onder": anim(1.0, {"root": {"position": kf([(0, (0, -16, 0))])}}),
        "speciaal": een(0.6, {"kop": {"position": kf([(0, (0, -8, 0)), (0.25, (0, 3, -3)), (0.6, (0, 0, 0))])}}),
        "aanval": een(0.4, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-25, 0, 0)), (0.4, (0, 0, 0))])}}),
    }
    pal = {"kristal": ROZE_ZOUT, "roze": ((246, 170, 200), "facet", (255, 214, 230)), "punt": ((255, 236, 246), "glans", None)}
    return m, a, pal, {"punt": (120, 70, 100)}


WEZENS = [("zandvlooi", m_zandvlooi), ("fennekguh", m_fennekguh), ("springpiep", m_springpiep), ("duinkruiper", m_duinkruiper),
          ("dadelaapje", m_dadelaapje), ("gierguh", m_gierguh), ("spiegelslang", m_spiegelslang), ("zoutkreeft", m_zoutkreeft),
          ("zandgeestje", m_zandgeestje), ("ruinewachter", m_ruinewachter), ("mika_lampendover", m_mika_lampendover),
          ("mika_ringdrager", m_mika_ringdrager), ("gedoofd_sterretje", m_gedoofd_sterretje), ("zoutslang", m_zoutslang)]
MIKA = {"mika_lampendover", "mika_ringdrager"}
VERSLAAN = {"zandvlooi", "spiegelslang", "zandgeestje", "zoutslang"}
NODIG = {
    "zandvlooi": ["idle", "hop", "verstopt", "stuiter", "blij"], "fennekguh": ["idle", "loop", "aanval", "speciaal", "blij"],
    "springpiep": ["idle", "hop", "spuug", "stuiter", "blij"], "duinkruiper": ["idle", "loop", "speciaal", "aanval", "blij"],
    "dadelaapje": ["idle", "loop", "aanval", "spuug", "blij"], "gierguh": ["idle", "vlieg", "zit", "aanval", "blij"],
    "spiegelslang": ["idle", "loop", "speciaal", "aanval"], "zoutkreeft": ["idle", "loop", "aanval", "blij"],
    "zandgeestje": ["idle", "zweef", "speciaal"], "ruinewachter": ["idle", "loop", "standbeeld", "speciaal", "blij"],
    "mika_lampendover": ["idle", "loop", "aanval", "speciaal"], "mika_ringdrager": ["idle", "loop", "aanval", "speciaal"],
    "gedoofd_sterretje": ["idle", "zweef", "speciaal", "blij"], "zoutslang": ["idle", "loop", "onder", "speciaal"],
}


# =====================================================================================================================
# the bosses, their objects, the pets
# =====================================================================================================================
def m_wormelien():
    m = Model("vr_z6_baas_wormelien", px=4, breedte=128, bounds=(3.4, 4, 2))
    m.bone("root")
    m.bone("lijf", "root", (0, 0, 0))
    for i, (y, r, z) in enumerate(((0, 9, 3), (8, 8, 1), (16, 7, -1))):
        m.cube("lijf", "huid" if i % 2 == 0 else "ring", (-r, y, z - r), (2 * r, 8.2, 2 * r))
    m.bone("kop", "lijf", (0, 24, -2))
    m.cube("kop", "huid", (-8, 24, -10), (16, 14, 15), gezicht={"mood": "blij", "ey": 0.5})
    m.cube("kop", "snoet", (-4, 25, -12), (8, 5, 2.5))
    m.cube("kop", "neus", (-1.5, 28.5, -12.6), (3, 1.5, 0.8))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "oor_" + kant
        m.bone(b, "kop", (sx * 6, 38, -3))
        m.cube(b, "oor", (sx * 6 - 2.5, 37.5, -4), (5, 5, 2))
        m.cube("kop", "wimper", (sx * 4 - 2.5, 35.5, -10.4), (5, 1, 0.6))
    m.bone("staart", "root", (0, 2, 10))
    m.cube("staart", "ring", (-5, 0, 9), (10, 6, 8))
    m.cube("staart", "huid", (-3, 0, 16), (6, 4, 6))
    a = {
        "idle": anim(3.0, samen(draai("lijf", 4, 3.0, "z"), draai("kop", 6, 3.0, "x"), draai("oor_l", 10, 3.0), draai("oor_r", -10, 3.0))),
        "walk": anim(1.2, samen(draai("lijf", 8, 1.2, "z"), draai("staart", 15, 1.2, "y"), bob("kop", 1.2, 1.0))),
        "aanval": een(0.6, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-20, 0, 0)), (0.35, (25, 0, 0)), (0.6, (0, 0, 0))])}}),
        "duik": een(0.8, {"root": {"position": kf([(0, (0, 0, 0)), (0.8, (0, -40, 0))])}}),
        "boven": een(0.8, {"root": {"position": kf([(0, (0, -40, 0)), (0.5, (0, 4, 0)), (0.8, (0, 0, 0))])}}),
        "slok": een(1.5, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.5, (30, 0, 0)), (1.0, (30, 0, 0)), (1.5, (-15, 0, 0))])},
                          "lijf": {"scale": kf([(0, (1, 1, 1)), (1.0, (1.15, 1, 1.15)), (1.5, (1, 1, 1))])}}),
        "spuug": een(0.6, {"kop": {"rotation": kf([(0, (-15, 0, 0)), (0.2, (-35, 0, 0)), (0.6, (0, 0, 0))])}}),
        "golf": een(0.6, {"lijf": {"rotation": kf([(0, (0, 0, 0)), (0.3, (0, 0, 15)), (0.6, (0, 0, 0))])}}),
        "slaap": anim(3.0, samen({"lijf": {"rotation": kf([(0, (0, 0, 70))]), "position": kf([(0, (0, -6, 0))])}}, adem("kop", 3.0, 1.03))),
        "hop": anim(0.5, hup("root", 0.5, 3)), "sprong": een(0.6, hup("root", 0.6, 5)),
    }
    pal = {"huid": ((250, 180, 196), "plekjes", (255, 206, 220)), "ring": ((240, 160, 182), "strepen", (255, 196, 212)),
           "snoet": ((255, 214, 224), None, None), "neus": ((200, 90, 120), None, None), "oor": ((246, 166, 190), None, None),
           "wimper": ((70, 40, 60), None, None)}
    return m, a, pal


def m_zoutkolos():
    m, a = z2.golem("vr_z6_baas_zoutkolos", 1.2)
    m.cube("lijf", "hart", (-2.5, 17, -4.6), (5, 5, 1))
    for x, y in ((-6, 23), (5, 24), (-2, 26)):
        m.cube("lijf", "kristal", (x - 1, y, -2), (2, 4, 2), rot=(0, 0, x * 3), pivot=(x, y, 0))
    a.setdefault("walk", a.get("loop", anim(1.0, stil())))
    a["stamp"] = een(0.8, {"lijf": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-15, 0, 0)), (0.5, (15, 0, 0)), (0.8, (0, 0, 0))])},
                           "root": {"position": kf([(0, (0, 0, 0)), (0.3, (0, 3, 0)), (0.5, (0, -1, 0)), (0.8, (0, 0, 0))])}})
    a["gooi"] = een(0.6, {"lijf": {"rotation": kf([(0, (0, 0, 0)), (0.25, (0, 30, 0)), (0.6, (0, 0, 0))])}})
    a["hart"] = een(1.0, {"lijf": {"scale": kf([(0, (1, 1, 1)), (0.5, (1.08, 1.08, 1.08)), (1.0, (1, 1, 1))])}})
    a.setdefault("aanval", a["stamp"])
    pal = {"steen": ZOUT, "donker": ((210, 196, 222), "facet", (236, 226, 244)), "kristal": ROZE_ZOUT, "hart": ((255, 210, 230), "glans", None)}
    return m, a, pal


def m_astrolabiumwachter():
    m = Model("vr_z6_baas_astrolabiumwachter", px=4, breedte=128, bounds=(3, 3.4, 1.6))
    m.bone("root")
    m.bone("kern", "root", (0, 22, 0))
    m.cube("kern", "ster", (-4, 18, -4), (8, 8, 8), gezicht={"mood": "blij", "ey": 0.5})
    m.cube("kern", "ster", (-1, 26, -1), (2, 4, 2))
    m.cube("kern", "ster", (-1, 14, -1), (2, 4, 2))
    m.bone("voet", "root", (0, 0, 0))
    m.cube("voet", "messing", (-5, 0, -5), (10, 2, 10))
    m.cube("voet", "messing", (-1, 2, -1), (2, 10, 2))
    for i in range(4):
        b = f"ring_{i + 1}"
        r = 8 + i * 3.2
        m.bone(b, "kern", (0, 22, 0), ((0, 0, 0), (60, 0, 0), (0, 0, 60), (45, 45, 0))[i])
        d = 1.2
        m.cube(b, "messing", (-r, 22 - d / 2, -r), (2 * r, d, d))
        m.cube(b, "messing", (-r, 22 - d / 2, r - d), (2 * r, d, d))
        m.cube(b, "messing", (-r, 22 - d / 2, -r), (d, d, 2 * r))
        m.cube(b, "messing", (r - d, 22 - d / 2, -r), (d, d, 2 * r))
        m.cube(b, "glim", (-0.8, 22 - 0.8, -r - 0.8), (1.6, 1.6, 1.6))
    spin = {f"ring_{i + 1}": {"rotation": kf([(0, (0, 0, 0)), (4.0, (0, 360 * (1 if i % 2 == 0 else -1), 0))])} for i in range(4)}
    snel = {f"ring_{i + 1}": {"rotation": kf([(0, (0, 0, 0)), (1.0, (0, 360 * (1 if i % 2 == 0 else -1), 0))])} for i in range(4)}
    a = {
        "idle": anim(4.0, samen(spin, bob("kern", 4.0, 1.0))),
        "walk": anim(4.0, samen(spin, bob("kern", 4.0, 1.0))),
        "zwaai": een(1.4, snel),
        "lijn": anim(1.0, {f"ring_{i + 1}": {"rotation": kf([(0, (0, 0, 0))])} for i in range(4)}, loop=False),
        "aanval": een(0.5, {"kern": {"scale": kf([(0, (1, 1, 1)), (0.2, (1.15, 1.15, 1.15)), (0.5, (1, 1, 1))])}}),
        "straal": een(0.6, {"kern": {"rotation": kf([(0, (0, 0, 0)), (0.3, (0, 45, 0)), (0.6, (0, 0, 0))])}}),
    }
    pal = {"ster": ((255, 230, 170), "glans", None), "messing": MESSING, "glim": ((140, 230, 226), None, None)}
    return m, a, pal


def m_glimmertje():
    m, a = z2.mika("vr_z6_baas_glimmertje", "houweel")
    m.cube("wapen", "steel", (4.2, 9, -0.5), (1, 20, 1), rot=(-60, 0, 0), pivot=(4.7, 9.5, 0))
    m.cube("wapen", "dover", (2.2, 27, -10), (5, 5, 5), rot=(-60, 0, 0), pivot=(4.7, 9.5, 0))
    m.cube("kop", "hoed", (-3, 28, -3), (6, 5, 6))
    m.cube("kop", "band", (-3.2, 28.5, -3.2), (6.4, 1, 6.4))
    a["walk"] = a["loop"]
    a["doof"] = a["klap"]
    a["gooi"] = a["spuug"]
    a["fluit"] = een(1.0, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-25, 0, 0)), (1.0, (0, 0, 0))])}})
    pal = mika_pal(((90, 60, 120), "strepen", (240, 200, 90)), helm=((60, 50, 80), None, None), dover=((170, 170, 186), "glans", None),
                   hoed=((70, 50, 90), None, None), band=((240, 200, 90), None, None))
    return m, a, pal


def m_sterrenguh():
    m = Model("vr_z6_baas_sterrenguh", px=4, breedte=128, bounds=(3.6, 4.2, 2))
    m.bone("root")
    for kant, sx in (("l", -1), ("r", 1)):
        b = "been_" + kant
        m.bone(b, "root", (sx * 4.5, 8, 0))
        m.cube(b, "steen", (sx * 4.5 - 3, 0, -3), (6, 8, 6))
    m.bone("lijf", "root", (0, 8, 0))
    m.cube("lijf", "steen", (-9, 8, -7), (18, 20, 14), gezicht={"mood": "blij", "ey": 0.62})
    m.cube("lijf", "hart", (-2.5, 13, -7.6), (5, 5, 0.8))
    for x, y in ((-6, 22), (5, 15), (-4, 11), (6, 24)):
        m.cube("lijf", "ster", (x - 0.8, y - 0.8, -7.5), (1.6, 1.6, 0.6))
    for kant, sx in (("l", -1), ("r", 1)):
        m.cube("lijf", "steen", (sx * 5.5 - 2, 28, -2), (4, 5, 3))
        b = "arm_" + kant
        m.bone(b, "lijf", (sx * 9, 24, 0))
        m.cube(b, "steen", (sx * 9 + (0 if sx > 0 else -4), 12, -2.5), (4, 12, 5))
    a = {
        "idle": anim(3.0, samen(adem("lijf", 3.0, 1.02), draai("arm_l", 6, 3.0, "x"), draai("arm_r", -6, 3.0, "x"))),
        "walk": anim(1.4, samen({"been_l": lk(20, 1.4), "been_r": lk(-20, 1.4)}, bob("lijf", 1.4, 0.6))),
        "aanval": een(0.7, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-120, 0, 0)), (0.5, (10, 0, 0)), (0.7, (0, 0, 0))])}}),
        "strooi": een(1.0, {"arm_l": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-150, 0, -30)), (1.0, (0, 0, 0))])},
                            "arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.4, (-150, 0, 30)), (1.0, (0, 0, 0))])}}),
        "golf": een(0.8, {"root": {"position": kf([(0, (0, 0, 0)), (0.3, (0, 4, 0)), (0.5, (0, -1, 0)), (0.8, (0, 0, 0))])}}),
        "hart": een(1.0, {"lijf": {"rotation": kf([(0, (0, 0, 0)), (0.3, (-12, 0, 0)), (1.0, (0, 0, 0))])}}),
        "blij": anim(1.0, samen(hup("root", 1.0, 2), draai("arm_l", 80, 0.5), draai("arm_r", -80, 0.5))),
        "hop": anim(0.5, hup("root", 0.5, 3)), "sprong": een(0.6, hup("root", 0.6, 5)),
    }
    pal = {"steen": ((200, 190, 222), "plekjes", (222, 214, 240)), "hart": ((255, 236, 170), "glans", None), "ster": ((255, 246, 200), None, None)}
    return m, a, pal


def m_zoutkorst():
    m = Model("vr_z6_baas_zoutkorst", px=4, breedte=64, bounds=(1.4, 1.2, 0.4))
    m.bone("root")
    for x, z, h in ((-3, -2, 4), (0, -3, 6), (3, -1, 4), (-1.5, 2, 5), (2, 2.5, 3.5)):
        m.cube("root", "zout", (x - 1.3, 0, z - 1.3), (2.6, h, 2.6), rot=(8, 25 * x, 6 * z), pivot=(x, 0, z))
    return m, {"idle": anim(2.0, adem("root", 2.0, 1.02))}, {"zout": ((176, 172, 186), "facet", (204, 200, 214))}


def m_zingkristal():
    m = Model("vr_z6_baas_zingkristal", px=4, breedte=64, bounds=(1, 1.8, 0.4))
    m.bone("root")
    m.bone("kristal", "root", (0, 0, 0))
    for x, z, h in ((0, 0, 12), (-2, 1, 8), (2, -1, 7), (1, 2, 6)):
        m.cube("kristal", "kristal", (x - 1.2, 0, z - 1.2), (2.4, h, 2.4), rot=(6 * z, 0, -8 * x), pivot=(x, 0, z))
    return m, {"idle": anim(1.0, {"kristal": {"scale": kf([(0, (1, 1, 1)), (0.5, (1.05, 1.03, 1.05)), (1.0, (1, 1, 1))])}})}, {"kristal": ROZE_ZOUT}


def m_vuurschaal():
    m = Model("vr_z6_baas_vuurschaal", px=4, breedte=64, bounds=(1.2, 1.6, 0.4))
    m.bone("root")
    m.cube("root", "messing", (-1, 0, -1), (2, 10, 2))
    m.cube("root", "messing", (-3, 0, -3), (6, 1, 6))
    m.cube("root", "schaal", (-4, 10, -4), (8, 3, 8))
    m.bone("vlam", "root", (0, 13, 0))
    m.cube("vlam", "vuur", (-2.5, 13, -2.5), (5, 4, 5))
    m.cube("vlam", "vuur2", (-1.5, 17, -1.5), (3, 3, 3))
    a = {"idle": anim(0.6, {"vlam": {"scale": kf([(0, (1, 1, 1)), (0.3, (0.9, 1.2, 0.9)), (0.6, (1, 1, 1))])}})}
    return m, a, {"messing": MESSING, "schaal": ((200, 150, 80), "glans", None), "vuur": ((255, 170, 70), None, None), "vuur2": ((255, 236, 150), None, None)}


def m_telescoop():
    m = Model("vr_z6_baas_telescoop", px=4, breedte=64, bounds=(1.2, 2.2, 0.6))
    m.bone("root")
    for a_ in (0, 120, 240):
        m.cube("root", "hout", (-0.5, 0, -0.5), (1, 12, 1), rot=(18, a_, 0), pivot=(0, 12, 0))
    m.bone("kijker", "root", (0, 12, 0))
    m.cube("kijker", "messing", (-1.5, 11, -10), (3, 3, 14), rot=(-15, 0, 0), pivot=(0, 12, 0))
    m.cube("kijker", "lens", (-1.8, 10.7, -11), (3.6, 3.6, 1.2), rot=(-15, 0, 0), pivot=(0, 12, 0))
    return m, {"idle": anim(2.0, stil())}, {"hout": ((150, 100, 70), None, None), "messing": MESSING, "lens": ((150, 230, 255), "glans", None)}


def m_zoutklontje():
    m = Model("zoutklontje", px=4, breedte=64, bounds=(1, 1, 0.4))
    m.bone("root")
    m.cube("root", "zout", (-3, 0, -3), (6, 5.5, 6), gezicht={"mood": "blij", "ey": 0.45})
    m.cube("root", "roze", (-1, 5.5, -1), (2, 2, 2))
    return m, {}, {"zout": ZOUT, "roze": ROZE_ZOUT}


# ---- the Bultguh (mount) -----------------------------------------------------------------------------------------------------------
def m_bultguh(naam="eco_bultguh"):
    """The camel-guh: two soft humps with the saddle between (two seats), a long neck, guh ears, tassels and bells. Saddle top ~y 31."""
    m = Model(naam, px=4, breedte=128, bounds=(2.4, 3, 1))
    m.bone("root")
    for nm, x, z in (("leg_fl", -3.2, -6), ("leg_fr", 3.2, -6), ("leg_bl", -3.2, 7), ("leg_br", 3.2, 7)):
        m.bone(nm, "root", (x, 16, z))
        m.cube(nm, "poot", (x - 1.4, 2, z - 1.4), (2.8, 14, 2.8))
        m.cube(nm, "hoef", (x - 1.7, 0, z - 1.8), (3.4, 2, 3.6))
    m.bone("body", "root", (0, 16, 0))
    m.cube("body", "vacht", (-6, 15, -10), (12, 10, 21))
    m.cube("body", "buik", (-5, 14.6, -8), (10, 0.6, 17))
    m.bone("bult_v", "body", (0, 25, -6))
    m.cube("bult_v", "vacht", (-4.5, 25, -9), (9, 5, 6))
    m.bone("bult_a", "body", (0, 25, 6))
    m.cube("bult_a", "vacht", (-4.5, 25, 3), (9, 5, 6))
    m.bone("zadel", "body", (0, 25, 0))
    m.cube("zadel", "zadel", (-5, 25, -3), (10, 1.5, 6))
    m.cube("zadel", "deken", (-6.4, 19, -4), (0.6, 6.5, 8))
    m.spiegel("zadel", "deken", (-6.4, 19, -4), (0.6, 6.5, 8))
    m.bone("kwastjes", "zadel", (0, 19, 0))
    for x in (-6.6, 6.0):
        for z in (-3.5, -1, 1.5, 4):
            m.cube("kwastjes", "kwast", (x, 16.5, z - 0.4), (0.8, 2.5, 0.8))
    m.bone("nek", "body", (0, 22, -9), (-20, 0, 0))
    m.cube("nek", "vacht", (-2.5, 21, -15), (5, 5, 7))
    m.cube("nek", "vacht", (-2.2, 24, -16), (4.4, 8, 4.5))
    m.bone("belletjes", "nek", (0, 24, -13))
    m.cube("belletjes", "band", (-2.8, 23.6, -16.4), (5.6, 1, 5))
    for x in (-2, 1):
        m.cube("belletjes", "bel", (x, 21.6, -16.8), (1.2, 1.6, 1.2))
    m.bone("hoofd", "nek", (0, 31, -14))
    m.cube("hoofd", "gezicht", (-3.5, 30, -20), (7, 6, 7), gezicht={"ey": 0.45})
    m.cube("hoofd", "snuit", (-2.5, 30, -22), (5, 3.2, 2.2))
    for kant, sgn in (("l", -1), ("r", 1)):
        b = "oor_" + kant
        m.bone(b, "hoofd", (sgn * 3, 36, -16), (0, 0, sgn * -25))
        if sgn < 0:
            m.cube(b, "oor", (-5, 35.5, -17), (2.5, 3.5, 1.2))
        else:
            m.spiegel(b, "oor", (-5, 35.5, -17), (2.5, 3.5, 1.2))
    m.bone("tail", "body", (0, 23, 11))
    m.cube("tail", "vacht", (-0.6, 17, 11), (1.2, 6, 1.2))
    m.cube("tail", "kwast", (-1, 15, 10.6), (2, 2.5, 2))
    return m


PALET_BULTGUH = {
    "vacht": ((242, 180, 176), "plekjes", (250, 204, 198)),
    "gezicht": ((242, 180, 176), None, None),
    "buik": ((255, 232, 226), None, None),
    "snuit": ((255, 220, 214), None, None),
    "oor": ((236, 166, 164), None, None),
    "poot": ((232, 168, 162), None, None),
    "hoef": ((150, 110, 100), None, None),
    "zadel": ((90, 200, 196), "strepen", (255, 220, 120)),
    "deken": ((255, 140, 180), "strepen", (255, 220, 120)),
    "kwast": ((255, 214, 90), None, None),
    "band": ((90, 200, 196), None, None),
    "bel": ((255, 210, 70), "glans", None),
}
BULTGUH_VACHT = {"saffraan": (255, 192, 72), "wolkwit": (246, 244, 255), "kaneel": (192, 120, 74), "sterrennacht": (58, 52, 120)}
BULTGUH_ROLLEN = ("vacht", "gezicht", "oor", "poot")


def anims_bultguh():
    """The quadruped gait of the stal mounts, plus swaying tassels and bells (and a gentle neck bob: it sways)."""
    a = RD.anims_viervoeter(True)
    for naam, lengte, hoek in (("idle", 2.4, 6), ("hop", 0.7, 14), ("ren", 0.4, 24), ("sprong", 0.4, 20)):
        b = a[naam]["bones"]
        b["kwastjes"] = {"rotation": kf([(0, (0, 0, -hoek)), (lengte / 2, (0, 0, hoek)), (lengte, (0, 0, -hoek))])}
        b["belletjes"] = {"rotation": kf([(0, (hoek, 0, 0)), (lengte / 2, (-hoek, 0, 0)), (lengte, (hoek, 0, 0))])}
        if naam != "idle":
            b["nek"] = {"rotation": kf([(0, (0, 0, 0)), (lengte / 2, (6, 0, 0)), (lengte, (0, 0, 0))])}
    return a


def palet_bultguh(kleur):
    p = dict(PALET_BULTGUH)
    basis = PALET_BULTGUH["vacht"][0]
    for rol in BULTGUH_ROLLEN:
        oud = PALET_BULTGUH[rol]
        f = sum(oud[0]) / max(1, sum(basis))
        p[rol] = (TR._schaal(kleur, f), oud[1], TR._schaal(TR._mix(kleur, (255, 255, 255), 0.25), f) if oud[2] else None)
    for rol in ("buik", "snuit"):
        p[rol] = (TR._mix(kleur, (255, 255, 255), 0.7), None, None)
    return p


def tuig_wormelien():
    """The Wormelien saddle blanket (zone-6 tack): a pink blanket with a sand-gold fringe and a little worm-guh face patch."""
    b = TR._blok
    e = [b((1.5, 8, 2), (14.5, 8.7, 14), "roze2"), b((1.5, 8.05, 1.8), (14.5, 8.75, 2.2), "goud"), b((1.5, 8.05, 13.8), (14.5, 8.75, 14.2), "goud")]
    for x0 in (1.0, 14.5):
        e.append(b((x0, 3.4, 2.5), (x0 + 0.5, 8.7, 13.5), "roze2"))
        for z in range(3, 14, 2):
            e.append(b((x0 - 0.05, 2.0, z), (x0 + 0.55, 3.4, z + 0.8), "geel"))
    for x, z in ((6, 6), (9, 6), (7.5, 9)):
        e.append(b((x, 8.7, z), (x + 1, 8.8, z + 1), "wit"))
    return e


def tuig_model_wormelien():
    els = tuig_wormelien()
    return {"textures": {"t": f"guhs:item/{TR.ITEM_TEX}/tuig", "particle": f"guhs:item/{TR.ITEM_TEX}/tuig"}, "elements": els,
            "display": {"fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
                        "gui": {"rotation": [25, -135, 0], "translation": [0, 0, 0], "scale": [0.6] * 3},
                        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.4] * 3},
                        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375] * 3},
                        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4] * 3}}}


# =====================================================================================================================
# gear
# =====================================================================================================================
# id: (slot kind, name NL, name EN, colour, accent)
GEAR = {
    "duinwandelaarstulband": ("hoofd", "Duinwandelaarstulband", "Dune Walker's Turban", (246, 196, 180), (255, 220, 120)),
    "duinwandelaarsmantel": ("borst", "Duinwandelaarsmantel", "Dune Walker's Mantle", (240, 186, 170), (255, 220, 120)),
    "duinwandelaarsbroek": ("benen", "Duinwandelaarsbroek", "Dune Walker's Trousers", (230, 176, 160), (120, 210, 200)),
    "duinwandelaarslaarzen": ("voeten", "Duinwandelaarslaarzen", "Dune Walker's Boots", (210, 160, 130), (120, 210, 200)),
    "duinwandelaarswanten": ("handschoenen", "Duinwandelaarswanten", "Dune Walker's Mittens", (240, 190, 170), (255, 220, 120)),
    "duinwandelaarscape": ("cape", "Duinwandelaarscape", "Dune Walker's Cape", (250, 200, 186), (120, 210, 200)),
    "zijdetulband": ("hoofd", "Zijdetulband", "Silk Turban", (255, 170, 210), (255, 210, 90)),
    "zijdegewaad": ("borst", "Zijdegewaad", "Silk Robe", (250, 160, 206), (255, 210, 90)),
    "zijdepofbroek": ("benen", "Zijdepofbroek", "Silk Harem Trousers", (240, 150, 200), (140, 230, 220)),
    "zijdeslofjes": ("voeten", "Zijdeslofjes", "Silk Slippers", (255, 180, 216), (255, 210, 90)),
    "zijdehandschoentjes": ("handschoenen", "Zijdehandschoentjes", "Silk Gloves", (255, 190, 220), (140, 230, 220)),
    "zijdesluier": ("cape", "Zijdesluier", "Silk Veil", (255, 200, 226), (255, 210, 90)),
    "karavaanwachtwanten": ("handschoenen", "Karavaanwachtwanten", "Caravan Guard Gloves", (200, 90, 90), (230, 170, 80)),
    "karavaanwachtcape": ("cape", "Karavaanwachtcape", "Caravan Guard Cape", (220, 100, 110), (250, 220, 150)),
    "karavaanwachtarmband": ("armband", "Karavaanwachtarmband", "Caravan Guard Bracelet", (230, 170, 80), (220, 100, 110)),
    "karavaanwachtlaarzen": ("voeten", "Karavaanwachtlaarzen", "Caravan Guard Boots", (190, 110, 90), (230, 170, 80)),
    "sterrenwachtkap": ("hoofd", "Sterrenwachtkap", "Observatory Hood", (90, 80, 160), (240, 200, 100)),
    "sterrenkaartmantel": ("borst", "Sterrenkaartmantel", "Star Map Mantle", (70, 70, 150), (240, 200, 100)),
    "sterrenwachtbroek": ("benen", "Sterrenwachtbroek", "Observatory Trousers", (80, 76, 150), (255, 240, 180)),
    "sterrenwachtlaarzen": ("voeten", "Sterrenwachtlaarzen", "Observatory Boots", (200, 160, 80), (90, 80, 160)),
    "messinghandschoenen": ("handschoenen", "Messing handschoenen", "Brass Gloves", (222, 176, 92), (90, 80, 160)),
    "sterrenkaartcape": ("cape", "Sterrenkaartcape", "Star Map Cape", (60, 60, 140), (255, 240, 180)),
    "saffraanring": ("ring", "Saffraanring", "Saffron Ring", (255, 200, 70), (255, 140, 60)),
    "woestijnroosring": ("ring", "Woestijnroosring", "Desert Rose Ring", (240, 200, 160), (255, 150, 190)),
    "rozenketting": ("ketting", "Rozenketting", "Rose Necklace", (255, 190, 210), (255, 110, 160)),
    "sterrenoorbel": ("oorbel", "Sterrenoorbel", "Star Earring", (255, 240, 180), (140, 200, 255)),
    "zoutkristalarmband": ("armband", "Zoutkristalarmband", "Salt Crystal Bracelet", (250, 220, 236), (200, 190, 230)),
}
SETNAAM = {"duinwandelaar": ("Duinwandelaarset", "Dune Walker Set"), "duinzijde": ("Duinzijdeset", "Dune Silk Set"),
           "karavaanwacht": ("Karavaanwachtset", "Caravan Guard Set"), "sterrenwacht": ("Sterrenwachtset", "Observatory Set")}
FLAVOUR = {
    "duinwandelaarstulband": ["Met twee gaatjes voor guh-oortjes.", "Houdt de zon uit je ogen en het zand uit je haar."],
    "duinwandelaarsmantel": ["Gewikkeld als een karavaanguh.", "Er zit nog een beetje roze zand in de plooien."],
    "duinwandelaarsbroek": ["Ruim genoeg voor een bultguh-rit.", "Knispert zacht als je loopt."],
    "duinwandelaarslaarzen": ["Zand erin, zand eruit. Zo gaat dat.", "Voor lange tochten over roze duinen."],
    "duinwandelaarswanten": ["Warm overdag, koel 's nachts. Hoe dan?", "Getest op hete karavaanpannetjes."],
    "duinwandelaarscape": ["Wappert als een karavaanvlag.", "Handig als zonnescherm bij een picknick."],
    "zijdetulband": ["Gewikkeld door Tapijtweefster Knoopje.", "Er zit een saffraandraadje doorheen."],
    "zijdegewaad": ["Zacht als een wolkje, roze als een duin.", "Zwiert als je een spreuk zegt."],
    "zijdepofbroek": ["Pof! Pof! Pof!", "Lekker ruim voor een buiging voor de sultane."],
    "zijdeslofjes": ["Met krulletjes aan de punt.", "Ze maken geen geluid op de bazaar."],
    "zijdehandschoentjes": ["Voor spreuken met een zacht randje.", "Er zijn kleine sterretjes op geborduurd."],
    "zijdesluier": ["Fladdert als je toveren gaat.", "Zie je het? Een heel klein gouden randje."],
    "karavaanwachtwanten": ["Gestreepte zadelstof, koperen gespen.", "Hebben al duizend bultguhs geaaid."],
    "karavaanwachtcape": ["De kleuren van de oude karavaanwacht.", "Ruikt naar kampvuur en kardemom."],
    "karavaanwachtarmband": ["Koper met een rood draadje.", "Rinkelt als bultguh-belletjes."],
    "karavaanwachtlaarzen": ["Voor wie nooit verdwaalt.", "Met een kompasje in de hak. Bijna."],
    "sterrenwachtkap": ["Een oude kap van de Sterrenguhs.", "Er staan sterrenbeelden op de binnenkant."],
    "sterrenkaartmantel": ["Een sterrenkaart om je schouders.", "Kijk 's nachts maar: hij glinstert mee."],
    "sterrenwachtbroek": ["Messing ringetjes langs de naad.", "Voor lange nachten achter de kijker."],
    "sterrenwachtlaarzen": ["Ze tikken als een klokje.", "Gemaakt voor de wenteltrap van de sterrenwacht."],
    "messinghandschoenen": ["Glad messing, warme vingers.", "Perfect voor het draaien van telescopen."],
    "sterrenkaartcape": ["De Moederoase staat er ook op. Heel klein.", "Een kaart die je nooit hoeft te vouwen."],
    "saffraanring": ["Saffraangoud uit de bazaar.", "Ruikt een heel klein beetje naar rijst."],
    "woestijnroosring": ["Een woestijnroos van roze zand, hard als steen.", "Past aan elke vinger. Ook aan je tweede ringvinger."],
    "rozenketting": ["Rozenwater-kraaltjes.", "Een cadeautje van Kloofmoeder Roosje."],
    "sterrenoorbel": ["Een vallend sterretje, gevangen in zilver.", "Twinkelt als je knipoogt."],
    "zoutkristalarmband": ["Elk kristalletje zingt een ander toontje.", "Uit de Zingende Zoutgrot."],
}
SPEC = {"schildridder": "Schildridder (Ridder)", "reuzenkussen": "Reuzenkussen (Ridder)", "scherpschutter": "Scherpschutter (Jager)",
        "strikkenzetter": "Strikkenzetter (Jager)", "sterrenwever": "Sterrenwever (Magier)", "zegenzanger": "Zegenzanger (Magier)"}
SPEC_EN = {"schildridder": "Shield Knight (Knight)", "reuzenkussen": "Giant Pillow (Knight)", "scherpschutter": "Sharpshooter (Hunter)",
           "strikkenzetter": "Snare Setter (Hunter)", "sterrenwever": "Star Weaver (Mage)", "zegenzanger": "Blessing Singer (Mage)"}


def _saffraantop(d):
    d.disc(11.5, 4.5, 3, (255, 190, 60))
    d.disc(11.5, 4.5, 1.5, (255, 240, 170))


def _fluittop(d):
    d.rect(9, 1, 14, 3, (120, 220, 210))
    for x in (10, 12):
        d.rect(x, 4, x, 4, (40, 60, 80))


def _sterrentop(d):
    d.disc(11.5, 4, 3, (90, 80, 160))
    d.disc(11.5, 4, 1.3, (255, 246, 190))


WAPENS = {
    "rozenkromzwaard": ("Rozenkromzwaard", "Rose Scimitar", lambda: zwaard((255, 170, 200), (255, 210, 90)),
                        ["Krom als een duin, scherp als een cactus.", "Voor de ridder die een schild én een glimlach draagt."]),
    "dadelpalmknots": ("Dadelpalmknots", "Date Palm Club", lambda: knots((150, 100, 60), (180, 120, 70)),
                       ["Een hele palmstam. Bijna.", "Er vallen soms dadels uit. Lekker na een gevecht."]),
    "duinvalkboog": ("Duinvalkboog", "Dune Falcon Bow", lambda: boog((230, 190, 150), (255, 140, 180)),
                     ["Voor schoten tot over de volgende duin.", "Een veertje van een gierguh aan de pees."]),
    "karavaankruisboog": ("Karavaankruisboog", "Caravan Crossbow", lambda: kruisboog((150, 90, 70), (230, 170, 80)),
                          ["Klik, klik, klik: zo snel als een karavaanbel.", "Gemaakt voor strikjes en valletjes."]),
    "saffraanstaf": ("Saffraanstaf", "Saffron Staff", lambda: staf(_saffraantop), ["Een bolletje saffraanvuur op een stokje.",
                                                                                   "Laat sterren dansen op je wil."]),
    "oasefluit": ("Oasefluit", "Oasis Flute", lambda: staf(_fluittop), ["Speel een liedje en iedereen voelt zich beter.",
                                                                        "Gesneden uit oaseriet. Klinkt als stromend water."]),
}
KRISTALLEN = {
    "rozenkwarts": ("Rozenkwarts", "Rose Quartz", 0xFFB8D0, "Roze en warm. Je klappen worden vaker kritiek."),
    "sterrenzout": ("Sterrenzout", "Star Salt", 0xE8E4F4, "Gehard sterrenlicht uit de Zoutvlakte. Meer schade tegen bazen."),
}
BAAS_ITEMS = {
    # id: (name NL, name EN, flavour, effect, icon, handheld)
    "wormelientand": ("Wormelientand", "Wormelien's Tooth", "Ze gaf hem zelf. Hij zat los.", "elke 4e klap doet 40%% meer schade",
                      lambda: knots((255, 220, 230), (250, 180, 196)), True),
    "duinzwemmerslaarzen": ("Duinzwemmerslaarzen", "Dune Swimmer Boots", "Je glijdt door het zand als een worm. Een lieve worm.",
                            "op zand loop je 15%% sneller", lambda: vw.icoon("voeten", (250, 180, 196), (255, 220, 120), None, 6801), False),
    "wimperwaaier": ("Wimperwaaier", "Lash Fan", "Een van Wormeliens wimpers. Ze heeft er genoeg.",
                     "in Het Zijdeland krijg je 8%% minder schade", lambda: z2.sieraad_icoon("oorbel", (255, 190, 210), (70, 40, 60), 6802), False),
    "zingend_zoutzwaard": ("Zingend Zoutzwaard", "Singing Salt Sword", "Als je zwaait, zingt het een hoge noot.",
                           "+20%% schade tegen constructen (zout, zand, messing)", lambda: zwaard((240, 236, 250), (255, 190, 220)), True),
    "zoutsplinterboog": ("Zoutsplinterboog", "Salt Splinter Bow", "Gesneden uit een splinter van de Zoutkolos.",
                         "+15%% schade op vijanden verder dan 10 blokken", lambda: boog((236, 232, 246), (255, 190, 220)), True),
    "zoutkristalkroon": ("Zoutkristalkroon", "Salt Crystal Crown", "Een kroontje dat de Zoutkolos nooit nodig had.",
                         "onder 30%% Vads een zoutschild: Knuffelschild 15%% (45 s afkoelen)", lambda: vw.icoon("hoofd", (240, 236, 250), (255, 190, 220), None, 6803), False),
    "astrolabiumring": ("Astrolabiumring", "Astrolabe Ring", "Hij draait als je niet kijkt.", "elke 11 s doet je volgende klap 30%% meer",
                        lambda: z2.sieraad_icoon("ring", (222, 176, 92), (140, 230, 226), 6804), False),
    "glimmertjes_dover": ("Glimmertjes Dover", "Glimmertje's Snuffer", "Hij dooft geen lampjes meer. Alleen nog vijanden.",
                          "je klappen maken vijanden soms duizelig (10%%, 1 s)", lambda: staf(lambda d: (d.rect(9, 2, 14, 6, (170, 170, 186)),
                                                                                                          d.rect(10, 1, 13, 1, (200, 200, 214)))), True),
    "sterrenkijkerkruisboog": ("Sterrenkijkerkruisboog", "Stargazer Crossbow", "Met een kijkertje erop. Je mist 's nachts nooit meer.",
                               "'s nachts +20%% schade", lambda: kruisboog((222, 176, 92), (90, 80, 160)), True),
    "sterrenhartamulet": ("Sterrenhartamulet", "Star Heart Amulet", "Een stukje van het hart van de Verzonken Sterrenguh. Het gloeit.",
                          "'s nachts geef je zelf licht en doen je elementen 6%% meer", lambda: z2.sieraad_icoon("ketting", (255, 236, 170), (90, 80, 160), 6805), False),
}
BAZEN = {
    # id: (name NL, name EN, subtitle NL, subtitle EN, tip NL)
    "wormelien": ("Koningin Wormelien", "Queen Wormelien", "Ze zwemt grauw en hongerig door het zand", "She swims through the sand, grey and hungry",
                  "Blijf bewegen als ze onderduikt (stilstaan = zandfontein). Roze plekken = schuilen achter een duin. Fase 3: klop de grauwe zoutkorst van haar rug, spring vanaf een duin of schiet hem eraf."),
    "zoutkolos": ("De Zoutkolos", "The Salt Colossus", "Gemaakt van samengekoekt sterrenzout", "Made of caked star salt",
                  "Rode barstlijnen = zoutgolven. Hoor je een hoge noot? Stap uit de paarse ring rond een kristal, of sla het stuk. Fase 3: kijk recht naar zijn gloeiende hart voor dubbele schade."),
    "astrolabiumwachter": ("De Astrolabiumwachter", "The Astrolabe Warden", "De bewaker van de Verzonken Sterrenwacht", "Warden of the Sunken Observatory",
                           "Sla vooral als de ringen in één lijn staan (gouden licht): dan komt je klap helemaal door. Bij een ringzwaai: sta in een roze opening."),
    "glimmertje": ("Kootje Glimmertje", "Kootje Glimmertje", "Ploegbaas van de Lampendovers", "Foreman of the Lamp Snuffers",
                   "Hij dooft de vuurschalen. Rechtsklik een donkere schaal 2x om hem weer aan te steken. Alle vier uit? Dan verstopt hij zich in het donker."),
    "sterrenguh": ("De Verzonken Sterrenguh", "The Sunken Star Guh", "Een oude reus van sterrensteen, grauw geworden", "An old giant of star stone, gone grey",
                   "Rechtsklik de vier telescopen tot ze allemaal op zijn hart gericht staan (gouden straal). Dan gloeit hij: dubbele schade!"),
}
FASES = {"wormelien": {2: "Duinspuug!", 3: "Helemaal boven!"}, "zoutkolos": {2: "Zingende kristallen!", 3: "Gloeiend hart!"},
         "astrolabiumwachter": {2: "Nog een ring!", 3: "Nog een ring!", 4: "Alle vier de ringen!"}, "glimmertje": {2: "Fluiten!"},
         "sterrenguh": {2: "Wanhoop", 3: "Sterrengruis!"}}
DOELEN = {"zoutkorst": ("Zoutkorst", "Salt Crust"), "zingkristal": ("Zingend kristal", "Singing Crystal"), "vuurschaal": ("Vuurschaal", "Fire Bowl"),
          "telescoop": ("Telescoop", "Telescope")}
HUISDIEREN = {
    "fennekje": ("Fennekje", "Een fennekguhtje met oren groter dan zijn hoofd. Hij hoort alles.", "Zeldzaam: vrolijk een Fennekguh op in De Roze Duinen."),
    "springpiepje": ("Springpiepje", "Boing! Boing! Hij stuitert overal achter je aan.", "Zeldzaam: vrolijk een Springpiep op (of een zijquest in Rozenkloof)."),
    "sterretje": ("Sterretje", "Een opgevrolijkt sterretje. 's Nachts gloeit het een beetje.", "Zeldzaam: vrolijk een Gedoofd sterretje op bij De Moederoase."),
    "dadelaapje": ("Dadelaapje", "Pikt geen munten meer. Alleen nog dadels. Soms.", "Zijquest 'De Dadelapenbende' (of heel zeldzaam van een Dadelaapje)."),
    "bobbeltje": ("Bobbeltje", "Het babybultguhtje van Oma Dadel. Hij wil met je mee!", "Het einde van De Sterrenkaravaan (de Moederoase)."),
    "wormelientje": ("Wormelientje", "Een piepklein roze zandwormpje met wimpers. Slaapt graag in je zak.", "Vrolijk Koningin Wormelien op (eerste keer)."),
    "zoutklontje": ("Zoutklontje", "Een klontje sterrenzout dat bleef liggen toen de Zoutkolos uit elkaar viel. Het piept.", "Versla De Zoutkolos (eerste keer)."),
    "sterrenguhtje": ("Sterrenguhtje", "Een mini Sterrenguh. Hij wijst altijd naar de Moederoase.", "Vrolijk De Verzonken Sterrenguh op (eerste keer)."),
}
ENTITEITEN = {
    "zandvlooi": ("Zandvlooi", "Sand Flea"), "fennekguh": ("Fennekguh", "Fennec Guh"), "springpiep": ("Springpiep", "Jerboa Squeak"),
    "duinkruiper": ("Duinkruiper", "Dune Creeper"), "dadelaapje": ("Dadelaapje", "Date Monkey"), "gierguh": ("Gierguh", "Vulture Guh"),
    "spiegelslang": ("Spiegelslang", "Mirage Snake"), "zoutkreeft": ("Zoutkreeft", "Salt Crayfish"), "zandgeestje": ("Zandgeestje", "Dust Sprite"),
    "ruinewachter": ("Ruïnewachter", "Ruin Warden"), "mika_lampendover": ("Mika-Lampendover", "Mika Lamp Snuffer"),
    "mika_ringdrager": ("Mika-Ringdrager", "Mika Ring Bearer"), "gedoofd_sterretje": ("Gedoofd sterretje", "Snuffed Little Star"),
    "zoutslang": ("De Zoutslang", "The Salt Serpent"),
}
TIPS = {
    "zandvlooi": "Ze zitten verstopt in het zand (alleen twee voelsprietjes). Kom je dichtbij, dan springen ze eruit!",
    "fennekguh": "Hij hoort je van ver en roept zijn maatje. Rode streep = zijn sprong: stap opzij.",
    "springpiep": "Rode cirkel = hij schopt zand: weg ermee, anders zie je even niks. Daarna stuitert hij weg en weer terug.",
    "duinkruiper": "Rode kegel voor hem = zijn staart prikt. Zijn gif is plakkerig. Loop om hem heen!",
    "dadelaapje": "Hij gooit dadels (rode cirkel) en pikt soms munten. Vrolijk hem op en je krijgt alles terug.",
    "gierguh": "Hij duikt op wie het LAAGST staat. Rode streep = duik. Daarna zit hij even op de grond: je kans!",
    "spiegelslang": "Een luchtspiegeling: er zijn er drie, maar maar één is echt. Een kopie spat uit elkaar als je hem raakt.",
    "zoutkreeft": "Zijn zoutkorst vangt klappen van voren. Van ACHTEREN is hij zacht: loop om hem heen!",
    "zandgeestje": "Alleen 's nachts. Paarse cirkel = hij zuigt je naar zich toe. Stap eruit!",
    "ruinewachter": "Een stil standbeeld... tot je dichtbij komt. Grote rode cirkel = zijn stamp: neem afstand.",
    "mika_lampendover": "Rode kegel = zijn reuzendover. Hij maakt het donker en dooft je Vahoeg en Snelpootjes.",
    "mika_ringdrager": "Paarse cirkel = hij opent een lichtvangpot. Loop uit de cirkel, dan breekt de straal.",
    "gedoofd_sterretje": "Drie rode cirkeltjes = sterrengruis. Vrolijk het op: soms wil het met je mee!",
    "zoutslang": "Ze zwemt door de rotsen en komt af op GELUID: rennen en slaan. Rode streep = daar komt ze uit!",
}
TIP_TITELS = {k: ENTITEITEN[k][0] for k in TIPS}
GEBIEDEN = {}   # (the area names are u-z6wereld's)
LEIDERS = {"oorschelp": ("Oude Oorschelp", "Old Earshell"), "pitje": ("Dadelbaas Pitje", "Date Boss Pitje"),
           "korstje": ("Kapitein Korstje", "Captain Crusty"), "oudstewachter": ("De Oudste Wachter", "The Eldest Warden"),
           "brie": ("Ringdrager Brie", "Ring Bearer Brie")}
VFX_AANVALLEN = {"duinspuug": ("Duinspuug", "Dune Spit"), "zandgolf": ("Zandgolf", "Sand Wave"), "zoutgolf": ("Zoutgolven", "Salt Waves"),
                 "ringzwaai": ("Ringzwaai", "Ring Sweep"), "doverklap": ("Doverklap", "Snuffer Slam"), "wanhoopsgolf": ("Wanhoopsgolf", "Wave of Despair")}

Z = "vadsrijk.z6wezens."
TEKSTEN = {
    Z + "mika.weg.0": ("Dit komt in mijn rapport!", "This goes in my report!"),
    Z + "mika.weg.1": ("De Ringvinger hoort hiervan!", "The Ring Finger will hear of this!"),
    Z + "mika.weg.2": ("Mijn dover! Mijn mooie dover!", "My snuffer! My beautiful snuffer!"),
    Z + "mika.weg.3": ("Terug naar de toren! TERUG!", "Back to the tower! BACK!"),
    Z + "dadelaapje.pikt": ("Het dadelaapje pikt %s brons!", "The date monkey pinches %s bronze!"),
    Z + "dadelaapje.terug": ("Het opgevrolijkte dadelaapje geeft je %s brons terug. Sorry!", "The cheered-up date monkey gives back %s bronze. Sorry!"),
    Z + "spiegelslang.kopie": ("Een luchtspiegeling!", "A mirage!"),
    Z + "zoutkreeft.korst": ("Zoutkorst!", "Salt crust!"),
    Z + "zoutkreeft.zwak": ("Zwakke plek!", "Weak spot!"),
    Z + "zoutslang.onder": ("Ze zwemt door de rots... wacht tot ze bovenkomt!", "She swims through the rock... wait until she comes up!"),
    Z + "ruinewachter.wakker": ("De wachter wordt wakker!", "The warden wakes up!"),
    Z + "lampendover.gedoofd": ("Je lichtje is gedoofd!", "Your little light is snuffed!"),
    Z + "ringdrager.los": ("De lichtstraal breekt!", "The light beam breaks!"),
    Z + "huisdier.gevonden": ("✦ Een nieuw huisdier wil met je mee: %s! (druk P)", "✦ A new pet wants to come along: %s! (press P)"),
    Z + "set.stukken": ("Set: %s (%s/%s gedragen)", "Set: %s (%s/%s worn)"),
    Z + "set.duinwandelaar.2": ("(2) Duinhuid: Verdediging +14 en Vads +80", "(2) Dune Hide: Defence +14 and Vads +80"),
    Z + "set.duinwandelaar.4": ("(4) Karavaanmoed: Schade tegen bazen +8%%", "(4) Caravan Courage: Damage to bosses +8%%"),
    Z + "set.duinwandelaar.6": ("(6) Duinloper: Loopsnelheid +8%%", "(6) Dune Runner: Movement speed +8%%"),
    Z + "set.duinzijde.2": ("(2) Zijdeglans: Wijsheid +10", "(2) Silk Sheen: Wisdom +10"),
    Z + "set.duinzijde.4": ("(4) Oasewater: Heal-kracht +12", "(4) Oasis Water: Heal power +12"),
    Z + "set.duinzijde.6": ("(6) Saffraanvuur: Elementschade +6%%", "(6) Saffron Fire: Element damage +6%%"),
    Z + "set.karavaanwacht.2": ("(2) Karavaanwacht: Vads +120", "(2) Caravan Guard: Vads +120"),
    Z + "set.karavaanwacht.4": ("(4) Wachtplicht: Schade tegen bazen +10%%", "(4) Guard Duty: Damage to bosses +10%%"),
    Z + "set.sterrenwacht.2": ("(2) Sterrenblik: Kritiek +4%%", "(2) Star Gaze: Critical +4%%"),
    Z + "set.sterrenwacht.4": ("(4) Sterrenkaart: Cooldownreductie +6%%", "(4) Star Map: Cooldown reduction +6%%"),
    Z + "set.sterrenwacht.6": ("(6) Sterretje: elke 20 s doet je volgende klap +40%% schade", "(6) Little Star: every 20 s your next hit deals +40%% damage"),
    Z + "set.sterretje": ("Sterretje!", "Little star!"),
    Z + "spec": ("Gemaakt voor: %s", "Made for: %s"),
    Z + "loot.guhddelijk": ("✦ %s vond iets GUHDDELIJKS: %s!", "✦ %s found something GUHDLY: %s!"),
    Z + "luik.in": ("Je klimt door het luik naar beneden... De Verzonken Sterrenwacht.", "You climb down through the hatch... The Sunken Observatory."),
    Z + "luik.uit": ("Je klimt terug naar boven, het Ruïnedal in.", "You climb back up into the Ruin Valley."),
    Z + "luik.hint.in": ("Een oud luik in de koepel (rechtsklik): naar De Verzonken Sterrenwacht", "An old hatch in the dome (right-click): to the Sunken Observatory"),
    Z + "luik.hint.uit": ("De ladder naar boven (rechtsklik)", "The ladder up (right-click)"),
    # Wormelien
    Z + "wm.fase2": ("DUINSPUUG", "DUNE SPIT"), Z + "wm.fase2.onder": ("Schuil op de roze plekken!", "Shelter on the pink spots!"),
    Z + "wm.fase3": ("HELEMAAL BOVEN!", "ALL THE WAY UP!"), Z + "wm.fase3.onder": ("Klop de grauwe zoutkorst van haar rug!", "Knock the grey salt crust off her back!"),
    Z + "wm.duikt": ("Ze duikt onder! Blijf bewegen, anders krijg je een zandfontein!", "She dives! Keep moving, or you get a sand fountain!"),
    Z + "wm.onder": ("Ze zit onder het zand! Wacht tot ze bovenkomt.", "She is under the sand! Wait until she comes up."),
    Z + "wm.schuil": ("Ze slokt zand op! Schuil op een roze plek!", "She gulps sand! Shelter on a pink spot!"),
    Z + "wm.korst": ("Een grauwe zoutkorst op haar rug! Spring er vanaf een duin tegenaan, of schiet hem eraf.", "A grey salt crust on her back! Jump at it from a dune, or shoot it off."),
    Z + "wm.korst_eerst": ("De zoutkorst beschermt haar! Klop eerst de korst eraf.", "The salt crust protects her! Knock the crust off first."),
    Z + "wm.korst_weg": ("De korst is eraf! Ze is even beduusd: nu raken!", "The crust is off! She is dazed for a moment: hit her now!"),
    Z + "wm.blij": ("Koningin Wormelien is weer roze... ze gaapt, en valt in slaap in het warme zand.", "Queen Wormelien is pink again... she yawns, and falls asleep in the warm sand."),
    # Zoutkolos
    Z + "zk.fase2": ("ZINGENDE KRISTALLEN", "SINGING CRYSTALS"), Z + "zk.fase2.onder": ("Hoge noot = paarse ring: stap eruit!", "High note = purple ring: step out!"),
    Z + "zk.fase3": ("GLOEIEND HART", "GLOWING HEART"), Z + "zk.fase3.onder": ("Kijk recht naar zijn hart: dubbele schade!", "Look straight at his heart: double damage!"),
    Z + "zk.kristallen": ("Hij gooit zingende kristallen! Luister naar de hoge noot.", "He throws singing crystals! Listen for the high note."),
    Z + "zk.gloeit": ("Zijn hart gloeit! Kijk ernaar terwijl je slaat (met de Sterrenlens nog beter).", "His heart glows! Look at it while you hit (even better with the Star Lens)."),
    Z + "zk.hart": ("Hart geraakt!", "Heart hit!"),
    Z + "zk.weg": ("De Zoutkolos valt uiteen in glinsterend zout.", "The Salt Colossus falls apart in glittering salt."),
    # Astrolabiumwachter
    Z + "aw.fase": ("NOG EEN RING", "ANOTHER RING"), Z + "aw.fase.onder": ("Sta in de roze openingen als de ringen zwaaien", "Stand in the pink gaps when the rings sweep"),
    Z + "aw.ring_erbij": ("Er draait nu %s ring(en)!", "%s ring(s) turning now!"),
    Z + "aw.nu": ("De ringen staan in één lijn: NU!", "The rings are aligned: NOW!"),
    Z + "aw.kaatst": ("De ringen kaatsen je klap af! Wacht op het gouden licht.", "The rings deflect your hit! Wait for the golden light."),
    # Glimmertje
    Z + "gl.fase2": ("FLUITEN!", "WHISTLE!"), Z + "gl.fase2.onder": ("Twee schalen gaan uit, en er komen Lampendovers", "Two bowls go out, and Lamp Snuffers come"),
    Z + "gl.gedoofd": ("Gedoofd! (%s van de 4 uit)", "Snuffed! (%s of 4 out)"),
    Z + "gl.aan": ("De schaal brandt weer! (%s van de 4 uit)", "The bowl burns again! (%s of 4 out)"),
    Z + "gl.blaas": ("Blazen... nog één keer (rechtsklik)!", "Blowing... once more (right-click)!"),
    Z + "gl.donker": ("Hij verstopt zich in het donker! Steek de vuurschalen aan.", "He hides in the dark! Light the fire bowls."),
    Z + "gl.weg": ("Glimmertje vlucht in een rookwolk. 'Mijn dover! De Ringvinger hoort hiervan!'", "Glimmertje flees in a cloud of smoke. 'My snuffer! The Ring Finger will hear of this!'"),
    # Sterrenguh
    Z + "sg.fase2": ("WANHOOP", "DESPAIR"), Z + "sg.fase2.onder": ("Een rode ring van hem af: kom dichtbij!", "A red ring from him outward: come close!"),
    Z + "sg.fase3": ("STERRENGRUIS", "STAR NOISE"), Z + "sg.fase3.onder": ("Hij strooit steeds meer sterrengruis", "He scatters more and more star noise"),
    Z + "sg.hart": ("ZIJN HART GLOEIT", "HIS HEART GLOWS"), Z + "sg.hart.onder": ("Alle bundels op zijn hart: dubbele schade!", "All beams on his heart: double damage!"),
    Z + "sg.telescoop": ("Telescoop gedraaid (%s van de 4 gericht)", "Telescope turned (%s of 4 aimed)"),
    Z + "sg.telescoop_goed": ("Deze staat goed! (%s van de 4 gericht)", "This one is right! (%s of 4 aimed)"),
    Z + "sg.schok": ("Sterrenschok! Blijf dicht bij hem!", "Star shock! Stay close to him!"),
    Z + "sg.wanhoop2": ("Waarom... is het... zo donker...", "Why... is it... so dark..."),
    Z + "sg.wanhoop3": ("De sterren... ik zie ze niet meer...", "The stars... I can't see them any more..."),
    Z + "sg.blij": ("De Verzonken Sterrenguh gloeit weer! 'Dank je, kleine reiziger. Hier: het Sterrenkompas.'", "The Sunken Star Guh glows again! 'Thank you, little traveller. Here: the Star Compass.'"),
    Z + "sg.kompas": ("✦ Je hebt het Sterrenkompas!", "✦ You have the Star Compass!"),
    # mount + tack
    "vadsrijk.rijdier.tandem.erbij": ("%s zit achterop!", "%s rides along behind you!"),
    "vadsrijk.rijdier.tandem.achterop": ("Je zit achterop. Alleen de eigenaar stuurt. (Shift = afstappen)", "You ride along behind. Only the owner steers. (Shift = get off)"),
}


def teksten():
    """{key: (nl, en)}"""
    t = dict(TEKSTEN)
    for k, (nl, en) in ENTITEITEN.items():
        t[f"entity.guhs.vr_z6_mob_{k}"] = (nl, en)
    for k, tip in TIPS.items():
        t[Z + f"tip.{k}.titel"] = (TIP_TITELS[k], ENTITEITEN[k][1])
        t[Z + f"tip.{k}"] = (tip, tip)
    for k, (nl, en) in LEIDERS.items():
        t[Z + f"leider.{k}"] = (nl, en)
    for s, (nl, en) in SETNAAM.items():
        t[Z + f"set.{s}"] = (nl, en)
    for k, (soort, nl, en, kleur, accent) in GEAR.items():
        t[f"item.guhs.vr_z6_mob_{k}"] = (nl, en)
        t[f"vadsrijk.loot.{k}"] = (nl, en)
    for k, regels in FLAVOUR.items():
        for i, r in enumerate(regels):
            t[f"vadsrijk.loot.{k}.flavour.{i}"] = ("\"" + r + "\"",) * 2
    for k, (nl, en, _i, fl) in WAPENS.items():
        t[f"item.guhs.vr_z6_{k}"] = (nl, en)
        t[f"vadsrijk.loot.{k}"] = (nl, en)
        for i, r in enumerate(fl):
            t[f"vadsrijk.loot.{k}.flavour.{i}"] = ("\"" + r + "\"",) * 2
    for k, nl in SPEC.items():
        t[Z + f"spec.{k}"] = (nl, SPEC_EN[k])
    for k, (nl, en, _kleur, fl) in KRISTALLEN.items():
        t[f"item.guhs.vr_z6_kristal_{k}"] = (nl, en)
        t[f"item.guhs.vr_z6_kristal_{k}.flavour"] = (fl, fl)
    for k, (nl, en, fl, effect, _i, _h) in BAAS_ITEMS.items():
        t[f"item.guhs.vr_z6_baas_{k}"] = (nl, en)
        t[f"vadsrijk.loot.z6baas_{k}"] = (nl, en)
        t[f"vadsrijk.loot.z6baas_{k}.flavour.0"] = ("\"" + fl + "\"",) * 2
        t[f"vadsrijk.bazen.effect.z6baas_{k}"] = (effect, effect)
    for b, (nl, en, onder, onder_en, tip) in BAZEN.items():
        t[f"entity.guhs.vr_z6_baas_{b}"] = (nl, en)
        t[Z + f"baas.{b}"] = (nl, en)
        t[Z + f"baas.{b}.onder"] = (onder, onder_en)
        t[Z + f"tip.baas_{b}.titel"] = (nl, en)
        t[Z + f"tip.baas_{b}"] = (tip, tip)
        for zone in ("z2bazen", "z3bazen"):   # (the zone-2/3 engine shows these too)
            t[f"vadsrijk.{zone}.tip.{b}.titel"] = (nl, en)
            t[f"vadsrijk.{zone}.tip.{b}"] = (tip, tip)
        for f, regel in FASES.get(b, {}).items():
            t[f"vadsrijk.kern.beleving.fase.vr_z6_baas_{b}.{f}"] = (f"Fase {f}: {regel}",) * 2
    for b in ("wormelien", "zoutkolos", "sterrenguh"):
        nl, en = BAZEN[b][0], BAZEN[b][1]
        t[f"item.guhs.vr_z6_baas_trofee_{b}"] = ("Trofee: " + nl, "Trophy: " + en)
    for k, (nl, en) in DOELEN.items():
        t[f"entity.guhs.vr_z6_baas_{k}"] = (nl, en)
    for k, (nl, en) in VFX_AANVALLEN.items():
        t[f"vadsrijk.vfx.aanval.{k}"] = (nl, en)
    for h, (naam, tekst, bron) in HUISDIEREN.items():
        t[f"vadsrijk.huisdier.{h}.naam"] = (naam, naam)
        t[f"vadsrijk.huisdier.{h}.tekst"] = (tekst, tekst)
        t[f"vadsrijk.huisdier.{h}.bron"] = (bron, bron)
    t["item.guhs.vr_z6_sterrenstof"] = ("Sterrenstof", "Star Dust")
    t["item.guhs.vr_z6_sterrenkompas"] = ("Het Sterrenkompas", "The Star Compass")
    # the Bultguh + the zone-6 tack
    E, R = "vadsrijk.economie.", "vadsrijk.rijdier."
    t["entity.guhs.vr_eco_bultguh"] = ("Bultguh", "Hump Guh")
    t[E + "rijdier.bultguh"] = ("Bultguh", "Hump Guh")
    t["item.guhs.vr_item_zadel_bultguh"] = ("Zadel van de Bultguh", "Hump Guh Saddle")
    t[E + "rijdier.eigen.bultguh"] = ("Een kameelguh met twee zachte bulten. Op zand loopt hij 30% sneller, en er kan iemand achterop!",
                                      "A camel guh with two soft humps. 30% faster on sand, and someone can ride along behind!")
    t[E + "stal.nog_niet.bultguh"] = ("Help de karavaan van Oma Dadel in Rozenkloof (Stalmeester Hobbel): dan kies je je eigen Bultguh.",
                                      "Help Grandma Date's caravan in Rozenkloof (Stable Master Hobbel): then you pick your own Hump Guh.")
    for v, (nl, en) in {"naturel": ("Zandroze", "Sand Pink"), "saffraan": ("Saffraan", "Saffron"), "wolkwit": ("Wolkwit", "Cloud White"),
                        "kaneel": ("Kaneel", "Cinnamon"), "sterrennacht": ("Sterrennacht", "Starry Night")}.items():
        t[R + f"vacht.bultguh.{v}"] = (nl, en)
    t[R + "bron.api.z6_stal"] = ("Quest: de karavaan van Oma Dadel (Rozenkloof).", "Quest: Grandma Date's caravan (Rozenkloof).")
    t[R + "bron.api.z6_raid_sterrennacht"] = ("Zeldzaam uit de raid De Sterrendovende Toren.", "Rare from the raid The Star-Snuffing Tower.")
    t[R + "tuig.wormelien_zadeldeken"] = ("Wormelien-zadeldeken", "Wormelien Saddle Blanket")
    t["item.guhs.vr_rijtuig_wormelien_zadeldeken"] = ("Wormelien-zadeldeken", "Wormelien Saddle Blanket")
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


def _bewaar(pad, icoon):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    if isinstance(icoon, Image.Image):
        vw._png(pad, icoon)
    elif hasattr(icoon, "png"):
        icoon.png(pad)
    else:
        vw._png(pad, Image.fromarray(np.asarray(icoon).astype(np.uint8)))


def _item(a, w, item, tex, icoon, handheld=False):
    _bewaar(os.path.join(a, "textures", "item", *tex.split("/")) + ".png", icoon)
    parent = "minecraft:item/handheld" if handheld else "minecraft:item/generated"
    w(os.path.join(a, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
    w(os.path.join(a, "models", "item", item + ".json"), {"parent": parent, "textures": {"layer0": "guhs:item/" + tex}})


def _alpha(img):
    return z4._alpha(img)


def _zwart(img):
    """A glowmask with nothing glowing (the AutoGlowing layer needs the file)."""
    a = np.zeros_like(np.asarray(img))
    return Image.fromarray(a)


def _sterrenstof():
    d = Doek()
    for x, y, r in ((5, 6, 1.6), (10, 4, 1.2), (8, 10, 2), (12, 11, 1.1), (4, 12, 1)):
        d.disc(x, y, r, (255, 240, 180))
        d.disc(x, y, r * 0.5, (255, 255, 240))
    d.schaduw()
    return d.rand()


def _kompas():
    d = Doek()
    d.disc(8, 8, 6.5, (222, 176, 92))
    d.disc(8, 8, 5.2, (60, 56, 120))
    d.poly([(8, 3), (9.4, 8), (8, 13), (6.6, 8)], (255, 240, 180))
    d.disc(8, 8, 1, (255, 150, 190))
    d.schaduw()
    return d.rand()


def assets(a, w=vw._schrijf_json):
    geo = os.path.join(a, "geckolib", "models", "entity")
    ani = os.path.join(a, "geckolib", "animations", "entity")
    tex_dir = os.path.join(a, "textures", *SUB.split("/"))
    btex = os.path.join(a, "textures", *BSUB.split("/"))
    n = 0
    modellen = {}
    for i, (naam, maak) in enumerate(WEZENS):
        with z4._v2():
            m, anims, pal, glow = maak()
        anims = z4._naloop(m, z2.leg_anims(m, anims))
        controleer(naam, m, anims, NODIG[naam])
        modellen[naam] = m
        w(os.path.join(geo, *SUB.split("/"), naam + ".geo.json"), m.geo())
        w(os.path.join(ani, *SUB.split("/"), naam + ".animation.json"), anim_bestand(anims))
        if naam in MIKA:
            tex = z2.schilder(m, pal, 6400 + i)
            blij = tex
        elif naam in VERSLAAN:
            tex = vw.schilder(m, pal, "blij", 6400 + i, False)
            blij = tex
        else:
            tex = vw.schilder(m, pal, "blij", 6400 + i, True)            # ontvadst: grey crust, grumpy
            blij = vw.schilder(m, pal, "blij", 6400 + i, False)          # cheered up: bright and happy
        vw._png(os.path.join(tex_dir, naam + ".png"), tex)
        vw._png(os.path.join(tex_dir, naam + "_blij.png"), blij)
        vw._png(os.path.join(tex_dir, naam + "_gloed.png"), vw.gloed(m, glow, 6500 + i))
        vw._png(os.path.join(tex_dir, naam + "_elite.png"), vw.elite_gloed(tex))
        n += 1
    # the pets' shared animation (root only) + their own little models
    huis = {"idle": anim(1.6, {"root": {"position": kf([(0, (0, 0, 0)), (0.8, (0, 0.8, 0)), (1.6, (0, 0, 0))])}}),
            "hop": anim(0.5, {"root": {"position": kf([(0, (0, 0, 0)), (0.25, (0, 3, 0)), (0.5, (0, 0, 0))])}}),
            "sprong": een(0.6, {"root": {"position": kf([(0, (0, 0, 0)), (0.3, (0, 6, 0)), (0.6, (0, 0, 0))]),
                                         "rotation": kf([(0, (0, 0, 0)), (0.6, (0, 360, 0))])}})}
    w(os.path.join(ani, *SUB.split("/"), "huisdier.animation.json"), anim_bestand(huis))
    # bosses + objects
    for naam, maak, nodig, verder in (
            ("wormelien", m_wormelien, ["idle", "walk", "duik", "boven", "slok", "spuug", "slaap"], "roze"),
            ("zoutkolos", m_zoutkolos, ["idle", "walk", "stamp", "gooi", "hart"], "hart"),
            ("astrolabiumwachter", m_astrolabiumwachter, ["idle", "walk", "zwaai", "lijn", "straal"], "goud"),
            ("glimmertje", m_glimmertje, ["idle", "walk", "doof", "gooi", "fluit"], None),
            ("sterrenguh", m_sterrenguh, ["idle", "walk", "strooi", "golf", "hart", "blij"], "roze")):
        with z4._v2():
            m, anims, pal = maak()
        controleer(naam, m, anims, nodig)
        modellen[naam] = m
        w(os.path.join(geo, f"vr_z6_baas_{naam}.geo.json"), m.geo())
        w(os.path.join(ani, f"vr_z6_baas_{naam}.animation.json"), anim_bestand(anims))
        if naam == "glimmertje":
            basis = z2.schilder(m, pal, 6600)
        else:
            basis = vw.schilder(m, pal, "blij", 6600 + len(naam), naam in ("wormelien", "sterrenguh"))
        vw._png(os.path.join(btex, naam + ".png"), basis)
        vw._png(os.path.join(btex, naam + "_glowmask.png"), _alpha(vw.gloed(m, {"glim": (90, 180, 170), "hart": (120, 90, 100)}, 6650)))
        if verder:
            blij = vw.schilder(m, pal, "blij", 6600 + len(naam), False)
            vw._png(os.path.join(btex, naam + "_blij.png"), blij)
            gl = {"roze": {"wimper": (40, 20, 30)}, "hart": {"hart": (255, 200, 230), "kristal": (120, 90, 110)},
                  "goud": {"messing": (200, 150, 60), "ster": (255, 230, 170), "glim": (140, 240, 230)}}[verder]
            if naam == "sterrenguh":
                gl = {"hart": (255, 236, 170), "ster": (255, 246, 200)}
            vw._png(os.path.join(btex, naam + "_blij_glowmask.png"), _alpha(vw.gloed(m, gl, 6660)))
        n += 1
    for naam, maak, gloed in (("zoutkorst", m_zoutkorst, None), ("zingkristal", m_zingkristal, {"kristal": (200, 120, 160)}),
                              ("vuurschaal", m_vuurschaal, {"vuur": (255, 170, 70), "vuur2": (255, 236, 150)}), ("telescoop", m_telescoop, None)):
        with z4._v2():
            m, anims, pal = maak()
        controleer(naam, m, anims, ["idle"])
        w(os.path.join(geo, f"vr_z6_baas_{naam}.geo.json"), m.geo())
        w(os.path.join(ani, f"vr_z6_baas_{naam}.animation.json"), anim_bestand(anims))
        vw._png(os.path.join(btex, naam + ".png"), vw.schilder(m, pal, "blij", 6700 + len(naam), False))
        if gloed:
            vw._png(os.path.join(btex, naam + "_glowmask.png"), _alpha(vw.gloed(m, gloed, 6750)))
    # pets: copies of the boss / mount models under the pet path, in their happy colours
    with z4._v2():
        kl = m_zoutklontje()[0]
    w(os.path.join(geo, *SUB.split("/"), "zoutklontje.geo.json"), kl.geo())
    vw._png(os.path.join(tex_dir, "zoutklontje.png"), vw.schilder(kl, m_zoutklontje()[2], "blij", 6800, False))
    for pet, bron, pal in (("wormelientje", m_wormelien, None), ("sterrenguhtje", m_sterrenguh, None)):
        with z4._v2():
            m, _a, p = bron()
        w(os.path.join(geo, *SUB.split("/"), pet + ".geo.json"), m.geo())
        vw._png(os.path.join(tex_dir, pet + ".png"), vw.schilder(m, p, "blij", 6600 + len(pet), False))
    bb = m_bultguh("bobbeltje")
    w(os.path.join(geo, *SUB.split("/"), "bobbeltje.geo.json"), bb.geo())
    vw._png(os.path.join(tex_dir, "bobbeltje.png"), vw.schilder(bb, PALET_BULTGUH, "blij", 6810, False))
    # ---- the Bultguh mount ----------------------------------------------------------------------------------------------------
    etex = os.path.join(a, "textures", "entity")
    m = m_bultguh()
    an = anims_bultguh()
    controleer("bultguh", m, an, ["idle", "hop", "ren", "sprong"])
    w(os.path.join(geo, "vr_eco_bultguh.geo.json"), m.geo())
    w(os.path.join(ani, "vr_eco_bultguh.animation.json"), anim_bestand(an))
    vw._png(os.path.join(etex, "vr_eco_bultguh.png"), vw.schilder(m, PALET_BULTGUH, "blij", 6900, False))
    for vacht, kleur in BULTGUH_VACHT.items():
        vw._png(os.path.join(etex, f"vr_eco_bultguh_{vacht}.png"), vw.schilder(m, palet_bultguh(kleur), "blij", 6900, False))
    mask = TR.model_masker(m, ("vacht", "oor", "poot"))
    for i, p in enumerate(TR.PATRONEN):
        vw._png(os.path.join(etex, f"vr_rijpatroon_bultguh_{p}.png"), Image.fromarray(TR.patroon(mask, "bultguh", p, 6950 + i)))
    zadel((242, 180, 176), [(5, 6), (10, 5)], (90, 200, 196)).png(os.path.join(a, "textures", "item", RD.ITEM_TEX, "vr_item_zadel_bultguh.png"))
    w(os.path.join(a, "models", "item", "vr_item_zadel_bultguh.json"),
      {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{RD.ITEM_TEX}/vr_item_zadel_bultguh"}})
    w(os.path.join(a, "items", "vr_item_zadel_bultguh.json"), {"model": {"type": "minecraft:model", "model": "guhs:item/vr_item_zadel_bultguh"}})
    w(os.path.join(a, "models", "item", "vr_rijtuig_wormelien_zadeldeken.json"), tuig_model_wormelien())
    w(os.path.join(a, "items", "vr_rijtuig_wormelien_zadeldeken.json"),
      {"model": {"type": "minecraft:model", "model": "guhs:item/vr_rijtuig_wormelien_zadeldeken"}})
    # ---- items ----------------------------------------------------------------------------------------------------------------
    for i, (k, (soort, nl, en, kleur, accent)) in enumerate(GEAR.items()):
        if soort in ("hoofd", "borst", "benen", "voeten"):
            icoon = vw.icoon(soort, kleur, accent, None, 7000 + i)
        elif soort in ("ring", "ketting", "oorbel"):
            icoon = z2.sieraad_icoon(soort, kleur, accent, 7000 + i)
        else:
            icoon = z4.nieuw_icoon(soort, kleur, accent, 7000 + i)
        _item(a, w, "vr_z6_mob_" + k, f"{SUB}/{k}", icoon)
    for k, (nl, en, icoon, _fl) in WAPENS.items():
        _item(a, w, "vr_z6_" + k, f"{SUB}/{k}", icoon(), handheld=True)
    for i, (k, (nl, en, kleur, _fl)) in enumerate(KRISTALLEN.items()):
        _item(a, w, "vr_z6_kristal_" + k, f"{SUB}/kristal_{k}", kristal(kleur, i))
    for k, (nl, en, fl, effect, icoon, hand) in BAAS_ITEMS.items():
        _item(a, w, "vr_z6_baas_" + k, f"{BSUB}/{k}", icoon(), handheld=hand)
    for i, b in enumerate(("wormelien", "zoutkolos", "sterrenguh")):
        _item(a, w, "vr_z6_baas_trofee_" + b, f"{BSUB}/trofee_{b}", z3.trofee_icoon(((255, 180, 206), (236, 230, 246), (255, 236, 170))[i], 7100 + i))
    _item(a, w, "vr_z6_sterrenstof", f"{SUB}/sterrenstof", _sterrenstof())
    _item(a, w, "vr_z6_sterrenkompas", f"{SUB}/sterrenkompas", _kompas())
    return n


def uiterlijken(a, w=vw._schrijf_json):
    """The worn look of every armour piece (head/chest/legs/feet): vanilla iron tinted (needs the minecraft jar in build/)."""
    import zipfile
    from io import BytesIO
    jar = os.path.join("build", "moddev", "artifacts", "minecraft-patched-26.1.2.112.jar")
    bron = {}
    if os.path.exists(jar):
        with zipfile.ZipFile(jar) as zf:
            for laag in ("humanoid", "humanoid_leggings"):
                bron[laag] = Image.open(BytesIO(zf.read(f"assets/minecraft/textures/entity/equipment/{laag}/iron.png"))).convert("RGBA")
    for hid, (soort, _nl, _en, kleur, _k2) in GEAR.items():
        if soort not in ("hoofd", "borst", "benen", "voeten"):
            continue
        uid = "vr_z6_mob_" + hid
        w(os.path.join(a, "equipment", uid + ".json"), {"layers": {"humanoid": [{"texture": "guhs:" + uid}], "humanoid_leggings": [{"texture": "guhs:" + uid}]}})
        for laag in ("humanoid", "humanoid_leggings"):
            doel = os.path.join(a, "textures", "entity", "equipment", laag, uid + ".png")
            if laag not in bron:
                if not os.path.exists(doel):
                    print("vadsrijk_u_z6wezens: no minecraft jar (run a gradle build first) for", doel)
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
        h.lang(k, en, nl)   # (make_v2.lang(key, en, nl))


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
    print(f"vadsrijk_u_z6wezens: {n} creature/boss models, the Bultguh, {len(GEAR) + len(WAPENS)} gear items, {len(t)} texts")


if __name__ == "__main__":
    _direct()
