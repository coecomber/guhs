"""
Safari-voorwerk, batch 4 (regenwoud + bamboe): reuzenpanda, luiaard, okapi, orang-oetan, toekan.
Zelfde stijl, werkwijze en bestandsformaat als batch 1 (dieren.py); gebruikt atelier/dieren/blad alleen als import.

    python safari-ontwerp/dieren/_bron/batch4.py [soort ...] [geen-vellen]     (geen soort: alle vijf + vellen + overzicht)

Per soort in safari-ontwerp/dieren/<soort>/: <soort>.geo.json/.png (vrouwtje, of beide als ze gelijk zijn),
<soort>_man.* (alleen orang-oetan: wangflappen), <soort>_baby.*, <soort>.animation.json (idle, walk, blij, eet;
luiaard + hang, toekan + vlieg), <soort>.bbmodel, <soort>_voorstel.png. Plus dieren/batch4_overzicht.png.
"""
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, gezicht, neusje, vorm_oor, transparent  # noqa: E402
import dieren as d1                                                     # noqa: E402  (baby, paar, vier_poten, ...)

UIT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "build", "safari_atelier"))  # staging; tools/features/safari_dieren.py copies into the assets
NEUS = d1.NEUS
POTEN4 = d1.POTEN4
ZIJ_LEEG = d1.ZIJ_LEEG


# =====================================================================================================================
# reuzenpanda (hoort naast de rode panda, maar groter en zwart-wit; extra rond)
# =====================================================================================================================
RZ_WIT = (246, 244, 240)
RZ_ZW = (42, 40, 48)
RZ_OOR_IN = (76, 70, 80)


def reuzenpanda(atlas):
    wit = at.fluffy(RZ_WIT, 10)
    zwart = plain(RZ_ZW, 8)

    def ooglappen(W, H, rng, a):
        for sx in (-1, 1):
            disc(a, W * (0.5 + sx * 0.25), H * 0.47, W * 0.17, RZ_ZW, 1.0, ry=H * 0.2)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 11, 0], [
                 cube(atlas, [-8, 5, -8], [16, 13, 17], wit, {"down": zwart}),
                 # de zwarte 'schouderband' over de voorkant van het lijf
                 cube(atlas, [-8.3, 8, -8.3], [16.6, 10.3, 6], zwart)])]
    bones += d1.vier_poten(atlas, "body", 2.3, -7.5, 3.5, [5, 6, 5], zwart, 6)
    bones.append(bone("head", "body", [0, 15, -8], [
        cube(atlas, [-7, 10.5, -18], [14, 12, 11], wit, {"north": gezicht(RZ_WIT, eye_y=0.47, eye_dx=0.25, eye_r=0.14, spots=ooglappen)}),
        cube(atlas, [-3, 10.8, -19.6], [6, 4, 1.6], plain(RZ_WIT, 6), {"north": neusje(RZ_WIT, NEUS)})]))
    oor = vorm_oor(RZ_ZW, RZ_OOR_IN)
    bones += d1.paar(atlas, "ear", "head", [5.5, 22, -12.5], [4, 21.2, -13], [4, 4, 1.5], zwart, dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(bone("tail", "body", [0, 15, 9], [cube(atlas, [-1.5, 13, 9], [3, 3, 1.5], wit)]))
    return geo("reuzenpanda", bones, atlas, 1.9, 1.7)


def reuzenpanda_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail")


# =====================================================================================================================
# luiaard (extra rond; lange armen met lichte klauwtjes; + 'hang': ondersteboven)
# =====================================================================================================================
LU = (170, 140, 106)
LU_L = (234, 216, 186)
LU_D = (98, 74, 58)
KLAUW = (236, 226, 204)


def luiaard(atlas):
    vacht = at.fluffy(LU, 14)

    def masker(W, H, rng, a):
        disc(a, W * 0.5, H * 0.55, W * 0.42, LU_L, 1.0, ry=H * 0.4)
        for sx in (-1, 1):                                            # de donkere oogstreep naar opzij
            disc(a, W * (0.5 + sx * 0.33), H * 0.44, W * 0.2, LU_D, 1.0, ry=H * 0.1)

    def klauwen(W, H, rng, a):
        for i in (-1, 1):
            disc(a, W / 2 + i * W * 0.22, H * 0.92, W * 0.13, KLAUW, 1.0, ry=H * 0.07)
    poot = plain(LU, 10, klauwen)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 11, 0], [cube(atlas, [-5.5, 6.5, -5.5], [11, 10, 12], vacht,
                                                    {"north": at.buik(LU, LU_L, cy=0.6), "down": plain(LU_L, 8)})])]
    # lange voorpoten (armen), kortere achterpoten
    for voor, z, size, top in (("front", -4.5, [2.6, 8.5, 2.6], 8.5), ("back", 2.5, [2.6, 7, 2.6], 7)):
        for side, sx in (("left", 1), ("right", -1)):
            x = 4.2 if voor == "front" else 2.6
            o = [x, 0, z] if sx > 0 else [-x - size[0], 0, z]
            bones.append(bone(f"leg_{voor}_{side}", "body", [o[0] + size[0] / 2, top, o[2] + size[2] / 2],
                              [cube(atlas, o, size, vacht, {"north": poot, "south": poot})]))
    bones.append(bone("head", "body", [0, 13, -5], [
        cube(atlas, [-5, 10.5, -13], [10, 9, 8], vacht, {"north": gezicht(LU, eye_y=0.44, eye_dx=0.27, eye_r=0.15, spots=masker)}),
        cube(atlas, [-2, 11, -14.3], [4, 3, 1.3], plain(LU_L, 6), {"north": neusje(LU_L, NEUS)})]))
    bones.append(bone("tail", "body", [0, 8, 6.5], [cube(atlas, [-1.5, 7, 6.5], [3, 2.5, 1.5], vacht)]))
    return geo("luiaard", bones, atlas, 1.3, 1.3)


def luiaard_anims():
    an = at.animaties(legs4=POTEN4, staart="tail")
    # hang: ondersteboven aan een tak (poten omhoog), langzaam heen en weer schommelen
    an["animations"]["hang"] = {"loop": True, "animation_length": 4.0, "bones": {
        "root": {"rotation": at.kf((0, [0, 0, 180]), (2.0, [0, 0, 186]), (4.0, [0, 0, 180])),
                 "position": at.kf((0, [0, 17, 0]), (4.0, [0, 17, 0]))},
        "head": {"rotation": at.kf((0, [0, 0, 0]), (2.0, [-14, 8, 0]), (4.0, [0, 0, 0]))}}}
    return an


# =====================================================================================================================
# okapi (schuw; chocoladebruin met zebrapootjes en -billen, licht gezicht; geen ossicones: geen m/v-verschil)
# =====================================================================================================================
OK = (84, 46, 42)
OK_L = (238, 224, 202)
OK_D = (52, 30, 30)
OK_STREEP = (34, 24, 26)


def okapi(atlas):
    bruin = plain(OK, 9)

    def billen(kant):
        def f(W, H, rng, a):
            n = int(W * 0.38)
            sl = slice(W - n, W) if kant == "west" else slice(0, n)     # de achterkant (+z) van het zijvlak
            for i in range(5):
                y0, y1 = int(H * (0.12 + i * 0.18)), int(H * (0.12 + i * 0.18 + 0.09))
                a[y0:y1, sl, :3] = OK_L
        return plain(OK, 9, f)
    streep = at.ringen(OK_L, OK_STREEP, langs="H", n=5, var=6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 16, 0], [cube(atlas, [-5, 12, -8], [10, 9, 15], bruin,
                                                    {"east": billen("east"), "west": billen("west"), "south": at.ringen(OK, OK_L, "H", n=4)})])]
    bones += d1.vier_poten(atlas, "body", 1.5, -7, 3.5, [3, 12.5, 3], streep, 12.5,
                           overrides={"up": plain(OK, 6)}, voet=(1.4, plain(OK_D, 6)))
    bones.append(bone("hals", "body", [0, 20, -6], [cube(atlas, [-2.5, 18, -9.5], [5, 9, 4.5], bruin)], rotation=[10, 0, 0]))
    gezichtje = gezicht(OK_L, eye_y=0.42, eye_r=0.17,
                        spots=lambda W, H, rng, a: disc(a, W * 0.5, H * 0.04, W * 0.4, OK, 1.0, ry=H * 0.12))
    bones.append(bone("head", "hals", [0, 26, -8], [
        cube(atlas, [-4, 25, -14.5], [8, 7.5, 8], plain(OK, 8), {"north": gezichtje, "east": at.buik(OK, OK_L, cx=0.3, cy=0.6),
                                                                 "west": at.buik(OK, OK_L, cx=0.7, cy=0.6)}),
        cube(atlas, [-2.5, 25.2, -16.6], [5, 4, 2.1], plain(OK_D, 6), {"north": neusje(OK_D, (20, 14, 18))})]))
    oor = vorm_oor(OK, (226, 168, 168), punt=0.3)
    bones += d1.paar(atlas, "ear", "head", [4, 31.5, -10], [3.4, 30.6, -10.5], [4, 4, 1], plain(OK, 6),
                     dict(ZIJ_LEEG, north=oor, south=oor), rotation=[0, 0, -20])
    bones.append(bone("tail", "body", [0, 20.5, 7], [cube(atlas, [-0.5, 13.5, 7], [1, 7, 1], bruin),
                                                     cube(atlas, [-1, 12, 6.75], [2, 2, 1.5], plain(OK_D, 6))]))
    return geo("okapi", bones, atlas, 1.3, 2.2)


def okapi_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail",
                        extra_idle={"hals": {"rotation": at.kf((0, [0, 0, 0]), (1.5, [5, 0, 2]), (3.0, [0, 0, 0]))}})


# =====================================================================================================================
# orang-oetan (lang oranje haar, lange armen; mannetje: wangflappen)
# =====================================================================================================================
OR = (200, 94, 44)
OR_D = (150, 64, 30)
OR_HUID = (196, 146, 118)
OR_HUID_D = (126, 88, 74)


def orang_oetan(atlas, man=False):
    haar = at.fluffy(OR, 16)

    def gezichtshuid(W, H, rng, a):
        disc(a, W * 0.5, H * 0.58, W * 0.42, OR_HUID, 1.0, ry=H * 0.44)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 12, 0], [cube(atlas, [-6, 8, -5], [12, 12, 10], haar,
                                                    {"north": at.buik(OR, OR_HUID, cy=0.55, rx=0.3, ry=0.32)})])]
    # lange armen buiten het lijf, korte achterpoten eronder (knokkelloop)
    for voor, z, size, x in (("front", -4.5, [3.4, 15, 3.4], 6), ("back", 1.5, [3.2, 8.5, 3.2], 2)):
        for side, sx in (("left", 1), ("right", -1)):
            o = [x, 0, z] if sx > 0 else [-x - size[0], 0, z]
            hand = plain(OR_HUID_D, 6)
            bones.append(bone(f"leg_{voor}_{side}", "body", [o[0] + size[0] / 2, size[1], o[2] + size[2] / 2],
                              [cube(atlas, o, size, haar, {"down": hand}),
                               cube(atlas, [o[0] + 0.2, 0, o[2] - 0.6], [size[0] - 0.4, 1.4, 1.4], hand)]))
    bones.append(bone("head", "body", [0, 19, -4], [
        cube(atlas, [-5, 17, -11.5], [10, 9.5, 8], haar, {"north": gezicht(OR, eye_y=0.45, eye_dx=0.24, eye_r=0.15, spots=gezichtshuid)}),
        cube(atlas, [-2.5, 17.4, -12.8], [5, 3.2, 1.3], plain(OR_HUID, 6), {"north": neusje(OR_HUID, OR_HUID_D)}),
        cube(atlas, [-4, 25.5, -9.5], [8, 2, 6], at.fluffy(OR_D, 14))]))            # een pluk haar bovenop
    if man:
        wang = plain(OR_HUID_D, 8, lambda W, H, rng, a: disc(a, W * 0.5, H * 0.5, W * 0.32, (150, 106, 90), 1.0, ry=H * 0.38))
        bones += d1.paar(atlas, "cheek", "head", [5.5, 21.5, -11], [4.6, 17.2, -11.3], [3.6, 9, 1.4], plain(OR_HUID_D, 8),
                         {"north": wang}, rotation=[0, -14, 0])
    return geo("orang_oetan_man" if man else "orang_oetan", bones, atlas, 1.6, 1.8)


def orang_oetan_anims():
    zwaai = {"leg_front_left": {"rotation": at.kf((0, [0, 0, 0]), (2.2, [0, 0, 0]), (2.45, [-60, 0, -20]), (2.7, [-50, 0, -10]), (3.0, [0, 0, 0]))}}
    return at.animaties(legs4=POTEN4, extra_idle=zwaai)


# =====================================================================================================================
# toekan (zwart, gele keel, grote kleurrijke snavel; + 'vlieg')
# =====================================================================================================================
TK = (32, 32, 40)
TK_KEEL = (255, 232, 110)
TK_OOGHUID = (110, 196, 236)
SN_GEEL = (255, 196, 48)
SN_ORANJE = (255, 128, 36)
SN_ROOD = (226, 52, 52)
SN_GROEN = (110, 186, 70)
TK_POOT = (110, 140, 190)


def toekan(atlas):
    zwart = plain(TK, 8)

    def ooghuid(W, H, rng, a):
        for sx in (-1, 1):
            disc(a, W * (0.5 + sx * 0.27), H * 0.33, W * 0.22, TK_OOGHUID, 1.0, ry=H * 0.24)
    snavel_zij = at.verloop(SN_GEEL, SN_ORANJE)

    rug = plain(SN_GROEN, 6)                                  # groene snavelrug bovenop
    bones = [bone("root", None, [0, 0, 0], [])]
    for side, sx in (("left", 1), ("right", -1)):
        x = 0.7 if sx > 0 else -1.9
        bones.append(bone(f"leg_{side}", "root", [sx * 1.3, 4, 0], [
            cube(atlas, [x, 0.5, -0.5], [1.2, 3.8, 1.2], plain(TK_POOT, 6)),
            cube(atlas, [x - 0.6, 0, -1.8], [2.4, 0.6, 2.6], plain(TK_POOT, 6))]))
    bones.append(bone("body", "root", [0, 8, 0], [
        cube(atlas, [-3.5, 4, -4], [7, 8, 8], zwart),
        cube(atlas, [-3, 6, -4.6], [6, 5.5, 1], plain(TK_KEEL, 6), {"east": plain(TK_KEEL, 4), "west": plain(TK_KEEL, 4)}),
        cube(atlas, [-2, 3.6, 3.5], [4, 1.5, 1.5], plain(SN_ROOD, 6))]))           # rood onder de staart
    bones.append(bone("tail", "body", [0, 6, 4], [cube(atlas, [-2, 5, 4], [4, 1.5, 6], zwart)], rotation=[-25, 0, 0]))
    bones += d1.paar(atlas, "wing", "body", [3.5, 11, 0], [3.5, 5, -3], [1, 6.5, 7], zwart)
    bones.append(bone("head", "body", [0, 12, -2], [
        cube(atlas, [-3.5, 11.5, -6.5], [7, 7, 7], zwart, {"north": gezicht(TK, eye_y=0.33, eye_dx=0.27, eye_r=0.15, blush=False, spots=ooghuid)})]))
    bones.append(bone("snavel", "head", [0, 13.5, -6.5], [
        cube(atlas, [-1.75, 11.6, -14], [3.5, 3.4, 7.5], snavel_zij, {"up": rug, "north": plain(SN_ROOD, 6)}),
        cube(atlas, [-1.25, 11.8, -15.4], [2.5, 2.4, 1.4], plain(SN_ROOD, 6))]))
    return geo("toekan", bones, atlas, 1.0, 1.3)


def toekan_anims():
    an = at.animaties(legs2=["leg_left", "leg_right"], staart="tail",
                      extra_idle={"snavel": {"rotation": at.kf((0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.2, [-8, 0, 0]), (2.4, [0, 0, 0]))}})
    F = 0.3
    an["animations"]["vlieg"] = {"loop": True, "animation_length": F, "bones": {
        "wing_left": {"rotation": at.kf((0, [0, 0, -10]), (F / 2, [0, 0, -75]), (F, [0, 0, -10]))},
        "wing_right": {"rotation": at.kf((0, [0, 0, 10]), (F / 2, [0, 0, 75]), (F, [0, 0, 10]))},
        "body": {"position": at.kf((0, [0, 4, 0]), (F / 2, [0, 5, 0]), (F, [0, 4, 0]))},
        "leg_left": {"rotation": at.kf((0, [70, 0, 0]), (F, [70, 0, 0]))},
        "leg_right": {"rotation": at.kf((0, [70, 0, 0]), (F, [70, 0, 0]))}}}
    return an


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "reuzenpanda":
        a = Atlas(410); g = reuzenpanda(a); anims = reuzenpanda_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", d1.baby(g, 0.55, 0.8), a)
    elif soort == "luiaard":
        a = Atlas(420); g = luiaard(a); anims = luiaard_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", d1.baby(g, 0.6, 0.8), a)
    elif soort == "okapi":
        a = Atlas(430); g = okapi(a); anims = okapi_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", d1.baby(g, 0.58, 0.8), a)
    elif soort == "orang_oetan":
        a = Atlas(440); g = orang_oetan(a); anims = orang_oetan_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(440); at.schrijf(d, soort + "_man", orang_oetan(am, man=True), am)
        at.schrijf(d, soort + "_baby", d1.baby(g, 0.6, 0.8), a)
    elif soort == "toekan":
        a = Atlas(450); g = toekan(a); anims = toekan_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", d1.baby(g, 0.6, 0.82), a)
    else:
        raise SystemExit(f"onbekende soort {soort}")
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort)


SOORTEN = ["reuzenpanda", "luiaard", "okapi", "orang_oetan", "toekan"]
NAMEN = {"reuzenpanda": "Reuzenpanda", "luiaard": "Luiaard", "okapi": "Okapi", "orang_oetan": "Orang-oetan", "toekan": "Toekan"}


# =====================================================================================================================
# voorstelvellen (zelfde opzet als blad.py, met de batch-4-namen en -titels)
# =====================================================================================================================
def vellen(soorten, overzicht=True):
    import blad as bl                                     # (zet de werkmap op guhs/; alle paden hier zijn absoluut)
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
        dr = ImageDraw.Draw(img)
        dr.text((20, 18), f"{NAMEN[soort]}  (safari-dieren, batch 4: regenwoud + bamboe)", font=bl.font(30, True), fill=(40, 30, 40))
        dr.text((20, 56), "voor / zij / schuin, en schuin naast een guh (zelfde schaal). Animaties volgen in de game.",
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

    for s in soorten:
        vel(s)
    if not overzicht:
        return
    guh = bl.guh_quads()
    x, quads = 0.0, []
    for naam, q, qb in [("guh", guh, None)] + [(s, bl.dier_quads(s), bl.dier_quads(s, "baby")) for s in SOORTEN]:
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
    dr.text((20, 18), "Safari-dieren batch 4 (regenwoud + bamboe): naast een guh (volwassen achter, jong ervoor)", font=bl.font(28, True), fill=(40, 30, 40))
    dr.text((20, 56), "Reuzenpanda · Luiaard · Okapi · Orang-oetan · Toekan (van links naar rechts, na de guh)", font=bl.font(17), fill=(80, 70, 80))
    pad = os.path.join(UIT, "batch4_overzicht.png")
    img.convert("RGB").save(pad)
    print(pad)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if a != "geen-vellen"]
    soorten = args or SOORTEN
    for s in soorten:
        maak(s)
    if "geen-vellen" not in sys.argv:
        vellen(soorten, overzicht=not args)
