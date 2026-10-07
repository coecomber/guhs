"""
einde2, slice bouw_zij2: the Guhvleugel-ringenrace (Kruimelwolken). Build only: the race itself (timing, records) is a
later phase, which reads the markers.

One crumb island wrapped in cloud is start AND finish: the Starttoren on its north side (you take off to the east), the
Landingsbaan on its south side (you come back from the east), the scoreboard and the stand between them. The course is a
loop of eighteen floating rings of guhs:ringenrace_ring: out along the north lane, around the far turn, back along the
south lane, with dives and climbs. Each ring is a marker ring_01 .. ring_18 (centre, axis, the free radius).

    bouw(h) -> the kit.Bouw, checked (raises SystemExit on a problem)
"""
import math
import random

import features.einde2_kit as kit
from features import einde2_bouw_zij2_gereedschap as g
from features import guheinde_bouw as gb

NAAM = "guhvleugel_ringenrace"
W, H, D = 256, 80, 96
Y = 150                                   # the world y of template y 0
RING = "guhs:ringenrace_ring"
VRIJ = 5                                  # the free radius of a ring (an Enderguh with a rider fits)
CX, CZ, TOP = 40, 48, 40                  # the island: its middle and its ground level
ROTS, STENEN, TRAP, PLAAT, MUUR = "guhs:kruimelrots", "guhs:kruimelrots_stenen", "guhs:kruimelrots_stenen_trap", \
    "guhs:kruimelrots_stenen_plaat", "guhs:kruimelrots_stenen_muur"
WOLK, DICHT = "guhs:kruimelwolk", "guhs:dichte_kruimelwolk"
START_Y, FINISH_Y = 54, 44                # the deck of the Starttoren and of the Landingsbaan

# the course: (x, y, z, axis). North lane to the east, the turn, south lane back to the west.
BAAN = [(84, 56, 26, "x"), (104, 52, 23, "x"), (124, 45, 28, "x"), (144, 40, 22, "x"), (164, 46, 30, "x"), (184, 56, 26, "x"),
        (204, 63, 21, "x"), (224, 59, 28, "x"), (241, 54, 42, "z"), (241, 49, 57, "z"), (224, 45, 71, "x"), (204, 39, 75, "x"),
        (184, 33, 68, "x"), (164, 30, 73, "x"), (144, 36, 66, "x"), (124, 44, 72, "x"), (104, 48, 68, "x"), (86, 48, 70, "x")]


def ring(b, i, x, y, z, as_):
    """One ring of the course: a hoop of ringenrace_ring blocks, one block thick, with four lichtstaafjes as spokes' ends."""
    for u in range(-VRIJ - 2, VRIJ + 3):
        for v in range(-VRIJ - 2, VRIJ + 3):
            if VRIJ + 0.3 < math.hypot(u, v) <= VRIJ + 1.45:
                if as_ == "x":
                    b.set(x, y + v, z + u, RING)
                else:
                    b.set(x + u, y + v, z, RING)
    b.ring(f"ring_{i:02d}", x, y, z, as_=as_, straal=VRIJ)


def eiland(b):
    grond = g.eiland(b, CX, CZ, 27, 34, TOP, 16, "guhs:kaaskorst", [(ROTS, 70), ("guhs:kaaskorst", 30)], NAAM, rafel=0.16, onder=ROTS)
    # a collar of dense cloud around its foot and loose puffs under it: the island sits in a cloud
    rng = random.Random(kit.seed(NAAM + "/kraag"))
    for k in range(26):
        hoek = k / 26 * math.tau
        r = rng.uniform(0.9, 1.08)
        g.bol(b, CX + math.cos(hoek) * 27 * r, TOP - rng.randint(5, 9), CZ + math.sin(hoek) * 34 * r, rng.uniform(3.0, 4.6), DICHT,
              alleen_leeg=True, plat=0.5)
    return grond


def plein(b, grond):
    """The paved middle: the landing pad in the west, the square, crumb-rock kerbs."""
    for (x, z), t in grond.items():
        d = math.hypot((x - CX) / 22.0, (z - CZ) / 28.0)
        if d < 1:
            b.set(x, t, z, STENEN if (x // 3 + z // 3) % 2 else ROTS)
    # the aankomst: 11 x 11, a pink guh nose in the middle so you know where to aim
    for x in range(19, 30):
        for z in range(43, 54):
            rand = x in (19, 29) or z in (43, 53)
            b.set(x, TOP, z, "pink_terracotta" if rand else "guhs:kaaskorst_stenen")
    for x, z in ((24, 48), (23, 48), (25, 48), (24, 47), (24, 49)):
        b.set(x, TOP, z, "pink_concrete")
    b.zone("aankomst", 19, TOP + 1, 43, 29, TOP + 8, 53, wie="de landingsplek van de ringenrace")
    b.prop("aankomst_speler", 24, TOP + 1, 48)
    b.prop("uitzicht", -28, 66, 4, wie="over het eiland heen de hele baan in")
    b.sign(30, TOP + 1, 48, "west", ["Guhvleugel-", "ringenrace", "Vleugels aan?", "Njeg, dan lopen"], wall=False)


def starttoren(b):
    """A square tower of crumb-rock bricks on the north side with the take-off deck on top (it sticks out to the east), a
    broad stair up its west and south faces, the start line and the race master's box."""
    x0, x1, z0, z1 = 44, 56, 18, 30
    for y in range(TOP + 1, START_Y):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                rand = x in (x0, x1) or z in (z0, z1)
                hoek = x in (x0, x1) and z in (z0, z1)
                if rand:
                    b.set(x, y, z, "guhs:kruimelrots" if hoek else (STENEN if (y - TOP) % 4 else "guhs:kaaskorst_stenen"))
    # the stair: up the south face from the west (13 steps, x 31 -> 43 would leave the tower: it runs along the face)
    for k in range(START_Y - TOP):
        x, y = x0 - 1 + k, TOP + 1 + k
        for z in (z1 + 1, z1 + 2, z1 + 3):
            g.trap(b, x, y, z, TRAP, "east")
            for yy in range(TOP + 1, y):
                b.set(x, yy, z, STENEN)
        b.set(x, y + 1, z1 + 4, MUUR)
        if k % 4 == 1:
            b.set(x, y + 2, z1 + 4, "guhs:lampion_geel", gb.lamp(False))
        b.set(x, y, z1 + 4, STENEN)
        for yy in range(TOP + 1, y):
            b.set(x, yy, z1 + 4, STENEN)
    # the deck: the tower top and a tongue to the east, cloud under the tongue
    for x in range(x0 - 1, 71):
        for z in range(z0 - 1, z1 + 5):
            if x > x1 + 1 and not (z0 + 1 <= z <= z1 - 1):
                continue
            tong = x > x1 + 1
            b.set(x, START_Y, z, DICHT if tong and (x + z) % 2 else STENEN)
            if tong:
                b.set(x, START_Y - 1, z, DICHT)
    for x in range(x1 + 2, 71):
        for z in range(z0 + 2, z1 - 1):
            if (x + z) % 3 == 0:
                b.set(x, START_Y - 2, z, DICHT)
    # the stair comes up through the deck: open the three lanes above its last steps
    for x in range(x0 - 1, x0 + 14):
        for z in (z1 + 1, z1 + 2, z1 + 3):
            if TOP + 1 + (x - (x0 - 1)) < START_Y:
                for y in range(TOP + 2 + (x - (x0 - 1)), START_Y + 1):
                    if b.get(x, y, z) is not None:
                        b.s.blocks.pop((x, y, z), None)
    # the railing (not where the stair arrives, not at the take-off edge)
    for x in range(x0 - 1, 71):
        for z in range(z0 - 1, z1 + 5):
            if b.get(x, START_Y, z) is None:
                continue
            rand = any(b.get(x + dx, START_Y, z + dz) is None for dx, dz in gb.DIRS.values())
            if rand and x < 70 and not (z in (z1 + 1, z1 + 2, z1 + 3) and x <= x0 + 13) and b.get(x, START_Y + 1, z) is None:
                b.set(x, START_Y + 1, z, MUUR)
    # the start line across the tongue, two masts with banners, the board
    g.schaakrand(b, [(66, z) for z in range(z0 + 1, z1)] + [(67, z) for z in range(z0 + 1, z1)], START_Y)
    b.prop("startlijn", 66, START_Y, 24, wie="de startstreep (twee breed, over de hele tong)")
    b.zone("start", x0 - 1, START_Y + 1, z0 - 1, 70, START_Y + 9, z1 - 1, wie="het startplatform")
    for z in (z0 + 1, z1 - 1):
        for k in range(1, 6):
            b.set(65, START_Y + k, z, "spruce_fence")
        b.set(65, START_Y + 6, z, "guhs:lampion_roze", gb.lamp(False))
        g.vlag(b, 66, START_Y + 5, z, "pink", facing="east")
    # the race master's box: a little raised desk on the north side of the deck
    for x in range(58, 62):
        b.set(x, START_Y + 1, z0, "spruce_planks")
    g.trap(b, 57, START_Y + 1, z0, "spruce_stairs", "east")
    b.set(61, START_Y + 2, z0, "spruce_fence")
    b.set(61, START_Y + 3, z0, "guhs:lampion_geel", gb.lamp(False))
    b.plek("racemeester", 59, START_Y + 2, z0, yaw=0.0, wie="de racemeester-guh, op zijn kistje naast de startstreep", kind="RINGENRACEGUH")
    b.sign(62, START_Y + 1, z0 + 1, "south", ["START", "Ringen op", "volgorde!", "Njeg smokkelen"], wall=False)
    b.sign(x0 - 2, TOP + 1, z1 + 2, "west", ["Starttoren", "13 treden", "Vadsige guhs:", "neem de tijd"], wall=False)
    for x, z in ((x0 + 3, z0 + 3), (x1 - 3, z0 + 3), (x0 + 3, z1 - 3), (x1 - 3, z1 - 3), (62, 24)):
        b.set(x, START_Y, z, "guhs:guh_kristal_lamp")
    # a wind sock of wool on the roof corner
    for k in range(1, 5):
        b.set(x0, START_Y + k, z0, "spruce_fence")
    for k, kleur in enumerate(("orange_wool", "white_wool", "orange_wool")):
        b.set(x0 + 1 + k, START_Y + 4, z0, kleur)


def landingsbaan(b):
    """The finish on the south side: a long deck of dense cloud and brick (you come in from the east), the chequered line
    under an arch, a cushion of soft cloud at the end, a stair down to the square."""
    x0, x1, z0, z1 = 40, 76, 62, 78
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            rand = z in (z0, z1)
            b.set(x, FINISH_Y, z, STENEN if rand or x % 6 == 0 else DICHT)
            if x > 58:                                           # (the part that hangs over the rim rests on cloud)
                b.set(x, FINISH_Y - 1, z, DICHT)
                if (x + z) % 2 == 0:
                    b.set(x, FINISH_Y - 2, z, DICHT)
            if rand and x < x1:
                b.set(x, FINISH_Y + 1, z, MUUR)
    for x in range(x0, 59):                                      # (legs down to the island where it stands on it)
        for z in (z0, z1):
            if x % 6 == 0:
                for y in range(TOP - 2, FINISH_Y):
                    if b.get(x, y, z) is None:
                        b.set(x, y, z, MUUR)
    # the finish line and its arch
    g.schaakrand(b, [(x, z) for x in (62, 63) for z in range(z0 + 1, z1)], FINISH_Y)
    b.prop("finishlijn", 62, FINISH_Y, 70, wie="de finishstreep (twee breed, over de hele baan)")
    b.zone("finish", x0, FINISH_Y + 1, z0, x1, FINISH_Y + 9, z1, wie="de landingsbaan")
    for z in (z0, z1):
        for y in range(FINISH_Y + 1, FINISH_Y + 9):
            b.set(62, y, z, "white_concrete" if y % 2 else "black_concrete")
    for z in range(z0, z1 + 1):
        b.set(62, FINISH_Y + 9, z, "white_concrete" if z % 2 else "black_concrete")
    for z in (z0 + 4, z1 - 4):
        g.hanglamp(b, 62, FINISH_Y + 8, z, lamp="guhs:lampion_geel")
    b.sign(61, FINISH_Y + 8, 70, "west", ["FINISH", "", "Remmen mag.", "Hoeft njeg."])
    b.set(62, FINISH_Y + 8, 70, "white_concrete")
    # the cushion at the west end: soft cloud you sink into, behind a low brick kerb
    for x in range(x0, x0 + 4):
        for z in range(z0 + 1, z1):
            for y in range(FINISH_Y + 1, FINISH_Y + 6):
                b.set(x, y, z, WOLK)
    for z in range(z0 + 1, z1):
        if z % 4 == 2:
            b.set(x0 + 4, FINISH_Y + 1, z, "guhs:guh_kristal_lamp")
        else:
            b.set(x0 + 4, FINISH_Y + 1, z, PLAAT, {"type": "bottom", "waterlogged": "false"})
    b.sign(x0 + 5, FINISH_Y + 1, z0 + 2, "east", ["Landingskussen", "Zacht.", "Echt waar.", "(meestal)"], wall=False)
    for x in range(46, 60, 6):
        b.set(x, FINISH_Y, 70, "guhs:guh_kristal_lamp")
    # the stair from the baan down to the square: north side, four steps
    for k in range(FINISH_Y - TOP):
        z, y = z0 - 1 - k, FINISH_Y - k
        for x in (50, 51, 52):
            g.trap(b, x, y, z, TRAP, "south")
            for yy in range(TOP + 1, y):
                b.set(x, yy, z, STENEN)
    for x in (50, 51, 52):
        b.s.blocks.pop((x, FINISH_Y + 1, z0), None)
    for x in (49, 53):
        g.lantaarnpaal(b, x, TOP + 1, z0 - 3, hoogte=3)


def scorebord(b):
    """Between tower and baan, facing the square: a wall of five by three in a frame of logs under a little roof."""
    x, z0 = 55, 43
    for z in range(z0 - 1, z0 + 6):
        b.set(x, TOP + 1, z, "guhs:kaaskorst_stenen")
        b.set(x, TOP + 5, z, "stripped_dark_oak_log", {"axis": "z"})
        g.plaat(b, x, TOP + 6, z, "spruce_slab")
        g.plaat(b, x - 1, TOP + 6, z, "spruce_slab")
    for y in range(TOP + 2, TOP + 5):
        for z in range(z0 - 1, z0 + 6):
            b.set(x, y, z, "black_concrete" if z0 <= z <= z0 + 4 else "stripped_dark_oak_log", None if z0 <= z <= z0 + 4 else {"axis": "y"})
    b.prop("scorebord", x, TOP + 2, z0 + 2, wie="het scorebord: het middelste blok van de onderste rij; 5 breed (z 43-47), 3 hoog (y 42-44), kijkt west")
    b.sign(x - 1, TOP + 5, z0 + 2, "west", ["~ RECORDS ~", "snelste guh", "van de leegte", ""])
    for z in (z0 - 1, z0 + 5):
        b.set(x - 1, TOP + 1, z, "spruce_fence")
        b.set(x - 1, TOP + 2, z, "guhs:lampion_roze", gb.lamp(False))


def aankleding(b, grond):
    """What makes the island a place: the winners' podium, boulders of crumb rock along the rim, tufts of korstgras."""
    # the podium, next to the scoreboard: gold in the middle and highest, of course
    for z, blok, hoog in ((50, "iron_block", 2), (51, "gold_block", 3), (52, "copper_block", 1)):
        for k in range(hoog):
            b.set(53, TOP + 1 + k, z, blok)
    b.sign(52, TOP + 1, 51, "west", ["2    1    3", "Erepodium", "(vierde guh:", "ook knap, njeg)"])
    rng = random.Random(kit.seed(NAAM + "/aankleding"))
    for k in range(9):
        hoek = k / 9 * math.tau + 0.3
        x, z = int(CX + math.cos(hoek) * 23), int(CZ + math.sin(hoek) * 29)
        if (x, z) in grond and b.get(x, TOP + 1, z) is None and not (17 <= x <= 31 and 41 <= z <= 55) and not (x > 38 and (z < 36 or z > 58)):
            g.bol(b, x, TOP + 1, z, rng.uniform(1.2, 2.1), lambda *_c: ROTS if rng.random() < 0.75 else "guhs:kaaskorst", alleen_leeg=True, plat=0.8)
    for (x, z), t in sorted(grond.items()):
        if b.get(x, t, z) == "guhs:kaaskorst" and b.get(x, t + 1, z) is None and rng.random() < 0.1:
            b.set(x, t + 1, z, "guhs:korstgras")


def tribune(b):
    """Three rows of seats that look east over the square at the scoreboard, and the Kruimelkraam."""
    for rij in range(3):
        x = 36 - rij * 2
        for z in range(40, 57):
            if z == 48:
                continue                                          # (the aisle)
            for yy in range(TOP + 1, TOP + 1 + rij):
                b.set(x, yy, z, "spruce_planks")
                b.set(x + 1, yy, z, "spruce_planks")
            g.trap(b, x, TOP + 1 + rij, z, "spruce_stairs", "west")
            if rij:
                b.set(x + 1, TOP + rij, z, "spruce_planks")
    for rij in range(3):
        for dx in (0, 1):
            x = 36 - rij * 2 + dx
            for yy in range(TOP + 1, TOP + 1 + rij):
                b.set(x, yy, 48, "spruce_planks")
            if rij and dx == 0:
                g.trap(b, x + 1, TOP + rij, 48, "spruce_stairs", "west")
    for z in (39, 57):
        g.lantaarnpaal(b, 34, TOP + 1, z, hoogte=4)
    # the Kruimelkraam: a stall with a striped awning on the north-west of the square
    kx, kz = 30, 28
    for x in range(kx, kx + 5):
        b.set(x, TOP + 1, kz, "barrel", {"facing": "up", "open": "false"} if x % 2 else None)
        if x % 2 == 0:
            b.set(x, TOP + 1, kz, "spruce_planks")
        for z in range(kz - 2, kz + 2):
            b.set(x, TOP + 4, z, "orange_wool" if x % 2 else "white_wool")
    for x, z in ((kx, kz - 2), (kx + 4, kz - 2), (kx, kz + 1), (kx + 4, kz + 1)):
        for y in range(TOP + 1, TOP + 4):
            b.set(x, y, z, "spruce_fence")
    b.set(kx + 1, TOP + 2, kz, "minecraft:cake", {"bites": "0"})
    b.set(kx + 3, TOP + 2, kz, "guhs:block_of_kaasknabbels")
    g.hanglamp(b, kx + 2, TOP + 3, kz - 1, lamp="guhs:lampion_geel", ketting=0)
    b.sign(kx + 2, TOP + 2, kz + 1, "south", ["Kruimelkraam", "Vandaag:", "kruimels.", "Morgen: ook."], wall=False)
    b.s.blocks.pop((kx + 2, TOP + 2, kz + 1), None)
    b.sign(kx + 2, TOP + 1, kz + 1, "south", ["Kruimelkraam", "Vandaag:", "kruimels.", "Morgen: ook."], wall=True)


def baan(b):
    for i, (x, y, z, as_) in enumerate(BAAN, 1):
        ring(b, i, x, y, z, as_)
    # puffs of cloud along the course and a few crumb islets with a light on them: something to steer by
    rng = random.Random(kit.seed(NAAM + "/baan"))
    for i, (x, y, z, as_) in enumerate(BAAN):
        dx, dz = (0, 1) if as_ == "x" else (1, 0)
        kant = 1 if i % 2 else -1
        g.wolkje(b, x + dz * 6 + dx * kant * 13, y - rng.randint(9, 12), z + dx * kant * 13 + dz * 6, rng.uniform(3.0, 4.5), f"{NAAM}/wolk{i}")
        if i % 3 == 1:
            ex, ey, ez = x + 9 * dz + dx * kant * -11, y - 10, z + 9 * dx + dz * kant * -11
            if b.inside(ex - 3, ey - 3, ez - 3) and b.inside(ex + 3, ey + 2, ez + 3):
                g.bol(b, ex, ey, ez, 2.4, lambda *_c: ROTS if rng.random() < 0.7 else "guhs:kaaskorst", alleen_leeg=True, plat=0.7)
                top = max(yy for (xx, yy, zz) in b.s.blocks if xx == ex and zz == ez and ey - 3 <= yy <= ey + 3)
                b.set(ex, top + 1, ez, "guhs:lichtstaafje", {"facing": "up"})


def _bouw(h):
    b = kit.Bouw(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"], prefix=g.PREFIX)
    grond = eiland(b)
    plein(b, grond)
    starttoren(b)
    landingsbaan(b)
    scorebord(b)
    tribune(b)
    aankleding(b, grond)
    baan(b)
    b.connect()
    start = (24, TOP + 1, 48)
    hub = lambda c: c[0] < 80                                               # noqa: E731
    n, over = g.verlicht(b, start, [g.op_vloer("guhs:lichtstaafje", {"facing": "up"}), g.in_vloer("guhs:guh_kristal_lamp"),
                                    g.in_wand("guhs:guh_kristal_lamp")])
    # (what may float: the rings, every cloud, and the crumb islets with their lights out along the course)
    los = lambda c: b.s.blocks[c][0] in (RING, WOLK, DICHT) or not hub(c)   # noqa: E731
    b.laatste_licht = n
    return b, start, over, los


def maak(h):
    """-> (the build, its problems)."""
    b, start, over, los = _bouw(h)
    problemen = b.check(start, ["start", "startlijn", "racemeester", ("ring", 16), "finish", "finishlijn", "scorebord"], zwevend_ok=los)
    problemen += [f"{len(over)} donkere plekken kregen geen licht, bv. {over[:4]}"] if over else []
    problemen += baan_check(b)
    return b, problemen


def bouw(h):
    b, problemen = maak(h)
    if problemen:
        raise SystemExit(f"{NAAM}: self-check found {len(problemen)} problem(s):\n  " + "\n  ".join(problemen[:40]))
    print(f"{NAAM}: self-check ok {b.stats}, {b.laatste_licht} lights added, {len(BAAN)} rings")
    return b


def baan_check(b):
    """The course as a later phase will read it: the rings in order, each a closed hoop of ring blocks with a free middle,
    each next one in reach (15 to 40 blocks on) and never more than 14 blocks higher or lower, the first in front of the
    start line and the last in front of the finish."""
    p = []
    ringen = [b.plekken[f"ring_{i:02d}"] for i in range(1, len(BAAN) + 1)]
    for i, r in enumerate(ringen, 1):
        x, y, z = r["pos"]
        hoepel = vrij = 0
        for u in range(-VRIJ - 2, VRIJ + 3):
            for v in range(-VRIJ - 2, VRIJ + 3):
                c = (x, y + v, z + u) if r["as"] == "x" else (x + u, y + v, z)
                naam = b.get(*c)
                if naam == RING:
                    hoepel += 1
                elif math.hypot(u, v) <= r["straal"] and naam is not None:
                    vrij += 1
        if hoepel < 30 or vrij:
            p.append(f"ring_{i:02d}: {hoepel} ringblokken, {vrij} blokken in de vrije opening")
        if r["straal"] < 4:
            p.append(f"ring_{i:02d}: straal {r['straal']} is kleiner dan 4")
    punten = [b.plekken["startlijn"]["pos"]] + [r["pos"] for r in ringen] + [b.plekken["finishlijn"]["pos"]]
    for i, (a, c) in enumerate(zip(punten, punten[1:])):
        d = math.dist(a, c)
        if not 12 <= d <= 40 or abs(a[1] - c[1]) > 14:
            p.append(f"stuk {i} van de baan: {d:.0f} blokken, {c[1] - a[1]} omhoog")
    return p
