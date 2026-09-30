"""
Het surfstrand van Guhwai'i (3.0, slice guhwaii-spellen) - the template guhwaii_surfstrand (53 x 22 x 53), a sunny beach on
the island's shore (placed unrotated at the beach, so the sea can lie on any side: the plaza is round and every side is pretty):

  - in the middle the round hula podium: a bamboo floor with a ring of steps, a star of flower carpets round the hibiscus
    flower mat you dance on, Lilo-guh (plek "hula") under a flower arch of bamboo, garlands and flowering azalea, tiki torches
    all round and two tiki statues guarding the arch
  - north-east Lilo's surf shack: bamboo walls, a thatched gable roof, racks full of surf boards, shell lanterns, tiki masks on
    the posts, a counter and a porch; Lilo-guh (plek "surf") waits in front of it (the surf spot is found from her)
  - south-west Tikiguh's round Tiki stall: a bamboo bar under a conical thatched roof, bar stools, the radio, masks and
    lanterns; Tikiguh behind the bar
  - north-west a lifeguard chair with a parasol, south-east parasols with beach towels and a sand castle, palm trees with
    coconuts round the edge, flower beds, and bamboo paths between it all
check(s): nothing floats, the three guhs and the mat can be walked to from the edge, the anchor jigsaw is in place.
Run it alone:  python tools/features/guhwaii_spellen_bouw.py   (from the project root)
"""
import math
import os
import random
import sys

if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

from features import sterrenwacht_hulp as hulp  # noqa: E402

AIR = hulp.AIR
NAME = "guhwaii_surfstrand"
W, H, D = 53, 22, 53
G = 4
C = 26
ANCHOR = f"guhs:{NAME}_midden"

SAND = "minecraft:sand"
MOZAIEK = "minecraft:bamboo_mosaic"
MOZAIEK_PLAAT = "minecraft:bamboo_mosaic_slab"
PLANKEN = "minecraft:bamboo_planks"
PAAL = "minecraft:stripped_bamboo_block"
HEK = "minecraft:bamboo_fence"
RIET = "guhs:tiki_rietdak"
RIET_TRAP = "guhs:tiki_rietdak_trap"
RIET_PLAAT = "guhs:tiki_rietdak_plaat"
STAM = "minecraft:jungle_log"
BLAD = "minecraft:jungle_leaves"

# the three guhs and the dance mat (template coordinates; the NPC's feet)
HULA_LILO = (26.5, G + 2, 21.5)
SURF_LILO = (41.5, G + 1, 19.5)
TIKIGUH = (13.5, G + 1, 39.5)
MAT = (C, G + 2, C)


def paal(s, x, y0, y1, z):
    for y in range(y0, y1 + 1):
        s.set(x, y, z, PAAL, {"axis": "y"})


def hek(s, x, y, z):
    s.set(x, y, z, HEK, {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})


def trap(s, x, y, z, facing, half="bottom", block=RIET_TRAP):
    s.set(x, y, z, block, {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"})


def plaat(s, x, y, z, block=RIET_PLAAT, kind="bottom"):
    s.set(x, y, z, block, {"type": kind, "waterlogged": "false"})


def tiki(s, x, y, z, name, facing="south"):
    s.set(x, y, z, f"guhs:{name}", {"facing": facing})


def blad(s, x, y, z, block=BLAD):
    s.set(x, y, z, block, {"persistent": "true", "distance": "1", "waterlogged": "false"})


# =====================================================================================================================
def grond(s, rng):
    for x in range(W):
        for z in range(D):
            for y in range(G):
                s.set(x, y, z, "minecraft:sandstone" if y < G - 1 else SAND)
            s.set(x, G, z, SAND)


def paden(s):
    """Bamboo paths: from the shack's porch and from the stall to the podium, and in from the four sides."""
    def pad(a, b, breed=1):
        steps = int(max(abs(b[0] - a[0]), abs(b[1] - a[1]))) + 1
        for k in range(steps + 1):
            t = k / steps
            x, z = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
            for dx in range(-breed, breed + 1):
                for dz in range(-breed, breed + 1):
                    if abs(dx) + abs(dz) <= breed:
                        s.set(round(x) + dx, G, round(z) + dz, MOZAIEK)
    pad((41, 21), (31, 29))
    pad((17, 35), (22, 30))
    for a, b in (((C, 0), (C, 18)), ((C, 34), (C, D - 1)), ((0, C), (18, C)), ((34, C), (W - 1, C))):
        pad(a, b)


def podium(s, rng):
    kleuren = ["minecraft:pink_carpet", "minecraft:magenta_carpet", "minecraft:yellow_carpet", "minecraft:pink_carpet", "minecraft:orange_carpet"]
    for x in range(C - 8, C + 9):
        for z in range(C - 8, C + 9):
            d = math.hypot(x - C, z - C)
            if d <= 6.2:
                s.set(x, G + 1, z, MOZAIEK if d < 5.4 else PAAL, None if d < 5.4 else {"axis": "y"})
                if 1.2 < d < 2.8:
                    ang = math.atan2(z - C, x - C)
                    s.set(x, G + 2, z, kleuren[int((ang + math.pi) / (2 * math.pi) * 10) % len(kleuren)])
            elif d <= 7.3:
                plaat(s, x, G + 1, z, MOZAIEK_PLAAT)
    s.set(*MAT, "guhs:tiki_bloemenmat")
    # the flower arch behind Lilo-guh (north), garlands on its beam, flowering azalea on top
    for x in (21, 31):
        paal(s, x, G + 1, G + 6, 19)
    for x in range(21, 32):
        s.set(x, G + 7, 19, PAAL, {"axis": "x"})
        if 22 <= x <= 30:
            tiki(s, x, G + 7, 20, "tiki_bloemenslinger", "south")
        if x % 2 == 1:
            blad(s, x, G + 8, 19, "minecraft:flowering_azalea_leaves")
    for x in (21, 31):
        blad(s, x, G + 8, 19, "minecraft:flowering_azalea_leaves")
        tiki(s, x, G + 5, 20, "tiki_masker_roze", "south")
    # tiki statues guarding the arch, torches all round
    tiki(s, 19, G + 1, 21, "tiki_beeld", "south")
    tiki(s, 33, G + 1, 21, "tiki_beeld", "south")
    for k in range(12):
        a = k / 12 * math.pi * 2
        x, z = round(C + math.cos(a) * 8.6), round(C + math.sin(a) * 8.6)
        if z < 21 and 20 <= x <= 32:
            continue                                        # (not in front of the arch)
        s.set(x, G + 1, z, "guhs:tiki_fakkel")


def paviljoen(s):
    """A Hawaiian hale over the podium: eight bamboo posts and a big round roof of thatch, a flowering crown on top."""
    for k in range(8):
        a = math.radians(22.5 + k * 45)
        x, z = round(C + math.cos(a) * 9.6), round(C + math.sin(a) * 9.6)
        paal(s, x, G + 1, G + 8, z)
    lagen = (11.2, 10.0, 8.6, 7.2, 5.8, 4.4, 3.0, 1.6)
    for layer, r in enumerate(lagen):
        y = G + 9 + layer
        for x in range(C - 12, C + 13):
            for z in range(C - 12, C + 13):
                d = math.hypot(x - C, z - C)
                if d <= r:
                    rand = d > r - 1.2 and layer < len(lagen) - 1
                    s.set(x, y, z, RIET_PLAAT if rand else RIET, {"type": "top", "waterlogged": "false"} if rand else None)
    for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
        blad(s, C + dx, G + 9 + len(lagen), C + dz, "minecraft:flowering_azalea_leaves")
    # shell lanterns hanging from the roof over the dance floor
    for k in range(6):
        a = math.radians(k * 60 + 30)
        x, z = round(C + math.cos(a) * 4.5), round(C + math.sin(a) * 4.5)
        y = G + 9
        while s.get(x, y, z) in (RIET, RIET_PLAAT) and y > G + 3:
            y -= 1
        tiki(s, x, y, z, "tiki_schelpjeslampion")


def gevelgezicht(s, x, facing):
    """A carved tiki-guh face in terracotta on a gable of the surf shack."""
    pal = {"skin": "minecraft:orange_terracotta", "ear": "minecraft:brown_terracotta", "ear_in": "minecraft:pink_terracotta",
           "eye": "minecraft:black_terracotta", "ring": "minecraft:white_terracotta", "shine": "minecraft:white_concrete",
           "nose": "minecraft:brown_terracotta", "mouth": "minecraft:red_terracotta", "cheek": "minecraft:pink_terracotta"}
    hulp.wall_face(s, 12, G + 6, x, 2.2, facing, pal)


def kano(s):
    """An outrigger canoe on the sand: a hull of spruce with pointed ends, a float on bamboo booms beside it."""
    z = 4
    for x in range(31, 37):
        s.set(x, G + 1, z, "minecraft:stripped_spruce_log", {"axis": "x"})
    trap(s, 30, G + 1, z, "east", block="minecraft:spruce_stairs")
    trap(s, 37, G + 1, z, "west", block="minecraft:spruce_stairs")
    for x in (32, 35):
        s.set(x, G + 2, z, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    for x in range(31, 37):
        s.set(x, G + 1, z + 3, "minecraft:stripped_bamboo_block", {"axis": "x"})
    for x in (32, 35):
        for dz in (1, 2):
            s.set(x, G + 1, z + dz, HEK, {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
    tiki(s, 38, G + 1, z + 1, "tiki_surfplankrek", "south")


def schelpjes(s, rng):
    """Shells (sea pickles) on the sand, and tiki torches along the paths."""
    n = 0
    while n < 26:
        x, z = rng.randrange(2, W - 2), rng.randrange(2, D - 2)
        if s.get(x, G, z) == SAND and s.get(x, G + 1, z) is None and math.hypot(x - C, z - C) > 12:
            s.set(x, G + 1, z, "minecraft:sea_pickle", {"pickles": str(rng.randint(1, 3)), "waterlogged": "false"})
            n += 1
    for t in (21, 15, 9, 3):
        for (x, z) in ((C - 2, t), (C + 2, D - 1 - t), (t, C + 2), (W - 1 - t, C - 2)):
            if s.get(x, G + 1, z) is None:
                s.set(x, G + 1, z, "guhs:tiki_fakkel")


def surfhut(s):
    x0, x1, z0, z1 = 35, 47, 8, 17
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 3):
            s.set(x, G, z, PLANKEN if z <= z1 else MOZAIEK)
    # walls: the back and the sides of bamboo planks, posts at the corners and the front; open windows in the sides
    for y in range(G + 1, G + 5):
        for x in range(x0, x1 + 1):
            s.set(x, y, z0, PLANKEN)
        for z in range(z0, z1 + 1):
            for x in (x0, x1):
                window = y in (G + 2, G + 3) and z in (11, 12, 13)
                s.set(x, y, z, AIR if window else PLANKEN)
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1), (39, z1), (43, z1)):
        paal(s, x, G + 1, G + 4, z)
    # the counter at the front (between the middle posts), with a radio and a board rack
    for x in range(36, 47):
        if x not in (39, 43):
            s.set(x, G + 1, z1, PLANKEN)
            plaat(s, x, G + 2, z1, MOZAIEK_PLAAT)
    for x in range(40, 43):
        s.set(x, G + 1, z1, AIR)                             # (the way in)
        s.set(x, G + 2, z1, AIR)
    # the steep gable roof of thatch (the ridge along x), its eaves out over the porch
    mid = (z0 - 1 + z1 + 2) / 2
    dak = {}
    for z in range(z0 - 1, z1 + 3):
        dz = min(z - (z0 - 1), (z1 + 2) - z)
        y = G + 5 + dz
        for x in range(x0 - 1, x1 + 2):
            if dz >= 6:
                s.set(x, y, z, RIET)
            else:
                trap(s, x, y, z, "south" if z < mid else "north")
                if dz >= 1:
                    s.set(x, y - 1, z, RIET)                    # (the thatch is thick: the steps hang together)
            dak[(x, z)] = y - (1 if 1 <= dz < 6 else 0)
    # the gables and the lintel over the open front: planks up to the roof
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                for y in range(G + 5, dak[(x, z)]):
                    if s.get(x, y, z) is None:
                        s.set(x, y, z, PLANKEN)
    # inside: racks full of boards, shell lanterns hanging from the roof
    for x in range(36, 47, 2):
        tiki(s, x, G + 1, z0 + 1, "tiki_surfplankrek", "south")
    for (x, z) in ((37, 11), (45, 11), (41, 12), (41, 15)):
        tiki(s, x, dak[(x, z)] - 1, z, "tiki_schelpjeslampion")
    for x in range(36, 39):
        plaat(s, x, G + 1, 14, MOZAIEK_PLAAT)
    gevelgezicht(s, x0, "west")
    gevelgezicht(s, x1, "east")
    # masks on the front posts, boards in the sand next to the porch
    tiki(s, 39, G + 3, z1 + 1, "tiki_masker", "south")
    tiki(s, 43, G + 3, z1 + 1, "tiki_masker", "south")
    for x in (34, 48):
        tiki(s, x, G + 1, 19, "tiki_surfplankrek", "south")


def tikistal(s):
    cx, cz = 13, 39
    for x in range(cx - 7, cx + 8):
        for z in range(cz - 7, cz + 8):
            d = math.hypot(x - cx, z - cz)
            if d <= 3.5:
                s.set(x, G, z, PLANKEN)
            elif d <= 4.5:
                ang = math.degrees(math.atan2(z - cz, x - cx))
                opening = -80 < ang < -10                          # (the way in, towards the podium)
                s.set(x, G, z, PLANKEN)
                if not opening:
                    s.set(x, G + 1, z, PLANKEN)
                    plaat(s, x, G + 2, z, MOZAIEK_PLAAT)
            elif 5.5 <= d <= 6.2 and (int(d * 7 + math.atan2(z - cz, x - cx) * 5) % 3 == 0):
                if not (-80 < math.degrees(math.atan2(z - cz, x - cx)) < -10):
                    tiki(s, x, G + 1, z, "tiki_kruk", "north")
    # posts and the conical thatched roof
    for (dx, dz) in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        paal(s, cx + dx, G + 1, G + 4, cz + dz)
        tiki(s, cx + dx + (1 if dx > 0 else -1), G + 3, cz + dz, "tiki_masker", "east" if dx > 0 else "west")
    for layer, r in enumerate((5.4, 4.3, 3.2, 2.1, 1.0)):
        y = G + 5 + layer
        for x in range(cx - 6, cx + 7):
            for z in range(cz - 6, cz + 7):
                d = math.hypot(x - cx, z - cz)
                if d <= r:
                    s.set(x, y, z, RIET if d <= r - 1.1 or layer == 4 else RIET_PLAAT, None if d <= r - 1.1 or layer == 4 else
                          {"type": "top", "waterlogged": "false"})
    s.set(cx, G + 10, cz, "minecraft:flowering_azalea_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})
    # the radio and lanterns
    tiki(s, cx + 2, G + 3, cz + 3, "tiki_radiootje", "north")
    for (dx, dz) in ((-2, 0), (2, 0), (0, 2)):
        tiki(s, cx + dx, G + 4, cz + dz, "tiki_schelpjeslampion")
    s.set(cx, G + 5, cz + 2, RIET)                             # (they hang from the roof: make sure there is thatch above)


def lifeguard(s):
    x, z = 12, 12
    for (dx, dz) in ((0, 0), (2, 0), (0, 2), (2, 2)):
        paal(s, x + dx, G + 1, G + 3, z + dz)
    for dx in range(3):
        for dz in range(3):
            s.set(x + dx, G + 4, z + dz, PLANKEN)
    trap(s, x + 1, G + 5, z + 1, "north", block="minecraft:bamboo_stairs")
    hek(s, x + 1, G + 5, z + 2)
    hek(s, x + 1, G + 6, z + 2)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            s.set(x + 1 + dx, G + 7, z + 2 + dz, "minecraft:red_wool" if (dx + dz) % 2 == 0 else "minecraft:white_wool")
    for y in range(G + 1, G + 4):
        s.set(x + 1, y, z, PLANKEN)
        s.set(x + 1, y, z - 1, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})


def parasols(s, rng):
    for (x, z, kleur) in ((38, 37, "pink"), (44, 43, "cyan"), (36, 45, "yellow")):
        for y in range(G + 1, G + 4):
            hek(s, x, y, z)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                s.set(x + dx, G + 4, z + dz, f"minecraft:{kleur}_wool" if (dx == 0 or dz == 0) else "minecraft:white_wool")
        s.set(x, G + 5, z, f"minecraft:{kleur}_carpet")
        # a towel next to it
        for k in range(3):
            s.set(x + 2, G + 1, z - 1 + k, f"minecraft:{kleur}_carpet" if k != 1 else "minecraft:white_carpet")
    # a sand castle with little towers
    x, z = 44, 33
    for dx in range(4):
        for dz in range(4):
            s.set(x + dx, G + 1, z + dz, "minecraft:sandstone")
    for (dx, dz) in ((0, 0), (3, 0), (0, 3), (3, 3)):
        s.set(x + dx, G + 2, z + dz, "minecraft:sandstone_wall", {"up": "true", "north": "none", "east": "none", "south": "none",
                                                                 "west": "none", "waterlogged": "false"})
    s.set(x + 1, G + 2, z + 1, "minecraft:chiseled_sandstone")
    s.set(x + 1, G + 3, z + 1, "minecraft:pink_banner", {"rotation": "0"})


def palm(s, x, z, hoogte, lean, rng):
    """A palm tree: a curved jungle log trunk leaning out, a star of drooping fronds, a few coconuts (cocoa) under the crown."""
    lx, lz = lean
    px, pz = x, z
    top = None
    for k in range(hoogte):
        f = (k / hoogte) ** 2
        nx, nz = x + round(lx * f * 3), z + round(lz * f * 3)
        if (nx, nz) != (px, pz) and k > 0:
            s.set(nx, G + k, pz, STAM, {"axis": "x"})          # (the trunk bends: a knot joins the two)
            s.set(nx, G + k, nz, STAM, {"axis": "z"})
        px, pz = nx, nz
        s.set(px, G + 1 + k, pz, STAM, {"axis": "y"})
        top = (px, G + 1 + k, pz)
    tx, ty, tz = top
    blad(s, tx, ty + 1, tz)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
        lengte = 4 if dx == 0 or dz == 0 else 3
        px, py, pz = tx, ty + 1, tz
        for i in range(1, lengte + 1):
            y = ty + 1 if i < lengte - 1 else ty               # (the fronds droop at their tips)
            nx, nz = tx + dx * i, tz + dz * i
            blad(s, px + dx, py, pz)                           # (a leaf chain that hangs together)
            blad(s, nx, py, nz)
            blad(s, nx, y, nz)
            px, py, pz = nx, y, nz
    for d, face in (((1, 0), "west"), ((-1, 0), "east"), ((0, 1), "north"), ((0, -1), "south")):
        if rng.random() < 0.7:
            s.set(tx + d[0], ty, tz + d[1], "minecraft:cocoa", {"facing": face, "age": "2"})


def tuintjes(s, rng):
    bloemen = ["minecraft:pink_tulip", "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip", "minecraft:orange_tulip"]
    for (cx, cz) in ((18, 16), (34, 34), (8, 28), (46, 24), (26, 44)):
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if math.hypot(x - cx, z - cz) <= 2.3 and s.get(x, G + 1, z) is None and s.get(x, G, z) == SAND:
                    s.set(x, G, z, "minecraft:grass_block", {"snowy": "false"})
                    if rng.random() < 0.7:
                        s.set(x, G + 1, z, rng.choice(bloemen))
        s.set(cx, G + 1, cz, "minecraft:flowering_azalea")


def entities(s, h):
    def npc(pos, kind, yaw, plek=None):
        nbt = {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": h.ms.Byte(1), "Rotation": h.ms.floats(float(yaw), 0.0)}
        if plek:
            nbt["RoleData"] = {"guhs_plek": plek}
        s.entity(pos[0], float(pos[1]), pos[2], nbt)
    npc(HULA_LILO, "lilo_guh", 0.0, "hula")
    npc(SURF_LILO, "lilo_guh", 0.0, "surf")
    npc(TIKIGUH, "tikiguh", -135.0)


def build(h):
    rng = random.Random(20301203)
    s = h.Structure((W, H, D))
    grond(s, rng)
    paden(s)
    podium(s, rng)
    paviljoen(s)
    surfhut(s)
    tikistal(s)
    lifeguard(s)
    parasols(s, rng)
    for (x, z, hoogte, lean) in ((4, 19, 8, (-1, 0)), (6, 46, 9, (-1, 1)), (22, 49, 7, (0, 1)), (49, 30, 8, (1, 0)), (47, 48, 9, (1, 1)),
                                  (29, 3, 8, (0, -1)), (3, 5, 7, (-1, -1)), (50, 6, 8, (1, -1))):
        palm(s, x, z, hoogte, lean, rng)
    tuintjes(s, rng)
    kano(s)
    schelpjes(s, rng)
    s.set(C, G, C, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": SAND, "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    entities(s, h)
    return s


def check(s):
    problems = []
    floating = hulp.check_floating(s, G)
    if floating:
        problems.append(f"floating: {floating[:8]} ({len(floating)})")
    if s.get(C, G, C) != "minecraft:jigsaw":
        problems.append("no anchor jigsaw")
    if s.get(*MAT) != "guhs:tiki_bloemenmat":
        problems.append("no flower mat")
    starts = [(x, G + 1, 0) for x in range(W)] + [(0, G + 1, z) for z in range(D)]
    reach = hulp.walk(s, starts, extra_passable=("guhs:tiki_bloemenmat", "guhs:tiki_bloemenslinger"))
    for name, (x, y, z) in (("hula Lilo", HULA_LILO), ("surf Lilo", SURF_LILO), ("Tikiguh", TIKIGUH), ("the mat", (MAT[0], MAT[1], MAT[2]))):
        x, y, z = int(x), int(y), int(z)
        if s.get(x, y - 1, z) in (None, AIR):
            problems.append(f"{name} doesn't stand on a floor")
        if s.get(x, y, z) not in (None, AIR, "guhs:tiki_bloemenmat") or s.get(x, y + 1, z) not in (None, AIR):
            problems.append(f"{name} is stuck in a block: {s.get(x, y, z)} / {s.get(x, y + 1, z)}")
        if not hulp.near_reachable(reach, x, y, z, 3):
            problems.append(f"{name} can't be reached")
    if problems:
        raise SystemExit("guhwaii_surfstrand check failed:\n  " + "\n  ".join(problems))
    return len(reach)


if __name__ == "__main__":
    import make_v2 as h
    st = build(h)
    print("walkable", check(st))
