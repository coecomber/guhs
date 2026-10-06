"""
bbq2 (paleizen) - the looks: the Worstzwijntje, the Mopper-Mika's, the three characters, the blocks and the item icons.

  worstzwijntje        a sweet hoglin parody: a braadworst on four stubby legs. A long round body in sausage colours with
                       grill stripes and a squiggle of mustard down its back (like on a hot dog), a knot at its tail end with
                       a curly tail, a guh face with a big flat snout, two tiny tusks, round guh ears with a nick. Its own geo,
                       animations (idle, walk, blij, eet, geluid: the names BoerderijDier plays) and texture.
  paleizen_mopper_mika the grumpy neighbours of the Mika-woonblokken: the Mika's model with six sets of things (bones
                       mopper_0 .. mopper_5: a flat cap, curlers, a nightcap with a pompon, a scarf, a chef's hat, sunglasses);
                       the renderer shows the set of the neighbour's number (feature/paleizen/client/PaleizenClient).
  mika_oma             the Mika's model in soft grey-lilac: a bun with two knitting needles through it, round glasses, a
                       crocheted shawl, a ball of wool at her feet.
  tolwachter_mika      a Nether-Mika in uniform: a blue cap with a golden badge, a big moustache, a coin bag on a strap.
  stalknechtguh        the sitting guh in hay colours: a tweed cap, a red neckerchief, dungarees and a hay fork.
  blocks               paleizen_brugplank (planks tied with rope), paleizen_brugleuning (a rope rail, a fence),
                       paleizen_breiwerk (a basket with balls of wool and two needles).
  items                the icons of the quest items, the basket, the recipe card and the spawn egg.

build(h) writes them all; preview() (python tools/features/paleizen_modellen.py) is not needed: wiki_renders draws geo files.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw

from features import beroepen_tex
from features import boerderij_dieren as bd
from features import knuffeldal_npcs as kn

# --- colours ----------------------------------------------------------------------------------------------------------
WORST = (226, 150, 112)            # the braadworst's skin
WORST_LICHT = (244, 190, 150)      # its belly
WORST_STREEP = (150, 82, 56)       # grill stripes
MOSTERD = (248, 208, 64)
SNUIT = (250, 170, 180)
SNUIT_GAT = (170, 80, 104)
SLAGTAND = (255, 250, 236)
HOEFJE = (110, 66, 50)
OOR_BINNEN = (240, 140, 160)
TOUW = (214, 186, 130)


# =====================================================================================================================
# the Worstzwijntje
# =====================================================================================================================
def _worstvel(strepen=True, mosterd=False):
    """Sausage skin: a warm pink-brown with a soft shine, darker grill stripes across it, optionally the mustard squiggle."""
    def paint(W, H, rng):
        a = bd.plain(WORST, 8)(W, H, rng).astype(np.float32)
        if strepen:
            for x in range(8, W, 18):
                for y in range(H):
                    for dx in range(3):
                        if 0 <= x + dx < W:
                            a[y, x + dx, :3] = a[y, x + dx, :3] * 0.5 + np.array(WORST_STREEP) * 0.5
        if mosterd:
            for x in range(W):
                y = H / 2 + math.sin(x / 3.2) * H * 0.22
                for dy in (-1, 0, 1):
                    yy = int(y + dy)
                    if 0 <= yy < H:
                        a[yy, x, :3] = MOSTERD if dy else (255, 232, 120)
        return np.clip(a, 0, 255).astype(np.uint8)
    return paint


def _snuit(W, H, rng):
    a = bd.plain(SNUIT, 6)(W, H, rng).astype(np.float32)
    for sx in (-1, 1):
        bd.disc(a, W / 2 + sx * W * 0.2, H * 0.48, W * 0.085, SNUIT_GAT, 1.0, ry=H * 0.2)
    for x in range(int(W * 0.3), int(W * 0.7)):
        a[int(H * 0.84), x, :3] = (214, 120, 140)
    return np.clip(a, 0, 255).astype(np.uint8)


def _knoop(W, H, rng):
    """The tied end of the sausage: wrinkles running to the middle."""
    a = bd.plain(WORST_STREEP, 8)(W, H, rng).astype(np.float32)
    for i in range(0, max(W, H), 3):
        for t in range(max(W, H)):
            x, y = int(W / 2 + (i - W / 2) * t / max(W, H)), int(H * t / max(W, H))
            if 0 <= x < W and 0 <= y < H:
                a[y, x, :3] = (110, 60, 44)
    return np.clip(a, 0, 255).astype(np.uint8)


def worstzwijntje_geo(atlas):
    vel = _worstvel()
    rug = _worstvel(strepen=True, mosterd=True)
    buik = bd.plain(WORST_LICHT, 8)
    kop = bd.plain(WORST, 8)
    hoef = bd.plain(HOEFJE, 6)
    tand = bd.plain(SLAGTAND, 4)
    bones = [
        bd.bone("root", None, [0, 0, 0], []),
        # the sausage: a long barrel, rounded with a second, slimmer and longer box, a light belly
        bd.bone("body", "root", [0, 7, 0], [
            bd.cube(atlas, [-4.5, 3.5, -6], [9, 7.5, 13], vel, overrides={"up": rug, "down": buik}),
            bd.cube(atlas, [-3.8, 4.2, -7], [7.6, 6.1, 15], vel, overrides={"up": rug, "down": buik}),
            bd.cube(atlas, [-3.2, 2.9, -5], [6.4, 1, 11], buik)]),
        # the tied end with a little curly tail
        bd.bone("tail", "body", [0, 8, 8], [
            bd.cube(atlas, [-1.4, 6, 8], [2.8, 2.8, 1.6], bd.plain(WORST_STREEP, 8), overrides={"south": _knoop}),
            bd.cube(atlas, [-0.5, 8.4, 9.2], [1, 2.2, 1], bd.plain(WORST, 6)),
            bd.cube(atlas, [-0.5, 10.2, 8.2], [1, 1, 2], bd.plain(WORST, 6)),
            bd.cube(atlas, [-0.5, 9.2, 7.6], [1, 1, 1], bd.plain(WORST, 6))]),
        # the head: a guh face, a broad flat snout, two tusks pointing up beside it
        bd.bone("head", "body", [0, 8, -6], [
            bd.cube(atlas, [-4.5, 3.6, -12.5], [9, 8, 6.5], kop, overrides={"north": bd.guh_face(WORST, eye_y=0.36, eye_dx=0.25, eye_r=0.17, snoet=False)}),
            bd.cube(atlas, [-3, 4, -14.2], [6, 3.2, 1.8], bd.plain(SNUIT, 6), overrides={"north": _snuit}),
            bd.cube(atlas, [-4.1, 4.6, -13.6], [1, 2.4, 1], tand),
            bd.cube(atlas, [3.1, 4.6, -13.6], [1, 2.4, 1], tand)]),
    ]
    bones += bd.ears(atlas, WORST, OOR_BINNEN, 3.6, 10.4, -10.5, 3.4, 3.4)
    for name, x, z in (("leg_front_left", 1.6, -5), ("leg_front_right", -3.9, -5), ("leg_back_left", 1.6, 4), ("leg_back_right", -3.9, 4)):
        bones.append(bd.bone(name, "body", [x + 1.15, 4, z + 1.15], [
            bd.cube(atlas, [x, 0, z], [2.3, 4.2, 2.3], bd.plain(WORST, 8), overrides={"down": hoef}),
            bd.cube(atlas, [x - 0.1, 0, z - 0.1], [2.5, 1.2, 2.5], hoef)]))
    return bd.geo("worstzwijntje", bones, 1.3, 1.2)


def worstzwijntje_anims():
    curl = {"tail": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 22, 0], "1.0": [0, -22, 0], "1.5": [0, 22, 0], "2.0": [0, -22, 0],
                                  "2.5": [0, 0, 0], "3.0": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "1.2": [6, 0, 0], "1.5": [10, 6, 0], "1.8": [10, -6, 0], "2.1": [6, 0, 0], "3.0": [0, 0, 0]}}}
    a = bd.animations("worstzwijntje", ["leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"], extra_idle=curl,
                      walk_extra={"tail": {"rotation": {"0.0": [0, 18, 0], "0.35": [0, -18, 0], "0.7": [0, 18, 0]}},
                                  "head": {"rotation": {"0.0": [4, 0, 0], "0.35": [8, 0, 0], "0.7": [4, 0, 0]}}},
                      blij_extra={"tail": {"rotation": {"0.0": [0, 0, 0], "0.1": [0, 35, 0], "0.2": [0, -35, 0], "0.3": [0, 35, 0],
                                                        "0.4": [0, -35, 0], "0.6": [0, 0, 0]}}})
    # onrustig: a nervous shuffle (the stable animals before they are petted calm), a loop
    a["animations"]["onrustig"] = {"loop": True, "animation_length": 0.8, "bones": {
        "body": {"position": {"0.0": [0, 0, 0], "0.1": [0.6, 0.8, 0], "0.2": [0, 0, 0], "0.3": [-0.6, 0.8, 0], "0.4": [0, 0, 0], "0.8": [0, 0, 0]},
                 "rotation": {"0.0": [0, 0, 0], "0.1": [0, 0, 4], "0.3": [0, 0, -4], "0.4": [0, 0, 0], "0.8": [0, 0, 0]}},
        "head": {"rotation": {"0.0": [0, 14, 0], "0.2": [0, -14, 0], "0.4": [0, 14, 0], "0.6": [0, -14, 0], "0.8": [0, 14, 0]}},
        "tail": {"rotation": {"0.0": [0, 30, 0], "0.1": [0, -30, 0], "0.2": [0, 30, 0], "0.3": [0, -30, 0], "0.4": [0, 30, 0], "0.8": [0, 30, 0]}},
        "ear_left": {"rotation": {"0.0": [0, 0, -12], "0.4": [0, 0, 6], "0.8": [0, 0, -12]}},
        "ear_right": {"rotation": {"0.0": [0, 0, 12], "0.4": [0, 0, -6], "0.8": [0, 0, 12]}}}}
    return a


def worstzwijntje(h):
    atlas = bd.Atlas(21300951)
    h.w(f"{h.A}/geckolib/models/entity/worstzwijntje.geo.json", worstzwijntje_geo(atlas))
    h.w(f"{h.A}/geckolib/animations/entity/worstzwijntje.animation.json", worstzwijntje_anims())
    h.save(Image.fromarray(atlas.img), "entity", "worstzwijntje.png")


# =====================================================================================================================
# the Mika's: the Mopper-Mika's, Mika-oma, the Tolwachter-Mika
# =====================================================================================================================
def _src(h, name):
    return Image.open(os.path.join(h.TEX, "entity", name)).convert("RGBA")


def _mika(h, identifier):
    geo_file = kn._load(h, "mika.geo.json")
    geo = geo_file["minecraft:geometry"][0]
    geo["description"]["identifier"] = f"geometry.{identifier}"
    return geo_file, geo


def _ruit(a, b):
    def f(block):
        for yy in range(32):
            for xx in range(32):
                block[yy, xx, :3] = a if ((xx // 8) + (yy // 8)) % 2 else b
    return f


def _streep(kleur, stap=8, dik=3):
    def f(block):
        for yy in range(0, 32, stap):
            block[yy:yy + dik, :, :3] = kleur
    return f


def _brei(licht):
    """A knitted look: little V stitches."""
    def f(block):
        for yy in range(0, 32, 4):
            for xx in range(0, 32, 4):
                block[yy, xx, :3] = licht
                block[yy + 1, xx + 1, :3] = licht
                block[yy, xx + 2, :3] = licht
    return f


def _glas(block):
    block[..., :3] = (60, 50, 60)
    block[5:27, 5:27, :3] = (206, 232, 244)
    block[8:12, 8:16, :3] = (255, 255, 255)


def _zonnebril(block):
    block[..., :3] = (20, 20, 26)
    block[6:10, 6:20, :3] = (90, 110, 150)


def _badge(block):
    block[..., :3] = (60, 90, 170)
    for yy in range(32):
        for xx in range(32):
            if abs(xx - 15.5) + abs(yy - 15.5) < 11:
                block[yy, xx, :3] = (250, 204, 70)
            if abs(xx - 15.5) + abs(yy - 15.5) < 5:
                block[yy, xx, :3] = (190, 140, 40)


def _munt(block):
    block[..., :3] = (156, 110, 60)
    for (cx, cy) in ((10, 12), (22, 18), (14, 24)):
        for yy in range(32):
            for xx in range(32):
                if math.hypot(xx - cx, yy - cy) < 5:
                    block[yy, xx, :3] = (250, 204, 70)


def mopper_mika(h):
    """The Mika's model with six sets of things; texture: the Nether-Mika's fur with the swatches painted in."""
    geo_file, geo = _mika(h, "paleizen_mopper_mika")
    sw = kn._swatches(geo, ["pet", "pet_klep", "krul", "krul_pen", "muts", "pompon", "sjaal", "sjaal_rand", "kok", "kok_rand", "bril", "krant"])
    H = [0, 12, -6]
    c = kn._cube
    sets = {
        # 0 Brom-Mika: a tweed flat cap pulled low, a rolled-up newspaper under his paw
        "mopper_0": [c([-6.2, 12.2, -12.6], [12.4, 1.8, 11.6], sw["pet"]), c([-5.2, 14.0, -11.6], [10.4, 0.9, 9.6], sw["pet"]),
                     c([-5.6, 12.2, -15.2], [11.2, 0.6, 2.8], sw["pet_klep"]), c([3.0, 0.2, -16.2], [5.6, 1.4, 1.4], sw["krant"])],
        # 1 Zeur-Mika: curlers in her fur, a row of three, pins through them
        "mopper_1": [c([-6.0, 12.4, -10.8], [3.2, 2.2, 2.2], sw["krul"]), c([-1.6, 12.6, -10.8], [3.2, 2.2, 2.2], sw["krul"]),
                     c([2.8, 12.4, -10.8], [3.2, 2.2, 2.2], sw["krul"]), c([-4.0, 12.4, -7.4], [3.2, 2.2, 2.2], sw["krul"]),
                     c([0.8, 12.4, -7.4], [3.2, 2.2, 2.2], sw["krul"]), c([-6.6, 13.2, -10.0], [13.2, 0.4, 0.4], sw["krul_pen"]),
                     c([-4.6, 13.2, -6.6], [9.2, 0.4, 0.4], sw["krul_pen"])],
        # 2 Snurk-Mika: a striped nightcap that flops to one side, a pompon on its end
        "mopper_2": [c([-6.0, 12.2, -11.6], [12, 1.6, 9.6], sw["muts"]), c([-4.6, 13.8, -10.4], [9.2, 1.6, 7.6], sw["muts"]),
                     c([-1.6, 15.4, -8.8], [6.4, 1.4, 4.8], sw["muts"]), c([3.6, 14.2, -7.8], [4.2, 1.4, 2.8], sw["muts"]),
                     c([7.0, 11.6, -7.6], [1.6, 3.2, 2.2], sw["muts"]), c([6.4, 9.4, -8.0], [2.8, 2.6, 2.8], sw["pompon"])],
        # 3 the neighbour on the gallery: a long knitted scarf
        "mopper_3": [c([-7.4, 1.8, -3.0], [14.8, 2.6, 4.6], sw["sjaal"]), c([3.6, -0.4, -4.2], [2.6, 3.0, 1.2], sw["sjaal"]),
                     c([3.6, -1.0, -4.2], [2.6, 0.7, 1.2], sw["sjaal_rand"])],
        # 4 the neighbour at the barbecue: a chef's hat
        "mopper_4": [c([-4.4, 12.2, -10.6], [8.8, 2.2, 7.6], sw["kok_rand"]), c([-5.2, 14.4, -11.4], [10.4, 3.4, 9.2], sw["kok"])],
        # 5 the neighbour on the roof: sunglasses and a little parasol hat
        "mopper_5": [c([-6.4, 6.0, -12.7], [5.2, 3.2, 0.6], sw["bril"]), c([1.2, 6.0, -12.7], [5.2, 3.2, 0.6], sw["bril"]),
                     c([-1.2, 7.4, -12.7], [2.4, 0.7, 0.6], sw["krul_pen"])],
    }
    for name, cubes in sets.items():
        geo["bones"].append({"name": name, "parent": "body" if name == "mopper_3" else "head", "pivot": H, "cubes": cubes})
    kn._save_geo(h, "paleizen_mopper_mika.geo.json", geo_file)
    a = np.asarray(_src(h, "nether_mika.png")).copy()
    rng = np.random.default_rng(21300952)
    p = kn._paint
    p(a, sw["pet"], (112, 122, 96), rng, 10, _ruit((96, 106, 84), (128, 138, 108)))
    p(a, sw["pet_klep"], (78, 86, 66), rng, 6)
    p(a, sw["krant"], (236, 232, 220), rng, 6, _streep((90, 90, 96), 6, 1))
    p(a, sw["krul"], (240, 130, 190), rng, 8, _streep((250, 180, 220), 8, 2))
    p(a, sw["krul_pen"], (70, 70, 80), rng, 4)
    p(a, sw["muts"], (240, 240, 246), rng, 6, _streep((90, 130, 220), 8, 4))
    p(a, sw["pompon"], (250, 250, 252), rng, 12)
    p(a, sw["sjaal"], (214, 60, 70), rng, 8, _brei((240, 110, 110)))
    p(a, sw["sjaal_rand"], (250, 220, 120), rng, 6)
    p(a, sw["kok"], (250, 250, 250), rng, 5, _streep((226, 226, 232), 8, 1))
    p(a, sw["kok_rand"], (236, 236, 240), rng, 4)
    p(a, sw["bril"], (20, 20, 26), rng, 3, _zonnebril)
    h.save(Image.fromarray(a), "entity", "paleizen_mopper_mika.png")


def mika_oma(h):
    geo_file, geo = _mika(h, "guh_npc_mika_oma")
    sw = kn._swatches(geo, ["haar", "knot", "naald", "bril", "brilrand", "sjaal", "sjaal_rand", "wol", "wol2"])
    c = kn._cube
    H = [0, 12, -6]
    geo["bones"].append({"name": "oma_haar", "parent": "head", "pivot": H, "cubes": [
        c([-6.4, 12.3, -11.6], [12.8, 0.9, 9.8], sw["haar"]),
        c([-2.6, 13.0, -6.4], [5.2, 3.6, 5.0], sw["knot"]),
        c([-1.8, 16.4, -5.6], [3.6, 0.8, 3.4], sw["knot"])]})
    geo["bones"].append({"name": "oma_naalden", "parent": "head", "pivot": [0, 14.6, -4], "cubes": [
        {**c([-5.4, 14.4, -4.2], [10.8, 0.45, 0.45], sw["naald"]), "pivot": [0, 14.6, -4], "rotation": [0, 0, 18]},
        {**c([-5.4, 14.4, -3.4], [10.8, 0.45, 0.45], sw["naald"]), "pivot": [0, 14.6, -3.2], "rotation": [0, 0, -18]}]})
    geo["bones"].append({"name": "oma_bril", "parent": "head", "pivot": H, "cubes": [
        c([-6.0, 5.4, -12.7], [4.8, 4.2, 0.5], sw["bril"]), c([1.2, 5.4, -12.7], [4.8, 4.2, 0.5], sw["bril"]),
        c([-1.2, 7.4, -12.7], [2.4, 0.6, 0.5], sw["brilrand"]), c([-7.2, 7.4, -12.6], [1.2, 0.6, 5.0], sw["brilrand"]),
        c([6.0, 7.4, -12.6], [1.2, 0.6, 5.0], sw["brilrand"])]})
    geo["bones"].append({"name": "oma_sjaal", "parent": "body", "pivot": [0, 6, 2], "cubes": [
        c([-7.2, 5.6, -2.6], [14.4, 3.6, 8.2], sw["sjaal"]), c([-7.4, 4.8, -2.8], [14.8, 0.9, 8.6], sw["sjaal_rand"]),
        c([-2.2, 2.2, -3.2], [4.4, 3.6, 0.8], sw["sjaal"])]})
    geo["bones"].append({"name": "oma_wol", "parent": "root", "pivot": [6, 1.5, -15], "cubes": [
        c([4.6, 0, -16.6], [3.2, 3.2, 3.2], sw["wol"]), c([8.0, 0, -15.2], [2.4, 2.4, 2.4], sw["wol2"])]})
    kn._save_geo(h, "guh_npc_mika_oma.geo.json", geo_file)
    # an old lady: soft grey-lilac fur, kind lilac eyes
    img = h.recolour(_src(h, "mika.png"), hue=0.76, sat=0.34, val=0.98, only=h.pinkish)
    a = np.asarray(img).copy()
    rng = np.random.default_rng(21300953)
    p = kn._paint
    p(a, sw["haar"], (226, 226, 234), rng, 8, _streep((204, 204, 216), 6, 1))
    p(a, sw["knot"], (232, 232, 240), rng, 8, _streep((206, 206, 218), 5, 1))
    p(a, sw["naald"], (190, 150, 90), rng, 4)
    p(a, sw["bril"], (206, 232, 244), rng, 3, _glas)
    p(a, sw["brilrand"], (60, 50, 60), rng, 3)
    p(a, sw["sjaal"], (186, 150, 226), rng, 8, _brei((222, 198, 246)))
    p(a, sw["sjaal_rand"], (250, 240, 252), rng, 5)
    p(a, sw["wol"], (240, 130, 180), rng, 10, _streep((250, 176, 210), 5, 2))
    p(a, sw["wol2"], (130, 200, 150), rng, 10, _streep((176, 230, 190), 5, 2))
    h.save(Image.fromarray(a), "entity", "npc_mika_oma.png")
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "guh_npc_mika_oma.animation.json"), _mika_anims(schommel=True))


def tolwachter(h):
    geo_file, geo = _mika(h, "guh_npc_tolwachter_mika")
    sw = kn._swatches(geo, ["pet", "klep", "badge", "snor", "riem", "buidel"])
    c = kn._cube
    H = [0, 12, -6]
    geo["bones"].append({"name": "tol_pet", "parent": "head", "pivot": H, "cubes": [
        c([-6.0, 12.3, -11.8], [12, 2.6, 10], sw["pet"]), c([-5.0, 14.9, -10.8], [10, 1.2, 8], sw["pet"]),
        c([-5.4, 12.3, -14.6], [10.8, 0.6, 3.0], sw["klep"]), c([-1.8, 12.6, -12.3], [3.6, 2.4, 0.6], sw["badge"])]})
    geo["bones"].append({"name": "tol_snor", "parent": "head", "pivot": [0, 4, -12.8], "cubes": [
        c([-4.6, 3.0, -13.4], [3.8, 1.6, 0.8], sw["snor"]), c([0.8, 3.0, -13.4], [3.8, 1.6, 0.8], sw["snor"]),
        c([-5.6, 2.2, -13.3], [1.4, 1.4, 0.7], sw["snor"]), c([4.2, 2.2, -13.3], [1.4, 1.4, 0.7], sw["snor"])]})
    geo["bones"].append({"name": "tol_buidel", "parent": "body", "pivot": [0, 6, 2], "cubes": [
        c([-7.0, 6.0, 1.0], [14, 1.0, 1.6], sw["riem"]), c([6.6, 0.6, 0.6], [1.0, 6.4, 2.4], sw["riem"]),
        c([6.2, 0.0, -0.6], [3.4, 3.8, 4.4], sw["buidel"])]})
    kn._save_geo(h, "guh_npc_tolwachter_mika.geo.json", geo_file)
    a = np.asarray(_src(h, "nether_mika.png")).copy()
    rng = np.random.default_rng(21300954)
    p = kn._paint
    p(a, sw["pet"], (60, 90, 170), rng, 8, _streep((44, 68, 136), 16, 3))
    p(a, sw["klep"], (26, 30, 46), rng, 4)
    p(a, sw["badge"], (250, 204, 70), rng, 4, _badge)
    p(a, sw["snor"], (50, 34, 30), rng, 8, _streep((84, 58, 48), 4, 1))
    p(a, sw["riem"], (120, 76, 44), rng, 6)
    p(a, sw["buidel"], (156, 110, 60), rng, 6, _munt)
    h.save(Image.fromarray(a), "entity", "npc_tolwachter_mika.png")
    h.w(os.path.join(h.A, "geckolib", "animations", "entity", "guh_npc_tolwachter_mika.animation.json"), _mika_anims(schommel=False))


def _mika_anims(schommel):
    """The animations of a Mika character (the names the NPC renderer plays): breathing, ears, tail; Mika-oma rocks gently."""
    idle = {"body": {"scale": {"0.0": [1, 1, 1], "2.0": [1.02, 1.03, 1.02], "4.0": [1, 1, 1]}},
            "tail": {"rotation": {"0.0": [0, -10, 0], "1.0": [0, 10, 0], "2.0": [0, -10, 0], "3.0": [0, 10, 0], "4.0": [0, -10, 0]}},
            "ear_left": {"rotation": {"0.0": [0, 0, 0], "2.6": [0, 0, 0], "2.75": [0, 0, -14], "2.9": [0, 0, 0], "4.0": [0, 0, 0]}},
            "ear_right": {"rotation": {"0.0": [0, 0, 0], "3.1": [0, 0, 0], "3.25": [0, 0, 14], "3.4": [0, 0, 0], "4.0": [0, 0, 0]}}}
    if schommel:
        idle["root"] = {"rotation": {"0.0": [-3, 0, 0], "2.0": [3, 0, 0], "4.0": [-3, 0, 0]}}
        idle["oma_naalden"] = {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 0, 5], "1.0": [0, 0, -5], "1.5": [0, 0, 5], "2.0": [0, 0, 0], "4.0": [0, 0, 0]}}
    else:
        idle["head"] = {"rotation": {"0.0": [-6, 0, 0], "1.0": [-8, 6, 0], "2.0": [-6, 0, 0], "3.0": [-8, -6, 0], "4.0": [-6, 0, 0]}}
    happy = {"body": {"position": {"0.0": [0, 0, 0], "0.15": [0, 2, 0], "0.3": [0, 0, 0], "0.45": [0, 1.5, 0], "0.6": [0, 0, 0]}},
             "head": {"rotation": {"0.0": [0, 0, 0], "0.3": [-16, 0, 0], "0.6": [0, 0, 0]}}}
    return {"format_version": "1.8.0", "animations": {
        "animation.guh_sitting.idle": {"loop": True, "animation_length": 4.0, "bones": idle},
        "animation.guh_sitting.happy": {"loop": False, "animation_length": 0.6, "bones": happy}}}


def stalknecht(h):
    H = [0, 13, 0]
    B = [0, 12, -4]
    beroepen_tex._npc(h, "stalknechtguh", 0.085, 0.55, 1.02, {
        # a tweed cap with a little peak, pulled over one ear
        "stal_pet": (("head", H), [([-6.6, 25.4, -6.8], [13.2, 2.0, 12.4], "pet", 0), ([-5.4, 27.4, -5.6], [10.8, 1.0, 10.0], "pet", 0),
                                   ([-5.6, 25.4, -9.6], [11.2, 0.7, 3.0], "klep", 0), ([-0.9, 28.2, -0.9], [1.8, 0.7, 1.8], "klep", 0)]),
        # a straw in his mouth
        "stal_strootje": (("head", H), [([-5.8, 16.2, -7.9], [4.8, 0.45, 0.45], "stro", 0)]),
        # a red neckerchief and dungarees with one strap undone
        "stal_doek": (("body", B), [([-4.6, 11.0, -4.9], [9.2, 1.6, 9.0], "doek", 0), ([-1.6, 9.0, -5.2], [3.2, 2.2, 0.6], "doek", 0)]),
        "stal_broek": (("body", B), [([-3.8, 2.2, -4.6], [7.6, 6.4, 0.8], "broek", 0), ([2.4, 8.4, -4.6], [1.2, 2.8, 0.7], "broek", 0),
                                     ([2.5, 8.0, -4.9], [1.0, 1.0, 0.4], "knoop", 0), ([-1.4, 4.2, -4.9], [2.8, 2.0, 0.4], "zak", 0)]),
        # the hay fork, standing beside him: a long handle, a crossbar, three prongs
        "stal_vork": (("body", B), [([7.4, 0.0, -3.0], [0.8, 22.0, 0.8], "steel", 0), ([5.6, 22.0, -3.1], [4.4, 0.7, 1.0], "ijzer", 0),
                                    ([5.6, 22.7, -3.0], [0.6, 4.2, 0.8], "ijzer", 0), ([7.5, 22.7, -3.0], [0.6, 4.6, 0.8], "ijzer", 0),
                                    ([9.4, 22.7, -3.0], [0.6, 4.2, 0.8], "ijzer", 0)]),
    }, {"pet": ((126, 134, 100), 10, _ruit((106, 114, 84), (142, 150, 114))), "klep": ((86, 92, 66), 6, None),
        "stro": ((232, 200, 100), 6, None), "doek": ((212, 50, 60), 6, _streep((255, 250, 250), 10, 1)),
        "broek": ((84, 118, 176), 10, _streep((104, 138, 196), 3, 1)), "knoop": ((246, 206, 60), 4, None),
        "zak": ((70, 100, 156), 6, None), "steel": ((160, 110, 66), 8, None), "ijzer": ((150, 156, 168), 6, None)}, 21300955)


# =====================================================================================================================
# blocks
# =====================================================================================================================
T = 16


def _planken(h):
    """Bridge planks: three broad crimson boards, a rope tied round them near both ends, nail heads."""
    src = h.vanilla("block/crimson_planks")
    img = h.recolour(src, hue=0.035, sat=0.72, val=0.92)
    d = ImageDraw.Draw(img)
    for y in (0, 5, 11):
        d.line((0, y, 15, y), fill=(70, 30, 26, 255))
    for x in (3, 12):
        for y in range(T):
            img.putpixel((x, y), TOUW + (255,) if y % 3 else (170, 140, 90, 255))
    for (x, y) in ((7, 2), (7, 8), (7, 13)):
        img.putpixel((x, y), (40, 36, 44, 255))
    return img


def _leuning():
    """The rope rail: twisted rope (the fence model takes it for posts and rails alike)."""
    img = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            t = (x + y * 2) % 4
            c = TOUW if t < 2 else (176, 146, 96)
            if (x * 3 + y) % 7 == 0:
                c = (236, 214, 164)
            img.putpixel((x, y), c + (255,))
    return img


def _mand():
    img = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            c = (176, 126, 70) if ((x // 2) + (y // 2)) % 2 else (146, 100, 54)
            img.putpixel((x, y), c + (255,))
    return img


def _wol(kleur, licht):
    img = Image.new("RGBA", (T, T))
    for y in range(T):
        for x in range(T):
            c = licht if (x + y) % 4 == 0 or (x - y) % 5 == 0 else kleur
            img.putpixel((x, y), c + (255,))
    return img


def blokken(h):
    A, w, el = h.A, h.w, h.el
    h.save(_planken(h), "block", "paleizen_brugplank.png")
    h.simple_block("paleizen_brugplank")
    h.self_drop("paleizen_brugplank")
    # the rope rail: vanilla's fence models with our rope
    h.save(_leuning(), "block", "paleizen_brugleuning.png")
    tex = {"texture": "guhs:block/paleizen_brugleuning"}
    w(f"{A}/models/block/paleizen_brugleuning_post.json", {"parent": "minecraft:block/fence_post", "textures": tex})
    w(f"{A}/models/block/paleizen_brugleuning_side.json", {"parent": "minecraft:block/fence_side", "textures": tex})
    w(f"{A}/models/block/paleizen_brugleuning_inventory.json", {"parent": "minecraft:block/fence_inventory", "textures": tex})
    w(f"{A}/blockstates/paleizen_brugleuning.json", {"multipart": [
        {"apply": {"model": "guhs:block/paleizen_brugleuning_post"}}] + [
        {"when": {d: "true"}, "apply": {"model": "guhs:block/paleizen_brugleuning_side", "uvlock": True, **({"y": y} if y else {})}}
        for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))]})
    w(f"{A}/models/item/paleizen_brugleuning.json", {"parent": "guhs:block/paleizen_brugleuning_inventory"})
    h.self_drop("paleizen_brugleuning")
    # Mika-oma's knitting: a wicker basket, a pink and a green ball of wool, two needles, a strip of knitting over the rim
    h.save(_mand(), "block", "paleizen_breiwerk_mand.png")
    h.save(_wol((240, 130, 180), (250, 186, 214)), "block", "paleizen_breiwerk_wol.png")
    h.save(_wol((130, 200, 150), (186, 232, 196)), "block", "paleizen_breiwerk_wol2.png")
    h.save(h.noise_tex((190, 150, 90), 8, 21300956), "block", "paleizen_breiwerk_naald.png")
    t = {"particle": "guhs:block/paleizen_breiwerk_mand", "mand": "guhs:block/paleizen_breiwerk_mand", "wol": "guhs:block/paleizen_breiwerk_wol",
         "wol2": "guhs:block/paleizen_breiwerk_wol2", "naald": "guhs:block/paleizen_breiwerk_naald"}
    els = [el([3, 0, 3], [13, 1, 13], "#mand"), el([3, 1, 3], [13, 5, 4], "#mand"), el([3, 1, 12], [13, 5, 13], "#mand"),
           el([3, 1, 4], [4, 5, 12], "#mand"), el([12, 1, 4], [13, 5, 12], "#mand"),
           el([4.5, 1, 4.5], [9, 5.5, 9], "#wol"), el([8, 1, 8], [11.5, 4.5, 11.5], "#wol2"),
           el([5, 5.5, 5], [8.5, 7, 8.5], "#wol"),
           el([6, 4, 7.6], [15, 4.6, 8.2], "#naald", rot={"origin": [8, 4, 8], "axis": "z", "angle": 22.5}),
           el([6, 4, 9.0], [15, 4.6, 9.6], "#naald", rot={"origin": [8, 4, 9], "axis": "z", "angle": 45}),
           el([10, 3, 12.4], [12.5, 6, 13.4], "#wol", faces=("north", "south", "up", "west", "east")),
           el([10, 0.5, 13.0], [12.5, 6, 13.6], "#wol", faces=("north", "south", "up", "down", "west", "east"))]
    w(f"{A}/models/block/paleizen_breiwerk.json", {"parent": "minecraft:block/block", "textures": t, "elements": els})
    w(f"{A}/blockstates/paleizen_breiwerk.json", {"variants": {"": {"model": "guhs:block/paleizen_breiwerk"}}})


# =====================================================================================================================
# item icons
# =====================================================================================================================
PAL = {"k": (60, 40, 46, 255), "w": (255, 250, 244, 255), "W": (226, 216, 206, 255), "r": (212, 60, 70, 255), "R": (160, 36, 48, 255),
       "o": (236, 150, 60, 255), "O": (196, 110, 40, 255), "y": (248, 208, 64, 255), "Y": (214, 168, 40, 255), "p": (240, 130, 180, 255),
       "P": (250, 186, 214, 255), "g": (130, 200, 150, 255), "G": (90, 160, 110, 255), "b": (176, 126, 70, 255), "B": (126, 86, 46, 255),
       "t": TOUW + (255,), "s": WORST + (255,), "S": WORST_STREEP + (255,), "n": SNUIT + (255,), "N": SNUIT_GAT + (255,),
       "c": (150, 82, 62, 255), "C": (112, 58, 44, 255), "l": (96, 140, 220, 255), "L": (60, 96, 170, 255), "z": (40, 36, 44, 255)}

ICONS = {
    # a bowl of oma's worstsoep, steaming, a slice of worst floating in it
    "paleizen_omasoep": [
        "................", "....W....W......", ".....W....W.....", "....W....W......", "................", "..kkkkkkkkkkkk..",
        ".kooooooosSoook.", ".kooyoooosSoook.", ".kwooooooooowwk.", "..kwwwwwwwwwwk..", "..kWwwwwwwwwWk..", "...kWWwwwwWWk...",
        "....kkWWWWkk....", "......kkkk......", "................", "................"],
    # the knitting: a half-knitted little hat on two needles, a ball of wool
    "paleizen_breiwerkje": [
        "................", "...b.......b....", "....b.....b.....", "....kpPpPpPk....", "....kPpPpPpk....", "....kpPpPpPk....",
        "....kPpPpPpk....", "....kpPpPpPk....", ".....kkkkkk.....", "......b..b......", ".....b....b.gg..", "....b......gGGg.",
        "...........gGgGg", "...........gGGgg", "............ggg.", "................"],
    # a sack of zwijnenvoer with a little snout stamped on it
    "paleizen_zwijnenvoer": [
        "................", "......tttt......", ".....tkkkkt.....", "......bbbb......", "....bbbbbbbb....", "...bbbbbbbbbb...",
        "...bbbnnnnbbb...", "..bbbnNnnNnbbb..", "..bbbnnnnnnbbb..", "..bbbbnnnnbbbb..", "..BbbbbbbbbbbB..", "..BBbbbbbbbbBB..",
        "...BBBbbbbBBB...", "....BBBBBBBB....", "................", "................"],
    # the runaway in your arms: a happy sausage face with a snout and two tusks
    "paleizen_gevangen_zwijntje": [
        "................", "..ss........ss..", ".sPPs......sPPs.", ".sPPssssssssPPs.", "..sssssssssssss.", "..ssszwssszwsss.",
        "..ssszzssszzsss.", "..sssssssssssss.", "..sswnnnnnnnwss.", "..sswnNnnnNnwss.", "..sssnnnnnnnsss.", "...sssssssssss..",
        "....SsssssssS...", ".....SSSSSSS....", "................", "................"],
    # a Worstzwijntje peeking out of a basket with a red bow
    "paleizen_worstzwijntje_mandje": [
        "................", "...ss......ss...", "..sPPs....sPPs..", "..sssssssssss...", "..sszwssszwss...", "..sszzssszzss...",
        "..swnnnnnnnws...", "..ssnNnnnNnss...", ".kkkkkkkkkkkkkk.", ".kbBbBbrrbBbBbk.", ".kBbBbrRRrbBbBk.", ".kbBbBbrrbBbBbk.",
        "..kBbBbBbBbBbk..", "..kkkkkkkkkkkk..", "................", "................"],
    # a long loose bridge plank with a rope round it
    "paleizen_losse_plank": [
        "................", "................", ".............cc.", "...........ccCc.", ".........cctCcc.", ".......ccCtccc..",
        ".....ccCcctcc...", "...ccCcccccc....", ".ccCtccccc......", ".cctcccc........", ".cctcc..........", ".ccc............",
        "................", "................", "................", "................"],
    # the building plan of the bridge: a blue sheet with a white bridge drawn on it, rolled at the ends
    "paleizen_recept_brug": [
        "................", ".WWWWWWWWWWWWWW.", ".WllllllllllllW.", ".lLLLLLLLLLLLLl.", ".lLwwwwwwwwwwLl.", ".lLwLLwLLwLLwLl.",
        ".lLwLLwLLwLLwLl.", ".lLLwLLLLLLwLLl.", ".lLLLwwwwwwLLLl.", ".lLLLLLLLLLLLLl.", ".lLwLLLLLLLLwLl.", ".lLLLLLLLLLLLLl.",
        ".WllllllllllllW.", ".WWWWWWWWWWWWWW.", "................", "................"],
}


def _egg(h, base, spots):
    def tint(img, rgb):
        a = np.asarray(img).astype(np.float32)
        a[..., :3] = a[..., :3] * np.array(rgb, np.float32) / 255.0
        return Image.fromarray(a.astype(np.uint8))
    return Image.alpha_composite(tint(h.vanilla("item/spawn_egg"), base), tint(h.vanilla("item/spawn_egg_overlay"), spots))


def items(h):
    for name, rows in ICONS.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
        h.save(h.grid(rows, PAL), "item", f"{name}.png")
        h.item_model(name)
    h.save(_egg(h, WORST, MOSTERD), "item", "worstzwijntje_spawn_egg.png")
    h.item_model("worstzwijntje_spawn_egg")
    h.w(f"{h.A}/models/item/paleizen_brugplank.json", {"parent": "guhs:block/paleizen_brugplank"})


def build(h):
    worstzwijntje(h)
    mopper_mika(h)
    mika_oma(h)
    tolwachter(h)
    stalknecht(h)
    blokken(h)
    items(h)
