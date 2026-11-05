"""Walkability checker for the Vadsrijk map tiles (FIX_BRIEF walkability standard; reusable by the integrator).

Run from the project root (after maak_kaart.py):
    python tools/vadsrijk/loopcheck.py                       # whole map
    python tools/vadsrijk/loopcheck.py --box 250,-560,420,-360   # only this x0,z0,x1,z1 box (world coords)
    python tools/vadsrijk/loopcheck.py --json uit.json       # also write all findings as json

It reads the generated tiles (src/main/resources/data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt), turns every block into a
movement class and builds the survival movement graph of a player (walk, step up 1/2 block, jump up 1 block, fall any depth,
climb ladders/vines/water). Findings:
  VAL    (trap)  : places you can reach by walking/falling from the edge of the checked window but never leave again
                   (pits, holes, rooms, water without a way out). One line per trapped region (centre, size, y range).
  TRAP   (stair) : a reachable stair whose upper side leads nowhere (air gap, wall) or is blocked by a fence/wall;
  TRAP-  (stair) : a reachable stair whose lower side has no floor (a step > 1 block down / a gap).
  PAD    (path)  : a dirt-path block with a solid block (trunk, leaves, cap, stem) in the 2 cells above it.
The movement model is deliberately simple (4 directions, slabs/stairs = half blocks, fences = 1.5 high, doors/gates/trapdoors
open, lanterns/plants/carpets passable). Windows of 4x4 tiles with a 1-tile margin; a trap is reported by the window whose core
holds its centre.
"""
import argparse
import glob
import gzip
import json
import os
import re
import struct
import sys
import time

import numpy as np
from scipy import ndimage
from scipy.sparse import coo_matrix
from scipy.sparse.csgraph import breadth_first_order

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
T = 64
Y0, Y1 = 16, 272          # checked height range

PASS, SOLID, HALF, FENCE, CLIMB = 0, 1, 2, 3, 4
# extra flag classes (still movement-equal to the above)
STAIR = 5                 # a stair block (HALF for movement); facing kept separately
DIRT_PATH = 6             # SOLID for movement, marks a path surface

_PASS_WOORDEN = ("structure_void", "short_grass", "tall_grass", "fern", "tulip", "allium", "daisy",
                 "cornflower", "bluet", "petals", "carpet", "torch", "button", "sign", "pressure_plate", "rail",
                 "sapling", "lily_pad", "riet", "guhbloem", "guhoortjes", "litteken_plant", "hanging_moss", "firefly_bush",
                 "bush", "portal", "cobweb", "pointed_dripstone", "waterlelie", "lantern[", "lantern", "chain", "bell",
                 "door", "_deur", "fence_gate", "trapdoor", "flower", "dandelion", "poppy", "orchid", "lilac", "rose", "peony",
                 "knuffel_", "redstone_wire", "lever", "amethyst_cluster", "kristal_cluster", "weidekristalletjes", "candle",
                 "lava", "snow[", "glow_lichen", "sculk_vein", "seagrass", "kelp", "head", "skull", "banner",
                 "item_frame", "flower_pot", "potted", "cave_air", "spore_blossom", "moss_carpet", "lightning_rod",
                 "end_rod", "tripwire", "dead_bush")
_PASS_PRECIES = {"minecraft:light", "minecraft:void_air", "guhs:vr_w_portaal", "minecraft:red_mushroom", "minecraft:brown_mushroom", "guhs:glimpaddenstoel", "guhs:schimmelpaddenstoel",
                 "minecraft:air", "minecraft:cave_air", "guhs:vr_w_litteken_plant"}


def klasse(naam, props):
    kort = naam.split(":", 1)[1]
    if naam in ("minecraft:water", "minecraft:bubble_column") or kort in ("ladder", "scaffolding") or kort.endswith("vine") \
            or kort.endswith("vines") or kort.endswith("vines_plant") or props.get("waterlogged") == "true" and kort.endswith("slab") is False and kort in ("seagrass", "kelp", "kelp_plant"):
        return CLIMB
    if kort.endswith(("leaves", "bladeren", "_log", "_wood", "_hoed", "_steel")) or "light" in kort and kort != "light":
        return SOLID
    if kort in ("sea_lantern", "redstone_lamp") or kort.startswith("weidekristal_"):   # full blocks despite "lantern"/"kristal"
        return SOLID
    if kort.endswith("_slab") or kort.endswith("_plaat"):
        t = props.get("type", "bottom")
        return HALF if t == "bottom" else SOLID
    if kort.endswith("stairs") or kort.endswith("_trap"):
        return STAIR
    if kort in ("campfire", "soul_campfire") or kort.endswith("_bed") or kort == "daylight_detector" or kort.startswith("snow"):
        return HALF
    if kort.endswith("fence_gate"):
        return PASS
    if kort.endswith("fence") or kort.endswith("_wall") and "wall_" not in kort or kort.endswith("_muur") \
            or "lantaarnpaal" in kort or kort in ("iron_bars",) or kort.endswith("pane"):
        return FENCE
    if naam in _PASS_PRECIES or "paddenstoel" in kort and not kort.endswith(("_hoed", "_steel", "_blok")):
        return PASS
    if kort == "dirt_path":
        return DIRT_PATH
    if kort.endswith("grass_block") or kort in ("knuffelgras", "weide_roze_gras"):
        return SOLID
    for w in _PASS_WOORDEN:
        if w in kort:
            return PASS
    return SOLID


_REC = np.dtype([("t_pos", "u1"), ("n_pos", ">u2"), ("s_pos", "S3"), ("et", "u1"), ("cnt", ">i4"),
                 ("x", ">i4"), ("y", ">i4"), ("z", ">i4"),
                 ("t_st", "u1"), ("n_st", ">u2"), ("s_st", "S5"), ("st", ">i4"), ("end", "u1")])


def lees_tegel(pad):
    """(palette [(name, props)], x, y, z, state) of a tile written by nbtsnel (fixed 36-byte block records)."""
    data = gzip.decompress(open(pad, "rb").read())
    i = data.index(b"palette") + len(b"palette")
    assert data[i] == 10
    n = struct.unpack_from(">i", data, i + 1)[0]
    p = i + 5
    pal = []

    def rstr(p):
        k = struct.unpack_from(">H", data, p)[0]
        return data[p + 2:p + 2 + k].decode(), p + 2 + k
    for _ in range(n):
        naam, props = None, {}
        while True:
            t = data[p]
            p += 1
            if t == 0:
                break
            k, p = rstr(p)
            if t == 8:
                v, p = rstr(p)
                if k == "Name":
                    naam = v
            elif t == 10:
                while True:
                    t2 = data[p]
                    p += 1
                    if t2 == 0:
                        break
                    k2, p = rstr(p)
                    v2, p = rstr(p)
                    props[k2] = v2
            else:
                raise ValueError(t)
        pal.append((naam, props))
    assert data[p:p + 9] == b"\x09\x00\x06blocks", data[p:p + 9]
    p += 9
    assert data[p] == 10
    nb = struct.unpack_from(">i", data, p + 1)[0]
    p += 5
    a = np.frombuffer(data, _REC, nb, p)
    return pal, a["x"].astype(np.int32), a["y"].astype(np.int32), a["z"].astype(np.int32), a["st"].astype(np.int32)


FACING = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}


class Venster:
    """Class volume of a box of tiles: k[x, y - Y0, z] + stair facing f[x, y, z] (0 none, 1..4 n/s/e/w)."""

    def __init__(self, tx0, tz0, tx1, tz1, map_=TEGELS):
        self.tx0, self.tz0 = tx0, tz0
        NX, NZ = (tx1 - tx0 + 1) * T, (tz1 - tz0 + 1) * T
        self.x0, self.z0 = tx0 * T, tz0 * T
        self.k = np.zeros((NX, Y1 - Y0, NZ), np.uint8)
        self.f = np.zeros((NX, Y1 - Y0, NZ), np.uint8)
        self.leeg = True
        for tx in range(tx0, tx1 + 1):
            for tz in range(tz0, tz1 + 1):
                pad = os.path.join(map_, f"t_{tx}_{tz}.nbt")
                if not os.path.exists(pad):
                    continue
                self.leeg = False
                pal, x, y, z, st = lees_tegel(pad)
                kl = np.array([klasse(n, p) for n, p in pal], np.uint8)
                fc = np.array([("north", "south", "east", "west").index(p["facing"]) + 1
                               if klasse(n, p) == STAIR and p.get("facing") in FACING else 0 for n, p in pal], np.uint8)
                m = (y >= Y0) & (y < Y1)
                ox, oz = (tx - tx0) * T, (tz - tz0) * T
                self.k[x[m] + ox, y[m] - Y0, z[m] + oz] = kl[st[m]]
                self.f[x[m] + ox, y[m] - Y0, z[m] + oz] = fc[st[m]]


SEEDS = None     # [p-wereld] optional: [[x, y, z], ...] stand spots that count as "safe" instead of the window ring
LINKS = []       # [p-wereld] optional: [[[x, y, z], [x, y, z], both], ...] lifts (both) / wind columns (one way) as extra edges
POIS = None      # [p-wereld] optional: {id: [x, y, z]}: report which are not reachable / not able to get back


def analyse(V, kern, route=None, soepel=None):
    """kern = (x0, z0, x1, z1) world box whose findings count. Returns a list of findings (dicts)."""
    k = V.k
    NX, NY, NZ = k.shape
    P = (k == PASS) | (k == CLIMB)
    SUP = (k == SOLID) | (k == DIRT_PATH)
    HALFB = (k == HALF) | (k == STAIR)
    FEN = k == FENCE
    CL = k == CLIMB

    def up(a, n=1):      # a shifted so that out[y] = a[y + n]
        o = np.zeros_like(a)
        o[:, :NY - n, :] = a[:, n:, :]
        return o

    def dn(a, n=1):      # out[y] = a[y - n]
        o = np.zeros_like(a)
        o[:, n:, :] = a[:, :NY - n, :]
        return o
    Pu1, Pu2 = up(P), up(P, 2)
    full = dn(SUP) & P & Pu1
    half = (HALFB | (dn(FEN) & P)) & Pu1 & Pu2
    klim = CL & (Pu1 | up(CL))
    node = full | half | klim
    lvl2 = np.zeros(k.shape, np.int16)      # level in half blocks (relative): 2y or 2y+1
    yy = np.arange(NY, dtype=np.int16)[None, :, None]
    lvl2 = 2 * yy + half.astype(np.int16)
    # land[x,y,z]: entering this column with the feet in cell y, where do you end up (node cell) ?
    land = np.full(k.shape, -1, np.int16)
    prev = np.full((NX, NZ), -1, np.int16)
    for y in range(NY):
        cur = np.where(node[:, y, :], y, np.where(P[:, y, :], prev, -1)).astype(np.int16)
        land[:, y, :] = cur
        prev = cur
    ids = np.full(k.shape, -1, np.int32)
    nx_, ny_, nz_ = np.nonzero(node)
    N = len(nx_)
    ids[nx_, ny_, nz_] = np.arange(N, dtype=np.int32)
    L = lvl2[nx_, ny_, nz_]
    bron, doel = [], []
    hoog_b, hoog_t, hoog_d = [], [], []
    leeg_b, leeg_k = [], []
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        bx, bz = nx_ + dx, nz_ + dz
        ok = (bx >= 0) & (bx < NX) & (bz >= 0) & (bz < NZ)
        s, x2, y2, z2 = np.nonzero(ok)[0], bx[ok], ny_[ok], bz[ok]
        Ls = L[s]
        # (a) same cell / one cell up in the neighbour column
        for dc in (0, 1, 2, 3, 4, 5, 6):
            yc = y2 + dc
            m = yc < NY
            s1, xx, yc1, zz = s[m], x2[m], yc[m], z2[m]
            t = ids[xx, yc1, zz]
            m = t >= 0
            s1, xx, yc1, zz, t = s1[m], xx[m], yc1[m], zz[m], t[m]
            Lt = lvl2[xx, yc1, zz]
            d = Lt - L[s1]
            hoofd = np.minimum(Lt // 2 + 1, NY - 1)
            m = (d <= 2) & (d >= -1) & P[nx_[s1], hoofd, nz_[s1]]
            hoog_b.append(s1[d > 2])
            hoog_t.append(t[d > 2])
            hoog_d.append(d[d > 2])
            bron.append(s1[m])
            doel.append(t[m])
        # (b) walk off into the neighbour column and fall (or stay level)
        yb = y2
        m = (yb + 1 < NY) & P[x2, yb, z2] & P[x2, np.minimum(yb + 1, NY - 1), z2]
        s1, xx, yb1, zz = s[m], x2[m], yb[m], z2[m]
        lt = land[xx, yb1, zz]
        m = lt >= 0
        t = ids[xx[m], lt[m], zz[m]]
        bron.append(s1[m])
        doel.append(t)
        # falling with nothing below at all = out of the map (under the terrain skin)
        leeg = (lt < 0) & ~(~P[xx, :, zz] & (np.arange(NY)[None, :] < yb1[:, None])).any(1)
        leeg_b.append(s1[leeg])
        leeg_k.append(np.stack([xx[leeg], yb1[leeg], zz[leeg]], 1))
    # vertical: climbing up/down inside a climbable column
    kl = np.nonzero(klim[nx_, ny_, nz_])[0]
    for dy in (1, -1):
        yc = ny_[kl] + dy
        m = (yc >= 0) & (yc < NY)
        t = ids[nx_[kl][m], yc[m], nz_[kl][m]]
        mm = t >= 0
        bron.append(kl[m][mm])
        doel.append(t[mm])
    bron = np.concatenate(bron)
    doel_ = np.concatenate(doel)
    if soepel is not None:
        # smooth walking only: every move changes the stand height by at most half a block (no jumps, no drops)
        dl = np.abs(L[doel_].astype(np.int32) - L[bron].astype(np.int32))
        m = dl <= 1
        Gs = coo_matrix((np.ones(int(m.sum()), np.int8), (bron[m], doel_[m])), shape=(N, N)).tocsr()

        def nid(p):
            if not (0 <= p[0] - V.x0 < NX and 2 <= p[1] - Y0 < NY - 2 and 0 <= p[2] - V.z0 < NZ):
                return -2
            i_ = ids[p[0] - V.x0, p[1] - Y0, p[2] - V.z0]
            if i_ < 0:      # nearest stand position in the column (within 2)
                for dy in (1, -1, 2, -2):
                    i_ = ids[p[0] - V.x0, p[1] - Y0 + dy, p[2] - V.z0]
                    if i_ >= 0:
                        break
            return int(i_)
        van, naar = nid(soepel[0]), nid(soepel[1])
        if van == -2 or naar == -2:
            pass
        elif van < 0 or naar < 0:
            print(f"soepel: geen staplek bij {soepel[0] if van < 0 else soepel[1]}")
        else:
            orde_s = breadth_first_order(Gs, van, directed=True, return_predecessors=False)
            ok = naar in set(orde_s.tolist())
            hoogste = max(orde_s, key=lambda i_: L[i_])
            print(f"soepel {soepel[0]} -> {soepel[1]}: {'JA, zonder springen' if ok else 'NEE'}; "
                  f"hoogste soepel bereikt: {[int(nx_[hoogste]) + V.x0, int(ny_[hoogste]) + Y0, int(nz_[hoogste]) + V.z0]}")
    doel = np.concatenate(doel)
    # exits: the stand position of every plain-ground column in the outer ring (2 wide) of the window
    ring = np.zeros((NX, NZ), bool)
    ring[:2, :] = ring[-2:, :] = ring[:, :2] = ring[:, -2:] = True
    topc = np.where(node.any(1), NY - 1 - np.argmax(node[:, ::-1, :], axis=1), -1)
    # only plain ground counts as a way out: ring columns with exactly one stand position (no roofs, caps, tree tops)
    #   (one run of stand positions: plain ground, or a water column you can swim up and down)
    runs = (node & ~dn(node)).sum(1)
    rx, rz = np.nonzero(ring & (topc >= 0) & (runs == 1))
    uit = ids[rx, topc[rx, rz], rz]

    def nid2(p):
        if not (0 <= p[0] - V.x0 < NX and 0 <= p[1] - Y0 < NY and 0 <= p[2] - V.z0 < NZ):
            return -2
        for dy in (0, 1, -1, 2, -2):
            yy_ = p[1] - Y0 + dy
            if 0 <= yy_ < NY:
                i_ = ids[p[0] - V.x0, yy_, p[2] - V.z0]
                if i_ >= 0:
                    return int(i_)
        return -1
    if SEEDS is not None:
        uit = np.array([i_ for i_ in (nid2(p) for p in SEEDS) if i_ >= 0], np.int64)
        lb, ld = [], []
        for (a_, b_, beide) in LINKS:
            ia, ib = nid2(a_), nid2(b_)
            if ia >= 0 and ib >= 0:
                lb.append(ia)
                ld.append(ib)
                if beide:
                    lb.append(ib)
                    ld.append(ia)
            else:
                print(f"  link zonder staplek: {a_} -> {b_} ({ia}, {ib})")
        bron = np.concatenate([bron, np.array(lb, np.int64)])
        doel = np.concatenate([doel, np.array(ld, np.int64)])
    S = N
    bron = np.concatenate([bron, np.full(len(uit), S), uit])
    doel = np.concatenate([doel, uit, np.full(len(uit), S)])
    G = coo_matrix((np.ones(len(bron), np.int8), (bron, doel)), shape=(N + 1, N + 1)).tocsr()
    F = np.zeros(N + 1, bool)
    orde, pred = breadth_first_order(G, S, directed=True, return_predecessors=True)
    F[orde] = True
    if route is not None:
        rx_, ry_, rz_ = route[0] - V.x0, route[1] - Y0, route[2] - V.z0
        doel_id = ids[rx_, ry_, rz_]
        print(f"route naar {route}: node {doel_id}")
        stappen = []
        while doel_id >= 0 and doel_id != S:
            stappen.append([int(nx_[doel_id]) + V.x0, int(ny_[doel_id]) + Y0, int(nz_[doel_id]) + V.z0, int(L[doel_id])])
            doel_id = pred[doel_id]
        for st in reversed(stappen):
            print("   ", st)
    R = np.zeros(N + 1, bool)
    R[breadth_first_order(G.T.tocsr(), S, directed=True, return_predecessors=False)] = True
    if POIS is not None:
        for k_, p in sorted(POIS.items()):
            i_ = nid2(p)
            if i_ == -2:
                continue
            if i_ < 0:
                print(f"POI   {k_:34s} {p}: geen staplek")
            elif not F[i_]:
                print(f"POI   {k_:34s} {p}: NIET BEREIKBAAR vanaf de veilige plekken")
            elif not R[i_]:
                print(f"POI   {k_:34s} {p}: bereikbaar maar je komt NIET terug (val)")
    val = F[:N] & ~R[:N]
    uitkomst = []
    kx0, kz0, kx1, kz1 = kern
    leeg_b = np.concatenate(leeg_b)
    leeg_k = np.concatenate(leeg_k)
    m = F[leeg_b]
    gezien = set()
    for (x, y, z) in leeg_k[m]:
        wx, wz = int(x) + V.x0, int(z) + V.z0
        if not (kx0 <= wx <= kx1 and kz0 <= wz <= kz1) or (wx // 4, wz // 4) in gezien:
            continue
        gezien.add((wx // 4, wz // 4))
        uitkomst.append(dict(soort="LEEG", x=wx, y=int(y) + Y0, z=wz, wat="je valt hier onder de kaart (geen bodem)"))
    hoog_b, hoog_t, hoog_d = np.concatenate(hoog_b), np.concatenate(hoog_t), np.concatenate(hoog_d)
    m = val[hoog_b] & R[hoog_t]
    hoog_b, hoog_t, hoog_d = hoog_b[m], hoog_t[m], hoog_d[m]
    if val.any():
        vm = np.zeros(k.shape, bool)
        vm[nx_[val], ny_[val], nz_[val]] = True
        lab, n = ndimage.label(vm, np.ones((3, 3, 3), bool))
        for i, sl in enumerate(ndimage.find_objects(lab)):
            pts = np.argwhere(lab[sl] == i + 1) + [s_.start for s_ in sl]
            c = pts.mean(0)
            wx, wz = int(c[0]) + V.x0, int(c[2]) + V.z0
            if not (kx0 <= wx <= kx1 and kz0 <= wz <= kz1):
                continue
            # the cheapest way out: the lowest too-high step from this region to a node that can escape
            lab_b = lab[nx_[hoog_b], ny_[hoog_b], nz_[hoog_b]] == i + 1
            uitweg = None
            if lab_b.any():
                j = np.nonzero(lab_b)[0][np.argmin(hoog_d[lab_b])]
                b_, t_ = hoog_b[j], hoog_t[j]
                uitweg = dict(van=[int(nx_[b_]) + V.x0, int(ny_[b_]) + Y0, int(nz_[b_]) + V.z0],
                              naar=[int(nx_[t_]) + V.x0, int(ny_[t_]) + Y0, int(nz_[t_]) + V.z0], blokken=float(hoog_d[j]) / 2)
            uitkomst.append(dict(soort="VAL", x=wx, y=int(c[1]) + Y0, z=wz, n=int(len(pts)), uitweg=uitweg,
                                 ymin=int(pts[:, 1].min()) + Y0, ymax=int(pts[:, 1].max()) + Y0,
                                 doos=[int(pts[:, 0].min()) + V.x0, int(pts[:, 2].min()) + V.z0,
                                       int(pts[:, 0].max()) + V.x0, int(pts[:, 2].max()) + V.z0]))
    # stairs (only the reachable ones)
    bereik = np.zeros(k.shape, bool)
    bereik[nx_[F[:N]], ny_[F[:N]], nz_[F[:N]]] = True
    sx, sy, sz = np.nonzero((k == STAIR) & bereik)
    for x, y, z in zip(sx, sy, sz):
        wx, wz = int(x) + V.x0, int(z) + V.z0
        if not (kx0 <= wx <= kx1 and kz0 <= wz <= kz1):
            continue
        fx, fz = list(FACING.values())[V.f[x, y, z] - 1]
        ux, uz = x + fx, z + fz
        lx, lz = x - fx, z - fz
        if not (0 <= ux < NX and 0 <= uz < NZ and 0 <= lx < NX and 0 <= lz < NZ and 1 <= y < NY - 3):
            continue
        boven_ok = node[ux, y + 1, uz] or (k[ux, y + 1, uz] == STAIR) or node[ux, y, uz] and half[ux, y, uz]
        if k[ux, y + 1, uz] == FENCE or k[ux, y, uz] == FENCE and not node[ux, y + 1, uz]:
            uitkomst.append(dict(soort="TRAP", x=wx, y=int(y) + Y0, z=wz, wat="hek blokkeert de bovenkant"))
        elif not boven_ok:
            # the top step of a flight may also end on a floor beside it (a landing) - only flag real dead ends
            if not (P[ux, y + 1, uz] and P[ux, y + 2, uz] and (SUP[ux, y, uz] or HALFB[ux, y, uz])):
                uitkomst.append(dict(soort="TRAP", x=wx, y=int(y) + Y0, z=wz, wat="bovenkant leidt nergens heen"))
        onder_ok = node[lx, y, lz] or k[lx, y - 1, lz] == STAIR or node[lx, y - 1, lz] or CL[lx, y, lz]
        if not onder_ok and P[lx, y, lz]:
            uitkomst.append(dict(soort="TRAP-", x=wx, y=int(y) + Y0, z=wz, wat="onderkant zonder vloer"))
    # dirt paths with something solid in the 2 cells above
    blok = ~P & (k != DIRT_PATH) & ~HALFB
    kand = (k == DIRT_PATH) & (up(blok) | up(blok, 2)) & bereik.any(1)[:, None, :]
    for x, y, z in zip(*np.nonzero(kand)):
        wx, wz = int(x) + V.x0, int(z) + V.z0
        if kx0 <= wx <= kx1 and kz0 <= wz <= kz1:
            uitkomst.append(dict(soort="PAD", x=wx, y=int(y) + 1 + Y0, z=wz, wat="blok op het pad"))
    return uitkomst


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--box", default=None, help="x0,z0,x1,z1 (world)")
    ap.add_argument("--json", default=None)
    ap.add_argument("--tegels", default=TEGELS)
    ap.add_argument("--venster", type=int, default=4, help="core tiles per window side")
    ap.add_argument("--soepel", default=None, help="x,y,z;x,y,z: can you walk from A to B without a single jump/drop?")
    ap.add_argument("--route", default=None, help="x,y,z: print how a player gets to this stand position (debug)")
    ap.add_argument("--zaden", default=None, help="json {seeds: [[x,y,z]..], links: [[a, b, both]..], pois: {id: [x,y,z]}}")
    ap.add_argument("--y0", type=int, default=None, help="lowest checked y (default 16)")
    ap.add_argument("--y1", type=int, default=None, help="highest checked y + 1 (default 272; zone 3 needs 316)")
    a = ap.parse_args()
    global Y0, Y1, SEEDS, LINKS, POIS
    if a.zaden:
        with open(a.zaden, encoding="utf-8") as f:
            zd = json.load(f)
        SEEDS, LINKS, POIS = zd["seeds"], zd.get("links", []), zd.get("pois")
    Y0 = a.y0 if a.y0 is not None else Y0
    Y1 = a.y1 if a.y1 is not None else Y1
    bes = glob.glob(os.path.join(a.tegels, "t_*.nbt"))
    tt = [tuple(int(v) for v in re.findall(r"-?\d+", os.path.basename(b))) for b in bes]
    TX0, TX1 = min(t[0] for t in tt), max(t[0] for t in tt)
    TZ0, TZ1 = min(t[1] for t in tt), max(t[1] for t in tt)
    box = [int(v) for v in a.box.split(",")] if a.box else [TX0 * T, TZ0 * T, TX1 * T + T - 1, TZ1 * T + T - 1]
    bx0, bz0, bx1, bz1 = box
    alles = []
    t0 = time.time()
    W = a.venster
    for cx in range(bx0 // T, bx1 // T + 1, W):
        for cz in range(bz0 // T, bz1 // T + 1, W):
            kx1, kz1 = min(cx + W - 1, bx1 // T), min(cz + W - 1, bz1 // T)
            V = Venster(max(cx - 1, TX0), max(cz - 1, TZ0), min(kx1 + 1, TX1), min(kz1 + 1, TZ1), a.tegels)
            if V.leeg:
                continue
            kern = (max(cx * T, bx0), max(cz * T, bz0), min(kx1 * T + T - 1, bx1), min(kz1 * T + T - 1, bz1))
            r = analyse(V, kern, [int(v) for v in a.route.split(",")] if a.route else None,
                        [[int(v) for v in p.split(",")] for p in a.soepel.split(";")] if a.soepel else None)
            alles += r
            print(f"  venster {cx},{cz}: {len(r)} bevindingen ({time.time() - t0:.0f} s)", flush=True)
    soorten = {}
    for b in alles:
        soorten.setdefault(b["soort"], []).append(b)
    print(f"\nloopcheck {box}: " + ", ".join(f"{k} {len(v)}" for k, v in sorted(soorten.items())) + f" ({time.time() - t0:.0f} s)")
    for b in sorted(soorten.get("VAL", []), key=lambda b: -b["n"]):
        u = b.get("uitweg")
        uu = f"; kortste uitweg {u['blokken']} blok omhoog bij {u['van']} -> {u['naar']}" if u else ""
        print(f"VAL   {b['x']:5d} {b['y']:4d} {b['z']:5d}  {b['n']:5d} plekken, y {b['ymin']}..{b['ymax']}, doos {b['doos']}{uu}")
    for s in ("LEEG", "TRAP", "TRAP-", "PAD"):
        for b in soorten.get(s, []):
            print(f"{s:5s} {b['x']:5d} {b['y']:4d} {b['z']:5d}  {b['wat']}")
    if a.json:
        with open(a.json, "w", encoding="utf-8") as f:
            json.dump(alles, f, indent=1)
    return alles


if __name__ == "__main__":
    main()
