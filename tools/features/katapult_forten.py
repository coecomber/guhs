"""
De 12 Mika-forten van de Knabbelkatapult (2.9, slice sjoelkatapult): hand-made templates katapult/fort_01 .. fort_12
(13 x 14 x 9: x across with the middle at x = 6, y up from the plot, z backwards; z = 0 is the front, facing the
catapult = north in the template). KatapultGame builds them one by one on the fort plot and knocks them down.

Materials (the cost of a knock, see KatapultFort.strength): Mika's, knabbelkisten, glass, ice and wool 1, wood 2, stone 4.
Every fort must stand by itself (KatapultFort.unstable: a block holds when it rests on something that stands, or hangs
on at most 3 blocks sideways from it) and has at least one Mika.
Run it alone:  python tools/features/katapult_forten.py   (from the project root): checks all forts, prints them.
"""
import os
import sys
from collections import deque

if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

W, H, D = 13, 14, 9
MID = 6
OVERHANG = 3

PLANK = "guhs:katapult_mikaplank"
LOG = "minecraft:dark_oak_log"
STONE = "guhs:mika_steen"
PILLAR = "guhs:mika_steen_pilaar"
GLASS = "minecraft:purple_stained_glass"
ICE = "minecraft:packed_ice"
BLUEICE = "minecraft:blue_ice"
WOOL = "minecraft:purple_wool"
PINKWOOL = "minecraft:magenta_wool"
MIKA = "guhs:katapult_mika"
KIST = "guhs:katapult_knabbelkist"
FENCE = "minecraft:dark_oak_fence"
SLAB_WOOD = "minecraft:dark_oak_slab"
SLAB_STONE = "guhs:mika_steen_plaat"
KOP = "guhs:verkoolde_mikakop"

NAMES = [  # (id, name)
    "Het Mikahutje", "De Wiebeltoren", "De Twee Torentjes", "Het IJspaleisje", "De Stenen Muur", "De Mikapiramide",
    "De Wolkenkrabber", "Het Mika-kasteeltje", "De Hangbrug", "Het Knabbelpakhuis", "De Mikamolen", "De Grote Mikaburcht",
]


class Fort:
    def __init__(self):
        self.blocks = {}

    def set(self, x, y, z, block, props=None):
        if 0 <= x < W and 0 <= y < H and 0 <= z < D:
            self.blocks[(x, y, z)] = (block, dict(props or {}))

    def fill(self, x0, y0, z0, x1, y1, z1, block, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, block, props)

    def walls(self, x0, y0, z0, x1, y1, z1, block, props=None):
        """A hollow box (walls only, no floor or roof)."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    for y in range(y0, y1 + 1):
                        self.set(x, y, z, block, props)

    def floor(self, x0, z0, x1, z1, y, block, props=None):
        self.fill(x0, y, z0, x1, y, z1, block, props)

    def mika(self, x, y, z):
        self.set(x, y, z, MIKA, {"facing": "north"})

    def kist(self, x, y, z):
        self.set(x, y, z, KIST, {"facing": "north"})

    def log(self, x0, y0, z0, x1, y1, z1):
        axis = "y" if y0 != y1 else ("x" if x0 != x1 else "z")
        self.fill(x0, y0, z0, x1, y1, z1, LOG, {"axis": axis})

    def pillar(self, x, y0, y1, z):
        self.fill(x, y0, z, x, y1, z, PILLAR, {"axis": "y"})

    def fence(self, x, y0, y1, z):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, FENCE, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})

    def slab(self, x, y, z, block=SLAB_WOOD, kind="bottom"):
        self.set(x, y, z, block, {"type": kind, "waterlogged": "false"})

    def kop(self, x, y, z):
        self.set(x, y, z, KOP, {"facing": "north"})


# --- the forts ------------------------------------------------------------------------------------------------------------------
def fort_01():
    """Het Mikahutje: a little wooden hut, one Mika on the roof, a crate inside. A good one to start with."""
    f = Fort()
    f.walls(4, 0, 3, 8, 2, 6, PLANK)
    for y in (0, 1):
        f.set(6, y, 3, "minecraft:air")
    f.blocks.pop((6, 0, 3), None)
    f.blocks.pop((6, 1, 3), None)
    f.floor(4, 3, 8, 6, 3, PLANK)
    f.kist(6, 0, 5)
    f.mika(6, 4, 4)
    f.slab(4, 4, 3)
    f.slab(8, 4, 3)
    f.set(5, 4, 5, WOOL)
    f.set(7, 4, 5, WOOL)
    return f


def fort_02():
    """De Wiebeltoren: a thin tall tower of planks with a Mika right on top, and a crate at its foot."""
    f = Fort()
    for y in range(0, 9):
        f.walls(5, y, 3, 7, y, 5, PLANK if y % 3 else LOG, {"axis": "y"} if y % 3 == 0 else None)
    f.floor(5, 3, 7, 5, 9, PLANK)
    f.mika(6, 10, 4)
    f.fence(5, 10, 10, 3)
    f.fence(7, 10, 10, 3)
    f.kist(3, 0, 4)
    f.kist(9, 0, 4)
    f.set(9, 1, 4, WOOL)
    return f


def fort_03():
    """De Twee Torentjes: two towers with a wooden bridge between them; a Mika on each tower, a crate on the bridge."""
    f = Fort()
    for x0 in (1, 9):
        f.walls(x0, 0, 3, x0 + 2, 5, 5, STONE if x0 == 1 else PLANK)
        f.floor(x0, 3, x0 + 2, 5, 6, PLANK)
        f.mika(x0 + 1, 7, 4)
        f.set(x0, 7, 3, FENCE, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
    f.fill(4, 6, 4, 8, 6, 4, PLANK)
    f.kist(6, 7, 4)
    f.fence(6, 0, 5, 4)                                          # a wobbly pole under the middle of the bridge
    return f


def fort_04():
    """Het IJspaleisje: walls of packed ice and purple glass (they break easily!), two Mikas and two crates inside."""
    f = Fort()
    f.walls(2, 0, 2, 10, 4, 6, ICE)
    for x in (4, 6, 8):
        for y in (1, 2):
            f.set(x, y, 2, GLASS)
    f.floor(2, 2, 10, 6, 5, BLUEICE)
    f.walls(4, 6, 3, 8, 7, 5, GLASS)
    f.floor(4, 3, 8, 5, 8, ICE)
    f.mika(4, 0, 4)
    f.mika(6, 6, 4)
    f.kist(8, 0, 4)
    f.kist(6, 0, 5)
    f.set(6, 9, 4, ICE)
    return f


def fort_05():
    """De Stenen Muur: a thick stone wall at the front; behind it a wooden stack with the Mikas. Shoot over it!"""
    f = Fort()
    f.fill(1, 0, 1, 11, 4, 1, STONE)
    for x in (1, 3, 5, 7, 9, 11):
        f.set(x, 5, 1, STONE)
    f.walls(3, 0, 5, 9, 3, 7, PLANK)
    f.floor(3, 5, 9, 7, 4, PLANK)
    f.mika(4, 5, 6)
    f.mika(8, 5, 6)
    f.kist(6, 0, 6)
    f.set(6, 5, 6, WOOL)
    f.set(6, 6, 6, WOOL)
    return f


def fort_06():
    """De Mikapiramide: steps of stone and wood; Mikas on the steps, a crate at the very top."""
    f = Fort()
    for lvl in range(5):
        a, b = lvl, 12 - lvl
        za, zb = 1 + lvl // 2, 7 - lvl // 2
        f.fill(a, lvl, za, b, lvl, zb, STONE if lvl % 2 == 0 else PLANK)
    f.mika(1, 1, 4)
    f.mika(11, 1, 4)
    f.mika(6, 5, 3)
    f.kist(6, 5, 5)
    f.kist(3, 3, 4)
    return f


def fort_07():
    """De Wolkenkrabber: a tall tower of wood, glass and stone, with a Mika on three floors."""
    f = Fort()
    mats = [STONE, STONE, PLANK, PLANK, GLASS, PLANK, PLANK, GLASS, PLANK, PLANK, GLASS, PLANK]
    for y, m in enumerate(mats):
        f.walls(4, y, 2, 8, y, 6, m)
        if y in (3, 7, 11):
            f.floor(4, 2, 8, 6, y, PLANK)
    for y in (4, 8):
        f.mika(6, y, 4)
    f.mika(6, 12, 4)
    f.kist(6, 0, 4)
    for x in (4, 8):
        f.set(x, 12, 2, FENCE, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
    return f


def fort_08():
    """Het Mika-kasteeltje: four little corner towers and a keep with glass windows; three Mikas and two crates."""
    f = Fort()
    for (x0, z0) in ((0, 0), (10, 0), (0, 6), (10, 6)):
        f.walls(x0, 0, z0, x0 + 2, 5, z0 + 2, STONE)
        f.floor(x0, z0, x0 + 2, z0 + 2, 6, SLAB_STONE, {"type": "bottom", "waterlogged": "false"})
    f.fill(3, 0, 1, 9, 2, 1, PLANK)
    for x in (4, 6, 8):
        f.set(x, 1, 1, GLASS)
    f.walls(4, 0, 3, 8, 6, 6, PLANK)
    for y in (2, 4):
        f.set(6, y, 3, GLASS)
    f.floor(4, 3, 8, 6, 7, PLANK)
    f.mika(1, 7, 1)
    f.mika(11, 7, 7)
    f.mika(6, 8, 4)
    f.kist(6, 0, 5)
    f.kist(6, 3, 1)
    f.kop(6, 8, 6)
    return f


def fort_09():
    """De Hangbrug: two stone pillars with a long wooden bridge, held up in the middle by one thin glass pole. Hit the pole!"""
    f = Fort()
    for x in (1, 11):
        f.walls(x - 1, 0, 3, x + 1, 5, 5, STONE)
    f.fill(0, 6, 3, 12, 6, 5, PLANK)
    f.fill(6, 0, 4, 6, 5, 4, GLASS)
    f.mika(3, 7, 4)
    f.mika(9, 7, 4)
    f.kist(6, 7, 4)
    for x in (0, 12):
        f.fence(x, 7, 8, 3)
    return f


def fort_10():
    """Het Knabbelpakhuis: a wooden warehouse stuffed with crates of stolen kaasknabbels, two Mikas keep watch."""
    f = Fort()
    f.walls(1, 0, 2, 11, 4, 7, PLANK)
    for x in (1, 11):
        for z in (2, 7):
            f.log(x, 0, z, x, 5, z)
    f.floor(1, 2, 11, 7, 5, PLANK)
    f.log(6, 0, 4, 6, 4, 4)                                      # the middle post that holds the loft
    for x in (3, 5, 7, 9):
        f.kist(x, 0, 4)
    f.kist(5, 1, 4)
    f.kist(7, 1, 4)
    for x in range(4, 9):                                        # a slit in the front wall: you can see the crates
        f.blocks.pop((x, 1, 2), None)
    f.fill(4, 6, 3, 8, 6, 6, PLANK)
    f.mika(4, 7, 4)
    f.mika(8, 7, 4)
    f.slab(6, 7, 4, SLAB_WOOD)
    return f


def fort_11():
    """De Mikamolen: a naughty Mika windmill (a Dutch mill, but Mika-style): a stone foot with a little door, a wooden
    body, a cap, wool sails on a hub, a Mika in the cap and one hiding in the foot."""
    f = Fort()
    f.walls(4, 0, 3, 8, 3, 7, STONE)
    f.blocks.pop((6, 0, 3), None)
    f.blocks.pop((6, 1, 3), None)
    f.walls(4, 4, 3, 8, 8, 7, PLANK)
    for y in (5, 7):
        f.set(4, y, 5, GLASS)
        f.set(8, y, 5, GLASS)
    f.floor(4, 3, 8, 7, 9, PLANK)
    f.walls(5, 10, 4, 7, 11, 6, PLANK)
    f.floor(5, 4, 7, 6, 12, PLANK)
    f.mika(6, 13, 5)
    f.set(6, 6, 2, LOG, {"axis": "z"})
    for d in (1, 2):                                             # the sails: a cross of wool round the hub
        for sx, sy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            f.set(6 + sx * d, 6 + sy * d, 2, PINKWOOL if d == 1 else WOOL)
    f.mika(6, 0, 5)
    f.kist(4, 10, 3)
    f.kist(8, 10, 7)
    return f


def fort_12():
    """De Grote Mikaburcht: the big one. A stone base, wooden halls, glass windows; four Mikas and three crates."""
    f = Fort()
    f.walls(0, 0, 1, 12, 3, 8, STONE)
    for x in (2, 4, 8, 10):
        f.set(x, 2, 1, GLASS)
    f.floor(0, 1, 12, 8, 4, PLANK)
    f.walls(2, 5, 2, 10, 8, 7, PLANK)
    for x in (4, 6, 8):
        f.set(x, 6, 2, GLASS)
        f.set(x, 7, 2, GLASS)
    f.floor(2, 2, 10, 7, 9, PLANK)
    f.walls(4, 10, 3, 8, 11, 6, STONE)
    f.floor(4, 3, 8, 6, 12, PLANK)
    f.mika(1, 0, 4)
    f.mika(11, 0, 4)
    f.mika(6, 5, 5)
    f.mika(6, 13, 4)
    f.kist(6, 0, 5)
    f.kist(3, 5, 4)
    f.kist(9, 5, 4)
    for x in (0, 12):
        for z in (1, 8):
            f.set(x, 5, z, STONE)
            f.kop(x, 6, z) if z == 1 else f.set(x, 6, z, SLAB_STONE, {"type": "bottom", "waterlogged": "false"})
    return f


FORTS = [fort_01, fort_02, fort_03, fort_04, fort_05, fort_06, fort_07, fort_08, fort_09, fort_10, fort_11, fort_12]


# --- the checks (the same rule as KatapultFort.unstable) -----------------------------------------------------------------------------
def unstable(positions):
    dist = {}
    q = deque()
    for p in positions:
        if p[1] == 0:
            dist[p] = 0
            q.append(p)
    while q:
        p = q.popleft()
        d = dist[p]
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (p[0] + dx, p[1] + dy, p[2] + dz)
            if n not in positions:
                continue
            nd = d + (0 if dy == 1 else 1)
            if nd > OVERHANG:
                continue
            if n not in dist or nd < dist[n]:
                dist[n] = nd
                if dy == 1:
                    q.appendleft(n)
                else:
                    q.append(n)
    return sorted(set(positions) - set(dist))


def check(f, i):
    problems = []
    solid = {p for p, (b, _) in f.blocks.items() if b != "minecraft:air"}
    if not any(b == MIKA for b, _ in f.blocks.values()):
        problems.append(f"fort {i}: no Mika")
    bad = unstable(solid)
    if bad:
        problems.append(f"fort {i} ({NAMES[i - 1]}): {len(bad)} blocks don't hold, e.g. {bad[:6]}")
    for (x, y, z), (b, _) in f.blocks.items():
        if b == MIKA or b == KIST:
            above = f.blocks.get((x, y + 1, z))
            if above and above[0] in (STONE, PILLAR) and b == MIKA:
                problems.append(f"fort {i}: a Mika at {(x, y, z)} is buried under stone")
    return problems


def build(h):
    """Writes the 12 fort templates; returns their blocks (for the castle's first fort)."""
    problems = []
    forts = []
    for i, make in enumerate(FORTS, 1):
        f = make()
        forts.append(f)
        problems += check(f, i)
        s = h.Structure((W, H, D))
        for (x, y, z), (b, props) in f.blocks.items():
            if b != "minecraft:air":
                s.set(x, y, z, b, props)
        s.save(f"katapult/fort_{i:02d}")
    if problems:
        raise SystemExit("katapult forts self-check failed:\n  " + "\n  ".join(problems))
    return forts


def counts(f):
    blocks = [b for b, _ in f.blocks.values() if b != "minecraft:air"]
    return blocks.count(MIKA), blocks.count(KIST), len(blocks)


if __name__ == "__main__":
    all_problems = []
    for i, make in enumerate(FORTS, 1):
        f = make()
        m, k, n = counts(f)
        pr = check(f, i)
        all_problems += pr
        print(f"{i:2d} {NAMES[i - 1]:24s} mikas {m} kisten {k} blocks {n} {'OK' if not pr else pr}")
    if all_problems:
        raise SystemExit(1)
