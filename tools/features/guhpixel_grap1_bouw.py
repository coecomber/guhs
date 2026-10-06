"""
The three arenas of the guhpixel slice "grap1" (templates data/guhs/structure/guhpixel/<name>.nbt) and their game test rooms.
Java mirrors of the coordinates: SkyblokSessie, BedwarsSessie, VadsniteSessie (feature/guhpixel/grap1); the game tests
PxGrap1GameTests check the stamped templates against those constants, controleer() below checks them here.

Rules for arena templates (CONTRACT_PX 3.6): at most 160^3, no free-flowing water or lava, only what the game needs;
unset positions stay void. Everything is deterministic (no random numbers).
"""
import math

SKYBLOK, BEDWARS, VADSNITE = "guhpixel/skyblok_eiland", "guhpixel/bedwars_eilanden", "guhpixel/vadsnite_eiland"
TEST_KAMERS = {"skyblok_test_kamer": (31, 30, 31), "bedwars_test_kamer": (57, 26, 57), "vadsnite_test_kamer": (53, 54, 53)}

# ---------------------------------------------------------------------------------------------------------------------
# Skyblok: "the" island in a painted box
# ---------------------------------------------------------------------------------------------------------------------
SKY_MAAT = (27, 24, 27)
SKY_BED_VOET, SKY_BED_HOOFD, SKY_KIST, SKY_DEUR = (12, 11, 10), (13, 11, 10), (15, 11, 11), (21, 16, 1)
SKY_START = (11, 11, 12)
SKY_EILAND_Y = 10
LUCHT, WOLK, NAAD = "minecraft:light_blue_terracotta", "minecraft:white_concrete", "minecraft:cyan_terracotta"   # (the clouds: fresh white paint)
WOLKJE = ["..###...", ".#####..", "########", ".######."]
WOLKJE_KLEIN = [".##..", "#####", ".###."]


def _wolk(vlak, u0, v0, vorm):
    """Paints a cloud in a face's own (u, v) cells; v grows upwards."""
    for dv, rij in enumerate(reversed(vorm)):
        for du, ch in enumerate(rij):
            if ch == "#":
                vlak[(u0 + du, v0 + dv)] = WOLK


def skyblok(h):
    W, H, _ = SKY_MAAT
    s = h.Structure(SKY_MAAT)
    # the painted faces: every face is (u, v) cells, clouds first, then the seams of the panels on top
    muren = {n: {} for n in ("noord", "zuid", "west", "oost", "plafond")}
    _wolk(muren["noord"], 3, 14, WOLKJE)
    _wolk(muren["noord"], 14, 9, WOLKJE_KLEIN)
    _wolk(muren["zuid"], 15, 15, WOLKJE)
    _wolk(muren["zuid"], 4, 10, WOLKJE_KLEIN)
    _wolk(muren["zuid"], 11, 4, WOLKJE_KLEIN)
    _wolk(muren["west"], 5, 16, WOLKJE)
    _wolk(muren["west"], 16, 11, WOLKJE_KLEIN)
    _wolk(muren["oost"], 12, 13, WOLKJE)
    _wolk(muren["oost"], 3, 6, WOLKJE_KLEIN)
    _wolk(muren["plafond"], 4, 5, WOLKJE_KLEIN)
    _wolk(muren["plafond"], 16, 17, WOLKJE)

    def verf(vlak, u, v):
        if u % 9 == 0 or v % 8 == 0:
            return NAAD
        return muren[vlak].get((u, v), LUCHT)
    for a in range(W):
        for y in range(H):
            s.set(a, y, 0, verf("noord", a, y))
            s.set(a, y, W - 1, verf("zuid", a, y))
            s.set(0, y, a, verf("west", a, y))
            s.set(W - 1, y, a, verf("oost", a, y))
        for b in range(W):
            s.set(a, H - 1, b, verf("plafond", a, b))
            s.set(a, 0, b, NAAD if a % 9 == 0 or b % 9 == 0 else LUCHT)        # the "void": a painted floor
    # the sun: a lamp in the ceiling
    for x in range(12, 15):
        for z in range(12, 15):
            s.set(x, H - 1, z, "minecraft:glowstone")
    # the island: an L of 6 x 6 minus a quarter, three layers thick
    for x in range(10, 16):
        for z in range(10, 16):
            if x >= 13 and z >= 13:
                continue
            s.set(x, SKY_EILAND_Y, z, "minecraft:grass_block", {"snowy": "false"})
            s.set(x, SKY_EILAND_Y - 1, z, "minecraft:dirt")
            s.set(x, SKY_EILAND_Y - 2, z, "minecraft:dirt")
    s.set(12, SKY_EILAND_Y - 2, 12, "minecraft:bedrock")
    # the tree
    tx, tz = 11, 14
    blad = {"persistent": "true", "distance": "1", "waterlogged": "false"}
    for y in range(13, 15):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) == 2 and abs(dz) == 2:
                    continue
                s.set(tx + dx, y, tz + dz, "minecraft:oak_leaves", blad)
    for y in range(15, 17):
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                if y == 16 and dx and dz:
                    continue
                s.set(tx + dx, y, tz + dz, "minecraft:oak_leaves", blad)
    for y in range(11, 15):
        s.set(tx, y, tz, "minecraft:oak_log", {"axis": "y"})
    # the start chest (the game fills it) and the bed
    s.set(*SKY_KIST, "minecraft:chest", {"facing": "west", "type": "single", "waterlogged": "false"})
    s.set(*SKY_BED_VOET, "minecraft:red_bed", {"facing": "east", "part": "foot", "occupied": "false"})
    s.set(*SKY_BED_HOOFD, "minecraft:red_bed", {"facing": "east", "part": "head", "occupied": "false"})
    # the ladder up the sky, the little ledge and the staff door (against the wall, the sky stays whole behind it)
    for y in range(1, 17):
        s.set(20, y, 1, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    for (x, z) in ((20, 2), (21, 1), (21, 2), (22, 1), (22, 2)):
        s.set(x, 15, z, "minecraft:light_blue_concrete")
    dx, dy, dz = SKY_DEUR
    for half, y in (("lower", dy), ("upper", dy + 1)):
        s.set(dx, y, dz, "minecraft:iron_door", {"facing": "south", "half": half, "hinge": "left", "open": "false", "powered": "false"})
    # the painters left their things on the "void"
    s.set(5, 1, 20, "minecraft:cauldron")
    s.set(6, 1, 20, "minecraft:white_carpet")
    s.set(6, 1, 21, "minecraft:white_carpet")
    s.set(4, 1, 21, "minecraft:light_blue_carpet")
    # light everywhere (a closed box has no daylight): invisible light blocks in every free cell, so the painted sky, the
    # clouds and the island are as bright as a real day (a grid of them left the box dim and yellowish in the client).
    # Around the island only every other cell: the open corner of the L and the room above the grass stay real air.
    for x in range(1, W - 1):
        for y in range(1, H - 1):
            for z in range(1, W - 1):
                if (x, y, z) in s.blocks:
                    continue
                if 9 <= x <= 16 and 9 <= z <= 16 and 8 <= y <= 18 and (x + y + z) % 2 == 0:
                    continue
                s.set(x, y, z, "minecraft:light", {"level": "15", "waterlogged": "false"})
    s.save(SKYBLOK)
    return s


# ---------------------------------------------------------------------------------------------------------------------
# Bedwars: your island with the bed, four tiny team islands around it
# ---------------------------------------------------------------------------------------------------------------------
BED_MAAT = (53, 22, 53)
BED_Y, BED_MIDDEN, BRUG_LENGTE = 8, 26, 13
BED_VOET, BED_HOOFD = (BED_MIDDEN, BED_Y + 1, BED_MIDDEN), (BED_MIDDEN, BED_Y + 1, BED_MIDDEN - 1)
TEAMS = (("rood", 0, -1, "red"), ("blauw", 1, 0, "blue"), ("groen", 0, 1, "lime"), ("geel", -1, 0, "yellow"))


def brug(dx, dz):
    """The 13 cells a team bridges over (the game places and removes them): from its island to yours."""
    return [(BED_MIDDEN + dx * (17 - i), BED_Y, BED_MIDDEN + dz * (17 - i)) for i in range(BRUG_LENGTE)]


def bedwars(h):
    s = h.Structure(BED_MAAT)
    m, y = BED_MIDDEN, BED_Y
    # your island: 9 x 9, pink and white, on end stone (as it should be)
    for x in range(m - 4, m + 5):
        for z in range(m - 4, m + 5):
            rand = abs(x - m) == 4 or abs(z - m) == 4
            s.set(x, y, z, "minecraft:white_wool" if rand else "minecraft:pink_wool")
    for laag, r in ((1, 3), (2, 2), (3, 1)):
        for x in range(m - r, m + r + 1):
            for z in range(m - r, m + r + 1):
                s.set(x, y - laag, z, "minecraft:end_stone")
    s.set(*BED_VOET, "minecraft:pink_bed", {"facing": "north", "part": "foot", "occupied": "false"})
    s.set(*BED_HOOFD, "minecraft:pink_bed", {"facing": "north", "part": "head", "occupied": "false"})
    # the "kussen-generator": a heap of pillows in a corner, and the team chest in another (decoration)
    for (x, yy, z) in ((m + 3, y + 1, m + 3), (m + 3, y + 2, m + 3), (m + 2, y + 1, m + 3), (m + 3, y + 1, m + 2)):
        s.set(x, yy, z, "minecraft:white_wool")
    s.set(m + 2, y + 2, m + 3, "minecraft:white_carpet")
    s.set(m - 3, y + 1, m + 3, "minecraft:ender_chest", {"facing": "north", "waterlogged": "false"})
    s.set(m - 3, y + 1, m - 3, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    s.set(m + 3, y + 1, m - 3, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # the four team islands: 5 x 5 of team wool, a flag on a corner
    for _, dx, dz, kleur in TEAMS:
        cx, cz = m + dx * 20, m + dz * 20
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                s.set(x, y, z, f"minecraft:{kleur}_wool")
        for x in range(cx - 1, cx + 2):
            for z in range(cz - 1, cz + 2):
                s.set(x, y - 1, z, "minecraft:end_stone")
        s.set(cx, y - 2, cz, "minecraft:end_stone")
        # the flag stands on the far side, out of the guh's way
        fx, fz = cx + (2 if dx >= 0 else -2), cz + (2 if dz >= 0 else -2)
        for yy in range(y + 1, y + 4):
            s.set(fx, yy, fz, "minecraft:oak_fence", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
        s.set(fx, y + 4, fz, f"minecraft:{kleur}_wool")
        s.set(fx, y + 5, fz, f"minecraft:{kleur}_carpet")
    s.save(BEDWARS)
    return s


# ---------------------------------------------------------------------------------------------------------------------
# Vadsnite: the island, and the Vadsbus hanging above it
# ---------------------------------------------------------------------------------------------------------------------
VADS_MAAT = (49, 48, 49)
VADS_Y, BUS_Y, VADS_MIDDEN = 10, 34, 24
LUIK = (23, 25, 20, 21)                     # x0, x1, z0, z1
BEDDEN = ((17, 11, 25, "pink"), (31, 11, 25, "magenta"), (24, 11, 33, "white"), (24, 11, 16, "yellow"))   # feet; heads one north
BUS_X, BUS_Z = (22, 26), (19, 29)
KNUFFELS = ("normal", "mint", "choco", "snow", "golden", "rainbow", "starry", "wolk", "pinguh", "pluisguh")
BOMEN = ((14, 17), (34, 19), (31, 34), (15, 32))


def vadsnite(h):
    s = h.Structure(VADS_MAAT)
    m, y = VADS_MIDDEN, VADS_Y
    # the island: soft moss with a sandy rim, a rounded underside
    for x in range(m - 15, m + 16):
        for z in range(m - 15, m + 16):
            d = math.hypot(x - m, z - m)
            if d <= 14.3:
                s.set(x, y, z, "minecraft:moss_block" if d <= 12.4 else "minecraft:sand")
            if d <= 13.2:
                s.set(x, y - 1, z, "minecraft:dirt" if d <= 11.5 else "minecraft:sandstone")
            for laag, r in ((2, 11.2), (3, 8.4), (4, 5.4), (5, 2.4)):
                if d <= r:
                    s.set(x, y - laag, z, "minecraft:stone")
    # a few little trees (the guhs land between them)
    blad = {"persistent": "true", "distance": "1", "waterlogged": "false"}
    for (tx, tz) in BOMEN:
        for yy in (y + 3, y + 4):
            for dx in range(-1, 2):
                for dz in range(-1, 2):
                    if yy == y + 4 and dx and dz:
                        continue
                    s.set(tx + dx, yy, tz + dz, "minecraft:oak_leaves", blad)
        for yy in range(y + 1, y + 4):
            s.set(tx, yy, tz, "minecraft:oak_log", {"axis": "y"})
    # the four sleeping spots, each with a lantern at its head end
    for (bx, by, bz, kleur) in BEDDEN:
        s.set(bx, by, bz, f"minecraft:{kleur}_bed", {"facing": "north", "part": "foot", "occupied": "false"})
        s.set(bx, by, bz - 1, f"minecraft:{kleur}_bed", {"facing": "north", "part": "head", "occupied": "false"})
        s.set(bx + 1, by, bz - 1, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    for (fx, fz, bloem) in ((20, 28, "pink_tulip"), (28, 20, "pink_tulip"), (27, 29, "oxeye_daisy"), (19, 19, "allium"), (29, 25, "dandelion")):
        s.set(fx, y + 1, fz, f"minecraft:{bloem}")
    s.set(26, y + 1, 30, "minecraft:barrel", {"facing": "up", "open": "false"})          # "buit" (decoration, stays shut)

    # the Vadsbus: a blue bus under a pink balloon, the hatch in the floor at the back, guhs asleep on the seats
    (x0, x1), (z0, z1) = BUS_X, BUS_Z
    BLAUW, GLAS = "minecraft:blue_concrete", "minecraft:light_blue_stained_glass"
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            in_luik = LUIK[0] <= x <= LUIK[1] and LUIK[2] <= z <= LUIK[3]
            if not in_luik:
                streep = z == LUIK[3] + 1 and x0 < x < x1
                s.set(x, BUS_Y, z, ("minecraft:yellow_concrete" if (x + z) % 2 else "minecraft:black_concrete") if streep else BLAUW)
            s.set(x, BUS_Y + 4, z, "minecraft:white_concrete" if x0 < x < x1 and z0 < z < z1 else BLAUW)
            rand = x in (x0, x1) or z in (z0, z1)
            if rand:
                for yy in range(BUS_Y + 1, BUS_Y + 4):
                    raam = yy == BUS_Y + 2 and ((x in (x0, x1) and z0 < z < z1 and z % 2 == 0) or (z == z1 and x0 < x < x1))
                    s.set(x, yy, z, GLAS if raam else BLAUW)
    s.set(m, BUS_Y + 4, m, "minecraft:sea_lantern")
    s.set(m, BUS_Y + 4, m + 3, "minecraft:sea_lantern")
    for i, z in enumerate(range(23, 28)):
        s.set(x0 + 1, BUS_Y + 1, z, f"guhs:knuffel_{KNUFFELS[(2 * i) % len(KNUFFELS)]}", {"facing": "east"})
        s.set(x1 - 1, BUS_Y + 1, z, f"guhs:knuffel_{KNUFFELS[(2 * i + 1) % len(KNUFFELS)]}", {"facing": "west"})
    s.set(m, BUS_Y + 1, z1 - 1, "guhs:knuffel_normal", {"facing": "south"})             # the driver (asleep at the wheel)
    # the balloon and its ropes
    s.sphere(m, BUS_Y + 9, m, 3.4, "minecraft:pink_wool", shell=1, inside=None)
    for (x, z) in ((x0, z0 + 2), (x1, z0 + 2), (x0, z1 - 2), (x1, z1 - 2)):
        for yy in range(BUS_Y + 5, BUS_Y + 7):
            s.set(x, yy, z, "minecraft:oak_fence", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"})
    s.save(VADSNITE)
    return s


# ---------------------------------------------------------------------------------------------------------------------
def controleer(sky, bed, vads):
    """The geometry the games rely on (the Java constants are checked against the same numbers by the game tests)."""
    problems = []

    def is_(s, pos, naam, wat):
        if s.get(*pos) != naam:
            problems.append(f"{wat}: {s.get(*pos)} at {pos}, expected {naam}")

    def vrij(s, pos, wat):
        if s.get(*pos) not in (None, "minecraft:air", "minecraft:light"):
            problems.append(f"{wat}: {s.get(*pos)} in the way at {pos}")
    for s, maat, naam in ((sky, SKY_MAAT, "skyblok"), (bed, BED_MAAT, "bedwars"), (vads, VADS_MAAT, "vadsnite")):
        if tuple(s.size) != maat or max(maat) > 160:
            problems.append(f"{naam}: size {s.size}")
        for (x, y, z), (b, props, _) in s.blocks.items():
            if b in ("minecraft:water", "minecraft:lava") or props.get("waterlogged") == "true":
                problems.append(f"{naam}: fluid {b} at {(x, y, z)}")
    # Skyblok
    is_(sky, SKY_BED_VOET, "minecraft:red_bed", "skyblok bed")
    is_(sky, SKY_BED_HOOFD, "minecraft:red_bed", "skyblok bed")
    is_(sky, SKY_KIST, "minecraft:chest", "skyblok chest")
    is_(sky, SKY_DEUR, "minecraft:iron_door", "skyblok door")
    is_(sky, (SKY_START[0], SKY_EILAND_Y, SKY_START[2]), "minecraft:grass_block", "skyblok start floor")
    vrij(sky, SKY_START, "skyblok start")
    vrij(sky, (SKY_START[0], SKY_START[1] + 1, SKY_START[2]), "skyblok start")
    for x in range(SKY_MAAT[0]):
        for y in range(SKY_MAAT[1]):
            for z in range(SKY_MAAT[2]):
                rand = x in (0, SKY_MAAT[0] - 1) or y in (0, SKY_MAAT[1] - 1) or z in (0, SKY_MAAT[2] - 1)
                if rand and sky.get(x, y, z) is None:
                    problems.append(f"skyblok: a hole in the sky at {(x, y, z)}")
    # Bedwars
    is_(bed, BED_VOET, "minecraft:pink_bed", "bedwars bed")
    is_(bed, BED_HOOFD, "minecraft:pink_bed", "bedwars bed")
    vrij(bed, (26, BED_Y + 1, 28), "bedwars start")
    for naam, dx, dz, kleur in TEAMS:
        is_(bed, (BED_MIDDEN + dx * 20, BED_Y, BED_MIDDEN + dz * 20), f"minecraft:{kleur}_wool", f"bedwars island {naam}")
        vrij(bed, (BED_MIDDEN + dx * 20, BED_Y + 1, BED_MIDDEN + dz * 20), f"bedwars island {naam}")
        cellen = brug(dx, dz)
        for c in cellen:
            if bed.get(*c) is not None:
                problems.append(f"bedwars bridge {naam}: {bed.get(*c)} already at {c}")
            vrij(bed, (c[0], c[1] + 1, c[2]), f"bedwars bridge {naam}")
        # the bridge joins both islands
        voor, na = (BED_MIDDEN + dx * 18, BED_Y, BED_MIDDEN + dz * 18), (BED_MIDDEN + dx * 4, BED_Y, BED_MIDDEN + dz * 4)
        if bed.get(*voor) is None or bed.get(*na) is None:
            problems.append(f"bedwars bridge {naam}: does not join the islands")
    # Vadsnite
    for (bx, by, bz, kleur) in BEDDEN:
        is_(vads, (bx, by, bz), f"minecraft:{kleur}_bed", "vadsnite bed")
        is_(vads, (bx, by, bz - 1), f"minecraft:{kleur}_bed", "vadsnite bed")
        if vads.get(bx, by - 1, bz) is None:
            problems.append(f"vadsnite bed at {(bx, by, bz)} floats")
    for x in range(LUIK[0], LUIK[1] + 1):
        for z in range(LUIK[2], LUIK[3] + 1):
            for yy in range(VADS_Y + 1, BUS_Y + 3):
                vrij(vads, (x, yy, z), "vadsnite hatch shaft")
            if vads.get(x, VADS_Y, z) != "minecraft:moss_block":
                problems.append(f"vadsnite: no moss under the hatch at {(x, z)}")
    is_(vads, (24, BUS_Y, 26), "minecraft:blue_concrete", "vadsnite bus floor under the start")
    vrij(vads, (24, BUS_Y + 1, 26), "vadsnite start")
    vrij(vads, (24, BUS_Y + 2, 26), "vadsnite start")
    for z in range(LUIK[3] + 1, 27):
        vrij(vads, (24, BUS_Y + 1, z), "vadsnite aisle")
    if problems:
        raise SystemExit("guhpixel grap1 arena check failed:\n  " + "\n  ".join(problems[:40]))


def build(h, lib):
    sky, bed, vads = skyblok(h), bedwars(h), vadsnite(h)
    controleer(sky, bed, vads)
    for naam, maat in TEST_KAMERS.items():
        lib.test_kamer(h, naam, maat)
    return sky, bed, vads
