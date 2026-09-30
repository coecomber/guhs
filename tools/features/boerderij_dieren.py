"""
De Guhboerderij (2.8) - the animals' looks: GeckoLib models, animations and textures of the guhschaapje, the
knabbelkippetje and the guhkoe, and Boerin Hooibaal's own sitting-guh model (straw hat, a straw in her mouth, an apron).

Every animal is unmistakably a guh: a blocky guh head (or, for the round little kippetje, a guh ball) with the big
glossy guh eyes (dark pupil, a blue-to-teal ring, two white highlights), a tiny snoet and pink blush, and two ROUND guh
ears with a darker inside (their cube faces are painted round, the corners see-through). Textures are painted face by
face on an atlas (each cube face gets its own patch, 1:1, at 4 texels per model pixel), so wool curls and cheese spots
don't stretch.

  build(h)             writes geo/entity/<dier>.geo.json, animations/entity/<dier>.animation.json, textures/entity/<dier>.png
                       for the three animals, and guh_npc_boerinneguh.geo.json + npc_boerinneguh.png
  preview()            (python tools/features/boerderij_dieren.py) writes a flat preview of the atlases to the scratch dir
Animations (names used by BoerderijDier): idle, walk, blij, eet, geluid.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image

S = 4                      # texels per model pixel
ATLAS = 128                # atlas size in model pixels (UV units)

# --- colours --------------------------------------------------------------------------------------------------------------
WOL = (255, 242, 247)
WOL_SCHADUW = (240, 214, 228)
SCHAAP_HUID = (250, 192, 214)
SCHAAP_GEZICHT = (255, 208, 226)
SCHAAP_POOT = (238, 166, 196)
KIP = (250, 206, 84)
KIP_LICHT = (255, 236, 164)
KIP_DONKER = (222, 164, 52)
SNAVEL = (246, 150, 52)
KAM = (240, 110, 120)
KOE = (255, 246, 226)
KOE_VLEK = (244, 196, 74)
KOE_GAT = (206, 146, 40)
KOE_GEZICHT = (250, 200, 214)
SNUIT = (252, 164, 190)
HOORN = (255, 232, 160)
HOEF = (150, 104, 84)
OOR_BINNEN = (236, 128, 168)


# =====================================================================================================================
# the atlas: every cube face gets its own painted patch
# =====================================================================================================================
class Atlas:
    def __init__(self, seed):
        self.img = np.zeros((ATLAS * S, ATLAS * S, 4), np.uint8)
        self.x = self.y = self.row = 0
        self.rng = np.random.default_rng(seed)

    def alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        if self.x + w > ATLAS:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > ATLAS:
            raise SystemExit("boerderij_dieren: the texture atlas is full")
        u, v = self.x, self.y
        self.x += w
        self.row = max(self.row, h)
        return u, v

    def patch(self, w, h, painter):
        """A face patch of w x h model pixels painted by painter(W, H, rng) -> RGBA array (H*S, W*S, 4)."""
        u, v = self.alloc(w, h)
        W, H = int(math.ceil(w)) * S, int(math.ceil(h)) * S
        arr = painter(W, H, self.rng)
        self.img[v * S:v * S + H, u * S:u * S + W] = arr
        return {"uv": [u, v], "uv_size": [w, h]}


def plain(colour, var=10, spots=None):
    """A soft fur/wool/skin patch: the colour with a little noise (and optional spots(W, H, rng, arr))."""
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        a[..., :3] = colour
        a[..., :3] += rng.normal(0, var / 2, (H, W, 1))
        # soft 2x2 clumps: guh fur is a bit furry
        clump = rng.normal(0, var / 3, ((H + 1) // 2, (W + 1) // 2, 1)).repeat(2, 0).repeat(2, 1)[:H, :W]
        a[..., :3] += clump
        a[..., 3] = 255
        if spots:
            spots(W, H, rng, a)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def disc(a, cx, cy, r, colour, alpha=1.0, ry=None):
    H, W = a.shape[:2]
    ry = ry or r
    y0, y1 = max(0, int(cy - ry - 1)), min(H, int(cy + ry + 2))
    x0, x1 = max(0, int(cx - r - 1)), min(W, int(cx + r + 2))
    for y in range(y0, y1):
        for x in range(x0, x1):
            if ((x + 0.5 - cx) / r) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1:
                a[y, x, :3] = a[y, x, :3] * (1 - alpha) + np.array(colour) * alpha
                a[y, x, 3] = 255


def curls(W, H, rng, a):
    """Wool curls: little light rings with a shadow under them."""
    for _ in range(W * H // 45):
        cx, cy = rng.uniform(0, W), rng.uniform(0, H)
        r = rng.uniform(2.2, 3.6)
        disc(a, cx, cy + 0.8, r, WOL_SCHADUW, 0.7)
        disc(a, cx, cy, r * 0.8, (255, 250, 252), 0.8)
        disc(a, cx - 0.6, cy - 0.6, r * 0.35, (255, 255, 255), 0.9)


def kaasvlekken(W, H, rng, a):
    """Cheese-coloured patches with little holes (like gatenkaas): the guhkoe's spots."""
    for _ in range(max(1, W * H // 420)):
        cx, cy = rng.uniform(0, W), rng.uniform(0, H)
        r = rng.uniform(6, 11)
        for k in range(5):
            disc(a, cx + rng.uniform(-r / 2, r / 2), cy + rng.uniform(-r / 2, r / 2), r * rng.uniform(0.45, 0.7), KOE_VLEK, 1.0)
        for k in range(3):
            disc(a, cx + rng.uniform(-r / 2, r / 2), cy + rng.uniform(-r / 2, r / 2), rng.uniform(1.2, 2.2), KOE_GAT, 1.0)


def kruimels(W, H, rng, a):
    for _ in range(W * H // 60):
        x, y = int(rng.uniform(0, W - 2)), int(rng.uniform(0, H - 2))
        a[y:y + 2, x:x + 2, :3] = KIP_DONKER if rng.random() < 0.6 else KIP_LICHT


def buikje(colour):
    def f(W, H, rng, a):
        disc(a, W / 2, H * 0.62, W * 0.36, colour, 1.0, ry=H * 0.34)
    return f


# --- the guh face ---------------------------------------------------------------------------------------------------------
def guh_face(fur, eye_y=0.42, eye_dx=0.26, eye_r=0.17, snoet=True, blush=True, spots=None):
    """A guh face on a fur patch: big glossy eyes (outline, dark pupil, blue->teal ring below, two white highlights),
    pink blush under them, a small nose and a soft mouth line."""
    def paint(W, H, rng):
        a = plain(fur, 8, spots)(W, H, rng).astype(np.float32)
        r = eye_r * W
        for sx in (-1, 1):
            cx, cy = W / 2 + sx * eye_dx * W, H * eye_y
            for y in range(H):
                for x in range(W):
                    dx, dy = (x + 0.5 - cx) / r, (y + 0.5 - cy) / (r * 1.08)
                    d = math.hypot(dx, dy)
                    if d > 1:
                        continue
                    if d > 0.86 or (dy < -0.55 and d > 0.78):
                        c = (22, 16, 34)                          # outline (thicker on top: lashes)
                    elif dy > -0.05 and d > 0.5:
                        t = min(1.0, (dy + 0.05) / 0.9)
                        c = (int(40 + 50 * t), int(100 + 110 * t), int(200 + 20 * t))   # the ring: blue -> teal
                    else:
                        c = (18, 18, 34)                          # pupil
                    a[y, x, :3] = c
            disc(a, cx + 0.22 * r, cy - 0.32 * r, 0.3 * r, (255, 255, 255))
            disc(a, cx - 0.32 * r, cy + 0.28 * r, 0.13 * r, (226, 232, 240))
            if blush:
                disc(a, cx + sx * 0.25 * r, cy + 1.45 * r, 0.62 * r, (255, 150, 186), 0.55, ry=0.32 * r)
        if snoet:
            ny = H * eye_y + 1.25 * r
            nw = max(2.0, W * 0.07)
            for y in range(int(ny), int(ny + nw * 0.8) + 1):
                half = nw * (1 - (y - ny) / (nw * 1.4))
                for x in range(int(W / 2 - half), int(W / 2 + half) + 1):
                    if 0 <= y < H and 0 <= x < W:
                        a[y, x, :3] = (214, 96, 150)
            my = int(ny + nw * 0.8) + 2
            for x in range(int(W / 2 - nw * 1.6), int(W / 2 + nw * 1.6) + 1):
                dy = int(round(abs(x - W / 2) / (nw * 1.6) * 1.5))
                if 0 <= my - dy < H and 0 <= x < W:
                    a[my - dy, x, :3] = np.array(fur) * 0.72
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def round_ear(fur, inner):
    """A round guh ear (see-through corners) with a darker inside."""
    def paint(W, H, rng):
        a = plain(fur, 8)(W, H, rng).astype(np.float32)
        out = np.zeros_like(a)
        disc(out, W / 2, H / 2, W / 2, fur, 1.0, ry=H / 2)
        mask = out[..., 3] > 0
        a[~mask, 3] = 0
        inside = np.zeros_like(a)
        disc(inside, W / 2, H * 0.55, W * 0.3, inner, 1.0, ry=H * 0.3)
        m2 = inside[..., 3] > 0
        a[m2, :3] = inside[m2, :3]
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def transparent(W, H, rng):
    return np.zeros((H, W, 4), np.uint8)


# =====================================================================================================================
# model building
# =====================================================================================================================
FACES = ("north", "south", "east", "west", "up", "down")


def face_size(face, size):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}[face]


def cube(atlas, origin, size, paint, overrides=None, inflate=None, pivot=None, rotation=None):
    """A cube whose every face gets its own patch painted by paint (or overrides[face])."""
    uv = {}
    for f in FACES:
        w, h = face_size(f, size)
        p = (overrides or {}).get(f, paint)
        uv[f] = atlas.patch(max(w, 0.5), max(h, 0.5), p)
    c = {"origin": origin, "size": size, "uv": uv}
    if inflate:
        c["inflate"] = inflate
    if rotation:
        c["pivot"], c["rotation"] = pivot, rotation
    return c


def bone(name, parent, pivot, cubes, rotation=None):
    b = {"name": name, "pivot": pivot, "cubes": cubes}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = rotation
    return b


def geo(identifier, bones, width, height):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{identifier}", "texture_width": ATLAS, "texture_height": ATLAS,
                        "visible_bounds_width": width, "visible_bounds_height": height, "visible_bounds_offset": [0, height / 2, 0]},
        "bones": bones}]}


def ears(atlas, fur, inner, x, y, z, w=3.6, h=3.6, parent="head"):
    """Two round guh ears (thin cubes), mirrored at +-x, their round face to the front."""
    ear = round_ear(fur, inner)
    edge = plain(fur, 6)
    out = []
    for side, sx in (("left", 1), ("right", -1)):
        ox = x if sx > 0 else -x - w
        out.append(bone(f"ear_{side}", parent, [sx * (x + w / 2), y, z + 0.5], [
            cube(atlas, [ox, y, z], [w, h, 1], edge, overrides={"north": ear, "south": ear})]))
    return out


# --- the guhschaapje ------------------------------------------------------------------------------------------------------
def schaapje(atlas):
    wol = plain(WOL, 6, curls)
    huid = plain(SCHAAP_HUID, 8)
    poot = plain(SCHAAP_POOT, 8)
    gezicht = plain(SCHAAP_GEZICHT, 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 6, 0], []),
             bone("wol", "body", [0, 10, 0], [cube(atlas, [-6, 5, -6], [12, 9, 13], wol),
                                              cube(atlas, [-4.5, 13.5, -4.5], [9, 2, 10], wol),
                                              cube(atlas, [-6.8, 7, -4], [1, 5, 9], wol), cube(atlas, [5.8, 7, -4], [1, 5, 9], wol),
                                              cube(atlas, [-3, 6.5, 6.5], [6, 5, 1.5], wol)]),
             bone("kaal", "body", [0, 10, 0], [cube(atlas, [-4.5, 6, -5], [9, 7, 11], huid)]),
             bone("tail", "body", [0, 10, 7], [cube(atlas, [-1.5, 9, 7], [3, 3, 2], wol)]),
             bone("head", "body", [0, 11, -6], [
                 cube(atlas, [-5, 8, -12.5], [10, 8.5, 7], gezicht, overrides={"north": guh_face(SCHAAP_GEZICHT, eye_y=0.44, eye_r=0.19)}),
                 cube(atlas, [-3.5, 16, -11], [7, 2, 5], wol)])]
    bones += ears(atlas, SCHAAP_GEZICHT, OOR_BINNEN, 4.4, 14.2, -10.5, 3.8, 3.8)
    for name, x, z in (("leg_front_left", 2.5, -4), ("leg_front_right", -4.5, -4), ("leg_back_left", 2.5, 3.5), ("leg_back_right", -4.5, 3.5)):
        bones.append(bone(name, "body", [x + 1, 5, z + 1], [cube(atlas, [x, 0, z], [2, 5.5, 2], poot,
                                                                 overrides={"down": plain((200, 120, 150), 6)})]))
    return geo("guhschaapje", bones, 1.3, 1.4)


# --- the knabbelkippetje ---------------------------------------------------------------------------------------------------
def kippetje(atlas):
    lijf = plain(KIP, 10, kruimels)
    snavel = plain(SNAVEL, 6)
    poot = plain(SNAVEL, 6)
    kam = plain(KAM, 8)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2, 0], []),
             bone("head", "body", [0, 5, 0], [
                 cube(atlas, [-4, 2, -4], [8, 7.5, 8], lijf, overrides={
                     "north": guh_face(KIP, eye_y=0.36, eye_dx=0.25, eye_r=0.19, snoet=False, spots=buikje(KIP_LICHT))}),
                 cube(atlas, [-0.9, 4.2, -5], [1.8, 1.2, 1], snavel),
                 cube(atlas, [-1, 9.5, -2], [2, 1.5, 3], kam), cube(atlas, [-0.5, 10.5, -1.5], [1, 1, 2], kam)])]
    bones += ears(atlas, KIP, OOR_BINNEN, 2.3, 8.6, -2.5, 2.8, 2.8)
    for side, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"wing_{side}", "body", [sx * 4, 7, 0], [cube(atlas, [4 if sx > 0 else -5, 3.5, -2.5], [1, 4, 5], plain(KIP_DONKER, 8))]))
    bones.append(bone("tail", "body", [0, 6, 4], [cube(atlas, [-1.5, 5.5, 4], [3, 3, 1.5], plain(KIP_DONKER, 8))]))
    for side, x in (("left", 1.2), ("right", -2.2)):
        bones.append(bone(f"leg_{side}", "body", [x + 0.5, 2, 0.5], [cube(atlas, [x, 0, 0], [1, 2.5, 1], poot),
                                                                        cube(atlas, [x - 0.5, 0, -1], [2, 0.5, 2], poot)]))
    return geo("knabbelkippetje", bones, 0.9, 1.0)


# --- the guhkoe ---------------------------------------------------------------------------------------------------------
def koe(atlas):
    lijf = plain(KOE, 8, kaasvlekken)
    gezicht = plain(KOE_GEZICHT, 8)
    hoef = plain(HOEF, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 12, 0], [cube(atlas, [-6, 9, -8], [12, 10, 17], lijf),
                                               cube(atlas, [-2.5, 7.5, 2.5], [5, 1.5, 4], plain(SNUIT, 6))]),
             bone("tail", "body", [0, 18, 9], [cube(atlas, [-0.5, 10, 9], [1, 8, 1], plain(KOE, 6)),
                                               cube(atlas, [-1, 8.5, 8.5], [2, 2, 2], plain(KOE_VLEK, 8))]),
             bone("head", "body", [0, 16, -8], [
                 cube(atlas, [-5, 13, -15], [10, 9, 7], gezicht, overrides={"north": guh_face(KOE_GEZICHT, eye_y=0.36, eye_r=0.16, snoet=False)}),
                 cube(atlas, [-3.5, 13.3, -15.8], [7, 3.2, 1], plain(SNUIT, 6), overrides={"north": snuit}),
                 cube(atlas, [-4.8, 21.6, -12.5], [1.6, 2.2, 1.6], plain(HOORN, 6)),
                 cube(atlas, [3.2, 21.6, -12.5], [1.6, 2.2, 1.6], plain(HOORN, 6))])]
    bones += ears(atlas, KOE_GEZICHT, OOR_BINNEN, 4.6, 18.2, -12.2, 3.8, 3.8)
    for name, x, z in (("leg_front_left", 2.5, -7), ("leg_front_right", -5.5, -7), ("leg_back_left", 2.5, 5), ("leg_back_right", -5.5, 5)):
        bones.append(bone(name, "body", [x + 1.5, 9, z + 1.5], [cube(atlas, [x, 0, z], [3, 9, 3], plain(KOE, 8),
                                                                     overrides={"down": hoef}),
                                                                cube(atlas, [x - 0.1, 0, z - 0.1], [3.2, 1.5, 3.2], hoef)]))
    return geo("guhkoe", bones, 1.6, 1.8)


def snuit(W, H, rng):
    a = plain(SNUIT, 6)(W, H, rng).astype(np.float32)
    for sx in (-1, 1):
        disc(a, W / 2 + sx * W * 0.22, H * 0.5, W * 0.07, (196, 86, 124), 1.0, ry=H * 0.18)
    my = int(H * 0.82)
    a[my:my + 1, int(W * 0.35):int(W * 0.65), :3] = (206, 110, 140)
    return np.clip(a, 0, 255).astype(np.uint8)


# =====================================================================================================================
# animations (names: idle, walk, blij, eet, geluid)
# =====================================================================================================================
def legs_walk(names, swing, length):
    out = {}
    for i, n in enumerate(names):
        s = swing if i in (0, 3) else -swing
        out[n] = {"rotation": {"0.0": [s, 0, 0], str(length / 2): [-s, 0, 0], str(length): [s, 0, 0]}}
    return out


def ear_twitch(length):
    return {"ear_left": {"rotation": {"0.0": [0, 0, 0], str(round(length * 0.8, 2)): [0, 0, 0], str(round(length * 0.85, 2)): [0, 0, -18],
                                      str(round(length * 0.9, 2)): [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], str(round(length * 0.4, 2)): [0, 0, 0], str(round(length * 0.45, 2)): [0, 0, 18],
                                       str(round(length * 0.5, 2)): [0, 0, 0]}}}


def animations(dier, legs, extra_idle=None, walk_extra=None, blij_extra=None):
    idle = {"body": {"scale": {"0.0": [1, 1, 1], "1.5": [1.02, 1.03, 1.02], "3.0": [1, 1, 1]}}}
    idle.update(ear_twitch(3.0))
    idle.update(extra_idle or {})
    walk = legs_walk(legs, 28, 0.7)
    walk["body"] = {"position": {"0.0": [0, 0, 0], "0.175": [0, 0.5, 0], "0.35": [0, 0, 0], "0.525": [0, 0.5, 0], "0.7": [0, 0, 0]}}
    walk.update(walk_extra or {})
    blij = {"body": {"position": {"0.0": [0, 0, 0], "0.15": [0, 3, 0], "0.3": [0, 0, 0], "0.45": [0, 2, 0], "0.6": [0, 0, 0]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "0.15": [0, 0, -25], "0.3": [0, 0, 10], "0.45": [0, 0, -20], "0.6": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "0.15": [0, 0, 25], "0.3": [0, 0, -10], "0.45": [0, 0, 20], "0.6": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "0.3": [-10, 0, 8], "0.6": [0, 0, 0]}}}
    blij.update(blij_extra or {})
    eet = {"head": {"rotation": {"0.0": [0, 0, 0], "0.2": [40, 0, 0], "0.35": [34, 0, 0], "0.5": [40, 0, 0], "0.65": [34, 0, 0],
                                 "0.8": [40, 0, 0], "1.0": [0, 0, 0]}}}
    geluid = {"head": {"rotation": {"0.0": [0, 0, 0], "0.15": [-18, 0, 0], "0.45": [-14, 0, 0], "0.6": [0, 0, 0]},
                       "scale": {"0.0": [1, 1, 1], "0.15": [1.05, 1.08, 1.05], "0.45": [1.03, 1.05, 1.03], "0.6": [1, 1, 1]}}}
    return {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 3.0, "bones": idle},
        "walk": {"loop": True, "animation_length": 0.7, "bones": walk},
        "blij": {"loop": False, "animation_length": 0.6, "bones": blij},
        "eet": {"loop": False, "animation_length": 1.0, "bones": eet},
        "geluid": {"loop": False, "animation_length": 0.6, "bones": geluid}}}


def schaapje_anims():
    return animations("guhschaapje", ["leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"],
                      extra_idle={"tail": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 15, 0], "2.0": [0, -15, 0], "3.0": [0, 0, 0]}}},
                      blij_extra={"tail": {"rotation": {"0.0": [0, 0, 0], "0.1": [0, 30, 0], "0.2": [0, -30, 0], "0.3": [0, 30, 0],
                                                        "0.4": [0, -30, 0], "0.6": [0, 0, 0]}}})


def kippetje_anims():
    flap = {"wing_left": {"rotation": {"0.0": [0, 0, 0], "0.1": [0, 0, -60], "0.2": [0, 0, 0], "0.3": [0, 0, -60], "0.4": [0, 0, 0],
                                       "0.5": [0, 0, -40], "0.6": [0, 0, 0]}},
            "wing_right": {"rotation": {"0.0": [0, 0, 0], "0.1": [0, 0, 60], "0.2": [0, 0, 0], "0.3": [0, 0, 60], "0.4": [0, 0, 0],
                                        "0.5": [0, 0, 40], "0.6": [0, 0, 0]}}}
    waddle = {"head": {"rotation": {"0.0": [0, 0, 6], "0.35": [0, 0, -6], "0.7": [0, 0, 6]}}}
    a = animations("knabbelkippetje", ["leg_left", "leg_right", "leg_right", "leg_left"], walk_extra=waddle, blij_extra=flap)
    a["animations"]["walk"]["bones"]["leg_left"] = {"rotation": {"0.0": [30, 0, 0], "0.35": [-30, 0, 0], "0.7": [30, 0, 0]}}
    a["animations"]["walk"]["bones"]["leg_right"] = {"rotation": {"0.0": [-30, 0, 0], "0.35": [30, 0, 0], "0.7": [-30, 0, 0]}}
    a["animations"]["eet"]["bones"]["head"] = {"rotation": {"0.0": [0, 0, 0], "0.15": [35, 0, 0], "0.3": [0, 0, 0], "0.45": [35, 0, 0],
                                                            "0.6": [0, 0, 0], "0.75": [35, 0, 0], "1.0": [0, 0, 0]}}
    a["animations"]["geluid"]["bones"].update(flap)
    return a


def koe_anims():
    swish = {"tail": {"rotation": {"0.0": [10, 0, 0], "0.75": [10, 0, 20], "1.5": [10, 0, -20], "2.25": [10, 0, 20], "3.0": [10, 0, 0]}}}
    return animations("guhkoe", ["leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"], extra_idle=swish,
                      blij_extra={"body": {"position": {"0.0": [0, 0, 0], "0.2": [0, 1.5, 0], "0.4": [0, 0, 0], "0.6": [0, 0, 0]}}})


# =====================================================================================================================
# Boerin Hooibaal: the sitting guh with a straw hat, a straw and an apron
# =====================================================================================================================
UV_NPC = 128
SW = 8


def _used(geo_):
    used = np.zeros((UV_NPC, UV_NPC), bool)
    for b in geo_["bones"]:
        for c in b.get("cubes", []):
            uv = c.get("uv")
            if isinstance(uv, dict):
                for f in uv.values():
                    (u, v), (w, h) = f["uv"], f["uv_size"]
                    used[int(min(v, v + h)):int(np.ceil(max(v, v + h))), int(min(u, u + w)):int(np.ceil(max(u, u + w)))] = True
            elif isinstance(uv, list):
                u, v = uv
                sx, sy, sz = c["size"]
                used[int(v):int(np.ceil(v + sz + sy)), int(u):int(np.ceil(u + 2 * (sx + sz)))] = True
    return used


def _swatches(geo_, names):
    used = _used(geo_)
    out = {}
    for name in names:
        for y in range(0, UV_NPC - SW + 1, SW):
            for x in range(0, UV_NPC - SW + 1, SW):
                if not used[y:y + SW, x:x + SW].any():
                    used[y:y + SW, x:x + SW] = True
                    out[name] = (x, y)
                    break
            if name in out:
                break
        if name not in out:
            raise SystemExit(f"boerderij_dieren: no free texture space for {name}")
    return out


def _cube(origin, size, swatch, inflate=0.0):
    face = {"uv": list(swatch), "uv_size": [SW, SW]}
    c = {"origin": origin, "size": size, "uv": {f: dict(face) for f in FACES}}
    if inflate:
        c["inflate"] = inflate
    return c


def _paint(arr, swatch, colour, rng, var=8, pattern=None):
    x, y = swatch
    block = arr[y * 4:(y + SW) * 4, x * 4:(x + SW) * 4]
    block[..., :3] = np.clip(np.array(colour, np.float32) + rng.normal(0, var / 2, (SW * 4, SW * 4, 1)), 0, 255).astype(np.uint8)
    block[..., 3] = 255
    if pattern:
        pattern(block)


def boerin(h):
    geo_file = json.load(open(os.path.join(h.A, "geo", "entity", "guh_sitting.geo.json"), encoding="utf-8"))
    g = geo_file["minecraft:geometry"][0]
    g["description"]["identifier"] = "geometry.guh_npc_boerinneguh"
    sw = _swatches(g, ["stro", "stro_donker", "band", "schort", "zak"])
    g["bones"].append({"name": "boerin_hoed", "parent": "head", "pivot": [0, 13, 0], "cubes": [
        _cube([-8.5, 25.6, -9.0], [17, 0.7, 16], sw["stro"]),
        _cube([-4.4, 26.3, -4.8], [8.8, 3.6, 8], sw["stro"]),
        _cube([-4.4, 26.3, -4.8], [8.8, 1.2, 8], sw["band"], inflate=0.12),
        _cube([-3.6, 29.9, -4.0], [7.2, 0.6, 6.4], sw["stro_donker"])]})
    g["bones"].append({"name": "boerin_strootje", "parent": "head", "pivot": [1.2, 16.2, -7.6], "cubes": [
        _cube([1.2, 16.0, -8.1], [5.0, 0.45, 0.45], sw["stro_donker"])]})
    g["bones"].append({"name": "boerin_schort", "parent": "body", "pivot": [0, 8, -4], "cubes": [
        _cube([-3.8, 2.4, -4.65], [7.6, 8.4, 0.5], sw["schort"]),
        _cube([-2.2, 4.0, -4.9], [4.4, 2.6, 0.3], sw["zak"]),
        _cube([-3.8, 10.8, -4.6], [1.0, 1.6, 0.4], sw["schort"]), _cube([2.8, 10.8, -4.6], [1.0, 1.6, 0.4], sw["schort"])]})
    h.w(os.path.join(h.A, "geo", "entity", "guh_npc_boerinneguh.geo.json"), geo_file)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png")).convert("RGBA")
    img = h.recolour(src, hue=0.07, sat=0.62, val=1.03, only=h.pinkish).convert("RGBA")
    a = np.asarray(img).copy()
    rng = np.random.default_rng(2851)

    def weave(block):
        for yy in range(0, 32, 4):
            for xx in range((yy // 4 % 2) * 4, 32, 8):
                block[yy:yy + 2, xx:xx + 4, :3] = (206, 164, 76)
    _paint(a, sw["stro"], (238, 204, 112), rng, 10, weave)
    _paint(a, sw["stro_donker"], (214, 172, 80), rng, 8)

    def dots(block):
        for yy in range(3, 32, 8):
            for xx in range(3, 32, 8):
                block[yy:yy + 2, xx:xx + 2, :3] = (255, 250, 250)
    _paint(a, sw["band"], (212, 50, 60), rng, 6, dots)

    def denim(block):
        for yy in range(0, 32, 3):
            block[yy, :, :3] = np.clip(block[yy, :, :3].astype(int) + 14, 0, 255)
        block[0:2, :, :3] = (250, 214, 110)                         # a yellow stitched hem
    _paint(a, sw["schort"], (96, 128, 186), rng, 8, denim)

    def pocket(block):
        block[0:2, :, :3] = (250, 214, 110)
        block[10:22, 12:20, :3] = (250, 170, 196)                   # a little pink heart-ish patch
    _paint(a, sw["zak"], (84, 114, 170), rng, 6, pocket)
    h.save(Image.fromarray(a), "entity", "npc_boerinneguh.png")


# =====================================================================================================================
def build(h):
    A = h.A
    for name, maker, anims, seed in (("guhschaapje", schaapje, schaapje_anims, 2851), ("knabbelkippetje", kippetje, kippetje_anims, 2852),
                                     ("guhkoe", koe, koe_anims, 2853)):
        atlas = Atlas(seed)
        h.w(f"{A}/geo/entity/{name}.geo.json", maker(atlas))
        h.w(f"{A}/animations/entity/{name}.animation.json", anims())
        h.save(Image.fromarray(atlas.img), "entity", f"{name}.png")
    boerin(h)


def check(h):
    """Every animation bone exists in its model, the models have a head and the wol/kaal bones where needed."""
    A = h.A
    problems = []
    for name in ("guhschaapje", "knabbelkippetje", "guhkoe"):
        g = json.load(open(f"{A}/geo/entity/{name}.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: bone {b['name']} has a missing parent {b['parent']}")
        if "head" not in names:
            problems.append(f"{name}: no head bone")
        anims = json.load(open(f"{A}/animations/entity/{name}.animation.json", encoding="utf-8"))["animations"]
        for an in ("idle", "walk", "blij", "eet", "geluid"):
            if an not in anims:
                problems.append(f"{name}: no animation {an}")
            for bn in anims.get(an, {}).get("bones", {}):
                if bn not in names:
                    problems.append(f"{name}: animation {an} moves a missing bone {bn}")
    g = json.load(open(f"{A}/geo/entity/guhschaapje.geo.json", encoding="utf-8"))["minecraft:geometry"][0]
    if not {"wol", "kaal"} <= {b["name"] for b in g["bones"]}:
        problems.append("guhschaapje: needs the bones wol and kaal")
    return problems


def preview():
    """A quick look at the three atlases (for the developer)."""
    out = os.environ.get("BOERDERIJ_PREVIEW", ".")
    for name, maker, seed in (("guhschaapje", schaapje, 2851), ("knabbelkippetje", kippetje, 2852), ("guhkoe", koe, 2853)):
        atlas = Atlas(seed)
        maker(atlas)
        Image.fromarray(atlas.img).save(os.path.join(out, f"preview_{name}.png"))


if __name__ == "__main__":
    preview()
