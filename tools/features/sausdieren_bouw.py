"""
bbq2 (sausdieren) - the Sausloper-stal (structure guhs:sausloper_stal, one template sausloper_stal on a cave floor of the
Guhbarbecuether, type guhs:barbecueput through wereld.bbq_structuur).

What stands there (template coordinates, x to the east, z to the south, ground layer G):
  - the STAL in the west: a timber-framed barn (a dark houtskoolsteen plinth, mustard infill between saté-stick posts, a
    copper gambrel roof with two pink guh ears on each gable) with three tall stalls that open to the east and a feed room.
    The stall floors are heated (gloeikool under a grate pattern): a Sausloper shivers on anything cold.
  - the ERF in the middle: a paved yard with the Verzorger-guh, a pindasaus-tuintje (the snack garden), a brushing spot
    and the pier you get on from.
  - the SAUSBAK in the east: an oval basin of kaasfrituursaus (one block deep, with a flush kerb you can always climb out
    over) around a snack island with a lamp mast, and the four gates of the test lap (N, E, S and the finish in the west).
The geometry the Java side needs (the NPC, the lap, the residents: feature/sausdieren/Stal.java) is in PLEKKEN; the module's
self-check compares it with the constants in that file.

  bouw(h)        -> (Structure, problems)   the template, its entities included (not saved)
  check(s)       geometry self-check: nothing floats, nothing that burns near the sauce, the sauce can't leak, the lap is free
  preview(out)   (python tools/features/sausdieren_bouw.py <out>) pictures of the template from four sides
"""
import math
import os
import random
import sys

MIDDEN = "guhs:sausloper_stal_midden"
G = 3                                   # the ground layer: its top block is the cave floor (see midden())
SIZE = (38, 17, 31)

HOUTSKOOL = "guhs:houtskoolsteen"
STENEN = "guhs:houtskoolsteen_stenen"
GEBARSTEN = "guhs:gebarsten_houtskoolsteen_stenen"
GEBEITELD = "guhs:gebeitelde_houtskoolsteen_stenen"
MUUR = "guhs:houtskoolsteen_stenen_muur"
HEK = "guhs:houtskoolsteen_stenen_hek"
PLAAT = "guhs:houtskoolsteen_stenen_plaat"
TRAP = "guhs:houtskoolsteen_stenen_trap"
TRALIES = "guhs:roosterijzer_tralies"
IJZER = "guhs:gepolijst_roosterijzer"
PILAAR = "guhs:roosterijzer_pilaar"
GLOEIKOOL = "guhs:gloeikool"
UIENLICHT = "guhs:uienlicht"
MOSTERD = "guhs:mosterd_blok"
SATE = "guhs:sate_stam"
WORST = "guhs:worst_stam"
VLEES = "guhs:sate_vlees"
NYLIUM = "guhs:pindasaus_nylium"
SCHEUTJES = "guhs:pindascheutjes"
PLASJE = "guhs:pindasausplasje"
AS = "guhs:as_blok"
AS_AARDE = "guhs:as_aarde"
SAUS = "guhs:kaasfrituursaus"
KOPER_TRAP = "minecraft:waxed_cut_copper_stairs"
KOPER_PLAAT = "minecraft:waxed_cut_copper_slab"
KOPER = "minecraft:waxed_cut_copper"
OOR = "minecraft:pink_terracotta"
OOR_BINNEN = "minecraft:magenta_terracotta"
AIR = "minecraft:air"

# the stable: x 2..12, z 5..25; the basin: an ellipse around (27, 15)
SX0, SX1, SZ0, SZ1 = 2, 12, 5, 25
BAK = (27.0, 15.0, 8.5, 10.5)
EILAND = 2.3
PALEN_Z = (5, 10, 15, 20, 25)           # the posts of the open front

# --- what the Java side needs (Stal.java; block coordinates, entities stand on y) -------------------------------------
PLEKKEN = {
    "NPC": (15, G + 1, 15),                                     # the Verzorger-guh, on the yard, looks at the basin
    "START": (22, G + 1, 13),                                   # where the test lap's Sausloper waits (next to the pier)
    "POORTEN": [(27, G + 1, 8), (33, G + 1, 15), (27, G + 1, 21), (22, G + 1, 15)],   # N, E, S, finish (W)
    "BEWONERS": [(24, G + 1, 8), (32, G + 1, 19), (7, G + 1, 12)],   # two in the basin, one in the middle stall
    "BAK": (27, G + 1, 15),                                     # the middle of the basin (the residents' home)
}
NPC_YAW = -90.0                                                 # east


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


def wall_props(**kw):
    p = {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"}
    p.update(kw)
    return p


def in_bak(x, z, extra=0.0):
    cx, cz, rx, rz = BAK
    return ((x + 0.5 - cx - 0.5) / (rx + extra)) ** 2 + ((z + 0.5 - cz - 0.5) / (rz + extra)) ** 2 <= 1.0


def roof_y(x):
    """The gambrel roof's profile over the stable (x 1..13): (y, kind) with kind 'trap_o' / 'trap_w' (steep lower part),
    'plaat_onder' / 'plaat_boven' (the shallow upper part) or 'nok' (the ridge)."""
    d = min(x - (SX0 - 1), (SX1 + 1) - x)          # 0 at the eaves .. 6 at the ridge
    oost = x < 7
    if d <= 2:
        return G + 5 + d, "trap_o" if oost else "trap_w"
    if d == 3:
        return G + 7, "plaat_boven"
    if d == 4:
        return G + 8, "plaat_onder"
    if d == 5:
        return G + 8, "plaat_boven"
    return G + 9, "nok"


class Bouw:
    def __init__(self, h):
        self.h = h
        self.s = h.Structure(SIZE)
        self.rng = random.Random(21301201)
        self.W, self.H, self.D = SIZE
        self.plaat = set()

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    # --- the ground ---------------------------------------------------------------------------------------------------
    def grond(self):
        """A frayed oval plate of charred ground with two layers of rock under it, and air above (so the cave is open)."""
        cx, cz = (self.W - 1) / 2, (self.D - 1) / 2
        for x in range(self.W):
            for z in range(self.D):
                d = math.hypot((x - cx) / (self.W / 2 - 0.3), (z - cz) / (self.D / 2 - 0.3))
                rond_stal = SX0 - 2 <= x <= SX1 + 2 and SZ0 - 2 <= z <= SZ1 + 2
                rand_stal = rond_stal and (x in (SX0 - 2, SX1 + 2) or z in (SZ0 - 2, SZ1 + 2))
                if d > 1.0 + self.rng.uniform(-0.06, 0.03) and not (rond_stal and (not rand_stal or self.rng.random() < 0.6)):
                    continue
                self.plaat.add((x, z))
                top = self.rng.choice([HOUTSKOOL] * 4 + [AS_AARDE] * 3 + [AS] + [GEBARSTEN])
                self.set(x, G, z, top)
                for y in range(0, G):
                    self.set(x, y, z, HOUTSKOOL)
        self.s.clear_above(self.plaat, G + 1)

    def pad(self, cells):
        for x, z in cells:
            if (x, z) in self.plaat:
                self.set(x, G, z, self.rng.choice([STENEN] * 5 + [GEBARSTEN] * 2 + [GEBEITELD]))

    # --- the basin ------------------------------------------------------------------------------------------------------
    def sausbak(self):
        cx, cz, rx, rz = BAK
        for x in range(self.W):
            for z in range(self.D):
                if in_bak(x, z):
                    self.set(x, G, z, SAUS, {"level": "0"})
                    self.set(x, G - 1, z, STENEN)
                    self.set(x, G - 2, z, HOUTSKOOL)
                    for y in range(G + 1, self.H):
                        self.set(x, y, z, AIR)
                elif in_bak(x, z, 1.25):
                    # the kerb, flush with the yard, and rock under it so the sauce is held on every side
                    self.set(x, G, z, IJZER)
                    self.set(x, G - 1, z, STENEN)
                    self.plaat.add((x, z))
                    for y in range(G + 1, self.H):
                        if self.get(x, y, z) is None:
                            self.set(x, y, z, AIR)
        # kerb posts with a lantern, all around except where the yard is
        for k in range(14):
            a = 2 * math.pi * (k + 0.5) / 14
            x, z = int(round(cx + math.cos(a) * (rx + 0.9))), int(round(cz + math.sin(a) * (rz + 0.9)))
            if x <= 19 or self.get(x, G, z) != IJZER:
                continue
            self.set(x, G + 1, z, MUUR, wall_props())
            if k % 2 == 0:
                self.set(x, G + 2, z, "lantern", {"hanging": "false", "waterlogged": "false"})
        # the snack island: two blocks high (a Sausloper can't climb it), nylium with pindascheutjes, a lamp mast
        ix, iz = int(cx), int(cz)
        for x in range(ix - 3, ix + 4):
            for z in range(iz - 3, iz + 4):
                d = math.hypot(x - cx, z - cz)
                if d <= EILAND:
                    self.set(x, G, z, STENEN)
                    self.set(x, G + 1, z, NYLIUM if d > 0.8 else STENEN)
                    if d > 0.8 and (x + z) % 2 == 0:
                        self.set(x, G + 2, z, SCHEUTJES)
        for y in range(G + 2, G + 7):
            self.set(ix, y, iz, PILAAR, {"axis": "y"})
        self.set(ix, G + 7, iz, GLOEIKOOL)
        self.set(ix, G + 8, iz, PLAAT, {"type": "bottom", "waterlogged": "false"})
        for dx, dz, kant in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
            self.set(ix + dx, G + 7, iz + dz, TRALIES, {kant: "true", "waterlogged": "false",
                                                        **{k: "false" for k in ("north", "south", "east", "west") if k != kant}})
            self.set(ix + dx, G + 6, iz + dz, "lantern", {"hanging": "true", "waterlogged": "false"})

    def poort(self, nr, a, b, top):
        """A gate of the test lap: two grill-iron posts in the sauce at a and b (x, z), a saté-stick beam over them, a
        lantern under the middle and its own colour on top (the finish: a checkered row)."""
        (ax, az), (bx, bz) = a, b
        for x, z in (a, b):
            self.set(x, G - 1, z, STENEN)
            self.set(x, G, z, GEBEITELD)
            for y in range(G + 1, G + 6):
                self.set(x, y, z, MUUR, wall_props())
        langs_x = az == bz
        n = (abs(bx - ax) if langs_x else abs(bz - az)) + 1
        for i in range(n):
            x, z = (min(ax, bx) + i, az) if langs_x else (ax, min(az, bz) + i)
            self.set(x, G + 6, z, SATE, {"axis": "x" if langs_x else "z"})
            if 0 < i < n - 1:
                if nr == 3:
                    self.set(x, G + 7, z, GEBEITELD if i % 2 else MOSTERD)
                elif i in (n // 2 - 1, n // 2):
                    self.set(x, G + 7, z, top)
        mx, mz = ((ax + bx) // 2, az) if langs_x else (ax, (az + bz) // 2)
        self.set(mx, G + 5, mz, "lantern", {"hanging": "true", "waterlogged": "false"})
        if nr == 3:
            for x, z in (a, b):
                self.set(x, G + 7, z, GLOEIKOOL)

    def poorten(self):
        self.poort(0, (27, 6), (27, 11), MOSTERD)
        self.poort(1, (30, 15), (35, 15), GLOEIKOOL)
        self.poort(2, (27, 19), (27, 24), UIENLICHT)
        self.poort(3, (19, 15), (24, 15), None)

    def steiger(self):
        """The pier you get on from: half a block above the yard, two wide, from the yard into the basin."""
        for x in range(17, 22):
            for z in (11, 12):
                if in_bak(x, z) or in_bak(x, z, 1.25):
                    self.set(x, G - 1, z, STENEN)
                    self.set(x, G, z, STENEN)
                self.set(x, G + 1, z, PLAAT, {"type": "bottom", "waterlogged": "false"})
        for z in (10, 13):
            self.set(21, G, z, STENEN)
            self.set(21, G - 1, z, STENEN)
            self.set(21, G + 1, z, MUUR, wall_props())
            self.set(21, G + 2, z, "lantern", {"hanging": "false", "waterlogged": "false"})

    # --- the stable ------------------------------------------------------------------------------------------------------
    def stal(self):
        # the floor: paved, with heated spots (a gloeikool checker) in the three stalls
        for x in range(SX0, SX1 + 1):
            for z in range(SZ0, SZ1 + 1):
                self.set(x, G, z, STENEN)
                for y in range(G + 1, self.H):
                    self.set(x, y, z, AIR)
        for z0 in (6, 11, 16):
            for x in range(4, 11):
                for z in range(z0, z0 + 4):
                    if (x + z) % 2 == 0:
                        self.set(x, G, z, GLOEIKOOL)
        # the back wall and the two side walls: a dark plinth, mustard infill, saté-stick posts
        def wand(x, z, venster=False):
            self.set(x, G + 1, z, STENEN)
            self.set(x, G + 2, z, STENEN)
            self.set(x, G + 3, z, TRALIES if venster else MOSTERD,
                     {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"} if venster else None)
            self.set(x, G + 4, z, MOSTERD)
        for z in range(SZ0, SZ1 + 1):
            wand(SX0, z, venster=z in (7, 8, 12, 13, 17, 18, 22, 23))
        for x in range(SX0, SX1 + 1):
            for z in (SZ0, SZ1):
                wand(x, z)
        for z in PALEN_Z:
            for x in (SX0, SX1):
                for y in range(G + 1, G + 5):
                    self.set(x, y, z, SATE, {"axis": "y"})
        for x in (7,):
            for z in (SZ0, SZ1):
                for y in range(G + 1, G + 5):
                    self.set(x, y, z, SATE, {"axis": "y"})
        # the beams on top of the walls and across every post line
        for z in range(SZ0, SZ1 + 1):
            for x in (SX0, SX1):
                self.set(x, G + 5, z, SATE, {"axis": "z"})
        for z in PALEN_Z:
            for x in range(SX0 + 1, SX1):
                self.set(x, G + 5, z, SATE, {"axis": "x"})
        # the stall partitions: a low wall with a grate on top, from the back wall to two blocks from the front
        for z in (10, 15, 20):
            for x in range(SX0 + 1, 10):
                self.set(x, G + 1, z, STENEN)
                self.set(x, G + 2, z, TRALIES, {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"})
            self.set(9, G + 2, z, MUUR, wall_props())
            self.set(9, G + 1, z, STENEN)
        # a manger at the back of every stall (nylium with pindascheutjes behind a kerb) and a hanging lantern at the front
        for z0 in (6, 11, 16):
            for z in (z0 + 1, z0 + 2):
                self.set(SX0 + 1, G + 1, z, NYLIUM)
                self.set(SX0 + 1, G + 2, z, SCHEUTJES)
                self.set(SX0 + 2, G + 1, z, TRAP, {"facing": "west", "half": "bottom", "shape": "straight", "waterlogged": "false"})
            self.set(SX1, G + 4, z0 + 1, "lantern", {"hanging": "true", "waterlogged": "false"})
        # the feed room (z 21..24): hay, barrels, a chest, a lantern
        for x, y, z in ((3, 1, 24), (4, 1, 24), (3, 2, 24), (3, 1, 23), (5, 1, 24)):
            self.set(x, G + y, z, "hay_block", {"axis": "y" if (x + y) % 2 else "x"})
        for x, y, z in ((3, 1, 21), (4, 1, 21), (3, 2, 21)):
            self.set(x, G + y, z, "barrel", {"facing": "up", "open": "false"})
        self.h.ms.chest(self.s, 6, G + 1, 24, "north", "guhs:chests/sausdieren_stal")
        self.set(SX1, G + 4, 22, "lantern", {"hanging": "true", "waterlogged": "false"})
        self.set(8, G + 1, 24, "smithing_table")
        self.set(9, G + 1, 24, "cauldron")
        # the roof and the gables
        for z in range(SZ0 - 1, SZ1 + 2):
            for x in range(SX0 - 1, SX1 + 2):
                y, soort = roof_y(x)
                if soort.startswith("trap"):
                    self.set(x, y, z, KOPER_TRAP, {"facing": "east" if soort == "trap_o" else "west", "half": "bottom",
                                                   "shape": "straight", "waterlogged": "false"})
                elif soort == "plaat_boven":
                    self.set(x, y, z, KOPER_PLAAT, {"type": "top", "waterlogged": "false"})
                elif soort == "plaat_onder":
                    self.set(x, y, z, KOPER_PLAAT, {"type": "bottom", "waterlogged": "false"})
                else:
                    self.set(x, y, z, SATE, {"axis": "z"})
        for z in (SZ0, SZ1):
            for x in range(SX0 + 1, SX1):
                top, soort = roof_y(x)
                for y in range(G + 6, top + (1 if soort == "nok" else 0)):
                    if self.get(x, y, z) in (None, AIR):
                        self.set(x, y, z, MOSTERD)
            # the loft hatch: a round grate window with a warm light behind it, under a saté-stick lintel
            self.set(7, G + 7, z, TRALIES, {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"})
            for x in (6, 8):
                self.set(x, G + 6, z, SATE, {"axis": "y"})
                self.set(x, G + 7, z, SATE, {"axis": "y"})
            self.set(7, G + 6, z, SATE, {"axis": "x"})
        self.set(7, G + 7, SZ0 + 1, UIENLICHT)
        self.set(7, G + 7, SZ1 - 1, UIENLICHT)
        # two pink guh ears on each gable end of the ridge
        for z, dz in ((SZ0 - 1, 1), (SZ1 + 1, -1)):
            for x in (5, 9):
                self.set(x, G + 9, z, OOR)
                self.set(x, G + 10, z, OOR)
                self.set(x, G + 9, z + dz, OOR)
                self.set(x, G + 10, z + dz, OOR)
                self.set(x + (1 if x == 5 else -1), G + 9, z, OOR_BINNEN)
        # the stall fronts: a stone fence with a double gate in the middle of each bay (the feed room stays open)
        for z0 in (6, 11, 16):
            self.set(SX1, G + 1, z0, HEK, {"north": "true", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
            self.set(SX1, G + 1, z0 + 3, HEK, {"north": "false", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
            for z in (z0 + 1, z0 + 2):
                self.set(SX1, G + 1, z, "crimson_fence_gate", {"facing": "east", "open": "false", "in_wall": "false", "powered": "false"})
        # a cupola on the middle of the ridge: a little vent tower with a copper cap and a spire
        for x in (6, 7, 8):
            for z in (14, 15, 16):
                hoek = x != 7 and z != 15
                self.set(x, G + 10, z, SATE if hoek else TRALIES, {"axis": "y"} if hoek else
                         {"north": str(x != 7 and False).lower(), "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
                self.set(x, G + 11, z, KOPER_PLAAT, {"type": "bottom", "waterlogged": "false"})
        for x, z in ((7, 14), (7, 16)):
            self.set(x, G + 10, z, TRALIES, {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"})
        for x, z in ((6, 15), (8, 15)):
            self.set(x, G + 10, z, TRALIES, {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
        self.set(7, G + 10, 15, UIENLICHT)
        self.set(7, G + 11, 15, KOPER)
        self.set(7, G + 12, 15, "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
        for x in (6, 8):
            self.set(x, G + 9, 15, KOPER)
        # orange banners on the two inner front posts
        for z in (10, 20):
            self.set(SX1 + 1, G + 4, z, "orange_wall_banner", {"facing": "east"})
        # the name sign on the middle front post, towards the yard
        self.bord(SX1 + 1, G + 3, 15, "east", ["~ Sausloper-stal ~", "Proefritjes:", "vraag het de", "Verzorger-guh"])

    def bord(self, x, y, z, facing, regels):
        import sign_text
        B = self.h.Byte
        leeg = sign_text.messages("sign.guhs.sausdieren", ["", "", "", ""])
        nbt = {"id": "minecraft:sign", "is_waxed": B(1),
               "front_text": {"messages": sign_text.messages("sign.guhs.sausdieren", regels), "color": "yellow", "has_glowing_text": B(1)},
               "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}}
        self.set(x, y, z, "crimson_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)

    # --- the yard ----------------------------------------------------------------------------------------------------------
    def erf(self):
        # paved: the strip in front of the stable, the way to the pier, the entrance path from the south
        self.pad([(x, z) for x in range(13, 19) for z in range(6, 25) if not in_bak(x, z, 1.25)])
        self.pad([(x, z) for x in range(14, 17) for z in range(25, 31)])
        # the pindasaus-tuintje: a bed of nylium full of pindascheutjes between two low walls, south of the yard
        for x, z in ((18, 26), (19, 26), (20, 26), (18, 27), (19, 27), (20, 27)):
            if (x, z) in self.plaat and not in_bak(x, z, 1.25):
                self.set(x, G, z, NYLIUM)
                self.set(x, G + 1, z, SCHEUTJES)
        for x, z in ((17, 26), (17, 27), (21, 26), (21, 27)):
            if (x, z) in self.plaat and not in_bak(x, z, 1.25):
                self.set(x, G + 1, z, MUUR, wall_props())
        # the brushing spot: puddles of pindasaus next to the keeper and a wash tub
        for x, z in ((14, 17), (14, 18), (15, 18)):
            self.set(x, G + 1, z, PLASJE)
        self.set(13, G + 1, 19, "cauldron")
        # a wheelbarrow of hay in the north corner (far from the sauce) and the saddle rack next to it
        self.set(14, G + 1, 6, TRAP, {"facing": "north", "half": "top", "shape": "straight", "waterlogged": "false"})
        self.set(14, G + 1, 7, "hay_block", {"axis": "y"})
        self.set(14, G + 1, 8, MUUR, wall_props())
        self.set(13, G + 1, 7, "hay_block", {"axis": "x"})
        # lantern posts along the yard
        for x, z in ((18, 7), (18, 23), (13, 27)):
            if (x, z) in self.plaat and not in_bak(x, z, 1.25):
                for y in range(G + 1, G + 4):
                    self.set(x, y, z, MUUR, wall_props())
                self.set(x, G + 4, z, "lantern", {"hanging": "false", "waterlogged": "false"})
        # an entrance arch over the path, with a guh ear on top and the welcome sign
        for x in (13, 17):
            for y in range(G + 1, G + 5):
                self.set(x, y, 29, SATE, {"axis": "y"})
        for x in range(13, 18):
            self.set(x, G + 5, 29, SATE, {"axis": "x"})
        for x in (14, 16):
            self.set(x, G + 6, 29, OOR)
        self.set(15, G + 6, 29, PLAAT, {"type": "bottom", "waterlogged": "false"})
        self.set(15, G + 4, 29, "lantern", {"hanging": "true", "waterlogged": "false"})
        self.bord(15, G + 5, 30, "south", ["Welkom bij de", "Sausloper-stal", "Niet voeren?", "Juist wel! Njeg"])

    def midden(self, x, z):
        """The centre jigsaw, in layer 0. The barbecueput type puts the jigsaw's piece on the cave floor and sinks it by the
        start pool's ground_level_delta, and the terrain is smoothed towards that same level: sausdieren.stal gives the pool
        ground_level_delta = G + 1, so layer G is the top block of the cave floor and the land meets the yard (with the
        jigsaw in layer G and the default delta of 1 the stable stood on a pedestal three blocks high)."""
        floor = self.s.blocks.get((x, 0, z))
        name, props = (floor[0], floor[1]) if floor else (HOUTSKOOL, {})
        final = name + ("[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]" if props else "")
        self.s.set(x, 0, z, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": MIDDEN, "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": final, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


def bouw(h, wezens=True):
    """The template (not saved) and the problems of its self-check."""
    b = Bouw(h)
    b.grond()
    b.sausbak()
    b.erf()
    b.stal()
    b.steiger()
    b.poorten()
    b.midden(17, 15)
    if wezens:
        from features import wereld
        x, y, z = PLEKKEN["NPC"]
        b.s.entity(x + 0.5, y, z + 0.5, wereld.npc(h, "verzorgerguh", "sausdieren_verzorger", yaw=NPC_YAW))
        for i, (x, y, z) in enumerate(PLEKKEN["BEWONERS"]):
            b.s.entity(x + 0.5, y, z + 0.5, {"id": "guhs:sausloper", "PersistenceRequired": h.Byte(1), "Invulnerable": h.Byte(1),
                                             "Rotation": h.floats(float(90 * i), 0.0), "NeoForgeData": {"guhs_bezetting": f"sausdieren_bewoner_{i}"}})
    return b.s, check(b.s)


# =====================================================================================================================
# the self-check
# =====================================================================================================================
BRANDBAAR = ("hay_block", "barrel", "chest", "wool", "carpet", "oak", "spruce", "birch", "bookshelf", "smithing_table", "_sign")
DUN = (SCHEUTJES, PLASJE, "minecraft:lantern", AIR)


def check(s):
    problems = []
    blocks = s.blocks

    def nm(c):
        b = blocks.get(c)
        return b[0] if b else None

    saus = [c for c, b in blocks.items() if b[0] == SAUS]
    if len(saus) < 150:
        problems.append(f"the basin holds only {len(saus)} blocks of sauce")
    # the sauce is held: rock under it, and next to it more sauce or something solid
    for (x, y, z) in saus:
        if y != G:
            problems.append(f"sauce outside the ground layer at {(x, y, z)}")
        if nm((x, y - 1, z)) in (None, AIR, SAUS):
            problems.append(f"the sauce at {(x, y, z)} has nothing under it")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = nm((x + dx, y, z + dz))
            if n in (None, AIR) or n in DUN:
                problems.append(f"the sauce at {(x, y, z)} can leak to {(x + dx, y, z + dz)}")
    # nothing that lava sets on fire within 3 blocks of the sauce (the sauce burns like lava)
    sx = {(x, z) for x, _, z in saus}
    for (x, y, z), b in blocks.items():
        if any(t in b[0] for t in BRANDBAAR) and "crimson" not in b[0] and "warped" not in b[0]:
            if any((x + dx, z + dz) in sx for dx in range(-3, 4) for dz in range(-3, 4)):
                problems.append(f"{b[0]} at {(x, y, z)} is within 3 blocks of the sauce (it would burn)")
    # nothing floats
    solid = {c for c, b in blocks.items() if b[0] != AIR}
    seen = {c for c in solid if c[1] <= G}
    todo = list(seen)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in solid and n not in seen:
                seen.add(n)
                todo.append(n)
    for c in sorted(solid - seen):
        problems.append(f"floating block {nm(c)} at {c}")
    for c, b in blocks.items():
        below = nm((c[0], c[1] - 1, c[2]))
        if b[0] in (PLASJE, SCHEUTJES, "minecraft:chest", "minecraft:barrel", "minecraft:hay_block", "minecraft:cauldron") or \
                (b[0] == "minecraft:lantern" and b[1].get("hanging") == "false"):
            if below in (None, AIR, PLASJE, SCHEUTJES, SAUS):
                problems.append(f"{b[0]} at {c} stands on nothing")
        if b[0] == "minecraft:lantern" and b[1].get("hanging") == "true" and nm((c[0], c[1] + 1, c[2])) in (None, AIR):
            problems.append(f"the hanging lantern at {c} hangs from nothing")
        if b[0] == SCHEUTJES and below not in (NYLIUM, HOUTSKOOL, AS, AS_AARDE):
            problems.append(f"pindascheutjes at {c} on {below}")
    # the keeper sits on the yard; the lap is free: sauce under every spot, four blocks of air above it
    x, y, z = PLEKKEN["NPC"]
    if nm((x, y - 1, z)) in (None, AIR, SAUS) or nm((x, y, z)) != AIR or nm((x, y + 1, z)) != AIR:
        problems.append(f"the Verzorger-guh has no place to sit at {PLEKKEN['NPC']}")
    for naam, plekken in (("start", [PLEKKEN["START"]]), ("gate", PLEKKEN["POORTEN"]), ("resident", PLEKKEN["BEWONERS"][:2])):
        for (x, y, z) in plekken:
            if nm((x, y - 1, z)) != SAUS:
                problems.append(f"the {naam} spot {(x, y, z)} is not on the sauce")
            for dy in range(0, 4):
                if nm((x, y + dy, z)) != AIR:
                    problems.append(f"the {naam} spot {(x, y, z)} is blocked at +{dy} by {nm((x, y + dy, z))}")
    x, y, z = PLEKKEN["BEWONERS"][2]
    if nm((x, y - 1, z)) not in (GLOEIKOOL, STENEN) or nm((x, y, z)) != AIR:
        problems.append(f"the stall resident has no stall at {(x, y, z)}")
    if [c[1] for c, b in blocks.items() if b[0] == "minecraft:jigsaw" and b[2] and b[2].get("name") == MIDDEN] != [0]:
        problems.append("the centre jigsaw belongs in layer 0, exactly once")
    return problems


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
class _H:
    """What bouw() needs of the make_v2 namespace, for the preview."""
    def __init__(self):
        import make_structures as ms
        self.ms, self.Structure, self.Byte, self.floats = ms, ms.Structure, ms.Byte, ms.floats


def gedraaid(s, kwart):
    """The same structure turned `kwart` quarter turns (the renderer always looks from the front-left)."""
    import make_structures as ms
    W, H, D = s.size
    for _ in range(kwart % 4):
        t = ms.Structure((D, H, W))
        for (x, y, z), b in s.blocks.items():
            t.blocks[(D - 1 - z, y, x)] = b
        s, (W, H, D) = t, t.size
    return s


def preview(out):
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    wr.SPECIAL_COLOURS["guhs:kaasfrituursaus"] = (240, 150, 36)
    wr.SPECIAL_COLOURS["minecraft:lantern"] = (255, 214, 120)
    wr.SPECIAL_COLOURS["guhs:pindascheutjes"] = (214, 150, 60)
    for k in (KOPER, KOPER_TRAP, KOPER_PLAAT):
        wr.SPECIAL_COLOURS[k] = (196, 110, 78)
    wr.SPECIAL_COLOURS["minecraft:orange_wall_banner"] = (240, 118, 20)
    wr.SPECIAL_COLOURS["minecraft:crimson_wall_sign"] = (126, 58, 86)
    wr.SPECIAL_COLOURS["minecraft:crimson_fence_gate"] = (126, 58, 86)
    wr.SPECIAL_COLOURS["minecraft:lightning_rod"] = (200, 120, 90)
    wr.SPECIAL_COLOURS[HEK] = (57, 47, 47)
    wr.block_colour.cache_clear()
    os.makedirs(out, exist_ok=True)
    s, problems = bouw(_H(), wezens=False)
    for p in problems:
        print("PROBLEM:", p)
    for k in range(4):
        wr.render_structure(gedraaid(s, k), {}, px=18, max_size=1500).save(os.path.join(out, f"sausloper_stal_{k}.png"))
    # a floor plan of the ground layer + everything standing on it
    from PIL import Image
    W, H, D = s.size
    plan = Image.new("RGB", (W * 16, D * 16), (20, 16, 18))
    for (x, y, z), b in sorted(s.blocks.items(), key=lambda t: t[0][1]):
        if b[0] in (AIR, "minecraft:jigsaw") or y < G or y > G + 2:
            continue
        c = wr.block_colour(b[0])
        if c:
            k = 1.0 if y == G else 0.8
            plan.paste(tuple(int(v * k) for v in c), (x * 16 + (y - G) * 2, z * 16 + (y - G) * 2, x * 16 + 16 - (y - G) * 2, z * 16 + 16 - (y - G) * 2))
    plan.save(os.path.join(out, "sausloper_stal_plan.png"))
    print("size", s.size, "blocks", sum(1 for b in s.blocks.values() if b[0] != AIR))


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
