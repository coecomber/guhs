"""
Safari-voorwerk, batch 3: zebra, stokstaartje, zeekoe, nijlpaard, capibara, krokodil (zelfde stijl en formaat als batch 1).

    python safari-ontwerp/dieren/_bron/batch3.py [soort ...] [--geen-vellen]   (geen soort: alle zes + vellen + overzicht)

Per soort in safari-ontwerp/dieren/<soort>/: <soort>.geo.json/.png (volwassen; bij het nijlpaard het vrouwtje),
<soort>_man.* (alleen nijlpaard: het mannetje is groter), <soort>_baby.*, <soort>.animation.json (idle, walk, blij, eet;
stokstaartje ook 'wacht', nijlpaard ook 'gaap'), <soort>.bbmodel en <soort>_voorstel.png; plus batch3_overzicht.png.
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
from dieren import baby, paar, vier_poten, nagels, ZIJ_LEEG, POTEN4   # noqa: E402

UIT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "build", "safari_atelier"))  # staging; tools/features/safari_dieren.py copies into the assets
NEUS = (40, 28, 34)
S = at.S


# =====================================================================================================================
# extra schilders (alleen voor deze batch)
# =====================================================================================================================
def strepen(base, streep, n=5, richting="V", var=6, golf=0.12, dik=0.45):
    """Zebra-achtige strepen: banden langs W ('V' = staande banden) of langs H ('H' = liggende banden), licht golvend."""
    def paint(W, H, rng):
        a = plain(base, var)(W, H, rng).astype(np.float32)
        yy, xx = np.mgrid[0:H, 0:W] + 0.5
        fase = rng.uniform(0, 2 * math.pi)
        if richting == "V":
            t = xx / W * n + golf * np.sin(yy / max(H, 1) * 2 * math.pi + fase)
        else:
            t = yy / H * n + golf * np.sin(xx / max(W, 1) * 2 * math.pi + fase)
        m = (t % 1.0) < dik
        a[m, :3] = np.array(streep) + rng.normal(0, var / 2, (int(m.sum()), 1))
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def glimlach_zij(base, lijn, tand, kant, var=8):
    """Zijkant van een krokodillensnuit: een brede glimlach die naar achteren omhoog krult, met ronde witte tandjes.
    east: rechts = voor, west: links = voor (zoals zijstrook in batch 1)."""
    def paint(W, H, rng):
        a = plain(base, var)(W, H, rng).astype(np.float32)
        for x in range(W):
            f = x / max(W - 1, 1)
            achter = 1 - f if kant == "east" else f            # 0 = voor (neus), 1 = achter (mondhoek)
            y = int(H * (0.62 - 0.32 * achter ** 2.2))
            for dy in range(-1, 2):
                if 0 <= y + dy < H:
                    a[y + dy, x, :3] = lijn
            if 0.08 < achter < 0.8 and (x // (S + 1)) % 2 == 0 and y + 2 + S // 2 < H:
                a[y + 2:y + 2 + S // 2, x, :3] = tand
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def rijen(base, bult, n=3, var=8):
    """Krokodillenrug: rijen donkere ovale schubbulten."""
    def spots(W, H, rng, a):
        stap = max(S * 3, H // 6)
        for y in range(stap // 2, H, stap):
            for i in range(n):
                disc(a, W * (i + 0.5) / n, y, W / n * 0.28, bult, 1.0, ry=stap * 0.3)
    return plain(base, var, spots)


def peddel(base, rand, var=6):
    """Zeekoestaart: een ronde peddel (doorzichtige hoeken), iets donkerder randje."""
    def paint(W, H, rng):
        a = plain(base, var)(W, H, rng).astype(np.float32)
        yy, xx = np.mgrid[0:H, 0:W] + 0.5
        nx, ny = (xx - W / 2) / (W / 2), (yy - H / 2) / (H / 2)
        r = nx ** 2 + ny ** 2
        a[r > 1.02, 3] = 0
        a[(r > 0.7) & (r <= 1.02), :3] = rand
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def snorhaartjes(fur, neus, stip):
    """Snuit met snorhaar-stipjes en een lief mondje (zeekoe, capibara)."""
    base = neusje(fur, neus)

    def paint(W, H, rng):
        a = base(W, H, rng).astype(np.float32)
        for sx in (-1, 1):
            for i in range(3):
                for j in range(2):
                    disc(a, W / 2 + sx * W * (0.22 + 0.09 * i), H * (0.5 + 0.12 * j), max(1.0, W * 0.025), stip, 1.0)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def groter(geo_, f, ident):
    """Hetzelfde model, alles f keer zo groot (het grotere mannetje)."""
    g = json.loads(json.dumps(geo_))
    gg = g["minecraft:geometry"][0]
    for b in gg["bones"]:
        b["pivot"] = [round(v * f, 3) for v in b["pivot"]]
        for c in b.get("cubes", []):
            c["origin"] = [round(v * f, 3) for v in c["origin"]]
            c["size"] = [round(v * f, 3) for v in c["size"]]
            if c.get("inflate"):
                c["inflate"] = round(c["inflate"] * f, 3)
    d = gg["description"]
    d["identifier"] = f"geometry.{ident}"
    d["visible_bounds_width"] = round(d["visible_bounds_width"] * f, 2)
    d["visible_bounds_height"] = round(d["visible_bounds_height"] * f, 2)
    d["visible_bounds_offset"] = [0, d["visible_bounds_height"] / 2, 0]
    return g


# =====================================================================================================================
# zebra (+ veulen met bruinige strepen, zoals echte zebraveulens)
# =====================================================================================================================
ZB_WIT = (248, 246, 242)
ZB_ZW = (36, 34, 42)
ZB_BRUIN = (112, 82, 66)
ZB_SNUIT = (78, 74, 84)


def zebra(atlas, veulen=False):
    st = ZB_BRUIN if veulen else ZB_ZW
    zij = strepen(ZB_WIT, st, n=6, richting="V", dik=0.42)
    rug = strepen(ZB_WIT, st, n=5, richting="V", dik=0.38)
    poot = strepen(ZB_WIT, st, n=5, richting="H", dik=0.45)
    wit = plain(ZB_WIT, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 14, 0], [cube(atlas, [-5, 12, -8], [10, 9, 16], zij, {"up": rug, "down": wit, "north": rug})])]
    bones += vier_poten(atlas, "body", 1.6, -7, 4.5, [2.8, 12, 2.8], poot, 12, voet=(1.5, plain(ZB_ZW, 6)))
    bones.append(bone("hals", "body", [0, 19, -6], [cube(atlas, [-2.5, 18, -10], [5, 10, 5], zij),
                                                    cube(atlas, [-0.75, 19, -5.6], [1.5, 10, 1.8], strepen(ZB_WIT, st, n=4, richting="H", dik=0.5))],
                      rotation=[12, 0, 0]))

    def voorhoofd(W, H, rng, a):
        for i in range(4):
            x0 = int(W * (0.3 + 0.12 * i))
            a[:int(H * 0.16), x0:x0 + max(1, W // 16), :3] = st
    bones.append(bone("head", "hals", [0, 27, -8], [
        cube(atlas, [-4, 24, -14], [8, 8.5, 8], strepen(ZB_WIT, st, n=4, richting="H", dik=0.35),
             {"north": gezicht(ZB_WIT, eye_y=0.38, eye_r=0.17, spots=voorhoofd), "down": wit}),
        cube(atlas, [-2.75, 23.6, -16.2], [5.5, 3.4, 2.4], plain(ZB_SNUIT, 6), {"north": neusje(ZB_SNUIT, (24, 22, 28))})]))
    oor = vorm_oor(ZB_WIT, (60, 56, 66), punt=0.55)
    bones += paar(atlas, "ear", "head", [2.6, 32.4, -10], [1.6, 32, -10.8], [2.4, 3.6, 1], wit, dict(ZIJ_LEEG, north=oor, south=oor))
    bones.append(bone("tail", "body", [0, 20, 8], [cube(atlas, [-0.5, 13, 8], [1, 7, 1], wit),
                                                   cube(atlas, [-1, 11, 7.75], [2, 3, 1.5], plain(ZB_ZW, 6))]))
    return geo("zebra", bones, atlas, 1.4, 2.3)


def zebra_anims():
    return at.animaties(legs4=POTEN4, oren=True, staart="tail",
                        extra_idle={"hals": {"rotation": at.kf((0, [0, 0, 0]), (1.5, [6, 0, 0]), (3.0, [0, 0, 0]))}})


# =====================================================================================================================
# stokstaartje (met 'wacht': rechtop op de achterpootjes)
# =====================================================================================================================
MK = (206, 172, 124)
MK_L = (238, 218, 182)
MK_D = (106, 80, 58)
MK_OOG = (92, 68, 50)


def stokstaartje(atlas):
    vacht = plain(MK, 10)
    licht = plain(MK_L, 8)
    donker = plain(MK_D, 8)
    rug = strepen(MK, MK_D, n=6, richting="V", dik=0.25)
    bones = [bone("root", None, [0, 0, 0], []),
             # draaipunt bij de heupen: 'wacht' kantelt het lijf daar omhoog terwijl de achterpootjes blijven staan
             bone("body", "root", [0, 4, 3.5], [cube(atlas, [-3, 3, -7], [6, 6, 11], vacht,
                                                     {"up": rug, "down": licht, "north": at.buik(MK, MK_L, cy=0.6)})])]
    for voor, parent, z, size in (("front", "body", -6, [2, 3.2, 2]), ("back", "root", 1.5, [2.2, 3, 2.6])):
        for side, sx in (("left", 1), ("right", -1)):
            o = [0.8, 0, z] if sx > 0 else [-0.8 - size[0], 0, z]
            bones.append(bone(f"leg_{voor}_{side}", parent, [o[0] + size[0] / 2, 3, o[2] + size[2] / 2],
                              [cube(atlas, o, size, donker)]))

    def oogvlekken(W, H, rng, a):
        for sx in (-1, 1):
            disc(a, W * (0.5 + sx * 0.26), H * 0.44, W * 0.22, MK_OOG, 1.0, ry=H * 0.24)
    bones.append(bone("head", "body", [0, 7, -7], [
        cube(atlas, [-3.5, 5.5, -13], [7, 6.5, 6.5], vacht, {"north": gezicht(MK_L, eye_y=0.44, eye_r=0.17, spots=oogvlekken)}),
        cube(atlas, [-1.5, 5.6, -14.6], [3, 2.2, 1.6], licht, {"north": neusje(MK_L, (52, 38, 32))})]))
    bones += paar(atlas, "ear", "head", [3.6, 10, -10], [3.5, 9.4, -10.6], [0.8, 1.6, 1.6], donker)
    bones.append(bone("tail", "body", [0, 6, 4], [cube(atlas, [-0.75, 5.2, 4], [1.5, 1.5, 7.5], vacht, {"up": rug}),
                                                  cube(atlas, [-0.8, 5.15, 11.4], [1.6, 1.6, 1.6], donker)], rotation=[20, 0, 0]))
    return geo("stokstaartje", bones, atlas, 0.9, 1.0)


def stokstaartje_anims():
    a = at.animaties(legs4=POTEN4, oren=False, staart="tail")
    # rechtop op wacht: lijf ~70 graden omhoog rond de heupen, kop terug waterpas en speurend links-rechts,
    # voorpootjes slap naar beneden, staart als steuntje plat naar achteren (onbeproefd in de game)
    a["animations"]["wacht"] = {"loop": True, "animation_length": 3.0, "bones": {
        "body": {"rotation": at.kf((0, [-70, 0, 0]), (3.0, [-70, 0, 0]))},
        "head": {"rotation": at.kf((0, [65, 0, 0]), (0.8, [65, 25, 0]), (1.6, [65, -25, 0]), (2.4, [65, 0, 0]), (3.0, [65, 0, 0]))},
        "leg_front_left": {"rotation": at.kf((0, [60, 0, 0]), (3.0, [60, 0, 0]))},
        "leg_front_right": {"rotation": at.kf((0, [60, 0, 0]), (3.0, [60, 0, 0]))},
        "tail": {"rotation": at.kf((0, [50, 0, 0]), (3.0, [50, 0, 0]))}}}
    return a


# =====================================================================================================================
# zeekoe (waterdier: peddelstaart, flippers, geen poten)
# =====================================================================================================================
ZK = (146, 140, 146)
ZK_L = (184, 176, 178)
ZK_D = (112, 108, 116)


def zeekoe(atlas):
    def vlekjes(W, H, rng, a):
        for _ in range(max(1, W * H // 600)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(1.5, 3.5), ZK_L, 0.5)
    huid = plain(ZK, 8, vlekjes)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [cube(atlas, [-6, 1, -8], [12, 10, 17], huid, {"down": plain(ZK_L, 6)}, inflate=0.0)])]
    bones.append(bone("head", "body", [0, 7, -8], [
        cube(atlas, [-5, 2.2, -15], [10, 8, 7.5], huid, {"north": gezicht(ZK, eye_y=0.36, eye_r=0.16)}),
        cube(atlas, [-4, 1.8, -17.4], [8, 4, 2.6], plain(ZK_L, 6), {"north": snorhaartjes(ZK_L, (70, 62, 70), (96, 88, 96))})]))
    bones += paar(atlas, "flipper", "body", [6, 4, -4], [6, 3.5, -6], [4, 1, 3.5], plain(ZK_D, 6), rotation=[0, 0, 25])
    bones.append(bone("tail", "body", [0, 6, 9], [cube(atlas, [-3, 3.5, 9], [6, 4, 5], huid),
                                                  cube(atlas, [-6, 4.6, 13], [12, 1.4, 7], plain(ZK_D, 6),
                                                       {"up": peddel(ZK, ZK_D), "down": peddel(ZK_L, ZK_D),
                                                        "north": transparent, "south": transparent, "east": transparent, "west": transparent})]))
    return geo("zeekoe", bones, atlas, 1.5, 1.0)


def zeekoe_anims():
    a = at.animaties(oren=False)
    zwem = {"tail": {"rotation": at.kf((0, [12, 0, 0]), (0.6, [-12, 0, 0]), (1.2, [12, 0, 0]))},
            "flipper_left": {"rotation": at.kf((0, [0, 0, -10]), (0.6, [0, 0, 10]), (1.2, [0, 0, -10]))},
            "flipper_right": {"rotation": at.kf((0, [0, 0, 10]), (0.6, [0, 0, -10]), (1.2, [0, 0, 10]))},
            "body": {"position": at.kf((0, [0, 0, 0]), (0.6, [0, 0.6, 0]), (1.2, [0, 0, 0]))}}
    a["animations"]["walk"] = {"loop": True, "animation_length": 1.2, "bones": zwem}
    a["animations"]["idle"]["bones"]["body"]["position"] = at.kf((0, [0, 0, 0]), (1.5, [0, 0.4, 0]), (3.0, [0, 0, 0]))
    a["animations"]["idle"]["bones"]["tail"] = {"rotation": at.kf((0, [0, 0, 0]), (1.5, [6, 0, 0]), (3.0, [0, 0, 0]))}
    return a


# =====================================================================================================================
# nijlpaard (mannetje = groter)
# =====================================================================================================================
NP = (152, 126, 152)
NP_L = (236, 170, 182)
NP_D = (116, 94, 120)


def nijlpaard(atlas):
    huid = plain(NP, 8)
    roze = plain(NP_L, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 9, 0], [cube(atlas, [-8, 5, -9], [16, 12, 19], huid, {"down": plain(NP_D, 6)})])]
    bones += vier_poten(atlas, "body", 3, -8, 5, [4.5, 5.5, 4.5], huid, 5.5, {"north": nagels(NP, NP_L), "down": plain(NP_D, 6)})
    bones.append(bone("head", "body", [0, 13, -9], [
        cube(atlas, [-6.5, 10, -17], [13, 9, 8], huid, {"north": gezicht(NP, eye_y=0.32, eye_dx=0.27, eye_r=0.15)}),
        cube(atlas, [-7, 7, -22], [14, 6.5, 6], huid, {"north": neusje(NP, NP_D, mond=True), "down": roze}),
        cube(atlas, [-4.5, 13.5, -21.5], [2.2, 1, 1.8], plain(NP_D, 6)),
        cube(atlas, [2.3, 13.5, -21.5], [2.2, 1, 1.8], plain(NP_D, 6))]))
    bones.append(bone("kaak", "head", [0, 7, -16.5], [
        cube(atlas, [-6.5, 4.6, -21.6], [13, 2.4, 5.6], roze),
        cube(atlas, [-5, 7, -21.2], [1.2, 1.4, 1.2], plain((250, 244, 226), 4)),
        cube(atlas, [3.8, 7, -21.2], [1.2, 1.4, 1.2], plain((250, 244, 226), 4))]))
    bones += paar(atlas, "ear", "head", [5, 19, -11.5], [4.6, 18.6, -12], [1.6, 1.8, 1.2], plain(NP_D, 6))
    bones.append(bone("tail", "body", [0, 14, 10], [cube(atlas, [-1, 10, 10], [2, 4, 1], plain(NP_D, 6))]))
    return geo("nijlpaard", bones, atlas, 2.0, 1.6)


def nijlpaard_anims():
    a = at.animaties(legs4=POTEN4, oren=True, staart="tail")
    a["animations"]["gaap"] = {"loop": False, "animation_length": 2.0, "bones": {
        "head": {"rotation": at.kf((0, [0, 0, 0]), (0.5, [-18, 0, 0]), (1.4, [-18, 0, 0]), (2.0, [0, 0, 0]))},
        "kaak": {"rotation": at.kf((0, [0, 0, 0]), (0.5, [40, 0, 0]), (1.4, [40, 0, 0]), (2.0, [0, 0, 0]))}}}
    return a


# =====================================================================================================================
# capibara (met vogeltje op de rug)
# =====================================================================================================================
CB = (172, 124, 82)
CB_D = (128, 90, 60)
CB_L = (200, 156, 114)
VOGEL = (255, 214, 92)


def capibara(atlas):
    vacht = at.fluffy(CB, 10)
    donker = plain(CB_D, 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], [cube(atlas, [-5.5, 4, -8], [11, 9, 15], vacht, {"down": donker})])]
    bones += vier_poten(atlas, "body", 1.6, -7, 3.5, [3, 4, 3], donker, 4)
    bones.append(bone("head", "body", [0, 10, -8], [
        cube(atlas, [-4.5, 7, -16], [9, 8, 8.5], vacht, {"north": gezicht(CB, eye_y=0.32, eye_r=0.16)}),
        cube(atlas, [-3.5, 7, -18.6], [7, 4, 2.6], plain(CB_L, 6), {"north": snorhaartjes(CB_L, (60, 42, 34), (120, 86, 60))})]))
    bones += paar(atlas, "ear", "head", [3.4, 15.4, -11], [3, 14.6, -11.6], [1.6, 1.4, 1.4], donker)
    bones.append(bone("vogeltje", "body", [0, 13, 0], [
        cube(atlas, [-1, 13, -1], [2, 1.6, 2.6], plain(VOGEL, 6)),
        cube(atlas, [-0.9, 14.6, -1.8], [1.8, 1.6, 1.8], plain(VOGEL, 6), {"north": gezicht(VOGEL, eye_y=0.45, eye_r=0.18, blush=False)}),
        cube(atlas, [-0.3, 15, -2.3], [0.6, 0.5, 0.5], plain((250, 150, 60), 4))]))
    return geo("capibara", bones, atlas, 1.3, 1.2)


def capibara_anims():
    a = at.animaties(legs4=POTEN4, oren=True)
    a["animations"]["idle"]["bones"]["vogeltje"] = {"rotation": at.kf((0, [0, 0, 0]), (1.0, [0, 30, 0]), (2.0, [0, -30, 0]), (3.0, [0, 0, 0]))}
    return a


# =====================================================================================================================
# krokodil (vriendelijk: grote glimlach, guh-oogjes bovenop, nooit eng)
# =====================================================================================================================
KR = (104, 156, 86)
KR_L = (218, 224, 152)
KR_D = (72, 114, 64)
TANDJE = (252, 250, 240)


def krokodil(atlas):
    huid = plain(KR, 8)
    licht = plain(KR_L, 6)
    rug = rijen(KR, KR_D)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4, 0], [cube(atlas, [-5, 1.5, -8], [10, 5, 16], huid, {"up": rug, "down": licht})])]
    bones += vier_poten(atlas, "body", 4.4, -7, 4, [3, 2.5, 3], huid, 2.5)
    bones.append(bone("tail", "body", [0, 4, 8], [cube(atlas, [-3.5, 1.8, 8], [7, 4, 8], huid, {"up": rug, "down": licht})]))
    bones.append(bone("tail_tip", "tail", [0, 3.5, 16], [cube(atlas, [-2, 2.2, 16], [4, 3, 7], huid, {"up": rug, "down": licht})]))

    def neusgaatjes(W, H, rng, a):
        for sx in (-1, 1):
            disc(a, W * (0.5 + sx * 0.18), H * 0.1, W * 0.07, KR_D, 1.0, ry=H * 0.03)
    bones.append(bone("head", "body", [0, 4.5, -8], [
        cube(atlas, [-4.5, 3, -13], [9, 5.5, 5], huid, {"north": gezicht(KR, eye_y=0.42, eye_dx=0.27, eye_r=0.2), "up": rug}),
        cube(atlas, [-3.5, 1.6, -21], [7, 3.4, 8], huid,
             {"north": neusje(KR, KR_D, mond=True), "east": glimlach_zij(KR, KR_D, TANDJE, "east"),
              "west": glimlach_zij(KR, KR_D, TANDJE, "west"), "up": plain(KR, 8, neusgaatjes), "down": licht})]))
    return geo("krokodil", bones, atlas, 1.3, 0.8)


def krokodil_anims():
    a = at.animaties(legs4=POTEN4, staart="tail")
    a["animations"]["walk"]["bones"]["tail_tip"] = {"rotation": at.kf((0, [0, -14, 0]), (0.4, [0, 14, 0]), (0.8, [0, -14, 0]))}
    return a


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "zebra":
        a = Atlas(310); g = zebra(a); anims = zebra_anims()
        img = at.schrijf(d, soort, g, a, anims)
        ak = Atlas(311); at.schrijf(d, soort + "_baby", baby(zebra(ak, veulen=True), 0.6, 0.8), ak)
    elif soort == "stokstaartje":
        a = Atlas(320); g = stokstaartje(a); anims = stokstaartje_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.62, 0.82), a)
    elif soort == "zeekoe":
        a = Atlas(330); g = zeekoe(a); anims = zeekoe_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.58, 0.8), a)
    elif soort == "nijlpaard":
        a = Atlas(340); g = nijlpaard(a); anims = nijlpaard_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_man", groter(g, 1.15, "nijlpaard_man"), a)
        at.schrijf(d, soort + "_baby", baby(g, 0.58, 0.8), a)
    elif soort == "capibara":
        a = Atlas(350); g = capibara(a); anims = capibara_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8, drop=("vogeltje",)), a)
    elif soort == "krokodil":
        a = Atlas(360); g = krokodil(a); anims = krokodil_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.85), a)
    else:
        raise SystemExit(f"onbekende soort {soort}")
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort)


SOORTEN = ["zebra", "stokstaartje", "zeekoe", "nijlpaard", "capibara", "krokodil"]
NAMEN = {"zebra": "Zebra", "stokstaartje": "Stokstaartje", "zeekoe": "Zeekoe", "nijlpaard": "Nijlpaard", "capibara": "Capibara",
         "krokodil": "Krokodil"}


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
        d.text((20, 18), f"{NAMEN[soort]}  (safari-dieren, batch 3)", font=bl.font(30, True), fill=(40, 30, 40))
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
        d.text((20, 18), "Safari-dieren batch 3: alle zes naast een guh (volwassen achter, jong ervoor)", font=bl.font(28, True), fill=(40, 30, 40))
        d.text((20, 56), "Zebra · Stokstaartje · Zeekoe · Nijlpaard · Capibara · Krokodil (van links naar rechts, na de guh)",
               font=bl.font(17), fill=(80, 70, 80))
        pad = os.path.join(UIT, "batch3_overzicht.png")
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
