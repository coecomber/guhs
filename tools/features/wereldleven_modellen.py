"""
Het guhleven (2.8, wereldleven) - models: the 21 plushies (a little sitting guh plush, with the variant's own bits: a long
neck, a crown, a cloud, a fish tail, wings...), the guh-xylofoon, the grijpmachine (two halves + the claw) and IJscoguh
Tingeling on his ice-cream bike (a GeckoLib model built from the sitting guh, plus the bike, the ice-cream box with three
ice creams on its lid, a parasol and a bell; its texture = the sitting guh's + painted swatches in the free UV space).
"""
import copy
import json
import os
import random

import numpy as np
from PIL import Image

from features import wereldleven_tex as tex

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
ALL = ("down", "up", "north", "south", "west", "east")


def el(frm, to, tex_, faces=ALL, uv=None, rot=None, face_uv=None):
    """A model element; uv: one uv rect for all faces, face_uv: {face: uv}."""
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


# =====================================================================================================================
# the plushies
# =====================================================================================================================
FUR, FACE, OOR, BUIK, EXTRA_A, EXTRA_B, POOT, NAAD = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 4, 12], [4, 8, 8, 12], [8, 8, 12, 12], \
    [12, 8, 16, 12], [0, 12, 4, 16], [4, 12, 8, 16]


def knuffel_elements(vid):
    """A little plush guh sitting up, its face to the north (the blockstate turns it)."""
    t = "#t"
    head_y = 4 if vid != "brontosaurus" else 8
    body_to_z = 13 if vid != "teckel" else 15.5
    bx0, bx1 = (4, 12) if vid != "mager" else (5, 11)
    els = [
        el([bx0, 0, 6], [bx1, 6, body_to_z], t, uv=FUR, face_uv={"north": BUIK}),                    # body
        el([3.5, head_y, 3], [12.5, head_y + 7, 10], t, uv=FUR, face_uv={"north": FACE}),            # head
        el([3, head_y + 5.5, 6], [6, head_y + 8.5, 7], t, uv=FUR, face_uv={"north": OOR}),           # ears
        el([10, head_y + 5.5, 6], [13, head_y + 8.5, 7], t, uv=FUR, face_uv={"north": OOR}),
        el([4.5, 0, 4], [7, 1.5, 6], t, uv=POOT),                                                     # front paws
        el([9, 0, 4], [11.5, 1.5, 6], t, uv=POOT),
        el([6.5, 3, 2.5], [9.5, 4.5, 3.5], t, uv=POOT) if vid != "brontosaurus" else None,          # arms holding the tummy
    ]
    if vid == "zeemeerguh":
        els += [el([6.5, 0.5, body_to_z], [9.5, 3, body_to_z + 2.5], t, uv=EXTRA_A),
                el([5, 0, body_to_z + 2], [11, 1, body_to_z + 3.5], t, uv=EXTRA_B)]
    else:
        els.append(el([7.5, 1, body_to_z], [8.5, 2, body_to_z + 2.5], t, uv=POOT))                 # tail
    if vid == "brontosaurus":
        els += [el([6, 4.5, 5], [10, 9, 8.5], t, uv=EXTRA_B), el([6.2, 6, 4.8], [9.8, 7, 8.7], t, uv=EXTRA_A)]
    if vid in ("ender", "vahoege_ender"):
        els += [el([0.5, 5, 8], [4, 10, 8.8], t, uv=EXTRA_A, rot={"origin": [4, 7, 8], "axis": "y", "angle": 22.5}),
                el([12, 5, 8], [15.5, 10, 8.8], t, uv=EXTRA_A, rot={"origin": [12, 7, 8], "axis": "y", "angle": -22.5})]
    if vid == "koning":
        els += [el([5, head_y + 7, 4.5], [11, head_y + 8.5, 8.5], t, uv=EXTRA_A),
                el([5, head_y + 8.5, 4.5], [6, head_y + 9.5, 5.5], t, uv=EXTRA_A), el([10, head_y + 8.5, 4.5], [11, head_y + 9.5, 5.5], t, uv=EXTRA_A),
                el([7.5, head_y + 8.5, 4.5], [8.5, head_y + 10, 5.5], t, uv=EXTRA_A),
                el([3, head_y - 1, 9], [13, head_y + 5, 10.5], t, uv=EXTRA_B)]                          # the mane
    if vid == "wolk":
        els += [el([4.5, head_y + 7, 4], [11.5, head_y + 9, 9], t, uv=EXTRA_A), el([6, head_y + 9, 5], [10, head_y + 10, 8], t, uv=EXTRA_A)]
    if vid == "pluisguh":
        els += [el([6, head_y + 7, 4.5], [10, head_y + 9, 7.5], t, uv=EXTRA_A), el([7, head_y + 9, 5], [9, head_y + 10, 6.5], t, uv=EXTRA_A),
                el([2.5, head_y + 0.5, 3.5], [3.5, head_y + 3.5, 6], t, uv=EXTRA_B), el([12.5, head_y + 0.5, 3.5], [13.5, head_y + 3.5, 6], t, uv=EXTRA_B)]
    if vid == "glitter":
        els += [el([7, head_y + 7, 5.5], [9, head_y + 9, 7.5], t, uv=EXTRA_A, rot={"origin": [8, head_y + 8, 6.5], "axis": "z", "angle": 45}),
                el([2.5, 3, 7], [3.5, 4, 8], t, uv=EXTRA_B), el([12.5, 6, 8], [13.5, 7, 9], t, uv=EXTRA_B)]
    if vid == "brococolief":
        els += [el([3.3, head_y + 5, 3.8], [12.7, head_y + 7.5, 10.2], t, uv=EXTRA_A)]                # the onesie's hood rim
    if vid == "golden":
        els += [el([7.2, head_y + 7, 6], [8.8, head_y + 8.5, 7.5], t, uv=EXTRA_A)]                    # a tiny gold tuft
    return [e for e in els if e]


def knuffel_models(h, ids):
    A, w = h.A, h.w
    for vid in ids:
        name = f"knuffel_{vid}"
        model = {"parent": "minecraft:block/block", "textures": {"particle": f"guhs:block/{name}", "t": f"guhs:block/{name}"},
                 "elements": knuffel_elements(vid),
                 "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 1.5, 0], "scale": [0.95, 0.95, 0.95]},
                             "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                             "fixed": {"rotation": [0, 180, 0], "scale": [0.8, 0.8, 0.8]},
                             "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5, 0.5, 0.5]},
                             "firstperson_righthand": {"rotation": [0, 135, 0], "scale": [0.5, 0.5, 0.5]}}}
        if vid == "ghost":
            model["render_type"] = "minecraft:translucent"
        w(f"{A}/models/block/{name}.json", model)
        w(f"{A}/blockstates/{name}.json", {"variants": {f"facing={f}": {"model": f"guhs:block/{name}", **({"y": r} if r else {})}
                                                        for f, r in ROT.items()}})
        w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})


# =====================================================================================================================
# the guh-xylofoon (facing: the side the player stands on; bars low -> high from the player's left to right)
# =====================================================================================================================
def xylofoon_model(h):
    A, w = h.A, h.w
    els = []
    for (x, z) in ((1.5, 3), (13, 3), (1.5, 11.5), (13, 11.5)):                              # legs
        els.append(el([x, 0, z], [x + 1.5, 6.5, z + 1.5], "#hout"))
    els.append(el([1, 6.5, 2.5], [15, 8, 4], "#hout"))                                            # front rail
    els.append(el([1, 6.5, 12], [15, 8, 13.5], "#hout"))                                          # back rail
    els.append(el([5, 1.5, 2.2], [11, 6.5, 2.7], "#gezicht", faces=("north", "south"), face_uv={"north": [0, 0, 16, 16]}))
    els.append(el([4, 2, 2.6], [12, 3, 12.6], "#hout", faces=("up", "down", "west", "east")))       # the shelf between the legs
    for i in range(8):
        x1 = 14.4 - i * 1.7                                   # (the player stands north: their left is east = high x)
        half = 6.2 - i * 0.45
        els.append(el([x1 - 1.4, 8, 8 - half], [x1, 9, 8 + half], f"#staaf{i}", uv=[0, 0, 16, 16]))
    # two mallets resting on the front rail: a stick with a knabbel on top
    els.append(el([1.5, 8, 1.2], [6, 8.6, 1.8], "#hamer", uv=[0, 8, 16, 16]))
    els.append(el([5.5, 7.8, 0.9], [7, 9.2, 2.1], "#hamer", uv=[0, 0, 8, 6]))
    els.append(el([10, 8, 1.2], [14.5, 8.6, 1.8], "#hamer", uv=[0, 8, 16, 16]))
    els.append(el([9, 7.8, 0.9], [10.5, 9.2, 2.1], "#hamer", uv=[0, 0, 8, 6]))
    textures = {"particle": "guhs:block/guh_xylofoon_hout", "hout": "guhs:block/guh_xylofoon_hout", "gezicht": "guhs:block/guh_xylofoon_gezicht",
                "hamer": "guhs:block/guh_xylofoon_hamer", **{f"staaf{i}": f"guhs:block/guh_xylofoon_staaf_{i}" for i in range(8)}}
    w(f"{A}/models/block/guh_xylofoon.json", {"parent": "minecraft:block/block", "textures": textures, "elements": els})
    w(f"{A}/blockstates/guh_xylofoon.json", {"variants": {f"facing={f}": {"model": "guhs:block/guh_xylofoon", **({"y": r} if r else {})}
                                                          for f, r in ROT.items()}})
    w(f"{A}/models/item/guh_xylofoon.json", {"parent": "guhs:block/guh_xylofoon"})


# =====================================================================================================================
# the grijpmachine (front = north)
# =====================================================================================================================
def grijpmachine_models(h):
    A, w = h.A, h.w
    tx = {"particle": "guhs:block/grijpmachine_kast", "kast": "guhs:block/grijpmachine_kast", "voor": "guhs:block/grijpmachine_voorkant",
          "paneel": "guhs:block/grijpmachine_paneel", "glas": "guhs:block/grijpmachine_glas", "frame": "guhs:block/grijpmachine_frame",
          "bord": "guhs:block/grijpmachine_bord", "rood": "guhs:block/grijpmachine_rood", "klauw": "guhs:block/grijpmachine_klauw"}
    onder = [
        el([0.5, 0, 0.5], [15.5, 1, 15.5], "#frame"),                                              # the gold foot
        el([1, 1, 1], [15, 14, 15], "#kast", face_uv={"north": [0, 0, 16, 16]}, uv=[0, 0, 16, 16]),
        el([1, 1, 0.98], [15, 14, 1], "#voor", faces=("north",), uv=[0, 0, 16, 16]),               # the chute and coin slot
        el([1, 12, 0], [15, 14.5, 4], "#paneel", face_uv={"north": [0, 0, 16, 3], "up": [0, 0, 16, 16]}),   # the control shelf
        el([12, 14.5, 1.5], [12.8, 17, 2.3], "#klauw"),                                              # the joystick
        el([11.4, 17, 0.9], [13.4, 19, 2.9], "#rood"),
        el([5, 14.5, 1.3], [7, 15.2, 3.3], "#rood"),                                                 # the big button
        el([2.5, 14.5, 1.3], [4, 15, 2.8], "#frame"),
        el([1, 14, 1], [15, 16, 15], "#frame", faces=("up", "north", "south", "west", "east")),    # the case's floor frame
    ]
    boven = [
        el([1, 0, 1], [2, 12, 2], "#frame"), el([14, 0, 1], [15, 12, 2], "#frame"),                  # posts
        el([1, 0, 14], [2, 12, 15], "#frame"), el([14, 0, 14], [15, 12, 15], "#frame"),
        el([2, 0, 1.3], [14, 12, 1.5], "#glas", faces=("north", "south"), uv=[0, 0, 16, 16]),       # glass
        el([2, 0, 14.5], [14, 12, 14.7], "#glas", faces=("north", "south"), uv=[0, 0, 16, 16]),
        el([1.3, 0, 2], [1.5, 12, 14], "#glas", faces=("west", "east"), uv=[0, 0, 16, 16]),
        el([14.5, 0, 2], [14.7, 12, 14], "#glas", faces=("west", "east"), uv=[0, 0, 16, 16]),
        el([0, 12, 0], [16, 16, 16], "#kast", face_uv={"north": [0, 0, 16, 16]}),                   # the sign box
        el([0, 12, -0.02], [16, 16, 0], "#bord", faces=("north",), uv=[0, 0, 16, 16]),
        el([0, 12, 16], [16, 16, 16.02], "#bord", faces=("south",), uv=[0, 0, 16, 16]),
        el([6, 16, 6], [10, 17.5, 10], "#rood"),                                                     # a light on top
    ]
    w(f"{A}/models/block/grijpmachine_onder.json", {"parent": "minecraft:block/block", "textures": tx, "elements": onder})
    w(f"{A}/models/block/grijpmachine_boven.json", {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
                                                    "textures": tx, "elements": boven})
    klauw = [
        el([7.6, 12, 7.6], [8.4, 16, 8.4], "#klauw"),                                                # the rope/rod
        el([6.5, 10.5, 6.5], [9.5, 12, 9.5], "#klauw"),                                              # the head
        el([6.2, 8, 7.5], [7.2, 10.8, 8.5], "#klauw", rot={"origin": [6.7, 10.5, 8], "axis": "z", "angle": -22.5}),
        el([8.8, 8, 7.5], [9.8, 10.8, 8.5], "#klauw", rot={"origin": [9.3, 10.5, 8], "axis": "z", "angle": 22.5}),
        el([7.5, 8, 8.8], [8.5, 10.8, 9.8], "#klauw", rot={"origin": [8, 10.5, 9.3], "axis": "x", "angle": 22.5}),
        el([7.5, 8, 6.2], [8.5, 10.8, 7.2], "#klauw", rot={"origin": [8, 10.5, 6.7], "axis": "x", "angle": -22.5}),
    ]
    w(f"{A}/models/block/grijpmachine_klauw.json", {"parent": "minecraft:block/block", "textures": {"particle": tx["klauw"], "klauw": tx["klauw"]},
                                                    "elements": klauw})
    w(f"{A}/blockstates/grijpmachine.json", {"variants": {
        f"facing={f},half={half}": {"model": f"guhs:block/grijpmachine_{'onder' if half == 'lower' else 'boven'}", **({"y": r} if r else {})}
        for f, r in ROT.items() for half in ("lower", "upper")}})
    h.item_model("grijpmachine")


# =====================================================================================================================
# IJscoguh Tingeling on his ice-cream bike (GeckoLib)
# =====================================================================================================================
GUH_OFFSET = (0, 9, 10)          # the sitting guh on the saddle, behind the ice-cream box


def ijscoguh(h):
    import make_guh_variants as mgv
    A = h.A
    src = json.load(open(f"{A}/geo/entity/guh_sitting.geo.json", encoding="utf-8"))
    geo = copy.deepcopy(src["minecraft:geometry"][0])
    ox, oy, oz = GUH_OFFSET
    for bone in geo["bones"]:
        if "pivot" in bone:
            bone["pivot"] = [bone["pivot"][0] + ox, bone["pivot"][1] + oy, bone["pivot"][2] + oz]
        for c in bone.get("cubes", []):
            c["origin"] = [c["origin"][0] + ox, c["origin"][1] + oy, c["origin"][2] + oz]
            if "pivot" in c:
                c["pivot"] = [c["pivot"][0] + ox, c["pivot"][1] + oy, c["pivot"][2] + oz]
    used = mgv.used_map(geo)
    swatches = {}

    def sw(name):
        if name not in swatches:
            swatches[name] = mgv.free_swatch(used)
        u, v = swatches[name]
        return {"uv": [u, v], "uv_size": [8, 8]}

    def cube(origin, size, swatch, sides=None, pivot=None, rotation=None):
        faces = {d: sw((sides or {}).get(d, swatch)) for d in ALL}
        c = {"origin": origin, "size": size, "uv": faces}
        if pivot:
            c["pivot"] = pivot
        if rotation:
            c["rotation"] = rotation
        return c

    def bone(name, parent, pivot, cubes):
        b = {"name": name, "pivot": pivot, "cubes": cubes}
        if parent:
            b["parent"] = parent
        geo["bones"].append(b)

    bone("fiets", "root", [0, 0, 0], [
        cube([-3, 7, 7], [6, 2, 7], "zadel"),                                    # the saddle
        cube([-0.5, 3, 9.5], [1, 4, 1], "frame"),                                # seat post
        cube([-0.5, 5, -8], [1, 1, 18], "frame"),                                # down tube to the box
        cube([-0.5, 17, -8], [1, 1, 10], "frame"),                               # the stem to the handlebar
        cube([-0.5, 17, 1], [1, 5, 1], "frame"),
        cube([-6, 21, 1], [12, 1, 1], "frame"),                                  # the handlebar
        cube([-7.5, 20.7, 0.7], [2, 1.6, 1.6], "handvat"), cube([5.5, 20.7, 0.7], [2, 1.6, 1.6], "handvat"),
        cube([-10, 4, -16.5], [20, 1, 1], "frame"),                              # the front axle
    ])
    bone("bak", "fiets", [0, 6, -16], [
        cube([-8, 6, -24], [16, 14, 16], "bak", sides={"north": "bak_voor", "up": "deksel", "down": "deksel"}),
        cube([-8.5, 20, -24.5], [17, 1, 17], "deksel"),
        # three ice creams sticking out of the lid
        cube([-6, 21, -22], [2, 3, 2], "hoorn"), cube([-6.5, 24, -22.5], [3, 2.5, 3], "bol_roze"),
        cube([4, 21, -22], [2, 3, 2], "hoorn"), cube([3.5, 24, -22.5], [3, 2.5, 3], "bol_mint"),
        cube([-1, 21, -12], [2, 3, 2], "hoorn"), cube([-1.5, 24, -12.5], [3, 2.5, 3], "bol_choco"),
    ])
    wheel_sides = {"east": "wiel", "west": "wiel"}
    bone("wiel_links", "fiets", [9, 4.5, -16], [cube([8, 0, -20.5], [2, 9, 9], "band", sides=wheel_sides)])
    bone("wiel_rechts", "fiets", [-9, 4.5, -16], [cube([-10, 0, -20.5], [2, 9, 9], "band", sides=wheel_sides)])
    bone("wiel_achter", "fiets", [0, 4, 10], [cube([-1, 0, 6], [2, 8, 8], "band", sides=wheel_sides)])
    bone("bel", "fiets", [5, 22, 1.5], [cube([4, 22, 0.5], [2, 1.5, 2], "bel"), cube([4.6, 23.5, 1.1], [0.8, 0.6, 0.8], "bel")])
    bone("parasol", "bak", [0, 21, -16], [
        cube([-0.5, 21, -16.5], [1, 23, 1], "paal"),
        cube([-13, 43, -29], [26, 1, 26], "parasol"),
        cube([-10, 44, -26], [20, 1.5, 20], "parasol"),
        cube([-6, 45.5, -22], [12, 1.5, 12], "parasol"),
        cube([-1, 47, -17], [2, 1.5, 2], "handvat"),
    ])
    # his white ice-cream cap with a pink band (on the head: the sitting guh's head top is y 24.5)
    hx, hy, hz = 0 + ox, 25.9 + oy, 0 + oz
    bone("ijscopet", "head", [0 + ox, 13 + oy, 0 + oz], [
        cube([hx - 5.5, hy, hz - 4.5], [11, 2.6, 9], "pet"),
        cube([hx - 5.6, hy, hz - 4.6], [11.2, 0.9, 9.2], "pet_band"),
        cube([hx - 5.5, hy, hz - 8.5], [11, 0.5, 4], "pet"),
    ])
    geo["description"] = {"identifier": "geometry.ijscoguh", "texture_width": 128, "texture_height": 128,
                          "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]}
    h.w(f"{A}/geo/entity/ijscoguh.geo.json", {"format_version": src.get("format_version", "1.12.0"), "minecraft:geometry": [geo]})
    # the texture: the sitting guh's, with the swatches painted in
    base = np.asarray(Image.open(f"{A}/textures/entity/guh_sitting.png").convert("RGBA")).copy()
    rng = random.Random(28808)
    for name, (u, v) in swatches.items():
        base[v * 4:(v + 8) * 4, u * 4:(u + 8) * 4] = tex.bike_swatch(name, rng)
    Image.fromarray(base).save(f"{A}/textures/entity/ijscoguh.png")
    # the animation: breathing and a little head sway (the wheels, feet and bell are turned in IJscoguhRenderer)
    anim = json.load(open(f"{A}/animations/entity/guh_sitting.animation.json", encoding="utf-8"))
    idle = next(iter(anim["animations"].values()))
    h.w(f"{A}/animations/entity/ijscoguh.animation.json", {"format_version": anim.get("format_version", "1.8.0"),
                                                           "animations": {"animation.ijscoguh.idle": idle}})
    return swatches


# =====================================================================================================================
# the grijpmachine piece of the Knuffeldal plein (slot "grijpmachine": 5 x 6 x 5, jigsaw (2, 0, 2) down_south)
# =====================================================================================================================
def grijpmachine_stuk(h):
    s = h.Structure((5, 6, 5))
    for x in range(1, 4):
        for z in range(1, 4):
            s.set(x, 0, z, "guhs:knuffelsteen")
    for (x, z) in ((1, 1), (3, 1), (1, 3), (3, 3)):
        s.set(x, 0, z, "guhs:knuffelklinkers")
    s.set(2, 0, 2, "minecraft:jigsaw", {"orientation": "down_south"},
          {"id": "minecraft:jigsaw", "name": "guhs:grijpmachine_voet", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": "guhs:knuffelklinkers", "joint": "aligned", "placement_priority": 0, "selection_priority": 0})
    s.set(2, 1, 2, "guhs:grijpmachine", {"facing": "south", "half": "lower"})
    s.set(2, 2, 2, "guhs:grijpmachine", {"facing": "south", "half": "upper"})
    # two plushies on the pedestal's front corners, looking at the plein, and a lantern on each back corner
    s.set(1, 1, 3, "guhs:knuffel_normal", {"facing": "south"})
    s.set(3, 1, 3, "guhs:knuffel_pluisguh", {"facing": "south"})
    for x in (1, 3):
        s.set(x, 1, 1, "guhs:knuffelsteen_muur", {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none",
                                                   "waterlogged": "false"})
        s.set(x, 2, 1, "guhs:lampion_roze" if x == 1 else "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})
    check_stuk(s)
    s.save("knuffeldal_stadje/grijpmachine")
    return s


def check_stuk(s):
    problems = []
    if s.size[0] > 5 or s.size[1] > 6 or s.size[2] > 5:
        problems.append(f"too big: {s.size}")
    if s.get(2, 0, 2) != "minecraft:jigsaw":
        problems.append("no jigsaw at (2, 0, 2)")
    lower, upper = s.blocks.get((2, 1, 2)), s.blocks.get((2, 2, 2))
    if not lower or lower[0] != "guhs:grijpmachine" or lower[1].get("half") != "lower" or not upper or upper[0] != "guhs:grijpmachine" \
            or upper[1].get("half") != "upper" or lower[1].get("facing") != upper[1].get("facing"):
        problems.append("the grijpmachine isn't two matching halves at (2, 1..2, 2)")
    for (x, y, z), (b, _, _) in s.blocks.items():
        if b != "minecraft:air" and y > 0 and s.get(x, y - 1, z) in (None, "minecraft:air"):
            problems.append(f"a floating {b} at {(x, y, z)}")
    front = [(x, 1, 4) for x in range(5)]
    if any(s.get(*p) not in (None, "minecraft:air") for p in front):
        problems.append("the front row (z 4) must stay free: the player stands there")
    if problems:
        raise SystemExit("grijpmachine piece check failed:\n  " + "\n  ".join(problems))
