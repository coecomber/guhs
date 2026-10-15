"""
Het Snuffeleiland (kern): the hand-painted little things. The Guhstation (a grey-black console box with a guh snoet as its
logo: three 16 x 16 faces and a block model of two boxes), the memory card, the blossom twig of the tree, and the logo of
the Guhstation's window. The dogs, the companions and the tree are NOT here: those are the approved models of
snuffel_modellen.py.
"""
import math

import numpy as np
from PIL import Image

KAST = (42, 43, 51)
KAST_LICHT = (74, 76, 88)
KAST_DONKER = (23, 24, 29)
DEKSEL = (58, 60, 70)
ROZE = (240, 156, 196)
ROZE_DONKER = (196, 104, 152)
OOR_BINNEN = (255, 198, 222)
OOG = (40, 24, 48)


def _vlak(kleur, var, seed):
    rng = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.uint8)
    n = rng.normal(0, var, (16, 16))
    for c in range(3):
        a[..., c] = np.clip(kleur[c] + n, 0, 255)
    a[..., 3] = 255
    return a


def _snoet(a, cx, cy):
    """A guh snoet of 7 x 6 pixels with its middle at (cx, cy): two round ears, a chubby face, two eyes, a little nose."""
    def zet(x, y, k):
        if 0 <= x < a.shape[1] and 0 <= y < a.shape[0]:
            a[y, x, :3] = k
            a[y, x, 3] = 255
    for ox in (-3, 2):                      # the ears (2 x 2, a lighter inside)
        for dx in (0, 1):
            for dy in (-3, -2):
                zet(cx + ox + dx, cy + dy, ROZE)
        zet(cx + ox + (1 if ox < 0 else 0), cy - 2, OOR_BINNEN)
    for dy in range(-1, 3):                 # the face
        breed = 3 if dy in (0, 1) else 2
        for dx in range(-breed, breed + 1):
            zet(cx + dx, cy + dy, ROZE)
    zet(cx - 2, cy, OOG)
    zet(cx + 1, cy, OOG)
    zet(cx - 1, cy + 1, ROZE_DONKER)        # the nose (the snoet is one pixel off-centre: the face is 7 wide, x -3..3)
    zet(cx, cy + 1, ROZE_DONKER)
    zet(cx - 3, cy + 1, (255, 176, 206))    # blush
    zet(cx + 3, cy + 1, (255, 176, 206))


def guhstation_boven():
    a = _vlak(KAST, 3, 21305111)
    a[4:12, 3:13, :3] = DEKSEL              # the lid
    a[4, 3:13, :3] = KAST_LICHT
    a[11, 3:13, :3] = KAST_DONKER
    for y in range(4, 12):                  # the round window in the lid
        for x in range(3, 13):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 12.5:
                a[y, x, :3] = (26, 27, 33)
    _snoet(a, 8, 7)
    a[13, 11, :3] = (110, 220, 130)         # the little green light on the rim
    return Image.fromarray(a)


def guhstation_voor():
    a = _vlak(KAST, 3, 21305112)
    a[12, :, :3] = KAST_LICHT
    a[15, :, :3] = KAST_DONKER
    for x0 in (4, 8):                       # two controller ports
        a[13:15, x0:x0 + 3, :3] = KAST_DONKER
        a[13, x0:x0 + 3, :3] = (12, 12, 15)
    a[13, 12, :3] = (110, 220, 130)         # power
    a[14, 2:4, :3] = (150, 152, 166)        # the lid's button
    # (rows 0..11 are never shown; keep them calm for the particle)
    return Image.fromarray(a)


def guhstation_zij():
    a = _vlak(KAST, 3, 21305113)
    a[12, :, :3] = KAST_LICHT
    a[15, :, :3] = KAST_DONKER
    for x in range(4, 12, 2):               # cooling slits
        a[13:15, x, :3] = KAST_DONKER
    a[11, :, :3] = DEKSEL                   # the edge of the lid (row 11: the lid's side is one pixel high)
    return Image.fromarray(a)


def guhstation_model():
    """Two boxes: the console (12 x 4 x 10) and its lid (10 x 1 x 8). The front is north; the top faces are turned half a
    turn so that the snoet is the right way up for whoever stands in front of it."""
    def vlak(tex, uv):
        return {"texture": tex, "uv": uv}
    kast = {"from": [2, 0, 3], "to": [14, 4, 13], "faces": {
        "north": vlak("#voor", [2, 12, 14, 16]), "south": vlak("#zij", [2, 12, 14, 16]), "east": vlak("#zij", [3, 12, 13, 16]),
        "west": vlak("#zij", [3, 12, 13, 16]), "up": dict(vlak("#boven", [2, 3, 14, 13]), rotation=180), "down": vlak("#zij", [2, 0, 14, 10])}}
    deksel = {"from": [3, 4, 4], "to": [13, 5, 12], "faces": {
        "north": vlak("#zij", [3, 11, 13, 12]), "south": vlak("#zij", [3, 11, 13, 12]), "east": vlak("#zij", [4, 11, 12, 12]),
        "west": vlak("#zij", [4, 11, 12, 12]), "up": dict(vlak("#boven", [3, 4, 13, 12]), rotation=180)}}
    return {"parent": "minecraft:block/block",
            "textures": {"particle": "guhs:block/guhstation_zij", "boven": "guhs:block/guhstation_boven", "voor": "guhs:block/guhstation_voor",
                         "zij": "guhs:block/guhstation_zij"},
            "elements": [kast, deksel],
            "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 3.5, 0], "scale": [0.9, 0.9, 0.9]},
                        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]},
                        "fixed": {"rotation": [0, 0, 0], "translation": [0, 4, 0], "scale": [0.9, 0.9, 0.9]},
                        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 4, 1], "scale": [0.45, 0.45, 0.45]},
                        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 5, 0], "scale": [0.55, 0.55, 0.55]},
                        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 5, 0], "scale": [0.55, 0.55, 0.55]}}}


def logo():
    """The guh snoet of the Guhstation's window (16 x 16, on transparent)."""
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            oor = min((x - 3.5) ** 2 + (y - 4.0) ** 2, (x - 11.5) ** 2 + (y - 4.0) ** 2) <= 6.5
            binnen = min((x - 3.5) ** 2 + (y - 4.2) ** 2, (x - 11.5) ** 2 + (y - 4.2) ** 2) <= 2.0
            kop = ((x - 7.5) / 6.6) ** 2 + ((y - 9.2) / 5.0) ** 2 <= 1
            if kop:
                a[y, x] = ROZE + (255,)
            elif oor:
                a[y, x] = (OOR_BINNEN if binnen else ROZE) + (255,)
    for x, y in ((5, 8), (10, 8), (5, 9), (10, 9)):
        a[y, x] = OOG + (255,)
    a[8, 5] = (255, 255, 255, 255)
    a[8, 10] = (255, 255, 255, 255)
    for x, y in ((7, 10), (8, 10)):
        a[y, x] = ROZE_DONKER + (255,)
    for x, y in ((3, 11), (4, 11), (11, 11), (12, 11)):
        a[y, x] = (255, 176, 206, 255)
    return Image.fromarray(a)


GEHEUGENKAART = [
    "................",
    "...kkkkkkkkkk...",
    "..kgGGGGGGGGgk..",
    "..kgwwwwwwwwgk..",
    "..kgwpwwwwpwgk..",
    "..kgwwpwwpwwgk..",
    "..kgwwwwwwwwgk..",
    "..kgwwppppwwgk..",
    "..kgwwwppwwwgk..",
    "..kgwwwwwwwwgk..",
    "..kgGGGGGGGGgk..",
    "..kggggggggggk..",
    "..kgygygygyggk..",
    "..kgygygygyggk..",
    "...kkkkkkkkkk...",
    "................"]
BLOESEMTAKJE = [
    "................",
    "..........rr....",
    ".........rRyr...",
    "..........rrl...",
    ".......rr..bL...",
    "......rRyr.b....",
    ".......rrlbb....",
    ".....ll..bb.....",
    "....lLl.bB......",
    ".....l.bb..rr...",
    "......bb..rRyr..",
    ".....bB....rr...",
    "....bb.l........",
    "...bB.lLl.......",
    "..bb...l........",
    "................"]
PAL = {"k": (30, 30, 36, 255), "g": (74, 76, 88, 255), "G": (110, 112, 126, 255), "w": (236, 236, 240, 255), "p": ROZE + (255,),
       "y": (232, 190, 80, 255), "b": (120, 84, 52, 255), "B": (86, 58, 36, 255), "l": (104, 176, 88, 255), "L": (70, 140, 70, 255),
       "r": (246, 160, 196, 255), "R": (226, 110, 160, 255)}


def muziekplaat():
    """The island's music disc: black vinyl, a sea-green label with a sandy paw print."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 7.6:
                px[x, y] = (30, 26, 34, 255) if int(d) % 2 else (48, 42, 54, 255)
            if d <= 3.6:
                px[x, y] = (64, 176, 170, 255)
    zand, zand_donker = (246, 226, 160, 255), (214, 186, 116, 255)
    for x, y in ((6, 6), (8, 5), (9, 6)):                                # three toes
        px[x, y] = zand
    for x, y in ((7, 8), (8, 8), (7, 9), (8, 9)):                        # the pad
        px[x, y] = zand
    px[8, 9] = zand_donker
    px[7, 7] = zand
    px[3, 4] = px[4, 3] = (110, 100, 120, 255)                           # shine on the vinyl
    px[11, 12] = px[12, 11] = (120, 200, 196, 255)                       # a glint of the sea
    return img


def build(h):
    h.save(muziekplaat(), "item", "music_disc_snuffeleiland.png")
    h.item_model("music_disc_snuffeleiland")
    h.save(guhstation_boven(), "block", "guhstation_boven.png")
    h.save(guhstation_voor(), "block", "guhstation_voor.png")
    h.save(guhstation_zij(), "block", "guhstation_zij.png")
    h.w(f"{h.A}/models/block/guhstation.json", guhstation_model())
    h.w(f"{h.A}/blockstates/guhstation.json", {"variants": h.facing_states("guhstation")})
    h.w(f"{h.A}/models/item/guhstation.json", {"parent": "guhs:block/guhstation"})
    h.self_drop("guhstation")
    h.save(h.grid(GEHEUGENKAART, PAL), "item", "snuffel_geheugenkaart.png")
    h.item_model("snuffel_geheugenkaart")
    h.save(h.grid(BLOESEMTAKJE, PAL), "item", "snuffel_bloesemtakje.png")
    h.item_model("snuffel_bloesemtakje")
    h.save(logo(), "gui", "snuffel", "guhstation_logo.png")
