"""
Op Safari! (1.6.0, slice F1b): het safari-busstation in de Guhmension, the way into Safaripark Vadse Bergen.

  - the structure safari_busstation (64 x 34 x 64, one piece around the anchor guhs:safari_busstation_midden), after the
    approved render safari-ontwerp/busstation_iso.png (source safari-ontwerp/_bron/render_busstation.py):
      a thatched lodge with guh ears on the ridge (north), the olifantenpoort in front of it (two elephants, their trunks
      meeting in an arch), the bus platform with the round yellow Guh-safaribus (east), three mini-enclosures with
      stokstaartjes, flamingo's and pinguins (west), a big park map beside the gate and a little giftshop (south east)
  - one guaranteed copy 500-2000 blocks from 0,0 (new chunks only) plus a rare set (copies at least ~2000 blocks apart)
  - the chauffeur-guh's skin (npc_safari_chauffeur) and the Dutch texts (English: tools/lang/en/c186_safari_busstation.json)

The animals, the chauffeur, the shop guh and the visitor guhs are not in the template: feature/safari/bus/Busstation
registers them with wereld.Bezetting, so every copy (also one placed by hand with /guhs safari busstation) gets them.
The coordinates the Java side uses are at the top (BUS, CHAUFFEUR, ...): keep them the same in Busstation.java.

check(s) is the self-check (SystemExit on problems): the bus blocks are only in the bus, the chauffeur's spot is free
and on a floor, the anchor is there, and the enclosure rule of round 36: next to a raise inside a pen the fence is at
least 2 blocks higher than that raise. Run on its own:  python tools/features/safari_busstation.py  (project root)
"""
import math
import os
import random

NAME = "safari_busstation"
ANCHOR = "guhs:safari_busstation_midden"
W, H, D = 64, 34, 64
G = 6                                   # the ground's top block (template y); people stand at G + 1
C = 32                                  # the anchor column (x, z)
MIDX = 31                               # the middle of the main path

# --- coordinates shared with feature/safari/bus/Busstation.java ----------------------------------------------------
BUS = (50, 26, 55, 41)                  # x1, z1, x2, z2 of the bus body (y G + 1 .. G + 9); its nose looks south (+z)
BUS_LIJF = "minecraft:yellow_terracotta"  # only the bus is made of it (check), so a click on it is a click on the bus
CHAUFFEUR = (57, G + 1, 38)             # on the platform beside the bus door, looking west at the bus
BEL = (56, G + 1, 44)                   # the bell at the stop, on the kerb in front of the bus
VERKOPER = (41, G + 1, 54)              # inside the giftshop
PENNEN = {                              # x1, z1, x2, z2 of the three mini-enclosures
    "stokstaartje": (2, 28, 13, 38),
    "flamingo": (2, 40, 13, 52),
    "pinguin": (2, 54, 13, 62),
}
DIEREN = [                              # (soort, variant, x, z): their homes; Busstation.java has the same list
    ("stokstaartje", "vrouw", 5, 31), ("stokstaartje", "man", 9, 35), ("stokstaartje", "vrouw", 4, 35),
    ("stokstaartje", "baby", 6, 34), ("stokstaartje", "vrouw", 10, 31),
    ("flamingo", "vrouw", 5, 44), ("flamingo", "man", 8, 46), ("flamingo", "vrouw", 9, 49), ("flamingo", "baby", 6, 48),
    ("pinguin", "vrouw", 6, 58), ("pinguin", "man", 4, 60), ("pinguin", "baby", 5, 61), ("pinguin", "baby", 7, 60),
]
BEZOEKERS = [(30, 30), (33, 29), (31, 44), (32, 52), (16, 41), (19, 32), (40, 46), (25, 30)]

TEKSTEN = {
    "entity.guhs.guh_npc.safari_chauffeur": "Chauffeur Toeter",
    "gui.guhs.safari.bus.chauffeur.hallo": "Goeiedag, vads! Ik ben Toeter, chauffeur van de Guh-safaribus. Hij is gratis en hij is geel. Zin in een ritje naar Safaripark Vadse Bergen?",
    "gui.guhs.safari.bus.chauffeur.instappen": "Instappen!",
    "gui.guhs.safari.bus.chauffeur.straks": "Nog even niet",
    "gui.guhs.safari.bus.chauffeur.straks.antwoord": "Prima, ik wacht hier wel. Ik heb toch nog een knabbel.",
    "gui.guhs.safari.bus.instappen": "Je stapt in de Guh-safaribus...",
    "gui.guhs.safari.bus.storing": "De bus wil niet starten. Probeer het zo nog eens, njeg.",
    "gui.guhs.safari.bus.al_bezig": "Even wachten, je zit al in een filmpje.",
    "gui.guhs.safari.bus.guhs_mee": "%s van je guhs rijden mee op het dak.",
    "gui.guhs.safari.bus.niet_bouwen": "Dit busstation is van iedereen: hier mag je niks slopen of bouwen.",
    "commands.guhs.safari.busstation.ok": "Busstation geplaatst bij %s.",
    "commands.guhs.safari.busstation.nee": "Hier past geen busstation (te scheef of geen plek). Probeer een vlakkere plek.",
    "commands.guhs.safari.busstation.waar": "Dichtstbijzijnde busstation: %s (%s blokken).",
    "commands.guhs.safari.busstation.geen": "Geen busstation gevonden binnen %s blokken.",
}
# the bus rides (feature/safari/bus/BusScenes): three variants of the way there (another joke each time), one way back
GRAPPEN = [
    "Weet je waarom de zebra strepen heeft? Zodat hij niet kwijtraakt in de streepjescode-winkel. Njeg.",
    "Wist je dat een flamingo roze wordt van wat hij eet? Ik eet vooral kaasknabbels. Kijk maar naar mijn wangen.",
    "Een olifant vergeet nooit iets. Ik vergeet altijd waar ik de bus geparkeerd heb. Gelukkig is hij geel.",
]
HEEN = {
    "titel": "Met de Guh-safaribus naar het park",
    "naam.chauffeur": "Toeter",
    "weg": "Riemen vast, snoet naar voren! Daar gaan we, vads!",
    "savanne": "Ruik je dat? Dat is de savanne. Of mijn lunch. Allebei lekker.",
    "zebra": "Njeg, even wachten! Zebra's hebben altijd voorrang.",
    "poort": "Kijk, de olifantenpoort! Welkom in Safaripark Vadse Bergen!",
    "uitstappen": "Eindhalte! Uitstappen aan de knuffelkant. Veel plezier, en niet de dieren voeren zonder te vragen!",
}
TERUG = {
    "titel": "Met de Guh-safaribus naar huis",
    "naam.chauffeur": "Toeter",
    "zon": "De zon zakt al achter de rieten lodge. Wat een dag, hè?",
    "doei": "Tot de volgende keer, vads!",
}


def mc(n):
    return n if ":" in n else "minecraft:" + n


def parse(spec):
    """'minecraft:x[a=b,c=d]' -> (name, props)."""
    if "[" in spec:
        naam, rest = spec.split("[", 1)
        props = dict(kv.split("=") for kv in rest.rstrip("]").split(","))
        return mc(naam), props
    return mc(spec), None


class Bouw:
    """The template with the render kit's little drawing helpers."""

    def __init__(self, h):
        self.s = h.Structure((W, H, D))

    def set(self, x, y, z, spec, nbt=None):
        naam, props = parse(spec)
        self.s.set(x, y, z, naam, props, nbt)

    def get(self, x, y, z):
        return self.s.get(int(round(x)), int(round(y)), int(round(z)))

    def fill(self, x1, y1, z1, x2, y2, z2, spec):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, spec)

    def air(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, "minecraft:air")

    def leeg(self, x, y, z):
        return self.get(x, y, z) in (None, "minecraft:air")

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, spec):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
                for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
                    if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 <= 1.0:
                        self.set(x, y, z, spec)

    def top_y(self, x, z):
        for y in range(H - 1, -1, -1):
            if not self.leeg(x, y, z):
                return y
        return 0


LOG = "minecraft:stripped_spruce_log[axis=y]"
POST = "minecraft:dark_oak_log[axis=y]"
HAY = "minecraft:hay_block[axis=y]"
WATER = "minecraft:water[level=0]"
BED = "minecraft:light_blue_concrete_powder"
GEZICHT_Z = "guhs:knuffelsteen_gezicht[facing=south,stemming=1]"
GEZICHT_N = "guhs:knuffelsteen_gezicht[facing=north,stemming=1]"
PANE = "minecraft:glass_pane"
FENCE_LOS = "[north=false,south=false,east=false,west=false,waterlogged=false]"


def box(b, x1, y1, z1, x2, y2, z2, wall, floor=None, corner=LOG):
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            if x in (x1, x2) or z in (z1, z2):
                b.fill(x, y1, z, x, y2, z, wall)
    for (x, z) in ((x1, z1), (x2, z1), (x1, z2), (x2, z2)):
        b.fill(x, y1, z, x, y2, z, corner)
    if floor:
        b.fill(x1, y1 - 1, z1, x2, y1 - 1, z2, floor)
    b.air(x1 + 1, y1, z1 + 1, x2 - 1, y2, z2 - 1)


def windows(b, x1, y, z1, x2, z2, every=3):
    for x in range(x1 + 2, x2 - 1, every):
        for z in (z1, z2):
            b.set(x, y, z, PANE + FENCE_LOS)
    for z in range(z1 + 2, z2 - 1, every):
        for x in (x1, x2):
            b.set(x, y, z, PANE + FENCE_LOS)


def thatch(b, x1, z1, x2, z2, y, overhang=1):
    a, bb, c, d = x1 - overhang, x2 + overhang, z1 - overhang, z2 + overhang
    lvl = 0
    while a <= bb and c <= d:
        for x in range(a, bb + 1):
            for z in range(c, d + 1):
                if x in (a, bb) or z in (c, d) or a + 1 >= bb or c + 1 >= d:
                    b.set(x, y + lvl, z, HAY)
        a, bb, c, d = a + 1, bb - 1, c + 1, d - 1
        lvl += 1
    return y + lvl


def bouw(h):
    rnd = random.Random(20261022)
    b = Bouw(h)
    # --- the ground: stone, dirt, knuffelgras; a round plate so the land runs on at the corners -----------------------
    for x in range(W):
        for z in range(D):
            b.fill(x, 0, z, x, G - 3, z, "minecraft:stone")
            b.fill(x, G - 2, z, x, G - 1, z, "minecraft:dirt")
            b.set(x, G, z, "guhs:knuffelgras")
    b.s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1)
    pad = ["guhs:knuffelklinkers", "guhs:knuffelklinkers", "minecraft:smooth_sandstone", "minecraft:sandstone"]

    def path_rect(x1, z1, x2, z2):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                b.set(x, G, z, rnd.choice(pad))

    path_rect(29, 26, 34, D - 1)
    path_rect(14, 27, 46, 34)
    path_rect(35, 44, 48, 47)
    path_rect(15, 35, 16, 41)
    path_rect(15, 41, 28, 43)
    # the bus road (east)
    for x in range(49, 57):
        for z in range(0, D):
            b.set(x, G, z, "minecraft:gray_concrete_powder" if x in (49, 56) else "minecraft:smooth_stone")

    # --- the lodge (north) ----------------------------------------------------------------------------------------------
    LX1, LZ1, LX2, LZ2 = 12, 6, 47, 24
    box(b, LX1, G + 1, LZ1, LX2, G + 7, LZ2, "minecraft:spruce_planks", floor="minecraft:spruce_planks")
    for x in range(LX1, LX2 + 1):
        for z in (LZ1, LZ2):
            b.set(x, G + 1, z, "minecraft:sandstone")
    for z in range(LZ1, LZ2 + 1):
        for x in (LX1, LX2):
            b.set(x, G + 1, z, "minecraft:sandstone")
    windows(b, LX1, G + 3, LZ1, LX2, LZ2)
    windows(b, LX1, G + 4, LZ1, LX2, LZ2)
    b.air(29, G + 1, LZ2, 34, G + 3, LZ2)           # the wide open front
    top = thatch(b, LX1, LZ1, LX2, LZ2, G + 8, overhang=2)
    # a ceiling under the thatch, so the lamps hang on something
    b.fill(LX1 + 1, G + 7, LZ1 + 1, LX2 - 1, G + 7, LZ2 - 1, "minecraft:spruce_planks")
    # inside: a waiting room with benches, a counter with a map, guh lamps, plants
    for x in range(LX1 + 3, LX2 - 2, 5):
        b.set(x, G + 1, 12, "guhs:guh_bank[facing=south]")
        b.set(x, G + 1, 18, "guhs:guh_bank[facing=north]")
    for x in (LX1 + 2, LX2 - 2):
        b.set(x, G + 1, LZ1 + 1, "minecraft:potted_fern")
        b.set(x, G + 1, LZ2 - 1, "minecraft:potted_fern")
    for x in range(LX1 + 4, LX2 - 3, 6):
        for z in (10, 15, 20):
            b.set(x, G + 6, z, "minecraft:lantern[hanging=true,waterlogged=false]")
    b.fill(26, G + 1, 8, 37, G + 1, 8, "minecraft:spruce_planks")    # the counter
    b.set(31, G + 2, 8, "guhs:guh_tafel")
    # porch with posts, and faces on the front
    for x in range(LX1, LX2 + 1):
        for z in (LZ2 + 1, LZ2 + 2):
            b.set(x, G, z, "minecraft:spruce_planks")
    for x in range(LX1, LX2 + 1, 4):
        if not 29 <= x <= 34:
            b.fill(x, G + 1, LZ2 + 2, x, G + 7, LZ2 + 2, LOG)
    for x in (22, 41):
        b.set(x, G + 6, LZ2, GEZICHT_Z)
    for ex in (24, 36):                                 # guh ears on the ridge
        b.fill(ex, top - 1, 14, ex + 1, top + 1, 16, "minecraft:pink_wool")
        b.set(ex, top + 2, 15, "minecraft:pink_wool")
        b.set(ex + 1, top + 2, 15, "minecraft:pink_wool")
    b.fill(27, G + 5, LZ2 + 1, 36, G + 6, LZ2 + 1, "minecraft:yellow_concrete")    # sign "Safaribus" over the door
    b.fill(28, G + 5, LZ2 + 1, 35, G + 5, LZ2 + 1, "minecraft:green_concrete")

    # --- the olifantenpoort ----------------------------------------------------------------------------------------------
    GZ = 41
    GRAY = "minecraft:light_gray_concrete"

    def olifant_zij(xc, zc, f):
        bx = xc - 3 * f
        for (a, c) in ((-2, -2), (-2, 1), (1, -2), (1, 1)):
            b.fill(bx + a, G + 1, zc + c, bx + a + 1, G + 3, zc + c + 1, GRAY)
        b.ellipsoid(bx, G + 5.5, zc + 0.5, 4.2, 3.0, 3.0, GRAY)
        hx = xc + 1.5 * f
        b.ellipsoid(hx, G + 8.5, zc + 0.5, 2.8, 2.8, 2.8, GRAY)
        for side in (-1, 1):
            ez = zc + 0.5 + side * 3.4
            b.fill(int(hx - f * 1), G + 7, int(round(ez)), int(hx + f * 1), G + 11, int(round(ez)), "minecraft:pink_wool")
        fx = int(round(hx + 2.8 * f))
        b.set(fx, G + 9, zc - 1, "minecraft:black_concrete")
        b.set(fx, G + 9, zc + 2, "minecraft:black_concrete")
        b.set(fx, G + 8, zc - 2, "minecraft:pink_wool")
        b.set(fx, G + 8, zc + 3, "minecraft:pink_wool")
        b.set(fx, G + 6, zc - 1, "minecraft:white_concrete")
        b.set(fx, G + 6, zc + 2, "minecraft:white_concrete")
        x0, x1 = fx, MIDX + (0 if f > 0 else 1)
        n = abs(x1 - x0)
        for i in range(n + 1):
            t = i / max(n, 1)
            x = x0 + f * i
            y = int(round(G + 7 + 6 * math.sin(t * math.pi / 2)))
            for zz in (zc, zc + 1):
                b.set(x, y, zz, GRAY)
                b.set(x, y + 1, zz, GRAY)

    olifant_zij(22, GZ, 1)
    olifant_zij(41, GZ, -1)
    for bx in (19, 44):
        for x in range(bx - 4, bx + 4):
            for z in range(GZ - 4, GZ + 6):
                if b.get(x, G, z) == "guhs:knuffelgras":
                    b.set(x, G, z, "minecraft:sandstone")
    b.fill(MIDX - 2, G + 11, GZ, MIDX + 3, G + 11, GZ, "minecraft:spruce_fence" + FENCE_LOS)
    b.fill(MIDX - 2, G + 9, GZ, MIDX + 3, G + 10, GZ, "minecraft:yellow_concrete")
    b.set(MIDX, G + 10, GZ, GEZICHT_Z)
    b.set(MIDX + 1, G + 9, GZ, "minecraft:green_concrete")

    # --- the big park map beside the gate ---------------------------------------------------------------------------------
    kaart = ["WWWiiiiBBB", "EEEEiiiBBR", "EE~~~~~~RR", "EAAAERRRDD", "AAAAARRR~D", "AA~~~~~~~D", "kSSSSSSSSS"]
    kleur = {"W": "white_concrete", "i": "light_gray_concrete", "B": "lime_concrete", "E": "green_concrete", "R": "green_terracotta",
             "D": "cyan_terracotta", "A": "orange_terracotta", "S": "yellow_concrete", "~": "light_blue_concrete", "k": "pink_concrete"}
    MX0, MZ = 15, 29
    for r, row in enumerate(kaart):
        for c, ch in enumerate(row):
            b.set(MX0 + c, G + 9 - r, MZ, kleur[ch])
    for x in (MX0 - 1, MX0 + 10):
        b.fill(x, G + 1, MZ, x, G + 10, MZ, "minecraft:dark_oak_log[axis=y]")
    b.fill(MX0 - 1, G + 10, MZ, MX0 + 10, G + 10, MZ, "minecraft:dark_oak_log[axis=x]")
    b.fill(MX0 - 1, G + 2, MZ, MX0 + 10, G + 2, MZ, "minecraft:dark_oak_planks")
    b.fill(MX0 + 2, G + 11, MZ, MX0 + 7, G + 11, MZ, HAY)

    # --- the mini-enclosures (west): the safari fence, 4 high, every raise at least 3 from it --------------------------
    def pen(x1, z1, x2, z2, vloer):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                b.set(x, G, z, vloer(x, z))
                if x in (x1, x2) or z in (z1, z2):
                    if (x - x1 + z - z1) % 3 == 0:
                        b.fill(x, G + 1, z, x, G + 4, z, POST)
                        b.set(x, G + 5, z, "minecraft:dark_oak_fence" + FENCE_LOS)
                    else:
                        b.fill(x, G + 1, z, x, G + 3, z, "minecraft:iron_bars" + FENCE_LOS)
                        b.set(x, G + 4, z, "minecraft:dark_oak_fence" + FENCE_LOS)

    x1, z1, x2, z2 = PENNEN["stokstaartje"]
    pen(x1, z1, x2, z2, lambda x, z: "minecraft:sand" if (x * 7 + z) % 5 else "minecraft:coarse_dirt")
    b.fill(7, G + 1, 32, 8, G + 2, 33, "minecraft:red_terracotta")        # the termite mound (middle of the pen)
    b.set(7, G + 3, 32, "minecraft:red_terracotta")
    x1, z1, x2, z2 = PENNEN["flamingo"]
    pen(x1, z1, x2, z2, lambda x, z: "minecraft:grass_block[snowy=false]")
    for x in range(3, 13):
        for z in range(41, 52):
            if math.hypot((x - 7.5) / 4.2, (z - 46.5) / 4.8) < 1:
                b.set(x, G - 1, z, BED)
                b.set(x, G, z, WATER)
    for (x, z) in ((4, 50), (11, 42), (12, 50)):
        b.fill(x, G + 1, z, x, G + 2, z, "minecraft:sugar_cane[age=0]")
    x1, z1, x2, z2 = PENNEN["pinguin"]
    pen(x1, z1, x2, z2, lambda x, z: "minecraft:snow_block" if x < 9 else "minecraft:packed_ice")
    for x in range(9, 13):
        for z in range(55, 62):
            b.set(x, G - 1, z, "minecraft:packed_ice")
            b.set(x, G, z, WATER)
    b.fill(5, G + 1, 57, 6, G + 2, 58, "minecraft:snow_block")            # the snow hill (middle of the pen)
    for z in (33, 46, 58):                                                  # little signs at the pens
        b.fill(15, G + 1, z, 15, G + 2, z, "minecraft:spruce_fence" + FENCE_LOS)
        b.set(15, G + 3, z, "minecraft:yellow_concrete")

    # --- the bus platform + the Guh-safaribus (east) ------------------------------------------------------------------
    for x in range(57, 63):
        for z in range(20, 56):
            b.set(x, G, z, "minecraft:smooth_stone")
    for x in (58, 62):
        for z in range(22, 55, 6):
            b.fill(x, G + 1, z, x, G + 6, z, LOG)
    thatch(b, 58, 22, 62, 54, G + 7, overhang=1)
    for z in range(26, 52, 5):
        if z != CHAUFFEUR[2]:
            b.set(61, G + 1, z, "guhs:guh_bank[facing=west]")
    b.fill(57, G + 1, 21, 57, G + 2, 21, "minecraft:spruce_fence" + FENCE_LOS)
    b.fill(57, G + 3, 20, 57, G + 4, 22, "minecraft:yellow_concrete")      # the stop sign
    for z in range(24, 52, 6):
        b.set(60, G + 6, z, "minecraft:lantern[hanging=true,waterlogged=false]")

    bx1, bz1, bx2, bz2 = BUS
    for z in (bz1 + 2, bz2 - 2):
        for x in (bx1, bx2):
            b.fill(x, G + 1, z, x, G + 1, z + 1, "minecraft:black_concrete")
    b.fill(bx1, G + 2, bz1, bx2, G + 6, bz2, BUS_LIJF)
    b.fill(bx1, G + 3, bz1, bx2, G + 3, bz2, "minecraft:green_terracotta")
    b.fill(bx1 + 1, G + 7, bz1 + 1, bx2 - 1, G + 7, bz2 - 1, BUS_LIJF)
    b.fill(bx1 + 2, G + 8, bz1 + 3, bx2 - 2, G + 8, bz2 - 3, BUS_LIJF)
    for (x, z) in ((bx1, bz1), (bx2, bz1), (bx1, bz2), (bx2, bz2)):
        b.set(x, G + 6, z, "minecraft:air")
        b.set(x, G + 2, z, "minecraft:air")
    for z in range(bz1 + 1, bz2):
        for x in (bx1, bx2):
            b.set(x, G + 5, z, "minecraft:light_blue_stained_glass" if z % 3 else BUS_LIJF)
    b.fill(bx1 + 1, G + 4, bz2, bx2 - 1, G + 5, bz2, "minecraft:light_blue_stained_glass")
    b.set(bx1 + 1, G + 2, bz2, "minecraft:yellow_concrete")
    b.set(bx2 - 1, G + 2, bz2, "minecraft:yellow_concrete")
    b.set(bx1 + 2, G + 3, bz2 + 1, GEZICHT_Z)
    b.set(bx1 + 3, G + 3, bz2 + 1, GEZICHT_Z)
    b.set(bx2, G + 2, CHAUFFEUR[2], "minecraft:spruce_door[facing=east,half=lower,hinge=left,open=false,powered=false]")
    b.set(bx2, G + 3, CHAUFFEUR[2], "minecraft:spruce_door[facing=east,half=upper,hinge=left,open=false,powered=false]")
    for x in (bx1 + 1, bx2 - 1):
        b.fill(x, G + 8, bz2 - 2, x, G + 9, bz2 - 1, "minecraft:pink_wool")
    for z in range(bz1 + 4, bz1 + 9):
        if z % 2:
            b.set(52, G + 9, z, "minecraft:barrel[facing=up,open=false]")
        else:
            b.set(53, G + 9, z, "minecraft:brown_wool")
    b.set(*BEL, "minecraft:bell[attachment=floor,facing=north,powered=false]")

    # --- the giftshop (south east) -------------------------------------------------------------------------------------
    SX1, SZ1, SX2, SZ2 = 37, 48, 46, 57
    box(b, SX1, G + 1, SZ1, SX2, G + 4, SZ2, "minecraft:white_terracotta", floor="minecraft:oak_planks")
    streep = ["minecraft:pink_terracotta", "minecraft:orange_terracotta", "minecraft:light_blue_terracotta"]
    for x in range(SX1, SX2 + 1):
        for z in range(SZ1, SZ2 + 1):
            for y in (G + 1, G + 2):
                if b.get(x, y, z) == "minecraft:white_terracotta":
                    b.set(x, y, z, streep[((x + z) // 2) % 3])
    windows(b, SX1, G + 3, SZ1, SX2, SZ2)
    b.air(41, G + 1, SZ1, 42, G + 2, SZ1)
    b.fill(SX1 + 1, G + 5, SZ1 + 1, SX2 - 1, G + 5, SZ2 - 1, "minecraft:oak_planks")
    thatch(b, SX1, SZ1, SX2, SZ2, G + 5, overhang=1)
    for x in range(SX1 + 1, SX2):
        b.set(x, G + 4, SZ1 - 1, "minecraft:orange_wool" if x % 2 else "minecraft:white_wool")
    b.set(SX1 + 4, G + 5, SZ1 - 1, GEZICHT_N)
    b.fill(SX1 + 2, G + 1, SZ2 - 2, SX2 - 2, G + 1, SZ2 - 2, "minecraft:spruce_planks")    # counter
    b.set(SX1 + 1, G + 1, SZ1 + 1, "minecraft:barrel[facing=up,open=false]")
    b.set(SX2 - 1, G + 1, SZ1 + 1, "minecraft:barrel[facing=up,open=false]")
    b.set(41, G + 4, 52, "minecraft:lantern[hanging=true,waterlogged=false]")

    # --- Guhmension green, lamps --------------------------------------------------------------------------------------
    bezet = set()
    for (a1, c1, a2, c2) in ((LX1 - 3, LZ1 - 3, LX2 + 3, LZ2 + 3), (12, GZ - 5, 50, GZ + 6), (0, 26, 16, D - 1),
                             (MX0 - 2, MZ - 2, MX0 + 12, MZ + 3), (SX1 - 2, SZ1 - 3, SX2 + 2, SZ2 + 2), (47, 0, W - 1, D - 1),
                             (27, 24, 36, D - 1), (12, 26, 48, 35), (33, 43, 49, 48)):
        for x in range(a1, a2 + 1):
            for z in range(c1, c2 + 1):
                bezet.add((x, z))
    cellen = [(x, z) for x in range(W) for z in range(D)]
    rnd.shuffle(cellen)
    for (x, z) in cellen:
        if (x, z) in bezet or b.get(x, G, z) != "guhs:knuffelgras" or not b.leeg(x, G + 1, z):
            continue
        r = rnd.random()
        if r < 0.025 and 3 <= x < W - 3 and 3 <= z < D - 3 and all((x + a, z + c) not in bezet for a in range(-2, 3) for c in range(-2, 3)):
            hoog = rnd.randint(4, 6)
            b.fill(x, G + 1, z, x, G + hoog, z, "minecraft:birch_log[axis=y]")
            b.ellipsoid(x, G + hoog + 1, z, 2.3, 1.8, 2.3, "minecraft:pink_wool")
            for a in range(-3, 4):
                for c in range(-3, 4):
                    bezet.add((x + a, z + c))
        elif r < 0.30:
            b.set(x, G + 1, z, "guhs:pluisgras")
        elif r < 0.36:
            b.set(x, G + 1, z, rnd.choice(["guhs:roze_guhbloem", "guhs:guhoortjes", "guhs:roze_guhbloem"]))
    for (x, z) in ((27, 50), (36, 50), (27, 58), (36, 58), (14, 36), (47, 45)):
        b.fill(x, G + 1, z, x, G + 2, z, "minecraft:dark_oak_fence" + FENCE_LOS)
        b.set(x, G + 3, z, "guhs:park_lantaarn")

    # --- the anchor --------------------------------------------------------------------------------------------------
    b.set(C, 0, C, "minecraft:jigsaw[orientation=up_north]",
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": "minecraft:stone",
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    return b


def check(b):
    problemen = []
    bx1, bz1, bx2, bz2 = BUS
    for (x, y, z), (naam, _p, _n) in b.s.blocks.items():
        if naam == BUS_LIJF and not (bx1 <= x <= bx2 and bz1 <= z <= bz2):
            problemen.append(f"bus body block outside the bus at {(x, y, z)}")
    x, y, z = CHAUFFEUR
    if b.leeg(x, y - 1, z) or not (b.leeg(x, y, z) and b.leeg(x, y + 1, z)):
        problemen.append("the chauffeur's spot is not free on a floor")
    x, y, z = VERKOPER
    if b.leeg(x, y - 1, z) or not (b.leeg(x, y, z) and b.leeg(x, y + 1, z)):
        problemen.append("the shop guh's spot is not free on a floor")
    if b.get(C, 0, C) != "minecraft:jigsaw":
        problemen.append("no anchor jigsaw")
    hek = {"minecraft:iron_bars", "minecraft:dark_oak_log", "minecraft:dark_oak_fence"}
    for soort, (x1, z1, x2, z2) in PENNEN.items():
        # the fence: its lowest top along the edge
        hek_top = min(max(y for y in range(G + 1, H) if b.get(x, y, z) in hek)
                      for x in range(x1, x2 + 1) for z in range(z1, z2 + 1) if x in (x1, x2) or z in (z1, z2))
        for x in range(x1 + 1, x2):
            for z in range(z1 + 1, z2):
                top = G
                for y in range(G + 1, H):
                    n = b.get(x, y, z)
                    if n not in (None, "minecraft:air", "minecraft:sugar_cane", "minecraft:water"):
                        top = y
                if top > G and min(x - x1, x2 - x, z - z1, z2 - z) <= 2 and hek_top < top + 2:
                    problemen.append(f"{soort}: a raise at {(x, top, z)} within 2 of the fence (top {hek_top})")
    for soort, variant, x, z in DIEREN:
        x1, z1, x2, z2 = PENNEN[soort]
        if not (x1 < x < x2 and z1 < z < z2):
            problemen.append(f"{soort} home {(x, z)} outside its pen")
    return problemen


def textures(h):
    """Chauffeur Toeter: the sitting guh recoloured like the lobby's guhs (safari_lobby), sunny yellow."""
    from PIL import Image
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    h.save(h.recolour(src, hue=0.13, sat=0.55, val=1.0, only=h.pinkish), "entity", "npc_safari_chauffeur.png")


def structures(h):
    b = bouw(h)
    problemen = check(b)
    if problemen:
        raise SystemExit("safari_busstation check failed:\n  " + "\n  ".join(problemen))
    b.s.save(NAME)
    none = {"bounding_box": "piece", "spawns": []}
    h.TEMPLATE_SIZES[NAME] = 32
    h.FLATNESS[NAME] = 10
    h.structure(NAME, h.GUHMENSION_LAND, spacing=160, separation=126, salt=20261031, start_y=-G, reach=80, centre=ANCHOR,
                spawn_overrides={"monster": none, "ambient": none})
    h.w(f"{h.D}/worldgen/structure_set/{NAME}_gegarandeerd.json", {
        "structures": [{"structure": f"guhs:{NAME}", "weight": 1}],
        "placement": {"type": "guhs:gegarandeerd", "salt": 20261032, "min_afstand": 500, "max_afstand": 2000,
                      "alleen_nieuw": True, "sector": 15, "sectoren": 16}})
    print(f"safari_busstation: ok ({len(b.s.blocks)} blocks)")


def texts(h):
    for k, v in TEKSTEN.items():
        h.lang(k, v, v)
    for i, grap in enumerate(GRAPPEN):
        for k, v in HEEN.items():
            h.lang(f"scene.guhs.safari_bus_heen_{i}.{k}", v, v)
        h.lang(f"scene.guhs.safari_bus_heen_{i}.grap", grap, grap)
    for k, v in TERUG.items():
        h.lang(f"scene.guhs.safari_bus_terug.{k}", v, v)


def build(h):
    structures(h)
    textures(h)
    texts(h)


if __name__ == "__main__":
    import sys
    import types
    sys.path.insert(0, "tools")
    import make_v2
    hh = types.SimpleNamespace(**vars(make_v2))
    problemen = check(bouw(hh))
    print("\n".join(problemen) or "safari_busstation check: ok")
