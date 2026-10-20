"""
Safari-voorwerk, batch 2 (savanne A): leeuw, luipaard, neushoorn, kafferbuffel, dikdik, jachtluipaard + de geheime
gouden_dikdik. Zelfde stijl, werkwijze en formaat als batch 1 (dieren.py); alleen geimporteerd, niets daarin aangepast.

    python safari-ontwerp/dieren/_bron/batch2.py [soort ...] [--geen-vellen]

Schrijft per soort in safari-ontwerp/dieren/<soort>/ (zoals batch 1):
    <soort>.geo.json + .png          vrouwtje (of beide als ze gelijk zijn)
    <soort>_man.geo.json + .png      alleen waar ze verschillen (leeuw, luipaard, neushoorn, kafferbuffel, dikdik, gouden_dikdik)
    <soort>_baby.geo.json + .png     het jong
    <soort>.animation.json           gedeeld: idle, walk, blij, eet
    <soort>.bbmodel                  Blockbench-project (volwassen dier)
    <soort>_voorstel.png             het voorstelvel
plus safari-ontwerp/dieren/batch2_overzicht.png.
"""
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import atelier as at                                                    # noqa: E402
import dieren as dr                                                     # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, met, gezicht, neusje, vorm_oor, transparent  # noqa: E402

UIT = dr.UIT
POTEN4, ZIJ_LEEG, vier_poten, paar, baby = dr.POTEN4, dr.ZIJ_LEEG, dr.vier_poten, dr.paar, dr.baby
NEUS = dr.NEUS


# =====================================================================================================================
# hulpjes
# =====================================================================================================================
def paar_cubes(atlas, naam, parent, pivot, cubes, rotation=None):
    """Een links/rechts-paar botten met meerdere kubussen: cubes = [(origin, size, paint), ...] voor de linkerkant (+x)."""
    out = []
    for side, sx in (("left", 1), ("right", -1)):
        cs = [cube(atlas, o if sx > 0 else at.mirror_x(o, s), s, p) for o, s, p in cubes]
        rot = None if rotation is None else [rotation[0], rotation[1] * sx, rotation[2] * sx]
        out.append(bone(f"{naam}_{side}", parent, [pivot[0] * sx, pivot[1], pivot[2]], cs, rot))
    return out


def groter(geo_, f, ident):
    """Hetzelfde model iets groter (luipaard-mannetje): alles schaalt met f."""
    g = baby(geo_, f, f)
    g["minecraft:geometry"][0]["description"]["identifier"] = f"geometry.{ident}"
    return g


def rozetten(base, ring, kern, dicht=1.0):
    """Luipaardrozetten: open donkere ringetjes (gebroken) met een iets donkerder hart."""
    def paint(W, H, rng):
        a = plain(base, 8)(W, H, rng).astype(np.float32)
        n = max(1, int(W * H / (7.5 * at.S) ** 2 * 1.4 * dicht))
        for _ in range(n):
            cx, cy = rng.uniform(0, W), rng.uniform(0, H)
            r = rng.uniform(1.0, 1.5) * at.S
            disc(a, cx, cy, r, ring, 1.0)
            disc(a, cx, cy, r * 0.62, kern, 1.0)
            # gebroken rand: een hapje basis eruit
            ang = rng.uniform(0, 2 * np.pi)
            disc(a, cx + np.cos(ang) * r * 0.95, cy + np.sin(ang) * r * 0.95, r * 0.42, base, 1.0)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def stippen(base, stip, dicht=1.0, rmin=0.45, rmax=0.75):
    """Jachtluipaard: losse, ronde, volle stippen."""
    def paint(W, H, rng):
        a = plain(base, 7)(W, H, rng).astype(np.float32)
        n = max(1, int(W * H / (4.5 * at.S) ** 2 * 1.3 * dicht))
        for _ in range(n):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(rmin, rmax) * at.S, stip, 1.0)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def glans(base, licht, n_frac=1.0):
    """Goud: een warme verloop-glans met witte glinsterpuntjes."""
    def paint(W, H, rng):
        t = (np.arange(H) / max(1, H - 1))[:, None, None]
        a = np.zeros((H, W, 4), np.float32)
        a[..., :3] = np.array(licht) * (1 - t) * 0.6 + np.array(base) * (0.4 + 0.6 * t)
        a[..., :3] += rng.normal(0, 4, (H, W, 1))
        a[..., 3] = 255
        for _ in range(int(W * H / 90 * n_frac)):
            x, y = int(rng.uniform(0, W)), int(rng.uniform(0, H))
            a[y, x, :3] = (255, 250, 214)
            if x + 1 < W:
                a[y, x + 1, :3] = (255, 236, 160)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def staart_pluim(atlas, parent, pivot, lengte, dikte, kleur, pluim, rotation=(20, 0, 0)):
    """Een hangend staartje met een pluimpje aan het eind (leeuw, neushoorn, buffel, olifant-achtig)."""
    x0, y0, z0 = pivot
    return bone("tail", parent, list(pivot), [
        cube(atlas, [-dikte / 2, y0 - lengte, z0], [dikte, lengte, dikte], kleur),
        cube(atlas, [-dikte, y0 - lengte - dikte * 2, z0 - dikte / 2], [dikte * 2, dikte * 2.2, dikte * 2], pluim)],
        rotation=list(rotation))


# =====================================================================================================================
# leeuw
# =====================================================================================================================
LE = (226, 174, 96)
LE_L = (248, 222, 170)
MA = (176, 100, 46)
MA_D = (130, 70, 34)


def leeuw(atlas, man=False):
    vacht = plain(LE, 9)
    licht = plain(LE_L, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 9, 0], [cube(atlas, [-6, 5, -7], [12, 9, 15], vacht, {"down": licht})])]
    bones += vier_poten(atlas, "body", 1.4, -6.5, 4.5, [3.6, 6, 3.6], vacht, 6, voet=(1.2, licht))
    head = [cube(atlas, [-5.5, 9, -16], [11, 10, 9], vacht, {"north": gezicht(LE, eye_y=0.42, eye_r=0.16)}),
            cube(atlas, [-2.6, 9.4, -17.4], [5.2, 3.8, 1.6], licht, {"north": neusje(LE_L, (150, 80, 84))})]
    bones.append(bone("head", "body", [0, 13, -7], head))
    if man:
        manen = at.fluffy(MA, 16)
        bones.append(bone("manen", "head", [0, 14, -10], [
            cube(atlas, [-8, 6.5, -14.5], [16, 15.5, 6.5], manen, {"north": at.fluffy(MA_D, 14)}),
            cube(atlas, [-4, 6, -16.6], [8, 3.4, 4], manen)]))                        # baardje onder de snuit
    oor = vorm_oor(LE, (236, 150, 150), punt=0.0, rand=MA_D if man else None)
    bones += paar(atlas, "ear", "head", [4.3, 19, -14.5], [3, 18.3, -15], [2.8, 2.8, 1], plain(LE, 6), dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(staart_pluim(atlas, "body", (0, 12.5, 8), 7, 1.2, plain(LE, 6), at.fluffy(MA_D, 12), rotation=(25, 0, 0)))
    return geo("leeuw_man" if man else "leeuw", bones, atlas, 1.6 if man else 1.4, 1.5)


def leeuw_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail")


# =====================================================================================================================
# luipaard (rozetten) en jachtluipaard (stippen + traanlijnen, slanker)
# =====================================================================================================================
LP = (236, 190, 100)
LP_L = (250, 234, 200)
LP_RING = (58, 42, 30)
LP_KERN = (206, 150, 72)


def luipaard(atlas):
    vacht = rozetten(LP, LP_RING, LP_KERN)
    licht = plain(LP_L, 6)

    def kopvlekjes(W, H, rng, a):
        for _ in range(7):
            disc(a, rng.uniform(W * 0.2, W * 0.8), rng.uniform(H * 0.02, H * 0.2), W * 0.025, LP_RING, 1.0)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 9, 0], [cube(atlas, [-5, 6, -7], [10, 8, 15], vacht, {"down": licht})])]
    bones += vier_poten(atlas, "body", 1.2, -6.5, 4.5, [3, 6.5, 3], rozetten(LP, LP_RING, LP_KERN, 0.6), 6.5, voet=(1.0, licht))
    bones.append(bone("head", "body", [0, 13, -7], [
        cube(atlas, [-5, 10, -15.5], [10, 9, 8.5], plain(LP, 7), {"north": gezicht(LP, eye_y=0.44, eye_r=0.165, spots=kopvlekjes),
                                                                   "east": rozetten(LP, LP_RING, LP_KERN, 0.7),
                                                                   "west": rozetten(LP, LP_RING, LP_KERN, 0.7)}),
        cube(atlas, [-2.4, 10.4, -16.9], [4.8, 3.6, 1.6], licht, {"north": neusje(LP_L, (150, 80, 84))})]))
    oor = vorm_oor(LP, (60, 44, 34), punt=0.0)
    bones += paar(atlas, "ear", "head", [3.8, 19, -12], [2.6, 18.4, -12.5], [2.6, 2.6, 1], plain(LP, 6), dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(bone("tail", "body", [0, 12.5, 8], [cube(atlas, [-1, 11.5, 8], [2, 2, 14], rozetten(LP, LP_RING, LP_KERN, 0.8),
                                                         {"south": plain(LP_RING, 6)})], rotation=[-12, 0, 0]))
    return geo("luipaard", bones, atlas, 1.4, 1.4)


def luipaard_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail")


JL = (232, 196, 120)
JL_L = (252, 240, 214)
JL_ZW = (36, 30, 30)


def jachtluipaard(atlas):
    vacht = stippen(JL, JL_ZW)
    licht = plain(JL_L, 6)

    def tranen(W, H, rng, a):
        # de zwarte traanlijnen: van de binnenooghoek omlaag naar de mondhoeken
        for sx in (-1, 1):
            x0, y0 = W / 2 + sx * W * 0.13, H * 0.58
            for i in range(int(H * 0.42)):
                y = y0 + i
                x = x0 + sx * i * 0.18
                disc(a, x, y, W * 0.028, JL_ZW, 1.0)
        for _ in range(6):
            disc(a, rng.uniform(W * 0.25, W * 0.75), rng.uniform(H * 0.03, H * 0.16), W * 0.022, JL_ZW, 1.0)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 11, 0], [cube(atlas, [-4, 8.5, -7], [8, 7, 15], vacht, {"down": licht, "north": licht})])]
    bones += vier_poten(atlas, "body", 0.9, -6.3, 4.8, [2.4, 9, 2.4], stippen(JL, JL_ZW, 0.7, 0.3, 0.5), 9, voet=(0.8, plain(JL_ZW, 6)))
    bones.append(bone("head", "body", [0, 15, -7], [
        cube(atlas, [-4.5, 13, -14.5], [9, 8, 7.5], plain(JL, 7), {"north": gezicht(JL, eye_y=0.4, eye_r=0.165, spots=tranen),
                                                                   "east": stippen(JL, JL_ZW, 0.6), "west": stippen(JL, JL_ZW, 0.6)}),
        cube(atlas, [-2, 13.3, -15.8], [4, 3, 1.3], licht, {"north": neusje(JL_L, JL_ZW)})]))
    oor = vorm_oor(JL, JL_ZW, punt=0.0)
    bones += paar(atlas, "ear", "head", [3.3, 21, -11.5], [2.4, 20.5, -12], [2.2, 2.2, 1], plain(JL, 6), dict(ZIJ_LEEG, north=oor, south=oor))
    staartverf = met(stippen(JL, JL_ZW, 0.8), lambda W, H, rng, a: a.__setitem__((slice(None), slice(int(W * 0.68), W), slice(0, 3)), JL_ZW))
    staartring = at.ringen(JL, JL_ZW, "H", n=3)
    bones.append(bone("tail", "body", [0, 14.5, 8], [cube(atlas, [-0.9, 13.6, 8], [1.8, 1.8, 14], stippen(JL, JL_ZW),
                                                          {"east": staartverf, "west": met(stippen(JL, JL_ZW, 0.8), lambda W, H, rng, a: a.__setitem__((slice(None), slice(0, int(W * 0.32)), slice(0, 3)), JL_ZW)),
                                                           "up": staartring, "down": staartring, "south": plain((250, 250, 248), 4)})],
                      rotation=[22, 0, 0]))
    return geo("jachtluipaard", bones, atlas, 1.3, 1.5)


def jachtluipaard_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail")


# =====================================================================================================================
# neushoorn
# =====================================================================================================================
NH = (168, 160, 164)
NH_D = (140, 132, 138)
NH_L = (196, 190, 192)
HOORN = (214, 202, 180)


def neushoorn(atlas, man=False):
    huid = plain(NH, 7)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 12, 0], [cube(atlas, [-8, 7, -10], [16, 12, 19], huid, {"down": plain(NH_D, 6)})])]
    bones += vier_poten(atlas, "body", 2.2, -8.5, 4, [4.6, 7.5, 4.6], huid, 7.5, {"north": dr.nagels(NH, (232, 224, 210)), "down": plain(NH_D, 6)})
    bones.append(bone("head", "body", [0, 14, -9], [
        cube(atlas, [-6, 9.5, -18.5], [12, 10, 9.5], huid, {"north": gezicht(NH, eye_y=0.36, eye_dx=0.28, eye_r=0.14)}),
        cube(atlas, [-4.5, 9.5, -21.6], [9, 5.6, 3.2], plain(NH_L, 6), {"north": neusje(NH_L, (96, 84, 92))})]))
    hl = [3.6, 6.8, 3.6] if man else [3, 4.4, 3]
    bones.append(bone("horn", "head", [0, 15, -20], [
        cube(atlas, [-hl[0] / 2, 15.1, -21.4], hl, plain(HOORN, 6)),
        cube(atlas, [-hl[0] / 2 + 0.6, 15.1 + hl[1], -21.4 + 0.6], [hl[0] - 1.2, 1.6, hl[2] - 1.2], plain(HOORN, 6)),
        cube(atlas, [-1, 19.5, -17.2], [2, 2.4 if man else 1.8, 2], plain(HOORN, 6))], rotation=[-12, 0, 0]))
    oor = vorm_oor(NH, (226, 170, 180), punt=0.5)
    bones += paar(atlas, "ear", "head", [4.4, 19.5, -11], [3.4, 19.2, -11.5], [2.4, 3.4, 1], huid, dict(ZIJ_LEEG, north=oor, south=oor),
                  rotation=[0, 0, -12])
    bones.append(staart_pluim(atlas, "body", (0, 17, 9), 5, 1, huid, plain((80, 74, 78), 6)))
    return geo("neushoorn_man" if man else "neushoorn", bones, atlas, 2.0, 1.7)


def neushoorn_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail")


# =====================================================================================================================
# kafferbuffel (+ het vogeltje op de rug)
# =====================================================================================================================
KB = (82, 72, 72)
KB_D = (58, 50, 52)
KB_L = (130, 118, 116)
KB_HOORN = (54, 48, 46)


def kafferbuffel(atlas, man=False):
    vacht = plain(KB, 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 12, 0], [cube(atlas, [-7, 7, -9], [14, 11, 17], vacht, {"down": plain(KB_D, 6)}),
                                               cube(atlas, [-6, 17.5, -8], [12, 2, 7], vacht)])]   # schoftbult
    bones += vier_poten(atlas, "body", 2.0, -7.5, 3.5, [3.8, 7.5, 3.8], vacht, 7.5, voet=(1.4, plain(KB_D, 6)))
    bones.append(bone("head", "body", [0, 14, -8], [
        cube(atlas, [-5.5, 9, -16.5], [11, 9.5, 8.5], vacht, {"north": gezicht(KB_L, eye_y=0.44, eye_r=0.16)}),
        cube(atlas, [-3.5, 9.3, -18], [7, 4, 1.6], plain(KB_L, 6), {"north": neusje(KB_L, (40, 34, 36))})]))
    hoorn = plain(KB_HOORN, 6)
    if man:
        # de "boss" (het hoornschild bovenop) + gekrulde hoorns: opzij, omlaag, en met de punt weer omhoog
        bones.append(bone("boss", "head", [0, 18.5, -12], [cube(atlas, [-5.5, 18.4, -14.5], [11, 1.8, 4.5], hoorn)]))
        bones += paar_cubes(atlas, "horn", "head", [5.5, 18.5, -12.5], [
            ([5.4, 17.2, -14], [4.2, 2.4, 2.6], hoorn),
            ([8.6, 13.6, -14], [2.4, 3.8, 2.4], hoorn),
            ([9.6, 12.4, -14], [2.8, 1.6, 2.2], hoorn),
            ([11.2, 12.4, -13.8], [1.6, 3.6, 1.6], hoorn)])
    else:
        bones += paar_cubes(atlas, "horn", "head", [5.5, 17.5, -12.5], [
            ([5.4, 16.6, -13.6], [2.8, 1.6, 1.8], hoorn),
            ([7.6, 16.6, -13.4], [1.4, 2.8, 1.4], hoorn)])
    oor = vorm_oor(KB, (190, 140, 140), punt=0.2)
    bones += paar(atlas, "ear", "head", [5.5, 15, -12], [5.5, 14.3, -12.5], [3.2, 2, 1], plain(KB, 6), dict(ZIJ_LEEG, north=oor, south=oor),
                  rotation=[0, 0, 15])
    bones.append(staart_pluim(atlas, "body", (0, 16.5, 8), 6, 1, vacht, plain(KB_D, 6)))
    # het madenpikkertje op de rug
    bones.append(bone("vogeltje", "body", [0, 18, 2], [
        cube(atlas, [-1, 18, 1.2], [2, 1.8, 3], plain((150, 120, 92), 6)),
        cube(atlas, [-0.8, 19.3, 0.1], [1.6, 1.6, 1.6], plain((150, 120, 92), 6), {"north": gezicht((150, 120, 92), eye_y=0.45, eye_r=0.2, blush=False)}),
        cube(atlas, [-0.3, 19.7, -0.8], [0.6, 0.6, 0.9], plain((236, 60, 50), 4))]))
    return geo("kafferbuffel_man" if man else "kafferbuffel", bones, atlas, 1.9, 1.7)


def kafferbuffel_anims():
    pik = {"vogeltje": {"rotation": at.kf((0, [0, 0, 0]), (1.8, [0, 0, 0]), (1.95, [25, 0, 0]), (2.1, [0, 0, 0]), (2.25, [25, 0, 0]), (2.4, [0, 0, 0]))}}
    return at.animaties(legs4=POTEN4, oren=True, staart="tail", extra_idle=pik)


# =====================================================================================================================
# dikdik (en de geheime gouden dikdik)
# =====================================================================================================================
DD = (184, 132, 94)
DD_L = (246, 230, 214)
DD_KUIF = (168, 96, 62)
GOUD = (226, 172, 48)
GOUD_L = (255, 232, 140)
GOUD_D = (176, 120, 30)


def dikdik(atlas, man=False, goud=False):
    if goud:
        vacht, licht, kuif = glans(GOUD, GOUD_L), glans(GOUD_L, (255, 248, 210), 0.6), glans(GOUD_D, GOUD)
        fur_face, oor_in = GOUD, (255, 200, 170)
    else:
        vacht, licht, kuif = plain(DD, 8), plain(DD_L, 6), at.fluffy(DD_KUIF, 12)
        fur_face, oor_in = DD, (236, 160, 160)

    def oogring(W, H, rng, a):
        for sx in (-1, 1):
            disc(a, W / 2 + sx * W * 0.26, H * 0.44, W * 0.21, DD_L if not goud else GOUD_L, 1.0, ry=H * 0.22)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 9, 0], [cube(atlas, [-3, 7, -5], [6, 6, 10], vacht, {"down": licht})])]
    bones += vier_poten(atlas, "body", 0.6, -4.6, 3.2, [1.6, 7.5, 1.6], vacht, 7.5, voet=(1.0, plain(GOUD_D if goud else (70, 50, 40), 6)))
    bones.append(bone("head", "body", [0, 12.5, -5], [
        cube(atlas, [-3.5, 11.5, -11], [7, 6.5, 6], vacht, {"north": gezicht(fur_face, eye_y=0.44, eye_r=0.17, spots=oogring)}),
        cube(atlas, [-1.5, 11.7, -12.8], [3, 3, 1.8], licht, {"north": neusje(DD_L if not goud else GOUD_L, NEUS)})]))
    bones.append(bone("kuif", "head", [0, 18, -8], [cube(atlas, [-1.2, 17.9, -9.4], [2.4, 1.6, 2.6], kuif)]))
    oor = vorm_oor(fur_face, oor_in, punt=0.35)
    bones += paar(atlas, "ear", "head", [3.2, 17, -8.5], [3.0, 16.4, -9], [2.6, 4.2, 1], plain(fur_face, 6), dict(ZIJ_LEEG, north=oor, south=oor),
                  rotation=[0, 0, -28])
    if man:
        hoorn = plain((236, 214, 120) if goud else (70, 56, 46), 5)
        bones += paar(atlas, "hoorn", "head", [1.4, 18, -8.8], [0.9, 17.8, -9.3], [0.9, 2.6, 0.9], hoorn, rotation=[-14, 0, 0])
    bones.append(bone("tail", "body", [0, 12.5, 5], [cube(atlas, [-0.6, 11, 5], [1.2, 1.6, 1.2], vacht)]))
    ident = ("gouden_dikdik" if goud else "dikdik") + ("_man" if man else "")
    return geo(ident, bones, atlas, 0.8, 1.2)


def dikdik_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail")


# =====================================================================================================================
# alles wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "leeuw":
        a = Atlas(210); g = leeuw(a); anims = leeuw_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(211); at.schrijf(d, soort + "_man", leeuw(am, man=True), am)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8), a)
    elif soort == "luipaard":
        a = Atlas(220); g = luipaard(a); anims = luipaard_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_man", groter(g, 1.12, "luipaard_man"), a)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8), a)
    elif soort == "jachtluipaard":
        a = Atlas(230); g = jachtluipaard(a); anims = jachtluipaard_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8), a)
    elif soort == "neushoorn":
        a = Atlas(240, 128); g = neushoorn(a); anims = neushoorn_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(240, 128); at.schrijf(d, soort + "_man", neushoorn(am, man=True), am)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8, drop=()), a)
    elif soort == "kafferbuffel":
        a = Atlas(250, 128); g = kafferbuffel(a); anims = kafferbuffel_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(250, 128); at.schrijf(d, soort + "_man", kafferbuffel(am, man=True), am)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8, drop=("horn_left", "horn_right", "vogeltje")), a)
    elif soort in ("dikdik", "gouden_dikdik"):
        goud = soort == "gouden_dikdik"
        a = Atlas(260 + goud); g = dikdik(a, goud=goud); anims = dikdik_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(260 + goud); at.schrijf(d, soort + "_man", dikdik(am, man=True, goud=goud), am)
        at.schrijf(d, soort + "_baby", baby(g, 0.62, 0.8), a)
    else:
        raise SystemExit(f"onbekende soort {soort}")
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort)


SOORTEN = ["leeuw", "luipaard", "neushoorn", "kafferbuffel", "dikdik", "jachtluipaard", "gouden_dikdik"]
NAMEN = {"leeuw": "Leeuw", "luipaard": "Luipaard", "neushoorn": "Neushoorn", "kafferbuffel": "Kafferbuffel", "dikdik": "Dikdik",
         "jachtluipaard": "Jachtluipaard", "gouden_dikdik": "Gouden dikdik (geheim)"}


# =====================================================================================================================
# vellen (blad.py hergebruikt; alleen de titels van batch 2 erbij)
# =====================================================================================================================
def vellen(soorten):
    import blad                                                          # zet de cwd naar guhs/ (wiki_renders)
    blad.NAMEN.update(NAMEN)
    for s in soorten:
        blad.vel(s)
        pad = os.path.join(UIT, s, f"{s}_voorstel.png")
        img = Image.open(pad).convert("RGB")
        d = ImageDraw.Draw(img)
        d.rectangle([0, 10, img.width, 52], fill=blad.BG[:3])
        d.text((20, 18), f"{NAMEN[s]}  (safari-dieren, batch 2)", font=blad.font(30, True), fill=(40, 30, 40))
        img.save(pad)
    # overzicht: dezelfde opzet als batch 1
    guh = blad.guh_quads()
    x, quads = 0.0, []
    for naam, q, qb in [("guh", guh, None)] + [(s, blad.dier_quads(s), blad.dier_quads(s, "baby")) for s in SOORTEN]:
        h, w = blad.hoogte(q)
        quads += blad.wr.offset_quads(q, dx=(x + w / 2))
        if qb is not None:
            quads += blad.wr.offset_quads(qb, dx=(x + w / 2), dz=-(w * 0.7 + 0.6))
        x += w + 0.7
    ppb = 95
    W, H = int(x * ppb * 0.95) + 200, 640
    t = blad.render_vast(quads, 18, -16, ppb, W, H - 100)
    img = Image.new("RGBA", (W, H), blad.BG)
    img.alpha_composite(t, (0, 90))
    d = ImageDraw.Draw(img)
    d.text((20, 18), "Safari-dieren batch 2: savanne A, naast een guh (volwassen achter, jong ervoor)", font=blad.font(28, True), fill=(40, 30, 40))
    d.text((20, 56), " · ".join(NAMEN[s] for s in SOORTEN) + " (van links naar rechts, na de guh)", font=blad.font(17), fill=(80, 70, 80))
    pad = os.path.join(UIT, "batch2_overzicht.png")
    img.convert("RGB").save(pad)
    print(pad)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    soorten = args or SOORTEN
    for s in soorten:
        maak(s)
    if "--geen-vellen" not in sys.argv:
        vellen(soorten)
