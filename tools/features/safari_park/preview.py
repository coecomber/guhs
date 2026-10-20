"""
Previews of the generated park (outside the repo, in ../../safari-ontwerp/park/):
  park_boven.png      top-down map, 1 px = 1 block, hillshade, fences dark, homes of the animals as dots
  park_<plek>.png     iso renders of a few spots with the vibekit renderer (base-ontwerpen/vibes/_bron)
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

from . import kit

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", "..", "..", ".."))       # MineScapeStory
OUT = os.path.join(ROOT, "safari-ontwerp", "park")

KLEUR = {
    "grass_block": (110, 150, 70), "podzol": (110, 80, 45), "moss_block": (90, 130, 50), "coarse_dirt": (130, 95, 65),
    "rooted_dirt": (140, 100, 70), "dirt": (130, 95, 65), "sand": (220, 205, 150), "red_sand": (190, 105, 50),
    "mud": (70, 60, 60), "muddy_mangrove_roots": (80, 65, 55), "clay": (160, 165, 180), "snow_block": (240, 245, 250),
    "snow": (245, 248, 252), "packed_ice": (150, 185, 235), "ice": (170, 205, 250), "gravel": (135, 128, 125),
    "stone": (125, 125, 125), "granite": (150, 105, 85), "andesite": (135, 135, 135), "cobblestone": (120, 120, 120),
    "mossy_cobblestone": (105, 125, 95), "terracotta": (150, 90, 65), "orange_terracotta": (160, 85, 40),
    "red_sandstone": (180, 100, 40), "water": (60, 120, 200), "dirt_path": (150, 125, 75),
    "smooth_sandstone": (225, 215, 165), "cut_sandstone": (215, 205, 150), "spruce_planks": (115, 85, 50),
    "dark_oak_fence": (60, 40, 20), "iron_bars": (60, 60, 60), "dark_oak_log": (60, 45, 25),
    "dark_oak_fence_gate": (90, 60, 30), "short_grass": (120, 165, 75), "tall_grass": (130, 170, 80),
    "fern": (80, 130, 60), "large_fern": (80, 130, 60), "dead_bush": (140, 110, 60), "bamboo": (120, 170, 60),
    "lily_pad": (40, 120, 40), "seagrass": (40, 100, 60), "sugar_cane": (140, 190, 100),
}


def _kleur(naam, zone_kleur=None):
    n = naam.split("[")[0].split(":")[1]
    if n.endswith("_leaves"):
        return {"acacia": (95, 125, 40), "jungle": (40, 100, 30), "mangrove": (60, 110, 40), "spruce": (40, 80, 50),
                "birch": (110, 150, 70), "oak": (60, 120, 40), "cherry": (235, 170, 200), "azalea": (90, 140, 60),
                "dark_oak": (45, 95, 35)}.get(n[:-7], (60, 120, 40))
    if n.endswith(("_log", "_wood")):
        return (100, 80, 50)
    return KLEUR.get(n, (170, 60, 170))


def boven(park, plekken, pad):
    W = kit.W
    img = np.zeros((W, W, 3), np.float32)
    v = park.v
    # top non-air per column
    nz = v != 0
    ys = kit.H - 1 - np.argmax(nz[:, ::-1, :], axis=1)
    ids = v[np.arange(W)[:, None], ys, np.arange(W)[None, :]]
    lut = np.array([_kleur(s) for s in park.pal], np.float32)
    img = lut[ids]
    # hillshade from the top height
    gx = np.gradient(ys.astype(float), axis=0)
    gz = np.gradient(ys.astype(float), axis=1)
    shade = np.clip(1 - (gx + gz) * 0.08, 0.6, 1.3)
    img *= shade[..., None]
    img[~park.binnen] *= 0.8
    img = np.clip(img, 0, 255).astype(np.uint8).transpose(1, 0, 2)       # [z, x] for the image
    im = Image.fromarray(img).resize((W * 2, W * 2), Image.NEAREST)
    d = ImageDraw.Draw(im)
    for p in plekken:
        x, _, z = p["centrum"]
        d.ellipse([x * 2 - 4, z * 2 - 4, x * 2 + 4, z * 2 + 4], fill=(255, 220, 60), outline=(40, 20, 0))
        d.text((x * 2 + 6, z * 2 - 6), p["soort"], fill=(255, 255, 255), stroke_width=2, stroke_fill=(30, 20, 10))
    ix, iy, iz, _ = park.ingang
    d.rectangle([ix * 2 - 6, iz * 2 - 6, ix * 2 + 6, iz * 2 + 6], outline=(255, 0, 0), width=3)
    im.save(pad)


def iso(park, naam, cx, cz, r=48, ss=1, yaw=35, pitch=32, scale=6):
    sys.path.insert(0, os.path.join(ROOT, "base-ontwerpen", "vibes", "_bron"))
    import vibekit as vk
    x0, x1 = max(cx - r, 0), min(cx + r, kit.W)
    z0, z1 = max(cz - r, 0), min(cz + r, kit.W)
    blok = park.v[x0:x1, :, z0:z1]
    ys = np.nonzero(blok.any(axis=(0, 2)))[0]
    ylo = max(int(park.hoogte[x0:x1, z0:z1].min()) - 4, int(ys.min()))
    yhi = int(ys.max()) + 1
    S = vk.Scene(x1 - x0, yhi - ylo + 1, z1 - z0)
    sub = blok[:, ylo:yhi + 1, :]
    gebruikt = np.unique(sub)
    m = {int(i): S.id(park.pal[int(i)]) if i else 0 for i in gebruikt}
    lut = np.zeros(int(gebruikt.max()) + 1, np.uint16)
    for i, j in m.items():
        lut[i] = j
    S.v[...] = lut[sub]
    rel = os.path.relpath(os.path.join(OUT, naam), vk.OUT)
    vk.render(S, rel, yaw=yaw, pitch=pitch, scale=scale, ss=ss)
    return os.path.join(OUT, naam + ".png")


SPOTS = [   # name, park coords (plattegrond C) -> world
    ("park_iso_entree", 90, 450), ("park_iso_savanne_delta", 400, 400), ("park_iso_ijskust", 110, 80),
    ("park_iso_bamboe", 440, 70),
]


def alles(park, plekken, log=print, iso_ook=True):
    os.makedirs(OUT, exist_ok=True)
    boven(park, plekken, os.path.join(OUT, "park_boven.png"))
    log(f"  {os.path.join(OUT, 'park_boven.png')}")
    if iso_ook:
        for naam, px, pz in SPOTS:
            p = iso(park, naam, px + kit.PARK_OFFSET, pz + kit.PARK_OFFSET)
            log(f"  {p}")
