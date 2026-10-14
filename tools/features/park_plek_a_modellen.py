"""
1.4.2 Het Knabbelpark, plekjes A - the looks:

  park_glijbaan        one slide piece: a pink shiny plank at 45 degrees with two white rails (rotated elements, rescaled to
                       run from the top back edge to the bottom front edge; FACING = downhill)
  park_schommel        a cherry seat on two ropes that go two blocks up; seven models, one per angle (STAND 0..6 = -45..45
                       degrees in steps of 15, the ropes turning round the top of the model = the bar above); positive angles
                       swing towards FACING (a rotation round +x moves the seat to model north, the blockstate turns north
                       to FACING)
  park_wip             a seesaw: a trestle and a three-block plank with handles and pink seat pads, level or tilted 22.5
                       degrees (KANT a = the end towards FACING up; the model's +x end is FACING)
  park_zandbak         sand with a little pink spade and a sand castle print on top
  park_picknickkleed   a red and white checked carpet with tiny pink guh hearts
  knabbelkoi           the koi of the Klaterdal (bio_dieren_modellen.koi: same cubes and atlas order = same UVs as
                       koi.geo.json) in kaasknabbel colours: cheese yellow, orange "holes", a toasted cap
"""
import math

from features import bio_dieren_modellen as bdm
from features.waterdiertjes_modellen import Atlas

FACING_Y = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
# the wip's model has its plank along +x: +x must point to FACING
WIP_Y = (("east", 0), ("south", 90), ("west", 180), ("north", 270))

DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
           "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
           "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.45, 0.45, 0.45]},
           "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
           "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.35, 0.35, 0.35]}}


def _el(frm, to, tex, rot=None):
    """An element with explicit uv on every face (the parts that reach outside the block would get uv outside 0..16)."""
    dx, dy, dz = (abs(to[i] - frm[i]) for i in range(3))
    maat = {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}
    e = {"from": frm, "to": to, "faces": {f: {"texture": tex, "uv": [0, 0, min(16, max(1, a)), min(16, max(1, b))]}
                                          for f, (a, b) in maat.items()}}
    if rot:
        e["rotation"] = rot
    return e


def _tex(h, naam, rijen, pal):
    h.save(h.grid(rijen, pal), "block", f"{naam}.png")


PAL = {".": (0, 0, 0, 0), "p": (246, 176, 204, 255), "P": (226, 132, 172, 255), "w": (255, 244, 248, 255), "W": (236, 222, 230, 255),
       "r": (214, 58, 64, 255), "R": (186, 40, 50, 255), "h": (255, 150, 186, 255), "y": (236, 210, 150, 255), "Y": (220, 190, 126, 255),
       "t": (196, 160, 104, 255), "T": (160, 124, 76, 255), "s": (232, 204, 140, 255), "k": (120, 80, 50, 255), "m": (194, 96, 136, 255)}


def textures(h):
    # the slide: shiny pink with a light stripe down the middle
    _tex(h, "park_glijbaan", [("pPppppphhppppppP" if i % 4 else "pPpppphwwhpppppP") for i in range(16)], PAL)
    _tex(h, "park_glijbaan_rand", ["wWwwwwwwwwwwwwWw"] * 16, PAL)
    # the rope of the swing
    _tex(h, "park_touw", [("yYty" * 4) if i % 2 else ("tyYy" * 4) for i in range(16)], PAL)
    # the picnic blanket: red and white checks (4 x 4), a tiny pink heart in some white squares
    rijen = []
    for y in range(16):
        r = ""
        for x in range(16):
            rood = ((x // 4) + (y // 4)) % 2 == 0
            c = "r" if rood else "w"
            if rood and (x % 4 == 0 or y % 4 == 0):
                c = "R"
            if not rood and (x // 4, y // 4) in ((1, 0), (3, 2), (0, 3), (2, 1)) and (x % 4, y % 4) in ((1, 1), (2, 1), (1, 2), (2, 2)):
                c = "h"
            r += c
        rijen.append(r)
    _tex(h, "park_picknickkleed", rijen, PAL)
    # the sandbox: vanilla sand with a pink spade and a little castle print
    zand = h.vanilla("block/sand").copy()
    schep = [(11, 3), (12, 3), (11, 4), (12, 4), (11, 5), (12, 5), (12, 6), (12, 7), (12, 8), (12, 9)]
    for (x, y) in schep:
        zand.putpixel((x, y), (236, 120, 168, 255) if y < 6 else (190, 120, 70, 255))
    for (x, y) in [(2, 10), (4, 10), (6, 10), (2, 11), (3, 11), (4, 11), (5, 11), (6, 11), (2, 12), (3, 12), (4, 12), (5, 12), (6, 12)]:
        zand.putpixel((x, y), (204, 180, 120, 255))
    h.save(zand, "block", "park_zandbak_top.png")


def models(h):
    A, w = h.A, h.w

    def model(naam, elementen, tex, parent="minecraft:block/block"):
        w(f"{A}/models/block/{naam}.json", {"parent": parent, "textures": {"particle": next(iter(tex.values())), **tex},
                                            "elements": elementen, "display": DISPLAY})

    # the slide piece (downhill = north): a plank across the block at 45 degrees, the rails beside and above it
    schuin = {"origin": [8, 8, 8], "axis": "x", "angle": -45, "rescale": True}
    model("park_glijbaan", [_el([1, 7, 0], [15, 8.5, 16], "#baan", schuin), _el([0, 7, 0], [1, 11, 16], "#rand", schuin),
                            _el([15, 7, 0], [16, 11, 16], "#rand", schuin)],
          {"baan": "guhs:block/park_glijbaan", "rand": "guhs:block/park_glijbaan_rand"})
    w(f"{A}/blockstates/park_glijbaan.json", {"variants": h.facing_states("park_glijbaan")})
    w(f"{A}/models/item/park_glijbaan.json", {"parent": "guhs:block/park_glijbaan"})
    # the swing: seven angles round the top of the model (the bar is the block two above)
    tex = {"zit": "minecraft:block/cherry_planks", "touw": "guhs:block/park_touw"}
    for stand in range(7):
        graden = (stand - 3) * 15
        rot = {"origin": [8, 32, 8], "axis": "x", "angle": graden} if graden else None
        model(f"park_schommel_{stand}", [_el([2, 4, 5], [14, 6, 11], "#zit", rot), _el([2.5, 6, 7.5], [3.5, 32, 8.5], "#touw", rot),
                                         _el([12.5, 6, 7.5], [13.5, 32, 8.5], "#touw", rot),
                                         _el([2, 5.5, 7], [4, 7.5, 9], "#touw", rot), _el([12, 5.5, 7], [14, 7.5, 9], "#touw", rot)], tex)
    w(f"{A}/blockstates/park_schommel.json", {"variants": {
        f"facing={f},stand={s}": {"model": f"guhs:block/park_schommel_{s}", **({"y": y} if y else {})} for f, y in FACING_Y for s in range(7)}})
    w(f"{A}/models/item/park_schommel.json", {"parent": "guhs:block/park_schommel_3"})
    # the seesaw: the trestle stays, the plank (with its handles and pads) tilts round the top of the trestle
    tex = {"plank": "minecraft:block/cherry_planks", "bok": "minecraft:block/stripped_cherry_log", "kussen": "minecraft:block/pink_wool",
           "greep": "minecraft:block/red_wool"}
    for kant, graden in (("vlak", 0), ("a", 22.5), ("b", -22.5)):
        rot = {"origin": [8, 9, 8], "axis": "z", "angle": graden} if graden else None
        model(f"park_wip_{kant}", [
            _el([6, 0, 6], [10, 8, 10], "#bok"), _el([3, 0, 7], [13, 1.5, 9], "#bok"),
            _el([-16, 8, 6.5], [32, 10, 9.5], "#plank", rot),
            _el([-15, 10, 6.5], [-9, 11, 9.5], "#kussen", rot), _el([25, 10, 6.5], [31, 11, 9.5], "#kussen", rot),
            _el([-8.5, 10, 7], [-7.5, 14, 9], "#greep", rot), _el([23.5, 10, 7], [24.5, 14, 9], "#greep", rot)], tex)
    w(f"{A}/blockstates/park_wip.json", {"variants": {
        f"facing={f},kant={k}": {"model": f"guhs:block/park_wip_{k}", **({"y": y} if y else {})} for f, y in WIP_Y for k in ("vlak", "a", "b")}})
    w(f"{A}/models/item/park_wip.json", {"parent": "guhs:block/park_wip_vlak"})
    # the sandbox sand
    w(f"{A}/models/block/park_zandbak.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/park_zandbak_top", "bottom": "minecraft:block/sand", "side": "minecraft:block/sand"}})
    w(f"{A}/blockstates/park_zandbak.json", {"variants": {"": {"model": "guhs:block/park_zandbak"}}})
    # the picnic blanket
    w(f"{A}/models/block/park_picknickkleed.json", {"parent": "minecraft:block/carpet", "textures": {"wool": "guhs:block/park_picknickkleed"}})
    w(f"{A}/blockstates/park_picknickkleed.json", {"variants": {"": {"model": "guhs:block/park_picknickkleed"}}})
    w(f"{A}/models/item/park_picknickkleed.json", {"parent": "guhs:block/park_picknickkleed"})


# --- the Knabbelkoi ---------------------------------------------------------------------------------------------------------
KNABBEL = ((246, 202, 92), (255, 236, 168), [((214, 142, 52), 4, 0.07, 0.14), ((255, 226, 140), 2, 0.12, 0.2)], (204, 122, 46),
           (255, 226, 140), (226, 140, 50), (240, 150, 80), None)


def knabbelkoi(h):
    """The texture on the koi's own geo: the same cubes in the same order as bdm.koi (and the same atlas seed)."""
    bdm.KOI["knabbel"] = KNABBEL
    try:
        atlas = Atlas(32, 16, 84010101)
        bdm.koi(atlas, "knabbel")
        h.save(atlas.image(), "entity", "knabbelkoi.png")
    finally:
        del bdm.KOI["knabbel"]


def build(h):
    textures(h)
    models(h)
    knabbelkoi(h)
