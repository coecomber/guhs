"""
Op Safari! zone module: the Bamboebergen landmarks (design 1.4 zone table).

  pagode-uitkijktoren  a three-tier red pagoda on the hilltop with flared roofs, hanging lanterns, a ladder up to an
                       open viewing floor, and a stone trail up from the truck route; the geheimpje (an old teapot,
                       a decorated pot) stands behind it
  hangbrug over kloof  the river gets steep rock walls (a gorge) and a sagging rope bridge from rim to rim, with cherry
                       trees on both rims (the rode panda's and the gibbons live there)
  theeterrassen        the slope stepped into terraces with stone-brick faces and rows of (flowering) azalea "tea"
  bamboedoolhof        a 21x21 maze with walls of tall bamboo, two entrances and a clearing in the middle (the
                       reuzenpanda's)
  schuilhut            a hide near the river

(Mist is not a block: the bamboe biome's fog does that.) Helpers come from zone_regenwoud.
"""
import math

import numpy as np
from scipy.ndimage import distance_transform_edt, gaussian_filter

from . import kit
from .kit import state
from .terrein import ZONES
from .zone_regenwoud import (RNG, ruim, zet_hoogte, steun, zoek, stairs, slab, schuilhut, hangbrug, hek_ok)

WATER_Y = kit.WATER_Y
PLAATS = {}


def _trail(park, van, naar, blok="minecraft:mossy_stone_bricks", lantaarn=8):
    """A walkable trail from van to naar: one column wide, heights eased so each step is at most 1 block."""
    (x0, z0), (x1, z1) = van, naar
    n = int(math.hypot(x1 - x0, z1 - z0))
    pts = []
    for i in range(n + 1):
        x = int(round(x0 + (x1 - x0) * i / max(n, 1)))
        z = int(round(z0 + (z1 - z0) * i / max(n, 1)))
        if not pts or pts[-1] != (x, z):
            pts.append((x, z))
    hs = [int(park.hoogte[x, z]) for x, z in pts]
    # ease: walk forward and backward limiting the change to 1 per step, keep the ends
    for _ in range(3):
        for i in range(1, len(hs)):
            hs[i] = max(min(hs[i], hs[i - 1] + 1), hs[i - 1] - 1)
        for i in range(len(hs) - 2, -1, -1):
            hs[i] = max(min(hs[i], hs[i + 1] + 1), hs[i + 1] - 1)
    for i, ((x, z), h) in enumerate(zip(pts, hs)):
        if park.water[x, z] or park.pad[x, z] or park.hek_afstand[x, z] < 4:
            continue
        for dx, dz in ((0, 0), (1, 0), (0, 1)):
            xx, zz = x + dx, z + dz
            if park.pad[xx, zz] or park.water[xx, zz]:
                continue
            zet_hoogte(park, xx, zz, h, top=blok, sub="dirt")
        if lantaarn and i % lantaarn == lantaarn // 2:
            lx, lz = x - 1, z
            park.set(lx, h + 1, lz, "minecraft:spruce_fence")
            park.set(lx, h + 2, lz, state("lantern", hanging="false", waterlogged="false"))


def pagode(park, log):
    px, pz, _, _ = kit.RESERVERINGEN["pagode"]
    g = int(np.median(park.hoogte[px - 4:px + 5, pz - 4:pz + 5]))
    ruim(park, px, pz, 12, extra=3)
    for dx in range(-7, 8):
        for dz in range(-7, 8):
            if max(abs(dx), abs(dz)) <= 6:
                zet_hoogte(park, px + dx, pz + dz, g, top="minecraft:stone_bricks")
            elif park.hoogte[px + dx, pz + dz] < g - 1:
                zet_hoogte(park, px + dx, pz + dz, g - 1, top="minecraft:mossy_stone_bricks")
    pilaar = state("stripped_crimson_hyphae", axis="y")
    wand = state("spruce_planks")
    dak = "dark_oak"

    def ring_dak(r, y, buiten=True):
        for i in range(-r, r + 1):
            for (x, z, f) in ((px + i, pz - r, "south"), (px + i, pz + r, "north"), (px - r, pz + i, "east"),
                              (px + r, pz + i, "west")):
                hoek = abs(i) == r
                park.set(x, y, z, stairs(f"{dak}_stairs", f, shape="outer_left" if hoek else "straight"))
        for (sx, sz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):     # upturned eaves
            park.set(px + sx * r, y + 1, pz + sz * r, slab(f"{dak}_slab", "bottom"))
            if buiten:
                park.set(px + sx * r, y - 1, pz + sz * r, state("lantern", hanging="true", waterlogged="false"))

    def verdieping(r, y0, hoog, open_=False):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                rand = abs(dx) == r or abs(dz) == r
                if not rand:
                    continue
                pil = (abs(dx) == r and abs(dz) == r) or (abs(dx) == r and dz % 2 == 0) or (abs(dz) == r and dx % 2 == 0)
                for y in range(y0, y0 + hoog):
                    if pil:
                        park.set(px + dx, y, pz + dz, pilaar)
                    elif open_:
                        if y == y0:
                            park.set(px + dx, y, pz + dz, "minecraft:spruce_fence")
                    elif y == y0 + 1 and (dx == 0 or dz == 0):
                        park.set(px + dx, y, pz + dz, "minecraft:air")           # windows / doors
                    elif y == y0 and (dx == 0 or dz == 0) and r >= 4:
                        park.set(px + dx, y, pz + dz, "minecraft:air")           # doorways on the ground floor
                    else:
                        park.set(px + dx, y, pz + dz, wand)

    # ground floor r4 (9x9), roof rings at r6/r5
    park.fill(px - 4, g, pz - 4, px + 4, g, pz + 4, state("spruce_planks"))
    verdieping(4, g + 1, 4)
    ring_dak(6, g + 5)
    ring_dak(5, g + 6, buiten=False)
    park.fill(px - 4, g + 6, pz - 4, px + 4, g + 6, pz + 4, state("spruce_planks"))
    # second floor r3, roof r5/r4
    verdieping(3, g + 7, 3)
    ring_dak(5, g + 10)
    ring_dak(4, g + 11, buiten=False)
    park.fill(px - 3, g + 11, pz - 3, px + 3, g + 11, pz + 3, state("spruce_planks"))
    # viewing floor r3 open (railing), roof r4/r3/r2 + spire
    verdieping(3, g + 12, 3, open_=True)
    ring_dak(4, g + 15)
    ring_dak(3, g + 16, buiten=False)
    ring_dak(2, g + 17, buiten=False)
    park.fill(px - 1, g + 18, pz - 1, px + 1, g + 18, pz + 1, state(f"{dak}_planks"))
    for y in range(g + 19, g + 22):
        park.set(px, y, pz, "minecraft:dark_oak_fence")
    park.set(px, g + 22, pz, "minecraft:gold_block")
    park.set(px, g + 14, pz + 1, state("lantern", hanging="true", waterlogged="false"))
    # ladder up the middle (a red post with the ladder on its north face), holes in the floors
    for y in range(g + 1, g + 13):
        park.set(px, y, pz, pilaar)
        park.set(px, y, pz - 1, state("ladder", facing="north", waterlogged="false"))
    for y in (g + 6, g + 11):
        park.set(px, y, pz - 1, state("ladder", facing="north", waterlogged="false"))
    # the geheimpje: an old teapot behind the pagode (north side of the platform)
    park.set(px + 3, g + 1, pz - 6, state("decorated_pot", facing="south", cracked="true", waterlogged="false"))
    park.set(px + 2, g + 1, pz - 6, "minecraft:potted_bamboo")
    PLAATS["pagode"] = (px, g + 13, pz)
    # a stone trail up from the nearest truck-route / path point
    xs, zs = np.nonzero(park.pad[px - 60:px + 61, pz - 60:pz + 61])
    if len(xs):
        i = int(np.argmin(np.hypot(xs - 60, zs - 60)))
        tx, tz = px - 60 + int(xs[i]), pz - 60 + int(zs[i])
        # start the trail at the platform edge facing the path
        a = math.atan2(tz - pz, tx - px)
        sx, sz = int(round(px + math.cos(a) * 7)), int(round(pz + math.sin(a) * 7))
        _trail(park, (sx, sz), (tx, tz))
        log(f"    pagode bij ({px},{pz}) y{g}, pad vanaf ({tx},{tz})")
    else:
        log(f"    pagode bij ({px},{pz}) y{g}")


def kloof_hangbrug(park, log):
    hx, hz, _, _ = kit.RESERVERINGEN["hangbrug"]
    xs, zs = np.nonzero(park.water[hx - 16:hx + 17, hz - 16:hz + 17])
    if len(xs) < 5:
        log("    hangbrug: geen rivier gevonden")
        return
    pts = np.stack([xs + hx - 16, zs + hz - 16], 1).astype(float)
    c = pts.mean(0)
    cov = np.cov((pts - c).T)
    w, v = np.linalg.eigh(cov)
    ux, uz = v[:, 1]                  # along the river
    nx, nz = -uz, ux                  # across
    # the bridge crosses at the water point closest to the reservation
    i = int(np.argmin(np.hypot(pts[:, 0] - hx, pts[:, 1] - hz)))
    bx, bz = pts[i]
    rng = np.random.default_rng(kit.SEED + 41)
    wd = distance_transform_edt(~park.water)
    RAND = 74
    # gorge walls: raise the banks along this stretch of river
    for dx in range(-16, 17):
        for dz in range(-16, 17):
            x, z = int(bx) + dx, int(bz) + dz
            along = dx * ux + dz * uz
            if abs(along) > 14 or park.water[x, z] or park.pad[x, z] or park.hek_afstand[x, z] < 5:
                continue
            d = wd[x, z]
            if d > 9:
                continue
            nh = RAND + int(rng.integers(-1, 2)) - max(0, int(abs(along)) - 10)
            if d > 6:
                nh -= int((d - 6) * 2)
            if nh <= park.hoogte[x, z]:
                continue
            ruim(park, x, z, 1)
            zet_hoogte(park, x, z, nh, top="grass_block", sub="stone", diep="stone")
            for y in range(int(park.vloer[x, z]), nh):
                if park.v[x, y, z] != 0:
                    park.v[x, y, z] = park.id(("stone", "andesite", "stone", "tuff")[int(rng.integers(0, 4))])
    # the bridge: from rim to rim across the water at (bx, bz)
    eind = []
    for s in (1, -1):
        t = 0
        while t < 20 and park.water[int(round(bx + nx * s * t)), int(round(bz + nz * s * t))]:
            t += 1
        t += 2
        ex, ez = int(round(bx + nx * s * t)), int(round(bz + nz * s * t))
        eind.append((ex, ez))
        for ddx in range(-2, 3):
            for ddz in range(-2, 3):
                zet_hoogte(park, ex + ddx, ez + ddz, RAND, top="minecraft:spruce_planks")
        for (ox, oz) in ((ex + int(round(-nz * 2)), ez + int(round(nx * 2))), (ex - int(round(-nz * 2)),
                                                                                 ez - int(round(nx * 2)))):
            for y in range(RAND + 1, RAND + 4):
                park.set(ox, y, oz, state("spruce_log", axis="y"))
            park.set(ox, RAND + 4, oz, state("lantern", hanging="false", waterlogged="false"))
    (ax, az), (cx, cz) = eind
    hangbrug(park, (ax + 0.5, az + 0.5, 0.5), (cx + 0.5, cz + 0.5, 0.5), RAND + 1, plank="spruce")
    # cherry trees on both rims (red panda's on one side, gibbons on the other)
    bomen = {1: [], -1: []}
    for s in (1, -1):
        for off in range(-12, 13, 4):
            for t in range(7, 14):
                tx = int(round(bx + nx * s * t + ux * off))
                tz = int(round(bz + nz * s * t + uz * off))
                if park.water[tx, tz] or park.pad[tx, tz] or park.hek_afstand[tx, tz] < 7:
                    continue
                if any(math.hypot(tx - ox, tz - oz) < 5 for ox, _, oz in bomen[s]) or                         any(math.hypot(tx - ex, tz - ez) < 4 for ex, ez in eind):
                    break
                gy = int(park.hoogte[tx, tz])
                top = park.boom("kers", tx, gy, tz, grootte=1.1)
                park.bomen.append((tx, top, tz, "kers"))
                bomen[s].append((tx, gy + 1, tz))
                break
            if len(bomen[s]) >= 4:
                break
    PLAATS["hangbrug_a"] = bomen[1]
    PLAATS["hangbrug_b"] = bomen[-1]
    PLAATS["hangbrug"] = (int(bx), RAND + 1, int(bz))
    log(f"    kloof + hangbrug bij ({int(bx)},{int(bz)}), rand y{RAND}, brug {eind}")


def theeterrassen(park, log):
    tx, tz, r, _ = kit.RESERVERINGEN["theeterrassen"]
    r = 15
    zi = ZONES.index("bamboe")
    pd = distance_transform_edt(~park.pad)
    x0, z0 = tx - r - 2, tz - r - 2
    sub = park.hoogte[x0:x0 + 2 * r + 5, z0:z0 + 2 * r + 5].astype(float)
    glad = gaussian_filter(sub, 2.0)
    nieuw = {}
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            x, z = tx + dx, tz + dz
            if math.hypot(dx, dz) > r or park.zone[x, z] != zi or park.water[x, z] or pd[x, z] <= 2 or \
                    park.hek_afstand[x, z] < 5:
                continue
            nh = int(round(glad[x - x0, z - z0] / 3.0) * 3)
            nieuw[(x, z)] = nh
    ruim(park, tx, tz, r + 1, extra=0)
    for (x, z), nh in nieuw.items():
        zet_hoogte(park, x, z, nh, top="grass_block", sub="dirt")
    # faces: where a neighbour is lower, the exposed side is stone brick
    for (x, z), nh in nieuw.items():
        laagste = min(int(park.hoogte[x + dx, z + dz]) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if laagste < nh:
            for y in range(laagste + 1, nh + 1):
                park.set(x, y, z, "minecraft:mossy_stone_bricks" if RNG.random() < 0.3 else "minecraft:stone_bricks")
        elif (z % 2 == 0) and park.v[x, nh + 1, z] == 0:
            park.set(x, nh + 1, z, "minecraft:flowering_azalea" if RNG.random() < 0.25 else "minecraft:azalea")
    # a small stone lantern at the top terrace
    if nieuw:
        (lx, lz), lh = max(nieuw.items(), key=lambda kv: kv[1])
        park.set(lx, lh + 1, lz, "minecraft:stone_brick_wall")
        park.set(lx, lh + 2, lz, state("lantern", hanging="false", waterlogged="false"))
    PLAATS["theeterrassen"] = (tx, int(park.hoogte[tx, tz]) + 1, tz)
    log(f"    theeterrassen bij ({tx},{tz}), {len(nieuw)} kolommen")


def doolhof(park, log):
    ox, oz, _, _ = kit.RESERVERINGEN["bamboedoolhof"]
    vermijd = [(x, z, r + 6) for n, (x, z, r, zone) in kit.RESERVERINGEN.items() if n != "bamboedoolhof"]
    if "hangbrug" in PLAATS:
        hx, _, hz = PLAATS["hangbrug"]
        vermijd.append((hx, hz, 22))
    plek = zoek(park, "bamboe", (ox, oz), 50, hek=7, pad=(4, None), water=(3, None), zonder=vermijd, dicht=12)
    if plek is None:
        log("    bamboedoolhof: geen plek")
        return
    cx, cz = plek
    N = 21
    x0, z0 = cx - N // 2, cz - N // 2
    g = int(np.median(park.hoogte[x0:x0 + N, z0:z0 + N]))
    ruim(park, cx, cz, 16, extra=2)
    orig = park.hoogte.copy()
    for dx in range(-6, N + 6):
        for dz in range(-6, N + 6):
            x, z = x0 + dx, z0 + dz
            d = max(-dx - 1, dx - N, -dz - 1, dz - N, 0)        # distance outside the maze square
            if park.water[x, z] or park.pad[x, z] or park.hek_afstand[x, z] < 5:
                continue
            nh = g if d == 0 else int(round(g + (int(orig[x, z]) - g) * min(d / 6.0, 1.0)))
            if d and nh == int(orig[x, z]):
                continue
            zet_hoogte(park, x, z, nh, top="podzol" if d == 0 else "grass_block", sub="dirt")
    # perfect maze on a 10x10 cell grid (cells at odd coordinates), depth-first
    rng = np.random.default_rng(kit.SEED + 43)
    muur = np.ones((N, N), bool)
    stack = [(1, 1)]
    muur[1, 1] = False
    while stack:
        cxm, czm = stack[-1]
        buren = [(cxm + dx, czm + dz) for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2))
                 if 0 < cxm + dx < N - 1 and 0 < czm + dz < N - 1 and muur[cxm + dx, czm + dz]]
        if not buren:
            stack.pop()
            continue
        nxm, nzm = buren[int(rng.integers(0, len(buren)))]
        muur[(cxm + nxm) // 2, (czm + nzm) // 2] = False
        muur[nxm, nzm] = False
        stack.append((nxm, nzm))
    # clearing in the middle and two entrances (west and east)
    muur[N // 2 - 2:N // 2 + 3, N // 2 - 2:N // 2 + 3] = False
    muur[0, N // 2 - 1 if (N // 2 - 1) % 2 else N // 2] = False
    ingang_w = (0, [z for z in range(1, N - 1, 2)][len(range(1, N - 1, 2)) // 2])
    ingang_o = (N - 1, [z for z in range(1, N - 1, 2)][len(range(1, N - 1, 2)) // 2 - 1])
    for (ix, iz) in (ingang_w, ingang_o):
        muur[ix, iz] = False
    for i in range(N):
        for j in range(N):
            x, z = x0 + i, z0 + j
            if muur[i, j]:
                hh = int(rng.integers(6, 10))
                for k in range(hh):
                    blad = "large" if k >= hh - 2 else ("small" if k >= hh - 4 else "none")
                    park.set(x, g + 1 + k, z, "minecraft:bamboo", age=1 if k < hh - 1 else 0, leaves=blad, stage=0)
            else:
                park.set(x, g, z, "minecraft:coarse_dirt" if rng.random() < 0.7 else "minecraft:rooted_dirt")
    # a bench of stone slabs in the clearing
    for k in (-1, 0, 1):
        park.set(cx + k, g + 1, cz + 2, slab("smooth_stone_slab", "bottom"))
    park.set(cx, g + 1, cz - 2, "minecraft:stone_brick_wall")
    park.set(cx, g + 2, cz - 2, state("lantern", hanging="false", waterlogged="false"))
    PLAATS["doolhof"] = (cx, g + 1, cz)
    log(f"    bamboedoolhof bij ({cx},{cz}) (reservering {ox},{oz}), y{g}")


def hut(park, log):
    vermijd = [(x, z, r + 8) for (x, z, r, zone) in kit.RESERVERINGEN.values()]
    for k in ("doolhof", "hangbrug"):
        if k in PLAATS:
            vermijd.append((PLAATS[k][0], PLAATS[k][2], 22))
    h = zoek(park, "bamboe", (590, 200), 120, hek=7, pad=(4, None), water=(2, 9), zonder=vermijd, dicht=2)
    if not h:
        log("    schuilhut bamboe: geen plek")
        return
    wd = getattr(park, "_water_d", None)
    if wd is None:
        wd = park._water_d = distance_transform_edt(~park.water)
    best = min(((wd[h[0] + dx * 4, h[1] + dz * 4], k) for k, (dx, dz) in
                {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}.items()))[1]
    schuilhut(park, h[0], h[1], kijk=best, hout="dark_oak")
    PLAATS["schuilhut"] = (h[0], int(park.hoogte[h[0], h[1]]) + 1, h[1])
    log(f"    schuilhut bamboe bij {h}, kijkt {best}")


def bouw(park, log=print):
    log("  bamboe:")
    pagode(park, log)
    kloof_hangbrug(park, log)
    theeterrassen(park, log)
    doolhof(park, log)
    hut(park, log)


def _droog(park, x, y, z):
    """The dry column next to a tree trunk (x, z) closest to it, as a standing position."""
    best = None
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if (dx, dz) == (0, 0) or park.water[x + dx, z + dz]:
                continue
            if "log" in park.get(x + dx, int(park.hoogte[x + dx, z + dz]) + 1, z + dz):
                continue
            d = dx * dx + dz * dz
            if best is None or d < best[0]:
                best = (d, x + dx, z + dz)
    if best is None:
        return x, y, z
    _, bx, bz = best
    return bx, int(park.hoogte[bx, bz]) + 1, bz


def plekken(park, plekken):
    """Rode panda's in the cherry trees on one rim of the gorge, gibbons on the other rim, reuzenpanda's in the maze."""
    n = {}
    for p in plekken:
        s = p["soort"]
        i = n.get(s, 0)
        n[s] = i + 1
        if s == "rode_panda" and PLAATS.get("hangbrug_a"):
            b = PLAATS["hangbrug_a"]
            x, y, z = _droog(park, *b[i % len(b)])
            p["centrum"] = [x, y, z]
            p["straal"] = 10
        elif s == "gibbon" and (PLAATS.get("hangbrug_b") or PLAATS.get("hangbrug_a")):
            b = PLAATS.get("hangbrug_b") or PLAATS["hangbrug_a"]
            x, y, z = _droog(park, *b[(i + 1) % len(b)])
            p["centrum"] = [x, y, z]
            p["straal"] = 12
        elif s == "reuzenpanda" and "doolhof" in PLAATS:
            x, y, z = PLAATS["doolhof"]
            p["centrum"] = [x + (1 if i == 0 else -1), y, z]
            p["straal"] = 10
