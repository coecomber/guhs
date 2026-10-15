"""
bbq2 (guhrio-beloning) - the textures of the building blocks (the flagpole, the green pipe in sixteen colours, the
highscore board) and the painters of the five outfits. Everything is drawn here; nothing is hand-made.

  blokken(h)        writes the block textures (the ?-block to build with reuses the engine's guhrio_vraagblok.png)
  KLEUREN           the sixteen dye colours a pipe can have (DyeColor names); green is the engine's own bright green
  kleding(rng, v)   the painters of the clothes swatches (make_guh_variants: clothes)
  iconen(ic)        the 16 x 16 item pictures of the outfits (make_clothes_icons: icons)
"""
import math

import numpy as np
from PIL import Image

# the dye colours (vanilla's texture colours); a pipe keeps the engine's own green when it is green
KLEUREN = {"white": (249, 255, 254), "orange": (249, 128, 29), "magenta": (199, 78, 189), "light_blue": (58, 179, 218),
           "yellow": (254, 216, 61), "lime": (128, 199, 31), "pink": (243, 139, 170), "gray": (86, 94, 98),
           "light_gray": (157, 157, 151), "cyan": (22, 156, 156), "purple": (137, 50, 184), "blue": (60, 68, 170),
           "brown": (131, 84, 50), "green": (58, 176, 66), "red": (190, 50, 44), "black": (52, 52, 60)}
PIJP_GROEN = (58, 176, 66)


def _vlak(kleur, var, seed, w=16, h=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((h, w, 4), np.uint8)
    n = rng.normal(0, var, (h, w))
    for c in range(3):
        a[..., c] = np.clip(kleur[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _kleur_pijp(img, kleur):
    """The engine's green pipe texture in another colour: every pixel keeps its light and dark."""
    if kleur == "green":
        return img
    a = np.asarray(img.convert("RGBA")).astype(np.float32)
    lum = a[..., 0] * 0.3 + a[..., 1] * 0.59 + a[..., 2] * 0.11
    basis = PIJP_GROEN[0] * 0.3 + PIJP_GROEN[1] * 0.59 + PIJP_GROEN[2] * 0.11
    t = (lum / basis)[..., None]
    rgb = np.array(KLEUREN[kleur], np.float32)
    uit = rgb * np.minimum(t, 1.0) + (255 - rgb) * np.clip(t - 1.0, 0, 1) * 0.75
    a[..., :3] = np.clip(uit, 0, 255)
    return Image.fromarray(a.astype(np.uint8))


def _mast():
    """The pole: a round green tube (light on the left, dark on the right), a ring every eight pixels."""
    a = np.zeros((16, 16, 4), np.uint8)
    kolom = [(170, 240, 150), (120, 220, 110), (84, 180, 84), (52, 132, 62)]
    for y in range(16):
        for x in range(16):
            k = kolom[x % 4]
            if y % 8 == 7:
                k = tuple(int(c * 0.72) for c in k)
            a[y, x] = (*k, 255)
    return Image.fromarray(a)


def _vlag():
    """The flag (the top left 7 x 8 pixels are the flag itself): pink with a little guh face, a darker hem."""
    a = np.zeros((16, 16, 4), np.uint8)
    roze, donker, wit, oog = (255, 120, 180), (214, 84, 146), (255, 255, 255), (60, 30, 70)
    for y in range(8):
        for x in range(7):
            a[y, x] = (*(donker if y in (0, 7) or x == 6 else roze), 255)
    for (x, y) in ((1, 2), (4, 2)):                               # ears
        a[y, x] = (*wit, 255)
    for (x, y) in ((1, 3), (2, 3), (3, 3), (4, 3), (1, 4), (4, 4), (1, 5), (2, 5), (3, 5), (4, 5)):
        a[y, x] = (*wit, 255)
    a[4, 2] = (*oog, 255)
    a[4, 3] = (*oog, 255)
    # the rest of the sheet: the golden ball (8..15, 0..7) and the stone foot's top (0..15, 8..15)
    for y in range(8):
        for x in range(8, 16):
            d = math.hypot(x - 11.5, y - 3.5)
            k = (255, 236, 150) if d < 1.6 else (252, 196, 56) if d < 3.4 else (176, 112, 20)
            a[y, x] = (*k, 255)
    a[8:16, :] = _vlak((150, 96, 60), 5, 9271, 16, 8)
    a[8, :, :3] = (206, 150, 104)
    a[15, :, :3] = (96, 56, 34)
    return Image.fromarray(a)


def _bord(soort):
    """The highscore block: gold with rivets; its front is a dark screen with a podium of three bars and a little clock."""
    a = _vlak((250, 196, 56), 5, 9281 + len(soort))
    licht, donker = (255, 236, 150), (164, 104, 18)
    a[0, :, :3] = licht
    a[:, 0, :3] = licht
    a[15, :, :3] = donker
    a[:, 15, :3] = donker
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        a[y, x, :3] = donker
    if soort == "voor":
        a[3:13, 3:13, :3] = (22, 20, 34)
        for x0, hoog, kleur in ((4, 3, (200, 200, 214)), (7, 6, (255, 214, 70)), (10, 2, (214, 130, 70))):   # 2nd, 1st, 3rd
            a[12 - hoog:12, x0:x0 + 2, :3] = kleur
        a[4, 4:6, :3] = (120, 220, 110)                        # a little flag over the screen
        a[4, 10:12, :3] = (255, 120, 180)
    elif soort == "boven":
        a[5:11, 5:11, :3] = (255, 226, 120)
        a[7:9, 7:9, :3] = donker
    else:
        a[4:12, 4:12, :3] = (226, 168, 40)
        a[4, 4:12, :3] = donker
        a[4:12, 4, :3] = donker
    return Image.fromarray(a)


def blokken(h, pijp, pijp_lijf, pijp_boven):
    """pijp / pijp_lijf / pijp_boven: the engine's three green pipe pictures (PIL), recoloured here."""
    h.save(_mast(), "block", "guhriobeloning_mast.png")
    h.save(_vlag(), "block", "guhriobeloning_vlag.png")
    for soort in ("voor", "zij", "boven"):
        h.save(_bord(soort), "block", f"guhriobeloning_scorebord_{soort}.png")
    for kleur in KLEUREN:
        if kleur == "green":
            continue                                            # (the engine's own three pictures)
        h.save(_kleur_pijp(pijp, kleur), "block", f"guhriobeloning_pijp_{kleur}.png")
        h.save(_kleur_pijp(pijp_lijf, kleur), "block", f"guhriobeloning_pijp_lijf_{kleur}.png")
        h.save(_kleur_pijp(pijp_boven, kleur), "block", f"guhriobeloning_pijp_boven_{kleur}.png")


def pijp_textuur(kleur, deel):
    """The texture id of a pipe part ("" the mouth's side, "lijf", "boven") in this colour."""
    if kleur == "green":
        return "guhs:block/guhrio_pijp" + (f"_{deel}" if deel else "")
    return "guhs:block/guhriobeloning_pijp" + (f"_{deel}" if deel else "") + f"_{kleur}"


# =====================================================================================================================
# the outfits
# =====================================================================================================================
LETTERS = {"G": [".####.", "#....#", "#.....", "#..###", "#....#", "#....#", ".####."],
           "L": ["#.....", "#.....", "#.....", "#.....", "#.....", "#.....", "######"],
           "V": ["#....#", "#....#", "#....#", ".#..#.", ".#..#.", "..##..", "..##.."]}


def kleding(rng, v):
    def pet(kleur, glim=False):
        def schilder():
            a = v.fabric(kleur, rng, 8)
            a[:4, :] = np.clip(np.array(kleur) + 34, 0, 255)      # a light top: the dome catches the light
            for x in range(0, 32, 8):                             # the seams of the panels
                a[:, x] = a[:, x] * 0.86
            if glim:
                for i in range(0, 32, 5):
                    a[i % 32, (i * 3) % 32] = (255, 250, 200)
            return a
        return schilder

    def klep(kleur):
        return lambda: v.fabric(tuple(int(c * 0.78) for c in kleur), rng, 6)

    def embleem(letter, kleur):
        def schilder():
            # (with an alpha channel: outside the round badge the swatch is see-through)
            a = np.zeros((32, 32, 4), np.float32)
            for y in range(32):
                for x in range(32):
                    d = math.hypot(x - 15.5, y - 15.5)
                    if d <= 15.2:
                        a[y, x] = (250, 250, 246, 255) if d <= 13 else (196, 196, 190, 255)
            for r, rij in enumerate(LETTERS[letter]):
                for c, ch in enumerate(rij):
                    if ch == "#":
                        a[6 + r * 3:9 + r * 3, 7 + c * 3:10 + c * 3, :3] = tuple(int(k * 0.9) for k in kleur)
            return a
        return schilder

    def snor():
        a = v.fabric((92, 54, 30), rng, 12)
        for x in range(0, 32, 3):
            a[:, x] = a[:, x] * 0.72
        a[:3, :] = (124, 78, 46)
        return a

    def goud():
        a = v.fabric((250, 200, 60), rng, 8)
        a[:5, :] = (255, 236, 150)
        a[27:, :] = (190, 128, 24)
        return a

    def juweel(kleur):
        def schilder():
            a = v.fabric(kleur, rng, 6)
            a[4:12, 4:12] = np.clip(np.array(kleur) + 90, 0, 255)
            a[22:, :] = np.array(kleur) * 0.6
            return a
        return schilder

    def schild():
        a = v.fabric((62, 168, 70), rng, 8)
        donker, licht = (30, 104, 44), (120, 214, 110)
        for y in range(32):                                       # six-sided plates: a honeycomb of dark seams
            for x in range(32):
                rij = y // 8
                xx = (x + (rij % 2) * 6) % 12
                if y % 8 == 0 or (xx == 0 and y % 8 < 8):
                    a[y, x] = donker
                elif y % 8 == 1 and 1 <= xx <= 4:
                    a[y, x] = licht
        return a

    def rand():
        a = v.fabric((246, 244, 232), rng, 6)
        a[26:, :] = (206, 202, 186)
        return a

    rood, groen, geel = (226, 44, 40), (62, 172, 66), (250, 204, 64)
    return {"guhriobeloning_rode_pet": {"guhriobeloning_pet": pet(rood), "guhriobeloning_pet_klep": klep(rood),
                                        "guhriobeloning_embleem": embleem("G", rood), "guhriobeloning_snor": snor},
            "guhriobeloning_groene_pet": {"guhriobeloning_pet": pet(groen), "guhriobeloning_pet_klep": klep(groen),
                                          "guhriobeloning_embleem": embleem("L", groen)},
            "guhriobeloning_gouden_pet": {"guhriobeloning_pet": pet(geel, glim=True), "guhriobeloning_pet_klep": klep((226, 160, 30)),
                                          "guhriobeloning_embleem": embleem("V", (214, 150, 20))},
            "guhriobeloning_prinsessenkroon": {"guhriobeloning_kroon": goud, "guhriobeloning_juweel_rood": juweel((214, 40, 60)),
                                               "guhriobeloning_juweel_blauw": juweel((50, 110, 226))},
            "guhriobeloning_schild": {"guhriobeloning_schild": schild, "guhriobeloning_schild_rand": rand}}


PET = ["................", "................", ".....aaaaaa.....", "...aabbbbbbaa...", "..abbbbwwbbbba..", "..abbbwllwbbba..",
       ".abbbbwllwbbbba.", ".abbbbbwwbbbbba.", ".aaaaaaaaaaaaaaa", "..akkkkkkkkkkkka", "...aaaaaaaaaaaa.", "................",
       "................", "................", "................", "................"]
PET_SNOR = PET[:11] + ["................", "..ss..ssss..ss..", ".sssssssssssss..", "..ssssss.sssss..", "................"]
KROON = ["................", "................", "...g...gg...g...", "...g..gggg..g...", "..ggg.gRRg.ggg..", "..gggggRRggggg..",
         "..gBggggggggBg..", "..gggggggggggg..", "..dddddddddddd..", "................", "................", "................",
         "................", "................", "................", "................"]
SCHILD = ["................", "....wwwwwwww....", "..wwaaaaaaaaww..", ".waabbdbbdbbaaw.", ".wabbbdbbdbbbaw.", "wabddddddddddbaw",
          "wabbbdbbbbdbbbaw", "wabbbdbbbbdbbbaw", "wabddddddddddbaw", ".wabbbdbbdbbbaw.", ".waabbdbbdbbaaw.", "..wwaaaaaaaaww..",
          "....wwwwwwww....", "................", "................", "................"]


def iconen(ic):
    def pet(rijen, kleur, letter):
        donker = tuple(int(c * 0.6) for c in kleur)
        return ic.icon(rijen, {"a": donker, "b": kleur, "w": (250, 250, 246), "l": letter, "k": tuple(int(c * 0.78) for c in kleur),
                               "s": (92, 54, 30)})
    return {"guhriobeloning_rode_pet": pet(PET_SNOR, (226, 44, 40), (200, 36, 34)),
            "guhriobeloning_groene_pet": pet(PET, (62, 172, 66), (40, 130, 48)),
            "guhriobeloning_gouden_pet": pet(PET, (250, 204, 64), (190, 128, 24)),
            "guhriobeloning_prinsessenkroon": ic.icon(KROON, {"g": (250, 200, 60), "d": (190, 128, 24), "R": (214, 40, 60), "B": (50, 110, 226)}),
            "guhriobeloning_schild": ic.icon(SCHILD, {"w": (246, 244, 232), "a": (30, 104, 44), "b": (62, 168, 70), "d": (30, 104, 44)})}
