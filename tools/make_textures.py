"""
Generates the block/item pixel-art textures. Edit the PNGs directly in any pixel editor if you prefer -
re-running this script overwrites them.

Requires: pillow, numpy.   Run from the project root:  python tools/make_textures.py
"""
import math
import os

import numpy as np
from PIL import Image

TEX = os.path.join("src", "main", "resources", "assets", "guhs", "textures")


def from_grid(grid, palette):
    img = Image.new("RGBA", (len(grid[0]), len(grid)))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            img.putpixel((x, y), palette[ch])
    return img


def save(img, *path):
    full = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)
    print("wrote", full)


# --- Kaas Knabbels bag (item) -----------------------------------------------------------------------------------
BAG = {'.': (0, 0, 0, 0), 'o': (150, 78, 20, 255), 'c': (214, 150, 48, 255), 'C': (250, 205, 90, 255),
       'y': (248, 196, 37, 255), 'Y': (255, 228, 92, 255), 'g': (232, 150, 42, 255), 'r': (214, 38, 28, 255),
       'R': (150, 22, 26, 255), 'k': (255, 236, 60, 255), 'p': (236, 160, 70, 255), 'P': (255, 200, 120, 255)}
save(from_grid([
    "...oCcCcCcCco...",
    "...ocCcCcCcCo...",
    "..oyYyyyyyygyo..",
    "..oYyRRRRRRygo..",
    "..oyRrkrkkrRgo..",
    "..oRrkrkrkkrRo..",
    "..oRrrrrrrrrRo..",
    "..oRkkrkkrkkRo..",
    "..oyRrrrrrrRgo..",
    "..oyyRRRRRRygo..",
    "..oYPPpyyyyygo..",
    "..oyypPPpyPpgo..",
    "..oyyyyypPPpgo..",
    "..orrygyyyyggo..",
    "...ocCcCcCcCo...",
    "...oCcCcCcCco...",
], BAG), "item", "kaas_knabbels.png")

# --- cheese puffs overlay for the kaasknabbel ores (transparent background) ----------------------------------------
PUFF = {'.': (0, 0, 0, 0), '#': (74, 34, 8, 255), 's': (184, 88, 18, 255), 'o': (240, 138, 36, 255),
        'h': (255, 190, 90, 255), 'w': (255, 226, 150, 255)}
save(from_grid([
    "..###...........",
    ".#hhw#......###.",
    "#ohhh#.....#hhw#",
    "#oohs#....#ohhh#",
    ".#sos#...#oohs#.",
    ".#soo##.#oohs#..",
    "..#sooo#ooos#...",
    "...#ssssss##....",
    "....######......",
    "................",
    "....####........",
    "...#hhhw##......",
    "..#ohhhhhw###...",
    "..#oooooohhhh#..",
    "...##sssssooo#..",
    ".....#######s#..",
], PUFF), "block", "kaasknabbel_overlay.png")

# --- Block of Kaasknabbels: a seamless pile of puffs -------------------------------------------------------------
PILE = {'#': (96, 44, 10, 255), 's': (184, 88, 18, 255), 'o': (236, 132, 34, 255), 'O': (246, 152, 44, 255),
        'h': (255, 188, 88, 255), 'w': (255, 222, 146, 255), 'd': (140, 64, 14, 255)}
save(from_grid([
    "oohw#ssoooohw#so",
    "ooos#dssooooos#s",
    "#ss#hhw#sssss#dd",
    "d##ohhhw#####hhw",
    "ooOOoohhs#dsOhhh",
    "ssOOoooos#sOOOoo",
    "#ssssooss#sooss#",
    "hw####ss##ssss#h",
    "hhhw#dd#hhw##dhh",
    "oohhs#sohhhhs#oo",
    "ooooss#ooooos#so",
    "#sooos#ssooss#ss",
    "##ss##hw#sss#hhw",
    "w#dd#hhhw###Ohhh",
    "hs#sOOhhhs#sOOoo",
    "os#sOOoos#dsooos",
], PILE), "block", "block_of_kaasknabbels.png")

# --- Guh portal: animated pink swirl with drifting cheese sparkles -----------------------------------------------
FRAMES = 16
portal = Image.new("RGBA", (16, 16 * FRAMES))
rng = np.random.default_rng(3)
sparkles = rng.uniform(0, 16, (6, 2))
for f in range(FRAMES):
    t = f / FRAMES
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            swirl = 0.5 + 0.5 * math.sin(3 * a + r * 0.9 - t * 2 * math.pi)
            wave = 0.5 + 0.5 * math.sin(x * 0.7 + y * 0.4 + t * 2 * math.pi)
            v = 0.65 * swirl + 0.35 * wave
            col = (int(235 + 20 * v), int(120 + 90 * v), int(170 + 60 * v), int(150 + 80 * v))
            portal.putpixel((x, y + 16 * f), col)
    for sx, sy in sparkles:
        px = int(sx) % 16
        py = int(sy - t * 16) % 16
        portal.putpixel((px, py + 16 * f), (255, 190, 70, 235))
save(portal, "block", "guh_portal.png")
with open(os.path.join(TEX, "block", "guh_portal.png.mcmeta"), "w") as fh:
    fh.write('{\n  "animation": {\n    "frametime": 2,\n    "interpolate": true\n  }\n}\n')

# --- Mika's vet: a jar of fat with a red lid and an evil little label ---------------------------------------------
VET = {'.': (0, 0, 0, 0), 'l': (150, 20, 40, 255), 'L': (200, 45, 60, 255), 'g': (200, 215, 225, 255),
       'G': (240, 248, 255, 255), 'f': (250, 238, 190, 255), 'F': (255, 250, 220, 255), 'd': (225, 205, 150, 255),
       'k': (40, 12, 28, 255), 'r': (220, 30, 45, 255), 'p': (226, 170, 190, 255)}
save(from_grid([
    "................",
    "....llllllll....",
    "...lLLLLLLLLl...",
    "...llllllllll...",
    "....gGGGGGGg....",
    "...gFFFFFFFFg...",
    "...gFfppppfFg...",
    "...gfpkrrkpfg...",
    "...gfppkkppfg...",
    "...gfpprrppfg...",
    "...gffppppffg...",
    "...gfffffffdg...",
    "...gffffffddg...",
    "...gdfffffddg...",
    "....gddddddg....",
    ".....gggggg.....",
], VET), "item", "mika_vet.png")

# --- gefrituurde kaasknabbels: golden fried puffs in a paper cone -------------------------------------------------
FRY = {'.': (0, 0, 0, 0), '#': (90, 40, 8, 255), 's': (160, 82, 16, 255), 'o': (214, 128, 28, 255),
       'h': (246, 178, 60, 255), 'w': (255, 220, 120, 255), 'p': (250, 250, 245, 255), 'P': (215, 215, 210, 255),
       'r': (210, 40, 40, 255)}
save(from_grid([
    "....##..##......",
    "...#hw##hw#.....",
    "..#ohhs#ohh#....",
    "..#soo##sos#....",
    ".#hw#s#hww#s#...",
    ".#ohh##ohhs##...",
    "..#ss#rpprpr#...",
    "...##prrprrp....",
    "....pPrpprrP....",
    "....rprrpPr.....",
    ".....prpPrp.....",
    ".....rPrrp......",
    "......prp.......",
    "......Pr........",
    ".......p........",
    "................",
], FRY), "item", "gefrituurde_kaasknabbels.png")


def solid(color, noise=6, seed=1):
    rng2 = np.random.default_rng(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = int(rng2.integers(-noise, noise + 1))
            img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in color) + (255,))
    return img


# --- frying pan: dark iron, golden oil, black handle --------------------------------------------------------------
save(solid((62, 62, 68), 5, 11), "block", "frying_pan_iron.png")
save(solid((70, 44, 30), 4, 12), "block", "frying_pan_handle.png")
oil = solid((226, 170, 40), 10, 13)
for x, y in ((3, 4), (10, 9), (6, 12), (12, 3)):
    oil.putpixel((x, y), (255, 236, 150, 255))
save(oil, "block", "frying_pan_oil.png")

# --- guh wheel: coral plastic ring and purple plastic stand -------------------------------------------------------
save(solid((248, 128, 118), 7, 21), "block", "guh_wheel_coral.png")
save(solid((150, 110, 205), 6, 22), "block", "guh_wheel_purple.png")

# --- guh spawner: a pink version of the spawner cage --------------------------------------------------------------
cage = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        edge = x in (0, 15) or y in (0, 15)
        bar = x in (5, 10) or y in (5, 10)
        if edge:
            cage.putpixel((x, y), (168, 60, 118, 255))
        elif bar:
            cage.putpixel((x, y), (236, 120, 170, 255) if (x + y) % 3 else (255, 180, 210, 255))
save(cage, "block", "guh_spawner.png")

# --- Bank Guh icon: a sitting pink guh with a little gold coin -----------------------------------------------------
BANK = {'.': (0, 0, 0, 0), 'o': (168, 96, 120, 255), 'p': (246, 204, 218, 255), 'P': (255, 228, 236, 255),
        'e': (236, 148, 165, 255), 'k': (30, 25, 40, 255), 'b': (90, 170, 225, 255), 'w': (255, 255, 255, 255),
        'n': (236, 118, 140, 255), 'g': (240, 190, 40, 255), 'G': (255, 230, 120, 255), 'd': (170, 120, 20, 255)}
save(from_grid([
    ".oo........oo...",
    "oeeo......oeeo..",
    "oeepoooooopeeo..",
    ".opPPPPPPPPppo..",
    ".oPPPPPPPPPPpo..",
    ".oPkbPPPPkbPpo..",
    ".oPkwPPPPkwPpo..",
    ".opPPPnnPPPppo..",
    "..opppppppppo...",
    "...oppnnnppo....",
    "..oppnnnnnppo.gg",
    "..oPpnnnnnppogGG",
    "..opppnnnpppodgG",
    "..oooppppppoo.dd",
    ".oPPo.oooo.oPPo.",
    ".oooo......oooo.",
], BANK), "item", "bank_guh.png")


# --- guh wire item: a little pile of pink dust -------------------------------------------------------------------
DUST = {'.': (0, 0, 0, 0), 'd': (150, 50, 90, 255), 'p': (235, 90, 160, 255), 'P': (255, 150, 205, 255), 'w': (255, 215, 235, 255)}
save(from_grid([
    "................",
    "................",
    "................",
    "........P.......",
    ".......pPp......",
    "......p.p.......",
    "....pP.pwp..p...",
    "...ppPpPPpp.Pp..",
    "..d.pPPwPPpp....",
    "...dppPPPPppd...",
    ".p.dpppPpppd..p.",
    "....ddpppppdd...",
    "..p..ddddddd....",
    "................",
    "................",
    "................",
], DUST), "item", "guh_wire.png")

# --- picked-up guh item: a guh face ---------------------------------------------------------------------------------
FACE = {'.': (0, 0, 0, 0), 'o': (168, 96, 120, 255), 'p': (246, 204, 218, 255), 'P': (255, 228, 236, 255),
        'e': (236, 148, 165, 255), 'k': (30, 25, 40, 255), 'b': (90, 170, 225, 255), 'w': (255, 255, 255, 255),
        'n': (236, 118, 140, 255), 'r': (245, 160, 180, 255)}
save(from_grid([
    ".oo..........oo.",
    "oeeo........oeeo",
    "oeepoooooooopeeo",
    ".opPPPPPPPPPPpo.",
    ".oPPPPPPPPPPPPo.",
    "oPPkkPPPPPPkkPPo",
    "oPkbwkPPPPkbwkPo",
    "oPkbbkPPPPkbbkPo",
    "oPPkkPPPPPPkkPPo",
    "oPrrPPPnnPPPrrPo",
    "oPPPPPPPPPPPPPPo",
    ".oPPPPnPPnPPPPo.",
    ".opPPPPnnPPPPpo.",
    "..oppPPPPPPppo..",
    "...oooooooooo...",
    "................",
], FACE), "item", "picked_up_guh.png")


# --- bucket of kaas saus -----------------------------------------------------------------------------------------
PAIL = {'.': (0, 0, 0, 0), 'k': (60, 60, 64, 255), 'g': (140, 140, 148, 255), 'G': (200, 200, 206, 255),
        'w': (235, 235, 240, 255), 's': (242, 165, 22, 255), 'S': (255, 205, 80, 255), 'd': (200, 120, 10, 255)}
save(from_grid([
    "................",
    "................",
    "...kkkkkkkkkk...",
    "..kSSSsSSSsSSk..",
    "..kgsSSsssSSgk..",
    "..kgGssdsssGgk..",
    "..kgGwgggggGgk..",
    "...kgwgggggGk...",
    "...kgwgggggGk...",
    "...kgGgggggGk...",
    "....kgggggggk...",
    "....kgggggggk...",
    "....kgGggggGk...",
    ".....kkkkkkk....",
    "................",
    "................",
], PAIL), "item", "kaas_saus_bucket.png")


# --- guh armour icons: a little helmet with ear holes, per tier ------------------------------------------------------
HELMET = [
    "................",
    "................",
    "..oo........oo..",
    ".oeeo......oeeo.",
    ".oeeoooooooeeo..",
    "..ohhhhhhhhhho..",
    ".ohhmmmmmmmmhho.",
    ".ohmmmmmmmmmmdo.",
    ".ommmmmmmmmmmdo.",
    ".ommdddddddmmdo.",
    ".omd.......dmdo.",
    ".omd.......dmdo.",
    ".odo.......odo..",
    "..o.........o...",
    "................",
    "................",
]
for tier, (h, m, d) in {"iron": ((245, 246, 250), (205, 207, 214), (140, 142, 150)),
                        "diamond": ((190, 255, 245), (74, 214, 204), (30, 140, 135)),
                        "netherite": ((125, 110, 118), (76, 66, 72), (40, 34, 38))}.items():
    pal = {'.': (0, 0, 0, 0), 'o': (30, 25, 30, 255), 'h': h + (255,), 'm': m + (255,), 'd': d + (255,), 'e': (238, 148, 165, 255)}
    save(from_grid(HELMET, pal), "item", f"{tier}_guh_armor.png")
