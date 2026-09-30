"""
3.0 (Guhverhalen), slice mewtwo: het kloon-eiland (the template guhs:kloon_eiland, one regio piece placed unrotated on the
Diepe Guhzee: verhaal_wereld.regio_structuur(..., "guhmension_zee", "zee", ...), the anchor block at y63, one above the water).

The island (template 80 x 76 x 80, template y 0 = world y 33, the anchor layer G = 30 = world y 63):
  - a rock pedestal from the sea floor (y ~34) up: stone, andesite, tuff, pink coral on its flanks; a rocky coast with two
    little sand coves, a steep cliff with pink terracotta and calcite bands up to the grassy plateau (knuffelgras, y 70)
  - the lab: the KOEPELHAL (a glass dome with quartz ribs, radius 12) with the cracked KLOONTANK in the middle (the block
    guhs:mewtwo_kloontank + 8 invisible guhs:mewtwo_tankwand parts; its BER draws the glass, the pink liquid and the cracks),
    consoles with guh-computers, test tubes and copper pipes, pink puddles of leaked knabbelsap
  - the rommelige KANTOORTJE of Professor Knabbelkloon (east wing): a desk full of papers, bookshelves, barrels, a brewing
    stand, a messy floor; the professor (NPC knabbelkloon) behind his desk
  - the TOREN (west) with a spiral staircase up to the ARENA on top (y 92, radius 12, with our own knabbelbal logo on the
    floor, stands, light pylons, the grote knabbelschaal for the big meal and the Guhtwo (story copy) in the middle)
  - the steiger (a wooden pier) with a little boothuisje on the south cove, where the Reisguh "Kloon-eiland" waits
  - the quest spots: 6 guhs:mewtwo_notitieplek (nummer 1..6) and 4 guhs:mewtwo_onderdelenkist (soort 1..4) all over the island,
    and 7 guhs:shuckle_plekje on the rocky coast (landdiertjes keeps 1-2 Sjokkels around each)
check() is the geometry self-check (SystemExit on problems): every quest spot once, reachable (a walkable spot next to it),
the tank and its parts, the copy, the professor, the Reisguh, nothing floating, the whole island inside the template and above
the sea floor; and (1.0.0, mewtwo_loop.py) a player's walk: from the steiger a flood fill over the standing spots (1.8 tall,
steps up to half a block, a full-block jump only with room above) reaches every note, crate, the professor, the knabbelschaal
and the arena, and the main route (steiger - beach - cliff stairs - koepelhal - tower - arena) needs no jump at all. The
spiral stairs are half steps (slab, block, slab...) and the arena floor opens over every step a head would bump into.
"""
import math
import random

from features import mewtwo_loop as loop

NAME = "kloon_eiland"
W = 80                  # template width/depth (x, z)
H = 76                  # template height
C = 40                  # the anchor (the island's middle) at template x/z C
Y0 = 33                 # world y of template y 0
G = 63 - Y0             # template y of the anchor layer (world y 63: the anchor block, one above the water)
REACH = 40              # how far the template reaches from its anchor (regio_structuur reach)
WATER = 62              # the top water block (world y)
FLOOR = 34              # the deep sea floor (world y)
PLAT = 70               # the plateau's grass (world y)
ARENA = 92              # the arena floor (world y)

DOME = (40, 44)         # the koepelhal's middle (x, z)
DOME_R = 12
TANK = (40, 71, 44)     # the kloontank controller (world y: it stands on the hall floor)
OFFICE = (53, 38, 62, 50)   # the office box (x0, z0, x1, z1)
TOWER = (21, 42)        # the tower's middle (x, z)
TOWER_R = 6
ARENA_R = 12
LOGO_DZ = 3             # the logo on the arena floor sits this far north of the tower's middle (the hatch of the stairs is south-east)

SEED = 20300901


def T(y):
    """template y of a world y"""
    return y - Y0


class Bouw:
    """The template with world-y coordinates (set/get/fill take world y)."""

    def __init__(self, h):
        self.h = h
        self.s = h.Structure((W, H, W))
        self.rng = random.Random(SEED)
        self.notities = {}      # nummer -> (x, y, z)
        self.kisten = {}        # soort -> (x, y, z)
        self.plekjes = []       # shuckle plekjes
        self.trap = []          # the spiral stairs: the step blocks, bottom to top

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, T(y), z, name, props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, T(y), z)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def air(self, x, y, z):
        self.set(x, y, z, "minecraft:air")

    def entity(self, x, y, z, nbt):
        self.s.entity(x, float(T(y)), z, nbt)


# =====================================================================================================================
# the island's shape
# =====================================================================================================================
def _golf(rng, n=6, amp=1.0):
    parts = [(k, rng.uniform(0, math.tau), amp * rng.uniform(0.35, 1.0) / k ** 0.6) for k in range(2, 2 + n)]
    return lambda a: sum(m * math.sin(k * a + p) for k, p, m in parts)


def hoek(x, z, mid=(C, C)):
    """The angle of (x, z) around mid in degrees: 0 east, 90 south, 180 west, 270 north."""
    return math.degrees(math.atan2(z - mid[1], x - mid[0])) % 360


def _hoekverschil(a, b):
    return abs((a - b + 180) % 360 - 180)


class Vorm:
    """The radius of the island per angle and height (world y)."""

    def __init__(self):
        rng = random.Random(SEED + 1)
        self.kust = _golf(rng, 6, 2.6)
        self.klif = _golf(rng, 5, 1.8)

    def r_kust(self, a):
        r = 31.0 + self.kust(math.radians(a))
        if _hoekverschil(a, 90) < 16:       # the south cove (the dock) reaches a little further out
            r += 2.0 * (1 - _hoekverschil(a, 90) / 16)
        return r

    def r_klif(self, a):
        r = 24.5 + self.klif(math.radians(a))
        if _hoekverschil(a, 90) < 14:       # the cove bites into the cliff (beach + stairs)
            r -= 3.5 * (1 - _hoekverschil(a, 90) / 14)
        if _hoekverschil(a, 180) < 25:      # the west: a rocky knoll under the arena's overhang
            r += 1.5
        return r

    def straal(self, a, y):
        """The island's radius at world y (None: nothing there)."""
        kust, klif = self.r_kust(a), self.r_klif(a)
        if y <= WATER:
            return kust + (WATER - y) * 0.13          # the pedestal widens towards the sea floor
        if y <= 64:
            return kust - (y - WATER) * 1.2           # the rocky coast / beach
        if y <= PLAT:
            t = (y - 64) / (PLAT - 64)
            return klif + (1 - t) * 1.4 - t * 0.3     # the cliff (a little steeper at the top)
        return None


def eiland(b, vorm):
    """The rock: pedestal, coast, cliff, plateau."""
    rng = b.rng
    top = {}
    for x in range(W):
        for z in range(W):
            d = math.hypot(x - C, z - C)
            a = hoek(x, z)
            for y in range(Y0, PLAT + 1):
                r = vorm.straal(a, y)
                if r is None or d > r:
                    continue
                top[(x, z)] = y
    for (x, z), ytop in top.items():
        d = math.hypot(x - C, z - C)
        a = hoek(x, z)
        for y in range(Y0, ytop + 1):
            rand = d > (vorm.straal(a, y) or 0) - 1.6      # the outer shell (what you see)
            if y == ytop and y >= PLAT:
                blk = "guhs:knuffelgras"
            elif y >= PLAT - 2 and ytop >= PLAT and not rand:
                blk = "minecraft:dirt"
            elif y <= 64 and y == ytop:
                # the coast: sand on the gentle bits, rocks elsewhere
                strand = _hoekverschil(a, 90) < 13 or _hoekverschil(a, 300) < 9
                blk = "minecraft:sand" if strand else rng.choice(["minecraft:stone", "minecraft:andesite", "minecraft:cobblestone",
                                                                  "minecraft:mossy_cobblestone", "minecraft:stone"])
            elif rand and y > WATER:
                # the cliff face: stone with bands of pink terracotta and calcite, a little tuff
                if y in (66, 67) and rng.random() < 0.8:
                    blk = "minecraft:pink_terracotta"
                elif y == 69 and rng.random() < 0.7:
                    blk = "minecraft:calcite"
                else:
                    blk = rng.choice(["minecraft:stone"] * 5 + ["minecraft:andesite"] * 2 + ["minecraft:tuff", "minecraft:cobblestone"])
            elif rand:
                # under water: stone with pink coral on the flanks, sand/gravel at the foot
                if y <= FLOOR + 1:
                    blk = rng.choice(["minecraft:sand", "minecraft:gravel", "minecraft:stone"])
                elif rng.random() < 0.07:
                    blk = rng.choice(["minecraft:brain_coral_block", "minecraft:bubble_coral_block", "guhs:kaaskoraalblok"])
                else:
                    blk = rng.choice(["minecraft:stone"] * 4 + ["minecraft:andesite", "minecraft:tuff"])
            else:
                blk = "minecraft:stone"
            b.set(x, y, z, blk)
    return top


def kust_details(b, vorm, top):
    """Boulders, sea plants and flowers; the shuckle plekjes on the rocky coast."""
    rng = random.Random(SEED + 2)
    for (x, z), ytop in list(top.items()):
        d = math.hypot(x - C, z - C)
        a = hoek(x, z)
        if ytop >= PLAT:
            if rng.random() < 0.05:
                b.set(x, ytop + 1, z, rng.choice(["guhs:roze_guhbloem", "guhs:kaasbloem", "minecraft:short_grass", "minecraft:allium"]))
        elif ytop <= WATER and d > vorm.r_kust(a) - 0.5 and rng.random() < 0.10:
            b.set(x, ytop + 1, z, rng.choice(["minecraft:seagrass", "minecraft:brain_coral", "minecraft:bubble_coral_fan", "minecraft:seagrass"]),
                  {"waterlogged": "true"} if True else None)
    # boulders on the coast (not in the coves)
    for i in range(14):
        a = rng.uniform(0, 360)
        if _hoekverschil(a, 90) < 22:
            continue
        r = vorm.r_kust(a) - 3.5
        cx, cz = C + r * math.cos(math.radians(a)), C + r * math.sin(math.radians(a))
        cy = top.get((int(round(cx)), int(round(cz))), 63) + 1
        rad = rng.uniform(1.2, 2.2)
        for x in range(int(cx - 3), int(cx + 4)):
            for z in range(int(cz - 3), int(cz + 4)):
                for y in range(cy, cy + 3):
                    if math.dist((x, y * 1.3, z), (cx, cy * 1.3, cz)) <= rad and (x, z) in top:
                        b.set(x, y, z, rng.choice(["minecraft:stone", "minecraft:andesite", "minecraft:mossy_cobblestone"]))
                        top[(x, z)] = max(top[(x, z)], y)
    # shuckle plekjes: on the rocks of the coast (never in a cove), a free spot on solid rock
    for a in (15, 55, 130, 170, 215, 255, 335):
        for r_off in (3.0, 2.0, 4.0, 1.0, 5.0):
            r = vorm.r_kust(a) - r_off
            x, z = int(round(C + r * math.cos(math.radians(a)))), int(round(C + r * math.sin(math.radians(a))))
            y = top.get((x, z))
            if y is None or y < 63 or y > 66:
                continue
            under = b.get(x, y, z) or ""
            if "sand" in under or b.get(x, y + 1, z) not in (None, "minecraft:air"):
                continue
            b.set(x, y + 1, z, "guhs:shuckle_plekje")
            b.plekjes.append((x, y + 1, z))
            break


# =====================================================================================================================
# the koepelhal
# =====================================================================================================================
QUARTZ = "minecraft:smooth_quartz"


def koepel(b):
    dx0, dz0 = DOME
    R = DOME_R
    # the floor (inside the dome) and the shell
    for x in range(dx0 - R - 1, dx0 + R + 2):
        for z in range(dz0 - R - 1, dz0 + R + 2):
            d2 = math.hypot(x - dx0, z - dz0)
            if d2 <= R + 0.3:
                ring = abs(d2 - 4.5) < 0.5 or abs(d2 - 8.5) < 0.5
                b.set(x, PLAT, z, "minecraft:purple_concrete" if ring else ("minecraft:white_concrete" if (x + z) % 2 else QUARTZ))
                for y in range(PLAT - 3, PLAT):
                    b.set(x, y, z, "minecraft:stone")
            for y in range(PLAT + 1, PLAT + R + 1):
                d3 = math.dist((x, y, z), (dx0, PLAT, dz0))
                if d3 <= R - 1.15:
                    b.air(x, y, z)
                elif d3 <= R + 0.25:
                    a = math.degrees(math.atan2(z - dz0, x - dx0)) % 360
                    rib = min(a % 45, 45 - a % 45) < (2.6 if y < PLAT + 9 else 5.5)
                    if y <= PLAT + 4:
                        # the wall: quartz with round windows between the ribs
                        raam = y in (PLAT + 2, PLAT + 3) and not rib
                        blk = "minecraft:pink_stained_glass" if raam else ("minecraft:quartz_pillar" if rib else QUARTZ)
                    elif y == PLAT + 5:
                        blk = "minecraft:purpur_block"                                # the ring at the foot of the glass
                    elif rib:
                        blk = "minecraft:quartz_block"
                    elif y >= PLAT + R - 1:
                        blk = "minecraft:purple_stained_glass"
                    else:
                        blk = "minecraft:pink_stained_glass" if (int(a // 15) + y) % 3 == 0 else "minecraft:white_stained_glass"
                    b.set(x, y, z, blk, {"axis": "y"} if blk == "minecraft:quartz_pillar" else None)
    b.set(dx0, PLAT + R, dz0, "minecraft:sea_lantern")                              # the top of the dome
    # the entrance (south) with a little porch, doors to the office (east) and the tower (west)
    for y in range(PLAT + 1, PLAT + 4):
        for x in range(dx0 - 1, dx0 + 2):
            for z in range(dz0 + R - 2, dz0 + R + 2):
                b.air(x, y, z)
        for z in range(dz0 - 1, dz0 + 2):
            for x in range(dx0 + R - 2, dx0 + R + 2):
                b.air(x, y, z)
            for x in range(dx0 - R - 2, dx0 - R + 2):
                b.air(x, y, z)
    for x in (dx0 - 2, dx0 + 2):
        for y in range(PLAT + 1, PLAT + 4):
            b.set(x, y, dz0 + R + 1, "minecraft:quartz_pillar", {"axis": "y"})
    for x in range(dx0 - 2, dx0 + 3):
        b.set(x, PLAT + 4, dz0 + R + 1, "minecraft:purpur_slab", {"type": "bottom", "waterlogged": "false"})
        b.set(x, PLAT + 4, dz0 + R + 2, "minecraft:purpur_slab", {"type": "bottom", "waterlogged": "false"})
        for z in range(dz0 + R - 1, dz0 + R + 3):
            b.set(x, PLAT, z, "guhs:knuffelklinkers")
    b.set(dx0 - 1, PLAT + 3, dz0 + R + 2, "guhs:lampion_roze", {"hanging": "true"})
    b.set(dx0 + 1, PLAT + 3, dz0 + R + 2, "guhs:lampion_roze", {"hanging": "true"})


def tank(b):
    """The kloontank in the middle of the hall: a 5 x 5 base, the tank (controller + parts), a cap with pipes to the dome."""
    tx, ty, tz = TANK
    for x in range(tx - 2, tx + 3):
        for z in range(tz - 2, tz + 3):
            edge = max(abs(x - tx), abs(z - tz)) == 2
            b.set(x, PLAT, z, "minecraft:amethyst_block" if edge else "minecraft:polished_deepslate")
    for x in range(tx - 1, tx + 2):
        for z in range(tz - 1, tz + 2):
            for y in range(ty, ty + 3):
                if (x, y, z) != (tx, ty, tz):
                    b.set(x, y, z, "guhs:mewtwo_tankwand")
            b.set(x, ty + 3, z, "minecraft:polished_deepslate")
    b.set(tx, ty, tz, "guhs:mewtwo_kloontank", {"facing": "south"})
    b.set(tx, ty + 4, tz, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    for y in range(ty + 5, PLAT + DOME_R):
        b.set(tx, y, tz, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    # copper pipes from the cap to the consoles (lightning rods lying down look like copper pipes)
    for dx, dz, facing in ((1, 0, "east"), (-1, 0, "west"), (0, -1, "north")):
        for i in range(2, 6):
            x, z = tx + dx * i, tz + dz * i
            b.set(x, ty + 3, z, "minecraft:lightning_rod", {"facing": facing, "powered": "false", "waterlogged": "false"})
        x, z = tx + dx * 6, tz + dz * 6
        for y in range(PLAT + 1, ty + 4):
            b.set(x, y, z, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    # leaked pink knabbelsap: puddles around the cracked tank
    rng = random.Random(SEED + 3)
    for x in range(tx - 4, tx + 5):
        for z in range(tz - 4, tz + 5):
            if 2 < math.hypot(x - tx, z - tz) <= 4.2 and rng.random() < 0.45 and b.get(x, PLAT + 1, z) in (None, "minecraft:air"):
                b.set(x, PLAT + 1, z, "minecraft:pink_carpet" if rng.random() < 0.7 else "minecraft:magenta_carpet")


def consoles(b):
    """Lab stations around the tank: a quartz table with a guh-computer, test tubes, buttons; lamps on the wall."""
    dx0, dz0 = DOME
    stations = []
    for a in (225, 270, 315, 20, 160):       # (never in front of the doors: south 90, east 0, west 180)
        r = 7.5
        x, z = int(round(dx0 + r * math.cos(math.radians(a)))), int(round(dz0 + r * math.sin(math.radians(a))))
        facing = {225: "south", 270: "south", 315: "south", 20: "west", 160: "east"}[a]
        stations.append((a, x, z, facing))
        side = (1, 0) if facing in ("south", "north") else (0, 1)
        for k in (-1, 0, 1):
            sx, sz = x + side[0] * k, z + side[1] * k
            b.set(sx, PLAT + 1, sz, "minecraft:smooth_quartz_slab", {"type": "top", "waterlogged": "false"})
        b.set(x, PLAT + 2, z, "guhs:mewtwo_computer", {"facing": facing})
        b.set(x + side[0], PLAT + 2, z + side[1], "guhs:mewtwo_reageerbuisjes", {"facing": facing})
        b.set(x - side[0], PLAT + 2, z - side[1], "minecraft:polished_blackstone_button" if a != 270 else "minecraft:air",
              {"face": "floor", "facing": facing, "powered": "false"} if a != 270 else None)
    # note 3 on the north station (next to the computer), in place of the button
    a, x, z, facing = stations[1]
    b.set(x - 1, PLAT + 2, z, "guhs:mewtwo_notitieplek", {"nummer": "3", "facing": facing})
    b.notities[3] = (x - 1, PLAT + 2, z)
    # note 4: on the floor beside the tank, in a pink puddle
    tx, ty, tz = TANK
    b.set(tx - 3, PLAT + 1, tz + 2, "guhs:mewtwo_notitieplek", {"nummer": "4", "facing": "east"})
    b.notities[4] = (tx - 3, PLAT + 1, tz + 2)
    # redstone lamps and a big screen on the inside of the wall
    for a in range(0, 360, 45):
        if a in (0, 90, 180):
            continue
        r = DOME_R - 1.6
        x, z = int(round(dx0 + r * math.cos(math.radians(a + 22.5)))), int(round(dz0 + r * math.sin(math.radians(a + 22.5))))
        if b.get(x, PLAT + 4, z) in (None, "minecraft:air"):
            b.set(x, PLAT + 4, z, "minecraft:redstone_lamp", {"lit": "true"})
    # a flower pot and a stool at the stations
    b.set(dx0 - 5, PLAT + 1, dz0 - 3, "minecraft:potted_allium")
    b.set(dx0 + 5, PLAT + 1, dz0 - 3, "guhs:potted_roze_guhbloem")


# =====================================================================================================================
# the office of Professor Knabbelkloon
# =====================================================================================================================
def kantoor(b):
    x0, z0, x1, z1 = OFFICE
    rng = random.Random(SEED + 4)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, PLAT, z, "minecraft:spruce_planks" if (x + z) % 5 else "minecraft:dark_oak_planks")
            wall = x in (x0, x1) or z in (z0, z1)
            for y in range(PLAT + 1, PLAT + 6):
                if wall:
                    corner = x in (x0, x1) and z in (z0, z1)
                    if corner:
                        blk = "minecraft:quartz_pillar"
                    elif y == PLAT + 5:
                        blk = "minecraft:purple_concrete"
                    elif y in (PLAT + 2, PLAT + 3) and ((z in (z0, z1) and (x - x0) % 3 == 1) or (x == x1 and (z - z0) % 3 == 1)):
                        blk = "minecraft:glass_pane"
                    else:
                        blk = "minecraft:white_concrete"
                    b.set(x, y, z, blk, {"axis": "y"} if blk == "minecraft:quartz_pillar" else None)
                else:
                    b.air(x, y, z)
    # the gable roof (along x), purpur stairs, with a guh-ear dormer at each end
    mid = (z0 + z1) / 2
    for i in range(0, int((z1 - z0) / 2) + 2):
        y = PLAT + 6 + i // 2 if False else PLAT + 6 + i // 2
        for x in range(x0 - 1, x1 + 2):
            zs = [z0 - 1 + i, z1 + 1 - i]
            for zz, facing in ((zs[0], "south"), (zs[1], "north")):
                if zz > mid + 0.5 and facing == "south" or zz < mid - 0.5 and facing == "north":
                    continue
                b.set(x, y, zz, "minecraft:purpur_stairs", {"facing": facing, "half": "bottom", "shape": "straight", "waterlogged": "false"})
                # fill the gable ends
                if x in (x0, x1):
                    for yy in range(PLAT + 6, y):
                        b.set(x, yy, zz, "minecraft:white_concrete")
    for x in range(x0 - 1, x1 + 2):
        b.set(x, PLAT + 6 + int((z1 - z0) / 2 + 1) // 2, int(mid), "minecraft:purpur_slab", {"type": "bottom", "waterlogged": "false"})
    # the ceiling
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            b.set(x, PLAT + 6, z, "minecraft:spruce_planks")
    # the door to the koepelhal (west wall, through the dome shell)
    for y in range(PLAT + 1, PLAT + 4):
        for x in range(x0 - 1, x0 + 1):
            b.air(x, y, int(DOME[1]))
            b.air(x, y, int(DOME[1]) - 1)
            b.air(x, y, int(DOME[1]) + 1)
    # bookshelves along the north wall
    for x in range(x0 + 1, x1):
        for y in range(PLAT + 1, PLAT + 4):
            if (x + y) % 4 == 0:
                b.set(x, y, z0 + 1, "minecraft:chiseled_bookshelf", {"facing": "south", "slot_0_occupied": "true", "slot_1_occupied": "false",
                                                                      "slot_2_occupied": "true", "slot_3_occupied": "true",
                                                                      "slot_4_occupied": "false", "slot_5_occupied": "true"})
            else:
                b.set(x, y, z0 + 1, "minecraft:bookshelf")
    # the desk (dark oak, in front of the bookshelves) with papers, a computer and note 1; the professor behind it
    dz = z0 + 4
    for x in range(x0 + 3, x0 + 7):
        b.set(x, PLAT + 1, dz, "minecraft:dark_oak_slab", {"type": "top", "waterlogged": "false"})
    b.set(x0 + 3, PLAT + 2, dz, "guhs:mewtwo_computer", {"facing": "north"})
    b.set(x0 + 4, PLAT + 2, dz, "guhs:mewtwo_notitieplek", {"nummer": "1", "facing": "south"})
    b.notities[1] = (x0 + 4, PLAT + 2, dz)
    b.set(x0 + 5, PLAT + 2, dz, "guhs:mewtwo_papieren", {"facing": "east"})
    b.set(x0 + 6, PLAT + 2, dz, "minecraft:brewing_stand", {"has_bottle_0": "true", "has_bottle_1": "false", "has_bottle_2": "true"})
    prof = (x0 + 4.5, PLAT + 1, dz - 1.5)
    b.prof = prof
    # note 2: behind a stack of books in the corner, parts crate 2 under the window
    b.set(x1 - 1, PLAT + 1, z0 + 1, "guhs:mewtwo_notitieplek", {"nummer": "2", "facing": "west"})
    b.notities[2] = (x1 - 1, PLAT + 1, z0 + 1)
    b.set(x0 + 2, PLAT + 1, z1 - 1, "guhs:mewtwo_onderdelenkist", {"soort": "2", "facing": "north"})
    b.kisten[2] = (x0 + 2, PLAT + 1, z1 - 1)
    # barrels, a lectern, a cauldron of knabbelsap, a pink couch (a bed), lanterns, messy papers everywhere
    for (x, z, y) in ((x1 - 1, z1 - 1, PLAT + 1), (x1 - 2, z1 - 1, PLAT + 1), (x1 - 1, z1 - 1, PLAT + 2), (x1 - 1, z1 - 2, PLAT + 1)):
        b.set(x, y, z, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(x1 - 1, PLAT + 1, z0 + 5, "minecraft:lectern", {"facing": "west", "has_book": "false", "powered": "false"})
    b.set(x1 - 1, PLAT + 1, z0 + 7, "minecraft:water_cauldron", {"level": "3"})
    b.set(x0 + 5, PLAT + 1, z1 - 1, "minecraft:pink_bed", {"facing": "east", "part": "foot", "occupied": "false"})
    b.set(x0 + 6, PLAT + 1, z1 - 1, "minecraft:pink_bed", {"facing": "east", "part": "head", "occupied": "false"})
    for (x, z) in ((x0 + 3, z0 + 3), (x1 - 3, z0 + 6), (x0 + 3, z1 - 3), (x1 - 3, z1 - 3)):
        b.set(x, PLAT + 5, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    for _ in range(14):
        x, z = rng.randint(x0 + 1, x1 - 1), rng.randint(z0 + 2, z1 - 1)
        if b.get(x, PLAT + 1, z) in (None, "minecraft:air") and abs(x - prof[0]) + abs(z - prof[2]) > 2:
            b.set(x, PLAT + 1, z, "guhs:mewtwo_papieren", {"facing": rng.choice(["north", "east", "south", "west"])})
    b.entity(prof[0], PLAT + 1, prof[2], {"id": "guhs:guh_npc", "Kind": "knabbelkloon", "PersistenceRequired": b.h.ms.Byte(1),
                                          "Rotation": b.h.ms.floats(0.0, 0.0)})
    return prof


# =====================================================================================================================
# the tower with the spiral stairs, the arena on top
# =====================================================================================================================
def toren(b):
    tx, tz = TOWER
    R = TOWER_R
    for x in range(tx - R - 1, tx + R + 2):
        for z in range(tz - R - 1, tz + R + 2):
            d = math.hypot(x - tx, z - tz)
            if d > R + 0.3:
                continue
            b.set(x, PLAT, z, "minecraft:polished_andesite" if d > 1.5 else "minecraft:quartz_pillar", {"axis": "y"} if d <= 1.5 else None)
            for y in range(PLAT - 4, PLAT):
                b.set(x, y, z, "minecraft:stone")
            for y in range(PLAT + 1, ARENA):
                if d > R - 0.9:
                    stripe = (y - PLAT) % 6 == 0
                    slit = (y - PLAT) % 6 == 3 and int(hoek(x, z, TOWER) // 30) % 3 == 1
                    b.set(x, y, z, "minecraft:purple_concrete" if stripe else ("minecraft:glass_pane" if slit else "minecraft:white_concrete"))
                elif d <= 1.2:
                    b.set(x, y, z, "minecraft:quartz_pillar", {"axis": "y"})
                else:
                    b.air(x, y, z)
    # the door to the hall (east) + the corridor
    for y in range(PLAT + 1, PLAT + 4):
        for x in range(tx + R - 1, DOME[0] - DOME_R + 2):
            for z in range(tz + 1, tz + 4):
                if z == tz + 2 or y < PLAT + 4:
                    b.air(x, y, z)
    for x in range(tx + R - 1, DOME[0] - DOME_R + 2):
        for z in range(tz, tz + 5):
            b.set(x, PLAT, z, "minecraft:purple_concrete" if z in (tz, tz + 4) else "minecraft:white_concrete")
            if z in (tz, tz + 4):
                for y in range(PLAT + 1, PLAT + 4):
                    b.set(x, y, z, "minecraft:white_concrete")
            b.set(x, PLAT + 4, z, "minecraft:smooth_quartz_slab", {"type": "bottom", "waterlogged": "false"})
    # the spiral stairs (1.0.0: half steps): one step per 22.5 degrees, each half a block higher (a bottom slab, then a full
    # block), from the tower floor (feet PLAT + 1) up to the last slab in the arena floor (feet ARENA + 0.5): you walk up
    # without jumping, 8 blocks of room per turn. b.trap: (feet, cells) bottom to top; the arena opens over the top steps
    # (arena(): every step whose head room would reach the arena floor). The first step lies north-west: from the door (east)
    # you walk clockwise under the higher steps to it; the top steps (the hatch) come out south-east, away from the logo
    start = 255.0
    steps = 2 * (ARENA - PLAT) - 1
    for j in range(steps):
        f = PLAT + 1 + 0.5 * (j + 1)
        a0 = start + j * 22.5
        cells = []
        for x in range(tx - R, tx + R + 1):
            for z in range(tz - R, tz + R + 1):
                d = math.hypot(x - tx, z - tz)
                if not (1.4 < d < R - 0.8):
                    continue
                if (hoek(x, z, TOWER) - a0) % 360 < 22.5:
                    cells.append((x, z))
        paars = j % 8 in (6, 7)
        for (x, z) in cells:
            if f % 1:
                b.set(x, int(f), z, "minecraft:purpur_slab" if paars else "minecraft:smooth_quartz_slab", {"type": "bottom", "waterlogged": "false"})
            else:
                b.set(x, int(f) - 1, z, "minecraft:purpur_block" if paars else "minecraft:smooth_quartz")
        b.trap.append((f, cells))
    # lanterns on the wall every half turn, above the head of whoever walks the step under them
    for j in range(0, steps, 8):
        f = b.trap[j][0]
        y = math.ceil(f + 1.8)
        a = start + j * 22.5 + 11
        for r in (R - 1.4, R - 1.1, R - 1.7):
            x, z = int(round(tx + r * math.cos(math.radians(a)))), int(round(tz + r * math.sin(math.radians(a))))
            wand = any(b.get(x + dx, y, z + dz) in ("minecraft:white_concrete", "minecraft:purple_concrete") for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if b.get(x, y, z) == "minecraft:air" and wand:
                b.set(x, y, z, "guhs:sterrenlantaarn")
                break
    # parts crate 3 on an inner step half way up, note 5 on the last full step (both on a full block, never on a slab)
    j3 = steps // 2 if steps // 2 % 2 else steps // 2 + 1
    f3, cells3 = b.trap[j3]
    inner = min(cells3, key=lambda c: math.hypot(c[0] - tx, c[1] - tz))
    b.set(inner[0], int(f3), inner[1], "guhs:mewtwo_onderdelenkist", {"soort": "3", "facing": "east"})
    b.kisten[3] = (inner[0], int(f3), inner[1])
    ftop, cellstop = b.trap[-2]
    inner = min(cellstop, key=lambda c: math.hypot(c[0] - tx, c[1] - tz))
    b.set(inner[0], int(ftop), inner[1], "guhs:mewtwo_notitieplek", {"nummer": "5", "facing": "north"})
    b.notities[5] = (inner[0], int(ftop), inner[1])


def hatch_steps(b):
    """The steps the arena floor must open over: a player on them, or stepping up half a block from them to the next one,
    would bump his head on it (feet + 0.5 + 1.8 > ARENA)."""
    return [k for k, (f, _) in enumerate(b.trap) if f + 0.5 + 1.8 > ARENA + 1e-6]


def _logo(x, z, cx, cz):
    """Our own logo on the arena floor: the knabbelbal (a round cheese knabbel with holes) with two guh ears on top."""
    dx, dz = x - cx, z - cz
    d = math.hypot(dx, dz)
    # the guh ears (rounded triangles above the ball, towards the north)
    for sx in (-1, 1):
        ex, ez = cx + sx * 2.3, cz - 3.9
        if math.hypot(x - ex, (z - ez) * 1.25) <= 1.6 and z < cz - 1.8:
            return "minecraft:pink_concrete"
    if d <= 3.7:
        for hx, hz, hr in ((-1.4, -1.0, 0.8), (1.3, 0.8, 1.0), (-0.5, 1.9, 0.6), (1.8, -1.6, 0.55)):
            if math.hypot(dx - hx, dz - hz) <= hr:
                return "minecraft:orange_concrete"
        return "minecraft:yellow_concrete" if d <= 3.0 else "minecraft:gold_block"
    return None


def arena(b):
    tx, tz = TOWER
    R = ARENA_R
    rng = random.Random(SEED + 5)
    top_steps = hatch_steps(b)
    hatch = set(c for k in top_steps for c in b.trap[k][1])
    uitgang = set(c for k in top_steps[-2:] for c in b.trap[k][1])   # (no railing next to the last two steps: the way out)
    for x in range(tx - R - 1, tx + R + 2):
        for z in range(tz - R - 1, tz + R + 2):
            d = math.hypot(x - tx, z - tz)
            if d > R + 0.3:
                continue
            if (x, z) in hatch and d < TOWER_R - 0.8:
                if b.get(x, ARENA, z) is None:   # (the last step's slab and note 5 stay)
                    b.air(x, ARENA, z)
                continue
            logo = _logo(x, z, tx, tz - LOGO_DZ)
            if d > R - 1.2:
                blk = "minecraft:purple_concrete"
            elif logo:
                blk = logo
            elif abs(math.hypot(x - tx, z - tz + LOGO_DZ) - 5.6) < 0.45 and d < R - 2:
                blk = "minecraft:white_concrete"
            elif abs(d - 8.0) < 0.5:
                blk = "minecraft:light_gray_concrete"
            else:
                blk = QUARTZ
            b.set(x, ARENA, z, blk)
            # the underside: corbels of quartz stairs round the rim, a thicker floor over the tower
            if d > TOWER_R:
                b.set(x, ARENA - 1, z, "minecraft:smooth_quartz_slab", {"type": "top", "waterlogged": "false"} ) if d > R - 1.2 else None
    # the rim: a low wall of glass with quartz posts, stands on the north side, light pylons
    for x in range(tx - R - 1, tx + R + 2):
        for z in range(tz - R - 1, tz + R + 2):
            d = math.hypot(x - tx, z - tz)
            if R - 0.9 < d <= R + 0.3:
                a = hoek(x, z, TOWER)
                post = int(a) % 30 < 5
                b.set(x, ARENA + 1, z, "minecraft:quartz_pillar" if post else "minecraft:purple_stained_glass_pane",
                      {"axis": "y"} if post else None)
    for a in range(0, 360, 60):
        x, z = int(round(tx + (R - 0.4) * math.cos(math.radians(a + 15)))), int(round(tz + (R - 0.4) * math.sin(math.radians(a + 15))))
        for y in range(ARENA + 1, ARENA + 4):
            b.set(x, y, z, "minecraft:quartz_pillar", {"axis": "y"})
        b.set(x, ARENA + 4, z, "guhs:sterrenlantaarn")
    # the stands (north arc): two rows of quartz stairs looking at the middle
    for x in range(tx - R, tx + R + 1):
        for z in range(tz - R, tz + R + 1):
            d = math.hypot(x - tx, z - tz)
            a = hoek(x, z, TOWER)
            if not (220 < a < 320):
                continue
            facing = "south"
            if 9.0 <= d < 10.0:
                b.set(x, ARENA + 1, z, "minecraft:quartz_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
            elif 10.0 <= d < R - 0.9:
                b.set(x, ARENA + 1, z, QUARTZ)
                b.set(x, ARENA + 2, z, "minecraft:quartz_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    # parts crate 4 on the top row of the stands
    for a in (268, 262, 274, 256, 280):
        x, z = int(round(tx + 10.4 * math.cos(math.radians(a)))), int(round(tz + 10.4 * math.sin(math.radians(a))))
        if b.get(x, ARENA + 2, z) == "minecraft:quartz_stairs" and b.get(x, ARENA + 3, z) in (None, "minecraft:air"):
            b.set(x, ARENA + 2, z, "guhs:mewtwo_onderdelenkist", {"soort": "4", "facing": "south"})
            b.kisten[4] = (x, ARENA + 2, z)
            break
    # the grote knabbelschaal on the south side (on a little round table of quartz), with lampions around
    sx, sz = tx, tz + 8
    b.set(sx, ARENA + 1, sz, "guhs:mewtwo_knabbelschaal")
    b.schaal = (sx, ARENA + 1, sz)
    for (x, z) in ((sx - 2, sz), (sx + 2, sz)):
        b.set(x, ARENA + 1, z, "minecraft:quartz_pillar", {"axis": "y"})
        b.set(x, ARENA + 2, z, "guhs:lampion_roze", {"hanging": "false"})
    # railing round the hatch (the stairs come up through the floor)
    for (x, z) in hatch - uitgang:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if any((n[0] + ex, n[1] + ez) in uitgang for ex in (-1, 0, 1) for ez in (-1, 0, 1)):
                continue
            if n not in hatch and b.get(n[0], ARENA, n[1]) not in (None, "minecraft:air") \
                    and math.hypot(n[0] - tx, n[1] - tz) < TOWER_R - 0.8 and b.get(n[0], ARENA + 1, n[1]) in (None, "minecraft:air"):
                b.set(n[0], ARENA + 1, n[1], "minecraft:white_stained_glass_pane")
    # the struts under the overhang (tower wall -> rim) and three slender pillars down to the rocks on the west
    for a in range(0, 360, 45):
        for i in range(0, R - TOWER_R + 1):
            r = TOWER_R + i
            x, z = int(round(tx + r * math.cos(math.radians(a)))), int(round(tz + r * math.sin(math.radians(a))))
            y = ARENA - 1 - (R - TOWER_R - i)
            if y > PLAT + 2:
                b.set(x, y, z, "minecraft:polished_andesite")
                b.set(x, y + 1, z, "minecraft:polished_andesite") if b.get(x, y + 1, z) in (None, "minecraft:air") else None
    for a in (150, 180, 210):
        x, z = int(round(tx + (R - 2) * math.cos(math.radians(a)))), int(round(tz + (R - 2) * math.sin(math.radians(a))))
        y = ARENA - 1
        while y > Y0 and b.get(x, y, z) in (None, "minecraft:air", "minecraft:smooth_quartz_slab"):
            b.set(x, y, z, "minecraft:quartz_pillar", {"axis": "y"})
            y -= 1
    # the Guhtwo (story copy: guhs_verhaal_guh = mewtwo; its spot is where it joins) north of the logo
    b.mewtwo = (tx + 0.5, ARENA + 1, tz - LOGO_DZ + 0.5)
    b.entity(b.mewtwo[0], ARENA + 1, b.mewtwo[2], {"id": "guhs:guh", "Variant": "mewtwo", "PersistenceRequired": b.h.ms.Byte(1),
                                                   "NeoForgeData": {"guhs_verhaal_guh": "mewtwo"},
                                                   "Rotation": b.h.ms.floats(0.0, 0.0)})


# =====================================================================================================================
# the path, the stairs down to the cove, the steiger and the boothuisje
# =====================================================================================================================
def haven(b, vorm, top):
    dx0, dz0 = DOME
    # the path from the hall's porch to the cliff edge
    z = dz0 + DOME_R + 3
    while top.get((dx0, z), 0) >= PLAT and z < W - 1:
        for x in range(dx0 - 1, dx0 + 2):
            b.set(x, PLAT, z, "guhs:knuffelklinkers")
            if b.get(x, PLAT + 1, z) not in (None, "minecraft:air"):
                b.air(x, PLAT + 1, z)
        if z % 3 == 0:
            b.set(dx0 - 2, PLAT + 1, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
        z += 1
    # stairs down the cliff (3 wide) to the cove's beach
    y = PLAT
    while y > 64:
        for x in range(dx0 - 1, dx0 + 2):
            b.set(x, y, z, "minecraft:stone_brick_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
            for yy in range(y + 1, y + 4):
                b.air(x, yy, z)
            for yy in range(y - 3, y):
                if b.get(x, yy, z) in (None, "minecraft:air"):
                    b.set(x, yy, z, "minecraft:stone_bricks")
        for x in (dx0 - 2, dx0 + 2):
            b.set(x, y, z, "minecraft:stone_bricks")
            b.set(x, y + 1, z, "minecraft:stone_brick_wall", {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none",
                                                           "waterlogged": "false"}) if y % 2 == 0 else None
            for yy in range(y - 4, y):
                if b.get(x, yy, z) in (None, "minecraft:air"):
                    b.set(x, yy, z, "minecraft:stone_bricks")
        y -= 1
        z += 1
    # the beach bit at the foot of the stairs (sand, a little wider). 1.0.0: it keeps the lowest stair (y 65, it used to clear
    # it: the stairs then started 1.5 blocks above the sand)
    for x in range(dx0 - 4, dx0 + 5):
        for zz in range(z - 1, z + 2):
            b.set(x, 64, zz, "minecraft:sand")
            for yy in range(63, 64):
                b.set(x, yy, zz, "minecraft:sandstone")
            if zz == z - 1 and abs(x - dx0) <= 1:
                continue
            for yy in range(65, 68):
                b.air(x, yy, zz)
    b.strand_z = z
    # the steiger: oak planks on posts from the beach to the edge of the template
    pz0 = z + 2
    for zz in range(pz0, W - 1):
        for x in range(dx0 - 1, dx0 + 2):
            b.set(x, 63, zz, "minecraft:spruce_planks")
            if b.get(x, 64, zz) not in (None, "minecraft:air"):
                b.air(x, 64, zz)
        if (zz - pz0) % 4 == 0:
            for x in (dx0 - 2, dx0 + 2):
                for yy in range(FLOOR, 65):
                    b.set(x, yy, zz, "minecraft:spruce_fence", {"north": "false", "south": "false", "east": "false", "west": "false",
                                                                "waterlogged": "false"})
                b.set(x, 65, zz, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # between beach and pier: planks over the shallow bit, with a half step (a slab) up to the sand (1.0.0)
    for zz in range(z + 1, pz0):
        for x in range(dx0 - 1, dx0 + 2):
            b.set(x, 63, zz, "minecraft:spruce_planks")
            if zz == z + 1:
                b.set(x, 64, zz, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
            else:
                b.air(x, 64, zz)
    b.steiger = (dx0, 64, pz0 + 3)
    # the boothuisje west of the stairs' foot
    hx0, hz0 = dx0 - 11, z - 3
    hx1, hz1 = hx0 + 6, hz0 + 5
    for x in range(hx0, hx1 + 1):
        for zz in range(hz0, hz1 + 1):
            for yy in range(Y0 + 20, 64):
                if b.get(x, yy, zz) in (None, "minecraft:air"):
                    b.set(x, yy, zz, "minecraft:stone")
            b.set(x, 64, zz, "minecraft:spruce_planks")
            wall = x in (hx0, hx1) or zz in (hz0, hz1)
            for yy in range(65, 69):
                if wall:
                    corner = x in (hx0, hx1) and zz in (hz0, hz1)
                    door = x == hx1 and zz in (hz0 + 2, hz0 + 3) and yy in (65, 66)
                    raam = yy == 66 and zz in (hz0, hz1) and x == hx0 + 3
                    if door:
                        b.air(x, yy, zz)
                    elif raam:
                        b.set(x, yy, zz, "minecraft:glass_pane")
                    else:
                        b.set(x, yy, zz, "minecraft:stripped_spruce_log" if corner else "minecraft:spruce_planks",
                              {"axis": "y"} if corner else None)
                else:
                    b.air(x, yy, zz)
    for i in range(0, 4):
        for x in range(hx0 - 1, hx1 + 2):
            for zz, facing in ((hz0 - 1 + i, "south"), (hz1 + 1 - i, "north")):
                b.set(x, 69 + i, zz, "minecraft:pink_terracotta" if i == 3 else "minecraft:spruce_stairs",
                      None if i == 3 else {"facing": facing, "half": "bottom", "shape": "straight", "waterlogged": "false"})
            if i < 3:
                for zz in range(hz0 + i, hz1 + 1 - i):
                    if x in (hx0, hx1):
                        b.set(x, 69 + i, zz, "minecraft:spruce_planks")
    # clear the rock above the hut's roof line on its back side (the cliff)
    for x in range(hx0 - 1, hx1 + 2):
        for zz in range(hz0 - 1, hz1 + 2):
            for yy in range(69, 74):
                cur = b.get(x, yy, zz)
                if cur and cur not in ("minecraft:spruce_stairs", "minecraft:pink_terracotta", "minecraft:spruce_planks") and yy >= 69 + max(0, min(zz - hz0 + 1, hz1 + 1 - zz)):
                    b.air(x, yy, zz)
    b.set(hx0 + 1, 65, hz0 + 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(hx0 + 1, 66, hz0 + 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    b.set(hx0 + 2, 65, hz0 + 1, "minecraft:barrel", {"facing": "north", "open": "false"})
    b.set(hx0 + 1, 65, hz1 - 1, "guhs:mewtwo_onderdelenkist", {"soort": "1", "facing": "east"})
    b.kisten[1] = (hx0 + 1, 65, hz1 - 1)
    b.set(hx1 - 2, 65, hz0 + 1, "guhs:mewtwo_notitieplek", {"nummer": "6", "facing": "south"})
    b.notities[6] = (hx1 - 2, 65, hz0 + 1)
    for x in range(hx0 + 1, hx1):
        for zz in range(hz0 + 1, hz1):
            b.set(x, 69, zz, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    b.set(hx0 + 3, 68, hz0 + 2, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    # a walk from the hut's door to the stairs
    for x in range(hx1 + 1, dx0 - 1):
        for zz in (hz0 + 2, hz0 + 3):
            b.set(x, 64, zz, "minecraft:spruce_planks")
            for yy in range(65, 68):
                b.air(x, yy, zz)


# =====================================================================================================================
# a bit of green on the plateau
# =====================================================================================================================
def tuin(b, top):
    rng = random.Random(SEED + 6)
    # a path of klinkers round the koepelhal (to the office, the tower and the porch), with a lampion now and then
    dx0, dz0 = DOME
    for x in range(dx0 - 16, dx0 + 17):
        for z in range(dz0 - 16, dz0 + 17):
            d = math.hypot(x - dx0, z - dz0)
            if 13.3 <= d <= 14.7 and top.get((x, z), 0) >= PLAT and b.get(x, PLAT, z) == "guhs:knuffelgras"                     and b.get(x, PLAT + 1, z) in (None, "minecraft:air", "guhs:roze_guhbloem", "guhs:kaasbloem", "minecraft:short_grass",
                                                   "minecraft:allium"):
                b.set(x, PLAT, z, "guhs:knuffelklinkers")
                b.air(x, PLAT + 1, z)
    for a in range(10, 360, 40):
        x, z = int(round(dx0 + 15.6 * math.cos(math.radians(a)))), int(round(dz0 + 15.6 * math.sin(math.radians(a))))
        if top.get((x, z), 0) >= PLAT and b.get(x, PLAT + 1, z) in (None, "minecraft:air") and b.get(x, PLAT, z) == "guhs:knuffelgras":
            b.set(x, PLAT + 1, z, "minecraft:spruce_fence", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
            b.set(x, PLAT + 2, z, "guhs:lampion_roze", {"hanging": "false"})
    # flower boxes under the office windows (outside), and a little chimney on its roof
    x0, z0, x1, z1 = OFFICE
    for x in range(x0 + 1, x1):
        if (x - x0) % 3 == 1:
            for z, facing in ((z0 - 1, "north"), (z1 + 1, "south")):
                if b.get(x, PLAT + 1, z) in (None, "minecraft:air"):
                    b.set(x, PLAT + 1, z, "minecraft:spruce_trapdoor", {"facing": facing, "half": "top", "open": "false", "powered": "false",
                                                                       "waterlogged": "false"})
                    b.set(x, PLAT + 2, z, rng.choice(["guhs:potted_roze_guhbloem", "minecraft:potted_allium", "guhs:potted_kaasbloem"]))
    for y in range(PLAT + 6, PLAT + 11):
        b.set(x1 - 2, y, z0 + 2, "minecraft:bricks")
    b.set(x1 - 2, PLAT + 11, z0 + 2, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for (cx, cz) in ((30, 30), (52, 57), (29, 57), (58, 30), (47, 27)):
        if top.get((cx, cz), 0) < PLAT or b.get(cx, PLAT + 1, cz) not in (None, "minecraft:air"):
            continue
        # a little guh blossom bush: a stem and a round crown of pink leaves
        for y in range(PLAT + 1, PLAT + 3):
            b.set(cx, y, cz, "minecraft:cherry_log", {"axis": "y"})
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                for y in range(PLAT + 3, PLAT + 6):
                    if math.dist((x, y * 1.2, z), (cx, (PLAT + 4) * 1.2, cz)) <= 2.4 and b.get(x, y, z) in (None, "minecraft:air"):
                        b.set(x, y, z, "minecraft:cherry_leaves", {"distance": "1", "persistent": "true", "waterlogged": "false"})
    for (x, z) in ((36, 58), (44, 58), (48, 52), (32, 52), (50, 36), (30, 36)):
        if top.get((x, z), 0) >= PLAT and b.get(x, PLAT + 1, z) in (None, "minecraft:air"):
            b.set(x, PLAT + 1, z, "guhs:lampion_roze", {"hanging": "false"})


# =====================================================================================================================
# the Reisguh, the anchor, air above
# =====================================================================================================================
def afwerking(b, top):
    from features import reisguh_plek
    h = b.h
    sx, sy, sz = b.steiger
    b.reisguh = reisguh_plek.zet(b.s, [(x, T(sy), z) for (x, _, z) in [(p[0], 0, p[2]) for p in reisguh_plek.rondom(sx, T(sy), sz)]],
                                 "Kloon-eiland", 180.0, h.ms.Byte, h.ms.floats, "(kloon_eiland)")
    # the anchor: in the middle of the island on the ground layer G (world y63: deep inside the rock)
    b.s.set(C, G, C, "minecraft:jigsaw", {"orientation": "up_north"},
            {"id": "minecraft:jigsaw", "name": f"guhs:{NAME}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
             "final_state": "minecraft:stone", "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    # air above the island (so nothing grows into the lab), only where there is island
    for (x, z), ytop in top.items():
        if ytop < WATER:
            continue
        for y in range(ytop + 1, Y0 + H):
            if b.get(x, y, z) is None:
                b.air(x, y, z)


# =====================================================================================================================
# the self-check
# =====================================================================================================================
SOLID_NOT = ("air", "carpet", "pane", "lantern", "lampion", "button", "chain", "rod", "flower", "bloem", "grass", "allium",
             "seagrass", "coral", "notitieplek", "papieren", "plekje", "tankwand", "fence", "sapling", "potted", "bed", "slab",
             "stairs", "brewing", "cauldron", "lectern", "computer", "reageerbuis", "onderdelenkist", "knabbelschaal", "wall")


def _vast(name):
    if not name or name == "minecraft:air":
        return False
    kort = name.split(":")[-1]
    return not any(t in kort for t in SOLID_NOT) or kort.endswith("stairs") or kort.endswith("_slab")


def _staanbaar(s, x, y, z):
    """Can a player stand in (x, y, z) (template coords)? floor below, two free blocks."""
    below = s.get(x, y - 1, z)
    free = lambda n: n is None or n == "minecraft:air" or any(t in n for t in ("carpet", "papieren", "flower", "bloem", "short_grass"))  # noqa: E731
    return below not in (None, "minecraft:air") and "tankwand" not in (below or "") and free(s.get(x, y, z)) and free(s.get(x, y + 1, z))


def check(b):
    s = b.s
    problems = []
    if sorted(b.notities) != [1, 2, 3, 4, 5, 6]:
        problems.append(f"notes: {sorted(b.notities)}")
    if sorted(b.kisten) != [1, 2, 3, 4]:
        problems.append(f"parts crates: {sorted(b.kisten)}")
    counts = {}
    for (x, y, z), (blk, props, _) in s.blocks.items():
        counts[blk] = counts.get(blk, 0) + 1
    if counts.get("guhs:mewtwo_notitieplek") != 6 or counts.get("guhs:mewtwo_onderdelenkist") != 4:
        problems.append(f"spots in the template: {counts.get('guhs:mewtwo_notitieplek')} notes, {counts.get('guhs:mewtwo_onderdelenkist')} crates")
    if counts.get("guhs:mewtwo_kloontank") != 1 or counts.get("guhs:mewtwo_tankwand") != 26:
        problems.append(f"the tank: {counts.get('guhs:mewtwo_kloontank')} controller, {counts.get('guhs:mewtwo_tankwand')} parts")
    if counts.get("guhs:mewtwo_knabbelschaal") != 1:
        problems.append("the knabbelschaal")
    if not 5 <= counts.get("guhs:shuckle_plekje", 0) <= 8:
        problems.append(f"shuckle plekjes: {counts.get('guhs:shuckle_plekje', 0)} (5-8)")
    # every spot can be reached: a free standing spot next to it (or it lies on the floor you walk on)
    for kind, spots in (("note", b.notities), ("crate", b.kisten)):
        for n, (x, y, z) in spots.items():
            ty = T(y)
            near = [(x + dx, ty + dy, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1) for dy in (-1, 0) if (dx, dz) != (0, 0)]
            if not any(_staanbaar(s, *p) for p in near):
                problems.append(f"{kind} {n} at {(x, y, z)} can't be reached")
    # entities: the copy, the professor, the Reisguh
    ids = [e[3].get("id") + ":" + str(e[3].get("Kind", e[3].get("Variant", ""))) for e in s.entities]
    for want in ("guhs:guh:mewtwo", "guhs:guh_npc:knabbelkloon", "guhs:guh_npc:reisguh"):
        if ids.count(want) != 1:
            problems.append(f"entity {want}: {ids.count(want)}")
    for (ex, ey, ez, nbt) in s.entities:
        if not _staanbaar(s, int(ex), int(ey), int(ez)):
            problems.append(f"{nbt.get('id')} {nbt.get('Kind', '')} at {(ex, ey + Y0, ez)} doesn't stand on a floor")
    # the stairs: each half step can be walked onto from the one before (no jump, room for the head)
    L = loop.Loop(s, Y0)
    for k in range(1, len(b.trap)):
        (f0, cells0), (f1, cells1) = b.trap[k - 1], b.trap[k]
        if not cells1:
            problems.append(f"stairs: no step {k}")
            continue
        if not any(abs(x0 - x1) + abs(z0 - z1) == 1 and L.stap(x0, z0, f0, x1, z1, f1, 0.5)
                   and f0 in L.standing(x0, z0, f0, 1) and f1 in L.standing(x1, z1, f1, 1)
                   for (x0, z0) in cells0 for (x1, z1) in cells1):
            problems.append(f"stairs: step {k} (feet {f1}) can't be walked onto from the one below")
    # walkability (mewtwo_loop): from the steiger (where the Reisguh drops you) a player reaches every quest spot, the
    # professor, the knabbelschaal and the arena; and the main route (steiger - beach - cliff stairs - path - koepelhal -
    # corridor - spiral stairs - arena) is walked without a single jump
    seen = L.reachable(b.steiger)
    doelen = {f"note {n}": p for n, p in b.notities.items()}
    doelen.update({f"crate {n}": p for n, p in b.kisten.items()})
    doelen["the knabbelschaal"] = b.schaal
    doelen["the professor"] = (int(b.prof[0]), int(b.prof[1]), int(b.prof[2]))
    for naam, p in doelen.items():
        if not L.spot_reached(seen, p):
            problems.append(f"walk: {naam} at {p} can't be reached from the steiger")
    mx, my, mz = int(b.mewtwo[0]), int(b.mewtwo[1]), int(b.mewtwo[2])
    if not L.feet_at(seen, mx, mz, my):
        problems.append(f"walk: the arena (the Guhtwo's spot {(mx, my, mz)}) can't be reached from the steiger")
    dx0, dz0 = DOME
    tx, tz = TOWER

    def route(x, z):
        return (abs(x - dx0) <= 1 and z >= dz0 + DOME_R - 2                                      # porch, path, stairs, beach, pier
                or math.hypot(x - dx0, z - dz0) <= DOME_R - 1                                  # the koepelhal
                or tx <= x <= dx0 - DOME_R + 2 and tz <= z <= tz + 4                           # the corridor
                or math.hypot(x - tx, z - tz) <= ARENA_R - 1)                                  # the tower and the arena
    lopen = L.reachable(b.steiger, allowed=route, max_up=0.5)
    if not L.feet_at(lopen, mx, mz, my):
        hoogste = max(f for (_, _, f) in lopen) / 2
        problems.append(f"walk: the route steiger -> arena needs a jump or a climb (got up to feet {hoogste})")
    b.loop = (len(seen), len(lopen))
    # the anchor
    if s.get(C, G, C) != "minecraft:jigsaw":
        problems.append("the anchor jigsaw")
    # nothing floating above the water (a block with no neighbour at all)
    for (x, y, z), (blk, props, _) in s.blocks.items():
        if blk == "minecraft:air" or y + Y0 <= WATER + 1:
            continue
        if all(s.get(x + dx, y + dy, z + dz) in (None, "minecraft:air") for dx, dy, dz in
               ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))):
            problems.append(f"a floating {blk} at {(x, y + Y0, z)}")
    # the island stays inside the template and its reach
    for (x, y, z) in s.blocks:
        if math.hypot(x - C, z - C) > REACH + 16:
            problems.append(f"block too far out at {(x, y, z)}")
            break
    if problems:
        raise SystemExit("kloon_eiland check failed:\n  " + "\n  ".join(problems[:40]))
    return counts


def build(h):
    b = Bouw(h)
    vorm = Vorm()
    top = eiland(b, vorm)
    kust_details(b, vorm, top)
    koepel(b)
    tank(b)
    consoles(b)
    kantoor(b)
    toren(b)
    arena(b)
    haven(b, vorm, top)
    tuin(b, top)
    afwerking(b, top)
    counts = check(b)
    b.s.save(NAME)
    h.w(f"{h.D}/worldgen/template_pool/{NAME}/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{NAME}",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    print(f"mewtwo: kloon_eiland ok ({len(b.s.blocks)} blocks, {len(b.plekjes)} shuckle plekjes, {len(b.trap)} half steps, "
          f"walk check ok: {b.loop[0]} standing spots reached, {b.loop[1]} on the route without jumping, "
          f"notes {sorted(b.notities)}, crates {sorted(b.kisten)})")
    return f"guhs:{NAME}/start", b
