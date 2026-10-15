"""
bbq2 (ring-sausuman) - the building: de Toren van Sausuman (structure guhs:sausuman_toren). One template on a cave floor of the
Guhbarbecuether (type guhs:barbecueput through wereld.bbq_structuur), x to the east, z to the south, ground layer G.

A slim black tower of polished blackstone: an eight-sided shaft (13 across below, 11 above a sloped shoulder) with a
basalt buttress on each of its four diagonals. Above the roof platform the buttresses go on as four horns that lean
outwards and end in a spike, with a smoking brazier between them. Copper pipes climb the walls and end in sputterpijpen
(block guhs:ringsausuman_sputterpijp: they puff). Inside, from the door in the south:
  - the machinehal (6 high): against the north wall DE RINGENBAKKER (block guhs:ringsausuman_ringenbakker in a machine of
    copper and basalt with a tray in front of it), Sausuman next to it with his Mokhoek behind him, and along the west
    wall five REAL vadskracht machines on Guhdraad, all on one Mika-rad (block guhs:ringsausuman_mikarad, 10 vadskracht):
    far too heavy, so everything stands still and the hover readout says so.
  - floor 1, the deegkamer: the Deegkneder (block guhs:ringsausuman_voorraad[soort=deeg]) between a Vadsmolen and a
    Knutselmachine that hang on a wire to nowhere.
  - floor 2, the sauskamer: the Sauskraan (soort=saus), a glass tank of kaassaus, the automatic frituur and brouwketel.
  - floor 3, the study: the Kaaskast (soort=kaas: there is only an onion in it), books, and the Pannantir on its pedestal.
  - the roof between the horns.
Straight flights of stairs along the walls, east and west in turn, so you cross every room.
Around it the yard: the path, lamp posts with uienlichten, the stumps of the sate trees he felled, a heap of failed
inventions and a Rustvuurtje of the Knabbelring (a rest point).

The geometry the Java side needs (feature/ringsausuman/Toren.java) is in PLEKKEN; the module's self-check compares it with the
constants in that file.

  toren(h)       -> (Structure, problems)   the template with its entities (not saved)
  preview(out)   (python tools/features/ring_sausuman_bouw.py <out>) pictures from four sides, cut open, and floor plans
"""
import math
import os
import random
import sys

G = 3                                   # the ground layer (what you walk on is G + 1)
SIZE = (29, G + 38, 31)
CX, CZ = 14, 13                         # the tower's axis
MIDDEN = "guhs:sausuman_toren_midden"
STRUCTUUR = "sausuman_toren"

# the floors: the y of the floor blocks (feet stand one higher)
V0, V1, V2, V3, DAK = G, G + 7, G + 12, G + 17, G + 22

AIR = "minecraft:air"
BAKSTEEN = "minecraft:polished_blackstone_bricks"
GEBARSTEN = "minecraft:cracked_polished_blackstone_bricks"
GLAD = "minecraft:polished_blackstone"
RUW = "minecraft:blackstone"
GEBEITELD = "minecraft:chiseled_polished_blackstone"
BASALT = "minecraft:polished_basalt"
TRAP = "minecraft:polished_blackstone_brick_stairs"
PLAAT = "minecraft:polished_blackstone_brick_slab"
MUUR = "minecraft:polished_blackstone_brick_wall"
TEGELS = "minecraft:deepslate_tiles"
GLAS = "minecraft:orange_stained_glass"
TANKGLAS = "minecraft:glass"
KOPER = "minecraft:waxed_copper_block"
KOPER_GESNEDEN = "minecraft:waxed_cut_copper"
KOPER_PLAAT = "minecraft:waxed_cut_copper_slab"
PIJP = "minecraft:lightning_rod"
KETTING = "minecraft:chain"
LANTAARN = "minecraft:lantern"
UIENLICHT = "guhs:uienlicht"
GLOEIKOOL = "guhs:gloeikool"
HOUTSKOOL = "guhs:houtskoolsteen"
AS = "guhs:as_blok"
AS_AARDE = "guhs:as_aarde"
SATE = "guhs:sate_stam"
IJZER = "guhs:gepolijst_roosterijzer"
TRALIES = "guhs:roosterijzer_tralies"
DRAAD = "guhs:guh_wire"
SAUS = "guhs:kaas_saus"
RUSTVUUR = "guhs:ring_rustvuur"
RINGENBAKKER = "guhs:ringsausuman_ringenbakker"
VOORRAAD = "guhs:ringsausuman_voorraad"
MIKARAD = "guhs:ringsausuman_mikarad"
SPUTTERPIJP = "guhs:ringsausuman_sputterpijp"
PANNANTIR = "guhs:ringsausuman_pannantir"
BORD = "minecraft:crimson_wall_sign"
# the real vadskracht machines that stand in the tower (all of them: facing + snoet=slaapt)
MACHINES = ("guhs:knabbelaar", "guhs:knutselmachine", "guhs:oogster", "guhs:guh_oven", "guhs:vadsmolen", "guhs:frituurautomaat",
            "guhs:brouwautomaat", "guhs:grillkoolpers", "guhs:neerzetter")
KNOPEN = MACHINES + (MIKARAD,)                                 # what Guhdraad joins
SOORTEN = ("deeg", "saus", "kaas")

PLEKKEN = {
    "NPC": (CX - 3, V0 + 1, CZ - 3),                           # Sausuman, in the hall next to his machine
    "BAKKER": (CX, V0 + 2, CZ - 5),                            # the Ringenbakker's face (the scene's anchor), looks south
    "VOORRADEN": [(CX, V1 + 1, CZ - 5), (CX, V2 + 1, CZ + 4), (CX, V3 + 1, CZ - 4)],   # deeg, saus, kaas
    "MIKARAD": (CX - 5, V0 + 1, CZ),
    "PANNANTIR": (CX, V3 + 2, CZ),
    "DEUR": (CX, V0 + 1, CZ + 7),                              # just outside the door
    "RUSTVUUR": (CX + 6, V0 + 1, CZ + 12),
}
NPC_YAW = -45.0                                                # he looks at whoever comes in (south-east)
# what the hall's net asks and gets (the hover readout shows these numbers; the texts name them)
HAL_MACHINES = [("guhs:knabbelaar", (-5, -2), "east"), ("guhs:knutselmachine", (-5, 2), "east"), ("guhs:oogster", (-4, 3), "north"),
                ("guhs:guh_oven", (-3, 3), "north"), ("guhs:vadsmolen", (-3, 4), "north")]


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


def oct_(r, c):
    """An eight-sided plan round the axis: {(dx, dz)} with |dx|, |dz| <= r and |dx| + |dz| <= c."""
    return {(dx, dz) for dx in range(-r, r + 1) for dz in range(-r, r + 1) if abs(dx) + abs(dz) <= c}


BUITEN_L, BINNEN_L = oct_(6, 9), oct_(5, 7)                    # the lower shaft (hall and floor 1)
BUITEN_H, BINNEN_H = oct_(5, 7), oct_(4, 5)                    # the upper shaft (floors 2 and 3)
ROK = oct_(7, 10)                                              # the plinth's skirt
HOEKEN = ((1, 1), (1, -1), (-1, 1), (-1, -1))


def steunbeer_laag(sx, sz):
    """A buttress of the lower shaft: (cell, top y) of its columns, stepped (the outer ones end lower)."""
    return [((sx * 4, sz * 5), G + 12), ((sx * 5, sz * 4), G + 12), ((sx * 5, sz * 5), G + 14), ((sx * 4, sz * 6), G + 11), ((sx * 6, sz * 4), G + 11),
            ((sx * 5, sz * 6), G + 8), ((sx * 6, sz * 5), G + 8)]


def steunbeer_hoog(sx, sz):
    """A buttress of the upper shaft, up to the roof: its cells."""
    return [(sx * 3, sz * 5), (sx * 4, sz * 4), (sx * 5, sz * 3), (sx * 4, sz * 5), (sx * 5, sz * 4)]


def hoorn(sx, sz):
    """A horn above the roof: [(cell, y0, y1)]: a thick foot, a part that leans outwards, a point."""
    voet = [(sx * 3, sz * 5), (sx * 4, sz * 4), (sx * 5, sz * 3), (sx * 4, sz * 5), (sx * 5, sz * 4)]
    return ([(c, DAK + 1, DAK + 4) for c in voet] + [((sx * 4, sz * 5), DAK + 5, DAK + 7), ((sx * 5, sz * 4), DAK + 5, DAK + 7),
                                                    ((sx * 5, sz * 5), DAK + 4, DAK + 12)])


def trappen():
    """The four flights: [(dx, dz0, y0, n, floor y it ends in)]: step i stands at (dx, y0 + i, dz0 - i), you climb north."""
    return [(4, 3, V0 + 1, 7, V1), (-4, 3, V1 + 1, 5, V2), (3, 2, V2 + 1, 5, V3), (-3, 2, V3 + 1, 5, DAK)]


class Bouw:
    def __init__(self, h):
        self.h = h
        self.s = h.Structure(SIZE)
        self.rng = random.Random(21302201)
        self.W, self.H, self.D = SIZE
        self.plaat = set()

    # --- basics ---------------------------------------------------------------------------------------------------------
    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mc(name), props, nbt)

    def zet(self, dx, y, dz, name, props=None, nbt=None):
        """A block at (dx, dz) from the tower's axis."""
        self.s.set(CX + dx, y, CZ + dz, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def bij(self, dx, y, dz):
        return self.s.get(CX + dx, y, CZ + dz)

    def lucht(self, x, y, z):
        if self.get(x, y, z) is None:
            self.set(x, y, z, AIR)

    def steen(self):
        return self.rng.choice([BAKSTEEN] * 7 + [GEBARSTEN] * 2 + [GLAD])

    def trap(self, dx, y, dz, facing, half="bottom"):
        self.zet(dx, y, dz, TRAP, {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"})

    def plaatje(self, dx, y, dz, soort="bottom", name=PLAAT):
        self.zet(dx, y, dz, name, {"type": soort, "waterlogged": "false"})

    def lantaarn(self, dx, y, dz, hangt=False):
        self.zet(dx, y, dz, LANTAARN, {"hanging": "true" if hangt else "false", "waterlogged": "false"})

    def machine(self, dx, y, dz, name, facing):
        props = {"facing": facing, "snoet": "slaapt"}
        if name == "guhs:guh_oven":
            props["lit"] = "false"
        if name == "guhs:grillkoolpers":
            props["perst"] = "false"
        self.zet(dx, y, dz, name, props)

    def draad(self, dx, y, dz):
        self.zet(dx, y, dz, DRAAD, {"north": "none", "east": "none", "south": "none", "west": "none", "powered": "false"})

    def bord(self, x, y, z, facing, regels):
        import sign_text
        B = self.h.Byte
        prefix = "sign.guhs.ring_sausuman"
        leeg = sign_text.messages(prefix, ["", "", "", ""])
        nbt = {"id": "minecraft:sign", "is_waxed": B(1),
               "front_text": {"messages": sign_text.messages(prefix, regels), "color": "orange", "has_glowing_text": B(1)},
               "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}}
        self.set(x, y, z, BORD, {"facing": facing, "waterlogged": "false"}, nbt)

    # --- the ground -------------------------------------------------------------------------------------------------------
    def grond(self):
        """A frayed oval plate of scorched ground with rock under it; the cave above it is opened (a wide shaft round the
        tower, whatever the ceiling does)."""
        cx, cz = (self.W - 1) / 2, (self.D - 1) / 2
        for x in range(self.W):
            for z in range(self.D):
                d = math.hypot((x - cx) / (self.W / 2 - 0.3), (z - cz) / (self.D / 2 - 0.3))
                if d > 1.0 + self.rng.uniform(-0.06, 0.03) and math.hypot(x - CX, z - CZ) > 9:
                    continue
                self.plaat.add((x, z))
                self.set(x, G, z, self.rng.choice([HOUTSKOOL] * 4 + [AS_AARDE] * 2 + [AS] * 2 + [RUW]))
                for y in range(0, G):
                    self.set(x, y, z, HOUTSKOOL)
        for x in range(self.W):
            for z in range(self.D):
                d = math.hypot(x - CX, z - CZ)
                if d <= 10.5:
                    top = self.H
                elif d <= 12.5:
                    top = G + 20
                else:
                    continue
                for y in range(G + 1, top):
                    self.lucht(x, y, z)
        self.s.clear_above(self.plaat, G + 1)

    def midden(self):
        """The centre jigsaw, in layer 0 under the path in front of the door; the start pool says where the ground is
        (ground_level_delta = G + 1, set by ring_sausuman.structuur, as the other cave buildings of this update do)."""
        x, z = PLEKKEN["DEUR"][0], PLEKKEN["DEUR"][2] + 2
        self.s.set(x, 0, z, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": MIDDEN, "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": HOUTSKOOL, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})

    # --- the shell --------------------------------------------------------------------------------------------------------
    def schacht(self):
        wand_l, wand_h = BUITEN_L - BINNEN_L, BUITEN_H - BINNEN_H
        for dx, dz in BUITEN_L | ROK:
            self.zet(dx, G, dz, GLAD if (dx, dz) in BINNEN_L else BAKSTEEN)
            self.plaat.add((CX + dx, CZ + dz))
        # the hall floor: tiles with a darker cross
        for dx, dz in BINNEN_L:
            self.zet(dx, V0, dz, TEGELS if dx == 0 or dz == 0 else GLAD)
        # the skirt: a plinth with a sloped top
        for dx, dz in ROK - BUITEN_L:
            self.zet(dx, G + 1, dz, self.steen())
            if abs(dx) == 7 and abs(dz) <= 3:
                self.trap(dx, G + 2, dz, "west" if dx > 0 else "east")
            elif abs(dz) == 7 and abs(dx) <= 3:
                self.trap(dx, G + 2, dz, "north" if dz > 0 else "south")
            else:
                self.plaatje(dx, G + 2, dz)
        # the lower shaft
        for y in range(G + 1, V2):
            for dx, dz in wand_l:
                self.zet(dx, y, dz, self.steen())
            for dx, dz in BINNEN_L:
                self.zet(dx, y, dz, AIR)
        # a band of chiselled stone under the shoulder, and the shoulder itself: a slope from the wide shaft to the narrow one
        for dx, dz in wand_l:
            if abs(dx) == 6 or abs(dz) == 6 or abs(dx) + abs(dz) == 9:
                self.zet(dx, V2 - 1, dz, GEBEITELD)
        for dx, dz in BUITEN_L - BUITEN_H:
            if abs(dx) == 6 and abs(dz) <= 3:
                self.trap(dx, V2, dz, "west" if dx > 0 else "east")
            elif abs(dz) == 6 and abs(dx) <= 3:
                self.trap(dx, V2, dz, "north" if dz > 0 else "south")
            else:
                self.plaatje(dx, V2, dz)
        # the upper shaft
        for y in range(V2, DAK):
            for dx, dz in wand_h:
                self.zet(dx, y, dz, self.steen())
            for dx, dz in BINNEN_H:
                self.zet(dx, y, dz, AIR)
        # the floors
        for dx, dz in BINNEN_L:
            self.zet(dx, V1, dz, GLAD if (dx + dz) % 2 else TEGELS)
        for dx, dz in BINNEN_H:
            self.zet(dx, V2, dz, GLAD if (dx + dz) % 2 else TEGELS)
            self.zet(dx, V3, dz, TEGELS if abs(dx) + abs(dz) in (2, 3) else GLAD)
        for dx, dz in BUITEN_H:
            self.zet(dx, DAK, dz, GLAD if (dx, dz) in BINNEN_H else BAKSTEEN)
        for y in (V1, V2):
            for dx, dz in wand_l if y == V1 else wand_h:
                if (abs(dx) + abs(dz)) % 2 == 0 and self.bij(dx, y, dz) not in (GEBEITELD,):
                    self.zet(dx, y, dz, GEBARSTEN if self.rng.random() < 0.3 else BAKSTEEN)

    def steunberen(self):
        for sx, sz in HOEKEN:
            for (dx, dz), top in steunbeer_laag(sx, sz):
                self.zet(dx, G, dz, RUW)
                self.plaat.add((CX + dx, CZ + dz))
                for y in range(G + 1, top + 1):
                    self.zet(dx, y, dz, BASALT, {"axis": "y"})
                # each column ends in a cap: the stepped look of a buttress
                if top == G + 8:
                    self.plaatje(dx, top + 1, dz)
                elif top == G + 11:
                    self.zet(dx, top + 1, dz, GEBEITELD)
                elif top == G + 14:
                    self.zet(dx, top + 1, dz, MUUR)
            for dx, dz in steunbeer_hoog(sx, sz):
                for y in range(V2, DAK + 1):
                    self.zet(dx, y, dz, BASALT, {"axis": "y"})
            for (dx, dz), y0, y1 in hoorn(sx, sz):
                for y in range(y0, y1 + 1):
                    self.zet(dx, y, dz, BASALT, {"axis": "y"})
            # the point: a spike on the outermost column, a cap on the two columns beside it
            self.zet(sx * 5, DAK + 13, sz * 5, MUUR)
            self.zet(sx * 5, DAK + 14, sz * 5, PIJP, {"facing": "up", "powered": "false", "waterlogged": "false"})
            self.zet(sx * 4, DAK + 8, sz * 5, MUUR)
            self.zet(sx * 5, DAK + 8, sz * 4, MUUR)
            for c in ((sx * 3, sz * 5), (sx * 5, sz * 3), (sx * 4, sz * 4)):
                self.plaatje(c[0], DAK + 5, c[1])

    def ramen_en_deur(self):
        # tall slits of orange glass: two per side below, one above; none where a machine or a flight stands against the wall
        for y0 in (V0 + 3, V1 + 2):
            for dz in (-1, 1):
                for dx in (-6, 6):
                    if y0 == V0 + 3 and dx == 6:
                        continue                               # (the hall's stairs)
                    for y in (y0, y0 + 1):
                        self.zet(dx, y, dz, GLAS)
            for dx in (-1, 1):
                for dz in (-6, 6):
                    if y0 == V0 + 3:
                        continue                               # (the machine in the north, the door in the south)
                    for y in (y0, y0 + 1):
                        self.zet(dx, y, dz, GLAS)
        for y0 in (V2 + 2, V3 + 2):
            for dx, dz in ((-5, 0), (5, 0), (0, -5), (0, 5)):
                if y0 == V2 + 2 and (dx, dz) == (0, -5):
                    continue                                   # (the sauce tank)
                for y in (y0, y0 + 1):
                    self.zet(dx, y, dz, GLAS)
        # the door: three wide, four high, a pointed arch, the skirt opened in front of it
        for dx in (-1, 0, 1):
            for y in range(V0 + 1, V0 + 5):
                self.zet(dx, y, 6, AIR)
            for y in (G + 1, G + 2):
                self.zet(dx, y, 7, AIR)
            self.zet(dx, G, 7, TEGELS)
            self.zet(dx, G, 6, TEGELS)
        self.trap(-1, V0 + 4, 6, "west", "top")
        self.trap(1, V0 + 4, 6, "east", "top")
        self.zet(0, V0 + 5, 6, GEBEITELD)
        # the porch: two basalt posts with a light, and the name
        for dx in (-2, 2):
            for y in range(G + 1, G + 4):
                self.zet(dx, y, 8, BASALT, {"axis": "y"})
            self.zet(dx, G, 8, RUW)
            self.zet(dx, G + 4, 8, UIENLICHT)
            self.plaatje(dx, G + 5, 8)
        self.bord(CX - 2, G + 2, CZ + 9, "south", ["Toren van", "SAUSUMAN", "van de", "Vele Sauzen"])
        self.bord(CX + 2, G + 2, CZ + 9, "south", ["Ringdragers:", "ring afgeven", "bij de deur.", "Dank u. Njeg."])

    def trappen(self):
        """Four straight flights along the walls. Under a flight the wall is solid; the floor above is open over it, with a
        low wall round the hole."""
        for nr, (dx, dz0, y0, n, vloer) in enumerate(trappen()):
            kant = 1 if dx > 0 else -1
            for i in range(n):
                dz, y = dz0 - i, y0 + i
                self.trap(dx, y, dz, "north")
                for yy in range(y0, y):
                    self.zet(dx, yy, dz, BAKSTEEN)
                # room to walk: three blocks of air over every step (through the floor above and, once, through the wall)
                for yy in range(y + 1, y + 4):
                    if yy <= vloer + 3 and self.bij(dx, yy, dz) != AIR:
                        self.zet(dx, yy, dz, AIR)
                if i >= 1:
                    for yy in range(y + 1, y + 3):
                        if self.bij(dx, yy, dz + 1) not in (AIR, None):
                            self.zet(dx, yy, dz + 1, AIR)
            # the strip between the flight and the wall: solid up to the floor above
            binnen = BINNEN_L if nr < 2 else BINNEN_H
            for dz in range(-6, 7):
                if (dx + kant, dz) in binnen:
                    for yy in range(y0, vloer + 1):
                        self.zet(dx + kant, yy, dz, BAKSTEEN)
            # the hole in the floor above: a low wall on its open side and at its far end
            top_dz = dz0 - (n - 1)
            open_dz = [dz for dz in range(top_dz + 1, dz0 + 1) if self.bij(dx, vloer, dz) == AIR]
            for dz in open_dz:
                self.zet(dx - kant, vloer + 1, dz, MUUR)
            if open_dz and self.bij(dx, vloer, max(open_dz) + 1) not in (AIR, None):
                self.zet(dx, vloer + 1, max(open_dz) + 1, MUUR)

    # --- the rooms --------------------------------------------------------------------------------------------------------
    def hal(self):
        y = V0 + 1
        # DE RINGENBAKKER: a wall of copper between two basalt boilers, its face in the middle, a funnel on top, a tray in front
        for dx in (-2, 2):
            for yy in range(y, y + 3):
                self.zet(dx, yy, -5, BASALT, {"axis": "y"})
            self.zet(dx, y + 3, -5, SPUTTERPIJP, {"groot": "false"})
        for dx in (-1, 0, 1):
            self.zet(dx, y, -5, KOPER_GESNEDEN)
        for dx in (-1, 1):
            self.zet(dx, y + 1, -5, IJZER)
            self.zet(dx, y + 2, -5, KOPER)
            self.plaatje(dx, y + 3, -5, name=KOPER_PLAAT)
        bx, by, bz = PLEKKEN["BAKKER"]
        self.set(bx, by, bz, RINGENBAKKER, {"facing": "south"})
        self.zet(0, y + 2, -5, "cauldron")
        self.zet(0, y + 3, -5, PIJP, {"facing": "up", "powered": "false", "waterlogged": "false"})
        self.plaatje(0, y, -4, name="minecraft:polished_blackstone_slab")
        # lights: two lanterns on chains
        for dz in (-1, 2):
            self.zet(0, V1 - 1, dz, KETTING, {"axis": "y", "waterlogged": "false"})
            self.lantaarn(0, V1 - 2, dz, hangt=True)
        # the west wall: everything he owns on one Mika-rad
        mx, my, mz = PLEKKEN["MIKARAD"]
        self.set(mx, my, mz, MIKARAD, {"facing": "east"})
        for name, (dx, dz), facing in HAL_MACHINES:
            self.machine(dx, y, dz, name, facing)
        for dz in range(-2, 3):
            self.draad(-4, y, dz)
        self.bord(CX - 4, y + 1, CZ - 1, "east", ["Mika-rad", "Vermogen:", "een beetje", ""])
        self.zet(-5, y + 1, -1, self.steen())
        # his Mokhoek, behind him
        self.bord(CX - 4, y + 2, CZ - 3, "south", ["~ MOKHOEK ~", "Alleen in", "noodgevallen", ""])
        self.zet(-2, y, -4, "minecraft:white_carpet")
        self.zet(-3, y, -4, "minecraft:white_carpet")

    def deegkamer(self):
        y = V1 + 1
        x, yy, z = PLEKKEN["VOORRADEN"][0]
        self.set(x, yy, z, VOORRAAD, {"facing": "south", "soort": "deeg"})
        self.machine(-2, y, -5, "guhs:vadsmolen", "south")
        self.machine(2, y, -5, "guhs:knutselmachine", "south")
        self.draad(-1, y, -5)
        self.draad(1, y, -5)
        self.bord(CX, y + 1, CZ - 5, "south", ["DEEGKNEDER", "Kneedt deeg.", "En poten, als", "je niet oplet."])
        self.zet(0, y + 1, -6, self.steen())
        # sacks of flour and a table full of failed rings
        for dx, dz, n in ((-4, -3, 2), (-3, -4, 1), (3, -4, 2), (2, 4, 1), (1, 5, 2)):
            for i in range(n):
                self.zet(dx, y + i, dz, "minecraft:hay_block", {"axis": "y"})
        self.zet(-2, y, 4, "guhs:tekentafel", {"facing": "north", "snoet": "slaapt"})
        self.zet(-1, y, 5, "minecraft:lectern", {"facing": "north", "has_book": "false", "powered": "false"})
        self.zet(0, V2 - 1, 0, KETTING, {"axis": "y", "waterlogged": "false"})
        self.lantaarn(0, V2 - 2, 0, hangt=True)
        self.zet(0, y, 2, "minecraft:yellow_carpet")

    def sauskamer(self):
        y = V2 + 1
        x, yy, z = PLEKKEN["VOORRADEN"][1]
        self.set(x, yy, z, VOORRAAD, {"facing": "north", "soort": "saus"})
        self.bord(CX, y + 1, CZ + 4, "north", ["SAUSKRAAN", "Heet!", "Niet likken.", "(Wel lekker.)"])
        self.zet(0, y + 1, 5, self.steen())
        # the tank: kaassaus behind glass in the north bay, a copper lid, a pipe up through the ceiling
        for yy in (y, y + 1, y + 2):
            self.zet(0, yy, -4, SAUS)
            self.zet(0, yy, -3, TANKGLAS)
            for dx in (-1, 1):
                self.zet(dx, yy, -4, TANKGLAS)
            self.zet(0, yy, -5, GLAD)
        self.zet(0, y + 3, -4, KOPER)
        for dx in (-1, 1):
            self.plaatje(dx, y + 3, -4, name=KOPER_PLAAT)
        self.plaatje(0, y + 3, -3, name=KOPER_PLAAT)
        self.machine(-2, y, -3, "guhs:frituurautomaat", "south")
        self.machine(2, y, -3, "guhs:brouwautomaat", "south")
        self.machine(-2, y, 3, "guhs:grillkoolpers", "north")
        self.draad(-2, y, -2)
        self.draad(-1, y, -2)
        self.draad(1, y, -2)
        self.draad(2, y, -2)
        self.zet(2, y, 3, "minecraft:cauldron")
        self.zet(1, y, 4, KOPER_GESNEDEN)
        self.zet(1, y + 1, 4, PIJP, {"facing": "up", "powered": "false", "waterlogged": "false"})
        self.lantaarn(-1, y, 4)
        self.zet(0, V3 - 1, 1, KETTING, {"axis": "y", "waterlogged": "false"})
        self.lantaarn(0, V3 - 2, 1, hangt=True)
        for dx, dz, kleur in ((0, 0, "orange"), (-1, 1, "yellow"), (1, -1, "red"), (-1, 0, "brown")):
            self.zet(dx, y, dz, f"minecraft:{kleur}_carpet")

    def studeerkamer(self):
        y = V3 + 1
        x, yy, z = PLEKKEN["VOORRADEN"][2]
        self.set(x, yy, z, VOORRAAD, {"facing": "south", "soort": "kaas"})
        self.bord(CX, y + 1, CZ - 4, "south", ["KAAS", "(voor de ring)", "Afblijven!", ""])
        self.zet(0, y + 1, -5, self.steen())
        # the Pannantir on its pedestal, in the middle of a round rug
        px, py, pz = PLEKKEN["PANNANTIR"]
        self.set(px, py - 1, pz, GEBEITELD)
        self.set(px, py, pz, PANNANTIR)
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            self.zet(dx, y, dz, "minecraft:white_carpet")
        for dx, dz, kleur in ((1, 1, "red"), (-1, -1, "yellow"), (1, -1, "orange"), (-1, 1, "brown")):
            self.zet(dx, y, dz, f"minecraft:{kleur}_carpet")
        # books on both sides of the cupboard and against the south wall, a lectern, a candle-lit corner
        for dx in (-2, -1, 1, 2):
            for i in range(2 if abs(dx) == 2 else 3):
                if dx in (-1, 1) and i == 0:
                    self.zet(dx, y + i, -4 if abs(dx) == 1 else -3, "minecraft:bookshelf")
                else:
                    self.zet(dx, y + i, -4 if abs(dx) == 1 else -3, "minecraft:bookshelf")
        for dx in (-1, 0, 1):
            self.zet(dx, y, 4, "minecraft:bookshelf")
            self.zet(dx, y + 1, 4, "minecraft:bookshelf")
        self.zet(0, y + 2, 4, UIENLICHT)
        self.zet(2, y, 2, "minecraft:lectern", {"facing": "west", "has_book": "false", "powered": "false"})
        self.lantaarn(-2, y, 3)
        self.zet(0, DAK - 1, 2, KETTING, {"axis": "y", "waterlogged": "false"})
        self.lantaarn(0, DAK - 2, 2, hangt=True)

    def dak(self):
        y = DAK + 1
        rand_ = {c for c in BUITEN_H if any((c[0] + a, c[1] + b) not in BUITEN_H for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
        hoorns = {c for sx, sz in HOEKEN for c, y0, y1 in hoorn(sx, sz)}
        for dx, dz in rand_ - hoorns:
            midden = (abs(dx) == 5 and dz == 0) or (abs(dz) == 5 and dx == 0)
            self.zet(dx, y, dz, GEBEITELD if midden else MUUR)
            if midden:
                self.lantaarn(dx, y + 1, dz)
        # the brazier between the horns: glowing coal in an iron cage, and the big chimney that never stops smoking
        self.zet(0, y, 0, GLOEIKOOL)
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            self.plaatje(dx, y, dz)
        self.zet(0, y + 1, 0, KOPER)
        self.zet(0, y + 2, 0, SPUTTERPIJP, {"groot": "true"})
        self.bord(CX, y, CZ + 3, "north", ["Uitkijkpunt", "Ringdragers", "spotten:", "gratis"])
        self.zet(0, y, 4, self.steen())

    def pijpen(self):
        """Copper pipes up the outside, each ending in a sputterpijp."""
        rod = {"facing": "up", "powered": "false", "waterlogged": "false"}
        # west: from the hall's machines up to under the shoulder
        self.zet(-7, G + 3, 2, KOPER)
        for y in range(G + 4, G + 10):
            self.zet(-7, y, 2, PIJP, rod)
        self.zet(-7, G + 10, 2, SPUTTERPIJP, {"groot": "true"})
        # north: the Ringenbakker's two exhausts
        for dx in (-2, 2):
            self.zet(dx, G + 5, -7, KOPER)
            self.zet(dx, G + 6, -7, PIJP, rod)
            self.zet(dx, G + 7, -7, SPUTTERPIJP, {"groot": "true"})
        # east: from the sauskamer up past the study
        self.zet(6, V2 + 2, -2, KOPER)
        for y in range(V2 + 3, V3 + 3):
            self.zet(6, y, -2, PIJP, rod)
        self.zet(6, V3 + 3, -2, SPUTTERPIJP, {"groot": "true"})
        # south: a short one over the door
        self.zet(3, V1 + 1, 7, KOPER)
        self.zet(3, V1 + 2, 7, PIJP, rod)
        self.zet(3, V1 + 3, 7, SPUTTERPIJP, {"groot": "false"})

    # --- the yard ---------------------------------------------------------------------------------------------------------
    def erf(self):
        def pad(x, z):
            if (x, z) in self.plaat:
                self.set(x, G, z, self.rng.choice([BAKSTEEN] * 4 + [GEBARSTEN] * 2 + [TEGELS]))
        for z in range(CZ + 8, self.D):
            for x in range(CX - 1, CX + 2):
                pad(x, z)
        # lamp posts along the path
        for z in (CZ + 12, CZ + 16):
            for x in (CX - 3, CX + 3):
                if (x, z) in self.plaat:
                    self.set(x, G + 1, z, MUUR)
                    self.set(x, G + 2, z, MUUR)
                    self.set(x, G + 3, z, UIENLICHT)
        # the stumps of the sate trees he felled, and a sign that is proud of it
        for x, z in ((5, 22), (8, 26), (22, 25), (24, 19), (4, 9), (24, 6), (20, 28), (3, 16)):
            if (x, z) in self.plaat:
                self.set(x, G + 1, z, SATE, {"axis": "y"})
        self.set(9, G + 1, 23, SATE, {"axis": "x"})
        self.set(10, G + 1, 23, SATE, {"axis": "x"})
        self.set(8, G + 2, 26, SATE, {"axis": "y"})
        self.bord(8, G + 2, 27, "south", ["Hier stond", "een bos.", "Nu staat hier", "vooruitgang."])
        # the heap of failed inventions (north-west)
        for x, y, z, name, props in ((4, 1, 5, IJZER, None), (5, 1, 5, KOPER_GESNEDEN, None), (5, 2, 5, "minecraft:cauldron", None),
                                      (4, 1, 6, "minecraft:anvil", {"facing": "east"}), (6, 1, 4, IJZER, None), (6, 2, 4, KOPER_PLAAT, {"type": "bottom", "waterlogged": "false"}),
                                      (5, 1, 4, KOPER, None), (5, 2, 4, PIJP, {"facing": "up", "powered": "false", "waterlogged": "false"}),
                                      (7, 1, 5, "guhs:knabbelbuis", {d: "false" for d in ("north", "east", "south", "west", "up", "down")}),
                                      (4, 2, 5, SPUTTERPIJP, {"groot": "false"})):
            self.set(x, G + y, z, mc(name), props)
        self.bord(5, G + 1, 6, "south", ["Uitvinding", "1 t/m 46.", "Doen het niet.", "NIET LACHEN."])
        # the rest point of the Knabbelring: a Rustvuurtje with two seats, east of the path
        rx, ry, rz = PLEKKEN["RUSTVUUR"]
        self.set(rx, ry, rz, RUSTVUUR)
        self.set(rx, G, rz, RUW)
        self.set(rx + 2, ry, rz, TRAP, {"facing": "east", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        self.set(rx, ry, rz + 2, TRAP, {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        # glowing coal and ash round the foot of the tower
        for x, z in ((8, 9), (20, 9), (7, 18), (21, 18), (10, 6), (19, 5)):
            if (x, z) in self.plaat and self.get(x, G + 1, z) in (None, AIR):
                self.set(x, G, z, GLOEIKOOL)

    # --- connections ------------------------------------------------------------------------------------------------------
    def verbind(self):
        """Walls and Guhdraad get the connections their neighbours give them (a template is placed with its states exactly as
        they are)."""
        blocks = self.s.blocks

        def naam(c):
            b = blocks.get(c)
            return b[0] if b else None

        def vol(n):
            return n is not None and n not in DUN and not any(t in n for t in ("_slab", "_stairs", "sign", "carpet"))
        for (x, y, z), (name, props, nbt) in list(blocks.items()):
            if name == MUUR:
                kant = {}
                for richting, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0))):
                    n = naam((x + dx, y, z + dz))
                    kant[richting] = vol(n) or n == MUUR
                boven = naam((x, y + 1, z))
                recht = (kant["north"] and kant["south"] and not kant["east"] and not kant["west"]) or \
                        (kant["east"] and kant["west"] and not kant["north"] and not kant["south"])
                p = {k: ("low" if v else "none") for k, v in kant.items()}
                p.update(up="true" if (not recht or boven not in (None, AIR)) else "false", waterlogged="false")
                blocks[(x, y, z)] = (name, p, nbt)
            elif name == DRAAD:
                p = {"powered": "false"}
                for richting, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0))):
                    p[richting] = "side" if naam((x + dx, y, z + dz)) in (DRAAD,) + KNOPEN else "none"
                blocks[(x, y, z)] = (name, p, nbt)


DUN = (AIR, LANTAARN, KETTING, PIJP, DRAAD, SPUTTERPIJP, PANNANTIR, RUSTVUUR, "minecraft:cauldron", "minecraft:anvil", "minecraft:lectern",
       "guhs:knabbelbuis", TRALIES)


def toren(h, wezens=True):
    """The tower's template (not saved) and the problems of its self-check."""
    b = Bouw(h)
    b.grond()
    b.schacht()
    b.steunberen()
    b.ramen_en_deur()
    b.trappen()
    b.hal()
    b.deegkamer()
    b.sauskamer()
    b.studeerkamer()
    b.dak()
    b.pijpen()
    b.erf()
    b.midden()
    b.verbind()
    if wezens:
        from features import wereld
        x, y, z = PLEKKEN["NPC"]
        b.s.entity(x + 0.5, y, z + 0.5, wereld.npc(h, "sausuman", "ringsausuman_sausuman", yaw=NPC_YAW))
    return b.s, check(b.s)


def test_kamer(h):
    """The game test room: a bare floor of blackstone (things on the floor stand at helper y 2)."""
    t = h.Structure((21, 12, 21))
    for x in range(21):
        for z in range(21):
            t.set(x, 0, z, GLAD)
    return t


# =====================================================================================================================
# the self-checks
# =====================================================================================================================
LOOPT_DOOR = (AIR, DRAAD, KETTING, PIJP, "minecraft:white_carpet", "minecraft:yellow_carpet", "minecraft:orange_carpet", "minecraft:red_carpet",
              "minecraft:brown_carpet")


def _vast(blocks):
    """The solid half blocks of the template: {(x, half y, z)} (half y = 2 * y, + 1 for the upper half)."""
    vol = set()
    for (x, y, z), b in blocks.items():
        n, p = b[0], b[1]
        if n in LOOPT_DOOR or "sign" in n or (n == LANTAARN and p.get("hanging") == "true"):
            continue
        if n.endswith("_slab"):
            if p.get("type") == "top":
                vol.add((x, 2 * y + 1, z))
            elif p.get("type") == "bottom":
                vol.add((x, 2 * y, z))
            else:
                vol.update(((x, 2 * y, z), (x, 2 * y + 1, z)))
        else:
            vol.update(((x, 2 * y, z), (x, 2 * y + 1, z)))
    return vol


def bereikbaar(blocks, start):
    """Every spot a player can walk to from `start` (x, y, z: feet on y): {(x, half y, z)} of the feet. A step goes up half a
    block, or a whole one onto a stair that climbs that way; down any height up to three blocks. A player is two blocks
    (four halves) high, and where the feet go up or down there has to be that much room in BOTH columns at the higher
    of the two heights (or the head bumps on the way)."""
    vol = _vast(blocks)
    hoog = 4

    def vrij(x, hy, z):
        return all((x, hy + i, z) not in vol for i in range(hoog))

    def staat(x, hy, z):
        return (x, hy - 1, z) in vol and vrij(x, hy, z)

    def trap_op(c, dx, dz):
        """Can you step onto this stair block moving (dx, dz)? From its low end and from its sides (onto the low half): yes."""
        b = blocks.get(c)
        if not b or not b[0].endswith("_stairs") or b[1].get("half") != "bottom":
            return False
        return {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[b[1]["facing"]] != (-dx, -dz)

    s = (start[0], 2 * start[1], start[2])
    if not staat(*s):
        return set()
    gezien, todo = {s}, [s]
    while todo:
        x, hy, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dh in (2, 1, 0, -1, -2, -3, -4, -5, -6):
                n = (x + dx, hy + dh, z + dz)
                if n in gezien or not staat(*n):
                    continue
                if dh == 2 and not trap_op((n[0], (n[1] - 2) // 2, n[2]), dx, dz):
                    continue
                if dh > 0 and not vrij(x, hy + dh, z):
                    continue
                if dh < 0 and not vrij(n[0], hy, n[2]):
                    continue
                gezien.add(n)
                todo.append(n)
                break
    return gezien


def check(s):
    problems = []
    blocks = s.blocks

    def nm(c):
        b = blocks.get(c)
        return b[0] if b else None

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
    for c in sorted(solid - seen)[:20]:
        problems.append(f"floating block {blocks[c][0]} at {c}")
    # what stands on something stands on something
    for c, b in blocks.items():
        onder = nm((c[0], c[1] - 1, c[2]))
        if b[0] in MACHINES + (MIKARAD, VOORRAAD, PANNANTIR, RUSTVUUR, DRAAD, "minecraft:cauldron", "minecraft:lectern", "minecraft:anvil") or \
                b[0].endswith("_carpet") or (b[0] == LANTAARN and b[1].get("hanging") == "false"):
            if onder in (None,) + DUN or onder.endswith("_carpet"):
                problems.append(f"{b[0]} at {c} stands on {onder}")
        if b[0] == LANTAARN and b[1].get("hanging") == "true" and nm((c[0], c[1] + 1, c[2])) != KETTING:
            problems.append(f"the hanging lantern at {c} hangs from nothing")
        if b[0] == MUUR and "north" not in b[1]:
            problems.append(f"the wall at {c} has no connections")
        if b[0] == BORD:
            dx, dz = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}[b[1]["facing"]]
            if nm((c[0] + dx, c[1], c[2] + dz)) in (None,) + DUN:
                problems.append(f"the sign at {c} hangs on nothing")
    # the spots the Java side uses
    x, y, z = PLEKKEN["NPC"]
    if nm((x, y - 1, z)) in (None, AIR) or nm((x, y, z)) not in (AIR, None) or nm((x, y + 1, z)) not in (AIR, None):
        problems.append(f"Sausuman has no place to sit at {PLEKKEN['NPC']}")
    if nm(PLEKKEN["BAKKER"]) != RINGENBAKKER or blocks[PLEKKEN["BAKKER"]][1].get("facing") != "south":
        problems.append("the Ringenbakker is not where the scene expects it")
    for soort, plek in zip(SOORTEN, PLEKKEN["VOORRADEN"]):
        b = blocks.get(plek)
        if not b or b[0] != VOORRAAD or b[1].get("soort") != soort:
            problems.append(f"no {soort} station at {plek}")
    if nm(PLEKKEN["MIKARAD"]) != MIKARAD or nm(PLEKKEN["PANNANTIR"]) != PANNANTIR or nm(PLEKKEN["RUSTVUUR"]) != RUSTVUUR:
        problems.append("the Mika-rad, the Pannantir or the Rustvuurtje is not where it should be")
    # from outside the door you can walk to Sausuman, to the machine, to every station, the Pannantir and onto the roof
    loop = bereikbaar(blocks, PLEKKEN["DEUR"])

    def bij(plek, wat, afstand=1):
        x, y, z = plek
        if not any((x + dx, 2 * yy, z + dz) in loop for dx in range(-afstand, afstand + 1) for dz in range(-afstand, afstand + 1)
                   for yy in (y, y - 1) if abs(dx) + abs(dz) == afstand):
            problems.append(f"{wat} at {plek} can't be reached from the door")
    bij(PLEKKEN["NPC"], "Sausuman")
    bij((PLEKKEN["BAKKER"][0], PLEKKEN["BAKKER"][1] - 1, PLEKKEN["BAKKER"][2] + 1), "the Ringenbakker")
    for soort, plek in zip(SOORTEN, PLEKKEN["VOORRADEN"]):
        bij(plek, f"the {soort} station")
    bij((PLEKKEN["PANNANTIR"][0], PLEKKEN["PANNANTIR"][1] - 1, PLEKKEN["PANNANTIR"][2]), "the Pannantir")
    bij(PLEKKEN["RUSTVUUR"], "the Rustvuurtje", 2)
    if not any(k[1] == 2 * (DAK + 1) and (k[0] - CX, k[2] - CZ) in BINNEN_H for k in loop):
        problems.append("the roof can't be reached")
    # nobody walks off the roof: every spot of the platform you can stand on has something to all four sides
    for (x, hy, z) in loop:
        if hy == 2 * (DAK + 1):
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, DAK + 1, z + dz)
                if nm(n) in (None, AIR) and nm((n[0], DAK, n[2])) in (None, AIR) and (n[0] - CX, n[2] - CZ) not in BUITEN_H:
                    problems.append(f"the roof is open at {n}")
    # the hall is free where the scene plays (the player's stand-in, Sam-guh, the camera): 2 x the room in front of the machine
    bx, by, bz = PLEKKEN["BAKKER"]
    for dx in range(-3, 3):
        for dz in range(1, 8):
            for dy in (0, 1, 2):
                c = (bx + dx, by - 1 + dy, bz + dz)
                if nm(c) not in (None, AIR, DRAAD, "minecraft:white_carpet") and not (dz == 1 and dx == 0 and dy == 0) and c != PLEKKEN["NPC"]:
                    if (c[0] - CX, c[2] - CZ) in BINNEN_L and not (nm(c) or "").endswith("wall_sign"):
                        problems.append(f"the scene's floor is not free at {c}: {nm(c)}")
    # the hall's net: the Mika-rad and all five machines hang on one run of wire
    net, todo = set(), [PLEKKEN["MIKARAD"]]
    while todo:
        c = todo.pop()
        if c in net:
            continue
        net.add(c)
        for d in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0)):
            n = (c[0] + d[0], c[1] + d[1], c[2] + d[2])
            if nm(n) in (DRAAD,) + KNOPEN:
                todo.append(n)
    if sum(1 for c in net if nm(c) in MACHINES) != len(HAL_MACHINES):
        problems.append(f"the hall's net holds {sum(1 for c in net if nm(c) in MACHINES)} machines, not {len(HAL_MACHINES)}")
    if not any(b[0] == "minecraft:jigsaw" and b[2] and b[2].get("name") == MIDDEN and c[1] == 0 for c, b in blocks.items()):
        problems.append("no centre jigsaw in layer 0")
    return problems


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
class _H:
    """What the builder needs of the make_v2 namespace, for the preview."""
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


def doorsnede(s, as_, waarde, tot=None):
    """The structure with everything in front of a plane left out (axis "x", "y" or "z"; keeps the part at or behind it)."""
    import make_structures as ms
    t = ms.Structure(s.size)
    i = {"x": 0, "y": 1, "z": 2}[as_]
    t.blocks = {c: b for c, b in s.blocks.items() if (c[i] >= waarde if as_ != "y" else c[i] <= waarde) and (tot is None or c[i] >= tot)}
    return t


def preview(out):
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    from PIL import Image
    kleur = {LANTAARN: (255, 214, 120), PIJP: (214, 122, 86), KETTING: (70, 74, 86), BORD: (126, 58, 86), UIENLICHT: (255, 226, 150),
             RINGENBAKKER: (236, 150, 60), VOORRAAD: (200, 170, 110), MIKARAD: (150, 110, 80), SPUTTERPIJP: (120, 122, 130),
             PANNANTIR: (255, 200, 80), RUSTVUUR: (240, 140, 40), DRAAD: (240, 120, 160), SAUS: (242, 165, 22), GLAS: (255, 150, 40),
             "guhs:knabbelbuis": (190, 220, 230), KOPER: (196, 110, 78), KOPER_GESNEDEN: (190, 106, 80), KOPER_PLAAT: (190, 106, 80), "minecraft:cauldron": (60, 60, 66), "minecraft:anvil": (70, 70, 74),
             "minecraft:lectern": (150, 110, 60), GLOEIKOOL: (255, 120, 40), "guhs:tekentafel": (170, 130, 80)}
    for m in MACHINES:
        kleur[m] = (236, 130, 170)
    wr.SPECIAL_COLOURS.update(kleur)
    wr.block_colour.cache_clear()
    os.makedirs(out, exist_ok=True)
    s, problems = toren(_H(), wezens=False)
    for p in problems:
        print("PROBLEM:", p)
    for k in range(4):
        wr.render_structure(gedraaid(s, k), {}, px=14, max_size=1500).save(os.path.join(out, f"toren_{k}.png"))
    wr.render_structure(doorsnede(s, "z", CZ), {}, px=14, max_size=1500).save(os.path.join(out, "toren_open_z.png"))
    wr.render_structure(doorsnede(gedraaid(s, 1), "z", SIZE[0] - 1 - CX), {}, px=14, max_size=1500).save(os.path.join(out, "toren_open_x.png"))
    W, H, D = s.size
    for naam, y0 in (("hal", V0), ("v1", V1), ("v2", V2), ("v3", V3), ("dak", DAK)):
        plan = Image.new("RGB", (W * 16, D * 16), (20, 16, 18))
        for (x, y, z), b in sorted(s.blocks.items(), key=lambda t: t[0][1]):
            if b[0] in (AIR, "minecraft:jigsaw") or y < y0 or y > y0 + 2:
                continue
            c = wr.block_colour(b[0])
            if c:
                k = 1.0 if y == y0 else 0.85
                d = (y - y0) * 3
                plan.paste(tuple(int(v * k) for v in c), (x * 16 + d, z * 16 + d, x * 16 + 16 - d, z * 16 + 16 - d))
        plan.save(os.path.join(out, f"plan_{naam}.png"))
    print("size", s.size, "blocks", sum(1 for b in s.blocks.values() if b[0] != AIR))


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
