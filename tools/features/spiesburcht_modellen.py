"""
The GeckoLib models (geo + animations + textures) of the Spiesburcht creatures, built from the guh and Mika models:

  - rookguh           (2.8) a real ghast, 3/4 of the size, with the guh face painted flat on its front (own texture);
                      the renderer swells its body a little and tints it rosy as it eats
  - vonk_mika         a fiery Mika head over a glowing coal, in three whirling rings of grillspiesjes (saté, worst, ui)
  - knekel_mika       a tall charred Mika skeleton with little horns, glowing red eyes and a hot grill fork
  - aangebrande_mika  a huge charred Mika body with three Mika heads on a grill bar, glowing ember cracks, a smoke skirt

The Mika / guh cubes keep their own UVs on a recoloured copy of mika.png / guh.png; every new cube samples an 8x8
"swatch" painted into a free part of that sheet. build(h) writes everything; check(h) is the self-check.
"""
import copy
import json
import math
import os

import numpy as np
from PIL import Image

UNITS = 128          # texture size in model units (the png is 4 px per unit)
PX = 4
SW = 8               # swatch size in units


# =====================================================================================================================
# geo helpers
# =====================================================================================================================
def load_geo(h, name):
    with open(os.path.join(h.A, "geo", "entity", f"{name}.geo.json"), encoding="utf-8") as f:
        return {b["name"]: b for b in json.load(f)["minecraft:geometry"][0]["bones"]}


def moved(bone, name, parent, scale=1.0, shift=(0, 0, 0), centre=(0, 0, 0)):
    """A copy of a bone, scaled around `centre` and shifted (cubes keep their UVs)."""
    b = copy.deepcopy(bone)
    b["name"] = name
    if parent:
        b["parent"] = parent
    else:
        b.pop("parent", None)

    def p(v):
        return [round((v[i] - centre[i]) * scale + centre[i] + shift[i], 3) for i in range(3)]
    b["pivot"] = p(b.get("pivot", [0, 0, 0]))
    for c in b.get("cubes", []):
        c["origin"] = p(c["origin"])
        c["size"] = [round(s * scale, 3) for s in c["size"]]
        if "pivot" in c:
            c["pivot"] = p(c["pivot"])
    return b


class Sheet:
    """The texture: a recoloured base sheet plus swatches painted in its free cells."""

    def __init__(self, base, used_bones):
        self.img = np.asarray(base.convert("RGBA")).astype(np.float32).copy()
        self.used = np.zeros((UNITS, UNITS), bool)
        for b in used_bones:
            for c in b.get("cubes", []):
                for face in c["uv"].values():
                    u, v = face["uv"]
                    w, hh = face["uv_size"]
                    u0, u1 = sorted((u, u + w))
                    v0, v1 = sorted((v, v + hh))
                    self.used[max(0, int(v0)):min(UNITS, int(math.ceil(v1)) + 1), max(0, int(u0)):min(UNITS, int(math.ceil(u1)) + 1)] = True
        self.swatches = {}
        self.glow = np.zeros_like(self.img)

    def swatch(self, name, painter, glow=False):
        """Paint a swatch (painter(rng) -> 32x32x3 or x4 array) in the first free 8x8 cell; returns its (u, v)."""
        if name in self.swatches:
            return self.swatches[name]
        for v in range(UNITS - SW, -1, -SW):
            for u in range(0, UNITS - SW + 1, SW):
                if not self.used[v:v + SW, u:u + SW].any():
                    self.used[v:v + SW, u:u + SW] = True
                    px = painter()
                    if px.shape[-1] == 3:
                        px = np.concatenate([px, np.full(px.shape[:2] + (1,), 255, np.float32)], -1)
                    self.img[v * PX:(v + SW) * PX, u * PX:(u + SW) * PX] = px
                    if glow:
                        self.glow[v * PX:(v + SW) * PX, u * PX:(u + SW) * PX] = px
                    self.swatches[name] = (u, v)
                    return (u, v)
        raise SystemExit(f"spiesburcht models: no free texture space for swatch {name}")

    def faces(self, name):
        u, v = self.swatches[name]
        return {f: {"uv": [u + 1, v + 1], "uv_size": [SW - 2, SW - 2]} for f in ("north", "south", "east", "west", "up", "down")}

    def save(self, h, name, glowmask=False):
        img = Image.fromarray(np.clip(self.img, 0, 255).astype(np.uint8))
        h.save(img, "entity", f"{name}.png")
        if glowmask:
            h.save(Image.fromarray(np.clip(self.glow, 0, 255).astype(np.uint8)), "entity", f"{name}_glowmask.png")


def cube(sheet, swatch, origin, size, pivot=None, rotation=None, inflate=0):
    c = {"origin": [round(o, 3) for o in origin], "size": [round(s, 3) for s in size], "uv": sheet.faces(swatch)}
    if rotation:
        c["pivot"], c["rotation"] = pivot, rotation
    if inflate:
        c["inflate"] = inflate
    return c


def bone(name, parent, pivot, cubes=(), rotation=None):
    b = {"name": name, "pivot": pivot, "cubes": list(cubes)}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = rotation
    return b


def write_geo(h, name, bones, width, height):
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{name}", "texture_width": UNITS, "texture_height": UNITS,
                        "visible_bounds_width": width, "visible_bounds_height": height, "visible_bounds_offset": [0, height / 2, 0]},
        "bones": bones}]}
    h.w(f"{h.A}/geo/entity/{name}.geo.json", geo)


def write_anims(h, name, anims):
    h.w(f"{h.A}/animations/entity/{name}.animation.json", {"format_version": "1.8.0", "animations": {
        f"animation.{name}.{k}": v for k, v in anims.items()}})


def loop(length, bones):
    return {"loop": True, "animation_length": length, "bones": bones}


def once(length, bones):
    return {"animation_length": length, "bones": bones}


def wave(length, amp, axis=0, phase=0.0, steps=8, offset=None):
    """Keyframes of a sine on one axis (rotation or position), `phase` in turns."""
    out = {}
    for i in range(steps + 1):
        t = length * i / steps
        v = [0.0, 0.0, 0.0] if offset is None else list(offset)
        v[axis] += round(amp * math.sin(2 * math.pi * (i / steps + phase)), 3)
        out[f"{t:.3f}"] = v
    return out


def spin(length, axis=1, turns=1, steps=8):
    return {f"{length * i / steps:.3f}": [360 * turns * i / steps if a == axis else 0 for a in range(3)] for i in range(steps + 1)}


# =====================================================================================================================
# texture helpers
# =====================================================================================================================
def rng_for(seed):
    return np.random.default_rng(seed)


def noise(base, var, rng, size=SW * PX):
    a = np.array(base, np.float32)[None, None, :].repeat(size, 0).repeat(size, 1)
    return np.clip(a + rng.integers(-var, var + 1, (size, size, 1)), 0, 255).astype(np.float32)


def blobs(a, colour, rng, n=8, rmin=2, rmax=6, alpha=0.5):
    s = a.shape[0]
    for _ in range(n):
        x, y = rng.integers(0, s, 2)
        r = rng.integers(rmin, rmax + 1)
        yy, xx = np.ogrid[:s, :s]
        m = (xx - x) ** 2 + (yy - y) ** 2 < r * r
        a[m, :3] = a[m, :3] * (1 - alpha) + np.array(colour, np.float32) * alpha
    return a


def cracks(a, colour, rng, n=5):
    s = a.shape[0]
    for _ in range(n):
        x, y = rng.integers(0, s, 2).astype(float)
        ang = rng.uniform(0, 2 * math.pi)
        for _step in range(rng.integers(8, 20)):
            ix, iy = int(x) % s, int(y) % s
            a[iy, ix, :3] = colour
            ang += rng.uniform(-0.7, 0.7)
            x += math.cos(ang)
            y += math.sin(ang)
    return a


def recolour_ramp(base, dark, light, keep=None):
    """Every pinkish pixel of a Mika / guh sheet, painted along a ramp by its brightness; `keep` pixels stay."""
    a = np.asarray(base.convert("RGBA")).astype(np.float32)
    rgb = a[..., :3] / 255
    mx, mn = rgb.max(-1), rgb.min(-1)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0)
    lum = rgb[..., 0] * .3 + rgb[..., 1] * .59 + rgb[..., 2] * .11
    fur = (a[..., 3] > 0) & (lum > 0.45) & (sat < 0.45)
    if keep is not None:
        fur &= ~keep(a)
    lo, hi = np.percentile(lum[fur], 2), np.percentile(lum[fur], 98)
    t = np.clip((lum - lo) / max(hi - lo, 1e-3), 0, 1)[..., None]
    col = np.array(dark, np.float32) * (1 - t) + np.array(light, np.float32) * t
    a[..., :3] = np.where(fur[..., None], col, a[..., :3])
    return a, fur


# =====================================================================================================================
# the Rookguh (2.8: a real ghast, about 3/4 of the size, with a flat guh face)
# =====================================================================================================================
ROOK_UNITS = 64          # the Rookguh's own sheet: 64x64 units, 4 px per unit (256x256 px)
ROOK_SCALE = 3.375       # the renderer draws the 16-unit body this many blocks wide (the vanilla ghast: 4.5; 3/4 of that)
# the nine tentacles of the ghast: (x, z, length), in the vanilla 3x3 grid with every other row shifted half a step
ROOK_TENTACLES = [(-3.75, -5, 11), (1.25, -5, 9), (6.25, -5, 13), (-6.25, 0, 10), (-1.25, 0, 14), (3.75, 0, 8),
                  (-3.75, 5, 12), (1.25, 5, 10), (6.25, 5, 12)]
ROOK_FACE_TOP = 13       # px row of the face side where the guh face (the eyes) starts


def rook_body_px(rng, size=64, base=(238, 237, 241), dark=False):
    """One ghast-like body side, painted on the unit grid (4 px blocks: the ghast's big pixels): soft white with grey
    mottles and a few darker specks; the bottom is a bit greyer."""
    u = size // PX
    a = np.zeros((u, u, 3), np.float32) + np.array(base, np.float32) * (0.9 if dark else 1.0)
    a += rng.integers(-5, 6, (u, u, 1))
    for _ in range(7 if dark else 5):                                          # grey mottles
        cx, cy, r = rng.integers(0, u), rng.integers(0, u), rng.integers(1, 3)
        yy, xx = np.ogrid[:u, :u]
        m = (xx - cx) ** 2 + (yy - cy) ** 2 <= r * r
        a[m] = a[m] * 0.55 + np.array((206, 204, 212), np.float32) * 0.45
    for _ in range(4):                                                        # a few darker specks
        cx, cy = rng.integers(0, u), rng.integers(0, u)
        a[cy, cx] = (188, 186, 196)
    a[0, :] *= 0.96                                                           # a slightly darker rim
    a[-1, :] *= 0.93
    a[:, 0] *= 0.96
    a[:, -1] *= 0.96
    return np.clip(a, 0, 255).repeat(PX, 0).repeat(PX, 1)


def rook_face(h, px):
    """The guh face on the front of the ghast: the eyes and snoet of guh.png (its fur swapped for the ghast's white),
    a little pink nose and blush, where the ghast has its sad face."""
    guh = np.asarray(Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")).astype(np.float32)
    face = guh[49 * PX:60 * PX, 0:14 * PX]                                      # the guh head's front (14x11 units)
    rgb = face[..., :3]
    d = np.abs(rgb - np.array((195, 160, 205), np.float32)).sum(-1)
    lum = rgb @ np.array((0.3, 0.59, 0.11), np.float32)
    mx, mn = rgb.max(-1), rgb.min(-1)
    sat = (mx - mn) / np.maximum(mx, 1)
    is_fur = (d < 70) & (face[..., 3] > 0)
    lilac = ~is_fur & (sat < 0.35) & (lum > 90) & (face[..., 3] > 0)           # the mouth lines: lilac shading
    y0, x0 = ROOK_FACE_TOP, (64 - face.shape[1]) // 2
    region = px[y0:y0 + face.shape[0], x0:x0 + face.shape[1], :3]
    out = region.copy()
    keep = ~is_fur & ~lilac & (face[..., 3] > 0)
    out[keep] = rgb[keep]
    out[lilac] = region[lilac] * 0.55 + np.array((120, 110, 130), np.float32) * 0.45
    px[y0:y0 + face.shape[0], x0:x0 + face.shape[1], :3] = out
    ny = y0 + 24                                                               # the snoet: a little pink nose
    for y, (a, b) in enumerate(((29, 35), (28, 36), (29, 35))):
        px[ny + y, a:b, :3] = (236, 128, 166) if y < 2 else (206, 98, 140)
    px[ny, 30:32, :3] = (255, 196, 216)
    for cx in (12, 52):                                                        # blush under the eyes
        for y in range(ny + 3, ny + 9):
            for x in range(cx - 6, cx + 6):
                e = ((x - cx) / 6.0) ** 2 + ((y - ny - 6) / 3.0) ** 2
                if e < 1:
                    px[y, x, :3] = px[y, x, :3] * (0.45 + 0.4 * e) + np.array((250, 150, 180), np.float32) * (0.55 - 0.4 * e)
    return px


def rook_tentacle_px(rng):
    """A tentacle side (2 x 14 units): white-grey, a little greyer towards the tip, with faint bands."""
    a = np.zeros((14, 2, 3), np.float32) + np.array((232, 231, 236), np.float32)
    a += rng.integers(-5, 6, (14, 2, 1))
    for y in range(14):
        a[y] *= 1.0 - 0.1 * y / 13
        if y % 4 == 3:
            a[y] *= 0.95
    return np.clip(a, 0, 255).repeat(PX, 0).repeat(PX, 1)


def rookguh(h):
    """The Rookguh (2.8): the vanilla ghast's shape (a 16-unit cube with nine dangling tentacles), drawn ROOK_SCALE
    blocks big by the renderer, with the guh face painted flat on its front. Our own painted texture."""
    rng = rng_for(2028)
    U = ROOK_UNITS
    img = np.zeros((U * PX, U * PX, 4), np.float32)

    def put(u, v, px):
        hh, ww = px.shape[:2]
        img[v * PX:v * PX + hh, u * PX:u * PX + ww, :3] = px[..., :3]
        img[v * PX:v * PX + hh, u * PX:u * PX + ww, 3] = 255

    faces = {"north": (0, 0), "south": (16, 0), "east": (32, 0), "west": (48, 0), "up": (0, 16), "down": (16, 16)}
    for f, (u, v) in faces.items():
        put(u, v, rook_face(h, rook_body_px(rng)) if f == "north" else rook_body_px(rng, dark=(f == "down")))
    for i in range(4):                                                           # four tentacle patterns
        put(32 + 2 * i, 16, rook_tentacle_px(rng))
    put(40, 16, np.zeros((2 * PX, 2 * PX, 3), np.float32) + np.array((206, 204, 214), np.float32))
    wang = np.zeros((2 * PX, 4 * PX, 3), np.float32) + np.array((250, 146, 180), np.float32)
    wang[0] = wang[-1] = (252, 176, 200)
    put(48, 16, wang)

    def uvs(u, v, w_, h_):
        return {"uv": [u, v], "uv_size": [w_, h_]}
    # "rook": no cubes, the renderer makes it plumper as it eats (an absolute scale, apart from the animations of "body")
    rook = bone("rook", "root", [0, 8.5, 0])
    body = bone("body", "rook", [0, 8.5, 0], [{"origin": [-8, 0.5, -8], "size": [16, 16, 16],
                                               "uv": {f: uvs(u, v, 16, 16) for f, (u, v) in faces.items()}}])
    tentacles = []
    for i, (x, z, length) in enumerate(ROOK_TENTACLES):
        side = uvs(32 + 2 * (i % 4), 16 + (14 - length), 2, length)
        tentacles.append(bone(f"tentacle_{i}", "body", [x, 0.5, z], [{
            "origin": [x - 1, 0.5 - length, z - 1], "size": [2, length, 2],
            "uv": {"north": side, "south": side, "east": side, "west": side, "up": uvs(40, 16, 2, 2), "down": uvs(40, 16, 2, 2)}}]))
    # rosy cheeks that show up as it eats (the renderer hides them while it's skinny), just in front of the face
    wang_uv = {f: uvs(48, 16, 4, 2) for f in ("north", "south", "east", "west", "up", "down")}
    cy = 16.5 - (ROOK_FACE_TOP + 24 + 6) / PX
    cheeks = bone("cheeks", "body", [0, cy, -8], [
        {"origin": [3.5, cy - 0.75, -8.15], "size": [3, 1.5, 0.1], "uv": wang_uv},
        {"origin": [-6.5, cy - 0.75, -8.15], "size": [3, 1.5, 0.1], "uv": wang_uv}])
    bones = [bone("root", None, [0, 0, 0]), rook, body] + tentacles + [cheeks]
    h.w(f"{h.A}/geo/entity/rookguh.geo.json", {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.rookguh", "texture_width": U, "texture_height": U,
                        "visible_bounds_width": 2, "visible_bounds_height": 3, "visible_bounds_offset": [0, 0.5, 0]},
        "bones": bones}]})
    h.save(Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)), "entity", "rookguh.png")

    def sway(length, amp, base, i, steps=12):
        """The ghast's tentacle sway (vanilla: xRot = 0.2 sin(age * 0.3 + i) + 0.4), per tentacle out of step."""
        return {f"{length * k / steps:.3f}": [round(base + amp * math.sin(2 * math.pi * k / steps + i), 3), 0, 0] for k in range(steps + 1)}
    calm = {f"tentacle_{i}": {"rotation": sway(1.6, 11, -20, i)} for i in range(9)}     # (negative: trailing back)
    wild = {f"tentacle_{i}": {"rotation": sway(0.6, 22, -12, i)} for i in range(9)}
    write_anims(h, "rookguh", {
        "float": loop(3.2, {"body": {"position": wave(3.2, 1.0, axis=1, steps=12)}, **calm}),
        "eat": once(0.6, {"body": {"scale": {"0.0": [1, 1, 1], "0.15": [1.06, 0.92, 1.06], "0.3": [1, 1, 1], "0.45": [1.06, 0.92, 1.06],
                                             "0.6": [1, 1, 1]}}}),
        "vahoeg": loop(0.8, {"body": {"rotation": wave(0.8, 8, axis=2), "position": wave(0.8, 1.5, axis=1, phase=0.25)}, **wild}),
    })
    return bones


def rookguh_check(h, bones):
    """The Rookguh really is a ghast: one 16-unit cube body, nine tentacles hanging under it, guh eyes on the front."""
    p = []
    body = [b for b in bones if b["name"] == "body"][0]
    if len(body["cubes"]) != 1 or body["cubes"][0]["size"] != [16, 16, 16]:
        p.append("rookguh: the body must be one 16x16x16 cube")
    tents = [b for b in bones if b["name"].startswith("tentacle_")]
    if len(tents) != 9:
        p.append(f"rookguh: {len(tents)} tentacles (the ghast has 9)")
    for t in tents:
        c = t["cubes"][0]
        if c["origin"][1] + c["size"][1] > body["cubes"][0]["origin"][1] + 1e-6 or not 8 <= c["size"][1] <= 14:
            p.append(f"rookguh: {t['name']} doesn't hang under the body")
        if not all(abs(c["origin"][i] + c["size"][i] / 2) <= 7 for i in (0, 2)):
            p.append(f"rookguh: {t['name']} sticks out beside the body")
    img = np.asarray(Image.open(os.path.join(h.TEX, "entity", "rookguh.png")).convert("RGBA")).astype(np.float32)
    front = img[:64, :64]
    dark = int((front[..., :3].sum(-1) < 150).sum())
    blue = int(((front[..., 2] > front[..., 0] + 40) & (front[..., 3] > 0)).sum())
    if dark < 150 or blue < 150:
        p.append(f"rookguh: no guh eyes on the face (dark {dark}, blue {blue})")
    light = img[:64, 64:256, :3].mean()
    if light < 205:
        p.append(f"rookguh: the body isn't ghast-white (mean {light:.0f})")
    return p


# =====================================================================================================================
# Mika-based models
# =====================================================================================================================
def mika_sheet(h, dark, light, used, keep=None):
    base = Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")
    a, fur = recolour_ramp(base, dark, light, keep)
    return Sheet(Image.fromarray(a.astype(np.uint8)), used), a, fur


def eyes_mask(a):
    """The Mika's red eyes."""
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    return (r > 150) & (g < 90) & (b < 90) & (a[..., 3] > 0)


def vonk_mika(h):
    mika = load_geo(h, "mika")
    s, sh = 0.62, (0, 15, 4)
    head = moved(mika["head"], "head", "root", s, sh)
    ears = [moved(mika[e], e, "head", s, sh) for e in ("ear_left", "ear_right")]
    sheet, a, fur = mika_sheet(h, (120, 30, 10), (255, 170, 60), [head] + ears)
    rng = rng_for(22)
    eyes = eyes_mask(np.asarray(Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")).astype(np.float32))
    sheet.img[eyes, :3] = (255, 240, 120)
    sheet.glow[eyes] = (255, 240, 120, 255)
    speck = fur & (rng.random(fur.shape) < 0.05)
    sheet.img[speck, :3] = (255, 230, 120)
    sheet.glow[speck] = (255, 200, 90, 255)
    sheet.swatch("stok", lambda: noise((150, 104, 60), 10, rng))
    sheet.swatch("sate", lambda: blobs(noise((150, 70, 34), 14, rng), (200, 130, 60), rng, n=6, alpha=0.6))
    sheet.swatch("worst", lambda: blobs(noise((176, 70, 50), 10, rng), (120, 40, 30), rng, n=5, alpha=0.5))
    sheet.swatch("ui", lambda: noise((240, 226, 190), 8, rng))
    sheet.swatch("kern", lambda: blobs(noise((255, 140, 30), 20, rng), (255, 230, 120), rng, n=8, alpha=0.7), glow=True)
    sheet.swatch("vlam", lambda: blobs(noise((255, 190, 60), 20, rng), (255, 90, 20), rng, n=6, alpha=0.6), glow=True)
    rings = []
    for name, y, r, off in (("rods_top", 18, 10, 45), ("rods_mid", 11, 8.5, 0), ("rods_low", 4, 6, 45)):
        cubes = []
        for k in range(4):
            ang = math.radians(off + 90 * k)
            x, z = round(r * math.cos(ang), 2), round(r * math.sin(ang), 2)
            cubes += [cube(sheet, "stok", [x - 0.5, y - 4, z - 0.5], [1, 9, 1]),
                      cube(sheet, "sate", [x - 1, y + 1.8, z - 1], [2, 2, 2]),
                      cube(sheet, "ui", [x - 0.9, y + 1, z - 0.9], [1.8, 0.8, 1.8]),
                      cube(sheet, "worst", [x - 1, y - 2.2, z - 1], [2, 3, 2])]
        rings.append(bone(name, "root", [0, y, 0], cubes))
    kern = bone("kern", "root", [0, 9, 0], [cube(sheet, "kern", [-3, 7, -3], [6, 6, 6]), cube(sheet, "vlam", [-2, 3.5, -2], [4, 4, 4]),
                                             cube(sheet, "vlam", [-1, 0.5, -1], [2, 3, 2])])
    flames = bone("vlammetjes", "head", [0, 23.5, 0], [cube(sheet, "vlam", [-3, 23.4, -2], [2, 2.5, 2]), cube(sheet, "vlam", [1, 23.4, -1], [2, 3, 2]),
                                                        cube(sheet, "vlam", [-0.8, 23.4, 1], [1.6, 2, 1.6])])
    bones = [bone("root", None, [0, 0, 0]), kern] + rings + [head] + ears + [flames]
    write_geo(h, "vonk_mika", bones, 2.5, 2.5)
    sheet.save(h, "vonk_mika", glowmask=True)
    write_anims(h, "vonk_mika", {
        "idle": loop(2.0, {"rods_top": {"rotation": spin(2.0)}, "rods_mid": {"rotation": spin(2.0, turns=-1)},
                           "rods_low": {"rotation": spin(2.0, turns=2)}, "head": {"position": wave(2.0, 0.8, axis=1)},
                           "kern": {"position": wave(1.0, 0.5, axis=1, steps=4), "rotation": spin(2.0)},
                           "vlammetjes": {"scale": {"0.0": [1, 1, 1], "0.5": [1.1, 1.4, 1.1], "1.0": [1, 1, 1], "1.5": [0.9, 1.2, 0.9], "2.0": [1, 1, 1]}}}),
        "shoot": once(0.4, {"head": {"rotation": {"0.0": [0, 0, 0], "0.1": [-14, 0, 0], "0.4": [0, 0, 0]}},
                            "kern": {"scale": {"0.0": [1, 1, 1], "0.1": [1.4, 1.4, 1.4], "0.4": [1, 1, 1]}}}),
    })
    return bones


def knekel_mika(h):
    mika = load_geo(h, "mika")
    s, sh = 0.75, (0, 28, 5)
    head = moved(mika["head"], "head", "body", s, sh)
    ears = [moved(mika[e], e, "head", s, sh) for e in ("ear_left", "ear_right")]
    tail = moved(mika["tail"], "tail", "body", 0.7, (0, 13.5, -5))
    sheet, a, fur = mika_sheet(h, (22, 18, 20), (74, 66, 68), [head, tail] + ears)
    rng = rng_for(23)
    eyes = eyes_mask(np.asarray(Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")).astype(np.float32))
    sheet.img[eyes, :3] = (255, 60, 40)
    sheet.glow[eyes] = (255, 70, 40, 255)
    sheet.swatch("bot", lambda: cracks(blobs(noise((58, 52, 54), 10, rng), (96, 90, 90), rng, n=5, alpha=0.4), (18, 14, 16), rng, 4))
    sheet.swatch("bot_licht", lambda: cracks(noise((110, 104, 102), 10, rng), (30, 26, 28), rng, 5))
    sheet.swatch("ijzer", lambda: noise((70, 70, 78), 10, rng))
    sheet.swatch("gloed", lambda: blobs(noise((255, 110, 30), 20, rng), (255, 220, 100), rng, n=6), glow=True)
    sheet.swatch("hoorn", lambda: noise((40, 30, 32), 8, rng))
    body = bone("body", "root", [0, 14, 0], [
        cube(sheet, "bot", [-1, 15.5, -0.5], [2, 12.5, 1.6]),                      # spine
        cube(sheet, "bot_licht", [-4.5, 25, -2.2], [9, 1.2, 4.2]),                  # ribs
        cube(sheet, "bot_licht", [-4, 22.5, -2], [8, 1.2, 4]),
        cube(sheet, "bot_licht", [-3.5, 20, -1.8], [7, 1.2, 3.6]),
        cube(sheet, "bot", [-3.5, 13.6, -1.6], [7, 2.4, 3.2]),                      # pelvis
        cube(sheet, "bot", [-6, 27, -1.2], [12, 1.6, 2.4]),                         # shoulders
    ])
    horns = bone("horns", "head", [0, 38, 0], [
        cube(sheet, "hoorn", [2.6, 38.3, -1.5], [1.6, 2.6, 1.6]), cube(sheet, "hoorn", [3.2, 40.6, -1.1], [1, 1.6, 1]),
        cube(sheet, "hoorn", [-4.2, 38.3, -1.5], [1.6, 2.6, 1.6]), cube(sheet, "hoorn", [-4.2, 40.6, -1.1], [1, 1.6, 1])])
    arm_l = bone("arm_left", "body", [5.5, 27.5, 0], [cube(sheet, "bot_licht", [5, 14.5, -1], [2, 13.5, 2]),
                                                      cube(sheet, "bot", [4.8, 13, -1.2], [2.4, 1.8, 2.4])])
    arm_r = bone("arm_right", "body", [-5.5, 27.5, 0], [cube(sheet, "bot_licht", [-7, 14.5, -1], [2, 13.5, 2]),
                                                        cube(sheet, "bot", [-7.2, 13, -1.2], [2.4, 1.8, 2.4])])
    fork = bone("fork", "arm_right", [-6, 14, 0], [
        cube(sheet, "ijzer", [-6.5, 13.6, -12], [1, 1, 15]),                        # the handle
        cube(sheet, "ijzer", [-8.5, 13.6, -13], [5, 1, 1]),                         # the crossbar
        cube(sheet, "gloed", [-8.5, 13.6, -17], [1, 1, 4]), cube(sheet, "gloed", [-6.5, 13.6, -17], [1, 1, 4]),
        cube(sheet, "gloed", [-4.5, 13.6, -17], [1, 1, 4])])
    leg_l = bone("leg_left", "root", [2, 14, 0], [cube(sheet, "bot_licht", [1.2, 1, -1], [2, 13.5, 2]),
                                                  cube(sheet, "bot", [0.8, 0, -2.2], [2.8, 1.2, 3.6])])
    leg_r = bone("leg_right", "root", [-2, 14, 0], [cube(sheet, "bot_licht", [-3.2, 1, -1], [2, 13.5, 2]),
                                                    cube(sheet, "bot", [-3.6, 0, -2.2], [2.8, 1.2, 3.6])])
    bones = [bone("root", None, [0, 0, 0]), body, head] + ears + [horns, tail, arm_l, arm_r, fork, leg_l, leg_r]
    write_geo(h, "knekel_mika", bones, 2, 3.2)
    sheet.save(h, "knekel_mika", glowmask=True)
    write_anims(h, "knekel_mika", {
        "idle": loop(2.4, {"body": {"rotation": wave(2.4, 1.5, axis=0)}, "arm_left": {"rotation": wave(2.4, 4, axis=2, offset=(0, 0, -4))},
                           "arm_right": {"rotation": wave(2.4, -4, axis=2, offset=(-10, 0, 4))}, "head": {"rotation": wave(2.4, 3, axis=0, phase=0.25)},
                           "tail": {"rotation": wave(2.4, 15, axis=1)}}),
        "walk": loop(1.0, {"leg_left": {"rotation": wave(1.0, 28, axis=0)}, "leg_right": {"rotation": wave(1.0, -28, axis=0)},
                           "arm_left": {"rotation": wave(1.0, -22, axis=0)}, "arm_right": {"rotation": wave(1.0, 10, axis=0, offset=(-12, 0, 0))},
                           "body": {"position": wave(0.5, 0.4, axis=1, steps=4)}, "tail": {"rotation": wave(1.0, 20, axis=1)}}),
        "attack": once(0.5, {"arm_right": {"rotation": {"0.0": [0, 0, 0], "0.15": [-75, 0, 0], "0.3": [25, 0, 0], "0.5": [0, 0, 0]}},
                             "body": {"rotation": {"0.0": [0, 0, 0], "0.2": [8, 0, 0], "0.5": [0, 0, 0]}}}),
    })
    return bones


def aangebrande_mika(h):
    mika = load_geo(h, "mika")
    body = moved(mika["body"], "body", "root", 1.9, (0, 6, -5), centre=(0, 0, 5))
    tail = moved(mika["tail"], "tail", "body", 1.9, (0, 6, -5), centre=(0, 0, 5))
    heads = []
    for name, s, sh in (("mid", 1.25, (0, 26, 2)), ("left", 0.85, (16, 23, 0)), ("right", 0.85, (-16, 23, 0))):
        heads.append(moved(mika["head"], f"head_{name}", "schouders", s, sh))
        heads += [moved(mika[e], f"{e}_{name}", f"head_{name}", s, sh) for e in ("ear_left", "ear_right")]
    sheet, a, fur = mika_sheet(h, (18, 14, 15), (70, 58, 56), [body, tail] + heads)
    rng = rng_for(24)
    src = np.asarray(Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")).astype(np.float32)
    eyes = eyes_mask(src)
    sheet.img[eyes, :3] = (255, 150, 30)
    sheet.glow[eyes] = (255, 160, 40, 255)
    # glowing ember cracks all over the charred skin
    crack = np.zeros(fur.shape, bool)
    for _ in range(260):
        x, y = rng.integers(0, fur.shape[1]), rng.integers(0, fur.shape[0])
        ang = rng.uniform(0, 2 * math.pi)
        for _step in range(rng.integers(4, 12)):
            ix, iy = int(x) % fur.shape[1], int(y) % fur.shape[0]
            crack[iy, ix] = True
            ang += rng.uniform(-0.8, 0.8)
            x += math.cos(ang)
            y += math.sin(ang)
    crack &= fur
    blue = (src[..., 2] > src[..., 0] + 40) & (src[..., 3] > 0)
    sheet.img[blue, :3] = (40, 30, 30)
    sheet.img[crack, :3] = (255, 120, 30)
    sheet.glow[crack] = (255, 130, 40, 255)
    sheet.swatch("rooster", lambda: noise((40, 40, 46), 8, rng))
    sheet.swatch("gloed", lambda: blobs(noise((255, 110, 30), 20, rng), (255, 220, 100), rng, n=8), glow=True)
    sheet.swatch("as", lambda: blobs(noise((96, 90, 90), 12, rng), (140, 134, 132), rng, n=8, alpha=0.5))
    sheet.swatch("hoorn", lambda: noise((30, 22, 24), 8, rng))
    bar = bone("schouders", "root", [0, 26, -4], [
        cube(sheet, "rooster", [-23, 25, -6], [46, 3, 5]),
        cube(sheet, "rooster", [-23, 23, -5.5], [1, 5, 4]), cube(sheet, "rooster", [22, 23, -5.5], [1, 5, 4])]
        + [cube(sheet, "gloed", [x, 28, -4.5], [1.5, 1, 2]) for x in range(-20, 21, 5)])
    coals = bone("kooltjes", "body", [0, 26, 8], [
        cube(sheet, "gloed", [-6, 24.5, 2], [4, 3, 4]), cube(sheet, "gloed", [2, 24.8, 5], [5, 2.5, 4]),
        cube(sheet, "gloed", [-3, 24.6, 9], [4, 2.5, 3]), cube(sheet, "gloed", [5, 24, 10], [3, 2, 3])])
    skirt = bone("rookrok", "root", [0, 6, 0], [
        cube(sheet, "as", [-9, 1, -8], [18, 6, 16]), cube(sheet, "as", [-6, -2, -5], [12, 4, 11]), cube(sheet, "as", [-3, -4, -2], [6, 3, 6])])
    horns = []
    for name, s, x0, y0 in (("mid", 1.25, 0, 43), ("left", 0.85, 16, 35), ("right", 0.85, -16, 35)):
        w = 2.2 * s
        horns.append(bone(f"horns_{name}", f"head_{name}", [x0, y0, 0], [
            cube(sheet, "hoorn", [x0 + 3.5 * s, y0, -6 * s], [w, 4 * s, w]), cube(sheet, "hoorn", [x0 + 4 * s, y0 + 4 * s, -5.5 * s], [w / 2, 2.5 * s, w / 2]),
            cube(sheet, "hoorn", [x0 - 3.5 * s - w, y0, -6 * s], [w, 4 * s, w]), cube(sheet, "hoorn", [x0 - 4 * s - w / 2, y0 + 4 * s, -5.5 * s], [w / 2, 2.5 * s, w / 2])]))
    bones = [bone("root", None, [0, 0, 0]), skirt, body, tail, coals, bar] + heads + horns
    write_geo(h, "aangebrande_mika", bones, 4, 4)
    sheet.save(h, "aangebrande_mika", glowmask=True)
    heads_idle = {"head_left": {"rotation": wave(3.0, 6, axis=2)}, "head_right": {"rotation": wave(3.0, -6, axis=2, phase=0.3)},
                  "head_mid": {"rotation": wave(3.0, 4, axis=0, phase=0.5)}}
    write_anims(h, "aangebrande_mika", {
        "idle": loop(3.0, {"root": {"position": wave(3.0, 1.5, axis=1)}, "tail": {"rotation": wave(3.0, 18, axis=1)},
                           "rookrok": {"rotation": spin(3.0)}, **heads_idle}),
        "spawn": loop(0.6, {"root": {"rotation": wave(0.6, 3, axis=2, steps=6)}, "head_left": {"rotation": {"0.0": [25, 0, 0]}},
                            "head_right": {"rotation": {"0.0": [25, 0, 0]}}, "head_mid": {"rotation": {"0.0": [20, 0, 0]}},
                            "rookrok": {"rotation": spin(0.6)}}),
        "charge": loop(0.5, {"head_left": {"rotation": {"0.0": [-12, 0, 0]}, "scale": wave(0.5, 0.06, axis=0, steps=4, offset=(1, 1, 1))},
                             "head_right": {"rotation": {"0.0": [-12, 0, 0]}, "scale": wave(0.5, 0.06, axis=0, steps=4, offset=(1, 1, 1))},
                             "head_mid": {"rotation": {"0.0": [-10, 0, 0]}, "scale": wave(0.5, 0.06, axis=0, steps=4, offset=(1, 1, 1))},
                             "root": {"position": wave(0.5, 0.5, axis=1, steps=4)}, "rookrok": {"rotation": spin(0.5, turns=2)}}),
        "rest": loop(2.0, {"root": {"position": wave(2.0, 0.8, axis=1, offset=(0, -3, 0))}, "head_left": {"rotation": {"0.0": [30, 0, 10]}},
                           "head_right": {"rotation": {"0.0": [30, 0, -10]}}, "head_mid": {"rotation": wave(2.0, 5, axis=0, offset=(28, 0, 0))},
                           "tail": {"rotation": {"0.0": [-20, 0, 0]}}}),
        "shoot": once(0.3, {"head_mid": {"position": {"0.0": [0, 0, 0], "0.08": [0, 0, 1.5], "0.3": [0, 0, 0]}},
                            "head_left": {"position": {"0.0": [0, 0, 0], "0.08": [0, 0, 1.2], "0.3": [0, 0, 0]}},
                            "head_right": {"position": {"0.0": [0, 0, 0], "0.08": [0, 0, 1.2], "0.3": [0, 0, 0]}}}),
    })
    return bones


def asguh_cheeks(h):
    """The Asguh's glowmask: its cheeks (and eyes' shine) glow like embers."""
    base = Image.open(os.path.join(h.TEX, "entity", "guh_asguh.png")).convert("RGBA")
    a = np.asarray(base).astype(np.float32)
    glow = np.zeros_like(a)
    cheek = (a[..., 0] > 200) & (a[..., 1] < 150) & (a[..., 2] < 110) & (a[..., 3] > 0)
    glow[cheek] = a[cheek]
    h.save(Image.fromarray(glow.astype(np.uint8)), "entity", "guh_asguh_glowmask.png")
    return int(cheek.sum())


def build(h):
    models = {"rookguh": rookguh(h), "vonk_mika": vonk_mika(h), "knekel_mika": knekel_mika(h), "aangebrande_mika": aangebrande_mika(h)}
    problems = rookguh_check(h, models["rookguh"])
    for name, bones in models.items():
        names = {b["name"] for b in bones}
        for b in bones:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: bone {b['name']} has a missing parent {b['parent']}")
        for need in ("root",):
            if need not in names:
                problems.append(f"{name}: no {need} bone")
        anims = json.load(open(f"{h.A}/animations/entity/{name}.animation.json", encoding="utf-8"))["animations"]
        for anim in anims.values():
            for b in anim["bones"]:
                if b not in names:
                    problems.append(f"{name}: animation moves a missing bone {b}")
    if problems:
        raise SystemExit("spiesburcht models: " + "; ".join(problems))
    return models
