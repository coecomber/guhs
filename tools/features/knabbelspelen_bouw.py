"""
De Knabbelspelen (2.9) - the building (template knabbelspelen, 96 x 32 x 96), full of guh things from every side:

  - the big striped circus tent in the middle (pink and white, four entrances), its roof rising to two tent tops shaped
    like guh ears, a guh face on the roof above the south entrance, flags on the ears; inside a circus ring with Juf
    Vahoegsakee in the middle (the invisible anchor under her) and tiers of seats all round
  - six play fields (feature/knabbelspelen/Speelvelden uses the same numbers), each with four fenced lanes of 5 and an
    entrance arch towards the tent (its scoreboard floats over it):
      north: Knabbelhappen (a striped gallows beam with the strings), Zaklopen (fluffy humps, start and finish lines),
             Mika-blikgooien (tables with 3-2-1 pyramids of Mika tins, a catch net with a big Mika face)
      south: Eierlopen (a slalom of flags, big eggs), Spijkerpoepen (three kaasmelk bottles in the ground per lane),
             Guhguhtje prik (a big guh seen from behind on a board per lane: pin its tail!)
  - klinker paths, benches for the fans, flowers, lampions
The self-check (check) raises SystemExit: nothing floats, Juf Vahoegsakee and every lane start can be walked to, the
Java spots hold what the game needs (tins, bottles, boards, the beam).
"""
import math
import random

from features import sterrenwacht_hulp as hulp

NAME = "knabbelspelen"
W, H, D = 96, 32, 96
G = 4
AX, AY, AZ = 47, G - 1, 48
NPC = (47.5, G + 1, 48.5)
ANCHOR = "guhs:knabbelspelen_midden"
JIGSAW = (47, G, 90)
CX, CZ, R = 47.5, 48.5, 14
BANEN, VELD_B, VELD_D = 4, 28, 26
VELDEN = {  # event: (x0, z0, north?)  (Speelvelden.veld)
    "knabbelhappen": (3, 3, True), "zaklopen": (34, 3, True), "blikgooien": (65, 3, True),
    "eierlopen": (3, 67, False), "spijkerpoepen": (34, 67, False), "guhguhtje_prik": (65, 67, False),
}
HAP_MAT, HAP_BALK, HAP_Y = 1, 4, G + 7
ZAK_HOBBELS, ZAK_FINISH = (5, 10, 15), 20
BLIK_MAT, BLIK_TAFEL = 1, 9
EI_VLAGGEN, EI_KANT, EI_FINISH = (4, 8, 12, 16), (1, -1, 1, -1), 20
FLES_U = (5, 10, 15)
PRIK_MAT, PRIK_BORD = 1, 10
BLIKKEN = ((-1, 0, 0), (0, 0, 0), (1, 0, 0), (-1, 1, 1), (0, 1, 1), (0, 2, 0))

KLINK = "guhs:knuffelklinkers"
GRAS = "minecraft:grass_block"
HEK = "guhs:vadshout_hek"
HOUT = "guhs:vadshout_planken"
MUUR = "guhs:knuffelsteen_muur"
BLOEMEN = ["guhs:roze_guhbloem", "guhs:guhoortjes", "guhs:kaasbloem", "minecraft:pink_tulip", "minecraft:cornflower", "minecraft:oxeye_daisy"]


def baan_x(x0, k):
    return x0 + 5 + 6 * k


def start_z(z0, noord):
    return z0 + VELD_D - 2 if noord else z0 + 2


def lz(z0, noord, u):
    """The template z of a lane's u."""
    return start_z(z0, noord) + (-u if noord else u)


class Bouw:
    def __init__(self, h):
        self.h = h
        self.s = h.Structure((W, H, D))
        self.rng = random.Random(29401)
        self.gezichten = 0

    def set(self, x, y, z, b, props=None):
        self.s.set(x, y, z, b, props)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    # --- ground and paths ---------------------------------------------------------------------------------------------
    def grond(self):
        for x in range(W):
            for z in range(D):
                for y in range(G):
                    self.set(x, y, z, "minecraft:dirt")
                self.set(x, G, z, GRAS)

    def pad(self, x0, z0, x1, z1):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                self.set(x, G, z, "guhs:knuffelsteen" if (x * 3 + z) % 7 == 0 else KLINK)

    def paden(self):
        for x in range(W):
            for z in range(D):
                d = math.dist((x + 0.5, z + 0.5), (CX, CZ))
                if R + 0.5 <= d <= R + 3.2:
                    self.set(x, G, z, KLINK if (x + z) % 5 else "guhs:knuffelsteen")
        self.pad(2, 29, 93, 33)
        self.pad(2, 63, 93, 66)
        self.pad(45, 33, 50, 36)
        self.pad(45, 61, 50, 64)
        self.pad(0, 46, 34, 51)
        self.pad(61, 46, 95, 51)
        self.pad(45, 91, 50, 95)
        self.pad(45, 0, 50, 2)

    # --- the circus tent ------------------------------------------------------------------------------------------------
    def dak_hoogte(self, x, z):
        d = math.dist((x + 0.5, z + 0.5), (CX, CZ))
        y = G + 7 + (R + 1 - d) * 0.75
        for sx in (-1, 1):
            de = math.dist((x + 0.5, z + 0.5), (CX + sx * 6.5, CZ))
            if de <= 4.5:
                y = max(y, G + 17 + (4.5 - de) * 1.6)
        return y

    def streep(self, x, z, n=16):
        a = math.degrees(math.atan2(z + 0.5 - CZ, x + 0.5 - CX)) % 360
        return int(a / (360 / n)) % 2 == 0

    def tent(self):
        rng = self.rng
        palet = dict(hulp.FACE_WOOL)
        for x in range(int(CX - R - 2), int(CX + R + 3)):
            for z in range(int(CZ - R - 2), int(CZ + R + 3)):
                d = math.dist((x + 0.5, z + 0.5), (CX, CZ))
                if d > R + 1.2:
                    continue
                # the wall: pink and white stripes, four entrances
                if R - 0.5 <= d <= R + 0.6:
                    ingang = min(abs(x + 0.5 - CX), abs(z + 0.5 - CZ)) <= 1.6
                    for y in range(G + 1, G + 7):
                        if ingang and y <= G + 4:
                            continue
                        self.set(x, y, z, "minecraft:pink_wool" if self.streep(x, z) else "minecraft:white_wool")
                # the floor inside: a pink ring (manege) round the middle, klinkers
                if d < R - 0.5:
                    self.set(x, G, z, "minecraft:pink_terracotta" if d < 6.2 else KLINK)
                # the roof, two blocks thick, rising to the two guh-ear tops
                top = self.dak_hoogte(x, z)
                ty = int(round(top))
                buren = [int(round(self.dak_hoogte(x + dx, z + dz))) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))]
                onder = min(ty - 1, min(buren) + 1)
                for y in range(onder, ty + 1):
                    if y < G + 7:
                        continue
                    blok = "minecraft:pink_wool" if self.streep(x, z) else "minecraft:white_wool"
                    for sx in (-1, 1):
                        de = math.dist((x + 0.5, z + 0.5), (CX + sx * 6.5, CZ))
                        if de <= 4.5 and y >= G + 16:
                            blok = "minecraft:magenta_wool" if (z + 0.5 > CZ + 1.5 and abs(x + 0.5 - (CX + sx * 6.5)) < 1.8) else "minecraft:pink_wool"
                    # the guh face on the south slope, over the south entrance
                    if z + 0.5 > CZ + 5 and abs(x + 0.5 - CX) < 7.5:
                        role = hulp.face_role(CX - (x + 0.5), (y + 0.5) - (G + 11.5), 4.6)
                        if role and role not in ("skin", "ear", "ear_in"):
                            blok = palet[role]
                    self.set(x, y, z, blok)
                # (walls under the roof edge: close the gap between the wall top and the roof)
                if R - 0.5 <= d <= R + 0.6:
                    for y in range(G + 7, ty - 1):
                        self.set(x, y, z, "minecraft:pink_wool" if self.streep(x, z) else "minecraft:white_wool")
        self.gezichten += 1
        # flags on the ear tops
        for sx in (-1, 1):
            x, z = int(CX + sx * 6.5), int(CZ)
            top = int(round(self.dak_hoogte(x, z)))
            for y in range(top + 1, top + 4):
                self.set(x, y, z, HEK)
            self.set(x + (1 if sx > 0 else -1), top + 3, z, "minecraft:red_wool" if sx > 0 else "minecraft:yellow_wool")
            self.set(x + (2 if sx > 0 else -2), top + 3, z, "minecraft:red_wool" if sx > 0 else "minecraft:yellow_wool")
        # the ring's curb (half slabs you step over), the seats all round, lampions
        for x in range(int(CX - R), int(CX + R) + 1):
            for z in range(int(CZ - R), int(CZ + R) + 1):
                d = math.dist((x + 0.5, z + 0.5), (CX, CZ))
                gang = min(abs(x + 0.5 - CX), abs(z + 0.5 - CZ)) <= 1.6
                if 6.0 <= d < 7.0 and not gang:
                    self.set(x, G + 1, z, "guhs:knuffelsteen_plaat", {"type": "bottom"})
                if 9.5 <= d < 13.4 and not gang:
                    rij = int(d - 9.5)
                    dx, dz = x + 0.5 - CX, z + 0.5 - CZ
                    facing = ("east" if dx > 0 else "west") if abs(dx) > abs(dz) else ("south" if dz > 0 else "north")
                    for y in range(G + 1, G + 1 + rij):
                        self.set(x, y, z, HOUT)
                    self.set(x, G + 1 + rij, z, "guhs:vadshout_trap", {"facing": facing, "half": "bottom", "shape": "straight"})
        for a in range(0, 360, 45):
            x = int(CX + 7.6 * math.cos(math.radians(a + 22.5)))
            z = int(CZ + 7.6 * math.sin(math.radians(a + 22.5)))
            self.set(x, G + 1, z, MUUR, {"up": "true"})
            self.set(x, G + 2, z, MUUR, {"up": "true"})
            self.set(x, G + 3, z, "guhs:lampion_" + ("roze" if a % 90 else "geel"), {"hanging": "false"})
        # Juf Vahoegsakee in the middle of the ring (the anchor under her), balloons of wool round her little stage
        self.set(AX, AY, AZ, "guhs:knabbelspelen_anker", {"facing": "north"})
        ms = self.h.ms
        self.s.entity(NPC[0], float(NPC[1]), NPC[2], {"id": "guhs:guh_npc", "Kind": "spelleiderguh", "PersistenceRequired": ms.Byte(1),
                                                     "Rotation": ms.floats(0.0, 0.0)})
        for (x, z) in ((AX - 2, AZ - 2), (AX + 2, AZ - 2), (AX - 2, AZ + 2), (AX + 2, AZ + 2)):
            self.set(x, G + 1, z, "guhs:seizoensbloembak", {"seizoen": "zomer"})

    # --- the fields ---------------------------------------------------------------------------------------------------------
    def veld(self, naam):
        x0, z0, noord = VELDEN[naam]
        rng = self.rng
        # the fences: between the lanes and at the far end; the start side is open (towards the tent)
        u_eind = 22
        for k in range(BANEN + 1):
            x = x0 + 2 + 6 * k
            for u in range(-1, u_eind + 1):
                self.set(x, G + 1, lz(z0, noord, u), HEK)
        for x in range(x0 + 2, x0 + 2 + 6 * BANEN + 1):
            self.set(x, G + 1, lz(z0, noord, u_eind), HEK)
        # lamp posts on the fence corners of the start side
        for k in range(BANEN + 1):
            x = x0 + 2 + 6 * k
            z = lz(z0, noord, -1)
            self.set(x, G + 1, z, MUUR, {"up": "true"})
            self.set(x, G + 2, z, MUUR, {"up": "true"})
            self.set(x, G + 3, z, "guhs:lampion_" + ("roze" if k % 2 else "geel"), {"hanging": "false"})
        # benches for the fans along the long sides
        for u in range(2, 20, 5):
            z = lz(z0, noord, u)
            self.set(x0, G + 1, z, "guhs:guh_bank", {"facing": "east"})
            self.set(x0 + VELD_B - 1, G + 1, z, "guhs:guh_bank", {"facing": "west"})
        # the entrance arch towards the tent (the event's scoreboard floats over it)
        za = z0 + VELD_D if noord else z0 - 1
        for x in (x0 + 9, x0 + 19):
            for y in range(G + 1, G + 5):
                self.set(x, y, za, MUUR, {"up": "true"})
        for x in range(x0 + 9, x0 + 20):
            self.set(x, G + 5, za, "minecraft:pink_wool" if x % 2 else "minecraft:white_wool")
        getattr(self, "veld_" + naam)(x0, z0, noord)

    def lane(self, x0, z0, noord, k, u, s):
        """(x, z) of lane k at (u, s)."""
        return baan_x(x0, k) + s, lz(z0, noord, u)

    def vloer(self, x0, z0, noord, blok, u0=-1, u1=22):
        for k in range(BANEN):
            for u in range(u0, u1):
                for s in range(-2, 3):
                    x, z = self.lane(x0, z0, noord, k, u, s)
                    self.set(x, G, z, blok)

    def lijn(self, x0, z0, noord, u, a, b):
        """A line across all lanes at u (start / finish): a checkered pattern of two blocks."""
        for k in range(BANEN):
            for s in range(-2, 3):
                x, z = self.lane(x0, z0, noord, k, u, s)
                self.set(x, G, z, a if (s + k) % 2 == 0 else b)

    def mat(self, x0, z0, noord, u, blok):
        for k in range(BANEN):
            for s in range(-1, 2):
                for du in (-1, 0, 1) if blok != "minecraft:pink_concrete" else (0,):
                    x, z = self.lane(x0, z0, noord, k, u + du, s)
                    self.set(x, G, z, blok)

    def veld_knabbelhappen(self, x0, z0, noord):
        self.vloer(x0, z0, noord, KLINK)
        self.mat(x0, z0, noord, HAP_MAT, "minecraft:pink_wool")
        # the gallows beam over u = 4..5 at HAP_Y, on posts on the fence lines, a striped canopy on top
        for u in (HAP_BALK, HAP_BALK + 1):
            z = lz(z0, noord, u)
            for x in range(x0 + 2, x0 + 2 + 6 * BANEN + 1):
                self.set(x, HAP_Y, z, "minecraft:pink_wool" if (x // 2) % 2 else "minecraft:white_wool")
            for k in range(BANEN + 1):
                for y in range(G + 1, HAP_Y):
                    self.set(x0 + 2 + 6 * k, y, z, "guhs:vadshout_stam", {"axis": "y"})
        for u in range(HAP_BALK - 1, HAP_BALK + 3):
            z = lz(z0, noord, u)
            for x in range(x0 + 1, x0 + 4 + 6 * BANEN):
                self.set(x, HAP_Y + 1, z, "guhs:pluisdak_plaat", {"type": "bottom"})
        for x in range(x0 + 1, x0 + 4 + 6 * BANEN, 3):
            self.set(x, HAP_Y + 2, lz(z0, noord, HAP_BALK), "guhs:lampion_roze", {"hanging": "false"})
        # a giant kaasknabbel sculpture at the side
        self.knabbel_beeld(x0 + 1, lz(z0, noord, 12))

    def veld_zaklopen(self, x0, z0, noord):
        self.vloer(x0, z0, noord, GRAS)
        self.lijn(x0, z0, noord, 0, "minecraft:white_concrete", "minecraft:white_concrete")
        self.lijn(x0, z0, noord, ZAK_FINISH, "minecraft:black_concrete", "minecraft:white_concrete")
        for u in ZAK_HOBBELS:
            for k in range(BANEN):
                for s in range(-2, 3):
                    x, z = self.lane(x0, z0, noord, k, u, s)
                    self.set(x, G + 1, z, "guhs:pluiswolblok")
        # the finish arch across the whole field, chequered
        zf = lz(z0, noord, ZAK_FINISH)
        for x in (x0 + 1, x0 + 27):
            for y in range(G + 1, G + 5):
                self.set(x, y, zf, MUUR, {"up": "true"})
        for x in range(x0 + 1, x0 + 28):
            self.set(x, G + 5, zf, "minecraft:black_wool" if x % 2 else "minecraft:white_wool")
        # sacks lying about
        for (x, z) in ((x0, lz(z0, noord, 8)), (x0 + 27, lz(z0, noord, 14))):
            self.set(x, G + 1, z, "minecraft:hay_block", {"axis": "y"})

    def veld_blikgooien(self, x0, z0, noord):
        self.vloer(x0, z0, noord, HOUT)
        self.mat(x0, z0, noord, BLIK_MAT, "minecraft:pink_wool")
        self.lijn(x0, z0, noord, BLIK_MAT + 2, "minecraft:white_concrete", "minecraft:white_concrete")
        facing = "south" if noord else "north"
        schuif = "links" if noord else "rechts"                    # (+s = east; see Blikgooien.blik)
        for k in range(BANEN):
            for s in (-1, 0, 1):
                x, z = self.lane(x0, z0, noord, k, BLIK_TAFEL, s)
                self.set(x, G + 1, z, "guhs:vadshout_planken")
            for (s, rij, sch) in BLIKKEN:
                x, z = self.lane(x0, z0, noord, k, BLIK_TAFEL, s)
                self.set(x, G + 2 + rij, z, "guhs:knabbelspelen_blik", {"facing": facing, "schuif": schuif if sch else "geen"})
            # the catch net behind the table with a big Mika face
            for u in (BLIK_TAFEL + 3,):
                for s in range(-2, 3):
                    x, z = self.lane(x0, z0, noord, k, u, s)
                    for y in range(G + 1, G + 6):
                        self.set(x, y, z, "minecraft:white_wool" if (y + s) % 2 else "minecraft:pink_wool")
                self.mika_gezicht(k, x0, z0, noord, BLIK_TAFEL + 3)

    def mika_gezicht(self, k, x0, z0, noord, u):
        """A purple Mika face on the net (eyes, a cheeky grin)."""
        for (s, y, b) in ((-1, G + 4, "minecraft:purple_wool"), (1, G + 4, "minecraft:purple_wool"), (-1, G + 2, "minecraft:purple_wool"),
                          (0, G + 2, "minecraft:magenta_wool"), (1, G + 2, "minecraft:purple_wool"), (0, G + 3, "minecraft:purple_wool")):
            x, z = self.lane(x0, z0, noord, k, u, s)
            self.set(x, y, z, b)

    def veld_eierlopen(self, x0, z0, noord):
        self.vloer(x0, z0, noord, "minecraft:smooth_sandstone")
        self.lijn(x0, z0, noord, 0, "minecraft:white_concrete", "minecraft:white_concrete")
        self.lijn(x0, z0, noord, EI_FINISH, "minecraft:black_concrete", "minecraft:white_concrete")
        kleuren = ("minecraft:pink_wool", "minecraft:light_blue_wool", "minecraft:yellow_wool", "minecraft:lime_wool")
        for k in range(BANEN):
            for i, (u, kant) in enumerate(zip(EI_VLAGGEN, EI_KANT)):
                x, z = self.lane(x0, z0, noord, k, u, kant * 2)
                self.set(x, G + 1, z, HEK)
                self.set(x, G + 2, z, HEK)
                self.set(x, G + 3, z, kleuren[i])
        # two big knabbeleggs at the start
        for x in (x0, x0 + 27):
            self.ei(x, lz(z0, noord, 3))

    def ei(self, x, z):
        for y in range(G + 1, G + 5):
            r = {G + 1: 1, G + 2: 1, G + 3: 1, G + 4: 0}[y]
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if abs(dx) + abs(dz) <= r + (1 if y == G + 2 else 0) and 0 <= x + dx < W:
                        self.set(x + dx, y, z + dz, "minecraft:pink_wool" if (y + dx) % 3 else "minecraft:white_wool")

    def veld_spijkerpoepen(self, x0, z0, noord):
        self.vloer(x0, z0, noord, GRAS)
        self.lijn(x0, z0, noord, 0, "minecraft:white_concrete", "minecraft:white_concrete")
        for k in range(BANEN):
            for u in FLES_U:
                x, z = self.lane(x0, z0, noord, k, u, 0)
                self.set(x, G, z, "guhs:knabbelspelen_kaasmelkfles", {"vol": "false"})
                for s in (-1, 1):
                    xx, zz = self.lane(x0, z0, noord, k, u, s)
                    self.set(xx, G, zz, "minecraft:yellow_concrete")
        # crates of kaasmelk at the side
        for (x, z) in ((x0, lz(z0, noord, 6)), (x0 + 27, lz(z0, noord, 12))):
            self.set(x, G + 1, z, "guhs:vadshout_planken")
            self.set(x, G + 2, z, "guhs:knabbelspelen_kaasmelkfles", {"vol": "true"})

    def veld_guhguhtje_prik(self, x0, z0, noord):
        self.vloer(x0, z0, noord, "minecraft:pink_terracotta")
        self.mat(x0, z0, noord, PRIK_MAT, "minecraft:pink_wool")
        for k in range(BANEN):
            # the board: a big guh seen from behind (round pink body, two ears), a magenta spot where the tail goes
            for s in range(-3, 4):
                for y in range(G + 1, G + 7):
                    x, z = self.lane(x0, z0, noord, k, PRIK_BORD, s)
                    rand = abs(s) == 3 or y == G + 6
                    if rand:
                        self.set(x, y, z, "guhs:vadshout_planken")
                        continue
                    lichaam = ((s / 2.3) ** 2 + ((y - (G + 2.6)) / 2.0) ** 2) <= 1.0
                    oor = y == G + 5 and abs(s) == 2
                    if s == 0 and y == G + 2:
                        b = "minecraft:magenta_concrete"
                    elif lichaam or oor:
                        b = "minecraft:pink_wool"
                    else:
                        b = "minecraft:light_blue_wool"
                    self.set(x, y, z, b)
        self.set(x0 + 1, G + 1, lz(z0, noord, 5), "guhs:seizoensbloembak", {"seizoen": "zomer"})
        self.set(x0 + 26, G + 1, lz(z0, noord, 5), "guhs:seizoensbloembak", {"seizoen": "zomer"})

    def knabbel_beeld(self, x, z):
        """A giant orange kaasknabbel on a little plinth."""
        self.set(x, G + 1, z, "guhs:knuffelsteen")
        for (dx, dy, dz) in ((0, 2, 0), (0, 3, 0), (0, 3, 1), (0, 4, 1), (0, 2, -1)):
            self.set(x + dx, G + dy, z + dz, "minecraft:orange_wool")
        self.set(x, G + 1, z + 1, "guhs:knuffelsteen")
        self.set(x, G + 1, z - 1, "guhs:knuffelsteen")

    # --- flowers, the edge, the jigsaw ----------------------------------------------------------------------------------
    def tuin(self):
        rng = self.rng
        for x in range(W):
            for z in range(D):
                if self.get(x, G, z) == GRAS and self.get(x, G + 1, z) is None and rng.random() < 0.07:
                    self.set(x, G + 1, z, rng.choice(BLOEMEN))
        for (x, z) in ((1, 1), (94, 1), (1, 94), (94, 94), (1, 48), (94, 48), (32, 94), (63, 94), (32, 1), (63, 1)):
            self.set(x, G + 1, z, MUUR, {"up": "true"})
            self.set(x, G + 2, z, MUUR, {"up": "true"})
            self.set(x, G + 3, z, "guhs:lampion_geel", {"hanging": "false"})

    def hekken(self):
        for (x, y, z), (b, props, nbt) in list(self.s.blocks.items()):
            if b == HEK:
                p = {}
                for d, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0))):
                    n = self.get(x + dx, y, z + dz)
                    p[d] = "true" if n in (HEK, MUUR, "guhs:vadshout_stam", "guhs:vadshout_planken") else "false"
                p["waterlogged"] = "false"
                self.s.blocks[(x, y, z)] = (b, p, nbt)

    def anker_jigsaw(self):
        x, y, z = JIGSAW
        self.s.blocks[(x, y, z)] = ("minecraft:jigsaw", {"orientation": "up_north"},
                                    {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty",
                                     "final_state": KLINK, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})

    def bouw(self):
        self.grond()
        self.paden()
        for naam in VELDEN:
            self.veld(naam)
        self.tent()
        self.tuin()
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
    extra = ("minecraft:jigsaw", "guhs:seizoensbloembak", "guhs:knabbelspelen_kaasmelkfles")
    reach = hulp.walk(s, [(47, G + 1, 94), (2, G + 1, 48), (93, G + 1, 48)], extra_passable=("minecraft:jigsaw",))
    if len(reach) < 3000:
        problems.append(f"only {len(reach)} walkable spots")
    x, y, z = int(NPC[0]), NPC[1], int(NPC[2])
    if s.get(x, y - 1, z) in (None, "minecraft:air") or s.get(x, y, z) not in (None, "minecraft:air"):
        problems.append("Juf Vahoegsakee doesn't stand on a floor")
    if not hulp.near_reachable(reach, x, y, z, r=2):
        problems.append("Juf Vahoegsakee can't be walked to")
    if s.get(AX, AY, AZ) != "guhs:knabbelspelen_anker":
        problems.append("the anchor isn't under Juf Vahoegsakee")
    for naam, (x0, z0, noord) in VELDEN.items():
        for k in range(BANEN):
            u0 = {"knabbelhappen": HAP_MAT, "blikgooien": BLIK_MAT, "guhguhtje_prik": PRIK_MAT}.get(naam, 0)
            sx, sz = baan_x(x0, k), lz(z0, noord, u0)
            if not hulp.near_reachable(reach, sx, G + 1, sz, r=0):
                problems.append(f"{naam}: the start of lane {k} can't be walked to")
            if s.get(sx, G + 2, sz) not in (None, "minecraft:air"):
                problems.append(f"{naam}: no headroom at the start of lane {k}")
        if naam == "blikgooien":
            for k in range(BANEN):
                for (bs, rij, _) in BLIKKEN:
                    if s.get(baan_x(x0, k) + bs, G + 2 + rij, lz(z0, noord, BLIK_TAFEL)) != "guhs:knabbelspelen_blik":
                        problems.append(f"blikgooien: a tin is missing in lane {k}")
        if naam == "spijkerpoepen":
            for k in range(BANEN):
                for u in FLES_U:
                    if s.get(baan_x(x0, k), G, lz(z0, noord, u)) != "guhs:knabbelspelen_kaasmelkfles":
                        problems.append(f"spijkerpoepen: a bottle is missing in lane {k}")
        if naam == "guhguhtje_prik":
            for k in range(BANEN):
                if s.get(baan_x(x0, k), G + 2, lz(z0, noord, PRIK_BORD)) != "minecraft:magenta_concrete":
                    problems.append(f"prik: the tail spot is missing in lane {k}")
        if naam in ("zaklopen", "eierlopen", "spijkerpoepen"):
            lane = hulp.walk(s, [(baan_x(x0, 0), G + 1, lz(z0, noord, 0))], extra_passable=extra)
            fin = (baan_x(x0, 0), G + 1, lz(z0, noord, 20))
            if naam != "zaklopen" and fin not in lane:
                problems.append(f"{naam}: the lane can't be walked to its end")
    return problems, len(reach)
