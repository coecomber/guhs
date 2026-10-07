"""
Textures and models of the guhpixel slice "among" that came with the task mini-games, the oefenrondje and the shop:
  - the sprite sheet of the task panels (textures/gui/among_taken.png, 16 x 16 cells; Java: among.client.TaakSpel)
  - the SUS-stickerbord (the keepsake of the oefenrondje; a wall decoration)
  - the shop's clothes: eight Ruimtepakjes (the suit bones plus an air tank on the back) and six hoedjes (own bones),
    with their painters (make_guh_variants.py) and icons (make_clothes_icons.py)
Everything is drawn here with fixed colours; nothing is random except the fabric noise of the clothes generator's own rng.
"""
import numpy as np
from PIL import Image

from features import guhpixel_lib as lib

# =====================================================================================================================
# the sprite sheet of the task panels
# =====================================================================================================================
KAASKNABBEL = [
    "................",
    "................",
    ".....oooooo.....",
    "...ooyyyyyyoo...",
    "..oyyyyyhyyyyo..",
    ".oyyhyyyyyyyyyo.",
    ".oyyyyyyyydyyyo.",
    ".oyyyyyyyyyyyyo.",
    ".oyydyyyyyyhyyo.",
    ".oyyyyyyyyyyyyo.",
    "..oyyyyhyyyyyo..",
    "...ooyyyyyyoo...",
    ".....oooooo.....",
    "................",
    "................",
    "................",
]
KNABBEL = [
    "................",
    "................",
    "..oooooooooooo..",
    "..oyyyyyyyyyyo..",
    "..oyhyyyyyyhyo..",
    "..oyyyyyyyyyyo..",
    "..oyyyydyyyyyo..",
    "..oyyyyyyyyyyo..",
    "..oyyyyyyydyyo..",
    "..oyhyyyyyyyyo..",
    "..oyyyyyyyyhyo..",
    "..oyyyyyyyyyyo..",
    "..oooooooooooo..",
    "................",
    "................",
    "................",
]
KRUIMEL = [
    "................",
    "................",
    "................",
    "....oo..........",
    "...oyyo....oo...",
    "...oyyo...oyyo..",
    "....oo....oyyo..",
    "...........oo...",
    "......oo........",
    ".....oyyo.......",
    ".....oyyo...o...",
    "......oo...oyo..",
    "............o...",
    "................",
    "................",
    "................",
]
GUH = [
    "................",
    "..oo........oo..",
    ".oppo......oppo.",
    ".opppoooooopppo.",
    ".oppppppppppppo.",
    "oppppppppppppppo",
    "oppkkppppppkkppo",
    "oppkwppppppkwppo",
    "oppppppnnppppppo",
    "oprrpppnnppprrpo",
    "opppppkppkpppppo",
    ".opppppkkpppppo.",
    ".oppppppppppppo.",
    "..oppppppppppo..",
    "...oooooooooo...",
    "................",
]
SLAAPGUH = [
    "..........z.....",
    "..oo.......zoo..",
    ".oppo......oppo.",
    ".opppoooooopppo.",
    ".oppppppppppppo.",
    "oppppppppppppppo",
    "oppppppppppppppo",
    "oppkkkppppkkkppo",
    "oppppppnnppppppo",
    "oprrpppnnppprrpo",
    "oppppppkkppppppo",
    ".oppppppppppppo.",
    ".oppppppppppppo.",
    "..oppppppppppo..",
    "...oooooooooo...",
    "................",
]
WOLK = [
    "................",
    "................",
    ".....ooo........",
    "....owwwoooo....",
    "...owwwwwwwwo...",
    "..owwwwwwwwwwo..",
    ".owwwwwwwwwwwwo.",
    ".owwwwwwwwwwwwo.",
    "..owwwwwwwwwwo..",
    "...oooooooooo...",
    ".......oo.......",
    ".....oo.........",
    "....o...........",
    "................",
    "................",
    "................",
]
JERRYCAN = [
    "................",
    "....oooooo......",
    "....o....o.ooo..",
    "..ooooooooooyo..",
    "..oyyyyyyyyyyo..",
    "..oyyyyyyyyyyo..",
    "..oyybyyyybyyo..",
    "..oyyybyybyyyo..",
    "..oyyyybbyyyyo..",
    "..oyyybyybyyyo..",
    "..oyybyyyybyyo..",
    "..oyyyyyyyyyyo..",
    "..oyyyyyyyyyyo..",
    "..oooooooooooo..",
    "................",
    "................",
]
WORST = [
    "................",
    "................",
    "................",
    "................",
    "..ooo......ooo..",
    ".orrroooooorrro.",
    ".orrrrrrrrrrrro.",
    ".orhhrrrrrrrrro.",
    ".orrrrrrrrrrrro.",
    "..oorrrrrrrroo..",
    "....oooooooo....",
    "................",
    "................",
    "................",
    "................",
    "................",
]
GUH_KLEUREN = {"o": (120, 70, 110), "p": (232, 170, 214), "k": (40, 22, 48), "w": (255, 255, 255), "n": (255, 130, 170), "r": (250, 140, 180),
               "z": (200, 220, 255)}
# cell: (rows, colours); the order is the index Java uses (TaakSpel.KAASKNABBEL ... SLAAPGUH)
VEL = [
    (KAASKNABBEL, {"o": (150, 100, 20), "y": (250, 200, 60), "h": (255, 235, 140), "d": (215, 155, 35)}),
    (KNABBEL, {"o": (96, 60, 30), "y": (176, 120, 70), "h": (210, 160, 105), "d": (140, 90, 50)}),
    (KRUIMEL, {"o": (140, 96, 40), "y": (244, 212, 138)}),
    (GUH, GUH_KLEUREN),
    (WOLK, {"o": (170, 140, 200), "w": (245, 235, 255)}),
    (JERRYCAN, {"o": (74, 44, 18), "y": (201, 138, 60), "b": (120, 74, 31)}),
    (WORST, {"o": (120, 30, 30), "r": (224, 82, 74), "h": (255, 160, 150)}),
    (SLAAPGUH, GUH_KLEUREN),
]
VEL_W, VEL_H = 64, 32


def vel(h):
    img = Image.new("RGBA", (VEL_W, VEL_H), (0, 0, 0, 0))
    for i, (rows, kleuren) in enumerate(VEL):
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), f"sprite {i} is not 16 x 16"
        for y, rij in enumerate(rows):
            for x, c in enumerate(rij):
                if c != ".":
                    img.putpixel(((i % 4) * 16 + x, (i // 4) * 16 + y), kleuren[c] + (255,))
    h.save(img, "gui", "among_taken.png")


# =====================================================================================================================
# the SUS-stickerbord
# =====================================================================================================================
BORD = [
    "................",
    "................",
    ".ffffffffffffff.",
    ".fwwwwwwwwwwwwf.",
    ".fwrrrwrwrwrrrf.",
    ".fwrwwwrwrwrwwf.",
    ".fwrrrwrwrwrrrf.",
    ".fwwwrwrwrwwwrf.",
    ".fwrrrwrrrwrrrf.",
    ".fwwwwwwwwwwwwf.",
    ".fwrrwwbbwwyywf.",
    ".fwrvwwbvwwyvwf.",
    ".fwrrwwbbwwyywf.",
    ".ffffffffffffff.",
    "................",
    "................",
]
BORD_KLEUREN = {"f": (150, 110, 70), "w": (250, 246, 238), "r": (216, 54, 47), "b": (47, 91, 216), "y": (242, 213, 60), "v": (170, 225, 245)}


def sus_bord(h, naam, lore):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, rij in enumerate(BORD):
        for x, c in enumerate(rij):
            img.putpixel((x, y), (BORD_KLEUREN[c] if c != "." else BORD_KLEUREN["f"]) + (255,))
    h.save(img, "block", "among_sus_bord.png")
    rand = Image.new("RGBA", (16, 16), BORD_KLEUREN["f"] + (255,))
    for i in range(0, 16, 4):
        for y in range(16):
            rand.putpixel((i, y), (132, 96, 60, 255))
    h.save(rand, "block", "among_sus_bord_rand.png")
    el = h.el
    lib.muurdeco(h, "among_sus_bord", [
        el([1, 2, 14], [15, 14, 16], "#rand", faces=("down", "up", "south", "west", "east")),
        el([1, 2, 14], [15, 14, 16], "#voor", faces=("north",)),
    ], {"voor": "guhs:block/among_sus_bord", "rand": "guhs:block/among_sus_bord_rand", "particle": "guhs:block/among_sus_bord_rand"}, naam, lore)


# =====================================================================================================================
# the shop's clothes
# =====================================================================================================================
# colour id: (suit colour, darker shade for the tank and the icon's edge)
PAKJES = {
    "rood": ((216, 54, 47), (140, 30, 28)), "blauw": ((47, 91, 216), (26, 52, 136)), "groen": ((47, 168, 74), (24, 104, 42)),
    "geel": ((242, 213, 60), (170, 140, 24)), "roze": ((242, 140, 200), (176, 84, 140)), "oranje": ((242, 144, 47), (170, 92, 20)),
    "paars": ((138, 69, 201), (84, 38, 132)), "wit": ((237, 237, 242), (160, 160, 176)),
}
HOEDJES = ("plantje", "ei", "wc_rol", "kaaspunt", "briefje", "knabbel")
CLOTHES = [f"among_ruimtepakje_{k}" for k in PAKJES] + [f"among_hoedje_{hd}" for hd in HOEDJES]
_B, _H = [0, 6, 6], [0, 6, -2]   # body and head pivot (make_guh_variants BODY_PIVOT, HEAD_PIVOT)
BONES = {
    # the air tank on the back of every Ruimtepakje
    "outfit_among_rugtank": ("body", _B, "among_tank", [([-2.6, 11.2, 2.4], [5.2, 3.0, 4.6], 0), ([-1.6, 14.2, 3.4], [3.2, 0.8, 2.6], 0)]),
    # plantje: a little pot with a stem and two leaves
    "outfit_among_plantje": ("head", _H, "among_pot", [([-1.6, 15, -7.6], [3.2, 2.0, 3.2], 0), ([-1.9, 16.6, -7.9], [3.8, 0.6, 3.8], 0)]),
    "outfit_among_plantje_blad": ("head", _H, "among_blad", [([-0.3, 17.2, -6.3], [0.6, 2.2, 0.6], 0), ([-2.0, 18.8, -6.6], [1.8, 0.4, 1.2], 0),
                                                              ([0.2, 19.4, -6.6], [1.8, 0.4, 1.2], 0)]),
    # ei: a fried egg, flat on the head, the yolk a little off centre
    "outfit_among_ei": ("head", _H, "among_eiwit", [([-3.6, 15, -9.6], [7.2, 0.5, 6.2], 0), ([-2.4, 15, -10.6], [4.0, 0.5, 1.0], 0),
                                                    ([3.6, 15, -8.4], [1.0, 0.5, 3.2], 0)]),
    "outfit_among_ei_dooier": ("head", _H, "among_dooier", [([-1.9, 15.5, -8.2], [3.0, 1.0, 3.0], 0)]),
    # wc_rol: a roll with its cardboard core and a loose sheet
    "outfit_among_wcrol": ("head", _H, "among_papier", [([-2.1, 15, -8.1], [4.2, 3.2, 4.2], 0), ([2.1, 15.2, -7.4], [0.2, 2.4, 2.8], 0)]),
    "outfit_among_wcrol_kern": ("head", _H, "among_karton", [([-0.9, 15.1, -6.9], [1.8, 3.25, 1.8], 0)]),
    # kaaspunt: a wedge in three steps
    "outfit_among_kaaspunt": ("head", _H, "among_kaas", [([-2.5, 15, -9.2], [5.0, 1.2, 5.6], 0), ([-2.5, 16.2, -7.6], [5.0, 1.2, 4.0], 0),
                                                         ([-2.5, 17.4, -6.0], [5.0, 1.0, 2.4], 0)]),
    # briefje: a sticky note standing on the forehead
    "outfit_among_briefje": ("head", _H, "among_briefje", [([-2.2, 15, -9.4], [4.4, 4.4, 0.3], 0)]),
    # knabbel: a giant kaasknabbel with a crumb beside it
    "outfit_among_knabbel": ("head", _H, "among_knabbel", [([-2.6, 15, -8.6], [5.2, 2.4, 3.2], 0), ([-1.6, 17.4, -8.0], [3.2, 1.0, 2.0], 0),
                                                           ([2.2, 15, -5.0], [1.0, 0.8, 1.0], 0)]),
}


def clothes(rng, v):
    out = {}
    for kleur, (pak, donker) in PAKJES.items():
        def suit(c=pak, d=donker):
            a = v.fabric(c, rng, 8)
            a[27:31, :] = d                                    # a darker hem
            a[0:2, :] = np.clip(np.array(c, np.float32) + 26, 0, 255)
            return np.clip(a, 0, 255)

        def tank(c=pak, d=donker):
            a = v.fabric(d, rng, 6)
            a[4:7, :] = (200, 204, 214)                        # two metal straps
            a[24:27, :] = (200, 204, 214)
            a[13:19, 13:19] = np.clip(np.array(c, np.float32) + 20, 0, 255)
            return np.clip(a, 0, 255)
        out[f"among_ruimtepakje_{kleur}"] = {"suit": suit, "among_tank": tank}

    def pot():
        a = v.fabric((190, 104, 66), rng, 8)
        a[0:5, :] = (214, 126, 84)
        a[5:7, :] = (150, 78, 48)
        return np.clip(a, 0, 255)

    def blad():
        a = v.fabric((84, 176, 76), rng, 10)
        a[:, 15:17] = (52, 130, 52)                            # the vein
        return np.clip(a, 0, 255)

    def eiwit():
        a = v.fabric((252, 250, 244), rng, 4)
        a[0:2, :] = a[30:32, :] = (232, 226, 210)
        a[:, 0:2] = a[:, 30:32] = (232, 226, 210)
        return np.clip(a, 0, 255)

    def dooier():
        a = v.fabric((250, 190, 40), rng, 6)
        a[6:12, 8:14] = (255, 226, 120)                        # a shine
        return np.clip(a, 0, 255)

    def papier():
        a = v.fabric((250, 250, 252), rng, 3)
        for y in range(7, 32, 8):
            a[y, :] = (214, 214, 226)                          # the tear lines
        return np.clip(a, 0, 255)

    def karton():
        a = v.fabric((176, 138, 96), rng, 6)
        a[10:22, 10:22] = (70, 52, 36)                         # the hole of the core
        return np.clip(a, 0, 255)

    def kaas():
        a = v.fabric((250, 204, 70), rng, 6)
        for (y, x, r) in ((6, 7, 3), (20, 22, 4), (22, 6, 2), (8, 24, 2)):
            a[y - r:y + r, x - r:x + r] = (226, 164, 40)       # the holes
        a[0:2, :] = (255, 228, 130)
        return np.clip(a, 0, 255)

    def briefje():
        a = v.fabric((255, 238, 110), rng, 3)
        a[0:4, :] = (240, 214, 80)                             # the sticky edge
        rood = (200, 40, 40)
        # "sus" in fat pixels: three letters of 8 x 12, at x 2, 12, 22
        for (x0, rijen) in ((2, ("########", "##......", "########", "......##", "########")),
                            (12, ("##....##", "##....##", "##....##", "##....##", "########")),
                            (22, ("########", "##......", "########", "......##", "########"))):
            for ry, rij in enumerate(rijen):
                for rx, c in enumerate(rij):
                    if c == "#":
                        a[10 + ry * 3:13 + ry * 3, x0 + rx] = rood
        return np.clip(a, 0, 255)

    def knabbel():
        a = v.fabric((244, 176, 52), rng, 12)
        for (y, x) in ((5, 6), (9, 20), (16, 11), (22, 25), (26, 5), (13, 28), (3, 27)):
            a[y:y + 3, x:x + 3] = (255, 220, 120)              # crunchy bits
        for (y, x) in ((12, 4), (20, 18), (7, 14), (27, 14)):
            a[y:y + 2, x:x + 2] = (206, 128, 30)
        return np.clip(a, 0, 255)

    out["among_hoedje_plantje"] = {"among_pot": pot, "among_blad": blad}
    out["among_hoedje_ei"] = {"among_eiwit": eiwit, "among_dooier": dooier}
    out["among_hoedje_wc_rol"] = {"among_papier": papier, "among_karton": karton}
    out["among_hoedje_kaaspunt"] = {"among_kaas": kaas}
    out["among_hoedje_briefje"] = {"among_briefje": briefje}
    out["among_hoedje_knabbel"] = {"among_knabbel": knabbel}
    return out


PAK_ICON = [
    "................",
    ".....aaaaaa.....",
    "....abbbbbba....",
    "...abbvvvvbba...",
    "...abvvwvvvba...",
    "...abvvvvvvba...",
    "..aabbvvvvbbaa..",
    ".attabbbbbbabba.",
    ".attabbbbbbabba.",
    ".attabbbbbbabba.",
    "..aaabbbbbbaaa..",
    "...abbbbbbbba...",
    "...abbbaabbba...",
    "...abba..abba...",
    "...aaaa..aaaa...",
    "................",
]
HOED_ICONS = {
    "plantje": (["................", "......g..gg.....", ".....ggg.ggg....", "......gg.gg.....", ".......ggg......", "........g.......",
                 "........g.......", "....aaaaaaaa....", "....appppppa....", "....aaaaaaaa....", ".....appppa.....", ".....appppa.....",
                 ".....appppa.....", "......aaaa......", "................", "................"],
                {"g": (84, 176, 76), "a": (120, 60, 36), "p": (200, 112, 72)}),
    "ei": (["................", "................", "................", "....aaaaaa......", "..aawwwwwwaaa...", ".awwwwwwwwwwwa..",
            ".awwwyyyywwwwwa.", "awwwyyhyyywwwwa.", "awwwyyyyyywwwwa.", ".awwwyyyywwwwa..", ".awwwwwwwwwwa...", "..aawwwwwwwa....",
            "....aaaaaaa.....", "................", "................", "................"],
           {"a": (190, 184, 168), "w": (252, 250, 244), "y": (250, 190, 40), "h": (255, 232, 140)}),
    "wc_rol": (["................", "................", "....aaaaaaaa....", "...awwwwwwwwa...", "..awwwkkkkwwwa..", "..awwkddddkwwa..",
                "..awwwkkkkwwwa..", "..awwwwwwwwwwa..", "..awwwwwwwwwwaa.", "..allllllllllawa", "..awwwwwwwwwwawa", "..awwwwwwwwwwawa",
                "..allllllllllawa", "...awwwwwwwwaaa.", "....aaaaaaaa....", "................"],
               {"a": (150, 150, 166), "w": (250, 250, 252), "k": (176, 138, 96), "d": (70, 52, 36), "l": (214, 214, 226)}),
    "kaaspunt": (["................", "................", "................", "............aa..", "..........aayya.", "........aayyyya.",
                  "......aayyyhyya.", "....aayyyyyyyya.", "..aayyhyyyyyyya.", ".ayyyyyyyydyyya.", ".ayydyyyyyyyyya.", ".ayyyyyyhyyyyya.",
                  ".aaaaaaaaaaaaaa.", "................", "................", "................"],
                 {"a": (160, 110, 20), "y": (250, 204, 70), "h": (255, 232, 140), "d": (226, 164, 40)}),
    "briefje": (["................", "................", "..aaaaaaaaaaaa..", "..addddddddddda.", "..ayyyyyyyyyyya.", "..ayrryryryrrya.",
                 "..ayryyryryryya.", "..ayrryryryrrya.", "..ayyryryryyrya.", "..ayrryrrryrrya.", "..ayyyyyyyyyyya.", "..ayyyyyyyyyya..",
                 "..ayyyyyyyyya...", "..aaaaaaaaaa....", "................", "................"],
                {"a": (170, 140, 24), "y": (255, 238, 110), "d": (240, 214, 80), "r": (200, 40, 40)}),
    "knabbel": (["................", "................", "................", "....aaaaaaa.....", "..aayyyyyyyaa...", ".ayyhyyyyyyyya..",
                 ".ayyyyydyyyhya..", "ayyyyyyyyyyyyya.", "ayydyyyyhyyyyya.", ".ayyyyyyyyydya..", ".ayyhyyyyyyyya..", "..aayyyyyyyaa...",
                 "....aaaaaaa..aa.", ".............aya", "..............a.", "................"],
                {"a": (150, 90, 16), "y": (244, 176, 52), "h": (255, 220, 120), "d": (206, 128, 30)}),
}


def icons(ic):
    out = {}
    for kleur, (pak, donker) in PAKJES.items():
        out[f"among_ruimtepakje_{kleur}"] = ic.icon(PAK_ICON, {"a": donker, "b": pak, "v": (150, 214, 246), "w": (255, 255, 255),
                                                               "t": tuple(min(255, c + 30) for c in donker)})
    for hoed, (rows, kleuren) in HOED_ICONS.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), f"icon {hoed} is not 16 x 16"
        out[f"among_hoedje_{hoed}"] = ic.icon(rows, kleuren)
    return out
