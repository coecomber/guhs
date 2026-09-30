"""
De Knuffelcreche (2.8) - the building in the plein slot "creche" of the Knuffeldal town: a GIANT BABY GUH sitting on the
ground, with a pacifier (speen) in its mouth, a nappy (luier) round its bottom with safety pins, two little feet in front,
stubby arms holding a rattle and a tiny guh plushie, two round ears and a curl on its head. The eyes are windows, the
door is between its feet under the pacifier. Inside: a ring of guh_wiegjes around a speelkleed, Juf Knuffel at the back,
a mobile of lanterns hanging from the dome. Gardens all round: a stroller, a sandbox, a slide, flower boxes, lamp posts
and a border of knuffelsteen faces.

Template rules (KNUFFEL_CONTRACT par. 6.3): 31 x H x 31, layers y 0-3 foundation, street level y = 4, ONE jigsaw at
(15, 4, 30) south_up "guhs:plein_ingang"; the front (z = 30) faces the plein. build(h) writes
knuffeldal_stadje/creche; check() (knuffeldal_stadje.check + the slot rules) raises SystemExit on problems.
Run on its own:  python tools/features/creche_bouw.py   (from the project root; saves nothing)
"""
import math
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from features import knuffeldal_stadje as st  # noqa: E402

W, H, D = 31, 40, 31
G = st.G
CX, CZ = 15.0, 13.0          # the body's middle (x, z)
RX, RZ = 12.6, 11.2          # its widest radii (at yy = YC)
YC, YR = 8.0, 13.2           # vertical profile: radius factor sqrt(1 - ((yy - YC) / YR)^2), yy = y - G

KS, KS_FACE, KS_MUUR = st.KS, st.KS_FACE, st.KS_MUUR
KLINK, GRAS = st.KLINK, st.GRAS
WIEG = "guhs:guh_wiegje"
KLEED = "guhs:speelkleed"
FUR = "minecraft:pink_wool"
FLUF = "guhs:pluisdak"
LUIER = "minecraft:white_wool"
LUIER_STIP = "minecraft:light_blue_wool"
SPEEN = "minecraft:light_blue_concrete"
SPEEN_RAND = "minecraft:white_concrete"
RING = "minecraft:yellow_concrete"
BLOS = "minecraft:magenta_wool"


def k(yy):
    return math.sqrt(max(0.0, 1.0 - ((yy - YC) / YR) ** 2))


def in_body(x, y, z, shrink=0.0):
    yy = y - G
    if yy < 1:
        return False
    f = k(yy)
    rx, rz = RX * f - shrink, RZ * f - shrink
    if rx <= 0.3 or rz <= 0.3:
        return False
    return ((x - CX) / rx) ** 2 + ((z - CZ) / rz) ** 2 <= 1.0


AROUND = [(dx, dy, dz) for dx in (-1, 0, 1) for dy in (-1, 0, 1) for dz in (-1, 0, 1) if (dx, dy, dz) != (0, 0, 0)]


def body_cells():
    """The body: shell (any of the 26 neighbours is outside: a closed, face-connected wall) and inside (air)."""
    shell, inner = set(), set()
    for y in range(G + 1, G + 24):
        for x in range(W):
            for z in range(D):
                if not in_body(x, y, z):
                    continue
                if all(in_body(x + dx, y + dy, z + dz) or y + dy <= G for dx, dy, dz in AROUND):
                    inner.add((x, y, z))
                else:
                    shell.add((x, y, z))
    return shell, inner


def front_z(shell, x, y):
    zs = [z for (xx, yy, z) in shell if xx == x and yy == y]
    return max(zs) if zs else None


def paint_face(b, shell, cu, cy, R, blocks, skip=None):
    """A guh face looking south (+z) on the round body: every face block lands on the frontmost shell block of its
    column; the eyes are windows (the glass goes one deeper where the wall is two thick)."""
    n = int(R + 1)
    for du in range(-n, n + 1):
        x = cu - du                                  # the face's right is west
        for dv in range(-n, n + 1):
            role = st.face_role(du, dv, R)
            if role is None or role == "skin":
                continue
            y = cy + dv
            z = front_z(shell, x, y)
            if z is None or (skip and skip(x, y, z, role)):
                continue
            b.set(x, y, z, blocks[role])
            if role in st.WINDOW_ROLES and (x, y, z - 1) in shell:
                b.set(x, y, z - 1, blocks[role])
    b.grote_gezichten += 1


def ellipsoid(b, c, r, block, only_empty=False, pick=None):
    cx, cy, cz = c
    rx, ry, rz = r
    out = []
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
            for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 <= 1.0 and 0 <= x < W and 0 <= z < D and y > G - 1:
                    if only_empty and b.get(x, y, z) not in (None, "minecraft:air"):
                        continue
                    b.set(x, y, z, pick(x, y, z) if pick else block)
                    out.append((x, y, z))
    return out


def crib_spots(inner):
    """Spots for the cribs: round the inside wall of the ground floor, not in front of the door or the Juf."""
    floor = {(x, z) for (x, y, z) in inner if y == G + 1}
    k1 = k(1)
    rxi, rzi = RX * k1 - 1.0, RZ * k1 - 1.0
    spots = []
    for deg in range(0, 360, 24):
        a = math.radians(deg)
        s = math.sin(a)
        if s > 0.8 or s < -0.93:                     # the door (front, +z) and the Juf (back)
            continue
        x = int(round(CX + (rxi - 1.4) * math.cos(a)))
        z = int(round(CZ + (rzi - 1.4) * s))
        if (x, z) in floor and all(math.dist((x, z), p) >= 2.0 for p in spots):
            spots.append((x, z))
    return spots


def facing_to(x, z, tx, tz):
    dx, dz = tx - x, tz - z
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def build(h):
    """Builds the creche; returns the Bouw (not saved)."""
    b = st.Bouw(h, (W, H, D), "knuffeldal_stadje/creche")
    mc = h.mc
    # --- foundation and ground ---
    for x in range(W):
        for z in range(D):
            for y in range(G):
                b.set(x, y, z, KS if y == G - 1 else "minecraft:pink_terracotta")
            b.set(x, G, z, GRAS)
    shell, inner = body_cells()
    # the floor inside (cherry planks), under the body knuffelsteen
    for (x, y, z) in inner:
        if y == G + 1:
            b.set(x, G, z, mc("cherry_planks"))
    for (x, y, z) in shell:
        if y == G + 1:
            b.set(x, G, z, KS)
    # --- the body: nappy round the bottom, pink fur, fluffy top ---
    for (x, y, z) in shell:
        yy = y - G
        if yy == 4:
            blk = LUIER_STIP                                   # the nappy's waistband
        elif yy <= 3:
            blk = LUIER_STIP if (x + 2 * y + z) % 7 == 0 else LUIER
        elif yy >= 17:
            blk = FLUF
        else:
            blk = FUR
        b.set(x, y, z, blk)
    # the nappy's safety pins on both sides
    for sx in (-1, 1):
        x = int(round(CX + sx * (RX * k(3) - 0.3)))
        col = [(xx, yy, zz) for (xx, yy, zz) in shell if xx == x and yy == G + 3]
        if col:
            zmid = sorted(col, key=lambda p: abs(p[2] - CZ))[0][2]
            for dz in (-1, 0, 1):
                b.set(x, G + 3, zmid + dz, "minecraft:light_blue_concrete")
            b.set(x, G + 3, zmid + 2, "minecraft:yellow_concrete")
            b.set(x, G + 4, zmid - 1, "minecraft:light_blue_concrete")
    # --- the face (south): the eyes are windows ---
    blocks = dict(st.FACE_BLOCKS)
    blocks.update({"eye": "minecraft:black_stained_glass", "ring": "minecraft:light_blue_stained_glass",
                   "shine": "minecraft:white_stained_glass", "cheek": BLOS, "nose": "minecraft:magenta_concrete",
                   "mouth": "minecraft:purple_wool", "skin": FUR})
    paint_face(b, shell, 15, G + 13, 8.4, blocks)
    # --- the pacifier: a flat shield over the mouth (filled back to the body), and its ring in front ---
    sy = G + 8
    zs = max(front_z(shell, x, y) for x in range(10, 21) for y in range(sy - 3, sy + 4) if front_z(shell, x, y) is not None) + 1
    for x in range(9, 22):
        for y in range(sy - 3, sy + 4):
            e = ((x - 15) / 5.6) ** 2 + ((y - sy) / 2.9) ** 2
            if e <= 1.0:
                fz = front_z(shell, x, y)
                if fz is None:
                    continue
                for z in range(fz + 1, zs):
                    b.set(x, y, z, SPEEN)
                b.set(x, y, zs, SPEEN_RAND if e > 0.72 else SPEEN)
    for x in range(9, 22):
        for y in range(sy - 5, sy + 6):
            d = math.hypot(x - 15, y - sy)
            if 3.3 <= d <= 4.7 and y > G + 3:
                b.set(x, y, zs + 1, RING)
    b.set(15, sy, zs + 1, "minecraft:pink_concrete")              # the knob in the middle
    # --- the feet in front, with pink toe beans ---
    for fx in (9.5, 20.5):
        ellipsoid(b, (fx, G + 1.2, 22.8), (2.6, 1.7, 3.3), FUR)
        tz = max(z for (x, y, z) in [(int(fx), G + 1, zz) for zz in range(D)] if b.get(int(fx), G + 1, z) == FUR)
        for tx in (int(fx) - 1, int(fx), int(fx) + 1):
            b.set(tx, G + 2, tz, "minecraft:pink_terracotta")
        b.set(int(fx), G + 1, tz, "minecraft:pink_terracotta")
    # --- the arms: the east one holds a rattle, the west one a tiny guh plushie ---
    ellipsoid(b, (2.4, G + 7, 15), (2.3, 2.6, 3.0), FUR)
    ellipsoid(b, (27.6, G + 7, 15), (2.3, 2.6, 3.0), FUR)
    for hx, side in ((0, -1), (30, 1)):                         # pink paw pads
        for (dy, dz) in ((0, 0), (-1, -1), (-1, 1)):
            if b.get(hx - side, G + 7 + dy, 15 + dz) == FUR or b.get(hx, G + 7 + dy, 15 + dz) == FUR:
                x = hx if b.get(hx, G + 7 + dy, 15 + dz) == FUR else hx - side
                b.set(x, G + 7 + dy, 15 + dz, "minecraft:pink_terracotta")
    for y in range(G + 8, G + 12):
        b.set(28, y, 16, "minecraft:white_concrete")
    ellipsoid(b, (28.0, G + 14.0, 16.0), (2.0, 2.0, 2.0), "minecraft:yellow_concrete",
              pick=lambda x, y, z: "minecraft:pink_concrete" if (x + y + z) % 3 == 0 else "minecraft:yellow_concrete")
    b.set(1, G + 9, 15, KS_FACE, {"facing": "west", "stemming": "0"})       # the plushie's face
    for z in (14, 15, 16):
        b.set(1, G + 10, z, "minecraft:pink_wool")                   # its ears
    b.gezichten += 1
    # --- the ears on the dome, and a curl on top ---
    for sx in (-1, 1):
        ex, ey, ez = int(round(CX + sx * 6.6)), G + 19, 12
        for du in range(-4, 5):
            for dv in range(-4, 5):
                r2 = du * du + dv * dv
                if r2 <= 11:
                    b.set(ex + du, ey + dv, ez, FUR)
                    b.set(ex + du, ey + dv, ez + 1, "minecraft:magenta_terracotta" if r2 <= 5 else FUR)
    top = max(y for (x, y, z) in shell if x == 15 and z == 13)
    for (dx, dy) in ((0, 1), (0, 2), (1, 3), (2, 3), (2, 2), (1, 2)):
        b.set(15 + dx, top + dy, 13, "minecraft:pink_concrete")
    # --- windows: round portholes on the sides, a heart window in the back ---
    for sx in (-1, 1):
        for dz in range(-3, 4):
            for dy in range(-3, 4):
                d = math.hypot(dz, dy * 1.1)
                if d > 3.2:
                    continue
                y, z = G + 11 + dy, int(CZ) - 3 + dz
                xs = [x for (x, yy, zz) in shell if yy == y and zz == z]
                if xs:
                    x = min(xs) if sx < 0 else max(xs)
                    glas = d <= 2.1
                    b.set(x, y, z, "minecraft:pink_stained_glass" if glas else "minecraft:white_concrete")
                    if glas and (x - sx, y, z) in shell:
                        b.set(x - sx, y, z, "minecraft:pink_stained_glass")
    heart = [".XXX.XXX.", "XXXXXXXXX", "XXXXXXXXX", ".XXXXXXX.", "..XXXXX..", "...XXX...", "....X...."]
    for row, line in enumerate(heart):
        for col, ch in enumerate(line):
            if ch == "X":
                x, y = 11 + col, G + 12 - row
                zs_ = [z for (xx, yy, z) in shell if xx == x and yy == y]
                if zs_:
                    z = min(zs_)
                    b.set(x, y, z, "minecraft:magenta_stained_glass")
                    if (x, y, z + 1) in shell:
                        b.set(x, y, z + 1, "minecraft:magenta_stained_glass")
    # a big light blue bow on the back of the head, and a big safety pin on the nappy's back
    bow = ["XX.....XX", "XXX...XXX", "XXXXOXXXX", "XXX...XXX", "XX.....XX"]
    for row, line in enumerate(bow):
        for col, ch in enumerate(line):
            if ch != ".":
                x, y = 11 + col, G + 19 - row
                zs_ = [z for (xx, yy, z) in shell if xx == x and yy == y]
                if zs_:
                    z = min(zs_) - 1
                    b.set(x, y, z, "minecraft:light_blue_concrete" if ch == "X" else "minecraft:white_concrete")
                    for zz in range(z + 1, min(zs_)):
                        b.set(x, y, zz, "minecraft:light_blue_concrete")
    for i, x in enumerate(range(10, 21)):
        zs_ = [z for (xx, yy, z) in shell if xx == x and yy == G + 3]
        if zs_:
            b.set(x, G + 3, min(zs_), "minecraft:yellow_concrete" if x in (10, 20) else "minecraft:light_blue_concrete")
    # the curly tail at the back, over the nappy
    back = min(z for (x, y, z) in shell if x == 15 and y == G + 6)
    for (u, v) in ((0, 0), (0, 1), (1, 2), (2, 2), (3, 1), (3, 0), (2, -1)):
        x, y = 15 + u, G + 6 + v
        b.set(x, y, back - 1, FUR)
        for z in range(back, int(CZ)):                     # (the tail sits against the round back)
            if (x, y, z) in shell:
                break
            b.set(x, y, z, FUR)
    # --- the door: an archway between the feet, under the pacifier ---
    for x in (14, 15, 16):
        for y in (G + 1, G + 2, G + 3):
            for z in range(int(CZ), D):
                if (x, y, z) in shell or b.get(x, y, z) == FUR:
                    b.air(x, y, z, x, y, z)
    # --- inside: the cribs round the wall, the play mat, the Juf, the mobile ---
    spots = crib_spots(inner)
    for (x, z) in spots:
        b.set(x, G + 1, z, WIEG, {"facing": facing_to(x, z, CX, CZ + 2), "baby": "ingestopt", "wens": "geen"})
    for x in range(12, 19):
        for z in range(10, 17):
            if (x, G + 1, z) in inner and b.get(x, G + 1, z) is None:
                b.set(x, G + 1, z, KLEED)
    juf_z = min(z for (x, y, z) in inner if x == 15 and y == G + 1) + 1
    b.npc(15, G + 1, juf_z, "juf_knuffel", "south")
    b.set(13, G + 1, juf_z - 1, "guhs:guh_stoel", {"facing": "south"})
    b.set(17, G + 1, juf_z - 1, "guhs:guh_kast", {"facing": "south", "open": "false"})
    b.set(18, G + 1, juf_z, "guhs:pink_zitzak", {"facing": "south"})
    b.set(12, G + 1, juf_z, "guhs:pink_zitzak", {"facing": "south"})
    # the mobile: chains from the dome with lanterns and glowing stars
    for (mx, mz, low) in ((15, 13, G + 11), (11, 11, G + 12), (19, 11, G + 12), (11, 16, G + 12), (19, 16, G + 12)):
        tops = [y for (x, y, z) in shell if x == mx and z == mz and y > G + 10]
        if not tops:
            continue
        ceil = min(tops)
        for y in range(low + 1, ceil):
            b.set(mx, y, mz, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        b.set(mx, low, mz, st.LAMP, {"hanging": "true", "waterlogged": "false"})
    # --- the gardens ---
    for z in range(D):
        for x in (13, 14, 15, 16, 17):
            if b.get(x, G + 1, z) is None and z >= 20 and (x, G + 1, z) not in inner:
                if b.get(x, G, z) == GRAS:
                    b.set(x, G, z, KLINK)
    b.starts.append((15, G + 1, D - 2))
    b.lantaarnpaal(11, 28)
    b.lantaarnpaal(19, 28)
    for x in list(range(1, 11)) + list(range(20, 30)):
        b.set(x, G + 1, 29, KS_MUUR, {"up": "true" if x in (1, 5, 10, 20, 25, 29) else "false", "east": "low", "west": "low",
                                     "north": "none", "south": "none", "waterlogged": "false"})
    # a border round the body: flower boxes and little knuffelsteen faces looking out
    ring = []
    for x in range(W):
        for z in range(D):
            if (x, G + 1, z) in shell or b.get(x, G + 1, z) is not None:
                continue
            if any((x + dx, G + 1, z + dz) in shell for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))) and not (13 <= x <= 17 and z > 18):
                ring.append((x, z))
    ring.sort(key=lambda p: math.atan2(p[1] - CZ, p[0] - CX))
    for i, (x, z) in enumerate(ring):
        if b.get(x, G, z) != GRAS:
            continue
        if i % 6 == 0:
            b.set(x, G + 1, z, KS_FACE, {"facing": facing_to(CX, CZ, x, z), "stemming": str(i // 6 % 4)})
            b.gezichten += 1
        elif i % 3 == 0:
            b.bloembak(x, z)
    # the stroller (front left): a pink tub on wheels with a light blue hood
    for x in range(3, 7):
        for z in (25, 26):
            b.set(x, G + 2, z, "minecraft:pink_concrete")
    for x in (3, 6):
        for z in (25, 26):
            b.set(x, G + 1, z, "minecraft:black_concrete")
    for z in (25, 26):
        b.set(3, G + 3, z, "minecraft:light_blue_wool")
        b.set(4, G + 3, z, "minecraft:light_blue_wool")
    b.set(7, G + 3, 25, "minecraft:white_concrete")
    b.set(7, G + 2, 25, "minecraft:white_concrete")
    # the sandbox (back left) and a little slide (back right)
    for x in range(2, 7):
        for z in range(1, 5):
            edge = x in (2, 6) or z in (1, 4)
            b.set(x, G, z, KS if edge else mc("sand"))
            if edge:
                b.set(x, G + 1, z, st.KS_PLAAT, {"type": "bottom", "waterlogged": "false"})
    b.set(4, G + 1, 2, mc("sand"))
    b.set(4, G + 2, 2, "minecraft:pink_concrete")                 # a sand guh (with a pink bucket)
    for (x, y, z) in ((24, G + 1, 2), (24, G + 2, 2), (24, G + 3, 2)):
        b.set(x, y, z, "minecraft:light_blue_concrete")
    b.set(25, G + 3, 2, "minecraft:light_blue_concrete")
    b.set(26, G + 3, 2, "minecraft:light_blue_concrete")
    for x, y in ((26, G + 3), (27, G + 2), (28, G + 1), (29, G + 1)):
        b.set(x, y, 3, "minecraft:yellow_concrete")
        for yy in range(G + 1, y):
            b.set(x, yy, 3, "minecraft:white_concrete")
    b.bank(8, 3, "south")
    b.boompje(1, 22, 4, 2.2)
    b.boompje(29, 23, 4, 2.2)
    b.grasveld(0, 0, W - 1, D - 1, 0.16)
    # the entrance on the plein
    b.jigsaw(15, G, D - 1, "south_up", "guhs:plein_ingang", "minecraft:empty", "minecraft:empty", KLINK)
    for x in (14, 15, 16):
        for y in (G + 1, G + 2, G + 3):
            for z in (D - 3, D - 2, D - 1):
                b.air(x, y, z, x, y, z)
    b.s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    b.wiegjes = spots
    return b


def slot_rules(b, name, npc_kind, min_extra=None):
    """The rules of a plein slot building (contract par. 6.3), shared with the tea house."""
    problems = []
    s = b.s
    if tuple(s.size) != (31, s.size[1], 31) or not (16 <= s.size[1] <= 48):
        problems.append(f"{name}: size {s.size}")
    js = b.jigsaws
    if len(js) != 1 or js[0][0] != (15, G, 30) or js[0][4] != "south_up" or js[0][1] != "guhs:plein_ingang" or js[0][2] != "minecraft:empty" \
            or js[0][3] != "minecraft:empty":
        problems.append(f"{name}: jigsaws {js}")
    for x in range(31):
        for z in range(31):
            for y in range(G):
                if not st.solid(s.get(x, y, z)):
                    problems.append(f"{name}: no foundation at {(x, y, z)}")
    for x in (14, 15, 16):
        for y in (G + 1, G + 2, G + 3):
            for z in (28, 29, 30):
                if s.get(x, y, z) not in (None, "minecraft:air"):
                    problems.append(f"{name}: the entrance is blocked at {(x, y, z)} ({s.get(x, y, z)})")
    kinds = [e[3]["Kind"] for e in s.entities if e[3]["id"] == "guhs:guh_npc"]
    if kinds != [npc_kind]:
        problems.append(f"{name}: NPCs {kinds}")
    for (x, y, z), (blk, _, _) in s.blocks.items():
        if blk != "minecraft:air" and not (0 <= x < 31 and 0 <= y < s.size[1] and 0 <= z < 31):
            problems.append(f"{name}: {blk} outside the template at {(x, y, z)}")
    # big and guhig: lots of blocks above the street, guh faces, and it looks different from all four sides
    above = sum(1 for (x, y, z), (blk, _, _) in s.blocks.items() if y > G and blk != "minecraft:air")
    if above < 1500:
        problems.append(f"{name}: only {above} blocks above the street: too small")
    if b.gezichten + b.grote_gezichten < 6:
        problems.append(f"{name}: only {b.gezichten} small and {b.grote_gezichten} big guh faces")
    top = max(y for (x, y, z), (blk, _, _) in s.blocks.items() if blk != "minecraft:air")
    if top < G + 16:
        problems.append(f"{name}: only {top - G} high")
    if min_extra:
        problems += min_extra(b)
    return problems


def check_creche(b):
    problems = slot_rules(b, "creche", "juf_knuffel")
    cribs = [(p, props) for p, (blk, props, _) in b.s.blocks.items() if blk == WIEG]
    if len(cribs) < 8:
        problems.append(f"creche: only {len(cribs)} cribs")
    for (x, y, z), props in cribs:
        dx, dz = st.FACING[props["facing"]]
        if not st.passable(b.get(x + dx, y, z + dz)) or not st.solid(b.get(x + dx, y - 1, z + dz)):
            problems.append(f"creche: the crib at {(x, y, z)} has no room in front of it for a baby to crawl out")
        b.targets[f"wiegje {x},{z}"] = (x + dx, y, z + dz)
    return problems


def build_and_check(h, save=True):
    b = build(h)
    problems = st.check(b, check_creche)
    if problems:
        raise SystemExit("creche geometry check failed:\n  " + "\n  ".join(problems[:60]))
    if save:
        b.s.save("knuffeldal_stadje/creche")
    return b


if __name__ == "__main__":
    import types
    import make_structures as ms
    stub = types.SimpleNamespace(mc=ms.mc, Structure=ms.Structure, ms=ms)
    bb = build(stub)
    found = st.check(bb, check_creche)
    print("creche:", len(bb.s.blocks), "blocks,", bb.walkable, "walkable,", bb.gezichten, "faces,", bb.grote_gezichten, "big faces,",
          len(bb.wiegjes), "cribs")
    print("\n".join(found[:80]) if found else "geometry check ok")
