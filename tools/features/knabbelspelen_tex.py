"""
De Knabbelspelen (2.9) - the pixel art: the spelenlintje (a pink ribbon with a golden guh medal), the loaned things
(guh sack, spoon with a knabbelei, knabbelspijker, the guhguhtje's tail, the soft pluisbal), the Mika tin and the
kaasmelk bottle, and Juf Vahoegsakee's look (a peachy sporty guh with a sweatband, a whistle and a clipboard).
"""
import os
import random

import numpy as np
from PIL import Image

from features import knuffeldal_npcs as npcs


def leeg():
    return np.zeros((16, 16, 4), np.uint8)


def zet(a, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        a[y, x] = (*c, 255)


def omlijn(a, kleur):
    """A dark outline around everything drawn."""
    out = a.copy()
    for y in range(16):
        for x in range(16):
            if a[y, x, 3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and a[y + dy, x + dx, 3] > 0
                                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out[y, x] = (*kleur, 255)
    return out


def lintje():
    a = leeg()
    for y in range(0, 8):                                        # the ribbon: two pink tails
        for x in (5, 6, 9, 10):
            zet(a, x - (y // 4 if x < 8 else -(y // 4)), y, (236, 104, 164) if x in (5, 10) else (250, 170, 206))
    for y in range(7, 16):
        for x in range(3, 13):
            d = ((x - 7.5) ** 2 + (y - 11) ** 2) ** 0.5
            if d <= 4.3:
                zet(a, x, y, (246, 200, 70) if d > 3.2 else (255, 226, 120))
    zet(a, 6, 10, (60, 40, 50))                                   # the guh face on the medal
    zet(a, 9, 10, (60, 40, 50))
    zet(a, 7, 12, (200, 110, 60))
    zet(a, 8, 12, (200, 110, 60))
    zet(a, 5, 11, (250, 150, 150))
    zet(a, 10, 11, (250, 150, 150))
    return omlijn(a, (120, 60, 70))


def zak():
    a = leeg()
    rng = random.Random(29410)
    for y in range(3, 16):
        for x in range(2, 14):
            if y < 5 and not (4 <= x <= 11):
                continue
            c = (196, 160, 110) if rng.random() < 0.7 else (176, 140, 94)
            zet(a, x, y, c)
    for x in range(4, 12):                                        # the tied top
        zet(a, x, 4, (150, 110, 70))
    for (x, y) in ((6, 1), (7, 2), (8, 2), (9, 1)):
        zet(a, x, y, (150, 110, 70))
    for (x, y) in ((5, 9), (10, 9)):                              # a guh face printed on it
        zet(a, x, y, (60, 40, 50))
    zet(a, 7, 11, (236, 104, 164))
    zet(a, 8, 11, (236, 104, 164))
    zet(a, 4, 10, (250, 150, 180))
    zet(a, 11, 10, (250, 150, 180))
    return omlijn(a, (100, 70, 44))


def lepel():
    a = leeg()
    for i in range(9):                                            # the handle, diagonal
        zet(a, 2 + i, 14 - i, (190, 190, 200))
    for y in range(2, 8):
        for x in range(9, 15):
            d = ((x - 11.5) ** 2 + (y - 4.5) ** 2) ** 0.5
            if d <= 2.9:
                zet(a, x, y, (210, 210, 220))
    for y in range(1, 7):                                         # the pink knabbelei on it
        for x in range(10, 14):
            d = ((x - 11.5) / 1.8) ** 2 + ((y - 3.5) / 2.4) ** 2
            if d <= 1:
                zet(a, x, y, (252, 190, 214) if (x + y) % 3 else (255, 225, 236))
    zet(a, 11, 2, (255, 255, 255))
    return omlijn(a, (90, 80, 100))


def spijker():
    a = leeg()
    for i in range(10):                                           # the shaft, diagonal
        zet(a, 3 + i, 12 - i, (170, 170, 180))
        zet(a, 4 + i, 12 - i, (130, 130, 140))
    zet(a, 2, 13, (120, 120, 130))
    for (x, y) in ((12, 1), (13, 1), (13, 2), (14, 2), (12, 2), (13, 3), (11, 1), (14, 3)):   # a kaasknabbel head
        zet(a, x, y, (242, 150, 48))
    zet(a, 13, 2, (252, 196, 84))
    return omlijn(a, (70, 60, 70))


def staartje():
    a = leeg()
    pts = [(3, 12), (4, 11), (5, 11), (6, 10), (7, 9), (8, 8), (9, 7), (10, 7), (11, 6), (12, 5), (12, 4), (11, 3), (10, 3), (10, 4)]
    for (x, y) in pts:                                             # a curly pink guh tail
        zet(a, x, y, (246, 150, 190))
        zet(a, x, y + 1, (230, 120, 170))
    for (x, y) in ((2, 13), (1, 14), (2, 14)):                     # the pin
        zet(a, x, y, (200, 200, 210))
    zet(a, 3, 13, (236, 60, 100))
    return omlijn(a, (120, 60, 90))


def pluisbal():
    a = leeg()
    rng = random.Random(29411)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d <= 5.4 + rng.random() * 1.1:
                c = (252, 196, 222) if rng.random() < 0.6 else (255, 230, 240)
                zet(a, x, y, c)
    zet(a, 6, 5, (255, 255, 255))
    zet(a, 5, 6, (255, 255, 255))
    return omlijn(a, (200, 120, 160))


# --- block textures --------------------------------------------------------------------------------------------------
def blik(voor):
    """A tin can: silver with rings; the front with a purple Mika face (squinty eyes, a cheeky grin)."""
    a = np.zeros((16, 16, 4), np.uint8)
    rng = random.Random(29412 + voor)
    for y in range(16):
        for x in range(16):
            v = 185 + int(40 * np.sin((x - 3) / 10 * np.pi)) + rng.randint(-6, 6)
            a[y, x] = (v, v, min(255, v + 10), 255)
    for y in (2, 13):
        a[y, :, :3] = (140, 140, 152)
    if voor:
        for y in range(4, 12):                                     # a purple label
            for x in range(3, 13):
                a[y, x, :3] = (176, 120, 196)
        for (x, y) in ((5, 6), (6, 6), (9, 6), (10, 6)):            # squinty eyes
            a[y, x, :3] = (40, 20, 50)
        for (x, y) in ((5, 9), (6, 10), (7, 10), (8, 10), (9, 10), (10, 9)):
            a[y, x, :3] = (40, 20, 50)
        a[10, 7, :3] = a[10, 8, :3] = (236, 104, 164)                # a tongue
        a[7, 4, :3] = a[7, 11, :3] = (236, 150, 200)
    return Image.fromarray(a)


def blik_boven():
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            v = 170 if d > 6 else 200
            a[y, x] = (v, v, v + 12, 255)
    a[7:9, 6:10, :3] = (150, 150, 160)
    return Image.fromarray(a)


def fles(hals=False):
    """The kaasmelk bottle: creamy yellow milk behind glass, a pink label with a guh face."""
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            a[y, x] = (252, 240, 196, 255) if not hals else (236, 240, 244, 255)
    if not hals:
        for x in range(16):
            a[0, x, :3] = (236, 240, 244)
        for y in range(6, 12):
            for x in range(2, 14):
                a[y, x, :3] = (246, 150, 190)
        for (x, y) in ((5, 8), (10, 8)):
            a[y, x, :3] = (60, 40, 50)
        a[10, 7, :3] = a[10, 8, :3] = (200, 90, 120)
        for y in range(16):
            a[y, 1, :3] = (255, 255, 255)
    else:
        for y in range(16):
            a[y, 4, :3] = (255, 255, 255)
    return Image.fromarray(a)


def build(h):
    for name, img in (("spelenlintje", lintje()), ("guh_zak", zak()), ("knabbelei_lepel", lepel()), ("knabbelspijker", spijker()),
                      ("guhguhtje_staartje", staartje()), ("blik_pluisbal", pluisbal())):
        h.save(Image.fromarray(img), "item", f"{name}.png")
    h.save(blik(1), "block", "knabbelspelen_blik_voor.png")
    h.save(blik(0), "block", "knabbelspelen_blik_zij.png")
    h.save(blik_boven(), "block", "knabbelspelen_blik_boven.png")
    h.save(fles(), "block", "knabbelspelen_kaasmelkfles.png")
    h.save(fles(True), "block", "knabbelspelen_kaasmelkfles_hals.png")
    spijkerkop = np.zeros((16, 16, 4), np.uint8)
    spijkerkop[..., :3] = (242, 150, 48)
    spijkerkop[..., 3] = 255
    spijkerkop[6:10, 6:10, :3] = (150, 150, 160)
    h.save(Image.fromarray(spijkerkop), "block", "knabbelspelen_spijkerkop.png")
    vahoegsakee(h)


# ======================================================================================================================
# Juf Vahoegsakee: a peachy sporty guh with a red-and-white sweatband, a whistle on a cord and a clipboard
# ======================================================================================================================
def vahoegsakee(h):
    geo_file = npcs._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_spelleiderguh"
    sw = npcs._swatches(geo, ["band", "koord", "fluit", "bord", "papier"])
    geo["bones"].append({"name": "vahoegsakee_zweetband", "parent": "head", "pivot": [0, 23, 0], "cubes": [
        npcs._cube([-8.3, 22.2, -4.8], [16.6, 1.6, 0.4], sw["band"]),
        npcs._cube([-8.3, 22.2, 3.4], [16.6, 1.6, 0.4], sw["band"]),
        npcs._cube([-8.3, 22.2, -4.8], [0.4, 1.6, 8.6], sw["band"]),
        npcs._cube([7.9, 22.2, -4.8], [0.4, 1.6, 8.6], sw["band"])]})
    geo["bones"].append({"name": "vahoegsakee_fluitje", "parent": "body", "pivot": [0, 12.5, -3.6], "cubes": [
        npcs._cube([-3.4, 12.2, -3.7], [6.8, 0.5, 0.5], sw["koord"]),
        npcs._cube([-0.3, 9.6, -3.9], [0.5, 2.8, 0.5], sw["koord"]),
        npcs._cube([-1.0, 8.4, -4.8], [2.0, 1.4, 2.4], sw["fluit"])]})
    geo["bones"].append({"name": "vahoegsakee_klembord", "parent": "body", "pivot": [-3.5, 8, -5], "cubes": [
        npcs._cube([-6.0, 6.0, -7.2], [4.2, 5.2, 0.5], sw["bord"]),
        npcs._cube([-5.6, 6.4, -7.35], [3.4, 4.2, 0.2], sw["papier"])]})
    npcs._save_geo(h, "guh_npc_spelleiderguh.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.055, sat=0.7, val=1.03, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(29420)

    def band(block):
        block[..., :3] = (230, 50, 70)
        for x in range(0, 32, 8):
            block[:, x:x + 4, :3] = (255, 255, 255)
    npcs._paint(a, sw["band"], (230, 50, 70), rng, 4, band)
    npcs._paint(a, sw["koord"], (230, 50, 70), rng, 6)
    npcs._paint(a, sw["fluit"], (210, 214, 224), rng, 6)
    npcs._paint(a, sw["bord"], (150, 110, 70), rng, 6)

    def papier(block):
        block[..., :3] = (250, 250, 244)
        for y in range(4, 32, 5):
            block[y, 3:29, :3] = (150, 170, 220)
        block[6:10, 22:28, :3] = (236, 104, 164)
    npcs._paint(a, sw["papier"], (250, 250, 244), rng, 2, papier)
    h.save(Image.fromarray(a), "entity", "npc_spelleiderguh.png")
