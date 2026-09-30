"""
De kaasmijn: a very rare cheese mine in the Guhmension, run by the Mijnguh.

On the surface a mine head shaped like a giant guh head wearing a yellow hard hat (you walk in through its mouth).
Inside: the Mijnguh (lends you a pickaxe, sells the miner outfit), a guh-face floor mosaic, a staircase, the
VAHOEG-sprong (a drop shaft into a water pool) and the bubble lift. Below it two levels:
  - de kaasgangen (L1): a ring tunnel with a powered rail loop (the Kaasexpress, with cart dispensers), three
    caverns with cheese veins and kaassaus pools, and the canteen of the guh miners
  - de diepe mijn (L2, deepslate): the Grote Kaasgrot with a guh-face kaassaus lake, the gold vein gallery and the
    treasure room with the kaaskluis in the mouth of a golden guh face.
Cheese veins (kaasader / diepe kaasader / gouden kaasader) drop kaasbrokken and goudkaas; mined, they become a
mined-out vein that slowly grows back (see KaasaderBlock.java), so the mine never runs dry.

build(h) makes everything (h = make_v2); check(...) is the geometry self-check of the template.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw

# --- the template ------------------------------------------------------------------------------------------------------
W, H, D = 96, 60, 96
G = 34                  # ground level in the template (placed on the surface; the mine is below it)
L1 = G - 14             # floor of the kaasgangen
L2 = G - 28             # floor of the diepe mijn
CX, CZ = 48, 50         # the guh head (dome) on the surface
RX, RY, RZ = 21, 19, 17
RING0, RING1 = 10, 85   # the rail loop of the Kaasexpress (L1): rails on this square
NPC = (57, G + 1, 55)   # the Mijnguh

CLOTHES = ["kaasmijn_helm", "kaasmijn_overall", "kaasmijn_zakdoek"]
BONES = {
    # a little headlamp on the front of the hard hat (outfit_: a guh without the helmet doesn't show it)
    "outfit_kaasmijn_lamp": ("head", [0, 6, -2], "kaasmijn_lamp", [([-1.5, 15.3, -11.9], [3, 2, 1.2], 0)]),
}

# blocks that don't fill their cell (you can stand in them / walk through them) and ones you can't jump onto
THIN = ("rail", "powered_rail", "lantern", "soul_lantern", "chain", "lampion", "sign", "carpet", "door", "torch",
        "button", "pressure_plate", "flower", "vlaggetjes", "guhbloem", "kaasbloem", "guhoortjes", "knabbelroos")
FENCY = ("fence", "wall", "pane", "iron_bars")
LIGHT = {"lantern": 15, "lampion_roze": 15, "lampion_geel": 15, "lampion_mint": 15, "ochre_froglight": 15, "pearlescent_froglight": 15,
         "shroomlight": 15, "glowstone": 15, "sea_lantern": 15, "redstone_block": 0, "guh_kristal_lamp": 15}


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


class Mine:
    """The template being built, plus the bookkeeping the decorations and the self-check need."""

    def __init__(self, h):
        self.h = h
        self.s = h.Structure((W, H, D))
        self.rng = random.Random(20240189)
        self.hollow = {}          # (x, y, z) -> zone: the dug-out air of the mine
        self.zones = {}           # zone -> floor y
        self.corridors = []       # (x0, x1, z0, z1, floor, height, zone)
        self.entities = []        # (x, y, z, kind) for the self-check
        self.water_ok = set()     # cells you may fall into from high up (the pool of the VAHOEG-sprong)

    # --- blocks ---
    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def solid_free(self, x, y, z):
        return (x, y, z) not in self.hollow and (x, y, z) not in self.s.blocks

    # --- digging ---
    def dig(self, x, y, z, zone):
        if 0 <= x < W and 0 <= y < H and 0 <= z < D:
            self.hollow[(x, y, z)] = zone
            self.s.set(x, y, z, "minecraft:air")

    def box(self, x0, x1, y0, y1, z0, z1, zone):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.dig(x, y, z, zone)

    def room(self, x0, x1, z0, z1, floor, height, zone):
        self.zones.setdefault(zone, floor)
        self.box(x0, x1, floor + 1, floor + height, z0, z1, zone)

    def corridor(self, x0, x1, z0, z1, floor, height, zone):
        self.zones.setdefault(zone, floor)
        self.box(x0, x1, floor + 1, floor + height, z0, z1, zone)
        self.corridors.append((x0, x1, z0, z1, floor, height, zone))

    def cave(self, cx, cz, floor, rx, rz, ry, zone, seed):
        """A cavern with a flat floor and a lumpy dome."""
        self.zones.setdefault(zone, floor)
        r = random.Random(seed)
        waves = [(r.uniform(0, 6.3), r.randint(2, 5), r.uniform(0.04, 0.1)) for _ in range(3)]
        for x in range(cx - rx - 2, cx + rx + 3):
            for z in range(cz - rz - 2, cz + rz + 3):
                a = math.atan2(z - cz, x - cx)
                k = 1 + sum(amp * math.sin(n * a + ph) for ph, n, amp in waves)
                d2 = ((x - cx) / (rx * k)) ** 2 + ((z - cz) / (rz * k)) ** 2
                if d2 > 1:
                    continue
                top = floor + max(3, int(round(ry * math.sqrt(max(0.0, 1 - d2)))))
                for y in range(floor + 1, top + 1):
                    self.dig(x, y, z, zone)


# =====================================================================================================================
# the mine
# =====================================================================================================================
def dig_mine(m):
    # --- the stair shaft: two lanes (x 38..40 and 42..44) from the hall floor down to L2 ---
    m.box(38, 44, L2 + 1, G - 1, 36, 46, "shaft")
    m.box(38, 40, G, G, 38, 44, "shaft")                     # the stairwell in the hall floor
    m.zones["shaft"] = L2
    # --- the VAHOEG-sprong: a 3x3 drop shaft from the hall into a pool at L2 ---
    m.box(50, 52, L2 + 1, G, 36, 38, "drop")
    m.box(50, 52, L2 - 2, L2, 36, 38, "drop")                # the pool (water)
    m.zones["drop"] = L2
    # --- the bubble lift: a water column from L2 up to the hall ---
    m.box(55, 55, L2 + 1, G, 37, 37, "lift")
    m.zones["lift"] = L2

    # --- L1: de kaasgangen ---
    m.room(36, 60, 48, 54, L1, 4, "l1hal")
    m.box(38, 44, L1 + 1, L1 + 3, 47, 47, "l1hal")            # from the stair landing into the hall
    for (x0, x1, z0, z1) in ((RING0, RING1, RING0, RING0 + 4), (RING0, RING1, RING1 - 4, RING1),
                             (RING0, RING0 + 4, RING0, RING1), (RING1 - 4, RING1, RING0, RING1)):
        m.corridor(x0, x1, z0, z1, L1, 4, "ring")
    m.cave(25, 52, L1, 8, 11, 6, "l1west", 11)                # Kaaskamer West (kaassaus pool)
    m.corridor(30, 36, 50, 52, L1, 3, "l1gang")
    m.corridor(14, 19, 50, 52, L1, 3, "l1gang")
    m.cave(71, 48, L1, 7, 10, 6, "l1oost", 12)                # Ertsgrot (lots of veins)
    m.corridor(60, 66, 50, 52, L1, 3, "l1gang")
    m.corridor(76, 81, 44, 46, L1, 3, "l1gang")
    m.cave(48, 68, L1, 12, 8, 7, "l1zuid", 13)                # Knabbelgrot
    m.corridor(46, 50, 54, 62, L1, 3, "l1gang")
    m.corridor(46, 50, 74, 81, L1, 3, "l1gang")
    m.room(20, 34, 18, 30, L1, 4, "kantine")                  # the canteen of the guh miners
    m.corridor(26, 28, 14, 18, L1, 3, "l1gang")
    m.corridor(25, 27, 30, 42, L1, 3, "l1gang")

    # --- L2: de diepe mijn ---
    m.room(46, 60, 34, 54, L2, 4, "l2hal")
    m.room(36, 60, 48, 54, L2, 4, "l2hal")
    m.box(38, 44, L2 + 1, L2 + 3, 47, 47, "l2hal")
    m.cave(48, 72, L2, 25, 14, 9, "l2grot", 21)               # De Grote Kaasgrot
    m.corridor(45, 51, 54, 60, L2, 4, "l2gang")
    m.room(5, 19, 63, 77, L2, 6, "schat")                     # the treasure room
    m.corridor(20, 26, 69, 71, L2, 3, "l2gang")
    m.cave(83, 69, L2, 7, 7, 5, "goud", 22)                   # the gold vein gallery
    m.corridor(71, 78, 68, 70, L2, 3, "l2gang")
    # (the stair landing / pool / lift get their own blocks later: they were dug with the shaft)


STONE_L1 = [("stone", 55), ("andesite", 15), ("cobblestone", 15), ("tuff", 8), ("stone", 7)]
STONE_L2 = [("deepslate", 60), ("cobbled_deepslate", 20), ("tuff", 14), ("polished_deepslate", 6)]


def pick(rng, table):
    total = sum(w for _, w in table)
    r = rng.uniform(0, total)
    for name, w in table:
        r -= w
        if r <= 0:
            return name
    return table[-1][0]


def shell(m):
    """Two blocks of rock around everything that was dug out (so natural caves or wool never show through)."""
    zone_of = {}
    for (x, y, z), zone in m.hollow.items():
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                for dz in range(-2, 3):
                    c = (x + dx, y + dy, z + dz)
                    if c[1] <= G - 1 and c not in m.hollow and c not in zone_of:
                        zone_of[c] = zone
    for c, zone in zone_of.items():
        if not (0 <= c[0] < W and 0 <= c[1] < H and 0 <= c[2] < D) or c in m.s.blocks:
            continue
        deep = c[1] < L1 - 3 or zone in ("l2hal", "l2grot", "l2gang", "schat", "goud")
        m.set(*c, pick(m.rng, STONE_L2 if deep else STONE_L1))
    m.shell_zone = zone_of


def pad(m):
    """The mine yard: rough stone and gravel, pink wool at the edge (like the Guhmension around it), soil below."""
    for x in range(W):
        for z in range(D):
            edge = min(x, z, W - 1 - x, D - 1 - z)
            if (x, G, z) not in m.hollow:
                if edge <= 2:
                    name = "pink_wool"
                else:
                    name = pick(m.rng, [("cobblestone", 30), ("andesite", 20), ("gravel", 15), ("stone", 25), ("coarse_dirt", 0), ("tuff", 10)])
                m.set(x, G, z, name)
            # (deep enough to reach the ground where the terrain is lower: the flatness check allows 12 blocks)
            for y in range(G - 13, G):
                if m.solid_free(x, y, z):
                    m.set(x, y, z, "dirt" if y >= G - 2 else "stone")


# --- the guh head with the hard hat -----------------------------------------------------------------------------------
def head_inside(x, y, z, grow=0.0):
    return ((x - CX) / (RX + grow)) ** 2 + ((y - (G + 0.5)) / (RY + grow)) ** 2 + ((z - CZ) / (RZ + grow)) ** 2 <= 1


def face_colour(u, v):
    """The face on the front of the head: u = x - CX (left/right), v = height above the ground."""
    if v >= 13:
        if abs(u) <= 1 and 15 <= v <= 17:
            return "ochre_froglight" if (u, v) == (0, 16) else "gold_block"   # the headlamp
        return None
    if (abs(u) - 7) ** 2 / 2.2 ** 2 + (v - 10) ** 2 / 2.4 ** 2 <= 1:    # eyes
        return "white_concrete" if (abs(u) - 7, v) in ((-1, 11), (-1, 10)) and u != 0 else "black_concrete"
    if (abs(u) - 12) ** 2 / 2.4 ** 2 + (v - 6) ** 2 / 1.6 ** 2 <= 1:    # cheeks
        return "magenta_concrete"
    if abs(u) <= 1 and 7 <= v <= 8:                                      # nose
        return "magenta_terracotta"
    if abs(u) <= 3 and v == 5 or abs(u) == 3 and 1 <= v <= 4:          # the mouth (the doorway is in it)
        return "black_concrete"
    if (u / 6.5) ** 2 + ((v - 4.5) / 4.2) ** 2 <= 1:                    # the white snout
        return "white_concrete"
    return None


def head(m):
    rng = m.rng
    inner = set()
    for x in range(CX - RX - 3, CX + RX + 4):
        for z in range(CZ - RZ - 3, CZ + RZ + 4):
            for y in range(G + 1, G + RY + 2):
                if head_inside(x, y, z):
                    inner.add((x, y, z))
    # the shell: every cell of the head that touches the outside, also diagonally (so the shell holds together)
    shellc = [c for c in inner if any((c[0] + dx, c[1] + dy, c[2] + dz) not in inner
                                      for dx in (-1, 0, 1) for dy in (-1, 0, 1) for dz in (-1, 0, 1) if c[1] + dy > G)]
    shellset = set(shellc)
    for c in inner - shellset:
        m.set(*c, "air")
    for (x, y, z) in shellc:
        u, v = x - CX, y - G
        front = z - CZ > 0.55 * RZ * math.sqrt(max(0.0, 1 - (u / RX) ** 2 - ((v - 0.5) / RY) ** 2))
        name = "pink_concrete" if (x * 3 + y * 5 + z) % 11 else "pink_wool"
        if v >= 13:
            name = "orange_concrete" if abs(u) <= 1 and not front else "yellow_concrete"
        elif front and face_colour(u, v):
            name = face_colour(u, v)
        elif abs(u) >= 16 and 5 <= v <= 8 and not front and (z - CZ) % 6 in (1, 2, 3):
            name = "pink_stained_glass"                          # round-ish windows along the sides
        if front and v >= 13 and face_colour(u, v):
            name = face_colour(u, v)
        m.set(x, y, z, name)
    # the brim of the hard hat
    for x in range(CX - RX - 3, CX + RX + 4):
        for z in range(CZ - RZ - 3, CZ + RZ + 4):
            if head_inside(x, G + 13, z, 2.2) and not head_inside(x, G + 13, z):
                m.set(x, G + 13, z, "yellow_concrete")
    # the ears poke out of the hat
    for side in (-1, 1):
        ex, ey = CX + side * 13, G + 17
        for x in range(ex - 5, ex + 6):
            for y in range(ey - 5, ey + 6):
                d = math.hypot(x - ex, (y - ey) * 1.1)
                if d <= 4.2:
                    for z in (CZ - 1, CZ, CZ + 1):
                        if (x, y, z) in inner and (x, y, z) not in shellset:
                            continue
                        inside = d <= 2.4 and z == CZ + 1
                        m.set(x, y, z, "pink_terracotta" if inside else "pink_concrete")
    # the mouth is the way in, with two buck teeth hanging in it
    for x in range(CX - 2, CX + 3):
        for y in range(G + 1, G + 5):
            for z in range(CZ + 8, CZ + RZ + 2):
                if m.get(x, y, z) and m.get(x, y, z) != "minecraft:air":
                    m.set(x, y, z, "air")
    for x in (CX - 1, CX + 1):
        for z in range(CZ + 12, CZ + RZ + 2):
            if (x, G + 4, z) in shellset:
                m.set(x, G + 4, z, "quartz_block")
    for z in range(CZ + 12, CZ + RZ + 2):
        if (CX, G + 4, z) in shellset:
            m.set(CX, G + 4, z, "black_concrete")
    m.head_inner = inner
    m.head_shell = shellset

    # --- the floor: polished andesite, a ring of stone bricks, the guh face mosaic ---
    for x in range(CX - RX, CX + RX + 1):
        for z in range(CZ - RZ, CZ + RZ + 1):
            if head_inside(x, G + 1, z) and (x, G, z) not in m.hollow:
                r = ((x - CX) / RX) ** 2 + ((z - CZ) / RZ) ** 2
                m.set(x, G, z, "stone_bricks" if r > 0.8 else "polished_andesite")
    mosaic(m, face_pattern(21, 13, hat=True), 38, 52, G, {
        "fur": "pink_concrete", "edge": "magenta_concrete", "ear": "pink_terracotta", "eye": "black_concrete", "shine": "white_concrete",
        "blush": "magenta_terracotta", "muzzle": "white_concrete", "nose": "magenta_concrete", "mouth": "black_concrete",
        "hat": "yellow_concrete", "hatedge": "orange_concrete", "lamp": "ochre_froglight"}, plane="floor")

    # --- lamps on chains from the top of the head ---
    for (x, z) in ((40, 46), (56, 46), (40, 58), (56, 58), (48, 50), (36, 50), (60, 50), (48, 40)):
        top = max(y for y in range(G + 1, G + RY + 1) if (x, y, z) in inner and (x, y, z) not in shellset)
        for y in range(G + 8, top + 1):
            m.set(x, y, z, "chain", {"axis": "y", "waterlogged": "false"})
        m.set(x, G + 7, z, "lantern", {"hanging": "true", "waterlogged": "false"})
    for (x, z) in ((30, 50), (66, 50), (40, 36), (56, 64), (40, 64), (33, 42), (33, 58), (63, 42), (63, 58), (48, 34), (46, 44)):
        if (x, G + 1, z) in inner and (x, G + 1, z) not in shellset:
            m.set(x, G + 1, z, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})


def face_pattern(w, h, hat=False):
    """A guh face as a w x h grid: {(i, j): part} with j = 0 the top row."""
    out = {}
    for i in range(w):
        for j in range(h):
            x = (i + 0.5) / w * 2 - 1
            y = 1 - (j + 0.5) / h * 2
            f = (x / 0.9) ** 2 + ((y + 0.1) / 0.8) ** 2
            ear = min(math.hypot((x - s * 0.66) / 0.9, (y - 0.74)) for s in (-1, 1))
            if f > 1 and ear > 0.26:
                continue
            part = "fur"
            if f > 1:
                part = "ear" if ear < 0.14 else "fur"
            else:
                if f > 0.8:
                    part = "edge"
                if hat and y > 0.36:
                    part = "hatedge" if y < 0.5 else "hat"
                    if abs(x) < 0.1 and 0.5 <= y < 0.72:
                        part = "lamp"
                elif min(math.hypot((x - s * 0.38) / 0.11, (y - 0.08) / 0.17) for s in (-1, 1)) <= 1:
                    part = "eye"
                elif min(math.hypot((x - s * 0.66) / 0.13, (y + 0.28) / 0.13) for s in (-1, 1)) <= 1:
                    part = "blush"
                elif abs(x) < 0.08 and -0.24 < y < -0.08:
                    part = "nose"
                elif -0.44 < y < -0.3 and abs(x) < 0.2:
                    part = "mouth"
                elif (x / 0.34) ** 2 + ((y + 0.34) / 0.26) ** 2 <= 1:
                    part = "muzzle"
            out[(i, j)] = part
    # a shine in each eye: the top-left cell of the eye
    for i, j in [k for k, p in out.items() if p == "eye"]:
        if out.get((i - 1, j)) != "eye" and out.get((i, j - 1)) != "eye":
            out[(i, j)] = "shine"
    return out


def mosaic(m, pattern, x0, z0, y, palette, plane="floor", facing_x=None):
    """Lays a face pattern: on the floor (i along x, j along z) or on a wall at x = facing_x (i along z, j down from y)."""
    for (i, j), part in pattern.items():
        if plane == "floor":
            m.set(x0 + i, y, z0 + j, palette[part])
        else:
            if palette.get(part):
                m.set(facing_x, y - j, z0 + i, palette[part])


# --- stairs, the drop and the lift -----------------------------------------------------------------------------------
def stairs(facing):
    return {"facing": facing, "half": "bottom", "shape": "straight", "waterlogged": "false"}


def shaft(m):
    planks = "spruce_planks"
    # the divider between the lanes (open at the landings)
    for y in range(L2 + 1, G):
        for z in range(36, 47):
            m.set(41, y, z, "stripped_spruce_log" if z in (36, 46) else "spruce_planks" if y % 7 == 0 else "stone_bricks",
                  {"axis": "y"} if z in (36, 46) else None)
    for landing, zs in ((G - 7, (36, 37)), (L1 - 7, (36, 37)), (L1, (45, 46)), (L2, (45, 46))):
        for z in zs:
            for x in range(38, 45):
                m.set(x, landing, z, planks)
            for y in range(landing + 1, landing + 4):
                m.set(41, y, z, "air")
    # the flights: A (lane 38..40) and C go down northwards, B and D (lane 42..44) southwards
    for top, xs, north in ((G, (38, 39, 40), True), (G - 7, (42, 43, 44), False), (L1, (38, 39, 40), True), (L1 - 7, (42, 43, 44), False)):
        for k in range(1, 8):
            z = 45 - k if north else 37 + k
            for x in xs:
                m.set(x, top - k + 1, z, "spruce_stairs", stairs("south" if north else "north"))
    # the L2 bottom of the shaft is a floor all over (the nook under the stairs too)
    for x in range(38, 45):
        for z in range(36, 47):
            m.set(x, L2, z, planks if z >= 45 else "cobbled_deepslate")
    # lights: lanterns hanging under the landings, froglights in the shaft walls
    for landing in (G - 7, L1 - 7):
        for x in (39, 43):
            m.set(x, landing - 1, 36, "lantern", {"hanging": "true", "waterlogged": "false"})
    for y in range(L2 + 3, G, 4):
        for (x, z) in ((37, 41), (45, 40), (39, 35), (43, 47)):
            if (x, y, z) not in m.hollow:
                m.set(x, y, z, "ochre_froglight")
    # fences around the stairwell in the hall
    for z in range(38, 45):
        m.set(37, G + 1, z, "spruce_fence")
        m.set(41, G + 1, z, "spruce_fence")
    for x in range(37, 42):
        m.set(x, G + 1, 37, "spruce_fence")
    sign(m, 39, G + 2, 37, "south", ["kaasmijn.trap1", "kaasmijn.trap2", "kaasmijn.trap3"], wall=False)

    # --- the VAHOEG-sprong ---
    for x in range(50, 53):
        for z in range(36, 39):
            for y in range(L2 - 2, L2 + 1):
                m.set(x, y, z, "water", {"level": "0"})
                m.water_ok.add((x, y, z))
    for x in range(49, 54):
        for z in range(35, 40):
            if x in (49, 53) or z in (35, 39):
                if not (x == 51 and z == 39):
                    m.set(x, G + 1, z, "spruce_fence")
                m.set(x, G, z, "yellow_concrete")
                m.set(x, L2, z, "yellow_concrete")
    sign(m, 52, G + 2, 39, "south", ["kaasmijn.sprong1", "kaasmijn.sprong2", "kaasmijn.sprong3"], wall=False)
    for y in range(L2 + 5, G, 3):                          # yellow stripes in the drop, so you see yourself fall
        for x in range(49, 54):
            for z in range(35, 40):
                if (x, y, z) not in m.hollow:
                    m.set(x, y, z, "yellow_terracotta" if (x + z + y) % 2 else "ochre_froglight")

    # --- the bubble lift ---
    for y in range(L2 + 1, G + 1):
        m.set(55, y, 37, "bubble_column", {"drag": "false"})
    m.set(55, L2, 37, "soul_sand")
    for y in range(L2 + 1, G + 4):
        for (x, z) in ((54, 36), (55, 36), (56, 36), (54, 37), (56, 37), (54, 38), (55, 38), (56, 38)):
            if y == G:
                m.set(x, y, z, "white_concrete")
            elif y <= L2 + 4 or y > G:
                if (x, z) == (55, 38) and (y <= L2 + 2 or y > G):
                    continue
                m.set(x, y, z, "pink_stained_glass")
            elif (x, y, z) not in m.hollow:
                m.set(x, y, z, "ochre_froglight" if y % 4 == 0 and (x, z) in ((54, 37), (56, 37)) else "stone_bricks")
    for x in range(54, 57):
        for z in range(36, 39):
            m.set(x, G + 4, z, "pink_stained_glass")
    m.set(55, L2 + 1, 38, "spruce_door", {"facing": "north", "half": "lower", "hinge": "left", "open": "true", "powered": "false"})
    m.set(55, L2 + 2, 38, "spruce_door", {"facing": "north", "half": "upper", "hinge": "left", "open": "true", "powered": "false"})
    sign(m, 57, G + 1, 39, "south", ["kaasmijn.lift1", "kaasmijn.lift2", "kaasmijn.lift3"], wall=False)
    sign(m, 54, L2 + 3, 39, "south", ["kaasmijn.lift1", "kaasmijn.lift4", ""])


def sign(m, x, y, z, facing, keys, wall=True):
    """A waxed sign with lang keys (so it's in the player's language). Wall signs hang on the block behind them."""
    msgs = [json.dumps({"translate": f"sign.guhs.{k}"}) if k else '""' for k in keys] + ['""'] * (4 - len(keys))
    text = {"messages": m.h.ms.NbtList(8, msgs), "color": "black", "has_glowing_text": m.h.Byte(1)}
    empty = {"messages": m.h.ms.NbtList(8, ['""'] * 4), "color": "black", "has_glowing_text": m.h.Byte(0)}
    nbt = {"id": "minecraft:sign", "is_waxed": m.h.Byte(1), "front_text": text, "back_text": empty}
    if wall:
        m.set(x, y, z, "spruce_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
    else:
        rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
        m.set(x, y, z, "spruce_sign", {"rotation": rot, "waterlogged": "false"}, nbt)


# --- tunnels: supports, lights, rails ---------------------------------------------------------------------------------
def supports(m):
    for (x0, x1, z0, z1, F, hgt, zone) in m.corridors:
        along_x = (x1 - x0) >= (z1 - z0)
        a0, a1 = (x0, x1) if along_x else (z0, z1)
        s0, s1 = (z0, z1) if along_x else (x0, x1)
        for a in range(a0 + 2, a1 - 1, 5):
            cells = []
            for y in range(F + 1, F + hgt + 1):
                cells += [((a, y, s0 - 1) if along_x else (s0 - 1, y, a), "post"), ((a, y, s1 + 1) if along_x else (s1 + 1, y, a), "post")]
            for s in range(s0 - 1, s1 + 2):
                cells.append(((a, F + hgt + 1, s) if along_x else (s, F + hgt + 1, a), "beam"))
            if not all(c not in m.hollow for c, _ in cells):
                continue                                    # (a crossing: no frame there)
            for c, part in cells:
                if part == "post":
                    m.set(*c, "stripped_spruce_log", {"axis": "y"})
                else:
                    m.set(*c, "stripped_spruce_log", {"axis": "z" if along_x else "x"})
            mid = (s0 + s1) // 2 + (1 if zone == "ring" and s1 - s0 >= 4 else 0)
            lamp = (a, F + hgt, mid) if along_x else (mid, F + hgt, a)
            if m.get(*lamp) == "minecraft:air":
                m.set(*lamp, "lantern", {"hanging": "true", "waterlogged": "false"})


def rails(m):
    """The Kaasexpress: a closed loop around L1. Powered rails in runs of 17 with a redstone block under the middle one."""
    y = L1 + 1
    loop = []
    for x in range(RING0, RING1 + 1):
        loop.append((x, RING0))
    for z in range(RING0 + 1, RING1 + 1):
        loop.append((RING1, z))
    for x in range(RING1 - 1, RING0 - 1, -1):
        loop.append((x, RING1))
    for z in range(RING1 - 1, RING0, -1):
        loop.append((RING0, z))
    corners = {(RING0, RING0): "south_east", (RING1, RING0): "south_west", (RING1, RING1): "north_west", (RING0, RING1): "north_east"}
    for (x, z) in loop:
        if (x, z) in corners:
            m.set(x, y, z, "rail", {"shape": corners[(x, z)], "waterlogged": "false"})
            continue
        side_x = z in (RING0, RING1)
        i = (x - RING0) if side_x else (z - RING0)             # distance from the corner it starts at
        shape = "east_west" if side_x else "north_south"
        if 3 <= i <= 72 and (i - 3) % 18 != 17:
            m.set(x, y, z, "powered_rail", {"shape": shape, "powered": "true", "waterlogged": "false"})
            if (i - 3) % 18 == 8:
                m.set(x, L1, z, "redstone_block")
        else:
            m.set(x, y, z, "rail", {"shape": shape, "waterlogged": "false"})
    m.loop = loop


# --- the rooms ----------------------------------------------------------------------------------------------------------
def miner(m, x, y, z, yaw, variant="normal", neck=True, scale=1.0):
    # (invulnerable and tagged: KaasmijnProtection keeps players from taming, leashing or undressing the miners)
    nbt = {"id": "guhs:guh", "NoAI": m.h.Byte(1), "Sitting": m.h.Byte(1), "PersistenceRequired": m.h.Byte(1), "Variant": variant,
           "Invulnerable": m.h.Byte(1), "Tags": m.h.ms.NbtList(8, ["guhs_kaasmijn_mijnwerker"]),
           "ClothesHead": "kaasmijn_helm", "Rotation": m.h.floats(float(yaw), 0.0),
           "attributes": m.h.compounds([{"id": "minecraft:scale", "base": m.h.Double(scale)}])}
    if neck:
        nbt["ClothesNeck"] = "kaasmijn_zakdoek"
    m.s.entity(x + 0.5, float(y), z + 0.5, nbt)
    m.entities.append((x, y, z, "miner"))


def cart(m, x, y, z, yaw, cheese=False):
    nbt = {"id": "minecraft:minecart", "Invulnerable": m.h.Byte(1), "Rotation": m.h.floats(float(yaw), 0.0), "Tags": m.h.ms.NbtList(8, ["guhs_kaasmijn"])}
    if cheese:
        nbt.update({"DisplayState": {"Name": "guhs:block_of_kaasknabbels"}, "CustomDisplayTile": m.h.Byte(1), "DisplayOffset": 6})
    m.s.entity(x + 0.5, y + 0.0625, z + 0.5, nbt)
    m.entities.append((x, y, z, "cart"))


def lamp_post(m, x, y, z, lamp="lantern"):
    m.set(x, y, z, "spruce_fence")
    m.set(x, y + 1, z, lamp if ":" in lamp else lamp, {"hanging": "false", "waterlogged": "false"})


def pool(m, cells, floor, depth=2):
    """A kaassaus pool sunk into the floor."""
    for (x, z) in cells:
        for y in range(floor - depth + 1, floor + 1):
            m.set(x, y, z, "guhs:kaas_saus", {"level": "0"})
        m.set(x, floor - depth, z, "smooth_sandstone")
    for (x, z) in cells:                                  # the rim
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if n not in cells and (n[0], floor + 1, n[1]) in m.hollow:
                m.set(n[0], floor, n[1], "yellow_terracotta")


def ellipse_cells(cx, cz, rx, rz):
    return {(x, z) for x in range(int(cx - rx) - 1, int(cx + rx) + 2) for z in range(int(cz - rz) - 1, int(cz + rz) + 2)
            if ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2 <= 1}


def ceiling_lamp(m, x, z, zone_floor):
    ys = [y for y in range(zone_floor + 1, zone_floor + 14) if (x, y, z) in m.hollow]
    if ys and m.get(x, max(ys), z) == "minecraft:air" and max(ys) - zone_floor >= 4:
        m.set(x, max(ys), z, "lantern", {"hanging": "true", "waterlogged": "false"})


def rooms(m):
    rng = m.rng
    # --- L1 hall by the stairs ---
    for x in range(36, 61):
        for z in range(48, 55):
            m.set(x, L1, z, "stone_bricks" if (x + z) % 5 else "polished_andesite")
    for x in (38, 46, 54):
        m.set(x, L1 + 4, 51, "lantern", {"hanging": "true", "waterlogged": "false"})
    sign(m, 37, L1 + 2, 48, "south", ["kaasmijn.l1_1", "kaasmijn.l1_2", "kaasmijn.l1_3"])
    miner(m, 58, L1 + 1, 49, 30, "choco")
    # --- Kaaskamer West: a round kaassaus pool, miners around it ---
    pool(m, ellipse_cells(25, 52, 3.2, 4.5), L1)
    for (x, z) in ((20, 46), (30, 46), (20, 58), (30, 58), (25, 44), (25, 60)):
        ceiling_lamp(m, x, z, L1)
    lamp_post(m, 21, L1 + 1, 52)
    lamp_post(m, 29, L1 + 1, 52)
    miner(m, 22, L1 + 1, 48, 45, "normal")
    miner(m, 28, L1 + 1, 57, 200, "mint")
    # --- Ertsgrot (east): lots of veins, scaffolding, a pile of cheese ---
    for (x, z) in ((69, 42), (73, 54), (71, 48)):
        ceiling_lamp(m, x, z, L1)
    lamp_post(m, 67, L1 + 1, 47)
    lamp_post(m, 75, L1 + 1, 50)
    for (x, z) in ((72, 42), (73, 42), (72, 43)):
        m.set(x, L1 + 1, z, "guhs:block_of_kaasknabbels")
    m.set(72, L1 + 2, 42, "guhs:block_of_kaasknabbels")
    miner(m, 70, L1 + 1, 44, 160, "snow")
    miner(m, 74, L1 + 1, 52, -60, "normal", neck=False)
    # --- Knabbelgrot (south): a pool shaped like a cheese wedge, a cart full of cheese on a short track ---
    wedge = {(x, z) for x in range(42, 55) for z in range(66, 71) if (z - 66) >= (54 - x) // 3 - 0}
    pool(m, wedge, L1)
    for x in range(40, 57):
        if (x, L1 + 1, 74) in m.hollow:
            m.set(x, L1 + 1, 74, "rail", {"shape": "east_west", "waterlogged": "false"})
    cart(m, 44, L1 + 1, 74, 90, cheese=True)
    cart(m, 52, L1 + 1, 74, 90, cheese=True)
    for (x, z) in ((40, 64), (56, 64), (48, 62), (42, 72), (54, 72)):
        ceiling_lamp(m, x, z, L1)
    lamp_post(m, 39, L1 + 1, 68)
    lamp_post(m, 57, L1 + 1, 68)
    miner(m, 41, L1 + 1, 64, -30, "normal")
    miner(m, 55, L1 + 1, 63, 30, "choco", neck=False)
    # --- the canteen ---
    for x in range(20, 35):
        for z in range(18, 31):
            m.set(x, L1, z, "spruce_planks" if (x + z) % 2 else "stripped_spruce_wood", {"axis": "y"} if (x + z) % 2 == 0 else None)
    for (tx, tz) in ((24, 22), (30, 22), (24, 27), (30, 27)):
        m.set(tx, L1 + 1, tz, "guhs:guh_tafel", {"facing": "south"})
        m.set(tx - 1, L1 + 1, tz, "guhs:guh_stoel", {"facing": "east"})
        m.set(tx + 1, L1 + 1, tz, "guhs:guh_stoel", {"facing": "west"})
    miner(m, 23, L1 + 1, 24, -90, "mint", scale=0.9)
    miner(m, 31, L1 + 1, 24, 90, "normal", scale=1.1)
    miner(m, 29, L1 + 1, 29, 90, "choco", neck=False)
    for x in (21, 22, 23):
        m.set(x, L1 + 1, 18, "barrel", {"facing": "up", "open": "false"})
    m.set(33, L1 + 1, 18, "guhs:knabbelbak", {"facing": "south"})
    m.set(34, L1 + 1, 18, "guhs:guh_kast", {"facing": "south", "open": "false"})
    for (x, z) in ((22, 20), (32, 20), (22, 28), (32, 28), (27, 24)):
        m.set(x, L1 + 4, z, "guhs:lampion_" + rng.choice(["roze", "geel", "mint"]), {"hanging": "true", "waterlogged": "false"})
    for x in range(21, 34, 3):
        m.set(x, L1 + 4, 30, "guhs:vlaggetjes", {"axis": "x"})
    # --- the stations of the Kaasexpress ---
    for x in (44, 50):
        m.set(x, L1 + 1, RING1 + 1, "guhs:karretjesautomaat", {"facing": "north"})
    m.set(60, L1 + 1, RING0 - 1, "guhs:karretjesautomaat", {"facing": "south"})
    cart(m, 47, L1 + 1, RING1, 90)
    sign(m, 46, L1 + 3, RING1 + 1, "north", ["kaasmijn.express1", "kaasmijn.express2", "kaasmijn.express3"])
    sign(m, 58, L1 + 3, RING0 - 1, "south", ["kaasmijn.express1", "kaasmijn.express2", "kaasmijn.express3"])
    for x in range(41, 56):
        m.set(x, L1, RING1 - 1, "yellow_concrete" if x % 2 else "black_concrete")   # the platform edge

    # --- L2 hall ---
    for x in range(36, 61):
        for z in range(34, 55):
            if (x, L2 + 1, z) in m.hollow and m.get(x, L2, z) not in ("minecraft:water", "minecraft:yellow_concrete", "minecraft:soul_sand"):
                if not (38 <= x <= 44 and z <= 46):
                    m.set(x, L2, z, "polished_deepslate" if (x + z) % 4 else "deepslate_tiles")
    for (x, z) in ((48, 44), (58, 44), (40, 51), (48, 51), (57, 51), (59, 35)):
        m.set(x, L2 + 4, z, "lantern", {"hanging": "true", "waterlogged": "false"})
    for (x, z) in ((56, 60), (84, 63), (57, 58)):
        ceiling_lamp(m, x, z, L2)
    for (x, z) in ((RING0 + 3, RING0 + 3), (RING1 - 3, RING0 + 3), (RING0 + 3, RING1 - 3), (RING1 - 3, RING1 - 3)):
        m.set(x, L1 + 4, z, "lantern", {"hanging": "true", "waterlogged": "false"})
    miner(m, 58, L2 + 1, 41, 90, "normal")
    # --- De Grote Kaasgrot: the guh-face kaassaus lake, a jetty, lamp posts, miners ---
    face = face_pattern(21, 15)
    lake = {}
    for (i, j), part in face.items():
        lake[(38 + i, 65 + j)] = part
    for (x, z), part in lake.items():
        if part in ("eye", "shine"):
            m.set(x, L2, z, "black_concrete" if part == "eye" else "sea_lantern")
        elif part == "nose":
            m.set(x, L2, z, "pearlescent_froglight")
        elif part == "blush":
            m.set(x, L2, z, "pink_concrete")
        elif part == "edge":
            m.set(x, L2, z, "pink_concrete")
        elif part == "mouth":
            m.set(x, L2, z, "black_concrete")
        else:
            m.set(x, L2, z, "guhs:kaas_saus", {"level": "0"})
            m.set(x, L2 - 1, z, "guhs:kaas_saus", {"level": "0"})
            m.set(x, L2 - 2, z, "smooth_sandstone")
    for (x, z) in ((30, 64), (66, 64), (30, 80), (66, 80), (48, 62), (38, 84), (58, 84), (26, 72), (70, 72)):
        ceiling_lamp(m, x, z, L2)
    for (x, z) in ((35, 68), (61, 68), (35, 77), (61, 77), (48, 82)):
        lamp_post(m, x, L2 + 1, z)
    miner(m, 33, L2 + 1, 62, 30, "normal")
    miner(m, 60, L2 + 1, 78, -140, "choco")
    miner(m, 60, L2 + 1, 61, -30, "snow", neck=False)
    # --- the gold vein gallery ---
    for (x, z) in ((80, 66), (86, 72), (83, 69)):
        ceiling_lamp(m, x, z, L2)
    lamp_post(m, 79, L2 + 1, 72, "guhs:lampion_geel")
    lamp_post(m, 84, L2 + 1, 64, "guhs:lampion_geel")
    miner(m, 85, L2 + 1, 66, -120, "normal", scale=0.85)
    # --- the treasure room ---
    treasure(m)


def treasure(m):
    x0, x1, z0, z1, F, hgt = 5, 19, 63, 77, L2, 6
    # walls of quartz bricks with pink bands, a gold-rimmed doorway
    for (x, y, z), zone in list(m.shell_zone.items()):
        if zone == "schat" and (x, y, z) not in m.hollow and x0 - 1 <= x <= x1 + 1 and z0 - 1 <= z <= z1 + 1 and F <= y <= F + hgt + 1:
            m.set(x, y, z, "pink_concrete" if y in (F + 3,) else "quartz_bricks")
    for z in range(68, 73):
        for y in range(F + 1, F + 5):
            if z in (68, 72) or y == F + 4:
                m.set(20, y, z, "gold_block")
    # floor: smooth quartz with a golden guh face
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            m.set(x, F, z, "smooth_quartz")
    mosaic(m, face_pattern(11, 9), 8, 66, F, {
        "fur": "gold_block", "edge": "yellow_concrete", "ear": "pink_concrete", "eye": "black_concrete", "shine": "white_concrete",
        "blush": "pink_concrete", "muzzle": "quartz_block", "nose": "magenta_concrete", "mouth": "black_concrete"})
    # the back wall: a big golden guh face, the kaaskluis in its mouth
    wall = face_pattern(15, 6)
    mosaic(m, wall, 0, 63, F + 6, {
        "fur": "gold_block", "edge": "raw_gold_block", "ear": "pink_concrete", "eye": "black_concrete", "shine": "white_concrete",
        "blush": "pink_concrete", "muzzle": "quartz_block", "nose": "magenta_concrete", "mouth": "black_concrete"}, plane="wall", facing_x=x0 - 1)
    m.set(x0, F + 1, 70, "guhs:kaaskluis", {"facing": "east"})
    for z in (67, 73):
        m.h.chest(m.s, x0, F + 1, z, "east", "guhs:chests/kaasmijn_schat")
    for (x, z) in ((6, 64), (6, 76), (18, 64), (18, 76)):
        m.set(x, F + 1, z, "gold_block")
        m.set(x, F + 2, z, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    for (x, z) in ((7, 65), (17, 75)):
        m.set(x, F + 1, z, "raw_gold_block")
    m.set(17, F + 1, 65, "guhs:block_of_kaasknabbels")
    for (x, z) in ((9, 67), (15, 67), (9, 73), (15, 73), (12, 70)):
        m.set(x, F + hgt + 1, z, "ochre_froglight")
    miner(m, 12, F + 1, 75, 180, "golden", scale=0.9)
    sign(m, 24, F + 2, 69, "south", ["kaasmijn.schat1", "kaasmijn.schat2", "kaasmijn.schat3"])


def cave_lights(m):
    """A lantern from the ceiling every 7 blocks in the caverns (where the ceiling is high enough)."""
    for (x, y, z), zone in list(m.hollow.items()):
        if zone in ("l1west", "l1oost", "l1zuid", "l2grot", "goud") and y == m.zones[zone] + 1 and x % 7 == 3 and z % 7 == 3:
            ceiling_lamp(m, x, z, m.zones[zone])


def ores(m):
    """Cheese veins in the walls (never in floors or ceilings): common in the caverns, gold only down below."""
    rate = {"ring": (0.018, 0), "l1gang": (0.04, 0), "l1hal": (0.01, 0), "l1west": (0.12, 0), "l1oost": (0.26, 0), "l1zuid": (0.1, 0),
            "kantine": (0, 0), "l2hal": (0.02, 0), "l2gang": (0.05, 0.004), "l2grot": (0.12, 0.012), "goud": (0.1, 0.18), "schat": (0, 0)}
    counts = {"guhs:kaasader": 0, "guhs:diepe_kaasader": 0, "guhs:gouden_kaasader": 0}
    rng = random.Random(777)
    plain = {mc(n) for n, _ in STONE_L1 + STONE_L2}
    for c in sorted(m.shell_zone):
        zone = m.shell_zone[c]
        if zone not in rate or m.get(*c) not in plain:
            continue
        x, y, z = c
        floor = m.zones.get(zone, L1)
        if not (floor + 1 <= y <= floor + 3):
            continue
        if not any((x + dx, y, z + dz) in m.hollow and m.hollow[(x + dx, y, z + dz)] == zone for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            continue
        normal, gold = rate[zone]
        r = rng.random()
        deep = y < L1 - 3
        if r < gold:
            name = "guhs:gouden_kaasader"
        elif r < gold + normal:
            name = "guhs:diepe_kaasader" if deep else "guhs:kaasader"
        else:
            continue
        m.set(x, y, z, name)
        counts[name] += 1
    m.ore_counts = counts


def froglights(m):
    """Cheese-yellow froglights in the tunnel walls, between the frames."""
    for (x0, x1, z0, z1, F, hgt, zone) in m.corridors:
        along_x = (x1 - x0) >= (z1 - z0)
        a0, a1 = (x0, x1) if along_x else (z0, z1)
        s0, s1 = (z0, z1) if along_x else (x0, x1)
        for a in range(a0 + 4, a1 - 1, 10):
            for s in (s0 - 1, s1 + 1):
                c = (a, F + 3, s) if along_x else (s, F + 3, a)
                if c not in m.hollow and m.get(*c) and "log" not in m.get(*c) and "automaat" not in m.get(*c):
                    m.set(*c, "ochre_froglight")


# --- the surface -------------------------------------------------------------------------------------------------------
def surface(m):
    rng = m.rng
    # the path to the mouth, with lamp posts
    for z in range(CZ + RZ - 2, D - 3):
        for x in range(CX - 3, CX + 4):
            if (x, G, z) not in m.head_inner and m.get(x, G + 1, z) in (None, "minecraft:air"):
                m.set(x, G, z, "yellow_terracotta" if abs(x - CX) == 3 else "smooth_sandstone")
    for z in range(CZ + RZ + 3, D - 4, 6):
        for x in (CX - 4, CX + 4):
            lamp_post(m, x, G + 1, z, "guhs:lampion_geel")
    sign(m, CX + 5, G + 1, D - 6, "south", ["kaasmijn.bord1", "kaasmijn.bord2", "kaasmijn.bord3"], wall=False)
    # cheese heaps and a track with cheese carts
    for (hx, hz, r) in ((16, 76, 4), (80, 78, 3), (18, 22, 3)):
        for x in range(hx - r, hx + r + 1):
            for z in range(hz - r, hz + r + 1):
                d = math.hypot(x - hx, z - hz)
                top = int(round((r - d) * 0.9))
                for y in range(G + 1, G + 1 + max(0, top)):
                    m.set(x, y, z, "guhs:block_of_kaasknabbels" if rng.random() < 0.8 else "yellow_terracotta")
    for x in range(22, 44):
        m.set(x, G, 80, "gravel")
        m.set(x, G + 1, 80, "rail", {"shape": "east_west", "waterlogged": "false"})
    cart(m, 26, G + 1, 80, 90, cheese=True)
    cart(m, 29, G + 1, 80, 90, cheese=True)
    for (x, z) in ((10, 40), (86, 40), (84, 16), (12, 90), (84, 90), (70, 88)):
        lamp_post(m, x, G + 1, z, "guhs:lampion_" + rng.choice(["roze", "geel", "mint"]))
    for (x, z) in ((22, 30), (74, 28), (76, 66), (20, 64), (60, 90), (34, 90)):
        m.set(x, G + 1, z, "guhs:" + rng.choice(["kaasbloem", "roze_guhbloem", "guhoortjes", "knabbelroos"]))
        m.set(x, G, z, "grass_block", {"snowy": "false"})
    # the Mijnguh's booth in the hall
    x, y, z = NPC
    for zz in range(z - 2, z + 3):
        m.set(x - 1, y, zz, "stripped_spruce_wood", {"axis": "y"})
    for xx in range(x - 1, x + 3):
        m.set(xx, y, z - 3, "barrel", {"facing": "up", "open": "false"})
        m.set(xx, y, z + 3, "barrel", {"facing": "up", "open": "false"})
    m.set(x + 2, y, z, "guhs:block_of_kaasknabbels")
    m.set(x + 2, y + 1, z, "guhs:block_of_kaasknabbels")
    m.set(x + 2, y, z - 1, "guhs:block_of_kaasknabbels")
    m.set(x - 1, y + 1, z - 2, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    m.set(x - 1, y + 1, z + 2, "guhs:lampion_geel", {"hanging": "false", "waterlogged": "false"})
    m.s.entity(x + 0.5, float(y), z + 0.5, {"id": "guhs:guh_npc", "Kind": "mijnguh", "PersistenceRequired": m.h.Byte(1),
                                             "Rotation": m.h.floats(90.0, 0.0)})
    m.entities.append((x, y, z, "npc"))
    sign(m, x - 1, y + 1, z + 3, "west", ["kaasmijn.mijnguh1", "kaasmijn.mijnguh2", "kaasmijn.mijnguh3"], wall=False)
    # a bench to sit on while the others dig
    for zz in (60, 61):
        m.set(36, G + 1, zz, "guhs:guh_bank", {"facing": "east"})
    miner(m, 34, G + 1, 46, 150, "mint", neck=False, scale=0.8)


def connect_fences(m):
    """Fences and panes connect to their neighbours (like they do when you place them)."""
    def solid(n):
        return n not in (None, "minecraft:air", "minecraft:water", "minecraft:bubble_column") and not any(t in n for t in THIN)
    for (x, y, z), (name, props, nbt) in list(m.s.blocks.items()):
        if name.endswith("_fence") or name.endswith("_pane"):
            p = {"waterlogged": "false"}
            for d, (dx, dz) in {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}.items():
                n = m.get(x + dx, y, z + dz)
                p[d] = "true" if n and (n.endswith("_fence") or n.endswith("_pane") or (solid(n) and "sign" not in n)) else "false"
            m.s.blocks[(x, y, z)] = (name, p, nbt)


# =====================================================================================================================
# the geometry self-check
# =====================================================================================================================
def check(m):
    """Walks the template like a player: from the path in front of the mouth everything must be reachable (and the way
    back up too), no floor has a hole you can fall through (except the drop into the pool), the Mijnguh and the miners
    sit on solid ground, the rail loop is closed, lanterns, rails and doors hang/stand on something, nothing floats,
    and there are no dark spots. Returns a list of problems."""
    blocks = m.s.blocks
    problems = []

    def name(c):
        b = blocks.get(c)
        if b is None:
            return None if c[1] > G else "natural"      # unset: natural rock underground, air above the pad
        return b[0]

    def passable(c):
        n = name(c)
        return n is None or n in ("minecraft:air", "minecraft:water", "minecraft:bubble_column", "guhs:kaas_saus") or \
            any(t in n for t in THIN) and not n.endswith("_door") or (n.endswith("_door") and blocks[c][1].get("open") == "true")

    def fluid(c):
        return name(c) in ("minecraft:water", "minecraft:bubble_column", "guhs:kaas_saus")

    def support(c):
        n = name(c)
        return n is not None and not passable(c) and not any(t in n for t in FENCY)

    def inside(c):
        return 0 <= c[0] < W and 0 <= c[2] < D and 0 <= c[1] < H

    def standable(c):
        return inside(c) and passable(c) and passable((c[0], c[1] + 1, c[2])) and (support((c[0], c[1] - 1, c[2])) or fluid(c))

    holes = set()

    def moves(c):
        x, y, z = c
        out = []
        if fluid(c) or name(c) == "minecraft:bubble_column":
            for dy in (1,) if name(c) == "minecraft:bubble_column" else (1, -1):
                n = (x, y + dy, z)
                if inside(n) and passable(n):
                    out.append(n)
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            # step up (a jump, or a stair) if there's headroom
            up = (nx, y + 1, nz)
            if inside(up) and passable(up) and passable((nx, y + 2, nz)) and passable((x, y + 2, z)) and support((nx, y, nz)):
                out.append(up)
            n = (nx, y, nz)
            if not (inside(n) and passable(n) and passable((nx, y + 1, nz))):
                continue
            yy = y
            while yy > 0 and not support((nx, yy - 1, nz)) and not fluid((nx, yy, nz)):
                yy -= 1
            fall = y - yy
            land = (nx, yy, nz)
            if fall > 3 and land not in m.water_ok:
                holes.add((n, fall))
                continue
            out.append(land)
        return out

    def bfs(start):
        seen = {start}
        todo = [start]
        while todo:
            c = todo.pop()
            for n in moves(c):
                if n not in seen:
                    seen.add(n)
                    todo.append(n)
        return seen

    start = (CX, G + 1, D - 4)
    if not standable(start):
        problems.append(f"the start {start} isn't standable")
    reach = bfs(start)
    targets = {
        "Mijnguh (talk)": (NPC[0] - 2, NPC[1], NPC[2]),
        "stair landing L1": (41, L1 + 1, 45), "stair landing L2": (41, L2 + 1, 45),
        "L1 hall": (48, L1 + 1, 51), "Kaaskamer West": (25, L1 + 1, 46), "Ertsgrot": (71, L1 + 1, 40),
        "Knabbelgrot": (48, L1 + 1, 62), "canteen": (27, L1 + 1, 20), "ring (station)": (48, L1 + 1, RING1 - 2),
        "ring north": (60, L1 + 1, RING0 + 2), "L2 hall": (48, L2 + 1, 50), "Grote Kaasgrot": (30, L2 + 1, 72),
        "treasure room (kluis)": (7, L2 + 1, 70), "gold gallery": (83, L2 + 1, 69), "lift door": (55, L2 + 1, 39),
        "drop pool": (51, L2, 37),
    }
    for label, c in targets.items():
        if c not in reach:
            problems.append(f"can't reach {label} at {c} from the entrance")
    back = bfs(targets["treasure room (kluis)"])
    if not any(abs(c[0] - start[0]) <= 1 and c[1] == start[1] and abs(c[2] - start[2]) <= 1 for c in back):
        problems.append("can't walk back up from the treasure room to the entrance")
    # holes: only report those next to places you can actually be
    for (n, fall) in sorted(holes):
        problems.append(f"hole: stepping into {n} drops {fall} blocks")

    # characters on solid ground
    for (x, y, z, kind) in m.entities:
        below = name((x, y - 1, z))
        if kind == "cart":
            if not name((x, y, z)) or "rail" not in name((x, y, z)):
                problems.append(f"cart at {(x, y, z)} is not on a rail")
        elif not support((x, y - 1, z)) or not passable((x, y, z)):
            problems.append(f"{kind} at {(x, y, z)} doesn't sit on solid ground ({below}, {name((x, y, z))})")

    # things that need something to hang on / stand on
    for (x, y, z), (n, props, _) in blocks.items():
        if n.endswith("rail") and not support((x, y - 1, z)):
            problems.append(f"rail at {(x, y, z)} has nothing under it")
        if n.endswith("lantern") or "lampion" in n:
            other = (x, y + 1, z) if props.get("hanging") == "true" else (x, y - 1, z)
            if not (support(other) or (name(other) or "").endswith("chain") or (name(other) or "").endswith("fence")):
                problems.append(f"{n} at {(x, y, z)} hangs/stands on nothing")
        if n in ("minecraft:gravel",) and not support((x, y - 1, z)) and name((x, y - 1, z)) != "natural":
            problems.append(f"gravel at {(x, y, z)} would fall")
        if n.endswith("_door") and props.get("half") == "lower" and not support((x, y - 1, z)):
            problems.append(f"door at {(x, y, z)} has no floor")

    # the rail loop: every rail of the loop connects to its neighbours in the loop
    loop = m.loop
    dirs = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
    for i, (x, z) in enumerate(loop):
        b = blocks.get((x, L1 + 1, z))
        if not b or "rail" not in b[0]:
            problems.append(f"loop: no rail at {(x, z)}")
            continue
        ends = b[1]["shape"].split("_")
        conn = {(x + dirs[e][0], z + dirs[e][1]) for e in ends}
        want = {loop[i - 1], loop[(i + 1) % len(loop)]}
        if conn != want:
            problems.append(f"loop: rail at {(x, z)} ({b[1]['shape']}) doesn't connect {want}")
        if b[0] == "minecraft:powered_rail":
            # powered within 8 rails of a redstone block (along the loop)
            if not any(blocks.get((loop[(i + k) % len(loop)][0], L1, loop[(i + k) % len(loop)][1]), ("",))[0] == "minecraft:redstone_block"
                       and all(blocks[(loop[(i + j) % len(loop)][0], L1 + 1, loop[(i + j) % len(loop)][1])][0] == "minecraft:powered_rail"
                               for j in range(min(0, k), max(0, k) + 1)) for k in range(-8, 9)):
                problems.append(f"loop: powered rail at {(x, z)} gets no power")

    # floating blocks: every block must connect (through blocks) to the ground layer or to the rock around the mine
    solidset = {c for c, b in blocks.items() if b[0] not in ("minecraft:air",)}
    anchored = set()
    todo = [c for c in solidset if c[1] <= G]
    anchored.update(todo)
    while todo:
        x, y, z = todo.pop()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in solidset and n not in anchored:
                anchored.add(n)
                todo.append(n)
    floating = solidset - anchored
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. {sorted(floating)[:5]}")

    # darkness: every place you can stand in the mine or the head gets some block light
    light = {}
    todo = []
    for c, b in blocks.items():
        n = b[0].split(":")[1]
        lvl = LIGHT.get(n, 0)
        if lvl:
            light[c] = lvl
            todo.append(c)
    opaque = lambda c: (name(c) not in (None, "minecraft:air", "minecraft:water", "minecraft:bubble_column", "guhs:kaas_saus")
                        and not passable(c) and not any(t in (name(c) or "") for t in FENCY + ("glass", "stairs", "slab", "chest", "guh_", "kluis", "automaat", "barrel")))
    head = 0
    while head < len(todo):
        c = todo[head]
        head += 1
        l = light[c] - 1
        if l <= 0:
            continue
        x, y, z = c
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if inside(n) and light.get(n, 0) < l and (not opaque(n) or n in m.hollow):
                light[n] = l
                todo.append(n)
    dark = [c for c in reach if (c in m.hollow or c in getattr(m, "head_inner", ())) and light.get(c, 0) < 4]
    if dark:
        problems.append(f"{len(dark)} dark spots where you can stand, e.g. {sorted(dark)[:60]}")
    m.stats = {"reachable": len(reach), "blocks": len(blocks), "ores": m.ore_counts, "loop": len(loop)}
    return problems


def build_structure(h):
    m = Mine(h)
    dig_mine(m)
    shell(m)
    pad(m)
    head(m)
    shaft(m)
    supports(m)
    rails(m)
    rooms(m)
    cave_lights(m)
    ores(m)
    froglights(m)
    surface(m)
    connect_fences(m)
    fp = [(x, z) for x in range(W) for z in range(D)]
    m.s.clear_above(fp, G + 1)
    # the anchor: the whole mine is placed around it (it becomes the hall floor again)
    floor = m.s.blocks[(48, G, 48)]
    final = floor[0] + ("[" + ",".join(f"{k}={v}" for k, v in floor[1].items()) + "]" if floor[1] else "")
    m.set(48, G, 48, "jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": "guhs:kaasmijn_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": final, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    problems = check(m)
    m.s.save("kaasmijn")
    print(f"kaasmijn: {m.stats}")
    m.problems = problems
    if problems:
        print("kaasmijn geometry check found problems:\n  " + "\n  ".join(problems[:40]))
    else:
        print("kaasmijn: geometry check ok")
    return m


# =====================================================================================================================
# textures
# =====================================================================================================================
CHEESE = (250, 200, 70)
CHEESE_DARK = (214, 150, 40)
CHEESE_LIGHT = (255, 232, 140)
GOLD = (255, 214, 70)
GOLD_LIGHT = (255, 246, 190)


def cheese_chunks(img, seed, colour=CHEESE, dark=CHEESE_DARK, light=CHEESE_LIGHT, n=4, sparkle=False):
    rng = random.Random(seed)
    px = img.load()
    spots = [(3, 4), (11, 3), (5, 11), (12, 11), (8, 7)][:n]
    for (cx, cy) in spots:
        cx += rng.randint(-1, 1)
        cy += rng.randint(-1, 1)
        r = rng.uniform(1.6, 2.5)
        for x in range(16):
            for y in range(16):
                d = math.hypot(x - cx, (y - cy) * 1.2)
                if d <= r:
                    c = light if (x - cx) + (y - cy) < -1 else colour
                    if d > r - 0.8 and (x - cx) + (y - cy) > 0:
                        c = dark
                    px[x, y] = c + (255,)
        px[int(cx), int(cy)] = dark + (255,)                     # a cheese hole
    if sparkle:
        for (x, y) in ((4, 3), (12, 10), (7, 12)):
            px[x, y] = (255, 255, 255, 255)
    return img


def mined_out(img, seed):
    rng = random.Random(seed)
    px = img.load()
    for (cx, cy) in [(3, 4), (11, 3), (5, 11), (12, 11), (8, 7)][:4]:
        for x in range(16):
            for y in range(16):
                d = math.hypot(x - cx, (y - cy) * 1.2)
                if d <= 1.8:
                    r, g, b, a = px[x, y]
                    px[x, y] = (int(r * 0.45), int(g * 0.42), int(b * 0.4), 255)
                elif d <= 2.4 and rng.random() < 0.5:
                    px[x, y] = (230, 196, 110, 255)                  # crumbs of cheese: it grows back
    return img


def kluis_textures(h):
    side = Image.new("RGBA", (16, 16))
    px = side.load()
    for x in range(16):
        for y in range(16):
            edge = x in (0, 15) or y in (0, 15)
            rivet = (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13))
            base = (190, 150, 50) if edge else (228, 188, 70)
            if rivet:
                base = (255, 240, 170)
            v = ((x * 7 + y * 13) % 5) - 2
            px[x, y] = tuple(max(0, min(255, c + v * 3)) for c in base) + (255,)
    front = side.copy()
    d = ImageDraw.Draw(front)
    d.ellipse((2, 3, 13, 14), fill=(244, 160, 190, 255), outline=(200, 100, 140, 255))   # a guh face on the vault door
    d.ellipse((1, 1, 5, 5), fill=(244, 160, 190, 255))
    d.ellipse((10, 1, 14, 5), fill=(244, 160, 190, 255))
    front.putpixel((5, 7), (20, 20, 20, 255))
    front.putpixel((10, 7), (20, 20, 20, 255))
    front.putpixel((5, 6), (255, 255, 255, 255))
    front.putpixel((10, 6), (255, 255, 255, 255))
    for x in (7, 8):
        front.putpixel((x, 9), (200, 60, 110, 255))
    d.ellipse((6, 10, 9, 13), fill=(120, 120, 130, 255), outline=(60, 60, 70, 255))       # the dial
    front.putpixel((7, 11), (240, 240, 240, 255))
    top = side.copy()
    h.save(side, "block", "kaaskluis_side.png")
    h.save(front, "block", "kaaskluis_front.png")
    h.save(top, "block", "kaaskluis_top.png")

    # the cart dispenser: spruce with a little minecart and a big red button
    base = h.vanilla("block/spruce_planks")
    front = base.copy()
    d = ImageDraw.Draw(front)
    d.rectangle((3, 4, 12, 9), fill=(120, 120, 130, 255), outline=(60, 60, 70, 255))
    d.rectangle((4, 5, 11, 6), fill=CHEESE + (255,))
    for x in (5, 10):
        d.ellipse((x - 1, 9, x + 1, 11), fill=(40, 40, 45, 255))
    d.rectangle((6, 12, 9, 14), fill=(210, 40, 50, 255), outline=(130, 20, 30, 255))
    h.save(front, "block", "karretjesautomaat_front.png")
    top = base.copy()
    d = ImageDraw.Draw(top)
    d.rectangle((0, 0, 15, 15), outline=(250, 200, 60, 255))
    h.save(top, "block", "karretjesautomaat_top.png")


ITEM_PAL = {
    ".": (0, 0, 0, 0), "k": (70, 45, 20, 255), "y": CHEESE + (255,), "Y": CHEESE_LIGHT + (255,), "d": CHEESE_DARK + (255,),
    "o": (170, 110, 30, 255), "g": GOLD + (255,), "G": GOLD_LIGHT + (255,), "w": (255, 255, 255, 255), "s": (120, 84, 50, 255),
    "S": (160, 120, 72, 255), "i": (200, 205, 215, 255), "I": (150, 155, 168, 255), "p": (244, 150, 190, 255), "P": (200, 90, 140, 255),
    "r": (210, 40, 50, 255),
}
ICONS = {
    "kaasbrok": ["................", "................", "................", "..........kk....", "........kkYYk...", "......kkYYYYYk..",
                 "....kkYYYYYYYyk.", "..kkYyyYYYYyyyk.", ".kyyyydyyyyyyyk.", ".kyyydddyyyydyk.", ".kyyyydyyyyddyk.", ".kyyyyyyyyyyyyk.",
                 ".kddyyyydyyyydk.", "..kkddddddddkk..", "....kkkkkkkk....", "................"],
    "goudkaas": ["................", "..........w.....", ".........wGw....", "..........kk....", "........kkGGk...", "......kkGGGGGk..",
                 "....kkGGGGGGGgk.", "..kkGggGGGGgggk.", ".kggggoggggggg.k", ".kgggoooggggogk.", ".kggggogggggoogk", ".kggggggggggggk.",
                 ".kooggggoggggok.", "..kkoooooooookk.", "....kkkkkkkk.w..", "................"],
    "leenhouweel": ["................", "...kkkkkkkk.....", "..kIiiiiiiIk....", ".kIikkkkkkiIk...", ".kik.....kSkik..", ".kk.....kSk.kk..",
                    "........kSk.....", ".......kSk......", "......kpk.......", ".....kpk........", "....kPk...rr....", "...kpk...r..r...",
                    "..kPk.....rr....", ".kpk............", ".kk.............", "................"],
    "kaashouweel": ["................", "...kkkkkkkk.....", "..kYyyyyyyYk....", ".kYykkkkkkyYk...", ".kyk.....kSkyk..", ".kk.....kSk.kk..",
                    "........kSk.....", ".......kSk......", "......ksk.......", ".....kSk........", "....ksk.........", "...kSk..........",
                    "..ksk...........", ".kSk............", ".kk.............", "................"],
}


# =====================================================================================================================
# build
# =====================================================================================================================
LANG = {
    # blocks and items
    "block.guhs.kaasader": ("Cheese Vein", "Kaasader"),
    "block.guhs.diepe_kaasader": ("Deep Cheese Vein", "Diepe kaasader"),
    "block.guhs.gouden_kaasader": ("Golden Cheese Vein", "Gouden kaasader"),
    "block.guhs.uitgemijnde_kaasader": ("Mined-out Cheese Vein", "Uitgemijnde kaasader"),
    "block.guhs.uitgemijnde_diepe_kaasader": ("Mined-out Deep Cheese Vein", "Uitgemijnde diepe kaasader"),
    "block.guhs.uitgemijnde_gouden_kaasader": ("Mined-out Golden Cheese Vein", "Uitgemijnde gouden kaasader"),
    "block.guhs.kaaskluis": ("Cheese Vault", "Kaaskluis"),
    "block.guhs.karretjesautomaat": ("Cart Dispenser", "Karretjesautomaat"),
    "item.guhs.kaasbrok": ("Chunk of Cheese", "Kaasbrok"),
    "item.guhs.goudkaas": ("Gold Cheese", "Goudkaas"),
    "item.guhs.leenhouweel": ("Mijnguh's Loaner Pickaxe", "Leenhouweel van de Mijnguh"),
    "item.guhs.kaashouweel": ("Cheese Pickaxe", "Kaashouweel"),
    "item.guhs.kaasmijn_helm": ("Guh Miner's Hard Hat", "Mijnguhhelm"),
    "item.guhs.kaasmijn_overall": ("Cheese Miner's Overalls", "Kaasmijnoverall"),
    "item.guhs.kaasmijn_zakdoek": ("Dusty Neckerchief", "Stoffige zakdoek"),
    "entity.guhs.guh_npc.mijnguh": ("Mijnguh", "Mijnguh"),
    # tooltips
    "item.guhs.leenhouweel.lore": ("Only for inside the cheese mine: the Mijnguh takes it back when you leave", "Alleen voor in de kaasmijn: de Mijnguh neemt hem terug als je weggaat"),
    "item.guhs.kaashouweel.lore": ("Digs cheese veins twice as fast. Vahoeg!", "Hakt kaasaders twee keer zo snel. Vahoeg!"),
    "item.guhs.goudkaas.lore": ("Rare and golden. The cheese vault opens for 2 of them", "Zeldzaam en goudgeel. De kaaskluis gaat open voor 2 stuks"),
    "block.guhs.kaaskluis.lore": ("Right-click with 2 gold cheese: a treasure!", "Rechtsklik met 2 goudkaas: een schat!"),
    "block.guhs.karretjesautomaat.lore": ("Right-click: a minecart on the rails next to it", "Rechtsklik: een karretje op de rails ernaast"),
    # the super compass
    "structure.guhs.kaasmijn": ("Cheese Mine", "Kaasmijn"),
    "structure.guhs.kaasmijn.tooltip": ("Very rare: a guh head with a hard hat, and a cheese mine deep below it",
                                        "Heel zeldzaam: een guhhoofd met een bouwhelm en een diepe kaasmijn eronder"),
    # the Mijnguh
    "quest.guhs.kaasmijn.hello": ("NJEG! Welcome to the cheese mine! Deep down there, cheese grows in the rock. Here's my loaner pickaxe: whack the yellow veins and the chunks are yours. I take it back when you leave the mine. VAHOEG, happy digging!",
                                  "NJEG! Welkom in de kaasmijn! Diep onder ons groeit kaas in de rotsen. Hier, mijn leenhouweel: hak op de gele aders en de brokken zijn voor jou. Als je de mijn uit gaat, neem ik hem terug. VAHOEG, veel hakplezier!"),
    "quest.guhs.kaasmijn.loan": ("Here, a loaner pickaxe. Only for in the mine, eh? Njeg!", "Hier, een leenhouweel. Alleen voor in de mijn, hè? Njeg!"),
    "quest.guhs.kaasmijn.on_floor": ("Njeg, your loaner pickaxe is lying right there on the floor! Pick it up, one each, vads.",
                                     "Njeg, je leenhouweel ligt daar gewoon op de grond! Raap hem op, eentje per guh, vads."),
    "quest.guhs.kaasmijn.full": ("Your pockets are full, njeg! Make a little room and I'll lend you a pickaxe.",
                                 "Je zakken zitten vol, njeg! Maak een plekje vrij, dan leen ik je een houweel."),
    "quest.guhs.kaasmijn.tip0": ("Down the stairs, or take the VAHOEG-sprong: there's water at the bottom, promise!", "Neem de trap naar beneden, of de VAHOEG-sprong: beneden is water, echt waar!"),
    "quest.guhs.kaasmijn.tip1": ("Tired legs? The Kaasexpress rides round the whole kaasgangen. Press the cart dispenser!", "Moeie pootjes? De Kaasexpress rijdt rondjes door de kaasgangen. Druk op de karretjesautomaat!"),
    "quest.guhs.kaasmijn.tip2": ("Gold cheese only grows deep down, in the deepslate. The kaaskluis in the treasure room loves it...", "Goudkaas groeit alleen heel diep, in het diepsteen. De kaaskluis in de schatkamer is er dol op..."),
    "quest.guhs.kaasmijn.tip3": ("A mined-out vein grows back by itself. Cheese is patient, just like a vadsige guh.", "Een uitgemijnde ader groeit vanzelf weer aan. Kaas heeft geduld, net als een vadsige guh."),
    "quest.guhs.kaasmijn.tip4": ("Want to look like a real mine guh? I've got a hard hat with a lamp, overalls and a neckerchief for chunks of cheese. You won't find them anywhere else, VAHOEG!", "Wil je eruitzien als een echte mijnguh? Ik heb een helm met lampje, een overall en een zakdoek voor kaasbrokken. Nergens anders te koop, VAHOEG!"),
    "quest.guhs.kaasmijn.back": ("The Mijnguh whistles: your loaner pickaxe goes back to the cheese mine. See you next time!", "De Mijnguh fluit: je leenhouweel gaat terug naar de kaasmijn. Tot de volgende keer!"),
    "quest.guhs.kaasmijn.no_build": ("Njeg! You may only dig the cheese veins here, not the mine itself.", "Njeg! Hier mag je alleen de kaasaders uithakken, niet de mijn zelf."),
    "quest.guhs.kaasmijn.need_pickaxe": ("That vein is too hard for paws: ask the Mijnguh for a pickaxe!", "Die ader is te hard voor pootjes: vraag de Mijnguh om een houweel!"),
    "quest.guhs.kaasmijn.miner_busy": ("Njeg, this miner is busy. Nibbling kaasbrokken is hard work!",
                                       "Njeg, deze mijnguh heeft het druk. Kaasbrokken knabbelen is zwaar werk!"),
    "quest.guhs.kaasmijn.growing": ("This vein is growing back. Patience, vadsig!", "Deze ader groeit weer aan. Geduld, vadsig!"),
    "quest.guhs.kaasmijn.kluis_need": ("The kaaskluis only opens for %s gold cheese (you have %s).", "De kaaskluis gaat alleen open voor %s goudkaas (je hebt er %s)."),
    "quest.guhs.kaasmijn.kluis_open": ("Click-clack... the kaaskluis opens! VAHOEG!", "Klik-klak... de kaaskluis gaat open! VAHOEG!"),
    "quest.guhs.kaasmijn.cart_ready": ("There's already a cart waiting here. All aboard!", "Er staat hier al een karretje klaar. Instappen maar!"),
    "quest.guhs.kaasmijn.cart_new": ("Toot toot! Get in and press forward. Sneak to get out.", "Tuut tuut! Stap in en druk op vooruit. Sluipen om uit te stappen."),
    "quest.guhs.kaasmijn.ride": ("Kaasexpress! Toot toot! (%s / %s seconds)", "Kaasexpress! Tuut tuut! (%s / %s seconden)"),
    "quest.guhs.kaasmijn.ride_done": ("VAHOEG! You rode the Kaasexpress for half a minute. Real mine guh stuff!", "VAHOEG! Je hebt een halve minuut in de Kaasexpress gereden. Echt iets voor een mijnguh!"),
    "quest.guhs.kaasmijn.cart_norail":("Njeg... there are no rails next to this dispenser.", "Njeg... er liggen geen rails naast deze automaat."),
    # signs
    "sign.guhs.kaasmijn.trap1": ("Stairs", "Trap"), "sign.guhs.kaasmijn.trap2": ("to the", "naar de"), "sign.guhs.kaasmijn.trap3": ("cheese tunnels", "kaasgangen"),
    "sign.guhs.kaasmijn.sprong1": ("VAHOEG-sprong!", "VAHOEG-sprong!"), "sign.guhs.kaasmijn.sprong2": ("Jump in:", "Spring maar:"),
    "sign.guhs.kaasmijn.sprong3": ("water below", "beneden is water"),
    "sign.guhs.kaasmijn.lift1": ("Bubble lift", "Bubbellift"), "sign.guhs.kaasmijn.lift2": ("up from", "omhoog vanaf"),
    "sign.guhs.kaasmijn.lift3": ("the deep mine", "de diepe mijn"), "sign.guhs.kaasmijn.lift4": ("up to the hall", "naar boven"),
    "sign.guhs.kaasmijn.l1_1": ("Cheese tunnels", "Kaasgangen"), "sign.guhs.kaasmijn.l1_2": ("Kaasexpress", "Kaasexpress"),
    "sign.guhs.kaasmijn.l1_3": ("<- canteen", "<- kantine"),
    "sign.guhs.kaasmijn.express1": ("KAASEXPRESS", "KAASEXPRESS"), "sign.guhs.kaasmijn.express2": ("Press the dispenser", "Druk op de automaat"),
    "sign.guhs.kaasmijn.express3": ("sneak = get out", "sluipen = uitstappen"),
    "sign.guhs.kaasmijn.schat1": ("Treasure room", "Schatkamer"), "sign.guhs.kaasmijn.schat2": ("Kaaskluis:", "Kaaskluis:"),
    "sign.guhs.kaasmijn.schat3": ("2 gold cheese", "2 goudkaas"),
    "sign.guhs.kaasmijn.bord1": ("DE KAASMIJN", "DE KAASMIJN"), "sign.guhs.kaasmijn.bord2": ("Mind your head!", "Pas op je hoofd!"),
    "sign.guhs.kaasmijn.bord3": ("VAHOEG!", "VAHOEG!"),
    "sign.guhs.kaasmijn.mijnguh1": ("Mijnguh", "Mijnguh"), "sign.guhs.kaasmijn.mijnguh2": ("Pickaxes on loan", "Houwelen te leen"),
    "sign.guhs.kaasmijn.mijnguh3": ("& cheese shop", "& kaaswinkeltje"),
    # advancements
    "advancements.guhs.guhmension.find_kaasmijn.title": ("Mind Your Head!", "Pas op je hoofd!"),
    "advancements.guhs.guhmension.find_kaasmijn.description": ("Find the very rare cheese mine", "Vind de heel zeldzame kaasmijn"),
    "advancements.guhs.guhmension.kaasmijn_goud.title": ("Gouda-Gold", "Goudse goudkaas"),
    "advancements.guhs.guhmension.kaasmijn_goud.description": ("Dig up gold cheese deep in the cheese mine", "Hak goudkaas uit, diep in de kaasmijn"),
    "advancements.guhs.guhmension.kaasmijn_mijnwerker.title": ("A Real Mine Guh", "Een echte mijnguh"),
    "advancements.guhs.guhmension.kaasmijn_mijnwerker.description": ("Buy the whole miner outfit from the Mijnguh", "Koop het hele mijnwerkerspakje bij de Mijnguh"),
}


def build(h):
    h.ms = __import__("make_structures")
    A, D_, R = h.A, h.D, h.R

    # --- blocks: the veins (vanilla stone / deepslate with cheese in them), mined out, the vault, the dispenser ---
    stone, deep = h.vanilla("block/stone"), h.vanilla("block/deepslate")
    h.save(cheese_chunks(stone.copy(), 1), "block", "kaasader.png")
    h.save(cheese_chunks(deep.copy(), 2), "block", "diepe_kaasader.png")
    h.save(cheese_chunks(deep.copy(), 3, GOLD, (220, 160, 30), GOLD_LIGHT, n=5, sparkle=True), "block", "gouden_kaasader.png")
    h.save(mined_out(stone.copy(), 4), "block", "uitgemijnde_kaasader.png")
    h.save(mined_out(deep.copy(), 5), "block", "uitgemijnde_diepe_kaasader.png")
    h.save(mined_out(deep.copy(), 6), "block", "uitgemijnde_gouden_kaasader.png")
    for name in ("kaasader", "diepe_kaasader", "gouden_kaasader", "uitgemijnde_kaasader", "uitgemijnde_diepe_kaasader", "uitgemijnde_gouden_kaasader"):
        h.simple_block(name)
    kluis_textures(h)
    for name in ("kaaskluis", "karretjesautomaat"):
        h.w(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/orientable", "textures": {
            "front": f"guhs:block/{name}_front", "side": f"guhs:block/{name}_side" if name == "kaaskluis" else "minecraft:block/spruce_planks",
            "top": f"guhs:block/{name}_top"}})
        h.w(f"{A}/blockstates/{name}.json", {"variants": h.facing_states(name)})
        h.w(f"{A}/models/item/{name}.json", {"parent": f"guhs:block/{name}"})
        h.self_drop(name)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{n}" for n in (
        "kaasader", "diepe_kaasader", "gouden_kaasader", "uitgemijnde_kaasader", "uitgemijnde_diepe_kaasader", "uitgemijnde_gouden_kaasader",
        "kaaskluis")])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:karretjesautomaat"])

    # --- loot: veins drop chunks of cheese (more with fortune), gold veins gold cheese ---
    def ore_loot(name, item, lo, hi, extra=None):
        pools = [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": item, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}},
            {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
            {"function": "minecraft:explosion_decay"}]}]}]
        if extra:
            pools.append(extra)
        h.w(f"{D_}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": pools})
    ore_loot("kaasader", "guhs:kaasbrok", 1, 3)
    ore_loot("diepe_kaasader", "guhs:kaasbrok", 2, 4)
    ore_loot("gouden_kaasader", "guhs:goudkaas", 1, 1, {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaasbrok",
                                                                                   "functions": h.count_fn(1, 3)}]})
    h.w(f"{D_}/loot_table/chests/kaasmijn_schat.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:goudkaas", "functions": h.count_fn(1, 2)}]},
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            {"type": "minecraft:item", "name": "guhs:kaasbrok", "weight": 6, "functions": h.count_fn(6, 16)},
            {"type": "minecraft:item", "name": "minecraft:gold_ingot", "weight": 4, "functions": h.count_fn(2, 6)},
            {"type": "minecraft:item", "name": "minecraft:raw_gold", "weight": 3, "functions": h.count_fn(3, 8)},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 4, "functions": h.count_fn(4, 10)},
            {"type": "minecraft:item", "name": "guhs:kaashoning", "weight": 2, "functions": h.count_fn(1, 3)},
            {"type": "minecraft:item", "name": "guhs:guh_kristal", "weight": 3, "functions": h.count_fn(3, 8)},
            {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 1, "functions": h.count_fn(1, 2)},
            {"type": "minecraft:item", "name": "minecraft:enchanted_golden_apple", "weight": 1}]}]})
    h.w(f"{D_}/loot_table/chests/kaasmijn_kluis.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
            {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 3, "functions": h.count_fn(1, 3)},
            {"type": "minecraft:item", "name": "minecraft:emerald", "weight": 3, "functions": h.count_fn(2, 6)},
            {"type": "minecraft:item", "name": "minecraft:gold_block", "weight": 3, "functions": h.count_fn(1, 2)},
            {"type": "minecraft:item", "name": "guhs:vahoege_vads_ingot", "weight": 2, "functions": h.count_fn(1, 2)},
            {"type": "minecraft:item", "name": "guhs:guh_kristal", "weight": 4, "functions": h.count_fn(4, 12)},
            {"type": "minecraft:item", "name": "guhs:gefrituurde_kaasknabbels", "weight": 4, "functions": h.count_fn(8, 16)},
            {"type": "minecraft:item", "name": "guhs:kaasfondue", "weight": 2},
            {"type": "minecraft:item", "name": "minecraft:golden_apple", "weight": 2, "functions": h.count_fn(1, 2)}]}]})

    # --- items ---
    for name, rows in ICONS.items():
        h.save(h.grid(rows, ITEM_PAL), "item", f"{name}.png")
        h.item_model(name, parent="minecraft:item/handheld" if "houweel" in name else "minecraft:item/generated")
    h.shapeless("kaasbrok_knabbels", ["guhs:kaasbrok"], "guhs:kaas_knabbels", 2)

    # --- the Mijnguh: a cheese-orange sitting guh ---
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    h.save(h.recolour(src, hue=0.1, sat=1.15, val=1.05, only=h.pinkish), "entity", "npc_mijnguh.png")

    # --- the structure: very rare, not in the mountains or the sea; nothing spawns inside ---
    h.TEMPLATE_SIZES["kaasmijn"] = 48          # (the anchor is in the middle: the flatness check samples +-48)
    h.FLATNESS["kaasmijn"] = 12
    none = {"bounding_box": "full", "spawns": []}
    h.structure("kaasmijn", h.GUHMENSION_LAND, spacing=90, separation=30, salt=20240189, start_y=-G, reach=80,
                centre="guhs:kaasmijn_midden", spawn_overrides={"creature": none, "monster": none, "ambient": none, "underground_water_creature": none})
    path = f"{D_}/worldgen/structure/kaasmijn.json"
    data = json.load(open(path, encoding="utf-8"))
    # the terrain is not carved or beard-filled around the mine: that would hollow out all the rock around the tunnels
    data["terrain_adaptation"] = "none"
    data["jigsaw"]["terrain_adaptation"] = "none"
    h.w(path, data)
    if build_structure(h).problems:
        raise SystemExit("kaasmijn: fix the template (see the geometry check above)")

    # --- advancements: shown ones in the Guhmension tab, hidden ones for the quests ---
    for name, parent, icon, frame, crit in [
        ("find_kaasmijn", "enter_guhmension", "guhs:kaasbrok", "goal",
         {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": ["guhs:kaasmijn"]}}}}),
        ("kaasmijn_goud", "find_kaasmijn", "guhs:goudkaas", "goal",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:goudkaas"}]}}),
        ("kaasmijn_mijnwerker", "kaasmijn_goud", "guhs:kaasmijn_helm", "challenge",
         {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
             {"items": "guhs:kaasmijn_helm"}, {"items": "guhs:kaasmijn_overall"}, {"items": "guhs:kaasmijn_zakdoek"}]}}),
    ]:
        h.w(f"{D_}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": {"done": crit}})
    for name in ("kaasmijn_leenhouweel", "kaasmijn_rondrit", "kaasmijn_kluis"):
        h.w(f"{D_}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})

    for key, (en, nl) in LANG.items():
        h.lang(key, en, nl)
    selfcheck_assets(h)


def selfcheck_assets(h):
    """check_assets.py only knows the registry classes: this checks our own blocks and items."""
    A = h.A
    missing = []
    for b in ("kaasader", "diepe_kaasader", "gouden_kaasader", "uitgemijnde_kaasader", "uitgemijnde_diepe_kaasader",
              "uitgemijnde_gouden_kaasader", "kaaskluis", "karretjesautomaat"):
        for p in (f"{A}/blockstates/{b}.json", f"{A}/models/item/{b}.json"):
            if not os.path.exists(p):
                missing.append(p)
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ("kaasbrok", "goudkaas", "leenhouweel", "kaashouweel"):
        if not os.path.exists(f"{A}/models/item/{i}.json") or not os.path.exists(f"{A}/textures/item/{i}.png"):
            missing.append(f"item {i}")
    if missing:
        raise SystemExit(f"kaasmijn assets missing: {missing}")


# =====================================================================================================================
# guh clothes: the miner outfit (only from the Mijnguh)
# =====================================================================================================================
def clothes(rng, v):
    f = lambda c, n=8: (lambda: v.fabric(c, rng, n))
    return {
        "kaasmijn_helm": {"helmet": f((250, 196, 30), 6), "kaasmijn_lamp": f((255, 248, 200), 4)},
        "kaasmijn_overall": {"suit": lambda: v.band((238, 128, 30), (225, 230, 235), rng, (9, 21))},
        "kaasmijn_zakdoek": {"scarf": lambda: v.dots((200, 40, 50), (250, 250, 250), rng, every=5, size=1)},
    }


def icons(ic):
    helm = ic.shaped("helmet", (190, 140, 0), (250, 196, 30), (255, 230, 90)).copy()
    px = helm.load()
    for (x, y) in ((6, 6), (7, 6), (6, 7), (7, 7)):            # the headlamp
        px[x, y] = (255, 250, 210, 255)
    return {
        "kaasmijn_helm": helm,
        "kaasmijn_overall": ic.shirt((238, 128, 30), (170, 80, 10), (225, 230, 235), "stripes"),
        "kaasmijn_zakdoek": ic.shaped("scarf", (120, 20, 25), (200, 40, 50), (250, 250, 250)),
    }


# =====================================================================================================================
# FTB quests (row y = 42)
# =====================================================================================================================
def ftb(fq):
    q = fq.q
    q("kaasmijn_find", "Pas op je hoofd!", "Ergens diep in de Guhmensie staat een reuzenguhhoofd met een gele bouwhelm op. Loop door zijn mond naar binnen: daar is de &6kaasmijn&r! Heel zeldzaam: gebruik het superkompas (Avontuur).",
      "guhs:kaasbrok", [fq.structure("kaasmijn")], rewards=(("guhs:kaas_knabbels", 16),), x=-8, y=42, shape="hexagon", xp=100)
    q("kaasmijn_leen", "Een houweel te leen", "Praat met de &dMijnguh&r in het hoofd. Hij leent je een houweel, zodat je niks hoeft mee te nemen. Njeg, wel teruggeven hoor!",
      "guhs:leenhouweel", [fq.adv("kaasmijn_leenhouweel")], x=-6, y=42)
    q("kaasmijn_brok", "Kaas uit de rotsen", "Hak op de gele &6kaasaders&r in de kaasgangen: die vind je nergens anders dan hier in de kaasmijn! Uitgemijnde aders groeien vanzelf weer aan.",
      "guhs:kaasbrok", [fq.item("guhs:kaasbrok", 32)], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-4, y=42)
    q("kaasmijn_rit", "De Kaasexpress", "Druk op een karretjesautomaat, stap in en rij een halve minuut rondjes door de kaasgangen. VAHOEG!",
      "minecraft:minecart", [fq.adv("kaasmijn_rondrit")], x=-2, y=42)
    q("kaasmijn_goud", "Goudse goudkaas", "Diep in de mijn, in het donkere diepsteen, glinstert soms een &6gouden kaasader&r. Neem de VAHOEG-sprong naar beneden!",
      "guhs:goudkaas", [fq.item("guhs:goudkaas")], rewards=(("guhs:kaasbrok", 16),), x=0, y=42)
    q("kaasmijn_kluis", "Klik-klak, de kaaskluis", "In de schatkamer zit de kaaskluis in de mond van een gouden guh. Voor 2 goudkaas gaat hij open...",
      "guhs:kaaskluis", [fq.adv("kaasmijn_kluis")], rewards=(("guhs:goudkaas", 1),), x=2, y=42, shape="gear", xp=200)
    q("kaasmijn_pakje", "Een echte mijnguh", "Koop de mijnguhhelm (met lampje!), de kaasmijnoverall en de stoffige zakdoek bij de Mijnguh (alleen hij verkoopt ze) en trek ze je guh aan. VAHOEG, wat een vakguh!",
      "guhs:kaasmijn_helm", [fq.item("guhs:kaasmijn_helm"), fq.item("guhs:kaasmijn_overall"), fq.item("guhs:kaasmijn_zakdoek")],
      rewards=(("guhs:kaashouweel", 1),), x=4, y=42, shape="rsquare", xp=150)
