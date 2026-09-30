"""
Item icons (16x16 pixel art) for the guh clothes that don't have one yet (see make_village_textures.py for the first
nine). Each icon is drawn from a small shape template in the piece's colours.

Run from the project root:  python tools/make_clothes_icons.py
"""
import os

import numpy as np
from PIL import Image

OUT = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "item")


def icon(rows, colours):
    img = np.zeros((16, 16, 4), np.uint8)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row.ljust(16, ".")):
            if ch in colours:
                img[y, x] = (*colours[ch], 255)
    return Image.fromarray(img)


def pad(rows):
    top = (16 - len(rows)) // 2
    return ["." * 16] * top + rows + ["." * 16] * (16 - len(rows) - top)


SHIRT = ["...aaa....aaa...", "..abbbaaaabbba..", ".abbbbbbbbbbbba.", ".abbbbbbbbbbbba.", ".aabbbbbbbbbbaa.",
         "..aabbbbbbbbaa..", "...abbbbbbbba...", "...abbbbbbbba...", "...abbbbbbbba...", "...abbbbbbbba...",
         "...abbbbbbbba...", "...aaaaaaaaaa..."]
SHAPES = {
    "helmet": pad(["....aaaaaaa.....", "...abbbcbbba....", "..abbbbcbbbba...", "..abbbbcbbbba...", ".aabbbbcbbbbaa..",
                   "aaaaaaaaaaaaaaa."]),
    "cap": pad(["....aaaaaa......", "...abbbbbba.....", "..abbbcbbbba....", "..abbbbbbbba....", "..aaaaaaaaaaaaa.",
                "..........aaaa.."]),
    "crown": pad(["..a..a..a..a....", "..aa.aa.aa.aa...", "..abbbbbbbbba...", "..abcbbcbbcba...", "..abbbbbbbbba...",
                  "..aaaaaaaaaaa..."]),
    "tall_hat": pad([".........aa.....", "........abba....", ".......abba.....", "......abbba.....", ".....abbbbba....",
                     ".....abcbbba....", "....abbbbbbba...", "....abbbbcbba...", ".aaaaaaaaaaaaaa.", "aaaaaaaaaaaaaaaa"]),
    "tricorn": pad(["......aaaa......", "....aabbbbaa....", "..aabbbbbbbbaa..", ".abbbbbbbbbbbba.", "abcccccccccccba",
                    ".aaaaaaaaaaaaaa."]),
    "santa": pad(["...........ww...", ".........aaww...", "......aabbba....", ".....abbbbba....", "....abbbbbbba...",
                  "...abbbbbbbbba..", "..wwwwwwwwwwwww.", "..wwwwwwwwwwwww."]),
    "mitre": pad(["......aa........", ".....abba.......", "....abccba......", "...abbccbba.....", "...abbccbba.....",
                  "...acccccca.....", "...abbccbba.....", "...aaaaaaaa....."]),
    "beret": pad(["..........w.....", "..........w.....", "....aaaaaaw.....", "..aabbbbbbbaa...", ".abbbbbbbbbbba..",
                  "aaaaaaaaaaaaaaa."]),
    "pumpkin": pad(["......dd........", "...aaaaaaaaa....", "..abbbabbbbba...", ".abkkbbbbkkbba..", ".abbbbbbbbbbba..",
                    ".abkbkbkbkbkba..", ".abbkkkkkkkbba..", "..abbbabbbbba...", "...aaaaaaaaa...."]),
    "rim_hat": pad([".....aaaaaa.....", "....abbbbbba....", "....abbbbbba....", "..aacccccccaa...", "aabbbbbbbbbbbaa.",
                    ".aaaaaaaaaaaaa.."]),
    "glasses": pad(["aaaaaaa..aaaaaaa", "abbbbba..abbbbba", "abbbbbaaaabbbbba", "abbbbba..abbbbba", ".aaaaa....aaaaa."]),
    "hearts": pad([".aa.aa....aa.aa.", "abbabba..abbabba", "abbbbbaaaabbbbba", ".abbba....abbba.", "..aba......aba..",
                   "...a........a..."]),
    "monocle": pad(["....aaaaa.......", "...a.....a......", "...a.....a......", "...a.....a......", "...a.....a......",
                    "....aaaaa.......", ".........a......", "..........a.....", "...........a...."]),
    "eyepatch": pad(["aaaaaaaaaaaaaaaa", "....abbbbba.....", "....abbbbba.....", "....abbbbba.....", ".....abbba......",
                     "......aaa......."]),
    "scarf": pad(["..aaaaaaaaaaa...", ".abcbcbcbcbcba..", ".abcbcbcbcbcba..", "..aaaaaaaabca...", ".........abca...",
                  ".........acba...", ".........abca...", ".........aaaa..."]),
    "stethoscope": pad(["..a.........a...", "..a.........a...", "...a.......a....", "....a.....a.....", ".....aaaaa......",
                        ".......a........", ".......a........", "......bbb.......", ".....bcccb......", "......bbb......."]),
    "backpack": pad(["....aaaaaa......", "...abbbbbba.....", "..aaaaaaaaaa....", "..abbbbbbbba....", "..abbccccbba....",
                     "..abbbbbbbba....", "..abbbbbbbba....", "..aaaaaaaaaa...."]),
    "cape": pad(["...aaaaaaaa.....", "..abbbbbbbba....", "..abbbbbbbba....", ".abbbbbbbbbba...", ".abbbbbbbbbba...",
                 "abbbbbbbbbbbba..", "wwwwwwwwwwwwww..", "wkwwkwwkwwkwww.."]),
}


def shirt(main, dark, extra=None, pattern=None):
    rows = list(SHIRT)
    if pattern == "stripes":
        rows = [r.replace("b", "c") if i % 3 == 1 else r for i, r in enumerate(rows)]
    elif pattern == "buttons":
        rows = [r[:7] + ("c" if "b" in r[6:9] and i % 2 else r[7]) + r[8:] for i, r in enumerate(rows)]
    elif pattern == "zigzag":
        rows = [r.replace("b", "c", 3) if i in (4, 8) else r for i, r in enumerate(rows)]
    elif pattern == "stars":
        rows = [r[:5] + "c" + r[6:] if i in (3, 7) else (r[:10] + "c" + r[11:] if i in (5, 9) else r) for i, r in enumerate(rows)]
    return icon(pad(rows), {"a": dark, "b": main, "c": extra or main})


def shaped(shape, a, b, c=None, **more):
    colours = {"a": a, "b": b, "c": c or b, "w": (250, 250, 250), "k": (40, 20, 5), "d": (60, 120, 30)}
    colours.update(more)
    return icon(SHAPES[shape], colours)


ICONS = {
    "sunglasses": shaped("glasses", (10, 10, 12), (35, 35, 45)),
    "heart_glasses": shaped("hearts", (150, 20, 80), (255, 90, 170)),
    "monocle": shaped("monocle", (230, 190, 60), (230, 190, 60)),
    "eyepatch": shaped("eyepatch", (10, 10, 10), (30, 30, 34)),
    "firefighter_helmet": shaped("helmet", (120, 10, 10), (210, 30, 30), (250, 210, 60)),
    "firefighter_jacket": shirt((140, 110, 60), (90, 70, 30), (240, 220, 60), "stripes"),
    "police_cap": shaped("cap", (15, 20, 50), (30, 40, 90), (230, 200, 70)),
    "police_uniform": shirt((35, 45, 100), (15, 20, 50), (230, 200, 70), "buttons"),
    "doctor_coat": shirt((245, 245, 245), (160, 170, 180), (150, 200, 230), "buttons"),
    "stethoscope": shaped("stethoscope", (90, 90, 100), (170, 170, 180), (220, 220, 230)),
    "builder_helmet": shaped("helmet", (180, 140, 0), (250, 200, 20), (255, 230, 90)),
    "safety_vest": shirt((255, 120, 20), (180, 70, 0), (210, 215, 220), "stripes"),
    "straw_hat": shaped("rim_hat", (160, 130, 60), (226, 196, 110), (200, 60, 60)),
    "overalls": shirt((60, 100, 170), (30, 55, 110), (220, 190, 80), "buttons"),
    "santa_hat": shaped("santa", (120, 10, 15), (200, 20, 30)),
    "christmas_sweater": shirt((190, 20, 30), (110, 10, 15), (245, 245, 245), "zigzag"),
    "winter_scarf": shaped("scarf", (120, 15, 20), (200, 30, 40), (245, 245, 245)),
    "sint_mitre": shaped("mitre", (120, 10, 15), (190, 20, 30), (240, 200, 60)),
    "piet_beret": shaped("beret", (20, 15, 30), (40, 30, 60)),
    "witch_hat": shaped("tall_hat", (25, 10, 35), (50, 20, 70), (120, 60, 150)),
    "ghost_sheet": shirt((248, 248, 252), (190, 190, 205)),
    "pumpkin_head": shaped("pumpkin", (180, 90, 10), (236, 130, 30)),
    "orange_crown": shaped("crown", (190, 90, 0), (255, 140, 0), (255, 220, 120)),
    "orange_shirt": shirt((255, 130, 10), (190, 80, 0)),
    "wizard_hat": shaped("tall_hat", (20, 25, 90), (40, 50, 150), (250, 230, 120)),
    "wizard_robe": shirt((35, 40, 120), (20, 20, 70), (250, 230, 120), "stars"),
    "knight_helmet": shaped("helmet", (100, 105, 115), (180, 185, 195), (230, 230, 235)),
    "knight_armour": shirt((160, 165, 178), (100, 105, 115), (230, 230, 235), "buttons"),
    "royal_crown": shaped("crown", (180, 130, 20), (245, 200, 60), (220, 40, 60)),
    "royal_cape": shaped("cape", (100, 10, 25), (170, 20, 40)),
    "pirate_hat": shaped("tricorn", (10, 10, 12), (25, 25, 30), (230, 200, 90)),
    "guh_backpack": shaped("backpack", (140, 70, 100), (230, 120, 170), (255, 200, 225)),
    "kermis_hoed": shaped("tall_hat", (120, 10, 25), (210, 30, 50), (250, 245, 240)),
    "kermis_jasje": shirt((200, 30, 50), (120, 15, 30), (250, 200, 60), "buttons"),
    "detective_pet": shaped("cap", (90, 60, 35), (150, 110, 70), (200, 160, 110)),
    "detective_vergrootglas": shaped("monocle", (200, 170, 90), (170, 220, 240)),
    "detective_jas": shirt((170, 130, 80), (110, 80, 45), (90, 60, 35), "buttons"),
    "koning_kroon": shaped("crown", (170, 120, 20), (250, 205, 60), (210, 30, 90)),
    "koning_mantel": shaped("cape", (80, 15, 110), (130, 30, 160)),
    "koning_ketting": shaped("stethoscope", (190, 140, 30), (245, 200, 60), (255, 235, 140)),
}

if __name__ == "__main__":
    import sys
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import features
    for module in features.modules():  # the 2.4 features' own clothes
        if hasattr(module, "icons"):
            ICONS.update(module.icons(sys.modules[__name__]))
    os.makedirs(OUT, exist_ok=True)
    for name, img in ICONS.items():
        img.save(os.path.join(OUT, f"{name}.png"))
    print(f"{len(ICONS)} clothing icons written")
