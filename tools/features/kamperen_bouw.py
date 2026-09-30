"""
Een kampeerplekje (2.8, slice "buiten") - the campsite (template kampeerplekje, 25 x 16 x 25) in the Knuffeldal and on
the Guhweides: a campfire in a ring of stones, three log benches, Opa Guh's spot by the fire, three guh-shaped tents
(a guh face with little ears on the front, two sleeping bags inside), more sleeping bags by the fire, lantern posts,
a woodpile, a picnic table, flowers, a path in, and a few guhs camping.
check(s): nothing floats, Opa Guh and every sleeping bag can be walked to from the path, the campfire burns.
Run it alone:  python tools/features/kamperen_bouw.py   (from the project root)
"""
import math
import os
import random
import sys

if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

from features import sterrenwacht_hulp as hulp  # noqa: E402

NAME = "kampeerplekje"
W, H, D = 25, 16, 25
G = 4
C = 12
ANCHOR = "guhs:kampeerplekje_midden"
KLINK = "guhs:knuffelklinkers"
SLAAPZAK = "guhs:guh_slaapzak"
OPA = (C, G + 1, C + 3)
TENTEN = [((C, 4), "south", "white"), ((4, C), "east", "lime"), ((20, C), "west", "light_blue")]
BLOEMEN = ["guhs:roze_gras", "guhs:guhoortjes", "guhs:kaasbloem", "guhs:roze_guhbloem"]


def fence(s, x, y, z):
    s.set(x, y, z, "minecraft:spruce_fence", {"north": "false", "east": "false", "south": "false", "west": "false", "waterlogged": "false"})


def tent(s, cx, cz, facing, kleur, info):
    """A guh tent: an A-frame 7 wide and 5 long; the front (towards `facing`) has a guh face and two little ears, the
    back an opening; two sleeping bags inside."""
    wol = f"minecraft:{kleur}_wool"
    fx, fz = hulp.STEP[facing]
    sx, sz = -fz, fx                     # sideways
    for along in range(-2, 3):
        for dy in range(1, 5):
            half = 4 - dy
            for side in range(-half, half + 1):
                x, z = cx + fx * along + sx * side, cz + fz * along + sz * side
                shell = abs(side) == half
                front = along == 2
                back = along == -2
                if shell or front or back:
                    blk = wol
                    if back and dy <= 2 and abs(side) <= 1 and not shell:
                        blk = hulp.AIR                  # the way in (at the back)
                    s.set(x, G + dy, z, blk)
                else:
                    s.set(x, G + dy, z, hulp.AIR)
    # the face on the front: eyes, blush, a little snoet; two ears at the top
    front = lambda side, dy: (cx + fx * 2 + sx * side, G + dy, cz + fz * 2 + sz * side)
    s.set(*front(-1, 2), "minecraft:black_wool")
    s.set(*front(1, 2), "minecraft:black_wool")
    s.set(*front(-2, 1), "minecraft:pink_concrete")
    s.set(*front(2, 1), "minecraft:pink_concrete")
    s.set(*front(0, 1), "minecraft:magenta_wool")
    s.set(*front(-1, 4), "minecraft:magenta_wool")
    s.set(*front(1, 4), "minecraft:magenta_wool")
    info["gezichten"] += 1
    # a floor and two sleeping bags inside, heads to the front
    zakken = []
    for along in range(-2, 3):
        for side in range(-3, 4):
            s.set(cx + fx * along + sx * side, G, cz + fz * along + sz * side, "minecraft:spruce_planks")
    for side in (-1, 1):
        x, z = cx + fx * 1 + sx * side, cz + fz * 1 + sz * side
        s.set(x, G + 1, z, SLAAPZAK, {"facing": facing, "occupied": "false"})
        zakken.append((x, G + 1, z))
    s.set(cx + fx * 0, G + 1, cz + fz * 0, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    return zakken


def build(h):
    s = h.Structure((W, H, D))
    rng = random.Random(28801)
    info = {"gezichten": 0, "slaapzakken": []}
    for x in range(W):
        for z in range(D):
            for y in range(G):
                s.set(x, y, z, "minecraft:pink_wool")
            s.set(x, G, z, "guhs:knuffelgras")
    # the path in (south)
    for z in range(C + 5, D):
        for x in (C - 1, C, C + 1):
            s.set(x, G, z, KLINK)
    # the campfire and its ring of stones
    s.set(C, G + 1, C, "minecraft:campfire", {"lit": "true", "signal_fire": "false", "facing": "north", "waterlogged": "false"})
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx or dz:
                s.set(C + dx, G, C + dz, "guhs:knuffelsteen")
                s.set(C + dx, G + 1, C + dz, "guhs:knuffelsteen_plaat", {"type": "bottom", "waterlogged": "false"})
    # log benches (north, west, east), Opa's spot (south)
    for d in range(-1, 2):
        s.set(C + d, G + 1, C - 3, "minecraft:stripped_oak_log", {"axis": "x"})
        s.set(C - 3, G + 1, C + d, "minecraft:stripped_oak_log", {"axis": "z"})
        s.set(C + 3, G + 1, C + d, "minecraft:stripped_oak_log", {"axis": "z"})
    s.set(C + 1, G + 1, C + 3, "guhs:guh_tafel", {"facing": "north"})
    s.set(C + 1, G + 2, C + 3, "guhs:lampion_geel", {"hanging": "false"})
    s.set(C - 1, G + 1, C + 3, "minecraft:barrel", {"facing": "up", "open": "false"},
          {"id": "minecraft:barrel", "LootTable": "guhs:chests/kampeerplekje"})
    # sleeping bags by the fire
    for (x, z, f) in ((C - 3, C + 3, "north"), (C + 3, C + 3, "north")):
        s.set(x, G + 1, z, SLAAPZAK, {"facing": f, "occupied": "false"})
        info["slaapzakken"].append((x, G + 1, z))
    # the tents
    for (tx, tz), f, kleur in TENTEN:
        info["slaapzakken"] += tent(s, tx, tz, f, kleur, info)
    # lantern posts in the corners, a woodpile, a picnic table, a sign with a guh face
    for (x, z) in ((2, 2), (22, 2), (2, 22), (22, 22)):
        for y in (G + 1, G + 2):
            fence(s, x, y, z)
        s.set(x, G + 3, z, "guhs:lampion_roze" if (x + z) % 4 else "guhs:lampion_mint", {"hanging": "false"})
    for (x, z) in ((19, 19), (20, 19), (19, 20), (20, 20)):
        s.set(x, G + 1, z, "minecraft:oak_log", {"axis": "x"})
    s.set(19, G + 2, 19, "minecraft:oak_log", {"axis": "x"})
    s.set(20, G + 2, 19, "minecraft:oak_log", {"axis": "x"})
    s.set(5, G + 1, 19, "guhs:guh_tafel", {"facing": "north"})
    s.set(4, G + 1, 19, "guhs:guh_stoel", {"facing": "east"})
    s.set(6, G + 1, 19, "guhs:guh_stoel", {"facing": "west"})
    s.set(5, G + 2, 19, "guhs:block_of_kaasknabbels")
    s.set(C + 3, G + 1, D - 2, "guhs:knuffelsteen_muur", {"up": "true", "north": "none", "east": "none", "south": "none", "west": "none",
                                                          "waterlogged": "false"})
    s.set(C + 3, G + 2, D - 2, "guhs:knuffelsteen_gezicht", {"facing": "south", "stemming": "1"})
    info["gezichten"] += 1
    for x in range(W):
        for z in range(D):
            if s.get(x, G + 1, z) is None and s.get(x, G, z) == "guhs:knuffelgras" and rng.random() < 0.12 \
                    and math.dist((x, z), (C, C)) > 5 and not (C - 2 <= x <= C + 2 and z > C + 3):
                s.set(x, G + 1, z, BLOEMEN[rng.randrange(len(BLOEMEN))])
    # the anchor (under the campfire's front stone)
    s.set(C, G, C + 1, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": "guhs:knuffelsteen",
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    # Opa Guh and a few guhs camping
    ms = h.ms
    s.entity(OPA[0] + 0.5, float(OPA[1]), OPA[2] + 0.5, {"id": "guhs:guh_npc", "Kind": "opa_guh", "PersistenceRequired": ms.Byte(1),
                                                         "Rotation": ms.floats(180.0, 0.0)})
    for (x, z, sc, var) in ((C - 2, C - 4, 1.0, "normal"), (C + 5, C - 1, 0.9, "normal"), (C - 5, C + 1, 0.7, "normal")):
        s.entity(x + 0.5, float(G + 1), z + 0.5, ms.guh_nbt(sc, Variant=var, Rotation=ms.floats(rng.uniform(0, 360), 0.0)))
    return s, info


def check(s, info):
    problems = []
    loose = hulp.check_floating(s, 0)
    if loose:
        problems.append(f"{len(loose)} floating blocks, e.g. {[(p, s.get(*p)) for p in loose[:6]]}")
    reach = hulp.walk(s, [(C, G + 1, D - 1)], extra_passable=("minecraft:jigsaw",))
    if len(reach) < 200:
        problems.append(f"only {len(reach)} walkable spots")
    if not hulp.near_reachable(reach, *OPA, r=1):
        problems.append("Opa Guh can't be walked to")
    x, y, z = OPA
    if hulp.passable(s.get(x, y - 1, z)) or not hulp.passable(s.get(x, y, z)) or not hulp.passable(s.get(x, y + 1, z)):
        problems.append("Opa Guh doesn't stand on a floor with room above")
    for p in info["slaapzakken"]:
        if not hulp.near_reachable(reach, *p, r=1):
            problems.append(f"the sleeping bag at {p} can't be reached")
        f = s.blocks[p][1]["facing"]
        bx, bz = hulp.STEP[hulp.OPP[f]]
        if s.get(p[0], p[1] + 1, p[2]) not in (None, hulp.AIR) or s.get(p[0] + bx, p[1] + 1, p[2] + bz) not in (None, hulp.AIR):
            problems.append(f"no room to sleep at {p} (like a bed: above it and above its foot end)")
    if s.get(C, G + 1, C) != "minecraft:campfire":
        problems.append("no campfire")
    if math.dist((OPA[0], OPA[2]), (C, C)) > 6:
        problems.append("Opa sits too far from the fire")
    if problems:
        raise SystemExit("kampeerplekje geometry check failed:\n  " + "\n  ".join(problems))
    return len(reach)


if __name__ == "__main__":
    import types
    import make_structures as ms_mod
    h = types.SimpleNamespace(Structure=ms_mod.Structure, ms=ms_mod)
    s, info = build(h)
    n = check(s, info)
    print(f"kampeerplekje ok: {len(s.blocks)} blocks, {n} walkable spots, {info['gezichten']} faces, {len(info['slaapzakken'])} sleeping bags")
