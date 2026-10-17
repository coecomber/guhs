"""
einde2, slice geest_bouw (CONTRACT_E2_B 4.5 + 7.8; DESIGN_EINDE2 5 "Geestenwereld revamp + Witte Lotus"): the White Lotus of
the Geestenwereld, rebuilt. It replaces the ruin of phase A (the puzzle gate stays):

  - a walled compound on a bigger island; in front of its gate the 3 x 3 lotus puzzle (as before);
  - inside: the court (lotus_hof) with a giant Pai Sho board as its floor (a little guh head in the middle), and round it
    five wings, one per master, each with its own style, an echo master, a memorial stone and its trial:
      Bumi-guh (west): three stone rooms in a row that "turn" (bumi_kamer_1..3, bumi_draai_1..3);
      Pakguh (east): an ice maze with mirrors, a stream that shifts platforms, the betrothal necklace at the end;
      Piandao-guh (north-west): a dojo with the rhythm spot, a forge and a lump of meteorite;
      Jeong Jeong-guh (north-east): a pond of eight candles and eight lotus leaves;
      Oom Miroh (south-east, inside the wall): the tea garden with three guest spirits and three teapots;
  - the old members' garden (south-west) with three echoes, and the sanctuary (north): six pedestals for the tiles round
    the closed stone lotus, the Oude Reigerguh asleep beside it, the founder at the door, and under it the Lotusarchief;
  - the Hemeltrap leaves the compound to the east and climbs to the Top der Voorgangers, which now holds the Hall: the
    eight memorial stones of the earlier Guhvatars in an arc and the fresco;
  - the Trap der meesters: the road from the Meditatiecirkel runs between five giant, half overgrown statues of the
    masters, with guh ears.
Markers: CONTRACT_E2_B 4.5 (every id there), plus the ones of phase A that stay.
"""
import math

from features import einde2_geestenwereld_bouw as g
from features import einde2_geestenwereld_tekst as tekst

LOTUS, TOP = g.LOTUS, g.TOP
GRAS, AARDE, STEEN, TEGEL, PLAAT, TRAP = g.GRAS, g.AARDE, g.STEEN, g.TEGEL, g.PLAAT, g.TRAP
HOUT, SHOUT, PLANK, SPLANK = g.HOUT, g.SHOUT, g.PLANK, g.SPLANK
CYAAN, ROZE, SCHADUW, NEVEL, LOTUSBLAD, LANTAARN = g.CYAAN, g.ROZE, g.SCHADUW, g.NEVEL, g.LOTUSBLAD, g.LANTAARN
HOED, GEDENK, PUZZEL, WATER = g.HOED, g.GEDENK, g.PUZZEL, g.WATER
_hash = g._hash

MEESTERS = ["bumi", "pakguh", "piandao", "jeong_jeong", "miroh"]
ECHO = {"bumi": "BUMI_GUH_ECHO", "pakguh": "PAKGUH_ECHO", "piandao": "PIANDAO_GUH_ECHO", "jeong_jeong": "JEONG_JEONG_GUH_ECHO",
        "miroh": "OOM_MIROH_ECHO"}
LAMP_AAN = {"hanging": "false", "waterlogged": "false"}
LAMP_HANG = {"hanging": "true", "waterlogged": "false"}
KAARS = {"candles": "3", "lit": "true", "waterlogged": "false"}


# =====================================================================================================================
# small tools
# =====================================================================================================================
def gat(b, x0, y0, z0, x1, y1, z1):
    """Empties a box (nothing stays: the dimension is air)."""
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                b.s.blocks.pop((x, y, z), None)


def vul(b, x0, y0, z0, x1, y1, z1, naam, props=None):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                b.set(x, y, z, naam, props)


def gebouw(b, x0, z0, x1, z1, f, hoog, wand, vloer, dak, rand=None):
    """A building: outer walls x0..x1, z0..z1 (inclusive) from f to f + hoog - 1, the floor at f - 1, a flat roof at f + hoog
    with a low rim (rand) round its edge; inside empty. wand / vloer may be a function (x, y, z) -> block."""
    kies = (lambda n: n if callable(n) else (lambda x, y, z: n))
    wand, vloer = kies(wand), kies(vloer)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand_cel = x in (x0, x1) or z in (z0, z1)
            b.set(x, f - 1, z, vloer(x, f - 1, z))
            for y in range(f, f + hoog):
                if rand_cel:
                    b.set(x, y, z, wand(x, y, z))
                else:
                    b.s.blocks.pop((x, y, z), None)
            b.set(x, f + hoog, z, dak)
            if rand and rand_cel:
                b.set(x, f + hoog + 1, z, rand, {"type": "bottom", "waterlogged": "false"} if "slab" in rand or "plaat" in rand else None)


def hanglamp(b, x, y, z):
    """A spirit lantern hanging under the block above it."""
    if not b.leeg(x, y + 1, z):
        b.set(x, y, z, LANTAARN, LAMP_HANG)


def bordje(b, x, y, z, facing, regels, wall=True):
    b.sign(x, y, z, facing, regels, wall=wall)


def plat_dak(b, x, z, y):
    """(mistreiger) a free spot on a flat roof: two air cells over a solid one."""
    return b.leeg(x, y, z) and b.leeg(x, y + 1, z) and not b.leeg(x, y - 1, z)


# =====================================================================================================================
# the White Lotus
# =====================================================================================================================
def witte_lotus(b):
    cx, cz, f, r = LOTUS["x"], LOTUS["z"], LOTUS["f"], LOTUS["r"]
    g.eiland(b, cx, cz, f, r, 26, 404)
    gz = cz + 36                                   # the front wall with the gate (z gz, gz + 1)
    # the ground of the compound and the courtyard: tiles with grass breaking through
    for x in range(cx - 48, cx + 49):
        for z in range(cz - 46, cz + 56):
            if b.s.blocks.get((x, f - 1, z), ("",))[0] == GRAS and _hash(x, z, 41) < 0.82:
                b.set(x, f - 1, z, TEGEL)
    pai_sho_hof(b, cx, cz, f)
    ingang(b, cx, cz, f, gz)
    bumi(b, cx, cz, f)
    pakguh(b, cx, cz, f)
    piandao(b, cx, cz, f)
    jeong_jeong(b, cx, cz, f)
    miroh(b, cx, cz, f)
    ledenhof(b, cx, cz, f)
    heiligdom(b, cx, cz, f)
    poort_en_voorplein(b, cx, cz, f, gz)
    b.zone("zone_lotus_ruines", cx - r - 8, f - 28, cz - r - 8, cx + r + 8, f + 22, cz + r + 8)
    return (cx, cz + 54)


def muur(b, cx, cz, f, gz, gaten):
    """The wall round the compound (only where nothing stands yet: the wings are part of it), 5 high, a lantern on a white
    pillar every few blocks; gaten = (x, z, r) where it stays open (the Hemeltrap)."""
    hoeken = [(cx - 34, gz), (cx - 47, cz + 16), (cx - 47, cz - 8), (cx - 38, cz - 36), (cx - 14, cz - 47), (cx + 14, cz - 47),
              (cx + 38, cz - 36), (cx + 47, cz - 8), (cx + 47, cz + 16), (cx + 34, gz)]
    cellen = []
    for (ax, az), (bx, bz) in zip(hoeken, hoeken[1:]):
        n = max(abs(bx - ax), abs(bz - az))
        for i in range(n + 1):
            c = (round(ax + (bx - ax) * i / n), round(az + (bz - az) * i / n))
            if not cellen or cellen[-1] != c:
                if cellen and cellen[-1][0] != c[0] and cellen[-1][1] != c[1]:
                    cellen.append((c[0], cellen[-1][1]))          # (face to face, never only an edge)
                cellen.append(c)
    for i, (x, z) in enumerate(cellen):
        if any(math.hypot(x - gx, z - gzz) <= gr for gx, gzz, gr in gaten):
            continue
        if not all(b.leeg(x, y, z) for y in range(f, f + 5)) or b.leeg(x, f - 1, z):
            continue
        pilaar = i % 7 == 0
        for y in range(f, f + 5):
            b.set(x, y, z, HOUT if pilaar else TEGEL, {"axis": "y"} if pilaar else None)
        if pilaar:
            b.set(x, f + 5, z, LANTAARN, LAMP_AAN)
        else:
            b.set(x, f + 5, z, PLAAT, {"type": "bottom", "waterlogged": "false"})


def pai_sho_hof(b, cx, cz, f):
    """The court: a Pai Sho board of 25 x 25 as its floor (the circle, the four gardens, the four gates, the lines), and
    in its very middle a little guh head with two ears."""
    bx, bz = cx, cz + 4
    for dx in range(-13, 14):
        for dz in range(-13, 14):
            x, z = bx + dx, bz + dz
            d = math.hypot(dx, dz)
            if abs(dx) == 13 or abs(dz) == 13:
                n = SPLANK
            elif d > 12.3:
                n = SPLANK
            elif max(abs(dx), abs(dz)) <= 1 or (abs(dx) == 1 and dz == -2):
                n = "minecraft:pink_concrete"                                       # the guh head (and its ears)
                if dz == 0 and abs(dx) == 1:
                    n = "minecraft:black_concrete"                                  # its eyes
            elif abs(dx) + abs(dz) <= 7:
                if dx == 0 or dz == 0:
                    n = STEEN
                else:
                    n = "minecraft:red_terracotta" if (dx > 0) == (dz < 0) else "minecraft:white_concrete"
            elif (abs(dx) <= 1 and abs(dz) >= 11) or (abs(dz) <= 1 and abs(dx) >= 11):
                n = "minecraft:gold_block"                                          # the four gates
            elif d > 11.3:
                n = STEEN
            elif dx % 3 == 0 or dz % 3 == 0:
                n = STEEN
            else:
                n = TEGEL
            b.set(x, f - 1, z, n)
            for y in range(f, f + 4):
                b.s.blocks.pop((x, y, z), None)
    for dx, dz in ((-13, -13), (13, -13), (-13, 13), (13, 13)):
        g.lantaarnpaal(b, bx + dx, f, bz + dz)
    b.zone("lotus_hof", bx - 12, f, bz - 12, bx + 12, f + 4, bz + 12, wie="het binnenhof van de Witte Lotus: een reusachtig Pai Sho-bord als vloer")


def ingang(b, cx, cz, f, gz):
    """From the gate north to the court: a colonnade of white pillars under a roof of slabs."""
    for z in range(cz + 18, gz):
        for x in range(cx - 3, cx + 4):
            b.set(x, f - 1, z, TEGEL if (z % 2 or abs(x - cx) > 1) else STEEN)
            for y in range(f, f + 5):
                b.s.blocks.pop((x, y, z), None)
        if (z - cz) % 3 == 0:
            for x in (cx - 4, cx + 4):
                for y in range(f, f + 5):
                    b.set(x, y, z, HOUT, {"axis": "y"})
        for x in range(cx - 4, cx + 5):
            b.set(x, f + 5, z, PLAAT, {"type": "bottom", "waterlogged": "false"})
    for z in range(cz + 19, gz, 4):
        hanglamp(b, cx, f + 4, z)


def poort_en_voorplein(b, cx, cz, f, gz):
    """The gate in the front wall (a lotus over it), the puzzle before it, the courtyard where the road arrives."""
    for x in range(cx - 8, cx + 9):
        for y in range(f, f + 10):
            if not (abs(x - cx) <= 2 and y < f + 6):
                b.set(x, y, gz, TEGEL)
                b.set(x, y, gz + 1, TEGEL)
            else:
                b.s.blocks.pop((x, y, gz), None)
                b.s.blocks.pop((x, y, gz + 1), None)
    for dx, dy in ((0, 7), (-1, 8), (1, 8), (0, 8), (0, 9), (-2, 9), (2, 9), (-1, 10), (1, 10)):
        b.set(cx + dx, f + dy, gz + 2, "minecraft:white_concrete")
        b.zet(cx + dx, f + dy, gz + 1, TEGEL)
    b.set(cx, f + 8, gz + 2, "minecraft:pink_concrete")
    b.prop("poort", cx, f, gz, wie="de poort van de Witte Lotus (dicht tot de puzzel klopt)")
    b.zone("poort_doorgang", cx - 2, f, gz, cx + 2, f + 5, gz + 1, wie="de doorgang van de poort")
    for (rij, kol), (stuk, goed) in g.TEGELS.items():
        x, z = cx - 1 + kol, gz + 6 + rij
        draai = (goed + 1 + int(_hash(rij, kol, 44) * 3)) % 4
        b.set(x, f - 1, z, PUZZEL, {"stuk": stuk, "draai": str(draai)})
        n = rij * 3 + kol + 1
        b.prop(f"tegel_{n}", x, f - 1, z, wie=f"puzzeltegel {stuk} (rij {rij + 1}, kolom {kol + 1}); goed bij draai {goed}"
                                               + (" (de middelste: elke draai is goed)" if stuk == "midden" else ""))
    for x in range(cx - 2, cx + 3):
        for z in range(gz + 5, gz + 10):
            if b.s.blocks.get((x, f - 1, z), ("",))[0] != PUZZEL:
                b.set(x, f - 1, z, "minecraft:polished_deepslate")
    b.zone("lotuspuzzel", cx - 1, f - 1, gz + 6, cx + 1, f, gz + 8, wie="de draaipuzzel van negen tegels voor de poort")
    g.gedenksteen(b, "witte_lotus", cx + 5, f, gz + 7, "west", ["Witte Lotus"])
    b.prop("plek_lotus_ruines", cx, f, gz + 14)
    g.meditatiesteen(b, "lotus_ruines", cx - 5, f, gz + 12)
    b.plek("libelkonijnguh_4", cx + 12, f, gz + 12, wie="libelkonijnguh", wezen="guhs:libelkonijnguh")
    for k, (dx, dz) in enumerate(((-12, 6), (12, 6), (-14, 12), (14, 16), (-18, 16))):     # broken columns
        if b.leeg(cx + dx, f - 1, gz + dz):
            continue
        for y in range(f, f + 3 + int(_hash(k, 45) * 6)):
            b.set(cx + dx, y, gz + dz, TEGEL if y % 3 else STEEN)
        b.zet(cx + dx + 1, f, gz + dz, TEGEL)


# --- the five wings ------------------------------------------------------------------------------------------------------
def vleugel_basis(b, meester, x0, z0, x1, z1, f, hoog, wand, vloer, dak, rand, deur, lore_plek, lore_facing, echo, echo_yaw,
                  start, einde, naam_regels):
    """What every wing has: the building, its door (x0, z0, x1, z1 of the opening, from f), the memorial stone, the echo
    master, the props where the trial starts and ends, the zone."""
    gebouw(b, x0, z0, x1, z1, f, hoog, wand, vloer, dak, rand)
    for x in range(x0 - 1, x1 + 2):                     # eaves: a ring of steps round the roof, leaning in
        for z in range(z0 - 1, z1 + 2):
            if x0 <= x <= x1 and z0 <= z <= z1:
                continue
            facing = "east" if x < x0 else ("west" if x > x1 else ("south" if z < z0 else "north"))
            b.zet(x, f + hoog, z, TRAP, {"facing": facing, "half": "bottom", "shape": "straight", "waterlogged": "false"})
    dx0, dz0, dx1, dz1, dh = deur
    gat(b, dx0, f, dz0, dx1, f + dh - 1, dz1)
    for x in range(min(dx0, dx1), max(dx0, dx1) + 1):
        for z in range(min(dz0, dz1), max(dz0, dz1) + 1):
            b.set(x, f - 1, z, TEGEL)
    lx, lz = lore_plek
    b.set(lx, f, lz, GEDENK, {"facing": lore_facing})
    b.prop(f"lore_{meester}", lx, f, lz, wie=f"gedenksteen: {tekst.LORE[meester][0]}")
    ex, ez = echo
    b.plek(f"echo_{meester}", ex, f, ez, yaw=echo_yaw, wie=f"de echo van {tekst.MEESTER_NAAM[meester]} (doorschijnend, gloeit)",
           kind=ECHO[meester])
    b.prop(f"proef_{meester}_start", start[0], start[1], start[2], wie=f"hier begint de proef van {tekst.MEESTER_NAAM[meester]}")
    b.prop(f"proef_{meester}_einde", einde[0], einde[1], einde[2], wie=f"hier eindigt de proef van {tekst.MEESTER_NAAM[meester]} (de tegel)")
    b.zone(f"vleugel_{meester}", x0, f, z0, x1, f + hoog - 1, z1, wie=f"de vleugel van {tekst.MEESTER_NAAM[meester]}")
    # the name over the door, outside
    sx = (dx0 + dx1) // 2
    sz = (dz0 + dz1) // 2
    return sx, sz


def bumi(b, cx, cz, f):
    """Bumi-guh (west): a heavy stone hall, mossy and green; three rooms in a row that turn (the obvious door is wrong)."""
    def wand(x, y, z):
        h = _hash(x, y, z, 501)
        return "minecraft:mossy_stone_bricks" if h < 0.35 else ("minecraft:chiseled_stone_bricks" if h > 0.94 else "minecraft:stone_bricks")
    x0, x1, z0, z1 = cx - 45, cx - 15, cz - 6, cz + 14
    vleugel_basis(b, "bumi", x0, z0, x1, z1, f, 5, wand, "minecraft:polished_andesite", "minecraft:mossy_stone_bricks",
                  "minecraft:mossy_stone_brick_slab", (x1, cz + 2, x1, cz + 6, 4), (cx - 17, cz - 4), "east",
                  (cx - 19, cz + 9), g.YAW["east"], (cx - 22, f, cz + 4), (cx - 44, f, cz + 4), ["Bumi-guh"])
    # the rooms block: solid stone between the antechamber (x > cx - 24) and the far end, rooms carved into it
    for x in range(x0 + 1, cx - 23):
        for z in range(z0 + 1, z1):
            for y in range(f, f + 5):
                b.set(x, y, z, wand(x, y, z))
    for i, (ra, rb) in enumerate(((cx - 29, cx - 25), (cx - 35, cx - 31), (cx - 41, cx - 37)), 1):
        gat(b, ra, f, cz + 2, rb, f + 3, cz + 6)
        mx = (ra + rb) // 2
        b.set(mx, f - 1, cz + 4, "minecraft:chiseled_stone_bricks")
        b.prop(f"bumi_draai_{i}", mx, f - 1, cz + 4, wie=f"de draaisteen van kamer {i} (draait de kamer)")
        b.zone(f"bumi_kamer_{i}", ra, f, cz + 2, rb, f + 3, cz + 6, wie=f"Bumi's draaiende kamer {i}")
        for x in (ra, rb):
            for z in (cz + 2, cz + 6):
                b.set(x, f + 3, z, "minecraft:green_terracotta")
        hanglamp(b, mx, f + 3, cz + 4)
        # four doors: east and west lead on, north and south into a little dead end (the "obvious" ones)
        gat(b, ra - 1, f, cz + 4, ra - 1, f + 1, cz + 4)
        gat(b, rb + 1, f, cz + 4, rb + 1, f + 1, cz + 4)
        gat(b, mx, f, cz + 1, mx, f + 1, cz + 1)
        gat(b, mx - 1, f, cz - 1, mx + 1, f + 2, cz)
        gat(b, mx, f, cz + 7, mx, f + 1, cz + 7)
        gat(b, mx - 1, f, cz + 8, mx + 1, f + 2, cz + 9)
        b.set(mx, f, cz - 1, "minecraft:mossy_cobblestone")              # a boulder in each dead end
        b.set(mx, f, cz + 9, "minecraft:mossy_cobblestone")
    gat(b, cx - 44, f, cz + 3, cx - 43, f + 3, cz + 5)                       # the end: a niche with a pedestal
    b.set(cx - 44, f, cz + 3, "minecraft:polished_andesite")
    b.set(cx - 44, f + 1, cz + 3, "minecraft:moss_carpet")
    hanglamp(b, cx - 43, f + 3, cz + 5)
    # the antechamber: lanterns, a few boulders (Bumi likes rocks), green trim
    for z in (cz - 3, cz + 4, cz + 11):
        hanglamp(b, cx - 19, f + 4, z)
    for x, z in ((cx - 22, cz - 4), (cx - 22, cz + 12), (cx - 16, cz + 12)):
        b.set(x, f, z, "minecraft:mossy_cobblestone")
    bordje(b, cx - 14, f + 4, cz + 4, "east", ["Bumi-guh", "Denk als een", "gekke oude", "koning"])
    b.mistreiger.append((cx - 30, cz + 10))
    b.mistreiger.append((cx - 20, cz - 2))


def doolhof(seed, n):
    """A maze of n x n cells (recursive backtracker): the set of open walls ((i, j), (i2, j2))."""
    import random
    rng = random.Random(seed)
    gezien, open_ = {(0, 3)}, set()
    stapel = [(0, 3)]
    while stapel:
        i, j = stapel[-1]
        buren = [(i + di, j + dj) for di, dj in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= i + di < n and 0 <= j + dj < n and (i + di, j + dj) not in gezien]
        if not buren:
            stapel.pop()
            continue
        nb = rng.choice(buren)
        open_.add(frozenset(((i, j), nb)))
        gezien.add(nb)
        stapel.append(nb)
    # a few extra openings: a maze of ice with loops (the mirrors make it hard enough)
    for k in range(5):
        i, j = rng.randrange(n - 1), rng.randrange(n)
        open_.add(frozenset(((i, j), (i + 1, j))))
    return open_


def pakguh(b, cx, cz, f):
    """Pakguh (east): a hall of ice; a maze with ice mirrors, a stream that shifts platforms, and at its end the lore of the
    betrothal necklace."""
    x0, x1, z0, z1 = cx + 15, cx + 43, cz - 8, cz + 13
    wand = lambda x, y, z: "minecraft:blue_ice" if (y - f) == 4 else ("minecraft:light_blue_stained_glass" if (y - f) in (2, 3) and (x + z) % 4 == 0 else "minecraft:packed_ice")  # noqa: E731
    vleugel_basis(b, "pakguh", x0, z0, x1, z1, f, 6, wand, "minecraft:packed_ice", "minecraft:dark_prismarine", "minecraft:prismarine_brick_slab",
                  (x0, cz + 1, x0, cz + 4, 4), (cx + 17, cz - 6), "east", (cx + 19, cz + 9), g.YAW["west"], (cx + 21, f, cz + 2),
                  (cx + 40, f, cz - 5), ["Pakguh"])
    # the divider between the antechamber and the maze (x cx + 22), the maze of 7 x 7 cells in x cx + 22 .. cx + 43
    n, mx0, mz0 = 7, cx + 22, cz - 8
    open_ = doolhof(707, n)
    for x in range(mx0, mx0 + 3 * n + 1):
        for z in range(mz0, mz0 + 3 * n + 1):
            lx, lz = x - mx0, z - mz0
            wandcel = lx % 3 == 0 or lz % 3 == 0
            if wandcel and lx % 3 == 0 and lz % 3 == 0:
                muurtje = True
            elif lx % 3 == 0 and lz % 3 != 0:          # a wall between (i - 1, j) and (i, j)
                i, j = lx // 3, lz // 3
                muurtje = not (0 < i < n and frozenset(((i - 1, j), (i, j))) in open_)
            elif lz % 3 == 0 and lx % 3 != 0:
                i, j = lx // 3, lz // 3
                muurtje = not (0 < j < n and frozenset(((i, j - 1), (i, j))) in open_)
            else:
                muurtje = False
            if x in (x0, x1) or z in (z0, z1):
                continue                                  # (the hall's own walls)
            for y in range(f, f + 3):
                if muurtje:
                    b.set(x, y, z, "minecraft:packed_ice" if (y - f) < 2 else "minecraft:blue_ice")
                else:
                    b.s.blocks.pop((x, y, z), None)
    gat(b, mx0, f, mz0 + 1 + 3 * 3, mx0, f + 2, mz0 + 2 + 3 * 3)   # the way in: cell (0, 3)
    # the stream: two wide across the maze (under its walls), water that shifts the platforms
    sz0 = mz0 + 1 + 3 * 3
    stroom = []
    for x in range(mx0 + 1, x1):
        for z in (sz0, sz0 + 1):
            b.set(x, f - 1, z, WATER, {"level": "0"})
            b.set(x, f - 2, z, "minecraft:dark_prismarine")
        stroom.append((x, f - 1, sz0))
    b.pad("ijs_stroom", stroom[::2] + [stroom[-1]], wie="de stroom door Pakguh's ijsdoolhof (schuift de platforms)")
    for i, x in enumerate((mx0 + 3, mx0 + 8, mx0 + 13, mx0 + 18), 1):
        b.set(x, f - 1, sz0, "minecraft:blue_ice")
        b.set(x, f - 1, sz0 + 1, "minecraft:blue_ice")
        b.prop(f"ijs_platform_{i}", x, f - 1, sz0, wie=f"ijsplatform {i} in de stroom")
    # six mirrors of ice in cells along the way (a pane of light blue glass in a frame of packed ice)
    spiegels = [(1, 1), (3, 0), (5, 2), (2, 5), (4, 6), (6, 4)]
    for i, (ci, cj) in enumerate(spiegels, 1):
        x, z = mx0 + 1 + 3 * ci, mz0 + 1 + 3 * cj
        if (x, f - 1, z) in b.s.blocks and b.s.blocks[(x, f - 1, z)][0] == WATER:
            z += 1
        b.set(x, f, z, "minecraft:light_blue_stained_glass")
        b.set(x, f + 1, z, "minecraft:light_blue_stained_glass")
        b.set(x, f + 2, z, "minecraft:packed_ice")
        b.prop(f"ijs_spiegel_{i}", x, f, z, wie=f"ijsspiegel {i} (toont je spiegelbeeld, of iets anders)")
    # the end: the necklace on a pedestal in the far corner cell (6, 0)
    ex, ez = mx0 + 1 + 3 * 6, mz0 + 1
    b.set(ex + 1, f, ez, "minecraft:polished_diorite")
    b.set(ex + 1, f + 1, ez, "minecraft:conduit", {"waterlogged": "false"})
    b.prop("verlovingsketting", ex + 1, f + 1, ez, wie="de verlovingsketting van Pakguh (lore)")
    b.plekken["proef_pakguh_einde"]["pos"] = [ex, f, ez + 1]
    for i in range(n):
        for j in range(0, n, 2):
            hanglamp(b, mx0 + 1 + 3 * i, f + 5, mz0 + 1 + 3 * j)
    for z in (cz - 4, cz + 4, cz + 11):
        hanglamp(b, cx + 18, f + 5, z)
    bordje(b, x0 - 1, f + 4, cz + 3, "west", ["Pakguh", "Het ijs", "onthoudt alles"])
    b.mistreiger.append((cx + 30, cz))
    b.mistreiger.append((cx + 38, cz + 9))


def piandao(b, cx, cz, f):
    """Piandao-guh (north-west): a dojo of dark wood and paper walls; the rhythm spot, a forge, a lump of meteorite."""
    x0, x1, z0, z1 = cx - 34, cx - 15, cz - 34, cz - 15

    def wand(x, y, z):
        if (x - x0) % 4 == 0 and (z - z0) % 4 == 0 or (x in (x0, x1) and z in (z0, z1)):
            return "minecraft:dark_oak_log"
        return "minecraft:dark_oak_planks" if y - f in (0, 4) else "minecraft:white_concrete"
    vleugel_basis(b, "piandao", x0, z0, x1, z1, f, 5, wand, "minecraft:spruce_planks", "minecraft:dark_oak_planks", "minecraft:dark_oak_slab",
                  (cx - 23, z1, cx - 21, z1, 4), (cx - 18, cz - 17), "south", (cx - 24, cz - 30), g.YAW["south"], (cx - 22, f, cz - 17),
                  (cx - 27, f, cz - 29), ["Piandao-guh"])
    # the sparring ring (sandstone) with the rhythm spot in its middle
    rx, rz = cx - 24, cz - 24
    for x in range(rx - 4, rx + 5):
        for z in range(rz - 4, rz + 5):
            d = math.hypot(x - rx, z - rz)
            if d <= 4.3:
                b.set(x, f - 1, z, "minecraft:smooth_sandstone" if d > 3.3 else "minecraft:cut_sandstone")
    b.prop("ritme_plek", rx, f, rz, wie="de plek van het ritmeduel (op de maat klikken = pareren)")
    # the forge in the north-west corner: anvil, blast furnace, grindstone, the meteorite
    b.set(cx - 32, f, cz - 31, "minecraft:blast_furnace", {"facing": "east", "lit": "false"})
    b.set(cx - 32, f, cz - 30, "minecraft:smithing_table")
    b.set(cx - 30, f, cz - 31, "minecraft:anvil", {"facing": "east"})
    b.set(cx - 32, f, cz - 28, "minecraft:grindstone", {"face": "floor", "facing": "east"})
    b.prop("smidse", cx - 30, f, cz - 31, wie="de smidse van Piandao-guh (aambeeld)")
    for (dx, dy, dz), n in {(0, 0, 0): "minecraft:obsidian", (1, 0, 0): "minecraft:crying_obsidian", (0, 0, 1): "minecraft:gilded_blackstone",
                            (1, 0, 1): "minecraft:obsidian", (0, 1, 0): "minecraft:crying_obsidian", (1, 1, 1): "minecraft:blackstone"}.items():
        b.set(cx - 28 + dx, f + dy, cz - 33 + dz, n)
    b.prop("meteoriet", cx - 28, f + 1, cz - 33, wie="een stuk meteoriet (daar smeedde hij ooit een zwaard van)")
    # sword racks: blades of iron bars against the walls
    for x in range(cx - 25, cx - 16, 3):
        b.set(x, f, z0 + 1, "minecraft:dark_oak_fence")
        b.set(x, f + 1, z0 + 1, "minecraft:iron_bars")
        b.set(x, f + 2, z0 + 1, "minecraft:iron_bars")
    for z in (cz - 30, cz - 24, cz - 19):
        hanglamp(b, cx - 24, f + 4, z)
    hanglamp(b, cx - 31, f + 4, cz - 29)
    bordje(b, cx - 22, f + 4, z1 + 1, "south", ["Piandao-guh", "De weg van", "het zwaard"])
    b.mistreiger.append((cx - 24, cz - 24))


def jeong_jeong(b, cx, cz, f):
    """Jeong Jeong-guh (north-east): the runaway fire master's retreat; red and black walls, open to the sky over a pond
    with eight candles and eight lotus leaves (light the candles, burn no leaf)."""
    x0, x1, z0, z1 = cx + 15, cx + 34, cz - 34, cz - 15

    def wand(x, y, z):
        if x in (x0, x1) and z in (z0, z1) or (y - f) == 4:
            return "minecraft:polished_blackstone_bricks"
        return "minecraft:red_nether_bricks" if (x + z + y) % 5 else "minecraft:nether_bricks"
    vleugel_basis(b, "jeong_jeong", x0, z0, x1, z1, f, 5, wand, "minecraft:polished_blackstone", "minecraft:polished_blackstone_bricks",
                  "minecraft:polished_blackstone_brick_slab", (cx + 22, z1, cx + 24, z1, 4), (cx + 19, cz - 17), "south",
                  (cx + 18, cz - 25), g.YAW["east"], (cx + 23, f, cz - 17), (cx + 30, f, cz - 31), ["Jeong Jeong-guh"])
    # open to the sky over the pond
    gat(b, x0 + 4, f + 5, z0 + 4, x1 - 4, f + 5, z1 - 4)
    px, pz = cx + 25, cz - 25
    vijver = []
    for x in range(px - 5, px + 6):
        for z in range(pz - 5, pz + 6):
            if math.hypot(x - px, z - pz) <= 5.3:
                b.set(x, f - 1, z, WATER, {"level": "0"})
                b.set(x, f - 2, z, "minecraft:polished_blackstone")
                vijver.append((x, z))
    for i in range(8):
        th = i * math.tau / 8
        kx, kz = px + round(math.cos(th) * 3), pz + round(math.sin(th) * 3)
        b.set(kx, f - 1, kz, "minecraft:polished_blackstone")
        b.set(kx, f, kz, "minecraft:candle", {"candles": "1", "lit": "false", "waterlogged": "false"})
        b.prop(f"kaars_{i + 1}", kx, f, kz, wie=f"kaars {i + 1} (aansteken zonder de lotusbladen te verbranden)")
        th2 = th + math.tau / 16
        lx, lz = px + round(math.cos(th2) * 4.6), pz + round(math.sin(th2) * 4.6)
        if (lx, lz) not in vijver or not b.s.blocks.get((lx, f - 1, lz), ("",))[0] == WATER:
            lx, lz = px + round(math.cos(th2) * 4), pz + round(math.sin(th2) * 4)
        b.set(lx, f - 1, lz, LOTUSBLAD)
        b.prop(f"lotusblad_{i + 1}", lx, f - 1, lz, wie=f"lotusblad {i + 1} (mag niet verbranden)")
    b.set(px, f - 1, pz, "minecraft:polished_blackstone")
    b.set(px, f, pz, "minecraft:soul_campfire", {"lit": "false", "facing": "south", "signal_fire": "false", "waterlogged": "false"})
    for x, z in ((x0 + 2, z0 + 2), (x1 - 2, z0 + 2), (x0 + 2, z1 - 2), (x1 - 2, z1 - 2)):
        hanglamp(b, x, f + 4, z)
    bordje(b, cx + 23, f + 4, z1 + 1, "south", ["Jeong Jeong-guh", "Vuur is een", "rivier: niet", "een fakkel"])
    b.mistreiger.append((x0 + 2, z0 + 2))


def miroh(b, cx, cz, f):
    """Oom Miroh, the Grote Lotus (south-east, inside the wall): an open tea garden under a bamboo roof; a low table with
    three guest spirits, three teapots on a bench, jasmine in pots. Miroh lives: his echo is a dream of him."""
    x0, x1, z0, z1 = cx + 7, cx + 32, cz + 19, cz + 34
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not b.leeg(x, f - 1, z):
                b.set(x, f - 1, z, GRAS if _hash(x, z, 600) < 0.7 else "minecraft:moss_block")
    tx, tz = cx + 20, cz + 26
    # the bamboo roof on four posts
    for dx in (-4, 4):
        for dz in (-3, 3):
            for y in range(f, f + 4):
                b.set(tx + dx, y, tz + dz, "minecraft:bamboo_block", {"axis": "y"})
    for x in range(tx - 5, tx + 6):
        for z in range(tz - 4, tz + 5):
            b.set(x, f + 4, z, "minecraft:bamboo_mosaic_slab", {"type": "bottom", "waterlogged": "false"})
    for x in range(tx - 3, tx + 4):
        for z in range(tz - 2, tz + 3):
            b.set(x, f + 5, z, "minecraft:bamboo_mosaic_slab", {"type": "bottom", "waterlogged": "false"})
    hanglamp(b, tx, f + 3, tz)
    # the low table, four cushions; Miroh at the head, the three guests
    b.set(tx, f, tz, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
    b.set(tx + 1, f, tz, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
    b.set(tx, f + 1, tz, "minecraft:flower_pot")
    b.prop("theetuin_tafel", tx, f, tz, wie="de theetafel van Oom Miroh (maak de wijsheid af)")
    gasten = [("geest_gast_1", tx - 2, tz, g.YAW["east"], "guhs:lantaarngeest"), ("geest_gast_2", tx, tz + 2, g.YAW["north"], "guhs:libelkonijnguh"),
              ("geest_gast_3", tx + 1, tz - 2, g.YAW["south"], None)]
    for id, x, z, yaw, wezen in gasten:
        if wezen:
            b.plek(id, x, f, z, yaw=yaw, wie="een geest met een probleem schuift aan bij Miroh", wezen=wezen)
        else:
            b.plek(id, x, f, z, yaw=yaw, wie="een geest met een probleem schuift aan bij Miroh", kind="THEEGEEST_GUH")
    for x in range(tx + 5, tx + 9):
        b.set(x, f, tz + 3, PLAAT, {"type": "top", "waterlogged": "false"})
    for i, x in enumerate((tx + 5, tx + 6, tx + 8), 1):
        b.set(x, f + 1, tz + 3, "minecraft:decorated_pot", {"facing": "north", "waterlogged": "false", "cracked": "false"})
        b.prop(f"theepot_{i}", x, f + 1, tz + 3, wie=f"theepot {i} (de juiste thee per geest)")
    for x, z in ((x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1), (tx - 7, tz + 6)):
        b.set(x, f, z, "minecraft:potted_flowering_azalea_bush")
    for x, z in ((x0 + 3, z1 - 2), (x1 - 3, z0 + 2)):
        g.lantaarnpaal(b, x, f, z)
    b.set(tx - 8, f, tz - 4, GEDENK, {"facing": "east"})
    b.prop("lore_miroh", tx - 8, f, tz - 4, wie=f"gedenksteen: {tekst.LORE['miroh'][0]}")
    b.plek("echo_miroh", tx + 3, f, tz, yaw=g.YAW["west"], wie="de echo van Oom Miroh: een droom van hem (hij leeft nog)", kind=ECHO["miroh"])
    b.prop("proef_miroh_start", tx - 4, f, tz + 5, wie="hier begint de proef van Oom Miroh")
    b.prop("proef_miroh_einde", tx + 3, f, tz + 5, wie="hier eindigt de proef van Oom Miroh (de tegel)")
    b.zone("vleugel_miroh", x0, f, z0, x1, f + 5, z1, wie="de theetuin van Oom Miroh, de Grote Lotus")
    b.zone("theetuin_miroh", tx - 5, f, tz - 4, tx + 5, f + 3, tz + 4, wie="onder het bamboedak")
    b.sign(tx - 8, f + 1, tz - 4, "east", ["Oom Miroh", "De Grote Lotus"], wall=False)


def ledenhof(b, cx, cz, f):
    """The old members' garden (south-west, inside the wall): three echoes of members from long ago at their stones, a
    still mirror pond, a chakra mat, a lost lantern."""
    x0, x1, z0, z1 = cx - 32, cx - 7, cz + 19, cz + 34
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not b.leeg(x, f - 1, z):
                b.set(x, f - 1, z, GRAS if _hash(x, z, 610) < 0.75 else TEGEL)
    for i, (x, z) in enumerate(((cx - 26, cz + 23), (cx - 19, cz + 23), (cx - 12, cz + 23)), 1):
        b.set(x, f, z - 2, TEGEL)
        b.set(x, f + 1, z - 2, "minecraft:white_concrete")
        b.set(x, f + 2, z - 2, "minecraft:pink_concrete")
        b.plek(f"lid_{i}", x, f, z, yaw=g.YAW["south"], wie="een oud lid van de Witte Lotus als echo (vertelt de geschiedenis)",
               kind="LOTUS_LID_ECHO")
    g.gedenksteen(b, "leden", cx - 19, f, cz + 31, "north", ["De oude leden"])
    b.prop("verdwaalde_lantaarn_4", cx - 30, f, cz + 32, wie="verdwaalde lantaarngeest (in de ledentuin)")
    b.plek("kennisvos_3", cx - 10, f, cz + 30, wie="een kennisvosje (sjouwt boekrollen)", wezen="guhs:kennisvosje")
    b.zone("ledenhof", x0, f, z0, x1, f + 4, z1, wie="de tuin van de oude leden")
    g.boom(b, x0 + 2, f, z0 + 2, 6, 3, ROZE, 611, hangers=False)
    g.spiegelvijver(b, "spiegelvijver_4", cx - 24, f, cz + 29)
    b.prop("chakra_6", cx - 14, f, cz + 27, wie="chakra 6: mediteren in de tuin van de oude leden")
    g.chakra_mat(b, cx - 14, f, cz + 27)


def heiligdom(b, cx, cz, f):
    """The sanctuary (north): a tall white hall; six pedestals for the tiles round the closed stone lotus, the Oude
    Reigerguh coiled asleep beside it, the founder at the door; behind the lotus the stairs down to the Lotusarchief."""
    x0, x1, z0, z1 = cx - 11, cx + 11, cz - 45, cz - 12

    def wand(x, y, z):
        if (x - x0) % 4 == 0 and (z - z0) % 4 == 0 or (x in (x0, x1) and z in (z0, z1)):
            return HOUT
        if (y - f) in (5, 6) and (x + z) % 2 == 0:
            return "minecraft:white_stained_glass"
        return TEGEL
    gebouw(b, x0, z0, x1, z1, f, 10, wand, STEEN, SPLANK, None)
    for x in range(x0, x1 + 1):                                   # the stepped roof
        for z in range(z0, z1 + 1):
            if (x - x0) * (x1 - x) * (z - z0) * (z1 - z) >= 0:
                if x0 + 2 <= x <= x1 - 2 and z0 + 2 <= z <= z1 - 2:
                    b.set(x, f + 11, z, PLANK)
                if x0 + 5 <= x <= x1 - 5 and z0 + 5 <= z <= z1 - 5:
                    b.set(x, f + 12, z, SPLANK)
                if x0 + 8 <= x <= x1 - 8 and z0 + 8 <= z <= z1 - 8:
                    b.set(x, f + 13, z, PLANK)
            if x in (x0, x1) or z in (z0, z1):
                if (x + z) % 2 == 0:
                    b.set(x, f + 11, z, ROZE)
    gat(b, cx - 3, f, z1, cx + 3, f + 5, z1)                    # the door, to the court
    for x in range(cx - 3, cx + 4):
        b.set(x, f - 1, z1, TEGEL)
    for z in range(z1 + 1, cz - 8):
        for x in range(cx - 3, cx + 4):
            b.set(x, f - 1, z, TEGEL)
    # the floor: a lotus of pink and white round the middle
    lx, lz = cx, cz - 30
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            d = math.hypot(x - lx, z - lz)
            hoek = math.atan2(z - lz, x - lx)
            if d < 8.5 and math.cos(hoek * 8) > 0.2 and d > 2:
                b.set(x, f - 1, z, "minecraft:pink_terracotta" if d < 5 else "minecraft:white_terracotta")
    # the stone lotus (closed): a column with petals of slab round its foot
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx or dz:
                b.set(lx + dx, f, lz + dz, "minecraft:quartz_slab", {"type": "bottom", "waterlogged": "false"})
    for y in range(f, f + 3):
        b.set(lx, y, lz, "minecraft:quartz_pillar", {"axis": "y"})
    b.set(lx, f + 3, lz, "minecraft:pink_concrete")
    b.prop("stenen_lotus", lx, f + 1, lz, wie="de gesloten stenen lotus (opent als alle tegels liggen)")
    # the six pedestals (6 = the White Lotus tile, right before the lotus)
    for i in range(6):
        th = math.pi / 2 + (i + 1) * math.tau / 6        # 6 -> south (towards the door)
        x, z = lx + round(math.cos(th) * 6), lz + round(math.sin(th) * 6)
        b.set(x, f, z, "minecraft:polished_diorite")
        b.set(x, f + 1, z, "minecraft:chiseled_quartz_block")
        b.prop(f"tegel_voetstuk_{i + 1}", x, f + 1, z, wie=("voetstuk 6: de Witte Lotus-tegel" if i == 5 else f"voetstuk {i + 1}: een tegel van een meester"))
    b.plek("reiger", lx - 3, f, lz - 1, yaw=g.YAW["east"], wie="de Oude Reigerguh, opgerold om de stenen lotus, slaapt", wezen="guhs:oude_reigerguh",
           los=True)
    b.plek("lid_stichter", cx + 6, f, z1 - 3, yaw=g.YAW["south"], wie="de stichter van de Witte Lotus (uit de tijd van Roku-guh), echo",
           kind="LOTUS_STICHTER_ECHO")
    g.gedenksteen(b, "heiligdom", cx - 6, f, z1 - 3, "south", ["Het heiligdom"])
    b.zone("heiligdom", x0 + 1, f, z0 + 1, x1 - 1, f + 9, z1 - 1, wie="het heiligdom van de Witte Lotus")
    for x, z in ((x0 + 3, z0 + 3), (x1 - 3, z0 + 3), (x0 + 3, z1 - 3), (x1 - 3, z1 - 3), (cx, z0 + 3), (cx - 7, lz), (cx + 7, lz)):
        hanglamp(b, x, f + 8, z)
    for x, z in ((cx - 9, cz - 20), (cx + 9, cz - 20), (cx - 9, cz - 38), (cx + 9, cz - 38)):
        b.set(x, f, z, "minecraft:candle", KAARS)
    # the stairs down to the archive (behind the lotus, going south under the floor)
    for k in range(12):
        z, y = cz - 42 + k, f - 1 - k
        for x in (cx - 1, cx, cx + 1):
            b.set(x, y, z, TRAP, {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
            for yy in range(y + 1, y + 4):
                b.s.blocks.pop((x, yy, z), None)
            for yy in range(y - 2, y):
                b.set(x, yy, z, STEEN)
        for x in (cx - 2, cx + 2):
            for yy in range(y - 1, y + 4):
                if yy < f - 1:
                    b.set(x, yy, z, TEGEL)
    for z in range(cz - 42, cz - 38):
        for x in (cx - 2, cx + 2):
            b.set(x, f, z, "minecraft:birch_fence")
    for x in (cx - 1, cx, cx + 1):
        b.set(x, f, cz - 38, "minecraft:birch_fence")
    b.prop("archief_ingang", cx, f, cz - 43, wie="de trap naar het Lotusarchief (achter de stenen lotus)", los=True)
    lotusarchief(b, cx, cz, f)
    for x, z in ((cx - 7, cz - 40), (cx + 7, cz - 40)):
        b.mistreiger.append((x, z))


def lotusarchief(b, cx, cz, f):
    """Under the sanctuary: the Lotusarchief. Shelves of scrolls, lecterns, a big map table of Pai Sho, lanterns; a veil of
    mist at the foot of the stairs."""
    y0 = f - 12                                   # the floor you stand on
    x0, x1, z0, z1 = cx - 11, cx + 11, cz - 30, cz - 13
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(y0 - 1, y0 + 8):
                if x in (x0, x1) or z in (z0, z1) or y in (y0 - 1, y0 + 7):
                    b.set(x, y, z, STEEN if y != y0 - 1 else TEGEL)
                else:
                    b.s.blocks.pop((x, y, z), None)
    gat(b, cx - 1, y0, z0, cx + 1, y0 + 2, z0)
    for x in (cx - 1, cx, cx + 1):
        for y in range(y0, y0 + 3):
            b.set(x, y, z0 + 1, NEVEL)                 # the veil of mist (you walk through it)
    for z in range(z0 + 2, z1):                       # shelves along the walls
        for y in range(y0, y0 + 5):
            for x in (x0 + 1, x1 - 1):
                b.set(x, y, z, "minecraft:bookshelf" if (z + y) % 4 else "minecraft:chiseled_bookshelf",
                      None if (z + y) % 4 else {"facing": "east" if x == x0 + 1 else "west", "slot_0_occupied": "true", "slot_1_occupied": "true",
                                                 "slot_2_occupied": "false", "slot_3_occupied": "true", "slot_4_occupied": "false", "slot_5_occupied": "true"})
    for x in range(cx - 3, cx + 4):                   # the map table
        for z in range(cz - 23, cz - 19):
            b.set(x, y0, z, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
    for x, z, facing in ((cx - 6, cz - 26, "north"), (cx + 6, cz - 26, "north"), (cx - 6, cz - 16, "south"), (cx + 6, cz - 16, "south")):
        b.set(x, y0, z, "minecraft:lectern", {"facing": facing, "has_book": "true", "powered": "false"})
    for x, z in ((cx - 6, cz - 21), (cx + 6, cz - 21), (cx, cz - 26), (cx, cz - 15), (cx - 8, cz - 27), (cx + 8, cz - 15)):
        hanglamp(b, x, y0 + 6, z)
    b.plek("kennisvos_1", cx - 4, y0, cz - 17, wie="een kennisvosje in het archief (sjouwt boekrollen)", wezen="guhs:kennisvosje")
    b.plek("kennisvos_2", cx + 4, y0, cz - 26, wie="een kennisvosje in het archief", wezen="guhs:kennisvosje")
    g.gedenksteen(b, "archief", cx - 3, y0, z1 - 1, "north", ["Lotusarchief"])
    b.zone("lotusarchief", x0 + 1, y0, z0 + 1, x1 - 1, y0 + 6, z1 - 1, wie="het Lotusarchief (opent als de reiger ontwaakt)")


# =====================================================================================================================
# the Top der Voorgangers: the Hall moved here
# =====================================================================================================================
def top_voorgangers(b):
    cx, cz, f, r = TOP["x"], TOP["z"], TOP["f"], TOP["r"]
    g.eiland(b, cx, cz, f, r, 18, 505)
    for x in range(cx - 4, cx + 5):                                   # the meeting circle
        for z in range(cz - 4, cz + 5):
            if math.hypot(x - cx, z - cz) <= 4.3:
                b.set(x, f - 1, z, TEGEL if (x + z) % 2 else STEEN)
    b.plek("roku", cx - 3, f, cz - 2, yaw=g.YAW["south"], wie="Roku-guh als geest (h4: raad voor de finale)", kind="ROKU_GUH")
    b.plek("kyoshi", cx + 3, f, cz - 2, yaw=g.YAW["south"], wie="Kyoshi-guh als geest (h4: raad voor de finale)", kind="KYOSHI_GUH")
    b.prop("fang_landing", cx, f, cz + 6, wie="waar Fang je neerzet")
    b.prop("top_landing", cx, f, cz + 7, wie="waar Fang landt (h4)")
    b.prop("top_speler", cx, f, cz + 3, wie="waar je staat voor Roku-guh en Kyoshi-guh")
    b.prop("plek_top_voorgangers", cx, f, cz + 9)
    g.meditatiesteen(b, "top_voorgangers", cx + 6, f, cz + 7)
    g.gedenksteen(b, "top", cx - 6, f, cz + 7, "south", ["Top der", "Voorgangers"])
    b.prop("verdwaalde_lantaarn_5", cx + 9, f, cz + 11, wie="verdwaalde lantaarngeest (heel hoog)")
    # the Hall of the Predecessors: the eight stones in an arc to the north, white pillars behind them, the fresco wall
    for i, id in enumerate(g.VOORGANGERS):
        th = math.pi + 0.3 + i * (math.pi - 0.6) / 7
        x, z = cx + round(math.cos(th) * 12), cz + round(math.sin(th) * 12)
        dx, dz = cx - x, cz - z
        facing = ("east" if dx > 0 else "west") if abs(dx) > abs(dz) else ("south" if dz > 0 else "north")
        b.set(x, f, z, TEGEL)
        b.set(x, f + 1, z, GEDENK, {"facing": facing})
        b.sign(x, f + 2, z, facing, g.VOORGANGER_BORD[id], wall=False)
        b.prop(f"lore_{id}", x, f + 1, z, wie=f"gedenksteen: {tekst.LORE[id][0]}", los=True)
        fx, fz = g.DIRS[facing]
        b.set(x + fx, f, z + fz, "minecraft:candle", KAARS)
        px, pz = cx + round(math.cos(th) * 15.5), cz + round(math.sin(th) * 15.5)
        for y in range(f, f + 6):
            b.set(px, y, pz, HOUT, {"axis": "y"})
        b.set(px, f + 6, pz, LANTAARN, LAMP_AAN)
    wz = cz - 19
    for x in range(cx - 8, cx + 9):
        for y in range(f, f + 13):
            b.set(x, y, wz, TEGEL)
        b.zet(x, f - 1, wz, STEEN)
        b.zet(x, f - 2, wz, STEEN)
    g.fresco(b, cx - 7, f + 1, wz + 1)
    b.set(cx, f, wz + 2, TEGEL)
    b.sign(cx, f, wz + 3, "south", ["Raava en Vaatu?", "Nooit van", "gehoord."], wall=True)
    b.prop("fresco", cx, f, wz + 4, wie="het fresco: twee geesten in elkaar gedraaid, licht en donker")
    b.zone("hal", cx - 17, f, wz + 1, cx + 17, f + 6, cz - 5, wie="de Hal van de Voorgangers (verhuisd naar de Top)")
    b.prop("chakra_7", cx, f, cz - 7, wie="chakra 7 (de kruin): mediteren op de Top")
    g.chakra_mat(b, cx, f, cz - 7)
    b.camera("top", (cx, f + 4, cz + 15), (cx, f + 1, cz - 2), zicht=["roku", "kyoshi", "top_speler"],
             wie="de camera van de Top: van achter de speler naar Roku-guh en Kyoshi-guh")
    for k in range(6):                                                # lanterns round the south half
        th = 0.35 + k * (math.pi - 0.7) / 5
        x, z = cx + round(math.cos(th) * 17), cz + round(math.sin(th) * 17)
        if not b.leeg(x, f - 1, z) and b.leeg(x, f, z):
            g.lantaarnpaal(b, x, f, z)
    b.zone("top", cx - r, f, cz - r, cx + r, f + 6, cz + r, wie="de top")
    b.zone("zone_top_voorgangers", cx - r - 6, f - 20, cz - r - 6, cx + r + 6, f + 16, cz + r + 6)


# =====================================================================================================================
# the Hemeltrap: from the compound (east) up to the Top
# =====================================================================================================================
def hemeltrap(b):
    cx, cz, f = LOTUS["x"], LOTUS["z"], LOTUS["f"]
    sx, sz, sr = cx + 82, cz - 28, 8
    for y in range(f - 6, TOP["f"] + 6):
        b.set(sx, y, sz, NEVEL)
    b.set(sx, f - 7, sz, STEEN)
    b.los_cellen |= {(sx, y, sz) for y in range(f - 7, TOP["f"] + 6)}
    start, voor = (cx + 16, cz - 12), (cx + 64, cz - 16)
    # fix4-lotus (playtest 3: "in het gebouw van de echo van Jeong Jeong-guh kan ik het huis niet uit, de trap loopt voor
    # de voordeur"): the stair began to climb at once, right along Jeong Jeong-guh's front door (cx + 22..24, cz - 15), so
    # outside the door it already stood 1.5 to 2 blocks up. Now it runs flat at the courtyard's height past the whole wing
    # (its east wall is cx + 34) and only then begins to climb.
    vlak = (cx + 38, cz - 12)
    vlak_route = g.pad(b, [start, vlak], f, f, half=1, lantaarn_elke=0, lotus_elke=0, seed=404)
    start = vlak
    th0 = math.atan2(voor[1] - sz, voor[0] - sx)
    hoogte = TOP["f"] - f
    omwentelingen = (hoogte / 0.42) / (2 * math.pi * sr)
    th_eind = -math.pi / 2
    th1 = th_eind - 2 * math.pi * math.ceil(omwentelingen)
    while th0 - th1 < 2 * math.pi * omwentelingen:
        th1 -= 2 * math.pi
    spiraal = [start, voor] + [(sx + math.cos(th0 + (th1 - th0) * i / 240) * sr, sz + math.sin(th0 + (th1 - th0) * i / 240) * sr)
                               for i in range(241)]
    route = g.pad(b, spiraal, f, TOP["f"], half=1, lantaarn_elke=18, lotus_elke=0, seed=405)
    b.hemeltrap_pad = b.paden[-1]
    eind = spiraal[-1]
    brug = g.pad(b, [eind, (eind[0] - 14, TOP["z"] + 12), (TOP["x"] + 20, TOP["z"] + 3)], TOP["f"], TOP["f"], half=1, lantaarn_elke=0,
                 lotus_elke=0, seed=406)
    b.pad("hemeltrap", vlak_route + route + brug, wie="de Hemeltrap: van de Witte Lotus omhoog naar de Top der Voorgangers")
    return [(cx + 16, cz - 12, 3), (start[0], start[1], 3), (cx + 45, cz - 14, 6)]


# =====================================================================================================================
# the Trap der meesters: five giant statues of the masters along the road from the Meditatiecirkel
# =====================================================================================================================
def meesterbeeld(b, sx, y, sz, facing, meester, seed):
    """A giant, half overgrown statue of a master with guh ears, on a plinth on its own little rock, looking at the road.
    y = the floor the statue stands on; facing = where it looks."""
    fx, fz = g.DIRS[facing]
    vx, vz = -fz, fx                                   # sideways

    def w(u, h, v):
        return sx + u * fx + v * vx, y + h, sz + u * fz + v * vz
    g.eiland(b, sx, sz, y, 6, 12, seed, top=STEEN, ruw=0.08)
    for u in range(-3, 4):
        for v in range(-3, 4):
            x, yy, z = w(u, -1, v)
            b.set(x, yy, z, TEGEL if (u + v) % 2 else STEEN)
    lijf = {}
    buik = 3.4 if meester == "miroh" else 3.0
    for u in range(-4, 5):
        for v in range(-4, 5):
            for h in range(0, 18):
                if ((u / 2.6) ** 2 + ((h - 5) / 5.2) ** 2 + (v / buik) ** 2) <= 1 or ((u / 2.6) ** 2 + ((h - 12) / 2.9) ** 2 + (v / 2.9) ** 2) <= 1:
                    lijf[(u, h, v)] = STEEN
    for v in (-2, 2):                                   # the ears
        for h, br in ((14, 1), (15, 1), (16, 0)):
            for du in range(-br, br + 1):
                for dv in range(0 if v > 0 else -br, br + 1 if v > 0 else 1):
                    lijf[(du, h, v + dv)] = STEEN
    for v in (-1, 1):                                   # the eyes, carved darker
        lijf[(2, 12, v)] = "minecraft:polished_deepslate"
    # half broken: one piece of the body is gone (before the paws and what they hold)
    kant = {"jeong_jeong": -3, "piandao": 3}.get(meester, 3 if _hash(seed, 3) < 0.5 else -3)
    breuk = (int(_hash(seed, 1) * 3) - 1, 6 + int(_hash(seed, 2) * 6), kant)
    for c in list(lijf):
        if math.dist(c, breuk) < 1.8 and lijf[c] == STEEN:
            del lijf[c]
    if meester == "bumi":                               # arms up, a boulder over his head
        for v in (-3, 3):
            for h in range(8, 19):
                lijf[(0, h, v)] = STEEN
        for u in range(-2, 3):
            for v in range(-3, 4):
                for h in range(18, 23):
                    if (u / 2.3) ** 2 + (v / 3.4) ** 2 + ((h - 20) / 2.3) ** 2 <= 1:
                        lijf[(u, h, v)] = "minecraft:mossy_cobblestone"
    elif meester == "pakguh":                           # a long beard and a shawl like a wave
        for h in range(7, 11):
            lijf[(3, h, 0)] = "minecraft:white_concrete"
        for v in range(-3, 4):
            if (2, 9, v) in lijf or (1, 9, v) in lijf:
                lijf[(2 if (2, 9, v) in lijf else 1, 9, v)] = "minecraft:prismarine"
    elif meester == "piandao":                          # a sword planted before him, both paws on the hilt
        for h in range(0, 9):
            lijf[(3, h, -2)] = "minecraft:iron_bars"
        lijf[(3, 9, -2)] = "minecraft:gold_block"
        lijf[(3, 9, -3)] = "minecraft:gold_block"
        lijf[(3, 9, -1)] = "minecraft:gold_block"
        lijf[(3, 10, -2)] = "minecraft:gold_block"
        lijf[(2, 9, -2)] = STEEN
        lijf[(1, 9, -2)] = STEEN
        lijf[(1, 9, -1)] = STEEN
    elif meester == "jeong_jeong":                      # a beard to his knees, a flame in one paw
        for h in range(3, 11):
            lijf[(3, h, 0)] = "minecraft:white_concrete"
        for v in range(3, 5):
            lijf[(1, 8, v)] = STEEN
        lijf[(1, 9, 4)] = "minecraft:shroomlight"
        lijf[(1, 10, 4)] = "minecraft:orange_stained_glass"
    elif meester == "miroh":                            # a teapot held before his round belly
        lijf[(3, 5, 0)] = STEEN
        lijf[(4, 5, 0)] = STEEN
        lijf[(4, 6, 0)] = "minecraft:decorated_pot"
    # overgrown: moss and glowing leaves on what is left
    for (u, h, v), n in lijf.items():
        x, yy, z = w(u, h, v)
        if n == STEEN and (u, h + 1, v) not in lijf and _hash(x, yy, z, seed) < 0.45:
            n = "minecraft:moss_block"
        elif n == STEEN and _hash(x, yy, z, seed, 2) < 0.12:
            n = "minecraft:mossy_stone_bricks"
        b.set(x, yy, z, n, {"facing": facing, "waterlogged": "false", "cracked": "true"} if n == "minecraft:decorated_pot" else None)
    for (u, h, v), n in lijf.items():                   # glowing leaves hanging off the shoulders and the head
        if n != STEEN or (u, h + 1, v) in lijf:
            continue
        x, yy, z = w(u, h, v)
        if _hash(x, yy, z, seed, 5) < 0.3:
            b.zet(x, yy + 1, z, CYAAN if _hash(x, z, seed) < 0.6 else ROZE)
    return w(3, 0, 0)


def trap_der_meesters(b, info):
    """The five statues along the road (the path made just before: info = its b.paden entry), alternating sides."""
    midden, totaal = info["midden"], info["totaal"]
    for i, meester in enumerate(MEESTERS):
        doel = 12 + (totaal - 24) * i / 4
        px, pz, h2, nx, nz, dd = min(midden, key=lambda m: abs(m[5] - doel))
        kant = 1 if i % 2 == 0 else -1
        y = h2 // 2 + (1 if h2 % 2 else 0)
        sx, sz = round(px + nx * kant * 10), round(pz + nz * kant * 10)
        kijk = (-nx * kant, -nz * kant)
        facing = ("east" if kijk[0] > 0 else "west") if abs(kijk[0]) > abs(kijk[1]) else ("south" if kijk[1] > 0 else "north")
        voor = meesterbeeld(b, sx, y, sz, facing, meester, 960 + i)
        # tiles from the road to the plinth
        fx, fz = g.DIRS[facing]
        x, z = voor[0] + fx, voor[2] + fz
        for _ in range(10):
            if not b.leeg(x, y - 1, z) and b.s.blocks[(x, y - 1, z)][0] in (TEGEL, PLAAT):
                break
            b.zet(x, y - 1, z, TEGEL)
            b.zet(x, y - 2, z, STEEN)
            x, z = x + fx, z + fz
        b.prop(f"meesterbeeld_{i + 1}", voor[0], voor[1], voor[2], wie=f"het reuzenbeeld van {tekst.MEESTER_NAAM[meester]} aan de Trap der meesters")
    b.pad("trap_der_meesters", info["punten"], wie="de Trap der meesters: de weg naar de Witte Lotus tussen de vijf beelden")
    eerste = midden[min(range(len(midden)), key=lambda k: abs(midden[k][5] - 6))]
    px, pz, h2, nx, nz, dd = eerste
    lx, lz = round(px - nx * 3), round(pz - nz * 3)
    y = h2 // 2 + (1 if h2 % 2 else 0)
    b.zet(lx, y - 1, lz, TEGEL)
    b.zet(lx, y - 2, lz, STEEN)
    for k in range(1, 3):
        b.zet(round(px - nx * k), y - 1, round(pz - nz * k), TEGEL)
    b.set(lx, y, lz, GEDENK, {"facing": "north"})
    b.s.blocks.pop((lx, y + 1, lz), None)
    b.prop("lore_trap_meesters", lx, y, lz, wie=f"gedenksteen: {tekst.LORE['trap_meesters'][0]}")
