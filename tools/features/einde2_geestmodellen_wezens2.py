"""
einde2, slice geest_modellen (phase B) - the six new creatures of the Geestenwereld (CONTRACT_E2_B 7.7), on the same Java
base class (GeestWezen) and the same model kit as einde2_geestmodellen_wezens (whose tables WEZENS / MODELLEN take these rows):

  kennisvosje     a spirit fox with round guh ears and a scroll on its back (Soort 0 silver-blue, 1 the rare golden one);
                  it leads you to a secret (Java zetDoel)
  mistreiger      a small white heron of the roofs with tiny guh ears; it flies up when you come close
  oude_reigerguh  the Old Heron: a huge see-through crane spirit with guh ears and a red crown, asleep coiled round the stone
                  lotus (poses slaap / ontwaak / spreek)
  wan_guh         the hooded figure in the distance: a dark robe with glowing trim, guh ears through the hood, two glowing
                  eyes in the shadow (verdwijn; ontkap lowers the hood: Wan-guh, the first)
  geestreus       giant silhouettes far away (Soort 0 a sitting guh as big as a hill, 1 a sky serpent with fins, 2 an antlered
                  deer spirit, 3 a robed figure with four arms): deep blue, see-through, faint glowing eyes; one bone per form
  kijkmasker      a mask of Koh's cave on a little hook (Soort 0-11 = the faces of the block geestenmasker), with guh ears;
                  its face (bone head) turns slowly towards you

Guh in the details, mysterious, never mean.
"""
import math

import numpy as np

from features import einde2_geestmodellen_wezens as W
from features.einde2_geestmodellen_wezens import (alfa, anim, bone, boog, cube, disc, ears, geo_, gloei, golf, guh_face, kf, knik,
                                                  met, oor_idle, plain, scherp, vacht)

# id -> (hitbox width, height, looks, a texture per look, see-through, glow, [(animation besides idle, ticks)])
WEZENS = {
    "kennisvosje": (0.6, 0.7, 2, True, True, True, [("praat", 24), ("loop", 0), ("wijs", 40), ("snuffel", 30)]),
    "mistreiger": (0.5, 0.9, 0, False, False, True, [("praat", 24), ("vlieg", 0), ("opvlieg", 20), ("poets", 40)]),
    "oude_reigerguh": (3.5, 3.0, 0, False, True, True, [("praat", 24), ("slaap", 0), ("ontwaak", 80), ("spreek", 0)]),
    "wan_guh": (0.8, 2.0, 0, False, True, True, [("praat", 24), ("verdwijn", 30), ("ontkap", 0), ("wijs", 40)]),
    "geestreus": (2.0, 6.0, 4, False, True, True, [("praat", 24), ("verschijn", 60), ("drijf", 0)]),
    "kijkmasker": (0.8, 0.9, 12, True, False, True, [("praat", 24), ("knipper", 16)]),
}

# the faces of the block geestenmasker, in its order (einde2_geestenwereld_tex.MASKERS)
MASKERS = ["noh", "guh", "mika", "guhdalf", "sponsguh", "vos", "aap", "lach", "verdriet", "blanco", "blauwe_geest", "uil"]


# =====================================================================================================================
# kennisvosje
# =====================================================================================================================
VOS = {0: dict(vacht=(214, 228, 250), licht=(246, 250, 255), donker=(150, 170, 214), a=214, gloed=0.35),
       1: dict(vacht=(250, 206, 96), licht=(255, 240, 180), donker=(214, 150, 50), a=235, gloed=0.75)}


def kennisvosje(a, soort=0):
    c = VOS[soort]
    allesgloed = soort == 1
    huid = gloei(alfa(plain(c["vacht"], 7), c["a"]), dim=c["gloed"]) if allesgloed else alfa(plain(c["vacht"], 7), c["a"])
    licht = gloei(alfa(plain(c["licht"], 4), c["a"]), dim=0.6)
    poot = alfa(plain(c["donker"], 6), c["a"])
    gezicht = scherp(guh_face(c["vacht"], eye_y=0.42, eye_dx=0.26, eye_r=0.16), 3.0)
    papier, lint = plain((240, 228, 196), 4), plain((200, 60, 60), 4)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [cube(a, [-3, 5, -6], [6, 5.5, 12], huid, overrides={"down": licht})]),
             bone("head", "body", [0, 9, -6], [cube(a, [-3.5, 7, -11], [7, 6, 5], huid, overrides={"north": gezicht}),
                                                cube(a, [-1.5, 7.2, -13.2], [3, 2.4, 2.2], licht),
                                                cube(a, [-0.6, 8.6, -13.5], [1.2, 0.8, 0.4], plain((60, 50, 70), 3))]),
             bone("tail", "body", [0, 9, 6], [cube(a, [-2, 7, 6], [4, 4, 8], huid), cube(a, [-1.6, 7.4, 14], [3.2, 3.2, 4], licht)],
                  rotation=[-25, 0, 0]),
             bone("rol", "body", [0, 11, 0], [cube(a, [-4.5, 10.5, -1.5], [9, 2.2, 2.2], papier),
                                               cube(a, [-3.2, 10.3, -1.7], [0.8, 2.6, 2.6], lint), cube(a, [2.4, 10.3, -1.7], [0.8, 2.6, 2.6], lint)])]
    bones += ears(a, c["vacht"], (246, 170, 196), 2.4, 12.6, -9, 2.6, 2.6)
    for kant, sx in (("left", 1), ("right", -1)):
        for voor, z in (("front", -5), ("back", 3)):
            x = 1.0 if sx > 0 else -3.0
            bones.append(bone(f"leg_{voor}_{kant}", "body", [x + 1, 5, z + 1], [cube(a, [x, 0, z], [2, 5, 2], poot)]))
    return geo_("kennisvosje", bones, 2, 2, [0, 0.6, 0])


def vos_animaties():
    L = 6.0
    idle = {"tail": {"rotation": golf(1, 18, L, keer=3, om=(-25, 0, 0))}, "head": {"rotation": golf(1, 10, L, keer=1, fase=0.6)},
            "rol": {"rotation": golf(2, 2, L, keer=2)}}
    idle.update(oor_idle(L, 14))
    loop = {"leg_front_left": {"rotation": golf(0, 30, 0.6)}, "leg_back_right": {"rotation": golf(0, 30, 0.6)},
            "leg_front_right": {"rotation": golf(0, -30, 0.6)}, "leg_back_left": {"rotation": golf(0, -30, 0.6)},
            "body": {"position": golf(1, 0.5, 0.6, keer=2)}, "tail": {"rotation": golf(1, 12, 0.6, om=(-15, 0, 0))}}
    wijs = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.4, [-12, 0, 0]), (1.6, [-12, 0, 0]), (2.0, [0, 0, 0])])},
            "leg_front_right": {"rotation": kf([(0, [0, 0, 0]), (0.4, [-50, 0, 0]), (1.6, [-50, 0, 0]), (2.0, [0, 0, 0])])},
            "tail": {"rotation": kf([(0, [-25, 0, 0]), (0.4, [-70, 0, 0]), (1.0, [-70, 20, 0]), (1.3, [-70, -20, 0]), (1.6, [-70, 0, 0]), (2.0, [-25, 0, 0])])}}
    snuffel = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [35, 0, 0]), (0.5, [30, 10, 0]), (0.8, [35, -10, 0]), (1.1, [30, 0, 0]), (1.5, [0, 0, 0])])},
               "body": {"rotation": kf([(0, [0, 0, 0]), (0.3, [8, 0, 0]), (1.1, [8, 0, 0]), (1.5, [0, 0, 0])])}}
    praat = {"head": knik(1.2, 10), "tail": {"rotation": golf(1, 25, 1.2, keer=2, om=(-25, 0, 0))}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "loop": anim(0.6, loop), "wijs": anim(2.0, wijs, False),
            "snuffel": anim(1.5, snuffel, False)}


# =====================================================================================================================
# mistreiger
# =====================================================================================================================
def mistreiger(a):
    veer = plain((246, 248, 252), 5)
    schaduw = plain((214, 222, 236), 5)
    snavel = gloei(plain((246, 196, 70), 4), dim=0.4)
    poot = plain((60, 56, 64), 4)
    gezicht = scherp(guh_face((246, 248, 252), eye_y=0.45, eye_dx=0.27, eye_r=0.18, snoet=False), 4.0)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 10, 0], [cube(a, [-2.5, 8, -4], [5, 5, 9], veer, overrides={"down": schaduw}),
                                                cube(a, [-1.5, 8.5, 5], [3, 2, 4], schaduw)]),
             bone("nek", "body", [0, 12, -3], [cube(a, [-1, 12, -5], [2, 6, 2], veer)], rotation=[-12, 0, 0]),
             bone("head", "nek", [0, 18, -4], [cube(a, [-1.6, 17.2, -6.4], [3.2, 3, 3.6], veer, overrides={"north": gezicht}),
                                               cube(a, [-0.5, 18.0, -10.6], [1, 1, 4.2], snavel),
                                               cube(a, [-0.4, 20.0, -4.0], [0.8, 0.6, 3.0], schaduw)], rotation=[24, 0, 0])]
    bones += ears(a, (246, 248, 252), (246, 170, 196), 1.0, 20.0, -5.4, 1.4, 1.4)
    for kant, sx in (("left", 1), ("right", -1)):
        x = 2.5 if sx > 0 else -3.5
        bones.append(bone(f"vleugel_{kant}", "body", [sx * 2.5, 12.5, -3], [cube(a, [x, 8.5, -3.5], [1, 4.5, 9], schaduw)]))
        bones.append(bone(f"poot_{kant}", "body", [sx * 1.0, 8, 0], [cube(a, [sx * 1.0 - 0.4, 0, -0.4], [0.8, 8, 0.8], poot),
                                                                    cube(a, [sx * 1.0 - 0.8, 0, -1.8], [1.6, 0.4, 2.4], poot)]))
    return geo_("mistreiger", bones, 2, 2, [0, 0.6, 0])


def reiger_animaties():
    L = 8.0
    idle = {"head": {"rotation": kf([(0, [0, 0, 0]), (L * 0.2, [0, 0, 0]), (L * 0.22, [0, 40, 0]), (L * 0.45, [0, 40, 0]), (L * 0.47, [0, -20, 0]),
                                     (L * 0.7, [0, -20, 0]), (L * 0.72, [0, 0, 0]), (L, [0, 0, 0])])},
            "nek": {"rotation": golf(0, 3, L, keer=2, om=(-12, 0, 0))}, "body": {"scale": kf([(0, [1, 1, 1]), (L / 2, [1.02, 1.04, 1.02]), (L, [1, 1, 1])])}}
    idle.update(oor_idle(L, 18))
    vlieg = {"vleugel_left": {"rotation": golf(2, -60, 0.6, om=(0, 0, -30))}, "vleugel_right": {"rotation": golf(2, 60, 0.6, om=(0, 0, 30))},
             "poot_left": {"rotation": [70, 0, 0]}, "poot_right": {"rotation": [70, 0, 0]}, "nek": {"rotation": [30, 0, 0]},
             "head": {"rotation": [-20, 0, 0]}}
    op = {"body": {"position": kf([(0, [0, 0, 0]), (0.25, [0, -2, 0]), (0.5, [0, 3, 0]), (1.0, [0, 0, 0])])},
          "vleugel_left": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 0, -100]), (0.6, [0, 0, -20]), (1.0, [0, 0, -60])])},
          "vleugel_right": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 0, 100]), (0.6, [0, 0, 20]), (1.0, [0, 0, 60])])}}
    poets = {"nek": {"rotation": kf([(0, [-12, 0, 0]), (0.4, [20, 60, 0]), (1.6, [20, 60, 0]), (2.0, [-12, 0, 0])])},
             "head": {"rotation": kf([(0, [0, 0, 0]), (0.4, [30, 30, 0]), (0.8, [40, 40, 0]), (1.2, [30, 30, 0]), (1.6, [40, 40, 0]), (2.0, [0, 0, 0])])},
             "vleugel_left": {"rotation": kf([(0, [0, 0, 0]), (0.4, [0, 0, -30]), (1.6, [0, 0, -30]), (2.0, [0, 0, 0])])}}
    praat = {"head": knik(1.2, 12), "nek": {"rotation": golf(0, 10, 1.2, keer=2, om=(-12, 0, 0))}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "vlieg": anim(0.6, vlieg), "opvlieg": anim(1.0, op, False),
            "poets": anim(2.0, poets, False)}


# =====================================================================================================================
# oude_reigerguh
# =====================================================================================================================
REIGER_A = 168
NEK_N = 4


def oude_reigerguh(a):
    veer = gloei(alfa(plain((236, 244, 255), 6, lambda W_, H, rng, ar: [ar.__setitem__((slice(y, y + 1), slice(None), slice(0, 3)), (214, 228, 250))
                                                                         for y in range(0, H, 5)]), REIGER_A), dim=0.42)
    zacht = gloei(alfa(plain((246, 250, 255), 4), REIGER_A), dim=0.5)
    zwart = gloei(alfa(plain((60, 66, 96), 6), 190), dim=0.25)
    kroon = gloei(plain((236, 70, 80), 5), dim=0.8)
    snavel = gloei(alfa(plain((230, 214, 160), 4), 210), dim=0.5)
    poot = alfa(plain((120, 130, 170), 5), 190)
    gezicht = scherp(gloei(alfa(guh_face((236, 244, 255), eye_y=0.46, eye_dx=0.27, eye_r=0.16, snoet=False), 220), dim=0.5), 1.2)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 44, 0], [cube(a, [-12, 38, -16], [24, 20, 34], veer, overrides={"down": zacht}),
                                                cube(a, [-9, 42, 18], [18, 12, 10], zacht)])]
    # the tail plumes: long, black-tipped
    bones.append(bone("staart", "body", [0, 48, 26], [cube(a, [-8, 40, 26], [16, 10, 18], zacht), cube(a, [-7, 38, 40], [14, 6, 10], zwart)],
                      rotation=[20, 0, 0]))
    # the neck in segments, rising from the chest to the head
    ouder, y, z = "body", 52, -16
    for i in range(NEK_N):
        bones.append(bone(f"nek_{i}", ouder, [0, y, z], [cube(a, [-3.5, y, z - 5], [7, 14, 7], veer)], rotation=[-10 if i else -20, 0, 0]))
        ouder, y = f"nek_{i}", y + 13
    hy, hz = y, z - 2
    bones.append(bone("head", ouder, [0, hy, hz], [cube(a, [-5.5, hy, hz - 9], [11, 10, 12], veer, overrides={"north": gezicht}),
                                                   cube(a, [-3, hy + 10, hz - 6], [6, 2, 6], kroon)], rotation=[48, 0, 0]))
    bones.append(bone("snavel", "head", [0, hy + 3, hz - 9], [cube(a, [-1.6, hy + 3.6, hz - 27], [3.2, 2.6, 18], snavel)]))
    bones.append(bone("snavel_onder", "head", [0, hy + 3, hz - 9], [cube(a, [-1.3, hy + 2.2, hz - 25], [2.6, 1.4, 16], snavel)]))
    for b in ears(a, (236, 244, 255), (246, 170, 196), 4.0, hy + 9, hz - 3, 4.5, 4.5):
        bones.append(b)
    for kant, sx in (("left", 1), ("right", -1)):
        x0 = 12 if sx > 0 else -14
        bones.append(bone(f"vleugel_{kant}", "body", [sx * 12, 56, -10], [cube(a, [x0, 40, -14], [2, 16, 30], veer)]))
        bones.append(bone(f"vleugel_{kant}_punt", f"vleugel_{kant}", [sx * 12, 56, 16], [cube(a, [x0 + sx * 0.5, 42, 16], [1.5, 13, 20], zwart)]))
        bones.append(bone(f"poot_{kant}", "body", [sx * 5, 40, 0], [cube(a, [sx * 5 - 1.5, 0, -1.5], [3, 40, 3], poot),
                                                                   cube(a, [sx * 5 - 3, 0, -6], [6, 1.5, 9], poot)]))
    return geo_("oude_reigerguh", bones, 8, 8, [0, 3, 0])


def _slaap_pose():
    """Asleep, coiled round the stone lotus: legs folded under, the body low, the neck curled back with the head under a
    wing, the wings round itself."""
    p = {"body": {"position": [0, -36, 0], "rotation": [0, 0, 0]},
         "poot_left": {"rotation": [90, 0, 0]}, "poot_right": {"rotation": [90, 0, 0]},
         "nek_0": {"rotation": [-60, 0, 0]}, "nek_1": {"rotation": [60, 30, 0]}, "nek_2": {"rotation": [70, 30, 0]},
         "nek_3": {"rotation": [60, 30, 0]}, "head": {"rotation": [20, 20, 0]},
         "vleugel_left": {"rotation": [0, -20, 10]}, "vleugel_right": {"rotation": [0, 20, -10]},
         "ear_left": {"rotation": [0, 0, -20]}, "ear_right": {"rotation": [0, 0, 20]}}
    return p


def oude_reiger_animaties():
    L = 10.0
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (L / 2, [1.02, 1.03, 1.02]), (L, [1, 1, 1])])},
            "head": {"rotation": golf(1, 12, L, keer=1)}, "nek_3": {"rotation": golf(0, 4, L, keer=2, om=(-10, 0, 0))},
            "staart": {"rotation": golf(1, 4, L, keer=1, om=(20, 0, 0))}}
    idle.update(oor_idle(L, 12))
    slaap = {k: {c: kf([(0, v), (4.0, v)]) if isinstance(v, list) else v for c, v in ch.items()} for k, ch in _slaap_pose().items()}
    slaap["body"]["scale"] = kf([(0, [1, 1, 1]), (2.0, [1.03, 1.05, 1.03]), (4.0, [1, 1, 1])])
    # wake: from the coil, stand, spread the wings wide, fold them (4 s)
    rust = _slaap_pose()
    ontwaak = {}
    for b, ch in rust.items():
        for c, v in ch.items():
            ontwaak.setdefault(b, {})[c] = kf([(0, v), (0.6, v), (2.0, [0, 0, 0] if c != "position" else [0, 0, 0]), (4.0, [0, 0, 0])])
    ontwaak["head"]["rotation"] = kf([(0, [20, 20, 0]), (0.6, [20, 20, 0]), (2.0, [-20, 0, 0]), (2.6, [-30, 0, 0]), (3.4, [0, 0, 0]), (4.0, [0, 0, 0])])
    ontwaak["vleugel_left"]["rotation"] = kf([(0, [0, -20, 10]), (1.6, [0, 0, 0]), (2.2, [0, 0, -80]), (3.0, [0, 0, -80]), (3.6, [0, 0, 0]), (4.0, [0, 0, 0])])
    ontwaak["vleugel_right"]["rotation"] = kf([(0, [0, 20, -10]), (1.6, [0, 0, 0]), (2.2, [0, 0, 80]), (3.0, [0, 0, 80]), (3.6, [0, 0, 0]), (4.0, [0, 0, 0])])
    ontwaak["snavel_onder"] = {"rotation": kf([(0, [0, 0, 0]), (2.2, [0, 0, 0]), (2.5, [20, 0, 0]), (2.9, [0, 0, 0]), (4.0, [0, 0, 0])])}
    spreek = {"snavel_onder": {"rotation": golf(0, 9, 1.6, keer=3, om=(9, 0, 0))}, "head": {"rotation": golf(1, 8, 4.8, keer=1)},
              "nek_3": {"rotation": golf(0, 5, 4.8, keer=2, om=(-10, 0, 0))},
              "vleugel_left": {"rotation": kf([(0, [0, 0, 0]), (2.4, [0, 0, -25]), (4.8, [0, 0, 0])])}}
    praat = {"head": knik(1.2, 10), "snavel_onder": {"rotation": golf(0, 10, 1.2, keer=3, om=(10, 0, 0))}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "slaap": anim(4.0, slaap), "ontwaak": anim(4.0, ontwaak, False),
            "spreek": anim(4.8, spreek)}


# =====================================================================================================================
# wan_guh
# =====================================================================================================================
def wan_guh(a):
    kleed = met(plain((44, 46, 74), 6), lambda W_, H, rng, ar: ar.__setitem__((slice(H - max(2, H // 10), H), slice(None), slice(0, 3)), (120, 214, 236)))
    kleed = gloei(kleed, lambda b: b[..., 2].astype(int) > 200, 0.8)
    kap = plain((38, 40, 66), 6)
    schaduw_face = met(plain((14, 12, 24), 3), lambda W_, H, rng, ar: [disc(ar, W_ / 2 + sx * W_ * 0.2, H * 0.52, W_ * 0.08, (190, 250, 255), ry=H * 0.06)
                                                                        for sx in (-1, 1)])
    schaduw_face = scherp(gloei(schaduw_face, lambda b: b[..., 2].astype(int) > 200, 1.0), 2.0)
    vacht = (240, 226, 214)
    gezicht = scherp(guh_face(vacht, eye_y=0.44, eye_dx=0.25, eye_r=0.16), 3.0)
    staf = plain((110, 84, 60), 5)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 12, 0], [cube(a, [-5.5, 0, -4.5], [11, 20, 9], kleed), cube(a, [-6.5, 0, -5.5], [13, 6, 11], kleed)]),
             bone("head", "body", [0, 22, 0], [cube(a, [-4, 21, -4], [8, 8, 8], plain(vacht, 5), overrides={"north": gezicht})])]
    bones += ears(a, vacht, (246, 170, 196), 2.8, 28.4, -1.5, 3, 3)
    # the hood over the head (with holes for the ears), its shadow over the face
    bones.append(bone("kap", "head", [0, 30, 2], [cube(a, [-5, 20.5, -4.5], [10, 9.5, 10], kap, overrides={"north": W.RANDLOOS_Y["north"]}),
                                                  cube(a, [-5.5, 20, 4.5], [11, 6, 1.5], kap)]))
    bones.append(bone("kap_schaduw", "kap", [0, 25, -4.5], [cube(a, [-4.2, 21.2, -4.7], [8.4, 7.6, 0.4], schaduw_face)]))
    for kant, sx in (("left", 1), ("right", -1)):
        x = 5.5 if sx > 0 else -8.5
        bones.append(bone(f"arm_{kant}", "body", [sx * 6.5, 19, 0], [cube(a, [x, 8, -2], [3, 11, 4], kleed)]))
    bones.append(bone("staf", "arm_right", [-7, 9, 0], [cube(a, [-7.5, -12, -0.5], [1, 34, 1], staf),
                                                       cube(a, [-8.5, 21, -1.5], [3, 3, 3], gloei(plain((150, 230, 250), 4), dim=0.9))]))
    return geo_("wan_guh", bones, 2, 3, [0, 1, 0])


def wan_animaties():
    L = 8.0
    idle = {"body": {"rotation": golf(2, 2, L, keer=1)}, "head": {"rotation": golf(1, 10, L, keer=1, fase=1.0)},
            "arm_left": {"rotation": golf(0, 4, L, keer=2)}}
    idle.update(oor_idle(L, 10))
    verdwijn = {"root": {"scale": kf([(0, [1, 1, 1]), (0.4, [1.05, 1.05, 1.05]), (1.5, W.WEG)]),
                         "position": kf([(0, [0, 0, 0]), (1.5, [0, 10, 0])])}}
    # (a pose that lasts loops, so it holds the hood down: the lowering itself is a cut in the scene)
    ontkap = {"kap": {"rotation": kf([(0, [-110, 0, 0]), (2.0, [-110, 0, 0])]), "position": kf([(0, [0, -3, 3]), (2.0, [0, -3, 3])])},
              "kap_schaduw": {"scale": kf([(0, W.WEG), (2.0, W.WEG)])}, "head": {"rotation": golf(1, 6, 2.0, keer=1, om=(-8, 0, 0))}}
    wijs = {"arm_left": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-160, 0, 10]), (1.6, [-160, 0, 10]), (2.0, [0, 0, 0])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (0.5, [-20, 0, 0]), (1.6, [-20, 0, 0]), (2.0, [0, 0, 0])])}}
    praat = {"head": knik(1.2, 6)}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "verdwijn": anim(1.5, verdwijn, False), "ontkap": anim(2.0, ontkap),
            "wijs": anim(2.0, wijs, False)}


# =====================================================================================================================
# geestreus: four giant silhouettes
# =====================================================================================================================
REUS, REUS_RAND = (40, 58, 120), (90, 120, 200)


def _reus_huid():
    return gloei(alfa(plain(REUS, 8, lambda W_, H, rng, ar: [disc(ar, rng.uniform(0, W_), rng.uniform(0, H), rng.uniform(2, 5), REUS_RAND, 0.35)
                                                               for _ in range(max(2, W_ * H // 400))]), 120), dim=0.18)


def _reus_ogen():
    return gloei(alfa(plain((200, 240, 255), 3), 230), dim=1.0)


def geestreus(a):
    huid, oog, rand_ = _reus_huid(), _reus_ogen(), gloei(alfa(plain(REUS_RAND, 6), 140), dim=0.4)
    bones = [bone("root", None, [0, 0, 0], [])]
    # 0: a sitting guh as big as a hill, with its ears
    v0 = [cube(a, [-80, 0, -60], [160, 130, 120], huid), cube(a, [-70, 130, -70], [140, 110, 120], huid),
          cube(a, [-46, 186, -71], [22, 18, 2], oog), cube(a, [24, 186, -71], [22, 18, 2], oog)]
    bones.append(bone("vorm_0", "root", [0, 0, 0], v0))
    bones.append(bone("vorm_0_oor_l", "vorm_0", [55, 240, 0], [cube(a, [30, 236, -10], [50, 60, 16], rand_)]))
    bones.append(bone("vorm_0_oor_r", "vorm_0", [-55, 240, 0], [cube(a, [-80, 236, -10], [50, 60, 16], rand_)]))
    # 1: a sky serpent with fins and two small ears, high up
    v1 = []
    for i in range(7):
        dik = 60 - i * 6
        v1.append(cube(a, [-dik / 2, 220 + 20 * math.sin(i * 0.8) - dik / 2, -150 + 60 * i], [dik, dik, 60], huid))
    v1 += [cube(a, [-34, 196, -200], [68, 56, 50], huid), cube(a, [-26, 226, -201], [14, 10, 2], oog), cube(a, [12, 226, -201], [14, 10, 2], oog),
           cube(a, [16, 252, -184], [16, 20, 6], rand_), cube(a, [-32, 252, -184], [16, 20, 6], rand_),
           cube(a, [30, 210, -110], [70, 4, 40], rand_), cube(a, [-100, 210, -110], [70, 4, 40], rand_)]
    bones.append(bone("vorm_1", "root", [0, 220, 0], v1))
    # 2: an antlered deer spirit with guh ears
    v2 = [cube(a, [-40, 160, -120], [80, 70, 220], huid), cube(a, [-26, 210, -170], [52, 90, 50], huid), cube(a, [-30, 290, -200], [60, 50, 70], huid),
          cube(a, [-22, 308, -201], [12, 10, 2], oog), cube(a, [10, 308, -201], [12, 10, 2], oog),
          cube(a, [16, 336, -176], [14, 18, 6], rand_), cube(a, [-30, 336, -176], [14, 18, 6], rand_)]
    for sx in (-1, 1):
        x = 20 if sx > 0 else -26
        v2 += [cube(a, [x, 340, -160], [6, 70, 6], rand_), cube(a, [x + 6 if sx > 0 else x - 30, 380, -160], [30, 5, 5], rand_),
               cube(a, [x, 400, -150], [6, 30, 6], rand_)]
        for z in (-110, 80):
            v2.append(cube(a, [sx * 24 - 8, 0, z], [16, 160, 16], huid))
    bones.append(bone("vorm_2", "root", [0, 0, 0], v2))
    # 3: a robed figure with four arms
    v3 = [cube(a, [-50, 0, -40], [100, 300, 80], huid), cube(a, [-38, 300, -38], [76, 76, 76], huid),
          cube(a, [-24, 330, -39], [14, 12, 2], oog), cube(a, [10, 330, -39], [14, 12, 2], oog),
          cube(a, [14, 372, -10], [24, 30, 8], rand_), cube(a, [-38, 372, -10], [24, 30, 8], rand_)]
    bones.append(bone("vorm_3", "root", [0, 0, 0], v3))
    for n, (y, hoek) in enumerate(((270, 40), (210, 70))):
        for sx, kant in ((1, "l"), (-1, "r")):
            x = 50 if sx > 0 else -70
            bones.append(bone(f"vorm_3_arm_{n}_{kant}", "vorm_3", [sx * 50, y, 0], [cube(a, [x, y - 140, -12], [20, 140, 24], huid)],
                              rotation=[0, 0, -sx * hoek]))
    return geo_("geestreus", bones, 40, 40, [0, 12, 0])


def reus_animaties():
    L = 20.0
    idle = {"root": {"rotation": golf(1, 4, L, keer=1)}, "vorm_1": {"rotation": golf(1, 8, L, keer=1), "position": golf(1, 12, L, keer=2)},
            "vorm_0_oor_l": {"rotation": golf(2, 6, L, keer=2)}, "vorm_0_oor_r": {"rotation": golf(2, -6, L, keer=2)},
            "vorm_3_arm_0_l": {"rotation": golf(2, 8, L, keer=1, om=(0, 0, -40))}, "vorm_3_arm_0_r": {"rotation": golf(2, -8, L, keer=1, om=(0, 0, 40))},
            "vorm_3_arm_1_l": {"rotation": golf(2, 6, L, keer=1, fase=1.0, om=(0, 0, -70))},
            "vorm_3_arm_1_r": {"rotation": golf(2, -6, L, keer=1, fase=1.0, om=(0, 0, 70))}}
    verschijn = {"root": {"scale": kf([(0, W.WEG), (2.5, [1.02, 1.02, 1.02]), (3.0, [1, 1, 1])]), "position": kf([(0, [0, -60, 0]), (3.0, [0, 0, 0])])}}
    drijf = {"root": {"position": golf(1, 24, 12.0, keer=1), "rotation": golf(1, 10, 12.0, keer=1)}}
    praat = {"root": {"rotation": golf(0, 2, 1.2, keer=1)}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "verschijn": anim(3.0, verschijn, False), "drijf": anim(12.0, drijf)}


# =====================================================================================================================
# kijkmasker
# =====================================================================================================================
def _masker_gezicht(naam):
    def paint(W_, H, rng):
        from features import einde2_geestenwereld_tex as gt
        bron = np.asarray(gt.masker(naam), np.float32)
        ys = (np.arange(H) * 16 // H).clip(0, 15)
        xs = (np.arange(W_) * 16 // W_).clip(0, 15)
        a = bron[ys][:, xs].copy()
        leeg = a[..., 3] == 0
        a[leeg, :3] = (70, 52, 44)                                     # the wooden board behind the round mask
        a[leeg, 3] = 255
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def kijkmasker(a, soort=0):
    naam = MASKERS[soort]
    hout = plain((70, 52, 44), 5)
    gezicht = scherp(gloei(_masker_gezicht(naam), lambda b: (b[..., :3].astype(int).min(-1) > 225), 0.35), 2.0)
    vacht = plain((236, 226, 220), 5)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 14, 2], [cube(a, [-1, 13, 1], [2, 2, 2], hout)]),
             bone("head", "body", [0, 8, 0], [cube(a, [-7, 1, -1], [14, 14, 2], hout, overrides={"north": gezicht})])]
    bones += ears(a, (236, 226, 220), (246, 170, 196), 4.2, 13.5, -0.5, 3, 3)
    del vacht
    return geo_("kijkmasker", bones, 2, 2, [0, 0.5, 0])


def masker_animaties():
    L = 6.0
    idle = {"head": {"position": golf(1, 0.4, L, keer=1)}}
    idle.update(oor_idle(L, 12))
    knipper = {"head": {"scale": kf([(0, [1, 1, 1]), (0.2, [1.08, 0.9, 1]), (0.4, [1, 1, 1]), (0.6, [1.06, 0.94, 1]), (0.8, [1, 1, 1])])}}
    praat = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 0, 10]), (0.6, [0, 0, -10]), (0.9, [0, 0, 6]), (1.2, [0, 0, 0])])}}
    return {"idle": anim(L, idle), "praat": anim(1.2, praat, False), "knipper": anim(0.8, knipper, False)}


# id -> (maker(atlas, soort), texel density, animations())
MODELLEN = {
    "kennisvosje": (kennisvosje, 2.0, vos_animaties),
    "mistreiger": (lambda a_, s: mistreiger(a_), 2.5, reiger_animaties),
    "oude_reigerguh": (lambda a_, s: oude_reigerguh(a_), 0.22, oude_reiger_animaties),
    "wan_guh": (lambda a_, s: wan_guh(a_), 0.9, wan_animaties),
    "geestreus": (lambda a_, s: geestreus(a_), 0.035, reus_animaties),
    "kijkmasker": (kijkmasker, 2.0, masker_animaties),
}
# bones the renderer shows per look of geestreus (the other forms are hidden)
REUS_VORMEN = ["vorm_0", "vorm_1", "vorm_2", "vorm_3"]
