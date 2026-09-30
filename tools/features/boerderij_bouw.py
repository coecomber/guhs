"""
De Guhboerderij (2.8) - the farm template (guhs:guhboerderij, 64 x 40 x 64, ground top at y = G = 4) and its geometry
self-check. Its three stalls ARE the three guh animals, as big as houses:

  - de Koeienschuur (north): a giant guhkoe. A long barn body of white wool with cheese-yellow spots (with little
    holes), a big fluffy roof, a tail at the back, hooves at the corners, and in front her head: a pink guh face with
    window eyes, a muzzle with nostrils, round ears and two horns. Her mouth is the barn door. Inside: three cow stalls
    with voerbakken and guhkoeien, Boerin Hooibaal at her counter, the farm chest, hay, lanterns.
  - de Schaapjesstal (north-west): a giant guhschaapje: a round cloud of pluiswol with a pink guh face, ears and little
    legs; its door opens onto the fenced schaapjeswei with guhschaapjes.
  - het Kippenhok (north-east): a giant knabbelkippetje: a round cheese-yellow guh ball with window eyes, a beak over the
    door, a comb on top, wings on its sides and orange feet in front; inside kippennestjes; a fenced kippenren.
  - the yard (voerbakken, a well, a hay-bale guh, benches, lamp posts), the moestuin (south-east: rows of
    guh_moestuinbakken with the three guh plants, bloempotten, a scarecrow guh), the guhmolen (south-west: a windmill
    with a guh face), and the entrance arch with guh faces (south). A fence all round.

build(h) returns the Bouw; check(b) lists problems (reachable on foot from the entrance, NPC/animals on a floor, doors
and ladders whole, lamps hang/stand on something, lit insides, nothing floats, jigsaw anchor).
Run on its own (from the project root):  python tools/features/boerderij_bouw.py
"""
import math
import os
import random
import sys
from collections import deque

W, H, D = 64, 40, 64
G = 4
NAME = "guhboerderij"
ANCHOR = "guhs:guhboerderij_midden"
CX, CZ = 32, 32

KS = "guhs:knuffelsteen"
KS_TRAP = "guhs:knuffelsteen_trap"
KS_PLAAT = "guhs:knuffelsteen_plaat"
KS_MUUR = "guhs:knuffelsteen_muur"
KS_FACE = "guhs:knuffelsteen_gezicht"
DAK = "guhs:pluisdak"
DAK_PLAAT = "guhs:pluisdak_plaat"
KLINK = "guhs:knuffelklinkers"
WOLBLOK = "guhs:pluiswolblok"
VOERBAK = "guhs:guh_voerbak"
NEST = "guhs:kippennestje"
BAK = "guhs:guh_moestuinbak"
POT = "guhs:guh_bloempot"
BLOEMBAK = "guhs:seizoensbloembak"
BANK = "guhs:guh_bank"
GRAS = "minecraft:grass_block"
LAMP = "minecraft:lantern"
HEK = "minecraft:cherry_fence"
POORT = "minecraft:cherry_fence_gate"
HOOI = "minecraft:hay_block"
PLANK = "minecraft:cherry_planks"
LOG = "minecraft:stripped_cherry_log"
LADDER = "minecraft:ladder"
FLOWERS = ["minecraft:pink_tulip", "minecraft:allium", "minecraft:oxeye_daisy", "minecraft:cornflower", "guhs:roze_guhbloem",
           "guhs:knabbelroos", "guhs:guhoortjes"]
PLANTS = set(FLOWERS) | {"minecraft:short_grass", "guhs:pluisgras"}
FACING = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}

THIN = ("lantern", "carpet", "sign", "_door", "ladder", "torch", "button", "pressure_plate", "flower_pot", "potted_", "candle",
        "seizoensslinger", "pluisgras", "short_grass", "vlaggetjes", "fence_gate", "banner")
LIGHTS = {"minecraft:lantern": 15, "minecraft:pearlescent_froglight": 15, "minecraft:campfire": 15, "minecraft:glowstone": 15, "minecraft:shroomlight": 15, "minecraft:sea_lantern": 15}


def solid(b):
    """Can you stand on it?"""
    if b is None or b in ("minecraft:air", "minecraft:water", "minecraft:jigsaw", "minecraft:structure_void"):
        return False
    if any(t in b for t in THIN) or b in PLANTS:
        return False
    return True


def passable(b):
    """Can your body be in it? (Fence gates: you open them.)"""
    return b is None or b in ("minecraft:air", "minecraft:jigsaw", "minecraft:structure_void") or b in PLANTS or any(t in b for t in (
        "carpet", "_door", "ladder", "lantern", "flower_pot", "potted_", "sign", "pressure_plate", "fence_gate", "banner", "seizoensslinger"))


def opaque(b):
    """Blocks light (roughly)."""
    return b is not None and solid(b) and not any(t in b for t in ("glass", "pane", "fence", "_wall", "muur", "slab", "plaat", "trap", "stairs",
                                                                  "voerbak", "nestje", "bloempot", "moestuinbak", "bank", "chest", "barrel",
                                                                  "composter", "cauldron", "bell", "leaves", "bladeren", "hopper", "lantern"))


class Bouw:
    def __init__(self, h):
        self.h = h
        self.s = h.Structure((W, H, D))
        self.rng = random.Random(20280501)
        self.targets = {}
        self.starts = []
        self.floors = []
        self.binnen = set()          # (x, z) columns under a roof (inside the buildings)
        self.gezichten = 0
        self.dieren = {}

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, name, props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def props(self, x, y, z):
        b = self.s.blocks.get((x, y, z))
        return b[1] if b else {}

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        self.s.fill(x0, y0, z0, x1, y1, z1, name, props)

    def air(self, x0, y0, z0, x1, y1, z1):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.s.blocks.pop((x, y, z), None)

    def npc(self, x, y, z, kind, facing):
        ms = self.h.ms
        self.s.entity(x + 0.5, float(y), z + 0.5, {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": ms.Byte(1),
                                                   "Rotation": ms.floats(YAW[facing], 0.0)})
        self.floors.append((x, y, z))
        self.targets[f"npc {kind}"] = (x, y, z)

    def dier(self, x, y, z, soort, baby=False):
        ms = self.h.ms
        nbt = {"id": f"guhs:{soort}", "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(self.rng.choice(list(YAW.values())), 0.0)}
        if baby:
            nbt["Age"] = -24000
        self.s.entity(x + 0.5, float(y), z + 0.5, nbt)
        self.floors.append((x, y, z))
        self.dieren[soort] = self.dieren.get(soort, 0) + 1

    def lamp_hang(self, x, y, z):
        self.set(x, y, z, LAMP, {"hanging": "true", "waterlogged": "false"})

    def lamp_ketting(self, x, z, plafond):
        """A lantern on a chain from the ceiling (the block above `plafond`), hanging at G + 4 (over your head)."""
        for y in range(G + 5, plafond + 1):
            if self.get(x, y, z) is None:
                self.set(x, y, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        self.lamp_hang(x, G + 4, z)

    def lamp_staand(self, x, y, z):
        self.set(x, y, z, LAMP, {"hanging": "false", "waterlogged": "false"})

    def lantaarnpaal(self, x, z, height=3):
        self.set(x, G + 1, z, KS_FACE, {"facing": self.rng.choice(list(FACING)), "stemming": str(self.rng.randrange(4))})
        self.gezichten += 1
        for y in range(G + 2, G + 1 + height):
            self.set(x, y, z, KS_MUUR, {"up": "true"})
        self.lamp_staand(x, G + 1 + height, z)


# =====================================================================================================================
# a guh face in blocks (the guh's own style: round eyes with a blue ring and a shine, blush, a small nose)
# =====================================================================================================================
def face_role(u, v, R, mouth=True):
    role = None
    if (u / (R + 0.4)) ** 2 + (v / (0.9 * R + 0.4)) ** 2 <= 1:
        role = "skin"
        for sx in (-1, 1):
            ex, ey = sx * 0.42 * R, 0.1 * R
            d = math.dist((u, v), (ex, ey))
            er = 0.27 * R + 0.35
            if d <= er:
                role = "ring" if (v < ey - 0.25 * er and d > 0.45 * er) else "eye"
                if round(u) == round(ex - 0.3 * er) and round(v) == round(ey + 0.35 * er):
                    role = "shine"
            if math.dist((u, v), (sx * 0.7 * R, -0.35 * R)) <= 0.13 * R + 0.25:
                role = "cheek"
        if math.dist((u, v), (0, -0.28 * R)) <= 0.08 * R + 0.3:
            role = "nose"
        y0 = round(-0.46 * R)
        if mouth and ((round(u), round(v)) in {(-1, y0), (0, y0 - 1), (1, y0)} or (R >= 5 and (round(u), round(v)) in {(-2, y0 + 1), (2, y0 + 1)})):
            role = "mouth"
    return role


WINDOW_FACE = {"eye": "minecraft:black_stained_glass", "ring": "minecraft:light_blue_stained_glass", "shine": "minecraft:white_stained_glass",
               "cheek": "minecraft:pink_concrete", "nose": "minecraft:magenta_concrete", "mouth": "minecraft:purple_concrete"}


def face_on_plane(b, cx, cy, cz, facing, R, blocks=None, only=("eye", "ring", "shine", "cheek", "nose", "mouth"), skip=None, depth=1, mouth=True):
    """Paints a guh face (radius R) onto the wall plane facing `facing`, centred on (cx, cy, cz). The eye blocks go
    `depth` blocks deep (windows through a thick wall)."""
    blocks = blocks or WINDOW_FACE
    dx, dz = FACING[facing]
    rx, rz = (-dz, dx)
    n = int(R + 1)
    for du in range(-n, n + 1):
        for dv in range(-n, n + 1):
            role = face_role(du, dv, R, mouth)
            if role is None or role not in only:
                continue
            x, y, z = cx + rx * du, cy + dv, cz + rz * du
            if skip and skip(x, y, z, role):
                continue
            for k in range(depth if role in ("eye", "ring", "shine") else 1):
                b.set(x - dx * k, y, z - dz * k, blocks[role])
    b.gezichten += 1


def ear_disc(b, cx, cy, z, r, outer, inner):
    """A round guh ear: a disc in the x-y plane (two blocks thick), its inside coloured on the front (z + 1)."""
    for du in range(-int(r) - 1, int(r) + 2):
        for dv in range(-int(r) - 1, int(r) + 2):
            d2 = du * du + dv * dv
            if d2 <= r * r + 0.5:
                b.set(cx + du, cy + dv, z, outer)
                b.set(cx + du, cy + dv, z + 1, inner if d2 <= (r - 1.3) ** 2 else outer)


def superellipse(cx, cz, rx, rz, p=4):
    def inside(x, z, shrink=0.0):
        return (abs(x - cx) / (rx - shrink)) ** p + (abs(z - cz) / (rz - shrink)) ** p <= 1.0
    return inside


def body(b, cx, cz, rx, rz, wall_top, wall, floor, roof, roof_h, p=4, base=KS, wall_fn=None):
    """A round building: floor, a wall ring from G+1 to wall_top, and a dome roof; returns (cells, inner, roof_top)."""
    inside = superellipse(cx, cz, rx, rz, p)
    cells = [(x, z) for x in range(int(cx - rx) - 1, int(cx + rx) + 2) for z in range(int(cz - rz) - 1, int(cz + rz) + 2) if inside(x, z)]
    inner = {(x, z) for (x, z) in cells if inside(x, z, 1.0)}
    top = {}
    for (x, z) in cells:
        for y in range(G - 3, G):
            b.set(x, y, z, KS)
        b.set(x, G, z, floor if (x, z) in inner else base)
        for y in range(G + 1, wall_top + 1):
            if (x, z) in inner:
                b.air(x, y, z, x, y, z)
            else:
                blk = wall_fn(x, y, z) if wall_fn else wall
                b.set(x, y, z, base if y <= G + 1 else blk)
        dist = min(1.0, math.hypot((x - cx) / rx, (z - cz) / rz))
        hgt = int(round(roof_h * math.sqrt(max(0.0, 1 - dist ** 2))))
        for y in range(wall_top + 1, wall_top + 2 + hgt):
            b.set(x, y, z, roof(x, y, z) if callable(roof) else roof)
        top[(x, z)] = wall_top + 1 + hgt
        if (x, z) in inner:
            b.binnen.add((x, z))
    return cells, inner, top


def outer_x(cells, z, side):
    row = [x for (x, zz) in cells if zz == z]
    return (max(row) if side > 0 else min(row)) if row else None


def outer_z(cells, x, side):
    col = [z for (xx, z) in cells if xx == x]
    return (max(col) if side > 0 else min(col)) if col else None


# =====================================================================================================================
# de Koeienschuur: a giant guhkoe
# =====================================================================================================================
KOE_C = (31.5, 11.5)
KOE_R = (12.5, 9.5)
KOE_TOP = G + 9


def koe_vlekken(b, count=26):
    """Cheese-coloured spots with little holes all over the cow's white body and back (on the shell blocks)."""
    shell = [p for p, (blk, _, _) in b.s.blocks.items() if blk == "minecraft:white_wool"]
    rng = b.rng
    for _ in range(count):
        cx, cy, cz = rng.choice(shell)
        r = rng.uniform(1.8, 3.4)
        for (x, y, z) in shell:
            d = math.dist((x, y, z), (cx, cy, cz))
            if d <= r and b.get(x, y, z) == "minecraft:white_wool":
                b.set(x, y, z, "minecraft:orange_wool" if d <= 0.75 and r > 2.5 else "minecraft:yellow_wool")


def koeienschuur(b):
    cx, cz = KOE_C
    rx, rz = KOE_R
    cells, inner, top = body(b, cx, cz, rx, rz, KOE_TOP, "minecraft:white_wool", PLANK, "minecraft:white_wool", 8)
    koe_vlekken(b)
    front = max(z for (_, z) in cells)                     # the body's front wall row (z = 21)
    # --- the head: a pink box in front of the body, x 25..38, z front+1..front+4, y G..G+12 -------------------------
    hx0, hx1, hz0, hz1, hy1 = 25, 38, front - 1, front + 4, G + 12
    for x in range(hx0, hx1 + 1):
        for z in range(hz0, hz1 + 1):
            edge_x = x in (hx0, hx1)
            edge_z = z == hz1
            for y in range(G - 3, G):
                b.set(x, y, z, KS)
            b.set(x, G, z, PLANK if not (edge_x or edge_z) else KS)
            for y in range(G + 1, hy1 + 1):
                corner = (x in (hx0, hx1)) and (y in (G + 1, hy1) or z == hz1) and y > hy1 - 1
                if edge_x or edge_z or y == hy1:
                    if not corner:
                        b.set(x, y, z, "minecraft:pink_wool")
                else:
                    b.air(x, y, z, x, y, z)
            if not (edge_x or edge_z):
                b.binnen.add((x, z))
    # round the head's front corners a little
    for (x, y) in ((hx0, hy1), (hx1, hy1), (hx0, G + 1), (hx1, G + 1)):
        b.air(x, y, hz1, x, y, hz1)
    # open the wall between head and body
    for x in range(hx0 + 1, hx1):
        for z in range(hz0, front + 1):
            for y in range(G + 1, min(hy1, KOE_TOP + 1)):
                b.air(x, y, z, x, y, z)
            b.set(x, G, z, PLANK)
            b.binnen.add((x, z))
    # the face on the front of the head: window eyes, blush; the muzzle; the door is her mouth
    hcx = (hx0 + hx1) // 2
    face_on_plane(b, hcx, G + 9, hz1, "south", 6.2, only=("eye", "ring", "shine", "cheek"), depth=1)
    for x in range(hcx - 4, hcx + 5):                     # the muzzle
        for y in range(G + 4, G + 7):
            if not (abs(x - hcx) == 4 and y in (G + 4, G + 6)):
                b.set(x, y, hz1 + 1, "minecraft:pink_concrete")
    for x in (hcx - 2, hcx + 2):
        b.set(x, G + 5, hz1 + 1, "minecraft:magenta_concrete")      # nostrils
    for x in range(hcx - 1, hcx + 2):                                # the door: her mouth
        for y in range(G + 1, G + 4):
            b.air(x, y, hz1, x, y, hz1)
    for x in (hcx - 2, hcx + 2):
        for y in range(G + 1, G + 4):
            b.set(x, y, hz1, LOG, {"axis": "y"})
    for x in range(hcx - 2, hcx + 3):
        b.set(x, G + 4, hz1, LOG, {"axis": "x"})
    b.set(hcx, G + 3, hz1 + 1, LAMP, {"hanging": "true", "waterlogged": "false"})
    # horns and ears on the head
    for x in (hx0 + 2, hx1 - 2):
        b.set(x, hy1 + 1, hz1 - 2, "minecraft:white_terracotta")
        b.set(x, hy1 + 2, hz1 - 2, "minecraft:yellow_terracotta")
    ear_disc(b, hx0 - 2, G + 10, hz1 - 2, 2.2, "minecraft:pink_wool", "minecraft:magenta_wool")
    ear_disc(b, hx1 + 2, G + 10, hz1 - 2, 2.2, "minecraft:pink_wool", "minecraft:magenta_wool")
    for x in (hx0 - 1, hx1 + 1):                                     # the ears hold on to the head
        b.set(x, G + 10, hz1 - 2, "minecraft:pink_wool")
    # the tail at the back (north), hooves at the four corners, a little face on each side
    tx = int(cx)
    back = min(z for (_, z) in cells)
    for (dx, dy) in ((0, 0), (0, -1), (0, -2), (0, -3), (0, -4)):
        b.set(tx + dx, G + 7 + dy, back - 1, "minecraft:white_wool")
    b.set(tx, G + 2, back - 1, "minecraft:yellow_wool")
    b.set(tx, G + 1, back - 1, "minecraft:yellow_wool")
    for (x, z) in ((int(cx - rx) + 2, int(cz - rz) + 1), (int(cx + rx) - 2, int(cz - rz) + 1), (int(cx - rx) + 2, int(cz + rz) - 1),
                   (int(cx + rx) - 2, int(cz + rz) - 1)):
        sx = -1 if x < cx else 1
        ox = outer_x(cells, z, sx)
        b.set(ox + sx, G + 1, z, "minecraft:brown_terracotta")
        b.set(ox + sx, G + 2, z, "minecraft:white_wool")
    for side in (-1, 1):
        wx = outer_x(cells, int(cz), side)
        facing = "east" if side > 0 else "west"
        face_on_plane(b, wx, G + 6, int(cz), facing, 3.6, only=("eye", "ring", "shine", "cheek"), depth=1)
        for z in (int(cz) - 6, int(cz) + 6):                          # windows with flower boxes
            x = outer_x(cells, z, side)
            for y in (G + 3, G + 4):
                b.set(x, y, z, "minecraft:white_stained_glass_pane", pane(True))
            b.set(x + side, G + 1, z, BLOEMBAK, {"seizoen": "lente"})
        for z in (int(cz) - 3, int(cz) + 3):
            x = outer_x(cells, z, side)
            b.set(x, G + 2, z, KS_FACE, {"facing": facing, "stemming": str(b.rng.randrange(4))})
            b.gezichten += 1
    face_on_plane(b, tx, G + 6, back, "north", 4.2, only=("eye", "ring", "shine", "cheek"), depth=1)
    for x in (tx - 8, tx - 7, tx + 7, tx + 8):                      # windows at the back, with flower boxes under them
        z = outer_z(cells, x, -1)
        for y in (G + 3, G + 4):
            b.set(x, y, z, "minecraft:white_stained_glass_pane", pane(False))
        b.set(x, G + 1, z - 1, BLOEMBAK, {"seizoen": "lente"})
    for x in (tx - 1, tx, tx + 1):                                 # the back door (hay goes in here)
        z = outer_z(cells, x, -1)
        for y in (G + 1, G + 2):
            b.set(x, y, z, LOG, {"axis": "y"})
        b.set(x, G + 3, z, LOG, {"axis": "x"})
    for (x, dz) in ((tx - 2, -1), (tx + 2, -1)):
        b.set(x, G + 1, outer_z(cells, x, -1) + dz, HOOI, {"axis": "y"})
    # --- inside: three cow stalls along the back, the counter, hay, the chest, lanterns ---------------------------------
    iz0 = min(z for (_, z) in inner)
    stall_front = iz0 + 6
    for i, x0 in enumerate((21, 28, 35)):
        x1 = x0 + 5
        for x in (x0, x1 + 1):
            if x == 42:
                continue
            for z in range(iz0, stall_front + 1):
                if (x, z) in inner:
                    b.set(x, G + 1, z, HEK)
        for x in range(x0 + 1, x1 + 1):
            if (x, stall_front) in inner:
                b.set(x, G + 1, stall_front, POORT if x == x0 + 3 else HEK,
                      {"facing": "south", "open": "false", "in_wall": "false", "powered": "false"} if x == x0 + 3 else None)
        b.set(x0 + 1, G + 1, iz0 + 1, VOERBAK, {"facing": "south", "voer": "2"})
        b.set(x0 + 4, G + 1, iz0, HOOI, {"axis": "y"})
        b.set(x0 + 5, G + 1, iz0, HOOI, {"axis": "y"})
        b.dier(x0 + 3, G + 1, iz0 + 3, "guhkoe", baby=(i == 1))
        b.lamp_ketting(x0 + 3, iz0 + 3, KOE_TOP)
    # the wall between x 42 and the east side: a hay corner and the chest
    b.set(42, G + 1, iz0 + 1, HOOI, {"axis": "y"})
    b.set(43, G + 1, iz0 + 2, HOOI, {"axis": "y"})
    b.set(42, G + 2, iz0 + 1, HOOI, {"axis": "y"})
    # Boerin Hooibaal behind her counter (west of the door), facing the door
    cz_ = front - 3
    for x in range(21, 26):
        b.set(x, G + 1, cz_, "minecraft:barrel" if x % 2 else "minecraft:composter",
              {"facing": "up", "open": "false"} if x % 2 else {"level": "0"})
    b.set(21, G + 2, cz_, "minecraft:potted_pink_tulip")
    b.npc(23, G + 1, cz_ - 2, "boerinneguh", "south")
    b.set(20, G + 1, cz_ - 2, "minecraft:chest", {"facing": "east", "type": "single", "waterlogged": "false"},
          {"id": "minecraft:chest", "LootTable": "guhs:chests/guhboerderij"})
    b.targets["de kist"] = (21, G + 1, cz_ - 2)
    # the east side of the aisle: voerbakken and a kippennestje with an egg (a little of everything to start with)
    b.set(40, G + 1, front - 2, VOERBAK, {"facing": "west", "voer": "4"})
    b.set(41, G + 1, front - 4, HOOI, {"axis": "y"})
    b.set(40, G + 1, front - 5, NEST, {"eieren": "1"})
    for (x, z) in ((25, front - 7), (31, front - 7), (38, front - 7), (hcx, front + 1), (31, front - 2)):
        b.lamp_ketting(x, z, KOE_TOP if z <= front else G + 11)
    b.targets["de koeienstallen"] = (31, G + 1, stall_front + 1)
    b.targets["de hooihoek"] = (41, G + 1, front - 2)
    b.deur = (hcx, G + 1, hz1 + 1)
    return cells, hz1


def pane(ns=True):
    return {"east": str(not ns).lower(), "west": str(not ns).lower(), "north": str(ns).lower(), "south": str(ns).lower(), "waterlogged": "false"}


# =====================================================================================================================
# de Schaapjesstal: a giant guhschaapje
# =====================================================================================================================
SCHAAP_C = (8.5, 10.5)
SCHAAP_R = (6.5, 7.5)
SCHAAP_TOP = G + 6


def schaapjesstal(b):
    cx, cz = SCHAAP_C
    rx, rz = SCHAAP_R
    rng = b.rng

    def wol(x, y, z):
        return "minecraft:white_wool" if rng.random() < 0.3 else WOLBLOK
    cells, inner, top = body(b, cx, cz, rx, rz, SCHAAP_TOP, None, HOOI, wol, 6, wall_fn=wol)
    for (x, z) in inner:
        b.set(x, G, z, HOOI, {"axis": "y"})
    front = max(z for (_, z) in cells)
    fx = int(round(cx))
    # the face (pink skin patch with window eyes) on the front, the door below it
    for du in range(-4, 5):
        for dv in range(-3, 4):
            if (du / 4.6) ** 2 + (dv / 3.8) ** 2 <= 1:
                z = outer_z(cells, fx + du, 1)
                if z is not None:
                    b.set(fx + du, G + 6 + dv, z, "minecraft:pink_wool")
    face_on_plane(b, fx, G + 6, front, "south", 3.8, only=("eye", "ring", "shine", "cheek", "nose"), depth=1)
    for x in range(fx - 1, fx + 2):
        for y in range(G + 1, G + 3):
            b.air(x, y, front, x, y, front)
            b.set(x, G, front, HOOI, {"axis": "y"})
    # ears on the sides of the head, little legs under the belly
    for side in (-1, 1):
        ex = fx + side * 6
        ez = outer_z(cells, ex, 1) or front - 1
        ear_disc(b, ex, G + 8, ez - 1, 1.8, "minecraft:pink_wool", "minecraft:magenta_wool")
        b.set(ex - side, G + 8, ez - 1, "minecraft:pink_wool")
    for x in (fx - 4, fx + 4):
        z = outer_z(cells, x, 1)
        b.set(x, G + 1, z + 1, "minecraft:pink_terracotta")
    for x in (fx - 4, fx + 4):
        z = outer_z(cells, x, -1)
        b.set(x, G + 1, z - 1, "minecraft:pink_terracotta")
    # a woolly tail at the back and a sleepy face there
    back = min(z for (_, z) in cells)
    b.set(fx, G + 4, back - 1, WOLBLOK)
    b.set(fx, G + 3, back - 1, WOLBLOK)
    # inside: a voerbak, a lantern, two schaapjes, hay
    iz0 = min(z for (_, z) in inner)
    b.set(fx - 3, G + 1, iz0, VOERBAK, {"facing": "south", "voer": "2"})
    b.set(fx + 3, G + 1, iz0, HOOI, {"axis": "y"})
    b.lamp_ketting(fx, int(cz) - 3, SCHAAP_TOP)
    b.lamp_ketting(fx, front - 3, SCHAAP_TOP)
    b.dier(fx - 2, G + 1, int(cz), "guhschaapje")
    b.dier(fx + 2, G + 1, int(cz) + 1, "guhschaapje", baby=True)
    b.targets["in de schaapjesstal"] = (fx, G + 1, int(cz))
    return cells, front


# =====================================================================================================================
# het Kippenhok: a giant knabbelkippetje
# =====================================================================================================================
KIP_C = (55.0, 10.0)
KIP_R = (6.0, 6.5)
KIP_TOP = G + 6


def kippenhok(b):
    cx, cz = KIP_C
    rx, rz = KIP_R
    rng = b.rng

    def geel(x, y, z):
        return "minecraft:yellow_wool" if rng.random() < 0.85 else "minecraft:yellow_terracotta"
    cells, inner, top = body(b, cx, cz, rx, rz, KIP_TOP, None, PLANK, geel, 6, p=2.6, wall_fn=geel)
    front = max(z for (_, z) in cells)
    fx = int(round(cx))
    face_on_plane(b, fx, G + 7, front, "south", 4.4, only=("eye", "ring", "shine", "cheek"), depth=1)
    for x in (fx - 1, fx, fx + 1):                                  # the beak over the door
        b.set(x, G + 5, front + 1, "minecraft:orange_terracotta")
    b.set(fx, G + 4, front + 1, "minecraft:orange_terracotta")
    for y in range(G + 1, G + 4):
        b.air(fx, y, front, fx, y, front)
    b.set(fx, G + 1, front, "minecraft:cherry_door", {"facing": "north", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    b.set(fx, G + 2, front, "minecraft:cherry_door", {"facing": "north", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    b.air(fx, G + 3, front, fx, G + 3, front)
    b.set(fx, G + 3, front, "minecraft:yellow_wool")
    # the comb on top, wings on the sides, feet in front
    tz = int(cz)
    ty = max(top.values())
    for (dz, dy) in ((-1, 0), (0, 0), (1, 0), (0, 1), (-1, 1)):
        b.set(fx, ty + dy, tz + dz, "minecraft:red_wool")
    for side in (-1, 1):
        wx = outer_x(cells, tz, side)
        for dz in range(-3, 3):
            for dy in range(0, 3 - abs(dz + 1) // 2):
                b.set(wx + side, G + 3 + dy, tz + dz, "minecraft:orange_wool" if dy == 0 else "minecraft:yellow_wool")
        ear_disc(b, fx + side * 4, ty - 1, tz - 1, 1.5, "minecraft:yellow_wool", "minecraft:pink_wool")
        b.set(fx + side * 4, ty - 2, tz - 1, "minecraft:yellow_wool")
    for x in (fx - 3, fx + 3):
        for (dx, dz) in ((0, 1), (0, 2), (-1, 2), (1, 2)):
            b.set(x + dx, G + 1, front + dz, "minecraft:orange_terracotta")
    # inside: a perch, nests, a voerbak, a lantern, kippetjes
    iz0 = min(z for (_, z) in inner)
    for x in (fx - 3, fx - 2, fx + 2, fx + 3):
        b.set(x, G + 1, iz0, NEST, {"eieren": "1" if x == fx + 2 else "0"})
    b.set(fx, G + 1, iz0, VOERBAK, {"facing": "south", "voer": "2"})
    b.lamp_ketting(fx, int(cz), KIP_TOP)
    for x in (fx - 1, fx + 1):
        b.dier(x, G + 1, int(cz) + 1, "knabbelkippetje")
    b.targets["in het kippenhok"] = (fx, G + 1, int(cz))
    return cells, front


# =====================================================================================================================
# fences, pastures, the yard, the moestuin, the molen, the entrance
# =====================================================================================================================
def hek_rand(b, x0, z0, x1, z1, gates=(), skip=None):
    """A fence along the edge of a rectangle (x0..x1, z0..z1), with fence gates at `gates` [(x, z, facing)]."""
    gate_at = {(x, z): f for (x, z, f) in gates}
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                if skip and skip(x, z):
                    continue
                if (x, z) in gate_at:
                    b.set(x, G + 1, z, POORT, {"facing": gate_at[(x, z)], "open": "false", "in_wall": "false", "powered": "false"})
                elif b.get(x, G + 1, z) is None:
                    b.set(x, G + 1, z, HEK)


def weitje(b, x0, z0, x1, z1, density=0.14):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if b.get(x, G, z) == GRAS and b.get(x, G + 1, z) is None and b.rng.random() < density:
                b.set(x, G + 1, z, b.rng.choice(["minecraft:short_grass", "minecraft:short_grass", "guhs:pluisgras"] + FLOWERS))


def pad(b, x0, z0, x1, z1):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for z in range(min(z0, z1), max(z0, z1) + 1):
            b.set(x, G, z, KLINK)
            b.air(x, G + 1, z, x, G + 2, z)


def grond(b):
    for x in range(W):
        for z in range(D):
            for y in range(G - 3, G):
                b.set(x, y, z, "minecraft:dirt")
            b.set(x, G, z, GRAS, {"snowy": "false"})


def schaapjeswei(b, stal_front):
    x0, z0, x1, z1 = 1, stal_front + 1, 16, 44
    hek_rand(b, x0, z0 - 1, x1, z1, gates=[(x1, 32, "east")], skip=lambda x, z: z == z0 - 1 and 3 <= x <= 14)
    # the stable's front wall closes the pasture at the top: fence from the stable's sides
    weitje(b, x0 + 1, z0, x1 - 1, z1 - 1)
    b.set(4, G + 1, 30, VOERBAK, {"facing": "east", "voer": "3"})
    b.set(3, G + 1, 38, HOOI, {"axis": "y"})
    b.set(3, G + 2, 38, HOOI, {"axis": "y"})
    for (x, z, baby) in ((6, 26, False), (11, 29, False), (8, 35, False), (12, 40, True)):
        b.air(x, G + 1, z, x, G + 2, z)
        b.dier(x, G + 1, z, "guhschaapje", baby=baby)
    b.targets["de schaapjeswei"] = (9, G + 1, 33)


def kippenren(b, hok_front):
    x0, z0, x1, z1 = 47, hok_front + 1, 62, 36
    hek_rand(b, x0, z0 - 1, x1, z1, gates=[(x0, 27, "west")], skip=lambda x, z: z == z0 - 1 and 50 <= x <= 60)
    for x in range(x0 + 1, x1):
        for z in range(z0, z1):
            if b.get(x, G + 1, z) is None and b.rng.random() < 0.2:
                b.set(x, G, z, "minecraft:coarse_dirt")
    b.set(60, G + 1, 30, VOERBAK, {"facing": "west", "voer": "3"})
    b.set(49, G + 1, 34, NEST, {"eieren": "0"})
    b.set(50, G + 1, 34, NEST, {"eieren": "0"})
    for (x, z, baby) in ((52, 25, False), (57, 28, False), (54, 32, True)):
        b.air(x, G + 1, z, x, G + 2, z)
        b.dier(x, G + 1, z, "knabbelkippetje", baby=baby)
    b.targets["de kippenren"] = (55, G + 1, 30)


def erf(b, koe_front):
    # paths: from the entrance to the barn door, and branches to the gates, the moestuin and the molen
    pad(b, 30, koe_front + 1, 33, 63)
    pad(b, 17, 31, 30, 33)
    pad(b, 34, 26, 46, 28)
    pad(b, 34, 48, 42, 50)
    pad(b, 16, 53, 30, 55)
    # the well (a cauldron of water under a little pluisdak roof)
    wx, wz = 24, 40
    for (x, z) in ((wx - 1, wz - 1), (wx + 1, wz - 1), (wx - 1, wz + 1), (wx + 1, wz + 1)):
        for y in range(G + 1, G + 4):
            b.set(x, y, z, KS_MUUR, {"up": "true"})
    for x in range(wx - 2, wx + 3):
        for z in range(wz - 2, wz + 3):
            b.set(x, G + 4, z, DAK_PLAAT, {"type": "bottom", "waterlogged": "false"})
    b.set(wx, G + 5, wz, KS_FACE, {"facing": "south", "stemming": "0"})
    b.gezichten += 1
    b.set(wx, G + 1, wz, "minecraft:water_cauldron", {"level": "3"})
    b.set(wx, G + 3, wz, LAMP, {"hanging": "true", "waterlogged": "false"})
    # a hay-bale guh: a big guh sitting in the yard, made of hay with a face, ears and a straw hat
    hx, hz = 39, 38
    for x in range(hx - 2, hx + 3):
        for z in range(hz - 2, hz + 2):
            for y in range(G + 1, G + 4):
                b.set(x, y, z, HOOI, {"axis": "y"})
    for x in range(hx - 1, hx + 2):
        for z in range(hz - 1, hz + 1):
            b.set(x, G + 4, z, HOOI, {"axis": "y"})
    b.set(hx - 1, G + 3, hz + 2, "minecraft:black_concrete")
    b.set(hx + 1, G + 3, hz + 2, "minecraft:black_concrete")
    b.set(hx, G + 2, hz + 2, "minecraft:pink_concrete")
    for x in (hx - 2, hx + 2):
        b.set(x, G + 4, hz - 1, "minecraft:yellow_terracotta")
        b.set(x, G + 5, hz - 1, "minecraft:yellow_terracotta")
    for x in range(hx - 1, hx + 2):
        b.set(x, G + 5, hz, "minecraft:yellow_carpet")
    b.set(hx, G + 5, hz - 1, "minecraft:red_wool")
    # benches and voerbakken, lamp posts
    b.set(27, G + 1, 44, BANK, {"facing": "north"})
    b.set(28, G + 1, 44, BANK, {"facing": "north"})
    b.set(20, G + 1, 36, VOERBAK, {"facing": "east", "voer": "1"})
    for (x, z) in ((29, 36), (34, 36), (29, 46), (34, 46), (29, 58), (34, 58), (20, 30), (44, 26), (35, 30)):
        b.lantaarnpaal(x, z)
    weitje(b, 18, 34, 28, 47, 0.08)
    weitje(b, 35, 30, 45, 46, 0.06)
    b.targets["het erf"] = (32, G + 1, 40)
    b.targets["de put"] = (wx, G + 1, wz + 2)


def moestuin(b):
    x0, z0, x1, z1 = 38, 44, 62, 62
    hek_rand(b, x0, z0, x1, z1, skip=lambda x, z: (z == z0 and 39 <= x <= 42) or (x == x0 and 48 <= z <= 51))
    pad(b, 39, 45, 61, 46)
    plants = ["knabbelplantje", "theekruid", "guhbloem"]
    for row, z in enumerate((48, 51, 54, 57, 60)):
        plant = plants[row % 3]
        for x in range(40, 61):
            if x in (47, 54):
                b.set(x, G, z, KLINK)
                continue
            groei = str(min(3, (x * 7 + row * 3) % 5))
            b.set(x, G + 1, z, BAK, {"facing": "south", "plant": plant, "groei": groei, "gewaterd": "false"})
        if z < 60:
            for x in range(39, 62):
                b.set(x, G, z + 1, KLINK)
                b.set(x, G, z + 2, KLINK)
    for x in (47, 54):
        for z in range(47, 62):
            b.set(x, G, z, KLINK)
    # bloempotten on the path's edge, a scarecrow guh in the middle
    for (x, z, plant) in ((40, 45, "guhbloem"), (43, 45, "theekruid"), (46, 45, "guhbloem"), (58, 45, "knabbelplantje"), (61, 45, "guhbloem")):
        b.set(x, G + 1, z, POT, {"facing": "south", "plant": plant, "groei": "3", "gewaterd": "false"})
    sx, sz = 54, 53
    b.set(sx, G + 1, sz, "minecraft:cherry_fence")
    b.set(sx, G + 2, sz, "minecraft:cherry_fence")
    b.set(sx, G + 3, sz, "minecraft:pink_wool")
    b.set(sx, G + 4, sz, "guhs:knuffelsteen_gezicht", {"facing": "south", "stemming": "2"})
    b.gezichten += 1
    for dx in (-1, 1):
        b.set(sx + dx, G + 3, sz, "minecraft:cherry_fence")
    b.set(sx, G + 5, sz, "minecraft:yellow_carpet")
    b.targets["de moestuin"] = (50, G + 1, 50)
    b.targets["achter in de moestuin"] = (57, G + 1, 59)


def molen(b):
    """A guh windmill: a round knuffelsteen tower with a guh face, a pluisdak cap with ears, and big sails."""
    cx, cz = 8, 55
    cells = [(x, z) for x in range(cx - 4, cx + 5) for z in range(cz - 4, cz + 5) if (x - cx) ** 2 + (z - cz) ** 2 <= 16.5]
    inner = {(x, z) for (x, z) in cells if (x - cx) ** 2 + (z - cz) ** 2 <= 8.5}
    top = G + 14
    for (x, z) in cells:
        for y in range(G - 3, G + 1):
            b.set(x, y, z, KS)
        for y in range(G + 1, top + 1):
            if (x, z) in inner:
                b.air(x, y, z, x, y, z)
            else:
                b.set(x, y, z, KS if (y - G) % 5 else "guhs:knuffelklinkers")
        b.set(x, G, z, PLANK if (x, z) in inner else KS)
        if (x, z) in inner:
            b.binnen.add((x, z))
    for (x, z) in cells:                                              # the cap: a pluisdak dome
        d = math.hypot(x - cx, z - cz) / 4.3
        hgt = int(round(3.5 * math.sqrt(max(0.0, 1 - d * d))))
        for y in range(top + 1, top + 2 + hgt):
            b.set(x, y, z, DAK)
    for side in (-1, 1):
        ear_disc(b, cx + side * 3, top + 4, cz - 1, 1.6, "minecraft:pink_wool", "minecraft:magenta_wool")
        b.set(cx + side * 3, top + 2, cz - 1, "minecraft:pink_wool")
    front = max(z for (x, z) in cells if x == cx)
    face_on_plane(b, cx, G + 9, front, "south", 3.4, only=("eye", "ring", "shine", "cheek", "nose"), depth=1)
    # the door and the sails (on the east side, facing the yard)
    for y in (G + 1, G + 2):
        for z in range(front - 2, front + 1):
            if (cx, z) not in inner:
                b.air(cx, y, z, cx, y, z)
    b.set(cx, G + 1, front, "minecraft:cherry_door", {"facing": "north", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    b.set(cx, G + 2, front, "minecraft:cherry_door", {"facing": "north", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    ex = max(x for (x, z) in cells if z == cz)
    hub = (ex + 1, top - 3, cz)
    b.set(*hub, "minecraft:stripped_cherry_log", {"axis": "x"})
    sx = hub[0] + 1
    b.set(sx, hub[1], hub[2], "minecraft:pink_concrete")
    for (dy, dz) in ((1, 0), (0, 1), (-1, 0), (0, -1)):          # four arms, each with its sail on one side (it turns!)
        py, pz = -dz, dy
        for k in range(1, 6):
            y, z = hub[1] + dy * k, hub[2] + dz * k
            b.set(sx, y, z, "minecraft:cherry_fence")
            if k >= 2:
                for w in (1, 2):
                    b.set(sx, y + py * w, z + pz * w, "minecraft:white_wool" if (k + w) % 2 else "minecraft:pink_wool")
    b.lamp_staand(cx - 1, G + 2, cz - 1)
    b.lamp_staand(cx + 1, G + 2, cz - 2)
    b.set(cx - 1, G + 1, cz - 1, HOOI, {"axis": "y"})
    b.set(cx + 1, G + 1, cz - 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.targets["in de guhmolen"] = (cx, G + 1, cz)
    pad(b, cx - 1, front + 1, cx + 1, front + 2)


def ingang(b):
    """The entrance arch (south): two knuffelsteen pillars with guh faces, a pluisdak arch and a big guh head on top."""
    x0, x1, z = 28, 35, 62
    for x in (x0, x0 + 1, x1 - 1, x1):
        for y in range(G + 1, G + 6):
            b.set(x, y, z, KS)
    for x in (x0, x1):
        b.set(x, G + 3, z + 1, KS_FACE, {"facing": "south", "stemming": str(x % 4)})
        b.gezichten += 1
    for x in range(x0, x1 + 1):
        b.set(x, G + 6, z, DAK)
        b.set(x, G + 7, z, DAK_PLAAT, {"type": "bottom", "waterlogged": "false"})
    for x in range(x0 + 2, x1 - 1):
        b.set(x, G + 5, z, DAK_PLAAT, {"type": "top", "waterlogged": "false"})
    hx = (x0 + x1) // 2
    for dx in range(-2, 3):
        for dy in range(0, 4):
            b.set(hx + dx, G + 7 + dy, z, "minecraft:pink_wool")
    b.set(hx - 1, G + 9, z + 1, "minecraft:black_concrete")
    b.set(hx + 1, G + 9, z + 1, "minecraft:black_concrete")
    b.set(hx - 1, G + 8, z + 1, "minecraft:light_blue_concrete")
    b.set(hx + 1, G + 8, z + 1, "minecraft:light_blue_concrete")
    b.set(hx, G + 8, z + 1, "minecraft:magenta_concrete")
    for dx in (-2, 2):
        b.set(hx + dx, G + 11, z, "minecraft:pink_wool")
        b.set(hx + dx, G + 12, z, "minecraft:magenta_wool")
    b.lamp_hang(x0 + 2, G + 4, z)
    b.lamp_hang(x1 - 2, G + 4, z)
    # the fence all round the farm (the entrance stays open)
    for x in range(W):
        for zz in (0, D - 1):
            if zz == D - 1 and x0 <= x <= x1:
                continue
            if b.get(x, G + 1, zz) is None:
                b.set(x, G + 1, zz, HEK)
    for zz in range(D):
        for x in (0, W - 1):
            if b.get(x, G + 1, zz) is None:
                b.set(x, G + 1, zz, HEK)
    for x in (x0 - 1, x1 + 1):
        b.set(x, G + 1, D - 1, BLOEMBAK, {"seizoen": "lente"})
    b.starts.append((31, G + 1, D - 1))
    b.starts.append((32, G + 1, D - 1))


def connect(b):
    """Fences, panes and walls join their neighbours."""
    for (x, y, z), (blk, props, nbt) in list(b.s.blocks.items()):
        if blk in (HEK, "minecraft:cherry_fence") or blk.endswith("_pane") or blk == KS_MUUR:
            p = dict(props)
            for d, (dx, dz) in FACING.items():
                n = b.get(x + dx, y, z + dz)
                join = n is not None and (solid(n) and "lantern" not in n and "voerbak" not in n and "nestje" not in n or n.endswith("_pane")
                                          or "fence" in n or n == KS_MUUR)
                if blk == KS_MUUR:
                    p[d] = "low" if join else "none"
                else:
                    p[d] = "true" if join else "false"
            if blk == KS_MUUR:
                p.setdefault("up", "true")
                p["waterlogged"] = "false"
            else:
                p["waterlogged"] = "false"
            b.s.blocks[(x, y, z)] = (blk, p, nbt)


def verlicht(b):
    """Where an inside spot is still dim: a pearlescent froglight (a pink glowing block) in the nearest wall."""
    for _ in range(60):
        light = light_map(b)
        reach = walk(b, b.starts)
        dark = sorted(p for p in reach if (p[0], p[2]) in b.binnen and light.get(p, 0) < 6)
        if not dark:
            return
        x, y, z = dark[0]
        best = None
        for r in range(1, 7):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    for yy in (G + 3, G + 2):
                        q = (x + dx, yy, z + dz)
                        blk = b.get(*q)
                        if blk and opaque(blk) and blk not in (KS_FACE,) and "glass" not in blk and any(
                                (n[0], n[2]) in b.binnen and b.get(*n) is None for n in neighbours(q) if n[1] == yy):
                            best = q
                            break
                    if best:
                        break
                if best:
                    break
            if best:
                break
        if not best:
            return
        b.set(*best, "minecraft:pearlescent_froglight", {"axis": "y"})


def anchor(b):
    b.set(CX, G - 1, CZ, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": "minecraft:dirt",
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


def build(h):
    b = Bouw(h)
    grond(b)
    koe_cells, koe_front = koeienschuur(b)
    stal_cells, stal_front = schaapjesstal(b)
    hok_cells, hok_front = kippenhok(b)
    schaapjeswei(b, stal_front)
    kippenren(b, hok_front)
    erf(b, koe_front)
    moestuin(b)
    molen(b)
    ingang(b)
    connect(b)
    verlicht(b)
    anchor(b)
    fp = [(x, z) for x in range(W) for z in range(D)]
    b.s.clear_above(fp, G + 1, top=H)
    return b


# =====================================================================================================================
# the geometry self-check
# =====================================================================================================================
def neighbours(p):
    x, y, z = p
    return ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1))


def walk(b, starts):
    get = b.s.get

    def standable(p):
        x, y, z = p
        if not (0 <= x < W and 0 <= z < D and 1 <= y < H - 1):
            return False
        if not passable(get(x, y, z)) or not passable(get(x, y + 1, z)):
            return False
        return solid(get(x, y - 1, z)) or get(x, y, z) == LADDER or get(x, y - 1, z) == LADDER

    seen = {p for p in starts if standable(p)}
    todo = deque(seen)
    while todo:
        x, y, z = todo.popleft()
        nxt = []
        for dx, dz in FACING.values():
            for dy in (1, 0, -1, -2, -3):
                n = (x + dx, y + dy, z + dz)
                if dy == 1 and not passable(get(x, y + 2, z)):
                    continue
                if dy < 0 and any(not passable(get(x + dx, y + k, z + dz)) for k in range(dy + 1, 2)):
                    continue
                # (you can't step up onto a fence or a wall: 1.5 high)
                if dy == 1 and any(t in (get(*(n[0], n[1] - 1, n[2])) or "") for t in ("fence", "_wall", "muur")):
                    continue
                if standable(n):
                    nxt.append(n)
                    break
        for n in nxt:
            if n not in seen:
                seen.add(n)
                todo.append(n)
    return seen


def light_map(b):
    level = {}
    todo = deque()
    for (x, y, z), (blk, props, _) in b.s.blocks.items():
        lv = LIGHTS.get(blk, 0)
        if lv:
            level[(x, y, z)] = lv
            todo.append((x, y, z))
    while todo:
        c = todo.popleft()
        lv = level[c] - 1
        if lv <= 0:
            continue
        for n in neighbours(c):
            if 0 <= n[0] < W and 0 <= n[1] < H and 0 <= n[2] < D and not opaque(b.get(*n)) and level.get(n, 0) < lv:
                level[n] = lv
                todo.append(n)
    return level


def check(b):
    problems = []
    s, get = b.s, b.get
    reach = walk(b, b.starts)
    b.walkable = len(reach)
    for name, p in b.targets.items():
        ok = p in reach or any((p[0] + dx, p[1] + dy, p[2] + dz) in reach for dx, dz in list(FACING.values()) + [(0, 0)] for dy in (-1, 0, 1))
        if not ok:
            problems.append(f"{name} {p} can't be reached on foot ({get(*p)} on {get(p[0], p[1] - 1, p[2])})")
    for (x, y, z) in b.floors:
        if not solid(get(x, y - 1, z)) or not passable(get(x, y, z)) or not passable(get(x, y + 1, z)):
            problems.append(f"someone at {(x, y, z)} doesn't stand free on a floor ({get(x, y - 1, z)}, {get(x, y, z)}, {get(x, y + 1, z)})")
    npcs = [e for e in s.entities if e[3]["id"] == "guhs:guh_npc"]
    if len(npcs) != 1 or npcs[0][3]["Kind"] != "boerinneguh":
        problems.append("exactly one NPC: Boerin Hooibaal")
    for soort, least in (("guhschaapje", 4), ("knabbelkippetje", 4), ("guhkoe", 3)):
        if b.dieren.get(soort, 0) < least:
            problems.append(f"too few {soort}: {b.dieren.get(soort, 0)}")
    chests = [(p, blk) for p, blk in s.blocks.items() if blk[0] == "minecraft:chest"]
    if len(chests) != 1 or chests[0][1][2].get("LootTable") != "guhs:chests/guhboerderij":
        problems.append("one chest with the farm's loot")
    for (x, y, z), (blk, props, _) in s.blocks.items():
        below, above = get(x, y - 1, z), get(x, y + 1, z)
        if blk == LAMP:
            support = above if props.get("hanging") == "true" else below
            if not solid(support) and not (support and any(t in support for t in ("_wall", "muur", "fence", "chain"))):
                problems.append(f"lantern at {(x, y, z)} hangs/stands on {support}")
        if blk in PLANTS or blk in (VOERBAK, NEST, BAK, POT, BLOEMBAK, BANK, "minecraft:water_cauldron", "minecraft:chest", "minecraft:barrel",
                                    "minecraft:composter") or blk.startswith("minecraft:potted_"):
            if not solid(below):
                problems.append(f"{blk} at {(x, y, z)} stands on {below}")
        if blk.endswith("_door") and props.get("half") == "lower" and (not (above or "").endswith("_door") or not solid(below)):
            problems.append(f"door at {(x, y, z)} is broken")
        if blk.endswith("_carpet") and not solid(below):
            problems.append(f"carpet at {(x, y, z)} on {below}")
        if blk == "minecraft:grass_block" and solid(above) and above not in ("minecraft:grass_block",):
            pass
    # nothing floats
    real = {p for p, (blk, _, _) in s.blocks.items() if blk != "minecraft:air"}
    seen = set()
    for p in real:
        if p in seen:
            continue
        comp, todo, grounded = [], [p], False
        seen.add(p)
        while todo:
            q = todo.pop()
            comp.append(q)
            if q[1] <= G:
                grounded = True
            for n in neighbours(q):
                if n in real and n not in seen:
                    seen.add(n)
                    todo.append(n)
        if not grounded:
            first = sorted(comp)[0]
            problems.append(f"{len(comp)} floating blocks from {first} ({get(*first)})")
    # the insides are lit
    light = light_map(b)
    dark = [p for p in reach if (p[0], p[2]) in b.binnen and light.get(p, 0) < 6]
    if dark:
        problems.append(f"{len(dark)} dark spots inside, e.g. {sorted(dark)[:5]}")
    # the anchor
    if get(CX, G - 1, CZ) != "minecraft:jigsaw":
        problems.append("no anchor jigsaw")
    # guh faces: lots of them
    if b.gezichten < 14:
        problems.append(f"only {b.gezichten} guh faces")
    return problems


if __name__ == "__main__":
    import types
    here = os.path.dirname(os.path.abspath(__file__))
    sys.path.insert(0, os.path.dirname(here))
    import make_structures as ms_
    h = types.SimpleNamespace(Structure=ms_.Structure, ms=ms_)
    bouw = build(h)
    pr = check(bouw)
    print("\n".join(pr) if pr else f"guhboerderij ok: {len(bouw.s.blocks)} blocks, {bouw.walkable} walkable spots, {bouw.gezichten} faces, "
                                    f"animals {bouw.dieren}")
