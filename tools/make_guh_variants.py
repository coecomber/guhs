"""
Guh variants: adds the clothing / neck bones to the guh model and makes one texture per variant.

  - bones (all hidden unless the variant wears them, see GuhVariant.java):
      outfit_suit*      a fleece onesie over the body and the tops of the legs (brococolief, sweater, rain, chef)
      outfit_rain_hat   a sou'wester            outfit_party_hat  a pointy party hat with a pom-pom
      outfit_chef_hat   a puffy chef's hat      outfit_bowtie     a bow tie under the chin
      neck              the brontosaurus guh's long, wrinkly neck (the renderer lifts the head on top of it)
  - every clothing face samples one 8x8 "fabric swatch" in a free part of the UV sheet, so a variant's texture only
    has to paint its swatches (and recolour the fur) to dress the guh
  - textures/entity/guh_<variant>.png for every variant except the normal guh (guh.png)

Run from the project root (after tools/import_lieke_model.py, if you re-import the model):
    python tools/make_guh_variants.py
"""
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image

ASSETS = os.path.join("src", "main", "resources", "assets", "guhs")
GEO = os.path.join(ASSETS, "geo", "entity", "guh.geo.json")
TEXTURES = os.path.join(ASSETS, "textures", "entity")
UV = 128
FUR = (195, 160, 205)
SWATCH = 8

# --- the extra bones ---------------------------------------------------------------------------------------------------
BODY_PIVOT, HEAD_PIVOT = [0, 6, 6], [0, 6, -2]
SUIT_BODY = [([-6.5, 2.5, 1], [13, 7, 9]), ([-4.5, 1, 0.5], [9, 10, 10]), ([-5.5, 2, -1], [11, 8, 13]),
             ([-6, 1.5, -2.5], [12, 9, 14])]
LEGS = {"leg_back_left": ([4.5, 1, 7], [6, 1, 9]), "leg_back_right": ([-8, 1, 7], [-6, 1, 9]),
        "leg_front_left": ([4.25, 1, -2.5], [2, 1, 0]), "leg_front_right": ([-7.75, 1, -2.5], [-6, 1, 0])}
# bone name: (parent, pivot, swatch, [(origin, size, inflate), ...])
H = HEAD_PIVOT
BONES = {
    # body clothes cover the body and tail, never the feet
    "outfit_suit": ("body", BODY_PIVOT, "suit", [(o, s, 0.45) for o, s in SUIT_BODY]),
    "outfit_suit_tail": ("tail", [0, 1.5, 12], "suit", [([-1.1, 2.65, 11.3], [2.2, 1.7, 2.5], 0)]),
    "outfit_cape": ("body", BODY_PIVOT, "cape", [([-6.8, 11.3, -1.5], [13.6, 0.4, 12.5], 0), ([-6.8, 4, 10.6], [13.6, 7.5, 0.4], 0),
                                                ([-7.2, 5, -1.5], [0.4, 6.5, 12], 0), ([6.8, 5, -1.5], [0.4, 6.5, 12], 0)]),
    # hats
    "outfit_rain_hat": ("head", H, "rain_hat", [([-6, 15.2, -11], [12, 0.6, 10], 0), ([-3.5, 15.8, -8.5], [7, 2.5, 5], 0)]),
    "outfit_party_hat": ("head", H, "party_hat", [([-2.5, 15, -8.5], [5, 2, 5], 0), ([-1.75, 17, -7.75], [3.5, 2, 3.5], 0),
                                                ([-1, 19, -7], [2, 2, 2], 0)]),
    "outfit_party_pom": ("head", H, "pom", [([-0.75, 21, -6.75], [1.5, 1.5, 1.5], 0)]),
    "outfit_chef_hat": ("head", H, "chef_hat", [([-3.5, 15, -8.5], [7, 1.5, 5], 0), ([-4.5, 16.5, -9.5], [9, 3.5, 7], 0)]),
    "outfit_helmet": ("head", H, "helmet", [([-6, 14.8, -10.8], [12, 2.5, 9.6], 0), ([-1, 17.3, -10], [2, 1.2, 8], 0),
                                          ([-6, 14.6, -12.2], [12, 0.4, 1.6], 0)]),
    "outfit_cap": ("head", H, "cap", [([-4, 15, -10], [8, 2, 7], 0), ([-4, 15, -12.5], [8, 0.5, 2.5], 0)]),
    "outfit_crown": ("head", H, "crown", [([-3.2, 15, -8.4], [6.4, 1.4, 5], 0)] +
                     [([x, 16.4, z], [1, 1.2, 1], 0) for x in (-3.2, 2.2) for z in (-8.4, -4.4)] + [([-0.5, 16.4, -8.4], [1, 1.6, 1], 0)]),
    "outfit_tall_hat": ("head", H, "tall_hat", [([-5.5, 15, -10.5], [11, 0.5, 9], 0), ([-3, 15.5, -8.5], [6, 3, 5], 0),
                                              ([-2, 18.5, -7.5], [4, 3, 3], 0), ([-1, 21.5, -6.8], [2, 2.5, 1.8], 0),
                                              ([-0.5, 23.5, -5.8], [1, 1, 2.5], 0)]),
    "outfit_tricorn": ("head", H, "tricorn", [([-5.5, 15, -9.8], [11, 1.6, 7.6], 0), ([-3.5, 16.6, -8.8], [7, 1.6, 5.6], 0),
                                            ([-1, 15, -10.8], [2, 1.6, 1], 0)]),
    "outfit_santa_hat": ("head", H, "santa_hat", [([-3.5, 15, -8.5], [7, 2.5, 5.5], 0), ([-2.5, 17.5, -7.8], [5, 2, 4], 0),
                                                ([-1.5, 19.5, -6.8], [3, 1.5, 3], 0), ([0, 20.5, -5.5], [1.5, 1.5, 3], 0)]),
    "outfit_santa_trim": ("head", H, "santa_trim", [([-3.8, 14.8, -8.8], [7.6, 1, 6.1], 0), ([0, 20.2, -3.2], [1.8, 1.8, 1.8], 0)]),
    "outfit_mitre": ("head", H, "mitre", [([-3, 15, -8.3], [6, 4, 4.6], 0), ([-2, 19, -7.8], [4, 2, 3.6], 0),
                                        ([-0.75, 21, -6.8], [1.5, 1, 1.6], 0)]),
    "outfit_beret": ("head", H, "beret", [([-4.5, 15, -9.5], [9, 1.6, 8], 0), ([-3.5, 16.6, -8.5], [7, 0.8, 6], 0)]),
    "outfit_beret_feather": ("head", H, "feather", [([2.8, 16, -7], [0.5, 4.5, 0.5], 0), ([2.8, 20, -6.5], [0.5, 1.5, 1.5], 0)]),
    "outfit_pumpkin": ("head", H, "pumpkin", [([-7.6, 1.8, -12.6], [15.2, 13.6, 12.6], 0)]),
    # glasses (the eyes sit at x +-1.25..7, y 7.25..12.5, just in front of z=-12)
    "outfit_glasses": ("head", H, "glasses", [([1, 7, -12.6], [6.2, 5.8, 0.4], 0), ([-7.2, 7, -12.6], [6.2, 5.8, 0.4], 0),
                                            ([-1.25, 10, -12.6], [2.5, 0.6, 0.4], 0)]),
    "outfit_monocle": ("head", H, "monocle", [([1, 12.3, -12.6], [6.2, 0.5, 0.4], 0), ([1, 7, -12.6], [6.2, 0.5, 0.4], 0),
                                            ([1, 7.5, -12.6], [0.5, 4.8, 0.4], 0), ([6.7, 7.5, -12.6], [0.5, 4.8, 0.4], 0),
                                            ([6.9, 4, -12.4], [0.3, 3, 0.3], 0)]),
    "outfit_eyepatch": ("head", H, "eyepatch", [([-7.2, 7, -12.6], [6.2, 5.8, 0.5], 0), ([-7.4, 11.8, -12.65], [14.8, 0.6, 0.4], 0)]),
    # neck
    "outfit_bowtie": ("head", H, "bowtie", [([-2.5, 1.2, -11], [2, 2, 0.8], 0), ([0.5, 1.2, -11], [2, 2, 0.8], 0),
                                          ([-0.5, 1.5, -11.3], [1, 1.4, 1], 0)]),
    "outfit_scarf": ("body", BODY_PIVOT, "scarf", [([-7.4, 1.2, -0.8], [14.8, 10.2, 2.4], 0), ([6.9, -1, 0], [1, 4, 1.5], 0)]),
    "outfit_stethoscope": ("head", H, "stethoscope", [([-4.5, 0.9, -11.6], [9, 0.5, 0.5], 0), ([-1, -0.6, -11.8], [2, 1.4, 0.6], 0)]),
    # back
    "outfit_backpack": ("body", BODY_PIVOT, "backpack", [([-3.5, 11.2, 1.5], [7, 4, 6], 0), ([-3.8, 14.7, 1.3], [7.6, 0.8, 3], 0)]),
    # the brontosaurus neck: wrinkly segments leaning forward, with rubber bands between them
    "neck": ("root", [0, 6, -4], "neck", [([-3.5 + 0.3 * (i % 2), 5 + 2.8 * i, -8.5 - 0.55 * i], [7 - 0.6 * (i % 2), 2.8, 6], 0)
                                          for i in range(9)]),
    "neck_bands": ("root", [0, 6, -4], "band", [([-3.5, 7.6 + 2.8 * i, -8.5 - 0.55 * i], [7, 0.4, 6], 0.25) for i in range(7)]),
    # the teckel guh: a longer body and a middle pair of legs (the renderer moves the back legs and tail back)
    "teckel_body": ("body", BODY_PIVOT, "teckel_fur", [([o[0], o[1], o[2] + 7], s, 0) for o, s in SUIT_BODY[2:]]),
    "teckel_leg_left": ("body", [6, 1, 5], "teckel_fur", [([4.5, 0, 3], [3.5, 2.5, 4.5], 0)]),
    "teckel_leg_right": ("body", [-6, 1, 5], "teckel_fur", [([-8, 0, 3], [3.5, 2.5, 4.5], 0)]),
    # the ender guh: dragon wings (a bony arm with fingers, and a thin skin between them) and spikes down its back
    "ender_wing_left": ("body", [6, 10, 3], "ender_bone", [([6, 9.4, 2.3], [13.5, 1.2, 1.2], 0), ([10, 9.7, 3.5], [0.6, 0.6, 8.5], 0),
                                                          ([14, 9.7, 3.5], [0.6, 0.6, 7], 0), ([18.6, 9.7, 3.5], [0.6, 0.6, 4.5], 0),
                                                          ([19.5, 9.4, 1.8], [1, 1, 1], 0)]),
    "ender_wing_left_skin": ("ender_wing_left", [6, 10, 3], "ender_wing", [([6.5, 9.9, 3.5], [3.5, 0.2, 9.5], 0), ([10, 9.9, 3.5], [4, 0.2, 7.5], 0),
                                                                            ([14, 9.9, 3.5], [4.6, 0.2, 6], 0), ([18.6, 9.9, 3.5], [0.6, 0.2, 4], 0)]),
    "ender_wing_right": ("body", [-6, 10, 3], "ender_bone", [([-19.5, 9.4, 2.3], [13.5, 1.2, 1.2], 0), ([-10.6, 9.7, 3.5], [0.6, 0.6, 8.5], 0),
                                                            ([-14.6, 9.7, 3.5], [0.6, 0.6, 7], 0), ([-19.2, 9.7, 3.5], [0.6, 0.6, 4.5], 0),
                                                            ([-20.5, 9.4, 1.8], [1, 1, 1], 0)]),
    "ender_wing_right_skin": ("ender_wing_right", [-6, 10, 3], "ender_wing", [([-10, 9.9, 3.5], [3.5, 0.2, 9.5], 0), ([-14, 9.9, 3.5], [4, 0.2, 7.5], 0),
                                                                              ([-18.6, 9.9, 3.5], [4.6, 0.2, 6], 0), ([-19.2, 9.9, 3.5], [0.6, 0.2, 4], 0)]),
    "ender_horns": ("head", H, "ender_bone", [([-4.5, 14.5, -5], [1.2, 2.5, 1.2], 0), ([3.3, 14.5, -5], [1.2, 2.5, 1.2], 0),
                                            ([-4.3, 16.8, -4.3], [0.8, 1.2, 0.8], 0), ([3.5, 16.8, -4.3], [0.8, 1.2, 0.8], 0)]),
    # the Koningguh: a fluffy white mane framing his face, a grand moustache and golden socks
    "koning_manen": ("head", H, "koning_manen", [([round(9.2 * math.cos(math.radians(a)) - 2, 2), round(8 + 8.6 * math.sin(math.radians(a)) - 2, 2), -10.8],
                                                 [4, 4, 4.5], 0.25) for a in range(0, 360, 24) if not 230 < a < 310]),
    "koning_snor": ("head", H, "koning_manen", [([-4, 4.3, -13.1], [3.6, 1.2, 0.6], 0), ([0.4, 4.3, -13.1], [3.6, 1.2, 0.6], 0),
                                               ([-4.8, 5, -13.05], [1, 1.2, 0.5], 0), ([3.8, 5, -13.05], [1, 1.2, 0.5], 0)]),
    "koning_sok_fl": ("leg_front_left", [2, 1, 0], "koning_goud", [([4.25, 0, -2.5], [3.5, 1.2, 4.5], 0.35)]),
    "koning_sok_fr": ("leg_front_right", [-6, 1, 0], "koning_goud", [([-7.75, 0, -2.5], [3.5, 1.2, 4.5], 0.35)]),
    "koning_sok_bl": ("leg_back_left", [6, 1, 9], "koning_goud", [([4.5, 0, 7], [3.5, 1.2, 4.5], 0.35)]),
    "koning_sok_br": ("leg_back_right", [-6, 1, 9], "koning_goud", [([-8, 0, 7], [3.5, 1.2, 4.5], 0.35)]),
    # the guh king's outfit: a tall jewelled crown, a long ermine-trimmed cape and a golden chain with a medallion
    "outfit_grand_crown": ("head", H, "grand_crown", [([-4.2, 15, -9.2], [8.4, 1.8, 6.4], 0)] +
                           [([x, 16.8, z], [1, 2.6, 1], 0) for x in (-4.2, -0.5, 3.2) for z in (-9.2, -3.8)] +
                           [([-0.5, 19.4, -6.5], [1, 1, 1], 0), ([-4.2, 18, -6.5], [8.4, 0.6, 0.8], 0)]),
    "outfit_long_cape": ("body", BODY_PIVOT, "long_cape", [([-7, 11.4, -1.5], [14, 0.4, 13.5], 0), ([-7, 0.2, 11.6], [14, 11.4, 0.4], 0),
                                                         ([-7.4, 0.5, -1.5], [0.4, 11, 13], 0), ([7, 0.5, -1.5], [0.4, 11, 13], 0),
                                                         ([-5, 0, 12], [10, 0.4, 3], 0)]),
    "outfit_long_cape_collar": ("body", BODY_PIVOT, "long_cape_collar", [([-7.6, 10.2, -2.6], [15.2, 2, 2.6], 0)]),
    "outfit_chain": ("head", H, "chain", [([-4.5, 1.6, -11.5], [9, 0.5, 0.5], 0), ([-4.5, 0.6, -11.5], [0.5, 1, 0.5], 0),
                                         ([4, 0.6, -11.5], [0.5, 1, 0.5], 0), ([-1.3, -1.2, -11.8], [2.6, 2.6, 0.6], 0)]),
    "ender_spikes": ("body", BODY_PIVOT, "ender_bone", [([-0.5, 10.8, z], [1, 1.6 - 0.2 * i, 1.2], 0) for i, z in enumerate((0, 3, 6, 9))]
                     + [([-0.4, 3.6, 17.5], [0.8, 1.2, 1.5], 0)]),
}
# the renderer lifts the head (and ears) by this much on the brontosaurus guh: keep in sync with GuhRenderer
NECK_LIFT = (22.0, -4.5)  # (up, forwards)
# the teckel guh's back legs and tail move back by this much (keep in sync with GuhVariant.TECKEL_STRETCH)
TECKEL_STRETCH = 7.0


def used_map(geo):
    used = [[False] * UV for _ in range(UV)]
    for bone in geo["bones"]:
        for cube in bone.get("cubes", []):
            for face in cube.get("uv", {}).values():
                (u, v), (w, h) = face["uv"], face["uv_size"]
                for x in range(int(min(u, u + w)), int(max(u, u + w) + 0.999)):
                    for y in range(int(min(v, v + h)), int(max(v, v + h) + 0.999)):
                        if 0 <= x < UV and 0 <= y < UV:
                            used[y][x] = True
    return used


def free_swatch(used):
    for y in range(0, UV - SWATCH + 1, SWATCH):
        for x in range(0, UV - SWATCH + 1, SWATCH):
            if all(not used[yy][xx] for yy in range(y, y + SWATCH) for xx in range(x, x + SWATCH)):
                for yy in range(y, y + SWATCH):
                    for xx in range(x, x + SWATCH):
                        used[yy][xx] = True
                return x, y
    raise RuntimeError("no free texture space for a swatch")


# bones that were renamed or dropped (so an old guh.geo.json loses them): kaasmijn_lamp is now outfit_kaasmijn_lamp
RETIRED_BONES = ("kaasmijn_lamp",)


SHARED_OUTFIT_FROM = "knuffeldal"   # features from this one on (tools/features FEATURES order) share the outfit space
BONE_MODULE = {}                     # bone name -> the feature module that added it (filled by main)


def shared_pool(bone):
    """The feature whose own outfit swatch pool this bone uses, or None for the space every guh texture shares."""
    module = BONE_MODULE.get(bone)
    if module is None or not bone.startswith("outfit_"):
        return None
    import features
    order = features.FEATURES
    return module if SHARED_OUTFIT_FROM in order and order.index(module) >= order.index(SHARED_OUTFIT_FROM) else None


def add_bones():
    """Adds (or re-adds) the variant bones to guh.geo.json; returns swatch name -> (u, v) in UV units."""
    geo_file = json.load(open(GEO, encoding="utf-8"))
    geo = geo_file["minecraft:geometry"][0]
    geo["bones"] = [b for b in geo["bones"] if b["name"] not in BONES and b["name"] not in RETIRED_BONES
                    and not b["name"].startswith(("outfit_", "neck", "teckel_"))]
    used = used_map(geo)
    swatches = {}
    # 2.8: the texture has room for ~100 swatches, the clothes of all features need more. A clothes piece is drawn with
    # only its own bones and its own texture (client.GuhClothesLayer), so the outfit_ swatches of each feature from
    # SHARED_OUTFIT_FROM on only have to be unique within that feature: those features share the space left over after
    # the variant bones and the older clothes (which stay unique everywhere, so their places never move).
    pools = {}
    for name, (parent, pivot, swatch, cubes) in BONES.items():
        pool = shared_pool(name)
        if pool is None:
            if swatch not in swatches:
                swatches[swatch] = free_swatch(used)
        else:
            pools.setdefault(pool, []).append(swatch)
    for pool, names in pools.items():
        pool_used = [row[:] for row in used]
        for swatch in names:  # a swatch name another pool already placed: keep that spot free here too
            if swatch in swatches:
                x, y = swatches[swatch]
                for yy in range(y, y + SWATCH):
                    for xx in range(x, x + SWATCH):
                        pool_used[yy][xx] = True
        for swatch in names:
            if swatch not in swatches:
                swatches[swatch] = free_swatch(pool_used)
    for name, (parent, pivot, swatch, cubes) in BONES.items():
        u, v = swatches[swatch]
        face = {"uv": [u, v], "uv_size": [SWATCH, SWATCH]}
        geo["bones"].append({"name": name, "parent": parent, "pivot": pivot, "cubes": [
            {"origin": origin, "size": size, **({"inflate": inflate} if inflate else {}),
             "uv": {d: dict(face) for d in ("north", "south", "east", "west", "up", "down")}}
            for origin, size, inflate in cubes]})
    with open(GEO, "w", encoding="utf-8") as f:
        json.dump(geo_file, f, indent=2)
    return swatches


# --- textures ----------------------------------------------------------------------------------------------------------
def fabric(colour, rng, noise=14):
    """A fleece-ish swatch: the colour with soft speckles."""
    px = SWATCH * 4
    base = np.array(colour, np.float32)
    a = base + rng.normal(0, noise / 2, (px, px, 1)) + rng.normal(0, noise / 4, (px, px, 3))
    return np.clip(a, 0, 255)


def stripes(c1, c2, rng, width=4, diagonal=False):
    px = SWATCH * 4
    a = np.zeros((px, px, 3), np.float32)
    for y in range(px):
        for x in range(px):
            a[y, x] = c1 if ((x + y if diagonal else y) // width) % 2 == 0 else c2
    return np.clip(a + rng.normal(0, 5, (px, px, 1)), 0, 255)


def dots(base, dot, rng, every=6, size=2):
    a = fabric(base, rng, 6)
    for y in range(0, SWATCH * 4, every):
        for x in range((y // every % 2) * (every // 2), SWATCH * 4, every):
            a[y:y + size, x:x + size] = dot
    return a


def band(base, stripe, rng, rows):
    """A fabric with a few horizontal stripes (rows are pixel rows 0..31)."""
    a = fabric(base, rng, 6)
    for r in rows:
        a[r:r + 3, :] = stripe
    return a


def buttons(base, button, rng):
    a = fabric(base, rng, 6)
    for y in range(3, SWATCH * 4, 7):
        a[y:y + 2, 15:17] = button
    return a


def stars(base, star, rng, n=14):
    a = fabric(base, rng, 8)
    for _ in range(n):
        x, y = rng.integers(1, SWATCH * 4 - 2, 2)
        a[y, x - 1:x + 2] = star
        a[y - 1:y + 2, x] = star
    return a


def zigzag(base, zig, rng):
    a = fabric(base, rng, 6)
    for x in range(SWATCH * 4):
        for off in (8, 22):
            y = off + abs((x % 8) - 4)
            a[y:y + 2, x] = zig
    return a


def ermine(base, rng):
    a = fabric(base, rng, 8)
    a[24:, :] = (245, 245, 240)
    for x in range(2, SWATCH * 4, 6):
        a[26:29, x] = (20, 20, 20)
    return a


def metal(base, rng):
    a = np.zeros((SWATCH * 4, SWATCH * 4, 3), np.float32)
    for y in range(SWATCH * 4):
        a[y, :] = np.array(base) * (1.15 - y / 90)
    for y in (4, 27):
        for x in range(3, SWATCH * 4, 8):
            a[y:y + 2, x:x + 2] = (230, 230, 235)
    return np.clip(a + rng.normal(0, 4, a.shape), 0, 255)


def pumpkin_face(rng):
    a = fabric((236, 130, 30), rng, 10)
    for x in range(0, SWATCH * 4, 8):
        a[:, x] = (200, 100, 20)
    for (x, y) in ((8, 10), (20, 10)):
        a[y:y + 4, x:x + 4] = (40, 20, 5)
    a[20:23, 7:25] = (40, 20, 5)
    return a


def straw(rng):
    a = fabric((226, 196, 110), rng, 10)
    for i in range(0, SWATCH * 4, 3):
        a[i, :] *= 0.85
        a[:, i] *= 0.92
    return np.clip(a, 0, 255)


def recolour_fur(img, colour, sparkle=False, rng=None):
    a = np.asarray(img).astype(np.int32)
    diff = a[..., :3] - np.array(FUR)
    mask = (np.abs(diff).sum(-1) < 40) & (a[..., 3] > 0)
    new = np.clip(np.array(colour) + diff, 0, 255)
    if sparkle:
        glint = rng.random(mask.shape) < 0.02
        new = np.where(glint[..., None], np.minimum(new + 60, 255), new)
    a[..., :3] = np.where(mask[..., None], new, a[..., :3])
    return Image.fromarray(a.astype(np.uint8))


# variant: fur colour (None = keep), {swatch: painter}
def variants(rng):
    pink_fleece = lambda: fabric((255, 64, 190), rng, 18)
    return {
        "brococolief": (None, {}),          # wears the pink onesie as clothes
        "mint": ((170, 226, 200), {}),
        "choco": ((150, 104, 82), {}),
        "snow": ((240, 238, 244), {}),
        "golden": ((242, 200, 96), {}),
        "brontosaurus": ((232, 222, 214), {"neck": lambda: fabric((236, 214, 214), rng, 20),
                                           "band": lambda: fabric((214, 190, 130), rng, 8)}),
        "starry": ((58, 40, 104), {}),
        "ghost": ((232, 238, 255), {}),
        "teckel": (None, {"teckel_fur": lambda: fabric(FUR, rng, 6)}),
        "koning": ((104, 58, 150), {"koning_manen": lambda: fabric((250, 248, 244), rng, 14),
                                    "koning_goud": lambda: fabric((245, 196, 60), rng, 10)}),
        "ender": ((46, 32, 60), {"ender_bone": lambda: fabric((22, 16, 30), rng, 6),
                                 "ender_wing": lambda: fabric((88, 38, 128), rng, 16)}),
        **{f"rainbow_{k}": (tuple(int(c * 255) for c in __import__("colorsys").hsv_to_rgb(k / 8, 0.42, 0.97)), {})
           for k in range(8)},
    }


# clothes you can put on a tamed guh (GuhClothes.java): {swatch: painter} on an otherwise see-through sheet
def clothes(rng):
    f = lambda c, n=8: (lambda: fabric(c, rng, n))
    return {
        "pink_onesie": {"suit": f((255, 64, 190), 18)},
        "striped_sweater": {"suit": lambda: stripes((64, 110, 210), (240, 240, 250), rng)},
        "raincoat": {"suit": f((250, 210, 40))},
        "chef_jacket": {"suit": f((246, 246, 246), 6)},
        "rain_hat": {"rain_hat": f((250, 210, 40))},
        "party_hat": {"party_hat": lambda: stripes((230, 60, 170), (250, 220, 60), rng, 3, diagonal=True), "pom": f((250, 250, 250), 6)},
        "chef_hat": {"chef_hat": f((252, 252, 252), 5)},
        "red_bowtie": {"bowtie": f((220, 40, 60))},
        "black_bowtie": {"bowtie": f((30, 30, 36), 6)},
        # glasses
        "sunglasses": {"glasses": f((20, 20, 26), 4)},
        "heart_glasses": {"glasses": f((255, 90, 170), 6)},
        "monocle": {"monocle": f((230, 190, 60), 6)},
        "eyepatch": {"eyepatch": f((18, 18, 20), 4)},
        # work outfits
        "firefighter_helmet": {"helmet": f((200, 30, 30))},
        "firefighter_jacket": {"suit": lambda: band((140, 110, 60), (240, 220, 60), rng, (8, 22))},
        "police_cap": {"cap": f((30, 40, 90), 6)},
        "police_uniform": {"suit": lambda: buttons((35, 45, 100), (230, 200, 70), rng)},
        "doctor_coat": {"suit": lambda: buttons((245, 245, 245), (150, 200, 230), rng)},
        "stethoscope": {"stethoscope": f((120, 120, 130), 6)},
        "builder_helmet": {"helmet": f((250, 200, 20))},
        "safety_vest": {"suit": lambda: band((255, 120, 20), (210, 215, 220), rng, (10, 20))},
        "straw_hat": {"rain_hat": lambda: straw(rng)},
        "overalls": {"suit": lambda: buttons((60, 100, 170), (220, 190, 80), rng)},
        # holidays
        "santa_hat": {"santa_hat": f((200, 20, 30)), "santa_trim": f((250, 250, 250), 5)},
        "christmas_sweater": {"suit": lambda: zigzag((190, 20, 30), (245, 245, 245), rng)},
        "winter_scarf": {"scarf": lambda: stripes((200, 30, 40), (245, 245, 245), rng, 4)},
        "sint_mitre": {"mitre": lambda: band((190, 20, 30), (240, 200, 60), rng, (2, 26))},
        "piet_beret": {"beret": f((40, 30, 60)), "feather": f((250, 250, 250), 5)},
        "witch_hat": {"tall_hat": f((50, 20, 70), 8)},
        "ghost_sheet": {"suit": f((248, 248, 252), 4)},
        "pumpkin_head": {"pumpkin": lambda: pumpkin_face(rng)},
        "orange_crown": {"crown": f((255, 140, 0))},
        "orange_shirt": {"suit": f((255, 130, 10))},
        # fantasy
        "wizard_hat": {"tall_hat": lambda: stars((40, 50, 150), (250, 230, 120), rng)},
        "wizard_robe": {"suit": lambda: stars((35, 40, 120), (250, 230, 120), rng)},
        "knight_helmet": {"helmet": lambda: metal((170, 175, 185), rng)},
        "knight_armour": {"suit": lambda: metal((160, 165, 178), rng)},
        "royal_crown": {"crown": f((245, 200, 60), 6)},
        "royal_cape": {"cape": lambda: ermine((170, 20, 40), rng)},
        "pirate_hat": {"tricorn": lambda: band((25, 25, 30), (230, 200, 90), rng, (0,))},
        # the backpack
        "guh_backpack": {"backpack": lambda: band((230, 120, 170), (140, 70, 100), rng, (6, 20))},
        # the kermis outfit (2.1.0; its textures were first made with their own rng, see the scratch kermis_clothes script)
        "kermis_hoed": {"tall_hat": lambda: stripes((210, 30, 50), (250, 245, 240), rng, 3)},
        "kermis_jasje": {"suit": lambda: buttons((200, 30, 50), (250, 200, 60), rng)},
        "kermis_strik": {"bowtie": lambda: dots((250, 200, 60), (220, 40, 60), rng, every=4, size=2)},
        # the detective outfit (2.2.0): a checked deerstalker-ish cap, a brass magnifying glass and a trench coat
        "detective_pet": {"cap": lambda: stripes((150, 110, 70), (115, 80, 50), rng, 2)},
        "detective_vergrootglas": {"monocle": f((200, 170, 90), 5)},
        "detective_jas": {"suit": lambda: buttons((170, 130, 80), (90, 60, 35), rng)},
        # the guh king's outfit (2.3.0): only from the Koningguh
        "koning_kroon": {"grand_crown": lambda: dots((245, 200, 60), (200, 30, 80), rng, every=5, size=2)},
        "koning_mantel": {"long_cape": lambda: fabric((120, 25, 150), rng, 12), "long_cape_collar": lambda: ermine((245, 245, 245), rng)},
        "koning_ketting": {"chain": f((240, 196, 60), 6)},
    }


def add_animations():
    """The launch move: sucking in air (the guh puffs up and leans its head back), and flying (paws stretched out)."""
    path = os.path.join(ASSETS, "animations", "entity", "guh.animation.json")
    anim = json.load(open(path, encoding="utf-8"))
    anim["animations"]["animation.guh.suck"] = {"loop": "hold_on_last_frame", "animation_length": 4.0, "bones": {
        "body": {"scale": {"0.0": [1, 1, 1], "1.0": [1.1, 1.08, 1.1], "2.0": [1.2, 1.16, 1.2], "3.0": [1.3, 1.22, 1.3],
                           "3.6": [1.38, 1.28, 1.38], "3.8": [1.34, 1.26, 1.34], "4.0": [1.4, 1.3, 1.4]}},
        "head": {"rotation": {"0.0": [0, 0, 0], "0.4": [-22, 0, 0], "2.0": [-26, 0, 3], "3.0": [-26, 0, -3], "4.0": [-30, 0, 0]},
                 "scale": {"0.0": [1, 1, 1], "4.0": [1.12, 1.12, 1.12]}},
        "ear_left": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 20], "2.0": [0, 0, 10], "3.0": [0, 0, 25], "4.0": [0, 0, 30]}},
        "ear_right": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, -20], "2.0": [0, 0, -10], "3.0": [0, 0, -25], "4.0": [0, 0, -30]}},
        "tail": {"rotation": {"0.0": [0, 0, 0], "4.0": [30, 0, 0]}}}}
    anim["animations"]["animation.guh.fly"] = {"loop": True, "animation_length": 0.4, "bones": {
        "leg_front_left": {"rotation": {"0.0": [-70, 0, 0], "0.2": [-80, 0, 0], "0.4": [-70, 0, 0]}},
        "leg_front_right": {"rotation": {"0.0": [-70, 0, 0], "0.2": [-80, 0, 0], "0.4": [-70, 0, 0]}},
        "leg_back_left": {"rotation": {"0.0": [70, 0, 0], "0.2": [80, 0, 0], "0.4": [70, 0, 0]}},
        "leg_back_right": {"rotation": {"0.0": [70, 0, 0], "0.2": [80, 0, 0], "0.4": [70, 0, 0]}},
        "ear_left": {"rotation": {"0.0": [-40, 0, 20], "0.2": [-50, 0, 25], "0.4": [-40, 0, 20]}},
        "ear_right": {"rotation": {"0.0": [-40, 0, -20], "0.2": [-50, 0, -25], "0.4": [-40, 0, -20]}},
        "tail": {"rotation": {"0.0": [20, 0, 0], "0.2": [15, 0, 0], "0.4": [20, 0, 0]}},
        "body": {"scale": {"0.0": [1.05, 0.95, 1.1]}}}}
    # the ender guh's wings: big slow flaps in the air, folded on its back on the ground
    anim["animations"]["animation.guh.ender_flap"] = {"loop": True, "animation_length": 0.8, "bones": {
        "ender_wing_left": {"rotation": {"0.0": [0, 0, 35], "0.4": [0, -10, -30], "0.8": [0, 0, 35]}},
        "ender_wing_right": {"rotation": {"0.0": [0, 0, -35], "0.4": [0, 10, 30], "0.8": [0, 0, -35]}},
        "ender_wing_left_skin": {"rotation": {"0.0": [0, 0, 8], "0.4": [0, 0, -12], "0.8": [0, 0, 8]}},
        "ender_wing_right_skin": {"rotation": {"0.0": [0, 0, -8], "0.4": [0, 0, 12], "0.8": [0, 0, -8]}},
        "leg_front_left": {"rotation": {"0.0": [-30, 0, 0]}}, "leg_front_right": {"rotation": {"0.0": [-30, 0, 0]}},
        "leg_back_left": {"rotation": {"0.0": [35, 0, 0]}}, "leg_back_right": {"rotation": {"0.0": [35, 0, 0]}},
        "tail": {"rotation": {"0.0": [15, 0, 0], "0.4": [5, 0, 0], "0.8": [15, 0, 0]}}}}
    anim["animations"]["animation.guh.ender_rest"] = {"loop": True, "animation_length": 3.0, "bones": {
        "ender_wing_left": {"rotation": {"0.0": [0, -55, -20], "1.5": [0, -52, -24], "3.0": [0, -55, -20]}},
        "ender_wing_right": {"rotation": {"0.0": [0, 55, 20], "1.5": [0, 52, 24], "3.0": [0, 55, 20]}}}}
    for module in feature_modules():  # the features' own guh animations (e.g. the emotes)
        if hasattr(module, "animations"):
            anim["animations"].update(module.animations())
    with open(path, "w", encoding="utf-8") as f:
        json.dump(anim, f, indent=2)


def feature_modules():
    import sys
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import features
    return features.modules()


def main():
    for module in feature_modules():  # the 2.4 features' own clothes bones
        BONES.update(getattr(module, "BONES", {}))
        for bone in getattr(module, "BONES", {}):
            BONE_MODULE[bone] = module.__name__.split(".")[-1]
    add_animations()
    swatches = add_bones()
    base = Image.open(os.path.join(TEXTURES, "guh.png")).convert("RGBA")
    rng = np.random.default_rng(7)
    extra = {}
    for i, module in enumerate(feature_modules()):  # the 2.4 features' own guh variants (own rng: the others never change)
        if hasattr(module, "variants"):
            extra.update(module.variants(np.random.default_rng(2000 + i), sys.modules[__name__]))
    for name, (fur, painters) in list(variants(rng).items()) + list(extra.items()):
        img = recolour_fur(base, fur, sparkle=name == "golden", rng=rng) if fur else base.copy()
        a = np.asarray(img).copy()
        for swatch, paint in painters.items():
            u, v = swatches[swatch]
            a[v * 4:(v + SWATCH) * 4, u * 4:(u + SWATCH) * 4, :3] = paint().astype(np.uint8)
            a[v * 4:(v + SWATCH) * 4, u * 4:(u + SWATCH) * 4, 3] = 255
        Image.fromarray(a).save(os.path.join(TEXTURES, f"guh_{name}.png"))
        if name == "ender":
            px = a.astype(np.int32)
            blue = (px[..., 2] > 150) & (px[..., 2] - px[..., 0] > 60) & (px[..., 3] > 0)
            light = blue & (px[..., 1] > 180)
            a[blue & ~light, :3] = (190, 60, 255)
            a[light, :3] = (245, 175, 255)
            glow = np.zeros_like(a)
            glow[blue] = a[blue]
            Image.fromarray(a).save(os.path.join(TEXTURES, f"guh_{name}.png"))
            Image.fromarray(glow).save(os.path.join(TEXTURES, f"guh_{name}_glowmask.png"))
        if name == "starry":
            fur_mask = np.abs(np.asarray(img).astype(np.int32)[..., :3] - np.array((58, 40, 104))).sum(-1) < 60
            glow = np.zeros_like(a)
            twinkle = (rng.random(fur_mask.shape) < 0.012) & fur_mask
            a[twinkle, :3] = (255, 250, 210)
            glow[twinkle] = (255, 250, 210, 255)
            Image.fromarray(a).save(os.path.join(TEXTURES, f"guh_{name}.png"))
            Image.fromarray(glow).save(os.path.join(TEXTURES, f"guh_{name}_glowmask.png"))
    os.makedirs(os.path.join(TEXTURES, "guh_clothes"), exist_ok=True)
    for name, painters in clothes(rng).items():
        a = np.zeros((base.height, base.width, 4), np.uint8)
        for swatch, paint in painters.items():
            u, v = swatches[swatch]
            a[v * 4:(v + SWATCH) * 4, u * 4:(u + SWATCH) * 4, :3] = paint().astype(np.uint8)
            a[v * 4:(v + SWATCH) * 4, u * 4:(u + SWATCH) * 4, 3] = 255
        Image.fromarray(a).save(os.path.join(TEXTURES, "guh_clothes", f"{name}.png"))
    for i, module in enumerate(feature_modules()):  # each feature paints with its own rng, so the others never change
        if not hasattr(module, "clothes"):
            continue
        frng = np.random.default_rng(1000 + i)
        for name, painters in module.clothes(frng, sys.modules[__name__]).items():
            a = np.zeros((base.height, base.width, 4), np.uint8)
            for swatch, paint in painters.items():
                u, v = swatches[swatch]
                px = paint().astype(np.uint8)   # (a painter may give an alpha channel: see-through parts, e.g. glasses)
                a[v * 4:(v + SWATCH) * 4, u * 4:(u + SWATCH) * 4, :3] = px[..., :3]
                a[v * 4:(v + SWATCH) * 4, u * 4:(u + SWATCH) * 4, 3] = px[..., 3] if px.shape[-1] == 4 else 255
            Image.fromarray(a).save(os.path.join(TEXTURES, "guh_clothes", f"{name}.png"))
    print("guh variant bones + textures written:", ", ".join(variants(rng)), "+ clothes:", ", ".join(clothes(rng)))


if __name__ == "__main__":
    random.seed(7)
    main()
