"""
bbq2 (fossiel-mijn): the two buildings of features/fossiel_mijn.py, built block by block.

  fossiel_opgraving   a dig on a cave floor of the Guhbarbecuether: a terraced pit with the half-buried skeleton of a giant
                      guh (belly down: the spine on top, the ribs a tunnel you walk through, the skull with its ears and
                      glowing eye sockets looking at whoever comes down the ramp), heaps of dug-out ash, a plank walk with
                      a hoist over the pit, the Archeoloog-guh's tent with his finds table, and the stand under a canopy
                      where every player puts their own little skeleton together.
  zoutkristalmijn     a salt mountain on a cave floor, white on top and bristling with crystal spires: a timbered mouth in
                      a rock face, a cart track with fallen rock on it winding through the mountain, a side gallery, and at
                      the end the crystal grotto, sunk a few steps, with the vein in its wall. In front: the Mijnwerker-guh's
                      shed, a buffer, a crane, barrels and a heap of salt.

Both are ONE template for the type guhs:barbecueput (wereld.bbq_structuur soort "grot"). The centre jigsaw is in layer 0 and
the start pool gets ground_level_delta = G + 1 (fossiel_mijn.structuren), so the ground you walk on (top block at template
y = G) lands exactly on the cave floor and the terrain is smoothed towards it, while the pit and the grotto go below it.

Numbers the Java side shares (FossielmijnFeature): ARCHEOLOOG, MIJNWERKER (the NPCs' template spots).
check(...) is the geometry self-check that build() runs: nothing floats, the NPCs sit, enough bottenzand / puin / vein, the
track is whole, every cavity of the mountain is closed in by rock.
"""
import math
import random

OPGRAVING = "fossiel_opgraving"
MIJN = "zoutkristalmijn"
OPGRAVING_MAAT = (41, 16, 41)
MIJN_MAAT = (43, 27, 43)
G_OPGRAVING = 3          # template y of the top block of the ground (you walk at G + 1)
G_MIJN = 5
# the NPCs (template block positions, feet level): the same numbers as FossielmijnFeature.ARCHEOLOOG_PLEK / MIJNWERKER_PLEK
ARCHEOLOOG = (27, G_OPGRAVING + 1, 28)
ARCHEOLOOG_YAW = 110.0   # (looks west-north-west: at the path and the pit)
MIJNWERKER = (27, G_MIJN + 1, 36)
MIJNWERKER_YAW = 90.0    # (looks west, at the track)

HOUTSKOOL = "guhs:houtskoolsteen"
STENEN = "guhs:houtskoolsteen_stenen"
GEBARSTEN = "guhs:gebarsten_houtskoolsteen_stenen"
GEBEITELD = "guhs:gebeitelde_houtskoolsteen_stenen"
MUUR = "guhs:houtskoolsteen_stenen_muur"
PLAAT = "guhs:houtskoolsteen_stenen_plaat"
TRAP = "guhs:houtskoolsteen_stenen_trap"
ROOSTER = "guhs:roosterijzer"
GEPOLIJST = "guhs:gepolijst_roosterijzer"
AS = "guhs:as_blok"
AS_AARDE = "guhs:as_aarde"
VERKOOLD = "guhs:verkoold_guhbot"
SMEUL = "guhs:smeulkooltjes"
BOT = "minecraft:bone_block"
KALK = "minecraft:calcite"
ZOUTHOOP = "minecraft:white_concrete_powder"
STAM = "minecraft:spruce_log"
PLANK = "minecraft:spruce_planks"
BOTTENZAND = "guhs:fossielmijn_bottenzand"
REK = "guhs:fossielmijn_skeletrek"
PUIN = "guhs:fossielmijn_puin"
ADER = "guhs:fossielmijn_zoutader"
ERTS = "guhs:fossielmijn_zoutkristalerts"
KRISTALBLOK = "guhs:fossielmijn_zoutkristalblok"
KRISTALLETJES = "guhs:fossielmijn_zoutkristalletjes"
AIR = "minecraft:air"

FENCE = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
WALL = {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"}
REK_UIT = {"schedel": "false", "ruggengraat": "false", "ribben": "false", "pootjes": "false", "staart": "false"}


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


def ruis(seed):
    """Smooth 2D value noise in 0..1 (a function of x, z), for hill shapes and patchy ground."""
    rng = random.Random(seed)
    grid = [[rng.random() for _ in range(64)] for _ in range(64)]

    def f(x, z, schaal=6.0):
        x, z = x / schaal, z / schaal
        x0, z0 = int(math.floor(x)), int(math.floor(z))
        fx, fz = x - x0, z - z0
        fx, fz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
        a, b = grid[z0 % 64][x0 % 64], grid[z0 % 64][(x0 + 1) % 64]
        c, d = grid[(z0 + 1) % 64][x0 % 64], grid[(z0 + 1) % 64][(x0 + 1) % 64]
        return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz
    return f


class Bouw:
    """A template under construction: the Structure, its ground level and what the self-check wants to know."""

    def __init__(self, h, naam, maat, g, seed):
        self.h = h
        self.naam = naam
        self.s = h.Structure(maat)
        self.W, self.H, self.D = maat
        self.G = g
        self.rng = random.Random(seed)
        self.npc = None
        self.vaten = []
        self.hol = set()

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def leeg(self, x, y, z):
        return self.get(x, y, z) in (None, AIR)

    def grond(self, cx, cz, straal, top, onder):
        """The ground: a frayed disc, `top(x, z, d)` on layer G, `onder` below it down to layer 0, air above (the cave is
        cleared over the whole site)."""
        for x in range(self.W):
            for z in range(self.D):
                d = math.hypot(x - cx, z - cz)
                if d > straal + self.rng.uniform(-1.3, 1.0):
                    continue
                self.set(x, self.G, z, top(x, z, d))
                for y in range(0, self.G):
                    self.set(x, y, z, onder)
                for y in range(self.G + 1, self.H):
                    self.set(x, y, z, AIR)

    def vloer(self, x, z):
        """The y of the highest block in this column at or under the ground (where something can stand)."""
        y = self.G
        while y > 0 and self.leeg(x, y, z):
            y -= 1
        return y

    def paal(self, x, z, y0, y1, name, props=None):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, name, props)

    def lantaarnpaal(self, x, z, hoog=3):
        y0 = self.G + 1
        self.paal(x, z, y0, y0 + hoog - 1, MUUR, WALL)
        self.set(x, y0 + hoog, z, "lantern", {"hanging": "false", "waterlogged": "false"})

    def lamp(self, x, y, z, hangend=False):
        self.set(x, y, z, "lantern", {"hanging": "true" if hangend else "false", "waterlogged": "false"})

    def plaat(self, x, y, z, boven=False, name="spruce_slab"):
        self.set(x, y, z, name, {"type": "top" if boven else "bottom", "waterlogged": "false"})

    def trap(self, x, y, z, facing, name="spruce_stairs", om=False):
        self.set(x, y, z, name, {"facing": facing, "half": "top" if om else "bottom", "shape": "straight", "waterlogged": "false"})

    def vat(self, x, y, z, loot=None, facing="up"):
        nbt = {"id": "minecraft:barrel"}
        if loot:
            nbt["LootTable"] = loot
            self.vaten.append((x, y, z))
        self.set(x, y, z, "barrel", {"facing": facing, "open": "false"}, nbt)

    def pot(self, x, y, z, facing="south"):
        self.set(x, y, z, "decorated_pot", {"facing": facing, "cracked": "false", "waterlogged": "false"}, {"id": "minecraft:decorated_pot"})

    def bord(self, x, y, z, regels, rotation=None, facing=None):
        """A waxed spruce sign: standing (rotation 0..15, 0 = its text faces south) or on a wall (facing = the side its text
        faces)."""
        import sign_text
        B = self.h.ms.Byte
        prefix = "sign.guhs.fossiel_mijn"
        nbt = {"id": "minecraft:sign", "is_waxed": B(1),
               "front_text": {"messages": sign_text.messages(prefix, regels), "color": "black", "has_glowing_text": B(0)},
               "back_text": {"messages": sign_text.messages(prefix, ["", "", "", ""]), "color": "black", "has_glowing_text": B(0)}}
        if facing:
            self.set(x, y, z, "spruce_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            self.set(x, y, z, "spruce_sign", {"rotation": str(rotation or 0), "waterlogged": "false"}, nbt)

    def midden(self, x, z):
        """The centre jigsaw, in layer 0 (see the module text): what the structure type puts on the cave floor."""
        self.s.set(x, 0, z, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": f"guhs:{self.naam}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": HOUTSKOOL, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


# =====================================================================================================================
# the Fossiel-opgraving
# =====================================================================================================================
# the pit: an ellipse around (PIT_X, PIT_Z); the outer ring is one step down, the inside two
PIT_X, PIT_Z, PIT_RX, PIT_RZ = 20, 16, 13.5, 8.6
# the giant skeleton lies along x at z = PIT_Z: the skull in the west, the tail in the east
RIBBEN_X = (15, 18, 21, 24)
BOTTENZAND_PLEKKEN = [(16, 16), (20, 15), (23, 17), (17, 11), (22, 11), (13, 21), (19, 21), (25, 21), (29, 13), (30, 19), (12, 11)]


def opgraving(h):
    b = Bouw(h, OPGRAVING, OPGRAVING_MAAT, G_OPGRAVING, 31101)
    W, H, D = OPGRAVING_MAAT
    G = b.G
    cx, cz = W // 2, D // 2                                   # 20, 20
    vlek = ruis(31102)

    def pit(x, z):
        return math.hypot((x - PIT_X) / PIT_RX, (z - PIT_Z) / PIT_RZ)

    def pad(x, z):
        """The paved path: from the gate north to the ramp, with a branch to the tent and one to the stand."""
        return (19 <= x <= 21 and 26 <= z <= 37) or (z in (29, 30) and 11 <= x <= 28)

    def top(x, z, d):
        if pad(x, z):
            return GEBARSTEN if (x * 5 + z * 3) % 4 == 0 else STENEN
        n = vlek(x, z)
        if n > 0.64:
            return HOUTSKOOL
        return AS if n < 0.32 else AS_AARDE
    b.grond(cx, cz, 19.4, top, AS_AARDE)

    # --- the pit: two terraces down into the ash ---
    for x in range(W):
        for z in range(D):
            p = pit(x, z)
            if p <= 1.0 and b.get(x, G, z) is not None:
                diep = 2 if p <= 0.82 or x < 14 else 1
                for y in range(G - diep + 1, G + 1):
                    b.set(x, y, z, AIR)
                b.set(x, G - diep, z, AS if vlek(x + 11, z + 5, 3.0) < 0.62 else AS_AARDE)
    # the rim: shored up with ash bricks here and there
    for x in range(W):
        for z in range(D):
            if 1.0 < pit(x, z) <= 1.13 and b.get(x, G, z) is not None and vlek(x + 40, z) > 0.5 and not pad(x, z):
                b.set(x, G, z, GEBARSTEN if (x + z) % 3 else STENEN)

    # --- the giant guh skeleton, belly down ---
    vloer = G - 2                                             # the pit's floor block; you walk at vloer + 1
    rug = vloer + 5                                           # the spine: four blocks of room under it
    for x in range(13, 30):
        b.set(x, rug, PIT_Z, BOT, {"axis": "x"})
    for x in (15, 18, 21, 24, 27):                            # the spikes on its back
        b.set(x, rug + 1, PIT_Z, VERKOOLD if x == 21 else BOT, {"axis": "y"})
    for i, x in enumerate(RIBBEN_X):
        for kant in (-1, 1):
            if i == 2 and kant == 1:                          # the third south rib broke off: its pieces lie in the ash
                b.set(x, rug, PIT_Z + 1, BOT, {"axis": "z"})
                b.set(x, vloer + 1, PIT_Z + 3, VERKOOLD, {"axis": "x"})
                b.set(x + 1, vloer + 1, PIT_Z + 4, VERKOOLD, {"axis": "z"})
                continue
            b.set(x, rug, PIT_Z + kant, BOT, {"axis": "z"})
            b.set(x, rug, PIT_Z + 2 * kant, BOT, {"axis": "z"})
            for y in range(vloer + 1, rug):
                zwart = (i + y + kant) % 5 == 0
                b.set(x, y, PIT_Z + 3 * kant, VERKOOLD if zwart else BOT, {"axis": "y"})
    # the hips, the hind legs and the tail (it curls off to the north-east and sinks into the pit's side)
    for dz in range(-3, 4):
        b.set(27, rug, PIT_Z + dz, BOT, {"axis": "z"})
    for kant in (-1, 1):
        for y in range(vloer + 1, rug):
            b.set(27, y, PIT_Z + 3 * kant, BOT, {"axis": "y"})
        b.set(26, vloer + 1, PIT_Z + 3 * kant, BOT, {"axis": "x"})         # a foot
        b.set(25, vloer + 1, PIT_Z + 3 * kant, VERKOOLD, {"axis": "x"})    # with a charred toe
    for i, (x, dy, dz) in enumerate(((30, 0, 0), (31, 0, 0), (31, -1, 0), (32, -1, 0), (32, -1, -1), (33, -1, -1), (33, -2, -1), (34, -2, -1),
                                     (34, -3, -1))):
        b.set(x, rug + dy, PIT_Z + dz, VERKOOLD if i == 5 else BOT, {"axis": "y" if i in (2, 6, 8) else "x"})
    # the skull: 5 x 5 x 5 and hollow, its face to the south (to whoever comes down the ramp): two eye sockets that glow
    # (a lamp inside), a nose, gap teeth, two round ears; the neck comes in at the top of its back
    sx0, sz0 = 8, PIT_Z - 2                                   # x 8..12, z 14..18
    for x in range(sx0, sx0 + 5):
        for z in range(sz0, sz0 + 5):
            for y in range(vloer + 1, vloer + 6):
                rand = x in (sx0, sx0 + 4) or z in (sz0, sz0 + 4) or y == vloer + 5
                hoekje = y == vloer + 5 and x in (sx0, sx0 + 4) and z in (sz0, sz0 + 4)
                b.set(x, y, z, (BOT if not hoekje else AIR) if rand else AIR, {"axis": "y"} if rand and not hoekje else None)
    zf = sz0 + 4                                              # the face
    for x in (sx0 + 1, sx0 + 3):                              # the eye sockets: deep and dark
        for y in (vloer + 3, vloer + 4):
            b.set(x, y, zf, AIR)
            b.set(x, y, zf - 1, "coal_block")
    b.set(sx0 + 2, vloer + 2, zf, AIR)                        # the nose
    b.set(sx0 + 2, vloer + 2, zf - 1, "coal_block")
    b.set(sx0 + 1, vloer + 1, zf, AIR)                        # the gaps between its teeth
    b.set(sx0 + 3, vloer + 1, zf, AIR)
    b.set(sx0 + 1, vloer + 1, zf - 1, "coal_block")
    b.set(sx0 + 3, vloer + 1, zf - 1, "coal_block")
    for x in (sx0, sx0 + 4):                                  # the ears: round plates on the corners of its head
        for z in (sz0 + 1, sz0 + 2, sz0 + 3):
            b.set(x, vloer + 6, z, BOT, {"axis": "z"})
        b.set(x, vloer + 7, sz0 + 2, VERKOOLD if x == sx0 else BOT, {"axis": "z"})
    b.set(13, rug, PIT_Z, BOT, {"axis": "x"})                 # the neck joins the back of the head (x 12 is the skull's wall)

    # --- the bottenzand: where the brush finds something; pegs with little flags mark the dig's grid ---
    for x, z in BOTTENZAND_PLEKKEN:
        b.set(x, b.vloer(x, z), z, BOTTENZAND, {"leeg": "false"})
    for i, (x, z) in enumerate(((14, 22), (26, 22), (14, 10), (26, 10), (20, 23), (31, 16), (20, 9))):
        y = b.vloer(x, z)
        if b.get(x, y, z) != BOTTENZAND and b.leeg(x, y + 1, z) and b.leeg(x, y + 2, z):
            b.set(x, y + 1, z, "spruce_fence", FENCE)
            b.set(x, y + 2, z, "pink_carpet" if i % 2 else "white_carpet")

    # --- the way down: two plank steps from the path into the pit ---
    for x in (19, 20, 21):
        b.trap(x, G, 25, "south")
        b.trap(x, G - 1, 24, "south")
        b.set(x, G, 24, AIR)
        for z in (23, 22):
            b.set(x, G, z, AIR)
            b.set(x, G - 1, z, AIR)
            b.set(x, G - 2, z, PLANK)

    # --- heaps of dug-out ash around the pit ---
    for (hx, hz, hr, hh) in ((8, 5, 4.2, 4), (32, 6, 3.6, 3), (4, 17, 3.0, 2), (36, 16, 2.6, 2)):
        for x in range(W):
            for z in range(D):
                d = math.hypot(x - hx, z - hz)
                if d < hr and b.get(x, G, z) is not None and pit(x, z) > 1.2:
                    top_y = G + max(1, int(round(hh * (1 - (d / hr) ** 1.7))))
                    for y in range(G + 1, top_y + 1):
                        b.set(x, y, z, AS if (x + y + z) % 4 else AS_AARDE)
    y = G + 1
    while not b.leeg(8, y, 5):
        y += 1
    b.set(8, y, 5, "spruce_fence", FENCE)                     # a flag on the biggest heap
    b.set(8, y + 1, 5, "pink_carpet")

    # --- the plank walk with the hoist, over the north rim ---
    py = G + 4
    for x in range(13, 28):
        for z in (6, 7):
            b.plaat(x, py, z)
    for x in (13, 20, 27):
        for z in (6, 7):
            y = py - 1
            while y > 0 and b.leeg(x, y, z):
                b.set(x, y, z, "spruce_fence", FENCE)
                y -= 1
    for x in range(13, 28):                                   # a railing on the outside (north)
        b.set(x, py + 1, 6, "spruce_fence", FENCE)
    for y in range(G + 1, py + 1):                            # the ladder up, on the west end
        b.set(12, y, 7, PLANK)
        b.set(12, y, 8, "ladder", {"facing": "south", "waterlogged": "false"})
    # the hoist: a beam out over the pit, a chain, and a bone in a sling on its way up
    b.paal(20, 6, py + 1, py + 3, "spruce_fence", FENCE)
    b.paal(20, 7, py + 1, py + 3, "spruce_fence", FENCE)
    for z in range(8, 13):
        b.set(20, py + 3, z, "spruce_fence", FENCE)
    for y in range(py, py + 3):
        b.set(20, y, 12, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(20, py - 1, 12, BOT, {"axis": "x"})
    for x in (16, 24):
        b.paal(x, 6, py + 1, py + 2, "spruce_fence", FENCE)
        b.lamp(x, py + 3, 6)

    # --- the Archeoloog-guh's tent (south-east): an A-frame of white canvas with a pink stripe, open to the west ---
    tx0, tx1, tz0 = 30, 36, 26                                # x 30..36, z 26..30; the ridge runs east-west
    for x in range(tx0, tx1 + 1):
        for dz, dy in ((0, 1), (0, 2), (1, 3), (2, 4), (3, 3), (4, 2), (4, 1)):
            b.set(x, G + dy, tz0 + dz, "pink_wool" if dy == 2 else "white_wool")
        for dz in (1, 2, 3):
            b.set(x, G, tz0 + dz, PLANK)
    for x in range(tx0 - 3, tx0):                             # the awning in front of it, on two poles: the workplace
        for dz in (0, 1, 2, 3, 4):
            b.set(x, G + 4, tz0 + dz, "pink_wool" if dz in (0, 4) else "white_wool")
    for dz in (0, 4):
        b.paal(tx0 - 3, tz0 + dz, G + 1, G + 3, "spruce_fence", FENCE)
    for dz, dy in ((1, 1), (1, 2), (2, 1), (2, 2), (2, 3), (3, 1), (3, 2)):   # the closed back (east)
        b.set(tx1, G + dy, tz0 + dz, "white_wool")
    b.set(tx1, G + 2, tz0 + 2, "pink_stained_glass_pane", {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
    b.set(tx1 - 1, G + 1, tz0 + 1, "red_bed", {"facing": "east", "part": "head", "occupied": "false"}, {"id": "minecraft:bed"})
    b.set(tx1 - 2, G + 1, tz0 + 1, "red_bed", {"facing": "east", "part": "foot", "occupied": "false"}, {"id": "minecraft:bed"})
    b.vat(tx1 - 1, G + 1, tz0 + 3, "guhs:chests/fossielmijn_opgraving")
    b.pot(tx1 - 1, G + 2, tz0 + 3, "west")
    b.set(tx1 - 2, G + 1, tz0 + 3, "bookshelf")
    b.set(tx1 - 4, G + 1, tz0 + 3, "lectern", {"facing": "north", "has_book": "false", "powered": "false"}, {"id": "minecraft:lectern"})
    b.lamp(tx1 - 3, G + 3, tz0 + 2, hangend=True)
    b.set(tx0 + 1, G + 1, tz0 + 2, "pink_carpet")
    b.set(tx0, G + 1, tz0 + 2, "white_carpet")
    b.bord(tx0 - 4, G + 4, tz0 + 2, ["Prof. dr. Guh", "Archeoloog", "(niet storen", "bij het kwasten)"], facing="west")
    b.lamp(tx0 - 2, G + 3, tz0 + 1, hangend=True)
    # his desk under the awning: a slab on two barrels, a pot of brushes
    b.vat(tx0 - 2, G + 1, tz0 + 4)
    b.pot(tx0 - 2, G + 2, tz0 + 4, "west")
    b.set(tx0 - 1, G + 1, tz0 + 4, "bookshelf")

    # --- the finds table and the washing-up, by the path in front of the tent ---
    b.npc = ARCHEOLOOG
    b.set(ARCHEOLOOG[0], G, ARCHEOLOOG[2], "pink_wool")      # his cushion
    for x in (24, 25, 26):
        b.set(x, G + 1, 32, "spruce_fence", FENCE)
        b.set(x, G + 2, 32, "spruce_pressure_plate", {"powered": "false"})
    b.set(27, G + 1, 32, "cartography_table")
    b.pot(27, G + 2, 32, "north")
    b.set(29, G + 1, 32, "powder_snow_cauldron", {"level": "3"})          # the tub of plaster
    b.set(30, G + 1, 32, "grindstone", {"face": "floor", "facing": "north"})
    b.vat(23, G + 1, 33)
    b.vat(23, G + 2, 33, facing="north")
    b.vat(22, G + 1, 33)
    b.pot(22, G + 2, 33, "north")
    # a camp fire with two log seats
    b.set(26, G + 1, 35, "campfire", {"lit": "true", "facing": "north", "signal_fire": "false", "waterlogged": "false"}, {"id": "minecraft:campfire"})
    b.set(24, G + 1, 35, STAM, {"axis": "z"})
    b.set(28, G + 1, 35, STAM, {"axis": "z"})

    # --- the stand, on a stone plinth under a canopy (south-west): the little skeleton looks east, at the path ---
    rx, rz = 9, 29
    for x in range(rx - 3, rx + 4):
        for z in range(rz - 2, rz + 3):
            b.set(x, G, z, GEBEITELD if abs(x - rx) == 3 and abs(z - rz) == 2 else STENEN)
    for x in range(rx - 2, rx + 3):
        for z in range(rz - 1, rz + 2):
            b.set(x, G + 1, z, GEPOLIJST)
    b.set(rx, G + 2, rz, REK, {"facing": "east", **REK_UIT})
    for x in (rx - 3, rx + 3):
        for z in (rz - 2, rz + 2):
            b.paal(x, z, G + 1, G + 4, MUUR, WALL)
    for x in range(rx - 3, rx + 4):
        for z in range(rz - 2, rz + 3):
            b.plaat(x, G + 5, z, boven=not (abs(x - rx) == 3 or abs(z - rz) == 2))
    for x in range(rx - 2, rx + 3):                           # a ridge on the canopy
        b.plaat(x, G + 6, rz)
    b.lamp(rx - 2, G + 4, rz - 1, hangend=True)
    b.lamp(rx + 2, G + 4, rz + 1, hangend=True)
    b.bord(rx + 4, G + 1, rz + 2, ["Hier komt:", "Tyrannoguhrus", "Njex", "(zelf bouwen!)"], rotation=12)

    # --- the gate in the south: two pillars, a beam and the name board ---
    for x in (17, 23):
        b.paal(x, 37, G + 1, G + 3, STENEN)
        b.set(x, G + 4, 37, GEBEITELD)
        b.lamp(x, G + 5, 37)
    for x in range(18, 23):
        b.plaat(x, G + 4, 37, boven=True)
    b.set(20, G + 3, 37, PLANK)
    b.set(19, G + 3, 37, "spruce_fence", FENCE)
    b.set(21, G + 3, 37, "spruce_fence", FENCE)
    b.bord(20, G + 3, 38, ["~ Opgraving ~", "Pas op: vers", "gekwaste", "botten!"], facing="south")
    for (x, z) in ((5, 12), (35, 11), (3, 24), (37, 22), (13, 36), (30, 36)):
        if b.get(x, G, z) is not None and b.leeg(x, G + 1, z):
            b.lantaarnpaal(x, z, 2)
    # the sieve: a frame on four legs with a little heap of ash under it; crates of bones by the ramp
    for x, z in ((14, 33), (16, 33), (14, 35), (16, 35)):
        b.set(x, G + 1, z, "spruce_fence", FENCE)
    for x in range(14, 17):
        for z in range(33, 36):
            b.set(x, G + 2, z, "spruce_trapdoor", {"facing": "north", "half": "bottom", "open": "false", "powered": "false", "waterlogged": "false"})
    b.set(15, G + 1, 34, AS)
    b.vat(23, G + 1, 26)
    b.set(24, G + 1, 26, BOT, {"axis": "x"})
    b.set(23, G + 2, 26, BOT, {"axis": "z"})
    for (x, z) in ((15, 26), (16, 26), (17, 26), (15, 27), (16, 27)):   # finds laid out on a sheet, west of the ramp
        if b.leeg(x, G + 1, z) and not b.leeg(x, G, z):
            b.set(x, G + 1, z, "white_carpet")
    b.set(14, G + 1, 26, VERKOOLD, {"axis": "z"})
    b.set(14, G + 1, 27, BOT, {"axis": "x"})
    b.bord(17, G + 1, 27, ["Verboden te", "knabbelen", "aan de", "vondsten. Njeg!"], rotation=0)
    for _ in range(18):                                       # embers in the ash, away from the camp
        x, z = b.rng.randrange(3, W - 3), b.rng.randrange(3, D - 3)
        if b.get(x, G, z) in (AS, AS_AARDE, HOUTSKOOL) and b.leeg(x, G + 1, z) and pit(x, z) > 1.3 and (z < 24 or x < 5 or x > 37):
            b.set(x, G + 1, z, SMEUL)
    b.midden(cx, cz)
    return b


# =====================================================================================================================
# the Zoutkristalmijn
# =====================================================================================================================
BERG_X, BERG_Z, BERG_RX, BERG_RZ = 21, 18, 17.5, 14.5
# the mountain: a steep foot (BERG_VOET high) with two peaks on it: (x, z, how high, how wide)
BERG_VOET = 7.5
PIEKEN = [(18, 14, 19.0, 13.0), (28, 20, 13.5, 9.0)]
MOND = (21, 32)                                               # the tunnel mouth (x, z); the track runs north from here
# the track at walking level: corner points (x, z) from the buffer in the yard to its end on the landing of the grotto
SPOOR = [(21, 38), (21, 27), (12, 27), (12, 18), (17, 18)]
PUIN_PLEKKEN = [(21, 30, "z"), (21, 28, "z"), (18, 27, "x"), (15, 27, "x"), (12, 24, "z"), (12, 21, "z"), (14, 18, "x")]
GROT = (24, 16)                                               # the grotto's middle (x, z)
GROT_RX, GROT_RZ = 6.6, 6.0
SPITSEN = [(13, 10, 6), (26, 9, 7), (21, 6, 4), (7, 20, 4), (34, 18, 5), (31, 25, 4), (9, 27, 3), (22, 20, 5), (15, 21, 4)]   # (x, z, how tall)


def spoorcellen():
    """Every (x, z) of the track, in order."""
    out = []
    for (x0, z0), (x1, z1) in zip(SPOOR, SPOOR[1:]):
        n = max(abs(x1 - x0), abs(z1 - z0))
        for i in range(n):
            out.append((x0 + (x1 - x0) * i // n, z0 + (z1 - z0) * i // n))
    out.append(SPOOR[-1])
    return out


def mijn(h):
    b = Bouw(h, MIJN, MIJN_MAAT, G_MIJN, 31111)
    W, H, D = MIJN_MAAT
    G = b.G
    cx, cz = W // 2, D // 2                                   # 21, 21
    vlek = ruis(31112)
    bult = ruis(31113)
    aders = ruis(31114)
    mx, mz = MOND

    def top(x, z, d):
        n = vlek(x, z)
        if z > mz and abs(x - mx) <= 9 and (abs(x - mx) <= 2 or n > 0.3):
            return GEBARSTEN if (x * 3 + z) % 4 == 0 else STENEN   # the paved yard
        return HOUTSKOOL if n > 0.45 else AS_AARDE
    b.grond(cx, cz, 20.4, top, HOUTSKOOL)

    # --- what is hollow: the tunnels (3 wide, 3 high), the side gallery, the grotto ---
    hol = set()
    cellen = spoorcellen()
    for (x, z) in cellen:
        if z <= mz:
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    if z + dz <= mz:
                        for y in range(G + 1, G + 4):
                            hol.add((x + dx, y, z + dz))
    for x in range(22, 30):                                   # the side gallery, east from the first corner (lower at its end)
        for z in (26, 27, 28):
            for y in range(G + 1, G + (4 if x < 28 else 3)):
                hol.add((x, y, z))
    gx, gz = GROT
    grot_vloer = G - 3                                        # the grotto's floor block: you walk three steps below the track
    for x in range(W):
        for z in range(D):
            d = math.hypot((x - gx) / GROT_RX, (z - gz) / GROT_RZ)
            for y in range(grot_vloer + 1, G + 10):
                dy = (y - (G + 1)) / 8.0 if y > G + 1 else 0.0
                if d * d + dy * dy <= 1.0:
                    hol.add((x, y, z))
    b.hol = hol

    # --- the mountain: a steep dome with lumps, never less than three blocks of rock around anything hollow, a rock
    #     face over the mouth; white with salt on top, seams of crystal through it ---
    hoogte = {}
    for x in range(W):
        for z in range(D):
            d = math.hypot((x - BERG_X) / BERG_RX, (z - BERG_Z) / BERG_RZ)
            if d < 1.0:
                hgt = BERG_VOET * (1 - d ** 5)
                for (px, pz, hoog, breed) in PIEKEN:
                    hgt = max(hgt, hoog * max(0.0, 1 - math.hypot(x - px, z - pz) / breed) ** 1.15)
                hgt += (bult(x, z, 5.0) - 0.5) * 4.0 + (bult(x + 9, z + 4, 2.2) - 0.5) * 2.0
                hoogte[(x, z)] = max(0, min(H - G - 4, int(round(hgt))))
    for (x, y, z) in hol:
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                ver = math.hypot(dx, dz)
                if ver > 3.3 or z + dz > mz:                  # (no rock bulging out into the yard)
                    continue
                key = (x + dx, z + dz)
                need = (y - G) + (3 if ver <= 1.5 else 2)
                if hoogte.get(key, 0) < need:
                    hoogte[key] = need
    for x in range(mx - 5, mx + 6):                           # the rock face the mouth is cut into
        for z in range(mz - 2, mz + 1):
            need = 8 - abs(x - mx) // 2 - (mz - z)
            if hoogte.get((x, z), 0) < need:
                hoogte[(x, z)] = need
    for (x, z), hgt in hoogte.items():
        if not b.s.inside(x, 0, z):
            continue
        for y in range(0, G + hgt + 1):
            if (x, y, z) in hol:
                continue
            if y <= G:
                if b.get(x, y, z) is None:
                    b.set(x, y, z, HOUTSKOOL)
                continue
            buiten = y >= G + hgt - 1
            n = aders(x + y * 0.7, z - y * 0.4, 4.0)
            wit = (y - G) + (bult(x + 3, z + 7, 4.0) - 0.5) * 5.0     # how far up the mountain, with a wobble: the salt cap
            if abs(n - 0.5) < 0.045:
                steen = KRISTALBLOK if buiten or (x + y + z) % 3 else ERTS      # the salt seams that run through it
            elif wit > 11.5 and buiten:
                steen = KALK if (x * 3 + y + z * 5) % 7 else KRISTALBLOK
            elif wit > 9.0 and buiten and (x + z + y) % 3 == 0:
                steen = KALK
            elif bult(x + 20, z + y, 3.0) > 0.74:
                steen = ROOSTER
            else:
                steen = HOUTSKOOL
            b.set(x, y, z, steen)
    for (x, y, z) in hol:
        if b.s.inside(x, y, z):
            b.set(x, y, z, AIR)
    # the crystal spires: leaning pillars of salt that stick out of the mountain
    for i, (sx, sz, hoog) in enumerate(SPITSEN):
        voet = G + hoogte.get((sx, sz), 0)
        leun = ((1, 0), (0, 1), (-1, 0), (0, -1))[i % 4]
        for j in range(hoog + 2):
            y = voet + 1 + j
            ox, oz = (leun[0] * (j // 3), leun[1] * (j // 3))
            dik = [(0, 0), (1, 0), (0, 1), (1, 1)] if j < hoog * 0.45 else [(0, 0), (1, 0)] if j < hoog * 0.8 else [(0, 0)]
            for dx, dz in dik:
                if b.s.inside(sx + ox + dx, y, sz + oz + dz) and (sx + ox + dx, y, sz + oz + dz) not in hol:
                    b.set(sx + ox + dx, y, sz + oz + dz, KRISTALBLOK)
    # (a pillar stands on what is under it: fill whatever it leans out over, down to the rock)
    for (x, y, z) in sorted(c for c, v in b.s.blocks.items() if v[0] == KRISTALBLOK and c[1] > G + 1):
        y -= 1
        while y > G and b.leeg(x, y, z) and (x, y, z) not in hol:
            b.set(x, y, z, KRISTALBLOK)
            y -= 1
    # small crystals growing on the salt outside
    for (x, z), hgt in sorted(hoogte.items()):
        y = G + hgt
        if hgt >= 2 and b.s.inside(x, y + 1, z) and b.get(x, y, z) in (KRISTALBLOK, KALK) and b.leeg(x, y + 1, z) and (x * 7 + z * 13) % 11 == 0:
            b.set(x, y + 1, z, KRISTALLETJES)

    # --- the grotto: its floor, the landing where the track ends, the steps down, the vein, the big crystals ---
    for x in range(W):
        for z in range(D):
            if (x, grot_vloer + 1, z) in hol:
                n = aders(x, z, 3.0)
                b.set(x, grot_vloer, z, KRISTALBLOK if n > 0.68 else KALK if n > 0.6 else (AS_AARDE if vlek(x, z + 9) < 0.4 else HOUTSKOOL))
    ex, ez = SPOOR[-1]                                        # (17, 18): the end of the track
    for x in range(ex, ex + 3):                               # the landing: track level, three deep into the grotto
        for z in (ez - 1, ez, ez + 1):
            for y in range(grot_vloer + 1, G + 1):
                b.set(x, y, z, STENEN if y == G else HOUTSKOOL)
    for i in range(3):                                        # the steps down, east of the landing
        for z in (ez - 1, ez, ez + 1):
            y = G - i
            b.trap(ex + 3 + i, y, z, "west", TRAP)
            for yy in range(grot_vloer + 1, y):
                b.set(ex + 3 + i, yy, z, HOUTSKOOL)
    for z in (ez - 2, ez + 2):                                # a low wall along the landing and the steps
        for x in range(ex, ex + 5):
            if (x, G + 1, z) in hol:
                for yy in range(grot_vloer + 1, G + 1):
                    b.set(x, yy, z, HOUTSKOOL)
                b.set(x, G + 1, z, MUUR, WALL)
    # the vein: the grotto's wall, north and east, is crystal from the floor to above your head
    ader = []
    for x in range(W):
        for z in range(D):
            for y in range(grot_vloer + 1, grot_vloer + 5):
                if (x, y, z) in hol or b.get(x, y, z) is None:
                    continue
                raakt = any((x + dx, y, z + dz) in hol for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                hoek = math.degrees(math.atan2(z - gz, x - gx))          # 0 = east, -90 = north
                if raakt and -160 <= hoek <= 50 and x >= gx - 2:
                    ader.append((x, y, z))
    for (x, y, z) in ader:
        b.set(x, y, z, ADER if (x + y * 2 + z) % 5 else KRISTALBLOK)
    # crystals hanging from the ceiling (they light the grotto) and standing on the floor
    for (x, y, z) in sorted(hol):
        boven = (x, y + 1, z)
        if y >= G + 3 and boven not in hol and b.s.inside(*boven) and not b.leeg(*boven) and (x * 5 + z * 3) % 6 == 0:
            b.set(*boven, KRISTALBLOK)
            if (x + z) % 2 and (x, y - 1, z) in hol:
                b.set(x, y, z, KRISTALBLOK)                   # a stalactite of salt
    # the big crystal in the middle: a cluster of pillars, the tallest nearly to the roof
    for (dx, dz, top_y) in ((0, 0, 8), (1, 0, 5), (0, 1, 4), (-1, 0, 3), (1, 1, 3), (0, -1, 2), (-1, 1, 1), (2, 0, 1)):
        px, pz = gx + 1 + dx, gz + dz
        for y in range(grot_vloer + 1, grot_vloer + 1 + top_y):
            b.set(px, y, pz, KRISTALBLOK)
        if (px, grot_vloer + 1 + top_y, pz) in hol:
            b.set(px, grot_vloer + 1 + top_y, pz, KRISTALLETJES)
    for (x, z) in ((22, 12), (28, 19), (25, 11), (29, 14), (23, 20), (26, 21), (21, 13), (28, 12)):
        if (x, grot_vloer + 1, z) in hol and b.leeg(x, grot_vloer + 1, z):
            b.set(x, grot_vloer + 1, z, KRISTALLETJES)
    b.vat(22, grot_vloer + 1, 21)
    b.lamp(29, grot_vloer + 1, 17)
    b.bord(ex + 2, G + 1, ez - 1, ["De kristalader!", "Hak maar raak:", "hij groeit", "altijd weer aan."], rotation=12)

    # --- the track: rails from the buffer in the yard to the landing, the fallen rock on it ---
    puin = {(x, z): as_ for x, z, as_ in PUIN_PLEKKEN}
    for i, (x, z) in enumerate(cellen):
        if b.leeg(x, G, z):
            b.set(x, G, z, HOUTSKOOL)
        kanten = set()
        for n in (cellen[i - 1] if i > 0 else None, cellen[i + 1] if i + 1 < len(cellen) else None):
            if n is not None:
                kanten.add({(0, -1): "north", (0, 1): "south", (1, 0): "east", (-1, 0): "west"}[(n[0] - x, n[1] - z)])
        if kanten <= {"north", "south"}:
            vorm = "north_south"
        elif kanten <= {"east", "west"}:
            vorm = "east_west"
        else:
            vorm = next(k for k in ("north", "south") if k in kanten) + "_" + next(k for k in ("east", "west") if k in kanten)
        if (x, z) in puin:
            b.set(x, G + 1, z, PUIN, {"axis": puin[(x, z)]})
        else:
            b.set(x, G + 1, z, "rail", {"shape": vorm, "waterlogged": "false"})
    # the buffers at both ends
    bx, bz = SPOOR[0]
    b.set(bx, G + 1, bz + 1, STENEN)
    b.plaat(bx, G + 2, bz + 1, name=PLAAT)
    b.set(bx - 1, G + 1, bz + 1, MUUR, WALL)
    b.set(bx + 1, G + 1, bz + 1, MUUR, WALL)
    b.set(ex + 1, G + 1, ez, STENEN)
    b.lamp(ex + 1, G + 2, ez)
    # loose rock next to the track, where the roof came down
    for (x, z) in ((20, 29), (22, 28), (13, 26), (11, 23), (13, 22), (17, 28), (19, 26)):
        if (x, G + 1, z) in hol and b.leeg(x, G + 1, z):
            b.set(x, G + 1, z, HOUTSKOOL if (x + z) % 2 else ROOSTER)

    # --- the timbering: a frame every few blocks, a lamp under some of them ---
    def raam(x, z, langs_z):
        """A frame across the tunnel at (x, z): two posts against the walls and a beam."""
        for dx, dz in (((-1, 0), (1, 0)) if langs_z else ((0, -1), (0, 1))):
            b.paal(x + dx, z + dz, G + 1, G + 2, "spruce_fence", FENCE)
            b.set(x + dx, G + 3, z + dz, PLANK)
        b.plaat(x, G + 3, z, boven=True)
    for z in (31, 29):
        raam(21, z, True)
    for x in (19, 16, 13):
        raam(x, 27, False)
    for z in (25, 22, 19):
        raam(12, z, True)
    raam(15, 18, False)
    for x in (24, 27):
        raam(x, 27, False)
    for (x, z) in ((21, 30), (20, 27), (17, 27), (12, 26), (12, 23), (12, 20), (16, 18), (25, 27)):
        if (x, G + 3, z) in hol and b.leeg(x, G + 3, z):
            b.lamp(x, G + 3, z, hangend=True)
    # the side gallery: a store of barrels, crystals where the roof gave way
    b.vat(26, G + 1, 26, "guhs:chests/fossielmijn_mijn")
    b.vat(27, G + 1, 26)
    b.vat(26, G + 2, 26, facing="south")
    b.set(29, G + 1, 27, KRISTALLETJES)
    b.set(29, G + 1, 28, KRISTALBLOK)
    b.set(29, G + 2, 28, KRISTALLETJES)
    b.set(28, G + 1, 28, HOUTSKOOL)
    b.set(23, G + 1, 28, "spruce_fence", FENCE)               # a rack: two posts and a plank
    b.set(24, G + 1, 28, "spruce_fence", FENCE)
    b.set(23, G + 2, 28, "spruce_pressure_plate", {"powered": "false"})
    b.set(24, G + 2, 28, "spruce_pressure_plate", {"powered": "false"})

    # --- the mouth: a timber portal against the rock face, the name over it ---
    pz = mz + 1                                               # the portal stands one block out of the rock
    for dx in (-2, 2):
        b.paal(mx + dx, pz, G + 1, G + 4, STAM, {"axis": "y"})
        b.lamp(mx + dx, G + 6, pz)
    for x in range(mx - 3, mx + 4):
        b.set(x, G + 5, pz, STAM, {"axis": "x"})
    for dx in (-1, 1):                                        # the braces in the corners
        b.trap(mx + dx, G + 4, pz, "east" if dx < 0 else "west", om=True)
    b.plaat(mx, G + 4, pz, boven=True)
    for x in range(mx - 2, mx + 3):                           # a little roof over the name board
        b.plaat(x, G + 7, pz)
        b.plaat(x, G + 7, pz + 1)
    for x in (mx - 2, mx + 2):
        b.set(x, G + 6, pz + 1, "spruce_fence", FENCE)
        b.set(x, G + 5, pz + 1, "spruce_fence", FENCE)
    for x in (mx - 1, mx, mx + 1):
        b.set(x, G + 6, pz, PLANK)
    b.bord(mx, G + 6, pz + 1, ["Zoutkristalmijn", "Geen zout,", "geen smaak.", "Njeg!"], facing="south")
    for dx in (-4, 4):                                        # crystals flank the mouth
        if b.leeg(mx + dx, G + 1, pz) and not b.leeg(mx + dx, G, pz):
            b.set(mx + dx, G + 1, pz, KRISTALBLOK)
            b.set(mx + dx, G + 2, pz, KRISTALBLOK if dx < 0 else KRISTALLETJES)
            if dx < 0:
                b.set(mx + dx, G + 3, pz, KRISTALLETJES)

    # --- the yard: the Mijnwerker-guh's shed (east of the track) ---
    nx, ny, nz = MIJNWERKER
    b.npc = MIJNWERKER
    hx0, hx1, hz0, hz1 = nx - 1, nx + 4, nz - 2, nz + 1       # x 26..31, z 34..37: open to the west and the south
    for x in range(hx0, hx1 + 1):
        for z in range(hz0, hz1 + 1):
            if not b.leeg(x, G, z):
                b.set(x, G, z, PLANK)
    for x in range(hx0, hx1 + 1):                             # the back wall (north) with a window, and the east wall
        for y in range(G + 1, G + 4):
            b.set(x, y, hz0, STAM if x in (hx0, hx1) else PLANK, {"axis": "y"} if x in (hx0, hx1) else None)
    b.set(hx0 + 2, G + 2, hz0, "spruce_fence", FENCE)
    b.set(hx0 + 3, G + 2, hz0, "spruce_fence", FENCE)
    for z in range(hz0 + 1, hz1 + 1):
        for y in range(G + 1, G + 4):
            b.set(hx1, y, z, STAM if z == hz1 else PLANK, {"axis": "y"} if z == hz1 else None)
    b.paal(hx0, hz1, G + 1, G + 3, "spruce_fence", FENCE)     # the open corner rests on a post
    for x in range(hx0 - 1, hx1 + 2):                         # the roof: slabs, half a step lower at the front
        for z in range(hz0 - 1, hz1 + 2):
            b.plaat(x, G + 4, z, boven=z <= hz0 + 1)
    b.set(hx1 - 1, G + 1, hz0 + 1, "stonecutter", {"facing": "south"})
    b.vat(hx1 - 1, G + 1, hz1)
    b.vat(hx1 - 1, G + 2, hz1, facing="west")
    b.vat(hx1 - 2, G + 1, hz1)
    b.set(hx1 - 2, G + 1, hz0 + 1, "grindstone", {"face": "floor", "facing": "south"})
    b.set(hx0 + 1, G + 1, hz0 + 1, STAM, {"axis": "x"})       # his bench
    b.lamp(hx0 + 2, G + 3, hz0 + 2, hangend=True)
    b.set(hx0 + 2, G + 4, hz0 + 2, PLANK)
    b.bord(hx0 - 1, G + 1, hz1 + 1, ["Mijnwerker-guh", "Zout te koop", "(na het", "opruimen)"], rotation=2)
    # --- the yard, west of the track: the heap of salt, its barrels and the crane ---
    for x in range(W):
        for z in range(D):
            d = math.hypot(x - 14, (z - 36) * 1.15)
            if d < 3.4 and not b.leeg(x, G, z) and b.leeg(x, G + 1, z):
                for y in range(G + 1, G + 1 + max(1, int(round(3.2 * (1 - (d / 3.4) ** 1.5))))):
                    b.set(x, y, z, ZOUTHOOP if (x + y + z) % 5 else KALK)
    b.set(14, G + 4, 36, KRISTALLETJES)
    b.vat(17, G + 1, 38)
    b.vat(18, G + 1, 38)
    b.vat(17, G + 2, 38, facing="south")
    b.paal(18, 35, G + 1, G + 5, "spruce_fence", FENCE)       # the crane: a mast, an arm over the track, a chain and a lamp
    for x in range(17, 21):
        b.set(x, G + 5, 35, "spruce_fence", FENCE)
    b.set(20, G + 4, 35, "chain", {"axis": "y", "waterlogged": "false"})
    b.lamp(20, G + 3, 35, hangend=True)
    b.set(18, G + 1, 34, KRISTALBLOK)
    for (x, z) in ((9, 34), (33, 33), (15, 40), (27, 40)):
        if not b.leeg(x, G, z) and b.leeg(x, G + 1, z):
            b.lantaarnpaal(x, z, 2)
    b.midden(cx, cz)
    return b


# =====================================================================================================================
# the geometry self-check
# =====================================================================================================================
OP_DE_GROND = (KRISTALLETJES, SMEUL, "minecraft:rail", PUIN, "minecraft:barrel", "minecraft:campfire", ZOUTHOOP, "minecraft:spruce_sign",
               "minecraft:white_carpet", "minecraft:pink_carpet", REK, "minecraft:cartography_table", "minecraft:lectern", "minecraft:red_bed",
               "minecraft:grindstone", "minecraft:stonecutter", "minecraft:decorated_pot", "minecraft:flower_pot", "minecraft:powder_snow_cauldron",
               "minecraft:spruce_pressure_plate")
GEEN_VLOER = (None, AIR, KRISTALLETJES, SMEUL, "minecraft:rail", "minecraft:white_carpet", "minecraft:pink_carpet", "minecraft:spruce_pressure_plate")
ACHTER = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}


def check(b, naam):
    blocks = b.s.blocks
    problems = []

    def nm(c):
        v = blocks.get(c)
        return v[0] if v else None

    def vrij(c):
        return nm(c) in (None, AIR)

    vast = {c for c, v in blocks.items() if v[0] != AIR}
    # nothing floats: everything above the ground hangs together with the ground
    gezien = {c for c in vast if c[1] <= b.G}
    todo = list(gezien)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in vast and n not in gezien:
                gezien.add(n)
                todo.append(n)
    for c in sorted(vast - gezien):
        problems.append(f"{naam}: floating {nm(c)} at {c}")
    for c, v in blocks.items():
        onder = nm((c[0], c[1] - 1, c[2]))
        staat = v[0] in OP_DE_GROND or (v[0] == "minecraft:lantern" and v[1].get("hanging") == "false")
        if staat and onder in GEEN_VLOER:
            problems.append(f"{naam}: {v[0]} at {c} stands on {onder}")
        if v[0] == "minecraft:lantern" and v[1].get("hanging") == "true" and vrij((c[0], c[1] + 1, c[2])):
            problems.append(f"{naam}: a hanging lantern at {c} hangs from nothing")
        if v[0] in ("minecraft:spruce_wall_sign", "minecraft:ladder"):
            dx, dz = ACHTER[v[1]["facing"]]
            if vrij((c[0] + dx, c[1], c[2] + dz)):
                problems.append(f"{naam}: the {v[0]} at {c} has no wall behind it")
    # the NPC sits on something, with room for its head
    x, y, z = b.npc
    if vrij((x, y - 1, z)) or not vrij((x, y, z)) or not vrij((x, y + 1, z)):
        problems.append(f"{naam}: the NPC has no place to sit at {b.npc}")
    if not b.vaten:
        problems.append(f"{naam}: no loot barrel")
    if sum(1 for v in blocks.values() if v[0] == "minecraft:jigsaw" and v[2] and v[2].get("name") == f"guhs:{naam}_midden") != 1:
        problems.append(f"{naam}: exactly one centre jigsaw")
    if any(c[1] != 0 for c, v in blocks.items() if v[0] == "minecraft:jigsaw"):
        problems.append(f"{naam}: the centre jigsaw belongs in layer 0")

    def tel(name):
        return sum(1 for v in blocks.values() if v[0] == name)
    if naam == OPGRAVING:
        if tel(BOTTENZAND) < 8:
            problems.append(f"{naam}: {tel(BOTTENZAND)} bottenzand (a player needs 5 different spots; 8 or more so nobody waits)")
        if tel(REK) != 1:
            problems.append(f"{naam}: exactly one skeleton stand")
        for c, v in blocks.items():
            if v[0] == BOTTENZAND:
                if not vrij((c[0], c[1] + 1, c[2])) or not vrij((c[0], c[1] + 2, c[2])):
                    problems.append(f"{naam}: bottenzand at {c} is covered by {nm((c[0], c[1] + 1, c[2]))} / {nm((c[0], c[1] + 2, c[2]))}")
            if v[0] == REK:
                # the little skeleton is three blocks long (along its facing) and nearly two high
                for dx in (-1, 0, 1):
                    for dy in (0, 1):
                        if (dx or dy) and not vrij((c[0] + dx, c[1] + dy, c[2])):
                            problems.append(f"{naam}: no room for the skeleton at {(c[0] + dx, c[1] + dy, c[2])}")
    else:
        if tel(PUIN) < 6:
            problems.append(f"{naam}: {tel(PUIN)} puin (a player clears 5)")
        if tel(ADER) < 12:
            problems.append(f"{naam}: only {tel(ADER)} vein blocks")
        # the track is whole: every cell is a rail or puin, on a floor, with two free blocks over it
        for (x, z) in spoorcellen():
            if nm((x, b.G + 1, z)) not in ("minecraft:rail", PUIN):
                problems.append(f"{naam}: the track is broken at {(x, z)}: {nm((x, b.G + 1, z))}")
            if vrij((x, b.G, z)):
                problems.append(f"{naam}: no floor under the track at {(x, z)}")
            if not vrij((x, b.G + 2, z)):
                problems.append(f"{naam}: the track is blocked at {(x, b.G + 2, z)} by {nm((x, b.G + 2, z))}")
        # every vein block can be reached: a free block next to it
        for c, v in blocks.items():
            if v[0] == ADER and not any(vrij((c[0] + dx, c[1], c[2] + dz)) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                problems.append(f"{naam}: the vein block at {c} can't be reached")
        # the mountain is closed: nothing hollow touches the outside except through the mouth
        for (x, y, z) in b.hol:
            if y <= b.G or (z >= MOND[1] and abs(x - MOND[0]) <= 1):
                continue
            for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, 0, 1), (0, 0, -1)):
                n = (x + d[0], y + d[1], z + d[2])
                if n not in b.hol and vrij(n):
                    problems.append(f"{naam}: the mountain is open at {n}")
    return problems


def test_kamer(h):
    """FossielmijnGameTests: a floor of houtskoolsteen (helper y 1), 16 x 16, eight high."""
    kamer = h.Structure((16, 8, 16))
    for x in range(16):
        for z in range(16):
            kamer.set(x, 0, z, HOUTSKOOL)
    kamer.save("fossielmijn_test_kamer")


def build_all(h):
    """Builds, checks and saves both templates (with their NPCs) and the test room; returns (problems, {name: Bouw})."""
    from features import wereld
    bouwsels = {OPGRAVING: opgraving(h), MIJN: mijn(h)}
    problems = []
    for naam, b in bouwsels.items():
        problems += check(b, naam)
    x, y, z = ARCHEOLOOG
    bouwsels[OPGRAVING].s.entity(x + 0.5, y, z + 0.5, wereld.npc(h, "archeoloogguh", "fossielmijn_archeoloog", yaw=ARCHEOLOOG_YAW))
    x, y, z = MIJNWERKER
    bouwsels[MIJN].s.entity(x + 0.5, y, z + 0.5, wereld.npc(h, "mijnwerkerguh", "fossielmijn_mijnwerker", yaw=MIJNWERKER_YAW))
    if not problems:
        for naam, b in bouwsels.items():
            b.s.save(naam)
        test_kamer(h)
    return problems, bouwsels
