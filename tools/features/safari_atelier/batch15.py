"""
Safari-dieren batch 15 (testronde 4, ronde 56: Het Kriebelhuis, de binnen-diepzeezaal): lantaarnvisje (rond diepzee-
vissie met een grote lieve mond en een hengeltje met een gloeiend lampje boven zijn kop) en Dumbo-octopusje (rond
roze-wit lijfje met twee grote oorvinnen die fladderen, korte armpjes met vliesjes, zweeft). Zelfde stijl en
bestandsformaat als batch 10-14; de gedeelde hulpjes komen uit batch12.py.

    python tools/features/safari_atelier/batch15.py [soort ...] [--geen-vellen]

Het lantaarnvisje heeft lantaarnvisje_glow.png (alleen het lampje); safari_dieren maakt daar lantaarnvisje_glowmask.png
van, en batch12.alles zet er ook lantaarnvisje_slaap_glowmask.png naast.
"""
import os
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
import atelier as at                                                    # noqa: E402
from atelier import Atlas, cube, bone, geo, plain, gezicht             # noqa: E402
import batch12 as b12                                                   # noqa: E402
from batch12 import extra, maak_soort                                   # noqa: E402
from batch13 import vis_anims, vinnen                                   # noqa: E402

UIT = os.path.abspath(os.path.join(HIER, "..", "..", "..", "build", "safari_atelier"))
SOORTEN = ["lantaarnvisje", "dumbo_octopus"]
NAMEN = {"lantaarnvisje": "Lantaarnvisje", "dumbo_octopus": "Dumbo-octopusje"}
kf = at.kf


# =====================================================================================================================
# lantaarnvisje: rond donkerblauw vissie, grote lieve mond, hengeltje met een gloeiend lampje boven zijn kop
# =====================================================================================================================
LV = (58, 70, 120)
LV_L = (120, 140, 196)
LV_MOND = (60, 22, 40)
LV_LIP = (232, 120, 160)
LV_HENGEL = (40, 46, 80)
LV_LAMP = (190, 255, 230)


def lantaarnvisje(atlas):
    huid = plain(LV, 8)
    lamp = cube(atlas, [-0.7, 9.4, -5.6], [1.4, 1.4, 1.4], plain(LV_LAMP, 4))
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 3.6, 0], [cube(atlas, [-2.4, 1.2, -2.8], [4.8, 4.8, 5.6], huid,
                                                     {"north": gezicht(LV, eye_y=0.36, eye_dx=0.26, eye_r=0.22),
                                                      "down": plain(LV_L, 4)})])]
    bones.append(bone("head", "body", [0, 4.0, -2.8], []))
    # de grote lieve mond: een roze lipje met een donkere binnenkant, vlak voor het gezichtje (onderaan)
    bones.append(bone("mond", "body", [0, 2.0, -2.9], [cube(atlas, [-1.6, 1.5, -3.0], [3.2, 1.0, 0.2], plain(LV_LIP, 4)),
                                                      cube(atlas, [-1.1, 1.7, -3.05], [2.2, 0.6, 0.2], plain(LV_MOND, 4))]))
    # het hengeltje: vanaf de kruin schuin naar voren, met het lampje aan het eind
    bones.append(bone("hengel", "body", [0, 6.0, -1.0], [cube(atlas, [-0.25, 6.0, -1.25], [0.5, 3.6, 0.5], plain(LV_HENGEL, 4)),
                                                        cube(atlas, [-0.25, 9.3, -5.4], [0.5, 0.5, 4.4], plain(LV_HENGEL, 4)),
                                                        lamp]))
    bones.append(bone("tail", "body", [0, 3.6, 2.8], [cube(atlas, [-0.2, 2.0, 2.8], [0.4, 3.2, 2.2], plain(LV, 6))]))
    bones += vinnen(atlas, "body", 3.0, -0.8, plain(LV_L, 6), 1.8)
    return geo("lantaarnvisje", bones, atlas, 0.5, 0.6), [lamp]


def lantaarnvisje_anims():
    an = vis_anims(1.2, ["vin_left", "vin_right"])
    # het lampje wiebelt aan het hengeltje (in idle en zwemmen), de mond hapt af en toe
    wiebel = {"rotation": kf((0, [6, 0, 4]), (0.8, [-6, 0, -4]), (1.6, [6, 0, 4]))}
    for a in ("idle", "walk", "zwem"):
        an["animations"][a]["bones"]["hengel"] = wiebel
    extra(an, "hap", 0.8, {"mond": {"scale": kf((0, [1, 1, 1]), (0.2, [1.1, 2.2, 1]), (0.5, [1, 1, 1]))},
                           "hengel": {"rotation": kf((0, [0, 0, 0]), (0.3, [-14, 0, 0]), (0.8, [0, 0, 0]))}}, loop=False)
    return an


# =====================================================================================================================
# Dumbo-octopusje: rond roze-wit lijfje, twee grote oorvinnen die fladderen, korte armpjes met vliesjes; zweeft
# =====================================================================================================================
DO = (246, 196, 206)
DO_L = (255, 236, 238)
DO_D = (226, 150, 170)


def dumbo_octopus(atlas):
    huid = plain(DO, 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 4.0, 0], [cube(atlas, [-3.0, 3.0, -3.0], [6.0, 5.6, 6.0], huid,
                                                     {"north": gezicht(DO, eye_y=0.42, eye_dx=0.25, eye_r=0.2),
                                                      "up": plain(DO_L, 4)})])]
    bones.append(bone("head", "body", [0, 6.0, -3.0], []))
    # de oorvinnen (de "Dumbo-oortjes") bovenaan opzij
    for side, sx in (("left", 1), ("right", -1)):
        o = [3.0, 6.2, -1.6] if sx > 0 else [-3.0 - 3.2, 6.2, -1.6]
        bones.append(bone(f"oor_{side}", "body", [sx * 3.0, 7.0, 0], [cube(atlas, o, [3.2, 2.4, 3.2], plain(DO_D, 6))],
                          rotation=[0, 0, sx * -15]))
    # het vliesrokje + acht korte armpjes eronder
    bones.append(bone("rokje", "body", [0, 3.0, 0], [cube(atlas, [-3.4, 2.0, -3.4], [6.8, 1.2, 6.8], plain(DO_L, 6))]))
    for i in range(8):
        hoek = i * 45
        import math
        x, z = 2.6 * math.sin(math.radians(hoek)), 2.6 * math.cos(math.radians(hoek))
        bones.append(bone(f"arm_{i}", "rokje", [x, 2.0, z], [cube(atlas, [x - 0.5, 0.0, z - 0.5], [1.0, 2.2, 1.0], plain(DO, 6))]))
    return geo("dumbo_octopus", bones, atlas, 0.6, 0.6)


def dumbo_anims():
    def fladder(L, hoogte=(1.4, 2.6), a=38):
        b = {"root": {"position": kf((0, [0, hoogte[0], 0]), (L / 2, [0, hoogte[1], 0]), (L, [0, hoogte[0], 0]))},
             "oor_left": {"rotation": kf((0, [0, 0, -15]), (L / 2, [0, 0, -15 - a]), (L, [0, 0, -15]))},
             "oor_right": {"rotation": kf((0, [0, 0, 15]), (L / 2, [0, 0, 15 + a]), (L, [0, 0, 15]))},
             "rokje": {"scale": kf((0, [1, 1, 1]), (L / 2, [1.12, 0.85, 1.12]), (L, [1, 1, 1]))}}
        for i in range(8):
            b[f"arm_{i}"] = {"rotation": kf((0, [10, 0, 0]), (L / 2, [-12, 0, 0]), (L, [10, 0, 0]))}
        return b
    an = at.animaties(extra_idle=fladder(3.0, (1.4, 2.0), 18))
    an["animations"]["walk"] = {"loop": True, "animation_length": 1.4, "bones": fladder(1.4)}
    extra(an, "fladder", 1.0, fladder(1.0, (1.6, 2.8), 48))
    extra(an, "zwem", 1.4, fladder(1.4))
    return an


# =====================================================================================================================
# wegschrijven
# =====================================================================================================================
def maak(soort):
    d = os.path.join(UIT, soort)
    if soort == "lantaarnvisje":
        a = Atlas(1510); g, gloed = lantaarnvisje(a); maak_soort(d, soort, g, a, lantaarnvisje_anims(), (0.6, 0.8))
        import batch9
        batch9.glow_png(a, gloed, os.path.join(d, "lantaarnvisje_glow.png"))
        batch9.glow_png(a, gloed, os.path.join(d, "lantaarnvisje_baby_glow.png"))   # de baby deelt de atlas (zelfde uv)
    elif soort == "dumbo_octopus":
        a = Atlas(1520); maak_soort(d, soort, dumbo_octopus(a), a, dumbo_anims(), (0.6, 0.8))
    else:
        raise SystemExit(f"onbekende soort {soort}")


def vellen(soorten, overzicht=True):
    b12.vellen_gedeeld(soorten, SOORTEN, NAMEN, 15, "Kriebelhuis, diepzeezaal", UIT, overzicht)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    b12.alles(sys.modules[__name__], args or SOORTEN, met_vellen="--geen-vellen" not in sys.argv)
