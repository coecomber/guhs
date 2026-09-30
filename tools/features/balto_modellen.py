"""
Baltoguh en Nomguh (3.0 Guhverhalen, slice balto) - the models:

  NPCs (SittingGuhRenderers.NPC_MODELEN, registered in feature/balto/client/BaltoClient):
    boris           a real goose (NOT a guh): grey-brown back, a cream chest, a long neck, an orange beak with a dark nail, beady
                    eyes, orange webbed feet and a cosy brown fur hat with ear flaps (an ushanka). His own geo + animations
                    (animation.guh_sitting.idle / .happy: the names the NPC renderer plays).
    steele_mika     the Mika's model as a proud husky sled champion: grey-and-white fur, a red racing cap with amber goggles and
                    a golden medal on a blue ribbon; own animations for the Mika's bones (chin up, tail wagging).
    muk, luk        big white polar-bear guhs: the sitting guh in white fur, small round bear ears, a bear muzzle with a black
                    nose; Muk wears a blue knitted scarf, Luk a green one (and a little fishing rod).
    rosy            a baby guh (Kind scale 0.55) sitting in her little bed: headboard, pillow, a pink quilt with hearts, a
                    yellow scarf and a red little nose.
    witte_wolfguh   the white wolf-guh: glowing white fur with a pale blue shimmer, pointy wolf ears, a bushy tail, a ruff.
  Blocks (JSON models): the Baltoguh-beeldje (a bronze wolf-guh sitting proudly on a snowy stone pedestal), the route
  marker pole with its glowing guh-ear lamp, the medicine chest.

build(h) writes geo/entity/guh_npc_<kind>.geo.json, animations/entity/guh_npc_{boris,steele_mika}.animation.json,
textures/entity/npc_<kind>.png and the block models. preview(h, out) draws them offline (wiki_renders).
"""
import json
import math
import os

import numpy as np
from PIL import Image

from features import boerderij_dieren as bd
from features import knuffeldal_npcs as kn
from features import uvfix

FACES = ("north", "south", "east", "west", "up", "down")


# =====================================================================================================================
# helpers on the sitting guh (swatches in free 8x8 cells of its 128 UV sheet)
# =====================================================================================================================
def _sitting(h, identifier):
    geo_file = kn._load(h, "guh_sitting.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = f"geometry.{identifier}"
    return geo_file, geo


def _bone(geo, name):
    for b in geo["bones"]:
        if b["name"] == name:
            return b
    raise KeyError(name)


def _src(h, name):
    return Image.open(os.path.join(h.TEX, "entity", name)).convert("RGBA")


def _flat(block, colour, rng, var=6):
    block[..., :3] = np.clip(np.array(colour, np.float32) + rng.normal(0, var / 2, (block.shape[0], block.shape[1], 1)), 0, 255).astype(np.uint8)
    block[..., 3] = 255


def _wolf_ears(sw_outer, sw_inner, dy, dz):
    """Big pointy wolf ears around the guh ears (walking guh coordinates shifted by dy/dz): (left cubes, right cubes)."""
    left = [kn._cube([4.2, 10.5 + dy, -6.8 + dz], [7.8, 6, 2.6], sw_outer),
            kn._cube([5.2, 16.5 + dy, -6.6 + dz], [5.8, 3, 2.2], sw_outer),
            kn._cube([6.2, 19.5 + dy, -6.4 + dz], [3.8, 2.5, 1.8], sw_outer),
            kn._cube([7.0, 22 + dy, -6.2 + dz], [2.2, 1.8, 1.4], sw_outer),
            kn._cube([5.6, 12 + dy, -7.05 + dz], [5.0, 7, 0.3], sw_inner)]
    right = []
    for c in left:
        o, s = c["origin"], c["size"]
        r = dict(c)
        r["origin"] = [-(o[0] + s[0]), o[1], o[2]]
        right.append(r)
    return left, right


# =====================================================================================================================
# Boris the goose
# =====================================================================================================================
GANS_GRIJS = (150, 138, 128)
GANS_LICHT = (232, 226, 214)
GANS_DONKER = (96, 86, 80)
SNAVEL = (246, 140, 50)
SNAVEL_NAGEL = (70, 50, 40)
MUTS = (122, 84, 58)
MUTS_BONT = (214, 190, 160)


def _goose_eye(fur):
    """A goose's side of the head: a beady black eye with a white glint (no guh eyes: Boris is a real goose)."""
    def paint(W, H, rng):
        a = bd.plain(fur, 6)(W, H, rng).astype(np.float32)
        bd.disc(a, W * 0.45, H * 0.4, max(1.6, W * 0.14), (20, 18, 24))
        bd.disc(a, W * 0.42, H * 0.36, max(0.6, W * 0.05), (255, 255, 255))
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def _feathers(colour, dark):
    def paint(W, H, rng):
        a = bd.plain(colour, 8)(W, H, rng).astype(np.float32)
        for y in range(0, H, 6):
            for x in range((y // 6 % 2) * 4, W, 8):
                bd.disc(a, x + 3, y + 4, 3.2, dark, 0.35, ry=2)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def boris_geo(atlas):
    grijs = _feathers(GANS_GRIJS, GANS_DONKER)
    licht = bd.plain(GANS_LICHT, 6)
    donker = bd.plain(GANS_DONKER, 6)
    snavel = bd.plain(SNAVEL, 5)
    muts = bd.plain(MUTS, 8)
    bont = bd.plain(MUTS_BONT, 10)
    bones = [
        bd.bone("root", None, [0, 0, 0], []),
        # a round goose body: a long rounded barrel, a cream belly and chest, the wings folded on its sides
        bd.bone("body", "root", [0, 8, 0], [
            bd.cube(atlas, [-4.5, 5, -6], [9, 8, 14], grijs, overrides={"north": licht, "down": licht}),
            bd.cube(atlas, [-5, 6, -4.5], [10, 6, 11], grijs, overrides={"down": licht}),
            bd.cube(atlas, [-3.5, 4, -7], [7, 6, 5], licht),                      # the round cream chest
            bd.cube(atlas, [-3.5, 4.2, -5], [7, 1, 10], licht),                    # the belly
            bd.cube(atlas, [-3.8, 12.6, -4], [7.6, 1.2, 9], grijs)]),
        bd.bone("tail", "body", [0, 10, 8], [bd.cube(atlas, [-2.5, 9.5, 7.5], [5, 3, 3.5], donker, overrides={"up": grijs}),
                                             bd.cube(atlas, [-1.8, 11, 10.5], [3.6, 2, 2], licht)]),
        bd.bone("wing_left", "body", [5, 12, -3], [bd.cube(atlas, [4.6, 6.5, -4], [1.4, 6, 11], _feathers(GANS_DONKER, GANS_GRIJS)),
                                                   bd.cube(atlas, [4.4, 7, 6.5], [1.2, 3.5, 3], donker)]),
        bd.bone("wing_right", "body", [-5, 12, -3], [bd.cube(atlas, [-6, 6.5, -4], [1.4, 6, 11], _feathers(GANS_DONKER, GANS_GRIJS)),
                                                     bd.cube(atlas, [-5.6, 7, 6.5], [1.2, 3.5, 3], donker)]),
        # the long neck in two parts (a gentle S), the head with a long orange beak and beady eyes
        bd.bone("hals", "body", [0, 11, -5], [bd.cube(atlas, [-1.8, 10.5, -7.2], [3.6, 5.5, 3.6], grijs, overrides={"north": licht}),
                                              bd.cube(atlas, [-1.6, 15.5, -7.8], [3.2, 4.5, 3.2], grijs, overrides={"north": licht})]),
        bd.bone("head", "hals", [0, 20, -6], [
            bd.cube(atlas, [-2.5, 19.4, -9.6], [5, 4.6, 6.4], grijs, overrides={"east": _goose_eye(GANS_GRIJS), "west": _goose_eye(GANS_GRIJS),
                                                                               "north": licht}),
            bd.cube(atlas, [-2.6, 19.2, -9.8], [5.2, 1.4, 3], licht),              # a pale chin strap
            bd.cube(atlas, [-1.3, 20.2, -14], [2.6, 1.6, 4.6], snavel),
            bd.cube(atlas, [-1.1, 19.4, -13.4], [2.2, 0.8, 3.8], snavel),
            bd.cube(atlas, [-0.7, 20.6, -14.4], [1.4, 1.2, 0.6], bd.plain(SNAVEL_NAGEL, 4))]),
        # the fur hat with ear flaps and a little red star
        bd.bone("boris_muts", "head", [0, 24, -6], [
            bd.cube(atlas, [-3.1, 23.8, -9.9], [6.2, 2.6, 6.8], muts),
            bd.cube(atlas, [-3.5, 23.4, -10.3], [7, 1.2, 7.6], bont),
            bd.cube(atlas, [-3.7, 20.6, -8.6], [1.2, 3.4, 3.6], bont),
            bd.cube(atlas, [2.5, 20.6, -8.6], [1.2, 3.4, 3.6], bont),
            bd.cube(atlas, [-0.8, 24.6, -10.5], [1.6, 1.6, 0.4], bd.plain((230, 60, 70), 4))]),
    ]
    for side, x in (("left", 1.2), ("right", -3.2)):
        bones.append(bd.bone(f"leg_{side}", "root", [x + 1, 5, 0], [
            bd.cube(atlas, [x + 0.5, 0.5, -0.5], [1, 4.5, 1], snavel),
            bd.cube(atlas, [x - 0.4, 0, -2.6], [2.8, 0.6, 3.4], snavel)]))
    return bd.geo("guh_npc_boris", bones, 1.2, 1.8)


def boris_anims():
    idle = {"body": {"scale": {"0.0": [1, 1, 1], "2.0": [1.02, 1.03, 1.02], "4.0": [1, 1, 1]}},
            "hals": {"rotation": {"0.0": [0, 0, 0], "1.0": [6, 0, 0], "2.0": [0, 8, 0], "3.0": [-4, -6, 0], "4.0": [0, 0, 0]}},
            "tail": {"rotation": {"0.0": [0, 0, 0], "2.4": [0, 0, 0], "2.55": [0, 18, 0], "2.7": [0, -18, 0], "2.85": [0, 0, 0], "4.0": [0, 0, 0]}},
            "wing_left": {"rotation": {"0.0": [0, 0, 0], "3.2": [0, 0, 0], "3.4": [0, 0, -25], "3.6": [0, 0, 0], "4.0": [0, 0, 0]}},
            "wing_right": {"rotation": {"0.0": [0, 0, 0], "3.2": [0, 0, 0], "3.4": [0, 0, 25], "3.6": [0, 0, 0], "4.0": [0, 0, 0]}}}
    happy = {"wing_left": {"rotation": {"0.0": [0, 0, 0], "0.15": [0, 0, -60], "0.3": [0, 0, -10], "0.45": [0, 0, -55], "0.6": [0, 0, 0]}},
             "wing_right": {"rotation": {"0.0": [0, 0, 0], "0.15": [0, 0, 60], "0.3": [0, 0, 10], "0.45": [0, 0, 55], "0.6": [0, 0, 0]}},
             "hals": {"rotation": {"0.0": [0, 0, 0], "0.3": [-15, 0, 0], "0.6": [0, 0, 0]}}}
    return {"format_version": "1.8.0", "animations": {
        "animation.guh_sitting.idle": {"loop": True, "animation_length": 4.0, "bones": idle},
        "animation.guh_sitting.happy": {"loop": False, "animation_length": 0.6, "bones": happy}}}


def boris(h):
    atlas = bd.Atlas(20300711)
    h.w(os.path.join(h.A, "geckolib", "models", "entity", "guh_npc_boris.geo.json"), boris_geo(atlas))
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "guh_npc_boris.animation.json"), boris_anims())
    h.save(Image.fromarray(atlas.img), "entity", "npc_boris.png")


# =====================================================================================================================
# Steele-Mika
# =====================================================================================================================
def steele(h):
    geo_file = kn._load(h, "mika.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = "geometry.guh_npc_steele_mika"
    geo["bones"] = [b for b in geo["bones"] if not b["name"].startswith("steele_")]
    sw = kn._swatches(geo, ["pet", "klep", "bril", "band", "medaille", "lint"])
    # the racing cap (a peak to the front), the goggles on it, a golden medal on a blue ribbon under the chin
    geo["bones"].append({"name": "steele_pet", "parent": "head", "pivot": [0, 12, -6], "cubes": [
        kn._cube([-5.5, 13.8, -11.5], [11, 2.4, 9], sw["pet"]),
        kn._cube([-4.5, 16.2, -10.5], [9, 1.4, 7], sw["pet"]),
        kn._cube([-5, 13.8, -14.8], [10, 0.6, 3.4], sw["klep"]),
        kn._cube([-5.8, 13.0, -11.9], [11.6, 1.0, 0.6], sw["band"]),
        kn._cube([-4.6, 13.3, -12.3], [3.4, 1.8, 0.8], sw["bril"]),
        kn._cube([1.2, 13.3, -12.3], [3.4, 1.8, 0.8], sw["bril"])]})
    geo["bones"].append({"name": "steele_medaille", "parent": "head", "pivot": [0, 1, -8], "cubes": [
        kn._cube([-3.5, 0.6, -11.8], [7, 0.8, 0.6], sw["lint"]),
        kn._cube([-1.4, -2.2, -12.2], [2.8, 2.8, 0.6], sw["medaille"])]})
    kn._save_geo(h, "guh_npc_steele_mika.geo.json", geo_file)
    # a husky: dark blue-grey fur (the pink parts), the light parts stay light
    img = h.recolour(_src(h, "mika.png"), hue=0.62, sat=0.25, val=0.62, only=h.pinkish)
    a = np.asarray(img).copy()
    rng = np.random.default_rng(20300712)
    kn._paint(a, sw["pet"], (212, 44, 56), rng, 6)
    kn._paint(a, sw["klep"], (160, 30, 40), rng, 4)
    kn._paint(a, sw["band"], (250, 250, 252), rng, 3)
    kn._paint(a, sw["bril"], (255, 186, 60), rng, 6, _bril)
    kn._paint(a, sw["lint"], (60, 110, 220), rng, 5)
    kn._paint(a, sw["medaille"], (250, 204, 70), rng, 5, _medaille)
    h.save(Image.fromarray(a), "entity", "npc_steele_mika.png")
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "guh_npc_steele_mika.animation.json"), steele_anims())


def _bril(block):
    block[..., :3] = (255, 186, 60)
    block[4:10, 4:12, :3] = (255, 240, 200)
    block[:3, :, :3] = (40, 40, 48)
    block[-3:, :, :3] = (40, 40, 48)


def _medaille(block):
    block[..., :3] = (250, 204, 70)
    for yy in range(32):
        for xx in range(32):
            d = math.hypot(xx - 15.5, yy - 15.5)
            if d > 14:
                block[yy, xx, :3] = (190, 140, 40)
            elif 6 < d < 8:
                block[yy, xx, :3] = (255, 236, 150)
    block[12:20, 14:18, :3] = (190, 140, 40)       # a big "1"
    block[12:14, 12:16, :3] = (190, 140, 40)


def steele_anims():
    idle = {"body": {"scale": {"0.0": [1, 1, 1], "2.0": [1.02, 1.03, 1.02], "4.0": [1, 1, 1]}},
            "head": {"rotation": {"0.0": [-10, 0, 0], "1.0": [-12, 0, 4], "2.0": [-10, 0, 0], "3.0": [-12, 0, -4], "4.0": [-10, 0, 0]}},
            "tail": {"rotation": {"0.0": [0, -18, 0], "0.5": [0, 18, 0], "1.0": [0, -18, 0], "1.5": [0, 18, 0], "2.0": [0, -18, 0],
                                  "2.5": [0, 18, 0], "3.0": [0, -18, 0], "3.5": [0, 18, 0], "4.0": [0, -18, 0]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "2.6": [0, 0, 0], "2.75": [0, 0, -14], "2.9": [0, 0, 0], "4.0": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "3.1": [0, 0, 0], "3.25": [0, 0, 14], "3.4": [0, 0, 0], "4.0": [0, 0, 0]}}}
    happy = {"body": {"position": {"0.0": [0, 0, 0], "0.15": [0, 2, 0], "0.3": [0, 0, 0], "0.45": [0, 1.5, 0], "0.6": [0, 0, 0]}},
             "head": {"rotation": {"0.0": [-10, 0, 0], "0.3": [-22, 0, 0], "0.6": [-10, 0, 0]}}}
    return {"format_version": "1.8.0", "animations": {
        "animation.guh_sitting.idle": {"loop": True, "animation_length": 4.0, "bones": idle},
        "animation.guh_sitting.happy": {"loop": False, "animation_length": 0.6, "bones": happy}}}


# =====================================================================================================================
# Muk and Luk, the polar-bear guhs
# =====================================================================================================================
def ijsbeer(h, kind, sjaal_kleur, hengel=False):
    geo_file, geo = _sitting(h, f"guh_npc_{kind}")
    sw = kn._swatches(geo, ["snuit", "neus", "sjaal", "sjaal_rand", "hengel"])
    # small round bear ears instead of the big guh ears
    for side, sx in (("ear_left", 1), ("ear_right", -1)):
        b = _bone(geo, side)
        x0 = 4.2 if sx > 0 else -8.7
        uv = _bone_uv(geo, side)
        b["cubes"] = [{**kn._cube([x0, 24.6, -2.0], [4.5, 4.2, 2], (0, 0)), "uv": uv}]
    head = _bone(geo, "head")
    head["cubes"].append(kn._cube([-3.5, 14.2, -8.6], [7, 4.2, 2.4], sw["snuit"]))
    head["cubes"].append(kn._cube([-1.4, 17.2, -9.2], [2.8, 1.6, 0.8], sw["neus"]))
    geo["bones"].append({"name": f"{kind}_sjaal", "parent": "body", "pivot": [0, 13, 0], "cubes": [
        kn._cube([-5.6, 12.4, -4.8], [11.2, 2.6, 9.6], sw["sjaal"]),
        kn._cube([2.6, 6.8, -5.4], [2.6, 5.8, 1.2], sw["sjaal"]),
        kn._cube([2.6, 6.0, -5.5], [2.6, 0.9, 1.3], sw["sjaal_rand"])]})
    if hengel:
        geo["bones"].append({"name": f"{kind}_hengel", "parent": "arm_left", "pivot": [3.5, 10, -4], "cubes": [
            kn._cube([2.6, 8, -7.5], [0.8, 0.8, 1], sw["hengel"]),
            kn._cube([2.6, 8.4, -8.5], [0.8, 12, 0.8], sw["hengel"])]})
    kn._save_geo(h, f"guh_npc_{kind}.geo.json", geo_file)
    img = h.recolour(_src(h, "guh_sitting.png"), hue=0.58, sat=0.08, val=1.06, only=h.pinkish)
    a = np.asarray(img).copy()
    rng = np.random.default_rng(20300713 if kind == "muk" else 20300714)
    kn._paint(a, sw["snuit"], (250, 246, 238), rng, 5)
    kn._paint(a, sw["neus"], (30, 28, 36), rng, 3)
    kn._paint(a, sw["sjaal"], sjaal_kleur, rng, 8, _gebreid(sjaal_kleur))
    kn._paint(a, sw["sjaal_rand"], (250, 250, 250), rng, 4)
    kn._paint(a, sw["hengel"], (150, 104, 62), rng, 6)
    h.save(Image.fromarray(a), "entity", f"npc_{kind}.png")


def _bone_uv(geo, name):
    """The uv of the first cube of a bone (an ear keeps its own ear texture)."""
    return _bone(geo, name)["cubes"][0]["uv"] if _bone(geo, name)["cubes"] else {}


def _gebreid(colour):
    def pattern(block):
        for y in range(0, 32, 4):
            block[y, :, :3] = np.clip(np.array(colour) * 0.8, 0, 255)
        for x in range(0, 32, 8):
            block[:, x, :3] = np.clip(np.array(colour) * 0.9, 0, 255)
    return pattern


# =====================================================================================================================
# Rosy in her little bed
# =====================================================================================================================
def rosy(h):
    geo_file, geo = _sitting(h, "guh_npc_rosy")
    sw = kn._swatches(geo, ["hout", "matras", "deken", "kussen", "sjaal", "neus"])
    geo["bones"].append({"name": "rosy_bedje", "parent": "root", "pivot": [0, 0, 0], "cubes": [
        kn._cube([-10, 0, -12], [20, 2.5, 26], sw["hout"]),                  # the frame
        kn._cube([-9.5, 2.5, -11.5], [19, 1.5, 25], sw["matras"]),
        kn._cube([-10, 0, 13], [20, 16, 1.6], sw["hout"]),                   # the headboard (behind her)
        kn._cube([-10, 15.6, 12.6], [20, 1.4, 2.4], sw["hout"]),
        kn._cube([-10, 0, -13.4], [20, 8, 1.6], sw["hout"]),                 # the footboard
        kn._cube([-10.6, 0, -13.4], [1.2, 10, 1.2], sw["hout"]), kn._cube([9.4, 0, -13.4], [1.2, 10, 1.2], sw["hout"]),
        kn._cube([-10.6, 0, 13.4], [1.2, 18, 1.2], sw["hout"]), kn._cube([9.4, 0, 13.4], [1.2, 18, 1.2], sw["hout"]),
        kn._cube([-7.5, 4, 5], [15, 5, 7.5], sw["kussen"]),                  # the pillow behind her back
        kn._cube([-9, 3.8, -11.5], [18, 3.4, 12], sw["deken"])]})             # the quilt over her lap
    geo["bones"].append({"name": "rosy_sjaal", "parent": "body", "pivot": [0, 13, 0], "cubes": [
        kn._cube([-5.4, 12.2, -4.6], [10.8, 2.4, 9.2], sw["sjaal"]),
        kn._cube([-4.6, 7.0, -5.2], [2.4, 5.4, 1.1], sw["sjaal"])]})
    head = _bone(geo, "head")
    head["cubes"].append(kn._cube([-1.2, 17.0, -7.9], [2.4, 1.8, 0.4], sw["neus"]))   # a red little sniffle nose
    kn._save_geo(h, "guh_npc_rosy.geo.json", geo_file)
    img = h.recolour(_src(h, "guh_sitting.png"), hue=0.95, sat=0.85, val=1.05, only=h.pinkish)
    a = np.asarray(img).copy()
    rng = np.random.default_rng(20300715)
    kn._paint(a, sw["hout"], (196, 146, 100), rng, 8)
    kn._paint(a, sw["matras"], (250, 248, 244), rng, 4)
    kn._paint(a, sw["deken"], (246, 164, 196), rng, 5, _hartjes)
    kn._paint(a, sw["kussen"], (255, 252, 250), rng, 3)
    kn._paint(a, sw["sjaal"], (252, 214, 80), rng, 6, _gebreid((252, 214, 80)))
    kn._paint(a, sw["neus"], (236, 80, 96), rng, 4)
    h.save(Image.fromarray(a), "entity", "npc_rosy.png")


def _hartjes(block):
    for cy in (7, 23):
        for cx in (7, 23):
            for dy in range(-3, 4):
                for dx in range(-3, 4):
                    if (abs(dx) + abs(dy) <= 3 and dy >= -1) or (dy == -2 and abs(dx) in (1, 2)):
                        block[cy + dy, cx + dx, :3] = (255, 250, 252)


# =====================================================================================================================
# the white wolf-guh
# =====================================================================================================================
def witte_wolf(h):
    geo_file, geo = _sitting(h, "guh_npc_witte_wolfguh")
    sw = kn._swatches(geo, ["oor", "oor_binnen", "staart", "kraag"])
    left, right = _wolf_ears(sw["oor"], sw["oor_binnen"], 13, 4.5)
    _bone(geo, "ear_left")["cubes"] += left
    _bone(geo, "ear_right")["cubes"] += right
    geo["bones"].append({"name": "wolf_staart", "parent": "body", "pivot": [0, 1.5, 4], "cubes": [
        kn._cube([-1.8, 0.8, 4], [3.6, 3, 5], sw["staart"]),
        kn._cube([-2.2, 1.2, 8.5], [4.4, 3.4, 4], sw["staart"]),
        kn._cube([-1.6, 1.8, 12.2], [3.2, 2.6, 2.4], sw["kraag"])]})
    geo["bones"].append({"name": "wolf_kraag", "parent": "body", "pivot": [0, 12, 0], "cubes": [
        kn._cube([-5.8, 10.5, -4.8], [11.6, 3, 9.6], sw["kraag"], inflate=0.2)]})
    kn._save_geo(h, "guh_npc_witte_wolfguh.geo.json", geo_file)
    img = h.recolour(_src(h, "guh_sitting.png"), hue=0.58, sat=0.12, val=1.1, only=h.pinkish)
    a = np.asarray(img).copy()
    rng = np.random.default_rng(20300716)
    # a soft icy shimmer on the fur
    fur = (a[..., 3] > 0) & (a[..., :3].min(-1) > 190)
    glint = (rng.random(fur.shape) < 0.03) & fur
    a[glint, :3] = (200, 230, 255)
    kn._paint(a, sw["oor"], (244, 248, 255), rng, 5)
    kn._paint(a, sw["oor_binnen"], (190, 220, 255), rng, 5)
    kn._paint(a, sw["staart"], (240, 246, 255), rng, 8)
    kn._paint(a, sw["kraag"], (252, 254, 255), rng, 6)
    h.save(Image.fromarray(a), "entity", "npc_witte_wolfguh.png")


# =====================================================================================================================
# block models
# =====================================================================================================================
def el(frm, to, tex, faces=None, uv=None, rot=None):
    e = {"from": frm, "to": to, "faces": {f: ({"texture": tex, "uv": uv} if uv else {"texture": tex}) for f in (faces or FACES)}}
    if rot:
        e["rotation"] = rot
    return e


def beeldje_elements():
    """The statue (facing north): a snowy stone pedestal, the bronze Baltoguh sitting proudly on it, pointy ears up, the
    bushy tail curled round his paws, a little scarf knot."""
    B, S, F = "#brons", "#sokkel", "#gezicht"
    e = [el([1, 0, 1], [15, 4, 15], S), el([2, 4, 2], [14, 5, 14], S),
         el([4.5, 5, 5], [11.5, 11.5, 12.5], B),                   # the body
         el([5, 5, 3.2], [11, 10, 6], B),                          # the chest
         el([5.2, 5, 2.4], [7.2, 6.8, 5], B), el([8.8, 5, 2.4], [10.8, 6.8, 5], B),   # the front paws
         el([4.2, 5, 9], [6.2, 7.5, 12.8], B), el([9.8, 5, 9], [11.8, 7.5, 12.8], B),  # the back legs
         el([3.8, 11, 2.2], [12.2, 17.6, 9.4], B, faces=("south", "east", "west", "up", "down")),
         {"from": [3.8, 11, 2.2], "to": [12.2, 17.6, 9.4], "faces": {"north": {"texture": F, "uv": [0, 0, 16, 16]}}},
         el([6.6, 11.6, 1.2], [9.4, 13.8, 2.3], B),                # the guhsnoet
         el([4.4, 10.4, 3], [11.6, 11.4, 8.6], "#sjaal"),           # the scarf
         el([4.0, 16.8, 4.4], [6.8, 19.4, 7.0], B), el([4.6, 19.4, 4.8], [6.2, 21.6, 6.6], B),     # left ear
         el([9.2, 16.8, 4.4], [12.0, 19.4, 7.0], B), el([9.8, 19.4, 4.8], [11.4, 21.6, 6.6], B),   # right ear
         el([7, 5, 12], [9, 7.2, 15.2], B), el([5.4, 5, 13.2], [8.2, 6.8, 15.2], B)]            # the tail curling round
    return e


def block_models(h):
    A, w = h.A, h.w
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    tex = {"particle": "guhs:block/baltoguh_beeldje_brons", "brons": "guhs:block/baltoguh_beeldje_brons",
           "sokkel": "guhs:block/baltoguh_beeldje_sokkel", "gezicht": "guhs:block/baltoguh_beeldje_gezicht",
           "sjaal": "minecraft:block/red_wool"}
    w(f"{A}/models/block/baltoguh_beeldje.json", {"parent": "minecraft:block/block", "textures": tex, "elements": uvfix.binnen(beeldje_elements()),
                                                  "display": {"gui": {"rotation": [30, 200, 0], "translation": [0, -2, 0], "scale": [0.55, 0.55, 0.55]},
                                                              "ground": {"translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
                                                              "fixed": {"scale": [0.6, 0.6, 0.6]},
                                                              "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
                                                              "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.36, 0.36, 0.36]}}})
    w(f"{A}/blockstates/baltoguh_beeldje.json", {"variants": {f"facing={f}": {"model": "guhs:block/baltoguh_beeldje", **({"y": r} if r else {})}
                                                               for f, r in rot.items()}})
    w(f"{A}/models/item/baltoguh_beeldje.json", {"parent": "guhs:block/baltoguh_beeldje"})
    # the route marker: the striped pole and the glowing lamp (a rounded guh ear on top)
    t = "#paal"
    w(f"{A}/models/block/nomguh_routepaal.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                  "textures": {"particle": "guhs:block/nomguh_routepaal", "paal": "guhs:block/nomguh_routepaal"},
                                                  "elements": [
        el([7, 0, 7], [9, 12.5, 9], t, uv=[0, 0, 2, 12.5]),
        el([6.5, 12.5, 6.5], [9.5, 13.5, 9.5], t, uv=[6, 10, 16, 16]),
        el([6.5, 13.5, 7.2], [9.5, 16, 8.8], t, uv=[6, 0, 16, 9]),
        el([7.2, 13.5, 6.5], [8.8, 15.4, 9.5], t, uv=[7, 1, 15, 8])]})
    w(f"{A}/blockstates/nomguh_routepaal.json", {"variants": {"": {"model": "guhs:block/nomguh_routepaal"}}})
    w(f"{A}/models/item/nomguh_routepaal.json", {"parent": "guhs:block/nomguh_routepaal"})
    # the medicine chest
    k = "#kist"
    w(f"{A}/models/block/nomguh_medicijnkist.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/nomguh_medicijnkist", "kist": "guhs:block/nomguh_medicijnkist", "hout": "minecraft:block/spruce_planks"},
        "elements": [el([2, 0, 4], [14, 7.5, 12], k, uv=[0, 0, 16, 16]),
                     el([1.6, 7.5, 3.6], [14.4, 9, 12.4], "#hout"),
                     el([7, 9, 7.4], [9, 10, 8.6], "#hout")],
        "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 1.5, 0], "scale": [0.8, 0.8, 0.8]}}})
    w(f"{A}/blockstates/nomguh_medicijnkist.json", {"variants": {f"facing={f}": {"model": "guhs:block/nomguh_medicijnkist", **({"y": r} if r else {})}
                                                                  for f, r in rot.items()}})
    w(f"{A}/models/item/nomguh_medicijnkist.json", {"parent": "guhs:block/nomguh_medicijnkist"})


def build(h):
    boris(h)
    steele(h)
    ijsbeer(h, "muk", (70, 120, 220))
    ijsbeer(h, "luk", (80, 180, 110), hengel=True)
    rosy(h)
    witte_wolf(h)
    block_models(h)


# =====================================================================================================================
# offline previews (python -c "...balto_modellen.preview(...)")
# =====================================================================================================================
def preview(out):
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    geo = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity")
    for kind in ("boris", "steele_mika", "muk", "luk", "rosy", "witte_wolfguh"):
        q = wr.geo_quads(os.path.join(geo, f"guh_npc_{kind}.geo.json"), f"guhs:entity/npc_{kind}")
        for yaw in (35, 160):
            wr.render(q, yaw=yaw, pitch=-15, size=384).save(os.path.join(out, f"npc_{kind}_{yaw}.png"))
