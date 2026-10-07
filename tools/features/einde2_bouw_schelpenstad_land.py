"""
einde2, slice bouw_schelpenstad - the land of the road trip: the slab the whole build lies on (an irregular rim, a tapering
underside, a ridge of rock around the loopgraaf, the beach), the road with its kerbs and verge lamps, and the loopgraaf
itself: the trench, the path down its west wall and up its east wall, the perches of the monsters and what lies on its floor.
"""
import math
from collections import deque

from features import guheinde_bouw as gb
from features.einde2_bouw_schelpenstad_basis import *  # noqa: F401,F403
from features.einde2_bouw_schelpenstad_basis import kit

N1, N2, N3, N4 = Ruis(SEED + 1), Ruis(SEED + 2), Ruis(SEED + 3), Ruis(SEED + 4)

# where the kerb of the road is open: (x0, x1, side) with side -1 = north, 1 = south
KERF = [(29, 34, -1), (38, 50, -1),            # the door path and the parking of De Ruige Kroeg
        (16, 17, 1), (44, 45, 1), (66, 67, -1), (72, 73, 1),
        (140, 141, 1), (150, 151, -1), (156, 157, 1)]
BERMLAMP = 8                                    # a verge lamp on both sides every so many blocks


# =====================================================================================================================
# the slab
# =====================================================================================================================
def rand_z(x):
    """The north and south rim of the land at x (None: no land)."""
    if x < 0 or x > LAND_OOST:
        return None
    lo = 10 + 4.0 * (N1(x, 0, 14) - 0.5) + 2.0 * (N2(x, 7, 5) - 0.5)
    hi = 86 + 4.0 * (N1(x, 40, 14) - 0.5) + 2.0 * (N2(x, 47, 5) - 0.5)
    k = glad((x - 78) / 8) * glad((138 - x) / 8)              # around the loopgraaf the slab is at its widest
    lo, hi = lo + (3 - lo) * k, hi + (92 - hi) * k
    if x < 20:                                                 # the causeway widens into the plain
        t = glad((x - 6) / 14)
        lo, hi = 44 + (lo - 44) * t, 52 + (hi - 52) * t
    if x > 196:                                                # the east end is round: the shore
        f = math.sqrt(max(0.0, 1 - ((x - 196) / (LAND_OOST - 195.5)) ** 2))
        lo, hi = 46 - (46 - lo) * f, 46 + (hi - 46) * f
    return int(round(lo)), int(round(hi))


def is_land(x, z):
    r = rand_z(x)
    return r is not None and r[0] <= z <= r[1]


def richel(x):
    """How much of the rocky ridge around the loopgraaf stands at x (0..1)."""
    return glad((x - 58) / 24) * glad((160 - x) / 24)


def strand(x, z):
    """How far into the beach this spot of Schelpenstad lies (0: not on the beach)."""
    return max(z - STRAND_Z, x - STRAND_X, 0) if x >= STAD_X + 2 else 0


def hoogte(x, z):
    """The y of the ground at (x, z), before roads and buildings."""
    if x >= STAD_X:
        return STAD_Y - min(3, strand(x, z) // 4)
    g, d, r = wegy(x), abs(z - wegz(x)), richel(x)
    vlak = 10 <= x <= 58 and 10 <= z <= wegz(x)               # the yard of De Ruige Kroeg
    bult = r * glad((d - 7) / 11) * (3.5 + 7.5 * N3(x, z, 11))
    klein = 0.0 if vlak else glad((d - 8) / 6) * (N4(x, z, 6) - 0.5) * 3.4 * (1 - r)
    return int(round(g + bult + klein))


def kloof_w(y, z):
    """The west face of the loopgraaf at height y (cells east of it are open)."""
    lean = (RAND_Y - y) * HELLING if y <= RAND_Y else -(y - RAND_Y) * 0.8
    return KLOOF_W + lean + 1.8 * (N2(z + y * 0.7, y * 1.3, 6) - 0.5)


def kloof_o(y, z):
    lean = (RAND_Y - y) * HELLING if y <= RAND_Y else -(y - RAND_Y) * 0.8
    return KLOOF_O - lean - 1.8 * (N2(z - y * 0.7 + 300, y * 1.3, 6) - 0.5)


def in_kloof(x, y, z):
    """Is this cell part of the open trench?"""
    if y <= BODEM or not (KLOOF_W - 12 < x < KLOOF_O + 12):
        return False
    lean = (RAND_Y - y) * 0.25 if y <= RAND_Y else -(y - RAND_Y) * 0.8
    if not (KLOOF_Z0 + lean < z < KLOOF_Z1 - lean):
        return False
    return kloof_w(y, z) < x < kloof_o(y, z)


def afstand_tot_rand(land):
    """For every land column: how many columns to the nearest column that is not land (1 = on the rim)."""
    dist, todo = {}, deque()
    for (x, z) in land:
        if any((x + dx, z + dz) not in land for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            dist[(x, z)] = 1
            todo.append((x, z))
    while todo:
        x, z = todo.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if n in land and n not in dist:
                dist[n] = dist[(x, z)] + 1
                todo.append(n)
    return dist


def stof(x, y, z, top, onder, r):
    """The block of the slab at this cell."""
    diep = top - y
    n = N4(x * 1.7 + y * 3.1, z * 1.7 - y * 2.3, 3)
    if KLOOF_W - 7 <= x <= KLOOF_O + 7 and KLOOF_Z0 - 1 <= z <= KLOOF_Z1 + 1 and y < RAND_Y - 2:   # the walls of the loopgraaf darken on the way down
        t = y + 3.0 * (n - 0.5)
        if t < 6:
            return "minecraft:blackstone" if n > 0.78 else KOOL
        if t < 11:
            return KOOL if n > 0.45 else BELEGEN
        if t < 16:
            return BELEGEN if n > 0.5 else ROTS
        return ROTS if n > 0.4 else KORST
    if x >= STAD_X and strand(x, z) >= 1 and diep < 2:
        return ZAND
    if diep == 0:
        if x >= STAD_X:
            return KORST
        if r > 0.35:
            return ROTS if n > 0.4 - 0.3 * r else KORST
        m = N3(x + 500, z, 7)
        return BARST if m > 0.36 else (KORST if n > 0.3 else ROTS)
    if y - onder < 2:
        return ROTS if n > 0.35 else BELEGEN
    if diep < 4:
        return KORST
    return KORST if n > 0.42 else ROTS


def slab(b):
    """The land itself. Returns {(x, z): top y}."""
    land = {(x, z) for x in range(W) for z in range(D) if is_land(x, z)}
    dist = afstand_tot_rand(land)
    tops = {}
    for (x, z) in sorted(land):
        top, r = hoogte(x, z), richel(x)
        d = dist[(x, z)]
        dik = min(15.0, 1.5 + 0.85 * d + 2.5 * N1(x + 90, z, 9))
        onder = max(1, int(round(top - dik)))
        if x < 10:                                              # the causeway: deck, two of kaaskorst and a tapering keel
            onder = WEG_Y - 2 - max(0, 3 - abs(z - 48))
        else:                                                   # under the loopgraaf the slab is a keel: all the way down
            f = glad(max(KLOOF_W - 5 - x, x - KLOOF_O - 5, 0) / 14)
            onder = max(1, int(round(top - 1.5 - 2.2 * d)), int(round(1 + (onder - 1) * f)))
        tops[(x, z)] = top
        for y in range(onder, top + 1):
            if not in_kloof(x, y, z):
                b.set(x, y, z, stof(x, y, z, top, onder, r))
    for (x, z), top in tops.items():                            # crumb sand falls: nothing but a solid block under it
        if b.naam(x, top, z) == ZAND and b.naam(x, top - 2, z) is None:
            b.set(x, top - 1, z, KORST)
            b.set(x, top, z, KORST)
    return tops


# =====================================================================================================================
# the road
# =====================================================================================================================
def weg_stuk(b, x0, x1, stoep_tot=None):
    """The road from x0 to x1 (inclusive): the profile of kit.WEG, following wegz and wegy. stoep_tot: no kerb beyond it."""
    Wg = kit.WEG
    for x in range(x0, x1 + 1):
        y = wegy(x)
        mids = [wegz(xx) for xx in (x - 1, x, x + 1) if x0 <= xx <= x1]
        dek = {z for c in mids for z in range(c - 3, c + 4)}
        stoep = {z for c in mids for z in range(c - 4, c + 5)} - dek
        for z in sorted(dek | stoep):
            for d in range(1, Wg["onder"] + 1):
                b.set(x, y - d, z, Wg["grond"])
            b.set(x, y, z, Wg["streep"] if z == wegz(x) else Wg["dek"])
            b.wis(x, y + 1, z, x, y + 8, z)
            b.vast.add((x, y + 1, z))
        for z in sorted(stoep):
            zij = -1 if z < wegz(x) else 1
            open_ = any(a <= x <= c and s == zij for a, c, s in KERF) or (stoep_tot is not None and x > stoep_tot)
            if not open_:
                b.set(x, y + 1, z, Wg["rand"])
        # one step: half a block first (the last column before a rise, the first after a drop)
        if x + 1 in STAP_OP or x in STAP_AF:
            for z in sorted(dek):
                b.set(x, y + 1, z, PLAAT, gb.slab())
        # the verge lamps, just outside the kerb on both sides
        if x % BERMLAMP == 2 and not (stoep_tot is not None and x > stoep_tot):
            for zij in (-1, 1):
                z = wegz(x) + 5 * zij
                if any(a - 1 <= x <= c + 1 and s == zij for a, c, s in KERF):
                    continue
                if b.naam(x, y, z) is None:                     # (on the causeway: a corbel to stand on)
                    b.set(x, y, z, Wg["grond"])
                    b.set(x, y - 1, z, Wg["grond"])
                b.wis(x, y + 1, z, x, y + 4, z)
                b.set(x, y, z, STENEN)
                b.paal(x, y + 1, z, 1, MUUR, "guhs:lampion_geel")


def weg(b):
    weg_stuk(b, 0, X_WEST_EIND)
    weg_stuk(b, X_OOST_BEGIN, X_OOST_EIND, stoep_tot=STAD_X + 2)


def weg_punten(x0, x1, stap=6):
    """Points along the stripe (walking height), for the pads."""
    xs = sorted(set(list(range(x0, x1, stap)) + [x1] + [p[0] for p in WEG_W + WEG_O if x0 <= p[0] <= x1]))
    return [(x, wegy(x) + 1 + (1 if (x + 1 in STAP_OP or x in STAP_AF) else 0), wegz(x)) for x in xs]


def weg_fouten(b, x0, x1, naam):
    """Is the road drivable from x0 to x1: at least 7 wide at one height, no step above 1, 6 free above it?"""
    p, vorige = [], None
    for x in range(x0, x1 + 1):
        c = wegz(x)
        hoog = {}
        for z in range(c - 3, c + 4):
            g = b.grond(x, z, van=wegy(x) + 1)
            hoog[z] = g
            if g is None:
                p.append(f"{naam} x {x} z {z}: geen wegdek")
                continue
            vrij = [y for y in range(g + 1, g + 7) if b.s.blocks.get((x, y, z)) is not None]
            if vrij:
                p.append(f"{naam} x {x} z {z}: {b.naam(x, vrij[0], z)} boven de weg (6 vrij)")
        vals = {v for v in hoog.values() if v is not None}
        if len(vals) > 1:
            p.append(f"{naam} x {x}: het wegdek is niet vlak over 7 breed ({sorted(vals)})")
        nu = max(vals) if vals else None
        if vorige is not None and nu is not None and abs(nu - vorige) > 1:
            p.append(f"{naam} x {x}: een stap van {abs(nu - vorige)}")
        vorige = nu
    return p[:30]


# =====================================================================================================================
# the loopgraaf
# =====================================================================================================================
PAD_W = [(89, 22, 48), (92, 22, 50), (96, 10, 74), (98, 10, 77), (101, 10, 75), (102, 4, 63)]       # down the west wall
PAD_O = [(116, 4, 36), (117, 10, 24), (119, 10, 22), (122, 10, 24), (126, 22, 48), (129, 22, 50)]   # up the east wall
SPOOR = [(102, 4, 63), (105, 4, 56), (109, 4, 49), (113, 4, 42), (116, 4, 36)]                      # across the floor
# the perches of the monsters: (x, y, z, the way the mouth of its cave looks, reachable on foot)
NISSEN = [(95, 14, 22, "east", False), (97, 9, 37, "east", False), (123, 14, 62, "west", False), (121, 9, 75, "west", False),
          (110, 4, 14, "south", True), (108, 4, 82, "north", True)]


def pad_punten(punten):
    """Every step of a path through its corner points: (x, y, z) with y changing at most one per step."""
    out = []
    for (x0, y0, z0), (x1, y1, z1) in zip(punten, punten[1:]):
        n = max(abs(x1 - x0), abs(z1 - z0))
        for i in range(n + (1 if (x1, y1, z1) == tuple(punten[-1]) else 0)):
            t = i / n
            out.append((int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t)), int(round(z0 + (z1 - z0) * t))))
    return out


def pad_leggen(b, punten, breed=3, vrij=4, wand=(-1, 0)):
    """A path as a ledge: every column within breed / 2 of the path gets the floor of the nearest step, free room above
    it and rock under it down to whatever is there (so a ledge never hangs loose); every sixth step a soul lantern on the
    side of the wall (wand = the way to the wall). Returns its steps."""
    stappen = pad_punten(punten)
    kolom = {}
    half = breed // 2
    for i, (x, y, z) in enumerate(stappen):
        for dx in range(-half, half + 1):
            for dz in range(-half, half + 1):
                c = (x + dx, z + dz)
                d = dx * dx + dz * dz
                if c not in kolom or d < kolom[c][0]:
                    kolom[c] = (d, y)
    for (x, z), (_, y) in sorted(kolom.items()):
        b.wis(x, y, z, x, y + vrij - 1, z)
        b.set(x, y - 1, z, STENEN if y > 14 else KOOLSTENEN)
        b.vast.add((x, y, z))
        yy = y - 2
        while yy >= 1 and b.naam(x, yy, z) is None:
            b.set(x, yy, z, stof(x, yy, z, y - 1, 1, 1.0))
            yy -= 1
    for i, (x, y, z) in enumerate(stappen):
        if i % 6 == 3 and kolom.get((x + wand[0], z + wand[1]), (0, None))[1] == y:
            b.set(x + wand[0], y, z + wand[1], "minecraft:soul_lantern", gb.lamp(False))
    return stappen


def nis(b, x, y, z, kijkt):
    """A perch: a little cave in the wall with a lip to sit on and teeth of stone around its mouth. (x, y, z) = where the
    monster sits (its feet)."""
    dx, dz = gb.DIRS[kijkt]
    px, pz = -dz, dx                                             # across the mouth
    for diep in range(-3, 3):                                    # from 3 into the wall to 2 out of it
        for breed in range(-2, 3):
            cx, cz = x + dx * diep + px * breed, z + dz * diep + pz * breed
            hoog = 4 if abs(breed) < 2 else 3
            b.wis(cx, y, cz, cx, y + hoog - 1, cz)
            if b.naam(cx, y - 1, cz) is None or diep <= 0:
                b.set(cx, y - 1, cz, KOOLSTENEN if y < 12 else STENEN)
            if diep > 0 and b.naam(cx, y - 2, cz) is None:       # the lip is two thick, and hangs on the wall
                b.set(cx, y - 2, cz, KOOL)
    for diep in range(-4, 0):                                    # rock behind and above the cave, whatever the wall did there
        for breed in range(-3, 4):
            for yy in range(y - 1, y + 5):
                cx, cz = x + dx * diep + px * breed, z + dz * diep + pz * breed
                rand = diep == -4 or abs(breed) == 3 or yy == y + 4
                if rand and b.naam(cx, yy, cz) is None and b.inside(cx, yy, cz):
                    b.set(cx, yy, cz, KOOL)
    for breed in (-2, 2):                                        # the teeth
        cx, cz = x + dx * 2 + px * breed, z + dz * 2 + pz * breed
        b.set(cx, y, cz, "minecraft:pointed_dripstone", {"thickness": "tip", "vertical_direction": "up", "waterlogged": "false"})
    b.vast.add((x, y, z))


def ijskraam(b, x, y, z):
    """The stand on the floor of the loopgraaf: FREE ICE CREAM. (Nobody is ever behind the counter.)"""
    for dx in range(0, 4):
        b.set(x + dx, y, z, "minecraft:white_concrete" if dx % 2 else "minecraft:pink_concrete")
        b.set(x + dx, y + 3, z, "minecraft:pink_wool" if dx % 2 else "minecraft:white_wool")
        b.set(x + dx, y + 3, z + 1, "minecraft:white_wool" if dx % 2 else "minecraft:pink_wool")
    for dx in (0, 3):
        b.set(x + dx, y + 1, z, "minecraft:birch_fence")
        b.set(x + dx, y + 2, z, "minecraft:birch_fence")
    b.set(x + 1, y + 1, z, "guhs:guh_taart", {"bites": "0"})
    b.set(x + 2, y + 1, z, "minecraft:candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    b.bord(x + 1, y, z + 1, "south", ["GRATIS IJS", "Kom dichterbij", "Nog dichterbij", "Njeg..."])
    b.bord(x + 2, y, z + 1, "south", ["Echt geen val", "Erewoord van", "een monster"])
    for dx in range(0, 4):
        b.vast.add((x + dx, y, z + 1))


def spits(b, x, z, hoog, rng):
    """A spire of dark rock on the floor of the loopgraaf."""
    y0 = b.grond(x, z, van=BODEM + 2)
    if y0 is None:
        return
    for d in range(hoog):
        r = 1 if d < hoog * 0.45 else 0
        for ax in range(-r, r + 1):
            for az in range(-r, r + 1):
                if abs(ax) + abs(az) <= r and (x + ax, z + az) not in VRIJ_SPOOR:
                    b.set(x + ax, y0 + 1 + d, z + az, KOOL if rng.random() > 0.2 else "minecraft:blackstone")
    b.set(x, y0 + 1 + hoog, z, "minecraft:pointed_dripstone", {"thickness": "tip", "vertical_direction": "up", "waterlogged": "false"})


VRIJ_SPOOR = set()       # columns of the floor that stay free: the trail, the stand


def loopgraaf(b):
    """Everything in the trench. Returns the steps of the two paths (down, up)."""
    rng = b.rng
    # the two rims: a level terrace where the road ends, with a low wall along the drop
    for (xa, xb, zc) in ((85, 90, 46), (128, 133, 50)):
        for x in range(xa, xb + 1):
            for z in range(zc - 9, zc + 10):
                if b.grond(x, z) is None:
                    continue
                b.wis(x, RAND_Y + 1, z, x, H - 1, z)
                for y in range(max(2, RAND_Y - 4), RAND_Y + 1):
                    if b.naam(x, y, z) is None:
                        b.set(x, y, z, ROTS)
                if abs(z - wegz(x)) > 4:
                    b.set(x, RAND_Y, z, STENEN if (x + z) % 3 else KORSTSTENEN)
    weg_stuk(b, 85, X_WEST_EIND)                                 # (the terrace took the kerbs: put the road back)
    weg_stuk(b, X_OOST_BEGIN, 133)
    for z in range(37, 56):                                      # a low wall along the drop (the path cuts its own way through)
        b.set(90, RAND_Y + 1, z, MUUR)
    for z in range(41, 60):
        b.set(128, RAND_Y + 1, z, MUUR)
    omlaag = pad_leggen(b, PAD_W, wand=(-1, 0))
    omhoog = pad_leggen(b, PAD_O, wand=(1, 0))
    for x, y, z in pad_punten(SPOOR) + [SPOOR[-1]]:              # the trail across the floor: worn stones
        for dx, dz in ((0, 0), (1, 0), (0, 1)):
            VRIJ_SPOOR.add((x + dx, z + dz))
            if b.naam(x + dx, BODEM, z + dz) is not None and rng.random() < 0.8:
                b.set(x + dx, BODEM, z + dz, KOOLSTENEN)
            b.wis(x + dx, BODEM + 1, z + dz, x + dx, BODEM + 4, z + dz)
            b.vast.add((x + dx, BODEM + 1, z + dz))
    for (x, y, z, kijkt, _) in NISSEN:
        nis(b, x, y, z, kijkt)
    for dx in range(-1, 6):
        for dz in range(-1, 4):
            VRIJ_SPOOR.add((104 + dx, 44 + dz))
    ijskraam(b, 104, BODEM + 1, 44)
    # spires and mould: the floor is no ballroom
    bezet = VRIJ_SPOOR | {(x, z) for (x, _, z) in b.vast}
    for _ in range(46):
        x, z = rng.randint(100, 118), rng.randint(14, 82)
        if all((x + ax, z + az) not in bezet for ax in range(-2, 3) for az in range(-2, 3)) and \
                all(math.hypot(x - n[0], z - n[2]) > 6 for n in NISSEN):
            spits(b, x, z, rng.randint(2, 7), rng)
    return omlaag, omhoog


def schimmel(b, cellen, rng, kans=0.07):
    """Glowing mould in the dark: a patch of schimmelkaas with a schimmelpaddenstoel on it, on some of these floor cells."""
    for (x, y, z) in sorted(cellen):
        if rng.random() < kans and (x, y, z) not in b.vast and b.naam(x, y, z) is None and b.naam(x, y + 1, z) is None \
                and b.solid_at((x, y - 1, z)) and b.naam(x, y - 1, z) in (KOOL, BELEGEN, ROTS, KOOLSTENEN, "minecraft:blackstone"):
            b.set(x, y - 1, z, SCHIMMEL)
            b.set(x, y, z, ZWAM)
