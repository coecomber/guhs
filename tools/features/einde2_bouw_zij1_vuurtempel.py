"""
einde2 (bouw_zij1): the Vuurtempel van Roku-guh in the Knabbelkoorbos (a guhs:leegte_bouw; the build only: the Vuurwijzen,
the opening of the fire doors and the talk with Guhvatar Roku-guh are a later phase and stand as markers in the sidecar).

A crescent of black rock with knabbelkoor on it, its horns to the south. On the west horn the landing place, on the east
horn a little volcano (with a lid on it), and in the thick middle the temple: a pagoda of five storeys in red and black.
The top storey is cut in two by a wall with FIVE fire doors side by side; behind them lies the sanctum with the statue of
Roku-guh, shut until all five are opened (the middle block of each door is its lock). A lava tunnel that only the
friendly Vuurwijze knows runs from an outcrop west of the temple to a hatch behind the altar.

G = the top block layer of the island, (TX, TZ) = the middle of the tower, VLOER = the floor layers of its storeys.
"""
import math

import features.einde2_kit as kit
from features import guheinde_bouw as gb
from features import einde2_bouw_zij1_gereedschap as g

NAAM = "vuurtempel_roku"
W, H, D = 128, 84, 112
Y = 146
G = 20
TX, TZ = 64, 26
HELFT = (17, 15, 13, 12, 11)
VLOER = (G, G + 9, G + 18, G + 27, G + 36)
DAK_Y = G + 45
GROOT, UIT = (64, 50, 50.0), (64, 86, 36.0)      # the crescent: a disc minus a disc (x, z, radius)
GROND, ROTS, BLOK, BLOKTRAP, BLOKPLAAT, PILAAR = "guhs:koorkorst", "blackstone", "guhs:knabbelkoorblok", "guhs:knabbelkoorblok_trap", \
    "guhs:knabbelkoorblok_plaat", "guhs:knabbelkoorpilaar"
ROOD, ZWART, ZWARTE_STENEN, VLOERSTEEN, GOUD = "red_nether_bricks", "polished_blackstone", "polished_blackstone_bricks", "polished_blackstone", \
    "gilded_blackstone"
DAK = ("nether_brick_stairs", "nether_brick_slab")
RAAM = "orange_stained_glass_pane"
WAL = "polished_blackstone_brick_wall"
TRAP = "polished_blackstone_brick_stairs"
LANDING = (21, 31, 59, 69)    # the landing place on the west horn: x0, x1, z0, z1
VULKAAN = (103, 62)
DEUR_Z = TZ + 1               # the wall with the five fire doors
DEUR_X = [TX - 8 + 4 * k for k in range(5)]      # the middle of each door


def vorm():
    rand, onder = g.ruis("vuurtempel_rand", 9.0), g.ruis("vuurtempel_onder", 6.0)

    def v(x, z):
        n = rand(x, z) * 1.8
        rand_afstand = min(GROOT[2] + n - math.hypot(x - GROOT[0], z - GROOT[1]), math.hypot(x - UIT[0], z - UIT[1]) - UIT[2] + n)
        if LANDING[0] - 1 <= x <= LANDING[1] + 1 and LANDING[2] - 1 <= z <= LANDING[3] + 1:
            rand_afstand = max(rand_afstand, 1.5)
        if rand_afstand <= 0:
            return None
        return G, int(round(G - 3.0 - min(15.0, rand_afstand * 0.9) - onder(x, z) * 1.5))
    return v


def rots(naam):
    vlek = g.ruis(naam, 4.0)

    def f(x, y, z):
        n = vlek(x + y * 2, z - y)
        return "basalt" if n > 0.55 else ("magma_block" if n < -0.8 and y < G - 13 else ROTS)
    return f


def brazier(b, x, y, z, hoog=2):
    """A fire basket on a post (too high to walk into)."""
    b.fill(x, y, z, x, y + hoog - 1, z, WAL)
    b.set(x, y + hoog, z, "campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})


def pad(b, top, punten, breed=1):
    """A paved path along corner points (x, z)."""
    for (x0, z0), (x1, z1) in zip(punten, punten[1:]):
        n = int(max(abs(x1 - x0), abs(z1 - z0)))
        for i in range(n + 1):
            cx, cz = round(x0 + (x1 - x0) * i / n), round(z0 + (z1 - z0) * i / n)
            for dx in range(-breed - 1, breed + 2):
                for dz in range(-breed - 1, breed + 2):
                    c = (cx + dx, cz + dz)
                    if c not in top:
                        continue
                    rand = max(abs(dx), abs(dz)) > breed
                    if rand and b.s.blocks[(c[0], G, c[1])][0] in (gb.mc(BLOK),):
                        continue
                    b.set(c[0], G, c[1], ZWART if rand else BLOK)


def poort(b, x, z, as_="x"):
    """A gate over the path: two red posts, two black beams (as_ = the axis the beam lies along)."""
    dx, dz = (1, 0) if as_ == "x" else (0, 1)
    for s in (-3, 3):
        b.fill(x + dx * s, G + 1, z + dz * s, x + dx * s, G + 5, z + dz * s, ROOD)
    for s in range(-4, 5):
        b.set(x + dx * s, G + 6, z + dz * s, "nether_brick_slab", gb.slab("bottom"))
        if abs(s) <= 3:
            b.set(x + dx * s, G + 5, z + dz * s, "nether_bricks" if abs(s) != 3 else GOUD)
    b.set(x, G + 4, z, "lantern", gb.lamp(True))


def verdieping(b, k):
    """Storey k of the tower: floor, walls with corner pillars and windows, and the eave around its top."""
    half, y = HELFT[k], VLOER[k]
    top = VLOER[k + 1] if k < 4 else DAK_Y
    x0, x1, z0, z1 = TX - half, TX + half, TZ - half, TZ + half
    b.fill(x0, y, z0, x1, y, z1, VLOERSTEEN)
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            if (x - TX) % 4 == 0 and (z - TZ) % 4 == 0:
                b.set(x, y, z, BLOK)
    for yy in range(y + 1, top):
        g.doos(b, x0, yy, z0, x1, yy, z1, ZWARTE_STENEN if yy <= y + 2 else (GOUD if yy == top - 1 else ROOD))
        b.fill(x0 + 1, yy, z0 + 1, x1 - 1, yy, z1 - 1, gb.AIR)
        for px, pz in ((x0, z0), (x0, z1), (x1, z0), (x1, z1)):
            b.set(px, yy, pz, ZWART)
    b.fill(x0, top, z0, x1, top, z1, VLOERSTEEN)
    for i in range(-half + 3, half - 2, 4):          # (windows: tall slits of orange glass)
        for yy in range(y + 4, y + 7):
            if not (k == 0 and abs(i) <= 2):
                b.set(TX + i, yy, z1, RAAM)
            b.set(TX + i, yy, z0, RAAM)
            b.set(x0, yy, TZ + i, RAAM)
            b.set(x1, yy, TZ + i, RAAM)
    # the eave: a ring of stairs at the ceiling, a ring of slabs one lower and further out, and tips that curl up
    for i in range(x0 - 1, x1 + 2):
        b.set(i, top, z0 - 1, DAK[0], gb.stair("south"))
        b.set(i, top, z1 + 1, DAK[0], gb.stair("north"))
    for i in range(z0, z1 + 1):
        b.set(x0 - 1, top, i, DAK[0], gb.stair("east"))
        b.set(x1 + 1, top, i, DAK[0], gb.stair("west"))
    for i in range(x0 - 2, x1 + 3):
        b.set(i, top, z0 - 2, DAK[1], gb.slab("bottom"))
        b.set(i, top, z1 + 2, DAK[1], gb.slab("bottom"))
    for i in range(z0 - 1, z1 + 2):
        b.set(x0 - 2, top, i, DAK[1], gb.slab("bottom"))
        b.set(x1 + 2, top, i, DAK[1], gb.slab("bottom"))
    for px, pz in ((x0 - 2, z0 - 2), (x0 - 2, z1 + 2), (x1 + 2, z0 - 2), (x1 + 2, z1 + 2)):
        b.set(px, top, pz, GOUD)
        b.set(px, top + 1, pz, "nether_brick_wall")


def lantaarns(b, k, plekken):
    top = VLOER[k + 1] if k < 4 else DAK_Y
    for x, z in plekken:
        g.hanglamp(b, x, top - 1, z, ketting=2)


def begane_grond(b):
    half, y = HELFT[0], VLOER[0]
    x0, x1, z0, z1 = TX - half, TX + half, TZ - half, TZ + half
    b.fill(TX - 1, y + 1, z1, TX + 1, y + 4, z1, gb.AIR)                           # (the door, to the south)
    b.fill(TX - 2, y + 5, z1, TX + 2, y + 5, z1, GOUD)
    b.fill(TX - 1, y, z0 + 5, TX + 1, y, z1 + 3, "red_terracotta")                 # (the runner, out onto the square)
    for px in (TX - 8, TX + 8):
        for pz in range(z0 + 5, z1 - 2, 6):
            g.pilaar(b, px, y + 1, VLOER[1] - 1, pz, PILAAR, {"axis": "y"})
            b.set(px, y + 1, pz, ZWART)
            b.set(px, VLOER[1] - 1, pz, GOUD)
    lantaarns(b, 0, [(TX - 4, pz) for pz in range(z0 + 8, z1 - 2, 6)] + [(TX + 4, pz) for pz in range(z0 + 8, z1 - 2, 6)]
              + [(TX - 12, TZ - 6), (TX + 12, TZ - 6), (TX - 12, TZ + 6), (TX + 12, TZ + 6)])
    # the altar at the north end: three steps, a slab of gold and an eternal fire that nobody may touch
    b.fill(TX - 3, y + 1, z0 + 3, TX + 3, y + 1, z0 + 4, ZWARTE_STENEN)
    for x in range(TX - 3, TX + 4):
        b.set(x, y + 1, z0 + 5, TRAP, gb.stair("north"))
    b.set(TX, y + 2, z0 + 3, GOUD)
    b.set(TX, y + 3, z0 + 3, "campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for x in (TX - 3, TX + 3):
        b.fill(x, y + 2, z0 + 3, x, y + 3, z0 + 3, WAL)
        b.set(x, y + 4, z0 + 3, "lantern", gb.lamp(False))
    b.sign(TX - 1, y + 2, z0 + 4, "south", ["Eeuwige vlam", "Brandt al sinds", "Roku-guh.", "Niet uitblazen"], wall=False)
    b.sign(TX + 1, y + 2, z0 + 4, "south", ["Offerschaal", "Knabbels mag.", "Marshmallows", "ook, njeg"], wall=False)
    # benches along the walls and a rack of scrolls
    for z in range(z0 + 8, z1 - 6, 3):
        b.set(x0 + 1, y + 1, z, "guhs:guh_bank", {"facing": "east"})
        b.set(x1 - 1, y + 1, z, "guhs:guh_bank", {"facing": "west"})
    b.fill(x0 + 1, y + 1, z0 + 1, x0 + 4, y + 2, z0 + 1, "bookshelf")
    b.plek("vuurwijze_1", TX - 3, y + 1, z1 - 3, yaw=0.0, wie="een Vuurwijze, links van de deur", kind="VUURWIJZE_MIKA")
    b.plek("vuurwijze_2", TX + 3, y + 1, z1 - 3, yaw=0.0, wie="een Vuurwijze, rechts van de deur", kind="VUURWIJZE_MIKA")
    b.plek("shyu", TX + 3, y + 1, z0 + 2, yaw=90.0, wie="de vriendelijke Vuurwijze, bij het luik achter het altaar", kind="VUURWIJZE_SHYU")
    # outside: the square, two fire baskets, the name
    b.sign(TX - 2, y + 3, z1 + 1, "south", ["Vuurtempel van", "Roku-guh", "Voeten vegen:", "as plakt, njeg"], wall=True, glow=True)
    b.sign(TX + 2, y + 3, z1 + 1, "south", ["Open bij", "zonnewende.", "En op dinsdag", "(niet deze)"], wall=True)
    brazier(b, TX - 4, y + 1, z1 + 2)
    brazier(b, TX + 4, y + 1, z1 + 2)


def bibliotheek(b):
    half, y = HELFT[1], VLOER[1]
    x0, x1, z0, z1 = TX - half, TX + half, TZ - half, TZ + half
    for x in range(x0 + 4, x1 - 3, 5):
        for z in (z0 + 3, z0 + 4, z1 - 4, z1 - 3):
            if abs(x - TX) > 1:
                b.fill(x, y + 1, z, x, y + 3, z, "bookshelf")
    b.set(TX, y + 1, TZ - 2, "lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    b.set(TX - 2, y + 1, TZ - 2, "guhs:guh_kristal_lamp")
    b.set(TX + 2, y + 1, TZ - 2, "guhs:guh_kristal_lamp")
    b.sign(TX, y + 1, TZ, "south", ["Bibliotheek", "Alle rollen", "zijn brandveilig", "(nu wel)"], wall=False)
    b.sign(TX - 4, y + 1, z1 - 2, "south", ["Rol 12: wie was", "Mikai's opa?", "(pagina", "weggebrand)"], wall=False)
    lantaarns(b, 1, [(TX - 6, TZ - 6), (TX + 6, TZ - 6), (TX - 6, TZ + 6), (TX + 6, TZ + 6), (TX, TZ + 8), (TX, TZ - 8)])
    b.plek("vuurwijze_3", TX + 2, y + 1, TZ + 3, yaw=0.0, wie="een Vuurwijze tussen de boekenkasten", kind="VUURWIJZE_MIKA")


def stiltezaal(b):
    """Storey 3: mats around a fire bowl set into the floor (the lava lies under glass, in a shell that hangs below)."""
    y = VLOER[2]
    b.fill(TX - 2, y - 2, TZ - 2, TX + 2, y - 1, TZ + 2, ZWART)
    b.fill(TX - 1, y - 1, TZ - 1, TX + 1, y - 1, TZ + 1, "lava", {"level": "0"})
    b.fill(TX - 2, y, TZ - 2, TX + 2, y, TZ + 2, GOUD)
    b.fill(TX - 1, y, TZ - 1, TX + 1, y, TZ + 1, "orange_stained_glass")
    for a in range(0, 360, 45):
        x, z = int(round(TX + 5 * math.cos(math.radians(a)))), int(round(TZ + 5 * math.sin(math.radians(a))))
        b.set(x, y + 1, z, "red_carpet" if a % 90 else "orange_carpet")
    b.sign(TX + 4, y + 1, TZ + 6, "south", ["Stiltezaal", "Adem in.", "Adem vuur uit.", "(buiten graag)"], wall=False)
    lantaarns(b, 2, [(TX - 7, TZ - 7), (TX + 7, TZ - 7), (TX - 7, TZ + 7), (TX + 7, TZ + 7)])
    b.plek("vuurwijze_4", TX - 4, y + 1, TZ + 6, yaw=0.0, wie="een Vuurwijze die doet alsof hij mediteert", kind="VUURWIJZE_MIKA")


def eetzaal(b):
    """Storey 4: the refectory of the Vuurwijzen."""
    half, y = HELFT[3], VLOER[3]
    x0, x1, z0, z1 = TX - half, TX + half, TZ - half, TZ + half
    for x in range(TX - 4, TX + 5, 2):
        b.set(x, y + 1, TZ, "guhs:guh_tafel", {"facing": "north"})
        b.set(x, y + 1, TZ - 1, "guhs:guh_stoel", {"facing": "south"})
        b.set(x, y + 1, TZ + 1, "guhs:guh_stoel", {"facing": "north"})
        if x % 4 == 0:
            b.set(x, y + 2, TZ, "candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    for z in (z0 + 1, z0 + 2):
        b.fill(TX - 3, y + 1, z, TX + 3, y + 1, z, "red_wool" if z == z0 + 1 else "orange_wool")
    b.fill(TX - 2, y + 2, z0 + 1, TX + 2, y + 2, z0 + 1, "red_wool")
    b.sign(TX, y + 1, z0 + 3, "south", ["Vuurvlokken", "Extra heet.", "Blussen met", "kaassaus"], wall=False)
    b.sign(TX + 5, y + 1, TZ + 4, "south", ["Eetzaal", "Wie morst,", "veegt zelf", "de as op"], wall=False)
    lantaarns(b, 3, [(TX - 6, TZ - 6), (TX + 6, TZ - 6), (TX - 6, TZ + 6), (TX + 6, TZ + 6)])


def heiligdom(b):
    """The top storey: the antechamber, the wall with the five fire doors, and behind it the sanctum."""
    half, y = HELFT[4], VLOER[4]
    x0, x1, z0, z1 = TX - half, TX + half, TZ - half, TZ + half
    b.fill(x0 + 1, y + 1, DEUR_Z, x1 - 1, DAK_Y - 1, DEUR_Z, ZWARTE_STENEN)
    for x in range(x0 + 1, x1, 4):
        b.fill(x, y + 1, DEUR_Z, x, DAK_Y - 1, DEUR_Z, ZWART)
        b.set(x, y + 5, DEUR_Z, GOUD)
    for nr, x in enumerate(DEUR_X, start=1):
        # a door is a slab of three by three with its lock in the middle: an eye that lights up when fire is blown in
        b.fill(x - 1, y + 1, DEUR_Z, x + 1, y + 3, DEUR_Z, "red_nether_bricks")
        b.set(x, y + 2, DEUR_Z, "redstone_lamp", {"lit": "false"})
        b.fill(x - 1, y + 4, DEUR_Z, x + 1, y + 4, DEUR_Z, GOUD)
        b.set(x, y + 5, DEUR_Z + 1, "dragon_wall_head", {"facing": "south", "powered": "false"}, {"id": "minecraft:skull"})
        b.prop(f"vuurdeur_{nr}", x, y + 2, DEUR_Z, wie=f"vuurdeur {nr}: het middelste blok is het slot")
    b.sign(DEUR_X[0] - 2, y + 2, DEUR_Z + 1, "south", ["Vijf deuren.", "Vijf vuurstoten", "tegelijk.", ""], wall=True)
    b.sign(DEUR_X[4] + 2, y + 2, DEUR_Z + 1, "south", ["Of klop netjes.", "(werkt niet,", "maar het is", "wel beleefd)"], wall=True)
    for x in (TX - 6, TX + 6):
        brazier(b, x, y + 1, z1 - 2)
    for x in (TX - 7, TX, TX + 7):
        b.set(x, y + 1, TZ + 6, "red_carpet")
    lantaarns(b, 4, [(TX - 5, TZ + 5), (TX + 5, TZ + 5), (TX - 5, TZ - 5), (TX + 5, TZ - 5), (TX, TZ - 2)])
    b.plek("vuurwijze_5", TX + 2, y + 1, TZ + 5, yaw=180.0, wie="de oudste Vuurwijze, op wacht voor de vuurdeuren", kind="VUURWIJZE_MIKA")
    # the sanctum: the statue of Roku-guh against the north wall, and the window the sun comes through
    sx, sz = TX, z0 + 2
    b.fill(sx - 3, y + 1, z0 + 1, sx + 3, y + 1, sz + 1, ZWART)                    # (the plinth)
    b.fill(sx - 2, y + 2, z0 + 1, sx + 2, y + 4, sz, "red_terracotta")             # (the robe)
    b.fill(sx - 1, y + 2, sz + 1, sx + 1, y + 2, sz + 1, "red_terracotta")         # (the knees: he sits)
    b.fill(sx - 1, y + 3, sz, sx + 1, y + 3, sz, "white_wool")                     # (the beard)
    b.fill(sx - 1, y + 4, z0 + 1, sx + 1, y + 6, sz, "pink_concrete")              # (the head of a guh)
    b.set(sx - 1, y + 5, sz, "red_stained_glass")                                  # (the eyes: they wait for the sun)
    b.set(sx + 1, y + 5, sz, "red_stained_glass")
    b.set(sx, y + 4, sz, "white_wool")
    for ex in (sx - 2, sx + 2):                                                    # (the round ears)
        b.fill(ex, y + 6, z0 + 1, ex, y + 7, z0 + 1, "pink_concrete")
    b.set(sx, y + 7, z0 + 1, "gold_block")                                         # (the top knot with the flame pin)
    b.set(sx, y + 7, sz, GOUD)
    b.prop("roku_beeld", sx, y + 1, sz + 1, wie="het beeld van Guhvatar Roku-guh (de voet)", los=True)
    b.plek("roku", sx, y + 1, sz + 5, yaw=0.0, wie="Guhvatar Roku-guh verschijnt hier, voor zijn beeld", kind="ROKU_GUH", los=True)
    b.set(x1, y + 5, sz, "orange_stained_glass")
    b.set(x1, y + 6, sz, GOUD)
    b.set(x1, y + 4, sz, GOUD)
    b.prop("zonnestraal", x1, y + 5, sz, wie="het venster waar bij zonnewende de straal door valt, recht op de ogen van het beeld", los=True)
    for x in (sx - 6, sx + 6):
        b.fill(x, y + 1, z0 + 2, x, y + 2, z0 + 2, WAL)
        b.set(x, y + 3, z0 + 2, "lantern", gb.lamp(False))
    b.set(sx - 8, y + 1, DEUR_Z - 2, "shroomlight")
    b.set(sx + 8, y + 1, DEUR_Z - 2, "shroomlight")
    b.zone("heiligdom", x0 + 1, y + 1, z0 + 1, x1 - 1, DAK_Y - 1, DEUR_Z - 1, wie="het heiligdom achter de vijf vuurdeuren (dicht)")


def trappen(b):
    """The stairs between the storeys, each along a wall, turning back every storey; the last one comes up in the
    antechamber of the top storey."""
    for k, (x, z, richting) in enumerate(((TX + 14, TZ + 4, "north"), (TX - 12, TZ - 4, "south"), (TX + 11, TZ + 4, "north"),
                                           (TX - 10, TZ - 2, "south"))):
        g.binnentrap(b, x, z, richting, VLOER[k], VLOER[k + 1], TRAP, WAL, vul=ZWARTE_STENEN)


def dak(b):
    half = HELFT[4]
    tip = g.piramidedak(b, TX - half - 1, TZ - half - 1, TX + half + 1, TZ + half + 1, DAK_Y, DAK[0], DAK[1], top="gold_block")
    b.set(TX, tip + 1, TZ, GOUD)
    b.set(TX, tip + 2, TZ, "nether_brick_wall")
    g.staaf(b, TX, tip + 3, TZ)


def lavatunnel(b):
    """The secret way of the friendly Vuurwijze: down through an outcrop west of the temple, a tunnel past windows on the
    lava, and a ladder up to a hatch behind the altar."""
    y = G - 8                                        # (the floor layer of the tunnel)
    z = TZ
    # the outcrop over the way in: you walk in from the west and the steps go down to the east
    for x in range(TX - 25, TX - 17):
        for zz in range(z - 3, z + 4):
            hoog = 5 - int(round(math.hypot((x - (TX - 22)) * 0.8, zz - z)))
            if hoog > 0 and x > TX - 25:
                b.fill(x, G + 1, zz, x, G + hoog, zz, ROTS)
    g.trap(b, TX - 18, y + 1, z, "west", 8, TRAP, vrij=3)
    b.set(TX - 26, G + 1, z - 1, "lantern", gb.lamp(False))
    b.sign(TX - 26, G + 1, z + 1, "west", ["Hier is niks.", "Echt niet.", "Doorlopen,", "njeg"], wall=False)
    # the tunnel east, then north under the hall; the floor is smooth stone
    b.fill(TX - 17, y + 1, z - 1, TX + 1, y + 3, z + 1, gb.AIR)
    b.fill(TX - 17, y, z - 1, TX + 1, y, z + 1, ZWART)
    b.fill(TX - 1, y + 1, TZ - 14, TX + 1, y + 3, z + 1, gb.AIR)
    b.fill(TX - 1, y, TZ - 14, TX + 1, y, z + 1, ZWART)
    for x in range(TX - 13, TX - 2, 4):               # (windows on the lava: glass, lava behind it, rock all around)
        for s in (-1, 1):
            b.set(x, y + 2, z + s * 2, "orange_stained_glass")
            b.set(x, y + 2, z + s * 3, "lava", {"level": "0"})
    b.sign(TX - 15, y + 1, z + 1, "east", ["Geheime gang", "Niet verder", "vertellen.", "Dit bordje ook"], wall=False)
    b.sign(TX + 1, y + 1, TZ - 13, "south", ["Ladder naar het", "altaar. Sssst:", "boven wordt", "gemediteerd"], wall=False)
    g.ladder(b, TX, y + 1, G - 1, TZ - 15, "south")


def vulkaan(b, top):
    """The little volcano on the east horn: the Vuurwijzen put a lid on it after someone dropped his knabbels in."""
    cx, cz = VULKAAN
    for x in range(cx - 10, cx + 11):
        for z in range(cz - 10, cz + 11):
            d = math.hypot(x - cx, z - cz)
            if (x, z) not in top or d > 9.5:
                continue
            hoog = int(round(9 * (1 - d / 9.5))) if d > 3.2 else 6
            for y in range(G + 1, G + 1 + hoog):
                b.set(x, y, z, "basalt" if (x * 3 + z * 5 + y) % 4 == 0 else ROTS)
            if d <= 2.4:
                b.set(x, G + 5, z, "lava", {"level": "0"})
                b.set(x, G + 6, z, "orange_stained_glass")
                b.set(x, G + 4, z, ROTS)
    b.sign(cx - 11, G + 1, cz - 2, "west", ["Vulkaan", "Deksel erop.", "(er viel steeds", "een knabbel in)"], wall=False)


def eendenvijver(b, top):
    """A pond for the schildpad-eendguhs, east of the temple: a closed basin level with the ground."""
    x0, x1, z0, z1 = TX + 25, TX + 30, TZ + 12, TZ + 16
    if any((x, z) not in top for x in range(x0 - 2, x1 + 3) for z in range(z0 - 2, z1 + 3)):
        raise SystemExit(f"{NAAM}: the pond does not lie on the island")
    b.fill(x0 - 1, G, z0 - 1, x1 + 1, G, z1 + 1, ZWART)
    b.fill(x0 - 1, G - 1, z0 - 1, x1 + 1, G - 1, z1 + 1, ROTS)
    b.fill(x0, G, z0, x1, G, z1, "water", {"level": "0"})
    b.set(x0 + 1, G, z1, BLOK)                       # (a stone to sit on, with a light rod)
    g.staaf(b, x0 + 1, G + 1, z1)
    b.set(x1, G + 1, z0 - 1, "lantern", gb.lamp(False))
    b.sign(x0 - 2, G + 1, z1 + 2, "south", ["Eendguhvijver", "Brood mag.", "Vuurvlokken", "niet. Echt niet"], wall=False)
    b.wezen("eendguh_1", x0 - 2, G + 1, z0, "guhs:schildpad_eendguh", yaw=-90.0, honk=6)
    b.wezen("eendguh_2", x1 + 2, G + 1, z1, "guhs:schildpad_eendguh", yaw=90.0, honk=6)


def versiering(b, top):
    rng = b.rng
    bezet = set()
    for (x, z) in top:
        if b.s.blocks[(x, G, z)][0] != gb.mc(GROND):
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    bezet.add((x + dx, z + dz))
    for (x, z) in sorted(top):
        if (x, z) in bezet or (x, G + 1, z) in b.s.blocks:
            continue
        if abs(x - TX) < 24 and abs(z - TZ) < 24 or (LANDING[0] - 2 <= x <= LANDING[1] + 2 and LANDING[2] - 2 <= z <= LANDING[3] + 2):
            continue
        r = rng.random()
        if r < 0.022:
            g.koorplant(b, x, G + 1, z, rng.randrange(3, 8))
            bezet.update((x + dx, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1))
        elif r < 0.03:
            g.staaf(b, x, G + 1, z)
    # glowing drops under the island
    for (x, z) in sorted(top):
        o = G
        while (x, o - 1, z) in b.s.blocks:
            o -= 1
        if o > 1 and rng.random() < 0.012:
            b.set(x, o - 1, z, "shroomlight")


def bouw(h):
    b = kit.Bouw(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"], prefix=g.PREFIX)
    top = g.eiland(b, vorm(), GROND, rots("vuurtempel_rots"), kern=ROTS)
    lavatunnel(b)
    for k in range(5):
        verdieping(b, k)
    begane_grond(b)
    bibliotheek(b)
    stiltezaal(b)
    eetzaal(b)
    heiligdom(b)
    trappen(b)
    dak(b)
    # the hatch behind the altar (the tunnel's ladder comes up here)
    b.set(TX, G, TZ - 15, "ladder", {"facing": "south", "waterlogged": "false"})
    # the square, the paths and the gates
    lx, lz = (LANDING[0] + LANDING[1]) // 2, (LANDING[2] + LANDING[3]) // 2
    for x in range(TX - 6, TX + 7):
        for z in range(TZ + 18, TZ + 24):
            if (x, z) in top:
                b.set(x, G, z, BLOK if (x + z) % 2 else ZWART)
    pad(b, top, [(lx, lz - 4), (lx + 6, lz - 14), (TX - 16, TZ + 21), (TX - 6, TZ + 21)])
    pad(b, top, [(TX + 6, TZ + 21), (TX + 24, TZ + 23), (VULKAAN[0] - 8, VULKAAN[1] - 6)])
    pad(b, top, [(TX - 16, TZ + 21), (TX - 24, TZ + 12), (TX - 27, TZ + 3), (TX - 27, TZ)])
    for x in range(LANDING[0], LANDING[1] + 1):
        for z in range(LANDING[2], LANDING[3] + 1):
            b.set(x, G, z, BLOK if (x in (LANDING[0], LANDING[1]) or z in (LANDING[2], LANDING[3])) else ZWART)
    poort(b, lx + 5, lz - 12, "x")
    poort(b, TX - 12, TZ + 21, "z")
    for x in (LANDING[0] - 1, LANDING[1] + 1):
        for z in (LANDING[2] - 1, LANDING[3] + 1):
            brazier(b, x, G + 1, z)
    b.sign(lx + 3, G + 1, LANDING[2] - 1, "south", ["Halvemaaneiland", "Tempel: het pad.", "Vulkaan: niet", "aanraken"], wall=False)
    vulkaan(b, top)
    eendenvijver(b, top)
    versiering(b, top)
    b.zone("aankomst", LANDING[0], G + 1, LANDING[2], LANDING[1], G + 6, LANDING[3], wie="de landingsplek op de westelijke hoorn")
    b.prop("aankomst_speler", lx, G + 1, lz)
    b.prop("uitzicht", TX, G + 22, D + 30, wie="van hier zie je de halve maan met de toren in het midden")
    return (b,)


def verlicht(b, start):
    return g.verlicht(b, start, "shroomlight", {gb.mc(GROND), gb.mc(BLOK), "minecraft:blackstone", "minecraft:polished_blackstone", "minecraft:basalt",
                                             "minecraft:polished_blackstone_bricks"},
                      min_licht=7, raster=7)


def check(b):
    start = tuple(b.plekken["aankomst_speler"]["pos"])
    verwacht = [("vuurwijze", 5), "shyu", ("vuurdeur", 5), "heiligdom", "roku_beeld", "roku", "zonnestraal", ("eendguh", 2)]
    problems = g.eerlijk(b) + b.check(start, verwacht, min_licht=5)
    # the sanctum must really be shut: nobody walks in before the five doors open
    reach = kit.Loper(b).bfs(start)
    x0, y0, z0 = b.plekken["heiligdom"]["pos"]
    x1, y1, z1 = b.plekken["heiligdom"]["tot"]
    binnen = [c for c in reach if x0 <= c[0] <= x1 and y0 <= c[1] <= y1 and z0 <= c[2] <= z1]
    if binnen:
        problems.append(f"het heiligdom is open: je kunt naar {binnen[:3]}")
    # and lava never touches air (it would flow, or set the knabbelkoor on fire)
    for c, bl in b.s.blocks.items():
        if bl[0] == "minecraft:lava":
            for dx, dy, dz in g.N6:
                n = b.s.blocks.get((c[0] + dx, c[1] + dy, c[2] + dz))
                if n is None or n[0] == gb.AIR or b.is_dun(n[0]):
                    problems.append(f"lava op {c} ligt open")
                    break
    return problems


def build(h):
    (b,) = bouw(h)
    start = tuple(b.plekken["aankomst_speler"]["pos"])
    b.connect()
    g.trapvormen(b)
    lampen = verlicht(b, start)
    problems = check(b)
    if problems:
        raise SystemExit(f"{NAAM}: self-check found {len(problems)} problem(s):\n  " + "\n  ".join(problems[:40]))
    b.save_tiles()
    kit.leegte_structuur(h, b, titel="Vuurtempel van Roku-guh", tooltip="Een rode pagode op een halvemaaneiland, met vijf vuurdeuren die dicht zijn", y=Y)
    print(f"{NAAM}: self-check ok {b.stats}, {lampen} lampen gelegd")
    return b
