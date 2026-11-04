"""
1.10.0 "Het Vadsrijk", G-MOUNTS: the mount system's assets (Java: feature/vadsrijk/economie: Rijdieren, RijdierSoort, Stal, client/*).

  * the Pluimguh (vr_eco_pluimguh): THE main mount, a big fluffy running bird with guh ears. PLACEHOLDER (the user still chooses):
    the concept model of renders-mounts/_bron/modellen is copied in (kopieer_pluimguh), one texture per colour:
    textures/entity/vr_eco_pluimguh_<kleur>.png (roze, mint, lavendel, honing, wolkwit, sterrennacht, goud); its animations here;
  * the Wolkenschaapje (vr_eco_wolkenschaapje): a round woolly sheep that hovers a bit;
  * Slakkie (vr_eco_slakkie): the caravan snail of the momenten slice (its model function, without the cart) with a little seat;
  * a "ren" (run) animation added to the economie mounts (Hoedhupper, Klodderros);
  * icons: the saddles of the two stal mounts and a Pluimguh egg per colour;
  * every text (Dutch; en_us gets the same Dutch).

All animations: idle, hop (walk), ren (run), sprong (in the air) - RijdierEntity picks them.
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_rijdieren.py
"""
import copy
import json
import os

import numpy as np
from PIL import Image

try:
    import vadsrijk_wezens as W
    import vadsrijk_momenten as MOM
    from vadsrijk_economie import Doek, _licht, zadel
except ImportError:  # (inside make_resources: the package import)
    from features import vadsrijk_wezens as W
    from features import vadsrijk_momenten as MOM
    from features.vadsrijk_economie import Doek, _licht, zadel

ITEM_TEX = "vadsrijk/economie"
kf, anim = W.kf, W.anim


# =====================================================================================================================
# the Pluimguh: the concept model of the mount renders (renders-mounts/_bron/mounts.py pluimguh(): a reusable GeckoLib model +
# one texture per colour, bones root/body/zadel/vleugel_l/r/leg_l/r/scheen_l/r/voet_l/r/nek/hoofd/snavel/oor_l/r/kuif/staart).
# The generator copies it into the mod when that folder is there; otherwise the committed copy stays. To swap the look: put a new
# vr_pluimguh.geo.json + vr_pluimguh_<kleur>.png there (same bone names keep the animations working) and set the seat height
# (RijdierSoort.PLUIMGUH zit = the ZADEL seat point y / 16).
# =====================================================================================================================
PLUIM_KLEUREN = ("roze", "mint", "lavendel", "honing", "wolkwit", "sterrennacht", "goud")
# (shell, spots, ears) per colour: only for the egg icons
EI_KLEUREN = {
    "roze": ((255, 172, 206), (255, 104, 168), (255, 140, 186)),
    "mint": ((158, 230, 198), (80, 196, 156), (255, 160, 196)),
    "lavendel": ((196, 172, 244), (146, 108, 222), (255, 160, 206)),
    "honing": ((255, 206, 112), (236, 150, 56), (255, 150, 170)),
    "wolkwit": ((244, 244, 252), (186, 208, 255), (255, 176, 206)),
    "sterrennacht": ((64, 58, 132), (250, 226, 120), (200, 170, 255)),
    "goud": ((255, 212, 76), (255, 244, 200), (255, 160, 150)),
}


def _pluim_bron():
    kandidaten = [os.environ.get("GUHS_MOUNT_BRON", "")]
    hier = os.path.abspath(".")
    for _ in range(4):
        kandidaten.append(os.path.join(hier, "renders-mounts", "_bron", "modellen"))
        hier = os.path.dirname(hier)
    for k in kandidaten:
        if k and os.path.exists(os.path.join(k, "vr_pluimguh.geo.json")):
            return k
    return None


def kopieer_viervoeter(a, w, naam):
    """The Knuffelhert / Guhwolf of the renders: geo + one texture (vr_<naam>.png) -> vr_eco_<naam>."""
    bron = _pluim_bron()
    if bron is None or not os.path.exists(os.path.join(bron, f"vr_{naam}.geo.json")):
        return
    with open(os.path.join(bron, f"vr_{naam}.geo.json"), encoding="utf-8") as f:
        geo = json.load(f)
    w(os.path.join(a, "geckolib", "models", "entity", f"vr_eco_{naam}.geo.json"), geo)
    _png(os.path.join(a, "textures", "entity", f"vr_eco_{naam}.png"), Image.open(os.path.join(bron, f"vr_{naam}.png")))


def anims_viervoeter(nek):
    """Knuffelhert (nek=True: hoofd hangs on nek, gewei) and Guhwolf (hoofd on body, sjaal): legs leg_fl/fr/bl/br, tail, oor_l/r."""
    kop = "nek" if nek else "hoofd"

    def benen(hoek, lengte):
        h = lengte / 2
        a = {}
        for b in ("leg_fl", "leg_br", "leg_fr", "leg_bl"):
            s = 1 if b in ("leg_fl", "leg_br") else -1
            a[b] = {"rotation": kf([(0, (hoek * s, 0, 0)), (h, (-hoek * s, 0, 0)), (lengte, (hoek * s, 0, 0))])}
        return a

    def bob(hoog, lengte):
        q = lengte / 4
        return kf([(0, (0, 0, 0)), (q, (0, hoog, 0)), (2 * q, (0, 0, 0)), (3 * q, (0, hoog, 0)), (lengte, (0, 0, 0))])

    idle = {"body": {"position": kf([(0, (0, 0, 0)), (1.2, (0, 0.35, 0)), (2.4, (0, 0, 0))])},
            "hoofd": {"rotation": kf([(0, (0, 0, 0)), (0.8, (-4, 10, 0)), (1.6, (0, -8, 0)), (2.4, (0, 0, 0))])},
            "tail": {"rotation": kf([(0, (0, -12, 0)), (0.6, (0, 12, 0)), (1.2, (0, -12, 0)), (1.8, (0, 12, 0)), (2.4, (0, -12, 0))])},
            "oor_l": {"rotation": kf([(0, (0, 0, 0)), (1.2, (0, 0, 10)), (2.4, (0, 0, 0))])},
            "oor_r": {"rotation": kf([(0, (0, 0, 0)), (1.2, (0, 0, -10)), (2.4, (0, 0, 0))])}}
    hop = benen(28, 0.7)
    hop.update({"body": {"position": bob(0.6, 0.7)},
                kop: {"rotation": kf([(0, (0, 0, 0)), (0.175, (5, 0, 0)), (0.35, (0, 0, 0)), (0.525, (5, 0, 0)), (0.7, (0, 0, 0))])},
                "tail": {"rotation": kf([(0, (10, 0, 0)), (0.35, (-10, 0, 0)), (0.7, (10, 0, 0))])}})
    # run: a gallop (front pair and back pair together, the body rocks)
    ren = {}
    for b, s in (("leg_fl", 1), ("leg_fr", 0.8), ("leg_bl", -1), ("leg_br", -0.8)):
        ren[b] = {"rotation": kf([(0, (45 * s, 0, 0)), (0.2, (-45 * s, 0, 0)), (0.4, (45 * s, 0, 0))])}
    ren.update({"body": {"position": bob(1.4, 0.4), "rotation": kf([(0, (-5, 0, 0)), (0.2, (5, 0, 0)), (0.4, (-5, 0, 0))])},
                kop: {"rotation": kf([(0, (10, 0, 0)), (0.2, (16, 0, 0)), (0.4, (10, 0, 0))])},
                "oor_l": {"rotation": kf([(0, (-30, 0, 0)), (0.4, (-30, 0, 0))])},
                "oor_r": {"rotation": kf([(0, (-30, 0, 0)), (0.4, (-30, 0, 0))])},
                "tail": {"rotation": kf([(0, (30, 0, 0)), (0.2, (15, 0, 0)), (0.4, (30, 0, 0))])}})
    sprong = {"leg_fl": {"rotation": kf([(0, (-55, 0, 0))])}, "leg_fr": {"rotation": kf([(0, (-50, 0, 0))])},
              "leg_bl": {"rotation": kf([(0, (50, 0, 0))])}, "leg_br": {"rotation": kf([(0, (55, 0, 0))])},
              "body": {"rotation": kf([(0, (-8, 0, 0))])},
              "tail": {"rotation": kf([(0, (35, 0, 0))])}}
    return {"idle": anim(2.4, idle), "hop": anim(0.7, hop), "ren": anim(0.4, ren), "sprong": anim(0.4, sprong)}


def kopieer_pluimguh(a, w):
    bron = _pluim_bron()
    if bron is None:
        print("vadsrijk_rijdieren: renders-mounts/_bron/modellen not found, the committed Pluimguh stays")
        return
    with open(os.path.join(bron, "vr_pluimguh.geo.json"), encoding="utf-8") as f:
        geo = json.load(f)
    w(os.path.join(a, "geckolib", "models", "entity", "vr_eco_pluimguh.geo.json"), geo)
    for k in PLUIM_KLEUREN:
        _png(os.path.join(a, "textures", "entity", f"vr_eco_pluimguh_{k}.png"), Image.open(os.path.join(bron, f"vr_pluimguh_{k}.png")))


def anims_pluimguh():
    def benen(hoek, lengte, knie):
        h = lengte / 2
        a = {"leg_l": {"rotation": kf([(0, (hoek, 0, 0)), (h, (-hoek, 0, 0)), (lengte, (hoek, 0, 0))])},
             "leg_r": {"rotation": kf([(0, (-hoek, 0, 0)), (h, (hoek, 0, 0)), (lengte, (-hoek, 0, 0))])}}
        # the knee bends on the way forward
        a["scheen_l"] = {"rotation": kf([(0, (0, 0, 0)), (h * 0.5, (knie, 0, 0)), (h, (0, 0, 0)), (lengte, (0, 0, 0))])}
        a["scheen_r"] = {"rotation": kf([(0, (0, 0, 0)), (h, (0, 0, 0)), (h * 1.5, (knie, 0, 0)), (lengte, (0, 0, 0))])}
        return a

    def bob(hoog, lengte):
        q = lengte / 4
        return kf([(0, (0, 0, 0)), (q, (0, hoog, 0)), (2 * q, (0, 0, 0)), (3 * q, (0, hoog, 0)), (lengte, (0, 0, 0))])

    idle = {"body": {"position": kf([(0, (0, 0, 0)), (1.0, (0, 0.4, 0)), (2.0, (0, 0, 0))])},
            "hoofd": {"rotation": kf([(0, (0, 0, 0)), (0.7, (-5, 12, 0)), (1.4, (0, -8, 0)), (2.0, (0, 0, 0))])},
            "staart": {"rotation": kf([(0, (0, -8, 0)), (1.0, (0, 8, 0)), (2.0, (0, -8, 0))])},
            "oor_l": {"rotation": kf([(0, (0, 0, 0)), (1.0, (0, 0, 8)), (2.0, (0, 0, 0))])},
            "oor_r": {"rotation": kf([(0, (0, 0, 0)), (1.0, (0, 0, -8)), (2.0, (0, 0, 0))])},
            "kuif": {"rotation": kf([(0, (0, 0, 0)), (1.0, (8, 0, 0)), (2.0, (0, 0, 0))])}}
    hop = dict(benen(30, 0.7, 25))
    hop.update({"body": {"position": bob(0.8, 0.7)},
                "nek": {"rotation": kf([(0, (0, 0, 0)), (0.175, (6, 0, 0)), (0.35, (0, 0, 0)), (0.525, (6, 0, 0)), (0.7, (0, 0, 0))])},
                "staart": {"rotation": kf([(0, (6, 0, 0)), (0.35, (-6, 0, 0)), (0.7, (6, 0, 0))])},
                "vleugel_l": {"rotation": kf([(0, (0, 0, 0)), (0.35, (0, 0, 6)), (0.7, (0, 0, 0))])},
                "vleugel_r": {"rotation": kf([(0, (0, 0, 0)), (0.35, (0, 0, -6)), (0.7, (0, 0, 0))])}})
    ren = dict(benen(50, 0.42, 45))
    ren.update({"body": {"position": bob(1.6, 0.42), "rotation": kf([(0, (4, 0, 0)), (0.42, (4, 0, 0))])},
                "nek": {"rotation": kf([(0, (16, 0, 0)), (0.21, (22, 0, 0)), (0.42, (16, 0, 0))])},
                "oor_l": {"rotation": kf([(0, (-30, 0, 0)), (0.42, (-30, 0, 0))])},
                "oor_r": {"rotation": kf([(0, (-30, 0, 0)), (0.42, (-30, 0, 0))])},
                "kuif": {"rotation": kf([(0, (-30, 0, 0)), (0.42, (-30, 0, 0))])},
                "staart": {"rotation": kf([(0, (20, 0, 0)), (0.21, (10, 0, 0)), (0.42, (20, 0, 0))])},
                "vleugel_l": {"rotation": kf([(0, (0, 0, 15)), (0.21, (0, 0, 30)), (0.42, (0, 0, 15))])},
                "vleugel_r": {"rotation": kf([(0, (0, 0, -15)), (0.21, (0, 0, -30)), (0.42, (0, 0, -15))])}})
    sprong = {"leg_l": {"rotation": kf([(0, (-35, 0, 0))])}, "leg_r": {"rotation": kf([(0, (-25, 0, 0))])},
              "scheen_l": {"rotation": kf([(0, (40, 0, 0))])}, "scheen_r": {"rotation": kf([(0, (35, 0, 0))])},
              "vleugel_l": {"rotation": kf([(0, (0, 0, 50)), (0.15, (0, 0, 75)), (0.3, (0, 0, 50))])},
              "vleugel_r": {"rotation": kf([(0, (0, 0, -50)), (0.15, (0, 0, -75)), (0.3, (0, 0, -50))])},
              "oor_l": {"rotation": kf([(0, (0, 0, 25))])}, "oor_r": {"rotation": kf([(0, (0, 0, -25))])},
              "staart": {"rotation": kf([(0, (-20, 0, 0))])}}
    return {"idle": anim(2.0, idle), "hop": anim(0.7, hop), "ren": anim(0.42, ren), "sprong": anim(0.3, sprong)}


# =====================================================================================================================
# the Wolkenschaapje (a round woolly sheep that floats a little: every animation lifts the root 2-3 px)
# =====================================================================================================================
def model_wolkenschaapje():
    m = W.Model("eco_wolkenschaapje", px=4, breedte=96, bounds=(2.5, 2.5, 0.8))
    m.bone("root")
    m.bone("body", "root", (0, 10, 0))
    m.cube("body", "wol", (-7, 5, -8), (14, 12, 16))
    m.cube("body", "wol", (-6, 16.5, -6), (12, 2, 12))
    m.cube("body", "wol", (-8, 7, -6), (1.5, 8, 12))
    m.spiegel("body", "wol", (-8, 7, -6), (1.5, 8, 12))
    m.cube("body", "wol", (-5, 7, 7.5), (10, 8, 1.5))
    m.cube("body", "zadel", (-4.5, 18, -3.5), (9, 1, 7))
    m.bone("head", "body", (0, 12, -8))
    m.cube("head", "gezicht", (-4, 8, -13.5), (8, 8, 6), gezicht={"ey": 0.42, "schaal": 1.0})
    m.cube("head", "wol", (-3.5, 15.5, -13), (7, 2.5, 5))
    for kant, sgn in (("l", -1), ("r", 1)):
        m.bone("oor_" + kant, "head", (4 * sgn, 14, -11), (0, 0, 25 * -sgn))
        if sgn < 0:
            m.cube("oor_" + kant, "gezicht", (-7.5, 13, -11.5), (3.5, 2, 2))
            m.cube("oor_" + kant, "oor", (-7, 13.2, -11.8), (2.5, 1.5, 0.5))
        else:
            m.spiegel("oor_" + kant, "gezicht", (-7.5, 13, -11.5), (3.5, 2, 2))
            m.spiegel("oor_" + kant, "oor", (-7, 13.2, -11.8), (2.5, 1.5, 0.5))
    for naam, x, z in (("leg_fl", -4, -5), ("leg_fr", 4, -5), ("leg_bl", -4, 5), ("leg_br", 4, 5)):
        m.bone(naam, "root", (x, 5, z))
        m.cube(naam, "poot", (x - 1.25, 2, z - 1.25), (2.5, 4, 2.5))
    m.bone("wolkje", "root", (0, 1, 0))
    m.cube("wolkje", "wolk", (-5, 0, -6), (10, 2, 12))
    m.cube("wolkje", "wolk", (-6.5, 0.5, -3), (13, 1.5, 6))
    return m


PALET_SCHAAP = {
    "wol": ((250, 246, 255), "plekjes", (236, 228, 248)),
    "gezicht": ((255, 214, 226), None, None),
    "oor": ((255, 150, 190), None, None),
    "poot": ((120, 100, 130), None, None),
    "zadel": ((160, 200, 255), "strepen", (130, 176, 240)),
    "wolk": ((226, 240, 255), "plekjes", (250, 252, 255)),
}


def anims_wolkenschaapje():
    def zweef(hoog, laag, lengte):
        return {"position": kf([(0, (0, laag, 0)), (lengte / 2, (0, hoog, 0)), (lengte, (0, laag, 0))])}

    poten = ("leg_fl", "leg_fr", "leg_bl", "leg_br")
    idle = {"root": zweef(3.0, 2.0, 2.4),
            "wolkje": {"scale": kf([(0, (1, 1, 1)), (1.2, (1.08, 1, 1.08)), (2.4, (1, 1, 1))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (1.2, (-4, 8, 0)), (2.4, (0, 0, 0))])},
            "oor_l": {"rotation": kf([(0, (0, 0, 0)), (1.2, (0, 0, -10)), (2.4, (0, 0, 0))])},
            "oor_r": {"rotation": kf([(0, (0, 0, 0)), (1.2, (0, 0, 10)), (2.4, (0, 0, 0))])}}
    for i, p in enumerate(poten):
        idle[p] = {"rotation": kf([(0, (6 if i % 2 else -6, 0, 0)), (1.2, (-6 if i % 2 else 6, 0, 0)), (2.4, (6 if i % 2 else -6, 0, 0))])}

    def lopen(hoek, lengte, hoog):
        a = {"root": zweef(hoog + 1.5, hoog, lengte),
             "body": {"rotation": kf([(0, (0, 0, -3)), (lengte / 2, (0, 0, 3)), (lengte, (0, 0, -3))])},
             "oor_l": {"rotation": kf([(0, (-20, 0, 0)), (lengte, (-20, 0, 0))])},
             "oor_r": {"rotation": kf([(0, (-20, 0, 0)), (lengte, (-20, 0, 0))])}}
        for i, p in enumerate(poten):
            s = 1 if p in ("leg_fl", "leg_br") else -1
            a[p] = {"rotation": kf([(0, (hoek * s, 0, 0)), (lengte / 2, (-hoek * s, 0, 0)), (lengte, (hoek * s, 0, 0))])}
        return a

    sprong = {"root": {"position": kf([(0, (0, 3, 0))])},
              "wolkje": {"scale": kf([(0, (1.25, 1, 1.25))])},
              "oor_l": {"rotation": kf([(0, (0, 0, -35))])}, "oor_r": {"rotation": kf([(0, (0, 0, 35))])}}
    for p in poten:
        sprong[p] = {"rotation": kf([(0, (35 if "f" in p[-2] else -35, 0, 0))])}
    return {"idle": anim(2.4, idle), "hop": anim(0.6, lopen(30, 0.6, 2.0)), "ren": anim(0.4, lopen(50, 0.4, 2.5)), "sprong": anim(0.5, sprong)}


# =====================================================================================================================
# Slakkie: the caravan snail (vadsrijk_momenten.model_karavaan) without its cart, centred, with a little seat on the shell
# =====================================================================================================================
SLAK_DZ = 21.0


def _verschuif(m, dz):
    for b in m.bones:
        b["pivot"][2] += dz
        for c in b["cubes"]:
            c["origin"][2] += dz
            if "pivot" in c:
                c["pivot"][2] += dz


def model_slakkie():
    m = MOM.model_karavaan()
    weg = {"kar"} | {b["name"] for b in m.bones if b.get("parent") == "kar"}
    m.bones = [b for b in m.bones if b["name"] not in weg]
    m.naam = "eco_slakkie"
    m.bounds = (3, 2.5, 0.8)
    _verschuif(m, SLAK_DZ)
    # the seat on the shell (shell top at y 21, z -2..13 after the shift): a cushion with a little backrest and armrests
    m.bone("zitje", "huis", (0, 21, 5))
    m.cube("zitje", "donker", (-4.5, 21, 0.5), (9, 1, 8))
    m.cube("zitje", "kussen", (-4, 22, 1), (8, 1, 7))
    m.cube("zitje", "donker", (-4.5, 22, 8.5), (9, 5, 1.5))
    m.cube("zitje", "kussen", (-4, 23, 8.2), (8, 3.5, 0.5))
    m.cube("zitje", "donker", (-5, 22, 1), (0.8, 2.5, 7))
    m.spiegel("zitje", "donker", (-5, 22, 1), (0.8, 2.5, 7))
    return m


def anims_slakkie():
    weg = ("kar", "wiel_lv", "wiel_rv", "wiel_la", "wiel_ra")
    basis = MOM.anims_karavaan()

    def zonder_kar(a):
        a = copy.deepcopy(a)
        for b in weg:
            a["bones"].pop(b, None)
        return a

    def tempo(a, f):
        a = zonder_kar(a)
        a["animation_length"] = round(a["animation_length"] * f, 3)
        for bone in a["bones"].values():
            for kanaal, keys in list(bone.items()):
                bone[kanaal] = {f"{float(t) * f:g}": v for t, v in keys.items()}
        return a

    sprong = {"huis": {"scale": kf([(0, (1.05, 0.92, 1.05)), (0.3, (1.05, 0.92, 1.05))])},
              "kop": {"rotation": kf([(0, (-12, 0, 0)), (0.3, (-12, 0, 0))])},
              "sprieten": {"rotation": kf([(0, (-20, 0, 0)), (0.3, (-20, 0, 0))])},
              "oor_l": {"rotation": kf([(0, (0, 0, -25)), (0.3, (0, 0, -25))])},
              "oor_r": {"rotation": kf([(0, (0, 0, 25)), (0.3, (0, 0, 25))])}}
    return {"idle": zonder_kar(basis["stil"]), "hop": tempo(basis["kruip"], 0.6), "ren": tempo(basis["kruip"], 0.35), "sprong": anim(0.3, sprong)}


# =====================================================================================================================
# "ren" for the economie mounts (their animation files come from vadsrijk_economie.animaties; this module runs after it)
# =====================================================================================================================
def ren_erbij(a, naam, w):
    pad = os.path.join(a, "geckolib", "animations", "entity", f"{naam}.animation.json")
    if not os.path.exists(pad):
        return
    with open(pad, encoding="utf-8") as f:
        data = json.load(f)
    anims = data["animations"]
    hop = anims.get("hop")
    if hop is None:
        return
    ren = copy.deepcopy(hop)
    f = 0.6
    ren["animation_length"] = round(hop["animation_length"] * f, 3)
    for bone in ren["bones"].values():
        for kanaal, keys in list(bone.items()):
            nieuw = {}
            for t, v in keys.items():
                if kanaal == "rotation":
                    v = [x * 1.35 for x in v]
                elif kanaal == "position":
                    v = [x * 1.3 for x in v]
                nieuw[f"{float(t) * f:g}"] = v
            bone[kanaal] = nieuw
    anims["ren"] = ren
    w(pad, data)


# =====================================================================================================================
# icons
# =====================================================================================================================
def ei(kleur, stip, oor):
    d = Doek()
    for y in range(3, 16):
        for x in range(16):
            # an egg: narrower at the top
            ry = 6.2 if y >= 9 else 6.2 - (9 - y) * 0.42
            if ((x - 7.5) / max(1, ry)) ** 2 + ((y - 9.5) / 6.2) ** 2 <= 1:
                d.px(x, y, kleur)
    for x, y in ((5, 8), (10, 10), (7, 12), (9, 6), (4, 11)):
        d.px(x, y, stip)
    d.rect(3, 2, 4, 4, kleur)                # guh ears on the egg
    d.rect(11, 2, 12, 4, kleur)
    d.px(4, 3, oor)
    d.px(11, 3, oor)
    d.px(6, 9, (40, 26, 40))
    d.px(9, 9, (40, 26, 40))
    d.px(5, 10, (255, 140, 180))
    d.px(10, 10, (255, 140, 180))
    d.schaduw()
    return d.rand()


ICONEN = {
    "vr_item_zadel_wolkenschaapje": lambda: zadel((246, 244, 255), [(5, 6), (9, 5), (11, 7)], (160, 200, 255)),
    "vr_item_zadel_slakkie": lambda: zadel((250, 176, 122), [(6, 5), (10, 6)], (255, 130, 180)),
    "vr_item_zadel_knuffelhert": lambda: zadel((214, 160, 120), [(5, 6), (10, 5)], (126, 196, 120)),
    "vr_item_zadel_guhwolf": lambda: zadel((200, 214, 236), [(6, 6), (10, 5)], (110, 170, 235)),
}
for _k, _v in EI_KLEUREN.items():
    ICONEN[f"vr_item_pluimei_{_k}"] = (lambda v: lambda: ei(v[0], v[1], v[2]))(_v)


# =====================================================================================================================
# texts
# =====================================================================================================================
E = "vadsrijk.economie."
KLEURNAMEN = {"roze": "Roze", "mint": "Munt", "lavendel": "Lavendel", "honing": "Honing", "wolkwit": "Wolkwit", "sterrennacht": "Sterrennacht",
              "goud": "Goud"}


def teksten():
    t = {
        "key.guhs.vr_rijdier": "Rijdier: opstappen / afstappen",
        "key.guhs.vr_rijdieren": "Rijdieren (scherm)",
        "entity.guhs.vr_eco_pluimguh": "Pluimguh",
        "entity.guhs.vr_eco_wolkenschaapje": "Wolkenschaapje",
        "entity.guhs.vr_eco_slakkie": "Slakkie",
        "entity.guhs.vr_eco_knuffelhert": "Knuffelhert",
        "entity.guhs.vr_eco_guhwolf": "Guhwolf",
        "item.guhs.vr_item_zadel_knuffelhert": "Zadel van het Knuffelhert",
        "item.guhs.vr_item_zadel_guhwolf": "Zadel van de Guhwolf",
        E + "rijdier.knuffelhert": "Knuffelhert",
        E + "rijdier.guhwolf": "Guhwolf",
        E + "rijdier.eigen.knuffelhert": "Zacht als een knuffel, met een sierlijk geweitje. Springt heel hoog.",
        E + "rijdier.eigen.guhwolf": "Een sneeuwwolfje met een sjaal. Snel en trouw.",
        E + "stal.nog_niet.knuffelhert": "Quest: Het Knuffelhert (Sporenwoud). Steek de lantaarns aan en volg het hert...",
        E + "stal.nog_niet.guhwolf": "Quest: De verdwaalde Guhwolf (Kandijkom). Er huilt een wolfje in de Splinterkloof...",
        "item.guhs.vr_item_zadel_wolkenschaapje": "Zadel van het Wolkenschaapje",
        "item.guhs.vr_item_zadel_slakkie": "Zadel van Slakkie",
        "item.guhs.vr_item_rijfluitje.uitleg": "Fluit je rijdier erbij (nog een keer: het huppelt naar huis). Hoeft niet meer: de rijdier-toets doet hetzelfde.",
        E + "rijfluitje.toets": "Rijdier-toets: standaard X (instelbaar).",
        E + "pluimei.uitleg": "Rechtsklik: het ei komt uit en de %s Pluimguh is van jou!",
        E + "rijdier.pluimguh": "Pluimguh",
        E + "rijdier.wolkenschaapje": "Wolkenschaapje",
        E + "rijdier.slakkie": "Slakkie",
        E + "rijdier.nieuw": "Nieuw rijdier: %s! Druk op je rijdier-toets (X) om op te stappen.",
        E + "rijdier.nieuw_kleur": "Nieuw rijdier: %2$s in %1$s! Kies hem met N.",
        E + "rijdier.geen": "Je hebt nog geen rijdier. Vraag eens rond in Zwamzoet...",
        E + "rijdier.wacht": "Even geduld, je rijdier komt over %s s.",
        E + "rijdier.nog_geschrokken": "Je rijdier is nog geschrokken... (%s s)",
        E + "rijdier.arena": "Je rijdier durft de arena niet in en huppelt naar huis!",
        E + "rijdier.arena_niet": "Rijdieren durven niet in een baasarena.",
        E + "rijdier.hud.rijdt": "rijdt",
        E + "rijdier.hud.schrik": "schrik!",
        E + "rijdier.hud.arena": "arena",
        E + "rijdier.eigen.pluimguh": "Het snelste rijdier van het Vadsrijk. Rent als de wind met wapperende oortjes.",
        E + "rijdier.eigen.hoedhupper": "Hupt vrolijk op zijn zwammenpootjes.",
        E + "rijdier.eigen.klodderros": "Wiebelt een beetje, maar springt het hoogst.",
        E + "rijdier.eigen.wolkenschaapje": "Zweeft een beetje: valt heel zacht en kan in de lucht nog een keer springen!",
        E + "rijdier.eigen.slakkie": "Langzaam maar heel deftig, met een eigen zitje.",
        E + "stal.nog_niet.pluimguh": "Kies je eerste Pluimguh bij een quest in De Schemerwortel.",
        E + "stal.nog_niet.wolkenschaapje": "Te koop bij de stal.",
        E + "stal.nog_niet.slakkie": "Te koop bij de stal (een luxe rijdier).",
        E + "stal.nog_niet.klodderros": "Koning Klodder heeft er soms een zadel van bij zich...",
        E + "stal.nog_niet.hoedhupper": "Iemand in Zwamzoet zoekt een hulpje...",
        E + "stal.koop": "Kopen (%s)",
        E + "stal.gekocht": "Gekocht! Je nieuwe rijdier staat in je stal.",
        E + "stal.eerst_starter": "Kies eerst je eerste Pluimguh (in De Schemerwortel).",
        E + "stal.gekozen_label": "Hier rijd je op",
        E + "stal.gekozen": "Je rijdier-toets roept nu dit rijdier.",
        E + "rijdieren.titel": "Rijdieren",
        E + "rijdieren.nog_niet": "Nog niet van jou",
        E + "rijdieren.van_jou": "Van jou!",
        E + "rijdieren.naam_kleur": "%s (%s)",
        E + "rijdieren.kies": "Hierop rijden",
        E + "rijdieren.uitleg": "%s = opstappen of afstappen. Kopen kan bij de stal.",
        E + "starter.titel": "Kies je Pluimguh!",
        E + "starter.vraag": "Welke Pluimguh wordt jouw maatje? De andere twee kun je later bij de stal kopen.",
        E + "starter.over.roze": "Lief en snel, zo roze als een knuffel.",
        E + "starter.over.mint": "Fris als een muntje en altijd vrolijk.",
        E + "starter.over.lavendel": "Dromerig en zacht, ruikt naar lavendel.",
        E + "starter.kies": "Deze wil ik!",
        E + "starter.grapje": "Nee, het is geen choco-dinges uit een of ander Fantasy-spel. Kijk maar naar de oortjes, njeg!",
        E + "stal.grapje": "Stalmeester: \"Een choco-wat? Nooit van gehoord. Dit is een Pluimguh. Kijk maar naar de oortjes, njeg!\"",
        E + "tip.rijdier.titel": "Je eerste rijdier!",
        E + "tip.rijdier": "Druk op X (je rijdier-toets) en de Hoedhupper komt eraan; nog een keer X en hij huppelt naar huis. Met N zie je al je rijdieren. Rijden geeft hem xp: dan wordt hij sneller en springt hij hoger.",
        E + "tip.starter.titel": "Je eigen Pluimguh!",
        E + "tip.starter": "Druk op X om op te stappen en weer af te stappen, N voor al je rijdieren en kleuren. Je Pluimguh rijdt in alle gebieden van het Vadsrijk.",
        E + "tip.rijden": "Spatie = een grote hup. Word je geraakt, dan schrikt je rijdier en huppelt hij weg. In een baasarena stap je af.",
    }
    for k, n in KLEURNAMEN.items():
        t[E + "pluimkleur." + k] = n
        t[f"item.guhs.vr_item_pluimei_{k}"] = f"Pluimguh-ei ({n})"
    t[E + "pluimkleur.bron.roze"] = "Een van de drie starters (quest in De Schemerwortel), of te koop bij de stal."
    t[E + "pluimkleur.bron.mint"] = "Een van de drie starters (quest in De Schemerwortel), of te koop bij de stal."
    t[E + "pluimkleur.bron.lavendel"] = "Een van de drie starters (quest in De Schemerwortel), of te koop bij de stal."
    t[E + "pluimkleur.bron.honing"] = "Te koop bij de stal, als je al een Pluimguh hebt."
    t[E + "pluimkleur.bron.wolkwit"] = "Een beloning van een quest in De Schemerwortel."
    t[E + "pluimkleur.bron.sterrennacht"] = "Een zeldzaam ei van een baas in De Schemerwortel."
    t[E + "pluimkleur.bron.goud"] = "Een heel geheim ei..."
    return t


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
    img.save(pad)


def assets(a, w=_schrijf_json):
    geo = os.path.join(a, "geckolib", "models", "entity")
    ani = os.path.join(a, "geckolib", "animations", "entity")
    tex = os.path.join(a, "textures", "entity")
    # the Pluimguh: one model, a texture per colour
    kopieer_pluimguh(a, w)
    w(os.path.join(ani, "vr_eco_pluimguh.animation.json"), W.anim_bestand(anims_pluimguh()))
    # the Knuffelhert and the Guhwolf (render models; return quests in zone 1 / zone 2)
    for naam, nek in (("knuffelhert", True), ("guhwolf", False)):
        kopieer_viervoeter(a, w, naam)
        w(os.path.join(ani, f"vr_eco_{naam}.animation.json"), W.anim_bestand(anims_viervoeter(nek)))
    # the Wolkenschaapje
    m = model_wolkenschaapje()
    w(os.path.join(geo, "vr_eco_wolkenschaapje.geo.json"), m.geo())
    w(os.path.join(ani, "vr_eco_wolkenschaapje.animation.json"), W.anim_bestand(anims_wolkenschaapje()))
    _png(os.path.join(tex, "vr_eco_wolkenschaapje.png"), W.schilder(m, PALET_SCHAAP, "blij", 3200, False))
    # Slakkie (the momenten painter: the spiral on the shell)
    m = model_slakkie()
    w(os.path.join(geo, "vr_eco_slakkie.geo.json"), m.geo())
    w(os.path.join(ani, "vr_eco_slakkie.animation.json"), W.anim_bestand(anims_slakkie()))
    _png(os.path.join(tex, "vr_eco_slakkie.png"), MOM.schilder_karavaan(m))
    # "ren" for the economie mounts
    for naam in ("vr_eco_hoedhupper", "vr_eco_klodderros"):
        ren_erbij(a, naam, w)
    # icons
    for naam, f in ICONEN.items():
        f().png(os.path.join(a, "textures", "item", ITEM_TEX, f"{naam}.png"))
        w(os.path.join(a, "models", "item", f"{naam}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{ITEM_TEX}/{naam}"}})
        w(os.path.join(a, "items", f"{naam}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})


def build(h):
    assets(h.A, h.w)
    for k, nl in teksten().items():
        h.lang(k, nl, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    assets(a)
    t = teksten()
    for taal in ("nl_nl", "en_us"):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(t)
        _schrijf_json(p, data)
    print(f"vadsrijk_rijdieren: 3 models, {len(PLUIM_KLEUREN)} pluimguh colours, {len(ICONEN)} icons, {len(t)} texts")


if __name__ == "__main__":
    _direct()
