"""
bbq2 (paleizen): the three new Mika palaces of the Guhbarbecuether as templates (Java: feature/paleizen).

  - guhs:mika_brugpaleis   the bastion-bridge parody (type guhs:burcht, placement "brug"): a long toll bridge of grill iron
                           on arches and piers. You walk in through the open mouth of a giant Mika face (the tolhuis, where
                           the Tolwachter-Mika sits next to his raised barrier), over a stretch where five rows of planks
                           are missing (a scaffold hangs under the gap, so nobody falls far) to the bell tower with the tolbel.
  - guhs:mika_woonblokken  the bastion-housing parody (type guhs:burcht, placement "paleis"): three crooked flats of grill
                           iron round a courtyard on a base in the sauce sea, with galleries, lit windows, balconies,
                           washing lines, chimneys and two stair towers. Mika-oma lives on the top gallery; three grumpy
                           neighbours (Mopper-Mika's) live below; her knitting blew onto the roof garden of the low block.
  - guhs:mika_stal         the bastion-stables parody (type guhs:barbecueput, one template on a cave floor): a red barn with
                           a hayloft and six boxes, a muddy paddock with a shelter, hay bales and a cart, and the
                           Stalknecht-guh in front of the big door under the Worstzwijntje sign.

Every build has a geometry self-check (nothing floats, the things that need a floor stand on one, there is a way on foot
or by ladder to every spot of the questlines) that build_all() runs. PLEKKEN holds the template coordinates the Java side
needs (feature/paleizen/PaleisPlekken.java; paleizen.selfcheck compares the two).
"""
import math
import random

from features import spiesburcht_burcht as sb
from features import wereld

mc = sb.mc
AIR = sb.AIR
STENEN, GEBARSTEN, GEBEITELD, TRAP, PLAAT, MUUR, HEK = sb.STENEN, sb.GEBARSTEN, sb.GEBEITELD, sb.TRAP, sb.PLAAT, sb.MUUR, sb.HEK
HOUTSKOOL, ROOSTER, PILAAR, GEPOLIJST, TRALIES = sb.HOUTSKOOL, sb.ROOSTER, sb.PILAAR, sb.GEPOLIJST, sb.TRALIES
GLOEIKOOL, AS, AS_AARDE, SATE, VLEES, WORST, MOSTERD = sb.GLOEIKOOL, sb.AS, sb.AS_AARDE, sb.SATE, sb.VLEES, sb.WORST, sb.MOSTERD
NYLIUM, SCHEUTJES, ZWAMMETJE, PLASJE, SMEUL, GRILLKOOL = sb.NYLIUM, sb.SCHEUTJES, sb.ZWAMMETJE, sb.PLASJE, sb.SMEUL, sb.GRILLKOOL
MIKAKOP, KNABBELS, UIENLICHT, ROOKGAT = sb.MIKAKOP, sb.KNABBELS, sb.UIENLICHT, sb.ROOKGAT

PLANK = "guhs:paleizen_brugplank"          # the bridge deck planks (the reward block; the missing rows are these)
LEUNING = "guhs:paleizen_brugleuning"      # the rope railing (the reward block)
BREIWERK = "guhs:paleizen_breiwerk"        # Mika-oma's lost knitting
VOERBAK = "guhs:guh_voerbak"
HOUT = "minecraft:crimson_planks"
HOUT_TRAP = "minecraft:crimson_stairs"
HOUT_PLAAT = "minecraft:crimson_slab"
HOUT_HEK = "minecraft:crimson_fence"
HOUT_POORT = "minecraft:crimson_fence_gate"
STAM = "minecraft:stripped_crimson_stem"
DAK = "minecraft:red_nether_bricks"
DAK_TRAP = "minecraft:red_nether_brick_stairs"
DAK_PLAAT = "minecraft:red_nether_brick_slab"
HOOI = "minecraft:hay_block"

# the template coordinates the questlines use: {structure: {name: (x, y, z) or (x0, y0, z0, x1, y1, z1)}}
PLEKKEN = {}
# every sign text key used in the templates (sign.guhs.<key>; paleizen.selfcheck wants a text for each)
SIGN_KEYS = set()


class Paleis(sb.Bouw):
    """sb.Bouw with the furniture of a house, the quest characters and a walk check that knows ladders."""

    def __init__(self, h, size, seed, naam):
        super().__init__(h, size, seed)
        self.naam = naam
        self.plek = PLEKKEN.setdefault(naam, {})
        self.plek.clear()
        self.wezens = []            # (x, y, z, what) of the quest characters and animals
        self.vloer = []             # cells that must have a floor under them (checked)

    # --- small parts -------------------------------------------------------------------------------------------------
    def sign(self, x, y, z, facing, keys, wall=True, wood="crimson"):
        SIGN_KEYS.update(k for k in keys if k)
        super().sign(x, y, z, facing, keys, wall=wall, wood=wood)

    def pane(self, x, y, z, kleur="orange"):
        self.set(x, y, z, f"{kleur}_stained_glass_pane", {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})

    def hfence(self, x, y, z):
        self.fence(x, y, z, block=HOUT_HEK)

    def gate(self, x, y, z, facing, open_=False):
        self.set(x, y, z, HOUT_POORT, {"facing": facing, "in_wall": "false", "open": "true" if open_ else "false", "powered": "false"})

    def door(self, x, y, z, facing, hinge="left", open_=False, wood="crimson"):
        for half, yy in (("lower", y), ("upper", y + 1)):
            self.set(x, yy, z, f"{wood}_door", {"facing": facing, "half": half, "hinge": hinge, "open": "true" if open_ else "false", "powered": "false"})

    def bed(self, x, y, z, facing, kleur="red"):
        """A bed with its foot at (x, y, z) and its head one block further in `facing`."""
        dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
        nbt = {"id": "minecraft:bed"}
        self.set(x, y, z, f"{kleur}_bed", {"facing": facing, "part": "foot", "occupied": "false"}, dict(nbt))
        self.set(x + dx, y, z + dz, f"{kleur}_bed", {"facing": facing, "part": "head", "occupied": "false"}, dict(nbt))

    def ladder(self, x, y0, y1, z, facing):
        """A ladder from y0 up to y1 (inclusive); `facing` is the side you climb on (away from the wall it hangs on)."""
        for y in range(y0, y1 + 1):
            self.set(x, y, z, "ladder", {"facing": facing, "waterlogged": "false"})

    def trapdoor(self, x, y, z, facing, half="bottom", open_=False, wood="crimson"):
        self.set(x, y, z, f"{wood}_trapdoor", {"facing": facing, "half": half, "open": "true" if open_ else "false", "powered": "false",
                                               "waterlogged": "false"})

    def carpet(self, x, y, z, kleur):
        self.set(x, y, z, f"{kleur}_carpet")

    def log(self, x, y, z, block, axis="y"):
        self.set(x, y, z, block, {"axis": axis})

    def pilaar(self, x, y0, y1, z):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, PILAAR, {"axis": "y"})

    def campfire(self, x, y, z, signal=False):
        self.set(x, y, z, "campfire", {"lit": "true", "facing": "north", "signal_fire": "true" if signal else "false", "waterlogged": "false"})

    def bel(self, x, y, z, facing="north"):
        self.set(x, y, z, "bell", {"attachment": "ceiling", "facing": facing, "powered": "false"}, {"id": "minecraft:bell"})

    def hoorns(self, x, y, z, side, along_x=True, lang=9):
        """A big Mika horn: a curved stack leaning out to `side` (-1 / +1), red at the tip."""
        horn = ((0, 0), (0, 1), (side, 1), (side, 2), (side, 3), (2 * side, 3), (2 * side, 4), (3 * side, 4), (3 * side, 5))[:lang]
        for i, (d, dy) in enumerate(horn):
            px, pz = (x + d, z) if along_x else (x, z + d)
            if i < len(horn) - 2:
                self.set(px, y + dy, pz, PILAAR, {"axis": "y"})
            else:
                self.set(px, y + dy, pz, "red_concrete")

    # --- the characters ----------------------------------------------------------------------------------------------
    def npc(self, x, y, z, kind, id, yaw):
        self.s.entity(x + 0.5, float(y), z + 0.5, wereld.npc(self.h, kind, id, yaw=yaw))
        self.wezens.append((x, y, z, kind))
        self.plek[kind] = (x, y, z)

    def wezen(self, x, y, z, entity, id, yaw, plek, **extra):
        """One of our own creatures with the Bezetting tag `id` (feature/wereld/Bezetting takes it over and repairs)."""
        nbt = {"id": entity, "PersistenceRequired": self.h.Byte(1), "Invulnerable": self.h.Byte(1), "Rotation": self.h.floats(float(yaw), 0.0),
               "NeoForgeData": {"guhs_bezetting": id}}
        nbt.update(extra)
        self.s.entity(x + 0.5, float(y), z + 0.5, nbt)
        self.wezens.append((x, y, z, entity))
        self.plek[plek] = (x, y, z)

    # --- finishing ---------------------------------------------------------------------------------------------------
    def connect(self):
        super().connect()
        dirs = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}
        for (x, y, z), (name, props, nbt) in list(self.s.blocks.items()):
            if name.endswith("_pane") or name == mc(LEUNING):
                new = dict(props)
                for d, (dx, dz) in dirs.items():
                    n = self.get(x + dx, y, z + dz)
                    join = n is not None and n != AIR and (n == name or (vast(n) and not n.endswith("_stairs") and n != mc(TRAP)))
                    new[d] = "true" if join else "false"
                self.s.blocks[(x, y, z)] = (name, new, nbt)


# =====================================================================================================================
# checks
# =====================================================================================================================
# blocks you walk through
DOOR = {mc(f"{w}_door") for w in ("crimson", "dark_oak", "spruce")}
OPEN = sb.PASS | DOOR | {mc("ladder"), mc("bell"), mc("crimson_trapdoor"), mc("crimson_fence_gate"), mc("flower_pot"), mc("potted_crimson_fungus"),
                         mc("potted_red_mushroom"), mc("potted_dead_bush"), mc("white_carpet"), mc("yellow_carpet"), mc("orange_carpet"),
                         mc("lime_carpet"), mc("light_blue_carpet"), mc("brown_carpet"), mc("magenta_carpet"), mc("purple_carpet"),
                         mc("crimson_wall_sign"), mc("crimson_sign"), mc("crimson_pressure_plate"), mc("crimson_button"), mc(BREIWERK),
                         mc("red_wall_banner"), mc("white_wall_banner"), mc("yellow_wall_banner"), mc("black_wall_banner")}
LAAG = {mc(VOERBAK), mc("cauldron"), mc("composter")}                # things you can't stand in but that aren't a wall


def vast(name):
    """Does this block carry and stop you (a wall, a floor)?"""
    return name is not None and name not in OPEN and name != mc(sb.SAUS)


def staplekken(b):
    blocks = b.s.blocks

    def nm(c):
        v = blocks.get(c)
        return v[0] if v else None
    cells = set()
    for (x, y, z), v in blocks.items():
        if not vast(v[0]) or vast(nm((x, y + 1, z))) or vast(nm((x, y + 2, z))):
            continue
        n = v[0]
        if n in (mc(HEK), mc(MUUR), mc(TRALIES), mc(LEUNING)) or n.endswith("_fence") or n.endswith("_pane") or n.endswith("_bed") or n in LAAG:
            continue
        cells.add((x, y + 1, z))
    return cells


def bereik(b, start, extra=()):
    """Every cell you can get to from `start`: walking (a step up or down), through doors, up and down ladders. `extra`:
    cells that count as floor although they are open (the planks of the bridge when it is mended)."""
    blocks = b.s.blocks
    cells = staplekken(b) | set(extra)
    ladders = {c for c, v in blocks.items() if v[0] == mc("ladder")}
    if start not in cells:
        return set(), cells
    seen = {start}
    todo = [start]
    while todo:
        x, y, z = todo.pop()
        here = (x, y, z)
        stap = []
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1, -2, -3):
                n = (x + dx, y + dy, z + dz)
                if n in cells:
                    if dy == 1 and vast(blocks.get((x, y + 2, z), (None,))[0]):
                        continue
                    if dy < 0 and any(vast(blocks.get((x + dx, yy, z + dz), (None,))[0]) for yy in range(y + dy + 2, y + 2)):
                        continue
                    stap.append(n)
                    break
            # onto a ladder next to you (at your feet or one down), and off it at any height
            for dy in (0, -1):
                n = (x + dx, y + dy, z + dz)
                if n in ladders:
                    stap.append(n)
        if here in ladders:
            for dy in (1, -1):
                n = (x, y + dy, z)
                if n in ladders or n in cells:
                    stap.append(n)
            if (x, y + 1, z) not in ladders and not vast(blocks.get((x, y + 1, z), (None,))[0]):
                stap.append((x, y + 1, z))                   # (out of the top of the ladder: the hatch cell)
        elif here not in cells:
            # the cell just above a ladder: step sideways onto a floor at this height
            pass
        if (x, y - 1, z) in ladders:
            stap.append((x, y - 1, z))
        for n in stap:
            if n not in seen:
                seen.add(n)
                todo.append(n)
    return seen, cells


def check(b, start, extra=()):
    """The shared checks of sb plus our own: the quest characters stand on a floor with room, the named spots can be reached."""
    naam = b.naam
    problems = sb.check_common(b, naam)
    blocks = b.s.blocks

    def nm(c):
        v = blocks.get(c)
        return v[0] if v else None
    for (x, y, z, wat) in b.wezens:
        if not vast(nm((x, y - 1, z))) or vast(nm((x, y, z))) or vast(nm((x, y + 1, z))):
            problems.append(f"{naam}: {wat} at {(x, y, z)} has no room to stand (on {nm((x, y - 1, z))}, in {nm((x, y, z))})")
    for c in b.vloer:
        if not vast(nm((c[0], c[1] - 1, c[2]))):
            problems.append(f"{naam}: nothing under {nm(c)} at {c}")
    seen, cells = bereik(b, start, extra)
    if not seen:
        problems.append(f"{naam}: the start {start} is not a place to stand")
        return problems
    for wat, cell in b.must_reach.items():
        if cell not in seen:
            problems.append(f"{naam}: can't get from {start} to {wat} at {cell}")
    return problems


# =====================================================================================================================
# room round a build, and the seams of a guhs:burcht (both found on a dev server: /guhs bouwcheck ... compleet)
# =====================================================================================================================
def hoogste(b, vanaf):
    """{(x, z): the highest y >= vanaf with a block of the build that is not air}."""
    hoog = {}
    for (x, y, z), v in b.s.blocks.items():
        if y >= vanaf and v[0] != AIR and y > hoog.get((x, z), -1):
            hoog[(x, z)] = y
    return hoog


def lucht(b, bereik_):
    """Carves the cave round a build: every cell the build left alone becomes air, per column from y0 up to y1 =
    bereik_(x, z) (None: not in this column). The Barbecuether is one big cave and the placement of a palace only looks at a
    few columns: where a copy lands half in the rock, its roofs, galleries and face would be buried, and since the building
    is protected nobody could dig their way to them (the first real copy of the flats had its roof garden full of rock)."""
    sx, sy, sz = b.s.size
    for x in range(sx):
        for z in range(sz):
            r = bereik_(x, z)
            if r is None:
                continue
            for y in range(max(0, r[0]), min(sy - 1, r[1]) + 1):
                if b.get(x, y, z) is None:
                    b.set(x, y, z, AIR)


def rondom(hoog, x, z, r=2):
    """The highest block of the build within r columns of (x, z), or None."""
    best = None
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            y = hoog.get((x + dx, z + dz))
            if y is not None and (best is None or y > best):
                best = y
    return best


RICHTING = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
# shapes that carry nothing that hangs on their side
GEEN_STEUN = ("_stairs", "_slab", "_fence", "_fence_gate", "_wall", "_pane", "_door", "_trapdoor", "_bed", "_carpet", "_sign", "_banner",
              "lantern", "campfire", "chain", "ladder", "bell", "cauldron", "composter", "_trap", "_plaat", "_muur", "_hek", "_tralies",
              "_brugleuning", "_breiwerk", "_voerbak", "_mikakop")


def steunt(name):
    """Can a ladder or a wall sign hang on the side of this block?"""
    return vast(name) and name != AIR and not name.endswith(GEEN_STEUN)


def naadparen(a, n, tegel):
    """The c along one axis for which the blocks c and c + 1 can lie on two sides of a seam of a guhs:burcht. A burcht is
    placed tile by tile and chunk by chunk, and after each bit the game lets the blocks of that bit look at their neighbours:
    a ladder whose wall, or half a bed whose other half, is in a bit that comes later breaks off. The seams are the tile
    edges and the chunk edges; the anchor `a` stands on block 8 of its chunk, and a turned copy moves the chunk edges one up."""
    return {c for c in range(-1, n) if (c + 1) % tegel == 0 or (c - a) % 16 in (7, 8)}


def check_steun(b, anker=None, tegel=sb.TILE):
    """Everything that hangs on the block beside it (ladders, wall signs, wall banners) or is two blocks long (beds) has that
    neighbour, and, for a burcht (anker given), not across a seam."""
    naam = b.naam
    nx = naadparen(anker[0], b.s.size[0], tegel) if anker else set()
    nz = naadparen(anker[2], b.s.size[2], tegel) if anker else set()
    problems = []
    for (x, y, z), (name, props, _nbt) in b.s.blocks.items():
        if name == mc("ladder") or name.endswith(("_wall_sign", "_wall_banner", "wall_torch")):
            dx, dz = RICHTING[props["facing"]]
            buur = (x - dx, y, z - dz)
            if not steunt(b.get(*buur)):
                problems.append(f"{naam}: {name} at {(x, y, z)} hangs on {b.get(*buur)}")
        elif name.endswith("_bed"):
            dx, dz = RICHTING[props["facing"]]
            s_ = 1 if props["part"] == "foot" else -1
            buur = (x + s_ * dx, y, z + s_ * dz)
            if b.get(*buur) != name:
                problems.append(f"{naam}: half a bed at {(x, y, z)}")
        else:
            continue
        if (buur[0] != x and min(x, buur[0]) in nx) or (buur[2] != z and min(z, buur[2]) in nz):
            problems.append(f"{naam}: {name} at {(x, y, z)} and its neighbour {buur} lie on two sides of a seam")
    return problems


# =====================================================================================================================
# Het Mika-brugpaleis
# =====================================================================================================================
BR = (96, 60, 31)
BR_CZ = 15
BR_Y = 28                      # the deck (template y); the piers go down to y 0
# the anchor (it stands on block 8 of its chunk): chosen with scratch/paleizen/anker.py so that no ladder, wall sign or bed lies
# on a seam (check_steun) and as few fence and bar arms as possible reach across one
BR_ANKER = (55, BR_Y, BR_CZ + 1)
POORT = (34, 48)               # the tolhuis (x from .. to)
GAT = (58, 62)                 # the five missing rows of planks (x from .. to), each BR_CZ - 2 .. BR_CZ + 2
TOREN = (76, 88)               # the bell tower
STEIGER = BR_Y - 3             # the floor of the scaffold under the gap


def brugpaleis(h):
    b = Paleis(h, BR, 21300901, "mika_brugpaleis")
    br_pijlers(b)
    br_dek(b)
    br_opritten(b)
    br_tolhuis(b)
    br_gat(b)
    br_klokkentoren(b)
    br_aankleding(b)
    br_lucht(b)
    b.connect()
    Y, cz = BR_Y, BR_CZ
    b.plek.update({"anker": BR_ANKER, "tolpoort": (38, Y + 1, cz - 2, 46, Y + 6, cz + 2), "tolpoort_buiten": (30, Y + 1, cz),
                   "gat": (GAT[0], Y, cz - 2, GAT[1], Y, cz + 2), "bel": (82, Y + 5, cz), "steiger": (60, STEIGER + 1, cz)})
    b.must_reach.update({"the Tolwachter": (35, Y + 1, cz), "the toll office": (40, Y + 1, cz - 6), "the Tolwachter's room": (40, Y + 8, cz),
                         "the edge of the gap": (57, Y + 1, cz), "the far side": (64, Y + 1, cz), "the bell": (82, Y + 1, cz),
                         "the east end": (93, Y - 2, cz), "the attic of the tolhuis": (40, Y + 14, cz)})
    return b


def br_pijlers(b):
    """The piers down to y 0 and the arches between them."""
    Y, cz = BR_Y, BR_CZ
    piers = [(12, 14), (24, 26), (53, 55), (65, 67)]
    for (x0, x1) in piers:
        for x in range(x0, x1 + 1):
            for z in range(cz - 3, cz + 4):
                for y in range(0, Y - 1):
                    rand = abs(z - cz) == 3 or x in (x0, x1)
                    b.set(x, y, z, GEBEITELD if y % 7 == 3 and rand else b.brick(0.15))
        for z in (cz - 4, cz + 4):                                  # a foot and a collar
            for x in range(x0, x1 + 1):
                for y in range(0, 4):
                    b.set(x, y, z, b.brick(0.2))
                b.stair(x, 4, z, "south" if z < cz else "north")
                b.stair(x, Y - 2, z, "south" if z < cz else "north", half="top")
    # the big piers of the tolhuis and the bell tower
    for (x0, x1, half) in ((POORT[0] + 2, POORT[1] - 2, 7), (TOREN[0] + 1, TOREN[1] - 1, 5)):
        for x in range(x0, x1 + 1):
            for z in range(cz - half, cz + half + 1):
                rand = x in (x0, x1) or abs(z - cz) == half
                hoek = x in (x0, x1) and abs(z - cz) == half
                for y in range(0, Y):
                    if hoek:
                        b.set(x, y, z, PILAAR, {"axis": "y"})
                    elif rand:
                        b.set(x, y, z, GEPOLIJST if y % 6 == 0 else GEBEITELD if y % 6 == 3 and (x + z) % 3 == 0 else ROOSTER)
                    elif y >= Y - 3 or y % 6 == 0:
                        b.set(x, y, z, ROOSTER)
        # glowing slits in the long sides of the pier
        for x in range(x0 + 2, x1 - 1, 3):
            for z in (cz - half, cz + half):
                for y in (Y - 12, Y - 11):
                    b.set(x, y, z, GLOEIKOOL)
    # the arches under the deck: from pier to pier (and to the abutments at the ends)
    spans = [(6, 11), (15, 23), (27, POORT[0] + 1), (POORT[1] - 1, 52), (56, 64), (68, TOREN[0]), (TOREN[1], 89)]
    for (a, e) in spans:
        n = e - a
        for i in range(n + 1):
            x = a + i
            rise = int(round(3.4 * math.sin(math.pi * i / max(1, n))))
            for z in range(cz - 3, cz + 4):
                for d in range(2, 6 - rise):
                    if abs(z - cz) == 3 or d < 4 - rise or i in (0, n):
                        b.set(x, Y - d, z, b.brick(0.2))


def br_dek(b):
    """The deck from end to end: a walkway five wide between two kerbs with grill bars, lamp posts and Mika heads."""
    Y, cz = BR_Y, BR_CZ
    for x in range(6, 90):
        for dz in range(-3, 4):
            z = cz + dz
            if abs(dz) == 3:
                b.set(x, Y, z, GEPOLIJST)
                b.bars(x, Y + 1, z)
                for y in range(Y + 2, Y + 6):
                    b.set(x, y, z, AIR)
            else:
                b.set(x, Y, z, GEPOLIJST if dz == 0 or x % 4 == 0 else ROOSTER)
                for y in range(Y + 1, Y + 6):
                    b.set(x, y, z, AIR)
            b.set(x, Y - 1, z, b.brick())
    # lamp posts (a pillar with a lantern) and Mika heads on the kerbs
    for i, x in enumerate((9, 17, 21, 29, 51, 56, 64, 69, 73)):
        for dz in (-3, 3):
            z = cz + dz
            if i % 2 == 0:
                b.pilaar(x, Y + 1, Y + 3, z)
                b.lantern(x, Y + 4, z)
            else:
                b.set(x, Y + 1, z, GEPOLIJST)
                b.mikakop(x, Y + 2, z, "south" if dz > 0 else "north")


def br_opritten(b):
    """Both ends: a broad stair of six steps down on a solid abutment, between low walls."""
    Y, cz = BR_Y, BR_CZ
    for end in (0, 1):
        for i in range(6):
            x = 5 - i if end == 0 else 90 + i
            top = Y - i
            for z in range(cz - 3, cz + 4):
                for y in range(0, top):
                    b.set(x, y, z, GEBEITELD if y % 7 == 3 and abs(z - cz) == 3 else b.brick(0.15))
                if abs(z - cz) == 3:
                    b.set(x, top, z, GEPOLIJST)
                    b.wall(x, top + 1, z)
                    for y in range(top + 2, top + 6):
                        b.set(x, y, z, AIR)
                else:
                    b.stair(x, top, z, "east" if end == 0 else "west")
                    for y in range(top + 1, top + 6):
                        b.set(x, y, z, AIR)
        # two gate posts with fire at the foot of the stair
        x = 0 if end == 0 else 95
        for z in (cz - 3, cz + 3):
            b.pilaar(x, Y - 4, Y - 2, z)
            b.set(x, Y - 1, z, GLOEIKOOL)


GEZICHT = [  # the tolhuis seen from the west: u = -9 .. 9 from left to right, the rows from v = 17 down to v = 1
    "...................",
    ".GG.............GG.",
    "..GGG.........GGG..",
    "....GGG.....GGG....",
    "...RRRR.....RRRR...",
    "...RRYY.....YYRR...",
    "...RRYY.....YYRR...",
    "...RRRR.....RRRR...",
    "........N.N........",
    "...................",
    "......LLLLLLL......",
    "......LW   WL......",
    "......LW   WL......",
    "......L     L......",
    "......L     L......",
    "......L     L......",
    "......L     L......",
]
GEZICHT_BLOK = {".": "black_concrete", "G": "gray_concrete", "R": "red_concrete", "Y": GLOEIKOOL, "N": "gray_concrete",
                "L": "red_nether_bricks", "W": "white_concrete"}


def br_gezicht(b, x, voor):
    """The giant Mika face on the west wall of the tolhuis (wall x, relief at `voor`): the gate is its open mouth, two
    fangs hang in its corners, the brows and the upper lip stick out."""
    Y, cz = BR_Y, BR_CZ
    for i, row in enumerate(GEZICHT):
        v = 17 - i
        for j, ch in enumerate(row):
            u = 9 - j                                              # (seen from the west, left is the south: +z)
            if ch == " ":
                b.set(x, Y + v, cz + u, AIR)
                continue
            b.set(x, Y + v, cz + u, GEZICHT_BLOK[ch])
            if ch == "G" or (ch == "L" and v == 7) or (ch == "W"):
                b.set(voor, Y + v, cz + u, GEZICHT_BLOK[ch])
    for u in (-1, 1):                                                # small teeth between the fangs
        b.set(voor, Y + 6, cz + u, "white_concrete")


def br_tolhuis(b):
    """The tolhuis: the passage through the Mika's mouth, the toll office and the waiting room beside it, the Tolwachter's
    room above, an attic and a roof with battlements and two great horns."""
    Y, cz = BR_Y, BR_CZ
    x0, x1 = POORT
    z0, z1 = cz - 9, cz + 9
    top = Y + 18
    floors = (Y + 7, Y + 13)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            hoek = x in (x0, x1) and z in (z0, z1)
            b.set(x, Y, z, GEPOLIJST if (x + z) % 2 else ROOSTER)
            b.set(x, Y - 1, z, ROOSTER)
            for y in range(Y + 1, top):
                if hoek:
                    b.set(x, y, z, PILAAR, {"axis": "y"})
                elif rand:
                    b.set(x, y, z, GEPOLIJST if y in (Y + 1, Y + 7, Y + 13) else ROOSTER)
                elif y in floors:
                    b.set(x, y, z, GEPOLIJST)
                else:
                    b.set(x, y, z, AIR)
            b.set(x, top, z, GEPOLIJST)
    for layer in range(6):                                           # a hipped roof of red nether brick
        for x in range(x0 + layer, x1 + 1 - layer):
            for z in range(z0 + layer, z1 + 1 - layer):
                d = min(x - x0, x1 - x, z - z0, z1 - z) - layer
                if d <= 1 or layer == 5:
                    b.set(x, top + 1 + layer, z, DAK if layer < 5 else GEPOLIJST)
    # corbels under the overhang (the house is wider than its pier)
    for x in range(x0, x1 + 1):
        for z in (z0, z0 + 1, z1 - 1, z1):
            b.stair(x, Y - 2, z, "south" if z < cz else "north", half="top", block="minecraft:polished_blackstone_brick_stairs")
    for z in range(z0, z1 + 1):
        for x in (x0, x0 + 1, x1 - 1, x1):
            b.stair(x, Y - 2, z, "east" if x < 40 else "west", half="top", block="minecraft:polished_blackstone_brick_stairs")
    # the passage, with the walls of the two side rooms
    for x in range(x0, x1 + 1):
        for z in range(cz - 2, cz + 3):
            for y in range(Y + 1, Y + 7):
                b.set(x, y, z, AIR)
        if x0 < x < x1:
            for z in (cz - 3, cz + 3):
                for y in range(Y + 1, Y + 7):
                    b.set(x, y, z, GEPOLIJST if y in (Y + 1, Y + 6) else ROOSTER)
    b.set(x0, Y, cz, GEPOLIJST)
    br_gezicht(b, x0, x0 - 1)
    # the east side of the passage: a plain arch with a lantern and a goodbye sign
    for u in range(-3, 4):
        b.set(x1, Y + 7, cz + u, GEBEITELD if abs(u) != 3 else GEPOLIJST)
    b.sign(x1 + 1, Y + 4, cz + 3, "east", ["paleizen.brug.dag1", "paleizen.brug.dag2", "paleizen.brug.dag3"])
    for x in (x0 + 4, x1 - 4):
        b.hang_lantern(x, Y + 7, cz, 1, soul=True)
    # --- the toll office (north) and the waiting room (south): a door each, the office with a counter window ---
    for (zw, zin) in ((cz - 3, -1), (cz + 3, 1)):
        for y in (Y + 1, Y + 2):
            b.set(44, y, zw, AIR)
        b.door(44, Y + 1, zw, "south" if zin < 0 else "north")
    for x in (37, 38, 39):                                           # the counter window of the office, grill bars over it
        b.set(x, Y + 2, cz - 3, AIR)
        b.bars(x, Y + 3, cz - 3)
        b.set(x, Y + 1, cz - 3, GEPOLIJST)
    # the office: sacks and barrels of toll knabbels, a chest, a desk, the ladder up
    for (x, z) in ((36, cz - 8), (37, cz - 8), (36, cz - 7)):
        b.set(x, Y + 1, z, KNABBELS)
    b.set(36, Y + 2, cz - 8, KNABBELS)
    b.chest(39, Y + 1, cz - 8, "south", "guhs:chests/paleizen_tolhuis")
    b.chest(41, Y + 1, cz - 8, "south", "guhs:chests/paleizen_tolhuis", barrel=True)
    b.set(38, Y + 1, cz - 5, "minecraft:crimson_stairs", {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    b.hang_lantern(41, Y + 7, cz - 6, 1)
    b.sign(42, Y + 3, cz - 4, "north", ["paleizen.brug.kantoor1", "paleizen.brug.kantoor2", "paleizen.brug.kantoor3"])
    b.ladder(46, Y + 1, floors[1] + 1, cz - 8, "south")              # (on the north wall)
    # the waiting room: a bench, a carpet, a price list
    for x in (36, 37, 38, 39):
        b.set(x, Y + 1, cz + 8, "minecraft:crimson_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for x in range(36, 42):
        for z in (cz + 5, cz + 6):
            b.carpet(x, Y + 1, z, "red")
    b.sign(40, Y + 3, cz + 4, "south", ["paleizen.brug.wacht1", "paleizen.brug.wacht2", "paleizen.brug.wacht3"])
    b.hang_lantern(41, Y + 7, cz + 6, 1)
    b.set(46, Y + 1, cz + 8, GRILLKOOL)
    b.campfire(46, Y + 2, cz + 8)
    # windows of the side rooms
    for x in (38, 41, 44):
        for z in (z0, z1):
            b.bars(x, Y + 3, z)
            b.bars(x, Y + 4, z)
    # --- the Tolwachter's room (first floor): his bed, a table, the heap of toll he "saved up", windows ---
    F = Y + 7
    for layer in range(3):
        r = 2 - layer
        for x in range(39 - r, 39 + r + 1):
            for z in range(cz + 4 - r, cz + 4 + r + 1):
                b.set(x, F + 1 + layer, z, KNABBELS)
    b.sign(42, F + 1, cz + 4, "east", ["paleizen.brug.spaar1", "paleizen.brug.spaar2", "paleizen.brug.spaar3"], wall=False)
    b.bed(36, F + 1, cz - 7, "east", "red")
    b.set(36, F + 1, cz - 5, "minecraft:crimson_fence", {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
    b.set(36, F + 2, cz - 5, "minecraft:crimson_pressure_plate", {"powered": "false"})
    b.chest(44, F + 1, cz + 8, "north", "guhs:chests/paleizen_tolhuis")
    for x in range(40, 44):
        for z in range(cz - 2, cz + 1):
            b.carpet(x, F + 1, z, "red")
    for x in (38, 41, 44):
        for z in (z0, z1):
            b.bars(x, F + 3, z)
            b.bars(x, F + 4, z)
    for z in (cz - 5, cz, cz + 5):
        b.bars(x1, F + 3, z)
        b.bars(x1, F + 4, z)
    b.hang_lantern(41, floors[1], cz, 2)
    # --- the attic: stores ---
    A = floors[1]
    for (x, z) in ((36, cz + 7), (37, cz + 7), (36, cz + 6), (40, cz - 8), (41, cz - 8)):
        b.set(x, A + 1, z, "barrel", {"facing": "up", "open": "false"})
    b.set(36, A + 2, cz + 7, "barrel", {"facing": "up", "open": "false"})
    for z in (cz - 5, cz + 5):
        b.bars(x1, A + 2, z)
    b.lantern(44, A + 1, cz)
    # --- the roof: a fire bowl on the flat top and two great horns over the face ---
    b.set(41, top + 7, cz, GRILLKOOL)
    b.campfire(41, top + 8, cz, signal=True)
    for s in (-1, 1):
        z = cz + s * 6
        for dx in (0, 1):
            b.set(x0 + dx, top + 1, z, GEPOLIJST)
            b.set(x0 + dx, top + 1, z - s, GEPOLIJST)
            b.set(x0 + dx, top + 2, z, GEPOLIJST)
        b.hoorns(x0, top + 3, z, s, along_x=False)
        b.hoorns(x0 + 1, top + 3, z, s, along_x=False)
    # two turrets beside the face, on brackets over the sauce
    for s in (-1, 1):
        zc = cz + s * 11
        for x in range(x0, x0 + 5):
            for z in range(zc - 1, zc + 2):
                rand = x in (x0, x0 + 4) or z in (zc - 1, zc + 1)
                for y in range(Y - 1, Y + 14):
                    b.set(x, y, z, GEPOLIJST if y in (Y - 1, Y + 6, Y + 13) else ROOSTER if rand else HOUTSKOOL)
                if rand and (x + z) % 2 == 0:
                    b.set(x, Y + 14, z, GEPOLIJST)
        for d in (1, 2, 3):                                           # the bracket: steps back to the pier
            for x in range(x0 + d, x0 + 5):
                for z in ((zc - 1, zc) if s > 0 else (zc, zc + 1)) if d > 1 else range(zc - 1, zc + 2):
                    b.set(x, Y - 1 - d, z, ROOSTER if d < 3 else GEPOLIJST)
        for y in (Y + 3, Y + 4, Y + 9, Y + 10):
            b.set(x0, y, zc, GLOEIKOOL)
        b.set(x0 + 2, Y + 14, zc, GRILLKOOL)
        b.campfire(x0 + 2, Y + 15, zc)
    # the Tolwachter-Mika on his little stage beside the mouth, and his raised barrier (a red-and-white pole) opposite
    b.npc(35, Y + 1, cz - 2, "tolwachter_mika", "paleizen_tolwachter", 90.0)
    for y in range(Y + 1, Y + 6):
        b.set(36, y, cz + 2, "red_concrete" if (y - Y) % 2 else "white_concrete")
    b.set(36, Y + 6, cz + 2, GEPOLIJST)
    b.sign(30, Y + 1, cz + 2, "west", ["paleizen.brug.tol1", "paleizen.brug.tol2", "paleizen.brug.tol3", "paleizen.brug.tol4"], wall=False)


def br_gat(b):
    """The broken stretch: five rows of planks are gone (the kerbs still hang there), a scaffold under it with a ladder back
    up on the west side. The rows before and after are planks too, so it is clear what is missing."""
    Y, cz = BR_Y, BR_CZ
    for x in range(GAT[0] - 3, GAT[1] + 4):
        for dz in range(-2, 3):
            z = cz + dz
            if GAT[0] <= x <= GAT[1]:
                b.set(x, Y, z, AIR)
                b.set(x, Y - 1, z, AIR)
            else:
                b.set(x, Y, z, PLANK)
    for x in range(GAT[0], GAT[1] + 1):                              # no arch under the gap: you look down at the scaffold
        for z in range(cz - 2, cz + 3):
            for d in range(2, 6):
                if b.get(x, Y - d, z) not in (None, AIR):
                    b.set(x, Y - d, z, AIR)
    # the scaffold: a plank floor hanging on posts from the kerbs, a rail round it
    S = STEIGER
    for x in range(GAT[0] - 1, GAT[1] + 2):
        for z in range(cz - 3, cz + 4):
            b.set(x, S, z, HOUT_PLAAT, {"type": "top", "waterlogged": "false"})
            for y in range(S + 1, Y - 1):
                if b.get(x, y, z) not in (None, AIR) and abs(z - cz) < 3:
                    b.set(x, y, z, AIR)
    for x in (GAT[0] - 1, GAT[0] + 2, GAT[1] + 1):
        for z in (cz - 3, cz + 3):
            for y in range(S + 1, Y - 1):
                b.hfence(x, y, z)
    for x in range(GAT[0], GAT[1] + 1):
        for z in (cz - 3, cz + 3):
            if b.get(x, S + 1, z) in (None, AIR):
                b.hfence(x, S + 1, z)
    for z in range(cz - 2, cz + 3):
        b.hfence(GAT[1] + 1, S + 1, z)
        b.set(GAT[1] + 1, S + 2, z, AIR)
    # the ladder back up: through a hatch in the deck just west of the gap
    lx = GAT[0] - 1
    for y in (Y, Y - 1):
        b.set(lx, y, cz - 2, AIR)
    for y in range(S + 1, Y + 1):
        b.set(lx - 1, y, cz - 2, b.brick(0.0) if y < Y else PLANK)
    b.ladder(lx, S + 1, Y, cz - 2, "east")
    for z in range(cz - 2, cz + 3):
        if z != cz - 2:
            b.set(lx, S + 1, z, AIR)
    b.sign(GAT[0] - 3, Y + 1, cz + 2, "west", ["paleizen.brug.gat1", "paleizen.brug.gat2", "paleizen.brug.gat3"], wall=False)
    b.sign(GAT[1], S + 1, cz + 2, "west", ["paleizen.brug.steiger1", "paleizen.brug.steiger2", "paleizen.brug.steiger3"], wall=False)
    b.lantern(GAT[0] + 2, S + 1, cz - 2)


def br_klokkentoren(b):
    """The bell tower: the passage with the tolbel hanging in it, an alcove with a chest, a closed shaft and an open belfry
    with a great golden bell under a pointed roof."""
    Y, cz = BR_Y, BR_CZ
    x0, x1 = TOREN
    z0, z1 = cz - 6, cz + 6
    cx = (x0 + x1) // 2
    shaft = Y + 12
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            hoek = x in (x0, x1) and z in (z0, z1)
            b.set(x, Y, z, GEPOLIJST if (x + z) % 2 else ROOSTER)
            b.set(x, Y - 1, z, ROOSTER)
            for y in range(Y + 1, shaft):
                if hoek:
                    b.set(x, y, z, PILAAR, {"axis": "y"})
                elif rand:
                    b.set(x, y, z, GEPOLIJST if y in (Y + 1, Y + 6) else ROOSTER)
                elif y == Y + 6:
                    b.set(x, y, z, GEPOLIJST)
                else:
                    b.set(x, y, z, AIR)
            b.set(x, shaft, z, GEPOLIJST)
    for x in (x0, x1):
        for z in (z0, z1):
            b.stair(x, Y - 2, z, "south" if z < cz else "north", half="top", block="minecraft:polished_blackstone_brick_stairs")
    for z in range(z0, z1 + 1):
        for x in (x0, x1):
            b.stair(x, Y - 2, z, "east" if x == x0 else "west", half="top", block="minecraft:polished_blackstone_brick_stairs")
    # the passage
    for x in (x0, x1):
        for z in range(cz - 2, cz + 3):
            for y in range(Y + 1, Y + 6):
                b.set(x, y, z, AIR)
        for z in (cz - 3, cz + 3):
            for y in range(Y + 1, Y + 7):
                b.set(x, y, z, GEBEITELD if y in (Y + 1, Y + 6) else GEPOLIJST)
    # the tolbel in the middle of the passage, on a short chain from the ceiling, in a ring of red carpet
    b.bel(cx, Y + 5, cz)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx or dz:
                b.carpet(cx + dx, Y + 1, cz + dz, "red")
    b.sign(cx, Y + 1, cz + 4, "north", ["paleizen.brug.bel1", "paleizen.brug.bel2", "paleizen.brug.bel3"], wall=False)
    # an alcove with the toll chest, barrels and a brazier
    b.chest(cx - 3, Y + 1, cz - 5, "south", "guhs:chests/paleizen_brug")
    b.set(cx - 4, Y + 1, cz - 5, "barrel", {"facing": "up", "open": "false"})
    b.set(cx + 4, Y + 1, cz - 5, GRILLKOOL)
    b.campfire(cx + 4, Y + 2, cz - 5)
    b.lantern(cx + 4, Y + 1, cz + 5)
    b.lantern(cx - 4, Y + 1, cz + 5)
    # slits in the shaft
    for y in (Y + 8, Y + 9, Y + 10):
        for (x, z) in ((cx, z0), (cx, z1), (x0, cz), (x1, cz)):
            b.set(x, y, z, GLOEIKOOL if y == Y + 9 else TRALIES, None if y == Y + 9 else {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
    # the belfry: four corner pillars, open arches, the great bell
    B = shaft
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            if rand:
                b.bars(x, B + 1, z)
            for y in range(B + 1, B + 7):
                if x in (x0, x0 + 1, x1 - 1, x1) and z in (z0, z0 + 1, z1 - 1, z1):
                    if (x in (x0, x1)) and (z in (z0, z1)):
                        b.set(x, y, z, PILAAR, {"axis": "y"})
            b.set(x, B + 7, z, GEPOLIJST if rand else AIR)
    for i in range(1, 12):                                            # arch heads
        for (x, z) in ((x0 + i, z0), (x0 + i, z1), (x0, z0 + i), (x1, z0 + i)):
            if i in (1, 11):
                b.set(x, B + 6, z, GEPOLIJST)
                b.set(x, B + 5, z, GEPOLIJST)
            elif i in (2, 10):
                b.set(x, B + 6, z, GEPOLIJST)
    # the roof: a stepped pyramid with a spike and two horns
    for layer in range(6):
        y = B + 7 + layer
        for x in range(x0 + layer, x1 + 1 - layer):
            for z in range(z0 + layer, z1 + 1 - layer):
                d = min(x - x0, x1 - x, z - z0, z1 - z) - layer
                if d <= 1 or layer == 0:
                    b.set(x, y, z, DAK if d <= 1 else GEPOLIJST)
    b.set(cx, B + 13, cz, DAK)
    b.pilaar(cx, B + 14, B + 16, cz)
    b.set(cx, B + 17, cz, GLOEIKOOL)
    for s in (-1, 1):
        b.hoorns(cx, B + 10, cz + s * 3, s, along_x=False, lang=7)
    # the great bell: gold, hanging from the roof on a chain, a dark clapper under it
    b.chain(cx, B + 7, cz)
    b.set(cx, B + 7, cz, GEPOLIJST)
    b.chain(cx, B + 6, cz)
    b.set(cx, B + 5, cz, "gold_block")
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            b.set(cx + dx, B + 4, cz + dz, "gold_block")
            b.set(cx + dx, B + 3, cz + dz, "gold_block" if (dx or dz) else AIR)
    for dx in (-2, 2):
        b.set(cx + dx, B + 3, cz, "raw_gold_block")
    for dz in (-2, 2):
        b.set(cx, B + 3, cz + dz, "raw_gold_block")
    b.chain(cx, B + 3, cz)
    b.set(cx, B + 2, cz, GLOEIKOOL)


def br_aankleding(b):
    """Signs at both ends and the banners of the tolhuis."""
    Y, cz = BR_Y, BR_CZ
    b.sign(7, Y + 1, cz + 2, "west", ["paleizen.brug.welkom1", "paleizen.brug.welkom2", "paleizen.brug.welkom3"], wall=False)
    b.sign(89, Y + 1, cz - 2, "east", ["paleizen.brug.welkom1", "paleizen.brug.welkom2", "paleizen.brug.welkom3"], wall=False)


def br_lucht(b):
    """Room round the bridge when it runs through rock: a vaulted tunnel over the deck, a dome over the tolhuis (so the Mika
    face can be seen from the deck in front of it) and one over the bell tower, and never less than three blocks of air over
    and two beside anything of the build above the deck."""
    Y, cz = BR_Y, BR_CZ
    hoog = hoogste(b, Y + 1)
    koepels = ((41, 19.0, 17.0, 31), (82, 15.0, 15.0, 31))            # x of the middle, radius along x and z, height

    def bereik_(x, z):
        dz = abs(z - cz)
        top = Y + (10, 10, 10, 10, 9, 7, 5)[dz] if dz <= 6 else None
        onder = Y + 1
        for (xm, rx, rz, h) in koepels:
            q = ((x - xm) / rx) ** 2 + ((z - cz) / rz) ** 2
            if q < 1.0:
                top = max(top or 0, Y + int(h * math.sqrt(1.0 - q)))
                onder = Y - 4                                         # (the corbels and brackets under the eaves too)
        near = rondom(hoog, x, z)
        if near is not None:
            top = max(top or 0, near + 3)
        return None if top is None else (onder, top)
    lucht(b, bereik_)


def check_brugpaleis(b):
    Y, cz = BR_Y, BR_CZ
    heel = [(x, Y + 1, z) for x in range(GAT[0], GAT[1] + 1) for z in range(cz - 2, cz + 3)]
    problems = check(b, (2, Y - 2, cz), extra=heel) + check_steun(b, BR_ANKER)
    # without the planks nobody walks from the tolhuis to the bell (the gap is the quest), but from the gap you land on the
    # scaffold and climb back to the west side
    seen, _ = bereik(b, (2, Y - 2, cz))
    if (64, Y + 1, cz) in seen:
        problems.append("mika_brugpaleis: the far side can be reached without mending the bridge")
    if (57, Y + 1, cz) not in seen or (60, STEIGER + 1, cz) not in seen:
        problems.append("mika_brugpaleis: the scaffold under the gap can't be reached from the west side")
    back, _ = bereik(b, (60, STEIGER + 1, cz))
    if (57, Y + 1, cz) not in back:
        problems.append("mika_brugpaleis: no way back up from the scaffold")
    for x in range(GAT[0], GAT[1] + 1):
        for z in range(cz - 2, cz + 3):
            if b.get(x, Y, z) not in (None, AIR):
                problems.append(f"mika_brugpaleis: the gap is not open at {(x, Y, z)}")
            if not vast(b.get(x, STEIGER, z)):
                problems.append(f"mika_brugpaleis: no scaffold under the gap at {(x, z)}")
    if b.get(*b.plek["bel"]) != mc("bell"):
        problems.append("mika_brugpaleis: the tolbel is not where PLEKKEN says")
    return problems


# =====================================================================================================================
# De Mika-stal
# =====================================================================================================================
ST = (45, 27, 41)
ST_G = 6                       # the ground layer (template y): the top block of the cave floor; under it a foot of rock
ST_MIDDEN = (22, ST_G, 20)     # the centre jigsaw
STAL_MIDDEN = "guhs:mika_stal_midden"
SCHUUR = (6, 28, 5, 19)        # the barn: x0, x1, z0, z1 (its walls)
NOK = 12                       # the ridge (z)
BOT = "minecraft:bone_block"
MODDER = "minecraft:mud"


def stal(h):
    b = Paleis(h, ST, 21300902, "mika_stal")
    st_grond(b)
    st_schuur(b)
    st_binnen(b)
    st_erf(b)
    st_wei(b)
    b.connect()
    G = ST_G
    x, y, z = ST_MIDDEN
    final = b.get(x, y, z) or mc(AS_AARDE)
    b.s.set(x, y, z, "minecraft:jigsaw", {"orientation": "up_north"},
            {"id": "minecraft:jigsaw", "name": STAL_MIDDEN, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": final,
             "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    b.must_reach.update({"the Stalknecht": (33, G + 1, 12), "box 0": (8, G + 1, 11), "box 3": (8, G + 1, 13), "the voerbak": (8, G + 1, 12),
                         "the hayloft": (9, G + 6, 16), "the paddock": (20, G + 1, 29), "behind the hay": b.plek["schuil_0"],
                         "behind the cart": b.plek["schuil_2"], "the shelter": (10, G + 1, 32)})
    return b


def st_grond(b):
    """The trampled ground: an oval of ash earth and houtskoolsteen, frayed at the edge, air above. Under it a foot of rock
    that narrows downwards: where the cave floor falls away under the edge the stable stands on a knoll, not on a floating
    plate (on the first real copies a fifth of the bottom layer hung over a dip)."""
    G = ST_G
    cx, cz = 22, 20
    for x in range(ST[0]):
        for z in range(ST[2]):
            d = math.hypot((x - cx) / 22.0, (z - cz) / 20.0)
            if d > 1.0 + b.rng.uniform(-0.05, 0.03):
                continue
            b.set(x, G, z, b.rng.choice([AS_AARDE] * 5 + [HOUTSKOOL] * 3 + [AS] + [GEBARSTEN]))
            for y in range(0, G):
                diep = G - 1 - y                                      # 0 = right under the ground layer
                if diep < 2:
                    b.set(x, y, z, HOUTSKOOL if (x + z + y) % 3 else AS_AARDE)
                elif d < 1.0 - (diep - 1) * 0.09 + ((x * 31 + z * 17 + y * 7) % 7 - 3) * 0.012:
                    b.set(x, y, z, HOUTSKOOL)
            top = ST[1] if d < 0.9 else G + 4 + int((1.0 - d) * 60)
            for y in range(G + 1, min(ST[1], top)):
                b.set(x, y, z, AIR)
    # the frayed edge leaves a loose column here and there: it would hang in the air where the ground falls away
    vast_ = {(cx, cz)}
    todo = [(cx, cz)]
    while todo:
        x, z = todo.pop()
        for n in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            if n not in vast_ and b.get(n[0], G, n[1]) is not None:
                vast_.add(n)
                todo.append(n)
    for (x, y, z) in [c for c in b.s.blocks if (c[0], c[2]) not in vast_]:
        del b.s.blocks[(x, y, z)]


def st_dak_y(z):
    """The top of the barn's roof over row z: a steep saddle roof, one up for every block in."""
    return ST_G + 13 - abs(z - NOK)


def st_schuur(b):
    """The barn: a plinth of stone, plank walls between bone-white posts, a big red saddle roof, the great door in the east
    gable under the Worstzwijntje sign, a hay door with a hoist in the west gable."""
    G = ST_G
    x0, x1, z0, z1 = SCHUUR
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            b.set(x, G, z, STENEN if rand else HOUT if 11 <= z <= 13 else AS_AARDE)
            for y in range(G + 1, st_dak_y(z)):
                if not rand:
                    b.set(x, y, z, AIR)
                elif x in (x0, x1) and z in (z0, z1) or (z in (z0, z1) and (x - x0) % 5 == 0 and y <= G + 5):
                    b.set(x, y, z, BOT, {"axis": "y"})
                elif y == G + 1:
                    b.set(x, y, z, STENEN)
                else:
                    b.set(x, y, z, HOUT)
    # the roof: stairs up both sides with an overhang, a ridge beam, bargeboards on the gables
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            y = st_dak_y(z)
            if z == NOK:
                b.set(x, y, z, DAK_PLAAT, {"type": "bottom", "waterlogged": "false"})
                b.set(x, y - 1, z, DAK)
            else:
                b.stair(x, y, z, "south" if z < NOK else "north", block=DAK_TRAP)
                if x in (x0 - 1, x1 + 1) and y - 1 > G + 5:
                    b.stair(x, y - 1, z, "north" if z < NOK else "south", half="top", block=DAK_TRAP)
    for x in range(x0, x1 + 1):                                       # windows with grill bars in the long walls
        if (x - x0) % 5 in (2, 3):
            for z in (z0, z1):
                b.bars(x, G + 3, z)
    # --- the east gable: the great door (five wide, an arch) and the Worstzwijntje sign over it ---
    for z in range(10, 15):
        for y in range(G + 1, G + 6):
            if y == G + 5 and z in (10, 14):
                b.stair(x1, y, z, "south" if z == 10 else "north", half="top", block=HOUT_TRAP)
            else:
                b.set(x1, y, z, AIR)
        b.set(x1, G, z, STENEN)
    for y in range(G + 1, G + 6):
        for z in (9, 15):
            b.set(x1, y, z, BOT, {"axis": "y"})
    sign = {G + 10: {-2: "o", -1: "p", 0: "p", 1: "p", 2: "o"},
            G + 9: {-3: "p", -2: "b", -1: "p", 0: "p", 1: "p", 2: "b", 3: "p"},
            G + 8: {-3: "p", -2: "p", -1: "s", 0: "s", 1: "s", 2: "p", 3: "p"},
            G + 7: {-3: "p", -2: "w", -1: "n", 0: "s", 1: "n", 2: "w", 3: "p"}}
    kleur = {"p": "pink_concrete", "o": "pink_terracotta", "b": "black_concrete", "s": "magenta_terracotta", "n": "black_concrete", "w": "white_concrete"}
    for y, row in sign.items():
        for dz, ch in row.items():
            b.set(x1, y, NOK + dz, kleur[ch])
    for dz in (-1, 0, 1):                                             # the snout sticks out
        b.set(x1 + 1, G + 8, NOK + dz, "magenta_terracotta")
    b.set(x1 + 1, G + 7, NOK, "magenta_terracotta")
    for z in (9, 15):
        b.set(x1 + 1, G + 4, z, "minecraft:crimson_fence", {"north": "false", "east": "false", "south": "false", "west": "true", "waterlogged": "false"})
        b.lantern(x1 + 1, G + 3, z, hanging=True)
    b.sign(x1 + 1, G + 2, 16, "east", ["paleizen.stal.deur1", "paleizen.stal.deur2", "paleizen.stal.deur3"])
    # --- the west gable: the hay door of the loft and a hoist with a bale on a chain ---
    for z in (11, 12, 13):
        for y in (G + 7, G + 8):
            b.set(x0, y, z, AIR)
        b.trapdoor(x0, G + 7, z, "east", half="bottom", open_=True)
    b.log(x0 - 1, G + 10, NOK, STAM, "x")
    b.log(x0 - 2, G + 10, NOK, STAM, "x")
    b.set(x0, G + 10, NOK, STAM, {"axis": "x"})
    for y in (G + 9, G + 8):
        b.chain(x0 - 2, y, NOK)
    b.set(x0 - 2, G + 7, NOK, HOOI, {"axis": "y"})
    # a little weather vane on the ridge: a sausage on a pin
    b.pilaar(x1, st_dak_y(NOK) + 1, st_dak_y(NOK) + 2, NOK)
    b.log(x1, st_dak_y(NOK) + 3, NOK, WORST, "x")
    b.log(x1 - 1, st_dak_y(NOK) + 3, NOK, WORST, "x")


def st_binnen(b):
    """Inside: the aisle, three boxes and a store on each side, the hayloft over the west half with its ladder."""
    G = ST_G
    x0, x1, z0, z1 = SCHUUR
    schot = (11, 16, 21)                                              # the partitions between the boxes (x)
    for (zf, zs) in ((10, range(z0 + 1, 10)), (14, range(15, z1))):
        for x in range(x0 + 1, x1):
            if x in schot:
                for z in zs:
                    b.set(x, G + 1, z, HOUT)
                    b.hfence(x, G + 2, z)
                for y in range(G + 1, G + 5):
                    b.log(x, y, zf, STAM)
            else:
                b.hfence(x, G + 1, zf)
        for z in zs:                                                  # straw on the floor of the boxes
            for x in range(x0 + 1, 21):
                if x not in schot and (x * 7 + z * 3) % 4 == 0:
                    b.carpet(x, G + 1, z, "yellow")
    for (x, zf) in ((9, 10), (14, 10), (19, 10), (9, 14), (14, 14), (19, 14), (24, 10), (24, 14)):
        b.gate(x, G + 1, zf, "north" if zf == 10 else "south", open_=(x, zf) == (19, 14))
    # the hayloft: a plank floor over the west half, a rail along its open edge, bales
    L = G + 5
    for x in range(x0 + 1, 17):
        for z in range(z0 + 1, z1):
            b.set(x, L, z, HOUT)
    for z in range(z0 + 2, z1 - 1):
        if z != 13:
            b.hfence(16, L + 1, z)
    for y in range(G + 1, G + 5):
        b.log(16, y, 13, STAM)
    b.ladder(17, G + 1, L, 13, "east")
    for (x, y, z) in ((8, L + 1, 8), (9, L + 1, 8), (8, L + 2, 8), (8, L + 1, 9), (12, L + 1, 15), (13, L + 1, 15), (12, L + 1, 16),
                      (12, L + 2, 15), (8, L + 1, 14), (14, L + 1, 9), (15, L + 1, 9)):
        b.set(x, y, z, HOOI, {"axis": "y" if (x + z) % 2 else "x"})
    b.lantern(11, L + 1, 12)
    b.plek["schuil_1"] = (9, L + 1, 16)
    # the boxes: name boards, the Worstzwijntjes (their home spots), the empty box of the runaway
    for nr, (x, z, yaw) in enumerate(((8, 7, 0.0), (13, 7, 0.0), (18, 7, 0.0), (8, 17, 180.0))):
        b.wezen(x, G + 1, z, "guhs:worstzwijntje", f"paleizen_stal_{nr}", yaw, f"zwijntje_{nr}", Stal=nr)
    for i, (x, zf) in enumerate(((11, 10), (16, 10), (21, 10), (11, 14))):   # (on the posts between the boxes)
        b.sign(x, G + 3, zf + (1 if zf == 10 else -1), "south" if zf == 10 else "north", [f"paleizen.stal.naam{i}"])
    b.sign(21, G + 3, 13, "north", ["paleizen.stal.leeg1", "paleizen.stal.leeg2", "paleizen.stal.leeg3"])
    # the Stalknecht's corner (south, second box): a bed in the straw, a lantern, his chest
    b.bed(13, G + 1, 17, "east", "lime")
    b.chest(15, G + 1, 18, "north", "guhs:chests/paleizen_stal")
    b.lantern(12, G + 1, 18)
    # the stores by the door: hay and feed on the north side, the knabbels the zwijntjes sniffed up on the south side
    for (x, y, z) in ((26, G + 1, 6), (27, G + 1, 6), (27, G + 2, 6), (27, G + 1, 7), (26, G + 1, 7), (23, G + 1, 6)):
        b.set(x, y, z, HOOI, {"axis": "y"})
    b.set(23, G + 1, 7, "barrel", {"facing": "up", "open": "false"})
    b.set(22, G + 1, 6, "composter", {"level": "3"})
    for (x, y, z) in ((27, G + 1, 18), (26, G + 1, 18), (27, G + 2, 18), (27, G + 1, 17)):
        b.set(x, y, z, KNABBELS)
    b.set(25, G + 1, 18, "barrel", {"facing": "up", "open": "false"})
    b.sign(23, G + 1, 17, "north", ["paleizen.stal.baas1", "paleizen.stal.baas2", "paleizen.stal.baas3"], wall=False)
    # the voerbak at the end of the aisle, lanterns from the beams
    b.set(7, G + 1, 12, VOERBAK, {"facing": "east", "voer": "0"})
    b.plek["voerbak"] = (7, G + 1, 12)
    for z in (11, 12, 13):
        b.log(21, G + 5, z, STAM, "z")
    b.log(21, G + 5, 10, STAM, "z")
    b.log(21, G + 5, 14, STAM, "z")
    b.lantern(21, G + 4, 12, hanging=True)
    b.lantern(26, G + 1, 10)


def st_erf(b):
    """The yard in front of the door: a paved patch, the Stalknecht-guh, a water trough, a signpost, lamp posts."""
    G = ST_G
    for x in range(29, 39):
        for z in range(7, 18):
            if (x - 29) + abs(z - 12) < 10 and b.get(x, G, z) is not None:
                b.set(x, G, z, STENEN if (x + z) % 5 else GEBARSTEN)
    b.npc(32, G + 1, 12, "stalknechtguh", "paleizen_stalknecht", -90.0)
    b.set(30, G + 1, 8, "cauldron")
    b.set(31, G + 1, 8, "barrel", {"facing": "up", "open": "false"})
    b.set(30, G + 1, 16, HOOI, {"axis": "x"})
    for (x, z) in ((36, 9), (36, 15)):
        b.hfence(x, G + 1, z)
        b.hfence(x, G + 2, z)
        b.lantern(x, G + 3, z)
    b.sign(37, G + 1, 10, "east", ["paleizen.stal.bord1", "paleizen.stal.bord2", "paleizen.stal.bord3"], wall=False)
    # a wheelbarrow: a slab and a stair with a load of straw
    b.set(34, G + 1, 17, HOUT_PLAAT, {"type": "top", "waterlogged": "false"})
    b.set(35, G + 1, 17, HOUT_TRAP, {"facing": "west", "half": "top", "shape": "straight", "waterlogged": "false"})
    b.set(34, G + 2, 17, HOOI, {"axis": "x"})


def st_wei(b):
    """The paddock south of the barn: mud and puddles inside a fence, a shelter, a stack of hay bales, a cart, a muck heap."""
    G = ST_G
    wx0, wx1, wz0, wz1 = 6, 38, 22, 36
    for x in range(wx0, wx1 + 1):
        for z in range(wz0, wz1 + 1):
            if b.get(x, G, z) is None:
                continue
            rand = x in (wx0, wx1) or z in (wz0, wz1)
            if rand:
                b.set(x, G, z, AS_AARDE)
                b.hfence(x, G + 1, z)
            else:
                r = b.rng.random()
                b.set(x, G, z, MODDER if r < 0.35 else NYLIUM if r < 0.5 else AS_AARDE)
                if b.get(x, G, z) == mc(NYLIUM) and b.rng.random() < 0.4:
                    b.set(x, G + 1, z, SCHEUTJES)
    b.gate(30, G + 1, wz0, "south")
    b.gate(31, G + 1, wz0, "south")
    for x in range(29, 33):                                           # the path from the yard to the gate
        for z in range(17, wz0 + 2):
            if b.get(x, G, z) is not None:
                b.set(x, G, z, STENEN if (x + z) % 4 else GEBARSTEN)
                if z > wz0:
                    b.set(x, G + 1, z, AIR)
    # a wallow: a shallow pool of mud with pindasaus puddles
    for (x, z) in ((18, 26), (19, 26), (20, 26), (18, 27), (19, 27), (20, 27), (21, 27), (19, 28), (20, 28)):
        b.set(x, G, z, MODDER)
        b.set(x, G + 1, z, AIR)
    for (x, z) in ((17, 25), (22, 28), (21, 25)):
        b.set(x, G, z, AS_AARDE)
        b.set(x, G + 1, z, PLASJE)
    # the shelter: a lean-to roof on four posts with hay under it
    for x in range(8, 14):
        for z in range(31, 36):
            b.set(x, G, z, AS_AARDE)
            b.set(x, G + 1, z, AIR)
            if (x, z) in ((8, 31), (13, 31), (8, 35), (13, 35)):
                for y in range(G + 1, G + 4):
                    b.log(x, y, z, STAM)
            b.set(x, G + 4, z, DAK_PLAAT, {"type": "bottom" if z < 33 else "top", "waterlogged": "false"})
    for (x, z) in ((9, 34), (10, 34), (9, 33)):
        b.set(x, G + 1, z, HOOI, {"axis": "x"})
    b.set(12, G + 1, 34, "cauldron")
    # the hay stack in the far corner (the runaway likes to hide behind it)
    for (x, y, z) in ((33, 1, 32), (34, 1, 32), (33, 1, 33), (34, 1, 33), (33, 2, 32), (34, 2, 32), (33, 2, 33), (35, 1, 32), (33, 3, 32)):
        b.set(x, G + y, z, HOOI, {"axis": "y" if (x + y) % 2 else "z"})
    for (x, z) in ((35, 34), (36, 34), (36, 33), (35, 33), (34, 34), (33, 34), (32, 34), (32, 33)):
        b.set(x, G, z, AS_AARDE)
        b.set(x, G + 1, z, AIR)
    b.plek["schuil_0"] = (35, G + 1, 34)
    # the cart: a plank bed on two axles, a load of hay, the shafts down in the mud
    for x in range(24, 28):
        for z in (30, 31):
            b.set(x, G, z, AS_AARDE)
            b.set(x, G + 1, z, HOUT_PLAAT, {"type": "top", "waterlogged": "false"})
    for x in (24, 27):
        for z in (29, 32):
            b.set(x, G, z, AS_AARDE)
            b.set(x, G + 1, z, "minecraft:polished_blackstone_wall", {"up": "true", "north": "none", "east": "none", "south": "none", "west": "none", "waterlogged": "false"})
    b.set(25, G + 2, 30, HOOI, {"axis": "x"})
    b.set(26, G + 2, 30, HOOI, {"axis": "x"})
    b.set(25, G + 2, 31, HOOI, {"axis": "x"})
    b.set(28, G, 30, AS_AARDE)
    b.set(28, G + 1, 30, HOUT_TRAP, {"facing": "west", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for (x, z) in ((25, 33), (26, 33), (25, 34), (24, 33), (23, 33), (23, 32), (23, 31)):
        b.set(x, G, z, AS_AARDE)
        b.set(x, G + 1, z, AIR)
    b.plek["schuil_2"] = (25, G + 1, 33)
    # the muck heap by the fence
    for (x, y, z) in ((8, 1, 24), (9, 1, 24), (10, 1, 24), (8, 1, 25), (9, 1, 25), (9, 2, 24)):
        b.set(x, G, z, AS_AARDE)
        b.set(x, G + y, z, "minecraft:coarse_dirt")
    b.set(10, G, 25, "minecraft:podzol", {"snowy": "false"})
    b.set(10, G + 1, 25, "brown_mushroom")
    # a piglet roots about in the mud, and where the runaway turns up when a player comes looking
    b.set(15, G, 28, AS_AARDE)
    b.set(15, G + 1, 28, AIR)
    b.wezen(15, G + 1, 28, "guhs:worstzwijntje", "paleizen_stal_4", 45.0, "zwijntje_4", Stal=4, Biggetje=b.h.Byte(1))
    b.set(22, G, 31, AS_AARDE)
    b.set(22, G + 1, 31, AIR)
    b.plek["ontsnapt"] = (22, G + 1, 31)
    for (x, z) in ((6, 22), (38, 22), (6, 36), (38, 36), (22, 36), (22, 22)):
        if b.get(x, G + 1, z) == mc(HOUT_HEK):
            b.hfence(x, G + 2, z)
            b.lantern(x, G + 3, z)


def check_stal(b):
    G = ST_G
    problems = check(b, (38, G + 1, 12)) + check_steun(b)
    jig = [c for c, v in b.s.blocks.items() if v[0] == "minecraft:jigsaw"]
    if jig != [ST_MIDDEN]:
        problems.append(f"mika_stal: the centre jigsaw is at {jig}")
    if sum(1 for w in b.wezens if w[3] == "guhs:worstzwijntje") < 5:
        problems.append("mika_stal: fewer than five Worstzwijntjes")
    if b.get(*b.plek["voerbak"]) != mc(VOERBAK):
        problems.append("mika_stal: the voerbak is not where PLEKKEN says")
    return problems


# =====================================================================================================================
# De Mika-woonblokken
# =====================================================================================================================
WB = (63, 60, 63)
WB_C = 31
WB_G = 12                      # the ground floor (template y); the base goes down to y 0, into the sauce
WB_ANKER = (WB_C - 7, WB_G, WB_C - 8)   # (not the middle: see BR_ANKER; here it keeps every bed whole)
V = 5                          # a storey: a floor and four blocks of room
BUITEN = 28                    # how far the base reaches from the middle
NETHER = "minecraft:nether_bricks"
BLACK = "minecraft:polished_blackstone_bricks"
# the five parts round the courtyard: x0, x1, z0, z1 (their outer walls), storeys, wall block, trim
C_ = WB_C
DELEN = {
    "noord": (C_ - 13, C_ + 13, C_ - 25, C_ - 15, 5, ROOSTER, GEPOLIJST),
    "ketelhuis": (C_ - 25, C_ - 14, C_ - 25, C_ - 15, 4, BLACK, GEPOLIJST),
    "saustoren": (C_ + 14, C_ + 25, C_ - 25, C_ - 15, 3, BLACK, GEPOLIJST),
    "west": (C_ - 25, C_ - 15, C_ - 14, C_ + 13, 4, STENEN, GEBEITELD),
    "oost": (C_ + 15, C_ + 25, C_ - 14, C_ + 13, 3, NETHER, DAK),
}
# the flats: part, (u0, u1) along the gallery, the door's u; "noord" runs along x, "west" and "oost" along z
FLATS = {"noord": [((C_ - 12, C_ - 1), C_ - 7), ((C_ + 1, C_ + 12), C_ + 7)],
         "west": [((C_ - 13, C_ - 2), C_ - 8), ((C_, C_ + 12), C_ + 6)],
         "oost": [((C_ - 13, C_ - 2), C_ - 8), ((C_, C_ + 12), C_ + 6)]}
KLEUREN = ("red", "orange", "yellow", "lime", "light_blue", "magenta", "white", "brown")


def woonblokken(h):
    b = Paleis(h, WB, 21300903, "mika_woonblokken")
    wb_basis(b)
    for naam in DELEN:
        wb_blok(b, naam)
    wb_galerijen(b)
    wb_trappenhuis(b, -1, 4)
    wb_trappenhuis(b, 1, 3)
    wb_flats(b)
    wb_daken(b)
    wb_poort_en_plein(b)
    wb_bewoners(b)
    wb_lucht(b)
    b.connect()
    c, G = WB_C, WB_G
    b.plek["anker"] = WB_ANKER
    b.must_reach.update({"the courtyard": (c, G + 1, c), "Mika-oma": (c - 3, G + 4 * V + 1, c - 13), "Brom-Mika": b.plek["mopper_0_voor"],
                         "Zeur-Mika": b.plek["mopper_1_voor"], "Snurk-Mika": b.plek["mopper_2_voor"],
                         "the knitting": (c + 20, G + 3 * V + 1, c + 7), "the roof of the west block": (c - 20, G + 4 * V + 1, c),
                         "the top of the east stairs": (c + 8, G + 3 * V + 1, c - 10)})
    return b


def wb_basis(b):
    """A ragged base of grill iron rising out of the sauce (wider at the top), the paved ground floor, a low wall round it."""
    c, G = WB_C, WB_G
    for x in range(WB[0]):
        for z in range(WB[2]):
            d = max(abs(x - c), abs(z - c))
            for y in range(0, G + 1):
                reach_ = BUITEN + 2 - (G - y) // 3
                if d <= reach_:
                    if y == G:
                        b.set(x, y, z, GEPOLIJST if (x // 2 + z // 2) % 2 else ROOSTER)
                    else:
                        b.set(x, y, z, PILAAR if (x + z) % 7 == 0 else ROOSTER if (x * 3 + z + y) % 4 else HOUTSKOOL, {"axis": "y"} if (x + z) % 7 == 0 else None)
            if d <= BUITEN + 2:
                for y in range(G + 1, G + 6):
                    b.set(x, y, z, AIR)
    # the low wall round the estate, a lantern post on every corner and beside the gate
    for x in range(c - BUITEN, c + BUITEN + 1):
        for z in range(c - BUITEN, c + BUITEN + 1):
            if max(abs(x - c), abs(z - c)) == BUITEN and not (z == c + BUITEN and abs(x - c) <= 3):
                b.set(x, G + 1, z, GEPOLIJST)
                if (x + z) % 4 == 0:
                    b.wall(x, G + 2, z)
    for (x, z) in ((c - BUITEN, c - BUITEN), (c + BUITEN, c - BUITEN), (c - BUITEN, c + BUITEN), (c + BUITEN, c + BUITEN), (c - 4, c + BUITEN), (c + 4, c + BUITEN)):
        b.pilaar(x, G + 1, G + 3, z)
        b.lantern(x, G + 4, z)


def wb_blok(b, naam):
    """One part: its floors and outer walls with a trim band at every floor, corner pillars and a parapet round the roof."""
    c, G = WB_C, WB_G
    x0, x1, z0, z1, n, muur, band = DELEN[naam]
    top = G + n * V
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = x in (x0, x1) or z in (z0, z1)
            hoek = x in (x0, x1) and z in (z0, z1)
            for y in range(G + 1, top):
                vloer = (y - G) % V == 0
                if hoek:
                    b.set(x, y, z, PILAAR, {"axis": "y"})
                elif rand:
                    b.set(x, y, z, band if vloer or y == G + 1 else muur if b.rng.random() > 0.06 or muur != STENEN else GEBARSTEN)
                elif vloer:
                    b.set(x, y, z, HOUT if naam in FLATS else muur)
                else:
                    b.set(x, y, z, AIR)
            if naam in FLATS and not rand:
                b.set(x, G, z, HOUT)
            b.set(x, top, z, GEPOLIJST)
            if rand:                                                 # the parapet
                b.set(x, top + 1, z, band if (x + z) % 2 == 0 or hoek else muur)
                if hoek:
                    b.set(x, top + 2, z, GEPOLIJST)
    # party walls between the flats
    if naam in FLATS:
        along_x = naam == "noord"
        (a0, a1), _ = FLATS[naam][0]
        mid = a1 + 1
        for y in range(G + 1, top):
            if along_x:
                for z in range(z0 + 1, z1):
                    b.set(mid, y, z, muur)
            else:
                for x in range(x0 + 1, x1):
                    b.set(x, y, mid, muur)


def wb_galerijen(b):
    """The galleries on the courtyard side of every storey above the ground: a walkway two wide with a rail of grill bars,
    on pillars that stand in the courtyard."""
    c, G = WB_C, WB_G
    cells = {}                                                        # (x, z) -> the highest storey that has a gallery there
    for x in range(c - 14, c + 15):
        for z in (c - 14, c - 13):
            cells[(x, z)] = 4
    for z in range(c - 14, c + 14):
        for x in (c - 14, c - 13):
            cells[(x, z)] = max(cells.get((x, z), 0), 3)
        for x in (c + 13, c + 14):
            cells[(x, z)] = max(cells.get((x, z), 0), 2)
    rail = {}
    for x in range(c - 12, c + 13):
        rail[(x, c - 12)] = 4
    for z in range(c - 12, c + 14):
        rail[(c - 12, z)] = max(rail.get((c - 12, z), 0), 3)
        rail[(c + 12, z)] = max(rail.get((c + 12, z), 0), 2)
    for k in range(1, 5):
        y = G + k * V
        for (x, z), hoog in cells.items():
            if k <= hoog:
                b.set(x, y, z, GEPOLIJST if (x + z) % 3 else ROOSTER)
        for (x, z), hoog in rail.items():
            if k <= hoog:
                b.set(x, y, z, GEPOLIJST)
                b.bars(x, y + 1, z)
        # the ends of a gallery that stops (the west one at its south end, the east one too): a rail across
        for (x, z), hoog in cells.items():
            if k <= hoog and z == c + 13 and (x, z + 1) not in cells:
                b.set(x, y, z + 1, GEPOLIJST)
                b.bars(x, y + 1, z + 1)
    # where the east gallery stops at storey 3 and 4 the north gallery gets a rail on its end (the roof of the east
    # block is the way on at storey 3; at storey 4 it is a dead end with a view)
    for z in (c - 14, c - 13):
        b.bars(c + 15, G + 4 * V + 1, z)
        b.set(c + 15, G + 4 * V, z, GEPOLIJST)
    b.bars(c + 14, G + 4 * V + 1, c - 12)
    b.bars(c + 13, G + 4 * V + 1, c - 12)
    b.set(c + 14, G + 4 * V, c - 12, GEPOLIJST)
    b.set(c + 13, G + 4 * V, c - 12, GEPOLIJST)
    for x in (c + 13, c + 14):                                        # storey 3, east: open to the roof garden, a rail to the south
        b.set(x, G + 3 * V, c - 12, GEPOLIJST)
        b.bars(x, G + 3 * V + 1, c - 12)
    # pillars under the rails
    for (x, z), hoog in rail.items():
        if (x + z) % 6 == 0 and abs(x - c) != 8:
            for y in range(G + 1, G + hoog * V):
                if b.get(x, y, z) in (None, AIR):
                    b.set(x, y, z, PILAAR, {"axis": "y"})


def wb_trappenhuis(b, kant, n):
    """A stair tower in an inner corner of the courtyard (kant -1 west, +1 east): 7 x 7, a square spiral round a solid core,
    one turn per storey, a door to every gallery on its north side. n = the highest storey it reaches."""
    c, G = WB_C, WB_G
    fx = lambda dx: c + kant * dx                                     # (mirrored for the east tower)
    z0, z1 = c - 11, c - 5
    top = G + (n + 1) * V
    for dx in range(5, 12):
        for z in range(z0, z1 + 1):
            x = fx(dx)
            rand = dx in (5, 11) or z in (z0, z1)
            hoek = dx in (5, 11) and z in (z0, z1)
            core = 7 <= dx <= 9 and z0 + 2 <= z <= z1 - 2
            for y in range(G + 1, top):
                if hoek:
                    b.set(x, y, z, PILAAR, {"axis": "y"})
                elif rand:
                    b.set(x, y, z, GEPOLIJST if (y - G) % V == 0 else ROOSTER)
                elif core:
                    b.set(x, y, z, GLOEIKOOL if (y - G) % V == 3 and (dx == 8 or z == c - 8) and not (dx == 8 and z == c - 8) else ROOSTER)
                else:
                    b.set(x, y, z, AIR)
            b.set(x, top, z, GEPOLIJST)
    # the ring: (dx, z) -> what stands there relative to a storey's floor
    N2, N3, NE = (8, z0 + 1), (7, z0 + 1), (6, z0 + 1)
    flat0 = [N2, N3, NE]
    stairs = [((6, z0 + 2), 1, "south"), ((6, z0 + 3), 2, "south"), ((6, z0 + 4), 3, "south"), ((7, z1 - 1), 4, "west"), ((8, z1 - 1), 5, "west")]
    block3 = (6, z1 - 1)
    flat5 = [(9, z1 - 1), (10, z1 - 1), (10, z0 + 4), (10, z0 + 3), (10, z0 + 2), (10, z0 + 1), (9, z0 + 1)]
    for k in range(0, n + 1):
        F = G + k * V
        for (dx, z) in flat0 + flat5:
            b.set(fx(dx), F, z, GEPOLIJST)
        if k == n:
            for (dx, z), off, face in stairs[:1]:                      # (the top landing stays open over the last steps)
                b.set(fx(dx), F, z, GEPOLIJST)
            continue
        for (dx, z), off, face in stairs:
            if face == "west" and kant > 0:
                face = "east"
            b.stair(fx(dx), F + off, z, face, block="minecraft:polished_blackstone_brick_stairs")
            if off > 1:
                b.set(fx(dx), F + off - 1, z, ROOSTER)
        b.set(fx(block3[0]), F + 3, block3[1], GEPOLIJST)
        b.set(fx(block3[0]), F + 2, block3[1], ROOSTER)
    # the doors (north side): to the courtyard at the ground, across the rail to the gallery on every storey
    for k in range(0, n + 1):
        F = G + k * V
        for y in (F + 1, F + 2):
            b.set(fx(8), y, z0, AIR)
        if k > 0:
            b.set(fx(8), F, z0 - 1, GEPOLIJST)
            b.set(fx(8), F + 1, z0 - 1, AIR)
        b.set(fx(8), F + 3, z0, GLOEIKOOL)
    # slits in the outer walls, a pointed roof with two horns
    for k in range(0, n + 1):
        F = G + k * V
        for (dx, z) in ((5, c - 8), (11, c - 8), (8, z1)):
            b.bars(fx(dx), F + 3, z)
    for layer in range(4):
        y = top + 1 + layer
        for dx in range(5 + layer, 12 - layer):
            for z in range(z0 + layer, z1 + 1 - layer):
                d = min(dx - 5, 11 - dx, z - z0, z1 - z) - layer
                if d <= 1:
                    b.set(fx(dx), y, z, DAK)
    b.pilaar(fx(8), top + 5, top + 6, c - 8)
    b.set(fx(8), top + 7, c - 8, GLOEIKOOL)
    for s in (-1, 1):
        b.hoorns(fx(8) + s * 2, top + 3, c - 8, s, along_x=True, lang=7)


def wb_flat(b, deel, u0, u1, du, k, soort):
    """Furnish one flat. Local coordinates: u along the gallery (u0 .. u1), w from the gallery wall (0) to the outer wall (8)."""
    c, G = WB_C, WB_G
    F = G + k * V
    x0, x1, z0, z1, n, muur, band = DELEN[deel]
    rng = random.Random(21300950 + sum(map(ord, deel)) * 131 + u0 * 17 + k)
    spiegel = rng.random() < 0.5
    U = u1 - u0 + 1
    if deel == "noord":
        P = lambda u, w: (u0 + (U - 1 - u if spiegel else u), z1 - 1 - w)
        uit, links = "north", ("east" if spiegel else "west")
    elif deel == "west":
        P = lambda u, w: (x1 - 1 - w, u0 + (U - 1 - u if spiegel else u))
        uit, links = "west", ("south" if spiegel else "north")
    else:
        P = lambda u, w: (x0 + 1 + w, u0 + (U - 1 - u if spiegel else u))
        uit, links = "east", ("south" if spiegel else "north")
    rechts = sb.rot_face(links, 2)
    binnen = sb.rot_face(uit, 2)
    ud = (du - u0)
    ud = U - 1 - ud if spiegel else ud                                # the door's u in local terms

    def zet(u, w, dy, name, props=None, nbt=None):
        x, z = P(u, w)
        b.set(x, F + dy, z, name, props, nbt)

    # the door and a little window beside it (in the gallery wall), a doormat outside
    dx_, dz_ = P(ud, -1)
    b.door(dx_, F + 1, dz_, uit, hinge="left" if rng.random() < 0.5 else "right")
    wx, wz = P(ud + (2 if ud + 2 < U else -2), -1)
    b.pane(wx, F + 2, wz, rng.choice(("orange", "orange", "yellow")))
    mx, mz = P(ud, -2)
    b.carpet(mx, F + 1, mz, rng.choice(KLEUREN))
    # the windows in the outer wall: two wide, two high, lit
    for pair in ((2, 3), (U - 4, U - 3)):
        glas = rng.choice(("orange", "orange", "orange", "yellow", "yellow", "gray"))
        for u in pair:
            for dy in (2, 3):
                x, z = P(u, 9)
                b.pane(x, F + dy, z, glas)
    # the bed in a corner by the outer wall, the stove and stores in the other corner
    kleur = rng.choice(KLEUREN)
    x, z = P(0, 7)
    b.bed(x, F + 1, z, uit, kleur if kleur != "brown" else "red")
    zet(U - 1, 8, 1, "smoker", {"facing": links, "lit": "true" if rng.random() < 0.4 else "false"})
    zet(U - 1, 7, 1, "barrel", {"facing": "up", "open": "false"})
    if soort == "oma" or rng.random() < 0.3:
        x, z = P(U - 1, 6)
        b.chest(x, F + 1, z, links, "guhs:chests/paleizen_woonblok")
    # a table under the window with a chair, something on the table
    zet(U // 2, 8, 1, HOUT_HEK, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})
    zet(U // 2, 8, 2, "crimson_pressure_plate", {"powered": "false"})
    zet(U // 2 - 1, 8, 1, HOUT_TRAP, {"facing": links, "half": "bottom", "shape": "straight", "waterlogged": "false"})
    # a carpet in the middle, a lantern over it, a plant in the corner by the gallery wall
    tapijt = rng.choice(KLEUREN)
    for u in range(U // 2 - 1, U // 2 + 2):
        for w in (3, 4, 5):
            x, z = P(u, w)
            b.carpet(x, F + 1, z, tapijt)
    x, z = P(U // 2, 4)
    b.hang_lantern(x, F + V, z, 1, soul=rng.random() < 0.25)
    if ud != 0:
        zet(0, 0, 1, rng.choice(("potted_crimson_fungus", "potted_red_mushroom", "potted_dead_bush")))
    # a couch facing a "telly" (a black slab of concrete) on the side wall
    if U >= 12 and soort != "oma":
        zet(U - 1, 3, 2, "black_concrete")
        zet(U - 1, 3, 1, HOUT)
        for w in (3, 4):
            zet(U - 4, w, 1, HOUT_TRAP, {"facing": links, "half": "bottom", "shape": "straight", "waterlogged": "false"})
    if soort == "oma":
        # Mika-oma's flat: balls of wool everywhere, a loom, a cake on the table, flower pots, a second carpet
        for (u, w, dy, wol) in ((3, 8, 1, "pink_wool"), (4, 8, 1, "white_wool"), (3, 8, 2, "lime_wool"), (U - 3, 1, 1, "magenta_wool"),
                                (U - 2, 1, 1, "light_blue_wool"), (U - 3, 1, 2, "yellow_wool")):
            zet(u, w, dy, wol)
        zet(U - 1, 4, 1, "loom", {"facing": links})
        zet(U // 2, 8, 2, "cake", {"bites": "2"})
        for (u, w) in ((2, 0), (U - 1, 0)):
            if u != ud:
                zet(u, w, 1, "potted_crimson_fungus")
        x, z = P(4, 8)                                                # (on the wall beside the window, over the wool)
        b.sign(x, F + 2, z, binnen, ["paleizen.woon.mand1", "paleizen.woon.mand2", "paleizen.woon.mand3"])
    return P, (ud, U, uit, links)


def wb_flats(b):
    c, G = WB_C, WB_G
    for deel, flats in FLATS.items():
        n = DELEN[deel][4]
        for i, ((u0, u1), du) in enumerate(flats):
            for k in range(n):
                soort = "oma" if (deel, i, k) == ("noord", 0, 4) else "gewoon"
                wb_flat(b, deel, u0, u1, du, k, soort)
    # balconies on the outside: a slab with a rail and a glass door, alternating per storey
    for deel, plekken in (("west", ((c - 8, 1), (c + 6, 2), (c - 8, 3))), ("oost", ((c + 6, 1), (c - 8, 2))),
                          ("noord", ((c - 7, 1), (c + 7, 2), (c - 7, 3), (c + 7, 4)))):
        x0, x1, z0, z1, n, muur, band = DELEN[deel]
        for (u, k) in plekken:
            F = G + k * V
            for du in (-2, -1, 0, 1, 2):
                for uitst in (1, 2):
                    if deel == "noord":
                        x, z = u + du, z0 - uitst
                    elif deel == "west":
                        x, z = x0 - uitst, u + du
                    else:
                        x, z = x1 + uitst, u + du
                    b.set(x, F, z, GEPOLIJST)
                    if uitst == 2 or abs(du) == 2:
                        b.bars(x, F + 1, z)
                    else:
                        b.set(x, F + 1, z, AIR)
                    b.set(x, F + 2, z, AIR)
            for du in (-2, -1, 0, 1, 2):                              # a striped awning over it
                wol = "red_wool" if du % 2 == 0 else "white_wool"
                if deel == "noord":
                    b.set(u + du, F + 4, z0 - 1, wol)
                elif deel == "west":
                    b.set(x0 - 1, F + 4, u + du, wol)
                else:
                    b.set(x1 + 1, F + 4, u + du, wol)
            for du in (-1, 0, 1):                                     # brackets under it
                if deel == "noord":
                    b.stair(u + du, F - 1, z0 - 1, "south", half="top", block="minecraft:polished_blackstone_brick_stairs")
                elif deel == "west":
                    b.stair(x0 - 1, F - 1, u + du, "east", half="top", block="minecraft:polished_blackstone_brick_stairs")
                else:
                    b.stair(x1 + 1, F - 1, u + du, "west", half="top", block="minecraft:polished_blackstone_brick_stairs")
            # a flower box of pindasaus-nylium on the rail and washing over it
            if deel == "noord":
                b.set(u + 2, F + 1, z0 - 2, NYLIUM)
                b.set(u + 2, F + 2, z0 - 2, SCHEUTJES)
                b.set(u - 1, F + 2, z0 - 2, b.rng.choice(("red_carpet", "white_carpet", "yellow_carpet")))
            elif deel == "west":
                b.set(x0 - 2, F + 1, u + 2, NYLIUM)
                b.set(x0 - 2, F + 2, u + 2, SCHEUTJES)
                b.set(x0 - 2, F + 2, u - 1, b.rng.choice(("red_carpet", "white_carpet", "yellow_carpet")))
            else:
                b.set(x1 + 2, F + 1, u + 2, NYLIUM)
                b.set(x1 + 2, F + 2, u + 2, SCHEUTJES)
                b.set(x1 + 2, F + 2, u - 1, b.rng.choice(("red_carpet", "white_carpet", "yellow_carpet")))


def wb_schoorsteen(b, x, y, z, hoog):
    """A 2 x 2 chimney with a smoking fire on it."""
    for dx in (0, 1):
        for dz in (0, 1):
            for dy in range(hoog):
                b.set(x + dx, y + dy, z + dz, GEPOLIJST if dy in (0, hoog - 1) else ROOSTER)
    b.set(x, y + hoog, z, GRILLKOOL)
    b.campfire(x, y + hoog + 1, z, signal=True)
    b.set(x + 1, y + hoog, z + 1, sb.ROOKGAT)


def wb_daken(b):
    """The roofs: chimneys, the boiler house's great stack, the sauce tank, the roof terrace of the west block and the roof
    garden of the east block (where Mika-oma's knitting landed)."""
    c, G = WB_C, WB_G
    # --- north block (5 storeys): two chimneys, a dish, a row of Mika heads on the parapet ---
    N = G + 5 * V
    wb_schoorsteen(b, c - 9, N + 1, c - 22, 4)
    wb_schoorsteen(b, c + 7, N + 1, c - 19, 3)
    b.pilaar(c, N + 1, N + 3, c - 20)
    for dx in (-1, 0, 1):
        for dy in (3, 4, 5):
            if dx or dy != 4:
                b.bars(c + dx, N + dy, c - 21)
    b.set(c, N + 4, c - 21, GLOEIKOOL)
    for x in range(c - 11, c + 12, 4):
        if b.get(x, N + 1, c - 15) is not None:
            b.mikakop(x, N + 2, c - 15, "south")
    # --- the boiler house (west corner): its great stack, glowing slits ---
    K = G + 4 * V
    sx, sz = c - 21, c - 21
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            rand = dx or dz
            for dy in range(1, 15):
                if rand:
                    b.set(sx + dx, K + dy, sz + dz, GEPOLIJST if dy % 4 == 0 else ROOSTER if (dx and dz) or dy % 4 != 2 else GLOEIKOOL)
    b.set(sx, K + 14, sz, sb.ROOKGAT)
    b.set(sx, K + 13, sz, GRILLKOOL)
    b.set(sx, K + 12, sz, ROOSTER)
    for (dx, dz) in ((-1, -1), (1, 1)):
        b.set(sx + dx, K + 15, sz + dz, GEPOLIJST)
    b.campfire(sx, K + 15, sz, signal=True)
    x0, x1, z0, z1 = DELEN["ketelhuis"][:4]
    for k in range(4):
        F = G + k * V
        for (x, z) in ((x0, c - 20), (x0, c - 22), (c - 20, z0), (c - 22, z0)):
            b.set(x, F + 3, z, GLOEIKOOL)
    b.sign(x1 - 3, G + 2, z1 + 1, "south", ["paleizen.woon.ketel1", "paleizen.woon.ketel2", "paleizen.woon.ketel3"])
    # --- the sauce tower (east corner): a round tank on legs with a band of orange glass, a pipe down the wall ---
    S = G + 3 * V
    tx, tz = c + 20, c - 20
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            d = math.hypot(dx, dz)
            if d > 3.3:
                continue
            for dy in range(3, 8):
                if d > 2.2 or dy in (3, 7):
                    b.set(tx + dx, S + dy, tz + dz, "orange_stained_glass" if dy in (5,) and d > 2.2 else GEPOLIJST if dy in (3, 7) else ROOSTER)
            if d < 1.5:
                b.set(tx + dx, S + 8, tz + dz, GEPOLIJST)
    for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        b.pilaar(tx + dx, S + 1, S + 2, tz + dz)
    x0, x1, z0, z1 = DELEN["saustoren"][:4]
    for y in range(G + 2, S + 4):
        b.chain(x1 + 1, y, tz) if y > G + 2 else b.set(x1 + 1, y, tz, GEPOLIJST)
    b.set(x1 + 1, S + 4, tz, GEPOLIJST)
    for k in range(3):
        F = G + k * V
        for (x, z) in ((x1, c - 22), (c + 22, z0), (c + 18, z0)):
            b.set(x, F + 3, z, "orange_stained_glass")
    b.sign(x0 + 3, G + 2, z1 + 1, "south", ["paleizen.woon.saus1", "paleizen.woon.saus2", "paleizen.woon.saus3"])
    # --- the west block's roof (storey 4, level with the top gallery): a terrace with chimneys, a deck chair, washing ---
    W = G + 4 * V
    wb_schoorsteen(b, c - 23, W + 1, c - 6, 3)
    wb_schoorsteen(b, c - 18, W + 1, c + 9, 2)
    for z in (c - 14, c - 13):                                        # the parapet opens where the top gallery meets the roof
        b.set(c - 15, W + 1, z, AIR)
        b.set(c - 15, W + 2, z, AIR)
    for (x, z) in ((c - 20, c + 2), (c - 20, c + 3)):
        b.set(x, W + 1, z, HOUT_TRAP, {"facing": "west", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for z in range(c - 2, c + 7):                                     # a washing line between two posts
        if z in (c - 2, c + 6):
            b.hfence(c - 17, W + 1, z)
            b.hfence(c - 17, W + 2, z)
            b.hfence(c - 17, W + 3, z)
        else:
            b.set(c - 17, W + 3, z, "chain", {"axis": "z", "waterlogged": "false"})
            if (z - c) % 2 == 0:
                b.set(c - 17, W + 2, z, b.rng.choice(("red_wool", "white_wool", "pink_wool", "yellow_wool")))
    b.lantern(c - 23, W + 1, c + 11)
    # the boiler house's roof is one with it (no parapet between them)
    for x in range(c - 24, c - 14):
        for y in (W + 1, W + 2):
            b.set(x, y, c - 15, AIR)
            b.set(x, y, c - 14, AIR)
    # --- the east block's roof (storey 3): the roof garden ---
    E = G + 3 * V
    for z in (c - 14, c - 13):
        for y in (E + 1, E + 2):
            b.set(c + 15, y, z, AIR)
    for x in range(c + 15, c + 25):
        for y in (E + 1, E + 2):
            b.set(x, y, c - 15, AIR)
            b.set(x, y, c - 14, AIR)
    for (bx0, bx1, bz0, bz1) in ((c + 22, c + 24, c - 10, c - 2), (c + 17, c + 19, c + 9, c + 12)):
        for x in range(bx0, bx1 + 1):
            for z in range(bz0, bz1 + 1):
                b.set(x, E + 1, z, NYLIUM)
                r = b.rng.random()
                if r < 0.5:
                    b.set(x, E + 2, z, SCHEUTJES)
                elif r < 0.65:
                    b.set(x, E + 2, z, ZWAMMETJE)
    # a pergola with a deck chair under it, and the knitting beside the chair
    for (x, z) in ((c + 18, c + 4), (c + 22, c + 4), (c + 18, c + 8), (c + 22, c + 8)):
        for y in range(E + 1, E + 4):
            b.hfence(x, y, z)
    for x in range(c + 18, c + 23):
        for z in range(c + 4, c + 9):
            if (x - c) % 2 == 0 or (z - c) % 2 == 0:
                b.set(x, E + 4, z, HOUT_PLAAT, {"type": "bottom", "waterlogged": "false"})
    b.set(c + 20, E + 1, c + 5, HOUT_TRAP, {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    b.set(c + 20, E + 1, c + 6, HOUT_PLAAT, {"type": "bottom", "waterlogged": "false"})
    b.set(c + 21, E + 1, c + 7, BREIWERK)
    b.vloer.append((c + 21, E + 1, c + 7))
    b.plek["breiwerk"] = (c + 21, E + 1, c + 7)
    b.sign(c + 19, E + 1, c + 7, "west", ["paleizen.woon.dak1", "paleizen.woon.dak2", "paleizen.woon.dak3"], wall=False)
    wb_schoorsteen(b, c + 17, E + 1, c - 4, 3)
    b.lantern(c + 23, E + 1, c + 11)


def wb_poort_en_plein(b):
    """The gate wall on the south side with the name over the arch and the letter boxes, and the courtyard: a barbecue,
    benches, a sand pit of ash, washing lines between the galleries."""
    c, G = WB_C, WB_G
    z = c + 14
    for x in range(c - 14, c + 15):
        for y in range(G + 1, G + 8):
            if abs(x - c) <= 2 and y <= G + 5:
                b.set(x, y, z, AIR)
            else:
                b.set(x, y, z, GEPOLIJST if y in (G + 1, G + 7) or abs(x - c) == 3 else ROOSTER)
        if (x - c) % 2 == 0:
            b.set(x, G + 8, z, GEPOLIJST)
    for x in (c - 2, c + 2):                                          # the corners of the arch
        b.stair(x, G + 5, z, "east" if x < c else "west", half="top", block="minecraft:polished_blackstone_brick_stairs")
    for s in (-1, 1):
        b.hoorns(c + s * 5, G + 8, z, s, along_x=True, lang=7)
    b.sign(c, G + 6, z + 1, "south", ["paleizen.woon.poort1", "paleizen.woon.poort2", "paleizen.woon.poort3"])
    for i, x in enumerate(range(c + 5, c + 11)):                      # the letter boxes
        b.set(x, G + 2, z - 1, "barrel", {"facing": "north", "open": "false"})
        b.set(x, G + 1, z - 1, GEPOLIJST)
    b.sign(c + 4, G + 2, z - 1, "north", ["paleizen.woon.post1", "paleizen.woon.post2", "paleizen.woon.post3"])
    b.sign(c - 5, G + 2, z - 1, "north", ["paleizen.woon.regels1", "paleizen.woon.regels2", "paleizen.woon.regels3", "paleizen.woon.regels4"])
    # the barbecue in the middle, benches round it
    for dx in (0, 1):
        b.set(c + dx, G + 1, c + 3, GRILLKOOL)
        b.bars(c + dx, G + 2, c + 3)
    b.log(c, G + 3, c + 3, WORST, "x")
    b.log(c + 1, G + 3, c + 3, WORST, "x")
    for dx in (-1, 0, 1, 2):
        b.set(c + dx, G + 1, c + 6, HOUT_TRAP, {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for dz in (2, 3, 4):
        b.set(c - 3, G + 1, c + dz, HOUT_TRAP, {"facing": "east", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    # the sand pit of ash with a bucket (a cauldron) and a little slide
    for x in range(c + 4, c + 9):
        for zz in range(c + 8, c + 12):
            rand = x in (c + 4, c + 8) or zz in (c + 8, c + 11)
            b.set(x, G + 1, zz, HOUT_PLAAT if rand else AS, {"type": "bottom", "waterlogged": "false"} if rand else None)
    b.set(c + 6, G + 2, c + 10, "cauldron")
    # washing lines across the courtyard, from rail to rail on the second storey
    for zz in (c + 2, c + 9):
        y = G + 2 * V + 1
        for x in range(c - 11, c + 12):
            b.set(x, y, zz, "chain", {"axis": "x", "waterlogged": "false"})
            if (x + zz) % 3 == 0:
                b.set(x, y - 1, zz, b.rng.choice(("red_wool", "white_wool", "pink_wool", "yellow_wool", "light_blue_wool")))
    # lamp posts and puddles
    for (x, zz) in ((c - 9, c + 10), (c + 10, c - 2), (c - 9, c - 2)):
        b.pilaar(x, G + 1, G + 3, zz)
        b.lantern(x, G + 4, zz)
    for (x, zz) in ((c - 6, c + 8), (c + 3, c - 3), (c - 2, c + 11)):
        b.set(x, G + 1, zz, PLASJE)
    # windows in the south end walls of the two side blocks
    for deel in ("west", "oost"):
        x0, x1, z0, z1, n, muur, band = DELEN[deel]
        for k in range(n):
            for x in (x0 + 2, x0 + 3, x1 - 3, x1 - 2):
                for dy in (2, 3):
                    b.pane(x, G + k * V + dy, z1, "orange" if (x + k) % 3 else "yellow")
    # the forecourt: worst trees with mustard crowns in planters, the bins, a bench
    for (x, zz) in ((c - 20, c + 19), (c - 10, c + 22), (c + 10, c + 22), (c + 20, c + 19)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                b.set(x + dx, G + 1, zz + dz, GEPOLIJST if dx or dz else AS_AARDE)
        for y in range(G + 2, G + 6):
            b.log(x, y, zz, WORST)
        for dx in (-2, -1, 0, 1, 2):
            for dz in (-2, -1, 0, 1, 2):
                for dy in (5, 6, 7):
                    if abs(dx) + abs(dz) + (dy - 5) * 2 <= 3 and (dx or dz or dy > 5):
                        b.set(x + dx, G + dy, zz + dz, MOSTERD)
    for i, x in enumerate(range(c + 15, c + 19)):
        b.set(x, G + 1, c + 15, "composter", {"level": str((i * 3) % 8)})
    b.sign(c + 19, G + 1, c + 15, "south", ["paleizen.woon.vuil1", "paleizen.woon.vuil2", "paleizen.woon.vuil3"], wall=False)
    for x in (c - 18, c - 17, c - 16):
        b.set(x, G + 1, c + 16, HOUT_TRAP, {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    # the way in from the landing outside the gate
    for x in range(c - 3, c + 4):
        for zz in range(c + 15, c + BUITEN + 3):
            b.set(x, G, zz, GEPOLIJST)


def wb_bewoners(b):
    """Mika-oma on the top gallery in front of her door, the three grumpy neighbours in their flats, three more Mika's who
    just live here."""
    c, G = WB_C, WB_G
    T = G + 4 * V
    b.npc(c - 3, T + 1, c - 14, "mika_oma", "paleizen_mika_oma", 0.0)
    b.set(c - 2, T + 1, c - 14, HOUT_TRAP, {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})   # her rocking chair
    b.set(c - 4, T + 1, c - 14, "potted_crimson_fungus")
    b.set(c - 1, T + 1, c - 14, "pink_wool")
    b.sign(c - 6, T + 2, c - 14, "south", ["paleizen.woon.oma1", "paleizen.woon.oma2", "paleizen.woon.oma3"])   # (on the wall beside her door)
    # the grumpy three: Brom-Mika (west block, ground floor), Zeur-Mika (east block, second storey), Snurk-Mika (north block, second storey)
    moppers = (((c - 20, G + 1, c + 5), -90.0, (c - 17, G + 1, c + 6)),
               ((c + 20, G + 2 * V + 1, c - 8), 90.0, (c + 17, G + 2 * V + 1, c - 8)),
               ((c + 5, G + 2 * V + 1, c - 20), 0.0, (c + 7, G + 2 * V + 1, c - 17)))
    for nr, ((x, y, z), yaw, voor) in enumerate(moppers):
        for yy in (y, y + 1):
            b.set(x, yy, z, AIR)
        b.wezen(x, y, z, "guhs:paleizen_mopper_mika", f"paleizen_mopper_{nr}", yaw, f"mopper_{nr}", Nr=nr)
        b.plek[f"mopper_{nr}_voor"] = voor
    for nr, ((x, y, z), yaw) in enumerate((((c - 14, G + V + 1, c + 2), -90.0), ((c + 2, G + 1, c + 5), 180.0), ((c - 21, T + 1, c + 3), 90.0)), start=3):
        b.wezen(x, y, z, "guhs:paleizen_mopper_mika", f"paleizen_mopper_{nr}", yaw, f"mopper_{nr}", Nr=nr)


def wb_lucht(b):
    """Room round the flats when they stand half in the rock: a dome with the plan of a rounded square over the whole estate
    (the roofs, the galleries, the roof garden with the knitting), and never less than three blocks of air over and two
    beside anything of the build."""
    c, G = WB_C, WB_G
    hoog = hoogste(b, G + 1)

    def bereik_(x, z):
        r = ((abs(x - c) / 31.5) ** 4 + (abs(z - c) / 31.5) ** 4) ** 0.25
        top = G + 6 + int(44 * math.sqrt(1.0 - r ** 6)) if r < 1.0 else None
        near = rondom(hoog, x, z)
        if near is not None:
            top = max(top or 0, near + 3)
        return None if top is None else (G + 1, top)
    lucht(b, bereik_)


def check_woonblokken(b):
    c, G = WB_C, WB_G
    problems = check(b, (c, G + 1, c + 20)) + check_steun(b, WB_ANKER)
    if b.get(*b.plek["breiwerk"]) != mc(BREIWERK):
        problems.append("mika_woonblokken: the knitting is not where PLEKKEN says")
    if sum(1 for w in b.wezens if w[3] == "guhs:paleizen_mopper_mika") < 6:
        problems.append("mika_woonblokken: fewer than six Mika neighbours")
    if len(b.chests) < 4:
        problems.append(f"mika_woonblokken: only {len(b.chests)} chests")
    return problems


def build_all(h):
    """Builds, checks and saves the three; returns {name: (bouw, problems)}."""
    out = {}
    br = brugpaleis(h)
    out["mika_brugpaleis"] = (br, check_brugpaleis(br), br.save_tiles("mika_brugpaleis"))
    wb = woonblokken(h)
    out["mika_woonblokken"] = (wb, check_woonblokken(wb), wb.save_tiles("mika_woonblokken"))
    st = stal(h)
    st.s.save("mika_stal")
    out["mika_stal"] = (st, check_stal(st), None)
    return out
