"""
Het Ballonfestival (2.8, slice "buiten") - the festival field (template ballonfestival, 72 x 40 x 72) on the Guhvelden
and the Roze pluisjes:

  - in the middle the round ballonsteiger (steps all round) with Kapitein Wolkje's guh balloon waiting on it
  - Kapitein Wolkje's kiosk (a striped roof, a guh face, mini luchtballonnen on its corners) just south of it
  - north: the Guhgezicht-bloemenveld, a guh face of wool and flowers lying in the grass (seen from the balloon)
  - east: De Reuzenguh, a giant guh balloon lying half-inflated in the grass, with its face, its ears, its basket
  - west: three more guh balloons (decoration) floating on their own little steigers
  - south: the entrance arch with a big guh face, two festival stands (knabbels and taart), benches, lampions, paths
  - south-east: the Wolkjestribune, a big grandstand with a pluisdak awning, guh faces on its front, back and sides
  - south-west: the Ballonnententje, a big striped festival tent with a guh-faced entrance portal
  - north-west: the Uitkijkguh, a lookout tower with a giant guh head (a face on every side) on top
  - guh-face gates over the west and north paths; guh faces on the steiger posts
check(s): nothing floats, the steiger and the Kapitein can be walked to from the entrance, the balloons have room.
Run it alone:  python tools/features/ballon_bouw.py   (from the project root)
"""
import math
import os
import random
import sys

if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

from features import sterrenwacht_hulp as hulp  # noqa: E402

AIR = hulp.AIR
GEZICHT = "guhs:knuffelsteen_gezicht"

NAME = "ballonfestival"
W, H, D = 72, 40, 72
G = 4
C = 36
ANCHOR = "guhs:ballonfestival_midden"
KLINK = "guhs:knuffelklinkers"
KS = "guhs:knuffelsteen"
STEIGER = "guhs:ballonsteiger"
MINI = "guhs:mini_luchtballon"
NPC = (C, G + 1, C + 9)
BALLON = (C, G + 2, C)                 # home of the flight balloon (on top of the steiger)
DECO = [((12, 24), 1, 40.0), ((10, 48), 2, 130.0), ((24, 62), 3, 250.0)]
REUZENGUH = (58, 36)
BLOEMENVELD = (C, 12)
BLOEMEN = ["guhs:roze_gras", "guhs:guhoortjes", "guhs:kaasbloem", "guhs:roze_guhbloem"]


def dist(x, z, cx=C, cz=C):
    return math.hypot(x - cx, z - cz)


def fence(s, x, y, z):
    s.set(x, y, z, "minecraft:spruce_fence", {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})


def build(h):
    s = h.Structure((W, H, D))
    rng = random.Random(28702)
    info = {"gezichten": 0}
    paden = set()
    # --- ground, paths --------------------------------------------------------------------------------------------------
    for x in range(W):
        for z in range(D):
            for y in range(G):
                s.set(x, y, z, "minecraft:pink_wool")
            d = dist(x, z)
            pad = d <= 9 or 13 <= d <= 15.5 or (abs(x - C) <= 1 and (z > C + 14 or z < C - 14)) or (abs(z - C) <= 1 and (x > C + 14 or x < C - 14))
            s.set(x, G, z, KLINK if pad else "minecraft:pink_wool")
            if pad:
                paden.add((x, z))
    # --- the steiger in the middle ------------------------------------------------------------------------------------------
    for x in range(C - 5, C + 6):
        for z in range(C - 5, C + 6):
            d = dist(x, z)
            if d <= 3.6:
                s.set(x, G + 1, z, STEIGER)
            elif d <= 4.6:
                dx, dz = x - C, z - C
                f = ("east" if dx > 0 else "west") if abs(dx) >= abs(dz) else ("south" if dz > 0 else "north")
                s.set(x, G + 1, z, "guhs:knuffelsteen_trap", {"facing": hulp.OPP[f], "half": "bottom", "shape": "straight", "waterlogged": "false"})
    for (x, z) in ((C - 5, C - 5), (C + 5, C - 5), (C - 5, C + 5), (C + 5, C + 5)):
        s.set(x, G + 1, z, KS)
        fence(s, x, G + 2, z)
        s.set(x, G + 3, z, MINI)
    # --- Kapitein Wolkje's kiosk (south of the steiger, open to the north) --------------------------------------------------
    kx0, kx1, kz0, kz1 = C - 3, C + 3, C + 7, C + 11
    for x in range(kx0, kx1 + 1):
        for z in range(kz0, kz1 + 1):
            s.set(x, G, z, "minecraft:spruce_planks")
    for (x, z) in ((kx0, kz0), (kx1, kz0), (kx0, kz1), (kx1, kz1)):
        for y in range(G + 1, G + 5):
            s.set(x, y, z, "minecraft:stripped_spruce_log", {"axis": "y"})
    for x in range(kx0 + 1, kx1):
        s.set(x, G + 1, kz1, "minecraft:spruce_planks")
        s.set(x, G + 2, kz1, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    for z in range(kz0 + 1, kz1):
        for x in (kx0, kx1):
            s.set(x, G + 1, z, "minecraft:spruce_planks")
            s.set(x, G + 2, z, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    # the counter at the front, with a gap in the middle
    for x in (kx0 + 1, kx0 + 2, kx1 - 2, kx1 - 1):
        s.set(x, G + 1, kz0, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
    s.set(kx0 + 1, G + 2, kz0, MINI)
    # the striped roof (a little pyramid of wool) with a guh face looking at the steiger
    for lvl in range(4):
        for x in range(kx0 - 1 + lvl, kx1 + 2 - lvl):
            for z in range(kz0 - 1 + lvl, kz1 + 2 - lvl):
                edge = x in (kx0 - 1 + lvl, kx1 + 1 - lvl) or z in (kz0 - 1 + lvl, kz1 + 1 - lvl)
                if edge or lvl >= 0:
                    s.set(x, G + 5 + lvl, z, "minecraft:pink_wool" if (x + z + lvl) % 2 else "minecraft:white_wool")
    s.set(C, G + 5, kz0 - 1, "guhs:knuffelsteen_gezicht", {"facing": "north", "stemming": "0"})
    s.set(C, G + 5, kz1 + 1, "guhs:knuffelsteen_gezicht", {"facing": "south", "stemming": "3"})
    info["gezichten"] += 2
    for (x, z) in ((kx0 - 1, kz0 - 1), (kx1 + 1, kz0 - 1), (kx0 - 1, kz1 + 1), (kx1 + 1, kz1 + 1)):
        s.set(x, G + 6, z, MINI)
    s.set(C, G + 9, C + 9, MINI)
    s.set(kx1 - 1, G + 1, kz1 - 1, "minecraft:chest", {"facing": "north", "type": "single", "waterlogged": "false"},
          {"id": "minecraft:chest", "LootTable": "guhs:chests/ballonfestival"})
    # --- the Guhgezicht-bloemenveld (north) ---------------------------------------------------------------------------------
    bx, bz = BLOEMENVELD
    n = hulp.floor_face(s, bx, bz, G, 10, up="north")
    info["gezichten"] += 1
    for x in range(bx - 13, bx + 14):
        for z in range(bz - 13, bz + 13):
            if 0 <= z < D and s.get(x, G, z) == "minecraft:pink_wool" and rng.random() < 0.35 and (x, z) not in paden:
                s.set(x, G + 1, z, BLOEMEN[rng.randrange(len(BLOEMEN))])
    info["bloemenveld"] = n
    # --- De Reuzenguh (east): a giant guh balloon lying half-inflated in the grass ----------------------------------------
    rx, rz = REUZENGUH
    ry = G + 6
    for x in range(rx - 11, min(W, rx + 12)):
        for z in range(rz - 9, rz + 10):
            for y in range(G + 1, G + 14):
                e = ((x - rx) / 10.5) ** 2 + ((y - ry) / 7.5) ** 2 + ((z - rz) / 8.5) ** 2
                if e <= 1 and y >= G + 1:
                    if e >= 0.72:
                        ang = math.degrees(math.atan2(z - rz, y - ry)) % 40
                        blk = "minecraft:white_wool" if ang < 20 else "minecraft:pink_wool"
                        if x < rx - 4:
                            role = hulp.face_role(-(z - rz), y - (ry + 1), 6)
                            if role and role not in ("ear", "ear_in"):
                                blk = hulp.FACE_WOOL[role] if role != "skin" else "minecraft:pink_wool"
                        s.set(x, y, z, blk)
                    else:
                        s.set(x, y, z, "minecraft:white_wool")
    info["gezichten"] += 1
    for sz in (-1, 1):                  # its ears, flopped up on top
        ez = rz + sz * 5
        for y in range(G + 11, G + 18):
            for z in range(ez - 3, ez + 4):
                if math.dist((y, z), (G + 14.5, ez)) <= 3.2:
                    for x in (rx - 1, rx):
                        s.set(x, y, z, "minecraft:magenta_wool" if math.dist((y, z), (G + 14.5, ez)) <= 1.8 and x == rx - 1 else "minecraft:pink_wool")
    # its basket, ropes and burner next to it
    for x in range(rx - 16, rx - 11):
        for z in range(rz + 6, rz + 11):
            s.set(x, G + 1, z, "minecraft:spruce_planks")
            if x in (rx - 16, rx - 12) or z in (rz + 6, rz + 10):
                s.set(x, G + 2, z, "minecraft:spruce_planks")
                s.set(x, G + 3, z, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    s.set(rx - 14, G + 2, rz + 8, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    for z in range(rz + 3, rz + 7):
        s.set(rx - 12, G + 1, z, "minecraft:white_carpet")
    # --- the three decoration balloons (west), each on its own steiger with mooring posts -----------------------------------
    ms = h.ms
    for (dx_, dz_), kleur, yaw in DECO:
        for x in range(dx_ - 2, dx_ + 3):
            for z in range(dz_ - 2, dz_ + 3):
                s.set(x, G + 1, z, STEIGER)
        for (x, z) in ((dx_ - 3, dz_ - 3), (dx_ + 3, dz_ + 3), (dx_ - 3, dz_ + 3), (dx_ + 3, dz_ - 3)):
            fence(s, x, G + 1, z)
            fence(s, x, G + 2, z)
            s.set(x, G + 3, z, "guhs:lampion_roze" if kleur % 2 else "guhs:lampion_mint", {"hanging": "false"})
        s.entity(dx_ + 0.5, float(G + 2), dz_ + 0.5, {"id": "guhs:guh_luchtballon", "Kleur": kleur, "Deco": ms.Byte(1),
                                                       "Rotation": ms.floats(yaw, 0.0), "PersistenceRequired": ms.Byte(1)})
    # the flight balloon on the middle steiger, facing the kiosk (south)
    s.entity(BALLON[0] + 0.5, float(BALLON[1]), BALLON[2] + 0.5, {"id": "guhs:guh_luchtballon", "Kleur": 0, "Rotation": ms.floats(0.0, 0.0)})
    # --- the entrance arch (south) ------------------------------------------------------------------------------------------
    az = 67
    for x in range(C - 6, C + 7):
        for y in range(G + 1, G + 12):
            pillar = abs(x - C) >= 5
            top = y >= G + 9
            if pillar or top:
                for z in (az, az + 1):
                    s.set(x, y, z, KS)
    for x in range(C - 7, C + 8):
        for z in (az - 1, az + 2):
            s.set(x, G + 12, z, "guhs:pluisdak_plaat", {"type": "bottom", "waterlogged": "false"})
        for z in (az, az + 1):
            s.set(x, G + 12, z, "guhs:pluisdak")
    n = hulp.wall_face(s, C, G + 17, az + 1, 4.2, "south")
    hulp.wall_face(s, C, G + 17, az, 4.2, "north")
    info["gezichten"] += 2
    # (a solid core behind the two faces so the head hangs together)
    for x in range(C - 4, C + 5):
        for y in range(G + 13, G + 22):
            if s.get(x, y, az + 1) is not None and s.get(x, y, az) is None:
                s.set(x, y, az, "minecraft:pink_wool")
            if s.get(x, y, az) is not None and s.get(x, y, az + 1) is None:
                s.set(x, y, az + 1, "minecraft:pink_wool")
    for x in (C - 6, C + 6):
        s.set(x, G + 13, az, MINI)
        s.set(x, G + 13, az + 1, MINI)
    # --- two festival stands ------------------------------------------------------------------------------------------------
    for (sx, sz, waar) in ((22, 50, "knabbels"), (47, 52, "taart")):
        for x in range(sx, sx + 5):
            for z in range(sz, sz + 4):
                s.set(x, G, z, "minecraft:spruce_planks")
        for (x, z) in ((sx, sz), (sx + 4, sz), (sx, sz + 3), (sx + 4, sz + 3)):
            for y in range(G + 1, G + 4):
                fence(s, x, y, z)
        for x in range(sx - 1, sx + 6):
            for z in range(sz - 1, sz + 5):
                s.set(x, G + 4, z, "minecraft:pink_wool" if (x - sx) % 2 == 0 else "minecraft:white_wool")
        for x in range(sx + 1, sx + 4):
            s.set(x, G + 1, sz, "minecraft:spruce_planks")
            s.set(x, G + 2, sz, "guhs:block_of_kaasknabbels" if waar == "knabbels" and x != sx + 2 else "minecraft:cake" if x == sx + 2 else "minecraft:spruce_slab",
                  {} if waar == "knabbels" and x != sx + 2 else ({"bites": "0"} if x == sx + 2 else {"type": "bottom", "waterlogged": "false"}))
        s.set(sx + 2, G + 5, sz - 1, "guhs:knuffelsteen_gezicht", {"facing": "north", "stemming": "3"})
        s.set(sx + 2, G + 5, sz + 4, "guhs:knuffelsteen_gezicht", {"facing": "south", "stemming": "0"})
        info["gezichten"] += 2
    # --- benches and lampions round the ring ----------------------------------------------------------------------------------
    for k in range(12):
        a = k * math.pi / 6 + math.pi / 12
        x, z = C + round(17 * math.cos(a)), C + round(17 * math.sin(a))
        if s.get(x, G + 1, z) is not None or (x, z) in paden:
            continue
        if k % 2:
            dx, dz = x - C, z - C
            f = ("east" if dx > 0 else "west") if abs(dx) >= abs(dz) else ("south" if dz > 0 else "north")
            s.set(x, G + 1, z, "guhs:guh_bank", {"facing": hulp.OPP[f]})
        else:
            fence(s, x, G + 1, z)
            s.set(x, G + 2, z, ["guhs:lampion_roze", "guhs:lampion_geel", "guhs:lampion_mint"][k % 3], {"hanging": "false"})
    # --- the picnic lawn (north-east): tables, chairs, striped sunshades -------------------------------------------------------
    for (tx, tz) in ((50, 12), (58, 18), (50, 24), (62, 8)):
        s.set(tx, G + 1, tz, "guhs:guh_tafel", {"facing": "north"})
        for f, (dx, dz) in hulp.STEP.items():
            if rng.random() < 0.8:
                s.set(tx + dx, G + 1, tz + dz, "guhs:guh_stoel", {"facing": hulp.OPP[f]})
        px_, pz_ = tx + 1, tz + 1
        for y in range(G + 1, G + 4):
            fence(s, px_, y, pz_)
        for x in range(px_ - 1, px_ + 2):
            for z in range(pz_ - 1, pz_ + 2):
                s.set(x, G + 4, z, "minecraft:pink_wool" if (x + z) % 2 else "minecraft:white_wool")
        s.set(px_, G + 5, pz_, MINI)
    # --- tall flagpoles with a mini balloon on top (seen from far away) -------------------------------------------------------
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        x, z = C + round(21 * math.cos(a)), C + round(21 * math.sin(a))
        if s.get(x, G + 1, z) is not None:
            x += 1
        for y in range(G + 1, G + 13):
            fence(s, x, y, z)
        s.set(x, G + 13, z, MINI)
        for y in (G + 10, G + 11):
            s.set(x + 1, y, z, "minecraft:pink_wool" if y % 2 else "minecraft:white_wool")
            s.set(x + 2, y, z, "minecraft:white_wool" if y % 2 else "minecraft:pink_wool")
    # --- bunting along the entrance path ---------------------------------------------------------------------------------------
    for z in range(48, 66, 5):
        for x in (C - 3, C + 3):
            for y in range(G + 1, G + 6):
                fence(s, x, y, z)
            s.set(x, G + 6, z, "guhs:lampion_roze" if z % 2 else "guhs:lampion_geel", {"hanging": "false"})
        for x in range(C - 2, C + 3):
            s.set(x, G + 5, z, "guhs:vlaggetjes", {"axis": "x"})
    tribune(s, info)
    tent(s, info)
    uitkijkguh(s, info)
    poort(s, info, "west")
    poort(s, info, "north")
    # guh faces on the steiger's corner posts (looking out)
    for (x, z) in ((C - 5, C - 5), (C + 5, C - 5), (C - 5, C + 5), (C + 5, C + 5)):
        s.set(x, G + 1, z, GEZICHT, {"facing": "north" if z < C else "south", "stemming": str((x + z) % 4)})
        info["gezichten"] += 1
    # the anchor (the steiger's middle, under the balloon)
    s.set(C, G, C, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": KLINK,
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    # room for the balloons (9 blocks up, 2 round)
    for (bx_, bz_), y0 in [((BALLON[0], BALLON[2]), BALLON[1])] + [((d[0][0], d[0][1]), G + 2) for d in DECO]:
        for x in range(bx_ - 2, bx_ + 3):
            for z in range(bz_ - 2, bz_ + 3):
                for y in range(y0, min(H, y0 + 10)):
                    if s.get(x, y, z) not in (None, hulp.AIR):
                        info.setdefault("in_de_weg", []).append((x, y, z))
    ms_npc = {"id": "guhs:guh_npc", "Kind": "ballonguh", "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(180.0, 0.0)}
    s.entity(NPC[0] + 0.5, float(NPC[1]), NPC[2] + 0.5, ms_npc)
    # 2.10.1: a Reisguh a few steps from Kapitein Wolkje: the festival's travel waypoint
    from features import reisguh_plek
    info["reisguh"] = reisguh_plek.zet(s, reisguh_plek.rondom(NPC[0] + 3, NPC[1], NPC[2]), "Ballonfestival", 180.0, ms.Byte, ms.floats,
                                       "(Ballonfestival)")
    return s, info


def richting(dx, dz):
    """The main direction of (dx, dz)."""
    return ("east" if dx > 0 else "west") if abs(dx) >= abs(dz) else ("south" if dz > 0 else "north")


def stripe(x, z, cx, cz, n=16):
    """Pink and white stripes round a centre (for the tent)."""
    return "minecraft:pink_wool" if int((math.degrees(math.atan2(z - cz, x - cx)) % 360) / (360 / n)) % 2 else "minecraft:white_wool"


def tribune(s, info):
    """De Wolkjestribune (south-east): five rows of guh benches looking north at the balloons, a knuffelsteen back wall
    with a big guh face to the outside, guh-faced side walls, a striped pluisdak awning with bunting and mini balloons."""
    x0, x1 = 53, 68
    zf, zb = 58, 68                        # front row, back wall (two thick)
    for k in range(5):
        for x in range(x0, x1 + 1):
            for z in range(zf + 2 * k, zb):
                s.set(x, G + 1 + k, z, KS)
            s.set(x, G + 1 + k, zf + 2 * k, "guhs:knuffelsteen_trap", {"facing": "south", "half": "bottom", "shape": "straight", "waterlogged": "false"})
        for x in range(x0 + 1, x1, 2):
            s.set(x, G + 2 + k, zf + 2 * k + 1, "guhs:guh_bank", {"facing": "north"})
    for x in range(x0 + 2, x1 - 1, 2):     # guh faces along the front of the lowest row
        s.set(x, G + 1, zf - 1, GEZICHT, {"facing": "north", "stemming": str(x % 4)})
        info["gezichten"] += 1
    for x in range(x0 - 1, x1 + 2):
        for y in range(G + 1, G + 13):
            for z in (zb, zb + 1):
                s.set(x, y, z, KS)
    for z in range(zf - 1, zb):
        for y in range(G + 1, min(G + 6 + max(0, (z - zf) // 2), G + 12) + 1):
            s.set(x0 - 1, y, z, KS)
            s.set(x1 + 1, y, z, KS)
    hulp.wall_face(s, (x0 + x1) // 2, G + 6, zb + 2, 4.2, "south")
    hulp.wall_face(s, zf + 5, G + 5, x1 + 2, 2.6, "east")
    hulp.wall_face(s, zf + 5, G + 5, x0 - 2, 2.6, "west")
    info["gezichten"] += 3
    # the striped awning over the rows, on the back wall and two front posts
    for (x, z) in ((x0 - 1, zf - 1), (x1 + 1, zf - 1)):
        for y in range(G + 1, G + 13):
            s.set(x, y, z, KS)
    for x in range(x0 - 1, x1 + 2):
        for z in range(zf - 1, zb + 2):
            s.set(x, G + 13, z, "guhs:pluisdak" if (x // 2) % 2 else "minecraft:white_wool")
        s.set(x, G + 13, zf - 2, "guhs:pluisdak_plaat", {"type": "bottom", "waterlogged": "false"})
        if x0 - 1 < x < x1 + 1:
            s.set(x, G + 12, zf - 1, "guhs:vlaggetjes", {"axis": "x"})
    for x in range(x0 + 1, x1, 4):
        s.set(x, G + 14, zb, MINI)
    s.set(x0 - 1, G + 14, zf - 1, MINI)
    s.set(x1 + 1, G + 14, zf - 1, MINI)
    for x in range(x0 + 1, x1, 3):         # knuffelsteen faces along the top of the back wall, looking out
        s.set(x, G + 12, zb + 1, GEZICHT, {"facing": "south", "stemming": str(x % 4)})
        info["gezichten"] += 1


def tent(s, info):
    """Het Ballonnententje (south-west): a big round striped tent with a pointy roof, a guh-faced portal towards the
    steiger, guh faces round its wall, benches round a little stage inside."""
    cx, cz, r = 9, 62, 7.5
    for x in range(int(cx - r - 2), int(cx + r + 3)):
        for z in range(int(cz - r - 2), int(cz + r + 3)):
            d = math.hypot(x - cx, z - cz)
            if d <= r + 0.5:
                s.set(x, G, z, "minecraft:spruce_planks")
            if r - 0.5 < d <= r + 0.5:
                for y in range(G + 1, G + 6):
                    s.set(x, y, z, stripe(x, z, cx, cz))
            for y in range(G + 6, G + 16):          # the cone
                rr = (r + 1) * (1 - (y - (G + 6)) / 10.0)
                if rr - 1.6 < d <= rr:
                    s.set(x, y, z, stripe(x, z, cx, cz))
    for y in range(G + 1, G + 19):                 # the middle pole
        s.set(cx, y, cz, "minecraft:stripped_spruce_log", {"axis": "y"})
    s.set(cx, G + 19, cz, MINI)
    for (dx, dz) in ((3, 3), (-3, 3), (3, -3), (-3, -3)):
        for y in range(G + 1, G + 12):
            if s.get(cx + dx, y, cz + dz) is None:
                fence(s, cx + dx, y, cz + dz)
    # the portal (east, towards the steiger): knuffelsteen with a door and a guh face above it
    px = int(cx + r) + 1
    for z in range(cz - 3, cz + 4):
        for y in range(G + 1, G + 12):
            s.set(px, y, z, KS)
    for z in range(cz - 1, cz + 2):
        for y in range(G + 1, G + 4):
            for x in range(px - 2, px + 1):
                s.set(x, y, z, AIR)
        for x in range(px - 2, px + 1):
            s.set(x, G, z, "minecraft:spruce_planks")
    hulp.wall_face(s, cz, G + 8, px + 1, 3.2, "east")
    info["gezichten"] += 1
    s.set(px, G + 12, cz - 3, MINI)
    s.set(px, G + 12, cz + 3, MINI)
    for z in (cz - 2, cz + 2):
        s.set(px + 1, G + 1, z, GEZICHT, {"facing": "east", "stemming": "0" if z < cz else "3"})
        info["gezichten"] += 1
    # guh faces in the wall (looking out)
    for k in range(16):
        a = k * math.pi / 8
        x, z = round(cx + r * math.cos(a)), round(cz + r * math.sin(a))
        if x >= px - 1 or s.get(x, G + 3, z) not in ("minecraft:pink_wool", "minecraft:white_wool") or k % 2:
            continue
        s.set(x, G + 3, z, GEZICHT, {"facing": richting(x - cx, z - cz), "stemming": str(k % 4)})
        info["gezichten"] += 1
    # inside: benches round a little stage
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            if (x, z) != (cx, cz):
                s.set(x, G + 1, z, "minecraft:pink_carpet")
    for k in range(8):
        a = k * math.pi / 4
        x, z = round(cx + 5 * math.cos(a)), round(cz + 5 * math.sin(a))
        if x > cx + 3 and abs(z - cz) <= 1:
            continue
        if s.get(x, G + 1, z) is None:
            s.set(x, G + 1, z, "guhs:guh_bank", {"facing": hulp.OPP[richting(x - cx, z - cz)]})


def uitkijkguh(s, info):
    """De Uitkijkguh (north-west): a round knuffelsteen lookout tower with windows and a gallery, and on top a giant guh
    head of pink wool with a guh face on every side, the ears up and a mini balloon."""
    cx, cz, r = 9, 9, 3.6
    top = G + 12
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 6, cz + 7):
            d = math.hypot(x - cx, z - cz)
            if d <= r + 0.5:
                s.set(x, G, z, KLINK)
                for y in range(G + 1, top):
                    if d > r - 0.6:
                        window = y in (G + 5, G + 6, G + 9, G + 10) and (x == cx or z == cz)
                        s.set(x, y, z, "minecraft:pink_stained_glass" if window else KS)
                s.set(x, top, z, KS)
            elif d <= r + 1.6:                     # the gallery round the top
                s.set(x, top, z, "guhs:pluisdak_plaat", {"type": "top", "waterlogged": "false"})
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):   # knuffelsteen faces low on the tower, looking out
        s.set(cx + 4 * dx, G + 3, cz + 4 * dz, GEZICHT, {"facing": richting(dx, dz), "stemming": str((dx + 2 * dz) % 4)})
        info["gezichten"] += 1
    for y in range(G + 1, G + 3):                  # a door on the east side
        for x in (cx + 3, cx + 4):
            s.set(x, y, cz + 1, AIR)
    h0 = top + 1                                   # the head: 11 x 9 x 11, rounded
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            for y in range(h0, h0 + 9):
                e = max(abs(x - cx) - 3, 0) ** 2 + max(abs(z - cz) - 3, 0) ** 2 + max(abs(y - (h0 + 4)) - 2, 0) ** 2
                if e <= 5:
                    s.set(x, y, z, "minecraft:pink_wool")
    for f, cu, at in (("south", cx, cz + 6), ("north", cx, cz - 6), ("east", cz, cx + 6), ("west", cz, cx - 6)):
        hulp.wall_face(s, cu, h0 + 3, at, 3.2, f)
        info["gezichten"] += 1
    # (the faces stand one block in front of the head: fill behind them so they hang on)
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 6, cz + 7):
            for y in range(h0 - 2, h0 + 12):
                if s.get(x, y, z) in (None, AIR):
                    continue
                ix = x - (1 if x > cx + 5 else -1 if x < cx - 5 else 0)
                iz = z - (1 if z > cz + 5 else -1 if z < cz - 5 else 0)
                if (ix, iz) != (x, z) and s.get(ix, y, iz) in (None, AIR):
                    s.set(ix, y, iz, "minecraft:pink_wool")
    s.set(cx, h0 + 9, cz, "minecraft:pink_wool")
    s.set(cx, h0 + 10, cz, MINI)


def poort(s, info, kant):
    """A guh-face gate over the west or the north path, at the field's edge (a face on both sides)."""
    a0, a1 = (2, 3) if kant == "west" else (1, 2)

    def put(u, y, v, name, props=None):
        # u: along the gate, v: through it
        if kant == "west":
            s.set(v, y, u, name, props)
        else:
            s.set(u, y, v, name, props)

    def got(u, y, v):
        return s.get(v, y, u) if kant == "west" else s.get(u, y, v)

    for u in range(C - 4, C + 5):
        for y in range(G + 1, G + 9):
            if abs(u - C) >= 3 or y >= G + 6:
                for v in (a0, a1):
                    put(u, y, v, KS)
    buiten_, binnen_ = ("west", "east") if kant == "west" else ("north", "south")
    hulp.wall_face(s, C, G + 12, a0 - 1, 3.2, buiten_)
    hulp.wall_face(s, C, G + 12, a1 + 1, 3.2, binnen_)
    for v in (a0, a1):
        for u in range(C - 5, C + 6):
            for y in range(G + 7, G + 19):
                if got(u, y, v) is None and (got(u, y, a0 - 1) is not None or got(u, y, a1 + 1) is not None):
                    put(u, y, v, "minecraft:pink_wool")
    for u in (C - 4, C + 4):                       # knuffelsteen faces at the feet of the gate, both sides
        put(u, G + 1, a0 - 1, GEZICHT, {"facing": buiten_, "stemming": "0"})
        put(u, G + 1, a1 + 1, GEZICHT, {"facing": binnen_, "stemming": "3"})
    info["gezichten"] += 2 + 4


def check(s, info):
    problems = []
    loose = hulp.check_floating(s, 0)
    if loose:
        problems.append(f"{len(loose)} floating blocks, e.g. {[(p, s.get(*p)) for p in loose[:6]]}")
    if info.get("in_de_weg"):
        problems.append(f"blocks in the balloons' way: {info['in_de_weg'][:6]}")
    reach = hulp.walk(s, [(C, G + 1, D - 1), (C, G + 1, 0), (0, G + 1, C), (W - 1, G + 1, C)], extra_passable=("minecraft:jigsaw",))
    if len(reach) < 2000:
        problems.append(f"only {len(reach)} walkable spots")
    if not hulp.near_reachable(reach, *NPC, r=1):
        problems.append("Kapitein Wolkje can't be walked to")
    if not hulp.near_reachable(reach, BALLON[0], G + 2, BALLON[2] + 2, r=1):
        problems.append("the steiger can't be walked onto")
    x, y, z = NPC
    if hulp.passable(s.get(x, y - 1, z)) or not hulp.passable(s.get(x, y, z)) or not hulp.passable(s.get(x, y + 1, z)):
        problems.append("the Kapitein doesn't stand on a floor with room above")
    if math.dist((NPC[0], NPC[2]), (BALLON[0], BALLON[2])) > 18:
        problems.append("the Kapitein is too far from his balloon")
    for (dx_, dz_), _, _ in DECO:
        if math.dist((NPC[0], NPC[2]), (dx_, dz_)) <= 20:
            problems.append(f"a decoration balloon at {(dx_, dz_)} is too close to the Kapitein")
    faces = sum(1 for (b, _, _) in s.blocks.values() if b == GEZICHT)
    if faces < 30:
        problems.append(f"too few knuffelsteen_gezicht blocks: {faces}")
    if info["gezichten"] < 8:
        problems.append(f"too few guh faces: {info['gezichten']}")
    if problems:
        raise SystemExit("ballonfestival geometry check failed:\n  " + "\n  ".join(problems))
    return len(reach)


if __name__ == "__main__":
    import types
    import make_structures as ms_mod
    h = types.SimpleNamespace(Structure=ms_mod.Structure, ms=ms_mod)
    s, info = build(h)
    n = check(s, info)
    print(f"ballonfestival ok: {len(s.blocks)} blocks, {n} walkable spots, {info.get('gezichten')} faces")
