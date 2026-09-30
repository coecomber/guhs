"""
Textures for Vahoege Vads (ore, raw, ingot, tools, armour + worn armour layers) and the two guh compasses.
Most are the vanilla diamond/iron/compass textures recoloured to guh pink & lilac, so they fit Minecraft's style.

Run from the project root (after a Gradle build, it reads the vanilla textures):  python tools/make_vads_textures.py
"""
import colorsys
import os
import zipfile

import numpy as np
from PIL import Image

JAR = os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar")
TEX = os.path.join("src", "main", "resources", "assets", "guhs", "textures")
jar = zipfile.ZipFile(JAR)


def vanilla(path):
    with jar.open(f"assets/minecraft/textures/{path}.png") as f:
        img = Image.open(f).convert("RGBA")
        img.load()
    return img


def save(img, *path):
    full = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def recolour(img, hue, sat_boost=1.0, pick=lambda h, s, v: True, value_scale=1.0):
    """Moves the hue of the chosen pixels to `hue` (0-1), keeping their shading."""
    a = np.asarray(img).astype(np.float32) / 255
    out = a.copy()
    for y in range(a.shape[0]):
        for x in range(a.shape[1]):
            r, g, b, al = a[y, x]
            if al == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r, g, b)
            if pick(h, s, v):
                nr, ng, nb = colorsys.hsv_to_rgb(hue, min(1, max(s, 0.35) * sat_boost), min(1, v * value_scale))
                out[y, x, :3] = (nr, ng, nb)
    return Image.fromarray((out * 255).astype(np.uint8), "RGBA")


PINK, LILAC = 0.92, 0.78
is_diamond = lambda h, s, v: 0.40 < h < 0.60 and s > 0.15   # cyan diamond pixels (not the wooden handles)
is_grey = lambda h, s, v: s < 0.2 and v > 0.15

# ingot + raw (from iron)
save(recolour(vanilla("item/iron_ingot"), LILAC, 1.4, is_grey, 1.0), "item", "vahoege_vads_ingot.png")
save(recolour(vanilla("item/raw_iron"), PINK, 1.3, lambda h, s, v: True), "item", "vahoege_vads.png")
# tools + armour items (from diamond)
for tool in ("sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots"):
    save(recolour(vanilla(f"item/diamond_{tool}"), PINK, 1.1, is_diamond), "item", f"vahoege_vads_{tool}.png")
# the paxel: the pickaxe with a lilac axe blade on its left end and a shovel spade on its right end
pa = np.asarray(recolour(vanilla("item/diamond_pickaxe"), PINK, 1.1, is_diamond)).copy()
BLADE = ["..LL....",       # axe blade (L light, M mid, D dark, O outline)
         ".LLMO...",
         "LLMMMO..",
         "LMMMDO..",
         "LMMDDO..",
         ".MMDO...",
         "..OO...."]
SPADE = ["..OO.",
         ".OLMO",
         "OLMMO",
         "OMMDO",
         "OMDDO",
         ".ODO.",
         "..O.."]
LCOL = {"L": (236, 200, 250), "M": (196, 140, 222), "D": (150, 92, 184), "O": (60, 26, 80)}
SCOL = {"L": (255, 210, 230), "M": (250, 150, 195), "D": (210, 90, 150), "O": (80, 20, 50)}
for (art, ox, oy, cols) in ((BLADE, 0, 0, LCOL), (SPADE, 11, 9, SCOL)):
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch != "." and 0 <= ox + x < 16 and 0 <= oy + y < 16:
                pa[oy + y, ox + x] = (*cols[ch], 255)
save(Image.fromarray(pa, "RGBA"), "item", "vahoege_vads_paxel.png")
# armour as worn (the 3D layers)
for layer in (1, 2):
    save(recolour(vanilla(f"models/armor/diamond_layer_{layer}"), PINK, 1.1, is_diamond),
         "entity", "equipment", "humanoid" if layer == 1 else "humanoid_leggings", "vahoege_vads.png")  # 26.1 equipment asset

# the ore: lilac "compressed" stone with glossy pink/magenta vads crystals
rng = np.random.default_rng(42)
ore = np.zeros((16, 16, 4), np.uint8)
for y in range(16):
    for x in range(16):
        n = int(rng.integers(-10, 11))
        ore[y, x] = (180 + n, 150 + n, 200 + n, 255)
        if (x + y * 3) % 7 == 0:
            ore[y, x, :3] = (160 + n, 128 + n, 185 + n)
crystals = [(3, 3), (10, 4), (6, 9), (12, 11), (2, 12)]
for cx, cy in crystals:
    for dx, dy, col in ((0, 0, (236, 90, 160)), (1, 0, (250, 140, 195)), (0, 1, (200, 60, 130)), (1, 1, (236, 90, 160)),
                        (-1, 0, (200, 60, 130)), (0, -1, (255, 200, 225)), (1, -1, (255, 235, 245))):
        if 0 <= cx + dx < 16 and 0 <= cy + dy < 16:
            ore[cy + dy, cx + dx, :3] = col
save(Image.fromarray(ore, "RGBA"), "block", "compressed_super_vahoege_vads.png")

# compasses: 32 needle frames each, case recoloured (pink for guh caves, dark red for the challenging caves)
for i in range(32):
    frame = vanilla(f"item/compass_{i:02d}")
    save(recolour(frame, PINK, 1.2, is_grey, 1.05), "item", f"guh_cave_compass_{i:02d}.png")
    dark = recolour(frame, 0.97, 1.4, is_grey, 0.55)
    save(dark, "item", f"challenge_compass_{i:02d}.png")
print("vads + compass textures written")
