"""
bbq2 (ring-h2) - Guhvendel, "het Laatste Knusse Huis" (structure guhs:guhvendel): the elf house of chapter 2 of the
Knabbelring, in a rock cirque of the Worstenwoud with three kaassaus waterfalls. One template on a cave floor of the
Guhbarbecuether (type guhs:barbecueput through wereld.bbq_structuur), x to the east, z to the south, ground layer G (you walk
on G + 1). The cirque is open to the south, where the gate is.

  - the CIRQUE: a horseshoe of charred rock with bands of mustard, 19 high. The great fall drops down its north wall into a
    pool; two smaller falls (north-west and north-east) feed it through two brooks. From the pool the sauce runs south down
    the middle to a round basin behind the gate. Every block of sauce is written in the state it would settle in (springs in
    a notch of the rock, a lip, a falling curtain, still sauce in a bed of rock): nothing flows away, nothing needs a tick.
  - the HALL on the west bank: a long white house of quartz and mother-of-pearl with pointed windows, a steep roof of green
    copper with turned-up eaves, a pillared porch facing the stream and a round tower with a spire. Inside: the long table,
    the hearth, Guhrond's corner with the shards of Knabsil.
  - the COUNCIL RING (de raadskring) on the east bank: a round terrace inside a crown of slender pillars, seats in a circle
    around the stone table where the ring is laid, Guhrond's high seat in the north, the council bell at its entrance.
  - a narrow arched bridge without railings between the two (what else), a kitchen pavilion with the buffet and a
    Rustvuurtje in the south-east, a garden of guhbloesem trees with the knabbel pile of Leguhlas and Gimguh in the
    south-west, the gate with its two lantern pillars in the south.

The cast (ring.cast: every character only exists for players whose step of ring_h2 is in its range): BEWONERS. The
geometry the Java side needs (feature/ringh2/Guhvendel.java) is KRING, BEL and BEWONERS; the module's self-check compares
them with that file.

  bouw(h, wezens=True) -> (Structure, problems)    the template with its entities (not saved) and what its check found
  preview(out)         (python tools/features/ring_h2_bouw.py <out>) pictures from four sides, cut open, and a plan
"""
import math
import os
import random
import sys

NAAM = "guhvendel"
MIDDEN = "guhs:guhvendel_midden"
G = 4                                   # the ground layer (what you walk on is G + 1)
SIZE = (61, 38, 61)
CX, CZ = 30, 35                         # the middle of the cirque (and the centre jigsaw)
RAND = 23.0                             # the flat floor of the cirque reaches this far from the middle
KLIF = 19                               # how high the rock stands above the floor

ROTS = "guhs:houtskoolsteen"
ROTS_STENEN = "guhs:houtskoolsteen_stenen"
MOSTERD = "guhs:mosterd_blok"
NYLIUM = "guhs:mosterd_nylium"
KAASMOS = "guhs:kaasmos"
SCHEUTJES = "guhs:mosterdscheutjes"
ZWAMMETJE = "guhs:worst_zwammetje"
WORST = "guhs:worst_stam"
STENEN = "minecraft:quartz_bricks"
BLOK = "minecraft:quartz_block"
GLAD = "minecraft:smooth_quartz"
PILAAR = "minecraft:quartz_pillar"
GEBEITELD = "minecraft:chiseled_quartz_block"
QTRAP = "minecraft:quartz_stairs"
QPLAAT = "minecraft:quartz_slab"
GTRAP = "minecraft:smooth_quartz_stairs"
GPLAAT = "minecraft:smooth_quartz_slab"
PAREL = "guhs:parelmoer"
TEGEL = "guhs:parelmoer_tegels"
KORST = "guhs:kaaskorst_stenen"
DAK = "minecraft:waxed_oxidized_cut_copper"
DTRAP = "minecraft:waxed_oxidized_cut_copper_stairs"
DPLAAT = "minecraft:waxed_oxidized_cut_copper_slab"
GLAS = "minecraft:white_stained_glass_pane"
SAUS = "guhs:kaas_saus"
STAM = "guhs:guhbloesem_log"
BLAD = "guhs:guhbloesem_leaves"
PLANKEN = "guhs:guhbloesem_planks"
LANTAARN = "minecraft:lantern"
KETTING = "minecraft:chain"
STAAF = "minecraft:end_rod"
LAMP = "guhs:guh_kristal_lamp"
UI = "guhs:uienlicht"
KNABBELBLOK = "guhs:block_of_kaasknabbels"
KLOK = "minecraft:bell"
VUUR = "guhs:ring_rustvuur"
BORD = "minecraft:cherry_wall_sign"
VAANDEL = "minecraft:lime_wall_banner"
TRALIES = "minecraft:iron_bars"
BOEKEN = "minecraft:bookshelf"
VAT = "minecraft:barrel"
BLOEMEN = ("guhs:knabbelroos", "guhs:kaasbloem", "guhs:roze_guhbloem")
AIR = "minecraft:air"

# things a player walks through (the walking check) and that never carry anything
DUN = (AIR, SCHEUTJES, ZWAMMETJE, LANTAARN, KETTING, STAAF, BORD, VAANDEL, "minecraft:white_carpet", "minecraft:lime_carpet",
       "minecraft:yellow_carpet") + BLOEMEN

# --- the hall (west bank) ----------------------------------------------------------------------------------------------
HX0, HX1, HZ0, HZ1 = 9, 22, 19, 33      # outer walls
HV = G + 1                              # its floor block (you stand on G + 2)
MUUR_TOP = G + 7
DEUR_Z = 26
TX, TZ, TR = 10, 35, 3.4                # the tower: axis and radius
TOREN_TOP = G + 18

# --- the council ring (east bank) -----------------------------------------------------------------------------------------
KX, KZ, KR = 41, 27, 7.3
KRING = (KX, G + 2, KZ)                 # the stone table in its middle: the anchor of the two cutscenes
BEL = (36, G + 3, 24)                   # the council bell (on its block, just inside the entrance)
# the seats, relative to the middle: the high seat, the benches (slabs) and the cushions
ZETEL = (0, -5)
BANKJES = [(5, -1), (5, 1), (-1, 5), (1, 5)]
KUSSENS = [(3, -4), (4, -3), (-3, -4), (-4, -3), (3, 4), (4, 3), (-3, 4), (-4, 3)]

# --- the rest ----------------------------------------------------------------------------------------------------------------
STROOM = (30, 31)                       # the x of the stream
BRUG_Z = (26, 27, 28)
BRUGJE_Z = (44, 45)
BEKKEN = (30.5, 52.0, 3.2)              # the round basin: middle and radius
POORT_Z = 57
PX0, PX1, PZ0, PZ1 = 37, 45, 40, 46     # the kitchen pavilion (its pillars stand on the corners)
RUSTVUUR = (38, G + 1, 51)
STAPEL = (19, 41)                       # the knabbel pile (2 x 2)
BOMEN = [(16, 40, 5), (23, 47, 4), (12, 48, 6), (47, 36, 5), (21, 54, 4), (40, 55, 5), (25, 20, 4)]
WORSTEN = [(5, 27, 9, 1), (55, 41, 11, -1), (40, 9, 8, 1)]      # (x, z, height, lean) on top of the rock

# the falls: (the x of the curtain, the z of the spring, how high above the floor the spring is); the curtain falls one
# block south of its spring, into a pool
VALLEN = [((29, 30, 31), 10, KLIF), ((17, 18), 13, 13), ((42, 43), 13, 13)]

# --- the cast ----------------------------------------------------------------------------------------------------------------
# (id, kind, x, y, z, dy, yaw, van, tot): the block the character stands in, how far above its bottom, where it looks
# (0 = south, 90 = west), and the steps of ring_h2 it exists for. Steps 0-3: all over the house; 4-5: in the council
# ring; from 6 on only Guhrond is left (the fellowship only exists in its own scenes).
PLEK = "guhvendel"
VOOR, RAAD, NA = (0, 3), (4, 5), (6, 99)
BEWONERS = [
    ("ringh2_guhrond_stoep", "guhrond", 24, G + 2, 25, 0.0, 270.0) + VOOR,
    ("ringh2_guhdalf_voor", "guhdalf", 34, G + 1, 30, 0.0, 135.0) + VOOR,
    ("ringh2_araguh_voor", "araguh", 34, G + 1, 55, 0.0, 0.0) + VOOR,
    ("ringh2_leguhlas_voor", "leguhlas", 18, G + 1, 44, 0.0, 270.0) + VOOR,
    ("ringh2_gimguh_voor", "gimguh", 21, G + 1, 44, 0.0, 90.0) + VOOR,
    ("ringh2_boromika_voor", "boromika", 15, G + 2, 30, 0.0, 0.0) + VOOR,
    ("ringh2_merrie_voor", "merrie", 40, G + 1, 44, 0.0, 180.0) + VOOR,
    ("ringh2_pippguh_voor", "pippguh", 42, G + 1, 44, 0.0, 180.0) + VOOR,
    ("ringh2_guhrond_raad", "guhrond", KX, G + 2, KZ - 5, 0.5, 0.0) + RAAD,
    ("ringh2_guhdalf_raad", "guhdalf", KX + 3, G + 2, KZ - 4, 0.5, 45.0) + RAAD,
    ("ringh2_araguh_raad", "araguh", KX - 3, G + 2, KZ - 4, 0.5, 315.0) + RAAD,
    ("ringh2_leguhlas_raad", "leguhlas", KX + 5, G + 2, KZ - 1, 0.5, 90.0) + RAAD,
    ("ringh2_gimguh_raad", "gimguh", KX + 5, G + 2, KZ + 1, 0.5, 90.0) + RAAD,
    ("ringh2_boromika_raad", "boromika", KX + 3, G + 2, KZ + 4, 0.5, 135.0) + RAAD,
    ("ringh2_merrie_raad", "merrie", 49, G + 1, 31, 0.0, 60.0) + RAAD,
    ("ringh2_pippguh_raad", "pippguh", 50, G + 1, 29, 0.0, 80.0) + RAAD,
    ("ringh2_guhrond_thuis", "guhrond", 24, G + 2, 27, 0.0, 270.0) + NA,
]
# where a player stands at the gate (the walking check starts here)
INGANG = (30, G + 1, 58)


def _ruis(x, z, seed=0):
    n = (x * 73856093) ^ (z * 19349663) ^ (seed * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def _glad(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


class Bouw:
    def __init__(self, h):
        self.h = h
        self.s = h.Structure(SIZE)
        self.rng = random.Random(21301700)
        self.W, self.H, self.D = SIZE
        self.top = {}              # (x, z) -> the y of the top block of the rock / the floor
        self.nat = set()           # (x, z) with sauce in the floor
        self.vrij = set()          # (x, z) of the floor that must stay bare (paths, buildings, sauce)

    # --- small things ---------------------------------------------------------------------------------------------------------
    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, name, props, nbt)

    def get(self, x, y, z):
        return self.s.get(x, y, z)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def trap(self, x, y, z, facing, name=QTRAP, om=False):
        self.set(x, y, z, name, {"facing": facing, "half": "top" if om else "bottom", "shape": "straight", "waterlogged": "false"})

    def plaat(self, x, y, z, name=QPLAAT, boven=False):
        self.set(x, y, z, name, {"type": "top" if boven else "bottom", "waterlogged": "false"})

    def zuil(self, x, z, y0, y1, name=PILAAR):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, name, {"axis": "y"} if name == PILAAR else None)

    def lantaarn(self, x, y, z, hangt=False):
        self.set(x, y, z, LANTAARN, {"hanging": "true" if hangt else "false", "waterlogged": "false"})

    def bord(self, x, y, z, facing, regels):
        import sign_text
        B = self.h.Byte
        prefix = "sign.guhs.ring_h2"
        leeg = sign_text.messages(prefix, ["", "", "", ""])
        nbt = {"id": "minecraft:sign", "is_waxed": B(1),
               "front_text": {"messages": sign_text.messages(prefix, regels), "color": "black", "has_glowing_text": B(0)},
               "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}}
        self.set(x, y, z, BORD, {"facing": facing, "waterlogged": "false"}, nbt)

    def vaandel(self, x, y, z, facing):
        self.set(x, y, z, VAANDEL, {"facing": facing}, {"id": "minecraft:banner"})

    # --- the cirque ---------------------------------------------------------------------------------------------------------
    def hoogte(self, x, z):
        """How high the rock stands above the floor here (0 = the floor of the cirque), or None outside the plate."""
        dx, dz = x - CX, z - CZ
        r = math.hypot(dx, dz)
        hoek = abs(math.degrees(math.atan2(dx, dz)))            # 0 = south (the opening), 180 = north
        wand = _glad((hoek - 44) / 30.0)                        # 0 in the opening, 1 where the wall stands
        buiten = 25.2 + 4.6 * wand + (_ruis(x, z, 1) - 0.5) * 1.2
        if r > buiten:
            return None
        klif = wand * min(KLIF, max(0.0, (r - RAND + 0.3) * 3.6))
        klif *= max(0.35, min(1.0, (buiten - r) / 1.6 + 0.45))  # the back of the wall rounds off
        if klif > 2:
            klif += (_ruis(x // 2, z // 2, 2) - 0.5) * 2.4 + (1.5 if _ruis(x // 4, z // 4, 3) > 0.62 else 0.0)   # ledges, not speckles
        return max(0, min(KLIF + 1, int(round(klif))))

    def terrein(self):
        for x in range(self.W):
            for z in range(self.D):
                hg = self.hoogte(x, z)
                if hg is None:
                    continue
                # the notches of the falls: a sheer wall behind each curtain, the full height
                for xs, zb, hoog in VALLEN:
                    if xs[0] - 2 <= x <= xs[-1] + 2:
                        if z < zb:
                            hg = max(hg, hoog + 2)
                        elif z == zb:
                            hg = hoog - 1 if xs[0] <= x <= xs[-1] else max(hg, hoog + 1)
                        elif z <= zb + 5 and xs[0] - 1 <= x <= xs[-1] + 1:
                            hg = 0
                self.top[(x, z)] = G + hg
        for (x, z), top in self.top.items():
            for y in range(0, top + 1):
                self.set(x, y, z, self.steen(x, y, z, top))
        # the air of the cirque: a dome over the plate, so the cave's own rock never hangs into the house
        for (x, z), top in self.top.items():
            r = math.hypot(x - CX, z - CZ)
            koepel = G + 10 + int(25 * math.sqrt(max(0.0, 1 - (r / 31.5) ** 2)))
            for y in range(top + 1, min(self.H, max(koepel, top + 5))):
                self.set(x, y, z, AIR)

    def steen(self, x, y, z, top):
        if top <= G:
            if y < G:
                return ROTS
            return NYLIUM if _ruis(x, z, 5) > 0.16 else KAASMOS
        # the wall: charred rock with wavy bands of mustard, mustard nylium on every ledge
        if y == top:
            return NYLIUM if _ruis(x // 2, z // 2, 6) > 0.22 else ROTS
        band = (y + int(2.2 * math.sin(x * 0.31 + z * 0.17))) % 6
        if y > G and band == 0:
            return MOSTERD
        return ROTS_STENEN if _ruis(x + y, z - y, 8) > 0.93 else ROTS

    def vloer(self, x, z):
        return self.top.get((x, z), -1) == G

    # --- the sauce -----------------------------------------------------------------------------------------------------------
    def bedding(self, x, z, oever=True):
        """Still sauce in the floor at (x, z): one deep, in a bed of rock."""
        self.set(x, G, z, SAUS, {"level": "0"})
        self.set(x, G - 1, z, ROTS)
        self.nat.add((x, z))
        self.vrij.add((x, z))

    def saus(self):
        # the pools at the foot of the falls
        for (xs, zb, hoog), (rx, rz) in zip(VALLEN, ((4.6, 2.6), (2.7, 1.9), (2.7, 1.9))):
            mx, mz = (xs[0] + xs[-1]) / 2.0, zb + 2.6
            for x in range(int(mx - rx - 1), int(mx + rx + 2)):
                for z in range(zb + 1, int(mz + rz + 2)):
                    if ((x - mx) / rx) ** 2 + ((z - mz) / rz) ** 2 <= 1.0 and self.vloer(x, z):
                        self.bedding(x, z)
            for x in xs:
                self.bedding(x, zb + 1)
                # the spring in its notch, the lip, the curtain
                top = G + hoog
                self.set(x, top, zb, SAUS, {"level": "0"})
                self.set(x, top, zb + 1, SAUS, {"level": "1"})
                for y in range(G + 1, top):
                    self.set(x, y, zb + 1, SAUS, {"level": "8"})
            # a stone cheek on either side of the lip (the notch itself is closed by the rock: terrein)
            for x in (xs[0] - 1, xs[-1] + 1):
                self.set(x, G + hoog, zb + 1, ROTS)
                self.set(x, G + hoog - 1, zb + 1, ROTS_STENEN)
        # the two brooks from the small pools to the great one
        for x in range(20, 29):
            self.bedding(x, 15)
        for x in range(33, 41):
            self.bedding(x, 15)
        # the stream down the middle, and the basin behind the gate
        for z in range(15, 50):
            for x in STROOM:
                self.bedding(x, z)
        bx, bz, br = BEKKEN
        for x in range(int(bx - br - 1), int(bx + br + 2)):
            for z in range(int(bz - br - 1), int(bz + br + 2)):
                if math.hypot(x - bx, z - bz) <= br:
                    self.bedding(x, z)
        # banks: a rim of worked stone around all still sauce (and never a hole next to it)
        for (x, z) in sorted(self.nat):
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    n = (x + dx, z + dz)
                    if n in self.nat or n not in self.top:
                        continue
                    if self.top[n] == G:
                        self.set(n[0], G, n[1], GLAD if (n[0] + n[1]) % 5 else KORST)
                        self.vrij.add(n)
        # a lamp in the middle of the basin
        self.set(30, G, 52, GLAD)
        self.set(31, G, 52, GLAD)
        self.nat.discard((30, 52))
        self.nat.discard((31, 52))
        for x in (30, 31):
            self.set(x, G + 1, 52, GEBEITELD)
        self.set(30, G + 2, 52, UI)
        self.set(31, G + 2, 52, UI)

    # --- paths, bridges ---------------------------------------------------------------------------------------------------
    def tegel(self, x, z):
        if self.vloer(x, z) and (x, z) not in self.nat:
            self.set(x, G, z, TEGEL if _ruis(x, z, 11) > 0.12 else PAREL)
            self.vrij.add((x, z))

    def paden(self):
        def lijn(x0, z0, x1, z1, breed=2):
            n = int(max(abs(x1 - x0), abs(z1 - z0))) + 1
            for i in range(n + 1):
                x, z = x0 + (x1 - x0) * i / n, z0 + (z1 - z0) * i / n
                for dx in range(breed):
                    for dz in range(breed):
                        self.tegel(int(round(x)) + dx, int(round(z)) + dz)
        lijn(30, 59, 30, 56)                       # in through the gate
        lijn(30, 56, 26, 53), lijn(31, 56, 35, 53)                         # round the basin
        lijn(26, 53, 27, 46), lijn(35, 53, 33, 46)
        lijn(27, 46, 27, 25), lijn(33, 46, 33, 25)                         # up both banks
        lijn(27, 25, 27, 19), lijn(33, 25, 33, 19)                         # on to the pool
        for x in (26, 27, 28):                     # from the hall's steps to the Bridge
            for z in range(24, 30):
                self.tegel(x, z)
        lijn(33, 43, 37, 43), lijn(34, 47, 36, 49)                         # to the pavilion and the fire
        lijn(27, 43, 20, 43)                       # into the garden
        lijn(20, 17, 26, 17), lijn(34, 17, 40, 17)                         # along the brooks

    def bruggen(self):
        # the Bridge: three wide, arched, and of course no railings
        for z in BRUG_Z:
            self.plaat(29, G + 1, z, GPLAAT)
            self.plaat(30, G + 1, z, GPLAAT, boven=True)
            self.plaat(31, G + 1, z, GPLAAT, boven=True)
            self.plaat(32, G + 1, z, GPLAAT)
        for z in (BRUG_Z[0] - 1, BRUG_Z[-1] + 1):
            for x in (29, 32):
                self.set(x, G + 1, z, GEBEITELD)
                self.lantaarn(x, G + 2, z)
        # the little bridge further south: a row of slabs
        for z in BRUGJE_Z:
            for x in (29, 30, 31, 32):
                self.plaat(x, G + 1, z, GPLAAT)

    # --- the hall ---------------------------------------------------------------------------------------------------------------
    def dak_y(self, x):
        """The y of the roof over column x of the hall (its ridge runs north-south between x 15 and 16)."""
        return MUUR_TOP + 1 + (x - HX0 if x <= 15 else HX1 - x)

    def hal(self):
        # the plinth and the floor
        for x in range(HX0, HX1 + 1):
            for z in range(HZ0, HZ1 + 1):
                for y in range(G, HV):
                    self.set(x, y, z, BLOK)
                rand = x in (HX0 + 1, HX1 - 1) or z in (HZ0 + 1, HZ1 - 1)
                self.set(x, HV, z, KORST if rand else TEGEL if (x + z) % 2 else PAREL)
                for y in range(HV + 1, self.dak_y(x) + 1):
                    self.set(x, y, z, AIR)
                self.vrij.add((x, z))
        # walls: mother-of-pearl between pillars of quartz
        for x in range(HX0, HX1 + 1):
            for z in range(HZ0, HZ1 + 1):
                if x not in (HX0, HX1) and z not in (HZ0, HZ1):
                    continue
                hoek = (x in (HX0, HX1) and z in (HZ0, HZ1))
                stijl = hoek or (x in (HX0, HX1) and z in (23, 29)) or (z in (HZ0, HZ1) and x in (13, 18))
                top = MUUR_TOP if x in (HX0, HX1) else self.dak_y(x) - 1
                for y in range(HV + 1, top + 1):
                    if stijl:
                        self.set(x, y, z, PILAAR, {"axis": "y"})
                    elif y == HV + 1:
                        self.set(x, y, z, STENEN)
                    elif y == MUUR_TOP and x in (HX0, HX1):
                        self.set(x, y, z, BLOK)
                    else:
                        self.set(x, y, z, PAREL)
        # pointed windows: two wide, glass below a little arch of upside-down stairs
        def raam(cells, as_, y0=HV + 2, hoog=3):
            (a, b) = cells
            for (x, z) in cells:
                for y in range(y0, y0 + hoog):
                    self.set(x, y, z, GLAS)
            if as_ == "z":           # the wall runs along z: the arch's feet stand north and south
                self.trap(a[0], y0 + hoog, a[1], "north", GTRAP, om=True)
                self.trap(b[0], y0 + hoog, b[1], "south", GTRAP, om=True)
            else:
                self.trap(a[0], y0 + hoog, a[1], "west", GTRAP, om=True)
                self.trap(b[0], y0 + hoog, b[1], "east", GTRAP, om=True)
        for z in (20, 30):
            raam(((HX1, z), (HX1, z + 1)), "z")                   # the east front, either side of the porch
        for z in (25, 30):
            raam(((HX0, z), (HX0, z + 1)), "z")                   # the west wall (over the rock)
        raam(((15, HZ0), (16, HZ0)), "x", HV + 3, 5)              # the north gable: a tall one, towards the fall
        raam(((15, HZ1), (16, HZ1)), "x", HV + 3, 5)              # and the south gable
        raam(((11, HZ0), (12, HZ0)), "x")
        raam(((19, HZ0), (20, HZ0)), "x")
        raam(((19, HZ1), (20, HZ1)), "x")
        # the doorway in the east front: three wide, four high, a pointed top
        for z in (DEUR_Z - 1, DEUR_Z, DEUR_Z + 1):
            for y in range(HV + 1, HV + 5):
                self.set(HX1, y, z, AIR)
        self.trap(HX1, HV + 4, DEUR_Z - 1, "north", GTRAP, om=True)
        self.trap(HX1, HV + 4, DEUR_Z + 1, "south", GTRAP, om=True)
        self.set(HX1, HV + 5, DEUR_Z, GEBEITELD)
        # the roof: green copper, steep, one block over the walls all round, the eaves turned up
        for z in range(HZ0 - 1, HZ1 + 2):
            for x in range(HX0, HX1 + 1):
                y = self.dak_y(x)
                self.trap(x, y, z, "east" if x <= 15 else "west", DTRAP)
            self.trap(HX0 - 1, MUUR_TOP, z, "east", DTRAP)
            self.trap(HX1 + 1, MUUR_TOP, z, "west", DTRAP)
            self.plaat(HX0 - 2, MUUR_TOP, z, DPLAAT)
            self.plaat(HX1 + 2, MUUR_TOP, z, DPLAAT)
        for z in range(HZ0 - 1, HZ1 + 2, 4):
            self.set(15, self.dak_y(15) + 1, z, STAAF, {"facing": "up"})
        # the porch: two pillars, a little pointed roof of its own, steps down to the stream
        for x in range(HX1 + 1, HX1 + 4):
            for z in range(DEUR_Z - 2, DEUR_Z + 3):
                for y in range(G, HV):
                    self.set(x, y, z, BLOK)
                self.set(x, HV, z, TEGEL if (x + z) % 2 else PAREL)
                self.vrij.add((x, z))
        for z in (DEUR_Z - 2, DEUR_Z + 2):
            for x in (HX1 + 3,):
                self.zuil(x, z, HV + 1, HV + 4)
                self.set(x, HV + 5, z, GEBEITELD)
        for x in range(HX1 + 1, HX1 + 5):
            for i, z in enumerate(range(DEUR_Z - 3, DEUR_Z + 4)):
                y = HV + 5 + min(i, 6 - i)
                if i == 3:
                    self.plaat(x, y, z, DPLAAT)
                    self.set(x, y - 1, z, DAK)
                else:
                    self.trap(x, y, z, "south" if i < 3 else "north", DTRAP)
        for z in (DEUR_Z - 1, DEUR_Z + 1):                        # the pediment
            self.set(HX1 + 3, HV + 6, z, BLOK)
        self.set(HX1 + 3, HV + 6, DEUR_Z, LAMP)
        self.set(HX1 + 3, HV + 7, DEUR_Z, BLOK)
        self.set(HX1 + 4, HV + 9, DEUR_Z, STAAF, {"facing": "up"})
        for z in (DEUR_Z - 1, DEUR_Z, DEUR_Z + 1):
            self.trap(HX1 + 4, HV, z, "west", GTRAP)
            self.set(HX1 + 4, G, z, BLOK)
            self.vrij.add((HX1 + 4, z))
        for z in (DEUR_Z - 2, DEUR_Z + 2):
            self.lantaarn(HX1 + 4, G + 2, z)
            self.set(HX1 + 4, G + 1, z, GEBEITELD)
            self.vrij.add((HX1 + 4, z))
        self.hal_binnen()

    def hal_binnen(self):
        v = HV + 1
        # the long table (slabs on the upper half) with cushions along it
        for z in range(23, 30):
            for x in (15, 16):
                self.plaat(x, v, z, GPLAAT, boven=True)
            if z % 2:
                self.set(14, v, z, "guhs:lime_kussen", {"facing": "east"})
                self.set(17, v, z, "guhs:white_kussen", {"facing": "west"})
        self.set(15, v + 1, 24, "guhs:guh_taart")
        self.lantaarn(16, v + 1, 26)
        self.set(15, v + 1, 28, "guhs:potted_knabbelroos")
        # the hearth in the west wall, between its two windows
        for z in (27, 28, 29):
            self.set(HX0 + 1, v, z, ROTS_STENEN)
            self.set(HX0 + 1, v + 2, z, ROTS_STENEN)
        self.set(HX0 + 1, v + 1, 27, ROTS_STENEN)
        self.set(HX0 + 1, v + 1, 29, ROTS_STENEN)
        self.set(HX0 + 1, v + 1, 28, UI)
        self.set(HX0 + 2, v + 1, 28, TRALIES)
        for y in range(v + 3, MUUR_TOP + 4):
            self.set(HX0 + 1, y, 28, ROTS_STENEN)
        # Guhrond's corner in the north: a low dais, his books, two banners
        for x in range(HX0 + 1, HX1):
            self.plaat(x, v, HZ0 + 1, GPLAAT)
            self.plaat(x, v, HZ0 + 2, GPLAAT)
        for x in (HX0 + 1, HX0 + 2, HX1 - 2, HX1 - 1):
            self.set(x, v, HZ0 + 1, BOEKEN)
            self.set(x, v + 1, HZ0 + 1, BOEKEN)
        self.vaandel(13, v + 3, HZ0 + 1, "south")
        self.vaandel(18, v + 3, HZ0 + 1, "south")
        # the shards of Knabsil, on their stone in the south
        self.set(15, v, HZ1 - 1, GEBEITELD)
        self.set(16, v, HZ1 - 1, GEBEITELD)
        self.set(15, v + 1, HZ1 - 1, "guhs:knabbelbak", {"facing": "north"})
        self.bord(16, v + 1, HZ1 - 1, "north", ["De scherven van", "Knabsil", "Niet aankomen!", "(Er is al geproefd)"])
        # light: crystal lamps on the wall pillars, lanterns under the ridge
        for (x, z) in ((HX0 + 1, 23), (HX1 - 1, 23), (HX1 - 1, 29), (13, HZ1 - 1), (18, HZ1 - 1)):
            self.set(x, v + 3, z, LAMP)
        for z in (22, 26, 30):
            self.set(15, self.dak_y(15) - 1, z, KETTING, {"axis": "y", "waterlogged": "false"})
            self.lantaarn(15, self.dak_y(15) - 2, z, hangt=True)

    def toren(self):
        """The round tower on the hall's south-west corner, with a pointed copper cap and a spire."""
        cellen = [(x, z) for x in range(TX - 4, TX + 5) for z in range(TZ - 4, TZ + 5)]
        for (x, z) in cellen:
            d = math.hypot(x - TX, z - TZ)
            if d > TR:
                continue
            binnen_hal = HX0 <= x <= HX1 and HZ0 <= z <= HZ1
            wand = d > TR - 1.15
            for y in range(G, TOREN_TOP + 1):
                if binnen_hal and y <= self.dak_y(x) and not (x in (HX0, HX1) or z in (HZ0, HZ1)):
                    continue                                  # (the hall's own room stays the hall's)
                if y <= HV:
                    self.set(x, y, z, BLOK)
                elif wand:
                    ring = y in (HV + 6, HV + 12)
                    self.set(x, y, z, BLOK if ring else PAREL if (y - HV) % 6 else STENEN)
                else:
                    self.set(x, y, z, AIR)
            self.vrij.add((x, z))
        # slit windows, a lamp room under the cap
        for (dx, dz) in ((0, 3), (-3, 0), (3, 0)):
            for y in (HV + 3, HV + 4, HV + 9, HV + 10, HV + 15, HV + 16):
                if not (HX0 <= TX + dx <= HX1 and HZ0 <= TZ + dz <= HZ1):
                    self.set(TX + dx, y, TZ + dz, GLAS)
        # the cap: rings of copper that get smaller, the eaves one wider than the wall
        for i, r in enumerate((4.5, 3.6, 2.9, 2.2, 1.5, 0.8)):
            y = TOREN_TOP + 1 + i
            for (x, z) in cellen:
                d = math.hypot(x - TX, z - TZ)
                if d <= r:
                    if i == 0 and d > TR:
                        self.plaat(x, y, z, DPLAAT)
                    else:
                        self.set(x, y, z, DAK)
        self.set(TX, TOREN_TOP + 7, TZ, DAK)
        self.set(TX, TOREN_TOP + 8, TZ, STAAF, {"facing": "up"})
        self.set(TX, TOREN_TOP + 9, TZ, STAAF, {"facing": "up"})

    # --- the council ring -------------------------------------------------------------------------------------------------
    def kring(self):
        v = G + 1
        cellen = [(x, z) for x in range(KX - 9, KX + 10) for z in range(KZ - 9, KZ + 10)]
        for (x, z) in cellen:
            d = math.hypot(x - KX, z - KZ)
            if d > KR:
                continue
            self.set(x, G, z, BLOK)
            if 2.6 <= d < 3.6:
                self.set(x, v, z, KORST)                       # the golden ring in the floor
            elif d > KR - 1.0:
                self.set(x, v, z, GLAD)
            else:
                self.set(x, v, z, TEGEL if (x + z) % 2 else PAREL)
            self.vrij.add((x, z))
        # steps up from the bridge (the west) and a ring of low steps for the eye
        for z in (KZ - 1, KZ, KZ + 1):
            self.trap(KX - 8, v, z, "east", GTRAP)
            self.set(KX - 8, G, z, BLOK)
            self.vrij.add((KX - 8, z))
        # the crown: slender pillars with a ring of slabs on top, a lantern hanging between every two
        pilaren = []
        for i in range(12):
            a = math.radians(i * 30 + 15)
            x, z = int(round(KX + math.cos(a) * 6.4)), int(round(KZ + math.sin(a) * 6.4))
            pilaren.append((x, z))
            self.zuil(x, z, v + 1, v + 5)
            self.set(x, v + 6, z, GEBEITELD)
        for (x, z) in cellen:
            d = math.hypot(x - KX, z - KZ)
            if 5.7 <= d <= 7.2:
                if (x, z) not in pilaren:
                    self.plaat(x, v + 6, z, GPLAAT, boven=True)
                if 6.3 <= d and _ruis(x, z, 21) > 0.45:
                    self.set(x, v + 7, z, BLAD, {"persistent": "true", "distance": "1", "waterlogged": "false"})
        for i in range(12):
            a = math.radians(i * 30)
            x, z = int(round(KX + math.cos(a) * 6.3)), int(round(KZ + math.sin(a) * 6.3))
            if (x, z) not in pilaren and self.get(x, v + 6, z) == GPLAAT and i != 6:
                self.lantaarn(x, v + 5, z, hangt=True)
        # the stone table in the middle (where the ring is laid)
        self.set(KX, v + 1, KZ, GEBEITELD)
        # the seats: Guhrond's high seat in the north, benches and cushions around
        zx, zz = KX + ZETEL[0], KZ + ZETEL[1]
        self.plaat(zx, v + 1, zz, QPLAAT)
        for dx in (-1, 1):
            self.zuil(zx + dx, zz, v + 1, v + 2)
            self.set(zx + dx, v + 3, zz, LAMP)
        self.zuil(zx, zz - 1, v + 1, v + 4)
        self.vaandel(zx, v + 3, zz, "south")
        for (dx, dz) in BANKJES:
            self.plaat(KX + dx, v + 1, KZ + dz, QPLAAT)
        for i, (dx, dz) in enumerate(KUSSENS):
            self.set(KX + dx, v + 1, KZ + dz, "guhs:lime_kussen" if i % 2 else "guhs:white_kussen",
                     {"facing": "south" if dz < 0 else "north"})
        # the council bell on its block, and a sign
        self.set(BEL[0], BEL[1] - 1, BEL[2], GEBEITELD)
        self.set(*BEL, KLOK, {"attachment": "floor", "facing": "south", "powered": "false"})
        self.bord(KX - 7, v + 2, KZ + 2, "west", ["Raad van Guhrond", "Luid de bel", "en ga zitten.", "Om de beurt praten"])

    # --- the kitchen pavilion ---------------------------------------------------------------------------------------------
    def paviljoen(self):
        for x in range(PX0, PX1 + 1):
            for z in range(PZ0, PZ1 + 1):
                self.set(x, G, z, TEGEL if (x + z) % 2 else PAREL)
                self.vrij.add((x, z))
        for x in (PX0, (PX0 + PX1) // 2, PX1):
            for z in (PZ0, PZ1):
                self.zuil(x, z, G + 1, G + 5)
        # a hipped roof of copper, one over the pillars
        for i in range(5):
            x0, x1, z0, z1 = PX0 - 1 + i, PX1 + 1 - i, PZ0 - 1 + i, PZ1 + 1 - i
            y = G + 5 + i
            if z0 > z1:
                break
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    rand = x in (x0, x1) or z in (z0, z1)
                    if not rand:
                        continue
                    if z0 == z1:
                        self.plaat(x, y, z, DPLAAT)
                    elif z == z0:
                        self.trap(x, y, z, "south", DTRAP)
                    elif z == z1:
                        self.trap(x, y, z, "north", DTRAP)
                    elif x == x0:
                        self.trap(x, y, z, "east", DTRAP)
                    else:
                        self.trap(x, y, z, "west", DTRAP)
        self.set((PX0 + PX1) // 2, G + 10, (PZ0 + PZ1) // 2, STAAF, {"facing": "up"})
        # the buffet along the north side, barrels and a shelf of pots, cushions to sit on
        for x in range(PX0 + 2, PX1 - 1):
            self.set(x, G + 1, PZ0 + 1, "guhs:feestbuffettafel", {"facing": "south", "gedekt": "true"})
        for (x, z) in ((PX0 + 1, PZ0 + 1), (PX1 - 1, PZ0 + 1)):
            self.set(x, G + 1, z, VAT, {"facing": "up", "open": "false"}, {"id": "minecraft:barrel"})
        self.set(PX1 - 1, G + 2, PZ0 + 1, "guhs:theepotje")
        self.set(PX0 + 1, G + 2, PZ0 + 1, KNABBELBLOK)
        for (x, z, kleur) in ((PX0 + 2, PZ1 - 1, "yellow"), (PX1 - 2, PZ1 - 1, "lime"), (PX0 + 4, PZ1 - 1, "white")):
            self.set(x, G + 1, z, f"guhs:{kleur}_kussen", {"facing": "north"})
        for x in (PX0 + 2, PX1 - 2):
            for z in (PZ0, PZ1):
                self.lantaarn(x, G + 5, z, hangt=True)
        self.set(PX0, G + 2, PZ1 + 1, GEBEITELD)
        self.set(PX0, G + 1, PZ1 + 1, GEBEITELD)
        self.bord(PX0, G + 2, PZ1 + 2, "south", ["Tweede ontbijt:", "van 9 uur tot op.", "Derde ontbijt:", "in overleg, njeg"])
        # the rest fire in its ring of stones, a sleeping place beside it
        rx, _, rz = RUSTVUUR
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) + abs(dz) <= 3 and self.vloer(rx + dx, rz + dz) and (rx + dx, rz + dz) not in self.nat:
                    self.set(rx + dx, G, rz + dz, ROTS_STENEN if (dx + dz) % 2 else KORST)
                    self.vrij.add((rx + dx, rz + dz))
        self.set(rx, G + 1, rz, VUUR)
        self.set(rx + 2, G + 1, rz + 1, STAM, {"axis": "z"})
        self.set(rx - 1, G + 1, rz + 2, STAM, {"axis": "x"})
        self.set(rx + 3, G + 1, rz - 1, "minecraft:lime_carpet")
        self.set(rx + 4, G + 1, rz - 1, "minecraft:lime_carpet")
        self.set(rx + 5, G + 1, rz - 1, "guhs:white_kussen", {"facing": "west"})
        for n in ((rx + 3, rz - 1), (rx + 4, rz - 1), (rx + 5, rz - 1)):
            self.vrij.add(n)

    # --- the garden, the trees, the gate ------------------------------------------------------------------------------------
    def boom(self, x, z, hoog):
        """A guhbloesem tree: a slightly bent trunk under a wide, flat cloud of pink."""
        if not self.vloer(x, z):
            return
        blad = {"persistent": "true", "distance": "1", "waterlogged": "false"}
        self.vrij.add((x, z))
        for y in range(G + 1, G + 1 + hoog):
            self.set(x, y, z, STAM, {"axis": "y"})
        ty = G + hoog
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                for dy in range(-1, 3):
                    d = math.hypot(dx, dz) + abs(dy - 0.5) * 1.5
                    if d <= 3.3 + (self.rng.random() - 0.5) * 0.9 and self.get(x + dx, ty + dy, z + dz) in (None, AIR):
                        self.set(x + dx, ty + dy, z + dz, BLAD, blad)

    def worst(self, x, z, hoog, helling):
        """A braadworst of the Worstenwoud on top of the rock: a fat sausage with a zigzag of mustard."""
        top = self.top.get((x, z))
        if top is None:
            return
        for i in range(hoog):
            sx = x + (helling * int((i / hoog) ** 2 * 3))
            r = 1.5 if i < hoog - 2 else 0.9
            for dx in range(-2, 3):
                for dz in range(-2, 3):
                    if dx * dx + dz * dz <= r * r + 0.2 and self.get(sx + dx, top + 1 + i, z + dz) in (None, AIR):
                        self.set(sx + dx, top + 1 + i, z + dz, WORST, {"axis": "y"})
            a = (i % 6 if i % 6 < 3 else 6 - i % 6) * 0.9
            mx, mz = sx + int(round(math.cos(a) * 2.0)), z + int(round(math.sin(a) * 2.0))
            if 1 < i < hoog - 2 and self.get(mx, top + 1 + i, mz) in (None, AIR):
                self.set(mx, top + 1 + i, mz, MOSTERD)
        self.set(x + helling * int(((hoog - 1) / hoog) ** 2 * 3), top + 1 + hoog, z, MOSTERD)

    def tuin(self):
        for x, z, hoog in BOMEN:
            self.boom(x, z, hoog)
        for x, z, hoog, helling in WORSTEN:
            self.worst(x, z, hoog, helling)
        # the knabbel pile Leguhlas and Gimguh are counting, with the score nailed to a post
        sx, sz = STAPEL
        for (dx, dz, n) in ((0, 0, 2), (1, 0, 1), (0, 1, 1), (1, 1, 3)):
            for i in range(n):
                self.set(sx + dx, G + 1 + i, sz + dz, KNABBELBLOK)
            self.vrij.add((sx + dx, sz + dz))
        self.set(sx + 3, G + 1, sz, GEBEITELD)
        self.set(sx + 3, G + 2, sz, GEBEITELD)
        self.bord(sx + 3, G + 2, sz + 1, "south", ["Knabbels geteld:", "Leguhlas 17", "Gimguh 17 en een half", "(dat halve telt niet)"])
        for n in ((sx + 3, sz), (sx + 3, sz + 1)):
            self.vrij.add(n)
        # a bench under the trees, and a sign at the great fall
        for z in (45, 46):
            self.plaat(13, G + 1, z, QPLAAT)
            self.vrij.add((13, z))
        self.set(26, G + 1, 18, GEBEITELD)
        self.set(26, G + 2, 18, GEBEITELD)
        self.bord(26, G + 2, 19, "south", ["Kaassaus van", "3000 jaar oud.", "NIET PROEVEN.", "Nou, één likje dan"])
        for n in ((26, 18), (26, 19)):
            self.vrij.add(n)

    def poort(self):
        """The gate: two tall pillars with a lamp on top, a pointed arch of copper over the path, low wing walls with
        flowers, the name of the house on the pillars."""
        for x in (28, 33):
            for z in (POORT_Z, POORT_Z + 1):
                self.set(x, G, z, BLOK)
                self.vrij.add((x, z))
            self.zuil(x, POORT_Z, G + 1, G + 6)
            self.set(x, G + 7, POORT_Z, GEBEITELD)
            self.set(x, G + 8, POORT_Z, LAMP)
            self.set(x, G + 9, POORT_Z, STAAF, {"facing": "up"})
            self.set(x, G + 1, POORT_Z + 1, GEBEITELD)
            self.lantaarn(x, G + 2, POORT_Z + 1)
        for x, facing, steun in ((29, "east", "west"), (32, "west", "east")):
            self.trap(x, G + 7, POORT_Z, facing, DTRAP)
            self.trap(x, G + 6, POORT_Z, steun, GTRAP, om=True)
        for x in (30, 31):
            self.trap(x, G + 7, POORT_Z, "west" if x == 30 else "east", GTRAP, om=True)
            self.set(x, G + 8, POORT_Z, DAK)
            self.plaat(x, G + 9, POORT_Z, DPLAAT)
        # the wings: a low wall that curves away on both sides, a flower on it
        for x, z in ((27, POORT_Z), (26, POORT_Z), (25, POORT_Z - 1), (34, POORT_Z), (35, POORT_Z), (36, POORT_Z - 1)):
            if self.vloer(x, z):
                self.set(x, G, z, BLOK)
                self.set(x, G + 1, z, STENEN)
                self.plaat(x, G + 2, z, GPLAAT)
                self.vrij.add((x, z))
        for x in (27, 34):
            self.set(x, G + 2, POORT_Z, GEBEITELD)
            self.set(x, G + 3, POORT_Z, "guhs:potted_knabbelroos")
        self.bord(28, G + 3, POORT_Z + 1, "south", ["~ Guhvendel ~", "Het Laatste", "Knusse Huis", "Pootjes vegen, njeg"])
        self.bord(33, G + 3, POORT_Z + 1, "south", ["Raad vandaag.", "Knabbels zelf", "meenemen. Ringen", "bij de bel melden"])

    def lantaarns(self):
        """Lantern stones along the paths, and nobody's seat grows over."""
        for (x, z) in ((26, 34), (35, 34), (26, 40), (35, 40), (26, 49), (35, 48), (22, 43), (26, 21), (35, 21)):
            if self.vloer(x, z) and (x, z) not in self.nat and self.get(x, G + 1, z) == AIR:
                self.set(x, G, z, GLAD)
                self.set(x, G + 1, z, GEBEITELD)
                self.lantaarn(x, G + 2, z)
                self.vrij.add((x, z))
        for b in BEWONERS:
            self.vrij.add((b[2], b[4]))

    def groen(self):
        """Mustard sprouts, worst mushrooms and flowers on the bare floor; flower beds along the hall and the ring."""
        for (x, z), top in sorted(self.top.items()):
            if (x, z) in self.vrij or self.get(x, top + 1, z) != AIR:
                continue
            if self.get(x, top, z) not in (NYLIUM, KAASMOS):
                continue
            k = _ruis(x, z, 31)
            if top == G:
                near = any((x + dx, z + dz) in self.vrij for dx in (-1, 0, 1) for dz in (-1, 0, 1))
                if k > (0.80 if near else 0.90):
                    self.set(x, top + 1, z, BLOEMEN[int(_ruis(x, z, 32) * 3) % 3])
                elif k < 0.10:
                    self.set(x, top + 1, z, SCHEUTJES)
            elif k < 0.16:
                self.set(x, top + 1, z, SCHEUTJES if k > 0.03 else ZWAMMETJE)

    def midden(self):
        """The centre jigsaw, in layer 0 (the start pool says where the ground is: ground_level_delta = G + 1)."""
        self.s.set(CX, 0, CZ, "minecraft:jigsaw", {"orientation": "up_north"},
                   {"id": "minecraft:jigsaw", "name": MIDDEN, "target": "minecraft:empty", "pool": "minecraft:empty",
                    "final_state": ROTS, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})

    def verbind(self):
        """Panes and bars get the connections their neighbours give them (a jigsaw piece is placed with the states exactly
        as they are in the template: nothing connects by itself)."""
        blocks = self.s.blocks

        def naam(c):
            b = blocks.get(c)
            return b[0] if b else None

        def vol(n):
            return n is not None and n not in DUN and n != SAUS and not any(t in n for t in ("_slab", "_plaat", "_stairs", "_trap", "kussen",
                                                                                              "leaves", "bell", "banner"))
        for (x, y, z), (name, props, nbt) in list(blocks.items()):
            if name not in (GLAS, TRALIES):
                continue
            p = {}
            for richting, (dx, dz) in (("north", (0, -1)), ("south", (0, 1)), ("east", (1, 0)), ("west", (-1, 0))):
                n = naam((x + dx, y, z + dz))
                p[richting] = "true" if (vol(n) or n in (GLAS, TRALIES) or (n is not None and "_stairs" in n)) else "false"
            p["waterlogged"] = "false"
            blocks[(x, y, z)] = (name, p, nbt)


def bouw(h, wezens=True):
    """Guhvendel's template (not saved) and the problems of its self-check."""
    b = Bouw(h)
    b.terrein()
    b.saus()
    b.paden()
    b.hal()
    b.toren()
    b.kring()
    b.paviljoen()
    b.bruggen()
    b.tuin()
    b.poort()
    b.lantaarns()
    b.groen()
    b.midden()
    b.verbind()
    if wezens:
        from features import ring
        for (id, kind, x, y, z, dy, yaw, van, tot) in BEWONERS:
            b.s.entity(x + 0.5, y + dy, z + 0.5, ring.cast(h, kind, id, "ring_h2", van, tot, plek=PLEK, yaw=yaw))
    return b.s, check(b.s)


# =====================================================================================================================
# checks
# =====================================================================================================================
def _vast(blocks):
    """The solid half blocks of the template: {(x, half y, z)} (half y = 2 * y, + 1 for the upper half)."""
    vol = set()
    for (x, y, z), b in blocks.items():
        n, p = b[0], b[1]
        if n in DUN or n == SAUS or "sign" in n or "banner" in n or n == "minecraft:jigsaw":
            continue
        if n.endswith("_slab"):
            if p.get("type") == "top":
                vol.add((x, 2 * y + 1, z))
            else:
                vol.add((x, 2 * y, z))
        elif n.endswith("_stairs"):
            vol.add((x, 2 * y + (1 if p.get("half") == "top" else 0), z))
        elif "kussen" in n or n == VUUR:
            vol.add((x, 2 * y, z))
        else:
            vol.update(((x, 2 * y, z), (x, 2 * y + 1, z)))
    return vol


def bereikbaar(blocks, start, hoog=4):
    """Every spot a player can walk to from `start` (x, y, z: feet on y): {(x, half y, z)} of the feet. A step is at most
    half a block up (a slab, a stair) or three blocks down, into a column with two blocks of room. Still sauce is waded
    through (it is one deep everywhere)."""
    vol = _vast(blocks)
    saus = {(x, 2 * y + 2, z) for (x, y, z), b in blocks.items() if b[0] == SAUS and b[1].get("level") == "0"}

    def staat(x, hy, z):
        return ((x, hy - 1, z) in vol or (x, hy, z) in saus) and all((x, hy + i, z) not in vol for i in range(hoog))

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


def check(s):
    problems = []
    blocks = s.blocks

    def nm(c):
        b = blocks.get(c)
        return b[0] if b else None

    def dicht(c):
        """Does this block hold sauce in? (Sauce only runs into air and plants: any other block, a slab too, stops it.)"""
        n = nm(c)
        return n is not None and n not in DUN and n != SAUS
    # 1. nothing floats: every block hangs together with the rock (leaves hang on their trunk, lanterns on their chain)
    zes = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
    schuin = tuple((dx, dy, dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) for dy in (1, -1))   # (a roof of stairs)
    solid = {c for c, b in blocks.items() if b[0] != AIR}
    seen = {c for c in solid if c[1] <= G}
    todo = list(seen)
    while todo:
        x, y, z = todo.pop()
        for d in zes + schuin:
            n = (x + d[0], y + d[1], z + d[2])
            if n in solid and n not in seen and (d in zes or blocks[n][0].endswith("_stairs") or blocks[(x, y, z)][0].endswith("_stairs")):
                seen.add(n)
                todo.append(n)
    for c in sorted(solid - seen)[:20]:
        problems.append(f"floating block {blocks[c][0]} at {c}")
    # 2. the sauce stays where it is: springs and still sauce are boxed in, a curtain hangs under sauce and ends in sauce
    for (x, y, z), b in blocks.items():
        if b[0] != SAUS:
            continue
        level = b[1].get("level")
        buren = [(x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1)]
        onder = (x, y - 1, z)
        if level == "0":
            for n in buren:
                if nm(n) != SAUS and not dicht(n):
                    # (the one open side of a spring: its lip)
                    problems.append(f"the sauce at {(x, y, z)} can run out at {n} ({nm(n)})")
            if nm(onder) != SAUS and not dicht(onder):
                problems.append(f"the sauce at {(x, y, z)} can run down at {onder} ({nm(onder)})")
        elif level == "8":
            if nm((x, y + 1, z)) != SAUS:
                problems.append(f"the curtain at {(x, y, z)} hangs under nothing")
            if nm(onder) != SAUS:
                problems.append(f"the curtain at {(x, y, z)} does not end in sauce")
        else:
            if nm(onder) != SAUS or not any(nm(n) == SAUS and blocks[n][1].get("level") == "0" for n in buren):
                problems.append(f"the lip at {(x, y, z)} has no spring behind it or no curtain under it")
    # (a spring's only open side is its lip: the check above reports the lip side as "sauce", so nothing to exempt)
    # 3. the things the story needs are there
    if nm(BEL) != KLOK:
        problems.append(f"no council bell at {BEL}")
    if nm(KRING) != GEBEITELD:
        problems.append(f"no stone table at {KRING}")
    if nm(RUSTVUUR) != VUUR:
        problems.append(f"no Rustvuurtje at {RUSTVUUR}")
    if not any(b[0] == "minecraft:jigsaw" and b[2] and b[2].get("name") == MIDDEN and c[1] == 0 for c, b in blocks.items()):
        problems.append("no centre jigsaw in layer 0")
    # 4. everybody has a place, and a player can walk to all of them from the gate
    loop = bereikbaar(blocks, INGANG)
    if not loop:
        problems.append(f"nobody can stand at the gate {INGANG}")

    def bij(x, y, z, ver=2):
        return any((x + dx, 2 * y + dh, z + dz) in loop for dx in range(-ver, ver + 1) for dz in range(-ver, ver + 1) for dh in (-2, -1, 0, 1, 2))
    for (id, kind, x, y, z, dy, yaw, van, tot) in BEWONERS:
        onder = nm((x, y - 1, z)) if dy == 0 else nm((x, y, z))
        if onder in (None, AIR) or onder in DUN:
            problems.append(f"{id} stands on nothing at {(x, y, z)}")
        if (dy == 0 and nm((x, y, z)) != AIR) or nm((x, y + 1, z)) != AIR:
            problems.append(f"{id} has no room at {(x, y, z)}: {nm((x, y, z))} / {nm((x, y + 1, z))}")
        if not bij(x, y, z):
            problems.append(f"{id} at {(x, y, z)} can't be walked to from the gate")
    for wat, (x, y, z) in (("the council bell", BEL), ("the stone table", KRING), ("the Rustvuurtje", RUSTVUUR), ("the hall", (16, HV + 1, 26)),
                           ("the tower", (TX, HV + 1, TZ)), ("the great pool", (27, G + 1, 19)), ("the pavilion", (41, G + 1, 44)),
                           ("the knabbel pile", (STAPEL[0], G + 1, STAPEL[1] + 2))):
        if not bij(x, y, z):
            problems.append(f"{wat} at {(x, y, z)} can't be walked to from the gate")
    # 5. a player in the council ring can't fall off the plate, and the bridge has room over it
    for z in BRUG_Z:
        for x in range(28, 34):
            for y in (G + 3, G + 4):
                if nm((x, y, z)) not in (AIR, None):
                    problems.append(f"the bridge is blocked at {(x, y, z)}")
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
    """The structure with everything in front of a plane left out (axis "x", "y" or "z"; keeps what is at or behind it)."""
    import make_structures as ms
    t = ms.Structure(s.size)
    i = {"x": 0, "y": 1, "z": 2}[as_]
    t.blocks = {c: b for c, b in s.blocks.items() if c[i] >= waarde and (tot is None or c[i] <= tot)}
    return t


def preview(out):
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    from PIL import Image
    kleur = {LANTAARN: (255, 214, 120), SCHEUTJES: (214, 170, 50), DAK: (79, 171, 147), DTRAP: (79, 171, 147), DPLAAT: (79, 171, 147),
             BORD: (226, 170, 180), VAANDEL: (120, 200, 60), GTRAP: (238, 232, 226), GPLAAT: (238, 232, 226), QPLAAT: (235, 229, 222),
             QTRAP: (235, 229, 222), VUUR: (240, 140, 40), KLOK: (253, 215, 60), STAAF: (250, 246, 236), KETTING: (70, 74, 86),
             LAMP: (240, 180, 220), UI: (255, 220, 130), "guhs:feestbuffettafel": (190, 140, 90), "guhs:theepotje": (230, 230, 240),
             "guhs:guh_taart": (250, 200, 210), "guhs:knabbelbak": (200, 150, 70), "guhs:potted_knabbelroos": (230, 120, 90),
             "guhs:lime_kussen": (130, 200, 70), "guhs:white_kussen": (240, 240, 240), "guhs:yellow_kussen": (240, 210, 70),
             "minecraft:lime_carpet": (130, 200, 70), "guhs:knabbelroos": (230, 120, 90), "guhs:roze_guhbloem": (240, 150, 200),
             TRALIES: (120, 120, 126), GLAD: (238, 232, 226), STENEN: (232, 226, 218), GEBEITELD: (228, 221, 212),
             BOEKEN: (150, 110, 70), VAT: (130, 96, 60), "guhs:kaasbloem": (240, 190, 40)}
    wr.SPECIAL_COLOURS.update(kleur)
    wr.block_colour.cache_clear()
    os.makedirs(out, exist_ok=True)
    s, problems = bouw(_H(), wezens=False)
    for p in problems:
        print("PROBLEM:", p)
    for k in range(4):
        wr.render_structure(gedraaid(s, k), {}, px=10, max_size=2000).save(os.path.join(out, f"{NAAM}_{k}.png"))
    wr.render_structure(doorsnede(gedraaid(s, 2), "z", 0, 36), {}, px=12, max_size=2400).save(os.path.join(out, f"{NAAM}_noord.png"))
    wr.render_structure(doorsnede(gedraaid(s, 1), "z", 14), {}, px=12, max_size=2400).save(os.path.join(out, f"{NAAM}_hal_open.png"))
    W, H, D = s.size
    plan = Image.new("RGB", (W * 14, D * 14), (20, 16, 18))
    for (x, y, z), b in sorted(s.blocks.items(), key=lambda t: t[0][1]):
        if b[0] in (AIR, "minecraft:jigsaw") or y < G or y > G + 3:
            continue
        c = wr.block_colour(b[0])
        if c:
            k = 1.0 if y == G else 0.85
            d = (y - G) * 2
            plan.paste(tuple(int(v * k) for v in c), (x * 14 + d, z * 14 + d, x * 14 + 14 - d, z * 14 + 14 - d))
    plan.save(os.path.join(out, f"{NAAM}_plan.png"))
    print(NAAM, "size", s.size, "blocks", sum(1 for b in s.blocks.values() if b[0] != AIR), "problems", len(problems))


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    preview(sys.argv[1] if len(sys.argv) > 1 else ".")
