"""
Safari-dieren batch 11 (testronde 4, ronde 50-51: het Kriebelhuis + de delta): spookdiertje, plompe lori, zeepaardje,
pijlstaartrog, zeeschildpad en pelikaan. Zelfde stijl, werkwijze en bestandsformaat als batch 1-9 (atelier/dieren/blad
alleen geimporteerd, niet aangepast). De gebruiker houdt van AFWIJKENDE VORMEN: een reuzenogig spookdiertje aan een takje,
een kogelronde lori die een tak knuffelt, een rechtop zwevend zeepaardje met krulstaart, een platte rog die 'vliegt', een
zeeschildpad met lange flippers en een pelikaan met een keelzak die kan opbollen.

    python tools/features/safari_atelier/batch11.py [soort ...] [--geen-vellen]

Per soort in build/safari_atelier/<soort>/ (zoals safari_dieren.py dat verwacht): <soort>.geo.json/.png, <soort>_baby.*,
<soort>.animation.json (idle, walk, blij, eet + per soort: spookdiertje draai/spring, plompe_lori knuffel, zeepaardje
zwem/krul, pijlstaartrog vliegen, zeeschildpad zwem, pelikaan slok), <soort>.bbmodel en <soort>_voorstel.png. Daarna:
dezelfde modellen met dichte oogjes (<naam>_slaap.png), de assets in src/main/resources (via safari_dieren.kopieer en
extra_animaties: slaap + rust), en batch11_overzicht.png. NIET in soorten.json (dat gebeurt bij de integratie).
"""
import json
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, disc, gezicht       # noqa: E402
from dieren import baby, paar, vier_poten, POTEN4                       # noqa: E402

UIT = os.path.abspath(os.path.join(HIER, "..", "..", "..", "build", "safari_atelier"))
SOORTEN = ["spookdiertje", "plompe_lori", "zeepaardje", "pijlstaartrog", "zeeschildpad", "pelikaan"]
NAMEN = {"spookdiertje": "Spookdiertje", "plompe_lori": "Plompe lori", "zeepaardje": "Zeepaardje",
         "pijlstaartrog": "Pijlstaartrog", "zeeschildpad": "Zeeschildpad", "pelikaan": "Pelikaan"}
kf = at.kf
S = at.S
LEEG = at.transparent


def verborgen(*botten):
    return {b: {"scale": kf((0, [0, 0, 0]))} for b in botten}


def extra(anims, naam, lengte, botten, loop=True):
    anims["animations"][naam] = {"loop": loop, "animation_length": lengte, "bones": botten}


def verberg_in_standaard(anims, *botten):
    for a in ("idle", "walk", "blij", "eet"):
        anims["animations"][a]["bones"].update(verborgen(*botten))


def oogringen(kleur, eye_y, eye_dx, eye_r, groot=1.5):
    """Donkere ringen rond de ogen (lori): op de vacht, onder de guh-oogjes."""
    def f(W, H, rng, a):
        for sx in (-1, 1):
            disc(a, W / 2 + sx * eye_dx * W, H * eye_y, eye_r * W * groot, kleur, 1.0, ry=eye_r * W * groot * 1.12)
    return f


# =====================================================================================================================
# spookdiertje (tarsier): reuzenogen, piepklein lijfje, lange vingers om een takje, lange dunne staart met pluim
# =====================================================================================================================
TS = (176, 144, 112)
TS_L = (222, 198, 168)
TS_D = (128, 100, 76)
TS_OOR = (238, 186, 170)
TAK = (110, 82, 56)


def spookdiertje(atlas):
    vacht = at.fluffy(TS, 12)
    bones = [bone("root", None, [0, 0, 0], [])]
    # het takje: verticaal voor het diertje, het klampt zich eraan vast (verborgen bij lopen/springen)
    bones.append(bone("takje", "root", [0, 0, -4.6], [cube(atlas, [-0.6, 0, -5.2], [1.2, 8.6, 1.2], plain(TAK, 8))]))
    bones.append(bone("body", "root", [0, 5, 0], [cube(atlas, [-2, 3, -1.6], [4, 5, 3.4], vacht, {"north": at.buik(TS, TS_L)})]))
    # lange achterpoten (gevouwen, kikkerachtig) met lange voetjes
    for side, sx in (("left", 1), ("right", -1)):
        dij, voet = [1.2, 1.4, -0.6], [1.0, 0, -2.6]
        if sx < 0:
            dij, voet = at.mirror_x(dij, [1.6, 3, 2.6]), at.mirror_x(voet, [1.4, 0.6, 3.6])
        bones.append(bone(f"leg_{side}", "body", [sx * 2, 4, 0.2], [cube(atlas, dij, [1.6, 3, 2.6], vacht),
                                                                      cube(atlas, voet, [1.4, 0.6, 3.6], plain(TS_D, 6))]))
    # lange dunne armpjes naar voren, met lange vingertjes om het takje
    for side, sx in (("left", 1), ("right", -1)):
        arm, vinger = [0.6, 5.6, -4.2], [0.1, 5.4, -5.6]
        if sx < 0:
            arm, vinger = at.mirror_x(arm, [0.8, 0.8, 3.2]), at.mirror_x(vinger, [0.9, 1.2, 0.5])
        bones.append(bone(f"arm_{side}", "body", [sx * 1.6, 6.6, -1.2], [cube(atlas, arm, [0.8, 0.8, 3.2], vacht),
                                                                          cube(atlas, vinger, [0.9, 1.2, 0.5], plain(TS_D, 4))]))
    # de kop: groot, met GIGANTISCHE ogen en dunne ronde oortjes
    kop = gezicht(TS, eye_y=0.46, eye_dx=0.255, eye_r=0.235)
    oor = at.vorm_oor(TS, TS_OOR, punt=0.1)
    bones.append(bone("head", "body", [0, 8, -0.2], [
        cube(atlas, [-3.6, 7.8, -3.6], [7.2, 6, 5.2], vacht, {"north": kop}),
        cube(atlas, [2.6, 12.4, -1.4], [2.2, 2.6, 0.4], LEEG, {"north": oor, "south": oor}),
        cube(atlas, [-4.8, 12.4, -1.4], [2.2, 2.6, 0.4], LEEG, {"north": oor, "south": oor})]))
    # lange dunne staart naar achteren-onder, met een pluimpje
    bones.append(bone("tail", "body", [0, 4, 1.6], [cube(atlas, [-0.35, 3.6, 1.6], [0.7, 0.7, 11], plain(TS_D, 6)),
                                                    cube(atlas, [-0.7, 3.3, 12.2], [1.4, 1.3, 3.2], at.fluffy(TS_D, 10))],
                      rotation=[-30, 0, 0]))                                    # hangt naar achteren-onder
    return geo("spookdiertje", bones, atlas, 1.0, 1.0)


def spookdiertje_anims():
    idle = {"head": {"rotation": kf((0, [0, 0, 0]), (1.2, [0, 0, 0]), (1.6, [0, 120, 0]), (2.6, [0, 120, 0]),
                                    (3.0, [0, 0, 0]), (4.0, [0, 0, 0]))},           # het beroemde koppie dat ver omdraait
            "tail": {"rotation": kf((0, [-30, 0, 0]), (2.0, [-26, 8, 0]), (4.0, [-30, 0, 0]))}}
    an = at.animaties(legs2=["leg_left", "leg_right"], extra_idle=idle)
    an["animations"]["idle"]["animation_length"] = 4.0
    # lopen = hupjes (de poten tegelijk), het takje weg
    L = 0.6
    an["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": {
        "body": {"position": kf((0, [0, 0, 0]), (L / 2, [0, 2.2, 0]), (L, [0, 0, 0]))},
        "leg_left": {"rotation": kf((0, [0, 0, 0]), (L / 2, [-40, 0, 0]), (L, [0, 0, 0]))},
        "leg_right": {"rotation": kf((0, [0, 0, 0]), (L / 2, [-40, 0, 0]), (L, [0, 0, 0]))},
        "arm_left": {"rotation": kf((0, [-30, 0, 0]))}, "arm_right": {"rotation": kf((0, [-30, 0, 0]))}}}
    an["animations"]["walk"]["bones"].update(verborgen("takje"))
    an["animations"]["blij"]["bones"].update(verborgen("takje"))
    extra(an, "draai", 2.0, {"head": {"rotation": kf((0, [0, 0, 0]), (0.5, [0, 170, 0]), (1.2, [0, 170, 0]), (1.6, [0, -60, 0]),
                                                     (2.0, [0, 0, 0]))}}, loop=False)
    extra(an, "spring", 0.9, {
        "takje": {"scale": kf((0, [0, 0, 0]))},
        "body": {"position": kf((0, [0, 0, 0]), (0.15, [0, -0.8, 0]), (0.45, [0, 7, -3]), (0.9, [0, 0, -6]))},
        "leg_left": {"rotation": kf((0, [20, 0, 0]), (0.15, [30, 0, 0]), (0.45, [-70, 0, 0]), (0.9, [0, 0, 0]))},
        "leg_right": {"rotation": kf((0, [20, 0, 0]), (0.15, [30, 0, 0]), (0.45, [-70, 0, 0]), (0.9, [0, 0, 0]))},
        "tail": {"rotation": kf((0, [-30, 0, 0]), (0.45, [5, 0, 0]), (0.9, [-30, 0, 0]))}}, loop=False)
    return an


# =====================================================================================================================
# plompe lori: kogelrond en pluizig, grote ronde ogen met donkere ringen, heel traag; "knuffel" (een tak omarmen)
# =====================================================================================================================
LO = (196, 160, 122)
LO_L = (232, 212, 186)
LO_D = (110, 76, 54)
LO_STREEP = (130, 94, 66)


def plompe_lori(atlas):
    def rugstreep(W, H, rng, a):
        a[:, int(W * 0.42):int(W * 0.58), :3] = np.array(LO_STREEP) + rng.normal(0, 4, (H, int(W * 0.58) - int(W * 0.42), 1))
    vacht = at.fluffy(LO, 14)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 7, 0], [cube(atlas, [-4.2, 2.4, -3.6], [8.4, 7.4, 7.6], vacht,
                                                    {"north": at.buik(LO, LO_L), "up": at.met(at.fluffy(LO, 14), rugstreep)})])]
    bones += vier_poten(atlas, "body", 1.4, -2.8, 1.6, [2.2, 3, 2.2], vacht, 3.2, voet=(0.6, plain(LO_D, 6)))
    ey, edx, er = 0.48, 0.255, 0.17
    kop = gezicht(LO, eye_y=ey, eye_dx=edx, eye_r=er, spots=oogringen(LO_D, ey, edx, er, groot=1.55))
    oor = at.vorm_oor(LO, LO_L, punt=0.0)
    bones.append(bone("head", "body", [0, 9.6, -2.6], [
        cube(atlas, [-3.8, 8.6, -7.6], [7.6, 6.6, 5.4], vacht, {"north": kop}),
        cube(atlas, [2.2, 13.4, -4.4], [1.8, 1.8, 0.4], LEEG, {"north": oor, "south": oor}),
        cube(atlas, [-4.0, 13.4, -4.4], [1.8, 1.8, 0.4], LEEG, {"north": oor, "south": oor})]))
    # de tak die hij knuffelt (alleen zichtbaar in "knuffel")
    bones.append(bone("tak", "body", [0, 6, -6], [cube(atlas, [-7, 5.2, -6.8], [14, 1.6, 1.6], plain(TAK, 8))]))
    return geo("plompe_lori", bones, atlas, 1.0, 1.0)


def plompe_lori_anims():
    an = at.animaties(legs4=POTEN4, extra_idle={"head": {"rotation": kf((0, [0, 0, 0]), (2.5, [0, 14, 0]), (5.0, [0, 0, 0]),
                                                                        (7.5, [0, -14, 0]), (10.0, [0, 0, 0]))}})
    an["animations"]["idle"]["animation_length"] = 10.0
    # heel traag lopen: dezelfde passen in 2,4 s
    w = an["animations"]["walk"]
    w["bones"] = {b: {ch: {str(round(float(t) * 3, 3)): v for t, v in keys.items()} for ch, keys in chans.items()}
                  for b, chans in w["bones"].items()}
    w["animation_length"] = 2.4
    verberg_in_standaard(an, "tak")
    extra(an, "knuffel", 6.0, {
        "tak": {"scale": kf((0, [1, 1, 1]))},
        "body": {"rotation": kf((0, [-8, 0, 0]), (3.0, [-12, 0, 0]), (6.0, [-8, 0, 0]))},
        "leg_front_left": {"rotation": kf((0, [-95, 0, -25]), (6.0, [-95, 0, -25]))},
        "leg_front_right": {"rotation": kf((0, [-95, 0, 25]), (6.0, [-95, 0, 25]))},
        "head": {"rotation": kf((0, [6, 0, 0]), (3.0, [10, 0, 10]), (6.0, [6, 0, 0]))}})
    return an


# =====================================================================================================================
# zeepaardje: rechtop, kop met snuitje bovenop, gekromd lijf, opgekrulde staart; zweeft rechtop; "krul" om een plantje
# =====================================================================================================================
ZP = (246, 176, 70)
ZP_D = (222, 132, 48)
ZP_L = (255, 222, 150)
ZP_VIN = (255, 236, 196)
ZEEGRAS = (78, 170, 96)


def zeepaardje(atlas):
    def ribbels(W, H, rng, a):
        for y in range(0, H, 2 * S):
            a[y:y + S // 2, :, :3] = np.array(ZP_D)
        for _ in range(max(2, W * H // 300)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.5, 0.9) * S, (255, 246, 220), 0.8)
    huid = plain(ZP, 8, ribbels)
    effen = plain(ZP, 6)
    vin = plain(ZP_VIN, 4, lambda W, H, rng, a: [a.__setitem__((slice(None), slice(x, x + 1), slice(0, 3)), ZP_D)
                                                for x in range(0, W, S)])
    bones = [bone("root", None, [0, 0, 0], [])]
    # het lijf (rechtop): buik naar voren, rug naar achteren
    bones.append(bone("body", "root", [0, 6, 0], [cube(atlas, [-1.9, 4.4, -1.8], [3.8, 5.2, 3.4], huid, {"north": plain(ZP_L, 6, ribbels)}),
                                                  cube(atlas, [-1.5, 9.4, -1.2], [3, 2.4, 2.8], huid)]))
    bones.append(bone("vin", "body", [0, 7, 1.6], [cube(atlas, [-0.15, 5.4, 1.6], [0.3, 3.6, 1.8], LEEG,
                                                        {"east": vin, "west": vin})]))
    # de kop met grote oogjes, het buisvormige snuitje en een kroontje
    bones.append(bone("head", "body", [0, 11.4, -0.2], [
        cube(atlas, [-2, 11.2, -2.4], [4, 3.6, 3.8], effen, {"north": gezicht(ZP, eye_y=0.42, eye_dx=0.25, eye_r=0.2)}),
        cube(atlas, [-0.65, 11.4, -5.4], [1.3, 1.3, 3.1], plain(ZP_D, 6)),
        cube(atlas, [-0.9, 14.8, -0.9], [1.8, 1, 1.8], plain(ZP_D, 6)),
        cube(atlas, [-0.5, 15.8, -0.5], [1, 0.7, 1], plain(ZP_L, 4))]))
    # de staart: drie steeds kleinere stukjes die naar voren omkrullen
    bones.append(bone("tail", "body", [0, 4.6, 0.4], [cube(atlas, [-1.3, 2.4, -0.8], [2.6, 2.4, 2.4], huid)], rotation=[-12, 0, 0]))
    bones.append(bone("tail_2", "tail", [0, 2.6, 0.2], [cube(atlas, [-1, 0.9, -0.4], [2, 1.9, 1.9], huid)], rotation=[-35, 0, 0]))
    bones.append(bone("tail_3", "tail_2", [0, 1.0, 0.2], [cube(atlas, [-0.75, -0.2, -0.4], [1.5, 1.4, 1.5], effen)], rotation=[-55, 0, 0]))
    bones.append(bone("tail_tip", "tail_3", [0, 0, 0.2], [cube(atlas, [-0.5, -0.8, -1.2], [1, 1, 1.2], plain(ZP_D, 6))], rotation=[-60, 0, 0]))
    # het plantje (zeegras) waar de staart omheen krult (alleen in "krul")
    bones.append(bone("plantje", "root", [0, 0, -1.5], [cube(atlas, [-0.4, 0, -1.9], [0.8, 11, 0.8], plain(ZEEGRAS, 8)),
                                                        cube(atlas, [-0.2, 4, -2.6], [0.4, 5, 0.6], plain(ZEEGRAS, 8))]))
    return geo("zeepaardje", bones, atlas, 0.6, 1.1)


def zeepaardje_anims():
    zweef = {"root": {"position": kf((0, [0, 1.5, 0]), (1.5, [0, 2.3, 0]), (3.0, [0, 1.5, 0]))},
             "vin": {"rotation": kf((0, [0, -18, 0]), (0.15, [0, 18, 0]), (0.3, [0, -18, 0]), (0.45, [0, 18, 0]), (0.6, [0, -18, 0]),
                                    (0.75, [0, 18, 0]), (0.9, [0, -18, 0]), (1.05, [0, 18, 0]), (1.2, [0, -18, 0]), (1.35, [0, 18, 0]),
                                    (1.5, [0, -18, 0]), (1.65, [0, 18, 0]), (1.8, [0, -18, 0]), (1.95, [0, 18, 0]), (2.1, [0, -18, 0]),
                                    (2.25, [0, 18, 0]), (2.4, [0, -18, 0]), (2.55, [0, 18, 0]), (2.7, [0, -18, 0]), (2.85, [0, 18, 0]),
                                    (3.0, [0, -18, 0]))},
             "tail_tip": {"rotation": kf((0, [-60, 0, 0]), (1.5, [-75, 0, 0]), (3.0, [-60, 0, 0]))}}
    an = at.animaties(extra_idle=zweef)
    L = 1.2
    zwem = {"root": {"position": kf((0, [0, 1.5, 0]), (L / 2, [0, 2.6, 0]), (L, [0, 1.5, 0]))},
            "body": {"rotation": kf((0, [-6, 0, 0]), (L / 2, [-2, 0, 0]), (L, [-6, 0, 0]))},
            "vin": {"rotation": kf((0, [0, -25, 0]), (L / 4, [0, 25, 0]), (L / 2, [0, -25, 0]), (3 * L / 4, [0, 25, 0]), (L, [0, -25, 0]))}}
    an["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": zwem}
    verberg_in_standaard(an, "plantje")
    extra(an, "zwem", L, dict(zwem, **verborgen("plantje")))
    extra(an, "krul", 4.0, {
        "plantje": {"scale": kf((0, [1, 1, 1]))},
        "root": {"position": kf((0, [0, 1.0, 0]))},
        "tail_2": {"rotation": kf((0, [-35, 0, 0]), (1.0, [-55, 0, 0]), (4.0, [-55, 0, 0]))},
        "tail_3": {"rotation": kf((0, [-55, 0, 0]), (1.0, [-85, 0, 0]), (4.0, [-85, 0, 0]))},
        "tail_tip": {"rotation": kf((0, [-60, 0, 0]), (1.0, [-95, 0, 0]), (4.0, [-95, 0, 0]))},
        "body": {"rotation": kf((0, [0, 0, 0]), (2.0, [0, 0, 6]), (4.0, [0, 0, 0]))},
        "head": {"rotation": kf((0, [0, 0, 0]), (2.0, [0, 20, 0]), (4.0, [0, 0, 0]))}})
    return an


# =====================================================================================================================
# pijlstaartrog: plat, brede vleugelvinnen die golven ("vliegen"), dunne staart met een stekeltje
# =====================================================================================================================
PR = (176, 150, 112)
PR_D = (132, 108, 80)
PR_ONDER = (246, 240, 232)


def pijlstaartrog(atlas):
    def vlekjes(W, H, rng, a):
        for _ in range(max(2, W * H // 160)):
            disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.5, 1.1) * S, PR_D, 0.7)
    rug = plain(PR, 8, vlekjes)
    onder = plain(PR_ONDER, 4)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 1, 0], [cube(atlas, [-4.2, 0.4, -5], [8.4, 1.8, 9.4], rug, {"down": onder})])]
    # de 'kop': een bolletje voorop met het gezichtje
    bones.append(bone("head", "body", [0, 1.4, -5], [cube(atlas, [-2.6, 0.4, -7.6], [5.2, 3, 2.8], rug,
                                                          {"north": gezicht(PR, eye_y=0.44, eye_dx=0.27, eye_r=0.19), "down": onder})]))
    # de vleugelvinnen: binnenstuk + smallere punt (de punt golft na)
    for side, sx in (("left", 1), ("right", -1)):
        binnen, punt = [4.2, 0.6, -3.6], [8.6, 0.8, -2.2]
        if sx < 0:
            binnen, punt = at.mirror_x(binnen, [4.4, 1.2, 7.4]), at.mirror_x(punt, [3.6, 0.8, 4.4])
        bones.append(bone(f"wing_{side}", "body", [sx * 4.2, 1.2, 0], [cube(atlas, binnen, [4.4, 1.2, 7.4], rug, {"down": onder})]))
        bones.append(bone(f"wing_{side}_tip", f"wing_{side}", [sx * 8.6, 1.2, 0], [cube(atlas, punt, [3.6, 0.8, 4.4], rug, {"down": onder})]))
    bones.append(bone("tail", "body", [0, 1.2, 4.4], [cube(atlas, [-0.35, 0.9, 4.4], [0.7, 0.7, 11], plain(PR_D, 6)),
                                                      cube(atlas, [-0.6, 1.3, 9.4], [1.2, 0.5, 2], plain((90, 74, 60), 4))]))
    return geo("pijlstaartrog", bones, atlas, 1.6, 0.4)


def pijlstaartrog_anims():
    def golf(L, a1, a2):
        return {"wing_left": {"rotation": kf((0, [0, 0, -a1]), (L / 2, [0, 0, a1]), (L, [0, 0, -a1]))},
                "wing_right": {"rotation": kf((0, [0, 0, a1]), (L / 2, [0, 0, -a1]), (L, [0, 0, a1]))},
                "wing_left_tip": {"rotation": kf((0, [0, 0, a2]), (L / 4, [0, 0, -a2]), (3 * L / 4, [0, 0, a2]), (L, [0, 0, a2]))},
                "wing_right_tip": {"rotation": kf((0, [0, 0, -a2]), (L / 4, [0, 0, a2]), (3 * L / 4, [0, 0, -a2]), (L, [0, 0, -a2]))},
                "tail": {"rotation": kf((0, [0, 8, 0]), (L / 2, [0, -8, 0]), (L, [0, 8, 0]))}}
    an = at.animaties(extra_idle=golf(3.0, 6, 8))
    L = 1.6
    vlieg = golf(L, 22, 28)
    vlieg["root"] = {"position": kf((0, [0, 1.0, 0]), (L / 2, [0, 2.0, 0]), (L, [0, 1.0, 0]))}
    an["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": vlieg}
    extra(an, "vliegen", L, vlieg)
    return an


# =====================================================================================================================
# zeeschildpad: hoog gewelfd schild met schubbenplaten, lange voorflippers, zwemt rustig
# =====================================================================================================================
SP_SCHILD = (122, 132, 78)
SP_PLAAT = (88, 98, 56)
SP_HUID = (176, 186, 132)
SP_HUID_D = (128, 140, 94)
SP_BUIK = (238, 226, 176)


def zeeschildpad(atlas):
    def platen(W, H, rng):
        a = plain(SP_SCHILD, 8)(W, H, rng).astype(np.float32)
        step = int(2.5 * S)
        for y in range(H):
            for x in range(W):
                if (x // step + y // step) % 2 == 0 and (x % step < S // 2 or y % step < S // 2):
                    a[y, x, :3] = SP_PLAAT
        return np.clip(a, 0, 255).astype(np.uint8)
    huid = plain(SP_HUID, 8, lambda W, H, rng, a: [disc(a, rng.uniform(0, W), rng.uniform(0, H), rng.uniform(0.4, 0.9) * S, SP_HUID_D, 0.8)
                                                   for _ in range(max(2, W * H // 200))])
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3, 0], [cube(atlas, [-5, 1.6, -5], [10, 3.6, 10.4], platen, {"down": plain(SP_BUIK, 6)}),
                                              cube(atlas, [-3.8, 5.2, -3.8], [7.6, 1.6, 8], platen)])]
    bones.append(bone("head", "body", [0, 3.4, -5], [cube(atlas, [-1.9, 2.0, -9.4], [3.8, 3.4, 4.4], huid,
                                                          {"north": gezicht(SP_HUID, eye_y=0.42, eye_dx=0.26, eye_r=0.19)})]))
    for side, sx in (("left", 1), ("right", -1)):
        voor, achter = [5, 2.4, -5.6], [4.4, 2.2, 3.4]
        if sx < 0:
            voor, achter = at.mirror_x(voor, [6.6, 0.8, 3]), at.mirror_x(achter, [3, 0.7, 2.6])
        bones.append(bone(f"flipper_{side}", "body", [sx * 5, 2.8, -4], [cube(atlas, voor, [6.6, 0.8, 3], huid)], rotation=[0, sx * -18, 0]))
        bones.append(bone(f"flipper_back_{side}", "body", [sx * 4.4, 2.6, 4.6], [cube(atlas, achter, [3, 0.7, 2.6], huid)],
                          rotation=[0, sx * 25, 0]))
    bones.append(bone("tail", "body", [0, 2.6, 5.4], [cube(atlas, [-0.6, 2.2, 5.4], [1.2, 0.8, 1.6], huid)]))
    return geo("zeeschildpad", bones, atlas, 1.4, 0.6)


def zeeschildpad_anims():
    idle = {"flipper_left": {"rotation": kf((0, [0, -18, 0]), (2.0, [0, -18, -10]), (4.0, [0, -18, 0]))},
            "flipper_right": {"rotation": kf((0, [0, 18, 0]), (2.0, [0, 18, 10]), (4.0, [0, 18, 0]))}}
    an = at.animaties(extra_idle=idle)
    an["animations"]["idle"]["animation_length"] = 4.0
    L = 2.0
    zwem = {"root": {"position": kf((0, [0, 0.5, 0]), (L / 2, [0, 1.3, 0]), (L, [0, 0.5, 0]))},
            "flipper_left": {"rotation": kf((0, [0, -18, -30]), (L / 2, [0, -30, 25]), (L, [0, -18, -30]))},
            "flipper_right": {"rotation": kf((0, [0, 18, 30]), (L / 2, [0, 30, -25]), (L, [0, 18, 30]))},
            "flipper_back_left": {"rotation": kf((0, [0, 25, 0]), (L / 2, [0, 35, 0]), (L, [0, 25, 0]))},
            "flipper_back_right": {"rotation": kf((0, [0, -25, 0]), (L / 2, [0, -35, 0]), (L, [0, -25, 0]))},
            "head": {"rotation": kf((0, [0, 0, 0]), (L / 2, [-6, 0, 0]), (L, [0, 0, 0]))}}
    an["animations"]["walk"] = {"loop": True, "animation_length": L, "bones": zwem}
    extra(an, "zwem", L, zwem)
    return an


# =====================================================================================================================
# pelikaan: wit, korte oranje pootjes met zwemvliezen, lange snavel met een grote oranje keelzak ("slok": zak bolt op)
# =====================================================================================================================
PK_WIT = (250, 248, 244)
PK_ZW = (54, 50, 56)
PK_POOT = (246, 150, 70)
PK_SNAVEL = (250, 196, 96)
PK_ZAK = (246, 140, 64)
PK_JONG = (196, 194, 200)


def pelikaan(atlas, jong=False):
    wit = at.fluffy(PK_JONG, 14) if jong else plain(PK_WIT, 6)
    poot = plain(PK_POOT, 6)
    bones = [bone("root", None, [0, 0, 0], [])]
    for side, sx in (("left", 1), ("right", -1)):
        x = 1.0 if sx > 0 else -2.4
        bones.append(bone(f"leg_{side}", "root", [sx * 1.7, 6.5, 0], [
            cube(atlas, [x, 0.5, -0.6], [1.4, 6, 1.4], poot),
            cube(atlas, [x - 1.2, 0, -3.4], [3.8, 0.6, 4], poot)]))

    def slagpen(side):
        def f(W, H, rng, a):
            if jong:
                return
            n = int(W * 0.35)
            if side == "left":
                a[:, :n, :3] = PK_ZW
            else:
                a[:, W - n:, :3] = PK_ZW
        kant = "east" if side == "left" else "west"
        return {kant: plain(PK_JONG if jong else PK_WIT, 6, f), "south": plain(PK_JONG if jong else PK_ZW, 6)}
    bones.append(bone("body", "root", [0, 10, 0], [cube(atlas, [-4.5, 6, -5.5], [9, 7.5, 12], wit)]))
    bones += paar(atlas, "wing", "body", [4.5, 13, 0], [4.5, 6.5, -4.5], [1.2, 6.4, 11], wit, slagpen)
    bones.append(bone("tail", "body", [0, 9, 6.5], [cube(atlas, [-2.5, 8.6, 6.5], [5, 2.6, 2.4], wit)]))
    bones.append(bone("hals", "body", [0, 12.5, -5], [cube(atlas, [-1.7, 12.5, -6.6], [3.4, 5.6, 3.2], wit)]))
    bones.append(bone("head", "hals", [0, 18, -5], [
        cube(atlas, [-2.8, 17.2, -8.4], [5.6, 5, 5.4], wit, {"north": gezicht(PK_JONG if jong else PK_WIT, eye_y=0.42, eye_r=0.19)}),
        cube(atlas, [-1.1, 18.4, -18.4], [2.2, 1.2, 10], plain(PK_SNAVEL, 5)),
        cube(atlas, [-0.8, 18.2, -19.2], [1.6, 1.4, 1.0], plain((232, 120, 70), 4))]))
    # de keelzak onder de snavel (bolt op bij "slok")
    bones.append(bone("keelzak", "head", [0, 18.2, -9.4], [cube(atlas, [-1.4, 15.8, -17.6], [2.8, 2.4, 8.4],
                                                                 plain(PK_ZAK if not jong else (214, 160, 120), 6))]))
    return geo("pelikaan_baby" if jong else "pelikaan", bones, atlas, 1.4, 1.6)


def pelikaan_anims():
    an = at.animaties(legs2=["leg_left", "leg_right"], waggel=True,
                      extra_idle={"hals": {"rotation": kf((0, [0, 0, 0]), (1.5, [4, 0, 0]), (3.0, [0, 0, 0]))}})
    extra(an, "slok", 2.0, {
        "head": {"rotation": kf((0, [0, 0, 0]), (0.4, [30, 0, 0]), (0.8, [-35, 0, 0]), (1.6, [-35, 0, 0]), (2.0, [0, 0, 0]))},
        "keelzak": {"scale": kf((0, [1, 1, 1]), (0.4, [1.1, 1.2, 1]), (0.8, [1.6, 2.4, 1.05]), (1.4, [1.6, 2.4, 1.05]),
                                (1.8, [1, 1, 1]), (2.0, [1, 1, 1]))}}, loop=False)
    L = 0.6
    extra(an, "vlieg", L, {
        "body": {"position": kf((0, [0, 6, 0]), (L / 2, [0, 6.6, 0]), (L, [0, 6, 0]))},
        "wing_left": {"rotation": kf((0, [0, 0, -70]), (L / 2, [0, 0, 20]), (L, [0, 0, -70]))},
        "wing_right": {"rotation": kf((0, [0, 0, 70]), (L / 2, [0, 0, -20]), (L, [0, 0, 70]))},
        "leg_left": {"rotation": kf((0, [70, 0, 0]))}, "leg_right": {"rotation": kf((0, [70, 0, 0]))}})
    return an


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "spookdiertje":
        a = Atlas(1110); g = spookdiertje(a); anims = spookdiertje_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.82), a)
    elif soort == "plompe_lori":
        a = Atlas(1120); g = plompe_lori(a); anims = plompe_lori_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.58, 0.8), a)
    elif soort == "zeepaardje":
        a = Atlas(1130); g = zeepaardje(a); anims = zeepaardje_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.6, 0.8), a)
    elif soort == "pijlstaartrog":
        a = Atlas(1140); g = pijlstaartrog(a); anims = pijlstaartrog_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.55, 0.75), a)
    elif soort == "zeeschildpad":
        a = Atlas(1150); g = zeeschildpad(a); anims = zeeschildpad_anims()
        img = at.schrijf(d, soort, g, a, anims)
        at.schrijf(d, soort + "_baby", baby(g, 0.5, 0.72), a)
    elif soort == "pelikaan":
        a = Atlas(1160); g = pelikaan(a); anims = pelikaan_anims()
        img = at.schrijf(d, soort, g, a, anims)
        ak = Atlas(1161); at.schrijf(d, soort + "_baby", baby(pelikaan(ak, jong=True), 0.55, 0.78), ak)
    else:
        raise SystemExit(f"onbekende soort {soort}")
    json.dump(at.bbmodel(soort, g, anims, img), open(os.path.join(d, f"{soort}.bbmodel"), "w", encoding="utf-8"))
    print("gemaakt:", soort)


# =====================================================================================================================
# vellen (zelfde opzet als batch 9)
# =====================================================================================================================
def vellen(soorten, overzicht=True):
    import blad as bl
    from PIL import Image, ImageDraw
    bl.UIT = UIT
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
        d.text((20, 18), f"{NAMEN[soort]}  (safari-dieren, batch 11: Kriebelhuis + delta)", font=bl.font(30, True), fill=(40, 30, 40))
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
    d.text((20, 18), "Safari-dieren batch 11 (Kriebelhuis + delta): naast een guh (volwassen achter, jong ervoor)",
           font=bl.font(28, True), fill=(40, 30, 40))
    d.text((20, 56), "Spookdiertje · Plompe lori · Zeepaardje · Pijlstaartrog · Zeeschildpad · Pelikaan (van links naar rechts, na de guh)",
           font=bl.font(17), fill=(80, 70, 80))
    pad = os.path.join(UIT, "batch11_overzicht.png")
    img.convert("RGB").save(pad)
    print(pad)


# =====================================================================================================================
# alles: modellen, slaapoogjes, assets (zonder soorten.json), vellen
# =====================================================================================================================
def alles(soorten, met_vellen=True):
    global UIT
    cwd = os.getcwd()
    os.makedirs(UIT, exist_ok=True)
    for s in soorten:
        maak(s)
        os.chdir(cwd)
    sys.path.insert(0, os.path.abspath(os.path.join(HIER, "..")))
    import safari_dieren as sd
    wakker = UIT
    at.SLAAP = True
    try:
        UIT = sd.STAGING_SLAAP
        os.makedirs(UIT, exist_ok=True)
        for s in soorten:
            maak(s)
            os.chdir(cwd)
    finally:
        at.SLAAP = False
        UIT = wakker
    sd.kopieer(soorten)
    sd.extra_animaties(soorten)
    if met_vellen:
        vellen(soorten, overzicht=True)
        os.chdir(cwd)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    alles(args or SOORTEN, met_vellen="--geen-vellen" not in sys.argv)
