"""
bbq2 (ring-h5) - the looks of chapter 5: GeckoLib models, animations and textures of its two creatures, and the textures and
models of its three blocks.

Het Oog van Sausron (entity guhs:oog_van_sausron): ONE huge Mika eye, drawn 4.6 times as big as it is modelled (the renderer
scales it; it fills the socket of the Mika head on the tower: 8 blocks wide, 4 high). An almond of fire (rows of slabs,
painted as one iris: white-hot in the middle, yellow, orange, a deep red rim), the Mika's slit pupil in front of it, a glint,
and two charcoal lids that close from above and below. Bones: root > kijk (a scene's pose) > oog (turned by the renderer like
a head) > bol, pupil, glans, lid_boven, lid_onder. Animations: waak (a slow pulse and a blink), zoek (wide, the pupil a
thread), slaap (lids shut, breathing), tevreden (a pleased squint). oog_van_sausron_glowmask.png lights the fire.

De Roosterwachter (entity guhs:ringh5_roosterwachter): the Mika's model in ash-grey with an iron helmet (his ears stick out
through it, a crest on top), a grate for a visor and a grill fork over his shoulder.

Blocks: ringh5_poortrooster (the grate of the gate: a cutout cube), ringh5_blik (the light of the Eye on the ground: a thin
see-through plate with a glowing eye on it, only ever shown by a block display), oog_van_sausron_beeldje (the statuette: a
little tower with the Mika head; its eye is an animated texture that blinks).

  build(h)       writes everything
  check(h)       the bones the Java side and the animations name exist
  preview(out)   (python tools/features/ring_h5_modellen.py <out>) offline renders
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

try:
    from features import boerderij_dieren as bd
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import boerderij_dieren as bd
from features import knuffeldal_npcs as kn

Atlas, plain, cube, bone, geo, transparent = bd.Atlas, bd.plain, bd.cube, bd.bone, bd.geo, bd.transparent
S = bd.S
SEED = 21302060

# the almond: rows of two pixels high, from the bottom up: (width, depth)
RIJEN = [(10, 3), (18, 4), (24, 4), (28, 5), (24, 4), (18, 4), (10, 3)]
HOOG = len(RIJEN) * 2            # 14 pixels
BREED = 28
MIDDEN_Y = HOOG / 2.0            # the pivot of the eye
LID = (30, 30, 34)
LID_GLOED = (214, 88, 30)


# =====================================================================================================================
# the Eye
# =====================================================================================================================
def _iris_kleur(d, hoek, rng_val):
    """The fire of the iris at distance d (0 middle .. 1 rim) of the almond."""
    vlam = 0.06 * math.sin(hoek * 9.0) + 0.04 * math.sin(hoek * 17.0 + 1.3) + (rng_val - 0.5) * 0.08
    d = d + vlam
    if d < 0.16:
        return (255, 250, 214)
    if d < 0.34:
        return (255, 226, 110)
    if d < 0.56:
        return (255, 176, 44)
    if d < 0.78:
        return (244, 112, 26)
    if d < 0.93:
        return (206, 52, 18)
    return (96, 18, 14)


def iris(x0, y0):
    """The painter of a front (or back) face whose left bottom corner is at (x0, y0) pixels of the almond."""
    def paint(W, H, rng):
        a = np.zeros((H, W, 4), np.uint8)
        ruis = rng.random((H, W))
        for py in range(H):
            for px in range(W):
                gx = x0 + (px + 0.5) / S
                gy = y0 + (H - py - 0.5) / S
                nx, ny = gx / (BREED / 2.0), (gy - MIDDEN_Y) / (HOOG / 2.0)
                d = math.sqrt(nx * nx + ny * ny * 1.15)
                a[py, px, :3] = _iris_kleur(d, math.atan2(ny, nx), ruis[py, px])
                a[py, px, 3] = 255
        return a
    return paint


def rand_vuur(W, H, rng):
    """The top, bottom and side faces of the almond: the deep red of its rim with glowing cracks."""
    a = np.zeros((H, W, 4), np.float32)
    a[..., :3] = (150, 34, 16)
    a[..., :3] += rng.normal(0, 10, (H, W, 1))
    for _ in range(max(1, W * H // 90)):
        x, y = int(rng.uniform(0, W)), int(rng.uniform(0, H))
        a[max(0, y - 1):y + 1, max(0, x - 2):x + 3, :3] = (250, 150, 40)
    a[..., 3] = 255
    return np.clip(a, 0, 255).astype(np.uint8)


def pupil_p(W, H, rng):
    a = np.zeros((H, W, 4), np.float32)
    a[..., :3] = (14, 8, 16)
    a[..., :3] += rng.normal(0, 3, (H, W, 1))
    a[..., 3] = 255
    return np.clip(a, 0, 255).astype(np.uint8)


def lid_p(W, H, rng):
    """Charcoal skin with glowing cracks."""
    a = np.zeros((H, W, 4), np.float32)
    a[..., :3] = LID
    a[..., :3] += rng.normal(0, 5, (H, W, 1))
    for i in range(max(1, W // 14)):
        x = int(rng.uniform(2, max(3, W - 2)))
        for y in range(H):
            if rng.random() < 0.8:
                xx = int(min(W - 1, max(0, x + math.sin(y * 0.5 + i) * 2)))
                a[y, xx, :3] = LID_GLOED
    a[..., 3] = 255
    return np.clip(a, 0, 255).astype(np.uint8)


def wit(W, H, rng):
    a = np.zeros((H, W, 4), np.uint8)
    a[...] = (255, 255, 240, 255)
    return a


def oog(atlas, gloei=False):
    """With gloei=True the same atlas layout painted for the glowmask (only what burns)."""
    def p(painter, brandt):
        return painter if (brandt or not gloei) else transparent

    bol = []
    for i, (breed, diep) in enumerate(RIJEN):
        y = i * 2
        x0 = -breed / 2.0
        bol.append(cube(atlas, [x0, y, -diep / 2.0], [breed, 2, diep], p(rand_vuur, True),
                        overrides={"north": p(iris(x0, y), True), "south": p(iris(x0, y), True)}))
    pupil = [cube(atlas, [-0.75, 1.5, -3.3], [1.5, 11, 0.8], p(pupil_p, False)),
             cube(atlas, [-1.25, 3.5, -3.4], [2.5, 7, 0.8], p(pupil_p, False)),
             cube(atlas, [-1.75, 5.5, -3.5], [3.5, 3, 0.8], p(pupil_p, False))]
    glans = [cube(atlas, [2.6, 9.0, -3.6], [1.6, 1.6, 0.6], p(wit, True)), cube(atlas, [4.6, 8.0, -3.4], [0.8, 0.8, 0.5], p(wit, True))]
    lid_boven = [cube(atlas, [-15, HOOG, -4.0], [30, 8, 1.2], p(lid_p, False)), cube(atlas, [-15, HOOG - 0.6, -4.2], [30, 0.6, 1.6], p(plain(LID_GLOED, 12), True))]
    lid_onder = [cube(atlas, [-15, -8, -4.0], [30, 8, 1.2], p(lid_p, False)), cube(atlas, [-15, 0, -4.2], [30, 0.6, 1.6], p(plain(LID_GLOED, 12), True))]
    bones = [bone("root", None, [0, 0, 0], []),
             bone("kijk", "root", [0, MIDDEN_Y, 0], []),
             bone("oog", "kijk", [0, MIDDEN_Y, 0], []),
             bone("bol", "oog", [0, MIDDEN_Y, 0], bol),
             bone("pupil", "oog", [0, MIDDEN_Y, -3], pupil),
             bone("glans", "oog", [0, MIDDEN_Y, -3], glans),
             bone("lid_boven", "oog", [0, HOOG, -4], lid_boven),
             bone("lid_onder", "oog", [0, 0, -4], lid_onder)]
    return geo("oog_van_sausron", bones, 3.0, 3.0)


def kf(pairs):
    return {str(round(t, 3)): list(v) for t, v in pairs}


def anim(length, bones, loop=True):
    return {"loop": loop, "animation_length": length, "bones": bones}


DICHT = HOOG / 2.0 + 0.2         # how far a lid travels to shut the eye


def oog_anims():
    waak = {"oog": {"scale": kf([(0, [1, 1, 1]), (2.5, [1.03, 1.04, 1]), (5.0, [1, 1, 1])])},
            "pupil": {"scale": kf([(0, [1, 1, 1]), (1.6, [0.8, 1.04, 1]), (3.4, [1.15, 0.98, 1]), (5.0, [1, 1, 1])])},
            "lid_boven": {"position": kf([(0, [0, 0, 0]), (4.4, [0, 0, 0]), (4.55, [0, -DICHT, 0]), (4.7, [0, -DICHT, 0]), (4.9, [0, 0, 0]), (5.0, [0, 0, 0])])},
            "lid_onder": {"position": kf([(0, [0, 0, 0]), (4.4, [0, 0, 0]), (4.55, [0, DICHT, 0]), (4.7, [0, DICHT, 0]), (4.9, [0, 0, 0]), (5.0, [0, 0, 0])])}}
    zoek = {"oog": {"scale": kf([(0, [1.08, 1.1, 1]), (0.3, [1.13, 1.16, 1]), (0.6, [1.08, 1.1, 1])])},
            "pupil": {"scale": kf([(0, [0.45, 1.12, 1]), (0.3, [0.36, 1.16, 1]), (0.6, [0.45, 1.12, 1])])},
            "lid_boven": {"position": kf([(0, [0, 1.0, 0])])}, "lid_onder": {"position": kf([(0, [0, -1.0, 0])])}}
    slaap = {"oog": {"scale": kf([(0, [1, 1, 1]), (2.0, [1.02, 1.03, 1]), (4.0, [1, 1, 1])])},
             "lid_boven": {"position": kf([(0, [0, -DICHT, 0]), (2.0, [0, -DICHT + 0.4, 0]), (4.0, [0, -DICHT, 0])])},
             "lid_onder": {"position": kf([(0, [0, DICHT, 0])])}}
    tevreden = {"oog": {"scale": kf([(0, [1.02, 1, 1]), (0.4, [1.05, 0.98, 1]), (0.8, [1.02, 1, 1])])},
                "pupil": {"scale": kf([(0, [1.35, 0.95, 1])])},
                "lid_boven": {"position": kf([(0, [0, -DICHT * 0.55, 0]), (0.4, [0, -DICHT * 0.62, 0]), (0.8, [0, -DICHT * 0.55, 0])])},
                "lid_onder": {"position": kf([(0, [0, DICHT * 0.5, 0])])}}
    return {"format_version": "1.8.0", "animations": {
        "animation.oog_van_sausron.waak": anim(5.0, waak), "animation.oog_van_sausron.zoek": anim(0.6, zoek),
        "animation.oog_van_sausron.slaap": anim(4.0, slaap), "animation.oog_van_sausron.tevreden": anim(0.8, tevreden)}}


def maak_oog():
    """In memory: (geo json, animation json, {texture name: RGBA array})."""
    atlas = Atlas(SEED)
    model = oog(atlas)
    tex = {"oog_van_sausron": atlas.img}
    atlas = Atlas(SEED)
    oog(atlas, gloei=True)
    tex["oog_van_sausron_glowmask"] = atlas.img
    return model, oog_anims(), tex


# =====================================================================================================================
# the Roosterwachter
# =====================================================================================================================
WACHTER = "ringh5_roosterwachter"


def _ijzer(licht=26):
    def p(block):
        block[:4, :, :3] = np.clip(block[:4, :, :3].astype(np.int32) + licht, 0, 255).astype(np.uint8)
        block[-4:, :, :3] = (block[-4:, :, :3] * 0.72).astype(np.uint8)
        for x in (5, 26):
            for y in (9, 22):
                block[y:y + 3, x:x + 3, :3] = (26, 24, 30)           # rivets
    return p


def _hout(block):
    for x in range(0, 32, 5):
        block[:, x, :3] = (block[:, x, :3] * 0.78).astype(np.uint8)


def wachter(h):
    """The Mika's model with a helmet, a grate visor and a grill fork; the texture: the Mika's sheet in ash-grey."""
    geo_file = kn._load(h, "mika.geo.json")
    g = geo_file["minecraft:geometry"][0]
    g["description"]["identifier"] = f"geometry.{WACHTER}"
    sw = kn._swatches(g, ["helm", "helm_rand", "kam", "tralie", "steel", "vork", "riem"])
    c = kn._cube
    H = [0, 12, -6]
    helm = [c([-6.3, 11.6, -12.4], [12.6, 2.4, 11.2], sw["helm"]), c([-5.3, 14.0, -11.4], [10.6, 1.0, 9.2], sw["helm"]),
            c([-6.6, 11.2, -12.8], [13.2, 0.8, 12.0], sw["helm_rand"]),
            c([-0.7, 15.0, -11.0], [1.4, 2.2, 8.0], sw["kam"]), c([-0.5, 17.2, -9.6], [1.0, 1.0, 5.2], sw["kam"])]
    vizier = [c([x, 4.6, -13.3], [0.7, 6.8, 0.6], sw["tralie"]) for x in (-5.9, -3.5, -1.1, 0.4, 2.8, 5.2)]
    vizier += [c([-6.3, 7.6, -13.4], [12.6, 0.7, 0.6], sw["tralie"]), c([-6.3, 4.4, -13.4], [12.6, 0.7, 0.6], sw["tralie"])]
    vork = [c([7.4, 0.0, -3.0], [0.9, 16.0, 0.9], sw["steel"]), c([6.2, 16.0, -3.2], [3.3, 0.8, 1.3], sw["vork"]),
            c([6.2, 16.8, -3.0], [0.7, 3.4, 0.9], sw["vork"]), c([7.5, 16.8, -3.0], [0.7, 4.0, 0.9], sw["vork"]), c([8.8, 16.8, -3.0], [0.7, 3.4, 0.9], sw["vork"])]
    riem = [c([-7.2, 3.4, -1.6], [14.4, 1.2, 9.6], sw["riem"])]
    g["bones"].append({"name": "wachter_helm", "parent": "head", "pivot": H, "cubes": helm})
    g["bones"].append({"name": "wachter_vizier", "parent": "head", "pivot": H, "cubes": vizier})
    g["bones"].append({"name": "wachter_vork", "parent": "body", "pivot": [8, 0, -3], "cubes": vork})
    g["bones"].append({"name": "wachter_riem", "parent": "body", "pivot": [0, 5, 5], "cubes": riem})
    kn._save_geo(h, f"{WACHTER}.geo.json", geo_file)
    img = Image.open(os.path.join(h.TEX, "entity", "mika.png")).convert("RGBA")
    img = h.recolour(img, hue=0.06, sat=0.22, val=0.5, only=h.pinkish)
    a = np.asarray(img.convert("RGBA")).copy()
    rng = np.random.default_rng(SEED + 7)
    p = kn._paint
    p(a, sw["helm"], (74, 74, 86), rng, 8, _ijzer())
    p(a, sw["helm_rand"], (48, 46, 56), rng, 6)
    p(a, sw["kam"], (190, 60, 40), rng, 14, _hout)
    p(a, sw["tralie"], (36, 34, 42), rng, 4)
    p(a, sw["steel"], (112, 84, 56), rng, 8, _hout)
    p(a, sw["vork"], (150, 152, 166), rng, 8, _ijzer(40))
    p(a, sw["riem"], (60, 44, 34), rng, 6)
    h.save(Image.fromarray(a), "entity", f"{WACHTER}.png")


# =====================================================================================================================
# the blocks
# =====================================================================================================================
def rooster_tex():
    """The grate of the gate: iron bars two pixels wide, eight apart, rivets where they cross; the holes are see-through."""
    a = np.zeros((16, 16, 4), np.uint8)
    rng = np.random.default_rng(SEED + 1)
    for y in range(16):
        for x in range(16):
            bx, by = x % 8 < 2, y % 8 < 2
            if not (bx or by):
                continue
            v = int(rng.integers(-5, 6))
            kleur = (52 + v, 50 + v, 58 + v)
            if (bx and x % 8 == 0 and not by) or (by and y % 8 == 0 and not bx):
                kleur = (78 + v, 76 + v, 88 + v)                # the lit edge of a bar
            if bx and by:
                kleur = (30, 28, 34) if (x % 8, y % 8) != (0, 0) else (96, 94, 108)
            a[y, x] = kleur + (255,)
    return Image.fromarray(a)


def blik_tex():
    """The light of the Eye on the ground: a glowing ring of fire with the Eye itself in it (an almond and a slit pupil)."""
    n = 64
    a = np.zeros((n, n, 4), np.float32)
    rng = np.random.default_rng(SEED + 2)
    for y in range(n):
        for x in range(n):
            dx, dy = (x + 0.5 - n / 2) / (n / 2), (y + 0.5 - n / 2) / (n / 2)
            d = math.sqrt(dx * dx + dy * dy)
            if d > 1:
                continue
            hoek = math.atan2(dy, dx)
            vlam = 0.03 * math.sin(hoek * 12) + 0.02 * math.sin(hoek * 23 + 1)
            if d > 0.84 + vlam:
                t = (1 - d) / 0.16
                a[y, x] = (255, 120 + 90 * t, 30 + 60 * t, 235 * min(1.0, t * 2.2 + 0.25))
            else:
                a[y, x] = (255, 170, 60, 70 + 40 * rng.random())
            # the almond in the middle
            ex, ey = dx / 0.6, dy / 0.3
            e = math.sqrt(ex * ex + ey * ey)
            if e < 1:
                a[y, x] = (255, 214, 96, 150) if e < 0.8 else (220, 70, 20, 200)
                if abs(dx) < 0.045 * (1.2 - abs(dy) / 0.3) and abs(dy) < 0.26:
                    a[y, x] = (40, 10, 14, 220)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def beeldje_tex():
    """The statuette's sheet (16 x 16): charcoal stone above, the red of ear and nose and two white fangs in its corners."""
    a = np.zeros((16, 16, 4), np.uint8)
    rng = np.random.default_rng(SEED + 3)
    for y in range(16):
        for x in range(16):
            v = int(rng.integers(-6, 7))
            a[y, x] = (46 + v, 40 + v, 46 + v, 255)
    for y in range(0, 16, 4):
        a[y, :, :3] = (34, 30, 36)                              # courses of stone
    return Image.fromarray(a)


def beeldje_oog_tex():
    """The statuette's eye: 6 x 3 pixels a frame (drawn 12 x 6), eight frames under each other: open six times, half, shut."""
    fw, fh = 16, 16
    frames = ["open"] * 6 + ["half", "dicht"]
    a = np.zeros((fh * len(frames), fw, 4), np.uint8)
    for i, staat in enumerate(frames):
        f = a[i * fh:(i + 1) * fh]
        f[...] = (26, 22, 28, 255)
        for y in range(fh):
            for x in range(fw):
                nx, ny = (x + 0.5 - fw / 2) / (fw / 2 - 1), (y + 0.5 - fh / 2) / (fh / 2 - 3)
                d = math.sqrt(nx * nx + ny * ny)
                open_ = staat == "open" or (staat == "half" and abs(ny) < 0.45)
                if d < 1 and open_:
                    f[y, x] = _iris_kleur(d, 0.0, 0.5) + (255,)
                    if abs(x + 0.5 - fw / 2) < 1.0 and abs(ny) < 0.8:
                        f[y, x] = (14, 8, 16, 255)
                elif d < 1:
                    f[y, x] = LID + (255,)
                    if abs(ny) < 0.14:
                        f[y, x] = LID_GLOED + (255,)
    return Image.fromarray(a)


def blokken(h):
    T = "guhs:block/"
    # the grate of the gate
    h.save(rooster_tex(), "block", "ringh5_poortrooster.png")
    h.simple_block("ringh5_poortrooster", render_type="minecraft:cutout")
    h.self_drop("ringh5_poortrooster")
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:ringh5_poortrooster"])
    # the light of the Eye on the ground
    h.save(blik_tex(), "block", "ringh5_blik.png")
    h.w(f"{h.A}/models/block/ringh5_blik.json", {
        "ambientocclusion": False, "render_type": "minecraft:translucent", "textures": {"particle": T + "ringh5_blik", "blik": T + "ringh5_blik"},
        "elements": [{"from": [0, 0.1, 0], "to": [16, 0.3, 16], "shade": False, "faces": {
            "up": {"uv": [0, 0, 16, 16], "texture": "#blik"}, "down": {"uv": [0, 0, 16, 16], "texture": "#blik"}}}]})
    h.w(f"{h.A}/blockstates/ringh5_blik.json", {"variants": {"": {"model": "guhs:block/ringh5_blik"}}})
    # the statuette: a plinth, the spire, the Mika head with its ears, nose and fangs, the eye that blinks
    h.save(beeldje_tex(), "block", "oog_van_sausron_beeldje.png")
    h.save(beeldje_oog_tex(), "block", "oog_van_sausron_beeldje_oog.png")
    h.w(os.path.join(h.TEX, "block", "oog_van_sausron_beeldje_oog.png.mcmeta"), {"animation": {"frametime": 8, "interpolate": False}})
    steen, oog_t, rood, tand = "#steen", "#oog", "#rood", "#tand"
    el = h.el
    elements = [el([4, 0, 4], [12, 1.5, 12], steen), el([5.5, 1.5, 5.5], [10.5, 3, 10.5], steen), el([6.5, 3, 6.5], [9.5, 8, 9.5], steen),
                el([3.5, 8, 5.5], [12.5, 14, 10.5], steen),                                            # the head
                el([2, 12.5, 7.5], [5.5, 16, 8.5], steen), el([10.5, 12.5, 7.5], [14, 16, 8.5], steen),     # the ears
                el([2.8, 13.3, 7.4], [4.7, 15.2, 7.5], rood, faces=("north",)), el([11.3, 13.3, 7.4], [13.2, 15.2, 7.5], rood, faces=("north",)),
                el([4, 12.6, 5.2], [12, 13.4, 5.5], rood),                                              # (a brow of embers)
                el([7.4, 8.4, 5.2], [8.6, 9.2, 5.5], rood),                                              # the nose
                el([5.8, 7.2, 5.4], [6.4, 8.2, 5.8], tand), el([9.6, 7.2, 5.4], [10.2, 8.2, 5.8], tand),
                {"from": [5, 9.6, 5.4], "to": [11, 12.4, 5.5], "faces": {"north": {"uv": [2, 4, 14, 12], "texture": oog_t}}}]
    h.furniture_model("oog_van_sausron_beeldje", elements, {
        "particle": T + "oog_van_sausron_beeldje", "steen": T + "oog_van_sausron_beeldje", "oog": T + "oog_van_sausron_beeldje_oog",
        "rood": "minecraft:block/red_nether_bricks", "tand": "minecraft:block/quartz_block_top"})
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:oog_van_sausron_beeldje"])


# =====================================================================================================================
def build(h):
    model, anims, tex = maak_oog()
    h.w(f"{h.A}/geckolib/models/entity/oog_van_sausron.geo.json", model)
    h.w(f"{h.A}/geckolib/animations/entity/oog_van_sausron.animation.json", anims)
    for naam, img in tex.items():
        h.save(Image.fromarray(img), "entity", f"{naam}.png")
    wachter(h)
    blokken(h)


NODIG = {"oog_van_sausron": (("waak", "zoek", "slaap", "tevreden"), ("root", "kijk", "oog", "pupil", "lid_boven", "lid_onder")),
         WACHTER: ((), ("root", "head", "body", "wachter_helm", "wachter_vizier", "wachter_vork"))}


def check(h):
    problems = []
    for name, (wanted, botten) in NODIG.items():
        pad = f"{h.A}/geckolib/models/entity/{name}.geo.json"
        if not os.path.exists(pad) or not os.path.exists(os.path.join(h.TEX, "entity", f"{name}.png")):
            problems.append(f"{name}: model or texture missing")
            continue
        g = json.load(open(pad, encoding="utf-8"))["minecraft:geometry"][0]
        names = {b["name"] for b in g["bones"]}
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                problems.append(f"{name}: bone {b['name']} has a missing parent {b['parent']}")
        for b in botten:
            if b not in names:
                problems.append(f"{name}: no bone {b}")
        if wanted:
            anims = json.load(open(f"{h.A}/geckolib/animations/entity/{name}.animation.json", encoding="utf-8"))["animations"]
            for an in wanted:
                if f"animation.{name}.{an}" not in anims:
                    problems.append(f"{name}: no animation {an}")
            for an, a in anims.items():
                for bn in a.get("bones", {}):
                    if bn not in names:
                        problems.append(f"{name}: animation {an} moves a missing bone {bn}")
    if not os.path.exists(os.path.join(h.TEX, "entity", "oog_van_sausron_glowmask.png")):
        problems.append("oog_van_sausron: no glowmask")
    for blok in ("ringh5_poortrooster", "ringh5_blik", "oog_van_sausron_beeldje"):
        if not os.path.exists(f"{h.A}/models/block/{blok}.json") or not os.path.exists(f"{h.A}/blockstates/{blok}.json"):
            problems.append(f"block {blok}: model or blockstate missing")
    return problems


def preview(out):
    """Offline renders: the Eye open (front, three quarter), its glowmask, the textures of the blocks."""
    import tempfile
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    model, _anims, tex = maak_oog()
    arrays = {f"mem:{k}": v.astype(np.float32) for k, v in tex.items()}
    echte = wr.tex_array
    wr.tex_array = lambda ref: arrays[ref] if ref in arrays else echte(ref)
    tmp = os.path.join(tempfile.mkdtemp(), "oog.geo.json")
    json.dump(model, open(tmp, "w"))
    tiles = []
    for textuur, hide in (("oog_van_sausron", ()), ("oog_van_sausron", ("lid_boven", "lid_onder")), ("oog_van_sausron_glowmask", ())):
        q = wr.geo_quads(tmp, f"mem:{textuur}", hide=hide)
        for yaw, pitch in ((0, 0), (35, -10)):
            tiles.append(wr.render(q, yaw, pitch, 420, margin=0.06))
    for naam, img in tex.items():
        Image.fromarray(img).save(os.path.join(out, f"tex_{naam}.png"))
    sheet = Image.new("RGBA", (420 * 2, 420 * 3), (30, 24, 28, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, ((i % 2) * 420, (i // 2) * 420))
    sheet.save(os.path.join(out, "oog_sheet.png"))
    wr.tex_array = echte
    rooster_tex().resize((256, 256), Image.NEAREST).save(os.path.join(out, "tex_poortrooster.png"))
    blik_tex().resize((384, 384), Image.NEAREST).save(os.path.join(out, "tex_blik.png"))
    beeldje_oog_tex().resize((64, 64 * 8), Image.NEAREST).save(os.path.join(out, "tex_beeldje_oog.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
