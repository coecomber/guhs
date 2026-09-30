"""
Het Sjoelhuisje (2.9, slice sjoelkatapult) - the template (44 x 30 x 58) in the Guhweides:

  - a long brown wooden hall (vadshout) with Dutch stepped gables at both ends; a big guh face on the front gable over
    the entrance arch (south) and another one on the back gable (north)
  - the roof IS a giant sjoelbak: waxed sjoelbak wood between raised rims, a tall gate bar with the four openings and
    the big numbers 2 3 4 1 on it, lanes behind it, and giant sjoelschijven lying on the roof (one slipping into gate 4)
  - round "puck windows" along both long walls: a ring of dark wood like the edge of a sjoelschijf, glass inside
  - inside, the real sjoelbak (5 wide, 24 long) on its table: head (sjoelen_kop), 19 rows of bakplank, the gate bar
    (sjoelen_poort) and two rows of lanes (sjoelen_vak), rims of dark oak, a tall back board; you stand at the head
  - Opoe Njegschuif next to the head, in her knitting corner (a tea table, a chair, baskets of wool); benches with guh
    plushies watching along both sides; lampions under the ceiling; shelves with stacks of pucks behind the back board
  - a garden round it: a path of knuffelklinkers to the entrance, flower beds, benches, lanterns on posts and a giant
    stack of sjoelschijven with a guh face on top
check(s): nothing floats, Opoe and the head of the bak can be walked to from the entrance, the bak is complete.
Run it alone:  python tools/features/sjoelen_bouw.py   (from the project root)
"""
import math
import os
import random
import sys

if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

from features import sterrenwacht_hulp as hulp  # noqa: E402

AIR = hulp.AIR
NAME = "sjoelhuisje"
W, H, D = 44, 32, 58
G = 4
ANCHOR = "guhs:sjoelhuisje_midden"
ANCHOR_POS = (22, G, 28)

# the hall
X0, X1, Z0, Z1 = 12, 31, 6, 47          # walls
TOP = G + 8                              # the walls go up to here; the roof starts one higher
ROOF = G + 9
# the real sjoelbak (facing north): 5 columns, the head at z = KOP_Z, the gate bar at KOP_Z - 20
BAK_X = 20
KOP_Z = 38
POORT_Z = KOP_Z - 20
BACK_Z = KOP_Z - 23
NPC = (17.5, G + 1, 40.5)
STAND = (22, G + 1, 40)
ENTRANCE = (22, G + 1, D - 1)

PLANKS = "guhs:vadshout_planken"
LOG = "guhs:vadshout_stam"
DARK = "minecraft:dark_oak_planks"
DARK_SLAB = "minecraft:dark_oak_slab"
BAKPLANK = "guhs:sjoelen_bakplank"
KLINK = "guhs:knuffelklinkers"
KS = "guhs:knuffelsteen"
FLOWERS = ["guhs:roze_guhbloem", "guhs:kaasbloem", "guhs:guhoortjes", "guhs:knabbelroos"]
KNUFFELS = ["normal", "mint", "choco", "golden", "rainbow", "snow", "starry", "pluisguh", "glitter", "wolk"]
DIGITS = {"1": ["010", "110", "010", "010", "111"], "2": ["111", "001", "111", "100", "111"], "3": ["111", "001", "111", "001", "111"],
          "4": ["101", "101", "111", "001", "001"]}


def slab(s, x, y, z, kind="bottom", block=DARK_SLAB):
    s.set(x, y, z, block, {"type": kind, "waterlogged": "false"})


def fence(s, x, y, z, block="guhs:vadshout_hek"):
    s.set(x, y, z, block, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})


def stairs(s, x, y, z, facing, block="guhs:vadshout_trap", half="bottom"):
    s.set(x, y, z, block, {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"})


def ground(s, rng):
    for x in range(W):
        for z in range(D):
            for y in range(G):
                s.set(x, y, z, "minecraft:dirt" if y < G - 1 else "minecraft:pink_wool")
            s.set(x, G, z, "minecraft:pink_concrete_powder" if rng.random() < 0.08 else "minecraft:pink_wool")


def paths(s):
    # from the south edge to the entrance, and a ring round the hall
    for z in range(Z1, D):
        for x in range(20, 25):
            s.set(x, G, z, KLINK)
    for x in range(X0 - 3, X1 + 4):
        for z in (Z0 - 3, Z0 - 2, Z1 + 2, Z1 + 3):
            s.set(x, G, z, KLINK)
    for z in range(Z0 - 3, Z1 + 4):
        for x in (X0 - 3, X0 - 2, X1 + 2, X1 + 3):
            s.set(x, G, z, KLINK)


def hall(s):
    # floor
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            s.set(x, G, z, PLANKS)
    # walls: a knuffelsteen plinth, vadshout planks, log posts every 7 blocks and at the corners
    for y in range(G + 1, TOP + 1):
        for x in range(X0, X1 + 1):
            for z in (Z0, Z1):
                s.set(x, y, z, KS if y == G + 1 else PLANKS)
        for z in range(Z0, Z1 + 1):
            for x in (X0, X1):
                post = (z - Z0) % 7 == 0 or z == Z1
                s.set(x, y, z, KS if y == G + 1 else (LOG if post else PLANKS), {"axis": "y"} if post and y > G + 1 else None)
    for x in (X0, X1):
        for z in (Z0, Z1):
            for y in range(G + 1, TOP + 1):
                s.set(x, y, z, LOG, {"axis": "y"})
    # a band of dark oak under the eaves
    for x in range(X0, X1 + 1):
        for z in (Z0, Z1):
            s.set(x, TOP, z, DARK)
    for z in range(Z0, Z1 + 1):
        for x in (X0, X1):
            s.set(x, TOP, z, DARK)
    # the entrance: an arch 3 wide, 4 high, with a dark oak frame
    for x in range(21, 24):
        for y in range(G + 1, G + 5):
            s.set(x, y, Z1, AIR)
    for y in range(G + 1, G + 6):
        s.set(20, y, Z1, LOG, {"axis": "y"})
        s.set(24, y, Z1, LOG, {"axis": "y"})
    for x in range(20, 25):
        s.set(x, G + 5, Z1, LOG, {"axis": "x"})
    stairs(s, 21, G + 4, Z1, "east", DARK.replace("planks", "stairs"), "top")
    stairs(s, 23, G + 4, Z1, "west", DARK.replace("planks", "stairs"), "top")
    # little roofs over the entrance (dark oak slabs on brackets)
    for x in range(19, 26):
        slab(s, x, G + 6, Z1 + 1, "bottom")
    for x in (19, 25):
        s.set(x, G + 5, Z1 + 1, "minecraft:dark_oak_fence", {"north": "true", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
    s.set(22, G + 5, Z1 + 1, "guhs:lampion_roze", {"hanging": "true"})


def puck_window(s, x_wall, zc, yc, outward):
    """A round window like a sjoelschijf: glass inside, a ring of dark wood, sticking out one block."""
    for dz in range(-3, 4):
        for dy in range(-3, 4):
            d = math.hypot(dz, dy)
            z, y = zc + dz, yc + dy
            if d <= 1.6:
                s.set(x_wall, y, z, "minecraft:light_blue_stained_glass")
            elif d <= 2.6:
                s.set(x_wall, y, z, "minecraft:stripped_dark_oak_log", {"axis": "x"})
                s.set(x_wall + outward, y, z, "minecraft:stripped_spruce_log", {"axis": "x"})


def gable(s, z, outward):
    """A Dutch stepped gable (trapgevel) at z, rising above the roof, with a big guh face in it."""
    steps = [(ROOF + k, X0 + k // 2, X1 - k // 2) for k in range(11)]
    for y, a, b in steps:
        for x in range(a, b + 1):
            s.set(x, y, z, PLANKS)
        for x in (a, b):
            s.set(x, y, z, LOG, {"axis": "y"})
    # the steps get little dark slabs on top, and the very top a lantern post
    for (y, a, b), nxt in zip(steps, steps[1:] + [(ROOF + 11, 0, -1)]):
        for x in range(a, b + 1):
            if not (nxt[1] <= x <= nxt[2]) and s.get(x, y + 1, z) in (None, AIR):
                slab(s, x, y + 1, z)
    for x, lamp in ((X0 + 5, "guhs:lampion_geel"), (X1 - 5, "guhs:lampion_roze")):
        s.set(x, ROOF + 11, z, LOG, {"axis": "y"})
        s.set(x, ROOF + 12, z, lamp, {"hanging": "false"})
    facing = "south" if outward > 0 else "north"
    return hulp.wall_face(s, 22, ROOF + 4, z, 5.0, facing)


def roof(s, rng):
    """The giant sjoelbak on the roof."""
    # the base (the ceiling of the hall) with eaves
    for x in range(X0 - 1, X1 + 2):
        for z in range(Z0 - 1, Z1 + 2):
            if X0 <= x <= X1 and Z0 <= z <= Z1:
                s.set(x, ROOF, z, DARK)
            else:
                slab(s, x, ROOF, z, "top")
    # the bak's surface and its rims (the gables are its ends)
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1):
            s.set(x, ROOF + 1, z, BAKPLANK)
        for x in (X0, X1):
            s.set(x, ROOF + 1, z, DARK)
            slab(s, x, ROOF + 2, z)
    # the gate bar: openings 3 wide at x 14-16, 18-20, 22-24, 26-28 (2 high); the numbers above them
    bar_z = Z0 + 7
    values = {14: "2", 18: "3", 22: "4", 26: "1"}
    for x in range(X0 + 1, X1):
        for y in range(ROOF + 2, ROOF + 9):
            s.set(x, y, bar_z, DARK)
    for x0, digit in values.items():
        for x in range(x0, x0 + 3):
            for y in (ROOF + 2, ROOF + 3):
                s.set(x, y, bar_z, AIR)
        for row, line in enumerate(DIGITS[digit]):
            for i, ch in enumerate(line):
                if ch == "1":
                    # the numbers read left-to-right from the south (the head of the giant bak)
                    s.set(x0 + i, ROOF + 8 - row, bar_z, "minecraft:white_wool")
    for x in range(X0 + 1, X1):
        slab(s, x, ROOF + 9, bar_z)
    # the lanes behind the bar (dividers up to the back gable)
    for x in (13, 17, 21, 25, 29, 30):
        for z in range(Z0 + 1, bar_z):
            s.set(x, ROOF + 2, z, DARK)
    # giant sjoelschijven: two sliding down the bak, one slipping into gate 4, one lying in lane 2
    for (cx, cz, r) in ((17.0, 30.0, 1.6), (25.0, 22.0, 1.6), (23.0, Z0 + 4.0, 1.0), (15.0, Z0 + 3.0, 1.0)):
        giant_puck(s, cx, ROOF + 2, cz, r)


def giant_puck(s, cx, y, cz, r):
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for z in range(int(cz - r - 1), int(cz + r + 2)):
            d = math.hypot(x - cx, z - cz)
            if d <= r + 0.5:
                edge = d > r - 0.6
                s.set(x, y, z, "minecraft:stripped_spruce_log" if edge else "minecraft:stripped_birch_log", {"axis": "y"})
    s.set(int(round(cx)), y, int(round(cz)), "minecraft:pink_concrete")


def bak(s):
    """The real sjoelbak: head, planks, gate bar, lanes, rims and the back board (facing north)."""
    for d in range(5):
        x = BAK_X + d
        s.set(x, G + 1, KOP_Z, "guhs:sjoelen_kop", {"facing": "north", "deel": str(d)})
        for z in range(POORT_Z + 1, KOP_Z):
            s.set(x, G + 1, z, "guhs:sjoelen_bakplank")
        s.set(x, G + 1, POORT_Z, "guhs:sjoelen_poort", {"facing": "north", "deel": str(d)})
        for z in (POORT_Z - 1, POORT_Z - 2):
            s.set(x, G + 1, z, "guhs:sjoelen_vak", {"facing": "north", "deel": str(d)})
    # rims (dark oak, a half slab higher than the surface) and the tall back board with a guh face
    for z in range(BACK_Z, KOP_Z + 1):
        for x in (BAK_X - 1, BAK_X + 5):
            s.set(x, G + 1, z, DARK)
            slab(s, x, G + 2, z)
    for x in range(BAK_X - 1, BAK_X + 6):
        s.set(x, G + 1, BACK_Z, DARK)
        s.set(x, G + 2, BACK_Z, DARK)
        s.set(x, G + 3, BACK_Z, DARK)
        slab(s, x, G + 4, BACK_Z)
    s.set(BAK_X + 2, G + 3, BACK_Z, "guhs:vadshout_gezicht", {"facing": "south", "stemming": "0"})
    # the corners of the head get little posts with lampions
    for x in (BAK_X - 1, BAK_X + 5):
        s.set(x, G + 2, KOP_Z, "minecraft:dark_oak_fence", {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
        s.set(x, G + 3, KOP_Z, "guhs:lampion_geel", {"hanging": "false"})
    # a carpet where you stand
    for x in range(BAK_X - 1, BAK_X + 6):
        for z in range(KOP_Z + 1, KOP_Z + 5):
            s.set(x, G + 1, z, "minecraft:pink_carpet" if (x + z) % 2 else "minecraft:white_carpet")


def inside(s, rng):
    # Opoe's knitting corner (west of the head)
    s.set(15, G + 1, 41, "guhs:theetafel", {"gedekt": "true"})
    s.set(14, G + 1, 41, "guhs:guh_stoel", {"facing": "east"})
    s.set(15, G + 1, 43, "guhs:guh_stoel", {"facing": "north"})
    for (x, z, c) in ((13, 44, "white"), (13, 45, "pink"), (14, 45, "yellow"), (13, 43, "light_blue")):
        s.set(x, G + 1, z, f"minecraft:{c}_wool")
    s.set(13, G + 2, 45, "minecraft:magenta_wool")
    s.set(14, G + 1, 44, "guhs:sjoelen_stapel", {"facing": "east"})
    s.set(16, G + 1, 38, "guhs:sjoelen_stapel", {"facing": "east"})
    s.set(17, G + 1, 38, "guhs:sjoelen_stapel", {"facing": "south"})
    for x in range(14, 19):
        for z in range(39, 43):
            if s.get(x, G + 1, z) in (None, AIR) and (x, z) != (int(NPC[0]), int(NPC[2])):
                s.set(x, G + 1, z, "minecraft:purple_carpet" if (x + z) % 3 == 0 else "minecraft:magenta_carpet")
    s.set(int(NPC[0]), G + 1, int(NPC[2]), "minecraft:purple_carpet")
    # the east corner at the head: a table with stacks of pucks and a guh_kast
    s.set(28, G + 1, 41, "guhs:guh_tafel", {"facing": "west"})
    s.set(29, G + 1, 41, "guhs:guh_stoel", {"facing": "west"})
    s.set(30, G + 1, 44, "guhs:guh_kast", {"facing": "west", "open": "false"})
    s.set(30, G + 1, 40, "guhs:sjoelen_stapel", {"facing": "west"})
    s.set(30, G + 1, 39, "guhs:sjoelen_stapel", {"facing": "west"})
    # benches with plushies watching, along both sides of the bak
    k = 0
    for z in range(20, 37, 4):
        for x, facing, bank_facing in ((16, "east", "east"), (28, "west", "west")):
            s.set(x, G + 1, z, "guhs:guh_bank", {"facing": bank_facing})
            s.set(x, G + 1, z + 1, "guhs:guh_bank", {"facing": bank_facing})
            s.set(x, G + 2, z, f"guhs:knuffel_{KNUFFELS[k % len(KNUFFELS)]}", {"facing": facing})
            k += 1
    # behind the back board: shelves (bookshelves), pots with flowers, stacks of pucks, a big seizoensbloembak
    for x in range(14, 30):
        if x in (21, 22, 23):
            continue
        s.set(x, G + 1, Z0 + 1, "minecraft:bookshelf")
        s.set(x, G + 2, Z0 + 1, "minecraft:bookshelf" if x % 3 else "guhs:sjoelen_stapel", {"facing": "south"} if x % 3 == 0 else None)
    for x in (14, 29):
        s.set(x, G + 1, Z0 + 3, "guhs:seizoensbloembak", {"seizoen": "lente"})
    s.set(22, G + 1, Z0 + 1, "guhs:knuffel_golden", {"facing": "south"})
    # lampions under the ceiling, in two rows
    for z in range(Z0 + 3, Z1 - 1, 5):
        for x in (15, 28):
            s.set(x, TOP, z, "guhs:lampion_roze" if (z // 5) % 2 else "guhs:lampion_geel", {"hanging": "true"})
        s.set(22, TOP, z, "guhs:lampion_mint", {"hanging": "true"})
    # flower pots on the window sills inside
    for z in (11, 18, 25, 32):
        s.set(X0 + 1, G + 1, z, "guhs:seizoensbloembak", {"seizoen": "zomer"})
        s.set(X1 - 1, G + 1, z, "guhs:seizoensbloembak", {"seizoen": "zomer"})


def garden(s, rng):
    # flower beds either side of the path
    for x in list(range(3, 18)) + list(range(27, 41)):
        for z in range(Z1 + 5, D - 2):
            if s.get(x, G, z) not in (KLINK,) and rng.random() < 0.3:
                s.set(x, G + 1, z, FLOWERS[rng.randrange(len(FLOWERS))])
    for x in range(1, W - 1):
        for z in range(1, Z0 - 4):
            if rng.random() < 0.25:
                s.set(x, G + 1, z, FLOWERS[rng.randrange(len(FLOWERS))])
    for z in range(Z0 - 4, Z1 + 5):
        for x in list(range(1, X0 - 4)) + list(range(X1 + 5, W - 1)):
            if rng.random() < 0.22:
                s.set(x, G + 1, z, FLOWERS[rng.randrange(len(FLOWERS))])
    # benches and lanterns along the path to the entrance
    for z in (Z1 + 5, Z1 + 9):
        s.set(18, G + 1, z, "guhs:guh_bank", {"facing": "east"})
        s.set(26, G + 1, z, "guhs:guh_bank", {"facing": "west"})
    for z in (Z1 + 3, Z1 + 7, D - 2):
        for x in (19, 25):
            fence(s, x, G + 1, z)
            fence(s, x, G + 2, z)
            s.set(x, G + 3, z, "guhs:lampion_geel", {"hanging": "false"})
    # the giant stack of sjoelschijven with a guh face on top (east of the path)
    cx, cz = 34.0, 52.0
    for level in range(4):
        r = 1.6 if level % 2 == 0 else 1.4
        for x in range(int(cx - 3), int(cx + 4)):
            for z in range(int(cz - 3), int(cz + 4)):
                d = math.hypot(x - cx, z - cz)
                if d <= r + 0.5:
                    s.set(x, G + 1 + level, z, "minecraft:stripped_spruce_log" if d > r - 0.6 else "minecraft:stripped_birch_log", {"axis": "y"})
    s.set(int(cx), G + 5, int(cz), "guhs:knuffelsteen_gezicht", {"facing": "south", "stemming": "0"})
    s.set(int(cx), G + 6, int(cz), "guhs:lampion_roze", {"hanging": "false"})
    # and a little one west of the path, with a pink puck on top
    cx, cz = 10.0, 52.0
    for level in range(2):
        for x in range(8, 13):
            for z in range(50, 55):
                d = math.hypot(x - cx, z - cz)
                if d <= 1.7:
                    s.set(x, G + 1 + level, z, "minecraft:stripped_spruce_log" if d > 1.0 else "minecraft:stripped_birch_log", {"axis": "y"})
    for x in range(9, 12):
        for z in range(51, 54):
            if math.hypot(x - cx, z - cz) <= 1.2:
                s.set(x, G + 3, z, "minecraft:pink_wool")
    s.set(10, G + 4, 52, "guhs:sjoelen_stapel", {"facing": "south"})
    # a low hedge along the east and west sides of the garden
    for z in range(Z0 - 4, D - 1, 1):
        for x in (0, W - 1):
            s.set(x, G + 1, z, "minecraft:flowering_azalea_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})


def build(h):
    s = h.Structure((W, H, D))
    rng = random.Random(29101)
    ground(s, rng)
    paths(s)
    hall(s)
    for z in (11, 18, 25, 32, 39):
        puck_window(s, X0, z, G + 5, -1)
        puck_window(s, X1, z, G + 5, 1)
    gable(s, Z1, 1)
    gable(s, Z0, -1)
    roof(s, rng)
    bak(s)
    inside(s, rng)
    garden(s, rng)
    # the centre jigsaw, under the middle of the bak
    s.set(*ANCHOR_POS, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": PLANKS,
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    s.entity(NPC[0], float(NPC[1]), NPC[2], {"id": "guhs:guh_npc", "Kind": "sjoelguh", "PersistenceRequired": h.ms.Byte(1),
                                              "Rotation": h.ms.floats(-90.0, 0.0)})
    return s, {"gezichten": 4}


def check(s):
    problems = []
    floating = hulp.check_floating(s, G)
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. {floating[:6]}")
    reach = hulp.walk(s, [ENTRANCE], extra_passable=("minecraft:jigsaw",))
    if not hulp.near_reachable(reach, int(NPC[0]), int(NPC[1]), int(NPC[2]), r=1):
        problems.append("Opoe can't be reached from the entrance")
    if STAND not in reach:
        problems.append(f"the spot at the head of the bak {STAND} can't be reached")
    x, y, z = int(NPC[0]), int(NPC[1]), int(NPC[2])
    if hulp.passable(s.get(x, y - 1, z)) or not hulp.passable(s.get(x, y, z)) or not hulp.passable(s.get(x, y + 1, z)):
        problems.append("Opoe doesn't stand on a floor with room above her")
    # the bak: 5 head blocks (deel 0..4), 19 rows of planks, the gate bar and two rows of lanes
    for d in range(5):
        x = BAK_X + d
        if s.blocks.get((x, G + 1, KOP_Z), (None, {}, None))[1].get("deel") != str(d):
            problems.append(f"head block {d} missing")
        for zz in range(POORT_Z + 1, KOP_Z):
            if s.get(x, G + 1, zz) != "guhs:sjoelen_bakplank":
                problems.append(f"bak plank missing at {(x, zz)}")
        if s.get(x, G + 1, POORT_Z) != "guhs:sjoelen_poort" or s.get(x, G + 1, POORT_Z - 1) != "guhs:sjoelen_vak":
            problems.append(f"gate bar / lane missing at x {x}")
        for zz in range(BACK_Z + 1, KOP_Z + 1):
            if not hulp.passable(s.get(x, G + 2, zz)):
                problems.append(f"something lies on the bak at {(x, G + 2, zz)}")
    if s.get(*ANCHOR_POS) != "minecraft:jigsaw":
        problems.append("no centre jigsaw")
    # whole doors: none used; nothing but air in the entrance arch
    for xx in range(21, 24):
        for yy in range(G + 1, G + 4):
            if not hulp.passable(s.get(xx, yy, Z1)):
                problems.append(f"the entrance is blocked at {(xx, yy)}")
    if problems:
        raise SystemExit("sjoelhuisje self-check failed:\n  " + "\n  ".join(problems))
    return len(reach)


if __name__ == "__main__":
    import types
    import make_structures as ms
    h = types.SimpleNamespace(Structure=ms.Structure, ms=ms)
    st, info = build(h)
    print("walkable:", check(st), info)
