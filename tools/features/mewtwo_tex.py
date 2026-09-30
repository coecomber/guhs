"""
3.0 (Guhverhalen), slice mewtwo: textures and models (called from mewtwo.build):
  - the Guhtwo: guh_mewtwo.png (the lilac-grey fur, a purple belly and chest, purple eyes, the tail/tube swatches of
    make_guh_variants), guh_mewtwo_gloed.png (the glow layer: the eyes and the tail's bulb), guh_slaap/guh_mewtwo.png
  - Mieuwguh: geo/entity/mew.geo.json, textures/entity/mew.png, animations/entity/mew.animation.json
  - Professor Knabbelkloon: geo/entity/guh_npc_knabbelkloon.geo.json + textures/entity/npc_knabbelkloon.png (the sitting guh
    with a crooked little pair of glasses, a lab coat with a pink knabbelsap stain, a messy white tuft and a clipboard)
  - the blocks (kloontank base + the textures its renderer draws, tankwand, notitieplek, onderdelenkist, knabbelschaal,
    computer, reageerbuisjes, papieren), the items (labnotitie, the 4 tankonderdelen, the spawn egg), the particles.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw

from features import knuffeldal_npcs as npcs

PX = 4                      # guh texture pixels per UV unit
EYE_BLUE_MIN = 90           # (a pixel is "eye blue" when blue is well above red)
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
PAARS = (128, 86, 168)
BUIK = (150, 104, 190)
LILA = (224, 216, 234)


def rgba(c, a=255):
    return tuple(int(v) for v in c[:3]) + (a,)


def noisy(base, var, seed, size=16):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            v = rng.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base[:3]) + (255,))
    return img


# =====================================================================================================================
# the Guhtwo's look
# =====================================================================================================================
def _faces(geo, bone):
    out = []
    for b in geo["bones"]:
        if b["name"] != bone:
            continue
        for cube in b.get("cubes", []):
            rects = {"_origin": cube["origin"], "_size": cube["size"]}
            for face, f in cube.get("uv", {}).items():
                (u, v), (w, hh) = f["uv"], f["uv_size"]
                x0, x1 = sorted((u, u + w))
                y0, y1 = sorted((v, v + hh))
                rects[face] = (int(round(x0 * PX)), int(round(y0 * PX)), int(round(x1 * PX)), int(round(y1 * PX)))
            out.append(rects)
    return out


def mewtwo_textures(h):
    import make_guh_variants as mgv
    from features import guhpolder_tex as gt
    ent = os.path.join(h.TEX, "entity")
    geo = json.load(open(os.path.join(h.A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]
    base = Image.open(os.path.join(ent, "guh.png")).convert("RGBA")
    p = gt.Painter(base, LILA)
    # the belly and the chest: purple (Guhtwo's own), the cheeks stay soft lilac
    for cube in _faces(geo, "body"):
        p.rect(cube["down"], BUIK)
        p.rect(cube["north"], BUIK, rows=(0.25, 1.0))
        for side in ("east", "west"):
            p.rect(cube[side], BUIK, rows=(0.72, 1.0))
    # the paws a little darker, the ears' inside pale purple
    for leg in ("leg_back_left", "leg_back_right", "leg_front_left", "leg_front_right"):
        for cube in _faces(geo, leg):
            for k, f in cube.items():
                if not k.startswith("_"):
                    p.rect(f, (196, 184, 214))
    for ear in ("ear_left", "ear_right"):
        for cube in _faces(geo, ear):
            p.rect(cube["north"], (190, 150, 214))
    a = p.a.astype(np.int32)
    # the eyes: blue irises become deep purple (with the shine kept)
    fx0, fy0, fx1, fy1 = 0, 49 * PX, 14 * PX, 60 * PX
    reg = a[fy0:fy1, fx0:fx1]
    blue = (reg[..., 2] > EYE_BLUE_MIN) & (reg[..., 2] - reg[..., 0] > 50) & (reg[..., 3] > 0)
    lum = reg[..., :3].mean(-1, keepdims=True) / 160.0
    purple = np.clip(np.array((150, 70, 214)) * lum, 0, 255)
    reg[..., :3] = np.where(blue[..., None], purple, reg[..., :3])
    a[fy0:fy1, fx0:fx1] = reg
    # the variant swatches (tail, bulb, tube) from make_guh_variants' own painting of guh_mewtwo.png, when it's there
    old_path = os.path.join(ent, "guh_mewtwo.png")
    staart = np.zeros(a.shape[:2], bool)
    if os.path.exists(old_path):
        old = np.asarray(Image.open(old_path).convert("RGBA")).astype(np.int32)
        for bone in ("mewtwo_staart", "mewtwo_bolletje", "mewtwo_buisje"):
            for cube in _faces(geo, bone):
                for k, (x0, y0, x1, y1) in ((k, f) for k, f in cube.items() if not k.startswith("_")):
                    a[y0:y1, x0:x1] = old[y0:y1, x0:x1]
                    if bone == "mewtwo_bolletje":
                        staart[y0:y1, x0:x1] = True
    img = Image.fromarray(a.astype(np.uint8))
    h.save(img, "entity", "guh_mewtwo.png")
    # the glow layer: the purple eyes and the tail's bulb
    glow = np.zeros_like(a)
    eyes = np.zeros(a.shape[:2], bool)
    eyes[fy0:fy1, fx0:fx1] = blue
    glow[eyes] = (190, 110, 255, 255)
    glow[staart & (a[..., 3] > 0)] = (170, 110, 230, 150)
    h.save(Image.fromarray(glow.astype(np.uint8)), "entity", "guh_mewtwo_gloed.png")
    # sleeping: the eyes closed (make_sleep_eyes' own recipe on our texture), and a glow without the eyes
    import make_sleep_eyes as mse
    os.makedirs(os.path.join(ent, "guh_slaap"), exist_ok=True)
    mse.sleepy("guh_mewtwo")
    glow[eyes] = (0, 0, 0, 0)
    h.save(Image.fromarray(glow.astype(np.uint8)), "entity", "guh_mewtwo_gloed_slaap.png")
    # the soft glow on the ground under a floating Guhtwo (drawn additively: black = nothing)
    zg = Image.new("RGBA", (32, 32), (0, 0, 0, 255))
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - 15.5, y - 15.5) / 15.5
            t = max(0.0, 1 - d) ** 1.6
            zg.putpixel((x, y), (int(170 * t), int(90 * t), int(235 * t), 255))
    h.save(zg, "entity", "mewtwo_zweefgloed.png")


# =====================================================================================================================
# a little geo helper (per-face uv, one texture): Mieuwguh
# =====================================================================================================================
class Atlas:
    """Hands out rectangles in a texture (shelf packing), 2 texture pixels per model unit."""

    def __init__(self, size=128, scale=2):
        self.size, self.scale = size, scale
        self.x = self.y = self.row = 1
        self.rects = []                # (x0, y0, x1, y1, colour key, face, cube name)

    def take(self, w, hh):
        w, hh = max(1, math.ceil(w * self.scale)), max(1, math.ceil(hh * self.scale))
        if self.x + w + 1 > self.size:
            self.x, self.y = 1, self.y + self.row + 1
            self.row = 0
        r = (self.x, self.y, self.x + w, self.y + hh)
        self.x += w + 1
        self.row = max(self.row, hh)
        assert self.y + hh < self.size, "Mieuwguh atlas full"
        return r

    def cube(self, name, origin, size, kleur, faces_kleur=None, inflate=0.0):
        w, hh, d = size
        spec = {"north": (w, hh), "south": (w, hh), "east": (d, hh), "west": (d, hh), "up": (w, d), "down": (w, d)}
        uv = {}
        for face, (fw, fh) in spec.items():
            r = self.take(fw, fh)
            self.rects.append(r + ((faces_kleur or {}).get(face, kleur), face, name))
            s = self.scale
            if face in ("up", "down"):
                uv[face] = {"uv": [r[0] / 1, r[1] / 1], "uv_size": [r[2] - r[0], r[3] - r[1]]}
            else:
                uv[face] = {"uv": [r[0], r[1]], "uv_size": [r[2] - r[0], r[3] - r[1]]}
        c = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": uv}
        if inflate:
            c["inflate"] = inflate
        return c


MEW = {"roze": (250, 178, 210), "licht": (255, 206, 226), "oor": (255, 150, 190), "wang": (255, 128, 170), "oog": (70, 130, 222),
       "donker": (60, 40, 70), "snoet": (255, 190, 214)}


def mew_model(h):
    at = Atlas(128, 2)
    bones = []

    def bone(name, parent, pivot, cubes, rotation=None):
        b = {"name": name, "pivot": pivot, "cubes": cubes}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = rotation
        bones.append(b)

    bone("root", None, [0, 0, 0], [])
    bone("body", "root", [0, 4.5, 0], [at.cube("body", [-2.5, 2.0, -2.0], [5, 5, 5], "roze", {"north": "buik", "down": "licht"})])
    bone("head", "body", [0, 7, -0.5], [at.cube("head", [-4, 6.6, -4.5], [8, 7, 7], "roze", {"north": "gezicht"}),
                                        at.cube("snoet", [-1.5, 7.2, -5.3], [3, 2, 1], "snoet", {"north": "snoetje"})])
    bone("ear_left", "head", [3.2, 12.6, -1.5], [at.cube("oor_l", [2.2, 12.2, -2.1], [3.4, 2.8, 0.9], "roze", {"north": "oor"})],
         rotation=[0, 0, -18])
    bone("ear_right", "head", [-3.2, 12.6, -1.5], [at.cube("oor_r", [-5.6, 12.2, -2.1], [3.4, 2.8, 0.9], "roze", {"north": "oor"})],
         rotation=[0, 0, 18])
    bone("arm_left", "body", [2.4, 5.5, -1.2], [at.cube("arm_l", [2.2, 3.6, -2.0], [1.4, 2.2, 1.4], "roze")])
    bone("arm_right", "body", [-2.4, 5.5, -1.2], [at.cube("arm_r", [-3.6, 3.6, -2.0], [1.4, 2.2, 1.4], "roze")])
    bone("leg_left", "body", [1.4, 2.6, 0.5], [at.cube("voet_l", [0.6, 0.8, -0.8], [1.8, 1.6, 2.8], "roze", {"down": "licht"})])
    bone("leg_right", "body", [-1.4, 2.6, 0.5], [at.cube("voet_r", [-2.4, 0.8, -0.8], [1.8, 1.6, 2.8], "roze", {"down": "licht"})])
    bone("tail", "body", [0, 4.0, 2.8], [at.cube("staart1", [-0.5, 3.5, 2.8], [1.0, 1.0, 4.0], "roze")], rotation=[-20, 0, 0])
    bone("tail2", "tail", [0, 4.0, 6.6], [at.cube("staart2", [-0.4, 3.6, 6.6], [0.8, 0.8, 4.0], "roze")], rotation=[-25, 0, 0])
    bone("tail3", "tail2", [0, 4.0, 10.4], [at.cube("staart3", [-0.4, 3.6, 10.4], [0.8, 0.8, 3.4], "roze")], rotation=[30, 0, 0])
    bone("tail_bol", "tail3", [0, 4.0, 13.6], [at.cube("bol", [-1.1, 3.2, 13.6], [2.2, 1.6, 3.2], "roze", {"north": "licht"})])
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.mew", "texture_width": 128, "texture_height": 128, "visible_bounds_width": 2,
                        "visible_bounds_height": 1.5, "visible_bounds_offset": [0, 0.5, 0]},
        "bones": bones}]}
    h.w(os.path.join(h.A, "geckolib", "models", "entity", "mew.geo.json"), geo)
    # the texture
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rng = random.Random(20300911)
    for (x0, y0, x1, y1, key, face, name) in at.rects:
        base = {"roze": MEW["roze"], "buik": MEW["licht"], "licht": MEW["licht"], "gezicht": MEW["roze"], "snoet": MEW["snoet"],
                "snoetje": MEW["snoet"], "oor": MEW["roze"]}[key]
        for y in range(y0, y1):
            for x in range(x0, x1):
                v = rng.randint(-6, 6)
                img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base) + (255,))
        w, hh = x1 - x0, y1 - y0
        if key == "gezicht":
            # two big blue eyes with a white shine and a dark pupil, pink cheeks
            for cx in (x0 + w * 0.28, x0 + w * 0.72):
                cy = y0 + hh * 0.42
                d.ellipse([cx - 1.9, cy - 2.4, cx + 1.4, cy + 2.4], fill=rgba(MEW["donker"]))
                d.ellipse([cx - 1.5, cy - 1.8, cx + 1.0, cy + 2.0], fill=rgba(MEW["oog"]))
                d.ellipse([cx - 0.8, cy - 0.2, cx + 0.5, cy + 1.4], fill=rgba((30, 50, 110)))
                d.rectangle([cx - 1.2, cy - 1.6, cx - 0.4, cy - 0.8], fill=rgba((255, 255, 255)))
            for cx in (x0 + w * 0.12, x0 + w * 0.88):
                d.ellipse([cx - 1.4, y0 + hh * 0.72 - 0.8, cx + 1.4, y0 + hh * 0.72 + 0.8], fill=rgba(MEW["wang"], 220))
        elif key == "snoetje":
            d.rectangle([x0 + w * 0.4, y0, x0 + w * 0.6, y0 + 1], fill=rgba((230, 110, 150)))
            d.arc([x0 + 1, y0 + hh * 0.3, x1 - 2, y1 - 1], 20, 160, fill=rgba(MEW["donker"]), width=1)
        elif key == "oor":
            d.rectangle([x0 + 1, y0 + 1, x1 - 2, y1 - 2], fill=rgba(MEW["oor"]))
        elif key == "buik":
            d.ellipse([x0 + 1, y0 + 1, x1 - 2, y1 - 1], fill=rgba((255, 222, 236)))
    h.save(img, "entity", "mew.png")
    # the animations: float (bob, paddling arms, a wavy tail), giggle (a little somersault)
    anim = {"format_version": "1.8.0", "animations": {
        "animation.mew.zweef": {"loop": True, "animation_length": 2.4, "bones": {
            "root": {"position": {"0.0": [0, 0, 0], "1.2": [0, 1.4, 0], "2.4": [0, 0, 0]}},
            "tail": {"rotation": {"0.0": [0, -12, 0], "1.2": [0, 12, 0], "2.4": [0, -12, 0]}},
            "tail2": {"rotation": {"0.0": [0, 15, 0], "1.2": [0, -15, 0], "2.4": [0, 15, 0]}},
            "tail3": {"rotation": {"0.0": [8, -10, 0], "1.2": [-8, 10, 0], "2.4": [8, -10, 0]}},
            "arm_left": {"rotation": {"0.0": [-20, 0, -10], "0.6": [20, 0, -20], "1.2": [-20, 0, -10], "1.8": [20, 0, -20], "2.4": [-20, 0, -10]}},
            "arm_right": {"rotation": {"0.0": [20, 0, 10], "0.6": [-20, 0, 20], "1.2": [20, 0, 10], "1.8": [-20, 0, 20], "2.4": [20, 0, 10]}},
            "leg_left": {"rotation": {"0.0": [25, 0, 0], "1.2": [35, 0, 0], "2.4": [25, 0, 0]}},
            "leg_right": {"rotation": {"0.0": [35, 0, 0], "1.2": [25, 0, 0], "2.4": [35, 0, 0]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "2.0": [0, 0, 0], "2.1": [0, 0, -12], "2.2": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "2.0": [0, 0, 0], "2.1": [0, 0, 12], "2.2": [0, 0, 0]}}}},
        "animation.mew.giechel": {"loop": False, "animation_length": 1.2, "bones": {
            "root": {"rotation": {"0.0": [0, 0, 0], "0.6": [-180, 0, 0], "1.2": [-360, 0, 0]},
                     "position": {"0.0": [0, 0, 0], "0.6": [0, 4, 0], "1.2": [0, 0, 0]}},
            "arm_left": {"rotation": {"0.0": [0, 0, 0], "0.3": [0, 0, -70], "0.9": [0, 0, -70], "1.2": [0, 0, 0]}},
            "arm_right": {"rotation": {"0.0": [0, 0, 0], "0.3": [0, 0, 70], "0.9": [0, 0, 70], "1.2": [0, 0, 0]}}}},
        "animation.mew.zwaai": {"loop": False, "animation_length": 1.0, "bones": {
            "arm_right": {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, 120], "0.4": [0, 0, 90], "0.6": [0, 0, 120], "0.8": [0, 0, 90],
                                       "1.0": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 0, -10], "1.0": [0, 0, 0]}}}},
    }}
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "mew.animation.json"), anim)


# =====================================================================================================================
# Professor Knabbelkloon (the sitting guh + glasses, lab coat, tuft, clipboard)
# =====================================================================================================================
def knabbelkloon(h):
    geo_file = npcs._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_knabbelkloon"
    sw = npcs._swatches(geo, ["jas", "glas", "haar", "klembord", "papier", "vlek", "pen"])
    H = [0, 13, 0]
    B = [0, 12, -4]
    c = npcs._cube
    # the glasses: two round-ish lenses (rims only), the left one a bit higher: crooked! A bridge and two arms to the ears
    geo["bones"].append({"name": "kloon_bril", "parent": "head", "pivot": H, "cubes": [
        c([0.6, 18.4, -7.7], [5.6, 4.8, 0.4], sw["glas"]), c([-6.2, 17.6, -7.7], [5.6, 4.8, 0.4], sw["glas"]),
        c([-0.7, 20.4, -7.8], [1.4, 0.5, 0.3], sw["pen"]),
        c([6.1, 20.9, -7.4], [0.4, 0.5, 4.4], sw["pen"]), c([-6.5, 20.1, -7.4], [0.4, 0.5, 4.4], sw["pen"])]})
    # a messy white tuft of hair (a professor's!)
    geo["bones"].append({"name": "kloon_haar", "parent": "head", "pivot": H, "cubes": [
        c([-2.4, 25.4, -3.2], [2.2, 1.8, 2.4], sw["haar"]), c([-0.6, 25.6, -1.8], [2.6, 2.4, 2.4], sw["haar"]),
        c([1.6, 25.3, -3.0], [1.8, 1.5, 2.0], sw["haar"]), c([-1.2, 27.2, -2.2], [1.4, 1.2, 1.6], sw["haar"]),
        c([6.2, 20.5, -2.0], [1.4, 2.6, 2.6], sw["haar"]), c([-7.6, 20.2, -2.0], [1.4, 2.8, 2.6], sw["haar"])]})
    # the lab coat: over the front and the sides of the body, a collar, a pocket with pens, a pink knabbelsap stain
    geo["bones"].append({"name": "kloon_jas", "parent": "body", "pivot": B, "cubes": [
        c([-5.4, 1.8, -4.6], [10.8, 10.4, 1.0], sw["jas"]), c([-5.6, 1.6, -4.0], [0.6, 10.2, 7.6], sw["jas"]),
        c([5.0, 1.6, -4.0], [0.6, 10.2, 7.6], sw["jas"]), c([-3.4, 11.6, -4.9], [6.8, 1.2, 0.6], sw["jas"]),
        c([1.4, 6.4, -4.9], [2.6, 2.4, 0.4], sw["jas"]), c([1.8, 8.4, -5.0], [0.5, 1.4, 0.4], sw["pen"]),
        c([2.8, 8.2, -5.0], [0.5, 1.6, 0.4], sw["vlek"]), c([-3.8, 3.2, -4.9], [2.2, 1.8, 0.3], sw["vlek"])]})
    # a clipboard held against the tummy
    geo["bones"].append({"name": "kloon_klembord", "parent": "body", "pivot": B, "cubes": [
        c([-2.4, 3.0, -6.4], [4.8, 5.6, 0.5], sw["klembord"]), c([-2.0, 3.4, -6.6], [4.0, 4.6, 0.3], sw["papier"]),
        c([-0.8, 8.4, -6.6], [1.6, 0.6, 0.4], sw["pen"])]})
    npcs._save_geo(h, "guh_npc_knabbelkloon.geo.json", geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.86, sat=0.42, val=1.02, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(20300921)

    def rim(block):
        block[..., :3] = (40, 34, 52)
        block[..., 3] = 255
        block[3:29, 3:29, 3] = 0                 # see-through lenses
        block[6:10, 6:12, :3] = (240, 240, 255)  # a glint on the rim
        block[6:10, 6:12, 3] = 180

    def papier(block):
        block[..., :3] = (250, 250, 246)
        for y in range(4, 30, 5):
            block[y, 3:29, :3] = (150, 150, 200)
        block[8:14, 6:14, :3] = (250, 206, 70)   # a drawn kaasknabbel
        block[20:24, 18:26, :3] = (190, 110, 220)

    npcs._paint(a, sw["jas"], (248, 248, 250), rng, 4)
    npcs._paint(a, sw["glas"], (40, 34, 52), rng, 2, rim)
    npcs._paint(a, sw["haar"], (246, 246, 250), rng, 10)
    npcs._paint(a, sw["klembord"], (160, 110, 64), rng, 8)
    npcs._paint(a, sw["papier"], (250, 250, 246), rng, 2, papier)
    npcs._paint(a, sw["vlek"], (246, 120, 180), rng, 12)
    npcs._paint(a, sw["pen"], (70, 60, 90), rng, 6)
    h.save(Image.fromarray(a), "entity", "npc_knabbelkloon.png")


# =====================================================================================================================
# blocks
# =====================================================================================================================
def _el(frm, to, tex_, faces=("down", "up", "north", "south", "west", "east"), uv=None, rot=None, face_uv=None):
    e = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": {}}
    for f in faces:
        spec = {"texture": tex_}
        u = (face_uv or {}).get(f, uv)
        if u:
            spec["uv"] = u
        e["faces"][f] = spec
    if rot:
        e["rotation"] = rot
    return e


def block_textures(h):
    save = h.save
    rng = random.Random(20300931)
    # the kloontank: the metal of its base and rings, the glass (whole / cracked), the pink liquid (animated), a bubble
    metaal = noisy((70, 64, 86), 6, 1)
    px = metaal.load()
    for x in (2, 13):
        for y in (2, 13):
            px[x, y] = (150, 140, 170, 255)
    for x in range(16):
        px[x, 0] = (110, 100, 130, 255)
        px[x, 15] = (40, 36, 50, 255)
    save(metaal, "block", "mewtwo_tankmetaal.png")
    glas = Image.new("RGBA", (16, 16), (220, 230, 255, 46))
    d = ImageDraw.Draw(glas)
    d.rectangle([0, 0, 15, 15], outline=(235, 240, 255, 110))
    d.line([(3, 2), (3, 13)], fill=(255, 255, 255, 120))
    d.line([(4, 2), (4, 6)], fill=(255, 255, 255, 90))
    save(glas, "block", "mewtwo_tankglas.png")
    barst = glas.copy()
    d = ImageDraw.Draw(barst)
    pts = [(8, 0), (7, 3), (9, 5), (6, 8), (8, 10), (5, 13), (6, 15)]
    d.line(pts, fill=(255, 255, 255, 220), width=1)
    d.line([(9, 5), (13, 6), (15, 4)], fill=(255, 255, 255, 200))
    d.line([(6, 8), (2, 9), (0, 11)], fill=(255, 255, 255, 200))
    d.polygon([(8, 10), (11, 11), (10, 13)], fill=(0, 0, 0, 0))
    d.line([(8, 10), (11, 11), (10, 13), (8, 10)], fill=(255, 255, 255, 230))
    save(barst, "block", "mewtwo_tankglas_barst.png")
    frames = []
    for f in range(8):
        fr = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                t = math.sin((x + f * 2) * 0.7) * math.cos((y - f) * 0.5)
                c = (246 + int(8 * t), 120 + int(24 * t), 190 + int(14 * t))
                fr.putpixel((x, y), tuple(max(0, min(255, v)) for v in c) + (200,))
        for k in range(3):
            bx, by = (3 + k * 5 + f) % 16, (15 - (f * 2 + k * 5)) % 16
            fr.putpixel((bx, by), (255, 230, 245, 230))
        frames.append(fr)
    strip = Image.new("RGBA", (16, 16 * len(frames)))
    for i, fr in enumerate(frames):
        strip.paste(fr, (0, 16 * i))
    save(strip, "block", "mewtwo_kloonvloeistof.png")
    h.w(os.path.join(h.TEX, "block", "mewtwo_kloonvloeistof.png.mcmeta"), {"animation": {"frametime": 3, "interpolate": True}})
    # a light for the tank's indicator lamps (red: missing, green: in), used by its renderer
    for naam, kleur in (("mewtwo_lampje_rood", (240, 70, 90)), ("mewtwo_lampje_groen", (90, 230, 120))):
        im = Image.new("RGBA", (16, 16), rgba(kleur))
        ImageDraw.Draw(im).rectangle([4, 4, 7, 7], fill=(255, 255, 255, 255))
        save(im, "block", naam + ".png")
    # papers and a pencil
    papier = Image.new("RGBA", (16, 16), (250, 250, 244, 255))
    d = ImageDraw.Draw(papier)
    for y in range(3, 15, 3):
        d.line([(2, y), (13 - (y % 4), y)], fill=(150, 150, 200, 255))
    d.rectangle([9, 9, 12, 12], fill=(250, 206, 70, 255))
    d.point([(10, 10), (11, 11)], fill=(236, 150, 50, 255))
    save(papier, "block", "mewtwo_papier.png")
    potlood = Image.new("RGBA", (16, 16), (250, 200, 60, 255))
    d = ImageDraw.Draw(potlood)
    d.rectangle([0, 0, 3, 15], fill=(240, 150, 190, 255))
    d.rectangle([13, 0, 15, 15], fill=(60, 50, 60, 255))
    save(potlood, "block", "mewtwo_potlood.png")
    clip = Image.new("RGBA", (16, 16), (150, 90, 210, 255))
    save(clip, "block", "mewtwo_paperclip.png")
    # messy papers on the floor (see-through between the sheets)
    rommel = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(rommel)
    r2 = random.Random(7)
    for _ in range(6):
        cx, cy = r2.randint(4, 27), r2.randint(4, 27)
        a0 = r2.uniform(0, math.pi)
        pts = [(cx + math.cos(a0 + k * math.pi / 2 + (0.3 if k % 2 else 0)) * 7, cy + math.sin(a0 + k * math.pi / 2 + (0.3 if k % 2 else 0)) * 6)
               for k in range(4)]
        d.polygon(pts, fill=(250, 250, 244, 255), outline=(200, 200, 210, 255))
        d.line([(cx - 3, cy - 1), (cx + 3, cy - 1)], fill=(150, 150, 200, 255))
        d.line([(cx - 3, cy + 1), (cx + 2, cy + 1)], fill=(150, 150, 200, 255))
    save(rommel, "block", "mewtwo_papieren.png")
    # the parts crate: pale wood with a purple band and a little guh logo; labels per part
    kist = noisy((226, 196, 150), 8, 3)
    d = ImageDraw.Draw(kist)
    for y in (0, 5, 10, 15):
        d.line([(0, y), (15, y)], fill=(186, 150, 100, 255))
    d.rectangle([0, 6, 15, 9], fill=(130, 90, 180, 255))
    save(kist, "block", "mewtwo_kist_zijkant.png")
    top = noisy((230, 204, 160), 8, 4)
    d = ImageDraw.Draw(top)
    d.rectangle([0, 0, 15, 15], outline=(160, 120, 80, 255))
    d.line([(0, 0), (15, 15)], fill=(186, 150, 100, 255))
    d.line([(15, 0), (0, 15)], fill=(186, 150, 100, 255))
    save(top, "block", "mewtwo_kist_top.png")
    for n, kleur in ((1, (170, 220, 250)), (2, (214, 130, 70)), (3, (160, 160, 180)), (4, (246, 130, 190))):
        lab = Image.new("RGBA", (16, 16), (250, 250, 244, 255))
        d = ImageDraw.Draw(lab)
        d.rectangle([0, 0, 15, 15], outline=(120, 100, 90, 255))
        d.ellipse([4, 3, 11, 12], fill=rgba(kleur))
        d.point([(6, 6), (9, 6)], fill=(40, 30, 50, 255))
        save(lab, "block", f"mewtwo_kist_label_{n}.png")
    # the knabbelschaal: a big quartz bowl full of kaasknabbels
    schaal = noisy((236, 230, 226), 5, 5)
    d = ImageDraw.Draw(schaal)
    d.line([(0, 3), (15, 3)], fill=(190, 150, 214, 255))
    d.line([(0, 12), (15, 12)], fill=(190, 150, 214, 255))
    save(schaal, "block", "mewtwo_schaal.png")
    knab = Image.new("RGBA", (16, 16), (240, 190, 70, 255))
    r3 = random.Random(11)
    d = ImageDraw.Draw(knab)
    for _ in range(14):
        x, y = r3.randint(0, 14), r3.randint(0, 14)
        d.rectangle([x, y, x + 1, y + 1], fill=r3.choice([(250, 214, 100, 255), (220, 150, 50, 255), (255, 230, 140, 255)]))
    save(knab, "block", "mewtwo_schaal_knabbels.png")
    # the guh-computer: a lilac box with a glowing screen (a guh face and lines of text)
    kast = noisy((214, 204, 226), 5, 6)
    save(kast, "block", "mewtwo_computer_kast.png")
    scherm = Image.new("RGBA", (16, 16), (30, 24, 50, 255))
    d = ImageDraw.Draw(scherm)
    d.rectangle([1, 1, 14, 14], fill=(40, 60, 70, 255))
    d.ellipse([3, 3, 9, 8], fill=(120, 240, 170, 255))
    d.point([(5, 5), (7, 5)], fill=(30, 60, 50, 255))
    for y in (10, 12):
        d.line([(3, y), (12, y)], fill=(120, 240, 170, 255))
    d.line([(10, 4), (13, 4)], fill=(250, 150, 200, 255))
    d.line([(10, 6), (12, 6)], fill=(250, 150, 200, 255))
    save(scherm, "block", "mewtwo_computer_scherm.png")
    toetsen = noisy((80, 76, 96), 4, 8)
    d = ImageDraw.Draw(toetsen)
    for y in range(2, 14, 3):
        for x in range(1, 15, 3):
            d.rectangle([x, y, x + 1, y + 1], fill=(230, 226, 236, 255))
    save(toetsen, "block", "mewtwo_computer_toetsen.png")
    # test tubes: a wooden rack, glass tubes with pink / purple / lilac knabbelsap
    save(noisy((170, 120, 80), 7, 9), "block", "mewtwo_rekje.png")
    for naam, kleur in (("roze", (246, 120, 190)), ("paars", (150, 80, 214)), ("lila", (200, 170, 240))):
        buis = Image.new("RGBA", (16, 16), (230, 240, 255, 110))
        d = ImageDraw.Draw(buis)
        d.rectangle([0, 6, 15, 15], fill=rgba(kleur, 230))
        d.line([(0, 6), (15, 6)], fill=(255, 255, 255, 200))
        save(buis, "block", f"mewtwo_buisje_{naam}.png")


def block_models(h):
    A, w = h.A, h.w
    # the kloontank's own block (the base plate in the middle; the renderer draws the glass, the liquid and the cracks)
    tank = {"parent": "minecraft:block/block", "textures": {"particle": "guhs:block/mewtwo_tankglas", "m": "guhs:block/mewtwo_tankmetaal"},
            "elements": [_el([0, 0, 0], [16, 2, 16], "#m")],
            "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.6, 0.6, 0.6]}}}
    w(f"{A}/models/block/mewtwo_kloontank.json", tank)
    w(f"{A}/blockstates/mewtwo_kloontank.json", {"variants": {f"facing={f}": {"model": "guhs:block/mewtwo_kloontank", **({"y": r} if r else {})}
                                                              for f, r in ROT.items()}})
    w(f"{A}/models/item/mewtwo_kloontank.json", {"parent": "guhs:block/mewtwo_kloontank"})
    # the invisible parts of the tank (only a particle texture for breaking)
    w(f"{A}/models/block/mewtwo_tankwand.json", {"textures": {"particle": "guhs:block/mewtwo_tankglas"}})
    w(f"{A}/blockstates/mewtwo_tankwand.json", {"variants": {"": {"model": "guhs:block/mewtwo_tankwand"}}})
    # a little stack of lab notes (with a pencil and a purple paperclip)
    notitie = {"parent": "minecraft:block/block", "textures": {"particle": "guhs:block/mewtwo_papier", "p": "guhs:block/mewtwo_papier",
                                                               "l": "guhs:block/mewtwo_potlood", "c": "guhs:block/mewtwo_paperclip"},
               "elements": [_el([3, 0, 3], [13, 0.6, 14], "#p", rot={"origin": [8, 0, 8], "axis": "y", "angle": 22.5}),
                            _el([4, 0.6, 2.5], [13, 1.2, 13], "#p", rot={"origin": [8, 0, 8], "axis": "y", "angle": -22.5}),
                            _el([3.5, 1.2, 3.5], [12.5, 1.8, 12.5], "#p"),
                            _el([9.5, 1.8, 3.2], [11, 2.1, 5.2], "#c"),
                            _el([4, 1.8, 10], [13, 2.6, 10.8], "#l", rot={"origin": [8, 2, 10], "axis": "y", "angle": 22.5})],
               "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [0.9, 0.9, 0.9]}}}
    w(f"{A}/models/block/mewtwo_notitieplek.json", notitie)
    w(f"{A}/blockstates/mewtwo_notitieplek.json", {"variants": {f"facing={f},nummer={n}": {"model": "guhs:block/mewtwo_notitieplek", **({"y": r} if r else {})}
                                                                for f, r in ROT.items() for n in range(1, 7)}})
    # the parts crate (a label per part, on the front)
    for n in range(1, 5):
        kist = {"parent": "minecraft:block/block", "textures": {"particle": "guhs:block/mewtwo_kist_zijkant", "s": "guhs:block/mewtwo_kist_zijkant",
                                                                "t": "guhs:block/mewtwo_kist_top", "l": f"guhs:block/mewtwo_kist_label_{n}"},
                "elements": [_el([2, 0, 2], [14, 10, 14], "#s", face_uv={"up": [0, 0, 16, 16]}, faces=("down", "north", "south", "west", "east")),
                             _el([1.5, 10, 1.5], [14.5, 11.5, 14.5], "#t"),
                             _el([5, 3, 1.6], [11, 8, 2], "#l", faces=("north",))]}
        kist["elements"][0]["faces"]["up"] = {"texture": "#t"}
        w(f"{A}/models/block/mewtwo_onderdelenkist_{n}.json", kist)
    w(f"{A}/blockstates/mewtwo_onderdelenkist.json", {"variants": {f"facing={f},soort={n}": {"model": f"guhs:block/mewtwo_onderdelenkist_{n}",
                                                                                          **({"y": r} if r else {})}
                                                                   for f, r in ROT.items() for n in range(1, 5)}})
    # the grote knabbelschaal: a quartz stand, a wide bowl, a heap of kaasknabbels
    schaal = {"parent": "minecraft:block/block", "textures": {"particle": "guhs:block/mewtwo_schaal", "s": "guhs:block/mewtwo_schaal",
                                                              "k": "guhs:block/mewtwo_schaal_knabbels"},
              "elements": [_el([5, 0, 5], [11, 1, 11], "#s"), _el([6.5, 1, 6.5], [9.5, 5, 9.5], "#s"),
                           _el([1, 5, 1], [15, 6, 15], "#s"),
                           _el([0, 6, 0], [16, 10, 1], "#s"), _el([0, 6, 15], [16, 10, 16], "#s"),
                           _el([0, 6, 1], [1, 10, 15], "#s"), _el([15, 6, 1], [16, 10, 15], "#s"),
                           _el([1, 6, 1], [15, 9, 15], "#k", faces=("up",)),
                           _el([4, 9, 4], [12, 11, 12], "#k"), _el([6, 11, 6], [10, 12, 10], "#k")],
              "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.62, 0.62, 0.62]}}}
    w(f"{A}/models/block/mewtwo_knabbelschaal.json", schaal)
    w(f"{A}/blockstates/mewtwo_knabbelschaal.json", {"variants": {"": {"model": "guhs:block/mewtwo_knabbelschaal"}}})
    w(f"{A}/models/item/mewtwo_knabbelschaal.json", {"parent": "guhs:block/mewtwo_knabbelschaal"})
    # the guh-computer (screen to the north) with a keyboard
    comp = {"parent": "minecraft:block/block", "textures": {"particle": "guhs:block/mewtwo_computer_kast", "k": "guhs:block/mewtwo_computer_kast",
                                                            "s": "guhs:block/mewtwo_computer_scherm", "t": "guhs:block/mewtwo_computer_toetsen"},
            "elements": [_el([3, 3, 5], [13, 12, 13], "#k", face_uv={"north": [0, 0, 16, 16]}),
                         _el([4, 4, 4.8], [12, 11, 5], "#s", faces=("north",), uv=[0, 0, 16, 16]),
                         _el([6, 0, 7], [10, 3, 11], "#k"),
                         _el([3, 0, 0.5], [13, 1, 4.5], "#t", face_uv={"up": [0, 0, 16, 16]})],
            "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.7, 0.7, 0.7]}}}
    w(f"{A}/models/block/mewtwo_computer.json", comp)
    w(f"{A}/blockstates/mewtwo_computer.json", {"variants": {f"facing={f}": {"model": "guhs:block/mewtwo_computer", **({"y": r} if r else {})}
                                                             for f, r in ROT.items()}})
    w(f"{A}/models/item/mewtwo_computer.json", {"parent": "guhs:block/mewtwo_computer"})
    # the rack of test tubes
    buis = {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
            "textures": {"particle": "guhs:block/mewtwo_rekje", "r": "guhs:block/mewtwo_rekje", "a": "guhs:block/mewtwo_buisje_roze",
                         "b": "guhs:block/mewtwo_buisje_paars", "c": "guhs:block/mewtwo_buisje_lila"},
            "elements": [_el([2, 0, 5], [14, 1.5, 11], "#r"), _el([2, 5, 6.5], [14, 6, 9.5], "#r"),
                         _el([2, 1.5, 7.5], [3, 5, 8.5], "#r"), _el([13, 1.5, 7.5], [14, 5, 8.5], "#r"),
                         _el([3.5, 1.5, 7], [5.5, 9, 9], "#a", uv=[0, 0, 16, 16]), _el([6.5, 1.5, 7], [8.5, 8, 9], "#b", uv=[0, 0, 16, 16]),
                         _el([9.5, 1.5, 7], [11.5, 9.5, 9], "#c", uv=[0, 0, 16, 16]), _el([12, 1.5, 7.2], [13.2, 7, 8.8], "#a", uv=[0, 0, 16, 16])],
            "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.8, 0.8, 0.8]}}}
    w(f"{A}/models/block/mewtwo_reageerbuisjes.json", buis)
    w(f"{A}/blockstates/mewtwo_reageerbuisjes.json", {"variants": {f"facing={f}": {"model": "guhs:block/mewtwo_reageerbuisjes", **({"y": r} if r else {})}
                                                                   for f, r in ROT.items()}})
    w(f"{A}/models/item/mewtwo_reageerbuisjes.json", {"parent": "guhs:block/mewtwo_reageerbuisjes"})
    # messy papers (a thin layer on the floor)
    pap = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
           "textures": {"particle": "guhs:block/mewtwo_papier", "p": "guhs:block/mewtwo_papieren"},
           "elements": [_el([0, 0, 0], [16, 0.25, 16], "#p", faces=("up", "down"), uv=[0, 0, 16, 16])]}
    w(f"{A}/models/block/mewtwo_papieren.json", pap)
    w(f"{A}/blockstates/mewtwo_papieren.json", {"variants": {f"facing={f}": {"model": "guhs:block/mewtwo_papieren", **({"y": r} if r else {})}
                                                             for f, r in ROT.items()}})
    w(f"{A}/models/item/mewtwo_papieren.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:block/mewtwo_papieren"}})


# =====================================================================================================================
# items and particles
# =====================================================================================================================
def items(h):
    A, w, save = h.A, h.w, h.save
    # the lab note: a crumpled page with scribbles, a drawn knabbel and a purple paperclip
    rows = ["................", "..aaaaaaaaaa....", "..abbbbbbbbba...", "..abcccbbbbbba..", "..abbbbbccbbba..", "..abcccccbbbba..",
            "..abbbbbbbbba...", "..abccbbyybba...", "..abbbbyooyba...", "..abccbbyybba...", "..abbbbbbbbba...", "..abcccccbbba...",
            "...abbbbbbbba...", "...aaaaaaaaaa...", "................", "................"]
    note = h.grid(rows, {"a": (170, 160, 150, 255), "b": (250, 250, 244, 255), "c": (140, 140, 200, 255), "y": (250, 206, 70, 255),
                         "o": (236, 150, 50, 255)})
    d = ImageDraw.Draw(note)
    d.line([(10, 0), (10, 3)], fill=(150, 90, 210, 255))
    d.line([(11, 0), (11, 3)], fill=(150, 90, 210, 255))
    save(note, "item", "mewtwo_labnotitie.png")
    w(f"{A}/models/item/mewtwo_labnotitie.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/mewtwo_labnotitie"}})
    # the four tank parts: a curved glass panel, a copper tube, a little pump, a bottle of pink knabbelsap
    parts = {
        1: (["................", "......aaaa......", "....aabbbbaa....", "...abbwbbbbba...", "..abbwbbbbbbba..", "..abwbbbbbbbba..",
             "..abwbbbbbbbba..", "..abbbbbbbbbba..", "..abbbbbbbbbba..", "..abbbbbbbbbba..", "..abbbbbbbbbba..", "..aaaaaaaaaaaa..",
             "................", "................", "................", "................"],
            {"a": (150, 170, 200, 255), "b": (200, 230, 250, 180), "w": (255, 255, 255, 230)}),
        2: (["................", "................", "..........aa....", ".........abba...", "........abba....", ".......abba.....",
             "......abba......", ".....abba.......", "....abba........", "...abba.........", "..abba..........", "..aaa...........",
             "................", "................", "................", "................"],
            {"a": (150, 80, 40, 255), "b": (230, 140, 80, 255)}),
        3: (["................", "......ccc.......", "......cdc.......", "....aaaaaaa.....", "...abbbbbbba....", "...abbeeebba....",
             "...abbebebba....", "...abbeeebbac...", "...abbbbbbbacd..", "...abbbbbbbac...", "....aaaaaaa.....", ".....a...a......",
             "....aaa.aaa.....", "................", "................", "................"],
            {"a": (80, 80, 96, 255), "b": (170, 170, 186, 255), "c": (120, 120, 140, 255), "d": (230, 230, 240, 255),
             "e": (246, 130, 190, 255)}),
        4: (["................", "......aa........", "......bb........", "......aa........", ".....abba.......", "....abccba......",
             "...abccccba.....", "...acccwcca.....", "...acccwcca.....", "...acccccca.....", "...acccccca.....",
             "...acccccca.....", "....aaaaaa......", "................", "................", "................"],
            {"a": (200, 210, 230, 255), "b": (160, 110, 70, 255), "c": (246, 130, 190, 255), "w": (255, 220, 240, 255)}),
    }
    overrides = []
    for n, (rs, pal) in parts.items():
        save(h.grid(rs, pal), "item", f"mewtwo_tankonderdeel_{n}.png")
        w(f"{A}/models/item/mewtwo_tankonderdeel_{n}.json", {"parent": "minecraft:item/generated",
                                                             "textures": {"layer0": f"guhs:item/mewtwo_tankonderdeel_{n}"}})
        overrides.append({"predicate": {"custom_model_data": n}, "model": f"guhs:item/mewtwo_tankonderdeel_{n}"})
    w(f"{A}/models/item/mewtwo_tankonderdeel.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:item/mewtwo_tankonderdeel_1"},
                                                     "overrides": overrides})
    w(f"{A}/models/item/mew_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    # the blocks without an item model of their own (the quest spots: creative only, for building your own lab)
    for b in ("mewtwo_notitieplek", "mewtwo_onderdelenkist"):
        w(f"{A}/models/item/{b}.json", {"parent": f"guhs:block/{b}" + ("_1" if b == "mewtwo_onderdelenkist" else "")})


def particles(h):
    save, w, A = h.save, h.w, h.A
    for i in range(4):
        im = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        r = 1 + i % 2
        col = [(200, 130, 255, 255), (230, 180, 255, 255), (170, 100, 240, 255), (250, 210, 255, 255)][i]
        d.line([(3.5 - r - 1, 3.5), (3.5 + r + 1, 3.5)], fill=col)
        d.line([(3.5, 3.5 - r - 1), (3.5, 3.5 + r + 1)], fill=col)
        d.point([(3, 3), (4, 4), (3, 4), (4, 3)], fill=(255, 255, 255, 255))
        save(im, "particle", f"mewtwo_gloed_{i}.png")
    w(f"{A}/particles/mewtwo_gloed.json", {"textures": [f"guhs:mewtwo_gloed_{i}" for i in range(4)]})
    x2 = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(x2)
    rows = ["...............", ".x...x..222....", "..x.x..2...2...", "...x......2....", "..x.x....2.....", ".x...x..2222...",
            "..............."]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        if x2.getpixel((x + dx, y + 4 + dy))[3] == 0:
                            x2.putpixel((x + dx, y + 4 + dy), (255, 255, 255, 255))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                x2.putpixel((x, y + 4), (150, 70, 214, 255))
    save(x2, "particle", "mewtwo_x2.png")
    w(f"{A}/particles/mewtwo_x2.json", {"textures": ["guhs:mewtwo_x2"]})
    bub = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    d = ImageDraw.Draw(bub)
    d.ellipse([1, 1, 6, 6], outline=(255, 200, 230, 255), fill=(250, 150, 200, 90))
    d.point([(2, 2)], fill=(255, 255, 255, 255))
    save(bub, "particle", "mewtwo_bubbel.png")
    w(f"{A}/particles/mewtwo_bubbel.json", {"textures": ["guhs:mewtwo_bubbel"]})


def build(h):
    mewtwo_textures(h)
    mew_model(h)
    knabbelkloon(h)
    block_textures(h)
    block_models(h)
    items(h)
    particles(h)
