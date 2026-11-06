"""
1.10 night round T, slice T-Z4DIEREN: the two moving animals/things of zone 4 "De Lampionkust" (Java: feature/vadsrijk/zone4/dieren).

  * Ouwe Dobber, the giant turtle that swims his round past the Schildpadeiland (GeckoLib model + animations "zwem" / "rust",
    drawn at 1/4 size and scaled x4 by the renderer: shell 14 x 11 blocks, top 3.5 blocks above the water):
      assets/guhs/geckolib/{models,animations}/entity/vadsrijk/zone4/dieren/ouwe_dobber.*
      assets/guhs/textures/vadsrijk/zone4/dieren/ouwe_dobber.png
  * the guh kites (one quad per kite, drawn by code): a diamond with two guh ears, tinted per kite (vlieger_kleur.png = the
    part that takes the kite's colour), and the fixed details on top (vlieger_vast.png: white cross, eyes, blush, ear insides).
  * all texts (Dutch, English where cheap).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_z4dieren.py
"""
import json
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_z2_wezens as z2

Model, kf, anim, anim_bestand = vw.Model, vw.kf, vw.anim, vw.anim_bestand
SUB = "vadsrijk/zone4/dieren"


# =====================================================================================================================
# Ouwe Dobber (units: 1/4 of a normal model unit; the renderer scales x4)
# =====================================================================================================================
def m_ouwe_dobber():
    m = Model("ouwe_dobber", px=2, breedte=256, bounds=(16, 6, 0))
    m.bone("root")
    m.bone("lijf", "root", (0, 0, 0))
    m.cube("lijf", "buik", (-20, -4, -25), (40, 4, 50))
    m.cube("lijf", "rand", (-22, 0, -28), (44, 5, 56))
    m.cube("lijf", "schild", (-19, 5, -25), (38, 4, 50))
    m.cube("lijf", "schild", (-15, 9, -20), (30, 3, 40))
    m.cube("lijf", "schild_top", (-10, 12, -14), (20, 2, 28))
    # little flowers growing on the old shell (he's that old)
    for (x, z) in ((-7, -6), (5, 7), (-2, 10), (8, -10), (-12, 4)):
        m.cube("lijf", "bloem", (x, 14 if abs(x) < 10 and abs(z) < 14 else 12, z), (2, 1, 2))
    # head with a sweet face, guh ears and a hibiscus
    m.bone("kop", "lijf", (0, 4, -28))
    m.cube("kop", "huid", (-5, 0, -35), (10, 7, 8))
    m.cube("kop", "huid", (-8, 1, -47), (16, 13, 13), gezicht={"mood": "lief", "ey": 0.42})
    m.cube("kop", "wang", (-7.5, 3, -47.2), (3, 2, 0.4))
    m.cube("kop", "wang", (4.5, 3, -47.2), (3, 2, 0.4))
    m.bone("oor_l", "kop", (5, 13, -40))
    m.cube("oor_l", "huid", (3, 13, -42), (4, 7, 3))
    m.cube("oor_l", "oor_binnen", (3.8, 14, -42.3), (2.4, 5, 0.4))
    m.bone("oor_r", "kop", (-5, 13, -40))
    m.cube("oor_r", "huid", (-7, 13, -42), (4, 7, 3))
    m.cube("oor_r", "oor_binnen", (-6.2, 14, -42.3), (2.4, 5, 0.4))
    m.cube("kop", "bloem", (-10, 11, -44), (3, 3, 3))
    # flippers
    m.bone("vin_lv", "lijf", (17, 2, -16))
    m.cube("vin_lv", "huid", (17, 0.5, -22), (18, 2.5, 10))
    m.bone("vin_rv", "lijf", (-17, 2, -16))
    m.cube("vin_rv", "huid", (-35, 0.5, -22), (18, 2.5, 10))
    m.bone("vin_la", "lijf", (16, 2, 18))
    m.cube("vin_la", "huid", (16, 0.5, 15), (11, 2.5, 7))
    m.bone("vin_ra", "lijf", (-16, 2, 18))
    m.cube("vin_ra", "huid", (-27, 0.5, 15), (11, 2.5, 7))
    m.bone("staart", "lijf", (0, 1, 27))
    m.cube("staart", "huid", (-2.5, 0, 27), (5, 3, 7))

    def peddel(bone, ay, az, t, fase=0.0, teken=1):
        pts = []
        for i in range(5):
            s = i / 4
            import math
            w = math.sin((s + fase) * 2 * math.pi)
            pts.append((round(s * t, 3), (0, teken * ay * w, teken * az * w)))
        return {bone: {"rotation": kf(pts)}}

    zwem = {}
    for d in (peddel("vin_lv", 28, 12, 4.0, 0, 1), peddel("vin_rv", 28, 12, 4.0, 0, -1),
              peddel("vin_la", 14, 6, 4.0, 0.5, 1), peddel("vin_ra", 14, 6, 4.0, 0.5, -1)):
        zwem.update(d)
    zwem["kop"] = {"rotation": kf([(0, (-2, 0, 0)), (1, (2, 3, 0)), (2, (-2, 0, 0)), (3, (2, -3, 0)), (4, (-2, 0, 0))]),
                   "position": kf([(0, (0, 0, 0)), (2, (0, 0.6, 0)), (4, (0, 0, 0))])}
    zwem["lijf"] = {"rotation": kf([(0, (0, 0, -1.5)), (2, (0, 0, 1.5)), (4, (0, 0, -1.5))]),
                    "position": kf([(0, (0, 0, 0)), (1, (0, 0.5, 0)), (2, (0, 0, 0)), (3, (0, 0.5, 0)), (4, (0, 0, 0))])}
    zwem["staart"] = {"rotation": kf([(0, (0, -10, 0)), (2, (0, 10, 0)), (4, (0, -10, 0))])}
    zwem["oor_l"] = {"rotation": kf([(0, (-6, 0, 0)), (2, (4, 0, 0)), (4, (-6, 0, 0))])}
    zwem["oor_r"] = {"rotation": kf([(0, (-6, 0, 0)), (2, (4, 0, 0)), (4, (-6, 0, 0))])}
    rust = {
        "kop": {"rotation": kf([(0, (0, 0, 0)), (2, (3, 14, 0)), (4, (0, 0, 0)), (6, (3, -14, 0)), (8, (0, 0, 0))])},
        "lijf": {"position": kf([(0, (0, 0, 0)), (4, (0, 0.4, 0)), (8, (0, 0, 0))])},
        "vin_lv": {"rotation": kf([(0, (0, 0, 0)), (4, (0, 0, 6)), (8, (0, 0, 0))])},
        "vin_rv": {"rotation": kf([(0, (0, 0, 0)), (4, (0, 0, -6)), (8, (0, 0, 0))])},
        "oor_l": {"rotation": kf([(0, (0, 0, 0)), (3.6, (0, 0, 0)), (3.8, (-14, 0, 0)), (4.0, (0, 0, 0)), (8, (0, 0, 0))])},
        "oor_r": {"rotation": kf([(0, (0, 0, 0)), (3.7, (0, 0, 0)), (3.9, (-14, 0, 0)), (4.1, (0, 0, 0)), (8, (0, 0, 0))])},
        "staart": {"rotation": kf([(0, (0, -4, 0)), (4, (0, 4, 0)), (8, (0, -4, 0))])},
    }
    a = {"zwem": anim(4.0, zwem), "rust": anim(8.0, rust)}
    pal = {"buik": ((246, 226, 180), "strepen", (230, 206, 156)),
           "rand": ((96, 168, 138), "plekjes", (78, 146, 118)),
           "schild": ((122, 196, 156), "facet", (96, 170, 132)),
           "schild_top": ((140, 210, 168), "facet", (110, 186, 146)),
           "huid": ((178, 226, 176), "plekjes", (160, 210, 160)),
           "wang": ((255, 170, 190), None, None),
           "oor_binnen": ((255, 190, 206), None, None),
           "bloem": ((255, 140, 186), "stippen", (255, 230, 120))}
    return m, a, pal


# =====================================================================================================================
# the kite textures (32 x 40: ears on top, diamond below; the quad is 1 x 1.25 of the kite size)
# =====================================================================================================================
KW, KH = 32, 40


def _vorm():
    yy, xx = np.mgrid[0:KH, 0:KW]
    ruit = np.abs(xx + 0.5 - 16) / 16 + np.abs(yy + 0.5 - 24) / 16 <= 1
    oor = (((xx + 0.5 - 10) / 3.4) ** 2 + ((yy + 0.5 - 8) / 6.5) ** 2 <= 1) | (((xx + 0.5 - 22) / 3.4) ** 2 + ((yy + 0.5 - 8) / 6.5) ** 2 <= 1)
    return ruit, oor, xx, yy


def vlieger_kleur():
    ruit, oor, xx, yy = _vorm()
    img = np.zeros((KH, KW, 4), np.uint8)
    m = ruit | oor
    # a soft shading: lighter at the top, a fold line darker
    licht = np.clip(255 - (yy - 8) * 1.4, 200, 255).astype(np.uint8)
    for c in range(3):
        img[..., c] = np.where(m, licht, 0)
    img[..., 3] = np.where(m, 255, 0)
    return Image.fromarray(img)


def vlieger_vast():
    ruit, oor, xx, yy = _vorm()
    img = np.zeros((KH, KW, 4), np.uint8)

    def zet(mask, kleur):
        img[mask, :3] = kleur
        img[mask, 3] = 255
    # the white cross (the kite's sticks)
    zet(ruit & (np.abs(xx + 0.5 - 16) < 0.6) & ~((yy > 17) & (yy < 29)), (255, 255, 255))
    zet(ruit & (np.abs(yy + 0.5 - 24) < 0.6) & ~((np.abs(xx + 0.5 - 16) < 6) & (yy > 18)), (255, 255, 255))
    # ear insides
    zet(((((xx + 0.5 - 10) / 1.6) ** 2 + ((yy + 0.5 - 8) / 4.0) ** 2) <= 1) | ((((xx + 0.5 - 22) / 1.6) ** 2 + ((yy + 0.5 - 8) / 4.0) ** 2) <= 1),
        (255, 196, 210))
    # eyes with a highlight, blush and a little mouth
    for ex in (12, 20):
        zet(np.hypot(xx + 0.5 - ex, yy + 0.5 - 21) <= 1.9, (24, 20, 28))
        zet(np.hypot(xx + 0.5 - (ex - 0.6), yy + 0.5 - 20.3) <= 0.6, (255, 255, 255))
    for bx in (9, 23):
        zet(((xx + 0.5 - bx) / 2.0) ** 2 + ((yy + 0.5 - 25) / 1.0) ** 2 <= 1, (255, 150, 176))
    for (x, y) in ((14, 26), (15, 27), (16, 26), (17, 27), (18, 26)):
        zet((xx == x) & (yy == y), (60, 30, 40))
    return Image.fromarray(img)


# =====================================================================================================================
# texts
# =====================================================================================================================
TEKSTEN = {   # key -> (nl, en)
    "entity.guhs.vr_z4_ouwe_dobber": ("Ouwe Dobber", "Old Bobber"),
    "entity.guhs.vr_z4_vlieger": ("Guh-vlieger", "Guh Kite"),
    "vadsrijk.z4dieren.dobber.op": ("Ouwe Dobber neemt je mee op zijn rondje! (sluipen = afstappen)",
                                    "Old Bobber takes you along on his round! (sneak = get off)"),
    "vadsrijk.z4dieren.dobber.vol": ("Ouwe Dobber zit vol. Wacht even op het volgende rondje!",
                                     "Old Bobber is full. Wait for the next round!"),
    "vadsrijk.z4dieren.dobber.af_steiger": ("Je stapt af op de steiger van het Schildpadeiland.",
                                            "You step off onto the Turtle Island jetty."),
    "vadsrijk.z4dieren.dobber.af_water": ("Plons! Je zwemt naast Ouwe Dobber. Klik op hem om weer op te stappen.",
                                          "Splash! You swim next to Old Bobber. Click him to climb back on."),
    "vadsrijk.z4dieren.dobber.gered": ("Ouwe Dobber vist je uit het water. Hè hè, adem maar even uit!",
                                       "Old Bobber scoops you out of the water. Phew, catch your breath!"),
    "vadsrijk.z4dieren.dobber.halte": ("Ouwe Dobber wacht bij de steiger van het Schildpadeiland. Klik op hem om mee te zwemmen!",
                                       "Old Bobber waits at the Turtle Island jetty. Click him to swim along!"),
    "vadsrijk.z4dieren.dobber.vertrek": ("Ouwe Dobber zwemt weer weg... dobber, dobber.",
                                         "Old Bobber swims off again... bob, bob."),
    "vadsrijk.z4dieren.dobber.terug": ("Ouwe Dobber is terug bij de steiger. Stap af als je wilt!",
                                       "Old Bobber is back at the jetty. Get off if you like!"),
}


def teksten():
    return dict(TEKSTEN)


# =====================================================================================================================
# writing
# =====================================================================================================================
def assets(a, w=vw._schrijf_json):
    geo_dir = os.path.join(a, "geckolib", "models", "entity", *SUB.split("/"))
    anim_dir = os.path.join(a, "geckolib", "animations", "entity", *SUB.split("/"))
    tex_dir = os.path.join(a, "textures", *SUB.split("/"))
    m, anims, pal = m_ouwe_dobber()
    bones = {b["name"] for b in m.bones}
    for an, x in anims.items():
        for b in x["bones"]:
            assert b in bones, f"ouwe_dobber: anim {an} moves unknown bone {b}"
    w(os.path.join(geo_dir, "ouwe_dobber.geo.json"), m.geo())
    w(os.path.join(anim_dir, "ouwe_dobber.animation.json"), anim_bestand(anims))
    vw._png(os.path.join(tex_dir, "ouwe_dobber.png"), z2.schilder(m, pal, 4400))
    vw._png(os.path.join(tex_dir, "vlieger_kleur.png"), vlieger_kleur())
    vw._png(os.path.join(tex_dir, "vlieger_vast.png"), vlieger_vast())


def build(h):
    assets(h.A, h.w)
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)   # (make_v2.lang is key, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    assets(a)
    for i, taal in enumerate(("nl_nl", "en_us")):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in teksten().items()})
        vw._schrijf_json(p, data)
    print(f"vadsrijk_t_z4dieren: 1 model, 2 kite textures, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
