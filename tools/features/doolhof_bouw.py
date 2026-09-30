"""
Het Guhdoolhof (2.9) - the building (template guhdoolhof, 96 x 36 x 96), made of guh blocks from every side:

  - the hedge field (58 x 58 at (19, 14)): the border hedge with guh-ear topiary and trimmed guh faces looking out, the
    exit gate in the north with a hedge arch, and a first lastig maze inside (Java grows a new one for every game:
    feature/doolhof/DoolhofVeld uses the same numbers: G, P, FX, FZ, the tower cells, the bridge posts, the lanterns)
  - the lookout guh in the middle: a big sitting guh (pink body, deck on its shoulders, a round head with a face and
    ears), reached by a bridge on posts from the plaza (railings: nobody drops into the maze)
  - the plaza in the south with Meneer Vadskronkel's kiosk (the invisible anchor under him), benches, flowers, a hedge
    guh on each side of the path and a welcome arch; garden paths around the field to the finish garden in the north
  - little guh-ear lanterns everywhere (they light up at night)
The self-check (check) raises SystemExit: nothing floats, the NPC, the deck, the finish and the maze start can be
walked to, the numbers match DoolhofVeld.
"""
import math
import random

from features import sterrenwacht_hulp as hulp

NAME = "guhdoolhof"
W, H, D = 96, 36, 96
G = 4
N, P = 19, 3
F = N * P + 1
FX, FZ = 19, 14
TOREN_VAN, TOREN_TOT = 8, 10
TOREN_A, TOREN_B = TOREN_VAN * P, (TOREN_TOT + 1) * P
MIDDEN = N // 2
BRUG_A1, BRUG_A2 = 27, 30
AX, AY, AZ = 36, G - 1, 84          # the anchor under Meneer Vadskronkel (DoolhofVeld.AX/AY/AZ)
NPC = (36.5, G + 1, 84.5)
ANCHOR = "guhs:guhdoolhof_midden"
JIGSAW = (47, G, 90)

HEG = "guhs:doolhofheg"
HEG_GEZICHT = "guhs:doolhofheg_gezicht"
LAMP = "guhs:doolhof_lantaarn"
KLINK = "guhs:knuffelklinkers"
GRAS = "minecraft:grass_block"
HOUT = "guhs:vadshout_planken"
STAM = "guhs:vadshout_stam"
HEK = "guhs:vadshout_hek"
MUUR = "guhs:knuffelsteen_muur"
STEEN = "guhs:knuffelsteen"
BLOEMEN = ["guhs:roze_guhbloem", "guhs:guhoortjes", "guhs:kaasbloem", "minecraft:pink_tulip", "minecraft:allium", "minecraft:oxeye_daisy"]


# ======================================================================================================================
# the maze plan (the same rules as DoolhofKaart / DoolhofVeld.dicht, for the first maze in the template)
# ======================================================================================================================
def toren_cel(x, z):
    return TOREN_VAN <= x <= TOREN_TOT and TOREN_VAN <= z <= TOREN_TOT


class Kaart:
    """A lastig maze (all 19 x 19 cells but the tower), start (9, 18), exit (9, 0)."""

    def __init__(self, seed):
        rng = random.Random(seed)
        self.oost = [[False] * N for _ in range(N)]
        self.zuid = [[False] * N for _ in range(N)]
        self.actief = [[not toren_cel(x, z) for z in range(N)] for x in range(N)]
        self.start, self.uit = (MIDDEN, N - 1), (MIDDEN, 0)
        gezien = {self.start}
        stapel = [self.start]
        while stapel:
            x, z = stapel[-1]
            opties = [(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))
                      if 0 <= x + dx < N and 0 <= z + dz < N and self.actief[x + dx][z + dz] and (x + dx, z + dz) not in gezien]
            if not opties:
                stapel.pop()
                continue
            nx, nz = rng.choice(opties)
            self.open(x, z, nx, nz)
            gezien.add((nx, nz))
            stapel.append((nx, nz))
        dicht = [(x, z, x + 1, z) for x in range(N - 1) for z in range(N) if self.actief[x][z] and self.actief[x + 1][z] and not self.oost[x][z]]
        dicht += [(x, z, x, z + 1) for x in range(N) for z in range(N - 1) if self.actief[x][z] and self.actief[x][z + 1] and not self.zuid[x][z]]
        rng.shuffle(dicht)
        for w in dicht[:round(len(dicht) * 0.035)]:
            self.open(*w)

    def open(self, x1, z1, x2, z2):
        if x2 == x1 + 1:
            self.oost[x1][z1] = True
        elif x1 == x2 + 1:
            self.oost[x2][z2] = True
        elif z2 == z1 + 1:
            self.zuid[x1][z1] = True
        else:
            self.zuid[x2][z2] = True

    def doorgang(self, x, z, dx, dz):
        nx, nz = x + dx, z + dz
        if not (0 <= nx < N and 0 <= nz < N) or not self.actief[x][z] or not self.actief[nx][nz]:
            return False
        if dx == 1:
            return self.oost[x][z]
        if dx == -1:
            return self.oost[nx][nz]
        if dz == 1:
            return self.zuid[x][z]
        return self.zuid[nx][nz]

    def dicht(self, bx, b):
        wa, wb = bx % P == 0, b % P == 0
        if wa and wb:
            return True
        cx, cz = bx // P, b // P
        if not wa and not wb:
            return not self.actief[cx][cz]
        if wa:
            return not self.doorgang(cx - 1, cz, 1, 0)
        return not self.doorgang(cx, cz - 1, 0, 1)

    def doodlopend(self):
        out = []
        for x in range(N):
            for z in range(N):
                if not self.actief[x][z] or (x, z) in (self.start, self.uit):
                    continue
                opens = [(dx, dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) if self.doorgang(x, z, dx, dz)]
                if len(opens) == 1:
                    out.append((x, z, opens[0]))
        return out


def toren(bx, b):
    return TOREN_A <= bx <= TOREN_B and TOREN_A <= b <= TOREN_B


def brugpaal(bx, b):
    return bx in (BRUG_A1, BRUG_A2) and b > TOREN_B


def lampje(bx, b):
    return ((bx // P) * 7 + (b // P) * 3) % 5 == 0


# ======================================================================================================================
# the building
# ======================================================================================================================
class Bouw:
    def __init__(self, h):
        self.h = h
        self.s = h.Structure((W, H, D))
        self.rng = random.Random(29240)
        self.gezichten = 0
        self.kaart = Kaart(29241)

    def set(self, x, y, z, b, props=None):
        self.s.set(x, y, z, b, props)

    # --- ground ---------------------------------------------------------------------------------------------------
    def grond(self):
        s = self.s
        for x in range(W):
            for z in range(D):
                for y in range(G - 1):
                    s.set(x, y, z, "minecraft:dirt")
                s.set(x, G - 1, z, "minecraft:dirt")
                s.set(x, G, z, GRAS)

    def pad(self, x0, z0, x1, z1, patroon=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                self.set(x, G, z, STEEN if patroon and (x + z) % 4 == 0 else KLINK)

    # --- the hedge field --------------------------------------------------------------------------------------------
    def veld(self):
        k = self.kaart
        for bx in range(F):
            for b in range(F):
                x, z = FX + bx, FZ + b
                if toren(bx, b):
                    continue
                rand = bx in (0, F - 1) or b in (0, F - 1)
                if rand:
                    poort = b == 0 and bx in (P * MIDDEN + 1, P * MIDDEN + 2)
                    for y in range(G + 1, G + 5):
                        self.set(x, y, z, "minecraft:air" if poort else HEG)
                    continue
                dicht = k.dicht(bx, b)
                for y in range(G + 1, G + 5):
                    self.set(x, y, z, HEG if dicht else "minecraft:air")
                if bx % P == 0 and b % P == 0 and not brugpaal(bx, b) and lampje(bx, b):
                    self.set(x, G + 5, z, LAMP, {"lit": "false"})
        # the dead ends: a trimmed guh face in the back wall
        for (cx, cz, (dx, dz)) in k.doodlopend():
            achter = (-dx, -dz)
            bx = {(-1, 0): P * cx, (1, 0): P * cx + P}.get(achter, P * cx + 1)
            b = {(0, -1): P * cz, (0, 1): P * cz + P}.get(achter, P * cz + 1)
            if 0 < bx < F - 1 and 0 < b < F - 1 and not toren(bx, b):
                facing = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(dx, dz)]
                self.set(FX + bx, G + 2, FZ + b, HEG_GEZICHT, {"facing": facing})
                self.gezichten += 1
        # the border: guh-ear topiary on top, trimmed faces looking out
        for i in range(0, F, 6):
            for (x, z) in ((FX + i, FZ), (FX + i, FZ + F - 1), (FX, FZ + i), (FX + F - 1, FZ + i)):
                if abs(x - (FX + P * MIDDEN + 1.5)) < 4 and z == FZ:
                    continue                                          # (not over the exit gate)
                if abs(x - (FX + 28.5)) < 4 and z == FZ + F - 1:
                    continue                                          # (not under the bridge)
                self.oortjes(x, G + 5, z, langs_x=z in (FZ, FZ + F - 1))
        for i in range(4, F - 4, 8):
            for (x, z, f) in ((FX + i, FZ - 0, "north"), (FX + i, FZ + F - 1, "south"), (FX, FZ + i, "west"), (FX + F - 1, FZ + i, "east")):
                if f == "north" and abs(x - (FX + P * MIDDEN + 1.5)) < 5:
                    continue
                if f == "south" and abs(x - (FX + 28.5)) < 4:
                    continue
                self.set(x, G + 2, z, HEG_GEZICHT, {"facing": f})
                self.gezichten += 1
        self.poortboog()

    def oortjes(self, x, y, z, langs_x):
        """Two little guh ears trimmed out of the top of the hedge."""
        for d in (0, 2):
            xx, zz = (x + d, z) if langs_x else (x, z + d)
            if FX <= xx < FX + F and FZ <= zz < FZ + F:
                self.set(xx, y, zz, HEG)
                self.set(xx, y + 1, zz, HEG)

    def poortboog(self):
        """The exit gate's hedge arch with a guh face and flags."""
        x0 = FX + P * MIDDEN
        for x in range(x0 - 1, x0 + 5):
            self.set(x, G + 5, FZ, HEG)
        for x in range(x0, x0 + 4):
            self.set(x, G + 6, FZ, HEG)
        self.set(x0 - 1, G + 6, FZ, HEG)
        self.set(x0 - 1, G + 7, FZ, HEG)
        self.set(x0 + 4, G + 6, FZ, HEG)
        self.set(x0 + 4, G + 7, FZ, HEG)
        self.set(x0 + 1, G + 6, FZ, HEG_GEZICHT, {"facing": "north"})
        self.set(x0 + 2, G + 6, FZ, HEG_GEZICHT, {"facing": "north"})
        self.gezichten += 2

    # --- the lookout guh ----------------------------------------------------------------------------------------------
    def toren(self):
        x0, z0 = FX + TOREN_A, FZ + TOREN_A          # 43, 38
        x1, z1 = FX + TOREN_B, FZ + TOREN_B          # 52, 47
        cx, cz = (x0 + x1 + 1) / 2, (z0 + z1 + 1) / 2
        # the body: a big pink guh body, a light belly on the front (south)
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                hoek = (x in (x0, x1)) and (z in (z0, z1))
                for y in range(G + 1, G + 7):
                    if hoek and y >= G + 5:
                        continue
                    rand = x in (x0, x1) or z in (z0, z1)
                    if not rand:
                        self.set(x, y, z, "minecraft:pink_wool")      # (filled: a sturdy guh)
                        continue
                    buik = z == z1 and abs(x + 0.5 - cx) < 3.2 and G + 2 <= y <= G + 5
                    self.set(x, y, z, "minecraft:white_wool" if buik else "minecraft:pink_wool")
                if hoek:
                    self.set(x, G + 5, z, "guhs:pluisdak")
                    self.set(x, G + 6, z, "guhs:pluisdak")
                self.set(x, G + 7, z, HOUT)                       # the deck (its shoulders)
        # little arms folded on the belly
        for x in (x0 + 2, x1 - 2):
            self.set(x, G + 4, z1, "minecraft:pink_concrete")
        # the railing round the deck (open to the bridge in the south)
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if (x in (x0, x1) or z in (z0, z1)) and not (z == z1 and x in (47, 48)):
                    self.set(x, G + 8, z, HEK)
        for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            self.set(x, G + 8, z, MUUR, {"up": "true"})
            self.set(x, G + 9, z, LAMP, {"lit": "false"})
        # the neck
        for x in range(46, 50):
            for z in range(41, 45):
                for y in range(G + 8, G + 11):
                    self.set(x, y, z, "minecraft:pink_wool")
        # the head: a round guh head with a face looking south over the bridge, and two big ears
        hx, hy, hz, r = 47.5, G + 14.5, 42.5, 5.3
        palet = dict(hulp.FACE_WOOL)
        for x in range(int(hx - r) - 1, int(hx + r) + 2):
            for y in range(int(hy - r) - 1, int(hy + r) + 2):
                for z in range(int(hz - r) - 1, int(hz + r) + 2):
                    d = math.dist((x + 0.5, y + 0.5, z + 0.5), (hx, hy, hz))
                    if d > r:
                        continue
                    blok = "minecraft:pink_wool"
                    if d > r - 1.2 and z + 0.5 > hz + 2.2:
                        role = hulp.face_role(hx - (x + 0.5), (y + 0.5) - hy, 4.3)
                        if role and role not in ("skin", "ear", "ear_in"):
                            blok = palet[role]
                    self.set(x, y, z, blok)
        top = int(hy + r)
        for sx in (-1, 1):
            ex = int(hx + sx * 3.2 - 0.5)
            for i, breed in enumerate((3, 3, 2, 1)):
                for dx in range(breed):
                    xx = ex + dx - breed // 2
                    binnen = i < 2 and dx == breed // 2
                    self.set(xx, top - 1 + i, 43, "minecraft:magenta_wool" if binnen else "minecraft:pink_wool")
                    self.set(xx, top - 1 + i, 42, "minecraft:pink_wool")
        self.gezichten += 1

    # --- the bridge -----------------------------------------------------------------------------------------------------
    def brug(self):
        z_eind = 73
        for z in range(FZ + TOREN_B + 1, z_eind + 1):
            for x in range(46, 50):
                self.set(x, G + 7, z, HOUT)
            self.set(46, G + 8, z, HEK)
            self.set(49, G + 8, z, HEK)
        # the posts: on the maze's posts (hedge below, wood above), on the border and on the plaza
        for b in range(TOREN_B + 3, F, 6):
            for bx in (BRUG_A1, BRUG_A2):
                x, z = FX + bx, FZ + b
                for y in range(G + 5, G + 7):
                    self.set(x, y, z, STAM, {"axis": "y"})
        for bx in (BRUG_A1, BRUG_A2):
            x = FX + bx
            for y in range(G + 5, G + 7):
                self.set(x, y, FZ + F - 1, STAM, {"axis": "y"})
            for y in range(G + 1, G + 7):
                self.set(x, y, z_eind, STAM, {"axis": "y"})
        for z in range(52, z_eind, 8):
            self.set(46, G + 9, z, LAMP, {"lit": "false"})
            self.set(49, G + 9, z + 4, LAMP, {"lit": "false"})
        # the stairs down to the plaza
        for i in range(7):
            z, y = z_eind + 1 + i, G + 7 - i
            for x in range(46, 50):
                self.set(x, y, z, "guhs:vadshout_trap", {"facing": "north", "half": "bottom", "shape": "straight"})
                for yy in range(G + 1, y):
                    self.set(x, yy, z, HOUT)
        for z in range(z_eind + 1, z_eind + 8):
            for x in (45, 50):
                self.set(x, G + 1, z, "guhs:seizoensbloembak", {"seizoen": "lente"})

    # --- the plaza with Meneer Vadskronkel's kiosk ------------------------------------------------------------------------
    def plein(self):
        self.pad(44, 81, 51, 95, patroon=True)
        self.pad(24, 82, 71, 93, patroon=True)
        # the kiosk: knuffelsteen posts, a pink pluisdak roof with a hedge face, the anchor under him
        cx, cz = AX, AZ
        for (x, z) in ((cx - 3, cz - 3), (cx + 3, cz - 3), (cx - 3, cz + 3), (cx + 3, cz + 3)):
            for y in range(G + 1, G + 5):
                self.set(x, y, z, MUUR, {"up": "true"})
        for x in range(cx - 4, cx + 5):
            for z in range(cz - 4, cz + 5):
                e = max(abs(x - cx), abs(z - cz))
                self.set(x, G + 5 + (4 - e) // 2, z, "guhs:pluisdak")
                for y in range(G + 5, G + 5 + (4 - e) // 2):
                    self.set(x, y, z, "guhs:pluisdak")
        self.set(cx, G + 8, cz, HEG)
        self.set(cx, G + 9, cz, HEG_GEZICHT, {"facing": "south"})
        self.set(cx - 1, G + 9, cz, HEG)
        self.set(cx + 1, G + 9, cz, HEG)
        self.set(cx - 1, G + 10, cz, HEG)
        self.set(cx + 1, G + 10, cz, HEG)
        self.gezichten += 1
        for x in range(cx - 2, cx + 3):
            self.set(x, G, cz + 2, "guhs:knuffelsteen")
            if x != cx:
                self.set(x, G + 1, cz - 2, "guhs:seizoensbloembak", {"seizoen": "zomer"})
        self.set(cx, G - 1, cz, "guhs:doolhof_anker", {"facing": "north"})
        ms = self.h.ms
        self.s.entity(NPC[0], float(NPC[1]), NPC[2], {"id": "guhs:guh_npc", "Kind": "doolhofguh", "PersistenceRequired": ms.Byte(1),
                                                     "Rotation": ms.floats(0.0, 0.0)})
        # benches, flowers, lanterns on posts
        for x in (28, 58, 66):
            self.set(x, G + 1, 92, "guhs:guh_bank", {"facing": "north"})
        for x in (40, 56):
            self.set(x, G + 1, 82, "guhs:guh_bank", {"facing": "south"})
        for (x, z) in ((25, 83), (70, 83), (25, 92), (70, 92), (43, 94), (52, 94)):
            self.set(x, G + 1, z, MUUR, {"up": "true"})
            self.set(x, G + 2, z, MUUR, {"up": "true"})
            self.set(x, G + 3, z, LAMP, {"lit": "false"})
        # two hedge guhs by the path, and the welcome arch
        self.heg_guh(41, 89, "south")
        self.heg_guh(54, 89, "south")
        for y in range(G + 1, G + 6):
            self.set(42, y, 95, HEG)
            self.set(53, y, 95, HEG)
        for x in range(42, 54):
            self.set(x, G + 6, 95, HEG)
        for x in range(43, 53):
            self.set(x, G + 7, 95, HEG)
        self.set(47, G + 7, 95, HEG_GEZICHT, {"facing": "south"})
        self.set(48, G + 7, 95, HEG_GEZICHT, {"facing": "south"})
        for x in (43, 52):
            self.set(x, G + 8, 95, HEG)
            self.set(x, G + 9, 95, HEG)
        self.gezichten += 2

    def heg_guh(self, x, z, facing):
        """A hedge trimmed into a sitting guh: a round body, a head with a face, two ears."""
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                self.set(x + dx, G + 1, z + dz, HEG)
                if abs(dx) + abs(dz) < 2:
                    self.set(x + dx, G + 2, z + dz, HEG)
        self.set(x, G + 3, z, HEG_GEZICHT, {"facing": facing})
        self.set(x - 1, G + 3, z, HEG)
        self.set(x + 1, G + 3, z, HEG)
        self.set(x - 1, G + 4, z, HEG)
        self.set(x + 1, G + 4, z, HEG)
        self.gezichten += 1

    # --- the gardens round the field, the finish garden ----------------------------------------------------------------
    def tuinen(self):
        rng = self.rng
        self.pad(7, 3, 10, 90)
        self.pad(85, 3, 88, 90)
        self.pad(7, 3, 88, 6)
        self.pad(7, 88, 24, 90)
        self.pad(71, 88, 88, 90)
        # the finish: from the exit gate to the north path, a little square with flags and benches
        self.pad(46, 7, 49, FZ - 1)
        self.pad(41, 7, 54, 11, patroon=True)
        for x in (41, 54):
            self.set(x, G + 1, 9, "guhs:guh_bank", {"facing": "east" if x == 41 else "west"})
        for (x, z) in ((42, 12), (53, 12)):
            for y in range(G + 1, G + 4):
                self.set(x, y, z, MUUR, {"up": "true"})
            self.set(x, G + 4, z, LAMP, {"lit": "false"})
        # trees, flowers, lanterns, ear bushes in the gardens
        for (x, z) in ((3, 12), (14, 20), (3, 36), (14, 48), (3, 62), (14, 76), (92, 14), (80, 24), (92, 40), (80, 52), (92, 66), (80, 78),
                       (22, 1), (70, 1)):
            self.boom(x, z)
        for x in list(range(0, 18)) + list(range(78, 96)):
            for z in range(8, 88):
                if self.s.get(x, G, z) == GRAS and self.s.get(x, G + 1, z) is None and rng.random() < 0.09:
                    self.set(x, G + 1, z, rng.choice(BLOEMEN))
        for z in range(10, 88, 9):
            for x in (6, 11, 84, 89):
                if self.s.get(x, G + 1, z) is None or self.s.get(x, G + 1, z) in BLOEMEN:
                    self.set(x, G + 1, z, MUUR, {"up": "true"})
                    self.set(x, G + 2, z, LAMP, {"lit": "false"})
        for z in range(14, 86, 12):
            for x in (13, 82):
                self.oor_bosje(x, z)
        for z in (30, 58):
            self.set(11, G + 1, z, "guhs:guh_bank", {"facing": "east"})
            self.set(84, G + 1, z, "guhs:guh_bank", {"facing": "west"})

    def boom(self, x, z):
        """A pluizenboom: a trunk and a fluffy crown."""
        for y in range(G + 1, G + 5):
            self.set(x, y, z, "guhs:pluizenboom_stam", {"axis": "y"})
        for dx in range(-2, 3):
            for dy in range(-1, 3):
                for dz in range(-2, 3):
                    if dx * dx + dy * dy * 1.5 + dz * dz <= 6.5 and 0 <= x + dx < W and 0 <= z + dz < D:
                        if self.s.get(x + dx, G + 5 + dy, z + dz) is None:
                            self.set(x + dx, G + 5 + dy, z + dz, "guhs:pluizenboom_bladeren")

    def oor_bosje(self, x, z):
        """A round hedge bush with two little guh ears."""
        self.set(x, G + 1, z, HEG)
        self.set(x, G + 2, z, HEG_GEZICHT, {"facing": "east" if x < 48 else "west"})
        self.set(x, G + 3, z - 1, HEG)
        self.set(x, G + 3, z + 1, HEG)
        self.set(x, G + 2, z - 1, HEG)
        self.set(x, G + 2, z + 1, HEG)
        self.gezichten += 1

    # --- finishing touches ------------------------------------------------------------------------------------------
    def hekken(self):
        """Fences and walls connect to their neighbours (a template keeps the states as saved)."""
        for (x, y, z), (b, props, nbt) in list(self.s.blocks.items()):
            if b == HEK:
                p = {}
                for d, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0))):
                    n = self.s.get(x + dx, y, z + dz)
                    p[d] = "true" if n in (HEK, MUUR) else "false"
                p["waterlogged"] = "false"
                self.s.blocks[(x, y, z)] = (b, p, nbt)

    def anker_jigsaw(self):
        x, y, z = JIGSAW
        self.set(x, y, z, "minecraft:jigsaw", {"orientation": "up_north"})
        self.s.blocks[(x, y, z)] = ("minecraft:jigsaw", {"orientation": "up_north"},
                                    {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty",
                                     "final_state": KLINK, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})

    def bouw(self):
        self.grond()
        self.tuinen()
        self.plein()
        self.veld()
        self.toren()
        self.brug()
        self.hekken()
        self.anker_jigsaw()
        self.s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
        return self


# ======================================================================================================================
# the self-check
# ======================================================================================================================
def check(b):
    s = b.s
    problems = []
    for p in hulp.check_floating(s, G)[:20]:
        problems.append(f"floating {s.get(*p)} at {p}")
    extra = ("minecraft:jigsaw", "guhs:doolhof_anker", LAMP, "guhs:seizoensbloembak", "guhs:vlaggetjes")
    starts = [(47, G + 1, 95 - 1), (8, G + 1, 50), (86, G + 1, 50)]
    reach = hulp.walk(s, starts, extra_passable=("minecraft:jigsaw",))
    if len(reach) < 1500:
        problems.append(f"only {len(reach)} walkable spots")
    x, y, z = int(NPC[0]), NPC[1], int(NPC[2])
    if s.get(x, y - 1, z) in (None, "minecraft:air") or s.get(x, y, z) not in (None, "minecraft:air"):
        problems.append("Meneer Vadskronkel doesn't stand on a floor")
    if not hulp.near_reachable(reach, x, y, z, r=2):
        problems.append("Meneer Vadskronkel can't be walked to")
    if s.get(AX, AY, AZ) != "guhs:doolhof_anker" or AY != G - 1:
        problems.append("the anchor isn't under Meneer Vadskronkel")
    # the deck of the lookout guh (over the bridge) and the finish garden
    if not hulp.near_reachable(reach, 47, G + 8, 45, r=1):
        problems.append("the lookout deck can't be reached over the bridge")
    if not hulp.near_reachable(reach, 47, G + 1, 10, r=1):
        problems.append("the finish garden can't be reached")
    # the maze: from the start everything, and out through the exit gate
    k = b.kaart
    mx, mz = FX + P * k.start[0] + 1, FZ + P * k.start[1] + 1
    maze = hulp.walk(s, [(mx, G + 1, mz)], extra_passable=extra)
    if (FX + P * MIDDEN + 1, G + 1, FZ - 1) not in maze:
        problems.append("the first maze has no way out")
    for cx in range(N):
        for cz in range(N):
            if k.actief[cx][cz] and (FX + P * cx + 1, G + 1, FZ + P * cz + 1) not in maze:
                problems.append(f"maze cell {cx},{cz} can't be reached")
                break
    # with the exit gate shut, the maze is closed: nobody walks from it onto the plaza or the bridge
    poort = [(FX + P * MIDDEN + d, y, FZ) for d in (1, 2) for y in range(G + 1, G + 5)]
    oud = {p: s.blocks.get(p) for p in poort}
    for p in poort:
        s.set(*p, HEG)
    dicht = hulp.walk(s, [(mx, G + 1, mz)], extra_passable=extra)
    for p, v in oud.items():
        if v is None:
            s.blocks.pop(p, None)
        else:
            s.blocks[p] = v
    if any(not (FX < xx < FX + F - 1 and FZ < zz < FZ + F - 1) or yy > G + 2 for (xx, yy, zz) in dicht):
        problems.append("the maze leaks out (not through its exit gate)")
    # the tower cells and the border are what DoolhofVeld expects
    if s.get(FX + TOREN_A, G + 2, FZ + TOREN_A + 3) in (None, "minecraft:air") or s.get(FX, G + 2, FZ + 3) not in (HEG, HEG_GEZICHT):
        problems.append("tower or border not where DoolhofVeld expects them")
    return problems, len(reach)
