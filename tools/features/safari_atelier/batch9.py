"""
Safari-voorwerk, batch 9 (extra dieren, ronde 38-39; bouwdieren): gibbon, gekko, glimworm, prieelvogel.
Zelfde stijl, werkwijze en bestandsformaat als batch 1 (dieren.py); hergebruikt atelier/dieren/blad zonder ze aan te passen.

    python safari-ontwerp/dieren/_bron/batch9.py [soort ...] [overzicht]     (geen argumenten: alle vier + vellen + overzicht)

Per soort in safari-ontwerp/dieren/<soort>/: <soort>.geo.json/.png (vrouwtje, of beide), <soort>_man.* (alleen prieelvogel:
glanzend blauwzwart met violette ogen), <soort>_baby.*, <soort>.animation.json (idle, walk, blij, eet + per soort: gibbon slinger,
gekko klim, glimworm hang, prieelvogel dans), <soort>.bbmodel en <soort>_voorstel.png. De glimworm heeft ook glimworm_glow.png
(+ glimworm_baby_glow.png): alleen het lichtgevende staarteind, voor een emissive laag in de game. Plus dieren/batch9_overzicht.png.
"""
import json
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, gezicht       # noqa: E402
from dieren import baby, paar, POTEN4                                   # noqa: E402

UIT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "build", "safari_atelier"))  # staging; tools/features/safari_dieren.py copies into the assets
SOORTEN = ["gibbon", "gekko", "glimworm", "prieelvogel"]
NAMEN = {"gibbon": "Gibbon", "gekko": "Gekko", "glimworm": "Glimworm", "prieelvogel": "Prieelvogel"}
NUL = [0, 0, 0]


def verborgen(*botten):
    """Kanalen die een bot in een animatie onzichtbaar houden (schaal 0)."""
    return {b: {"scale": at.kf((0, [0, 0, 0]))} for b in botten}


def extra(anims, naam, lengte, botten, loop=True):
    anims["animations"][naam] = {"loop": loop, "animation_length": lengte, "bones": botten}


def verberg_in_standaard(anims, *botten):
    for a in ("idle", "walk", "blij", "eet"):
        anims["animations"][a]["bones"].update(verborgen(*botten))


# =====================================================================================================================
# gibbon: rond pluizig lijf, heel lange armen, donker gezichtsmasker met een lichte ring
# =====================================================================================================================
GB = (222, 196, 150)
GB_L = (240, 222, 186)
GB_MASKER = (52, 42, 44)
GB_HAND = (92, 74, 66)


def gibbon(atlas):
    vacht = at.fluffy(GB, 14)

    def masker(W, H, rng, a):
        a[..., :3] = np.array(GB) + rng.normal(0, 4, (H, W, 1))
        disc(a, W / 2, H * 0.52, W * 0.49, GB_L, 1.0, ry=H * 0.47)            # de lichte ring
        disc(a, W / 2, H * 0.54, W * 0.36, GB_MASKER, 1.0, ry=H * 0.34)      # het donkere masker
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 9, 0], [cube(atlas, [-4.25, 5, -3.5], [8.5, 8, 7], vacht, {"north": at.buik(GB, GB_L)})])]
    bones += paar(atlas, "leg", "body", [1.6, 5, 0], [0.5, 0, -1.4], [2.4, 5, 2.8], vacht, {"down": plain(GB_HAND, 6)})
    for side, sx in (("left", 1), ("right", -1)):
        o = [4.25, 1.6, -1] if sx > 0 else at.mirror_x([4.25, 1.6, -1], [2, 11, 2])
        h = [4.05, 0.4, -1.2] if sx > 0 else at.mirror_x([4.05, 0.4, -1.2], [2.4, 1.6, 2.4])
        bones.append(bone(f"arm_{side}", "body", [sx * 5.25, 12.5, 0],
                          [cube(atlas, o, [2, 11, 2], vacht), cube(atlas, h, [2.4, 1.6, 2.4], plain(GB_HAND, 6))]))
    bones.append(bone("head", "body", [0, 13, 0], [
        cube(atlas, [-3.2, 12.6, -3.6], [6.4, 6.2, 6.2], vacht,
             {"north": gezicht(GB_MASKER, eye_y=0.46, eye_r=0.16, spots=masker)})]))
    return geo("gibbon", bones, atlas, 1.0, 1.4)


def gibbon_anims():
    L = 0.8
    armen = {"arm_left": {"rotation": at.kf((0, [165, 0, -12]), (L / 2, [175, 0, -4]), (L, [165, 0, -12]))},
             "arm_right": {"rotation": at.kf((0, [175, 0, 4]), (L / 2, [165, 0, 12]), (L, [175, 0, 4]))}}
    zwaai = {"arm_left": {"rotation": at.kf((0, [0, 0, 0]), (1.0, [6, 0, -4]), (2.0, [-6, 0, 0]), (3.0, [0, 0, 0]))},
             "arm_right": {"rotation": at.kf((0, [0, 0, 0]), (1.0, [-6, 0, 4]), (2.0, [6, 0, 0]), (3.0, [0, 0, 0]))}}
    anims = at.animaties(legs2=["leg_left", "leg_right"], extra_idle=zwaai, extra_walk=armen)
    # slinger: aan beide armen boven zich, het lijf zwaait heen en weer, beentjes bungelen mee
    extra(anims, "slinger", 2.0, {
        "root": {"position": at.kf((0, [0, 9, 0]))},
        "arm_left": {"rotation": at.kf((0, [180, 0, -8]))},
        "arm_right": {"rotation": at.kf((0, [180, 0, 8]))},
        "body": {"rotation": at.kf((0, [22, 0, 0]), (1.0, [-22, 0, 0]), (2.0, [22, 0, 0]))},
        "leg_left": {"rotation": at.kf((0, [-15, 0, 0]), (1.0, [15, 0, 0]), (2.0, [-15, 0, 0]))},
        "leg_right": {"rotation": at.kf((0, [-10, 0, 0]), (1.0, [20, 0, 0]), (2.0, [-10, 0, 0]))}})
    return anims


# =====================================================================================================================
# gekko: plat lijf, grote kop met grote ogen, poten opzij met ronde kleefteentjes
# =====================================================================================================================
GK = (118, 196, 78)
GK_D = (84, 150, 56)
GK_BUIK = (228, 236, 186)
GK_STIP = (242, 128, 58)
GK_TEEN = (196, 232, 150)


def gekko(atlas):
    def stippen(W, H, rng, a):
        for _ in range(max(2, W * H // 260)):
            r = rng.uniform(0.9, 1.6) * at.S
            disc(a, rng.uniform(0, W), rng.uniform(0, H), r, GK_STIP, 1.0)
    huid = plain(GK, 8, stippen)
    effen = plain(GK, 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2, 0], [cube(atlas, [-2.5, 1, -4], [5, 2.6, 9], huid, {"down": plain(GK_BUIK, 6)})])]
    for voor, z in (("front", -2.6), ("back", 3.2)):
        for side, sx in (("left", 1), ("right", -1)):
            poot = [2.3, 1.1, z - 0.7]
            teen = [4.6, 0, z - 1.4]
            if sx < 0:
                poot, teen = at.mirror_x(poot, [2.6, 1.2, 1.4]), at.mirror_x(teen, [2.2, 1.1, 2.8])
            bones.append(bone(f"leg_{voor}_{side}", "body", [sx * 2.4, 1.7, z],
                              [cube(atlas, poot, [2.6, 1.2, 1.4], effen),
                               cube(atlas, teen, [2.2, 1.1, 2.8], plain(GK_TEEN, 6))]))
    bones.append(bone("head", "body", [0, 2.6, -4], [
        cube(atlas, [-3, 1, -9], [6, 4, 5], effen, {"north": gezicht(GK, eye_y=0.42, eye_dx=0.27, eye_r=0.21),
                                                   "down": plain(GK_BUIK, 6)})]))
    bones.append(bone("tail", "body", [0, 2.2, 5], [cube(atlas, [-1.5, 1.2, 5], [3, 2, 5], huid)]))
    bones.append(bone("tail_tip", "tail", [0, 2.1, 10], [cube(atlas, [-0.9, 1.4, 10], [1.8, 1.4, 5], plain(GK_D, 6))]))
    return geo("gekko", bones, atlas, 1.0, 0.6)


def gekko_anims():
    L = 0.8
    walk = {"body": {"rotation": at.kf((0, [0, 9, 0]), (L / 2, [0, -9, 0]), (L, [0, 9, 0]))},
            "tail_tip": {"rotation": at.kf((0, [0, -16, 0]), (L / 2, [0, 16, 0]), (L, [0, -16, 0]))}}
    idle = {"tail_tip": {"rotation": at.kf((0, [0, 0, 0]), (1.5, [0, 12, 0]), (3.0, [0, 0, 0]))}}
    anims = at.animaties(legs4=POTEN4, staart="tail", extra_idle=idle, extra_walk=walk)
    # klim: het hele beestje omhoog tegen een muur (buik naar de muur), de teentjes stappen om beurten
    poten = {}
    for i, n in enumerate(POTEN4):
        s = 22 if i in (0, 3) else -22
        poten[n] = {"rotation": at.kf((0, [0, s, 0]), (0.5, [0, -s, 0]), (1.0, [0, s, 0]))}
    extra(anims, "klim", 1.0, {
        "root": {"rotation": at.kf((0, [-90, 0, 0])), "position": at.kf((0, [0, 5, 0]), (0.5, [0, 5.6, 0]), (1.0, [0, 6.2, 0]))},
        "tail_tip": {"rotation": at.kf((0, [0, -12, 0]), (0.5, [0, 12, 0]), (1.0, [0, -12, 0]))}, **poten})
    return anims


# =====================================================================================================================
# glimworm: klein zacht larfje met een blauw gloeiend staarteind; bot "draadje" voor hangen
# =====================================================================================================================
GW = (236, 226, 196)
GW_D = (214, 200, 166)
GLOED = (112, 214, 255)
GLOED_KERN = (214, 246, 255)


def glimworm(atlas):
    def ringetjes(W, H, rng, a):
        for i in range(1, 4):
            x = int(W * i / 4)
            a[:, max(0, x - 1):x + 1, :3] = np.array(GW_D)
    zacht = plain(GW, 6, ringetjes)
    gloed = plain(GLOED, 6, lambda W, H, rng, a: disc(a, W / 2, H / 2, W * 0.32, GLOED_KERN, 0.9, ry=H * 0.32))
    staart_eind = cube(atlas, [-1.05, 0.1, 5.4], [2.1, 2.0, 2.2], gloed)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 1.4, 0], [cube(atlas, [-1.6, 0, -3.2], [3.2, 2.8, 3.4], zacht),
                                                cube(atlas, [-1.45, 0, 0], [2.9, 2.6, 3], zacht)])]
    bones.append(bone("head", "body", [0, 1.6, -3], [
        cube(atlas, [-2, 0, -6.6], [4, 3.4, 3.6], plain(GW, 6), {"north": gezicht(GW, eye_y=0.44, eye_r=0.2)})]))
    bones.append(bone("tail", "body", [0, 1.2, 3], [cube(atlas, [-1.2, 0, 3], [2.4, 2.3, 2.6], zacht), staart_eind]))
    # draadje: in rust een piepklein knopje achter op het staarteind; "hang" rekt het (schaal z) uit tot een zijden draadje
    bones.append(bone("draadje", "tail", [0, 1.1, 7.6], [cube(atlas, [-0.2, 0.9, 7.6], [0.4, 0.4, 0.5], plain((244, 248, 252), 4))]))
    return geo("glimworm", bones, atlas, 0.6, 0.4), staart_eind


def glimworm_anims():
    walk = {"body": {"scale": at.kf((0, [1, 1, 1]), (0.4, [0.92, 1.06, 1.12]), (0.8, [1, 1, 1]))},
            "head": {"position": at.kf((0, [0, 0, 0]), (0.4, [0, 0, -0.6]), (0.8, [0, 0, 0]))},
            "tail": {"position": at.kf((0, [0, 0, 0]), (0.4, [0, 0, 0.6]), (0.8, [0, 0, 0]))}}
    anims = at.animaties(extra_walk=walk)
    # hang: kop naar beneden aan een zijden draadje (staarteind boven), zachtjes slingerend
    extra(anims, "hang", 3.0, {
        "root": {"rotation": at.kf((0, [90, 0, 0]), (1.5, [90, 0, 8]), (3.0, [90, 0, 0])), "position": at.kf((0, [0, 10, 0]))},
        "draadje": {"scale": at.kf((0, [1, 1, 30]))},
        "head": {"rotation": at.kf((0, [0, 0, 0]), (1.0, [0, 10, 0]), (2.0, [0, -10, 0]), (3.0, [0, 0, 0]))}})
    return anims


def glow_png(atlas, cubes, pad):
    """Alleen de vlakken van de lichtgevende kubussen, de rest doorzichtig (de emissive laag)."""
    src = np.asarray(atlas.image().convert("RGBA"))
    out = np.zeros_like(src)
    for c in cubes:
        for f in c["uv"].values():
            (u, v), (w, h) = f["uv"], f["uv_size"]
            x0, y0 = int(u * at.S), int(v * at.S)
            x1, y1 = int(np.ceil((u + w) * at.S)), int(np.ceil((v + h) * at.S))
            out[y0:y1, x0:x1] = src[y0:y1, x0:x1]
    Image.fromarray(out).save(pad)


# =====================================================================================================================
# prieelvogel: compact zangvogeltje met kort snaveltje; ♂ glanzend blauwzwart met violette ogen, ♀ olijfgroen/bruin
# =====================================================================================================================
PV_M = (36, 38, 74)
PV_M_GLANS = (78, 92, 170)
PV_M_SNAVEL = (232, 222, 150)
PV_V = (122, 132, 72)
PV_V_VLEUGEL = (134, 102, 62)
PV_V_BUIK = (222, 216, 170)
PV_V_SNAVEL = (70, 66, 54)
PV_POOT = (176, 160, 130)
BLAUW = (62, 112, 232)


def violet(painter):
    """Maakt de blauw-naar-turquoise ring van de guh-ogen violet (het prieelvogelmannetje)."""
    def paint(W, H, rng):
        a = painter(W, H, rng).astype(np.float32)
        r, g, b = a[..., 0], a[..., 1], a[..., 2]
        ring = (b >= 190) & (g >= 95) & (r <= 100)
        t = np.clip((g - 100) / 110, 0, 1)
        a[ring, 0] = (118 + 60 * t)[ring]
        a[ring, 1] = (48 + 40 * t)[ring]
        a[ring, 2] = (196 + 40 * t)[ring]
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def prieelvogel(atlas, man=False):
    if man:
        def glans(W, H, rng, a):
            for _ in range(max(2, W * H // 90)):
                disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.5, 1.0) * at.S, PV_M_GLANS, 0.55)
        veer = plain(PV_M, 8, glans)
        vleugel, buik, snavel = veer, veer, plain(PV_M_SNAVEL, 4)
        kop = violet(gezicht(PV_M, eye_y=0.44, eye_r=0.18))
    else:
        def schubjes(W, H, rng, a):
            for y in range(0, H, 3):
                for x in range((y // 3) % 2 * 2, W, 4):
                    a[y, x, :3] = np.array(PV_V_BUIK) * 0.82
        veer = plain(PV_V, 8)
        vleugel, buik, snavel = plain(PV_V_VLEUGEL, 8), plain(PV_V_BUIK, 6, schubjes), plain(PV_V_SNAVEL, 4)
        kop = gezicht(PV_V, eye_y=0.44, eye_r=0.18)
    poot = plain(PV_POOT, 4)
    bones = [bone("root", None, [0, 0, 0], [])]
    for side, sx in (("left", 1), ("right", -1)):
        p, v = [0.7, 0, -0.5], [0.3, 0, -1.6]
        if sx < 0:
            p, v = at.mirror_x(p, [1, 3.2, 1]), at.mirror_x(v, [1.8, 0.5, 2.2])
        bones.append(bone(f"leg_{side}", "root", [sx * 1.2, 3.2, 0], [cube(atlas, p, [1, 3.2, 1], poot), cube(atlas, v, [1.8, 0.5, 2.2], poot)]))
    bones.append(bone("body", "root", [0, 6, 0], [cube(atlas, [-3, 3, -3.5], [6, 6, 7], veer, {"north": buik, "down": buik})]))
    bones += paar(atlas, "wing", "body", [3, 8.5, -1], [3, 4, -2.5], [1, 4.6, 6.4], vleugel)
    bones.append(bone("tail", "body", [0, 5.5, 3.5], [cube(atlas, [-1.8, 4.6, 3.4], [3.6, 1.2, 5], vleugel)], rotation=[-18, 0, 0]))
    bones.append(bone("head", "body", [0, 9, -2], [
        cube(atlas, [-3, 8.6, -6.2], [6, 5.6, 5.6], veer, {"north": kop}),
        cube(atlas, [-0.9, 9.6, -7.8], [1.8, 1.4, 1.6], snavel)]))
    # blauw_ding: een blauw bloemetje/besje in de snavel (het pronkstuk bij de dans)
    bones.append(bone("blauw_ding", "head", [0, 9.6, -8.6], [cube(atlas, [-0.65, 8.9, -9.3], [1.3, 1.3, 1.3], plain(BLAUW, 6,
                                                                lambda W, H, rng, a: disc(a, W / 2, H / 2, W * 0.22, (190, 214, 255), 0.9)))]))
    return geo("prieelvogel_man" if man else "prieelvogel", bones, atlas, 0.7, 0.9)


def prieelvogel_anims():
    anims = at.animaties(legs2=["leg_left", "leg_right"], staart="tail",
                         extra_idle={"head": {"rotation": at.kf((0, [0, 0, 0]), (0.8, [0, 0, 14]), (1.2, [0, 0, 0]),
                                                                (2.2, [0, 0, -14]), (2.6, [0, 0, 0]), (3.0, [0, 0, 0]))}})
    verberg_in_standaard(anims, "blauw_ding")
    # dans: het blauwe dingetje pronkend in de snavel, vleugels open, op en neer wippen en de kop heen en weer gooien
    extra(anims, "dans", 1.6, {
        "blauw_ding": {"scale": at.kf((0, [1, 1, 1]), (0.4, [1.25, 1.25, 1.25]), (0.8, [1, 1, 1]), (1.2, [1.25, 1.25, 1.25]), (1.6, [1, 1, 1]))},
        "body": {"position": at.kf((0, [0, 0, 0]), (0.2, [0, 1.2, 0]), (0.4, [0, 0, 0]), (0.6, [0, 1.2, 0]), (0.8, [0, 0, 0]),
                                   (1.0, [0, 1.2, 0]), (1.2, [0, 0, 0]), (1.4, [0, 1.2, 0]), (1.6, [0, 0, 0])),
                 "rotation": at.kf((0, [0, 0, 0]), (0.4, [0, 25, 0]), (0.8, [0, 0, 0]), (1.2, [0, -25, 0]), (1.6, [0, 0, 0]))},
        "wing_left": {"rotation": at.kf((0, [0, 0, 0]), (0.2, [0, 0, -55]), (0.4, [0, 0, -20]), (0.6, [0, 0, -55]), (0.8, [0, 0, 0]),
                                        (1.0, [0, 0, -55]), (1.2, [0, 0, -20]), (1.4, [0, 0, -55]), (1.6, [0, 0, 0]))},
        "wing_right": {"rotation": at.kf((0, [0, 0, 0]), (0.2, [0, 0, 55]), (0.4, [0, 0, 20]), (0.6, [0, 0, 55]), (0.8, [0, 0, 0]),
                                         (1.0, [0, 0, 55]), (1.2, [0, 0, 20]), (1.4, [0, 0, 55]), (1.6, [0, 0, 0]))},
        "head": {"rotation": at.kf((0, [-10, 0, 0]), (0.4, [-20, 0, 18]), (0.8, [-10, 0, 0]), (1.2, [-20, 0, -18]), (1.6, [-10, 0, 0]))},
        "tail": {"rotation": at.kf((0, [0, 0, 0]), (0.4, [-25, 0, 0]), (0.8, [0, 0, 0]), (1.2, [-25, 0, 0]), (1.6, [0, 0, 0]))}})
    return anims


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "gibbon":
        a = Atlas(910); g = gibbon(a); anims = gibbon_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.58, 0.8), a)
    elif soort == "gekko":
        a = Atlas(920); g = gekko(a); anims = gekko_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.82), a)
    elif soort == "glimworm":
        a = Atlas(930); g, eind = glimworm(a); anims = glimworm_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.62, 0.85), a)
        glow_png(a, [eind], os.path.join(d, "glimworm_glow.png"))
        glow_png(a, [eind], os.path.join(d, "glimworm_baby_glow.png"))   # de baby deelt de atlas (zelfde uv)
    elif soort == "prieelvogel":
        a = Atlas(940); g = prieelvogel(a); anims = prieelvogel_anims()
        img = at.schrijf(d, soort, g, a, anims)
        am = Atlas(941); at.schrijf(d, soort + "_man", prieelvogel(am, man=True), am)
        at.schrijf(d, soort + "_baby", baby(g, 0.62, 0.84), a)
    else:
        raise SystemExit(f"onbekende soort {soort}")
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort)


# =====================================================================================================================
# vellen (de vel/overzicht-functies van blad.py, met de batch-9-namen en -titels)
# =====================================================================================================================
def vellen(soorten, overzicht=True):
    import blad as bl
    from PIL import ImageDraw
    bl.NAMEN.update(NAMEN)

    def vel(soort):
        heeft_man = os.path.exists(os.path.join(UIT, soort, f"{soort}_man.geo.json"))
        varianten = [("", "volwassen" + (" (vrouwtje)" if heeft_man else ""))]
        if heeft_man:
            varianten.append(("man", "mannetje"))
        varianten.append(("baby", "jong"))
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
        d.text((20, 18), f"{NAMEN[soort]}  (safari-dieren, batch 9: bouwdieren)", font=bl.font(30, True), fill=(40, 30, 40))
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
    items.insert(5, ("prieelvogel_man", bl.dier_quads("prieelvogel", "man"), None))
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
    d.text((20, 18), "Safari-dieren batch 9 (bouwdieren): naast een guh (volwassen achter, jong ervoor)", font=bl.font(28, True), fill=(40, 30, 40))
    d.text((20, 56), "Gibbon · Gekko · Glimworm · Prieelvogel ♀ · Prieelvogel ♂ (van links naar rechts, na de guh)",
           font=bl.font(17), fill=(80, 70, 80))
    pad = os.path.join(UIT, "batch9_overzicht.png")
    img.convert("RGB").save(pad)
    print(pad)


if __name__ == "__main__":
    args = sys.argv[1:]
    soorten = [a for a in args if a != "overzicht"] or SOORTEN
    for s in soorten:
        maak(s)
    vellen(soorten, overzicht=(not args or "overzicht" in args))
