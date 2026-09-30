"""
Generates the STARTING version of the Guh model, texture and animations.

After the first run you should edit the model in Blockbench instead (see README.md) -- re-running this
script overwrites guh.geo.json / guh.png / guh.animation.json with the generated version.

Coordinates are Bedrock/GeckoLib style: 16 units = 1 block, y up, the face points to -Z ("north").

Requires: pillow, numpy.   Run from the project root:  python tools/make_guh_model.py
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ASSETS = os.path.join("src", "main", "resources", "assets", "guhs")
UV_W, UV_H = 128, 128   # UV space used in the model file
PX = 4                  # texture pixels per UV unit -> 512x512 png (smooth enough for cute eyes)

FUR = (250, 204, 218)
FUR_DARK = (236, 184, 200)
INNER_EAR = (238, 148, 165)
NOSE = (240, 150, 168)
LEATHER = (140, 86, 46)
LEATHER_DARK = (96, 56, 30)
MIKA_FUR = (226, 170, 190)
MIKA_TAIL = (104, 38, 88)
MIKA_SPADE = (150, 20, 40)

# ---------------------------------------------------------------------------------------------------
# Geometry: bone -> list of cubes (origin = min corner, size), plus paint hints per cube
# ---------------------------------------------------------------------------------------------------
BONES = [
    # name,     parent,  pivot
    ("root", None, [0, 0, 0]),
    ("body", "root", [0, 5, 5]),
    ("tail", "body", [0, 0.5, 11]),
    ("leg_back_left", "body", [6, 1, 8]),
    ("leg_back_right", "body", [-6, 1, 8]),
    ("head", "root", [0, 5, -2]),
    ("ear_left", "head", [7.5, 12, -7.5]),
    ("ear_right", "head", [-7.5, 12, -7.5]),
    ("leg_front_left", "root", [4, 1, -11]),
    ("leg_front_right", "root", [-4, 1, -11]),
    ("saddle", "body", [0, 10, 4]),  # only shown when the guh wears a saddle (GuhRenderer)
]


def rounded(name, origin, size, insets, **paint):
    """A soft, rounded box made of overlapping slabs.

    insets = one (x, y, z) inset per slab. Each slab keeps its planes on distinct depths so nothing z-fights.
    Paint hints (face=True, logo=True, ...) apply to the slab that owns that face.
    """
    cubes = []
    for i, (ix, iy, iz) in enumerate(insets):
        o = [origin[0] + ix, origin[1] + iy, origin[2] + iz]
        sz = [size[0] - 2 * ix, size[1] - 2 * iy, size[2] - 2 * iz]
        hints = {}
        if paint.get("face") and iz == 0:
            hints["face"] = True
        if paint.get("logo") and ix == 0:
            hints["logo"] = True
        if paint.get("belly") and iz == 0:
            hints["belly"] = True
        cubes.append(dict(name=f"{name}_{i}", origin=o, size=sz, **hints))
    return cubes


# slab insets: (x-slab, y-slab, z-slab, corner filler) -- per axis all values differ -> no z-fighting
HEAD_ROUNDING = [(0, 2.5, 2.5), (2.5, 0, 2), (1, 1.5, 0), (1.5, 1, 1)]
BODY_ROUNDING = [(0, 1.5, 2), (2, 0, 1.5), (1, 1, 0), (0.5, 0.5, 0.5)]

CUBES = {
    "body": rounded("body", [-6.5, 0, -2], [13, 10, 13], BODY_ROUNDING, logo=True),
    "tail": [dict(name="tail", origin=[-0.75, 0, 10.5], size=[1.5, 1, 7], tint=FUR_DARK)],
    "leg_back_left": [dict(name="paw_bl", origin=[4.5, 0, 6], size=[3.5, 2.5, 4.5])],
    "leg_back_right": [dict(name="paw_br", origin=[-8, 0, 6], size=[3.5, 2.5, 4.5])],
    "head": rounded("head", [-8, 0, -12], [16, 14, 12], HEAD_ROUNDING, face=True) + [
        dict(name="nose", origin=[-1, 4.5, -12.75], size=[2, 1.5, 1], tint=NOSE),
    ],
    "ear_left": [
        dict(name="ear_l_wide", origin=[4.5, 12, -8], size=[7, 5, 1], ear=True),
        dict(name="ear_l_tall", origin=[5.5, 11, -8.25], size=[5, 7, 1.5], ear=True),
    ],
    "ear_right": [
        dict(name="ear_r_wide", origin=[-11.5, 12, -8], size=[7, 5, 1], ear=True),
        dict(name="ear_r_tall", origin=[-10.5, 11, -8.25], size=[5, 7, 1.5], ear=True),
    ],
    "leg_front_left": [dict(name="paw_fl", origin=[2.25, 0, -13.5], size=[3.5, 2, 3.5])],
    "leg_front_right": [dict(name="paw_fr", origin=[-5.75, 0, -13.5], size=[3.5, 2, 3.5])],
    "saddle": [
        dict(name="saddle_seat", origin=[-4, 10, 0.5], size=[8, 1, 7], tint=LEATHER),
        dict(name="saddle_front", origin=[-3, 10.5, 0], size=[6, 1.5, 1], tint=LEATHER_DARK),
        dict(name="saddle_back", origin=[-3.5, 10.5, 7], size=[7, 2, 1], tint=LEATHER_DARK),
        dict(name="strap_left", origin=[6.5, 2, 3], size=[0.5, 8.5, 2], tint=LEATHER_DARK),
        dict(name="strap_right", origin=[-7, 2, 3], size=[0.5, 8.5, 2], tint=LEATHER_DARK),
    ],
}

# Sitting guh (quest guh + Bank Guh block): sits up like the LPS figure, big head on top, belly patch,
# front paws raised in front of the chest, hind feet pointing forward, tail lying behind.
SITTING_BONES = [
    ("root", None, [0, 0, 0]),
    ("body", "root", [0, 2, 0]),
    ("tail", "body", [0, 0.5, 4]),
    ("foot_left", "body", [4, 1, -4]),
    ("foot_right", "body", [-4, 1, -4]),
    ("arm_left", "body", [3.5, 10, -4]),
    ("arm_right", "body", [-3.5, 10, -4]),
    ("head", "body", [0, 13, 0]),
    ("ear_left", "head", [7.5, 24, -1]),
    ("ear_right", "head", [-7.5, 24, -1]),
]
SITTING_BODY_ROUNDING = [(0, 1.5, 1.5), (1.5, 0, 1), (1, 1, 0), (0.5, 0.5, 0.5)]
SITTING_CUBES = {
    "body": rounded("body", [-5, 1, -4], [10, 12, 8], SITTING_BODY_ROUNDING, belly=True) + [
        dict(name="thigh_l", origin=[4.5, 1, -3], size=[2, 5, 6]),
        dict(name="thigh_r", origin=[-6.5, 1, -3], size=[2, 5, 6]),
    ],
    "tail": [dict(name="tail", origin=[-0.75, 0, 4], size=[1.5, 1, 8], tint=FUR_DARK)],
    "foot_left": [dict(name="foot_l", origin=[2.5, 0, -7], size=[3.5, 2, 5])],
    "foot_right": [dict(name="foot_r", origin=[-6, 0, -7], size=[3.5, 2, 5])],
    "arm_left": [dict(name="paw_l", origin=[1.5, 7, -6.5], size=[3, 3.5, 3])],
    "arm_right": [dict(name="paw_r", origin=[-4.5, 7, -6.5], size=[3, 3.5, 3])],
    "head": rounded("head", [-8, 12, -7], [16, 14, 13], HEAD_ROUNDING, face=True) + [
        dict(name="nose", origin=[-2, 16.5, -7.75], size=[4, 1.5, 1], tint=NOSE),
    ],
    "ear_left": [
        dict(name="ear_l_wide", origin=[4.5, 24, -1.5], size=[7, 5, 1], ear=True),
        dict(name="ear_l_tall", origin=[5.5, 23, -1.75], size=[5, 7, 1.5], ear=True),
    ],
    "ear_right": [
        dict(name="ear_r_wide", origin=[-11.5, 24, -1.5], size=[7, 5, 1], ear=True),
        dict(name="ear_r_tall", origin=[-10.5, 23, -1.75], size=[5, 7, 1.5], ear=True),
    ],
}

# Mika: same guh, but an evil devil-tipped tail and no saddle
MIKA_CUBES = {k: v for k, v in CUBES.items() if k != "saddle"}
MIKA_CUBES["tail"] = [
    dict(name="tail", origin=[-0.75, 0, 10.5], size=[1.5, 1, 7], tint=MIKA_TAIL),
    dict(name="tail_spade", origin=[-1.75, 0, 17.5], size=[3.5, 1, 1.5], tint=MIKA_SPADE),
    dict(name="tail_spade_tip", origin=[-0.75, 0, 19], size=[1.5, 1, 1], tint=MIKA_SPADE),
]


def face_sizes(size):
    w, h, d = size
    return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}


class ShelfPacker:
    def __init__(self, width):
        self.width, self.x, self.y, self.row_h = width, 0, 0, 0

    def place(self, w, h):
        w, h = math.ceil(w), math.ceil(h)
        if self.x + w > self.width:
            self.x, self.y, self.row_h = 0, self.y + self.row_h, 0
        pos = (self.x, self.y)
        self.x += w
        self.row_h = max(self.row_h, h)
        return pos


# ---------------------------------------------------------------------------------------------------
# Painting helpers
# ---------------------------------------------------------------------------------------------------
rng = np.random.default_rng(7)
img = Image.new("RGBA", (UV_W * PX, UV_H * PX), (0, 0, 0, 0))


def paint_fur(box, color, direction):
    """Fill a face with soft plush fur: vertical gradient + fine noise."""
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    light = {"up": 1.06, "north": 1.0, "south": 0.97, "east": 0.98, "west": 0.98, "down": 0.86}[direction]
    arr = np.zeros((h, w, 4), dtype=np.float32)
    grad = np.linspace(1.02, 0.94, h)[:, None] if direction not in ("up", "down") else np.ones((h, 1))
    noise = rng.normal(0, 0.018, (h, w))
    for c in range(3):
        arr[..., c] = color[c] * light * grad * (1 + noise)
    arr[..., 3] = 255
    img.paste(Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)), (x0, y0))


def paint_eye(draw, cx, cy, r, side):
    """Big shiny Littlest-Pet-Shop eye."""
    lash = (35, 25, 40, 255)
    draw.ellipse([cx - r * 1.08, cy - r * 1.12, cx + r * 1.08, cy + r * 1.02], fill=lash)          # outline/lash
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 255, 255, 255))                     # white rim
    draw.ellipse([cx - r * 0.9, cy - r * 0.9, cx + r * 0.9, cy + r * 0.9], fill=(70, 150, 210, 255))   # iris dark
    draw.ellipse([cx - r * 0.78, cy - r * 0.62, cx + r * 0.78, cy + r * 0.9], fill=(120, 196, 235, 255))  # iris light
    draw.ellipse([cx - r * 0.5, cy - r * 0.55, cx + r * 0.5, cy + r * 0.45], fill=(20, 20, 30, 255))    # pupil
    draw.ellipse([cx - r * 0.42, cy - r * 0.52, cx - r * 0.02, cy - r * 0.12], fill=(255, 255, 255, 255))  # big glint
    draw.ellipse([cx + r * 0.2, cy + r * 0.2, cx + r * 0.4, cy + r * 0.4], fill=(255, 255, 255, 255))      # small glint
    # little lash flick at the outer top
    draw.line([cx + side * r * 0.8, cy - r * 0.8, cx + side * r * 1.25, cy - r * 1.2], fill=lash, width=3)


def paint_face(box):
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    # blush
    for bx in (x0 + w * 0.14, x1 - w * 0.14):
        d.ellipse([bx - 7, y0 + h * 0.74 - 4, bx + 7, y0 + h * 0.74 + 4], fill=(245, 150, 170, 150))
    layer = layer.filter(ImageFilter.GaussianBlur(2.5))
    img.alpha_composite(layer)
    d = ImageDraw.Draw(img)
    r = w * 0.17
    paint_eye(d, x0 + w * 0.26, y0 + h * 0.42, r, side=-1)
    paint_eye(d, x1 - w * 0.26, y0 + h * 0.42, r, side=1)
    # tiny "w" mouth under the nose
    mc, my = x0 + w / 2, y0 + h * 0.86
    mouth = (200, 110, 130, 255)
    d.line([mc - 6, my - 2, mc - 3, my + 2, mc, my - 1, mc + 3, my + 2, mc + 6, my - 2], fill=mouth, width=2)


def paint_evil_face(box):
    """Mika: red slit eyes, angry eyebrows and a fanged grin."""
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    d = ImageDraw.Draw(img)
    r = w * 0.15
    for cx, side in ((x0 + w * 0.26, -1), (x1 - w * 0.26, 1)):
        cy = y0 + h * 0.46
        d.ellipse([cx - r * 1.08, cy - r * 1.02, cx + r * 1.08, cy + r * 1.02], fill=(25, 10, 20, 255))
        d.ellipse([cx - r * 0.95, cy - r * 0.9, cx + r * 0.95, cy + r * 0.9], fill=(200, 20, 35, 255))
        d.ellipse([cx - r * 0.7, cy - r * 0.6, cx + r * 0.7, cy + r * 0.7], fill=(255, 70, 60, 255))
        d.rectangle([cx - r * 0.16, cy - r * 0.75, cx + r * 0.16, cy + r * 0.75], fill=(15, 0, 5, 255))  # slit pupil
        d.ellipse([cx - r * 0.55, cy - r * 0.6, cx - r * 0.25, cy - r * 0.3], fill=(255, 220, 220, 255))
        # angry eyebrow: high on the outside, low towards the nose
        outer = cx + side * r * 1.3
        inner = cx - side * r * 1.0
        # the brow covers the top of the eye, lowest near the nose -> angry squint
        d.polygon([(outer, cy - r * 2.0), (outer, cy - r * 1.35), (inner, cy - r * 0.35), (inner, cy - r * 2.0)],
                  fill=MIKA_FUR + (255,))
        d.line([outer, cy - r * 1.35, inner, cy - r * 0.35], fill=(40, 12, 28, 255), width=6)
    # evil grin with a fang
    mc, my = x0 + w / 2, y0 + h * 0.84
    d.arc([mc - 12, my - 9, mc + 12, my + 5], start=15, end=165, fill=(90, 10, 40, 255), width=3)
    d.polygon([(mc + 3, my + 3), (mc + 7, my + 3), (mc + 5, my + 8)], fill=(255, 255, 255, 255))


def paint_belly(box):
    """Darker pink belly oval, like the LPS figure."""
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    d = ImageDraw.Draw(img)
    d.ellipse([x0 + w * 0.2, y0 + h * 0.3, x1 - w * 0.2, y1 - h * 0.02], fill=(236, 118, 140, 255))


def lieke_face():
    """Lieke's face (from the guh model/texture) as a picture, or None if it isn't there."""
    try:
        geo = json.load(open(os.path.join(ASSETS, "geckolib", "models", "entity", "guh.geo.json")))["minecraft:geometry"][0]
        tex = Image.open(os.path.join(ASSETS, "textures", "entity", "guh.png")).convert("RGBA")
    except OSError:
        return None
    scale = tex.width / geo["description"]["texture_width"]
    head = next(b for b in geo["bones"] if b["name"] == "head")
    front = max(head["cubes"], key=lambda c: c["size"][0] if c["size"][0] <= 14 else 0)  # the 14-wide face cube
    (u, v), (w, h) = front["uv"]["north"]["uv"], front["uv"]["north"]["uv_size"]
    return tex.crop((int(u * scale), int(v * scale), int((u + w) * scale), int((v + h) * scale)))


LIEKE_FUR = (194, 159, 204)


def paint_lieke_face(box, fur):
    """Lieke's eyes, nose and mouth on this face, with her lilac fur swapped for this model's fur colour."""
    face = lieke_face()
    if face is None:
        paint_face(box)
        return
    x0, y0, x1, y1 = box
    face = face.resize((x1 - x0, y1 - y0), Image.NEAREST)
    for yy in range(face.height):
        for xx in range(face.width):
            r, g, b, a = face.getpixel((xx, yy))
            if a < 128 or math.dist((r, g, b), LIEKE_FUR) < 22:
                continue  # fur: keep the fur already painted here
            img.putpixel((x0 + xx, y0 + yy), (r, g, b, 255))


def paint_logo(box):
    """The little blue tag on the plush's hip."""
    x0, y0, x1, y1 = box
    cx, cy, r = x0 + (x1 - x0) * 0.3, y0 + (y1 - y0) * 0.45, 6
    d = ImageDraw.Draw(img)
    d.ellipse([cx - r, cy - r * 1.2, cx + r, cy + r * 1.2], fill=(40, 60, 170, 255))
    d.ellipse([cx - r * 0.55, cy - r * 0.75, cx + r * 0.55, cy + r * 0.75], outline=(170, 60, 160, 255), width=2)


def paint_inner_ear(box):
    x0, y0, x1, y1 = box
    d = ImageDraw.Draw(img)
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=FUR + (255,))
    d.rounded_rectangle([x0 + 3, y0 + 3, x1 - 4, y1 - 4], radius=5, fill=INNER_EAR + (255,))


# ---------------------------------------------------------------------------------------------------
# Build geo.json + paint texture
# ---------------------------------------------------------------------------------------------------
def build(identifier, cubes_by_bone, fur, evil=False, bones=None, lieke=False):
    """Returns (geo json, texture) for one variant of the model."""
    global img
    img = Image.new("RGBA", (UV_W * PX, UV_H * PX), (0, 0, 0, 0))
    packer = ShelfPacker(UV_W)
    bones_json = []
    for name, parent, pivot in (bones or BONES):
        if name not in cubes_by_bone and name == "saddle":
            continue
        bone = {"name": name, "pivot": pivot}
        if parent:
            bone["parent"] = parent
        cubes = []
        for cube in cubes_by_bone.get(name, []):
            uv = {}
            for direction, (fw, fh) in face_sizes(cube["size"]).items():
                u, v = packer.place(fw, fh)
                uv[direction] = {"uv": [u, v], "uv_size": [fw, fh]}
                box = (u * PX, v * PX, u * PX + math.ceil(fw * PX), v * PX + math.ceil(fh * PX))
                paint_fur(box, cube.get("tint", fur), direction)
                if cube.get("face") and direction == "north":
                    if evil:
                        paint_evil_face(box)
                    elif lieke:
                        paint_lieke_face(box, fur)
                    else:
                        paint_face(box)
                if cube.get("ear") and direction == "north":
                    paint_inner_ear(box)
                if cube.get("logo") and direction == "east":
                    paint_logo(box)
                if cube.get("belly") and direction == "north":
                    paint_belly(box)
            cubes.append({"origin": cube["origin"], "size": cube["size"], "uv": uv})
        if cubes:
            bone["cubes"] = cubes
        bones_json.append(bone)

    used_h = packer.y + packer.row_h
    assert used_h <= UV_H, f"texture too small, need {used_h} rows"
    print(identifier, "UV rows used:", used_h, "of", UV_H)
    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": identifier,
                "texture_width": UV_W,
                "texture_height": UV_H,
                "visible_bounds_width": 3,
                "visible_bounds_height": 2,
                "visible_bounds_offset": [0, 0.75, 0],
            },
            "bones": bones_json,
        }],
    }
    return geo, img


# ---------------------------------------------------------------------------------------------------
# Animations (names are referenced from GuhEntity.java)
# ---------------------------------------------------------------------------------------------------
animations = {
    "format_version": "1.8.0",
    "animations": {
        "animation.guh.idle": {
            "loop": True,
            "animation_length": 4.0,
            "bones": {
                "body": {"scale": {"0.0": [1, 1, 1], "2.0": [1.03, 1.05, 1.0], "4.0": [1, 1, 1]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "2.0": [-2, 0, 0], "4.0": [0, 0, 0]}},
                "ear_left": {"rotation": {"0.0": [0, 0, 0], "2.6": [0, 0, 0], "2.75": [0, 0, -14], "2.9": [0, 0, 0],
                                          "4.0": [0, 0, 0]}},
                "ear_right": {"rotation": {"0.0": [0, 0, 0], "3.1": [0, 0, 0], "3.25": [0, 0, 14], "3.4": [0, 0, 0],
                                           "4.0": [0, 0, 0]}},
                "tail": {"rotation": {"0.0": [0, -8, 0], "2.0": [0, 8, 0], "4.0": [0, -8, 0]}},
            },
        },
        "animation.guh.walk": {
            "loop": True,
            "animation_length": 0.6,
            "bones": {
                "root": {"position": {"0.0": [0, 0, 0], "0.15": [0, 0.8, 0], "0.3": [0, 0, 0], "0.45": [0, 0.8, 0],
                                      "0.6": [0, 0, 0]},
                         "rotation": {"0.0": [0, 0, -4], "0.3": [0, 0, 4], "0.6": [0, 0, -4]}},
                "body": {"scale": {"0.0": [1.03, 0.96, 1], "0.15": [0.98, 1.03, 1], "0.3": [1.03, 0.96, 1],
                                   "0.45": [0.98, 1.03, 1], "0.6": [1.03, 0.96, 1]}},
                "leg_front_left": {"rotation": {"0.0": [25, 0, 0], "0.3": [-25, 0, 0], "0.6": [25, 0, 0]}},
                "leg_front_right": {"rotation": {"0.0": [-25, 0, 0], "0.3": [25, 0, 0], "0.6": [-25, 0, 0]}},
                "leg_back_left": {"rotation": {"0.0": [-25, 0, 0], "0.3": [25, 0, 0], "0.6": [-25, 0, 0]}},
                "leg_back_right": {"rotation": {"0.0": [25, 0, 0], "0.3": [-25, 0, 0], "0.6": [25, 0, 0]}},
                "ear_left": {"rotation": {"0.0": [8, 0, 0], "0.3": [-8, 0, 0], "0.6": [8, 0, 0]}},
                "ear_right": {"rotation": {"0.0": [-8, 0, 0], "0.3": [8, 0, 0], "0.6": [-8, 0, 0]}},
                "tail": {"rotation": {"0.0": [0, -15, 0], "0.3": [0, 15, 0], "0.6": [0, -15, 0]}},
            },
        },
        "animation.guh.sit": {
            "loop": True,
            "animation_length": 3.0,
            "bones": {
                "root": {"position": {"0.0": [0, -0.5, 0]}},
                "body": {"scale": {"0.0": [1.06, 0.9, 1.02], "1.5": [1.08, 0.88, 1.02], "3.0": [1.06, 0.9, 1.02]}},
                "leg_front_left": {"position": {"0.0": [0, 0.6, 1]}},
                "leg_front_right": {"position": {"0.0": [0, 0.6, 1]}},
                "leg_back_left": {"position": {"0.0": [0, 0.6, -1]}},
                "leg_back_right": {"position": {"0.0": [0, 0.6, -1]}},
                "tail": {"rotation": {"0.0": [0, 35, 0]}},
            },
        },
        "animation.guh.happy": {
            "loop": False,
            "animation_length": 0.8,
            "bones": {
                "root": {"position": {"0.0": [0, 0, 0], "0.2": [0, 3, 0], "0.4": [0, 0, 0], "0.55": [0, 1.5, 0],
                                      "0.7": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.2": [0, 0, 10], "0.4": [0, 0, -10], "0.6": [0, 0, 6],
                                      "0.8": [0, 0, 0]}},
                "body": {"scale": {"0.0": [1, 1, 1], "0.1": [1.1, 0.85, 1.05], "0.25": [0.95, 1.08, 1],
                                   "0.4": [1.08, 0.9, 1.04], "0.8": [1, 1, 1]}},
            },
        },
    },
}

os.makedirs(os.path.join(ASSETS, "geckolib", "models", "entity"), exist_ok=True)
os.makedirs(os.path.join(ASSETS, "geckolib", "animations", "entity"), exist_ok=True)
os.makedirs(os.path.join(ASSETS, "textures", "entity"), exist_ok=True)
# The guh itself is now Lieke's model (tools/import_lieke_model.py / blockbench/guh.bbmodel), so only Mika is built here.
for name, cubes, fur, evil in (("mika", MIKA_CUBES, MIKA_FUR, True),):
    geo, texture = build("geometry." + name, cubes, fur, evil)
    with open(os.path.join(ASSETS, "geckolib", "models", "entity", name + ".geo.json"), "w") as f:
        json.dump(geo, f, indent=2)
    texture.save(os.path.join(ASSETS, "textures", "entity", name + ".png"))
geo, texture = build("geometry.guh_sitting", SITTING_CUBES, FUR, bones=SITTING_BONES, lieke=True)
with open(os.path.join(ASSETS, "geckolib", "models", "entity", "guh_sitting.geo.json"), "w") as f:
    json.dump(geo, f, indent=2)
texture.save(os.path.join(ASSETS, "textures", "entity", "guh_sitting.png"))
sitting_animations = {
    "format_version": "1.8.0",
    "animations": {
        "animation.guh_sitting.idle": {
            "loop": True, "animation_length": 4.0,
            "bones": {
                "body": {"scale": {"0.0": [1, 1, 1], "2.0": [1.03, 1.04, 1.03], "4.0": [1, 1, 1]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 5], "2.0": [0, 0, 0], "3.0": [0, 0, -5], "4.0": [0, 0, 0]}},
                "arm_left": {"rotation": {"0.0": [0, 0, 0], "0.5": [-20, 0, 0], "1.0": [0, 0, 0], "4.0": [0, 0, 0]}},
                "arm_right": {"rotation": {"0.0": [0, 0, 0], "0.6": [-20, 0, 0], "1.1": [0, 0, 0], "4.0": [0, 0, 0]}},
                "ear_left": {"rotation": {"0.0": [0, 0, 0], "2.6": [0, 0, 0], "2.75": [0, 0, -14], "2.9": [0, 0, 0], "4.0": [0, 0, 0]}},
                "ear_right": {"rotation": {"0.0": [0, 0, 0], "3.1": [0, 0, 0], "3.25": [0, 0, 14], "3.4": [0, 0, 0], "4.0": [0, 0, 0]}},
                "tail": {"rotation": {"0.0": [0, -10, 0], "2.0": [0, 10, 0], "4.0": [0, -10, 0]}},
            },
        },
        "animation.guh_sitting.happy": {
            "loop": False, "animation_length": 0.8,
            "bones": {
                "root": {"position": {"0.0": [0, 0, 0], "0.2": [0, 4, 0], "0.4": [0, 0, 0], "0.55": [0, 2, 0], "0.7": [0, 0, 0]}},
                "arm_left": {"rotation": {"0.0": [0, 0, 0], "0.2": [-80, 0, 0], "0.6": [-80, 0, 0], "0.8": [0, 0, 0]}},
                "arm_right": {"rotation": {"0.0": [0, 0, 0], "0.2": [-80, 0, 0], "0.6": [-80, 0, 0], "0.8": [0, 0, 0]}},
            },
        },
    },
}
with open(os.path.join(ASSETS, "geckolib", "animations", "entity", "guh_sitting.animation.json"), "w") as f:
    json.dump(sitting_animations, f, indent=2)
# Mika re-uses the guh animations (same bone names)
with open(os.path.join(ASSETS, "geckolib", "animations", "entity", "guh.animation.json"), "w") as f:
    json.dump(animations, f, indent=2)
