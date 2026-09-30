"""
3.0 (Guhverhalen), slice hemel: the template of Het Hemelkapelletje (DESIGN_30 §4), made by hemel.build(h).

A small, rare cloud chapel on a floating islet high above a little cloud plaza:

  - the plaza on the ground: a round cloud-white plaza with a big pink heart, a golden ring, lampions, benches and two
    wolkenliften (up and down; the eilanden blocks guhs:wolkenlift / guhs:wolkenstroom)
  - the islet (~40 blocks up): a lawn of knuffelgras with guh flowers and guhbloesem trees, a hedge of cloud puffs along
    the rim, a skirt of pink and white clouds underneath and guh crystals hanging from its rocky bottom; a terrace with a
    golden hoop gate where the up lift puffs you off, and a walkway into the down lift
  - the chapel: white quartz with golden trims, quartz pilasters, tall pink windows, a heart window behind the altar and a
    striped pink-and-white cloud dome with a golden ring and a golden heart on its spire; inside a pink carpet aisle,
    benches, hanging lampions, and on the quartz altar the Knuffelhart (guhs:knuffelhart: a glowing pink heart under a
    glass dome) with the wolkenhoeder (NPC WOLKENHOEDER) next to it
  - loose pink and white clouds drifting around the islet, a little hemelkist in the garden

check(s) is the geometry self-check (SystemExit on problems): walk floors, everything that matters reachable on foot from
the up lift's landing, exactly one Knuffelhart with air above it, the wolkenhoeder on solid ground, the lift columns whole
and free, the jigsaw, the height. Keep the layout constants in sync with feature/hemel/HemelProtection.java (SKY_FROM,
PLAZA_RADIUS) and HemelGameTests.
"""
import math
import random
from collections import deque

NAME = "hemelkapelletje"
W, H, D = 41, 70, 43
G = 4                         # the plaza floor (template y); below it the foundation (sunk into the land)
C = 20                        # x of the middle line
PLAZA = (20, 31)              # plaza centre (x, z) = the anchor jigsaw
PLAZA_R = 10.5
ISLET = (20, 18)              # islet centre (x, z)
ISLET_R = 15
S = G + 40                    # islet top block y
F = S + 1                     # chapel floor block y
SKY_FROM = G + 20             # everything from here up belongs to the sky part (HemelProtection.SKY_FROM)
UP_COL = [(x, z) for x in (14, 15, 16) for z in (36, 37, 38)]
DOWN_COL = [(x, z) for x in (24, 25, 26) for z in (36, 37, 38)]
TOP = S + 2                   # the highest stream block of both lifts
HART = (20, F + 2, 9)         # the Knuffelhart on the altar
HOEDER = (23, F + 1, 11)      # where the wolkenhoeder stands
KIST = (29, S + 1, 25)
ANCHOR = f"guhs:{NAME}_midden"

LIFT, STREAM = "guhs:wolkenlift", "guhs:wolkenstroom"
PLANTS = {"guhs:roze_gras", "guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"}
LAMPS = {"guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint"}
PASSABLE = {None, "minecraft:air", "minecraft:pink_carpet", "minecraft:white_carpet", STREAM} | PLANTS | LAMPS
NOT_FLOOR = {None, "minecraft:air", STREAM, "minecraft:pink_carpet", "minecraft:white_carpet"} | PLANTS | LAMPS | {
    "minecraft:cherry_fence"}


def mc(name):
    return "minecraft:" + name


def _post(s, x, y, z):
    s.set(x, y, z, mc("cherry_fence"), {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})


def _fence(s, x, y, z, x_axis):
    s.set(x, y, z, mc("cherry_fence"), {"north": str(not x_axis).lower(), "south": str(not x_axis).lower(),
                                        "east": str(x_axis).lower(), "west": str(x_axis).lower(), "waterlogged": "false"})


def _lamp(s, x, y, z, colour="roze", hanging=False):
    s.set(x, y, z, f"guhs:lampion_{colour}", {"hanging": str(hanging).lower(), "waterlogged": "false"})


def _lamppost(s, x, y, z, colour="roze"):
    """A cherry post with a lampion on it (y = the ground block)."""
    _post(s, x, y + 1, z)
    _post(s, x, y + 2, z)
    _lamp(s, x, y + 3, z, colour)


def _cloud(s, rng, cx, cy, cz, r, colours=("white_wool", "white_wool", "pink_wool"), flat=1.6, only_empty=True, avoid=None):
    """A puffy cloud: a few flattened balls of wool around (cx, cy, cz)."""
    puffs = [(cx, cy, cz, r)] + [(cx + rng.uniform(-r, r), cy + rng.uniform(-0.4, 0.6) * r / 2, cz + rng.uniform(-r, r),
                                  r * rng.uniform(0.55, 0.8)) for _ in range(3)]
    for (px, py, pz, pr) in puffs:
        colour = mc(rng.choice(colours))
        for x in range(int(px - pr - 1), int(px + pr + 2)):
            for y in range(int(py - pr - 1), int(py + pr + 2)):
                for z in range(int(pz - pr - 1), int(pz + pr + 2)):
                    if math.dist((x, (y - py) * flat + py, z), (px, py, pz)) <= pr and (not only_empty or s.get(x, y, z) is None):
                        if avoid and avoid(x, y, z):
                            continue
                        s.set(x, y, z, colour)


def _heart_shape():
    """A pixel heart (9 wide, 8 high), rows from the top: the set of (dx, dy) with dy 0 = the tip, dx -4..4."""
    rows = [".XX...XX.", "XXXX.XXXX", "XXXXXXXXX", "XXXXXXXXX", ".XXXXXXX.", "..XXXXX..", "...XXX...", "....X...."]
    out = set()
    for i, row in enumerate(rows):
        for j, ch in enumerate(row):
            if ch == "X":
                out.add((j - 4, len(rows) - 1 - i))
    return out


def _in_lift_space(x, y, z):
    """The lifts' columns and a margin around them, and the terrace landing: never put clouds there."""
    return (13 <= x <= 27 and 34 <= z <= 40) or (12 <= x <= 28 and 28 <= z <= 36 and y >= S - 3)


# =====================================================================================================================
# the template
# =====================================================================================================================
def template(h):
    s = h.Structure((W, H, D))
    rng = random.Random(2030_1001)
    px, pz = PLAZA

    # --- the plaza: cloud white with a pink heart, a golden ring, lampions, benches --------------------------------
    heart = _heart_shape()
    plaza = []
    for x in range(W):
        for z in range(D):
            d = math.dist((x, z), (px, pz))
            if d > PLAZA_R:
                continue
            plaza.append((x, z))
            for y in range(G):
                s.set(x, y, z, mc("white_concrete") if y == G - 1 else mc("pink_wool"))
            # the big heart in the middle (2 blocks per heart pixel), pink with a magenta outline
            cell = (x - px, pz + 3 - z)
            if cell in heart:
                edge = any((cell[0] + a, cell[1] + b) not in heart for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                s.set(x, G, z, mc("magenta_concrete") if edge else mc("pink_concrete"))
            elif d > PLAZA_R - 1.1:
                s.set(x, G, z, mc("gold_block") if (x + z) % 3 == 0 else mc("yellow_concrete"))
            elif d > PLAZA_R - 2.2:
                s.set(x, G, z, mc("pink_concrete") if (x * 3 + z) % 4 else mc("white_concrete"))
            else:
                s.set(x, G, z, mc("white_concrete") if (x + z) % 2 else "minecraft:smooth_quartz")
    for (x, z) in UP_COL:
        s.set(x, G, z, LIFT, {"facing": "north", "down": "false"})
        for y in range(G + 1, TOP + 1):
            s.set(x, y, z, STREAM, {"facing": "north", "down": "false"})
    for (x, z) in DOWN_COL:
        s.set(x, G, z, LIFT, {"facing": "south", "down": "true"})
        for y in range(G + 1, TOP + 1):
            s.set(x, y, z, STREAM, {"facing": "south", "down": "true"})
    for k in range(10):
        a = k / 10 * 2 * math.pi + 0.3
        x, z = round(px + math.cos(a) * (PLAZA_R - 0.4)), round(pz + math.sin(a) * (PLAZA_R - 0.4))
        if not any(abs(x - cx) <= 1 and abs(z - cz) <= 1 for (cx, cz) in UP_COL + DOWN_COL):
            _lamppost(s, x, G, z, ("roze", "geel")[k % 2])
    for (x, z, f) in ((px - 6, pz - 3, "east"), (px + 6, pz - 3, "west"), (px - 6, pz + 1, "east"), (px + 6, pz + 1, "west")):
        s.set(x, G + 1, z, "guhs:guh_bank", {"facing": f})
    # two cloud puff pillars with a golden ball on each side of the lifts (a little welcome gate)
    for x in (12, 28):
        for y in range(G + 1, G + 4):
            s.set(x, y, 37, mc("white_wool"))
        s.set(x, G + 4, 37, mc("gold_block"))
        s.set(x - 1, G + 3, 37, mc("pink_wool"))
        s.set(x + 1, G + 3, 37, mc("pink_wool"))
    for (x, z) in ((px - 3, pz - 7), (px + 3, pz - 7), (px - 8, pz), (px + 8, pz)):
        s.set(x, G + 1, z, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:guhoortjes"]))
    s.set(px, G, pz, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": "minecraft:white_concrete", "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.clear_above(plaza, G + 1, top=SKY_FROM - 2)

    # --- the islet --------------------------------------------------------------------------------------------------
    ix, iz = ISLET
    bumps = [rng.uniform(-0.8, 1.2) for _ in range(14)]

    def rim(a):
        t = (a / (2 * math.pi)) * 14
        i = int(t) % 14
        f = t - int(t)
        return ISLET_R + bumps[i] * (1 - f) + bumps[(i + 1) % 14] * f

    cells = {}
    for x in range(ix - ISLET_R - 3, ix + ISLET_R + 4):
        for z in range(iz - ISLET_R - 3, iz + ISLET_R + 4):
            d = math.dist((x, z), (ix, iz))
            rr = rim(math.atan2(z - iz, x - ix) % (2 * math.pi))
            if d <= rr and 0 <= x < W and 0 <= z < D and (x, z) not in UP_COL and (x, z) not in DOWN_COL:
                cells[(x, z)] = d / rr
    for (x, z), f in cells.items():
        depth = int(round((1 - f) ** 0.75 * ISLET_R * 0.95 + 2 + rng.random() * 1.5))
        s.set(x, S, z, "guhs:knuffelgras")
        for k in range(1, depth + 1):
            y = S - k
            if k <= 1:
                block = mc("pink_terracotta")
            elif k == depth and f < 0.45 and rng.random() < 0.35:
                block = "guhs:guh_kristal_blok"
            else:
                block = rng.choice([mc("calcite"), mc("calcite"), mc("pink_terracotta"), mc("white_terracotta"), "guhs:kaasknabbel_stone"])
            s.set(x, y, z, block)
    # crystals hanging from the bottom
    for _ in range(9):
        x, z = rng.randint(ix - 7, ix + 7), rng.randint(iz - 7, iz + 7)
        if (x, z) not in cells:
            continue
        low = min(y for y in range(S, 0, -1) if s.get(x, y, z) is not None and s.get(x, y - 1, z) is None)
        for k in range(1, rng.randint(2, 4)):
            s.set(x, low - k, z, "guhs:guh_kristal_blok")
    # the skirt of pink and white clouds under the rim
    n = int(ISLET_R * 2.2)
    for i in range(n):
        a = i / n * 2 * math.pi + rng.uniform(-0.08, 0.08)
        cx, cz = ix + math.cos(a) * (ISLET_R - 0.5), iz + math.sin(a) * (ISLET_R - 0.5)
        _cloud(s, rng, cx, S - 2.2, cz, rng.uniform(1.6, 2.6), colours=("white_wool", "pink_wool", "pink_wool", "white_wool"),
               avoid=_in_lift_space)
    # the lawn's hedge of cloud puffs along the rim (open where the terrace is)
    for (x, z), f in cells.items():
        if f > 0.9 and not (12 <= x <= 28 and z >= 26):
            if all((x + a, z + b) in cells for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))) and f < 0.97:
                continue
            sector = int((math.degrees(math.atan2(z - iz, x - ix)) % 360) // 40)
            s.set(x, S + 1, z, mc("pink_wool") if sector % 3 == 1 else mc("white_wool"))

    # --- the terrace, the landing and the walkway into the down lift --------------------------------------------------
    for x in range(13, 28):
        for z in range(28, 36):
            if (x, z) in UP_COL or (x, z) in DOWN_COL:
                continue
            edge = x in (13, 27)
            s.set(x, S, z, mc("gold_block") if edge and z % 2 == 0 else "minecraft:smooth_quartz" if not edge else mc("white_concrete"))
            for y in range(S + 1, S + 4):
                s.blocks.pop((x, y, z), None)
            s.set(x, S - 1, z, mc("white_wool"))
    for z in range(28, 36):
        _fence(s, 13, S + 1, z, False)
        _fence(s, 27, S + 1, z, False)
    for x in range(13, 28):                                  # the south edge: open only where the two lifts are
        if not (14 <= x <= 16 or 24 <= x <= 26):
            _fence(s, x, S + 1, 35, True)
    for (x, z) in ((13, 35), (27, 35), (17, 35), (23, 35)):
        _post(s, x, S + 1, z)
        _lamp(s, x, S + 2, z, "roze")
    # the golden hoop gate over the path, with little clouds on top
    for i in range(15):
        a = math.pi * i / 14
        x = 20 + math.cos(a) * 3.5
        y = S + 1 + math.sin(a) * 5.2
        s.set(round(x), round(y), 29, mc("gold_block"))
    for x in range(16, 25):
        for y in range(S + 1, S + 8):
            if s.get(x, y, 29) == mc("gold_block") and s.get(x, y + 1, 29) is None and abs(x - 20) >= 2 and y > S + 3:
                s.set(x, y + 1, 29, mc("pink_wool"))
    s.set(20, S + 7, 29, mc("white_wool"))
    s.set(19, S + 7, 29, mc("white_wool"))
    s.set(21, S + 7, 29, mc("white_wool"))
    # the path from the terrace to the chapel door
    for z in range(23, 29):
        for x in (19, 20, 21):
            s.set(x, S, z, mc("pink_concrete") if (x + z) % 2 else mc("white_concrete"))
            s.blocks.pop((x, S + 1, z), None)
    _lamppost(s, 17, S, 26, "geel")
    _lamppost(s, 23, S, 26, "geel")

    # --- the chapel -------------------------------------------------------------------------------------------------
    x0, x1, z0, z1 = 13, 27, 7, 23                           # outer walls
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            s.set(x, S, z, "minecraft:smooth_quartz")         # the platform the chapel stands on
            for y in range(S + 1, S + 16):
                s.blocks.pop((x, y, z), None)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            aisle = x == C and z >= 11
            s.set(x, F, z, mc("pink_concrete") if aisle else ("minecraft:smooth_quartz" if (x + z) % 2 else mc("white_concrete")))
    # the step up to the door
    for x in (19, 20, 21):
        s.set(x, F, z1 + 1, mc("quartz_stairs"), {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    # walls: quartz with pilasters, a golden band on top
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x not in (x0, x1) and z not in (z0, z1):
                continue
            for y in range(F + 1, F + 7):
                pilaster = (x in (x0, x1) and (z - z0) % 4 == 0) or (z in (z0, z1) and (x - x0) % 4 == 0)
                if y == F + 6:
                    block = mc("gold_block") if pilaster else mc("chiseled_quartz_block")
                elif pilaster:
                    block = mc("quartz_pillar")
                else:
                    block = "minecraft:smooth_quartz"
                s.set(x, y, z, block, {"axis": "y"} if block == mc("quartz_pillar") else None)
    # tall pink windows on the sides, rounded at the top
    for z in (9, 13, 17, 21):
        for x in (x0, x1):
            for y in range(F + 2, F + 5):
                s.set(x, y, z, mc("pink_stained_glass"))
            s.set(x, F + 5, z, mc("magenta_stained_glass"))
    # the front: an arched door with a golden heart above it
    for x in (19, 20, 21):
        for y in range(F + 1, F + 4):
            s.set(x, y, z1, mc("air"))
    s.set(19, F + 4, z1, mc("quartz_stairs"), {"facing": "east", "half": "top", "shape": "straight", "waterlogged": "false"})
    s.set(21, F + 4, z1, mc("quartz_stairs"), {"facing": "west", "half": "top", "shape": "straight", "waterlogged": "false"})
    s.set(20, F + 4, z1, mc("air"))
    for (dx, dy) in ((-1, 2), (1, 2), (-1, 1), (0, 1), (1, 1), (0, 0)):
        s.set(20 + dx, F + 4 + dy, z1 + 1, mc("gold_block"))
    # the ceiling round the dome, then the dome: pink and white stripes like a cloud, a golden ring, a golden heart on top
    dcx, dcz, rx, rz, ry = C, 15, 6.6, 7.6, 7.0
    base = F + 7
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if ((x - dcx) / rx) ** 2 + ((z - dcz) / rz) ** 2 > 1:
                s.set(x, base, z, mc("pink_concrete") if (x in (x0, x1) or z in (z0, z1)) else mc("white_concrete"))
    top = base
    for x in range(int(dcx - rx - 1), int(dcx + rx + 2)):
        for z in range(int(dcz - rz - 1), int(dcz + rz + 2)):
            for y in range(base, base + int(ry) + 2):
                e = ((x - dcx) / rx) ** 2 + ((z - dcz) / rz) ** 2 + ((y - base) / ry) ** 2
                inner = ((x - dcx) / (rx - 1.1)) ** 2 + ((z - dcz) / (rz - 1.1)) ** 2 + ((y - base) / (ry - 1.1)) ** 2
                if e <= 1 and inner > 1:
                    band = (y - base) in (2, 5)
                    block = mc("gold_block") if y == base else mc("white_wool") if band or y >= base + ry - 0.5 else mc("pink_wool")
                    s.set(x, y, z, block)
                    top = max(top, y)
                elif e <= 1:
                    s.set(x, y, z, mc("air"))
    for y in range(top + 1, top + 3):
        s.set(dcx, y, dcz, mc("gold_block"))
    for (dx, dy) in _heart_shape():                          # a golden heart on the spire (every other pixel: 5 wide)
        if dx % 2 == 0 and dy % 2 == 0:
            s.set(dcx + dx // 2, top + 3 + dy // 2, dcz, mc("gold_block"))
    # the corners of the roof: little cloud puffs
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        _cloud(s, rng, x, base + 1.2, z, 1.4, colours=("white_wool", "pink_wool"), flat=1.3)
    # the heart window behind the altar (north wall)
    for (dx, dy) in heart:
        s.set(C + dx, F + 2 + dy, z0, mc("magenta_stained_glass") if dy in (0, 1) or abs(dx) == 4 else mc("pink_stained_glass"))
    # inside: the altar with the Knuffelhart, lampions, flowers, benches, hanging lampions
    ax, ay, az = HART
    for x in range(ax - 2, ax + 3):
        s.set(x, F + 1, az, mc("chiseled_quartz_block") if x == ax else mc("quartz_bricks"))
        s.set(x, F + 1, az - 1, mc("quartz_bricks"))
    s.set(ax - 3, F + 1, az, mc("quartz_stairs"), {"facing": "east", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    s.set(ax + 3, F + 1, az, mc("quartz_stairs"), {"facing": "west", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    s.set(ax, ay, az, "guhs:knuffelhart")
    s.set(ax, ay + 1, az, mc("air"))
    for x in (ax - 2, ax + 2):
        _lamp(s, x, F + 2, az, "roze")
    s.set(ax - 1, F + 2, az, mc("potted_pink_tulip"))
    s.set(ax + 1, F + 2, az, mc("potted_allium"))
    s.set(ax - 1, F + 2, az - 1, mc("potted_azure_bluet"))
    s.set(ax + 1, F + 2, az - 1, mc("potted_oxeye_daisy"))
    s.set(ax, F + 2, az - 1, mc("gold_block"))
    for x in range(x0 + 1, x1):                             # a soft pink carpet in front of the altar
        for z in (az + 1,):
            if x != C:
                s.set(x, F + 1, z, mc("white_carpet"))
    for z in range(az + 1, z1):
        s.set(C, F + 1, z, mc("pink_carpet"))
    for z in (14, 17, 20):
        for x in (16, 17, 18, 22, 23, 24):
            s.set(x, F + 1, z, "guhs:guh_bank", {"facing": "north"})
    for (x, z) in ((x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)):
        s.set(x, F + 1, z, mc("potted_pink_tulip") if (x + z) % 2 else mc("potted_allium"))
    for (x, z) in ((x0 + 1, 13), (x1 - 1, 13), (x0 + 1, 17), (x1 - 1, 17)):
        s.set(x, F + 1, z, "guhs:white_kussen" if x == x0 + 1 else "guhs:pink_kussen", {"facing": "east" if x == x0 + 1 else "west"})
    # hanging lampions from the dome (each on a little chain from the dome shell)
    for (x, z, col) in ((dcx, dcz, "roze"), (dcx - 3, dcz, "mint"), (dcx + 3, dcz, "mint"), (dcx, dcz - 4, "geel"), (dcx, dcz + 4, "geel")):
        y = base + 1
        while y <= top and s.get(x, y, z) in (None, mc("air")):
            y += 1
        if y > top or y - 3 <= F + 2:
            continue
        s.set(x, y - 1, z, mc("chain"), {"axis": "y", "waterlogged": "false"})
        _lamp(s, x, y - 2, z, col, hanging=True)
    # the wolkenhoeder stands next to the altar, looking at the door
    hx, hy, hz = HOEDER
    s.entity(hx + 0.5, float(hy), hz + 0.5, {"id": "guhs:guh_npc", "Kind": "wolkenhoeder", "PersistenceRequired": h.ms.Byte(1),
                                             "Rotation": h.ms.floats(0.0, 0.0)})

    # --- the garden round the chapel ----------------------------------------------------------------------------------
    for (x, z) in ((8, 11), (32, 11), (9, 25), (31, 22)):
        if (x, z) in cells and cells[(x, z)] < 0.8:
            h.blossom_tree(s, rng, x, S + 1, z)
    for (x, z) in ((10, 18), (30, 16)):
        _lamppost(s, x, S, z, "roze")
    h.ms.chest(s, *KIST, "west", f"guhs:chests/{NAME}")
    s.set(KIST[0], KIST[1], KIST[2] + 1, "guhs:pink_zitzak", {"facing": "west"})
    s.set(KIST[0] - 1, KIST[1], KIST[2] - 2, "guhs:guh_bank", {"facing": "south"})
    for (x, z), f in cells.items():
        if s.get(x, S, z) == "guhs:knuffelgras" and s.get(x, S + 1, z) is None and s.get(x, S + 2, z) is None:
            r = rng.random()
            if r < 0.07:
                s.set(x, S + 1, z, rng.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]))
            elif r < 0.26:
                s.set(x, S + 1, z, "guhs:roze_gras")

    # --- clouds drifting round the islet ----------------------------------------------------------------------------
    for (cx, cy, cz, r) in ((4, S + 6, 30, 2.4), (36, S + 3, 30, 2.2), (35, S + 9, 6, 2.8), (5, S + 11, 5, 2.4),
                            (37, S - 8, 18, 2.6), (3, S - 5, 17, 2.2), (20, S + 20, 3, 2.6), (30, S - 16, 34, 2.0)):
        _cloud(s, rng, cx, cy, cz, r, colours=("white_wool", "white_wool", "pink_wool"),
               avoid=_in_lift_space)

    # the lift columns stay free of everything (clouds, trees, hedge)
    for (x, z) in UP_COL + DOWN_COL:
        for y in range(TOP + 1, TOP + 4):
            s.blocks.pop((x, y, z), None)
    s.hart = HART
    return s


def solid(block):
    return block not in NOT_FLOOR and block not in PASSABLE


# =====================================================================================================================
# the geometry self-check
# =====================================================================================================================
def walk(s, start, jumps=True):
    """Every spot (feet position) you can reach on foot (1 block up or down, 3 down at most)."""
    get = s.get

    def free(x, y, z):
        return get(x, y, z) in PASSABLE and get(x, y + 1, z) in PASSABLE and 0 <= x < W and 0 <= z < D and y + 1 < H

    seen = {start}
    todo = deque([start])
    while todo:
        x, y, z = todo.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            for dy in (0, 1, -1, -2, -3):
                ny = y + dy
                if dy == 1 and not free(x, y + 2, z):
                    continue
                if free(nx, ny, nz) and solid(get(nx, ny - 1, nz)) and (nx, ny, nz) not in seen:
                    if dy < 0 and not all(get(nx, yy, nz) in PASSABLE for yy in range(ny, y + 2)):
                        continue
                    seen.add((nx, ny, nz))
                    todo.append((nx, ny, nz))
                    break
    return seen


def check(s):
    problems = []
    get = s.get
    # 1. exactly one Knuffelhart, on the altar, with air above it (its glass dome sticks out a little)
    harten = [p for p, (b, _, _) in s.blocks.items() if b == "guhs:knuffelhart"]
    if harten != [HART]:
        problems.append(f"Knuffelhart: {harten} (should be exactly {HART})")
    if get(HART[0], HART[1] + 1, HART[2]) != "minecraft:air":
        problems.append("no air above the Knuffelhart")
    if not solid(get(HART[0], HART[1] - 1, HART[2])):
        problems.append("the Knuffelhart has no altar under it")
    # 2. the wolkenhoeder, on a floor, with room
    hoeders = [e for e in s.entities if e[3].get("Kind") == "wolkenhoeder"]
    if len(hoeders) != 1:
        problems.append(f"{len(hoeders)} wolkenhoeders (should be 1)")
    for (ex, ey, ez, _) in hoeders:
        p = (int(ex), int(ey), int(ez))
        if not solid(get(p[0], p[1] - 1, p[2])) or get(*p) not in PASSABLE or get(p[0], p[1] + 1, p[2]) not in PASSABLE:
            problems.append(f"the wolkenhoeder at {p} doesn't stand free on a floor")
    # 3. the lifts: whole columns, pads, free on top
    for col, down in ((UP_COL, False), (DOWN_COL, True)):
        for (x, z) in col:
            if get(x, G, z) != LIFT:
                problems.append(f"lift pad missing at {(x, G, z)}")
            for y in range(G + 1, TOP + 1):
                if get(x, y, z) != STREAM or s.blocks[(x, y, z)][1].get("down") != str(down).lower():
                    problems.append(f"lift column broken at {(x, y, z)}")
                    break
            if get(x, TOP + 1, z) not in (None, "minecraft:air"):
                problems.append(f"something on top of the lift at {(x, TOP + 1, z)}")
    for x in (14, 15, 16):                                   # the landing right next to the up lift
        if not solid(get(x, S, 35)):
            problems.append(f"no terrace next to the up lift at {(x, S, 35)}")
    for x in (24, 25, 26):                                   # the walkway right next to the down lift
        if not solid(get(x, S, 35)) or get(x, S + 1, 35) not in PASSABLE:
            problems.append(f"no open walkway next to the down lift at {(x, S, 35)}")
    # 4. walking from the landing: the altar, the wolkenhoeder, the chest, the benches, the down lift
    reach = walk(s, (15, S + 1, 34))
    must = {"in front of the altar": (HART[0], F + 1, HART[2] + 2), "next to the wolkenhoeder": (HOEDER[0], F + 1, HOEDER[2] + 1),
            "at the hemelkist": (KIST[0] - 1, S + 1, KIST[2]), "the back of the chapel": (15, F + 1, 9),
            "the down lift walkway": (25, S + 1, 35), "the garden (west)": (9, S + 1, 18)}
    for what, p in must.items():
        if p not in reach:
            problems.append(f"can't walk to {what} {p}")
    ground = walk(s, (20, G + 1, 33))
    if not any(p in ground for p in ((15, G + 1, 37), (15, G + 1, 35))):
        problems.append("can't walk to the up lift on the plaza")
    # 5. the anchor and the size
    if get(PLAZA[0], G, PLAZA[1]) != "minecraft:jigsaw":
        problems.append("no anchor jigsaw")
    if max(y for (_, y, _) in s.blocks) >= H:
        problems.append("too high for the template")
    # 6. plants and lamps stand on something
    for (x, y, z), (b, props, _) in s.blocks.items():
        if b in PLANTS and not solid(get(x, y - 1, z)):
            problems.append(f"{b} floats at {(x, y, z)}")
        if b in LAMPS and props.get("hanging") == "false" and get(x, y - 1, z) in (None, "minecraft:air"):
            problems.append(f"{b} floats at {(x, y, z)}")
        if b in LAMPS and props.get("hanging") == "true" and get(x, y + 1, z) in (None, "minecraft:air"):
            problems.append(f"hanging {b} hangs from nothing at {(x, y, z)}")
    return problems


def build(h):
    """The template + its check; returns the Structure (saved by hemel.build)."""
    s = template(h)
    problems = check(s)
    if problems:
        raise SystemExit("hemelkapelletje geometry check failed:\n  " + "\n  ".join(problems[:40]))
    print("hemelkapelletje: geometry check ok")
    return s
