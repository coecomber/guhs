"""
Safari-voorwerk, batch 1 (stijlproef): rode panda, olifant, pinguin, giraffe, bever, flamingo.

    python safari-ontwerp/dieren/_bron/dieren.py [soort ...]     (geen soort: alle zes)

Schrijft per soort in safari-ontwerp/dieren/<soort>/:
    <soort>.geo.json + <soort>.png             volwassen (vrouwtje, of beide als ze gelijk zijn)
    <soort>_man.geo.json + <soort>_man.png     alleen waar mannetje en vrouwtje verschillen (olifant, giraffe)
    <soort>_baby.geo.json + <soort>_baby.png   het jong
    <soort>.animation.json                     gedeeld door alle drie (dezelfde botnamen): idle, walk, blij, eet
    <soort>.bbmodel                            Blockbench-project van het volwassen dier (incl. animaties)
Coordinaten in modelpixels (16 = 1 blok), voorkant = -z (de north-vlakken), zoals alle guhs-entity's.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, met, gezicht, neusje, vorm_oor, transparent  # noqa: E402

UIT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "build", "safari_atelier"))  # staging; tools/features/safari_dieren.py copies into the assets


# =====================================================================================================================
# hulpjes
# =====================================================================================================================
def zijstrook(fur, licht, kant, frac=0.35):
    """Zijvlak met een lichte strook aan de voorkant (east: rechts = voor, west: links = voor)."""
    def f(W, H, rng, a):
        n = int(W * frac)
        if kant == "east":
            a[:, W - n:, :3] = licht
        else:
            a[:, :n, :3] = licht
    return plain(fur, 8, f)


def nagels(fur, nagel):
    def f(W, H, rng, a):
        for i in (-1, 0, 1):
            disc(a, W / 2 + i * W * 0.28, H * 0.9, W * 0.11, nagel, 1.0, ry=H * 0.07)
    return plain(fur, 8, f)


def baby(geo_, k, hk, drop=(), head="head"):
    """Maakt een jong van een volwassen model: lijf en poten schalen met k, de kop (en alles eraan) met hk
    rond zijn eigen draaipunt (chibi: een relatief nog grotere kop). Botten in drop vallen weg."""
    g = json.loads(json.dumps(geo_))
    gg = g["minecraft:geometry"][0]
    par = {b["name"]: b.get("parent") for b in gg["bones"]}

    def onder_kop(n):
        while n:
            if n == head:
                return True
            n = par.get(n)
        return False
    hp = next(b["pivot"] for b in gg["bones"] if b["name"] == head)
    hp2 = [v * k for v in hp]
    bones = []
    for b in gg["bones"]:
        if b["name"] in drop:
            continue
        if onder_kop(b["name"]):
            T = lambda p: [hp2[i] + (p[i] - hp[i]) * hk for i in range(3)]   # noqa: E731
            s = hk
        else:
            T = lambda p: [p[i] * k for i in range(3)]                         # noqa: E731
            s = k
        b["pivot"] = [round(v, 3) for v in T(b["pivot"])]
        for c in b.get("cubes", []):
            c["origin"] = [round(v, 3) for v in T(c["origin"])]
            c["size"] = [round(v * s, 3) for v in c["size"]]
            if c.get("inflate"):
                c["inflate"] = round(c["inflate"] * s, 3)
        bones.append(b)
    gg["bones"] = bones
    d = gg["description"]
    d["identifier"] += "_baby"
    d["visible_bounds_width"] = round(d["visible_bounds_width"] * hk, 2)
    d["visible_bounds_height"] = round(d["visible_bounds_height"] * hk, 2)
    d["visible_bounds_offset"] = [0, d["visible_bounds_height"] / 2, 0]
    return g


def paar(atlas, naam, parent, pivot, origin, size, paint, overrides=None, rotation=None):
    """Een links/rechts-paar botten (x gespiegeld): <naam>_left op +x, <naam>_right op -x."""
    out = []
    for side, sx in (("left", 1), ("right", -1)):
        o = origin if sx > 0 else at.mirror_x(origin, size)
        ov = overrides(side) if callable(overrides) else overrides
        rot = None if rotation is None else [rotation[0], rotation[1] * sx, rotation[2] * sx]
        out.append(bone(f"{naam}_{side}", parent, [pivot[0] * sx, pivot[1], pivot[2]], [cube(atlas, o, size, paint, ov)], rot))
    return out


def vier_poten(atlas, parent, x, zv, za, size, paint, top_y, overrides=None, voet=None):
    """leg_front_left/right (z = zv) en leg_back_left/right (z = za), x = afstand van het midden tot de binnenkant."""
    bones = []
    for voor, z in (("front", zv), ("back", za)):
        for side, sx in (("left", 1), ("right", -1)):
            o = [x, 0, z] if sx > 0 else [-x - size[0], 0, z]
            cubes = [cube(atlas, o, size, paint, overrides)]
            if voet:
                cubes.append(cube(atlas, [o[0] - 0.1, 0, o[2] - 0.1], [size[0] + 0.2, voet[0], size[2] + 0.2], voet[1]))
            bones.append(bone(f"leg_{voor}_{side}", parent, [o[0] + size[0] / 2, top_y, o[2] + size[2] / 2], cubes))
    return bones


ZIJ_LEEG = {"east": transparent, "west": transparent, "up": transparent, "down": transparent}
POTEN4 = ["leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"]


# =====================================================================================================================
# rode panda
# =====================================================================================================================
RP_RUST = (196, 84, 42)
RP_DONKER = (66, 38, 32)
RP_WIT = (252, 244, 232)
RP_RING = (232, 168, 112)
NEUS = (40, 28, 34)


def rode_panda(atlas):
    def masker(W, H, rng, a):
        disc(a, W * 0.5, H * 0.9, W * 0.3, RP_WIT, 1.0, ry=H * 0.2)
        for sx in (-1, 1):
            disc(a, W * (0.5 + sx * 0.4), H * 0.72, W * 0.13, RP_WIT, 1.0, ry=H * 0.16)
            disc(a, W * (0.5 + sx * 0.26), H * 0.16, W * 0.09, RP_WIT, 1.0, ry=H * 0.06)
    rust = plain(RP_RUST, 10)
    donker = plain(RP_DONKER, 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [cube(atlas, [-5, 4, -6], [10, 8, 13], rust, {"down": donker}),
                                              cube(atlas, [-4.5, 3.6, -6.4], [9, 6, 3], donker)])]
    bones += vier_poten(atlas, "body", 1.2, -5.5, 4.5, [3, 4.5, 3], donker, 4.5)
    bones.append(bone("head", "body", [0, 9, -6], [
        cube(atlas, [-6, 6.5, -15], [12, 10, 9], rust, {"north": gezicht(RP_RUST, eye_y=0.46, eye_r=0.16, spots=masker)}),
        cube(atlas, [-2.5, 6.8, -16.4], [5, 3.6, 1.6], plain(RP_WIT, 6), {"north": neusje(RP_WIT, NEUS)})]))
    oor = vorm_oor(RP_RUST, RP_DONKER, punt=0.45, rand=RP_WIT)
    bones += paar(atlas, "ear", "head", [4.6, 16, -11], [3, 15.6, -11.5], [3.6, 3.6, 1], rust, dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(bone("tail", "body", [0, 9, 6.5], [
        cube(atlas, [-2.5, 6.5, 6.5], [5, 5, 12], rust, {"east": at.ringen(RP_RUST, RP_RING, "W"), "west": at.ringen(RP_RUST, RP_RING, "W"),
                                                        "up": at.ringen(RP_RUST, RP_RING, "H"), "down": at.ringen(RP_RUST, RP_RING, "H"),
                                                        "south": plain(RP_DONKER, 8)})], rotation=[18, 0, 0]))
    return geo("rode_panda", bones, atlas, 1.6, 1.3)


def rode_panda_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail")


# =====================================================================================================================
# olifant
# =====================================================================================================================
OL = (158, 164, 180)
OL_D = (132, 138, 156)
OL_ROZE = (240, 170, 190)
IVOOR = (250, 244, 226)


def olifant(atlas, man=False):
    grijs = plain(OL, 9)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 10, 0], [cube(atlas, [-9, 8, -10], [18, 14, 20], grijs, {"down": plain(OL_D, 8)})])]
    bones += vier_poten(atlas, "body", 2.5, -8, 4, [5.5, 9, 5.5], grijs, 9, {"north": nagels(OL, IVOOR), "down": plain(OL_D, 6)})
    bones.append(bone("head", "body", [0, 16, -9], [
        cube(atlas, [-8, 12, -18.5], [16, 13.5, 9], grijs, {"north": gezicht(OL, eye_y=0.36, eye_dx=0.27, eye_r=0.15)})]))
    for side, sx in (("left", 1), ("right", -1)):
        buiten = vorm_oor(OL, OL_D, punt=0.0)
        binnen = vorm_oor(OL, OL_ROZE, punt=0.0)
        ov = {"east": buiten if sx > 0 else binnen, "west": binnen if sx > 0 else buiten,
              "north": transparent, "south": transparent, "up": transparent, "down": transparent}
        o = [7.8, 12.5, -17] if sx > 0 else [-8.8, 12.5, -17]
        bones.append(bone(f"ear_{side}", "head", [sx * 8, 20, -16.5], [cube(atlas, o, [1, 14, 12], grijs, ov)], rotation=[0, sx * 28, 0]))
    bones.append(bone("trunk", "head", [0, 15, -18.5], [cube(atlas, [-2.5, 8, -21], [5, 7.5, 3], grijs)]))
    bones.append(bone("trunk_tip", "trunk", [0, 8.5, -19.5], [cube(atlas, [-2, 3.5, -21], [4, 5, 3], grijs),
                                                             cube(atlas, [-2.2, 2.6, -23.2], [4.4, 2.2, 3], grijs,
                                                                  {"north": neusje(OL_ROZE, (110, 90, 110), mond=False)})],
                      rotation=[-25, 0, 0]))
    tand = [2, 2, 6] if man else [1.4, 1.4, 3]
    bones += paar(atlas, "tusk", "head", [3.5, 12, -18.5], [3, 11 if man else 11.5, -18.5 - tand[2]], tand, plain(IVOOR, 6),
                  rotation=[-12 if man else 0, 0, 0])
    bones.append(bone("tail", "body", [0, 20, 10], [cube(atlas, [-0.5, 13, 10], [1, 7, 1], grijs),
                                                    cube(atlas, [-1, 11.5, 9.75], [2, 2, 1.5], plain((70, 70, 80), 6))]))
    return geo("olifant_man" if man else "olifant", bones, atlas, 2.2, 2.2)


def olifant_anims():
    flap = {"ear_left": {"rotation": at.kf((0, [0, 0, 0]), (0.75, [0, -18, 0]), (1.5, [0, 0, 0]), (2.25, [0, -18, 0]), (3.0, [0, 0, 0]))},
            "ear_right": {"rotation": at.kf((0, [0, 0, 0]), (0.75, [0, 18, 0]), (1.5, [0, 0, 0]), (2.25, [0, 18, 0]), (3.0, [0, 0, 0]))},
            "trunk": {"rotation": at.kf((0, [0, 0, 0]), (1.5, [-8, 0, 6]), (3.0, [0, 0, 0]))}}
    return at.animaties(legs4=POTEN4, staart="tail", extra_idle=flap)


# =====================================================================================================================
# pinguin (keizerspinguin) + kuiken
# =====================================================================================================================
ZW = (34, 36, 48)
WIT = (250, 250, 248)
GEEL = (255, 206, 92)
ORANJE = (255, 168, 60)
KUIKEN = (178, 178, 186)
KUIKEN_L = (214, 214, 220)


def pinguin(atlas, kuiken=False):
    if kuiken:
        lijf, buik_top, buik_ond = at.fluffy(KUIKEN, 16), KUIKEN_L, KUIKEN_L
        zij = lambda kant: at.fluffy(KUIKEN, 16)                                         # noqa: E731
        kop = plain(ZW, 8)
        face = gezicht(WIT, eye_y=0.5, eye_r=0.18, spots=lambda W, H, rng, a: a.__setitem__((slice(0, int(H * 0.2)), slice(None), slice(0, 3)), ZW))
        kopzij = plain(ZW, 8)
    else:
        lijf, buik_top, buik_ond = plain(ZW, 8), GEEL, WIT
        zij = lambda kant: zijstrook(ZW, WIT, kant, 0.3)                                 # noqa: E731
        kop = plain(ZW, 8)
        face = gezicht(ZW, eye_y=0.5, eye_r=0.17)

        def vlek(kant):
            def f(W, H, rng, a):
                cx = W * (0.72 if kant == "east" else 0.28)
                disc(a, cx, H * 0.62, W * 0.2, ORANJE, 1.0, ry=H * 0.26)
                disc(a, cx, H * 0.7, W * 0.13, GEEL, 1.0, ry=H * 0.16)
            return plain(ZW, 8, f)
        kopzij = None
    buik = at.verloop(buik_top, buik_ond)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2, 0], [cube(atlas, [-6, 2, -5], [12, 15, 10], lijf, {"north": buik, "east": zij("east"), "west": zij("west")}),
                                              cube(atlas, [-5, 3.5, -5.8], [10, 11, 1], buik, {"east": plain(WIT, 4), "west": plain(WIT, 4)}),
                                              cube(atlas, [-2, 2, 5], [4, 2, 2], plain(ZW, 6))])]
    hz = {"north": face}
    if kopzij is None:
        hz["east"], hz["west"] = vlek("east"), vlek("west")
    bones.append(bone("head", "body", [0, 17, -1], [
        cube(atlas, [-5, 16.5, -5.5], [10, 8.5, 9], kop, hz),
        cube(atlas, [-1, 18.3, -8], [2, 1.6, 2.6], plain(ZW, 4), {"down": plain(ORANJE if not kuiken else ZW, 4)})]))
    bones += paar(atlas, "flipper", "body", [6, 15, 0], [6, 6, -3], [1, 9.5, 5], plain(KUIKEN if kuiken else ZW, 8), rotation=[0, 0, -10])
    poot = plain((60, 60, 72), 6)
    for side, sx in (("left", 1), ("right", -1)):
        o = [1, 0, -6] if sx > 0 else [-4.5, 0, -6]
        bones.append(bone(f"leg_{side}", "body", [sx * 2.75, 2, -3.5], [cube(atlas, o, [3.5, 2, 4.5], poot)]))
    return geo("pinguin", bones, atlas, 1.2, 1.7)


def pinguin_anims():
    return at.animaties(legs2=["leg_left", "leg_right"], waggel=True,
                        extra_idle={"flipper_left": {"rotation": at.kf((0, [0, 0, 0]), (2.2, [0, 0, 0]), (2.4, [0, 0, -25]), (2.6, [0, 0, 0]))},
                                    "flipper_right": {"rotation": at.kf((0, [0, 0, 0]), (2.2, [0, 0, 0]), (2.4, [0, 0, 25]), (2.6, [0, 0, 0]))}})


# =====================================================================================================================
# giraffe
# =====================================================================================================================
GI = (246, 222, 168)
GI_V = (176, 104, 52)
GI_L = (255, 244, 220)
GI_D = (110, 70, 44)


def giraffe(atlas, man=False):
    vlek = at.lijn_vlekken(GI, GI_V, cel=7.0, lijn=1.4)
    licht = plain(GI_L, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 16, 0], [cube(atlas, [-6, 14, -9], [12, 10, 17], vlek, {"down": licht})])]
    bones += vier_poten(atlas, "body", 1.8, -8, 4, [3, 15, 3], at.lijn_vlekken(GI, GI_V, cel=5.0, lijn=1.2), 15,
                        voet=(1.6, plain(GI_D, 6)))
    bones.append(bone("hals", "body", [0, 22, -7], [cube(atlas, [-3, 21, -10], [6, 16, 5], vlek),
                                                    cube(atlas, [-0.75, 22, -5.4], [1.5, 15, 1.6], plain(GI_D, 6))], rotation=[8, 0, 0]))
    gezichtje = gezicht(GI, eye_y=0.4, eye_r=0.17,
                        spots=lambda W, H, rng, a: disc(a, W * 0.5, H * 0.08, W * 0.12, GI_V, 1.0, ry=H * 0.08))
    bones.append(bone("head", "hals", [0, 36, -8], [
        cube(atlas, [-4.5, 35, -14], [9, 8, 10], plain(GI, 6), {"north": gezichtje}),
        cube(atlas, [-3, 35, -16.2], [6, 4.5, 2.4], licht, {"north": neusje(GI_L, GI_D)})]))
    h = 3.6 if man else 2.4
    for sx in (1, -1):
        x = 1.2 if sx > 0 else -2.8
        bones.append(bone("ossicone_" + ("left" if sx > 0 else "right"), "head", [sx * 2, 43, -9.7], [
            cube(atlas, [x, 43, -10.5], [1.6, h, 1.6], plain(GI, 6)),
            cube(atlas, [x - 0.2, 43 + h, -10.7], [2, 1.2, 2], plain(GI_D, 6))]))
    oor = vorm_oor(GI, (240, 170, 180), punt=0.4)
    bones += paar(atlas, "ear", "head", [4.5, 41, -9], [4.5, 40.4, -10.5], [3, 1.2, 2.2], plain(GI, 6), {"up": oor}, rotation=[0, 0, -15])
    bones.append(bone("tail", "body", [0, 23, 8], [cube(atlas, [-0.5, 15, 8], [1, 8, 1], plain(GI, 6)),
                                                   cube(atlas, [-1, 13.5, 7.75], [2, 2.5, 1.5], plain(GI_D, 6))]))
    return geo("giraffe_man" if man else "giraffe", bones, atlas, 1.6, 3.2)


def giraffe_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail",
                        extra_idle={"hals": {"rotation": at.kf((0, [0, 0, 0]), (1.5, [4, 0, 3]), (3.0, [0, 0, 0]))}})


# =====================================================================================================================
# bever
# =====================================================================================================================
BV = (132, 84, 52)
BV_L = (196, 150, 108)
BV_D = (88, 56, 38)
BV_STAART = (78, 66, 64)
TAND = (252, 190, 98)


def bever(atlas):
    bruin = at.fluffy(BV, 12)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [cube(atlas, [-6.5, 1.5, -6], [13, 10, 14], bruin, {"north": at.buik(BV, BV_L, cy=0.55), "down": plain(BV_L, 8)})])]
    for voor, z, size in (("front", -5.5, [3, 3, 3]), ("back", 3.5, [3.5, 3, 4])):
        for side, sx in (("left", 1), ("right", -1)):
            o = [3, 0, z] if sx > 0 else [-3 - size[0], 0, z]
            bones.append(bone(f"leg_{voor}_{side}", "body", [o[0] + size[0] / 2, 3, o[2] + size[2] / 2],
                              [cube(atlas, o, size, plain(BV_D, 8))]))
    bones.append(bone("head", "body", [0, 9, -6], [
        cube(atlas, [-5, 5, -14], [10, 9, 8.5], bruin, {"north": gezicht(BV, eye_y=0.36, eye_r=0.17)}),
        cube(atlas, [-3, 5.2, -15.4], [6, 4, 1.6], plain(BV_L, 6), {"north": neusje(BV_L, NEUS, mond=False)}),
        cube(atlas, [-1.2, 3.4, -15.1], [2.4, 2, 0.6], plain(TAND, 4),
             {"north": plain(TAND, 4, lambda W, H, rng, a: a.__setitem__((slice(None), slice(W // 2 - 1, W // 2 + 1), slice(0, 3)), (214, 150, 70)))})]))
    oor = vorm_oor(BV, BV_D)
    bones += paar(atlas, "ear", "head", [4.3, 13.5, -10], [3.4, 12.6, -10.5], [2, 2, 1], plain(BV, 6), dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(bone("tail", "body", [0, 3, 7.5], [cube(atlas, [-3.5, 1.5, 7.5], [7, 1.5, 10], at.schubben(BV_STAART, (54, 46, 46)))]))
    return geo("bever", bones, atlas, 1.3, 1.1)


def bever_anims():
    klap = {"tail": {"rotation": at.kf((0, [0, 0, 0]), (2.5, [0, 0, 0]), (2.65, [18, 0, 0]), (2.8, [0, 0, 0]), (3.0, [0, 0, 0]))}}
    return at.animaties(legs4=POTEN4, oren=True, extra_idle=klap)


# =====================================================================================================================
# flamingo + kuiken
# =====================================================================================================================
FL = (250, 146, 176)
FL_L = (255, 196, 212)
FL_D = (226, 96, 136)
FL_ZW = (40, 34, 44)
FK = (222, 222, 228)
FK_D = (150, 150, 160)


def flamingo(atlas, kuiken=False):
    veer = at.fluffy(FK, 14) if kuiken else plain(FL, 8)
    licht = at.fluffy(FK, 10) if kuiken else plain(FL_L, 8)
    pootkl = plain(FK_D if kuiken else FL_D, 6)
    bones = [bone("root", None, [0, 0, 0], [])]
    for side, sx in (("left", 1), ("right", -1)):
        x = 0.8 if sx > 0 else -1.8
        bones.append(bone(f"leg_{side}", "root", [sx * 1.3, 14, 0], [
            cube(atlas, [x, 0.5, -0.5], [1, 14, 1], pootkl),
            cube(atlas, [x - 0.2, 7, -0.7], [1.4, 1.4, 1.4], pootkl),
            cube(atlas, [x - 1, 0, -2.2], [3, 0.6, 3], pootkl)]))

    def vleugel(side):
        def f(W, H, rng, a):
            n = int(W * 0.3)
            if not kuiken:
                if side == "left":
                    a[:, :n, :3] = FL_ZW
                else:
                    a[:, W - n:, :3] = FL_ZW
        return {("east" if side == "left" else "west"): plain(FL_D if not kuiken else FK, 8, f)}
    bones.append(bone("body", "root", [0, 17, 0], [cube(atlas, [-4, 13.5, -5], [8, 7, 11], veer, {"down": licht}),
                                                   cube(atlas, [-3, 15, 6], [6, 4, 3], licht)]))
    bones += paar(atlas, "wing", "body", [4, 20, 0], [4, 14.5, -3.5], [1, 5.5, 9], veer, vleugel)
    bones.append(bone("hals", "body", [0, 19, -5], [cube(atlas, [-1.25, 19, -6.5], [2.5, 9, 2.5], veer)]))
    bones.append(bone("head", "hals", [0, 29, -6], [
        cube(atlas, [-3.5, 27.5, -10], [7, 6.5, 7], veer, {"north": gezicht(FK if kuiken else FL, eye_y=0.38, eye_r=0.18)}),
        cube(atlas, [-1.25, 27.8, -12.4], [2.5, 2.4, 2.5], plain(FL_ZW if kuiken else (242, 228, 228), 4)),
        cube(atlas, [-1, 26.2, -12.9], [2, 2, 1.5], plain(FL_ZW, 4))]))
    return geo("flamingo", bones, atlas, 1.0, 2.3)


def flamingo_anims():
    return at.animaties(legs2=["leg_left", "leg_right"],
                        extra_idle={"hals": {"rotation": at.kf((0, [0, 0, 0]), (1.5, [6, 0, 0]), (3.0, [0, 0, 0]))}})


# =====================================================================================================================
# alles wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "rode_panda":
        a = Atlas(110); g = rode_panda(a); anims = rode_panda_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.62, 0.82), a)
    elif soort == "olifant":
        a = Atlas(120, 128); g = olifant(a); anims = olifant_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(120, 128); at.schrijf(d, soort + "_man", olifant(am, man=True), am)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.78, drop=("tusk_left", "tusk_right")), a)
    elif soort == "pinguin":
        a = Atlas(130); g = pinguin(a); anims = pinguin_anims()
        img = at.schrijf(d, soort, g, a, anims)
        ak = Atlas(131); at.schrijf(d, soort + "_baby", baby(pinguin(ak, kuiken=True), 0.6, 0.8), ak)
    elif soort == "giraffe":
        a = Atlas(140, 128); g = giraffe(a); anims = giraffe_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(140, 128); at.schrijf(d, soort + "_man", giraffe(am, man=True), am)
        at.schrijf(d, soort + "_baby", baby(g, 0.58, 0.8), a)
    elif soort == "bever":
        a = Atlas(150); g = bever(a); anims = bever_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.62, 0.8), a)
    elif soort == "flamingo":
        a = Atlas(160); g = flamingo(a); anims = flamingo_anims()
        img = at.schrijf(d, soort, g, a, anims)
        ak = Atlas(161); at.schrijf(d, soort + "_baby", baby(flamingo(ak, kuiken=True), 0.55, 0.8), ak)
    else:
        raise SystemExit(f"onbekende soort {soort}")
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort)


SOORTEN = ["rode_panda", "olifant", "pinguin", "giraffe", "bever", "flamingo"]

if __name__ == "__main__":
    for s in sys.argv[1:] or SOORTEN:
        maak(s)
