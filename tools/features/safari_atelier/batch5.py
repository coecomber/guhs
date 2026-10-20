"""
Safari-voorwerk, batch 5 (IJskust + Oerijstijd): ijsbeer, zeehond, sabeltandtijger, mammoet, wolharige neushoorn.
Zelfde stijl, werkwijze en formaat als batch 1 (dieren.py); gebruikt atelier/dieren/blad alleen als import.

    python safari-ontwerp/dieren/_bron/batch5.py [soort ...] [geenvel]

Schrijft per soort in safari-ontwerp/dieren/<soort>/ (basis = vrouwtje, _man = mannetje waar ze verschillen, _baby = jong):
    <soort>[_man|_baby].geo.json + .png, <soort>.animation.json, <soort>.bbmodel, <soort>_voorstel.png
en safari-ontwerp/dieren/batch5_overzicht.png.
"""
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, gezicht, neusje, vorm_oor, transparent  # noqa: E402
import dieren as d1                                                     # noqa: E402
from dieren import baby, paar, vier_poten, ZIJ_LEEG, POTEN4, NEUS       # noqa: E402

UIT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "build", "safari_atelier"))  # staging; tools/features/safari_dieren.py copies into the assets
NAMEN = {"ijsbeer": "IJsbeer", "zeehond": "Zeehond", "sabeltandtijger": "Sabeltandtijger", "mammoet": "Mammoet",
         "wolharige_neushoorn": "Wolharige neushoorn"}
SOORTEN = list(NAMEN)


# =====================================================================================================================
# hulpjes
# =====================================================================================================================
def schaal(geo_, f, naam):
    """Hetzelfde model f keer zo groot (het mannetje van de ijsbeer), met een eigen identifier."""
    g = baby(geo_, f, f)
    gg = g["minecraft:geometry"][0]
    gg["description"]["identifier"] = f"geometry.{naam}"
    return g


def wol(colour, donker, var=12):
    """Lange wol: pluizig met korte donkere en lichte haarstreepjes van boven naar onder."""
    def f(W, H, rng, a):
        for _ in range(W * H // 40):
            x, y = int(rng.uniform(0, W)), int(rng.uniform(0, H))
            L = int(rng.uniform(2, 6))
            c = donker if rng.random() < 0.6 else np.clip(np.array(colour) + 22, 0, 255)
            a[y:y + L, x, :3] = c
    return plain(colour, var, f)


def strepen(base, streep, n=5, var=8):
    """Vage tijgerstrepen dwars over het vlak (verticaal), licht golvend."""
    def f(W, H, rng, a):
        for i in range(n):
            x0 = W * (i + 0.5) / n + rng.uniform(-W * 0.04, W * 0.04)
            for y in range(H):
                x = int(x0 + np.sin(y / max(1, H) * 3.1) * W * 0.03)
                w = max(1, int(W * 0.035))
                a[y, max(0, x - w):min(W, x + w), :3] = a[y, max(0, x - w):min(W, x + w), :3] * 0.4 + np.array(streep) * 0.6
    return plain(base, var, f)


def vlekjes(base, vlek, dichtheid=55, var=8):
    """Zeehondvlekjes: kleine onregelmatige donkere stippen."""
    def f(W, H, rng, a):
        for _ in range(max(1, W * H // (dichtheid * 16))):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(1.2, 2.6) * at.S / 4 * 2, vlek, 0.85)
    return plain(base, var, f)


def pootkussen(fur, kussen):
    """Onderkant van een poot: een donker kussentje."""
    return plain(fur, 6, lambda W, H, rng, a: disc(a, W / 2, H / 2, W * 0.36, kussen, 1.0, ry=H * 0.36))


# =====================================================================================================================
# ijsbeer
# =====================================================================================================================
IJ = (246, 244, 236)
IJ_D = (224, 220, 206)
IJ_L = (255, 253, 246)
IJ_OOR = (236, 196, 200)
KUSSEN = (70, 64, 72)


def ijsbeer(atlas):
    wit = at.fluffy(IJ, 10)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 9, 0], [cube(atlas, [-7, 5.5, -9], [14, 12, 18], wit, {"down": plain(IJ_D, 8)})])]
    bones += vier_poten(atlas, "body", 2.2, -8, 4, [4.5, 6.5, 4.5], wit, 6.5, {"down": pootkussen(IJ, KUSSEN)})
    bones.append(bone("head", "body", [0, 14, -9], [
        cube(atlas, [-6, 10, -17.5], [12, 10, 9], wit, {"north": gezicht(IJ, eye_y=0.4, eye_r=0.16)}),
        cube(atlas, [-3, 10.4, -19.9], [6, 4.4, 2.4], plain(IJ_L, 6), {"north": neusje(IJ_L, NEUS)})]))
    oor = vorm_oor(IJ, IJ_OOR)
    bones += paar(atlas, "ear", "head", [4.6, 20, -12.8], [3.2, 19.4, -13.3], [3, 3, 1.2], plain(IJ, 6), dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(bone("tail", "body", [0, 14, 9], [cube(atlas, [-1.5, 12.5, 9], [3, 3, 2], wit)]))
    return geo("ijsbeer", bones, atlas, 1.7, 1.5)


def ijsbeer_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail")


# =====================================================================================================================
# zeehond (+ witte pluizige pup)
# =====================================================================================================================
ZH = (168, 176, 188)
ZH_V = (118, 126, 142)
ZH_L = (214, 220, 228)
PUP = (240, 240, 242)
PUP_L = (252, 252, 252)


def zeehond(atlas, pup=False):
    if pup:
        lijf, licht, kopf = at.fluffy(PUP, 12), plain(PUP_L, 6), PUP
    else:
        lijf, licht, kopf = vlekjes(ZH, ZH_V), plain(ZH_L, 6), ZH
    vin = plain(ZH_V if not pup else (210, 210, 216), 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4, 0], [cube(atlas, [-5, 0, -5], [10, 8, 13], lijf, {"down": licht}),
                                              cube(atlas, [-3.5, 0.3, 8], [7, 5, 5], lijf, {"down": licht})])]
    bones.append(bone("head", "body", [0, 7, -5], [
        cube(atlas, [-4.5, 3.5, -12.5], [9, 8.5, 8], lijf, {"north": gezicht(kopf, eye_y=0.4, eye_dx=0.25, eye_r=0.19)}),
        cube(atlas, [-2.5, 3.8, -14.1], [5, 3.4, 1.6], licht, {"north": neusje(ZH_L if not pup else PUP_L, NEUS)})]))
    bones += paar(atlas, "flipper", "body", [5, 1, -2], [5, 0, -3.5], [3.5, 0.8, 3.5], vin, rotation=[0, -20, 0])
    bones.append(bone("tail", "body", [0, 1, 13], [cube(atlas, [-3.6, 0.4, 13], [3.2, 0.8, 4], vin),
                                                   cube(atlas, [0.4, 0.4, 13], [3.2, 0.8, 4], vin)]))
    return geo("zeehond_baby" if pup else "zeehond", bones, atlas, 1.1, 0.8)


def zeehond_anims():
    L = 0.8
    hobbel = {"body": {"position": at.kf((0, [0, 0, 0]), (L / 4, [0, 0.8, 0]), (L / 2, [0, 0, 0]), (3 * L / 4, [0, 0.8, 0]), (L, [0, 0, 0])),
                       "rotation": at.kf((0, [-5, 0, 0]), (L / 2, [5, 0, 0]), (L, [-5, 0, 0]))},
              "flipper_left": {"rotation": at.kf((0, [0, 15, 0]), (L / 2, [0, -15, 0]), (L, [0, 15, 0]))},
              "flipper_right": {"rotation": at.kf((0, [0, -15, 0]), (L / 2, [0, 15, 0]), (L, [0, -15, 0]))}}
    return at.animaties(staart="tail", extra_walk=hobbel,
                        extra_idle={"flipper_left": {"rotation": at.kf((0, [0, 0, 0]), (2.2, [0, 0, 0]), (2.4, [0, 0, 20]), (2.6, [0, 0, 0]))}})


# =====================================================================================================================
# sabeltandtijger (lieve kleine sabeltandjes)
# =====================================================================================================================
ST = (214, 168, 108)
ST_D = (150, 104, 60)
ST_L = (250, 232, 204)
ST_OOR = (240, 180, 170)
TANDJE = (252, 250, 240)


def sabeltandtijger(atlas):
    vacht = strepen(ST, ST_D, n=5)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 10, 0], [cube(atlas, [-5, 6, -7], [10, 9, 15], vacht,
                                                    {"down": plain(ST_L, 6), "north": plain(ST, 8), "south": plain(ST, 8)})])]
    bones += vier_poten(atlas, "body", 1.4, -6.5, 4, [3.5, 7, 3.5], plain(ST, 8), 7, {"down": pootkussen(ST, (120, 80, 70))})

    def voorhoofd(W, H, rng, a):
        for dx in (-0.12, 0, 0.12):
            a[int(H * 0.04):int(H * 0.16), int(W * (0.5 + dx) - W * 0.02):int(W * (0.5 + dx) + W * 0.02), :3] = ST_D
    bones.append(bone("head", "body", [0, 13, -7], [
        cube(atlas, [-5.5, 10, -15.5], [11, 9.5, 8.5], plain(ST, 8), {"north": gezicht(ST, eye_y=0.44, eye_r=0.16, spots=voorhoofd)}),
        cube(atlas, [-3, 10.4, -17.3], [6, 4, 1.8], plain(ST_L, 6), {"north": neusje(ST_L, (150, 80, 90))})]))
    bones += paar(atlas, "tooth", "head", [1.3, 10.4, -16.6], [0.8, 8.3, -17], [1, 2.2, 1], plain(TANDJE, 4))
    oor = vorm_oor(ST, ST_OOR)
    bones += paar(atlas, "ear", "head", [4.4, 19.4, -12], [3.1, 19, -12.5], [2.6, 2.6, 1], plain(ST, 6), dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(bone("tail", "body", [0, 14, 8], [cube(atlas, [-1, 12.5, 8], [2, 2, 3], plain(ST, 6), {"south": plain(ST_D, 6)})]))
    return geo("sabeltandtijger", bones, atlas, 1.3, 1.3)


def sabeltandtijger_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail")


# =====================================================================================================================
# mammoet (wollig bruin, hoge koepelkop, gekrulde slagtanden, kleine oortjes)
# =====================================================================================================================
MA = (124, 82, 54)
MA_D = (88, 56, 38)
MA_L = (164, 116, 80)
MA_SLURF = (106, 72, 50)
IVOOR = (250, 240, 214)


def tand(atlas, side, sx, man):
    """Een gekrulde slagtand: een basisstuk naar voren-omlaag en een punt die omhoog en naar binnen krult."""
    dik = 2.2 if man else 1.6
    L1, L2 = (8, 6.5) if man else (5, 4)
    x0 = 3.2 if sx > 0 else -3.2 - dik
    basis = bone(f"tusk_{side}", "head", [sx * (3.2 + dik / 2), 12, -17.5],
                 [cube(atlas, [x0, 11, -17.5 - L1], [dik, dik, L1], plain(IVOOR, 6))], rotation=[22, 0, 0])
    punt = bone(f"tusk_tip_{side}", f"tusk_{side}", [sx * (3.2 + dik / 2), 11 + dik / 2, -17.5 - L1 + dik / 2],
                [cube(atlas, [x0, 11, -17.5 - L1], [dik, L2, dik], plain(IVOOR, 6))], rotation=[-30, 0, -sx * 18])
    return [basis, punt]


def mammoet(atlas, man=False):
    w = wol(MA, MA_D)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 12, 0], [cube(atlas, [-9, 9, -10], [18, 15, 20], w, {"down": plain(MA_D, 8)}),
                                               cube(atlas, [-6, 24, -9.5], [12, 3.5, 9], w),
                                               cube(atlas, [-9.6, 7.4, -10.6], [19.2, 3.2, 21.2], wol(MA_D, (64, 40, 28)),
                                                    {"up": transparent, "down": plain(MA_D, 8)})])]
    bones += vier_poten(atlas, "body", 2.5, -8.5, 4, [5.5, 9, 5.5], wol(MA, MA_D), 9, voet=(1.4, plain(MA_D, 6)))
    bones.append(bone("head", "body", [0, 18, -9], [
        cube(atlas, [-7.5, 13, -17.5], [15, 13, 8.5], w, {"north": gezicht(MA, eye_y=0.44, eye_dx=0.26, eye_r=0.15)}),
        cube(atlas, [-5, 26, -16], [10, 4, 6.5], w)]))
    oor = vorm_oor(MA, MA_D)
    for side, sx in (("left", 1), ("right", -1)):
        ov = {"east": oor, "west": oor, "north": transparent, "south": transparent, "up": transparent, "down": transparent}
        o = [7.2, 19, -14.5] if sx > 0 else [-8.2, 19, -14.5]
        bones.append(bone(f"ear_{side}", "head", [sx * 7.7, 21, -12.7], [cube(atlas, o, [1, 4, 3.5], plain(MA, 6), ov)], rotation=[0, sx * 20, 0]))
    bones.append(bone("trunk", "head", [0, 15, -17.5], [cube(atlas, [-2.5, 7.5, -20.5], [5, 7.5, 3], wol(MA_SLURF, MA_D))]))
    bones.append(bone("trunk_tip", "trunk", [0, 8, -19], [cube(atlas, [-2, 3, -20.5], [4, 5, 3], plain(MA_SLURF, 8)),
                                                         cube(atlas, [-2.2, 2.2, -22.6], [4.4, 2, 3], plain(MA_SLURF, 8),
                                                              {"north": neusje((150, 110, 90), (70, 50, 46), mond=False)})],
                      rotation=[-25, 0, 0]))
    for side, sx in (("left", 1), ("right", -1)):
        bones += tand(atlas, side, sx, man)
    bones.append(bone("tail", "body", [0, 21, 10], [cube(atlas, [-0.5, 15, 10], [1, 6, 1], plain(MA, 6)),
                                                    cube(atlas, [-1, 13.5, 9.75], [2, 2, 1.5], plain(MA_D, 6))]))
    return geo("mammoet_man" if man else "mammoet", bones, atlas, 2.4, 2.4)


def mammoet_anims():
    zwaai = {"trunk": {"rotation": at.kf((0, [0, 0, 0]), (1.5, [-8, 0, 6]), (3.0, [0, 0, 0]))}}
    return at.animaties(legs4=POTEN4, oren=True, staart="tail", extra_idle=zwaai)


# =====================================================================================================================
# wolharige neushoorn
# =====================================================================================================================
WN = (150, 104, 70)
WN_D = (104, 70, 46)
WN_L = (196, 156, 120)
HOORN = (214, 204, 182)
HOORN_D = (170, 158, 136)


def wolharige_neushoorn(atlas, man=False):
    w = wol(WN, WN_D)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 10, 0], [cube(atlas, [-7.5, 7, -10], [15, 12, 19], w, {"down": plain(WN_D, 8)}),
                                               cube(atlas, [-5, 19, -9], [10, 3, 8], w)])]
    bones += vier_poten(atlas, "body", 2.2, -8.5, 4.5, [4.5, 7.5, 4.5], wol(WN_D, (80, 52, 34)), 7.5, voet=(1.2, plain((70, 50, 40), 6)))
    h = 6.5 if man else 4.5
    hoofd = [cube(atlas, [-5.5, 8, -19], [11, 10, 10.5], w, {"north": gezicht(WN, eye_y=0.36, eye_r=0.16)}),
             cube(atlas, [-4, 8.3, -21.2], [8, 5, 2.2], plain(WN_L, 6), {"north": neusje(WN_L, (70, 46, 40))})]
    bones.append(bone("head", "body", [0, 14, -9], hoofd, rotation=[10, 0, 0]))
    bones.append(bone("horn", "head", [0, 13.3, -20.4], [cube(atlas, [-1.2, 13.3, -21.6], [2.4, h, 2.4], at.verloop(HOORN_D, HOORN))],
                      rotation=[28, 0, 0]))
    bones.append(bone("horn_back", "head", [0, 18, -17], [cube(atlas, [-0.9, 18, -17.9], [1.8, 2.4 if man else 1.8, 1.8], plain(HOORN, 6))],
                      rotation=[15, 0, 0]))
    oor = vorm_oor(WN, WN_D, punt=0.4)
    bones += paar(atlas, "ear", "head", [4.2, 18, -11.5], [3.4, 17.6, -12], [2.4, 3, 1], plain(WN, 6), dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(bone("tail", "body", [0, 17, 9], [cube(atlas, [-0.5, 12, 9], [1, 5, 1], plain(WN, 6)),
                                                   cube(atlas, [-1, 10.5, 8.75], [2, 2, 1.5], plain(WN_D, 6))]))
    return geo("wolharige_neushoorn_man" if man else "wolharige_neushoorn", bones, atlas, 1.9, 1.6)


def wolharige_neushoorn_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail")


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "ijsbeer":
        a = Atlas(510); g = ijsbeer(a); anims = ijsbeer_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_man", schaal(g, 1.12, "ijsbeer_man"), a)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8), a)
    elif soort == "zeehond":
        a = Atlas(520); g = zeehond(a); anims = zeehond_anims()
        img = at.schrijf(d, soort, g, a, anims)
        ap = Atlas(521); gp = zeehond(ap, pup=True)
        gp = baby(gp, 0.62, 0.82); gp["minecraft:geometry"][0]["description"]["identifier"] = "geometry.zeehond_baby"
        at.schrijf(d, soort + "_baby", gp, ap)
    elif soort == "sabeltandtijger":
        a = Atlas(530); g = sabeltandtijger(a); anims = sabeltandtijger_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8, drop=("tooth_left", "tooth_right")), a)
    elif soort == "mammoet":
        a = Atlas(540, 128); g = mammoet(a); anims = mammoet_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(540, 128); at.schrijf(d, soort + "_man", mammoet(am, man=True), am)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.78, drop=("tusk_left", "tusk_right", "tusk_tip_left", "tusk_tip_right")), a)
    elif soort == "wolharige_neushoorn":
        a = Atlas(550); g = wolharige_neushoorn(a); anims = wolharige_neushoorn_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(550); at.schrijf(d, soort + "_man", wolharige_neushoorn(am, man=True), am)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8), a)
    else:
        raise SystemExit(f"onbekende soort {soort}")
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort)


# =====================================================================================================================
# voorstelvellen (kopie van blad.vel / blad.overzicht met eigen titels; blad.py zelf blijft onaangeroerd)
# =====================================================================================================================
def vellen(soorten, overzicht=True):
    cwd = os.getcwd()
    import blad as bl                                                   # (zet de cwd naar guhs/)
    try:
        bl.NAMEN.update(NAMEN)
        for s in soorten:
            vel(bl, s)
        if overzicht:
            overzicht_vel(bl, SOORTEN)
    finally:
        os.chdir(cwd)


def vel(bl, soort):
    heeft_man = os.path.exists(os.path.join(UIT, soort, f"{soort}_man.geo.json"))
    varianten = [("", "volwassen" + (" (vrouwtje)" if heeft_man else ""))]
    if heeft_man:
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
    dr = ImageDraw.Draw(img)
    dr.text((20, 18), f"{NAMEN[soort]}  (safari-dieren, batch 5)", font=bl.font(30, True), fill=(40, 30, 40))
    dr.text((20, 56), "voor / zij / schuin, en schuin naast een guh (zelfde schaal). Placeholders: animaties volgen in de game.",
            font=bl.font(16), fill=(80, 70, 80))
    for c, (lab, _, _) in enumerate(bl.VIEWS + [("naast een guh", 0, 0)]):
        dr.text((200 + c * CW + 10, 88), lab, font=bl.font(16, True), fill=(90, 80, 90))
    combs = {}
    ppb_g = ppb * 0.8
    for v, _ in varianten:
        dier = bl.dier_quads(soort, v)
        combs[v] = bl.naast(dier, bl.guh_quads(), dx=-(bl.xbreed(dier) / 2 + bl.xbreed(guh) / 2 + 0.3))
        sx, sy = bl.span(combs[v], 35, -18)
        ppb_g = min(ppb_g, (GW - 30) / sx, (CH - 50) / sy)
    for r, (v, lab) in enumerate(varianten):
        y = 110 + r * CH
        dr.text((20, y + CH / 2 - 12), lab, font=bl.font(20, True), fill=(50, 40, 50))
        for c, (_, yaw, pitch) in enumerate(bl.VIEWS):
            img.alpha_composite(bl.render_vast(qs[v], yaw, pitch, ppb, CW, CH - 20), (200 + c * CW, y))
        img.alpha_composite(bl.render_vast(combs[v], 35, -18, ppb_g, GW, CH - 20), (200 + 3 * CW, y))
    pad = os.path.join(UIT, soort, f"{soort}_voorstel.png")
    img.convert("RGB").save(pad)
    print(pad)


def overzicht_vel(bl, soorten):
    guh = bl.guh_quads()
    x = 0.0
    quads = []
    for naam, q, qb in [("guh", guh, None)] + [(s, bl.dier_quads(s), bl.dier_quads(s, "baby")) for s in soorten]:
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
    dr = ImageDraw.Draw(img)
    dr.text((20, 18), "Safari-dieren batch 5 (IJskust + Oerijstijd) naast een guh (volwassen achter, jong ervoor)", font=bl.font(28, True), fill=(40, 30, 40))
    dr.text((20, 56), "IJsbeer · Zeehond · Sabeltandtijger · Mammoet · Wolharige neushoorn (van links naar rechts, na de guh)", font=bl.font(17), fill=(80, 70, 80))
    pad = os.path.join(UIT, "batch5_overzicht.png")
    img.convert("RGB").save(pad)
    print(pad)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if a != "geenvel"]
    soorten = args or SOORTEN
    for s in soorten:
        maak(s)
    if "geenvel" not in sys.argv:
        vellen(soorten, overzicht=not args)
