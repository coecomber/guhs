"""X-Z1GROND: the height-based foundation check for zone 1 (+ the Knuffeleiland / Knuffelkamp tutorial island), called by
funderingcheck.py when _cache/z1/terrein.npz exists (maak_kaart writes it; or python tools/vadsrijk/wereld/fundering1.py).

Names alone cannot tell walls from ground in zone 1 (roze_knuffelsteen temple walls, terracotta mushroom stems, stone towers),
and most things that hang in the air are meant to (giant mushroom caps, tree-top walkways, viewpoint platforms, bridges, the
temple's door lintels). So this check only uses SOLID vs not, plus the generator's ground height T per column:

  per column: walk up from the ground T through the solid base run (the ground and whatever stands on it), to the first
  non-solid cell s; b = the next solid cell above it (the underside of the first overhang); gap = b - s.
  GAT    1 <= gap <= 6, the gap is outside air (not a hollowed interior, not water), the support s - 1 >= T (not dug), AND the
         same structure meets the ground right next to it: a column within 3 that is solid at b and at b - 1 with its ground
         T == b - 1. That is a wall / floor / stair row that stands on the ground and runs on over a dip = no foundation.
         A mushroom cap, a walkway, a platform, a bridge deck or a lintel over a door never meets the ground at its own
         underside, so it is not reported (no name lists needed).
  ZWEEFT the same with 7 <= gap <= 16 (a wall row running off a cliff edge).
  LEK    a water block with outside AIR beside it or under it (as funderingcheck).
  BROK   a lone component (26-connected) of any non-air, non-water blocks of <= 64 blocks, not touching the tile edge or water:
         it touches nothing, so it floats (a lantern/fence/crumb in the air). Leaves and plants count as blocks here, so a
         branch inside a canopy is not a crumb.
"""
import os
import sys
from collections import OrderedDict, defaultdict

import numpy as np
from scipy import ndimage

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "audit"))
import tegellezer as TL  # noqa: E402

ZACHT = ("air", "water", "lava", "leaves", "short_grass", "tall_grass", "fern", "flower", "tulip", "allium", "daisy", "bluet", "poppy", "dandelion",
         "cornflower", "orchid", "lily", "petals", "rose", "lilac", "peony", "sapling", "carpet", "lantern", "lampion", "torch",
         "sign", "banner", "vine", "chain", "candle", "button", "lever", "rail", "pressure_plate", "bush", "kelp",
         "seagrass", "pickle", "web", "snow[", "_head", "skull", "hibiscus", "guhbloem", "guhoortjes", "riet",
         "glimplant", "spore", "azalea", "berries", "wildflowers", "leaf_litter", "frogspawn", "ladder",
         "light[", "structure_void", "barrier", "_rod", "bell", "pot", "vadstouw", "vlaggetjes", "slingers", "tripwire",
         "_blad", "palm_kroon", "lantaarn", "lampje", "cactus", "sugar_cane", "bamboo", "cocoa", "dead_bush", "scaffolding",
         "dripleaf", "pointed_dripstone", "string", "campfire", "trapdoor", "_button", "hanging_moss", "glimpaddenstoel")
MAX_GAT, MAX_ZWEEF, R_RAAK, MAX_BROK = 6, 16, 5, 64
TOL = 1   # the built ground may sit 1 off the generator height (Knuffelkamp paths and terraces, the ground block under a floor)
LUCHT, ZACHTK, VAST, WATER, GROND, HOED = 0, 1, 2, 3, 4, 5
# mushroom-cap material: a cap rim hanging just over the ground is how a giant mushroom looks (never a missing foundation)
_HOED = ("_wool", "_mushroom_block", "paddenstoel_hoed", "knuffelblok_", "wolkenblok")   # (+ cloud blocks)
_SLINGER = ("slinger", "lampion", "vlaggetjes", "vadstouw")   # garlands strung between poles (their pieces hang free)
# natural ground (the support under a gap): a gap of 2+ over anything else (a slab walkway, a plank floor, a paved path) is a
# passage under an arch / a deck / a needle's rock, never a missing foundation
_GROND = ("grass_block", "dirt", "coarse_dirt", "rooted_dirt", "podzol", "mycelium", "mud", "clay", "gravel", "sand", "stone",
          "andesite", "diorite", "granite", "tuff", "calcite", "dripstone_block", "snow_block", "moss_block", "terracotta",
          "sandstone", "knuffelgras", "_gras", "deepslate", "mossy_cobblestone", "farmland")
_NIET_GROND = ("glazed_terracotta", "_stairs", "_slab", "_wall", "chiseled_", "cut_", "_bricks", "_brick", "polished_", "smooth_",
               "stone_button", "pressure", "stonecutter", "dirt_path")


def klasse(s):
    n = s.split("[")[0].split(":")[-1]
    if n in ("air", "cave_air", "void_air", "light", "structure_void", "barrier"):
        return LUCHT                                 # (invisible light sources / voids count as air)
    if n == "water" or n.endswith("bubble_column"):
        return WATER
    if any(w in n for w in _HOED):
        return HOED
    if any(w in n for w in _GROND) and not any(w in n for w in _NIET_GROND):
        return GROND
    if n.endswith(("_mushroom_block", "mushroom_stem")) or ("paddenstoel" in n and "glimpaddenstoel" not in n):
        return VAST                                  # (mushroom BLOCKS are solid; the plants are soft)
    if n in ("brown_mushroom", "red_mushroom", "crimson_fungus", "warped_fungus") or any(w in s for w in ZACHT):
        return ZACHTK
    if "waterlogged=true" in s and ("slab" in n or "stairs" in n):
        return VAST
    return VAST


class Tegels:
    def __init__(self, map_):
        self.map, self.lru = map_, OrderedDict()

    def kl(self, tx, tz):
        k = (tx, tz)
        if k in self.lru:
            self.lru.move_to_end(k)
            return self.lru[k]
        p = os.path.join(self.map, f"t_{tx}_{tz}.nbt")
        if not os.path.exists(p):
            v = None
        else:
            pal, (xs, ys, zs, st) = TL.lees(p)
            lut = np.array([klasse(s) for s in pal], np.uint8)
            v = np.zeros((TL.T, TL.HOOG, TL.T), np.uint8)
            v[xs, ys, zs] = lut[st]
            v = (v, pal, (xs, ys, zs, st))
        self.lru[k] = v
        if len(self.lru) > 110:
            self.lru.popitem(last=False)
        return v


def venster(TG, tx, tz, m):
    """The class volume of tile (tx, tz) with a margin of m columns from its neighbours (missing = air)."""
    V = np.zeros((64 + 2 * m, TL.HOOG, 64 + 2 * m), np.uint8)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            t = TG.kl(tx + dx, tz + dz)
            if t is None:
                continue
            v = t[0]
            a0, a1 = max(0, 64 * dx + m), min(64 + 2 * m, 64 * dx + 64 + m)
            c0, c1 = max(0, 64 * dz + m), min(64 + 2 * m, 64 * dz + 64 + m)
            if a0 >= a1 or c0 >= c1:
                continue
            V[a0:a1, :, c0:c1] = v[a0 - 64 * dx - m:a1 - 64 * dx - m, :, c0 - 64 * dz - m:c1 - 64 * dz - m]
    return V


def vind(V, Tw, m, bewust=None, pad=None):
    """The foundation gaps of a class window V (X, Y, Z) with ground heights Tw (X, Z), only in the inner part (margin m):
    [(i, k, s, b)] window coords, s..b-1 = the gap cells (outside air), b = the underside that should meet the ground.
    bewust (X, Z) bool = columns that float on purpose (bridges, piers); pad (X, Z) bool = path columns (a gap of 2+ there is
    a passage under an arch). Also returns the outside-air mask."""
    X, Y, Z = V.shape
    vast = (V == VAST) | (V == GROND) | (V == HOED)
    yy = np.arange(Y)[None, :, None]
    Tc = Tw[:, None, :]
    # s = the first non-solid cell above the ground's solid base run; b = the first solid cell above s
    boven = yy > Tc
    nietvast = boven & ~vast
    heeft_s = nietvast.any(1)
    s = np.where(heeft_s, np.argmax(nietvast, axis=1), Y)
    na = (yy > s[:, None, :]) & vast
    heeft_b = na.any(1) & heeft_s & (Tw >= 0)
    b = np.where(heeft_b, np.argmax(na, axis=1), Y)
    gap = b - s
    # outside air: non-solid, non-water cells connected to the window's top / sides
    lucht = (V == LUCHT) | (V == ZACHTK)
    lab, _n = ndimage.label(lucht)
    rand = np.unique(lab[:, -1, :])               # (sky only: the tiles are hollowed, so the inside of the ground is
    buiten = np.isin(lab, rand[rand > 0])          #  air too, and it reaches the window sides)
    uit = []
    kand = heeft_b & (gap >= 1) & (gap <= MAX_ZWEEF)
    if bewust is not None:
        kand &= ~bewust
    kand[:m, :] = kand[-m:, :] = False
    kand[:, :m] = kand[:, -m:] = False
    ii, kk = np.nonzero(kand)
    for i, k in zip(ii.tolist(), kk.tolist()):
        bb, ss, T0 = int(b[i, k]), int(s[i, k]), int(Tw[i, k])
        if V[i, ss, k] == WATER or V[i, ss - 1, k] == WATER:
            continue                                     # over water (a jetty / a bridge)
        if ss - 1 < T0 - TOL or not vast[i, ss - 1, k]:
            continue                                     # dug below the ground (a cave mouth, a stairwell)
        if V[i, bb, k] == HOED:
            continue                                     # a mushroom cap's rim
        if bb - ss >= 2 and (V[i, ss - 1, k] != GROND or V[i, bb, k] == GROND or (pad is not None and pad[i, k])):
            continue                                     # a passage over a built floor or a path (arch, deck, walkway under
                                                         # rock), or a natural overhang / cave mouth
        if not buiten[i, ss:bb, k].any():
            continue                                     # hollowed interior of a solid mass
        # the same structure meets its ground right next to it
        a0, a1, c0, c1 = i - R_RAAK, i + R_RAAK + 1, k - R_RAAK, k + R_RAAK + 1
        raak = vast[a0:a1, bb, c0:c1] & vast[a0:a1, bb - 1, c0:c1] & (np.abs(Tw[a0:a1, c0:c1] - (bb - 1)) <= TOL)
        if not raak.any():
            continue
        uit.append((i, k, ss, bb))
    return uit, buiten


def tegel(TG, tx, tz, Tw, bewust=None, pad=None):
    """Findings in tile (tx, tz). Tw = the ground heights of the window (64 + 2m)^2."""
    m = R_RAAK + 1
    V = venster(TG, tx, tz, m)
    t = TG.kl(tx, tz)
    if t is None:
        return []
    Y = V.shape[1]
    X, Z = V.shape[0], V.shape[2]
    gaten, buiten = vind(V, Tw, m, bewust, pad)
    fout = []
    for (i, k, ss, bb) in gaten:
        soort = "GAT" if bb - ss <= MAX_GAT else "ZWEEFT"
        fout.append((soort, tx * 64 + i - m, bb, tz * 64 + k - m, bb - ss, _naam(t, i - m, bb, k - m)))
    # leaking water
    v0 = V[m:-m, :, m:-m]
    bu = buiten[m:-m, :, m:-m] & (v0 == LUCHT)
    w = v0 == WATER
    lek = np.zeros_like(w)
    for (dx, dy, dz) in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
        sh = np.zeros_like(bu)
        xs_ = slice(max(dx, 0), 64 + min(dx, 0))
        xd = slice(max(-dx, 0), 64 + min(-dx, 0))
        ys_ = slice(max(dy, 0), Y + min(dy, 0))
        yd = slice(max(-dy, 0), Y + min(-dy, 0))
        zs_ = slice(max(dz, 0), 64 + min(dz, 0))
        zd = slice(max(-dz, 0), 64 + min(-dz, 0))
        sh[xd, yd, zd] = bu[xs_, ys_, zs_]
        lek |= w & sh
    for i, y, k in zip(*np.nonzero(lek)):
        fout.append(("LEK", tx * 64 + int(i), int(y), tz * 64 + int(k), 0, "minecraft:water"))
    # crumbs: small components of any non-air non-water blocks, inside the tile (+ margin), not touching water
    iets = V != LUCHT
    iets &= V != WATER
    lb, nb = ndimage.label(iets, structure=np.ones((3, 3, 3), bool))
    if nb:
        maat = ndimage.sum(np.ones_like(lb), lb, index=np.arange(1, nb + 1))
        sl = ndimage.find_objects(lb)
        natm = ndimage.binary_dilation(V == WATER, iterations=1) if (V == WATER).any() else None
        for j, mm in enumerate(maat, 1):
            if mm > MAX_BROK:
                continue
            s_ = sl[j - 1]
            if s_[0].start == 0 or s_[0].stop == X or s_[2].start == 0 or s_[2].stop == Z or s_[1].start == 0:
                continue
            cx, cz = (s_[0].start + s_[0].stop) // 2 - m, (s_[2].start + s_[2].stop) // 2 - m
            if not (0 <= cx < 64 and 0 <= cz < 64):
                continue                                 # the neighbour tile reports it
            if natm is not None and natm[s_][lb[s_] == j].any():
                continue                                 # on / at the water (lily pads, a buoy)
            nm = _naam_w(TG, tx, tz, *(np.argwhere(lb[s_] == j)[0] + np.array([s_[0].start, s_[1].start, s_[2].start])
                                        - np.array([m, 0, m])))
            if any(w_ in nm for w_ in _SLINGER):
                continue                                 # a garland between poles
            if (V[s_][lb[s_] == j] == HOED).all():
                continue                                 # wool / cap blocks only: a cloud, a flag, a cap ornament (on purpose)
            if bewust is not None and bewust[cx + m, cz + m]:
                continue
            cel = np.argwhere(lb[s_] == j)[0]
            gi, gy, gk = cel[0] + s_[0].start - m, cel[1] + s_[1].start, cel[2] + s_[2].start - m
            fout.append(("BROK", tx * 64 + cx, int(s_[1].start), tz * 64 + cz, int(mm), _naam_w(TG, tx, tz, gi, gy, gk)))
    return fout


def _naam(t, i, y, k):
    xs, ys, zs, st = t[2]
    m = (xs == i) & (ys == y) & (zs == k)
    return t[1][int(st[m][0])] if m.any() else "?"


def _naam_w(TG, tx, tz, i, y, k):
    ttx, ttz = tx + i // 64, tz + k // 64
    t = TG.kl(ttx, ttz)
    return "?" if t is None else _naam(t, i % 64, y, k % 64)


def clusters(fout):
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
            mx = max(groep, key=lambda g: g[4])
            n = sum(g[4] for g in groep) if soort == "BROK" else len(groep)
            uit.append((soort, sum(xs) // len(xs), mx[2], sum(zs) // len(zs), n, mx[4], mx[5].split("[")[0]))
    return sorted(uit, key=lambda u: (u[0], -u[4]))


def bewuste_dozen(zone):
    """[(x1, z1, x2, z2)] areas that float on purpose (zone 1: wereld/fundering1.BEWUST: the fossil bridge, the piers)."""
    if zone == "2":
        sys.path.insert(0, os.path.join(HIER, "zone2"))
        import fundering2
        return fundering2.BEWUST
    if zone != "1":
        return []
    sys.path.insert(0, os.path.join(HIER, "wereld"))
    import fundering1
    return fundering1.BEWUST


def bewust_masker(dozen, x0, z0, n):
    if not dozen:
        return None
    xs = np.arange(x0, x0 + n)[:, None]
    zs = np.arange(z0, z0 + n)[None, :]
    m = np.zeros((n, n), bool)
    for (a, b, c, d) in dozen:
        m |= (xs >= a) & (xs <= c) & (zs >= b) & (zs <= d)
    return m


def check(zone, maxn, tegels_map, doos, ref, stil=False):
    Tz, x1, z1 = ref[:3]
    Pz = ref[3] if len(ref) > 3 else None
    tx0, tz0, tx1, tz1 = doos
    TG = Tegels(tegels_map)
    m = R_RAAK + 1
    dozen = bewuste_dozen(zone)
    fout = []
    for tx in range(tx0, tx1 + 1):
        for tz in range(tz0, tz1 + 1):
            if not os.path.exists(os.path.join(tegels_map, f"t_{tx}_{tz}.nbt")):
                continue
            Tw = np.full((64 + 2 * m, 64 + 2 * m), -1, np.int32)
            gx0, gz0 = tx * 64 - m - x1, tz * 64 - m - z1
            a0, c0 = max(0, -gx0), max(0, -gz0)
            a1, c1 = min(64 + 2 * m, Tz.shape[0] - gx0), min(64 + 2 * m, Tz.shape[1] - gz0)
            Pw = np.zeros_like(Tw, bool)
            if a1 > a0 and c1 > c0:
                Tw[a0:a1, c0:c1] = Tz[gx0 + a0:gx0 + a1, gz0 + c0:gz0 + c1]
                if Pz is not None:
                    Pw[a0:a1, c0:c1] = Pz[gx0 + a0:gx0 + a1, gz0 + c0:gz0 + c1]
            fout += tegel(TG, tx, tz, Tw, bewust_masker(dozen, tx * 64 - m, tz * 64 - m, 64 + 2 * m), Pw)
    cl = clusters(fout)
    tel, plek = defaultdict(int), defaultdict(int)
    for f in fout:
        tel[f[0]] += f[4] if f[0] == "BROK" else 1
    for c in cl:
        plek[c[0]] += 1
    if not stil:
        print(f"zone {zone} (hoogte): " + ", ".join(f"{k} {tel[k]} cellen / {plek[k]} plekken" for k in ("GAT", "ZWEEFT", "LEK",
                                                                                                         "BROK")))
        for c in cl[:maxn]:
            print(f"   {c[0]:6s} /tp @s {c[1]} {c[2] + 1} {c[3]}   {c[4]:4d} cel  max {c[5]:2d}  {c[6]}")
    return dict(tel), dict(plek), cl
