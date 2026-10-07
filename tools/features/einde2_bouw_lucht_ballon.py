"""
einde2, slice bouw_lucht: the Vredesballon on its building site, in five stages (DESIGN 6: doek, mand, brander, knabbelvoorraad,
versiering). Each stage is an overlay template zuidelijke_luchttempel/laag_vredesballon_<n> of 25 x 42 x 25 on the marker
vredesballon_anker, and each holds everything of the stage before it (cumulative: a later phase shows ONE of them, or puts
them down one after the other; every cell of stage n is also a cell of stage n + 1, so a newer stage always covers the older).

Blocks only, and only blocks that draw themselves from their block state (no block entity, no entity, nothing that needs a
neighbour to stay): a later phase shows a stage to one player at a time (build contract 6.6).

  1 doek         four masts with a crane frame, and the cloth hanging from it: the whole envelope, still plain
  2 mand         the basket on its trestles under the mouth, ropes up to the cloth
  3 brander      the burner (a machine with a snoet) in the basket, its flame in the mouth of the balloon
  4 knabbelvoorraad   blocks of kaasknabbels in and around the basket, ballast bags on its sides, a chute
  5 versiering   the cloth in orange and yellow gores with a guh's face and two ears, a ring of flags, pennants on the masts
"""
import math

SX, SY, SZ = 25, 42, 25
MID = 12                       # the middle (x and z)
MAND_Y = 2                     # the basket's floor (it stands on trestles)
MOND_Y = 14                    # the mouth of the envelope
BOL_Y, BOL_R = 26, 9.2         # the middle and the radius of the round part
STADIA = ["doek", "mand", "brander", "knabbelvoorraad", "versiering"]
KNABBELS = "guhs:block_of_kaasknabbels"


def straal(y):
    """The radius of the envelope at height y: a cone from the mouth up to the ball, then the ball."""
    if y < MOND_Y or y > BOL_Y + BOL_R:
        return -1.0
    if y <= BOL_Y:
        return 2.6 + (BOL_R - 2.6) * math.sin((y - MOND_Y) / float(BOL_Y - MOND_Y) * math.pi / 2)
    return BOL_R * math.sqrt(max(0.0, 1 - ((y - BOL_Y) / BOL_R) ** 2))


def doekcellen():
    """The cells of the cloth: the skin of the shape of straal() (every cell of it that has a neighbour outside, corners
    counted too, so the skin is one piece); the mouth at the bottom is open."""
    def erin(x, y, z):
        d = math.hypot(x - MID, z - MID)
        if y < MOND_Y:
            return y == MOND_Y - 1 and d <= 1.9      # (the throat: the mouth stays open)
        return d <= straal(y) + 0.3

    cellen = []
    for y in range(MOND_Y, int(BOL_Y + BOL_R) + 1):
        for x in range(SX):
            for z in range(SZ):
                if erin(x, y, z) and not all(erin(x + dx, y + dy, z + dz) for dx in (-1, 0, 1) for dy in (-1, 0, 1) for dz in (-1, 0, 1)):
                    cellen.append((x, y, z))
    return cellen


def kleur_kaal(x, y, z):
    """Stage 1-4: plain cloth with a few patches (it was sewn from what the monks had lying about)."""
    lap = (x * 7 + y * 13 + z * 5) % 23
    return "minecraft:yellow_wool" if lap == 0 else "minecraft:orange_wool" if lap == 7 else "minecraft:light_gray_wool" if lap in (3, 15) else "minecraft:white_wool"


def kleur_versierd(x, y, z):
    """Stage 5: orange and yellow gores, a band of flags around the middle, and on the south side a guh's face."""
    dx, dy = x - MID, y - BOL_Y
    if z > MID + 4:                                    # the face looks south
        if abs(abs(dx) - 3) <= 0.5 and 2 <= dy <= 3:
            return "minecraft:black_wool"              # the eyes
        if abs(dx) <= 0.5 and 0 <= dy <= 1 or (abs(dx) <= 1 and dy == 0):
            return "minecraft:pink_wool"               # the nose
        if abs(abs(dx) - 5) <= 0.5 and dy == 0:
            return "minecraft:magenta_wool"            # the blush
    if dy == -3:
        return ("minecraft:light_blue_wool", "minecraft:white_wool", "minecraft:lime_wool", "minecraft:white_wool")[(x + z) % 4]
    hoek = int((math.degrees(math.atan2(z - MID, x - MID)) + 360 + 22.5) // 45) % 8
    return "minecraft:orange_wool" if hoek % 2 else "minecraft:yellow_wool"


def stadium(h, n):
    """Stage n (1..5) as a Structure."""
    s = h.Structure((SX, SY, SZ))
    lucht = "minecraft:air"

    def log(x, y, z, as_="y"):
        s.set(x, y, z, "minecraft:stripped_spruce_log", {"axis": as_})

    # --- 1: the masts, the crane frame, the cloth -----------------------------------------------------------------------
    for mx in (1, SX - 2):
        for mz in (1, SZ - 2):
            for y in range(0, SY - 2):
                log(mx, y, mz)
            for y in (10, 20, 30):                     # rungs to climb the mast by
                s.set(mx + (1 if mx == 1 else -1), y, mz, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
            s.set(mx, SY - 2, mz, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    top = SY - 3
    for a in range(1, SX - 1):
        for mz in (1, SZ - 2, MID):
            log(a, top, mz, "x")
        for mx in (1, SX - 2, MID):
            log(mx, top, a, "z")
    for y in range(int(BOL_Y + BOL_R) + 1, top):
        s.set(MID, y, MID, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    cellen = doekcellen()
    for (x, y, z) in cellen:
        s.set(x, y, z, kleur_versierd(x, y, z) if n >= 5 else kleur_kaal(x, y, z))
    for x, z, k in ((4, 20, "white_wool"), (5, 20, "white_wool"), (4, 21, "orange_wool"), (20, 4, "yellow_wool"), (20, 5, "white_wool")):
        s.set(x, 0, z, "minecraft:" + k)               # rolls of cloth on the ground
    s.set(4, 1, 20, "minecraft:white_wool")
    if n < 2:
        return s
    # --- 2: the basket (7 x 7, woven of planks and logs) on four trestles, and the ropes -------------------------------------
    for x in range(MID - 3, MID + 4):
        for z in range(MID - 3, MID + 4):
            rand = abs(x - MID) == 3 or abs(z - MID) == 3
            s.set(x, MAND_Y, z, "minecraft:spruce_planks")
            if rand:
                for y in range(MAND_Y + 1, MAND_Y + 4):
                    s.set(x, y, z, "minecraft:stripped_oak_log" if (x + z + y) % 2 else "minecraft:oak_planks", {"axis": "y"} if (x + z + y) % 2 else None)
    for dx in (-3, 3):
        for dz in (-3, 3):
            for y in range(0, MAND_Y):
                log(MID + dx, y, MID + dz)
            for y in range(MAND_Y + 4, MOND_Y + 2):    # a rope from each corner up into the cloth
                s.set(MID + dx, y, MID + dz, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    for y in range(MAND_Y + 1, MAND_Y + 4):            # the little gate in the south side stands open
        s.set(MID, y, MID + 3, lucht)
    s.set(MID, 0, MID + 4, "minecraft:spruce_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    s.set(MID, 1, MID + 4, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
    s.set(MID, 0, MID + 5, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    if n < 3:
        return s
    # --- 3: the burner: a copper machine with a snoet, its flame in the mouth of the balloon -----------------------------------
    by = MAND_Y + 1
    for x in range(MID - 1, MID + 2):
        for z in range(MID - 1, MID + 2):
            s.set(x, by, z, "minecraft:waxed_cut_copper")
            s.set(x, by + 1, z, "minecraft:waxed_copper_block")
    s.set(MID - 1, by + 1, MID + 1, "minecraft:black_concrete")      # the snoet looks south, at whoever climbs in
    s.set(MID + 1, by + 1, MID + 1, "minecraft:black_concrete")
    s.set(MID, by, MID + 1, "minecraft:pink_terracotta")
    s.set(MID, by + 2, MID, "minecraft:waxed_cut_copper")
    for y in range(by + 3, by + 6):
        s.set(MID, y, MID, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    for y in range(by + 6, MOND_Y):
        s.set(MID, y, MID, "minecraft:shroomlight")
    s.set(MID, MOND_Y, MID, "minecraft:orange_stained_glass")
    s.set(MID, MOND_Y + 1, MID, "minecraft:yellow_stained_glass")
    # the cable: the burner runs on vadskracht (a copper box on the ground next to the basket, rods up to the basket's floor)
    s.set(MID + 5, 0, MID - 1, "minecraft:waxed_copper_block")
    s.set(MID + 5, 1, MID - 1, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    s.set(MID + 5, MAND_Y, MID - 1, "minecraft:lightning_rod", {"facing": "west", "powered": "false", "waterlogged": "false"})
    s.set(MID + 4, MAND_Y, MID - 1, "minecraft:lightning_rod", {"facing": "west", "powered": "false", "waterlogged": "false"})
    if n < 4:
        return s
    # --- 4: the stock of knabbels: in the basket, as ballast bags on its sides, a pile and a chute next to it -------------------
    for x, z, hoog in ((MID - 2, MID - 2, 2), (MID + 2, MID - 2, 2), (MID - 2, MID + 2, 1), (MID + 2, MID + 2, 1), (MID - 2, MID, 1), (MID + 2, MID, 1), (MID, MID - 2, 2)):
        for i in range(hoog):
            s.set(x, by + i, z, KNABBELS)
    for dx, dz in ((-4, -1), (-4, 1), (4, -1), (4, 1), (-1, -4), (1, -4)):
        s.set(MID + dx, MAND_Y + 2, MID + dz, KNABBELS)
        s.set(MID + dx, MAND_Y + 3, MID + dz, "minecraft:brown_wool")
    for x, z, hoog in ((5, 5, 3), (6, 5, 2), (5, 6, 2), (6, 6, 1), (4, 5, 1), (5, 4, 1)):
        for i in range(hoog):
            s.set(x, i, z, KNABBELS)
    for i in range(3):                                 # the chute from the pile towards the basket
        s.set(7 + i, 2 - i, 7 + i, "minecraft:spruce_stairs", {"facing": "west", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        for y in range(0, 2 - i):
            s.set(7 + i, y, 7 + i, "minecraft:spruce_planks")
    if n < 5:
        return s
    # --- 5: the decoration: the cloth is coloured above; ears, a golden top, pennants ---------------------------------------
    for s_ in (-1, 1):
        ex, ey = MID + s_ * 5, int(BOL_Y + BOL_R) - 1
        for x in range(ex - 2, ex + 3):
            for y in range(ey - 1, ey + 4):
                for z in range(MID - 1, MID + 2):
                    if math.dist((x, y, z), (ex, ey + 1, MID)) <= 2.3 and (x, y, z) not in s.blocks:
                        s.set(x, y, z, "minecraft:pink_wool")
    for mx in (1, SX - 2):
        for mz in (1, SZ - 2):
            binnen = 1 if mx == 1 else -1
            for i, k in enumerate(("orange_wool", "yellow_wool", "light_blue_wool")):
                s.set(mx + binnen * (1 + i), SY - 4, mz, "minecraft:" + k)
    for a in range(3, SX - 3, 2):                      # flags under the crane frame
        s.set(a, top - 1, 1, "minecraft:" + ("orange_wool" if a % 4 == 1 else "yellow_wool"))
        s.set(a, top - 1, SZ - 2, "minecraft:" + ("orange_wool" if a % 4 == 1 else "yellow_wool"))
    return s


def controle(lagen):
    """What must hold for the five stages -> problems."""
    p = []
    vast = ("minecraft:chest", "minecraft:barrel", "sign", "banner", "campfire", "_bed", "furnace", "hopper", "bell", "lectern", "shulker", "minecraft:fire")
    for n, s in enumerate(lagen, 1):
        if s.entities:
            p.append(f"laag {n}: entities")
        for c, (naam, props, nbt) in s.blocks.items():
            if nbt or any(t in naam for t in vast):
                p.append(f"laag {n}: {naam} op {c} heeft een block entity (of blijft niet staan)")
                break
        if n > 1:
            mist = [c for c in lagen[n - 2].blocks if c not in s.blocks]
            if mist:
                p.append(f"laag {n} mist {len(mist)} cellen van laag {n - 1}, bv. {sorted(mist)[:3]}")
            if len(s.blocks) <= len(lagen[n - 2].blocks) and n < 5:
                p.append(f"laag {n} voegt niets toe")
        # one piece, standing on the ground (y 0)
        cellen = {c for c, bl in s.blocks.items() if bl[0] != "minecraft:air"}
        todo = [c for c in cellen if c[1] == 0]
        gezien = set(todo)
        while todo:
            x, y, z = todo.pop()
            for q in ((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)):
                if q in cellen and q not in gezien:
                    gezien.add(q)
                    todo.append(q)
        if len(gezien) != len(cellen):
            p.append(f"laag {n}: {len(cellen) - len(gezien)} blokken hangen los, bv. {sorted(cellen - gezien)[:3]}")
    if lagen and lagen[-1].blocks == lagen[-2].blocks:
        p.append("laag 5 is gelijk aan laag 4")
    return p


def maak(h):
    return [stadium(h, n) for n in range(1, len(STADIA) + 1)]
