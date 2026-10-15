"""
einde2, slice bouw_zij2: the Leeuwschildpad-eiland (any of the six outer biomes). Build only: the wise, vadsige counsel
and the one blessing per player are a later phase, which reads the markers.

The island IS the creature: a lion-turtle guh floating in the void, looking south. Its shell is a dome of scutes, 68 wide
and 80 long, with earth and a forest of guhbloesem and oak on its back; under the rim hang the pale belly, a tail, two
hind flippers and the right front flipper. Its head is a guh's head with a lion's mane behind it and a white beard under
the chin: since 2026-10-15 a big moving MODEL (the entity guhs:leeuwschildpad_guh at the marker `kop`), no longer blocks.
Its LEFT front flipper is raised and curls around in front of its face: the palm, with its pink pads, is where you stand
to be spoken to; the claws round the palm are the model's too.

You land in a clearing on the back of the shell, walk through the forest over the top (the old stone ring), down a trail
along the flank to the left shoulder, and along the arm to the palm.

    maak(h) -> (the kit.Bouw, its problems); bouw(h) raises SystemExit on a problem
"""
import math
import random

import features.einde2_kit as kit
from features import einde2_bouw_zij2_gereedschap as g
from features import guheinde_bouw as gb

NAAM = "leeuwschildpad_eiland"
W, H, D = 112, 80, 152
Y = 160                                   # the world y of template y 0
CX, CZ = 56, 62                           # the middle of the shell
A, B_ = 40.0, 34.0                        # half its length (z) and half its width (x)
R0, KOEPEL, BUIK = 30, 22.0, 9.0          # the rim's height, the dome above it, the belly under it
BOS = 0.66                                # inside this part of the shell (0 middle .. 1 rim) earth lies on it
HUID = [("lime_terracotta", 62), ("green_terracotta", 30), ("moss_block", 8)]
HUID_ONDER = [("yellow_terracotta", 70), ("lime_terracotta", 30)]
PAD = [("mossy_cobblestone", 45), ("cobblestone", 20), ("mossy_stone_bricks", 25), ("coarse_dirt", 10)]
KOP_SCHAAL = 1.7
KOP_HOEK = (CX - 17, R0 - 8, 113)         # the low corner of the box of the guh's head at KOP_SCHAAL (looking south)
KOP_NEUS_GUH = (0, 6.2, -12.4)            # the nose in the guh model: the marker `kop`, the origin of the model of the head
PALM = (57, 141)                          # the middle of the palm (x, z); its top is at R0


def maat(x, z):
    return math.hypot((x - CX) / B_, (z - CZ) / A)


def schild_top(x, z):
    d = maat(x, z)
    return None if d > 1 else int(round(R0 + KOEPEL * (1 - d * d) ** 0.42))


def schild(b):
    """The shell: the dome (scutes, with earth on the middle part), the pale belly, hollow inside. Returns {(x, z): top y}."""
    rng = random.Random(kit.seed(NAAM + "/schild"))
    zaden = [(CX, z, "rug") for z in (28, 45, 62, 79, 96)] + [(CX + s * 19, z, "zij") for s in (-1, 1) for z in (37, 54, 70, 87)]
    for k in range(26):
        hoek = k / 26 * math.tau
        zaden.append((CX + math.cos(hoek) * B_ * 0.95, CZ + math.sin(hoek) * A * 0.95, "rand"))
    toppen = {}
    for x in range(int(CX - B_) - 1, int(CX + B_) + 2):
        for z in range(int(CZ - A) - 1, int(CZ + A) + 2):
            t = schild_top(x, z)
            if t is not None:
                toppen[(x, z)] = t
    for (x, z), t in toppen.items():
        d = maat(x, z)
        onder = int(round(R0 - 1 - BUIK * (1 - d * d) ** 0.5))
        buren = [toppen.get(n) for n in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))]
        laag = min([t] + [n for n in buren if n is not None]) - 1          # (down to under the lowest neighbour: no gaps in a steep flank)
        if None in buren:
            laag = onder
        # the colour of the scute here
        afst = sorted((math.hypot(x - sx, (z - sz)), soort) for sx, sz, soort in zaden)
        (d1, soort), (d2, _s) = afst[0], afst[1]
        if d2 - d1 < 1.7:
            schub = "brown_terracotta"
        elif soort == "rand":
            schub = "yellow_terracotta" if int(d1 / 2.5) % 2 == 0 else "green_terracotta"
        else:
            schub = "lime_terracotta" if int(d1 / 3.2) % 2 else "green_terracotta"
        for y in range(max(laag, onder), t + 1):
            if d < BOS:
                b.set(x, y, z, ("moss_block" if rng.random() < 0.9 else "rooted_dirt") if y == t else "dirt")
            else:
                b.set(x, y, z, schub if y == t else "green_terracotta")
        # the belly: two thick, pale, with the seams of the plastron
        naad = abs(x - CX) < 1 or (z - CZ) % 16 == 0
        for y in (onder, onder + 1):
            if y < max(laag, onder):
                b.set(x, y, z, "brown_terracotta" if naad and y == onder else gb.pick(rng, HUID_ONDER))
    return toppen


def klomp(b, punten, r, ry, blok, top=None):
    """A limb: flattened balls along a line of points (x, y, z). blok(x, y, z, boven) -> block name. top: nothing is
    written above this y (a flat back to walk on)."""
    cellen = set()
    for (ax, ay, az), (bx, by, bz) in zip(punten, punten[1:]):
        n = int(math.dist((ax, ay, az), (bx, by, bz)) * 1.5) + 1
        for k in range(n + 1):
            t = k / n
            cx, cy, cz = ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t
            for x in range(int(cx - r) - 1, int(cx + r) + 2):
                for z in range(int(cz - r) - 1, int(cz + r) + 2):
                    for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
                        if math.hypot(x - cx, z - cz) <= r and abs(y - cy) <= ry * math.sqrt(max(0.0, 1 - (math.hypot(x - cx, z - cz) / r) ** 2)) + 0.6:
                            if top is None or y <= top:
                                cellen.add((x, y, z))
    for (x, y, z) in sorted(cellen):
        if not b.inside(x, y, z):
            continue
        boven = (x, y + 1, z) not in cellen
        open_ = any(n not in cellen for n in ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1))) and (x, y - 1, z) in cellen
        b.set(x, y, z, blok(x, y, z, boven or open_))
    return cellen


def lijf(b):
    """Tail, hind flippers, the right front flipper, the neck."""
    rng = random.Random(kit.seed(NAAM + "/lijf"))
    huid = lambda x, y, z, boven: gb.pick(rng, HUID) if boven or y >= R0 - 2 else gb.pick(rng, HUID_ONDER)   # noqa: E731
    klomp(b, [(CX, R0 - 3, 26), (CX, R0 - 4, 14), (CX, R0 - 5, 6)], 4.0, 2.6, huid)                 # the tail
    klomp(b, [(CX, R0 - 5, 6), (CX, R0 - 5, 3)], 2.0, 1.6, huid)
    for s in (-1, 1):                                                                              # the hind flippers
        klomp(b, [(CX + s * 22, R0 - 3, 34), (CX + s * 34, R0 - 4, 22), (CX + s * 40, R0 - 5, 10)], 6.0, 2.4, huid)
    klomp(b, [(CX - 24, R0 - 3, 90), (CX - 38, R0 - 4, 100), (CX - 46, R0 - 5, 114)], 6.5, 2.4, huid)   # the right front flipper, at rest
    for kx, kz in ((CX - 50, 117), (CX - 46, 120), (CX - 42, 119)):
        b.set(kx, R0 - 5, kz, "bone_block", {"axis": "z"})
    for s in (-1, 1):
        for kx, kz in ((CX + s * 44, 6), (CX + s * 40, 4), (CX + s * 37, 6)):
            b.set(kx, R0 - 5, kz, "bone_block", {"axis": "z"})
    klomp(b, [(CX, R0 + 2, 96), (CX, R0 + 4, 106), (CX, R0 + 5, 115)], 10.0, 9.0, huid)             # the neck


def arm(b):
    """The left front flipper, raised: from the shoulder out and around to the palm in front of the face. Its back is
    flat (the way to the palm); the palm has pink pads; its four claws are the model's (they move)."""
    rng = random.Random(kit.seed(NAAM + "/arm"))
    huid = lambda x, y, z, boven: gb.pick(rng, HUID) if boven or y >= R0 - 1 else gb.pick(rng, HUID_ONDER)   # noqa: E731
    lijn = [(CX + 24, R0 - 2, 88), (CX + 37, R0 - 2, 102), (CX + 38, R0 - 2, 120), (CX + 26, R0 - 2, 135), (PALM[0] + 6, R0 - 2, PALM[1])]
    klomp(b, lijn, 5.0, 3.0, huid, top=R0)
    px, pz = PALM
    for x in range(px - 8, px + 9):
        for z in range(pz - 8, pz + 9):
            d = math.hypot(x - px, z - pz)
            if d <= 7.2:
                for y in range(R0 - 3, R0 + 1):
                    if y == R0:
                        kussen = d < 2.4 or any(math.hypot(x - (px + dx), z - (pz + dz)) < 1.5 for dx, dz in ((-4, -3), (0, -5), (4, -3), (-5, 2)))
                        b.set(x, y, z, "pink_terracotta" if kussen else gb.pick(rng, HUID))
                    elif d > 5.6 or y == R0 - 3:
                        b.set(x, y, z, gb.pick(rng, HUID_ONDER) if y == R0 - 3 else gb.pick(rng, HUID))
    # (the four claws on the far side are the model's: guhs:leeuwschildpad_guh curls them for the blessing)
    b.prop("zegen_plek", px, R0, pz, wie="het middelste kussentje van de handpalm: hier sta je voor de zegen")
    b.plek("leeuwschildpad", px - 2, R0 + 1, pz - 5, yaw=20.0, wie="de Leeuwschildpad-guh: de praatplek op de palm, vlak voor zijn snuit", kind="LEEUWSCHILDPAD_GUH")
    b.sign(px + 5, R0 + 1, pz - 2, "west", ["Stil staan.", "Hij denkt na.", "Dat duurt", "even. Njeg."], wall=False)
    for hx, hz in ((px + 5, pz + 3), (px - 5, pz + 2)):
        b.set(hx, R0 + 1, hz, "guhs:guh_kristal_lamp")
    return lijn


def kop(b):
    """The head is no longer of blocks (DESIGN 5, 2026-10-15): the entity guhs:leeuwschildpad_guh (look 0, a resident at the
    marker `kop`) IS the head, a big moving model with the guh's face, the mane and the beard, exactly where the guh's head
    voxelised at KOP_SCHAAL with its low corner at KOP_HOEK used to be (tools/features/einde2_cast_schurk_wezens.py does the
    same sums). Here only the marker: the nose, the model's origin. Returns naar(xm, ym, zm): a guh model point -> cell."""
    ox, oy, oz = KOP_HOEK
    _maat, naar = g.voxel_guh_in(b, ox, oy, oz, KOP_SCHAAL, botten=("head", "ear_left", "ear_right"), naar_zuid=True,
                                 kleur=lambda deel, pm, blok: None)          # (no block: the model stands there)
    nx_, ny_, nz_ = naar(*KOP_NEUS_GUH)
    b.prop("kop", nx_, ny_, nz_, wie="de snuit van de Leeuwschildpad-guh: hier staat het model van zijn kop (guhs:leeuwschildpad_guh)")
    return naar


def pad_en_bos(b, toppen):
    """The clearing where you land, the path over the top with the stone ring, the trail down the left flank, the forest."""
    rng = random.Random(kit.seed(NAAM + "/bos"))
    bezet = set()
    # the clearing: a level terrace of mossy bricks, 11 x 11
    ax, az, ay = CX, 43, schild_top(CX, 43)
    for x in range(ax - 6, ax + 7):
        for z in range(az - 6, az + 7):
            rand = abs(x - ax) == 6 or abs(z - az) == 6
            t = toppen.get((x, z), ay)
            for y in range(min(t, ay) - 1, max(t, ay) + 3):
                b.s.blocks.pop((x, y, z), None)
            for y in range(min(t, ay) - 2, ay + 1):
                b.set(x, y, z, "dirt" if y < ay else ("mossy_stone_bricks" if not rand else ("moss_block" if rng.random() < 0.5 else "mossy_cobblestone")))
            toppen[(x, z)] = ay
            bezet.add((x, z))
    for x, z in ((ax, az), (ax - 1, az), (ax + 1, az), (ax, az - 1), (ax, az + 1)):
        b.set(x, ay, z, "pink_concrete")
    b.zone("aankomst", ax - 5, ay + 1, az - 5, ax + 5, ay + 10, az + 5, wie="de open plek achter op het schild")
    b.prop("aankomst_speler", ax, ay + 1, az)
    b.prop("uitzicht", CX, R0 + 24, 188, wie="van voren, hoog: recht in zijn gezicht, het bos op zijn rug erachter")
    b.sign(ax + 4, ay + 1, az + 6, "south", ["Je staat op", "iemand. Loop", "zachtjes, hij", "is kietelig."], wall=False)
    # the path: from the clearing over the top to where the trail starts, at the front of the forest
    def stap(x, z):
        t = toppen.get((x, z))
        if t is not None:
            b.set(x, t, z, gb.pick(rng, PAD))
            bezet.add((x, z))
    for z in range(az + 6, 87):
        for dx in (-1, 0, 1):
            stap(CX + dx + int(round(math.sin(z / 6.0) * 2)), z)
    # the stone ring on the top of the shell
    tx, tz = CX, CZ
    ty = toppen[(tx, tz)]
    for k in range(8):
        hoek = k / 8 * math.tau + 0.39
        x, z = int(round(tx + math.cos(hoek) * 5)), int(round(tz + math.sin(hoek) * 5))
        t = toppen[(x, z)]
        for y in range(t + 1, t + (4 if k % 2 else 3)):
            b.set(x, y, z, "mossy_cobblestone" if y < t + 2 else "cobblestone")
        if k % 2:
            b.set(x, t + 4, z, "lantern", gb.lamp(False))
        bezet.add((x, z))
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if math.hypot(dx, dz) <= 3.3:
                b.set(tx + dx, toppen[(tx + dx, tz + dz)], tz + dz, "mossy_stone_bricks" if (dx + dz) % 2 else "chiseled_stone_bricks")
                bezet.add((tx + dx, tz + dz))
    b.set(tx, ty, tz, "guhs:guh_kristal_lamp")
    b.prop("schild_top", tx, ty, tz, wie="het hoogste punt van het schild, midden in de steenkring")
    b.sign(tx + 2, ty + 1, tz - 3, "south", ["Hier woont", "niemand. Hier", "IS iemand.", ""], wall=False)
    b.zone("bos", int(CX - B_ * BOS), R0 + 12, int(CZ - A * BOS), int(CX + B_ * BOS), R0 + 36, int(CZ + A * BOS), wie="het bos op het schild")
    # the trail: from the front of the forest down along the left flank to the shoulder, three wide, a wall on its inside
    spoor = []
    d0, d1, h0, h1 = BOS - 0.02, 1.0, math.radians(90), math.radians(38)
    y0 = schild_top(CX, int(CZ + A * d0))
    n = 150
    for k in range(n + 1):
        t = k / n
        d, hoek = d0 + (d1 - d0) * t ** 0.85, h0 + (h1 - h0) * t
        y = int(round(y0 + (R0 - y0) * t))
        for o in (0, 1, 2, 3):
            dd = d + o / 37.0
            x, z = int(round(CX + math.cos(hoek) * B_ * dd)), int(round(CZ + math.sin(hoek) * A * dd))
            spoor.append((x, y, z))
    gezet = {}
    for (x, y, z) in spoor:
        if (x, z) in gezet and gezet[(x, z)] <= y:
            continue
        gezet[(x, z)] = y
    for (x, z), y in sorted(gezet.items()):
        t = toppen.get((x, z))
        for yy in range(y + 1, max(y + 5, (t or y) + 1)):
            b.s.blocks.pop((x, yy, z), None)
        onder = min(y - 1, (t if t is not None else y) - 1)
        for yy in range(onder, y + 1):
            b.set(x, yy, z, gb.pick(rng, PAD) if yy == y else "mossy_cobblestone")
        toppen[(x, z)] = y
        bezet.add((x, z))
    # close what the cut opened: wherever a trail cell has a neighbour column whose top is higher, that column is solid down to the trail
    for (x, z), y in sorted(gezet.items()):
        for nx, nz in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            t = toppen.get((nx, nz))
            if (nx, nz) not in gezet and t is not None and t > y:
                for yy in range(y, t):
                    if b.get(nx, yy, nz) is None:
                        b.set(nx, yy, nz, "green_terracotta" if maat(nx, nz) >= BOS else "dirt")
            elif (nx, nz) not in gezet and t is None:
                pass
    for k in range(10, n, 22):                                 # lanterns on posts along the outside of the trail
        t = k / n
        d, hoek = d0 + (d1 - d0) * t ** 0.85 + 3.4 / 37.0, h0 + (h1 - h0) * t
        x, z = int(round(CX + math.cos(hoek) * B_ * d)), int(round(CZ + math.sin(hoek) * A * d))
        if (x, z) in gezet:
            y = gezet[(x, z)]
            b.set(x, y + 1, z, "mossy_cobblestone_wall")
            b.set(x, y + 2, z, "lantern", gb.lamp(False))
    # the forest
    bomen = 0
    for _ in range(1500):
        if bomen >= 30:
            break
        hoek, d = rng.uniform(0, math.tau), math.sqrt(rng.uniform(0.01, (BOS - 0.07) ** 2))
        x, z = int(CX + math.cos(hoek) * B_ * d), int(CZ + math.sin(hoek) * A * d)
        if any((x + dx, z + dz) in bezet for dx in range(-2, 3) for dz in range(-2, 3)):
            continue
        boom(b, x, toppen[(x, z)] + 1, z, rng, roze=bomen % 3 != 2)
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                bezet.add((x + dx, z + dz))
        bomen += 1
    for (x, z), t in sorted(toppen.items()):
        if maat(x, z) < BOS and (x, z) not in bezet and b.get(x, t + 1, z) is None and b.get(x, t, z) in ("minecraft:moss_block", "minecraft:grass_block"):
            kans = rng.random()
            if kans < 0.16:
                b.set(x, t + 1, z, "short_grass")
            elif kans < 0.2:
                b.set(x, t + 1, z, "fern")
            elif kans < 0.225:
                b.set(x, t + 1, z, rng.choice(["pink_tulip", "dandelion", "poppy"]))
            elif kans < 0.25:
                b.set(x, t + 1, z, "moss_carpet")
    return bomen


def boom(b, x, y, z, rng, roze=True):
    """A tree: guhbloesem (pink) or oak, a round crown, a lantern hanging under it."""
    hoog = rng.randint(4, 6)
    stam, loof = ("guhs:guhbloesem_log", "guhs:guhbloesem_leaves") if roze else ("oak_log", "oak_leaves")
    for k in range(hoog):
        b.set(x, y + k, z, stam, {"axis": "y"})
    top = y + hoog
    kroon = set()
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for dy in range(-2, 3):
                r = math.hypot(dx, dz, dy * 1.3)
                if r <= 2.9 and (r < 2.2 or rng.random() < 0.6) and b.get(x + dx, top + dy, z + dz) is None:
                    kroon.add((x + dx, top + dy, z + dz))
    # (only the leaves that hang together with the trunk: no loose leaf in the air)
    vast, todo = set(), [(x, top, z)] if (x, top, z) in kroon else [c for c in kroon if c[0] == x and c[2] == z][:1]
    vast.update(todo)
    while todo:
        cx, cy, cz = todo.pop()
        for n in ((cx + 1, cy, cz), (cx - 1, cy, cz), (cx, cy + 1, cz), (cx, cy - 1, cz), (cx, cy, cz + 1), (cx, cy, cz - 1)):
            if n in kroon and n not in vast:
                vast.add(n)
                todo.append(n)
    for c in sorted(vast):
        g.blad(b, *c, loof)
    dx, dz = rng.choice([(1, 0), (-1, 0), (0, 1), (0, -1)])
    if b.get(x + dx, top - 2, z + dz) is not None and b.get(x + dx, top - 3, z + dz) is None:
        b.set(x + dx, top - 3, z + dz, "lantern", gb.lamp(True))


def _bouw(h):
    b = kit.Bouw(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"], prefix=g.PREFIX)
    toppen = schild(b)
    lijf(b)
    kop(b)
    arm(b)
    b.bomen = pad_en_bos(b, toppen)
    b.connect()
    start = (CX, schild_top(CX, 43) + 1, 43)
    n, over = g.verlicht(b, start, [g.in_vloer("shroomlight"), g.op_vloer("lantern", gb.lamp(False)), g.in_wand("shroomlight")])
    b.laatste_licht = n
    return b, start, over


VERWACHT = ["kop", "leeuwschildpad", "zegen_plek", "bos", "schild_top"]


def eigen_check(b):
    """It must read as a creature: the head (the model) has room where it stands, the forest is on its back."""
    p = []
    nx, ny, nz = b.plekken["kop"]["pos"]
    in_de_kop = [c for c in b.s.blocks if nx - 12 <= c[0] <= nx + 12 and ny + 1 <= c[1] <= ny + 14 and nz - 8 <= c[2] <= nz]
    if in_de_kop:
        p.append(f"{len(in_de_kop)} blokken staan waar het model van de kop staat, bv. {sorted(in_de_kop)[:4]}")
    los = [c for c, bl in b.s.blocks.items() if "leaves" in bl[0] and bl[1].get("persistent") != "true"]
    if los:
        p.append(f"{len(los)} bladeren zijn niet persistent (ze vallen af, de tegels krijgen geen buur-updates), bv. {los[:3]}")
    if b.bomen < 18:
        p.append(f"maar {b.bomen} bomen op het schild: dat is geen bos")
    return p


def maak(h):
    b, start, over = _bouw(h)
    problemen = b.check(start, VERWACHT)
    problemen += [f"{len(over)} donkere plekken kregen geen licht, bv. {over[:4]}"] if over else []
    problemen += eigen_check(b)
    return b, problemen


def bouw(h):
    b, problemen = maak(h)
    if problemen:
        raise SystemExit(f"{NAAM}: self-check found {len(problemen)} problem(s):\n  " + "\n  ".join(problemen[:40]))
    print(f"{NAAM}: self-check ok {b.stats}, {b.laatste_licht} lights added, {b.bomen} trees")
    return b
