"""
De Knabbelbakkerij (2.8, plein slot "bakkerij" of the Knuffeldal town): the building template
knuffeldal_stadje/bakkerij (31 x 28 x 31, ground top at y = G = 4, the front at z = 30 faces the plein) and the small
GameTest bakery bakkerij_test, with their geometry self-checks.

The building is a giant kaasknabbel bread on a knuffelsteen bread board:
  - a pale "break band" of crumb (the bread's sides, y G+1..G+5) with the dark bottom crust, and above it the golden
    crust dome that rises over the sides like a real loaf, with three scored cuts full of melted cheese, cheese drips
    down the crust, knabbel crumbs, and two round guh ears on top;
  - a big guh face in the crust at the front (the eyes are windows, blush, a nose; the door is its mouth), a sleepy guh
    face with a curly cheese tail at the back, round porthole windows with little knuffelsteen faces on both ends;
  - two brick chimneys with bakkerij_schoorsteen pots that puff knabbelwolkjes;
  - a terrace to the plein with tables, lamp posts, flower boxes and bread stalls, paths all round, a lawn behind;
  - inside (cosy): the kitchen with three knabbelovens under guh-faced hoods, flour barrels and a work table, the long
    counter with cakes, Bakker Korstje behind it, three customer spots (markers bakkerij_klantplek) and the door
    marker (bakkerij_ingang), café corners with tables and bean bags, carpets, lanterns on chains.
check() (from knuffeldal_stadje) walks it: every spot the game needs is reachable on foot from the plein, Korstje
stands on a floor, nothing floats; check_bakkerij() adds the slot rules of the contract (size, jigsaw, entrance) and
the game's needs (markers, ovens with a free spot in front, a chimney pot).
"""
import math

from features import knuffeldal_stadje as st

G = st.G
SIZE = (31, 28, 31)
CX, CZ = 15, 15
RX, RZ = 11.5, 8.5           # the bread's sides (the break band): x 4..26, z 7..23
CAP = 8                      # how high the crust dome rises above the sides
KS, KS_FACE, KS_MUUR, KS_PLAAT, KS_TRAP = st.KS, st.KS_FACE, st.KS_MUUR, st.KS_PLAAT, st.KS_TRAP
KLINK, GRAS, DAK_PLAAT = st.KLINK, st.GRAS, st.DAK_PLAAT
OVEN = "guhs:knabbeloven"
POT = "guhs:bakkerij_schoorsteen"
KLANTPLEK = "guhs:bakkerij_klantplek"
INGANG = "guhs:bakkerij_ingang"

# the bread
CRUMB = ["minecraft:smooth_sandstone", "minecraft:smooth_sandstone", "minecraft:sandstone", "minecraft:birch_planks"]
BOTTOM = "minecraft:brown_terracotta"
CRUST_DARK = "minecraft:terracotta"
CRUST = "minecraft:orange_terracotta"
CRUST_GOLD = "minecraft:yellow_terracotta"
CHEESE = "minecraft:yellow_concrete"
CHEESE_LIGHT = "minecraft:yellow_concrete_powder"
KNABBEL = "minecraft:orange_concrete"
GLAZE = "minecraft:honeycomb_block"


def inside0(x, z, grow=0.0):
    """The bread's sides (rounded rectangle footprint)."""
    return (abs(x - CX) / (RX + grow)) ** 4 + (abs(z - CZ) / (RZ + grow)) ** 4 <= 1.0


def inside1(x, z):
    """The crust dome's footprint: one block wider all round (the loaf rises over its sides)."""
    return inside0(x, z, 1.0)


def dome_top(x, z):
    q = (abs(x - CX) / (RX + 1.0)) ** 2.6 + (abs(z - CZ) / (RZ + 1.0)) ** 2
    return G + 6 + int(round(CAP * math.sqrt(max(0.0, 1.0 - q))))


def bouw(h):
    b = st.Bouw(h, SIZE, "bakkerij")
    W, H, D = SIZE
    mc = h.mc
    rng = b.rng
    # --- the ground: foundation, lawn, paths all round, the terrace to the plein ------------------------------------
    for x in range(W):
        for z in range(D):
            for y in range(G):
                b.set(x, y, z, mc("pink_wool"))
            b.set(x, G, z, GRAS)
    for x in range(W):
        for z in range(D):
            ring = (1 <= x <= 29 and 1 <= z <= 29) and (x <= 2 or x >= 28 or z <= 2)
            terras = z >= 25
            if ring or terras:
                b.set(x, G, z, KS if (terras and (x + z) % 5 == 0 and 2 < x < 28) else KLINK)
    # the bread board: a knuffelsteen plinth one wider than the dome, with a rim of slabs
    board = [(x, z) for x in range(W) for z in range(D) if inside0(x, z, 1.6)]
    for (x, z) in board:
        b.set(x, G, z, KS)
    # --- the sides of the bread (break band) ------------------------------------------------------------------------
    cells0 = [(x, z) for x in range(W) for z in range(D) if inside0(x, z)]
    set0 = set(cells0)
    ring0 = {(x, z) for (x, z) in cells0 if any((x + dx, z + dz) not in set0 for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
    inner0 = set0 - ring0
    for (x, z) in cells0:
        if (x, z) in ring0:
            for y in range(G + 1, G + 6):
                blk = BOTTOM if y == G + 1 else rng.choice(CRUMB)
                if y == G + 5 and (x * 7 + z * 3) % 4 == 0:
                    blk = CHEESE_LIGHT                                   # where the dough burst open: a bit of cheese
                b.set(x, y, z, blk)
        else:
            shop = z >= 15
            b.set(x, G, z, mc("cherry_planks") if shop else KLINK)
    # --- the crust dome ---------------------------------------------------------------------------------------------
    cells1 = [(x, z) for x in range(W) for z in range(D) if inside1(x, z)]
    set1 = set(cells1)
    tops = {c: dome_top(*c) for c in cells1}
    for (x, z) in cells1:
        top = tops[(x, z)]
        low = top
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            low = min(low, tops[n] if n in set1 else G + 6)
        low = max(G + 6, min(low, top - 1))
        if (x, z) not in set0 or (x, z) in ring0:
            low = G + 6                                                # the overhang and the top of the sides: solid crust
        for y in range(low, top + 1):
            t = (y - (G + 6)) / CAP
            v = (x * 73 + y * 31 + z * 57) % 13                          # a little variation, no noise
            if (x, z) not in set0 and y == G + 6:
                blk = CRUST_DARK
            elif t > 0.8:
                blk = GLAZE if v < 3 else CRUST_GOLD
            elif t > 0.55:
                blk = CRUST_GOLD if v < 5 else CRUST
            elif t > 0.2:
                blk = CRUST if v else CRUST_GOLD
            else:
                blk = CRUST_DARK if v < 9 else CRUST
            b.set(x, y, z, blk)
    # the scored cuts: three diagonal slashes over the top, pale crumb with melted cheese in them
    for i, c0 in enumerate((-8, 0, 8)):
        for s in range(-5, 6):
            x = CX + c0 + s
            z = CZ - s // 2 + (1 if i == 1 else 0)
            if (x, z) in set1 and tops[(x, z)] >= G + 11:
                y = tops[(x, z)]
                b.set(x, y, z, CHEESE if abs(s) < 4 else CHEESE_LIGHT)
                for (nx, nz) in ((x, z - 1), (x, z + 1)):
                    if (nx, nz) in set1 and tops[(nx, nz)] >= y - 1 and b.get(nx, tops[(nx, nz)], nz) not in (CHEESE, CHEESE_LIGHT):
                        b.set(nx, tops[(nx, nz)], nz, "minecraft:smooth_sandstone")
    # cheese drips running down the crust on both long sides, and knabbel crumbs sprinkled on the top
    for x in (6, 10, 13, 18, 21, 24):
        for zside, sgn in ((min(z for (xx, z) in cells1 if xx == x), -1), (max(z for (xx, z) in cells1 if xx == x), 1)):
            if sgn > 0 and 12 <= x <= 18:
                continue                                              # (not over the face)
            length = 2 + (x * 3) % 3
            zz = zside
            for k in range(length):
                y = G + 6 + k
                if b.get(x, y, zz) and b.get(x, y, zz) != "minecraft:air":
                    b.set(x, y, zz, CHEESE)
            b.set(x, G + 6, zz, CHEESE)
    for (x, z) in cells1:
        if tops[(x, z)] >= G + 10 and (x * 13 + z * 7) % 11 == 0 and b.get(x, tops[(x, z)], z) not in (CHEESE, CHEESE_LIGHT):
            b.set(x, tops[(x, z)], z, KNABBEL)
    # --- the interior: air under the dome ------------------------------------------------------------------------------
    for (x, z) in inner0:
        ceiling = tops[(x, z)] - 1
        for (nx, nz) in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            if (nx, nz) in set1:
                ceiling = min(ceiling, tops[(nx, nz)] - 1)
        for y in range(G + 1, max(G + 6, ceiling)):
            b.air(x, y, z, x, y, z)
    # a crumb ceiling edge inside above the sides (so the ceiling has no holes where the dome meets the sides)
    for (x, z) in inner0:
        if any((x + dx, z + dz) in ring0 for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            if b.get(x, G + 6, z) in (None, "minecraft:air"):
                b.set(x, G + 6, z, CRUST_DARK)
    zmax = max(z for (x, z) in cells0 if x == CX)                 # the front of the sides (z 23)
    zmin = min(z for (x, z) in cells0 if x == CX)                 # the back (z 7)
    # --- the front: the door (the guh's mouth) and the big guh face in the crust ---------------------------------------
    for x in (CX - 1, CX, CX + 1):
        for y in (G + 1, G + 2, G + 3):
            b.air(x, y, zmax, x, y, zmax)
        b.set(x, G, zmax, mc("pink_wool"))                        # a pink tongue for a doormat
    for x in (CX - 2, CX + 2):
        b.set(x, G + 1, zmax, KS_MUUR, {"up": "true"})
        b.set(x, G + 2, zmax, KS_MUUR, {"up": "true"})
        b.set(x, G + 3, zmax, KS_MUUR, {"up": "true"})
    for x in range(CX - 1, CX + 2):
        b.set(x, G + 4, zmax, KS)
    face = dict(st.FACE_BLOCKS)
    face.update({"eye": "minecraft:black_stained_glass", "ring": "minecraft:light_blue_stained_glass", "shine": "minecraft:white_stained_glass",
                 "cheek": "minecraft:pink_terracotta", "nose": "minecraft:magenta_terracotta", "mouth": "minecraft:brown_concrete"})
    face_op_brood(b, CX, G + 7, "south", 6.2, face, skip=lambda x, y, z, role: y <= G + 4 and abs(x - CX) <= 2)
    # a little awning of pluisdak over the door with a lantern, and two display windows either side
    for x in range(CX - 2, CX + 3):
        b.set(x, G + 4, zmax + 1, DAK_PLAAT, {"type": "bottom", "waterlogged": "false"})
    b.set(CX, G + 3, zmax + 1, st.LAMP, {"hanging": "true", "waterlogged": "false"})
    for x0 in (7, 20):
        for x in range(x0, x0 + 3):
            z = max(z for (xx, z) in cells0 if xx == x)
            for y in (G + 2, G + 3):
                b.set(x, y, z, mc("pink_stained_glass_pane"), st.pane(False))
            b.bloembak(x, z + 1)
    # --- the back: a sleepy guh face and a curly cheese tail --------------------------------------------------------------
    slaap = dict(face)
    slaap.update({"eye": "minecraft:brown_concrete", "ring": CRUST, "shine": CRUST})
    face_op_brood(b, CX, G + 7, "north", 5.4, slaap, slaperig=True)
    for (u, v) in ((0, 1), (0, 2), (1, 3), (2, 3), (3, 2), (3, 1), (2, 0)):
        b.set(CX + 4 + u, G + v, zmin - 1, CHEESE)
    b.set(CX + 4, G, zmin - 1, KS)
    # --- both ends: a round porthole window, knuffelsteen faces either side, flower boxes ------------------------------
    for side, sgn in (("west", -1), ("east", 1)):
        xs = [x for (x, z) in cells0 if z == CZ]
        xe = min(xs) if sgn < 0 else max(xs)
        for dz in (-1, 0, 1):
            for y in (G + 2, G + 3, G + 4):
                if abs(dz) + abs(y - (G + 3)) <= 1:
                    b.set(xe, y, CZ + dz, mc("light_blue_stained_glass_pane"), st.pane(True))
                    if (xe - sgn, CZ + dz) in ring0:
                        b.set(xe - sgn, y, CZ + dz, mc("light_blue_stained_glass_pane"), st.pane(True))
        for dz in (-4, 4):
            row = [x for (x, z) in cells0 if z == CZ + dz]
            xf = min(row) if sgn < 0 else max(row)
            b.set(xf, G + 3, CZ + dz, KS_FACE, {"facing": side, "stemming": str((dz // 4 + 1 + (sgn > 0)) % 4)})
            b.gezichten += 1
            b.bloembak(xf + sgn, CZ + dz)
    # --- the ears on top of the loaf, a little to the front -----------------------------------------------------------
    for sx in (-1, 1):
        ex, ez = CX + sx * 7, CZ + 3
        base = tops[(ex, ez)]
        ey = base + 3
        for du in range(-3, 4):
            for dv in range(-3, 4):
                r2 = du * du + dv * dv
                if r2 <= 10 and ey + dv >= base:
                    inner = r2 <= 4 and dv > -3
                    b.set(ex + du, ey + dv, ez, "minecraft:pink_wool" if inner and r2 <= 1 else "minecraft:pink_terracotta" if inner else CRUST)
                    b.set(ex + du, ey + dv, ez - 1, CRUST_GOLD if dv > 0 else CRUST)
        for du in range(-3, 4):                                   # (the ear stands on the crust: fill under it)
            for zz in (ez, ez - 1):
                for y in range(tops.get((ex + du, zz), base), ey):
                    if b.get(ex + du, y, zz) in (None, "minecraft:air"):
                        b.set(ex + du, y, zz, CRUST)
    # --- two chimneys with puffing pots, over the kitchen --------------------------------------------------------------
    for (hx, hz, extra) in ((CX + 6, CZ - 4, 3), (CX - 7, CZ - 3, 2)):
        base = tops[(hx, hz)]
        for y in range(base + 1, base + 1 + extra):
            b.set(hx, y, hz, mc("bricks"))
        b.set(hx, base + 1 + extra, hz, POT)
    # --- inside: the kitchen ------------------------------------------------------------------------------------------
    zo = zmin + 1                                                 # the back wall's inside (ovens stand here)
    ovens = []
    for x in (CX - 6, CX, CX + 6):
        b.set(x, G + 1, zo, OVEN, {"facing": "south", "lit": "false"})
        b.set(x, G + 3, zo, KS_FACE, {"facing": "south", "stemming": str((x // 6) % 4)})
        b.set(x, G + 2, zo, mc("bricks"))
        b.gezichten += 1
        ovens.append((x, G + 1, zo))
        b.targets[f"oven {x}"] = (x, G + 1, zo + 1)
    for x in (CX - 9, CX - 3, CX + 3, CX + 9):
        if (x, zo) in inner0:
            b.set(x, G + 1, zo, mc("barrel"), {"facing": "up", "open": "false"})
            b.set(x, G + 2, zo, mc("white_wool"))                 # flour sacks
    b.set(CX - 1, G + 1, zo + 3, "guhs:guh_tafel", {"facing": "south"})
    b.set(CX + 1, G + 1, zo + 3, "guhs:guh_tafel", {"facing": "south"})
    b.set(CX + 1, G + 2, zo + 3, "guhs:guh_taart")
    # the counter (with a way through on the west end), cakes on it, Korstje behind it
    zc = 15
    xs = sorted(x for (x, z) in inner0 if z == zc)
    for x in range(xs[0] + 2, xs[-1] + 1):
        b.set(x, G + 1, zc, mc("stripped_cherry_log"), {"axis": "x"})
    for x in (xs[0] + 4, CX - 2, CX + 2, xs[-1] - 3):
        b.set(x, G + 2, zc, "guhs:guh_taart")
    b.npc(xs[0] + 3, G + 1, zc - 1, "bakkerguh", "south")
    b.targets["door naar de keuken"] = (xs[0], G + 1, zc)
    # the customer spots (in front of the counter) and the door marker
    plekken = [(CX - 4, G + 1, zc + 1), (CX, G + 1, zc + 1), (CX + 4, G + 1, zc + 1)]
    ingang = (CX, G + 1, zmax - 1)
    for i, p in enumerate(plekken):
        b.targets[f"klantplek {i}"] = p
    b.targets["ingang"] = ingang
    # a carpet runner from the door to the counter, café corners, bean bags
    for z in range(zc + 2, zmax):
        for x in (CX - 1, CX, CX + 1):
            if b.get(x, G + 1, z) is None:
                b.set(x, G + 1, z, mc("pink_carpet"))
    for (tx, tz) in ((xs[0] + 2, 19), (xs[-1] - 2, 19)):
        b.set(tx, G + 1, tz, "guhs:guh_tafel", {"facing": "south"})
        b.set(tx, G + 1, tz - 1, "guhs:guh_stoel", {"facing": "south"})
        b.set(tx, G + 1, tz + 1, "guhs:guh_stoel", {"facing": "north"})
        b.set(tx, G + 2, tz, mc("potted_pink_tulip") if tx < CX else mc("potted_allium"))
    b.set(xs[0] + 1, G + 1, 21, "guhs:pink_zitzak", {"facing": "east"})
    b.set(xs[-1] - 1, G + 1, 21, "guhs:pink_zitzak", {"facing": "west"})
    # lanterns on chains from the dome
    for (lx, lz) in ((CX - 6, 11), (CX + 6, 11), (CX, 13), (CX - 5, 19), (CX + 5, 19)):
        y = G + 1
        while b.get(lx, y, lz) in (None, "minecraft:air") and y < SIZE[1] - 1:
            y += 1
        for cy in range(G + 6, y):
            b.set(lx, cy, lz, mc("chain"), {"axis": "y", "waterlogged": "false"})
        b.set(lx, G + 5, lz, st.LAMP, {"hanging": "true", "waterlogged": "false"})
    # --- outside: the terrace, lamp posts, flower boxes, bread stalls, benches ------------------------------------------
    for (tx, tz) in ((5, 27), (25, 27)):
        b.set(tx, G + 1, tz, "guhs:guh_tafel", {"facing": "south"})
        b.set(tx - 1, G + 1, tz, "guhs:guh_stoel", {"facing": "east"})
        b.set(tx + 1, G + 1, tz, "guhs:guh_stoel", {"facing": "west"})
        b.set(tx, G + 2, tz, "guhs:guh_taart")
    for x in (2, 28):
        b.lantaarnpaal(x, 28, height=4)
    for x in (10, 20):
        b.lantaarnpaal(x, 29, height=3)
    for x in (8, 12, 18, 22):
        b.bloembak(x, 29)
    # bread stalls: a barrel with a pluisdak roof on posts, a flower box
    for sx in (1, 29):
        b.set(sx, G + 1, 24, mc("barrel"), {"facing": "up", "open": "false"})
        b.set(sx, G + 1, 22, mc("barrel"), {"facing": "up", "open": "false"})
        b.set(sx, G + 1, 23, KS_FACE, {"facing": "south" if sx == 1 else "south", "stemming": "3"})
        b.gezichten += 1
    b.bank(13, 30, "south")
    b.bank(17, 30, "south")
    # a festive garland over the terrace, between the two big lamp posts
    b.slinger(3, 28, 27, 28, G + 4)
    # knuffelsteen posts with little guh faces on the corners of the bread board
    for (px, pz, f) in ((3, 25, "south"), (27, 25, "south"), (3, 5, "north"), (27, 5, "north")):
        if b.get(px, G + 1, pz) is None:
            b.set(px, G + 1, pz, KS_MUUR, {"up": "true"})
            b.set(px, G + 2, pz, KS_FACE, {"facing": f, "stemming": str((px + pz) % 4)})
            b.gezichten += 1
    # the lawn behind: fluff, flowers, mushrooms, a few pluizen-bushes and leaf piles
    for (bx, bz) in ((4, 0), (26, 0), (0, 5), (30, 5)):
        for dx in (0, 1):
            for dy in (1, 2):
                if b.get(bx + dx, G + dy, bz) is None and b.get(bx + dx, G, bz) == GRAS:
                    b.set(bx + dx, G + dy, bz, st.BLAD, {"distance": "1", "persistent": "true", "waterlogged": "false"})
    b.grasveld(0, 0, W - 1, D - 1, 0.1)
    # --- the jigsaw to the plein and the walking starts -----------------------------------------------------------------
    b.jigsaw(15, G, 30, "south_up", "guhs:plein_ingang", "minecraft:empty", "minecraft:empty", KLINK)
    for x in (14, 15, 16):
        for y in (G + 1, G + 2, G + 3):
            for z in (28, 29, 30):
                b.air(x, y, z, x, y, z)
    b.starts = [(15, G + 1, 29), (1, G + 1, 29), (29, G + 1, 29)]
    b.s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=SIZE[1])
    b.plekken, b.ingang, b.ovens = plekken, ingang, ovens
    return b


def face_op_brood(b, cx, cy, facing, R, blocks, skip=None, slaperig=False):
    """Paints a guh face (radius R) onto the round front or back of the loaf: every face spot lands on the outermost block
    of the loaf in its row and column (the crust or the sides), windows go two blocks deep. slaperig: closed eyes."""
    dz = 1 if facing == "south" else -1
    n = int(R + 1)
    for du in range(-n, n + 1):
        x = cx - du * dz                                          # (a face looking south has its right on the west)
        for dv in range(-n, n + 1):
            role = st.face_role(du, dv, R)
            if role is None or role == "skin":
                continue
            if slaperig and role in ("eye", "ring", "shine"):
                # closed eyes: a little downward arc instead of a round eye
                a = abs(abs(du) - 0.42 * R)
                dv0 = round(0.1 * R)
                if not (a <= 1.3 and dv == (dv0 - 1 if a <= 0.5 else dv0)):
                    continue
                role = "eye"
            y = cy + dv
            zs = [z for z in range(SIZE[2]) if b.get(x, y, z) not in (None, "minecraft:air")
                  and (inside1(x, z) or inside0(x, z))]
            if not zs:
                continue
            z = max(zs) if dz > 0 else min(zs)
            if skip and skip(x, y, z, role):
                continue
            b.set(x, y, z, blocks[role])
            if role in ("eye", "ring", "shine") and not slaperig:
                back = z - dz
                if b.get(x, y, back) not in (None, "minecraft:air"):
                    b.set(x, y, back, blocks[role])
    b.grote_gezichten += 1


def check_bakkerij(b):
    """The slot rules of the contract (par. 6.3) and what Korstje's game needs."""
    problems = []
    W, H, D = SIZE
    if not (16 <= H <= 48) or (W, D) != (31, 31):
        problems.append(f"size {SIZE}")
    js = b.jigsaws
    if len(js) != 1 or js[0][0] != (15, G, 30) or js[0][1] != "guhs:plein_ingang" or js[0][4] != "south_up":
        problems.append(f"jigsaw {js}")
    for x in (14, 15, 16):
        for y in (G + 1, G + 2, G + 3):
            for z in (28, 29, 30):
                if b.get(x, y, z) not in (None, "minecraft:air"):
                    problems.append(f"entrance blocked at {(x, y, z)}: {b.get(x, y, z)}")
    for x in range(W):
        for z in range(D):
            for y in range(G):
                if b.get(x, y, z) in (None, "minecraft:air"):
                    problems.append(f"foundation hole at {(x, y, z)}")
    for (x, y, z) in b.ovens:
        if b.get(x, y, z + 1) not in (None, "minecraft:air") or b.get(x, y + 1, z + 1) not in (None, "minecraft:air"):
            problems.append(f"no room in front of the oven at {(x, y, z)}")
    for p in b.plekken + [b.ingang]:
        if b.get(*p) not in (None, "minecraft:air", "minecraft:pink_carpet") or not st.solid(b.get(p[0], p[1] - 1, p[2])):
            problems.append(f"marker spot {p} is not free on a floor")
    pots = [p for p, (blk, _, _) in b.s.blocks.items() if blk == POT]
    if len(pots) < 1:
        problems.append("no chimney pot")
    for (x, y, z) in pots:
        if b.get(x, y + 1, z) not in (None, "minecraft:air"):
            problems.append(f"the chimney pot at {(x, y, z)} is covered")
    npcs = [e for e in b.s.entities if e[3].get("id") == "guhs:guh_npc"]
    if len(npcs) != 1 or npcs[0][3].get("Kind") != "bakkerguh":
        problems.append(f"npcs {npcs}")
    faces = sum(1 for (blk, _, _) in b.s.blocks.values() if blk == KS_FACE)
    if faces < 8 or b.grote_gezichten < 2:
        problems.append(f"only {faces} knuffelsteen faces / {b.grote_gezichten} big faces")
    top = max(y for (x, y, z) in b.s.blocks if b.s.blocks[(x, y, z)][0] != "minecraft:air")
    if top >= H:
        problems.append(f"too tall: {top}")
    return problems


def zet_markers(b):
    """The markers go in after the walking check (they're invisible, but the check doesn't know them)."""
    for p in b.plekken:
        b.set(*p, KLANTPLEK)
    b.set(*b.ingang, INGANG)


def test_bakkerij(h):
    """The GameTest bakery (15 x 6 x 15, floor at template y 0): Korstje behind a counter at z 6, three ovens at z 2
    facing south, three customer spots at z 7 and the door marker at (7, 1, 12)."""
    mc = h.mc
    s = h.Structure((15, 6, 15))
    for x in range(15):
        for z in range(15):
            s.set(x, 0, z, KLINK if z <= 6 else mc("cherry_planks"))
    for x in (4, 7, 10):
        s.set(x, 1, 2, OVEN, {"facing": "south", "lit": "false"})
    for x in range(3, 13):
        s.set(x, 1, 6, mc("stripped_cherry_log"), {"axis": "x"})
    for x in (5, 7, 9):
        s.set(x, 1, 7, KLANTPLEK)
    s.set(7, 1, 12, INGANG)
    ms = h.ms
    s.entity(1.5, 1.0, 5.5, {"id": "guhs:guh_npc", "Kind": "bakkerguh", "PersistenceRequired": ms.Byte(1), "NoAI": ms.Byte(1),
                             "Rotation": ms.floats(0.0, 0.0)})
    s.save("bakkerij_test")
