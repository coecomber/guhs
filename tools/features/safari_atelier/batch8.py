"""
Safari-voorwerk, batch 8 (extra dieren, ronde 38-39): struisvogel, ooievaar, specht, narwal, octopus
(zelfde stijl en formaat als batch 1-7).

    python safari-ontwerp/dieren/_bron/batch8.py [soort ...] [--geen-vellen]   (geen soort: alle vijf + vellen + overzicht)

Per soort in safari-ontwerp/dieren/<soort>/: <soort>.geo.json/.png (volwassen; bij de narwal het vrouwtje, zonder slagtand),
<soort>_man.* (alleen narwal: met de lange spiraaltand), <soort>_baby.*, <soort>.animation.json (idle, walk, blij, eet +
struisvogel 'ren' en 'zit', ooievaar 'vlieg', specht 'tik', narwal 'zwem', octopus 'zwaai'), <soort>.bbmodel en
<soort>_voorstel.png; plus batch8_overzicht.png.
Hergebruikt atelier/dieren/blad uit batch 1 (alleen geimporteerd, niet aangepast).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, met, gezicht, neusje, vorm_oor, transparent  # noqa: E402
from dieren import baby, paar, ZIJ_LEEG                                 # noqa: E402

UIT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "build", "safari_atelier"))  # staging; tools/features/safari_dieren.py copies into the assets
S = at.S
kf = at.kf
ALLES_LEEG = {f: transparent for f in ("north", "south", "east", "west", "up", "down")}


def groter(geo_, f, ident):
    """Hetzelfde model, alles f keer zo groot."""
    g = json.loads(json.dumps(geo_))
    gg = g["minecraft:geometry"][0]
    for b in gg["bones"]:
        b["pivot"] = [round(v * f, 3) for v in b["pivot"]]
        for c in b.get("cubes", []):
            c["origin"] = [round(v * f, 3) for v in c["origin"]]
            c["size"] = [round(v * f, 3) for v in c["size"]]
    d = gg["description"]
    d["identifier"] = f"geometry.{ident}"
    return g


def stippen(base, stip, dichtheid=500, rmin=1.2, rmax=2.6, alpha=0.9):
    def f(W, H, rng, a):
        for _ in range(max(1, W * H // dichtheid)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(rmin, rmax) * S / 4, stip, alpha)
    return plain(base, 8, f)


def spiraal(base, lijn, n=7):
    """Spiraaltand: schuine lijnen langs de lengte."""
    def paint(W, H, rng):
        a = plain(base, 6)(W, H, rng).astype(np.float32)
        yy, xx = np.mgrid[0:H, 0:W] + 0.5
        t = (xx / max(W, 1) + yy / max(H, 1) * n) % 1.0
        a[t < 0.25, :3] = lijn
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def poot_anims(L=0.8, s=28, links="leg_left", rechts="leg_right"):
    return {links: {"rotation": kf((0, [s, 0, 0]), (L / 2, [-s, 0, 0]), (L, [s, 0, 0]))},
            rechts: {"rotation": kf((0, [-s, 0, 0]), (L / 2, [s, 0, 0]), (L, [-s, 0, 0]))}}


def anim(an, naam, lengte, bones, loop=True):
    an["animations"][naam] = {"loop": loop, "animation_length": lengte, "bones": bones}
    return an


# =====================================================================================================================
# struisvogel: groot rond pluizig lijf, lange hals, lange poten
# =====================================================================================================================
SV_ZW = (46, 40, 46)
SV_WIT = (250, 248, 244)
SV_HUID = (226, 178, 170)          # roze-grijze hals en dijen
SV_POOT = (196, 168, 160)
SV_KOP = (176, 162, 160)
SV_SNAVEL = (236, 200, 150)
SV_KUIKEN = (176, 150, 120)
SV_KUIKEN_D = (120, 96, 74)


def struisvogel(atlas, kuiken=False):
    if kuiken:
        def streepjes(W, H, rng, a):
            for i in range(3):
                x = W * (0.25 + i * 0.25)
                a[:, max(0, int(x - W * 0.04)):int(x + W * 0.04), :3] = SV_KUIKEN_D
        veer = at.fluffy(SV_KUIKEN, 18)
        rug = plain(SV_KUIKEN, 14, streepjes)
        licht = at.fluffy((214, 196, 170), 14)
        kopkl = at.fluffy(SV_KUIKEN, 14)
        halskl = at.fluffy(SV_KUIKEN, 12)
    else:
        veer = at.fluffy(SV_ZW, 14)
        rug = veer
        licht = at.fluffy(SV_ZW, 10)
        kopkl = plain(SV_KOP, 8)
        halskl = plain(SV_HUID, 8)
    dij = plain(SV_HUID, 8)
    poot = plain(SV_POOT, 6)
    bones = [bone("root", None, [0, 0, 0], [])]
    for side, sx in (("left", 1), ("right", -1)):
        x = 1.6 if sx > 0 else -4.6
        bones.append(bone(f"leg_{side}", "root", [sx * 3.1, 17, 0], [
            cube(atlas, [x, 10, -1.5], [3, 7.5, 3], dij),                       # dikke dij
            cube(atlas, [x + 0.8, 0.6, -0.7], [1.4, 9.6, 1.4], poot),           # lange dunne loop
            cube(atlas, [x - 0.2, 0, -3.2], [3.4, 0.8, 4], poot)]))             # twee grote tenen
    bones.append(bone("body", "root", [0, 20, 0], [
        cube(atlas, [-7, 15.5, -7], [14, 11, 15], veer, {"up": rug, "down": licht}),
        cube(atlas, [-5.5, 17, 8], [11, 8, 3], at.fluffy(SV_WIT if not kuiken else SV_KUIKEN, 16))]))   # staartpluim
    vleugel = at.fluffy(SV_WIT if not kuiken else SV_KUIKEN, 14)
    bones += paar(atlas, "wing", "body", [7, 24, 0], [7, 17, -5], [1.5, 7, 11], veer,
                  {"south": vleugel},
                  rotation=[0, 0, -8])
    bones.append(bone("hals", "body", [0, 25, -5.5], [cube(atlas, [-1.5, 25, -7], [3, 12, 3], halskl)]))
    bones.append(bone("head", "hals", [0, 37, -6], [
        cube(atlas, [-3.5, 36, -9.5], [7, 6, 6.5], kopkl, {"north": gezicht(SV_KOP if not kuiken else SV_KUIKEN, eye_y=0.4, eye_r=0.19)}),
        cube(atlas, [-2, 36.6, -12], [4, 1.5, 2.6], plain(SV_SNAVEL, 5))]))
    return geo("struisvogel", bones, atlas, 1.4, 2.8)


def struisvogel_anims():
    an = at.animaties(legs2=["leg_left", "leg_right"],
                      extra_idle={"hals": {"rotation": kf((0, [0, 0, 0]), (1.5, [8, 0, 0]), (3.0, [0, 0, 0]))}},
                      extra_walk={"hals": {"rotation": kf((0, [6, 0, 0]), (0.4, [-4, 0, 0]), (0.8, [6, 0, 0]))}})
    L = 0.36
    ren = {"body": {"position": kf((0, [0, 0, 0]), (L / 4, [0, 1.2, 0]), (L / 2, [0, 0, 0]), (3 * L / 4, [0, 1.2, 0]), (L, [0, 0, 0])),
                    "rotation": kf((0, [8, 0, 0]), (L, [8, 0, 0]))},
           "hals": {"rotation": kf((0, [-22, 0, 0]), (L / 2, [-18, 0, 0]), (L, [-22, 0, 0]))},
           "wing_left": {"rotation": kf((0, [0, 0, -25]), (L / 2, [0, 0, -40]), (L, [0, 0, -25]))},
           "wing_right": {"rotation": kf((0, [0, 0, 25]), (L / 2, [0, 0, 40]), (L, [0, 0, 25]))}}
    ren.update(poot_anims(L, 45))
    anim(an, "ren", L, ren)
    anim(an, "zit", 3.0, {
        "body": {"position": kf((0, [0, -9, 0]), (1.5, [0, -8.6, 0]), (3.0, [0, -9, 0]))},
        "leg_left": {"rotation": kf((0, [-80, 0, 0]), (3.0, [-80, 0, 0]))},
        "leg_right": {"rotation": kf((0, [-80, 0, 0]), (3.0, [-80, 0, 0]))},
        "hals": {"rotation": kf((0, [0, 0, 0]), (1.5, [10, 0, 0]), (3.0, [0, 0, 0]))}})
    return an


# =====================================================================================================================
# ooievaar: wit, zwarte slagpennen, rode snavel en poten, bundeltje in de snavel
# =====================================================================================================================
OV_WIT = (250, 250, 248)
OV_ZW = (44, 40, 48)
OV_ROOD = (226, 72, 58)
OV_JONG = (226, 226, 230)
OV_JONG_SN = (70, 62, 66)
DOEK = (150, 196, 236)
DOEK_L = (236, 244, 252)


def ooievaar(atlas, jong=False):
    wit = at.fluffy(OV_JONG, 14) if jong else plain(OV_WIT, 6)
    pootkl = plain((214, 150, 140) if jong else OV_ROOD, 6)
    snavel = plain(OV_JONG_SN if jong else OV_ROOD, 5)
    bones = [bone("root", None, [0, 0, 0], [])]
    for side, sx in (("left", 1), ("right", -1)):
        x = 0.8 if sx > 0 else -2
        bones.append(bone(f"leg_{side}", "root", [sx * 1.4, 13, 0], [
            cube(atlas, [x, 0.5, -0.6], [1.2, 13, 1.2], pootkl),
            cube(atlas, [x - 1, 0, -2.6], [3.2, 0.6, 3.4], pootkl)]))

    def slagpen(side):
        def f(W, H, rng, a):
            n = int(W * 0.42)
            if side == "left":                      # east-vlak: rechts = voor, dus zwart links (achter)
                a[:, :n, :3] = OV_ZW
            else:
                a[:, W - n:, :3] = OV_ZW
            a[int(H * 0.75):, :, :3] = OV_ZW        # onderrand zwart
        kant = "east" if side == "left" else "west"
        return {kant: plain(OV_WIT if not jong else OV_JONG, 6, f), "south": plain(OV_ZW, 6), "down": plain(OV_ZW, 6)}
    bones.append(bone("body", "root", [0, 16, 0], [cube(atlas, [-4, 12.5, -5.5], [8, 7.5, 12], wit),
                                                   cube(atlas, [-2.5, 13.5, 6.5], [5, 3, 3], plain(OV_WIT if not jong else OV_JONG, 6))]))
    bones += paar(atlas, "wing", "body", [4, 19.5, 0], [4, 13, -4], [1.2, 6.5, 11], wit, slagpen)
    bones.append(bone("hals", "body", [0, 18, -5], [cube(atlas, [-1.5, 18, -6.8], [3, 6.5, 3], wit)]))
    bones.append(bone("head", "hals", [0, 25, -5.5], [
        cube(atlas, [-3, 23.5, -9], [6, 6, 6], wit, {"north": gezicht(OV_JONG if jong else OV_WIT, eye_y=0.42, eye_r=0.19)}),
        cube(atlas, [-0.9, 24.2, -15], [1.8, 1.8, 6], snavel)]))
    if not jong:
        bones.append(bone("bundeltje", "head", [0, 24.5, -14], [
            cube(atlas, [-0.2, 21.6, -14.4], [0.4, 3, 0.4], plain((240, 236, 226), 3)),
            cube(atlas, [-2, 18.4, -16], [4, 3.6, 3.6], plain(DOEK, 6, lambda W, H, rng, a: [disc(a, x, y, W * 0.08, DOEK_L, 1.0)
                                                                                             for x in (W * 0.25, W * 0.75) for y in (H * 0.3, H * 0.75)])),
            cube(atlas, [-0.8, 21.6, -15], [1.6, 1, 1.6], plain(DOEK, 4))]))
    return geo("ooievaar", bones, atlas, 1.3, 2.0)


def ooievaar_anims():
    an = at.animaties(legs2=["leg_left", "leg_right"],
                      extra_idle={"hals": {"rotation": kf((0, [0, 0, 0]), (1.5, [5, 0, 0]), (3.0, [0, 0, 0]))}})
    L = 0.6
    anim(an, "vlieg", L, {
        "body": {"position": kf((0, [0, 6, 0]), (L / 2, [0, 6.6, 0]), (L, [0, 6, 0])),
                 "rotation": kf((0, [-8, 0, 0]), (L, [-8, 0, 0]))},
        "wing_left": {"rotation": kf((0, [0, 0, -70]), (L / 2, [0, 0, 20]), (L, [0, 0, -70]))},
        "wing_right": {"rotation": kf((0, [0, 0, 70]), (L / 2, [0, 0, -20]), (L, [0, 0, 70]))},
        "leg_left": {"rotation": kf((0, [70, 0, 0]), (L, [70, 0, 0]))},
        "leg_right": {"rotation": kf((0, [70, 0, 0]), (L, [70, 0, 0]))}})
    return an


# =====================================================================================================================
# specht: rechtop zittend, zwart-wit met rood petje, stijve steunstaart, 'tik'
# =====================================================================================================================
SP_ZW = (40, 36, 44)
SP_WIT = (250, 248, 244)
SP_ROOD = (220, 48, 52)
SP_BUIK = (242, 232, 218)
SP_SNAVEL = (110, 106, 116)


def specht(atlas):
    def balkjes(W, H, rng, a):
        for i in range(3):
            y = H * (0.3 + i * 0.22)
            a[int(y):int(y + H * 0.07), int(W * 0.2):int(W * 0.8), :3] = SP_WIT
    rug = plain(SP_ZW, 8, balkjes)
    buik = at.verloop(SP_BUIK, SP_BUIK)

    def onderstaart(W, H, rng, a):
        a[int(H * 0.72):, :, :3] = SP_ROOD
    bones = [bone("root", None, [0, 0, 0], [])]
    for side, sx in (("left", 1), ("right", -1)):
        x = 0.6 if sx > 0 else -2.2
        bones.append(bone(f"leg_{side}", "root", [sx * 1.4, 2, -0.5], [cube(atlas, [x, 0, -1.6], [1.6, 2, 2.4], plain(SP_SNAVEL, 5))]))
    bones.append(bone("body", "root", [0, 2, 0], [
        cube(atlas, [-3, 2, -2.5], [6, 9, 5], plain(SP_ZW, 8), {"north": plain(SP_BUIK, 8, onderstaart), "south": rug}),
        cube(atlas, [-2.5, 7, -2.9], [5, 3.5, 0.6], plain(SP_WIT, 5), {"east": plain(SP_WIT, 4), "west": plain(SP_WIT, 4)})]))
    bones.append(bone("tail", "body", [0, 3, 2.5], [cube(atlas, [-2, -0.5, 2.3], [4, 5, 1.2], plain(SP_ZW, 6))], [-15, 0, 0]))
    vl = plain(SP_ZW, 8, lambda W, H, rng, a: [disc(a, rng.uniform(0, W), rng.uniform(0, H), W * 0.12, SP_WIT, 1.0) for _ in range(5)])
    bones += paar(atlas, "wing", "body", [3, 10, 0], [3, 3.5, -2], [1, 6.5, 4.5], vl)

    def kopzij(W, H, rng, a):
        a[int(H * 0.45):, :, :3] = SP_WIT                       # witte wang
        a[int(H * 0.38):int(H * 0.48), :, :3] = SP_ZW           # zwarte baardstreep erboven
    bones.append(bone("head", "body", [0, 11, 0], [
        cube(atlas, [-3, 11, -3], [6, 5.5, 5.5], plain(SP_ZW, 8),
             {"north": gezicht(SP_WIT, eye_y=0.42, eye_r=0.18), "east": plain(SP_ZW, 6, kopzij), "west": plain(SP_ZW, 6, kopzij),
              "up": plain(SP_ROOD, 6)}),
        cube(atlas, [-2.5, 16.5, -2.5], [5, 1, 4.5], plain(SP_ROOD, 6)),          # rood petje
        cube(atlas, [-0.75, 13, -5.6], [1.5, 1.3, 2.7], plain(SP_SNAVEL, 4))]))
    return geo("specht", bones, atlas, 0.8, 1.2)


def specht_anims():
    an = at.animaties(legs2=["leg_left", "leg_right"],
                      extra_idle={"tail": {"rotation": kf((0, [-15, 0, 0]), (1.5, [-12, 0, 0]), (3.0, [-15, 0, 0]))}})
    tik = {"head": {"rotation": kf((0, [0, 0, 0]),
                                   (0.06, [28, 0, 0]), (0.12, [0, 0, 0]), (0.18, [28, 0, 0]), (0.24, [0, 0, 0]),
                                   (0.30, [28, 0, 0]), (0.36, [0, 0, 0]), (0.42, [28, 0, 0]), (0.48, [0, 0, 0]),
                                   (1.4, [0, 0, 0]))},
           "body": {"rotation": kf((0, [0, 0, 0]), (0.1, [6, 0, 0]), (0.45, [6, 0, 0]), (0.6, [0, 0, 0]), (1.4, [0, 0, 0]))}}
    anim(an, "tik", 1.4, tik)
    return an


# =====================================================================================================================
# narwal: vadsige gevlekte walvisvorm, mannetje met lange spiraaltand
# =====================================================================================================================
NW = (150, 156, 168)
NW_D = (96, 102, 116)
NW_L = (220, 224, 230)
NW_KALF = (126, 132, 148)
TAND = (246, 238, 214)
TAND_L = (196, 182, 150)


def staartvin(top, rand):
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.uint8)
        yy, xx = np.mgrid[0:H, 0:W] + 0.5
        cx = W / 2
        dx = np.abs(xx - cx) / (W / 2)
        # twee lobben: breed naar buiten, naar achteren (onderkant) uitgesneden in het midden
        rand_y = H * (0.35 + 0.55 * dx)
        m = (yy < rand_y) & (dx < 1.0)
        a[m] = list(top) + [255]
        e = m & (yy > rand_y - 1.5 * S / 4)
        a[e] = list(rand) + [255]
        return a
    return paint


def narwal(atlas, man=False, kalf=False):
    basis = NW_KALF if kalf else NW
    huid = stippen(basis, NW_D, 260, 1.0, 2.2, 0.75)
    buik = plain(NW_L, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [cube(atlas, [-6, 1, -9], [12, 10, 19], huid, {"down": buik})])]
    bones.append(bone("head", "body", [0, 6, -9], [
        cube(atlas, [-5.5, 1.5, -16], [11, 9, 7.5], huid, {"north": gezicht(basis, eye_y=0.42, eye_r=0.16), "down": buik}),
        cube(atlas, [-4.5, 9.8, -14.5], [9, 1.6, 6], huid)]))                          # bolle 'meloen' bovenop
    if man:
        bones.append(bone("tusk", "head", [1.8, 3.6, -16], [cube(atlas, [1.2, 3, -32], [1.2, 1.2, 16], spiraal(TAND, TAND_L, 9))]))
    bones += paar(atlas, "flipper", "body", [6, 3.5, -5], [6, 3, -7], [3.5, 1, 3.5], plain(NW_D, 6), rotation=[0, 0, 28])
    bones.append(bone("tail", "body", [0, 6, 10], [
        cube(atlas, [-3, 3.5, 10], [6, 4.5, 6], huid),
        cube(atlas, [-7, 5, 15], [14, 1.2, 7], plain(NW_D, 6),
             {"up": staartvin(NW_D, NW_D), "down": staartvin(NW_L, NW_D),
              "north": transparent, "south": transparent, "east": transparent, "west": transparent})]))
    return geo("narwal_man" if man else "narwal", bones, atlas, 1.6, 1.0)


def narwal_anims():
    an = at.animaties(extra_idle={"tail": {"rotation": kf((0, [0, 0, 0]), (1.5, [8, 0, 0]), (3.0, [0, 0, 0]))},
                                  "flipper_left": {"rotation": kf((0, [0, 0, 0]), (1.5, [0, 0, -10]), (3.0, [0, 0, 0]))},
                                  "flipper_right": {"rotation": kf((0, [0, 0, 0]), (1.5, [0, 0, 10]), (3.0, [0, 0, 0]))}},
                      extra_walk={"tail": {"rotation": kf((0, [14, 0, 0]), (0.4, [-14, 0, 0]), (0.8, [14, 0, 0]))}})
    L = 1.2
    anim(an, "zwem", L, {
        "body": {"rotation": kf((0, [-4, 0, 0]), (L / 2, [4, 0, 0]), (L, [-4, 0, 0])),
                 "position": kf((0, [0, 0, 0]), (L / 2, [0, 1, 0]), (L, [0, 0, 0]))},
        "tail": {"rotation": kf((0, [18, 0, 0]), (L / 2, [-18, 0, 0]), (L, [18, 0, 0]))},
        "flipper_left": {"rotation": kf((0, [0, 0, -15]), (L / 2, [0, 0, 10]), (L, [0, 0, -15]))},
        "flipper_right": {"rotation": kf((0, [0, 0, 15]), (L / 2, [0, 0, -10]), (L, [0, 0, 15]))}})
    return an


# =====================================================================================================================
# octopus: ronde kop met acht krullende armen
# =====================================================================================================================
OC = (238, 128, 110)
OC_D = (206, 92, 84)
OC_L = (255, 200, 180)
ARMEN = 8


def octopus(atlas):
    vel = plain(OC, 8, lambda W, H, rng, a: [disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.6, 1.3) * S / 2, OC_D, 0.6)
                                              for _ in range(max(1, W * H // 700))])

    def zuignapjes(W, H, rng, a):
        for i in range(max(2, int(W / (S * 1.2)))):
            disc(a, (i + 0.5) * W / max(2, int(W / (S * 1.2))), H / 2, min(W, H) * 0.22, (255, 226, 214), 1.0)
    onder = plain(OC_L, 6, zuignapjes)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4, 0], [cube(atlas, [-4.5, 2.5, -4.5], [9, 3, 9], vel, {"down": plain(OC_L, 6)})])]
    bones.append(bone("head", "body", [0, 5.5, 0], [
        cube(atlas, [-5.5, 5.5, -5.5], [11, 9, 11], vel, {"north": gezicht(OC, eye_y=0.5, eye_r=0.18)}),
        cube(atlas, [-4.5, 14.5, -4.5], [9, 2, 9], vel)]))
    for i in range(ARMEN):
        hoek = 360.0 / ARMEN * i + 22.5                          # rond, geen arm recht naar voren (gezicht vrij)
        bones.append(bone(f"arm_{i}", "body", [0, 3.2, 0], [
            cube(atlas, [-1.1, 2.2, -11], [2.2, 1.8, 7], vel, {"down": onder})], [0, hoek, 0]))
        bones.append(bone(f"arm_{i}_tip", f"arm_{i}", [0, 3.2, -11], [
            cube(atlas, [-0.8, 3.0, -15.5], [1.6, 1.4, 4.8], vel, {"down": onder})], [-35, 0, 0]))
    return geo("octopus", bones, atlas, 1.6, 1.1)


def octopus_anims():
    golf = {}
    for i in range(ARMEN):
        fase = (i % 2) * 1.5
        golf[f"arm_{i}"] = {"rotation": kf((0, [4 if fase else -4, 0, 0]), (1.5, [-4 if fase else 4, 0, 0]), (3.0, [4 if fase else -4, 0, 0]))}
        golf[f"arm_{i}_tip"] = {"rotation": kf((0, [0, 0, 0]), (1.5, [-12 if fase else 12, 0, 0]), (3.0, [0, 0, 0]))}
    an = at.animaties(extra_idle=golf)
    L = 0.8
    loop = {}
    for i in range(ARMEN):
        s = 10 if i % 2 else -10
        loop[f"arm_{i}"] = {"rotation": kf((0, [s, 0, 0]), (L / 2, [-s, 0, 0]), (L, [s, 0, 0]))}
    an["animations"]["walk"]["bones"].update(loop)
    zw = {"head": {"rotation": kf((0, [0, 0, 0]), (0.4, [0, 0, 8]), (0.8, [0, 0, -8]), (1.2, [0, 0, 0]))}}
    for i in range(ARMEN):
        s = 25 if i % 2 else -25
        zw[f"arm_{i}"] = {"rotation": kf((0, [0, 0, 0]), (0.3, [s, 0, 0]), (0.6, [-s, 0, 0]), (0.9, [s, 0, 0]), (1.2, [0, 0, 0]))}
        zw[f"arm_{i}_tip"] = {"rotation": kf((0, [0, 0, 0]), (0.3, [-30, 0, 0]), (0.6, [20, 0, 0]), (0.9, [-30, 0, 0]), (1.2, [0, 0, 0]))}
    zw["arm_0"] = {"rotation": kf((0, [0, 0, 0]), (0.2, [-60, 0, 0]), (0.4, [-40, 0, 0]), (0.6, [-60, 0, 0]), (0.8, [-40, 0, 0]),
                                  (1.0, [-60, 0, 0]), (1.2, [0, 0, 0]))}     # eentje zwaait echt
    anim(an, "zwaai", 1.2, zw, loop=False)
    return an


# =====================================================================================================================
# alles wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "struisvogel":
        a = Atlas(810); g = struisvogel(a); anims = struisvogel_anims()
        img = at.schrijf(d, soort, g, a, anims)
        ak = Atlas(811); at.schrijf(d, soort + "_baby", baby(struisvogel(ak, kuiken=True), 0.5, 0.75), ak)
    elif soort == "ooievaar":
        a = Atlas(820); g = ooievaar(a); anims = ooievaar_anims()
        img = at.schrijf(d, soort, g, a, anims)
        ak = Atlas(821); at.schrijf(d, soort + "_baby", baby(ooievaar(ak, jong=True), 0.6, 0.8), ak)
    elif soort == "specht":
        a = Atlas(830); g = specht(a); anims = specht_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.65, 0.85), a)
    elif soort == "narwal":
        a = Atlas(840); g = narwal(a); anims = narwal_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(840); at.schrijf(d, soort + "_man", narwal(am, man=True), am)
        ak = Atlas(841); at.schrijf(d, soort + "_baby", baby(narwal(ak, kalf=True), 0.55, 0.75), ak)
    elif soort == "octopus":
        a = Atlas(850); g = octopus(a); anims = octopus_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8), a)
    else:
        raise SystemExit(f"onbekende soort {soort}")
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort)


SOORTEN = ["struisvogel", "ooievaar", "specht", "narwal", "octopus"]
NAMEN = {"struisvogel": "Struisvogel", "ooievaar": "Ooievaar", "specht": "Specht", "narwal": "Narwal", "octopus": "Octopus"}


# =====================================================================================================================
# voorstelvellen (zelfde opzet als blad.py van batch 1; blad wordt alleen geimporteerd)
# =====================================================================================================================
def vellen(soorten, overzicht=True):
    cwd = os.getcwd()
    import blad as bl                                                  # (doet os.chdir naar guhs/)
    bl.NAMEN.update(NAMEN)

    def vel(soort):
        varianten = [("", "volwassen" + (" (vrouwtje)" if os.path.exists(os.path.join(UIT, soort, f"{soort}_man.geo.json")) else ""))]
        if os.path.exists(os.path.join(UIT, soort, f"{soort}_man.geo.json")):
            varianten.append(("man", "mannetje"))
        varianten.append(("baby", "jong"))
        guh = bl.guh_quads()
        gh, gw = bl.hoogte(guh)
        maxh, maxw = gh, gw
        qs = {}
        for v, _ in varianten:
            qs[v] = bl.dier_quads(soort, v)
            h, w = bl.hoogte(qs[v])
            maxh, maxw = max(maxh, h), max(maxw, w)
        CW, CH = 360, 380
        ppb = min(170, (CH - 40) / maxh, (CW - 30) / (maxw * 1.05))
        GW = 560
        W, H = 200 + CW * 3 + GW, 90 + CH * len(varianten) + 40
        img = Image.new("RGBA", (W, H), bl.BG)
        d = ImageDraw.Draw(img)
        d.text((20, 18), f"{NAMEN[soort]}  (safari-dieren, batch 8)", font=bl.font(30, True), fill=(40, 30, 40))
        d.text((20, 56), "voor / zij / schuin, en schuin naast een guh (zelfde schaal). Animaties volgen in de game.",
               font=bl.font(16), fill=(80, 70, 80))
        for c, (lab, _, _) in enumerate(bl.VIEWS + [("naast een guh", 0, 0)]):
            d.text((200 + c * CW + 10, 88), lab, font=bl.font(16, True), fill=(90, 80, 90))
        combs = {}
        ppb_g = ppb * 0.8
        for v, _ in varianten:
            dier = bl.dier_quads(soort, v)
            combs[v] = bl.naast(dier, bl.guh_quads(), dx=-(bl.xbreed(dier) / 2 + bl.xbreed(guh) / 2 + 0.3))
            sx, sy = bl.span(combs[v], 35, -18)
            ppb_g = min(ppb_g, (GW - 30) / sx, (CH - 50) / sy)
        for r, (v, lab) in enumerate(varianten):
            y = 110 + r * CH
            d.text((20, y + CH / 2 - 12), lab, font=bl.font(20, True), fill=(50, 40, 50))
            for c, (_, yaw, pitch) in enumerate(bl.VIEWS):
                img.alpha_composite(bl.render_vast(qs[v], yaw, pitch, ppb, CW, CH - 20), (200 + c * CW, y))
            img.alpha_composite(bl.render_vast(combs[v], 35, -18, ppb_g, GW, CH - 20), (200 + 3 * CW, y))
        pad = os.path.join(UIT, soort, f"{soort}_voorstel.png")
        img.convert("RGB").save(pad)
        print(pad)

    def overzichtvel():
        guh = bl.guh_quads()
        x = 0.0
        items = [("guh", guh, None)] + [(s, bl.dier_quads(s), bl.dier_quads(s, "baby")) for s in SOORTEN]
        quads = []
        for naam, q, qb in items:
            h, w = bl.hoogte(q)
            quads += bl.wr.offset_quads(q, dx=(x + w / 2))
            if qb is not None:
                quads += bl.wr.offset_quads(qb, dx=(x + w / 2), dz=-(w * 0.7 + 0.6))
            x += w + 0.7
        ppb = 95
        W, H = int(x * ppb * 0.95) + 200, 700
        t = bl.render_vast(quads, 18, -16, ppb, W, H - 100)
        img = Image.new("RGBA", (W, H), bl.BG)
        img.alpha_composite(t, (0, 90))
        d = ImageDraw.Draw(img)
        d.text((20, 18), "Safari-dieren batch 8: alle vijf naast een guh (volwassen achter, jong ervoor)", font=bl.font(28, True), fill=(40, 30, 40))
        d.text((20, 56), "Struisvogel · Ooievaar · Specht · Narwal · Octopus (van links naar rechts, na de guh)",
               font=bl.font(17), fill=(80, 70, 80))
        pad = os.path.join(UIT, "batch8_overzicht.png")
        img.convert("RGB").save(pad)
        print(pad)

    for s in soorten:
        vel(s)
    if overzicht:
        overzichtvel()
    os.chdir(cwd)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    soorten = args or SOORTEN
    for s in soorten:
        maak(s)
    if "--geen-vellen" not in sys.argv:
        vellen(soorten, overzicht=not args)
