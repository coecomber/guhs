"""
Guhwai'i (3.0, slice guhwaii): the two buildings (templates) and the test rooms.

  ohana(h)    guhwaii_ohana (51 x 34 x 51, ground layer G=4, anchor guhs:guhwaii_ohana_midden in the middle): Lilo-guh and
              Nani-guh's stilt house on the beach. The template is placed unrotated and the sea may lie on any side, so the
              house stands in the middle, open to all four sides: a round lagoon pond under it, palm-log stilts, a wide
              veranda all around with bamboo railings, stairs on all four sides, a thatched hip roof with two pink guh ears
              on top. Inside: Lilo's room (her bed, 626's little basket, a record player, the 626 poster, hibiscus pots) and
              Nani's kitchen (counters, a sink, the coconut bowl). Outside: the guh-asiel "Pootje Thuis" with its kennels
              (626-guh's story copy waits in front of it), guh palms, a fire pit with seats, a sandcastle, stepping stones,
              flowers, tiki torches and the Reisguh "Ohana op Guhwai'i".
  capsule(h)  guhwaii_capsule (35 x 20 x 35, G=4, anchor guhs:guhwaii_capsule_midden): 626-guh's crashed capsule on the
              island's hill: a skid trail, a crater, the round pod (white with a pink band, a cracked glass dome, antennas),
              its hatch open with a ramp; inside, free-standing, the vadsigheid-scanner, the pilot seat, the control panel,
              the logbook of Experiment 626 on a lectern and the poster; still smoking a bit; the Reisguh "De capsule".
  tests(h)    guhwaii_test_strand (sand, a bit of water), guhwaii_test_muur (a wall with a ceiling), guhwaii_test_scanner
Each building is checked (nothing floats, the NPCs and important spots can be walked to from the edge, the jigsaw, one
Reisguh). Protected in the game (GuhwaiiEvents): no breaking or building there, except the rommeltjes.
"""
import json
import math
import random

from features import reisguh_plek
from features import sterrenwacht_hulp as hulp
from make_structures import Byte, NbtList, compounds

AIR = "minecraft:air"
G = 4
STAM = "guhs:guhwaii_palm_stam"
PLANK = "guhs:guhwaii_palm_planken"
BLAD = "guhs:guhwaii_palm_blad"
HOOI = "minecraft:hay_block"
BAMBOE_HEK = "minecraft:bamboo_fence"
ZAND = "minecraft:sand"
BLOEMEN = ["guhs:roze_hibiscus", "guhs:guhwaii_plumeria", "guhs:guhwaii_paradijsbloem", "guhs:guhwaii_orchidee"]
OHANA_W, OHANA_H = 51, 34
CAPSULE_W, CAPSULE_H = 35, 22


def jigsaw(s, x, y, z, name, final):
    s.set(x, y, z, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": name, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": final,
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})


def trap(facing, half="bottom", shape="straight"):
    return {"facing": facing, "half": half, "shape": shape, "waterlogged": "false"}


def sign(s, x, y, z, rotation, regels, hout="bamboo"):
    """A standing sign (four lines); 1.2.0: each line a translate key sign.guhs.guhwaii.<slug>, the Dutch as fallback."""
    import sign_text
    msgs = sign_text.messages("sign.guhs.guhwaii", regels)
    empty = NbtList(8, [json.dumps({"text": ""})] * 4)
    s.set(x, y, z, f"minecraft:{hout}_sign", {"rotation": str(rotation), "waterlogged": "false"},
          {"id": "minecraft:sign", "front_text": {"messages": msgs, "color": "black", "has_glowing_text": Byte(0)},
           "back_text": {"messages": empty, "color": "black", "has_glowing_text": Byte(0)}, "is_waxed": Byte(1)})


def palm(s, x, gy, z, hoog, scheef, kijk, rng, noten=2):
    """A guh-palm in a template (like the worldgen one): trunk from gy+1, a leaning top, the face, fronds, coconuts."""
    dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[scheef]
    stam = []
    vorig = 0
    for i in range(1, hoog + 1):
        opzij = int(i * i / (hoog * hoog) * 1.5 + 0.001)
        if opzij != vorig:                                   # (an elbow: the trunk stays face-connected)
            stam.append((x + dx * vorig, gy + i, z + dz * vorig))
        stam.append((x + dx * opzij, gy + i, z + dz * opzij))
        vorig = opzij
    for i, p in enumerate(stam):
        if i == 2:
            s.set(*p, "guhs:guhwaii_palm_gezicht", {"facing": kijk, "knipoog": "false"})
        else:
            s.set(*p, STAM, {"axis": "y"})
    tx, ty, tz = stam[-1]
    blad = {"persistent": "true", "distance": "1", "waterlogged": "false"}
    s.set(tx, ty + 1, tz, BLAD, blad)
    s.set(tx, ty + 2, tz, BLAD, blad)
    for (rx, rz) in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
        lang = 3 if rx and rz else 4
        punten = [(tx, ty + 1, tz)] + [(tx + rx * k, ty + (1 if k < lang else 0), tz + rz * k) for k in range(1, lang + 1)]
        punten.append((tx + rx * lang, ty - 1, tz + rz * lang))     # the drooping tip
        for p in verbind(punten):
            s.set(*p, BLAD, blad)
    for i, (rx, rz) in enumerate(((1, 0), (-1, 0), (0, 1), (0, -1))[:noten]):
        s.set(tx + rx, ty, tz + rz, "guhs:kokosnoot", {"rijp": str(rng.choice((1, 2))), "hangend": "true"})


def verbind(punten):
    """The points with face-adjacent steps filled in between (x first, then z, then y): leaves must touch by a face."""
    uit = [punten[0]]
    for (x, y, z) in punten[1:]:
        cx, cy, cz = uit[-1]
        while (cx, cy, cz) != (x, y, z):
            if cx != x:
                cx += 1 if x > cx else -1
            elif cz != z:
                cz += 1 if z > cz else -1
            else:
                cy += 1 if y > cy else -1
            uit.append((cx, cy, cz))
    return uit


def bloemen(s, x0, z0, x1, z1, y, rng, kans=0.25):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            onder = s.get(x, y - 1, z)
            if s.get(x, y, z) in (None, AIR) and onder in (ZAND, "minecraft:grass_block") and rng.random() < kans:
                s.set(x, y, z, rng.choice(BLOEMEN))


# =====================================================================================================================
# the stilt house
# =====================================================================================================================
def ohana(h):
    rng = random.Random(30201)
    W, H = OHANA_W, OHANA_H
    c = W // 2
    s = h.Structure((W, H, W))
    F = G + 5                     # the house floor (you walk at F + 1)
    x0, x1 = 15, 35               # the platform (with the veranda)
    w0, w1 = 17, 33               # the walls
    # --- the ground: sand, grass patches, the lagoon pond under the house --------------------------------------------------
    for x in range(W):
        for z in range(W):
            d = math.hypot(x - c, z - c)
            for y in range(G):
                s.set(x, y, z, "minecraft:sandstone" if y < G - 2 else ZAND)
            vijver = d <= 11.5 + math.sin(math.atan2(z - c, x - c) * 3) * 0.8
            if vijver:
                s.set(x, G - 2, z, ZAND)
                s.set(x, G - 1, z, "minecraft:water" if d <= 10.5 else ZAND)
                s.set(x, G, z, "minecraft:water" if d <= 9.8 else ZAND)
            else:
                gras = (x < 12 or x > 38 or z < 12 or z > 38) and (math.hypot(x - 8, z - 42) < 7 or math.hypot(x - 44, z - 40) < 6
                                                                     or math.hypot(x - 6, z - 8) < 6)
                s.set(x, G, z, "minecraft:grass_block" if gras else ZAND, {"snowy": "false"} if gras else None)
    # --- stilts and bracing --------------------------------------------------------------------------------------------------
    palen = [(x, z) for x in (x0, 21, 29, x1) for z in (x0, 21, 29, x1) if x in (x0, x1) or z in (x0, x1)]
    palen += [(21, 21), (29, 21), (21, 29), (29, 29)]
    for (x, z) in palen:
        bodem = G - 2 if math.hypot(x - c, z - c) <= 11.5 else G + 1
        for y in range(bodem, F):
            s.set(x, y, z, STAM, {"axis": "y"})
    # --- the floor, the railing, the stairs ------------------------------------------------------------------------------------
    for x in range(x0, x1 + 1):
        for z in range(x0, x1 + 1):
            s.set(x, F, z, PLANK)
    midden = range(c - 1, c + 2)
    for k in range(x0, x1 + 1):
        for (x, z) in ((k, x0), (k, x1), (x0, k), (x1, k)):
            if k in midden:
                continue
            s.set(x, F + 1, z, BAMBOE_HEK, {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
    trede = {"west": (-1, 0), "east": (1, 0), "north": (0, -1), "south": (0, 1)}
    for kant, (dx, dz) in trede.items():
        opp = {"west": "east", "east": "west", "north": "south", "south": "north"}[kant]
        for k in range(1, F - G + 1):
            y = F - k + 1
            for m in midden:
                if dx:
                    x = (x0 if dx < 0 else x1) + dx * k
                    z = m
                else:
                    z = (x0 if dz < 0 else x1) + dz * k
                    x = m
                s.set(x, y, z, "minecraft:bamboo_mosaic_stairs", trap(opp))
                for yy in range(G + 1, y):
                    s.set(x, yy, z, "minecraft:bamboo_mosaic")
        # a tiki torch on both sides of the foot of the stairs
        for m in (c - 2, c + 2):
            if dx:
                tx, tz = (x0 if dx < 0 else x1) + dx * (F - G), m
            else:
                tx, tz = m, (x0 if dz < 0 else x1) + dz * (F - G)
            s.set(tx, G + 1, tz, BAMBOE_HEK, {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
            s.set(tx, G + 2, tz, BAMBOE_HEK, {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
            s.set(tx, G + 3, tz, "minecraft:torch")
    # --- the walls ----------------------------------------------------------------------------------------------------------------
    deuren = {(w0, c): "west", (w1, c): "east", (29, w0): "north", (21, w1): "south"}
    for y in range(F + 1, F + 5):
        for k in range(w0, w1 + 1):
            for (x, z) in ((k, w0), (k, w1), (w0, k), (w1, k)):
                hoek = x in (w0, w1) and z in (w0, w1)
                s.set(x, y, z, STAM if hoek else PLANK, {"axis": "y"} if hoek else None)
    for y in range(F + 2, F + 4):   # windows: pairs of light-blue panes
        for k in (19, 20, 23, 27, 30, 31):
            for (x, z) in ((k, w0), (k, w1), (w0, k), (w1, k)):
                if abs(k - c) > 1:
                    s.set(x, y, z, "minecraft:light_blue_stained_glass_pane", {"east": "false", "west": "false", "north": "false",
                                                                                "south": "false", "waterlogged": "false"})
    for (x, z), f in deuren.items():
        s.set(x, F + 1, z, "minecraft:bamboo_door", {"facing": {"west": "east", "east": "west", "north": "south", "south": "north"}[f],
                                                      "half": "lower", "hinge": "left", "open": "false", "powered": "false"})
        s.set(x, F + 2, z, "minecraft:bamboo_door", {"facing": {"west": "east", "east": "west", "north": "south", "south": "north"}[f],
                                                      "half": "upper", "hinge": "left", "open": "false", "powered": "false"})
    # the wall between Lilo's room (west) and the kitchen (east), with an open doorway
    for y in range(F + 1, F + 5):
        for z in range(w0 + 1, w1):
            if not (z == c and y <= F + 2):
                s.set(c, y, z, PLANK)
    for x in range(w0 + 1, w1):
        for z in range(w0 + 1, w1):
            for y in range(F + 1, F + 5):
                if s.get(x, y, z) is None:
                    s.set(x, y, z, AIR)
    # --- the roof: a thatched hip roof over the veranda, two pink guh ears on the top -------------------------------------------
    for x in range(x0 - 1, x1 + 2):
        for z in range(x0 - 1, x1 + 2):
            s.set(x, F + 5, z, HOOI, {"axis": "y"})
    for k in range(1, 6):
        for x in range(x0 - 1 + 2 * k, x1 + 2 - 2 * k):
            for z in range(x0 - 1 + 2 * k, x1 + 2 - 2 * k):
                rand = min(x - (x0 - 1 + 2 * k), (x1 + 1 - 2 * k) - x, z - (x0 - 1 + 2 * k), (x1 + 1 - 2 * k) - z)
                if rand <= 2 or k == 5:
                    s.set(x, F + 5 + k, z, HOOI, {"axis": "y"})
    top = F + 5 + 5                                           # the 3 x 3 peak
    for ex in (c - 1, c + 1):
        for (dy, dz, blok) in ((1, 0, "minecraft:pink_wool"), (2, 0, "minecraft:pink_wool"), (1, 1, "minecraft:pink_wool"),
                               (1, -1, "minecraft:pink_wool"), (3, 0, "minecraft:pink_wool")):
            s.set(ex, top + dy, c + dz, blok)
        s.set(ex, top + 2, c - 1 if ex < c else c + 1, "minecraft:magenta_wool")
    # lanterns under the eaves (hanging) at the veranda corners and over the stairs
    for (x, z) in ((x0, x0), (x0, x1), (x1, x0), (x1, x1), (x0, c), (x1, c), (c, x0), (c, x1)):
        s.set(x, F + 4, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    # --- Lilo's room (west) ---------------------------------------------------------------------------------------------------
    y = F + 1
    s.set(19, y, 19, "minecraft:red_bed", {"facing": "south", "part": "head", "occupied": "false"})
    s.set(19, y, 20, "minecraft:red_bed", {"facing": "south", "part": "foot", "occupied": "false"})
    for (x, z) in ((21, 18), (22, 18), (21, 19), (22, 19)):      # 626's little basket
        s.set(x, y, z, "minecraft:blue_carpet")
    for (x, z) in ((20, 18), (23, 18), (23, 19), (20, 19)):
        s.set(x, y, z, "minecraft:oak_trapdoor", {"facing": "north", "half": "bottom", "open": "false", "powered": "false",
                                                 "waterlogged": "false"})
    s.set(18, y, 31, "minecraft:jukebox", {"has_record": "false"})
    s.set(18, y + 1, 31, "minecraft:potted_cactus")
    s.set(24, y, 32, "guhs:potted_roze_hibiscus")
    s.set(18, y, 22, "guhs:potted_guhwaii_orchidee")
    s.set(18, y + 1, 26, "guhs:vadsigheid_poster", {"facing": "east"})
    s.set(22, y, 26, "minecraft:red_carpet")
    s.set(21, y, 26, "minecraft:pink_carpet")
    s.set(22, y, 27, "minecraft:pink_carpet")
    s.set(21, y, 27, "minecraft:red_carpet")
    s.set(18, y, 28, "minecraft:chest", {"facing": "east", "type": "single", "waterlogged": "false"},
          {"id": "minecraft:chest", "LootTable": "minecraft:chests/village/village_fisher"})
    s.set(24, y + 3, 21, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    # --- Nani's kitchen (east) --------------------------------------------------------------------------------------------------
    for z in range(18, 23):
        s.set(32, y, z, "minecraft:barrel", {"facing": "up", "open": "false"})
        if z in (18, 22):
            s.set(32, y + 1, z, "minecraft:smooth_quartz_slab", {"type": "bottom", "waterlogged": "false"})
    s.set(32, y, 23, "minecraft:smoker", {"facing": "west", "lit": "false"})
    s.set(32, y, 20, "minecraft:water_cauldron", {"level": "3"})
    s.set(32, y + 1, 19, "guhs:kokosnoot", {"rijp": "2", "hangend": "false"})
    s.set(32, y + 1, 21, "guhs:kokosnoot", {"rijp": "1", "hangend": "false"})
    s.set(32, y + 2, 20, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # the table with two stools
    s.set(28, y, 29, BAMBOE_HEK, {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
    s.set(28, y + 1, 29, "minecraft:bamboo_pressure_plate", {"powered": "false"})
    s.set(27, y, 29, "minecraft:bamboo_stairs", trap("east"))
    s.set(29, y, 29, "minecraft:bamboo_stairs", trap("west"))
    s.set(31, y, 32, "guhs:potted_guhwaii_plumeria")
    s.set(26, y, 18, "guhs:potted_roze_hibiscus")
    s.set(30, y + 3, 26, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    # --- the veranda: beach chairs, pots -----------------------------------------------------------------------------------------
    for (x, z, f) in ((x0 + 1, 18, "east"), (x0 + 1, 31, "east"), (x1 - 1, 31, "west")):
        s.set(x, F + 1, z, "minecraft:bamboo_stairs", trap(f))
    for (x, z) in ((x0 + 1, x0 + 1), (x1 - 1, x1 - 1), (x1 - 1, x0 + 1), (x0 + 1, x1 - 1)):
        s.set(x, F + 1, z, "guhs:" + rng.choice(["potted_roze_hibiscus", "potted_guhwaii_plumeria", "potted_guhwaii_paradijsbloem"]))
    # --- the asiel "Pootje Thuis" (north-east) -------------------------------------------------------------------------------------
    ax0, ax1, az0, az1 = 38, 47, 3, 11
    for x in range(ax0, ax1 + 1):
        for z in range(az0, az1 + 1):
            s.set(x, G, z, "minecraft:smooth_sandstone")
            rand = x in (ax0, ax1) or z in (az0, az1)
            for yy in range(G + 1, G + 4):
                if rand:
                    hoek = x in (ax0, ax1) and z in (az0, az1)
                    s.set(x, yy, z, STAM if hoek else "minecraft:stripped_bamboo_block", {"axis": "y"})
                else:
                    s.set(x, yy, z, AIR)
    # a gabled roof of bamboo mosaic (the ridge along x), little pink guh ears on both ends of the ridge
    for k in range(6):
        y = G + 4 + k
        for x in range(ax0 - 1, ax1 + 2):
            if k < 5:
                s.set(x, y, az0 - 1 + k, "minecraft:bamboo_mosaic_stairs", trap("south"))
                s.set(x, y, az1 + 1 - k, "minecraft:bamboo_mosaic_stairs", trap("north"))
            else:
                s.set(x, y, az0 - 1 + k, "minecraft:bamboo_mosaic")
        for z in range(az0 + k, az1 + 1 - k):
            if k < 5:
                for x in (ax0, ax1):
                    s.set(x, y, z, "minecraft:stripped_bamboo_block", {"axis": "y"})
                for x in range(ax0 + 1, ax1):
                    s.set(x, y, z, AIR)
    ridge = az0 + 4
    for x in (ax0 - 1, ax1 + 1):
        s.set(x, G + 10, ridge, "minecraft:pink_wool")
        s.set(x, G + 11, ridge, "minecraft:pink_wool")
        s.set(x, G + 10, ridge - 1 if x < ax1 else ridge + 1, "minecraft:magenta_wool")
    # its door (south side) and windows, three kennels with little beds inside
    s.set(42, G + 1, az1, AIR)
    s.set(42, G + 2, az1, AIR)
    for x in (39, 45):
        s.set(x, G + 2, az1, "minecraft:bamboo_fence", {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
    for i, x in enumerate((39, 42, 45)):
        s.set(x, G + 1, az0 + 1, ["minecraft:pink_wool", "minecraft:light_blue_wool", "minecraft:yellow_wool"][i])
        for z in (az0 + 2, az0 + 3):
            s.set(x - 1, G + 1, z, BAMBOE_HEK, {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
            s.set(x + 1, G + 1, z, BAMBOE_HEK, {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
        s.set(x, G + 1, az0 + 4, "minecraft:oak_fence_gate", {"facing": "south", "in_wall": "false", "open": "false", "powered": "false"})
    s.set(46, G + 1, 10, "minecraft:composter", {"level": "0"})
    for x in (40, 44):
        s.set(x, G + 3, 8, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
        for y in range(G + 4, G + 8):
            s.set(x, y, 8, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    sign(s, 44, G + 1, az1 + 2, 0, ["Guh-asiel", "Pootje Thuis", "Adopteer een", "vriendje! Njeg"])
    # --- the garden: palms, the fire pit, a sandcastle, stepping stones, flowers --------------------------------------------------
    palm(s, 6, G, 6, 7, "south", "south", rng, 3)
    palm(s, 44, G, 40, 6, "west", "west", rng, 2)
    palm(s, 8, G, 42, 7, "east", "east", rng, 2)
    palm(s, 45, G, 22, 5, "west", "west", rng, 2)
    palm(s, 4, G, 24, 6, "north", "east", rng, 1)
    # the fire pit with log seats
    fx, fz = 10, 32
    s.set(fx, G, fz, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for (dx, dz, f) in ((2, 0, "west"), (-2, 0, "east"), (0, 2, "north"), (0, -2, "south")):
        s.set(fx + dx, G + 1, fz + dz, STAM, {"axis": "x" if dx else "z"})
    for (dx, dz) in ((1, 1), (-1, -1), (1, -1), (-1, 1)):
        s.set(fx + dx, G, fz + dz, "minecraft:smooth_sandstone")
    # a sandcastle with a pink flag
    sx, sz = 40, 31
    for (dx, dz) in ((0, 0), (2, 0), (0, 2), (2, 2)):
        for yy in (G + 1, G + 2):
            s.set(sx + dx, yy, sz + dz, "minecraft:sandstone")
        s.set(sx + dx, G + 3, sz + dz, "minecraft:sandstone_wall", {"east": "none", "west": "none", "north": "none", "south": "none",
                                                                     "up": "true", "waterlogged": "false"})
    for (dx, dz) in ((1, 0), (0, 1), (2, 1), (1, 2), (1, 1)):
        s.set(sx + dx, G + 1, sz + dz, "minecraft:smooth_sandstone")
    s.set(sx + 1, G + 2, sz + 1, "minecraft:sandstone_wall", {"east": "none", "west": "none", "north": "none", "south": "none",
                                                               "up": "true", "waterlogged": "false"})
    s.set(sx + 1, G + 3, sz + 1, "minecraft:pink_banner", {"rotation": "4"})
    # stepping stones from each staircase outwards
    for kant, (dx, dz) in trede.items():
        for k in range(F - G + 1, F - G + 6, 2):
            x = (x0 if dx < 0 else x1) + dx * k if dx else c
            z = (x0 if dz < 0 else x1) + dz * k if dz else c
            if s.inside(x, G, z) and s.get(x, G + 1, z) in (None, AIR):
                s.set(x, G, z, "minecraft:smooth_sandstone")
    for (x, z) in ((3, 44), (47, 47), (47, 16), (2, 14)):
        s.set(x, G + 1, z, "guhs:schilly_eitjes", {"eitjes": str(rng.randint(2, 4)), "rijp": "0"})
    # a parasol with beach towels, flower beds at the stairs and round the asiel
    px, pz = 9, 14
    for y in range(G + 1, G + 4):
        s.set(px, y, pz, BAMBOE_HEK, {"east": "false", "west": "false", "north": "false", "south": "false", "waterlogged": "false"})
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            s.set(px + dx, G + 4, pz + dz, "minecraft:pink_wool" if (dx + dz) % 2 == 0 else "minecraft:white_wool")
    s.set(px, G + 5, pz, "minecraft:pink_wool")
    for (x, z, kleur) in ((7, 16, "light_blue"), (7, 17, "light_blue"), (11, 16, "pink"), (11, 17, "pink")):
        s.set(x, G + 1, z, f"minecraft:{kleur}_carpet")
    for kant, (dx, dz) in trede.items():
        for m in (c - 3, c + 3):
            for k in (F - G, F - G + 1):
                x = (x0 if dx < 0 else x1) + dx * k if dx else m
                z = (x0 if dz < 0 else x1) + dz * k if dz else m
                if s.get(x, G + 1, z) in (None, AIR) and s.get(x, G, z) in (ZAND, "minecraft:grass_block"):
                    s.set(x, G + 1, z, BLOEMEN[(x + z) % 4])
    for x in range(ax0 - 1, ax1 + 2):
        if s.get(x, G + 1, az1 + 1) in (None, AIR) and x not in (42,):
            s.set(x, G + 1, az1 + 1, BLOEMEN[x % 4])
    bloemen(s, 0, 0, W - 1, W - 1, G + 1, rng, 0.025)
    # --- the NPCs, the story copy of 626, the Reisguh -----------------------------------------------------------------------------
    ms = h.ms
    lilo = (22, F + 1, 34)
    nani = (29, F + 1, 22)
    s.entity(lilo[0] + 0.5, float(lilo[1]), lilo[2] + 0.5, {"id": "guhs:guh_npc", "Kind": "lilo_guh", "PersistenceRequired": ms.Byte(1),
                                                            "Rotation": ms.floats(0.0, 0.0)})
    s.entity(nani[0] + 0.5, float(nani[1]), nani[2] + 0.5, {"id": "guhs:guh_npc", "Kind": "nani_guh", "PersistenceRequired": ms.Byte(1),
                                                            "Rotation": ms.floats(90.0, 0.0)})
    kopie = (42, G + 1, 15)
    s.entity(kopie[0] + 0.5, float(kopie[1]), kopie[2] + 0.5,
             ms.guh_nbt(1.25, Variant="stitch626", Personality="playful",
                        NeoForgeData={"guhs_verhaal_guh": "stitch626"}, Rotation=ms.floats(0.0, 0.0)))
    reis = reisguh_plek.zet(s, reisguh_plek.rondom(c + 6, G + 1, 44, 4), "Ohana op Guhwai'i", 180, ms.Byte, ms.floats, "guhwaii_ohana")
    # --- the anchor, clearing, save -----------------------------------------------------------------------------------------------
    jigsaw(s, c, G, c, "guhs:guhwaii_ohana_midden", "minecraft:water")
    s.clear_above([(x, z) for x in range(W) for z in range(W)], G + 1, top=H)
    check(s, "guhwaii_ohana", [lilo, nani, kopie, reis], G)
    s.save("guhwaii_ohana")
    h.w(f"{h.D}/worldgen/template_pool/guhwaii_ohana/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": "guhs:guhwaii_ohana",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    return "guhs:guhwaii_ohana/start", G


# =====================================================================================================================
# the capsule
# =====================================================================================================================
LOGBOEK = [
    "Logboek van\nEXPERIMENT 626\n\nGemaakt door de\ngekke professor\nJumba-guh.",
    "Doel: de allervadsigste guh ooit maken.\nZo vads dat hij TE VADS is.\n\n...",
    "Meting 1: vads.\nMeting 2: heel vads.\nMeting 3: VAHOEG.\nMeting 4: de meter is kapot. Njeg?!",
    "Conclusie:\nEen guh kan nooit te vads zijn.\n\nONBEREKENBAAR VAHOEG.\n\n(626 is ontsnapt. Hij is lief.)",
]


def capsule(h):
    rng = random.Random(30202)
    W, H = CAPSULE_W, CAPSULE_H
    c = W // 2
    s = h.Structure((W, H, W))
    # --- the hill top: grass, the crater, the skid trail ------------------------------------------------------------------------
    for x in range(W):
        for z in range(W):
            d = math.hypot(x - c, z - c)
            spoor = abs(x - c) <= 2 and z > c + 6
            for y in range(G):
                s.set(x, y, z, "minecraft:dirt")
            if d < 11:
                diep = 2 if d < 8 else 1
                for y in range(G - diep + 1, G + 1):
                    s.set(x, y, z, AIR)
                s.set(x, G - diep, z, "minecraft:coarse_dirt" if rng.random() < 0.6 else ZAND)
            elif spoor:
                s.set(x, G, z, AIR)
                s.set(x, G - 1, z, "minecraft:coarse_dirt" if rng.random() < 0.7 else "minecraft:rooted_dirt")
            else:
                wal = 11 <= d < 12.5
                s.set(x, G, z, "minecraft:dirt" if wal else "minecraft:grass_block", {"snowy": "false"} if not wal else None)
                if wal:
                    if rng.random() < 0.8:
                        s.set(x, G + 1, z, "minecraft:grass_block", {"snowy": "false"})
                    else:
                        s.set(x, G + 1, z, "minecraft:coarse_dirt")
    # --- the pod: a white ball, half sunk, a pink band, a cracked glass dome, antennas ----------------------------------------------
    cy = G + 2
    R = 6
    for x in range(c - R - 1, c + R + 2):
        for y in range(G - 2, cy + R + 2):
            for z in range(c - R - 1, c + R + 2):
                d = math.dist((x, y, z), (c, cy, c))
                if d <= R + 0.3:
                    if d > R - 1.3:
                        if y >= cy + 3:
                            blok = "minecraft:light_blue_stained_glass" if rng.random() > 0.12 else AIR
                        elif y == cy:
                            blok = "minecraft:pink_concrete"
                        elif y == cy + 1 or y == cy - 1:
                            blok = "minecraft:white_concrete" if (x + z) % 5 else "minecraft:sea_lantern"
                        else:
                            blok = "minecraft:white_concrete"
                        s.set(x, y, z, blok)
                    else:
                        s.set(x, y, z, AIR)
    for x in range(c - 5, c + 6):
        for z in range(c - 5, c + 6):
            if math.hypot(x - c, z - c) <= 4.9:
                s.set(x, G - 1, z, "minecraft:polished_andesite" if (x + z) % 2 else "minecraft:smooth_stone")
    for (x, z) in ((c - 3, c - 3), (c + 3, c + 3), (c - 3, c + 3), (c + 3, c - 3)):
        s.set(x, G - 1, z, "minecraft:sea_lantern")
    # two guh-ear fins on top (626's pod is a guh pod after all), pink with a magenta inside
    for ex in (c - 3, c + 3):
        top = max(y for y in range(G, cy + R + 2) if R - 1.3 < math.dist((ex, y, c), (c, cy, c)) <= R + 0.3)
        for (dy, dz) in ((1, 0), (2, 0), (3, 0), (1, -1), (1, 1), (2, -1), (2, 1)):
            s.set(ex, top + dy, c + dz, "minecraft:pink_concrete")
        s.set(ex + (1 if ex > c else -1), top + 2, c, "minecraft:magenta_concrete")
        s.set(ex, top, c, "minecraft:white_concrete")
    # antennas on top, each on the highest glass of its column (made whole there)
    for (x, z, blok, props) in ((c, c - 2, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"}),
                                (c, c + 2, "minecraft:end_rod", {"facing": "up"})):
        hoogste = max(y for y in range(G, cy + R + 2) if R - 1.3 < math.dist((x, y, z), (c, cy, c)) <= R + 0.3)
        s.set(x, hoogste, z, "minecraft:light_blue_stained_glass")
        s.set(x, hoogste + 1, z, blok, props)
    # the hatch (south) and its ramp down into the crater
    for x in range(c - 1, c + 2):
        for y in range(G, G + 3):
            s.set(x, y, c + R, AIR)
            s.set(x, y, c + R - 1, AIR)
        s.set(x, G - 1, c + R, "minecraft:smooth_stone")
        s.set(x, G - 1, c + R - 1, "minecraft:smooth_stone")
        s.set(x, G - 1, c + R + 1, "minecraft:smooth_stone_slab", {"type": "bottom", "waterlogged": "false"})
    # --- inside: the scanner (free-standing), the pilot seat, the control panel, the logbook, the poster -------------------------
    vloer = G
    s.set(c, vloer, c, "guhs:vadsigheid_scanner", {"facing": "south"})
    s.set(c, vloer, c - 3, "minecraft:quartz_stairs", trap("north"))
    for x in range(c - 2, c + 3):
        s.set(x, vloer, c - 4, "minecraft:polished_andesite")
        s.set(x, vloer, c - 5, "minecraft:polished_andesite")
        if x % 2:
            s.set(x, vloer + 1, c - 4, "minecraft:stone_button", {"face": "floor", "facing": "south", "powered": "false"})
        else:
            s.set(x, vloer + 1, c - 4, "minecraft:sea_lantern")
    # 1.2.0: the pages and the book's name are translate keys (book.guhs.logboek626.*, the Dutch as fallback); the title
    # and author of a written book are plain strings in Minecraft (no components), so those stay Dutch
    import sign_text
    pages = [sign_text.line("book.guhs.logboek626", p, key=f"book.guhs.logboek626.{n + 1}") for n, p in enumerate(LOGBOEK)]
    book = {"id": "minecraft:written_book", "count": 1, "components": {
        "minecraft:written_book_content": {"title": {"raw": "Logboek 626"}, "author": "Jumba-guh", "generation": 0,
                                           "pages": compounds([{"raw": p} for p in pages])},
        "minecraft:custom_name": sign_text.line("book.guhs.logboek626", "Logboek 626", key="book.guhs.logboek626.title", italic=False)}}
    s.set(c + 4, vloer, c - 1, "minecraft:lectern", {"facing": "west", "has_book": "true", "powered": "false"},
          {"id": "minecraft:lectern", "Book": book, "Page": 0})
    s.set(c - 4, vloer + 1, c, "guhs:vadsigheid_poster", {"facing": "east"})
    s.set(c - 5, vloer + 1, c, "minecraft:white_concrete")
    # --- around: smoke, debris, flattened flowers, a bent palm -------------------------------------------------------------------
    s.set(c - 6, G - 1, c + 4, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    s.set(c - 7, G - 1, c + 4, "minecraft:gravel")
    s.set(c - 5, G - 1, c + 4, "minecraft:gravel")
    for (x, z) in ((c + 6, c + 3), (c - 4, c - 7), (c + 7, c - 3), (c + 3, c + 9), (c - 2, c + 12)):
        for y in (G - 1, G, G + 1, G + 2):
            if s.get(x, y, z) in (None, AIR) and s.get(x, y - 1, z) not in (None, AIR):
                s.set(x, y, z, "minecraft:iron_trapdoor", {"facing": "north", "half": "bottom", "open": "false", "powered": "false",
                                                          "waterlogged": "false"})
                break
    palm(s, 3, G, 4, 6, "east", "east", rng, 2)
    palm(s, 30, G, 29, 5, "north", "north", rng, 1)
    bloemen(s, 0, 0, W - 1, W - 1, G + 1, rng, 0.06)
    for x in range(W):
        for z in range(W):
            if s.get(x, G + 1, z) is None and s.get(x, G, z) == "minecraft:grass_block" and rng.random() < 0.05:
                s.set(x, G + 1, z, "guhs:" + rng.choice(["roze_hibiscus", "guhwaii_plumeria"]))
    sign(s, c + 3, G, c + 8, 8, ["EXPERIMENT 626", "NIET AANRAKEN!", "(behalve als je", "een guh bent)"])
    ms = h.ms
    reis = reisguh_plek.zet(s, reisguh_plek.rondom(c + 9, G + 1, c + 11, 4), "De capsule van 626", 0, ms.Byte, ms.floats, "guhwaii_capsule")
    jigsaw(s, c + 11, G, c, "guhs:guhwaii_capsule_midden", "minecraft:grass_block")
    s.clear_above([(x, z) for x in range(W) for z in range(W)], G + 2, top=H)
    check(s, "guhwaii_capsule", [(c, G + 1, c + 1), reis], G - 2)
    s.save("guhwaii_capsule")
    h.w(f"{h.D}/worldgen/template_pool/guhwaii_capsule/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": "guhs:guhwaii_capsule",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    return "guhs:guhwaii_capsule/start", G


# =====================================================================================================================
# checks and tests
# =====================================================================================================================
def check(s, name, plekken, ground_y):
    problems = []
    zwevend = hulp.check_floating(s, ground_y)
    if zwevend:
        problems.append(f"{len(zwevend)} floating blocks, e.g. {zwevend[:6]}")
    extra = tuple(BLOEMEN) + ("guhs:schilly_eitjes", "guhs:kokosnoot", "minecraft:bamboo_door", "minecraft:oak_fence_gate",
                              "guhs:guhwaii_palm_kiemplant")
    W = s.size[0]
    starts = [(x, y, z) for x in range(W) for z in (0, W - 1) for y in range(1, s.size[1] - 1)] + \
             [(x, y, z) for z in range(W) for x in (0, W - 1) for y in range(1, s.size[1] - 1)]
    bereik = hulp.walk(s, starts, extra_passable=extra)
    for p in plekken:
        if not hulp.near_reachable(bereik, *p, r=2):
            problems.append(f"{p} can't be walked to")
    if reisguh_plek.aantal(s) != 1:
        problems.append(f"{reisguh_plek.aantal(s)} Reisguhs")
    if sum(1 for b in s.blocks.values() if b[0] == "minecraft:jigsaw") != 1:
        problems.append("not one jigsaw")
    if problems:
        raise SystemExit(f"{name} self-check failed:\n  " + "\n  ".join(problems))


def tests(h):
    t = h.Structure((12, 8, 12))
    for x in range(12):
        for z in range(12):
            t.set(x, 0, z, ZAND)
    for x in range(9, 12):
        for z in range(12):
            t.set(x, 0, z, "minecraft:water")
    t.save("guhwaii_test_strand")
    m = h.Structure((10, 8, 10))
    for x in range(10):
        for z in range(10):
            m.set(x, 0, z, "minecraft:smooth_stone")
            m.set(x, 5, z, "minecraft:smooth_stone")        # a ceiling at helper y 6 (4 free blocks above the floor)
    for z in range(10):
        for y in range(1, 5):
            m.set(0, y, z, "minecraft:smooth_stone")        # a wall on the west side
    for x in range(1, 10):
        for z in range(10):
            for y in range(1, 5):
                m.set(x, y, z, AIR)
    m.save("guhwaii_test_muur")
    hu = h.Structure((20, 8, 20))
    for x in range(20):
        for z in range(20):
            hu.set(x, 0, z, "minecraft:smooth_stone")
    hu.save("guhwaii_test_huisje")
    sc = h.Structure((9, 6, 9))
    for x in range(9):
        for z in range(9):
            sc.set(x, 0, z, "minecraft:smooth_stone")
    sc.set(4, 1, 4, "guhs:vadsigheid_scanner", {"facing": "south"})
    sc.save("guhwaii_test_scanner")
