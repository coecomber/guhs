"""
Safari, batch 10 (ronde 50-51, het Kriebelhuis + extra dieren met afwijkende vormen): kameleon, reuzenschildpad, slang,
hoornkikker, kiwi, schubdier. Zelfde stijl, werkwijze en bestandsformaat als batch 1-9 (atelier/dieren/blad, niet aangepast).

    python tools/features/safari_atelier/batch10.py [soort ...] [overzicht]
        geen argumenten: alle zes; modellen + slaapoogjes + extra animaties naar de mod-assets, de vellen en
        het overzicht naar build/safari_atelier/ en een kopie per soort naar ../../safari-ontwerp/dieren/<soort>/.

Per soort: <soort>.geo.json/.png, <soort>_baby.*, <soort>.animation.json (idle, walk, blij, eet, slaap, rust + per soort:
kameleon kijk, reuzenschildpad verstop, slang kopje, hoornkikker hop, kiwi snuffel, schubdier oprollen),
<soort>[_baby]_slaap.png, <soort>.bbmodel en <soort>_voorstel.png. De kameleon heeft ook kleurvarianten
(kameleon_<kleur>.png, zelfde uv) voor het langzaam van kleur veranderen in de game. Nog NIET in soorten.json (dat gebeurt
bij de bouw van het Kriebelhuis).
"""
import json
import os
import shutil
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, gezicht       # noqa: E402
from dieren import baby, POTEN4                                         # noqa: E402

UIT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "build", "safari_atelier"))
SOORTEN = ["kameleon", "reuzenschildpad", "slang", "hoornkikker", "kiwi", "schubdier"]
NAMEN = {"kameleon": "Kameleon", "reuzenschildpad": "Reuzenschildpad", "slang": "Slang", "hoornkikker": "Hoornkikker",
         "kiwi": "Kiwi", "schubdier": "Schubdier"}


def extra(anims, naam, lengte, botten, loop=True):
    anims["animations"][naam] = {"loop": loop, "animation_length": lengte, "bones": botten}


def mond(kleur, y=0.74, breed=0.62, dik=1):
    """Een brede glimlach-mondlijn onderaan een gezicht (spot-functie)."""
    def f(W, H, rng, a):
        cy = H * y
        for x in range(int(W * (0.5 - breed / 2)), int(W * (0.5 + breed / 2))):
            t = (x + 0.5 - W / 2) / (W * breed / 2)
            yy = int(cy - (1 - t * t) * H * 0.06 * -1)
            for d in range(dik * at.S // 2 + 1):
                if 0 <= yy + d < H:
                    a[yy + d, x, :3] = kleur
    return f


# =====================================================================================================================
# kameleon: krulstaart, torentjesogen die draaien, kam op de kop, pootjes als knijpertjes; kleurvarianten
# =====================================================================================================================
KAMELEON_KLEUREN = {
    "": ((112, 200, 96), (230, 220, 112), (78, 150, 70)),          # groen met gele streep (basis)
    "roze": ((236, 132, 176), (255, 214, 120), (196, 92, 140)),
    "blauw": ((96, 170, 226), (232, 244, 160), (62, 120, 186)),
    "oranje": ((242, 150, 70), (255, 230, 140), (196, 104, 44)),
}


def kameleon(atlas, kleur=""):
    K, STREEP, D = KAMELEON_KLEUREN[kleur]

    def streep(W, H, rng, a):
        a[int(H * 0.55):int(H * 0.72), :, :3] = np.array(STREEP) + rng.normal(0, 4, (int(H * 0.72) - int(H * 0.55), W, 1))
    huid = plain(K, 8, streep)
    effen = plain(K, 8)
    donker = plain(D, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [cube(atlas, [-2.2, 4, -4], [4.4, 5.4, 8.5], huid, {"down": plain(STREEP, 6)}),
                                              cube(atlas, [-0.6, 9.4, -3.4], [1.2, 1.0, 7], donker)])]           # rugkam
    for voor, z in (("front", -3.2), ("back", 2.6)):
        for side, sx in (("left", 1), ("right", -1)):
            o = [1.6, 0, z] if sx > 0 else [-2.8, 0, z]
            bones.append(bone(f"leg_{voor}_{side}", "body", [o[0] + 0.6, 4.6, o[2] + 0.6],
                              [cube(atlas, o, [1.2, 4.6, 1.2], effen),
                               cube(atlas, [o[0] - 0.3, 0, o[2] - 0.5], [1.8, 0.8, 2.2], donker)]))
    bones.append(bone("head", "body", [0, 7, -4], [
        cube(atlas, [-2.6, 4.6, -9], [5.2, 5, 5.2], effen, {"north": gezicht(K, eye_y=0.46, eye_r=0.18)}),
        cube(atlas, [-1.4, 9.6, -7.4], [2.8, 2.2, 3.6], donker)]))                                             # helmkam
    # torentjesogen: een groen kegeltje opzij van de kop met een donker pupilletje; ze draaien los van elkaar
    for side, sx in (("left", 1), ("right", -1)):
        o = [2.6, 6.6, -8] if sx > 0 else [-4.0, 6.6, -8]
        oog = plain(K, 6, lambda W, H, rng, a: (disc(a, W / 2, H / 2, W * 0.36, (250, 250, 240), 1.0),
                                                 disc(a, W / 2, H / 2, W * 0.2, (40, 30, 48), 1.0)))
        bones.append(bone(f"eye_{side}", "head", [sx * 3.0, 7.6, -7.4],
                          [cube(atlas, o, [1.4, 2.0, 2.0], effen, {"east" if sx > 0 else "west": oog})]))
    # krulstaart: steeds kleinere stukjes die naar beneden en naar binnen krullen (alleen bot-rotaties)
    bones.append(bone("tail", "body", [0, 6.4, 4.5], [cube(atlas, [-1.2, 5, 4.5], [2.4, 2.6, 4], huid)], rotation=[-20, 0, 0]))
    bones.append(bone("tail2", "tail", [0, 6.2, 8.5], [cube(atlas, [-1.0, 5.1, 8.5], [2.0, 2.2, 3.2], effen)], rotation=[-55, 0, 0]))
    bones.append(bone("tail3", "tail2", [0, 6.1, 11.7], [cube(atlas, [-0.8, 5.3, 11.7], [1.6, 1.8, 2.6], effen)], rotation=[-70, 0, 0]))
    bones.append(bone("tail4", "tail3", [0, 6.1, 14.3], [cube(atlas, [-0.6, 5.5, 14.3], [1.2, 1.4, 2.2], donker)], rotation=[-80, 0, 0]))
    return geo("kameleon" + (f"_{kleur}" if kleur else ""), bones, atlas, 1.0, 0.9)


def kameleon_anims():
    oogjes = {"eye_left": {"rotation": at.kf((0, [0, 0, 0]), (0.7, [0, 30, 0]), (1.4, [0, 30, 0]), (2.0, [0, -25, 0]),
                                             (3.0, [0, 0, 0]))},
              "eye_right": {"rotation": at.kf((0, [0, 0, 0]), (0.9, [0, -35, 0]), (1.8, [0, 10, 0]), (2.4, [0, 10, 0]),
                                              (3.0, [0, 0, 0]))},
              "tail4": {"rotation": at.kf((0, [-80, 0, 0]), (1.5, [-95, 0, 0]), (3.0, [-80, 0, 0]))}}
    walk = {"body": {"rotation": at.kf((0, [0, 0, 4]), (0.6, [0, 0, -4]), (1.2, [0, 0, 4]))}}
    anims = at.animaties(legs4=POTEN4, extra_idle=oogjes, extra_walk=walk)
    anims["animations"]["walk"]["animation_length"] = 0.8                       # (de kf-tijden van extra_walk lopen door)
    # kijk: de oogjes draaien alle kanten op terwijl hij stilstaat, de staart krult strakker
    extra(anims, "kijk", 2.4, {
        "eye_left": {"rotation": at.kf((0, [0, 0, 0]), (0.4, [-30, 40, 0]), (0.8, [20, -20, 0]), (1.4, [0, 45, 0]), (2.4, [0, 0, 0]))},
        "eye_right": {"rotation": at.kf((0, [0, 0, 0]), (0.5, [25, -40, 0]), (1.0, [-20, 30, 0]), (1.6, [0, -45, 0]), (2.4, [0, 0, 0]))},
        "tail3": {"rotation": at.kf((0, [-70, 0, 0]), (1.2, [-95, 0, 0]), (2.4, [-70, 0, 0]))}})
    return anims


# =====================================================================================================================
# reuzenschildpad: vadsig koepelschild, dikke olifantenpootjes, lange nek
# =====================================================================================================================
RS = (150, 136, 104)
RS_D = (104, 92, 70)
SCHILD = (122, 104, 70)
SCHILD_L = (176, 154, 104)
RAND = (92, 78, 56)


def reuzenschildpad(atlas):
    schild = at.lijn_vlekken(SCHILD, SCHILD_L, cel=5.0, lijn=1.0)
    huid = plain(RS, 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [
                 cube(atlas, [-6.5, 3.4, -7], [13, 2, 14], plain(RAND, 6)),                     # schildrand
                 cube(atlas, [-6, 5.4, -6.5], [12, 4, 13], schild),
                 cube(atlas, [-4.8, 9.4, -5.2], [9.6, 2.6, 10.4], schild),
                 cube(atlas, [-3, 12, -3.4], [6, 1.4, 6.8], schild),
                 cube(atlas, [-5.6, 2.4, -6.2], [11.2, 1.2, 12.4], plain(SCHILD_L, 6))])]          # buikschild
    for voor, z in (("front", -5), ("back", 2.5)):
        for side, sx in (("left", 1), ("right", -1)):
            o = [3.2, 0, z] if sx > 0 else [-6.4, 0, z]
            bones.append(bone(f"leg_{voor}_{side}", "body", [o[0] + 1.6, 3.6, o[2] + 1.6],
                              [cube(atlas, o, [3.2, 3.6, 3.2], huid, {"down": plain(RS_D, 6)})]))
    bones.append(bone("neck", "body", [0, 6, -6], [cube(atlas, [-1.5, 4.6, -11.4], [3, 3, 6], huid)], rotation=[-18, 0, 0]))
    bones.append(bone("head", "neck", [0, 6.2, -11.2], [
        cube(atlas, [-2.6, 5.2, -15.4], [5.2, 4.4, 4.6], huid, {"north": gezicht(RS, eye_y=0.42, eye_r=0.18)})]))
    bones.append(bone("tail", "body", [0, 4.6, 6.8], [cube(atlas, [-0.6, 4, 6.8], [1.2, 1.2, 1.8], huid)]))
    return geo("reuzenschildpad", bones, atlas, 1.3, 1.0)


def reuzenschildpad_anims():
    L = 1.6                                                                       # super traag
    walk = {}
    for i, n in enumerate(POTEN4):
        s = 16 if i in (0, 3) else -16
        walk[n] = {"rotation": at.kf((0, [s, 0, 0]), (L / 2, [-s, 0, 0]), (L, [s, 0, 0]))}
    walk["neck"] = {"rotation": at.kf((0, [-18, 0, 0]), (L / 2, [-14, 0, 0]), (L, [-18, 0, 0]))}
    anims = at.animaties(extra_idle={"neck": {"rotation": at.kf((0, [-18, 0, 0]), (2.0, [-26, 0, 0]), (4.0, [-18, 0, 0]))}},
                         extra_walk=walk)
    anims["animations"]["walk"]["animation_length"] = L
    anims["animations"]["walk"]["bones"]["body"] = {"position": at.kf((0, [0, 0, 0]), (L / 2, [0, 0.3, 0]), (L, [0, 0, 0]))}
    anims["animations"]["idle"]["animation_length"] = 4.0
    # verstop: kop en pootjes trekken in het schild (heel traag), even blijven, en weer uit
    weg = {"neck": {"position": at.kf((0, [0, 0, 0]), (0.8, [0, -0.8, 5.5]), (2.6, [0, -0.8, 5.5]), (3.4, [0, 0, 0])),
                    "rotation": at.kf((0, [-18, 0, 0]), (0.8, [0, 0, 0]), (2.6, [0, 0, 0]), (3.4, [-18, 0, 0]))}}
    for n in POTEN4:
        weg[n] = {"scale": at.kf((0, [1, 1, 1]), (0.8, [1, 0.25, 1]), (2.6, [1, 0.25, 1]), (3.4, [1, 1, 1]))}
    weg["root"] = {"position": at.kf((0, [0, 0, 0]), (0.8, [0, -2.6, 0]), (2.6, [0, -2.6, 0]), (3.4, [0, 0, 0]))}
    extra(anims, "verstop", 3.4, weg, loop=False)
    return anims


# =====================================================================================================================
# slang: ligt opgerold in twee ringen, het kopje komt uit het midden omhoog
# =====================================================================================================================
SL = (150, 210, 110)
SL_D = (96, 160, 74)
SL_BUIK = (240, 226, 150)
TONG = (236, 92, 120)


def slang(atlas):
    def ruitjes(W, H, rng, a):
        stap = 3 * at.S
        for y in range(H):
            for x in range(W):
                if abs((x % stap) - stap / 2) + abs((y % stap) - stap / 2) < stap * 0.28:
                    a[y, x, :3] = SL_D
    huid = plain(SL, 6, ruitjes)
    effen = plain(SL, 6)
    buikje = plain(SL_BUIK, 4)

    def ring(r_buiten, dik, y, h):
        """Een vierkante ring (4 kubussen) met buitenmaat 2*r_buiten, dikte dik, op hoogte y."""
        b = r_buiten
        return [cube(atlas, [-b, y, -b], [2 * b, h, dik], huid, {"down": buikje}),
                cube(atlas, [-b, y, b - dik], [2 * b, h, dik], huid, {"down": buikje}),
                cube(atlas, [-b, y, -b + dik], [dik, h, 2 * b - 2 * dik], huid, {"down": buikje}),
                cube(atlas, [b - dik, y, -b + dik], [dik, h, 2 * b - 2 * dik], huid, {"down": buikje})]
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 1.5, 0], ring(5.0, 2.6, 0, 2.6) + ring(3.6, 2.4, 2.6, 2.4)),
             bone("neck", "body", [0, 3, 0], [cube(atlas, [-1.1, 2.4, -1.1], [2.2, 4.6, 2.2], effen, {"north": buikje})]),
             bone("head", "neck", [0, 7, 0], [
                 cube(atlas, [-2.2, 6.6, -3.6], [4.4, 3.0, 4.8], effen, {"north": gezicht(SL, eye_y=0.40, eye_r=0.19)})]),
             bone("tong", "head", [0, 7, -3.6], [cube(atlas, [-0.3, 6.9, -4.8], [0.6, 0.25, 1.2], plain(TONG, 4))]),
             # het puntje van de staart steekt een beetje uit de buitenste ring
             bone("tail", "body", [4.6, 1.3, 4.6], [cube(atlas, [4.2, 0, 4.2], [3, 1.6, 1.6], effen)], rotation=[0, -30, 0])]
    return geo("slang", bones, atlas, 1.0, 0.8)


def slang_anims():
    adem = {"body": {"scale": at.kf((0, [1, 1, 1]), (1.5, [1.03, 1.05, 1.03]), (3.0, [1, 1, 1]))},
            "head": {"rotation": at.kf((0, [0, 0, 0]), (1.0, [0, 15, 0]), (2.0, [0, -15, 0]), (3.0, [0, 0, 0]))},
            "tong": {"scale": at.kf((0, [1, 1, 0]), (2.2, [1, 1, 0]), (2.3, [1, 1, 1]), (2.45, [1, 1, 0]), (2.55, [1, 1, 1]),
                                    (2.7, [1, 1, 0]), (3.0, [1, 1, 0]))}}
    walk = {"body": {"scale": at.kf((0, [1, 1, 1]), (0.4, [1.06, 0.96, 0.94]), (0.8, [1, 1, 1])),
                     "rotation": at.kf((0, [0, -6, 0]), (0.4, [0, 6, 0]), (0.8, [0, -6, 0]))},
            "neck": {"rotation": at.kf((0, [-20, 0, 0]))}}
    anims = at.animaties(extra_idle=adem, extra_walk=walk)
    # kopje: het kopje komt nieuwsgierig hoog uit de krul omhoog, kijkt rond en zakt weer
    extra(anims, "kopje", 3.0, {
        "neck": {"position": at.kf((0, [0, 0, 0]), (0.6, [0, 3.2, 0]), (2.4, [0, 3.2, 0]), (3.0, [0, 0, 0])),
                 "scale": at.kf((0, [1, 1, 1]), (0.6, [1, 1.5, 1]), (2.4, [1, 1.5, 1]), (3.0, [1, 1, 1]))},
        "head": {"rotation": at.kf((0, [0, 0, 0]), (0.9, [-10, 25, 0]), (1.5, [-10, -25, 0]), (2.1, [-10, 0, 0]), (3.0, [0, 0, 0]))},
        "tong": {"scale": at.kf((0, [1, 1, 0]), (1.0, [1, 1, 1]), (1.15, [1, 1, 0]), (1.3, [1, 1, 1]), (1.45, [1, 1, 0]))}},
        loop=False)
    for a in ("walk", "blij", "eet"):
        anims["animations"][a]["bones"].setdefault("tong", {"scale": at.kf((0, [1, 1, 0]))})
    return anims


# =====================================================================================================================
# hoornkikker: zo rond als een bal, een enorme glimlachmond, twee hoorntjes boven de oogjes, pootjes opzij
# =====================================================================================================================
HK = (132, 186, 84)
HK_VLEK = (196, 132, 62)
HK_BUIK = (236, 224, 168)
HK_MOND = (70, 52, 46)


def hoornkikker(atlas):
    def vlekken(W, H, rng, a):
        for _ in range(max(2, W * H // 220)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(1.0, 2.2) * at.S, HK_VLEK, 1.0)
    huid = plain(HK, 8, vlekken)
    # de bal in twee helften: onder (body, buikje) en boven (head, gezicht + mond), zodat de kop mee kan knikken
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4, 0], [
                 cube(atlas, [-5, 1, -5], [10, 3.6, 10], huid, {"down": plain(HK_BUIK, 6)}),
                 cube(atlas, [-4.2, 0.2, -4.2], [8.4, 0.8, 8.4], plain(HK_BUIK, 6))]),
             bone("head", "body", [0, 4.6, 0], [
                 cube(atlas, [-5, 4.6, -5], [10, 4.4, 10], huid,
                      {"north": at.met(gezicht(HK, eye_y=0.36, eye_dx=0.28, eye_r=0.17, blush=True), mond(HK_MOND, y=0.84, breed=0.76))})])]
    # hoorntjes boven de oogjes
    for side, sx in (("left", 1), ("right", -1)):
        o = [1.6, 9, -4.6] if sx > 0 else [-3.0, 9, -4.6]
        bones.append(bone(f"hoorn_{side}", "head", [sx * 2.3, 9, -3.8], [cube(atlas, o, [1.4, 1.6, 1.6], plain(HK_VLEK, 6))],
                          rotation=[-15, 0, sx * -10]))
    # pootjes: korte voorpootjes, grote achterpoten plat opzij
    for side, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"leg_front_{side}", "body", [sx * 3.5, 2, -4],
                          [cube(atlas, [2.8, 0, -5.4] if sx > 0 else [-4.2, 0, -5.4], [1.4, 2, 2], plain(HK, 6))]))
        bones.append(bone(f"leg_back_{side}", "body", [sx * 4.5, 2, 2.5],
                          [cube(atlas, [4.4, 0, 0.6] if sx > 0 else [-7.4, 0, 0.6], [3, 1.6, 4.2], huid)]))
    return geo("hoornkikker", bones, atlas, 0.8, 0.8)


def hoornkikker_anims():
    hop = {"root": {"position": at.kf((0, [0, 0, 0]), (0.2, [0, 3.0, 0]), (0.4, [0, 0, 0]), (0.8, [0, 0, 0]))},
           "body": {"scale": at.kf((0, [1.06, 0.9, 1.06]), (0.12, [0.95, 1.08, 0.95]), (0.4, [1.06, 0.92, 1.06]), (0.8, [1, 1, 1]))},
           "leg_back_left": {"rotation": at.kf((0, [0, 0, 0]), (0.15, [-35, 0, 0]), (0.4, [0, 0, 0]))},
           "leg_back_right": {"rotation": at.kf((0, [0, 0, 0]), (0.15, [-35, 0, 0]), (0.4, [0, 0, 0]))}}
    adem = {"body": {"scale": at.kf((0, [1, 1, 1]), (0.5, [1.04, 0.97, 1.04]), (1.0, [1, 1, 1]), (1.5, [1.04, 0.97, 1.04]),
                                    (2.0, [1, 1, 1]), (3.0, [1, 1, 1]))}}
    anims = at.animaties(extra_idle=adem, extra_walk=hop)
    # hop: een vrolijk sprongetje op de plek
    extra(anims, "hop", 0.8, {k: v for k, v in hop.items()}, loop=False)
    return anims


# =====================================================================================================================
# kiwi: rond pluizig bolletje met een lange gebogen snavel en korte stevige pootjes
# =====================================================================================================================
KW = (150, 112, 74)
KW_D = (112, 82, 54)
KW_SNAVEL = (226, 206, 160)
KW_POOT = (190, 170, 130)


def kiwi(atlas):
    def streepjes(W, H, rng, a):
        for _ in range(W * H // 40):
            x, y = int(rng.uniform(0, W)), int(rng.uniform(0, H))
            a[y:y + 2, x, :3] = KW_D
    veer = plain(KW, 10, streepjes)
    poot = plain(KW_POOT, 4)
    snavel = plain(KW_SNAVEL, 4)
    bones = [bone("root", None, [0, 0, 0], [])]
    for side, sx in (("left", 1), ("right", -1)):
        p, v = [0.6, 0, -0.6], [0.2, 0, -2.0]
        if sx < 0:
            p, v = at.mirror_x(p, [1.4, 2.6, 1.4]), at.mirror_x(v, [2.2, 0.6, 2.6])
        bones.append(bone(f"leg_{side}", "root", [sx * 1.6, 2.6, 0], [cube(atlas, p, [1.4, 2.6, 1.4], poot),
                                                                       cube(atlas, v, [2.2, 0.6, 2.6], poot)]))
    bones.append(bone("body", "root", [0, 6, 0], [cube(atlas, [-4.5, 2.2, -4.5], [9, 8, 9.5], veer)]))
    bones.append(bone("head", "body", [0, 8.2, -4], [
        cube(atlas, [-3, 6.6, -7.4], [6, 5, 3.4], veer, {"north": gezicht(KW, eye_y=0.38, eye_dx=0.27, eye_r=0.16)})]))
    # de lange gebogen snavel: drie stukjes die steeds iets meer naar beneden buigen (bot-rotaties)
    bones.append(bone("snavel", "head", [0, 8, -7.4], [cube(atlas, [-0.6, 7.4, -10.4], [1.2, 1.1, 3], snavel)], rotation=[6, 0, 0]))
    bones.append(bone("snavel2", "snavel", [0, 7.95, -10.4], [cube(atlas, [-0.5, 7.45, -13.0], [1.0, 1.0, 2.6], snavel)], rotation=[8, 0, 0]))
    bones.append(bone("snavel3", "snavel2", [0, 7.95, -13.0], [cube(atlas, [-0.4, 7.5, -15.0], [0.8, 0.9, 2.0], snavel)], rotation=[10, 0, 0]))
    return geo("kiwi", bones, atlas, 0.9, 0.8)


def kiwi_anims():
    anims = at.animaties(legs2=["leg_left", "leg_right"], waggel=True)
    # snuffel: de snavel prikt een paar keer in de grond (de hele kop buigt voorover)
    extra(anims, "snuffel", 2.0, {
        "head": {"rotation": at.kf((0, [0, 0, 0]), (0.3, [40, 0, 0]), (0.5, [32, 0, 0]), (0.7, [42, 0, 0]), (0.9, [32, 0, 0]),
                                   (1.1, [42, 0, 0]), (1.5, [38, 0, 6]), (2.0, [0, 0, 0]))},
        "body": {"rotation": at.kf((0, [0, 0, 0]), (0.3, [12, 0, 0]), (1.5, [12, 0, 0]), (2.0, [0, 0, 0]))}}, loop=False)
    return anims


# =====================================================================================================================
# schubdier: lijf vol schubben, klein puntsnuitje, lange dikke schubbenstaart; rolt zich op tot een bal
# =====================================================================================================================
SD = (178, 138, 92)
SD_SCHUB = (122, 88, 56)
SD_HUID = (232, 196, 170)


def schubdier(atlas):
    schub = at.schubben(SD, SD_SCHUB)
    huid = plain(SD_HUID, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 5, 0], [cube(atlas, [-3.2, 2.6, -5], [6.4, 5.4, 10], schub, {"down": huid})])]
    for voor, z in (("front", -4), ("back", 3)):
        for side, sx in (("left", 1), ("right", -1)):
            o = [1.8, 0, z] if sx > 0 else [-3.4, 0, z]
            bones.append(bone(f"leg_{voor}_{side}", "body", [o[0] + 0.8, 2.8, o[2] + 0.8],
                              [cube(atlas, o, [1.6, 2.8, 1.6], huid)]))
    bones.append(bone("head", "body", [0, 5.6, -5], [
        cube(atlas, [-2.2, 3.6, -9], [4.4, 3.8, 4], schub, {"north": gezicht(SD_HUID, eye_y=0.40, eye_r=0.18), "down": huid}),
        cube(atlas, [-1.2, 3.8, -11], [2.4, 2, 2], huid, {"north": at.neusje(SD_HUID, (70, 46, 40), mond=False)})]))
    bones.append(bone("tail", "body", [0, 4.6, 5], [cube(atlas, [-2.4, 2.8, 5], [4.8, 3.4, 5], schub, {"down": huid})], rotation=[10, 0, 0]))
    bones.append(bone("tail_tip", "tail", [0, 4.2, 10], [cube(atlas, [-1.6, 3.0, 10], [3.2, 2.4, 4.4], schub, {"down": huid})], rotation=[12, 0, 0]))
    return geo("schubdier", bones, atlas, 0.9, 0.7)


def schubdier_anims():
    anims = at.animaties(legs4=POTEN4, staart="tail")
    # oprollen: kop naar binnen, staart eromheen, pootjes weg: een schubbenbal; even blijven, dan weer uitrollen
    rol = {"head": {"rotation": at.kf((0, [0, 0, 0]), (0.5, [110, 0, 0]), (2.5, [110, 0, 0]), (3.0, [0, 0, 0]))},
           "tail": {"rotation": at.kf((0, [10, 0, 0]), (0.5, [-120, 0, 0]), (2.5, [-120, 0, 0]), (3.0, [10, 0, 0]))},
           "tail_tip": {"rotation": at.kf((0, [12, 0, 0]), (0.5, [-110, 0, 0]), (2.5, [-110, 0, 0]), (3.0, [12, 0, 0]))},
           "body": {"rotation": at.kf((0, [0, 0, 0]), (0.5, [-12, 0, 0]), (2.5, [-12, 0, 0]), (3.0, [0, 0, 0])),
                    "scale": at.kf((0, [1, 1, 1]), (0.5, [1.05, 1.15, 0.8]), (2.5, [1.05, 1.15, 0.8]), (3.0, [1, 1, 1]))},
           "root": {"position": at.kf((0, [0, 0, 0]), (0.5, [0, -2.4, 0]), (2.5, [0, -2.4, 0]), (3.0, [0, 0, 0]))}}
    for n in POTEN4:
        rol[n] = {"scale": at.kf((0, [1, 1, 1]), (0.4, [1, 0.1, 1]), (2.6, [1, 0.1, 1]), (3.0, [1, 1, 1]))}
    extra(anims, "oprollen", 3.0, rol, loop=False)
    return anims


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "kameleon":
        a = Atlas(1010); g = kameleon(a); anims = kameleon_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.82), a)
        for kleur in KAMELEON_KLEUREN:
            if kleur:                                    # zelfde seed + zelfde kubussen = zelfde uv, andere kleuren
                ak = Atlas(1010); kameleon(ak, kleur)
                ak.image().save(os.path.join(d, f"kameleon_{kleur}.png"))
    elif soort == "reuzenschildpad":
        a = Atlas(1020); g = reuzenschildpad(a); anims = reuzenschildpad_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.5, 0.75), a)
    elif soort == "slang":
        a = Atlas(1030); g = slang(a); anims = slang_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.58, 0.8), a)
    elif soort == "hoornkikker":
        a = Atlas(1040); g = hoornkikker(a); anims = hoornkikker_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.72), a)
    elif soort == "kiwi":
        a = Atlas(1050); g = kiwi(a); anims = kiwi_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.85), a)
    elif soort == "schubdier":
        a = Atlas(1060); g = schubdier(a); anims = schubdier_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.58, 0.82), a)
    else:
        raise SystemExit(f"onbekende soort {soort}")
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort, "" if not at.SLAAP else "(slaap)")


# =====================================================================================================================
# naar de mod-assets: dezelfde stappen als safari_dieren.py (kopieer + slaapoogjes + extra animaties), zonder soorten.json
# =====================================================================================================================
def naar_assets(soorten):
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    import safari_dieren as sd
    global UIT
    oud = UIT
    UIT = sd.STAGING
    for s in soorten:
        maak(s)
    at.SLAAP = True
    try:
        UIT = sd.STAGING_SLAAP
        for s in soorten:
            maak(s)
    finally:
        at.SLAAP = False
        UIT = sd.STAGING
    sd.kopieer(soorten)
    sd.extra_animaties(soorten)
    UIT = oud


def kopie_ontwerp(soorten):
    """Per soort de bestanden + voorstelvel naar safari-ontwerp/dieren/<soort>/ (het ontwerp-archief)."""
    ontwerp = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "..", "..",
                                           "safari-ontwerp", "dieren"))
    import safari_dieren as sd
    for s in soorten:
        doel = os.path.join(ontwerp, s)
        os.makedirs(doel, exist_ok=True)
        bron = os.path.join(UIT, s)
        for f in os.listdir(bron):
            shutil.copyfile(os.path.join(bron, f), os.path.join(doel, f))
        # de animatie mét slaap/rust uit de assets, en de slaapoogjes
        shutil.copyfile(os.path.join(sd.ANIM_DIR, f"{s}.animation.json"), os.path.join(doel, f"{s}.animation.json"))
        for f in os.listdir(sd.TEX_DIR):
            if f.startswith(s) and f.endswith("_slaap.png"):
                shutil.copyfile(os.path.join(sd.TEX_DIR, f), os.path.join(doel, f))
    o = os.path.join(UIT, "batch10_overzicht.png")
    if os.path.exists(o):
        shutil.copyfile(o, os.path.join(ontwerp, "batch10_overzicht.png"))
    return ontwerp


# =====================================================================================================================
# vellen (zoals batch 9)
# =====================================================================================================================
def vellen(soorten, overzicht=True):
    import blad as bl
    from PIL import ImageDraw
    bl.NAMEN.update(NAMEN)

    def vel(soort):
        varianten = [("", "volwassen"), ("baby", "jong")]
        guh = bl.guh_quads()
        maxh, maxw = bl.hoogte(guh)
        qs = {}
        for v, _ in varianten:
            qs[v] = bl.dier_quads(soort, v)
            h, w = bl.hoogte(qs[v])
            maxh, maxw = max(maxh, h), max(maxw, w)
        CW, CH, GW_ = 360, 380, 560
        ppb = min(170, (CH - 40) / maxh, (CW - 30) / (maxw * 1.05))
        W, H = 200 + CW * 3 + GW_, 90 + CH * len(varianten) + 40
        img = Image.new("RGBA", (W, H), bl.BG)
        d = ImageDraw.Draw(img)
        d.text((20, 18), f"{NAMEN[soort]}  (safari-dieren, batch 10: Kriebelhuis + rare vormen)", font=bl.font(30, True), fill=(40, 30, 40))
        d.text((20, 56), "voor / zij / schuin, en schuin naast een guh (zelfde schaal). Animaties volgen in de game.",
               font=bl.font(16), fill=(80, 70, 80))
        for c, (lab, _, _) in enumerate(bl.VIEWS + [("naast een guh", 0, 0)]):
            d.text((200 + c * CW + 10, 88), lab, font=bl.font(16, True), fill=(90, 80, 90))
        combs, ppb_g = {}, ppb * 0.8
        for v, _ in varianten:
            dier = bl.dier_quads(soort, v)
            combs[v] = bl.naast(dier, bl.guh_quads(), dx=-(bl.xbreed(dier) / 2 + bl.xbreed(guh) / 2 + 0.3))
            sx, sy = bl.span(combs[v], 35, -18)
            ppb_g = min(ppb_g, (GW_ - 30) / sx, (CH - 50) / sy)
        for r, (v, lab) in enumerate(varianten):
            y = 110 + r * CH
            d.text((20, y + CH / 2 - 12), lab, font=bl.font(20, True), fill=(50, 40, 50))
            for c, (_, yaw, pitch) in enumerate(bl.VIEWS):
                img.alpha_composite(bl.render_vast(qs[v], yaw, pitch, ppb, CW, CH - 20), (200 + c * CW, y))
            img.alpha_composite(bl.render_vast(combs[v], 35, -18, ppb_g, GW_, CH - 20), (200 + 3 * CW, y))
        pad = os.path.join(UIT, soort, f"{soort}_voorstel.png")
        img.convert("RGB").save(pad)
        print(pad)

    for s in soorten:
        vel(s)
    if not overzicht:
        return
    import wiki_renders as wr
    items = [("guh", bl.guh_quads(), None)] + [(s, bl.dier_quads(s), bl.dier_quads(s, "baby")) for s in SOORTEN]
    quads, x = [], 0.0
    for naam, q, qb in items:
        h, w = bl.hoogte(q)
        quads += wr.offset_quads(q, dx=(x + w / 2))
        if qb is not None:
            quads += wr.offset_quads(qb, dx=(x + w / 2), dz=-(w * 0.7 + 0.6))
        x += w + 0.7
    ppb = 150
    W, H = int(x * ppb * 0.95) + 260, 640
    t = bl.render_vast(quads, 18, -16, ppb, W, H - 100)
    img = Image.new("RGBA", (W, H), bl.BG)
    img.alpha_composite(t, (0, 90))
    d = ImageDraw.Draw(img)
    d.text((20, 18), "Safari-dieren batch 10 (Kriebelhuis + rare vormen): naast een guh (volwassen achter, jong ervoor)",
           font=bl.font(28, True), fill=(40, 30, 40))
    d.text((20, 56), "Kameleon · Reuzenschildpad · Slang · Hoornkikker · Kiwi · Schubdier (van links naar rechts, na de guh)",
           font=bl.font(17), fill=(80, 70, 80))
    pad = os.path.join(UIT, "batch10_overzicht.png")
    img.convert("RGB").save(pad)
    print(pad)


if __name__ == "__main__":
    args = sys.argv[1:]
    soorten = [a for a in args if a != "overzicht"] or SOORTEN
    naar_assets(soorten)
    vellen(soorten, overzicht=(not args or "overzicht" in args))
    print("ontwerp-kopie:", kopie_ontwerp(soorten))
