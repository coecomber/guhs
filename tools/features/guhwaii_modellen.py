"""
Guhwai'i (3.0, slice guhwaii): the looks.

  BONES (variant)   the 626-guh's own bones (GuhVariant STITCH626, prefix "stitch"): big notched ears over the guh ears (with a
                    pink inside) and a pair of little extra arms on the sides of his body; two swatches (stitch_vacht,
                    stitch_oor_binnen)
  stitch_look(h)    guh_stitch626.png: blue fur, a light-blue belly and chin, a darker blue patch down his back, a big dark
                    nose, blue paws; the swatches of make_guh_variants are kept; then his sleeping-eyes texture again
  npcs(h)           Lilo-guh (long black hair, a red dress with white leaves, a pink hibiscus behind her ear) and Nani-guh (hair
                    in a bun with a plumeria, a light-blue flowery top) on the sitting guh
  animations()      animation.guh.emote_ukelele: 626 sits up and strums with his extra arms, head bobbing, ears flapping
"""
import json
import os

import numpy as np
from PIL import Image

from features import guhpolder_tex as ptex
from features import sterrenwacht_hulp as hulp

BLAUW = (79, 123, 216)
LICHTBLAUW = (156, 194, 246)
DONKERBLAUW = (48, 82, 166)
NEUS = (36, 36, 84)
OOR_ROZE = (240, 150, 190)

# --- the 626-guh's own bones -------------------------------------------------------------------------------------------------
EAR_L_PIVOT, EAR_R_PIVOT = [7.5, 11, -5.5], [-7.5, 11, -5.5]
_OOR = [([4.5, 9.5, -6.35], [11, 8, 1.2], 0), ([14.5, 12.5, -6.35], [2.5, 4, 1.2], 0), ([6.0, 17.3, -6.35], [6, 1.5, 1.2], 0)]
_OOR_BINNEN = [([6.0, 11.0, -6.6], [8, 5, 0.3], 0)]


def _spiegel(cubes):
    return [([-(o[0] + s[0]), o[1], o[2]], list(s), i) for o, s, i in cubes]


BONES = {
    "stitch_oor_links": ("ear_left", EAR_L_PIVOT, "stitch_vacht", _OOR),
    "stitch_oor_rechts": ("ear_right", EAR_R_PIVOT, "stitch_vacht", _spiegel(_OOR)),
    "stitch_oor_links_binnen": ("ear_left", EAR_L_PIVOT, "stitch_oor_binnen", _OOR_BINNEN),
    "stitch_oor_rechts_binnen": ("ear_right", EAR_R_PIVOT, "stitch_oor_binnen", _spiegel(_OOR_BINNEN)),
    # the extra arms: out of the body's sides, a bit behind the head, little paws forward
    "stitch_arm_links": ("body", [6.8, 6.5, 1.5], "stitch_vacht", [([6.4, 5.4, 0.2], [2.4, 2.2, 2.6], 0), ([7.6, 5.2, -2.6], [2.0, 2.0, 3.0], 0)]),
    "stitch_arm_rechts": ("body", [-6.8, 6.5, 1.5], "stitch_vacht", [([-8.8, 5.4, 0.2], [2.4, 2.2, 2.6], 0), ([-9.6, 5.2, -2.6], [2.0, 2.0, 3.0], 0)]),
}


def variant_painters(rng, v):
    return {"stitch_vacht": lambda: v.fabric(BLAUW, rng, 8), "stitch_oor_binnen": lambda: v.fabric(OOR_ROZE, rng, 6)}


def stitch_look(h):
    """guh_stitch626.png (painted over the guh fur like the Pinguh); make_guh_variants' swatches stay."""
    geo = json.load(open(os.path.join(h.A, "geo", "entity", "guh.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]
    base = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    pad = os.path.join(h.TEX, "entity", "guh_stitch626.png")
    oud = np.asarray(Image.open(pad).convert("RGBA")) if os.path.exists(pad) else None
    p = ptex.Painter(base, BLAUW)
    body = ptex._faces(geo, "body")
    for cube in body:
        p.rect(cube["down"], LICHTBLAUW)
        p.rect(cube["north"], LICHTBLAUW, rows=(0.3, 1.0))
        p.rect(cube["up"], DONKERBLAUW)                        # the dark patch down his back
        for side in ("east", "west"):
            p.rect(cube[side], LICHTBLAUW, rows=(0.72, 1.0))
    head = ptex._faces(geo, "head")
    for cube in head:
        if cube["_size"][0] <= 4:                            # the snoet: a big dark nose
            for k, f in cube.items():
                if not k.startswith("_"):
                    p.rect(f, NEUS, anything=True)
            continue
        p.rect(cube["down"], LICHTBLAUW)                      # the chin
        p.rect(cube["south"], DONKERBLAUW, rows=(0.0, 0.45))  # a dark patch on the back of his head
    for leg in ("leg_back_left", "leg_back_right", "leg_front_left", "leg_front_right"):
        for cube in ptex._faces(geo, leg):
            p.rect(cube["down"], LICHTBLAUW)
    for bone in ("tail",):
        for cube in ptex._faces(geo, bone):
            for k, f in cube.items():
                if not k.startswith("_"):
                    p.rect(f, DONKERBLAUW)
    a = p.a.astype(np.uint8)
    if oud is not None:
        leeg = np.asarray(base)[..., 3] == 0
        a[leeg] = oud[leeg]                                   # the variant bones' swatches (make_guh_variants)
    h.save(Image.fromarray(a), "entity", "guh_stitch626.png")
    # his sleeping eyes from this look (make_sleep_eyes ran before make_resources)
    import make_sleep_eyes
    os.makedirs(make_sleep_eyes.OUT, exist_ok=True)
    make_sleep_eyes.sleepy("guh_stitch626")


# --- Lilo-guh and Nani-guh ----------------------------------------------------------------------------------------------------
HAAR = (30, 24, 30)
JURK = (212, 44, 50)
TOPJE = (120, 196, 236)


def _blaadjes(block):
    """White leaves on a red dress (Lilo's famous dress)."""
    for (cx, cy) in ((6, 6), (22, 10), (12, 22), (27, 26), (3, 27), (18, 3)):
        for dy in range(-3, 4):
            for dx in range(-1, 2):
                if abs(dx) + abs(dy) <= 3:
                    block[max(0, min(31, cy + dy)), max(0, min(31, cx + dx + dy // 2)), :3] = (250, 250, 246)


def _bloemetjes(block):
    for (cx, cy) in ((7, 7), (23, 9), (13, 21), (26, 25), (4, 26)):
        for (dx, dy) in ((0, -2), (0, 2), (-2, 0), (2, 0)):
            block[max(0, min(31, cy + dy)), max(0, min(31, cx + dx)), :3] = (255, 255, 255)
        block[cy, cx, :3] = (255, 214, 70)


def _hibiscus(block):
    block[..., :3] = (255, 120, 180)
    for y in range(32):
        for x in range(32):
            if (x - 16) ** 2 + (y - 16) ** 2 < 30:
                block[y, x, :3] = (210, 40, 110)
    block[8:12, 16:18, :3] = (255, 226, 90)


def _plumeria(block):
    block[..., :3] = (252, 250, 240)
    for y in range(32):
        for x in range(32):
            if (x - 16) ** 2 + (y - 16) ** 2 < 40:
                block[y, x, :3] = (255, 214, 70)


def lilo(h):
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_lilo_guh")
    sw = hulp.swatches(geo, ["haar", "jurk", "bloem", "blad"])
    c = hulp.cube
    geo["bones"].append({"name": "lilo_haar", "parent": "head", "pivot": [0, 24, 2], "cubes": [
        c([-6.8, 24.6, -5.6], [13.6, 1.8, 11.4], sw["haar"]),            # the top of her head
        c([-5.8, 23.4, -7.3], [11.6, 1.8, 1.0], sw["haar"]),             # a fringe
        c([-7.4, 13.0, 3.6], [14.8, 12.0, 2.6], sw["haar"]),             # long hair down her back
        c([-7.6, 15.0, -3.5], [1.0, 9.0, 7.0], sw["haar"]), c([6.6, 15.0, -3.5], [1.0, 9.0, 7.0], sw["haar"])]})
    geo["bones"].append({"name": "lilo_bloem", "parent": "head", "pivot": [-8, 22, -1], "cubes": [
        c([-10.4, 21.2, -2.6], [3.0, 3.0, 1.6], sw["bloem"]), c([-9.6, 20.4, -1.6], [1.4, 1.2, 1.2], sw["blad"])]})
    geo["bones"].append({"name": "lilo_rokje", "parent": "body", "pivot": [0, 8, 0], "cubes": [
        c([-5.5, 1.0, -4.5], [11.0, 8.5, 9.0], sw["jurk"], inflate=0.25),
        c([-3.2, 9.5, -4.6], [1.0, 3.0, 0.4], sw["jurk"]), c([2.2, 9.5, -4.6], [1.0, 3.0, 0.4], sw["jurk"])]})
    hulp.save_geo(h, "guh_npc_lilo_guh.geo.json", geo_file)
    a = hulp.sitting_texture(h, 0.99, 0.62, 1.03)
    rng = np.random.default_rng(30131)
    hulp.paint_swatch(a, sw["haar"], HAAR, rng, 6)
    hulp.paint_swatch(a, sw["jurk"], JURK, rng, 6, _blaadjes)
    hulp.paint_swatch(a, sw["bloem"], (255, 120, 180), rng, 4, _hibiscus)
    hulp.paint_swatch(a, sw["blad"], (84, 176, 74), rng, 8)
    h.save(Image.fromarray(a), "entity", "npc_lilo_guh.png")


def nani(h):
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_nani_guh")
    sw = hulp.swatches(geo, ["haar", "topje", "bloem"])
    c = hulp.cube
    geo["bones"].append({"name": "nani_haar", "parent": "head", "pivot": [0, 24, 2], "cubes": [
        c([-6.8, 24.6, -5.6], [13.6, 1.6, 11.4], sw["haar"]),
        c([-7.2, 17.0, 3.8], [14.4, 8.0, 2.2], sw["haar"]),
        c([-2.5, 25.5, 2.0], [5.0, 4.0, 4.5], sw["haar"])]})            # the bun
    geo["bones"].append({"name": "nani_bloem", "parent": "head", "pivot": [8, 22, -1], "cubes": [
        c([7.4, 21.2, -2.6], [3.0, 3.0, 1.6], sw["bloem"])]})
    geo["bones"].append({"name": "nani_topje", "parent": "body", "pivot": [0, 8, 0], "cubes": [
        c([-5.4, 5.0, -4.4], [10.8, 7.5, 8.8], sw["topje"], inflate=0.22)]})
    hulp.save_geo(h, "guh_npc_nani_guh.geo.json", geo_file)
    a = hulp.sitting_texture(h, 0.98, 0.7, 0.98)
    rng = np.random.default_rng(30132)
    hulp.paint_swatch(a, sw["haar"], HAAR, rng, 6)
    hulp.paint_swatch(a, sw["topje"], TOPJE, rng, 6, _bloemetjes)
    hulp.paint_swatch(a, sw["bloem"], (252, 250, 240), rng, 4, _plumeria)
    h.save(Image.fromarray(a), "entity", "npc_nani_guh.png")


def npcs(h):
    lilo(h)
    nani(h)


# --- the ukelele emote ----------------------------------------------------------------------------------------------------------
def _kf(pairs):
    return {str(float(t)): v for t, v in pairs}


def animations():
    t = [0.0, 0.25, 0.5, 0.75, 1.0]
    return {"animation.guh.emote_ukelele": {"loop": True, "animation_length": 1.0, "bones": {
        # sitting up a little, swaying to the beat
        "root": {"rotation": _kf(zip(t, [[0, 0, -4], [0, 0, 0], [0, 0, 4], [0, 0, 0], [0, 0, -4]])),
                 "position": _kf(zip(t, [[0, 0, 0], [0, 0.6, 0], [0, 0, 0], [0, 0.6, 0], [0, 0, 0]]))},
        "body": {"rotation": _kf([(0, [-12, 0, 0])]),
                 "scale": _kf(zip(t, [[1.03, 0.98, 1], [1.0, 1.02, 1], [1.03, 0.98, 1], [1.0, 1.02, 1], [1.03, 0.98, 1]]))},
        "head": {"rotation": _kf(zip(t, [[0, 0, 8], [0, 0, 2], [0, 0, -8], [0, 0, -2], [0, 0, 8]]))},
        # the front paws hold the neck and the body of the ukelele
        "leg_front_left": {"rotation": _kf([(0, [-55, 0, -20])]), "position": _kf([(0, [0, 3, -1])])},
        "leg_front_right": {"rotation": _kf([(0, [-50, 0, 25])]), "position": _kf([(0, [0, 2.5, -1])])},
        # the extra arms strum: down, up, down, up
        "stitch_arm_links": {"rotation": _kf(zip(t, [[-40, 0, 10], [-10, 0, 25], [-40, 0, 10], [-10, 0, 25], [-40, 0, 10]]))},
        "stitch_arm_rechts": {"rotation": _kf(zip(t, [[-10, 0, -25], [-40, 0, -10], [-10, 0, -25], [-40, 0, -10], [-10, 0, -25]]))},
        # his big ears flap along
        "ear_left": {"rotation": _kf(zip(t, [[0, 0, 12], [0, 0, -6], [0, 0, 12], [0, 0, -6], [0, 0, 12]]))},
        "ear_right": {"rotation": _kf(zip(t, [[0, 0, -6], [0, 0, 12], [0, 0, -6], [0, 0, 12], [0, 0, -6]]))},
        "tail": {"rotation": _kf(zip(t, [[0, -25, 0], [0, 25, 0], [0, -25, 0], [0, 25, 0], [0, -25, 0]]))},
    }}}
