"""U-OVERGANG: the seam pass over the zone edges (FEEDBACK_RONDE4: "het voelt als knip-en-plakwerk").

Every zone builds its own tiles with its own sea floor (zone 1 light-blue terracotta at 49, zone 2 gravel 53-60, zone 3 gravel
50-60, zone 4 pink sand 34-55) and the procedural sea between them (ZeeVulling) had yet another floor, so every tile-box edge
was a straight under-water cliff + a hard change of floor colour, and zone 3's cloud sea stopped dead at its box. This pass runs
after the map build (maak_kaart.py, every --zone) over the edge tiles of every zone and makes the seams meet the shared open
sea of zeenaad.py (= ZeeVulling in Java):

  * floor height: clamped towards zeenaad.vloer with an allowed deviation that grows 0.35 block per block away from the box
    edge (so at the edge the floor IS the open-sea floor, and an old under-water cliff becomes a slope over <= 48 blocks);
  * floor block: dithered towards zeenaad.materiaal over the same band (no colour edge);
  * cloud sea: zone 3's banks (zeenaad.wolk) are continued into the neighbouring tiles' open water (zone 2's east edge, the
    Splinterwand row, zone 4's west edge) where they thin out, exactly like ZeeVulling continues them in the open sea.

Only OPEN-SEA columns are touched (water from the floor up to y 62, nothing but air/clouds above, no block entities), so coasts,
piers, wrecks and islands never change. Every step is idempotent (a clamp, a per-column choice, a fixed cloud shape): running
it twice gives the same tiles.

Run from the project root:  python tools/vadsrijk/overgang.py [--alleen tx,tz;...] [--droog]
"""
import argparse
import gzip
import os
import struct
import sys
import time

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, ".."))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
UIT = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
sys.path.insert(0, HIER)
import zeenaad as ZN  # noqa: E402

T, HOOG = 64, 320
ZEE = 62
BAND = 48            # blocks from a box edge in which the floor is pulled to the open sea
HELLING = 0.35       # allowed floor deviation per block away from the edge
WOLK_BEREIK = 72     # zone-3 cloud banks reach this far past the zone-3 box

# tile groups (inclusive WORLD tile indices): every group's outer edge meets the open sea or another group
GROEPEN = {
    "z1": (-11, -11, 11, 18),        # zone 1 + Knuffelkamp (one grid)
    "wand": (-2, -12, 15, -12),      # the Splinterwand row (barriere.maak_tegels)
    "z2": (6, -23, 15, -13),
    "z3": (16, -16, 26, -3),
    "z4": (27, -12, 39, -5),         # zone 4 (+ zone 4's sea under the shared zone-5 tiles tx 38..39)
}
WOLK_NAMEN = set(ZN.WOLK) | {"guhs:wolkenblok_wit"}
VAST_NIET = {"minecraft:air", "minecraft:cave_air", "minecraft:structure_void"}


# ------------------------------------------------------------------------------------------------ tile io (keeps block entities)
_REC = np.dtype([("t_pos", "u1"), ("n_pos", ">u2"), ("s_pos", "S3"), ("et", "u1"), ("cnt", ">i4"),
                 ("x", ">i4"), ("y", ">i4"), ("z", ">i4"),
                 ("t_st", "u1"), ("n_st", ">u2"), ("s_st", "S5"), ("st", ">i4"), ("end", "u1")])


def _rstr(d, p):
    k = struct.unpack_from(">H", d, p)[0]
    return d[p + 2:p + 2 + k].decode(), p + 2 + k


def _skip(d, p, t):
    """Skip one NBT payload of type t at p; returns the new position."""
    if t in (1,):
        return p + 1
    if t == 2:
        return p + 2
    if t in (3, 5):
        return p + 4
    if t in (4, 6):
        return p + 8
    if t == 8:
        return p + 2 + struct.unpack_from(">H", d, p)[0]
    if t == 7:
        return p + 4 + struct.unpack_from(">i", d, p)[0]
    if t == 11:
        return p + 4 + 4 * struct.unpack_from(">i", d, p)[0]
    if t == 12:
        return p + 4 + 8 * struct.unpack_from(">i", d, p)[0]
    if t == 9:
        et = d[p]
        n = struct.unpack_from(">i", d, p + 1)[0]
        p += 5
        for _ in range(n):
            p = _skip(d, p, et)
        return p
    if t == 10:
        while True:
            tt = d[p]
            p += 1
            if tt == 0:
                return p
            _, p = _rstr(d, p)
            p = _skip(d, p, tt)
    raise ValueError(t)


class Tegel:
    def __init__(self, pad):
        d = gzip.decompress(open(pad, "rb").read())
        i = d.index(b"\x09\x00\x07palette")
        self.kop = d[:i]
        p = i + 10
        assert d[p] == 10
        n = struct.unpack_from(">i", d, p + 1)[0]
        p += 5
        self.pal = []
        for _ in range(n):
            naam, props = None, {}
            while True:
                t = d[p]
                p += 1
                if t == 0:
                    break
                k, p = _rstr(d, p)
                if t == 8:
                    v, p = _rstr(d, p)
                    if k == "Name":
                        naam = v
                elif t == 10:
                    while True:
                        t2 = d[p]
                        p += 1
                        if t2 == 0:
                            break
                        k2, p = _rstr(d, p)
                        v2, p = _rstr(d, p)
                        props[k2] = v2
                else:
                    raise ValueError(t)
            self.pal.append((naam, props))
        assert d[p:p + 9] == b"\x09\x00\x06blocks", d[p:p + 9]
        p += 9
        assert d[p] == 10
        nb = struct.unpack_from(">i", d, p + 1)[0]
        p += 5
        # fixed 36-byte records first (nbtsnel), then the slow ones with an nbt compound (block entities): kept verbatim
        kan = min(nb, (len(d) - p) // 36)
        a = np.frombuffer(d, _REC, kan, p)
        ok = ((a["t_pos"] == 9) & (a["n_pos"] == 3) & (a["s_pos"] == b"pos") & (a["et"] == 3) & (a["cnt"] == 3)
              & (a["t_st"] == 3) & (a["n_st"] == 5) & (a["s_st"] == b"state") & (a["end"] == 0))
        nvast = kan if ok.all() else int(np.argmin(ok))
        a = a[:nvast]
        p += nvast * 36
        self.traag = []
        for _ in range(nb - nvast):
            q = _skip(d, p, 10)
            raw = d[p:q]
            x, y, z = struct.unpack_from(">iii", raw, raw.index(b"\x09\x00\x03pos") + 11)
            self.traag.append(((x, y, z), raw))
            p = q
        self.staart = d[p:]
        self.vol = np.full((T, HOOG, T), -1, np.int32)
        self.vol[a["x"], a["y"], a["z"]] = a["st"]
        self.slot = np.zeros((T, T), bool)
        for (x, y, z), _ in self.traag:
            self.vol[x, y, z] = -2                     # locked (block entity): never touched
            self.slot[x, z] = True

    def id(self, naam):
        for i, (n, pr) in enumerate(self.pal):
            if n == naam and not pr:
                return i
        self.pal.append((naam, {}))
        return len(self.pal) - 1

    def schrijf(self, pad):
        sys.path.insert(0, os.path.join(HIER, "wereld"))
        import nbtsnel
        xs, ys, zs = np.nonzero(self.vol >= 0)
        st = self.vol[xs, ys, zs]
        a = np.zeros(len(xs), _REC)
        a["t_pos"], a["n_pos"], a["s_pos"], a["et"], a["cnt"] = 9, 3, b"pos", 3, 3
        a["x"], a["y"], a["z"] = xs, ys, zs
        a["t_st"], a["n_st"], a["s_st"] = 3, 5, b"state"
        a["st"] = st
        a = a[np.lexsort((a["x"], a["z"], a["y"]))]
        pal = b""
        for (name, props) in self.pal:
            e = {"Name": name}
            if props:
                e["Properties"] = dict(props)
            pal += nbtsnel._compound_kv(e) + b"\x00"
        body = (self.kop + b"\x09\x00\x07palette" + bytes([10]) + struct.pack(">i", len(self.pal)) + pal
                + b"\x09\x00\x06blocks" + bytes([10]) + struct.pack(">i", len(a) + len(self.traag)) + a.tobytes()
                + b"".join(r for _, r in self.traag) + self.staart)
        with open(pad, "wb") as f:
            f.write(gzip.compress(body, compresslevel=9, mtime=0))


# ------------------------------------------------------------------------------------------------ the pass
def _groep_box(g):
    a0, b0, a1, b1 = GROEPEN[g]
    return a0 * T, b0 * T, a1 * T + T - 1, b1 * T + T - 1


def groep_van(tx, tz):
    for g, (a0, b0, a1, b1) in GROEPEN.items():
        if a0 <= tx <= a1 and b0 <= tz <= b1:
            return g
    return None


def te_doen():
    """Every existing tile within BAND of its group's edge, or within WOLK_BEREIK of the zone-3 box (cloud continuation)."""
    uit = []
    for f in os.listdir(UIT):
        if not (f.startswith("t_") and f.endswith(".nbt")):
            continue
        tx, tz = (int(v) for v in f[2:-4].split("_"))
        g = groep_van(tx, tz)
        if g is None:
            continue
        x0, z0, x1, z1 = _groep_box(g)
        tx0, tz0 = tx * T, tz * T
        rand = min(tx0 - x0, x1 - (tx0 + T - 1), tz0 - z0, z1 - (tz0 + T - 1))
        zb = ZN.Z3_BOX
        dx = max(zb[0] - (tx0 + T - 1), tx0 - zb[2], 0)
        dz = max(zb[1] - (tz0 + T - 1), tz0 - zb[3], 0)
        if rand < BAND / 0.55 or (g != "z3" and np.hypot(dx, dz) < WOLK_BEREIK):
            uit.append((tx, tz))
    return sorted(uit)


def verwerk(tx, tz, droog=False):
    pad = os.path.join(UIT, f"t_{tx}_{tz}.nbt")
    if not os.path.exists(pad):
        return None
    g = groep_van(tx, tz)
    x0, z0, x1, z1 = _groep_box(g)
    Tg = Tegel(pad)
    vol = Tg.vol
    W = Tg.id("minecraft:water")
    STEEN = Tg.id("minecraft:stone")
    MAT = [Tg.id(n) for n in ZN.MATERIAAL]
    WOLKID = [Tg.id(n) for n in ZN.WOLK]
    n_pal = len(Tg.pal)
    # class tables over the palette + 2 extra entries: n_pal = locked (block entity), n_pal + 1 = absent
    water_l = np.array([n == "minecraft:water" and pr.get("level", "0") == "0" for n, pr in Tg.pal] + [False, False])
    wolk_l = np.array([n in WOLK_NAMEN for n, _ in Tg.pal] + [False, False])
    leeg_l = np.array([n in VAST_NIET for n, _ in Tg.pal] + [False, True])
    gi = np.where(vol == -2, n_pal, np.where(vol == -1, n_pal + 1, vol))
    isW, isC, isL = water_l[gi], wolk_l[gi], leeg_l[gi]

    X = (tx * T + np.arange(T))[:, None].astype(np.float64) + np.zeros((1, T))
    Z = (tz * T + np.arange(T))[None, :].astype(np.float64) + np.zeros((T, 1))
    d = np.minimum(np.minimum(X - x0, x1 - X), np.minimum(Z - z0, z1 - Z))
    d = d * (0.55 + 0.9 * ZN.ruis2(X, Z, 40, 31))                      # a wobbly band: the deep zone-4 basin etc. get an organic rim
    G = ZN.vloer(X, Z)
    M = ZN.materiaal(X, Z)
    wt, ws = ZN.wolk(X, Z) if g != "z3" else (np.zeros((T, T), np.int32), None)
    hsh = ZN.hash01(X, Z, 21)                                          # per-column dither value in [0, 1)
    gewijzigd = np.zeros((T, T), bool)
    n_vloer = n_mat = n_wolk = 0
    for i in range(T):
        for k in range(T):
            if Tg.slot[i, k]:
                continue
            kolW = isW[i, :, k]
            if not (kolW[ZEE - 1] and (kolW[ZEE] or isC[i, ZEE, k])):
                continue
            boven = slice(ZEE + 1, ZEE + 30)
            if not np.all(isL[i, boven, k] | isC[i, boven, k]):
                continue
            y = ZEE - 1
            while y > 10 and kolW[y]:
                y -= 1
            f = y                                                      # floor top (solid or absent)
            if f <= 10:
                continue
            band = d[i, k] < BAND
            if band:
                Mx = int(np.floor(HELLING * d[i, k]))
                fn = int(min(max(f, G[i, k] - Mx), G[i, k] + Mx))
                w = 1.0 - d[i, k] / BAND
                w = w * w * (3 - 2 * w)
                oud = vol[i, f, k]
                if hsh[i, k] < w or oud < 0 or isL[i, f, k]:
                    mat = MAT[M[i, k]]
                else:
                    mat = oud
                if fn != f or vol[i, fn, k] != mat:
                    if fn < f:
                        vol[i, fn + 1:f + 1, k] = W
                        n_vloer += 1
                    elif fn > f:
                        vol[i, f + 1:fn, k] = STEEN
                        n_vloer += 1
                    if vol[i, fn, k] != mat:
                        n_mat += 1
                    vol[i, fn, k] = mat
                    for yy in range(fn - 3, fn):
                        if vol[i, yy, k] == -1:
                            vol[i, yy, k] = STEEN
                    gewijzigd[i, k] = True
            t = int(wt[i, k])
            if t >= 1:
                for yy in range(ZEE, ZEE + 1 + t):
                    v = vol[i, yy, k]
                    if v == -1 or v == W or (v >= 0 and isW[i, yy, k]):
                        vol[i, yy, k] = WOLKID[ws[i, k]]
                        n_wolk += 1
    # absent cells under the sea next to new water (hollowed rock exposed by a lowered floor): solid stone
    if gewijzigd.any():
        rond = gewijzigd.copy()
        rond[1:] |= gewijzigd[:-1]
        rond[:-1] |= gewijzigd[1:]
        rond[:, 1:] |= gewijzigd[:, :-1]
        rond[:, :-1] |= gewijzigd[:, 1:]
        isWn = vol == W
        buur = np.zeros_like(isWn)
        buur[1:] |= isWn[:-1]
        buur[:-1] |= isWn[1:]
        buur[:, 1:] |= isWn[:, :-1]
        buur[:, :-1] |= isWn[:, 1:]
        buur[:, :, 1:] |= isWn[:, :, :-1]
        buur[:, :, :-1] |= isWn[:, :, 1:]
        gat = (vol == -1) & buur & rond[:, None, :]
        gat[:, ZEE:, :] = False
        vol[gat] = STEEN
    if not droog and (n_vloer or n_mat or n_wolk):
        Tg.schrijf(pad)
    return n_vloer, n_mat, n_wolk


def main(alleen=None, droog=False):
    t0 = time.time()
    tegels = te_doen()
    if alleen:
        tegels = [t for t in tegels if t in alleen]
    tot = [0, 0, 0]
    for n, (tx, tz) in enumerate(tegels):
        r = verwerk(tx, tz, droog)
        if r:
            for j in range(3):
                tot[j] += r[j]
    print(f"overgang: {len(tegels)} randtegels, vloer {tot[0]} kolommen, bodemblok {tot[1]}, wolk {tot[2]} blokken "
          f"({time.time() - t0:.0f} s)")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--alleen", default=None)
    ap.add_argument("--droog", action="store_true")
    a = ap.parse_args()
    al = None
    if a.alleen:
        al = {tuple(int(v) for v in s.split(",")) for s in a.alleen.split(";")}
    main(al, a.droog)
