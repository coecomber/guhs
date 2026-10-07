"""
Het Snuffeleiland, the DOCK (DESIGN_VERHALENPAD C "Getting there"): the template of the steigerhuisje and the floor of
the game tests. Java: feature/snuffelsteiger (Steiger.java names the same spots: snuffel_steiger.selfcheck compares them).

The steigerhuisje stands on the shore of a Diepe Guhzee, with its pier out over the water (template +z = out to sea; the
structure type guhs:kust_steiger turns it that way):

  z  0..13   the plot, a little quay of stone bricks (layer G = world y 64, DEK_Y) with its own foundation under it:
             the cottage of the dog family (white walls, a red tiled roof, a chimney that smokes), its porch, the yard with
             the lantern feast (a pergola of lampions over a long table), a garden wall with a gate inland;
  z 14..32   the pier on piles: lantern arches for the feast, the platform at its end where Kapitein Zoutsnoet stands,
             a ship's bell, a ladder out of the water. His boat (an entity: SteigerBoot) lies moored on the east side.

Inside the cottage: the sickbed (a pink bed) with a nightstand and flowers, papa's bed with his striped scarf on the
wall over it and his map table, the hearth behind a fire screen, a table, a rug.

The land behind a shore is seldom as low as a quay. So the plot's outer ring (its back and its two sides) is written
with MARKER blocks, and the start pool's rule processor (snuffel_steiger.processor) decides per block when the dock is
placed: where the land is higher than the plot, the marker becomes a retaining wall of stone bricks, exactly as high as
the land behind it; where there is only air, it becomes what stands there on flat land: the garden wall, the gate, a
lantern, or nothing. steigerhuisje(h, voorbeeld=True) gives the flat-land outcome (for pictures).

Nothing in this template is a container with loot and nothing can be used up: lanterns, candles, flowers.
"""
import random

G = 30                       # template y of the ground / deck blocks: the piles under the pier go down to the sea's floor (y 34)
DEK_Y = 64                   # ...which is this world y (the sea's top water block is y 62)
SX, SY, SZ = 23, G + 11, 34
ANKER = (10, G, 14)          # the first plank of the pier: the first column over the water, on the pier's axis
VOET = (ANKER[0], G + 1, ANKER[2])   # where you stand on it: the anchor block of the cutscenes

# the plot and the house
PLOT_Z = 13                  # the quay's last row (land); from PLOT_Z + 1 on: the sea
HUIS = (3, 2, 11, 9)         # x0, z0, x1, z1 of its walls
DEUR = (7, 9)
# the pier
PIER_X = (9, 11)
PIER_Z = (14, 30)
PLATFORM = (4, 27, 12, 32)   # x0, z0, x1, z1 (west of the pier and at its end: the east side is the boat's water)
# where everybody is (feet; x, y, z in template coordinates, yaw as in the unrotated template)
BED_HOOFD, BED_VOET = (4, G + 1, 5), (4, G + 1, 6)
PLEK_PUP = ((4.5, G + 1.5625, 5.85), 180.0)
PLEK_BUUR = ((5.5, G + 1, 6.5), 90.0)
PLEK_KAPITEIN = ((11.5, G + 1, 29.5), 170.0)
PLEK_BOOT = ((14.6, G - 1.45, 28.5), 0.0)
BEL = (8, G + 2, 32)
BORD = (15, G + 1, 2)

AIR = "minecraft:air"
# the markers of the plot's outer ring: block -> (what it is where the land is no higher: (name, props) or None for air,
#                                                 what it is where the land stands in its place)
M_OP, M_OP_MOS, M_MUUR, M_MUUR_MOS, M_HEK, M_LANTAARN = ("minecraft:white_stained_glass", "minecraft:light_gray_stained_glass",
                                                         "minecraft:yellow_stained_glass", "minecraft:lime_stained_glass",
                                                         "minecraft:orange_stained_glass", "minecraft:red_stained_glass")
MARKERS = {
    M_OP: (None, "minecraft:stone_bricks"),
    M_OP_MOS: (None, "minecraft:mossy_stone_bricks"),
    M_MUUR: (("minecraft:cobblestone_wall", {}), "minecraft:stone_bricks"),
    M_MUUR_MOS: (("minecraft:mossy_cobblestone_wall", {}), "minecraft:mossy_stone_bricks"),
    M_HEK: (("minecraft:spruce_fence_gate", {"facing": "south", "open": "false", "in_wall": "true", "powered": "false"}), "minecraft:stone_bricks"),
    M_LANTAARN: (("minecraft:lantern", {"hanging": "false", "waterlogged": "false"}), "minecraft:stone_bricks"),
}
KEERMUUR = 7                 # the retaining wall can stand this many blocks over the plot (snuffel_steiger.LAND allows no higher land)
FUNDERING = 9                # the plot's own foundation goes this deep (snuffel_steiger.LAND allows no deeper hole)
FENCES = ("spruce_fence", "dark_oak_fence")
WALLS = ("cobblestone_wall", "mossy_cobblestone_wall")
PANES = ("glass_pane",)


def mc(n):
    return n if ":" in n else "minecraft:" + n


class Bouw:
    def __init__(self, h, size):
        self.h = h
        self.s = h.Structure(size)

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None, only_empty=False):
        self.s.fill(x0, y0, z0, x1, y1, z1, mc(name), props, only_empty=only_empty)

    def trap(self, x, y, z, name, facing, half="bottom"):
        """Stairs; facing = the side of the high part."""
        self.set(x, y, z, name, {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"})

    def lantaarn(self, x, y, z, hangend=False):
        self.set(x, y, z, "lantern", {"hanging": "true" if hangend else "false", "waterlogged": "false"})

    def lampion(self, x, y, z, kleur, hangend=True):
        self.set(x, y, z, f"guhs:lampion_{kleur}", {"hanging": "true" if hangend else "false", "waterlogged": "false"})

    def verbind(self):
        """Fences, walls and panes join their neighbours (the game does this when it places the template; written out here
        so the pictures of the renderer show the same)."""
        blocks = self.s.blocks

        def naam(p):
            b = blocks.get(p)
            return b[0].split(":")[1] if b else None

        def vast(n):
            return n is not None and n != "air" and not any(t in n for t in (
                "fence", "wall", "pane", "lantern", "lampion", "carpet", "candle", "potted", "door", "sign", "banner", "bed", "leaves", "slab",
                "stairs", "ladder", "bell", "campfire", "bars", "torch", "tulip", "allium", "dandelion", "poppy", "bluet", "grass", "azalea",
                "cauldron", "button", "trapdoor", "plate")) or n in ("grass_block",)

        for p, (name, props, nbt) in list(blocks.items()):
            kort = name.split(":")[1]
            soort = "fence" if kort in FENCES else "wall" if kort in WALLS else "pane" if kort in PANES or kort == "iron_bars" else None
            if soort is None:
                continue
            x, y, z = p
            nieuw = dict(props)
            for kant, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("west", (-1, 0)), ("east", (1, 0))):
                n = naam((x + dx, y, z + dz))
                zelfde = n is not None and ((soort == "fence" and n in FENCES) or (soort == "wall" and (n in WALLS or n.endswith("fence_gate")))
                                            or (soort == "pane" and (n in PANES or n == "iron_bars")))
                if soort == "fence" and n is not None and n.endswith("fence_gate"):
                    zelfde = True
                aan = zelfde or vast(n)
                if soort == "wall":
                    nieuw[kant] = "low" if aan else "none"
                else:
                    nieuw[kant] = "true" if aan else "false"
            if soort == "wall":
                recht = (nieuw["north"] != "none" and nieuw["south"] != "none" and nieuw["east"] == "none" and nieuw["west"] == "none") or \
                        (nieuw["east"] != "none" and nieuw["west"] != "none" and nieuw["north"] == "none" and nieuw["south"] == "none")
                boven = naam((x, y + 1, z))
                nieuw["up"] = "false" if recht and boven in (None, "air") else "true"
            nieuw.setdefault("waterlogged", "false")
            blocks[p] = (name, nieuw, nbt)


def _bord(b, x, y, z, regels, rotation=0):
    import sign_text
    B = b.h.Byte
    prefix = "sign.guhs.snuffelsteiger"
    nbt = {"id": "minecraft:sign", "is_waxed": B(1),
           "front_text": {"messages": sign_text.messages(prefix, regels), "color": "black", "has_glowing_text": B(0)},
           "back_text": {"messages": sign_text.messages(prefix, ["", "", "", ""]), "color": "black", "has_glowing_text": B(0)}}
    nbt["back_text"] = dict(nbt["front_text"])
    b.set(x, y, z, "spruce_sign", {"rotation": str(rotation), "waterlogged": "false"}, nbt)


def steigerhuisje(h, voorbeeld=False):
    """-> (Structure, problems). voorbeeld: the markers of the outer ring as they turn out on flat land (for pictures)."""
    b = Bouw(h, (SX, SY, SZ))
    rng = random.Random(2130_5201)
    x0, z0, x1, z1 = HUIS

    # ------------------------------------------------------------------------------------------------------------------
    # the plot: a quay with its own foundation (so it never floats and never drowns), stone on the outside
    # ------------------------------------------------------------------------------------------------------------------
    for x in range(SX):
        for z in range(PLOT_Z + 1):
            rand = x in (0, SX - 1) or z in (0, PLOT_Z)
            for y in range(G - FUNDERING, G):
                if rand:
                    r = rng.random()
                    b.set(x, y, z, "mossy_stone_bricks" if r < 0.22 else "cracked_stone_bricks" if r < 0.3 else "stone_bricks")
                else:
                    b.set(x, y, z, "dirt" if y >= G - 3 else "stone")
            # the top layer: the quay strip of stone bricks along the water, grass behind it
            if z >= 11 or rand:
                r = rng.random()
                b.set(x, G, z, "mossy_stone_bricks" if r < 0.12 else "stone_bricks")
            else:
                b.set(x, G, z, "grass_block", {"snowy": "false"})
    # the path from the gate in the back wall to the quay (gravelly: coarse dirt and path), two wide
    for z in range(1, 11):
        for x in (16, 17):
            b.set(x, G, z, "dirt_path" if (x + z) % 5 else "coarse_dirt")
    # a little branch of the path to the porch
    for x in range(12, 16):
        b.set(x, G, 10, "dirt_path")

    # ------------------------------------------------------------------------------------------------------------------
    # the outer ring (the back and the two sides): markers. On flat land: the garden wall with its gate and corner pillars
    # with lanterns; against higher land: a retaining wall as high as that land (see the module text)
    # ------------------------------------------------------------------------------------------------------------------
    pilaren = {(0, 0), (SX - 1, 0), (15, 0), (18, 0), (0, 10), (SX - 1, 10)}
    for x in range(SX):
        for z in range(PLOT_Z + 1):
            if not (z == 0 or x in (0, SX - 1)):
                continue
            mos = rng.random() < 0.25
            if z <= 10:
                if (x, z) in pilaren:
                    b.set(x, G + 1, z, "stone_bricks")
                    b.set(x, G + 2, z, M_LANTAARN)
                elif z == 0 and x in (16, 17):
                    b.set(x, G + 1, z, M_HEK)
                    b.set(x, G + 2, z, M_OP)
                else:
                    b.set(x, G + 1, z, M_MUUR_MOS if rng.random() < 0.3 else M_MUUR)
                    b.set(x, G + 2, z, M_OP_MOS if mos else M_OP)
            else:
                # (beside the quay strip: open on flat land)
                b.set(x, G + 1, z, M_OP)
                b.set(x, G + 2, z, M_OP)
            for y in range(G + 3, G + KEERMUUR + 1):
                b.set(x, y, z, M_OP_MOS if rng.random() < 0.2 else M_OP)
    # bollards on the quay's edge
    for x in (4, 14, 18):
        b.set(x, G + 1, PLOT_Z, "dark_oak_fence")

    # ------------------------------------------------------------------------------------------------------------------
    # the cottage
    # ------------------------------------------------------------------------------------------------------------------
    b.fill(x0, G, z0, x1, G, z1, "spruce_planks")
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                hoek = x in (x0, x1) and z in (z0, z1)
                for y in range(G + 1, G + 4):
                    if hoek:
                        b.set(x, y, z, "stripped_spruce_log", {"axis": "y"})
                    elif y == G + 1:
                        b.set(x, y, z, "stone_bricks")
                    else:
                        b.set(x, y, z, "calcite")
    # the gables (the roof's ridge runs along x): white up to the tiles, a little window in the east one
    for x in (x0, x1):
        for k, (za, zb) in enumerate(((3, 8), (4, 7), (5, 6))):
            b.fill(x, G + 4 + k, za, x, G + 4 + k, zb, "calcite")
    b.set(x1, G + 5, 5, "glass_pane")
    b.set(x1, G + 5, 6, "glass_pane")
    # the roof of red tiles, one block over the walls on every side
    for k in range(4):
        zn, zs, y = z0 - 1 + k, z1 + 1 - k, G + 3 + k
        for x in range(x0 - 1, x1 + 2):
            b.trap(x, y, zn, "brick_stairs", "south")
            b.trap(x, y, zs, "brick_stairs", "north")
    for x in range(x0 - 1, x1 + 2):
        b.set(x, G + 7, 5, "brick_slab", {"type": "bottom", "waterlogged": "false"})
        b.set(x, G + 7, 6, "brick_slab", {"type": "bottom", "waterlogged": "false"})
    # a beam under the ridge with the lamp of the room
    b.fill(x0 + 1, G + 4, 5, x1 - 1, G + 4, 5, "stripped_spruce_log", {"axis": "x"})
    b.lantaarn(7, G + 3, 5, hangend=True)
    # the door (south, towards the water) and the windows with their shutters
    dx, dz = DEUR
    b.set(dx, G + 1, dz, "spruce_door", {"facing": "north", "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
    b.set(dx, G + 2, dz, "spruce_door", {"facing": "north", "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    for wx in (5, 9):
        b.set(wx, G + 2, z1, "glass_pane")
    for wz in (5, 6):
        b.set(x1, G + 2, wz, "glass_pane")
    b.set(x0, G + 2, 7, "glass_pane")
    for wx in (4, 6, 8, 10):                                    # shutters beside the two front windows
        b.set(wx, G + 2, z1 + 1, "warped_trapdoor", {"facing": "south", "half": "bottom", "open": "true", "powered": "false", "waterlogged": "false"})
    for wz in (4, 7):                                           # ...and beside the yard window
        b.set(x1 + 1, G + 2, wz, "warped_trapdoor", {"facing": "east", "half": "bottom", "open": "true", "powered": "false", "waterlogged": "false"})

    # the hearth in the north wall: bricks, the fire behind a screen of bars, the chimney up through the roof
    for x in (6, 7, 8):
        for y in range(G + 1, G + 4):
            b.set(x, y, z0, "bricks")
    b.set(6, G + 1, z0 + 1, "bricks")
    b.set(8, G + 1, z0 + 1, "bricks")
    b.set(6, G + 2, z0 + 1, "brick_slab", {"type": "bottom", "waterlogged": "false"})
    b.set(8, G + 2, z0 + 1, "brick_slab", {"type": "bottom", "waterlogged": "false"})
    b.set(7, G + 2, z0 + 1, "bricks")
    b.set(7, G + 1, z0 + 1, "campfire", {"lit": "true", "facing": "south", "signal_fire": "false", "waterlogged": "false"})
    b.set(7, G + 1, z0 + 2, "iron_bars")
    for y in range(G + 4, G + 9):
        b.set(7, y, z0, "bricks")
    b.set(7, G + 9, z0, "campfire", {"lit": "true", "facing": "south", "signal_fire": "false", "waterlogged": "false"})

    # the sickbed against the west wall, a nightstand with flowers at its head, a candle
    b.set(*BED_HOOFD, "pink_bed", {"facing": "north", "part": "head", "occupied": "false"}, {"id": "minecraft:bed"})
    b.set(*BED_VOET, "pink_bed", {"facing": "north", "part": "foot", "occupied": "false"}, {"id": "minecraft:bed"})
    b.set(4, G + 1, 4, "spruce_planks")
    b.set(4, G + 2, 4, "potted_pink_tulip")
    b.set(4, G + 1, 3, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
    b.set(4, G + 2, 3, "candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    # papa's bed in the north-east corner, his striped scarf on the wall over it, his map table next to it
    b.set(10, G + 1, 3, "blue_bed", {"facing": "north", "part": "head", "occupied": "false"}, {"id": "minecraft:bed"})
    b.set(10, G + 1, 4, "blue_bed", {"facing": "north", "part": "foot", "occupied": "false"}, {"id": "minecraft:bed"})
    from make_structures import NbtList
    b.set(10, G + 3, 3, "blue_wall_banner", {"facing": "south"},
          {"id": "minecraft:banner", "patterns": NbtList(10, [{"color": "white", "pattern": "minecraft:small_stripes"}])})
    b.set(9, G + 1, 3, "cartography_table")
    # the table with two stools, the kitchen corner, books, a rug
    for x in (8, 9):
        b.set(x, G + 1, 7, "spruce_slab", {"type": "top", "waterlogged": "false"})
    b.set(8, G + 2, 7, "candle", {"candles": "1", "lit": "true", "waterlogged": "false"})
    b.set(9, G + 2, 7, "potted_allium")
    b.trap(8, G + 1, 8, "spruce_stairs", "south")
    b.set(10, G + 1, 8, "water_cauldron", {"level": "3"})
    b.set(10, G + 1, 7, "smoker", {"facing": "west", "lit": "false"}, {"id": "minecraft:smoker"})
    b.set(4, G + 1, 8, "bookshelf")
    b.set(4, G + 2, 8, "bookshelf")
    b.set(5, G + 1, 8, "potted_fern")
    for x, z, kleur in ((6, 5, "white"), (7, 5, "pink"), (8, 5, "white"), (6, 6, "pink"), (7, 6, "white"), (8, 6, "pink")):
        b.set(x, G + 1, z, f"{kleur}_carpet")

    # the porch: planks, two posts, the roof's edge carried on, lanterns, a bench, bushes under the windows
    b.fill(x0, G, z1 + 1, x1, G, z1 + 2, "spruce_planks")
    for x in (x0, x1):
        b.fill(x, G + 1, z1 + 2, x, G + 2, z1 + 2, "spruce_fence")
    for x in range(x0 - 1, x1 + 2):
        b.set(x, G + 3, z1 + 2, "brick_slab", {"type": "bottom", "waterlogged": "false"})
    b.lantaarn(x0 + 1, G + 2, z1 + 2, hangend=True)
    b.lantaarn(x1 - 1, G + 2, z1 + 2, hangend=True)
    b.trap(9, G + 1, z1 + 1, "spruce_stairs", "north")
    b.trap(10, G + 1, z1 + 1, "spruce_stairs", "north")
    b.set(5, G + 1, z1 + 1, "flowering_azalea_leaves", {"persistent": "true", "distance": "7", "waterlogged": "false"})

    # ------------------------------------------------------------------------------------------------------------------
    # the garden: bushes west of the house, a cherry tree in the yard's corner, flowers
    # ------------------------------------------------------------------------------------------------------------------
    blad = {"persistent": "true", "distance": "7", "waterlogged": "false"}
    for x, z, soort in ((1, 3, "azalea_leaves"), (1, 4, "flowering_azalea_leaves"), (1, 8, "flowering_azalea_leaves"), (1, 9, "azalea_leaves")):
        b.set(x, G + 1, z, soort, blad)
    tx, tz = 20, 3
    for y in range(G + 1, G + 5):
        b.set(tx, y, tz, "cherry_log", {"axis": "y"})
    for ddx in range(-2, 3):
        for ddz in range(-2, 3):
            for ddy in range(0, 3):
                if abs(ddx) + abs(ddz) + ddy * 1.4 <= 3.3 and not (ddx == 0 and ddz == 0 and ddy == 0):
                    p = (tx + ddx, G + 4 + ddy, tz + ddz)
                    if 1 <= p[0] < SX - 1 and p[2] >= 1 and b.get(*p) is None:
                        b.set(*p, "cherry_leaves", blad)

    # ------------------------------------------------------------------------------------------------------------------
    # the lantern feast in the yard: a pergola of lampions over a long table with candles and benches
    # ------------------------------------------------------------------------------------------------------------------
    for px in (13, 19):
        for pz in (4, 8):
            b.fill(px, G + 1, pz, px, G + 3, pz, "spruce_fence")
    for pz in (4, 8):
        b.fill(13, G + 4, pz, 19, G + 4, pz, "spruce_fence")
    for px in (13, 19):
        b.fill(px, G + 4, 4, px, G + 4, 8, "spruce_fence")
    for x, z, kleur in ((14, 4, "roze"), (16, 4, "geel"), (18, 4, "mint"), (14, 8, "mint"), (16, 8, "roze"), (18, 8, "geel"),
                        (13, 6, "geel"), (19, 6, "roze")):
        b.lampion(x, G + 3, z, kleur)
    for x in (14, 15):
        b.set(x, G, 6, "spruce_planks")
        b.set(x, G + 1, 6, "spruce_slab", {"type": "top", "waterlogged": "false"})
    b.set(14, G + 2, 6, "candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    b.set(15, G + 2, 6, "potted_dandelion")
    for x in (14, 15):
        b.trap(x, G + 1, 5, "spruce_stairs", "north")
        b.trap(x, G + 1, 7, "spruce_stairs", "south")
    _bord(b, *BORD, ["Vanavond:", "Lantaarnfeest", "op de steiger", "Njeg!"], rotation=0)
    # barrels and a crate on the quay, a lantern post where the pier begins
    b.set(19, G + 1, 12, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
    b.set(19, G + 2, 12, "barrel", {"facing": "east", "open": "false"}, {"id": "minecraft:barrel"})
    b.set(21, G + 1, 12, "composter", {"level": "0"})
    b.lampion(21, G + 2, 12, "geel", hangend=False)
    b.set(20, G + 1, 12, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
    b.lampion(20, G + 2, 12, "mint", hangend=False)
    b.lampion(2, G + 1, 12, "roze", hangend=False)

    # flowers and tufts on the grass
    planten = ["short_grass"] * 5 + ["pink_tulip", "allium", "dandelion", "azure_bluet", "poppy"]
    for x in range(1, SX - 1):
        for z in range(1, 11):
            if b.get(x, G, z) == "minecraft:grass_block" and b.get(x, G + 1, z) is None and rng.random() < 0.2 and not (12 <= x <= 19 and 4 <= z <= 9):
                b.set(x, G + 1, z, rng.choice(planten))

    # ------------------------------------------------------------------------------------------------------------------
    # the pier: planks on piles, lantern arches for the feast, the platform at its end
    # ------------------------------------------------------------------------------------------------------------------
    px0, px1 = PIER_X
    pz0, pz1 = PIER_Z
    b.fill(px0, G, pz0, px1, G, pz1, "spruce_planks")
    for z in range(pz0, pz1 + 1):
        b.set(px0 + 1, G, z, "stripped_spruce_log", {"axis": "z"})       # a darker runner down the middle
    qx0, qz0, qx1, qz1 = PLATFORM
    b.fill(qx0, G, qz0, qx1, G, qz1, "spruce_planks")
    for x in range(qx0, qx1 + 1):
        for z in (qz0, qz1):
            b.set(x, G, z, "stripped_spruce_log", {"axis": "x"})
    # piles beside the pier (outside the planks), each one block over the deck; every other pair carries an arch
    for i, z in enumerate((15, 18, 21, 24)):
        for x in (px0 - 1, px1 + 1):
            b.fill(x, 0, z, x, G + 1, z, "spruce_log", {"axis": "y"})
        if i % 2 == 0:
            for x in (px0 - 1, px1 + 1):
                b.fill(x, G + 2, z, x, G + 3, z, "spruce_fence")
            b.fill(px0 - 1, G + 4, z, px1 + 1, G + 4, z, "spruce_fence")
            b.lampion(px0, G + 3, z, "roze" if i == 0 else "mint")
            b.lampion(px1, G + 3, z, "geel" if i == 0 else "roze")
        else:
            b.lantaarn(px0 - 1, G + 2, z)
            b.lantaarn(px1 + 1, G + 2, z)
    # piles under the platform (its corners and the middle of its sides), the corner ones stand up with a lantern
    for x, z in ((qx0, qz0), (qx1, qz0), (qx0, qz1), (qx1, qz1)):
        b.fill(x, 0, z, x, G + 1, z, "spruce_log", {"axis": "y"})
        b.lantaarn(x, G + 2, z)
    for x, z in ((qx0, 30), (qx1, 30), (BEL[0], qz1)):
        b.fill(x, 0, z, x, G - 1, z, "spruce_log", {"axis": "y"})
    # the ship's bell on its post at the far end
    b.set(BEL[0], G + 1, qz1, "spruce_log", {"axis": "y"})
    b.set(*BEL, "bell", {"attachment": "floor", "facing": "north", "powered": "false"}, {"id": "minecraft:bell"})
    # cargo on the platform, lampions
    b.set(5, G + 1, 28, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
    b.set(6, G + 1, 28, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
    b.set(5, G + 2, 28, "barrel", {"facing": "south", "open": "false"}, {"id": "minecraft:barrel"})
    b.lampion(6, G + 2, 28, "roze", hangend=False)
    b.set(5, G + 1, 31, "composter", {"level": "0"})
    b.lampion(6, G + 1, 31, "geel", hangend=False)
    # a ladder out of the water on the west side of the platform
    for y in range(G - 3, G + 1):
        b.set(qx0 - 1, y, 30, "ladder", {"facing": "west", "waterlogged": "true" if y <= G - 2 else "false"})

    # ------------------------------------------------------------------------------------------------------------------
    # air over the plot (a hill is cut away instead of burying the cottage) and over the pier (never water: y > sea level)
    # ------------------------------------------------------------------------------------------------------------------
    for x in range(SX):
        for z in range(PLOT_Z + 1):
            if z == 0 or x in (0, SX - 1):
                continue                      # (the outer ring: its markers decide; above them the land stays as it is)
            b.fill(x, G + 1, z, x, SY - 1, z, AIR, only_empty=True)
    for z in range(PLOT_Z + 1, SZ):
        for x in range(qx0 - 1, 18):
            b.fill(x, G + 1, z, x, G + 4, z, AIR, only_empty=True)
    if voorbeeld:
        for p, (name, props, nbt) in list(b.s.blocks.items()):
            if name in MARKERS:
                vlak = MARKERS[name][0]
                b.s.blocks[p] = (AIR, {}, None) if vlak is None else (vlak[0], dict(vlak[1]), None)
    b.verbind()
    return b.s, controleer(b.s)


LOOPBAAR = ("minecraft:spruce_planks", "minecraft:stripped_spruce_log", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
            "minecraft:grass_block", "minecraft:dirt_path", "minecraft:coarse_dirt")


def controleer(s):
    """Everybody stands on something with room over their head; the way from the gate through the yard, over the quay and
    down the pier to the captain is open; the door can be walked through; nothing floats."""
    problems = []

    def vrij(x, y, z):
        return s.get(x, y, z) in (None, AIR) or s.get(x, y, z).split(":")[1] in ("short_grass", "pink_tulip", "allium", "dandelion", "azure_bluet", "poppy") \
            or s.get(x, y, z).endswith("_carpet")

    def staat(naam, plek):
        bx, by, bz = int(plek[0] // 1), int(plek[1] // 1), int(plek[2] // 1)
        if s.get(bx, by - 1, bz) not in LOOPBAAR:
            problems.append(f"{naam} at {plek} stands on {s.get(bx, by - 1, bz)}")
        for dy in (0, 1):
            if not vrij(bx, by + dy, bz):
                problems.append(f"{naam} at {plek}: {s.get(bx, by + dy, bz)} in the way")

    staat("buurvrouw", PLEK_BUUR[0])
    staat("kapitein", PLEK_KAPITEIN[0])
    if s.get(*BED_HOOFD) != "minecraft:pink_bed" or s.get(*BED_VOET) != "minecraft:pink_bed":
        problems.append("the sickbed is not where the puppy lies")
    # a walk: gate -> path -> quay -> pier -> platform, and quay -> porch -> door -> the bedside
    route = [(16, 1), (16, 5), (16, 10), (16, 12), (10, 12), (10, 14), (10, 20), (10, 26), (10, 29), (11, 29)]
    route2 = [(10, 12), (7, 12), (7, 10), (7, 9), (7, 8), (5, 7), (5, 6)]
    for pad in (route, route2):
        for (ax, az), (bx, bz) in zip(pad, pad[1:]):
            n = max(abs(bx - ax), abs(bz - az))
            for i in range(n + 1):
                x, z = ax + (bx - ax) * i // max(n, 1), az + (bz - az) * i // max(n, 1)
                if s.get(x, G, z) not in LOOPBAAR:
                    problems.append(f"the walk has no floor at {(x, z)}: {s.get(x, G, z)}")
                for y in (G + 1, G + 2):
                    blok = s.get(x, y, z)
                    if not vrij(x, y, z) and not (blok or "").endswith("_door") and not (blok or "").endswith("fence_gate") and blok not in (M_HEK, M_OP):
                        problems.append(f"the walk is blocked at {(x, y, z)} by {blok}")
    # the water under the pier stays water: nothing but piles and the ladder below the deck there
    for (x, y, z), (name, _, _) in s.blocks.items():
        if z > PLOT_Z and y < G and name not in ("minecraft:spruce_log", "minecraft:ladder"):
            problems.append(f"{name} under the pier at {(x, y, z)}")
        if z > PLOT_Z and y == G and name not in ("minecraft:spruce_planks", "minecraft:stripped_spruce_log", "minecraft:spruce_log", "minecraft:ladder"):
            problems.append(f"{name} in the deck layer at {(x, y, z)}")
    # where the boat lies and where it sails: open (no pile, no plank)
    bx, by, bz = PLEK_BOOT[0]
    for x in range(int(bx) - 1, int(bx) + 2):
        for z in range(int(bz) - 3, SZ):
            for y in range(G - 3, G + 4):
                if s.get(x, y, z) not in (None, AIR):
                    problems.append(f"{s.get(x, y, z)} in the boat's water at {(x, y, z)}")
    if s.get(*ANKER) not in ("minecraft:spruce_planks", "minecraft:stripped_spruce_log"):
        problems.append("the anchor is not a plank of the pier")
    return problems


# --- the game tests' floor: as big as the dock's footprint (a rim of one block around it), stone where the quay is, planks
# where the pier is; the tests stand a copy on it by hand (its box and its turn only: SnuffelsteigerGameTests) ---------------------
TEST_MAAT = (SX + 2, 6, SZ + 2)


def test_vloer(h):
    t = h.Structure(TEST_MAAT)
    for x in range(TEST_MAAT[0]):
        for z in range(TEST_MAAT[2]):
            t.set(x, 0, z, "minecraft:stone_bricks" if z <= PLOT_Z + 1 else "minecraft:spruce_planks")
    return t
