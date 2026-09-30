"""
Piep (2.8.1) - the looks: GeckoLib models, animations and textures of the pieppiepmuisje, Poepschilly, the boze
kaasknabbel and the Boze Oppernabbel, the roze guh koek block models, and an editable Blockbench project of each
(blockbench/<name>.bbmodel, bedrock format like blockbench/guh.bbmodel, texture embedded, animations inside).

The Blockbench projects belong to the user now (hand-tweaked): by default this script writes NOTHING (see build()); the
game files come from Blockbench exports (or tools/features/piep_bbexport.py).
To bring a hand edit into the game: in Blockbench File > Export > Bedrock Geometry (-> assets/guhs/geckolib/models/entity/<name>.geo.json),
Animation > Export Animations (-> assets/guhs/geckolib/animations/entity/<name>.animation.json) and save the texture.

Bone names the Java code uses (keep them when editing; see PIEP_NOTES.md):
  pieppiepmuisje : root, body, head, ear_left, ear_right, foot_front_left/right, foot_back_left/right
  poepschilly    : root, body, shell, head, flipper_front_left/right, flipper_back_left/right, tail
  boze_kaasknabbel / boze_oppernabbel : root, body, face, brow_left, brow_right (+ crown on the Oppernabbel)
Animations: idle, walk + (muisje) blij, eet, piep, zit | (schilly) swim, blij, kruip | (knabbels) aanval, zieli.

  build(h)     writes everything
  preview()    python tools/features/piep_modellen.py  -> renders to .scratch/piep_previews (needs the game files)
"""
import base64
import copy
import io
import json
import math
import os
import uuid as _uuid

import numpy as np
from PIL import Image

S = 4                      # texels per model pixel
ATLAS = 64                 # atlas size in model pixels (UV units)
BB_DIR = "blockbench"
MARKER = "piep_generated"

# --- colours (from the photos) -------------------------------------------------------------------------------------
MUIS = (42, 34, 51)             # dark purple-black fleece  #2a2233
MUIS_LICHT = (62, 52, 74)
CREME = (236, 228, 208)         # the fluffy cream band / chin
CREME_SCHADUW = (214, 204, 182)
OOR = (92, 90, 98)              # grey felt ears
NEUS = (240, 142, 162)
OOG = (14, 12, 18)
POOT = (26, 22, 30)

SCHILD = (112, 128, 62)         # moss green shell
SCHILD_RAND = (138, 152, 82)
SCHILD_DONKER = (86, 100, 46)
HUID = (238, 228, 208)          # cream head/flippers
VLEK = (104, 94, 80)            # brown-grey mottles
BUIK = (240, 230, 214)
AMBER = (196, 122, 40)

KAAS = (245, 169, 58)           # the plush: warm orange-yellow  #F5A93A
KAAS_LICHT = (255, 196, 92)
KAAS_DONKER = (214, 116, 30)
KAAS_BOOS = (240, 150, 46)      # the Oppernabbel: a bit deeper
WENKBRAUW = (92, 44, 20)
GOUD = (250, 206, 72)
GOUD_DONKER = (206, 150, 40)

KOEK_ROZE = (226, 84, 170)
KOEK_ROZE_LICHT = (242, 128, 196)
KOEK_RAND = (206, 150, 92)
KOEK_RAND_DONKER = (176, 118, 66)
KOEK_BINNEN = (240, 214, 160)
SCHOTEL = (244, 244, 250)
SCHOTEL_RAND = (190, 206, 236)


# =====================================================================================================================
# painting helpers
# =====================================================================================================================
class Atlas:
    def __init__(self, seed, size=ATLAS, s=S):
        self.size = size
        self.s = s
        self.img = np.zeros((size * s, size * s, 4), np.uint8)
        self.x = self.y = self.row = 0
        self.rng = np.random.default_rng(seed)

    def alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        if self.x + w > self.size:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > self.size:
            raise SystemExit("piep_modellen: the texture atlas is full")
        u, v = self.x, self.y
        self.x += w
        self.row = max(self.row, h)
        return u, v

    def patch(self, w, h, painter):
        u, v = self.alloc(w, h)
        k = self.s
        W, H = int(math.ceil(w)) * k, int(math.ceil(h)) * k
        self.img[v * k:v * k + H, u * k:u * k + W] = painter(W, H, self.rng)
        return {"uv": [u, v], "uv_size": [w, h]}


def plain(colour, var=10, extra=None, clump=2):
    """Fleece/felt/skin: the colour with soft noise in little clumps (and an optional extra(W, H, rng, a))."""
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        a[..., :3] = colour
        a[..., :3] += rng.normal(0, var / 2, (H, W, 1))
        c = rng.normal(0, var / 2.5, ((H + clump - 1) // clump, (W + clump - 1) // clump, 1)).repeat(clump, 0).repeat(clump, 1)[:H, :W]
        a[..., :3] += c
        a[..., 3] = 255
        if extra:
            extra(W, H, rng, a)
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


def line(a, x0, y0, x1, y1, colour, width=1.0):
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(n + 1):
        t = i / max(1, n)
        disc(a, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, width / 2 + 0.3, colour)


def fluff(W, H, rng, a):
    """Fluffy sheep-fleece: lighter and darker tufts."""
    for _ in range(W * H // 12):
        x, y = rng.uniform(0, W), rng.uniform(0, H)
        disc(a, x, y, rng.uniform(0.8, 1.8), (255, 250, 238) if rng.random() < 0.5 else CREME_SCHADUW, 0.5)


def mottles(colour, density=60, rmin=1.5, rmax=3.2, ring=None):
    """Brown-grey mottled patches (the turtle's scales): blotches with a thin lighter gap between them."""
    def f(W, H, rng, a):
        for _ in range(max(2, W * H // density)):
            x, y = rng.uniform(0, W), rng.uniform(0, H)
            r = rng.uniform(rmin, rmax)
            disc(a, x, y, r, colour, 0.9, ry=r * rng.uniform(0.6, 1.0))
    return f


def schubben(colour, cell=5.0):
    """The turtle plush's pattern: rows of dark rounded scales with thin cream gaps between them."""
    def f(W, H, rng, a):
        rows = int(H / cell) + 2
        cols = int(W / cell) + 2
        for r in range(rows):
            for c in range(cols):
                x = (c + (0.5 if r % 2 else 0)) * cell + rng.uniform(-0.6, 0.6)
                y = r * cell + rng.uniform(-0.6, 0.6)
                if rng.random() < 0.88:
                    disc(a, x, y, cell * rng.uniform(0.36, 0.46), colour, 0.92, ry=cell * rng.uniform(0.3, 0.42))
    return f


def round_patch(colour, var=8, extra=None):
    """A round felt shape: the corners are see-through."""
    def paint(W, H, rng):
        a = plain(colour, var, extra)(W, H, rng)
        for y in range(H):
            for x in range(W):
                if ((x + 0.5 - W / 2) / (W / 2)) ** 2 + ((y + 0.5 - H / 2) / (H / 2)) ** 2 > 1.0:
                    a[y, x, 3] = 0
        return a
    return paint


def specks(colour, n=40):
    def f(W, H, rng, a):
        for _ in range(max(3, W * H // n)):
            x, y = int(rng.uniform(0, W - 1)), int(rng.uniform(0, H - 1))
            a[y:y + 1, x:x + 2, :3] = colour
    return f


def combine(*fns):
    def f(W, H, rng, a):
        for fn in fns:
            if fn:
                fn(W, H, rng, a)
    return f


def solid(colour):
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.uint8)
        a[..., :3] = colour
        a[..., 3] = 255
        return a
    return paint


# =====================================================================================================================
# model building (bedrock geo 1.12.0, per-face UV)
# =====================================================================================================================
FACES = ("north", "south", "east", "west", "up", "down")


def face_size(face, size):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}[face]


def cube(atlas, origin, size, paint, overrides=None, inflate=None):
    uv = {}
    for f in FACES:
        w, h = face_size(f, size)
        p = (overrides or {}).get(f, paint)
        uv[f] = atlas.patch(max(w, 0.5), max(h, 0.5), p)
    c = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": uv}
    if inflate:
        c["inflate"] = inflate
    return c


def mirror_x(origin, size):
    return [-(origin[0] + size[0]), origin[1], origin[2]]


def bone(name, parent, pivot, cubes, rotation=None):
    b = {"name": name, "pivot": pivot, "cubes": cubes}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = rotation
    return b


def geo(identifier, bones, width, height, atlas_size=ATLAS):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{identifier}", "texture_width": atlas_size, "texture_height": atlas_size,
                        "visible_bounds_width": width, "visible_bounds_height": height, "visible_bounds_offset": [0, height / 2, 0]},
        "bones": bones}]}


def kf(pairs):
    """{time: value} with rounded, string times."""
    return {str(round(t, 3)): v for t, v in pairs}


# =====================================================================================================================
# the pieppiepmuisje: an egg/bean of dark fleece, a fluffy cream band, cream chin, glossy pink nose, bead eyes
# =====================================================================================================================
def muisje_face(W, H, rng):
    """The front of the head: dark fleece above, the cream chin patch rising in a soft point under the nose."""
    a = plain(MUIS, 9)(W, H, rng).astype(np.float32)
    for y in range(H):
        for x in range(W):
            dx = abs(x + 0.5 - W / 2) / (W / 2)
            edge = H * (0.60 + 0.30 * dx * dx)       # chin line: high in the middle, lower at the sides
            if y + 0.5 > edge:
                a[y, x, :3] = np.array(CREME) + rng.normal(0, 6)
    fluff_edge = np.zeros_like(a)
    return np.clip(a, 0, 255).astype(np.uint8)


def muisje_cheek(W, H, rng):
    """The sides of the head: dark, with the cream chin wrapping under."""
    a = plain(MUIS, 9)(W, H, rng).astype(np.float32)
    for y in range(H):
        for x in range(W):
            t = x / max(1, W - 1)                    # 0 = front ... 1 = back (east/west faces run front->back or back->front)
            if y + 0.5 > H * 0.78:
                a[y, x, :3] = np.array(CREME) + rng.normal(0, 6)
    return np.clip(a, 0, 255).astype(np.uint8)


def oog(W, H, rng):
    """A black glossy bead with a white shine."""
    a = np.zeros((H, W, 4), np.float32)
    a[..., :3] = OOG
    a[..., 3] = 255
    disc(a, W * 0.66, H * 0.32, max(1.0, W * 0.2), (250, 250, 255))
    disc(a, W * 0.3, H * 0.72, max(0.6, W * 0.1), (90, 90, 110), 0.8)
    return a.astype(np.uint8)


def neus(W, H, rng):
    a = np.zeros((H, W, 4), np.float32)
    a[..., :3] = NEUS
    a[..., 3] = 255
    disc(a, W * 0.62, H * 0.3, max(1.0, W * 0.16), (255, 214, 224))
    a[-1:, :, :3] = (214, 112, 132)
    return a.astype(np.uint8)


# the ears: (rest rotation, cube origin (left ear), cube size, pivot (left ear)); MUISJE_OREN picks one
OREN = {"A": ((-28, 12, -14), [0.35, 4.6, -2.7], [1.9, 1.7, 0.3], [1.2, 4.6, -2.55]),
        "B": ((58, 14, -12), [0.45, 4.55, -2.3], [1.5, 1.35, 0.3], [1.2, 4.6, -2.15]),
        "C": ((32, 18, -16), [0.4, 4.5, -2.35], [1.7, 1.5, 0.3], [1.2, 4.6, -2.15])}
MUISJE_OREN = os.environ.get("MUISJE_OREN", "C")
EAR = OREN[MUISJE_OREN][0]        # the ears' rest pose


def ear_rot(sx, dx=0, dz=0):
    return [EAR[0] + dx, sx * EAR[1], sx * (EAR[2] + dz)]


def muisje(atlas):
    fleece = plain(MUIS, 10, specks(MUIS_LICHT, 30))
    band = plain(CREME, 10, fluff)
    buik = plain(CREME, 8, fluff)
    oor = round_patch(OOR, 8, specks((120, 118, 126), 25))
    poot = plain(POOT, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 2, 0], [
                 cube(atlas, [-2.5, 0.7, -2.6], [5, 3.6, 6.0], fleece, overrides={"down": buik}),
                 cube(atlas, [-2.1, 4.3, -2.3], [4.2, 0.6, 5.5], fleece),
                 cube(atlas, [-1.4, 4.9, -1.6], [2.8, 0.3, 4.2], fleece),
                 cube(atlas, [-2.1, 0.2, -2.3], [4.2, 0.5, 5.5], buik),
                 cube(atlas, [-2.1, 1.0, 3.4], [4.2, 3.2, 0.6], fleece),
                 cube(atlas, [-1.4, 1.5, 4.0], [2.8, 2.2, 0.4], fleece),
                 cube(atlas, [-2.9, 1.2, -2.2], [0.4, 2.8, 5.4], fleece),
                 cube(atlas, [2.5, 1.2, -2.2], [0.4, 2.8, 5.4], fleece),
                 # the fluffy cream band around the middle (a little proud of the body all round)
                 cube(atlas, [-3.05, 0.4, 0.3], [6.1, 4.2, 1.6], band),
                 cube(atlas, [-2.5, 4.6, 0.3], [5.0, 0.55, 1.6], band),
                 cube(atlas, [-1.8, 5.15, 0.3], [3.6, 0.3, 1.6], band)]),
             bone("head", "body", [0, 2.5, -2.6], [
                 cube(atlas, [-2.3, 0.7, -3.3], [4.6, 3.6, 0.8], fleece, overrides={"north": muisje_face, "east": muisje_cheek,
                                                                                    "west": muisje_cheek}),
                 cube(atlas, [-1.9, 1.0, -3.7], [3.8, 2.9, 0.5], fleece, overrides={"north": muisje_face}),
                 cube(atlas, [-1.6, 0.35, -3.5], [3.2, 0.7, 1.2], buik),
                 cube(atlas, [-1.9, 4.3, -3.0], [3.8, 0.5, 0.8], fleece),
                 # the bead eyes and the glossy pink nose
                 cube(atlas, [0.55, 2.55, -3.95], [0.9, 0.9, 0.3], solid(OOG), overrides={"north": oog}),
                 cube(atlas, mirror_x([0.55, 2.55, -3.95], [0.9, 0.9, 0.3]), [0.9, 0.9, 0.3], solid(OOG), overrides={"north": oog}),
                 cube(atlas, [-0.45, 1.85, -4.05], [0.9, 0.7, 0.45], solid(NEUS), overrides={"north": neus})])]
    # the two small grey felt ears, lying back flat-ish on top of the head
    for side, sx in (("left", 1), ("right", -1)):
        _r, eo, es, ep = OREN[MUISJE_OREN]
        o = eo if sx > 0 else mirror_x(eo, es)
        bones.append(bone(f"ear_{side}", "head", [sx * ep[0], ep[1], ep[2]], [cube(atlas, o, list(es), oor)],
                          rotation=[EAR[0], sx * EAR[1], sx * EAR[2]]))
    for name, x, z, sz in (("foot_front_left", 0.9, -2.2, (1.0, 0.45, 0.8)), ("foot_front_right", -1.9, -2.2, (1.0, 0.45, 0.8)),
                           ("foot_back_left", 0.7, 2.6, (1.3, 0.55, 1.1)), ("foot_back_right", -2.0, 2.6, (1.3, 0.55, 1.1))):
        bones.append(bone(name, "body", [x + sz[0] / 2, 0.5, z + sz[2] / 2], [cube(atlas, [x, 0, z], list(sz), poot)]))
    return geo("pieppiepmuisje", bones, 0.6, 0.5)


def muisje_anims():
    EL, ER = ear_rot(1), ear_rot(-1)
    idle = {"body": {"scale": kf([(0, [1, 1, 1]), (0.6, [1.03, 0.97, 1.02]), (1.2, [1, 1, 1]), (2.0, [1, 1, 1])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (1.3, [0, 0, 0]), (1.4, [-6, 0, 0]), (1.5, [0, 0, 0]), (1.6, [-6, 0, 0]),
                                     (1.7, [0, 0, 0]), (2.0, [0, 0, 0])])},
            "ear_left": {"rotation": kf([(0, EL), (0.9, EL), (1.0, ear_rot(1, -20, -10)), (1.1, EL),
                                         (2.0, EL)])},
            "ear_right": {"rotation": kf([(0, ER), (1.5, ER), (1.6, ear_rot(-1, -20, -10)), (1.7, ER),
                                          (2.0, ER)])}}
    # quick little trots: short steps, a bouncy body, the tail wagging along
    L = 0.3
    walk = {"body": {"position": kf([(0, [0, 0, 0]), (L / 4, [0, 0.45, 0]), (L / 2, [0, 0, 0]), (3 * L / 4, [0, 0.45, 0]), (L, [0, 0, 0])]),
                     "rotation": kf([(0, [0, 0, 3]), (L / 2, [0, 0, -3]), (L, [0, 0, 3])])}}
    for n, ph in (("foot_front_left", 0), ("foot_back_right", 0), ("foot_front_right", 1), ("foot_back_left", 1)):
        s = 0.7 if ph == 0 else -0.7
        walk[n] = {"position": kf([(0, [0, 0, s]), (L / 2, [0, 0.3, -s]), (L, [0, 0, s])])}
    blij = {"body": {"position": kf([(0, [0, 0, 0]), (0.12, [0, 2.2, 0]), (0.25, [0, 0, 0]), (0.37, [0, 1.6, 0]), (0.5, [0, 0, 0])]),
                     "rotation": kf([(0, [0, 0, 0]), (0.25, [0, 180, 0]), (0.5, [0, 360, 0])])},
            "ear_left": {"rotation": kf([(0, EL), (0.15, ear_rot(1, -40, -16)), (0.5, EL)])},
            "ear_right": {"rotation": kf([(0, ER), (0.15, ear_rot(-1, -40, -16)), (0.5, ER)])}}
    eet = {"head": {"rotation": kf([(t, [(16 if i % 2 else 4), 0, 0]) for i, t in enumerate([0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8])] +
                                   [(0.9, [0, 0, 0])])},
           "body": {"scale": kf([(0, [1, 1, 1]), (0.45, [1.04, 0.96, 1.02]), (0.9, [1, 1, 1])])}}
    piep = {"head": {"rotation": kf([(0, [0, 0, 0]), (0.08, [-16, 0, 0]), (0.3, [-12, 0, 0]), (0.4, [0, 0, 0])]),
                     "scale": kf([(0, [1, 1, 1]), (0.08, [1.08, 1.1, 1.06]), (0.3, [1.04, 1.06, 1.03]), (0.4, [1, 1, 1])])},
            "ear_left": {"rotation": kf([(0, EL), (0.08, ear_rot(1, -25, -12)), (0.4, EL)])},
            "ear_right": {"rotation": kf([(0, ER), (0.08, ear_rot(-1, -25, -12)), (0.4, ER)])}}
    zit = {"body": {"scale": kf([(0, [1.03, 0.95, 1.03]), (1.0, [1.05, 0.93, 1.04]), (2.0, [1.03, 0.95, 1.03])])},
           "foot_front_left": {"position": kf([(0, [0, 0.2, -0.3])])}, "foot_front_right": {"position": kf([(0, [0, 0.2, -0.3])])}}
    return {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 2.0, "bones": idle},
        "walk": {"loop": True, "animation_length": L, "bones": walk},
        "blij": {"loop": False, "animation_length": 0.5, "bones": blij},
        "eet": {"loop": False, "animation_length": 0.9, "bones": eet},
        "piep": {"loop": False, "animation_length": 0.4, "bones": piep},
        "zit": {"loop": True, "animation_length": 2.0, "bones": zit}}}


# =====================================================================================================================
# Poepschilly: a green sea turtle plush (domed mossy shell, cream head and flippers with mottles, amber-rimmed eyes)
# =====================================================================================================================
def schild_top(W, H, rng):
    """The top of the shell: moss green terry with a darker seam down the middle (front to back)."""
    a = plain(SCHILD, 12, specks(SCHILD_RAND, 18), clump=2)(W, H, rng).astype(np.float32)
    for y in range(H):
        x = W / 2 + math.sin(y * 0.35) * 0.6
        for dx in (-1, 0):
            xi = int(x) + dx
            if 0 <= xi < W:
                a[y, xi, :3] = SCHILD_DONKER
    return np.clip(a, 0, 255).astype(np.uint8)


def schild_zij(W, H, rng):
    a = plain(SCHILD, 12, specks(SCHILD_RAND, 18))(W, H, rng).astype(np.float32)
    a[:1, :, :3] = np.array(SCHILD_RAND)
    return np.clip(a, 0, 255).astype(np.uint8)


def schilly_eye_side(front_is_left):
    """A side of the head: cream with mottles, the amber-rimmed black eye near the front."""
    def paint(W, H, rng):
        a = plain(HUID, 8, schubben(VLEK, 3.6))(W, H, rng).astype(np.float32)
        cx = W * (0.3 if front_is_left else 0.7)
        cy = H * 0.4
        r = max(2.4, min(W, H) * 0.26)
        disc(a, cx, cy, r + 1.2, HUID)
        disc(a, cx, cy, r, AMBER)
        disc(a, cx, cy, r * 0.72, OOG)
        disc(a, cx + r * 0.25, cy - r * 0.3, r * 0.26, (255, 255, 255))
        # a cream lower half (the throat)
        a[int(H * 0.72):, :, :3] = np.array(BUIK) + rng.normal(0, 4, (H - int(H * 0.72), W, 1))
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def schilly_snoet(W, H, rng):
    a = plain(HUID, 8, schubben(VLEK, 3.6))(W, H, rng).astype(np.float32)
    a[int(H * 0.6):, :, :3] = np.array(BUIK) + rng.normal(0, 4, (H - int(H * 0.6), W, 1))
    line(a, W * 0.3, H * 0.66, W * 0.7, H * 0.66, (150, 120, 96), 1.0)     # a little smile
    return np.clip(a, 0, 255).astype(np.uint8)


def schilly(atlas):
    top = schild_top
    zij = schild_zij
    rand = plain(SCHILD_RAND, 10, specks(SCHILD, 20))
    buik = plain(BUIK, 6)
    vlek = plain(HUID, 8, schubben(VLEK, 4.6))
    huid = plain(HUID, 6)
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 1.5, 0], [cube(atlas, [-2.8, 0.4, -2.8], [5.6, 1.1, 6.2], buik)]),
             bone("shell", "body", [0, 2, 0], [
                 cube(atlas, [-3.5, 1.3, -3.2], [7.0, 0.7, 7.0], rand, overrides={"down": buik}),
                 cube(atlas, [-3.1, 2.0, -2.9], [6.2, 1.3, 6.4], zij, overrides={"up": top}),
                 cube(atlas, [-2.3, 3.3, -2.2], [4.6, 0.9, 5.0], zij, overrides={"up": top})]),
             bone("head", "body", [0, 1.8, -3.0], [
                 cube(atlas, [-1.1, 1.0, -3.6], [2.2, 1.6, 0.9], vlek, overrides={"down": buik}),
                 cube(atlas, [-1.45, 0.9, -6.0], [2.9, 2.3, 2.6], vlek, overrides={
                     "north": schilly_snoet, "east": schilly_eye_side(True), "west": schilly_eye_side(False), "down": buik})])]
    # big front flippers (swept back), small back flippers: mottled on top, cream underneath
    for side, sx in (("left", 1), ("right", -1)):
        o = [2.6, 1.0, -2.3]
        size = [3.5, 0.5, 1.6]
        bones.append(bone(f"flipper_front_{side}", "body", [sx * 2.7, 1.25, -1.6],
                          [cube(atlas, o if sx > 0 else mirror_x(o, size), size, vlek, overrides={"down": huid})],
                          rotation=[0, sx * 32, sx * -6]))
        o2 = [2.2, 0.8, 2.4]
        size2 = [2.5, 0.45, 1.4]
        bones.append(bone(f"flipper_back_{side}", "body", [sx * 2.3, 1.0, 2.9],
                          [cube(atlas, o2 if sx > 0 else mirror_x(o2, size2), size2, vlek, overrides={"down": huid})],
                          rotation=[0, sx * -28, 0]))
    bones.append(bone("tail", "body", [0, 1.1, 3.4], [cube(atlas, [-0.4, 0.8, 3.3], [0.8, 0.5, 1.0], huid)]))
    return geo("poepschilly", bones, 0.8, 0.5)


def schilly_anims():
    fl, fr = [0, 32, -6], [0, -32, 6]
    bl, br = [0, -28, 0], [0, 28, 0]

    def add(a, b):
        return [a[i] + b[i] for i in range(3)]
    idle = {"shell": {"scale": kf([(0, [1, 1, 1]), (1.5, [1.02, 1.03, 1.02]), (3.0, [1, 1, 1])])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (1.0, [4, 10, 0]), (2.0, [4, -10, 0]), (3.0, [0, 0, 0])])},
            "flipper_front_left": {"rotation": kf([(0, fl), (1.5, add(fl, [0, 0, -6])), (3.0, fl)])},
            "flipper_front_right": {"rotation": kf([(0, fr), (1.5, add(fr, [0, 0, 6])), (3.0, fr)])},
            "flipper_back_left": {"rotation": kf([(0, bl)])}, "flipper_back_right": {"rotation": kf([(0, br)])}}
    L = 1.0
    walk = {"body": {"rotation": kf([(0, [0, 0, 2]), (L / 2, [0, 0, -2]), (L, [0, 0, 2])]),
                     "position": kf([(0, [0, 0, 0]), (L / 4, [0, 0.3, 0]), (L / 2, [0, 0, 0]), (3 * L / 4, [0, 0.3, 0]), (L, [0, 0, 0])])},
            "flipper_front_left": {"rotation": kf([(0, add(fl, [0, 25, 0])), (L / 2, add(fl, [0, -20, 0])), (L, add(fl, [0, 25, 0]))])},
            "flipper_front_right": {"rotation": kf([(0, add(fr, [0, 20, 0])), (L / 2, add(fr, [0, -25, 0])), (L, add(fr, [0, 20, 0]))])},
            "flipper_back_left": {"rotation": kf([(0, add(bl, [0, -15, 0])), (L / 2, add(bl, [0, 15, 0])), (L, add(bl, [0, -15, 0]))])},
            "flipper_back_right": {"rotation": kf([(0, add(br, [0, -15, 0])), (L / 2, add(br, [0, 15, 0])), (L, add(br, [0, -15, 0]))])},
            "head": {"rotation": kf([(0, [0, 6, 0]), (L / 2, [0, -6, 0]), (L, [0, 6, 0])])}}
    Ls = 1.6
    swim = {"body": {"position": kf([(0, [0, 0, 0]), (Ls / 2, [0, 0.6, 0]), (Ls, [0, 0, 0])])},
            "flipper_front_left": {"rotation": kf([(0, add(fl, [0, 10, -35])), (Ls / 2, add(fl, [0, -25, 30])), (Ls, add(fl, [0, 10, -35]))])},
            "flipper_front_right": {"rotation": kf([(0, add(fr, [0, -10, 35])), (Ls / 2, add(fr, [0, 25, -30])), (Ls, add(fr, [0, -10, 35]))])},
            "flipper_back_left": {"rotation": kf([(0, add(bl, [0, -12, 0])), (Ls / 2, add(bl, [0, 12, 0])), (Ls, add(bl, [0, -12, 0]))])},
            "flipper_back_right": {"rotation": kf([(0, add(br, [0, 12, 0])), (Ls / 2, add(br, [0, -12, 0])), (Ls, add(br, [0, 12, 0]))])}}
    blij = {"body": {"position": kf([(0, [0, 0, 0]), (0.15, [0, 1.5, 0]), (0.3, [0, 0, 0]), (0.45, [0, 1.0, 0]), (0.6, [0, 0, 0])])},
            "flipper_front_left": {"rotation": kf([(0, fl), (0.15, add(fl, [0, 0, -40])), (0.3, fl), (0.45, add(fl, [0, 0, -40])), (0.6, fl)])},
            "flipper_front_right": {"rotation": kf([(0, fr), (0.15, add(fr, [0, 0, 40])), (0.3, fr), (0.45, add(fr, [0, 0, 40])), (0.6, fr)])},
            "head": {"rotation": kf([(0, [0, 0, 0]), (0.3, [-15, 0, 0]), (0.6, [0, 0, 0])])}}
    # kruip: crawls into the guh (shrinks and tucks in: the game hides it after this)
    kruip = {"head": {"position": kf([(0, [0, 0, 0]), (0.4, [0, 0, 1.2])])},
             "body": {"scale": kf([(0, [1, 1, 1]), (0.5, [0.9, 0.8, 0.9]), (0.8, [0.2, 0.2, 0.2])]),
                      "position": kf([(0, [0, 0, 0]), (0.8, [0, 2, -3])])}}
    return {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 3.0, "bones": idle},
        "walk": {"loop": True, "animation_length": L, "bones": walk},
        "swim": {"loop": True, "animation_length": Ls, "bones": swim},
        "blij": {"loop": False, "animation_length": 0.6, "bones": blij},
        "kruip": {"loop": False, "animation_length": 0.8, "bones": kruip}}}


# =====================================================================================================================
# the boze kaasknabbel (and the Boze Oppernabbel): a lumpy cheese puff with angry eyebrows
# =====================================================================================================================
KNABBEL_H = 14.0          # the crescent's height (model px); the renderers scale it down
KNABBEL_W = 4.8           # widest, seam to seam
KNABBEL_D_RAND = 2.2      # thickness at the seam
KNABBEL_D_MIDDEN = 3.4    # thickness in the middle (the lens shape)
KNABBEL_BOOG = 2.4        # how far the middle bows out (+x) compared with the ends
KNABBEL_SEGMENTEN = 10


def knabbel_x(y):
    """The crescent's centre line: the middle bows out to +x, both ends curl back to -x."""
    t = (y - KNABBEL_H / 2) / (KNABBEL_H / 2)
    return KNABBEL_BOOG * (1 - t * t) - KNABBEL_BOOG / 2


def knabbel_w(y):
    """Rounded ends: narrower near the top and bottom."""
    t = abs(y - KNABBEL_H / 2) / (KNABBEL_H / 2)
    return KNABBEL_W * (0.5 + 0.5 * math.sqrt(max(0.0, 1 - t ** 3)))


def plush(colour):
    """Soft short plush: warm orange-yellow with slightly lighter highlights."""
    return plain(colour, 7, specks(tuple(min(255, c + 16) for c in colour), 26), clump=3)


def seam(colour):
    """A side face: the sewn seam ridge running along the middle."""
    def paint(W, H, rng):
        a = plush(colour)(W, H, rng).astype(np.float32)
        mid = W / 2
        for x in range(W):
            d = abs(x + 0.5 - mid)
            if d < max(1.0, W * 0.07):
                a[:, x, :3] = np.array(colour) * 0.84
            elif d < max(2.0, W * 0.16):
                a[:, x, :3] = np.minimum(255, np.array(colour) * 1.06)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def knabbel_face(colour, boss=False):
    """The boze-but-zieli face: open frowning eyes (flat, cross top edge), a pouty mouth and the pink blush dots."""
    def paint(W, H, rng):
        a = plush(colour)(W, H, rng).astype(np.float32)
        ink = (26, 18, 16)
        r = W * 0.14
        for sx in (-1, 1):
            cx, cy = W / 2 + sx * W * 0.24, H * 0.42
            # the eye: a black oval, its top cut off at a slant (lower towards the middle: cross)
            for y in range(H):
                for x in range(W):
                    dx, dy = (x + 0.5 - cx) / r, (y + 0.5 - cy) / (r * 1.2)
                    if dx * dx + dy * dy > 1:
                        continue
                    t = (x + 0.5 - cx) * -sx / r               # +1 towards the middle
                    if dy < -0.15 + 0.45 * t:
                        continue
                    a[y, x, :3] = ink
            disc(a, cx - sx * r * 0.25, cy + r * 0.25, max(1.0, r * 0.28), (255, 255, 255))
            # a little lash flick at the outer corner
            line(a, cx + sx * r * 0.9, cy + r * 0.1, cx + sx * r * 1.35, cy - r * 0.25, ink, max(1.0, W * 0.03))
            # blush
            disc(a, cx + sx * r * 0.7, cy + r * 1.9, r * 0.5, (240, 128, 150), 0.85, ry=r * 0.36)
        # the pout: a small downward arc
        mw = W * (0.14 if not boss else 0.18)
        for i in range(int(mw * 4) + 1):
            x = W / 2 - mw + i * 0.5
            y = H * 0.74 - math.cos((x - W / 2) / mw * math.pi / 2) * H * 0.05
            disc(a, x, y, max(0.9, W * 0.028), ink)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def knabbel_bones(atlas, colour, crown=False):
    stof = plush(colour)
    rand = seam(colour)
    seg_h = KNABBEL_H / KNABBEL_SEGMENTEN
    body = []
    for i in range(KNABBEL_SEGMENTEN):
        y0 = i * seg_h
        yc = y0 + seg_h / 2
        x, w = knabbel_x(yc), knabbel_w(yc)
        end = i in (0, KNABBEL_SEGMENTEN - 1)
        d_r = KNABBEL_D_RAND * (0.8 if end else 1.0)
        d_m = KNABBEL_D_MIDDEN * (0.8 if end else 1.0)
        # the seam part (full width, thinner) and the thick middle (the lens)
        body.append(cube(atlas, [x - w / 2, y0, -d_r / 2], [w, seg_h, d_r], stof, overrides={"east": rand, "west": rand}))
        body.append(cube(atlas, [x - w / 2 + 0.7, y0, -d_m / 2], [w - 1.4, seg_h, d_m], stof))
    # the rounded end caps
    for y, top in ((-0.35, False), (KNABBEL_H, True)):
        yc = KNABBEL_H - 0.2 if top else 0.2
        x, w = knabbel_x(yc), knabbel_w(yc) * 0.72
        body.append(cube(atlas, [x - w / 2, y, -KNABBEL_D_MIDDEN * 0.34], [w, 0.35, KNABBEL_D_MIDDEN * 0.68], stof))
    fy = KNABBEL_H * 0.6                                         # the face: about 40 % from the top
    fx, fw = knabbel_x(fy), knabbel_w(fy) - 1.3
    fh = 3.9
    bones = [bone("root", None, [0, 0, 0], []),
             bone("body", "root", [0, 0, 0], body),
             bone("face", "body", [fx, fy, -KNABBEL_D_MIDDEN / 2], [
                 cube(atlas, [fx - fw / 2, fy - fh / 2, -KNABBEL_D_MIDDEN / 2 - 0.12], [fw, fh, 0.12], stof,
                      overrides={"north": knabbel_face(colour, crown)})])]
    brauw = plain((52, 26, 16), 5)
    for side, sx in (("left", 1), ("right", -1)):
        size = [1.5 if not crown else 1.8, 0.4, 0.25]
        cx = fx + sx * fw * 0.24
        o = [cx - size[0] / 2, fy + 0.95, -KNABBEL_D_MIDDEN / 2 - 0.3]
        bones.append(bone(f"brow_{side}", "face", [cx, fy + 1.15, -KNABBEL_D_MIDDEN / 2 - 0.2], [cube(atlas, o, size, brauw)],
                          rotation=[0, 0, sx * 24]))
    if crown:
        goud = plain(GOUD, 8, specks((255, 236, 150), 16))
        punt = plain(GOUD_DONKER, 6)
        tx = knabbel_x(KNABBEL_H - 0.5)
        ty = KNABBEL_H + 0.3
        cubes = [cube(atlas, [tx - 1.6, ty, -1.3], [3.2, 0.9, 2.6], goud)]
        for dx, dz in ((-1.6, -1.3), (0.9, -1.3), (-1.6, 0.6), (0.9, 0.6), (-0.35, -1.3)):
            cubes.append(cube(atlas, [tx + dx, ty + 0.9, dz], [0.7, 0.8, 0.7], punt))
        cubes.append(cube(atlas, [tx - 0.35, ty + 0.2, -1.5], [0.7, 0.55, 0.25], solid((236, 90, 150))))   # a pink knabbel gem
        bones.append(bone("crown", "body", [tx, ty, 0], cubes, rotation=[0, 0, 14]))
    return bones


def kaasknabbel(atlas):
    return geo("boze_kaasknabbel", knabbel_bones(atlas, KAAS), 0.6, 1.0)


def oppernabbel(atlas):
    return geo("boze_oppernabbel", knabbel_bones(atlas, KAAS_BOOS, crown=True), 0.6, 1.1)


def knabbel_anims(boss=False):
    L = 0.5
    BL, BR = [0, 0, 24], [0, 0, -24]
    # it rocks a little on its bottom end, the brows twitch
    idle = {"body": {"rotation": kf([(0, [0, 0, -3]), (0.6, [0, 0, 3]), (1.2, [0, 0, -3])]),
                     "scale": kf([(0, [1, 1, 1]), (0.6, [1.03, 0.97, 1.03]), (1.2, [1, 1, 1])])},
            "brow_left": {"rotation": kf([(0, BL), (0.6, [0, 0, 32]), (1.2, BL)])},
            "brow_right": {"rotation": kf([(0, BR), (0.6, [0, 0, -32]), (1.2, BR)])}}
    # hop hop on its end: squash, jump with a wiggle, stretch, land
    walk = {"body": {"position": kf([(0, [0, 0, 0]), (0.1, [0, 0, 0]), (0.25, [0, 3.5, 0]), (0.4, [0, 0, 0]), (L, [0, 0, 0])]),
                     "scale": kf([(0, [1.1, 0.88, 1.1]), (0.1, [1.08, 0.9, 1.08]), (0.2, [0.94, 1.1, 0.94]), (0.35, [1, 1, 1]),
                                  (0.42, [1.12, 0.86, 1.12]), (L, [1.1, 0.88, 1.1])]),
                     "rotation": kf([(0, [0, 0, 0]), (0.2, [0, 0, 8]), (0.3, [0, 0, -8]), (0.4, [0, 0, 0])])}}
    aanval = {"body": {"rotation": kf([(0, [0, 0, 0]), (0.15, [12, 0, 0]), (0.3, [-30, 0, 0]), (0.5, [0, 0, 0])]),
                       "position": kf([(0, [0, 0, 0]), (0.3, [0, 1, -2.5]), (0.5, [0, 0, 0])])},
              "brow_left": {"rotation": kf([(0, BL), (0.2, [0, 0, 38]), (0.5, BL)])},
              "brow_right": {"rotation": kf([(0, BR), (0.2, [0, 0, -38]), (0.5, BR)])}}
    # zieli...: the brows go up (sad instead of cross) and it flops over onto its back
    zieli = {"brow_left": {"rotation": kf([(0, BL), (0.3, [0, 0, -20])])},
             "brow_right": {"rotation": kf([(0, BR), (0.3, [0, 0, 20])])},
             "body": {"rotation": kf([(0, [0, 0, 0]), (0.3, [0, 0, 0]), (0.8, [80, 0, 0]), (0.9, [70, 0, 0]), (1.0, [78, 0, 0])])}}
    anims = {
        "idle": {"loop": True, "animation_length": 1.2, "bones": idle},
        "walk": {"loop": True, "animation_length": L, "bones": walk},
        "aanval": {"loop": False, "animation_length": 0.5, "bones": aanval},
        "zieli": {"loop": "hold_on_last_frame", "animation_length": 1.0, "bones": zieli}}
    if boss:
        idle["crown"] = {"rotation": kf([(0, [0, 0, 14]), (0.6, [0, 0, 8]), (1.2, [0, 0, 14])])}
        # stamp: a big jump and a slam (the shockwave is the game's)
        anims["stamp"] = {"loop": False, "animation_length": 1.0, "bones": {
            "body": {"position": kf([(0, [0, 0, 0]), (0.2, [0, -1, 0]), (0.5, [0, 9, 0]), (0.7, [0, 0, 0]), (1.0, [0, 0, 0])]),
                     "scale": kf([(0, [1, 1, 1]), (0.2, [1.2, 0.8, 1.2]), (0.45, [0.9, 1.15, 0.9]), (0.7, [1.3, 0.7, 1.3]), (1.0, [1, 1, 1])])},
            "crown": {"position": kf([(0, [0, 0, 0]), (0.5, [0, 1.5, 0]), (0.7, [0, 0, 0])])}}}
    return {"format_version": "1.8.0", "animations": anims}


# =====================================================================================================================
# the roze guh koek: a block model (on a saucer, 4 bites) + its textures
# =====================================================================================================================


def koek_top(size=64):
    """The pink glaze with the big glossy blue eyes, the little nose triangle and the soft mouth lines (from the photo)."""
    rng = np.random.default_rng(2811)
    a = np.zeros((size, size, 4), np.float32)
    a[..., :3] = KOEK_ROZE
    a[..., :3] += rng.normal(0, 3, (size, size, 1))
    a[..., 3] = 255
    s = size / 64
    # a soft sheen
    disc(a, 22 * s, 14 * s, 12 * s, KOEK_ROZE_LICHT, 0.35, ry=6 * s)
    for sx in (-1, 1):
        cx, cy = 32 + sx * 13.5, 24
        cx, cy = cx * s, cy * s
        rx, ry = 11.5 * s, 10 * s
        disc(a, cx, cy, rx, (50, 18, 44), ry=ry)                      # dark outline
        disc(a, cx, cy + 0.8 * s, rx * 0.86, (40, 160, 210), ry=ry * 0.82)    # blue iris
        disc(a, cx, cy - 1.2 * s, rx * 0.78, (30, 16, 40), ry=ry * 0.66)      # the big dark pupil (upper part)
        disc(a, cx - sx * 2.5 * s, cy - 2.2 * s, 3.0 * s, (255, 252, 248), ry=2.6 * s)   # white shine
        disc(a, cx + sx * 3.2 * s, cy + 2.4 * s, 1.6 * s, (255, 252, 248))
        # the dark lash line on top
        for i in range(-11, 12):
            x = cx + i * s
            y = cy - ry * math.sqrt(max(0, 1 - (i / 11.2) ** 2)) - 0.4 * s
            disc(a, x, y, 1.0 * s, (40, 12, 34))
    # the snoet: a line with a little brown triangle under it
    line(a, 23 * s, 38 * s, 41 * s, 38.5 * s, (130, 44, 66), 1.2 * s)
    for i in range(6):
        w = (6 - i) * 0.6 * s
        line(a, 32 * s - w, (39.5 + i * 0.8) * s, 32 * s + w, (39.5 + i * 0.8) * s, (120, 40, 56), 1.0 * s)
    line(a, 28 * s, 45 * s, 36 * s, 45 * s, (130, 44, 66), 1.0 * s)
    # the double smile near the bottom
    for off, half in ((0, 11), (3, 13)):
        for i in range(-half * 2, half * 2 + 1):
            x = 32 * s + i * 0.5 * s
            y = (52 + off) * s - math.cos(i / (half * 2) * math.pi / 2) * 1.5 * s + 1.5 * s
            disc(a, x, y, 0.7 * s, (130, 44, 66))
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def koek_side(size=32):
    rng = np.random.default_rng(2812)
    a = np.zeros((size, size, 4), np.float32)
    a[..., 3] = 255
    s = size / 16
    for y in range(size):
        for x in range(size):
            if y < 1.3 * s + max(0.0, math.sin(x * 0.7)) * 0.9 * s:          # the glaze, dripping a little
                a[y, x, :3] = np.array(KOEK_ROZE) + rng.normal(0, 4)
            else:
                a[y, x, :3] = np.array(KOEK_RAND) + rng.normal(0, 9)
                if rng.random() < 0.08:
                    a[y, x, :3] = KOEK_RAND_DONKER
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def bakje_tex(size=32):
    """The clear plastic tray: see-through, a little milky, with brighter creases and edges."""
    a = np.zeros((size, size, 4), np.float32)
    a[..., :3] = (236, 240, 246)
    a[..., 3] = 70
    for i in range(size):
        a[i, 0, 3] = a[i, -1, 3] = a[0, i, 3] = a[-1, i, 3] = 150
        a[i, 0, :3] = a[i, -1, :3] = a[0, i, :3] = a[-1, i, :3] = (250, 252, 255)
    for k in range(3):                                        # a few shiny creases
        x0 = int(size * (0.2 + 0.3 * k))
        for i in range(size):
            x = x0 + i // 3
            if 0 <= x < size:
                a[i, x, 3] = 120
                a[i, x, :3] = (255, 255, 255)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


# the tray (facing north): 12 wide (x 2..14), 15 long (z 0.5..15.5); the koeken in 2 columns x 3 rows, filled row by row
BAKJE = (2.0, 14.0, 0.5, 15.5)
KOEK_B, KOEK_D = 5.0, 4.6
KOEK_PLEKKEN = [(x, z) for z in (0.9, 5.7, 10.5) for x in (2.6, 8.4)]


def _box(frm, to, faces):
    return {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": faces}


def _alle(tex, uv=(0, 0, 16, 16), skip=()):
    return {f: {"uv": list(uv), "texture": tex} for f in ("north", "south", "east", "west", "up", "down") if f not in skip}


def koek_elements(koeken):
    """Block model elements: the plastic tray and `koeken` (1-6) koeken in it, each with its face on top."""
    x0, x1, z0, z1 = BAKJE
    els = [_box([x0, 0, z0], [x1, 0.5, z1], _alle("#bakje")),
           _box([x0, 0.5, z0], [x1, 2.3, z0 + 0.3], _alle("#bakje", skip=("down",))),
           _box([x0, 0.5, z1 - 0.3], [x1, 2.3, z1], _alle("#bakje", skip=("down",))),
           _box([x0, 0.5, z0 + 0.3], [x0 + 0.3, 2.3, z1 - 0.3], _alle("#bakje", skip=("down",))),
           _box([x1 - 0.3, 0.5, z0 + 0.3], [x1, 2.3, z1 - 0.3], _alle("#bakje", skip=("down",)))]
    side = {f: {"uv": [0, 0, 16, 6], "texture": "#side"} for f in ("north", "south", "east", "west")}
    for (x, z) in KOEK_PLEKKEN[:koeken]:
        body = dict(side)
        body["down"] = {"uv": [0, 0, 16, 16], "texture": "#side"}
        els.append(_box([x, 0.5, z], [x + KOEK_B, 2.1, z + KOEK_D], body))
        cap = {f: {"uv": [0, 0, 16, 1], "texture": "#side"} for f in ("north", "south", "east", "west")}
        cap["up"] = {"uv": [0, 0, 16, 16], "texture": "#top"}
        els.append(_box([x + 0.3, 2.1, z + 0.3], [x + KOEK_B - 0.3, 2.6, z + KOEK_D - 0.3], cap))
    return els


def koek_model(koeken):
    return {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "textures": {
        "particle": "guhs:block/roze_guh_koek_top", "top": "guhs:block/roze_guh_koek_top", "side": "guhs:block/roze_guh_koek_side",
        "bakje": "guhs:block/roze_guh_koek_bakje"},
        "elements": koek_elements(koeken)}


# =====================================================================================================================
# Blockbench projects
# =====================================================================================================================
def _uid(*parts):
    return str(_uuid.uuid5(_uuid.NAMESPACE_URL, "guhs-piep/" + "/".join(str(p) for p in parts)))


def _png_data(img):
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def _texture_entry(name, rel_path, img, uv_w, uv_h, idx=0):
    return {"name": name, "relative_path": rel_path, "folder": "", "namespace": "", "id": str(idx), "group": "", "scope": 0,
            "width": img.width, "height": img.height, "uv_width": uv_w, "uv_height": uv_h, "particle": idx == 0, "use_as_default": False,
            "layers_enabled": False, "sync_to_project": "", "file_format": "png", "render_mode": "default", "render_sides": "auto",
            "wrap_mode": "limited", "pbr_channel": "color", "fps": 7, "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
            "frame_interpolate": False, "visible": True, "internal": True, "saved": True, "uuid": _uid(name, "tex"),
            "source": _png_data(img)}


def bbmodel_entity(name, geo_file, anims, img):
    """A Blockbench (bedrock entity) project from a geo + animations + texture, like blockbench/guh.bbmodel."""
    g = geo_file["minecraft:geometry"][0]
    tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
    groups, elements, children = [], [], {}
    gid = {}
    for i, b in enumerate(g["bones"]):
        gid[b["name"]] = _uid(name, "bone", b["name"])
        rot = b.get("rotation", [0, 0, 0])
        groups.append({"name": b["name"], "uuid": gid[b["name"]], "export": True, "locked": False, "scope": 0, "selected": False,
                       "visibility": True, "_static": {"properties": {}, "temp_data": {}},
                       "origin": [-b["pivot"][0], b["pivot"][1], b["pivot"][2]], "rotation": [-rot[0], -rot[1], rot[2]],
                       "bedrock_binding": "", "color": i % 8, "children": [], "reset": False, "shade": True, "mirror_uv": False,
                       "autouv": 0, "isOpen": True, "primary_selected": False})
        children[b["name"]] = []
        for j, c in enumerate(b.get("cubes", [])):
            (ox, oy, oz), (sx, sy, sz) = c["origin"], c["size"]
            eid = _uid(name, "cube", b["name"], j)
            el = {"name": b["name"], "box_uv": False, "render_order": "default", "locked": False, "export": True, "scope": 0,
                  "allow_mirror_modeling": True, "from": [-(ox + sx), oy, oz], "to": [-ox, oy + sy, oz + sz], "autouv": 0,
                  "color": i % 8, "origin": [-b["pivot"][0], b["pivot"][1], b["pivot"][2]],
                  "faces": {d: {"uv": [f["uv"][0], f["uv"][1], f["uv"][0] + f["uv_size"][0], f["uv"][1] + f["uv_size"][1]], "texture": 0}
                            for d, f in c["uv"].items()},
                  "type": "cube", "uuid": eid}
            if c.get("inflate"):
                el["inflate"] = c["inflate"]
            elements.append(el)
            children[b["name"]].append(eid)

    def node(bn):
        kids = list(children[bn]) + [node(x["name"]) for x in g["bones"] if x.get("parent") == bn]
        return {"uuid": gid[bn], "isOpen": True, "children": kids}
    outliner = [node(b["name"]) for b in g["bones"] if not b.get("parent")]

    animations = []
    for an, a in anims["animations"].items():
        animators = {}
        for bn, chans in a["bones"].items():
            kfs = []
            for ch, vals in chans.items():
                if isinstance(vals, list):
                    vals = {"0.0": vals}
                for t, v in vals.items():
                    kfs.append({"channel": ch, "data_points": [{"x": v[0], "y": v[1], "z": v[2]}], "uuid": _uid(name, an, bn, ch, t),
                                "time": float(t), "color": -1, "interpolation": "linear", "bezier_linked": True,
                                "bezier_left_time": [-0.1, -0.1, -0.1], "bezier_left_value": [0, 0, 0],
                                "bezier_right_time": [0.1, 0.1, 0.1], "bezier_right_value": [0, 0, 0]})
            animators[gid[bn]] = {"name": bn, "type": "bone", "keyframes": kfs}
        loop = a.get("loop")
        animations.append({"uuid": _uid(name, "anim", an), "name": an,
                           "loop": "loop" if loop is True else ("hold" if loop == "hold_on_last_frame" else "once"),
                           "override": False, "length": a["animation_length"], "snapping": 24, "selected": False,
                           "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators})
    return {"meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": False},
            "name": name, "model_identifier": name, "visible_box": [g["description"]["visible_bounds_width"],
                                                                     g["description"]["visible_bounds_height"], 0.25],
            "variable_placeholders": "", "multi_file_ruleset": "", "variable_placeholder_buttons": [], "bedrock_animation_mode": "entity",
            "timeline_setups": [], "unhandled_root_fields": {}, MARKER: True,
            "resolution": {"width": tw, "height": th}, "elements": elements, "groups": groups, "outliner": outliner,
            "textures": [_texture_entry(f"{name}.png", f"../src/main/resources/assets/guhs/textures/entity/{name}.png", img, tw, th)],
            "animations": animations}


def bbmodel_block(name, model, textures):
    """A Blockbench (java block) project from a block model and its {var: (image, game path)} textures."""
    order = list(textures)
    elements = []
    for j, e in enumerate(model["elements"]):
        faces = {}
        for d, f in e["faces"].items():
            faces[d] = {"uv": f["uv"], "texture": order.index(f["texture"].lstrip("#"))}
        elements.append({"name": "bakje" if j < 5 else ("koek" if (j - 5) % 2 == 0 else "glazuur"), "box_uv": False, "render_order": "default", "locked": False,
                         "export": True, "scope": 0, "allow_mirror_modeling": True, "from": e["from"], "to": e["to"], "autouv": 0,
                         "color": j, "origin": [8, 8, 8], "faces": faces, "type": "cube", "uuid": _uid(name, "el", j)})
    texs = []
    for i, var in enumerate(order):
        img, rel = textures[var]
        t = _texture_entry(var, rel, img, 16, 16, i)
        t["name"] = var
        texs.append(t)
    return {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": name, "parent": "block/block",
            "ambientocclusion": True, "front_gui_light": False, "visible_box": [1, 1, 0], "variable_placeholders": "",
            "variable_placeholder_buttons": [], "timeline_setups": [], "unhandled_root_fields": {}, MARKER: True,
            "resolution": {"width": 16, "height": 16}, "elements": elements,
            "outliner": [e["uuid"] for e in elements], "textures": texs, "display": {}}


# =====================================================================================================================
ENTITIES = (("pieppiepmuisje", muisje, muisje_anims, 28101, 4), ("poepschilly", schilly, schilly_anims, 28102, 4),
            ("boze_kaasknabbel", kaasknabbel, lambda: knabbel_anims(False), 28103, 8),
            ("boze_oppernabbel", oppernabbel, lambda: knabbel_anims(True), 28104, 8))


def _hand_edited(path):
    if not os.path.exists(path):
        return False
    try:
        return not json.load(open(path, encoding="utf-8")).get(MARKER, False)
    except (OSError, ValueError):
        return True


def koek_textures():
    return {"top": koek_top(), "side": koek_side(), "bakje": bakje_tex()}


def build(h):
    """The models are the user's now: blockbench/*.bbmodel is edited by hand and exported to the game files. This script only
    (re)writes them when asked: PIEP_MODELLEN=1 writes the game files (geo, animations, textures, koek block models) from the
    code above, and PIEP_BBMODELS=1 also rewrites the Blockbench projects (never by default)."""
    if os.environ.get("PIEP_MODELLEN") != "1":
        return
    A = h.A
    bbmodels = os.environ.get("PIEP_BBMODELS") == "1"
    for name, maker, anims, seed, texels in ENTITIES:
        atlas = Atlas(seed, s=texels)
        g = maker(atlas)
        an = anims()
        img = Image.fromarray(atlas.img)
        h.w(f"{A}/geckolib/models/entity/{name}.geo.json", g)
        h.w(f"{A}/geckolib/animations/entity/{name}.animation.json", an)
        h.save(img, "entity", f"{name}.png")
        if bbmodels:
            os.makedirs(BB_DIR, exist_ok=True)
            with open(os.path.join(BB_DIR, f"{name}.bbmodel"), "w", encoding="utf-8") as f:
                json.dump(bbmodel_entity(name, g, an, img), f)
    # the koek block
    koek_block(h, bbmodels)


def koek_block(h, bbmodels=False):
    """The koek block: textures, the six models (1-6 koeken) and (only when asked) the Blockbench project of the full tray."""
    A = h.A
    tex = koek_textures()
    for k, img in tex.items():
        h.save(img, "block", f"roze_guh_koek_{k}.png")
    for n in range(1, 7):
        h.w(f"{A}/models/block/roze_guh_koek_{n}.json", koek_model(n))
    if bbmodels:
        with open(os.path.join(BB_DIR, "roze_guh_koek.bbmodel"), "w", encoding="utf-8") as f:
            json.dump(bbmodel_block("roze_guh_koek", koek_model(6), {
                k: (img, f"../src/main/resources/assets/guhs/textures/block/roze_guh_koek_{k}.png") for k, img in tex.items()}), f)


def check(h):
    problems = []
    for name, *_ in ENTITIES:
        gp = f"{h.A}/geckolib/models/entity/{name}.geo.json"
        if not os.path.exists(gp):
            problems.append(f"piep: missing {gp}")
            continue
        g = json.load(open(gp, encoding="utf-8"))["minecraft:geometry"][0]
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: bone {b['name']} has a missing parent {b['parent']}")
        anims = json.load(open(f"{h.A}/geckolib/animations/entity/{name}.animation.json", encoding="utf-8"))["animations"]
        for an, a in anims.items():
            for bn in a.get("bones", {}):
                if bn not in names:
                    problems.append(f"{name}: animation {an} moves a missing bone {bn}")
        for need in ("idle", "walk"):
            if need not in anims:
                problems.append(f"{name}: no animation {need}")
    return problems


# =====================================================================================================================
def preview():
    """Renders the models from a few sides into .scratch/piep_previews (run from the project root after build)."""
    import sys
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    out = os.path.join(".scratch", "piep_previews")
    os.makedirs(out, exist_ok=True)
    views = (("front", 180 + 25, -18), ("side", 90, -10), ("back", 25, -25), ("top", 200, -65))
    for name, *_ in ([] if os.environ.get("PIEP_PREVIEW") == "koek" else ENTITIES):
        quads = wr.geo_quads(os.path.join(wr.ASSETS, "geckolib", "models", "entity", f"{name}.geo.json"), f"guhs:entity/{name}")
        imgs = [wr.render(quads, yaw=y, pitch=p, size=360) for _, y, p in views]
        sheet = Image.new("RGBA", (360 * len(imgs), 360), (200, 214, 226, 255))
        for i, im in enumerate(imgs):
            sheet.alpha_composite(im, (360 * i, 0))
        sheet.save(os.path.join(out, f"{name}.png"))
    imgs = []
    for n in (1, 3, 6):
        quads = wr.model_quads(f"guhs:block/roze_guh_koek_{n}")
        imgs.append(wr.render(quads, yaw=200, pitch=-40, size=360))
    sheet = Image.new("RGBA", (360 * len(imgs), 360), (200, 214, 226, 255))
    for i, im in enumerate(imgs):
        sheet.alpha_composite(im, (360 * i, 0))
    sheet.save(os.path.join(out, "roze_guh_koek.png"))
    print("previews in", out)


if __name__ == "__main__":
    preview()
