"""
1.10 "Het Vadsrijk", zone 3 "De Schemerwortel", slice wezens (Z3_BRIEF.md). Java: feature/vadsrijk/zone3/wezens.

Everything generated (no hand-made art), on the zone-1/zone-2 helpers (vadsrijk_wezens.Model/kf/anim, vadsrijk_z2_wezens builders):
  * GeckoLib models + animations (assets/guhs/geckolib/{models,animations}/entity/vadsrijk/zone3/wezens/):
      Kruinzee      bladrover (Mika sky pirate, hook), grauwe_rups (segmented grey caterpillar, curls into a ball),
                    bijtbloesem (rotten flower with a biting bulb)
      Takkenbanen   kaasschutter (Mika with a cheese musket), schorsmarter (lithe bark marten), grauwe_wesp (a swarm of 5 wasps)
      Schorsstad    schorskruiper (wooden bark spider), lantaarnloze (grey tree-folk ghost with a dead lantern),
                    harsgolem (grey resin golem, amber shards)
      Wortelvelden  rotzwijn (big boar, mould ridge, tusks), wortelslang (long root snake), mika_schimmelboer (Mika with a spray
                    tank), schimmelbolletje (the boer's little mould puff)
      Wortelduister wortelschim (faceless tall shadow with fleeting guh ears), dove_ziener (big blind root giant)
      critters      sporenmot, lantaarnkever, wortelmuis (the honey bee is the existing guh_bee model)
    Real monsters, not gory: glowing eyes, mould tufts, bark and resin; only the critters have small friendly eyes.
  * per creature: <naam>.png, <naam>_gloed.png (glowmask), <naam>_elite.png (the additive gold glow); per contract elite
    contract_<id>.png (the same model, its own colours) in textures/vadsrijk/zone3/wezens/.
  * 15 gear base types (items vr_z3_mob_*, icons, worn look), 8 trophies (vr_z3_mob_trofee_*), all Dutch texts.

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_z3_wezens.py
"""
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

try:
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2

Model, kf, anim, anim_bestand = vw.Model, vw.kf, vw.anim, vw.anim_bestand
samen, adem, zwaai, een, stil = z2.samen, z2.adem, z2.zwaai, z2.een, z2.stil
SUB = "vadsrijk/zone3/wezens"

GRAUW = ((132, 134, 146), "plekjes", (112, 116, 130))
SCHIMMEL = ((126, 150, 170), "plekjes", (96, 118, 140))     # Mika mould: grey-blue
PLUIS = ((196, 198, 206), "plekjes", (170, 174, 186))
SCHORS = ((104, 82, 62), "strepen", (84, 64, 48))
HARS = ((176, 132, 70), "glans", None)
AMBER = ((255, 150, 190), "facet", (230, 110, 160))


# =====================================================================================================================
# builders
# =====================================================================================================================
def keten(naam, n, seg, h, b, W=128, gezicht="beest", vanaf_staart=False, poten=True):
    """A segmented body along z (front = -z): bones s0..s{n-1}, each a child of the previous one (pivot at the joint),
    so a rotation on a joint bends everything behind it. vanaf_staart: s0 is the tail end and the head is the last one."""
    m = Model(naam, px=4, breedte=W, bounds=(3, 2, 0.6))
    m.bone("root")
    z0 = -n * seg / 2
    for i in range(n):
        k = (n - 1 - i) if vanaf_staart else i          # k = position from the front
        zf = z0 + k * seg                                # front edge of this segment
        joint = zf if not vanaf_staart else zf + seg     # joint with the previous bone in the chain
        ouder = "root" if i == 0 else f"s{i - 1}"
        piv = (0, h / 2, (zf + seg / 2) if i == 0 else joint)
        m.bone(f"s{i}", ouder, piv)
        kop = k == 0
        bb = b * (1.0 if kop else max(0.55, 1 - 0.06 * k))
        hh = h * (1.0 if kop else max(0.6, 1 - 0.05 * k))
        m.cube(f"s{i}", "huid", (-bb / 2, 0, zf), (bb, hh, seg + 0.4), gezicht={"mood": gezicht, "ey": 0.4} if kop else None)
        if poten and not kop:
            for sx in (-1, 1):
                m.cube(f"s{i}", "poot", (sx * (bb / 2) - (1 if sx < 0 else 0), -0.4, zf + seg / 2 - 0.5), (1, 1.4, 1))
    return m


def m_bladrover():
    m, a = z2.mika("bladrover", "houweel")
    m.cube("kop", "lap", (-3.9, 21.2, -4.9), (3.2, 2.6, 0.5))           # an eye patch
    m.cube("lijf", "jas", (-4, 9.2, 2.2), (8, 9, 1.2))                  # a coat tail
    m.cube("lijf", "sjerp", (-3.9, 13.5, -2.7), (7.8, 1.6, 5.4), rot=(0, 0, 20), pivot=(0, 14, 0))
    pal = {"vacht": ((200, 140, 162), "plekjes", (184, 126, 148)), "snuit": ((230, 172, 188), None, None), "oor": ((236, 146, 166), None, None),
           "poot": ((236, 166, 182), None, None), "vest": ((120, 40, 46), "strepen", (220, 196, 140)), "broek": ((60, 56, 70), None, None),
           "laars": ((40, 30, 26), None, None), "helm": ((36, 32, 40), None, None), "lamp": ((236, 220, 120), None, None),
           "steel": ((96, 70, 48), None, None), "ijzer": ((150, 152, 164), None, None), "lap": ((20, 18, 22), None, None),
           "jas": ((90, 30, 36), None, None), "sjerp": ((220, 180, 70), "strepen", (190, 140, 40))}
    return m, a, pal, {"lamp": (120, 100, 30)}


def m_kaasschutter():
    m, a = z2.mika("kaasschutter", "boor")
    m.cube("kop", "helm", (-2.5, 29.3, -2.5), (5, 3, 4.5))              # a tall hat
    m.cube("motor", "kaas", (-3.4, 11.4, 2.8), (6.8, 6.8, 4))           # a cheese barrel on the back
    pal = {"vacht": ((210, 150, 168), "plekjes", (194, 136, 154)), "snuit": ((236, 176, 192), None, None), "oor": ((240, 150, 170), None, None),
           "poot": ((240, 170, 186), None, None), "vest": ((60, 70, 120), "strepen", (230, 200, 90)), "broek": ((80, 66, 56), None, None),
           "laars": ((50, 40, 34), None, None), "helm": ((50, 46, 60), None, None), "lamp": ((255, 220, 90), None, None),
           "steel": ((80, 60, 44), None, None), "ijzer": ((110, 100, 90), None, None), "boor": ((70, 64, 60), "strepen", (50, 46, 44)),
           "kaas": ((250, 210, 90), "stippen", (220, 170, 60))}
    return m, a, pal, {"lamp": (160, 120, 30), "kaas": (40, 30, 0)}


def m_mika_schimmelboer():
    m, a = z2.mika("mika_schimmelboer", "boor")
    m.cube("kop", "helm", (-7, 25.3, -7), (14, 0.8, 13))                # a wide straw hat brim
    m.cube("motor", "tank", (-3.6, 10.6, 2.6), (7.2, 8.4, 4.6))         # the spray tank
    pal = {"vacht": ((196, 140, 162), "plekjes", (180, 126, 146)), "snuit": ((226, 168, 184), None, None), "oor": ((236, 146, 166), None, None),
           "poot": ((236, 166, 182), None, None), "vest": ((90, 110, 70), "strepen", (70, 86, 54)), "broek": ((70, 84, 110), None, None),
           "laars": ((44, 50, 40), None, None), "helm": ((222, 196, 120), "strepen", (200, 170, 96)), "lamp": ((180, 220, 240), None, None),
           "steel": ((60, 60, 66), None, None), "ijzer": ((120, 130, 140), None, None), "boor": ((110, 140, 160), None, None),
           "tank": SCHIMMEL}
    return m, a, pal, {"tank": (40, 60, 80), "lamp": (60, 90, 110)}


def m_grauwe_rups():
    m = keten("grauwe_rups", 6, 4.2, 7, 8, gezicht="beest")
    for i in (1, 3, 4):
        m.cube(f"s{i}", "pluis", (-1.5, 6, -12.6 + i * 4.2 + 1), (3, 2.2, 2.2))
    m.cube("s0", "tand", (-2.5, 0.6, -13.4), (1.2, 1.6, 1))
    m.cube("s0", "tand", (1.3, 0.6, -13.4), (1.2, 1.6, 1))
    golf = {f"s{i}": {"rotation": kf([(0, (0, 7 * (1 if i % 2 else -1), 0)), (0.4, (0, -7 * (1 if i % 2 else -1), 0)), (0.8, (0, 7 * (1 if i % 2 else -1), 0))])}
            for i in range(1, 6)}
    bal = {f"s{i}": {"rotation": kf([(0, (-58, 0, 0))])} for i in range(1, 6)}
    bal["s0"] = {"rotation": kf([(0, (25, 0, 0))])}
    rol = dict(bal)
    rol["root"] = {"rotation": kf([(0, (0, 0, 0)), (0.25, (-180, 0, 0)), (0.5, (-360, 0, 0))]), "position": kf([(0, (0, 3, 0))])}
    a = {
        "idle": anim(2.0, samen(adem("s2", 2.0, 1.03))),
        "loop": anim(0.8, samen(golf, {"root": {"position": kf([(0, (0, 0, 0)), (0.4, (0, 0.4, 0)), (0.8, (0, 0, 0))])}})),
        "aanval": een(0.4, {"s0": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-25, 0, 0)), (0.3, (15, 0, 0)), (0.4, (0, 0, 0))])}}),
        "bal": anim(1.0, bal),
        "rol": anim(0.5, rol),
        "duizelig": anim(0.8, {"s0": {"rotation": kf([(0, (0, 0, -15)), (0.4, (0, 0, 15)), (0.8, (0, 0, -15))])}}),
    }
    pal = {"huid": GRAUW, "poot": ((90, 92, 104), None, None), "pluis": PLUIS, "tand": ((210, 206, 196), None, None)}
    return m, a, pal, {}


def m_bijtbloesem():
    m = Model("bijtbloesem", px=4, breedte=96, bounds=(2, 3, 0.6))
    m.bone("root")
    m.bone("stam", "root", (0, 0, 0))
    m.cube("stam", "stengel", (-1.2, 0, -1.2), (2.4, 15, 2.4))
    for sx in (-1, 1):
        m.cube("stam", "blad", (sx * 1.2 - (7 if sx < 0 else 0), 3, -2.5), (7, 0.6, 5), rot=(0, 0, sx * -20), pivot=(sx * 1.2, 3, 0))
    m.bone("kop", "stam", (0, 15, 0))
    m.cube("kop", "bol", (-4.5, 15, -4.5), (9, 8, 9), gezicht={"mood": "beest", "ey": 0.38})
    m.bone("blaadjes", "kop", (0, 19, 0))
    for i in range(6):
        hk = i * 60 * np.pi / 180
        x, z = np.cos(hk) * 5.4, np.sin(hk) * 5.4
        m.cube("blaadjes", "bloesem", (x - 2.2, 17, z - 2.2), (4.4, 1, 4.4), rot=(20 * np.sin(hk), 0, -20 * np.cos(hk)), pivot=(x, 17, z))
    m.cube("kop", "meeldraad", (-1.5, 23, -1.5), (3, 2.4, 3))
    a = {
        "idle": anim(3.0, {"kop": {"rotation": kf([(0, (0, 0, -4)), (1.5, (0, 0, 4)), (3.0, (0, 0, -4))])}}),
        "loop": anim(1.0, stil()),
        "vermomd": anim(3.0, {"kop": {"rotation": kf([(0, (35, 0, 0)), (1.5, (32, 0, 3)), (3, (35, 0, 0))])},
                              "blaadjes": {"scale": kf([(0, (1.25, 1.6, 1.25))])}}),
        "blaas": anim(1.0, {"kop": {"rotation": kf([(0, (-30, 0, 0))]), "scale": kf([(0, (1.0, 1.0, 1.0)), (0.9, (1.15, 1.15, 1.15))])}}, loop="hold"),
        "spuug": een(0.5, {"kop": {"rotation": kf([(0, (-30, 0, 0)), (0.15, (25, 0, 0)), (0.5, (0, 0, 0))])}}),
        "speciaal": een(0.6, {"blaadjes": {"scale": kf([(0, (1.25, 1.6, 1.25)), (0.3, (0.8, 0.8, 0.8)), (0.6, (1, 1, 1))])}}),
        "aanval": een(0.4, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.12, (-20, 0, 0)), (0.25, (30, 0, 0)), (0.4, (0, 0, 0))])}}),
    }
    pal = {"stengel": ((84, 112, 74), "strepen", (70, 96, 62)), "blad": ((96, 124, 84), None, None), "bol": ((176, 150, 168), "plekjes", (150, 128, 148)),
           "bloesem": ((216, 176, 196), "plekjes", (176, 164, 176)), "meeldraad": ((200, 196, 120), None, None)}
    return m, a, pal, {"meeldraad": (60, 60, 20)}


def m_schorsmarter():
    m, a = z2.vierpoot("schorsmarter", lang=13, hoog=4.5, kop=4.5, poot=4, W=96)
    m.cube("staart", "vacht", (-1.4, 6, 6.5), (2.8, 2.6, 9))
    for sx in (-1, 1):
        m.cube("kop", "oor", (sx * 1.4 - 0.7, 9.5, -10.4), (1.4, 1.6, 0.8))
    a["sprong"] = anim(0.8, {"lijf": {"position": kf([(0, (0, -1.5, 0))]), "rotation": kf([(0, (-12, 0, 0))])},
                             "staart": {"rotation": kf([(0, (-50, 0, 0)), (0.4, (-60, 10, 0)), (0.8, (-50, 0, 0))])}})
    pal = {"vacht": SCHORS, "snuit": ((150, 130, 110), None, None), "poot": ((70, 56, 44), None, None), "oor": ((120, 100, 84), None, None)}
    return m, a, pal, {}


def m_grauwe_wesp():
    m = Model("grauwe_wesp", px=4, breedte=96, bounds=(3, 2, 0.6))
    m.bone("root")
    m.bone("zwerm", "root", (0, 8, 0))
    plekken = [(0, 9, -4), (5, 11, 1), (-5, 7, 2), (2, 5, 5), (-2, 13, -1)]
    for i, (x, y, z) in enumerate(plekken):
        b = f"w{i}"
        m.bone(b, "zwerm", (x, y, z))
        m.cube(b, "lijf", (x - 1, y - 1, z - 2), (2, 2, 4), gezicht={"mood": "spin", "ey": 0.4})
        m.cube(b, "angel", (x - 0.4, y - 0.6, z + 2), (0.8, 0.8, 1.2))
        m.cube(b, "vleugel", (x - 4, y + 1, z - 1), (3, 0.3, 2.4))
        m.cube(b, "vleugel", (x + 1, y + 1, z - 1), (3, 0.3, 2.4))
    bob = {f"w{i}": {"position": kf([(0, (0, 0, 0)), (0.25 + 0.05 * i, (0, 1.2, 0)), (0.5 + 0.1 * i, (0, 0, 0))])} for i in range(5)}
    a = {
        "idle": anim(2.0, samen(bob, {"zwerm": {"rotation": kf([(0, (0, 0, 0)), (1.0, (0, 180, 0)), (2.0, (0, 360, 0))])}})),
        "vlieg": anim(1.2, samen(bob, {"zwerm": {"rotation": kf([(0, (0, 0, 0)), (0.6, (0, 180, 0)), (1.2, (0, 360, 0))])}})),
        "aanval": een(0.4, {"zwerm": {"position": kf([(0, (0, 0, 0)), (0.15, (0, 0, -5)), (0.4, (0, 0, 0))]),
                                      "scale": kf([(0, (1, 1, 1)), (0.15, (0.7, 0.7, 0.7)), (0.4, (1, 1, 1))])}}),
    }
    pal = {"lijf": ((150, 148, 120), "strepen", (70, 68, 64)), "angel": ((60, 56, 56), None, None), "vleugel": ((210, 214, 224), None, None)}
    return m, a, pal, {}


def spin(naam):
    m = Model(naam, px=4, breedte=128, bounds=(3, 2, 0.5))
    m.bone("root")
    m.bone("lijf", "root", (0, 6, 0))
    m.cube("lijf", "vacht", (-4, 4, -5), (8, 5, 8))
    m.bone("achterlijf", "lijf", (0, 6, 3))
    m.cube("achterlijf", "vacht", (-5.5, 4, 3), (11, 8, 11))
    m.cube("achterlijf", "mos", (-4, 11.6, 5), (8, 1, 7))
    m.bone("kop", "lijf", (0, 6.5, -5))
    m.cube("kop", "vacht", (-3, 4.5, -10), (6, 4.5, 5), gezicht={"mood": "spin", "ey": 0.4})
    m.cube("kop", "tand", (-2, 3.2, -10.6), (1, 2, 1))
    m.cube("kop", "tand", (1, 3.2, -10.6), (1, 2, 1))
    for i in range(4):
        z = -3.5 + i * 2.6
        for kant, sx in (("l", -1), ("r", 1)):
            b = f"poot_{kant}{i}"
            m.bone(b, "lijf", (sx * 4, 6.5, z), (0, sx * (30 - i * 20), sx * -35))
            ox = -14 if sx < 0 else 4
            m.cube(b, "poot", (ox, 5.8, z - 0.6), (10, 1.3, 1.3))
    lp = {}
    for i in range(4):
        for kant, sx in (("l", -1), ("r", 1)):
            f = 1 if (i + (0 if sx < 0 else 1)) % 2 else -1
            lp[f"poot_{kant}{i}"] = {"rotation": kf([(0, (0, sx * (30 - i * 20) + 12 * f, sx * -35)), (0.2, (0, sx * (30 - i * 20) - 12 * f, sx * -30)),
                                                     (0.4, (0, sx * (30 - i * 20) + 12 * f, sx * -35))])}
    a = {
        "idle": anim(2.0, samen(adem("achterlijf", 2.0, 1.03))),
        "loop": anim(0.4, lp),
        "aanval": een(0.4, {"kop": {"rotation": kf([(0, (0, 0, 0)), (0.12, (-20, 0, 0)), (0.25, (20, 0, 0)), (0.4, (0, 0, 0))])}}),
        "spuug": een(0.5, {"achterlijf": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-35, 0, 0)), (0.5, (0, 0, 0))])}}),
        "verstopt": anim(2.0, {"root": {"scale": kf([(0, (1.25, 0.3, 1.25))]), "position": kf([(0, (0, -0.5, 0))])}}),
        "speciaal": een(0.5, {"root": {"scale": kf([(0, (1.25, 0.3, 1.25)), (0.2, (1, 1.2, 1)), (0.5, (1, 1, 1))]),
                                       "position": kf([(0, (0, 0, 0)), (0.2, (0, 4, 0)), (0.5, (0, 0, 0))])}}),
    }
    return m, a


def m_schorskruiper():
    m, a = spin("schorskruiper")
    pal = {"vacht": SCHORS, "poot": ((84, 66, 50), "strepen", (66, 52, 40)), "tand": ((190, 176, 150), None, None), "mos": ((96, 120, 70), "plekjes", (80, 104, 60))}
    return m, a, pal, {}


def m_lantaarnloze():
    m, a = z2.zwever("lantaarnloze", hoog=14, breed=7, gezicht="geest")
    m.bone("lantaarn", "arm_r", (5, 4, 0))
    m.cube("lantaarn", "ijzer", (4.6, 1, -0.4), (0.8, 3, 0.8))
    m.cube("lantaarn", "lamp", (3.5, -3, -1.5), (3, 4, 3))
    m.cube("lantaarn", "ijzer", (3.2, 1, -1.8), (3.6, 0.6, 3.6))
    a["blaas"] = anim(1.0, {"lijf": {"rotation": kf([(0, (-12, 0, 0))])}, "kop": {"scale": kf([(0, (1, 1, 1)), (0.9, (1.15, 1.1, 1.15))])}}, loop="hold")
    a["uit"] = anim(2.0, {"lantaarn": {"scale": kf([(0, (0.7, 0.7, 0.7))])}, "arm_r": {"rotation": kf([(0, (20, 0, 10))])},
                          "lijf": {"position": kf([(0, (0, -1, 0)), (1.0, (0, -0.4, 0)), (2.0, (0, -1, 0))])}})
    pal = {"mantel": ((120, 122, 134), "strepen", (100, 102, 116)), "kristal": ((150, 150, 160), "facet", (120, 120, 132)),
           "ijzer": ((70, 66, 64), None, None), "lamp": ((120, 116, 104), None, None)}
    return m, a, pal, {"kristal": (40, 50, 70), "mantel": (14, 20, 30)}


def m_harsgolem():
    m, a = z2.golem("harsgolem", 1.15)
    pal = {"steen": ((136, 120, 104), "glans", None), "donker": ((90, 74, 60), None, None), "kristal": ((214, 150, 70), "facet", (180, 116, 46))}
    return m, a, pal, {"kristal": (110, 60, 10), "donker": (30, 16, 4)}


def m_rotzwijn():
    m, a = z2.vierpoot("rotzwijn", lang=18, hoog=10, kop=8, poot=6, W=128)
    m.bone("rug", "lijf", (0, 16, 0))
    for i in range(5):
        m.cube("rug", "schimmel", (-2.5, 15.5, -7 + i * 3.4), (5, 2.5 - abs(i - 2) * 0.4, 3))
    for sx in (-1, 1):
        m.cube("kop", "tand", (sx * 2.4 - 0.6, 9, -18.4), (1.2, 3.6, 1.2), rot=(-25, 0, sx * 10), pivot=(sx * 2.4, 9, -18))
        m.cube("kop", "vacht", (sx * 3 - 1, 15, -11), (2, 2.6, 1), rot=(0, 0, sx * -20), pivot=(sx * 3, 15, -10.5))
    lk = lambda an, t: {"rotation": kf([(0, (an, 0, 0)), (t / 2, (-an, 0, 0)), (t, (an, 0, 0))])}
    a["schraap"] = anim(0.5, {"poot_lv": lk(40, 0.5), "kop": {"rotation": kf([(0, (18, 0, 0))])}, "lijf": {"rotation": kf([(0, (6, 0, 0))])}})
    a["storm"] = anim(0.3, {"poot_lv": lk(45, 0.3), "poot_ra": lk(45, 0.3), "poot_rv": lk(-45, 0.3), "poot_la": lk(-45, 0.3),
                            "kop": {"rotation": kf([(0, (20, 0, 0))])}})
    a["duizelig"] = anim(0.8, {"kop": {"rotation": kf([(0, (0, 0, -14)), (0.4, (0, 0, 14)), (0.8, (0, 0, -14))])}})
    pal = {"vacht": ((96, 84, 80), "plekjes", (78, 68, 66)), "snuit": ((150, 116, 110), None, None), "poot": ((60, 52, 50), None, None),
           "tand": ((236, 226, 200), None, None), "schimmel": SCHIMMEL}
    return m, a, pal, {"schimmel": (20, 30, 40)}


def m_wortelslang():
    m = keten("wortelslang", 8, 3.6, 3.6, 4.4, gezicht="beest", vanaf_staart=True, poten=False)
    m.cube("s7", "tand", (-1.2, -0.6, -15.2), (0.8, 1.2, 0.8))
    m.cube("s7", "tand", (0.4, -0.6, -15.2), (0.8, 1.2, 0.8))
    for i in (2, 4, 6):
        m.cube(f"s{i}", "wortel", (-0.6, 3.4, -14.4 + (7 - i) * 3.6 + 1), (1.2, 1.6, 1.2))
    golf = {f"s{i}": {"rotation": kf([(0, (0, 10 * (1 if i % 2 else -1), 0)), (0.5, (0, -10 * (1 if i % 2 else -1), 0)), (1.0, (0, 10 * (1 if i % 2 else -1), 0))])}
            for i in range(1, 8)}
    op = {"s5": {"rotation": kf([(0, (-30, 0, 0))])}, "s6": {"rotation": kf([(0, (-30, 0, 0))])}, "s7": {"rotation": kf([(0, (40, 0, 0)), (0.6, (35, 4, 0)), (1.2, (40, 0, 0))])}}
    a = {
        "idle": anim(2.0, samen(adem("s4", 2.0, 1.03))),
        "loop": anim(1.0, golf),
        "aanval": een(0.4, {"s7": {"rotation": kf([(0, (0, 0, 0)), (0.15, (-30, 0, 0)), (0.3, (20, 0, 0)), (0.4, (0, 0, 0))])}}),
        "onder": anim(2.0, {"root": {"position": kf([(0, (0, -14, 0))])}}),
        "omhoog": anim(1.2, op),
        "speciaal": een(0.6, {"root": {"position": kf([(0, (0, -12, 0)), (0.25, (0, 3, 0)), (0.6, (0, 0, 0))])}}),
        "spuug": een(0.6, {"root": {"position": kf([(0, (0, 0, 0)), (0.6, (0, -14, 0))])}}),
    }
    pal = {"huid": ((92, 74, 58), "strepen", (70, 56, 44)), "tand": ((220, 214, 196), None, None), "wortel": ((120, 96, 70), None, None)}
    return m, a, pal, {}


def m_wortelschim():
    m, a = z2.zwever("wortelschim", hoog=16, breed=6.5, gezicht=None)
    for sx in (-1, 1):
        m.cube("kop", "oor", (sx * 2.4 - 1.1, 25, -1), (2.2, 2.2, 1.2))
    a["loop"] = a["vlieg"]
    a["uithaal"] = anim(1.0, {"arm_r": {"rotation": kf([(0, (-120, 0, 20))])}, "arm_l": {"rotation": kf([(0, (-120, 0, -20))])},
                              "lijf": {"rotation": kf([(0, (15, 0, 0))])}})
    a["aanval"] = een(0.5, {"arm_r": {"rotation": kf([(0, (-120, 0, 20)), (0.2, (20, 0, 0)), (0.5, (0, 0, 0))])},
                            "arm_l": {"rotation": kf([(0, (-120, 0, -20)), (0.2, (20, 0, 0)), (0.5, (0, 0, 0))])}})
    pal = {"mantel": ((34, 30, 46), "strepen", (24, 22, 34)), "kristal": ((60, 54, 80), None, None), "oor": ((50, 44, 64), None, None)}
    return m, a, pal, {"oor": (90, 40, 80), "kristal": (40, 30, 70)}


def m_dove_ziener():
    m, a = z2.golem("dove_ziener", 1.5)
    for reg in m.regios:                                   # blind: no eyes at all
        reg["gezicht"] = None
    top = 10 * 1.5 + 12 * 1.5
    m.bone("wortels", "kop", (0, top + 7, 0))
    for (x, z, h, rx, rz) in ((-3, -1, 8, -10, 30), (3, -1, 9, -10, -30), (-1, 2, 7, 25, 10), (2, 2, 6, 30, -15)):
        m.cube("wortels", "wortel", (x - 0.8, top + 6, z - 0.8), (1.6, h, 1.6), rot=(rx, 0, rz), pivot=(x, top + 6, z))
    a["brul"] = anim(0.6, {"kop": {"rotation": kf([(0, (-30, 0, 0)), (0.3, (-34, 0, 3)), (0.6, (-30, 0, 0))])},
                           "arm_l": {"rotation": kf([(0, (-100, 0, 30))])}, "arm_r": {"rotation": kf([(0, (-100, 0, -30))])}})
    pal = {"steen": ((96, 78, 62), "strepen", (78, 62, 50)), "donker": ((60, 48, 40), None, None), "kristal": ((140, 120, 160), "plekjes", (110, 96, 130)),
           "wortel": ((110, 88, 64), None, None)}
    return m, a, pal, {"kristal": (60, 30, 90)}


def m_schimmelbolletje():
    m = Model("schimmelbolletje", px=4, breedte=64, bounds=(1, 1, 0.3))
    m.bone("root")
    m.bone("lijf", "root", (0, 0, 0))
    m.cube("lijf", "huid", (-3.5, 0, -3.5), (7, 6, 7), gezicht={"mood": "beest", "ey": 0.45})
    m.cube("lijf", "hoed", (-4.5, 5.5, -4.5), (9, 2.5, 9))
    a = {
        "idle": anim(1.5, adem("lijf", 1.5, 1.06)),
        "hop": anim(0.5, {"lijf": {"position": kf([(0, (0, 0, 0)), (0.2, (0, 3, 0)), (0.5, (0, 0, 0))]),
                                   "scale": kf([(0, (1.1, 0.9, 1.1)), (0.2, (0.95, 1.1, 0.95)), (0.5, (1.1, 0.9, 1.1))])}}),
        "loop": anim(0.5, {"lijf": {"position": kf([(0, (0, 0, 0)), (0.2, (0, 3, 0)), (0.5, (0, 0, 0))])}}),
    }
    pal = {"huid": ((150, 170, 186), "plekjes", (126, 148, 166)), "hoed": ((96, 118, 140), "stippen", (196, 206, 216))}
    return m, a, pal, {}


# critters (passive, small friendly eyes: the guh touch)
def m_sporenmot():
    m, a = z2.vlieger("sporenmot", lijf=(2, 2, 4), vleugel=(5, 0.3, 5), kop=2, W=64, gezicht="lief")
    a["loop"] = a["vlieg"]
    pal = {"vacht": ((170, 140, 200), None, None), "vleugel": ((196, 160, 230), "plekjes", (150, 220, 210))}
    return m, a, pal, {"vleugel": (60, 90, 100), "vacht": (40, 30, 60)}


def m_lantaarnkever():
    m, a = z2.vlieger("lantaarnkever", lijf=(2.5, 2, 3.5), vleugel=(3, 0.3, 3), kop=2, W=64, gezicht="lief")
    m.cube("lijf", "lampje", (-1.2, 4.3, 1.75), (2.4, 1.6, 2))
    a["loop"] = a["vlieg"]
    pal = {"vacht": ((70, 60, 80), None, None), "vleugel": ((120, 100, 130), None, None), "lampje": ((255, 214, 120), None, None)}
    return m, a, pal, {"lampje": (255, 200, 100)}


def m_wortelmuis():
    m, a = z2.vierpoot("wortelmuis", lang=5, hoog=3.5, kop=3, poot=1.5, W=64, gezicht="lief", staart=True)
    for sx in (-1, 1):
        m.cube("kop", "oor", (sx * 1.4 - 1, 5.6, -4.6), (2, 2, 0.5))
    pal = {"vacht": ((160, 132, 110), None, None), "snuit": ((220, 190, 180), None, None), "poot": ((236, 190, 186), None, None),
           "oor": ((240, 180, 186), None, None)}
    return m, a, pal, {}


WEZENS = [("bladrover", m_bladrover), ("grauwe_rups", m_grauwe_rups), ("bijtbloesem", m_bijtbloesem), ("kaasschutter", m_kaasschutter),
          ("schorsmarter", m_schorsmarter), ("grauwe_wesp", m_grauwe_wesp), ("schorskruiper", m_schorskruiper), ("lantaarnloze", m_lantaarnloze),
          ("harsgolem", m_harsgolem), ("rotzwijn", m_rotzwijn), ("wortelslang", m_wortelslang), ("mika_schimmelboer", m_mika_schimmelboer),
          ("wortelschim", m_wortelschim), ("dove_ziener", m_dove_ziener), ("schimmelbolletje", m_schimmelbolletje)]
CRITTERS = [("sporenmot", m_sporenmot), ("lantaarnkever", m_lantaarnkever), ("wortelmuis", m_wortelmuis)]

# animations the Java code triggers or holds, per creature (the generator checks they exist)
NODIG = {
    "bladrover": ["idle", "loop", "aanval", "mik", "spuug", "blij"],
    "grauwe_rups": ["idle", "loop", "aanval", "bal", "rol", "duizelig", "blij"],
    "bijtbloesem": ["idle", "vermomd", "blaas", "spuug", "speciaal", "aanval", "blij"],
    "kaasschutter": ["idle", "loop", "aanval", "mik", "spuug", "blij"],
    "schorsmarter": ["idle", "loop", "aanval", "sprong", "blij"],
    "grauwe_wesp": ["idle", "vlieg", "aanval", "blij"],
    "schorskruiper": ["idle", "loop", "aanval", "verstopt", "speciaal", "spuug", "blij"],
    "lantaarnloze": ["idle", "vlieg", "spreuk", "speciaal", "blaas", "uit", "aanval", "blij"],
    "harsgolem": ["idle", "loop", "aanval", "hef", "klap", "speciaal", "blij"],
    "rotzwijn": ["idle", "loop", "aanval", "schraap", "storm", "duizelig", "blij"],
    "wortelslang": ["idle", "loop", "aanval", "onder", "omhoog", "speciaal", "spuug", "blij"],
    "mika_schimmelboer": ["idle", "loop", "aanval", "mik", "spuug", "blij"],
    "wortelschim": ["idle", "loop", "aanval", "uithaal", "blij"],
    "dove_ziener": ["idle", "loop", "aanval", "hef", "klap", "brul", "blij"],
    "schimmelbolletje": ["idle", "hop", "aanval", "blij"],
    "sporenmot": ["idle", "loop", "vlieg"], "lantaarnkever": ["idle", "loop", "vlieg"], "wortelmuis": ["idle", "loop"],
}

# the contract elites: the same model as their kind, their own colours (palette roles overridden)
CONTRACT = {
    "ouwe_rotkeiler": ("rotzwijn", {"vacht": ((70, 62, 64), "plekjes", (52, 46, 48)), "schimmel": ((90, 120, 150), "stippen", (200, 214, 230)),
                                    "tand": ((255, 236, 170), None, None)}),
    "bramzwijn_de_brede": ("rotzwijn", {"vacht": ((88, 70, 52), "strepen", (66, 52, 40)), "schimmel": ((70, 110, 50), "stippen", (150, 40, 80)),
                                        "tand": ((240, 240, 230), None, None)}),
    "moeder_wesp": ("grauwe_wesp", {"lijf": ((170, 150, 90), "strepen", (40, 36, 40)), "vleugel": ((236, 226, 200), None, None)}),
    "de_harsreus": ("harsgolem", {"steen": ((150, 108, 70), "glans", None), "kristal": ((255, 120, 180), "facet", (220, 80, 140))}),
    "kapitein_kaaskop": ("kaasschutter", {"vest": ((30, 30, 60), "strepen", (240, 210, 90)), "helm": ((250, 214, 90), "stippen", (220, 170, 60)),
                                          "kaas": ((255, 222, 100), "stippen", (230, 180, 70))}),
    "grootvader_rups": ("grauwe_rups", {"huid": ((104, 106, 118), "plekjes", (84, 86, 98)), "pluis": ((236, 236, 240), "plekjes", (210, 210, 220))}),
    "de_stille_ziener": ("dove_ziener", {"steen": ((64, 58, 72), "strepen", (50, 44, 58)), "wortel": ((150, 120, 200), None, None)}),
    "slangenkoningin_sapsie": ("wortelslang", {"huid": ((50, 140, 130), "strepen", (36, 110, 100)), "wortel": ((250, 210, 90), None, None)}),
}


# =====================================================================================================================
# gear
# =====================================================================================================================
HARNAS = {
    # id: (slot, name, colour, accent, set)
    "bloesemkap": ("hoofd", "Bloesemkap", (240, 176, 206), (120, 200, 110), "bloesem"),
    "bloesemjak": ("borst", "Bloesemjak", (236, 168, 200), (250, 220, 120), "bloesem"),
    "bloesembroek": ("benen", "Bloesembroek", (210, 150, 186), (120, 200, 110), "bloesem"),
    "bloesemtrippers": ("voeten", "Bloesemtrippers", (246, 196, 220), (250, 220, 120), "bloesem"),
    "schorshelm": ("hoofd", "Schorshelm", (120, 92, 68), (255, 150, 190), "schors"),
    "schorspantser": ("borst", "Schorspantser", (110, 84, 62), (255, 150, 190), "schors"),
    "schorsbeenstukken": ("benen", "Schorsbeenstukken", (104, 80, 60), (214, 150, 70), "schors"),
    "schorslaarzen": ("voeten", "Schorslaarzen", (96, 74, 56), (214, 150, 70), "schors"),
    "wortelkroon": ("hoofd", "Wortelkroon", (130, 104, 150), (90, 220, 210), "wortel"),
    "wortelmantel": ("borst", "Wortelmantel", (96, 80, 120), (255, 120, 190), "wortel"),
    "wortelbroek": ("benen", "Wortelbroek", (90, 76, 104), (90, 220, 210), "wortel"),
    "wortelstappers": ("voeten", "Wortelstappers", (84, 68, 60), (255, 120, 190), "wortel"),
}
SIERADEN = {
    "honingring": ("ring", "Honingring", (240, 196, 80), (255, 230, 140)),
    "harsketting": ("ketting", "Harsketting", (200, 150, 90), (255, 140, 190)),
    "sporenoorbel": ("oorbel", "Sporenoorbel", (150, 130, 190), (120, 230, 210)),
}
TROFEEEN = {
    "ouwe_rotkeiler": ("Slagtand van Ouwe Rotkeiler", (236, 226, 200)),
    "moeder_wesp": ("Angel van Moeder Wesp", (200, 180, 90)),
    "de_harsreus": ("Harshart van De Harsreus", (255, 140, 190)),
    "kapitein_kaaskop": ("Kaaskop's Kapiteinshoed", (250, 214, 90)),
    "grootvader_rups": ("Pluk Grootvaderpluis", (230, 230, 236)),
    "de_stille_ziener": ("Wortelkroon van De Stille Ziener", (150, 120, 200)),
    "slangenkoningin_sapsie": ("Sapsie's Kroontje", (60, 200, 190)),
    "bramzwijn_de_brede": ("Bramtak van Bramzwijn", (150, 40, 80)),
}


def trofee_icoon(kleur, seed):
    """A little gold plaque with the trophy's colour in a round frame."""
    r = vw._rng(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(2, 2), (13, 2), (13, 10), (7, 14), (2, 10)], fill=(200, 160, 60, 255), outline=(120, 90, 30, 255))
    k = tuple(int(c) for c in kleur) + (255,)
    d.ellipse((4, 4, 11, 10), fill=k, outline=(90, 70, 30, 255))
    a = np.asarray(img).copy()
    m = a[..., 3] > 0
    a[m, :3] = np.clip(a[m, :3].astype(int) + r.normal(0, 5, (m.sum(), 3)).astype(int), 0, 255)
    return Image.fromarray(a)


ENTITEITEN = {
    "bladrover": "Bladrover", "grauwe_rups": "Grauwe Rups", "bijtbloesem": "Bijtbloesem", "kaasschutter": "Kaasschutter",
    "schorsmarter": "Schorsmarter", "grauwe_wesp": "Grauwe Wespenzwerm", "schorskruiper": "Schorskruiper", "lantaarnloze": "Lantaarnloze",
    "harsgolem": "Harsgolem", "rotzwijn": "Rotzwijn", "wortelslang": "Wortelslang", "mika_schimmelboer": "Mika-schimmelboer",
    "schimmelbolletje": "Schimmelbolletje", "wortelschim": "Wortelschim", "dove_ziener": "De Dove Ziener",
    "honingbij": "Honingbij", "sporenmot": "Sporenmot", "lantaarnkever": "Lantaarnkever", "wortelmuis": "Wortelmuis",
}

FLAVOUR = {
    "bloesemkap": ["Ruikt naar lente. En een beetje naar rups.", "Er waait altijd een blaadje vanaf.", "Een bij dacht dat je een bloem was."],
    "bloesemjak": ["Licht als een blaadje, taai als een tak.", "Geplukt in de Kruinzee, gestreken door een imkerguh.", "Zoemt zacht als je rent."],
    "bloesembroek": ["Met zakjes vol stuifmeel. Niet niezen.", "Roze knieën, stoere benen.", "Getest op honderd takken."],
    "bloesemtrippers": ["Je laat bloemblaadjes achter. Romantisch.", "Trip, trap, takkenbaan.", "Zacht genoeg om een rups niet wakker te maken."],
    "schorshelm": ["Een stukje van de oudste boom van de wereld.", "Klopt iemand op je hoofd? Dat hoor je niet.", "Ruikt naar hars en avontuur."],
    "schorspantser": ["Jaarringen inbegrepen.", "Van binnen nog warm van de stam.", "Het boomvolk zou trots zijn. Denken we."],
    "schorsbeenstukken": ["Stevig als een wortel, buigzaam als een tak.", "Kraakt een beetje. Dat hoort zo.", "Er zit een harsdruppel op. Plakt niet. Meestal."],
    "schorslaarzen": ["Voor wie ergens wil blijven staan.", "Zelfs een Rotzwijn duwt je niet om. Bijna niet.", "Stampen mag."],
    "wortelkroon": ["Er groeit een klein roze aderje in.", "Gevlochten door iets wat al heel lang slaapt.", "In het donker gloeit hij zachtjes."],
    "wortelmantel": ["Ruikt naar aarde en een beetje naar ei.", "Warm als een wortel in de winter.", "Schimmel glijdt er gewoon vanaf."],
    "wortelbroek": ["Voor diep onder de grond. Of de bank.", "Wortels vinden de weg. Jij nu ook.", "Met een zakje voor een lantaarnkever."],
    "wortelstappers": ["Je voeten vinden de grond, ook in het donker.", "Sluipen gaat vanzelf. Fijn bij een Dove Ziener.", "Wortelzacht."],
    "honingring": ["Plakt een beetje. Dat is liefde.", "Een druppel honing, gevangen in goud.", "De bijen zoemen als je hem draagt."],
    "harsketting": ["Er zit een klein roze kristalletje in de hars.", "Ruikt naar dennenbos en moed.", "Hard als een golem, mooi als een zonsondergang."],
    "sporenoorbel": ["Gloeit zacht in het donker.", "Een sporenmot had hem verloren. Nu niet meer.", "Je hoort het bos fluisteren."],
}

TEKSTEN = {
    "vadsrijk.z3wezens.te_donker": "Te donker!",
    "vadsrijk.z3wezens.zwakke_plek": "Zwakke plek!",
    "vadsrijk.z3wezens.beslopen": "Beslopen! x2",
    "vadsrijk.z3wezens.harsschild": "Harsschild!",
    "vadsrijk.z3wezens.lantaarn.aan": "Lantaarn aan!",
    "vadsrijk.z3wezens.harsgolem.kristal": "*KRAK* Amberroze!",
    "vadsrijk.z3wezens.schimmelboer.roep": "Groei, mijn bolletjes!",
    "vadsrijk.z3wezens.moeder_wesp.splits": "ZZZWERMEN!",
    "vadsrijk.z3wezens.ziener.brul": "HRRROOOAAAR!",
    "vadsrijk.z3wezens.ziener.kwijt": "...?",
    "vadsrijk.z3wezens.mika.weg.0": "Dit komt in mijn rapport!",
    "vadsrijk.z3wezens.mika.weg.1": "Terug naar de zeppelin! TERUG!",
    "vadsrijk.z3wezens.mika.weg.2": "De Graaf hoort hiervan!",
    "vadsrijk.z3wezens.mika.weg.3": "Ik kom terug! Met meer kaas!",
    "vadsrijk.z3wezens.set.bloesem": "Bloesemset",
    "vadsrijk.z3wezens.set.schors": "Schorsset",
    "vadsrijk.z3wezens.set.wortel": "Wortelset",
    "vadsrijk.z3wezens.set.stukken": "Set: %s (%s/4 gedragen)",
    "vadsrijk.z3wezens.set.bloesem.2": "(2) Bloesemvlucht: +6 Kritiek",
    "vadsrijk.z3wezens.set.bloesem.4": "(4) Stuifmeel: je kritieke klappen maken het wezen Gesporen",
    "vadsrijk.z3wezens.set.schors.2": "(2) Schorshuid: +12 Verdediging",
    "vadsrijk.z3wezens.set.schors.4": "(4) Harsschild: zak je onder 50%% Vads, dan krijg je een harsschild (1x per 30 s)",
    "vadsrijk.z3wezens.set.wortel.2": "(2) Wortelvast: +8 Elementweerstand",
    "vadsrijk.z3wezens.set.wortel.4": "(4) Wortellicht: je geeft zelf licht in het Wortelduister en doet +10%% op rotwezens",
    "vadsrijk.z3wezens.leider.rupsenmoeder": "Rupsenmoeder",
    "vadsrijk.z3wezens.leider.kapitein_bladrover": "Kapitein-Bladrover",
    "vadsrijk.z3wezens.leider.harskolos": "Harskolos",
    "vadsrijk.z3wezens.leider.wilde_rotkeiler": "Wilde Rotkeiler",
    "vadsrijk.z3wezens.leider.ziener_oudste": "Ziener-oudste",
    "vadsrijk.z3wezens.contract.ouwe_rotkeiler": "Ouwe Rotkeiler",
    "vadsrijk.z3wezens.contract.moeder_wesp": "Moeder Wesp",
    "vadsrijk.z3wezens.contract.de_harsreus": "De Harsreus",
    "vadsrijk.z3wezens.contract.kapitein_kaaskop": "Kapitein Kaaskop",
    "vadsrijk.z3wezens.contract.grootvader_rups": "Grootvader Rups",
    "vadsrijk.z3wezens.contract.de_stille_ziener": "De Stille Ziener",
    "vadsrijk.z3wezens.contract.slangenkoningin_sapsie": "Slangenkoningin Sapsie",
    "vadsrijk.z3wezens.contract.bramzwijn_de_brede": "Bramzwijn de Brede",
    "vadsrijk.z3wezens.contract.hulp": "Hulpje",
    "vadsrijk.z3wezens.contract.verslagen": "Jachtdoelwit verslagen: %s!",
    "vadsrijk.z3wezens.contract.vertrokken": "%s is weer verdwenen. Neem het contract opnieuw aan op het Jachtbord.",
    "vadsrijk.z3wezens.cmd.gespawnd": "%s gemaakt (level %s%s).",
    "vadsrijk.z3wezens.cmd.onbekend": "Dat ken ik niet: %s",
    "vadsrijk.z3wezens.cmd.set": "Set %s gegeven (level %s).",
    "vadsrijk.z3wezens.cmd.loot": "Loot van %s (level %s): %s items, %s brons.",
    "vadsrijk.z3wezens.cmd.spawners": "%s zone-3-spawners; max levend per laag: %s",
    "vadsrijk.z3wezens.cmd.opgeruimd": "%s zone-3-wezens opgeruimd.",
    "vadsrijk.z3wezens.cmd.alle": "%s wezens op een rij gezet.",
    "vadsrijk.z3wezens.cmd.contract": "%s staat klaar op %s %s %s.",
    "vadsrijk.z3wezens.cmd.licht": "Licht hier: %s (Wortelduister: %s, jij draagt licht: %s)",
    "vadsrijk.z3wezens.tip.donker.titel": "Te donker!",
    "vadsrijk.z3wezens.tip.donker": "Buiten het licht zie je alleen een schim, en je klappen doen maar half zoveel. Vecht in het licht van je Nestlantaarn of bij de roze aders!",
    "vadsrijk.z3wezens.tip.bladrover.titel": "Bladrover",
    "vadsrijk.z3wezens.tip.bladrover": "Een rode streep naar jou = zijn enterhaak. Stap uit de streep, anders trekt hij je naar zich toe.",
    "vadsrijk.z3wezens.tip.grauwe_rups.titel": "Grauwe Rups",
    "vadsrijk.z3wezens.tip.grauwe_rups": "Rolt hij zich op? Opzij! Na het rollen is hij even duizelig: dan doe je extra schade.",
    "vadsrijk.z3wezens.tip.bijtbloesem.titel": "Bijtbloesem",
    "vadsrijk.z3wezens.tip.bijtbloesem": "Niet elke bloem is lief. Ze loopt niet weg: van een afstand ben je veilig. Gele kegel = stuifmeel.",
    "vadsrijk.z3wezens.tip.kaasschutter.titel": "Kaasschutter",
    "vadsrijk.z3wezens.tip.kaasschutter": "De rode streep is waar zijn kaaskogel heen gaat. Ren naar hem toe: van dichtbij slaat hij op de vlucht.",
    "vadsrijk.z3wezens.tip.schorsmarter.titel": "Schorsmarter",
    "vadsrijk.z3wezens.tip.schorsmarter": "Ze komen per twee en springen van boven op je. Rode cirkel = daar landt hij.",
    "vadsrijk.z3wezens.tip.grauwe_wesp.titel": "Grauwe Wespenzwerm",
    "vadsrijk.z3wezens.tip.grauwe_wesp": "Eén zwerm, één levensbalk. Klappen die meer wezens tegelijk raken werken hier het best.",
    "vadsrijk.z3wezens.tip.schorskruiper.titel": "Schorskruiper",
    "vadsrijk.z3wezens.tip.schorskruiper": "Die schors bewoog! Schorskruipers springen uit de wand als je te dichtbij komt. Pas op voor hun web.",
    "vadsrijk.z3wezens.tip.lantaarnloze.titel": "Lantaarnloze",
    "vadsrijk.z3wezens.tip.lantaarnloze": "Blaast hij het licht uit? Klik op hem om zijn lantaarn weer aan te steken: dan staat hij even stil.",
    "vadsrijk.z3wezens.tip.harsgolem.titel": "Harsgolem",
    "vadsrijk.z3wezens.tip.harsgolem": "Uit de rode cirkel blijven! Breekt er een roze kristal uit zijn rug, sla dan van achteren: x1,5.",
    "vadsrijk.z3wezens.tip.rotzwijn.titel": "Rotzwijn",
    "vadsrijk.z3wezens.tip.rotzwijn": "Schraapt hij met zijn poot? Dan stormt hij langs de rode streep. Stap opzij, en laat hem tegen een muur knallen.",
    "vadsrijk.z3wezens.tip.wortelslang.titel": "Wortelslang",
    "vadsrijk.z3wezens.tip.wortelslang": "Een stofspoor naar je toe = hij komt onder je omhoog. Raak hem als hij boven is!",
    "vadsrijk.z3wezens.tip.mika_schimmelboer.titel": "Mika-schimmelboer",
    "vadsrijk.z3wezens.tip.mika_schimmelboer": "Zijn schimmelvelden maken je Gesporen. Versla hem en zijn velden verdwijnen mee.",
    "vadsrijk.z3wezens.tip.wortelschim.titel": "Wortelschim",
    "vadsrijk.z3wezens.tip.wortelschim": "In het donker sluipt hij achter je. Blijf in het licht, dan zie je hem en raak je hem goed.",
    "vadsrijk.z3wezens.tip.dove_ziener.titel": "De Dove Ziener",
    "vadsrijk.z3wezens.tip.dove_ziener": "Hij is blind maar hoort alles. Sluip (shift) naar hem toe: je eerste klap doet dan dubbel.",
    "vadsrijk.z3wezens.tip.schimmelbolletje.titel": "Schimmelbolletje",
    "vadsrijk.z3wezens.tip.schimmelbolletje": "Klein, maar het stuift. Pak eerst de schimmelboer!",
}


def teksten():
    t = dict(TEKSTEN)
    for k, n in ENTITEITEN.items():
        t[f"entity.guhs.vr_z3_mob_{k}"] = n
    for k, (slot, naam, kleur, accent, s) in HARNAS.items():
        t[f"item.guhs.vr_z3_mob_{k}"] = naam
        t[f"vadsrijk.loot.{k}"] = naam
    for k, (soort, naam, kleur, steen) in SIERADEN.items():
        t[f"item.guhs.vr_z3_mob_{k}"] = naam
        t[f"vadsrijk.loot.{k}"] = naam
    for k, (naam, kleur) in TROFEEEN.items():
        t[f"item.guhs.vr_z3_mob_trofee_{k}"] = naam
    for k, regels in FLAVOUR.items():
        for i, regel in enumerate(regels):
            t[f"vadsrijk.loot.{k}.flavour.{i}"] = "\"" + regel + "\""
    return t


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


def assets(a, w=vw._schrijf_json):
    geo_dir = os.path.join(a, "geckolib", "models", "entity", *SUB.split("/"))
    anim_dir = os.path.join(a, "geckolib", "animations", "entity", *SUB.split("/"))
    tex_dir = os.path.join(a, "textures", *SUB.split("/"))
    n = 0
    makers = dict(WEZENS + CRITTERS)
    for i, (naam, maak) in enumerate(WEZENS + CRITTERS):
        m, anims, pal, glow = maak()
        if (naam, maak) in WEZENS:
            anims = z2.leg_anims(m, anims)
        controleer(naam, m, anims)
        w(os.path.join(geo_dir, naam + ".geo.json"), m.geo())
        w(os.path.join(anim_dir, naam + ".animation.json"), anim_bestand(anims))
        tex = z2.schilder(m, pal, 3200 + i)
        vw._png(os.path.join(tex_dir, naam + ".png"), tex)
        vw._png(os.path.join(tex_dir, naam + "_gloed.png"), z2.schilder(m, pal, 3300 + i, glow, glow=True))
        if (naam, maak) in WEZENS:
            vw._png(os.path.join(tex_dir, naam + "_elite.png"), vw.elite_gloed(tex))
        n += 1
    for j, (cid, (basis, extra)) in enumerate(CONTRACT.items()):
        m, anims, pal, glow = makers[basis]()
        p2 = dict(pal)
        p2.update(extra)
        vw._png(os.path.join(tex_dir, "contract_" + cid + ".png"), z2.schilder(m, p2, 3400 + j))
    item_tex = os.path.join(a, "textures", "item", *SUB.split("/"))
    for i, (k, (slot, naam, kleur, accent, s)) in enumerate(HARNAS.items()):
        vw._png(os.path.join(item_tex, k + ".png"), vw.icoon(slot, kleur, accent, "zwam" if s == "wortel" else "klodder" if s == "schors" else None, 3500 + i))
    for i, (k, (soort, naam, kleur, steen)) in enumerate(SIERADEN.items()):
        vw._png(os.path.join(item_tex, k + ".png"), z2.sieraad_icoon(soort, kleur, steen, 3550 + i))
    for i, (k, (naam, kleur)) in enumerate(TROFEEEN.items()):
        vw._png(os.path.join(item_tex, "trofee_" + k + ".png"), trofee_icoon(kleur, 3600 + i))
    namen = list(HARNAS) + list(SIERADEN) + ["trofee_" + k for k in TROFEEEN]
    for k in namen:
        item = "vr_z3_mob_" + k
        w(os.path.join(a, "items", item + ".json"), {"model": {"type": "minecraft:model", "model": "guhs:item/" + item}})
        w(os.path.join(a, "models", "item", item + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{SUB}/{k}"}})
    return n


def uiterlijken(a, w=vw._schrijf_json):
    """The worn look of every armour piece (KernRegistry.harnasEigenschappen(slot, "vr_z3_mob_<id>")): vanilla iron tinted."""
    import zipfile
    from io import BytesIO
    jar = os.path.join("build", "moddev", "artifacts", "minecraft-patched-26.1.2.112.jar")
    bron = {}
    if os.path.exists(jar):
        with zipfile.ZipFile(jar) as z:
            for laag in ("humanoid", "humanoid_leggings"):
                bron[laag] = Image.open(BytesIO(z.read(f"assets/minecraft/textures/entity/equipment/{laag}/iron.png"))).convert("RGBA")
    for hid, (_slot, _naam, kleur, _k2, _set) in HARNAS.items():
        uid = "vr_z3_mob_" + hid
        w(os.path.join(a, "equipment", uid + ".json"), {"layers": {"humanoid": [{"texture": "guhs:" + uid}], "humanoid_leggings": [{"texture": "guhs:" + uid}]}})
        for laag in ("humanoid", "humanoid_leggings"):
            doel = os.path.join(a, "textures", "entity", "equipment", laag, uid + ".png")
            if laag not in bron:
                if not os.path.exists(doel):
                    print("vadsrijk_z3_wezens: no minecraft jar (run a gradle build first) for", doel)
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
    for k, nl in teksten().items():
        h.lang(k, nl, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    n = assets(a)
    uiterlijken(a)
    t = teksten()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(t)
        vw._schrijf_json(p, data)
    print(f"vadsrijk_z3_wezens: {n} models, {len(HARNAS) + len(SIERADEN)} gear items, {len(TROFEEEN)} trophies, {len(t)} texts")


if __name__ == "__main__":
    _direct()
