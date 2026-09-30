"""
Het Knabbelthee-huisje (2.8) - the building in the plein slot "theehuis" of the Knuffeldal town: a GIANT TEAPOT of white
porcelain with painted pink and blue flower bands and golden rims, a spout on the east side that puffs steam (a campfire
in its tip), a big handle on the west side, and a LID that is a pink guh head: a guh face looking at the plein, two round
ears and a kaasknabbel knob on top. Knuffelsteen guh faces are painted all round the belly. Inside: theetafels in the
middle with guh_stoelen round them (the theekransje), Mevrouw Theelepel at the back in front of her counter with
theepotjes, lanterns hanging in the lid. A terrace with tables in front, a tea garden at the back.

Template rules (KNUFFEL_CONTRACT par. 6.3): see creche_bouw.slot_rules. build(h) writes knuffeldal_stadje/theehuis.
Run on its own:  python tools/features/theehuis_bouw.py   (from the project root; saves nothing)
"""
import math
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from features import knuffeldal_stadje as st  # noqa: E402
from features import creche_bouw as cb  # noqa: E402

W, H, D = 31, 34, 31
G = st.G
CX, CZ = 15.0, 13.0
LID_Y = G + 14                    # where the lid starts (its golden lip)
KS, KS_FACE, KLINK, GRAS = st.KS, st.KS_FACE, st.KLINK, st.GRAS
PORS = "minecraft:white_concrete"
PORS2 = "minecraft:white_terracotta"
GOUD = "minecraft:yellow_concrete"
ROZE = "minecraft:pink_concrete"
BLAUW = "minecraft:light_blue_concrete"
FUR = "minecraft:pink_wool"
TAFEL = "guhs:theetafel"
POTJE = "guhs:theepotje"
STOEL = "guhs:guh_stoel"
AROUND = cb.AROUND


def radius(y):
    """The pot's radius at height y (the body, then the lid, which sticks out a little: its lip)."""
    if y <= G:
        return 0.0
    if y < LID_Y:
        return 10.0 * math.sqrt(max(0.0, 1 - ((y - (G + 6)) / 9.5) ** 2))
    return 6.4 * math.sqrt(max(0.0, 1 - ((y - LID_Y) / 8.2) ** 2))


def in_pot(x, y, z):
    r = radius(y)
    return r > 0.4 and math.hypot(x - CX, z - CZ) <= r


def pot_cells():
    shell, inner = set(), set()
    for y in range(G + 1, H):
        for x in range(W):
            for z in range(D):
                if not in_pot(x, y, z):
                    continue
                if all(in_pot(x + dx, y + dy, z + dz) or y + dy <= G for dx, dy, dz in AROUND):
                    inner.add((x, y, z))
                else:
                    shell.add((x, y, z))
    return shell, inner


def outward(x, z):
    return cb.facing_to(CX, CZ, x, z)


def build(h):
    b = st.Bouw(h, (W, H, D), "knuffeldal_stadje/theehuis")
    mc = h.mc
    for x in range(W):
        for z in range(D):
            for y in range(G):
                b.set(x, y, z, KS if y == G - 1 else "minecraft:pink_terracotta")
            b.set(x, G, z, GRAS)
    shell, inner = pot_cells()
    for (x, y, z) in inner:
        if y == G + 1:
            b.set(x, G, z, mc("dark_oak_planks") if (x + z) % 2 else mc("cherry_planks"))
    for (x, y, z) in shell:
        if y == G + 1:
            b.set(x, G, z, KS)
    # --- the porcelain, with bands: gold at the foot and the lip, a pink flower band round the belly, blue dots ---
    for (x, y, z) in shell:
        yy = y - G
        ang = math.atan2(z - CZ, x - CX)
        if y >= LID_Y + 1:
            blk = FUR if y < H - 2 else FUR
        elif y == LID_Y:
            blk = GOUD
        elif yy == 1:
            blk = GOUD
        elif yy in (6, 7, 8):
            k = int(round((ang + math.pi) / (2 * math.pi) * 40))
            blk = ROZE if yy != 7 or k % 4 else "minecraft:magenta_concrete"
            if yy == 7 and k % 4 == 2:
                blk = "minecraft:white_concrete"
        elif yy in (5, 9):
            blk = GOUD if yy == 5 else PORS
        elif yy in (3, 11) and int(round((ang + math.pi) / (2 * math.pi) * 48)) % 3 == 0:
            blk = BLAUW
        else:
            blk = PORS
        b.set(x, y, z, blk)
    # little knuffelsteen guh faces painted round the belly (on the pink band), looking out
    ring = sorted([(x, z) for (x, y, z) in shell if y == G + 7 and all((x + dx, G + 7, z + dz) not in inner for dx, dz in ((0, 0),))
                   and any(not in_pot(x + dx, G + 7, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))],
                  key=lambda p: math.atan2(p[1] - CZ, p[0] - CX))
    for i, (x, z) in enumerate(ring):
        if i % 5 == 0 and not (13 <= x <= 17 and z > CZ):
            b.set(x, G + 7, z, KS_FACE, {"facing": outward(x, z), "stemming": str(i // 5 % 4)})
            b.gezichten += 1
    # --- the lid: a guh head with a face to the plein, two ears, a kaasknabbel knob ---
    blocks = dict(st.FACE_BLOCKS)
    blocks.update({"eye": "minecraft:black_stained_glass", "ring": "minecraft:light_blue_stained_glass",
                   "shine": "minecraft:white_stained_glass", "cheek": "minecraft:magenta_wool", "nose": "minecraft:magenta_concrete",
                   "mouth": "minecraft:purple_wool", "skin": FUR})
    cb.paint_face(b, shell, 15, LID_Y + 4, 5.2, blocks)
    for sx in (-1, 1):
        ex, ey, ez = int(round(CX + sx * 4.2)), LID_Y + 8, 13
        for du in range(-3, 4):
            for dv in range(-3, 4):
                r2 = du * du + dv * dv
                if r2 <= 7:
                    b.set(ex + du, ey + dv, ez, FUR)
                    b.set(ex + du, ey + dv, ez + 1, "minecraft:magenta_terracotta" if r2 <= 3 else FUR)
    lid_top = max(y for (x, y, z) in shell if x == 15 and z == 13)
    b.set(15, lid_top + 1, 13, "guhs:block_of_kaasknabbels")
    b.set(15, lid_top + 2, 13, "guhs:block_of_kaasknabbels")
    b.set(14, lid_top + 1, 13, GOUD)
    b.set(16, lid_top + 1, 13, GOUD)
    # --- the spout (east): a tube from the belly up and out, steaming ---
    path = [(23.0, G + 4.0, 13.0), (25.5, G + 6.0, 13.0), (27.0, G + 8.5, 13.0), (28.0, G + 11.0, 13.0), (28.6, G + 13.0, 13.0)]
    spout = set()
    for i in range(len(path) - 1):
        (x0, y0, z0), (x1, y1, z1) = path[i], path[i + 1]
        steps = 8
        for t in range(steps + 1):
            px, py, pz = x0 + (x1 - x0) * t / steps, y0 + (y1 - y0) * t / steps, z0 + (z1 - z0) * t / steps
            r = 1.7 - 0.5 * (i + t / steps) / (len(path) - 1)
            for x in range(int(px - 2), int(px + 3)):
                for y in range(int(py - 2), int(py + 3)):
                    for z in range(int(pz - 2), int(pz + 3)):
                        if (x - px) ** 2 + (y - py) ** 2 + (z - pz) ** 2 <= r * r and 0 <= x < W and (x, y, z) not in inner:
                            spout.add((x, y, z))
    for (x, y, z) in spout:
        b.set(x, y, z, GOUD if y in (G + 5,) else PORS)
    tip = max(spout, key=lambda p: (p[1], p[0]))
    b.set(tip[0], tip[1] + 1, tip[2], "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    # --- the handle (west): a big half ring ---
    for x in range(0, 8):
        for y in range(G + 2, G + 15):
            d = math.hypot(x - 4.2, (y - (G + 8)) * 0.95)
            if 3.4 <= d <= 5.0 and x <= 5:
                for z in (12, 13, 14):
                    if (x, y, z) not in inner:
                        b.set(x, y, z, GOUD if z == 13 and d > 4.3 else PORS)
    for y in (G + 4, G + 5, G + 11, G + 12):                      # (the ends go into the pot)
        for x in range(4, 8):
            for z in (12, 13, 14):
                if (x, y, z) not in inner and b.get(x, y, z) is None and math.hypot(x - 4.2, (y - (G + 8)) * 0.95) <= 5.2:
                    b.set(x, y, z, PORS)
    # --- the door (front) with a little gold frame and an awning; round flower windows ---
    fz = max(z for (x, y, z) in shell if x == 15 and y == G + 2)
    for x in (14, 15, 16):
        for y in (G + 1, G + 2, G + 3):
            for z in range(int(CZ), D):
                if (x, y, z) in shell:
                    b.air(x, y, z, x, y, z)
    for x in (13, 17):
        for y in (G + 1, G + 2, G + 3, G + 4):
            z = max(zz for (xx, yy, zz) in shell if xx == x and yy == y)
            b.set(x, y, z, GOUD)
    for x in (13, 14, 15, 16, 17):
        z = max(zz for (xx, yy, zz) in shell if xx == x and yy == G + 4)
        b.set(x, G + 4, z, GOUD)
        b.set(x, G + 5, z + 1, "guhs:pluisdak_plaat", {"type": "bottom", "waterlogged": "false"})
    b.set(15, G + 4, fz + 1, st.LAMP, {"hanging": "true", "waterlogged": "false"})
    for (wx, wy) in ((9, G + 10), (21, G + 10), (6, G + 4), (24, G + 4)):
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                d = math.hypot(dx, dy)
                if d > 2.3:
                    continue
                x, y = wx + dx, wy + dy
                zs = [zz for (xx, yy, zz) in shell if xx == x and yy == y and zz > CZ]
                if not zs:
                    continue
                z = max(zs)
                glas = d <= 1.3
                b.set(x, y, z, "minecraft:light_blue_stained_glass" if glas else ROZE)
                if glas and (x, y, z - 1) in shell:
                    b.set(x, y, z - 1, "minecraft:light_blue_stained_glass")
    # the back: round windows too, and a big painted flower
    for (wx, wy) in ((10, G + 4), (20, G + 4)):
        for dx in range(-1, 2):
            for dy in range(-1, 2):
                x, y = wx + dx, wy + dy
                zs = [zz for (xx, yy, zz) in shell if xx == x and yy == y and zz < CZ]
                if zs:
                    b.set(x, y, min(zs), "minecraft:light_blue_stained_glass")
                    if (x, y, min(zs) + 1) in shell:
                        b.set(x, y, min(zs) + 1, "minecraft:light_blue_stained_glass")
    flower = ["..X.X..", ".XXOXX.", "XXOYOXX", ".XXOXX.", "..X.X.."]
    for row, line in enumerate(flower):
        for col, ch in enumerate(line):
            if ch == ".":
                continue
            x, y = 12 + col, G + 12 - row
            zs = [zz for (xx, yy, zz) in shell if xx == x and yy == y and zz < CZ]
            if zs:
                b.set(x, y, min(zs), {"X": ROZE, "O": "minecraft:magenta_concrete", "Y": GOUD}[ch])
    # --- inside: the tea tables and chairs, Mevrouw Theelepel and her counter, the lanterns ---
    for x in (14, 15, 16):
        for z in (12, 13):
            b.set(x, G + 1, z, TAFEL, {"gedekt": "false"})
    chairs = [((x, 11), "south") for x in (14, 15, 16)] + [((x, 14), "north") for x in (14, 15, 16)] + \
             [((13, z), "east") for z in (12, 13)] + [((17, z), "west") for z in (12, 13)]
    for (x, z), f in chairs:
        b.set(x, G + 1, z, STOEL, {"facing": f})
    for x in range(10, 21):
        for z in (10, 15):
            if b.get(x, G + 1, z) is None and (x, G + 1, z) in inner and abs(x - 15) <= 4:
                b.set(x, G + 1, z, mc("pink_carpet"))
    back = min(z for (x, y, z) in inner if x == 15 and y == G + 1)
    for x in range(12, 19):
        b.set(x, G + 1, back, KS)
        b.set(x, G + 2, back, "guhs:knuffelsteen_plaat", {"type": "bottom", "waterlogged": "false"})
    for x in (12, 15, 18):
        b.set(x, G + 3, back, POTJE, {"facing": "south"})
    b.set(13, G + 3, back, mc("flower_pot"))
    b.set(17, G + 3, back, mc("potted_pink_tulip"))
    b.npc(15, G + 1, back + 2, "theeguh", "south")
    for (x, z) in ((10, 17), (20, 17)):
        if (x, G + 1, z) in inner:
            b.set(x, G + 1, z, "guhs:pink_zitzak", {"facing": "north"})
    for (lx, lz, low) in ((15, 13, G + 12), (11, 11, G + 10), (19, 11, G + 10), (11, 16, G + 10), (19, 16, G + 10)):
        tops = [y for (x, y, z) in shell if x == lx and z == lz and y > G + 8]
        if not tops:
            continue
        ceil = min(tops)
        for y in range(low + 1, ceil):
            b.set(lx, y, lz, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        b.set(lx, low, lz, st.LAMP, {"hanging": "true", "waterlogged": "false"})
    # --- the terrace in front, and the gardens ---
    for x in range(W):
        for z in range(D):
            if b.get(x, G, z) == GRAS and z >= 22 and 11 <= x <= 19:
                b.set(x, G, z, KLINK)
    for (tx, tz) in ((5, 26), (25, 26)):
        b.set(tx, G + 1, tz, "guhs:guh_tafel", {"facing": "south"})
        b.set(tx - 1, G + 1, tz, STOEL, {"facing": "east"})
        b.set(tx + 1, G + 1, tz, STOEL, {"facing": "west"})
        b.set(tx, G + 2, tz, POTJE, {"facing": "south"})
        for (dx, dz) in ((-1, -1), (0, -1), (1, -1), (-1, 1), (0, 1), (1, 1), (-2, 0), (2, 0)):
            if b.get(tx + dx, G, tz + dz) == GRAS:
                b.set(tx + dx, G, tz + dz, KLINK)
    b.lantaarnpaal(10, 28)
    b.lantaarnpaal(20, 28)
    for x in list(range(1, 10)) + list(range(21, 30)):
        if x % 2:
            b.bloembak(x, 30)
    # the tea garden at the back: little tea bushes (azaleas), a bench, a lamp post
    for (x, z) in ((3, 2), (6, 1), (9, 2), (21, 2), (24, 1), (27, 2), (2, 6), (28, 6)):
        if b.get(x, G + 1, z) is None:
            b.set(x, G + 1, z, "minecraft:flowering_azalea" if (x + z) % 2 else "minecraft:azalea")
    b.bank(15, 1, "south")
    b.lantaarnpaal(1, 18)
    b.lantaarnpaal(29, 20)
    b.grasveld(0, 0, W - 1, D - 1, 0.14)
    b.starts.append((15, G + 1, D - 2))
    b.jigsaw(15, G, D - 1, "south_up", "guhs:plein_ingang", "minecraft:empty", "minecraft:empty", KLINK)
    for x in (14, 15, 16):
        for y in (G + 1, G + 2, G + 3):
            for z in (D - 3, D - 2, D - 1):
                b.air(x, y, z, x, y, z)
    b.s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    return b


def check_theehuis(b):
    problems = cb.slot_rules(b, "theehuis", "theeguh")
    tafels = [p for p, (blk, _, _) in b.s.blocks.items() if blk == TAFEL]
    stoelen = [p for p, (blk, _, _) in b.s.blocks.items() if blk == STOEL and any(
        b.get(p[0] + dx, p[1], p[2] + dz) == TAFEL for dx in (-2, -1, 0, 1, 2) for dz in (-2, -1, 0, 1, 2))]
    if len(tafels) < 4 or len(stoelen) < 6:
        problems.append(f"theehuis: {len(tafels)} tea tables, {len(stoelen)} chairs at them")
    for (x, y, z) in stoelen:
        if b.get(x, y + 1, z) not in (None, "minecraft:air"):
            problems.append(f"theehuis: no room above the chair at {(x, y, z)}")
    if not any(blk == "minecraft:campfire" for (blk, _, _) in b.s.blocks.values()):
        problems.append("theehuis: the spout doesn't steam")
    return problems


def build_and_check(h, save=True):
    b = build(h)
    problems = st.check(b, check_theehuis)
    if problems:
        raise SystemExit("theehuis geometry check failed:\n  " + "\n  ".join(problems[:60]))
    if save:
        b.s.save("knuffeldal_stadje/theehuis")
    return b


if __name__ == "__main__":
    import types
    import make_structures as ms
    stub = types.SimpleNamespace(mc=ms.mc, Structure=ms.Structure, ms=ms)
    bb = build(stub)
    found = st.check(bb, check_theehuis)
    print("theehuis:", len(bb.s.blocks), "blocks,", bb.walkable, "walkable,", bb.gezichten, "faces,", bb.grote_gezichten, "big faces")
    print("\n".join(found[:80]) if found else "geometry check ok")
