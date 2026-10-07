"""
einde2 (bouw_water): what stands ON the ground of the Noordelijke Waterstam (einde2_bouw_water_grond.py): the dressing of
the ice wall and its gate, the bridges, the houses and igloos of the lower city, the market, Meester Pakguh's training
square with the wave pool, the palace of hoofdman Arnook-guh, the spirit oasis, the sled run's start and finish, the
landing place with the team's camp and the harbour. Zhao's ship and the fleet are einde2_bouw_water_schip.py.

Every function takes the build `b` (kit.Bouw, already holding the ground). Nothing here digs deeper than the shell of the
ground. Block states are written as they must look (the tiles are placed without neighbour updates); fences, panes and
walls get their arms from b.connect() at the end.
"""
import math

from features import guheinde_bouw as gb
from features.einde2_bouw_water_grond import (BASSIN, DWARS, EILANDJE, GROOT, H, IJSSTEEN, KANSEL, MUUR, MUUR_X, MUUR_Z, MX, OASE, P0, PALEIS,
                                              PIER, POORT_X, RING_N, RING_Z, SEA, STAD1, STAD2, T0, T1, T2, TOREN_O, TOREN_W, TRAP_GROOT, VIJVER)

IJS = IJSSTEEN
TRAP, PLAAT = IJS + "_trap", IJS + "_plaat"
LANTAARN = "guhs:einde2_bouw_water_ijslantaarn"
TEKEN = "guhs:einde2_bouw_water_stamteken"
PAK, BLAUW, SNEEUW = "packed_ice", "blue_ice", "snow_block"
TEGEL = "guhs:knuffelbad_badtegel"
PAAL = "prismarine_wall"
RUIT = "light_blue_stained_glass_pane"
GLAS = "light_blue_stained_glass"
WATER = {"level": "0"}
KIST_HUIS, KIST_PALEIS = "guhs:chests/einde2_bouw_water_huis", "guhs:chests/einde2_bouw_water_paleis"
# the four streets that cross every canal of the lower city on a bridge (x ranges), and the bridges over the great canal
STEGEN = [(62, 65), (92, 95), (129, 132), (159, 162)]
RIJ_N, RIJ_Z = (110, 115), (119, 124)        # z ranges of the two rows of houses (they rise straight out of the cross canal)
UIT = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}

# the emblem of the Waterstam (a crescent moon over three waves), 11 x 11: on the wall above the gate and in the throne hall
EMBLEEM = ["...RRRRR...",
           "..RDDDDDR..",
           ".RDDDWWDDR.",
           ".RDDWWDDDR.",
           "RDDWWDDDDDR",
           "RDDWWDDDWDR",
           "RDDDWWWWWDR",
           "RDDDDDDDDDR",
           ".RWWDWWDWR.",
           "..RDDDDDR..",
           "...RRRRR..."]
EMBLEEM_BLOK = {"R": "prismarine_bricks", "D": "lapis_block", "W": "snow_block"}


def spiegel(x):
    """The same spot on the other side of the city's axis."""
    return 2 * MX - x


def trap(b, x, y, z, facing, half="bottom", blok=TRAP):
    b.set(x, y, z, blok, gb.stair(facing, half))


def plaat(b, x, y, z, t="bottom", blok=PLAAT):
    b.set(x, y, z, blok, gb.slab(t))


def lantaarnpaal(b, x, y, z, hoog=2):
    """A post with an ice lantern on it; y = the first block above the floor."""
    for i in range(hoog):
        b.set(x, y + i, z, PAAL)
    b.set(x, y + hoog, z, LANTAARN)


def ladder(b, x, y0, y1, z, facing):
    for y in range(y0, y1 + 1):
        b.set(x, y, z, "ladder", {"facing": facing, "waterlogged": "false"})


def bed(b, x, y, z, facing, kleur="light_blue"):
    """The foot at (x, y, z), the head one further in `facing`."""
    dx, dz = UIT[facing]
    b.set(x, y, z, f"{kleur}_bed", {"facing": facing, "part": "foot", "occupied": "false"})
    b.set(x + dx, y, z + dz, f"{kleur}_bed", {"facing": facing, "part": "head", "occupied": "false"})


def ton(b, x, y, z):
    b.set(x, y, z, "barrel", {"facing": "up", "open": "false"})


def vuurtje(b, x, y, z, soul=False):
    b.set(x, y, z, "soul_campfire" if soul else "campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})


def ijsguh(b, x, y, z, facing):
    """A guh carved out of snow, three wide: (x, y, z) = the middle of its front, y = the first block above the floor."""
    ox, oz = UIT[facing]
    zx, zz = (0, 1) if ox else (1, 0)
    for k in (-1, 0, 1):
        for diep in (0, 1):
            for dy in (0, 1):
                b.set(x + zx * k - ox * diep, y + dy, z + zz * k - oz * diep, PAK if (k, diep, dy) == (0, 0, 0) else SNEEUW)
    for k in (-1, 1):
        b.set(x + zx * k, y + 2, z + zz * k, SNEEUW)                                   # the ears
        b.set(x + zx * k + ox, y + 1, z + zz * k + oz, "polished_blackstone_button", {"face": "wall", "facing": facing, "powered": "false"})
    b.set(x + ox, y, z + oz, "polished_blackstone_button", {"face": "wall", "facing": facing, "powered": "false"})   # the belly button


def slee(b, x, y, z):
    """A little sled parked on a quay (two long, along x)."""
    b.set(x, y, z, "spruce_slab", gb.slab("bottom"))
    b.set(x + 1, y, z, "spruce_stairs", gb.stair("east"))


def rustvuur(b, x, y, z):
    """What marks a rest point: a little blue fire in a ring of ice bricks' slabs."""
    vuurtje(b, x, y, z, soul=True)


# =====================================================================================================================
# the ice wall
# =====================================================================================================================
def muur(b):
    z0, z1 = MUUR_Z
    x0, x1 = MUUR_X
    # the emblem above the gate, on the front of the gatehouse
    b.art(EMBLEEM, EMBLEEM_BLOK, "z", z1 + 2, MX - 5, SEA + 19)
    # ice lanterns: in the vault of the gate, in the walkway, on the towers
    for z in range(z0 + 1, z1 + 2, 4):
        b.set(MX, SEA + 8, z, LANTAARN)
    for x in range(x0 + 10, x1 - 6, 9):
        if not (TOREN_W[0] - 1 <= x <= TOREN_O[1] + 1):
            b.set(x, MUUR, z0 + 4, LANTAARN)
    for xa, xb in (TOREN_W, TOREN_O):
        xm = (xa + xb) // 2
        b.set(xm, MUUR + 12, z0 + 4, LANTAARN)                    # the tip of the snow cap
        b.set(xm, MUUR + 13, z0 + 4, "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
        b.set(xm, MUUR + 7, z0 + 4, LANTAARN)                     # in the ceiling of the passage
        for x in (xa, xb):                                        # banners down the front of the towers
            for y in range(MUUR + 2, MUUR + 8):
                b.set(x, y, z1 + 4, "blue_wool" if (y - MUUR) % 3 else "white_wool")
            b.set(x, MUUR + 8, z1 + 4, TEKEN)
    for x in (x0 + 2, x1 - 2):                                    # the end towers
        b.set(x, MUUR + 10, z0 + 4, LANTAARN)
        b.set(x, MUUR + 4, z0 + 4, LANTAARN)
    for x in (MX - 5, MX + 5):                                    # the Golfkansel: two lanterns on its parapet towards the city
        b.set(x, KANSEL + 2, z0, LANTAARN)
    # a cornice of slabs under the sea-side parapet, and lanterns frozen into the face of the wall
    for x in range(x0 + 7, x1 - 6):
        if TOREN_W[0] - 1 <= x <= TOREN_O[1] + 1 or not b.gr.leeg(x, MUUR, z1 + 1):
            continue
        plaat(b, x, MUUR, z1 + 1, "top")
    for x in range(x0 + 12, x1 - 6, 8):
        if not (TOREN_W[0] - 2 <= x <= TOREN_O[1] + 2) and b.gr.leeg(x, SEA + 13, z1 + 1):
            b.set(x, SEA + 13, z1, LANTAARN)
    for x in range(x0 + 12, x1 - 6, 8):                           # and on the city side
        if not (TOREN_W[0] - 2 <= x <= TOREN_O[1] + 2) and b.gr.leeg(x, SEA + 12, z0 - 1):
            b.set(x, SEA + 12, z0, LANTAARN)
    # two guhs of snow welcome whoever comes through the gate
    ijsguh(b, MX - 10, T0 + 1, 133, "south")
    ijsguh(b, MX + 10, T0 + 1, 133, "south")
    # proper steps: the stairs up the inside of the wall, the three steps in each gate tower
    for kant, facing in ((1, "east"), (-1, "west")):
        xs = 127 if kant == 1 else 97
        for i in range(18):
            for z in (z0 - 2, z0 - 1):
                trap(b, xs + kant * i, T0 + 1 + i, z, facing)
    for i in range(3):
        for z in range(z0 + 3, z0 + 7):
            trap(b, TOREN_W[1] - 2 + i, MUUR + 1 + i, z, "east")
            trap(b, TOREN_O[0] + 2 - i, MUUR + 1 + i, z, "west")
    # the gate: a bell to ring, and what the guard has to say about it
    b.set(POORT_X[1] + 2, T0 + 3, z1 + 4, "bell", {"attachment": "single_wall", "facing": "north", "powered": "false"})
    b.sign(POORT_X[1] + 3, T0 + 2, z1 + 4, "south", ["Noordelijke", "Waterstam", "Voeten vegen,", "njeg"])
    b.sign(POORT_X[1] + 4, T0 + 2, z1 + 4, "south", ["Poort open voor", "wie kan water-", "sturen. Anders:", "aanbellen"])
    b.sign(POORT_X[0] - 2, T0 + 2, z1 + 4, "south", ["Vuurnatie?", "Wij zijn er", "niet. Kom later", "maar terug."])
    # on the wall top
    b.sign(MX - 4, KANSEL + 2, z1 + 1, "north", ["De Golfkansel", "Mooi uitzicht.", "Niet spetteren", "naar beneden"], wall=False)
    b.sign(x0 + 30, MUUR + 2, z1, "north", ["Uitkijkpost", "Zie je zwarte", "sneeuw? Dan", "heel hard roepen"], wall=False)


# =====================================================================================================================
# bridges
# =====================================================================================================================
def brug(b, as_, lo, hi, oever0, oever1):
    """An arched ice bridge. as_ = the axis you walk along; lo..hi = how wide it is on the other axis; oever0 / oever1 = the
    last land cell on either side. Three steps up, a deck two clear blocks above the water (a guh in a boat fits under it),
    three steps down; a low rail over the water when it is wide enough."""
    op, af = ("east", "west") if as_ == "x" else ("south", "north")

    def zet(a, c, y, naam, props=None):
        if as_ == "x":
            b.set(a, y, c, naam, props)
        else:
            b.set(c, y, a, naam, props)

    for c in range(lo, hi + 1):
        for i in range(3):
            for a, facing in ((oever0 - 2 + i, op), (oever1 + 2 - i, af)):
                for y in range(T0 + 1, T0 + 1 + i):
                    zet(a, c, y, IJS)
                zet(a, c, T0 + 1 + i, TRAP, gb.stair(facing))
        for a in range(oever0 + 1, oever1):
            zet(a, c, T0 + 3, IJS)
    midden_a, midden_c = (oever0 + oever1) // 2, (lo + hi) // 2
    zet(midden_a, midden_c, T0 + 3, LANTAARN)
    if hi - lo >= 3:
        for c in (lo, hi):
            for a in range(oever0 + 1, oever1):
                zet(a, c, T0 + 4, PAAL)
            for a in (oever0 + 1, oever1 - 1):
                zet(a, c, T0 + 5, LANTAARN)


def bruggen(b):
    for lo, hi in STEGEN:
        brug(b, "z", lo, hi, RING_N[2] - 1, RING_N[3] + 1)
        brug(b, "z", lo, hi, DWARS[2] - 1, DWARS[3] + 1)
        brug(b, "z", lo, hi, RING_Z[2] - 1, RING_Z[3] + 1)
    brug(b, "x", 113, 115, GROOT[0] - 1, GROOT[1] + 1)
    brug(b, "x", 121, 124, GROOT[0] - 1, GROOT[1] + 1)


# =====================================================================================================================
# houses
# =====================================================================================================================
BEDDEN = ["light_blue", "blue", "white", "cyan"]
TAPIJT = ["light_blue_carpet", "blue_carpet", "white_carpet", "cyan_carpet"]


def huis(b, x0, z0, w, d, y, deur, r, verdiepingen=2, kist=False, bord=None):
    """A house of ice bricks: `verdiepingen` storeys of three clear blocks, a flat roof with a rim of snow, a spruce door on
    the `deur` side, windows, a ladder up, and something to live with. y = the floor block (the ground it stands on).
    Returns the cell in front of its door."""
    x1, z1 = x0 + w - 1, z0 + d - 1
    b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, "spruce_planks")
    hoeken = {(x0, z0), (x0, z1), (x1, z0), (x1, z1)}
    for v in range(verdiepingen):
        ya = y + 1 + v * 4
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    for yy in range(ya, ya + 3):
                        b.set(x, yy, z, PAK if (x, z) in hoeken else IJS)
                    lang = (x not in (x0, x1) and (x - x0) % 2 == 0 and 2 <= x - x0 <= w - 3) or \
                           (z not in (z0, z1) and (z - z0) % 2 == 0 and 2 <= z - z0 <= d - 3)
                    if lang:
                        b.set(x, ya + 1, z, RUIT)
        b.fill(x0, ya + 3, z0, x1, ya + 3, z1, IJS)
        if v < verdiepingen - 1:
            b.fill(x0 + 1, ya + 3, z0 + 1, x1 - 1, ya + 3, z1 - 1, "spruce_planks")
    top = y + verdiepingen * 4
    # the roof: a rim of ice-brick slabs with snow on the corners, and a blanket of snow in the middle
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, z) in hoeken:
                b.set(x, top + 1, z, SNEEUW)
            elif x in (x0, x1) or z in (z0, z1):
                plaat(b, x, top + 1, z)
            elif r.random() < 0.45:
                b.set(x, top, z, SNEEUW)
    soort = r.random()
    if soort < 0.35 and w >= 9 and d >= 6:                        # a mound of snow on the roof
        mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
        for ddx in range(-2, 3):
            for ddz in range(-2, 3):
                for ddy in range(0, 3):
                    if math.sqrt(ddx * ddx + ddy * ddy * 1.6 + ddz * ddz) <= 2.3 and z0 < mz + ddz < z1:
                        b.set(mx + ddx, top + 1 + ddy, mz + ddz, SNEEUW)
    elif soort < 0.65:                                            # a chimney with a fire in it: somebody is home
        cx_, cz_ = x0 + 2, z1 - 2
        b.set(cx_, top + 1, cz_, IJS)
        b.set(cx_, top + 2, cz_, IJS)
        vuurtje(b, cx_, top + 3, cz_)
    # the door, a lantern above it
    if deur in ("north", "south"):
        dx, dz = x0 + w // 2, (z0 if deur == "north" else z1)
    else:
        dx, dz = (x0 if deur == "west" else x1), z0 + d // 2
    gb.door(b, dx, y + 1, dz, gb.OPP[deur], wood="spruce")
    b.set(dx, y + 3, dz, LANTAARN)
    ox, oz = UIT[deur]
    # inside: a light in the ceiling of every storey, a ladder in a back corner (through the floors)
    bx, bz = (x1 - 1 if dx - x0 < w // 2 + 1 else x0 + 1), (z1 - 1 if deur == "north" else z0 + 1)
    if deur in ("east", "west"):
        bx, bz = (x1 - 1 if deur == "west" else x0 + 1), z0 + 1
    for v in range(verdiepingen):
        b.set((x0 + x1) // 2, y + 4 + v * 4, (z0 + z1) // 2, LANTAARN)
    if verdiepingen > 1:
        muur_x = bx + (1 if bx == x1 - 1 else -1)
        ladder(b, bx, y + 1, y + 1 + (verdiepingen - 1) * 4, bz, "west" if muur_x > bx else "east")
        for v in range(1, verdiepingen):
            b.set(bx, y + v * 4, bz, "ladder", {"facing": "west" if muur_x > bx else "east", "waterlogged": "false"})
    # downstairs: a stove, a barrel, a table; upstairs (or in the other corner): the bed
    binnen = [(x, z) for x in range(x0 + 1, x1) for z in range(z0 + 1, z1) if (x, z) != (bx, bz) and (x, z) != (dx - ox, dz - oz)]
    hoek = [(x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)]
    hoek = [c for c in hoek if c in binnen and abs(c[0] - (dx - ox)) + abs(c[1] - (dz - oz)) > 1]
    r.shuffle(hoek)
    meubel = ["smoker", "barrel", "crafting_table"]
    for c, m in zip(hoek, meubel):
        if m == "smoker":
            b.set(c[0], y + 1, c[1], "smoker", {"facing": gb.OPP[deur] if deur in ("north", "south") else deur, "lit": "false"})
        elif m == "barrel":
            ton(b, c[0], y + 1, c[1])
        else:
            b.set(c[0], y + 1, c[1], m)
    for _ in range(3):
        c = r.choice(binnen)
        if b.get(c[0], y + 1, c[1]) is None:
            b.set(c[0], y + 1, c[1], r.choice(TAPIJT))
    yb = y + 1 + (verdiepingen - 1) * 4
    kant = "east" if bx == x0 + 1 else "west"                     # the bed stands against the wall across from the ladder
    bed_x = x1 - 2 if kant == "east" else x0 + 2
    bed_z = z1 - 1 if bz == z0 + 1 else z0 + 1
    if verdiepingen == 1:
        bed_z = z0 + 1 if deur == "south" else z1 - 1
    if b.get(bed_x, yb, bed_z) is None and b.get(bed_x + UIT[kant][0], yb, bed_z) is None:
        bed(b, bed_x, yb, bed_z, kant, r.choice(BEDDEN))
    if kist:
        kx = x0 + 1 if bed_x > (x0 + x1) // 2 else x1 - 1
        kz = bed_z
        if b.get(kx, yb, kz) is None and (kx, kz) != (bx, bz):
            b.chest(kx, yb, kz, "south" if bed_z == z0 + 1 else "north", KIST_HUIS)
    if bord:
        b.sign(dx + (1 if deur in ("north", "south") else 0) + ox, y + 2, dz + (1 if deur in ("east", "west") else 0) + oz, deur, bord)
    return dx + ox, dz + oz


def iglo(b, cx, cz, r, y, deur, kleur="light_blue", bord=None, binnen=None, portaal=1):
    """A dome of snow on a ring of ice bricks: r = its outer radius, y = the floor block. A short porch on the `deur`
    side (portaal=0: only the doorway through the dome, nothing sticks out). binnen(b, cx, cz, y) furnishes it (default: a
    bed, a barrel and a rug). Returns the cell in front of the porch."""
    ox, oz = UIT[deur]
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            for dy in range(0, r + 1):
                d = math.sqrt(dx * dx + dy * dy + dz * dz)
                if r - 1.2 < d <= r + 0.3:
                    b.set(cx + dx, y + 1 + dy, cz + dz, IJS if dy == 0 else SNEEUW)
            if math.hypot(dx, dz) <= r - 1.2:
                b.set(cx + dx, y, cz + dz, "spruce_planks")
    b.set(cx, y + r, cz, LANTAARN)
    # the porch: two blocks of tunnel, one wide and two high, through the dome and a little out of it
    px, pz = cx + ox * r, cz + oz * r
    for k in range(-1, portaal + 1):
        tx, tz = px + ox * k, pz + oz * k
        zx, zz = (0, 1) if ox else (1, 0)
        for yy in (y + 1, y + 2):
            b.set(tx, yy, tz, "minecraft:air")
            if k >= 1:
                b.set(tx + zx, yy, tz + zz, SNEEUW)
                b.set(tx - zx, yy, tz - zz, SNEEUW)
        if k >= 1:
            for q in (-1, 0, 1):
                b.set(tx + zx * q, y + 3, tz + zz * q, SNEEUW)
    b.set(px + ox * portaal, y + 3, pz + oz * portaal, LANTAARN)
    if binnen:
        binnen(b, cx, cz, y)
    else:
        zx, zz = (0, 1) if ox else (1, 0)
        bed(b, cx - ox * (r - 3) + zx * (r - 2), y + 1, cz - oz * (r - 3) + zz * (r - 2), gb.OPP[deur], kleur)
        ton(b, cx - zx * (r - 2), y + 1, cz - zz * (r - 2))
        b.set(cx, y + 1, cz, f"{kleur}_carpet")
    if bord and portaal:
        zx, zz = (0, 1) if ox else (1, 0)
        b.sign(px + ox * (portaal + 1) + zx, y + 2, pz + oz * (portaal + 1) + zz, deur, bord)
    elif bord:                                                    # (no porch to hang it on: it stands beside the doorway)
        zx, zz = (0, 1) if ox else (1, 0)
        b.sign(px + ox + zx * 2, y + 1, pz + oz + zz * 2, deur, bord, wall=False)
    return px + ox * (portaal + 1), pz + oz * (portaal + 1)


def kraam(b, x, y, z, kleur, waar, bord, facing="north"):
    """A market stall of 3 x 2: two posts, a striped awning, a counter with wares. (x, z) = its north-west corner; the
    counter is on the `facing` side."""
    zc = z if facing == "north" else z + 1
    zb = z + 1 if facing == "north" else z
    for dx in (0, 2):
        b.set(x + dx, y + 1, zb, "spruce_fence")
        b.set(x + dx, y + 2, zb, "spruce_fence")
    for dx in range(3):
        for zz in (z, z + 1):
            b.set(x + dx, y + 3, zz, f"{kleur}_wool" if dx % 2 == 0 else "white_wool")
        b.set(x + dx, y + 1, zc, waar[dx % len(waar)], {"facing": "up", "open": "false"} if waar[dx % len(waar)] == "barrel" else None)
    b.set(x + 1, y + 1, zb, "spruce_trapdoor", {"facing": facing, "half": "top", "open": "false", "powered": "false", "waterlogged": "false"})
    b.sign(x + 1, y + 2, zc, facing, bord, wall=False)


def benedenstad(b, r):
    """The lower city: two rows of houses on both sides of the great canal, with the market at the basin."""
    namen = iter([
        ["Iglo te huur", "Koud, klein en", "heel gezellig.", "Vraag naar Guh"],
        ["Bakker Brrr", "IJskoekjes", "(niet aan", "likken, njeg)"],
        ["Visboer", "Vis van", "gisteren. Ook", "toen al vers"],
        ["Kapper", "Snorren knippen", "Alleen op", "afspraak"],
        ["Gondelverhuur", "Zelf duwen met", "watersturen", "Geen golfjes!"],
        ["Hier woont", "een guh die", "het NIET koud", "heeft. Echt."],
        ["Warme sokken", "Twee halen,", "twee betalen", ""],
        ["Stil a.u.b.", "Guh doet een", "winterslaapje", "(tot de lente)"],
    ])
    y = T0
    # plots: (x0, breedte) in the west half; the east half is its mirror image. The plot next to the basin is the market.
    plots = [(51, 10), (67, 11), (80, 11), (97, 9)]
    for rij, (za, zb), deur in (("n", RIJ_N, "north"), ("z", RIJ_Z, "south")):
        for helft in (0, 1):
            for i, (px, w) in enumerate(plots):
                x0 = px if helft == 0 else spiegel(px + w - 1)
                if rij == "n" and i == 3:
                    continue                                      # (the market, and across the canal the gondola yard)
                hoog = 2 if r.random() < 0.7 else 3
                bord = next(namen, None) if r.random() < 0.55 else None
                if i in (1, 2) and r.random() < 0.3:
                    # (no porch: its doorway is in the dome itself, and one block of quay stays free in front of it)
                    iglo(b, x0 + w // 2, 112 if deur == "north" else 122, 3, y, deur, kleur=r.choice(BEDDEN), bord=bord, portaal=0)
                else:
                    huis(b, x0, za, w, zb - za + 1, y, deur, r, verdiepingen=hoog, kist=r.random() < 0.35, bord=bord)
    # the market west of the basin: three stalls
    kraam(b, 97, y, 111, "blue", ["barrel", "dried_kelp_block", "barrel"], ["Verse vis!", "(gisteren ook", "al vers, njeg)", ""])
    kraam(b, 102, y, 113, "light_blue", ["snow_block", "barrel", "packed_ice"], ["Plaksnorren", "voor de les van", "Pakguh. Echt", "haar van guh!"])
    kraam(b, 97, y, 114, "cyan", ["barrel", "barrel", "sea_lantern"], ["Zeepruimen", "5 voor 1", "knabbel. Proeven", "op eigen risico"], facing="south")
    # the gondola yard east of the basin: a boat on trestles and a shed
    for x in range(120, 127):
        b.set(x, y + 2, 112, "spruce_slab", gb.slab("bottom"))
        if x in (121, 125):
            b.set(x, y + 1, 112, "spruce_fence")
    for x in (120, 126):
        b.set(x, y + 2, 112, "spruce_stairs", gb.stair("east" if x == 126 else "west"))
    b.sign(123, y + 1, 111, "north", ["Gondelwerf", "Bijna af.", "Al drie winters", "bijna af."], wall=False)
    ton(b, 119, y + 1, 114)
    ton(b, 119, y + 2, 114)
    ton(b, 120, y + 1, 114)
    # what lies about on the broad quays (always against the wall side, never in the way)
    for x in (54, 86, 98):
        for xx in (x, spiegel(x)):
            ton(b, xx, y + 1, 100)
            if xx % 4 == 2:
                ton(b, xx, y + 2, 100)
    for x in (67, 76):
        slee(b, x, y + 1, 100)
        slee(b, spiegel(x) - 1, y + 1, 100)
    for x in (50, 70, 152, 172):
        b.set(x, y + 1, 139, "spruce_fence")
        b.set(x, y + 2, 139, "spruce_fence")
        b.set(x + 1, y + 2, 139, "spruce_fence")
        b.set(x + 2, y + 2, 139, "spruce_fence")
        b.set(x + 2, y + 1, 139, "spruce_fence")
        b.set(x + 1, y + 1, 139, "cobweb")
    ton(b, 41, y + 1, 110)
    ton(b, 41, y + 1, 124)
    slee(b, 182, y + 1, 111)
    ton(b, 183, y + 1, 123)
    # lantern posts along the quays, mooring posts at the gondola stops
    for x in list(range(46, 110, 12)) + list(range(118, 182, 12)):
        lantaarnpaal(b, x, y + 1, 100)
        lantaarnpaal(b, x, y + 1, 136)
    for z in (104, 117, 130):
        lantaarnpaal(b, 40, y + 1, z)
        lantaarnpaal(b, 184, y + 1, z)


def gondelhaltes(b):
    """Where the gondola starts (the basin at the grand stairs) and its three stops: a blue-and-white mooring post and a
    sign. Returns [(x, y, z)]: the quay cell of each, the start first."""
    plekken = [(MX, T0 + 1, 102, "north"), (MX - 4, T0 + 1, 135, "south"), (42, T0 + 1, 117, "west"), (182, T0 + 1, 117, "east")]
    tekst = [["Gondel", "Naar de poort", "Zelf duwen met", "watersturen"], ["Halte Poort", "", "Niet duwen op", "de gondelier"],
             ["Halte West", "Volgende gondel", "komt als jij", "hem duwt, njeg"], ["Halte Oost", "Vandaag geen", "golven hoger", "dan je guh"]]
    uit = []
    for (x, y, z, kant), regels in zip(plekken, tekst):
        zx, zz = (1, 0) if kant in ("north", "south") else (0, 1)
        for i, kleur in enumerate(("blue_wool", "white_wool", "blue_wool")):
            b.set(x + zx * 2, y + i, z + zz * 2, kleur)
        b.set(x + zx * 2, y + 3, z + zz * 2, LANTAARN)
        b.sign(x - zx * 2, y, z - zz * 2, "south" if kant == "north" else ("north" if kant == "south" else kant), regels, wall=False)
        uit.append((x, y, z))
    return uit


# =====================================================================================================================
# the middle tier: Meester Pakguh's training square, the wave pool, the houses of the west side
# =====================================================================================================================
PLEIN = (90, 134, 77, 93)
DOELEN = [(132, z) for z in (78, 81, 84, 87, 90)]


def middenstad(b, r):
    y = T1
    x0, x1, z0, z1 = PLEIN
    # the square: a ring of blue and white in the paving, with the crescent in it
    for x in range(MX - 7, MX + 8):
        for z in range(85 - 7, 85 + 8):
            d = math.hypot(x - MX, z - 85)
            if 5.6 < d <= 6.6:
                b.set(x, y, z, "light_blue_concrete")
            elif d <= 5.6 and math.hypot(x - MX - 2, z - 85) > 4.2:
                b.set(x, y, z, "white_concrete")
            elif d <= 5.6:
                b.set(x, y, z, "light_blue_terracotta")
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        lantaarnpaal(b, x, y + 1, z, hoog=3)
    # the whip targets: pillars of ice with a target on top, in a row on the east side
    for x, z in DOELEN:
        b.set(x, y + 1, z, PAK)
        b.set(x, y + 2, z, "target")
    b.sign(x1 - 1, y + 1, 92, "west", ["Zweepdoelen", "Raak = goed.", "Mis = nat.", "Sokka-guh: nat."], wall=False)
    # Pakguh's seat and his rule
    trap(b, MX, y + 1, z0, "north")
    b.set(MX - 1, y + 1, z0, PAK)
    b.set(MX + 1, y + 1, z0, PAK)
    for x in list(range(96, 105)) + list(range(120, 129)):       # benches for whoever comes to watch
        trap(b, x, y + 1, z0, "north")
    for x in (94, 130):
        b.set(x, y + 1, z0, "water_cauldron", {"level": "3"})     # (practice water, for those who forgot theirs)
    b.sign(MX - 3, y + 1, z0, "south", ["Meester Pakguh", "Les: elke dag", "Alleen guhs", "met een snor"], wall=False)
    b.sign(MX + 3, y + 1, z0, "south", ["Geen snor?", "Dan eerst een", "snor. Zo zijn", "de regels."], wall=False)
    # the wave pool: a rim of slabs, lanterns in its bed, a pavilion on the islet
    bx0, bx1, bz0, bz1 = BASSIN
    for x in range(bx0 - 1, bx1 + 2):
        for z in range(bz0 - 1, bz1 + 2):
            if (x in (bx0 - 1, bx1 + 1) or z in (bz0 - 1, bz1 + 1)) and not (PIER[2] <= z <= PIER[3] and x == bx0 - 1) and not (bx0 + 5 <= x <= bx0 + 9 and z == bz1 + 1):
                if (x + z) % 6 == 0:
                    b.set(x, y + 1, z, LANTAARN)
                else:
                    plaat(b, x, y + 1, z)
    for x in range(bx0 + 3, bx1, 6):
        for z in range(bz0 + 3, bz1, 6):
            if b.get(x, y - 3, z) == "minecraft:blue_ice":
                b.set(x, y - 3, z, "sea_lantern")
    ex, ez, _ = EILANDJE
    for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        for yy in range(y + 1, y + 4):
            b.set(ex + dx, yy, ez + dz, PAAL)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if max(abs(dx), abs(dz)) == 3 and abs(dx) != abs(dz):
                plaat(b, ex + dx, y + 4, ez + dz)
            elif max(abs(dx), abs(dz)) <= 2:
                b.set(ex + dx, y + 4, ez + dz, SNEEUW if max(abs(dx), abs(dz)) == 2 else IJS)
    b.set(ex, y + 4, ez, LANTAARN)
    b.set(ex, y + 5, ez, TEKEN)
    b.sign(PIER[1], y + 1, PIER[2], "west", ["Naar het eiland?", "Bevries zelf", "maar een brug.", "- Pakguh"], wall=False)
    b.sign(bx0 + 7, y + 1, bz1 + 2, "south", ["Golfbassin", "Wie een golf", "maakt, dweilt", "hem zelf op"], wall=False)
    # the west side: two houses, the moustache-free igloo school, lanterns along the street
    huis(b, 54, 78, 11, 7, y, "south", r, verdiepingen=2, kist=True, bord=["Familie Guh", "Bel 2x. Wij", "zitten boven", "bij de kachel"])
    iglo(b, 71, 81, 4, y, "south", kleur="cyan", bord=["Snorrenschool", "Voor guhs die", "bij Pakguh", "willen blijven"])
    huis(b, 78, 78, 10, 7, y, "south", r, verdiepingen=3, kist=False)
    iglo(b, 50, 81, 3, y, "south", kleur="white", bord=["Brr-en-breakfast", "Bed van sneeuw,", "ontbijt van vis.", "Vier sterren."])
    iglo(b, 174, 84, 3, y, "west", kleur="blue", bord=["Badmeester", "Niet rennen", "langs het", "golfbassin!"])
    for x in (58, 66, 76, 88):
        lantaarnpaal(b, x, y + 1, 86)
    for x in (60, 84):
        lantaarnpaal(b, x, y + 1, 93)
    # Sokka-guh's ice sculpture (he says it is Yue-guh; it is a fish)
    for dx, dy in ((0, 1), (1, 1), (2, 1), (1, 2), (3, 2), (3, 1), (3, 0), (1, 0)):
        b.set(84 + dx, y + 2 + dy, 96, BLAUW)
    b.fill(84, y + 1, 96, 87, y + 1, 96, IJS)
    b.sign(86, y + 1, 95, "north", ["IJsbeeld van", "Sokka-guh: 'Dit", "is Yue-guh.'", "(het is een vis)"], wall=False)
    # a rim along the edge of the tier (never where stairs go down)
    rand_tier(b, STAD1, y, [TRAP_GROOT, (49, 52), (172, 175)])
    for x in (146, 156, 166, 174):
        lantaarnpaal(b, x, y + 1, 77)
    lantaarnpaal(b, 170, y + 1, 90)


def tiertrappen(b):
    """Steps on the six stairs cut into the tiers (the ground leaves whole blocks there), and on the two steps up to the
    podium of the palace."""
    for (xa, xb), z, top in [(TRAP_GROOT, 94, T1), (TRAP_GROOT, 70, T2), ((49, 52), 94, T1), ((172, 175), 94, T1), ((72, 75), 70, T2), ((149, 152), 70, T2)]:
        for i in range(6):
            for x in range(xa, xb + 1):
                trap(b, x, top - i, z + i, "north")
    zp = PALEIS[3]
    for x in range(MX - 8, MX + 9):
        trap(b, x, T2 + 1, zp + 2, "north")
        trap(b, x, P0, zp + 1, "north")


def rand_tier(b, tier, y, open_x):
    """A low wall of posts along the south edge of a tier, open where stairs go down."""
    x0, x1, z0, z1 = tier
    for x in range(x0, x1 + 1):
        if any(a <= x <= c for a, c in open_x):
            continue
        b.set(x, y + 1, z1, PAAL if x % 6 else IJS)
        if x % 6 == 0:
            b.set(x, y + 2, z1, LANTAARN if x % 12 == 0 else SNEEUW)


# =====================================================================================================================
# the palace terrace: the palace, the healing hut, the armoury
# =====================================================================================================================
def bovenstad(b, r):
    y = T2
    rand_tier(b, STAD2, y, [TRAP_GROOT, (72, 75), (149, 152)])

    def genezer(b, cx, cz, y):
        # a healing pool in the middle (one block of water in a ring of slabs), a practice dummy, mats
        b.set(cx, y, cz, "water", WATER)
        b.set(cx, y - 1, cz, "sea_lantern")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            b.set(cx + dx, y + 1, cz + dz, "light_blue_carpet")
        b.set(cx - 3, y + 1, cz - 1, "hay_block", {"axis": "y"})
        b.set(cx - 3, y + 2, cz - 1, "carved_pumpkin", {"facing": "east"})
        bed(b, cx + 2, y + 1, cz - 3, "east", "white")
        ton(b, cx + 3, y + 1, cz + 1)
        b.set(cx - 2, y + 1, cz + 2, "potted_blue_orchid")
    iglo(b, 74, 50, 5, y, "east", bord=["Genezershut", "Yugoda-guh", "Blub blub,", "beter!"], binnen=genezer)
    b.sign(81, y + 1, 53, "east", ["De pop heet", "Sokka-guh. Hij", "wordt elke les", "beter gemaakt."], wall=False)
    # the armoury: a long house, a row of practice dummies in front of it
    huis(b, 144, 44, 12, 8, y, "south", r, verdiepingen=1, kist=True, bord=["Wapenkamer", "Speren van ijs.", "Smelten bij", "het vuur. Tja."])
    for x in (145, 148, 151, 154):
        b.set(x, y + 1, 56, "spruce_fence")
        b.set(x, y + 2, 56, "hay_block", {"axis": "y"})
        b.set(x, y + 3, 56, "carved_pumpkin", {"facing": "south"})
    huis(b, 66, 37, 11, 6, y, "south", r, verdiepingen=2, kist=True, bord=["Maanwachters", "Wij tellen elke", "nacht de maan.", "Het is er 1."])
    huis(b, 66, 59, 9, 7, y, "east", r, verdiepingen=2, bord=["De Dampende Guh", "Thee! Heet!", "(blijft 4 tellen", "heet, njeg)"])
    huis(b, 147, 36, 11, 6, y, "south", r, verdiepingen=3, kist=True)
    huis(b, 149, 59, 9, 7, y, "west", r, verdiepingen=2, bord=["Sleeenstalling", "Ook voor otters", "(die betalen", "nooit)"])
    ijsguh(b, MX - 11, y + 1, 67, "south")
    ijsguh(b, MX + 11, y + 1, 67, "south")
    for x in (MX - 16, MX + 16):
        lantaarnpaal(b, x, y + 1, 66, hoog=3)
    for x in (68, 82, 142, 158):
        lantaarnpaal(b, x, y + 1, 68)
    for x in (66, 158):
        lantaarnpaal(b, x, y + 1, 36)
        lantaarnpaal(b, x, y + 1, 52)
    paleis(b)


def paleis(b):
    """The palace of hoofdman Arnook-guh: three tiers of ice, each smaller than the one below, each with a snowy eave. Below:
    the throne hall with its two long pools; on its roof the council hall, where Yue-guh has her corner; on top the
    moon room."""
    x0, x1, z0, z1 = PALEIS
    y = P0

    def laag(xa, xb, za, zb, ya, hoog, ramen=True):
        """Walls of one tier (blue ice pilasters, tall windows between them) and its roof with an eave of snowy slabs."""
        for x in range(xa, xb + 1):
            for z in range(za, zb + 1):
                if not (x in (xa, xb) or z in (za, zb)):
                    continue
                hoek = x in (xa, xb) and z in (za, zb)
                lang = x - xa if z in (za, zb) else z - za
                pilaster = hoek or lang % 6 == 0
                for yy in range(ya, ya + hoog):
                    raam = ramen and not pilaster and lang % 6 in (2, 3, 4) and ya + 1 <= yy <= ya + hoog - 2
                    b.set(x, yy, z, BLAUW if pilaster else (GLAS if raam else IJS))
        top = ya + hoog
        b.fill(xa, top, za, xb, top, zb, IJS)
        for x in range(xa - 1, xb + 2):
            for z in range(za - 1, zb + 2):
                if x in (xa - 1, xb + 1) or z in (za - 1, zb + 1):
                    hoek = x in (xa - 1, xb + 1) and z in (za - 1, zb + 1)
                    if hoek:
                        b.set(x, top, z, SNEEUW)
                        b.set(x, top + 1, z, LANTAARN)
                    else:
                        plaat(b, x, top, z, "top")
                elif x in (xa, xb) or z in (za, zb):
                    b.set(x, top + 1, z, SNEEUW if (x + z) % 2 else IJS)
        return top

    # --- tier A: the throne hall ---
    b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, TEGEL)
    dak_a = laag(x0, x1, z0, z1, y + 1, 10)
    for x in range(MX - 3, MX + 4):                              # the great door: an arch seven wide
        for yy in range(y + 1, y + 9):
            if yy < y + 7 or abs(x - MX) <= (2 if yy == y + 7 else 1):
                b.set(x, yy, z1, "minecraft:air")
            else:
                b.set(x, yy, z1, IJS)
    for x in (MX - 4, MX + 4):
        for yy in range(y + 1, y + 11):
            b.set(x, yy, z1, BLAUW)
    b.set(MX, y + 10, z1 + 1, TEKEN)
    for x in (MX - 4, MX + 4):
        for yy in range(y + 3, y + 10):
            b.set(x, yy, z1 + 1, "blue_wool" if yy % 2 else "white_wool")
    b.sign(MX + 5, y + 2, z1 + 1, "south", ["Paleis van", "hoofdman", "Arnook-guh", "Kloppen = njeg"])
    b.sign(MX - 5, y + 2, z1 + 1, "south", ["Bezoekuur:", "als de maan", "op is. Dus", "eigenlijk altijd"])
    for x in (100, 124):                                         # two rows of columns
        for z in (44, 49, 54, 59):
            b.set(x, y + 1, z, IJS)
            for yy in range(y + 2, y + 10):
                b.set(x, yy, z, PAK)
            b.set(x, y + 10, z, IJS)
            b.set(x + (1 if x < MX else -1), y + 9, z, LANTAARN)
    for xa in (91, 129):                                         # the two long pools, flush in the floor
        b.fill(xa, y, 43, xa + 4, y, 59, "water", WATER)
        for z in range(44, 60, 5):
            b.set(xa + 2, y - 1, z, "sea_lantern")
        for z in (45, 52, 57):
            b.set(xa + 1 + (z % 3), y + 1, z, "guhs:guh_waterlelie")
    for z in range(44, 59, 2):                                   # benches of ice along the pools
        trap(b, 97, y + 1, z, "west")
        trap(b, 127, y + 1, z, "east")
    ijsguh(b, MX - 8, y + 1, 60, "south")
    ijsguh(b, MX + 8, y + 1, 60, "south")
    for z in range(60, z1):                                      # the runner from the door to the mosaic
        for x in range(MX - 2, MX + 3):
            b.set(x, y + 1, z, "blue_carpet" if abs(x - MX) == 2 else "light_blue_carpet")
    # the dais: two broad steps, three seats against the north wall under the emblem
    b.fill(102, y + 1, z0 + 1, 122, y + 1, 45, IJS)
    for x in range(102, 123):
        trap(b, x, y + 1, 46, "north")
    b.fill(106, y + 2, z0 + 1, 118, y + 2, 43, TEGEL)
    for x in range(106, 119):
        trap(b, x, y + 2, 44, "north")
    for x, hoog in ((MX, 4), (MX - 3, 3), (MX + 3, 3)):
        trap(b, x, y + 3, z0 + 2, "north", blok="quartz_stairs")
        for yy in range(y + 3, y + 3 + hoog):
            b.set(x, yy, z0 + 1, BLAUW)
        b.set(x, y + 3 + hoog, z0 + 1, TEKEN if x == MX else SNEEUW)
    for x in (MX - 6, MX + 6):
        b.set(x, y + 3, z0 + 2, IJS)
        b.set(x, y + 4, z0 + 2, LANTAARN)
    for j, row in enumerate(EMBLEEM):                            # the emblem as a mosaic in the floor of the hall
        for i, ch in enumerate(row):
            if ch != ".":
                b.set(MX - 5 + i, y, 49 + j, {"R": "prismarine_bricks", "D": "light_blue_concrete", "W": "white_concrete"}[ch])
    for x, z in ((MX, 53), (MX, 60), (108, 41), (116, 41)):      # ice lanterns on chains from the ceiling
        b.set(x, y + 10, z, "chain", {"axis": "y", "waterlogged": "false"})
        b.set(x, y + 9, z, "chain", {"axis": "y", "waterlogged": "false"})
        b.set(x, y + 8, z, LANTAARN)
    # the stairs up to the roof terrace, along the east wall (a solid flight), and the well they come up through
    for k in range(11):
        z = 54 - k
        for x in (x1 - 2, x1 - 1):
            for yy in range(y + 1, y + 1 + k):
                b.set(x, yy, z, IJS)
            trap(b, x, y + 1 + k, z, "north")
    b.fill(x1 - 2, dak_a, 44, x1 - 1, dak_a, 54, "minecraft:air")
    for z in range(44, 56):
        b.set(x1 - 3, dak_a + 1, z, PAAL)
    b.set(x1 - 2, dak_a + 1, 55, PAAL)
    b.set(x1 - 1, dak_a + 1, 55, PAAL)
    b.sign(x1 - 3, y + 1, 56, "south", ["Naar boven:", "raadzaal en", "maankamer.", "Voeten vegen!"], wall=False)
    # --- tier B: the council hall ---
    bx0, bx1, bz0, bz1 = 98, 126, 42, 59
    dak_b = laag(bx0, bx1, bz0, bz1, dak_a + 1, 7)
    b.fill(bx0 + 1, dak_a, bz0 + 1, bx1 - 1, dak_a, bz1 - 1, TEGEL)
    for z in (50, 51):                                           # the door from the terrace, on the east side
        for yy in range(dak_a + 1, dak_a + 4):
            b.set(bx1, yy, z, "minecraft:air")
    b.set(bx1, dak_a + 4, 50, LANTAARN)
    for x in range(108, 117):                                    # the council table: a slab of ice with the map of the city on it
        for z in (50, 51, 52):
            plaat(b, x, dak_a + 1, z, "top")
            if (x + z) % 3 == 0:
                b.set(x, dak_a + 2, z, "light_blue_carpet" if x % 2 else "white_carpet")
    for x in range(108, 117, 2):
        trap(b, x, dak_a + 1, 48, "north", blok="quartz_stairs")
        trap(b, x, dak_a + 1, 54, "south", blok="quartz_stairs")
    b.sign(107, dak_a + 1, 53, "west", ["Krijgsraad", "Punt 1: vloot.", "Punt 2: wie", "at de zeepruimen"], wall=False)
    # Yue-guh's corner in the west end: a white bed, a mirror of ice, a chest, a flower
    bed(b, bx0 + 2, dak_a + 1, bz0 + 2, "west", "white")
    for yy in (dak_a + 1, dak_a + 2):
        b.set(bx0 + 1, yy, bz0 + 6, RUIT)
    b.set(bx0 + 1, dak_a + 1, bz0 + 4, "potted_lily_of_the_valley")
    b.chest(bx0 + 1, dak_a + 1, bz1 - 2, "east", KIST_PALEIS)
    for x in range(bx0 + 1, bx0 + 5):
        for z in range(bz0 + 1, bz0 + 6):
            if b.get(x, dak_a + 1, z) is None:
                b.set(x, dak_a + 1, z, "white_carpet")
    b.sign(bx0 + 5, dak_a + 1, bz0 + 3, "east", ["Hoekje van Yue.", "Sokka-guh: niet", "binnen zonder", "kloppen. Of ooit"], wall=False)
    for z in range(bz0 + 2, bz1 - 1, 3):                         # books along the east end, a lectern
        b.set(bx1 - 1, dak_a + 1, z, "bookshelf")
        b.set(bx1 - 1, dak_a + 2, z, "bookshelf")
    b.set(bx1 - 3, dak_a + 1, bz1 - 2, "lectern", {"facing": "north", "has_book": "false", "powered": "false"})
    for x in (104, 120):
        b.set(x, dak_b, 50, LANTAARN)
    # the ladder to the moon room, on a column of blue ice near the north wall
    for yy in range(dak_a + 1, dak_b + 2):
        b.set(MX, yy, 44, BLAUW)
    ladder(b, MX, dak_a + 1, dak_b + 1, 45, "south")
    b.set(MX, dak_b + 2, 44, LANTAARN)
    # --- tier C: the moon room ---
    cx0, cx1, cz0, cz1 = 106, 118, 43, 55
    dak_c = laag(cx0, cx1, cz0, cz1, dak_b + 1, 6)
    b.fill(cx0 + 1, dak_b, cz0 + 1, cx1 - 1, dak_b, cz1 - 1, TEGEL)
    b.set(MX, dak_b, 45, "ladder", {"facing": "south", "waterlogged": "false"})
    for x in range(MX - 2, MX + 3):
        for z in range(47, 52):
            d = math.hypot(x - MX, z - 49)
            if d <= 2.3:
                b.set(x, dak_b + 1, z, "white_carpet" if math.hypot(x - MX - 1, z - 49) > 1.6 else "blue_carpet")
    b.set(cx0 + 1, dak_b + 1, cz1 - 1, "lectern", {"facing": "east", "has_book": "false", "powered": "false"})
    b.sign(cx1 - 1, dak_b + 1, cz1 - 1, "west", ["Maankamer", "Tui trekt, La", "duwt. Niet", "tussen komen."], wall=False)
    b.set(MX, dak_c, 49, LANTAARN)
    # the spire: a stepped cap of snow and ice with the emblem and a lantern on top
    for i in range(1, 6):
        b.fill(cx0 + i, dak_c + i, cz0 + i, cx1 - i, dak_c + i, cz1 - i, SNEEUW if i % 2 else IJS)
    b.set(MX, dak_c + 6, 49, TEKEN)
    b.set(MX, dak_c + 7, 49, LANTAARN)
    b.set(MX, dak_c + 8, 49, "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})


# =====================================================================================================================
# the spirit oasis
# =====================================================================================================================
def oase(b, r):
    cx, cz, straal = OASE
    vx, vz, vr = VIJVER
    y = T2
    # the door in the ice: a round frame of spruce around a little door, at the terrace end of the tunnel
    dz = STAD2[2] - 1
    for dx in (-1, 0, 1):
        for yy in (y + 1, y + 2, y + 3):
            if dx == 0 and yy < y + 3:
                continue
            b.set(MX + dx, yy, dz, "stripped_spruce_log", {"axis": "y" if dx else "x"})
    gb.door(b, MX, y + 1, dz, "north", wood="spruce")
    b.set(MX, y + 4, dz + 1, TEKEN)
    b.sign(MX + 2, y + 2, dz + 1, "south", ["Geestenoase", "Vissen NIET", "voeren, aaien", "of meenemen!"])
    b.sign(MX - 2, y + 2, dz + 1, "south", ["Het is hier", "warm. Jas uit,", "njeg. Het gras", "is echt."])
    b.set(MX, y + 4, dz - 3, "ochre_froglight", {"axis": "y"})
    # green and warm: carpets of moss, azalea, bamboo, flowers; froglights in the rock for the glow
    planten = ["azalea", "flowering_azalea", "moss_carpet", "moss_carpet", "blue_orchid", "lily_of_the_valley", "pink_tulip", "moss_carpet"]
    for x in range(int(cx - straal) - 1, int(cx + straal) + 2):
        for z in range(int(cz - straal) - 1, int(cz + straal) + 2):
            d, dv = math.hypot(x - cx, z - cz), math.hypot(x - vx, z - vz)
            if d > straal - 0.6 or b.get(x, y, z) != "minecraft:moss_block" or b.get(x, y + 1, z) is not None:
                continue
            if abs(x - MX) <= 1 and z > vz:
                continue                                         # (the path from the door to the pond stays clear)
            if dv <= 1.7:
                continue                                         # (the islet: the little gate stands there)
            kans = r.random()
            if d > straal - 2.6 and kans < 0.5:
                if kans < 0.2:
                    hoog = r.randint(3, 6)
                    for i in range(hoog):
                        b.set(x, y + 1 + i, z, "bamboo", {"age": "0", "leaves": "large" if i >= hoog - 2 else ("small" if i == hoog - 3 else "none"), "stage": "1"})
                else:
                    b.set(x, y + 1, z, "azalea" if kans < 0.38 else "flowering_azalea")
            elif kans < 0.42:
                b.set(x, y + 1, z, r.choice(planten))
    for a in range(0, 360, 45):
        fx, fz = int(round(cx + (straal - 0.2) * math.cos(math.radians(a + 20)))), int(round(cz + (straal - 0.2) * math.sin(math.radians(a + 20))))
        if b.get(fx, y + 2, fz) not in (None, "minecraft:air") and abs(fx - MX) > 2:
            b.set(fx, y + 2 + (a // 45) % 3, fz, "ochre_froglight" if a % 90 else "verdant_froglight", {"axis": "y"})
    # the pond: a bed of sand with light in it, lily pads, and on the islet the little gate
    for x in range(int(vx - vr) - 1, int(vx + vr) + 2):
        for z in range(int(vz - vr) - 1, int(vz + vr) + 2):
            if b.get(x, y - 1, z) == "minecraft:water" and b.get(x, y - 2, z) == "minecraft:stone":
                b.set(x, y - 2, z, "sea_lantern" if (x + 2 * z) % 5 == 0 else "sand")
    for dx, dz2 in ((-3, 2), (3, -2), (2, 3), (-2, -3)):
        if b.get(vx + dx, y, vz + dz2) == "minecraft:water":
            b.set(vx + dx, y + 1, vz + dz2, "guhs:guh_waterlelie")
    for dx in (-1, 1):
        for yy in (y + 1, y + 2):
            b.set(vx + dx, yy, vz, "stripped_spruce_log", {"axis": "y"})
    for dx in (-1, 0, 1):
        b.set(vx + dx, y + 3, vz, "spruce_slab", gb.slab("bottom"))
    b.set(vx, y + 4, vz, "spruce_slab", gb.slab("bottom"))
    b.set(vx, y + 3, vz, "lantern", gb.lamp(True))
    # the waterfall on the north wall: it drops out of a spout of ice into the brook (a still column: source on top, falling below)
    wz = int(cz - straal) + 1
    top = y + 12
    for yy in range(y + 1, top):
        b.set(MX, yy, wz, "water", {"level": "8"})
    b.set(MX, top, wz, "water", WATER)
    for dx, dz2 in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        b.set(MX + dx, top, wz + dz2, PAK)
    b.set(MX, top + 1, wz, PAK)
    for dx in (-1, 1):
        b.set(MX + dx, top, wz - 1, BLAUW)
        b.set(MX + dx, top - 1, wz - 1, BLAUW)
        b.set(MX + dx, top - 1, wz, BLAUW)
    b.set(MX, top - 1, wz - 1, PAK)
    b.sign(MX + 3, y + 1, int(vz + vr) + 2, "south", ["Tui en La", "De maan en de", "zee. Ze zwemmen", "al heel lang."], wall=False)


# =====================================================================================================================
# the sled run: its start on the wall and its finish on the shelf, with the otters' cove
# =====================================================================================================================
def sleebaan(b, baan):
    z1 = MUUR_Z[1]
    # the start: two posts with a banner between them, a sign
    for z in (z1 + 2, z1 + 6):
        for yy in range(MUUR + 1, MUUR + 4):
            b.set(94, yy, z, PAAL)
    for z in range(z1 + 2, z1 + 7):
        b.set(94, MUUR + 4, z, "blue_wool" if z % 2 else "white_wool")
    b.sign(97, MUUR + 1, z1 + 2, "west", ["Otterslee", "Hou je vast", "aan je otter.", "TUUT!"], wall=False)
    b.set(96, MUUR, z1 + 4, LANTAARN)
    # lanterns in the banks, every eighth block of the way down
    for (x, z), (top, soort) in sorted(baan.items()):
        if soort == "wal" and (x + 2 * z) % 9 == 0 and b.get(x, top, z) == "minecraft:snow_block":
            b.set(x, top, z, LANTAARN)
    # the finish: a heap of snow to slide into, a sign, and in the cove between the two legs the otters' snow burrows
    for dx, dz, hoog in ((0, -1, 1), (0, 0, 2), (0, 1, 1), (1, 0, 1), (1, -1, 1), (1, 1, 1)):
        for i in range(hoog):
            b.set(93 + dx, T0 + 1 + i, 163 + dz, SNEEUW)
    b.sign(91, T0 + 1, 166, "north", ["Finish!", "Nog een keer?", "De otter vindt", "het prima."], wall=False)
    for cx, cz in ((66, 158), (74, 157), (82, 158)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if (dx, dz) != (0, 1):
                    b.set(cx + dx, T0 + 1, cz + dz, SNEEUW)
                if abs(dx) + abs(dz) <= 1:
                    b.set(cx + dx, T0 + 2, cz + dz, SNEEUW)
        b.set(cx, T0 + 1, cz, "minecraft:air")
        b.set(cx, T0 + 1, cz + 1, "minecraft:air")
    b.set(70, T0, 158, LANTAARN)
    b.set(78, T0, 158, LANTAARN)
    b.sign(86, T0 + 1, 158, "east", ["Otterhoek", "Niet voeren.", "Ze stelen je", "vis toch wel."], wall=False)


# =====================================================================================================================
# outside the wall, east of the gate: the landing place, the camp, the harbour
# =====================================================================================================================
AANKOMST = (120, 132, 153, 165)
KAMP = (135, 151, 153, 165)
VUUR = (144, 156)


def kamp(b):
    y = T0
    x0, x1, z0, z1 = AANKOMST
    for x, z in ((x0 - 1, z0 - 1), (x1 + 1, z0 - 1), (x0 - 1, z1 + 1), (x1 + 1, z1 + 1)):
        lantaarnpaal(b, x, y + 1, z)
    for x in range(x0, x1 + 1):                                  # a ring in the snow marks the landing place
        for z in range(z0, z1 + 1):
            d = math.hypot(x - (x0 + x1) / 2, z - (z0 + z1) / 2)
            if 5.0 < d <= 6.0:
                b.set(x, y, z, "light_blue_concrete")
    b.sign(x0 - 1, y + 1, z0 + 2, "east", ["Landingsijs", "Bizons, ballons", "en bootjes:", "hier neerzetten"], wall=False)
    # the camp: a tent of blue and white cloth, a fire with logs to sit on, hay and water for Appa
    tx, tz = 137, 154
    for i in range(5):
        for dz in range(5):
            hoog = 3 - abs(i - 2)
            if hoog > 0 and i != 2:
                b.set(tx + i, y + hoog, tz + dz, "blue_wool" if dz % 2 else "white_wool")
            elif i == 2:
                b.set(tx + i, y + 3, tz + dz, "spruce_slab", gb.slab("bottom"))
    for yy in (y + 1, y + 2):
        b.set(tx + 2, yy, tz, "spruce_fence")
    b.set(tx + 2, y + 1, tz + 1, "blue_carpet")
    b.set(tx + 2, y + 1, tz + 2, "light_blue_carpet")
    b.set(tx + 2, y + 1, tz + 3, "blue_carpet")
    b.set(tx + 2, y + 2, tz + 2, "minecraft:air")
    fx, fz = VUUR
    vuurtje(b, fx, y + 1, fz)
    for dx, dz, axis in ((-2, 0, "z"), (2, 0, "z"), (0, 2, "x")):
        b.set(fx + dx, y + 1, fz + dz, "stripped_spruce_log", {"axis": axis})
    for dx, dz in ((4, 4), (4, 5), (5, 4)):
        b.set(fx + dx, y + 1, fz + dz, "hay_block", {"axis": "y"})
    b.set(fx + 5, y + 2, fz + 4, "hay_block", {"axis": "y"})
    b.set(fx + 5, y + 1, fz + 6, "water_cauldron", {"level": "3"})
    b.sign(fx + 4, y + 1, fz + 3, "west", ["Kamp Guhvatar", "Appa parkeert", "bij het hooi.", "Momo: overal."], wall=False)
    for (dx, dz), kleur in zip(((-3, -1), (-3, 1), (3, -2), (4, 2), (-3, 3)), ("orange", "blue", "light_blue", "green", "red")):
        b.set(fx + dx, y + 1, fz + dz, f"{kleur}_carpet")           # bedrolls: Aang, Katara, Sokka, Toph, Zuka
    b.set(fx + 1, y + 1, fz - 1, "cauldron")
    for i in range(3):                                           # Sokka-guh's own little tent (he says it is the war tent)
        for dz in range(3):
            if i != 1:
                b.set(138 + i, y + 1, 162 + dz, "light_blue_wool" if dz % 2 else "white_wool")
            else:
                b.set(138 + i, y + 2, 162 + dz, "spruce_slab", gb.slab("bottom"))
    b.set(139, y + 1, 162, "spruce_fence")
    b.sign(141, y + 1, 163, "east", ["Krijgstent", "van Sokka-guh.", "Kloppen! (op", "het zeil, ja)"], wall=False)
    for x in (148, 151):
        b.set(x, y + 1, 154, "spruce_fence")
        b.set(x, y + 2, 154, "spruce_fence")
    b.set(149, y + 2, 154, "spruce_fence")
    b.set(150, y + 2, 154, "spruce_fence")
    b.set(149, y + 1, 154, "white_banner", {"rotation": "8"})
    lantaarnpaal(b, 152, y + 1, 153)
    lantaarnpaal(b, 152, y + 1, 165)


def kano(b, x, z, lengte):
    """A Waterstam canoe on the water, bow to the south: a floor of spruce slabs between gunwales of trapdoors."""
    y = SEA + 1
    for i in range(lengte):
        b.set(x, y, z + i, "spruce_slab", gb.slab("bottom"))
        if 0 < i < lengte - 1:
            for dx, kant in ((-1, "east"), (1, "west")):
                b.set(x + dx, y, z + i, "spruce_trapdoor", {"facing": kant, "half": "bottom", "open": "true", "powered": "false", "waterlogged": "false"})
    b.set(x, y + 1, z, "spruce_fence")
    b.set(x, y + 2, z, "lantern", gb.lamp(False))


def haven(b):
    y = T0
    # two piers of ice into the sea, each with posts and a lantern at the end; the east one runs along Zhao's ship
    for px0, px1, pz0, pz1 in ((154, 156, 166, 182), (173, 175, 160, 192)):
        b.fill(px0, SEA - 5, pz0, px1, y, pz1, IJS)
        b.fill(px0, y, pz0, px1, y, pz1, SNEEUW)
        b.fill(px0 + 1, y, pz0, px0 + 1, y, pz1, IJS)
        for z in range(pz0 + 3, pz1 + 1, 7):
            b.set(px0, y + 1, z, "spruce_fence")
        lantaarnpaal(b, px0 + 1, y + 1, pz1)
        b.set(px0 + 1, SEA - 3, (pz0 + pz1) // 2, "sea_lantern")
    kano(b, 152, 170, 6)
    kano(b, 158, 172, 7)
    kano(b, 152, 177, 5)
    # the harbour master's igloo, racks of drying fish (kelp), crates
    iglo(b, 163, 154, 3, y, "south", kleur="blue", bord=["Havenmeester", "Aanleggen kost", "1 vis per dag.", "Vuurnatie: 100"])
    for x in (166, 168):
        for yy in (y + 1, y + 2):
            b.set(x, yy, 164, "spruce_fence")
    b.set(167, y + 2, 164, "spruce_fence")
    b.set(167, y + 1, 164, "cobweb")
    for x, z, n in ((169, 156, 2), (170, 156, 1), (169, 157, 1), (171, 158, 2), (160, 160, 1), (148, 166, 1)):
        for i in range(n):
            ton(b, x, y + 1 + i, z)
    b.sign(157, y + 1, 164, "south", ["Haven", "Zwemmen mag.", "Het water is", "wel heel nat."], wall=False)
    b.sign(172, y + 1, 159, "west", ["Steiger 2", "Bezet door een", "HEEL groot", "schip. Sorry."], wall=False)
    lantaarnpaal(b, 180, y + 1, 153)
    lantaarnpaal(b, 196, y + 1, 153)
    lantaarnpaal(b, 196, y + 1, 159)
    lantaarnpaal(b, 165, y + 1, 159)
