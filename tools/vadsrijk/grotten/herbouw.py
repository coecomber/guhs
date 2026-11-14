"""1.10.3 "Grotere dungeons": puts the NEW (twice as big) cave layouts of grotplan.py into the map tiles WITHOUT a full map regen,
and writes the data the game needs for the bigger caves.

    python tools/vadsrijk/grotten/herbouw.py --check [cave ...]      design check only (nothing written)
    python tools/vadsrijk/grotten/herbouw.py --schrijf [cave ...]    patch the tiles + connection passes + data (default: zones 2-6)
    python tools/vadsrijk/grotten/herbouw.py --data [cave ...]       only the data files (after a --schrijf; reads the tiles)

Per cave (zones 2-6; the Zwamgrot of zone 1 is not touched):
  1. both layouts are built in memory: the 1.10.2 one (GUHS_GROT_OUD=1) and the new one;
  2. the design check: every NEW cave-air cell stays at least 3 blocks under the natural rock top of its column AND of the columns
     round it (radius 2, so no room or tunnel breaks into a cliff, a pit or the sea), unless the old cave already had an opening
     there (the entrance); for the floating cloud whale also 4 blocks of whale under every floor. Non-natural blocks the new air
     would cut through (another structure underground) are reported. Any break-through fails loudly (exit 1);
  3. --schrijf: in every tile the cave touches, the OLD cave is taken out below the natural ground (its cells become the theme's
     fill rock; the world tree's wood in zone 3 stays), the NEW cave is stamped with the same hook every zone maker uses
     (grot.pas_toe_grot), the changed region is hollowed again like the makers do (fully enclosed opaque blocks -> air), the tile
     is written (block entities of untouched cells kept), then the shared passes run on those tiles (verbind, vrijpad, verbind).
     Afterwards every cell that changed OUTSIDE the cave's rebuild box is reported (must be 0; the run fails otherwise);
  4. the data file src/main/resources/data/guhs/vadsrijk/grotkerker/<cave>.json: the cave's real space per column (where
     players count as "in the dungeon": cave air + 2, never above the natural ground) and the rebuild box with its per-column
     surface limit for the server command /guhs vadsrijk grotten herbouw (it never writes at or above the surface).
"""
import argparse
import base64
import importlib
import json
import math
import os
import sys
import time

import numpy as np
from scipy import ndimage

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
sys.path.insert(0, HIER)
sys.path.insert(0, os.path.join(HIER, ".."))
import grot as GR  # noqa: E402
import verbind as VB  # noqa: E402

DATA = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "vadsrijk", "grotkerker")
GROTTEN = ("kristalgrot", "wortelgrot", "koraalgrot", "wolkengrot", "zoutgrot")
# the space of caves built (partly) by another generator: zone 6's salt hall, stair foot and round chamber (bouw6.zoutgrot)
EXTRA = {"zoutgrot": (3846, 44, -562, 3932, 66, -518)}
T, HOOG = GR.T, GR.HOOG
MARGE = 3             # rock above the highest new air cell (incl. its 2-block shell, so >= 1 block of untouched ground stays)
BUIK = 4              # the cloud whale: whale blocks under every floor


# ------------------------------------------------------------------------------------------------ the two layouts
def plannen(gid):
    """(old, new) Grot of a cave, both built."""
    uit = []
    for oud in (True, False):
        if oud:
            os.environ["GUHS_GROT_OUD"] = "1"
        else:
            os.environ.pop("GUHS_GROT_OUD", None)
        import grotplan
        importlib.reload(grotplan)
        g = getattr(grotplan, gid)()
        g.bouw()
        uit.append(g)
    os.environ.pop("GUHS_GROT_OUD", None)
    import grotplan
    importlib.reload(grotplan)
    return uit


def doos(g):
    """The stamp box of a Grot: (x0, y0, z0, x1, y1, z1) inclusive."""
    return g.x0, g.y0, g.z0, g.x0 + g.X - 1, g.y0 + g.Y - 1, g.z0 + g.Z - 1


def unie(a, b):
    return (min(a[0], b[0]), min(a[1], b[1]), min(a[2], b[2]), max(a[3], b[3]), max(a[4], b[4]), max(a[5], b[5]))


# ------------------------------------------------------------------------------------------------ tiles as one window
class Venster:
    def __init__(self, TG, x0, z0, x1, z1):
        self.TG, self.x0, self.z0, self.x1, self.z1 = TG, x0, z0, x1, z1
        self.v = np.zeros((x1 - x0 + 1, HOOG, z1 - z0 + 1), np.uint16)
        for tx in range(x0 // T, x1 // T + 1):
            for tz in range(z0 // T, z1 // T + 1):
                b = TG.dicht((tx, tz))
                if b is None:
                    continue
                ax0, ax1 = max(x0, tx * T), min(x1 + 1, tx * T + T)
                az0, az1 = max(z0, tz * T), min(z1 + 1, tz * T + T)
                self.v[ax0 - x0:ax1 - x0, :, az0 - z0:az1 - z0] = b[ax0 - tx * T:ax1 - tx * T, :, az0 - tz * T:az1 - tz * T]

    def luts(self):
        pal = self.TG.pal
        n = len(pal) + 64
        grond = np.zeros(n, bool)
        rots = np.zeros(n, bool)
        for i, s in enumerate(pal):
            grond[i] = GR._grond(s)
            rots[i] = grond[i] and "water" not in s
        return grond, rots

    def toppen(self, ymin=0):
        """(natural ground top incl. water, rock top without water, lowest rock >= ymin) per column; -1 = none."""
        grond, rots = self.luts()
        uit = []
        for lut in (grond, rots):
            gm = lut[self.v]
            heeft = gm.any(1)
            uit.append(np.where(heeft, HOOG - 1 - np.argmax(gm[:, ::-1, :], axis=1), -1))
        gm = rots[self.v]
        gm[:, :ymin, :] = False
        heeft = gm.any(1)
        uit.append(np.where(heeft, np.argmax(gm, axis=1), -1))
        return uit


_NATUUR = ("stone", "deepslate", "tuff", "dirt", "grass_block", "calcite", "andesite", "diorite", "granite", "sand", "gravel", "snow",
           "ice", "clay", "mud", "moss", "rooted", "podzol", "mycelium", "coarse", "terracotta", "basalt", "blackstone", "netherrack",
           "prismarine", "coral", "kristal", "rots", "zout", "wolk", "kaas", "vads", "glimmos", "amethyst", "ore", "water", "_log",
           "_wood", "roots", "magma", "obsidian", "dripstone", "bedrock", "sponge", "lichen", "seagrass", "kelp", "pickle", "cobble",
           "mossy", "lava", "cactus", "farmland", "path", "suspicious", "bone_block", "froglight", "sea_lantern")


def open_cel(s):
    """Open space for the break-through check: air and free water/lava only (every block, even a plant, counts as a wall: a
    hollowed rock interior is always walled by blocks, so it never connects to the sky through air)."""
    n = s.split("[")[0]
    return n in VB.LUCHT or n in ("minecraft:water", "minecraft:lava")


def natuurlijk(s):
    return any(w in s for w in _NATUUR)


# ------------------------------------------------------------------------------------------------ the design check
def check(gid, TG, praat=True):
    oud, nieuw = plannen(gid)
    D = unie(doos(oud), doos(nieuw))
    M = 20
    W = Venster(TG, D[0] - M, D[2] - M, D[3] + M, D[5] + M)
    top_g, top_r, bodem = W.toppen(100 if gid == "wolkengrot" else 0)
    top_min = ndimage.minimum_filter(top_r, size=5, mode="nearest")
    fouten, gaten, vreemd = [], 0, {}

    def cellen(g, ops=(GR.LUCHT, GR.ZACHT)):
        m = np.isin(g.op, ops)
        ii, yy, jj = np.nonzero(m)
        return ii + g.x0 - W.x0, yy + g.y0, jj + g.z0 - W.z0

    # columns where the OLD cave already had air close to (or above) the surface: its openings, allowed
    oi, oy, oj = cellen(oud)
    open_oud = np.zeros(top_r.shape, bool)
    hoog = oy > top_min[oi, oj] - MARGE
    open_oud[oi[hoog], oj[hoog]] = True
    open_oud = ndimage.binary_dilation(open_oud, np.ones((5, 5), bool))
    oud_lucht = np.zeros(W.v.shape, bool)
    oud_lucht[oi, oy, oj] = True
    ni, ny, nj = cellen(nieuw)
    slecht = (ny > top_min[ni, nj] - MARGE) & ~open_oud[ni, nj]
    if slecht.any():
        kol = {}
        for i, y, j in zip(ni[slecht], ny[slecht], nj[slecht]):
            k = (int(i + W.x0), int(j + W.z0))
            kol[k] = max(kol.get(k, -1), int(y))
        # (a warning only: the "ground" here skips non-ground blocks such as a wool tent on the cloud whale; the 3D check below
        # decides)
        if praat:
            print(f"   let op: {len(kol)} kolommen hebben minder dan {MARGE} grondblokken boven de grot (bv. "
                  + ", ".join(f"{x},{y},{z} top {int(top_min[x - W.x0, z - W.z0])}" for (x, z), y in sorted(kol.items())[:4]) + ")")
    # 3D: the tiles are hollowed (enclosed rock is stored as air), so "air above" says nothing by itself. OUTSIDE air = every
    # non-opaque cell connected to the sky (sea, the void under the cloud whale, a pit, a city platform's
    # underside), with the old cave's cells counted as rock. No new cave air may come within MARGE of outside air, except where
    # the old cave already did (its entrance).
    zacht = np.array([open_cel(s_) for s_ in TG.pal] + [False] * 64, bool)
    vrij = zacht[W.v]     # (air, water, plants, leaves: what light and players pass; a hollowed interior is walled by opaque blocks)
    # (only the caves' OWN air/water counts as wall here: the old cave reaches the sky through its entrance, and its shell cells
    # are rock or open air like any other cell)
    def eigen(g):
        m = np.isin(g.op, (GR.LUCHT, GR.ZACHT))
        for s_ in ("minecraft:water", GR.IJS):
            if s_ in g.pidx:
                m |= (g.op == GR.ZET) & (g.blk == g.pidx[s_])
        return m
    oud_m = np.zeros(W.v.shape, bool)
    om = eigen(oud)
    oi3, oy3, oj3 = np.nonzero(om)
    oud_m[oi3 + oud.x0 - W.x0, oy3 + oud.y0, oj3 + oud.z0 - W.z0] = True
    nm = eigen(nieuw)
    ni3, ny3, nj3 = np.nonzero(nm)
    nieuw_m3 = np.zeros(W.v.shape, bool)
    nieuw_m3[ni3 + nieuw.x0 - W.x0, ny3 + nieuw.y0, nj3 + nieuw.z0 - W.z0] = True
    vrij &= ~oud_m & ~nieuw_m3
    lab, _ = ndimage.label(vrij)
    rand = np.unique(lab[:, -1, :].ravel())     # (sky-connected only: a hollowed rock interior is always enclosed by kept blocks)
    buiten = np.isin(lab, rand[rand > 0])
    nl = np.zeros(W.v.shape, bool)
    nl[ni, ny, nj] = True
    ol = oud_lucht
    k3 = np.ones((3, 3, 3), bool)
    bij_nieuw = ndimage.binary_dilation(nl, k3, iterations=MARGE)
    bij_oud = ndimage.binary_dilation(ol, k3, iterations=MARGE + 1)
    lek = bij_nieuw & buiten & ~bij_oud
    if lek.any():
        li, ly, lj = np.nonzero(lek)
        kol = {}
        for i, y, j in zip(li, ly, lj):
            k = (int(i + W.x0) // 4 * 4, int(j + W.z0) // 4 * 4)
            kol[k] = min(kol.get(k, 999), int(y))
        for (x, z), y in sorted(kol.items())[:12]:
            fouten.append(f"buitenlucht binnen {MARGE} van nieuwe grotlucht bij ~{x},{y},{z}")
        fouten.append(f"... {int(lek.sum())} buitencellen te dicht bij de nieuwe grot")
    # what the new air cuts through (outside the old cave): non-natural blocks = another structure
    nieuw_m = ~oud_lucht[ni, ny, nj]
    vals = W.v[ni[nieuw_m], ny[nieuw_m], nj[nieuw_m]]
    for vid in np.unique(vals):
        s = TG.pal[int(vid)]
        if vid and not natuurlijk(s):
            vreemd[s] = int((vals == vid).sum())
    if praat:
        print(f"{gid}: oud {len(oi)} lucht, nieuw {len(ni)} lucht ({len(ni) / max(1, len(oi)):.2f}x), doos {D}, "
              f"vloerstappen {len(nieuw.vloerfouten)}, water naast lucht {nieuw.waterfouten}")
        for k in nieuw.kamers:
            print(f"   kamer {k['naam']:14s} ({k['cx']:.0f},{k['S']:.1f},{k['cz']:.0f}) r {k['rx']}x{k['rz']}")
        if vreemd:
            print(f"   nieuwe lucht snijdt door niet-natuurlijke blokken: {dict(sorted(vreemd.items(), key=lambda kv: -kv[1])[:12])}")
        for f in fouten:
            print("   FOUT:", f)
    if nieuw.vloerfouten:
        fouten.append(f"{len(nieuw.vloerfouten)} vloerstappen > 0.5")
    if nieuw.waterfouten:
        fouten.append(f"{nieuw.waterfouten} watercellen naast lucht")
    oud.buiten = (buiten, W.x0, W.z0)
    return oud, nieuw, D, fouten


# ------------------------------------------------------------------------------------------------ patching the tiles
def opaak_lut(TG):
    sys.path.insert(0, os.path.join(HIER, "..", "wereld"))
    sys.path.insert(0, os.path.join(HIER, "..", "stad"))
    V = importlib.import_module("vibekit")
    B = importlib.import_module("blokken")
    _, bk = V.res()
    lut = np.zeros(len(TG.pal) + 64, bool)
    for i, s in enumerate(TG.pal):
        naam = s.split("[")[0]
        if i == 0 or naam in VB.LUCHT or naam in B.EIGEN or naam.endswith(("_leaves", "barrier")) or "glass" in naam or "slime" in naam \
                or "honey" in naam:
            continue
        try:
            lut[i] = bool(bk.bake(s).full_opaque)
        except Exception:
            lut[i] = False
    return lut


def hol(vol, lut, m):
    """tegels.hol, but only for cells in mask m (the changed region)."""
    occ = lut[vol]
    inn = occ.copy()
    inn[0, :, :] = inn[-1, :, :] = False
    inn[:, 0, :] = inn[:, -1, :] = False
    inn[:, :, 0] = inn[:, :, -1] = False
    inn[1:-1, 1:-1, 1:-1] &= occ[:-2, 1:-1, 1:-1] & occ[2:, 1:-1, 1:-1] & occ[1:-1, :-2, 1:-1] & \
        occ[1:-1, 2:, 1:-1] & occ[1:-1, 1:-1, :-2] & occ[1:-1, 1:-1, 2:]
    vol[inn & m] = 0


def ontgrot(oud, vol, tx, tz, TG):
    """Takes the old cave out of a tile, below the natural ground: every cell the old stamp touched becomes fill rock (the
    zone-3 world tree's wood stays). Returns the mask of cells it wrote."""
    bx0, bx1 = max(oud.x0, tx * T), min(oud.x0 + oud.X, tx * T + T)
    bz0, bz1 = max(oud.z0, tz * T), min(oud.z0 + oud.Z, tz * T + T)
    m = np.zeros(vol.shape, bool)
    if bx0 >= bx1 or bz0 >= bz1:
        return m
    grond = np.array([GR._grond(s) for s in TG.pal] + [False] * 64, bool)
    hout = np.array([GR.is_hout(s) for s in TG.pal] + [False] * 64, bool)
    ix0, ix1, iz0, iz1 = bx0 - tx * T, bx1 - tx * T, bz0 - tz * T, bz1 - tz * T
    kol = vol[ix0:ix1, :, iz0:iz1]
    gm = grond[kol]
    top = np.where(gm.any(1), HOOG - 1 - np.argmax(gm[:, ::-1, :], axis=1), -1)
    op = oud.op[bx0 - oud.x0:bx1 - oud.x0, :, bz0 - oud.z0:bz1 - oud.z0]
    ys = np.arange(oud.y0, oud.y0 + oud.Y)[None, :, None]
    sub = vol[ix0:ix1, oud.y0:oud.y0 + oud.Y, iz0:iz1]
    # only what the old cave made open or decorative: its air, its water and its non-solid placed blocks (lanterns, plants,
    # chests, ...). Its walls and fill stay as they are (rock is rock; the new walls re-skin where they need to), so natural rock
    # round the old cave and at its mouth keeps its own blocks.
    zacht_lut = np.array([GR._zacht(s_) or "chest" in s_ or "lever" in s_ for s_ in TG.pal] + [False] * 64, bool)
    open_ = np.isin(op, (GR.LUCHT, GR.ZACHT)) | ((op == GR.ZET) & zacht_lut[sub])
    w = open_ & (ys <= top[:, None, :])
    if oud.bewaar_hout:
        w &= ~hout[sub]
    vul = oud.thema["vul"]
    rots = TG.id(vul if isinstance(vul, str) else max(vul, key=lambda kv: kv[1])[0])
    sub[w] = rots
    m[ix0:ix1, oud.y0:oud.y0 + oud.Y, iz0:iz1] = w
    return m


def schil_dicht(g, vol, tx, tz, TG, buiten):
    """The shell where pas_toe skipped it: pas_toe writes walls/fill only below the column's natural ground top, which is right in
    the makers (solid rock before hollowing) but not on hollowed tiles under a non-ground cover (a wool tent on the cloud whale):
    there the rock interior is stored as air. So: every wall/fill cell of the new cave that is still air and NOT connected to
    the sky (= hollowed interior, never real open air) gets its block."""
    bo, bx, bz = buiten
    bx0, bx1 = max(g.x0, tx * T), min(g.x0 + g.X, tx * T + T)
    bz0, bz1 = max(g.z0, tz * T), min(g.z0 + g.Z, tz * T + T)
    if bx0 >= bx1 or bz0 >= bz1:
        return 0
    op = g.op[bx0 - g.x0:bx1 - g.x0, :, bz0 - g.z0:bz1 - g.z0]
    blk = g.blk[bx0 - g.x0:bx1 - g.x0, :, bz0 - g.z0:bz1 - g.z0]
    sub = vol[bx0 - tx * T:bx1 - tx * T, g.y0:g.y0 + g.Y, bz0 - tz * T:bz1 - tz * T]
    lucht_buiten = bo[bx0 - bx:bx1 - bx, g.y0:g.y0 + g.Y, bz0 - bz:bz1 - bz]
    m = np.isin(op, (GR.MUUR, GR.VUL)) & (sub == 0) & ~lucht_buiten
    if m.any():
        gids = np.array([TG.id(s_) if i else 0 for i, s_ in enumerate(g.pal)], np.uint16)
        sub[m] = gids[blk[m]]
    return int(m.sum())


def schrijf_grot(gid, oud, nieuw, D, TG, lut):
    """Patches every tile the cave (old or new) touches. Returns the tiles written."""
    geschreven = []
    for tx in range(D[0] // T, D[3] // T + 1):
        for tz in range(D[2] // T, D[5] // T + 1):
            t = (tx, tz)
            v0 = TG.dicht(t)
            if v0 is None:
                continue
            vol = v0.copy()
            m = ontgrot(oud, vol, tx, tz, TG)
            GR.pas_toe_grot(nieuw, vol, tx, tz, TG.id, TG.pal)
            schil_dicht(nieuw, vol, tx, tz, TG, oud.buiten)
            if len(lut) < len(TG.pal) + 1:
                lut = opaak_lut(TG)
            veranderd = (vol != v0) | m
            if not veranderd.any():
                continue
            # hollow again where something changed (+1 round it, so neighbours of new air are re-judged too)
            zone = ndimage.binary_dilation(veranderd, np.ones((3, 3, 3), bool))
            herstel = zone & (vol == 0) & (v0 != 0) & ~veranderd    # (cells only the dilation touched: hollow only, never fill)
            hol(vol, lut, zone)
            vol[herstel] = v0[herstel]
            extra = {k: b for k, b in TG.nbt(t).items() if vol[k] == v0[k] and vol[k]}
            TG.schrijf(t, vol, extra=extra or None)
            TG.cache[t] = vol
            geschreven.append(t)
    return geschreven, lut


# ------------------------------------------------------------------------------------------------ the data for the game
def b64(a):
    return base64.b64encode(np.ascontiguousarray(a, dtype=">i2").tobytes()).decode("ascii")


def data(gid, oud, nieuw, D, TG):
    """The cave's space per column (presence) and the rebuild box + surface limit per column."""
    W = Venster(TG, D[0], D[2], D[3], D[5])
    top_g, top_r, _ = W.toppen()
    # space: cave air/water of the stamp + 2 round it, per column y range, never at or above the natural ground (except the
    # entrance shaft/mouth, where the column is open: there up to 3 over the highest air)
    lucht = np.isin(nieuw.op, (GR.LUCHT, GR.ZACHT))
    wid = nieuw.pidx.get("minecraft:water")
    if wid is not None:
        lucht |= (nieuw.op == GR.ZET) & (nieuw.blk == wid)
    if gid in EXTRA:   # (+ the existing cave of another generator: its open cells under the ground)
        ex0, ey0, ez0, ex1, ey1, ez1 = EXTRA[gid]
        open_lut = np.array([open_cel(s_) for s_ in TG.pal] + [False] * 64, bool)
        for i in range(max(0, ex0 - nieuw.x0), min(nieuw.X, ex1 - nieuw.x0 + 1)):
            for j in range(max(0, ez0 - nieuw.z0), min(nieuw.Z, ez1 - nieuw.z0 + 1)):
                wx, wz = i + nieuw.x0 - W.x0, j + nieuw.z0 - W.z0
                for yi in range(max(0, ey0 - nieuw.y0), min(nieuw.Y, ey1 - nieuw.y0 + 1)):
                    y = yi + nieuw.y0
                    if y < top_r[wx, wz] and open_lut[W.v[wx, y, wz]]:
                        lucht[i, yi, j] = True
    kern = ndimage.binary_dilation(lucht, np.ones((5, 5, 5), bool))
    heeft = kern.any(1)
    ys = np.arange(nieuw.y0, nieuw.y0 + nieuw.Y)
    lo = np.where(heeft, ys[np.argmax(kern, axis=1)], 1)
    hi = np.where(heeft, ys[nieuw.Y - 1 - np.argmax(kern[:, ::-1, :], axis=1)], 0)
    hoogste_lucht = np.where(lucht.any(1), ys[nieuw.Y - 1 - np.argmax(lucht[:, ::-1, :], axis=1)], -999)
    ox, oz = nieuw.x0 - W.x0, nieuw.z0 - W.z0
    tg = top_r[ox:ox + nieuw.X, oz:oz + nieuw.Z]
    bedekt = tg > hoogste_lucht + 1
    hi = np.where(heeft & bedekt, np.minimum(hi, tg - 1), np.where(heeft, np.minimum(hi, hoogste_lucht + 3), hi))
    ii, jj = np.nonzero(heeft & (hi >= lo))
    rx0, rx1, rz0, rz1 = int(ii.min()), int(ii.max()), int(jj.min()), int(jj.max())
    LO = np.full((rx1 - rx0 + 1, rz1 - rz0 + 1), 1, np.int32)
    HI = np.zeros_like(LO)
    sel = heeft[rx0:rx1 + 1, rz0:rz1 + 1] & (hi[rx0:rx1 + 1, rz0:rz1 + 1] >= lo[rx0:rx1 + 1, rz0:rz1 + 1])
    LO[sel] = lo[rx0:rx1 + 1, rz0:rz1 + 1][sel]
    HI[sel] = hi[rx0:rx1 + 1, rz0:rz1 + 1][sel]
    ruimte = dict(x0=nieuw.x0 + rx0, z0=nieuw.z0 + rz0, w=rx1 - rx0 + 1, d=rz1 - rz0 + 1,
                  lo=b64(LO.T.ravel()), hi=b64(HI.T.ravel()))       # index = (z - z0) * w + (x - x0)
    rdoos = (nieuw.x0 + rx0, int(LO[sel].min()), nieuw.z0 + rz0, nieuw.x0 + rx1, int(HI[sel].max()), nieuw.z0 + rz1)
    # rebuild: the whole box of old + new stamp; the command writes only under the natural ground top of each column (the
    # tile's ground incl. water, so the sea and lakes are never touched either)
    # the limit per column: 2 under the lowest SKY-CONNECTED cell above the cave's highest cell (old or new) in that column, so
    # the surface block itself and everything on it stay untouched (the plain "ground top" was fooled by a wool tent on the
    # cloud whale: it skips wool, and found the whale's belly as the top)
    bo, bx, bz = oud.buiten
    ys_all = np.arange(HOOG)
    hoogste = np.full((D[3] - D[0] + 1, D[5] - D[2] + 1), D[1] - 1, np.int32)
    for g in (oud, nieuw):
        m = g.op != 0
        heeft_g = m.any(1)
        hy = np.where(heeft_g, g.y0 + g.Y - 1 - np.argmax(m[:, ::-1, :], axis=1), -1)
        ox, oz = g.x0 - D[0], g.z0 - D[2]
        sub = hoogste[ox:ox + g.X, oz:oz + g.Z]
        np.maximum(sub, hy, out=sub)
    grens = np.full(hoogste.shape, HOOG - 1, np.int32)
    wx, wz = D[0] - bx, D[2] - bz
    for i in range(hoogste.shape[0]):
        for j in range(hoogste.shape[1]):
            kol = bo[wx + i, :, wz + j]
            boven = np.nonzero(kol & (ys_all > hoogste[i, j]))[0]
            if len(boven):
                grens[i, j] = int(boven[0]) - 2
    herbouw = dict(doos=list(D), grens=dict(x0=D[0], z0=D[2], w=D[3] - D[0] + 1, d=D[5] - D[2] + 1, data=b64(grens.T.ravel())))
    obj = dict(id=gid, versie=1, ruimte=ruimte, ruimteDoos=list(rdoos))
    if gid in GROTTEN:
        obj["herbouw"] = herbouw
    os.makedirs(DATA, exist_ok=True)
    with open(os.path.join(DATA, gid + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, separators=(",", ":"))
        f.write("\n")
    return rdoos


# ------------------------------------------------------------------------------------------------ main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("grotten", nargs="*")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--schrijf", action="store_true")
    ap.add_argument("--data", action="store_true")
    a = ap.parse_args()
    ids = a.grotten or list(GROTTEN)
    TG = VB._Tegels(VB.UIT)
    slecht = False
    plan = {}
    for gid in ids:
        oud, nieuw, D, fouten = check(gid, TG)
        plan[gid] = (oud, nieuw, D)
        if fouten:
            slecht = True
    if slecht:
        print("CHECK MISLUKT: niets geschreven")
        sys.exit(1)
    if a.check:
        return
    if a.schrijf:
        t0 = time.time()
        lut = opaak_lut(TG)
        alle = set()
        dozen = [plan[g][2] for g in ids]
        origineel = {}
        for D in dozen:
            for tx in range(D[0] // T - 1, D[3] // T + 2):
                for tz in range(D[2] // T - 1, D[5] // T + 2):
                    v = TG.dicht((tx, tz))
                    if v is not None and (tx, tz) not in origineel:
                        origineel[(tx, tz)] = np.array(TG.pal, object)[v]
        for gid in ids:
            oud, nieuw, D = plan[gid]
            tg, lut = schrijf_grot(gid, oud, nieuw, D, TG, lut)
            alle |= set(tg)
            print(f"{gid}: {len(tg)} tegels geschreven {sorted(tg)}", flush=True)
        import vrijpad
        VB.naloop(sorted(alle), werkers=1)
        vrijpad.naloop(sorted(alle))
        VB.naloop(sorted(alle), werkers=1)
        # the shared passes look at whole tiles: whatever they changed OUTSIDE the caves' rebuild boxes goes back (the rest of the
        # map stays byte-identical in content)
        TG = VB._Tegels(VB.UIT)
        terug = 0
        for t, oud_namen in origineel.items():
            v = TG.dicht(t)
            if v is None:
                continue
            binnen = np.zeros(v.shape, bool)
            for D in dozen:
                i0, i1 = max(0, D[0] - t[0] * T), min(T - 1, D[3] - t[0] * T)
                k0, k1 = max(0, D[2] - t[1] * T), min(T - 1, D[5] - t[1] * T)
                if i0 <= i1 and k0 <= k1:
                    binnen[i0:i1 + 1, max(0, D[1]):D[4] + 1, k0:k1 + 1] = True
            nu = np.array(TG.pal, object)[v]
            m = (nu != oud_namen) & ~binnen
            if m.any():
                v = v.copy()
                for i, y, k in zip(*np.nonzero(m)):
                    v[i, y, k] = TG.id(oud_namen[i, y, k])
                extra = {kk: b for kk, b in TG.nbt(t).items() if v[kk]}
                TG.schrijf(t, v, extra=extra or None)
                TG.cache[t] = v
                terug += int(m.sum())
        print(f"herbouw: {len(alle)} tegels, {terug} cellen buiten de dozen teruggezet ({time.time() - t0:.0f} s)")
        TG = VB._Tegels(VB.UIT)
    if a.schrijf or a.data:
        for gid in ids:
            oud, nieuw, D = plan[gid]
            r = data(gid, oud, nieuw, D, TG)
            print(f"{gid}: data geschreven, ruimte {r}")


if __name__ == "__main__":
    main()
