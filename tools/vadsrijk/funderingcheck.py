"""X-KAART: foundation / floating-structure / leaking-water check over the generated map tiles (user feedback round X: "in zone 6
sluiten veel gebouwen niet goed aan op de grond of laten ze dingen zweven, of laten paden/water raar lopen").

    python tools/vadsrijk/funderingcheck.py [--zone 1..6|alle] [--max N] [--json uit.json] [--ook-steden]
Exit code 1 when zone 6 outside the towns has any GAT (the gate for a zone-6 regen).
Zone 6 uses the generator's own ground heights (_cache/z6/terrein.npz, written by maak6 / fundering6) when present: its rock
bands are also wall materials, so names alone cannot tell the ground there. Zones 1-5 (and zone 6 without the cache) use names.

Per column (x, z) of a tile:
  T = the highest TERRAIN block (by name, per zone: sand/terracotta/grass/stone/... see TERREIN) or water;
  b = the lowest BUILT block above T (anything solid that is not terrain, not a plant/flower/leaf/carpet/lantern/sign/vine);
  gap = b - T - 1 = the air (or plants) between the ground and the structure.
  base = the lowest b in the 5x5 columns around it: a column counts only when it IS the structure's bottom (b == base), so a
  roof eave, an arch over a passage or a balcony (higher than the walls next to it) never counts.
Findings (outside the town boxes, unless --ook-steden):
  GAT    1 <= gap <= 6 at the bottom of a structure: a floating corner / wall / stair / fence / path over a dip (= no foundation);
  ZWEEFT gap > 6 at the bottom of a structure (whole thing in the air: a ring/lens on purpose, or a real bug) - listed, no gate;
  LEK    a water block with an AIR cell beside it or under it (it would flow there: hanging water, a waterfall out of nowhere);
  BROK   a lone terrain/built bit of <= 8 blocks not touching anything else (floating terrain crumbs, a floating block).
The gap cells must be OUTSIDE air (connected to the sky in the tile): the hollowed interior of solid masses is not a gap.
Findings are clustered (8-connected in x/z) into spots: the report prints one line per spot (centre, size, max gap).
"""
import argparse
import json
import os
import re
import sys
from collections import defaultdict

import numpy as np
from scipy import ndimage

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "audit"))
import tegellezer as TL  # noqa: E402

ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
JAVA = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")
ZONES = {"1": (-11, -11, 11, 18),   # X-Z1GROND: the zone-1 map grid only (wereld/kaart.py TX/TZ; was -20,-16,15,20 = zone 2 too)
          "2": (6, -23, 15, -13), "3": (16, -16, 26, -3), "4": (27, -12, 39, -5),
         "5": (38, -12, 48, -5), "6": (51, -18, 66, -7)}
MAX_GAT = 6
HOOGTE_ZONES = ("1", "2")   # X-Z1GROND: zones checked against the generator's own ground heights (funderingcheck_hoogte.py)

# terrain by name: what the land generators lay as ground. Zone 6's rock BANDS (pleister_roze/perzik, roze_marmer) are also wall
# materials, so they are NOT terrain here (the bands only show on cliff faces; the column top is sand/rock/grass/salt).
TERREIN_ALG = ("grass_block", "dirt", "coarse_dirt", "rooted_dirt", "podzol", "mycelium", "mud", "packed_mud", "clay", "gravel",
               "sand", "red_sand", "stone", "deepslate", "andesite", "diorite", "granite", "tuff", "calcite", "dripstone_block",
               "snow_block", "powder_snow", "ice", "packed_ice", "blue_ice", "moss_block", "terracotta", "concrete_powder",
               "sandstone", "netherrack", "bedrock", "obsidian", "basalt", "blackstone", "dirt_path", "farmland", "_ore",
               "roze_zand", "knuffelgras", "knuffelsteen", "wolkenweide_gras", "wolkenweide_steen", "walvishuid",
               "guh_kristalsteen", "kristalsteen", "zwamgrond", "sporengrond", "schemergrond", "wortelgrond", "mosgrond",
               "zoutkorst", "lampiongras", "glimgrond", "moddergrond", "kustzand", "duinzand")
NIET_TERREIN = ("glazed_terracotta", "_stairs", "_slab", "_wall", "chiseled_", "cut_", "_bricks", "_brick")
ZACHT = ("air", "water", "lava", "leaves", "grass", "fern", "flower", "tulip", "allium", "daisy", "bluet", "poppy", "dandelion",
         "cornflower", "orchid", "lily", "petals", "rose", "lilac", "peony", "sapling", "carpet", "lantern", "lampion", "torch",
         "sign", "banner", "vine", "chain", "candle", "button", "lever", "rail", "pressure_plate", "mushroom", "bush", "kelp",
         "seagrass", "pickle", "moss_carpet", "web", "snow[", "_head", "skull", "hibiscus", "guhbloem", "guhoortjes", "riet",
         "glimplant", "glimpaddenstoel", "dripleaf", "spore", "azalea", "berries", "wildflowers", "leaf_litter", "frogspawn",
         "light[", "structure_void", "barrier", "_rod", "bell", "pot", "vadstouw", "vlaggetjes", "slingers", "tripwire",
         "_blad", "palm_kroon", "lantaarn", "lampje", "cactus", "sugar_cane", "bamboo", "cocoa", "dead_bush", "scaffolding")


def klasse(s):
    """0 = air/soft, 1 = terrain, 2 = built, 3 = water."""
    n = s.split("[")[0].split(":")[-1]
    if n in ("water",) or n.endswith("bubble_column"):
        return 3
    if s.split("[")[0] == "minecraft:grass_block" or n in ("knuffelgras", "wolkenweide_gras", "lampiongras"):
        return 1
    if any(w in n for w in TERREIN_ALG) and not any(w in n for w in NIET_TERREIN):
        return 1
    if any(w in n or w in s for w in ZACHT):
        return 0
    return 2


# ------------------------------------------------------------------------------------------------ towns (skipped)
def steden(zone):
    """[(x1, z1, x2, z2)] of the town areas of a zone (stad = true), from the generated *WereldPlekken Gebied lists + zone 6's plan."""
    uit = []
    if zone == "6":
        sys.path.insert(0, os.path.join(HIER, "zone6"))
        import plan6
        uit += [(g[1], g[2], g[3], g[4]) for g in plan6.GEBIEDEN if g[8]]
        return uit
    pat = re.compile(r'new Gebied\("[^"]+",\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*"[^"]*",\s*\d+,\s*\d+,\s*true\)')
    namen = {"1": "wereld/WereldPlekken.java", "2": "zone2/wereld/Z2WereldPlekken.java", "3": "zone3/wereld/Z3WereldPlekken.java",
             "4": "zone4/wereld/Z4WereldPlekken.java", "5": "zone5/wereld/Z5WereldPlekken.java"}
    p = os.path.join(JAVA, namen.get(zone, ""))
    if os.path.isfile(p):
        with open(p, encoding="utf-8") as f:
            uit += [tuple(int(v) for v in m) for m in pat.findall(f.read())]
    return uit


def in_stad(x, z, boxen):
    return any(a <= x <= c and b <= z <= d for (a, b, c, d) in boxen)


# ------------------------------------------------------------------------------------------------ one tile
T0 = {}   # zone 6: the ground heights of the generator (_cache/z6/terrein.npz, written by maak6): (T, x1, z1)


def laad_terrein(zone="6"):
    """X-Z1GROND: zone 1 too (_cache/z1/terrein.npz, written by maak_kaart / wereld/fundering1.py)."""
    k = "z" + zone
    p = os.path.join(HIER, "_cache", k, "terrein.npz")
    if os.path.exists(p) and k not in T0:
        d = np.load(p)
        T0[k] = (d["T"].astype(np.int32), int(d["b"][0]), int(d["b"][2])) + ((d["pad"],) if "pad" in d.files else ())
    return k in T0


def tegel(pad, tx, tz, ref=None):
    """ref: an (64, 64) ground height per column (zone 6: the generator's), else the highest terrain-named block."""
    pal, (xs, ys, zs, st) = TL.lees(pad)
    kl = np.array([klasse(s) for s in pal] + [0], np.int8)
    vol = np.full((TL.T, TL.HOOG, TL.T), len(pal), np.int32)
    vol[xs, ys, zs] = st
    K = kl[vol]                                   # (64, 320, 64) classes
    namen = pal + ["minecraft:air"]
    Y = np.arange(TL.HOOG)[None, :, None]
    # T = highest terrain/water y per column; b = lowest built y above T
    grond = (K == 1) | (K == 3)
    T = np.where(grond.any(1), TL.HOOG - 1 - np.argmax(grond[:, ::-1, :], axis=1), -1)
    if ref is not None:
        T = ref
    boven = (K == 2) & (Y > T[:, None, :])
    heeft_b = boven.any(1)
    b = np.where(heeft_b, np.argmax(boven, axis=1), 10 ** 6)
    # outside air: soft/air cells connected (6-neighbour) to the top layer or the tile sides
    lucht = (K == 0)
    lab, n = ndimage.label(lucht)
    rand = np.unique(np.concatenate([lab[:, -1, :].ravel(), lab[0].ravel(), lab[-1].ravel(), lab[:, :, 0].ravel(),
                                     lab[:, :, -1].ravel()]))
    buiten = np.isin(lab, rand[rand > 0])
    gap = np.where(heeft_b & (T >= 0), b - T - 1, -1)
    bkaart = np.where(heeft_b & (T >= 0), b, 10 ** 6)   # (the 5x5 "is it the bottom" test runs zone-wide in check())
    fout = []
    ii, kk = np.nonzero(gap >= 1)
    for i, k in zip(ii.tolist(), kk.tolist()):
        t, bb = int(T[i, k]), int(b[i, k])
        if ref is not None:   # the gap = the soft run right under b; its support must be the ground (not dug below it)
            y = bb - 1
            while y > t and K[i, y, k] == 0:
                y -= 1
            if y == bb - 1:
                continue
            if y < t or (K[i, y, k] == 0 and buiten[i, y, k]):
                continue                              # dug below the old ground (a stairwell, a cave mouth)
            t = y
        if "carpet" in namen[vol[i, bb - 1, k]]:
            continue                                  # standing on a carpet (a tent wall on its rug)
        if K[i, t, k] == 3:
            continue                                  # over water (a bridge / a jetty): not a foundation issue
        if not buiten[i, t + 1:bb, k].any():
            continue                                  # hollowed interior of a solid mass
        soort = "GAT" if bb - t - 1 <= MAX_GAT else "ZWEEFT"
        fout.append((soort, tx * 64 + i, bb, tz * 64 + k, bb - t - 1, namen[vol[i, bb, k]]))
    # leaking water: a water cell with an outside-air neighbour beside or below
    w = (K == 3)
    lek = np.zeros_like(w)
    for (dx, dy, dz) in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
        sh = np.zeros_like(buiten)
        xs_ = slice(max(dx, 0), TL.T + min(dx, 0))
        xd = slice(max(-dx, 0), TL.T + min(-dx, 0))
        ys_ = slice(max(dy, 0), TL.HOOG + min(dy, 0))
        yd = slice(max(-dy, 0), TL.HOOG + min(-dy, 0))
        zs_ = slice(max(dz, 0), TL.T + min(dz, 0))
        zd = slice(max(-dz, 0), TL.T + min(-dz, 0))
        sh[xd, yd, zd] = buiten[xs_, ys_, zs_] & (vol[xs_, ys_, zs_] == len(pal))   # true air only (not a plant)
        lek |= w & sh
    for i, y, k in zip(*np.nonzero(lek)):
        fout.append(("LEK", tx * 64 + int(i), int(y), tz * 64 + int(k), 0, namen[vol[i, y, k]]))
    # crumbs: small solid components (terrain or built) not touching anything (26-connected)
    vast = (K == 1) | (K == 2)
    lb, nb = ndimage.label(vast, structure=np.ones((3, 3, 3), bool))
    if nb:
        maat = ndimage.sum(np.ones_like(lb), lb, index=np.arange(1, nb + 1))
        sl = ndimage.find_objects(lb)
        for j, m in enumerate(maat, 1):
            if m > 8:
                continue
            s_ = sl[j - 1]
            if s_[0].start == 0 or s_[0].stop == TL.T or s_[2].start == 0 or s_[2].stop == TL.T or s_[1].start == 0:
                continue                              # touches the tile edge: may continue in the neighbour tile
            # a lone bit with only air (no plant/water) around and under it
            onder = s_[1].start - 1
            sub = lab[s_[0], onder, s_[2]] if onder >= 0 else None
            if sub is not None and (K[s_[0], onder, s_[2]] == 0).all():
                cx, cz = (s_[0].start + s_[0].stop) // 2, (s_[2].start + s_[2].stop) // 2
                fout.append(("BROK", tx * 64 + cx, s_[1].start, tz * 64 + cz, int(m),
                             namen[vol[s_][lb[s_] == j][0]]))
    return fout, bkaart


def clusters(fout):
    """8-connected spots per kind in x/z: (kind, cx, y, cz, cells, max gap, example block)."""
    per = defaultdict(list)
    for f in fout:
        per[f[0]].append(f)
    uit = []
    for soort, lijst in per.items():
        cel = {(f[1], f[3]): f for f in lijst}
        gezien = set()
        for c in cel:
            if c in gezien:
                continue
            stapel, groep = [c], []
            gezien.add(c)
            while stapel:
                p = stapel.pop()
                groep.append(cel[p])
                for dx in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        q = (p[0] + dx, p[1] + dz)
                        if q in cel and q not in gezien:
                            gezien.add(q)
                            stapel.append(q)
            xs = [g[1] for g in groep]
            zs = [g[3] for g in groep]
            m = max(groep, key=lambda g: g[4])
            uit.append((soort, sum(xs) // len(xs), m[2], sum(zs) // len(zs), len(groep), m[4], m[5].split("[")[0]))
    return sorted(uit, key=lambda u: (u[0], -u[4]))


def check(zone, maxn, ook_steden=False, stil=False):
    if zone in HOOGTE_ZONES and laad_terrein(zone):   # X-Z1GROND: the height-based check (the generator's ground)
        import funderingcheck_hoogte as FH
        return FH.check(zone, maxn, TEGELS, ZONES[zone], T0["z" + zone], stil)
    tx0, tz0, tx1, tz1 = ZONES[zone]
    boxen = [] if ook_steden else steden(zone)
    fout = []
    bkaarten = {}
    for naam in sorted(os.listdir(TEGELS)):
        if not naam.startswith("t_") or not naam.endswith(".nbt"):
            continue
        tx, tz = map(int, naam[2:-4].split("_"))
        if not (tx0 <= tx <= tx1 and tz0 <= tz <= tz1):
            continue
        ref = None
        if zone == "6" and laad_terrein("6"):
            Tz, x1, z1 = T0["z6"]
            i0, k0 = tx * 64 - x1, tz * 64 - z1
            if 0 <= i0 and 0 <= k0 and i0 + 64 <= Tz.shape[0] and k0 + 64 <= Tz.shape[1]:
                ref = Tz[i0:i0 + 64, k0:k0 + 64]
        f_, bk = tegel(os.path.join(TEGELS, naam), tx, tz, ref)
        bkaarten[(tx, tz)] = bk
        fout += [f for f in f_ if not in_stad(f[1], f[3], boxen)]

    def bvan(x, z):
        bk = bkaarten.get((x // 64, z // 64))
        return 10 ** 6 if bk is None else int(bk[x % 64, z % 64])
    # a GAT/ZWEEFT column counts only when it IS the bottom of its structure: no lower b in the 5x5 around it (across tiles)
    if zone == "6":   # the Sterrendovende Toren's rings and lens float on purpose (plan6 area z6_toren)
        sys.path.insert(0, os.path.join(HIER, "zone6"))
        import plan6
        tb = [(g[1], g[2], g[3], g[4]) for g in plan6.GEBIEDEN if g[0] == "z6_toren"]
        fout = [f for f in fout if not (f[0] == "ZWEEFT" and in_stad(f[1], f[3], tb))]
    fout = [f for f in fout if f[0] not in ("GAT", "ZWEEFT")
            or all(bvan(f[1] + dx, f[3] + dz) >= f[2] for dx in range(-2, 3) for dz in range(-2, 3))]
    if zone == "5":   # (zone-4 tiles tx 38..39 are shared: keep only y >= 118 for zone 5, the rest is zone 4's)
        fout = [f for f in fout if f[2] >= 118]
    if zone == "4":
        fout = [f for f in fout if not (f[1] >= 38 * 64 and f[2] >= 118)]
    cl = clusters(fout)
    tel = defaultdict(int)
    plek = defaultdict(int)
    for f in fout:
        tel[f[0]] += 1
    for c in cl:
        plek[c[0]] += 1
    if not stil:
        print(f"zone {zone}{' (steden overgeslagen: %d)' % len(boxen) if boxen else ''}: "
              + ", ".join(f"{k} {tel[k]} cellen / {plek[k]} plekken" for k in ("GAT", "ZWEEFT", "LEK", "BROK")))
        for c in cl[:maxn]:
            print(f"   {c[0]:6s} /tp @s {c[1]} {c[2] + 1} {c[3]}   {c[4]:4d} cel  max {c[5]:2d}  {c[6]}")
    return dict(tel), dict(plek), cl


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--zone", default="alle")
    ap.add_argument("--max", type=int, default=30)
    ap.add_argument("--json", default=None)
    ap.add_argument("--ook-steden", action="store_true")
    ap.add_argument("--tegels", default=None, help="another tile folder (e.g. a before/after comparison)")
    a = ap.parse_args()
    global TEGELS
    if a.tegels:
        TEGELS = a.tegels
    alle, poort = {}, 0
    for z in (ZONES if a.zone == "alle" else [a.zone]):
        tel, plek, cl = check(z, a.max, a.ook_steden)
        alle[z] = dict(cellen=tel, plekken=plek, lijst=cl)
        if z == "6":
            poort = tel.get("GAT", 0)
    if a.json:
        with open(a.json, "w", encoding="utf-8") as f:
            json.dump(alle, f, indent=1)
    sys.exit(1 if poort else 0)


if __name__ == "__main__":
    main()
