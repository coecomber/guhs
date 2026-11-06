"""
1.10 "Het Vadsrijk", zone 5 "Hoogvads": the combat slice (T-Z5WEZENS). Java: feature/vadsrijk/zone5/wezens.

Everything generated (no hand-made art), on the zone-1/2/3 helpers (vadsrijk_wezens.Model/kf/anim, vadsrijk_z2_wezens builders):
  * GeckoLib models + animations (assets/guhs/geckolib/{models,animations}/entity/vadsrijk/zone5/wezens/):
      Proefarena   bezeten_oefenpop (a straw dummy with grey fluff), sparringridder (a knight GUH with helmet, plume, round shield
                   and a pillow sword)
      Buikgangen   mika_saboteur (grey overalls, wrench, gas sack), mika_lontlegger (cheese-bomb basket), grauwe_pluismijt
      Wolkrand     grauwe_wolkduivel (grey cloud-imp, horns, trident), mistslier (grey veil wisp), mika_harpoenier (harpoon gun)
      Schuilnest   vermomde_mika (a Mika with a Wolkwacht-knight costume as the bone "vermomming")
      bosses       zilverstaart (slim Mika spy, silver tail tip, cloak, hatpin, + costume), gorgonzola (a fat boiler-suit Mika),
                   donderbuik (a fat grumpy thundercloud; donderbuik_roze = cheered up)
      props/pets   klep (warm-air valve; klep_dicht, klep_gas), lokpop (the spy's decoy knight), warmpje (pet: a warm cloud puff),
                   huisdier.animation.json (idle/hop/sprong on "root", for the 3 pets)
  * per thing: <naam>.png, <naam>_gloed.png (glowmask), <naam>_elite.png; item icons; worn armour looks; all texts (nl + en).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_z5wezens.py
"""
import json
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
    from features.vadsrijk_economie import Doek, _licht, zwaard, boog, staf
    from features.vadsrijk_z2_systemen import hamer, kruisboog
    from features.vadsrijk_z3_systemen import knots
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
    from features.vadsrijk_economie import Doek, _licht, zwaard, boog, staf
    from features.vadsrijk_z2_systemen import hamer, kruisboog
    from features.vadsrijk_z3_systemen import knots

Model, kf, anim, anim_bestand = vw.Model, vw.kf, vw.anim, vw.anim_bestand
samen, adem, zwaai, een, stil = z2.samen, z2.adem, z2.zwaai, z2.een, z2.stil
SUB = "vadsrijk/zone5/wezens"

GRAUW = ((132, 134, 146), "plekjes", (112, 116, 130))
PLUIS = ((196, 198, 206), "plekjes", (170, 174, 186))
STRO = ((222, 190, 112), "strepen", (190, 156, 84))
WOLK = ((236, 240, 248), "plekjes", (214, 222, 236))
ROZE = ((255, 170, 210), "plekjes", (250, 140, 190))
HEMEL = ((150, 190, 240), "strepen", (120, 160, 220))
GOUD = ((240, 200, 90), "glans", None)
ZILVER = ((214, 220, 232), "glans", None)


def mika_pal(vest, broek=((70, 74, 96), None, None), helm=((60, 60, 70), None, None), **extra):
    p = {"vacht": ((206, 146, 166), "plekjes", (190, 132, 152)), "snuit": ((232, 172, 188), None, None), "oor": ((238, 148, 168), None, None),
         "poot": ((238, 168, 184), None, None), "vest": vest, "broek": broek, "laars": ((48, 40, 36), None, None), "helm": helm,
         "lamp": ((255, 230, 140), None, None), "steel": ((90, 70, 52), None, None), "ijzer": ((150, 152, 164), None, None),
         "boor": ((130, 132, 142), "strepen", (100, 100, 112))}
    p.update(extra)
    return p


# =====================================================================================================================
# the costume (Wolkwacht knight) as a bone, shared by vermomde_mika, zilverstaart and lokpop
# =====================================================================================================================
def kostuum(m, ouder="root"):
    m.bone("vermomming", ouder, (0, 0, 0))
    m.cube("vermomming", "kostuum", (-5.5, 1.5, -5), (11, 16, 10))                       # padded round body
    m.cube("vermomming", "tabberd", (-5.8, 4, -5.4), (11.6, 9, 0.6))
    m.cube("vermomming", "kostuum", (-6, 17, -6), (12, 11, 11), gezicht={"mood": "blij", "ey": 0.55})   # painted guh face
    m.cube("vermomming", "helmkostuum", (-6.4, 25, -6.4), (12.8, 3.5, 11.8))
    m.cube("vermomming", "pluim", (-1, 28.5, -3), (2, 5, 7))
    for sx in (-1, 1):
        m.cube("vermomming", "kostuum", (sx * 4 - 1.5, 27, -1), (3, 3, 2))              # felt ears
    m.cube("vermomming", "laars", (-5, 0, -4), (4, 1.6, 6))
    m.cube("vermomming", "laars", (1, 0, -4), (4, 1.6, 6))


KOSTUUM_PAL = {"kostuum": ((250, 236, 240), "plekjes", (236, 220, 228)), "tabberd": ((120, 170, 236), "strepen", (240, 210, 110)),
               "helmkostuum": ((200, 206, 220), "glans", None), "pluim": ((255, 140, 190), None, None)}


# =====================================================================================================================
# the creatures
# =====================================================================================================================
def m_bezeten_oefenpop():
    m = Model("bezeten_oefenpop", px=4, breedte=96, bounds=(2, 3, 0.8))
    m.bone("root")
    m.bone("paal", "root", (0, 0, 0))
    m.cube("paal", "hout", (-1, 0, -1), (2, 10, 2))
    m.cube("paal", "hout", (-3, 0, -3), (6, 1, 6))
    m.bone("lijf", "root", (0, 10, 0))
    m.cube("lijf", "stro", (-4.5, 9, -3.5), (9, 12, 7))
    m.cube("lijf", "touw", (-4.7, 13, -3.7), (9.4, 1.2, 7.4))
    m.cube("lijf", "pluis", (-5, 18, -1), (2.5, 2.5, 3))
    m.cube("lijf", "pluis", (2.8, 11, 1), (2.5, 2.5, 2.5))
    m.bone("kop", "lijf", (0, 21, 0))
    m.cube("kop", "zak", (-3.5, 21, -3.5), (7, 7, 7), gezicht={"mood": "geest", "ey": 0.45})
    m.cube("kop", "pluis", (-1.5, 27.5, -1.5), (3, 2, 3))
    for kant, sx in (("l", -1), ("r", 1)):
        b = "arm_" + kant
        m.bone(b, "lijf", (sx * 4.5, 19, 0), (0, 0, sx * -70))
        ox = -12.5 if sx < 0 else 4.5
        m.cube(b, "stro", (ox, 17.5, -1.5), (8, 3, 3))
    a = {
        "idle": anim(2.0, samen(adem("lijf", 2.0, 1.02), {"kop": {"rotation": kf([(0, (0, -6, 4)), (1, (0, 6, -4)), (2, (0, -6, 4))])}})),
        "loop": anim(0.5, {"root": {"position": kf([(0, (0, 0, 0)), (0.25, (0, 2.5, 0)), (0.5, (0, 0, 0))]),
                                    "rotation": kf([(0, (0, 0, -6)), (0.25, (0, 0, 6)), (0.5, (0, 0, -6))])}}),
        "stil": anim(1.0, stil("root")),
        "draai": anim(0.4, {"root": {"rotation": kf([(0, (0, 0, 0)), (0.2, (0, 180, 0)), (0.4, (0, 360, 0))])}}),
        "aanval": een(0.4, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-60, 0, 0)), (0.4, (0, 0, 0))])},
                            "lijf": {"rotation": kf([(0, (0, 0, 0)), (0.15, (15, 0, 0)), (0.4, (0, 0, 0))])}}),
    }
    pal = {"hout": ((120, 92, 66), "strepen", (96, 72, 52)), "stro": STRO, "touw": ((150, 110, 70), None, None), "zak": ((200, 186, 150), "plekjes", (180, 166, 130)),
           "pluis": PLUIS}
    return m, a, pal, {"zak": (20, 24, 40)}


def m_sparringridder():
    m = Model("sparringridder", px=4, breedte=96, bounds=(2, 2, 0.6))
    m.bone("root")
    m.bone("lijf", "root", (0, 2, 0))
    m.cube("lijf", "vacht", (-6, 2, -5.5), (12, 12, 11), gezicht={"mood": "blij", "ey": 0.45})
    m.cube("lijf", "tabberd", (-6.3, 2.5, -5.8), (12.6, 4, 0.5))
    for sx in (-1, 1):
        m.cube("lijf", "vacht", (sx * 4 - 1.5, 17, -1), (3, 3.5, 2))                     # guh ears through the helmet
        m.cube("lijf", "poot", (sx * 3 - 1.5, 0, -2), (3, 2, 4))
    m.bone("helm", "lijf", (0, 14, 0))
    m.cube("helm", "helm", (-6.4, 13, -6), (12.8, 3.5, 12))
    m.cube("helm", "pluim", (-1, 16.5, -3), (2, 5, 7))
    m.bone("arm_l", "lijf", (-6, 8, 0))
    m.cube("arm_l", "vacht", (-8, 5, -1.5), (2.5, 4, 3))
    m.bone("schild", "arm_l", (-8, 7, -2))
    m.cube("schild", "schild", (-9.5, 3, -5), (1.2, 8, 8))
    m.cube("schild", "tabberd", (-9.8, 6, -2), (0.4, 2, 2))
    m.bone("arm_r", "lijf", (6, 8, 0))
    m.cube("arm_r", "vacht", (5.5, 5, -1.5), (2.5, 4, 3))
    m.bone("wapen", "arm_r", (7, 6, -1))
    m.cube("wapen", "steel", (6.4, 5, -2), (1.2, 1.2, 4))
    m.cube("wapen", "kussen", (5.4, 4, -10), (3.2, 3.2, 8))
    bob = {"lijf": {"position": kf([(0, (0, 0, 0)), (0.5, (0, 0.6, 0)), (1, (0, 0, 0))])}}
    a = {
        "idle": anim(1.0, bob),
        "loop": anim(0.5, {"root": {"position": kf([(0, (0, 0, 0)), (0.25, (0, 1.5, 0)), (0.5, (0, 0, 0))])}}),
        "aanval": een(0.5, {"arm_r": {"rotation": kf([(0, (-60, 40, 0)), (0.2, (20, -60, 0)), (0.5, (0, 0, 0))])}}),
        "mik": anim(0.6, {"arm_r": {"rotation": kf([(0, (-70, 40, 0)), (0.3, (-80, 45, 0)), (0.6, (-70, 40, 0))])}}),
        "blok": anim(1.0, {"arm_l": {"rotation": kf([(0, (0, -40, 0))])}, "schild": {"rotation": kf([(0, (0, -50, 0))])}}),
        "buig": anim(1.0, {"lijf": {"rotation": kf([(0, (30, 0, 0))])}}),
        "blij": anim(1.0, samen(bob, {"root": {"position": kf([(0, (0, 0, 0)), (0.25, (0, 2, 0)), (0.5, (0, 0, 0))])}})),
    }
    pal = {"vacht": ((252, 190, 214), "plekjes", (244, 172, 200)), "poot": ((240, 160, 190), None, None), "tabberd": ((120, 170, 236), "strepen", (240, 210, 110)),
           "helm": ((206, 212, 226), "glans", None), "pluim": ((255, 120, 180), None, None), "schild": ((120, 170, 236), "facet", (250, 226, 130)),
           "steel": ((150, 110, 80), None, None), "kussen": ((255, 250, 250), "plekjes", (236, 226, 240))}
    return m, a, pal, {}


def m_mika_saboteur():
    m, a = z2.mika("mika_saboteur", "houweel")
    m.cube("lijf", "zak", (-3.4, 10, 2.4), (6.8, 6, 3.5))                                 # gas spanners in a sack
    pal = mika_pal(((120, 124, 136), "strepen", (90, 94, 104)), helm=((90, 92, 102), None, None), zak=((150, 150, 160), "plekjes", (120, 120, 130)))
    return m, a, pal, {"lamp": (160, 130, 40)}


def m_mika_lontlegger():
    m, a = z2.mika("mika_lontlegger", "boor")
    m.cube("motor", "kaas", (-3.6, 11, 2.4), (7.2, 5, 4.6))
    for x in (-2.5, 0, 2.5):
        m.cube("motor", "lont", (x - 0.3, 16, 4), (0.6, 2, 0.6))
    pal = mika_pal(((170, 60, 50), "strepen", (240, 200, 90)), helm=((240, 200, 80), "stippen", (210, 160, 50)),
                   kaas=((250, 210, 90), "stippen", (220, 170, 60)), lont=((60, 50, 40), None, None))
    return m, a, pal, {"lamp": (160, 120, 30), "lont": (90, 40, 0)}


def m_grauwe_pluismijt():
    m, a = z2.vierpoot("grauwe_pluismijt", lang=5, hoog=3.5, kop=3, poot=1.4, W=64, gezicht="spin", staart=False)
    m.cube("lijf", "pluis", (-2, 4.6, -1.5), (4, 2, 3))
    pal = {"vacht": GRAUW, "snuit": ((100, 102, 114), None, None), "poot": ((80, 82, 94), None, None), "pluis": PLUIS}
    return m, a, pal, {}


def m_grauwe_wolkduivel():
    m, a = z2.vlieger("grauwe_wolkduivel", lijf=(7, 6, 7), vleugel=(9, 0.8, 7), kop=5, W=96, gezicht="beest")
    for sx in (-1, 1):
        m.cube("kop", "hoorn", (sx * 1.8 - 0.6, 10.5, -6.5), (1.2, 2.4, 1.2), rot=(0, 0, sx * -20), pivot=(sx * 1.8, 10.5, -6))
    m.cube("lijf", "wolk", (-4.5, 9, -4), (9, 2.5, 8))
    m.bone("drietand", "lijf", (4, 6, -2))
    m.cube("drietand", "ijzer", (4, 4, -6), (0.8, 0.8, 9))
    m.cube("drietand", "ijzer", (3.2, 4, -7), (2.4, 0.8, 0.8))
    pal = {"vacht": ((110, 112, 126), "plekjes", (90, 92, 106)), "vleugel": ((150, 152, 166), "plekjes", (130, 132, 146)), "wolk": PLUIS,
           "hoorn": ((60, 58, 70), None, None), "ijzer": ((90, 90, 100), None, None)}
    return m, a, pal, {"vacht": (0, 0, 0)}


def m_mistslier():
    m, a = z2.zwever("mistslier", hoog=14, breed=8, gezicht="geest")
    a["blaas"] = anim(1.0, {"lijf": {"rotation": kf([(0, (-12, 0, 0))])}, "kop": {"scale": kf([(0, (1, 1, 1)), (0.9, (1.2, 1.1, 1.2))])}}, loop="hold")
    pal = {"mantel": ((180, 184, 196), "strepen", (160, 164, 178)), "kristal": ((214, 218, 228), "plekjes", (196, 200, 212))}
    return m, a, pal, {"kristal": (40, 50, 70), "mantel": (10, 14, 22)}


def m_mika_harpoenier():
    m, a = z2.mika("mika_harpoenier", "boor")
    m.cube("wapen", "steel", (3.5, 9, -16), (1.2, 1.2, 7))                                # the harpoon on the gun
    m.cube("wapen", "ijzer", (3, 8.5, -17.5), (2.2, 2.2, 1.5))
    m.cube("lijf", "touw", (-4, 12, -2.6), (8, 1, 5.2), rot=(0, 0, 25), pivot=(0, 13, 0))
    pal = mika_pal(((60, 90, 130), "strepen", (230, 230, 240)), helm=((40, 60, 90), None, None), touw=((180, 150, 100), None, None))
    return m, a, pal, {"lamp": (120, 120, 160)}


def m_vermomde_mika():
    m, a = z2.mika("vermomde_mika", "houweel")
    kostuum(m)
    a["wacht"] = anim(2.0, adem("vermomming", 2.0, 1.01))
    a["speciaal"] = een(0.5, {"vermomming": {"scale": kf([(0, (1, 1, 1)), (0.2, (1.3, 1.3, 1.3)), (0.5, (0.01, 0.01, 0.01))])}})
    pal = mika_pal(((40, 40, 50), "strepen", (70, 70, 84)), helm=((40, 40, 50), None, None))
    pal.update(KOSTUUM_PAL)
    return m, a, pal, {"lamp": (100, 20, 20)}


# ---- bosses ----------------------------------------------------------------------------------------------------------
def baas_anims(a):
    a.setdefault("walk", a.get("loop", anim(1.0, stil())))
    a.setdefault("blij", anim(1.0, stil()))
    return a


def m_zilverstaart():
    m, a = z2.mika("zilverstaart", "houweel")
    m.cube("staart", "zilver", (-0.8, 9.3, 10.6), (1.6, 1.6, 2.4))                       # the silver tail tip: the tell
    m.cube("lijf", "mantel", (-4.2, 6, 2.3), (8.4, 12, 1))                                 # the stolen Wolkwacht cloak
    m.cube("kop", "monocle", (1, 21.5, -5.6), (2, 2, 0.4))
    kostuum(m)
    a["speciaal"] = een(0.6, {"lijf": {"scale": kf([(0, (1, 1, 1)), (0.3, (1.15, 0.9, 1.15)), (0.6, (1, 1, 1))])}})
    pal = mika_pal(((60, 50, 90), "strepen", (200, 200, 220)), helm=((50, 40, 70), None, None), zilver=ZILVER,
                   mantel=((110, 160, 230), "strepen", (240, 210, 110)), monocle=((230, 230, 240), None, None), steel=((210, 214, 226), None, None),
                   ijzer=((230, 234, 244), None, None))
    pal.update(KOSTUUM_PAL)
    return m, baas_anims(a), pal, {"zilver": (200, 210, 240), "lamp": (120, 20, 30)}


def m_gorgonzola():
    m, a = z2.mika("gorgonzola", "boor")
    m.cube("lijf", "buik", (-4.5, 9.5, -3.4), (9, 7.5, 6))                                  # the belly
    m.cube("motor", "tank", (-3.8, 10.5, 2.6), (7.6, 8, 4.6))
    m.cube("wapen", "ijzer", (2.6, 7, -12), (4.2, 2, 2.4))                                 # the big wrench jaw
    pal = mika_pal(((80, 110, 90), "strepen", (60, 86, 70)), helm=((200, 170, 60), None, None), buik=((80, 110, 90), None, None),
                   tank=((150, 156, 170), "strepen", (120, 124, 136)), boor=((160, 162, 172), "strepen", (130, 130, 140)))
    return m, baas_anims(a), pal, {"lamp": (160, 120, 30)}


def m_donderbuik():
    m = Model("donderbuik", px=4, breedte=256, bounds=(6, 6, 1))
    m.bone("root")
    m.bone("lijf", "root", (0, 20, 0))
    m.cube("lijf", "wolk", (-28, 4, -20), (56, 34, 40), gezicht={"mood": "boos", "ey": 0.4})
    for (x, y, z, s) in ((-30, 26, -8, 18), (14, 28, -6, 20), (-12, 34, -10, 22), (-36, 8, -6, 14), (24, 10, -8, 14)):
        m.cube("lijf", "wolk2", (x, y, z), (s, s * 0.7, s))
    m.bone("arm_l", "lijf", (-28, 18, 0))
    m.cube("arm_l", "wolk2", (-40, 10, -6), (12, 12, 12))
    m.bone("arm_r", "lijf", (28, 18, 0))
    m.cube("arm_r", "wolk2", (28, 10, -6), (12, 12, 12))
    m.bone("regen", "root", (0, 4, 0))
    for i in range(8):
        x = -22 + i * 6
        m.cube("regen", "druppel", (x, 0, -10 + (i % 3) * 8), (1, 3, 1))
    bob = {"lijf": {"position": kf([(0, (0, 0, 0)), (1.0, (0, 2, 0)), (2.0, (0, 0, 0))])},
           "regen": {"position": kf([(0, (0, 2, 0)), (0.5, (0, -2, 0))])}}
    a = {
        "idle": anim(2.0, bob),
        "walk": anim(2.0, samen(bob, {"lijf": {"rotation": kf([(0, (0, 0, -3)), (1, (0, 0, 3)), (2, (0, 0, -3))])}})),
        "aanval": een(0.6, {"arm_r": {"rotation": kf([(0, (0, 0, 0)), (0.25, (-40, 0, 30)), (0.6, (0, 0, 0))])}}),
        "zuig": een(2.5, {"lijf": {"scale": kf([(0, (1, 1, 1)), (2.0, (1.15, 1.1, 1.15)), (2.5, (0.95, 0.95, 0.95))])}}),
        "bliksem": een(0.8, {"lijf": {"position": kf([(0, (0, 0, 0)), (0.1, (1, 0, 0)), (0.2, (-1, 0, 0)), (0.3, (1, 0, 0)), (0.8, (0, 0, 0))])}}),
        "blij": anim(1.0, {"root": {"position": kf([(0, (0, 0, 0)), (0.5, (0, 3, 0)), (1, (0, 0, 0))])}}),
    }
    pal = {"wolk": ((120, 124, 140), "plekjes", (100, 104, 120)), "wolk2": ((140, 144, 160), "plekjes", (116, 120, 136)),
           "druppel": ((150, 190, 240), None, None)}
    return m, a, pal, {"druppel": (40, 80, 160)}


def m_klep():
    m = Model("klep", px=4, breedte=64, bounds=(2, 3, 0.8))
    m.bone("root")
    m.cube("root", "pijp", (-2, 0, -2), (4, 26, 4))
    m.cube("root", "pijp", (-3, 0, -3), (6, 2, 6))
    m.cube("root", "meter", (-1.5, 18, -2.6), (3, 3, 0.6))
    m.bone("wiel", "root", (0, 12, -3))
    m.cube("wiel", "wiel", (-4, 8, -3.6), (8, 8, 1))
    m.cube("wiel", "pijp", (-1, 11, -4), (2, 2, 1))
    a = {"idle": anim(2.0, stil()), "pomp": een(0.5, {"wiel": {"rotation": kf([(0, (0, 0, 0)), (0.5, (0, 0, 120))])}})}
    pal = {"pijp": ((200, 130, 90), "strepen", (170, 104, 70)), "meter": ((240, 236, 220), None, None), "wiel": ((255, 140, 190), "facet", (220, 100, 160))}
    return m, a, pal, {"meter": (120, 80, 20)}


def m_lokpop():
    m = Model("lokpop", px=4, breedte=96, bounds=(2, 3, 0.8))
    m.bone("root")
    kostuum(m)
    a = {"idle": anim(2.0, adem("vermomming", 2.0, 1.01))}
    pal = dict(KOSTUUM_PAL)
    pal["laars"] = ((48, 40, 36), None, None)
    return m, a, pal, {}


def m_warmpje():
    m = Model("warmpje", px=4, breedte=64, bounds=(1, 1, 0.3))
    m.bone("root")
    m.cube("root", "wolk", (-4, 1, -4), (8, 6, 8), gezicht={"mood": "blij", "ey": 0.4})
    m.cube("root", "wolk", (-5, 3, -2), (2, 3, 4))
    m.cube("root", "wolk", (3, 3, -2), (2, 3, 4))
    a = {"idle": anim(1.5, {"root": {"position": kf([(0, (0, 0, 0)), (0.75, (0, 1, 0)), (1.5, (0, 0, 0))])}})}
    pal = {"wolk": ((255, 196, 160), "plekjes", (255, 170, 150))}
    return m, a, pal, {"wolk": (90, 40, 20)}


WEZENS = [("bezeten_oefenpop", m_bezeten_oefenpop), ("sparringridder", m_sparringridder), ("mika_saboteur", m_mika_saboteur),
          ("mika_lontlegger", m_mika_lontlegger), ("grauwe_pluismijt", m_grauwe_pluismijt), ("grauwe_wolkduivel", m_grauwe_wolkduivel),
          ("mistslier", m_mistslier), ("mika_harpoenier", m_mika_harpoenier), ("vermomde_mika", m_vermomde_mika)]
BAZEN = [("zilverstaart", m_zilverstaart), ("gorgonzola", m_gorgonzola), ("donderbuik", m_donderbuik)]
PROPS = [("klep", m_klep), ("lokpop", m_lokpop), ("warmpje", m_warmpje)]

# animations the Java code triggers or holds (the generator checks they exist)
NODIG = {
    "bezeten_oefenpop": ["idle", "loop", "stil", "draai", "aanval", "blij"],
    "sparringridder": ["idle", "loop", "aanval", "mik", "blok", "buig", "blij"],
    "mika_saboteur": ["idle", "loop", "aanval", "spuug", "blij"],
    "mika_lontlegger": ["idle", "loop", "aanval", "spuug", "blij"],
    "grauwe_pluismijt": ["idle", "loop", "aanval", "blij"],
    "grauwe_wolkduivel": ["idle", "vlieg", "aanval", "duik", "blij"],
    "mistslier": ["idle", "vlieg", "spreuk", "speciaal", "blaas", "blij"],
    "mika_harpoenier": ["idle", "loop", "aanval", "mik", "spuug", "blij"],
    "vermomde_mika": ["idle", "loop", "aanval", "wacht", "speciaal", "blij"],
    "zilverstaart": ["idle", "walk", "aanval", "spuug", "speciaal", "klap"],
    "gorgonzola": ["idle", "walk", "aanval", "hef", "klap", "spuug"],
    "donderbuik": ["idle", "walk", "aanval", "zuig", "bliksem", "blij"],
    "klep": ["idle", "pomp"], "lokpop": ["idle"], "warmpje": ["idle"],
}


# =====================================================================================================================
# painting (zone-2 painter + the guh faces of zone 1 for "blij"/"boos")
# =====================================================================================================================
GUH_MOODS = ("blij", "boos", "o")


def schilder(model, palet, seed, glow_rollen=None, glow=False):
    S = model.px
    img = np.zeros((model.hoogte() * S, model.W * S, 4), np.uint8)
    img[..., 3] = 255 if glow else 0
    r = vw._rng(seed)
    for reg in model.regios:
        x, y, w, h = reg["r"]
        blok = img[y * S:(y + h) * S, x * S:(x + w) * S]
        if glow:
            if glow_rollen and reg["rol"] in glow_rollen:
                vw._vul(blok, glow_rollen[reg["rol"]], r, 8)
        else:
            kleur, pat, k2 = palet.get(reg["rol"], ((255, 0, 255), None, None))
            f = reg["f"]
            donker = 0.78 if f == "down" else 0.9 if f in ("east", "west", "south") else 1.0
            vw._vul(blok, tuple(np.array(kleur) * donker), r)
            if pat:
                vw._patroon(blok, pat, k2, r, f)
        g = reg["gezicht"]
        if g:
            mood = g.get("mood", "beest")
            if mood in GUH_MOODS:
                if not glow:
                    vw.gezicht(blok, mood, g.get("ey", 0.42))
            else:
                z2.oog(blok, mood, g.get("ey", 0.4), glow)
    return Image.fromarray(img)


def roze_versie(pal):
    """Donderbuik cheered up: pink cloud, happy face (the face mood is swapped by the caller)."""
    p = dict(pal)
    p["wolk"] = ((255, 196, 222), "plekjes", (250, 170, 206))
    p["wolk2"] = ((255, 214, 232), "plekjes", (252, 186, 216))
    p["druppel"] = ((255, 140, 200), None, None)
    return p


# =====================================================================================================================
# gear
# =====================================================================================================================
HARNAS = {
    # id: (slot, name nl, name en, colour, accent)
    "wolkwachthelm": ("hoofd", "Wolkwachthelm", "Cloudguard Helmet", (206, 212, 230), (255, 140, 190)),
    "wolkwachtkuras": ("borst", "Wolkwachtkuras", "Cloudguard Cuirass", (196, 204, 226), (120, 170, 236)),
    "wolkwachtbeenstukken": ("benen", "Wolkwachtbeenstukken", "Cloudguard Greaves", (186, 196, 220), (120, 170, 236)),
    "wolkwachtlaarzen": ("voeten", "Wolkwachtlaarzen", "Cloudguard Boots", (176, 186, 214), (240, 210, 110)),
}
VIRTUEEL = {
    # id: (soort, name nl, name en, colour, accent)
    "wolkwachthandschoenen": ("handschoenen", "Wolkwachthandschoenen", "Cloudguard Gauntlets", (200, 208, 228), (255, 140, 190)),
    "wolkwachtcape": ("cape", "Wolkwachtcape", "Cloudguard Cape", (120, 170, 236), (240, 210, 110)),
    "wolkring": ("ring", "Wolkring", "Cloud Ring", (236, 240, 250), (150, 200, 255)),
    "walvisketting": ("ketting", "Walvisketting", "Whale Necklace", (180, 160, 220), (255, 170, 210)),
    "regendruppeloorbel": ("oorbel", "Regendruppeloorbel", "Raindrop Earring", (200, 220, 240), (110, 170, 240)),
    "stormarmband": ("armband", "Stormarmband", "Storm Bangle", (170, 176, 196), (255, 230, 120)),
}
WAPENS = {
    "wolkwachtzwaard": ("Wolkwachtzwaard", "Cloudguard Sword", lambda: zwaard((214, 224, 246), (255, 140, 190))),
    "walvisbotknots": ("Walvisbotknots", "Whalebone Club", lambda: knots((236, 228, 210), (150, 200, 255))),
    "stormboog": ("Stormboog", "Storm Bow", lambda: boog((150, 160, 190), (200, 230, 255))),
    "harpoenkruisboog": ("Harpoenkruisboog", "Harpoon Crossbow", lambda: kruisboog((120, 90, 70), (200, 200, 210))),
    "wolkenstaf": ("Wolkenstaf", "Cloud Staff", lambda: staf(wolktop)),
    "bliksemstaf": ("Bliksemstaf", "Lightning Staff", lambda: staf(bliksemtop)),
}
FLAVOUR = {
    "wolkwachthelm": ["Gepoetst tot je je eigen oortjes erin ziet.", "Er zit een veertje bij. Altijd.", "De pluim wiebelt als je knikt."],
    "wolkwachtkuras": ["Licht als een wolk, stevig als een knuffel.", "Op de borst: een geborduurd walvisje.", "Ruikt naar soep en zonneschijn."],
    "wolkwachtbeenstukken": ["Gemaakt om te marcheren, en om te huppelen.", "Klinkt een beetje als belletjes.", "Nooit koude knietjes meer."],
    "wolkwachtlaarzen": ["Zolen van samengeperste wolk.", "Je laat kleine wolkjes achter.", "De Wolkwacht staat er stevig op."],
    "wolkwachthandschoenen": ["Zachte vingers, harde klappen.", "Ook geschikt om koekjes te delen.", "Het wapentje van de Wolkwacht staat erop."],
    "wolkwachtcape": ["Wappert ook als er geen wind is.", "Lichtblauw met een gouden randje.", "Koningin Zachtmoed breide de eerste."],
    "wolkring": ["Er draait een piepklein wolkje in rond.", "Koel aan je vinger, warm aan je hart.", "Uit de oude schatkist van de Raad."],
    "walvisketting": ["Een zingend schelpje van Moeder Wolkwal.", "Als je hem tegen je oor houdt, hoor je haar neuriën.", "Gekregen, nooit gekocht."],
    "regendruppeloorbel": ["Een druppel die nooit valt.", "Glinstert in elke kleur van de regenboog.", "Plink."],
    "stormarmband": ["Knettert een beetje als je hem aandoet.", "Gesmeed in een onweer. Een klein onweer.", "Je haren staan er recht van."],
    "wolkwachtzwaard": ["Het zwaard van de Wolkwacht: scherp, maar beleefd.", "De gevest heeft roze oortjes.", "Voor de walvis, voor de stad!"],
    "walvisbotknots": ["Een oud melkbotje van Moeder Wolkwal. Ze had er nog genoeg.", "Bonk. Met liefde.", "Heel zwaar, heel trouw."],
    "stormboog": ["De pees zoemt als een onweerswolk.", "Pijlen zo snel als de wind boven de wolken.", "Wolkwachters schieten er vliegers mee los."],
    "harpoenkruisboog": ["Afgepakt van een Mika-harpoenier. Nu doet hij goede dingen.", "Klik. Zoef. Raak.", "Het touw is vervangen door een lint."],
    "wolkenstaf": ["Bovenop draait een wolkje dat regent op wie het nodig heeft.", "Je heals ruiken naar regen op een zomerdag.", "Zacht als een kussen."],
    "bliksemstaf": ["Een pijpje bliksem in een glazen bol.", "Niet aanraken met natte pootjes.", "ZAP! Maar dan lief."],
}

BAAS_ITEMS = {
    # item id: (type, name nl, name en, icon maker, flavour)
    "vr_z5_baas_hoedenspeld": ("z5baas_hoedenspeld", "Zilverstaarts Hoedenspeld", "Silvertail's Hatpin",
                               lambda: zwaard((220, 226, 240), (200, 200, 220)), "Een spion ziet alles. Behalve deze."),
    "vr_z5_baas_vermommingsmantel": ("z5baas_vermommingsmantel", "Mantel der Vermommingen", "Cloak of Disguises",
                                     lambda: cape_icoon((70, 60, 110), (210, 214, 230)), "Draai hem om en je bent iemand anders. Bijna."),
    "vr_z5_baas_moersleutel": ("z5baas_moersleutel", "Gorgonzola's Moersleutel", "Gorgonzola's Wrench",
                               lambda: hamer((160, 162, 172)), "Hij draaide er kleppen mee dicht. Jij draait ze weer open."),
    "vr_z5_baas_kleppenstaf": ("z5baas_kleppenstaf", "Kleppenstaf", "Valve Staff", lambda: staf(kleptop), "Warme lucht, warme vads, warme heals."),
    "vr_z5_baas_donderboog": ("z5baas_donderboog", "Donderboog", "Thunder Bow", lambda: boog((110, 116, 140), (255, 236, 120)),
                              "Rommelt zacht als je hem spant."),
    "vr_z5_baas_onweerstaf": ("z5baas_onweerstaf", "Onweerstaf", "Storm Staff", lambda: staf(bliksemtop), "Een stukje Donderbuik, nu roze en vrolijk."),
    "vr_z5_baas_regenjasje": ("z5baas_regenjasje", "Donderbuiks Regenjasje", "Thunderbelly's Raincoat", lambda: jas_icoon((255, 170, 210)),
                              "Het regent alleen nog maar roze druppeltjes. En alleen op jou."),
}
TROFEEEN = {
    "zilverstaart": ("Zilveren Staartpunt", "Silver Tail Tip", (220, 226, 240)),
    "gorgonzola": ("Gouden Kleppenwiel", "Golden Valve Wheel", (255, 170, 210)),
    "donderbuik": ("Ingemaakt Onweertje", "Jarred Thunderstorm", (150, 160, 200)),
}


def wolktop(d):
    for x, y in ((10, 3), (12, 2), (13, 4), (11, 5)):
        d.disc(x, y, 1.8, (240, 244, 252))
    d.px(9, 7, (150, 200, 255))
    d.px(13, 7, (150, 200, 255))


def bliksemtop(d):
    d.disc(11, 4, 3, (200, 210, 240))
    d.lijn(12, 1, 10, 4, (255, 236, 120))
    d.lijn(10, 4, 12, 4, (255, 236, 120))
    d.lijn(12, 4, 10, 7, (255, 236, 120))


def kleptop(d):
    d.disc(11, 4, 3, (255, 140, 190))
    d.disc(11, 4, 1.4, (200, 130, 90))
    d.lijn(8, 4, 14, 4, (200, 130, 90))
    d.lijn(11, 1, 11, 7, (200, 130, 90))


def cape_icoon(kleur, rand):
    d = Doek()
    d.poly([(5, 2), (11, 2), (14, 14), (2, 14)], kleur)
    d.lijn(5, 2, 11, 2, rand, dik=2)
    d.lijn(2, 14, 14, 14, rand)
    d.schaduw()
    return d.rand()


def handschoen_icoon(kleur, accent):
    d = Doek()
    d.rect(4, 6, 11, 13, kleur)
    for x in (4, 6, 8, 10):
        d.rect(x, 3, x + 1, 6, _licht(kleur, 1.1))
    d.rect(11, 7, 13, 9, kleur)
    d.rect(4, 12, 11, 13, accent)
    d.schaduw()
    return d.rand()


def armband_icoon(kleur, steen):
    d = Doek()
    for t in range(40):
        a = t / 40 * 2 * np.pi
        d.px(int(round(8 + np.cos(a) * 5)), int(round(8 + np.sin(a) * 3.5)), kleur)
    d.disc(8, 4.5, 1.5, steen)
    d.schaduw()
    return d.rand()


def jas_icoon(kleur):
    d = Doek()
    d.poly([(4, 3), (12, 3), (13, 14), (3, 14)], kleur)
    d.rect(7, 3, 9, 14, _licht(kleur, 0.85))
    d.rect(2, 4, 4, 10, kleur)
    d.rect(12, 4, 14, 10, kleur)
    d.schaduw()
    return d.rand()


def trofee_icoon(kleur):
    d = Doek()
    d.disc(8, 7, 5.5, (230, 190, 80))
    d.disc(8, 7, 4, kleur)
    d.rect(6, 12, 10, 14, (200, 160, 60))
    d.schaduw()
    return d.rand()


# =====================================================================================================================
# texts
# =====================================================================================================================
Z = "vadsrijk.z5wezens."
ENTITEITEN = {
    "vr_z5_mob_bezeten_oefenpop": ("Bezeten Oefenpop", "Possessed Training Dummy"),
    "vr_z5_mob_sparringridder": ("Sparringridder", "Sparring Knight"),
    "vr_z5_mob_mika_saboteur": ("Mika-saboteur", "Mika Saboteur"),
    "vr_z5_mob_mika_lontlegger": ("Mika-lontlegger", "Mika Fuse-Layer"),
    "vr_z5_mob_grauwe_pluismijt": ("Grauwe Pluismijt", "Grey Fluff Mite"),
    "vr_z5_mob_grauwe_wolkduivel": ("Grauwe Wolkduivel", "Grey Cloud Imp"),
    "vr_z5_mob_mistslier": ("Mistslier", "Mist Veil"),
    "vr_z5_mob_mika_harpoenier": ("Mika-harpoenier", "Mika Harpooner"),
    "vr_z5_mob_vermomde_mika": ("Vermomde Mika", "Disguised Mika"),
    "vr_z5_baas_zilverstaart": ("Kootje Zilverstaart", "Kootje Silvertail"),
    "vr_z5_baas_gorgonzola": ("Opzichter Gorgonzola", "Overseer Gorgonzola"),
    "vr_z5_baas_donderbuik": ("Donderbuik", "Thunderbelly"),
    "vr_z5_baas_klep": ("Warmeluchtklep", "Warm-Air Valve"),
    "vr_z5_baas_lokpop": ("Wolkwachter", "Cloudguard"),
}
TIPS = {
    "bezeten_oefenpop": ("Bezeten Oefenpop", "Die oefenpop bewoog! Grauw pluis is erin gekropen. Rode cirkel = hij draait rond: stap weg. Versla hem en het pluis waait eruit."),
    "sparringridder": ("Sparren", "De ridder slaat met een kussen langs de rode strook: stap eruit. Roze glinster aan zijn schild = hij blokt, wacht even. Drie kussenklappen en je verliest (het doet geen pijn)."),
    "mika_saboteur": ("Mika-saboteur", "Rode cirkel = daar gooit hij een gassleutel. Blijf uit de grijze wolk, die maakt je plakkerig."),
    "mika_lontlegger": ("Mika-lontlegger", "Hij blijft op afstand en gooit kaasbommen (rode cirkel). Ren naar hem toe: van dichtbij rent hij weg."),
    "grauwe_pluismijt": ("Grauwe Pluismijt", "Ze knabbelen aan de walvis. Klein en snel: klappen die meer mijten raken werken het best."),
    "grauwe_wolkduivel": ("Grauwe Wolkduivel", "Een rode streep onder je = hij duikt. Stap opzij!"),
    "mistslier": ("Mistslier", "Grijze cirkel = mist. Wie erin blijft ziet bijna niks en wordt plakkerig. Loop eruit."),
    "mika_harpoenier": ("Mika-harpoenier", "Rode lijn = zijn harpoen. Raak je hem, dan trekt hij je naar zich toe."),
    "vermomde_mika": ("Vermomde Mika", "Niet elke Wolkwachter is echt! Een rode cirkel achter je = hij verschijnt daar. Stap weg."),
}
BAAS_TIPS = {
    "zilverstaart": ("Kootje Zilverstaart", "Vijf Wolkwachters? Eén is hem: zoek de ZILVEREN glinster bij de staart. Een lokpop raken kost je wat Vads.",
                     "Ontsnapt hij? Draai de drie grauwgasventielen dicht (rechtsklik, 3x)."),
    "gorgonzola": ("Opzichter Gorgonzola", "Hij draait de warme-luchtkleppen dicht. Rechtsklik een dichte klep 3x om hem weer open te draaien.",
                   "Elke dichte klep maakt hem taaier en laat de kou binnen."),
    "donderbuik": ("Donderbuik", "Roze cirkels = wolkenkussens. Sta erop als hij gaat zuigen, anders trekt hij je naar zijn buik.",
                   "Gele cirkel: sta er samen in, dan deel je de donderslag."),
}

TEKSTEN = {
    Z + "mika.weg.0": "Dit komt in mijn rapport!", Z + "mika.weg.1": "De Ringvinger hoort hiervan!",
    Z + "mika.weg.2": "Ik was hier nooit!", Z + "mika.weg.3": "Mijn vermomming! Mijn mooie vermomming!",
    Z + "oefenpop.wakker": "De oefenpop beweegt!", Z + "vermomd.ontmaskerd": "Ontmaskerd!",
    Z + "leider.strobaal": "Opperpop Strobaal", Z + "leider.mistmoeder": "Moeder Mist", Z + "leider.meestervermommer": "Meester-Vermommer Feta",
    Z + "sparren.start": "Op je plaats... klaar... sparren! (ontwijk de rode strook, sla niet op het roze schild)",
    Z + "sparren.bezet": "Deze ridder spart al met iemand anders.",
    Z + "sparren.eerst_klikken": "Rechtsklik eerst op de ridder om te sparren.",
    Z + "sparren.geblokt": "Geblokt!",
    Z + "sparren.kussenklap": "Kussenklap! (%s/%s)",
    Z + "sparren.verloren.klappen": "Drie kussenklappen: de ridder wint deze ronde. Nog even oefenen!",
    Z + "sparren.verloren.weg": "De sparpartij is afgelopen.",
    Z + "sparren.gewonnen": "✦ Goed gevochten! De ridder buigt. (Proefpenningen: %s)",
    Z + "luik.buik_in": "Je klimt door het onderhoudsluik naar beneden... De Buikgangen.",
    Z + "luik.buik_uit": "Je klimt terug naar de kelder van de Soepkeuken.",
    Z + "luik.nest_in": "Achter de losse plank... een geheim Mika-nest!",
    Z + "luik.nest_uit": "Je glipt terug de Luchthaven in.",
    Z + "luik.zuidvin_af": "De wolkenlift zakt naar de zuidvin.", Z + "luik.zuidvin_op": "De wolkenlift brengt je terug naar de stad.",
    Z + "luik.noordvin_af": "De wolkenlift zakt naar de noordvin.", Z + "luik.noordvin_op": "De wolkenlift brengt je terug naar de stad.",
    Z + "luik.staart_af": "De wolkenlift zakt naar de staartvin.", Z + "luik.staart_op": "De wolkenlift brengt je terug naar het Staartpark.",
    Z + "luik.hint.buik_in": "Een onderhoudsluik (rechtsklik): naar de Buikgangen",
    Z + "luik.hint.buik_uit": "Het luik naar boven (rechtsklik)",
    Z + "luik.hint.nest_in": "Die plank zit los... (rechtsklik)",
    Z + "luik.hint.nest_uit": "Het luik naar boven (rechtsklik)",
    Z + "luik.hint.zuidvin_af": "Wolkenlift naar de zuidvin (rechtsklik)", Z + "luik.hint.zuidvin_op": "Wolkenlift naar de stad (rechtsklik)",
    Z + "luik.hint.noordvin_af": "Wolkenlift naar de noordvin (rechtsklik)", Z + "luik.hint.noordvin_op": "Wolkenlift naar de stad (rechtsklik)",
    Z + "luik.hint.staart_af": "Wolkenlift naar de staartvin (rechtsklik)", Z + "luik.hint.staart_op": "Wolkenlift naar het Staartpark (rechtsklik)",
    Z + "vangnet": "Een wolkje vangt je op! Pfoe.",
    Z + "wolkkussen": "Wolkkussen!",
    Z + "set.stukken": "Wolkwachtset (%s/%s gedragen)",
    Z + "set.2": "(2) Wolkwacht: Verdediging +10 en Vads +60",
    Z + "set.4": "(4) Ridderplicht: Schade tegen bazen +8%",
    Z + "set.6": "(6) Wolkkussen: onder 30% Vads vangt een wolk je op (Knuffelschild 20% van je max Vads, 6 s; 1x per 45 s)",
    Z + "loot.guhddelijk": "✦ %s vond iets GUHDDELIJKS: %s!",
    # Gorgonzola
    Z + "gz.fase2": "Gorgonzola fluit!", Z + "gz.fase2.onder": "Twee saboteurs komen helpen",
    Z + "gz.fase3": "Alle kleppen dicht!", Z + "gz.fase3.onder": "Draai er minstens drie weer open",
    Z + "gz.kleppen_eerst": "Hij is te taai zo: draai eerst de kleppen open!",
    Z + "gz.klep_open": "Deze klep is open. Goed zo!",
    Z + "gz.klep_draai": "Draaien... (%s/%s)",
    Z + "gz.klep_weer_open": "Warme lucht stroomt weer! (nog %s dicht)",
    Z + "gz.draait": "Hij draait een klep dicht!",
    Z + "gz.fluit": "FWIEEET! Jongens, hier!",
    Z + "gz.alles_dicht": "Geen warme lucht, geen walvis! Alles DICHT!",
    Z + "gz.weg": "Mijn kleppen... dit komt in mijn rapport!",
    # Zilverstaart
    Z + "zs.fase2": "De Vermommingen", Z + "zs.fase2.onder": "Welke Wolkwachter is hem? Zoek de zilveren glinster",
    Z + "zs.fase3": "De ontsnapping!", Z + "zs.fase3.onder": "Draai de grauwgasventielen dicht",
    Z + "zs.welke": "Vijf Wolkwachters... welke is hem? (zilveren glinster bij de staart)",
    Z + "zs.lokpop": "Een lokpop!",
    Z + "zs.gevonden": "Gevonden! Hij is even uit zijn evenwicht: +50% schade!",
    Z + "zs.vermom": "Zoek me maar, ridder. Ik ben iedereen en niemand.",
    Z + "zs.betrapt": "Hoe... hoe zag je dat?!",
    Z + "zs.haha": "Hahaha! Te laat!",
    Z + "zs.ontsnap": "Genoeg! Ik vlieg weg door het spuitgat, en de walvis gaat grauw!",
    Z + "zs.ventielen_eerst": "Hij houdt zich vast: draai eerst de grauwgasventielen dicht!",
    Z + "zs.ventiel_dicht": "Ventiel dicht! (%s/%s)",
    Z + "zs.route_weg": "Nee! Mijn ontsnappingsroute!",
    Z + "zs.weg": "De Ringvinger... zal... niet blij zijn...",
    # Donderbuik
    Z + "db.fase2": "Donderbuik rommelt!", Z + "db.fase2.onder": "Wolkduiveltjes uit zijn buik",
    Z + "db.fase3": "Stortbui!", Z + "db.fase3.onder": "Hij zuigt vaker en bliksemt dubbel",
    Z + "db.kussen": "Hij gaat zuigen! Spring op een roze wolkenkussen!",
    Z + "db.slag_ok": "Samen opgevangen!", Z + "db.slag_mis": "Niet genoeg in de gele cirkel!",
    Z + "db.blij": "Hihihi... het kriebelt... ik ben weer roze!",
}
TEKSTEN_EN = {
    Z + "mika.weg.0": "This goes in my report!", Z + "mika.weg.1": "The Ring Finger will hear of this!",
    Z + "mika.weg.2": "I was never here!", Z + "mika.weg.3": "My disguise! My beautiful disguise!",
    Z + "oefenpop.wakker": "The dummy moves!", Z + "vermomd.ontmaskerd": "Unmasked!",
    Z + "leider.strobaal": "Chief Dummy Strawbale", Z + "leider.mistmoeder": "Mother Mist", Z + "leider.meestervermommer": "Master of Disguise Feta",
    Z + "sparren.start": "On your marks... ready... spar! (dodge the red strip, don't hit the pink shield)",
    Z + "sparren.bezet": "This knight is already sparring with someone else.",
    Z + "sparren.eerst_klikken": "Right-click the knight first to spar.",
    Z + "sparren.geblokt": "Blocked!", Z + "sparren.kussenklap": "Pillow bonk! (%s/%s)",
    Z + "sparren.verloren.klappen": "Three pillow bonks: the knight wins this round. Practise a bit more!",
    Z + "sparren.verloren.weg": "The sparring bout is over.",
    Z + "sparren.gewonnen": "✦ Well fought! The knight bows. (Trial tokens: %s)",
    Z + "vangnet": "A little cloud catches you! Phew.", Z + "wolkkussen": "Cloud cushion!",
    Z + "set.stukken": "Cloudguard Set (%s/%s worn)",
    Z + "set.2": "(2) Cloudguard: Defence +10 and Vads +60",
    Z + "set.4": "(4) Knightly Duty: Damage to bosses +8%",
    Z + "set.6": "(6) Cloud Cushion: below 30% Vads a cloud catches you (Hug Shield 20% of your max Vads, 6 s; once per 45 s)",
}
GEBIEDEN = {
    "z5_proefarena": ("De Proefarena van de Wolkwacht", "The Cloudguard Trial Grounds"),
    "z5_buikgangen": ("De Buikgangen", "The Belly Tunnels"),
    "z5_schuilnest": ("Het Schuilnest", "The Hideout"),
    "z5_wolkrand_zuid": ("De Wolkrand: de Zuidvin", "The Cloud Edge: South Fin"),
    "z5_wolkrand_noord": ("De Wolkrand: de Noordvin", "The Cloud Edge: North Fin"),
    "z5_staartvlakte": ("De Staartvlakte", "The Tail Flats"),
}
HUISDIEREN = {
    "donderbuikje": ("Donderbuikje", "Een piepklein roze regenwolkje dat kriebelt als het regent.", "Versla Donderbuik op de Staartvlakte."),
    "warmpje": ("Warmpje", "Een wolkje warme lucht dat ontsnapte toen jij de kleppen opendraaide.", "Versla Opzichter Gorgonzola in de Buikgangen."),
    "lokpopje": ("Lokpopje", "Een van Zilverstaarts lokpoppen. Hij wil nu bij jou horen.", "Versla Kootje Zilverstaart."),
}


def teksten():
    """(nl, en) per key."""
    t = {k: (v, TEKSTEN_EN.get(k, v)) for k, v in TEKSTEN.items()}
    for k, (nl, en) in ENTITEITEN.items():
        t["entity.guhs." + k] = (nl, en)
    for k, (nl, en) in TIPS.items():
        t[Z + "tip." + k + ".titel"] = (nl, nl)
        t[Z + "tip." + k] = (en, en)
    for k, (titel, tip, tip2) in BAAS_TIPS.items():
        t[Z + "baas." + k] = (titel, ENTITEITEN["vr_z5_baas_" + k][1])
        t[Z + "baas." + k + ".onder"] = ({"zilverstaart": "De spion van de Ringvinger", "gorgonzola": "Kootje van de Kleppen",
                                          "donderbuik": "De Grauwe Onweerswolk"}[k],) * 2
        t["vadsrijk.z2bazen.tip." + k + ".titel"] = (titel, titel)
        t["vadsrijk.z2bazen.tip." + k] = (tip, tip)
        t["vadsrijk.z3bazen.tip." + k + ".titel"] = (titel, titel)
        t["vadsrijk.z3bazen.tip." + k] = (tip2, tip2)
    for k, (slot, nl, en, kleur, accent) in HARNAS.items():
        t["item.guhs.vr_z5_mob_" + k] = (nl, en)
        t["vadsrijk.loot." + k] = (nl, en)
    for k, (soort, nl, en, kleur, accent) in VIRTUEEL.items():
        t["item.guhs.vr_z5_mob_" + k] = (nl, en)
        t["vadsrijk.loot." + k] = (nl, en)
    for k, (nl, en, _i) in WAPENS.items():
        t["item.guhs.vr_z5_mob_" + k] = (nl, en)
        t["vadsrijk.loot." + k] = (nl, en)
    for k, regels in FLAVOUR.items():
        for i, r in enumerate(regels):
            t[f"vadsrijk.loot.{k}.flavour.{i}"] = ("\"" + r + "\"",) * 2
    for item, (typ, nl, en, _i, fl) in BAAS_ITEMS.items():
        t["item.guhs." + item] = (nl, en)
        t["vadsrijk.loot." + typ] = (nl, en)
        t[f"vadsrijk.loot.{typ}.flavour.0"] = ("\"" + fl + "\"",) * 2
    for b, (nl, en, _k) in TROFEEEN.items():
        t["item.guhs.vr_z5_baas_trofee_" + b] = (nl, en)
    for g, (nl, en) in GEBIEDEN.items():
        t["vadsrijk.kern.gebied." + g] = (nl, en)
    for h, (naam, tekst, bron) in HUISDIEREN.items():
        t[f"vadsrijk.huisdier.{h}.naam"] = (naam, naam)
        t[f"vadsrijk.huisdier.{h}.tekst"] = (tekst, tekst)
        t[f"vadsrijk.huisdier.{h}.bron"] = (bron, bron)
    return t


def en_of(s):
    return s


# =====================================================================================================================
# writing
# =====================================================================================================================
def controleer(naam, m, anims):
    bones = {b["name"] for b in m.bones}
    for an, a in anims.items():
        for b in a["bones"]:
            assert b in bones, f"{naam}: anim {an} moves unknown bone {b}"
    for nodig in NODIG[naam]:
        assert nodig in anims, f"{naam}: missing anim {nodig}"


def _item(a, w, naam, img, handheld=False):
    vw._png(os.path.join(a, "textures", "item", *SUB.split("/"), naam + ".png"), img)
    w(os.path.join(a, "items", naam + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + naam}})
    w(os.path.join(a, "models", "item", naam + ".json"), {"parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
                                                          "textures": {"layer0": f"guhs:item/{SUB}/{naam}"}})


def _doek_img(d):
    """Doek -> PIL image (Doek.png writes a file; we go through a temp array)."""
    a = np.clip(d.a, 0, 255).astype(np.uint8) if d.a.max() > 1.0 else (np.clip(d.a, 0, 1) * 255).astype(np.uint8)
    return Image.fromarray(a, "RGBA")


def assets(a, w=vw._schrijf_json):
    geo_dir = os.path.join(a, "geckolib", "models", "entity", *SUB.split("/"))
    anim_dir = os.path.join(a, "geckolib", "animations", "entity", *SUB.split("/"))
    tex_dir = os.path.join(a, "textures", *SUB.split("/"))
    n = 0
    for i, (naam, maak) in enumerate(WEZENS + BAZEN + PROPS):
        m, anims, pal, glow = maak()
        if (naam, maak) in WEZENS:
            anims = z2.leg_anims(m, anims)
        controleer(naam, m, anims)
        w(os.path.join(geo_dir, naam + ".geo.json"), m.geo())
        w(os.path.join(anim_dir, naam + ".animation.json"), anim_bestand(anims))
        tex = schilder(m, pal, 5200 + i)
        vw._png(os.path.join(tex_dir, naam + ".png"), tex)
        vw._png(os.path.join(tex_dir, naam + "_gloed.png"), schilder(m, pal, 5300 + i, glow, glow=True))
        vw._png(os.path.join(tex_dir, naam + "_elite.png"), vw.elite_gloed(tex))
        if naam == "donderbuik":
            for reg in m.regios:
                if reg["gezicht"]:
                    reg["gezicht"] = dict(reg["gezicht"], mood="blij")
            vw._png(os.path.join(tex_dir, "donderbuik_roze.png"), schilder(m, roze_versie(pal), 5400))
        if naam == "klep":
            dicht = dict(pal, wiel=GRAUW, pijp=((140, 142, 152), "strepen", (120, 122, 132)))
            vw._png(os.path.join(tex_dir, "klep_dicht.png"), schilder(m, dicht, 5401))
            gas = dict(pal, wiel=((110, 110, 120), "plekjes", (90, 90, 100)), pijp=((120, 122, 130), "plekjes", (96, 96, 106)))
            vw._png(os.path.join(tex_dir, "klep_gas.png"), schilder(m, gas, 5402))
        n += 1
    # the pets' shared animation (bones: root only)
    w(os.path.join(anim_dir, "huisdier.animation.json"), anim_bestand({
        "idle": anim(1.6, {"root": {"position": kf([(0, (0, 0, 0)), (0.8, (0, 0.8, 0)), (1.6, (0, 0, 0))])}}),
        "hop": anim(0.5, {"root": {"position": kf([(0, (0, 0, 0)), (0.25, (0, 3, 0)), (0.5, (0, 0, 0))])}}),
        "sprong": een(0.6, {"root": {"position": kf([(0, (0, 0, 0)), (0.3, (0, 6, 0)), (0.6, (0, 0, 0))]),
                                     "rotation": kf([(0, (0, 0, 0)), (0.6, (0, 360, 0))])}}),
    }))
    # items
    for i, (k, (slot, nl, en, kleur, accent)) in enumerate(HARNAS.items()):
        vw._png(os.path.join(a, "textures", "item", *SUB.split("/"), k + ".png"), vw.icoon(slot, kleur, accent, None, 5500 + i))
        w(os.path.join(a, "items", "vr_z5_mob_" + k + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/vr_z5_mob_" + k}})
        w(os.path.join(a, "models", "item", "vr_z5_mob_" + k + ".json"), {"parent": "minecraft:item/generated",
                                                                          "textures": {"layer0": f"guhs:item/{SUB}/{k}"}})
    for i, (k, (soort, nl, en, kleur, accent)) in enumerate(VIRTUEEL.items()):
        if soort in ("ring", "ketting", "oorbel"):
            img = z2.sieraad_icoon(soort, kleur, accent, 5550 + i)
        elif soort == "armband":
            img = armband_icoon(kleur, accent)
        elif soort == "handschoenen":
            img = handschoen_icoon(kleur, accent)
        else:
            img = cape_icoon(kleur, accent)
        _bewaar(os.path.join(a, "textures", "item", *SUB.split("/"), k + ".png"), img)
        w(os.path.join(a, "items", "vr_z5_mob_" + k + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/vr_z5_mob_" + k}})
        w(os.path.join(a, "models", "item", "vr_z5_mob_" + k + ".json"), {"parent": "minecraft:item/generated",
                                                                          "textures": {"layer0": f"guhs:item/{SUB}/{k}"}})
    for k, (nl, en, maak) in WAPENS.items():
        _item_doek(a, w, "vr_z5_mob_" + k, k, maak(), True)
    for item, (typ, nl, en, maak, fl) in BAAS_ITEMS.items():
        _item_doek(a, w, item, item, maak(), item not in ("vr_z5_baas_vermommingsmantel", "vr_z5_baas_regenjasje"))
    for b, (nl, en, kleur) in TROFEEEN.items():
        _item_doek(a, w, "vr_z5_baas_trofee_" + b, "trofee_" + b, trofee_icoon(kleur), False)
    return n


def _item_doek(a, w, item, tex, icoon, handheld):
    """icoon: whatever the shared icon makers return (a PIL image or a Doek)."""
    pad = os.path.join(a, "textures", "item", *SUB.split("/"), tex + ".png")
    _bewaar(pad, icoon)
    w(os.path.join(a, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
    w(os.path.join(a, "models", "item", item + ".json"), {"parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
                                                          "textures": {"layer0": f"guhs:item/{SUB}/{tex}"}})


def _bewaar(pad, icoon):
    if isinstance(icoon, Image.Image):
        vw._png(pad, icoon)
    elif hasattr(icoon, "png"):
        os.makedirs(os.path.dirname(pad), exist_ok=True)
        icoon.png(pad)
    else:
        vw._png(pad, Image.fromarray(np.asarray(icoon).astype(np.uint8)))


def uiterlijken(a, w=vw._schrijf_json):
    """The worn look of the 4 armour pieces (KernRegistry.harnasEigenschappen(slot, "vr_z5_mob_<id>")): vanilla iron tinted."""
    import zipfile
    from io import BytesIO
    jar = os.path.join("build", "moddev", "artifacts", "minecraft-patched-26.1.2.112.jar")
    bron = {}
    if os.path.exists(jar):
        with zipfile.ZipFile(jar) as z:
            for laag in ("humanoid", "humanoid_leggings"):
                bron[laag] = Image.open(BytesIO(z.read(f"assets/minecraft/textures/entity/equipment/{laag}/iron.png"))).convert("RGBA")
    for hid, (_slot, _nl, _en, kleur, _k2) in HARNAS.items():
        uid = "vr_z5_mob_" + hid
        w(os.path.join(a, "equipment", uid + ".json"), {"layers": {"humanoid": [{"texture": "guhs:" + uid}], "humanoid_leggings": [{"texture": "guhs:" + uid}]}})
        for laag in ("humanoid", "humanoid_leggings"):
            doel = os.path.join(a, "textures", "entity", "equipment", laag, uid + ".png")
            if laag not in bron:
                if not os.path.exists(doel):
                    print("vadsrijk_t_z5wezens: no minecraft jar (run a gradle build first) for", doel)
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
        h.lang(k, nl, en)


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
    print(f"vadsrijk_t_z5wezens: {n} models, {len(HARNAS) + len(VIRTUEEL) + len(WAPENS)} gear items, {len(BAAS_ITEMS)} boss items, {len(t)} texts")


if __name__ == "__main__":
    _direct()
