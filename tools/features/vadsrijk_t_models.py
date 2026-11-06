"""
1.10 night round T, slice T-MODELS (decision R5 "hybride"): the showcase models + the texts of the model showroom.
Java: feature/vadsrijk/tmodels. The generator upgrade itself (model kit v2) is vadsrijk_t_modelkit.py; zone 1-3 use it via
vadsrijk_wezens.stijl(2).

  * De Grauwe Klodderreus, hand-shaped on kit v2 (was the vanilla slime model with a grey texture):
      geckolib/models/entity/vadsrijk/tmodels/grauwe_klodderreus.geo.json  (+ animations, same folder layout)
      textures/vadsrijk/tmodels/grauwe_klodderreus.png (grey, grumpy), _blij.png (pink, happy, after the plof), _gloed.png (eyes)
    Bones: root > kern (roll axis) > lijf > (wiebel > oor_l, oor_r), mond, wenkbrauw_l, wenkbrauw_r, druppels.
    Animations: idle, sprong (in the air), land, rol, blij (main controller) + spuug, brul, aanloop (trigger "actie").
  * Vahoegnir: kept as designed in o-draak (renders-draak); see vadsrijk_draak.py (T-MODELS polish there).
  * Dutch texts of /guhs vadsrijk models ... (the showroom).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_t_models.py
"""
import json
import os
import sys

import numpy as np

try:
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_t_modelkit as kit
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import vadsrijk_wezens as vw
    from features import vadsrijk_t_modelkit as kit

kf, anim, anim_bestand = vw.kf, vw.anim, vw.anim_bestand
SUB = "vadsrijk/tmodels"

GRIJS = (170, 166, 186)
ROZE = (255, 150, 200)


# =====================================================================================================================
# De Grauwe Klodderreus
# =====================================================================================================================
def model_klodderreus():
    m = vw.Model("grauwe_klodderreus", px=4, breedte=128, bounds=(5, 5, 1.5), stijl=2)
    m.bone("root")
    m.bone("kern", "root", (0, 6.5, 0))          # the roll axis (centre of the body)
    m.bone("lijf", "kern", (0, 0, 0))            # squash and stretch from the ground
    # the blob: a wide rounded body with the face on its flat front, a rounded dome on the jiggle bone
    m.cube("lijf", "slijm", (-8, 0, -8), (16, 12.5, 16), gezicht={"ey": 0.44, "schaal": 1.0}, rond=3.5)
    m.bone("wiebel", "lijf", (0, 7, 0))
    m.cube("wiebel", "slijm", (-5, 10.4, -5), (10, 4.2, 10), rond=2.0)
    m.cube("wiebel", "slijm", (-3, 14.2, -3), (6, 1.4, 6), rond=0.5)
    # guh ears on the dome (they jiggle with it)
    m.bone("oor_l", "wiebel", (-4.2, 13.2, 0), (0, 0, 20))
    m.cube("oor_l", "slijm", (-7, 12.4, -1.5), (4.6, 5.2, 3), rond=0.8)
    m.cube("oor_l", "oor", (-6.4, 13.2, -1.8), (3.2, 3.8, 0.5))
    m.bone("oor_r", "wiebel", (4.2, 13.2, 0), (0, 0, -20))
    m.spiegel("oor_r", "slijm", (-7, 12.4, -1.5), (4.6, 5.2, 3), rond=0.8)
    m.spiegel("oor_r", "oor", (-6.4, 13.2, -1.8), (3.2, 3.8, 0.5))
    # 3D brows over the eyes (grumpy; tucked away in "blij")
    for kant, sx in (("l", -1), ("r", 1)):
        b = "wenkbrauw_" + kant
        m.bone(b, "lijf", (sx * 2.6, 8.6, -8), (0, 0, sx * -16))
        m.cube(b, "brauw", (sx * 2.6 - 1.7, 8.2, -8.6), (3.4, 0.9, 0.9), rond=0)
    # the mouth: a dark plate just behind the face, pushed out (and opened) when he spits or roars
    m.bone("mond", "lijf", (0, 4.6, -7.6))
    m.cube("mond", "mond", (-2.2, 3.8, -7.9), (4.4, 1.6, 0.4), rond=0)
    m.cube("mond", "tong", (-1.2, 3.8, -7.95), (2.4, 0.7, 0.3), rond=0)
    # drips running down the sides (on the flat parts of the body), each with a drop at the end
    m.bone("druppels", "lijf", (0, 9, 0))
    for (x, z, top, l) in ((-5.0, -8.35, 9.6, 3.6), (5.6, -8.35, 8.8, 2.6), (-8.35, 3.0, 9.4, 4.2), (8.35, -2.5, 9.6, 3.2), (1.5, 8.35, 9.2, 3.8)):
        sx, sz = (0.9, 0.7) if abs(z) > 8 else (0.7, 0.9)
        m.cube("druppels", "druppel", (x - sx / 2, top - l, z - sz / 2), (sx, l, sz), rond=0)
        dx, dz = (1.7, 1.2) if abs(z) > 8 else (1.2, 1.7)
        m.cube("druppels", "druppel", (x - dx / 2, top - l - 1.4, z - dz / 2), (dx, 1.8, dz), rond=0)
    return m


def anims_klodderreus():
    sq = lambda pts: {"scale": kf(pts)}
    rot = lambda pts: {"rotation": kf(pts)}
    pos = lambda pts: {"position": kf(pts)}
    oren = lambda a, l: {"oor_l": rot([(0, (0, 0, a)), (l / 2, (0, 0, -a)), (l, (0, 0, a))]),
                         "oor_r": rot([(0, (0, 0, -a)), (l / 2, (0, 0, a)), (l, (0, 0, -a))])}
    a = {
        "idle": anim(2.4, dict({"lijf": sq([(0, (1, 1, 1)), (1.2, (1.04, 0.95, 1.04)), (2.4, (1, 1, 1))]),
                                "druppels": sq([(0, (1, 1, 1)), (1.4, (1, 1.18, 1)), (2.4, (1, 1, 1))]),
                                "wenkbrauw_l": rot([(0, (0, 0, 0)), (1.8, (0, 0, -6)), (2.0, (0, 0, 0)), (2.4, (0, 0, 0))]),
                                "wenkbrauw_r": rot([(0, (0, 0, 0)), (1.8, (0, 0, 6)), (2.0, (0, 0, 0)), (2.4, (0, 0, 0))])},
                               **oren(5, 2.4))),
        "sprong": anim(0.8, dict({"lijf": sq([(0, (0.86, 1.2, 0.86)), (0.4, (0.9, 1.14, 0.9)), (0.8, (0.86, 1.2, 0.86))]),
                                  "druppels": sq([(0, (1, 1.5, 1)), (0.4, (1, 1.3, 1)), (0.8, (1, 1.5, 1))]),
                                  "oor_l": rot([(0, (-25, 0, 20)), (0.4, (-30, 0, 26)), (0.8, (-25, 0, 20))]),
                                  "oor_r": rot([(0, (-25, 0, -20)), (0.4, (-30, 0, -26)), (0.8, (-25, 0, -20))]),
                                  "wenkbrauw_l": rot([(0, (0, 0, -12))]), "wenkbrauw_r": rot([(0, (0, 0, 12))])})),
        "land": anim(0.7, {"lijf": sq([(0, (1.35, 0.62, 1.35)), (0.15, (0.9, 1.14, 0.9)), (0.32, (1.08, 0.94, 1.08)), (0.5, (0.98, 1.02, 0.98)),
                                       (0.7, (1, 1, 1))]),
                           "oor_l": rot([(0, (0, 0, -30)), (0.2, (0, 0, 18)), (0.45, (0, 0, -6)), (0.7, (0, 0, 0))]),
                           "oor_r": rot([(0, (0, 0, 30)), (0.2, (0, 0, -18)), (0.45, (0, 0, 6)), (0.7, (0, 0, 0))]),
                           "druppels": sq([(0, (1, 0.5, 1)), (0.3, (1, 1.3, 1)), (0.7, (1, 1, 1))])}, loop="hold"),
        "rol": anim(0.8, {"kern": rot([(0, (0, 0, 0)), (0.4, (-180, 0, 0)), (0.8, (-360, 0, 0))]),
                          "lijf": sq([(0, (1, 0.92, 1)), (0.8, (1, 0.92, 1))]),
                          "oor_l": rot([(0, (0, 0, 35))]), "oor_r": rot([(0, (0, 0, -35))])}),
        "blij": anim(1.0, dict({"lijf": dict(sq([(0, (1.1, 0.9, 1.1)), (0.25, (0.94, 1.1, 0.94)), (0.5, (1.1, 0.9, 1.1)), (0.75, (0.94, 1.1, 0.94)),
                                                 (1.0, (1.1, 0.9, 1.1))]),
                                             **rot([(0, (0, 0, -6)), (0.5, (0, 0, 6)), (1.0, (0, 0, -6))])),
                                "wenkbrauw_l": pos([(0, (0, 0, 2))]), "wenkbrauw_r": pos([(0, (0, 0, 2))])},
                               **oren(22, 0.5))),
        # trigger animations ("actie" controller): spuug (phase 2 kloddertjes + the spit ball), brul (phase change), aanloop (aiming a jump)
        "spuug": anim(0.9, {"lijf": sq([(0, (1, 1, 1)), (0.3, (1.16, 1.12, 1.16)), (0.45, (0.9, 0.92, 1.12)), (0.7, (1.04, 0.97, 1.04)), (0.9, (1, 1, 1))]),
                            "mond": dict(pos([(0, (0, 0, 0)), (0.3, (0, 0, -0.5)), (0.6, (0, 0, -0.5)), (0.9, (0, 0, 0))]),
                                         **sq([(0, (1, 1, 1)), (0.3, (1.3, 1.4, 1)), (0.45, (1.5, 2.6, 1)), (0.7, (1.3, 1.6, 1)), (0.9, (1, 1, 1))]))},
                    loop=False),
        "brul": anim(1.2, {"lijf": sq([(0, (1, 1, 1)), (0.25, (0.92, 1.12, 0.92)), (0.9, (0.95, 1.1, 0.95)), (1.2, (1, 1, 1))]),
                           "mond": dict(pos([(0, (0, 0, 0)), (0.2, (0, 0, -0.5)), (1.0, (0, 0, -0.5)), (1.2, (0, 0, 0))]),
                                        **sq([(0, (1, 1, 1)), (0.25, (1.6, 3.0, 1)), (0.6, (1.7, 3.2, 1)), (1.0, (1.5, 2.6, 1)), (1.2, (1, 1, 1))])),
                           "oor_l": rot([(0, (0, 0, 0)), (0.25, (-20, 0, -14)), (0.4, (-24, 0, -8)), (0.55, (-20, 0, -14)), (1.2, (0, 0, 0))]),
                           "oor_r": rot([(0, (0, 0, 0)), (0.25, (-20, 0, 14)), (0.4, (-24, 0, 8)), (0.55, (-20, 0, 14)), (1.2, (0, 0, 0))]),
                           "wenkbrauw_l": rot([(0, (0, 0, 0)), (0.25, (0, 0, -18)), (1.0, (0, 0, -18)), (1.2, (0, 0, 0))]),
                           "wenkbrauw_r": rot([(0, (0, 0, 0)), (0.25, (0, 0, 18)), (1.0, (0, 0, 18)), (1.2, (0, 0, 0))])}, loop=False),
        "aanloop": anim(0.5, {"lijf": dict(sq([(0, (1.14, 0.82, 1.14)), (0.25, (1.17, 0.79, 1.17)), (0.5, (1.14, 0.82, 1.14))]),
                                           **pos([(0, (-0.15, 0, 0)), (0.125, (0.15, 0, 0)), (0.25, (-0.15, 0, 0)), (0.375, (0.15, 0, 0)), (0.5, (-0.15, 0, 0))])),
                              "oor_l": rot([(0, (15, 0, 30))]), "oor_r": rot([(0, (15, 0, -30))]),
                              "wenkbrauw_l": rot([(0, (0, 0, -14))]), "wenkbrauw_r": rot([(0, (0, 0, 14))])}),
    }
    kit.wiebel_kap(a)
    return a


def palet_klodderreus(blij):
    k = ROZE if blij else GRIJS
    return {"slijm": (k, "glans", None), "oor": ((255, 180, 214) if blij else (196, 160, 186), None, None),
            "brauw": ((96, 70, 104), None, None), "mond": ((86, 36, 74), None, None), "tong": ((240, 120, 160), None, None),
            "druppel": (tuple(int(c * 0.78) for c in k), None, None), "bel": ((255, 236, 246) if blij else (214, 210, 226), "glans", None)}


def textures_klodderreus(m):
    grauw = vw.schilder(m, palet_klodderreus(False), "boos", 4100, False)    # grey + the grumpy face
    blij = vw.schilder(m, palet_klodderreus(True), "blij", 4100, False)
    # glowmask: only the eyes (purple-grey ontvadst glow, the iris under the grumpy lid), like the grey dragon
    S = m.px
    g = np.zeros((m.hoogte() * S, m.W * S, 4), np.uint8)
    g[..., 3] = 255

    def ogen(gb, gz):
        ey, sch = gz["ey"], gz["schaal"] * 1.1
        oo, cy, er = vw._ogen_van(gb, ey, sch)
        hh, ww = gb.shape[:2]
        yy, xx = np.mgrid[0:hh, 0:ww].astype(np.float32)
        for ex in oo:
            d = np.hypot(xx - ex, yy - (cy + er * 0.25))
            m_ = np.clip(er * 0.62 - d + 0.5, 0, 1) * (yy > cy - er * 0.1)
            gb[..., :3] = np.maximum(gb[..., :3], (m_[..., None] * np.array((150, 90, 200))).astype(np.uint8))

    kit.gezichten_wereld(m, g, ogen, achtergrond=(0, 0, 0))
    from PIL import Image
    return grauw, blij, Image.fromarray(g)


# =====================================================================================================================
# texts (the showroom command)
# =====================================================================================================================
TEKSTEN = {
    "vadsrijk.tmodels.cmd.toon": "%s wezens neergezet om te bekijken (stil, onkwetsbaar). Weg: /guhs vadsrijk models weg",
    "vadsrijk.tmodels.cmd.weg": "%s showroom-wezens opgeruimd.",
    "vadsrijk.tmodels.cmd.plof": "%s wezens opgevrolijkt: zo zien ze er roze en blij uit.",
    "vadsrijk.tmodels.cmd.reus": "De Grauwe Klodderreus staat klaar (%s).",
    "vadsrijk.tmodels.cmd.reus.stil": "stil, om te bekijken",
    "vadsrijk.tmodels.cmd.reus.vecht": "hij vecht, lv 3",
    "vadsrijk.tmodels.cmd.anim": "Animatie '%s' gestart bij %s Klodderreus(en).",
    "vadsrijk.tmodels.cmd.onbekend": "Onbekende groep '%s' (zone1, zone2, zone3, critters, alles).",
}
TEKSTEN_EN = {
    "vadsrijk.tmodels.cmd.toon": "%s creatures placed to look at (frozen, invulnerable). Remove: /guhs vadsrijk models weg",
    "vadsrijk.tmodels.cmd.weg": "%s showroom creatures removed.",
    "vadsrijk.tmodels.cmd.plof": "%s creatures cheered up: this is their pink, happy look.",
    "vadsrijk.tmodels.cmd.reus": "The Grey Blob Giant is ready (%s).",
    "vadsrijk.tmodels.cmd.reus.stil": "frozen, to look at",
    "vadsrijk.tmodels.cmd.reus.vecht": "he fights, lv 3",
    "vadsrijk.tmodels.cmd.anim": "Animation '%s' started on %s Blob Giant(s).",
    "vadsrijk.tmodels.cmd.onbekend": "Unknown group '%s' (zone1, zone2, zone3, critters, alles).",
}


# =====================================================================================================================
# writing
# =====================================================================================================================
def assets(a, w=vw._schrijf_json):
    geo_dir = os.path.join(a, "geckolib", "models", "entity", *SUB.split("/"))
    anim_dir = os.path.join(a, "geckolib", "animations", "entity", *SUB.split("/"))
    tex_dir = os.path.join(a, "textures", *SUB.split("/"))
    m = model_klodderreus()
    an = anims_klodderreus()
    bones = {b["name"] for b in m.bones}
    for naam, x in an.items():
        for b in x["bones"]:
            assert b in bones, f"klodderreus: anim {naam} moves unknown bone {b}"
    w(os.path.join(geo_dir, "grauwe_klodderreus.geo.json"), m.geo())
    w(os.path.join(anim_dir, "grauwe_klodderreus.animation.json"), anim_bestand(an))
    grauw, blij, gloed = textures_klodderreus(m)
    vw._png(os.path.join(tex_dir, "grauwe_klodderreus.png"), grauw)
    vw._png(os.path.join(tex_dir, "grauwe_klodderreus_blij.png"), blij)
    vw._png(os.path.join(tex_dir, "grauwe_klodderreus_gloed.png"), gloed)
    return kit.kubussen(m)


def build(h):
    assets(h.A, h.w)
    for k, nl in TEKSTEN.items():
        h.lang(k, TEKSTEN_EN.get(k, nl), nl)   # (key, en, nl)


def _direct():
    a = os.path.join("src", "main", "resources", "assets", "guhs")
    n = assets(a)
    for taal, t in (("nl_nl", TEKSTEN), ("en_us", TEKSTEN_EN)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update(t)
        vw._schrijf_json(p, data)
    print(f"vadsrijk_t_models: Grauwe Klodderreus {n} cubes, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
