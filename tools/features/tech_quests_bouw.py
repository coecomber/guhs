"""
bbq2 (tech-quests): the Oude Guhrad-centrale of features/tech_quests.py, built block by block.

An old power station on a cave floor of the Guhbarbecuether: a brick hall under a barrel vault of weathered copper with a
glazed ridge, a gable with a wheel window over the door, two tall chimneys behind it, pylons with cables on the forecourt
and an old Guhrad sunk into the ash. Inside, from north to south:
  - the radergalerij: five old Guhraden against the north wall, a catwalk and a run of pipes over them;
  - the oefenhal: five bays, one per wheel, each with a practice setup that is broken (the Uitvinder-guh's questline);
  - the cross aisle;
  - west the Uitvinder-guh's workshop, east the bordes of De Grote Knabbelmachine.

ONE template for the type guhs:barbecueput (wereld.bbq_structuur soort "grot"); the centre jigsaw is in layer 0 and the start
pool gets ground_level_delta = G + 1 (tech_quests.structuur), as fossiel-mijn does.

**What is NOT in the template.** A structure start is turned at random, and the Guhrad (a 3 x 3 multi-block), Guhdraad and
the Guh Oven do not turn with a template. So everything of the practice hall that is a guh machine is put down by code
(feature/techquest/Centrale.java richtIn) the first time a player comes near a copy, turned the way the copy is, and put
back in its broken state when the players have walked away. The template only holds the building. PLEKKEN below are those
spots: THE SAME NUMBERS AS Centrale.java (tech_quests.selfcheck compares the two files). voorbeeld(b) puts stand-ins for
them into a copy of the template for the preview pictures only.
"""
import math
import random

NAAM = "oude_guhrad_centrale"
MAAT = (47, 35, 47)
G = 3                       # template y of the top block of the ground
F = G + 1                   # where you walk
CX = 23                     # the middle aisle
X0, X1, Z0, Z1 = 6, 40, 9, 31          # the hall's outer walls
SY = F + 9                  # where the vault springs from the side walls
RISE = 11                   # how high the vault rises above that
RZ = Z0 + 1                 # the row of the wheels (their kern blocks)
NPC = (14, F, 24)
NPC_YAW = -90.0             # (looks east: at the aisle and the Grote Knabbelmachine)
KERN = (28, F + 1, 24)      # the kern of De Grote Knabbelmachine (its mouth), looking west

# --- the spots that Centrale.java furnishes (template coordinates) --------------------------------------------------------
PLEKKEN = {
    "RADEREN": [(11, F, RZ), (17, F, RZ), (23, F, RZ), (29, F, RZ), (35, F, RZ)],
    "S1_DRAAD": [(11, F, RZ + 1), (11, F, RZ + 3)], "S1_GAT": [(11, F, RZ + 2)], "S1_OVEN": [(11, F, RZ + 4)],
    "S2_DRAAD": [(17, F, RZ + 1), (17, F, RZ + 2), (15, F, RZ + 3), (17, F, RZ + 3), (19, F, RZ + 3)],
    "S2_KNIP": [(16, F, RZ + 3), (18, F, RZ + 3)], "S2_OVENS": [(15, F, RZ + 4), (19, F, RZ + 4)], "S2_MOLEN": [(17, F, RZ + 4)],
    "S3_BATTERIJ": [(23, F, RZ + 1)], "S3_VAT_A": [(21, F, RZ + 4)], "S3_STUK": [(22, F, RZ + 4)],
    "S3_BUIS": [(23, F, RZ + 4), (24, F, RZ + 4)], "S3_VAT_B": [(25, F, RZ + 4)],
    "S4_DRAAD": [(29, F, RZ + 1), (29, F, RZ + 2), (29, F, RZ + 3), (28, F, RZ + 3)], "S4_VAT_A": [(27, F, RZ + 4)],
    "S4_FILTER": [(28, F, RZ + 4)], "S4_BUIS": [(29, F, RZ + 4), (30, F, RZ + 4)], "S4_VAT_B": [(31, F, RZ + 4)],
    "S5_DRAAD": [(35, F, RZ + 1)], "S5_BRON": [(35, G, RZ + 2)], "S5_POMP": [(35, F, RZ + 2)], "S5_SLANG": [(35, F, RZ + 3)],
    "S5_GAT": [(35, F, RZ + 4)], "S5_SAUSVAT": [(35, F, RZ + 5)],
    "TEKENTAFEL": [(8, F, 22)],
    "KERN": [KERN], "UITVINDER": [NPC],
}
OPSTELLINGEN = 5

HOUTSKOOL = "guhs:houtskoolsteen"
STENEN = "guhs:houtskoolsteen_stenen"
GEBARSTEN = "guhs:gebarsten_houtskoolsteen_stenen"
GEBEITELD = "guhs:gebeitelde_houtskoolsteen_stenen"
MUUR = "guhs:houtskoolsteen_stenen_muur"
PLAAT = "guhs:houtskoolsteen_stenen_plaat"
TRAP = "guhs:houtskoolsteen_stenen_trap"
ROOSTER = "guhs:roosterijzer"
GEPOLIJST = "guhs:gepolijst_roosterijzer"
PILAAR = "guhs:roosterijzer_pilaar"
TRALIES = "guhs:roosterijzer_tralies"
AS = "guhs:as_blok"
AS_AARDE = "guhs:as_aarde"
SMEUL = "guhs:smeulkooltjes"
KOPER = ("minecraft:waxed_exposed_cut_copper", "minecraft:waxed_weathered_cut_copper", "minecraft:waxed_oxidized_cut_copper")
KOPERBLOK = "minecraft:waxed_exposed_copper"
KETEL = "minecraft:waxed_copper_block"
ROOSTERKOPER = "minecraft:waxed_weathered_copper_grate"
BOL = "minecraft:waxed_copper_bulb"
GLAS = "minecraft:orange_stained_glass"
GEEL_GLAS = "minecraft:yellow_stained_glass"
RUIT = "minecraft:orange_stained_glass_pane"
PLANK = "minecraft:spruce_planks"
KNABBELMACHINE = "guhs:grote_knabbelmachine"
AIR = "minecraft:air"

FENCE = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
WALL = {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"}
ACHTER = {"north": (0, 1), "south": (0, -1), "west": (1, 0), "east": (-1, 0)}


def mc(n):
    return n if ":" in n else f"minecraft:{n}"


def ruis(seed):
    """Smooth 2D value noise in 0..1, for patchy ground and patina."""
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


def gewelf(x):
    """The y of the vault's skin over column x (None outside the hall)."""
    t = (x - CX) / ((X1 - X0) / 2.0 + 0.5)
    if abs(t) > 1:
        return None
    return SY + int(round(RISE * math.sqrt(1 - t * t)))


class Bouw:
    """The template under construction and what the self-check wants to know."""

    def __init__(self, h):
        self.h = h
        self.naam = NAAM
        self.s = h.Structure(MAAT)
        self.W, self.H, self.D = MAAT
        self.G = G
        self.rng = random.Random(30801)
        self.npc = NPC
        self.vaten = []

    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, mc(name), props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def leeg(self, x, y, z):
        return self.get(x, y, z) in (None, AIR)

    def vul(self, x0, y0, z0, x1, y1, z1, name, props=None):
        self.s.fill(x0, y0, z0, x1, y1, z1, mc(name), props)

    def paal(self, x, z, y0, y1, name, props=None):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, name, props)

    def lamp(self, x, y, z, hangend=False):
        self.set(x, y, z, "lantern", {"hanging": "true" if hangend else "false", "waterlogged": "false"})

    def ketting(self, x, z, y0, y1):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, "chain", {"axis": "y", "waterlogged": "false"})

    def plaat(self, x, y, z, boven=False, name=PLAAT):
        self.set(x, y, z, name, {"type": "top" if boven else "bottom", "waterlogged": "false"})

    def trap(self, x, y, z, facing, name=TRAP, om=False):
        self.set(x, y, z, name, {"facing": facing, "half": "top" if om else "bottom", "shape": "straight", "waterlogged": "false"})

    def vat(self, x, y, z, loot=None, facing="up"):
        nbt = {"id": "minecraft:barrel"}
        if loot:
            nbt["LootTable"] = loot
            self.vaten.append((x, y, z))
        self.set(x, y, z, "barrel", {"facing": facing, "open": "false"}, nbt)

    def bol(self, x, y, z):
        self.set(x, y, z, BOL, {"lit": "true", "powered": "false"})

    def staaf(self, x, y, z, facing="up"):
        """A lightning rod: a thin copper pipe."""
        self.set(x, y, z, "lightning_rod", {"facing": facing, "powered": "false", "waterlogged": "false"})

    def bord(self, x, y, z, regels, rotation=None, facing=None):
        """A waxed spruce sign: standing (rotation 0..15, 0 = its text faces south) or on a wall (facing = the side its text
        faces)."""
        import sign_text
        B = self.h.ms.Byte
        prefix = "sign.guhs.tech_quests"
        nbt = {"id": "minecraft:sign", "is_waxed": B(1),
               "front_text": {"messages": sign_text.messages(prefix, regels), "color": "black", "has_glowing_text": B(0)},
               "back_text": {"messages": sign_text.messages(prefix, ["", "", "", ""]), "color": "black", "has_glowing_text": B(0)}}
        if facing:
            self.set(x, y, z, "spruce_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            self.set(x, y, z, "spruce_sign", {"rotation": str(rotation or 0), "waterlogged": "false"}, nbt)

    def midden(self, x, z):
        """The centre jigsaw, in layer 0: what the structure type puts on the cave floor."""
        self.s.set(x, 0, z, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": f"guhs:{self.naam}_midden", "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": HOUTSKOOL, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


def koper(vlek, x, y, z):
    """Patchy patina: mostly weathered, greener low down and in streaks, a few plates still brown."""
    n = vlek(x * 1.7 + y * 0.6, z * 1.3 + y * 0.9, 3.5)
    return KOPER[2] if n > 0.66 else KOPER[0] if n < 0.2 else KOPER[1]


def baksteen(x, y, z):
    k = (x * 7 + y * 13 + z * 5) % 11
    return GEBARSTEN if k == 0 else STENEN


def centrale(h):
    b = Bouw(h)
    W, H, D = MAAT
    vlek = ruis(30802)
    patina = ruis(30803)

    # === the ground: a frayed disc of ash and charcoal rock, the cave cleared above it =====================================
    mx, mz, straal = W // 2, D // 2, 23.2
    for x in range(W):
        for z in range(D):
            d = math.hypot(x - mx, z - mz)
            if d > straal + b.rng.uniform(-1.2, 0.6):
                continue
            n = vlek(x, z)
            b.set(x, G, z, HOUTSKOOL if n > 0.62 else AS if n < 0.3 else AS_AARDE)
            for y in range(0, G):
                b.set(x, y, z, HOUTSKOOL if y < G - 1 else AS_AARDE)
            # (the cave is opened over the site: high over the hall and its chimneys, lower towards the rim)
            top = H if d < 19 else max(F + 6, int(H - (d - 19) * 4.5))
            for y in range(F, min(H, top)):
                b.set(x, y, z, AIR)

    # === the hall: floor, walls with pilasters and tall windows ===========================================================
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            b.set(x, G, z, STENEN)
            b.set(x, G - 1, z, HOUTSKOOL)
    for x in range(X0 + 1, X1):                                # the floor inside
        for z in range(Z0 + 1, Z1):
            gang = abs(x - CX) <= 2 or RZ + 6 <= z <= RZ + 8
            rand = abs(x - CX) == 3 or z in (RZ + 5, RZ + 9)
            if z <= RZ + 5:
                blok = GEPOLIJST                               # the practice hall: a clean iron floor
            elif gang:
                blok = GEBARSTEN if (x * 3 + z * 7) % 13 == 0 else STENEN
            elif rand:
                blok = GEPOLIJST
            else:
                blok = "minecraft:polished_blackstone" if (x + z) % 2 else STENEN
            b.set(x, G, z, blok)
    for x in range(X0 + 1, X1):                                # the hazard line along the front of the bays
        b.set(x, G, RZ + 6, "minecraft:yellow_concrete" if (x // 1) % 2 else "minecraft:black_concrete")

    pilaster_z = (Z0, Z0 + 4, Z0 + 9, Z0 + 13, Z0 + 18, Z1)   # along the side walls; windows in between
    for z in range(Z0, Z1 + 1):
        for x in (X0, X1):
            pil = z in pilaster_z
            for y in range(F, SY + 1):
                b.set(x, y, z, PILAAR if pil else baksteen(x, y, z), {"axis": "y"} if pil else None)
            b.set(x, F, z, GEBEITELD if not pil else PILAAR, None if not pil else {"axis": "y"})
            b.set(x, SY, z, GEPOLIJST)                         # the eaves beam
            for y in range(SY + 1, gewelf(x)):                 # (and the strip between it and the foot of the vault)
                b.set(x, y, z, ROOSTER if pil else KOPER[1])
    for a, c in zip(pilaster_z, pilaster_z[1:]):               # a tall window with a round top in every bay of the side walls
        m = (a + c) // 2
        breed = (m, m + 1) if (c - a) % 2 == 1 else (m,)
        for x, west in ((X0, True), (X1, False)):
            for z in breed:
                for y in range(F + 2, F + 7):
                    b.set(x, y, z, GLAS)
                b.set(x, F + 1, z, GEBEITELD)
                b.trap(x, F + 7, z, "east" if west else "west", om=True)
            for z in (breed[0] - 1, breed[-1] + 1):            # the window's frame
                for y in range(F + 2, F + 7):
                    b.set(x, y, z, ROOSTER)
            # a buttress outside, under each pilaster
    for z in pilaster_z[1:-1]:
        for x, dx, kant in ((X0, -1, "west"), (X1, 1, "east")):
            b.paal(x + dx, z, F, F + 3, STENEN)
            b.trap(x + dx, F + 4, z, "east" if dx < 0 else "west")
            b.set(x + dx, G, z, STENEN)

    # === the gables (north and south) and the copper vault with its ribs; over the ridge a glazed monitor ==================
    for x in range(X0, X1 + 1):
        top = gewelf(x)
        for z in (Z0, Z1):
            for y in range(F, top):
                b.set(x, y, z, baksteen(x, y, z))
        for z in range(Z0 - 1, Z1 + 2):
            rib = z in (Z0 - 1, Z1 + 1) or z in pilaster_z
            if abs(x - CX) <= 1 and Z0 < z < Z1:
                continue                                       # the ridge is open: the monitor stands over it
            b.set(x, top, z, ROOSTER if rib else koper(patina, x, top, z))
            # (the skin is one block thick: where the curve is steep the gap to the next column is filled)
            for dx in (-1, 1):
                buur = gewelf(x + dx)
                if buur is not None and buur < top - 1:
                    for y in range(buur + 1, top):
                        b.set(x, y, z, ROOSTER if rib else koper(patina, x, y, z))
    for z in range(Z0 - 1, Z1 + 2):                            # the gutters
        for x in (X0 - 1, X1 + 1):
            b.plaat(x, SY, z, boven=True)
    nok = gewelf(CX)
    for z in range(Z0, Z1 + 1):                                # the monitor: two glass walls and a little copper roof
        eind = z in (Z0, Z1)
        for x in (CX - 2, CX + 2):
            for y in (nok + 1, nok + 2):
                b.set(x, y, z, ROOSTER if eind or z in pilaster_z else GLAS)
        if eind:
            for x in (CX - 1, CX, CX + 1):
                for y in (nok, nok + 1, nok + 2):
                    b.set(x, y, z, baksteen(x, y, z))
        for x in range(CX - 2, CX + 3):
            b.set(x, nok + 3, z, koper(patina, x, nok + 3, z) if abs(x - CX) < 2 else ROOSTER)
        for x in (CX - 3, CX + 3):
            b.plaat(x, nok + 3, z, boven=False, name="minecraft:waxed_weathered_cut_copper_slab")
        b.plaat(CX, nok + 4, z, boven=False, name="minecraft:waxed_oxidized_cut_copper_slab")
    for z in (Z0 + 4, Z0 + 11, Z0 + 18):                       # three lightning rods on the monitor
        b.set(CX, nok + 4, z, KOPERBLOK)
        b.staaf(CX, nok + 5, z)

    # === the south gable: the door, the wheel window, the name ============================================================
    for x in range(CX - 1, CX + 2):                            # the doorway: three wide, four high, a round top
        for y in range(F, F + 4):
            b.set(x, y, Z1, AIR)
    b.trap(CX - 1, F + 3, Z1, "east", om=True)
    b.trap(CX + 1, F + 3, Z1, "west", om=True)
    for x in (CX - 2, CX + 2):                                 # its frame: two iron posts and a lintel
        b.paal(x, Z1, F, F + 4, PILAAR, {"axis": "y"})
    for x in range(CX - 2, CX + 3):
        b.set(x, F + 4, Z1, GEPOLIJST)
    # the wheel window: a rim of iron, eight spokes, warm glass, a glowing hub
    wy = F + 12
    for x in range(CX - 6, CX + 7):
        for y in range(wy - 6, wy + 7):
            d = math.hypot(x - CX, y - wy)
            if d > 5.6:
                continue
            dx, dy = abs(x - CX), abs(y - wy)
            if d > 4.6:
                b.set(x, y, Z1, ROOSTER)
            elif dx == 0 and dy == 0:
                b.bol(x, y, Z1)
            elif dx == 0 or dy == 0 or dx == dy:
                b.set(x, y, Z1, GEPOLIJST)
            else:
                b.set(x, y, Z1, GEEL_GLAS if (dx + dy) % 2 else GLAS)
    # the parapet: the gable stands a block proud of the roof, with a stepped pediment that hides the monitor's end
    for x in range(X0, X1 + 1):
        top = gewelf(x)
        b.set(x, top, Z1, STENEN)
        b.set(x, top + 1, Z1, GEPOLIJST)
    for x in range(CX - 5, CX + 6):
        hoog = nok + (5 if abs(x - CX) <= 1 else 4 if abs(x - CX) <= 3 else 2)
        for y in range(gewelf(x), hoog):
            b.set(x, y, Z1, baksteen(x, y, Z1))
        b.set(x, hoog, Z1, GEPOLIJST)
    b.bol(CX, nok + 3, Z1)
    b.staaf(CX, nok + 6, Z1)
    for x in (X0, X0 + 7, X1 - 7, X1):                         # pilasters on the gable, up to the roof
        top = gewelf(x)
        b.paal(x, Z1, F, top - 1, PILAAR, {"axis": "y"})
    for x in (X0 + 3, X0 + 4, X1 - 4, X1 - 3):                 # a narrow window left and right of the door
        for y in range(F + 2, F + 6):
            b.set(x, y, Z1, GLAS)
        b.set(x, F + 1, Z1, GEBEITELD)
    for x in (X0 + 10, X0 + 11, X1 - 11, X1 - 10):
        for y in range(F + 2, F + 5):
            b.set(x, y, Z1, GLAS)
        b.set(x, F + 1, Z1, GEBEITELD)
    # the canopy over the door, with the name board and two lamps
    for x in range(CX - 3, CX + 4):
        b.plaat(x, F + 5, Z1 + 1, boven=False, name="minecraft:waxed_weathered_cut_copper_slab")
        b.plaat(x, F + 5, Z1 + 2, boven=False, name="minecraft:waxed_weathered_cut_copper_slab")
    for x in (CX - 3, CX + 3):
        b.paal(x, Z1 + 2, F, F + 4, MUUR, WALL)
        b.lamp(x, F + 4, Z1 + 1, hangend=True)
    b.bord(CX - 1, F + 4, Z1 + 1, ["~ Oude ~", "Guhrad-", "centrale", ""], facing="south")
    b.bord(CX, F + 4, Z1 + 1, ["sinds", "heel lang", "geleden", "njeg"], facing="south")
    b.bord(CX + 1, F + 4, Z1 + 1, ["100%", "vadskracht", "0% rook", "(bijna)"], facing="south")

    # === the north gable outside: the two chimneys and their flues ========================================================
    for sx in (X0 + 4, X1 - 4):
        cz = Z0 - 4                                            # the middle of the chimney
        for x in range(sx - 2, sx + 3):                        # the foot: five by five, seven high, with a sloping shoulder
            for z in range(cz - 2, cz + 3):
                hoek = abs(x - sx) == 2 and abs(z - cz) == 2
                b.set(x, G, z, STENEN)
                for y in range(0, G):
                    b.set(x, y, z, HOUTSKOOL)
                if abs(x - sx) == 2 or abs(z - cz) == 2:
                    for y in range(F, F + (6 if hoek else 7)):
                        b.set(x, y, z, PILAAR if hoek else baksteen(x, y, z), {"axis": "y"} if hoek else None)
                    if hoek:
                        b.plaat(x, F + 6, z)
                    else:
                        kant = "east" if x < sx - 1 else "west" if x > sx + 1 else "south" if z < cz - 1 else "north"
                        b.trap(x, F + 7, z, kant)
        for x in range(sx - 1, sx + 2):                        # the shaft: three by three, banded with iron, hollow at the top
            for z in range(cz - 1, cz + 2):
                for y in range(F, H - 3):
                    if x != sx or z != cz:
                        b.set(x, y, z, ROOSTER if y % 6 == 3 and y > F + 7 else baksteen(x, y, z))
                    else:
                        b.set(x, y, z, AIR if y >= H - 6 else "minecraft:coal_block")
        for x in range(sx - 2, sx + 3):                        # the collar near the top, and the crown
            for z in range(cz - 2, cz + 3):
                if abs(x - sx) == 2 or abs(z - cz) == 2:
                    b.plaat(x, H - 6, z, boven=True)
        for x in range(sx - 1, sx + 2):
            for z in range(cz - 1, cz + 2):
                if x != sx or z != cz:
                    b.set(x, H - 3, z, GEPOLIJST)
        for y in range(F, F + 5):                              # the flue from the hall into the chimney
            for x in (sx - 1, sx, sx + 1):
                b.set(x, y, Z0 - 1, baksteen(x, y, Z0 - 1))
        for x in (sx - 1, sx, sx + 1):
            b.plaat(x, F + 5, Z0 - 1)
    for x in range(X0 + 9, X1 - 8):                            # a blind arcade between them
        if (x - CX) % 4 == 0:
            b.paal(x, Z0 - 1, F, F + 6, MUUR, WALL)
            b.set(x, G, Z0 - 1, STENEN)

    # === inside, north: the radergalerij (the wheels themselves come from Centrale.java) ==================================
    kernen = [p[0] for p in PLEKKEN["RADEREN"]]
    for i, kx in enumerate(kernen):
        for x in range(kx - 1, kx + 2):                        # the wheel's pit: a darker plate with a brass edge
            b.set(x, G, RZ, "minecraft:polished_blackstone")
        b.bol(kx, F + 4, Z0)                                   # a lamp in the wall over every wheel
        b.bord(kx - 1, F + 5, RZ, ["Rad %d" % (i + 1), ("Opa Njeg", "Tante Vads", "Ome Knabbel", "Oma Vahoeg", "Neef Guh")[i],
                                   ("rent sinds", "rent al", "rent sinds", "rent voor", "rent sinds")[i],
                                   ("het begin", "heel lang", "gisteren", "de lol", "het ontbijt")[i]], facing="south")
    for x in (X0 + 2, X0 + 8, X0 + 14, X0 + 20, X0 + 26, X0 + 32):   # the bay walls: low, with a post and a lamp at the front
        for z in range(RZ, RZ + 5):
            b.set(x, F, z, STENEN if z < RZ + 4 else GEBEITELD)
            if z < RZ + 4:
                b.set(x, F + 1, z, TRALIES, {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
        b.paal(x, RZ + 4, F + 1, F + 2, MUUR, WALL)
        b.lamp(x, F + 3, RZ + 4)
    # the catwalk over the wheels, with a railing and a ladder at each end
    cy = F + 4
    for x in range(X0 + 1, X1):
        for z in (RZ, RZ + 1):
            b.plaat(x, cy - 1, z, boven=True, name="minecraft:waxed_weathered_cut_copper_slab")
        b.set(x, cy, RZ + 1, TRALIES, {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"})
    for lx, kant in ((X0 + 1, "east"), (X1 - 1, "west")):
        for y in range(F, cy):
            b.set(lx, y, RZ + 2, "ladder", {"facing": "south", "waterlogged": "false"})
            b.set(lx, y, RZ + 1, PILAAR, {"axis": "y"})
        b.set(lx, cy - 1, RZ + 1, PILAAR, {"axis": "y"})
        b.set(lx, cy, RZ + 1, AIR)
        b.set(lx, cy - 1, RZ + 2, AIR)
        b.set(lx, cy - 1, RZ + 2, "ladder", {"facing": "south", "waterlogged": "false"})
    # the pipes: a fat copper main along the north wall, risers to every wheel, and two bends off to the chimneys
    py = F + 7
    for x in range(X0 + 2, X1 - 1):
        b.set(x, py, Z0 + 1, KOPERBLOK if (x - CX) % 6 else ROOSTERKOPER)
    for kx in kernen:
        for y in range(cy + 1, py):
            b.staaf(kx, y, Z0 + 1)
        b.set(kx, py, Z0 + 1, KETEL)
    for sx in (X0 + 4, X1 - 4):
        for y in range(py + 1, py + 4):
            b.set(sx, y, Z0 + 1, KOPERBLOK)
        b.set(sx, py + 3, Z0, "minecraft:blast_furnace", {"facing": "south", "lit": "true"}, {"id": "minecraft:blast_furnace"})
    # the control wall between the pipes: dials and lamps
    for x in range(X0 + 6, X1 - 5):
        k = (x - CX) % 6
        if k == 3:
            b.set(x, py + 1, Z0 + 0, "minecraft:observer", {"facing": "north", "powered": "false"})
        elif k == 0:
            b.bol(x, py + 2, Z0)

    # === the practice hall: the signs of the five setups (their machines come from Centrale.java) =========================
    titels = (["Opstelling 1", "De losse draad", "Leg Guhdraad", "in het gat"],
              ["Opstelling 2", "Te zwaar!", "Knip een draad", "bij de rode wol"],
              ["Opstelling 3", "Achterstevoren", "Sluip + klik:", "draai het stuk"],
              ["Opstelling 4", "Het filter staat", "verkeerd: kies", "alleen deze"],
              ["Opstelling 5", "De slang", "is zoek: leg er", "een in het gat"])
    for i, kx in enumerate(kernen):
        b.bord(kx - 2, F, RZ + 5, titels[i], rotation=0)
        b.set(kx - 2, G, RZ + 5, GEBEITELD)
    for (x, y, z) in PLEKKEN["S2_KNIP"]:                       # where the wire may be cut: red wool in the floor
        b.set(x, G, z, "minecraft:red_wool")
    for (x, y, z) in PLEKKEN["S1_GAT"] + PLEKKEN["S5_GAT"]:    # where something is missing: a yellow tile
        b.set(x, G, z, "minecraft:yellow_glazed_terracotta", {"facing": "north"})
    # (the little sauce pit of setup 5: its sauce comes from Centrale.java, the rim keeps it in)
    bx, by, bz = PLEKKEN["S5_BRON"][0]
    b.set(bx, by - 1, bz, GEPOLIJST)

    # === the lamps under the vault: chains down from the ribs =============================================================
    for z in pilaster_z[1:-1]:
        for x in (CX - 9, CX, CX + 9):
            top = gewelf(x) if x != CX else nok + 3            # (the middle ones hang from the monitor's roof)
            lang = 7 if x == CX else 2
            b.ketting(x, z, top - lang, top - 1)
            b.lamp(x, top - lang - 1, z, hangend=True)

    # === the workshop of the Uitvinder-guh (south-west) ===================================================================
    wx0, wx1, wz0, wz1 = X0 + 1, CX - 4, RZ + 9, Z1 - 1      # x 7..19, z 19..30
    for x in range(wx0 + 2, wx1 - 1):                          # a worn rug
        for z in range(wz0 + 3, wz1 - 2):
            rand = x in (wx0 + 2, wx1 - 2) or z in (wz0 + 3, wz1 - 3)
            b.set(x, F, z, "minecraft:cyan_carpet" if rand else "minecraft:light_blue_carpet" if (x + z) % 2 else "minecraft:white_carpet")
    b.set(NPC[0], F, NPC[2], AIR)
    b.set(NPC[0], G, NPC[2], "minecraft:pink_wool")           # his cushion, sunk into the floor
    # the west wall: a long workbench (the Tekentafel on it comes from Centrale.java), shelves above
    for z in range(wz0, wz1 + 1):
        if z == PLEKKEN["TEKENTAFEL"][0][2]:
            continue
        k = z - wz0
        if k in (0, 1):
            b.vat(wx0, F, z, "guhs:chests/tech_quests_werkplaats" if k == 0 else None, facing="east")
        elif k == 2:
            b.set(wx0, F, z, "minecraft:crafting_table")
        elif k in (4, 5):
            b.set(wx0, F, z, "minecraft:smithing_table" if k == 4 else "minecraft:cartography_table")
        elif k in (6, 7):
            b.set(wx0, F, z, "minecraft:bookshelf")
            b.set(wx0, F + 1, z, "minecraft:bookshelf")
        elif k == 8:
            b.set(wx0, F, z, "minecraft:lectern", {"facing": "east", "has_book": "false", "powered": "false"}, {"id": "minecraft:lectern"})
        elif k == 9:
            b.set(wx0, F, z, "minecraft:grindstone", {"face": "floor", "facing": "east"})
        else:
            b.vat(wx0, F, z, None, facing="up")
    for z in range(wz0, wz0 + 6):                              # a shelf of jars and bulbs over the bench
        b.plaat(wx0, F + 2, z, boven=True, name="minecraft:spruce_slab")
    b.set(wx0, F + 3, wz0, "minecraft:decorated_pot", {"facing": "east", "cracked": "false", "waterlogged": "false"}, {"id": "minecraft:decorated_pot"})
    b.bol(wx0, F + 3, wz0 + 2)
    b.set(wx0, F + 3, wz0 + 4, "minecraft:flower_pot")
    # his drawing table in the middle: a big sheet (blue carpet) on a plank table, a stool, a stack of rolled-up plans
    tx, tz = 11, wz0 + 8
    for x in range(tx, tx + 3):
        for z in (tz, tz + 1):
            b.set(x, F, z, PLANK)
            b.set(x, F + 1, z, "minecraft:blue_carpet" if (x, z) != (tx + 2, tz + 1) else "minecraft:white_carpet")
    b.trap(tx + 1, F, tz + 2, "north", name="minecraft:spruce_stairs")
    b.set(tx + 1, F + 1, tz, "guhs:knabbelmachine_beeldje", {"facing": "south"})   # his scale model of the machine
    # along the south wall: crates, a furnace corner, his bed roll (a guh sleeping bag) and a kettle
    b.vat(wx0 + 1, F, wz1, None)
    b.vat(wx0 + 2, F, wz1, "guhs:chests/tech_quests_werkplaats")
    b.vat(wx0 + 1, F + 1, wz1, None, facing="north")
    b.set(wx0 + 4, F, wz1, "minecraft:blast_furnace", {"facing": "north", "lit": "false"}, {"id": "minecraft:blast_furnace"})
    b.set(wx0 + 5, F, wz1, "minecraft:anvil", {"facing": "east"})
    b.set(wx0 + 7, F, wz1, "minecraft:cauldron")
    b.set(wx0 + 8, F, wz1, "minecraft:composter", {"level": "3"})
    b.set(wx1 - 1, F, wz1, KETEL)                              # a small copper boiler in the corner by the door
    b.set(wx1 - 1, F + 1, wz1, KETEL)
    b.staaf(wx1 - 1, F + 2, wz1)
    b.staaf(wx1 - 1, F + 3, wz1)
    b.bord(wx1 - 1, F + 1, wz1 - 1, ["Niet aankomen", "(heet)", "(en van mij)", "njeg"], facing="north")
    # a pin-up board on the wall by the bench and the sign with his name
    b.bord(NPC[0] + 2, F, NPC[2] + 3, ["De Uitvinder-", "guh", "(denkt na,", "niet storen)"], rotation=12)
    # a screen of shelves between the workshop and the cross aisle
    for x in range(wx0 + 3, wx1 - 2, 1):
        if x % 4 == 2:
            b.paal(x, wz0, F, F + 2, PILAAR, {"axis": "y"})
            b.lamp(x, F + 3, wz0)
        elif x % 4 == 0:
            b.set(x, F, wz0, "minecraft:bookshelf")
            b.set(x, F + 1, wz0, "minecraft:bookshelf")
        else:
            b.vat(x, F, wz0, None, facing="south")

    # === the bordes of De Grote Knabbelmachine (south-east) ===============================================================
    kx, ky, kz = KERN
    pcx, pcz = kx + 3, kz                                      # the middle of the machine: three blocks behind its mouth
    for x in range(pcx - 6, pcx + 7):
        for z in range(pcz - 6, pcz + 7):
            if not (X0 < x < X1 and Z0 < z < Z1):
                continue
            d = math.hypot(x - pcx, z - pcz)
            if d <= 5.4:
                b.set(x, F, z, GEPOLIJST if d <= 4.4 else GEBEITELD)
                b.set(x, G, z, STENEN)
            elif d <= 6.3 and b.get(x, G, z) is not None:
                b.set(x, G, z, GEPOLIJST)
    for x in range(pcx - 6, pcx + 7):                          # the railing around it; the front (west) stays open at the mouth
        for z in range(pcz - 6, pcz + 7):
            if not (X0 < x < X1 and Z0 < z < Z1):
                continue
            d = math.hypot(x - pcx, z - pcz)
            if 4.4 < d <= 5.4 and not (x <= kx - 1 and abs(z - kz) <= 1):
                b.set(x, F + 1, z, MUUR, WALL)
    for z in (kz - 2, kz + 2):                                 # two lamp posts at the opening
        b.paal(kx - 2, z, F + 1, F + 2, MUUR, WALL)
        b.lamp(kx - 2, F + 3, z)
    for z in (kz - 1, kz, kz + 1):                             # the steps up to the mouth
        b.trap(kx - 3, F, z, "east")
    b.set(kx, ky, kz, KNABBELMACHINE, {"facing": "west"}, {"id": "guhs:techquest_knabbelmachine"})
    # nobody walks through the machine: its body is closed off (the model is drawn by the client, per player)
    for x in range(kx + 1, kx + 7):
        for z in range(kz - 3, kz + 4):
            for y in range(ky, ky + 7):
                if math.hypot(x - pcx, z - pcz) <= 3.6:
                    b.set(x, y, z, "minecraft:barrier")
    for z in (kz - 1, kz + 1):
        b.set(kx, ky, z, "minecraft:barrier")
    b.bord(kx - 4, F, kz + 3, ["Bouwplaats", "De Grote", "Knabbelmachine", "(niet voeren)"], rotation=4)

    # === pipes along the side walls inside, an emblem in the floor where the aisles cross ==================================
    for z in range(Z0 + 1, Z1):
        for x in (X0 + 1, X1 - 1):
            if z in pilaster_z[1:-1]:
                b.set(x, SY - 1, z, ROOSTERKOPER)
            elif z > RZ + 2:
                b.staaf(x, SY - 1, z, "south")
    for dx in range(-2, 3):
        for dz in range(-1, 2):
            if abs(dx) == 2 or abs(dz) == 1:
                b.set(CX + dx, G, RZ + 7 + dz, "minecraft:waxed_cut_copper")
    b.set(CX, G, RZ + 7, "minecraft:pink_glazed_terracotta", {"facing": "south"})

    # === outside, east: two copper sauce tanks on legs, piped into the wall; west: the coal pen ===========================
    for tz in (RZ + 3, RZ + 10):
        tx = X1 + 4
        if b.get(tx, G, tz) is None:
            continue
        for x in range(tx - 1, tx + 2):
            for z in range(tz - 1, tz + 2):
                hoek = abs(x - tx) == 1 and abs(z - tz) == 1
                b.set(x, G, z, STENEN)
                if hoek:
                    b.paal(x, z, F, F + 1, MUUR, WALL)
                for y in range(F + 2, F + 7):
                    b.set(x, y, z, koper(patina, x, y, z).replace("_cut", ""))
                b.plaat(x, F + 7, z, name="minecraft:waxed_weathered_cut_copper_slab")
        b.set(tx, F + 7, tz, KETEL)
        b.staaf(tx, F + 8, tz)
        for x in range(X1 + 1, tx - 1):                        # the pipe into the hall
            b.set(x, F + 4, tz, KOPERBLOK)
        b.set(X1, F + 4, tz, ROOSTERKOPER)
    px0, px1, pz0, pz1 = X0 - 5, X0 - 2, RZ + 4, RZ + 10
    for x in range(px0, px1 + 1):
        for z in range(pz0, pz1 + 1):
            if b.get(x, G, z) is None:
                continue
            rand = x == px0 or z in (pz0, pz1)
            if rand:
                b.set(x, G, z, STENEN)
                b.set(x, F, z, baksteen(x, F, z))
                if (x + z) % 2 == 0:
                    b.plaat(x, F + 1, z)
            else:
                for y in range(F, F + 1 + ((x * 3 + z * 5) % 3 > 0) + (x == px1 and z % 2)):
                    b.set(x, y, z, "minecraft:coal_block")
    for z in (Z1 - 5, Z1 - 4, Z1 - 3):                         # a waiting bench along the aisle, by the door
        b.trap(CX - 3, F, z, "west", name="minecraft:spruce_stairs")
    for z in (Z1 - 2, Z1 - 1):                                 # a stack of crates by the door, east
        b.vat(CX + 4, F, z, None, facing="up" if z % 2 else "west")
    b.vat(CX + 4, F + 1, Z1 - 1, None)

    # === the forecourt: the path, lamp posts, pylons with cables, the sunken old wheel ====================================
    for z in range(Z1 + 1, D - 2):
        for x in range(CX - 1, CX + 2):
            if b.get(x, G, z) is not None:
                b.set(x, G, z, GEBARSTEN if (x * 5 + z * 3) % 7 == 0 else STENEN)
    for x in range(CX - 4, CX + 5):                            # a paved apron in front of the door
        for z in range(Z1 + 1, Z1 + 4):
            if b.get(x, G, z) is not None:
                b.set(x, G, z, baksteen(x, G, z))
    for (x, z) in ((CX - 4, Z1 + 7), (CX + 4, Z1 + 7), (CX - 4, Z1 + 12), (CX + 4, Z1 + 12)):
        if b.get(x, G, z) is not None:
            b.paal(x, z, F, F + 2, MUUR, WALL)
            b.lamp(x, F + 3, z)
    # two pylons: a mast with a cross arm, cables (chains) sagging to the gable
    for px in (X0 + 5, X1 - 5):
        pz = Z1 + 6
        if b.get(px, G, pz) is None:
            continue
        b.set(px, G, pz, STENEN)
        b.paal(px, pz, F, F + 7, PILAAR, {"axis": "y"})
        for dx in (-1, 0, 1):
            b.set(px + dx, F + 8, pz, GEPOLIJST)
        for dx in (-1, 1):
            b.staaf(px + dx, F + 9, pz)
            for z in range(pz - 1, Z1, -1):                    # the cable to the gable
                b.set(px + dx, F + 8, z, "chain", {"axis": "z", "waterlogged": "false"})
    # the old Guhrad, half sunk into the ash west of the path: a rim with spokes, leaning
    ox, oz, r = CX - 10, Z1 + 9, 3.6
    for x in range(ox - 4, ox + 5):
        for y in range(G - 1, G + 9):
            d = math.hypot(x - ox, y - (G + 2))
            dx, dy = abs(x - ox), abs(y - (G + 2))
            z = oz
            if y <= G:
                continue
            if 2.9 < d <= 4.2:
                b.set(x, y, z, ROOSTER if (x + y) % 3 else PLANK)
            elif d <= 2.9 and (dx == 0 or dy == 0):
                b.set(x, y, z, "minecraft:spruce_fence", {**FENCE, "east": "true", "west": "true"} if dy == 0 else FENCE)
    b.set(ox, G + 2, oz, GEPOLIJST)
    b.bord(ox + 3, F, oz + 2, ["Rad nr. 6", "(met pensioen)", "", ""], rotation=2)
    # a heap of coal and ash east of the path, with a shovel-cart of barrels
    hx, hz = CX + 10, Z1 + 9
    for x in range(hx - 4, hx + 5):
        for z in range(hz - 4, hz + 5):
            d = math.hypot(x - hx, z - hz)
            if d < 3.6 and b.get(x, G, z) is not None:
                for y in range(F, F + max(1, int(round(3 * (1 - (d / 3.6) ** 1.6))))):
                    b.set(x, y, z, "minecraft:coal_block" if (x + y * 2 + z) % 3 == 0 else AS)
    b.vat(hx - 5, F, hz + 1, None)
    b.vat(hx - 5, F, hz + 2, "guhs:chests/tech_quests_werkplaats", facing="west")
    for _ in range(26):                                        # embers in the ash around the site
        x, z = b.rng.randrange(2, W - 2), b.rng.randrange(2, D - 2)
        buiten = not (X0 - 2 <= x <= X1 + 2 and Z0 - 6 <= z <= Z1 + 3)
        if buiten and b.get(x, G, z) in (AS, AS_AARDE) and b.leeg(x, F, z) and abs(x - CX) > 2:
            b.set(x, F, z, SMEUL)
    b.midden(mx, mz)
    return b


# =====================================================================================================================
# the stand-ins of what Centrale.java puts down (preview pictures only), the self-check, the test room
# =====================================================================================================================
def voorbeeld(b):
    """Stand-ins (plain blocks of about the right colour) at PLEKKEN, for the preview pictures: never saved."""
    P = PLEKKEN
    for (x, y, z) in P["RADEREN"]:
        for dx in (-1, 0, 1):
            for dy in (0, 1, 2):
                b.set(x + dx, y + dy, z, "minecraft:pink_concrete" if (dx, dy) != (0, 1) else "minecraft:pink_wool")
    for key in ("S1_DRAAD", "S2_DRAAD", "S2_KNIP", "S4_DRAAD", "S5_DRAAD"):
        for (x, y, z) in P[key]:
            b.set(x, y, z, "minecraft:pink_carpet")
    for key in ("S1_OVEN", "S2_OVENS"):
        for (x, y, z) in P[key]:
            b.set(x, y, z, "minecraft:pink_terracotta")
    x, y, z = P["S2_MOLEN"][0]
    b.set(x, y, z, "minecraft:pink_terracotta")
    b.set(x, y + 1, z, "minecraft:white_wool")
    for key in ("S3_VAT_A", "S3_VAT_B", "S4_VAT_A", "S4_VAT_B"):
        x, y, z = P[key][0]
        b.vat(x, y, z)
    for key in ("S3_STUK", "S3_BUIS", "S4_FILTER", "S4_BUIS"):
        for (x, y, z) in P[key]:
            b.set(x, y, z, "minecraft:glass")
    x, y, z = P["S3_BATTERIJ"][0]
    b.set(x, y, z, "minecraft:magenta_terracotta")
    x, y, z = P["S5_BRON"][0]
    b.set(x, y, z, "minecraft:orange_concrete")
    x, y, z = P["S5_POMP"][0]
    b.set(x, y, z, "minecraft:pink_terracotta")
    x, y, z = P["S5_SLANG"][0]
    b.set(x, y, z, "minecraft:orange_carpet")
    x, y, z = P["S5_SAUSVAT"][0]
    b.vat(x, y, z)
    x, y, z = P["TEKENTAFEL"][0]
    b.set(x, y, z, "minecraft:fletching_table")
    return b


def check(b):
    blocks = b.s.blocks
    problems = []

    def nm(c):
        v = blocks.get(c)
        return v[0] if v else None

    def vrij(c):
        return nm(c) in (None, AIR)

    vast = {c for c, v in blocks.items() if v[0] != AIR}
    gezien = {c for c in vast if c[1] <= G}
    todo = list(gezien)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in vast and n not in gezien:
                gezien.add(n)
                todo.append(n)
    for c in sorted(vast - gezien):
        if nm(c) != "minecraft:barrier":
            problems.append(f"floating {nm(c)} at {c}")
    for c, v in blocks.items():
        if v[0] == "minecraft:lantern" and v[1].get("hanging") == "true" and vrij((c[0], c[1] + 1, c[2])):
            problems.append(f"a hanging lantern at {c} hangs from nothing")
        if v[0] == "minecraft:lantern" and v[1].get("hanging") == "false" and vrij((c[0], c[1] - 1, c[2])):
            problems.append(f"a lantern at {c} stands on nothing")
        if v[0] in ("minecraft:spruce_wall_sign", "minecraft:ladder"):
            dx, dz = ACHTER[v[1]["facing"]]
            if vrij((c[0] + dx, c[1], c[2] + dz)):
                problems.append(f"the {v[0]} at {c} has no wall behind it")
        if v[0] == "minecraft:spruce_sign" and vrij((c[0], c[1] - 1, c[2])):
            problems.append(f"the sign at {c} stands on nothing")
    # the spots Centrale.java furnishes: free, with a floor under them (the sauce pit: a hole with walls)
    for key, plekken in PLEKKEN.items():
        for (x, y, z) in plekken:
            if key == "S5_BRON":
                if vrij((x, y - 1, z)) or any(vrij((x + dx, y, z + dz)) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    problems.append(f"{key}: the sauce pit at {(x, y, z)} leaks")
                continue
            if key in ("KERN", "S5_POMP"):
                continue
            if not vrij((x, y, z)) or not vrij((x, y + 1, z)):
                problems.append(f"{key}: {(x, y, z)} is taken by {nm((x, y, z))} / {nm((x, y + 1, z))}")
            if vrij((x, y - 1, z)):
                problems.append(f"{key}: {(x, y, z)} has no floor")
    for (x, y, z) in PLEKKEN["RADEREN"]:                        # a Guhrad is 3 wide and 3 high
        for dx in (-1, 0, 1):
            for dy in (0, 1, 2):
                if not vrij((x + dx, y + dy, z)):
                    problems.append(f"wheel at {(x, y, z)}: {(x + dx, y + dy, z)} is taken by {nm((x + dx, y + dy, z))}")
    x, y, z = PLEKKEN["S2_MOLEN"][0]
    if not vrij((x, y + 1, z)):
        problems.append("the Vadsmolen of setup 2 has no room above it")
    if nm(KERN) != KNABBELMACHINE:
        problems.append("the kern of the Grote Knabbelmachine is missing")
    if not vrij((KERN[0] - 1, KERN[1], KERN[2])) or vrij((KERN[0] - 1, KERN[1] - 1, KERN[2])):
        problems.append("nobody can stand in front of the Grote Knabbelmachine")
    # the door is open and everything a player must reach can be walked to from the forecourt
    from features import sterrenwacht_hulp as hulp
    start = (CX, F, Z1 + 4)
    bereik = hulp.walk(b.s, [start], extra_passable=("guhs:smeulkooltjes", "minecraft:ladder"))
    doelen = {"the Uitvinder-guh": NPC, "the mouth of the machine": (KERN[0] - 1, KERN[1], KERN[2]), "the catwalk": (CX, F + 4, RZ)}
    for i, (kx, _, _) in enumerate(PLEKKEN["RADEREN"]):
        doelen[f"setup {i + 1}"] = (kx + 1, F, RZ + 3)
    for naam, (x, y, z) in doelen.items():
        if not hulp.near_reachable(bereik, x, y, z, 2):
            problems.append(f"{naam} at {(x, y, z)} cannot be reached from the door")
    x, y, z = NPC
    if vrij((x, y - 1, z)) or not vrij((x, y, z)) or not vrij((x, y + 1, z)):
        problems.append(f"the NPC has no place to sit at {NPC}")
    if not b.vaten:
        problems.append("no loot barrel")
    if sum(1 for v in blocks.values() if v[0] == "minecraft:jigsaw") != 1 or nm((MAAT[0] // 2, 0, MAAT[2] // 2)) != "minecraft:jigsaw":
        problems.append("exactly one centre jigsaw, in layer 0")
    return problems


def test_kamer(h):
    """techquest_test_kamer: a stone floor two blocks thick (the sauce pit of setup 5 needs a block under it), 33 x 8 x 13."""
    s = h.Structure((33, 8, 13))
    s.fill(0, 0, 0, 32, 1, 12, "minecraft:stone")
    s.save("techquest_test_kamer")


def build(h):
    """Builds, checks and saves the template (with its NPC) and the test room; returns (problems, Bouw)."""
    from features import wereld
    b = centrale(h)
    problems = check(b)
    x, y, z = NPC
    b.s.entity(x + 0.5, y, z + 0.5, wereld.npc(h, "uitvinderguh", "techquest_uitvinder", yaw=NPC_YAW))
    if not problems:
        b.s.save(NAAM)
        test_kamer(h)
    return problems, b
