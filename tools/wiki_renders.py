"""
Renders the pictures for the Guhs wiki (docs): mobs from their .geo.json models, blocks from their block models,
item icons, and isometric views of the structures. Everything comes from the mod's own files, so the wiki
always matches the mod.

Usage (from the project root):  python tools/wiki_renders.py <output folder>   (add --only-29 / --only-210 / --only-30 / --only-128 /
--only-px / --only-bbq2 for just the pictures of 2.9 / 2.10 / 3.0 / 1.2.8 / Guhpixel / bbq2; -h shows this text)
Requires: pillow, numpy
"""
import collections
import json
import math
import os
import sys
import zipfile
from functools import lru_cache

import numpy as np
from PIL import Image

ASSETS = os.path.join("src", "main", "resources", "assets", "guhs")
VANILLA_JAR = os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar")
_jar = zipfile.ZipFile(VANILLA_JAR) if os.path.exists(VANILLA_JAR) else None


# ---------------------------------------------------------------------------------------------------------------------
# textures
# ---------------------------------------------------------------------------------------------------------------------
@lru_cache(maxsize=None)
def texture(ref):
    """'guhs:block/x' / 'minecraft:item/y' / 'block/z' -> RGBA image (first frame of animated textures)."""
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    if ns == "guhs":
        img = Image.open(os.path.join(ASSETS, "textures", path + ".png")).convert("RGBA")
    else:
        with _jar.open(f"assets/minecraft/textures/{path}.png") as f:
            img = Image.open(f).convert("RGBA")
            img.load()
    if img.height > img.width:  # animated: first frame
        img = img.crop((0, 0, img.width, img.width))
    return img


@lru_cache(maxsize=None)
def tex_array(ref):
    return np.asarray(texture(ref)).astype(np.float32)


# ---------------------------------------------------------------------------------------------------------------------
# a tiny textured-quad renderer (orthographic, z-buffered point splatting, supersampled)
# ---------------------------------------------------------------------------------------------------------------------
class Quad:
    def __init__(self, origin, u, v, tex, uv, normal, tint=None):
        self.origin, self.u, self.v = np.array(origin, float), np.array(u, float), np.array(v, float)
        self.tex, self.uv, self.normal, self.tint = tex, uv, np.array(normal, float), tint


def rot_matrix(yaw, pitch):
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rx = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    return rx @ ry


def render(quads, yaw=35, pitch=-25, size=512, margin=0.08, ss=2):
    """Render quads (world units) to a transparent RGBA image of `size` px, auto-fitted."""
    R = rot_matrix(yaw, pitch)
    corners = []
    for q in quads:
        for a in (0, 1):
            for b in (0, 1):
                corners.append(R @ (q.origin + a * q.u + b * q.v))
    corners = np.array(corners)
    lo, hi = corners[:, :2].min(0), corners[:, :2].max(0)
    span = max(hi - lo) or 1
    W = size * ss
    scale = W * (1 - 2 * margin) / span
    center = (lo + hi) / 2
    zbuf = np.full((W, W), np.inf)
    col = np.zeros((W, W, 4), np.float32)
    light = np.array([-0.35, 0.85, -0.4])
    light /= np.linalg.norm(light)
    for q in quads:
        n = R @ q.normal
        if n[2] > 1e-6:  # facing away (camera looks down +z)
            continue
        shade = 0.62 + 0.38 * max(0.0, float(np.dot(q.normal, light)))
        tex = tex_array(q.tex) if isinstance(q.tex, str) else q.tex
        th, tw = tex.shape[:2]
        u0, v0, u1, v1 = q.uv
        lu = np.linalg.norm(R @ q.u) * scale
        lv = np.linalg.norm(R @ q.v) * scale
        nu, nv = max(2, int(lu * 1.6) + 1), max(2, int(lv * 1.6) + 1)
        a = (np.arange(nu) + 0.5) / nu
        b = (np.arange(nv) + 0.5) / nv
        A, B = np.meshgrid(a, b)
        P = q.origin + A[..., None] * q.u + B[..., None] * q.v
        S = P @ R.T
        px = ((S[..., 0] - center[0]) * scale + W / 2).astype(int)
        py = (-(S[..., 1] - center[1]) * scale + W / 2).astype(int)
        z = S[..., 2]
        tu = np.clip((u0 + A * (u1 - u0)) * tw, 0, tw - 1).astype(int)
        tv = np.clip((v0 + B * (v1 - v0)) * th, 0, th - 1).astype(int)
        rgba = tex[tv, tu].copy()
        if q.tint is not None:
            rgba[..., :3] *= np.array(q.tint[:3]) / 255.0
        m = (px >= 0) & (px < W) & (py >= 0) & (py < W) & (rgba[..., 3] > 20)
        px, py, z, rgba = px[m], py[m], z[m], rgba[m]
        order = np.argsort(-z)  # far first, so nearer points overwrite
        for X, Y, Z, C in zip(px[order], py[order], z[order], rgba[order]):
            if Z < zbuf[Y, X]:
                zbuf[Y, X] = Z
                col[Y, X, :3] = C[:3] * shade
                col[Y, X, 3] = 255
    img = Image.fromarray(np.clip(col, 0, 255).astype(np.uint8), "RGBA")
    return img.resize((size, size), Image.LANCZOS)


# ---------------------------------------------------------------------------------------------------------------------
# sources of quads
# ---------------------------------------------------------------------------------------------------------------------
def geo_quads(geo_path, tex_ref, hide=(), show_only_variant_bones=(), lift=None):
    """Quads for a GeckoLib/bedrock .geo.json model (units: blocks).
    The guh variant bones (clothes, neck) are hidden unless listed in show_only_variant_bones (prefixes);
    lift=(bone, (dx, dy, dz)) moves that bone and everything attached to it (the brontosaurus guh's head).
    Bone rotations (degrees, around the bone's pivot, also those of its parents) are applied like GeckoLib does
    (the Zeemeerguh character's curled mermaid tail)."""
    geo = json.load(open(geo_path))["minecraft:geometry"][0]
    tw, th = geo["description"]["texture_width"], geo["description"]["texture_height"]
    parents = {b["name"]: b.get("parent") for b in geo["bones"]}
    by_name = {b["name"]: b for b in geo["bones"]}

    def bone_rot(b):
        """GeckoLib turns a bedrock bone by Z, Y, X (X and Y negated, in its x-mirrored space); in ours: Rz(-z) Ry(y) Rx(-x)."""
        rx, ry, rz = (math.radians(a) for a in b.get("rotation", (0, 0, 0)))
        rx, rz = -rx, -rz
        X = np.array([[1, 0, 0], [0, math.cos(rx), -math.sin(rx)], [0, math.sin(rx), math.cos(rx)]])
        Y = np.array([[math.cos(ry), 0, math.sin(ry)], [0, 1, 0], [-math.sin(ry), 0, math.cos(ry)]])
        Z = np.array([[math.cos(rz), -math.sin(rz), 0], [math.sin(rz), math.cos(rz), 0], [0, 0, 1]])
        return Z @ Y @ X

    def transform(name):
        """(M, t): the bone's (and its parents') rotations as p -> M p + t, in blocks."""
        M, t = np.eye(3), np.zeros(3)
        while name:
            b = by_name[name]
            if any(b.get("rotation", (0, 0, 0))):
                R, piv = bone_rot(b), np.array(b.get("pivot", (0, 0, 0)), float) / 16
                M, t = R @ M, R @ (t - piv) + piv
            name = parents.get(name)
        return M, t

    def under(name, root):
        while name:
            if name == root:
                return True
            name = parents.get(name)
        return False
    quads = []
    for bone in geo["bones"]:
        if bone["name"] in hide:
            continue
        if bone["name"].startswith(("outfit_", "neck", "teckel", "ender", "koning", "wolk", "zeemeer", "asguh", "pluis", "pinguh", "balto", "mewtwo", "stitch", "samguh", "guhshi", "bloesem", "tanuki")) and not bone["name"].startswith(tuple(show_only_variant_bones) or ("-none-",)):
            continue
        shift = [v / 16 for v in lift[1]] if lift and under(bone["name"], lift[0]) else [0, 0, 0]
        M, t = transform(bone["name"])
        for c in bone.get("cubes", []):
            inf = c.get("inflate", 0)
            (x0, y0, z0) = [(v - inf) / 16 + sh for v, sh in zip(c["origin"], shift)]
            (w, h, d) = [(v + 2 * inf) / 16 for v in c["size"]]
            x1, y1, z1 = x0 + w, y0 + h, z0 + d
            faces = {
                "north": ((x1, y1, z0), (-w, 0, 0), (0, -h, 0), (0, 0, -1)),
                "south": ((x0, y1, z1), (w, 0, 0), (0, -h, 0), (0, 0, 1)),
                "east": ((x1, y1, z1), (0, 0, -d), (0, -h, 0), (1, 0, 0)),
                "west": ((x0, y1, z0), (0, 0, d), (0, -h, 0), (-1, 0, 0)),
                "up": ((x1, y1, z0), (-w, 0, 0), (0, 0, d), (0, 1, 0)),
                "down": ((x1, y0, z1), (-w, 0, 0), (0, 0, -d), (0, -1, 0)),
            }
            for name, (o, u, v, n) in faces.items():
                if name not in c.get("uv", {}):
                    continue
                (fu, fv), (fw, fh) = c["uv"][name]["uv"], c["uv"][name]["uv_size"]
                o, u, v, n = M @ np.array(o, float) + t, M @ np.array(u, float), M @ np.array(v, float), M @ np.array(n, float)
                quads.append(Quad(o, u, v, tex_ref, (fu / tw, fv / th, (fu + fw) / tw, (fv + fh) / th), n))
    return quads


def java_box_quads(tex, tex_size, origin, size, uv, pivot=(0, 0, 0), rot_x=0.0, inflate=0.0):
    """Quads for a vanilla (Java) entity model box, in world units (feet at y=0, like a mob of 1.5 blocks' model)."""
    (x0, y0, z0), (w, h, d) = origin, size
    x0, y0, z0 = x0 - inflate, y0 - inflate, z0 - inflate
    x1, y1, z1 = x0 + w + 2 * inflate, y0 + h + 2 * inflate, z0 + d + 2 * inflate
    u, v = uv
    tw, th = tex_size
    c, s_ = math.cos(rot_x), math.sin(rot_x)

    def world(p):
        x, y, z = p
        y, z = y * c - z * s_, y * s_ + z * c                     # the part's rotation around its pivot
        x, y, z = x + pivot[0], y + pivot[1], z + pivot[2]
        return np.array([-x / 16, 1.5 - y / 16, z / 16])           # vanilla models are drawn upside down and mirrored

    faces = {  # name: (corner, u-direction end, v-direction end, texture rect, outward normal in model space)
        "north": ((x0, y0, z0), (x1, y0, z0), (x0, y1, z0), (u + d, v + d, w, h), (0, 0, -1)),
        "south": ((x1, y0, z1), (x0, y0, z1), (x1, y1, z1), (u + 2 * d + w, v + d, w, h), (0, 0, 1)),
        "west": ((x0, y0, z1), (x0, y0, z0), (x0, y1, z1), (u, v + d, d, h), (-1, 0, 0)),
        "east": ((x1, y0, z0), (x1, y0, z1), (x1, y1, z0), (u + d + w, v + d, d, h), (1, 0, 0)),
        "down": ((x0, y0, z0), (x1, y0, z0), (x0, y0, z1), (u + d, v, w, d), (0, -1, 0)),
        "up": ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1), (u + d + w, v, w, d), (0, 1, 0)),
    }
    quads = []
    for corner, uend, vend, (fu, fv, fw, fh), n in faces.values():
        o, ue, ve = world(corner), world(uend), world(vend)
        normal = world(np.array(corner) + np.array(n)) - o
        quads.append(Quad(o, ue - o, ve - o, tex, (fu / tw, fv / th, (fu + fw) / tw, (fv + fh) / th), normal / np.linalg.norm(normal)))
    return quads


def villager_quads(layers, ears=False):
    """The vanilla villager model, drawn once per texture layer (base skin, type, profession); ears/tail = guh villager."""
    parts = [  # origin, size, uv, pivot, rot_x, inflate
        ((-4, -10, -4), (8, 10, 8), (0, 0), (0, 0, 0), 0, 0),              # head
        ((-4, -10, -4), (8, 10, 8), (32, 0), (0, 0, 0), 0, 0.51),          # hat
        ((-1, -1, -6), (2, 4, 2), (24, 0), (0, -2, 0), 0, 0),              # nose
        ((-4, 0, -3), (8, 12, 6), (16, 20), (0, 0, 0), 0, 0),              # body
        ((-4, 0, -3), (8, 20, 6), (0, 38), (0, 0, 0), 0, 0.5),             # robe
        ((-4, 2, -2), (8, 4, 4), (40, 38), (0, 3, -1), -0.75, 0),          # folded arms
        ((-8, -2, -2), (4, 8, 4), (44, 22), (0, 3, -1), -0.75, 0),
        ((4, -2, -2), (4, 8, 4), (44, 22), (0, 3, -1), -0.75, 0),
        ((-2, 0, -2), (4, 12, 4), (0, 22), (-2, 12, 0), 0, 0),             # legs
        ((-2, 0, -2), (4, 12, 4), (0, 22), (2, 12, 0), 0, 0),
    ]
    quads = []
    for i, tex in enumerate(layers):
        for origin, size, uv, pivot, rot, inflate in parts:
            quads += java_box_quads(tex, (64, 64), origin, size, uv, pivot, rot, inflate + 0.06 * i)
    if ears:
        f = "guhs:entity/villager/guh_features"
        for origin, size, uv in (((-7.5, -13.5, -1), (4, 4, 1), (0, 0)), ((3.5, -13.5, -1), (4, 4, 1), (0, 0)),
                                 ((-7, -14, -0.9), (3, 1, 0.8), (0, 6)), ((4, -14, -0.9), (3, 1, 0.8), (0, 6))):
            quads += java_box_quads(f, (32, 32), origin, size, uv)
        quads += java_box_quads(f, (32, 32), (-0.5, 0, 0), (1, 1, 7), (0, 10), (0, 10.5, 2.5), 0.55)
    return quads


def _resolve(textures, ref):
    while ref.startswith("#"):
        ref = textures.get(ref[1:], "minecraft:missingno")
    return ref


def _load_model(ref):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    if ns == "guhs":
        return json.load(open(os.path.join(ASSETS, "models", path + ".json")))
    with _jar.open(f"assets/minecraft/models/{path}.json") as f:
        return json.load(f)


def model_quads(ref, extra_textures=None, offset=(0, 0, 0), tint=None):
    """Quads for a Java block/item model (with parents, element rotations and texture variables)."""
    model = _load_model(ref)
    textures, elements = {}, None
    chain = [model]
    while "parent" in chain[-1] and not chain[-1]["parent"].startswith("builtin"):
        chain.append(_load_model(chain[-1]["parent"]))
    for m in reversed(chain):
        textures.update(m.get("textures", {}))
        if "elements" in m:
            elements = m["elements"]
    textures.update(extra_textures or {})
    # (26.1: a texture may be written as {"sprite": ..., "force_translucent": true})
    textures = {k: (v.get("sprite", "minecraft:missingno") if isinstance(v, dict) else v) for k, v in textures.items()}
    quads = []
    for el in elements or []:
        f, t = np.array(el["from"], float), np.array(el["to"], float)
        x0, y0, z0 = f / 16
        x1, y1, z1 = t / 16
        w, h, d = x1 - x0, y1 - y0, z1 - z0
        defaults = {
            "north": ((16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]), (x1, y1, z0), (-w, 0, 0), (0, -h, 0), (0, 0, -1)),
            "south": ((f[0], 16 - t[1], t[0], 16 - f[1]), (x0, y1, z1), (w, 0, 0), (0, -h, 0), (0, 0, 1)),
            "east": ((16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]), (x1, y1, z1), (0, 0, -d), (0, -h, 0), (1, 0, 0)),
            "west": ((f[2], 16 - t[1], t[2], 16 - f[1]), (x0, y1, z0), (0, 0, d), (0, -h, 0), (-1, 0, 0)),
            "up": ((f[0], f[2], t[0], t[2]), (x0, y1, z0), (w, 0, 0), (0, 0, d), (0, 1, 0)),
            "down": ((f[0], 16 - t[2], t[0], 16 - f[2]), (x0, y0, z1), (w, 0, 0), (0, 0, -d), (0, -1, 0)),
        }
        rot = el.get("rotation")
        for name, face in el.get("faces", {}).items():
            uv_default, o, u, v, n = defaults[name]
            uv = face.get("uv", uv_default)
            tex = _resolve(textures, face["texture"])
            o, u, v, n = np.array(o), np.array(u), np.array(v), np.array(n, float)
            if rot:
                ang = math.radians(rot["angle"])
                c, s = math.cos(ang), math.sin(ang)
                axis = rot["axis"]
                M = {"x": np.array([[1, 0, 0], [0, c, -s], [0, s, c]]),
                     "y": np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]]),
                     "z": np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])}[axis]
                origin = np.array(rot["origin"], float) / 16
                o = M @ (o - origin) + origin
                u, v, n = M @ u, M @ v, M @ n
            quads.append(Quad(o + offset, u, v, tex, tuple(x / 16 for x in uv), n,
                              tint if face.get("tintindex") is not None else None))
    return quads


def cube_quads(tex_ref, overlay=None):
    quads = model_quads("minecraft:block/cube_all", {"all": tex_ref})
    if overlay:
        for q in model_quads("minecraft:block/cube_all", {"all": overlay}):
            q.origin = q.origin + q.normal * 0.002  # a hair in front of the base texture
            quads.append(q)
    return quads


def item_icon(ref, size=64):
    return texture(ref).resize((size, size), Image.NEAREST)


# ---------------------------------------------------------------------------------------------------------------------
# structures: isometric voxel view (block colours = average texture colour), guhs drawn in
# ---------------------------------------------------------------------------------------------------------------------
BLOCK_TEXTURES = {  # block id -> texture used for its colour
    "guhs:block_of_kaasknabbels": "guhs:block/block_of_kaasknabbels", "guhs:guh_spawner": "guhs:block/guh_spawner",
    "guhs:kaas_saus": None, "guhs:frying_pan": "guhs:block/frying_pan_iron", "guhs:guh_wheel": "guhs:block/guh_wheel_coral",
    "guhs:guh_wheel_part": "guhs:block/guh_wheel_coral", "minecraft:chest": "block/oak_planks",
    "minecraft:spruce_log": "block/spruce_log", "minecraft:hay_block": "block/hay_block_side",
    "minecraft:birch_fence": "block/birch_planks", "minecraft:cake": "block/cake_top",
    "minecraft:potted_pink_tulip": "block/pink_tulip", "minecraft:potted_allium": "block/allium",
    "minecraft:iron_bars": "block/iron_bars", "minecraft:cobweb": "block/cobweb",
}
SPECIAL_COLOURS = {"minecraft:water": (70, 120, 220), "guhs:kaas_saus": (242, 165, 22), "minecraft:spruce_leaves": (61, 99, 61), "minecraft:oak_leaves": (72, 120, 40), "minecraft:jigsaw": None, "minecraft:air": None}
SEE_THROUGH = ("glass", "cobweb", "iron_bars", "kaas_saus")


@lru_cache(maxsize=None)
def block_colour(block):
    if block in SPECIAL_COLOURS:
        return SPECIAL_COLOURS[block]
    name = block.split(":")[1]
    if name.endswith("_carpet"):
        name = name.replace("_carpet", "_wool")
    if name.endswith("_froglight"):
        name += "_side"
    ref = BLOCK_TEXTURES.get(block, "block/" + name)
    if block.startswith("guhs:") and block not in BLOCK_TEXTURES:
        # our own blocks: the block's texture, or a likely part of it
        for suffix in ("", "_side", "_top", "_stage7", "_front"):
            base = name.replace("slee_rail_part", "slee_rail").replace("potted_", "")
            for dutch in ("_trap", "_plaat", "_muur"):      # (stairs, slabs, walls of our own stones)
                if base.endswith(dutch):
                    base = base[: -len(dutch)]
            if os.path.exists(os.path.join(ASSETS, "textures", "block", base + suffix + ".png")):
                ref = "guhs:block/" + base + suffix
                break
        else:
            for wool in ("_zitzak", "_kussen"):
                if name.endswith(wool):
                    ref = "block/" + name[:-len(wool)] + "_wool"
    try:
        a = tex_array(ref)
    except (KeyError, FileNotFoundError):
        a = None
        # vanilla shapes made of another block's texture: stairs, slabs, doors, snow block...
        base = name
        for suffix in ("_stairs", "_slab", "_wall", "_fence_gate", "_fence", "_pressure_plate", "_button", "_pane", "_door"):
            if base.endswith(suffix):
                base = base[: -len(suffix)]
        tries = [base.replace("_bed", "_wool"), base + "_log_lit", base + "_planks", base + "_top", base + "_side", base + "_door_bottom", base.replace("_block", ""),
                 base.replace("polished_blackstone_brick", "polished_blackstone_bricks")]
        for tname in tries:
            try:
                a = tex_array("block/" + tname)
                break
            except (KeyError, FileNotFoundError):
                continue
        if a is None:
            return (200, 0, 200)
    opaque = a[a[..., 3] > 100][:, :3]
    return tuple(int(v) for v in opaque.mean(0)) if len(opaque) else (200, 200, 200)


def render_structure(struct, guh_sprites, px=10, max_size=1100, cutaway=False, glass_water=False):
    """Isometric view from the front-left, with the structure's guhs/Mikas pasted in.
    cutaway: leave out the ceiling and the two front walls, to look inside a room.
    glass_water: water is see-through and only its outside faces are drawn (to look into a sea from the side)."""
    from PIL import ImageDraw
    W, H, D = struct.size
    blocks = {k: v[0] for k, v in struct.blocks.items() if v[0] not in ("minecraft:air", "minecraft:jigsaw")
              and not (cutaway and (k[1] == H - 1 or (k[1] > 0 and (k[0] == 0 or k[2] == 0))))}
    a, b, c = px, px * 0.5, px
    w = int((W + D) * a + 2 * px)
    h = int((W + D) * b + H * c + 2 * px)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    water = Image.new("RGBA", (w, h), (0, 0, 0, 0)) if glass_water else None
    wdraw = ImageDraw.Draw(water) if glass_water else None
    ox, oy = D * a + px, (W + D) * b + H * c + px

    def P(x, y, z):
        return (ox + (x - z) * a, oy - (x + z) * b - y * c)

    def solid(pos):
        name = blocks.get(pos)
        return name is not None and not any(t in name for t in SEE_THROUGH) and not (glass_water and name == "minecraft:water")

    items = []
    for (x, y, z), name in blocks.items():
        if solid((x, y + 1, z)) and solid((x - 1, y, z)) and solid((x, y, z - 1)):
            continue  # fully hidden
        items.append(((x, y, z), name))
    items.sort(key=lambda t: (-(t[0][0] + t[0][2]), t[0][1]))
    for (x, y, z), name in items:
        colour = block_colour(name)
        if colour is None:
            continue
        alpha = 120 if any(t in name for t in SEE_THROUGH) else 255
        top = [P(x, y + 1, z), P(x + 1, y + 1, z), P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)]
        left = [P(x, y, z), P(x, y + 1, z), P(x, y + 1, z + 1), P(x, y, z + 1)]
        front = [P(x, y, z), P(x + 1, y, z), P(x + 1, y + 1, z), P(x, y + 1, z)]
        if glass_water and name == "minecraft:water":
            # only the faces towards the outside (air or the edge), faintly, on the water layer (put over the picture at the end)
            for face, nb, k in ((top, (x, y + 1, z), 1.0), (left, (x - 1, y, z), 0.72), (front, (x, y, z - 1), 0.86)):
                if nb not in blocks:
                    wdraw.polygon(face, fill=tuple(int(v * k) for v in colour) + (90 if face is not top else 125,))
            continue
        draw.polygon(top, fill=colour + (alpha,))
        draw.polygon(left, fill=tuple(int(v * 0.72) for v in colour) + (alpha,))
        draw.polygon(front, fill=tuple(int(v * 0.86) for v in colour) + (alpha,))
        if glass_water:
            for face in (top, left, front):          # (a block in front of the water hides it)
                wdraw.polygon(face, fill=(0, 0, 0, 0))
    if glass_water:
        img.alpha_composite(water)
    for x, y, z, nbt in sorted(struct.entities, key=lambda e: -(e[0] + e[2])):
        kind = nbt["id"].split(":")[1]
        sprite = guh_sprites.get(kind)
        if sprite is None:
            continue
        scale = 1.0
        for att in nbt.get("attributes", []):
            if att["id"].endswith("scale"):
                scale = float(att["base"])
        size = max(12, int(sprite["blocks"] * scale * px * 1.45))
        spr = sprite["img"].resize((size, size), Image.LANCZOS)
        sx, sy = P(x, y, z)
        img.alpha_composite(spr, (int(sx - size / 2), int(sy - size * sprite["foot"])))
    img = img.crop(img.getbbox())
    if max(img.size) > max_size:
        img.thumbnail((max_size, max_size), Image.LANCZOS)
    return img


# ---------------------------------------------------------------------------------------------------------------------
def main(out):
    os.makedirs(out, exist_ok=True)
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    # mobs
    ARMOR = tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    guh = geo_quads(geo("guh"), "guhs:entity/guh", hide=("saddle",) + ARMOR)
    save(render(guh, 35, -22, 560), "guh")
    save(render(guh, 0, -6, 560), "guh_front")
    save(render(geo_quads(geo("guh"), "guhs:entity/guh", hide=ARMOR), 145, -28, 560), "guh_saddle")
    for tier in ("iron", "diamond", "netherite"):
        others = tuple(a for a in ARMOR if not a.startswith(f"armor_{tier}"))
        save(render(geo_quads(geo("guh"), "guhs:entity/guh", hide=("saddle",) + others), 35, -22, 360), f"guh_armor_{tier}")
    save(render(geo_quads(geo("mika"), "guhs:entity/mika"), 30, -18, 560), "mika")
    # guh variants (brococolief is a secret: not rendered)
    VARIANTS = {"mint": (), "choco": (), "snow": (), "golden": (), "brontosaurus": ("neck",)}
    for name, bones in VARIANTS.items():
        lift = ("head", (0, 22, -4.5)) if name == "brontosaurus" else None
        quads = geo_quads(geo("guh"), f"guhs:entity/guh_{name}", hide=("saddle",) + ARMOR, show_only_variant_bones=bones, lift=lift)
        save(render(quads, 35, -20, 360), f"guh_variant_{name}")
    # guhs wearing clothes (each piece drawn with its own texture, like in the game)
    bone_names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    OUTFITS = {"sweater": ["striped_sweater"], "rain": ["raincoat", "rain_hat"], "party": ["party_hat", "red_bowtie"],
               "chef": ["chef_jacket", "chef_hat", "black_bowtie"], "onesie": ["pink_onesie"]}
    PIECE_BONES = {"pink_onesie": ("outfit_suit",), "striped_sweater": ("outfit_suit",), "raincoat": ("outfit_suit",),
                   "chef_jacket": ("outfit_suit",), "rain_hat": ("outfit_rain_hat",), "party_hat": ("outfit_party",),
                   "chef_hat": ("outfit_chef_hat",), "red_bowtie": ("outfit_bowtie",), "black_bowtie": ("outfit_bowtie",)}
    for name, pieces in OUTFITS.items():
        quads = geo_quads(geo("guh"), "guhs:entity/guh", hide=("saddle",) + ARMOR)
        for piece in pieces:
            bones = PIECE_BONES[piece]
            others = tuple(b for b in bone_names if not b.startswith(bones))
            quads += geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
        save(render(quads, 35, -20, 360), f"guh_outfit_{name}")
    # guh villagers (a vanilla villager with the guh type texture, ears and tail), one per profession
    for prof in ("none", "vads_temmer", "guh_kleermaker", "vadssmid", "hamsterbouwer", "mika_jager"):
        layers = ["minecraft:entity/villager/villager", "guhs:entity/villager/type/guh"] +                  ([f"guhs:entity/villager/profession/{prof}"] if prof != "none" else [])
        save(render(villager_quads(layers, ears=True), 25, -10, 360), f"villager_{prof}")
    sitting = geo_quads(geo("guh_sitting"), "guhs:entity/guh_sitting")
    save(render(sitting, 28, -12, 560), "guh_sitting")

    # blocks
    for name, base in (("kaasknabbel_stone", "block/stone"), ("kaasknabbel_deepslate", "block/deepslate"),
                       ("kaasknabbel_dirt", "block/dirt"), ("kaasknabbel_cobblestone", "block/cobblestone")):
        save(render(cube_quads(base, "guhs:block/kaasknabbel_overlay"), 45, -30, 256), name)
    save(render(cube_quads("guhs:block/block_of_kaasknabbels"), 45, -30, 256), "block_of_kaasknabbels")
    save(render(cube_quads("guhs:block/guh_spawner"), 45, -30, 256), "guh_spawner")
    save(render(cube_quads("block/pink_concrete"), 45, -30, 128), "pink_concrete")
    save(render(cube_quads("guhs:block/compressed_super_vahoege_vads"), 45, -30, 256), "compressed_super_vahoege_vads")
    for job in ("knabbelbak", "naaitafel", "vadsaambeeld", "buizenbank", "mikatrofee"):
        save(render(model_quads(f"guhs:block/{job}"), 30, -30, 256), job)
    save(render(model_quads("guhs:block/frying_pan") + model_quads("guhs:block/frying_pan_oil"), 30, -40, 320), "frying_pan")
    save(render(model_quads("guhs:item/guh_wheel"), 25, -12, 420), "guh_wheel")
    portal = [Quad((1, 1, 0.5), (-1, 0, 0), (0, -1, 0), "guhs:block/guh_portal", (0, 0, 1, 1), (0, 0, -1))]
    frame = []
    for (x, y) in [(-1, i) for i in range(5)] + [(2, i) for i in range(5)] + [(0, 0), (1, 0), (0, 4), (1, 4)]:
        for q in cube_quads("guhs:block/block_of_kaasknabbels"):
            q.origin = q.origin + np.array([x, y, 0.0])
            frame.append(q)
    inside = []
    for x in (0, 1):
        for y in (1, 2, 3):
            inside.append(Quad((x + 1, y + 1, 0.5), (-1, 0, 0), (0, -1, 0), "guhs:block/guh_portal", (0, 0, 1, 1), (0, 0, -1)))
    save(render(frame + inside, 20, -10, 420), "guh_portal")
    save(render(cube_quads("guhs:block/kaas_saus_still"), 45, -30, 256), "kaas_saus")
    line = texture("block/redstone_dust_line0")
    stone = texture("block/smooth_stone")
    dot = texture("block/redstone_dust_dot")
    strip = Image.new("RGBA", (16 * 5, 16 * 2 + 4), (0, 0, 0, 0))
    for row, colour in enumerate(((122, 46, 74), (255, 92, 184))):
        tint = lambda im: Image.fromarray((np.asarray(im).astype(np.float32) * (np.array(colour + (255,)) / 255)).astype(np.uint8), "RGBA")
        line_t, dot_t = tint(line).rotate(90), tint(dot)
        for i in range(5):
            strip.alpha_composite(stone, (i * 16, row * 20))
            strip.alpha_composite(line_t, (i * 16, row * 20))
            strip.alpha_composite(dot_t, (i * 16, row * 20))
    save(strip.resize((strip.width * 6, strip.height * 6), Image.NEAREST), "guh_wire")

    # items
    for ref in ("guhs:item/kaas_knabbels", "guhs:item/gefrituurde_kaasknabbels", "guhs:item/mika_vet",
                "guhs:item/bank_guh", "guhs:item/picked_up_guh", "guhs:item/kaas_saus_bucket", "guhs:item/guh_wire",
                "guhs:item/iron_guh_armor", "guhs:item/diamond_guh_armor", "guhs:item/netherite_guh_armor",
                "item/diamond", "item/netherite_ingot", "item/netherite_upgrade_smithing_template",
                "item/iron_ingot", "item/stick", "item/redstone", "item/pink_dye", "item/saddle", "item/name_tag", "item/bucket",
                "guhs:item/vahoege_vads", "guhs:item/vahoege_vads_ingot", "guhs:item/vahoege_vads_sword",
                "guhs:item/vahoege_vads_pickaxe", "guhs:item/vahoege_vads_axe", "guhs:item/vahoege_vads_shovel",
                "guhs:item/vahoege_vads_hoe", "guhs:item/vahoege_vads_paxel", "guhs:item/vahoege_vads_shears", "guhs:item/vahoege_vads_helmet",
                "guhs:item/vahoege_vads_chestplate", "guhs:item/vahoege_vads_leggings", "guhs:item/vahoege_vads_boots",
                "guhs:item/guh_cave_compass_00", "guhs:item/challenge_compass_00", "item/compass_00",
                "guhs:item/pink_onesie", "guhs:item/striped_sweater", "guhs:item/raincoat", "guhs:item/chef_jacket",
                "guhs:item/rain_hat", "guhs:item/party_hat", "guhs:item/chef_hat", "guhs:item/red_bowtie",
                "guhs:item/black_bowtie", "guhs:item/mika_mepper", "item/string", "block/pink_wool",
                "block/composter_side", "block/yellow_stained_glass", "block/oak_planks", "block/pink_terracotta",
                "block/polished_blackstone"):
        save(item_icon(ref, 64), "icon_" + ref.split("/")[-1])

    # block renders double as recipe icons
    for name in ("block_of_kaasknabbels", "frying_pan", "guh_wheel", "pink_concrete", "compressed_super_vahoege_vads",
                 "knabbelbak", "naaitafel", "vadsaambeeld", "buizenbank", "mikatrofee"):
        Image.open(os.path.join(out, name + ".png")).save(os.path.join(out, "icon_" + name + ".png"))

    # structures (with little guh / Mika sprites in them)
    sys.path.insert(0, "tools")
    import make_structures as ms
    captured = {}
    ms.Structure.save = lambda self, name: captured.__setitem__(name, self)
    for fn in ("hamster_house", "hamster_house_medium", "hamster_house_large", "evil_mika_home", "guh_picnic",
               "cheese_fountain", "central_room", "nest_room", "pantry_room", "grand_cheese_fountain", "guh_statue",
               "dungeon_hall", "parkour_room", "spawner_room", "mika_den", "hamster_house_extra_extra_large", "guhramid"):
        getattr(ms, fn)()
    ms.guh_village("layout_a", 1)
    # the builds copied from the "Guh structures" world (skipped if that world isn't there)
    import import_world_builds as iw
    if os.path.isdir(iw.WORLD):
        iw.main()
    sprites = {
        "guh": {"img": render(guh, 45, -30, 256, margin=0.02), "blocks": 1.0, "foot": 0.85},
        "mika": {"img": render(geo_quads(geo("mika"), "guhs:entity/mika"), 45, -30, 256, margin=0.02), "blocks": 1.0, "foot": 0.85},
        "quest_guh": {"img": render(sitting, 45, -20, 256, margin=0.02), "blocks": 1.6, "foot": 0.95},
    }
    for name, struct in captured.items():
        room = name.startswith("guh_caves/") or name.startswith("challenging_guh_caves/")
        px = 12 if max(struct.size) < 40 else 8 if max(struct.size) < 100 else 5
        save(render_structure(struct, sprites, px=px, cutaway=room, max_size=1400 if px == 5 else 1100),
             "structure_" + name.split("/")[-1])
    main_v2(out)


# ---------------------------------------------------------------------------------------------------------------------
# 2.0.0 renders: new variants, clothes, NPCs, sled + rails, bees, fish, slimes, crystals, furniture, structures
# ---------------------------------------------------------------------------------------------------------------------
def offset_quads(quads, dx=0.0, dy=0.0, dz=0.0):
    for q in quads:
        q.origin = q.origin + np.array([dx, dy, dz])
    return quads


def box_quads(p0, p1, tex, uv=(0, 0, 1, 1), tint=None):
    """An axis-aligned box from p0 to p1 (blocks) with one texture region on every face."""
    (x0, y0, z0), (x1, y1, z1) = p0, p1
    w, h, d = x1 - x0, y1 - y0, z1 - z0
    faces = [((x1, y1, z0), (-w, 0, 0), (0, -h, 0), (0, 0, -1)), ((x0, y1, z1), (w, 0, 0), (0, -h, 0), (0, 0, 1)),
             ((x1, y1, z1), (0, 0, -d), (0, -h, 0), (1, 0, 0)), ((x0, y1, z0), (0, 0, d), (0, -h, 0), (-1, 0, 0)),
             ((x1, y1, z0), (-w, 0, 0), (0, 0, d), (0, 1, 0)), ((x1, y0, z1), (-w, 0, 0), (0, 0, -d), (0, -1, 0))]
    return [Quad(o, u, v, tex, uv, n, tint) for o, u, v, n in faces]


def rail_quads(pieces):
    """Sled rails along slee_track pieces, like SleeRailRenderer: candy-stripe rails on wooden sleepers."""
    import slee_track as st
    out = []
    for anchor, facing, shape, _down in pieces:
        end = st.JUMP_RAMP / st.JUMP_LENGTH if shape == "jump" else 1.0   # the jump's flight has no rails
        length = st.LENGTH[shape] * (st.JUMP_RAMP / st.LENGTH["jump"] if shape == "jump" else 1)
        n = int(math.ceil(length / 0.25))
        for i in range(n):
            (pa, ha), (pb, _) = st.world(anchor, facing, shape, end * i / n), st.world(anchor, facing, shape, end * (i + 1) / n)
            hx, hz = pb[0] - pa[0], pb[2] - pa[2]
            ln = math.hypot(hx, hz) or 1
            side = np.array([-hz / ln, 0, hx / ln])
            tex = (0, 0, 0.5, 0.5) if (i // 2) % 2 == 0 else (0.5, 0, 1, 0.5)
            for s in (-1, 1):
                a = np.array(pa) + side * 0.55 * s + np.array([0, 0.08, 0])
                b = np.array(pb) + side * 0.55 * s + np.array([0, 0.08, 0])
                u, v, wv = b - a, np.array([0, 0.12, 0]), side * 0.14
                o = a - side * 0.07
                out += [Quad(o + v, u, wv, "guhs:block/slee_rail", tex, (0, 1, 0)),
                        Quad(o + v, u, -v, "guhs:block/slee_rail", tex, tuple(-side)),
                        Quad(o + v + wv, u, -v, "guhs:block/slee_rail", tex, tuple(side))]
        sleepers = max(2, round(length / 0.5))
        for i in range(sleepers):
            p, h = st.world(anchor, facing, shape, end * (i + 0.5) / sleepers)
            hx, hz = h[0], h[2]
            ln = math.hypot(hx, hz) or 1
            fwd, side = np.array(h) / np.linalg.norm(h), np.array([-hz / ln, 0, hx / ln])
            o = np.array(p) - side * 0.8 - fwd * 0.14 + np.array([0, 0.08, 0])
            out.append(Quad(o, side * 1.6, fwd * 0.28, "guhs:block/slee_rail", (0, 0.5, 0.5, 1), (0, 1, 0)))
            out.append(Quad(o, side * 1.6, np.array([0, -0.08, 0]), "guhs:block/slee_rail", (0, 0.5, 0.5, 1), tuple(-fwd)))
    return out


def main_v2(out):
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    ARMOR = tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    hide = ("saddle",) + ARMOR
    # new variants
    save(render(geo_quads(geo("guh"), "guhs:entity/guh_rainbow_0", hide=hide), 35, -20, 360), "guh_variant_rainbow")
    save(render(geo_quads(geo("guh"), "guhs:entity/guh_starry", hide=hide), 35, -20, 360), "guh_variant_starry")
    ghost_tex = tex_array("guhs:entity/guh_ghost").copy()
    save(render(geo_quads(geo("guh"), ghost_tex, hide=hide), 35, -20, 360), "guh_variant_ghost")
    back = geo_quads(geo("guh"), "guhs:entity/guh_teckel", hide=tuple(b for b in [x["name"] for x in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
                                                                     if b not in ("leg_back_left", "leg_back_right", "tail")))
    front = geo_quads(geo("guh"), "guhs:entity/guh_teckel", hide=hide + ("leg_back_left", "leg_back_right", "tail"), show_only_variant_bones=("teckel",))
    save(render(front + offset_quads(back, dz=7 / 16), 62, -18, 360), "guh_variant_teckel")

    # clothes: themed outfits
    bone_names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    import re
    PIECES = {m[0].lower(): tuple(re.findall(r'"([a-z_]+)"', m[1])) for m in re.findall(r'^\s{4}([A-Z_]+)\(Slot\.[A-Z]+, ([^)]*)\)', src, re.M)}
    OUTFITS = {"work_fire": ["firefighter_helmet", "firefighter_jacket"], "work_police": ["police_cap", "police_uniform", "sunglasses"],
               "work_doctor": ["doctor_coat", "stethoscope", "heart_glasses"], "work_builder": ["builder_helmet", "safety_vest"],
               "work_farmer": ["straw_hat", "overalls"], "holiday_christmas": ["santa_hat", "christmas_sweater", "winter_scarf"],
               "holiday_sint": ["sint_mitre"], "holiday_piet": ["piet_beret"], "holiday_halloween": ["pumpkin_head", "ghost_sheet"],
               "holiday_witch": ["witch_hat"], "holiday_kingsday": ["orange_crown", "orange_shirt"],
               "fantasy_wizard": ["wizard_hat", "wizard_robe"], "fantasy_knight": ["knight_helmet", "knight_armour"],
               "fantasy_royal": ["royal_crown", "royal_cape", "monocle"], "fantasy_pirate": ["pirate_hat", "eyepatch"],
               "backpack": ["guh_backpack", "party_hat"]}
    for name, pieces in OUTFITS.items():
        quads = geo_quads(geo("guh"), "guhs:entity/guh", hide=hide)
        for piece in pieces:
            bones = PIECES[piece]
            others = tuple(b for b in bone_names if b not in bones)
            quads += geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
        save(render(quads, 145 if name == "backpack" else 35, -20, 360), f"guh_outfit_{name}")

    # quest characters
    sitting_geo = geo("guh_sitting")
    for kind in ("moeder_vadsig", "tandarts", "maagenzym", "slee_guh"):
        save(render(geo_quads(sitting_geo, f"guhs:entity/npc_{kind}"), 28, -12, 360), f"npc_{kind}")
    save(render(geo_quads(geo("mika"), "guhs:entity/mika"), 20, -14, 360), "mika_baas")
    save(render(geo_quads(geo("mika"), "guhs:entity/nether_mika"), 30, -18, 360), "nether_mika")
    villager_prof = ["minecraft:entity/villager/villager", "guhs:entity/villager/type/guh", "guhs:entity/villager/profession/knabbelboer"]
    save(render(villager_quads(villager_prof, ears=True), 25, -10, 360), "villager_knabbelboer")

    # the sled and its rails
    import slee_track as st
    sled = geo_quads(geo("guh_slee"), "guhs:entity/guh_slee")
    team = list(sled)
    for ahead, side in ((1.55, -0.38), (1.55, 0.38), (2.55, -0.38), (2.55, 0.38)):   # like GuhSleeRenderer
        for q in geo_quads(geo("guh"), "guhs:entity/guh", hide=hide):
            q.origin, q.u, q.v = q.origin * 0.6 + np.array([side, 0.1, -ahead]), q.u * 0.6, q.v * 0.6
            team.append(q)
    save(render(team, 35, -22, 520), "guh_slee")
    for shape in ("straight", "curve_right", "slope"):
        pieces = st.build((0, 0, 0), "north", [shape])
        save(render(rail_quads(pieces), 35, -35, 360), f"rail_{shape}")
    track = st.build((0, 0, 0), "north", ["straight", "curve_right", "straight", "slope", "straight", "down", "straight"])
    sled_on = geo_quads(geo("guh_slee"), "guhs:entity/guh_slee")
    p0, h0 = st.world((0, 0, 0), "north", "straight", 0.5)
    for q in sled_on:  # the sled faces -z in the model; the first piece runs towards -z too
        q.origin = q.origin + np.array([p0[0], p0[1] + st.RIDE_HEIGHT, p0[2]])
    save(render(rail_quads(track) + sled_on, 150, -50, 700, margin=0.03), "sled_track")

    # creatures
    save(render(geo_quads(geo("guh_bee"), "guhs:entity/guh_bee"), 30, -18, 360), "guh_bee")
    save(render(geo_quads(geo("guh_vis"), "guhs:entity/guh_vis"), 30, -18, 360), "guh_vis")
    slime = java_box_quads("guhs:entity/guh_slime", (64, 32), (-3, 17, -3), (6, 6, 6), (0, 16))
    for (u, v, x) in ((32, 0, -3.25), (32, 4, 1.25)):
        slime += java_box_quads("guhs:entity/guh_slime", (64, 32), (x, 18, -3.5), (2, 2, 2), (u, v))
    slime += java_box_quads("guhs:entity/guh_slime", (64, 32), (0, 21, -3.5), (1, 1, 1), (32, 8))
    outer = java_box_quads("guhs:entity/guh_slime", (64, 32), (-4, 16, -4), (8, 8, 8), (0, 0))
    save(render(slime, 30, -18, 300), "guh_slime")  # (the see-through outer layer would hide its face here)

    # blocks
    for b in ("maagwand", "maagbodem", "tong", "tand", "verteerde_kaasknabbels", "guh_kristal_blok", "guh_kristal_lamp",
              "guh_kristalsteen"):
        save(render(cube_quads(f"guhs:block/{b}"), 45, -30, 256), b)
    for m in ("guh_kristal_cluster", "knabbelkorf_honey", "guh_stoel", "guh_tafel", "guh_bank", "guh_kast", "lampion_roze",
              "lampion_geel", "lampion_mint", "vlaggetjes", "kaasbloem", "guhoortjes", "roze_guhbloem", "knabbelroos", "roze_gras",
              "kaasknabbelplant_stage7", "zaadbak", "guh_taart", "guh_waterlelie", "roze_slijmblok", "pink_zitzak", "pink_kussen"):
        try:
            save(render(model_quads(f"guhs:block/{m}"), 30, -30, 256), m)
        except Exception as e:  # noqa: BLE001
            print("could not render", m, e)
    row = []
    for i, c in enumerate(("pink", "light_blue", "yellow", "lime", "purple", "white")):
        row += offset_quads(model_quads(f"guhs:block/{c}_zitzak"), dx=i * 1.15)
    save(render(row, 20, -25, 700), "zitzakken")
    row = []
    for i, c in enumerate(("pink", "magenta", "orange", "cyan", "red", "black")):
        row += offset_quads(model_quads(f"guhs:block/{c}_kussen"), dx=i * 1.0)
    save(render(row, 20, -35, 700), "kussens")
    frame, inside = [], []
    for (x, y) in [(-1, i) for i in range(5)] + [(2, i) for i in range(5)] + [(0, 0), (1, 0), (0, 4), (1, 4)]:
        frame += offset_quads(cube_quads("guhs:block/tand"), x, y, 0)
    for x in (0, 1):
        for y in (1, 2, 3):
            inside.append(Quad((x + 1, y + 1, 0.5), (-1, 0, 0), (0, -1, 0), "guhs:block/maag_portal", (0, 0, 1, 1), (0, 0, -1)))
    save(render(frame + inside, 20, -10, 420), "maag_portal")

    # a guh blossom tree (trunk + blossom crown, blocks as cubes)
    tree = []
    for y in range(5):
        tree += offset_quads(cube_quads("guhs:block/guhbloesem_log"), 0, y, 0)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            for dy, r in ((4, 2), (5, 3), (6, 1)):
                if abs(dx) + abs(dz) <= r and not (dx == 0 and dz == 0 and dy == 4):
                    tree += offset_quads(cube_quads("guhs:block/guhbloesem_leaves"), dx, dy, dz)
    for i, (px_, py_) in enumerate(((1.6, 2.5), (-1.4, 1.2), (0.7, 0.6))):   # tiny falling guhs
        tree.append(Quad((px_ + 0.25, py_ + 0.25, 1.8), (-0.5, 0, 0), (0, -0.5, 0), f"guhs:particle/guh_blaadje_{i}", (0, 0, 1, 1), (0, 0, -1)))
    save(render(tree, 30, -20, 420), "guhbloesem_tree")

    # the pink moon over pink stars
    rng = np.random.default_rng(7)
    sky = Image.new("RGBA", (900, 420))
    px = sky.load()
    for y in range(420):
        for x in range(900):
            t_ = y / 420
            px[x, y] = (int(20 + 40 * t_), int(10 + 20 * t_), int(45 + 50 * t_), 255)
    for _ in range(420):
        x, y = int(rng.integers(0, 900)), int(rng.integers(0, 420))
        c = [(255, 150, 210), (230, 170, 255), (255, 255, 255)][int(rng.integers(0, 3))]
        s = int(rng.integers(1, 3))
        for dx in range(s):
            for dy in range(s):
                if x + dx < 900 and y + dy < 420:
                    px[x + dx, y + dy] = c + (255,)
    moon = texture("guhs:environment/pink_moon").resize((170, 170), Image.NEAREST)
    sky.alpha_composite(moon, (620, 60))
    save(sky, "pink_sky")

    # item icons (recipes and cards)
    ICONS = ["guh_buikfluitje", "mika_spoorkompas_00", "taartkruimels_00", "verloren_guh_taart", "guh_ballon", "guh_kristal",
             "guh_kristal_verrekijker", "guhdex", "sleeglijder", "guh_belletje", "roze_lint", "sleebouwersboek", "guh_slee",
             "sleerail_recht", "sleerail_bocht", "sleerail_helling", "guh_vis", "gebakken_guh_vis", "guh_vis_bucket", "kaashoning",
             "guh_slimeball", "guh_cupcake", "macaron_roze", "macaron_mint", "macaron_citroen", "macaron_choco", "kaasfondue",
             "kaasknabbel_milkshake", "kaasknabbelzaadjes", "guh_taart_item", "maagzuur_bucket", "guh_backpack"]
    ICONS += [c for c in ("sunglasses", "heart_glasses", "monocle", "eyepatch", "firefighter_helmet", "firefighter_jacket", "police_cap",
                          "police_uniform", "doctor_coat", "stethoscope", "builder_helmet", "safety_vest", "straw_hat", "overalls",
                          "santa_hat", "christmas_sweater", "winter_scarf", "sint_mitre", "piet_beret", "witch_hat", "ghost_sheet",
                          "pumpkin_head", "orange_crown", "orange_shirt", "wizard_hat", "wizard_robe", "knight_helmet", "knight_armour",
                          "royal_crown", "royal_cape", "pirate_hat")]
    for name in ICONS:
        save(item_icon(f"guhs:item/{name}", 64), "icon_" + name)
    for ref in ("item/book", "item/spyglass", "item/glass_bottle", "item/sugar", "item/egg", "item/wheat", "item/milk_bucket",
                "item/bowl", "item/shears", "item/feather", "item/paper", "item/iron_nugget", "item/cocoa_beans", "item/yellow_dye",
                "item/lime_dye", "item/magenta_dye", "item/orange_dye", "item/lantern", "block/torch", "block/cherry_planks",
                "block/glass", "block/white_wool", "block/yellow_wool", "block/composter_side"):
        try:
            save(item_icon(ref, 64), "icon_" + ref.split("/")[-1])
        except KeyError:
            print("no vanilla texture", ref)
    chest = texture("entity/chest/normal")                      # the chest's front: lid + bottom
    icon_chest = Image.new("RGBA", (14, 15))
    icon_chest.paste(chest.crop((14, 14, 28, 19)), (0, 0))
    icon_chest.paste(chest.crop((14, 33, 28, 43)), (0, 5))
    save(icon_chest.resize((64, 64), Image.NEAREST), "icon_chest")
    for name in ("guh_kristal_blok", "guh_kristal_lamp", "knabbelkorf_honey", "guh_stoel", "guh_tafel", "guh_bank", "guh_kast",
                 "lampion_roze", "vlaggetjes", "zaadbak", "roze_slijmblok", "pink_zitzak", "pink_kussen", "guh_kristal_cluster",
                 "kaasbloem", "guhoortjes", "roze_guhbloem", "knabbelroos"):
        if os.path.exists(os.path.join(out, name + ".png")):
            Image.open(os.path.join(out, name + ".png")).save(os.path.join(out, "icon_" + name + ".png"))

    # new structures
    sys.path.insert(0, "tools")
    import make_structures as ms
    import make_v2 as mv
    captured = {}
    ms.Structure.save = lambda self, name: captured.__setitem__(name, self)
    mv.Structure.save = ms.Structure.save
    mv.heiligdom()
    mv.mika_kamp()
    mv.sleehut()
    ms.hamster_house_extra_extra_large()
    ms.guh_village("layout_a", 1)
    sitting = geo_quads(sitting_geo, "guhs:entity/guh_sitting")
    guh = geo_quads(geo("guh"), "guhs:entity/guh", hide=hide)
    sprites = {
        "guh": {"img": render(guh, 45, -30, 256, margin=0.02), "blocks": 1.0, "foot": 0.85},
        "mika": {"img": render(geo_quads(geo("mika"), "guhs:entity/mika"), 45, -30, 256, margin=0.02), "blocks": 1.0, "foot": 0.85},
        "mika_baas": {"img": render(geo_quads(geo("mika"), "guhs:entity/mika"), 45, -30, 256, margin=0.02), "blocks": 1.6, "foot": 0.85},
        "guh_npc": {"img": render(geo_quads(sitting_geo, "guhs:entity/npc_moeder_vadsig"), 45, -20, 256, margin=0.02), "blocks": 3.4, "foot": 0.95},
        "guh_slee": {"img": render(sled, 45, -30, 256, margin=0.02), "blocks": 1.4, "foot": 0.7},
        "quest_guh": {"img": render(sitting, 45, -20, 256, margin=0.02), "blocks": 1.6, "foot": 0.95},
    }
    for name, struct in captured.items():
        px_ = 12 if max(struct.size) < 40 else 8 if max(struct.size) < 100 else 5
        save(render_structure(struct, sprites, px=px_, max_size=1400 if px_ == 5 else 1100), "structure_" + name.split("/")[-1])


def main_v21(out):
    """2.1.0: the ender guh, the Kermis-guh, the kermis outfit, the coaster pieces, the guh kermis, the new guh tree."""
    import re
    import slee_track as st
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    ARMOR = tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    hide = ("saddle",) + ARMOR
    ender = geo_quads(geo("guh"), "guhs:entity/guh_ender", hide=hide, show_only_variant_bones=("ender",))
    save(render(ender, 35, -20, 360), "guh_variant_ender")
    save(render(ender, 160, -35, 360), "guh_variant_ender_back")
    save(render(geo_quads(geo("guh_sitting"), "guhs:entity/npc_kermis_guh"), 28, -12, 360), "npc_kermis_guh")
    # the kermis outfit on a guh
    bone_names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    PIECES = {m[0].lower(): tuple(re.findall(r'"([a-z_]+)"', m[1])) for m in re.findall(r'^\s{4}([A-Z_]+)\(Slot\.[A-Z]+, ([^)]*)\)', src, re.M)}
    quads = geo_quads(geo("guh"), "guhs:entity/guh", hide=hide)
    for piece in ("kermis_hoed", "kermis_jasje", "kermis_strik"):
        bones = PIECES[piece]
        others = tuple(b for b in bone_names if b not in bones)
        quads += geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
    save(render(quads, 35, -20, 360), "guh_outfit_kermis")
    # the coaster pieces
    for shape in ("drop", "spiral_right", "jump"):
        pieces = st.build((0, 0, 0), "north", [shape] + (["straight"] if shape == "jump" else []))
        save(render(rail_quads(pieces), 35, -30, 360), f"rail_{shape}")
    # the sled's dashboard up close
    save(render(geo_quads(geo("guh_slee"), "guhs:entity/guh_slee"), 160, -35, 360), "guh_slee_knopjes")
    # icons
    for name in ("sleerail_drop", "sleerail_kurkentrekker", "sleerail_schans", "kermisbon", "kermiskompas_00", "kermis_hoed",
                 "kermis_jasje", "kermis_strik"):
        save(item_icon(f"guhs:item/{name}", 64), "icon_" + name)
    # the new guh blossom tree trunk
    tree = []
    for y in range(5):
        tree += offset_quads(cube_quads("guhs:block/guhbloesem_log_gezicht" if y == 1 else "guhs:block/guhbloesem_log"), 0, y, 0)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            for dy, r in ((4, 2), (5, 3), (6, 1)):
                if abs(dx) + abs(dz) <= r and not (dx == 0 and dz == 0 and dy == 4):
                    tree += offset_quads(cube_quads("guhs:block/guhbloesem_leaves"), dx, dy, dz)
    save(render(tree, 30, -20, 420), "guhbloesem_tree")
    # the guh kermis from above
    sys.path.insert(0, "tools")
    import make_structures as ms
    import make_v2 as mv
    captured = {}
    ms.Structure.save = lambda self, name: captured.__setitem__(name, self)
    mv.Structure.save = ms.Structure.save
    mv.kermis_structure(st)
    sitting_geo = geo("guh_sitting")
    sled = geo_quads(geo("guh_slee"), "guhs:entity/guh_slee")
    sprites = {
        "guh": {"img": render(geo_quads(geo("guh"), "guhs:entity/guh", hide=hide), 45, -30, 256, margin=0.02), "blocks": 1.0, "foot": 0.85},
        "guh_npc": {"img": render(geo_quads(sitting_geo, "guhs:entity/npc_kermis_guh"), 45, -20, 256, margin=0.02), "blocks": 1.4, "foot": 0.95},
        "guh_slee": {"img": render(sled, 45, -30, 256, margin=0.02), "blocks": 1.4, "foot": 0.7},
    }
    kermis = captured["guh_kermis"]
    # rails as a thin pink-and-white line along the track instead of their (invisible) blocks
    for pos in [k for k, v in kermis.blocks.items() if v[0].startswith("guhs:slee_rail")]:
        del kermis.blocks[pos]
    S = ["straight"]
    seq = (S * 2 + ["spiral_right", "spiral_right"] + S * 2 + ["curve_right", "down_drop"] + S + ["jump"] + S + ["curve_right"]
           + S + ["drop"] + S + ["down_drop", "curve_right"] + S * 2 + ["slope", "straight", "down"] + S + ["curve_right"])
    track = st.build((5, 2, 23), "north", seq)
    n = 0
    for anchor, facing, shape, _down in track:
        end = st.JUMP_RAMP / st.JUMP_LENGTH if shape == "jump" else 1.0
        for i in range(41):
            (x, y, z), _ = st.world(anchor, facing, shape, end * i / 40)
            cell = (math.floor(x - 0.01), math.floor(y + 0.01), math.floor(z - 0.01))
            if cell not in kermis.blocks or kermis.blocks[cell][0] == "minecraft:air":
                n += 1
                kermis.blocks[cell] = ("minecraft:pink_concrete" if (n // 3) % 2 else "minecraft:white_concrete", {}, None)
    SPECIAL_COLOURS.setdefault("minecraft:grass_block", (112, 168, 72))
    save(render_structure(kermis, sprites, px=12, max_size=1100), "structure_guh_kermis")
    # the kermis coaster in 3D (rails only, with a sled at the station)
    S = ["straight"]
    seq = (S * 2 + ["spiral_right", "spiral_right"] + S * 2 + ["curve_right", "down_drop"] + S + ["jump"] + S + ["curve_right"]
           + S + ["drop"] + S + ["down_drop", "curve_right"] + S * 2 + ["slope", "straight", "down"] + S + ["curve_right"])
    track = st.build((5, 2, 23), "north", seq)
    p0, _ = st.world((5, 2, 23), "north", "straight", 0.5)
    sled_on = geo_quads(geo("guh_slee"), "guhs:entity/guh_slee")
    for q in sled_on:
        q.origin = q.origin + np.array([p0[0], p0[1] + st.RIDE_HEIGHT, p0[2]])
    save(render(rail_quads(track) + sled_on, 215, -38, 900, margin=0.03), "kermis_coaster")


def main_v22(out):
    """2.2.0: verstopguh: the house (outside and floor 1 inside), Verstopguhtje, the detective outfit, icons."""
    import re
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    save(render(geo_quads(geo("guh_sitting"), "guhs:entity/npc_verstopguhtje"), 28, -12, 360), "npc_verstopguhtje")
    bone_names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    PIECES = {m[0].lower(): tuple(re.findall(r'"([a-z_]+)"', m[1])) for m in re.findall(r'^\s{4}([A-Z_]+)\(Slot\.[A-Z]+, ([^)]*)\)', src, re.M)}
    quads = geo_quads(geo("guh"), "guhs:entity/guh", hide=hide)
    for piece in ("detective_pet", "detective_vergrootglas", "detective_jas"):
        bones = PIECES[piece]
        others = tuple(b for b in bone_names if b not in bones)
        quads += geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
    save(render(quads, 35, -20, 360), "guh_outfit_detective")
    for name in ("verstopguhticket", "verstopkompas_00", "detective_pet", "detective_vergrootglas", "detective_jas"):
        save(item_icon(f"guhs:item/{name}", 64), "icon_" + name)
    save(render(cube_quads("guhs:block/eenrichtingsglas"), 45, -30, 256), "eenrichtingsglas")
    sys.path.insert(0, "tools")
    import make_structures as ms
    import make_v2 as mv
    captured = {}
    ms.Structure.save = lambda self, name: captured.__setitem__(name, self)
    mv.Structure.save = ms.Structure.save
    mv.verstop_structure()
    house = captured["verstopguh_huis"]
    SPECIAL_COLOURS.setdefault("minecraft:grass_block", (112, 168, 72))
    SPECIAL_COLOURS["guhs:verstopplek"] = None
    SPECIAL_COLOURS["guhs:verstopstart"] = None
    SPECIAL_COLOURS["guhs:eenrichtingsglas"] = (250, 200, 225)
    sprites = {"guh_npc": {"img": render(geo_quads(geo("guh_sitting"), "guhs:entity/npc_verstopguhtje"), 45, -20, 256, margin=0.02),
                           "blocks": 1.4, "foot": 0.95}}
    save(render_structure(house, sprites, px=8, max_size=1100), "structure_verstopguh_huis")
    SPECIAL_COLOURS["guhs:eenrichtingsglas"] = None                       # looking through the one-way glass
    save(render_structure(house, sprites, px=8, max_size=1100), "verstopguh_huis_glas")
    for pos in [k for k in house.blocks if k[1] >= 7]:
        del house.blocks[pos]
    save(render_structure(house, {}, px=8, max_size=1100), "verstopguh_huis_beneden")


def main_v23(out):
    """2.3.0: the guh castle, the Koningguh (with and without his outfit), the throne, icons."""
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    king = geo_quads(geo("guh"), "guhs:entity/guh_koning", hide=hide, show_only_variant_bones=("koning",))
    save(render(king, 35, -15, 360), "guh_variant_koning")
    dressed = list(king)
    for piece, bones in (("koning_kroon", ("outfit_grand_crown",)), ("koning_mantel", ("outfit_long_cape",)), ("koning_ketting", ("outfit_chain",))):
        others = tuple(b for b in names if not b.startswith(bones))
        dressed += geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
    save(render(dressed, 35, -15, 400), "guh_koning_pakje")
    save(render(model_quads("guhs:block/koningstroon"), 200, -20, 300), "koningstroon")
    for name in ("koningskompas_00", "koning_kroon", "koning_mantel", "koning_ketting"):
        save(item_icon(f"guhs:item/{name}", 64), "icon_" + name)
    sys.path.insert(0, "tools")
    import make_structures as ms
    import make_v2 as mv
    captured = {}
    ms.Structure.save = lambda self, name: captured.__setitem__(name, self)
    mv.Structure.save = ms.Structure.save
    mv.kasteel_structure()
    castle = captured["guh_kasteel"]
    G = mv.KASTEEL_G
    SPECIAL_COLOURS.setdefault("minecraft:grass_block", (112, 168, 72))
    SPECIAL_COLOURS.setdefault("minecraft:moss_block", (90, 140, 50))
    SPECIAL_COLOURS["minecraft:jigsaw"] = None

    def crop(x0, x1, y0, y1, z0, z1):
        """A part of the castle, seen from the garden side (south)."""
        part = ms.Structure((x1 - x0, y1 - y0, z1 - z0))
        for (x, y, z), v in castle.blocks.items():
            if x0 <= x < x1 and y0 <= y < y1 and z0 <= z < z1:
                part.blocks[(x - x0, y - y0, z1 - 1 - z)] = v
        return part
    save(render_structure(crop(0, 256, 0, 172, 0, 256), {}, px=4, max_size=1500), "structure_guh_kasteel")
    save(render_structure(crop(100, 160, G + 86, 172, 50, 92), {}, px=10, max_size=700), "guh_kasteel_hoofd")
    guard_img = render(geo_quads(geo("guh_sitting"), "guhs:entity/npc_poortwachter"), 45, -20, 256, margin=0.02)
    save(render_structure(crop(96, 162, G - 1, G + 44, 118, 176), {"guh_npc": {"img": guard_img, "blocks": 1.8, "foot": 0.95}},
                          px=8, max_size=1100), "guh_kasteel_poort")
    save(render(geo_quads(geo("guh_sitting"), "guhs:entity/npc_poortwachter"), 28, -12, 360), "npc_poortwachter")
    sprites = {"guh": {"img": render(dressed, 45, -30, 256, margin=0.02), "blocks": 2.5, "foot": 0.85}}
    save(render_structure(crop(76, 180, G - 1, G + 16, 10, 124), sprites, px=7, max_size=1200), "guh_kasteel_troonzaal")



FEATURE_STRUCTURES = {"beauty": "guh_beauty_theater", "race": "guh_racebaan", "meppen": "mika_mep_hal", "disco": "guh_disco",
                      "golf": "guh_golfbaan", "smul": "vadsig_eetfestijn", "vissen": "guhvis_vijver", "eilanden": "zwevende_eilanden",
                      "kaasmijn": "kaasmijn", "bibliotheek": "guhbibliotheek"}
FEATURE_NPCS = {"beauty": "showguh", "race": "raceguh", "meppen": "mepguh", "disco": "djguh", "golf": "golfguh", "smul": "smulguh",
                "vissen": "visguh", "kaasmijn": "mijnguh", "bibliotheek": "bibliothecaris"}


FRONT_TURNS = {"zwevende_eilanden": 0}   # how often to turn each building to see its front (default 1)


def turned(struct, turns):
    """The structure turned a quarter turn (clockwise seen from above) this many times, to look at another side."""
    import make_structures as ms
    for _ in range(turns % 4):
        W, H, D = struct.size
        t = ms.Structure((D, H, W))
        t.blocks = {(D - 1 - z, y, x): v for (x, y, z), v in struct.blocks.items()}
        t.entities = [(D - z, y, x, nbt) for x, y, z, nbt in struct.entities]
        struct = t
    return struct


def main_v24(out, base_items=None):
    """2.4.0: the 7 minigames and 3 rare places: their buildings, guh characters, outfits, the Wolkguh and item icons."""
    import re
    import types
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    bone_names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    PIECES = {m[0].lower(): tuple(re.findall(r'"([a-z_]+)"', m[1])) for m in re.findall(r'^\s{4}([A-Z_]+)\(Slot\.[A-Z]+, ([^)]*)\)', src, re.M)}
    sys.path.insert(0, "tools")
    import features
    import make_structures as ms
    import make_v2 as mv
    for kind in FEATURE_NPCS.values():
        save(render(geo_quads(geo("guh_sitting"), f"guhs:entity/npc_{kind}"), 28, -12, 360), f"npc_{kind}")
    wolk = geo_quads(geo("guh"), "guhs:entity/guh_wolk", hide=hide, show_only_variant_bones=("wolk",))
    save(render(wolk, 35, -15, 360), "guh_variant_wolk")
    for name, module in zip(features.FEATURES, features.modules()):
        pieces = getattr(module, "CLOTHES", [])
        if pieces:
            quads = geo_quads(geo("guh"), "guhs:entity/guh", hide=hide)
            for piece in pieces:
                bones = PIECES[piece]
                others = tuple(b for b in bone_names if not b.startswith(bones))
                quads += geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
            save(render(quads, 35, -20, 360), f"guh_outfit_{name}")
    # every new item gets an icon (base_items: the item textures of 2.3, to leave out)
    item_dir = os.path.join(ASSETS, "textures", "item")
    for f in sorted(os.listdir(item_dir)):
        if f.endswith(".png") and (base_items is None or f not in base_items) and not re.search(r"_\d\d\.png$", f):
            save(item_icon(f"guhs:item/{f[:-4]}", 64), "icon_" + f[:-4])
    # the buildings: every module's build(h) with Structure.save captured
    captured = {}
    ms.Structure.save = lambda self, name: captured.__setitem__(name, self)
    mv.Structure.save = ms.Structure.save
    h = types.SimpleNamespace(**vars(mv))
    for module in features.modules():
        module.build(h)
    SPECIAL_COLOURS.setdefault("minecraft:grass_block", (112, 168, 72))
    SPECIAL_COLOURS.setdefault("minecraft:moss_block", (90, 140, 50))
    SPECIAL_COLOURS["minecraft:jigsaw"] = None
    SPECIAL_COLOURS["minecraft:structure_void"] = None
    SPECIAL_COLOURS["minecraft:barrier"] = None
    SPECIAL_COLOURS["minecraft:light"] = None
    for name, sid in FEATURE_STRUCTURES.items():
        st = captured.get(sid)
        if st is None:
            print("no structure captured for", sid)
            continue
        kind = FEATURE_NPCS.get(name)
        sprites = {"guh_npc": {"img": render(geo_quads(geo("guh_sitting"), f"guhs:entity/npc_{kind}"), 45, -20, 256, margin=0.02),
                               "blocks": 1.4, "foot": 0.95}} if kind else {}
        W, H, D = st.size
        px = max(3, min(8, int(1500 / (W + D))))
        front = turned(st, FRONT_TURNS.get(sid, 1))
        if sid == "kaasmijn":
            # above ground: the mine head; below: the tunnels with the rock left out
            counts = collections.Counter(y for (x, y, z), v in st.blocks.items() if v[0] == "minecraft:grass_block")
            ground = counts.most_common(1)[0][0] if counts else 0
            top = ms.Structure(front.size)
            top.blocks = {k: v for k, v in front.blocks.items() if k[1] >= ground - 1}
            top.entities = [e for e in front.entities if e[1] >= ground - 1]
            save(render_structure(top, sprites, px=px, max_size=1500), f"structure_{sid}")
            rock = ("stone", "deepslate", "andesite", "granite", "diorite", "tuff", "dirt", "gravel", "calcite", "cobbl", "grass_block")
            tunnels = ms.Structure(front.size)
            tunnels.blocks = {k: v for k, v in front.blocks.items() if k[1] < ground - 1 and not any(r in v[0] for r in rock)}
            save(render_structure(tunnels, {}, px=px, max_size=1500), "kaasmijn_gangen")
        else:
            save(render_structure(front, sprites, px=px, max_size=1500), f"structure_{sid}")
    main_v26(out, captured)


def main_v26(out, captured):
    """2.6.0: the Guheinde: the Knabbelkelder, the Knabbelberg, the Mika-vesting with the vetschip, Opper-Mika, his starved
    Enderguh, the magere guh and the Vahoege Enderguh (the item icons come from main_v24's loop)."""
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    mager = render(geo_quads(geo("guh"), "guhs:entity/guh_mager", hide=hide), 35, -20, 360)
    save(mager, "guh_variant_mager")
    save(render(geo_quads(geo("guh"), "guhs:entity/guh_vahoege_ender", hide=hide, show_only_variant_bones=("ender",)), 35, -20, 360),
         "guh_variant_vahoege_ender")
    save(render(geo_quads(geo("guh"), "guhs:entity/guh_hongerig_0", hide=hide, show_only_variant_bones=("ender",)), 35, -20, 360),
         "hongerige_enderguh")
    save(render(geo_quads(geo("guh"), "guhs:entity/guh_hongerig_8", hide=hide, show_only_variant_bones=("ender",)), 35, -20, 360),
         "hongerige_enderguh_vahoeg")
    opper = geo_quads(geo("mika"), "guhs:entity/opper_mika")
    save(render(opper, 30, -18, 360), "opper_mika")
    mika = render(geo_quads(geo("mika"), "guhs:entity/mika"), 45, -30, 256, margin=0.02)
    sprites = {"guh": {"img": render(geo_quads(geo("guh"), "guhs:entity/guh_mager", hide=hide), 45, -30, 256, margin=0.02), "blocks": 1.0, "foot": 0.85},
               "mika": {"img": mika, "blocks": 1.0, "foot": 0.85}}
    # a few block icons (flat), and a proper crown icon (its item texture is only the swatch of the 3D model)
    for block in ("kaaskorst", "kaaskorst_stenen", "mika_steen", "knabbelportaalframe_top", "enderguh_ei_0", "opper_mikatrofee_front"):
        save(item_icon(f"guhs:block/{block}", 64), f"icon_{block}")
    crown = Image.new("RGBA", (16, 16))
    gold, light, gem = (250, 200, 60, 255), (255, 236, 150, 255), (255, 110, 180, 255)
    for x in range(2, 14):
        for y in range(8, 13):
            crown.putpixel((x, y), gold if y < 12 else (200, 150, 30, 255))
    for x0 in (2, 6, 10):
        for dy in range(4):
            for x in range(x0 + dy // 2, x0 + 4 - dy // 2):
                crown.putpixel((x, 7 - dy), light if dy == 3 else gold)
    crown.putpixel((7, 10), gem)
    crown.putpixel((8, 10), gem)
    save(crown.resize((64, 64), Image.NEAREST), "icon_knabbelkroon")
    for sid, turns, px, cut in (("knabbelkelder", 1, 7, True), ("guheinde_knabbelberg", 1, 9, False), ("mika_vesting_schip", 1, 6, False)):
        st = captured.get(sid)
        if st is None:
            print("no structure captured for", sid)
            continue
        if cut:
            # underground: a cross-section just above the main floor, so you look into the rooms
            import make_structures as ms
            sliced = ms.Structure(st.size)
            sliced.blocks = {k: v for k, v in st.blocks.items() if k[1] <= 14}
            sliced.entities = [e for e in st.entities if e[1] <= 14]
            st = sliced
        save(render_structure(turned(st, turns), sprites, px=px, max_size=1500), f"structure_{sid}")
    main_v27(out, captured)


def first_model(block):
    """The block model of a guhs block's first blockstate variant (or multipart part)."""
    bs = json.load(open(os.path.join(ASSETS, "blockstates", block + ".json")))
    if "variants" in bs:
        v = next(iter(bs["variants"].values()))
    else:
        v = bs["multipart"][0]["apply"]
    return (v[0] if isinstance(v, list) else v)["model"]


def rookguh_quads():
    """The Rookguh (2.8, a ghast with a guh face) posed like in its float animation: the tentacles trail back a bit,
    each at its own angle; the cheeks (they come when it has eaten) hidden."""
    import tempfile
    g = json.load(open(os.path.join(ASSETS, "geckolib", "models", "entity", "rookguh.geo.json")))
    for b in g["minecraft:geometry"][0]["bones"]:
        if b["name"].startswith("tentacle_"):
            b["rotation"] = [round(-20 + 11 * math.sin(int(b["name"][9:])), 2), 0, 0]
    with tempfile.TemporaryDirectory() as tmp:
        path = os.path.join(tmp, "rookguh.geo.json")
        with open(path, "w") as f:
            json.dump(g, f)
        return geo_quads(path, "guhs:entity/rookguh", hide=("cheeks",))


def main_v27(out, captured=None):
    """2.7.0: De Guhbarbecuether (dimension, portal, Grillguh, Spiesburcht, Mika-grillpaleis, brewing, baken, boss), the
    Gatenkaasgrotten (Voorraadkelder, Vadswaker), the Kaasmoeras (heksenhut, kikkerguhs, kaasmotten) and the Vadswoud
    (boomhutdorp, Boswachterguh, Knabbelplukker). Item icons come from main_v24's loop; captured = main_v24's structures."""
    import re
    import types
    import make_structures as ms
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    bone_names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    PIECES = {m[0].lower(): tuple(re.findall(r'"([a-z_]+)"', m[1])) for m in re.findall(r'^\s{4}([A-Z_]+)\(Slot\.[A-Z]+, ([^)]*)\)', src, re.M)}

    # --- creatures (GeckoLib models) ---
    mobs = {"vonk_mika": "vonk_mika", "knekel_mika": "knekel_mika", "aangebrande_mika": "aangebrande_mika",
            "vadswaker": "vadswaker", "kaasmot": "kaasmot", "moerasheks_mika": "moerasheks_mika"}
    for name, tex in mobs.items():
        save(render(geo_quads(geo(name), f"guhs:entity/{tex}"), 30, -18, 400), name)
    # 2.8: the Rookguh is a little ghast with a guh face (tentacles trailing like in its float animation, still skinny)
    save(render(rookguh_quads(), 30, -12, 400), "rookguh")
    # 2.8: the Reisguh as a guh-conductor (his own model: cap and whistle)
    save(render(geo_quads(geo("guh_npc_reisguh"), "guhs:entity/npc_reisguh"), 28, -12, 360), "npc_reisguh")
    frogs = []
    for i, kleur in enumerate(("roze", "mint", "geel")):
        save(render(geo_quads(geo("kikkerguh"), f"guhs:entity/kikkerguh_{kleur}"), 30, -18, 360), f"kikkerguh_{kleur}")
        frogs += offset_quads(geo_quads(geo("kikkerguh"), f"guhs:entity/kikkerguh_{kleur}"), dx=i * 1.0)
    save(render(frogs, 20, -18, 600), "kikkerguhs")
    # guh variants and NPCs
    save(render(geo_quads(geo("guh"), "guhs:entity/guh_kaasmoerasguh", hide=hide), 35, -20, 360), "guh_variant_kaasmoerasguh")
    save(render(geo_quads(geo("guh"), "guhs:entity/guh_asguh", hide=hide, show_only_variant_bones=("asguh",)), 35, -20, 360), "guh_variant_asguh")
    for kind in ("grillguh", "boswachterguh", "knabbelplukker"):
        save(render(geo_quads(geo("guh_sitting"), f"guhs:entity/npc_{kind}"), 28, -12, 360), f"npc_{kind}")
    # guh outfits
    for name, pieces in (("grill", ["grill_koksmuts", "grill_schort", "grill_halsdoek"]), ("boswachter", ["boswachtershoed", "boswachtersjas"]),
                         ("pluk", ["plukmuts", "plukmandje"])):
        quads = geo_quads(geo("guh"), "guhs:entity/guh", hide=hide)
        for piece in pieces:
            bones = PIECES[piece]
            others = tuple(b for b in bone_names if not b.startswith(bones))
            quads += geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
        save(render(quads, 145 if name == "pluk" else 35, -20, 360), f"guh_outfit_{name}")

    # --- blocks: 3D renders (from their block models) that double as recipe icons ---
    BLOCKS = ["houtskoolsteen", "houtskoolsteen_stenen", "gebarsten_houtskoolsteen_stenen", "gebeitelde_houtskoolsteen_stenen",
              "roosterijzer", "roosterijzer_pilaar", "gepolijst_roosterijzer", "gloeikool", "grillkool", "as_blok", "as_aarde",
              "pindasaus_nylium", "mosterd_nylium", "sate_stam", "worst_stam", "sate_vlees", "mosterd_blok", "uienlicht", "rookgat",
              "houtskoolsteen_kaasknabbelerts", "verkoold_guhbot", "gatenkaas", "gatenkaas_stenen", "belegen_kaas_stenen",
              "belegen_kaas_tegels", "kaasmos", "kaaskorrelerts", "knabbelsensor", "knabbelschreeuwer", "borrelende_kaassaus",
              "kaasmodder", "modderig_kaasgras", "vadshout_stam", "vadshout_planken", "vadshout_bladeren", "vadsmos",
              "vadshout_gezicht", "guhnestje", "guhbrouwketel", "knabbelbaken", "verkoolde_mikakop", "roosterijzer_tralies",
              "vadshout_hek", "vadshout_deur", "kaasriet", "moerasgras", "knabbelbessenstruik", "sate_zwammetje", "worst_zwammetje",
              "pindascheutjes", "smeulkooltjes", "vadshout_zaailing", "kaasmos_tapijt", "houtskoolsteen_stenen_plaat",
              "houtskoolsteen_stenen_trap", "houtskoolsteen_stenen_muur", "houtskoolsteen_stenen_hek", "vadshout_trap", "vadshout_plaat",
              "vadshout_luik", "vadshout_poort", "vadshout_gestript", "gatenkaas_stenen_trap", "belegen_kaas_plaat", "pindasausplasje",
              "mosterdscheutjes", "vadstouw"]
    MODELS = {"knabbelbessenstruik": "guhs:block/knabbelbessenstruik_3"}
    FLAT = ("roosterijzer_tralies", "vadshout_deur", "vadstouw", "mosterdscheutjes", "vadshout_zaailing", "kaasriet", "moerasgras", "pindascheutjes", "smeulkooltjes",
            "sate_zwammetje", "worst_zwammetje")   # thin blocks: their icon is the flat item texture
    for b in BLOCKS:
        try:
            img = render(model_quads(MODELS.get(b) or first_model(b)), 30, -30, 256)
            save(img, b)
            if b not in FLAT:
                img.save(os.path.join(out, "icon_" + b + ".png"))
        except Exception as e:  # noqa: BLE001
            print("could not render", b, e)
    row = []
    for i, kleur in enumerate(("roze", "mint", "geel")):
        row += offset_quads(model_quads(f"guhs:block/motknabbel_{kleur}"), dx=i * 1.15)
    save(render(row, 20, -25, 600), "motknabbels")
    save(render(model_quads("guhs:block/motknabbel_roze"), 30, -30, 256), "icon_motknabbel")
    row = []
    for i in range(4):
        row += offset_quads(model_quads(f"guhs:block/vadshout_gezicht_{i}"), dx=i * 1.15)
    save(render(row, 10, -12, 700), "vadshout_gezichtjes")
    for m in ("guhbrouwketel_vahoegheid_3", "guhbrouwketel_aan"):
        try:
            save(render(model_quads(f"guhs:block/{m}"), 30, -35, 320), m)
        except Exception as e:  # noqa: BLE001
            print("could not render", m, e)
    # the grillkool portal (4x5 frame)
    frame, inside = [], []
    for (x, y) in [(-1, i) for i in range(5)] + [(2, i) for i in range(5)] + [(0, 0), (1, 0), (0, 4), (1, 4)]:
        frame += offset_quads(cube_quads("guhs:block/grillkool"), x, y, 0)
    for x in (0, 1):
        for y in (1, 2, 3):
            inside.append(Quad((x + 1, y + 1, 0.5), (-1, 0, 0), (0, -1, 0), "guhs:block/barbecuether_portaal", (0, 0, 1, 1), (0, 0, -1)))
    save(render(frame + inside, 20, -10, 420), "barbecuether_portaal")
    # the Aangebrande Mika's T of ash with three charred Mika heads
    t_shape = []
    for (x, y) in ((0, 0), (0, 1), (-1, 1), (1, 1)):
        t_shape += offset_quads(model_quads(first_model("as_blok")), x, y, 0)
    for x in (-1, 0, 1):
        t_shape += offset_quads(model_quads(first_model("verkoolde_mikakop")), x, 2, 0)
    save(render(t_shape, 25, -15, 420), "aangebrande_mika_t")
    for b in ("roosterijzer_tralies", "vadshout_zaailing", "kaasriet_top", "moerasgras", "pindascheutjes", "smeulkooltjes",
              "sate_zwammetje", "worst_zwammetje"):
        if not os.path.exists(os.path.join(ASSETS, "textures", "item", b + ".png")):
            save(item_icon(f"guhs:block/{b}", 64), "icon_" + b.replace("_top", ""))
    # flat icons for recipe grids (vanilla ingredients)
    for ref in ("item/charcoal", "item/flint", "item/coal", "item/fermented_spider_eye", "block/mud", "block/moss_block",
                "item/bone_meal", "item/glass_bottle", "item/sugar", "item/stick", "item/string", "block/glass", "item/iron_ingot",
                "item/yellow_dye", "block/pink_wool", "item/bucket"):
        try:
            save(item_icon(ref, 64), "icon_" + ref.split("/")[-1])
        except KeyError:
            print("no vanilla texture", ref)

    # --- the buildings ---
    if captured is None:
        import features
        import make_v2 as mv
        captured = {}
        ms.Structure.save = lambda self, name: captured.__setitem__(name, self)
        mv.Structure.save = ms.Structure.save
        h = types.SimpleNamespace(**vars(mv))
        for module in features.modules():
            module.build(h)
    for k in ("minecraft:jigsaw", "minecraft:structure_void", "minecraft:barrier", "minecraft:light"):
        SPECIAL_COLOURS[k] = None
    SPECIAL_COLOURS.setdefault("minecraft:grass_block", (112, 168, 72))
    SPECIAL_COLOURS.setdefault("minecraft:moss_block", (90, 140, 50))
    BLOCK_TEXTURES.update({f"guhs:vadshout_{k}": "guhs:block/vadshout_planken" for k in ("hek", "plaat", "trap", "poort")})
    BLOCK_TEXTURES.update({"guhs:kaas_stalactiet": "guhs:block/kaas_stalactiet_down_middle", "guhs:belegen_kaas_plaat": "guhs:block/belegen_kaas_stenen",
                           "guhs:belegen_kaas_muur": "guhs:block/belegen_kaas_stenen", "guhs:belegen_kaas_trap": "guhs:block/belegen_kaas_stenen",
                           "guhs:houtskoolsteen_stenen_hek": "guhs:block/houtskoolsteen_stenen", "guhs:kaasmos_tapijt": "guhs:block/kaasmos",
                           "guhs:knabbelbessenstruik": "guhs:block/knabbelbessenstruik_3", "guhs:vadshout_gezicht": "guhs:block/vadshout_gezicht_0",
                           "minecraft:moss_carpet": "block/moss_block", "guhs:guhnestje": "guhs:block/guhnestje_rand",
                           "guhs:guh_tafel": "block/cherry_planks", "guhs:guh_bank": "block/pink_wool", "guhs:motknabbel": "guhs:block/motknabbel_roze_side",
                           "minecraft:water_cauldron": "block/cauldron_side", "minecraft:pink_banner": "block/pink_wool"})
    for wood in ("dark_oak", "crimson", "mangrove", "spruce", "oak", "birch", "cherry"):
        BLOCK_TEXTURES[f"minecraft:{wood}_sign"] = BLOCK_TEXTURES[f"minecraft:{wood}_wall_sign"] = f"block/{wood}_planks"
    for plant in ("dead_bush", "fern", "red_mushroom", "brown_mushroom"):
        BLOCK_TEXTURES[f"minecraft:potted_{plant}"] = "block/flower_pot"
    block_colour.cache_clear()
    SPECIAL_COLOURS["guhs:kaasfrituursaus"] = (236, 120, 30)
    SPECIAL_COLOURS["guhs:borrelende_kaassaus"] = (226, 190, 60)
    SPECIAL_COLOURS["guhs:barbecuether_portaal"] = (255, 110, 40)
    for n in ("water", "lava"):
        SPECIAL_COLOURS.setdefault("minecraft:" + n, (70, 120, 220) if n == "water" else (230, 100, 20))

    def tiles(name):
        """The Spiesburcht and the Mika-grillpaleis are saved as 32x32 tiles (<name>/stuk_i_j): put them back together."""
        parts = {k: v for k, v in captured.items() if k.startswith(name + "/stuk_")}
        if not parts:
            return None
        W = D = 0
        for k, st in parts.items():
            i, j = (int(v) for v in k.rsplit("_", 2)[-2:])
            W, D = max(W, i * 32 + st.size[0]), max(D, j * 32 + st.size[2])
        whole = ms.Structure((W, max(st.size[1] for st in parts.values()), D))
        for k, st in parts.items():
            i, j = (int(v) for v in k.rsplit("_", 2)[-2:])
            for (x, y, z), v in st.blocks.items():
                whole.blocks[(x + i * 32, y, z + j * 32)] = v
            whole.entities += [(x + i * 32, y, z + j * 32, nbt) for x, y, z, nbt in st.entities]
        return whole

    sprite = lambda quads, blocks, foot=0.85, pitch=-30: {"img": render(quads, 45, pitch, 256, margin=0.02), "blocks": blocks, "foot": foot}
    sprites = {
        "guh": sprite(geo_quads(geo("guh"), "guhs:entity/guh", hide=hide), 1.0),
        "mika": sprite(geo_quads(geo("mika"), "guhs:entity/mika"), 1.0),
        "nether_mika": sprite(geo_quads(geo("mika"), "guhs:entity/nether_mika"), 1.0),
        "kikkerguh": sprite(geo_quads(geo("kikkerguh"), "guhs:entity/kikkerguh_roze"), 0.85),
        "moerasheks_mika": sprite(geo_quads(geo("moerasheks_mika"), "guhs:entity/moerasheks_mika"), 1.3),
        "vonk_mika": sprite(geo_quads(geo("vonk_mika"), "guhs:entity/vonk_mika"), 1.2),
        "knekel_mika": sprite(geo_quads(geo("knekel_mika"), "guhs:entity/knekel_mika"), 1.6),
    }
    npc = lambda kind: {"guh_npc": sprite(geo_quads(geo("guh_sitting"), f"guhs:entity/npc_{kind}"), 1.4, 0.95, -20)}
    jobs = [  # structure, render name, turns, sprites, keep (block filter) or None
        ("barbecueput_groot", "barbecueput_groot", 1, npc("grillguh"), None),
        ("barbecueput_klein_a", "barbecueput_klein", 1, {}, None),
        ("gatenkaas_mijnschacht", "gatenkaas_mijnschacht", 1, {}, "roof"),
        ("stille_voorraadkelder", "stille_voorraadkelder", 1, {}, "roof"),
        ("moerasheks_hut", "moerasheks_hut", 1, sprites, None),
        ("boomhutdorp", "boomhutdorp", 1, dict(sprites, **npc("boswachterguh")), None),
    ]
    for sid, name, turns, spr, keep in jobs + [("spiesburcht", "spiesburcht", 1, sprites, None),
                                               ("mika_grillpaleis", "mika_grillpaleis", 1, sprites, None)]:
        st = captured.get(sid) or tiles(sid)
        if st is None:
            print("no structure captured for", sid)
            continue
        if keep == "roof":
            # underground: leave out the rock above the floor and the "ceiling", so you look into the halls
            W, H, D = st.size
            cut = ms.Structure(st.size)
            top = CUT_HEIGHT.get(sid, H // 2)
            cut.blocks = {k: v for k, v in st.blocks.items() if k[1] <= top}
            cut.entities = [e for e in st.entities if e[1] <= top]
            st = cut
        W, H, D = st.size
        px = max(3, min(10, int(1500 / (W + D))))
        save(render_structure(turned(st, turns), spr, px=px, max_size=1500), f"structure_{name}")
    main_v27_zee(out, captured)
    main_v28(out, captured)


def main_v27_zee(out, captured=None):
    """2.7.0, later: the Diepe Guhzee (a made-up piece of sea, cut open at the front), the Guhbubbel's new template (without its
    water) and the Zeemeerguh character with her own model (a curled mermaid tail: geo guh_npc_zeemeerguh)."""
    import random
    import types
    import make_structures as ms
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    npc = geo_quads(geo("guh_npc_zeemeerguh"), "guhs:entity/npc_zeemeerguh", show_only_variant_bones=("zeemeer",))
    save(render(npc, 28, -12, 360), "npc_zeemeerguh")
    save(item_icon("guhs:block/kaaskoraal", 64), "icon_kaaskoraal")
    for k in ("minecraft:jigsaw", "minecraft:structure_void", "minecraft:barrier", "minecraft:light"):
        SPECIAL_COLOURS[k] = None

    # --- the Guhbubbel: the template of the middle of a Diepe Guhzee, without its water ---
    if captured is None or "onderwater" not in captured:
        import features
        import make_v2 as mv
        got = {}
        ms.Structure.save = lambda self, name: got.__setitem__(name, self)
        mv.Structure.save = ms.Structure.save
        dict(zip(features.FEATURES, features.modules()))["onderwater"].build(types.SimpleNamespace(**vars(mv)))
        captured = dict(captured or {}, onderwater=got["onderwater"])
    st = captured["onderwater"]
    dry = ms.Structure(st.size)
    dry.blocks = {k: v for k, v in st.blocks.items() if v[0] not in ("minecraft:water", "minecraft:bubble_column", "guhs:guh_waterlelie")}
    dry.entities = list(st.entities)                             # (the lily pads would float in the air without the water)
    W, H, D = st.size
    zee = geo_quads(geo("guh"), "guhs:entity/guh_zeemeerguh", hide=hide, show_only_variant_bones=("zeemeer",))
    sprites = {"guh_npc": {"img": render(npc, 45, -20, 256, margin=0.02), "blocks": 1.4, "foot": 0.95},
               "guh": {"img": render(zee, 45, -30, 256, margin=0.02), "blocks": 1.0, "foot": 0.6}}   # (the wild Zeemeerguhs in the sea)
    save(render_structure(turned(dry, 0), sprites, px=max(3, min(8, int(1500 / (W + D)))), max_size=1500), "structure_onderwater")

    # --- a mood picture of the Diepe Guhzee: the edge of a sea, cut open at the front ---
    rng = random.Random(62)
    W, D, Y0 = 84, 64, 26                    # (world y = picture y + Y0)
    sea = ms.Structure((W, 46, D))
    cx, cz, R = 56, 20, 36                   # the sea's middle and the dam's inside
    WATER, FLOOR = 62 - Y0, 34 - Y0
    for x in range(W):
        for z in range(D):
            r = math.hypot(x - cx, z - cz) + 1.6 * math.sin(x * 0.21) + 1.3 * math.cos(z * 0.17)
            if r < R - 9:                    # the flat bottom with low dunes
                top, kind = FLOOR + int(round(0.8 * math.sin(x * 0.45) * math.cos(z * 0.38) + 0.6)), "sea"
            elif r < R:                      # the slope up to the dam
                f = (r - (R - 9)) / 9
                top, kind = int(round(FLOOR + (WATER - FLOOR) * (f * f * (3 - 2 * f)))), "sea"
            elif r < R + 4:                  # the dam with its beach: one block above the water
                top, kind = WATER + 1, "beach"
            else:                            # the land: pink wool hills
                top, kind = WATER + 1 + int(min(6, (r - R - 4) * 0.45 + 1.5 * math.sin(x * 0.3 + z * 0.2))), "land"
            if kind == "sea" and (x == W - 1 or z == D - 1):
                top, kind = WATER, "wall"            # the back sides of the picture: a wall of sand to look at through the water
            for y in range(FLOOR - 3, top + 1):
                if kind == "land":
                    name = "minecraft:pink_wool"
                elif kind == "wall":
                    name = "minecraft:sand" if y > FLOOR - 1 else "minecraft:sandstone"
                elif y >= top - 2:
                    name = "minecraft:sand" if kind == "beach" or (x * 7 + z * 13) % 11 else rng.choice(["minecraft:pink_terracotta", "minecraft:clay"])
                else:
                    name = "minecraft:sandstone" if y >= top - 5 else "minecraft:pink_wool"
                sea.set(x, y, z, name)
            if kind == "sea":
                plant, h = None, 0
                roll = rng.random()
                if r < R - 3:
                    if roll < 0.045:
                        plant, h = "guhs:kaaskoraal", 1
                    elif roll < 0.065:
                        plant, h = rng.choice(["minecraft:brain_coral_block", "minecraft:tube_coral_block", "minecraft:horn_coral_block"]), rng.randint(1, 2)
                    elif roll < 0.078 and top < WATER - 8:
                        plant, h = "minecraft:kelp_plant", rng.randint(4, (WATER - top) * 2 // 3)
                    elif roll < 0.16:
                        plant, h = "minecraft:seagrass", 1
                    elif roll < 0.26:
                        plant, h = "minecraft:sea_pickle", 1
                for y in range(top + 1, WATER + 1):
                    sea.set(x, y, z, plant if plant and y <= top + h else "minecraft:water")
    for x, y, z, kind in ([(rng.uniform(34, 80), rng.uniform(FLOOR + 5, WATER - 4), rng.uniform(3, 44), "guh_vis") for _ in range(16)]
                         + [(46, WATER - 10, 8, "zeemeerguh"), (66, FLOOR + 6, 28, "zeemeerguh")]
                         + [(12, WATER + 5, 40, "guh"), (17, WATER + 2, 50, "guh"), (6, WATER + 6, 22, "guh"), (26, WATER + 2, 59, "guh")]):
        sea.entities.append((x, y, z, {"id": "guhs:" + kind}))
    sprite = lambda quads, blocks, foot=0.85: {"img": render(quads, 45, -30, 256, margin=0.02), "blocks": blocks, "foot": foot}
    sprites = {"guh": sprite(geo_quads(geo("guh"), "guhs:entity/guh", hide=hide), 2.6),      # (drawn a bit big, to see them)
               "guh_vis": sprite(geo_quads(geo("guh_vis"), "guhs:entity/guh_vis"), 1.8, 0.5),
               "zeemeerguh": sprite(geo_quads(geo("guh"), "guhs:entity/guh_zeemeerguh", hide=hide, show_only_variant_bones=("zeemeer",)), 3.0, 0.5)}
    old = SPECIAL_COLOURS.get("minecraft:water")
    SPECIAL_COLOURS["minecraft:water"] = (232, 106, 184)      # the Diepe Guhzee's own water colour
    BLOCK_TEXTURES.setdefault("minecraft:kelp_plant", "block/kelp_plant")
    BLOCK_TEXTURES.setdefault("minecraft:seagrass", "block/seagrass")
    block_colour.cache_clear()
    save(render_structure(sea, sprites, px=12, max_size=1400, glass_water=True), "diepe_guhzee")
    SPECIAL_COLOURS["minecraft:water"] = old
    block_colour.cache_clear()


# ---------------------------------------------------------------------------------------------------------------------
# 2.8.0 renders: Knuffeldal (town, NPCs, Pluisguh, Kruimel-Mika), the buildings, farm animals, hairstyles, plushies...
# ---------------------------------------------------------------------------------------------------------------------
NPCS_28 = ("burgemeesterguh", "cocotje", "bakkerguh", "juf_knuffel", "theeguh", "kapperguh", "boerinneguh", "sterrenkijkerguh",
           "ballonguh", "opa_guh", "badmeesterguh")
VANILLA_ICONS_28 = ("bricks", "furnace", "dirt", "terracotta", "lantern", "pink_carpet", "light_blue_carpet", "white_carpet", "pink_concrete",
                    "white_concrete", "white_terracotta", "pink_terracotta", "smooth_sandstone", "glass_bottle", "glow_ink_sac", "gold_ingot",
                    "iron_ingot", "iron_nugget", "milk_bucket", "paper", "pink_dye", "red_dye", "yellow_dye", "lime_dye", "light_blue_dye",
                    "purple_dye", "quartz", "redstone", "slime_ball", "snowball", "stick", "string", "sugar", "wheat", "wheat_seeds",
                    "white_wool", "pink_wool", "oak_planks", "bucket")
KAPSELS_28 = ("krullen", "kuifje", "knotjes", "strikjes", "pluisbol", "vlechtjes", "hanenkam", "matje")
HAARVERF_28 = {"roze": (255, 143, 203), "mint": (143, 240, 196), "citroen": (255, 240, 122), "lavendel": (199, 166, 255),
               "hemelsblauw": (140, 203, 255), "perzik": (255, 180, 127), "zilver": (216, 220, 230)}
# a 2.8 feature's outfit picture: which pieces go on one guh (one per slot; main_v24's loop puts ALL of them on)
OUTFITS_28 = {"knuffeldal": ["burgemeesterssjerp", "bloesemkransje", "knus_sjaaltje"], "kapper": ["kapsel_krullen", "kapperscape"],
              "kamperen": ["pyjama_pakje", "slaapmutsje"]}
# how the slot buildings and loose structures are turned to see their front (the slot buildings face the plein at z = 30)
TURNS_28 = {"knuffeldal_stadje/bakkerij": 2, "knuffeldal_stadje/theehuis": 2, "knuffeldal_stadje/kapper": 2, "knuffeldal_stadje/creche": 2,
            "knuffeldal_stadje/hoek_noordwest": 2, "knuffeldal_stadje/plein": 0, "guhboerderij": 1, "guh_sterrenwacht": 1,
            "ballonfestival": 1, "kampeerplekje": 1, "knuffelbad": 1, "guheinde_terugpoort": 1}


def town_28(captured):
    """The Knuffeldal town put together like the jigsaw does it: the plein, the four slot buildings and the four corner streets
    (each child turned so its connector faces the plein's jigsaw, and moved against it), and the grijpmachine."""
    import make_structures as ms
    plein = captured["knuffeldal_stadje/plein"]
    step = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
    need = {"north": 2, "south": 0, "east": 3, "west": 1}     # turns that bring the child's south-facing connector to face this way
    opposite = {"north": "south", "south": "north", "east": "west", "west": "east"}
    parts = [(plein, (0, 0, 0))]
    for pos, v in plein.blocks.items():
        if v[0] != "minecraft:jigsaw" or v[2].get("pool", "minecraft:empty") == "minecraft:empty":
            continue
        child = captured.get(v[2]["pool"].split(":", 1)[1])
        if child is None:
            continue
        facing = v[1]["orientation"].split("_")[0]
        turns = 0 if facing in ("up", "down") else need[opposite[facing]]
        ct = turned(child, turns)
        conn = [p for p, b in ct.blocks.items() if b[0] == "minecraft:jigsaw" and b[2].get("name") == v[2]["target"]]
        if not conn:
            continue
        target = tuple(a + b for a, b in zip(pos, step[facing]))
        parts.append((ct, tuple(t - c for t, c in zip(target, conn[0]))))
    lo = [min(o[i] for _, o in parts) for i in range(3)]
    hi = [max(o[i] + st.size[i] for st, o in parts) for i in range(3)]
    town = ms.Structure(tuple(h - l for h, l in zip(hi, lo)))
    for st, o in parts:
        d = [o[i] - lo[i] for i in range(3)]
        for (x, y, z), b in st.blocks.items():
            if b[0] == "minecraft:jigsaw" and (x + d[0], y + d[1], z + d[2]) in town.blocks:
                continue
            town.blocks[(x + d[0], y + d[1], z + d[2])] = b
        town.entities += [(x + d[0], y + d[1], z + d[2], nbt) for x, y, z, nbt in st.entities]
    return town


def main_v28(out, captured=None):
    """2.8.0 "Knuffeldal": the town (plein, Bakkerij, Theehuis, Kapper, Creche, Cocotje's street), the Guhboerderij, the
    Sterrenwacht, the Ballonfestival, the Kampeerplekje, the Knuffelbad, the Guheinde Terugpoort; the 11 new NPCs, the Pluisguh,
    the Kruimel-Mika, the farm animals, the IJscoguh, the balloon, the swim ring and ducks, the 8 hairstyles (+ dyes), the
    outfits, the plushies and the new blocks. Item icons come from main_v24's loop; captured = main_v24's structures."""
    import re
    import types
    import make_structures as ms
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name, crop=False):
        if crop:        # (the pictures of a row of things: without the empty space above and below)
            box = img.getbbox()
            img = img.crop((max(0, box[0] - 8), max(0, box[1] - 8), min(img.width, box[2] + 8), min(img.height, box[3] + 8)))
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    bone_names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    PIECES = {m[0].lower(): tuple(re.findall(r'"([a-z_]+)"', m[1])) for m in re.findall(r'^\s{4}([A-Z_]+)\(Slot\.[A-Z]+, ([^)]*)\)', src, re.M)}

    def npc_quads(kind):
        model = geo(f"guh_npc_{kind}") if os.path.exists(geo(f"guh_npc_{kind}")) else geo("guh_sitting")
        return geo_quads(model, f"guhs:entity/npc_{kind}")

    def dressed(pieces, tex="guhs:entity/guh", variant_bones=(), tint=None):
        quads = geo_quads(geo("guh"), tex, hide=hide, show_only_variant_bones=variant_bones)
        for piece in pieces:
            bones = PIECES[piece]
            others = tuple(b for b in bone_names if not b.startswith(bones))
            extra = geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
            if tint and piece.startswith("kapsel_"):
                for q in extra:
                    q.tint = tint
            quads += extra
        return quads

    # --- the new NPCs (each has its own model with its hat/apron/whistle...) and a group picture ---
    group = []
    for i, kind in enumerate(NPCS_28):
        save(render(npc_quads(kind), 28, -12, 360), f"npc_{kind}")
        group += offset_quads(npc_quads(kind), dx=i * 1.05)
    save(render(group, 12, -12, 1200, margin=0.03), "knuffeldal_npcs", crop=True)

    # --- creatures ---
    save(render(geo_quads(geo("guh"), "guhs:entity/guh_pluisguh", hide=hide, show_only_variant_bones=("pluis",)), 35, -20, 360), "guh_variant_pluisguh")
    save(render(geo_quads(geo("kruimel_mika"), "guhs:entity/kruimel_mika"), 30, -18, 360), "kruimel_mika")
    save(render(geo_quads(geo("guhschaapje"), "guhs:entity/guhschaapje", hide=("kaal",)), 30, -18, 360), "guhschaapje")
    save(render(geo_quads(geo("knabbelkippetje"), "guhs:entity/knabbelkippetje"), 30, -18, 360), "knabbelkippetje")
    save(render(geo_quads(geo("guhkoe"), "guhs:entity/guhkoe"), 30, -18, 360), "guhkoe")
    farm = offset_quads(geo_quads(geo("guhkoe"), "guhs:entity/guhkoe"), dx=-1.3) + geo_quads(geo("guhschaapje"), "guhs:entity/guhschaapje", hide=("kaal",)) + \
        offset_quads(geo_quads(geo("knabbelkippetje"), "guhs:entity/knabbelkippetje"), dx=1.1)
    save(render(farm, 20, -16, 700), "boerderij_dieren", crop=True)
    save(render(geo_quads(geo("ijscoguh"), "guhs:entity/ijscoguh"), 35, -16, 420), "ijscoguh")
    save(render(geo_quads(geo("creche_babyguh"), "guhs:entity/creche_babyguh"), 30, -18, 300), "creche_babyguh")
    kleuren = ("roze", "mint", "lavendel", "citroen")
    save(render(geo_quads(geo("guh_luchtballon"), "guhs:entity/guh_luchtballon_roze", hide=("vlam",)), 30, -10, 460), "guh_luchtballon")
    balloons = []
    for i, k in enumerate(kleuren):
        balloons += offset_quads(geo_quads(geo("guh_luchtballon"), f"guhs:entity/guh_luchtballon_{k}", hide=("vlam",)), dx=i * 4.2)
    save(render(balloons, 15, -8, 900, margin=0.03), "luchtballonnen", crop=True)
    save(render(geo_quads(geo("zwembandje"), "guhs:entity/zwembandje"), 30, -25, 360), "zwembandje")
    duck_bones = {"normaal": (), "guheendje": ("eend_oren",), "badmeestereendje": ("eend_petje", "eend_fluitje"),
                  "duikeendje": ("eend_snorkel", "eend_bril"), "maaneendje": ("eend_slaapmuts",), "gouden_eendje": ("eend_kroontje",)}
    all_extras = tuple(b["name"] for b in json.load(open(geo("badeendje")))["minecraft:geometry"][0]["bones"] if b["name"].startswith("eend_"))
    ducks = []
    for i, (soort, bones) in enumerate(duck_bones.items()):
        q = geo_quads(geo("badeendje"), f"guhs:entity/badeendje_{soort}", hide=tuple(b for b in all_extras if not b.startswith(bones or ("-",))))
        ducks += offset_quads(q, dx=i * 0.75)
    save(render(ducks, 15, -18, 800, margin=0.03), "badeendjes", crop=True)

    # --- hairstyles (the kapper): the 8 kapsels, and the krullen in the hair dyes ---
    row = []
    for i, stijl in enumerate(KAPSELS_28):
        save(render(dressed([f"kapsel_{stijl}"]), 25, -15, 300), f"guh_kapsel_{stijl}")
        row += offset_quads(dressed([f"kapsel_{stijl}"]), dx=i * 1.0)
    save(render(row, 10, -10, 1200, margin=0.03), "kapsels", crop=True)
    row = []
    for i, (verf, rgb) in enumerate(HAARVERF_28.items()):
        row += offset_quads(dressed(["kapsel_krullen"], tint=rgb), dx=i * 1.0)
    save(render(row, 10, -10, 1100, margin=0.03), "haarverf", crop=True)
    save(render(dressed(["kapsel_pluisbol"], tex="guhs:entity/guh_pluisguh", variant_bones=("pluis",)), 30, -15, 360), "pluisguh_kapsel")
    # outfits: main_v24's loop put all of a feature's pieces on one guh; for these three that is too much at once
    for name, pieces in OUTFITS_28.items():
        save(render(dressed(pieces), 35, -20, 360), f"guh_outfit_{name}")

    # --- blocks (3D, double as recipe icons) ---
    BLOCKS = ["knuffelgras", "pluizenboom_stam", "pluizenboom_bladeren", "guhpaddenstoel", "guhpaddenstoel_hoed", "knuffelsteen",
              "knuffelsteen_gezicht", "pluisdak", "knuffelklinkers", "feestbuffettafel", "knus_oorkonde", "seizoensbloembak", "seizoensslinger",
              "bladerhoopje", "knabbeloven", "bakkerij_schoorsteen", "guh_wiegje", "speelkleed", "feestslingers", "theetafel", "theepotje",
              "kappersstoel", "haarwasbak", "pluiswolblok", "guh_voerbak", "kippennestje", "guh_bloempot", "guh_moestuinbak", "guh_telescoop",
              "sterrenlantaarn", "ballonsteiger", "mini_luchtballon", "guh_slaapzak", "guh_wastobbe", "glijbaan_start", "glimtegel",
              "trechtertegel", "knuffelbad_badtegel", "guh_xylofoon"]
    for b in BLOCKS:
        try:
            img = render(model_quads(first_model(b)), 30, -30, 256)
            save(img, b)
            img.save(os.path.join(out, "icon_" + b + ".png"))
        except Exception as e:  # noqa: BLE001
            print("could not render", b, e)
    for b in ("grijpmachine", "sneeuwpopguh"):
        try:
            q = model_quads(f"guhs:block/{b}_onder") + offset_quads(model_quads(f"guhs:block/{b}_boven"), dy=1.0)
            img = render(q, 30, -20, 320)
            save(img, b)
            img.save(os.path.join(out, "icon_" + b + ".png"))
        except Exception as e:  # noqa: BLE001
            print("could not render", b, e)
    row = []
    for i in range(4):
        row += offset_quads(model_quads(f"guhs:block/knuffelsteen_gezicht_{i}"), dx=i * 1.15)
    save(render(row, 10, -12, 700), "knuffelsteen_gezichtjes", crop=True)
    row = []
    for i, s in enumerate(("lente", "zomer", "herfst", "winter")):
        row += offset_quads(model_quads(f"guhs:block/seizoensbloembak_{s}"), dx=i * 1.15)
    save(render(row, 15, -20, 700), "seizoensbloembakken", crop=True)
    # the plushies from the grijpmachine (21: one per real guh kind + the golden glitter one)
    names = [f[:-5] for f in sorted(os.listdir(os.path.join(ASSETS, "blockstates")))
             if f.startswith("knuffel_") and f != "knuffel_brococolief.json"]
    row = []
    for i, n in enumerate(names):
        row += offset_quads(model_quads(first_model(n)), dx=(i % 7) * 0.8, dz=(i // 7) * 0.9)
        save(render(model_quads(first_model(n)), 30, -25, 256), "icon_" + n)
    save(render(row, 25, -30, 1000, margin=0.03), "knuffels", crop=True)
    # flat icons of the vanilla ingredients in the 2.8 recipe cards (make_wiki's recipe_card), where they don't exist yet
    for n in VANILLA_ICONS_28:
        if os.path.exists(os.path.join(out, f"icon_{n}.png")):
            continue
        for ref in (f"item/{n}", f"block/{n}", f"block/{n}_front", f"block/{n.replace('_carpet', '_wool')}", f"block/{n.replace('smooth_', '')}_top"):
            try:
                save(item_icon(ref, 64), "icon_" + n)
                break
            except (KeyError, FileNotFoundError):
                continue
        else:
            print("no vanilla texture for", n)

    # --- the buildings ---
    if captured is None:
        import features
        import make_v2 as mv
        captured = {}
        ms.Structure.save = lambda self, name: captured.__setitem__(name, self)
        mv.Structure.save = ms.Structure.save
        h = types.SimpleNamespace(**vars(mv))
        for module in features.modules():
            module.build(h)
    for k in ("minecraft:jigsaw", "minecraft:structure_void", "minecraft:barrier", "minecraft:light"):
        SPECIAL_COLOURS[k] = None
    SPECIAL_COLOURS.setdefault("minecraft:grass_block", (112, 168, 72))
    SPECIAL_COLOURS.update({"guhs:bakkerij_klantplek": None, "guhs:bakkerij_ingang": None, "guhs:wolkenstroom": None})   # (markers, an invisible lift stream)
    BLOCK_TEXTURES.update({"minecraft:smooth_sandstone": "block/sandstone_top", "minecraft:quartz_stairs": "block/quartz_block_side",
                           "minecraft:quartz_slab": "block/quartz_block_side", "minecraft:water_cauldron": "block/cauldron_side"})
    for wood in ("birch", "crimson", "spruce", "oak", "cherry", "dark_oak", "mangrove"):
        BLOCK_TEXTURES[f"minecraft:{wood}_sign"] = BLOCK_TEXTURES[f"minecraft:{wood}_wall_sign"] = f"block/{wood}_planks"
    block_colour.cache_clear()
    used = {v[0] for sid in captured if sid in TURNS_28 or sid.startswith("knuffeldal_stadje/") for v in captured[sid].blocks.values()}
    for block in sorted(used):
        if block.startswith("guhs:") and block_colour(block) == (200, 0, 200):
            try:
                quads = model_quads(first_model(block.split(":")[1]))
                big = max(quads, key=lambda q: np.linalg.norm(np.cross(q.u, q.v)))
                BLOCK_TEXTURES[block] = big.tex
            except Exception as e:  # noqa: BLE001
                print("no colour for", block, e)
    block_colour.cache_clear()
    sprite = lambda quads, blocks, foot=0.85, pitch=-30: {"img": render(quads, 45, pitch, 256, margin=0.02), "blocks": blocks, "foot": foot}
    sprites = {"guh": sprite(geo_quads(geo("guh"), "guhs:entity/guh", hide=hide), 1.0),
               "guh_normal": sprite(geo_quads(geo("guh"), "guhs:entity/guh", hide=hide), 1.0),
               "guh_pluisguh": sprite(geo_quads(geo("guh"), "guhs:entity/guh_pluisguh", hide=hide, show_only_variant_bones=("pluis",)), 1.0),
               "guhschaapje": sprite(geo_quads(geo("guhschaapje"), "guhs:entity/guhschaapje", hide=("kaal",)), 1.1),
               "knabbelkippetje": sprite(geo_quads(geo("knabbelkippetje"), "guhs:entity/knabbelkippetje"), 0.7),
               "guhkoe": sprite(geo_quads(geo("guhkoe"), "guhs:entity/guhkoe"), 1.5),
               "badeendje": sprite(geo_quads(geo("badeendje"), "guhs:entity/badeendje_normaal", hide=all_extras), 0.5, 0.7)}
    for i, k in enumerate(kleuren):
        sprites[f"guh_luchtballon_{i}"] = sprite(geo_quads(geo("guh_luchtballon"), f"guhs:entity/guh_luchtballon_{k}", hide=("vlam",)), 6.5, 0.95, -15)
    for kind in NPCS_28:
        sprites[f"npc_{kind}"] = sprite(npc_quads(kind), 1.4, 0.95, -20)

    def keyed(st):
        """A copy with every entity's id made specific (npc_<kind>, guh_<variant>, guh_luchtballon_<kleur>), for the sprites."""
        c = ms.Structure(st.size)
        c.blocks = st.blocks
        c.entities = []
        for x, y, z, nbt in st.entities:
            n = dict(nbt)
            base = n["id"].split(":")[1]
            if base == "guh_npc":
                n["id"] = "guhs:npc_" + str(n.get("Kind", "")).lower()
            elif base == "guh" and n.get("Variant"):
                n["id"] = "guhs:guh_" + str(n["Variant"])
            elif base == "guh_luchtballon":
                n["id"] = "guhs:guh_luchtballon_" + str(int(n.get("Kleur", 0)))
            c.entities.append((x, y, z, n))
        return c

    jobs = [("knuffeldal_stadje/plein", "knuffeldal_plein"), ("knuffeldal_stadje/bakkerij", "bakkerij"), ("knuffeldal_stadje/theehuis", "theehuis"),
            ("knuffeldal_stadje/kapper", "kapper"), ("knuffeldal_stadje/creche", "creche"), ("knuffeldal_stadje/hoek_noordwest", "cocotje_straat"),
            ("guhboerderij", "guhboerderij"), ("guh_sterrenwacht", "guh_sterrenwacht"), ("ballonfestival", "ballonfestival"),
            ("kampeerplekje", "kampeerplekje"), ("knuffelbad", "knuffelbad"), ("guheinde_terugpoort", "guheinde_terugpoort")]
    for sid, name in jobs:
        st = captured.get(sid)
        if st is None:
            print("no structure captured for", sid)
            continue
        W, H, D = st.size
        px = max(3, min(24, int(1500 / (W + D))))
        save(render_structure(keyed(turned(st, TURNS_28.get(sid, 1))), sprites, px=px, max_size=1500), f"structure_{name}")
    if "knuffeldal_stadje/plein" in captured:
        town = town_28(captured)
        W, H, D = town.size
        save(render_structure(keyed(town), sprites, px=max(3, int(1700 / (W + D))), max_size=1700), "structure_knuffeldal_stadje")
    main_v281(out, captured)


# 2.8.1: in-game pictures (from the real-client autocheck, kept in docs/screenshots/piep): name -> crop box (1280 x 720)
SHOTS_281 = {"piep_schouder_voor": (440, 260, 840, 620), "piep_twee_schildpadden": (300, 250, 980, 450),
             "piep_guh_wiebel": (320, 180, 960, 540), "piep_nest_zuid": (280, 80, 1000, 600), "piep_nest_binnen": (0, 0, 1280, 720),
             "piep_knus_overzicht": (330, 120, 950, 580), "piep_koek_bakjes": (90, 220, 1190, 420),
             "piep_oppernabbel_bossbar": (250, 0, 950, 440)}


def main_v281(out, captured=None):
    """2.8.1 "Piep": the pieppiepmuisje, Poepschilly and Schilly (side by side: they differ a bit), the boze kaasknabbel and
    the Boze Oppernabbel, the roze guh koek tray (1-6 koeken), the kaasknabbel-nest (outside and cut open), the new item
    icons, and a few in-game screenshots. captured = main_v24's structures (else the nest is built on its own)."""
    import types
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name, crop=False):
        if crop:
            box = img.getbbox()
            img = img.crop((max(0, box[0] - 8), max(0, box[1] - 8), min(img.width, box[2] + 8), min(img.height, box[3] + 8)))
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    def scaled(quads, s):
        for q in quads:
            q.origin, q.u, q.v = q.origin * s, q.u * s, q.v * s
        return quads

    ent = lambda n: geo_quads(geo(n), f"guhs:entity/{n}")
    # --- the creatures ---
    save(render(ent("pieppiepmuisje"), 30, -18, 360), "pieppiepmuisje")
    save(render(ent("poepschilly"), 30, -22, 360), "poepschilly")
    save(render(ent("schilly"), 30, -22, 360), "schilly")
    save(render(offset_quads(ent("poepschilly"), dx=-0.55) + offset_quads(ent("schilly"), dx=0.55), 20, -20, 800, margin=0.03),
         "schildpadden", crop=True)
    save(render(ent("boze_kaasknabbel"), 25, -12, 360), "boze_kaasknabbel")
    save(render(ent("boze_oppernabbel"), 25, -12, 360), "boze_oppernabbel")
    # the Oppernabbel is three times a knabbel (PiepClient: 1.65 against 0.55)
    group = scaled(ent("boze_oppernabbel"), 3.0)
    xs = [c for q in group for c in (q.origin[0], (q.origin + q.u + q.v)[0])]
    w = max(xs) - min(xs)
    group += offset_quads(ent("boze_kaasknabbel"), dx=-w * 0.75, dz=0.6) + offset_quads(ent("boze_kaasknabbel"), dx=w * 0.75, dz=0.6)
    save(render(group, 20, -12, 800, margin=0.03), "boze_kaasknabbels", crop=True)

    # --- the roze guh koek: the tray full, and 1 to 6 in a row ---
    koek = render(model_quads("guhs:block/roze_guh_koek_6"), 30, -35, 360)
    save(koek, "roze_guh_koek_bakje")
    row = []
    for n in range(1, 7):
        row += offset_quads(model_quads(f"guhs:block/roze_guh_koek_{n}"), dx=(n - 1) * 1.15)
    save(render(row, 15, -35, 1000, margin=0.03), "roze_guh_koeken", crop=True)
    # --- item icons (main_v24's loop makes them too, when the whole chain runs) ---
    for n in ("roze_guh_koek", "roze_guh_koek_recept", "pieppiepmuisje_item"):
        save(item_icon(f"guhs:item/{n}", 64), "icon_" + n)

    # --- the kaasknabbel-nest: from outside, and cut open above the arena floor (the kern and the six holes) ---
    st = (captured or {}).get("kaasknabbel_nest")
    if st is None:
        sys.path.insert(0, "tools")
        import make_v2 as mv
        from features import piep_nest
        st, _ = piep_nest.build(types.SimpleNamespace(**vars(mv)))
    for k in ("minecraft:jigsaw", "minecraft:structure_void", "minecraft:barrier", "minecraft:light"):
        SPECIAL_COLOURS[k] = None
    SPECIAL_COLOURS.setdefault("minecraft:grass_block", (112, 168, 72))
    block_colour.cache_clear()
    for block in sorted({v[0] for v in st.blocks.values()}):
        if block.startswith("guhs:") and block_colour(block) == (200, 0, 200):
            try:
                quads = model_quads(first_model(block.split(":")[1]))
                big = max(quads, key=lambda q: np.linalg.norm(np.cross(q.u, q.v)))
                BLOCK_TEXTURES[block] = big.tex
            except Exception as e:  # noqa: BLE001
                print("no colour for", block, e)
    block_colour.cache_clear()
    import make_structures as ms
    W, H, D = st.size
    save(render_structure(st, {}, px=max(3, min(24, int(1500 / (W + D)))), max_size=1500), "structure_kaasknabbel_nest")
    from features import piep_nest
    cut = ms.Structure(st.size)
    cut.blocks = {k: v for k, v in st.blocks.items() if k[1] <= piep_nest.G + 2}
    cut.entities = []
    save(render_structure(cut, {}, px=max(3, min(24, int(1500 / (W + D)))), max_size=1500), "kaasknabbel_nest_arena", crop=True)

    # --- in-game screenshots ---
    shots = os.path.join("docs", "screenshots", "piep")
    for name, box in SHOTS_281.items():
        path = os.path.join(shots, name + ".png")
        if not os.path.exists(path):
            print("no screenshot", path)
            continue
        img = Image.open(path).convert("RGB").crop(box)
        if img.width > 960:
            img = img.resize((960, round(img.height * 960 / img.width)), Image.LANCZOS)
        elif img.width < 640:
            img = img.resize((img.width * 2, img.height * 2), Image.NEAREST)
        save(img, "shot_" + name)
    main_v29(out, captured)


CUT_HEIGHT = {"stille_voorraadkelder": 16, "gatenkaas_mijnschacht": 4}



def main_v25(out, base_items=None):
    """2.5.0: the Guhbubbel (without its water, to see the dome), the Zeemeerguh, the diving and parade outfits, new icons."""
    import re
    import types
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    bone_names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    PIECES = {m[0].lower(): tuple(re.findall(r'"([a-z_]+)"', m[1])) for m in re.findall(r'^\s{4}([A-Z_]+)\(Slot\.[A-Z]+, ([^)]*)\)', src, re.M)}
    sys.path.insert(0, "tools")
    import features
    import make_structures as ms
    import make_v2 as mv
    save(render(geo_quads(geo("guh_npc_zeemeerguh"), "guhs:entity/npc_zeemeerguh", show_only_variant_bones=("zeemeer",)), 28, -12, 360), "npc_zeemeerguh")
    zee = geo_quads(geo("guh"), "guhs:entity/guh_zeemeerguh", hide=hide, show_only_variant_bones=("zeemeer",))
    save(render(zee, 35, -15, 360), "guh_variant_zeemeerguh")
    for name in ("onderwater", "evenementen"):
        module = dict(zip(features.FEATURES, features.modules()))[name]
        quads = geo_quads(geo("guh"), "guhs:entity/guh", hide=hide)
        for piece in getattr(module, "CLOTHES", []):
            bones = PIECES[piece]
            others = tuple(b for b in bone_names if not b.startswith(bones))
            quads += geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
        save(render(quads, 35, -20, 360), f"guh_outfit_{name}")
    item_dir = os.path.join(ASSETS, "textures", "item")
    for f in sorted(os.listdir(item_dir)):
        if f.endswith(".png") and (base_items is None or f not in base_items) and not re.search(r"_\d\d\.png$", f):
            save(item_icon(f"guhs:item/{f[:-4]}", 64), "icon_" + f[:-4])
    captured = {}
    ms.Structure.save = lambda self, name: captured.__setitem__(name, self)
    mv.Structure.save = ms.Structure.save
    dict(zip(features.FEATURES, features.modules()))["onderwater"].build(types.SimpleNamespace(**vars(mv)))
    st = captured["onderwater"]
    for k in ("minecraft:jigsaw", "minecraft:structure_void", "minecraft:barrier", "minecraft:light"):
        SPECIAL_COLOURS[k] = None
    main_v27_zee(out, {"onderwater": st})       # (the Guhbubbel's picture is made the 2.7 way: with the Zeemeerguh, without lily pads)



# ---------------------------------------------------------------------------------------------------------------------
# 2.9.0 "De Grote Guhspelen"
# ---------------------------------------------------------------------------------------------------------------------
NPCS_29 = ("sjoelguh", "doolhofguh", "katapultguh", "spelleiderguh", "schaatsmeesterguh", "stempelguh", "circuitguh",
           "brandweerguh", "politieguh", "apothekerguh", "bouwvakkerguh")
# the minigame modules with their own outfit (tools/features/<name>.py CLOTHES)
OUTFITS_29 = ("sjoelen", "doolhof", "katapult", "knabbelspelen", "elftocht", "circuit")
# structure id -> (picture name, turns): the isometric views (front-left)
STRUCTURES_29 = {"sjoelhuisje": ("structure_sjoelhuisje", 1), "guhdoolhof": ("structure_guhdoolhof", 0),
                 "knabbelkatapult": ("structure_knabbelkatapult", 0), "knabbelspelen": ("structure_knabbelspelen", 0),
                 "guh_circuit": ("structure_guh_circuit", 0), "elfguhjestocht": ("structure_elfguhjestocht", 0),
                 "knuffeldal_stadje/beroepenstraat": ("structure_beroepenstraat", 1), "guh_village/layout_c": ("structure_guhdorp_bouwplaats", 1)}
# in-game pictures (the real-client visual QA of 2.9, docs/screenshots/29_<name>.png, 1280 x 720): name -> crop box
# (None: find the GUI panel in the middle by itself; GUI screenshots are at GUI scale 2, so they are shown 1:1).
_FULL = (0, 0, 1280, 720)
_DEX = (432, 214, 848, 516)          # the Guhdex / Superkompas frame (300 x 210 GUI px + a bit)
_KAST = (400, 188, 880, 540)         # the wardrobe (332 x 250)
SHOTS_29 = {
    "biome_guhpolder": _FULL, "pinguh_klassiek_keizer": (160, 330, 1280, 720), "pinguh_overzicht": (300, 230, 1280, 600),
    "pinguh_slaap_klassiek_keizer": (0, 280, 1280, 720), "pinguh_kleding": (560, 280, 1280, 720),
    **{f"elftocht_dorp{i:02d}_{n}": _FULL for i, n in enumerate(("guhwarden", "snuh", "ijlguh", "knabbelsloten", "vadsvoren", "guhdeloopen",
                                                                 "vadskum", "knabbelsward", "guhlingen", "franeguh", "dokguh"), 1)},
    "npc_schaatsmeester_template": _FULL, "npc_stempelguh_dorp": _FULL, "npc_sjoelguh_in_gebouw": _FULL,
    "gebouw_guhdoolhof_3_close": _FULL, "npc_doolhofguh_omgeving": _FULL, "gebouw_knabbelkatapult_3_close": _FULL,
    "npc_katapultguh_omgeving": _FULL, "gebouw_knabbelspelen_3_close": _FULL, "npc_spelleiderguh_omgeving": _FULL,
    "gebouw_guh_circuit_3_close": _FULL, "npc_circuitguh_omgeving": _FULL, "gebouw_knuffeldal_stadje_3_close": _FULL,
    "beroepenstraat_oost": _FULL, "beroepenstraat_west": _FULL, "npc_politieguh_in_gebouw": _FULL, "bouwplaats_layout_c": _FULL,
    "npc_bouwvakkerguh_template": _FULL, "guhdorp_layout_c_zuid": (300, 120, 980, 560), "npcs_rij1_links": _FULL,
    "npcs_rij2_rechts": _FULL, "oren_rij2_oorwarmers": (0, 300, 1280, 720), "oren_overzicht": (300, 250, 1280, 600),
    "kleding_eten_klaar": (300, 80, 1060, 720), "kleding_al_heb": (240, 120, 1280, 720),
    "kast_hoofd": _KAST, "kast_oren": _KAST, "kast_zoek": _KAST, "kast_dobbel": _KAST, "kast_rugzak": _KAST, "kast_weinig": _KAST,
    "gids_minigames_01": _DEX, "gids_minigames_16": _DEX, "gids_kleding_01": _DEX, "gids_kleding_slot_01": _DEX,
    "gids_knus_overzicht": _DEX, "gids_superkompas_03_minigames": _DEX, "gids_superkompas_01_avontuur": _DEX,
    "guhdex_38_pinguh": _DEX, "guhdex_55_sjoelguh": _DEX,
    "piepmenu_muisje": (464, 236, 816, 506), "piepmenu_poepschilly": (464, 236, 816, 506), "piepmenu_schilly": (464, 236, 816, 506),
    "scherm_sjoelen": None, "scherm_doolhofguh_in_gebouw": None, "scherm_katapult": None, "scherm_spelleiderguh_in_gebouw": None,
    "scherm_schaatsmeester": None, "scherm_circuitguh_in_gebouw": None, "scherm_beauty": None, "scherm_meppen": None,
    "scherm_golf": None, "scherm_smul": None, "scherm_vissen": None, "scherm_race": None, "scherm_disco": None,
}


def load_structure(sid):
    """A structure template read back from its generated .nbt (data/guhs/structure/<sid>.nbt) as a make_structures.Structure,
    so the pictures can be made without running the generators (which write files)."""
    import make_structures as ms
    from make_v2 import read_nbt
    path = os.path.join("src", "main", "resources", "data", "guhs", "structure", sid + ".nbt")
    if not os.path.exists(path):
        return None
    root = read_nbt(path)
    st = ms.Structure(tuple(root["size"]))
    palette = root["palette"] if "palette" in root else root["palettes"][0]
    for b in root["blocks"]:
        p = palette[b["state"]]
        st.blocks[tuple(b["pos"])] = (p["Name"], p.get("Properties", {}), b.get("nbt"))
    st.entities = [(e["pos"][0], e["pos"][1], e["pos"][2], e["nbt"]) for e in root.get("entities", [])]
    return st


def panel_box(img):
    """The GUI panel of a screen screenshot (the world behind a screen is blurred, so it is smooth): the box of rows and
    columns around the middle with many sharp edges. Falls back to the middle of the picture."""
    a = np.asarray(img.convert("L"), dtype=np.float32)
    edges = np.abs(np.diff(a, axis=1))[:-1, :] + np.abs(np.diff(a, axis=0))[:, :-1]
    sharp = edges > 40
    cols = sharp[200:520].sum(0)
    rows = sharp[:, 330:950].sum(1)
    cx = [x for x in range(260, 1020) if cols[x] > 6]
    cy = [y for y in range(90, 690) if rows[y] > 6]
    if not cx or not cy:
        return (340, 140, 940, 600)
    return (max(0, min(cx) - 6), max(0, min(cy) - 6), min(1280, max(cx) + 8), min(720, max(cy) + 8))


def recipe_icons_29(out):
    """Flat icons for the ingredients and results of the 2.9 recipe cards (make_wiki's recipe_card) that don't exist yet."""
    def save(img, name):
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)
    for n in ("beroepen_marshmallowvuur", "beroepen_mengketel", "doolhof_lantaarn", "doolhofheg", "katapult_mikaplank", "knabbelspelen_blik",
              "knotwilg_stam", "sjoelen_bakplank", "sjoelen_stapel", "guh_molentje", "guh_ijsbloempje", "polderijs", "sjoelschijfje"):
        if os.path.exists(os.path.join(ASSETS, "textures", "item", n + ".png")):
            save(item_icon(f"guhs:item/{n}", 64), "icon_" + n)
            continue
        try:
            im = json.load(open(os.path.join(ASSETS, "models", "item", n + ".json")))
            ref = im["parent"] if im.get("parent", "").startswith("guhs:block/") else f"guhs:item/{n}"
            if "layer0" in im.get("textures", {}) and not im.get("parent", "").startswith("guhs:block/"):
                save(item_icon(im["textures"]["layer0"], 64), "icon_" + n)
            else:
                save(render(model_quads(ref), 30, -30, 256).resize((64, 64), Image.LANCZOS), "icon_" + n)
        except (OSError, KeyError, ValueError) as e:
            print("no icon for", n, e)
    for n, ref in (("birch_slab", "block/birch_planks"), ("campfire", "item/campfire"), ("cauldron", "item/cauldron"),
                   ("dark_oak_planks", "block/dark_oak_planks"), ("honeycomb", "item/honeycomb"), ("packed_ice", "block/packed_ice"),
                   ("spruce_planks", "block/spruce_planks"), ("leaves", "block/oak_leaves")):
        if os.path.exists(os.path.join(out, f"icon_{n}.png")):
            continue
        try:
            img = item_icon(ref, 64)
            if n == "leaves":           # (leaves are grey in the jar: the biome colour tints them)
                a = np.asarray(img).astype(np.float32)
                a[..., :3] *= np.array([72, 120, 40]) / 255.0
                img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
            save(img, "icon_" + n)
        except (KeyError, FileNotFoundError, TypeError) as e:
            print("no vanilla texture for", n, e)


def main_v29(out, captured=None):
    """2.9.0 "De Grote Guhspelen": the 11 new guh characters, the Pinguh (klassiek, keizer, pluis; awake and asleep), the six
    minigame outfits and the OREN pieces, the new blocks (Guhpolder, molentje, the games, the beroepen), the new item icons,
    the buildings (Sjoelhuisje, Guhdoolhof, Knabbelkatapult + its 12 Mika forts, Knabbelspelen, Guh-Circuit, the
    Elf-Guhjestocht, the Beroepenstraat and Bob's bouwplaats) and the in-game screenshots of the 2.9 visual QA.
    The buildings come from `captured` (the generators' structures) or else from their generated .nbt files."""
    import re
    sys.path.insert(0, "tools")
    import make_structures as ms
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(img, name, crop=False):
        if crop:
            box = img.getbbox()
            img = img.crop((max(0, box[0] - 8), max(0, box[1] - 8), min(img.width, box[2] + 8), min(img.height, box[3] + 8)))
        img.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    bone_names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    PIECES = {m[0].lower(): tuple(re.findall(r'"([a-z_]+)"', m[1])) for m in re.findall(r'^\s{4}([A-Z_]+)\(Slot\.[A-Z]+, ([^)]*)\)', src, re.M)}

    def npc_quads(kind):
        model = geo(f"guh_npc_{kind}") if os.path.exists(geo(f"guh_npc_{kind}")) else geo("guh_sitting")
        return geo_quads(model, f"guhs:entity/npc_{kind}")

    def dressed(pieces, tex="guhs:entity/guh", variant_bones=()):
        quads = geo_quads(geo("guh"), tex, hide=hide, show_only_variant_bones=variant_bones)
        for piece in pieces:
            bones = PIECES[piece]
            others = tuple(b for b in bone_names if not b.startswith(bones))
            quads += geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
        return quads

    def scaled(quads, s):
        for q in quads:
            q.origin, q.u, q.v = q.origin * s, q.u * s, q.v * s
        return quads

    # --- the 11 new guh characters: each on its own, and two group pictures (the games; the beroepen) ---
    for kind in NPCS_29:
        save(render(npc_quads(kind), 28, -12, 360), f"npc_{kind}")
    for name, kinds in (("grote_guhspelen_npcs", NPCS_29[:7]), ("beroepen_npcs", NPCS_29[7:])):
        group = []
        for i, kind in enumerate(kinds):
            group += offset_quads(npc_quads(kind), dx=i * 1.05)
        save(render(group, 12, -12, 1200 if len(kinds) > 4 else 800, margin=0.03), name, crop=True)

    # --- the Pinguh: its three looks, a group (the keizer is 1.18x, the chick is a baby), and asleep ---
    pin = lambda tex, s=1.0: scaled(geo_quads(geo("guh"), f"guhs:entity/{tex}", hide=hide, show_only_variant_bones=("pinguh",)), s)
    for look, tex in (("klassiek", "guh_pinguh"), ("keizer", "guh_pinguh_keizer"), ("pluis", "guh_pinguh_pluis")):
        save(render(pin(tex), 35, -20, 360), f"guh_variant_pinguh_{look}")
    group = offset_quads(pin("guh_pinguh"), dx=-1.6) + pin("guh_pinguh_keizer", 1.18) + offset_quads(pin("guh_pinguh_pluis", 0.55), dx=1.45)
    save(render(group, 25, -15, 900, margin=0.03), "pinguhs", crop=True)
    if os.path.exists(os.path.join(ASSETS, "textures", "entity", "guh_slaap", "guh_pinguh.png")):
        row = offset_quads(pin("guh_slaap/guh_pinguh"), dx=-0.7) + offset_quads(pin("guh_slaap/guh_pinguh_keizer"), dx=0.7)
        save(render(row, 8, -8, 700, margin=0.03), "pinguhs_slapen", crop=True)
        save(render(geo_quads(geo("guh"), "guhs:entity/guh_slaap/guh", hide=hide), 8, -8, 360), "guh_slapen")

    # --- outfits: the six new minigame sets, and the OREN pieces (each alone, and with a hat) ---
    import features
    mods = dict(zip(features.FEATURES, features.modules()))
    for name in OUTFITS_29:
        pieces = [c for c in getattr(mods[name], "CLOTHES", []) if c in PIECES]
        save(render(dressed(pieces), 35, -20, 360), f"guh_outfit_{name}")
    oren = ["oorstrikje_roze", "oorstrikje_mint", "oorstrikje_geel", "katapult_oorbelletjes", "elftocht_oorwarmers", "disco_koptelefoontje"]
    for piece in oren:
        save(render(dressed([piece]), 20, -10, 300), f"oren_{piece}")
    save(render(dressed(["elftocht_schaatsmuts", "elftocht_oorwarmers", "elftocht_sjaal"]), 30, -15, 360), "oren_met_muts")

    # --- new blocks, in rows ---
    blocks = {
        "guhpolder_blokken": ["guhs:block/rijpgras", "guhs:block/polderijs", "guhs:block/knotwilg_stam", "guhs:block/knotwilg_bladeren",
                              "guhs:block/ijspegelguh_kristal", "guhs:block/guh_ijsbloempje", "guhs:block/rijpsprietjes", "guhs:block/guh_molentje"],
        "elftocht_blokken": ["guhs:block/elftocht_lampion_aan", "guhs:block/elftocht_vuurkorf_aan", "guhs:block/elftocht_kopjes_chocovet",
                             "guhs:block/elftocht_kopjes_snert", "guhs:block/elf_guhjeskruisje"],
        "spelen_blokken": ["guhs:block/sjoelen_stapel", "guhs:block/doolhofheg_gezicht", "guhs:block/doolhof_lantaarn_aan",
                           "guhs:block/katapult_mika", "guhs:block/katapult_knabbelkist", "guhs:block/knabbelspelen_blik_geen",
                           "guhs:block/knabbelspelen_kaasmelkfles_vol"],
        "circuit_blokken": ["guhs:block/circuit_regenboogweg_3", "guhs:block/circuit_kaasweg", "guhs:block/circuit_kaassaus",
                            "guhs:block/circuit_bergijs", "guhs:block/circuit_stuiterpaddenstoel", "guhs:block/circuit_boostring"],
        "beroepen_blokken": ["guhs:block/beroepen_marshmallowvuur_3", "guhs:block/beroepen_mengketel", "guhs:block/beroepen_snotkruid_3",
                             "guhs:block/beroepen_knabbelbuit", "guhs:block/beroepen_dakpan"],
    }
    for name, refs in blocks.items():
        row, i = [], 0
        for ref in refs:
            try:
                row += offset_quads(model_quads(ref), dx=i * 1.3)
                i += 1
            except Exception as e:  # noqa: BLE001
                print("no model", ref, e)
        if row:
            save(render(row, 20, -25, 1100, margin=0.03), name, crop=True)
    for ref, name, yaw in (("guhs:block/guh_molentje", "guh_molentje", 30), ("guhs:block/elf_guhjeskruisje", "elf_guhjeskruisje", 200)):
        try:
            save(render(model_quads(ref), yaw, -20, 360), name)
        except Exception as e:  # noqa: BLE001
            print("no model", ref, e)

    # --- the new Mikas (they never do damage) ---
    for n in ("doolhof_mika", "knabbeldief_mika"):
        if os.path.exists(geo(n)):
            save(render(geo_quads(geo(n), f"guhs:entity/{n}"), 30, -15, 360), n)
    save(render(geo_quads(geo("mika"), "guhs:entity/circuit_mikapikker"), 30, -15, 360), "circuit_mikapikker")

    # --- item icons of 2.9 (coins, loaned things, drinks, clothes, the turtles as items) ---
    item_dir = os.path.join(ASSETS, "textures", "item")
    icons = ["sjoelschijfje", "doolhofknabbel", "katapultster", "spelenlintje", "elfstempel", "circuitbeker", "knabbelmeel",
             "guh_schaatsen", "stempelkaart", "warme_chocovet", "snert_kommetje", "gestolen_knabbel", "sjoelen_schijven",
             "katapult_pluisballen", "guh_zak", "knabbelei_lepel", "knabbelspijker", "guhguhtje_staartje", "blik_pluisbal",
             "guh_brandslang", "kaasmelkdrankje", "snotkruidje", "beroepen_dakpan", "poepschilly_item", "schilly_item",
             "guh_molentje", "elf_guhjeskruisje", "guh_ijsbloempje", "polderijs", "rijpgras"]
    for mod in OUTFITS_29 + ("kleding", "disco"):
        icons += [c for c in getattr(mods[mod], "CLOTHES", []) if c in PIECES]
    for n in dict.fromkeys(icons):
        if os.path.exists(os.path.join(item_dir, n + ".png")):
            save(item_icon(f"guhs:item/{n}", 64), "icon_" + n)
            continue
        try:
            im = json.load(open(os.path.join(ASSETS, "models", "item", n + ".json")))
            parent = im.get("parent", "")
            if parent.startswith("guhs:block/"):
                save(render(model_quads(parent), 30, -30, 256).resize((64, 64), Image.LANCZOS), "icon_" + n)
            elif "elements" in im:
                save(render(model_quads(f"guhs:item/{n}"), 30, -30, 256).resize((64, 64), Image.LANCZOS), "icon_" + n)
            elif "layer0" in im.get("textures", {}):
                save(item_icon(im["textures"]["layer0"], 64), "icon_" + n)
            else:
                print("no icon for", n)
        except (OSError, KeyError, ValueError) as e:
            print("no icon for", n, e)

    recipe_icons_29(out)

    # --- the buildings ---
    for k in ("minecraft:jigsaw", "minecraft:structure_void", "minecraft:barrier", "minecraft:light"):
        SPECIAL_COLOURS[k] = None
    SPECIAL_COLOURS.setdefault("minecraft:grass_block", (112, 168, 72))
    SPECIAL_COLOURS.update({"guhs:doolhof_anker": None, "guhs:knabbelspelen_anker": None, "guhs:circuit_boostring": (236, 120, 220),
                            "guhs:beroepen_guhtjeplek": None, "guhs:beroepen_kluisplek": None, "guhs:beroepen_verstopplek": None,
                            "guhs:circuit_mikaplek": None, "guhs:circuit_rolplek": None, "guhs:circuit_looping": None,
                            "guhs:katapult_fortplek": None, "guhs:beroepen_dakplek": (170, 200, 230)})
    for wood in ("birch", "crimson", "spruce", "oak", "cherry", "dark_oak", "mangrove", "acacia", "jungle", "warped", "bamboo"):
        for kind in ("sign", "wall_sign", "hanging_sign", "wall_hanging_sign"):
            BLOCK_TEXTURES.setdefault(f"minecraft:{wood}_{kind}", f"block/{wood}_planks")
    block_colour.cache_clear()
    structs = {}
    for sid in list(STRUCTURES_29) + [f"katapult/fort_{i:02d}" for i in range(1, 13)]:
        st = (captured or {}).get(sid) or load_structure(sid)
        if st is None:
            print("no structure for", sid)
            continue
        structs[sid] = st
    for block in sorted({v[0] for st in structs.values() for v in st.blocks.values()}):
        if block.startswith("guhs:") and block_colour(block) == (200, 0, 200):
            try:
                quads = model_quads(first_model(block.split(":")[1]))
                big = max(quads, key=lambda q: np.linalg.norm(np.cross(q.u, q.v)))
                BLOCK_TEXTURES[block] = big.tex
            except Exception as e:  # noqa: BLE001
                print("no colour for", block, e)
    block_colour.cache_clear()
    sprite = lambda quads, blocks, foot=0.85, pitch=-30: {"img": render(quads, 45, pitch, 256, margin=0.02), "blocks": blocks, "foot": foot}
    guh = geo_quads(geo("guh"), "guhs:entity/guh", hide=hide)
    sprites = {"guh": sprite(guh, 1.0), "guh_normal": sprite(guh, 1.0),
               "guh_pinguh": sprite(pin("guh_pinguh"), 1.0),
               "guh_pluisguh": sprite(geo_quads(geo("guh"), "guhs:entity/guh_pluisguh", hide=hide, show_only_variant_bones=("pluis",)), 1.0)}
    for kind in NPCS_29 + NPCS_28:
        sprites[f"npc_{kind}"] = sprite(npc_quads(kind), 1.4, 0.95, -20)

    def keyed(st):
        c = ms.Structure(st.size)
        c.blocks = st.blocks
        c.entities = []
        for x, y, z, nbt in st.entities:
            n = dict(nbt)
            base = str(n.get("id", "")).split(":")[-1]
            if base == "guh_npc":
                n["id"] = "guhs:npc_" + str(n.get("Kind", "")).lower()
            elif base == "guh":
                n["id"] = "guhs:guh_" + (str(n["Variant"]) if n.get("Variant") else "normal")
            c.entities.append((x, y, z, n))
        return c
    for sid, (name, turns) in STRUCTURES_29.items():
        st = structs.get(sid)
        if st is None:
            continue
        W, H, D = st.size
        px = max(3, min(24, int(1500 / (W + D))))
        save(render_structure(keyed(turned(st, turns)), sprites, px=px, max_size=1500), name)
    # the 12 Mika forts of the Knabbelkatapult, in two rows
    forts = [structs[f"katapult/fort_{i:02d}"] for i in range(1, 13) if f"katapult/fort_{i:02d}" in structs]
    if forts:
        fw, fh, fd = forts[0].size
        rows = ms.Structure((6 * (fw + 3), fh, 2 * (fd + 6)))
        for i, f in enumerate(forts):
            ox, oz = (i % 6) * (fw + 3), (1 - i // 6) * (fd + 6)
            for (x, y, z), b in f.blocks.items():
                rows.blocks[(x + ox, y, z + oz)] = b
        save(render_structure(rows, {}, px=10, max_size=1500), "katapult_forten", crop=True)

    shots_29(out)
    main_v210(out, captured)


def shots_29(out):
    """The in-game screenshots of 2.9 (docs/screenshots/29_*), cropped (SHOTS_29) and at most 960 wide, as 256-colour PNGs
    (a third of the size, so the PDFs stay small; GUI screenshots have far fewer colours anyway)."""
    for name, box in SHOTS_29.items():
        path = os.path.join("docs", "screenshots", "29_" + name + ".png")
        if not os.path.exists(path):
            print("no screenshot", path)
            continue
        full = Image.open(path).convert("RGB")
        img = full.crop(box or panel_box(full))
        if img.width > 960:
            img = img.resize((960, round(img.height * 960 / img.width)), Image.LANCZOS)
        img.quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.FLOYDSTEINBERG).save(
            os.path.join(out, "shot29_" + name + ".png"), optimize=True)
        print("rendered", "shot29_" + name)


# ---------------------------------------------------------------------------------------------------------------------
# 2.10.0 "Lieve vadsjes van elkaar"
# ---------------------------------------------------------------------------------------------------------------------
_HUISJE = (185, 121, 843, 599)       # the huisje screen (the 2.10 screenshots are 1028 x 720)
_MIJN = (205, 119, 823, 579)         # the Guhdex tab Mijn guhs
_EMOTES = (89, 147, 939, 571)        # the emote picker (two columns)
_F10 = (0, 0, 1028, 720)
# in-game pictures (the real-client visual QA of 2.10, docs/screenshots/210_<name>.png): name -> crop box
SHOTS_210 = {
    "gui_huisje_scherm": _HUISJE, "gui_huisje_klusjes": _HUISJE, "gui_huisje_koepel_uit": _HUISJE, "gui_guhbel_kamer": (185, 129, 843, 591),
    "gui_mijnguhs_lijst": _MIJN, "gui_mijnguhs_pagina": _MIJN, "gui_mijnguhs_baby": _MIJN, "gui_mijnguhs_pagina1_scroll": _MIJN,
    "gui_mijnguhs_pagina1_eind": _MIJN, "gui_samen_emotes": _EMOTES, "gui_samen_emotes_opslot": _EMOTES,
    "huisje_klein": _F10, "huisje_medium": _F10, "huisje_groot": _F10, "huisje_groot_nacht": _F10, "huisjes_voor": _F10, "huisjes_koepel": _F10,
    "klusjes_erf_bezig": _F10, "klusjes_lampjes_avond": _F10,
    "speelgoed_speeltuin": (160, 150, 900, 640), "speelgoed_spelen2": _F10, "speelgoed_spelen3": (0, 180, 1028, 620), "speelgoed_tunnel": _F10,
    "guhkamer_deur": _F10, "guhkamer_binnen": _F10, "guhkamer_binnen_deur": _F10,
    "samen_juichen": _F10, "samen_verdrietje": _F10, "samen_record_dans": _F10, "samen_kart_voor": _F10, "samen_kart_achter": _F10,
    "samen_bff_knuffel": _F10, "samen_zielsguh_naam": _F10, "samen_kleding": (0, 120, 1028, 560), "samen_kleding_dichtbij": (200, 40, 830, 560),
    "favorietjes_explosie": (300, 180, 1028, 620), "favorietjes_hints": (0, 60, 800, 620),
    "doolhof_bovenaf": _F10, "doolhof_knabbel_zweeft": _F10, "doolhof_start_hud": _F10,
    "golf_bovenaf": _F10, "golf_hole1_tees": _F10, "golf_hole1_mat_dichtbij": _F10, "golf_hole3_tees": _F10, "golf_hole8_9_tees": _F10,
    "namen_piep": (0, 250, 700, 620),
    "elftocht_bovenaf": _F10, "elftocht_overzicht_west": _F10, "elftocht_overzicht_zuid": _F10, "elftocht_kanaal_bocht": _F10,
    "elftocht_relief_sloten1": _F10, "elftocht_relief_sloten2": _F10, "elftocht_startboog": _F10,
    "elftocht_dorp01_guhwarden": _F10, "elftocht_dorp03_ijlguh": _F10, "elftocht_dorp05_vadsvoren": _F10, "elftocht_dorp08_knabbelsward": _F10,
    "elftocht_dorp11_dokguh": _F10,
    "structure_guh_sterrenwacht_2_side": _F10, "structure_kaasknabbel_nest_2_side": _F10, "structure_knabbelkatapult_3_close": _F10,
    "structure_sjoelhuisje_2_side": _F10,
}
SAMEN_210 = ("samen_hartjesspeldje", "samen_knuffeltruitje", "samen_zielskroontje", "gouden_hartjeshalsbandje")


def shots_210(out):
    """The in-game screenshots of 2.10 (docs/screenshots/210_*), cropped (SHOTS_210) and at most 960 wide, as 256-colour PNGs."""
    for name, box in SHOTS_210.items():
        path = os.path.join("docs", "screenshots", "210_" + name + ".png")
        if not os.path.exists(path):
            print("no screenshot", path)
            continue
        im = Image.open(path).convert("RGB").crop(box)
        if im.width > 960:
            im = im.resize((960, round(im.height * 960 / im.width)), Image.LANCZOS)
        im.quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.FLOYDSTEINBERG).save(
            os.path.join(out, "shot210_" + name + ".png"), optimize=True)
        print("rendered", "shot210_" + name)


def main_v210(out, captured=None):
    """2.10.0 "Lieve vadsjes van elkaar": the three Guhhuisjes (a guh head: ears as the roof, eye windows, the snoet as the
    door), the toys (glijbaantje, wip, schommel, pluizige tunnel), the guhlampje, the Guhkamer door, the four hartjes
    clothes, the hearts, item and recipe icons, the rebuilt Elf-Guhjestocht and the golf course with its three tees per
    hole (from their .nbt), and the in-game screenshots of the 2.10 visual QA."""
    import re
    sys.path.insert(0, "tools")
    import make_structures as ms
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(im, name, crop=False):
        if crop:
            box = im.getbbox()
            im = im.crop((max(0, box[0] - 8), max(0, box[1] - 8), min(im.width, box[2] + 8), min(im.height, box[3] + 8)))
        im.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    def scaled(quads, s):
        for q in quads:
            q.origin, q.u, q.v = q.origin * s, q.u * s, q.v * s
        return quads

    def span_x(quads):
        xs = [c for q in quads for c in (q.origin[0], (q.origin + q.u)[0], (q.origin + q.v)[0], (q.origin + q.u + q.v)[0])]
        return min(xs), max(xs)

    # --- the Guhhuisjes: each on its own, and the three together at their real sizes (2, 3 and 4 blocks) ---
    sizes = (("klein", 1.0), ("medium", 1.5), ("groot", 2.0))
    for size, _ in sizes:
        save(render(model_quads(f"guhs:block/guhhuisje_{size}_model"), 20, -18, 420), f"guhhuisje_{size}")
    group, x = [], 0.0
    for size, s in sizes:
        quads = scaled(model_quads(f"guhs:block/guhhuisje_{size}_model"), s)
        lo, hi = span_x(quads)
        group += offset_quads(quads, dx=x - lo)
        x += hi - lo + 0.5
    save(render(group, 20, -14, 1100, margin=0.03), "guhhuisjes", crop=True)

    # --- the toys, the guhlampje and the Guhkamer door ---
    def toy(ref, extra=()):
        quads = model_quads(ref)
        for r, off in extra:
            quads += offset_quads(model_quads(r), *off)
        return quads
    spec = {
        "guh_glijbaantje": ("guhs:block/guh_glijbaantje", ()),
        "guh_wip": ("guhs:block/guh_wip", (("guhs:block/guh_wip_plank", (0, 0, 0)),)),
        "guh_schommel": ("guhs:block/guh_schommel", (("guhs:block/guh_schommel_zitje", (0, 0, 0)),)),
        "pluizige_tunnel": ("guhs:block/pluizige_tunnel_dak", (("guhs:block/pluizige_tunnel_ingang", (0, 0, 0)),)),
        "klusjes_guhlampje": ("guhs:block/klusjes_guhlampje_aan", ()),
        "klusjes_guhlampje_uit": ("guhs:block/klusjes_guhlampje_uit", ()),
        "wilde_guhweerder": ("guhs:block/wilde_guhweerder", ()),   # 1.2.0
        "guhkamer_deur": ("guhs:block/guhkamer_deur_onder", (("guhs:block/guhkamer_deur_boven", (0, 1, 0)),)),
    }
    for name, (ref, extra) in spec.items():
        try:
            save(render(toy(ref, extra), 30 if name.startswith(("pluizige", "klusjes", "wilde")) else 210, -22, 420), name)
        except Exception as e:  # noqa: BLE001
            print("no render for", name, e)
    row, x = [], 0.0
    for name in ("guh_schommel", "guh_wip", "guh_glijbaantje"):
        quads = toy(*spec[name])
        lo, hi = span_x(quads)
        row += offset_quads(quads, dx=x - lo)
        x += hi - lo + 0.7
    save(render(row, 205, -20, 1300, margin=0.03), "speelgoed_rij", crop=True)

    # --- the four hartjes clothes: each piece on a guh, the three levels side by side, and all of them together ---
    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    bone_names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    PIECES = {m[0].lower(): tuple(re.findall(r'"([a-z_]+)"', m[1])) for m in re.findall(r'^\s{4}([A-Z_]+)\(Slot\.[A-Z]+, ([^)]*)\)', src, re.M)}

    def dressed(pieces, tex="guhs:entity/guh"):
        quads = geo_quads(geo("guh"), tex, hide=hide)
        for piece in pieces:
            bones = PIECES[piece]
            others = tuple(b for b in bone_names if not b.startswith(bones))
            quads += geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
        return quads
    for piece in SAMEN_210:
        if piece in PIECES:
            save(render(dressed([piece]), 25, -15, 360), "kleding_" + piece)
        else:
            print("no clothes piece", piece)
    save(render(dressed([p for p in SAMEN_210 if p in PIECES]), 30, -18, 420), "guh_outfit_samen")
    trio = []
    for i, pieces in enumerate((["samen_hartjesspeldje"], ["samen_knuffeltruitje"], ["samen_zielskroontje", "gouden_hartjeshalsbandje"])):
        trio += offset_quads(dressed([p for p in pieces if p in PIECES]), dx=i * 1.3)
    save(render(trio, 18, -12, 1100, margin=0.03), "band_niveaus", crop=True)

    # --- the hearts (the particle textures), big and crisp ---
    ims = [texture(f"guhs:particle/{n}").resize((96, 96), Image.NEAREST)
           for n in [f"band_hartje_{i}" for i in range(3)] + ["band_groot_hartje"]
           if os.path.exists(os.path.join(ASSETS, "textures", "particle", n + ".png"))]
    if ims:
        strip = Image.new("RGBA", (len(ims) * 112 - 16, 96), (0, 0, 0, 0))
        for i, im in enumerate(ims):
            strip.paste(im, (i * 112, 0), im)
        save(strip, "band_hartjes")

    # --- item icons (and the vanilla ones the recipe cards need) ---
    item_dir = os.path.join(ASSETS, "textures", "item")
    for n in ("guhhuisje_klein", "guhhuisje_medium", "guhhuisje_groot", "guh_glijbaantje", "guh_wip", "guh_schommel", "pluizige_tunnel",
              "knabbelbal", "guhbel", "klusjes_guhlampje", "klusjes_schelpje") + SAMEN_210:
        if os.path.exists(os.path.join(item_dir, n + ".png")):
            save(item_icon(f"guhs:item/{n}", 64), "icon_" + n)
        else:
            print("no icon for", n)
    try:   # 1.2.0: the Wilde-guhweerder (a block item: its model, small)
        save(render(model_quads("guhs:block/wilde_guhweerder"), 30, -25, 256).resize((64, 64), Image.LANCZOS), "icon_wilde_guhweerder")
    except Exception as e:  # noqa: BLE001
        print("no icon for wilde_guhweerder", e)
    try:   # 1.2.5: the Guhoven (lit, its guh face to the front), and its recipe icon
        oven = render(model_quads("guhs:block/guh_oven_on"), 30, -25, 320)
        save(oven, "guh_oven")
        save(oven.resize((64, 64), Image.LANCZOS), "icon_guh_oven")
    except Exception as e:  # noqa: BLE001
        print("no render for guh_oven", e)
    for n, ref in (("ladder", "block/ladder"), ("oak_door", "item/oak_door"), ("glass_pane", "block/glass"),
                   ("oak_slab", "minecraft:block/oak_slab"), ("oak_fence", "minecraft:block/oak_fence_inventory")):
        if os.path.exists(os.path.join(out, f"icon_{n}.png")):
            continue
        try:
            if ref.startswith("minecraft:block/"):
                save(render(model_quads(ref), 30, -30, 256).resize((64, 64), Image.LANCZOS), "icon_" + n)
            else:
                save(item_icon(ref, 64), "icon_" + n)
        except Exception as e:  # noqa: BLE001
            print("no vanilla icon for", n, e)

    # --- the rebuilt Elf-Guhjestocht and the golf course with three tees per hole (from their .nbt) ---
    for k in ("minecraft:jigsaw", "minecraft:structure_void", "minecraft:barrier", "minecraft:light"):
        SPECIAL_COLOURS[k] = None
    SPECIAL_COLOURS.setdefault("minecraft:grass_block", (112, 168, 72))
    SPECIAL_COLOURS.setdefault("minecraft:moss_block", (90, 140, 50))
    block_colour.cache_clear()
    sprite = lambda quads, blocks, foot=0.85, pitch=-30: {"img": render(quads, 45, pitch, 256, margin=0.02), "blocks": blocks, "foot": foot}
    guh = geo_quads(geo("guh"), "guhs:entity/guh", hide=hide)
    npc = lambda kind: geo_quads(geo(f"guh_npc_{kind}") if os.path.exists(geo(f"guh_npc_{kind}")) else geo("guh_sitting"), f"guhs:entity/npc_{kind}")
    sprites = {"guh": sprite(guh, 1.0), "guh_normal": sprite(guh, 1.0),
               "guh_pinguh": sprite(geo_quads(geo("guh"), "guhs:entity/guh_pinguh", hide=hide, show_only_variant_bones=("pinguh",)), 1.0)}
    for kind in NPCS_29 + ("golfguh",):
        sprites[f"npc_{kind}"] = sprite(npc(kind), 1.4, 0.95, -20)

    def keyed(st):
        c = ms.Structure(st.size)
        c.blocks = st.blocks
        c.entities = []
        for x, y, z, nbt in st.entities:
            n = dict(nbt)
            base = str(n.get("id", "")).split(":")[-1]
            if base == "guh_npc":
                n["id"] = "guhs:npc_" + str(n.get("Kind", "")).lower()
            elif base == "guh":
                n["id"] = "guhs:guh_" + (str(n["Variant"]) if n.get("Variant") else "normal")
            c.entities.append((x, y, z, n))
        return c
    for sid, name, turns, pxmax in (("elfguhjestocht", "structure_elfguhjestocht", STRUCTURES_29.get("elfguhjestocht", (None, 0))[1], 24),
                                    ("guh_golfbaan", "structure_guh_golfbaan", FRONT_TURNS.get("guh_golfbaan", 1), 8)):
        st = (captured or {}).get(sid) or load_structure(sid)
        if st is None:
            print("no structure for", sid)
            continue
        for block in sorted({v[0] for v in st.blocks.values()}):
            if block.startswith("guhs:") and block_colour(block) == (200, 0, 200):
                try:
                    quads = model_quads(first_model(block.split(":")[1]))
                    big = max(quads, key=lambda q: np.linalg.norm(np.cross(q.u, q.v)))
                    BLOCK_TEXTURES[block] = big.tex
                except Exception as e:  # noqa: BLE001
                    print("no colour for", block, e)
        block_colour.cache_clear()
        W, H, D = st.size
        px = max(3, min(pxmax, int(1500 / (W + D))))
        save(render_structure(keyed(turned(st, turns)), sprites, px=px, max_size=1500), name)

    shots_210(out)
    main_v30(out, captured)


# ---------------------------------------------------------------------------------------------------------------------
# 3.0.0 "Guhverhalen" (+ the 2.10.1 screenshots)
# ---------------------------------------------------------------------------------------------------------------------
NPCS_30 = ("timmerguh", "boris", "steele_mika", "muk", "luk", "rosy", "witte_wolfguh", "knabbelkloon", "wolkenhoeder", "lilo_guh",
           "nani_guh", "tikiguh")
# the story guhs: (variant id, texture, variant bone prefix)
VERHAALGUHS_30 = (("baltoguh", "guh_baltoguh", "balto"), ("mewtwo", "guh_mewtwo", "mewtwo"), ("stitch626", "guh_stitch626", "stitch"))
# the critters: picture name -> (geo model, texture); all bones shown
CRITTERS_30 = {
    "pluisvinkje": ("pluisvinkje", "pluisvinkje"), "kaasmeesje": ("kaasmeesje", "kaasmeesje"), "guh_uiltje": ("guh_uiltje", "guh_uiltje"),
    "zeemeeuwtje": ("zeemeeuwtje", "zeemeeuwtje"),
    **{f"guhxolotl_{k}": ("guhxolotl", f"guhxolotl_{k}") for k in ("roze", "mint", "choco", "wit", "goud")},
    "guh_eendje": ("guh_eendje", "guh_eendje"), "guh_eendje_kuiken": ("guh_eendje", "guh_eendje_kuiken"),
    **{f"knabbelvlindertje_{k}": ("knabbelvlindertje", f"knabbelvlindertje_{k}") for k in ("kaasgeel", "roze", "mint", "lila")},
    "glimguhtje": ("glimguhtje", "glimguhtje"), "lieveheersbeestje": ("lieveheersbeestje", "lieveheersbeestje"),
    "pluisegeltje": ("pluisegeltje", "pluisegeltje"),
    **{f"guh_konijntje_{k}": ("guh_konijntje", f"guh_konijntje_{k}") for k in ("roze", "wit", "choco", "grijs")},
    "pluiseekhoorntje": ("pluiseekhoorntje", "pluiseekhoorntje"), "shuckle": ("shuckle", "shuckle"), "mew": ("mew", "mew"),
}
# the outfit sets of the four story questlines + the Timmerguh (clothes pieces, lower case)
OUTFITS_30 = {"timmerguh": ("timmer_helmpje", "timmer_gereedschapsriem"),
              "balto": ("balto_sjaaltje", "balto_wolfsoortjes", "balto_sneeuwmuts", "balto_wantjes"),
              "mewtwo": ("mewtwo_trainerpetje", "mewtwo_trainerpakje", "mewtwo_staartje", "mew_ballonnetje"),
              "hemel": ("hemel_aureooltje", "hemel_wolkenvleugeltjes"),
              "guhwaii": ("guhwaii_hularokje", "guhwaii_bloemenkrans", "guhwaii_stitchoren", "guhwaii_surfplankje")}
# blocks drawn from their block model (first blockstate variant)
BLOCKS_30 = ("baltoguh_beeldje", "nomguh_routepaal", "nomguh_medicijnkist", "nomguh_sneeuwdak", "sneeuwguhspar_gezicht", "sneeuwguhspar_naalden",
             "sneeuwguhspar_zaailing", "baltoslee_sneeuwguh", "baltoslee_minislee", "baltoslee_sledebellen", "baltoslee_beker",
             "baltoslee_hondenmand", "baltoslee_lantaarnpaal", "knuffelhart", "mewtwo_computer", "mewtwo_reageerbuisjes", "mewtwo_papieren",
             "mewtwo_knabbelschaal", "mewtwo_onderdelenkist", "vadsigheid_scanner", "vadsigheid_poster", "guhwaii_palm_gezicht",
             "guhwaii_palm_blad", "guhwaii_palm_kiemplant", "kokosnoot", "roze_hibiscus", "guhwaii_plumeria", "guhwaii_paradijsbloem",
             "guhwaii_orchidee", "schilly_eitjes", "guhwaii_rommeltje", "tiki_fakkel", "tiki_masker", "tiki_masker_roze", "tiki_beeld",
             "tiki_rietdak", "tiki_bloemenslinger", "tiki_schelpjeslampion", "tiki_surfplankrek", "tiki_bloemenmat", "tiki_kruk",
             "tiki_radiootje", "vogels_voerhuisje", "landdiertjes_knabbelvoorraadje", "landdiertjes_steentjespad",
             "timmerguh_dakplek")
# item icons (textures/item/<name>.png)
ITEMS_30 = ("timmerguh_bouwboekje", "timmerguh_dakpluisje", "sneeuwslee", "sledebelletje", "schelpjesmunt", "surfplankje_leen", "herinnering",
            "pluisveertje", "kokosnoot", "kokosmelk", "mewtwo_labnotitie", "mewtwo_tankonderdeel_1", "mewtwo_tankonderdeel_2",
            "mewtwo_tankonderdeel_3", "mewtwo_tankonderdeel_4", "guhxolotl_emmertje_roze", "guhxolotl_emmertje_goud",
            "landdiertjes_guhsteentje", "landdiertjes_bessensapje", "pluisegeltje_item", "guh_konijntje_item", "pluiseekhoorntje_item",
            "shuckle_item") + tuple(p for ps in OUTFITS_30.values() for p in ps)
# structures (id -> picture name, turns)
STRUCTURES_30 = {"nomguh": ("structure_nomguh", 0), "kloon_eiland": ("structure_kloon_eiland", 0),
                 "hemelkapelletje": ("structure_hemelkapelletje", 0), "guhwaii_ohana": ("structure_guhwaii_ohana", 0),
                 "guhwaii_capsule": ("structure_guhwaii_capsule", 0), "guhwaii_surfstrand": ("structure_guhwaii_surfstrand", 0),
                 "knuffeldal_stadje/bouwplaats": ("structure_timmerguh_bouwplaats", 1)}
_F30 = (0, 0, 1028, 720)
_DEX30 = (205, 178, 825, 614)        # a Guhdex page in the 3.0 screenshots (1028 x 720)
# in-game pictures of the 3.0 visual QA (docs/screenshots/30_<name>.png): name -> crop box
SHOTS_30 = {
    "timmerguh_bouwplaats": _F30, "timmerguh_npc": _F30, "gui_timmerguh_praat": _F30, "gui_timmerguh_huisje_alleen_kijken": (178, 112, 852, 590),
    "balto_nomguh_overzicht": _F30, "balto_nomguh_plein": _F30, "balto_nomguh_stal_start": _F30, "balto_nomguh_boot": _F30,
    "balto_boris": _F30, "balto_muk_luk": _F30, "balto_rosy": _F30, "balto_steele_mika": _F30, "balto_toendra_sneeuwstorm": _F30,
    "balto_beeldje_routepaal": (220, 220, 1028, 660), "gui_balto_wolfmoment": _F30, "gui_balto_boris": _F30,
    "nomguh_berghut_binnen": _F30, "nomguh_stal_binnen": _F30, "nomguh_ziekenhuisje_binnen": _F30,
    "baltoslee_sprint_steele_naast_je": _F30, "baltoslee_eigen_sneeuwslee": (280, 230, 880, 560), "baltoslee_deco": (100, 380, 1000, 580),
    "gui_baltoslee_steele": (205, 165, 825, 560), "gui_baltoslee_sprint_onderweg": _F30, "gui_baltoslee_tocht_storm": _F30,
    "mewtwo_eiland_overzicht": _F30, "mewtwo_koepelhal_kapot": _F30, "mewtwo_koepelhal_heel": _F30, "mewtwo_kantoortje": _F30,
    "mewtwo_arena": _F30, "mewtwo_mew_rond_het_eiland": _F30, "mewtwo_steiger": _F30, "gui_mewtwo_professor": _F30,
    "hemel_kapelletje": _F30, "hemel_kapel_binnen": _F30, "hemel_kapel_altaar": _F30, "gui_hemel_scherm": (178, 136, 852, 584),
    "gui_verhaal_wolkjes": _DEX30,
    "structure_guhwaii_ohana_1_overview": _F30, "structure_guhwaii_capsule_1_overview": _F30, "structure_guhwaii_surfstrand_1_overview": _F30,
    "guhwaii_ohana_lilo_kamer": _F30, "guhwaii_ohana_keuken": _F30, "guhwaii_capsule_binnen": _F30, "guhwaii_capsule_poster": (160, 120, 900, 720),
    "guhwaii_scanner_palmen_626": (150, 230, 900, 560), "gui_guhwaii_626_menu": _F30,
    "guhwaiispellen_surfen": _F30, "guhwaiispellen_hula": _F30, "gui_guhwaiispellen_lilo": (180, 118, 852, 584),
    "gui_guhwaiispellen_tikiwinkel": (228, 168, 820, 548),
    "vogels_overdag": (60, 150, 1028, 660), "vogels_nacht": (60, 150, 1028, 660),
    "waterdiertjes_eendjes_en_insectjes": (0, 240, 1028, 480), "waterdiertjes_guhxolotls": (0, 330, 1028, 640),
    "waterdiertjes_glimguhtjes_nacht": (0, 300, 1028, 600),
    "landdiertjes_rij": (40, 420, 1028, 700), "landdiertjes_schouder": _F30, "landdiertjes_voorraadje": (300, 280, 1028, 720),
    "verhaal_verhaalguhs": (150, 150, 1028, 650), "gui_verhaal_superkompas_10_verhalen": (205, 158, 825, 636),
    "biome_guhmension_sneeuwguhtoendra": _F30, "biome_guhmension_guhwaii": _F30, "guhsneeuw_landschap": _F30,
    "dex_67_baltoguh": _DEX30, "dex_68_mewtwo": _DEX30, "dex_69_stitch626": _DEX30, "dex_70_mew": _DEX30, "dex_87_guhxolotl": _DEX30,
    "dex_95_shuckle": _DEX30,
    "gui_guhmenu_dagboekje": _DEX30, "gui_guhmenu_guhkamer": (172, 112, 856, 668),
}
SHOTS_2101 = {"guhsneeuw_ooghoogte": (0, 0, 1280, 720), "guhsneeuw_oost": (0, 0, 1280, 720), "guhsneeuw_lucht": (0, 0, 1280, 720)}


def shots_30(out):
    """The in-game screenshots of the 3.0 visual QA (docs/screenshots/30_*) and of 2.10.1 (2101_*), cropped and at most 960
    wide, as 256-colour PNGs (shot30_* / shot2101_*)."""
    for prefix, shots in (("30", SHOTS_30), ("2101", SHOTS_2101)):
        for name, box in shots.items():
            path = os.path.join("docs", "screenshots", f"{prefix}_{name}.png")
            if not os.path.exists(path):
                print("no screenshot", path)
                continue
            im = Image.open(path).convert("RGB").crop(box)
            if im.width > 960:
                im = im.resize((960, round(im.height * 960 / im.width)), Image.LANCZOS)
            im.quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.FLOYDSTEINBERG).save(
                os.path.join(out, f"shot{prefix}_{name}.png"), optimize=True)
            print("rendered", f"shot{prefix}_{name}")


def main_v30(out, captured=None):
    """3.0.0 "Guhverhalen": the three story guhs (Baltoguh, Guhtwo, 626-guh), the twelve new characters, the critters
    (vogeltjes, waterdiertjes, insectjes, landdiertjes, Sjokkel, Mieuwguh), the sleds and sledehondjes, all sixteen new
    clothes (each on a guh and as a set per story), the new blocks and items, the new buildings (from their .nbt) and the
    in-game screenshots of the 3.0 visual QA and of 2.10.1."""
    import re
    sys.path.insert(0, "tools")
    import make_structures as ms
    os.makedirs(out, exist_ok=True)
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(im, name, crop=False):
        if crop:
            box = im.getbbox()
            im = im.crop((max(0, box[0] - 8), max(0, box[1] - 8), min(im.width, box[2] + 8), min(im.height, box[3] + 8)))
        im.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))

    # --- the three story guhs, front and back, and the three together ---
    trio = []
    for i, (vid, tex, bones) in enumerate(VERHAALGUHS_30):
        quads = geo_quads(geo("guh"), f"guhs:entity/{tex}", hide=hide, show_only_variant_bones=(bones,))
        save(render(quads, 35, -18, 360), f"guh_variant_{vid}")
        save(render(quads, 150, -22, 360), f"guh_variant_{vid}_back")
        trio += offset_quads(quads, dx=i * 1.35)
    save(render(trio, 22, -14, 1100, margin=0.03), "verhaalguhs_30", crop=True)

    # --- the twelve new characters, one by one and in a row ---
    row, x = [], 0.0
    for kind in NPCS_30:
        path = geo(f"guh_npc_{kind}") if os.path.exists(geo(f"guh_npc_{kind}")) else geo("guh_sitting")
        try:
            quads = geo_quads(path, f"guhs:entity/npc_{kind}")
            save(render(quads, 30, -12, 360), f"npc_{kind}")
            xs = [c for q in quads for c in (q.origin[0], (q.origin + q.u)[0], (q.origin + q.v)[0], (q.origin + q.u + q.v)[0])]
            row += offset_quads(quads, dx=x - min(xs))
            x += max(xs) - min(xs) + 0.25
        except Exception as e:  # noqa: BLE001
            print("no render for npc", kind, e)
    if row:
        save(render(row, 12, -8, 1500, margin=0.02), "npcs_30", crop=True)

    # --- the critters (every bone shown: they have no guh variant bones) ---
    for name, (model, tex) in CRITTERS_30.items():
        try:
            save(render(geo_quads(geo(model), f"guhs:entity/{tex}", show_only_variant_bones=("",)), 30, -18, 320, margin=0.06), f"critter_{name}")
        except Exception as e:  # noqa: BLE001
            print("no render for critter", name, e)

    def strip(names, tile=220, gap=10):
        ims = [Image.open(os.path.join(out, f"critter_{n}.png")).resize((tile, tile), Image.LANCZOS) for n in names
               if os.path.exists(os.path.join(out, f"critter_{n}.png"))]
        im = Image.new("RGBA", (len(ims) * (tile + gap) - gap, tile), (0, 0, 0, 0))
        for i, t in enumerate(ims):
            im.alpha_composite(t, (i * (tile + gap), 0))
        return im
    save(strip([f"guhxolotl_{k}" for k in ("roze", "mint", "choco", "wit", "goud")]), "critters_guhxolotls")
    save(strip([f"guh_konijntje_{k}" for k in ("roze", "wit", "choco", "grijs")]), "critters_konijntjes")
    save(strip([f"knabbelvlindertje_{k}" for k in ("kaasgeel", "roze", "mint", "lila")]), "critters_vlindertjes")
    save(strip(["pluisvinkje", "kaasmeesje", "guh_uiltje", "zeemeeuwtje"]), "critters_vogeltjes")

    # --- the sleds, a sledehondje and the surfboard ---
    for name, model, tex, hidden, yaw in (("baltoslee_slee", "baltoslee_slee", "baltoslee_slee", (), 35),
                                          ("baltoslee_slee_steele", "baltoslee_slee", "baltoslee_slee_steele", ("kist",), 35),
                                          ("sneeuwslee", "baltoslee_slee", "sneeuwslee", ("kist",), 35),
                                          ("baltoslee_sledehondje", "baltoslee_sledehondje", "baltoslee_sledehondje", ("belletje", "tong"), 30),
                                          ("guhwaiispellen_surfplank", "guhwaiispellen_surfplank", "guhwaiispellen_surfplank", (), 30)):
        try:
            save(render(geo_quads(geo(model), f"guhs:entity/{tex}", hide=hidden, show_only_variant_bones=("",)), yaw, -22, 360, margin=0.04),
                 f"entity_{name}")
        except Exception as e:  # noqa: BLE001
            print("no render for", name, e)
    try:     # the sneeuwslee with its four guh-sledehondjes in front (two by two)
        team = geo_quads(geo("baltoslee_slee"), "guhs:entity/sneeuwslee", hide=("kist",), show_only_variant_bones=("",))
        dog = lambda dx, dz: offset_quads(geo_quads(geo("baltoslee_sledehondje"), "guhs:entity/baltoslee_sledehondje", hide=("belletje", "tong"),
                                                    show_only_variant_bones=("",)), dx=dx, dz=dz)
        for dz in (-1.6, -2.7):
            team += dog(-0.35, dz) + dog(0.35, dz)
        save(render(team, 40, -22, 620, margin=0.04), "sneeuwslee_span", crop=True)
    except Exception as e:  # noqa: BLE001
        print("no sled team", e)

    # --- the sixteen clothes: each piece on a guh, and the set of every story on one guh ---
    bone_names = [b["name"] for b in json.load(open(geo("guh")))["minecraft:geometry"][0]["bones"]]
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhClothes.java"), encoding="utf-8").read()
    PIECES = {m[0].lower(): tuple(re.findall(r'"([a-z_]+)"', m[1])) for m in re.findall(r'^\s{4}([A-Z_0-9]+)\(Slot\.[A-Z]+, ([^)]*)\)', src, re.M)}

    def dressed(pieces, tex="guhs:entity/guh"):
        quads = geo_quads(geo("guh"), tex, hide=hide)
        for piece in pieces:
            bones = PIECES[piece]
            others = tuple(b for b in bone_names if not b.startswith(bones))
            quads += geo_quads(geo("guh"), f"guhs:entity/guh_clothes/{piece}", hide=others, show_only_variant_bones=bones)
        return quads
    for module, pieces in OUTFITS_30.items():
        have = [p for p in pieces if p in PIECES]
        for piece in have:
            back = piece in ("mew_ballonnetje", "hemel_wolkenvleugeltjes", "guhwaii_surfplankje", "mewtwo_trainerpakje", "timmer_gereedschapsriem")
            save(render(dressed([piece]), 150 if back else 25, -18 if back else -15, 360), "kleding_" + piece)
        save(render(dressed(have), 30, -18, 420), f"guh_outfit_{module}")
        save(render(dressed(have), 150, -22, 420), f"guh_outfit_{module}_back")

    # --- blocks (their block model) and item icons ---
    for block in BLOCKS_30:
        try:
            quads = model_quads(first_model(block))
            if not quads:
                raise ValueError("no elements")
            flip = (lambda im: im.transpose(Image.FLIP_LEFT_RIGHT)) if block == "vadsigheid_poster" else (lambda im: im)   # (its face is drawn mirrored)
            save(flip(render(quads, 30, -24, 320, margin=0.05)), f"block_{block}")
            save(flip(render(quads, 30, -30, 256).resize((64, 64), Image.LANCZOS)), "icon_" + block)
        except Exception as e:  # noqa: BLE001
            print("no block render for", block, e)
    item_dir = os.path.join(ASSETS, "textures", "item")
    for n in ITEMS_30:
        if os.path.exists(os.path.join(item_dir, n + ".png")):
            save(item_icon(f"guhs:item/{n}", 64), "icon_" + n)
        else:
            print("no icon for", n)
    for n, ref in (("guhwaii_ukelele", "guhs:item/guhwaii_ukelele_voor"), ("guhxolotl_emmertje", "guhs:item/guhxolotl_emmertje_roze"),
                   ("mewtwo_tankonderdeel", "guhs:item/mewtwo_tankonderdeel_1"), ("guh_kristal", "guhs:item/guh_kristal"),
                   ("gouden_kaasknabbel", "guhs:item/gouden_kaasknabbel"), ("carrot", "item/carrot"), ("sweet_berries", "item/sweet_berries"),
                   ("guh_vis", "guhs:item/guh_vis"), ("cobblestone", "block/cobblestone"), ("smooth_stone", "block/smooth_stone")):
        try:
            save(item_icon(ref, 64), "icon_" + n)
        except Exception as e:  # noqa: BLE001
            print("no icon for", n, e)

    # --- the new buildings, from their .nbt (isometric, front-left) ---
    for k in ("minecraft:jigsaw", "minecraft:structure_void", "minecraft:barrier", "minecraft:light"):
        SPECIAL_COLOURS[k] = None
    SPECIAL_COLOURS.setdefault("minecraft:grass_block", (112, 168, 72))
    SPECIAL_COLOURS.setdefault("minecraft:moss_block", (90, 140, 50))
    SPECIAL_COLOURS.setdefault("minecraft:snow", (240, 244, 250))
    SPECIAL_COLOURS.setdefault("minecraft:snow_block", (236, 241, 248))
    SPECIAL_COLOURS.setdefault("minecraft:powder_snow", (240, 244, 250))
    for k in ("guhs:wolkenstroom", "guhs:mewtwo_tankwand", "guhs:shuckle_plekje", "guhs:mewtwo_notitieplek"):   # (invisible blocks)
        SPECIAL_COLOURS[k] = None
    for k, ref in (("smooth_quartz", "quartz_block_bottom"), ("smooth_quartz_slab", "quartz_block_bottom"), ("quartz_stairs", "quartz_block_side"),
                   ("purpur_slab", "purpur_block"), ("purpur_stairs", "purpur_block"), ("stone_brick_stairs", "stone_bricks"),
                   ("stone_brick_wall", "stone_bricks"), ("smooth_sandstone", "sandstone_top"), ("water_cauldron", "cauldron_side"),
                   ("spruce_sign", "spruce_planks"), ("spruce_wall_sign", "spruce_planks"), ("oak_sign", "oak_planks"), ("oak_wall_sign", "oak_planks"),
                   ("bamboo_sign", "bamboo_planks"), ("pink_banner", "pink_wool"), ("pink_wall_banner", "pink_wool"), ("white_wall_banner", "white_wool"),
                   ("potted_azure_bluet", "flower_pot"), ("potted_oxeye_daisy", "flower_pot"), ("potted_cactus", "flower_pot"), ("cocoa", "cocoa_stage2")):
        BLOCK_TEXTURES["minecraft:" + k] = "block/" + ref
    block_colour.cache_clear()
    sprite = lambda quads, blocks, foot=0.85, pitch=-30: {"img": render(quads, 45, pitch, 256, margin=0.02), "blocks": blocks, "foot": foot}
    guh = geo_quads(geo("guh"), "guhs:entity/guh", hide=hide)
    sprites = {"guh": sprite(guh, 1.0), "guh_normal": sprite(guh, 1.0)}
    for vid, tex, bones in VERHAALGUHS_30:
        sprites["guh_" + vid] = sprite(geo_quads(geo("guh"), f"guhs:entity/{tex}", hide=hide, show_only_variant_bones=(bones,)), 1.25)
    for kind in NPCS_30 + ("reisguh",):
        path = geo(f"guh_npc_{kind}") if os.path.exists(geo(f"guh_npc_{kind}")) else geo("guh_sitting")
        try:
            sprites[f"npc_{kind}"] = sprite(geo_quads(path, f"guhs:entity/npc_{kind}"), 1.4, 0.95, -20)
        except Exception as e:  # noqa: BLE001
            print("no sprite for", kind, e)
    for name, (model, tex) in CRITTERS_30.items():
        if name in ("shuckle", "mew", "pluisvinkje", "guh_eendje"):
            sprites[name] = sprite(geo_quads(geo(model), f"guhs:entity/{tex}", show_only_variant_bones=("",)), 0.8)

    def keyed(st):
        c = ms.Structure(st.size)
        c.blocks = st.blocks
        c.entities = []
        for x, y, z, nbt in st.entities:
            n = dict(nbt)
            base = str(n.get("id", "")).split(":")[-1]
            if base == "guh_npc":
                n["id"] = "guhs:npc_" + str(n.get("Kind", "")).lower()
            elif base == "guh":
                n["id"] = "guhs:guh_" + (str(n["Variant"]) if n.get("Variant") else "normal")
            c.entities.append((x, y, z, n))
        return c
    for sid, (name, turns) in STRUCTURES_30.items():
        st = (captured or {}).get(sid) or load_structure(sid)
        if st is None:
            print("no structure for", sid)
            continue
        for block in sorted({v[0] for v in st.blocks.values()}):
            if block.startswith("guhs:") and block_colour(block) == (200, 0, 200):
                try:
                    quads = model_quads(first_model(block.split(":")[1]))
                    big = max(quads, key=lambda q: np.linalg.norm(np.cross(q.u, q.v)))
                    BLOCK_TEXTURES[block] = big.tex
                except Exception as e:  # noqa: BLE001
                    print("no colour for", block, e)
        block_colour.cache_clear()
        W, H, D = st.size
        px = max(3, min(16, int(1500 / (W + D))))
        save(render_structure(keyed(turned(st, turns)), sprites, px=px, max_size=1500), name)

    shots_30(out)
    main_v128(out)


# ---------------------------------------------------------------------------------------------------------------------
# 1.2.8: het Bleekwoud (bleekhout, bleekmos, the guh hearts, kaashars and harsstenen, the oogbloempje, the Kraakguh and
# the Kraak-Mika, the Bleke Open Plek and the Houthakkershutje), the guh-palm wood set, the grand cheese fountain with
# its pink ball, and the huisje screen's "Wat kan hier?" overview
# ---------------------------------------------------------------------------------------------------------------------
WOOD_128 = ("bleekhout", "guhwaii_palm")
# blocks whose first blockstate model is the whole block
BLOCKS_128 = ("bleekhout_stam", "bleekhout_gestript", "bleekhout_gezicht", "bleekhout_planken", "bleekhout_bladeren", "bleekhout_trap",
              "bleekhout_plaat", "bleekhout_poort", "bleekhout_luik", "bleekmos", "bleekmos_tapijt", "krakend_guhhartje", "verzuurd_guhhartje",
              "kaashars_blok", "harsstenen", "harsstenen_trap", "harsstenen_plaat", "gebeitelde_harsstenen",
              "guhwaii_palm_stam", "guhwaii_palm_gestript", "guhwaii_palm_planken", "guhwaii_palm_trap", "guhwaii_palm_plaat",
              "guhwaii_palm_poort", "guhwaii_palm_luik")
# blocks with a model of their own for the inventory (fences and walls)
INVENTORY_128 = {"bleekhout_hek": "bleekhout_hek_inventory", "guhwaii_palm_hek": "guhwaii_palm_hek_inventory", "harsstenen_muur": "harsstenen_muur_inventory"}
# plants (a cross of two planes): a 3D picture, and the flat texture as their icon
CROSS_128 = ("bleekhout_zaailing", "oogbloempje", "open_oogbloempje", "bleek_hangmos")
STRUCTURES_128 = {"bleke_open_plek": ("structure_bleke_open_plek", 0), "houthakkershutje": ("structure_houthakkershutje", 0)}
# which way to look at a piece so its front shows (yaw), and where things are in the little Bleekwoud scene
YAW_128 = {"trap": 210, "poort": 210, "deur": 240}
HEART_128 = (3, 3, 3)
GUH_128 = (1.5, 1, -0.2)
SCENE_YAW_128 = 215
_PANEL128 = (310, 120, 970, 600)     # the huisje screen and its overview in the 1.2.8 screenshots (1280 x 720, GUI scale 2)
# in-game pictures of the 1.2.8 visual QA (docs/screenshots/128_<name>.png, made with tools/autocheck/overzicht128.txt): name -> crop box
SHOTS_128 = {"gui_huisje_knop": _PANEL128, "gui_overzicht_klusjes": _PANEL128, "gui_overzicht_speeltjes": _PANEL128,
             "gui_overzicht_en": _PANEL128}


def shots_128(out):
    """The in-game screenshots of 1.2.8 (docs/screenshots/128_*), cropped, as 256-colour PNGs (shot128_*)."""
    for name, box in SHOTS_128.items():
        path = os.path.join("docs", "screenshots", f"128_{name}.png")
        if not os.path.exists(path):
            print("no screenshot", path)
            continue
        im = Image.open(path).convert("RGB").crop(box)
        im.quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.FLOYDSTEINBERG).save(
            os.path.join(out, f"shot128_{name}.png"), optimize=True)
        print("rendered", f"shot128_{name}")


def main_v128(out):
    """1.2.8: the blocks of the Bleekwoud and the guh-palm wood set (3D, from their block models; they double as icons),
    the Kraakguh and the Kraak-Mika, the two Bleekwoud structures and the grand cheese fountain (from their .nbt), a
    little piece of Bleekwoud for the biome page, and the screenshots of the huisje overview."""
    sys.path.insert(0, "tools")
    os.makedirs(out, exist_ok=True)
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")

    def save(im, name, crop=False):
        if crop:
            box = im.getbbox()
            im = im.crop((max(0, box[0] - 8), max(0, box[1] - 8), min(im.width, box[2] + 8), min(im.height, box[3] + 8)))
        im.save(os.path.join(out, name + ".png"))
        print("rendered", name)

    def block(name, quads, yaw=30, pitch=-24, icon=True):
        save(render(quads, yaw, pitch, 320, margin=0.05), f"block_{name}")
        if icon:
            save(render(quads, yaw, -30, 256).resize((64, 64), Image.LANCZOS), "icon_" + name)

    # --- blocks ---
    for b in BLOCKS_128:
        block(b, model_quads(first_model(b)), yaw=YAW_128.get(b.rsplit("_", 1)[-1], 30))
    for b in ("krakend_guhhartje", "verzuurd_guhhartje"):    # (the big picture: awake, at night; the icon stays the item's look)
        block(b, model_quads(f"guhs:block/{b}_wakker"), icon=False)
    for b, model in INVENTORY_128.items():
        block(b, model_quads(f"guhs:block/{model}"), yaw=120)
    for wood in WOOD_128:    # a door is two blocks: both halves (its icon is the flat item texture, like in the game)
        door = model_quads(f"guhs:block/{wood}_deur_bottom_left") + offset_quads(model_quads(f"guhs:block/{wood}_deur_top_left"), dy=1)
        block(f"{wood}_deur", door, yaw=YAW_128["deur"], pitch=-14, icon=False)
    for b in CROSS_128:
        block(b, model_quads(first_model(b)), yaw=20, pitch=-14, icon=False)
    # kaashars sticks to the side of a block: clumps on a bleekhoutstam
    stam = model_quads(first_model("bleekhout_stam"))
    hars = [Quad((1, 1, -0.01), (-1, 0, 0), (0, -1, 0), "guhs:block/kaashars", (0, 0, 1, 1), (0, 0, -1)),
            Quad((1, 1, 1.01), (-1, 0, 0), (0, -1, 0), "guhs:block/kaashars", (0, 0, 1, 1), (0, 0, 1)),
            Quad((1.01, 1, 1), (0, 0, -1), (0, -1, 0), "guhs:block/kaashars", (0, 0, 1, 1), (1, 0, 0)),
            Quad((-0.01, 1, 1), (0, 0, -1), (0, -1, 0), "guhs:block/kaashars", (0, 0, 1, 1), (-1, 0, 0))]
    block("kaashars", stam + hars, icon=False)
    # flat icons: the item textures (plants, doors, signs, the resin clump, the brick, the spawn eggs)
    for name, ref in [(b, f"guhs:block/{b}") for b in CROSS_128 if b != "bleek_hangmos"] + \
                     [(n, f"guhs:item/{n}") for n in ("bleek_hangmos", "bleekhout_deur", "bleekhout_bord", "guhwaii_palm_deur", "guhwaii_palm_bord",
                                                      "kaashars", "harssteen", "kraakguh_spawn_egg", "kraak_mika_spawn_egg")]:
        save(item_icon(ref, 64), "icon_" + name)
    # the guh hearts: asleep and awake, the creaking one and the soured one
    row = []
    for i, m in enumerate(("krakend_guhhartje_slaapt", "krakend_guhhartje_wakker", "verzuurd_guhhartje_slaapt", "verzuurd_guhhartje_wakker")):
        row += offset_quads(model_quads(f"guhs:block/{m}"), dx=i * 1.2)
    save(render(row, 20, -20, 760, margin=0.03), "guhhartjes", crop=True)
    # the oogbloempje by day (closed) and at night (open)
    row = model_quads(first_model("oogbloempje")) + offset_quads(model_quads(first_model("open_oogbloempje")), dx=1.1)
    save(render(row, 20, -14, 520, margin=0.03), "oogbloempjes", crop=True)

    # --- the Kraakguh and the Kraak-Mika (every bone shown: the moss and the twig are theirs) ---
    def kraak(k):
        """Its wooden skin with the glowing eyes (the glow mask) drawn over it, like in the game at night."""
        skin = Image.alpha_composite(texture(f"guhs:entity/{k}"), texture(f"guhs:entity/{k}_glowmask"))
        return geo_quads(geo(k), np.asarray(skin).astype(np.float32), show_only_variant_bones=("",))
    save(render(kraak("kraakguh"), 35, -20, 480), "kraakguh")
    save(render(kraak("kraakguh"), 150, -22, 400), "kraakguh_back")
    save(render(kraak("kraak_mika"), 30, -18, 480), "kraak_mika")
    save(render(kraak("kraak_mika"), 150, -22, 400), "kraak_mika_back")

    # --- a little piece of Bleekwoud: a thick tree with a heart in its trunk, moss, carpets, hanging moss and oogbloempjes ---
    scene = []
    put = lambda model, x, y, z: scene.extend(offset_quads(model_quads(model), x, y, z))
    trunk = ((3, 3), (4, 3), (3, 4), (4, 4))
    for x in range(8):
        for z in range(8):
            put("guhs:block/bleekmos", x, 0, z)
    for x, z in trunk:
        for y in range(1, 7):
            put("guhs:block/krakend_guhhartje_wakker" if (x, y, z) == HEART_128 else "guhs:block/bleekhout_stam", x, y, z)
    for x in range(1, 7):
        for z in range(1, 7):
            for y in (6, 7, 8):
                edge = x in (1, 6) or z in (1, 6)
                if ((x, z) in trunk and y == 6) or (y == 8 and edge) or (y == 6 and edge and (x + z) % 2):
                    continue
                put("guhs:block/bleekhout_bladeren", x, y, z)
    for x, z, n in ((1, 2, 2), (2, 1, 1), (6, 2, 3), (5, 1, 2), (1, 5, 1), (2, 6, 2), (6, 5, 2), (5, 6, 1)):
        for i in range(n):
            put("guhs:block/bleek_hangmos_punt" if i == n - 1 else "guhs:block/bleek_hangmos", x, 5 - i, z)
    for x, z in ((0, 1), (1, 0), (7, 0), (0, 5), (2, 2), (7, 6), (5, 7), (6, 6)):
        put("guhs:block/bleekmos_tapijt", x, 1, z)
    for x, z, m in ((1, 1, "open_oogbloempje"), (5, 0, "oogbloempje"), (0, 3, "open_oogbloempje"), (2, 0, "oogbloempje"), (0, 7, "open_oogbloempje"),
                    (7, 2, "open_oogbloempje"), (6, 7, "oogbloempje"), (7, 5, "open_oogbloempje"), (3, 7, "open_oogbloempje")):
        put(f"guhs:block/{m}", x, 1, z)
    scene += offset_quads(kraak("kraakguh"), *GUH_128)
    save(render(scene, SCENE_YAW_128, -20, 900, margin=0.03), "bleekwoud", crop=True)

    # --- the buildings, from their .nbt (isometric, front-left) ---
    for k in ("minecraft:jigsaw", "minecraft:structure_void", "minecraft:barrier", "minecraft:light"):
        SPECIAL_COLOURS[k] = None
    for k, ref in (("lantern", "lantern"), ("light_gray_bed", "light_gray_wool"), ("glass_pane", "glass"), ("lectern", "lectern_sides"),
                   ("crafting_table", "crafting_table_top"), ("potted_pink_tulip", "pink_tulip"), ("potted_allium", "allium"),
                   ("chest", "oak_planks")):
        BLOCK_TEXTURES.setdefault("minecraft:" + k, "block/" + ref)
    BLOCK_TEXTURES.update({"guhs:bleekhout_wandbord": "guhs:block/bleekhout_planken", "guhs:bleekhout_bord": "guhs:block/bleekhout_planken",
                           "guhs:bleekmos_tapijt": "guhs:block/bleekmos", "guhs:bleekhout_deur": "guhs:block/bleekhout_deur_bottom"})
    block_colour.cache_clear()
    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    guh = geo_quads(geo("guh"), "guhs:entity/guh", hide=hide)
    sprites = {"guh": {"img": render(guh, 45, -30, 256, margin=0.02), "blocks": 1.0, "foot": 0.85}}

    def colours(st):
        """Our own blocks without a texture of their own name (stairs, fences...): the colour of their model's biggest face."""
        for name in sorted({v[0] for v in st.blocks.values()}):
            if name.startswith("guhs:") and block_colour(name) == (200, 0, 200):
                try:
                    quads = model_quads(first_model(name.split(":")[1]))
                    BLOCK_TEXTURES[name] = max(quads, key=lambda q: np.linalg.norm(np.cross(q.u, q.v))).tex
                except Exception as e:  # noqa: BLE001
                    print("no colour for", name, e)
        block_colour.cache_clear()

    for sid, (name, turns) in STRUCTURES_128.items():
        st = load_structure(sid)
        if st is None:
            print("no structure for", sid)
            continue
        colours(st)
        W, H, D = st.size
        save(render_structure(turned(st, turns), sprites, px=max(3, min(22, int(1500 / (W + D)))), max_size=1500), name)
    # the grand cheese fountain: its ball is pink glazed terracotta now (drawn the way main() draws it: 12 px a block)
    st = load_structure("grand_cheese_fountain")
    if st is not None:
        colours(st)
        save(render_structure(st, sprites, px=12, max_size=1100), "structure_grand_cheese_fountain")
    shots_128(out)


def main_px(out):
    """guhpixel: the kern's own pictures (the Guhpixel-poort, the Netwerkkabeltje) and those of every slice
    (tools/wiki_px/<slice>.py renders(r): r is this module, r.OUT the folder; pictures are named <namespace>_*.png)."""
    import importlib
    sys.path.insert(0, "tools")
    os.makedirs(out, exist_ok=True)
    r = sys.modules[__name__]
    r.OUT = out
    try:
        poort = render(model_quads("guhs:block/guhpixel_poort"), 30, -25, 320)
        poort.save(os.path.join(out, "guhpixel_poort.png"))
        poort.resize((64, 64), Image.LANCZOS).save(os.path.join(out, "icon_guhpixel_poort.png"))
        item_icon("guhs:item/guhpixel_netwerkkabeltje").save(os.path.join(out, "icon_guhpixel_netwerkkabeltje.png"))
        print("rendered guhpixel_poort, icon_guhpixel_netwerkkabeltje")
    except Exception as e:
        print("no render for the guhpixel kern", e)
    # the NPC pages of the wiki site look for npc_<kind>.png (own model when the kind has one, else the sitting guh)
    geo = lambda n: os.path.join(ASSETS, "geckolib", "models", "entity", n + ".geo.json")
    for kind in ("lobby_welkomstguh", "lobby_verkoper_guh", "lobby_chatguh", "internetcafe_beheerder", "internetcafe_slaper", "skyblok_guh",
                 "bedwars_guh", "vadsnite_guh", "guhmon_gymleider", "bzg_presentatrice", "bzg_boer", "among_kapitein", "among_logboekguh",
                 "reisbureau_agent"):
        try:
            model = geo(f"guh_npc_{kind}") if os.path.exists(geo(f"guh_npc_{kind}")) else geo("guh_sitting")
            render(geo_quads(model, f"guhs:entity/npc_{kind}"), 28, -12, 360).save(os.path.join(out, f"npc_{kind}.png"))
        except Exception as e:  # noqa: BLE001
            print("no render for npc", kind, e)
    print("rendered the guhpixel npc pictures")
    import wiki_px
    for x in wiki_px.SLICES:
        importlib.import_module(f"wiki_px.{x}").renders(r)


def main_bbq2(out, only=None):
    """bbq2 (Guh-technologie, the Guhbarbecuether buildings, In de ban van de Knabbelring, Super Guhrio): the characters, the
    creatures, the blocks and the buildings, drawn by tools/wiki_bbq2/renders.py (renders(r): r is this module, r.OUT the folder).
    only: a set of "npcs" / "wezens" / "blokken" / "bouwwerken" / "verhalenpad" (default everything; "verhalenpad" = Het Guhpad
    and Het Snuffeleiland, tools/wiki_bbq2/renders_pad.py)."""
    sys.path.insert(0, "tools")
    os.makedirs(out, exist_ok=True)
    r = sys.modules[__name__]
    r.OUT = out
    from wiki_bbq2 import renders as bbq2
    bbq2.renders(r, only)


if __name__ == "__main__":
    if sys.argv[1:2] in (["-h"], ["--help"]):
        print(__doc__)
    elif "--only-px" in sys.argv:      # (just the guhpixel pictures, into an existing img folder)
        sys.argv.remove("--only-px")
        main_px(sys.argv[1] if len(sys.argv) > 1 else os.path.join("docs", "wiki", "img"))
    elif "--only-bbq2" in sys.argv:    # (just the bbq2 pictures, into an existing img folder; --bbq2=npcs,wezens,blokken,bouwwerken,verhalenpad for a part)
        sys.argv.remove("--only-bbq2")
        part = next((a for a in sys.argv if a.startswith("--bbq2=")), None)
        if part:
            sys.argv.remove(part)
        main_bbq2(sys.argv[1] if len(sys.argv) > 1 else os.path.join("docs", "wiki", "img"), set(part[7:].split(",")) if part else None)
    elif "--only-128" in sys.argv:     # (just the 1.2.8 pictures, into an existing img folder)
        sys.argv.remove("--only-128")
        main_v128(sys.argv[1] if len(sys.argv) > 1 else os.path.join("docs", "wiki", "img"))
    elif "--only-30" in sys.argv:        # (just the 3.0 + 2.10.1 pictures, into an existing img folder)
        sys.argv.remove("--only-30")
        main_v30(sys.argv[1] if len(sys.argv) > 1 else os.path.join("docs", "wiki", "img"))
    elif "--only-210" in sys.argv:       # (just the 2.10 pictures, into an existing img folder)
        sys.argv.remove("--only-210")
        main_v210(sys.argv[1] if len(sys.argv) > 1 else os.path.join("docs", "wiki", "img"))
    elif "--only-29" in sys.argv:        # (just the 2.9 pictures, into an existing img folder)
        sys.argv.remove("--only-29")
        main_v29(sys.argv[1] if len(sys.argv) > 1 else os.path.join("docs", "wiki", "img"))
    else:
        main(sys.argv[1] if len(sys.argv) > 1 else os.path.join("docs", "wiki", "img"))
        main_px(sys.argv[1] if len(sys.argv) > 1 else os.path.join("docs", "wiki", "img"))
        main_bbq2(sys.argv[1] if len(sys.argv) > 1 else os.path.join("docs", "wiki", "img"))
