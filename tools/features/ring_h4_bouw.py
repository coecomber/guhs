"""
bbq2 (ring-h4) - Caras Guhladhon, the tree city of Guhladriel in the golden wood Guhlórien, and the river Guhduin with the
Arguhnath (structure guhs:guhladriel_boomstad: ONE build of 192 x 52 x 96, saved as tiles of 32 x 32 like a guhs:burcht).

From west to east (x; north is -z):
  - the gate (x 0..12): two leaning saté trunks that meet in an arch, the path climbs four half steps into the glade;
  - the glade (a disc of 34 blocks around (48, 48), a rim of rock around it): pindasaus-nylium, white paths, and
      * de Grote Spies (48, 46): the great tree with a wide spiral stair around its trunk up to the hall of Guhladriel
        (a ring platform at y 25 under the crown, her seat on the west side);
      * the guest tree (30, 28) with the gastenvlonder (y 12: sleeping bags and a Rustvuurtje: the rest of the chapter);
      * two more flet trees (68, 28) and (68, 68), rope bridges hall -> north-east flet -> south-east flet -> a stair down;
      * the mirror dell (31, 67): a green hollow of moss and white flowers, a ring of water, the Spiegel van Guhladriel on
        its pedestal in the middle;
  - the harbour (x 84..94): a jetty with the elf boats on a round pool of sauce;
  - the Guhduin (x 90..172): a calm river of kaassaus through a gorge, and half-way, one on each bank, the two giant guh
    statues of the Arguhnath: sitting guh kings of old tuff with knabbel crowns, one paw raised: "halt, njeg";
  - the landing (180, 48): a little beach with a jetty, a Rustvuurtje and the signpost to the Zwarte Roosterpoort.

Template y 0 is the layer the cave floor is replaced by (the anchor's y): the sauce of the river lies in layer 1, its banks
are layers 1-2, the glade's ground is layer 4. Everything the build needs to be free is set to AIR (a dome over the glade, a
vault over the gorge): the placement only looks at the two ends, a copy may stand half in the rock (reports/slice_paleizen.md 5).

  bouw(h) -> Stad            the build (not saved), its PLEKKEN (every spot the Java side uses) and ROUTE (the boat's path)
  check(stad) -> problems    nothing important floats, every spot can be walked to, seams (paleizen_bouw.check_steun)
  save(h, stad)              the tiles guhs:guhladriel_boomstad/stuk_<i>_<j>
  preview(out)               pictures (python tools/features/ring_h4_bouw.py <out>, from the worktree root)
"""
import math
import os
import random
import sys

NAAM = "guhladriel_boomstad"
SIZE = (192, 52, 96)
TILE = 32
ANKER = (96, 0, 48)          # the middle of the build: the smallest reach (it stands on block 8 of its chunk)
SEED = 21301900

# --- the lie of the land ---------------------------------------------------------------------------------------------------
STAD = (48, 48)               # the middle of the glade
R_WEIDE = 34                  # the glade
R_RAND = 39                   # the rim of rock around it
R_KOEPEL = 43                 # the dome of air over it
GROND = 4                     # the top layer of the glade's ground (you walk in layer 5)
OEVER = 2                     # the top layer of the river banks
SAUS_Y = 1                    # the layer of the sauce
POORT_Z = 48
RIVIER = (90, 172)            # the river (x from .. to)
AANLEG = (180, 48)            # the landing
R_AANLEG = 11
BEELD_X = 131                 # the Arguhnath

GROOT = (48, 46)              # de Grote Spies
ZAAL_Y = 25                   # the hall's floor layer
GAST = (30, 28)               # the guest tree
GAST_Y = 12
NO = (68, 28)                 # the north-east flet tree
NO_Y = 21
ZO = (68, 68)                 # the south-east flet tree
ZO_Y = 15
DAL = (31, 67)                # the mirror dell
R_DAL = 9

# --- blocks ----------------------------------------------------------------------------------------------------------------
AIR = "minecraft:air"
ROTS = "guhs:houtskoolsteen"
STENEN = "guhs:houtskoolsteen_stenen"
GEBARSTEN = "guhs:gebarsten_houtskoolsteen_stenen"
NYLIUM = "guhs:pindasaus_nylium"
AS_AARDE = "guhs:as_aarde"
STAM = "guhs:sate_stam"
VLEES = "guhs:sate_vlees"
MOSTERD = "guhs:mosterd_blok"
UI = "guhs:uienlicht"
SAUS = "guhs:kaas_saus"
VUUR = "guhs:ring_rustvuur"
SPIEGEL = "guhs:ringh4_spiegel"
KNABBELBLOK = "guhs:block_of_kaasknabbels"
PLANK = "minecraft:birch_planks"
PLAAT = "minecraft:birch_slab"
HEK = "minecraft:birch_fence"
LUIK = "minecraft:birch_trapdoor"
KALK = "minecraft:calcite"
DIORIET = "minecraft:polished_diorite"
KWARTS = "minecraft:quartz_block"
KWARTS_GLAD = "minecraft:smooth_quartz"
KWARTS_PLAAT = "minecraft:smooth_quartz_slab"
KWARTS_PILAAR = "minecraft:quartz_pillar"
MOS = "minecraft:moss_block"
LANTAARN = "minecraft:soul_lantern"
KETTING = "minecraft:chain"
STAAF = "minecraft:end_rod"
GLOEI = "minecraft:shroomlight"
TUF = "minecraft:tuff"
TUF_STENEN = "minecraft:tuff_bricks"
TUF_GEBEITELD = "minecraft:chiseled_tuff_bricks"
TUF_GLAD = "minecraft:polished_tuff"
TUF_PLAAT = "minecraft:tuff_brick_slab"
OOG = "minecraft:polished_blackstone"
WATER = "minecraft:water"

BLAD = (VLEES, MOSTERD, GLOEI, UI)
NIET_VAST = (AIR, SAUS, WATER, LANTAARN, KETTING, STAAF, "minecraft:white_carpet", "minecraft:light_blue_carpet", "minecraft:moss_carpet",
             "minecraft:lily_of_the_valley", "minecraft:white_tulip", "minecraft:oxeye_daisy", "minecraft:azure_bluet", "guhs:pindascheutjes",
             "guhs:sate_zwammetje", "minecraft:birch_sign", "minecraft:white_banner", "minecraft:flowering_azalea", "minecraft:lime_carpet",
             "minecraft:green_carpet", "minecraft:yellow_carpet", "minecraft:cyan_carpet")


def _hek():
    return {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}


def _plaat(soort):
    return {"type": soort, "waterlogged": "false"}


def _stam(a, b):
    """The axis of a log that runs from a to b."""
    d = [abs(b[i] - a[i]) for i in range(3)]
    return {"axis": "xyz"[d.index(max(d))]}


def _ruis(x, z, schaal, seed):
    """Smooth value noise 0..1."""
    def r(i, j):
        n = (i * 374761393 + j * 668265263 + seed * 2147483647) & 0xFFFFFFFF
        n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
        return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0
    fx, fz = x / schaal, z / schaal
    i, j = math.floor(fx), math.floor(fz)
    u, v = fx - i, fz - j
    u, v = u * u * (3 - 2 * u), v * v * (3 - 2 * v)
    return (r(i, j) * (1 - u) + r(i + 1, j) * u) * (1 - v) + (r(i, j + 1) * (1 - u) + r(i + 1, j + 1) * u) * v


def rivier_z(x):
    """The middle of the Guhduin at this x: one and a half meander, straight (and furthest north) at the statues."""
    t = min(1.0, max(0.0, (x - RIVIER[0]) / (RIVIER[1] - RIVIER[0])))
    env = min(1.0, 4 * t, 4 * (1 - t))
    env = env * env * (3 - 2 * env)
    return 48 + 7 * math.sin(t * 3 * math.pi) * env


class Stad:
    def __init__(self, h):
        self.h = h
        self.naam = NAAM
        self.s = h.Structure(SIZE)
        self.rng = random.Random(SEED)
        self.plek = {"anker": ANKER}
        self.route = []
        self.moet_bereiken = {}
        self.grond = {}              # (x, z) -> the top layer of the ground there
        self.soort = {}              # (x, z) -> "weide" | "rand" | "saus" | "oever" | "wand" | "aanleg" | "poort" | "dal"
        self.openingen = set()       # cells of a flet's floor that stay open (a stair comes up there)
        self.treden_lijst = []       # (x, z, height you stand on) of every step of a stair or a bridge: the room above stays free

    # --- the basics ----------------------------------------------------------------------------------------------------------
    def set(self, x, y, z, name, props=None, nbt=None):
        self.s.set(x, y, z, name, props, nbt)

    def get(self, x, y, z):
        return self.s.get(int(x), int(y), int(z))

    def vrij(self, x, y, z):
        return self.get(x, y, z) in (None, AIR)

    def zet_als_vrij(self, x, y, z, name, props=None):
        if self.s.inside(x, y, z) and self.vrij(x, y, z):
            self.set(x, y, z, name, props)

    def lijn(self, a, b, name, props=None, dik=0):
        stappen = int(max(abs(b[i] - a[i]) for i in range(3)) * 2) + 1
        for i in range(stappen + 1):
            t = i / stappen
            p = [a[k] + (b[k] - a[k]) * t for k in range(3)]
            for dx in range(-dik, dik + 1):
                for dy in range(-dik, dik + 1):
                    for dz in range(-dik, dik + 1):
                        if abs(dx) + abs(dy) + abs(dz) <= max(1, dik):
                            self.set(round(p[0]) + dx, round(p[1]) + dy, round(p[2]) + dz, name, props)

    def bol(self, cx, cy, cz, rx, ry, rz, kies, seed=0, alleen_vrij=True):
        """An ellipsoid with a frayed edge; kies(x, y, z) -> (name, props)."""
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                    if d <= 1.0 + (_ruis(x + y * 3, z - y * 5, 3.0, seed) - 0.5) * 0.45:
                        if not alleen_vrij or self.vrij(x, y, z):
                            name, props = kies(x, y, z)
                            self.set(x, y, z, name, props)

    # --- 1. the land ------------------------------------------------------------------------------------------------------------
    def koepel(self, x, z):
        """The highest layer of free air over the glade at this column (None: not under the dome)."""
        d = math.hypot(x - STAD[0], z - STAD[1])
        if d > R_KOEPEL:
            return None
        return min(SIZE[1] - 1, int(13 + 39 * math.sqrt(1 - (d / (R_KOEPEL + 0.5)) ** 2)))

    def terrein(self):
        W, H, D = SIZE
        mid = [(x / 4.0, rivier_z(x / 4.0)) for x in range(RIVIER[0] * 4, RIVIER[1] * 4 + 1)]
        for x in range(W):
            for z in range(D):
                dc = math.hypot(x - STAD[0], z - STAD[1])
                da = math.hypot(x - AANLEG[0], z - AANLEG[1])
                # the distance to the river's middle line (only where it can matter)
                dr = None
                if RIVIER[0] - 12 <= x <= RIVIER[1] + 10:
                    dr = min(math.hypot(x - mx, z - mz) for mx, mz in mid if abs(mx - x) < 24)
                    # the round pools at both ends
                    dr = min(dr, math.hypot(x - (RIVIER[0] + 1), z - 48) - 3.5, math.hypot(x - (RIVIER[1] - 1), z - 48) - 2.5)
                n1, n2 = _ruis(x, z, 9.0, 11), _ruis(x, z, 4.0, 23)
                soort, top, lucht = None, None, None
                # the alcoves of the statues: the banks are wider there
                nis = max(0.0, 1 - abs(x - BEELD_X) / 9.0)
                bank = 7.0 + 8.5 * min(1.0, nis * 1.6)
                wand = bank + 4.5 + n1 * 3.5
                if dr is not None and dr <= 3.6:
                    soort, top = "saus", 0
                elif dr is not None and dr <= bank and not (dc <= R_WEIDE - 2 or da <= R_AANLEG - 2):
                    soort, top = "oever", OEVER
                elif dc <= R_WEIDE:
                    soort, top = "weide", GROND
                    # down to the harbour
                    if x >= 74 and abs(z - 48) <= 16:
                        top = max(OEVER, GROND - (x - 73) // 4)
                    elif _ruis(x, z, 7.0, 41) > 0.66 and not self.bij_pad(x, z, 3.6) and math.hypot(x - DAL[0], z - DAL[1]) > R_DAL + 2.5 and x < 72 and all(
                            math.hypot(x - bx, z - bz) > 8.5 for bx, bz in (GROOT, GAST, ZO)):
                        top = GROND + 1                            # low mounds between the paths
                elif da <= R_AANLEG - 2:
                    soort, top = "aanleg", OEVER
                    if x >= 184 and abs(z - AANLEG[1]) <= 4:
                        soort, top = "poort", max(0, OEVER - (x - 183) // 3)
                elif x <= 18 and abs(z - POORT_Z) <= 5 - (0 if x > 3 else 1):
                    soort, top = "poort", min(GROND, x // 3)      # (the way in: through the rim)
                elif dc <= R_RAND:
                    # the rim: rock that climbs away from the glade; open at the gate and the harbour
                    soort, top = "rand", int(GROND + 1 + (dc - R_WEIDE) * 0.7 + n1 * 3.5 + n2 * 1.5)
                elif x >= 184 and abs(z - AANLEG[1]) <= 4 and da <= R_AANLEG + 4:
                    soort, top = "poort", max(0, OEVER - (x - 183) // 3)
                elif da <= R_AANLEG + 2:
                    soort, top = "rand", int(OEVER + 1 + (da - R_AANLEG + 2) * 1.4 + n1 * 3)
                elif dr is not None and dr <= wand:
                    # the walls of the gorge: steep, terraced, higher towards the outside
                    soort, top = "wand", int(OEVER + 2 + (dr - bank) * 3.2 + n1 * 5 + n2 * 2)
                    top = min(top, 15 + int(n1 * 9))
                if soort is None:
                    continue
                # the dell of the mirror
                dd = math.hypot(x - DAL[0], z - DAL[1])
                if soort == "weide" and dd <= R_DAL:
                    soort = "dal"
                    top = 1 if dd <= 5.5 else 2 if dd <= 6.7 else 3 if dd <= 7.9 else 4
                self.soort[(x, z)], self.grond[(x, z)] = soort, top
                for y in range(0, top + 1):
                    self.set(x, y, z, ROTS)
                if soort == "saus":
                    self.set(x, SAUS_Y, z, SAUS, {"level": "0"})
                elif soort in ("weide", "aanleg", "oever", "poort"):
                    self.set(x, top, z, NYLIUM if n2 < 0.8 or soort == "weide" else AS_AARDE)
                elif soort == "dal":
                    self.set(x, top, z, MOS)
                elif soort in ("rand", "wand") and n2 > 0.72:
                    self.set(x, top, z, STENEN if n1 > 0.5 else GEBARSTEN)
                # free air above it
                k = self.koepel(x, z)
                if dr is not None and dr <= wand + 1:
                    gewelf = int(34 - (dr / (wand + 1)) ** 2 * 9)
                    k = gewelf if k is None else max(k, gewelf)
                if da <= R_AANLEG + 4:
                    boven = int(8 + 14 * math.sqrt(max(0.0, 1 - (da / (R_AANLEG + 4.5)) ** 2)))
                    k = boven if k is None else max(k, boven)
                if soort == "poort" and x <= 18:
                    k = max(k or 0, 10)
                if k is not None:
                    for y in range((SAUS_Y if soort == "saus" else top) + 1, min(H - 1, k) + 1):
                        self.set(x, y, z, AIR)

    PADEN = [([(0, 48), (14, 48), (26, 46), (38, 42), (41, 40)], 1.6),                  # gate -> the foot of the great stair
             ([(26, 46), (27, 55), (29, 58)], 1.3),                                    # -> the dell
             ([(38, 42), (36, 34), (35, 31)], 1.3),                                    # -> the guest tree
             ([(41, 40), (40, 50), (48, 57), (60, 56), (74, 49), (86, 48)], 1.5),      # around the tree -> the harbour
             ([(60, 56), (63, 61)], 1.3),                                              # -> the south-east stair
             ([(172, 48), (180, 48), (191, 48)], 1.4)]                                 # the landing -> on to the Roosterpoort

    def bij_pad(self, x, z, afstand):
        for punten, _ in self.PADEN:
            for a, b in zip(punten, punten[1:]):
                lx, lz = b[0] - a[0], b[1] - a[1]
                t = max(0.0, min(1.0, ((x - a[0]) * lx + (z - a[1]) * lz) / (lx * lx + lz * lz)))
                if math.hypot(x - a[0] - lx * t, z - a[1] - lz * t) <= afstand:
                    return True
        return False

    def paden(self):
        """White paths through the glade: from the gate to the great tree, the dell, the harbour, the flet trees."""
        def pad(punten, breed=1.3):
            for a, b in zip(punten, punten[1:]):
                n = int(math.dist(a, b) * 2) + 1
                for i in range(n + 1):
                    px, pz = a[0] + (b[0] - a[0]) * i / n, a[1] + (b[1] - a[1]) * i / n
                    for x in range(int(px - breed - 1), int(px + breed + 2)):
                        for z in range(int(pz - breed - 1), int(pz + breed + 2)):
                            if math.hypot(x - px, z - pz) <= breed and self.soort.get((x, z)) in ("weide", "poort", "aanleg", "oever"):
                                top = self.grond[(x, z)]
                                if self.get(x, top + 1, z) in (None, AIR):
                                    self.set(x, top, z, KALK if _ruis(x, z, 1.5, 5) < 0.7 else DIORIET)

        for punten, breed in self.PADEN:
            pad(punten, breed)

    def treden(self):
        """Half steps where the paths climb a whole block (the gate, the harbour, the landing, the dell)."""
        for (x, z), top in list(self.grond.items()):
            if self.soort[(x, z)] not in ("weide", "poort", "aanleg", "dal", "oever"):
                continue
            if not self.vrij(x, top + 1, z):
                continue
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                buur = self.grond.get((x + dx, z + dz))
                if buur == top + 1 and self.soort[(x + dx, z + dz)] in ("weide", "poort", "aanleg", "dal", "oever"):
                    op_pad = self.get(x, top, z) in (KALK, DIORIET) or self.soort[(x, z)] in ("dal", "poort")
                    if op_pad:
                        self.set(x, top + 1, z, KWARTS_PLAAT if self.soort[(x, z)] != "dal" else "minecraft:mossy_cobblestone_slab", _plaat("bottom"))
                    break

    # --- 2. trees ---------------------------------------------------------------------------------------------------------------
    def blad(self, seed):
        def kies(x, y, z):
            r = _ruis(x * 7 + y * 13, z * 5 - y * 11, 1.0, seed)
            m = _ruis(x + y, z - y, 3.5, seed + 1)
            if r > 0.975:
                return GLOEI, None
            if m > 0.66:
                return MOSTERD, None
            return VLEES, None
        return kies

    def boom(self, cx, cz, straal, top, kroon, seed, takken=5, wortels=True):
        """A saté tree: a pale trunk with a flared foot, branches that carry clumps of leaves, a crown on top.
        straal: of the trunk; top: the trunk's top layer; kroon = (radius, height)."""
        voet = self.grond.get((cx, cz), GROND)
        for y in range(1, top + 1):
            r = straal + max(0.0, (voet + 4 - y)) * 0.45 if wortels else straal
            if y > top - 4:
                r = max(0.9, straal - (y - (top - 4)) * 0.3)
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                for z in range(int(cz - r - 1), int(cz + r + 2)):
                    if math.hypot(x - cx, z - cz) <= r + 0.3 and (y > self.grond.get((x, z), -1) or True):
                        if y <= self.grond.get((x, z), 99) and math.hypot(x - cx, z - cz) > straal + 0.3:
                            continue
                        self.set(x, y, z, STAM, {"axis": "y"})
        if wortels:
            for i in range(6):
                a = i * math.pi / 3 + self.rng.uniform(-0.3, 0.3)
                lang = straal + 2.5 + self.rng.random() * 2
                ex, ez = cx + math.cos(a) * lang, cz + math.sin(a) * lang
                eind = (round(ex), self.grond.get((round(ex), round(ez)), voet) + 1, round(ez))
                begin = (round(cx + math.cos(a) * straal), voet + 3, round(cz + math.sin(a) * straal))
                if self.soort.get((eind[0], eind[2])) in ("weide", "rand"):
                    self.lijn(begin, eind, STAM, _stam(begin, eind))
        kr, kh = kroon
        blad = self.blad(seed)
        tips = []
        for i in range(takken):
            a = i * 2 * math.pi / takken + self.rng.uniform(-0.35, 0.35) + seed
            y0 = top - 3 - self.rng.randint(0, max(1, int(kh * 0.6)))
            lang = kr * self.rng.uniform(0.5, 0.75)
            begin = (round(cx + math.cos(a) * straal), y0, round(cz + math.sin(a) * straal))
            eind = (round(cx + math.cos(a) * lang), y0 + self.rng.randint(2, 4), round(cz + math.sin(a) * lang))
            self.lijn(begin, eind, STAM, _stam(begin, eind), dik=1 if straal >= 3.5 else 0)
            tips.append(eind)
        self.bol(cx, top + kh * 0.45, cz, kr, kh, kr, blad, seed)
        for ex, ey, ez in tips:
            self.bol(ex, ey + 1.5, ez, kr * 0.5, kh * 0.6, kr * 0.5, blad, seed + ex)
        return tips

    def spiesje(self, x, z, hoog, seed):
        """A young saté tree on the rim or a bank: a thin trunk, a small clump."""
        top = self.grond.get((x, z))
        if top is None or not self.vrij(x, top + 1, z):
            return
        for y in range(top + 1, top + hoog + 1):
            self.set(x, y, z, STAM, {"axis": "y"})
        self.bol(x, top + hoog + 1, z, 2.6, 2.2, 2.6, self.blad(seed), seed)

    def bomen(self):
        self.boom(GROOT[0], GROOT[1], 4.0, 37, (16, 10), 3, takken=7)
        self.boom(GAST[0], GAST[1], 2.3, 23, (9, 6), 5)
        self.boom(NO[0], NO[1], 2.3, 31, (9, 6), 7)
        self.boom(ZO[0], ZO[1], 2.3, 26, (9, 6), 9)
        self.boom(26, 48, 1.8, 17, (7, 5), 13, takken=4)                              # by the path from the gate
        self.boom(56, 70, 1.6, 15, (6, 4.5), 17, takken=4, wortels=False)
        # young trees on the rim and along the gorge
        rng = random.Random(SEED + 1)
        for _ in range(900):
            x, z = rng.randint(2, SIZE[0] - 3), rng.randint(2, SIZE[2] - 3)
            soort = self.soort.get((x, z))
            if soort == "rand" and rng.random() < 0.5 or soort == "wand" and rng.random() < 0.16 or soort == "oever" and rng.random() < 0.05:
                if abs(x - BEELD_X) < 12 and soort != "wand":
                    continue
                if all(self.get(x + dx, self.grond[(x, z)] + 2, z + dz) in (None, AIR) for dx in range(-2, 3) for dz in range(-2, 3)):
                    self.spiesje(x, z, rng.randint(3, 6), rng.randint(0, 99))

    # --- 3. flets, stairs, bridges ----------------------------------------------------------------------------------------------
    def wenteltrap(self, cx, cz, r_in, r_uit, van, tot, eind_hoek, stijging, richting=1, hek=True):
        """A spiral stair of half steps around a trunk. van / tot: the heights you stand on at the bottom and the top
        (a top layer + 1); eind_hoek: where it arrives (radians, 0 = east, pi/2 = south); stijging: blocks per radian."""
        totaal = (tot - van) / stijging
        cellen = {}
        for x in range(int(cx - r_uit - 2), int(cx + r_uit + 3)):
            for z in range(int(cz - r_uit - 2), int(cz + r_uit + 3)):
                d = math.hypot(x - cx, z - cz)
                if d <= r_in or d > r_uit + 1.0:
                    continue
                a = math.atan2(z - cz, x - cx)
                terug = ((eind_hoek - a) * richting) % (2 * math.pi)       # how far before the end, along the turn
                while terug <= totaal + 0.2:
                    hoogte = tot - terug * stijging
                    cellen.setdefault((x, z), []).append((hoogte, d > r_uit))
                    terug += 2 * math.pi
        for (x, z), lijst in cellen.items():
            for hoogte, rand in lijst:
                t = round(hoogte * 2) / 2.0
                t = max(van, min(tot, t))
                if rand:
                    if not hek:
                        continue
                    t = math.ceil(t)
                    if t - 1 <= self.grond.get((x, z), -9):
                        continue
                    self.set(x, t - 1, z, PLAAT, _plaat("top"))
                    self.set(x, t, z, HEK, _hek())
                    continue
                if (int(t) - 1 if t == int(t) else int(t)) <= self.grond.get((x, z), -9):
                    continue                                                # (the ground itself is the first step)
                if t == int(t):
                    self.set(x, int(t) - 1, z, PLAAT, _plaat("top"))
                else:
                    self.set(x, int(t), z, PLAAT, _plaat("bottom"))
                self.treden_lijst.append((x, z, t))
                if tot - hoogte < 3.6:
                    self.openingen.add((x, z))

    def vlonder(self, cx, cz, y, straal, r_stam, hek_open=()):
        """A flet: a round floor of planks around a trunk, a railing on its rim (open where hek_open(x, z) says so),
        beams under it, lamps under its rim."""
        for x in range(int(cx - straal - 1), int(cx + straal + 2)):
            for z in range(int(cz - straal - 1), int(cz + straal + 2)):
                d = math.hypot(x - cx, z - cz)
                if d > straal + 0.3 or d <= r_stam + 0.3 or (x, z) in self.openingen:
                    continue
                self.set(x, y, z, PLANK if (x + z) % 5 else "minecraft:stripped_birch_wood")
                if d > straal - 0.75:
                    if not any(f(x, z) for f in hek_open):
                        self.set(x, y + 1, z, HEK, _hek())
        for i in range(8):
            a = i * math.pi / 4 + 0.2
            begin = (round(cx + math.cos(a) * (r_stam + 0.5)), y - max(3, int(straal * 0.45)), round(cz + math.sin(a) * (r_stam + 0.5)))
            eind = (round(cx + math.cos(a) * (straal - 1.5)), y - 1, round(cz + math.sin(a) * (straal - 1.5)))
            stappen = int(max(abs(eind[k] - begin[k]) for k in range(3)) * 2) + 1
            for j in range(stappen + 1):
                p = [round(begin[k] + (eind[k] - begin[k]) * j / stappen) for k in range(3)]
                if (p[0], p[2]) not in self.openingen and self.vrij(*p):
                    self.set(*p, STAM, _stam(begin, eind))
            lx, lz = round(cx + math.cos(a + 0.39) * (straal - 0.6)), round(cz + math.sin(a + 0.39) * (straal - 0.6))
            if self.get(lx, y, lz) in (PLANK, "minecraft:stripped_birch_wood") and self.vrij(lx, y - 1, lz) and self.vrij(lx, y - 2, lz):
                self.set(lx, y - 1, lz, KETTING, {"axis": "y", "waterlogged": "false"})
                self.set(lx, y - 2, lz, LANTAARN, {"hanging": "true", "waterlogged": "false"})

    def brug(self, a, b, doorhang=1.0, breed=1):
        """A rope bridge from a to b ((x, height you stand on, z)): half steps that sag a little, a railing on both sides."""
        lengte = math.hypot(b[0] - a[0], b[2] - a[2])
        ux, uz = (b[0] - a[0]) / lengte, (b[2] - a[2]) / lengte
        n = int(lengte * 3) + 1
        dek = {}
        for i in range(n + 1):
            t = i / n
            hoogte = a[1] + (b[1] - a[1]) * t - doorhang * 4 * t * (1 - t)
            px, pz = a[0] + (b[0] - a[0]) * t, a[2] + (b[2] - a[2]) * t
            for k in range(-(breed + 1) * 2, (breed + 1) * 2 + 1):
                w = k / 2.0
                x, z = round(px - uz * w), round(pz + ux * w)
                oud = dek.get((x, z))
                if oud is None or abs(w) < abs(oud[1]):
                    dek[(x, z)] = (hoogte, w)
        for (x, z), (hoogte, w) in dek.items():
            t = round(hoogte * 2) / 2.0
            if abs(w) > breed + 0.25:
                t = math.ceil(t)
                if self.vrij(x, t - 1, z):
                    self.set(x, t - 1, z, PLAAT, _plaat("top"))
                if self.vrij(x, t, z):
                    self.set(x, t, z, HEK, _hek())
                continue
            if t == int(t):
                self.set(x, int(t) - 1, z, PLAAT, _plaat("top"))
            else:
                self.set(x, int(t), z, PLAAT, _plaat("bottom"))
            self.treden_lijst.append((x, z, t))

    def maak_vrij(self):
        """Three blocks of room over every step of the stairs and the bridges (roots, beams, leaves, railings in the way go)."""
        for x, z, t in self.treden_lijst:
            for y in range(int(math.ceil(t)), int(math.ceil(t)) + 3):
                if self.get(x, y, z) not in (None, AIR):
                    self.set(x, y, z, AIR)

    def stad(self):
        gx, gz = GROOT
        # the great stair first (it says where the hall's floor stays open), then the hall
        self.wenteltrap(gx, gz, 4.4, 6.5, GROND + 1, ZAAL_Y + 1, 0.0, 2.4)
        self.wenteltrap(GAST[0], GAST[1], 2.6, 4.5, GROND + 1, GAST_Y + 1, math.radians(300), 1.45)
        self.wenteltrap(ZO[0], ZO[1], 2.6, 4.5, GROND + 1, ZO_Y + 1, math.radians(250), 1.45)
        # the bridges' ends: on the rims of the flets
        def rand(c, straal, naar):
            d = math.hypot(naar[0] - c[0], naar[1] - c[1])
            return c[0] + (naar[0] - c[0]) / d * straal, c[1] + (naar[1] - c[1]) / d * straal
        a1, b1 = rand(GROOT, 10.2, NO), rand(NO, 6.2, GROOT)
        a2, b2 = rand(NO, 6.2, ZO), rand(ZO, 6.2, NO)

        def bij(p, r=2.4):
            return lambda x, z: math.hypot(x - p[0], z - p[1]) <= r
        self.vlonder(gx, gz, ZAAL_Y, 10.5, 4.0, hek_open=(bij(a1),))
        self.vlonder(GAST[0], GAST[1], GAST_Y, 6.5, 2.3)
        for c, yy in ((GAST, GAST_Y), (NO, NO_Y), (ZO, ZO_Y)):
            self.ruim_blad(c[0], c[1], 7.5, yy + 1, yy + 4)
        self.vlonder(NO[0], NO[1], NO_Y, 6.5, 2.3, hek_open=(bij(b1), bij(a2)))
        self.vlonder(ZO[0], ZO[1], ZO_Y, 6.5, 2.3, hek_open=(bij(b2),))
        self.brug((a1[0], ZAAL_Y + 1, a1[1]), (b1[0], NO_Y + 1, b1[1]), doorhang=0.5)
        self.brug((a2[0], NO_Y + 1, a2[1]), (b2[0], ZO_Y + 1, b2[1]), doorhang=1.5)
        self.maak_vrij()
        self.zaal()
        self.gastenvlonder()
        self.voorraadvlonder()
        self.uitkijk()

    def ruim_blad(self, cx, cz, straal, y0, y1):
        """No leaves in this cylinder (the room of a flet under a crown)."""
        for x in range(int(cx - straal - 1), int(cx + straal + 2)):
            for z in range(int(cz - straal - 1), int(cz + straal + 2)):
                if math.hypot(x - cx, z - cz) <= straal:
                    for y in range(y0, y1 + 1):
                        if self.get(x, y, z) in BLAD:
                            self.set(x, y, z, AIR)

    def zaal(self):
        """The hall of Guhladriel: a ring of slender quartz pillars, a canopy ring with lamps, her seat on the west side."""
        gx, gz = GROOT
        y = ZAAL_Y + 1
        self.ruim_blad(gx, gz, 12.0, y, y + 7)
        for i in range(12):
            a = i * math.pi / 6 + math.pi / 12
            x, z = round(gx + math.cos(a) * 8.6), round(gz + math.sin(a) * 8.6)
            if (x, z) in self.openingen or self.get(x, ZAAL_Y, z) is None or self.get(x, ZAAL_Y, z) == AIR:
                continue
            for yy in range(y, y + 5):
                self.set(x, yy, z, KWARTS_PILAAR, {"axis": "y"})
            self.set(x, y + 6, z, STAAF, {"facing": "up"})
        for x in range(gx - 11, gx + 12):
            for z in range(gz - 11, gz + 12):
                d = math.hypot(x - gx, z - gz)
                if 7.4 < d <= 9.9 and (x, z) not in self.openingen:
                    self.set(x, y + 5, z, KWARTS_PLAAT, _plaat("bottom"))
        for i in range(6):
            a = i * math.pi / 3
            x, z = round(gx + math.cos(a) * 6.6), round(gz + math.sin(a) * 6.6)
            if (x, z) not in self.openingen and self.vrij(x, y + 4, z):
                self.lijn((round(gx + math.cos(a) * 4.6), y + 5, round(gz + math.sin(a) * 4.6)), (x, y + 4, z), STAM, {"axis": "x"})
                self.set(x, y + 3, z, LANTAARN, {"hanging": "true", "waterlogged": "false"})
        # the seat: a dais of smooth quartz against the trunk, a high back, carpets towards the stair
        for dz in (-2, -1, 0, 1, 2):
            for dx in (-7, -6, -5):
                if math.hypot(dx, dz) > 4.3:
                    self.set(gx + dx, y, gz + dz, KWARTS_GLAD if abs(dz) < 2 or dx == -5 else KWARTS_PLAAT,
                             None if abs(dz) < 2 or dx == -5 else _plaat("bottom"))
        self.set(gx - 5, y + 1, gz, "minecraft:quartz_stairs", {"facing": "east", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        for dz in (-1, 1):
            for yy in (y + 1, y + 2):
                self.set(gx - 5, yy, gz + dz, KWARTS_PILAAR, {"axis": "y"})
            self.set(gx - 5, y + 3, gz + dz, STAAF, {"facing": "up"})
        for dx in (-9, -8):
            for dz in (-1, 0, 1):
                self.zet_als_vrij(gx + dx, y, gz + dz, "minecraft:light_blue_carpet" if dz == 0 else "minecraft:white_carpet")
        for dz in (-3, 3):
            self.set(gx - 8, y, gz + dz, "minecraft:white_banner", {"rotation": "4"}, {"id": "minecraft:banner"})
        self.plek["zaal"] = (gx - 8, y, gz)                       # where Guhladriel stands (she looks west, over the glade)
        self.plek["zaal_voor"] = (gx - 9, y, gz + 2)
        self.moet_bereiken["Guhladriel's hall"] = (gx - 9, y, gz + 2)

    def slaapzak(self, x, y, z, kleur, dx, dz):
        kijkt = {(0, 1): "south", (0, -1): "north", (1, 0): "east", (-1, 0): "west"}[(dx, dz)]
        self.set(x, y, z, f"guhs:{kleur}_kussen", {"facing": kijkt})
        for i in (1, 2):
            self.set(x + dx * i, y, z + dz * i, f"minecraft:{kleur}_carpet")

    def gastenvlonder(self):
        """The guest flet: a Rustvuurtje on a hearth of stone, sleeping bags, a low table with knabbels."""
        cx, cz = GAST
        y = GAST_Y + 1
        # (the stair comes up on the north-west side: everything stands on the south and east half)
        self.set(cx + 3, GAST_Y, cz + 3, STENEN)
        self.set(cx + 3, y, cz + 3, VUUR)
        self.plek["gast_vuur"] = (cx + 3, y, cz + 3)
        self.slaapzak(cx + 5, y, cz - 2, "white", 0, 1)
        self.slaapzak(cx - 1, y, cz + 5, "lime", 1, 0)
        self.slaapzak(cx - 4, y, cz + 3, "yellow", 1, 0)
        self.set(cx + 1, y, cz + 3, HEK, _hek())
        self.set(cx + 1, y + 1, cz + 3, "minecraft:white_carpet")
        self.set(cx + 3, y, cz + 1, KNABBELBLOK)
        self.set(cx + 3, y, cz + 5, "minecraft:white_banner", {"rotation": "0"}, {"id": "minecraft:banner"})
        self.moet_bereiken["the guest flet"] = (cx + 2, y, cz + 2)
        self.plek["gast"] = (cx + 2, y, cz + 2)

    def voorraadvlonder(self):
        """The north-east flet: the larder (blocks of knabbels, meat, a brewing stand of the elves)."""
        cx, cz = NO
        y = NO_Y + 1
        for dx, dz, blok in ((4, 2, KNABBELBLOK), (4, 3, KNABBELBLOK), (3, 4, VLEES), (4, 2, None), (-4, -3, MOSTERD), (-3, -4, KNABBELBLOK)):
            if blok and self.get(cx + dx, NO_Y, cz + dz) in (PLANK, "minecraft:stripped_birch_wood") and self.vrij(cx + dx, y, cz + dz):
                self.set(cx + dx, y, cz + dz, blok)
        self.set(cx + 4, y + 1, cz + 2, KNABBELBLOK)
        self.set(cx - 4, y, cz + 3, HEK, _hek())
        self.set(cx - 4, y + 1, cz + 3, LANTAARN, {"hanging": "false", "waterlogged": "false"})
        self.moet_bereiken["the larder flet"] = (cx - 3, y, cz + 1)

    def uitkijk(self):
        """The south-east flet: a lookout over the harbour with two benches and a telescope of the elves."""
        cx, cz = ZO
        y = ZO_Y + 1
        for dz in (-1, 0, 1):
            self.set(cx + 4, y, cz + dz, "minecraft:birch_stairs", {"facing": "east", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        self.set(cx + 3, y, cz + 4, HEK, _hek())
        self.set(cx + 3, y + 1, cz + 4, STAAF, {"facing": "up"})
        self.moet_bereiken["the lookout flet"] = (cx + 3, y, cz - 3)

    # --- 4. the mirror -----------------------------------------------------------------------------------------------------------
    def spiegeldal(self):
        cx, cz = DAL
        # a ring of water around the island with the pedestal
        for x in range(cx - 5, cx + 6):
            for z in range(cz - 5, cz + 6):
                d = math.hypot(x - cx, z - cz)
                if 1.6 < d <= 3.3 and not (abs(z - cz) <= 0 and x > cx):       # (a causeway from the east)
                    self.set(x, 1, z, WATER, {"level": "0"})
                    self.set(x, 0, z, "minecraft:mossy_cobblestone")
                elif d <= 1.6:
                    self.set(x, 1, z, KWARTS_GLAD)
        for dx in (2, 3):
            self.set(cx + dx, 1, cz, KWARTS_GLAD)
        self.set(cx, 2, cz, KWARTS_PILAAR, {"axis": "y"})
        self.set(cx, 3, cz, SPIEGEL)
        self.plek["spiegel"] = (cx, 3, cz)
        self.moet_bereiken["the mirror"] = (cx + 1, 2, cz)
        # a spring: a rock on the west side with water in a bowl
        for (dx, dy, dz) in ((-5, 2, 0), (-5, 2, -1), (-5, 2, 1), (-6, 2, 0), (-5, 3, 0), (-6, 3, 0), (-6, 3, -1), (-6, 4, 0), (-5, 3, 1)):
            self.set(cx + dx, dy, cz + dz, "minecraft:mossy_cobblestone")
        self.set(cx - 4, 2, cz, "minecraft:mossy_cobblestone_slab", _plaat("bottom"))
        # white flowers, moss, two azaleas, the lamps of the dell
        rng = random.Random(SEED + 2)
        for x in range(cx - R_DAL, cx + R_DAL + 1):
            for z in range(cz - R_DAL, cz + R_DAL + 1):
                if self.soort.get((x, z)) != "dal":
                    continue
                top = self.grond[(x, z)]
                if self.get(x, top, z) != MOS or not self.vrij(x, top + 1, z):
                    continue
                r = rng.random()
                if r < 0.16:
                    self.set(x, top + 1, z, rng.choice(["minecraft:lily_of_the_valley", "minecraft:white_tulip", "minecraft:oxeye_daisy", "minecraft:azure_bluet"]))
                elif r < 0.24:
                    self.set(x, top + 1, z, "minecraft:moss_carpet")
        for a in (0.9, 2.5, 3.9, 5.4):
            x, z = round(cx + math.cos(a) * 7.2), round(cz + math.sin(a) * 7.2)
            top = self.grond.get((x, z))
            if top is not None and self.vrij(x, top + 1, z):
                self.set(x, top + 1, z, HEK, _hek())
                self.set(x, top + 2, z, LANTAARN, {"hanging": "false", "waterlogged": "false"})
        self.set(cx - 6, 5, cz + 5, "minecraft:flowering_azalea")
        self.set(cx + 5, 5, cz - 6, "minecraft:flowering_azalea")
        for p in ((cx - 6, 4, cz + 5), (cx + 5, 4, cz - 6)):
            self.set(*p, MOS)
        self.plek["dal"] = (cx + 3, 2, cz + 2)                    # where Guhladriel stands after the mirror
        self.plek["dal_rand"] = (cx + 6, 3, cz - 4)

    # --- 5. the gate, the harbour, the landing ----------------------------------------------------------------------------------
    def poort(self):
        """Two leaning saté trunks that meet in an arch over the path, a clump of leaves on top, lamps and banners."""
        x = 12
        for kant in (-1, 1):
            voet = (x, 1, POORT_Z + kant * 5)
            knie = (x, 9, POORT_Z + kant * 4)
            self.lijn(voet, knie, STAM, {"axis": "y"}, dik=1)
            self.lijn(knie, (x, 12, POORT_Z + kant * 1), STAM, {"axis": "z"}, dik=1)
            self.set(x - 1, 8, POORT_Z + kant * 3, KETTING, {"axis": "y", "waterlogged": "false"})
            self.set(x - 1, 7, POORT_Z + kant * 3, LANTAARN, {"hanging": "true", "waterlogged": "false"})
        self.lijn((x, 12, POORT_Z - 1), (x, 12, POORT_Z + 1), STAM, {"axis": "z"}, dik=1)
        self.bol(x, 15, POORT_Z, 6, 3.5, 8, self.blad(31), 31)
        # the dome stops at the gate: carve the way in
        self.bord(6, POORT_Z + 4, 12, ["Caras Guhladhon", "de Boomstad van", "Vrouwe Guhladriel", "Pootjes vegen, njeg"])
        self.plek["poort"] = (4, self.grond[(4, POORT_Z)] + 1, POORT_Z)
        self.plek["leguhlas_poort"] = (17, GROND + 1, POORT_Z - 3)
        self.moet_bereiken["Leguhlas at the gate"] = (17, GROND + 1, POORT_Z - 2)

    def bord(self, x, z, rotatie, regels, y=None):
        import sign_text
        B = self.h.Byte
        top = self.grond.get((x, z), 0) if y is None else y - 1
        leeg = sign_text.messages("sign.guhs.ringh4", ["", "", "", ""])
        self.set(x, top + 1, z, "minecraft:birch_sign", {"rotation": str(rotatie), "waterlogged": "false"}, {
            "id": "minecraft:sign", "is_waxed": B(1),
            "front_text": {"messages": sign_text.messages("sign.guhs.ringh4", regels), "color": "black", "has_glowing_text": B(0)},
            "back_text": {"messages": leeg, "color": "black", "has_glowing_text": B(0)}})

    def steiger(self, x0, x1, z, y=OEVER + 1):
        """A jetty of planks (layer y - 1 ... you stand on y) on posts in the sauce, lamps at its end."""
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for dz in (-1, 0, 1):
                self.set(x, y - 1, z + dz, PLANK)
                if self.get(x, SAUS_Y, z + dz) == SAUS and abs(dz) == 1 and (x - x0) % 3 == 0:
                    self.set(x, SAUS_Y, z + dz, STAM, {"axis": "y"})
        eind = x1
        for dz in (-1, 1):
            self.set(eind, y, z + dz, HEK, _hek())
            self.set(eind, y + 1, z + dz, LANTAARN, {"hanging": "false", "waterlogged": "false"})

    def haven(self):
        """The harbour of the glade: the jetty into the round pool, the boats along it, Leguhlas and Gimguh on the quay."""
        self.steiger(83, 92, 48)
        self.plek["steiger"] = (86, OEVER + 1, 48)
        self.plek["leguhlas_steiger"] = (82, OEVER + 1, 51)
        self.plek["gimguh_steiger"] = (82, OEVER + 1, 45)
        self.plek["boot"] = (93.5, SAUS_Y + 0.72, 50.6)           # the guide boat: moored at the end of the jetty, nose east
        self.plek["boot_deco_1"] = (89.5, SAUS_Y + 0.72, 45.0)
        self.plek["boot_deco_2"] = (89.5, SAUS_Y + 0.72, 51.2)
        self.bord(81, 53, 4, ["Elfenbootjes", "naar de overkant", "Niet wiebelen!", "~ Leguhlas"])
        self.moet_bereiken["the jetty"] = (91, OEVER + 1, 48)

    def aanleg(self):
        """The landing: a short jetty, a camp (the Rustvuurtje, a log, two sleeping bags), the signpost east."""
        ax, az = AANLEG
        self.steiger(175, 171, 48)
        self.set(ax + 1, OEVER + 1, az - 5, VUUR)
        self.plek["aanleg_vuur"] = (ax + 1, OEVER + 1, az - 5)
        for i in range(3):
            self.set(ax - 1 + i, OEVER + 1, az - 8, STAM, {"axis": "x"})
        self.slaapzak(ax + 4, OEVER + 1, az - 6, "cyan", 0, 1)
        self.slaapzak(ax - 3, OEVER + 1, az - 6, "lime", 0, 1)
        self.set(ax + 5, OEVER + 1, az - 3, HEK, _hek())
        self.set(ax + 5, OEVER + 2, az - 3, LANTAARN, {"hanging": "false", "waterlogged": "false"})
        self.bord(ax + 3, az + 3, 12, ["Zwarte Roosterpoort", "die kant op -->", "Rots in de weg?", "Hakken maar, njeg"])
        self.bord(ax - 3, az + 3, 4, ["Terug naar de", "Boomstad? Stap in", "het bootje aan", "de steiger"])
        self.plek["aanleg"] = (ax - 2, OEVER + 1, az)             # where the boat puts you ashore
        self.plek["boot_terug"] = (170.5, SAUS_Y + 0.72, 50.6)    # the boat back, nose west
        self.plek["uitgang"] = (190, self.grond[(190, az)] + 1, az)
        self.moet_bereiken["the landing"] = (ax - 2, OEVER + 1, az)
        self.moet_bereiken["the way on"] = (190, self.grond[(190, az)] + 1, az)

    def vaarroute(self):
        """The boat's path: from the end of the jetty down the middle of the Guhduin to the landing's jetty."""
        punten = [(93.5, 50.6), (96.0, 49.6)]
        x = 99.0
        while x <= 164.0:
            punten.append((x, rivier_z(x)))
            x += 3.0
        punten += [(167.0, 49.2), (170.5, 50.6)]
        self.route = [(round(px, 2), SAUS_Y + 0.72, round(pz, 2)) for px, pz in punten]
        for px, _, pz in self.route:
            if self.get(round(px - 0.5), SAUS_Y, round(pz - 0.5)) != SAUS:
                raise SystemExit(f"ring_h4: the boat's path leaves the sauce at {(px, pz)}")

    # --- 6. the Arguhnath -----------------------------------------------------------------------------------------------------
    def beeld(self, x0, z0, arm_z):
        """A giant sitting guh king of old tuff that looks west (upstream), the paw on the river's side raised: halt!
        (x0, z0): the middle of its plinth; arm_z: +1 / -1, the side of the raised paw."""
        rng = random.Random(SEED + z0)
        y0 = OEVER + 1

        def steen(x, y, z, soort=None):
            r = rng.random()
            self.set(x, y, z, soort or (TUF_STENEN if r < 0.72 else TUF if r < 0.9 else TUF_GLAD))

        def doos(xa, xb, ya, yb, za, zb, rond=1.5, soort=None):
            mx, my, mz = (xa + xb) / 2.0, (ya + yb) / 2.0, (za + zb) / 2.0
            for x in range(xa, xb + 1):
                for y in range(ya, yb + 1):
                    for z in range(za, zb + 1):
                        # rounded: cut the corners where three or two faces meet
                        ex = max(0.0, abs(x - mx) - ((xb - xa) / 2.0 - rond))
                        ey = max(0.0, abs(y - my) - ((yb - ya) / 2.0 - rond))
                        ez = max(0.0, abs(z - mz) - ((zb - za) / 2.0 - rond))
                        if math.sqrt(ex * ex + ey * ey + ez * ez) <= rond + 0.25:
                            steen(x0 + x, y0 + y, z0 + z, soort)

        # the plinth: three layers, a chiseled band
        for y in range(0, 3):
            for x in range(-7, 8):
                for z in range(-8, 9):
                    rand = y == 1 and (abs(x) == 7 or abs(z) == 8)
                    if y < 2 or (abs(x) <= 6 and abs(z) <= 7):
                        steen(x0 + x, y0 + y, z0 + z, TUF_GEBEITELD if rand else None)
        doos(-5, 5, 3, 9, -6, 6, rond=2.6)                        # the belly: round and vads
        doos(-4, 4, 9, 13, -5, 5, rond=2.0)                       # the chest, a little narrower
        doos(-6, 5, 13, 22, -7, 7, rond=3.0)                      # the head: bigger than the body, as a guh's is
        doos(-8, -5, 3, 4, -5, -2, rond=0.8)                      # two little hind paws that stick out in front
        doos(-8, -5, 3, 4, 2, 5, rond=0.8)

        def voor(y, z):
            """The front (west) block of the statue at this height and z, as an x relative to x0 (None: nothing there)."""
            for x in range(-9, 6):
                if self.get(x0 + x, y0 + y, z0 + z) not in (None, AIR):
                    return x
            return None

        for y in range(5, 9):                                     # a smooth belly
            for z in range(-3, 4):
                if abs(z) + abs(y - 6.5) <= 4 and voor(y, z) is not None:
                    self.set(x0 + voor(y, z), y0 + y, z0 + z, TUF_GLAD)
        for kant in (-1, 1):                                      # big round ears on the corners of the head
            for y in range(19, 27):
                for z in range(4, 12):
                    d = math.hypot(y - 22.5, z - 7.5)
                    if d <= 3.3:
                        for x in (-1, 0, 1):
                            if x == -1 and d <= 1.9:
                                self.set(x0 + x, y0 + y, z0 + kant * z, TUF_GLAD)   # the inside of the ear
                            else:
                                steen(x0 + x, y0 + y, z0 + kant * z)
        # the face: two big eyes with a glint, a muzzle with a nose
        for kant in (-1, 1):
            for y in (17, 18, 19):
                for z in (3, 4, 5):
                    x = voor(y, kant * z)
                    if x is not None:
                        self.set(x0 + x, y0 + y, z0 + kant * z, KWARTS if (y, z) == (19, 3) else OOG)
        for y in (14, 15, 16):
            for z in (-2, -1, 0, 1, 2):
                if (y, abs(z)) != (16, 2):
                    self.set(x0 - 7, y0 + y, z0 + z, TUF_GLAD)
        self.set(x0 - 8, y0 + 16, z0, OOG)                        # the nose
        self.set(x0 - 7, y0 + 14, z0, TUF)
        # the paws: one rests on the belly, the other is raised with its flat side to whoever comes down the river
        rust = -arm_z
        doos(-7, -5, 8, 10, min(rust * 3, rust * 5), max(rust * 3, rust * 5), rond=0.8)
        for x in range(-8, -3):                                   # the upper arm, forward from the shoulder
            for y in (10, 11, 12):
                for z in (6, 7):
                    steen(x0 + x, y0 + y, z0 + arm_z * z)
        for x in (-9, -8):                                        # the forearm, up
            for y in range(12, 17):
                for z in (6, 7):
                    steen(x0 + x, y0 + y, z0 + arm_z * z)
        for y in range(14, 20):                                   # the paw: flat, toes up
            for z in range(4, 10):
                if (y, z) not in ((14, 4), (14, 9), (19, 4), (19, 9)) and not (y == 19 and z in (6,)):
                    steen(x0 - 10, y0 + y, z0 + arm_z * z, TUF_GLAD if y in (15, 16) and z in (6, 7) else None)
        # the knabbel crown, between the ears
        for x in range(-4, 4):
            for z in range(-3, 4):
                if max(abs(x + 0.5) - 0.5, abs(z)) >= 3:
                    self.set(x0 + x, y0 + 23, z0 + z, KNABBELBLOK)
                    if (x in (-4, 3) and abs(z) in (0, 3)) or (abs(z) == 3 and x in (-1, 0)):
                        self.set(x0 + x, y0 + 24, z0 + z, KNABBELBLOK)
        # fire bowls at the plinth's west corners
        for kant in (-1, 1):
            self.set(x0 - 7, y0 + 3, z0 + kant * 8, TUF_GEBEITELD)
            self.set(x0 - 7, y0 + 4, z0 + kant * 8, GLOEI)

    def beelden(self):
        zm = rivier_z(BEELD_X)
        self.plek["beeld_noord"] = (BEELD_X, OEVER + 1, round(zm - 12.5))
        self.plek["beeld_zuid"] = (BEELD_X, OEVER + 1, round(zm + 12.5))
        self.beeld(BEELD_X, round(zm - 12.5), +1)
        self.beeld(BEELD_X, round(zm + 12.5), -1)

    def tuinen(self):
        """What makes the glade a garden: beds of moss with white flowers around a lamp of the elves, boulders, log benches
        by the paths, young saté trees."""
        rng = random.Random(SEED + 5)

        def vrij_gras(x, z):
            top = self.grond.get((x, z))
            return (self.soort.get((x, z)) == "weide" and self.get(x, top, z) == NYLIUM and self.vrij(x, top + 1, z) and self.vrij(x, top + 2, z))

        bloemen = ["minecraft:lily_of_the_valley", "minecraft:white_tulip", "minecraft:oxeye_daisy", "minecraft:azure_bluet"]
        for cx, cz, r in ((38, 59, 3.2), (57, 37, 3.0), (62, 47, 2.6), (21, 39, 3.0), (42, 25, 2.8), (53, 65, 2.6), (73, 41, 2.4), (20, 58, 2.4),
                          (45, 70, 2.6), (58, 22, 2.6)):
            if not vrij_gras(cx, cz):
                continue
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                for z in range(int(cz - r - 1), int(cz + r + 2)):
                    if math.hypot(x - cx, z - cz) <= r + (_ruis(x, z, 2.0, 7) - 0.5) and vrij_gras(x, z):
                        top = self.grond[(x, z)]
                        self.set(x, top, z, MOS)
                        k = rng.random()
                        if (x, z) != (cx, cz) and k < 0.34:
                            self.set(x, top + 1, z, rng.choice(bloemen))
                        elif (x, z) != (cx, cz) and k < 0.42:
                            self.set(x, top + 1, z, "minecraft:moss_carpet")
            top = self.grond[(cx, cz)]
            self.set(cx, top, cz, MOS)
            if rng.random() < 0.6:
                self.set(cx, top + 1, cz, KWARTS_PILAAR, {"axis": "y"})
                self.set(cx, top + 2, cz, STAAF, {"facing": "up"})
            else:
                self.set(cx, top + 1, cz, "minecraft:flowering_azalea")
        # boulders
        for _ in range(60):
            x, z = rng.randint(12, 84), rng.randint(12, 84)
            if vrij_gras(x, z) and not self.bij_pad(x, z, 2.5):
                top = self.grond[(x, z)]
                self.set(x, top + 1, z, rng.choice([STENEN, GEBARSTEN, ROTS]))
                if rng.random() < 0.5 and vrij_gras(x + 1, z):
                    self.set(x + 1, self.grond[(x + 1, z)] + 1, z, "guhs:houtskoolsteen_stenen_plaat", _plaat("bottom"))
        # log benches beside the paths
        for x, z, as_ in ((22, 51, "x"), (33, 40, "z"), (52, 59, "x"), (67, 50, "x"), (44, 36, "x")):
            for i in range(3):
                bx, bz = (x + i, z) if as_ == "x" else (x, z + i)
                if vrij_gras(bx, bz):
                    self.set(bx, self.grond[(bx, bz)] + 1, bz, STAM, {"axis": as_})
        # young trees
        for _ in range(200):
            x, z = rng.randint(12, 84), rng.randint(12, 84)
            if vrij_gras(x, z) and not self.bij_pad(x, z, 3.0) and math.hypot(x - DAL[0], z - DAL[1]) > R_DAL + 5 and all(
                    self.get(x + dx, self.grond[(x, z)] + dy, z + dz) in (None, AIR) for dx in range(-3, 4) for dz in range(-3, 4) for dy in range(2, 9)):
                self.spiesje(x, z, rng.randint(3, 5), rng.randint(0, 99))

    # --- 7. light and life --------------------------------------------------------------------------------------------------------
    def licht(self):
        """Lamp posts along the paths and the banks, hanging lamps in the crowns."""
        rng = random.Random(SEED + 3)
        geplaatst = []

        def paal(x, z, hoog=2):
            top = self.grond.get((x, z))
            if top is None or not all(self.vrij(x, top + i, z) for i in range(1, hoog + 3)):
                return False
            if self.get(x, top, z) in (KALK, DIORIET, SAUS, WATER, None) or any(math.hypot(x - px, z - pz) < 6 for px, pz in geplaatst):
                return False
            for i in range(1, hoog + 1):
                self.set(x, top + i, z, HEK, _hek())
            self.set(x, top + hoog + 1, z, LANTAARN, {"hanging": "false", "waterlogged": "false"})
            geplaatst.append((x, z))
            return True

        # next to the paths
        for (x, z), top in sorted(self.grond.items()):
            if self.get(x, top, z) in (KALK, DIORIET):
                for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2)):
                    if self.get(x + dx, self.grond.get((x + dx, z + dz), 0), z + dz) == NYLIUM and rng.random() < 0.25:
                        paal(x + dx, z + dz)
        # along the banks
        for x in range(RIVIER[0] + 6, RIVIER[1] - 2, 7):
            for kant in (-1, 1):
                z = round(rivier_z(x) + kant * 5.4)
                if self.soort.get((x, z)) == "oever":
                    paal(x, z, 1)
        # in the crowns: lamps on chains under the leaves, over the glade
        for _ in range(500):
            x, z = rng.randint(8, 88), rng.randint(8, 88)
            if self.soort.get((x, z)) not in ("weide", "dal") or any(math.hypot(x - px, z - pz) < 7 for px, pz in geplaatst):
                continue
            for y in range(SIZE[1] - 2, 12, -1):
                if self.get(x, y, z) in BLAD and self.vrij(x, y - 1, z):
                    lang = rng.randint(1, 3)
                    if all(self.vrij(x, y - i, z) for i in range(1, lang + 3)) and y - lang - 1 > self.grond[(x, z)] + 6:
                        for i in range(1, lang + 1):
                            self.set(x, y - i, z, KETTING, {"axis": "y", "waterlogged": "false"})
                        self.set(x, y - lang - 1, z, LANTAARN, {"hanging": "true", "waterlogged": "false"})
                        geplaatst.append((x, z))
                    break

    def planten(self):
        rng = random.Random(SEED + 4)
        for (x, z), top in sorted(self.grond.items()):
            if self.get(x, top, z) == NYLIUM and self.vrij(x, top + 1, z):
                r = rng.random()
                if r < 0.07:
                    self.set(x, top + 1, z, "guhs:pindascheutjes")
                elif r < 0.085:
                    self.set(x, top + 1, z, "guhs:sate_zwammetje")

    def bedekt(self):
        """Nylium under a solid block turns to rock in the game (like grass under a block): make it rock here, so a copy is
        exactly its template (/guhs bouwcheck ... compleet)."""
        for (x, y, z), (name, props, _) in list(self.s.blocks.items()):
            if name != NYLIUM:
                continue
            boven = self.s.blocks.get((x, y + 1, z))
            if boven is None:
                continue
            b = boven[0]
            if _top(b, boven[1]) == 1.0 and not b.endswith(("_slab", "_stairs", "_plaat", "_trapdoor")) and b not in (VUUR, SPIEGEL, HEK):
                self.set(x, y, z, ROTS)

    # --- 8. who lives here ------------------------------------------------------------------------------------------------------
    def bewoners(self):
        from features import ring
        h = self.h

        def cast(kind, id, plek, van, tot, rol, yaw):
            x, y, z = self.plek[plek]
            self.s.entity(x + 0.5, y, z + 0.5, ring.cast(h, kind, id, "ring_h4", van, tot, plek=rol, yaw=yaw))

        cast("leguhlas", "ringh4_leguhlas_poort", "leguhlas_poort", 0, 1, "boomstad_poort", 90.0)
        cast("guhladriel", "ringh4_guhladriel_zaal", "zaal", 0, 3, "boomstad", 90.0)
        cast("guhladriel", "ringh4_guhladriel_dal", "dal", 4, 5, "boomstad", 135.0)
        cast("guhladriel", "ringh4_guhladriel_thuis", "zaal", 6, 99, "boomstad", 90.0)
        cast("leguhlas", "ringh4_leguhlas_steiger", "leguhlas_steiger", 2, 99, "boomstad_steiger", 90.0)
        cast("gimguh", "ringh4_gimguh_steiger", "gimguh_steiger", 2, 99, "boomstad_steiger", 90.0)
        for id, plek, soort, yaw in (("ringh4_boot", "boot", 0, -90.0), ("ringh4_boot_deco_1", "boot_deco_1", 2, -90.0),
                                     ("ringh4_boot_deco_2", "boot_deco_2", 2, -90.0), ("ringh4_boot_terug", "boot_terug", 1, 90.0)):
            x, y, z = self.plek[plek]
            self.s.entity(x, y, z, {"id": "guhs:ringh4_elfenbootje", "Soort": soort, "Invulnerable": h.Byte(1), "NoGravity": h.Byte(1),
                                    "Rotation": h.floats(yaw, 0.0), "NeoForgeData": {"guhs_bezetting": id}})


def bouw(h):
    """The whole build (not saved)."""
    s = Stad(h)
    s.terrein()
    s.paden()
    s.bomen()
    s.stad()
    s.spiegeldal()
    s.poort()
    s.haven()
    s.aanleg()
    s.vaarroute()
    s.beelden()
    s.tuinen()
    s.treden()
    s.licht()
    s.planten()
    s.bedekt()
    s.bewoners()
    return s


# =====================================================================================================================
# checks
# =====================================================================================================================
def _top(name, props):
    """The height you stand on in a block of this kind (relative to its layer), or None when you walk through it; 9 = a wall."""
    if name is None or name in NIET_VAST or name.endswith(("_sign", "_banner", "_carpet")):
        return None
    if name.endswith("_slab") or name == PLAAT:
        return 0.5 if props.get("type") == "bottom" else 1.0
    if name.endswith(("_fence", "_wall")) or name == HEK:
        return 9
    return 1.0


def loopbaar(stad):
    """Every (x, z, height in half blocks) a player can stand on: something to stand on and two blocks of room."""
    blocks = stad.s.blocks
    staan = {}
    for (x, y, z), (name, props, _) in blocks.items():
        t = _top(name, props)
        if t is None or t == 9:
            continue
        hoogte = y + t
        vrij = True
        for yy in (math.floor(hoogte), math.floor(hoogte) + 1):
            if yy == y and t == 0.5:
                continue
            b = blocks.get((x, yy, z))
            if b is not None and _top(b[0], b[1]) is not None:
                vrij = False
        if vrij:
            staan.setdefault((x, z), []).append(hoogte)
    return staan


def bereik(stad, start):
    """Flood from `start` (x, y, z): a step of half a block up, a jump of one block, any way down."""
    staan = loopbaar(stad)
    def dichtbij(x, y, z):
        hs = [hh for hh in staan.get((x, z), []) if abs(hh - y) <= 1.01]
        return min(hs, key=lambda hh: abs(hh - y)) if hs else None
    h0 = dichtbij(*start[:1], start[1], start[2])
    if h0 is None:
        return set()
    gezien = {(start[0], start[2], h0)}
    stapel = [(start[0], start[2], h0)]
    while stapel:
        x, z, hh = stapel.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for h2 in staan.get((x + dx, z + dz), []):
                if h2 - hh <= 1.01 and hh - h2 <= 12 and (x + dx, z + dz, h2) not in gezien:
                    gezien.add((x + dx, z + dz, h2))
                    stapel.append((x + dx, z + dz, h2))
    return gezien


def check(stad):
    from features import paleizen_bouw
    problems = []
    blocks = stad.s.blocks
    # every spot the story needs can be walked to from the gate
    start = stad.plek["poort"]
    gezien = bereik(stad, start)
    if not gezien:
        problems.append(f"nobody can stand at the gate {start}")
    for naam, (x, y, z) in stad.moet_bereiken.items():
        if not any((x, z, hh) in gezien for hh in (y, y + 0.5, y - 0.5)):
            problems.append(f"{naam} {(x, y, z)} can't be walked to from the gate")
    # without jumping: the main way (gate -> hall, guest flet, mirror, jetty) only has half steps
    # things that hang or stand on something
    for (x, y, z), (name, props, _) in blocks.items():
        if name == LANTAARN:
            steun = blocks.get((x, y + 1, z) if props.get("hanging") == "true" else (x, y - 1, z))
            if steun is None or steun[0] in (AIR, SAUS, WATER):
                problems.append(f"a lamp without support at {(x, y, z)}")
        elif name.endswith(("_carpet", "_sign", "_banner")) or name in ("minecraft:lily_of_the_valley", "minecraft:white_tulip", "minecraft:oxeye_daisy",
                                                                       "minecraft:azure_bluet", "guhs:pindascheutjes", "guhs:sate_zwammetje",
                                                                       "minecraft:flowering_azalea", VUUR):
            steun = blocks.get((x, y - 1, z))
            if steun is None or _top(steun[0], steun[1]) is None:
                problems.append(f"{name} on nothing at {(x, y, z)}")
            if name in ("guhs:pindascheutjes", "guhs:sate_zwammetje") and steun and steun[0] != NYLIUM:
                problems.append(f"{name} not on nylium at {(x, y, z)}")
            if name.startswith("minecraft:") and name.endswith(("valley", "tulip", "daisy", "bluet", "azalea")) and steun and steun[0] != MOS:
                problems.append(f"{name} not on moss at {(x, y, z)}")
    # the sauce and the water can't run away: every side and the bottom is closed
    for (x, y, z), (name, props, _) in blocks.items():
        if name in (SAUS, WATER):
            for c in ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1), (x, y - 1, z)):
                b = blocks.get(c)
                if b is None or (b[0] != name and _top(b[0], b[1]) != 1.0):
                    problems.append(f"{name} at {(x, y, z)} can run away through {c} ({b[0] if b else 'nothing'})")
                    break
    # the important things exist
    for naam in ("spiegel", "gast_vuur", "aanleg_vuur"):
        x, y, z = stad.plek[naam]
        if stad.get(x, y, z) not in (SPIEGEL, VUUR):
            problems.append(f"plek {naam} is {stad.get(x, y, z)}")
    for naam in ("zaal", "dal", "leguhlas_poort", "leguhlas_steiger", "gimguh_steiger", "aanleg", "steiger"):
        x, y, z = stad.plek[naam]
        onder = blocks.get((x, y - 1, z))
        if onder is None or _top(onder[0], onder[1]) is None or not stad.vrij(x, y, z) and stad.get(x, y, z) not in NIET_VAST:
            problems.append(f"plek {naam} {(x, y, z)}: nothing to stand on, or no room (under: {onder[0] if onder else None}, at: {stad.get(x, y, z)})")
    if len(stad.s.entities) != 10:
        problems.append(f"{len(stad.s.entities)} entities, expected 10")
    # the seams of a burcht (ladders, wall signs, beds: there are none, this keeps it that way)
    problems += paleizen_bouw.check_steun(stad, ANKER, TILE)
    return problems


def save(h, stad):
    """The tiles <NAAM>/stuk_<i>_<j> (empty ones left out). Returns (tiles_x, tiles_z, saved)."""
    W, H, D = SIZE
    tegels, wezens = {}, {}
    for (x, y, z), b in stad.s.blocks.items():
        tegels.setdefault((x // TILE, z // TILE), {})[(x % TILE, y, z % TILE)] = b
    for (x, y, z, nbt) in stad.s.entities:
        wezens.setdefault((int(x) // TILE, int(z) // TILE), []).append((x - (int(x) // TILE) * TILE, y, z - (int(z) // TILE) * TILE, nbt))
    nx, nz = (W + TILE - 1) // TILE, (D + TILE - 1) // TILE
    bewaard = []
    for i in range(nx):
        for j in range(nz):
            if (i, j) not in tegels:
                continue
            t = h.Structure((min(TILE, W - i * TILE), H, min(TILE, D - j * TILE)))
            t.blocks = tegels[(i, j)]
            t.entities = wezens.get((i, j), [])
            t.save(f"{NAAM}/stuk_{i}_{j}")
            bewaard.append((i, j))
    return nx, nz, bewaard


def plekken_json(stad):
    """data/guhs/ringh4/plekken.json: every spot (template coordinates of the whole build) and the boat's path."""
    return {"anker": list(ANKER), "grootte": list(SIZE),
            "plekken": {k: [float(c) for c in v] for k, v in sorted(stad.plek.items())},
            "route": [[float(c) for c in p] for p in stad.route]}


def test_template(h):
    """RingH4GameTests: a floor, the mirror on a pedestal, a Rustvuurtje, a strip of sauce for the boat."""
    s = h.Structure((25, 8, 17))
    for x in range(25):
        for z in range(17):
            s.set(x, 0, z, ROTS)
    for x in range(3, 22):
        for z in range(12, 15):
            s.set(x, 0, z, SAUS, {"level": "0"})
    s.set(4, 1, 4, KWARTS_PILAAR, {"axis": "y"})
    s.set(4, 2, 4, SPIEGEL)
    s.set(12, 1, 4, VUUR)
    return s


# =====================================================================================================================
# pictures (not part of the build)
# =====================================================================================================================
class _H:
    """What bouw() needs of the make_v2 namespace, for the preview."""
    def __init__(self):
        import make_structures as ms
        self.ms, self.Structure, self.Byte, self.floats = ms, ms.Structure, ms.Byte, ms.floats


def _deel(s, f, grootte=None):
    import make_structures as ms
    n = ms.Structure(grootte or s.size)
    n.blocks = {c: v for c, v in s.blocks.items() if f(c, v)}
    return n


def _uitsnede(s, x0, x1, z0, z1, y0=0, y1=99):
    import make_structures as ms
    n = ms.Structure((x1 - x0 + 1, min(s.size[1], y1 + 1) - y0, z1 - z0 + 1))
    n.blocks = {(x - x0, y - y0, z - z0): v for (x, y, z), v in s.blocks.items() if x0 <= x <= x1 and z0 <= z <= z1 and y0 <= y <= y1}
    return n


def _gedraaid(s, kwart):
    import make_structures as ms
    for _ in range(kwart % 4):
        W, H, D = s.size
        t = ms.Structure((D, H, W))
        for (x, y, z), b in s.blocks.items():
            t.blocks[(D - 1 - z, y, x)] = b
        s = t
    return s


def preview(out, alleen=None):
    import wiki_renders as wr
    kleur = {LANTAARN: (150, 236, 240), KETTING: (70, 74, 90), STAAF: (240, 236, 250), HEK: (196, 178, 122), VUUR: (255, 150, 50),
             "minecraft:birch_sign": (196, 178, 122), "minecraft:white_banner": (240, 240, 240), SPIEGEL: (170, 230, 255), SAUS: (242, 165, 22),
             "guhs:pindascheutjes": (214, 150, 60), "guhs:sate_zwammetje": (190, 110, 60), "minecraft:flowering_azalea": (110, 150, 60),
             "minecraft:lily_of_the_valley": (240, 244, 240), "minecraft:white_tulip": (240, 244, 240), "minecraft:oxeye_daisy": (240, 244, 230),
             "minecraft:azure_bluet": (230, 240, 240), "minecraft:moss_carpet": (90, 110, 45), LUIK: (196, 178, 122),
             KWARTS_PLAAT: (236, 230, 222), KWARTS_GLAD: (236, 230, 222), "minecraft:quartz_stairs": (236, 230, 222), KWARTS_PILAAR: (230, 224, 216),
             "minecraft:mossy_cobblestone_slab": (110, 120, 100), TUF_PLAAT: (98, 104, 96), "minecraft:stripped_birch_wood": (196, 176, 118),
             "minecraft:birch_stairs": (196, 178, 122), WATER: (70, 130, 230)}
    for k in ("white", "lime", "yellow", "cyan"):
        kleur[f"guhs:{k}_kussen"] = {"white": (236, 236, 236), "lime": (120, 200, 60), "yellow": (240, 200, 60), "cyan": (40, 150, 160)}[k]
    wr.SPECIAL_COLOURS.update(kleur)
    wr.SEE_THROUGH = ("glass", "cobweb", "iron_bars")
    wr.block_colour.cache_clear()
    os.makedirs(out, exist_ok=True)
    stad = bouw(_H())
    for p in check(stad):
        print("PROBLEM:", p)
    s = stad.s
    print("size", SIZE, "blocks", sum(1 for v in s.blocks.values() if v[0] != AIR), "air", sum(1 for v in s.blocks.values() if v[0] == AIR))
    kaal = _deel(s, lambda c, v: stad.soort.get((c[0], c[2])) not in ("wand", "rand") or v[0] not in (ROTS, STENEN, GEBARSTEN))
    kaal_zb = _deel(kaal, lambda c, v: v[0] not in BLAD)
    beelden = {
        "alles": lambda: wr.render_structure(s, {}, px=7, max_size=2400),
        "binnen": lambda: wr.render_structure(kaal, {}, px=8, max_size=2600),
        "binnen_om": lambda: wr.render_structure(_gedraaid(kaal, 2), {}, px=8, max_size=2600),
        "stad_binnen": lambda: wr.render_structure(_uitsnede(kaal, 0, 95, 0, 95), {}, px=13, max_size=2400),
        "stad_binnen_zb": lambda: wr.render_structure(_uitsnede(kaal_zb, 0, 95, 0, 95), {}, px=13, max_size=2400),
        "stad_binnen_zb_om": lambda: wr.render_structure(_gedraaid(_uitsnede(kaal_zb, 0, 95, 0, 95), 2), {}, px=13, max_size=2400),
        "rivier_binnen": lambda: wr.render_structure(_uitsnede(kaal, 80, 191, 14, 82), {}, px=11, max_size=2600),
        "rivier_binnen_om": lambda: wr.render_structure(_gedraaid(_uitsnede(kaal, 80, 191, 14, 82), 2), {}, px=11, max_size=2600),
        "beelden_binnen": lambda: wr.render_structure(_uitsnede(kaal, 112, 150, 18, 66, 0, 34), {}, px=22, max_size=2400),
        "alles_om": lambda: wr.render_structure(_gedraaid(s, 2), {}, px=7, max_size=2400),
        "stad": lambda: wr.render_structure(_uitsnede(s, 0, 95, 0, 95), {}, px=12, max_size=2200),
        "stad_om": lambda: wr.render_structure(_gedraaid(_uitsnede(s, 0, 95, 0, 95), 2), {}, px=12, max_size=2200),
        "stad_zonder_blad": lambda: wr.render_structure(_deel(_uitsnede(s, 0, 95, 0, 95), lambda c, v: v[0] not in BLAD, (96, 52, 96)), {}, px=12, max_size=2200),
        "stad_zonder_blad_om": lambda: wr.render_structure(_gedraaid(_deel(_uitsnede(s, 0, 95, 0, 95), lambda c, v: v[0] not in BLAD, (96, 52, 96)), 2), {},
                                                           px=12, max_size=2200),
        "stad_snede": lambda: wr.render_structure(_uitsnede(s, 0, 95, 46, 95), {}, px=12, max_size=2200),
        "zaal": lambda: wr.render_structure(_deel(_uitsnede(s, 34, 62, 32, 60, 20, 34), lambda c, v: v[0] not in BLAD), {}, px=26, max_size=1800),
        "dal": lambda: wr.render_structure(_uitsnede(s, 20, 42, 56, 78, 0, 9), {}, px=30, max_size=1800),
        "gast": lambda: wr.render_structure(_deel(_uitsnede(s, 20, 40, 18, 38, 0, 18), lambda c, v: v[0] not in BLAD), {}, px=26, max_size=1800),
        "poort": lambda: wr.render_structure(_gedraaid(_uitsnede(s, 0, 20, 36, 60, 0, 22), 1), {}, px=26, max_size=1800),
        "haven": lambda: wr.render_structure(_uitsnede(s, 72, 100, 36, 60, 0, 12), {}, px=26, max_size=1800),
        "rivier": lambda: wr.render_structure(_uitsnede(s, 88, 191, 14, 82), {}, px=10, max_size=2400),
        "rivier_om": lambda: wr.render_structure(_gedraaid(_uitsnede(s, 88, 191, 14, 82), 2), {}, px=10, max_size=2400),
        "beelden": lambda: wr.render_structure(_gedraaid(_uitsnede(s, 116, 146, 22, 62, 0, 34), 1), {}, px=20, max_size=2200),
        "beelden_voor": lambda: wr.render_structure(_gedraaid(_deel(_uitsnede(s, 116, 146, 22, 62, 0, 34), lambda c, v: c[0] >= 0), 3), {}, px=20, max_size=2200),
        "beeld_dichtbij": lambda: wr.render_structure(_gedraaid(_uitsnede(s, 120, 140, 18, 40, 2, 32), 1), {}, px=30, max_size=2000),
        "aanleg": lambda: wr.render_structure(_uitsnede(s, 164, 191, 34, 62, 0, 14), {}, px=26, max_size=1800),
    }
    for naam, maak in beelden.items():
        if alleen and naam not in alleen:
            continue
        maak().save(os.path.join(out, f"boomstad_{naam}.png"))
        print("picture", naam)
    return stad


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    preview(sys.argv[1] if len(sys.argv) > 1 else ".", sys.argv[2:])
