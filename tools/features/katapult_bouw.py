"""
De Knabbelkatapult (2.9, slice sjoelkatapult) - the template (64 x 46 x 78) on the Vadskliffen:

  south  a cheerful guh castle: pink knuffelsteen walls with battlements, four octagonal corner towers with pluisdak
         roofs, guh ears and guh faces, a gatehouse with a big guh face over the gate, a courtyard with flower beds, a
         pluisbal statue with a guh face, a Mika practice dummy and stacks of pluisballen; two staircases up to the wide
         north wall
  wall   on the north wall the Knabbelkatapult itself: a wooden frame with a crossbar, ropes and the bucket (the
         katapult_werper, shooting north) with a pink pluisbal in it; Kapitein Floepguh stands next to it
  gorge  a deep rocky gorge (pink and magenta terracotta, flowers and old pluisballen at the bottom)
  north  on a cliff ledge the fort plot (katapult_fortplek, facing the castle) with fort 1 on it, and behind it the
         crooked Mika fort: a leaning keep with a big Mika face, two leaning towers with purple roofs and flags,
         scaffolding, a pile of stolen kaasknabbels and cheeky Mika figures on the battlements
check(s): nothing floats, the catapult and the Kapitein can be walked to from the gate, the markers are right, the ball
has a free way out, the fort plot is clear apart from fort 1.
Run it alone:  python tools/features/katapult_bouw.py   (from the project root)
"""
import math
import os
import random
import sys

if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

from features import katapult_forten as forten  # noqa: E402
from features import sterrenwacht_hulp as hulp  # noqa: E402

AIR = hulp.AIR
NAME = "knabbelkatapult"
W, H, D = 64, 46, 78
G = 12                          # castle ground (the courtyard floor)
L = G + 4                       # the ledge with the Mika fort
B = G - 7                       # the bottom of the gorge
ANCHOR = "guhs:knabbelkatapult_midden"
ANCHOR_POS = (32, G, 62)
WALK = G + 10                   # the top of the north wall (you stand on it)
WERPER = (32, WALK + 1, 45)     # the catapult's bucket (facing north)
PLEK = (32, L, 24)              # the fortplek (facing south, towards the castle)
NPC = (39.5, WALK + 1, 47.5)
ENTRANCE = (32, G + 1, D - 1)
KS = "guhs:knuffelsteen"
KSM = "guhs:knuffelsteen_muur"
KLINK = "guhs:knuffelklinkers"
MIKASTEEN = "guhs:mika_steen"
FLOWERS = ["guhs:roze_guhbloem", "guhs:kaasbloem", "guhs:guhoortjes", "guhs:knabbelroos"]
MIKA_FACE = {"skin": "minecraft:pink_concrete", "ear": "minecraft:pink_concrete", "ear_in": "minecraft:magenta_concrete",
             "eye": "minecraft:red_concrete", "ring": "minecraft:black_concrete", "shine": "minecraft:white_concrete",
             "nose": "minecraft:magenta_concrete", "mouth": "minecraft:black_concrete", "cheek": "minecraft:pink_concrete"}


def wall_post(s, x, y, z):
    s.set(x, y, z, KSM, {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"})


def fence(s, x, y, z, block="minecraft:dark_oak_fence"):
    s.set(x, y, z, block, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})


def slab(s, x, y, z, block, kind="bottom"):
    s.set(x, y, z, block, {"type": kind, "waterlogged": "false"})


# --- the land ------------------------------------------------------------------------------------------------------------------
def gorge_top(x, z, rng_cache={}):
    """The height of the ground at (x, z): ledge, gorge or castle ground."""
    if z <= 29:
        return L
    if z >= 44:
        return G
    wobble = ((x * 7 + z * 13) % 5 == 0) - ((x * 3 + z * 5) % 7 == 0)
    profile = {30: L - 2, 31: L - 5, 32: B + 3, 33: B + 1, 41: B + 2, 42: B + 4, 43: G - 1}
    return profile.get(z, B) + (wobble if z not in (30, 43) else 0)


def land(s, rng):
    for x in range(W):
        for z in range(D):
            top = gorge_top(x, z)
            for y in range(top + 1):
                band = (y + (x // 5 + z // 7) % 2) % 5 == 0
                s.set(x, y, z, "minecraft:magenta_terracotta" if band else "minecraft:pink_terracotta")
            if z >= 44 or z <= 29:
                s.set(x, top, z, "minecraft:pink_wool" if rng.random() > 0.1 else "minecraft:pink_concrete_powder")
    # the gorge floor: flowers and old pluisballen
    for x in range(1, W - 1):
        for z in range(33, 41):
            r = rng.random()
            if r < 0.12:
                s.set(x, gorge_top(x, z) + 1, z, FLOWERS[rng.randrange(len(FLOWERS))])
    for (cx, cz) in ((10, 36), (23, 38), (41, 35), (52, 39), (30, 34)):
        y = gorge_top(cx, cz) + 1
        for dx in (0, 1):
            for dz in (0, 1):
                s.set(cx + dx, y, cz + dz, "minecraft:pink_wool")
        s.set(cx, y + 1, cz, "minecraft:pink_wool")


# --- the castle ----------------------------------------------------------------------------------------------------------------------
def octagon(x0, z0, size=7, cut=2):
    cells = []
    for x in range(x0, x0 + size):
        for z in range(z0, z0 + size):
            dx = min(x - x0, x0 + size - 1 - x)
            dz = min(z - z0, z0 + size - 1 - z)
            if dx + dz >= cut:
                cells.append((x, z))
    return cells


def tower(s, x0, z0, faces):
    """An octagonal tower (7 x 7), from the bottom of the world up, with a pluisdak roof, guh ears and guh faces."""
    cells = set(octagon(x0, z0))
    top = G + 15
    for (x, z) in cells:
        ground = gorge_top(x, z)
        for y in range(0, top + 1):
            edge = any((x + dx, z + dz) not in cells for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if y <= max(G, ground) or edge:
                s.set(x, y, z, KS if y % 6 else "guhs:knuffelsteen_plaat" if False else KS)
            elif y > G:
                s.set(x, y, z, AIR)
        s.set(x, G + 10, z, KS)                                 # a floor at the height of the wall walk
    # battlements
    for (x, z) in cells:
        if any((x + dx, z + dz) not in cells for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))) and (x + z) % 2 == 0:
            s.set(x, top + 1, z, KS)
    # the roof: a pluisdak cone
    cx, cz = x0 + 3, z0 + 3
    for lvl in range(6):
        r = 3.4 - lvl * 0.6
        for x in range(x0 - 1, x0 + 8):
            for z in range(z0 - 1, z0 + 8):
                if math.hypot(x - cx, z - cz) <= r + 0.3:
                    s.set(x, top + 1 + lvl, z, "guhs:pluisdak")
    # guh ears on the roof (pink wool with magenta inside)
    for sx in (-1, 1):
        ex = cx + sx * 2
        for dy in range(3):
            s.set(ex, top + 3 + dy, cz, "minecraft:pink_wool")
        s.set(ex, top + 3, cz - 1, "minecraft:magenta_wool")
        s.set(ex, top + 4, cz - 1, "minecraft:magenta_wool")
        s.set(ex + sx, top + 3, cz, "minecraft:pink_wool")
    s.set(cx, top + 7, cz, "guhs:lampion_roze", {"hanging": "false"})
    # guh faces on the flat sides
    for facing in faces:
        if facing == "north":
            hulp.wall_face(s, cx, G + 13, z0, 2.4, "north")
        elif facing == "south":
            hulp.wall_face(s, cx, G + 13, z0 + 6, 2.4, "south")
        elif facing == "west":
            hulp.wall_face(s, cz, G + 13, x0, 2.4, "west")
        else:
            hulp.wall_face(s, cz, G + 13, x0 + 6, 2.4, "east")
    return cells


def castle(s, rng):
    X0, X1, Z0, Z1 = 8, 55, 44, 74
    # the courtyard
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            s.set(x, G, z, "minecraft:pink_wool")
    for z in range(49, D):
        for x in range(30, 35):
            s.set(x, G, z, KLINK)
    for x in range(12, 52):
        for z in (52, 53):
            s.set(x, G, z, KLINK)
    # the north wall: thick, with the wall walk on top (y WALK), from the gorge up
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z0 + 5):
            for y in range(0, WALK + 1):
                s.set(x, y, z, KS if y < WALK else KLINK)
    # battlements on its north edge, open in front of the catapult
    for x in range(X0, X1 + 1):
        if 28 <= x <= 36:
            continue
        s.set(x, WALK + 1, Z0, KS)
        if x % 2 == 0:
            s.set(x, WALK + 2, Z0, KS)
    for x in (27, 37):
        s.set(x, WALK + 2, Z0, KS)
        s.set(x, WALK + 3, Z0, "guhs:lampion_geel", {"hanging": "false"})
    # a railing on the inner edge (open at the stairs)
    for x in range(X0 + 1, X1):
        if x in (20, 21, 42, 43):
            continue
        fence(s, x, WALK + 1, Z0 + 4, "guhs:vadshout_hek")
    # the other walls
    for y in range(G + 1, G + 9):
        for x in range(X0, X1 + 1):
            s.set(x, y, Z1, KS)
        for z in range(Z0, Z1 + 1):
            s.set(X0, y, z, KS)
            s.set(X1, y, z, KS)
    for x in range(X0, X1 + 1):
        if x % 2 == 0:
            s.set(x, G + 9, Z1, KS)
    for z in range(Z0 + 5, Z1 + 1):
        if z % 2 == 0:
            s.set(X0, G + 9, z, KS)
            s.set(X1, G + 9, z, KS)
    # the gatehouse with a big guh face over the gate
    for x in range(26, 39):
        for y in range(G + 1, G + 15):
            s.set(x, y, Z1, KS)
        if x % 2 == 0:
            s.set(x, G + 15, Z1, KS)
    for x in range(30, 35):
        for y in range(G + 1, G + 6):
            s.set(x, y, Z1, AIR)
    for x in range(29, 36):
        s.set(x, G + 6, Z1, "guhs:knuffelsteen_plaat", {"type": "top", "waterlogged": "false"})
    hulp.wall_face(s, 32, G + 10, Z1, 3.6, "south")
    for x in (28, 36):
        s.set(x, G + 3, Z1 + 1, "guhs:lampion_roze", {"hanging": "false"})
        wall_post(s, x, G + 1, Z1 + 1)
        wall_post(s, x, G + 2, Z1 + 1)
    # the corner towers
    tower(s, X0 - 3, Z0 - 2, ("north", "west"))
    tower(s, X1 - 3, Z0 - 2, ("north", "east"))
    tower(s, X0 - 3, Z1 - 3, ("south", "west"))
    tower(s, X1 - 3, Z1 - 3, ("south", "east"))
    # two staircases up to the wall walk (they go up towards the middle), filled underneath
    for x0, step in ((50, -1), (13, 1)):
        for i in range(10):
            x = x0 + step * i
            y = G + 1 + i
            for z in range(Z0 + 5, Z0 + 8):
                for yy in range(G + 1, y):
                    s.set(x, yy, z, KS)
                s.set(x, y, z, "guhs:knuffelsteen_trap", {"facing": "west" if step < 0 else "east", "half": "bottom", "shape": "straight",
                                                          "waterlogged": "false"})
        # the landing at the top, next to the wall walk
        x = x0 + step * 10
        for z in range(Z0 + 5, Z0 + 8):
            for yy in range(G + 1, WALK + 1):
                s.set(x, yy, z, KS)
        for xx in (x, x - step):
            for z in range(Z0 + 5, Z0 + 8):
                for yy in range(G + 1, WALK + 1):
                    s.set(xx, yy, z, KS)
    # courtyard: flower beds, a pluisbal statue with a guh face, a Mika practice dummy, benches and lanterns
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 9, Z1):
            if s.get(x, G, z) == "minecraft:pink_wool" and s.get(x, G + 1, z) is None and rng.random() < 0.18:
                s.set(x, G + 1, z, FLOWERS[rng.randrange(len(FLOWERS))])
    s.sphere(20, G + 3, 64, 2.2, "minecraft:pink_wool")
    for dx, dz in ((0, 0), (1, 0), (0, 1), (1, 1)):
        s.set(19 + dx, G + 1, 63 + dz, "minecraft:pink_wool")
    s.set(20, G + 3, 62, "guhs:knuffelsteen_gezicht", {"facing": "north", "stemming": "0"})
    s.set(20, G + 6, 64, "guhs:lampion_geel", {"hanging": "false"})
    fence(s, 44, G + 1, 64)
    fence(s, 44, G + 2, 64)
    s.set(44, G + 3, 64, "guhs:katapult_mika", {"facing": "west"})
    for z in (58, 68):
        s.set(26, G + 1, z, "guhs:guh_bank", {"facing": "east"})
        s.set(38, G + 1, z, "guhs:guh_bank", {"facing": "west"})
        for x in (29, 35):
            fence(s, x, G + 1, z, "guhs:vadshout_hek")
            fence(s, x, G + 2, z, "guhs:vadshout_hek")
            s.set(x, G + 3, z, "guhs:lampion_roze" if z == 58 else "guhs:lampion_mint", {"hanging": "false"})
    return X0, X1, Z0, Z1


def catapult(s):
    """The Knabbelkatapult on the north wall: two A-frames, a crossbar high up, ropes, the bucket, pluisballen ready."""
    wx, wy, wz = WERPER
    for x in (wx - 2, wx + 2):
        for z in range(wz - 1, wz + 4):
            s.set(x, wy, z, "minecraft:dark_oak_log", {"axis": "z"})
        for y in range(wy + 1, wy + 4):
            s.set(x, y, wz + 1, "minecraft:dark_oak_log", {"axis": "y"})
        s.set(x, wy + 1, wz, "minecraft:dark_oak_stairs", {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        s.set(x, wy + 1, wz + 2, "minecraft:dark_oak_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for x in range(wx - 2, wx + 3):
        s.set(x, wy + 4, wz + 1, "minecraft:dark_oak_log", {"axis": "x"})
    s.set(wx - 1, wy + 3, wz + 1, "guhs:vadstouw", {"axis": "y"})
    s.set(wx + 1, wy + 3, wz + 1, "guhs:vadstouw", {"axis": "y"})
    s.set(wx, wy + 5, wz + 1, "guhs:block_of_kaasknabbels")            # the counterweight: a block of kaasknabbels!
    s.set(wx, wy, wz, "guhs:katapult_werper", {"facing": "north"})
    # stacks of pluisballen either side
    for x in (wx - 5, wx + 4):
        for dx in (0, 1):
            for dz in (0, 1):
                s.set(x + dx, wy, wz + dz, "minecraft:pink_wool")
        s.set(x, wy + 1, wz, "minecraft:pink_wool")
    s.set(wx - 5, wy + 1, wz + 1, "minecraft:magenta_wool")


# --- the Mika side -------------------------------------------------------------------------------------------------------------------
def plot(s, fort):
    px, py, pz = PLEK
    for x in range(px - 8, px + 9):
        for z in range(pz - 10, pz + 3):
            s.set(x, py, z, "minecraft:polished_blackstone" if (x + z) % 2 else "minecraft:polished_blackstone_bricks")
    s.set(px, py, pz, "guhs:katapult_fortplek", {"facing": "south"})
    # fort 1, as the game builds it (the plot faces south: the template is turned half round)
    for (tx, ty, tz), (b, props) in fort.blocks.items():
        if b == AIR:
            continue
        s.set(px - (tx - forten.MID), py + 1 + ty, pz - tz, b, rotate180(props))
    # posts with lampions on the plot's corners (outside the fort's box)
    for (x, z) in ((px - 8, pz + 2), (px + 8, pz + 2), (px - 8, pz - 10), (px + 8, pz - 10)):
        fence(s, x, py + 1, z)
        fence(s, x, py + 2, z)
        s.set(x, py + 3, z, "guhs:lampion_roze", {"hanging": "false"})


def rotate180(props):
    flip = {"north": "south", "south": "north", "east": "west", "west": "east"}
    out = {}
    for k, v in props.items():
        if k == "facing":
            out[k] = flip.get(v, v)
        elif k in flip:
            out[flip[k]] = v
        else:
            out[k] = v
    return out


def mika_fort(s, rng):
    """The crooked Mika fort behind the plot: everything leans a bit (Mika's build like that)."""
    # the keep: 9 x 9, leaning east one block every 4 layers, with a big Mika face looking at the castle
    for y in range(L + 1, L + 22):
        lean = (y - L - 1) // 4
        for x in range(28 + lean, 37 + lean):
            for z in range(3, 12):
                edge = x in (28 + lean, 36 + lean) or z in (3, 11)
                if edge or y == L + 11:
                    s.set(x, y, z, MIKASTEEN if (x + y) % 5 else "guhs:mika_steen_pilaar", {"axis": "y"} if (x + y) % 5 == 0 else None)
        if y == L + 21:
            for x in range(28 + lean, 37 + lean):
                for z in range(3, 12):
                    if (x in (28 + lean, 36 + lean) or z in (3, 11)) and (x + z) % 2 == 0:
                        s.set(x, y + 1, z, MIKASTEEN)
    hulp.wall_face(s, 34, L + 15, 3, 3.6, "south", palette=MIKA_FACE)
    # a gate in the keep (the Mika's front door), with a crooked roof over it
    for x in (31, 32, 33):
        for y in range(L + 1, L + 4):
            s.set(x, y, 3, AIR)
    for x in range(30, 35):
        slab(s, x, L + 4, 2, "minecraft:dark_oak_slab", "top")
    # two leaning towers with purple roofs and flags
    for x0, lean_dir in ((17, -1), (43, 1)):
        for y in range(L + 1, L + 16):
            lean = lean_dir * ((y - L - 1) // 5)
            for x in range(x0 + lean, x0 + 5 + lean):
                for z in range(5, 10):
                    if x in (x0 + lean, x0 + 4 + lean) or z in (5, 9):
                        s.set(x, y, z, "minecraft:polished_blackstone_bricks" if (y // 3) % 2 else MIKASTEEN)
        lean = lean_dir * ((L + 15 - L - 1) // 5)
        for lvl in range(4):
            for x in range(x0 + lean - 1 + lvl, x0 + lean + 6 - lvl):
                for z in range(4 + lvl, 11 - lvl):
                    s.set(x, L + 16 + lvl, z, "minecraft:purple_wool")
        fx = x0 + lean + 2
        for y in range(L + 20, L + 24):
            fence(s, fx, y, 7)
        s.set(fx + 1, L + 23, 7, "minecraft:magenta_wool")
        s.set(fx + 2, L + 23, 7, "minecraft:magenta_wool")
        s.set(fx + 1, L + 22, 7, "minecraft:magenta_wool")
        s.set(x0 + lean + 2, L + 10, 4, "minecraft:purple_stained_glass")
    # walls between the towers and the keep, with Mika figures sitting on them (they're watching you!)
    for x in list(range(22, 28)) + list(range(38, 43)):
        for y in range(L + 1, L + 6):
            s.set(x, y, 7, MIKASTEEN)
        if x % 2 == 0:
            s.set(x, L + 6, 7, MIKASTEEN)
    for x in (23, 26, 39, 41):
        s.set(x, L + 7 if x % 2 == 0 else L + 6, 7, "guhs:katapult_mika", {"facing": "south"})
    # scaffolding on the keep's front (rickety!)
    for x in (27, 38):
        for y in range(L + 1, L + 9):
            fence(s, x, y, 2)
    for x in range(27, 39):
        slab(s, x, L + 9, 2, "minecraft:dark_oak_slab")
    # the stolen kaasknabbels: a pile and crates in front of the keep's door
    for (x, z) in ((24, 12), (25, 12), (24, 13), (40, 12), (41, 13)):
        s.set(x, L + 1, z, "guhs:block_of_kaasknabbels")
    s.set(24, L + 2, 12, "guhs:block_of_kaasknabbels")
    s.set(25, L + 1, 13, "guhs:katapult_knabbelkist", {"facing": "south"})
    s.set(40, L + 1, 13, "guhs:katapult_knabbelkist", {"facing": "south"})
    # a crooked Mika banner pole at the ledge's edge
    for x in (22, 42):
        for y in range(L + 1, L + 6):
            fence(s, x, y, 27)
        s.set(x + 1, L + 5, 27, "minecraft:purple_wool")
        s.set(x + 1, L + 4, 27, "minecraft:magenta_wool")


def build(h):
    s = h.Structure((W, H, D))
    rng = random.Random(29301)
    land(s, rng)
    castle(s, rng)
    catapult(s)
    forts = forten.FORTS
    plot(s, forts[0]())
    mika_fort(s, rng)
    s.set(*ANCHOR_POS, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": KLINK,
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    footprint = [(x, z) for x in range(W) for z in range(D)]
    for (x, z) in footprint:
        top = max(gorge_top(x, z), 0)
        for y in range(top + 1, H):
            if (x, y, z) not in s.blocks:
                s.set(x, y, z, AIR)
    s.entity(NPC[0], float(NPC[1]), NPC[2], {"id": "guhs:guh_npc", "Kind": "katapultguh", "PersistenceRequired": h.ms.Byte(1),
                                              "Rotation": h.ms.floats(90.0, 0.0)})
    return s, {"gezichten": 4 * 2 + 1 + 1 + 1}


def check(s):
    problems = []
    floating = hulp.check_floating(s, 0)
    if floating:
        problems.append(f"{len(floating)} floating blocks, e.g. {floating[:6]}")
    reach = hulp.walk(s, [ENTRANCE], extra_passable=("minecraft:jigsaw",))
    wx, wy, wz = WERPER
    behind = (wx, wy, wz + 2)
    if behind not in reach:
        problems.append(f"the spot behind the catapult {behind} can't be reached from the gate")
    if not hulp.near_reachable(reach, int(NPC[0]), int(NPC[1]), int(NPC[2]), r=1):
        problems.append("Kapitein Floepguh can't be reached")
    x, y, z = int(NPC[0]), int(NPC[1]), int(NPC[2])
    if hulp.passable(s.get(x, y - 1, z)) or not hulp.passable(s.get(x, y, z)) or not hulp.passable(s.get(x, y + 1, z)):
        problems.append("the Kapitein doesn't stand on a floor with room above him")
    if s.get(*WERPER) != "guhs:katapult_werper" or s.get(*PLEK) != "guhs:katapult_fortplek":
        problems.append("the catapult's bucket or the fort plot is missing")
    # the ball's way out: straight up and north from the bucket, nothing in the way for 6 blocks
    for i in range(1, 7):
        for dy in (1, 2):
            if not hulp.passable(s.get(wx, wy + dy + (i > 2), wz - i)):
                problems.append(f"something in the ball's way at {(wx, wy + dy, wz - i)}")
    # the fort's box: only fort 1 in it
    px, py, pz = PLEK
    for tx in range(forten.W):
        for ty in range(forten.H):
            for tz in range(forten.D):
                b = s.get(px - (tx - forten.MID), py + 1 + ty, pz - tz)
                if b not in (None, AIR) and (tx, ty, tz) not in forten.FORTS[0]().blocks:
                    problems.append(f"something else in the fort's box at {(tx, ty, tz)}: {b}")
    if s.get(*ANCHOR_POS) != "minecraft:jigsaw":
        problems.append("no centre jigsaw")
    if problems:
        raise SystemExit("knabbelkatapult self-check failed:\n  " + "\n  ".join(problems[:20]))
    return len(reach)


if __name__ == "__main__":
    import types
    import make_structures as ms
    hh = types.SimpleNamespace(Structure=ms.Structure, ms=ms)
    st, info = build(hh)
    print("walkable:", check(st), info)
