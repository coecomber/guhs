"""
Piep (2.8.1) - the kaasknabbel-nest (template kaasknabbel_nest, 37 x 22 x 37): a big kaaskorst hill full of holes like a
gatenkaas, with guh faces looking out on every side, four tunnels into a round arena in the middle, six little holes in
the arena wall (where the boze kaasknabbels come from) and the golden kern in the middle (step on it: the fight starts,
KaasknabbelNest.java). Cheese and guh blocks everywhere: kaaskorst, gatenkaas, kaasaders, knabbel blocks, guh plushies,
kaasbloemen and roze guhbloemen round the foot.

The numbers the game uses (keep them in sync with KaasknabbelNest.java): C (the centre), G (the floor), the six spawn holes
at SPAWN_R blocks on the angles SPAWN_HOEKEN, and the kern radius.
Run it alone:  python tools/features/piep_nest.py   (from the project root)
"""
import math
import os
import random
import sys

if __name__ == "__main__":
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

NAME = "kaasknabbel_nest"
W, H, D = 37, 22, 37
C = 18                      # the centre (x and z)
G = 4                       # the arena floor (template y); the template starts G blocks under the ground
R_HEUVEL, H_HEUVEL = 17.0, 14.0
R_ARENA, H_ARENA = 9.5, 7.5
TUNNEL_HOEKEN = (45, 135, 225, 315)
SPAWN_HOEKEN = (0, 60, 120, 180, 240, 300)
SPAWN_R = 9
ANCHOR = "guhs:kaasknabbel_nest_midden"
AIR = "minecraft:air"

KORST = "guhs:kaaskorst"
KORST_STENEN = "guhs:kaaskorst_stenen"
SHELL_MIX = [("guhs:kaaskorst", 50), ("guhs:gatenkaas", 18), ("guhs:kaaskorst_stenen", 8), ("guhs:gebarsten_kaaskorst_stenen", 5),
             ("guhs:aangevreten_kaaskorst_stenen", 5), ("guhs:kaasader", 6), ("guhs:belegen_kaas_stenen", 5), ("guhs:block_of_kaasknabbels", 3)]
PLUCHE = ["guhs:knuffel_normal", "guhs:knuffel_mint", "guhs:knuffel_choco", "guhs:knuffel_golden", "guhs:knuffel_pluisguh", "guhs:knuffel_snow"]
BLOEMEN = ["guhs:kaasbloem", "guhs:roze_guhbloem", "guhs:guhoortjes", "guhs:roze_gras"]
FACING_OUT = {0: "east", 90: "south", 180: "west", 270: "north"}


def pick(rng, mix):
    total = sum(w for _, w in mix)
    r = rng.uniform(0, total)
    for name, w in mix:
        r -= w
        if r <= 0:
            return name
    return mix[0][0]


def in_heuvel(dx, dy, dz, bump):
    return (dx / (R_HEUVEL + bump)) ** 2 + (dz / (R_HEUVEL + bump)) ** 2 + (dy / (H_HEUVEL + bump)) ** 2 <= 1.0


def in_arena(dx, dy, dz):
    return dy >= 1 and (dx / R_ARENA) ** 2 + (dz / R_ARENA) ** 2 + ((dy - 0.5) / H_ARENA) ** 2 <= 1.0


def spawn_plekken():
    """The six holes in the arena wall (template x, y, z of the spot a knabbel stands on)."""
    out = []
    for a in SPAWN_HOEKEN:
        r = math.radians(a)
        out.append((C + round(math.cos(r) * SPAWN_R), G + 1, C + round(math.sin(r) * SPAWN_R)))
    return out


def build(h):
    rng = random.Random(2028101)
    s = h.Structure((W, H, D))
    info = {}

    # a lumpy height bump per column so the hill isn't a perfect dome
    bumps = {}
    for x in range(W):
        for z in range(D):
            bumps[(x, z)] = math.sin(x * 0.7) * 0.6 + math.cos(z * 0.55) * 0.6 + rng.uniform(-0.3, 0.3)

    # --- the foundation (under the ground) and the hill ---------------------------------------------------------------------
    for x in range(W):
        for z in range(D):
            dx, dz = x - C, z - C
            r = math.hypot(dx, dz)
            if r > R_HEUVEL + 1.2:
                continue
            for y in range(0, G):
                s.set(x, y, z, KORST if y < G - 1 else KORST_STENEN)
            for y in range(G, H):
                dy = y - G
                if in_heuvel(dx, dy, dz, bumps[(x, z)]):
                    s.set(x, y, z, pick(rng, SHELL_MIX))

    # --- the arena inside ---------------------------------------------------------------------------------------------------
    for x in range(W):
        for z in range(D):
            dx, dz = x - C, z - C
            for y in range(G, H):
                if in_arena(dx, y - G, dz):
                    s.set(x, y, z, AIR)
            if (dx / R_ARENA) ** 2 + (dz / R_ARENA) ** 2 <= 1.0:
                ring = math.hypot(dx, dz)
                s.set(x, G, z, "guhs:belegen_kaas_tegels" if 4.5 < ring < 5.5 else KORST_STENEN)

    # --- the four tunnels in (arched, 3 wide) ---------------------------------------------------------------------------------
    for a in TUNNEL_HOEKEN:
        ang = math.radians(a)
        ux, uz = math.cos(ang), math.sin(ang)
        for t10 in range(int(R_ARENA * 10) - 10, int((R_HEUVEL + 2) * 10)):
            t = t10 / 10
            cx, cz = C + ux * t, C + uz * t
            for ox in range(-2, 3):
                for oz in range(-2, 3):
                    x, z = int(round(cx + ox)), int(round(cz + oz))
                    side = abs((x - cx) * -uz + (z - cz) * ux)
                    if side > 1.6:
                        continue
                    for y in range(G + 1, G + 5):
                        top = G + 3 + (1 if side < 0.8 else 0)
                        if y <= top:
                            s.set(x, y, z, AIR)
                    if s.get(x, G, z) is not None:
                        s.set(x, G, z, KORST_STENEN)
    info["tunnels"] = TUNNEL_HOEKEN

    # --- swiss-cheese holes in the outside of the hill ------------------------------------------------------------------------
    for i in range(22):
        a = rng.uniform(0, math.tau)
        el = rng.uniform(0.15, 1.2)                    # elevation angle (radians): mostly the upper half
        r = R_HEUVEL * math.cos(el) * 0.97
        hx, hz = C + math.cos(a) * r, C + math.sin(a) * r
        hy = G + H_HEUVEL * math.sin(el) * 0.95
        rad = rng.uniform(1.3, 2.6)
        for x in range(int(hx - rad - 1), int(hx + rad + 2)):
            for y in range(int(hy - rad - 1), int(hy + rad + 2)):
                for z in range(int(hz - rad - 1), int(hz + rad + 2)):
                    if y > G and math.dist((x, y, z), (hx, hy, hz)) <= rad and s.get(x, y, z) is not None:
                        s.set(x, y, z, AIR)
        # a darker rim round the hole
        for k in range(10):
            b = k / 10 * math.tau
            x, y, z = hx + math.cos(b) * (rad + 0.6), hy + math.sin(b) * (rad + 0.6), hz
            if s.get(int(round(x)), int(round(y)), int(round(z))) not in (None, AIR):
                s.set(x, y, z, "guhs:gatenkaas")

    # --- the six knabbel holes in the arena wall ----------------------------------------------------------------------------
    for a in SPAWN_HOEKEN:
        ang = math.radians(a)
        for t10 in range(int(R_ARENA * 10) - 15, int(R_ARENA * 10) + 25):
            t = t10 / 10
            cx, cz = C + math.cos(ang) * t, C + math.sin(ang) * t
            for x in range(int(cx) - 1, int(cx) + 2):
                for z in range(int(cz) - 1, int(cz) + 2):
                    for y in range(G + 1, G + 3):
                        if math.hypot(x + 0.5 - cx - 0.5, z + 0.5 - cz - 0.5) < 1.3:
                            s.set(x, y, z, AIR)
                    s.set(x, G, z, "guhs:gatenkaas_stenen")
        # a little crown of knabbel blocks over each hole
        ex, ez = C + round(math.cos(ang) * (R_ARENA + 0.5)), C + round(math.sin(ang) * (R_ARENA + 0.5))
        if s.get(ex, G + 3, ez) not in (None, AIR):
            s.set(ex, G + 3, ez, "guhs:block_of_kaasknabbels")
    info["spawns"] = spawn_plekken()
    for (x, y, z) in info["spawns"]:
        s.set(x, y, z, AIR)
        s.set(x, y + 1, z, AIR)
        s.set(x, y - 1, z, "guhs:gatenkaas_stenen")

    # --- the kern in the middle ---------------------------------------------------------------------------------------------
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            s.set(C + dx, G, C + dz, "guhs:gouden_kaasader")
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):
        s.set(C + dx, G, C + dz, "guhs:block_of_kaasknabbels")
    for dx, dz in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        s.set(C + dx, G, C + dz, "guhs:guh_kristal_lamp")
    # the anchor sits under the kern (its final state: gold)
    s.set(C, G - 1, C, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": ANCHOR, "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": "guhs:gouden_kaasader", "joint": "rollable", "placement_priority": 0, "selection_priority": 0})

    # --- light and cheese drips in the arena --------------------------------------------------------------------------------
    for k in range(8):
        a = math.radians(k * 45 + 22.5)
        x, z = C + round(math.cos(a) * (R_ARENA - 0.2)), C + round(math.sin(a) * (R_ARENA - 0.2))
        for y in range(G + 3, G + 5):
            if s.get(x, y, z) not in (None, AIR):
                s.set(x, y, z, "guh_kristal_lamp" if False else "guhs:guh_kristal_lamp")
                break
    for i in range(14):
        a, r = rng.uniform(0, math.tau), rng.uniform(2.5, R_ARENA - 2.5)
        x, z = C + round(math.cos(a) * r), C + round(math.sin(a) * r)
        for y in range(H - 1, G + 3, -1):
            if s.get(x, y, z) == AIR and s.get(x, y + 1, z) not in (None, AIR):
                s.set(x, y, z, "guhs:kaas_stalactiet", {"thickness": "tip", "vertical_direction": "down"})
                break

    # --- guh faces looking out on every side ----------------------------------------------------------------------------------
    for a, facing in FACING_OUT.items():
        for yoff, stemming in ((6, 0), (2, 1)):
            ang = math.radians(a + (20 if yoff == 2 else 0))
            ux, uz = math.cos(ang), math.sin(ang)
            y = G + yoff
            for t10 in range(int((R_HEUVEL + 2) * 10), 0, -2):
                x, z = C + round(ux * t10 / 10), C + round(uz * t10 / 10)
                if s.get(x, y, z) not in (None, AIR):
                    f = FACING_OUT[min(FACING_OUT, key=lambda k: min(abs(k - (a + (20 if yoff == 2 else 0)) % 360), 360 - abs(k - (a + (20 if yoff == 2 else 0)) % 360)))]
                    s.set(x, y, z, "guhs:knuffelsteen_gezicht", {"facing": f, "stemming": str(stemming)})
                    break
    # a big guh face at the very top
    for y in range(H - 1, G, -1):
        if s.get(C, y, C) not in (None, AIR):
            s.set(C, y + 1, C, "guhs:knuffel_golden", {"facing": "south"})
            break

    # --- round the foot: flowers, plushies by the tunnels, knabbel piles ------------------------------------------------------
    for x in range(W):
        for z in range(D):
            r = math.hypot(x - C, z - C)
            if R_HEUVEL - 1.5 < r < R_HEUVEL + 1.2 and s.get(x, G, z) is None and rng.random() < 0.35:
                s.set(x, G - 1, z, "guhs:kaasmos")
                s.set(x, G, z, rng.choice(BLOEMEN))
    for i, a in enumerate(TUNNEL_HOEKEN):
        ang = math.radians(a)
        for side in (-2, 2):
            x = C + round(math.cos(ang) * (R_HEUVEL + 0.5) - math.sin(ang) * side)
            z = C + round(math.sin(ang) * (R_HEUVEL + 0.5) + math.cos(ang) * side)
            s.set(x, G - 1, z, KORST_STENEN)
            f = FACING_OUT[min(FACING_OUT, key=lambda k: min(abs(k - a) % 360, 360 - abs(k - a) % 360))]
            s.set(x, G, z, PLUCHE[(i * 2 + (side > 0)) % len(PLUCHE)], {"facing": f})
    # the path out of each tunnel
    for a in TUNNEL_HOEKEN:
        ang = math.radians(a)
        for t in range(int(R_HEUVEL) - 1, int(R_HEUVEL) + 2):
            for w in (-1, 0, 1):
                x = C + round(math.cos(ang) * t - math.sin(ang) * w)
                z = C + round(math.sin(ang) * t + math.cos(ang) * w)
                if 0 <= x < W and 0 <= z < D:
                    s.set(x, G - 1, z, KORST_STENEN)
                    if s.get(x, G, z) not in (None, AIR) and "knuffel" not in (s.get(x, G, z) or ""):
                        s.set(x, G, z, AIR)

    # air above the whole footprint so hills nearby don't poke through
    footprint = [(x, z) for x in range(W) for z in range(D) if math.hypot(x - C, z - C) <= R_HEUVEL + 1.2]
    s.clear_above(footprint, G)
    info["kern"] = (C, G, C)
    return s, info


def check(s, info):
    """The kern and the spawn holes are open, the arena is reachable from every tunnel."""
    problems = []
    for (x, y, z) in info["spawns"]:
        if s.get(x, y, z) not in (None, AIR) or s.get(x, y + 1, z) not in (None, AIR):
            problems.append(f"nest: spawn hole {(x, y, z)} is not open")
        if s.get(x, y - 1, z) in (None, AIR):
            problems.append(f"nest: spawn hole {(x, y, z)} has no floor")
    for y in range(G + 1, G + 4):
        if s.get(C, y, C) not in (None, AIR):
            problems.append(f"nest: the kern is blocked at y {y}")
    for a in TUNNEL_HOEKEN:
        ang = math.radians(a)
        for t in range(int(R_ARENA), int(R_HEUVEL) - 1):
            x, z = C + round(math.cos(ang) * t), C + round(math.sin(ang) * t)
            if s.get(x, G + 1, z) not in (None, AIR) or s.get(x, G + 2, z) not in (None, AIR):
                problems.append(f"nest: tunnel {a} blocked at {(x, z)}")
                break
    return problems


if __name__ == "__main__":
    import types
    import make_structures as ms
    h = types.SimpleNamespace(Structure=ms.Structure)
    s, info = build(h)
    print(check(s, info), len(s.blocks))
