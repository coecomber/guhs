"""
3.0 (Guhverhalen), slice waterdiertjes - the looks: GeckoLib models, animations and textures of

  guhxolotl           a guh that is an axolotl: the round guh head with the big glossy guh eyes, a tiny snoet, pink blush,
                      two little round guh ears and three fluffy gill plumes on each side; a long soft body, four splayed
                      paws and a tail with a fin.  Five colours: roze, mint, choco, wit and (rare) goud (+ a glowmask for
                      the gold one's sparkly spots).
  guh_eendje          a round duck with a guh face and two round guh ears, an orange beak; mama is cream-white with a pink
                      blush, the kuikentjes (babies) use the same model with a fluffy yellow texture.
  knabbelvlindertje   a butterfly with a tiny guh head and big wings with knabbel dots and guh-ear spots, in four colours.
  glimguhtje          a firefly that is a tiny round guh with see-through wings and a glowing belly lantern (glowmask).
  lieveheersbeestje   a ladybird whose black spots are little guh heads (a dot with two round ears), and whose tiny black
                      head has two guh eyes.

Every texture is painted face by face on an atlas (each cube face its own patch, S texels per model pixel), so the faces
don't stretch; the geo files declare the atlas in model units (GeckoLib scales the PNG). Same allocation order = same UVs,
so the colour variants share one geo file.

  build(h)    writes geo/entity/<id>.geo.json, animations/entity/<id>.animation.json and textures/entity/<id>*.png
  check(h)    -> list of problems (bones of every animation exist, parents exist, textures exist, sizes sane)
  preview(out)  (python tools/features/waterdiertjes_modellen.py <dir>) renders all of them with wiki_renders
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

# --- colours --------------------------------------------------------------------------------------------------------------
# guhxolotl: (fur, belly, gills, gill tips, inner ear)
XOLOTL = {
    "roze": ((248, 178, 208), (255, 224, 238), (214, 58, 132), (250, 118, 176), (226, 110, 160)),
    "mint": ((160, 226, 196), (226, 250, 238), (240, 128, 178), (255, 182, 214), (236, 128, 168)),
    "choco": ((170, 116, 88), (222, 184, 156), (244, 136, 170), (255, 186, 206), (236, 128, 168)),
    "wit": ((250, 246, 248), (255, 255, 255), (246, 112, 140), (255, 170, 190), (240, 140, 170)),
    "goud": ((252, 206, 84), (255, 238, 170), (255, 150, 60), (255, 206, 120), (236, 150, 60)),
}
XOLOTL_KLEUREN = list(XOLOTL)
# guh-eendje: (body, wing, belly, beak, feet, inner ear)
EENDJE = {
    "mama": ((255, 248, 236), (244, 232, 220), (255, 240, 244), (250, 150, 60), (246, 140, 50), (240, 150, 180)),
    "kuiken": ((255, 226, 104), (250, 208, 80), (255, 238, 150), (250, 160, 80), (246, 150, 60), (250, 170, 110)),
}
# knabbelvlindertje: (wing, wing edge, dots, body)
VLINDER = {
    "kaasgeel": ((252, 214, 86), (214, 150, 40), (255, 246, 196), (86, 60, 50)),
    "roze": ((248, 160, 200), (206, 90, 150), (255, 232, 244), (96, 56, 76)),
    "mint": ((150, 222, 190), (70, 160, 128), (236, 255, 244), (60, 90, 80)),
    "lila": ((196, 168, 240), (130, 100, 200), (246, 236, 255), (72, 60, 100)),
}
VLINDER_KLEUREN = list(VLINDER)
OUTLINE = (22, 16, 34)


# =====================================================================================================================
# the atlas (like boerderij_dieren.Atlas, with its own scale)
# =====================================================================================================================
class Atlas:
    def __init__(self, units, s, seed):
        self.units, self.s = units, s
        self.img = np.zeros((units * s, units * s, 4), np.uint8)
        self.glow = np.zeros((units * s, units * s, 4), np.uint8)
        self.x = self.y = self.row = 0
        self.seed = seed
        self.rng = np.random.default_rng(seed)

    def alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        if self.x + w > self.units:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > self.units:
            raise SystemExit("waterdiertjes_modellen: the texture atlas is full")
        u, v = self.x, self.y
        self.x += w
        self.row = max(self.row, h)
        return u, v

    def patch(self, w, h, painter, glow=None):
        """A face patch of w x h model pixels painted by painter(W, H, rng) -> RGBA (H, W, 4); glow(W, H, rng) -> the same
        area of the glowmask (only the glowing parts opaque)."""
        u, v = self.alloc(w, h)
        s = self.s
        W, H = int(math.ceil(w)) * s, int(math.ceil(h)) * s
        self.img[v * s:v * s + H, u * s:u * s + W] = painter(W, H, self.rng)
        if glow is not None:
            self.glow[v * s:v * s + H, u * s:u * s + W] = glow(W, H, self.rng)
        return {"uv": [u, v], "uv_size": [w, h]}

    def image(self):
        return Image.fromarray(self.img)

    def glowmask(self):
        return Image.fromarray(self.glow)


# =====================================================================================================================
# painters
# =====================================================================================================================
def disc(a, cx, cy, r, colour, alpha=1.0, ry=None):
    H, W = a.shape[:2]
    ry = ry or r
    y0, y1 = max(0, int(cy - ry - 1)), min(H, int(cy + ry + 2))
    x0, x1 = max(0, int(cx - r - 1)), min(W, int(cx + r + 2))
    if r <= 0 or ry <= 0:
        return
    for y in range(y0, y1):
        for x in range(x0, x1):
            if ((x + 0.5 - cx) / r) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1:
                a[y, x, :3] = a[y, x, :3] * (1 - alpha) + np.array(colour[:3]) * alpha
                a[y, x, 3] = 255


def plain(colour, var=8, extra=None):
    """Soft fur/skin: the colour with a little noise and soft 2x2 clumps (and optional extra(W, H, rng, a))."""
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        a[..., :3] = colour[:3]
        a[..., :3] += rng.normal(0, var / 2, (H, W, 1))
        clump = rng.normal(0, var / 3, ((H + 1) // 2, (W + 1) // 2, 1)).repeat(2, 0).repeat(2, 1)[:H, :W]
        a[..., :3] += clump
        a[..., 3] = 255
        if extra:
            extra(W, H, rng, a)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def transparent(W, H, rng):
    return np.zeros((H, W, 4), np.uint8)


def guh_eyes(a, W, H, eye_y, eye_dx, eye_r, blush=True, fur=None):
    """The big glossy guh eyes (outline thicker on top, dark pupil, a blue->teal ring below, two white highlights) and
    the pink blush under them."""
    r = eye_r * W
    for sx in (-1, 1):
        cx, cy = W / 2 + sx * eye_dx * W, H * eye_y
        for y in range(max(0, int(cy - r * 1.2)), min(H, int(cy + r * 1.2) + 2)):
            for x in range(max(0, int(cx - r - 1)), min(W, int(cx + r) + 2)):
                dx, dy = (x + 0.5 - cx) / r, (y + 0.5 - cy) / (r * 1.08)
                d = math.hypot(dx, dy)
                if d > 1:
                    continue
                if d > 0.84 or (dy < -0.55 and d > 0.76):
                    c = OUTLINE
                elif dy > -0.05 and d > 0.48:
                    t = min(1.0, (dy + 0.05) / 0.9)
                    c = (int(40 + 50 * t), int(100 + 110 * t), int(200 + 20 * t))
                else:
                    c = (18, 18, 34)
                a[y, x, :3] = c
                a[y, x, 3] = 255
        disc(a, cx + 0.22 * r, cy - 0.32 * r, max(0.7, 0.3 * r), (255, 255, 255))
        if r > 3:
            disc(a, cx - 0.32 * r, cy + 0.28 * r, 0.13 * r, (226, 232, 240))
        if blush:
            disc(a, cx + sx * 0.3 * r, cy + 1.45 * r, 0.62 * r, (255, 140, 180), 0.6, ry=0.32 * r)


def snoet_mouth(a, W, H, y, fur, nose=(214, 96, 150), width=0.07):
    """A tiny pink guh snoet with the soft mouth line under it."""
    nw = max(1.5, W * width)
    for yy in range(int(y), int(y + nw * 0.8) + 1):
        half = nw * (1 - (yy - y) / (nw * 1.4))
        for x in range(int(W / 2 - half), int(W / 2 + half) + 1):
            if 0 <= yy < H and 0 <= x < W:
                a[yy, x, :3] = nose
    my = int(y + nw * 0.8) + 2
    for x in range(int(W / 2 - nw * 1.6), int(W / 2 + nw * 1.6) + 1):
        dy = int(round(abs(x - W / 2) / (nw * 1.6) * 1.5))
        if 0 <= my - dy < H and 0 <= x < W:
            a[my - dy, x, :3] = np.array(fur[:3]) * 0.66


def guh_face(fur, eye_y=0.42, eye_dx=0.26, eye_r=0.17, snoet=True, blush=True, extra=None):
    def paint(W, H, rng):
        a = plain(fur, 7, extra)(W, H, rng).astype(np.float32)
        guh_eyes(a, W, H, eye_y, eye_dx, eye_r, blush)
        if snoet:
            snoet_mouth(a, W, H, H * eye_y + 1.3 * eye_r * W, fur)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def round_ear(fur, inner):
    def paint(W, H, rng):
        a = plain(fur, 7)(W, H, rng).astype(np.float32)
        out = np.zeros_like(a)
        disc(out, W / 2, H / 2, W / 2, fur, 1.0, ry=H / 2)
        a[out[..., 3] == 0, 3] = 0
        inside = np.zeros_like(a)
        disc(inside, W / 2, H * 0.56, W * 0.28, inner, 1.0, ry=H * 0.28)
        m = inside[..., 3] > 0
        a[m, :3] = inside[m, :3]
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def belly(colour, cy=0.55, rx=0.36, ry=0.36):
    def f(W, H, rng, a):
        disc(a, W / 2, H * cy, W * rx, colour, 1.0, ry=H * ry)
    return f


def fluffy(base, tip):
    """A fluffy gill plume: soft strands from the base colour to a lighter tip, with frilly see-through edges."""
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        for x in range(W):
            t = x / max(1, W - 1)
            c = np.array(base, np.float32) * (1 - t) + np.array(tip, np.float32) * t
            for y in range(H):
                a[y, x, :3] = c + rng.normal(0, 6)
                a[y, x, 3] = 255
        # frills: every other texel on the top and bottom row sticks out a little (see-through gaps)
        for x in range(W):
            if (x // max(1, W // 8)) % 2:
                a[0, x, 3] = 0
                a[H - 1, x, 3] = 0
        # strands: slightly lighter lines along the plume
        for y in range(1, H - 1, 2):
            a[y, :, :3] = np.minimum(255, a[y, :, :3] + 16)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def gold_sparkles(W, H, rng, a):
    """Little sparkly spots of the golden guhxolotl."""
    for _ in range(max(1, W * H // 90)):
        x, y = rng.uniform(0, W), rng.uniform(0, H)
        disc(a, x, y, rng.uniform(0.8, 1.6), (255, 250, 214), 0.9)


def sparkle_glow(W, H, rng):
    """The gold one's glowmask: the same sparkles, faint (drawn at full bright by the glow layer)."""
    a = np.zeros((H, W, 4), np.float32)
    r2 = np.random.default_rng(int(rng.integers(0, 1 << 30)))
    for _ in range(max(1, W * H // 120)):
        disc(a, r2.uniform(0, W), r2.uniform(0, H), r2.uniform(0.6, 1.2), (255, 244, 190), 1.0)
    return np.clip(a, 0, 255).astype(np.uint8)


def tail_fin(colour, edge):
    """The tail fin: a see-through rounded fin (thin plane) with soft rays and a darker rim."""
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        for y in range(H):
            for x in range(W):
                t = x / max(1, W - 1)                          # (along the tail: the fin tapers towards the tip)
                half = H * (0.5 - 0.18 * t) * (1 - (t ** 3) * 0.6)
                d = abs(y + 0.5 - H / 2)
                if d <= half:
                    rim = d > half - 1.2
                    ray = (x % 4 == 0)
                    c = np.array(edge if rim else colour, np.float32)
                    if ray and not rim:
                        c = c * 0.92
                    a[y, x, :3] = c
                    a[y, x, 3] = 235
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def wing_vlinder(wing, edge, dots, lower=False):
    """A butterfly wing (flat plane): a rounded lobe with a darker rim, knabbel dots and a guh-ear spot."""
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.float32)
        cx, cy = W * 0.45, H * (0.55 if not lower else 0.45)
        rx, ry = W * 0.5, H * 0.5
        for y in range(H):
            for x in range(W):
                d = ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2
                if d <= 1 and not (x < W * 0.1 and abs(y - H / 2) > H * 0.25):
                    c = np.array(edge if d > 0.72 else wing, np.float32)
                    c += rng.normal(0, 5)
                    a[y, x, :3] = c
                    a[y, x, 3] = 255
        # knabbel dots on the rim and one big guh-head spot (a dot with two round ears) in the middle
        for k in range(5):
            ang = -1.2 + k * 0.6
            disc(a, cx + math.cos(ang) * rx * 0.82, cy + math.sin(ang) * ry * 0.82, max(0.8, W * 0.06), dots)
        gx, gy, gr = cx + W * 0.05, cy, W * 0.14
        disc(a, gx, gy, gr, edge)
        disc(a, gx - gr * 0.8, gy - gr * 0.9, gr * 0.5, edge)
        disc(a, gx + gr * 0.8, gy - gr * 0.9, gr * 0.5, edge)
        disc(a, gx, gy + gr * 0.1, gr * 0.45, dots)
        a[a[..., 3] == 0] = 0
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def wing_clear(W, H, rng):
    """A see-through firefly/ladybird hind wing: pale, with veins."""
    a = np.zeros((H, W, 4), np.float32)
    for y in range(H):
        for x in range(W):
            d = ((x + 0.5 - W * 0.45) / (W * 0.5)) ** 2 + ((y + 0.5 - H / 2) / (H * 0.5)) ** 2
            if d <= 1:
                vein = (x + y) % 5 == 0 or d > 0.8
                a[y, x] = (200, 214, 240, 190) if vein else (236, 244, 255, 120)
    return np.clip(a, 0, 255).astype(np.uint8)


def lantern(W, H, rng):
    """The glimguhtje's belly lantern: warm yellow-green, brightest in the middle."""
    a = np.zeros((H, W, 4), np.float32)
    for y in range(H):
        for x in range(W):
            d = math.hypot((x + 0.5 - W / 2) / (W / 2), (y + 0.5 - H / 2) / (H / 2))
            t = max(0.0, 1 - d)
            a[y, x, :3] = (200 + 55 * t, 236 + 19 * t, 96 + 110 * t)
            a[y, x, 3] = 255
    return np.clip(a, 0, 255).astype(np.uint8)


def lantern_glow(W, H, rng):
    return lantern(W, H, rng)


def shell(red=(222, 40, 52), spot=(26, 20, 28)):
    """The ladybird's shell half: shiny red with guh-head spots (a dot with two round ears) and a light glint."""
    def f(W, H, rng, a):
        for (px, py, pr) in ((0.3, 0.3, 0.13), (0.7, 0.55, 0.12), (0.35, 0.78, 0.11)):
            cx, cy, r = W * px, H * py, W * pr
            disc(a, cx, cy, r, spot)
            disc(a, cx - r * 0.85, cy - r * 0.95, r * 0.5, spot)
            disc(a, cx + r * 0.85, cy - r * 0.95, r * 0.5, spot)
        disc(a, W * 0.72, H * 0.18, W * 0.08, (255, 190, 190), 0.8)
    return plain(red, 6, f)


# =====================================================================================================================
# model building
# =====================================================================================================================
FACES = ("north", "south", "east", "west", "up", "down")


def face_size(face, size):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}[face]


def cube(atlas, origin, size, paint, overrides=None, glow=None, inflate=None, only=None):
    """A cube whose faces each get their own patch (paint, or overrides[face]); glow[face] paints the glowmask; only =
    the faces to keep (a thin plane needs just north/south or up/down)."""
    uv = {}
    for f in FACES:
        if only and f not in only:
            continue
        w, h = face_size(f, size)
        if w <= 0 or h <= 0:
            continue
        p = (overrides or {}).get(f, paint)
        uv[f] = atlas.patch(max(w, 0.5), max(h, 0.5), p, (glow or {}).get(f))
    c = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": uv}
    if inflate:
        c["inflate"] = inflate
    return c


def bone(name, parent, pivot, cubes=(), rotation=None):
    b = {"name": name, "pivot": [round(v, 3) for v in pivot], "cubes": list(cubes)}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = rotation
    return b


def geo(identifier, atlas, bones, width, height):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{identifier}", "texture_width": atlas.units, "texture_height": atlas.units,
                        "visible_bounds_width": width, "visible_bounds_height": height, "visible_bounds_offset": [0, height / 2, 0]},
        "bones": bones}]}


def ears(atlas, fur, inner, x, y, z, w, h, parent="head"):
    ear = round_ear(fur, inner)
    edge = plain(fur, 6)
    out = []
    for side, sx in (("left", 1), ("right", -1)):
        ox = x if sx > 0 else -x - w
        out.append(bone(f"ear_{side}", parent, [sx * (x + w / 2), y, z + 0.4],
                        [cube(atlas, [ox, y, z], [w, h, 0.8], edge, overrides={"north": ear, "south": ear})]))
    return out


# --- the guhxolotl ----------------------------------------------------------------------------------------------------------
def guhxolotl(atlas, kleur):
    fur, buik, kieuw, tip, oor = XOLOTL[kleur]
    goud = kleur == "goud"
    huid = plain(fur, 7, gold_sparkles if goud else None)
    glow = sparkle_glow if goud else None
    g6 = {f: glow for f in FACES} if glow else None
    buikje = plain(buik, 5)
    poot = plain(tuple(int(c * 0.94) for c in fur), 6)
    head_face = guh_face(fur, eye_y=0.38, eye_dx=0.25, eye_r=0.16, snoet=False)
    bones = [bone("root", None, [0, 0, 0]),
             bone("body", "root", [0, 2.5, 0], [cube(atlas, [-3, 0.5, -4], [6, 4, 10], huid, overrides={"down": buikje}, glow=g6)])]
    head = bone("head", "body", [0, 3, -4], [
        cube(atlas, [-4, 0.2, -10], [8, 6.2, 6.2], huid, overrides={"north": head_face, "down": buikje}, glow=g6),
        cube(atlas, [-0.8, 1.9, -10.45], [1.6, 0.8, 0.45], plain((236, 128, 166), 4)),        # the snoet
    ])
    bones.append(head)
    bones += ears(atlas, fur, oor, 1.4, 6.0, -8.2, 2.4, 2.4)
    # three fluffy gill plumes per side (each its own bone: up, level, down), sticking out backwards-sideways
    pluim = fluffy(kieuw, tip)
    for side, sx in (("left", 1), ("right", -1)):
        for i, (dy, rz, lengte) in enumerate(((5.2, 38, 4.4), (3.5, 8, 4.0), (1.8, -24, 3.6))):
            x0 = 3.8 if sx > 0 else -3.8 - lengte
            bones.append(bone(f"gill_{side}_{i}", "head", [sx * 3.8, dy + 0.75, -7.2], [
                cube(atlas, [x0, dy, -7.7], [lengte, 1.5, 1.0], pluim)],
                rotation=[0, sx * 18, sx * rz]))
    for name, x, z in (("leg_front_left", 3, -3.5), ("leg_front_right", -6, -3.5), ("leg_back_left", 3, 3.0), ("leg_back_right", -6, 3.0)):
        piv = [x + (0 if x > 0 else 3), 1.2, z + 1]
        bones.append(bone(name, "body", piv, [cube(atlas, [x, 0.3, z], [3, 1, 2], poot)]))
    bones.append(bone("tail", "body", [0, 2.6, 6], [
        cube(atlas, [-1.5, 1.2, 6], [3, 2.8, 5.5], huid, overrides={"down": buikje}, glow=g6),
        cube(atlas, [0, 0.2, 6.5], [0, 5, 7.5], tail_fin(tuple(min(255, int(c * 0.9 + 25)) for c in fur), kieuw), only=("east", "west")),
    ]))
    return geo("guhxolotl", atlas, bones, 1.2, 0.8)


def guhxolotl_anims():
    gills = {}
    for side, sx in (("left", 1), ("right", -1)):
        for i in range(3):
            ph = i * 0.25
            gills[f"gill_{side}_{i}"] = {"rotation": {"0.0": [0, 0, 0], str(round(0.6 + ph, 2)): [0, sx * 8, sx * 10],
                                                      str(round(1.2 + ph, 2)): [0, 0, 0], "2.0": [0, 0, 0]}}
    idle = {"body": {"position": {"0.0": [0, 0, 0], "1.0": [0, 0.35, 0], "2.0": [0, 0, 0]}},
            "tail": {"rotation": {"0.0": [0, 6, 0], "1.0": [0, -6, 0], "2.0": [0, 6, 0]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "1.6": [0, 0, 0], "1.7": [0, 0, -16], "1.8": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "0.8": [0, 0, 0], "0.9": [0, 0, 16], "1.0": [0, 0, 0]}}}
    idle.update(gills)
    swim = {"body": {"rotation": {"0.0": [0, 8, 0], "0.3": [0, -8, 0], "0.6": [0, 8, 0]}},
            "head": {"rotation": {"0.0": [0, -6, 0], "0.3": [0, 6, 0], "0.6": [0, -6, 0]}},
            "tail": {"rotation": {"0.0": [0, -24, 0], "0.3": [0, 24, 0], "0.6": [0, -24, 0]}},
            "leg_front_left": {"rotation": {"0.0": [0, -60, 0]}}, "leg_front_right": {"rotation": {"0.0": [0, 60, 0]}},
            "leg_back_left": {"rotation": {"0.0": [0, -70, 0]}}, "leg_back_right": {"rotation": {"0.0": [0, 70, 0]}}}
    for side, sx in (("left", 1), ("right", -1)):
        for i in range(3):
            swim[f"gill_{side}_{i}"] = {"rotation": {"0.0": [0, sx * -14, 0], "0.3": [0, sx * -6, 0], "0.6": [0, sx * -14, 0]}}
    walk = {"body": {"rotation": {"0.0": [0, 10, 0], "0.4": [0, -10, 0], "0.8": [0, 10, 0]}},
            "tail": {"rotation": {"0.0": [0, -18, 0], "0.4": [0, 18, 0], "0.8": [0, -18, 0]}},
            "leg_front_left": {"rotation": {"0.0": [0, 30, 0], "0.4": [0, -30, 0], "0.8": [0, 30, 0]}},
            "leg_back_right": {"rotation": {"0.0": [0, 30, 0], "0.4": [0, -30, 0], "0.8": [0, 30, 0]}},
            "leg_front_right": {"rotation": {"0.0": [0, 30, 0], "0.4": [0, -30, 0], "0.8": [0, 30, 0]}},
            "leg_back_left": {"rotation": {"0.0": [0, 30, 0], "0.4": [0, -30, 0], "0.8": [0, 30, 0]}}}
    walk.update(gills)
    # "plat als een pannenkoekje": it plays dead on its back (paws up), the gills flat, and peeks now and then
    plat = {"root": {"rotation": {"0.0": [0, 0, 180]}, "position": {"0.0": [0, 5.2, 0]}},
            "leg_front_left": {"rotation": {"0.0": [0, 0, 40]}}, "leg_front_right": {"rotation": {"0.0": [0, 0, -40]}},
            "leg_back_left": {"rotation": {"0.0": [0, 0, 40]}}, "leg_back_right": {"rotation": {"0.0": [0, 0, -40]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "1.6": [0, 0, 0], "1.8": [0, 0, 20], "2.2": [0, 0, 20], "2.4": [0, 0, 0], "3.0": [0, 0, 0]}}}
    for side, sx in (("left", 1), ("right", -1)):
        for i in range(3):
            plat[f"gill_{side}_{i}"] = {"rotation": {"0.0": [0, sx * -20, sx * -10]}}
    blij = {"body": {"position": {"0.0": [0, 0, 0], "0.15": [0, 2, 0], "0.3": [0, 0, 0], "0.45": [0, 1.5, 0], "0.6": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "0.2": [-12, 0, 10], "0.4": [-12, 0, -10], "0.6": [0, 0, 0]}},
            "tail": {"rotation": {"0.0": [0, 0, 0], "0.1": [0, 30, 0], "0.2": [0, -30, 0], "0.3": [0, 30, 0], "0.4": [0, -30, 0], "0.6": [0, 0, 0]}}}
    for side, sx in (("left", 1), ("right", -1)):
        for i in range(3):
            blij[f"gill_{side}_{i}"] = {"rotation": {"0.0": [0, 0, 0], "0.15": [0, sx * 20, sx * 18], "0.3": [0, 0, 0],
                                                     "0.45": [0, sx * 20, sx * 18], "0.6": [0, 0, 0]}}
    eet = {"head": {"rotation": {"0.0": [0, 0, 0], "0.15": [18, 0, 0], "0.3": [0, 0, 0], "0.45": [18, 0, 0], "0.6": [0, 0, 0]},
                    "scale": {"0.0": [1, 1, 1], "0.15": [1.06, 0.96, 1.06], "0.3": [1, 1, 1], "0.45": [1.06, 0.96, 1.06], "0.6": [1, 1, 1]}}}
    droog = {"head": {"rotation": {"0.0": [14, 0, 0], "1.5": [18, 0, 4], "3.0": [14, 0, 0]}},
             "tail": {"rotation": {"0.0": [0, 0, 0]}}}
    for side, sx in (("left", 1), ("right", -1)):
        for i in range(3):
            droog[f"gill_{side}_{i}"] = {"rotation": {"0.0": [0, sx * -30, sx * -30]}}
    blub = {"head": {"rotation": {"0.0": [0, 0, 0], "0.2": [-20, 0, 0], "0.6": [-20, 0, 0], "0.8": [0, 0, 0]}},
            "body": {"scale": {"0.0": [1, 1, 1], "0.2": [1.05, 1.1, 1.05], "0.8": [1, 1, 1]}}}
    return {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 2.0, "bones": idle},
        "swim": {"loop": True, "animation_length": 0.6, "bones": swim},
        "walk": {"loop": True, "animation_length": 0.8, "bones": walk},
        "plat": {"loop": True, "animation_length": 3.0, "bones": plat},
        "droog": {"loop": True, "animation_length": 3.0, "bones": droog},
        "blij": {"loop": False, "animation_length": 0.6, "bones": blij},
        "eet": {"loop": False, "animation_length": 0.6, "bones": eet},
        "blub": {"loop": False, "animation_length": 0.8, "bones": blub}}}


# --- the guh-eendje ---------------------------------------------------------------------------------------------------------
def eendje(atlas, soort):
    lijf, vleugel, buik, snavel, poot, oor = EENDJE[soort]
    donsje = plain(lijf, 8)
    bones = [bone("root", None, [0, 0, 0]),
             bone("body", "root", [0, 3, 0], [
                 cube(atlas, [-3.5, 1.5, -4], [7, 5, 9], donsje, overrides={"down": plain(buik, 5)}),
                 cube(atlas, [-2.5, 4.5, 4.4], [5, 2.5, 2], donsje)]),              # the little tail feathers up at the back
             bone("head", "body", [0, 6.5, -3], [
                 cube(atlas, [-3, 6, -6.2], [6, 5.6, 5.2], donsje, overrides={
                     "north": guh_face(lijf, eye_y=0.36, eye_dx=0.25, eye_r=0.18, snoet=False)}),
                 cube(atlas, [-1.9, 6.9, -8.3], [3.8, 1.2, 2.2], plain(snavel, 5)),        # the beak
                 cube(atlas, [-1.5, 6.1, -7.9], [3, 0.8, 1.7], plain(tuple(int(c * 0.85) for c in snavel), 5))])]
    bones += ears(atlas, lijf, oor, 1.3, 11.0, -4.6, 2.3, 2.3)
    for side, sx in (("left", 1), ("right", -1)):
        bones.append(bone(f"wing_{side}", "body", [sx * 3.5, 5.8, -2], [
            cube(atlas, [3.5 if sx > 0 else -4.5, 2.5, -2.5], [1, 3.5, 6], plain(vleugel, 7))]))
        x = 1 if sx > 0 else -2
        bones.append(bone(f"leg_{side}", "body", [x + 0.5, 1.8, 0.5], [
            cube(atlas, [x, 0, 0], [1, 2, 1], plain(poot, 5)),
            cube(atlas, [x - 0.6, 0, -1.4], [2.2, 0.4, 2.2], plain(poot, 5))]))           # webbed feet
    return geo("guh_eendje", atlas, bones, 0.9, 0.9)


def eendje_anims():
    waggel = {"body": {"rotation": {"0.0": [0, 0, 7], "0.3": [0, 0, -7], "0.6": [0, 0, 7]}},
              "leg_left": {"rotation": {"0.0": [30, 0, 0], "0.3": [-30, 0, 0], "0.6": [30, 0, 0]}},
              "leg_right": {"rotation": {"0.0": [-30, 0, 0], "0.3": [30, 0, 0], "0.6": [-30, 0, 0]}},
              "head": {"rotation": {"0.0": [0, 0, -4], "0.3": [0, 0, 4], "0.6": [0, 0, -4]}}}
    idle = {"body": {"scale": {"0.0": [1, 1, 1], "1.5": [1.02, 1.03, 1.02], "3.0": [1, 1, 1]}},
            "tail_dummy": None,
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "2.4": [0, 0, 0], "2.55": [0, 0, -18], "2.7": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "1.2": [0, 0, 0], "1.35": [0, 0, 18], "1.5": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 12, 0], "2.0": [0, -8, 0], "3.0": [0, 0, 0]}}}
    idle.pop("tail_dummy")
    zwem = {"body": {"position": {"0.0": [0, 0, 0], "0.5": [0, 0.35, 0], "1.0": [0, 0, 0]},
                     "rotation": {"0.0": [0, 0, 2], "0.5": [0, 0, -2], "1.0": [0, 0, 2]}},
            "leg_left": {"rotation": {"0.0": [50, 0, 0], "0.5": [-10, 0, 0], "1.0": [50, 0, 0]}},
            "leg_right": {"rotation": {"0.0": [-10, 0, 0], "0.5": [50, 0, 0], "1.0": [-10, 0, 0]}}}
    kwak = {"head": {"rotation": {"0.0": [0, 0, 0], "0.12": [-22, 0, 0], "0.3": [-6, 0, 0], "0.42": [-22, 0, 0], "0.6": [0, 0, 0]}},
            "body": {"scale": {"0.0": [1, 1, 1], "0.12": [1.03, 1.05, 1.03], "0.6": [1, 1, 1]}}}
    flap = {"wing_left": {"rotation": {"0.0": [0, 0, 0], "0.1": [0, 0, -70], "0.2": [0, 0, 0], "0.3": [0, 0, -70], "0.4": [0, 0, 0],
                                       "0.5": [0, 0, -50], "0.6": [0, 0, 0]}},
            "wing_right": {"rotation": {"0.0": [0, 0, 0], "0.1": [0, 0, 70], "0.2": [0, 0, 0], "0.3": [0, 0, 70], "0.4": [0, 0, 0],
                                        "0.5": [0, 0, 50], "0.6": [0, 0, 0]}},
            "body": {"position": {"0.0": [0, 0, 0], "0.15": [0, 1.5, 0], "0.3": [0, 0, 0], "0.45": [0, 1, 0], "0.6": [0, 0, 0]}}}
    eet = {"head": {"rotation": {"0.0": [0, 0, 0], "0.15": [45, 0, 0], "0.3": [10, 0, 0], "0.45": [45, 0, 0], "0.6": [10, 0, 0],
                                 "0.75": [45, 0, 0], "1.0": [0, 0, 0]}}}
    return {"format_version": "1.8.0", "animations": {
        "idle": {"loop": True, "animation_length": 3.0, "bones": idle},
        "walk": {"loop": True, "animation_length": 0.6, "bones": waggel},
        "zwem": {"loop": True, "animation_length": 1.0, "bones": zwem},
        "kwak": {"loop": False, "animation_length": 0.6, "bones": kwak},
        "blij": {"loop": False, "animation_length": 0.6, "bones": flap},
        "eet": {"loop": False, "animation_length": 1.0, "bones": eet}}}


# --- the knabbelvlindertje ----------------------------------------------------------------------------------------------------
def vlindertje(atlas, kleur):
    wing, edge, dots, lijf = VLINDER[kleur]
    body = plain(lijf, 6)
    kop = guh_face(lijf, eye_y=0.44, eye_dx=0.25, eye_r=0.2, snoet=False, blush=True)
    bones = [bone("root", None, [0, 0, 0]),
             bone("body", "root", [0, 1.5, 0], [cube(atlas, [-0.5, 1, -1.2], [1, 1, 3.6], body)]),
             bone("head", "body", [0, 1.6, -1.2], [
                 cube(atlas, [-0.9, 0.8, -2.8], [1.8, 1.7, 1.6], body, overrides={"north": kop}),
                 cube(atlas, [-0.8, 2.4, -2.4], [0.25, 1.6, 0.25], plain(lijf, 4)),
                 cube(atlas, [0.55, 2.4, -2.4], [0.25, 1.6, 0.25], plain(lijf, 4)),
                 cube(atlas, [-1.0, 3.9, -2.55], [0.55, 0.55, 0.55], plain(dots, 4)),
                 cube(atlas, [0.45, 3.9, -2.55], [0.55, 0.55, 0.55], plain(dots, 4))])]
    for side, sx in (("left", 1), ("right", -1)):
        x0 = 0.5 if sx > 0 else -5.5
        bones.append(bone(f"wing_{side}", "body", [sx * 0.5, 2, 0], [
            cube(atlas, [x0, 2, -3.2], [5, 0, 4.2], wing_vlinder(wing, edge, dots), only=("up", "down")),
            cube(atlas, [x0 + (0 if sx > 0 else 1), 1.95, 0.8], [4, 0, 3.4], wing_vlinder(wing, edge, dots, lower=True), only=("up", "down"))]))
    return geo("knabbelvlindertje", atlas, bones, 0.8, 0.6)


def vlindertje_anims():
    fly = {"wing_left": {"rotation": {"0.0": [0, 0, 30], "0.1": [0, 0, -70], "0.2": [0, 0, 30]}},
           "wing_right": {"rotation": {"0.0": [0, 0, -30], "0.1": [0, 0, 70], "0.2": [0, 0, -30]}},
           "body": {"position": {"0.0": [0, 0, 0], "0.1": [0, 0.5, 0], "0.2": [0, 0, 0]}}}
    zit = {"wing_left": {"rotation": {"0.0": [0, 0, -80], "1.2": [0, 0, -80], "1.5": [0, 0, -20], "1.8": [0, 0, -80], "3.0": [0, 0, -80]}},
           "wing_right": {"rotation": {"0.0": [0, 0, 80], "1.2": [0, 0, 80], "1.5": [0, 0, 20], "1.8": [0, 0, 80], "3.0": [0, 0, 80]}},
           "head": {"rotation": {"0.0": [0, 0, 0], "2.2": [0, 0, 0], "2.4": [0, 0, 12], "2.6": [0, 0, 0]}}}
    return {"format_version": "1.8.0", "animations": {
        "fly": {"loop": True, "animation_length": 0.2, "bones": fly},
        "zit": {"loop": True, "animation_length": 3.0, "bones": zit}}}


# --- the glimguhtje ------------------------------------------------------------------------------------------------------------
GLIM = (250, 226, 236)
GLIM_OOR = (236, 140, 176)


def glimguhtje(atlas):
    fur = plain(GLIM, 6)
    face = guh_face(GLIM, eye_y=0.42, eye_dx=0.25, eye_r=0.19, snoet=True)
    bones = [bone("root", None, [0, 0, 0]),
             bone("body", "root", [0, 2, 0], [cube(atlas, [-1.6, 0.8, -1.6], [3.2, 3, 3.2], fur, overrides={"north": face})]),
             bone("lantern", "body", [0, 1.6, 1.6], [
                 cube(atlas, [-1.3, 0.5, 1.5], [2.6, 2.2, 1.8], lantern, glow={f: lantern_glow for f in FACES})])]
    bones += ears(atlas, GLIM, GLIM_OOR, 0.5, 3.6, -0.6, 1.2, 1.2, parent="body")
    for side, sx in (("left", 1), ("right", -1)):
        x0 = 0.2 if sx > 0 else -3.4
        bones.append(bone(f"wing_{side}", "body", [sx * 0.4, 3.8, 0.4], [
            cube(atlas, [x0, 3.8, -0.8], [3.2, 0, 2.6], wing_clear, only=("up", "down"))]))
    return geo("glimguhtje", atlas, bones, 0.5, 0.5)


def glimguhtje_anims():
    fly = {"wing_left": {"rotation": {"0.0": [0, 0, 20], "0.06": [0, 0, -40], "0.12": [0, 0, 20]}},
           "wing_right": {"rotation": {"0.0": [0, 0, -20], "0.06": [0, 0, 40], "0.12": [0, 0, -20]}}}
    glim = {"lantern": {"scale": {"0.0": [1, 1, 1], "1.0": [1.12, 1.12, 1.12], "2.0": [1, 1, 1]}},
            "body": {"position": {"0.0": [0, 0, 0], "1.0": [0, 0.6, 0], "2.0": [0, 0, 0]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "1.4": [0, 0, 0], "1.5": [0, 0, -20], "1.6": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "0.4": [0, 0, 0], "0.5": [0, 0, 20], "0.6": [0, 0, 0]}}}
    return {"format_version": "1.8.0", "animations": {
        "fly": {"loop": True, "animation_length": 0.12, "bones": fly},
        "glim": {"loop": True, "animation_length": 2.0, "bones": glim}}}


# --- the lieveheersbeestje -----------------------------------------------------------------------------------------------------
ZWART = (30, 24, 34)


def lieveheersbeestje(atlas):
    kop = guh_face(ZWART, eye_y=0.45, eye_dx=0.24, eye_r=0.22, snoet=False, blush=True)
    # (on a black head the guh eyes get a white rim so you can see them)

    def kop_wit(W, H, rng):
        a = kop(W, H, rng).astype(np.float32)
        r = 0.22 * W
        for sx in (-1, 1):
            cx, cy = W / 2 + sx * 0.24 * W, H * 0.45
            for y in range(H):
                for x in range(W):
                    d = math.hypot((x + 0.5 - cx) / r, (y + 0.5 - cy) / (r * 1.08))
                    if 1.0 < d <= 1.28:
                        a[y, x, :3] = (250, 250, 255)
        return np.clip(a, 0, 255).astype(np.uint8)
    zwart = plain(ZWART, 5)
    bones = [bone("root", None, [0, 0, 0]),
             bone("body", "root", [0, 1, 0], [cube(atlas, [-1.4, 0.3, -1.4], [2.8, 1.1, 3.2], zwart)]),
             bone("head", "body", [0, 1, -1.4], [cube(atlas, [-1, 0.35, -2.5], [2, 1.3, 1.1], zwart, overrides={"north": kop_wit})])]
    for side, sx in (("left", 1), ("right", -1)):
        x0 = 0 if sx > 0 else -1.6
        bones.append(bone(f"schild_{side}", "body", [sx * 0.1, 1.4, -1.5], [
            cube(atlas, [x0, 1.0, -1.6], [1.6, 1.0, 3.5], shell(), overrides={"down": zwart}, only=("up", "north", "south", "east", "west"))]))
        bones.append(bone(f"wing_{side}", "body", [sx * 0.2, 1.6, -0.5], [
            cube(atlas, [0.2 if sx > 0 else -3.0, 1.55, -0.8], [2.8, 0, 3.2], wing_clear, only=("up", "down"))]))
        for i, z in enumerate((-1.0, 0.0, 1.0)):
            bones.append(bone(f"leg_{side}_{i}", "body", [sx * 1.2, 0.5, z], [
                cube(atlas, [1.2 if sx > 0 else -1.9, 0, z - 0.15], [0.7, 0.3, 0.3], zwart)]))
    return geo("lieveheersbeestje", atlas, bones, 0.4, 0.3)


def lieveheersbeestje_anims():
    walk = {}
    for side in ("left", "right"):
        for i in range(3):
            s = 25 if (i % 2 == 0) == (side == "left") else -25
            walk[f"leg_{side}_{i}"] = {"rotation": {"0.0": [0, s, 0], "0.15": [0, -s, 0], "0.3": [0, s, 0]}}
    walk["head"] = {"rotation": {"0.0": [0, 0, -4], "0.15": [0, 0, 4], "0.3": [0, 0, -4]}}
    fly = {"schild_left": {"rotation": {"0.0": [0, -20, -50]}}, "schild_right": {"rotation": {"0.0": [0, 20, 50]}},
           "wing_left": {"rotation": {"0.0": [0, 0, 20], "0.05": [0, 0, -40], "0.1": [0, 0, 20]}},
           "wing_right": {"rotation": {"0.0": [0, 0, -20], "0.05": [0, 0, 40], "0.1": [0, 0, -20]}}}
    zit = {"head": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 14, 0], "2.0": [0, -14, 0], "3.0": [0, 0, 0]}},
           "leg_left_0": {"rotation": {"0.0": [0, 0, 0], "2.4": [0, 0, 0], "2.55": [0, -30, 0], "2.7": [0, 0, 0]}}}
    helpen = {"body": {"position": {"0.0": [0, 0, 0], "0.2": [0, 0.8, 0], "0.4": [0, 0, 0], "0.6": [0, 0.8, 0], "0.8": [0, 0, 0]}},
              "schild_left": {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, -25], "0.4": [0, 0, 0], "0.6": [0, 0, -25], "0.8": [0, 0, 0]}},
              "schild_right": {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, 25], "0.4": [0, 0, 0], "0.6": [0, 0, 25], "0.8": [0, 0, 0]}}}
    return {"format_version": "1.8.0", "animations": {
        "walk": {"loop": True, "animation_length": 0.3, "bones": walk},
        "fly": {"loop": True, "animation_length": 0.1, "bones": fly},
        "zit": {"loop": True, "animation_length": 3.0, "bones": zit},
        "helpen": {"loop": False, "animation_length": 0.8, "bones": helpen}}}


# =====================================================================================================================
# writing
# =====================================================================================================================
MODELS = ["guhxolotl", "guh_eendje", "knabbelvlindertje", "glimguhtje", "lieveheersbeestje"]
TEXTURES = ([f"guhxolotl_{k}" for k in XOLOTL] + ["guhxolotl_goud_glowmask", "guh_eendje", "guh_eendje_kuiken"]
            + [f"knabbelvlindertje_{k}" for k in VLINDER] + ["glimguhtje", "glimguhtje_glowmask", "lieveheersbeestje"])
ANIMS = {"guhxolotl": ["idle", "swim", "walk", "plat", "droog", "blij", "eet", "blub"],
         "guh_eendje": ["idle", "walk", "zwem", "kwak", "blij", "eet"],
         "knabbelvlindertje": ["fly", "zit"], "glimguhtje": ["fly", "glim"], "lieveheersbeestje": ["walk", "fly", "zit", "helpen"]}


def _write(A, name, obj):
    path = os.path.join(A, *name.split("/"))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


def build(h):
    A = h.A
    g = None
    for kleur in XOLOTL:
        atlas = Atlas(64, 8, 30140101)
        g = guhxolotl(atlas, kleur)
        h.save(atlas.image(), "entity", f"guhxolotl_{kleur}.png")
        if kleur == "goud":
            h.save(atlas.glowmask(), "entity", "guhxolotl_goud_glowmask.png")
    _write(A, "geckolib/models/entity/guhxolotl.geo.json", g)
    _write(A, "geckolib/animations/entity/guhxolotl.animation.json", guhxolotl_anims())
    for soort, tex in (("mama", "guh_eendje"), ("kuiken", "guh_eendje_kuiken")):
        atlas = Atlas(64, 8, 30140102)
        g = eendje(atlas, soort)
        h.save(atlas.image(), "entity", f"{tex}.png")
    _write(A, "geckolib/models/entity/guh_eendje.geo.json", g)
    _write(A, "geckolib/animations/entity/guh_eendje.animation.json", eendje_anims())
    for kleur in VLINDER:
        atlas = Atlas(32, 16, 30140103)
        g = vlindertje(atlas, kleur)
        h.save(atlas.image(), "entity", f"knabbelvlindertje_{kleur}.png")
    _write(A, "geckolib/models/entity/knabbelvlindertje.geo.json", g)
    _write(A, "geckolib/animations/entity/knabbelvlindertje.animation.json", vlindertje_anims())
    atlas = Atlas(32, 16, 30140104)
    _write(A, "geckolib/models/entity/glimguhtje.geo.json", glimguhtje(atlas))
    h.save(atlas.image(), "entity", "glimguhtje.png")
    h.save(atlas.glowmask(), "entity", "glimguhtje_glowmask.png")
    _write(A, "geckolib/animations/entity/glimguhtje.animation.json", glimguhtje_anims())
    atlas = Atlas(32, 16, 30140105)
    _write(A, "geckolib/models/entity/lieveheersbeestje.geo.json", lieveheersbeestje(atlas))
    h.save(atlas.image(), "entity", "lieveheersbeestje.png")
    _write(A, "geckolib/animations/entity/lieveheersbeestje.animation.json", lieveheersbeestje_anims())


def check(h):
    A = h.A
    problems = []
    for name in MODELS:
        g = json.load(open(os.path.join(A, "geckolib", "models", "entity", f"{name}.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]
        names = [b["name"] for b in g["bones"]]
        if len(set(names)) != len(names):
            problems.append(f"{name}: duplicate bone names")
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: {b['name']} hangs on a missing {b['parent']}")
            for c in b.get("cubes", []):
                for f, uv in c["uv"].items():
                    (u, v), (w, hh) = uv["uv"], uv["uv_size"]
                    if u + w > g["description"]["texture_width"] + 1e-6 or v + hh > g["description"]["texture_height"] + 1e-6:
                        problems.append(f"{name}: a uv outside the atlas ({b['name']} {f})")
        anims = json.load(open(os.path.join(A, "geckolib", "animations", "entity", f"{name}.animation.json"), encoding="utf-8"))["animations"]
        for an in ANIMS[name]:
            if an not in anims:
                problems.append(f"{name}: no animation {an}")
        for an, body in anims.items():
            for bn in body["bones"]:
                if bn not in names:
                    problems.append(f"{name}: animation {an} moves a missing bone {bn}")
        top = max(c["origin"][1] + c["size"][1] for b in g["bones"] for c in b.get("cubes", []))
        if top > 16:
            problems.append(f"{name}: {top} units tall")
    for t in TEXTURES:
        if not os.path.exists(os.path.join(h.TEX, "entity", f"{t}.png")):
            problems.append(f"missing texture entity/{t}.png")
    # the guh look: every guhxolotl colour has the glossy guh eyes (blue ring + white shine) somewhere on its sheet
    for k in XOLOTL:
        a = np.asarray(Image.open(os.path.join(h.TEX, "entity", f"guhxolotl_{k}.png")).convert("RGBA")).astype(np.int32)
        blue = int(((a[..., 2] > a[..., 0] + 60) & (a[..., 3] > 0)).sum())
        if blue < 40:
            problems.append(f"guhxolotl_{k}: no guh eyes ({blue})")
    return problems


def preview(out):
    """Renders every model/texture to out/ with wiki_renders (for looking at them, not part of the build)."""
    here = os.path.dirname(os.path.abspath(__file__))
    sys.path.insert(0, os.path.dirname(here))
    import wiki_renders as wr
    A = os.path.join("src", "main", "resources", "assets", "guhs")
    os.makedirs(out, exist_ok=True)
    shots = [("guhxolotl", f"guhxolotl_{k}") for k in XOLOTL] + [("guh_eendje", "guh_eendje"), ("guh_eendje", "guh_eendje_kuiken")] + \
            [("knabbelvlindertje", f"knabbelvlindertje_{k}") for k in VLINDER] + [("glimguhtje", "glimguhtje"), ("lieveheersbeestje", "lieveheersbeestje")]
    tiles = []
    for model, tex in shots:
        q = wr.geo_quads(os.path.join(A, "geckolib", "models", "entity", f"{model}.geo.json"), f"guhs:entity/{tex}")
        front = wr.render(q, 20, -20, 256, margin=0.06)
        side = wr.render(q, 60, -25, 256, margin=0.06)
        tile = Image.new("RGBA", (512, 256), (120, 170, 210, 255))
        tile.alpha_composite(front, (0, 0))
        tile.alpha_composite(side, (256, 0))
        tile.save(os.path.join(out, f"{tex}.png"))
        tiles.append(tile)
    sheet = Image.new("RGBA", (1024, 256 * ((len(tiles) + 1) // 2)), (120, 170, 210, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 2) * 512, (i // 2) * 256))
    sheet.save(os.path.join(out, "_alle.png"))
    print("preview:", out)


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
