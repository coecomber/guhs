"""
Safari-dieren batch 14 (testronde 4, ronde 53: Het Kriebelhuis, Terrarium + Nachtzaal): kraaghagedis, gifkikker (3
kleuren), baardagaam, fruitvleermuis, galago, suikereekhoorn en fennek. Zelfde stijl en bestandsformaat als batch 10-13;
de gedeelde hulpjes komen uit batch12.py.

    python tools/features/safari_atelier/batch14.py [soort ...] [--geen-vellen]

Extra animaties: kraaghagedis kraag + ren (op twee pootjes), baardagaam zonnen, fruitvleermuis hang + eet, galago spring,
suikereekhoorn zweef, fennek oren. De gifkikker heeft kleurtextures gifkikker_{blauw,rood,geel}.png (zelfde uv; de basis is
blauw), net als de kameleon in batch 10.
"""
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, gezicht       # noqa: E402
from dieren import vier_poten, POTEN4                                   # noqa: E402
import batch12 as b12                                                   # noqa: E402
from batch12 import extra, verberg_in_standaard, maak_soort            # noqa: E402

UIT = os.path.abspath(os.path.join(HIER, "..", "..", "..", "build", "safari_atelier"))
SOORTEN = ["kraaghagedis", "gifkikker", "baardagaam", "fruitvleermuis", "galago", "suikereekhoorn", "fennek"]
NAMEN = {"kraaghagedis": "Kraaghagedis", "gifkikker": "Gifkikkertje", "baardagaam": "Baardagaam",
         "fruitvleermuis": "Fruitvleermuisje", "galago": "Galago", "suikereekhoorn": "Suikereekhoorn", "fennek": "Fennek"}
kf = at.kf
S = at.S
LEEG = at.transparent


# =====================================================================================================================
# kraaghagedis: slank, lange staart, een grote franjekraag om de nek (opgevouwen; "kraag" klapt hem uit), rent op 2 poten
# =====================================================================================================================
KH = (176, 150, 96)
KH_D = (126, 104, 64)
KRAAG = (236, 120, 70)
KRAAG_L = (252, 200, 110)


def kraaghagedis(atlas):
    def ribben(W, H, rng, a):
        cx, cy = W / 2, H
        for y in range(H):
            for x in range(W):
                hoek = np.arctan2(cy - y, x - cx)
                if np.hypot(x - cx, (cy - y) * W / (2 * H)) > W / 2:
                    a[y, x, 3] = 0                                     # rond waaiertje, hoeken doorzichtig
                elif abs((hoek * 6 / np.pi) % 1.0 - 0.5) < 0.1:
                    a[y, x, :3] = KRAAG_L
    huid = plain(KH, 8, lambda W, H, rng, a: [disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.4, 0.8) * S, KH_D, 0.7)
                                              for _ in range(max(2, W * H // 160))])
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3.4, 0], [cube(atlas, [-1.8, 2.2, -3.2], [3.6, 2.8, 6.4], huid)])]
    bones.append(bone("head", "body", [0, 4, -3.2], [cube(atlas, [-1.8, 3, -6.6], [3.6, 3, 3.6], huid,
                                                          {"north": gezicht(KH, eye_y=0.44, eye_dx=0.25, eye_r=0.2)})]))
    # de kraag: dun rond scherm achter de kop, opgevouwen klein (schaal 0.25); "kraag" zet hem op ware grootte
    kraag = plain(KRAAG, 6, ribben)
    bones.append(bone("kraag", "head", [0, 4.6, -3.4], [cube(atlas, [-3.8, 0.4, -3.6], [7.6, 5.6, 0.3], LEEG,
                                                              {"north": kraag, "south": kraag})]))
    bones.append(bone("tail", "body", [0, 3.4, 3.2], [cube(atlas, [-0.7, 2.6, 3.2], [1.4, 1.4, 10], huid)], rotation=[-6, 0, 0]))
    bones += vier_poten(atlas, "body", 1.0, -2.6, 1.6, [1.2, 2.4, 1.2], plain(KH_D, 6), 2.4)
    return geo("kraaghagedis", bones, atlas, 0.6, 0.6)


def kraaghagedis_anims():
    klein = {"kraag": {"scale": kf((0, [0.3, 0.3, 1]))}}
    an = at.animaties(legs4=POTEN4, staart="tail", extra_idle=dict(klein))
    for a in ("walk", "blij", "eet"):
        an["animations"][a]["bones"].update(klein)
    extra(an, "kraag", 3.0, {"kraag": {"scale": kf((0, [0.3, 0.3, 1]), (0.25, [1, 1, 1]), (2.6, [1, 1, 1]), (3.0, [0.3, 0.3, 1]))},
                             "head": {"rotation": kf((0, [0, 0, 0]), (0.25, [-14, 0, 0]), (2.6, [-14, 0, 0]), (3.0, [0, 0, 0]))},
                             "body": {"position": kf((0, [0, 0, 0]), (0.25, [0, 1, 0]), (2.6, [0, 1, 0]), (3.0, [0, 0, 0]))}},
          loop=False)
    L = 0.4
    ren = {"kraag": {"scale": kf((0, [0.3, 0.3, 1]))},
           "body": {"rotation": kf((0, [-45, 0, 0])), "position": kf((0, [0, 2.4, 0]), (L / 2, [0, 3.2, 0]), (L, [0, 2.4, 0]))},
           "leg_front_left": {"rotation": kf((0, [40, 0, 0]))}, "leg_front_right": {"rotation": kf((0, [40, 0, 0]))},
           "leg_back_left": {"rotation": kf((0, [60, 0, 0]), (L / 2, [0, 0, 0]), (L, [60, 0, 0]))},
           "leg_back_right": {"rotation": kf((0, [0, 0, 0]), (L / 2, [60, 0, 0]), (L, [0, 0, 0]))},
           "tail": {"rotation": kf((0, [30, 0, 0]))}}
    extra(an, "ren", L, ren)
    return an


# =====================================================================================================================
# gifkikkertje: klein rond kikkertje met grote oogjes en zwarte vlekjes; kleurvarianten blauw / rood / geel
# =====================================================================================================================
GIF_KLEUREN = {"": ((70, 130, 230), (40, 70, 160)), "blauw": ((70, 130, 230), (40, 70, 160)),
               "rood": ((230, 70, 70), (150, 36, 40)), "geel": ((250, 210, 60), (180, 140, 30))}


def gifkikker(atlas, kleur=""):
    K, D = GIF_KLEUREN[kleur]

    def vlekjes(W, H, rng, a):
        for _ in range(max(2, W * H // 140)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.5, 0.9) * S, (30, 26, 40), 0.85)
    huid = plain(K, 8, vlekjes)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2, 0], [cube(atlas, [-2.2, 0.8, -2], [4.4, 3, 4.6], huid)])]
    bones.append(bone("head", "body", [0, 2.6, -2], [cube(atlas, [-2.4, 1.2, -4.4], [4.8, 3.2, 2.6], plain(K, 8),
                                                          {"north": gezicht(K, eye_y=0.44, eye_dx=0.26, eye_r=0.22)})]))
    for side, sx in (("left", 1), ("right", -1)):
        o = [1.6, 0, 0.4] if sx > 0 else [-1.6 - 1.8, 0, 0.4]
        bones.append(bone(f"leg_back_{side}", "body", [sx * 2, 1.2, 1.2], [cube(atlas, o, [1.8, 1.2, 2.6], plain(D, 6))]))
        o = [1.4, 0, -2.2] if sx > 0 else [-1.4 - 1, 0, -2.2]
        bones.append(bone(f"leg_front_{side}", "body", [sx * 1.6, 1.2, -1.8], [cube(atlas, o, [1, 1.4, 1], plain(D, 6))]))
    return geo("gifkikker", bones, atlas, 0.4, 0.3)


def gifkikker_anims():
    an = at.animaties(extra_idle={"body": {"scale": kf((0, [1, 1, 1]), (0.6, [1.04, 0.96, 1.04]), (1.2, [1, 1, 1]), (3.0, [1, 1, 1]))}})
    L = 0.6
    hop = {"body": {"position": kf((0, [0, 0, 0]), (L / 2, [0, 3, 0]), (L, [0, 0, 0]))},
           "leg_back_left": {"rotation": kf((0, [0, 0, 0]), (L / 4, [-40, 0, 0]), (L, [0, 0, 0]))},
           "leg_back_right": {"rotation": kf((0, [0, 0, 0]), (L / 4, [-40, 0, 0]), (L, [0, 0, 0]))}}
    an["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": hop}
    extra(an, "hop", L, hop)
    return an


# =====================================================================================================================
# baardagaam: breed en plat, een stekelige 'baard' onder de kin, ligt graag te zonnen ("zonnen": plat, ogen half dicht)
# =====================================================================================================================
BA = (214, 176, 116)
BA_D = (170, 128, 76)
BA_L = (240, 214, 170)


def baardagaam(atlas):
    def schubjes(W, H, rng, a):
        for y in range(0, H, S):
            for x in range((y // S % 2) * S // 2, W, S):
                a[y:y + 1, x:x + S // 2, :3] = np.array(BA_D)
    huid = plain(BA, 8, schubjes)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2.4, 0], [cube(atlas, [-3, 1.2, -3.2], [6, 2.6, 7], huid, {"down": plain(BA_L, 6)})])]
    bones.append(bone("head", "body", [0, 2.8, -3.2], [cube(atlas, [-2.4, 1.6, -6.6], [4.8, 3, 3.4], huid,
                                                            {"north": gezicht(BA, eye_y=0.42, eye_dx=0.26, eye_r=0.19)})]))
    baard = []
    for x in (-2.2, -1.2, -0.2, 0.8, 1.8):
        baard.append(cube(atlas, [x, 0.9, -6.4], [0.5, 0.9, 0.5], plain(BA_D, 4)))
    bones.append(bone("baard", "head", [0, 1.6, -5], baard))
    bones.append(bone("tail", "body", [0, 2.2, 3.8], [cube(atlas, [-0.9, 1.4, 3.8], [1.8, 1.4, 8], huid)]))
    bones += vier_poten(atlas, "body", 2.2, -2.8, 2.2, [1.4, 1.6, 1.4], plain(BA_D, 6), 1.8)
    return geo("baardagaam", bones, atlas, 0.7, 0.4)


def baardagaam_anims():
    an = at.animaties(legs4=POTEN4, staart="tail")
    extra(an, "zonnen", 6.0, {"body": {"position": kf((0, [0, -0.8, 0])), "scale": kf((0, [1.08, 0.9, 1]), (3, [1.1, 0.88, 1]), (6, [1.08, 0.9, 1]))},
                              "leg_front_left": {"rotation": kf((0, [0, 0, 40]))}, "leg_front_right": {"rotation": kf((0, [0, 0, -40]))},
                              "leg_back_left": {"rotation": kf((0, [0, 0, 40]))}, "leg_back_right": {"rotation": kf((0, [0, 0, -40]))},
                              "head": {"rotation": kf((0, [-10, 0, 0]), (3, [-14, 0, 0]), (6, [-10, 0, 0]))}})
    return an


# =====================================================================================================================
# fruitvleermuisje: pluizig lijfje, vossensnuitje met grote oogjes, vliezige vleugels; "hang" (ondersteboven), "eet" fruit
# =====================================================================================================================
FV = (122, 84, 60)
FV_L = (196, 140, 92)
FV_VLIES = (86, 58, 52)
FRUIT = (240, 150, 60)


def fruitvleermuis(atlas):
    vacht = at.fluffy(FV, 14)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3, 0], [cube(atlas, [-1.8, 1.6, -1.6], [3.6, 4, 3.2], vacht, {"north": at.buik(FV, FV_L)})])]
    bones.append(bone("head", "body", [0, 5.6, -0.6], [
        cube(atlas, [-2, 5.4, -2.6], [4, 3.4, 3], at.fluffy(FV_L, 12), {"north": gezicht(FV_L, eye_y=0.44, eye_dx=0.25, eye_r=0.2)}),
        cube(atlas, [-0.7, 5.6, -3.4], [1.4, 1.2, 0.9], plain(FV_L, 6)),
        cube(atlas, [1.0, 8.6, -1.6], [1.2, 1.8, 0.5], plain(FV_VLIES, 4)),
        cube(atlas, [-2.2, 8.6, -1.6], [1.2, 1.8, 0.5], plain(FV_VLIES, 4))]))
    vlies = plain(FV_VLIES, 6, lambda W, H, rng, a: [a.__setitem__((slice(None), slice(x, x + 1), slice(0, 3)), (60, 40, 36))
                                                      for x in range(0, W, 2 * S)])
    for side, sx in (("left", 1), ("right", -1)):
        o = [1.8, 2.0, -0.4] if sx > 0 else [-1.8 - 4.6, 2.0, -0.4]
        bones.append(bone(f"wing_{side}", "body", [sx * 1.8, 5.2, 0], [cube(atlas, o, [4.6, 3.6, 0.3], vlies)],
                          rotation=[0, -sx * 70, 0]))                                  # opgevouwen langs het lijfje
    for side, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"leg_{side}", "body", [sx * 0.9, 1.6, 0], [cube(atlas, [sx * 0.9 - 0.3, 0, -0.3], [0.6, 1.6, 0.6], plain(FV_VLIES, 4))]))
    bones.append(bone("fruit", "head", [0, 5, -3.4], [cube(atlas, [-1.1, 3.8, -4.6], [2.2, 2.2, 2.2], plain(FRUIT, 10))]))
    return geo("fruitvleermuis", bones, atlas, 0.4, 0.6)


def fruitvleermuis_anims():
    an = at.animaties(legs2=["leg_left", "leg_right"], extra_idle={"head": {"rotation": kf((0, [0, 0, 0]), (1.5, [0, 15, 0]), (3.0, [0, 0, 0]))}})
    verberg_in_standaard(an, "fruit")
    an["animations"]["eet"]["bones"].pop("fruit", None)
    an["animations"]["eet"]["bones"]["fruit"] = {"scale": kf((0, [1, 1, 1]), (0.9, [0.6, 0.6, 0.6]), (1.0, [0, 0, 0]))}
    hang = {"root": {"rotation": kf((0, [180, 0, 0]), (1.5, [180, 0, 6]), (3.0, [180, 0, 0])), "position": kf((0, [0, 10, 0]))},
            "wing_left": {"rotation": kf((0, [0, 0, 0]))}, "wing_right": {"rotation": kf((0, [0, 0, 0]))},
            "fruit": {"scale": kf((0, [0, 0, 0]))}}
    extra(an, "hang", 3.0, hang)
    L = 0.4
    extra(an, "vlieg", L, {"fruit": {"scale": kf((0, [0, 0, 0]))},
                           "root": {"position": kf((0, [0, 6, 0]), (L / 2, [0, 6.8, 0]), (L, [0, 6, 0]))},
                           "wing_left": {"rotation": kf((0, [0, 70, -40]), (L / 2, [0, 70, 30]), (L, [0, 70, -40]))},
                           "wing_right": {"rotation": kf((0, [0, -70, 40]), (L / 2, [0, -70, -30]), (L, [0, -70, 40]))}})
    return an


# =====================================================================================================================
# galago (bushbaby): reuzenogen, grote ronde oortjes, lange achterpoten, lange pluimstaart; "spring" (enorme sprongen)
# =====================================================================================================================
GA = (186, 166, 140)
GA_L = (232, 220, 200)
GA_OOR = (232, 176, 168)


def galago(atlas):
    vacht = at.fluffy(GA, 14)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4.4, 0], [cube(atlas, [-1.8, 3.2, -1.6], [3.6, 4.4, 3.2], vacht, {"north": at.buik(GA, GA_L)})])]
    kop = gezicht(GA, eye_y=0.48, eye_dx=0.25, eye_r=0.24)
    oor = at.vorm_oor(GA, GA_OOR, punt=0.0)
    bones.append(bone("head", "body", [0, 7.6, -0.4], [
        cube(atlas, [-2.6, 7.4, -2.8], [5.2, 4.4, 3.8], vacht, {"north": kop}),
        cube(atlas, [1.6, 11.0, -1.2], [2.6, 2.8, 0.4], LEEG, {"north": oor, "south": oor}),
        cube(atlas, [-4.2, 11.0, -1.2], [2.6, 2.8, 0.4], LEEG, {"north": oor, "south": oor})]))
    for side, sx in (("left", 1), ("right", -1)):
        dij, voet = [0.9, 1.0, -0.4], [0.9, 0, -2.4]
        if sx < 0:
            dij, voet = at.mirror_x(dij, [1.6, 3.4, 2.4]), at.mirror_x(voet, [1.4, 0.7, 3.4])
        bones.append(bone(f"leg_{side}", "body", [sx * 1.7, 4.2, 0.4], [cube(atlas, dij, [1.6, 3.4, 2.4], vacht),
                                                                         cube(atlas, voet, [1.4, 0.7, 3.4], plain(GA_L, 6))]))
    bones.append(bone("tail", "body", [0, 4, 1.6], [cube(atlas, [-0.8, 3.6, 1.6], [1.6, 1.6, 10], at.fluffy(GA, 16))],
                      rotation=[-25, 0, 0]))
    return geo("galago", bones, atlas, 0.4, 0.8)


def galago_anims():
    an = at.animaties(legs2=["leg_left", "leg_right"], staart="tail")
    L = 1.0
    spring = {"body": {"position": kf((0, [0, 0, 0]), (0.15, [0, -0.8, 0]), (0.5, [0, 10, -6]), (1.0, [0, 0, -12])),
                       "rotation": kf((0, [0, 0, 0]), (0.5, [-20, 0, 0]), (1.0, [0, 0, 0]))},
              "leg_left": {"rotation": kf((0, [20, 0, 0]), (0.15, [30, 0, 0]), (0.5, [-70, 0, 0]), (1.0, [0, 0, 0]))},
              "leg_right": {"rotation": kf((0, [20, 0, 0]), (0.15, [30, 0, 0]), (0.5, [-70, 0, 0]), (1.0, [0, 0, 0]))},
              "tail": {"rotation": kf((0, [-25, 0, 0]), (0.5, [10, 0, 0]), (1.0, [-25, 0, 0]))}}
    extra(an, "spring", L, spring, loop=False)
    return an


# =====================================================================================================================
# suikereekhoorn: klein, grijs met een donkere rugstreep, grote oogjes, een vlieghuid tussen de pootjes; "zweef"
# =====================================================================================================================
SE = (170, 170, 182)
SE_L = (240, 236, 230)
SE_D = (70, 66, 76)


def suikereekhoorn(atlas):
    def streep(W, H, rng, a):
        a[:, int(W * 0.44):int(W * 0.56), :3] = np.array(SE_D)
    vacht = at.fluffy(SE, 12)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2.6, 0], [cube(atlas, [-2, 1.4, -2.6], [4, 2.8, 5.4], vacht,
                                                     {"up": at.met(vacht, streep), "down": plain(SE_L, 6)})])]
    bones.append(bone("head", "body", [0, 3.2, -2.6], [
        cube(atlas, [-2.2, 2.0, -5.6], [4.4, 3.6, 3.2], vacht, {"north": gezicht(SE, eye_y=0.46, eye_dx=0.26, eye_r=0.23)}),
        cube(atlas, [1.0, 5.4, -3.6], [1.4, 1.6, 0.4], plain(SE_L, 4)),
        cube(atlas, [-2.4, 5.4, -3.6], [1.4, 1.6, 0.4], plain(SE_L, 4))]))
    # de vlieghuid: in rust opgevouwen (schaal), bij "zweef" uitgespreid tussen voor- en achterpootjes
    vlies = plain(SE, 8)
    for side, sx in (("left", 1), ("right", -1)):
        o = [2.0, 2.4, -2.2] if sx > 0 else [-2.0 - 3.4, 2.4, -2.2]
        bones.append(bone(f"vlies_{side}", "body", [sx * 2, 2.8, 0], [cube(atlas, o, [3.4, 0.3, 4.6], vlies, {"down": plain(SE_L, 6)})]))
    bones += vier_poten(atlas, "body", 1.0, -2.4, 1.6, [1.0, 1.6, 1.0], plain(SE_D, 4), 1.6)
    bones.append(bone("tail", "body", [0, 3, 2.8], [cube(atlas, [-0.9, 2.2, 2.8], [1.8, 1.8, 6.5], at.fluffy(SE_D, 12))],
                      rotation=[-20, 0, 0]))
    return geo("suikereekhoorn", bones, atlas, 0.4, 0.4)


def suikereekhoorn_anims():
    dicht = {"vlies_left": {"scale": kf((0, [0.1, 1, 1]))}, "vlies_right": {"scale": kf((0, [0.1, 1, 1]))}}
    an = at.animaties(legs4=POTEN4, staart="tail", extra_idle=dict(dicht))
    for a in ("walk", "blij", "eet"):
        an["animations"][a]["bones"].update(dicht)
    L = 1.6
    extra(an, "zweef", L, {
        "vlies_left": {"scale": kf((0, [1, 1, 1]))}, "vlies_right": {"scale": kf((0, [1, 1, 1]))},
        "root": {"position": kf((0, [0, 6, 0]), (L, [0, 1, -10]))},
        "leg_front_left": {"rotation": kf((0, [-60, 0, 50]))}, "leg_front_right": {"rotation": kf((0, [-60, 0, -50]))},
        "leg_back_left": {"rotation": kf((0, [60, 0, 50]))}, "leg_back_right": {"rotation": kf((0, [60, 0, -50]))},
        "tail": {"rotation": kf((0, [0, 0, 0]))}}, loop=False)
    return an


# =====================================================================================================================
# fennek: klein zandkleurig vosje met REUSACHTIGE oren, pluimstaart met donker puntje
# =====================================================================================================================
FN = (236, 206, 160)
FN_L = (252, 240, 220)
FN_OOR = (240, 170, 160)


def fennek(atlas):
    vacht = at.fluffy(FN, 12)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4, 0], [cube(atlas, [-2.2, 2.4, -3], [4.4, 3.6, 6.4], vacht, {"north": at.buik(FN, FN_L)})])]
    bones += vier_poten(atlas, "body", 1.0, -2.6, 2.0, [1.3, 2.6, 1.3], vacht, 2.6)
    oor = at.vorm_oor(FN, FN_OOR, punt=0.55)
    bones.append(bone("head", "body", [0, 6, -2.8], [
        cube(atlas, [-2.6, 5.2, -6.4], [5.2, 4.2, 4], vacht, {"north": gezicht(FN, eye_y=0.42, eye_dx=0.25, eye_r=0.2, snoet=True)})]))
    for side, sx in (("left", 1), ("right", -1)):
        o = [0.8, 9.2, -4.6] if sx > 0 else [-0.8 - 3.4, 9.2, -4.6]
        bones.append(bone(f"ear_{side}", "head", [sx * 2, 9.2, -4.4], [cube(atlas, o, [3.4, 6, 0.5], LEEG, {"north": oor, "south": oor})],
                          rotation=[0, 0, -sx * 15]))
    bones.append(bone("tail", "body", [0, 4.4, 3.4], [cube(atlas, [-1.2, 3.2, 3.4], [2.4, 2.4, 6], at.fluffy(FN, 14)),
                                                     cube(atlas, [-1.2, 3.2, 9.4], [2.4, 2.4, 1.6], plain((120, 90, 70), 8))],
                      rotation=[-15, 0, 0]))
    return geo("fennek", bones, atlas, 0.5, 0.8)


def fennek_anims():
    an = at.animaties(legs4=POTEN4, staart="tail", oren=True)
    extra(an, "oren", 2.0, {"ear_left": {"rotation": kf((0, [0, 0, 0]), (0.3, [-20, 30, 0]), (1.2, [-20, 30, 0]), (2.0, [0, 0, 0]))},
                            "ear_right": {"rotation": kf((0, [0, 0, 0]), (0.5, [-20, -30, 0]), (1.4, [-20, -30, 0]), (2.0, [0, 0, 0]))},
                            "head": {"rotation": kf((0, [0, 0, 0]), (0.8, [0, 0, 12]), (1.6, [0, 0, -8]), (2.0, [0, 0, 0]))}}, loop=False)
    return an


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "kraaghagedis":
        a = Atlas(1410); maak_soort(d, soort, kraaghagedis(a), a, kraaghagedis_anims(), (0.58, 0.8))
    elif soort == "gifkikker":
        a = Atlas(1420); maak_soort(d, soort, gifkikker(a), a, gifkikker_anims(), (0.6, 0.85))
        for kleur in ("blauw", "rood", "geel"):
            ak = Atlas(1420); gifkikker(ak, kleur)
            ak.image().save(os.path.join(d, f"gifkikker_{kleur}.png"))
    elif soort == "baardagaam":
        a = Atlas(1430); maak_soort(d, soort, baardagaam(a), a, baardagaam_anims(), (0.58, 0.8))
    elif soort == "fruitvleermuis":
        a = Atlas(1440); maak_soort(d, soort, fruitvleermuis(a), a, fruitvleermuis_anims(), (0.6, 0.82))
    elif soort == "galago":
        a = Atlas(1450); maak_soort(d, soort, galago(a), a, galago_anims(), (0.6, 0.82))
    elif soort == "suikereekhoorn":
        a = Atlas(1460); maak_soort(d, soort, suikereekhoorn(a), a, suikereekhoorn_anims(), (0.6, 0.82))
    elif soort == "fennek":
        a = Atlas(1470); maak_soort(d, soort, fennek(a), a, fennek_anims(), (0.58, 0.8))
    else:
        raise SystemExit(f"onbekende soort {soort}")


def vellen(soorten, overzicht=True):
    b12.vellen_gedeeld(soorten, SOORTEN, NAMEN, 14, "Kriebelhuis, Terrarium + Nachtzaal", UIT, overzicht)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    b12.alles(sys.modules[__name__], args or SOORTEN, met_vellen="--geen-vellen" not in sys.argv)
