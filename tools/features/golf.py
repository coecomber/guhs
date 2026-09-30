"""
Guhgolf (2.4): a 9-hole guh minigolf course in the Guhmension with the Golfguh in a guh-face clubhouse.

  build(h)    the blocks (felt, tees (2.10: three per hole, one per level), cups with numbered flags, the windmill hub and
              sails), the golf ball entity
              texture, the club and golfballetje icons, the Golfguh's texture, all texts, advancements and the
              guh_golfbaan structure (with a geometry self-check)
  ftb(fq)     the golf quests (row y=34)
  BONES / clothes / CLOTHES / icons: the golf outfit (flat cap, sun visor, argyle sweater)

Java side: nl.juiced.guhs.feature.golf (GolfGame, GolfBallEntity, ...).
"""
import json
import math
import os
import random
import re
from collections import deque

import numpy as np
from PIL import Image

# ---------------------------------------------------------------------------------------------------------------------
# the golf outfit
# ---------------------------------------------------------------------------------------------------------------------
_H = [0, 6, -2]  # the guh's head pivot (see make_guh_variants.HEAD_PIVOT)
BONES = {
    # a flat cap: a flat crown that sticks out a little at the front, with a short brim and a button on top
    "outfit_flat_cap": ("head", _H, "flat_cap", [([-5, 15, -10.8], [10, 1.4, 9], 0), ([-5.3, 16.4, -12], [10.6, 0.7, 9.6], 0),
                                                ([-4.5, 15, -13.2], [9, 0.45, 2.4], 0), ([-0.5, 17.1, -7.2], [1, 0.4, 1], 0)]),
    # a sun visor: a band round the forehead (just above the eyes) and a big brim over them
    "outfit_visor": ("head", _H, "visor", [([-7.3, 12.7, -12.6], [14.6, 1.4, 0.5], 0), ([-7.7, 12.7, -12.6], [0.4, 1.4, 7], 0),
                                          ([7.3, 12.7, -12.6], [0.4, 1.4, 7], 0), ([-6.5, 13.5, -15.8], [13, 0.4, 3.3], 0)]),
}
CLOTHES = ["golf_pet", "golf_zonneklep", "golf_trui"]


def _argyle(rng, v):
    px = v.SWATCH * 4
    a = v.fabric((248, 182, 210), rng, 6)
    for y in range(px):
        for x in range(px):
            if ((x + y) // 8 + (x - y + 64) // 8) % 2 == 0:
                a[y, x] = (214, 84, 148)                   # the diamonds
            if (x + y) % 16 == 8 or (x - y + 64) % 16 == 8:
                a[y, x] = (250, 208, 76)                   # thin golden lines over them
    return a


def _tweed(rng, v):
    a = v.fabric((238, 230, 234), rng, 8)
    px = v.SWATCH * 4
    for y in range(px):
        for x in range(px):
            if x % 8 < 2 or y % 8 < 2:
                a[y, x] = (205, 120, 165) if (x % 8 < 2) != (y % 8 < 2) else (150, 70, 115)
    return a


def clothes(rng, v):
    return {
        "golf_pet": {"flat_cap": lambda: _tweed(rng, v)},
        "golf_zonneklep": {"visor": lambda: v.band((246, 110, 172), (255, 255, 255), rng, (13,))},
        "golf_trui": {"suit": lambda: _argyle(rng, v)},
    }


VISOR_ICON = ["................", "................", "................", "................", "................",
              "..aaaaaaaaaaaa..", ".abwwwwwwwwwwba.", ".abbbbbbbbbbbba.", "..aabbbbbbbbaa..", "....aabbbbaa....",
              "......aaaa......", "................", "................", "................", "................",
              "................"]
ARGYLE_ICON = ["...aaa....aaa...", "..abbbaaaabbba..", ".abbbcbbbbcbbba.", ".abbcgcbbcgcbba.", ".aabbcbbbbcbbaa.",
               "..aabbbbbbbbaa..", "...abbbcbbbba...", "...abbcgcbbba...", "...abbbcbbbba...", "...abbbbbbbba...",
               "...abbbbbbbba...", "...aaaaaaaaaa..."]


def icons(ic):
    return {
        "golf_pet": ic.shaped("cap", (140, 60, 105), (238, 228, 232), (205, 120, 165)),
        "golf_zonneklep": ic.icon(VISOR_ICON, {"a": (170, 50, 110), "b": (246, 110, 172), "w": (255, 255, 255)}),
        "golf_trui": ic.icon(ic.pad(ARGYLE_ICON), {"a": (150, 50, 105), "b": (248, 182, 210), "c": (214, 84, 148), "g": (250, 208, 76)}),
    }


# ---------------------------------------------------------------------------------------------------------------------
# the holes (also in GolfGame.PAR / HOLE_NAMES)
# ---------------------------------------------------------------------------------------------------------------------
HOLE_NAMES = [("guhpaadje", "The Guh Path", "Het Guhpaadje"), ("vadsbocht", "The Vads Bend", "De Vadsbocht"),
              ("guhmolen", "The Guh Windmill", "De Guhmolen"), ("kaassausmoeras", "Cheese Sauce Swamp", "Kaassausmoeras"),
              ("vahoegschans", "The Vahoeg Jump", "De Vahoegschans"), ("guhmond", "The Guh Mouth", "De Guhmond"),
              ("bumperbal", "Bumper Ball", "Bumperbal"), ("kaasknabbelheuvel", "Kaasknabbel Hill", "De Kaasknabbelheuvel"),
              ("grote_vads", "The Big Vads", "De Grote Vads")]
PAR = [2, 3, 3, 3, 3, 3, 3, 3, 4]
# 2.10: the par per level (makkelijk from the tee close to the cup, medium as always, lastig from the far tee);
# the same numbers as GolfBanen.PARS (the self-check compares them)
PARS = {"makkelijk": [2, 2, 2, 2, 2, 2, 2, 2, 2], "medium": PAR, "lastig": [3, 4, 4, 4, 4, 4, 4, 4, 5]}

W, HT, D = 96, 26, 96        # the template
G = 1                         # floor level: y=0 is the foundation, the course is on y=1, the ball rolls at y=2
STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
LEFT = {"north": "west", "west": "south", "south": "east", "east": "north"}
SIGN_ROT = {"north": 0, "east": 4, "south": 8, "west": 12}   # a standing sign facing back at the player on the tee

# n: rects (x0, z0, x1, z1) of felt, tee (x, z, facing) = the medium tee, cup (x, z), wall height;
# 2.10: tee_m / tee_l = the makkelijk tee (close to the cup) and the lastig tee (further away; some lanes got longer for it),
# bumpers_l = the extra slime bumpers of a lastig round (placed by the Golfguh; the same as GolfBanen.LASTIG_BUMPERS)
LANES = [
    {"rects": [(10, 83, 41, 85)], "tee": (34, 84, "west"), "cup": (12, 84), "wall": 1,
     "tee_m": (21, 84, "west"), "tee_l": (39, 84, "west"), "bumpers_l": [(27, 83), (27, 85), (16, 83), (16, 85)]},
    {"rects": [(6, 56, 8, 82), (6, 56, 24, 58)], "tee": (7, 76, "north"), "cup": (22, 57), "wall": 1,
     "tee_m": (10, 57, "east"), "tee_l": (7, 80, "north"), "bumpers_l": [(7, 70), (16, 57)]},
    {"rects": [(26, 37, 28, 57), (27, 32, 27, 36), (26, 18, 28, 31)], "tee": (27, 52, "north"), "cup": (27, 21), "wall": 1,
     "tee_m": (27, 29, "north"), "tee_l": (27, 55, "north"), "bumpers_l": [(27, 46), (26, 40), (28, 40)]},
    {"rects": [(31, 9, 60, 15)], "tee": (36, 12, "east"), "cup": (58, 12), "wall": 1,
     "tee_m": (53, 12, "east"), "tee_l": (33, 12, "east"), "bumpers_l": [(40, 10), (40, 11), (40, 13), (40, 14), (55, 12)]},
    {"rects": [(62, 6, 90, 8)], "tee": (66, 7, "east"), "cup": (87, 7), "wall": 2,
     "tee_m": (82, 7, "east"), "tee_l": (64, 7, "east"), "bumpers_l": [(69, 6), (69, 8)]},
    {"rects": [(84, 11, 86, 38)], "tee": (85, 16, "south"), "cup": (85, 37), "wall": 1,
     "tee_m": (85, 31, "south"), "tee_l": (85, 13, "south"), "bumpers_l": [(84, 21), (86, 21), (85, 26)]},
    {"rects": [(80, 46, 90, 68)], "tee": (85, 50, "south"), "cup": (85, 65), "wall": 1,
     "tee_m": (85, 60, "south"), "tee_l": (82, 48, "south"), "bumpers_l": [(85, 53), (83, 54), (87, 54)]},
    {"rects": [(62, 76, 92, 80)], "tee": (88, 78, "west"), "cup": (72, 78), "wall": 2,
     "tee_m": (76, 78, "west"), "tee_m_y": 1, "tee_l": (90, 78, "west"), "bumpers_l": [(84, 77), (84, 79), (81, 78)]},
    {"rects": [(54, 86, 92, 90), (54, 72, 58, 85)], "tee": (89, 88, "west"), "cup": (56, 74), "wall": 2,
     "tee_m": (56, 83, "north"), "tee_l": (90, 86, "west"), "bumpers_l": [(82, 87), (82, 89), (66, 88)]},
]
TEE_KINDS = (("tee_m", "makkelijk"), ("tee", "medium"), ("tee_l", "lastig"))


def tee_y(lane, kind):
    """The height of a tee (template): the floor, or one up for the makkelijk tee on the Kaasknabbelheuvel."""
    return G + (lane.get("tee_m_y", 0) if kind == "tee_m" else 0)
NPC = (47.5, G + 1.0, 46.5)
ENTRANCE = (47, 95)


def _floor_cells(lane):
    cells = set()
    for x0, z0, x1, z1 in lane["rects"]:
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                cells.add((x, z))
    return cells


def _wall_cells(cells):
    walls = set()
    for (x, z) in cells:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                n = (x + dx, z + dz)
                if n not in cells:
                    walls.add(n)
    return walls


def _sign_nbt(lines, colour="black", glow=False):
    msgs = [json.dumps(l) if not isinstance(l, str) else json.dumps({"text": l}) for l in lines] + [json.dumps("")] * (4 - len(lines))
    empty = [json.dumps("")] * 4
    from make_structures import NbtList, Byte
    return {"id": "minecraft:sign",
            "front_text": {"messages": NbtList(8, msgs), "color": colour, "has_glowing_text": Byte(1 if glow else 0)},
            "back_text": {"messages": NbtList(8, empty), "color": "black", "has_glowing_text": Byte(0)},
            "is_waxed": Byte(1)}


def golf_structure(h):
    """The guh golf course: 96x96, 9 lanes round a clubhouse shaped like a guh face, a plaza with a guh-face mosaic and
    the scoreboard, a guh-face gate in the south. Returns the structure and the metadata for the self-check."""
    S = h.Structure((W, HT, D))
    mc = h.mc
    rng = random.Random(2404)
    LEAVES = {"persistent": "true", "distance": "7", "waterlogged": "false"}
    FENCE0 = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
    FLOWERS = ["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]

    def lamp(hanging="false", colour=None):
        return ("guhs:lampion_" + (colour or rng.choice(["roze", "geel", "mint"])), {"hanging": hanging, "waterlogged": "false"})

    def stairs(facing, block="cherry_stairs"):
        return mc(block), {"facing": facing, "half": "bottom", "shape": "straight", "waterlogged": "false"}

    SLAB = (mc("cherry_slab"), {"type": "bottom", "waterlogged": "false"})

    # --- the ground: foundation, grass, the hedge round it with a path inside --------------------------------------
    fp = [(x, z) for x in range(W) for z in range(D)]
    for x, z in fp:
        S.set(x, 0, z, mc("dirt"))
        ring = min(x, z, W - 1 - x, D - 1 - z)
        if ring == 0:
            S.set(x, G, z, mc("grass_block"), {"snowy": "false"})
            if not (44 <= x <= 51 and z == D - 1):
                S.set(x, G + 1, z, mc("flowering_azalea_leaves"), LEAVES)
                if (x + z) % 2:
                    S.set(x, G + 2, z, mc("flowering_azalea_leaves"), LEAVES)
        elif ring == 1:
            S.set(x, G, z, mc("smooth_quartz"))
        else:
            S.set(x, G, z, mc("grass_block"), {"snowy": "false"})

    # --- the lanes: felt, walls with crystal lamps and golden corners, tees, cups, signs ------------------------------
    all_floor = {}
    for n, lane in enumerate(LANES):
        cells = _floor_cells(lane)
        lane["cells"] = cells
        for c in cells:
            all_floor[c] = n
    for n, lane in enumerate(LANES):
        cells = lane["cells"]
        lane["walls"] = walls = _wall_cells(cells)
        for (x, z) in walls:
            assert (x, z) not in all_floor, f"hole {n + 1}: its wall at {(x, z)} is on the floor of hole {all_floor[(x, z)] + 1}"
            S.set(x, G, z, mc("white_concrete"))
            top = G + lane["wall"]
            corner = sum((x + dx, z + dz) in cells for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))) == 0
            for y in range(G + 1, top + 1):
                if corner:
                    S.set(x, y, z, mc("gold_block"))
                elif y == top and (x * 7 + z * 13) % 9 == 0:
                    S.set(x, y, z, "guhs:guh_kristal_lamp")
                else:
                    S.set(x, y, z, mc("pink_concrete") if ((x + z) // 2) % 2 else mc("white_concrete"))
        for (x, z) in cells:
            S.set(x, G, z, "guhs:golfbaan_vilt")
        tx, tz, facing = lane["tee"]
        S.set(tx, G, tz, "guhs:golfbaan_afslag", {"facing": facing, "hole": str(n + 1), "niveau": "medium"})
        cx, cz = lane["cup"]
        S.set(cx, G, cz, "guhs:golfbaan_hole", {"hole": str(n + 1)})
        # the sign next to the tee: on top of the nearest wall to the left
        lx, lz = STEP[LEFT[facing]]
        k = 1
        while (tx + lx * k, tz + lz * k) in cells:
            k += 1
        name = HOLE_NAMES[n][0]
        S.set(tx + lx * k, G + lane["wall"] + 1, tz + lz * k, mc("cherry_sign"), {"rotation": str(SIGN_ROT[facing]), "waterlogged": "false"},
              _sign_nbt([{"text": f"Hole {n + 1}", "color": "dark_purple", "bold": True}, {"translate": f"gui.guhs.golf.hole.{name}"},
                         {"translate": "gui.guhs.golf.sign_par", "with": [str(PAR[n])]}]))
        lane["sign"] = (tx + lx * k, G + lane["wall"] + 1, tz + lz * k)
        if lane["wall"] > 1:                       # a low bit of wall behind the (furthest) tee, so you can climb in
            fx_, fz_, far_facing = lane["tee_l"]
            sx, sz = STEP[far_facing]
            lx, lz = STEP[LEFT[far_facing]]
            bx, bz = fx_ - 3 * sx, fz_ - 3 * sz
            for d in (-1, 0, 1):
                wx, wz = bx + d * lx, bz + d * lz
                if (wx, wz) in walls:
                    for y in range(G + 2, G + lane["wall"] + 1):
                        S.set(wx, y, wz, mc("air"))

    # --- hole 2, De Vadsbocht: pink slime bumpers in the outer corner of the bend ---------------------------------------
    for z in range(55, 60):
        S.set(5, G + 1, z, "guhs:roze_slijmblok")
    for x in range(5, 10):
        S.set(x, G + 1, 55, "guhs:roze_slijmblok")

    # --- hole 3, De Guhmolen: the guh windmill over the lane; its lower sail closes the tunnel every other second -------
    for x in range(23, 32):
        for z in range(32, 37):
            S.set(x, G, z, mc("white_concrete"))
            for y in range(G + 1, 11):
                corner = x in (23, 31) and z in (32, 36)
                S.set(x, y, z, mc("white_concrete") if corner or y == 10 else mc("pink_terracotta") if y % 3 == 0 else mc("pink_concrete"))
    for x in range(24, 31):                       # the cap on top, and guh ears
        for z in range(33, 36):
            S.set(x, 11, z, mc("pink_terracotta"))
            S.set(x, 12, z, mc("pink_terracotta") if 25 <= x <= 29 else mc("air"))
    for ex in (24, 29):
        for dx in range(2):
            for y in range(13, 15):
                S.set(ex + dx, y, 34, mc("pink_concrete") if (y == 14 or dx == (0 if ex == 24 else 1)) else mc("magenta_concrete"))
    S.set(27, 13, 34, *lamp())
    for z in range(32, 37):                       # the tunnel through it
        S.set(27, G, z, "guhs:golfbaan_vilt")
        S.set(27, G + 1, z, mc("air"))
        S.set(27, G + 2, z, mc("air"))
    S.set(27, G + 3, 34, "guhs:guh_kristal_lamp")
    for (x, y) in ((25, 7), (29, 7)):             # little round windows
        S.set(x, y, 36, mc("pink_stained_glass"))
        S.set(x, y, 32, mc("pink_stained_glass"))
    hub = (27, 5, 37)
    S.set(*hub, "guhs:golfbaan_molenas", {"facing": "south"})
    for k in range(1, 4):                         # the sails, as a "+" to start with
        for (x, y) in ((27, 5 + k), (27, 5 - k), (27 - k, 5), (27 + k, 5)):
            S.set(x, y, 37, "guhs:golfbaan_wiek")

    # --- hole 4, Kaassausmoeras: a pond of kaassaus in the middle of the wide lane --------------------------------------
    for x in range(43, 51):
        for z in range(11, 14):
            S.set(x, G, z, "guhs:kaas_saus", {"level": "0"})

    # --- hole 5, De Vahoegschans: a ramp up to a slime launch pad, over a ditch of kaassaus -----------------------------
    for z in range(6, 9):
        S.set(72, G + 1, z, *SLAB)
        S.set(73, G + 1, z, *stairs("east"))
        S.set(74, G + 1, z, "guhs:golfbaan_vilt")
        S.set(75, G + 1, z, "guhs:roze_slijmblok")
        for x in range(76, 80):
            S.set(x, G, z, "guhs:kaas_saus", {"level": "0"})

    # --- hole 6, De Guhmond: a giant guh head; the ball rolls into its mouth, onto its tongue, into the cup ----------------
    hc = (85, 5, 37)
    for x in range(77, 94):
        for y in range(0, 14):
            for z in range(29, 46):
                d = math.dist((x, y * 1.08, z), (hc[0], hc[1] * 1.08, hc[2]))
                if d <= 7.4:
                    S.set(x, y, z, mc("pink_wool") if y > 0 else mc("dirt"))
    for x in range(84, 87):                        # the mouth and the cave inside
        for z in range(29, 39):
            for y in range(G + 1, G + 4):
                S.set(x, y, z, mc("air"))
            S.set(x, G, z, "guhs:tong")
    S.set(85, G, 37, "guhs:golfbaan_hole", {"hole": "6"})
    S.set(85, G + 4, 35, "guhs:guh_kristal_lamp")
    S.set(85, G + 4, 32, "guhs:guh_kristal_lamp")
    front = {}                                     # the frontmost block of the head per (x, y): the face goes there
    for (x, y, z), (name, _p, _n) in list(S.blocks.items()):
        if name == "minecraft:pink_wool" and 29 <= z <= 37 and 77 <= x <= 93:
            if (x, y) not in front or z < front[(x, y)]:
                front[(x, y)] = z
    for x in (84, 86):                             # two front teeth hanging over the mouth
        S.set(x, G + 3, 30, "guhs:tand")
    for (x, y) in ((81, 8), (82, 8), (81, 9), (82, 9), (88, 8), (89, 8), (88, 9), (89, 9)):   # eyes, with a twinkle
        if (x, y) in front:
            twinkle = (x, y) in ((81, 9), (88, 9))
            S.set(x, y, front[(x, y)], mc("white_concrete") if twinkle else mc("black_concrete"))
    for (x, y) in ((80, 5), (90, 5), (79, 5), (91, 5)):                                     # blushing cheeks
        if (x, y) in front:
            S.set(x, y, front[(x, y)], mc("magenta_concrete"))
    for (x, y) in ((85, 6), (84, 6), (86, 6)):                                                # the nose
        if (x, y) in front:
            S.set(x, y, front[(x, y)], mc("pink_terracotta"))
    for ex in (80, 90):                                                                      # ears
        for dx in (-1, 0, 1):
            for y in range(11, 15):
                if abs(dx) + (y - 11) // 2 <= 2:
                    S.set(ex + dx, y, 37, mc("magenta_concrete") if dx == 0 and y < 14 else mc("pink_concrete"))

    # --- hole 7, Bumperbal: slime bumpers in the shape of a guh face; the cup is its mouth -----------------------------
    for (x, z) in ((82, 56), (83, 56), (87, 56), (88, 56), (85, 59), (82, 62), (83, 63), (87, 63), (88, 62)):
        S.set(x, G + 1, z, "guhs:roze_slijmblok")

    # --- hole 8, De Kaasknabbelheuvel: up onto a kaasknabbel hill with the cup on top, and down the other side -------
    for z in range(76, 81):
        S.set(80, G + 1, z, *SLAB)
        S.set(79, G + 1, z, *stairs("west"))
        for x in range(70, 79):
            S.set(x, G, z, "guhs:block_of_kaasknabbels")
            S.set(x, G + 1, z, "guhs:golfbaan_vilt")
        S.set(69, G + 1, z, *stairs("east"))
    S.set(72, G, 78, "guhs:block_of_kaasknabbels")
    S.set(72, G + 1, 78, "guhs:golfbaan_hole", {"hole": "8"})
    LANES[7]["cup_y"] = G + 1

    # --- hole 9, De Grote Vads: a hump, slime in the bend and a kaassaus moat along the last bit -----------------------
    for z in range(86, 91):
        S.set(78, G + 1, z, *SLAB)
        S.set(77, G + 1, z, *stairs("west"))
        S.set(76, G + 1, z, "guhs:golfbaan_vilt")
        S.set(75, G + 1, z, *stairs("east"))
        S.set(74, G + 1, z, *SLAB)
    for z in range(86, 92):
        S.set(53, G + 1, z, "guhs:roze_slijmblok")
        S.set(53, G + 2, z, "guhs:roze_slijmblok")
    for x in range(53, 59):
        S.set(x, G + 1, 91, "guhs:roze_slijmblok")
        S.set(x, G + 2, 91, "guhs:roze_slijmblok")
    for z in range(78, 82):
        for x in (54, 58):
            S.set(x, G, z, "guhs:kaas_saus", {"level": "0"})

    # --- 2.10: the makkelijk and lastig tees (after the holes' own things: the tongue and the hill are under two of them) --
    for n, lane in enumerate(LANES):
        for kind, level in (("tee_m", "makkelijk"), ("tee_l", "lastig")):
            tx, tz, facing = lane[kind]
            S.set(tx, tee_y(lane, kind), tz, "guhs:golfbaan_afslag", {"facing": facing, "hole": str(n + 1), "niveau": level})

    # --- the clubhouse: its front is a big guh face, the mouth is the door ------------------------------------------------
    X0, X1, Z0, Z1, TOP = 36, 59, 38, 57, 11
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            wall = x in (X0, X1) or z in (Z0, Z1)
            S.set(x, G, z, (mc("cherry_planks") if (x + z) % 2 else mc("stripped_cherry_wood")) if not wall else mc("white_concrete"),
                  None if wall or (x + z) % 2 else {"axis": "y"})
            for y in range(G + 1, TOP):
                if wall:
                    S.set(x, y, z, mc("pink_concrete") if z == Z1 else mc("white_concrete") if (y + x + z) % 5 else mc("pink_terracotta"))
                else:
                    S.set(x, y, z, mc("air"))
            S.set(x, TOP, z, mc("gold_block") if wall else mc("white_concrete"))
    for x in range(X0, X1 + 1):                   # the face on the front (z = Z1)
        for y in range(G + 1, TOP + 1):
            S.set(x, y, Z1, mc("pink_concrete"))
    for (x, y) in [(x, y) for x in range(40, 43) for y in range(7, 10)] + [(x, y) for x in range(53, 56) for y in range(7, 10)]:
        S.set(x, y, Z1, mc("white_concrete") if (x, y) in ((40, 9), (53, 9)) else mc("black_concrete"))
    for x in (47, 48):
        S.set(x, 6, Z1, mc("pink_terracotta"))       # nose
    for x in (38, 39, 56, 57):
        S.set(x, 5, Z1, mc("magenta_concrete"))      # cheeks
    for x in list(range(X0, X0 + 3)) + list(range(X1 - 2, X1 + 1)):
        S.set(x, 6, Z1, mc("white_concrete"))        # whiskers
        S.set(x, 4, Z1, mc("white_concrete"))
    for x in range(45, 51):
        S.set(x, 5, Z1, mc("magenta_terracotta"))    # the upper lip
    for x in range(46, 50):                          # the mouth: the door
        for y in (G + 1, G + 2, G + 3):
            S.set(x, y, Z1, mc("air"))
    S.set(47, G + 3, Z1, "guhs:tand")               # and two front teeth
    S.set(48, G + 3, Z1, "guhs:tand")
    for ex in (37, 53):                              # ears on the roof
        rows = [(0, 6), (0, 6), (0, 6), (1, 5), (1, 5), (2, 4)]
        for i, (a, b) in enumerate(rows):
            for dx in range(a, b):
                inner = 1 <= i <= 4 and a + 1 <= dx < b - 1
                S.set(ex + dx, TOP + 1 + i, Z1 - 1, mc("magenta_concrete") if inner else mc("pink_concrete"))
    for z in range(41, 55, 4):                       # windows on the sides
        for y in (5, 6):
            S.set(X0, y, z, mc("pink_stained_glass"))
            S.set(X0, y, z + 1, mc("pink_stained_glass"))
            S.set(X1, y, z, mc("pink_stained_glass"))
            S.set(X1, y, z + 1, mc("pink_stained_glass"))
    # inside: a pink rug with a guh face, the Golfguh (the world's top 3 floats above her), a sign pointing at it, lamps, benches, trophies and clubs
    for x in range(41, 55):
        for z in range(41, 53):
            if math.dist((x, z * 1.2), (47.5, 47 * 1.2)) < 6.3:
                S.set(x, G + 1, z, mc("pink_carpet"))
    for (x, z) in ((45, 45), (50, 45)):
        S.set(x, G + 1, z, mc("black_carpet"))
    for (x, z) in ((44, 47), (51, 47)):
        S.set(x, G + 1, z, mc("magenta_carpet"))
    S.entity(NPC[0], NPC[1], NPC[2], {"id": "guhs:guh_npc", "Kind": "golfguh", "PersistenceRequired": h.Byte(1),
                                      "Rotation": h.floats(0.0, 0.0)})
    S.set(47, 6, Z0 + 1, mc("cherry_wall_sign"), {"facing": "south", "waterlogged": "false"},
          _sign_nbt([{"text": "Top 3", "bold": True}, {"translate": "gui.guhs.golf.sign_top3_1"}, {"translate": "gui.guhs.golf.sign_top3_2"}, ""],
                   "magenta", True))
    S.set(46, 6, Z0 + 1, mc("gold_block"))
    S.set(48, 6, Z0 + 1, mc("gold_block"))
    for x in range(44, 52):                          # the counter behind her, with trophies
        S.set(x, G + 1, Z0 + 2, mc("cherry_planks"))
        S.set(x, G + 2, Z0 + 2, mc("gold_block") if x in (45, 50) else mc("cherry_slab"), None if x in (45, 50) else {"type": "bottom", "waterlogged": "false"})
    for x in (45, 50):
        S.set(x, G + 3, Z0 + 2, *lamp("false", "geel"))
    for z in range(Z0 + 2, Z1 - 1, 3):               # a rack of guh golf clubs along the west wall
        S.set(X0 + 1, G + 1, z, mc("cherry_fence"), FENCE0)
        S.set(X0 + 1, G + 2, z, mc("lightning_rod"), {"facing": "up", "powered": "false", "waterlogged": "false"})
    for z in range(Z0 + 3, Z1 - 2, 4):               # benches along the east wall
        S.set(X1 - 1, G + 1, z, "guhs:guh_bank", {"facing": "west"})
    for (x, z) in ((40, 42), (55, 42), (40, 53), (55, 53), (47, 50), (48, 43)):
        S.set(x, TOP - 1, z, *lamp("true"))
    for x in range(X0 + 1, X1):
        if x % 3 == 0:
            S.set(x, TOP - 1, Z0 + 1, "guhs:vlaggetjes", {"axis": "x"})
            S.set(x, TOP - 1, Z1 - 1, "guhs:vlaggetjes", {"axis": "x"})
    for (x, z) in ((38, 40), (57, 40), (38, 55), (57, 55)):
        S.set(x, G + 1, z, "guhs:guh_kristal_lamp")

    # --- the plaza in front: a guh-face mosaic and the scoreboard with all 9 holes -------------------------------------
    for x in range(36, 60):
        for z in range(58, 70):
            S.set(x, G, z, mc("smooth_quartz") if (x + z) % 2 else mc("white_concrete"))
    cx, cz = 47.5, 63.5
    for x in range(36, 60):
        for z in range(58, 70):
            d = math.dist((x, z), (cx, cz))
            if d < 5.6:
                S.set(x, G, z, mc("pink_concrete"))
            elif d < 6.3:
                S.set(x, G, z, mc("gold_block"))
    for (x, z) in ((45, 61), (50, 61)):
        S.set(x, G, z, mc("black_concrete"))
    for (x, z) in ((44, 64), (51, 64)):
        S.set(x, G, z, mc("magenta_concrete"))
    for (x, z) in ((47, 64), (48, 64)):
        S.set(x, G, z, mc("pink_terracotta"))
    for (x, z) in ((46, 66), (47, 67), (48, 67), (49, 66)):
        S.set(x, G, z, mc("white_concrete"))
    for (ex, ez) in ((42, 58), (53, 58)):            # its ears, poking out towards the clubhouse
        for dx in range(3):
            S.set(ex + dx, G, ez, mc("pink_concrete"))
    for z in range(59, 69):                          # the scoreboard on the west side of the plaza
        for y in range(G, G + 6):
            S.set(36, y, z, mc("white_concrete") if y < G + 5 else mc("gold_block"))
    for i in range(9):
        y = G + 3 if i < 5 else G + 2
        z = [60, 62, 64, 66, 68, 61, 63, 65, 67][i]
        name = HOLE_NAMES[i][0]
        S.set(37, y, z, mc("cherry_wall_sign"), {"facing": "east", "waterlogged": "false"},
              _sign_nbt([{"text": f"Hole {i + 1}", "color": "dark_purple", "bold": True}, {"translate": f"gui.guhs.golf.hole.{name}"},
                         {"translate": "gui.guhs.golf.sign_par", "with": [str(PAR[i])]}]))
    S.set(37, G + 4, 64, mc("cherry_wall_sign"), {"facing": "east", "waterlogged": "false"},
          _sign_nbt([{"text": "Guhgolf", "color": "light_purple", "bold": True}, {"translate": "gui.guhs.golf.sign_total"},
                     {"translate": "gui.guhs.golf.sign_par", "with": [str(sum(PAR))]}], "black", True))
    for (x, z) in ((38, 58), (58, 58), (38, 69), (58, 69)):   # lamp posts
        S.set(x, G + 1, z, mc("cherry_fence"), FENCE0)
        S.set(x, G + 2, z, mc("cherry_fence"), FENCE0)
        S.set(x, G + 3, z, mc("gold_block"))
        S.set(x, G + 4, z, *lamp())
    for x in (40, 55):
        S.set(x, G + 1, 68, "guhs:guh_bank", {"facing": "north"})

    # --- the way in: a path from the gate (a guh face with ears) to the plaza -------------------------------------------
    for z in range(70, D):
        for x in range(45, 51):
            S.set(x, G, z, mc("pink_concrete") if x in (45, 50) else mc("smooth_quartz"))
    for z in range(72, 94, 5):
        for x in (44, 51):
            S.set(x, G + 1, z, mc("cherry_fence"), FENCE0)
            S.set(x, G + 2, z, *lamp())
    for x in (43, 44, 51, 52):                       # the gate
        for y in range(G + 1, G + 7):
            S.set(x, y, D - 2, mc("pink_concrete"))
    for x in range(43, 53):
        for y in (G + 7, G + 8):
            S.set(x, y, D - 2, mc("pink_concrete"))
    for (x, y) in ((46, G + 8), (49, G + 8)):
        S.set(x, y, D - 2, mc("black_concrete"))
    S.set(47, G + 7, D - 2, mc("pink_terracotta"))
    S.set(48, G + 7, D - 2, mc("pink_terracotta"))
    for ex in (43, 51):
        for dx in range(2):
            for y in (G + 9, G + 10):
                S.set(ex + dx, y, D - 2, mc("magenta_concrete") if y == G + 9 and dx == (1 if ex == 43 else 0) else mc("pink_concrete"))
    S.set(45, G + 9, D - 2, *lamp("false", "roze"))
    S.set(50, G + 9, D - 2, *lamp("false", "roze"))

    # --- guh statues with a golf flag, round the course -------------------------------------------------------------------
    for (x, z, facing) in ((40, 76, "east"), (64, 64, "west"), (14, 30, "east"), (70, 50, "south")):
        guh_statue(S, mc, x, z, facing)

    # --- little paths of pink gravel: from the plaza to hole 1, and from every cup to the next tee (over the grass) --------
    stops = [(47, 70)]
    for n, lane in enumerate(LANES):
        stops.append(lane["tee"][:2])
        stops.append(lane["cup"])
    stops.append((47, 69))
    def walk(a, b):
        (x0, z0), (x1, z1) = a, b
        cells = [(x, z0) for x in range(min(x0, x1), max(x0, x1) + 1)] + [(x1, z) for z in range(min(z0, z1), max(z0, z1) + 1)]
        for (x, z) in cells:
            for dx in (0, 1):
                if S.get(x + dx, G, z) == "minecraft:grass_block" and S.get(x + dx, G + 1, z) in (None, "minecraft:air"):
                    S.set(x + dx, G, z, mc("pink_concrete_powder") if (x + z) % 5 else mc("smooth_quartz"))
    walk(stops[0], stops[1])
    for i in range(2, len(stops) - 1, 2):
        walk(stops[i], stops[i + 1])

    # --- the garden: blossom trees, flowers, lamps where it's open grass --------------------------------------------------
    taken = set()
    for (x, y, z) in list(S.blocks):
        if y >= G and S.get(x, y, z) not in ("minecraft:grass_block", "minecraft:air", None):
            taken.add((x, z))
    for (x, z) in ((18, 70), (30, 66), (16, 40), (40, 26), (70, 22), (70, 40), (66, 58), (72, 64), (20, 92), (30, 90), (62, 30),
                   (12, 20), (50, 30), (10, 48)):
        if all((x + dx, z + dz) not in taken for dx in range(-3, 4) for dz in range(-3, 4)):
            h.blossom_tree(S, rng, x, G + 1, z)
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    taken.add((x + dx, z + dz))
    for x, z in fp:
        if (x, z) in taken or min(x, z, W - 1 - x, D - 1 - z) < 3 or S.get(x, G, z) != "minecraft:grass_block" or S.get(x, G + 1, z):
            continue
        r = rng.random()
        if r < 0.07:
            S.set(x, G + 1, z, rng.choice(FLOWERS))
        elif r < 0.075 and (x + z) % 3 == 0:
            S.set(x, G + 1, z, mc("cherry_fence"), FENCE0)
            S.set(x, G + 2, z, *lamp())

    S.clear_above(fp, G + 1)
    _drop_loose_leaves(S)
    return S


def guh_statue(S, mc, x, z, facing):
    """A block guh (3 wide, about 4 high) on a golden plinth, looking `facing`, with a golf flag next to it."""
    fx, fz = STEP[facing]
    sx, sz = -fz, fx                                   # to its side
    def at(f, s_, y, block):
        S.set(x + fx * f + sx * s_, y, z + fz * f + sz * s_, block)
    for f in range(-2, 3):
        for s_ in range(-2, 3):
            at(f, s_, G, mc("gold_block") if abs(f) == 2 or abs(s_) == 2 else mc("smooth_quartz"))
    for f in range(-1, 2):                             # a round pink body
        for s_ in range(-1, 2):
            for y in (G + 1, G + 2):
                at(f, s_, y, mc("pink_wool"))
    for s_ in range(-1, 2):                            # the head at the front, a bit higher
        for y in (G + 2, G + 3):
            at(2, s_, y, mc("pink_wool"))
    at(2, -1, G + 3, mc("black_concrete"))
    at(2, 1, G + 3, mc("black_concrete"))
    at(3, 0, G + 2, mc("pink_terracotta"))             # the nose
    at(2, -1, G + 4, mc("pink_concrete"))              # ears
    at(2, 1, G + 4, mc("pink_concrete"))
    at(-2, 0, G + 1, mc("white_wool"))                 # its tail... or a golf ball?
    S.set(x - sx * 2, G + 1, z - sz * 2, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    S.set(x - sx * 2, G + 2, z - sz * 2, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    S.set(x - sx * 2, G + 3, z - sz * 2, mc("pink_wool"))
    S.set(x - sx * 2 + fx, G + 3, z - sz * 2 + fz, mc("pink_wool"))


def _drop_loose_leaves(S):
    """Blossom trees are random: a leaf that ended up on its own (touching nothing) goes."""
    for p, (name, _pr, _nb) in list(S.blocks.items()):
        if name == "guhs:guhbloesem_leaves":
            x, y, z = p
            if not any(S.get(x + d[0], y + d[1], z + d[2]) not in (None, "minecraft:air")
                       for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))):
                S.set(x, y, z, "minecraft:air")


# ---------------------------------------------------------------------------------------------------------------------
# the self-check: walkable floors without holes, the way in, the Golfguh on solid ground, nothing floating, every lane
# playable from tee to cup
# ---------------------------------------------------------------------------------------------------------------------
PASSABLE = ("minecraft:air", "minecraft:cherry_sign", "minecraft:cherry_wall_sign", "minecraft:pink_carpet", "minecraft:black_carpet",
            "minecraft:magenta_carpet", "guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes", "guhs:vlaggetjes")
FLUID = ("guhs:kaas_saus",)


def _passable(S, x, y, z):
    b = S.get(x, y, z)
    return b is None or b in PASSABLE


def _solid(S, x, y, z):
    b = S.get(x, y, z)
    return b is not None and b not in PASSABLE and b not in FLUID


def check(S):
    problems = []
    # 1. the foundation and the floor: no holes anywhere (kaassaus pits have a floor under them)
    for x in range(W):
        for z in range(D):
            if not _solid(S, x, 0, z):
                problems.append(f"hole in the foundation at {(x, 0, z)}")
            b = S.get(x, G, z)
            if b is None or b == "minecraft:air":
                problems.append(f"hole in the floor at {(x, G, z)}")
    # 2. where you can stand: (x, z) -> feet height (the lowest free spot on something solid, up to 1 block up)
    def stand(x, z):
        for y in (G + 1, G + 2, G + 3):
            if _solid(S, x, y - 1, z) and _passable(S, x, y, z) and _passable(S, x, y + 1, z):
                return y
            if S.get(x, y - 1, z) in FLUID and _passable(S, x, y, z):
                return y - 1
        return None
    start = (ENTRANCE[0], ENTRANCE[1])
    seen = {start: stand(*start)}
    queue = deque([start])
    while queue:
        x, z = queue.popleft()
        y = seen[(x, z)]
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if not (0 <= n[0] < W and 0 <= n[1] < D) or n in seen:
                continue
            ny = stand(*n)
            if ny is None or ny - y > 1 or (ny > y and not _passable(S, x, y + 2, z)):
                continue
            seen[n] = ny
            queue.append(n)
    npc = (int(math.floor(NPC[0])), int(math.floor(NPC[2])))
    if npc not in seen:
        problems.append("the Golfguh can't be reached from the entrance")
    if not _solid(S, npc[0], G, npc[1]) or not _passable(S, npc[0], G + 1, npc[1]) or not _passable(S, npc[0], G + 2, npc[1]):
        problems.append("the Golfguh isn't standing on solid ground with room above")
    for n, lane in enumerate(LANES):
        for kind, level in TEE_KINDS:
            tx, tz, facing = lane[kind]
            ty = tee_y(lane, kind)
            if S.get(tx, ty, tz) != "guhs:golfbaan_afslag" or S.blocks[(tx, ty, tz)][1].get("niveau") != level:
                problems.append(f"hole {n + 1}: no {level} tee at {(tx, ty, tz)}")
            sx, sz = STEP[facing]
            spot = (tx - 2 * sx, tz - 2 * sz)
            if spot not in lane["cells"] or spot not in seen:
                problems.append(f"hole {n + 1}: nowhere to stand behind the {level} tee {spot}")
            if not (_passable(S, spot[0], ty + 1, spot[1]) and _passable(S, spot[0], ty + 2, spot[1])):
                problems.append(f"hole {n + 1}: the spot behind the {level} tee isn't free")
            if level == "lastig" and spot in lane["bumpers_l"]:
                problems.append(f"hole {n + 1}: a lastig bumper where you stand to hit")
        cx, cz = lane["cup"]
        if not any((cx + dx, cz + dz) in seen for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            problems.append(f"hole {n + 1}: you can't walk to the cup")
    # 3. the ball can roll from every tee to its cup (over felt, tees, ramps, slime pads and tongue; hole 5 jumps its ditch)
    course = ("guhs:golfbaan_vilt", "guhs:golfbaan_afslag", "guhs:golfbaan_hole", "guhs:tong", "guhs:roze_slijmblok",
              "minecraft:cherry_slab", "minecraft:cherry_stairs")
    def rolls(x, z, n):
        for y in (G + 1, G):
            b = S.get(x, y, z)
            if b in course:
                return _passable(S, x, y + 1, z) or S.get(x, y + 1, z) == "guhs:golfbaan_wiek"   # (the sails turn)
            if b in FLUID:
                return n == 4
        return False
    for n, lane in enumerate(LANES):
        goal = lane["cup"]
        for kind, level in TEE_KINDS:
            blocked = set(lane["bumpers_l"]) if level == "lastig" else set()
            tx, tz, _f = lane[kind]
            got, queue = {(tx, tz)}, deque([(tx, tz)])
            while queue:
                c = queue.popleft()
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    m = (c[0] + dx, c[1] + dz)
                    if m not in got and m not in blocked and m in lane["cells"] and rolls(*m, n):
                        got.add(m)
                        queue.append(m)
            if goal not in got:
                problems.append(f"hole {n + 1}: the ball can't get from the {level} tee to the cup")
        for (x, z) in lane["bumpers_l"]:              # a lastig bumper goes on free felt (the Golfguh only fills air)
            if (x, z) not in lane["cells"] or S.get(x, G, z) not in ("guhs:golfbaan_vilt", "guhs:golfbaan_afslag") \
                    or not _passable(S, x, G + 1, z) or (x, z) == goal:
                problems.append(f"hole {n + 1}: the lastig bumper {(x, z)} isn't on free felt")
        for (x, z) in lane["walls"]:
            if not _solid(S, x, G + 1, z) and S.get(x, G + 1, z) not in ("minecraft:air",):
                problems.append(f"hole {n + 1}: odd wall at {(x, z)}")
    # 4. nothing floating: every block hangs together with the foundation
    solid = {p for p, (name, _pr, _nb) in S.blocks.items() if name != "minecraft:air"}
    reached = {p for p in solid if p[1] == 0}
    queue = deque(reached)
    while queue:
        x, y, z = queue.popleft()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            m = (x + d[0], y + d[1], z + d[2])
            if m in solid and m not in reached:
                reached.add(m)
                queue.append(m)
    floating = solid - reached
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. {sorted(floating)[:5]}")
    problems += _java_check()
    return problems


def _java_check():
    """GolfBanen.java has the same pars, cups and lastig bumpers as this file (the Java side places the bumpers)."""
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "java", "nl", "juiced", "guhs",
                        "feature", "golf", "GolfBanen.java")
    text = open(path, encoding="utf-8").read()
    problems = []

    def block(name):
        start = text.index(name)
        return text[text.index("{", start):text.index(";", start)]
    pars = [[int(v) for v in re.findall(r"\d+", row)] for row in re.findall(r"\{([\d,\s]+)\}", block("int[][] PARS"))]
    if pars != [PARS["makkelijk"], PARS["medium"], PARS["lastig"]]:
        problems.append(f"GolfBanen.PARS {pars} != golf.py PARS")
    cups = [tuple(int(v) for v in row.split(",")) for row in re.findall(r"\{(\d+, \d+, \d+)\}", block("int[][] CUPS"))]
    want = [(lane["cup"][0], lane.get("cup_y", G), lane["cup"][1]) for lane in LANES]
    if cups != want:
        problems.append(f"GolfBanen.CUPS {cups} != {want}")
    rows = re.findall(r"/\* (\d) \*/ \{(.*)\},?\s*$", block("int[][][] LASTIG_BUMPERS"), re.M)
    bumpers = {int(n): [tuple(int(v) for v in b.split(",")) for b in re.findall(r"\{(\d+, \d+)\}", body)] for n, body in rows}
    for n, lane in enumerate(LANES):
        if bumpers.get(n + 1) != list(lane["bumpers_l"]):
            problems.append(f"hole {n + 1}: GolfBanen.LASTIG_BUMPERS {bumpers.get(n + 1)} != {lane['bumpers_l']}")
    facings = re.findall(r"Direction\.(\w+)", block("Direction[] TEE_FACING"))
    if [f.lower() for f in facings] != [lane["tee"][2] for lane in LANES]:
        problems.append(f"GolfBanen.TEE_FACING {facings}")
    return problems


# ---------------------------------------------------------------------------------------------------------------------
# textures
# ---------------------------------------------------------------------------------------------------------------------
FELT = (242, 150, 192)
DIGITS = {"1": ["010", "110", "010", "010", "111"], "2": ["111", "001", "111", "100", "111"], "3": ["111", "001", "111", "001", "111"],
          "4": ["101", "101", "111", "001", "001"], "5": ["111", "100", "111", "001", "111"], "6": ["111", "100", "111", "101", "111"],
          "7": ["111", "001", "010", "010", "010"], "8": ["111", "101", "111", "101", "111"], "9": ["111", "101", "111", "001", "111"]}


def _felt(seed):
    img = Image.new("RGBA", (16, 16))
    r = random.Random(seed)
    for y in range(16):
        for x in range(16):
            stripe = 10 if (x // 4) % 2 else -4             # mowed stripes
            v = r.randint(-8, 8) + stripe
            img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in FELT) + (255,))
    return img


def _textures(h):
    felt = _felt(2401)
    h.save(felt, "block", "golfbaan_vilt.png")
    # the tee: a white mat with a golden peg and an arrow pointing the way (north on the texture); 2.10: one per level, the
    # arrow (and the edge) green for makkelijk, pink for medium (as always), red for lastig
    for suffix, arrow, edge in (("", (224, 84, 150), (250, 208, 76)), ("_makkelijk", (70, 178, 96), (150, 226, 150)),
                                ("_lastig", (212, 44, 60), (238, 120, 60))):
        tee = felt.copy()
        for y in range(2, 14):
            for x in range(2, 14):
                tee.putpixel((x, y), (252, 246, 250, 255) if 2 < x < 13 and 2 < y < 13 else edge + (255,))
        for i, row in enumerate(["...pp...", "..pppp..", ".pppppp.", "...pp...", "...pp...", "...pp..."]):
            for j, ch in enumerate(row):
                if ch == "p":
                    tee.putpixel((4 + j, 3 + i), arrow + (255,))
        for (x, y) in ((7, 11), (8, 11), (7, 12), (8, 12)):
            tee.putpixel((x, y), (250, 208, 76, 255))
        if suffix == "_lastig":                    # two little Mika horns at the top corners: watch out!
            for (x, y) in ((3, 3), (4, 3), (3, 4), (11, 3), (12, 3), (12, 4)):
                tee.putpixel((x, y), (140, 40, 90, 255))
        if suffix == "_makkelijk":                 # and a little heart at the bottom: take it easy
            for (x, y) in ((4, 11), (6, 11), (4, 12), (5, 12), (6, 12), (5, 13)):
                tee.putpixel((x, y), (236, 96, 160, 255))
        h.save(tee, "block", f"golfbaan_afslag{suffix}.png")
    h.save(h.noise_tex((58, 32, 46), 8, 2402), "block", "golfbaan_hole_binnen.png")
    rim = felt.copy()
    for i in range(16):
        for j in range(2):
            rim.putpixel((i, j), (255, 255, 255, 255))
    h.save(rim, "block", "golfbaan_hole_rand.png")
    pole = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            pole.putpixel((x, y), (250, 208, 76, 255) if (y // 3) % 2 else (255, 255, 255, 255))
    h.save(pole, "block", "golfbaan_vlag_stok.png")
    for n in range(1, 10):                         # the flags: pink with a white number (and a white border)
        flag = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y in range(12):
            for x in range(16):
                edge = y in (0, 11) or x == 15
                flag.putpixel((x, y), (255, 255, 255, 255) if edge else (236, 96, 160, 255))
        for i, row in enumerate(DIGITS[str(n)]):
            for j, ch in enumerate(row):
                if ch == "1":
                    for dy in range(2):
                        for dx in range(2):
                            flag.putpixel((5 + j * 2 + dx, 1 + i * 2 + dy), (255, 255, 255, 255))
        h.save(flag, "block", f"golfbaan_vlag_{n}.png")
    # the windmill: a guh face on the hub, candy-striped sails
    face = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = math.dist((x + 0.5, y + 0.5), (8, 8.5))
            face.putpixel((x, y), (250, 208, 76, 255) if d > 7.2 else (248, 170, 200, 255))
    for (x, y) in ((4, 6), (5, 6), (4, 7), (5, 7), (10, 6), (11, 6), (10, 7), (11, 7)):
        face.putpixel((x, y), (30, 20, 30, 255))
    face.putpixel((4, 6), (255, 255, 255, 255))
    face.putpixel((10, 6), (255, 255, 255, 255))
    for (x, y) in ((7, 9), (8, 9)):
        face.putpixel((x, y), (214, 84, 148, 255))
    for (x, y) in ((3, 10), (12, 10)):
        face.putpixel((x, y), (230, 110, 170, 255))
    for (x, y) in ((6, 11), (7, 12), (8, 12), (9, 11)):
        face.putpixel((x, y), (140, 40, 90, 255))
    h.save(face, "block", "golfbaan_molenas.png")
    h.save(h.noise_tex((250, 246, 240), 6, 2403), "block", "golfbaan_molenas_zijkant.png")
    sail = Image.new("RGBA", (16, 16))
    r = random.Random(2404)
    for y in range(16):
        for x in range(16):
            c = (236, 96, 160) if ((x + y) // 4) % 2 else (252, 248, 250)
            v = r.randint(-5, 5)
            sail.putpixel((x, y), tuple(max(0, min(255, ch + v)) for ch in c) + (255,))
    h.save(sail, "block", "golfbaan_wiek.png")
    # the golf ball: a pink guh rolled up (32x16, box UV: a 5x5x5 ball and two little ears)
    ball = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    r = random.Random(2405)
    for y in range(10):
        for x in range(20):
            if (y < 5 and 5 <= x < 15) or y >= 5:
                v = r.randint(-8, 8)
                dimple = (x + y * 2) % 3 == 0
                c = (236, 150, 186) if dimple else (250, 186, 212)
                ball.putpixel((x, y), tuple(max(0, min(255, ch + v)) for ch in c) + (255,))
    for (x, y) in ((6, 6), (8, 6)):                # the face on the front (north: u 5..10, v 5..10)
        ball.putpixel((x, y), (30, 20, 30, 255))
    ball.putpixel((7, 7), (214, 84, 148, 255))
    ball.putpixel((5, 8), (240, 110, 170, 255))
    ball.putpixel((9, 8), (240, 110, 170, 255))
    for x in range(0, 8):
        for y in range(10, 12):
            ball.putpixel((x, y), (214, 104, 158, 255))
    h.save(ball, "entity", "guh_golfbal.png")
    # the Golfguh: a fresh golf-green guh
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    h.save(h.recolour(src, hue=0.36, sat=0.75, val=1.02, only=h.pinkish), "entity", "npc_golfguh.png")
    # items: the golfballetje (a white guh golf ball with pink ears) and the club
    h.save(h.grid(["................", "....pp....pp....", "....pPp..pPp....", "....pwwwwwwp....", "...wwwswwswww...",
                   "..wwkwwwwwwkww..", "..wswwwmmwwwsw..", "..wwwwwwwwwwww..", "..wwswwwwwwsww..", "..wwwwswwswwww..",
                   "...wwwwwwwwww...", "....wwsswwww....", ".....wwwwww.....", "................", "................",
                   "................"], h.ITEM_PAL), "item", "golfballetje.png")
    h.save(h.grid(["............mm..", "...........mPm..", "..........mPm...", ".........sSm....", "........sS......",
                   ".......sS.......", "......sS........", ".....sS.........", "....sS..........", "...sS...........",
                   "..sS............", ".gsS............", "gGgg............", "gGGgg...........", ".gggg...........",
                   "................"], h.ITEM_PAL), "item", "guhgolfclub.png")


def _models(h):
    A, D = h.A, h.D
    h.simple_block("golfbaan_vilt")
    h.self_drop("golfbaan_vilt")
    h.shaped("golfbaan_vilt", ["WW", "WW"], {"W": "minecraft:pink_wool"}, "guhs:golfbaan_vilt", 4)
    h.simple_block("golfbaan_wiek")
    for suffix in ("", "_makkelijk", "_lastig"):
        h.w(f"{A}/models/block/golfbaan_afslag{suffix}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"guhs:block/golfbaan_afslag{suffix}", "side": "guhs:block/golfbaan_vilt", "bottom": "guhs:block/golfbaan_vilt"}})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    model = {"makkelijk": "guhs:block/golfbaan_afslag_makkelijk", "medium": "guhs:block/golfbaan_afslag", "lastig": "guhs:block/golfbaan_afslag_lastig"}
    h.w(f"{A}/blockstates/golfbaan_afslag.json", {"variants": {
        f"facing={f},hole={n},niveau={lvl}": ({"model": model[lvl], "y": r} if r else {"model": model[lvl]})
        for f, r in rot.items() for n in range(1, 10) for lvl in ("makkelijk", "medium", "lastig")}})
    h.w(f"{A}/models/item/golfbaan_afslag.json", {"parent": "guhs:block/golfbaan_afslag"})
    # the cup: a hole in the felt with a numbered flag on a golden pole
    inner = {"texture": "#inner"}
    for n in range(1, 10):
        felt = lambda cull=None: {"texture": "#felt", **({"cullface": cull} if cull else {})}
        els = [{"from": [0, 0, 0], "to": [16, 8, 16], "faces": {
            "down": felt("down"), "north": felt("north"), "south": felt("south"), "east": felt("east"), "west": felt("west"),
            "up": {"texture": "#inner"}}}]
        for frm, to in (([0, 8, 0], [3, 16, 16]), ([13, 8, 0], [16, 16, 16]), ([3, 8, 0], [13, 16, 3]), ([3, 8, 13], [13, 16, 16])):
            faces = {}
            for side in ("north", "south", "east", "west"):
                outer = (side == "west" and frm[0] == 0) or (side == "east" and to[0] == 16) or \
                        (side == "north" and frm[2] == 0) or (side == "south" and to[2] == 16)
                faces[side] = felt(side) if outer else dict(inner)
            faces["up"] = {"texture": "#rim", "uv": [frm[0], frm[2], to[0], to[2]], "cullface": "up"}
            els.append({"from": frm, "to": to, "faces": faces})
        els.append({"from": [7.5, 8, 7.5], "to": [8.5, 31, 8.5], "faces": {s: {"texture": "#pole"} for s in ("north", "south", "east", "west", "up")}})
        els.append({"from": [8.5, 24, 8], "to": [16.5, 30, 8], "faces": {"north": {"texture": "#flag", "uv": [16, 0, 0, 12]},
                                                                         "south": {"texture": "#flag", "uv": [0, 0, 16, 12]}}})
        h.w(f"{A}/models/block/golfbaan_hole_{n}.json", {"textures": {
            "particle": "guhs:block/golfbaan_vilt", "felt": "guhs:block/golfbaan_vilt", "inner": "guhs:block/golfbaan_hole_binnen",
            "rim": "guhs:block/golfbaan_vilt", "pole": "guhs:block/golfbaan_vlag_stok", "flag": f"guhs:block/golfbaan_vlag_{n}"},
            "render_type": "minecraft:cutout", "elements": els})
    h.w(f"{A}/blockstates/golfbaan_hole.json", {"variants": {f"hole={n}": {"model": f"guhs:block/golfbaan_hole_{n}"} for n in range(1, 10)}})
    h.w(f"{A}/models/item/golfbaan_hole.json", {"parent": "guhs:block/golfbaan_hole_1"})
    h.w(f"{A}/models/block/golfbaan_molenas.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": "guhs:block/golfbaan_molenas", "side": "guhs:block/golfbaan_molenas_zijkant", "top": "guhs:block/golfbaan_molenas_zijkant"}})
    h.w(f"{A}/blockstates/golfbaan_molenas.json", {"variants": {
        f"facing={f}": ({"model": "guhs:block/golfbaan_molenas", "y": r} if r else {"model": "guhs:block/golfbaan_molenas"})
        for f, r in rot.items()}})
    h.w(f"{A}/models/item/golfbaan_molenas.json", {"parent": "guhs:block/golfbaan_molenas"})
    h.item_model("golfballetje")
    h.item_model("guhgolfclub", parent="minecraft:item/handheld")


def _advancements(h):
    D = h.D
    for name in ("golf_ronde", "golf_par", "golf_hole_in_one", "golf_lastig_par"):
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for name, parent, icon, frame, crit, en, nl in [
        ("find_guh_golfbaan", "enter_guhmension", "guhs:golfballetje", "goal",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:guh_golfbaan"]}}}},
         ("Fore, guh!", "Find the rare guh golf course"), ("Fore, guh!", "Vind de zeldzame guhgolfbaan")),
        ("golf_gespeeld", "find_guh_golfbaan", "guhs:golfballetje", "goal",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:golfballetje"}]}},
         ("Balletje, balletje", "Earn a golfballetje on the guh golf course"), ("Balletje, balletje", "Verdien een golfballetje op de guhgolfbaan")),
        ("golf_outfit", "golf_gespeeld", "guhs:golf_pet", "challenge",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
             {"items": "guhs:golf_pet"}, {"items": "guhs:golf_zonneklep"}, {"items": "guhs:golf_trui"}]}},
         ("Guh on the green", "Buy the whole golf outfit from the Golfguh"), ("Guh op de green", "Koop het hele golfpakje bij de Golfguh")),
    ]:
        h.w(f"{D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": crit}})
        h.lang(f"advancements.guhs.guhmension.{name}.title", en[0], nl[0])
        h.lang(f"advancements.guhs.guhmension.{name}.description", en[1], nl[1])


TEXTS = {  # key: (English, Dutch)
    "block.guhs.golfbaan_vilt": ("Golf Felt", "Golfvilt"),
    "block.guhs.golfbaan_afslag": ("Guh Golf Tee", "Afslagmatje"),
    "block.guhs.golfbaan_hole": ("Guh Golf Hole", "Golfhole"),
    "block.guhs.golfbaan_molenas": ("Guh Windmill Hub", "Guhmolen-as"),
    "block.guhs.golfbaan_wiek": ("Windmill Sail", "Molenwiek"),
    "item.guhs.golfballetje": ("Golfballetje", "Golfballetje"),
    "item.guhs.guhgolfclub": ("Guh Golf Club (borrowed)", "Guhgolfclub (geleend)"),
    "item.guhs.guhgolfclub.lore": ("Hold right-click near your ball to charge the power bar, look where it has to go, let go to hit. VAHOEG!",
                                   "Houd rechtsklik ingedrukt bij je bal om de krachtbalk op te laden, kijk waar hij heen moet en laat los om te slaan. VAHOEG!"),
    "item.guhs.guhgolfclub.loan": ("On loan from the Golfguh: she wants it back after your round.",
                                   "Geleend van de Golfguh: na je rondje wil ze hem terug."),
    "item.guhs.golf_pet": ("Vadsy Flat Cap", "Vadsige golfpet"),
    "item.guhs.golf_zonneklep": ("Guh Sun Visor", "Guhzonneklep"),
    "item.guhs.golf_trui": ("Argyle Vads Sweater", "Ruitjesvadstrui"),
    "entity.guhs.guh_golfbal": ("Guh Golf Ball", "Guhgolfbal"),
    "entity.guhs.guh_npc.golfguh": ("Golfguh", "Golfguh"),
    "structure.guhs.guh_golfbaan": ("Guh Golf Course", "Guhgolfbaan"),
    "structure.guhs.guh_golfbaan.tooltip": ("Minigame: 9-hole mini golf with the Golfguh, golfballetjes and a golf outfit",
                                            "Minigame: 9 holes minigolf met de Golfguh, golfballetjes en een golfpakje"),
    # the Golfguh talks
    "quest.guhs.golf.hello1": ("NJEG! Welcome to the Guh Golf Course! Nine holes, one ball, zero kaasknabbels (I ate them). Fancy a round? Vahoeg!",
                               "NJEG! Welkom op de Guhgolfbaan! Negen holes, één bal, nul kaasknabbels (die heb ik opgegeten). Zin in een rondje? Vahoeg!"),
    "quest.guhs.golf.hello2": ("Fore! Oops, you only shout that when a ball comes flying... I mean: hi! Want to golf? I'll lend you my very best club.",
                               "Fore! Oeps, dat roep je alleen als er een bal aan komt vliegen... Ik bedoel: hoi! Wil je golfen? Ik leen je mijn allermooiste club."),
    "quest.guhs.golf.hello3": ("Did you know a guh golf ball is really a guh rolled up into a ball? It loves it! Will you play a round?",
                               "Wist je dat een guhgolfbal eigenlijk een opgerolde guh is? Hij vindt het heerlijk! Speel je een rondje?"),
    "quest.guhs.golf.playing": ("You're golfing! Your ball is waiting for you. Want to stop? You can do that here too.",
                                "Je bent aan het golfen! Je bal wacht op je. Wil je stoppen? Dat kan hier ook."),
    "quest.guhs.golf.busy": ("Njeg, %s is golfing right now. Watch along, or come back in a bit!",
                             "Njeg, %s is nu aan het golfen. Kijk lekker mee of kom zo terug!"),
    "quest.guhs.golf.elsewhere": ("You're already golfing on another course!", "Je bent al aan het golfen op een andere baan!"),
    "quest.guhs.golf.broken": ("Njeg... I can't find my holes any more. Is my course broken?",
                               "Njeg... ik kan mijn holes niet meer vinden. Is mijn baan kapot?"),
    "quest.guhs.golf.full": ("Your pockets are full! Free up one spot for the golf club and I'll lend it to you.",
                             "Je zakken zitten vol! Maak één plekje vrij voor de golfclub, dan leen ik hem je."),
    "quest.guhs.golf.start": ("Yay! Here's your golf club. Your ball is on hole 1. Ready? 3... 2... 1...",
                              "Joepie! Hier is je golfclub. Je bal ligt klaar op hole 1. Klaar? 3... 2... 1..."),
    "quest.guhs.golf.how": ("Houd rechtsklik ingedrukt met de club om de krachtbalk op te laden (hoe langer, hoe harder, tot 100%%), kijk waar de bal heen moet en laat los om te slaan. Kaassaus of buiten de baan = 1 strafslag. Elke hole heeft drie afslagmatjes: groen (makkelijk, vlak bij de hole), roze (medium) en rood (lastig, helemaal achteraan).",
                            "Houd rechtsklik ingedrukt met de club om de krachtbalk op te laden (hoe langer, hoe harder, tot 100%%), kijk waar de bal heen moet en laat los om te slaan. Kaassaus of buiten de baan = 1 strafslag. Elke hole heeft drie afslagmatjes: groen (makkelijk, vlak bij de hole), roze (medium) en rood (lastig, helemaal achteraan)."),
    "quest.guhs.golf.stopped": ("You stop golfing. The club goes back in my bag. See you next time!",
                                "Je stopt met golfen. De club gaat terug in mijn tas. Tot de volgende keer!"),
    "quest.guhs.golf.walked_away": ("You walked away from the golf course. The Golfguh takes her club back.",
                                    "Je liep weg van de golfbaan. De Golfguh pakt haar club terug."),
    "quest.guhs.golf.idle": ("Njeg, did you fall asleep? The Golfguh ends your round.", "Njeg, ben je in slaap gevallen? De Golfguh stopt je rondje."),
    "quest.guhs.golf.hole_done": ("Hole %s (%s): %s strokes (par %s) - %s +%s golfballetjes",
                                  "Hole %s (%s): %s slagen (par %s) - %s +%s golfballetjes"),
    "quest.guhs.golf.card": ("=== Your guh golf scorecard ===", "=== Jouw guhgolf-scorekaart ==="),
    "quest.guhs.golf.total": ("Total: %s strokes (%s). Bonus: +%s golfballetjes", "Totaal: %s slagen (%s). Bonus: +%s golfballetjes"),
    "quest.guhs.golf.record": ("New record: %s strokes! (it was %s)", "Nieuw record: %s slagen! (was %s)"),
    "quest.guhs.golf.first_record": ("Your first round: %s strokes. That's your record straight away!",
                                     "Je eerste rondje: %s slagen. Dat is meteen je record!"),
    "quest.guhs.golf.no_record": ("Your record stays at %s strokes. One more round?", "Je record blijft %s slagen. Nog een rondje?"),
    "quest.guhs.golf.first": ("Your very first round of guh golf! A present from the Golfguh: 6 golfballetjes, kaasknabbels and fried kaasknabbels. VAHOEG!",
                              "Je allereerste rondje guhgolf! Een cadeautje van de Golfguh: 6 golfballetjes, kaasknabbels en gefrituurde kaasknabbels. VAHOEG!"),
    "quest.guhs.golf.club_record": ("CLUB RECORD! Your name now floats at the very top above the Golfguh. Vadsig goed!",
                                    "CLUBRECORD! Jouw naam zweeft nu helemaal bovenaan boven de Golfguh. Vadsig goed!"),
    # on screen
    "gui.guhs.golf.hole_title": ("Hole %s", "Hole %s"),
    "gui.guhs.golf.hole_sub": ("%s - par %s", "%s - par %s"),
    "gui.guhs.golf.go": ("VAHOEG!", "VAHOEG!"),
    "gui.guhs.golf.go_sub": ("Swing away!", "Sla maar raak!"),
    "gui.guhs.golf.bar": ("Hole %s/9: %s (par %s) | stroke %s/%s | %s blocks to go | total %s",
                          "Hole %s/9: %s (par %s) | slag %s/%s | nog %s blokken | totaal %s"),
    "gui.guhs.golf.poke": ("Njeg, don't poke it! Hold right-click with your club and let go to hit.",
                           "Njeg, niet porren! Houd rechtsklik ingedrukt met je club en laat los om te slaan."),
    "gui.guhs.golf.wait_countdown": ("Wait for it... 3, 2, 1!", "Nog even wachten... 3, 2, 1!"),
    "gui.guhs.golf.wait_ball": ("Wait until your ball lies still!", "Wacht tot je bal stil ligt!"),
    "gui.guhs.golf.too_far": ("Walk to your ball (within 3 blocks) to hit it", "Loop naar je bal (binnen 3 blokken) om te slaan"),
    "gui.guhs.golf.vahoeg": ("VAHOEG! Full power!", "VAHOEG! Volle kracht!"),
    "gui.guhs.golf.saus": ("SPLOSH!", "PLONS!"),
    "gui.guhs.golf.out": ("Out of bounds!", "Buiten de baan!"),
    "gui.guhs.golf.wrong_cup": ("Njeg, that's the wrong hole!", "Njeg, dat is de verkeerde hole!"),
    "gui.guhs.golf.penalty": ("+1 penalty stroke, your ball goes back", "+1 strafslag, je bal gaat terug"),
    "gui.guhs.golf.lip": ("Hit the rim! So close...", "Rand geraakt! Net niet..."),
    "gui.guhs.golf.result.ace": ("HOLE-IN-ONE!", "HOLE-IN-ONE!"),
    "gui.guhs.golf.result.albatros": ("ALBATROSS!", "ALBATROS!"),
    "gui.guhs.golf.result.eagle": ("EAGLE!", "EAGLE!"),
    "gui.guhs.golf.result.birdie": ("Birdie!", "Birdie!"),
    "gui.guhs.golf.result.par": ("Par", "Par"),
    "gui.guhs.golf.result.bogey": ("Bogey", "Bogey"),
    "gui.guhs.golf.result.double_bogey": ("Double bogey", "Dubbele bogey"),
    "gui.guhs.golf.result.more": ("Vadsy lot of strokes", "Vadsig veel slagen"),
    "gui.guhs.golf.result.max": ("8 strokes: picked up", "8 slagen: opgeraapt"),
    "gui.guhs.golf.result_sub": ("%s strokes, +%s golfballetjes", "%s slagen, +%s golfballetjes"),
    "gui.guhs.golf.done_title": ("Round done!", "Rondje klaar!"),
    "gui.guhs.golf.done_sub": ("%s strokes (%s)", "%s slagen (%s)"),
    "gui.guhs.golf.card_par": ("Par:", "Par:"),
    "gui.guhs.golf.card_you": ("You:", "Jij:"),
    "gui.guhs.golf.sign_par": ("Par %s", "Par %s"),
    "gui.guhs.golf.sign_total": ("9 holes", "9 holes"),
    "gui.guhs.golf.sign_top3_1": ("floats above", "zweeft boven"),
    "gui.guhs.golf.sign_top3_2": ("the Golfguh!", "de Golfguh!"),
    "gui.guhs.scorebord.golf": ("Guh golf top 3: fewest strokes", "Guhgolf top 3: minste slagen"),
    "gui.guhs.golf.scorebord_heading": ("9 holes, par %s", "9 holes, par %s"),
    "gui.guhs.golf.no_build": ("Njeg! The guh golf course is sacred: no breaking or building here.",
                               "Njeg! De guhgolfbaan is heilig: hier mag je niks slopen of bouwen."),
    "gui.guhs.golf.power": ("Power: %s%%", "Kracht: %s%%"),
    "gui.guhs.golf.question": ("A round of guh golf? 9 holes, at most 8 strokes each. The fewer strokes, the more golfballetjes!",
                               "Een rondje guhgolf? 9 holes, maximaal 8 slagen per hole. Hoe minder slagen, hoe meer golfballetjes!"),
    "gui.guhs.golf.mine": ("You're golfing (hole %s). You can always stop.", "Je bent aan het golfen (hole %s). Stoppen kan altijd."),
    "gui.guhs.golf.busy": ("%s is golfing (hole %s). Watch along or wait a bit!", "%s is aan het golfen (hole %s). Kijk mee of wacht even!"),
    "gui.guhs.golf.play": ("Play 9 holes!", "Speel 9 holes!"),
    "gui.guhs.golf.play.tooltip": ("You get a golf club and your ball is ready on hole 1", "Je krijgt een golfclub en je bal ligt klaar op hole 1"),
    "gui.guhs.golf.stop": ("Stop", "Stoppen"),
    "gui.guhs.golf.stop.tooltip": ("Your round ends; you keep the golfballetjes you already have",
                                   "Je rondje stopt; je houdt de golfballetjes die je al hebt"),
    "gui.guhs.golf.shop": ("Shop", "Winkeltje"),
    "gui.guhs.golf.shop.tooltip": ("The golf outfit for your guh, for golfballetjes", "Het golfpakje voor je guh, voor golfballetjes"),
    "gui.guhs.golf.par_line": ("Par per hole: %s (total %s)", "Par per hole: %s (totaal %s)"),
    # 2.10: three tees per hole, a par per level, lastig bumpers (Dutch in both files)
    "gui.guhs.golf.par_line_niveau": ("Par op %s: %s (totaal %s)", "Par op %s: %s (totaal %s)"),
    "gui.guhs.golf.bord_par": ("(max %s slagen, par %s)", "(max %s slagen, par %s)"),
    "gui.guhs.guhdex.info.golfguh": ("De Golfguh leent je een club voor 9 holes guhminigolf. Elke hole heeft drie afslagmatjes: groen voor "
                                     "makkelijk (vlak bij de hole), roze voor medium en rood voor lastig (helemaal achteraan). Op lastig zet ze "
                                     "extra roze slijmbumpers neer en draait haar guhmolen twee keer zo snel. Rechtsklik vasthouden, loslaten, VAHOEG!",
                                     "De Golfguh leent je een club voor 9 holes guhminigolf. Elke hole heeft drie afslagmatjes: groen voor "
                                     "makkelijk (vlak bij de hole), roze voor medium en rood voor lastig (helemaal achteraan). Op lastig zet ze "
                                     "extra roze slijmbumpers neer en draait haar guhmolen twee keer zo snel. Rechtsklik vasthouden, loslaten, VAHOEG!"),
    "gui.guhs.golf.records": ("Your records:", "Jouw records:"),
    "gui.guhs.golf.best": ("Best round: %s   Rounds: %s   Hole-in-ones: %s", "Beste rondje: %s   Rondjes: %s   Hole-in-ones: %s"),
    "gui.guhs.golf.club": ("Club record (no. 1 on the top 3): %s with %s strokes", "Clubrecord (nr. 1 van de top 3): %s met %s slagen"),
    "gui.guhs.golf.club_none": ("Club record: nobody yet!", "Clubrecord: nog niemand!"),
}


def build(h):
    _textures(h)
    _models(h)
    _advancements(h)
    for key, (en, nl) in TEXTS.items():
        h.lang(key, en, nl)
    for name, en, nl in HOLE_NAMES:
        h.lang(f"gui.guhs.golf.hole.{name}", en, nl)
    # the course: rare, big, on flat ground; nothing spawns on it
    h.TEMPLATE_SIZES["guh_golfbaan"] = W
    h.FLATNESS["guh_golfbaan"] = 30
    none = {"bounding_box": "full", "spawns": []}
    h.structure("guh_golfbaan", h.GUHMENSION_LAND, spacing=44, separation=16, salt=20240144,
                spawn_overrides={"creature": none, "monster": none, "ambient": none})
    S = golf_structure(h)
    problems = check(S)
    if problems:
        raise AssertionError("guh_golfbaan self-check:\n  " + "\n  ".join(problems))
    S.save("guh_golfbaan")
    print(f"guh_golfbaan: {len(S.blocks)} blocks, self-check OK")
    _test_lanes(h)


def _test_lanes(h):
    """For the GameTests: three straight lanes (z 1-3, 5-7, 9-11; felt on y=1, walls round them): a plain one, one with
    a cup at x=10 and one with a kaassaus ditch at x=10-11."""
    S = h.Structure((20, 5, 13))
    mc = h.mc
    for x in range(20):
        for z in range(13):
            S.set(x, 0, z, mc("stone"))
            wall = x in (0, 19) or z in (0, 4, 8, 12)
            S.set(x, 1, z, mc("white_concrete") if wall else "guhs:golfbaan_vilt")
            if wall:
                S.set(x, 2, z, mc("white_concrete"))
    S.set(10, 1, 6, "guhs:golfbaan_hole", {"hole": "1"})
    for x in (10, 11):
        for z in (9, 10, 11):
            S.set(x, 1, z, "guhs:kaas_saus", {"level": "0"})
    S.save("golf_testbaan")


def ftb(fq):
    fq.q("golf_baan", "Fore, guh!", "Zoek de zeldzame &dguhgolfbaan&r (superkompas: Minigames > Guhgolfbaan). In het clubhuis met het guhgezicht woont de &aGolfguh&r.",
         "guhs:guhmensie_superkompas", [fq.structure("guh_golfbaan")], x=-8, y=34)
    fq.q("golf_ronde", "Negen holes vol vads", "Speel een heel rondje guhgolf: de Golfguh leent je een club. Houd rechtsklik ingedrukt om de krachtbalk op te laden, kijk waar de bal heen moet en laat los! Met een goed rondje zweef jij in de top 3 boven de Golfguh.",
         "guhs:golfballetje", [fq.adv("golf_ronde")], rewards=(("guhs:golfballetje", 4),), x=-6, y=34, xp=200)
    fq.q("golf_par", "Par-guh", "Speel een rondje op medium (par 27) of lastig (par 36) in par of minder. Tip: de kaassaus kost je een strafslag!",
         "guhs:golfballetje", [fq.adv("golf_par")], rewards=(("guhs:golfballetje", 6),), x=-4, y=34, shape="gear", xp=300)
    fq.q("golf_ace", "VAHOEG! Hole-in-one!", "Sla een bal in één keer in de hole. Het Guhpaadje is lang, maar wie weet...",
         "guhs:golfbaan_vilt", [fq.adv("golf_hole_in_one")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=-2, y=34, shape="hexagon", xp=300)
    fq.q("golf_balletjes", "Balletjesvadser", "Spaar 20 golfballetjes (hoe minder slagen, hoe meer balletjes).",
         "guhs:golfballetje", [fq.item("guhs:golfballetje", 20)], rewards=(("guhs:kaas_knabbels", 16),), x=0, y=34)
    fq.q("golf_pet", "Petje op!", "Koop de vadsige golfpet bij de Golfguh (10 golfballetjes).",
         "guhs:golf_pet", [fq.item("guhs:golf_pet")], x=2, y=34)
    fq.q("golf_outfit", "Guh op de green", "Koop het hele golfpakje: de vadsige golfpet, de guhzonneklep en de ruitjesvadstrui.",
         "guhs:golf_trui", [fq.adv("guhs:guhmension/golf_outfit")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=4, y=34, shape="gear", xp=300)
    fq.q("golf_lastig_par", "Van het rode matje", "Speel een rondje op &clastig&r in par (36) of minder: vanaf de rode afslagmatjes helemaal "
         "achteraan, langs de extra slijmbumpers en onder de turbo-guhmolen door. Dat is pas vahoeg golfen, njeg!",
         "guhs:golfbaan_afslag", [fq.adv("golf_lastig_par")], rewards=(("guhs:golfballetje", 10),), x=6, y=34, shape="gear", xp=300)
