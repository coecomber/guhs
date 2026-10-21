"""
Safari-dieren batch 13 (testronde 4, ronde 53: Het Kriebelhuis, de Aquariumtunnel): clownvisje (+ een losse anemoon),
kwalletje (gloeit), kogelvis (blaast op), zeester, zee-egeltje, babyhaai en regenboogvisje (voor een school). Zelfde
stijl en bestandsformaat als batch 10-12; de gedeelde hulpjes komen uit batch12.py.

    python tools/features/safari_atelier/batch13.py [soort ...] [--geen-vellen]

Alles zwemt: walk = zwemmen (+ een eigen "zwem"), behalve zeester en zee-egel (heel traag over de bodem). De anemoon is
een los, stilstaand model (geen dier, geen dex-soort): een thuis voor het clownvisje. Het kwalletje heeft kwal_glow.png
(de tentakels en de binnenkant van het klokje; niet het gezichtje, zodat de slaapoogjes niet gaan gloeien); safari_dieren
maakt daar kwal_glowmask.png van, en batch12.alles zet er ook kwal_slaap_glowmask.png naast.
"""
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, gezicht       # noqa: E402
import batch12 as b12                                                   # noqa: E402
from batch12 import extra, verberg_in_standaard, maak_soort            # noqa: E402

UIT = os.path.abspath(os.path.join(HIER, "..", "..", "..", "build", "safari_atelier"))
SOORTEN = ["clownvis", "anemoon", "kwal", "kogelvis", "zeester", "zee_egel", "babyhaai", "regenboogvis"]
NAMEN = {"clownvis": "Clownvisje", "anemoon": "Anemoon (thuis van het clownvisje)", "kwal": "Kwalletje",
         "kogelvis": "Kogelvis", "zeester": "Zeester", "zee_egel": "Zee-egeltje", "babyhaai": "Babyhaai",
         "regenboogvis": "Regenboogvisje"}
kf = at.kf
S = at.S
LEEG = at.transparent


def vis_zwem(L=1.0, staart="tail", a=24, vin=None, hoogte=(1.0, 1.8)):
    z = {"root": {"position": kf((0, [0, hoogte[0], 0]), (L / 2, [0, hoogte[1], 0]), (L, [0, hoogte[0], 0]))},
         staart: {"rotation": kf((0, [0, a, 0]), (L / 2, [0, -a, 0]), (L, [0, a, 0]))},
         "body": {"rotation": kf((0, [0, -4, 0]), (L / 2, [0, 4, 0]), (L, [0, -4, 0]))}}
    for n in vin or []:
        z[n] = {"rotation": kf((0, [0, 0, -20]), (L / 4, [0, 0, 20]), (L / 2, [0, 0, -20]), (3 * L / 4, [0, 0, 20]), (L, [0, 0, -20]))}
    return z


def vis_anims(L=1.0, vin=None, staart="tail"):
    an = at.animaties(extra_idle=vis_zwem(3.0, staart, 10, vin, (1.0, 1.5)))
    z = vis_zwem(L, staart, 24, vin)
    an["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": z}
    extra(an, "zwem", L, z)
    return an


def vinnen(atlas, parent, y, z, paint, lengte=2.2):
    out = []
    for side, sx in (("left", 1), ("right", -1)):
        o = [1.4, y - 0.8, z] if sx > 0 else [-1.4 - 0.2, y - 0.8, z]
        out.append(bone(f"vin_{side}", parent, [sx * 1.4, y, z], [cube(atlas, o, [0.2, 1.6, lengte], paint)], rotation=[0, sx * 30, 0]))
    return out


# =====================================================================================================================
# clownvisje: oranje met witte banden met een donker randje, ronde vinnetjes
# =====================================================================================================================
CV = (250, 128, 40)
CV_W = (252, 250, 246)
CV_D = (40, 34, 40)


def clownvis(atlas):
    def banden(W, H, rng, a):
        for f in (0.28, 0.62):
            x0, x1 = int(W * f), int(W * (f + 0.12))
            a[:, x0 - 1:x1 + 1, :3] = np.array(CV_D)
            a[:, x0:x1, :3] = np.array(CV_W)
    huid = plain(CV, 8, banden)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3, 0], [cube(atlas, [-1.6, 1.4, -3.4], [3.2, 3.8, 6.4], huid,
                                                   {"north": gezicht(CV, eye_y=0.42, eye_dx=0.25, eye_r=0.2)})])]
    bones.append(bone("head", "body", [0, 3.3, -3.4], []))
    bones.append(bone("tail", "body", [0, 3.3, 3], [cube(atlas, [-0.2, 1.6, 3], [0.4, 3.4, 2.4], plain(CV, 6, lambda W, H, rng, a:
                                                     a.__setitem__((slice(None), slice(W - S, W), slice(0, 3)), CV_D)))]))
    bones.append(bone("rugvin", "body", [0, 5.2, 0], [cube(atlas, [-0.2, 5.2, -1.6], [0.4, 1.4, 3.4], plain(CV, 6))]))
    bones += vinnen(atlas, "body", 2.8, -1.4, plain(CV, 6), 1.8)
    return geo("clownvis", bones, atlas, 0.4, 0.5)


# de anemoon: een kussentje met wuivende tentakeltjes (stilstaand model, alleen een idle-wuif)
AN = (236, 120, 170)
AN_L = (255, 196, 220)


def anemoon(atlas):
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 1, 0], [cube(atlas, [-3, 0, -3], [6, 2, 6], plain(AN, 8))])]
    bones.append(bone("head", "body", [0, 2, 0], []))
    rng = np.random.default_rng(5)
    for i in range(9):
        x, z = (i % 3 - 1) * 1.8, (i // 3 - 1) * 1.8
        h = 3.5 + rng.uniform(0, 1.5)
        bones.append(bone(f"tentakel_{i}", "body", [x, 2, z], [cube(atlas, [x - 0.4, 2, z - 0.4], [0.8, h, 0.8], plain(AN, 6)),
                                                              cube(atlas, [x - 0.55, 2 + h, z - 0.55], [1.1, 1.1, 1.1], plain(AN_L, 4))]))
    return geo("anemoon", bones, atlas, 0.6, 0.5)


def anemoon_anims():
    idle = {f"tentakel_{i}": {"rotation": kf((0, [6 * ((-1) ** i), 0, 4]), (1.5 + 0.1 * i, [-6 * ((-1) ** i), 0, -4]),
                                             (3.0, [6 * ((-1) ** i), 0, 4]))} for i in range(9)}
    an = at.animaties(extra_idle=idle)
    an["animations"]["walk"] = {"loop": True, "animation_length": 3.0, "bones": idle}
    return an


# =====================================================================================================================
# kwalletje: rond klokje met gezichtje en zachte gloeiende tentakeltjes; pulst omhoog
# =====================================================================================================================
KW = (196, 176, 246)
KW_L = (236, 226, 255)
KW_GLOED = (176, 220, 255)


def kwal(atlas):
    klok = plain(KW, 6, lambda W, H, rng, a: a.__setitem__((slice(None), slice(None), 3), 220))
    gloed = plain(KW_GLOED, 6)
    bones = [bone("root", None, [0, 0, 0], [])]
    kern = cube(atlas, [-1.6, 6.2, -1.6], [3.2, 2.2, 3.2], gloed)                # de gloeiende binnenkant (zichtbaar onder het klokje)
    bones.append(bone("body", "root", [0, 6, 0], [cube(atlas, [-2.8, 6, -2.8], [5.6, 4.2, 5.6], klok,
                                                       {"north": gezicht(KW, eye_y=0.5, eye_dx=0.24, eye_r=0.19)}), kern]))
    bones.append(bone("head", "body", [0, 8, -2.8], []))
    tentakels = []
    for i, (x, z) in enumerate(((-1.6, -1.6), (1.2, -1.6), (-1.6, 1.2), (1.2, 1.2), (-0.2, -0.2))):
        c = cube(atlas, [x, 1.4, z], [0.5, 4.6, 0.5], gloed)
        tentakels.append(c)
        bones.append(bone(f"tentakel_{i}", "body", [x + 0.25, 6, z + 0.25], [c]))
    return geo("kwal", bones, atlas, 0.5, 0.7), [kern] + tentakels


def kwal_anims():
    def puls(L):
        b = {"root": {"position": kf((0, [0, 2, 0]), (L * 0.3, [0, 3.4, 0]), (L, [0, 2, 0]))},
             "body": {"scale": kf((0, [1, 1, 1]), (L * 0.15, [0.85, 1.12, 0.85]), (L * 0.35, [1.06, 0.94, 1.06]), (L, [1, 1, 1]))}}
        for i in range(5):
            b[f"tentakel_{i}"] = {"rotation": kf((0, [8, 0, 0]), (L * 0.3, [-10, 0, 6]), (L, [8, 0, 0]))}
        return b
    an = at.animaties(extra_idle=puls(3.0))
    an["animations"]["walk"] = {"loop": True, "animation_length": 1.6, "bones": puls(1.6)}
    extra(an, "zwem", 1.6, puls(1.6))
    return an


# =====================================================================================================================
# kogelvis: rond vissie met gezichtje; "opblazen" tot een stekelbolletje
# =====================================================================================================================
KG = (226, 206, 132)
KG_L = (250, 244, 214)
KG_D = (156, 130, 76)


def kogelvis(atlas):
    def stippen(W, H, rng, a):
        for _ in range(max(2, W * H // 120)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H * 0.6), rng.uniform(0.4, 0.8) * S, KG_D, 0.8)
    huid = plain(KG, 8, stippen)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3.4, 0], [cube(atlas, [-2.2, 1.4, -2.6], [4.4, 4, 5.2], huid,
                                                     {"north": gezicht(KG, eye_y=0.44, eye_dx=0.26, eye_r=0.2), "down": plain(KG_L, 6)})])]
    bones.append(bone("head", "body", [0, 3.4, -2.6], []))
    # stekeltjes (alleen bij opblazen te zien)
    stekels = []
    for (x, y, z) in ((-2.6, 3.2, 0), (2.2, 3.2, 0), (0, 5.4, 0), (0, 1.0, 0), (-1.4, 4.8, -1.8), (1.0, 4.8, -1.8),
                      (-1.4, 4.8, 1.8), (1.0, 4.8, 1.8), (0, 3.2, 2.6)):
        stekels.append(cube(atlas, [x, y, z], [0.4, 0.4, 0.4], plain(KG_D, 4)))
    bones.append(bone("stekels", "body", [0, 3.4, 0], stekels))
    bones.append(bone("tail", "body", [0, 3.4, 2.6], [cube(atlas, [-0.2, 2.2, 2.6], [0.4, 2.4, 1.8], plain(KG, 6))]))
    bones += vinnen(atlas, "body", 3.2, -1.0, plain(KG_L, 6), 1.4)
    return geo("kogelvis", bones, atlas, 0.5, 0.5)


def kogelvis_anims():
    an = vis_anims(1.0, ["vin_left", "vin_right"])
    verberg_in_standaard(an, "stekels")
    an["animations"]["zwem"]["bones"].update(b12.verborgen("stekels"))
    extra(an, "opblazen", 4.0, {
        "body": {"scale": kf((0, [1, 1, 1]), (0.3, [1.7, 1.7, 1.6]), (3.4, [1.7, 1.7, 1.6]), (4.0, [1, 1, 1]))},
        "stekels": {"scale": kf((0, [0, 0, 0]), (0.25, [1, 1, 1]), (3.5, [1, 1, 1]), (3.6, [0, 0, 0]))},
        "root": {"position": kf((0, [0, 1, 0]), (0.3, [0, 2, 0]), (4.0, [0, 1, 0]))}}, loop=False)
    return an


# =====================================================================================================================
# zeester: plat met vijf armpjes en een gezichtje bovenop; heel traag over de bodem
# =====================================================================================================================
ZS = (246, 132, 96)
ZS_L = (255, 210, 170)


def zeester(atlas):
    def bobbels(W, H, rng, a):
        for _ in range(max(2, W * H // 100)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.3, 0.6) * S, ZS_L, 0.9)
    huid = plain(ZS, 8, bobbels)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 0.5, 0], [cube(atlas, [-1.6, 0, -1.6], [3.2, 1.2, 3.2], huid,
                                                     {"up": gezicht(ZS, eye_y=0.46, eye_dx=0.24, eye_r=0.2)})])]
    bones.append(bone("head", "body", [0, 1, 0], []))
    for i in range(5):
        hoek = i * 72
        bones.append(bone(f"arm_{i}", "body", [0, 0.5, 0], [cube(atlas, [-0.8, 0.05, -5.6], [1.6, 0.9, 4.2], huid)],
                          rotation=[0, hoek, 0]))
    return geo("zeester", bones, atlas, 0.7, 0.15)


def zeester_anims():
    idle = {f"arm_{i}": {"rotation": kf((0, [0, i * 72, 0]), (1.5 + i * 0.2, [6, i * 72, 0]), (3.0, [0, i * 72, 0]))} for i in range(5)}
    an = at.animaties(extra_idle=idle)
    L = 4.0
    kruip = {f"arm_{i}": {"rotation": kf((0, [0, i * 72, 0]), (L / 2, [10 * ((-1) ** i), i * 72, 0]), (L, [0, i * 72, 0]))}
             for i in range(5)}
    kruip["body"] = {"rotation": kf((0, [0, 0, 0]), (L, [0, 30, 0]))}
    an["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": kruip}
    extra(an, "kruip", L, kruip)
    return an


# =====================================================================================================================
# zee-egeltje: rond bolletje met stekeltjes en een gezichtje; schuifelt traag
# =====================================================================================================================
ZE = (146, 92, 170)
ZE_D = (100, 60, 126)


def zee_egel(atlas):
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 1.6, 0], [cube(atlas, [-2, 0, -2], [4, 3.2, 4], plain(ZE, 8),
                                                     {"north": gezicht(ZE, eye_y=0.5, eye_dx=0.25, eye_r=0.2)})])]
    bones.append(bone("head", "body", [0, 1.6, -2], []))
    stekels = []
    rng = np.random.default_rng(9)
    for _ in range(16):
        f = rng.integers(0, 4)
        x, y, z = rng.uniform(-1.8, 1.4), rng.uniform(0.6, 2.8), rng.uniform(-1.8, 1.4)
        if f == 0:
            o, sz = [x, 3.2, z], [0.4, 1.6, 0.4]
        elif f == 1:
            o, sz = [1.9, y, z], [1.6, 0.4, 0.4]
        elif f == 2:
            o, sz = [-3.5, y, z], [1.6, 0.4, 0.4]
        else:
            o, sz = [x, y, 1.9], [0.4, 0.4, 1.6]
        stekels.append(cube(atlas, o, sz, plain(ZE_D, 4)))
    bones.append(bone("stekels", "body", [0, 1.6, 0], stekels))
    return geo("zee_egel", bones, atlas, 0.4, 0.35)


def zee_egel_anims():
    an = at.animaties(extra_idle={"stekels": {"scale": kf((0, [1, 1, 1]), (1.5, [1.06, 1.06, 1.06]), (3.0, [1, 1, 1]))}})
    L = 3.0
    an["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": {
        "body": {"rotation": kf((0, [0, 0, -4]), (L / 2, [0, 0, 4]), (L, [0, 0, -4]))}}}
    return an


# =====================================================================================================================
# babyhaai: rond en lief, grijs met een witte buik, rugvin, staartvin; zwemt rondjes
# =====================================================================================================================
BH = (138, 150, 170)
BH_L = (240, 242, 246)


def babyhaai(atlas):
    huid = plain(BH, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3.4, 0], [cube(atlas, [-2.2, 1.4, -4.4], [4.4, 4, 8.8], huid,
                                                     {"down": plain(BH_L, 4), "north": gezicht(BH, eye_y=0.42, eye_dx=0.27, eye_r=0.19)})])]
    bones.append(bone("head", "body", [0, 3.4, -4.4], []))
    bones.append(bone("rugvin", "body", [0, 5.4, -0.6], [cube(atlas, [-0.3, 5.4, -1.6], [0.6, 2.6, 2.4], huid)], rotation=[-20, 0, 0]))
    bones.append(bone("tail", "body", [0, 3.4, 4.4], [cube(atlas, [-0.8, 2.4, 4.4], [1.6, 2, 2.4], huid),
                                                     cube(atlas, [-0.3, 1.0, 6.4], [0.6, 4.8, 1.8], huid)]))
    bones += vinnen(atlas, "body", 2.2, -1.6, huid, 2.4)
    return geo("babyhaai", bones, atlas, 0.6, 0.5)


# =====================================================================================================================
# regenboogvisje: klein, met een kleurverloop van roze via geel naar blauw (bedoeld voor een school)
# =====================================================================================================================
def regenboogvis(atlas):
    def regenboog(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        kleuren = [(250, 110, 150), (250, 200, 80), (110, 200, 120), (100, 160, 240), (170, 120, 230)]
        for x in range(W):
            t = x / max(1, W - 1) * (len(kleuren) - 1)
            i = min(int(t), len(kleuren) - 2)
            f = t - i
            a[:, x, :3] = np.array(kleuren[i]) * (1 - f) + np.array(kleuren[i + 1]) * f
        a[..., 3] = 255
        a[..., :3] += rng.normal(0, 4, (H, W, 1))
        return np.clip(a, 0, 255).astype(np.uint8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2, 0], [cube(atlas, [-0.8, 1.0, -2.2], [1.6, 2.2, 4.4], regenboog,
                                                   {"north": gezicht((250, 110, 150), eye_y=0.42, eye_dx=0.25, eye_r=0.21)})])]
    bones.append(bone("head", "body", [0, 2, -2.2], []))
    bones.append(bone("tail", "body", [0, 2.1, 2.2], [cube(atlas, [-0.1, 0.9, 2.2], [0.2, 2.4, 1.6], plain((170, 120, 230), 6))]))
    return geo("regenboogvis", bones, atlas, 0.3, 0.3)


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "clownvis":
        a = Atlas(1310); maak_soort(d, soort, clownvis(a), a, vis_anims(0.8, ["vin_left", "vin_right"]), (0.6, 0.8))
    elif soort == "anemoon":
        a = Atlas(1315); maak_soort(d, soort, anemoon(a), a, anemoon_anims(), None)
    elif soort == "kwal":
        a = Atlas(1320); g, gloed = kwal(a); maak_soort(d, soort, g, a, kwal_anims(), (0.6, 0.8))
        import batch9
        batch9.glow_png(a, gloed, os.path.join(d, "kwal_glow.png"))
        batch9.glow_png(a, gloed, os.path.join(d, "kwal_baby_glow.png"))      # de baby deelt de atlas (zelfde uv)
    elif soort == "kogelvis":
        a = Atlas(1330); maak_soort(d, soort, kogelvis(a), a, kogelvis_anims(), (0.6, 0.8))
    elif soort == "zeester":
        a = Atlas(1340); maak_soort(d, soort, zeester(a), a, zeester_anims(), (0.6, 0.8))
    elif soort == "zee_egel":
        a = Atlas(1350); maak_soort(d, soort, zee_egel(a), a, zee_egel_anims(), (0.6, 0.8))
    elif soort == "babyhaai":
        a = Atlas(1360); maak_soort(d, soort, babyhaai(a), a, vis_anims(1.4, ["vin_left", "vin_right"]), (0.6, 0.8))
    elif soort == "regenboogvis":
        a = Atlas(1370); maak_soort(d, soort, regenboogvis(a), a, vis_anims(0.6), (0.6, 0.8))
    else:
        raise SystemExit(f"onbekende soort {soort}")


def vellen(soorten, overzicht=True):
    b12.vellen_gedeeld(soorten, SOORTEN, NAMEN, 13, "Kriebelhuis, Aquariumtunnel", UIT, overzicht)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    b12.alles(sys.modules[__name__], args or SOORTEN, met_vellen="--geen-vellen" not in sys.argv)
