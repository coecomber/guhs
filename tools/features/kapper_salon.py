"""
De Pluiskapper "Knip & Vads" (2.8): the plein-slot template knuffeldal_stadje/kapper (31 x 36 x 31, ground top at y = 4,
the one jigsaw guhs:plein_ingang at (15, 4, 30) south_up: the front faces the plein).

The salon is a big guh head with an enormous pink curly perm as its roof ("the hair"):
  - front (to the plein): the guh face (its eyes are windows, blush, a nose), the door under an awning, a front yard with
    two kapperspalen (striped barber's poles), flower boxes, lamp posts with little guh faces and a bench;
  - the walls get a barber's-pole band (pink / white / sky-blue stripes running round the whole building);
  - the roof: a curly hair dome with two guh ears poking out, a billboard "KNIP & / VADS" (readable from the front and
    the back), giant open scissors standing on the crown and a comb stuck in the hair at the back;
  - east: a giant föhn standing on its handle, blowing at the hair (with its pink cord on the ground);
  - west: a giant comb leaning against the wall; north: a sleepy guh face and four giant bottles of haarverf;
  - inside: the showstoel on a round podium in front of a Hollywood mirror (froglights round it) with Kapper Krulletje
    beside it, three stations with kappersstoelen and mirrors (west), three haarwasbakken (east), a waiting corner with
    a bench and bean bags, a counter with bottles of dye (candles) and framed scissors, hair tufts on the floor, lanterns.
Checked by check(): reachable on foot from the plein, Krulletje and the chairs reachable, nothing floats, doorway free,
the jigsaw, size, lanterns hang on something, and there are enough guh faces.
"""
import math
import os

try:
    from features import knuffeldal_stadje as st
except ImportError:                                  # (run on its own)
    import sys
    sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    from features import knuffeldal_stadje as st

W, H, D = 31, 36, 31
G = 4
NAME = "knuffeldal_stadje/kapper"
CX, CZ, RX, RZ = 15, 13.5, 12.6, 11.6
WALL_TOP = G + 10            # y 14: wall top and the ceiling
KS, KS_TRAP, KS_PLAAT, KS_MUUR, KS_FACE = st.KS, st.KS_TRAP, st.KS_PLAAT, st.KS_MUUR, st.KS_FACE
DAK, DAK_PLAAT, KLINK, GRAS = st.DAK, st.DAK_PLAAT, st.KLINK, st.GRAS
STOEL, WASBAK = "guhs:kappersstoel", "guhs:haarwasbak"
LAMP = st.LAMP
PAAL = ["minecraft:pink_concrete", "minecraft:white_concrete", "minecraft:light_blue_concrete", "minecraft:white_concrete"]
SHOWSTOEL = (15, G + 1, 10)
KRULLETJE = (12, G + 1, 11)

FONT = {
    "K": ["X.X", "X.X", "XX.", "X.X", "X.X"], "N": ["X..X", "XX.X", "X.XX", "X..X", "X..X"], "I": ["X", "X", "X", "X", "X"],
    "P": ["XXX", "X.X", "XXX", "X..", "X.."], "&": [".X.", "X.X", ".X.", "X.X", ".XX"], "V": ["X.X", "X.X", "X.X", "X.X", ".X."],
    "A": [".X.", "X.X", "XXX", "X.X", "X.X"], "D": ["XX.", "X.X", "X.X", "X.X", "XX."], "S": ["XXX", "X..", "XXX", "..X", "XXX"],
    " ": ["..", "..", "..", "..", ".."],
}


def inside(x, z, shrink=0.0):
    return ((x - CX) / (RX - shrink)) ** 4 + ((z - CZ) / (RZ - shrink)) ** 4 <= 1.0


def dome_height(x, z):
    d = math.sqrt(((x - CX) / (RX + 1)) ** 2 + ((z - CZ) / (RZ + 1)) ** 2)
    return WALL_TOP + 1 + int(round(8.0 * math.sqrt(max(0.0, 1 - min(1.0, d) ** 2))))


def text_width(line):
    return sum(len(FONT[c][0]) for c in line) + len(line) - 1


def build(h):
    mc = h.mc
    b = st.Bouw(h, (W, H, D), NAME)
    cells = [(x, z) for x in range(W) for z in range(D) if inside(x, z)]
    inner = {(x, z) for (x, z) in cells if inside(x, z, 1.0)}
    cellset = set(cells)
    roof_cells = [(x, z) for x in range(W) for z in range(D) if inside(x, z, -1.0)]

    # --- ground: foundation y 0..3, the yard at y 4 (grass, the klinkers path, a paved ring round the salon) --------------
    for x in range(W):
        for z in range(D):
            for y in range(G):
                b.set(x, y, z, mc("dirt") if y < G - 1 else KS)
            ring = inside(x, z, -2.0)
            b.set(x, G, z, KLINK if (13 <= x <= 17 and z >= 22) or ring else GRAS)

    # --- the walls: knuffelsteen with a barber's-pole band, the floor and the ceiling -------------------------------------
    for (x, z) in cells:
        if (x, z) in inner:
            b.set(x, G, z, mc("white_concrete") if (x + z) % 2 else mc("pink_concrete"))
            b.set(x, WALL_TOP, z, mc("cherry_planks"))
        else:
            for y in range(G + 1, WALL_TOP + 1):
                if y in (G + 8, G + 9):                                   # the barber's-pole band (stripes run round)
                    wall = PAAL[((x + z + y) // 2) % 4]
                elif y == G + 1:
                    wall = KS
                else:
                    wall = "guhs:knuffelsteen" if y < G + 8 else mc("pink_terracotta")
                b.set(x, y, z, wall)
            b.set(x, G, z, KS)

    # --- the face on the front (south) wall: eye windows, blush, nose, a smile; the door under an awning -----------------
    face_blocks = dict(st.FACE_BLOCKS)
    face_blocks.update({"eye": mc("black_stained_glass"), "ring": mc("light_blue_stained_glass"), "shine": mc("white_stained_glass"),
                        "skin": "guhs:knuffelsteen"})
    st.face_on_wall(b, cells, inner, "south", 15, G + 7, 5.6, face_blocks, skip=lambda x, y, z, role: y <= G + 4 and abs(x - 15) <= 2)
    front_z = max(z for (x, z) in cells if x == 15)
    for x in (14, 15, 16):
        for y in (G + 1, G + 2, G + 3):
            b.air(x, y, front_z, x, y, front_z)
    for x in range(12, 19):
        b.set(x, G + 4, front_z + 1, DAK_PLAAT, {"type": "bottom", "waterlogged": "false"})
        b.set(x, G + 4, front_z, DAK)
    b.set(15, G + 3, front_z + 1, LAMP, {"hanging": "true", "waterlogged": "false"})
    for x in (13, 17):
        b.set(x, G + 1, front_z + 1, mc("potted_pink_tulip") if x == 13 else mc("potted_allium"))

    # --- the side faces (the eyes are windows) and the back face; little knuffelsteen faces along the base -------------------
    st.face_on_wall(b, cells, inner, "west", int(CZ), G + 9, 3.6, face_blocks)
    st.face_on_wall(b, cells, inner, "east", int(CZ), G + 9, 3.6, face_blocks)
    st.face_on_wall(b, cells, inner, "north", 15, G + 7, 4.4, face_blocks)
    for (x, z) in cells:
        if (x, z) in inner:
            continue
        out = [(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) if (x + dx, z + dz) not in cellset]
        if out and (x * 7 + z * 3) % 9 == 0 and b.get(x, G + 1, z) == KS and not (12 <= x <= 18 and z > 20):
            dx, dz = out[0][0] - x, out[0][1] - z
            facing = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(dx, dz)]
            b.set(x, G + 1, z, KS_FACE, {"facing": facing, "stemming": str((x + z) % 4)})
            b.gezichten += 1

    interior(h, b, inner)
    roof(h, b, roof_cells)
    fohn(h, b)
    kam(h, b)
    achterkant(h, b, cells)
    voortuin(h, b, front_z)

    # --- the jigsaw (the plein's entrance) --------------------------------------------------------------------------------
    b.jigsaw(15, G, 30, "south_up", "guhs:plein_ingang", "minecraft:empty", "minecraft:empty", KLINK)
    for x in (14, 15, 16):
        for y in (G + 1, G + 2, G + 3):
            for z in (28, 29, 30):
                b.air(x, y, z, x, y, z)
    b.s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    b.starts = [(x, G + 1, 30) for x in (14, 15, 16)]
    b.targets["de showstoel"] = (SHOWSTOEL[0], SHOWSTOEL[1], SHOWSTOEL[2] + 2)
    b.targets["station west"] = (8, G + 1, 12)
    b.targets["wasbak oost"] = (21, G + 1, 11)
    b.targets["wachthoek"] = (21, G + 1, 19)
    problems = st.check(b, extra)
    if problems:
        raise SystemExit("kapper salon check failed:\n  " + "\n  ".join(problems[:30]))
    b.s.save(NAME)
    return b


def interior(h, b, inner):
    mc = h.mc
    # the showstoel on a round podium in front of the Hollywood mirror on the back wall; Krulletje beside it
    sx, sy, sz = SHOWSTOEL
    for x in range(sx - 3, sx + 4):
        for z in range(sz - 2, sz + 4):
            if math.hypot(x - sx, z - (sz + 0.5)) <= 3.0 and (x, z) in inner and (x, z) != (sx, sz):
                b.set(x, G + 1, z, mc("magenta_carpet") if math.hypot(x - sx, z - (sz + 0.5)) > 2.2 else mc("pink_carpet"))
    b.set(sx, sy, sz, STOEL, {"facing": "south"})
    back_z = min(z for (x, z) in inner if x == sx) - 1                    # the back wall behind the show chair
    for x in range(sx - 3, sx + 4):
        for y in range(G + 2, G + 8):
            edge = x in (sx - 3, sx + 3) or y in (G + 2, G + 7)
            if edge:
                b.set(x, y, back_z, mc("pearlescent_froglight") if (x + y) % 2 == 0 else mc("white_concrete"), {"axis": "y"} if (x + y) % 2 == 0 else None)
            else:
                b.set(x, y, back_z, mc("glass"))
    kx, ky, kz = KRULLETJE
    b.npc(kx, ky, kz, "kapperguh", "east")
    b.set(kx - 1, G + 1, kz - 1, "guhs:guh_kast", {"facing": "south", "open": "false"})
    # three stations on the west side: a kappersstoel facing a mirror in the wall, a little cupboard with dye in between
    for z in (6, 11, 16):
        row = sorted(x for (x, zz) in inner if zz == z)
        wx = row[0] - 1                                                    # the wall
        b.set(row[0] + 1, G + 1, z, STOEL, {"facing": "west"})
        for zz in (z - 1, z, z + 1):
            for y in (G + 2, G + 3, G + 4):
                wall_x = min(x for (x, z2) in inner if z2 == zz) - 1
                b.set(wall_x, y, zz, mc("glass") if y < G + 4 else mc("quartz_block"))
        b.set(row[0], G + 1, z + 2, mc("white_carpet"))                   # hair tufts on the floor
        b.set(row[0] + 2, G + 1, z - 1, mc("pink_carpet"))
    for z in (8, 13):
        row = sorted(x for (x, zz) in inner if zz == z)
        b.set(row[0], G + 1, z, "guhs:guh_kast", {"facing": "east", "open": "false"})
        c = ("pink", "lime", "light_blue")[(z // 5) % 3]
        b.set(row[0], G + 2, z, mc(f"{c}_candle"), {"candles": "3", "lit": "false", "waterlogged": "false"})
    # three haarwasbakken on the east side, shampoo shelves above them
    for z in (7, 10, 13):
        row = sorted(x for (x, zz) in inner if zz == z)
        b.set(row[-1], G + 1, z, WASBAK, {"facing": "west"})
        b.set(row[-1] - 2, G + 1, z, "guhs:guh_stoel", {"facing": "east"})
        wall_x = row[-1] + 1
        b.set(row[-1], G + 4, z, mc("quartz_slab"), {"type": "top", "waterlogged": "false"})
        b.set(row[-1], G + 5, z, mc(("pink", "white", "magenta")[z % 3] + "_candle"), {"candles": "3", "lit": "false", "waterlogged": "false"})
    # the waiting corner (south-east): a bench, bean bags, a table with a flower
    for (x, z, f) in ((20, 21, "north"), (22, 20, "north")):
        if (x, z) in inner:
            b.set(x, G + 1, z, "guhs:guh_bank", {"facing": f})
    for (x, z, c) in ((24, 17, "pink"), (24, 19, "magenta")):
        if (x, z) in inner:
            b.set(x, G + 1, z, f"guhs:{c}_zitzak", {"facing": "west"})
    if (22, 17) in inner:
        b.set(22, G + 1, 17, "guhs:guh_tafel", {"facing": "north"})
        b.set(22, G + 2, 17, mc("potted_pink_tulip"))
    # the counter (south-west): a desk of knuffelsteen with the kassa (a guh face) and bottles of dye
    for x in range(6, 11):
        if (x, 20) in inner:
            b.set(x, G + 1, 20, KS_PLAAT, {"type": "top", "waterlogged": "false"})
    b.set(8, G + 2, 20, KS_FACE, {"facing": "north", "stemming": "0"})
    b.gezichten += 1
    for i, x in enumerate((6, 7, 9, 10)):
        if (x, 20) in inner:
            b.set(x, G + 2, 20, mc(("pink", "yellow", "light_blue", "lime")[i] + "_candle"), {"candles": str(2 + i % 3), "lit": "false", "waterlogged": "false"})
    # framed scissors and dye on the walls (item frames)
    frames = [((sx - 5), G + 4, back_z, 3, "guhs:kappersschaar"), ((sx + 5), G + 4, back_z, 3, "guhs:haarverf_roze"),
              ((sx - 5), G + 6, back_z, 3, "guhs:haarverf_mint"), ((sx + 5), G + 6, back_z, 3, "guhs:krulmunt")]
    for (x, y, wz, facing, item) in frames:
        if b.get(x, y, wz) not in (None, "minecraft:air") and b.get(x, y, wz + 1) in (None, "minecraft:air"):
            b.s.entity(x + 0.5, y + 0.5, wz + 1 + 0.03125, {"id": "minecraft:item_frame", "Facing": h.ms.Byte(facing), "Fixed": h.ms.Byte(1),
                                                              "Invulnerable": h.ms.Byte(1), "TileX": x, "TileY": y, "TileZ": wz + 1,
                                                              "Item": {"id": item, "count": 1}})
    # lanterns under the ceiling, so it's light everywhere
    for (x, z) in inner:
        if x % 5 == 0 and z % 5 == 2 and b.get(x, WALL_TOP - 1, z) is None:
            b.set(x, WALL_TOP - 1, z, LAMP, {"hanging": "true", "waterlogged": "false"})


def roof(h, b, roof_cells):
    """The curly hair dome (pluisdak with pink and white wool curls), two ears, the scissors, the billboard."""
    mc = h.mc
    top = {}
    for (x, z) in roof_cells:
        ht = dome_height(x, z)
        for y in range(WALL_TOP + 1, ht + 1):
            k = (x * 5 + z * 3 + y * 7) % 11
            blk = mc("white_wool") if k == 0 else mc("pink_wool") if k in (3, 7) else DAK
            b.set(x, y, z, blk)
        top[(x, z)] = ht
    # curls: little bumps sticking out of the hair
    for (x, z), ht in top.items():
        if (x * 13 + z * 7) % 9 == 0 and ht > WALL_TOP + 2:
            b.set(x, ht + 1, z, mc("pink_wool") if (x + z) % 2 else mc("white_wool"))
    # the ears, poking out of the hair on both sides (round, their insides pink)
    for sx in (-1, 1):
        ex, ez, ey = int(round(CX + sx * 9.5)), 13, WALL_TOP + 9
        for du in range(-3, 4):
            for dv in range(-3, 4):
                r2 = du * du + dv * dv
                if r2 <= 10:
                    b.set(ex + du, ey + dv, ez + 1, mc("pink_terracotta") if r2 <= 4 else mc("pink_wool"))
                    b.set(ex + du, ey + dv, ez, mc("pink_wool"))
        for y in range(WALL_TOP + 1, ey - 2):                               # the ear stands in the hair
            b.set(ex, y, ez, mc("pink_wool"))
    # the giant scissors on the crown: open, blades up, pink rings in the hair
    pz = 9
    px_, py_ = 15, dome_height(15, pz) + 5
    for sgn in (-1, 1):
        line(b, px_, py_, px_ + sgn * 4, py_ + 7, pz, mc("iron_block"))              # a blade, pointing up
        line(b, px_ - sgn, py_ + 1, px_ + sgn * 3, py_ + 7, pz, mc("light_gray_concrete"), only_empty=True)
        line(b, px_, py_, px_ - sgn * 3, py_ - 3, pz, mc("light_gray_concrete"))     # the shank down to the ring
        rx, ry = px_ - sgn * 4, py_ - 5
        for du in range(-2, 3):
            for dv in range(-2, 3):
                if 2 <= abs(du) + abs(dv) <= 3 and not (abs(du) == 2 and abs(dv) == 2):
                    b.set(rx + du, ry + dv, pz, mc("pink_concrete"))
        for y in range(WALL_TOP + 1, ry - 1):                              # (the ring sits in the hair)
            b.set(rx, y, pz, DAK)
    b.set(px_, py_, pz, mc("gold_block"))                                   # the screw
    # the name in the hair: "KNIP" on a board at the front (to the plein), "VADS" at the back
    bord(h, b, "KNIP", 22, 1)
    bord(h, b, "VADS", 5, -1)


def bord(h, b, text, bz, facing):
    """A board with block letters, stuck in the front (facing 1: south) or back (-1: north) of the hair."""
    mc = h.mc
    bw = text_width(text) + 4
    bx0 = 15 - bw // 2
    by0 = dome_height(15, bz) - 1
    bh = 7
    for x in range(bx0, bx0 + bw):
        for y in range(by0, by0 + bh):
            edge = x in (bx0, bx0 + bw - 1) or y in (by0, by0 + bh - 1)
            b.set(x, y, bz, PAAL[(x + y) % 4] if edge else mc("white_concrete"))
        for y in range(WALL_TOP + 1, by0):                                  # the hair fills up under it
            if b.get(x, y, bz) in (None, "minecraft:air"):
                b.set(x, y, bz, DAK)
    x = 15 - text_width(text) // 2
    for ch in text:
        for r, row in enumerate(FONT[ch]):
            for c, p in enumerate(row):
                if p == "X":
                    lx = x + c if facing > 0 else 2 * 15 - (x + c)
                    b.set(lx, by0 + bh - 2 - r, bz + facing, mc("magenta_concrete"))
        x += len(FONT[ch][0]) + 1


def line(b, x0, y0, x1, y1, z, blk, only_empty=False):
    """A line of blocks from (x0, y0) to (x1, y1) in the plane z, every step face-connected (a staircase, no diagonals)."""
    n = max(abs(x1 - x0), abs(y1 - y0))
    last = None
    for i in range(n + 1):
        x, y = int(round(x0 + (x1 - x0) * i / n)), int(round(y0 + (y1 - y0) * i / n))
        pts = [(x, y)]
        if last and last[0] != x and last[1] != y:
            pts.insert(0, (x, last[1]))
        for (px, py) in pts:
            if not only_empty or b.get(px, py, z) in (None, "minecraft:air"):
                b.set(px, py, z, blk)
        last = (x, y)


def fohn(h, b):
    """A giant föhn on the east side: a sky-blue body with a white grille, the nozzle blowing into the hair, standing on
    its pink handle, with its pink cord over the ground and a golden button."""
    mc = h.mc
    fz, fy = 11, WALL_TOP + 5
    for x in range(W - 5, W):
        for dy in range(-2, 3):
            for dz in range(-2, 3):
                if dy * dy + dz * dz <= 5:
                    b.set(x, fy + dy, fz + dz, mc("white_concrete") if x == W - 1 else mc("light_blue_concrete"))
    b.set(W - 1, fy, fz, mc("gray_concrete"))                               # the grille at the back
    for dy, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        b.set(W - 1, fy + dy, fz + dz, mc("light_gray_concrete"))
    for x in (W - 6, W - 7):                                               # the nozzle, into the hair
        for dy, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            b.set(x, fy + dy, fz + dz, mc("gray_concrete") if x == W - 7 else mc("light_gray_concrete"))
    b.set(W - 3, fy + 2, fz + 1, mc("gold_block"))                         # the button
    for y in range(G + 1, fy - 2):                                          # the handle, down to the ground
        b.set(W - 2, y, fz, mc("pink_concrete"))
        if y < G + 4:
            b.set(W - 2, y, fz + 1, mc("pink_concrete"))
    for z in range(fz + 2, fz + 9):                                         # the cord on the ground
        if b.get(W - 2, G + 1, z) is None:
            b.set(W - 2, G + 1, z, mc("pink_carpet"))


def kam(h, b):
    """A giant comb leaning against the west wall, and a comb stuck in the hair at the back."""
    mc = h.mc
    for z in range(16, 24):
        b.set(1, G + 8, z, mc("white_concrete"))
        if z % 2 == 0:
            for y in range(G + 1, G + 8):
                b.set(1, y, z, mc("white_concrete"))
    for z in range(24, 27):
        b.set(1, G + 8, z, mc("pink_concrete"))
        b.set(1, G + 7, z, mc("pink_concrete"))
    top = max(dome_height(x, 8) for x in range(5, 11)) + 3
    for x in range(5, 11):
        b.set(x, top, 8, mc("white_concrete"))
        if x % 2 == 1:
            for y in range(dome_height(x, 8) - 1, top):
                b.set(x, y, 8, mc("white_concrete"))
    b.set(4, top, 8, mc("pink_concrete"))
    b.set(3, top, 8, mc("pink_concrete"))


def achterkant(h, b, cells):
    """Four giant bottles of haarverf at the back, and a curly tail."""
    mc = h.mc
    for i, (x, glass) in enumerate(((6, "pink"), (10, "lime"), (20, "yellow"), (24, "light_blue"))):
        col = [z for (xx, z) in cells if xx == x]
        z = min(col) - 2
        for y in range(G + 1, G + 4):
            b.set(x, y, z, mc(f"{glass}_stained_glass"))
        b.set(x, G + 4, z, mc("white_concrete"))
        b.set(x, G + 5, z, mc("pink_concrete"))
        b.set(x, G + 6, z, mc("pink_concrete"))
    back = min(z for (x, z) in cells if x == 15)
    for (u, v) in ((0, 1), (0, 2), (1, 3), (2, 3), (3, 2), (3, 1), (2, 0)):
        b.set(13 + u, G + v, back - 1, mc("pink_wool"))


def voortuin(h, b, front_z):
    """Two kapperspalen by the path, lamp posts, flower boxes, a bench."""
    mc = h.mc
    for x in (11, 19):
        z = 27
        b.set(x, G + 1, z, KS)
        for y in range(G + 2, G + 8):
            b.set(x, y, z, PAAL[(y - G) % 4])
        b.set(x, G + 8, z, mc("white_concrete"))
        b.set(x, G + 9, z, LAMP, {"hanging": "false", "waterlogged": "false"})
    for x in (5, 25):
        b.lantaarnpaal(x, 28)
    for x in (8, 9, 21, 22):
        b.bloembak(x, 29)
    b.bank(7, 26, "south")
    b.bank(23, 26, "south")


def extra(b):
    problems = []
    # the jigsaw and the doorway
    js = b.jigsaws
    if len(js) != 1 or js[0][0] != (15, G, 30) or js[0][1] != "guhs:plein_ingang" or js[0][4] != "south_up":
        problems.append(f"jigsaw {js}")
    for x in (14, 15, 16):
        for y in (G + 1, G + 2, G + 3):
            for z in (28, 29, 30):
                if b.get(x, y, z) not in (None, "minecraft:air"):
                    problems.append(f"the entrance is blocked at {(x, y, z)}: {b.get(x, y, z)}")
    # the foundation: y 0..3 solid under everything
    for x in range(W):
        for z in range(D):
            for y in range(G):
                if b.get(x, y, z) in (None, "minecraft:air"):
                    problems.append(f"hole in the foundation at {(x, y, z)}")
    # the chairs: the showstoel is the one nearest to Krulletje (KappersShow.scan), with room to stand in front of it
    stoelen = [p for p, (blk, _, _) in b.s.blocks.items() if blk == STOEL]
    if len(stoelen) < 4:
        problems.append(f"only {len(stoelen)} kappersstoelen")
    nearest = min(stoelen, key=lambda p: (p[0] - KRULLETJE[0]) ** 2 + (p[1] - KRULLETJE[1]) ** 2 + (p[2] - KRULLETJE[2]) ** 2)
    if nearest != SHOWSTOEL:
        problems.append(f"the chair nearest to Krulletje is {nearest}, not the showstoel {SHOWSTOEL}")
    sx, sy, sz = SHOWSTOEL
    for y in (sy + 1, sy + 2):
        if b.get(sx, y, sz) not in (None, "minecraft:air"):
            problems.append(f"no room above the showstoel: {b.get(sx, y, sz)}")
    if sum(1 for (blk, _, _) in b.s.blocks.values() if blk == WASBAK) < 3:
        problems.append("fewer than 3 haarwasbakken")
    if b.gezichten + b.grote_gezichten < 12:
        problems.append(f"only {b.gezichten} small and {b.grote_gezichten} big guh faces")
    if max(p[1] for p in b.s.blocks) >= H:
        problems.append("the building is too high")
    return problems


# =====================================================================================================================
# on its own: python tools/features/kapper_salon.py [out_dir]  (from the project root: the check, and four views as pngs)
# =====================================================================================================================
COLOURS = {"pink": (240, 150, 190), "white": (245, 245, 245), "light_blue": (140, 200, 250), "magenta": (200, 70, 180),
           "knuffelsteen": (230, 170, 190), "pluisdak": (250, 170, 210), "klinkers": (200, 120, 130), "knuffelgras": (250, 190, 220),
           "glass": (190, 230, 255), "black": (30, 30, 40), "iron": (220, 220, 225), "gray": (130, 130, 135), "gold": (250, 210, 60),
           "quartz": (240, 235, 230), "terracotta": (210, 130, 140), "froglight": (255, 250, 220), "cherry": (230, 170, 170),
           "lantern": (255, 200, 90), "dirt": (130, 90, 60), "lime": (140, 230, 100), "yellow": (250, 230, 90), "purple": (140, 60, 170)}


def colour(name):
    for k, c in COLOURS.items():
        if k in name:
            return c
    return (180, 120, 200)


def views(b, out):
    from PIL import Image
    s = b.s
    blocks = {p: n for p, (n, _, _) in s.blocks.items() if n not in ("minecraft:air", "minecraft:jigsaw")}
    scale = 8
    imgs = []
    for name, axis, sign in (("front", 2, 1), ("back", 2, -1), ("east", 0, 1), ("west", 0, -1)):
        im = Image.new("RGB", (W * scale, H * scale), (170, 210, 250))
        for u in range(W):
            for y in range(H):
                rng = range(D - 1, -1, -1) if sign > 0 else range(D)
                for d in rng:
                    p = (u if axis == 2 else d, y, d if axis == 2 else u)
                    if axis == 2 and sign < 0:
                        p = (W - 1 - u, y, d)
                    if axis == 0 and sign > 0:
                        p = (d, y, W - 1 - u)
                    if p in blocks:
                        c = colour(blocks[p])
                        shade = 1.0 - 0.012 * ((D - 1 - d) if sign > 0 else d)
                        for yy in range(scale):
                            for xx in range(scale):
                                im.putpixel((u * scale + xx, (H - 1 - y) * scale + yy), tuple(int(v * shade) for v in c))
                        break
        im.save(os.path.join(out, f"kapper_{name}.png"))
    im = Image.new("RGB", (W * scale, D * scale), (0, 0, 0))
    for x in range(W):
        for z in range(D):
            for y in range(H - 1, -1, -1):
                if (x, y, z) in blocks:
                    c = colour(blocks[(x, y, z)])
                    shade = 0.5 + 0.5 * y / H
                    for yy in range(scale):
                        for xx in range(scale):
                            im.putpixel((x * scale + xx, z * scale + yy), tuple(int(v * shade) for v in c))
                    break
    im.save(os.path.join(out, "kapper_top.png"))
    for level in (G + 1, G + 3):
        im = Image.new("RGB", (W * scale, D * scale), (0, 0, 0))
        for x in range(W):
            for z in range(D):
                n = blocks.get((x, level, z)) or blocks.get((x, level - 1, z))
                c = colour(n) if n else (0, 0, 0)
                if (x, level, z) not in blocks:
                    c = tuple(v // 2 for v in c)
                for yy in range(scale):
                    for xx in range(scale):
                        im.putpixel((x * scale + xx, z * scale + yy), c)
        im.save(os.path.join(out, f"kapper_plan_{level}.png"))


if __name__ == "__main__":
    import os
    import sys
    import types
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    import make_structures as ms
    saved = []
    ms.Structure.save = lambda self, name: saved.append(name)
    stub = types.SimpleNamespace(mc=ms.mc, Structure=ms.Structure, ms=ms)
    bouw = build(stub)
    print("kapper salon:", len(bouw.s.blocks), "blocks,", bouw.walkable, "walkable,", bouw.gezichten, "faces,", bouw.grote_gezichten, "big faces")
    if len(sys.argv) > 1:
        views(bouw, sys.argv[1])
