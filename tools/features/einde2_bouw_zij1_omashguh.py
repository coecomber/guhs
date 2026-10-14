"""
einde2 (bouw_zij1): Omashguh, the mountain town of the Kristalkaasklippen (a guhs:leegte_bouw; the build only: the side
quest, koning Bumi-guh and the townsfolk are a later phase and stand as markers in the sidecar).

A floating slab of klipkaas with a walled town of three tiers on it, a stepped pyramid: the lower town inside the wall,
the terrace of the second tier, and on top the palace of koning Bumi-guh. The mail does not walk here: from the platform in
front of the palace three post chutes (guhs:einde2_bouw_zij1_postgoot) run down to the post office, two around the
tiers (linksom and rechtsom) and one straight on into the kurkentrekker, the spiral tower on the post square.
Inside the third tier the three trial rooms, inside the second the post office and the burrow of the das-molguhs.

Everything is laid out from the middle C: T2 and T3 are the half sizes of the tiers, MUUR that of the town wall.
"""
import features.einde2_kit as kit
from features import guheinde_bouw as gb
from features import einde2_bouw_zij1_gereedschap as g

NAAM = "omashguh"
W, H, D = 136, 88, 144
Y = 142                       # the world y of template y 0
C = 68                        # the middle (x and z)
G = 24                        # the top block layer of the island (you stand at G + 1)
T2, T3, PALEIS, MUUR = 30, 17, 10, 52
Y2, Y3 = 38, 52               # the top block layers of the second and the third tier
STEEN, STENEN, TRAP, PLAAT, WAL = "guhs:klipkaas", "guhs:klipkaas_stenen", "guhs:klipkaas_stenen_trap", "guhs:klipkaas_stenen_plaat", \
    "guhs:klipkaas_stenen_muur"
DAK = ("dark_prismarine_stairs", "dark_prismarine_slab")
HUISMUUR = ["smooth_sandstone", "cut_sandstone", "guhs:klipkaas_stenen", "yellow_terracotta"]
GOOT = g.GOOT

# the three chutes: corner points (x, z, y of the chute block); between two points the chute drops evenly
LINKSOM = [(C - 4, C + 16, Y3), (C - 4, C + 19, Y3), (C - 19, C + 19, 47), (C - 19, C + 8, 42), (C - 32, C + 8, 42), (C - 32, C + 32, 31),
           (C - 2, C + 32, G)]
RECHTSOM = [(2 * C - x, z, y) for x, z, y in LINKSOM]
TOREN = (C, C + 41)           # the middle of the spiral tower
RING = [(C - 2, C + 39), (C - 2, C + 43), (C + 2, C + 43), (C + 2, C + 39), (C, C + 39)]


def vorm():
    rand, onder = g.ruis("omashguh_rand", 11.0), g.ruis("omashguh_onder", 7.0)

    def v(x, z):
        d = ((abs(x - C) / 66.0) ** 4 + (abs(z - C) / 66.0) ** 4) ** 0.25 + rand(x, z) * 0.03
        dl = ((x - C) ** 2 + (z - (C + 61)) ** 2) ** 0.5 / (12.5 + rand(x + 40, z) * 1.2)
        diep = 0.0
        if d < 1.0:
            diep = 3.0 + 20.0 * (1.0 - d * d) ** 0.7
        if dl < 1.0:
            diep = max(diep, 3.0 + 6.0 * (1.0 - dl * dl))
        if diep <= 0.0:
            return None
        return G, int(round(G - diep - onder(x, z) * 1.6))
    return v


def rots(erts):
    def f(x, y, z):
        return "guhs:kristalkaas_erts" if erts(x + y * 3, z - y * 2) > 0.86 else STEEN
    return f


def blok(b, half, y0, y1):
    """A tier: a square block of klipkaas with a face of klipkaasstenen (a band of cut sandstone every fifth layer)."""
    x0, x1 = C - half, C + half
    b.fill(x0, y0, x0, x1, y1, x1, STEEN)
    for y in range(y0, y1 + 1):
        g.doos(b, x0, y, x0, x1, y, x1, "cut_sandstone" if (y - y0) % 5 == 4 else STENEN)
    b.fill(x0, y1, x0, x1, y1, x1, STENEN)
    for i in range(x0 + 2, x1 - 1, 4):               # (a row of tiles along the edge)
        for a, c in ((i, x0 + 2), (i, x1 - 2), (x0 + 2, i), (x1 - 2, i)):
            b.set(a, y1, c, "cut_sandstone")


def borstwering(b, half, y, open_=()):
    """The parapet on the edge of a tier (open_ = cells that stay free)."""
    x0, x1 = C - half, C + half
    for i in range(x0, x1 + 1):
        for c in ((i, x0), (i, x1), (x0, i), (x1, i)):
            if c not in open_:
                b.set(c[0], y, c[1], WAL)


def stadsmuur(b):
    lo, hi = C - MUUR, C + MUUR
    for i in range(lo, hi + 1):
        for a, c in ((i, lo), (i, lo + 1), (i, hi), (i, hi - 1), (lo, i), (lo + 1, i), (hi, i), (hi - 1, i)):
            for y in range(G + 1, G + 7):
                b.set(a, y, c, "cut_sandstone" if y == G + 4 else STENEN)
    for i in range(lo, hi + 1, 2):                   # (battlements)
        for a, c in ((i, lo), (i, hi), (lo, i), (hi, i)):
            b.set(a, G + 7, c, WAL)
    for tx, tz in ((lo, lo), (lo, hi), (hi, lo), (hi, hi)):          # (corner towers with a green cap)
        x0, z0 = min(max(tx - 2, lo - 1), hi - 4), min(max(tz - 2, lo - 1), hi - 4)
        b.fill(x0, G + 1, z0, x0 + 5, G + 10, z0 + 5, STENEN)
        g.piramidedak(b, x0 - 1, z0 - 1, x0 + 6, z0 + 6, G + 10, *DAK, top="guhs:kristalkaas_blok")
    # the gate in the south wall: three thick slabs of stone that stand open (in the series earthbenders push them aside)
    b.fill(C - 3, G + 1, hi - 1, C + 3, G + 5, hi, gb.AIR)
    for x in (C - 5, C - 4, C + 4, C + 5):
        b.fill(x, G + 1, hi - 2, x, G + 9, hi + 1, STENEN)
    b.fill(C - 5, G + 6, hi - 1, C + 5, G + 9, hi, STENEN)
    b.fill(C - 3, G + 6, hi - 1, C + 3, G + 6, hi, "cut_sandstone")
    for x in range(C - 5, C + 6, 2):
        b.set(x, G + 10, hi, WAL)
    b.set(C - 3, G + 5, hi + 1, "lantern", gb.lamp(True))
    b.set(C + 3, G + 5, hi + 1, "lantern", gb.lamp(True))
    b.fill(C - 3, G + 6, hi + 1, C + 3, G + 6, hi + 1, PLAAT, gb.slab("top"))
    b.sign(C - 4, G + 3, hi + 2, "south", ["~ Omashguh ~", "Welkom, njeg!", "De post glijdt", "hier vanzelf"], glow=True)
    b.sign(C + 4, G + 3, hi + 2, "south", ["Koning:", "Bumi-guh", "(112 jaar,", "nog niet moe)"])


def goot(b, punten, lus=None):
    """Lays a chute along corner points (x, z, y): the chute blocks (each pushes on to the next), a trough around them
    (a kerb and a wall, so nobody slides out; a full block in the bends), three free blocks above and a post under it every fourth block.
    lus = extra (x, z) corners that are walked `rondes` times while the chute drops to the y after it (the spiral)."""
    cellen = []
    for (x0, z0, y0), (x1, z1, y1) in zip(punten, punten[1:]):
        n = abs(x1 - x0) + abs(z1 - z0)
        assert n and (x0 == x1 or z0 == z1), f"a chute runs straight: {(x0, z0)} -> {(x1, z1)}"
        sx, sz = (x1 > x0) - (x1 < x0), (z1 > z0) - (z1 < z0)
        for i in range(n):
            cellen.append((x0 + sx * i, y0 - int((y0 - y1) * i / n + 0.5), z0 + sz * i))
    cellen.append((punten[-1][0], punten[-1][2], punten[-1][1]))
    eigen = set((x, y, z) for x, y, z in cellen)
    for i, (x, y, z) in enumerate(cellen[:-1]):
        nx, ny, nz = cellen[i + 1]
        assert ny in (y, y - 1), f"chute drops more than one at {(x, y, z)}"
        facing = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(nx - x, nz - z)]
        b.set(x, y, z, GOOT, {"facing": facing})
        for d in (1, 2, 3):
            b.set(x, y + d, z, gb.AIR)
    # around a bend the trough gets a FULL block instead of the thin wall: a rider swings wide in a bend, and past the
    # thin wall (narrower than a block) they would end up over the kerb, off the chute, and stick there
    bocht = set()
    for i in range(1, len(cellen) - 1):
        (ax, _ay, az), (x, _y, z), (bx, _by, bz) = cellen[i - 1], cellen[i], cellen[i + 1]
        if (x - ax, z - az) != (bx - x, bz - z):
            bocht.update(range(i - 1, i + 2))
    for i, (x, y, z) in enumerate(cellen[:-1]):
        if i < 2:
            continue                                 # (the first blocks lie in the platform: you step in from the side)
        laatste = i >= len(cellen) - 3
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                c = (x + dx, z + dz)
                if (dx, dz) == (0, 0) or any((c[0], yy, c[1]) in eigen for yy in range(y - 2, y + 3)):
                    continue
                if laatste and (c[0] - x, c[1] - z) == (cellen[i + 1][0] - x, cellen[i + 1][2] - z):
                    continue
                if b.s.blocks.get((c[0], y, c[1]), (gb.AIR,))[0] == gb.AIR:
                    b.set(c[0], y, c[1], STENEN)
                boven = b.s.blocks.get((c[0], y + 1, c[1]), (gb.AIR,))[0]
                if boven == gb.AIR or boven == WAL:
                    b.set(c[0], y + 1, c[1], STENEN if i in bocht else WAL)
        if i % 4 == 2:
            yy = y - 1
            while yy > 0 and (x, yy, z) not in b.s.blocks:
                yy -= 1
            if yy > 0:
                for q in range(yy + 1, y):
                    b.set(x, q, z, WAL)
    return cellen


def kurkentrekker(b):
    """The spiral tower on the post square: the straight chute from the palace comes in at the top, goes three times
    around the stone core and slides out at the bottom, in front of the post office."""
    cx, cz = TOREN
    b.fill(cx - 1, G + 1, cz - 1, cx + 1, 56, cz + 1, STENEN)
    for y in range(G + 2, 56, 4):
        b.fill(cx - 1, y, cz - 1, cx + 1, y, cz + 1, "cut_sandstone")
    for px, pz in ((cx - 3, cz - 3), (cx + 3, cz - 3), (cx - 3, cz + 3), (cx + 3, cz + 3)):
        b.fill(px, G + 1, pz, px, 56, pz, STENEN)
    punten = [(C, C + 16, Y3), (C, C + 19, Y3), (C, C + 39, 48)]
    y = 48
    for _ronde in range(3):
        for i, (x, z) in enumerate(RING):
            y -= (1, 2, 2, 2, 1)[i]
            punten.append((x, z, y))
    assert y == G, y
    punten.append((C, C + 36, G))
    cellen = goot(b, punten)
    b.fill(cx - 3, 56, cz - 3, cx + 3, 56, cz + 3, PLAAT, gb.slab("bottom"))
    tip = g.piramidedak(b, cx - 4, cz - 4, cx + 4, cz + 4, 56, *DAK, top="guhs:kristalkaas_blok")
    g.kristal(b, cx, tip + 1, cz)
    return cellen


def paleis(b):
    """The palace of koning Bumi-guh on the third tier: the throne hall, a lantern storey on top, two green roofs."""
    x0, x1 = C - PALEIS, C + PALEIS
    g.kamer(b, x0, Y3, x0, x1, Y3 + 10, x1, STENEN, vloer="polished_andesite", plafond=STENEN)
    for i in range(x0 + 1, x1):
        for j in range(x0 + 1, x1):
            if (i + j) % 2 == 0:
                b.set(i, Y3, j, "cut_sandstone")
    b.fill(C - 1, Y3, x0 + 4, C + 1, Y3, x1 - 1, "green_terracotta")              # (the green runner to the throne)
    for y in (Y3 + 5, Y3 + 9):
        g.doos(b, x0, y, x0, x1, y, x1, "cut_sandstone")
    # the door (south) and windows of yellow glass
    b.fill(C - 1, Y3 + 1, x1, C + 1, Y3 + 4, x1, gb.AIR)
    b.fill(C - 2, Y3 + 5, x1, C + 2, Y3 + 5, x1, "chiseled_sandstone")
    for i in (C - 6, C - 5, C + 5, C + 6):
        for kant in (x0, x1):
            b.fill(i, Y3 + 2, kant, i, Y3 + 4, kant, "yellow_stained_glass_pane")
            b.fill(kant, Y3 + 2, i, kant, Y3 + 4, i, "yellow_stained_glass_pane")
    # pillars with lanterns, the dais and the throne
    for px in (C - 5, C + 5):
        for pz in (C - 5, C, C + 5):
            g.pilaar(b, px, Y3 + 1, Y3 + 9, pz, STENEN)
            b.set(px, Y3 + 1, pz, "chiseled_sandstone")
            b.set(px + (1 if px < C else -1), Y3 + 6, pz, "lantern", gb.lamp(True))
            b.set(px + (1 if px < C else -1), Y3 + 7, pz, PLAAT, gb.slab("top"))
    b.fill(C - 4, Y3 + 1, x0 + 1, C + 4, Y3 + 1, x0 + 4, "polished_andesite")
    for x in range(C - 4, C + 5):
        b.set(x, Y3 + 1, x0 + 5, TRAP, gb.stair("north"))
    b.fill(C - 1, Y3 + 1, x0 + 1, C + 1, Y3 + 1, x0 + 4, "green_terracotta")
    # (a plain throne, not a "royal" one: that kind seats a new wild Koningguh on itself every day, and this is Bumi-guh's)
    b.set(C, Y3 + 2, x0 + 1, "guhs:koningstroon", {"facing": "south", "royal": "false"})
    for x in (C - 2, C + 2):
        b.fill(x, Y3 + 2, x0 + 1, x, Y3 + 4, x0 + 1, "guhs:kristalkaas_blok")
        g.kristal(b, x, Y3 + 5, x0 + 1)
    b.sign(C - 1, Y3 + 4, x0 + 1, "south", ["Koning Bumi-guh", "Hier zit de", "gekste guh van", "Omashguh"])
    b.sign(C + 1, Y3 + 4, x0 + 1, "south", ["Regel 1: de", "koning heeft", "gelijk. Regel 2:", "kiekeboe!"])
    b.plek("bumi", C, Y3 + 2, x0 + 3, yaw=0.0, wie="koning Bumi-guh, voor zijn troon", kind="BUMI_GUH")
    # Flopsie's corner: a bowl as big as a bath and a pile of hay
    b.fill(C + 6, Y3 + 1, x0 + 1, C + 9, Y3 + 1, x0 + 2, "hay_block", {"axis": "y"})
    b.set(C + 9, Y3 + 2, x0 + 1, "hay_block", {"axis": "y"})
    b.set(C + 6, Y3 + 1, x0 + 4, "guhs:guh_voerbak", {"facing": "south", "voer": "4"})
    b.sign(C + 9, Y3 + 3, x0 + 1, "west", ["Flopsie", "Niet voeren.", "Hij heeft al", "een heel schaap"], wall=True)
    b.set(C + 8, Y3 + 9, x0 + 3, PLAAT, gb.slab("top"))
    b.set(C + 8, Y3 + 8, x0 + 3, "lantern", gb.lamp(True))
    b.plek("flopsie", C + 7, Y3 + 1, x0 + 4, yaw=20.0, wie="Flopsie-guh, het veel te grote huisdier van de koning", kind="FLOPSIE")
    # the kristalsnoep: the reward of the side quest lies on a pedestal of kristalkaas
    b.set(C - 7, Y3 + 1, x0 + 3, "guhs:kristalkaas_blok")
    g.kristal(b, C - 7, Y3 + 2, x0 + 3)
    b.sign(C - 7, Y3 + 1, x0 + 4, "south", ["Kristalsnoep", "Lekker.", "Groeit door.", "Opeten dus"], wall=True)
    b.prop("kristalsnoep", C - 7, Y3 + 1, x0 + 3, wie="de sokkel van de kristalsnoep-ring")
    b.zone("troonzaal", x0 + 1, Y3 + 1, x0 + 1, x1 - 1, Y3 + 9, x1 - 1, wie="de troonzaal")
    # the roof skirt, the lantern storey and its roof
    for x in range(x0 - 1, x1 + 2):
        b.set(x, Y3 + 10, x0 - 1, DAK[0], gb.stair("south"))
        b.set(x, Y3 + 10, x1 + 1, DAK[0], gb.stair("north"))
    for z in range(x0, x1 + 1):
        b.set(x0 - 1, Y3 + 10, z, DAK[0], gb.stair("east"))
        b.set(x1 + 1, Y3 + 10, z, DAK[0], gb.stair("west"))
    b.fill(x0, Y3 + 11, x0, x1, Y3 + 11, x1, "dark_prismarine")
    u0, u1 = C - 6, C + 6
    g.kamer(b, u0, Y3 + 11, u0, u1, Y3 + 18, u1, STENEN, vloer="dark_prismarine")
    for i in range(u0 + 2, u1 - 1):
        for kant in (u0, u1):
            b.fill(i, Y3 + 13, kant, i, Y3 + 16, kant, "yellow_stained_glass")
            b.fill(kant, Y3 + 13, i, kant, Y3 + 16, i, "yellow_stained_glass")
    b.fill(C - 1, Y3 + 12, C - 1, C + 1, Y3 + 14, C + 1, "guhs:kristalkaas_blok")       # (the light in the lantern)
    tip = g.piramidedak(b, u0 - 1, u0 - 1, u1 + 1, u1 + 1, Y3 + 18, *DAK, top="guhs:kristalkaas_blok")
    b.set(C, tip + 1, C, "guhs:kristalkaas_blok")
    g.kristal(b, C, tip + 2, C)


def platform(b):
    """The top of the third tier around the palace: the platform where the mail (and whoever dares) goes into the chutes."""
    borstwering(b, T3, Y3 + 1, open_={(C - 4, C + T3), (C, C + T3), (C + 4, C + T3), (C + 6, C - T3)})
    for x in (C - 6, C - 2, C + 2, C + 6):
        b.set(x, Y3 + 1, C + T3 - 4, "guhs:kristalkaas_blok")
        g.kristal(b, x, Y3 + 2, C + T3 - 4)
    b.sign(C - 2, Y3 + 1, C + T3 - 3, "south", ["Postglijbaan", "Alleen pakjes", "(en guhs die", "durven)"], wall=True)
    b.sign(C + 2, Y3 + 1, C + T3 - 3, "south", ["Bukken = remmen", "Springen = dom", "Gillen = mag", "Njeeeeg!"], wall=True)
    b.prop("glijbaan_start", C, Y3 + 1, C + T3 - 2, wie="het laadplatform: hier beginnen de drie postglijbanen")


def trappen(b):
    """The two wide stairs on the north side: town -> terrace, terrace -> palace."""
    for x0, y0, half, top in ((C - 13, G + 1, T2, Y2), (C - 7, Y2 + 1, T3, Y3)):
        z = C - half - 2
        g.trap(b, x0, y0, z, "east", 14, TRAP, breed=2, vul=STENEN)
        for i in range(14):                          # (a kerb with a rail on the open side)
            b.fill(x0 + i, y0, z - 1, x0 + i, y0 + i, z - 1, STENEN)
            b.set(x0 + i, y0 + i + 1, z - 1, WAL)
        b.fill(x0 + 14, y0, z - 1, x0 + 14, top, z + 1, STENEN)                    # (the pier the stair ends against)
        b.fill(x0 + 14, top + 1, z - 1, x0 + 14, top + 1, z + 1, WAL)
    b.sign(C - 15, G + 1, C - T2 - 2, "west", ["Naar boven:", "het terras, de", "proeven en de", "koning"], wall=False)


def proeven(b):
    """The three trial rooms inside the third tier (none of them is what it looks like)."""
    y0, y1 = Y2, Y2 + 12
    # --- proef 1 (west): the key in the waterfall. The waterfall is glass and ice: you cannot swim up a painting -----
    x0, x1, z0, z1 = C - 16, C - 6, C - 12, C + 12
    g.kamer(b, x0, y0, z0, x1, y1, z1, STEEN, vloer=STENEN, plafond=STEEN)
    b.fill(x0 - 1, y0 + 1, C, x0, y0 + 3, C + 1, gb.AIR)                           # (the doorway in the west face)
    b.fill(x0 + 2, y0, z0 + 2, x1 - 2, y0, z0 + 6, "water", {"level": "0"})        # (the pool: a closed basin)
    b.fill(x0 + 2, y0 - 1, z0 + 2, x1 - 2, y0 - 1, z0 + 6, "prismarine")
    for y in range(y0 + 1, y1):
        for x in range(x0 + 3, x1 - 2):
            b.set(x, y, z0 + 1, "cyan_stained_glass" if (x + y) % 3 else "blue_ice")
            if y > y0 + 2 and (x * 7 + y * 3) % 5 == 0:
                b.set(x, y, z0 + 2, "white_stained_glass")
    b.set(C - 11, y1 - 3, z0 + 2, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(C - 11, y1 - 2, z0 + 2, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(C - 11, y1 - 1, z0 + 2, "chain", {"axis": "y", "waterlogged": "false"})
    b.set(C - 11, y1 - 4, z0 + 2, "gold_block")                                    # (the key box, high in the fall)
    for i, (sx, sz) in enumerate(((x0 + 2, z0 + 8), (x0 + 3, z0 + 9), (x1 - 2, z0 + 8), (x1 - 3, z0 + 10))):
        b.fill(sx, y0 + 1, sz, sx, y0 + 1 + i % 3, sz, STEEN)                       # (stalagmites: steps that lead nowhere)
        g.kristal(b, sx, y0 + 2 + i % 3, sz)
    for z in (C - 2, C + 6):
        for x in (x0 + 1, x1 - 1):
            b.set(x, y0 + 4, z, "lantern", gb.lamp(True))
            b.set(x, y0 + 5, z, PLAAT, gb.slab("top"))
    b.sign(x0 - 2, y0 + 4, C - 1, "west", ["Proef 1", "De sleutel", "hangt in de", "waterval"], wall=True)
    b.sign(x1 - 1, y0 + 2, C + 8, "west", ["Tip van Bumi:", "het is niet", "wat je denkt.", "Nooit. Njeg."], wall=True)
    b.zone("proef_1", x0 + 1, y0 + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1, wie="proef 1: de sleutel in de waterval")
    b.prop("proef_1_start", x0 + 2, y0 + 1, C, wie="het begin van proef 1")
    # --- proef 2 (east): find Flopsie. A hutch for a tiny pet, and paw prints that are far too big --------------------
    x0, x1 = C + 6, C + 16
    g.kamer(b, x0, y0, z0, x1, y1, z1, STEEN, vloer="moss_block", plafond=STEEN)
    b.fill(x1, y0 + 1, C, x1 + 1, y0 + 3, C + 1, gb.AIR)
    for z in range(z0 + 3, z1 - 2, 5):                                           # (the paw prints: three toes and a pad)
        x = x0 + 4 + (z // 5) % 2 * 2
        b.fill(x, y0, z, x + 1, y0, z + 1, "coarse_dirt")
        for t in (-1, 1, 3):
            b.set(x + (t + 1) // 2, y0, z - 2, "coarse_dirt")
    b.fill(x0 + 1, y0 + 1, z0 + 1, x0 + 3, y0 + 1, z0 + 3, "spruce_fence")
    b.fill(x0 + 1, y0 + 1, z0 + 1, x0 + 2, y0 + 1, z0 + 2, "hay_block", {"axis": "y"})
    b.set(x0 + 2, y0 + 2, z0 + 1, "guhs:guh_voerbak", {"facing": "south", "voer": "1"})
    b.sign(x0 + 3, y0 + 2, z0 + 3, "south", ["Flopsie", "klein, lief,", "past in dit", "hokje (echt)"], wall=False)
    b.fill(x1 - 3, y0 + 1, z1 - 3, x1 - 1, y0 + 2, z1 - 1, "hay_block", {"axis": "y"})
    b.set(x1 - 2, y0 + 3, z1 - 2, "hay_block", {"axis": "y"})
    b.fill(x0 + 4, y0 + 1, z1 - 1, x0 + 6, y0 + 1, z1 - 1, "yellow_wool")          # (a collar, for a neck like a tree)
    b.set(x0 + 5, y0 + 2, z1 - 1, "gold_block")
    for z in (C - 6, C + 6):
        for x in (x0 + 1, x1 - 1):
            b.set(x, y0 + 4, z, "lantern", gb.lamp(True))
            b.set(x, y0 + 5, z, PLAAT, gb.slab("top"))
    b.sign(x1 + 2, y0 + 4, C - 1, "east", ["Proef 2", "Flopsie zoek!", "Hij is KLEIN", "en schattig"], wall=True)
    b.zone("proef_2", x0 + 1, y0 + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1, wie="proef 2: Flopsie zoek")
    b.prop("proef_2_start", x1 - 2, y0 + 1, C, wie="het begin van proef 2")
    # --- proef 3 (middle, the door in the south face): choose your opponent. Three pedestals, and the right answer is
    #     the old guh with the silly hat ------------------------------------------------------------------------------
    x0, x1, z0, z1 = C - 5, C + 5, C - 5, C + 16
    g.kamer(b, x0, y0, z0, x1, y1, z1, STEEN, vloer=STENEN, plafond=STEEN)
    b.fill(C - 1, y0 + 1, z1, C + 1, y0 + 3, z1 + 1, gb.AIR)
    for x in range(x0 + 2, x1 - 1):
        for z in range(z0 + 6, z0 + 13):
            if 2.2 < ((x - C) ** 2 + (z - (z0 + 9)) ** 2) ** 0.5 < 3.6:
                b.set(x, y0, z, "red_sandstone")
    for i, x in enumerate((C - 3, C, C + 3)):
        b.set(x, y0 + 1, z0 + 2, "chiseled_sandstone")
        b.set(x, y0 + 2, z0 + 1, "lantern", gb.lamp(False))
        b.set(x, y0 + 1, z0 + 1, STENEN)
    b.sign(C - 3, y0 + 1, z0 + 3, "south", ["De Enge", "bijl: groot", "humeur: slecht", ""], wall=True)
    b.sign(C, y0 + 1, z0 + 3, "south", ["De Hele Enge", "bijl: groter", "humeur: weg", ""], wall=True)
    b.sign(C + 3, y0 + 1, z0 + 3, "south", ["De Oude", "hoed: gek", "kracht: ???", "(kies die niet)"], wall=True)
    for x in (x0 + 1, x1 - 1):
        b.set(x, y0 + 1, z0 + 9, "grindstone", {"face": "floor", "facing": "north"})
        b.set(x, y0 + 4, z0 + 5, "lantern", gb.lamp(True))
        b.set(x, y0 + 5, z0 + 5, PLAAT, gb.slab("top"))
        b.set(x, y0 + 4, z0 + 15, "lantern", gb.lamp(True))
        b.set(x, y0 + 5, z0 + 15, PLAAT, gb.slab("top"))
    b.sign(C - 2, y0 + 4, z1 + 2, "south", ["Proef 3", "Kies wijs.", "Of gek.", "Mag ook."], wall=True)
    b.zone("proef_3", x0 + 1, y0 + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1, wie="proef 3: kies je tegenstander")
    b.prop("proef_3_start", C, y0 + 1, z1 - 2, wie="het begin van proef 3")


def postkantoor(b):
    """Inside the second tier, behind the post square: the hall where the mail of three chutes is sorted."""
    x0, x1, z0, z1, y0, y1 = C - 11, C + 11, C + 19, C + T2 - 1, G, G + 7
    g.kamer(b, x0, y0, z0, x1, y1, z1, STENEN, vloer="polished_andesite", plafond=STENEN)
    b.fill(C - 1, G + 1, z1, C + 1, G + 3, z1 + 1, gb.AIR)                         # (the door to the square)
    b.fill(C - 2, G + 4, z1 + 1, C + 2, G + 4, z1 + 1, "chiseled_sandstone")
    # the counter, the sorting racks and the sacks
    for x in range(C - 7, C + 8):
        if x not in (C - 4, C + 4):
            b.set(x, G + 1, z0 + 4, PLAAT if x % 2 else "cut_sandstone", gb.slab("top") if x % 2 else None)
    for x in range(x0 + 1, x1, 2):
        b.fill(x, G + 1, z0 + 1, x, G + 3, z0 + 1, "bookshelf")
    for x, z, n in ((x0 + 1, z1 - 1, 2), (x0 + 2, z1 - 1, 1), (x1 - 1, z1 - 1, 3), (x1 - 2, z1 - 1, 1), (x1 - 1, z1 - 2, 1)):
        b.fill(x, G + 1, z, x, G + n, z, "white_wool" if (x + n) % 2 else "brown_wool")
    b.set(C - 5, G + 2, z0 + 4, "guhs:guh_kristal_lamp")
    b.set(C + 5, G + 2, z0 + 4, "guhs:guh_kristal_lamp")
    b.set(C + 2, G + 2, z0 + 4, "lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    for x in (C - 6, C, C + 6):
        g.hanglamp(b, x, y1 - 1, z0 + 7, ketting=1)
    b.chest(x0 + 1, G + 1, z0 + 6, "east", "guhs:chests/einde2_bouw_zij1_omashguh")
    b.sign(x0 + 1, G + 2, z0 + 7, "east", ["Verloren post", "Al 100 jaar", "niet opgehaald.", "Pak maar, njeg"], wall=True)
    b.sign(C - 2, G + 2, z0 + 4, "south", ["Loket 1", "Gesloten.", "De postguh", "doet een dutje"], wall=False)
    b.sign(C + 3, G + 4, z1 + 2, "south", ["Postkantoor", "Open als de", "postguh wakker", "is"], wall=True)
    b.plek("postguh", C, G + 1, z0 + 2, yaw=0.0, wie="de postguh, achter zijn loket", kind="POSTGUH")
    b.zone("postkantoor", x0 + 1, G + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1, wie="het postkantoor")
    # the post square in front of it: the sacks where the three chutes end
    for x in range(C - 2, C + 3):
        for z in range(C + T2 + 1, C + T2 + 6):
            b.set(x, G, z, "white_wool" if (x + z) % 2 else "brown_wool")
    b.prop("glijbaan_finish", C, G + 1, C + T2 + 4, wie="de postbak: hier komen de drie glijbanen uit")
    b.sign(C - 3, G + 1, C + T2 + 6, "south", ["Postbak", "Zacht landen", "verplicht", ""], wall=False)
    # (a nod to the merchant whose stall always stands where the mail comes down)
    b.fill(C + 10, G + 1, C + T2 + 8, C + 12, G + 1, C + T2 + 8, "spruce_slab", gb.slab("bottom"))
    b.set(C + 13, G + 1, C + T2 + 9, "spruce_fence")
    b.set(C + 10, G + 1, C + T2 + 9, "melon")
    b.set(C + 12, G + 1, C + T2 + 10, "melon")
    b.sign(C + 11, G + 1, C + T2 + 10, "south", ["Hier stond een", "koolkraam.", "Tot de post", "kwam."], wall=False)


def das_mol_grot(b):
    """The burrow of the das-molguhs in the east side of the second tier: a round cave full of kristalkaas."""
    cx, cy, cz = C + 22, G + 4, C - 6
    for x in range(cx - 7, cx + 8):
        for y in range(G + 1, G + 9):
            for z in range(cz - 8, cz + 9):
                if ((x - cx) / 6.6) ** 2 + ((y - cy) / 4.4) ** 2 + ((z - cz) / 7.6) ** 2 <= 1.0:
                    b.set(x, y, z, gb.AIR)
    b.fill(cx + 5, G + 1, cz - 1, C + T2, G + 3, cz, gb.AIR)                       # (the way in, through the east face)
    for x, z, y in ((cx - 4, cz - 3, G + 1), (cx + 2, cz + 5, G + 1), (cx - 2, cz + 4, G + 1), (cx + 3, cz - 5, G + 1), (cx, cz, G + 1)):
        b.set(x, y, z, "guhs:kristalkaas_blok")
        g.kristal(b, x, y + 1, z)
    for x, z in ((cx - 5, cz + 1), (cx + 1, cz - 6), (cx - 1, cz + 6)):
        b.set(x, G, z, "guhs:kristalkaas_erts")
    b.sign(C + T2 + 1, G + 4, cz - 2, "east", ["Das-molguhs", "Graven altijd.", "Zien niks.", "Vinden alles."], wall=True)
    b.wezen("das_molguh_1", cx - 2, G + 1, cz - 2, "guhs:das_molguh", yaw=90.0, honk=6)
    b.wezen("das_molguh_2", cx + 2, G + 1, cz + 2, "guhs:das_molguh", yaw=-60.0, honk=6)


def huizen(b):
    """The houses of the lower town and of the terrace, each with a townsguh's spot at some doors."""
    lo, hi = C - MUUR + 3, C + MUUR - 3
    onder = [(lo, C - 26, 7, 7, "east"), (lo, C - 8, 8, 7, "east"), (lo + 1, C + 14, 7, 8, "east"),
             (hi - 7, C - 26, 7, 7, "west"), (hi - 8, C - 8, 8, 7, "west"), (hi - 8, C + 14, 7, 8, "west"),
             (C - 28, lo, 7, 7, "south"), (C - 4, lo, 8, 7, "south"), (C + 18, lo, 7, 7, "south"),
             (C - 34, hi - 7, 7, 7, "north"), (C - 20, hi - 7, 8, 7, "north"), (C + 13, hi - 7, 8, 7, "north"), (C + 27, hi - 7, 7, 7, "north")]
    t0, t1 = C - T2 + 2, C + T2 - 2
    boven = [(t0, C - 26, 6, 7, "east"), (t0, C - 14, 6, 7, "east"), (t1 - 6, C - 26, 6, 7, "west"), (t1 - 6, C - 14, 6, 7, "west"),
             (C - 24, t0, 7, 6, "south"), (C + 17, t0, 7, 6, "south"),
             (C - 26, t1 - 6, 7, 6, "north"), (C - 13, t1 - 6, 7, 6, "north"), (C + 6, t1 - 6, 7, 6, "north"), (C + 19, t1 - 6, 7, 6, "north")]
    deuren = []
    for i, (x, z, w, d, kant) in enumerate(onder):
        deuren.append(g.huis(b, x, z, x + w, z + d, G + 1, kant, HUISMUUR, DAK, hoog=4, binnen=i))
    for i, (x, z, w, d, kant) in enumerate(boven):
        deuren.append(g.huis(b, x, z, x + w, z + d, Y2 + 1, kant, HUISMUUR, DAK, hoog=4, binnen=i + 1))
    wie = ["een Omashguhse bakker", "een burger met een brief", "de buurvrouw van de postguh", "een guh die op de post wacht",
           "een guh met een kar vol kristalkaas", "een terrasbewoner", "de wachter bij de trap"]
    for n, i in enumerate((1, 4, 7, 10, 12, 14, 19)):
        x, y, z = deuren[i]
        kant = (onder + boven)[i][4]
        dx, dz = gb.DIRS[kant]
        b.plek(f"bewoner_{n + 1}", x + dx, y, z + dz, yaw=g.YAW[kant], wie=wie[n], kind="OMASHGUH_BURGER")


def steunberen(b, ritten):
    """Buttresses against the faces of the two tiers, wherever no chute runs along the face."""
    bezet = set()
    for cellen in ritten:
        for x, _y, z in cellen:
            bezet.update((x + dx, z + dz) for dx in (-2, -1, 0, 1, 2) for dz in (-2, -1, 0, 1, 2))
    for half, y0, y1 in ((T2, G + 1, Y2), (T3, Y2 + 1, Y3)):
        lo, hi = C - half - 1, C + half + 1
        for i in range(lo + 4, hi - 3, 6):
            for x, z, naar in ((i, lo, "south"), (i, hi, "north"), (lo, i, "east"), (hi, i, "west")):
                if (x, z) in bezet or any((x, y, z) in b.s.blocks and b.s.blocks[(x, y, z)][0] != gb.AIR for y in range(y0, y1)):
                    continue
                b.fill(x, y0, z, x, y1 - 4, z, STENEN)
                b.set(x, y1 - 3, z, TRAP, gb.stair(naar))
                b.set(x, y0 + 2, z, "cut_sandstone")


def versiering(b, top):
    """Crystal spires outside the wall, clusters under the island, lanterns along the streets."""
    rng = b.rng
    for _ in range(90):                              # (loose clusters of kristalkaas on the bare rock outside the wall)
        x, z = rng.randrange(2, W - 2), rng.randrange(2, D - 2)
        buiten = max(abs(x - C), abs(z - C)) > MUUR + 2 and not (abs(x - C) < 9 and z > C)
        if buiten and (x, z) in top and (x, G + 1, z) not in b.s.blocks:
            if rng.random() < 0.25:
                b.set(x, G + 1, z, STEEN)
                g.kristal(b, x, G + 2, z)
            else:
                g.kristal(b, x, G + 1, z)
    spitsen = [(C - 57, C - 20), (C - 58, C + 12), (C + 57, C - 14), (C + 58, C + 18), (C - 18, C - 58), (C + 22, C - 57), (C - 30, C + 57),
               (C + 34, C + 57)]
    for i, (x, z) in enumerate(spitsen):
        if (x, z) not in top:
            continue
        hoog = 5 + (i * 3) % 5
        for y in range(G + 1, G + 1 + hoog):
            b.set(x, y, z, "guhs:kristalkaas_blok" if y > G + hoog - 2 else STEEN)
            if y <= G + hoog // 2 and (x + 1, z) in top:
                b.set(x + 1, y, z, STEEN)
        g.kristal(b, x, G + 1 + hoog, z)
    for (x, z), t in sorted(top.items()):
        o = t
        while (x, o - 1, z) in b.s.blocks:
            o -= 1
        if o > 1 and rng.random() < 0.02:
            g.kristal(b, x, o - 1, z, "down")
    # street lanterns on posts along the main street and around the square
    for x, z in ((C - 5, C + 47), (C + 5, C + 47), (C - 8, C + 38), (C + 8, C + 38), (C - 40, C + 36), (C + 40, C + 36), (C - 40, C - 36),
                 (C + 40, C - 36), (C - 14, C - 36), (C + 14, C - 36), (C - 38, C), (C + 38, C)):
        b.set(x, G + 1, z, WAL)
        b.set(x, G + 2, z, WAL)
        b.set(x, G + 3, z, "lantern", gb.lamp(False))
    # the landing place outside the gate
    b.sign(C + 5, G + 1, C + 58, "south", ["Landingsplek", "Bizons links", "Vetbootjes", "rechts"], wall=False)
    for x in (C - 7, C + 7):
        for z in (C + 55, C + 67):
            b.set(x, G + 1, z, "guhs:kristalkaas_blok")
            g.kristal(b, x, G + 2, z)


def bouw(h):
    b = kit.Bouw(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"], prefix=g.PREFIX)
    top = g.eiland(b, vorm(), STEEN, rots(g.ruis("omashguh_erts", 3.0)), kern=STEEN)
    # the paved town inside the wall
    for x in range(C - MUUR + 2, C + MUUR - 1):
        for z in range(C - MUUR + 2, C + MUUR - 1):
            b.set(x, G, z, STENEN if (x // 3 + z // 3) % 2 else "smooth_sandstone")
    for x in range(C - 3, C + 4):                    # (the road from the landing to the gate and on to the post square)
        for z in range(C + T2 + 1, D):
            if (x, z) in top:
                b.set(x, G, z, "cut_sandstone")
    stadsmuur(b)
    blok(b, T2, G + 1, Y2)
    blok(b, T3, Y2 + 1, Y3)
    borstwering(b, T2, Y2 + 1, open_={(C, C - T2)})
    paleis(b)
    platform(b)
    proeven(b)
    postkantoor(b)
    das_mol_grot(b)
    huizen(b)
    trappen(b)
    ritten = [goot(b, LINKSOM), kurkentrekker(b), goot(b, RECHTSOM)]
    steunberen(b, ritten)
    versiering(b, top)
    b.zone("aankomst", C - 6, G + 1, C + 55, C + 6, G + 6, C + 67, wie="de landingsplek buiten de poort")
    b.prop("aankomst_speler", C, G + 1, C + 59)
    b.prop("uitzicht", C + 20, G + 24, D + 30, wie="van hier zie je de hele berg, met de glijbanen")
    return b, ritten


def check(b, ritten):
    start = tuple(b.plekken["aankomst_speler"]["pos"])
    einde = tuple(b.plekken["glijbaan_finish"]["pos"])
    problems = []
    namen = ("linksom", "de kurkentrekker", "rechtsom")
    for i, cellen in enumerate(ritten):
        x, y, z = cellen[0]
        rit, p = g.glijcheck(b, (x, y + 1, z), einde)
        problems += p
        if len(rit) < len(cellen) - 1:
            problems.append(f"glijbaan {i + 1}: de rit stopt na {len(rit)} van {len(cellen)} blokken")
        b.pad(f"glijbaan_route_{i + 1}", g.hoeken(rit), wie=f"postglijbaan {namen[i]}: {len(rit)} blokken")
    verwacht = ["troonzaal", "bumi", "flopsie", "proef_1", "proef_2", "proef_3", "proef_1_start", "proef_2_start", "proef_3_start",
                "postkantoor", "postguh", "glijbaan_start", "glijbaan_finish", ("glijbaan_route", 3), "kristalsnoep", ("bewoner", 5),
                ("das_molguh", 2)]
    problems += g.eerlijk(b) + b.check(start, verwacht, min_licht=5)
    return problems


def verlicht(b, start):
    return g.verlicht(b, start, "ochre_froglight", {STEEN, STENEN, "minecraft:smooth_sandstone", "minecraft:cut_sandstone",
                                                          "minecraft:polished_andesite", "minecraft:moss_block", "minecraft:spruce_planks"}, min_licht=7, raster=7)


def build(h):
    b, ritten = bouw(h)
    start = tuple(b.plekken["aankomst_speler"]["pos"])
    b.connect()
    g.trapvormen(b)
    lampen = verlicht(b, start)
    problems = check(b, ritten)
    if problems:
        raise SystemExit(f"{NAAM}: self-check found {len(problems)} problem(s):\n  " + "\n  ".join(problems[:40]))
    b.save_tiles()
    kit.leegte_structuur(h, b, titel="Omashguh", tooltip="Een bergstadje van klipkaas waar de post vanzelf naar beneden glijdt", y=Y)
    print(f"{NAAM}: self-check ok {b.stats}, {lampen} lampen gelegd, ritten {[len(r) for r in ritten]}")
    return b
