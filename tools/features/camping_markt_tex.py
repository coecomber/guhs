"""
bbq2 (camping-markt): the textures of features/camping_markt.py, all painted here (numpy / PIL, 16x16 unless said).

  blocks   tent canvas in five colours (a woven cloth with a seam), the pitch sign (free / taken), the tent peg, the
           chopping block (bark, rings with axe cuts), the camp fire's ash bed, the brass of the scales, the stack of vads
           bars and its five number plates
  items    the tent bag, the bundle of fire wood, the roasting stick, the recipe card of the Plantagebak, the Keurstempel
           (ICONS, with ITEM_PAL)
"""
import random

import numpy as np

from features import barbecuether_tex as bt

# tent canvas: (base, light thread, dark thread)
DOEK = {
    "rood": ((196, 64, 56), (222, 96, 84), (150, 44, 40)),
    "geel": ((232, 190, 70), (248, 214, 110), (190, 148, 44)),
    "groen": ((98, 140, 80), (128, 168, 104), (70, 106, 58)),
    "blauw": ((78, 126, 176), (112, 156, 200), (54, 94, 138)),
    "creme": ((236, 226, 200), (250, 244, 226), (200, 188, 160)),
}
HOUT = (126, 92, 56)
HOUT_LICHT = (164, 126, 82)
HOUT_DONKER = (84, 58, 36)
SCHORS = (72, 50, 34)
IJZER = (176, 178, 186)
IJZER_LICHT = (226, 228, 234)
IJZER_DONKER = (104, 106, 116)
MESSING = (214, 172, 70)
MESSING_LICHT = (250, 222, 130)
MESSING_DONKER = (150, 110, 36)
VADS = (142, 86, 172)
VADS_LICHT = (196, 146, 222)
VADS_DONKER = (92, 50, 122)
VADS_RAND = (58, 28, 84)


def tentdoek(kleur, seed):
    """Woven canvas: threads over and under each other, a stitched seam along the bottom edge."""
    basis, licht, donker = (np.array(c, np.float32) for c in DOEK[kleur])
    a = np.zeros((16, 16, 3), np.float32)
    for y in range(16):
        for x in range(16):
            over = (x // 2 + y // 2) % 2 == 0
            a[y, x] = basis * 0.6 + (licht if over else donker) * 0.4
            if x % 2 == 0 and over:
                a[y, x] = a[y, x] * 0.5 + licht * 0.5
    a = bt.grain(a, seed, 3)
    a[14, :] = donker                                          # the seam
    a[13, 1::3] = licht                                        # the stitches
    a[15, :] = basis * 0.85
    return bt.img(a)


def plank(seed, tekst=None, kleur=(88, 150, 70)):
    """A wooden board with a painted word: "VRIJ" (green) or "BEZET" (red), in 3x5 pixel letters."""
    a = np.zeros((16, 16, 3), np.float32)
    for y in range(16):
        for x in range(16):
            a[y, x] = HOUT_DONKER if y % 5 == 4 else HOUT_LICHT if (x * 3 + y // 5 * 7) % 13 == 0 else HOUT
    a = bt.grain(a, seed, 4)
    if tekst:
        letters = {"V": ["x.x", "x.x", "x.x", "x.x", ".x."], "R": ["xx.", "x.x", "xx.", "x.x", "x.x"], "I": ["x", "x", "x", "x", "x"],
                   "J": [".x", ".x", ".x", "xx", "xx"], "B": ["xx.", "x.x", "xx.", "x.x", "xx."], "E": ["xx", "x.", "xx", "x.", "xx"],
                   "Z": ["xxx", "..x", ".x.", "x..", "xxx"], "T": ["xxx", ".x.", ".x.", ".x.", ".x."]}
        breed = sum(len(letters[c][0]) + 1 for c in tekst) - 1
        x0 = (16 - breed) // 2
        a[4:11, max(0, x0 - 1):min(16, x0 + breed + 1)] = (244, 238, 222)      # a painted white patch
        for c in tekst:
            for dy, rij in enumerate(letters[c]):
                for dx, ch in enumerate(rij):
                    if ch == "x" and 0 <= x0 + dx < 16:
                        a[5 + dy, x0 + dx] = kleur
            x0 += len(letters[c][0]) + 1
    return bt.img(a)


def ijzer(seed):
    """Forged iron of a tent peg: grey with a light edge and hammer dents."""
    n = bt.vnoise(16, 4, seed, 2)
    a = bt.ramp(n, [(0, IJZER_DONKER), (0.5, IJZER), (1, IJZER_LICHT)])
    a = bt.grain(a, seed, 4)
    a[:, 0] = IJZER_LICHT
    a[:, 15] = IJZER_DONKER
    return bt.img(a)


def stronk_zij(seed):
    """Bark of the chopping block: dark ridges running up."""
    rng = random.Random(seed)
    a = np.zeros((16, 16, 3), np.float32)
    for x in range(16):
        kleur = SCHORS if rng.random() < 0.55 else HOUT_DONKER
        for y in range(16):
            a[y, x] = kleur if rng.random() < 0.85 else HOUT
    a = bt.grain(a, seed, 5)
    a[0, :] = HOUT_LICHT                                       # the cut edge on top
    return bt.img(a)


def stronk_boven(seed):
    """The top of the chopping block: year rings, and the scars of many axe blows."""
    a = np.zeros((16, 16, 3), np.float32)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            a[y, x] = SCHORS if d > 7.4 else HOUT if int(d * 1.3) % 2 else HOUT_LICHT
    a = bt.grain(a, seed, 4)
    for (x0, y0, dx, dy, n) in ((3, 5, 1, 1, 4), (9, 3, 1, 0, 4), (6, 10, 1, -1, 3), (10, 9, 0, 1, 3)):   # axe cuts
        for i in range(n):
            a[y0 + dy * i, x0 + dx * i] = HOUT_DONKER
    return bt.img(a)


def asbed(seed):
    """The bed of the camp fire: grey ash with charred bits and a few embers."""
    a = np.asarray(bt.as_aarde(seed).convert("RGB")).astype(np.float32)
    rng = np.random.default_rng(seed)
    for _ in range(9):
        a[rng.integers(2, 14), rng.integers(2, 14)] = bt.CHAR_DARK
    for _ in range(4):
        a[rng.integers(4, 12), rng.integers(4, 12)] = bt.EMBER
    return bt.img(a)


def messing(seed):
    """Polished brass of the scales."""
    n = bt.vnoise(16, 3, seed, 2)
    a = bt.ramp(n, [(0, MESSING_DONKER), (0.45, MESSING), (1, MESSING_LICHT)])
    a = bt.grain(a, seed, 3)
    a[0, :] = MESSING_LICHT
    a[15, :] = MESSING_DONKER
    return bt.img(a)


def vadsstaven(seed):
    """Bars of vahoege vads, stacked: purple bars with a light top edge, a dark outline and a little shine."""
    a = np.zeros((16, 16, 3), np.float32)
    for rij in range(4):
        y0 = rij * 4
        schuif = 0 if rij % 2 == 0 else 4
        for x in range(16):
            xx = (x + schuif) % 8
            a[y0, x] = VADS_LICHT
            a[y0 + 1, x] = VADS
            a[y0 + 2, x] = VADS
            a[y0 + 3, x] = VADS_DONKER
            if xx == 0:
                a[y0:y0 + 4, x] = VADS_RAND
            if xx == 2:
                a[y0 + 1, x] = (236, 206, 250)                 # the shine
    return bt.img(bt.grain(a, seed, 3))


CIJFERS = {1: [".x.", "xx.", ".x.", ".x.", "xxx"], 2: ["xx.", "..x", ".x.", "x..", "xxx"], 3: ["xx.", "..x", ".x.", "..x", "xx."],
           4: ["x.x", "x.x", "xxx", "..x", "..x"], 5: ["xxx", "x..", "xx.", "..x", "xx."]}


def nummerbord(n, seed):
    """The brass plate with a stack's number (the whole 16x16 texture; the model shows its middle)."""
    a = np.asarray(messing(seed).convert("RGB")).astype(np.float32)
    a[3:13, 3:13] = (250, 240, 214)
    a[3, 3:13] = a[12, 3:13] = MESSING_DONKER
    a[3:13, 3] = a[3:13, 12] = MESSING_DONKER
    for dy, rij in enumerate(CIJFERS[n]):
        for dx, ch in enumerate(rij):
            if ch == "x":
                a[5 + dy, 6 + dx] = VADS_RAND
    return bt.img(a)


# =====================================================================================================================
# items
# =====================================================================================================================
ITEM_PAL = {
    ".": (0, 0, 0, 0), "k": (40, 30, 26, 255), "g": (98, 140, 80, 255), "G": (70, 106, 58, 255), "l": (128, 168, 104, 255),
    "y": (232, 190, 70, 255), "Y": (190, 148, 44, 255), "h": HOUT + (255,), "H": HOUT_DONKER + (255,), "L": HOUT_LICHT + (255,),
    "s": SCHORS + (255,), "t": (220, 200, 160, 255), "T": (176, 150, 110, 255), "w": (252, 250, 246, 255), "W": (224, 216, 206, 255),
    "p": (250, 190, 210, 255), "c": (244, 238, 222, 255), "C": (206, 196, 172, 255), "r": (200, 56, 50, 255), "R": (150, 36, 34, 255),
    "v": VADS + (255,), "V": VADS_DONKER + (255,), "m": MESSING + (255,), "M": MESSING_DONKER + (255,), "n": (250, 222, 130, 255),
    "b": (120, 160, 90, 255), "o": (186, 112, 40, 255),
}
ICONS = {
    # the tent bag: a green duffel with two straps and a yellow cord, a tent pole sticking out
    "campingmarkt_tentzak": ["................", "............hH..", "...........hH...", "....kkkkkkkhk...", "...kllgggggGGk..",
                             "..klgYggggYgGGk.", "..kggYggggYggGk.", "..kggYggggYggGk.", "..kggYggggYggGk.", "..kggYggggYggGk.",
                             "..kgGYggggYgGGk.", "...kGGGGGGGGGk..", "....kkkkkkkkk...", "................", "................",
                             "................"],
    # a bundle of split logs with a cord around it
    "campingmarkt_brandhout": ["................", "................", "....kkk..kkk....", "...kLLhkkLLhk...", "..kLhhsksLhhsk..",
                               "..khhsskkhhssk..", ".kkkttttttttkkk.", ".kLtTTTTTTTTthk.", ".kkkttttttttkkk.", "..kLhhskkLhhsk..",
                               "..khhsskkhhssk..", "...khssk.khssk..", "....kkk...kkk...", "................", "................",
                               "................"],
    # the roasting stick: a long stick with a fat white marshmallow (a pink blush) on its tip
    "campingmarkt_roosterstok": ["................", "..........kkkk..", ".........kwwwpk.", ".........kwwwwk.", ".........kWwwwk.",
                                 "........hkWWWk..", ".......hH.kkk...", "......hH........", ".....hH.........", "....hH..........",
                                 "...hH...........", "..hH............", ".hH.............", ".H..............", "................",
                                 "................"],
    # the recipe card: a sheet with a little tree in a planter and lines of writing
    "campingmarkt_recept_plantagebak": ["................", "..kkkkkkkkkkkk..", "..kcccccccccCk..", "..kcccbbbcccCk..", "..kccbbbbbccCk..",
                                        "..kccbbbbbccCk..", "..kccchhhcccCk..", "..kcchhhhhccCk..", "..kcchooohccCk..", "..kcchhhhhccCk..",
                                        "..kccccccccCCk..", "..kcTTTTTTTcCk..", "..kcTTTTTccCCk..", "..kCCCCCCCCCCk..", "..kkkkkkkkkkkk..",
                                        "................"],
    # the Keurstempel: a wooden grip, a brass collar, a red stamp face
    "campingmarkt_keurstempel": ["................", "......kkkk......", ".....kLLhhk.....", ".....kLhhhk.....", ".....khhhHk.....",
                                 "......khHk......", "......khHk......", "......khHk......", ".....kmnmMk.....", "....kmnmmmMk....",
                                 "...kkkkkkkkkk...", "...krrrrrrRRk...", "...krrrrrRRRk...", "...kkkkkkkkkk...", "................",
                                 "................"],
}
