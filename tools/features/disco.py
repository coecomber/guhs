"""
The Guhdisco (2.4): a disco club in the Guhmension where the DJ-guh plays Simon says on a light-up dance floor
(nl.juiced.guhs.feature.disco). This module makes its blocks (the four disco tiles, the twinkling dance floor, the
disco ball, the guhshakes on the bar), the discomunt, the DJ-guh, the disco outfit (glitter suit, afro, star glasses),
the structure guh_disco (+ a small test floor for the GameTests), the advancements, the texts and the FTB quests.

2.9 ("De Grote Guhspelen"): the song is the level. The four songs (sounds/disco/*.ogg: the approved 70's remix of
"Ze hangen aan me vet" + Vadsige Tango, Mika-Mambo and Njeg-Njeg Boogie) are made by tools/remix/ (run separately, the
OGGs are committed; tools/remix/check_songs.py checks them); here only sounds.json, texts, advancements (tab
grote_guhspelen) and the DJ's new shop piece, the koptelefoontje (OREN slot).
"""
import math
import os
import random
from collections import deque

import numpy as np
from PIL import Image

from features import spelen as _spelen

KLEUREN = {  # tile colour: (rgb, en, nl) - keep in sync with DiscoTileBlock.Kleur
    "roze": ((255, 110, 199), "pink", "roze"),
    "blauw": ((79, 182, 255), "blue", "blauw"),
    "geel": ((255, 225, 77), "yellow", "geel"),
    "groen": ((92, 240, 122), "green", "groen"),
}
SMAKEN = {  # guhshake flavour: liquid colour
    "aardbei": (246, 120, 170), "munt": (140, 230, 190), "choco": (130, 84, 60), "kaas": (250, 206, 80)}
FLOOR = [(255, 90, 200), (190, 90, 255), (80, 150, 255), (70, 230, 240), (90, 240, 120), (255, 230, 70), (255, 150, 60), (255, 70, 120)]
CLOTHES = ["disco_glitterpak", "disco_afro", "disco_bril", "disco_koptelefoontje"]
# the four songs of the disco (sound event guhs:disco.<id>, file sounds/disco/<id>.ogg): keep in sync with DiscoLiedje.java
LIEDJES = ["vadsige_tango", "ze_hangen_disco70", "mika_mambo", "njeg_njeg_boogie"]

H = [0, 6, -2]  # the guh's head pivot (make_guh_variants.HEAD_PIVOT)
BONES = {
    # a big puffy afro with a golden pick in it (the ears poke through: guh style)
    "outfit_disco_afro": ("head", H, "disco_afro", [([-6.5, 14.5, -11.5], [13, 4.5, 10], 0), ([-5, 19, -10.5], [10, 2, 8], 0),
                                                   ([-9, 11, -10], [2.5, 5, 7.5], 0), ([6.5, 11, -10], [2.5, 5, 7.5], 0),
                                                   ([-7.5, 13, -11], [15, 2, 8], 0)]),
    "outfit_disco_afro_kam": ("head", H, "disco_afro_kam", [([3, 19.5, -8], [2.5, 3.5, 0.5], 0), ([3, 21, -8.1], [2.5, 0.8, 0.7], 0)]),
    # star-shaped disco glasses (a plus-star per eye and a little bridge; the eyes sit at x +-1.25..7, y 7.25..12.5)
    "outfit_disco_bril": ("head", H, "disco_bril", [([1.3, 8.2, -12.7], [5.6, 3.4, 0.4], 0), ([2.7, 6.6, -12.7], [2.8, 6.6, 0.4], 0),
                                                   ([-6.9, 8.2, -12.7], [5.6, 3.4, 0.4], 0), ([-5.5, 6.6, -12.7], [2.8, 6.6, 0.4], 0),
                                                   ([-1.3, 10, -12.7], [2.6, 0.6, 0.4], 0)]),
}
# 2.9: the koptelefoontje (OREN): on each guh ear a little headphone that clamps it (a cup in front, one behind, a
# band over the ear tip), with a golden music note on the front cup. The ear cubes: x 4.5..11.5, y 10..17, z -6.25..-4.75.
BONES.update(_spelen.oren("outfit_oren_koptelefoon", "oren_koptelefoon", cubes=[
    ([5.75, 11.25, -7.6], [4.5, 4.5, 1.35], 0),       # front cup
    ([6.25, 11.75, -8.0], [3.5, 3.5, 0.4], 0),        # its cushion rim
    ([5.75, 11.25, -4.75], [4.5, 4.5, 1.35], 0),      # back cup
    ([7.4, 15.75, -7.3], [1.2, 1.6, 0.8], 0),         # front arm
    ([7.4, 15.75, -4.5], [1.2, 1.6, 0.8], 0),         # back arm
    ([7.4, 17.2, -7.3], [1.2, 0.9, 3.6], 0),          # the band over the ear tip
]))
BONES.update(_spelen.oren("outfit_oren_koptelefoon_noot", "oren_koptelefoon_noot", cubes=[
    ([7.4, 12.4, -8.3], [0.8, 2.4, 0.35], 0),         # the stem of the note
    ([6.5, 12.1, -8.3], [1.4, 1.0, 0.35], 0),         # its head
    ([7.9, 14.3, -8.3], [1.1, 0.5, 0.35], 0),         # the flag
]))


# =====================================================================================================================
# textures
# =====================================================================================================================
def _img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def tile_texture(rgb, lit, seed):
    """A glass floor tile: a rim, a soft inner glow; lit it shines (white hot middle), unlit it's a dark glass."""
    rng = np.random.default_rng(seed)
    a = np.zeros((16, 16, 4), np.float32)
    c = np.array(rgb, np.float32)
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            d = math.hypot(x - 7.5, y - 7.5) / 10.6
            if edge == 0:
                col = c * (0.55 if lit else 0.25)
            elif edge == 1:
                col = c * (0.95 if lit else 0.42) + (40 if lit else 0)
            elif lit:
                col = c + (255 - c) * max(0.0, 0.85 - d * 1.3)
            else:
                col = c * (0.34 + 0.12 * (1 - d))
            a[y, x, :3] = col
    a[..., :3] += rng.normal(0, 4 if lit else 3, (16, 16, 1))
    for i in range(3, 7):                                      # a little glass shine
        a[i, 10 - i + 3, :3] = a[i, 10 - i + 3, :3] * 0.5 + (255 if lit else 120) * 0.5
    a[..., 3] = 255
    return _img(a)


def floor_texture(fase):
    """The dance floor: 8 frames of disco colours (each fase starts two colours further), every other frame dimmed."""
    frames = []
    for f in range(8):
        rgb = np.array(FLOOR[(f + 2 * fase) % 8], np.float32)
        bright = 1.0 if (f + fase) % 2 == 0 else 0.5
        a = np.zeros((16, 16, 4), np.float32)
        for y in range(16):
            for x in range(16):
                edge = min(x, y, 15 - x, 15 - y)
                d = math.hypot(x - 7.5, y - 7.5) / 10.6
                if edge == 0:
                    col = np.array((40, 20, 50), np.float32)
                elif bright < 1:
                    col = rgb * 0.3 + np.array((45, 20, 70)) * 0.7 * (0.8 + 0.3 * (1 - d))
                else:
                    col = rgb * (0.75 + 0.35 * (1 - d))
                    if d < 0.35:
                        col = col + (255 - col) * (0.35 - d) * 1.6
                a[y, x, :3] = col
        a[..., 3] = 255
        frames.append(a)
    return _img(np.concatenate(frames, 0))


def ball_texture():
    """The disco ball's mirror tiles: 4x4 facets of silver, and every frame a few of them flash in a disco colour."""
    rng = random.Random(77)
    frames = []
    base = [[rng.randint(150, 215) for _ in range(4)] for _ in range(4)]
    for f in range(8):
        a = np.zeros((16, 16, 4), np.float32)
        flashes = {(rng.randrange(4), rng.randrange(4)): FLOOR[rng.randrange(8)] for _ in range(3)}
        for fy in range(4):
            for fx in range(4):
                v = base[fy][fx]
                col = np.array(flashes.get((fx, fy), (v, v, v + 12)), np.float32)
                for y in range(fy * 4, fy * 4 + 4):
                    for x in range(fx * 4, fx * 4 + 4):
                        c = col.copy()
                        if x % 4 == 3 or y % 4 == 3:
                            c = c * 0.45                         # the gaps between the mirrors
                        elif x % 4 == 0 and y % 4 == 0:
                            c = c + (255 - c) * 0.6              # a shine on each mirror
                        a[y, x, :3] = c
        if f % 2 == 0:                                           # a white star glint
            gx, gy = rng.randrange(1, 15), rng.randrange(1, 15)
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                a[gy + dy, gx + dx, :3] = 255
        a[..., 3] = 255
        frames.append(a)
    return _img(np.concatenate(frames, 0))


def shake_texture(liquid):
    """One 16x16 sheet for a guhshake: glass cup with the shake (x0-5, y0-8), its bottom (y10-15), cream side (x6-12, y0-1),
    cream top (x6-12, y3-9), the cherry (14, 0) and the striped straw (x14, y2-7)."""
    a = np.zeros((16, 16, 4), np.float32)
    liq = np.array(liquid, np.float32)
    for y in range(9):
        for x in range(6):
            col = liq * (1.05 - y * 0.035)
            if x == 0 or x == 5:
                col = col * 0.7 + np.array((230, 240, 255)) * 0.3   # the glass edges
            if x == 1 and 1 <= y <= 6:
                col = col * 0.5 + 255 * 0.5                         # a shine
            a[y, x, :3] = col
    for y in range(10, 16):
        for x in range(6):
            a[y, x, :3] = liq * 0.8
    cream = np.array((255, 244, 246), np.float32)
    a[0:2, 6:13, :3] = cream * np.array((1, 0.97, 0.98))
    for y in range(3, 10):
        for x in range(6, 13):
            a[y, x, :3] = cream - (6 if (x + y) % 3 == 0 else 0)
    a[5:7, 8:10, :3] = (220, 30, 60)                               # a cherry on the cream (seen from above)
    a[0, 14, :3] = (220, 30, 60)
    for y in range(2, 8):
        a[y, 14, :3] = (240, 60, 130) if y % 2 else (255, 255, 255)
    a[..., 3] = 255
    return _img(a)


def coin_texture():
    a = np.zeros((16, 16, 4), np.float32)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 7.2:
                if d > 6.2:
                    col = (170, 110, 20)
                else:
                    shine = max(0.0, 1 - math.hypot(x - 4.5, y - 4.5) / 6)
                    col = tuple(np.array((240, 196, 60)) + (255 - np.array((240, 196, 60))) * shine * 0.6)
                a[y, x] = (*col, 255)
    face = np.array((255, 170, 205), np.float32)
    for y in range(16):
        for x in range(16):
            if math.hypot(x - 7.5, y - 8.8) <= 3.7 or math.hypot(x - 4.6, y - 4.6) <= 1.4 or math.hypot(x - 10.4, y - 4.6) <= 1.4:
                a[y, x, :3] = face
    for (x, y) in ((6, 8), (9, 8)):
        a[y, x, :3] = (30, 20, 30)
    for (x, y) in ((5, 10), (10, 10)):
        a[y, x, :3] = (250, 110, 160)
    a[10, 7:9, :3] = (200, 90, 130)
    a[2, 3, :3] = a[3, 2, :3] = a[3, 3, :3] = a[3, 4, :3] = a[4, 3, :3] = (255, 255, 255)
    return _img(a)


# =====================================================================================================================
# guh clothes (make_guh_variants / make_clothes_icons)
# =====================================================================================================================
def clothes(rng, v):
    def glitter():
        a = v.fabric((236, 110, 200), rng, 10)
        mask = rng.random((32, 32)) < 0.16
        colours = np.array([(255, 255, 255), (255, 220, 90), (150, 230, 255), (255, 170, 230)], np.float32)
        pick = rng.integers(0, 4, (32, 32))
        a[mask] = colours[pick[mask]]
        for y in range(0, 32, 8):                                   # a few rows of golden sequins
            a[y, ::3] = (255, 205, 60)
        return a

    def afro():
        a = v.fabric((214, 70, 170), rng, 16)
        for _ in range(60):                                        # curls
            x, y = rng.integers(1, 31, 2)
            a[y - 1:y + 1, x - 1:x + 1] = (245, 120, 210)
            a[y, x] = (170, 40, 130)
        return a

    def bril():
        a = np.zeros((32, 32, 3), np.float32)
        for y in range(32):
            for x in range(32):
                t = y / 31
                a[y, x] = np.array((255, 70, 180)) * (1 - t) + np.array((150, 70, 255)) * t
        a[:4, :] = a[-4:, :] = (250, 200, 50)
        a[:, :4] = a[:, -4:] = (250, 200, 50)
        for i in range(8, 20):
            a[i, 28 - i:31 - i] = (255, 240, 250)                  # a shine
        return np.clip(a + rng.normal(0, 4, a.shape), 0, 255)

    def koptelefoon():
        a = v.fabric((244, 120, 196), rng, 8)                     # glossy pink cups
        for y in range(32):
            a[y, :] = a[y, :] * (1.12 - 0.3 * y / 31)              # a shine from above
        a[::8, :] = (170, 80, 230)                                 # purple seams (the band shows them as stripes)
        a[1::8, :] = (200, 120, 245)
        return np.clip(a, 0, 255)

    def noot():
        a = v.fabric((250, 205, 60), rng, 6)
        a[::5, ::5] = (255, 245, 200)                              # a golden glint
        return a

    return {
        "disco_glitterpak": {"suit": glitter},
        "disco_afro": {"disco_afro": afro, "disco_afro_kam": lambda: v.fabric((250, 200, 60), rng, 8)},
        "disco_bril": {"disco_bril": bril},
        "disco_koptelefoontje": {"oren_koptelefoon": koptelefoon, "oren_koptelefoon_noot": noot},
    }


def icons(ic):
    afro = ["....aaaaaaa.....", "..aabbcbbbbaa...", ".abbbbbbcbbbba.k", ".abcbbbbbbbcbakk", "abbbbbcbbbbbbak.",
            "abbcbbbbbbcbba..", "abbbbbbbbbbbba..", "abcbbbbcbbbbba..", ".abbb.....bba...", ".abb.......ba...", "..aa.......aa..."]
    star = ["...a...", "..aba..", "aabcbaa", ".abbba.", ".ab.ba.", "aa...aa"]
    bril = [star[i] + ("aa" if i == 2 else "..") + star[i] for i in range(len(star))]
    koptelefoon = ["....aaaaaaaa....", "...a.cccccc.a...", "..a..........a..", "..a..........a..", "..a..........a..",
                   ".bbbb......bbbb.", "bccnnb....bccnnb", "bccncb....bccncb", "bcnncb....bcnncb", "bcnncb....bcnncb",
                   "bccccb....bccccb", ".bbbb......bbbb."]
    return {
        "disco_koptelefoontje": ic.icon(ic.pad(koptelefoon), {"a": (170, 80, 230), "c": (244, 120, 196), "b": (150, 40, 120),
                                                             "n": (250, 205, 60)}),
        "disco_glitterpak": ic.shirt((236, 110, 200), (150, 40, 120), (255, 225, 110), "stars"),
        "disco_afro": ic.icon(ic.pad(afro), {"a": (140, 30, 110), "b": (214, 70, 170), "c": (250, 140, 220), "k": (250, 200, 60)}),
        "disco_bril": ic.icon(ic.pad(bril), {"a": (220, 170, 40), "b": (255, 80, 180), "c": (255, 245, 250)}),
    }


# =====================================================================================================================
# the structure
# =====================================================================================================================
PASSABLE = ("minecraft:air", "carpet", "vlaggetjes", "potted", "guhbloem", "knabbelroos", "kaasbloem", "guhoortjes", "lampion", "disco_milkshake")
SOLID_LIGHT = {"froglight": 15, "guh_kristal_lamp": 15, "lampion": 15, "disco_bal": 12, "disco_dansvloer": 10,
               "guh_kristal_blok": 10, "disco_tegel": 5, "sea_lantern": 15, "glowstone": 15, "disco_milkshake": 4}


def passable(name):
    return name is None or any(p in name for p in PASSABLE)


def light_of(name):
    if name is None:
        return 0
    return max([v for k, v in SOLID_LIGHT.items() if k in name] or [0])


FONT = {
    "V": ["X.X", "X.X", "X.X", "X.X", ".X."], "A": [".X.", "X.X", "XXX", "X.X", "X.X"], "H": ["X.X", "X.X", "XXX", "X.X", "X.X"],
    "O": ["XXX", "X.X", "X.X", "X.X", "XXX"], "E": ["XXX", "X..", "XX.", "X..", "XXX"], "G": ["XXX", "X..", "X.X", "X.X", "XXX"],
    "!": ["X", "X", "X", ".", "X"], "S": ["XXX", "X..", "XXX", "..X", "XXX"], "K": ["X.X", "X.X", "XX.", "X.X", "X.X"],
    "D": ["XX.", "X.X", "X.X", "X.X", "XX."], "I": ["X", "X", "X", "X", "X"], "C": ["XXX", "X..", "X..", "X..", "XXX"],
    "U": ["X.X", "X.X", "X.X", "X.X", "XXX"],
}
NEON = ["pearlescent_froglight", "ochre_froglight", "verdant_froglight"]


def neon_text(s, h, text, x_at, z0, dz, y_top):
    """Froglight letters (3x5 font) on a wall: along z from z0 in steps of dz, at wall depth x = x_at."""
    z = z0
    for i, ch in enumerate(text):
        rows = FONT[ch]
        for r, row in enumerate(rows):
            for c, px in enumerate(row):
                if px == "X":
                    s.set(x_at, y_top - r, z + dz * c, h.mc(NEON[i % 3]), {"axis": "y"})
        z += dz * (len(rows[0]) + 1)


def disco_structure(h):
    """The Guhdisco's club: 72x64 (in 96x84 grounds, see disco_grounds). A pink club with a guh face as its front (the door is its mouth, with buck teeth), inside
    the dance floor shaped like a guh head (the four Simon-says tiles are its eyes and cheeks, a pink crystal nose in
    the middle), a mirror disco ball, the DJ booth: a giant guh head on the stage with the DJ-guh in its mouth and
    speakers on both sides, a bar with guhshakes, a lounge with vadszakken, neon letters, and a plaza with a guh mosaic."""
    mc, Structure = h.mc, h.Structure
    W, HH, D = 72, 24, 64
    s = Structure((W, HH, D))
    rng = random.Random(4404)
    X0, X1, Z0, Z1 = 6, 65, 4, 46          # outer walls
    CEIL, ROOF = 14, 15
    cx, cz = 36, 28                        # middle of the dance floor (the nose)
    fp = [(x, z) for x in range(W) for z in range(D)]

    # --- the ground: dirt and grass (the hedge round the edge comes with the grounds, see disco_grounds) ---
    for x, z in fp:
        s.set(x, 0, z, mc("dirt"))
        s.set(x, 1, z, mc("grass_block"), {"snowy": "false"})

    # --- the shell: walls with a golden trim and a neon band, the ceiling (stars) and the roof ---
    for y in range(1, ROOF + 1):
        for i in range(X0, X1 + 1):
            for (x, z) in ((i, Z0), (i, Z1)):
                s.set(x, y, z, wall_block(mc, x - X0, y))
        for i in range(Z0, Z1 + 1):
            for (x, z) in ((X0, i), (X1, i)):
                s.set(x, y, z, wall_block(mc, i - Z0, y))
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            s.set(x, ROOF, z, mc("white_concrete"))
            inside = X0 < x < X1 and Z0 < z < Z1
            if inside:
                star = (x * 7 + z * 3) % 13 == 0
                s.set(x, CEIL, z, "guhs:guh_kristal_lamp" if star else mc("black_concrete"))
                s.set(x, 1, z, mc("white_concrete") if (x + z) % 2 else mc("pink_concrete"))
                for y in range(2, CEIL):
                    s.set(x, y, z, mc("air"))
            edge = x in (X0, X1) or z in (Z0, Z1)
            if edge:
                s.set(x, ROOF + 1, z, mc("pink_concrete") if (x + z) % 2 else mc("yellow_concrete"))

    # --- the dance floor: a guh head of twinkling tiles, ears towards the stage ---
    for x in range(cx - 14, cx + 15):
        for z in range(cz - 14, cz + 14):
            head = ((x - cx) / 12.0) ** 2 + ((z - cz - 1) / 10.5) ** 2
            ear = min(math.hypot(x - (cx - 8), z - (cz - 10)), math.hypot(x - (cx + 8), z - (cz - 10)))
            if head <= 1 or ear <= 3.2:
                s.set(x, 1, z, "guhs:disco_dansvloer", {"fase": str((x & 1) + 2 * (z & 1))})
            elif head <= 1.2 or ear <= 4.1:
                s.set(x, 1, z, mc("smooth_quartz"))
    for (x, z) in ((cx - 3, 35), (cx - 2, 36), (cx - 1, 36), (cx, 35), (cx + 1, 36), (cx + 2, 36), (cx + 3, 35)):   # the mouth
        s.set(x, 1, z, mc("black_concrete"))
    for side in (-1, 1):                                                                                      # whiskers
        for k in range(4):
            s.set(cx + side * (7 + k), 1, 31 + (k // 2), mc("white_concrete"))
            s.set(cx + side * (7 + k), 1, 34 + (k // 3), mc("white_concrete"))
    # the Simon-says tiles: four 4x4 fields round a golden cross, the crystal nose in the middle
    quads = {("w", "n"): "roze", ("e", "n"): "blauw", ("w", "s"): "geel", ("e", "s"): "groen"}
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            if x == cx and z == cz:
                s.set(x, 1, z, "guhs:guh_kristal_blok")
            elif x == cx or z == cz:
                s.set(x, 1, z, mc("gold_block"))
            else:
                k = quads[("w" if x < cx else "e", "n" if z < cz else "s")]
                s.set(x, 1, z, f"guhs:disco_tegel_{k}", {"lit": "false"})

    # --- the disco ball, on a chain from the ceiling ---
    for x in range(cx - 3, cx + 4):
        for y in range(7, 14):
            for z in range(cz - 3, cz + 4):
                if math.dist((x, y, z), (cx, 10, cz)) <= 2.4:
                    s.set(x, y, z, "guhs:disco_bal")
    s.set(cx, 13, cz, mc("chain"), {"axis": "y", "waterlogged": "false"})

    # --- the stage with the DJ booth: a giant guh head, the DJ-guh sits in its mouth ---
    for x in range(20, 53):
        for z in range(5, 14):
            s.set(x, 2, z, mc("yellow_concrete") if z == 13 else mc("smooth_quartz"))
    for x in range(33, 40):
        s.set(x, 2, 14, mc("quartz_stairs"), {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    bx, by = 36, 7.5
    for x in range(28, 45):
        for y in range(3, 14):
            if ((x - bx) / 7.6) ** 2 + ((y - by) / 5.1) ** 2 <= 1:
                for z in range(7, 11):
                    s.set(x, y, z, mc("pink_concrete"))
    for x, y in ((29, 12), (30, 12), (31, 12), (29, 13), (30, 13), (42, 12), (43, 12), (41, 12), (43, 13), (42, 13)):   # ears
        for z in (8, 9):
            s.set(x, y, z, mc("pink_concrete"))
    s.set(30, 12, 10, mc("magenta_concrete"))
    s.set(42, 12, 10, mc("magenta_concrete"))
    for x in (32, 33, 39, 40):                                   # eyes, with a white shine
        for y in (8, 9, 10):
            s.set(x, y, 10, mc("black_concrete"))
    s.set(32, 10, 10, mc("white_concrete"))
    s.set(39, 10, 10, mc("white_concrete"))
    for x in (30, 31, 41, 42):                                   # cheeks
        s.set(x, 6, 10, mc("magenta_concrete"))
    s.set(36, 7, 10, mc("pink_terracotta"))                      # nose
    s.set(36, 6, 10, mc("black_concrete"))
    for x in range(34, 39):                                      # the mouth: the DJ's seat
        for y in range(3, 6):
            for z in range(8, 11):
                s.set(x, y, z, mc("air"))
    s.set(35, 5, 10, mc("white_concrete"))                       # buck teeth
    s.set(37, 5, 10, mc("white_concrete"))
    s.set(34, 3, 9, mc("jukebox"), {"has_record": "false"})      # the turntables
    s.set(38, 3, 9, mc("jukebox"), {"has_record": "false"})
    s.set(34, 4, 9, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    s.set(38, 4, 9, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})
    s.entity(36.5, 3.0, 9.4, {"id": "guhs:guh_npc", "Kind": "djguh", "PersistenceRequired": h.Byte(1), "Rotation": h.floats(0.0, 0.0)})
    for sx in (22, 46):                                          # speakers
        for x in range(sx, sx + 5):
            for y in range(3, 12):
                for z in range(6, 10):
                    s.set(x, y, z, mc("black_concrete"))
        for wy in (5, 9):
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    s.set(sx + 2 + dx, wy + dy, 9, mc("polished_blackstone") if dx == dy == 0 else mc("light_gray_concrete"))
        s.set(sx + 2, 11, 9, mc("white_concrete"))
        s.set(sx + 2, 12, 8, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
        s.set(sx, 12, 7, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
        s.set(sx + 4, 12, 7, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})
    for x in range(20, 53, 3):                                   # footlights along the stage
        if not 33 <= x <= 39:
            s.set(x, 3, 13, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})

    # --- neon letters on the side walls ---
    neon_text(s, h, "VAHOEG!", X1 - 1, 16, 1, 10)             # east wall (read facing east: left to right = +z)
    neon_text(s, h, "SHAKES", X0 + 1, 37, -1, 10)             # west wall, above the bar

    # --- the bar (west): a counter with guhshakes, stools, the back bar with cupboards and framed shakes ---
    smaken = list(SMAKEN)
    for z in range(17, 36):
        s.set(13, 2, z, mc("quartz_block") if z % 4 else mc("pink_terracotta"))
        if z % 2:
            s.set(13, 3, z, "guhs:disco_milkshake", {"smaak": smaken[(z // 2) % 4]})
        if z % 3 == 0:
            s.set(14, 2, z, "guhs:guh_stoel", {"facing": "west"})
    for x in range(8, 13):
        s.set(x, 2, 17, mc("quartz_block"))
        if x < 12:                                                # (a gap to get behind the bar)
            s.set(x, 2, 35, mc("quartz_block"))
    for z in range(18, 35):
        if z % 4:
            s.set(7, 2, z, "guhs:guh_kast", {"facing": "east", "open": "false"})
        else:
            s.set(7, 2, z, mc("quartz_block"))
        if z % 2 == 0:
            s.set(7, 3, z, "guhs:disco_milkshake", {"smaak": smaken[z % 4]})
    s.set(10, 2, 26, mc("cauldron"))
    for z in (20, 26, 32):                                        # framed shakes on the back wall
        s.entity(7.03125, 5.5, z + 0.5, {"id": "minecraft:item_frame", "Facing": h.Byte(5), "Fixed": h.Byte(1), "Invulnerable": h.Byte(1),
                                          "TileX": 7, "TileY": 5, "TileZ": z,
                                          "Item": {"id": "guhs:kaasknabbel_milkshake", "count": 1}})
    for z in range(18, 35, 4):                                    # lampgions above the bar
        s.set(11, 13, z, "guhs:lampion_roze" if z % 8 else "guhs:lampion_geel", {"hanging": "true", "waterlogged": "false"})

    # --- the lounge (east): vadszakken round little tables, cushions ---
    zitzak = ["pink", "magenta", "white", "purple", "light_blue", "yellow"]
    for i, z in enumerate(range(18, 40, 5)):
        s.set(60, 2, z, "guhs:guh_tafel", {"facing": "west"})
        s.set(58, 2, z, f"guhs:{zitzak[i % 6]}_zitzak", {"facing": "east"})
        s.set(62, 2, z, f"guhs:{zitzak[(i + 2) % 6]}_zitzak", {"facing": "west"})
        s.set(60, 2, z + 2, f"guhs:{zitzak[(i + 4) % 6]}_kussen", {"facing": "north"})
        for x in range(57, 64):
            for dz in (-1, 0, 1):
                if s.get(x, 2, z + dz) in (None, "minecraft:air"):
                    s.set(x, 2, z + dz, mc("pink_carpet") if (x + dz) % 2 else mc("magenta_carpet"))
    for z in range(18, 40, 5):
        s.set(60, 13, z, "guhs:lampion_mint", {"hanging": "true", "waterlogged": "false"})

    # --- the corners: tables with stools, flower pots, guh portraits by the stage, bunting over the dance floor ---
    for tx in (12, 19, 52, 59):
        s.set(tx, 2, 41, "guhs:guh_tafel", {"facing": "north"})
        s.set(tx - 1, 2, 41, "guhs:guh_stoel", {"facing": "east"})
        s.set(tx + 1, 2, 41, "guhs:guh_stoel", {"facing": "west"})
        s.set(tx, 13, 41, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})
    for i, (x, z) in enumerate(((8, 44), (63, 44), (8, 6), (19, 6), (53, 6), (63, 6), (24, 44), (48, 44))):
        s.set(x, 2, z, ("guhs:potted_roze_guhbloem", "guhs:potted_knabbelroos", "guhs:potted_kaasbloem", "guhs:potted_guhoortjes")[i % 4])
    h.ms.guh_portrait(s, 11, 5, Z0 + 1)
    h.ms.guh_portrait(s, 54, 5, Z0 + 1)
    for z in (20, 37):
        for x in range(X0 + 1, X1):
            if s.get(x, 12, z) == "minecraft:air":
                s.set(x, 12, z, "guhs:vlaggetjes", {"axis": "x"})
    for x in (24, 48):
        for z in range(Z0 + 1, Z1):
            if s.get(x, 12, z) == "minecraft:air":
                s.set(x, 12, z, "guhs:vlaggetjes", {"axis": "z"})

    # --- lampgions over the rest of the floor, so no dark corners ---
    for x in range(10, 64, 7):
        for z in range(8, 45, 7):
            if s.get(x, 13, z) == "minecraft:air" and s.get(x, CEIL, z) == "minecraft:black_concrete":
                s.set(x, 13, z, "guhs:lampion_roze" if (x + z) % 3 else "guhs:lampion_geel", {"hanging": "true", "waterlogged": "false"})

    # --- the front: a big guh face, the door is its mouth (with buck teeth), ears above the roof ---
    fx, fy = 36, 9
    for x in range(fx - 9, fx + 10):
        for y in range(1, 19):
            d = math.hypot(x - fx, y - fy)
            if d <= 8.6:
                s.set(x, y, Z1 + 1, mc("magenta_concrete") if d > 7.8 else mc("pink_concrete"))
                if y > ROOF:
                    s.set(x, y, Z1, mc("pink_concrete"))
    for ex in (fx - 7, fx + 7):                                  # ears
        for x in range(ex - 3, ex + 4):
            for y in range(14, 22):
                d = math.hypot(x - ex, y - 17.5)
                if d <= 3.1 and y > ROOF - 1:
                    s.set(x, y, Z1 + 1, mc("magenta_concrete") if d <= 1.7 else mc("pink_concrete"))
                    s.set(x, y, Z1, mc("pink_concrete"))
    for x in (fx - 4, fx - 3, fx + 3, fx + 4):                   # eyes
        for y in (11, 12, 13):
            s.set(x, y, Z1 + 2, mc("black_concrete"))
    s.set(fx - 4, 13, Z1 + 2, mc("white_concrete"))
    s.set(fx + 3, 13, Z1 + 2, mc("white_concrete"))
    for x in (fx - 7, fx - 6, fx + 6, fx + 7):                   # cheeks
        s.set(x, 8, Z1 + 2, mc("magenta_concrete"))
    s.set(fx, 9, Z1 + 2, mc("pink_terracotta"))                  # nose
    s.set(fx, 8, Z1 + 1, mc("black_concrete"))                   # mouth
    s.set(fx - 1, 7, Z1 + 1, mc("black_concrete"))
    s.set(fx + 1, 7, Z1 + 1, mc("black_concrete"))
    s.set(fx - 2, 6, Z1 + 1, mc("black_concrete"))
    s.set(fx + 2, 6, Z1 + 1, mc("black_concrete"))
    for x in range(fx - 1, fx + 2):                              # the door
        for y in range(2, 6):
            for z in (Z1, Z1 + 1):
                s.set(x, y, z, mc("air"))
    s.set(fx - 1, 5, Z1 + 1, mc("white_concrete"))               # buck teeth
    s.set(fx + 1, 5, Z1 + 1, mc("white_concrete"))
    for z in (Z1, Z1 + 1):
        for x in range(fx - 1, fx + 2):
            s.set(x, 1, z, mc("gold_block") if z == Z1 else mc("pink_concrete"))
    s.set(fx - 2, 4, Z1 + 2, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    s.set(fx + 2, 4, Z1 + 2, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    s.set(fx - 2, 3, Z1 + 2, mc("quartz_block"))
    s.set(fx + 2, 3, Z1 + 2, mc("quartz_block"))
    for z in range(Z1 - 5, Z1):                                   # a red carpet in
        for x in range(fx - 1, fx + 2):
            s.set(x, 2, z, mc("red_carpet") if x == fx else mc("pink_carpet"))

    # --- the plaza: a pink path with a guh mosaic, lampgion posts, benches ---
    mx, mz = 36, 56
    for x in range(fx - 3, fx + 4):
        for z in range(Z1 + 2, D - 1):
            s.set(x, 1, z, mc("white_concrete") if x in (fx - 3, fx + 3) else mc("pink_concrete_powder"))
    for x in range(mx - 8, mx + 9):
        for z in range(mz - 8, mz + 8):
            d = math.hypot(x - mx, z - mz)
            ear = min(math.hypot(x - (mx - 5), z - (mz - 5.5)), math.hypot(x - (mx + 5), z - (mz - 5.5)))
            if d <= 6.5 or ear <= 2.2:
                s.set(x, 1, z, mc("pink_terracotta") if (d > 5.7 and ear > 2.2) or (ear <= 2.2 and d > 6.5 and ear > 1.3) else mc("pink_concrete"))
            elif d <= 7.4:
                s.set(x, 1, z, mc("white_concrete"))
    for (x, z) in ((mx - 3, mz - 2), (mx - 2, mz - 2), (mx + 2, mz - 2), (mx + 3, mz - 2), (mx - 3, mz - 1), (mx - 2, mz - 1),
                   (mx + 2, mz - 1), (mx + 3, mz - 1)):
        s.set(x, 1, z, mc("black_concrete"))
    for (x, z) in ((mx - 4, mz + 1), (mx - 5, mz + 1), (mx + 4, mz + 1), (mx + 5, mz + 1)):
        s.set(x, 1, z, mc("magenta_concrete"))
    s.set(mx, 1, mz, mc("magenta_terracotta"))
    for (x, z) in ((mx - 1, mz + 2), (mx + 1, mz + 2), (mx, mz + 1)):
        s.set(x, 1, z, mc("black_concrete"))
    for (x, z) in ((fx - 5, 50), (fx + 5, 50), (fx - 5, 61), (fx + 5, 61), (fx - 12, 53), (fx + 12, 53)):
        s.fill(x, 2, z, x, 3, z, mc("cherry_fence"))
        s.set(x, 4, z, "guhs:lampion_roze" if z < 55 else "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    for (x, z, f) in ((fx - 9, 58, "east"), (fx + 9, 58, "west"), (fx - 9, 54, "east"), (fx + 9, 54, "west")):
        s.set(x, 2, z, "guhs:guh_bank", {"facing": f})
    # flower beds along the front wall
    for x in list(range(X0, fx - 9)) + list(range(fx + 10, X1 + 1)):
        s.set(x, 1, Z1 + 1, mc("moss_block"))
        s.set(x, 2, Z1 + 1, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]))

    disco_exterior(h, s, rng, X0, X1, Z0, Z1, ROOF, fx)
    s.clear_above(fp, 2)
    problems = check_disco(s, cx, cz, X0, X1, Z0, Z1, CEIL)
    for p in problems[:20]:
        print("guh_disco:", p)
    assert not problems, f"guh_disco has {len(problems)} problems"
    disco_grounds(h, s).save("guh_disco")


GUH_FACE = ["M.....M", "MPPPPPM", "PkbPkbP", "PkwPkwP", "PPPmPPP", "PPmPmPP", ".PPPPP."]
GUH_FACE_BLOCKS = {"M": "magenta_concrete", "P": "pink_concrete", "k": "black_concrete", "b": "light_blue_concrete",
                   "w": "white_concrete", "m": "magenta_concrete"}
BALL_ART = ["..SWS..", ".WSWSW.", "SWSWSWS", "WSWFWSW", "SWSWSWS", ".WSWSW.", "..SWS.."]
BALL_BLOCKS = {"S": "light_gray_concrete", "W": "white_concrete", "F": "pearlescent_froglight"}
GUH_FLOWERS = ["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]


def disco_exterior(h, s, rng, X0, X1, Z0, Z1, ROOF, fx):
    """The outside of the club, so it's a guh building from every side (only outside the walls and on top of the roof;
    the inside is untouched): pilasters with lampgions on top, guh-face and mirror-ball medallions in the bays between
    them, flower beds and a white-and-pink path round the walls, four corner turrets with guh ears and mirror balls, a
    sunburst roof with a giant mirror-ball guh head (ears, eyes, cheeks) in the middle, a GUHDISCO marquee on the roof,
    lamp posts and guh topiaries round the back and two guh statues by the plaza."""
    mc = h.mc
    lamp = lambda kind: (f"guhs:lampion_{kind}", {"hanging": "false", "waterlogged": "false"})

    def in_shell(x, z):
        return X0 <= x <= X1 and Z0 <= z <= Z1

    # the four walls as (u range, u -> (x, z) at distance d outside, u of the wall's own pillars)
    sides = {
        "north": (range(X0, X1 + 1), lambda u, d: (u, Z0 - d), X0),
        "west": (range(Z0, Z1 + 1), lambda u, d: (X0 - d, u), Z0),
        "east": (range(Z0, Z1 + 1), lambda u, d: (X1 + d, u), Z0),
        "south": (range(X0, X1 + 1), lambda u, d: (u, Z1 + d), X0),
    }
    face_r = 9.5                                                   # the front's guh face (and its door) stays free
    for name, (us, at, u0) in sides.items():
        pillars = [u for u in us if (u - u0) % 8 == 0 and us[0] + 2 <= u <= us[-1] - 2]
        blocked = (lambda u: abs(u - fx) <= face_r) if name == "south" else (lambda u: False)
        # the path (distance 2) and the flower beds (distance 1) along the wall
        if name != "south":
            for u in range(us[0] + 1, us[-1]):
                x, z = at(u, 2)
                s.set(x, 1, z, mc("white_concrete") if u % 2 else mc("pink_concrete"))
                x, z = at(u, 1)
                s.set(x, 1, z, mc("moss_block"))
                s.set(x, 2, z, rng.choice(GUH_FLOWERS))
        # pilasters: quartz columns with a chiseled capital and a lampgion on top
        for u in pillars:
            if blocked(u):
                continue
            x, z = at(u, 1)
            s.set(x, 1, z, mc("quartz_block"))
            s.fill(x, 2, z, x, ROOF - 1, z, mc("quartz_pillar"), {"axis": "y"})
            s.set(x, ROOF, z, mc("chiseled_quartz_block"))
            s.set(x, ROOF + 1, z, *lamp("roze" if (u // 8) % 2 else "geel"))
        # the bays between the pillars: a guh face, then a mirror ball, then a guh face...
        bays = [p + 1 for p, q in zip(pillars, pillars[1:]) if q - p == 8]
        for k, b in enumerate(bays):
            if any(blocked(u) for u in range(b, b + 7)):
                continue
            art, blocks = (GUH_FACE, GUH_FACE_BLOCKS) if k % 2 == 0 else (BALL_ART, BALL_BLOCKS)
            for row, line in enumerate(art):
                for col, ch in enumerate(line):
                    if ch in blocks:
                        x, z = at(b + col, 1)
                        s.set(x, 10 - row, z, mc(blocks[ch]), {"axis": "y"} if "froglight" in blocks[ch] else None)
            for col in range(7):                                    # a golden cornice over each medallion
                x, z = at(b + col, 1)
                s.set(x, 11, z, mc("yellow_concrete") if col % 2 else mc("gold_block"))

    # --- the corner turrets: striped towers with a mirror ball and two guh ears on top ---
    for (cx_, cz_) in ((X0 - 1, Z0 - 1), (X1 + 1, Z0 - 1), (X0 - 1, Z1 + 1), (X1 + 1, Z1 + 1)):
        for x in range(cx_ - 1, cx_ + 2):
            for z in range(cz_ - 1, cz_ + 2):
                if in_shell(x, z):
                    continue
                s.set(x, 1, z, mc("quartz_bricks"))
                corner = abs(x - cx_) == 1 and abs(z - cz_) == 1
                for y in range(2, ROOF + 3):
                    band = mc("pink_concrete") if (y // 2) % 2 else mc("white_concrete")
                    s.set(x, y, z, mc("quartz_pillar") if corner else band, {"axis": "y"} if corner else None)
                s.set(x, ROOF + 3, z, mc("gold_block") if (x + z) % 2 else mc("yellow_concrete"))
        s.set(cx_, ROOF + 3, cz_, mc("pearlescent_froglight"), {"axis": "y"})
        s.set(cx_, ROOF + 4, cz_, "guhs:disco_bal")
        ears = ((cx_ - 1, cz_ - 1), (cx_ + 1, cz_ + 1)) if (cx_ < X0) != (cz_ < Z0) else ((cx_ - 1, cz_ + 1), (cx_ + 1, cz_ - 1))
        for (ex, ez) in ears:
            s.set(ex, ROOF + 4, ez, mc("pink_concrete"))
            s.set(ex, ROOF + 5, ez, mc("magenta_concrete"))

    # --- the roof: a pink-and-white sunburst round a giant mirror-ball guh head ---
    hx, hz, hy = 36, 25, ROOF + 0.5
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            a = math.atan2(z - hz, x - hx)
            ray = int((a + math.pi) / (2 * math.pi) * 24) % 2
            d = math.hypot(x - hx, z - hz)
            b = "pink_concrete" if ray else "white_concrete"
            if 11.5 < d < 12.6 or ((x - X0) % 10 == 5 and (z - Z0) % 10 == 5 and d > 13):
                b = NEON[(x + z) % 3]
            elif d <= 11.5:
                b = "magenta_concrete"
            s.set(x, ROOF + 1, z, mc(b), {"axis": "y"} if "froglight" in b else None)
    rx, ry = 9.5, 6.2
    for x in range(hx - 10, hx + 11):
        for z in range(hz - 10, hz + 11):
            for y in range(ROOF + 1, ROOF + 8):
                if ((x - hx) / rx) ** 2 + ((y - hy) / ry) ** 2 + ((z - hz) / rx) ** 2 <= 1:
                    s.set(x, y, z, "guhs:disco_bal")
    top = s.size[1] - 1
    for side in (-1, 1):                                             # the ears
        ex, ey = hx + side * 5.5, ROOF + 5.4
        for x in range(int(ex) - 4, int(ex) + 5):
            for y in range(ROOF + 2, top + 1):
                dd = math.hypot(x - ex, y - ey)
                if dd <= 3.0:
                    for z in (hz - 2, hz - 1):
                        s.set(x, y, z, mc("pink_concrete"))
                    if dd <= 1.8:
                        s.set(x, y, hz - 1, mc("magenta_concrete"))

    def front(x, y):                                                  # the head's surface block facing the front
        zs = [z for z in range(hz, hz + 12) if s.get(x, y, z) == "guhs:disco_bal"]
        return max(zs) if zs else None
    face = ([(x, y, "black_concrete") for x in (hx - 4, hx - 3, hx + 3, hx + 4) for y in (ROOF + 4, ROOF + 5)] +
            [(hx - 4, ROOF + 5, "white_concrete"), (hx + 3, ROOF + 5, "white_concrete"),
             (hx - 6, ROOF + 3, "magenta_concrete"), (hx + 6, ROOF + 3, "magenta_concrete"),
             (hx, ROOF + 3, "pink_terracotta"), (hx - 1, ROOF + 2, "black_concrete"), (hx + 1, ROOF + 2, "black_concrete")])
    for (x, y, b) in face:
        z = front(x, y)
        if z is not None:
            s.set(x, y, z, mc(b))

    # --- the GUHDISCO marquee at the back of the roof, readable from the front and from the back ---
    text = "GUHDISCO"
    width = sum(len(FONT[c][0]) for c in text) + len(text) - 1
    bz, bx0 = Z0 + 4, hx - width // 2 - 1
    bx1 = bx0 + width + 1
    for x in range(bx0, bx1 + 1):
        for y in range(ROOF + 2, top + 1):
            edge = x in (bx0, bx1) or y in (ROOF + 2, top)
            if edge:
                lit = (x + y) % 2 == 0
                s.set(x, y, bz, mc("ochre_froglight") if lit else mc("yellow_concrete"), {"axis": "y"} if lit else None)
            else:
                s.set(x, y, bz, mc("magenta_concrete"))
    for x in (bx0 + 2, bx1 - 2, (bx0 + bx1) // 2):                    # the feet
        s.set(x, ROOF + 2, bz + 1, mc("quartz_pillar"), {"axis": "y"})
        s.set(x, ROOF + 2, bz - 1, mc("quartz_pillar"), {"axis": "y"})
    x = bx0 + 1
    for i, ch in enumerate(text):
        for r, row in enumerate(FONT[ch]):
            for c, px in enumerate(row):
                if px == "X":
                    s.set(x + c, top - 1 - r, bz + 1, mc(NEON[i % 3]), {"axis": "y"})               # from the front: +x
                    s.set(bx0 + bx1 - (x + c), top - 1 - r, bz - 1, mc(NEON[i % 3]), {"axis": "y"})  # from the back
        x += len(FONT[ch][0]) + 1

    # --- round the back: lamp posts and guh topiaries (blossom balls with ears and eyes) ---
    blossom = {"persistent": "true", "distance": "1", "waterlogged": "false"}
    for i, x in enumerate(range(X0 + 4, X1 - 2, 8)):
        if i % 2:
            s.fill(x, 2, 0, x, 4, 0, mc("cherry_fence"))
            s.set(x, 5, 0, *lamp("roze"))
        else:
            s.set(x, 2, 1, "guhs:guhbloesem_log", {"axis": "y"})
            for dx in (-1, 0, 1):
                for dy in (3, 4, 5):
                    for dz in (0, 1, 2):
                        if abs(dx) + abs(dz - 1) + abs(dy - 4) < 3:
                            s.set(x + dx, dy, dz, "guhs:guhbloesem_leaves", blossom)
            s.set(x - 1, 6, 1, "guhs:guhbloesem_leaves", blossom)
            s.set(x + 1, 6, 1, "guhs:guhbloesem_leaves", blossom)
            s.set(x - 1, 4, 0, mc("black_concrete"))
            s.set(x + 1, 4, 0, mc("black_concrete"))
    for i, z in enumerate(range(Z0 + 6, Z1 - 2, 8)):                   # and lamp posts along the east side
        s.fill(X1 + 3, 2, z, X1 + 3, 4, z, mc("cherry_fence"))
        s.set(X1 + 3, 5, z, *lamp("mint" if i % 2 else "geel"))

    # --- two guh statues on quartz plinths, either side of the plaza ---
    for ox in (fx - 26, fx + 15):
        zl = s.size[2] - 1
        for x in range(ox - 1, ox + 13):
            for z in range(Z1 + 2, zl + 1):
                s.set(x, 1, z, mc("quartz_bricks") if (x + z) % 2 else mc("pink_concrete"))
                if ox <= x < ox + 12:
                    s.set(x, 2, z, mc("quartz_block"))
        h.ms.voxel_guh(s, ox, 3, Z1 + 2, scale=0.5, face_south=True)
        for (x, z) in ((ox - 1, Z1 + 2), (ox + 12, Z1 + 2), (ox - 1, zl), (ox + 12, zl)):
            s.set(x, 2, z, *lamp("roze"))


# the club (72x64) sits in bigger grounds (96x84, like the other minigames: ~80-110 wide); keep in sync with DiscoGameTests
GW, GD, OX, OZ = 96, 84, 12, 6


def disco_grounds(h, club):
    """The grounds round the club: the club itself (shifted by OX/OZ), a flowery hedge round the edge, a neon DISCO arch
    over the path in, a shake terrace with parasols (west) and a guh stable with hay (east), where you park your guh
    while you dance."""
    mc = h.mc
    g = h.Structure((GW, club.size[1], GD))
    rng = random.Random(4405)
    fp = [(x, z) for x in range(GW) for z in range(GD)]
    for x, z in fp:
        g.set(x, 0, z, mc("dirt"))
        g.set(x, 1, z, mc("grass_block"), {"snowy": "false"})
    for (x, y, z), b in club.blocks.items():
        g.blocks[(x + OX, y, z + OZ)] = b
    for x, y, z, nbt in club.entities:
        nbt = dict(nbt)
        if "TileX" in nbt:
            nbt["TileX"] += OX
            nbt["TileZ"] += OZ
        g.entity(x + OX, y, z + OZ, nbt)
    px0, px1 = 36 - 3 + OX, 36 + 3 + OX                       # the pink path in (the club's plaza path, on to the edge)
    for x in range(px0, px1 + 1):
        for z in range(club.size[2] + OZ - 1, GD):
            g.set(x, 1, z, mc("white_concrete") if x in (px0, px1) else mc("pink_concrete_powder"))
    flowers = ["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]
    # --- a flowery hedge round the grounds, open where the path comes in ---
    for x, z in fp:
        ring = min(x, z, GW - 1 - x, GD - 1 - z)
        in_club = OX <= x < OX + club.size[0] and OZ <= z < OZ + club.size[2]
        if ring == 0 and (x + z) % 4 and not (px0 <= x <= px1 and z == GD - 1):
            g.set(x, 2, z, mc("flowering_azalea_leaves"), {"persistent": "true", "distance": "7", "waterlogged": "false"})
        elif ring == 1 and not in_club and (x * 7 + z * 3) % 5 == 0 and not px0 <= x <= px1:
            g.set(x, 2, z, rng.choice(flowers))
    # --- the neon arch: DISCO in froglights on a pink band, on two quartz pillars ---
    az = GD - 6
    ax0, ax1 = px0 - 6, px1 + 6
    for x in range(ax0, ax1 + 1):
        for y in range(9, 16):
            g.set(x, y, az, mc("magenta_concrete") if y in (9, 15) or x in (ax0, ax1) else mc("pink_concrete"))
    for x in (ax0, ax1):
        g.fill(x, 2, az, x, 8, az, mc("quartz_pillar"), {"axis": "y"})
        g.set(x, 16, az, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    x = ax0 + 1
    for i, ch in enumerate("DISCO"):                          # read from the path (facing north): left to right = +x
        for r, row in enumerate(FONT[ch]):
            for c, px in enumerate(row):
                if px == "X":
                    g.set(x + c, 14 - r, az + 1, mc(NEON[i % 3]), {"axis": "y"})
                    g.set(x + c, 14 - r, az - 1, mc(NEON[i % 3]), {"axis": "y"})
        x += len(FONT[ch][0]) + 1
    # --- west: the shake terrace, a wooden deck with parasols, tables and stools ---
    for x in range(2, OX + 4):
        for z in range(22, 58):
            g.set(x, 1, z, mc("stripped_cherry_wood") if (x + z) % 5 else mc("cherry_planks"), {"axis": "y"} if (x + z) % 5 else None)
    for i, (tx, tz) in enumerate(((6, 26), (6, 34), (6, 42), (6, 50), (12, 30), (12, 46))):
        g.set(tx, 2, tz, "guhs:guh_tafel", {"facing": "north"})
        g.set(tx - 1, 2, tz, "guhs:guh_stoel", {"facing": "east"})
        g.set(tx + 1, 2, tz, "guhs:guh_stoel", {"facing": "west"})
        g.fill(tx, 3, tz, tx, 4, tz, mc("cherry_fence"))
        wool = ("pink_wool", "white_wool") if i % 2 else ("magenta_wool", "yellow_wool")
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                g.set(tx + dx, 5, tz + dz, mc(wool[(dx + dz) % 2]))
        g.set(tx, 6, tz, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    for z in (22, 57):
        for x in range(2, OX + 4, 3):
            g.set(x, 2, z, "guhs:potted_" + flowers[(x // 3) % 4].split(":")[1])
    # --- east: the guh stable, a fenced paddock with hay bales and a trough, open to the path ---
    sx0, sx1 = OX + club.size[0] - 2, GW - 3
    for x in range(sx0, sx1 + 1):
        for z in range(22, 58):
            edge = x in (sx0, sx1) or z in (22, 57)
            if edge and not (x == sx0 and 36 <= z <= 40):
                g.set(x, 2, z, mc("cherry_fence"))
            elif not edge:
                g.set(x, 1, z, mc("coarse_dirt") if (x * 3 + z) % 4 == 0 else mc("grass_block"), None if (x * 3 + z) % 4 == 0 else {"snowy": "false"})
    for (x, z) in ((sx1 - 2, 25), (sx1 - 3, 25), (sx1 - 2, 26), (sx1 - 2, 54), (sx1 - 3, 54), (sx1 - 2, 53)):
        g.set(x, 2, z, mc("hay_block"), {"axis": "y"})
    g.set(sx1 - 2, 3, 25, mc("hay_block"), {"axis": "x"})
    for z in range(30, 34):
        g.set(sx1 - 1, 2, z, mc("composter"), {"level": "0"})
    for z in (22, 57):
        for x in (sx0, sx1):
            g.set(x, 3, z, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})
    # --- the front lawn: guh blossom trees with a ring of guh flowers, lampgions hanging from them ---
    blossom = {"persistent": "true", "distance": "1", "waterlogged": "false"}
    for (x, z) in ((9, 74), (24, 78), (GW - 25, 78), (GW - 10, 74)):
        g.fill(x, 2, z, x, 5, z, "guhs:guhbloesem_log", {"axis": "y"})
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                for dy in (5, 6, 7, 8):
                    if dx * dx + dz * dz + 2 * (dy - 6.5) ** 2 <= 9.5 and (dx or dz or dy > 5):
                        g.set(x + dx, dy, z + dz, "guhs:guhbloesem_leaves", blossom)
                if 2 <= abs(dx) + abs(dz) <= 3 and (dx + dz) % 2 == 0:
                    g.set(x + dx, 2, z + dz, flowers[(dx - dz) % 4])
        g.set(x + 2, 4, z, "guhs:lampion_roze", {"hanging": "true", "waterlogged": "false"})
        g.set(x - 2, 4, z, "guhs:lampion_geel", {"hanging": "true", "waterlogged": "false"})
    g.clear_above(fp, 2)
    return g


def wall_block(mc, i, y):
    if y in (13,):
        return mc("yellow_concrete")                          # golden trim
    if y == 12:
        return mc(NEON[(i // 2) % 3]) if i % 2 == 0 else mc("magenta_stained_glass")
    if i % 8 == 0:
        return mc("quartz_pillar")
    if y == 5 and i % 8 == 4:
        return mc("pearlescent_froglight")                     # wall lights between the pillars
    return mc("white_concrete") if y <= 2 or y >= 14 else mc("pink_concrete")


def check_disco(s, cx, cz, X0, X1, Z0, Z1, CEIL):
    """Geometry self-check: no holes in the floors, everything reachable from the plaza, the DJ-guh on solid ground, no
    floating blocks, and light everywhere inside (no mob spawns)."""
    problems = []
    get = s.get
    W, HH, D = s.size
    # 1. floors: every spot inside and on the path has a solid floor
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            if passable(get(x, 1, z)):
                problems.append(f"hole in the floor at {(x, 1, z)}")
    # 2. reachability: walk from the plaza edge (up/down one block per step)
    def solid(x, y, z):
        return not passable(get(x, y, z)) and "fence" not in (get(x, y, z) or "")

    def open_(x, y, z):
        return solid(x, y - 1, z) and passable(get(x, y, z)) and passable(get(x, y + 1, z))
    start = (36, 2, D - 1)
    assert open_(*start), "the plaza edge is walkable"
    seen, todo = {start}, deque([start])
    while todo:
        x, y, z = todo.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1):
                n = (x + dx, y + dy, z + dz)
                if n in seen or not (0 <= n[0] < W and 1 <= n[1] < HH - 1 and 0 <= n[2] < D):
                    continue
                if dy == 1 and not passable(get(x, y + 2, z)):
                    continue                                    # no room to jump
                if open_(*n):
                    seen.add(n)
                    todo.append(n)
    for name, target in (("the dance floor's nose", (cx, 2, cz)), ("the DJ-guh's counter", (36, 3, 11)),
                         ("the pink tiles", (cx - 3, 2, cz - 3)), ("the green tiles", (cx + 3, 2, cz + 3)), ("the bar", (15, 2, 26)),
                         ("the lounge", (56, 2, 25))):
        if target not in seen:
            problems.append(f"can't walk to {name} {target}")
    # 3. the DJ-guh sits on the stage with room above
    if not solid(36, 2, 9) or not passable(get(36, 3, 9)) or not passable(get(36, 4, 9)):
        problems.append("the DJ-guh has no seat")
    # 4. floating blocks: everything hangs together with the ground
    blocks = {p for p, b in s.blocks.items() if b[0] != "minecraft:air"}
    reach = {p for p in blocks if p[1] == 0}
    todo = deque(reach)
    while todo:
        x, y, z = todo.popleft()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in blocks and n not in reach:
                reach.add(n)
                todo.append(n)
    for p in sorted(blocks - reach)[:10]:
        problems.append(f"floating block {get(*p)} at {p}")
    # 5. light: flood the block light through the air; every walkable spot inside gets some
    light = {}
    todo = deque()
    for p, b in s.blocks.items():
        lv = light_of(b[0])
        if lv:
            light[p] = lv
            todo.append(p)
    while todo:
        p = todo.popleft()
        lv = light[p] - 1
        if lv <= 0:
            continue
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (p[0] + d[0], p[1] + d[1], p[2] + d[2])
            if s.inside(*n) and passable(get(*n)) and light.get(n, 0) < lv:
                light[n] = lv
                todo.append(n)
    dark = [p for p in seen if X0 < p[0] < X1 and Z0 < p[2] < Z1 and p[1] < CEIL and light.get(p, 0) < 1]
    for p in dark[:10]:
        problems.append(f"dark spot inside at {p}")
    print(f"guh_disco check: {len(seen)} walkable spots, {len(blocks)} blocks, {len(problems)} problems")
    return problems


def test_floor(h):
    """A small floor for the GameTests: the four 4x4 fields round a golden cross, the DJ-guh at the north end."""
    mc = h.mc
    s = h.Structure((13, 6, 16))
    for x in range(13):
        for z in range(16):
            s.set(x, 0, z, mc("white_concrete"))
    quads = {(0, 0): "roze", (1, 0): "blauw", (0, 1): "geel", (1, 1): "groen"}
    for x in range(2, 11):
        for z in range(5, 14):
            if x == 6 or z == 9:
                s.set(x, 0, z, mc("gold_block"))
            else:
                s.set(x, 0, z, f"guhs:disco_tegel_{quads[(int(x > 6), int(z > 9))]}", {"lit": "false"})
    s.set(6, 4, 9, "guhs:disco_bal")
    s.set(6, 5, 9, mc("chain"), {"axis": "y", "waterlogged": "false"})
    s.entity(6.5, 1.0, 1.5, {"id": "guhs:guh_npc", "Kind": "djguh", "PersistenceRequired": h.Byte(1), "Rotation": h.floats(0.0, 0.0)})
    s.save("disco_testvloer")


# =====================================================================================================================
# everything else
# =====================================================================================================================
def build(h):
    A, D, w, lang = h.A, h.D, h.w, h.lang

    # --- the disco tiles: unlit and lit ---
    for i, (k, (rgb, en, nl)) in enumerate(KLEUREN.items()):
        name = f"disco_tegel_{k}"
        h.save(tile_texture(rgb, False, 100 + i), "block", f"{name}.png")
        h.save(tile_texture(rgb, True, 200 + i), "block", f"{name}_lit.png")
        for suffix in ("", "_lit"):
            w(f"{A}/models/block/{name}{suffix}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/{name}{suffix}"}})
        w(f"{A}/blockstates/{name}.json", {"variants": {"lit=false": {"model": f"guhs:block/{name}"}, "lit=true": {"model": f"guhs:block/{name}_lit"}}})
        w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}_lit"})
        h.self_drop(name)
        h.shaped(name, ["GDG", "DLD", "GDG"], {"G": "minecraft:glass", "D": f"minecraft:{ {'roze': 'pink', 'blauw': 'light_blue', 'geel': 'yellow', 'groen': 'lime'}[k] }_dye",
                                                "L": "minecraft:glowstone_dust"}, f"guhs:{name}", 8)
        lang(f"block.guhs.{name}", f"Disco Tile ({en})", f"Discotegel ({nl})")

    # --- the dance floor: 4 phases of an animated texture ---
    for f in range(4):
        h.save(floor_texture(f), "block", f"disco_dansvloer_{f}.png")
        w(f"{h.TEX}/block/disco_dansvloer_{f}.png.mcmeta", {"animation": {"frametime": 6, "interpolate": False}})
        w(f"{A}/models/block/disco_dansvloer_{f}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/disco_dansvloer_{f}"}})
    w(f"{A}/blockstates/disco_dansvloer.json", {"variants": {f"fase={f}": {"model": f"guhs:block/disco_dansvloer_{f}"} for f in range(4)}})
    w(f"{A}/models/item/disco_dansvloer.json", {"parent": "guhs:block/disco_dansvloer_0"})
    h.self_drop("disco_dansvloer")
    h.shaped("disco_dansvloer", ["PBY", "GLG", "YBP"], {"P": "minecraft:pink_dye", "B": "minecraft:light_blue_dye", "Y": "minecraft:yellow_dye",
                                                       "G": "minecraft:glass", "L": "minecraft:glowstone"}, "guhs:disco_dansvloer", 8)

    # --- the disco ball ---
    h.save(ball_texture(), "block", "disco_bal.png")
    w(f"{h.TEX}/block/disco_bal.png.mcmeta", {"animation": {"frametime": 4, "interpolate": False}})
    h.simple_block("disco_bal")
    h.self_drop("disco_bal")
    h.shaped("disco_bal", ["NGN", "GLG", "NGN"], {"N": "minecraft:iron_nugget", "G": "minecraft:glass", "L": "minecraft:glowstone_dust"},
             "guhs:disco_bal", 4)

    # --- the guhshakes on the bar ---
    faces = lambda uv, tex="#all": {d: {"uv": uv, "texture": tex} for d in ("north", "south", "east", "west")}
    w(f"{A}/models/block/disco_milkshake_base.json", {"parent": "minecraft:block/block", "textures": {"particle": "#all"}, "elements": [
        {"from": [5, 0, 5], "to": [11, 9, 11], "faces": {**faces([0, 0, 6, 9]), "down": {"uv": [0, 10, 6, 16], "texture": "#all"},
                                                         "up": {"uv": [0, 10, 6, 16], "texture": "#all"}}},
        {"from": [4.5, 9, 4.5], "to": [11.5, 11, 11.5], "faces": {**faces([6, 0, 13, 2]), "up": {"uv": [6, 3, 13, 10], "texture": "#all"},
                                                                 "down": {"uv": [6, 3, 13, 10], "texture": "#all"}}},
        {"from": [7.5, 11, 7.5], "to": [8.5, 12, 8.5], "faces": {**faces([14, 0, 15, 1]), "up": {"uv": [14, 0, 15, 1], "texture": "#all"}}},
        {"from": [9, 10, 7.5], "to": [10, 15, 8.5], "faces": {**faces([14, 2, 15, 7]), "up": {"uv": [14, 2, 15, 3], "texture": "#all"}}}]})
    for smaak, liquid in SMAKEN.items():
        h.save(shake_texture(liquid), "block", f"disco_milkshake_{smaak}.png")
        w(f"{A}/models/block/disco_milkshake_{smaak}.json", {"parent": "guhs:block/disco_milkshake_base",
                                                             "textures": {"all": f"guhs:block/disco_milkshake_{smaak}"}})
    w(f"{A}/blockstates/disco_milkshake.json", {"variants": {f"smaak={k}": {"model": f"guhs:block/disco_milkshake_{k}"} for k in SMAKEN}})
    w(f"{A}/models/item/disco_milkshake.json", {"parent": "guhs:block/disco_milkshake_aardbei"})
    h.self_drop("disco_milkshake")

    # --- the discomunt ---
    h.save(coin_texture(), "item", "discomunt.png")
    h.item_model("discomunt")

    # --- the DJ-guh: a purple sitting guh ---
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    h.save(h.recolour(src, hue=0.76, sat=1.25, val=1.0, only=h.pinkish), "entity", "npc_djguh.png")

    # --- the structure (and the GameTests' little floor) ---
    h.TEMPLATE_SIZES["guh_disco"] = GW
    h.FLATNESS["guh_disco"] = 30
    none = {"bounding_box": "full", "spawns": []}
    h.structure("guh_disco", h.GUHMENSION_LAND, spacing=32, separation=12, salt=20240133,
                spawn_overrides={"creature": none, "monster": none, "ambient": none})
    disco_structure(h)
    test_floor(h)

    # --- advancements: shown (find it, the whole outfit) and hidden quest ones (granted by the game) ---
    for name, parent, icon, frame, crit, title, desc in [
        ("find_guh_disco", "enter_guhmension", "guhs:disco_bal", "goal",
         {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:guh_disco"]}}}}},
         ("Saturday Guh Fever", "Guhkoorts op zaterdagavond"), ("Find the Guhdisco", "Vind de Guhdisco")),
        ("disco_outfit", "find_guh_disco", "guhs:disco_afro", "challenge",
         {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
             {"items": "guhs:disco_glitterpak"}, {"items": "guhs:disco_afro"}, {"items": "guhs:disco_bril"}]}}},
         ("Stayin' Vads", "Blijf vads, blijf swingen"), ("Buy the whole disco outfit from the DJ Guh", "Koop het hele discopakje bij de DJ-guh")),
    ]:
        w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": crit})
        lang(f"advancements.guhs.guhmension.{name}.title", *title)
        lang(f"advancements.guhs.guhmension.{name}.description", *desc)
    for name in ("disco_eerste", "disco_5", "disco_10", "disco_15"):
        w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    w(f"{D}/advancement/quest/disco_winkel.json", {
        "criteria": {c: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{c}"}]}} for c in CLOTHES},
        "requirements": [CLOTHES]})

    # --- 2.9: the songs (streamed from the dance floor; made by tools/remix/) ---
    for lied in LIEDJES:
        if not os.path.exists(f"{A}/sounds/disco/{lied}.ogg"):
            raise SystemExit(f"disco: sounds/disco/{lied}.ogg is missing - run python tools/remix/make_*.py (see tools/remix/README.md)")

    def songs(d):
        for lied in LIEDJES:
            d[f"disco.{lied}"] = {"sounds": [{"name": f"guhs:disco/{lied}", "stream": True, "attenuation_distance": 40}],
                                  "subtitle": f"subtitles.guhs.disco.{lied}"}
    h.patch_json(f"{A}/sounds.json", songs)

    # --- 2.9: the Grote Guhspelen tab: every song, the Mika-Mambo master, all four songs, the headphones ---
    for name, parent, icon, frame, crit, title, desc in [
        ("disco_disco70", "root", "guhs:disco_tegel_roze", "task", None,
         "Ze hangen aan me vet!", "Dans op de 70's-versie van \"Ze hangen aan me vet\" in de Guhdisco"),
        ("disco_tango", "disco_disco70", "guhs:disco_tegel_geel", "task", None,
         "Vadsige Tango", "Dans de Vadsige Tango: langzaam, dramatisch en heel erg vads"),
        ("disco_mambo", "disco_disco70", "guhs:disco_tegel_groen", "task", None,
         "Mika-Mambo", "Dans op de Mika-Mambo, het snelste liedje van de DJ-guh (lastig!)"),
        ("disco_boogie", "disco_disco70", "guhs:disco_tegel_blauw", "task", None,
         "Njeg-Njeg Boogie", "Dans op het bonusliedje, de Njeg-Njeg Boogie"),
        ("disco_mambo_meester", "disco_mambo", "guhs:disco_bal", "challenge", None,
         "Mika-Mambo-meester", "Dans 10 kleuren achter elkaar goed op de Mika-Mambo. De Mika's giechelen niet meer, ze klappen!"),
        ("disco_alle_liedjes", "disco_boogie", "guhs:disco_dansvloer", "goal", None,
         "De hele plaat rond", "Dans op alle vier de liedjes minstens 3 kleuren goed"),
        ("disco_koptelefoontje", "disco_disco70", "guhs:disco_koptelefoontje", "task",
         {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:disco_koptelefoontje"}]}}},
         "Beats op je oren", "Koop het discokoptelefoontje bij de DJ-guh"),
    ]:
        w(f"{D}/advancement/grote_guhspelen/{name}.json", {
            "parent": f"guhs:grote_guhspelen/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.grote_guhspelen.{name}.title"},
                        "description": {"translate": f"advancements.guhs.grote_guhspelen.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": crit or {"done": {"trigger": "minecraft:impossible"}}})
        lang(f"advancements.guhs.grote_guhspelen.{name}.title", title, title)
        lang(f"advancements.guhs.grote_guhspelen.{name}.description", desc, desc)

    # --- 2.9 texts (Dutch in both files) ---
    for key, nl in [
        ("item.guhs.disco_koptelefoontje", "Discokoptelefoontje"),
        ("subtitles.guhs.disco.ze_hangen_disco70", "Discomuziek: Ze hangen aan me vet"),
        ("subtitles.guhs.disco.vadsige_tango", "Discomuziek: Vadsige Tango"),
        ("subtitles.guhs.disco.mika_mambo", "Discomuziek: Mika-Mambo"),
        ("subtitles.guhs.disco.njeg_njeg_boogie", "Discomuziek: Njeg-Njeg Boogie"),
        ("gui.guhs.disco.lied.ze_hangen_disco70", "Ze hangen aan me vet"),
        ("gui.guhs.disco.lied.vadsige_tango", "Vadsige Tango"),
        ("gui.guhs.disco.lied.mika_mambo", "Mika-Mambo"),
        ("gui.guhs.disco.lied.njeg_njeg_boogie", "Njeg-Njeg Boogie"),
        # (2.9 visual QA: the short name on the DJ-guh's song button, where the full one doesn't fit)
        ("gui.guhs.disco.lied.ze_hangen_disco70.kort", "Ze hangen!"),
        ("gui.guhs.disco.lied.ze_hangen_disco70.tooltip", "De vaste plaat van de DJ-guh: \"Ze hangen aan me vet\" in een vette 70's-discojas. Medium: na een tijdje komen de kleuren op dubbele tellen."),
        ("gui.guhs.disco.lied.vadsige_tango.tooltip", "Langzaam en dramatisch, met een bandoneon en zingende guhs. Makkelijk: rustig tempo en veel tijd per stap."),
        ("gui.guhs.disco.lied.mika_mambo.tooltip", "Snel en brutaal: de Mika's zingen het koor! Lastig: je begint met 3 kleuren, al gauw dubbele tellen, en 50% meer discomunten."),
        ("gui.guhs.disco.lied.njeg_njeg_boogie.tooltip", "Het bonusliedje met een eigen top 3: boogie-woogiepiano en guhs die \"njeg-njeg!\" zingen. Je begint met 2 kleuren."),
        ("gui.guhs.disco.lied.knop", "%s (%s)"),
        ("gui.guhs.disco.lied.bpm", "%s tellen per minuut"),
        ("gui.guhs.disco.bonus", "Bonus"),
        ("gui.guhs.disco.kies", "Kies je liedje:"),
        ("gui.guhs.scorebord.disco.lied", "%s (%s)"),
        ("quest.guhs.disco.title.dubbel", "DUBBELE TELLEN!"),
        ("quest.guhs.disco.title.dubbel.sub", "nu twee kleuren per tel, njeg!"),
    ]:
        lang(key, nl, nl)

    # --- texts ---
    for key, en, nl in [
        ("entity.guhs.guh_npc.djguh", "DJ Guh", "DJ-guh"),
        ("item.guhs.discomunt", "Disco Coin", "Discomunt"),
        ("item.guhs.disco_glitterpak", "Glitter Vads Suit", "Glittervadspak"),
        ("item.guhs.disco_afro", "Groovy Guh Afro", "Vahoege afropruik"),
        ("item.guhs.disco_bril", "Disco Star Glasses", "Sterren-discobril"),
        ("block.guhs.disco_dansvloer", "Dance Floor Tile", "Dansvloertegel"),
        ("block.guhs.disco_bal", "Disco Ball Block", "Discobalblok"),
        ("block.guhs.disco_milkshake", "Guhshake (decoration)", "Guhshake (decoratie)"),
        ("block.guhs.disco_milkshake.slurp", "Slurp! ...njeg, it's only for show. The DJ Guh sells real ones!",
         "Slurp! ...njeg, deze is alleen om naar te kijken. Echte shakes koop je bij de DJ-guh!"),
        ("structure.guhs.guh_disco", "Guhdisco", "Guhdisco"),
        ("structure.guhs.guh_disco.tooltip", "Minigame: dance Simon says with the DJ Guh, disco coins and a disco outfit",
         "Minigame: dans 'Simon zegt' op de beat van vier liedjes bij de DJ-guh, discomunten en een discopakje"),
        ("gui.guhs.disco.no_build", "Njeg! Hands off the Guhdisco: nothing to break or build here. Just dance!",
         "Njeg! Van de Guhdisco blijf je af: hier mag je niks slopen of bouwen. Gewoon dansen!"),
        # the DJ-guh
        ("quest.guhs.disco.hello", "YO, VAHOEG! Welcome to the Guhdisco! I play the colours, you dance them after me. Ready to shake your vads?",
         "YO, VAHOEG! Welkom in de Guhdisco! Kies een liedje: ik speel de kleuren precies op de beat en jij danst ze na. Zin om je vads te laten swingen?"),
        ("quest.guhs.disco.busy", "Njeg, the floor is taken! Watch closely and learn from the pros... your turn is next.",
         "Njeg, de vloer is bezet! Kijk goed en leer van de profs... straks ben jij aan de beurt."),
        ("quest.guhs.disco.dancing", "Hey, you're dancing! Back to the floor, the beat doesn't wait!",
         "Hé, jij bent aan het dansen! Terug naar de vloer, de beat wacht niet!"),
        ("quest.guhs.disco.broken", "Njeg... my dance floor is broken, I can't find my four tile colours.",
         "Njeg... mijn dansvloer is kapot, ik kan mijn vier tegelkleuren niet vinden."),
        ("quest.guhs.disco.go", "Watch the tiles light up and listen to the notes. Then step on the same colours in the same order! Every round one more colour, and faster... One wrong step and the music stops. Two of the same? That never happens: step onto the golden cross between colours if you like.",
         "Liedje: %1$s (%2$s). Luister naar de beat: op elke tel flitst er een kleur en klinkt er een toon. Stap daarna op dezelfde kleuren in dezelfde volgorde! Elke ronde komt er een kleur bij. Het tempo is gewoon dat van het liedje... maar straks komen de kleuren op dubbele tellen. Een verkeerde stap en de muziek stopt. Het gouden kruis is veilig: daar mag je altijd op staan."),
        ("quest.guhs.disco.first", "Your first dance in the Guhdisco! A welcome present: 4 disco coins and a kaasknabbel shake. VAHOEG!",
         "Je eerste dansje in de Guhdisco! Een welkomstcadeautje: 4 discomunten en een kaasknabbelshake. VAHOEG!"),
        ("quest.guhs.disco.left", "You stepped off the dance floor: the song is over!",
         "Je stapte van de dansvloer: de muziek stopt!"),
        ("quest.guhs.disco.done", "The music stops! You danced %s colours in a row and get %s disco coin(s).",
         "De muziek stopt! Je danste %s kleuren achter elkaar na en krijgt %s discomunt(en)."),
        ("quest.guhs.disco.record", "NEW RECORD: %s colours! (it was %s)",
         "NIEUW RECORD op %3$s: %1$s kleuren! (was %2$s)"),
        ("quest.guhs.disco.best", "Your record: %s colours.",
         "Jouw record op %2$s: %1$s kleuren."),
        ("quest.guhs.disco.record_first", "Your first record: %s colours! Can you get on the top 3 above the stage? VAHOEG!",
         "Je eerste record op %2$s: %1$s kleuren! Kom jij in de top 3 boven het podium? VAHOEG!"),
        ("quest.guhs.disco.no_best", "No record yet: not a single colour right. Next time: listen to the notes, guh!",
         "Nog geen record: geen enkele kleur goed. Volgende keer: luister naar de tonen, guh!"),
        ("gui.guhs.scorebord.disco", "Top 3 vadsest dancers", "Top 3 vadsigste dansguhs"),
        ("gui.guhs.scorebord.disco.kleuren", "most colours in a row", "meeste kleuren op een rij"),
        ("quest.guhs.disco.title.ready", "Get ready...", "Klaar voor de start..."),
        ("quest.guhs.disco.title.ready.sub", "watch the tiles, listen to the notes",
         "%1$s - %2$s tellen per minuut - luister naar de beat"),
        ("quest.guhs.disco.title.dans", "DANCE!", "DANSEN!"),
        ("quest.guhs.disco.title.dans.sub", "shake that vads", "schud die vads"),
        ("quest.guhs.disco.title.vahoeg", "VAHOEG!", "VAHOEG!"),
        ("quest.guhs.disco.title.vahoeg.sub", "%s colours, what a dancing guh!", "%s kleuren, wat een dansguh!"),
        ("quest.guhs.disco.title.njeg", "NJEG!", "NJEG!"),
        ("quest.guhs.disco.title.fout", "that should have been %s", "dat had %s moeten zijn"),
        ("quest.guhs.disco.title.te_laat", "too slow! it was %s", "te laat! het was %s"),
        ("quest.guhs.disco.bar.kijk", "Round %s: watch and listen...", "Ronde %s: kijk en luister..."),
        ("quest.guhs.disco.bar.jouw_beurt", "Your turn! %s / %s", "Jouw beurt! %s / %s"),
        ("quest.guhs.disco.bar.jouw_beurt_tijd", "Your turn! %s / %s  (%s s)", "Jouw beurt! %s / %s  (%s s)"),
        ("quest.guhs.disco.bar.goed", "Right! %s colours danced", "Goed zo! %s kleuren gedanst"),
        ("quest.guhs.disco.bar.kijker", "%s is dancing: round %s (%s done)",
         "%1$s danst op %4$s: ronde %2$s (%3$s gehaald)"),
        ("quest.guhs.disco.kleur.roze", "pink", "roze"),
        ("quest.guhs.disco.kleur.blauw", "blue", "blauw"),
        ("quest.guhs.disco.kleur.geel", "yellow", "geel"),
        ("quest.guhs.disco.kleur.groen", "green", "groen"),
        # the screen
        ("gui.guhs.disco.question", "The DJ plays a row of colours: the tiles flash, every colour has its own note. Dance it after him, one tile at a time. Every round one more colour, and faster!",
         "De DJ-guh draait een echt liedje en speelt op de beat een rij kleuren: de tegels flitsen, elke kleur heeft zijn eigen toon. Dans hem na, tegel voor tegel! Het liedje is je niveau."),
        ("gui.guhs.disco.running", "%s is dancing (round %s). One dancer at a time: watch and wait for your turn!",
         "%1$s danst nu op %3$s (ronde %2$s). Er kan er maar een tegelijk: kijk mee en wacht op je beurt!"),
        ("gui.guhs.disco.mine", "You're dancing (round %s)! Want to stop? You keep the disco coins for what you danced.",
         "Je bent aan het dansen (ronde %s)! Wil je stoppen? Je krijgt de discomunten voor wat je al gedanst hebt."),
        ("gui.guhs.disco.start", "Dance!", "Dansen!"),
        ("gui.guhs.disco.start.tooltip", "You go to the middle of the dance floor; after 3, 2, 1 the DJ starts. No items needed, you can't get hurt or hungry while dancing",
         "Je gaat naar het midden van de dansvloer, het liedje begint en na 3, 2, 1 speelt de DJ de kleuren. Je hebt niks nodig en tijdens het dansen krijg je geen honger of schade"),
        ("gui.guhs.disco.stop", "Stop dancing", "Stoppen met dansen"),
        ("gui.guhs.disco.stop.tooltip", "The music stops; you get the disco coins for the colours you danced", "De muziek stopt; je krijgt de discomunten voor de kleuren die je gedanst hebt"),
        ("gui.guhs.disco.shop", "Shop", "Winkeltje"),
        ("gui.guhs.disco.shop.tooltip", "The disco outfit (only here!), shakes and dance floor, for disco coins",
         "Het discopakje en het discokoptelefoontje (alleen hier!), shakes en dansvloer, voor discomunten"),
        ("gui.guhs.disco.best", "Your record: %s colours", "Jouw record: %s kleuren"),
        ("gui.guhs.disco.no_best", "No record yet: dance!", "Nog geen record: dansen!"),
        ("gui.guhs.disco.munten", "Disco coins in your pocket: %s (1 per 2 colours +1, from 3)",
         "Discomunten op zak: %s (1 per 2 kleuren +1, vanaf 3; Mika-Mambo +50%%)"),
        ("gui.guhs.disco.first", "Your first dance comes with a welcome present!", "Bij je eerste dansje krijg je een welkomstcadeautje!"),
    ]:
        lang(key, nl, nl)       # (2.9: all in-game text is Dutch, also in en_us)


def ftb(fq):
    q = fq.q
    q("disco_vind", "Guhkoorts op zaterdagavond", "Zoek de &dGuhdisco&r in de Guhmensie (superkompas: Minigames > Guhdisco). Volg de beat naar de roze club met een guhgezicht!",
      "guhs:disco_bal", [fq.structure("guh_disco")], rewards=(("guhs:kaas_knabbels", 16),), x=-8, y=32)
    q("disco_eerste", "Eerste dansje", "Praat met de &5DJ-guh&r in de mond van de grote guhkop en dans je eerste 'Simon zegt'. Je hebt niks nodig: gewoon je dansbeentjes!",
      "guhs:disco_tegel_roze", [fq.adv("disco_eerste")], rewards=(("guhs:kaasknabbel_milkshake", 2),), x=-6, y=32, xp=100)
    q("disco_5", "Dansbeentjes", "Dans een rij van &e5 kleuren&r foutloos na.", "guhs:disco_tegel_blauw", [fq.adv("disco_5")],
      rewards=(("guhs:discomunt", 3),), x=-4, y=32, xp=150)
    q("disco_10", "Discoguh", "Dans een rij van &e10 kleuren&r foutloos na. Luister naar de tonen, dat helpt!", "guhs:disco_tegel_geel",
      [fq.adv("disco_10")], rewards=(("guhs:discomunt", 5),), x=-2, y=32, xp=300, shape="hexagon")
    q("disco_15", "Vahoege dansmachine", "Dans een rij van &e15 kleuren&r foutloos na. Alleen de allervadsigste dansers halen dit! Wie het verst komt, staat met naam en toenaam in de &etop 3&r boven het podium.",
      "guhs:disco_tegel_groen", [fq.adv("disco_15")], rewards=(("guhs:discomunt", 7),), x=0, y=32, xp=500, shape="gear")
    q("disco_winkel", "Glitter!", "Koop een stuk van het discopakje bij de DJ-guh (met discomunten).", "guhs:disco_bril",
      [fq.adv("disco_winkel")], rewards=(("guhs:disco_dansvloer", 16),), x=2, y=32)
    q("disco_outfit", "Blijf vads, blijf swingen", "Het hele discopakje: glittervadspak, vahoege afropruik en sterren-discobril. Trek het je guh aan en swing!",
      "guhs:disco_afro", [fq.adv("guhs:guhmension/disco_outfit")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=4, y=32, xp=300, shape="gear")
    # 2.9: the songs (the song is the level)
    q("disco_mambo", "Mika-Mambo-meester", "Kies bij de &5DJ-guh&r het lastigste liedje, de &cMika-Mambo&r, en dans &e10 kleuren&r achter elkaar goed. Pas op: al gauw komen de kleuren op dubbele tellen!",
      "guhs:disco_tegel_groen", [fq.adv("guhs:grote_guhspelen/disco_mambo_meester")], rewards=(("guhs:discomunt", 8),), deps=("disco_5",), x=0, y=34, xp=400, shape="gear")
    q("disco_liedjes", "De hele plaat rond", "Dans op alle vier de liedjes van de DJ-guh minstens &e3 kleuren&r goed: de &eVadsige Tango&r, &dZe hangen aan me vet&r, de &cMika-Mambo&r en de &bNjeg-Njeg Boogie&r.",
      "guhs:disco_dansvloer", [fq.adv("guhs:grote_guhspelen/disco_alle_liedjes")], rewards=(("guhs:discomunt", 5), ("guhs:kaasknabbel_milkshake", 2)), deps=("disco_eerste",), x=-2, y=34, xp=300)
    q("disco_koptelefoontje", "Beats op je oren", "Koop het &ddiscokoptelefoontje&r bij de DJ-guh en zet het je guh op de oren. Nu hoort hij de beat overal!",
      "guhs:disco_koptelefoontje", [fq.adv("guhs:grote_guhspelen/disco_koptelefoontje")], rewards=(("guhs:disco_bal", 4),), deps=("disco_winkel",), x=2, y=34)
