"""
Het Ballonfestival (2.8, slice "buiten") - the guh_luchtballon's GeckoLib model, animation and textures: a hot-air
balloon shaped like a big guh head (a round-ish envelope of stacked blocks, two round guh ears with pink insides, the
guh face on its front), ropes down to a wicker basket with a burner and a flame. Four colours (roze, mint, lavendel,
citroen), each with its own stripes. The model's front (-z) is where it flies.
Every face samples its own region of a 256 x 256 sheet (per-face uv): the face region is painted with a big guh face.
"""
import math
import random

import numpy as np
from PIL import Image

from features import sterrenwacht_hulp as hulp

TEX = 256
KLEUREN = {  # name: (main, stripe, light)
    "roze": ((246, 150, 196), (255, 244, 248), (255, 206, 226)),
    "mint": ((150, 226, 196), (250, 255, 252), (206, 246, 228)),
    "lavendel": ((196, 170, 238), (255, 248, 236), (226, 212, 250)),
    "citroen": ((250, 222, 110), (255, 252, 240), (255, 240, 180)),
}
# regions of the sheet: name -> (u, v, w, h)
REGIO = {
    "gezicht": (0, 0, 96, 72),
    "stof": (96, 0, 64, 64),
    "stof2": (160, 0, 64, 64),
    "top": (96, 64, 64, 32),
    "oor": (0, 72, 32, 32),
    "oor_in": (32, 72, 32, 32),
    "mand": (64, 104, 48, 32),
    "mand_bodem": (112, 104, 32, 32),
    "touw": (144, 104, 8, 32),
    "metaal": (152, 104, 16, 16),
    "vlam": (168, 104, 16, 16),
    "rand": (184, 104, 48, 8),
}


def uv(name, faces=None, override=None):
    u, v, w, h = REGIO[name]
    out = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        r = (override or {}).get(f, name)
        uu, vv, ww, hh = REGIO[r]
        out[f] = {"uv": [uu, vv], "uv_size": [ww, hh]}
    return out


def cube(origin, size, name, override=None, inflate=0.0):
    c = {"origin": origin, "size": size, "uv": uv(name, override=override)}
    if inflate:
        c["inflate"] = inflate
    return c


def geo():
    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    mand = [cube([-12, 0, -12], [24, 2, 24], "mand_bodem"),
            cube([-12, 2, -12], [24, 13, 2], "mand"), cube([-12, 2, 10], [24, 13, 2], "mand"),
            cube([-12, 2, -10], [2, 13, 20], "mand"), cube([10, 2, -10], [2, 13, 20], "mand"),
            cube([-13, 15, -13], [26, 2, 3], "rand"), cube([-13, 15, 10], [26, 2, 3], "rand"),
            cube([-13, 15, -10], [3, 2, 20], "rand"), cube([10, 15, -10], [3, 2, 20], "rand")]
    bones.append({"name": "mand", "parent": "root", "pivot": [0, 0, 0], "cubes": mand})
    touwen = []
    for sx in (-1, 1):
        for sz in (-1, 1):
            touwen.append(cube([sx * 11 - 0.5, 16, sz * 11 - 0.5], [1, 34, 1], "touw"))
    bones.append({"name": "touwen", "parent": "root", "pivot": [0, 16, 0], "cubes": touwen})
    # (the burner hangs high enough above the basket for a guh's head, the envelope above it)
    bones.append({"name": "brander", "parent": "root", "pivot": [0, 38, 0], "cubes": [
        cube([-3, 38, -3], [6, 4, 6], "metaal"), cube([-5, 37, -0.5], [10, 1, 1], "metaal"), cube([-0.5, 37, -5], [1, 1, 10], "metaal")]})
    bones.append({"name": "vlam", "parent": "brander", "pivot": [0, 42, 0], "cubes": [cube([-2, 42, -2], [4, 6, 4], "vlam")]})
    env = [cube([-8, 48, -8], [16, 6, 16], "stof"),
           cube([-20, 54, -20], [40, 8, 40], "stof", override={"up": "top", "down": "stof2"}),
           cube([-30, 62, -30], [60, 40, 60], "stof", override={"north": "gezicht"}),
           cube([-26, 102, -26], [52, 8, 52], "stof2", override={"up": "top"}),
           cube([-18, 110, -18], [36, 6, 36], "top")]
    bones.append({"name": "ballon", "parent": "root", "pivot": [0, 48, 0], "cubes": env})
    for side, sx in (("links", -1), ("rechts", 1)):
        cx = sx * 21
        bones.append({"name": f"oor_{side}", "parent": "ballon", "pivot": [cx, 112, 0], "cubes": [
            cube([cx - 7, 110, -3], [14, 16, 6], "oor"),
            cube([cx - 9, 113, -3], [18, 10, 6], "oor"),
            cube([cx - 5, 126, -3], [10, 2, 6], "oor"),
            cube([cx - 5, 113, -3.6], [10, 10, 0.6], "oor_in")]})
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.guh_luchtballon", "texture_width": TEX, "texture_height": TEX,
                        "visible_bounds_width": 5, "visible_bounds_height": 10, "visible_bounds_offset": [0, 4.5, 0]},
        "bones": bones}]}


def animation():
    return {"format_version": "1.8.0", "animations": {"animation.guh_luchtballon.zweven": {"loop": True, "animation_length": 4.0, "bones": {
        "ballon": {"rotation": {"0.0": [0, 0, -1.2], "2.0": [0, 0, 1.2], "4.0": [0, 0, -1.2]}},
        "oor_links": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 5], "2.0": [0, 0, 0], "3.0": [0, 0, 3], "4.0": [0, 0, 0]}},
        "oor_rechts": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, -3], "2.0": [0, 0, 0], "3.0": [0, 0, -5], "4.0": [0, 0, 0]}},
        "vlam": {"scale": {"0.0": [1, 1, 1], "0.3": [1.1, 1.3, 1.1], "0.6": [0.9, 0.8, 0.9], "1.0": [1.05, 1.2, 1.05], "1.4": [1, 0.9, 1],
                           "2.0": [1.1, 1.35, 1.1], "2.5": [0.95, 0.85, 0.95], "3.2": [1.05, 1.2, 1.05], "4.0": [1, 1, 1]}}}}}}


def fill(a, name, painter):
    u, v, w, h = REGIO[name]
    block = painter(w, h)
    a[v:v + h, u:u + w] = block


def textures(rng_seed=28701):
    out = {}
    for kleur, (main, stripe, light) in KLEUREN.items():
        rng = np.random.default_rng(rng_seed + len(out))
        a = np.zeros((TEX, TEX, 4), np.uint8)

        def fabric(base, var=6):
            def p(w, h):
                b = np.zeros((h, w, 4), np.uint8)
                b[..., :3] = np.clip(np.array(base) + rng.normal(0, var, (h, w, 1)), 0, 255)
                b[..., 3] = 255
                return b
            return p

        def stripes(w, h):
            b = fabric(main)(w, h)
            for x in range(w):
                if (x // 8) % 2 == 1:
                    b[:, x, :3] = np.clip(np.array(stripe) + rng.normal(0, 4, (h, 1)), 0, 255)
            b[:2, :, :3] = np.array(main) * 0.85
            return b

        def stripes_diag(w, h):
            b = fabric(main)(w, h)
            for y in range(h):
                for x in range(w):
                    if ((x + y) // 8) % 2 == 1:
                        b[y, x, :3] = stripe
            return b

        def face(w, h):
            b = stripes(w, h)
            # a lighter oval of guh fur in the middle, with the face on it
            for y in range(h):
                for x in range(w):
                    if ((x - w / 2 + 0.5) / (w * 0.42)) ** 2 + ((y - h / 2 + 0.5) / (h * 0.46)) ** 2 <= 1:
                        b[y, x, :3] = np.clip(np.array(light) + rng.normal(0, 4), 0, 255)
            img = Image.fromarray(b).copy()
            hulp.paint_face(img, (w // 2 - 40, h // 2 - 30, 80, 60), 0)
            return np.asarray(img).copy()

        fill(a, "gezicht", face)
        fill(a, "stof", stripes)
        fill(a, "stof2", stripes_diag)
        fill(a, "top", fabric(main, 8))
        fill(a, "oor", fabric(main, 8))
        fill(a, "oor_in", fabric((236, 110, 168), 6))

        def wicker(w, h):
            b = fabric((176, 124, 70), 8)(w, h)
            for y in range(h):
                for x in range(w):
                    if (y // 3 + x // 4) % 2 == 0:
                        b[y, x, :3] = np.clip(b[y, x, :3].astype(int) - 30, 0, 255)
            b[:2, :, :3] = (120, 80, 45)
            b[-2:, :, :3] = (120, 80, 45)
            return b
        fill(a, "mand", wicker)
        fill(a, "mand_bodem", fabric((140, 96, 54), 8))
        fill(a, "touw", fabric((226, 206, 160), 6))
        fill(a, "metaal", fabric((110, 110, 124), 10))

        def vlam(w, h):
            b = np.zeros((h, w, 4), np.uint8)
            for y in range(h):
                for x in range(w):
                    t = y / h
                    b[y, x, :3] = (255, int(200 - 120 * (1 - t)), int(60 * t))
                    b[y, x, 3] = 255
            return b
        fill(a, "vlam", vlam)
        fill(a, "rand", fabric((120, 80, 45), 6))
        out[kleur] = Image.fromarray(a)
    return out


def build(h):
    h.w(f"{h.A}/geckolib/models/entity/guh_luchtballon.geo.json", geo())
    h.w(f"{h.A}/geckolib/animations/entity/guh_luchtballon.animation.json", animation())
    for kleur, img in textures().items():
        h.save(img, "entity", f"guh_luchtballon_{kleur}.png")
    # GeckoLib's DefaultedEntityGeoModel also looks for textures/entity/guh_luchtballon.png (the renderer picks the colour)
    h.save(textures()["roze"], "entity", "guh_luchtballon.png")
