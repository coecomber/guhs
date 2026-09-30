"""
Het Guhdoolhof (2.9) - the pixel art: the maze hedge (dense, trimmed, little pink guh-flowers), the hedge with a trimmed
guh face, the guh-ear lantern (off / lit), the doolhofknabbel coin, the gestolen knabbel, Meneer Vadskronkel's look
(a mint guh with a gardener's hat, a leaf sprig and a curly moustache) and the Heg-Mika (a Mika in camouflage leaves).
"""
import os
import random

import numpy as np
from PIL import Image

from features import knuffeldal_npcs as npcs

# hedge greens, dark to light, and the little flowers
GROEN = [(38, 84, 44), (48, 104, 52), (60, 124, 60), (76, 146, 70), (98, 168, 84), (124, 186, 98)]
BLOEM = [(250, 150, 196), (255, 196, 222), (236, 104, 164)]


def heg_pixels(seed, bloemen=True):
    """A 16x16 dense hedge: clumps of leaves (light on top-left, shadow bottom-right), a few pink guh-flowers."""
    rng = random.Random(seed)
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            a[y, x] = (*GROEN[1], 255)
    # leaf clumps: little blobs, lit from the top-left
    for _ in range(34):
        cx, cy = rng.randrange(16), rng.randrange(16)
        r = rng.choice((1, 1, 2))
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy <= r * r + 1:
                    x, y = (cx + dx) % 16, (cy + dy) % 16
                    shade = 3 + (1 if dx <= 0 and dy <= 0 else 0) - (1 if dx > 0 and dy > 0 else 0)
                    shade = max(0, min(5, shade + rng.choice((-1, 0, 0, 1))))
                    a[y, x, :3] = GROEN[shade]
    for _ in range(18):                                              # deep gaps between the leaves
        x, y = rng.randrange(16), rng.randrange(16)
        a[y, x, :3] = GROEN[0]
    if bloemen:
        for _ in range(3):
            x, y = rng.randrange(1, 15), rng.randrange(1, 15)
            c = rng.choice(BLOEM)
            a[y, x, :3] = c
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if rng.random() < 0.5:
                    a[y + dy, x + dx, :3] = BLOEM[1]
            a[y, x, :3] = (255, 236, 120)                               # a yellow heart
            a[y - 1, x, :3] = c
    return a


def heg_gezicht(seed):
    """The hedge with a trimmed guh face: two big eyes (deep cut, a shine), pink cheeks of flowers, a smile."""
    a = heg_pixels(seed, bloemen=False)
    for (ex, ey) in ((4, 6), (11, 6)):
        for dy in range(-2, 2):
            for dx in range(-1, 2):
                a[ey + dy, ex + dx, :3] = (20, 22, 30)
        a[ey - 2, ex - 1, :3] = (255, 255, 255)
        a[ey + 1, ex, :3] = (60, 120, 210)
        a[ey + 1, ex + 1, :3] = (60, 120, 210)
    for (cx, cy) in ((2, 10), (13, 10)):
        for dx in (0, 1):
            a[cy, cx + dx - (1 if cx > 8 else 0), :3] = BLOEM[0]
    for x in range(6, 10):
        a[12 if x in (6, 9) else 13, x, :3] = (26, 52, 30)
    a[11, 7, :3] = a[11, 8, :3] = BLOEM[2]                         # a little nose
    # ears on top: lighter trimmed tufts
    for (ex) in (3, 12):
        for dx in (-1, 0, 1):
            a[0, ex + dx, :3] = GROEN[5]
            a[1, ex + dx, :3] = GROEN[4]
    return a


def lantaarn(aan):
    """The guh-ear lantern: a pink little lamp with a glass belly; lit: warm yellow light inside."""
    a = np.zeros((16, 16, 4), np.uint8)
    frame = (190, 90, 140)
    glas = (255, 214, 120) if aan else (120, 96, 118)
    kern = (255, 250, 200) if aan else (92, 70, 90)
    for y in range(16):
        for x in range(16):
            a[y, x] = (*frame, 255)
    for y in range(3, 13):
        for x in range(3, 13):
            a[y, x, :3] = glas
    for y in range(6, 10):
        for x in range(6, 10):
            a[y, x, :3] = kern
    for x in range(16):
        a[0, x, :3] = a[15, x, :3] = (150, 64, 110)
    for y in range(16):
        a[y, 0, :3] = a[y, 15, :3] = (150, 64, 110)
    return a


def knabbel_icon(munt):
    """A kaasknabbel item: orange, puffy, crumbly; as the coin wrapped in a hedge leaf with a gold rim."""
    img = np.zeros((16, 16, 4), np.uint8)
    rng = random.Random(29201 if munt else 29202)
    if munt:
        for y in range(16):
            for x in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if d <= 7.4:
                    img[y, x] = (*((246, 200, 70) if d > 6.2 else GROEN[3 if (x + y) % 3 else 4]), 255)
                if 6.2 < d <= 6.8:
                    img[y, x, :3] = (200, 150, 40)
    # the knabbel itself: a curly puff
    pts = [(5, 6), (6, 5), (7, 5), (8, 6), (9, 7), (10, 8), (9, 9), (8, 10), (7, 10), (6, 9), (5, 8), (7, 7), (8, 8), (6, 7), (7, 8), (8, 7)]
    if not munt:
        pts = [(x * 1.5 - 3, y * 1.5 - 3) for x, y in pts]
    for (x, y) in pts:
        for dy in (0, 1):
            for dx in (0, 1):
                xx, yy = int(x + dx), int(y + dy)
                if 0 <= xx < 16 and 0 <= yy < 16:
                    c = (242, 150, 48) if rng.random() < 0.7 else (252, 196, 84)
                    img[yy, xx] = (*c, 255)
    # a dark outline
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if img[y, x, 3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and img[y + dy, x + dx, 3] > 0
                                         and tuple(img[y + dy, x + dx, :3]) in ((242, 150, 48), (252, 196, 84))
                                         for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out[y, x] = (150, 76, 30, 255)
    if not munt:
        out[3, 12] = out[4, 13] = (255, 255, 220, 255)                 # a sparkle: it's precious
        out[2, 13] = out[4, 11] = (255, 240, 150, 255)
        for (x, y) in ((11, 11), (12, 12), (10, 12)):                 # a little Mika paw print (it was stolen!)
            out[y, x] = (130, 90, 150, 255)
    return Image.fromarray(out)


def build(h):
    for i, name in enumerate(("doolhofheg", "doolhofheg_b")):
        h.save(Image.fromarray(heg_pixels(29210 + i)), "block", f"{name}.png")
    h.save(Image.fromarray(heg_gezicht(29212)), "block", "doolhofheg_gezicht.png")
    h.save(Image.fromarray(lantaarn(False)), "block", "doolhof_lantaarn.png")
    h.save(Image.fromarray(lantaarn(True)), "block", "doolhof_lantaarn_aan.png")
    h.save(knabbel_icon(True), "item", "doolhofknabbel.png")
    h.save(knabbel_icon(False), "item", "gestolen_knabbel.png")
    vadskronkel(h)
    heg_mika(h)


# ======================================================================================================================
# Meneer Vadskronkel: a mint guh gardener with a green bucket hat (a leaf sprig on it) and a curly moustache
# ======================================================================================================================
def vadskronkel(h):
    geo_file = npcs._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_doolhofguh"
    sw = npcs._swatches(geo, ["hoed", "band", "blad", "snor", "schaar"])
    geo["bones"].append({"name": "vadskronkel_hoed", "parent": "head", "pivot": [0, 26, 0], "cubes": [
        npcs._cube([-6.6, 25.6, -6.4], [13.2, 0.8, 11.8], sw["hoed"]),     # the wide brim
        npcs._cube([-4.6, 26.4, -4.4], [9.2, 3.2, 7.8], sw["hoed"]),       # the crown
        npcs._cube([-4.7, 26.4, -4.5], [9.4, 1.0, 8.0], sw["band"])]})     # the band
    geo["bones"].append({"name": "vadskronkel_takje", "parent": "vadskronkel_hoed", "pivot": [3.6, 27.5, -1], "cubes": [
        npcs._cube([3.4, 27.2, -1.4], [0.6, 3.6, 0.6], sw["schaar"]),
        npcs._cube([2.4, 30.2, -2.2], [2.6, 1.6, 2.2], sw["blad"]),
        npcs._cube([4.0, 29.0, -1.8], [2.0, 1.2, 1.6], sw["blad"])]})
    geo["bones"].append({"name": "vadskronkel_snor", "parent": "head", "pivot": [0, 16.5, -5.4], "cubes": [
        npcs._cube([-4.4, 16.2, -5.6], [3.8, 1.1, 0.6], sw["snor"]),
        npcs._cube([0.6, 16.2, -5.6], [3.8, 1.1, 0.6], sw["snor"]),
        npcs._cube([-5.2, 16.9, -5.55], [1.2, 1.4, 0.5], sw["snor"]),       # the curls
        npcs._cube([4.0, 16.9, -5.55], [1.2, 1.4, 0.5], sw["snor"])]})
    geo["bones"].append({"name": "vadskronkel_heggenschaar", "parent": "body", "pivot": [4, 9, -5], "cubes": [
        npcs._cube([3.2, 6.0, -7.4], [0.8, 3.6, 0.8], sw["schaar"]),
        npcs._cube([4.4, 6.0, -7.4], [0.8, 3.6, 0.8], sw["schaar"]),
        npcs._cube([3.0, 9.6, -7.6], [2.4, 3.8, 0.4], sw["band"])]})
    npcs._save_geo(h, "guh_npc_doolhofguh.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.40, sat=0.45, val=1.02, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(29220)
    npcs._paint(a, sw["hoed"], (70, 132, 70), rng, 8)
    npcs._paint(a, sw["band"], (244, 138, 184), rng, 6)
    npcs._paint(a, sw["blad"], (110, 190, 90), rng, 12)
    npcs._paint(a, sw["snor"], (238, 236, 228), rng, 5)
    npcs._paint(a, sw["schaar"], (150, 110, 70), rng, 6)
    h.save(Image.fromarray(a), "entity", "npc_doolhofguh.png")


# ======================================================================================================================
# the Heg-Mika: the Mika's model with leaves and twigs on its head (camouflage!), leafy green fur
# ======================================================================================================================
def heg_mika(h):
    geo_file = npcs._load(h, "mika.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.doolhof_mika"
    geo["bones"] = [b for b in geo["bones"] if not b["name"].startswith("heg_")]
    sw = npcs._swatches(geo, ["blad", "blad2", "takje"])
    geo["bones"].append({"name": "heg_bladeren", "parent": "head", "pivot": [0, 14, -6], "cubes": [
        npcs._cube([-5.0, 14.0, -9.0], [4.0, 2.6, 4.0], sw["blad"]),
        npcs._cube([0.6, 14.0, -8.0], [4.4, 3.0, 4.2], sw["blad2"]),
        npcs._cube([-2.2, 15.8, -6.2], [4.2, 2.4, 3.8], sw["blad"]),
        npcs._cube([-0.4, 16.8, -7.6], [0.6, 3.4, 0.6], sw["takje"]),
        npcs._cube([-1.4, 19.8, -8.2], [2.4, 1.2, 1.6], sw["blad2"])]})
    npcs._save_geo(h, "doolhof_mika.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")
    img = h.recolour(src, hue=0.30, sat=0.55, val=0.95, only=lambda hh, ss, vv: vv > 0.3).convert("RGBA")
    a = np.asarray(img).copy()
    rng = random.Random(29230)
    opaque = np.argwhere(a[..., 3] > 0)
    for _ in range(1400):                                          # leaf speckles all over
        y, x = opaque[rng.randrange(len(opaque))]
        c = rng.choice(GROEN[2:])
        a[y:y + 2, x:x + 2, :3] = c
    nrng = np.random.default_rng(29231)
    npcs._paint(a, sw["blad"], GROEN[3], nrng, 14)
    npcs._paint(a, sw["blad2"], GROEN[4], nrng, 14)
    npcs._paint(a, sw["takje"], (120, 84, 50), nrng, 8)
    h.save(Image.fromarray(a), "entity", "doolhof_mika.png")
