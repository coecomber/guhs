"""
1.4.2 Het Knabbelpark - the templates (make_structures.Structure) and the pools of the jigsaw park, and the
PLEKJES API for whoever adds more plekjes (the follow-up slice park-plekjes; reports/slice_park.md "Plekjes API").

The park grows like a vanilla village (Java: feature/park/ParkStructure):

  start          knabbelpark/plein       the fountain plaza (rigid): the one Reisguh "Knabbelpark" on the island in the middle of
                                         the round fountain, lawns, flower beds, blossom trees, benches, lanterns; four exits
  required       knabbelpark/laan_*      three short lanes (terrain_matching) from the plaza's N/E/W exits, each to ONE
                                         required piece (single-element pools, higher selection/placement priority: they
                                         go first, nothing random can take their room):
                 knabbelpark/beeldentuin   the three statues on their plinths with their plaques
                 knabbelpark/hertenkooi    the paddock with three Hertguhtjes, the kooi-verzorger-guh by her hay bales
                 knabbelpark/parkwachter   the parkwachter-guh's hut with its counter, a notice board
  random walks   knabbelpark/paden       terrain_matching park walks: straight (long, short), bend, T, crossing, a little
                                         roundabout; every walk is a park walk by itself: the path in the middle, flower
                                         borders, hedges, lantern posts, benches, here and there a litter bin; the
                                         straight ones have side connectors for plekjes
                 knabbelpark/eind        the walks' fallback (also at the last depth): a dead end with a bench, or a ring of
                                         flowers
  plekjes        knabbelpark/plekjes     rigid nature spots on the walks' sides: bloemenweide, rustplekje, bosje,
                                         bloesemboom (and an empty element: now and then a side stays open)

Conventions (every piece keeps them, the Java relies on them):
  - rigid pieces (plaza, required pieces, plekjes): the foundation layer at y = 0, the ground (grass, paving) at y = G = 1,
    you walk at y = G + 1 = 2; every jigsaw sits at walk level y = 2 on the template's edge, facing out. The jigsaw
    placement then puts the ground layer exactly on the surface (start: ground_level_delta G + 1 = 2, written as a
    guhs:grond_single_pool_element), the terrain meets the piece at its ground.
  - walks (terrain_matching): the path layer at y = 0 (it replaces the surface block, column by column), the decoration
    at y = 1..3, every jigsaw at y = 1 (vanilla's street convention), final state structure_void.
  - the path is minecraft:dirt_path (over water: minecraft:cherry_planks, a little bridge: the processor PAD_PROCESSOR);
    ParkPaden.java smooths steps of 2+ between path blocks into 1-block steps, so NO rigid piece may use cherry planks,
    and a rigid piece's entrance and exit cells are dirt_path (the walk next to it then knows the piece's floor).
  - connectors: walks: name = target = guhs:park_pad, pool guhs:knabbelpark/paden; a walk's side connector: name
    guhs:park_zij, target guhs:park_plek, pool guhs:knabbelpark/plekjes; a plekje's entrance: name guhs:park_plek, on
    its SOUTH edge in the middle, at y = 2, facing south (the generator turns it to the walk).
  - nature first (DESIGN 9a): lawns of grass with pink knuffelgras patches, flowers and pink plants everywhere, hedges,
    blossom and pluizen trees; the walks carry their own street furniture (park_bankje, lantern posts = two cherry fence
    posts + guhs:park_lantaarn, guhs:park_prullenbak).
"""
import math
import random

from . import wereld

G = 1                  # the ground layer of a rigid piece (foundation at 0, walk at G + 1)
LOOP = G + 1           # walk level of a rigid piece = the y of its jigsaws

PAD = "minecraft:dirt_path"
BRUG = "minecraft:cherry_planks"
GRAS = "minecraft:grass_block"
ROZE_GRAS = "guhs:knuffelgras"
GROND = "minecraft:dirt"
KLINKERS = "guhs:knuffelklinkers"
HEG = "guhs:doolhofheg"
PAAL = "minecraft:cherry_fence"
LAMP = "guhs:park_lantaarn"
BANK = "guhs:park_bankje"
BAK = "guhs:park_prullenbak"
BLOESEM_STAM, BLOESEM_BLAD = "guhs:guhbloesem_log", "guhs:guhbloesem_leaves"
PLUIS_STAM, PLUIS_BLAD = "guhs:pluizenboom_stam", "guhs:pluizenboom_bladeren"
BLOEMEN = ["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:guhoortjes", "guhs:kaasbloem", "guhs:oogbloempje", "minecraft:pink_tulip",
           "minecraft:allium", "minecraft:lily_of_the_valley", "minecraft:white_tulip", "guhs:roze_guhbloem", "guhs:knabbelroos"]
PLANTJES = ["guhs:roze_gras", "guhs:pluisgras", "minecraft:short_grass", "guhs:roze_gras"]
AIR, VOID = "minecraft:air", "minecraft:structure_void"

# the connectors (feature/park/ParkPlekjes.java has the same names)
J_PAD = "guhs:park_pad"
J_ZIJ, J_PLEK = "guhs:park_zij", "guhs:park_plek"
J_LAAN, J_LAAN_EIND, J_VERPLICHT = "guhs:park_laan", "guhs:park_laan_eind", "guhs:park_verplicht"
POOL = "guhs:knabbelpark/"
P_PADEN, P_EIND, P_PLEKJES = POOL + "paden", POOL + "eind", POOL + "plekjes"

DIRS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}


def naar(dx, dz):
    """The facing for a direction (dx, dz)."""
    for f, d in DIRS.items():
        if d == (dx, dz):
            return f
    raise ValueError((dx, dz))


# =====================================================================================================================
# helpers: jigsaws, the park style
# =====================================================================================================================
def jigsaw(s, x, y, z, facing, name, target="minecraft:empty", pool="minecraft:empty", final=AIR, sel=0, plaats=0):
    """A jigsaw on the template's edge, facing out (horizontal: orientation <facing>_up, joint aligned)."""
    s.set(x, y, z, "minecraft:jigsaw", {"orientation": f"{facing}_up"},
          {"id": "minecraft:jigsaw", "name": name, "target": target, "pool": pool, "final_state": final, "joint": "aligned",
           "selection_priority": sel, "placement_priority": plaats})


def hek_props(s, x, y, z):
    """The connections of a fence post (to fences next to it)."""
    out = {"waterlogged": "false"}
    for f, (dx, dz) in DIRS.items():
        n = s.get(x + dx, y, z + dz)
        out[f] = "true" if n in (PAAL, "minecraft:cherry_fence_gate") else "false"
    return out


def hekken_vast(s):
    """Sets the connections of every fence post of the template (after all of them are placed)."""
    for (x, y, z), (name, props, nbt) in list(s.blocks.items()):
        if name == PAAL:
            s.blocks[(x, y, z)] = (name, hek_props(s, x, y, z), nbt)


def lantaarnpaal(s, x, y, z):
    """A park lantern post: two cherry fence posts and the guh lantern on top (y = the first post)."""
    s.set(x, y, z, PAAL, {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    s.set(x, y + 1, z, PAAL, {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    s.set(x, y + 2, z, LAMP)


def bankje(s, x, y, z, kijk):
    """A park bench; kijk = the way you look when you sit on it."""
    s.set(x, y, z, BANK, {"facing": kijk})


def prullenbak(s, x, y, z, kijk):
    s.set(x, y, z, BAK, {"facing": kijk})


def bloem(rng):
    return rng.choice(BLOEMEN)


def plantje(rng, s, x, y, z, bloemen=0.35, gras=0.3):
    """Something green or pink on a lawn spot (or nothing)."""
    r = rng.random()
    if r < bloemen:
        s.set(x, y, z, bloem(rng))
    elif r < bloemen + gras:
        s.set(x, y, z, rng.choice(PLANTJES))


def gazon(rng, s, x, y, z, roze=0.25):
    """A lawn block: grass, now and then pink knuffelgras (in patches: from the noise of the spot)."""
    patch = (math.sin(x * 0.55 + z * 0.21) + math.cos(z * 0.47 - x * 0.18)) * 0.5
    zet_gras(s, x, y, z, ROZE_GRAS if patch > 1 - 2 * roze or rng.random() < roze * 0.25 else GRAS)


def zet_gras(s, x, y, z, name):
    s.set(x, y, z, name, {"snowy": "false"} if name == GRAS else None)


def boom(rng, s, x, y, z, hoogte=4, r=2.4, soort="bloesem"):
    """A tree: a trunk from y up and a round, slightly ragged crown (blossom: pink; pluizen: fluffy), a few hanging leaves."""
    stam, blad = (BLOESEM_STAM, BLOESEM_BLAD) if soort == "bloesem" else (PLUIS_STAM, PLUIS_BLAD)
    for dy in range(hoogte):
        s.set(x, y + dy, z, stam, {"axis": "y"})
    cy = y + hoogte
    for dx in range(-int(r) - 1, int(r) + 2):
        for dz in range(-int(r) - 1, int(r) + 2):
            for dy in range(-1, int(r) + 1):
                d = math.sqrt(dx * dx + dz * dz + (dy * 1.35) ** 2)
                if d <= r + 0.15 and (d < r - 0.6 or rng.random() < 0.8) and s.inside(x + dx, cy + dy, z + dz):
                    if s.get(x + dx, cy + dy, z + dz) is None:
                        s.set(x + dx, cy + dy, z + dz, blad)
    for _ in range(int(r * 3)):
        dx, dz = rng.randint(-int(r), int(r)), rng.randint(-int(r), int(r))
        if 1 < math.hypot(dx, dz) <= r and s.get(x + dx, cy - 1, z + dz) == blad and s.get(x + dx, cy - 2, z + dz) is None:
            s.set(x + dx, cy - 2, z + dz, blad)


def struik(rng, s, x, y, z, soort="bloesem"):
    """A low bush: one or two leaf blocks."""
    blad = BLOESEM_BLAD if soort == "bloesem" else PLUIS_BLAD
    s.set(x, y, z, blad)
    if rng.random() < 0.4:
        s.set(x, y + 1, z, blad)


def onderlaag(s, sx, sz, blok=GROND):
    """The foundation layer of a rigid piece."""
    s.fill(0, 0, 0, sx - 1, 0, sz - 1, blok)


def lucht_erboven(s, sx, sz):
    """Air above the ground of a rigid piece (hills nearby slope down to it instead of poking through)."""
    s.clear_above([(x, z) for x in range(sx) for z in range(sz)], LOOP)


# =====================================================================================================================
# walks (terrain_matching): one painter for every shape
# =====================================================================================================================
class Wandeling:
    """A park walk on a grid: the path cells, its exits (connectors at the edges) and side connectors; paint() puts the
    path at y = 0 and lets the borders become flower beds, hedges, lantern posts, benches and litter bins."""

    def __init__(self, h, naam, sx, sz, seed):
        self.h, self.naam, self.sx, self.sz = h, naam, sx, sz
        self.s = h.Structure((sx, 4, sz))
        self.rng = random.Random(seed)
        self.pad = set()
        self.uit = []        # (x, z, facing)
        self.zij = []        # (x, z, facing)
        self.vast = set()    # cells already decorated by hand

    def cellen(self, pred):
        for x in range(self.sx):
            for z in range(self.sz):
                if pred(x, z):
                    self.pad.add((x, z))

    def afstand(self, x, z):
        best = 99
        for (px, pz) in self.pad:
            best = min(best, max(abs(px - x), abs(pz - z)))
        return best

    def richting_pad(self, x, z):
        """The way from this border cell to the nearest path cell (one of the four)."""
        best = None
        for f, (dx, dz) in DIRS.items():
            for k in (1, 2):
                if (x + dx * k, z + dz * k) in self.pad:
                    if best is None or k < best[0]:
                        best = (k, f)
        return best[1] if best else "north"

    def paint(self, lantaarns=8, bank=0.5, bak=0.35):
        s, rng = self.s, self.rng
        # the side connectors: the path goes on to the edge there, with an opening in the hedge
        for (x, z, f) in self.zij:
            dx, dz = DIRS[f]
            k = 0
            while (x - dx * k, z - dz * k) not in self.pad and k < 4:
                self.pad.add((x - dx * k, z - dz * k))
                k += 1
        for x in range(self.sx):
            for z in range(self.sz):
                if (x, z) in self.pad:
                    s.set(x, 0, z, PAD)
                    for y in (1, 2, 3):
                        s.set(x, y, z, AIR)
        # borders
        rand1, rand2 = [], []
        for x in range(self.sx):
            for z in range(self.sz):
                if (x, z) in self.pad:
                    continue
                d = self.afstand(x, z)
                if d == 1:
                    rand1.append((x, z))
                elif d >= 2:
                    rand2.append((x, z))
        open_zij = set()
        for (x, z, f) in self.zij:
            dx, dz = DIRS[f]
            px, pz = -dz, dx   # (across)
            for k in (-1, 1):
                open_zij.add((x + px * k, z + pz * k))
        teller = 0
        for (x, z) in sorted(rand2, key=lambda c: (c[1], c[0])):
            if (x, z) in self.vast:
                continue
            gazon(rng, s, x, 0, z)
            if (x, z) in open_zij:
                plantje(rng, s, x, 1, z, 0.6, 0.2)
                continue
            teller += 1
            if lantaarns and teller % lantaarns == lantaarns // 2:
                lantaarnpaal(s, x, 1, z)
            elif rng.random() < 0.82:
                s.set(x, 1, z, HEG)
                if rng.random() < 0.18:
                    s.set(x, 2, z, BLOESEM_BLAD)   # (a blossom bush in the hedge)
            else:
                plantje(rng, s, x, 1, z, 0.7, 0.2)
        for (x, z) in rand1:
            if (x, z) in self.vast:
                continue
            gazon(rng, s, x, 0, z, roze=0.35)
            r = rng.random()
            kijk = self.richting_pad(x, z)
            if r < 0.05 * bank:
                bankje(s, x, 1, z, kijk)
            elif r < 0.05 * bank + 0.03 * bak:
                prullenbak(s, x, 1, z, kijk)
            else:
                plantje(rng, s, x, 1, z, 0.55, 0.25)
        for (x, z, f) in self.uit:
            jigsaw(s, x, 1, z, f, J_PAD, J_PAD, P_PADEN, final=VOID)
        for (x, z, f) in self.zij:
            jigsaw(s, x, 1, z, f, J_ZIJ, J_PLEK, P_PLEKJES, final=AIR)
        hekken_vast(s)
        return s

    def zet_bank(self, x, z, kijk):
        bankje(self.s, x, 1, z, kijk)
        gazon(self.rng, self.s, x, 0, z)
        self.vast.add((x, z))

    def zet_bak(self, x, z, kijk):
        prullenbak(self.s, x, 1, z, kijk)
        gazon(self.rng, self.s, x, 0, z)
        self.vast.add((x, z))

    def zet_paal(self, x, z):
        lantaarnpaal(self.s, x, 1, z)
        gazon(self.rng, self.s, x, 0, z)
        self.vast.add((x, z))


def recht(h, naam, lengte, seed, zij=True):
    """A straight walk along z (7 wide), exits at both ends; side connectors in the middle (and at a quarter on the long one)."""
    w = Wandeling(h, naam, 7, lengte, seed)
    w.cellen(lambda x, z: 2 <= x <= 4)
    w.uit = [(3, 0, "north"), (3, lengte - 1, "south")]
    if zij:
        w.zij = [(0, lengte // 2, "west"), (6, lengte // 2, "east")]
        if lengte >= 16:
            w.zij += [(6, 3, "east"), (0, lengte - 4, "west")]
    w.zet_bank(1, lengte // 2 + 2 if lengte < 16 else lengte // 2 + 3, "east")
    w.zet_paal(0, 1)
    w.zet_bak(1, 1, "east")
    if lengte >= 16:
        w.zet_bank(5, lengte - 3, "west")
        w.zet_paal(6, lengte - 2)
    return w.paint()


def bocht(h, seed):
    w = Wandeling(h, "bocht", 7, 7, seed)
    w.cellen(lambda x, z: (2 <= x <= 4 and z >= 2) or (2 <= z <= 4 and x >= 2))
    w.uit = [(3, 6, "south"), (6, 3, "east")]
    w.zet_paal(0, 0)
    struik(w.rng, w.s, 1, 1, 1)
    w.vast.add((1, 1))
    w.zet_bank(5, 5, "north")
    return w.paint(lantaarns=0)


def splitsing(h, seed):
    w = Wandeling(h, "splitsing", 7, 7, seed)
    w.cellen(lambda x, z: (2 <= x <= 4 and z >= 2) or 2 <= z <= 4)
    w.uit = [(3, 6, "south"), (6, 3, "east"), (0, 3, "west")]
    w.zet_paal(3, 0)
    w.zet_bank(2, 1, "south")
    w.zet_bak(4, 1, "south")
    return w.paint(lantaarns=0)


def kruising(h, seed):
    w = Wandeling(h, "kruising", 7, 7, seed)
    w.cellen(lambda x, z: 2 <= x <= 4 or 2 <= z <= 4)
    w.uit = [(3, 0, "north"), (3, 6, "south"), (6, 3, "east"), (0, 3, "west")]
    for (x, z) in ((0, 0), (6, 6)):
        w.zet_paal(x, z)
    w.zet_bak(6, 0, "south")
    return w.paint(lantaarns=0)


def rotonde(h, seed):
    """A little round with a flower bed and a small blossom tree in the middle, four exits."""
    w = Wandeling(h, "rotonde", 11, 11, seed)
    w.cellen(lambda x, z: 2.4 <= math.hypot(x - 5, z - 5) <= 4.3 or (4 <= x <= 6 and (z <= 2 or z >= 8))
             or (4 <= z <= 6 and (x <= 2 or x >= 8)))
    w.uit = [(5, 0, "north"), (5, 10, "south"), (10, 5, "east"), (0, 5, "west")]
    s, rng = w.s, w.rng
    for x in range(3, 8):
        for z in range(3, 8):
            if (x, z) not in w.pad and math.hypot(x - 5, z - 5) < 2.4:
                gazon(rng, s, x, 0, z, roze=0.6)
                if (x, z) != (5, 5):
                    s.set(x, 1, z, bloem(rng))
                w.vast.add((x, z))
    boom(rng, s, 5, 1, 5, hoogte=2, r=1.4)
    for (x, z, kijk) in ((1, 1, "south"), (9, 9, "north")):
        w.zet_bank(x, z, kijk)
    w.zet_paal(9, 1)
    w.zet_paal(1, 9)
    return w.paint(lantaarns=0)


def eind_bank(h, seed):
    """The end of a walk: a little round with a bench, a lantern and a bin."""
    w = Wandeling(h, "eind_bank", 7, 6, seed)
    w.cellen(lambda x, z: 2 <= x <= 4 and z >= 2 or (1 <= x <= 5 and 2 <= z <= 3))
    w.uit = [(3, 5, "south")]
    w.zet_bank(3, 1, "south")
    w.zet_bank(2, 1, "south")
    w.zet_paal(5, 1)
    w.zet_bak(1, 1, "south")
    return w.paint(lantaarns=0)


def eind_bloemen(h, seed):
    """The end of a walk: a ring of flowers around a pluizenboompje."""
    w = Wandeling(h, "eind_bloemen", 7, 7, seed)
    w.cellen(lambda x, z: 2 <= x <= 4 and z >= 5)
    w.uit = [(3, 6, "south")]
    s, rng = w.s, w.rng
    for x in range(7):
        for z in range(5):
            if math.hypot(x - 3, z - 2) <= 2.6:
                gazon(rng, s, x, 0, z, roze=0.5)
                s.set(x, 1, z, bloem(rng))
                w.vast.add((x, z))
    boom(rng, s, 3, 1, 2, hoogte=2, r=1.3, soort="pluis")
    return w.paint(lantaarns=0)


# =====================================================================================================================
# rigid pieces: the ground of a lawn
# =====================================================================================================================
def tuin(h, sx, sy, sz, seed, roze=0.25):
    """A rigid piece's start: foundation, lawn everywhere, nothing on it yet."""
    s = h.Structure((sx, sy, sz))
    rng = random.Random(seed)
    onderlaag(s, sx, sz)
    for x in range(sx):
        for z in range(sz):
            gazon(rng, s, x, G, z, roze=roze)
    return s, rng


def pad_cel(s, x, z, blok=PAD):
    s.set(x, G, z, blok)
    s.set(x, LOOP, z, AIR)
    s.set(x, LOOP + 1, z, AIR)


def lawn_planten(s, rng, sx, sz, bloemen=0.3, gras=0.3):
    for x in range(sx):
        for z in range(sz):
            if s.get(x, LOOP, z) is None and s.get(x, G, z) in (GRAS, ROZE_GRAS):
                plantje(rng, s, x, LOOP, z, bloemen, gras)


def heg_rand(s, rng, sx, sz, gaten=(), kans=0.9):
    """A hedge along the edge of a rigid piece, open at the gaps (x, z)."""
    for x in range(sx):
        for z in range(sz):
            if (x in (0, sx - 1) or z in (0, sz - 1)) and (x, z) not in gaten and s.get(x, LOOP, z) is None and s.get(x, G, z) in (GRAS, ROZE_GRAS):
                if rng.random() < kans:
                    s.set(x, LOOP, z, HEG)


# =====================================================================================================================
# the start: the fountain plaza
# =====================================================================================================================
PLEIN = (33, 12, 33)
PC = 16


def plein(h):
    sx, sy, sz = PLEIN
    s, rng = tuin(h, sx, sy, sz, 1420001, roze=0.3)
    c = PC
    # the four walks from the edges to the plaza ring (3 wide, dirt path on the last two cells for the walk outside)
    for k in range(sx):
        for w in (c - 1, c, c + 1):
            for (x, z) in ((w, k), (k, w)):
                if math.hypot(x - c, z - c) > 6.3:
                    pad_cel(s, x, z, PAD if min(x, z, sx - 1 - x, sz - 1 - z) <= 1 else KLINKERS)
    # the ring of paving round the fountain, with a pink rose pattern
    for x in range(sx):
        for z in range(sz):
            d = math.hypot(x - c, z - c)
            if 3.6 < d <= 6.3:
                a = math.atan2(z - c, x - c)
                patroon = 5.0 < d < 5.6 and int((a + math.pi) / (math.pi / 8)) % 2 == 0
                pad_cel(s, x, z, "minecraft:pink_terracotta" if patroon else KLINKERS)
            elif 1.6 < d <= 3.6:
                s.set(x, 0, z, "minecraft:pink_terracotta")    # (the basin's floor)
                s.set(x, G, z, "minecraft:water", {"level": "0"})
                s.set(x, LOOP, z, AIR)
            elif d <= 1.6:
                pad_cel(s, x, z, KLINKERS)                      # the island of the Reisguh
    # four little bridges over the water to the island, and four guh-head spouts on the diagonals
    for (dx, dz) in ((0, -1), (0, 1), (1, 0), (-1, 0)):
        for k in (2, 3):
            pad_cel(s, c + dx * k, c + dz * k, "minecraft:smooth_quartz")
    for (dx, dz, kijk) in ((-2, -2, "north"), (2, -2, "east"), (2, 2, "south"), (-2, 2, "west")):
        s.set(c + dx, G, c + dz, "minecraft:quartz_block")
        s.set(c + dx, LOOP, c + dz, "guhs:knuffelsteen_gezicht", {"facing": kijk, "stemming": "1"})
    s.set(c, LOOP + 3, c, AIR)
    # the Reisguh "Knabbelpark" in the very middle
    s.entity(c + 0.5, float(LOOP), c + 0.5, reisguh_nbt(h))
    # benches round the ring facing the fountain, lanterns, a litter bin
    for (dx, dz) in ((-5, -3), (5, -3), (-5, 3), (5, 3), (-3, -5), (3, -5), (-3, 5), (3, 5)):
        x, z = c + dx, c + dz
        kijk = naar(-int(math.copysign(1, dx)), 0) if abs(dx) > abs(dz) else naar(0, -int(math.copysign(1, dz)))
        bankje(s, x, LOOP, z, kijk)
    for (dx, dz) in ((-5, -5), (5, -5), (-5, 5), (5, 5)):
        lantaarnpaal(s, c + dx, LOOP, c + dz)
    prullenbak(s, c + 6, LOOP, c - 2, "west")
    prullenbak(s, c - 6, LOOP, c + 2, "east")
    # flower beds round the ring, blossom trees in the four corners, a pluizenboom here and there
    for x in range(sx):
        for z in range(sz):
            d = math.hypot(x - c, z - c)
            if 6.3 < d <= 8.5 and s.get(x, LOOP, z) is None and s.get(x, G, z) in (GRAS, ROZE_GRAS):
                zet_gras(s, x, G, z, ROZE_GRAS if rng.random() < 0.5 else GRAS)
                s.set(x, LOOP, z, bloem(rng))
    for (x, z) in ((5, 5), (27, 5), (5, 27), (27, 27)):
        boom(rng, s, x, LOOP, z, hoogte=4, r=2.6)
    for (x, z) in ((10, 3), (22, 29), (3, 22), (29, 10)):
        boom(rng, s, x, LOOP, z, hoogte=3, r=1.9, soort="pluis")
    for (x, z) in ((10, 29), (22, 3), (3, 10), (29, 22)):
        struik(rng, s, x, LOOP, z)
    heg_rand(s, rng, sx, sz, gaten={(w, e) for w in (c - 1, c, c + 1) for e in (0, sz - 1)} | {(e, w) for w in (c - 1, c, c + 1) for e in (0, sx - 1)})
    lawn_planten(s, rng, sx, sz, 0.18, 0.3)
    # the four exits: N, E, W to the required lanes (they go first), S a random walk
    jigsaw(s, c, LOOP, 0, "north", "guhs:park_uit", J_LAAN, POOL + "laan_beeldentuin", sel=10, plaats=10)
    jigsaw(s, sx - 1, LOOP, c, "east", "guhs:park_uit", J_LAAN, POOL + "laan_hertenkooi", sel=10, plaats=10)
    jigsaw(s, 0, LOOP, c, "west", "guhs:park_uit", J_LAAN, POOL + "laan_parkwachter", sel=10, plaats=10)
    jigsaw(s, c, LOOP, sz - 1, "south", J_PAD, J_PAD, P_PADEN)
    hekken_vast(s)
    lucht_erboven(s, sx, sz)
    return s


def reisguh_nbt(h):
    from features import park
    return {"id": "guhs:guh_npc", "Kind": "reisguh", "ReisName": park.REISGUH_NAAM, "PersistenceRequired": h.Byte(1), "Invulnerable": h.Byte(1),
            "Rotation": h.floats(0.0, 0.0), "NeoForgeData": {"guhs_bezetting": "park_reisguh", "guhs_reis_vast": h.Byte(1)}}


# =====================================================================================================================
# the three required lanes (terrain_matching; the far end leads to exactly one required piece)
# =====================================================================================================================
LAAN = (7, 9)


def laan(h, doel, seed):
    sx, sz = LAAN
    w = Wandeling(h, "laan_" + doel, sx, sz, seed)
    w.cellen(lambda x, z: 2 <= x <= 4)
    w.zet_paal(0, 4)
    w.zet_paal(6, 4)
    s = w.paint(lantaarns=0)
    jigsaw(s, 3, 1, sz - 1, "south", J_LAAN, final=VOID)
    jigsaw(s, 3, 1, 0, "north", J_LAAN_EIND, J_VERPLICHT, POOL + doel, final=VOID, plaats=9)
    return s


# =====================================================================================================================
# the required pieces
# =====================================================================================================================
BEELDENTUIN = (29, 11, 17)
HERTENKOOI = (23, 9, 23)
PARKWACHTER = (17, 11, 15)
# the three statues: (block x, z, soort) on their pedestals, the plaque (x, z, nr) in front (the texts: park.PLAQUETTE_TEKST)
BEELDEN = [(5, 6, "joostvadsje"), (7, 6, "trapguh"), (9, 6, "suusvadsje"), (13, 6, "liekguh"), (15, 6, "lunde"), (21, 6, "opa_kaasguh")]
SOKKELS = [(5, 9), (12, 16), (20, 22)]          # x ranges of the three pedestals (z 5..6)
PLAQUETTES = [(7, 9, 1), (14, 9, 2), (21, 9, 3)]


def beeldentuin(h, plekken):
    sx, sy, sz = BEELDENTUIN
    s, rng = tuin(h, sx, sy, sz, 1420002, roze=0.2)
    mid = sx // 2
    # the walks: from the entrance (south) north to a cross walk in front of the statues, round the back to the exit
    for z in range(10, sz):
        for x in (mid - 1, mid, mid + 1):
            pad_cel(s, x, z)
    for x in range(2, sx - 2):
        for z in (10, 11):
            pad_cel(s, x, z)
    for z in range(2, 12):
        for x in (2, 3, sx - 4, sx - 3):
            pad_cel(s, x, z)
    for x in range(2, sx - 2):
        for z in (2, 3):
            pad_cel(s, x, z)
    for z in (0, 1):
        for x in (mid - 1, mid, mid + 1):
            pad_cel(s, x, z)
    # the pedestals (stone bricks with a polished top), the statues on them, flowers round them
    for (x0, x1) in SOKKELS:
        for x in range(x0, x1 + 1):
            for z in (5, 6, 7):
                s.set(x, G, z, "minecraft:stone_bricks")
                s.set(x, LOOP, z, "minecraft:polished_andesite" if z != 7 else "minecraft:stone_brick_slab", {} if z != 7 else {"type": "bottom", "waterlogged": "false"})
    for (x, z, soort) in BEELDEN:
        s.set(x, LOOP + 1, z, "guhs:park_beeld", {"facing": "south", "beeld": soort})
    for (x, z, nr) in PLAQUETTES:
        s.set(x, LOOP, z, "guhs:park_plaquette", {"facing": "north", "nr": str(nr)})
        s.set(x, G, z, "minecraft:polished_andesite")
    for (x0, x1) in SOKKELS:
        for x in range(x0 - 1, x1 + 2):
            for z in (4, 8):
                if s.get(x, LOOP, z) is None and s.get(x, G, z) in (GRAS, ROZE_GRAS):
                    s.set(x, LOOP, z, bloem(rng))
    # a hedge with blossom trees behind, benches facing the statues, lanterns
    for x in range(sx):
        if not (mid - 1 <= x <= mid + 1):
            s.set(x, LOOP, 0, HEG)
            s.set(x, LOOP + 1, 0, HEG if rng.random() < 0.7 else BLOESEM_BLAD)
    for (x, z) in ((1, 14), (27, 14)):
        boom(rng, s, x, LOOP, z, hoogte=4, r=2.2)
    for x in (9, 19):
        bankje(s, x, LOOP, 13, "north")
    for x in (8, 10, 18, 20):
        s.set(x, LOOP, 13, bloem(rng))
    for (x, z) in ((4, 12), (24, 12), (4, 1), (24, 1)):
        lantaarnpaal(s, x, LOOP, z)
    prullenbak(s, mid + 2, LOOP, 12, "west")
    # a sign at the entrance
    bord(h, s, mid - 2, LOOP, sz - 2, "south", ["De Beeldentuin", "Lees de bordjes!", "Niet op de", "beelden klimmen. Njeg."])
    heg_rand(s, rng, sx, sz, gaten={(x, z) for x in (mid - 1, mid, mid + 1) for z in (0, sz - 1)}, kans=0.85)
    lawn_planten(s, rng, sx, sz, 0.3, 0.3)
    jigsaw(s, mid, LOOP, sz - 1, "south", J_VERPLICHT)
    jigsaw(s, mid, LOOP, 0, "north", J_PAD, J_PAD, P_PADEN)
    hekken_vast(s)
    lucht_erboven(s, sx, sz)
    return s


KOOI = (7, 4, 18, 18)      # fence ring x0, z0, x1, z1


def hertenkooi(h, plekken):
    sx, sy, sz = HERTENKOOI
    s, rng = tuin(h, sx, sy, sz, 1420003, roze=0.15)
    x0, z0, x1, z1 = KOOI
    # the walks: in from the west, round the paddock, out to the east
    for x in range(0, x0 - 1):
        for z in (10, 11, 12):
            pad_cel(s, x, z)
    for z in range(2, sz - 2):
        for x in (x0 - 3, x0 - 2):
            pad_cel(s, x, z)
        for x in (x1 + 2, x1 + 3):
            pad_cel(s, x, z)
    for x in range(x0 - 3, x1 + 4):
        for z in (z0 - 2, z0 - 1, z1 + 2):
            if 0 <= z < sz:
                pad_cel(s, x, z)
    for x in range(x1 + 2, sx):
        for z in (10, 11, 12):
            pad_cel(s, x, z)
    # the paddock: soft grass and flowers inside, a cherry fence round it
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            zet_gras(s, x, G, z, GRAS)
            if rand:
                s.set(x, LOOP, z, PAAL)
            else:
                s.set(x, LOOP, z, AIR)
                s.set(x, LOOP + 1, z, AIR)
                if rng.random() < 0.3:
                    plantje(rng, s, x, LOOP, z, 0.4, 0.6)
    # inside: a little shelter (stripped cherry posts, a pink roof), hay, a water trough, a blossom tree
    for (x, z) in ((x1 - 4, z0 + 1), (x1 - 1, z0 + 1), (x1 - 4, z0 + 3), (x1 - 1, z0 + 3)):
        for y in (LOOP, LOOP + 1):
            s.set(x, y, z, "minecraft:stripped_cherry_log", {"axis": "y"})
    for x in range(x1 - 5, x1 + 1):
        for z in range(z0 + 1, z0 + 5):
            s.set(x, LOOP + 2, z, "minecraft:pink_wool")
    for (x, z) in ((x1 - 3, z0 + 2), (x1 - 2, z0 + 2)):
        s.set(x, LOOP, z, "minecraft:hay_block", {"axis": "y"})
    for x in (x0 + 2, x0 + 3):
        s.set(x, LOOP, z1 - 2, "minecraft:water_cauldron", {"level": "3"})
    boom(rng, s, x0 + 3, LOOP, z0 + 3, hoogte=3, r=1.8)
    # the Hertguhtjes
    for n, (x, y, z) in enumerate(plekken["HERTEN"], start=1):
        assert x0 < x < x1 and z0 < z < z1, (x, z)
        s.set(x, y, z, AIR)
        s.entity(x + 0.5, float(y), z + 0.5, h.ms.guh_nbt(0.85, Variant="hertguhtje", Behavior="PASSIVE", Invulnerable=h.Byte(1),
                                                       KnusVlaggen=1 << 29, NeoForgeData={"guhs_bezetting": f"park_hert_{n}"}))
    # the kooi-verzorger by her hay bales (north of her: the path)
    vx, vy, vz = plekken["VERZORGER"]
    for (x, z) in ((vx - 1, vz + 1), (vx + 1, vz + 1), (vx - 1, vz)):
        s.set(x, LOOP, z, "minecraft:hay_block", {"axis": "y"})
    s.set(vx + 1, LOOP, vz, "minecraft:composter", {"level": "4"})
    s.set(vx, G, vz, "minecraft:coarse_dirt")
    s.set(vx, LOOP, vz, AIR)
    s.set(vx, LOOP + 1, vz, AIR)
    s.entity(vx + 0.5, float(vy), vz + 0.5, wereld.npc(h, "kooiverzorgerguh", "park_verzorger", yaw=plekken["VERZORGER_YAW"]))
    # benches to watch the Hertguhtjes, lanterns, a bin, a sign
    bankje(s, x0 - 4, LOOP, 7, "east")
    bankje(s, x1 + 4, LOOP, 15, "west")
    for (x, z) in ((x0 - 4, 3), (x1 + 4, 19), (x0 - 4, 19), (x1 + 4, 3)):
        lantaarnpaal(s, x, LOOP, z)
    prullenbak(s, x0 - 4, LOOP, 9, "east")
    bord(h, s, x0 - 1, LOOP, 9, "west", ["De Hertenkooi", "Voeren mag!", "(alleen kaasknabbels)", "Vahoeg!"])
    for (x, z) in ((1, 2), (1, 20), (21, 21)):
        boom(rng, s, x, LOOP, z, hoogte=3, r=1.6, soort="pluis")
    heg_rand(s, rng, sx, sz, gaten={(x, z) for x in (0, sx - 1) for z in (10, 11, 12)}, kans=0.85)
    lawn_planten(s, rng, sx, sz, 0.25, 0.35)
    jigsaw(s, 0, LOOP, 11, "west", J_VERPLICHT)
    jigsaw(s, sx - 1, LOOP, 11, "east", J_PAD, J_PAD, P_PADEN)
    hekken_vast(s)
    lucht_erboven(s, sx, sz)
    return s


HUT = (3, 3, 9, 11)        # the hut x0, z0, x1, z1 (the counter on x1)


def parkwachter(h, plekken):
    sx, sy, sz = PARKWACHTER
    s, rng = tuin(h, sx, sy, sz, 1420004, roze=0.25)
    hx0, hz0, hx1, hz1 = HUT
    wx, wy, wz = plekken["WACHTER"]
    # walks: from the entrance (east) to the counter; round the south of the hut to the exit (west)
    for x in range(hx1 + 1, sx):
        for z in (wz - 1, wz, wz + 1):
            pad_cel(s, x, z)
    for x in range(1, sx - 1):
        for z in (hz1 + 1, hz1 + 2):
            pad_cel(s, x, z)
    for z in range(wz - 1, hz1 + 2):
        for x in (0, 1):
            pad_cel(s, x, z)
    for x in range(hx1 + 1, hx1 + 3):
        for z in range(wz, hz1 + 2):
            pad_cel(s, x, z)
    # the hut: a floor of spruce, birch walls with stripped cherry corners, a pink roof with two round guh ears
    for x in range(hx0, hx1 + 1):
        for z in range(hz0, hz1 + 1):
            s.set(x, G, z, "minecraft:spruce_planks")
            rand = x in (hx0, hx1) or z in (hz0, hz1)
            for y in range(LOOP, LOOP + 3):
                if rand:
                    hoek = x in (hx0, hx1) and z in (hz0, hz1)
                    s.set(x, y, z, "minecraft:stripped_cherry_log" if hoek else "minecraft:birch_planks", {"axis": "y"} if hoek else {})
                else:
                    s.set(x, y, z, AIR)
            s.set(x, LOOP + 3, z, "minecraft:pink_concrete")
    for x in range(hx0 - 1, hx1 + 2):
        for z in range(hz0 - 1, hz1 + 2):
            if not (hx0 <= x <= hx1 and hz0 <= z <= hz1):
                s.set(x, LOOP + 3, z, "minecraft:pink_wool")
    for (ex, ez) in ((hx0 + 1, hz0 + 2), (hx0 + 1, hz1 - 2)):
        s.set(ex, LOOP + 4, ez, "minecraft:pink_wool")
        s.set(ex, LOOP + 5, ez, "minecraft:pink_wool")
        s.set(ex + 1, LOOP + 4, ez, "minecraft:pink_wool")
    # the counter: an open front on the east side, the parkwachter behind it
    for z in range(hz0 + 2, hz1 - 1):
        s.set(hx1, LOOP, z, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
        s.set(hx1, LOOP + 1, z, AIR)
        s.set(hx1, LOOP + 2, z, "minecraft:pink_wool" if z % 2 else "minecraft:white_wool")   # (a striped awning)
    for z in (wz - 1, wz, wz + 1):
        s.set(hx1 + 1, G, z, PAD)
    s.set(wx, wy, wz, AIR)
    s.entity(wx + 0.5, float(wy), wz + 0.5, wereld.npc(h, "parkwachterguh", "park_wachter", yaw=plekken["WACHTER_YAW"]))
    s.set(hx0 + 1, LOOP, hz0 + 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    s.set(hx0 + 1, LOOP, hz1 - 1, "minecraft:potted_cherry_sapling")
    s.set(hx0 + 1, LOOP + 1, hz0 + 1, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # windows
    for (x, z) in ((hx0, hz0 + 4), (hx0 + 3, hz0), (hx0 + 3, hz1)):
        s.set(x, LOOP + 1, z, "minecraft:pink_stained_glass_pane", pane(x == hx0))
    # the notice board with the park rules, benches, a lantern, flowers, trees
    bord(h, s, hx1 + 3, LOOP, wz - 3, "south", ["Parkregels:", "1. Wees lief", "2. Geen Mika's op", "het gras. Njeg!"])
    bord(h, s, hx1 + 4, LOOP, wz - 3, "south", ["3. Voer de", "Hertguhtjes alleen", "kaasknabbels", "4. VAHOEG!"])
    bankje(s, hx1 + 5, LOOP, wz + 3, "north")
    lantaarnpaal(s, hx1 + 2, LOOP, wz - 2)
    prullenbak(s, hx1 + 6, LOOP, wz - 2, "south")
    for (x, z) in ((14, 1), (2, 1)):
        boom(rng, s, x, LOOP, z, hoogte=4, r=2.0)
    for x in range(hx0, hx1 + 1):
        if s.get(x, LOOP, hz0 - 1) is None:
            s.set(x, LOOP, hz0 - 1, bloem(rng))
    heg_rand(s, rng, sx, sz, gaten={(sx - 1, z) for z in (wz - 1, wz, wz + 1)} | {(0, z) for z in range(wz - 1, hz1 + 3)}, kans=0.85)
    lawn_planten(s, rng, sx, sz, 0.3, 0.3)
    jigsaw(s, sx - 1, LOOP, wz, "east", J_VERPLICHT)
    jigsaw(s, 0, LOOP, wz, "west", J_PAD, J_PAD, P_PADEN)
    hekken_vast(s)
    lucht_erboven(s, sx, sz)
    return s


def pane(ns):
    """Glass pane connections: along the wall (ns: the wall runs north-south)."""
    return {"north": str(ns).lower(), "south": str(ns).lower(), "east": str(not ns).lower(), "west": str(not ns).lower(), "waterlogged": "false"}


def bord(h, s, x, y, z, kijk, regels):
    """A standing sign (cherry) with four Dutch lines (translatable: sign_text)."""
    import sign_text
    B = h.Byte
    voor = {"messages": sign_text.messages("sign.guhs.park", regels), "color": "black", "has_glowing_text": B(0)}
    leeg = {"messages": sign_text.messages("sign.guhs.park", ["", "", "", ""]), "color": "black", "has_glowing_text": B(0)}
    rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[kijk]
    s.set(x, y, z, "minecraft:cherry_sign", {"rotation": rot, "waterlogged": "false"},
          {"id": "minecraft:sign", "is_waxed": B(1), "front_text": voor, "back_text": leeg})


# =====================================================================================================================
# PLEKJES API: the example plekjes (rigid, G = 1, entrance on the south edge in the middle at y = 2, facing south)
# =====================================================================================================================
def plekje_begin(h, naam, sx, sy, sz, seed, roze=0.3):
    """The start of every plekje: foundation, lawn, the entrance jigsaw and a little path in (call plekje_af at the end)."""
    s, rng = tuin(h, sx, sy, sz, seed, roze=roze)
    mid = sx // 2
    for z in (sz - 1, sz - 2):
        pad_cel(s, mid, z)
    return s, rng, mid


def plekje_af(s, rng, sx, sz, heg=0.5):
    mid = sx // 2
    heg_rand(s, rng, sx, sz, gaten={(mid - 1, sz - 1), (mid, sz - 1), (mid + 1, sz - 1)}, kans=heg)
    lawn_planten(s, rng, sx, sz, 0.35, 0.3)
    jigsaw(s, mid, LOOP, sz - 1, "south", J_PLEK)
    hekken_vast(s)
    lucht_erboven(s, sx, sz)
    return s


def bloemenweide(h):
    """A meadow full of flowers round a blossom tree, a bench in its shade."""
    sx, sy, sz = 13, 10, 11
    s, rng, mid = plekje_begin(h, "bloemenweide", sx, sy, sz, 1420101, roze=0.45)
    for z in range(5, sz):
        pad_cel(s, mid, z)
    boom(rng, s, mid, LOOP, 3, hoogte=4, r=2.6)
    bankje(s, mid, LOOP, 5, "south")
    for x in range(sx):
        for z in range(sz):
            if s.get(x, LOOP, z) is None and rng.random() < 0.6:
                s.set(x, LOOP, z, bloem(rng))
    return plekje_af(s, rng, sx, sz, heg=0.3)


def rustplekje(h):
    """A round of paving with three benches facing a flower bed, a lantern and a bin."""
    sx, sy, sz = 9, 6, 9
    s, rng, mid = plekje_begin(h, "rustplekje", sx, sy, sz, 1420102)
    c = 4
    for x in range(sx):
        for z in range(sz):
            d = math.hypot(x - c, z - c)
            if 1.2 < d <= 3.3:
                pad_cel(s, x, z, KLINKERS)
            elif d <= 1.2:
                zet_gras(s, x, G, z, ROZE_GRAS)
                s.set(x, LOOP, z, bloem(rng))
    for z in (sz - 1, sz - 2):
        pad_cel(s, mid, z)
    for (x, z, kijk) in ((c - 3, c, "east"), (c + 3, c, "west"), (c, c - 3, "south")):
        bankje(s, x, LOOP, z, kijk)
    lantaarnpaal(s, c - 3, LOOP, c - 3)
    prullenbak(s, c + 3, LOOP, c - 2, "west")
    return plekje_af(s, rng, sx, sz, heg=0.8)


def bosje(h):
    """A little wood: two pluizenbomen and a blossom tree, bushes, mushrooms and flowers under them."""
    sx, sy, sz = 11, 11, 10
    s, rng, mid = plekje_begin(h, "bosje", sx, sy, sz, 1420103, roze=0.2)
    for z in range(4, sz):
        pad_cel(s, mid, z)
    boom(rng, s, 2, LOOP, 3, hoogte=5, r=2.3, soort="pluis")
    boom(rng, s, 8, LOOP, 2, hoogte=4, r=2.0, soort="pluis")
    boom(rng, s, 8, LOOP, 7, hoogte=3, r=1.8)
    for (x, z) in ((1, 7), (4, 1), (6, 5), (9, 4)):
        struik(rng, s, x, LOOP, z, soort="pluis" if rng.random() < 0.5 else "bloesem")
    for (x, z) in ((3, 6), (7, 4)):
        s.set(x, LOOP, z, "guhs:guhpaddenstoel")
    bankje(s, mid - 1, LOOP, 4, "east")
    return plekje_af(s, rng, sx, sz, heg=0.4)


def bloesemboom(h):
    """One big blossom tree with a carpet of petals under it and a bench to look at it."""
    sx, sy, sz = 11, 12, 11
    s, rng, mid = plekje_begin(h, "bloesemboom", sx, sy, sz, 1420104, roze=0.6)
    boom(rng, s, 5, LOOP, 4, hoogte=5, r=3.4)
    for x in range(sx):
        for z in range(sz):
            if s.get(x, LOOP, z) is None and math.hypot(x - 5, z - 4) < 4.5 and rng.random() < 0.7:
                s.set(x, LOOP, z, "minecraft:pink_petals", {"facing": rng.choice(list(DIRS)), "flower_amount": str(rng.randint(1, 4))})
    for z in range(8, sz):
        pad_cel(s, mid, z)
    bankje(s, mid, LOOP, 8, "north")
    return plekje_af(s, rng, sx, sz, heg=0.25)


# --- the pools ----------------------------------------------------------------------------------------------------------
PADEN = [("recht_lang", 5), ("recht_kort", 3), ("bocht", 5), ("splitsing", 2), ("kruising", 1), ("rotonde", 1)]
EINDEN = [("eind_bank", 2), ("eind_bloemen", 1)]
PLEKJES = [("bloemenweide", 2), ("rustplekje", 2), ("bosje", 3), ("bloesemboom", 2)]
PLEKJES += [("eendenvijver", 2), ("speeltuin", 2), ("picknickweide", 2), ("muziektuin", 2), ("japanse_tuin", 2)]   # <park-a> park_plek_a.py
PLEKJES_LEEG = 1                     # the weight of "nothing here" in the plekjes pool
VERPLICHT = ["plein", "laan_beeldentuin", "laan_hertenkooi", "laan_parkwachter", "beeldentuin", "hertenkooi", "parkwachter"]


def templates(h, plekken):
    """{template name (under knabbelpark/): Structure} of every piece."""
    t = {"plein": plein(h)}
    for doel, seed in (("beeldentuin", 1420011), ("hertenkooi", 1420012), ("parkwachter", 1420013)):
        t["laan_" + doel] = laan(h, doel, seed)
    t["beeldentuin"] = beeldentuin(h, plekken)
    t["hertenkooi"] = hertenkooi(h, plekken)
    t["parkwachter"] = parkwachter(h, plekken)
    t["recht_lang"] = recht(h, "recht_lang", 16, 1420021)
    t["recht_kort"] = recht(h, "recht_kort", 9, 1420022)
    t["bocht"] = bocht(h, 1420023)
    t["splitsing"] = splitsing(h, 1420024)
    t["kruising"] = kruising(h, 1420025)
    t["rotonde"] = rotonde(h, 1420026)
    t["eind_bank"] = eind_bank(h, 1420031)
    t["eind_bloemen"] = eind_bloemen(h, 1420032)
    t["bloemenweide"] = bloemenweide(h)
    t["rustplekje"] = rustplekje(h)
    t["bosje"] = bosje(h)
    t["bloesemboom"] = bloesemboom(h)
    from features import park_plek_a    # <park-a>
    t.update(park_plek_a.templates(h))
    t.update(park_plek_b.templates(h))      # park-plekjes b (its plekjes, the uitkijktoren and the filler)
    return t


PAD_PROCESSOR = {"processor_type": "minecraft:rule", "rules": [
    {"input_predicate": {"predicate_type": "minecraft:block_match", "block": b},
     "location_predicate": {"predicate_type": "minecraft:block_match", "block": "minecraft:water"},
     "output_state": {"Name": BRUG}} for b in (PAD, GRAS, ROZE_GRAS)]}


def element(naam, projectie, delta=None):
    e = {"location": f"guhs:knabbelpark/{naam}", "projection": projectie,
         "processors": {"processors": [PAD_PROCESSOR]} if projectie == "terrain_matching" else "minecraft:empty"}
    if delta is None:
        return {"element_type": "minecraft:single_pool_element", **e}
    return {"element_type": "guhs:grond_single_pool_element", **e, "ground_level_delta": delta}


def pools(h):
    D, w = h.D, h.w
    pool = lambda naam, elementen, fallback="minecraft:empty": w(f"{D}/worldgen/template_pool/knabbelpark/{naam}.json",
                                                                {"fallback": fallback, "elements": elementen})
    pool("start", [{"weight": 1, "element": element("plein", "rigid", delta=LOOP)}])
    for doel in ("beeldentuin", "hertenkooi", "parkwachter"):
        pool("laan_" + doel, [{"weight": 1, "element": element("laan_" + doel, "terrain_matching")}])
        pool(doel, [{"weight": 1, "element": element(doel, "rigid")}])
    pool("paden", [{"weight": n, "element": element(naam, "terrain_matching")} for naam, n in PADEN], fallback=P_EIND)
    pool("eind", [{"weight": n, "element": element(naam, "terrain_matching")} for naam, n in EINDEN])
    pool("plekjes", [{"weight": n, "element": element(naam, "rigid")} for naam, n in PLEKJES]
         + [{"weight": PLEKJES_LEEG, "element": {"element_type": "minecraft:empty_pool_element"}}])


# =====================================================================================================================
# self-checks of the templates
# =====================================================================================================================
def _vrij(s, x, y, z):
    n = s.get(x, y, z)
    if n is None or n in (AIR, VOID, "minecraft:water"):
        return True
    kort = n.split(":")[-1]
    return any(t in kort for t in ("grass", "gras", "bloem", "roos", "oortjes", "tulip", "allium", "lily", "petals", "jigsaw", "paddenstoel")) \
        and "block" not in kort and "knuffelgras" not in kort


def _vloer(s, x, y, z):
    n = s.get(x, y, z)
    return n is not None and not _vrij(s, x, y, z) and not any(t in n for t in ("fence", "sign", "lantaarn", "bankje", "prullenbak", "plaquette", "beeld"))


def bereikbaar(s, van):
    """The walk-level cells reachable from `van` (x, z) inside a rigid template (floor at G, room at G+1 and G+2; water is
    wadeable)."""
    sx, _, sz = s.size
    gezien, rij = {van}, [van]
    while rij:
        x, z = rij.pop()
        for dx, dz in DIRS.values():
            n = (x + dx, z + dz)
            if n in gezien or not (0 <= n[0] < sx and 0 <= n[1] < sz):
                continue
            water = s.get(n[0], G, n[1]) == "minecraft:water"
            if (_vloer(s, n[0], G, n[1]) or water) and _vrij(s, n[0], LOOP, n[1]) and _vrij(s, n[0], LOOP + 1, n[1]):
                gezien.add(n)
                rij.append(n)
    return gezien


def check(h, t, plekken):
    """Every required spot reachable on foot from its piece's entrance; jigsaws where the Java expects them; no cherry
    planks in rigid pieces; NPCs stand on a floor with room."""
    problems = []

    def bij(cellen, x, z, r=2.0):
        return any(math.hypot(cx - x, cz - z) <= r for cx, cz in cellen)

    def jigsaws(s):
        return [(p, n["nbt"]) for p, n in ((p, {"nbt": b[2]}) for p, b in s.blocks.items() if b[0] == "minecraft:jigsaw")]

    rigid = ["plein", "beeldentuin", "hertenkooi", "parkwachter"] + [n for n, _ in PLEKJES]
    for naam in rigid:
        s = t[naam]
        if any(b[0] == BRUG for b in s.blocks.values()):
            problems.append(f"{naam}: cherry planks in a rigid piece (ParkPaden would take them for a walk)")
        for (x, y, z), nbt in jigsaws(s):
            if y != LOOP:
                problems.append(f"{naam}: jigsaw {nbt['name']} at y {y}, not {LOOP}")
            edge = x in (0, s.size[0] - 1) or z in (0, s.size[2] - 1)
            if not edge:
                problems.append(f"{naam}: jigsaw {nbt['name']} not on the edge")
            if s.get(x, G, z) not in (PAD,):
                problems.append(f"{naam}: no dirt path under the jigsaw at {(x, z)} (the walk next to it must see the floor)")
    for naam, _ in PADEN + EINDEN:
        s = t[naam]
        for (x, y, z), nbt in jigsaws(s):
            if y != 1:
                problems.append(f"{naam}: walk jigsaw at y {y}")
            if s.get(x, 0, z) != PAD:
                problems.append(f"{naam}: no path under the jigsaw at {(x, z)}")
    for naam, _ in PLEKJES:
        s = t[naam]
        ing = [(p, nbt) for p, nbt in jigsaws(s) if nbt["name"] == J_PLEK]
        if len(ing) != 1 or ing[0][0] != (s.size[0] // 2, LOOP, s.size[2] - 1):
            problems.append(f"plekje {naam}: the entrance jigsaw must be at the middle of the south edge: {ing}")
    # reachability inside the required pieces
    p = t["plein"]
    cel = bereikbaar(p, (PC, 0))
    if not bij(cel, plekken["REISGUH"][0], plekken["REISGUH"][2], 1.5):
        problems.append("plein: the Reisguh's island is not reachable from the north exit")
    for x, z in ((PC, PLEIN[2] - 1), (0, PC), (PLEIN[0] - 1, PC)):
        if (x, z) not in cel:
            problems.append(f"plein: exit {(x, z)} not connected")
    b = t["beeldentuin"]
    cel = bereikbaar(b, (BEELDENTUIN[0] // 2, BEELDENTUIN[2] - 1))
    for (x, z, nr) in PLAQUETTES:
        if not bij(cel, x, z, 1.5):
            problems.append(f"beeldentuin: plaque {nr} not reachable")
    if (BEELDENTUIN[0] // 2, 0) not in cel:
        problems.append("beeldentuin: the back exit is not reachable")
    k = t["hertenkooi"]
    cel = bereikbaar(k, (0, 11))
    vx, _, vz = plekken["VERZORGER"]
    if not bij(cel, vx, vz, 2.5):
        problems.append("hertenkooi: the kooi-verzorger is not reachable")
    if (HERTENKOOI[0] - 1, 11) not in cel:
        problems.append("hertenkooi: the east exit is not reachable")
    x0, z0, x1, z1 = KOOI
    if not any(bij(cel, x, z, 1.0) for x in range(x0, x1 + 1) for z in (z0, z1)):
        problems.append("hertenkooi: nobody can stand at the fence to feed")
    inside = bereikbaar(k, (plekken["HERTEN"][0][0], plekken["HERTEN"][0][2]))
    if any(c in cel for c in inside):
        problems.append("hertenkooi: the paddock is open (the Hertguhtjes would walk out)")
    for (x, y, z) in plekken["HERTEN"]:
        if (x, z) not in inside:
            problems.append(f"hertenkooi: Hertguhtje at {(x, z)} not in the paddock")
    w = t["parkwachter"]
    cel = bereikbaar(w, (PARKWACHTER[0] - 1, plekken["WACHTER"][2]))
    wx, _, wz = plekken["WACHTER"]
    if not bij(cel, wx, wz, 2.2):
        problems.append("parkwachter: the counter is not reachable")
    if (0, wz) not in cel:
        problems.append("parkwachter: the west exit is not reachable")
    for naam, (x, y, z) in (("parkwachter", plekken["WACHTER"]), ("hertenkooi", plekken["VERZORGER"])):
        s = t[naam]
        if not (_vloer(s, x, G, z) and _vrij(s, x, LOOP, z) and _vrij(s, x, LOOP + 1, z)):
            problems.append(f"{naam}: the NPC at {(x, z)} has no floor or no room")
    for naam, _ in PLEKJES:
        s = t[naam]
        cel = bereikbaar(s, (s.size[0] // 2, s.size[2] - 1))
        if len(cel) < 4:
            problems.append(f"plekje {naam}: you can hardly walk in")
    # every walk: its exits connected over the path
    for naam, _ in PADEN + EINDEN + [("laan_beeldentuin", 0)]:
        s = t[naam]
        pad = {(x, z) for (x, y, z), b in s.blocks.items() if y == 0 and b[0] == PAD}
        uit = [(x, z) for (x, y, z), b in s.blocks.items() if b[0] == "minecraft:jigsaw"]
        if uit:
            van = uit[0]
            gezien, rij = {van}, [van]
            while rij:
                cx, cz = rij.pop()
                for dx, dz in DIRS.values():
                    n = (cx + dx, cz + dz)
                    if n in pad and n not in gezien:
                        gezien.add(n)
                        rij.append(n)
            for u in uit:
                if u not in gezien:
                    problems.append(f"{naam}: connector {u} not on the path")
    return problems


# --- park-plekjes b (tools/features/park_plek_b.py): four more random plekjes; the uitkijktoren and the filler are swapped in by
# feature/park/plek/Uitkijktoren.schik (not in the pool) ------------------------------------------------------------------
from features import park_plek_b  # noqa: E402
PLEKJES += park_plek_b.PLEKJES
