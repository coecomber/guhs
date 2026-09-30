"""
The Guh Beauty Vads-wedstrijd (2.4): the beauty contest in the Guh Beauty Theater (guh_beauty_theater).

  - the theatre: a grand catwalk theatre (81 x 101): a guh-face facade (the door is its mouth), a foyer with a guh-face
    floor mosaic, a stage in the shape of a guh face behind a proscenium with drapes, a runway with lights, tiered
    audience benches, the jury desk with three jury guhs and a dressing room with mirrors and loaner wardrobes
  - the Showguh (a lilac sitting guh), the showrozet, the loaner wardrobe block, the stage markers
  - the Showster outfit (only in the Showguh's shop): a tiara, a "Miss Vadsig" sash and a glitter bow
  - advancements, lang (Dutch jokes included) and the FTB quests (row y=26)

The Java side lives in nl.juiced.guhs.feature.beauty.
"""
import math
import os
import random

import numpy as np
from PIL import Image

STRUCT = "guh_beauty_theater"
CLOTHES = ["showster_tiara", "showster_sjerp", "showster_strik"]

# --- guh clothes bones (make_guh_variants format: bone: (parent, pivot, swatch, [(origin, size, inflate)])) --------------
_HEAD, _BODY = [0, 6, -2], [0, 6, 6]
BONES = {
    # a little golden tiara on the front of the head, with a pink gem in the middle
    "outfit_tiara": ("head", _HEAD, "tiara", [([-4, 15, -9.6], [8, 0.8, 1], 0), ([-1, 15.8, -9.6], [2, 2.2, 1], 0),
                                            ([-3.6, 15.8, -9.6], [1.2, 1.3, 1], 0), ([2.4, 15.8, -9.6], [1.2, 1.3, 1], 0),
                                            ([-4, 15, -8.6], [0.8, 0.8, 3.6], 0), ([3.2, 15, -8.6], [0.8, 0.8, 3.6], 0)]),
    "outfit_tiara_gem": ("head", _HEAD, "tiara_gem", [([-0.6, 16.4, -9.9], [1.2, 1.2, 0.4], 0)]),
    # the "Miss Vadsig" sash: over the back and diagonally down both flanks, with a golden knot on the hip
    "outfit_sash": ("body", _BODY, "sash", [([-7.1, 11.2, 0.6], [14.2, 0.5, 2.4], 0)]
                    + [([7.0, 9.2 - 2 * i, 0.6 + 1.1 * i], [0.5, 2.1, 2.4], 0) for i in range(4)]
                    + [([-7.5, 9.2 - 2 * i, 0.6 - 1.1 * i], [0.5, 2.1, 2.4], 0) for i in range(4)]),
    "outfit_sash_knot": ("body", _BODY, "sash_knot", [([7.2, 2.4, 4.4], [0.7, 2.4, 2.4], 0), ([7.3, 0.6, 4.8], [0.5, 1.8, 0.6], 0),
                                                     ([7.3, 0.6, 6.0], [0.5, 1.8, 0.6], 0)]),
    # a big glitter bow under the chin, with two ribbon tails
    "outfit_glitter_bow": ("head", _HEAD, "glitter_bow", [([-4.6, 0.4, -11.4], [3.7, 3.2, 1], 0), ([0.9, 0.4, -11.4], [3.7, 3.2, 1], 0),
                                                        ([-0.9, 0.9, -11.8], [1.8, 2.2, 1.2], 0), ([-2.4, -1.9, -11.2], [1, 2.3, 0.6], 0),
                                                        ([1.4, -1.9, -11.2], [1, 2.3, 0.6], 0)]),
}


def _glitter(v, rng, base, spark, density=0.12, noise=10):
    a = v.fabric(base, rng, noise)
    mask = rng.random(a.shape[:2]) < density
    a[mask] = spark
    return a


def _sash(v, rng):
    a = v.fabric((250, 120, 185), rng, 6)
    a[0:4, :] = (245, 200, 70)
    a[28:32, :] = (245, 200, 70)
    for x in range(3, 29, 3):                     # "MISS VADSIG" in tiny white letters (well... dashes)
        a[14:17, x:x + 2] = (255, 250, 252)
    return a


def clothes(rng, v):
    return {
        "showster_tiara": {"tiara": lambda: v.metal((236, 200, 96), rng),
                           "tiara_gem": lambda: _glitter(v, rng, (255, 80, 170), (255, 235, 250), 0.18)},
        "showster_sjerp": {"sash": lambda: _sash(v, rng), "sash_knot": lambda: _glitter(v, rng, (240, 195, 70), (255, 250, 220))},
        "showster_strik": {"glitter_bow": lambda: _glitter(v, rng, (232, 70, 175), (255, 236, 250), 0.16)},
    }


def icons(ic):
    tiara = ["................", "................", ".......c........", "......aca.......", ".a...abcba...a..", ".aa..abbba..aa..",
             ".aba.abbba.aba..", ".abbaabbbaabba..", ".abbbbbbbbbbba..", ".aaaaaaaaaaaaa..", "................"]
    sash = ["aa..............", "abba............", ".abbba..........", "..abwba.........", "...abbba........", "....abwba.......",
            ".....abbba......", "......abwba.....", ".......abbba....", "........abbba...", ".........abbcc..", "..........acddc.",
            "...........cddc.", "...........ccc..", "..........c..c..", "..........c..c.."]
    bow = ["..aa.......aa...", ".abba.....abba..", ".abbba...abbba..", ".abwbbaaabbwba..", ".abbbbacabbbba..", ".abbwbacabwbba..",
           ".abbbbaaabbbba..", ".abbba.a.abbba..", ".abba.aba.abba..", "..aa..aba..aa...", "......aba.......", ".....ab.ba......",
           ".....a...a......"]
    gold = {"a": (190, 140, 30), "b": (250, 210, 80), "c": (255, 80, 170)}
    return {
        "showster_tiara": ic.icon(ic.pad(tiara), gold),
        "showster_sjerp": ic.icon(sash, {"a": (245, 200, 70), "b": (250, 120, 185), "w": (255, 250, 252), "c": (230, 180, 50),
                                         "d": (255, 90, 170)}),
        "showster_strik": ic.icon(ic.pad(bow), {"a": (150, 20, 100), "b": (235, 70, 175), "w": (255, 240, 250), "c": (250, 210, 80)}),
    }


# --- textures of the items and blocks --------------------------------------------------------------------------------------
def rozet_icon():
    img = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 6.5)
            angle = math.atan2(y - 6.5, x - 7.5)
            if r < 2.0:
                img[y, x] = (255, 250, 245, 255)
            elif r < 3.4:
                img[y, x] = (245, 200, 70, 255)
            elif r < 6.4:
                pleat = int((angle + math.pi) / (2 * math.pi) * 14) % 2
                img[y, x] = ((250, 120, 185, 255) if pleat else (225, 70, 160, 255)) if r < 5.6 else (160, 40, 110, 255)
    for (x0, x1) in ((4, 6), (9, 11)):                       # the two ribbon tails with a V cut
        for y in range(11, 16):
            for x in range(x0, x1 + 1):
                if not (y == 15 and x == (x0 + x1) // 2) and img[y, x, 3] == 0:
                    img[y, x] = (245, 200, 70, 255) if x in (x0, x1) else (250, 120, 185, 255)
    img[6, 6] = img[6, 9] = (40, 20, 30, 255)                # a tiny guh face in the middle: two eyes and a mouth
    img[7, 7] = img[7, 8] = (255, 120, 170, 255)
    return Image.fromarray(img)


def leenkast_textures(h):
    wood = np.asarray(h.noise_tex((236, 160, 190), 8, 2404)).copy()
    front = wood.copy()
    front[0, :] = front[15, :] = front[:, 0] = front[:, 15] = (196, 104, 146, 255)
    front[:, 7:9] = (196, 104, 146, 255)
    for y in range(2, 14):
        for x in list(range(2, 6)) + list(range(10, 14)):
            front[y, x] = (250, 200, 222, 255)
    front[8, 6] = front[8, 9] = (250, 205, 60, 255)                     # golden knobs
    for (x, y) in ((3, 4), (4, 3), (4, 4), (12, 4), (11, 3), (11, 4)):  # a little coat hanger on each door
        front[y, x] = (250, 205, 60, 255)
    front[3, 3] = front[3, 12] = (250, 205, 60, 255)
    side = wood.copy()
    side[0, :] = side[15, :] = side[:, 0] = side[:, 15] = (196, 104, 146, 255)
    top = wood.copy()
    top[:, 0] = top[:, 15] = top[0, :] = top[15, :] = (196, 104, 146, 255)
    for x in (5, 6, 9, 10):                                               # guh ears on top
        top[3:6, x] = (240, 120, 170, 255)
    return Image.fromarray(front), Image.fromarray(side), Image.fromarray(top)


# --- the theatre ---------------------------------------------------------------------------------------------------------------
W, H, D = 81, 32, 101
X0, X1, Z0, Z1 = 4, 76, 4, 96          # the outer walls
TOP = 21                                # the ceiling (and roof)
STAGE = 2                               # the stage / runway / dressing room floor (block y); you stand at STAGE + 1
FACE = (40, 27, 20.5, 11.2)             # the guh-face stage: centre x, z and radii
EARS = [(27, 18, 5.3), (53, 18, 5.3)]
RUNWAY = (38, 42)                       # runway x range (from the chin of the stage to the end)
END = (40, 64)                          # the end of the catwalk (where the model poses)
END_R = 3.6
MODEL = (40, 27)                        # where the model is dressed: the middle of the face
SHOWGUH = (35.5, 34.5)
JURY_Z = 71
BENCH_Z = (43, 63)
AUDIENCE = [(31, 1, 45), (27, 2, 53), (33, 0, 60), (49, 1, 46), (53, 2, 54), (47, 0, 61)]   # audience guhs on the benches (x, tier, z)
PASSABLE_PREFIX = ("minecraft:air", "minecraft:light", "guhs:beauty_plek", "guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes",
                   "guhs:vlaggetjes", "minecraft:pink_carpet", "minecraft:magenta_carpet", "minecraft:white_carpet")


def in_face(x, z):
    cx, cz, rx, rz = FACE
    return ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2 <= 1.0


def ear(x, z):
    for (ex, ez, r) in EARS:
        d = math.hypot(x - ex, z - ez)
        if d <= r:
            return d / r
    return None


def stage_cells():
    return {(x, z) for x in range(W) for z in range(15, 40) if in_face(x, z) or ear(x, z) is not None}


def runway_cells():
    cells = set()
    for x in range(RUNWAY[0], RUNWAY[1] + 1):
        for z in range(30, END[1] + 1):
            cells.add((x, z))
    for x in range(W):
        for z in range(55, 72):
            if math.hypot(x - END[0], z - END[1]) <= END_R:
                cells.add((x, z))
    return cells


def theater(h):
    mc, ms = h.mc, h.ms
    s = h.Structure((W, H, D))
    rng = random.Random(2404)
    stair = lambda facing: {"facing": facing, "half": "bottom", "shape": "straight", "waterlogged": "false"}
    fence_x = {"east": "true", "west": "true", "north": "false", "south": "false", "waterlogged": "false"}
    fence_z = {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"}
    lamp = lambda hanging: {"hanging": "true" if hanging else "false", "waterlogged": "false"}

    # --- the plate: the floor inside; outside a promenade, flower beds, hedges, lamp posts and trees (see grounds) ---
    grounds(h, s, rng)

    # --- the shell: walls with pilasters, arched windows, bunting and a golden trim; the ceiling full of lights ---
    for y in range(1, TOP):
        for side in "NSWE":
            lo, hi = wall_span(side)
            for a in range(lo, hi + 1):
                x, z = wall_xz(side, a, 0)
                block, props = wall_block(mc, side, a - lo, y)
                s.set(x, y, z, block, props)
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            edge = x in (X0, X1) or z in (Z0, Z1)
            light = not edge and (x - X0) % 6 == 3 and (z - Z0) % 6 == 3
            s.set(x, TOP, z, mc("pearlescent_froglight") if light else mc("gold_block") if edge else mc("white_concrete"),
                  {"axis": "y"} if light else None)

    # --- the back: the dressing room and backstage, raised to stage height, behind a curtain wall ---
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, 18):
            for y in range(1, STAGE):
                s.set(x, y, z, mc("white_concrete"))
            s.set(x, STAGE, z, (mc("pink_wool") if (x + z) % 2 else mc("white_wool")) if z < 14 else mc("cherry_planks"))
    for x in range(X0 + 1, X1):                                    # the curtain wall between the stage and the dressing room
        for y in range(STAGE + 1, TOP):
            fold = x % 3 == 0
            s.set(x, y, 14, mc("yellow_wool") if y == STAGE + 1 else mc("pink_wool") if fold else mc("magenta_wool"))
    for dx in (33, 34, 46, 47):                                    # two doorways (with golden frames)
        for y in range(STAGE + 1, STAGE + 4):
            s.set(dx, y, 14, mc("air"))
    for (a, b) in ((32, 35), (45, 48)):
        for y in range(STAGE + 1, STAGE + 5):
            s.set(a, y, 14, mc("gold_block"))
            s.set(b, y, 14, mc("gold_block"))
        for x in range(a, b + 1):
            s.set(x, STAGE + 4, 14, mc("gold_block"))
    dressing_room(h, s, rng)

    # --- the stage: a guh face (with ears) seen from above, with a golden rim ---
    cells = stage_cells()
    for (x, z) in cells:
        s.set(x, 1, z, mc("quartz_block"))
        s.set(x, STAGE, z, stage_block(mc, x, z, cells))
    # the runway (lit along its edges) and the round end platform
    rw = runway_cells() - cells
    for (x, z) in rw:
        s.set(x, 1, z, mc("quartz_block"))
        round_end = math.hypot(x - END[0], z - END[1]) <= END_R
        if round_end:
            rim = math.hypot(x - END[0], z - END[1]) > END_R - 1.1
            top = mc("gold_block") if rim else mc("pink_concrete") if (x + z) % 2 else mc("white_concrete")
        elif x in RUNWAY:
            top = mc("sea_lantern") if z % 4 == 0 else mc("magenta_concrete")
        else:
            top = mc("white_concrete")
        s.set(x, STAGE, z, top)
    # steps up to the runway, on both sides just in front of the stage
    for z in (39, 40, 41):
        s.set(RUNWAY[0] - 1, 1, z, mc("quartz_block"))
        s.set(RUNWAY[0] - 1, STAGE, z, mc("cherry_stairs"), stair("east"))
        s.set(RUNWAY[0] - 2, 1, z, mc("cherry_stairs"), stair("east"))
        s.set(RUNWAY[1] + 1, 1, z, mc("quartz_block"))
        s.set(RUNWAY[1] + 1, STAGE, z, mc("cherry_stairs"), stair("west"))
        s.set(RUNWAY[1] + 2, 1, z, mc("cherry_stairs"), stair("west"))
    # the stage markers, the loaner wardrobes on the stage and the spotlights above it
    s.set(MODEL[0], STAGE + 1, MODEL[1], "guhs:beauty_plek", {"spot": "model"})
    s.set(END[0], STAGE + 1, END[1], "guhs:beauty_plek", {"spot": "einde"})
    for x in (23, 57):
        s.set(x, STAGE + 1, 19, "guhs:beauty_leenkast", {"facing": "south"})
        s.set(x + (1 if x < 40 else -1), STAGE + 1, 18, "guhs:potted_roze_guhbloem")
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            s.set(MODEL[0] + dx, TOP - 1, MODEL[1] + dz, "guhs:guh_kristal_lamp")
    for z in range(40, END[1] + 1, 6):                            # spotlights over the catwalk
        for x in (RUNWAY[0] - 1, RUNWAY[1] + 1):
            s.set(x, TOP - 1, z, mc("end_rod"), {"facing": "down"})
    proscenium(h, s)

    # --- the audience: tiered benches on both sides of the runway (front row: vadszakken), with rails at the ends ---
    for side in (-1, 1):
        for r in range(0, 14):
            xb = 35 - 2 * r if side < 0 else 45 + 2 * r
            t = r // 2
            facing = "east" if side < 0 else "west"
            for x in (xb, xb + side):
                if not (X0 < x < X1):
                    continue
                for z in range(BENCH_Z[0] - 1, BENCH_Z[1] + 2):
                    for y in range(1, t + 1):
                        s.set(x, y, z, "guhs:guhbloesem_planks" if y == t else mc("white_concrete"))
                    if t >= 1 and z in (BENCH_Z[0] - 1, BENCH_Z[1] + 1):
                        s.set(x, t + 1, z, mc("cherry_fence"), fence_x)
            for z in range(BENCH_Z[0], BENCH_Z[1] + 1):
                if z in (50, 57):
                    continue                                          # aisles
                block = "guhs:pink_zitzak" if r == 0 else "guhs:guh_bank"
                s.set(xb, t + 1, z, block, {"facing": facing})
        # the top tier runs on to the wall (no drop behind the last row)
        last = 35 - 2 * 13 + side if side < 0 else 45 + 2 * 13 + side
        for x in (range(X0 + 1, last) if side < 0 else range(last + 1, X1)):
            for z in range(BENCH_Z[0] - 1, BENCH_Z[1] + 2):
                for y in range(1, 7):
                    s.set(x, y, z, "guhs:guhbloesem_planks" if y == 6 else mc("white_concrete"))
                if z in (BENCH_Z[0] - 1, BENCH_Z[1] + 1):
                    s.set(x, 7, z, mc("cherry_fence"), fence_x)
    # a carpet from the foyer around the jury desk to both aisles
    for x in range(30, 51):
        for z in (74, 75):
            s.set(x, 1, z, mc("magenta_carpet"))
    for x in (30, 31, 49, 50):
        for z in range(65, 74):
            s.set(x, 1, z, mc("magenta_carpet"))

    # --- the jury desk: a golden dais with a long table and three jury guhs on cushions ---
    for x in range(33, 48):
        for z in range(JURY_Z - 2, JURY_Z + 3):
            rim = x in (33, 47) or z in (JURY_Z - 2, JURY_Z + 2)
            s.set(x, 1, z, mc("gold_block") if rim else mc("white_concrete"))
    for x in range(34, 47):
        s.set(x, 2, JURY_Z - 1, "guhs:guh_tafel", {"facing": "north"})
    s.set(35, 3, JURY_Z - 1, "guhs:potted_knabbelroos")
    for x in (38, 42):
        s.set(x, 3, JURY_Z - 1, "guhs:lampion_geel", lamp(False))
    jury = [("vadsma", "snow", {"ClothesEyes": "monocle", "ClothesNeck": "black_bowtie"}),
            ("glitterguh", "mint", {"ClothesEyes": "heart_glasses", "ClothesHead": "party_hat", "ClothesNeck": "red_bowtie"}),
            ("knabbel", "choco", {"ClothesHead": "rain_hat", "ClothesNeck": "winter_scarf"})]
    for i, (name, variant, clothes_) in enumerate(jury):
        x = 36 + 4 * i
        s.set(x, 2, JURY_Z, "guhs:pink_kussen", {"facing": "north"})
        s.entity(x + 0.5, 2.25, JURY_Z + 0.5, ms.guh_nbt(0.95, NoAI=h.Byte(1), Invulnerable=h.Byte(1), Sitting=h.Byte(1),
                                                         Variant=variant, CustomName='{"translate":"entity.guhs.beauty_jury.%s"}' % name,
                                                         Tags=ms.NbtList(8, ["guhs_beauty_jury", f"guhs_beauty_jury{i + 1}"]),
                                                         Rotation=h.floats(180.0, 0.0), **clothes_))
    # a sign of lampgions behind the jury
    for x in range(34, 47, 3):
        s.set(x, 2, JURY_Z + 2, mc("cherry_fence"), fence_x)
        s.set(x, 3, JURY_Z + 2, ("guhs:lampion_roze", "guhs:lampion_mint", "guhs:lampion_geel")[x % 3], lamp(False))

    # --- the audience guhs (they cheer for good scores) ---
    for (x, t, z), variant in zip(AUDIENCE, ("normal", "mint", "snow", "choco", "normal", "mint")):
        yaw = 270.0 if x < 40 else 90.0
        s.entity(x + 0.5, t + 1.5, z + 0.5, ms.guh_nbt(0.8, NoAI=h.Byte(1), Invulnerable=h.Byte(1), Sitting=h.Byte(1), Variant=variant,
                                                      Tags=ms.NbtList(8, ["guhs_beauty_publiek"]), Rotation=h.floats(yaw, 0.0)))

    foyer(h, s, rng)
    exterior(h, s)
    facade(h, s)

    # --- the Showguh: on the stage, by the start of the catwalk, facing the audience ---
    s.entity(SHOWGUH[0], STAGE + 1.0, SHOWGUH[1], {"id": "guhs:guh_npc", "Kind": "showguh", "PersistenceRequired": h.Byte(1),
                                                    "Rotation": h.floats(0.0, 0.0)})
    s.set(int(SHOWGUH[0]) - 1, STAGE + 1, int(SHOWGUH[1]), "guhs:lampion_roze", lamp(False))

    # hanging lampgions all over the hall
    for x in range(10, 72, 10):
        for z in range(22, 92, 12):
            if s.get(x, TOP - 1, z) in (None, "minecraft:air"):
                s.set(x, TOP - 1, z, ("guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint")[(x + z) % 3], lamp(True))
    light_up(s)
    footprint = [(x, z) for x in range(W) for z in range(D)]
    s.clear_above(footprint, 1)
    return s


def light_up(s):
    """Invisible light blocks two blocks above every floor (every 5 blocks): a bright theatre, and no dark corners for
    anything to spawn in. (The ceiling lights are too high up to reach the floor.)"""
    for x in range(X0 + 2, X1 - 1, 5):
        for z in range(Z0 + 2, Z1 - 1, 5):
            for y in range(1, TOP - 3):
                if s.get(x, y, z) in (None, "minecraft:air") and solid(s, x, y - 1, z):
                    if all(s.get(x, y + k, z) in (None, "minecraft:air") for k in (1, 2)):
                        s.set(x, y + 2, z, "minecraft:light", {"level": "15", "waterlogged": "false"})
                    break


# --- the outside -----------------------------------------------------------------------------------------------------------------
# The four walls, each seen from outside: "N" the back, "S" the front (with the guh-face facade), "W" and "E" the long sides.
# Along a wall `a` is its x (N/S) or z (W/E); `out` is how far a relief sticks out of the wall (0 = the wall itself).
BAYS = {"N": "MBM---MBM", "S": "WM-----MW", "W": "WMWM---MWMW", "E": "WMWM---MWMW"}   # W window, M medallion, B banner, - big face
KINDS = {"W": "window", "M": "medallion", "B": "banner", "-": "face"}
BIG_FACES = {"N": (40, 11, 9.5), "W": (50, 12, 8.0), "E": (50, 12, 8.0)}             # centre a, y and radius of the big guh faces
MEDALLION = [".GG.GG.",                                                              # a guh head in a golden frame (top row first)
             "GMMGMMG",
             "GPPPPPG",
             "GKPPPKG",
             "GMPMPMG",
             "GKPKPKG",
             ".GKPKG.",
             "..GGG.."]


def wall_span(side):
    return (X0, X1) if side in "NS" else (Z0, Z1)


def wall_xz(side, a, out):
    return {"N": (a, Z0 - out), "S": (a, Z1 + out), "W": (X0 - out, a), "E": (X1 + out, a)}[side]


def bay(side, i):
    """What this spot along a wall is part of: ("pilaster", 0), (the bay's kind, the cell in the bay 1..7), or ("plain", 0)."""
    if side in "NS":
        if i % 8 == 0:
            return "pilaster", 0
        b, c = i // 8, i % 8
    else:                                           # the long walls (93 blocks): pilasters from 2 to 90, and on the corners
        if i in (0, Z1 - Z0) or (i - 2) % 8 == 0:
            return "pilaster", 0
        if i < 2 or i > 90:
            return "plain", 0
        b, c = (i - 2) // 8, (i - 2) % 8
    return KINDS[BAYS[side][b]], c


def paint_face(mc, put, ca, cy, r, thick_from):
    """A big guh face (ears, eyes with a shine, blush, nose, a :3 mouth) on a wall, centred on (ca, cy) with radius r.
    put(a, y, out, block) places a block; from y thick_from up the face is two blocks thick (so the ears stand firm)."""
    k = r / 13.6
    ex, ey, er = 11.5 * k, 12.5 * k, max(2.6, 4.6 * k)
    span = range(int(ca - r - er - 2), int(ca + r + er + 3))
    face = {(a, y) for a in span for y in range(1, H) if math.hypot(a - ca, y - cy) <= r}
    ears = {(a, y) for a in span for y in range(1, H) for sx in (-1, 1) if math.hypot(a - (ca + sx * ex), y - (cy + ey)) <= er}
    shape = face | ears
    for (a, y) in shape:
        edge = any((a + da, y + dy) not in shape and y + dy >= 1 for da, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        inner_ear = (a, y) not in face and min(math.hypot(a - ca - sx * ex, y - cy - ey) for sx in (-1, 1)) < er * 0.56
        block = mc("gold_block") if edge else mc("magenta_wool") if inner_ear else mc("pink_wool")
        u, v = a - ca, y - cy
        for sx in (-1, 1):
            eu, ev = sx * 6 * k, 2 * k
            if ((u - eu) / max(1.1, 1.7 * k)) ** 2 + ((v - ev) / max(1.6, 2.5 * k)) ** 2 <= 1:
                block = mc("white_concrete") if (round(u - eu), round(v - ev)) == (-1, 1) else mc("black_concrete")
            if ((u - sx * 10.5 * k) / max(1.3, 2.3 * k)) ** 2 + ((v + 2 * k) / max(0.8, 1.3 * k)) ** 2 <= 1:
                block = mc("magenta_wool")
        if abs(u) <= 1.2 and round(v) == round(-1 * k):
            block = mc("magenta_wool")
        mouth_v = round(-4 * k)
        if (round(v) == mouth_v and round(abs(u)) in (0, 3)) or (round(v) == mouth_v - 1 and round(abs(u)) in (1, 2)):
            block = mc("black_concrete")
        put(a, y, 1, block)
        if y >= thick_from:
            put(a, y, 0, block)


def cube_head(s, mc, x0, y0, z0, faces, n=5, hgt=4):
    """A blocky guh head (n x n, hgt high, two ears on top) with a face on each side in `faces` ("N", "S", "W", "E")."""
    rows = {5: {hgt - 1: "PPPPP", hgt - 2: "PKPKP", hgt - 3: "MPMPM", 0: "PPKPP"},
            3: {2: "PPP", 1: "KPK", 0: "MPM"}}[n]
    colour = {"P": "pink_wool", "K": "black_concrete", "M": "magenta_wool"}
    for dx in range(n):
        for dz in range(n):
            for dy in range(hgt):
                s.set(x0 + dx, y0 + dy, z0 + dz, mc("pink_wool"))
    for f in faces:
        for dy in range(hgt):
            line = rows.get(dy, "P" * n)
            for k in range(n):
                x, z = {"N": (x0 + k, z0), "S": (x0 + k, z0 + n - 1), "W": (x0, z0 + k), "E": (x0 + n - 1, z0 + k)}[f]
                s.set(x, y0 + dy, z, mc(colour[line[k]]))
    # the ears: left and right of a single face, or on the two side corners of a corner head
    if len(faces) == 1:
        mid = (n - 1) // 2
        corners = {"S": [(0, mid), (n - 1, mid)], "N": [(0, mid), (n - 1, mid)], "W": [(mid, 0), (mid, n - 1)], "E": [(mid, 0), (mid, n - 1)]}[faces]
    else:
        xs = 0 if "W" in faces else n - 1
        zs = 0 if "N" in faces else n - 1
        corners = [(n - 1 - xs, zs), (xs, n - 1 - zs)]
    for (cx, cz) in corners:
        s.set(x0 + cx, y0 + hgt, z0 + cz, mc("pink_wool"))
        s.set(x0 + cx, y0 + hgt + 1, z0 + cz, mc("magenta_wool"))


def grounds(h, s, rng):
    """The floor inside, and around the theatre: a pink-and-white promenade, flower beds along the walls, a blossom hedge
    with lamp posts, the path to the door with lampions, little blossom trees on the corners."""
    mc = h.mc
    fence = {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}
    garden = random.Random(7070)
    for x in range(W):
        for z in range(D):
            if X0 <= x <= X1 and Z0 <= z <= Z1:
                s.set(x, 0, z, "guhs:guhbloesem_planks")
                continue
            ring = min(x, z, W - 1 - x, D - 1 - z)
            path = 35 <= x <= 45 and z > Z1
            if not path and ring in (1, 2) and rng.random() < 0.3:
                rng.choice("abcd")                      # (the draws of the old garden: the rest of the theatre stays the same)
            if path:
                s.set(x, 0, z, mc("white_concrete") if 36 <= x <= 44 else mc("pink_concrete"))
            elif ring in (1, 2):
                s.set(x, 0, z, mc("white_concrete") if (x + z) % 4 < 2 else mc("pink_concrete"))
            else:
                s.set(x, 0, z, mc("grass_block"), {"snowy": "false"})
                if ring == 0 and (x + z) % 5:
                    s.set(x, 1, z, mc("flowering_azalea_leaves") if (x + z) % 3 else mc("azalea_leaves"),
                          {"persistent": "true", "distance": "7", "waterlogged": "false"})
                elif ring == 3 and garden.random() < 0.7:
                    s.set(x, 1, z, garden.choice(["guhs:roze_guhbloem", "guhs:knabbelroos", "guhs:kaasbloem", "guhs:guhoortjes"]))
    # lamp posts in the hedge, in line with every other pilaster
    posts = [(0, z) for z in range(Z0 + 2, Z1, 16)] + [(W - 1, z) for z in range(Z0 + 2, Z1, 16)] + [(x, 0) for x in range(X0, X1 + 1, 16)]
    for (x, z) in posts:
        for y in (1, 2, 3):
            s.set(x, y, z, mc("cherry_fence"), fence)
        s.set(x, 4, z, ("guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint")[(x + z) % 3], {"hanging": "false", "waterlogged": "false"})
    # lampions along the path to the door
    for z in (98, 99, 100):
        for x in (34, 46):
            s.set(x, 1, z, mc("cherry_fence"), fence)
            if z != 99:
                s.set(x, 2, z, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    # little blossom trees on the four corners
    blossom = {"persistent": "true", "distance": "1", "waterlogged": "false"}
    for (x, z) in ((1, 1), (W - 2, 1), (1, D - 2), (W - 2, D - 2)):
        for y in range(1, 5):
            s.set(x, y, z, "guhs:guhbloesem_log", {"axis": "y"})
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in (4, 5, 6):
                    if (dx or dz or dy > 4) and abs(dx) + abs(dz) + (dy - 5) * (dy - 5) <= (3 if dy == 5 else 1) + (dy == 4):
                        if not (X0 <= x + dx <= X1 and Z0 <= z + dz <= Z1):
                            s.set(x + dx, dy, z + dz, "guhs:guhbloesem_leaves", blossom)


def exterior(h, s):
    """Everything on the outside of the walls and on the roof (the front's big guh face itself is in facade)."""
    mc = h.mc
    art = {"G": "gold_block", "M": "magenta_wool", "P": "pink_wool", "K": "black_concrete"}
    # --- the walls: pilasters, window sills, guh medallions, banners, a cornice and a big guh face on the back and both sides ---
    for side in "NSWE":
        lo, hi = wall_span(side)

        def put(a, y, out, block, props=None, side=side):
            x, z = wall_xz(side, a, out)
            s.set(x, y, z, block, props)
        for a in range(lo, hi + 1):
            kind, c = bay(side, a - lo)
            put(a, 20, 1, mc("quartz_block"))                                  # the cornice
            if kind == "pilaster":
                for y in range(1, 20):
                    put(a, y, 1, mc("gold_block") if y >= 17 else mc("quartz_block") if y == 1 else mc("quartz_pillar"),
                        {"axis": "y"} if 1 < y < 17 else None)
            elif kind == "window" and 2 <= c <= 6:
                put(a, 4, 1, mc("gold_block"))                                 # the sill
            elif kind == "medallion":
                for row, line in enumerate(MEDALLION):
                    if line[c - 1] in art:
                        put(a, 13 - row, 1, mc(art[line[c - 1]]))
            elif kind == "banner" and 2 <= c <= 6:
                put(a, 13, 1, mc("gold_block"))                                # the rod
                if 3 <= c <= 5:
                    for y in range(4 if c == 4 else 5, 13):
                        block = mc("magenta_wool") if c != 4 else mc("pink_wool")
                        if (y == 10 and c in (3, 5)) or (y == 8 and c == 4):
                            block = mc("black_concrete")                       # a little guh face on the banner
                        if y <= 5:
                            block = mc("gold_block")                           # the tassel
                        put(a, y, 1, block)
        if side in BIG_FACES:
            ca, cy, r = BIG_FACES[side]
            paint_face(mc, put, ca, cy, r, TOP)
    # --- the roof: a striped big top in three tiers, with golden hips, a parapet of little guh heads and a guh face on top ---
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            dx_, dz_ = min(x - X0, X1 - x), min(z - Z0, Z1 - z)
            d = min(dx_, dz_)
            if d == 0:
                s.set(x, TOP + 1, z, mc("gold_block"))
                along = x - X0 if dz_ == 0 else z - Z0
                p = (along - 6) % 5
                if p in (0, 1, 2) and not (dx_ < 6 and dz_ < 6):
                    s.set(x, TOP + 2, z, mc("black_concrete") if p != 1 else mc("pink_concrete"))
                    if p != 1:
                        s.set(x, TOP + 3, z, mc("pink_concrete"))
                continue
            top = TOP + 1 + min((d - 1) // 3, 3)
            for y in range(TOP + 1, top):
                s.set(x, y, z, mc("white_concrete"))
            along = z - (Z0 + Z1) / 2 if dx_ < dz_ else x - (X0 + X1) / 2
            stripe = int((abs(along) + 2) // 4) % 2
            if d < 10:
                block = mc("gold_block") if dx_ == dz_ else mc("pink_concrete") if stripe else mc("white_concrete")
            elif d == 10:
                block = mc("sea_lantern") if (x + z) % 3 == 0 else mc("gold_block")     # a ring of marquee lights
            else:
                block = mc("pink_concrete") if x % 4 == 0 and z % 4 == 0 else mc("white_concrete")
            s.set(x, top, z, block)
    roof_face(s, mc)
    # the four corners: a big blocky guh head looking out both ways
    for (x0, z0, faces) in ((X0, Z0, "NW"), (X1 - 4, Z0, "NE"), (X0, Z1 - 4, "SW"), (X1 - 4, Z1 - 4, "SE")):
        for dx in range(5):
            for dz in range(5):
                s.set(x0 + dx, TOP + 1, z0 + dz, mc("gold_block"))
        cube_head(s, mc, x0, TOP + 2, z0, faces)
    # spotlights on the corners of the top tier
    for x in (X0 + 11, X1 - 11):
        for z in (Z0 + 11, Z1 - 11):
            s.set(x, TOP + 5, z, mc("gold_block"))
            s.set(x, TOP + 6, z, mc("sea_lantern"))
            s.set(x, TOP + 7, z, mc("end_rod"), {"facing": "up"})
    # --- the front: two marquee blades with bulbs and a guh head on top, fountains, two guh busts by the door ---
    for bx in (20, 60):
        for z in (97, 98, 99):
            for y in range(5, 20):
                rim = z == 99 or y in (5, 19)
                block = (mc("sea_lantern") if (y + z) % 2 else mc("gold_block")) if rim else \
                    mc("white_concrete") if y % 4 == 0 else mc("magenta_wool")
                s.set(bx, y, z, block)
        cube_head(s, mc, bx - 1, 20, 97, "S", n=3, hgt=3)
    for fx in (10, 70):                                                        # the fountains
        for x in range(fx - 3, fx + 4):
            for z in range(97, 101):
                rim = x in (fx - 3, fx + 3) or z in (97, 100)
                s.set(x, 0, z, mc("quartz_block"))
                s.set(x, 1, z, mc("quartz_block") if rim else mc("water"), None if rim else {"level": "0"})
                s.set(x, 2, z, mc("air"))
        for y in (1, 2):
            s.set(fx, y, 98, mc("quartz_pillar"), {"axis": "y"})
        cube_head(s, mc, fx - 1, 3, 97, "S", n=3, hgt=3)
    for sx in (29, 51):                                                        # guh busts by the door
        for x in range(sx - 1, sx + 2):
            for z in range(98, 101):
                s.set(x, 1, z, mc("gold_block"))
                s.set(x, 2, z, mc("quartz_block"))
        cube_head(s, mc, sx - 1, 3, 98, "S", n=3, hgt=3)
    # --- light on the roof: nothing spawns up there in the dark ---
    for x in range(X0 + 2, X1 - 1, 5):
        for z in range(Z0 + 2, Z1 - 1, 5):
            y = max(yy for yy in range(TOP, H) if s.get(x, yy, z) not in (None, "minecraft:air"))
            if y + 1 < H:
                s.set(x, y + 1, z, "minecraft:light", {"level": "15", "waterlogged": "false"})


def roof_face(s, mc):
    """A giant guh face on the top tier (read from the front: its ears at the back), outlined in neon, with standing ears."""
    y = TOP + 4
    cx, cz, rx, rz = 40, 54, 21, 25
    face = {(x, z) for x in range(W) for z in range(D) if ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2 <= 1}
    for (x, z) in face:
        block, raised = mc("pink_concrete"), None
        if any((x + dx, z + dz) not in face for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            raised = mc("pearlescent_froglight")
        for sx in (-1, 1):
            ex, ez = cx + sx * 9, cz - 7
            if ((x - ex) / 3.0) ** 2 + ((z - ez) / 4.0) ** 2 <= 1:
                raised = mc("white_concrete") if (x, z) == (ex - 1, ez - 2) else mc("black_concrete")
            if ((x - (cx + sx * 14)) / 3.5) ** 2 + ((z - (cz + 4)) / 2.2) ** 2 <= 1:
                block = mc("magenta_concrete")
        if (abs(x - cx) <= 1 and z == cz + 1) or (x, z) == (cx, cz + 2):
            block = mc("magenta_concrete")
        u = abs(x - cx)
        if u <= 6:
            mz = cz + 6 + (0 if u in (0, 6) else 1 if u in (1, 5) else 2)
            if z in (mz, mz + 1):
                block = mc("black_concrete")
        s.set(x, y, z, block)
        if raised:
            s.set(x, y + 1, z, raised, {"axis": "y"} if raised.endswith("froglight") else None)
    for sx in (-1, 1):                                             # the ears stand up at the back of the face
        ex, ez, rx, ry = cx + sx * 14, 37, 8.5, 6.4
        for x in range(ex - 9, ex + 10):
            for yy in range(y + 1, H):
                dd = math.hypot((x - ex) / rx, (yy - y) / ry)
                if dd <= 1:
                    rim = any(math.hypot((x + da - ex) / rx, (yy + dy - y) / ry) > 1 for da, dy in ((1, 0), (-1, 0), (0, 1)))
                    for z in (ez, ez + 1, ez + 2):
                        s.set(x, yy, z, mc("gold_block") if rim else mc("magenta_wool") if dd < 0.6 else mc("pink_wool"))


def wall_block(mc, side, i, y):
    """The wall itself (i along the wall, y up): a magenta base, quartz pilasters, arched windows in the window bays,
    a white band with bunting, a golden line and a white top. Returns (block, properties)."""
    kind, c = bay(side, i)
    if kind == "pilaster":
        return mc("quartz_pillar"), {"axis": "y"}
    if y == 1:
        return mc("magenta_concrete"), None
    if y == 19:
        return mc("gold_block"), None
    if 14 <= y <= 20:
        if y == 17 or (y == 16 and i % 3 == 1):
            return (mc("magenta_concrete") if (i // 3) % 2 else mc("yellow_concrete")), None
        return mc("white_concrete"), None
    if kind == "window":
        if (3 <= c <= 5 and 5 <= y <= 11) or (c == 4 and y == 12):
            return mc("pink_stained_glass"), None
        if (c in (2, 6) and 5 <= y <= 11) or (c in (3, 5) and y == 12) or (c == 4 and y == 13):
            return mc("white_concrete"), None
    return mc("pink_concrete"), None


def stage_block(mc, x, z, cells):
    """The mosaic on top of the stage: a guh face with ears, eyes with a shine, blushing cheeks, a nose and a ":3" mouth."""
    if any((x + dx, z + dz) not in cells for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
        return mc("gold_block")
    e = ear(x, z)
    if e is not None and not in_face(x, z):
        return mc("magenta_concrete") if e < 0.55 else mc("pink_concrete")
    for (ex, ez) in ((33, 24), (47, 24)):
        if ((x - ex) / 2.3) ** 2 + ((z - ez) / 2.8) ** 2 <= 1:
            return mc("white_concrete") if (x, z) == (ex - 1, ez - 1) else mc("black_concrete")
    for (bx, bz) in ((28, 30), (52, 30)):
        if ((x - bx) / 2.6) ** 2 + ((z - bz) / 1.6) ** 2 <= 1:
            return mc("magenta_concrete")
    if (x, z) in ((39, 29), (40, 29), (41, 29), (40, 30)):
        return mc("magenta_concrete")
    if (x, z) in ((37, 32), (38, 33), (39, 33), (40, 32), (41, 33), (42, 33), (43, 32)):
        return mc("black_concrete")
    return mc("pink_concrete")


def proscenium(h, s):
    """The golden frame in front of the stage: two pillars, a beam with a guh portrait, and pink drapes tied with gold."""
    mc = h.mc
    for x in (15, 16, 64, 65):
        for z in (36, 37):
            for y in range(1, 18):
                s.set(x, y, z, mc("quartz_pillar"), {"axis": "y"})
    for x in range(15, 66):
        for z in (36, 37):
            s.set(x, 17, z, mc("gold_block"))
            s.set(x, 18, z, mc("white_concrete"))
            s.set(x, 19, z, mc("white_concrete") if x % 4 else mc("gold_block"))
            s.set(x, 20, z, mc("white_concrete"))
    h.ms.guh_portrait(s, 37, 13, 37)                    # a guh looking down at the stage from the beam
    for x in range(37, 44):
        s.set(x, 12, 37, mc("gold_block"))
    for (x0, step) in ((17, 1), (63, -1)):             # the drapes: wide at the top, gathered at the tie, open below
        for y in range(6, 17):
            width = 7 if y >= 13 else 3 if y >= 10 else 2
            for k in range(width):
                x = x0 + step * k
                s.set(x, y, 37, mc("yellow_wool") if y == 10 else mc("magenta_wool") if k % 2 == 0 else mc("pink_wool"))


def dressing_room(h, s, rng):
    """Behind the curtain: mirrors with light bulbs, dressing tables, loaner wardrobes and cosy cushions."""
    mc = h.mc
    y0 = STAGE + 1
    for cx in (9, 17, 25, 55, 63, 71):
        for x in range(cx - 1, cx + 2):
            for y in range(y0 + 1, y0 + 4):
                s.set(x, y, Z0 + 1, mc("light_blue_stained_glass"))           # the mirror
            s.set(x, y0 + 4, Z0 + 1, mc("quartz_block"))
        for y in range(y0 + 1, y0 + 5):
            s.set(cx - 2, y, Z0 + 1, mc("sea_lantern") if y % 2 else mc("quartz_block"))   # the bulbs
            s.set(cx + 2, y, Z0 + 1, mc("sea_lantern") if y % 2 else mc("quartz_block"))
        for x in range(cx - 1, cx + 2):
            s.set(x, y0, Z0 + 2, "guhs:guh_tafel", {"facing": "north"})
        s.set(cx, y0, Z0 + 4, "guhs:guh_stoel", {"facing": "north"})
        s.set(cx - 1, y0 + 1, Z0 + 2, rng.choice(["guhs:potted_roze_guhbloem", "guhs:potted_knabbelroos", "guhs:potted_kaasbloem"]))
        s.set(cx + 1, y0 + 1, Z0 + 2, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    for x in list(range(12, 21)) + list(range(60, 69)):                  # rows of loaner wardrobes
        if x not in (16, 64):
            s.set(x, y0, 13, "guhs:beauty_leenkast", {"facing": "north"})
    for x in range(37, 44):                                               # a big wardrobe wall in the middle
        s.set(x, y0, Z0 + 1, "guhs:beauty_leenkast", {"facing": "south"})
        s.set(x, y0 + 1, Z0 + 1, "guhs:beauty_leenkast", {"facing": "south"})
    for (x, z) in ((5, 9), (5, 11), (75, 9), (75, 11)):
        s.set(x, y0, z, "guhs:pink_zitzak", {"facing": "east" if x < 40 else "west"})
    for (x, z) in ((28, 8), (30, 10), (50, 8), (52, 10)):
        s.set(x, y0, z, rng.choice(["guhs:pink_kussen", "guhs:white_kussen", "guhs:magenta_kussen"]), {"facing": "south"})
    s.set(40, y0, 9, "guhs:lampion_mint", {"hanging": "false", "waterlogged": "false"})


def foyer(h, s, rng):
    """The foyer: a white floor with a huge guh face mosaic, a ticket counter, vadszakken and bunting."""
    mc = h.mc
    fx, fz = 40, 86
    for x in range(X0 + 1, X1):
        for z in range(78, Z1):
            s.set(x, 0, z, mc("white_concrete") if (x + z) % 2 else mc("quartz_block"))
    face = set()
    for x in range(W):
        for z in range(76, Z1):
            d = ((x - fx) / 10.5) ** 2 + ((z - fz) / 7.5) ** 2
            e = min(math.hypot(x - (fx - 7), z - (fz - 7)), math.hypot(x - (fx + 7), z - (fz - 7)))
            if d <= 1 or e <= 3.2:
                face.add((x, z))
    for (x, z) in face:
        edge = any((x + dx, z + dz) not in face for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        block = mc("magenta_concrete") if edge else mc("pink_concrete")
        for (ex, ez) in ((fx - 4, fz - 1), (fx + 4, fz - 1)):
            if abs(x - ex) <= 1 and abs(z - ez) <= 1:
                block = mc("white_concrete") if (x, z) == (ex - 1, ez - 1) else mc("black_concrete")
        if (x, z) in ((fx - 7, fz + 2), (fx - 6, fz + 2), (fx + 6, fz + 2), (fx + 7, fz + 2)):
            block = mc("magenta_concrete")
        if (x, z) in ((fx - 2, fz + 3), (fx - 1, fz + 4), (fx, fz + 3), (fx + 1, fz + 4), (fx + 2, fz + 3)):
            block = mc("black_concrete")
        s.set(x, 0, z, block)
    # the ticket counter (with a cake) and the vadszakken
    for z in range(82, 90):
        s.set(9, 1, z, mc("cherry_planks"))
        s.set(9, 2, z, mc("pink_terracotta") if z % 2 else mc("white_terracotta"))
    s.set(9, 3, 84, "guhs:block_of_kaasknabbels")
    s.set(9, 3, 87, "guhs:lampion_roze", {"hanging": "false", "waterlogged": "false"})
    s.set(6, 1, 85, "guhs:beauty_leenkast", {"facing": "east"})
    for z in range(80, 94, 2):
        s.set(75, 1, z, rng.choice(["guhs:pink_zitzak", "guhs:magenta_zitzak", "guhs:white_zitzak"]), {"facing": "west"})
    for (x, z) in ((5, 79), (5, 95), (75, 95), (60, 95), (20, 95)):
        s.set(x, 1, z, rng.choice(["guhs:potted_roze_guhbloem", "guhs:potted_knabbelroos", "guhs:potted_guhoortjes"]))
    for z in (80, 88):                                                # bunting across the foyer
        for x in range(X0 + 1, X1):
            s.set(x, 15, z, "guhs:vlaggetjes", {"axis": "x"})
    for x0 in (22, 52):                                               # guh posters on the inside of the front wall
        h.ms.guh_portrait(s, x0, 6, Z1 - 1)
        for y in range(5, 14):
            s.set(x0 - 1, y, Z1 - 1, mc("gold_block"))
            s.set(x0 + 7, y, Z1 - 1, mc("gold_block"))
        for x in range(x0 - 1, x0 + 8):
            s.set(x, 5, Z1 - 1, mc("gold_block"))
            s.set(x, 13, Z1 - 1, mc("gold_block"))


def facade(h, s):
    """The front: a giant guh face on the wall (its ears stick out above the roof), and the door is its mouth."""
    mc = h.mc
    cx, cy, r = 40, 12, 13.6
    zf = Z1 + 1
    face = {(x, y) for x in range(W) for y in range(1, H) if math.hypot(x - cx, y - cy) <= r}
    ears = {(x, y) for x in range(W) for y in range(1, H) for (ex, ey) in ((28.5, 24.5), (51.5, 24.5)) if math.hypot(x - ex, y - ey) <= 4.6}
    shape = face | ears
    for (x, y) in shape:
        edge = any((x + dx, y + dy) not in shape and y + dy >= 1 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        in_ear = (x, y) in ears and (x, y) not in face
        inner_ear = in_ear and min(math.hypot(x - 28.5, y - 24.5), math.hypot(x - 51.5, y - 24.5)) < 2.6
        block = mc("gold_block") if edge else mc("magenta_wool") if inner_ear else mc("pink_wool")
        for (ex, ey) in ((34, 14), (46, 14)):
            if ((x - ex) / 1.7) ** 2 + ((y - ey) / 2.5) ** 2 <= 1:
                block = mc("white_concrete") if (x, y) == (ex - 1, ey + 1) else mc("black_concrete")
        for (bx, by) in ((29, 10), (51, 10)):
            if ((x - bx) / 2.3) ** 2 + ((y - by) / 1.3) ** 2 <= 1:
                block = mc("magenta_wool")
        if (x, y) in ((40, 11), (39, 11), (41, 11)):
            block = mc("magenta_wool")
        if (x, y) in ((37, 8), (38, 7), (39, 7), (40, 8), (41, 7), (42, 7), (43, 8)):
            block = mc("black_concrete")
        s.set(x, y, zf, block)
        if y >= TOP:
            s.set(x, y, Z1, block)                  # thicker above the roof, so the ears stand firm
    for y in range(1, 7):                            # the mouth is the door, framed in quartz
        for x in range(37, 44):
            s.set(x, y, Z1, mc("air"))
            s.set(x, y, zf, mc("air"))
        s.set(36, y, zf, mc("quartz_pillar"), {"axis": "y"})
        s.set(44, y, zf, mc("quartz_pillar"), {"axis": "y"})
    for x in range(36, 45):
        s.set(x, 7, zf, mc("quartz_block") if s.get(x, 7, zf) != mc("black_concrete") else mc("black_concrete"))
    for x in range(37, 44):
        s.set(x, 0, Z1, mc("magenta_concrete"))
        s.set(x, 0, zf, mc("magenta_concrete"))


# --- the geometry self-check -------------------------------------------------------------------------------------------------
def passable(s, x, y, z):
    b = s.get(x, y, z)
    return b is None or b.startswith(PASSABLE_PREFIX)


def solid(s, x, y, z):
    return s.inside(x, y, z) and not passable(s, x, y, z)


def standable(s, x, y, z):
    below = s.get(x, y - 1, z) or ""
    return (s.inside(x, y, z) and s.inside(x, y + 1, z) and passable(s, x, y, z) and passable(s, x, y + 1, z) and solid(s, x, y - 1, z)
            and not below.endswith(("_fence", "_wall")))        # (nobody stands on a fence)


def check(s, npcs):
    """No holes in the floors, everything reachable from the path outside, characters on solid ground, no floating blocks."""
    problems = []
    for x in range(W):
        for z in range(D):
            if not solid(s, x, 0, z):
                problems.append(f"hole in the plate at {x},{z}")
    # walk around from the path in front of the door
    start = (40, 1, D - 1)
    seen, todo = {start}, [start]
    deep_falls = set()
    while todo:
        x, y, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if not s.inside(nx, y, nz):
                continue
            options = []
            if standable(s, nx, y, nz):
                options.append(y)
            elif standable(s, nx, y + 1, nz) and passable(s, x, y + 2, z):
                options.append(y + 1)
            elif passable(s, nx, y, nz) and passable(s, nx, y + 1, nz):
                for ny in range(y - 1, 0, -1):          # falling down
                    if not passable(s, nx, ny, nz):
                        break
                    if standable(s, nx, ny, nz):
                        options.append(ny)
                        if y - ny > 3:
                            deep_falls.add((nx, ny, nz, y - ny))
                        break
            for ny in options:
                if (nx, ny, nz) not in seen:
                    seen.add((nx, ny, nz))
                    todo.append((nx, ny, nz))
    for f in sorted(deep_falls)[:5]:
        problems.append(f"a drop of {f[3]} blocks to {f[:3]} (fall damage)")

    def near(p, dist=2.5):
        return any(math.dist((p[0], p[1], p[2]), (q[0] + 0.5, q[1], q[2] + 0.5)) <= dist for q in seen)

    targets = {"the model spot": (MODEL[0] + 0.5, STAGE + 1, MODEL[1] + 0.5), "the end of the catwalk": (END[0] + 0.5, STAGE + 1, END[1] + 0.5),
               "the dressing room": (20.5, STAGE + 1, 9.5), "the dressing room (east)": (60.5, STAGE + 1, 9.5),
               "the jury desk": (40.5, 1, JURY_Z - 3.5), "the top benches": (8.5, 8, 53.5), "the foyer": (40.5, 1, 86.5)}
    for (x, y, z, what) in npcs:
        targets[what] = (x, y, z)
        fx, fy, fz = math.floor(x), math.floor(y), math.floor(z)
        seated = y != fy                        # sitting on a cushion or a bench (inside its block)
        if not solid(s, fx, fy if seated else fy - 1, fz) or not passable(s, fx, fy + 1 if seated else fy, fz):
            problems.append(f"{what} does not stand on solid ground at {x},{y},{z}")
    for what, p in targets.items():
        if not near(p, 3.0):
            problems.append(f"can't walk to {what} {p}")
    for (x, y, z), (b, _, _) in s.blocks.items():
        if b == "guhs:beauty_leenkast" and not any((x + dx, y - up, z + dz) in seen for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))
                                                     for up in (0, 1)):
            problems.append(f"loaner wardrobe at {x},{y},{z} can't be reached")
    # nothing floats: every block hangs on to the ground through other blocks
    blocks = {p for p, (b, _, _) in s.blocks.items() if b not in ("minecraft:air", "minecraft:light", "guhs:beauty_plek")}
    attached = {p for p in blocks if p[1] == 0}
    todo = list(attached)
    while todo:
        x, y, z = todo.pop()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + d[0], y + d[1], z + d[2])
            if n in blocks and n not in attached:
                attached.add(n)
                todo.append(n)
    floating = blocks - attached
    for p in sorted(floating)[:5]:
        problems.append(f"floating block {s.get(*p)} at {p}")
    return problems, len(seen)


# --- everything ---------------------------------------------------------------------------------------------------------------
def build(h):
    lang = h.lang
    # the Showguh: a lilac glamour guh
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    h.save(h.recolour(src, hue=0.80, sat=1.15, val=1.05, only=h.pinkish), "entity", "npc_showguh.png")
    # the showrozet
    h.save(rozet_icon(), "item", "showrozet.png")
    h.item_model("showrozet")
    # the loaner wardrobe
    front, side, top = leenkast_textures(h)
    h.save(front, "block", "beauty_leenkast_front.png")
    h.save(side, "block", "beauty_leenkast_side.png")
    h.save(top, "block", "beauty_leenkast_top.png")
    h.w(f"{h.A}/models/block/beauty_leenkast.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": "guhs:block/beauty_leenkast_front", "side": "guhs:block/beauty_leenkast_side", "top": "guhs:block/beauty_leenkast_top",
        "particle": "guhs:block/beauty_leenkast_side"}})
    h.w(f"{h.A}/blockstates/beauty_leenkast.json", {"variants": {f"facing={f}": ({"model": "guhs:block/beauty_leenkast", "y": r} if r
                                                                                  else {"model": "guhs:block/beauty_leenkast"})
                                                                 for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    h.w(f"{h.A}/models/item/beauty_leenkast.json", {"parent": "guhs:block/beauty_leenkast"})
    h.self_drop("beauty_leenkast")
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:beauty_leenkast"])
    h.shaped("beauty_leenkast", ["PGP", "PKP", "PPP"], {"P": "guhs:guhbloesem_planks", "G": "minecraft:glass_pane", "K": "guhs:guh_kast"},
             "guhs:beauty_leenkast")
    # the invisible stage markers
    h.w(f"{h.A}/models/block/beauty_plek.json", {"textures": {"particle": "minecraft:block/pink_stained_glass"}})
    h.w(f"{h.A}/blockstates/beauty_plek.json", {"variants": {"spot=model": {"model": "guhs:block/beauty_plek"},
                                                             "spot=einde": {"model": "guhs:block/beauty_plek"}}})

    # --- the theatre ---
    h.TEMPLATE_SIZES[STRUCT] = D
    h.FLATNESS[STRUCT] = 30
    none = {"bounding_box": "full", "spawns": []}
    h.structure(STRUCT, h.GUHMENSION_LAND, spacing=44, separation=16, salt=20240101,
                spawn_overrides={"creature": none, "monster": none, "ambient": none})
    s = theater(h)
    npcs = [(SHOWGUH[0], STAGE + 1.0, SHOWGUH[1], "the Showguh")] + [(36.5 + 4 * i, 2.25, JURY_Z + 0.5, f"jury guh {i + 1}") for i in range(3)]
    npcs += [(x + 0.5, t + 1.5, z + 0.5, f"audience guh {n + 1}") for n, (x, t, z) in enumerate(AUDIENCE)]
    problems, reached = check(s, npcs)
    if problems:
        raise SystemExit("guh beauty theatre geometry check failed:\n  " + "\n  ".join(problems))
    print(f"guh beauty theatre: geometry ok ({reached} spots to stand on)")
    s.save(STRUCT)

    # --- advancements ---
    for name, parent, icon, frame, crit, title, desc in [
        ("find_guh_beauty_theater", "enter_guhmension", "guhs:showrozet", "goal",
         {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": f"guhs:{STRUCT}"}}}}},
         ("Showtime!", "Showtijd!"), ("Find the Guh Beauty Theater", "Vind het Guh Beauty Theater")),
        ("beauty_showster", "find_guh_beauty_theater", "guhs:showster_tiara", "challenge",
         # one criterion per piece (all three needed): wearing one on your guh while buying the next still counts
         {c: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"guhs:{c}"}]}} for c in CLOTHES},
         ("Miss Vadsig", "Miss Vadsig"), ("Buy the whole Showster outfit from the Showguh", "Koop het hele Showster-pakje bij de Showguh")),
    ]:
        h.w(f"{h.D}/advancement/guhmension/{name}.json", {
            "parent": f"guhs:guhmension/{parent}",
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guhmension.{name}.title"},
                        "description": {"translate": f"advancements.guhs.guhmension.{name}.description"},
                        "frame": frame, "show_toast": True, "announce_to_chat": True},
            "criteria": crit})
        lang(f"advancements.guhs.guhmension.{name}.title", *title)
        lang(f"advancements.guhs.guhmension.{name}.description", *desc)
    for name in ("beauty_first", "beauty_good", "beauty_extase", "beauty_own_guh"):
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})

    write_lang(lang)


THEMES = {  # id: (en, nl, hint en, hint nl)
    "winter": ("Winter fun", "Winterpret", "Dress warm: hats, scarves and sweaters!", "Warm aankleden: mutsen, sjaals en truien!"),
    "feest": ("Party time!", "Feestje!", "Party hats, bows and lots of colour!", "Feesthoedjes, strikjes en heel veel kleur!"),
    "werk": ("At work", "Aan het werk", "Which job? A helmet, a cap, a coat...", "Welk beroep? Een helm, een pet, een jas..."),
    "sprookjes": ("Fairy tales", "Sprookjes", "Knights, wizards and kings...", "Ridders, tovenaars en koningen..."),
    "griezelig": ("Spooky", "Griezelig", "Boo! Witches, ghosts and pumpkins.", "Boe! Heksen, spoken en pompoenen."),
    "zomer": ("Summer sun", "Zomerzon", "Sun, a straw hat and sunglasses!", "Zon, een strohoed en een zonnebril!"),
    "gala": ("Chic gala", "Chic gala", "So chic! A bow tie, a monocle, a cape...", "Zo chic! Een strikje, een monocle, een mantel..."),
    "koningsdag": ("King's Day", "Koningsdag", "Orange on top! And a crown.", "Oranje boven! En een kroontje."),
    "hollands_weer": ("Dutch weather", "Hollands weer", "Rain, wind and... more rain.", "Regen, wind en... nog meer regen."),
}

JURY_LINES = {  # judge: {band: (en, nl)}   (%s = the theme)
    "vadsma": {
        "hello": ("I'm Miss Vadsma. I judge the theme: every piece has to fit. Strict, but vadsig!",
                  "Ik ben Juf Vadsma. Ik let op het thema: elk kledingstuk moet passen. Streng maar vadsig!"),
        "naked": ("No clothes? Njeg. One point, for the walking.", "Geen kleertjes? Njeg. Eén punt, voor het lopen."),
        "low": ("That has very little to do with %s, dear.", "Dat heeft weinig met %s te maken, lieverd."),
        "mid": ("A start, but %s? Meh.", "Het begin is er, maar %s? Mwah."),
        "high": ("Neatly on theme: %s. I'm impressed!", "Keurig binnen het thema %s. Ik ben onder de indruk!"),
        "top": ("PERFECT %s! My monocle just fell off!", "PERFECT %s! Mijn monocle valt eruit!"),
    },
    "glitterguh": {
        "hello": ("Mister Glitterguh, charmed. I love complete outfits and pieces that belong together.",
                  "Meneer Glitterguh, aangenaam. Ik hou van complete outfits en kleertjes die bij elkaar horen."),
        "naked": ("A naked guh on MY catwalk? I'm breaking out in glitter!", "Een naakte guh op MIJN catwalk? Ik krijg er glitteruitslag van!"),
        "low": ("So much is missing! Hat, glasses, bow, suit: finish it!", "Er mist van alles! Hoedje, brilletje, strikje, pakje: maak het af!"),
        "mid": ("Cute, but it could be more complete. Head, eyes, neck AND body!", "Leuk, maar het kan completer. Hoofd, ogen, nek én lijf!"),
        "high": ("What a complete picture, darling! Almost glitterproof.", "Wat een compleet plaatje, schat! Bijna glitterproof."),
        "top": ("COMPLETE! MATCHING! That is a LOOK! I'm done!", "COMPLEET! BIJ ELKAAR! Dit is een LOOK! Ik ben op!"),
    },
    "knabbel": {
        "hello": ("I'm Granny Knabbel. I score with my heart. Your own guh or a Showster piece? Extra cute!",
                  "Ik ben Oma Knabbel. Ik geef punten met mijn hart. Een eigen guh of een Showster-kledingstuk? Extra schattig!"),
        "naked": ("Oh dear, the poor thing must be cold. A knabbel for courage anyway.", "Ach gossie, hij heeft het vast koud. Toch een knabbeltje voor de moed."),
        "low": ("I still think it's sweet. Here, have a cheese snack.", "Ik vind hem toch lief. Hier, een kaasknabbeltje."),
        "mid": ("What a lovely vadsig thing. Granny is happy!", "Wat een lekker vadsig ding. Oma is tevreden!"),
        "high": ("Oh, my old guh heart! What a sweetie!", "Oh, mijn oude guhhartje! Wat een schatje!"),
        "top": ("VAHOEG! Granny is crying with joy! Ten knabbels!", "VAHOEG! Oma huilt van geluk! Tien knabbels!"),
    },
}

LANG = {  # key: (en, nl)
    "entity.guhs.guh_npc.showguh": ("Showguh", "Showguh"),
    "entity.guhs.beauty_model": ("Model %s", "Model %s"),
    "entity.guhs.beauty_jury.vadsma": ("Miss Vadsma", "Juf Vadsma"),
    "entity.guhs.beauty_jury.glitterguh": ("Mister Glitterguh", "Meneer Glitterguh"),
    "entity.guhs.beauty_jury.knabbel": ("Granny Knabbel", "Oma Knabbel"),
    "item.guhs.showrozet": ("Show Rosette", "Showrozet"),
    "item.guhs.showrozet.lore": ("Won on the catwalk of the Guh Beauty Theater. Spend it at the Showguh.",
                                 "Gewonnen op de catwalk van het Guh Beauty Theater. Te besteden bij de Showguh."),
    "item.guhs.showster_tiara": ("Showster Tiara", "Showster-tiara"),
    "item.guhs.showster_sjerp": ("Miss Vadsig Sash", "Miss Vadsig-sjerp"),
    "item.guhs.showster_strik": ("Glitter Bow", "Glitterstrik"),
    "block.guhs.beauty_leenkast": ("Loaner Wardrobe", "Leenkledingkast"),
    "block.guhs.beauty_plek": ("Stage Spot", "Podiumplek"),
    f"structure.guhs.{STRUCT}": ("Guh Beauty Theater", "Guh Beauty Theater"),
    f"structure.guhs.{STRUCT}.tooltip": ("Minigame: the Showguh's beauty contest, show rosettes and the Showster outfit",
                                         "Minigame: de Vads-wedstrijd van de Showguh, showrozetten en het Showster-pakje"),
    # talking to the Showguh
    "quest.guhs.beauty.hello_first": ("VAHOEG, a new face! Welcome to the Guh Beauty Theater. Here we dress guhs for the Vads contest: I give a theme, you pick the clothes from the loaner wardrobe, and the model walks the catwalk. The jury gives points, you get show rosettes. Ready?",
                                      "VAHOEG, een nieuw gezicht! Welkom in het Guh Beauty Theater. Hier kleden we guhs aan voor de Vads-wedstrijd: ik geef een thema, jij kiest de kleertjes uit de leenkledingkast en het model loopt over de catwalk. De jury geeft punten, jij krijgt showrozetten. Zin in?"),
    "quest.guhs.beauty.hello": ("There you are again, top stylist! Another round of vadsig glamour?",
                                "Daar ben je weer, topstylist! Nog een rondje vadsige glamour?"),
    "quest.guhs.beauty.running": ("Shhh, there's a show on! Grab a seat on the benches and clap along. You're next!",
                                  "Sssst, er loopt al een show! Pak een plekje op de tribune en klap mee. Daarna ben jij!"),
    "quest.guhs.beauty.busy_self": ("You're in the show already! Dress your model (click the model or a loaner wardrobe).",
                                    "Jij staat al in de show! Kleed je model aan (klik op het model of een leenkledingkast)."),
    "quest.guhs.beauty.broken": ("Njeg... I can't find my stage any more. No stage, no show!",
                                 "Njeg... ik kan mijn podium niet meer vinden. Zonder podium geen show!"),
    "quest.guhs.beauty.no_own_guh": ("I don't see your own guh anywhere! Bring your tamed guh (close by), or take one of my models.",
                                     "Ik zie je eigen guh nergens! Neem je tamme guh mee (dichtbij), of kies een model van mij."),
    "quest.guhs.beauty.start": ("Showtime! %s is waiting on the stage. Three rounds, three themes. Make it vadsig!",
                                "Showtijd! %s staat klaar op het podium. Drie rondes, drie thema's. Maak er iets vadsigs van!"),
    # the show
    "quest.guhs.beauty.round": ("Round %s of %s! The theme is... %s!", "Ronde %s van %s! Het thema is... %s!"),
    "quest.guhs.beauty.theme": ("Theme: %s", "Thema: %s"),
    "quest.guhs.beauty.dress_title": ("Dress up!", "Aankleden!"),
    "quest.guhs.beauty.dress_help": ("Click a piece to put it on the model. Closed the wardrobe? Click the model or a loaner wardrobe. Done? Click 'Onto the catwalk!'",
                                     "Klik op een kledingstuk om het model aan te kleden. Kast dicht? Klik op het model of een leenkledingkast. Klaar? Klik op 'De catwalk op!'"),
    "quest.guhs.beauty.bar": ("Round %s/%s - %s - %s left - wearing %s piece(s)", "Ronde %s/%s - %s - nog %s - %s kledingstuk(ken) aan"),
    "quest.guhs.beauty.walk_title": ("Catwalk!", "Catwalk!"),
    "quest.guhs.beauty.walk": ("A big hand for %s! The theme: %s. Look at those hips wiggle!",
                               "Applaus voor %s! Het thema: %s. Kijk die heupjes wiebelen!"),
    "quest.guhs.beauty.walk_naked": ("Uh... %s walks onto the catwalk without any clothes! Wasn't the theme %s?!",
                                     "Eh... %s loopt de catwalk op zonder kleertjes! Het thema was toch %s?!"),
    "quest.guhs.beauty.pose": ("%s strikes a pose for the jury... VAHOEG!", "%s poseert voor de jury... VAHOEG!"),
    "quest.guhs.beauty.jury_bar": ("%s: %s points", "%s: %s punten"),
    "quest.guhs.beauty.round_total": ("%s gets %s of the %s points: %s show rosette(s)!", "%s krijgt %s van de %s punten: %s showrozet(ten)!"),
    "quest.guhs.beauty.rosettes": ("+%s show rosette(s)", "+%s showrozet(ten)"),
    "quest.guhs.beauty.extase": ("The jury is ecstatic! They're standing on their cushions clapping!",
                                 "De jury is in extase! Ze staan op hun kussens te klappen!"),
    "quest.guhs.beauty.finale": ("End of the show! Total: %s of the %s points. You won %s show rosettes (%s of them bonus).",
                                 "Einde van de show! Totaal: %s van de %s punten. Je won %s showrozetten (waarvan %s bonus)."),
    "quest.guhs.beauty.first": ("Your very first show! The Showguh has a present for you: 4 extra show rosettes, balloons and fried cheese snacks.",
                                "Je allereerste show! De Showguh heeft een cadeautje: 4 extra showrozetten, ballonnen en gefrituurde kaasknabbels."),
    "quest.guhs.beauty.record": ("NEW RECORD: %s points!", "NIEUW RECORD: %s punten!"),
    "quest.guhs.beauty.best": ("Your record stays at %s points. Even more vadsig next time!", "Jouw record blijft %s punten. Volgende keer nog vadsiger!"),
    "quest.guhs.beauty.record_title": ("NEW RECORD!", "NIEUW RECORD!"),
    "quest.guhs.beauty.finale_title": ("What a show!", "Wat een show!"),
    "quest.guhs.beauty.finale_sub": ("%s / %s points", "%s / %s punten"),
    "quest.guhs.beauty.applause": ("Thunderous applause for %s and %s: %s points!", "Daverend applaus voor %s en %s: %s punten!"),
    "quest.guhs.beauty.stopped": ("The show stopped. You keep your %s show rosette(s). See you next time!",
                                  "De show is gestopt. Je houdt je %s showrozet(ten). Tot de volgende keer!"),
    "quest.guhs.beauty.publiek": ("Shhh, I'm watching the show! VAHOEG!", "Sssst, ik kijk naar de show! VAHOEG!"),
    # screens and messages
    "gui.guhs.beauty.title": ("Guh Beauty Vads Contest", "Guh Beauty Vads-wedstrijd"),
    "gui.guhs.beauty.explain": ("%s rounds, each with a theme. Dress the model from the loaner wardrobe (head, eyes, neck and body), then it walks the catwalk. Each jury guh gives 1-10 points: Miss Vadsma judges the theme, Mister Glitterguh a complete look, Granny Knabbel her heart. Points become show rosettes. You don't need anything yourself!",
                                "%s rondes, elk met een thema. Kleed het model aan uit de leenkledingkast (hoofd, ogen, nek en lijf), dan loopt het over de catwalk. Elk jurylid geeft 1-10 punten: Juf Vadsma let op het thema, Meneer Glitterguh op een complete look, Oma Knabbel op haar hart. Punten worden showrozetten. Je hebt zelf niks nodig!"),
    "gui.guhs.beauty.running": ("%s is on stage right now (round %s of %s, theme %s). Sit down on the benches and watch!",
                                "%s staat nu op het podium (ronde %s van %s, thema %s). Ga gezellig op de tribune zitten en kijk mee!"),
    "gui.guhs.beauty.best.none": ("You've never been in a show. Time for your debut!", "Je hebt nog nooit meegedaan. Tijd voor je debuut!"),
    "gui.guhs.beauty.best": ("Your record: %s of %s points (%s shows)", "Jouw record: %s van de %s punten (%s shows)"),
    "gui.guhs.beauty.start": ("Start the show!", "Start de show!"),
    "gui.guhs.beauty.start.tooltip": ("The Showguh gives you a model. You don't need anything yourself!",
                                      "Je krijgt een model van de Showguh. Je hebt zelf niks nodig!"),
    "gui.guhs.beauty.start_own": ("With %s", "Met %s"),
    "gui.guhs.beauty.start_own.tooltip": ("Your own tamed guh walks the catwalk! It gets its own clothes back after the show, and what it wears now it may wear in the show too.",
                                          "Je eigen tamme guh loopt de catwalk! Na de show krijgt hij zijn eigen kleertjes terug, en wat hij nu draagt mag hij in de show ook aan."),
    "gui.guhs.beauty.start_own.none": ("With your own guh", "Met je eigen guh"),
    "gui.guhs.beauty.start_own.none.tooltip": ("Bring your tamed guh (within 24 blocks) to let it walk the catwalk itself.",
                                               "Neem je tamme guh mee (binnen 24 blokken) om hem zelf te laten showen."),
    "gui.guhs.beauty.shop": ("Showster shop", "Showster-winkel"),
    "gui.guhs.beauty.shop.tooltip": ("The glitter bow, the Miss Vadsig sash and the Showster tiara, for show rosettes",
                                     "De glitterstrik, de Miss Vadsig-sjerp en de Showster-tiara, voor showrozetten"),
    "gui.guhs.beauty.wardrobe": ("Loaner wardrobe", "Leenkledingkast"),
    "gui.guhs.beauty.ready": ("Onto the catwalk!", "De catwalk op!"),
    "gui.guhs.beauty.ready.tooltip": ("Done dressing? Then the model walks the catwalk now.", "Klaar met aankleden? Dan loopt het model nu over de catwalk."),
    "gui.guhs.beauty.undress_all": ("All off", "Alles uit"),
    "gui.guhs.beauty.quit": ("Stop", "Stoppen"),
    "gui.guhs.beauty.quit.tooltip": ("Stop the show (you keep the show rosettes you won)", "Stop de show (je houdt je gewonnen showrozetten)"),
    "gui.guhs.beauty.round": ("Round %s of %s", "Ronde %s van %s"),
    "gui.guhs.beauty.own_piece": ("Your own guh's", "Van je eigen guh"),
    "gui.guhs.beauty.click_on": ("Click: put on", "Klik: aantrekken"),
    "gui.guhs.beauty.click_off": ("Click: take off", "Klik: uittrekken"),
    "gui.guhs.beauty.leenkast.closed": ("The loaner wardrobe only opens during your own show. Ask the Showguh!",
                                        "De leenkledingkast gaat alleen open tijdens je eigen show. Vraag het de Showguh!"),
    "gui.guhs.beauty.no_build": ("Njeg! The theatre is sacred: no breaking or building here.", "Njeg! Het theater is heilig: hier mag je niks slopen of bouwen."),
    # the world's top 3 (Scorebord) over the Showguh's head
    "gui.guhs.scorebord.beauty": ("Top 3 Vads stylists", "Top 3 vadsigste stylisten"),
    "gui.guhs.scorebord.beauty.show": ("Best show (of %s points)", "Beste show (van de %s punten)"),
    # the jury's reasons: about THIS outfit
    "quest.guhs.beauty.reason.vadsma.perfect": ("Look at that: %s for %s! A perfect fit, VAHOEG!", "Kijk nou: %s bij %s! Past perfect, VAHOEG!"),
    "quest.guhs.beauty.reason.vadsma.good": ("%s for %s? Quite neat. Not perfect, but vads enough.", "%s bij %s? Best netjes. Niet perfect, wel vads genoeg."),
    "quest.guhs.beauty.reason.vadsma.off": ("%s at %s?! Njeg! That fits like a cheese snack in a birdcage.",
                                            "%s bij %s?! Njeg! Dat past als een kaasknabbel in een vogelkooi."),
    "quest.guhs.beauty.reason.glitterguh.missing.head": ("No hat?! A bald guh head is not a look, darling!", "Geen hoedje?! Een kaal guhhoofd is geen look, schat!"),
    "quest.guhs.beauty.reason.glitterguh.missing.eyes": ("Nothing on the eyes? Glasses, darling, GLASSES! The eyes are the windows of the vads!",
                                                          "Niks op de oogjes? Een brilletje, schat, een BRILLETJE! De ogen zijn de ramen van de vads!"),
    "quest.guhs.beauty.reason.glitterguh.missing.neck": ("A bare neck! Where is the bow? Where is the drama?", "Een blote nek! Waar is het strikje? Waar is het drama?"),
    "quest.guhs.beauty.reason.glitterguh.missing.body": ("No outfit on the body? That round guh belly is begging for a suit!",
                                                          "Geen pakje aan? Dat ronde guhbuikje smeekt om een outfit!"),
    "quest.guhs.beauty.reason.glitterguh.set": ("%s AND %s: they belong together! Glitter-VAHOEG!", "%s ÉN %s: die horen bij elkaar! Glitter-VAHOEG!"),
    "quest.guhs.beauty.reason.knabbel.own": ("%s is a real sweetie. Your own guh always gets an extra knabbel from Granny!",
                                             "%s is echt een lieverdje. Een eigen guh krijgt altijd een extra knabbeltje van Oma!"),
    "quest.guhs.beauty.reason.knabbel.showster": ("Ooh, a Showster piece: %s! Granny's heart is melting like cheese in the fryer!",
                                                  "Oeh, een Showster-stuk: %s! Oma's hart smelt als kaas in de frituur!"),
    "quest.guhs.beauty.reason.knabbel.piece": ("And look: %s! Grandpa Guh wore one just like it, back in the day. VAHOEG!",
                                               "En kijk: %s! Opa Guh had er vroeger precies zo één. VAHOEG!"),
    "gui.guhs.beauty.model_hands_off": ("Hands off! Only the show's stylist may dress the model.",
                                        "Niet aankomen! Alleen de stylist van de show mag het model aankleden."),
}


def write_lang(lang):
    for key, (en, nl) in LANG.items():
        lang(key, en, nl)
    for tid, (en, nl, hint_en, hint_nl) in THEMES.items():
        lang(f"gui.guhs.beauty.theme.{tid}", en, nl)
        lang(f"gui.guhs.beauty.hint.{tid}", hint_en, hint_nl)
    for judge, lines in JURY_LINES.items():
        for band, (en, nl) in lines.items():
            lang(f"quest.guhs.beauty.jury.{judge}.{band}", en, nl)


def ftb(fq):
    y = 26
    fq.q("beauty_theater", "Showtijd!", "Zoek het &dGuh Beauty Theater&r (superkompas: Minigames). Je herkent het aan de gevel: een reuzenguh, en de deur is zijn mond!",
         "guhs:guhmensie_superkompas", [fq.structure(STRUCT)], x=-8, y=y)
    fq.q("beauty_first", "Op de catwalk", "Praat met de &dShowguh&r op het podium en doe mee aan de Vads-wedstrijd. Je hoeft zelf niks mee te nemen: model en kleertjes krijg je te leen (of neem je eigen tamme guh mee)! De jury houdt bordjes met punten omhoog, en boven de Showguh zweeft de &6top 3&r van de wereld.",
         "guhs:showrozet", [fq.adv("beauty_first")], rewards=(("guhs:kaas_knabbels", 16),), x=-6, y=y, xp=100)
    fq.q("beauty_good", "Vahoege stylist", f"Haal in één show minstens &665 van de 90&r punten. Tip: Juf Vadsma wil het thema, Meneer Glitterguh een complete look (hoofd, ogen, nek én lijf).",
         "guhs:showrozet", [fq.adv("beauty_good")], rewards=(("guhs:gefrituurde_kaasknabbels", 8),), x=-4, y=y, xp=200)
    fq.q("beauty_extase", "De jury in extase", "Haal in één ronde &628 punten of meer&r. De jury staat op haar kussens te klappen!",
         "guhs:showster_strik", [fq.adv("beauty_extase")], rewards=(("guhs:guh_kristal", 8),), x=-2, y=y, xp=300, shape="gear")
    fq.q("beauty_own", "Mijn guh is een ster", "Laat je &deigen tamme guh&r de catwalk op lopen (kies 'Met ...' bij de Showguh). Na de show krijgt hij zijn eigen kleertjes terug.",
         "guhs:guh_spawn_egg", [fq.adv("beauty_own_guh")], rewards=(("guhs:kaas_knabbels", 24),), x=0, y=y, xp=150)
    fq.q("beauty_rozetten", "Rozettenverzamelaar", "Spaar 16 showrozetten: genoeg voor de Showster-tiara!",
         "guhs:showrozet", [fq.item("guhs:showrozet", 16)], rewards=(("guhs:guh_ballon", 3),), x=2, y=y)
    fq.q("beauty_strik", "Glitter!", "Koop de &dglitterstrik&r bij de Showguh (showrozetten).",
         "guhs:showster_strik", [fq.item("guhs:showster_strik")], x=4, y=y)
    fq.q("beauty_showster", "Miss Vadsig", "Koop het hele Showster-pakje: de glitterstrik, de Miss Vadsig-sjerp en de Showster-tiara.",
         "guhs:showster_tiara", [fq.adv("guhs:guhmension/beauty_showster")], rewards=(("minecraft:diamond", 3),), x=6, y=y, xp=400, shape="gear")
