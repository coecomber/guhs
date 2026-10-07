"""
einde2 (the Guheinde expansion), slice vesting: the pieces of the modular Mika-vesting (not a feature itself: einde2_vesting
calls it).

The vesting of the outer islands used to be one template (a tower, with or without a vetschip: features/guheinde_bouw.py).
Now it is put together like an End City, from jigsaw pieces (templates einde2_vesting/<stuk>, pools einde2_vesting/<pool>):

  the keep      kern (the start: the wachtlokaal on a plinth, with the vesting-kapitein's desk) with a fundering under it,
                then always, stacked: keuken (the spijtige Mika-kok's kitchen) -> slaapzaal or depot -> troon (the throne
                room with the schatkamer) -> dak. One spiral staircase runs through all of them;
  on the ground a cellenblok with six magere guhs (north) and a bit of yard (east): the duwpop or the knabbelkar;
  the harbour   from the second storey a steiger (with the havenbord: where the notice for the vetbootje will hang) to ONE
                of three vetschepen: the classic two-master, a sloep, or a hull under a vetblaas;
  the rest      bridges (short, long, roofed) from the kitchen (west) and the second storey (north) to side towers that
                float on a pointed foot: a wachttoren, a kerkertoren (two more cells) or a round uitkijktoren, each with a
                room and a roof on top and maybe another bridge. What does not fit ends in a balkon or a lookout.

Every piece is drawn in its own coordinates, has its jigsaw blocks (Stuk.jig), its markers (einde2_kit.Bouw) and a
self-check (controleer): nothing outside the template, nothing floating, every lamp and sign on something, every marker and
chest within reach, every walkable floor lit. The pools say what may hang on what (POOLS); einde2_vesting_proef.py puts
whole vestingen together the way the game does and walks through them.

The game's rules that shape all this (JigsawPlacement): a child piece lies wholly outside every other piece's box; the
jigsaws of a piece are tried in the order of their selection priority and a child's own children wait in a queue ordered by
the placement priority of the jigsaw it hangs on. So the keep and the steiger (priority 10) are complete before the first
side tower is tried: what a vesting always has, it always has.
"""
import math
import random

import features.einde2_kit as kit
from features import guheinde_bouw as gb
from features.guheinde_bouw import AIR, GRAFFITI, MIKA_FACE, MSTEEN, lamp, slab, stair

NS = "einde2_vesting"
PREFIX = "sign.guhs.einde2_vesting"
PILAAR, TRAP, PLAAT, LAMP = "guhs:mika_steen_pilaar", "guhs:mika_steen_trap", "guhs:mika_steen_plaat", "guhs:guh_kristal_lamp"
KORST, KNABBELS = "guhs:kaaskorst", "guhs:block_of_kaasknabbels"
AS_Y = {"axis": "y"}
LEEG = "minecraft:empty"
ANKER = "guhs:mika_vesting_midden"

P = 8                   # a storey: its floor and seven blocks of room
HN, TN = 15, 11         # the keep and a side tower, outside
TR, CR, ER, FR = 8, 4, 3, 7     # how deep the foot of a side tower, the cellenblok, the yard and the keep goes
MAX_BOVEN = 44          # no block of a vesting lies more than this above the floor of its start (build contract 6.2)
MAX_AFSTAND = 80        # jigsaw max_distance_from_center: two vestingen (at least 192 blocks apart) never meet
DIEPTE = 7              # jigsaw size

KIST_GEWOON, KIST_GOED, KIST_SCHAT = "guhs:chests/einde2_vesting_gewoon", "guhs:chests/einde2_vesting_goed", "guhs:chests/einde2_vesting_schat"
KIST_SCHIP = "guhs:chests/vetschip"
# entity tags (the Java side knows a cell guh and the wings of a vetschip by them: VestingCellen, VestingVleugels)
TAG_CEL, TAG_VLEUGELS = "guhs_einde2_vesting_cel", "guhs_einde2_vesting_vleugels"


def nid(n):
    return n if ":" in n else f"guhs:{NS}/{n}"


# =====================================================================================================================
# a piece in the making
# =====================================================================================================================
class Stuk(kit.Bouw):
    """One jigsaw piece (template einde2_vesting/<naam>)."""

    def __init__(self, h, naam, size):
        super().__init__(h, f"{NS}/{naam}", size, kit.seed(f"vesting/{naam}"), prefix=PREFIX)
        self.naam = naam
        self.jigs = []          # the jigsaw blocks: dicts (pos, richting, naam, doel, pool, sel, plc, final)
        self.begin = None       # where the self-check starts walking (None: nothing to walk on)
        self.lijsten = []       # the cells item frames hang in
        self.vloer = 0          # the y of the floor a player walks in on
        self.wel = set()        # (x, z) of the pillar of its spiral staircase

    def jig(self, x, y, z, richting, naam="", doel="", pool="", final=None, sel=0, plc=0):
        """A jigsaw block. richting: where it points (up, down, north...). naam: what it is called (a child hangs by this
        name); doel + pool: the name it asks for and the pool the child comes from. It turns into `final` (default: the
        block that stood there)."""
        if final is None:
            er = self.s.blocks.get((x, y, z))
            final = MSTEEN if er is None else er[0] + ("[" + ",".join(f"{k}={v}" for k, v in sorted(er[1].items())) + "]" if er[1] else "")
        orient = f"{richting}_north" if richting in ("up", "down") else f"{richting}_up"
        self.set(x, y, z, "jigsaw", {"orientation": orient},
                 {"id": "minecraft:jigsaw", "name": nid(naam) if naam else LEEG, "target": nid(doel) if doel else LEEG,
                  "pool": nid(pool) if pool else LEEG, "final_state": final, "joint": "aligned", "placement_priority": plc,
                  "selection_priority": sel})
        self.jigs.append({"pos": (x, y, z), "richting": richting, "naam": naam, "doel": doel, "pool": pool, "sel": sel, "plc": plc,
                          "final": final})

    def mika(self, x, y, z, yaw=0.0):
        gb.mika(self, x, y, z, yaw)

    def ton(self, x, y, z, n=1):
        for i in range(n):
            self.set(x, y + i, z, "barrel", {"facing": "up", "open": "false"})

    def mager(self, x, y, z, yaw=0.0):
        """A magere guh in its cell (the tag is how VestingCellen knows it)."""
        ms = self.h.ms
        self.ent(x + 0.5, float(y), z + 0.5, {"id": "guhs:guh", "Variant": "mager", "PersistenceRequired": ms.Byte(1), "Age": 0,
                                              "Rotation": ms.floats(float(yaw), 0.0), "Tags": ms.NbtList(8, [TAG_CEL])}, "mager")

    def schuilguh(self, x, y, z, kant="down"):
        """A Schuilguh on the block at that side of it. A piece is turned when it is placed and the face in its NBT is not:
        the Schuilguh takes hold of the block that is really there within half a second (it holds one block only)."""
        ms = self.h.ms
        self.ent(x + 0.5, float(y), z + 0.5, {"id": "guhs:schuilguh", "PersistenceRequired": ms.Byte(1), "Honk": 0,
                                              "AttachFace": ms.Byte(gb.FACING_BYTE[kant]), "Rotation": ms.floats(0.0, 0.0)}, "schuilguh")

    def vleugels(self, x, y, z, kant):
        """The Guhvleugels in an item frame in this cell, on the wall at that side of it. Fixed and tagged: a player does
        not take them out, VestingVleugels hands every player a pair of their own."""
        ms = self.h.ms
        dx, dz = gb.DIRS[kant]
        kijkt = gb.OPP[kant]
        self.ent(x + 0.5 + dx * 0.46875, y + 0.5, z + 0.5 + dz * 0.46875,
                 {"id": "minecraft:item_frame", "Facing": ms.Byte(gb.FACING_BYTE[kijkt]), "Item": {"id": "guhs:guhvleugels", "count": 1},
                  "Invulnerable": ms.Byte(1), "Fixed": ms.Byte(1), "Tags": ms.NbtList(8, [TAG_VLEUGELS])}, "frame")
        self.lijsten.append(((x, y, z), kant))


class Op:
    """A piece seen from a shifted origin, so a storey is drawn in the coordinates of its tower."""

    def __init__(self, b, ox=0, oy=0, oz=0):
        self.b, self.o = b, (ox, oy, oz)

    def _p(self, x, y, z):
        return x + self.o[0], y + self.o[1], z + self.o[2]

    def set(self, x, y, z, *a, **k):
        self.b.set(*self._p(x, y, z), *a, **k)

    def get(self, x, y, z):
        return self.b.get(*self._p(x, y, z))

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        self.b.fill(*self._p(x0, y0, z0), *self._p(x1, y1, z1), name, props)

    def chest(self, x, y, z, facing, loot):
        self.b.chest(*self._p(x, y, z), facing, loot)

    def sign(self, x, y, z, facing, lines, **k):
        self.b.sign(*self._p(x, y, z), facing, lines, **k)

    def art(self, rows, pal, plane, fixed, u0, ytop):
        ox, oy, oz = self.o
        if plane == "x":
            self.b.art(rows, pal, "x", fixed + ox, u0 + oz, ytop + oy)
        else:
            self.b.art(rows, pal, "z", fixed + oz, u0 + ox, ytop + oy)

    def jig(self, x, y, z, richting, **k):
        self.b.jig(*self._p(x, y, z), richting, **k)

    def plek(self, id, x, y, z, **k):
        self.b.plek(id, *self._p(x, y, z), **k)

    def prop(self, id, x, y, z, **k):
        self.b.prop(id, *self._p(x, y, z), **k)

    def zone(self, id, x0, y0, z0, x1, y1, z1, **k):
        self.b.zone(id, *self._p(x0, y0, z0), *self._p(x1, y1, z1), **k)

    def mika(self, x, y, z, yaw=0.0):
        self.b.mika(*self._p(x, y, z), yaw)

    def mager(self, x, y, z, yaw=0.0):
        self.b.mager(*self._p(x, y, z), yaw)

    def schuilguh(self, x, y, z, kant="down"):
        self.b.schuilguh(*self._p(x, y, z), kant)

    def deur(self, x, y, z, facing, wood="crimson", hinge="left"):
        gb.door(self.b, *self._p(x, y, z), facing, wood=wood, hinge=hinge)

    def ton(self, x, y, z, n=1):
        self.b.ton(*self._p(x, y, z), n)


# =====================================================================================================================
# what every tower is made of
# =====================================================================================================================
# the spiral staircase: eight steps around a pillar, one storey per turn. Seen from the pillar (cx, cz): step k stands at
# SPIRAAL[k] on y + 1 + k; the eighth step lies IN the floor of the storey above, at (cx + 1, cz + 1): there you step off to
# the east or the south, or walk on up. So every storey of a tower has the same stairwell and they stack in any order.
SPIRAAL = [((0, 1), "west"), ((-1, 1), "west"), ((-1, 0), "north"), ((-1, -1), "north"), ((0, -1), "east"), ((1, -1), "east"), ((1, 0), "south")]


def _wel(t, cx, cz):
    """Remembers where the stairwell of a piece is (its pillar stands on the pillar of the piece below: the self-check of
    one piece must not call that floating)."""
    b, (ox, _, oz) = (t.b, t.o) if isinstance(t, Op) else (t, (0, 0, 0))
    b.wel.add((cx + ox, cz + oz))


def spiraal(t, cx, y0, cz):
    _wel(t, cx, cz)
    for k, ((dx, dz), kant) in enumerate(SPIRAAL):
        t.set(cx + dx, y0 + 1 + k, cz + dz, TRAP, stair(kant))
        if k:                                        # (the underside: an upside-down step, so the stair is one ribbon)
            t.set(cx + dx, y0 + k, cz + dz, TRAP, stair(gb.OPP[kant], "top"))
    t.set(cx + 1, y0 + P - 1, cz + 1, TRAP, stair("north", "top"))
    for y in range(y0 + 1, y0 + P):
        t.set(cx, y, cz, LAMP if y == y0 + 4 else PILAAR, None if y == y0 + 4 else AS_Y)


def trapgat(t, cx, y0, cz):
    """The floor of a storey that has a spiral under it: the stairwell is open and the last step lies in the floor."""
    _wel(t, cx, cz)
    for (dx, dz), _ in SPIRAAL:
        t.set(cx + dx, y0, cz + dz, AIR)
    t.set(cx + 1, y0, cz + 1, TRAP, stair("south"))
    t.set(cx, y0, cz, PILAAR, AS_Y)


def vierkant(t, n, y0, hoog=P, lampen=True):
    """One storey of a square tower: the floor (crystal lamps in it: the light of this storey and of the one below), the
    walls with their corner pillars, and air inside (so an island never pokes through a room)."""
    m = n - 1
    for x in range(n):
        for z in range(n):
            rand, hoek = x in (0, m) or z in (0, m), x in (0, m) and z in (0, m)
            if hoek:
                t.set(x, y0, z, PILAAR, AS_Y)
            elif rand:
                t.set(x, y0, z, PILAAR, {"axis": "x" if z in (0, m) else "z"})
            else:
                t.set(x, y0, z, LAMP if lampen and x % 4 == 2 and z % 4 == 2 else MSTEEN)
            for y in range(y0 + 1, y0 + hoog):
                if hoek:
                    t.set(x, y, z, PILAAR, AS_Y)
                else:
                    t.set(x, y, z, MSTEEN if rand else AIR)


def rond_soort(x, z):
    """A cell of the round tower (11 x 11, middle 5, 5): 'in' (the room), 'muur' or None (outside)."""
    d = math.hypot(x - 5, z - 5)
    return "in" if d <= 4.4 else "muur" if d <= 5.4 else None


def rond(t, y0, hoog=P, lampen=True):
    for x in range(TN):
        for z in range(TN):
            soort = rond_soort(x, z)
            if soort is None:
                continue
            if soort == "in":
                t.set(x, y0, z, LAMP if lampen and (x, z) in ((5, 2), (5, 8), (2, 5), (8, 5)) else MSTEEN)
            else:
                t.set(x, y0, z, PILAAR, AS_Y)
            for y in range(y0 + 1, y0 + hoog):
                t.set(x, y, z, MSTEEN if soort == "muur" else AIR)


def ramen(t, y0, cellen, glas="purple_stained_glass_pane"):
    for (x, z) in cellen:
        for y in (y0 + 3, y0 + 4):
            if t.get(x, y, z) == MSTEEN:
                t.set(x, y, z, glas)


def voet(t, n, diep, rond_=False, seed=0):
    """The pointed foot under a floating piece: layer after layer smaller, down to one glowing block (y 0 .. diep - 1)."""
    rng = random.Random(seed)
    c = (n - 1) / 2
    for k in range(diep):
        y = diep - 1 - k
        r = (n / 2 + 0.2) * (1 - (k + 1) / (diep + 0.6)) ** 0.8
        for x in range(n):
            for z in range(n):
                dx, dz = abs(x - c), abs(z - c)
                d = math.hypot(dx, dz) if rond_ else 0.55 * max(dx, dz) + 0.45 * math.hypot(dx, dz)
                if d <= r + 0.25 or (dx, dz) == (0, 0):
                    if k == diep - 1 and (dx, dz) == (0, 0):
                        t.set(x, y, z, LAMP)
                    elif k == 0 and d > r - 1.0:
                        t.set(x, y, z, PILAAR, AS_Y)
                    else:
                        t.set(x, y, z, KORST if k >= 2 and rng.random() < 0.18 + 0.08 * k else MSTEEN)


def kantelen(t, n, y, sla_over=()):
    """Battlements on the edge of a square roof: every other block, pillars with a lampion on the corners."""
    m = n - 1
    for x in range(n):
        for z in range(n):
            if not (x in (0, m) or z in (0, m)) or (x, z) in sla_over:
                continue
            if x in (0, m) and z in (0, m):
                t.set(x, y, z, PILAAR, AS_Y)
                t.set(x, y + 1, z, PILAAR, AS_Y)
                t.set(x, y + 2, z, "guhs:lampion_roze", lamp(False))
            else:
                t.set(x, y, z, MSTEEN)
                if (x + z) % 2 == 0:
                    t.set(x, y + 1, z, PLAAT, slab())


# =====================================================================================================================
# the keep
# =====================================================================================================================
def kern(h):
    """The start: the ground floor of the keep (the wachtlokaal) on a plinth of 19 x 19. Its anchor lies in the middle of
    the Mika face in the floor."""
    b = Stuk(h, "kern", (19, P, 19))
    for x in range(19):
        for z in range(19):
            ring = min(x, z, 18 - x, 18 - z)
            if ring == 0:
                hoek = x in (0, 18) and z in (0, 18)
                b.set(x, 0, z, PILAAR, AS_Y if hoek else {"axis": "x" if z in (0, 18) else "z"})
            elif ring == 1:
                b.set(x, 0, z, MSTEEN)
            if ring < 2:
                for y in range(1, 5):
                    b.set(x, y, z, AIR)
    for (x, z) in ((0, 0), (18, 0), (0, 18), (18, 18)):
        b.set(x, 1, z, PILAAR, AS_Y)
        b.set(x, 2, z, "guhs:lampion_roze", lamp(False))
    for (x, z) in ((9, 0), (0, 9), (18, 9), (5, 18), (13, 18)):
        b.set(x, 0, z, LAMP)
    t = Op(b, 2, 0, 2)
    vierkant(t, HN, 0)
    spiraal(t, 2, 0, 2)
    # the gate (south) with its arch, the back door to the cellenblok (north)
    for x in (6, 7, 8):
        for y in (1, 2, 3):
            t.set(x, y, 14, AIR)
    t.set(7, 4, 14, AIR)
    t.set(6, 4, 14, TRAP, stair("west", "top"))
    t.set(8, 4, 14, TRAP, stair("east", "top"))
    for x in (6, 7, 8):
        t.set(x, 1, 13, "purple_carpet")
        t.set(x, 1, 12, "purple_carpet")
    t.deur(7, 1, 0, "south")
    # Mika's face in the floor, and outside on the walls
    for j, row in enumerate(MIKA_FACE):
        for i, ch in enumerate(row):
            t.set(4 + i, 0, 5 + j, GRAFFITI[ch])
    t.art(MIKA_FACE, GRAFFITI, "x", 14, 4, 7)
    t.art(MIKA_FACE, GRAFFITI, "x", 0, 6, 7)
    ramen(t, 0, [(3, 14), (11, 14), (10, 0), (12, 0), (4, 0)])
    # the kapitein's desk, the wanted poster, the guard's things
    t.set(10, 1, 4, "guhs:guh_tafel", {"facing": "south"})
    t.set(11, 1, 4, "guhs:guh_tafel", {"facing": "south"})
    t.set(11, 2, 4, "guhs:lampion_geel", lamp(False))
    t.plek("kapitein", 10, 1, 3, yaw=0.0, wie="de vesting-kapitein, achter zijn bureau", kind="VESTINGKAPITEIN_MIKA")
    t.sign(10, 3, 1, "south", ["KAPITEIN", "van de vesting", "Klachten?", "NJEG."], wood="crimson")
    t.sign(13, 2, 8, "west", ["GEZOCHT:", "vadsige guhs.", "Beloning:", "0 knabbels."], wood="crimson")
    t.ton(12, 1, 1, 2)
    t.ton(13, 1, 1)
    t.ton(13, 1, 2)
    t.chest(13, 1, 12, "west", KIST_GEWOON)
    t.set(1, 1, 11, TRAP, stair("west"))
    t.set(1, 1, 12, TRAP, stair("west"))
    t.set(1, 1, 13, "guhs:lampion_roze", lamp(False))
    t.set(13, 1, 13, "guhs:lampion_roze", lamp(False))
    t.sign(1, 2, 9, "east", ["Voeten vegen!", "(Mika's niet,", "die hebben", "pootjes)"], wood="crimson")
    t.mika(7, 1, 10, 0)
    # what hangs on it: the keep goes up, the foot goes down, the cells stand north, the yard east
    t.jig(0, 7, 0, "up", doel="hoofd_op", pool="hoofd_1", sel=10, plc=10)
    b.anchor(9, 0, 9, ANKER)
    b.jig(9, 0, 10, "down", doel="fundering", pool="fundering", sel=12)
    b.jig(9, 0, 0, "north", doel="cellen_in", pool="cellen", sel=11)
    b.jig(18, 0, 9, "east", doel="erf_in", pool="erf", sel=9)
    b.begin = (9, 1, 17)
    return b


def fundering(h):
    """The foot of the keep: Mika-steen that turns into the island's own kaaskorst, down to a point."""
    b = Stuk(h, "fundering", (19, FR, 19))
    voet(b, 19, FR, seed=11)
    for x in range(19):
        for z in range(19):
            b.set(x, FR - 1, z, PILAAR if min(x, z, 18 - x, 18 - z) == 0 else MSTEEN, AS_Y if min(x, z, 18 - x, 18 - z) == 0 else None)
    b.jig(9, FR - 1, 10, "up", naam="fundering")
    return b


def hoofd(h, naam, pool_op, deuren):
    """A storey of the keep (15 x 8 x 15) with its shell, its stairwell and its jigsaws; deuren: the sides with a door."""
    b = Stuk(h, naam, (HN, P, HN))
    vierkant(b, HN, 0)
    trapgat(b, 2, 0, 2)
    spiraal(b, 2, 0, 2)
    t = Op(b)
    b.jig(0, 0, 0, "down", naam="hoofd_op")
    if pool_op:
        b.jig(0, 7, 0, "up", doel="hoofd_op", pool=pool_op, sel=10, plc=10)
    for kant in deuren:
        x, z = {"west": (0, 7), "east": (14, 7), "north": (7, 0), "south": (7, 14)}[kant]
        t.deur(x, 1, z, gb.OPP[kant])
    b.begin = (4, 1, 4)
    return b, t


def keuken(h):
    """The first storey: the kitchen of the spijtige Mika-kok (his spot and the room are markers; he comes in phase B)."""
    b, t = hoofd(h, "keuken", "hoofd_2", ("west", "south"))
    b.jig(0, 0, 7, "west", doel="brug_in", pool="brug")
    b.jig(7, 0, 14, "south", doel="brug_in", pool="balkon", sel=5)
    # the cooking wall (north): vats, two smokers, the soup kettle
    t.ton(6, 1, 1)
    t.set(7, 1, 1, "smoker", {"facing": "south", "lit": "false"})
    t.set(8, 1, 1, "smoker", {"facing": "south", "lit": "false"})
    t.set(9, 1, 1, "water_cauldron", {"level": "3"})
    t.set(10, 1, 1, "furnace", {"facing": "south", "lit": "false"})
    t.ton(11, 1, 1, 2)
    t.ton(12, 1, 1)
    t.set(13, 1, 1, "composter", {"level": "5"})
    t.set(7, 2, 1, "guhs:lampion_geel", lamp(False))
    t.plek("kok", 9, 1, 3, yaw=0.0, wie="de spijtige Mika-kok, bij zijn soepketel", kind="MIKA_KOK")
    t.zone("keuken", 1, 1, 1, 13, 6, 13, wie="de keuken van de vesting")
    t.sign(4, 3, 1, "south", ["KEUKEN", "Vandaag:", "knabbelsoep", "(zonder knabbel)"], wood="crimson")
    # the table in the middle, with what is left of the cake
    for x in (6, 7, 8):
        t.set(x, 1, 7, "guhs:guh_tafel", {"facing": "south"})
    t.set(7, 2, 7, "cake", {"bites": "4"})
    t.set(6, 2, 7, "guhs:lampion_roze", lamp(False))
    for x in (6, 8):
        t.set(x, 1, 9, TRAP, stair("south"))
    # the pantry corner (south-east), and what the cook keeps behind it for the cells
    for (x, z, n) in ((13, 9, 2), (13, 10, 3), (13, 11, 2), (12, 13, 2), (11, 13, 1), (12, 11, 1)):
        t.ton(x, 1, z, n)
    t.chest(13, 1, 13, "west", KIST_GEWOON)
    t.sign(13, 2, 12, "west", ["Extra portie", "voor de cellen.", "Sssst! Niet", "verklappen."], wood="crimson")
    t.set(10, 1, 13, KNABBELS)
    t.set(1, 1, 13, "guhs:lampion_roze", lamp(False))
    t.set(1, 1, 9, KNABBELS)
    t.set(1, 2, 9, "guhs:lampion_geel", lamp(False))
    ramen(t, 0, [(4, 0), (10, 0), (12, 0), (14, 4), (14, 7), (14, 10), (0, 11), (3, 14), (11, 14)])
    t.mika(5, 1, 10, 180)
    return b


def slaapzaal(h):
    """The second storey, one of two: the dormitory. A door north (a bridge) and east (the steiger)."""
    b, t = hoofd(h, "slaapzaal", "hoofd_3", ("north", "east"))
    tweede(b)
    for x in (3, 6, 9, 12):
        t.set(x, 1, 13, "purple_bed", {"facing": "south", "part": "head", "occupied": "false"})
        t.set(x, 1, 12, "purple_bed", {"facing": "south", "part": "foot", "occupied": "false"})
    for x in (4, 7, 10):
        t.ton(x, 1, 13)
        t.set(x, 2, 13, "guhs:lampion_roze", lamp(False))
    for z in (9, 10):
        for x in range(3, 13):
            t.set(x, 1, z, "purple_carpet")
    t.chest(13, 1, 2, "west", KIST_GEWOON)
    t.ton(13, 1, 1, 2)
    t.set(1, 1, 13, "guhs:lampion_roze", lamp(False))
    t.sign(1, 2, 8, "east", ["Stil! Mika", "slaapt.", "(of doet", "alsof)"], wood="crimson")
    t.sign(13, 2, 10, "west", ["Wie snurkt", "slaapt op", "de brug.", "-de kapitein"], wood="crimson")
    ramen(t, 0, [(3, 14), (7, 14), (11, 14), (0, 10), (14, 11), (11, 0)])
    t.mika(8, 1, 10, 90)
    return b


def depot(h):
    """The second storey, the other one: the knabbeldepot (everything 'borrowed')."""
    b, t = hoofd(h, "depot", "hoofd_3", ("north", "east"))
    tweede(b)
    for (x, z, n) in ((1, 13, 3), (2, 13, 2), (1, 12, 2), (3, 13, 1), (1, 11, 1), (13, 13, 3), (12, 13, 3), (13, 12, 2), (11, 13, 1),
                      (13, 11, 1), (13, 1, 2), (12, 1, 1)):
        for y in range(1, 1 + n):
            t.set(x, y, z, KNABBELS)
    for (x, z, n) in ((6, 13, 2), (7, 13, 1), (8, 13, 2), (10, 1, 1), (13, 3, 1)):
        t.ton(x, 1, z, n)
    t.chest(5, 1, 13, "north", KIST_GEWOON)
    t.chest(9, 1, 13, "north", KIST_GEWOON)
    t.set(7, 2, 13, "guhs:lampion_geel", lamp(False))
    t.set(1, 1, 8, "guhs:lampion_geel", lamp(False))
    t.sign(1, 2, 9, "east", ["Gestolen? NEE.", "Geleend.", "Voor altijd.", "-Mika"], wood="crimson")
    t.sign(4, 3, 13, "north", ["TELLING:", "veel.", "Te veel om", "te tellen."], wood="crimson")
    ramen(t, 0, [(3, 14), (11, 14), (0, 10), (14, 11), (11, 0)])
    t.mika(7, 1, 9, 0)
    return b


def tweede(b):
    """What both second storeys have: a bridge north and, east, the steiger with the vetschip (always: priority 10)."""
    b.jig(7, 0, 0, "north", doel="brug_in", pool="brug")
    b.jig(14, 0, 7, "east", doel="steiger_in", pool="steiger", sel=9, plc=10)


def troon(h):
    """The third storey: Mika's throne, and behind bars the schatkamer (the top loot room). No doors: nothing hangs on it."""
    b, t = hoofd(h, "troon", "hoofd_dak", ())
    # the throne against the north wall
    t.set(7, 1, 2, TRAP, stair("north"))
    for y in (1, 2, 3):
        t.set(7, y, 1, PILAAR, AS_Y)
    t.set(7, 4, 1, LAMP)
    for x in (6, 8):
        t.set(x, 1, 2, PLAAT, slab())
        t.set(x, 1, 1, PILAAR, AS_Y)
        t.set(x, 2, 1, PILAAR, AS_Y)
        t.set(x, 3, 1, "guhs:lampion_roze", lamp(False))
    for z in range(3, 9):
        t.set(7, 1, z, "purple_carpet")
    t.sign(9, 2, 1, "south", ["TROON VAN", "MIKA", "Niet zitten!", "(ook niet even)"], wood="crimson")
    # the schatkamer in the south-east corner: walls, bars to look through, a door
    for z in range(8, 14):
        for y in range(1, P):
            t.set(8, y, z, MSTEEN)
    for x in range(8, 14):
        for y in range(1, P):
            t.set(x, y, 8, MSTEEN)
    for z in (9, 10, 12, 13):
        for y in (2, 3):
            t.set(8, y, z, "iron_bars")
    for x in (10, 12):
        for y in (2, 3):
            t.set(x, y, 8, "iron_bars")
    t.deur(8, 1, 11, "east")
    t.zone("schatkamer", 9, 1, 9, 13, 6, 13, wie="de schatkamer van de vesting")
    t.chest(13, 1, 10, "west", KIST_SCHAT)
    t.chest(13, 1, 12, "west", KIST_SCHAT)
    for (x, z, n) in ((13, 13, 3), (12, 13, 2), (11, 13, 1), (13, 9, 2), (12, 9, 1), (13, 11, 1)):
        for y in range(1, 1 + n):
            t.set(x, y, z, KNABBELS)
    t.set(10, 1, 13, "honeycomb_block")
    t.set(10, 2, 13, "guhs:lampion_geel", lamp(False))
    t.set(9, 1, 9, "guhs:lampion_geel", lamp(False))
    t.sign(7, 3, 11, "west", ["SCHATKAMER", "Alles van Mika.", "(was van guhs)", "NJEG!"], wood="crimson")
    # the rest of the hall
    t.art(MIKA_FACE, GRAFFITI, "x", 0, 6, 7)
    t.art(MIKA_FACE, GRAFFITI, "z", 14, 1, 7)
    t.set(1, 1, 13, "guhs:lampion_roze", lamp(False))
    t.set(13, 1, 1, "guhs:lampion_roze", lamp(False))
    t.ton(12, 1, 1)
    ramen(t, 0, [(4, 0), (10, 0), (12, 0), (14, 3), (14, 5), (0, 4), (0, 13)])
    t.mika(7, 1, 5, 0)
    return b


def dak(h):
    """The roof of the keep: battlements, the flag with Mika's face, two Schuilguhs enjoying the view."""
    b = Stuk(h, "dak", (HN, 11, HN))
    t = Op(b)
    vierkant(t, HN, 0, hoog=1)
    trapgat(t, 2, 0, 2)
    kantelen(t, HN, 1)
    for (x, z) in ((7, 3), (11, 7), (7, 11), (3, 7)):
        t.set(x, 0, z, LAMP)
    for y in range(1, 11):
        t.set(7, y, 7, "crimson_fence")
    t.art(MIKA_FACE, GRAFFITI, "z", 7, 8, 10)
    t.sign(7, 1, 8, "south", ["Vesting van", "de Mika's.", "Guhs? NJEG!", ""], wall=False, wood="crimson")
    t.set(12, 1, 12, "barrel", {"facing": "up", "open": "false"})
    t.set(12, 2, 12, "lightning_rod", {"facing": "south", "powered": "false", "waterlogged": "false"})
    t.sign(11, 1, 12, "west", ["VERREKIJKER", "Zie je een guh?", "Roep NJEG.", "(heel hard)"], wall=False, wood="crimson")
    t.schuilguh(12, 1, 2)
    t.schuilguh(2, 1, 12)
    t.mika(9, 1, 5, 180)
    b.jig(0, 0, 0, "down", naam="hoofd_op")
    b.begin = (4, 1, 4)
    return b


# =====================================================================================================================
# on the ground: the cellenblok and the yard
# =====================================================================================================================
CEL_RIJEN = (2, 6, 10)     # the middle z of the three cells on each side of the corridor


def cellenblok(h):
    """Six cells with a magere guh in each, three on each side of a corridor, and the guard's corner by the door (which is
    the keep's back door)."""
    W, D = 15, 16
    b = Stuk(h, "cellenblok", (W, CR + P, D))
    y0 = CR
    t = Op(b, 0, y0, 0)
    for k in range(CR):                              # the foot: a little smaller every layer
        for x in range(k, W - k):
            for z in range(k, D - k):
                b.set(x, CR - 1 - k, z, MSTEEN if k < 2 else KORST)
    for x in range(W):
        for z in range(D):
            rand = x in (0, W - 1) or z in (0, D - 1)
            t.set(x, 0, z, PILAAR if rand else MSTEEN, AS_Y if rand else None)
            for y in range(1, 6):
                t.set(x, y, z, MSTEEN if rand else AIR)
            t.set(x, 6, z, LAMP if x in (2, 7, 12) and z in (2, 6, 10, 13) else MSTEEN)
            if rand and (x + z) % 2 == 0:
                t.set(x, 7, z, PLAAT, slab())
    n = 0
    for zijde, voor, ver, kijkt in (("w", 5, 1, "east"), ("o", 9, 13, "west")):
        binnen = range(1, 5) if zijde == "w" else range(10, 14)
        for z in range(1, 13):                         # the front of the cells
            for y in range(1, 6):
                t.set(voor, y, z, MSTEEN)
        for zc in CEL_RIJEN:
            n += 1
            for x in binnen:                           # the wall behind this cell (the last one: to the guard's corner)
                for y in range(1, 6):
                    t.set(x, y, zc + 2, MSTEEN)
            t.deur(voor, 1, zc, kijkt, wood="oak", hinge="left" if n % 2 else "right")
            for z in (zc - 1, zc + 1):
                for y in (2, 3):
                    t.set(voor, y, z, "iron_bars")
            t.set(ver, 1, zc - 1, "guhs:knabbelbak", {"facing": kijkt})
            t.set(ver, 1, zc + 1, "yellow_carpet")
            t.set(ver, 5, zc, "guhs:lampion_geel", lamp(True))
            gx = 3 if zijde == "w" else 11
            t.mager(gx, 1, zc, yaw=-90 if zijde == "w" else 90)
            t.prop(f"cel_{n}", gx, 1, zc, wie="hier staat de magere guh van deze cel")
    for z in CEL_RIJEN:
        t.set(7, 5, z, "guhs:lampion_roze", lamp(True))
    # the guard's corner by the door
    t.set(2, 1, 14, "guhs:guh_tafel", {"facing": "north"})
    t.set(2, 2, 14, "cake", {"bites": "5"})
    t.set(1, 1, 14, TRAP, stair("west"))
    t.ton(13, 1, 14, 2)
    t.ton(12, 1, 14)
    t.chest(11, 1, 14, "north", KIST_GEWOON)
    t.sign(3, 1, 14, "east", ["Sleutels van", "de cellen?", "Opgegeten.", "-Mika"], wall=False, wood="crimson")
    t.sign(4, 3, 13, "south", ["LOGEERVLEUGEL", "Ontbijt: geen.", "Lunch: geen.", "Diner: NJEG."], wood="crimson")
    t.sign(10, 3, 13, "south", ["Magere guhs?", "Een knabbel", "= VAHOEG!", "(niet doen)"], wood="crimson")
    for y in (2, 3):
        t.set(7, y, 0, "iron_bars")
    for y in (1, 2):
        t.set(7, y, D - 1, AIR)
    t.mika(7, 1, 8, 0)
    t.jig(7, 0, D - 1, "south", naam="cellen_in")
    b.begin = (7, y0 + 1, 14)
    b.vloer = y0
    return b


def erf(h, naam):
    """A bit of paved yard east of the keep, with something of the Mika's on it."""
    b = Stuk(h, naam, (9, ER + 5, 9))
    y0 = ER
    t = Op(b, 0, y0, 0)
    rng = random.Random(kit.seed(naam))
    for x in range(9):
        for z in range(9):
            d = math.hypot(x - 4, z - 4)
            if d > 4.6 and x != 0:
                continue
            if x == 0 and abs(z - 4) > 2:
                continue
            for k in range(ER):
                if d <= 4.6 - 1.3 * (k + 1) or (x, z) == (4, 4):
                    b.set(x, ER - 1 - k, z, KORST)
            t.set(x, 0, z, gb.pick(rng, [(MSTEEN, 60), ("guhs:kaaskorst_stenen", 25), ("guhs:gebarsten_kaaskorst_stenen", 15)]))
            for y in range(1, 5):
                t.set(x, y, z, AIR)
    t.set(4, 0, 4, LAMP)
    if naam == "erf_duwpop":
        # where the Mika's practise shoving: two guh dummies of straw (the dummies win)
        for (x, z) in ((3, 2), (6, 5)):
            t.set(x, 1, z, "crimson_fence")
            t.set(x, 2, z, "hay_block", AS_Y)
            t.set(x, 3, z, "pink_wool")
        t.set(2, 1, 6, "hay_block", AS_Y)
        t.set(2, 2, 6, "guhs:lampion_roze", lamp(False))
        t.sign(4, 1, 7, "north", ["DUWOEFENING", "Pop = guh.", "Duwen maar!", "(de pop wint)"], wall=False, wood="crimson")
    else:
        # a cart full of knabbels, "honestly found"
        for x in (3, 4, 5):
            for z in (2, 3):
                t.set(x, 1, z, "spruce_slab", slab("top"))
        for (x, z, blok) in ((3, 2, KNABBELS), (4, 2, KNABBELS), (4, 3, KNABBELS), (5, 3, "barrel")):
            t.set(x, 2, z, blok, {"facing": "up", "open": "false"} if blok == "barrel" else None)
        t.set(4, 3, 2, KNABBELS)
        for z in (2, 3):
            t.set(6, 1, z, "spruce_fence")
        for (x, z) in ((3, 1), (5, 1), (3, 4), (5, 4)):
            t.set(x, 1, z, "stripped_spruce_log", {"axis": "z"})
        t.set(2, 1, 6, KNABBELS)
        t.set(2, 2, 6, "guhs:lampion_geel", lamp(False))
        t.sign(5, 1, 6, "north", ["Eerlijk", "gevonden.", "(in een", "guhhuisje)"], wall=False, wood="crimson")
    t.jig(0, 0, 4, "west", naam="erf_in")
    b.begin = (1, y0 + 1, 4)
    b.vloer = y0
    return b


# =====================================================================================================================
# bridges, balconies, the steiger
# =====================================================================================================================
def dek(b, lang, palen=True):
    """A bridge deck along x, five wide: three to walk on, a kerb with a fence on each side, lampions on posts."""
    for x in range(lang):
        for z in range(5):
            b.set(x, 0, z, PILAAR if z in (0, 4) else LAMP if z == 2 and x % 4 == 2 else MSTEEN, {"axis": "x"} if z in (0, 4) else None)
        for z in (0, 4):
            if palen and x % 4 == 2:
                b.set(x, 1, z, PILAAR, AS_Y)
                b.set(x, 2, z, "guhs:lampion_roze", lamp(False))
            else:
                b.set(x, 1, z, "crimson_fence")
        for z in (1, 2, 3):
            for y in (1, 2, 3):
                b.set(x, y, z, AIR)


def brug(h, naam, lang):
    b = Stuk(h, naam, (lang, 4, 5))
    dek(b, lang)
    if lang > 10:
        # the long one: a Schuilguh on a post half way, and a warning
        m = lang // 2
        b.set(m, 1, 0, PILAAR, AS_Y)
        b.set(m, 1, 4, PILAAR, AS_Y)
        b.set(m, 2, 4, "guhs:lampion_geel", lamp(False))
        b.schuilguh(m, 2, 0)
        b.sign(m - 1, 1, 3, "west", ["Niet omlaag", "kijken.", "(Mika keek.", "Mika huilde.)"], wall=False, wood="crimson")
    b.jig(0, 0, 2, "west", naam="brug_in")
    b.jig(lang - 1, 0, 2, "east", doel="toren_deur", pool="toren")
    b.begin = (0, 1, 2)
    return b


def brug_overdekt(h):
    """The roofed bridge: walls with windows, lamps in the ceiling, a Schuilguh on the wall."""
    lang = 11
    b = Stuk(h, "brug_overdekt", (lang, 7, 5))
    for x in range(lang):
        for z in range(5):
            b.set(x, 0, z, PILAAR if z in (0, 4) else MSTEEN, {"axis": "x"} if z in (0, 4) else None)
            b.set(x, 4, z, LAMP if z == 2 and x % 4 == 1 else MSTEEN)
            if z in (0, 4):
                for y in (1, 2, 3):
                    b.set(x, y, z, "purple_stained_glass_pane" if y == 2 and x % 2 == 1 and 0 < x < lang - 1 else MSTEEN)
                b.set(x, 5, z, PLAAT, slab())
            else:
                for y in (1, 2, 3):
                    b.set(x, y, z, AIR)
                b.set(x, 5, z, (LAMP if x % 4 == 3 else MSTEEN) if z == 2 else PLAAT, None if z == 2 else slab())
        if x % 4 != 3:                                   # (the ridge, with a lamp in it now and then: the roof can be walked on)
            b.set(x, 6, 2, PLAAT, slab())
    b.sign(4, 2, 1, "south", ["TOCHTGANG", "Deur dicht!", "(er is geen", "deur) -Mika"], wood="crimson")
    b.schuilguh(6, 2, 3, "south")
    b.jig(0, 0, 2, "west", naam="brug_in")
    b.jig(lang - 1, 0, 2, "east", doel="toren_deur", pool="toren")
    b.begin = (0, 1, 2)
    return b


def balkon(h):
    """A door that leads nowhere gets a balcony (also what hangs on the kitchen, above the gate)."""
    b = Stuk(h, "balkon", (3, 4, 5))
    for x in range(3):
        for z in range(5):
            b.set(x, 0, z, PILAAR if x == 2 or z in (0, 4) else MSTEEN, {"axis": "z" if x == 2 else "x"} if x == 2 or z in (0, 4) else None)
            if x == 2 or z in (0, 4):
                hoek = x == 2 and z in (0, 4)
                b.set(x, 1, z, PILAAR if hoek else "crimson_fence", AS_Y if hoek else None)
                if hoek:
                    b.set(x, 2, z, "guhs:lampion_roze", lamp(False))
            else:
                for y in (1, 2, 3):
                    b.set(x, y, z, AIR)
    b.jig(0, 0, 2, "west", naam="brug_in")
    b.begin = (0, 1, 2)
    return b


def brug_einde(h):
    """A bridge whose tower never came: a lookout with a promise on a sign."""
    b = Stuk(h, "brug_einde", (5, 4, 5))
    for x in range(5):
        for z in range(5):
            rand = x == 4 or z in (0, 4)
            b.set(x, 0, z, PILAAR if rand else LAMP if (x, z) == (2, 2) else MSTEEN, {"axis": "z" if x == 4 else "x"} if rand else None)
            if rand:
                hoek = x == 4 and z in (0, 4)
                b.set(x, 1, z, PILAAR if hoek else "crimson_fence", AS_Y if hoek else None)
                if hoek:
                    b.set(x, 2, z, "guhs:lampion_roze", lamp(False))
            else:
                for y in (1, 2, 3):
                    b.set(x, y, z, AIR)
    b.sign(3, 1, 3, "west", ["Hier komt nog", "een toren.", "Ooit. Echt.", "-Mika"], wall=False, wood="crimson")
    b.schuilguh(3, 1, 1)
    b.jig(0, 0, 2, "west", naam="toren_deur")
    b.begin = (0, 1, 2)
    return b


def steiger(h):
    """The pier from the keep to the vetschip, with the havenbord: the marker vetbootje_opdracht is the free spot on it where
    the notice for the vetbootje will hang (every vesting has one steiger and one of the three ships on it)."""
    b = Stuk(h, "steiger", (7, 5, 5))
    dek(b, 7, palen=False)
    for x in (1, 5):
        b.set(x, 1, 4, PILAAR, AS_Y)
        b.set(x, 2, 4, "guhs:lampion_geel", lamp(False))
    b.set(3, 1, 0, PILAAR, AS_Y)
    for y in (2, 3):
        b.set(3, y, 0, "dark_oak_planks")
    b.set(3, 4, 0, "guhs:lampion_geel", lamp(False))
    b.sign(3, 3, 1, "south", ["HAVENBORD", "Schip klaar?", "Bijna. Morgen.", "-de kapitein"], wood="dark_oak")
    b.prop("vetbootje_opdracht", 3, 2, 1, wie="hier hangt de opdracht voor het vetbootje (op het havenbord)")
    b.jig(0, 0, 2, "west", naam="steiger_in")
    b.jig(6, 0, 2, "east", doel="schip_in", pool="schip", sel=9, plc=10)
    b.begin = (0, 1, 2)
    return b


# =====================================================================================================================
# the side towers: an entry storey on a pointed foot, a room, a roof
# =====================================================================================================================
def toren(h, naam, rond_=False):
    """The storey a bridge arrives at (11 x 16 x 11): the door west, a door east that may lead on, the foot under it."""
    b = Stuk(h, naam, (TN, TR + P, TN))
    voet(b, TN, TR, rond_=rond_, seed=kit.seed(naam))
    t = Op(b, 0, TR, 0)
    if rond_:
        rond(t, 0)
        spiraal(t, 5, 0, 5)
        t.jig(5, 7, 0, "up", doel="rond_op", pool="kamer_rond", sel=10)
    else:
        vierkant(t, TN, 0)
        spiraal(t, 2, 0, 2)
        t.jig(0, 7, 0, "up", doel="vierkant_op", pool="kamer_vierkant", sel=10)
    t.deur(0, 1, 5, "east")
    t.deur(10, 1, 5, "west")
    t.jig(0, 0, 5, "west", naam="toren_deur")
    t.jig(10, 0, 5, "east", doel="brug_in", pool="verder")
    b.begin = (1, TR + 1, 5)
    b.vloer = TR
    return b, t


def toren_wacht(h):
    b, t = toren(h, "toren_wacht")
    t.set(8, 1, 1, "guhs:guh_tafel", {"facing": "south"})
    t.set(8, 2, 1, "guhs:lampion_roze", lamp(False))
    t.set(7, 1, 1, TRAP, stair("west"))
    t.ton(9, 1, 1, 2)
    t.ton(9, 1, 2)
    t.chest(9, 1, 9, "west", KIST_GEWOON)
    t.ton(8, 1, 9)
    t.set(1, 1, 9, "guhs:lampion_roze", lamp(False))
    t.sign(5, 3, 1, "south", ["WACHTPOST", "Niemand gezien.", "(ik sliep)", "-Mika"], wood="crimson")
    t.schuilguh(9, 3, 7, "east")
    ramen(t, 0, [(3, 10), (7, 10), (6, 0), (10, 2), (10, 8), (0, 8)])
    t.mika(6, 1, 7, 0)
    return b


def toren_kerker(h):
    """Two more cells (cel_7, cel_8): "the guest room"."""
    b, t = toren(h, "toren_kerker")
    for x in list(range(1, 4)) + list(range(7, 10)):
        for y in range(1, P):
            t.set(x, y, 7, MSTEEN)
    for x in (4, 6):
        for z in (7, 8, 9):
            for y in range(1, P):
                t.set(x, y, z, MSTEEN)
    for n, (xc, kijkt) in enumerate(((2, -90), (8, 90))):
        t.deur(xc, 1, 7, "south", wood="oak", hinge="left" if n else "right")
        for x in (xc - 1, xc + 1):
            for y in (2, 3):
                t.set(x, y, 7, "iron_bars")
        ver = 1 if xc == 2 else 9
        t.set(ver, 1, 9, "guhs:lampion_geel", lamp(False))
        t.set(xc, 1, 9, "yellow_carpet")
        t.mager(xc, 1, 8, yaw=180)
        t.prop(f"cel_{7 + n}", xc, 1, 8, wie="hier staat de magere guh van deze cel")
    for y in range(3, P):
        t.set(5, y, 7, MSTEEN)
    t.ton(5, 1, 9, 2)
    t.set(5, 1, 8, "guhs:lampion_roze", lamp(False))
    t.sign(5, 3, 6, "north", ["LOGEERKAMER", "Gasten blijven", "heel lang.", "-Mika"], wood="crimson")
    t.ton(9, 1, 1)
    t.ton(8, 1, 1)
    ramen(t, 0, [(6, 0), (10, 2), (0, 8)])
    t.mika(7, 1, 3, 180)
    return b


def toren_rond(h):
    """The round uitkijktoren: the stairs go round the middle."""
    b, t = toren(h, "toren_rond", rond_=True)
    t.ton(2, 1, 2)
    t.ton(8, 1, 2, 2)
    t.set(2, 1, 8, KNABBELS)
    t.set(2, 2, 8, "guhs:lampion_roze", lamp(False))
    t.chest(8, 1, 8, "north", KIST_GEWOON)
    t.set(5, 1, 9, "guhs:lampion_roze", lamp(False))
    t.sign(5, 3, 1, "south", ["UITKIJKTOREN", "Kijk uit:", "guhs zijn", "overal. NJEG"], wood="crimson")
    t.schuilguh(5, 3, 9, "south")
    ramen(t, 0, [(1, 2), (9, 2), (1, 8), (9, 8), (3, 0), (7, 0)])
    t.mika(7, 1, 3, 180)
    return b


def kamer(h, naam, rond_=False):
    """A storey on top of an entry storey."""
    b = Stuk(h, naam, (TN, P, TN))
    t = Op(b)
    if rond_:
        rond(t, 0)
        trapgat(t, 5, 0, 5)
        spiraal(t, 5, 0, 5)
        b.jig(5, 0, 0, "down", naam="rond_op")
        b.jig(5, 7, 0, "up", doel="rond_op", pool="dak_rond", sel=10)
        b.begin = (7, 1, 7)
    else:
        vierkant(t, TN, 0)
        trapgat(t, 2, 0, 2)
        spiraal(t, 2, 0, 2)
        b.jig(0, 0, 0, "down", naam="vierkant_op")
        b.jig(0, 7, 0, "up", doel="vierkant_op", pool="dak_vierkant", sel=10)
        b.begin = (4, 1, 4)
    return b, t


def kamer_slaap(h):
    b, t = kamer(h, "kamer_slaap")
    for x in (3, 6):
        t.set(x, 1, 9, "purple_bed", {"facing": "south", "part": "head", "occupied": "false"})
        t.set(x, 1, 8, "purple_bed", {"facing": "south", "part": "foot", "occupied": "false"})
    t.ton(4, 1, 9)
    t.set(4, 2, 9, "guhs:lampion_roze", lamp(False))
    for x in range(3, 8):
        t.set(x, 1, 6, "purple_carpet")
    t.chest(9, 1, 9, "west", KIST_GEWOON)
    t.ton(9, 1, 1, 2)
    t.set(9, 1, 5, "guhs:lampion_roze", lamp(False))
    t.sign(9, 2, 7, "west", ["Dromen over", "knabbels is", "verplicht.", "-de kapitein"], wood="crimson")
    t.schuilguh(1, 3, 7, "west")
    ramen(t, 0, [(5, 10), (8, 10), (10, 3), (10, 7), (6, 0), (0, 6)])
    t.mika(6, 1, 4, 0)
    return b


def kamer_buit(h):
    """The loot room of a side tower (the middle tier): what a Mika takes home from a raid."""
    b, t = kamer(h, "kamer_buit")
    for (x, z, n) in ((9, 9, 3), (8, 9, 2), (9, 8, 2), (7, 9, 1), (9, 7, 1), (1, 9, 2), (2, 9, 1), (9, 1, 2), (8, 1, 1)):
        for y in range(1, 1 + n):
            t.set(x, y, z, KNABBELS)
    t.chest(5, 1, 9, "north", KIST_GOED)
    t.ton(4, 1, 9)
    t.ton(6, 1, 9, 2)
    t.set(4, 2, 9, "guhs:lampion_geel", lamp(False))
    t.set(9, 1, 4, "honeycomb_block")
    t.set(9, 2, 4, "guhs:lampion_geel", lamp(False))
    t.set(1, 1, 6, "guhs:lampion_geel", lamp(False))
    t.sign(5, 3, 9, "north", ["BUITKAMER", "Buit van Mika.", "Eerlijk gejat.", "NJEG!"], wood="crimson")
    ramen(t, 0, [(10, 6), (6, 0), (0, 7), (2, 10)])
    t.mika(6, 1, 6, 0)
    return b


def kamer_rond_stook(h):
    """The room of a round tower: the vetstokerij (the middle loot tier as well)."""
    b, t = kamer(h, "kamer_rond_stook", rond_=True)
    t.set(2, 1, 3, "water_cauldron", {"level": "2"})
    t.set(2, 1, 7, "cauldron")
    t.set(3, 1, 8, "honey_block")
    t.set(3, 2, 8, "guhs:lampion_geel", lamp(False))
    t.set(8, 1, 7, "honey_block")
    t.set(8, 2, 7, "honey_block")
    t.set(7, 1, 8, "honeycomb_block")
    t.ton(8, 1, 3, 2)
    t.ton(7, 1, 2)
    t.set(3, 1, 2, "guhs:lampion_geel", lamp(False))
    t.chest(5, 1, 9, "north", KIST_GOED)
    t.sign(5, 3, 1, "south", ["VETSTOKERIJ", "Roeren, niet", "proeven.", "(toch geproefd)"], wood="crimson")
    ramen(t, 0, [(1, 2), (9, 2), (1, 8), (9, 8), (10, 5), (0, 5)])
    t.mika(7, 1, 5, 90)
    return b


def dak_kantelen(h):
    """The roof of a square side tower: battlements and a pennant."""
    b = Stuk(h, "dak_kantelen", (TN, 9, TN))
    vierkant(b, TN, 0, hoog=1, lampen=False)
    trapgat(b, 2, 0, 2)
    kantelen(b, TN, 1)
    for (x, z) in ((6, 3), (3, 7), (8, 7)):
        b.set(x, 0, z, LAMP)
    for y in range(1, 8):
        b.set(7, y, 7, "crimson_fence")
    for (dx, y) in ((1, 7), (2, 7), (3, 7), (1, 6), (2, 6), (1, 5)):
        b.set(7 + dx, y, 7, "purple_wool" if (dx + y) % 2 else "magenta_wool")
    b.schuilguh(8, 1, 2)
    b.set(2, 1, 8, "barrel", {"facing": "up", "open": "false"})
    b.jig(0, 0, 0, "down", naam="vierkant_op")
    b.begin = (4, 1, 4)
    return b


def dak_plat(h):
    """The plain roof (what a square tower gets when nothing else fits): a rim and a lamp."""
    b = Stuk(h, "dak_plat", (TN, 3, TN))
    vierkant(b, TN, 0, hoog=1, lampen=False)
    trapgat(b, 2, 0, 2)
    for x in range(TN):
        for z in range(TN):
            if x in (0, 10) or z in (0, 10):
                b.set(x, 1, z, PLAAT, slab())
    for (x, z) in ((6, 3), (3, 7), (8, 7)):
        b.set(x, 0, z, LAMP)
    b.set(8, 1, 8, PILAAR, AS_Y)
    b.set(8, 2, 8, "guhs:lampion_roze", lamp(False))
    b.jig(0, 0, 0, "down", naam="vierkant_op")
    b.begin = (4, 1, 4)
    return b


def dak_spits(h):
    """The spire of the round tower: a lookout under a pointed cap."""
    b = Stuk(h, "dak_spits", (TN, 11, TN))
    rond(b, 0, hoog=1, lampen=True)
    trapgat(b, 5, 0, 5)
    for x in range(TN):
        for z in range(TN):
            if rond_soort(x, z) == "muur":
                open_ = (x + z) % 3 == 0
                b.set(x, 1, z, MSTEEN)
                b.set(x, 2, z, "crimson_fence" if open_ else MSTEEN)
                b.set(x, 3, z, MSTEEN)
    for k in range(7):                                   # the cap: rings that close in
        y = 4 + k
        r_uit, r_in = 5.6 - 0.8 * k, 4.4 - 0.8 * k
        for x in range(TN):
            for z in range(TN):
                d = math.hypot(x - 5, z - 5)
                if d <= r_uit and (d > r_in or k == 6):
                    b.set(x, y, z, "purple_terracotta" if (k % 2 == 0) else "magenta_terracotta")
    b.set(5, 9, 5, LAMP)
    b.set(5, 8, 5, "guhs:lampion_roze", lamp(True))
    b.set(8, 1, 3, "barrel", {"facing": "up", "open": "false"})
    b.set(8, 2, 3, "lightning_rod", {"facing": "east", "powered": "false", "waterlogged": "false"})
    b.schuilguh(3, 1, 7)
    b.jig(5, 0, 0, "down", naam="rond_op")
    b.begin = (7, 1, 7)
    return b


def dak_rond_plat(h):
    b = Stuk(h, "dak_rond_plat", (TN, 3, TN))
    rond(b, 0, hoog=1, lampen=True)
    trapgat(b, 5, 0, 5)
    for x in range(TN):
        for z in range(TN):
            if rond_soort(x, z) == "muur":
                b.set(x, 1, z, PLAAT, slab())
    b.set(8, 1, 5, PILAAR, AS_Y)
    b.set(8, 2, 5, "guhs:lampion_roze", lamp(False))
    b.jig(5, 0, 0, "down", naam="rond_op")
    b.begin = (7, 1, 7)
    return b


# =====================================================================================================================
# the vetschepen
# =====================================================================================================================
class OudSchip:
    """guheinde_bouw.ship() draws the classic vetschip in the coordinates of the old template (mika_vesting_schip): this is
    that template seen from the new piece (and its signs go through sign_text, its wings get their tag)."""

    DX, DY, DZ = 6 - gb.SX, 6 - gb.DK, -11

    def __init__(self, b):
        self.b, self.h = b, b.h
        self.frame_cell = None

    def _p(self, x, y, z):
        return x + self.DX, y + self.DY, z + self.DZ

    def set(self, x, y, z, *a, **k):
        if x + self.DX >= 0:                           # (the old bridge to the tower: only its last block is the gangway)
            self.b.set(*self._p(x, y, z), *a, **k)

    def chest(self, x, y, z, facing, loot):
        self.b.chest(*self._p(x, y, z), facing, loot)

    def sign(self, x, y, z, facing, key, nl, en, wall=True, wood="birch"):
        self.b.sign(*self._p(x, y, z), facing, nl, wall=wall, wood=wood)

    def ent(self, x, y, z, nbt, kind):
        if kind == "frame":                            # (the wings: a frame of our own kind, in the same cell)
            fx, fy, fz = self.frame_cell = self._p(int(math.floor(x)), int(math.floor(y)), int(math.floor(z)))
            self.b.vleugels(fx, fy, fz, "north")
        else:
            px, py, pz = self._p(x, y, z)
            self.b.ent(px, py, pz, nbt, kind)


def schip_groot(h):
    """The classic vetschip (the one of the old vestingen): two masts, Mika's face on the sail, the hold with the vet, the
    Guhvleugels in the bow cabin."""
    b = Stuk(h, "schip_groot", (13, 22, 40))
    oud = OudSchip(b)
    gb.ship(oud)
    dk = gb.DK + OudSchip.DY
    for z in (5, 12, 17, 27, 33):                          # (more light on deck than the old ship had)
        hw = gb.ship_half(z - OudSchip.DZ)
        for x in (6 - int(hw + 0.3), 6 + int(hw + 0.3)):
            if b.get(x, dk + 1, z) and b.get(x, dk + 2, z) is None:
                b.set(x, dk + 2, z, "guhs:lampion_geel", lamp(False))
    for z in (19, 23):
        b.set(0, dk + 2, z, "guhs:lampion_roze", lamp(False))
    b.schuilguh(6, dk + 1, 37)
    b.jig(0, dk, 21, "west", naam="schip_in")
    b.begin = (0, dk + 1, 21)
    b.vloer = dk
    return b


def romp(b, sx, dk, z0, z1, hw, boeg, hek, diep, rng):
    """A hull along z (the bow north): returns half(z), the half width of the deck at z (None: no ship there)."""
    def half(z):
        if z < z0 or z > z1:
            return None
        if z < z0 + boeg:
            return hw * (z - z0 + 1) / (boeg + 1)
        if z <= z1 - hek:
            return hw
        return hw - (z - (z1 - hek)) * (hw * 0.45 / hek)

    cellen = set()
    for z in range(z0, z1 + 1):
        for x in range(sx - 7, sx + 8):
            u = x - sx
            if abs(u) > half(z) + 0.3:
                continue
            onder = dk - diep + max(0, abs(u) - 1) + max(0, (z0 + boeg) - z) // 2
            for y in range(onder, dk):
                cellen.add((x, y, z))
    for c in sorted(cellen):
        x, y, z = c
        buiten = any((x + dx, y + dy, z + dz) not in cellen for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)))
        if not buiten:
            b.set(*c, "brown_terracotta")
        elif y == dk - 1:
            b.set(*c, "honey_block")
        elif abs(x - sx) <= 1 and (x, y - 1, z) not in cellen:
            b.set(*c, "honeycomb_block")
        else:
            b.set(*c, gb.pick(rng, [("orange_terracotta", 40), ("yellow_terracotta", 30), ("brown_terracotta", 30)]))
    return half


def dek_schip(b, sx, dk, z0, z1, half, gat):
    """The deck and the bulwark (gat: the z of the gangway, on the west side)."""
    for z in range(z0, z1 + 1):
        for x in range(sx - 7, sx + 8):
            u = x - sx
            if abs(u) > half(z) + 0.3:
                continue
            rand = abs(u) > half(z) - 0.7 or z in (z0, z1)
            b.set(x, dk, z, "brown_terracotta" if rand else ("yellow_terracotta" if u % 2 else "orange_terracotta"))
            if rand and not (u < 0 and abs(z - gat) <= 1):
                b.set(x, dk + 1, z, "honeycomb_block" if z % 5 else "brown_terracotta")
            else:
                for y in (dk + 1, dk + 2):
                    b.set(x, y, z, AIR)


def kajuit(b, sx, dk, za, zb, deur_z, vleugel_kant):
    """A small cabin from za to zb (three wide inside): the Guhvleugels on its far wall, a chest of vet, a lampion."""
    for x in range(sx - 2, sx + 3):
        for z in range(za, zb + 1):
            muur = x in (sx - 2, sx + 2) or z in (za, zb)
            for y in range(dk + 1, dk + 5):
                if y == dk + 4:
                    b.set(x, y, z, "orange_terracotta" if muur else "brown_terracotta")
                elif muur:
                    b.set(x, y, z, "yellow_terracotta" if y != dk + 1 else "brown_terracotta")
                else:
                    b.set(x, y, z, AIR)
            b.set(x, dk, z, "brown_terracotta" if muur else "orange_terracotta")
    for y in (dk + 1, dk + 2):
        b.set(sx, y, deur_z, AIR)
    ver = zb if deur_z == za else za
    binnen = ver - 1 if ver == zb else ver + 1
    zm = (za + zb) // 2
    for x in (sx - 2, sx + 2):
        b.set(x, dk + 2, zm, "yellow_stained_glass_pane")
    b.vleugels(sx, dk + 2, binnen, vleugel_kant)
    b.set(sx, dk + 3, zm, "guhs:lampion_roze", lamp(True))
    b.chest(sx - 1, dk + 1, binnen, gb.OPP[vleugel_kant], KIST_SCHIP)
    b.sign(sx + 1, dk + 2, binnen, gb.OPP[vleugel_kant], ["Guhvleugels!", "Van Mika.", "AFBLIJVEN.", "(njeg!)"], wood="spruce")


def loopplank(b, dk, gat, tot):
    """The gangway: planks from x 0 to the hull, the jigsaw in the first one."""
    for x in range(0, tot):
        for z in (gat - 1, gat, gat + 1):
            b.set(x, dk, z, "spruce_planks")
            for y in (dk + 1, dk + 2):
                b.set(x, y, z, AIR)
    b.set(tot - 1, dk, gat, LAMP)
    b.jig(0, dk, gat, "west", naam="schip_in")
    b.begin = (0, dk + 1, gat)
    b.vloer = dk


def schip_sloep(h):
    """The sloep: one mast with Mika's face on the sail, a cabin aft."""
    sx, dk, z0, z1 = 5, 4, 2, 22
    b = Stuk(h, "schip_sloep", (11, 17, 24))
    rng = random.Random(kit.seed("schip_sloep"))
    half = romp(b, sx, dk, z0, z1, 3.4, 6, 4, 4, rng)
    dek_schip(b, sx, dk, z0, z1, half, 12)
    loopplank(b, dk, 12, 2)
    for y in range(dk + 1, dk + 12):
        b.set(sx, y, 9, "stripped_dark_oak_log", AS_Y)
    for x in range(sx - 4, sx + 5):
        if x != sx:
            b.set(x, dk + 10, 9, "stripped_dark_oak_log", {"axis": "x"})
    pal = {"b": "brown_wool", "p": "pink_wool", "K": "black_wool", "R": "red_wool", "W": "white_wool"}
    for j, row in enumerate(gb.mika_face_rows(7, 6, "b")):
        for i, ch in enumerate(row):
            x = sx - 3 + i
            if x != sx:
                b.set(x, dk + 9 - j, 9, pal[ch])
    b.set(sx, dk + 12, 9, "purple_wool")
    kajuit(b, sx, dk, 15, 20, 15, "south")
    b.set(sx, dk + 2, z1, "guhs:lampion_geel", lamp(False))
    for (x, z) in ((sx - 2, 5), (sx + 2, 5)):
        b.set(x, dk + 2, z, "guhs:lampion_geel", lamp(False))
    for x in (sx - 1, sx + 1):                           # the little figurehead
        b.set(x, dk, z0 - 1, "pink_wool")
    for y in (dk, dk + 1):
        b.set(sx, y, z0 - 1, "pink_wool")
    b.set(sx, dk + 1, z0 - 2, "black_wool")
    b.ton(sx + 2, dk + 1, 13)
    b.sign(sx + 2, dk + 1, 11, "west", ["DE SLOEP", "Klein maar", "vettig.", "-Mika"], wall=False, wood="spruce")
    b.mika(sx, dk + 1, 6, 0)
    return b


def schip_ballon(h):
    """The vetblaas: a hull that hangs under a big bubble of vet with Mika's face on both sides."""
    sx, dk, z0, z1 = 5, 4, 2, 18
    b = Stuk(h, "schip_ballon", (11, 22, 21))
    rng = random.Random(kit.seed("schip_ballon"))
    half = romp(b, sx, dk, z0, z1, 3.4, 5, 3, 4, rng)
    dek_schip(b, sx, dk, z0, z1, half, 9)
    loopplank(b, dk, 9, 2)
    # the bubble
    yc, zc, rx, ry, rz = dk + 12, 10, 4.7, 4.7, 8.2
    bel = set()
    for x in range(11):
        for y in range(dk + 6, dk + 18):
            for z in range(21):
                d = ((x - sx) / rx) ** 2 + ((y - yc) / ry) ** 2 + ((z - zc) / rz) ** 2
                if d <= 1.0:
                    bel.add((x, y, z))
    schil = {c for c in bel if any((c[0] + dx, c[1] + dy, c[2] + dz) not in bel          # (a skin without gaps: all 26 neighbours)
                                   for dx in (-1, 0, 1) for dy in (-1, 0, 1) for dz in (-1, 0, 1))}
    for (x, y, z) in sorted(schil):
        b.set(x, y, z, "orange_wool" if (z // 2) % 3 == 0 else "yellow_wool" if (z // 2) % 3 == 1 else "honeycomb_block")
    pal = {"p": "pink_wool", "K": "black_wool", "R": "red_wool", "W": "white_wool"}
    for j, row in enumerate(MIKA_FACE):                   # the face, on the west and the east side
        for i, ch in enumerate(row):
            y, z = yc + 3 - j, zc - 3 + i
            xs = sorted(x for (x, yy, zz) in schil if yy == y and zz == z)
            if xs:
                b.set(xs[0], y, z, pal[ch])
                b.set(xs[-1], y, z, pal[ch])
    onder = min(y for (x, y, z) in schil if (x, z) == (sx, zc))
    b.set(sx, onder, zc, "shroomlight")
    # the ropes: chains from four posts on the bulwark up to the bubble
    for (x, z) in ((sx - 3, 6), (sx + 3, 6), (sx - 3, 13), (sx + 3, 13)):
        b.set(x, dk + 2, z, "crimson_fence")
        top = min(y for (xx, y, zz) in schil if (xx, zz) == (x, z))
        for y in range(dk + 3, top):
            b.set(x, y, z, "chain", {"axis": "y", "waterlogged": "false"})
    kajuit(b, sx, dk, 12, 17, 12, "south")
    b.set(sx, dk + 2, z0, "guhs:lampion_geel", lamp(False))
    b.set(sx, dk + 2, z1, "guhs:lampion_geel", lamp(False))
    b.ton(sx + 2, dk + 1, 10)
    b.sign(sx + 2, dk + 1, 6, "west", ["DE VETBLAAS", "Niet prikken!", "(Mika prikte", "al een keer)"], wall=False, wood="spruce")
    b.mika(sx, dk + 1, 5, 0)
    return b


# =====================================================================================================================
# everything together
# =====================================================================================================================
MAKERS = [
    kern, fundering, keuken, slaapzaal, depot, troon, dak, cellenblok, lambda h: erf(h, "erf_duwpop"), lambda h: erf(h, "erf_knabbelkar"),
    lambda h: brug(h, "brug_kort", 9), lambda h: brug(h, "brug_lang", 15), brug_overdekt, balkon, brug_einde, steiger,
    toren_wacht, toren_kerker, toren_rond, kamer_slaap, kamer_buit, kamer_rond_stook, dak_kantelen, dak_plat, dak_spits, dak_rond_plat,
    schip_groot, schip_sloep, schip_ballon,
]

# pool: ([(piece, weight)], fallback pool or None). A fallback is what the game takes when nothing of the pool fits, and at
# the last depth (so a tower always gets a roof, a bridge an end, a door a balcony).
POOLS = {
    "start": ([("kern", 1)], None),
    "fundering": ([("fundering", 1)], None),
    "hoofd_1": ([("keuken", 1)], None),
    "hoofd_2": ([("slaapzaal", 1), ("depot", 1)], None),
    "hoofd_3": ([("troon", 1)], None),
    "hoofd_dak": ([("dak", 1)], None),
    "cellen": ([("cellenblok", 1)], None),
    "erf": ([("erf_duwpop", 1), ("erf_knabbelkar", 1)], None),
    "brug": ([("brug_kort", 3), ("brug_lang", 2), ("brug_overdekt", 2)], "balkon"),
    "verder": ([("balkon", 4), ("brug_kort", 2), ("brug_lang", 1), ("brug_overdekt", 1)], "balkon"),
    "balkon": ([("balkon", 1)], None),
    "toren": ([("toren_wacht", 3), ("toren_kerker", 2), ("toren_rond", 3)], "brug_einde"),
    "brug_einde": ([("brug_einde", 1)], None),
    "kamer_vierkant": ([("kamer_slaap", 2), ("kamer_buit", 2), ("dak_kantelen", 1)], "dak_vierkant"),
    "kamer_rond": ([("kamer_rond_stook", 3), ("dak_spits", 1)], "dak_rond"),
    "dak_vierkant": ([("dak_kantelen", 1)], "dak_vierkant_plat"),
    "dak_vierkant_plat": ([("dak_plat", 1)], None),
    "dak_rond": ([("dak_spits", 1)], "dak_rond_plat"),
    "dak_rond_plat": ([("dak_rond_plat", 1)], None),
    "steiger": ([("steiger", 1)], None),
    "schip": ([("schip_groot", 1), ("schip_sloep", 1), ("schip_ballon", 1)], None),
}
SCHEPEN = ["schip_groot", "schip_sloep", "schip_ballon"]
# what every vesting has, whatever the dice say
ALTIJD = ["kern", "fundering", "keuken", "troon", "dak", "cellenblok", "steiger"]
# the markers of the build contract (6.7) and the piece each lies in
PLEKKEN = {"kok": "keuken", "keuken": "keuken", "kapitein": "kern", "schatkamer": "troon", "vetbootje_opdracht": "steiger",
           **{f"cel_{i}": "cellenblok" for i in range(1, 7)}, "cel_7": "toren_kerker", "cel_8": "toren_kerker"}


def controleer(b):
    """The self-check of one piece -> its problems."""
    naam = b.naam
    p = []
    if b.begin is None:
        p += list(b.fouten)
        if b.oob:
            p.append(f"{len(b.oob)} blokken buiten het stuk, bv. {b.oob[:4]}")
        houvast = b.jigs[0]["pos"]
        los = gb.floating(b, lambda c: c == houvast)
        if los:
            p.append(f"{len(los)} zwevende blokken, bv. {sorted(los)[:5]}")
        p += gb.attach_problems(b)
        reach = set()
    else:
        p += b.check(b.begin, standaard=False, zwevend_ok=lambda c: any(abs(c[0] - x) <= 1 and abs(c[2] - z) <= 1 for x, z in b.wel))
        reach = kit.Loper(b).bfs(tuple(b.begin))
        blocks = b.s.blocks
        for c, bl in blocks.items():
            if bl[0] == "minecraft:chest" and not gb.near(reach, c, dy=(0, -1, 1)):
                p.append(f"kist op {c} is niet bereikbaar")
        for (x, y, z, kind, nbt) in b.ents:
            c = (int(math.floor(x)), int(math.floor(y)), int(math.floor(z)))
            if kind in ("mika", "mager") and c not in reach:
                p.append(f"{kind} op {c} staat niet op een bereikbare vloer")
            if kind == "schuilguh":
                vast = [k for k, (dx, dy, dz) in (("down", (0, -1, 0)), ("up", (0, 1, 0)), ("north", (0, 0, -1)), ("south", (0, 0, 1)),
                                                  ("west", (-1, 0, 0)), ("east", (1, 0, 0)))
                        if b.solid_at((c[0] + dx, c[1] + dy, c[2] + dz)) and not gb.is_fency(blocks[(c[0] + dx, c[1] + dy, c[2] + dz)][0])]
                wil = [k for k, v in gb.FACING_BYTE.items() if v == int(nbt["AttachFace"])][0]
                if vast != [wil]:
                    p.append(f"schuilguh op {c}: houdt {wil} vast, maar de vaste blokken om hem heen zijn {vast} (precies die ene)")
                if blocks.get(c, (AIR,))[0] != AIR and c in blocks:
                    p.append(f"schuilguh op {c} zit in een blok")
                if not (gb.near(reach, c, dy=(-2, -1, 0, 1)) or c in reach):
                    p.append(f"schuilguh op {c}: daar kun je niet bij")
        for (c, kant) in b.lijsten:
            dx, dz = gb.DIRS[kant]
            if blocks.get(c, (AIR,))[0] != AIR or not b.solid_at((c[0] + dx, c[1], c[2] + dz)):
                p.append(f"de lijst met de Guhvleugels op {c} hangt niet aan een muur")
            if not (gb.near(reach, (c[0], c[1] - 1, c[2])) or (c[0], c[1] - 1, c[2]) in reach):
                p.append(f"de lijst met de Guhvleugels op {c} is niet bereikbaar")
    namen = [j["naam"] for j in b.jigs if j["naam"]]
    if naam != "kern" and len(namen) != 1:
        p.append(f"een stuk hangt aan precies een jigsaw met een naam: {namen}")
    for j in b.jigs:
        x, y, z = j["pos"]
        dx, dy, dz = {"up": (0, 1, 0), "down": (0, -1, 0), **{k: (v[0], 0, v[1]) for k, v in gb.DIRS.items()}}[j["richting"]]
        if b.inside(x + dx, y + dy, z + dz):
            p.append(f"jigsaw {j['pos']} wijst het stuk in ({j['richting']}): een kind moet buiten de doos liggen")
        if j["pool"] and j["pool"] not in POOLS:
            p.append(f"jigsaw {j['pos']}: pool {j['pool']} bestaat niet")
    return [f"{naam}: {x}" for x in p]


def maak(h):
    """Builds every piece -> ({naam: Stuk}, problems). Nothing is saved."""
    stukken, problemen = {}, []
    for maker in MAKERS:
        b = maker(h)
        b.connect()
        stukken[b.naam] = b
    for b in stukken.values():
        problemen += controleer(b)
    # the pools: every piece is in one, every name a pool asks for is offered by all of its pieces
    in_pool = {s for stuks, _ in POOLS.values() for s, _ in stuks}
    for naam in stukken:
        if naam not in in_pool:
            problemen.append(f"{naam} zit in geen pool")
    for b in stukken.values():
        for j in b.jigs:
            if not j["pool"]:
                continue
            pool = j["pool"]
            while pool:
                stuks, terugval = POOLS[pool]
                for s, _ in stuks:
                    if j["doel"] not in [k["naam"] for k in stukken[s].jigs]:
                        problemen.append(f"{b.naam} vraagt {j['doel']} uit pool {pool}, maar {s} heeft die jigsaw niet")
                pool = terugval
    for id, stuk in PLEKKEN.items():
        if id not in stukken[stuk].plekken:
            problemen.append(f"plek {id} ontbreekt in {stuk}")
    for b in stukken.values():
        for id in b.plekken:
            if PLEKKEN.get(id) != b.naam:
                problemen.append(f"plek {id} in {b.naam} staat niet in PLEKKEN")
    return stukken, problemen


def schrijf(h, stukken):
    """Saves the templates and writes the pools."""
    for b in stukken.values():
        b.save()
    for pool, (stuks, terugval) in POOLS.items():
        h.w(f"{h.D}/worldgen/template_pool/{NS}/{pool}.json", {
            "fallback": nid(terugval) if terugval else LEEG,
            "elements": [{"weight": w, "element": {"element_type": "minecraft:single_pool_element", "location": nid(s), "projection": "rigid",
                                                   "processors": LEEG}} for s, w in stuks]})
