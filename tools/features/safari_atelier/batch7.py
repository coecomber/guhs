"""
Safari-voorwerk, batch 7: Australie (koala, kangoeroe, wombat, quokka) + de aaihoek (geitje, alpaca, cavia, ezeltje).
Zelfde stijl en formaat als batch 1 (dieren.py); hergebruikt atelier/dieren/blad alleen via import.

    python safari-ontwerp/dieren/_bron/batch7.py [soort ...] [--geen-vellen]

Schrijft per soort in safari-ontwerp/dieren/<soort>/ de geo/png/animation/bbmodel (+ _baby, sinds ronde 34 ook
bij de aaihoekdieren) en een <soort>_voorstel.png, plus dieren/batch7_overzicht.png.
Geen man/vrouw-verschil in deze batch. De kangoeroe-volwassene heeft een bot "buidel_jong" (een joey-kopje in de
buidel, aan/uit te zetten); het losse jong is de joey zelf.
Extra animaties: kangoeroe hop (= walk), geitje knabbel, alpaca spuug, cavia piep, ezeltje balk.
"""
import json
import os
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, gezicht, neusje, vorm_oor, transparent  # noqa: E402
import dieren as dn                                                     # noqa: E402  (alleen hulpjes: baby, paar, vier_poten)

UIT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "build", "safari_atelier"))  # staging; tools/features/safari_dieren.py copies into the assets
POTEN4, ZIJ_LEEG, paar, vier_poten, baby = dn.POTEN4, dn.ZIJ_LEEG, dn.paar, dn.vier_poten, dn.baby
NEUS = (40, 28, 34)


def vlekken(base, kleuren, n=7, rmin=0.18, rmax=0.36, var=8):
    """Grote ronde vlekken (cavia-lapjes)."""
    def f(W, H, rng, a):
        for i in range(n):
            c = kleuren[i % len(kleuren)]
            disc(a, rng.uniform(0, W), rng.uniform(0, H), W * rng.uniform(rmin, rmax), c, 1.0, ry=H * rng.uniform(rmin, rmax))
    return plain(base, var, f)


# =====================================================================================================================
# koala
# =====================================================================================================================
KO = (150, 152, 164)
KO_L = (228, 226, 232)
KO_D = (112, 114, 128)
KO_NEUS = (52, 46, 58)


def koala(atlas):
    grijs = at.fluffy(KO, 12)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [cube(atlas, [-5.5, 3, -5], [11, 10, 10], grijs,
                                                   {"north": at.buik(KO, KO_L, cy=0.6), "down": plain(KO_L, 8)})])]
    bones += vier_poten(atlas, "body", 1.5, -4.5, 2.5, [3, 3.5, 3], plain(KO_D, 8), 3.5)
    bones.append(bone("head", "body", [0, 12, -3], [
        cube(atlas, [-6.5, 11, -10], [13, 11, 9], grijs, {"north": gezicht(KO, eye_y=0.4, eye_dx=0.28, eye_r=0.15)}),
        cube(atlas, [-1.6, 12.2, -11.4], [3.2, 4.2, 1.4], plain(KO_NEUS, 6),
             {"north": plain(KO_NEUS, 6, lambda W, H, rng, a: disc(a, W * 0.32, H * 0.25, W * 0.12, (120, 112, 128), 1.0))})]))
    oor = vorm_oor(KO, KO_L, punt=0.0, rand=(244, 242, 246))
    bones += paar(atlas, "ear", "head", [6.5, 20, -6], [5.5, 17, -7], [5.5, 5.5, 1.5], grijs, dict(ZIJ_LEEG, north=oor, south=oor))
    return geo("koala", bones, atlas, 1.4, 1.5)


def koala_anims():
    return at.animaties(legs4=POTEN4, oren=True)


# =====================================================================================================================
# kangoeroe (+ joey in de buidel)
# =====================================================================================================================
KA = (198, 132, 84)
KA_L = (242, 214, 178)
KA_D = (150, 96, 60)


def kangoeroe(atlas):
    vacht = plain(KA, 9)

    def buidel(W, H, rng, a):
        disc(a, W * 0.5, H * 0.66, W * 0.36, KA_L, 1.0, ry=H * 0.3)
        y = int(H * 0.5)
        a[y:y + 2, int(W * 0.2):int(W * 0.8), :3] = KA_D                 # de rand van de buidel
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 8, 0], [cube(atlas, [-4.5, 6, -4], [9, 13, 8], vacht,
                                                   {"north": plain(KA, 9, buidel), "down": plain(KA_L, 8)})])]
    for side, sx in (("left", 1), ("right", -1)):
        dij_o = [1.2, 2, -3] if sx > 0 else [-4.7, 2, -3]
        voet_o = [1.2, 0, -8] if sx > 0 else [-4.7, 0, -8]
        bones.append(bone(f"leg_{side}", "body", [sx * 3, 8, 0], [cube(atlas, dij_o, [3.5, 7, 6], vacht),
                                                                  cube(atlas, voet_o, [3.5, 2, 9], plain(KA_D, 8))]))
    bones += paar(atlas, "arm", "body", [3.5, 16, -3.5], [2.6, 12, -5.5], [1.6, 4.5, 1.6], plain(KA_D, 8), rotation=[-20, 0, 0])
    bones.append(bone("head", "body", [0, 19, -1], [
        cube(atlas, [-4, 19, -5.5], [8, 7.5, 8], vacht, {"north": gezicht(KA, eye_y=0.42, eye_r=0.17)}),
        cube(atlas, [-2.2, 19.4, -8], [4.4, 3.6, 2.5], plain(KA_L, 6), {"north": neusje(KA_L, NEUS)})]))
    oor = vorm_oor(KA, (238, 160, 160), punt=0.65)
    bones += paar(atlas, "ear", "head", [2.6, 26, -2], [1.4, 26, -2.5], [2.8, 5.5, 1], vacht, dict(ZIJ_LEEG, north=oor, south=oor),
                  rotation=[0, 0, -12])
    bones.append(bone("tail", "body", [0, 7, 3.5], [cube(atlas, [-1.6, 3, 3.5], [3.2, 3.2, 13], vacht)], rotation=[-6, 0, 0]))
    bones.append(bone("buidel_jong", "body", [0, 11.5, -4], [
        cube(atlas, [-1.8, 11, -6], [3.6, 3.2, 2.8], plain(KA, 8), {"north": gezicht(KA, eye_y=0.42, eye_r=0.2)}),
        cube(atlas, [-1.8, 14.2, -4.8], [1.2, 1.8, 0.6], plain(KA, 6)), cube(atlas, [0.6, 14.2, -4.8], [1.2, 1.8, 0.6], plain(KA, 6))]))
    return geo("kangoeroe", bones, atlas, 1.3, 2.0)


def kangoeroe_anims():
    L = 0.6
    hop = {"body": {"position": at.kf((0, [0, 0, 0]), (L * 0.3, [0, 4, 0]), (L * 0.6, [0, 0, 0]), (L, [0, 0, 0])),
                    "rotation": at.kf((0, [0, 0, 0]), (L * 0.3, [10, 0, 0]), (L * 0.6, [0, 0, 0]), (L, [0, 0, 0]))},
           "leg_left": {"rotation": at.kf((0, [0, 0, 0]), (L * 0.3, [35, 0, 0]), (L * 0.6, [0, 0, 0]), (L, [0, 0, 0]))},
           "leg_right": {"rotation": at.kf((0, [0, 0, 0]), (L * 0.3, [35, 0, 0]), (L * 0.6, [0, 0, 0]), (L, [0, 0, 0]))},
           "tail": {"rotation": at.kf((0, [0, 0, 0]), (L * 0.3, [-15, 0, 0]), (L * 0.6, [0, 0, 0]), (L, [0, 0, 0]))}}
    piep = {"buidel_jong": {"position": at.kf((0, [0, -2, 0]), (1.2, [0, -2, 0]), (1.5, [0, 0, 0]), (2.6, [0, 0, 0]), (3.0, [0, -2, 0]))}}
    a = at.animaties(oren=True, staart="tail", extra_idle=piep)
    a["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": hop}
    return a


# =====================================================================================================================
# wombat
# =====================================================================================================================
WO = (142, 112, 90)
WO_L = (192, 166, 140)
WO_D = (96, 76, 62)


def wombat(atlas):
    bruin = at.fluffy(WO, 12)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 5, 0], [cube(atlas, [-6.5, 2.5, -6], [13, 9.5, 14], bruin, {"down": plain(WO_L, 8)})])]
    bones += vier_poten(atlas, "body", 2.5, -5.5, 4.5, [3.5, 2.5, 3.5], plain(WO_D, 8), 2.5)
    bones.append(bone("head", "body", [0, 8, -6], [
        cube(atlas, [-5.5, 4, -13], [11, 9, 7.5], bruin, {"north": gezicht(WO, eye_y=0.36, eye_dx=0.27, eye_r=0.15)}),
        cube(atlas, [-3, 4.5, -14.4], [6, 3.5, 1.6], plain(WO_D, 6), {"north": neusje(WO_D, (40, 32, 36), mond=False)})]))
    oor = vorm_oor(WO, WO_D, punt=0.35)
    bones += paar(atlas, "ear", "head", [4.2, 13, -8], [3.2, 12.5, -8.5], [2.4, 2.4, 1], plain(WO, 6), dict(ZIJ_LEEG, north=oor, south=oor))
    return geo("wombat", bones, atlas, 1.3, 1.0)


def wombat_anims():
    return at.animaties(legs4=POTEN4, oren=True, waggel=True)


# =====================================================================================================================
# quokka
# =====================================================================================================================
QU = (168, 124, 88)
QU_L = (226, 200, 164)
QU_D = (110, 80, 58)


def quokka(atlas):
    vacht = at.fluffy(QU, 10)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 5, 0], [cube(atlas, [-4, 3, -4.5], [8, 8, 9], vacht,
                                                   {"north": at.buik(QU, QU_L, cy=0.62), "down": plain(QU_L, 8)})])]
    for voor, z, size in (("front", -4, [2, 3, 2]), ("back", 1.5, [2.8, 4, 4.5])):
        for side, sx in (("left", 1), ("right", -1)):
            o = [1.2, 0, z] if sx > 0 else [-1.2 - size[0], 0, z]
            bones.append(bone(f"leg_{voor}_{side}", "body", [o[0] + size[0] / 2, size[1], o[2] + size[2] / 2],
                              [cube(atlas, o, size, plain(QU_D, 8))]))
    bones.append(bone("head", "body", [0, 10, -3], [
        cube(atlas, [-4.5, 9.5, -10], [9, 8, 7.5], vacht, {"north": gezicht(QU, eye_y=0.4, eye_r=0.17)}),
        cube(atlas, [-2, 9.8, -11.4], [4, 3.2, 1.6], plain(QU_L, 6), {"north": neusje(QU_L, NEUS)})]))
    oor = vorm_oor(QU, (236, 168, 170), punt=0.0)
    bones += paar(atlas, "ear", "head", [3.8, 17, -6], [2.6, 16.6, -6.5], [3, 3, 1], plain(QU, 6), dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(bone("tail", "body", [0, 4, 4.5], [cube(atlas, [-0.75, 3, 4.5], [1.5, 1.5, 6], plain(QU_D, 6))], rotation=[-10, 0, 0]))
    return geo("quokka", bones, atlas, 1.0, 1.1)


def quokka_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail")


# =====================================================================================================================
# aaihoek: geitje
# =====================================================================================================================
GE = (246, 242, 234)
GE_L = (252, 222, 222)
GE_D = (200, 192, 184)
HOORN = (176, 170, 168)


def geitje(atlas):
    wit = at.fluffy(GE, 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 9, 0], [cube(atlas, [-4.5, 7, -6], [9, 7, 12], wit, {"down": plain(GE_D, 6)})])]
    bones += vier_poten(atlas, "body", 1.2, -5.5, 3.5, [2.5, 7, 2.5], plain(GE, 6), 7, voet=(1.2, plain((96, 86, 84), 6)))
    bones.append(bone("head", "body", [0, 13, -5], [
        cube(atlas, [-4.5, 12, -12], [9, 8, 7.5], wit, {"north": gezicht(GE, eye_y=0.42, eye_r=0.16)}),
        cube(atlas, [-2.2, 12.3, -13.6], [4.4, 3.4, 1.8], plain(GE_L, 6), {"north": neusje(GE_L, (150, 90, 100))}),
        cube(atlas, [-1, 10, -12.6], [2, 2.4, 1.5], plain(GE_D, 6))]))                              # het sikje
    bones += paar(atlas, "horn", "head", [2, 20, -8], [1.2, 19.6, -8.8], [1.4, 2.8, 1.4], plain(HOORN, 6), rotation=[25, 0, 0])
    oor = vorm_oor(GE, GE_L, punt=0.3)
    bones += paar(atlas, "ear", "head", [4.5, 18, -8], [4.5, 17.4, -9.2], [3.5, 1, 2], plain(GE, 6), {"up": oor}, rotation=[0, 0, 25])
    bones.append(bone("tail", "body", [0, 13, 6], [cube(atlas, [-0.75, 12.5, 6], [1.5, 2.5, 1], wit)], rotation=[-30, 0, 0]))
    return geo("geitje", bones, atlas, 1.1, 1.4)


def geitje_anims():
    a = at.animaties(legs4=POTEN4, oren=True, staart="tail")
    a["animations"]["knabbel"] = {"loop": False, "animation_length": 1.4, "bones": {
        "head": {"rotation": at.kf((0, [0, 0, 0]), (0.2, [-25, 0, 0]), (0.35, [-20, 6, 0]), (0.5, [-25, -6, 0]),
                                   (0.65, [-20, 6, 0]), (0.8, [-25, -6, 0]), (1.1, [-15, 0, 0]), (1.4, [0, 0, 0]))}}}
    return a


# =====================================================================================================================
# aaihoek: alpaca
# =====================================================================================================================
AL = (242, 228, 204)
AL_L = (252, 244, 228)
AL_D = (196, 172, 140)


def alpaca(atlas):
    wol = at.fluffy(AL, 14)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 10, 0], [cube(atlas, [-5, 8, -6.5], [10, 9, 13], wol, {"down": plain(AL_D, 8)}, inflate=0.2)])]
    bones += vier_poten(atlas, "body", 1.4, -5.5, 3.5, [3, 8, 3], at.fluffy(AL, 10), 8, voet=(1.2, plain(AL_D, 6)))
    bones.append(bone("hals", "body", [0, 16, -5], [cube(atlas, [-2.5, 15, -7.5], [5, 11, 5], wol)], rotation=[6, 0, 0]))
    bones.append(bone("head", "hals", [0, 26, -6], [
        cube(atlas, [-3.5, 25, -11], [7, 6.5, 6.5], plain(AL_L, 6), {"north": gezicht(AL_L, eye_y=0.42, eye_r=0.18)}),
        cube(atlas, [-2, 25.2, -12.4], [4, 3, 1.6], plain(AL_L, 6), {"north": neusje(AL_L, (120, 92, 90))}),
        cube(atlas, [-3, 30.8, -9.5], [6, 3, 5], wol)]))                                       # de pluizige kuif
    oor = vorm_oor(AL, AL_D, punt=0.5)
    bones += paar(atlas, "ear", "head", [2.5, 33, -7], [1.6, 32.5, -7.5], [1.8, 4.5, 1], plain(AL, 6), dict(ZIJ_LEEG, north=oor, south=oor),
                  rotation=[0, 0, -10])
    bones.append(bone("tail", "body", [0, 16, 6.5], [cube(atlas, [-1.2, 13.5, 6.5], [2.4, 3, 2], wol)]))
    return geo("alpaca", bones, atlas, 1.1, 2.3)


def alpaca_anims():
    a = at.animaties(legs4=POTEN4, oren=True, staart="tail",
                     extra_idle={"hals": {"rotation": at.kf((0, [0, 0, 0]), (1.5, [4, 0, 3]), (3.0, [0, 0, 0]))}})
    a["animations"]["spuug"] = {"loop": False, "animation_length": 1.0, "bones": {
        "hals": {"rotation": at.kf((0, [0, 0, 0]), (0.35, [-18, 0, 0]), (0.5, [14, 0, 0]), (0.75, [10, 0, 0]), (1.0, [0, 0, 0]))},
        "ear_left": {"rotation": at.kf((0, [0, 0, 0]), (0.35, [-35, 0, 0]), (1.0, [0, 0, 0]))},
        "ear_right": {"rotation": at.kf((0, [0, 0, 0]), (0.35, [-35, 0, 0]), (1.0, [0, 0, 0]))}}}
    return a


# =====================================================================================================================
# aaihoek: cavia
# =====================================================================================================================
CA = (250, 246, 238)
CA_O = (232, 150, 72)
CA_B = (110, 72, 50)


def cavia(atlas):
    lapjes = vlekken(CA, [CA_O, CA_B, CA_O], n=6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4, 0], [cube(atlas, [-4.5, 1, -4], [9, 7, 11], lapjes, {"down": plain(CA, 6)})])]
    bones += vier_poten(atlas, "body", 2, -3.5, 4.5, [2, 1, 2], plain((236, 190, 180), 6), 1)
    bones.append(bone("head", "body", [0, 5, -4], [
        cube(atlas, [-4, 1.5, -9], [8, 6.5, 5.5], plain(CA_O, 8),
             {"north": gezicht(CA_O, eye_y=0.42, eye_r=0.17,
                               spots=lambda W, H, rng, a: disc(a, W * 0.5, H * 0.85, W * 0.2, CA, 1.0, ry=H * 0.25))}),
        cube(atlas, [-1.8, 1.8, -10.2], [3.6, 2.6, 1.2], plain(CA, 6), {"north": neusje(CA, (200, 110, 130))})]))
    oor = vorm_oor(CA_O, (240, 170, 176), punt=0.0)
    bones += paar(atlas, "ear", "head", [3.6, 8, -6], [3, 7.4, -6.5], [2.4, 2, 0.6], plain(CA_O, 6), dict(ZIJ_LEEG, north=oor, south=oor),
                  rotation=[0, 0, -30])
    return geo("cavia", bones, atlas, 0.8, 0.6)


def cavia_anims():
    a = at.animaties(legs4=POTEN4, oren=True)
    a["animations"]["piep"] = {"loop": False, "animation_length": 0.8, "bones": {
        "body": {"position": at.kf((0, [0, 0, 0]), (0.12, [0, 3, 0]), (0.24, [0, 0, 0]), (0.4, [0, 2, 0]), (0.52, [0, 0, 0]), (0.8, [0, 0, 0])),
                 "rotation": at.kf((0, [0, 0, 0]), (0.12, [0, 15, 0]), (0.24, [0, 0, 0]), (0.4, [0, -12, 0]), (0.52, [0, 0, 0]))}}}
    return a


# =====================================================================================================================
# aaihoek: ezeltje
# =====================================================================================================================
EZ = (156, 150, 156)
EZ_L = (232, 226, 226)
EZ_D = (84, 78, 86)


def ezeltje(atlas):
    grijs = plain(EZ, 9)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 10, 0], [cube(atlas, [-5, 8, -7], [10, 9, 14], grijs, {"down": plain(EZ_L, 8)}),
                                               cube(atlas, [-0.75, 17, -6.5], [1.5, 1, 10], plain(EZ_D, 6))])]   # rugstreep
    bones += vier_poten(atlas, "body", 1.4, -6.5, 3.5, [3, 8, 3], grijs, 8, voet=(1.4, plain(EZ_D, 6)))
    bones.append(bone("head", "body", [0, 15, -6], [
        cube(atlas, [-4, 13, -13], [8, 8, 8], grijs, {"north": gezicht(EZ, eye_y=0.38, eye_r=0.16)}),
        cube(atlas, [-3, 13.2, -15], [6, 4.4, 2.4], plain(EZ_L, 6), {"north": neusje(EZ_L, EZ_D)}),
        cube(atlas, [-0.75, 18, -7], [1.5, 4, 3], plain(EZ_D, 6))]))                               # manen
    oor = vorm_oor(EZ, EZ_L, punt=0.6, rand=EZ_D)
    bones += paar(atlas, "ear", "head", [2.5, 21, -9], [1.4, 20.6, -9.5], [2.6, 8, 1.2], grijs, dict(ZIJ_LEEG, north=oor, south=oor),
                  rotation=[0, 0, -14])
    bones.append(bone("tail", "body", [0, 16, 7], [cube(atlas, [-0.5, 10, 7], [1, 6, 1], grijs),
                                                   cube(atlas, [-1, 8.5, 6.75], [2, 2, 1.5], plain(EZ_D, 6))]))
    return geo("ezeltje", bones, atlas, 1.2, 1.8)


def ezeltje_anims():
    a = at.animaties(legs4=POTEN4, oren=True, staart="tail")
    a["animations"]["balk"] = {"loop": False, "animation_length": 1.6, "bones": {
        "head": {"rotation": at.kf((0, [0, 0, 0]), (0.3, [-35, 0, 0]), (0.5, [-30, 0, 0]), (0.7, [-35, 0, 0]),
                                   (0.9, [-30, 0, 0]), (1.3, [-30, 0, 0]), (1.6, [0, 0, 0]))},
        "ear_left": {"rotation": at.kf((0, [0, 0, 0]), (0.3, [40, 0, 0]), (1.3, [40, 0, 0]), (1.6, [0, 0, 0]))},
        "ear_right": {"rotation": at.kf((0, [0, 0, 0]), (0.3, [40, 0, 0]), (1.3, [40, 0, 0]), (1.6, [0, 0, 0]))}}}
    return a


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
DIEREN = {   # soort: (bouwer, animaties, seed, jong=(k, hk, drop) of None)
    "koala": (koala, koala_anims, 710, (0.62, 0.82, ())),
    "kangoeroe": (kangoeroe, kangoeroe_anims, 720, (0.5, 0.72, ("buidel_jong",))),
    "wombat": (wombat, wombat_anims, 730, (0.62, 0.8, ())),
    "quokka": (quokka, quokka_anims, 740, (0.62, 0.82, ())),
    # aaihoek-jongen (ronde 34, "voor de gezelligheid"): lammetje zonder hoorntjes, extra pluizige cria,
    # mini-cavia, veulentje met lange pootjes (zie jong_na)
    "geitje": (geitje, geitje_anims, 750, (0.6, 0.8, ("horn_left", "horn_right"))),
    "alpaca": (alpaca, alpaca_anims, 760, (0.6, 0.8, ())),
    "cavia": (cavia, cavia_anims, 770, (0.6, 0.78, ())),
    "ezeltje": (ezeltje, ezeltje_anims, 780, (0.58, 0.8, ())),
}
NAMEN = {"koala": "Koala", "kangoeroe": "Kangoeroe", "wombat": "Wombat", "quokka": "Quokka",
         "geitje": "Geitje (aaihoek)", "alpaca": "Alpaca (aaihoek)", "cavia": "Cavia (aaihoek)", "ezeltje": "Ezeltje (aaihoek)"}


POTEN = ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right")


def lange_poten(geo_, f):
    """Veulen-pootjes: rekt de poten f keer op (onderkant blijft op de grond) en tilt de rest van het lijf mee op."""
    gg = geo_["minecraft:geometry"][0]
    poot = [c for b in gg["bones"] if b["name"] in POTEN for c in b.get("cubes", [])]
    delta = max(c["size"][1] for c in poot) * (f - 1)
    for b in gg["bones"]:
        if b["name"] == "root":
            continue
        b["pivot"][1] = round(b["pivot"][1] + delta, 3)
        for c in b.get("cubes", []):
            if b["name"] in POTEN:
                c["size"][1] = round(c["size"][1] * f, 3)
            else:
                c["origin"][1] = round(c["origin"][1] + delta, 3)
    return geo_


def pluizig(geo_, extra, botten=("body", "hals")):
    """Cria: wat extra opgeblazen wol op lijf en hals."""
    for b in geo_["minecraft:geometry"][0]["bones"]:
        if b["name"] in botten:
            for c in b.get("cubes", []):
                c["inflate"] = round(c.get("inflate", 0) + extra, 3)
    return geo_


JONG_NA = {"ezeltje": lambda g: lange_poten(g, 1.35), "alpaca": lambda g: pluizig(g, 0.35)}


def maak(soort):
    bouw, anim, seed, jong = DIEREN[soort]
    d = os.path.join(UIT, soort)
    a = Atlas(seed, 128)
    g = bouw(a)
    anims = anim()
    img = at.schrijf(d, soort, g, a, anims)
    if jong:
        bg = baby(g, jong[0], jong[1], drop=jong[2])
        if soort in JONG_NA:
            bg = JONG_NA[soort](bg)
        at.schrijf(d, soort + "_baby", bg, a)
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort)


# =====================================================================================================================
# voorstelvellen (kopie van blad.vel/overzicht met eigen titels en zonder verplicht jong)
# =====================================================================================================================
def vellen(soorten):
    import blad                                                          # (blad doet os.chdir naar guhs/)
    from PIL import Image, ImageDraw
    for soort in soorten:
        varianten = [("", "volwassen")]
        if os.path.exists(os.path.join(UIT, soort, f"{soort}_baby.geo.json")):
            varianten.append(("baby", "jong"))
        guh = blad.guh_quads()
        maxh, maxw = blad.hoogte(guh)
        qs = {}
        for v, _ in varianten:
            qs[v] = blad.dier_quads(soort, v)
            h, w = blad.hoogte(qs[v])
            maxh, maxw = max(maxh, h), max(maxw, w)
        CW, CH, GW = 360, 380, 560
        ppb = min(170, (CH - 40) / maxh, (CW - 30) / (maxw * 1.05))
        W, H = 200 + CW * 3 + GW, 90 + CH * len(varianten) + 40
        img = Image.new("RGBA", (W, H), blad.BG)
        d = ImageDraw.Draw(img)
        d.text((20, 18), f"{NAMEN[soort]}  (safari, batch 7)", font=blad.font(30, True), fill=(40, 30, 40))
        d.text((20, 56), "voor / zij / schuin, en schuin naast een guh (zelfde schaal). Animaties volgen in de game.",
               font=blad.font(16), fill=(80, 70, 80))
        for c, (lab, _, _) in enumerate(blad.VIEWS + [("naast een guh", 0, 0)]):
            d.text((200 + c * CW + 10, 88), lab, font=blad.font(16, True), fill=(90, 80, 90))
        combs, ppb_g = {}, ppb * 0.8
        for v, _ in varianten:
            dier = blad.dier_quads(soort, v)
            combs[v] = blad.naast(dier, blad.guh_quads(), dx=-(blad.xbreed(dier) / 2 + blad.xbreed(guh) / 2 + 0.3))
            sx, sy = blad.span(combs[v], 35, -18)
            ppb_g = min(ppb_g, (GW - 30) / sx, (CH - 50) / sy)
        for r, (v, lab) in enumerate(varianten):
            y = 110 + r * CH
            d.text((20, y + CH / 2 - 12), lab, font=blad.font(20, True), fill=(50, 40, 50))
            for c, (_, yaw, pitch) in enumerate(blad.VIEWS):
                img.alpha_composite(blad.render_vast(qs[v], yaw, pitch, ppb, CW, CH - 20), (200 + c * CW, y))
            img.alpha_composite(blad.render_vast(combs[v], 35, -18, ppb_g, GW, CH - 20), (200 + 3 * CW, y))
        pad = os.path.join(UIT, soort, f"{soort}_voorstel.png")
        img.convert("RGB").save(pad)
        print(pad)


def overzicht(soorten):
    import blad
    from PIL import Image, ImageDraw
    import wiki_renders as wr
    guh = blad.guh_quads()
    x, quads = 0.0, []
    items = [("guh", guh, None)]
    for s in soorten:
        b = os.path.join(UIT, s, f"{s}_baby.geo.json")
        items.append((s, blad.dier_quads(s), blad.dier_quads(s, "baby") if os.path.exists(b) else None))
    for naam, q, qb in items:
        h, w = blad.hoogte(q)
        quads += wr.offset_quads(q, dx=(x + w / 2))
        if qb is not None:
            quads += wr.offset_quads(qb, dx=(x + w / 2), dz=-(w * 0.7 + 0.6))
        x += w + 0.7
    ppb = 95
    W, H = int(x * ppb * 0.95) + 200, 640
    img = Image.new("RGBA", (W, H), blad.BG)
    img.alpha_composite(blad.render_vast(quads, 18, -16, ppb, W, H - 100), (0, 90))
    d = ImageDraw.Draw(img)
    d.text((20, 18), "Safari batch 7: Australië + aaihoek, naast een guh (volwassen achter, jong ervoor)", font=blad.font(28, True), fill=(40, 30, 40))
    d.text((20, 56), "Koala · Kangoeroe · Wombat · Quokka · Geitje · Alpaca · Cavia · Ezeltje (van links naar rechts, na de guh)",
           font=blad.font(17), fill=(80, 70, 80))
    pad = os.path.join(UIT, "batch7_overzicht.png")
    img.convert("RGB").save(pad)
    print(pad)


SOORTEN = list(DIEREN)

if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    soorten = args or SOORTEN
    for s in soorten:
        maak(s)
    if "--geen-vellen" not in sys.argv:
        vellen(soorten)
        overzicht(SOORTEN)
