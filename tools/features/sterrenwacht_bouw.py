"""
De Guh-Sterrenwacht (2.8, slice "buiten") - the building (template guh_sterrenwacht, 48 x 64 x 48), high on the
Guhpieken and the Vadskliffen:

  - a round terrace (knuffelklinkers, a knuffelsteen parapet with sterrenlantaarns) on a thick plate that stands on
    twelve deep legs (each with a guh face peeking out under the terrace) and a core: it can stand on a slope
  - a round tower with star windows and eight guh faces all round, a door on the south side
  - on top a big white-and-blue DOME that is a guh's head: two huge round ears, a guh face on its south side
  - the BIG telescope: a light-blue tube with golden rings sticking far out of a slit in the dome, with little guh
    ears at its lens; its eyepiece (a guh_telescoop) at the bottom, inside
  - inside: a star-map floor (De Grote Knabbel in yellow stars), bookshelves, a desk, sterrenlantaarns, two more
    telescopes, a chest, and Professor Sterretje; outside four little telescopes on the terrace, benches, flower boxes

check(s) is the geometry self-check (SystemExit on problems): nothing floats, every telescope and the Professor can be
walked to from the stairs at the edge, the Professor stands on a floor, the jigsaw is there.
Run it alone:  python tools/features/sterrenwacht_bouw.py   (from the project root)
"""
import math
import os
import random
import sys

if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

from features import sterrenwacht_hulp as hulp  # noqa: E402

NAME = "guh_sterrenwacht"
W, H, D = 48, 64, 48
G = 24                          # the terrace floor (the plate and the legs are below it)
C = 24                          # the middle (x and z)
ANCHOR = "guhs:guh_sterrenwacht_midden"
TERRAS = 20.5
TOREN = 11.5
KOEPEL_Y = G + 12
KOEPEL = 11.5
KS = "guhs:knuffelsteen"
KLINK = "guhs:knuffelklinkers"
TELESCOOP = "guhs:guh_telescoop"
LANTAARN = "guhs:sterrenlantaarn"

# the big telescope: from its eyepiece end up through the dome's slit
BUIS_VAN = (C + 0.5, G + 3.5, C + 2.5)
BUIS_RICHTING = (0.35, 0.75, -0.55)
BUIS_LENGTE = 32
BUIS_R = 1.8
EYEPIECE = (C, G + 1, C + 4)          # the guh_telescoop under the big one
NPC = (C - 4, G + 1, C + 3)
KLEINE_TELESCOPEN = [(C + 8, G + 1, C + 1), (C - 8, G + 1, C - 2)]   # inside, by the windows


def d2(x, z):
    return math.hypot(x + 0.5 - (C + 0.5), z + 0.5 - (C + 0.5))


def buis_punt(t):
    n = math.sqrt(sum(c * c for c in BUIS_RICHTING))
    return tuple(BUIS_VAN[i] + BUIS_RICHTING[i] / n * t for i in range(3))


def afstand_tot_buis(x, y, z):
    """Distance of a block's middle to the telescope's axis, and how far along it (t)."""
    n = math.sqrt(sum(c * c for c in BUIS_RICHTING))
    dvec = [c / n for c in BUIS_RICHTING]
    p = (x + 0.5 - BUIS_VAN[0], y + 0.5 - BUIS_VAN[1], z + 0.5 - BUIS_VAN[2])
    t = sum(p[i] * dvec[i] for i in range(3))
    tt = max(0.0, min(BUIS_LENGTE, t))
    q = [BUIS_VAN[i] + dvec[i] * tt for i in range(3)]
    return math.dist((x + 0.5, y + 0.5, z + 0.5), q), t


def facing_out(dx, dz):
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def build(h):
    s = h.Structure((W, H, D))
    rng = random.Random(28601)
    info = {"gezichten": 0}

    # --- the plate, the legs and the core -----------------------------------------------------------------------------
    for x in range(W):
        for z in range(D):
            d = d2(x, z)
            if d <= TERRAS:
                for y in range(G - 5, G):
                    s.set(x, y, z, KS)
                # the floor
                if d > TERRAS - 1:
                    s.set(x, G, z, KS)
                elif d <= TOREN - 1:
                    s.set(x, G, z, KLINK)
                else:
                    s.set(x, G, z, KLINK)
            if d <= 4.5:
                for y in range(0, G - 5):
                    s.set(x, y, z, KS)
    for k in range(12):
        a = k * math.pi / 6
        lx, lz = C + round(17.5 * math.cos(a)), C + round(17.5 * math.sin(a))
        for x in (lx, lx + 1):
            for z in (lz, lz + 1):
                for y in range(0, G - 5):
                    s.set(x, y, z, KS)
        # a guh face peeking out under the terrace, looking outward
        f = facing_out(math.cos(a), math.sin(a))
        fx, fz = (lx + (1 if f == "east" else 0), lz + (1 if f == "south" else 0))
        s.set(fx, G - 6, fz, "guhs:knuffelsteen_gezicht", {"facing": f, "stemming": str(k % 4)})
        info["gezichten"] += 1

    # --- the terrace: parapet with posts and lanterns, four stairs down ------------------------------------------------
    trappen = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}
    open_rand = set()
    for f, (dx, dz) in trappen.items():
        for w in (-1, 0, 1):
            for r in range(19, 24):
                x = C + dx * r + (w if dx == 0 else 0)
                z = C + dz * r + (w if dz == 0 else 0)
                open_rand.add((x, z))
    for x in range(W):
        for z in range(D):
            d = d2(x, z)
            if TERRAS - 1 < d <= TERRAS and (x, z) not in open_rand:
                ang = math.degrees(math.atan2(z - C, x - C)) % 30
                if ang < 3.5:
                    s.set(x, G + 1, z, KS)
                    s.set(x, G + 2, z, LANTAARN)
                else:
                    s.set(x, G + 1, z, "guhs:knuffelsteen_plaat", {"type": "bottom", "waterlogged": "false"})
    for f, (dx, dz) in trappen.items():
        up = hulp.OPP[f]
        for w in (-1, 0, 1):
            for i, r in enumerate((21, 22, 23)):
                x = C + dx * r + (w if dx == 0 else 0)
                z = C + dz * r + (w if dz == 0 else 0)
                y = G - 1 - i
                s.set(x, y, z, "guhs:knuffelsteen_trap", {"facing": up, "half": "bottom", "shape": "straight", "waterlogged": "false"})
                for yy in range(G - 5, y):
                    s.set(x, yy, z, KS)
        # the gap in the terrace edge: floor
        for w in (-1, 0, 1):
            for r in (19, 20):
                x = C + dx * r + (w if dx == 0 else 0)
                z = C + dz * r + (w if dz == 0 else 0)
                s.set(x, G, z, KLINK)
        # two lanterns on posts at every stair
        for w in (-2, 2):
            x = C + dx * 20 + (w if dx == 0 else 0)
            z = C + dz * 20 + (w if dz == 0 else 0)
            s.set(x, G + 1, z, KS)
            s.set(x, G + 2, z, LANTAARN)

    # --- the tower ----------------------------------------------------------------------------------------------------------
    for x in range(W):
        for z in range(D):
            d = d2(x, z)
            if TOREN - 1 < d <= TOREN:
                ang = math.degrees(math.atan2(z - C, x - C)) % 45
                for y in range(G + 1, G + 12):
                    raam = G + 3 <= y <= G + 5 and 18 <= ang <= 27
                    s.set(x, y, z, "minecraft:light_blue_stained_glass" if raam else KS)
                s.set(x, G + 12, z, KS)
            if TOREN <= d <= TOREN + 1:
                s.set(x, G + 12, z, "guhs:knuffelsteen_plaat", {"type": "bottom", "waterlogged": "false"})
                s.set(x, G + 7, z, "guhs:pluisdak_plaat", {"type": "top", "waterlogged": "false"})
    # the door (south) with a little porch roof
    for x in (C - 1, C, C + 1):
        for y in (G + 1, G + 2, G + 3):
            for z in (C + 10, C + 11):
                s.set(x, y, z, hulp.AIR)
    for x in range(C - 2, C + 3):
        s.set(x, G + 4, C + 12, "guhs:pluisdak_trap", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    s.set(C, G + 5, C + 11, "guhs:knuffelsteen_gezicht", {"facing": "south", "stemming": "0"})
    info["gezichten"] += 1
    # eight guh faces round the tower (under the pluisdak ledge)
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        fx, fz = C + round(10.9 * math.cos(a)), C + round(10.9 * math.sin(a))
        if abs(fx - C) <= 1 and fz > C:
            continue
        f = facing_out(math.cos(a), math.sin(a))
        s.set(fx, G + 9, fz, "guhs:knuffelsteen_gezicht", {"facing": f, "stemming": str(k % 4)})
        info["gezichten"] += 1

    # --- the dome: a guh's head -------------------------------------------------------------------------------------------
    face = {}
    R = 8.0
    for x in range(W):
        for z in range(D):
            for y in range(KOEPEL_Y, KOEPEL_Y + 13):
                dist = math.dist((x + 0.5, y + 0.5, z + 0.5), (C + 0.5, KOEPEL_Y + 0.5, C + 0.5))
                if not (KOEPEL - 1.15 <= dist <= KOEPEL):
                    continue
                block = "minecraft:white_concrete"
                ang = math.degrees(math.atan2(z - C, x - C)) % 45
                if ang < 4 or ang > 41:
                    block = "minecraft:light_blue_concrete"
                if y >= KOEPEL_Y + 10:
                    block = "minecraft:pink_concrete"
                if z > C + 2:
                    role = hulp.face_role(-(x - C), y - (KOEPEL_Y + 5), R)
                    if role and role not in ("ear", "ear_in"):
                        block = {"skin": "minecraft:pink_concrete", "eye": "minecraft:black_concrete", "ring": "minecraft:light_blue_concrete",
                                 "shine": "minecraft:white_concrete", "nose": "minecraft:magenta_concrete", "mouth": "minecraft:purple_concrete",
                                 "cheek": "minecraft:pink_terracotta"}[role]
                        face[role] = face.get(role, 0) + 1
                s.set(x, y, z, block)
    info["gezicht_koepel"] = face
    # the big round ears
    for sx in (-1, 1):
        ex, ey = C + sx * 7, G + 25
        for x in range(ex - 4, ex + 5):
            for y in range(ey - 5, ey + 5):
                d = math.dist((x, y), (ex, ey))
                if d <= 3.8:
                    for z in (C - 1, C):
                        dome = math.dist((x + 0.5, y + 0.5, z + 0.5), (C + 0.5, KOEPEL_Y + 0.5, C + 0.5))
                        if dome < KOEPEL - 1.15:
                            continue
                        s.set(x, y, z, "minecraft:magenta_wool" if d <= 2.2 and z == C else "minecraft:pink_wool")
    info["gezichten"] += 1

    # --- the big telescope ------------------------------------------------------------------------------------------------
    for x in range(W):
        for z in range(D):
            for y in range(G + 1, H):
                dist, t = afstand_tot_buis(x, y, z)
                if -0.5 <= t <= BUIS_LENGTE + 0.5 and dist <= BUIS_R:
                    if t > BUIS_LENGTE - 1.2:
                        blk = "minecraft:light_blue_stained_glass" if dist <= 0.9 else "minecraft:gold_block"
                    elif (t % 6) < 1.0:
                        blk = "minecraft:gold_block"
                    else:
                        blk = "minecraft:light_blue_concrete"
                    s.set(x, y, z, blk)
                elif -0.5 <= t <= BUIS_LENGTE and BUIS_R < dist <= BUIS_R + 1.3:
                    # the slit in the dome
                    if s.get(x, y, z) is not None and y >= KOEPEL_Y and s.get(x, y, z) not in ("minecraft:pink_wool", "minecraft:magenta_wool"):
                        s.set(x, y, z, hulp.AIR)
    # little guh ears at the lens
    end = buis_punt(BUIS_LENGTE - 1.5)
    for sx in (-1, 1):
        ox = end[0] + sx * 1.6
        oz = end[2] + sx * 1.1
        for dy in (1.2, 2.2):
            s.set(math.floor(ox), math.floor(end[1] + dy), math.floor(oz), "minecraft:pink_wool")
        s.set(math.floor(ox), math.floor(end[1] + 1.2), math.floor(oz) + (1 if sx < 0 else -1), "minecraft:magenta_wool")
    # its pedestal and the eyepiece
    for y in range(G + 1, G + 4):
        s.set(C, y, C + 2, "minecraft:gold_block" if y == G + 3 else "minecraft:copper_block")
    s.set(C, G + 1, C + 3, "minecraft:cut_copper_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    s.set(*EYEPIECE, TELESCOOP, {"facing": "north"})

    # --- inside ---------------------------------------------------------------------------------------------------------------
    ster = [(0.10, 0.80), (0.90, 0.80), (0.74, 0.22), (0.40, 0.64), (0.56, 0.52), (0.64, 0.70)]
    for x in range(W):
        for z in range(D):
            d = d2(x, z)
            if d <= 6.5:
                s.set(x, G, z, "minecraft:black_concrete" if rng.random() > 0.05 else "minecraft:white_concrete")
    for (u, v) in ster:
        s.set(C - 5 + round(u * 9), G, C - 5 + round(v * 9), "minecraft:yellow_concrete")
    for k in range(10):
        a = k * math.pi / 5
        x, z = C + round(9.3 * math.cos(a)), C + round(9.3 * math.sin(a))
        if abs(x - C) <= 2 and z > C:
            continue
        if (x, z) in [(t[0], t[2]) for t in KLEINE_TELESCOPEN]:
            continue
        s.set(x, G + 1, z, "minecraft:bookshelf" if k % 3 else LANTAARN)
        if k % 3:
            s.set(x, G + 2, z, "minecraft:bookshelf")
    for (x, y, z) in KLEINE_TELESCOPEN:
        s.set(x, y, z, TELESCOOP, {"facing": "east" if x > C else "west"})
    s.set(C - 6, G + 1, C + 5, "guhs:guh_tafel", {"facing": "east"})
    s.set(C - 5, G + 1, C + 6, "guhs:guh_stoel", {"facing": "north"})
    s.set(C - 6, G + 2, C + 5, LANTAARN)
    s.set(C + 6, G + 1, C + 6, "minecraft:cartography_table")
    s.set(C + 7, G + 1, C + 4, "minecraft:chest", {"facing": "west", "type": "single", "waterlogged": "false"},
          {"id": "minecraft:chest", "LootTable": "guhs:chests/guh_sterrenwacht"})
    for (x, z) in ((C - 3, C - 7), (C + 3, C - 7), (C - 8, C + 5), (C + 8, C + 7)):
        s.set(x, G + 1, z, LANTAARN)
    ms = h.ms
    s.entity(NPC[0] + 0.5, float(NPC[1]), NPC[2] + 0.5, {"id": "guhs:guh_npc", "Kind": "sterrenkijkerguh", "PersistenceRequired": ms.Byte(1),
                                                         "Rotation": ms.floats(200.0, 0.0)})

    # --- the terrace: four little telescopes, benches, flower boxes ---------------------------------------------------------
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        x, z = C + round(16 * math.cos(a)), C + round(16 * math.sin(a))
        f = facing_out(math.cos(a), math.sin(a))
        s.set(x, G + 1, z, TELESCOOP, {"facing": f})
        s.set(x + (1 if x < C else -1), G + 1, z, LANTAARN)
        bx, bz = C + round(14 * math.cos(a + 0.35)), C + round(14 * math.sin(a + 0.35))
        s.set(bx, G + 1, bz, "guhs:guh_bank", {"facing": hulp.OPP[facing_out(math.cos(a + 0.35), math.sin(a + 0.35))]})
        fx, fz = C + round(18 * math.cos(a - 0.3)), C + round(18 * math.sin(a - 0.3))
        s.set(fx, G + 1, fz, "guhs:seizoensbloembak", {"seizoen": "lente"})
    # the anchor (the middle of the star map)
    s.set(C, G, C, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": "minecraft:black_concrete",
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.clear_above([(x, z) for x in range(W) for z in range(D) if d2(x, z) <= 23.5], G + 1, top=H)
    return s, info


def check(s, info):
    problems = []
    loose = [p for p in hulp.check_floating(s, 0)]
    if loose:
        problems.append(f"{len(loose)} floating blocks, e.g. {[(p, s.get(*p)) for p in loose[:6]]}")
    starts = [(C, G - 2, C + 23), (C, G - 2, C - 23), (C + 23, G - 2, C), (C - 23, G - 2, C)]
    reach = hulp.walk(s, starts, extra_passable=("minecraft:jigsaw",))
    if len(reach) < 400:
        problems.append(f"only {len(reach)} walkable spots")
    for name, (x, y, z) in [("the Professor", NPC), ("the eyepiece", EYEPIECE)] + [(f"telescope {t}", t) for t in KLEINE_TELESCOPEN]:
        if not hulp.near_reachable(reach, x, y, z, 1 if name != "the Professor" else 0):
            problems.append(f"{name} at {(x, y, z)} can't be walked to")
    x, y, z = NPC
    if hulp.passable(s.get(x, y - 1, z)) or not hulp.passable(s.get(x, y, z)) or not hulp.passable(s.get(x, y + 1, z)):
        problems.append("the Professor doesn't stand on a floor with room above")
    # every telescope on the terrace can be reached
    for p, (b, props, _) in s.blocks.items():
        if b == TELESCOOP and not hulp.near_reachable(reach, *p, r=1):
            problems.append(f"telescope at {p} can't be reached")
    if s.get(C, G, C) != "minecraft:jigsaw":
        problems.append("no anchor jigsaw")
    if info["gezichten"] < 20 or sum(info["gezicht_koepel"].values()) < 40:
        problems.append(f"too few guh faces: {info}")
    if any(not (0 <= x < W and 0 <= y < H and 0 <= z < D) for (x, y, z) in s.blocks):
        problems.append("blocks outside the template")
    if problems:
        raise SystemExit("guh_sterrenwacht geometry check failed:\n  " + "\n  ".join(problems))
    return len(reach)


if __name__ == "__main__":
    import types
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    import make_structures as ms_mod
    h = types.SimpleNamespace(Structure=ms_mod.Structure, ms=ms_mod)
    s, info = build(h)
    n = check(s, info)
    print(f"guh_sterrenwacht ok: {len(s.blocks)} blocks, {n} walkable spots, {info}")
