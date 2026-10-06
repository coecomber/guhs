"""
bbq2 (toren-peper) - the two buildings: de Rookguh-vuurtoren (structure guhs:rookguh_vuurtoren) and de Pepertuin met kas
(structure guhs:pepertuin). Each is one template on a cave floor of the Guhbarbecuether (type guhs:barbecueput through
wereld.bbq_structuur), x to the east, z to the south, ground layer G.

De Rookguh-vuurtoren:
  - the TOWER: a slim octagonal lighthouse (7 wide, 28 high) in red and white bands on a dark plinth, with a gallery, a
    glass lantern room around the lamp (block guhs:torenpeper_vuurtorenlamp) and a copper cap with two pink guh ears. Inside
    a spiral of half steps winds round a pillar (4 blocks per turn), from the door in the south up to the lantern room.
  - the keeper's COTTAGE against its east side, the forecourt with the Torenwachter-guh, the foghorn and the knabbelbak
    the Rookguhs come home to.
De Pepertuin:
  - the KAS: a glasshouse (11 x 17) with a gabled glass roof. West of its aisle the three kweekbakken (block
    guhs:torenpeper_kweekbak, one per soil: every player grows their own plant in them), east of it show beds with real
    pepper plants on the three soils, and at the far end the brewing corner with a Guhbrouwketel.
  - the GARDEN east of it: three open beds, a guh scarecrow, a compost heap, a low wall with a gate; the Peperteler-guh
    sits on the path in front of the kas.
The geometry the Java side needs (feature/torenpeper/Vuurtoren.java and Pepertuin.java) is in PLEKKEN_TOREN / PLEKKEN_TUIN;
the module's self-check compares it with the constants in those files.

  toren(h) / tuin(h)   -> (Structure, problems)   a template with its entities (not saved)
  preview(out)         (python tools/features/toren_peper_bouw.py <out>) pictures of both from four sides, cut open, and plans
"""
import math
import os
import random
import sys

G = 3                                   # the ground layer of both templates (what you walk on is G + 1)

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
NYLIUM = "guhs:pindasaus_nylium"
SCHEUTJES = "guhs:pindascheutjes"
AS = "guhs:as_blok"
AS_AARDE = "guhs:as_aarde"
KNABBELBLOK = "guhs:block_of_kaasknabbels"
KETEL = "guhs:guhbrouwketel"
LAMP = "guhs:torenpeper_vuurtorenlamp"
KWEEKBAK = "guhs:torenpeper_kweekbak"
PEPERPLANT = "guhs:torenpeper_peperplant"
WIT = "minecraft:white_concrete"
ROOD = "minecraft:red_concrete"
GLAS = "minecraft:glass"
KOPER = "minecraft:waxed_cut_copper"
KOPER_PLAAT = "minecraft:waxed_cut_copper_slab"
KOPER_TRAP = "minecraft:waxed_cut_copper_stairs"
OOR = "minecraft:pink_terracotta"
OOR_BINNEN = "minecraft:magenta_terracotta"
LANTAARN = "minecraft:lantern"
AIR = "minecraft:air"
SOORTEN = ("groen", "rood", "roze")
GROND_VAN = {"groen": AS_AARDE, "rood": GLOEIKOOL, "roze": NYLIUM}      # the soil that makes each pepper (PeperplantBlock)

# =====================================================================================================================
# the lighthouse: geometry
# =====================================================================================================================
TOREN_SIZE = (33, 33, 33)
TOREN_MIDDEN = "guhs:rookguh_vuurtoren_midden"
TX, TZ = 15, 13                          # the tower's axis
STAPPEN = 38                             # half steps from the floor to the lantern room (19 blocks)
LAMP_Y = G + 21
# the eight sectors round the pillar, clockwise seen from above, starting in the south (where the door is)
SECTOREN = [(0, 1), (-1, 1), (-1, 0), (-1, -1), (0, -1), (1, -1), (1, 0), (1, 1)]
BINNEN = [(dx, dz) for dx in range(-2, 3) for dz in range(-2, 3) if not (abs(dx) == 2 and abs(dz) == 2)]
WAND = [(dx, dz) for dx in range(-3, 4) for dz in range(-3, 4)
        if (max(abs(dx), abs(dz)) == 3 and min(abs(dx), abs(dz)) <= 1) or (abs(dx) == 2 and abs(dz) == 2)]

PLEKKEN_TOREN = {
    "NPC": (15, G + 1, 20),                                     # the Torenwachter-guh, on the forecourt, looks south
    "LAMP": (TX, LAMP_Y, TZ),                                   # the lamp in the lantern room
    "BAK": (TX, G + 20, TZ + 4),                                # on the gallery, in front of the lantern room's door
    "DEUR": (TX, G + 1, TZ + 4),                                # just outside the tower door
}
NPC_YAW_TOREN = 0.0                                             # south

# =====================================================================================================================
# the pepper garden: geometry
# =====================================================================================================================
TUIN_SIZE = (31, 15, 27)
TUIN_MIDDEN = "guhs:pepertuin_midden"
KX0, KX1, KZ0, KZ1 = 4, 14, 5, 21         # the kas (outer walls)
NOK = 9                                   # the x of the ridge and of the aisle

PLEKKEN_TUIN = {
    "NPC": (11, G + 1, 23),                                     # the Peperteler-guh, in front of the kas, looks south
    "BAKKEN": [(7, G + 1, 17), (7, G + 1, 13), (7, G + 1, 9)],  # the kweekbakken: groen, rood, roze
    "KETEL": (9, G + 1, 6),                                     # the Guhbrouwketel of the brewing corner
}
NPC_YAW_TUIN = 0.0


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


def sector(dx, dz):
    """The sector (0..7, see SECTOREN) of a cell inside the tower, relative to its axis."""
    return SECTOREN.index(((dx > 0) - (dx < 0), (dz > 0) - (dz < 0)))


def rand(cells, extra=()):
    """The cells next to (4 neighbours) but not in `cells` (nor in `extra`)."""
    s = set(cells) | set(extra)
    return sorted({(x + dx, z + dz) for x, z in cells for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))} - s)


class Bouw:
    def __init__(self, h, size, seed):
        self.h = h
        self.s = h.Structure(size)
        self.rng = random.Random(seed)
        self.W, self.H, self.D = size
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

    def lucht(self, x, y, z):
        if self.get(x, y, z) is None:
            self.set(x, y, z, AIR)

    # --- shared pieces ------------------------------------------------------------------------------------------------
    def grond(self, vast=()):
        """A frayed oval plate of charred ground with rock under it, and air above (so the cave is open). `vast`: boxes
        (x0, z0, x1, z1) that are always part of the plate."""
        cx, cz = (self.W - 1) / 2, (self.D - 1) / 2
        for x in range(self.W):
            for z in range(self.D):
                d = math.hypot((x - cx) / (self.W / 2 - 0.3), (z - cz) / (self.D / 2 - 0.3))
                binnen = any(x0 <= x <= x1 and z0 <= z <= z1 for x0, z0, x1, z1 in vast)
                if d > 1.0 + self.rng.uniform(-0.06, 0.03) and not binnen:
                    continue
                self.plaat.add((x, z))
                self.set(x, G, z, self.rng.choice([HOUTSKOOL] * 5 + [AS_AARDE] * 2 + [AS] + [GEBARSTEN]))
                for y in range(0, G):
                    self.set(x, y, z, HOUTSKOOL)
        self.s.clear_above(self.plaat, G + 1)

    def pad(self, cells):
        for x, z in cells:
            if (x, z) in self.plaat:
                self.set(x, G, z, self.rng.choice([STENEN] * 5 + [GEBARSTEN] * 2 + [GEBEITELD]))

    def lantaarnpaal(self, x, z, hoog=3):
        for y in range(G + 1, G + 1 + hoog):
            self.set(x, y, z, MUUR)
        self.set(x, G + 1 + hoog, z, LANTAARN, {"hanging": "false", "waterlogged": "false"})

    def bord(self, x, y, z, facing, regels, prefix="sign.guhs.toren_peper"):
        import sign_text
        B = self.h.Byte
        leeg = sign_text.messages(prefix, ["", "", "", ""])
        nbt = {"id": "minecraft:sign", "is_waxed": B(1),
               "front_text": {"messages": sign_text.messages(prefix, regels), "color": "yellow", "has_glowing_text": B(1)},
               "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}}
        self.set(x, y, z, "crimson_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)

    def midden(self, x, z, naam):
        """The centre jigsaw, in layer 0: what the structure type puts on the cave floor. The start pool says where the
        ground really is (ground_level_delta = G + 1, set by toren_peper.structuren, like the fossiel-mijn slice does): the
        ground layer lands on the cave floor and the terrain is smoothed towards it, not towards the bottom of the rock."""
        self.s.set(x, 0, z, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": naam, "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": HOUTSKOOL, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})

    def verbind(self):
        """Walls, fences and bars get the connections their neighbours give them (a jigsaw piece is placed with the states
        exactly as they are in the template: nothing connects by itself)."""
        blocks = self.s.blocks

        def naam(c):
            b = blocks.get(c)
            return b[0] if b else None

        def vol(n):
            return n is not None and n != AIR and n not in DUN and not any(t in n for t in ("_plaat", "_slab", "_trap", "_stairs", "sign", "banner",
                                                                                            "lantern", "kweekbak", "campfire", "guhbrouwketel"))
        for (x, y, z), (name, props, nbt) in list(blocks.items()):
            muur, hek, tralies = name == MUUR, name == HEK, name == TRALIES
            if not (muur or hek or tralies):
                continue
            kant = {}
            for richting, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0))):
                n = naam((x + dx, y, z + dz))
                kant[richting] = vol(n) or n == name or (n in (MUUR, HEK, TRALIES) and (muur or tralies))
            if muur:
                boven = naam((x, y + 1, z))
                recht = (kant["north"] and kant["south"] and not kant["east"] and not kant["west"]) or \
                        (kant["east"] and kant["west"] and not kant["north"] and not kant["south"])
                up = not recht or boven not in (None, AIR)
                p = {k: ("low" if v else "none") for k, v in kant.items()}
                p.update(up="true" if up else "false", waterlogged="false")
            else:
                p = {k: ("true" if v else "false") for k, v in kant.items()}
                p["waterlogged"] = "false"
            blocks[(x, y, z)] = (name, p, nbt)


DUN = (SCHEUTJES, PEPERPLANT, LANTAARN, AIR)


# =====================================================================================================================
# the lighthouse
# =====================================================================================================================
def trede(n):
    """Half step n of the spiral (1..): (y, slab type) of its slab; the top of step n is at G + 1 + n / 2."""
    if n % 2:
        return G + 1 + (n - 1) // 2, "bottom"
    return G + n // 2, "top"


class Toren(Bouw):
    def __init__(self, h):
        super().__init__(h, TOREN_SIZE, 21301401)

    def cel(self, dx, dz):
        return TX + dx, TZ + dz

    def schacht(self):
        """The shaft: plinth, red and white bands, the windows, the door, the pillar and the spiral inside."""
        wand = [self.cel(*c) for c in WAND]
        binnen = [self.cel(*c) for c in BINNEN]
        # room for the whole tower, whatever the cave's ceiling does: a column of air around it
        for x in range(self.W):
            for z in range(self.D):
                d = math.hypot(x - TX, z - TZ)
                if d <= 7.5:
                    for y in range(G + 1, self.H):
                        self.lucht(x, y, z)
                elif d <= 9.5:
                    for y in range(G + 14, self.H):
                        self.lucht(x, y, z)
        for x, z in binnen + wand:
            self.set(x, G, z, STENEN)
            self.plaat.add((x, z))
        for y in range(G + 1, G + 20):
            band = (y - (G + 4)) // 3
            steen = STENEN if y <= G + 3 else (WIT if band % 2 == 0 else ROOD)
            for x, z in wand:
                self.set(x, y, z, GEBEITELD if y == G + 3 else steen)
            for x, z in binnen:
                self.set(x, y, z, AIR)
        # the plinth's skirt: a ring of stone with a slab on it
        rok = rand(wand, binnen)
        for x, z in rok:
            self.set(x, G, z, STENEN)
            self.set(x, G + 1, z, STENEN)
            self.set(x, G + 2, z, PLAAT, {"type": "bottom", "waterlogged": "false"})
            self.plaat.add((x, z))
        # the door (south): two high under a chiselled lintel, the skirt opened in front of it
        for y in (G + 1, G + 2):
            self.set(TX, y, TZ + 3, AIR)
            self.set(TX, y, TZ + 4, AIR)
        self.set(TX, G + 3, TZ + 3, GEBEITELD)
        for dx in (-1, 1):
            self.set(TX + dx, G + 2, TZ + 4, STENEN)
            self.set(TX + dx, G + 3, TZ + 4, LANTAARN, {"hanging": "false", "waterlogged": "false"})
        # windows: one glass block in the middle of a side, where the stairs pass
        for (dx, dz), ys in (((3, 0), (G + 6, G + 11, G + 16)), ((-3, 0), (G + 5, G + 10, G + 14)), ((0, -3), (G + 7, G + 12, G + 17)),
                             ((0, 3), (G + 8, G + 13))):
            for y in ys:
                self.set(TX + dx, y, TZ + dz, GLAS)
        # the pillar, with a light in it every turn
        for y in range(G + 1, LAMP_Y):
            self.set(TX, y, TZ, UIENLICHT if (y - G) % 4 == 0 and y < G + 19 else PILAAR, None if (y - G) % 4 == 0 and y < G + 19 else {"axis": "y"})
        # the spiral: one half step per sector; the last three sectors (east, south-east, south) are one flat landing, the
        # floor of the lantern room (a fourth would hang too low over the steps under it)
        for n in range(1, STAPPEN + 3):
            y, soort = trede(min(n, STAPPEN))
            k = n % 8
            for dx, dz in BINNEN:
                if (dx, dz) != (0, 0) and sector(dx, dz) == k:
                    self.set(TX + dx, y, TZ + dz, PLAAT, {"type": soort, "waterlogged": "false"})

    def top(self):
        """The gallery, the lantern room with the lamp and the copper cap with two guh ears."""
        wand = [self.cel(*c) for c in WAND]
        binnen = [self.cel(*c) for c in BINNEN]
        ring1 = rand(wand, binnen)
        ring2 = rand(ring1, wand + binnen)
        dek_y = G + 19
        for x, z in ring1:
            self.set(x, dek_y - 1, z, PLAAT, {"type": "top", "waterlogged": "false"})          # the corbel under the deck
        for x, z in ring1 + ring2:
            self.set(x, dek_y, z, IJZER)
        for x, z in ring2:
            self.set(x, dek_y + 1, z, MUUR)                                                     # the railing
        # the lantern room: glass between four iron posts, the door to the gallery in the south
        for x, z in wand:
            paal = abs(x - TX) == 2 and abs(z - TZ) == 2
            for y in range(dek_y + 1, dek_y + 4):
                self.set(x, y, z, PILAAR if paal else GLAS, {"axis": "y"} if paal else None)
        for x, z in binnen:
            for y in range(dek_y + 1, dek_y + 4):
                self.set(x, y, z, AIR)
        self.set(TX, dek_y + 1, TZ + 3, AIR)
        self.set(TX, dek_y + 2, TZ + 3, AIR)
        self.set(TX, dek_y + 3, TZ + 3, IJZER)
        # the lamp on the pillar, a chain up to the cap
        self.set(TX, dek_y + 1, TZ, IJZER)
        x, y, z = PLEKKEN_TOREN["LAMP"]
        self.set(x, y, z, LAMP, {"lit": "false"}, {"id": "guhs:torenpeper_vuurtorenlamp"})
        self.set(TX, dek_y + 3, TZ, "chain", {"axis": "y", "waterlogged": "false"})
        # the cap: an eave of copper slabs, four shrinking layers, a glowing finial and a rod
        y = dek_y + 4
        for x, z in ring1:
            self.set(x, y, z, KOPER_PLAAT, {"type": "bottom", "waterlogged": "false"})
        for x, z in wand + binnen:
            self.set(x, y, z, KOPER)
        for x, z in binnen:
            self.set(x, y + 1, z, KOPER)
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                self.set(TX + dx, y + 2, TZ + dz, KOPER)
        self.set(TX, y + 3, TZ, KOPER)
        self.set(TX, y + 4, TZ, GLOEIKOOL)
        self.set(TX, y + 5, TZ, "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
        for sx in (-1, 1):
            self.set(TX + sx * 2, y + 2, TZ, OOR)
            self.set(TX + sx * 2, y + 3, TZ, OOR)
            self.set(TX + sx * 2, y + 2, TZ + 1, OOR_BINNEN)
        # the knabbelbak the Rookguhs come home to: blocks of kaasknabbels in the railing, left and right of the door
        x, y, z = PLEKKEN_TOREN["BAK"]
        for dx in (-2, 2):
            self.set(x + dx, y, z, KNABBELBLOK)
        for dx, dz in ((-4, -4), (4, -4), (0, -9), (-5, -4), (5, -4)):
            if self.get(x + dx, y, z + dz) == MUUR:
                self.set(x + dx, y + 1, z + dz, LANTAARN, {"hanging": "false", "waterlogged": "false"})

    def huisje(self):
        """The keeper's cottage against the east side: stone to the sill, mustard plaster, a copper saddle roof (ridge
        along x), a chimney with a smoking fire, a window on each side."""
        x0, x1, z0, z1 = 20, 27, 10, 16
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                self.plaat.add((x, z))
                self.set(x, G, z, STENEN)
                for y in range(G + 1, G + 9):
                    self.set(x, y, z, AIR)
                if x in (x0, x1) or z in (z0, z1):
                    hoek = x in (x0, x1) and z in (z0, z1)
                    self.set(x, G + 1, z, STENEN)
                    for y in (G + 2, G + 3):
                        self.set(x, y, z, SATE if hoek else MOSTERD, {"axis": "y"} if hoek else None)
                    if hoek:
                        self.set(x, G + 1, z, SATE, {"axis": "y"})
        # the roof: stairs up from the north and south eaves to a ridge of slabs, the gables filled in
        for x in range(x0 - 1, x1 + 2):
            for i in range(0, 4):
                for z, facing in ((z0 - 1 + i, "south"), (z1 + 1 - i, "north")):
                    self.set(x, G + 3 + i, z, KOPER_TRAP, {"facing": facing, "half": "bottom", "shape": "straight", "waterlogged": "false"})
            self.set(x, G + 6, (z0 + z1) // 2, KOPER_PLAAT, {"type": "top", "waterlogged": "false"})
        for x in (x0, x1):
            for i in range(1, 3):
                for z in range(z0 + i, z1 - i + 1):
                    self.set(x, G + 3 + i, z, MOSTERD)
        # door (south, towards the forecourt) and windows
        for y in (G + 1, G + 2):
            self.set(22, y, z1, AIR)
        self.set(22, G + 3, z1, SATE, {"axis": "x"})
        for x, z in ((25, z1), (x1, 13), (24, z0)):
            self.set(x, G + 2, z, GLAS)
        self.set(x1, G + 5, 13, GLAS)
        # inside: a bed, a table with a lantern, a chest with odds and ends, a rug, the stove under the chimney
        self.set(26, G + 1, 11, "red_bed", {"facing": "east", "part": "head", "occupied": "false"}, {"id": "minecraft:bed"})
        self.set(25, G + 1, 11, "red_bed", {"facing": "east", "part": "foot", "occupied": "false"}, {"id": "minecraft:bed"})
        self.set(26, G + 1, 15, "smoker", {"facing": "west", "lit": "false"}, {"id": "minecraft:smoker"})
        for y in range(G + 2, G + 8):
            self.set(26, y, 15, STENEN)
        self.set(26, G + 8, 15, "campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"},
                 {"id": "minecraft:campfire"})
        self.set(21, G + 1, 11, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
        self.h.ms.chest(self.s, 22, G + 1, 11, "south", "guhs:chests/toren_peper_wachter")
        self.set(24, G + 1, 13, TRAP, {"facing": "north", "half": "top", "shape": "straight", "waterlogged": "false"})    # the table
        self.set(24, G + 2, 13, LANTAARN, {"hanging": "false", "waterlogged": "false"})
        self.set(23, G + 1, 13, "red_carpet")
        self.set(23, G + 1, 14, "white_carpet")
        self.set(21, G + 3, 13, LANTAARN, {"hanging": "true", "waterlogged": "false"})
        self.set(21, G + 4, 13, "chain", {"axis": "y", "waterlogged": "false"})

    def erf(self):
        """The forecourt south of the tower: paved, with the path to the edge of the plate, the name sign, the foghorn on
        its stand, a bench, lantern posts, and coal for the lamp."""
        self.pad([(x, z) for x in range(11, 25) for z in range(17, 24)])
        self.pad([(x, z) for x in range(14, 17) for z in range(24, 33)])
        self.pad([(x, z) for x in range(20, 25) for z in range(17, 19)])
        # the foghorn: a copper horn on an iron stand, pointing south-west into the smoke
        for y in (G + 1, G + 2):
            self.set(10, y, 21, PILAAR, {"axis": "y"})
        self.set(10, G + 3, 21, KOPER)
        self.set(9, G + 3, 22, KOPER_TRAP, {"facing": "east", "half": "top", "shape": "straight", "waterlogged": "false"})
        self.set(9, G + 3, 21, KOPER_TRAP, {"facing": "east", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        self.set(8, G + 3, 21, KOPER_PLAAT, {"type": "top", "waterlogged": "false"})
        self.set(8, G + 4, 21, KOPER_PLAAT, {"type": "bottom", "waterlogged": "false"})
        self.set(8, G + 2, 21, KOPER_PLAAT, {"type": "top", "waterlogged": "false"})
        # a bench next to the keeper, and his bucket of coals
        for x in (17, 18):
            self.set(x, G + 1, 20, TRAP, {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        self.set(19, G + 1, 20, "cauldron")
        self.set(13, G + 1, 19, GLOEIKOOL)
        self.set(12, G + 1, 19, "guhs:grillkool")
        self.set(12, G + 1, 18, "guhs:grillkool")
        self.set(12, G + 2, 19, "guhs:smeulkooltjes")
        self.lantaarnpaal(24, 20)
        # a low quay wall along the south of the forecourt, open where the path comes in, a lantern on each end
        for x in list(range(9, 14)) + list(range(17, 25)):
            self.set(x, G, 24, STENEN)
            self.set(x, G + 1, 24, MUUR)
        for x in (9, 13, 17, 24):
            self.set(x, G + 2, 24, LANTAARN, {"hanging": "false", "waterlogged": "false"})
        # the entrance: two posts with a beam and the name
        for x in (13, 17):
            for y in range(G + 1, G + 5):
                self.set(x, y, 28, SATE, {"axis": "y"})
        for x in range(13, 18):
            self.set(x, G + 5, 28, SATE, {"axis": "x"})
        self.set(14, G + 6, 28, OOR)
        self.set(16, G + 6, 28, OOR)
        self.set(15, G + 4, 28, LANTAARN, {"hanging": "true", "waterlogged": "false"})
        self.bord(15, G + 5, 29, "south", ["~ Rookguh-vuurtoren ~", "Licht aan =", "Rookguhs thuis.", "Njeg!"])
        self.bord(TX + 1, G + 2, TZ + 5, "south", ["Lamp kapot?", "Vraag het de", "Torenwachter-guh", ""])
        # a little skiff on its side for when the sauce rises (it never does): two slabs and a mast
        for x in (4, 5, 6):
            self.set(x, G + 1, 26, PLAAT, {"type": "bottom", "waterlogged": "false"})
        self.set(3, G + 1, 26, TRAP, {"facing": "east", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        self.set(7, G + 1, 26, TRAP, {"facing": "west", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        self.set(5, G + 2, 26, MUUR)
        self.set(5, G + 3, 26, "white_banner", {"rotation": "4"})


def toren(h, wezens=True):
    """The lighthouse's template (not saved) and the problems of its self-check."""
    b = Toren(h)
    b.grond(vast=[(TX - 5, TZ - 5, TX + 5, TZ + 5), (19, 9, 28, 17)])
    b.erf()
    b.schacht()
    b.huisje()
    b.top()
    b.midden(16, 19, TOREN_MIDDEN)
    b.verbind()
    if wezens:
        from features import wereld
        x, y, z = PLEKKEN_TOREN["NPC"]
        b.s.entity(x + 0.5, y, z + 0.5, wereld.npc(h, "torenwachterguh", "torenpeper_torenwachter", yaw=NPC_YAW_TOREN))
    return b.s, check_toren(b.s)


# =====================================================================================================================
# the pepper garden
# =====================================================================================================================
def dak_y(x):
    """The glass roof of the kas over column x (KX0..KX1): eaves at G + 5, one up per block to the ridge."""
    return G + 5 + min(x - KX0, KX1 - x)


class Tuin(Bouw):
    def __init__(self, h):
        super().__init__(h, TUIN_SIZE, 21301411)

    def plant(self, x, y, z, soort, age):
        self.set(x, y, z, PEPERPLANT, {"age": str(age), "soort": soort})

    def kas(self):
        for x in range(KX0, KX1 + 1):
            for z in range(KZ0, KZ1 + 1):
                self.plaat.add((x, z))
                self.set(x, G, z, STENEN if (x + z) % 3 else GEBARSTEN)
                for y in range(G + 1, dak_y(x) + 1):
                    self.set(x, y, z, AIR)
        # walls: a stone sill, glass between saté posts, a beam on top
        for x in range(KX0, KX1 + 1):
            for z in range(KZ0, KZ1 + 1):
                rand_x, rand_z = x in (KX0, KX1), z in (KZ0, KZ1)
                if not (rand_x or rand_z):
                    continue
                paal = (rand_x and (z - KZ0) % 4 == 0) or (rand_z and x in (KX0, KX1, NOK - 2, NOK + 2))     # (NOK +- 2: the door posts)
                self.set(x, G + 1, z, STENEN)
                top = G + 4 if rand_x else dak_y(x) - 1
                for y in range(G + 2, top + 1):
                    self.set(x, y, z, SATE if paal else GLAS, {"axis": "y"} if paal else None)
        # the roof: one glass block per column, a saté ridge beam with lights under it
        for z in range(KZ0, KZ1 + 1):
            for x in range(KX0, KX1 + 1):
                self.set(x, dak_y(x), z, SATE if x == NOK else GLAS, {"axis": "z"} if x == NOK else None)
            for x in (KX0, KX1):
                self.set(x, G + 5, z, SATE, {"axis": "z"})
        for z in (KZ0 + 3, KZ0 + 8, KZ0 + 13):
            self.set(NOK, dak_y(NOK) - 1, z, UIENLICHT)
        # the door (south): as wide as the aisle, three high, under a beam
        for x in range(NOK - 1, NOK + 2):
            for y in range(G + 1, G + 4):
                self.set(x, y, KZ1, AIR)
        for x in (NOK - 2, NOK + 2):
            self.set(x, G + 1, KZ1, SATE, {"axis": "y"})
        for x in range(NOK - 2, NOK + 3):
            self.set(x, G + 4, KZ1, SATE, {"axis": "x"})
        # west of the aisle: the three kweekbakken, each in front of a post with its sign, between low benches of plants
        namen = {"groen": ["Njegpeper", "groeit op", "as-aarde", ""], "rood": ["Vahoegpeper", "groeit op", "gloeikool.", "Heet!"],
                 "roze": ["Snoeppeper", "groeit op", "pindasaus-", "nylium. Zoet!"]}
        for soort, (x, y, z) in zip(SOORTEN, PLEKKEN_TUIN["BAKKEN"]):
            self.set(x, y, z, KWEEKBAK, {"soort": soort, "groei": "0"}, {"id": "guhs:torenpeper_kweekbak"})
            for yy in (G + 1, G + 2):
                self.set(x - 1, yy, z, SATE, {"axis": "y"})
            self.bord(x - 1, G + 2, z + 1, "south", namen[soort])
            self.set(x - 1, G + 3, z, LANTAARN, {"hanging": "false", "waterlogged": "false"})
            self.set(x - 2, G + 1, z, GROND_VAN[soort])                     # (a block of its soil against the glass, to show)
            self.set(x, G + 1, z - 1, PLAAT, {"type": "bottom", "waterlogged": "false"})
            self.set(x, G + 1, z + 1, PLAAT, {"type": "bottom", "waterlogged": "false"})
        # east of the aisle: three raised show beds of real pepper plants on their soils (anyone may pick the ripe ones)
        for i, soort in enumerate(SOORTEN):
            z0 = 16 - i * 4
            for x in (11, 12):
                for z in range(z0, z0 + 3):
                    self.set(x, G + 1, z, GROND_VAN[soort])
                    self.plant(x, G + 2, z, soort, 3 if (x + z) % 2 else 1 + (z % 2))
            for z in range(z0, z0 + 3):
                self.set(13, G + 1, z, STENEN)
            self.set(13, G + 2, z0 + 1, LANTAARN, {"hanging": "false", "waterlogged": "false"})
        for z in (11, 15):
            for x in (11, 12):
                self.set(x, G + 1, z, STENEN)
        # the brewing corner at the far end: the ketel, a barrel, the chest, a shelf of bottles
        x, y, z = PLEKKEN_TUIN["KETEL"]
        self.set(x, y, z, KETEL, {"facing": "south", "lit": "false", "borrelt": "false", "vulling": "0", "brouwsel": "0"}, {"id": "guhs:guhbrouwketel"})
        self.set(x - 2, y, z, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
        self.set(x - 3, y, z, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
        self.set(x - 3, y + 1, z, "barrel", {"facing": "south", "open": "false"}, {"id": "minecraft:barrel"})
        self.h.ms.chest(self.s, x + 2, y, z, "south", "guhs:chests/toren_peper_kas")
        self.set(x + 3, y, z, "cauldron")
        self.set(x + 4, y, z, "composter", {"level": "3"})
        self.bord(x + 1, G + 2, KZ0 + 1, "south", ["Brouwhoek", "Peper + bouillon", "= vuur in je buik", "Njeg!"])
        self.set(x + 1, G + 2, KZ0, SATE, {"axis": "y"})
        self.set(x + 1, G + 3, KZ0, SATE, {"axis": "y"})

    def tuin(self):
        """East of the kas: three open beds, a guh scarecrow, a compost heap, a wall with a gate, the path."""
        self.pad([(x, z) for x in range(8, 13) for z in range(22, 27)])
        self.pad([(x, z) for x in range(13, 28) for z in range(22, 24)])
        self.pad([(x, z) for x in range(15, 17) for z in range(6, 22)])
        for i, soort in enumerate(SOORTEN):
            x0 = 18 + i * 3
            for x in (x0, x0 + 1):
                for z in range(8, 19):
                    if (x, z) in self.plaat:
                        self.set(x, G, z, GROND_VAN[soort])
                        if (x * 3 + z) % 4 != 0:
                            self.plant(x, G + 1, z, soort, (x + z * 2) % 4)
        # the wall round the garden, with lanterns on its corners and a gate in the south
        for x in range(17, 28):
            for z in (6, 20):
                if (x, z) in self.plaat and not (z == 20 and x in (22, 23)):
                    self.set(x, G + 1, z, MUUR)
        for z in range(6, 21):
            if (27, z) in self.plaat:
                self.set(27, G + 1, z, MUUR)
        for x, z in ((17, 20), (27, 20), (27, 6), (17, 6), (21, 20), (24, 20)):
            if (x, z) in self.plaat:
                self.set(x, G + 1, z, STENEN)
                self.set(x, G + 2, z, LANTAARN, {"hanging": "false", "waterlogged": "false"})
        self.pad([(x, z) for x in (22, 23) for z in (19, 20, 21)])
        # the scarecrow: a guh on a stick with a straw body, to keep the Mika's off the peppers
        for y in (G + 1, G + 2):
            self.set(17, y, 13, SATE, {"axis": "y"})
        self.set(17, G + 3, 13, "hay_block", {"axis": "y"})
        for z in (12, 13, 14):
            self.set(17, G + 4, z, "pink_wool")
        self.set(17, G + 5, 12, OOR)
        self.set(17, G + 5, 14, OOR)
        self.set(17, G + 3, 12, HEK)
        self.set(17, G + 3, 14, HEK)
        self.bord(18, G + 4, 13, "east", ["", "Njeg!", "", ""])
        # compost, a water tub, baskets of the harvest
        self.set(25, G + 1, 21, "composter", {"level": "6"})
        self.set(26, G + 1, 21, "composter", {"level": "2"})
        self.set(18, G + 1, 21, "cauldron")
        self.set(19, G + 1, 21, "hay_block", {"axis": "y"})
        self.set(14, G + 1, 22, "red_glazed_terracotta", {"facing": "south"})
        self.set(13, G + 1, 24, "lime_glazed_terracotta", {"facing": "east"})
        # the entrance: an arch over the path with a pepper-red banner and the name
        for x in (8, 12):
            for y in range(G + 1, G + 5):
                self.set(x, y, 25, SATE, {"axis": "y"})
        for x in range(8, 13):
            self.set(x, G + 5, 25, SATE, {"axis": "x"})
        self.set(9, G + 6, 25, OOR)
        self.set(11, G + 6, 25, OOR)
        self.set(10, G + 4, 25, LANTAARN, {"hanging": "true", "waterlogged": "false"})
        self.bord(10, G + 5, 26, "south", ["~ De Pepertuin ~", "Kas van de", "Peperteler-guh", "Heet & zoet!"])
        self.lantaarnpaal(13, 25)


def tuin(h, wezens=True):
    """The pepper garden's template (not saved) and the problems of its self-check."""
    b = Tuin(h)
    b.grond(vast=[(KX0 - 1, KZ0 - 1, KX1 + 1, KZ1 + 1), (16, 5, 28, 21)])
    b.tuin()
    b.kas()
    b.midden(15, 14, TUIN_MIDDEN)
    b.verbind()
    if wezens:
        from features import wereld
        x, y, z = PLEKKEN_TUIN["NPC"]
        b.s.entity(x + 0.5, y, z + 0.5, wereld.npc(h, "pepertelerguh", "torenpeper_peperteler", yaw=NPC_YAW_TUIN))
    return b.s, check_tuin(b.s)


# =====================================================================================================================
# the self-checks
# =====================================================================================================================
LOOPT_DOOR = (AIR, SCHEUTJES, PEPERPLANT, "minecraft:red_carpet", "minecraft:white_carpet", "guhs:smeulkooltjes")


def _vast(blocks):
    """The solid half blocks of a template: {(x, half y, z)} (half y = 2 * y, + 1 for the upper half)."""
    vol = set()
    for (x, y, z), b in blocks.items():
        n, p = b[0], b[1]
        if n in LOOPT_DOOR or "sign" in n or "banner" in n:
            continue
        if n.endswith("_plaat") or n.endswith("_slab"):
            if p.get("type") == "top":
                vol.add((x, 2 * y + 1, z))
            elif p.get("type") == "bottom":
                vol.add((x, 2 * y, z))
            else:
                vol.update(((x, 2 * y, z), (x, 2 * y + 1, z)))
        elif n == KWEEKBAK or n == "minecraft:red_bed":
            vol.add((x, 2 * y, z))
        else:
            vol.update(((x, 2 * y, z), (x, 2 * y + 1, z)))
    return vol


def bereikbaar(blocks, start, hoog=4):
    """Every spot a player can walk to from `start` (x, y, z: feet on y): {(x, half y, z)} of the feet. A step is at most
    half a block up or three blocks down, into a column with two blocks of room."""
    vol = _vast(blocks)

    def staat(x, hy, z):
        return (x, hy - 1, z) in vol and all((x, hy + i, z) not in vol for i in range(hoog))

    s = (start[0], 2 * start[1], start[2])
    if not staat(*s):
        return set()
    gezien, todo = {s}, [s]
    while todo:
        x, hy, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dh in (1, 0, -1, -2, -3, -4, -5, -6):
                n = (x + dx, hy + dh, z + dz)
                if n not in gezien and staat(*n) and (dh <= 0 or all((x, hy + hoog + i, z) not in vol for i in range(dh))):
                    gezien.add(n)
                    todo.append(n)
                    break
    return gezien


def _zweeft(blocks, problems):
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
        problems.append(f"floating block {blocks[c][0]} at {c}")


def _staat_op(blocks, problems):
    def nm(c):
        b = blocks.get(c)
        return b[0] if b else None

    for c, b in blocks.items():
        below = nm((c[0], c[1] - 1, c[2]))
        if b[0] in ("minecraft:chest", "minecraft:barrel", "minecraft:cauldron", "minecraft:composter", KWEEKBAK, KETEL, SCHEUTJES, PEPERPLANT) or \
                (b[0] == LANTAARN and b[1].get("hanging") == "false"):
            if below in (None, AIR) or below in DUN:
                problems.append(f"{b[0]} at {c} stands on nothing")
        if b[0] == LANTAARN and b[1].get("hanging") == "true" and nm((c[0], c[1] + 1, c[2])) in (None, AIR):
            problems.append(f"the hanging lantern at {c} hangs from nothing")
        if b[0] == PEPERPLANT and below != GROND_VAN[b[1]["soort"]]:
            problems.append(f"the {b[1]['soort']} pepper plant at {c} stands on {below}")
        if b[0] in (MUUR, HEK, TRALIES) and "north" not in b[1]:
            problems.append(f"{b[0]} at {c} has no connections")


def _zit(blocks, plek, wie, problems):
    def nm(c):
        b = blocks.get(c)
        return b[0] if b else None

    x, y, z = plek
    if nm((x, y - 1, z)) in (None, AIR) or nm((x, y, z)) != AIR or nm((x, y + 1, z)) != AIR:
        problems.append(f"{wie} has no place to sit at {plek}")


def check_toren(s):
    problems = []
    blocks = s.blocks
    _zweeft(blocks, problems)
    _staat_op(blocks, problems)
    _zit(blocks, PLEKKEN_TOREN["NPC"], "the Torenwachter-guh", problems)
    x, y, z = PLEKKEN_TOREN["LAMP"]
    if s.get(x, y, z) != LAMP:
        problems.append(f"no lamp at {PLEKKEN_TOREN['LAMP']}")
    # from the keeper you can walk in through the door, up the spiral, round the lamp and out onto the gallery
    loop = bereikbaar(blocks, PLEKKEN_TOREN["NPC"])
    feet = 2 * (LAMP_Y - 1)
    if not any((TX + dx, feet, TZ + dz) in loop for dx, dz in ((0, 1), (1, 0), (1, 1), (-1, 1))):
        problems.append("the lantern room can't be reached over the stairs")
    bx, by, bz = PLEKKEN_TOREN["BAK"]
    if (bx, 2 * by, bz) not in loop:
        problems.append(f"the gallery at {PLEKKEN_TOREN['BAK']} can't be reached")
    dx, dy, dz = PLEKKEN_TOREN["DEUR"]
    if (dx, 2 * dy, dz) not in loop:
        problems.append("the tower door can't be reached from the forecourt")
    if not any(k[0] == 22 and k[2] == 13 for k in loop):
        problems.append("the cottage can't be entered")
    # nobody falls off the gallery: every spot of the deck's rim has a railing
    for (x, hy, z) in loop:
        if hy == 2 * (G + 20) and math.hypot(x - TX, z - TZ) > 3.9:
            for ddx, ddz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + ddx, G + 20, z + ddz)
                if s.get(*n) in (None, AIR) and s.get(n[0], G + 19, n[2]) in (None, AIR):
                    problems.append(f"the gallery is open at {n}")
    # next to the gallery there is always room for a lost Rookguh (Vuurtoren.startplek falls back on it)
    for hoek in range(0, 360, 15):
        x, z = TX + 0.5 + math.cos(math.radians(hoek)) * 7.0, TZ + 0.5 + math.sin(math.radians(hoek)) * 7.0
        for cx in (int(x - 0.9), int(x + 0.9)):
            for cz in (int(z - 0.9), int(z + 0.9)):
                for cy in (LAMP_Y - 2, LAMP_Y - 1, LAMP_Y):
                    if s.get(cx, cy, cz) != AIR:
                        problems.append(f"no room next to the gallery at {(cx, cy, cz)}: {s.get(cx, cy, cz)}")
    if not any(b[0] == "minecraft:jigsaw" and b[2] and b[2].get("name") == TOREN_MIDDEN and c[1] == 0 for c, b in blocks.items()):
        problems.append("no centre jigsaw in layer 0")
    return problems


def check_tuin(s):
    problems = []
    blocks = s.blocks
    _zweeft(blocks, problems)
    _staat_op(blocks, problems)
    _zit(blocks, PLEKKEN_TUIN["NPC"], "the Peperteler-guh", problems)
    loop = bereikbaar(blocks, PLEKKEN_TUIN["NPC"])
    for soort, (x, y, z) in zip(SOORTEN, PLEKKEN_TUIN["BAKKEN"]):
        b = blocks.get((x, y, z))
        if not b or b[0] != KWEEKBAK or b[1].get("soort") != soort:
            problems.append(f"no kweekbak for {soort} at {(x, y, z)}")
        if (x + 1, 2 * y, z) not in loop:
            problems.append(f"the kweekbak at {(x, y, z)} can't be reached from the aisle")
        # (its plant is drawn in the block above it)
        if s.get(x, y + 1, z) != AIR:
            problems.append(f"no room for a plant above the kweekbak at {(x, y, z)}")
    x, y, z = PLEKKEN_TUIN["KETEL"]
    if s.get(x, y, z) != KETEL or (x, 2 * y, z + 1) not in loop:
        problems.append("the Guhbrouwketel is not where it should be, or can't be reached")
    # every plant inside the kas has glass over it (it grows fast there), and can be reached to pick
    for (x, y, z), b in blocks.items():
        if b[0] != PEPERPLANT:
            continue
        in_kas = KX0 < x < KX1 and KZ0 < z < KZ1
        glas = any(s.get(x, yy, z) == GLAS for yy in range(y + 1, y + 9))
        if in_kas != glas:
            problems.append(f"the pepper plant at {(x, y, z)}: in the kas {in_kas}, glass above {glas}")
        if not any((x + dx, 2 * (y - 1) + h, z + dz) in loop for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0)) for h in (0, 1, 2)):
            problems.append(f"the pepper plant at {(x, y, z)} can't be reached")
    for soort in SOORTEN:
        n = sum(1 for b in blocks.values() if b[0] == PEPERPLANT and b[1]["soort"] == soort and b[1]["age"] == "3")
        if n < 3:
            problems.append(f"only {n} ripe {soort} pepper plants")
    if not any(b[0] == "minecraft:jigsaw" and b[2] and b[2].get("name") == TUIN_MIDDEN and c[1] == 0 for c, b in blocks.items()):
        problems.append("no centre jigsaw in layer 0")
    return problems


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
class _H:
    """What the builders need of the make_v2 namespace, for the preview."""
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


def doorsnede(s, as_, waarde):
    """The structure with everything in front of a plane left out (axis "x" or "z"; keeps the part at or behind it)."""
    import make_structures as ms
    t = ms.Structure(s.size)
    i = 0 if as_ == "x" else 2
    t.blocks = {c: b for c, b in s.blocks.items() if c[i] >= waarde}
    return t


def preview(out):
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    from PIL import Image
    kleur = {LANTAARN: (255, 214, 120), SCHEUTJES: (214, 150, 60), KOPER: (196, 110, 78), KOPER_TRAP: (196, 110, 78), KOPER_PLAAT: (196, 110, 78),
             "minecraft:crimson_wall_sign": (126, 58, 86), "minecraft:lightning_rod": (200, 120, 90), HEK: (57, 47, 47), MUUR: (62, 52, 52),
             LAMP: (255, 236, 150), KWEEKBAK: (120, 84, 60), PEPERPLANT: (70, 150, 60), KETEL: (60, 56, 62), "minecraft:chain": (70, 74, 86),
             "minecraft:white_banner": (236, 236, 236), "minecraft:campfire": (240, 140, 40), KNABBELBLOK: (244, 196, 70),
             "minecraft:red_bed": (160, 40, 40), "guhs:smeulkooltjes": (240, 120, 30), "minecraft:composter": (120, 84, 44)}
    wr.SPECIAL_COLOURS.update(kleur)
    wr.block_colour.cache_clear()
    os.makedirs(out, exist_ok=True)
    for naam, maak in (("rookguh_vuurtoren", toren), ("pepertuin", tuin)):
        s, problems = maak(_H(), wezens=False)
        for p in problems:
            print(f"PROBLEM ({naam}):", p)
        for k in range(4):
            wr.render_structure(gedraaid(s, k), {}, px=16, max_size=1600).save(os.path.join(out, f"{naam}_{k}.png"))
        if naam == "rookguh_vuurtoren":
            wr.render_structure(doorsnede(s, "z", TZ), {}, px=16, max_size=1600).save(os.path.join(out, f"{naam}_open.png"))
        else:
            wr.render_structure(doorsnede(s, "z", 13), {}, px=16, max_size=1600).save(os.path.join(out, f"{naam}_open.png"))
            wr.render_structure(doorsnede(gedraaid(s, 1), "z", 21), {}, px=16, max_size=1600).save(os.path.join(out, f"{naam}_open2.png"))
        W, H, D = s.size
        plan = Image.new("RGB", (W * 16, D * 16), (20, 16, 18))
        for (x, y, z), b in sorted(s.blocks.items(), key=lambda t: t[0][1]):
            if b[0] in (AIR, "minecraft:jigsaw") or y < G or y > G + 2:
                continue
            c = wr.block_colour(b[0])
            if c:
                k = 1.0 if y == G else 0.8
                plan.paste(tuple(int(v * k) for v in c), (x * 16 + (y - G) * 2, z * 16 + (y - G) * 2, x * 16 + 16 - (y - G) * 2, z * 16 + 16 - (y - G) * 2))
        plan.save(os.path.join(out, f"{naam}_plan.png"))
        print(naam, "size", s.size, "blocks", sum(1 for b in s.blocks.values() if b[0] != AIR))


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
