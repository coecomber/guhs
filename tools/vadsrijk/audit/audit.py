"""Walkability audit of the Vadsrijk map tiles (survival rules) + the safe-spot list for the unstuck key.

Run from the project root (after `python tools/vadsrijk/maak_kaart.py`):
    python tools/vadsrijk/audit/audit.py                 # report + PNG + safe spots
    python tools/vadsrijk/audit/audit.py --alleen-plekken   # only the safe-spot json (the generator hook)
Output:
    AUDIT_VADSRIJK.md                                     (project root: findings with coordinates, grouped per area)
    tools/vadsrijk/_cache/audit_vadsrijk.png              (top-down map with the problems marked; + per-area crops)
    src/main/resources/data/guhs/vadsrijk/veilige_plekken.json   (safe spots for kern.veilig.VeiligePlekken)

How it works: the tiles are loaded into one dense block-class volume (bedrock at y 0 like the flat generator; the moving
temple doors counted as open). Every column is cut into "runs" (vertical stretches of passable blocks); a run with room
for a player is a node. Edges between runs of neighbouring columns follow the Minecraft movement rules (in half blocks):
step up at most 1 block (a jump, needs head room; stairs/slabs are just that), fall down any height, fence/wall tops count
as 1.5 high, water lets you swim up to its surface (+1 block out), ladders/vines/cloud streams let you climb. The city
lifts and the airship are extra edges. Seeds = every campfire + the arrival jetty.
  - good (network) = reachable from a seed AND can walk back to a seed;
  - pit/trap = reachable from a seed but can NOT get back (incl. falling under the map onto the bedrock);
  - plus: stair-flight ends that don't connect, doors with a blocked side or not reachable, trees/leaves on paths,
    grass/flower floors under building roofs, path steps of 2+ blocks, water more than 10 swims from a way out.
"""
import argparse
import json
import os
import re
import sys
import time

import numpy as np
from scipy import sparse
from scipy.sparse import csgraph

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
import blokklassen as BK  # noqa: E402
import tegellezer as TL  # noqa: E402

ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
PLEKKEN_JAVA = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "wereld", "WereldPlekken.java")
CACHE = os.path.join(ROOT, "tools", "vadsrijk", "_cache")
RAPPORT = os.path.join(ROOT, "AUDIT_VADSRIJK.md")
PLEKKEN_JSON = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "vadsrijk", "veilige_plekken.json")

T = 64
GROOT = 1000          # "top" of the sky run (open to the sky)
ROOSTER = 8           # safe spots: at most one per 8x8 columns per height band
BAND = 12
DAK = ("dakpan", "tentdoek")   # stairs of these are roofs

# kern Gebieden (order = Gebieden.op: first match wins)
GEBIEDEN = [("zwamzoet", -160, -160, 160, 160), ("haven", -120, 400, 120, 600), ("klodderweide", 160, -300, 600, 400),
            ("sporenwoud", -600, -160, -160, 400), ("druipgrotten", -600, -600, -160, -160), ("zwamtempel", 160, -600, 600, -300),
            ("knuffelkamp", -128 - 384, 900, 128 - 384, 1150), ("ring", -660, -660, 660, 660)]   # T-WERELD: camp moved (-384, 0)


def gebied(x, z):
    for g, x0, z0, x1, z1 in GEBIEDEN:
        if x0 <= x <= x1 and z0 <= z <= z1:
            return g
    return "buiten zone 1"


# ------------------------------------------------------------------ inputs
def lees_plekken():
    src = open(PLEKKEN_JAVA, encoding="utf-8").read()
    bp = r"new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)"
    alle = {m.group(1): tuple(int(v) for v in m.group(2, 3, 4)) for m in re.finditer(r'Map\.entry\("([^"]+)", ' + bp + r"\)", src)}
    kv = re.search(r"KAMPVUREN = List\.of\((.*?)\);", src, re.S)
    kampvuren = [tuple(int(v) for v in m.groups()) for m in re.finditer(bp, kv.group(1))] if kv else []
    deuren = [(m.group(1), tuple(int(v) for v in m.group(2, 3, 4)), tuple(int(v) for v in m.group(5, 6, 7)))
              for m in re.finditer(r'new Deur\("([^"]+)", ' + bp + ", " + bp, src)]
    vast = {m.group(1): tuple(int(v) for v in m.group(2, 3, 4)) for m in re.finditer(r"(\w+) = " + bp, src)}
    return alle, kampvuren, deuren, vast


def laad():
    # (integration) zone 1 only: the zone-2 tiles (tz -48..-28) share the folder but are a separate map
    # T-WERELD: zone 2 (tx 6..15, tz -23..-13), zone 3 and zone 4/5 now sit next to zone 1: only the zone-1 grid (+ camp)
    lijst = [t for t in TL.tegels(TEGELS) if -11 <= t[0] <= 11 and -11 <= t[1] <= 18]
    txs = [t[0] for t in lijst]
    tzs = [t[1] for t in lijst]
    tx0, tz0 = min(txs), min(tzs)
    NX, NZ = (max(txs) - tx0 + 1) * T, (max(tzs) - tz0 + 1) * T
    data = []
    ymax = 0
    for tx, tz, pad in lijst:
        pal, (xs, ys, zs, st) = TL.lees(pad)
        data.append((tx, tz, pal, xs, ys, zs, st))
        if len(ys):
            ymax = max(ymax, int(ys.max()))
    NY = ymax + 4
    cls = np.zeros((NX, NZ, NY), np.uint8)
    mat = np.zeros((NX, NZ, NY), np.uint8)
    cls[:, :, 0] = BK.SOLID                       # the flat generator's bedrock layer
    trappen, deuren = [], []
    for tx, tz, pal, xs, ys, zs, st in data:
        lut = np.array([BK.klasse(s) for s in pal], np.uint8).reshape(-1, 2)
        ox, oz = (tx - tx0) * T, (tz - tz0) * T
        cls[ox + xs, oz + zs, ys] = lut[st, 0]
        mat[ox + xs, oz + zs, ys] = lut[st, 1]
        # stairs + doors with their properties (sparse)
        for i, s in enumerate(pal):
            n, props = BK.splits(s)
            if lut[i, 1] == BK.M_TRAP and props.get("half") == "bottom":
                k = st == i
                f = props.get("facing", "north")
                if any(w in n for w in DAK):
                    continue                               # roofs are not stairs to walk on
                for x, y, z in zip(xs[k], ys[k], zs[k]):
                    trappen.append((ox + int(x), int(y), oz + int(z), f))
            elif lut[i, 1] == BK.M_DEUR and props.get("half", "lower") == "lower" and "gate" not in n:
                k = st == i
                f = props.get("facing", "north")
                for x, y, z in zip(xs[k], ys[k], zs[k]):
                    deuren.append((ox + int(x), int(y), oz + int(z), f, n))
    X0, Z0 = tx0 * T, tz0 * T
    return cls, mat, X0, Z0, trappen, deuren


RICHTING = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}


# ------------------------------------------------------------------ runs
class Runs:
    pass


def maak_runs(cls):
    NX, NZ, NY = cls.shape
    P = np.isin(cls, BK.PASSABLE)
    onder = np.zeros_like(P)
    onder[:, :, 1:] = P[:, :, :-1]
    boven = np.zeros_like(P)
    boven[:, :, :-1] = P[:, :, 1:]
    sx, sz, sy = np.nonzero(P & ~onder)
    ex, ez, ey = np.nonzero(P & ~boven)
    del onder, boven
    assert len(sx) == len(ex)
    R = Runs()
    R.x, R.z, R.b = sx.astype(np.int32), sz.astype(np.int32), sy.astype(np.int32)
    t = ey.astype(np.int32)
    t[t == NY - 1] = GROOT
    R.t = t
    fc = cls[sx, sz, sy - 1]
    R.fc = fc
    F = 2 * R.b
    F = np.where(fc == BK.SLAB, F - 1, F)
    F = np.where(fc == BK.FENCE, F + 1, F)
    R.F = F.astype(np.int32)
    # swim / climb chain from the floor up
    X = (cls == BK.WATER) | (cls == BK.CLIMB)
    start = R.b.copy()
    c0 = X[R.x, R.z, R.b]
    c1 = ~c0 & (cls[R.x, R.z, np.minimum(R.b + 1, NY - 1)] == BK.CLIMB)
    start[c1] += 1
    actief = np.nonzero(c0 | c1)[0]
    top = start.copy()
    while len(actief):
        nxt = top[actief] + 1
        ok = (nxt < NY) & (nxt <= R.t[actief])
        ok[ok] = X[R.x[actief[ok]], R.z[actief[ok]], nxt[ok]]
        top[actief[ok]] = nxt[ok]
        actief = actief[ok]
    heeft = c0 | c1
    water_top = heeft & (cls[R.x, R.z, np.minimum(top, NY - 1)] == BK.WATER)
    H = np.where(heeft, 2 * top + 2 * water_top.astype(np.int32), R.F)   # (out of water: onto a block 1 above the surface)
    H = np.minimum(H, 2 * R.t - 2)
    R.H = np.maximum(H, R.F).astype(np.int32)
    R.water = cls[R.x, R.z, R.b] == BK.WATER
    R.geldig = (2 * R.t - 2 >= R.F)
    R.bodem = R.b <= 1                      # standing on the bedrock: under the map
    return R, P


def kolomtabel(R, NX, NZ, ids):
    """Padded per-column tables of the given run ids: (K, F, H, T, ID) arrays shaped (NX, NZ, K)."""
    col = R.x[ids].astype(np.int64) * NZ + R.z[ids]
    volg = np.argsort(col, kind="stable")
    ids = ids[volg]
    col = col[volg]
    eerste = np.searchsorted(col, col, side="left")
    rang = np.arange(len(col)) - eerste
    K = int(rang.max()) + 1
    tab = {}
    for naam, w, vul in (("F", R.F, 32000), ("H", R.H, -32000), ("T", R.t, -1000), ("ID", None, -1)):
        a = np.full((NX * NZ, K), vul, np.int32)
        a[col, rang] = ids if w is None else w[ids]
        tab[naam] = a.reshape(NX, NZ, K)
    return K, tab


def randen(R, NX, NZ):
    ids = np.nonzero(R.geldig)[0].astype(np.int32)
    K, tab = kolomtabel(R, NX, NZ, ids)
    F, H, Tt, ID = (tab[k].reshape(NX * NZ, K) for k in ("F", "H", "T", "ID"))
    kx = np.arange(NX * NZ) // NZ
    kz = np.arange(NX * NZ) % NZ
    src, dst = [], []
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for k1 in range(K):
            cols = np.nonzero(ID[:, k1] >= 0)[0]
            nx_, nz_ = kx[cols] + dx, kz[cols] + dz
            binnen = (nx_ >= 0) & (nx_ < NX) & (nz_ >= 0) & (nz_ < NZ)
            cols = cols[binnen]
            nb = (nx_[binnen] * NZ + nz_[binnen])
            iA, FA, HA, TA = ID[cols, k1], F[cols, k1], H[cols, k1], Tt[cols, k1]
            for k2 in range(K):
                iB = ID[nb, k2]
                heeft = iB >= 0
                if not heeft.any():
                    break                                  # (runs are packed: no k2 here means none above it either)
                FB, TB = F[nb, k2], Tt[nb, k2]
                lo = np.maximum(FA, FB - 2)
                hi = np.minimum(HA, 2 * TB - 2)
                ok = heeft & (lo <= hi) & ((hi >= FB) | (FB <= 2 * TA - 2))
                if ok.any():
                    src.append(iA[ok])
                    dst.append(iB[ok])
    return np.concatenate(src), np.concatenate(dst), K, tab


def run_op(R, tab, X0, Z0, x, y, z, marge=2):
    """The run in column (x, z) that contains feet height y (or the nearest floor within marge below)."""
    ix, iz = x - X0, z - Z0
    if not (0 <= ix < tab["ID"].shape[0] and 0 <= iz < tab["ID"].shape[1]):
        return -1
    beste, bd = -1, 999
    for i in tab["ID"][ix, iz]:
        if i < 0:
            continue
        b, t = R.b[i], R.t[i]
        if b - marge <= y <= t:
            d = 0 if b <= y else b - y
            d = min(d, abs(y - b) if y >= b else d)
            if beste < 0 or abs(y - b) < bd:
                beste, bd = int(i), abs(y - b)
    return beste


def bereik(G, bronnen):
    N = G.shape[0]
    mask = np.zeros(N, bool)
    if len(bronnen) == 0:
        return mask
    # super-source: one BFS from a virtual node
    s = np.asarray(bronnen, np.int64)
    extra = sparse.csr_matrix((np.ones(len(s), np.int8), (np.full(len(s), N), s)), shape=(N + 1, N + 1))
    G2 = sparse.bmat([[G, None], [None, sparse.csr_matrix((1, 1), dtype=np.int8)]], format="csr") + extra
    order = csgraph.breadth_first_order(G2, N, directed=True, return_predecessors=False)
    mask[order[order < N]] = True
    return mask


def clusters(R, sel, src, dst, N):
    """Connected groups of the selected runs (edges both ways)."""
    idx = np.nonzero(sel)[0]
    if len(idx) == 0:
        return []
    k = sel[src] & sel[dst]
    G = sparse.csr_matrix((np.ones(int(k.sum()), np.int8), (src[k], dst[k])), shape=(N, N))
    n, lab = csgraph.connected_components(G, directed=False)
    lab = lab[idx]
    volg = np.argsort(lab, kind="stable")
    lab, idx = lab[volg], idx[volg]
    grens = np.nonzero(np.diff(lab))[0] + 1
    return [g for g in np.split(idx, grens)]


def raster_clusters(punten, straal=3):
    """Groups (x, y, z, ...) points that lie within `straal` blocks of each other (2D)."""
    if not punten:
        return []
    pts = np.array([(p[0], p[2]) for p in punten])
    n = len(pts)
    from scipy.spatial import cKDTree
    paren = cKDTree(pts).query_pairs(straal, output_type="ndarray")
    G = sparse.csr_matrix((np.ones(len(paren)), (paren[:, 0], paren[:, 1])), shape=(n, n)) if len(paren) else sparse.csr_matrix((n, n))
    _, lab = csgraph.connected_components(G, directed=False)
    groepen = {}
    for i, l in enumerate(lab):
        groepen.setdefault(l, []).append(punten[i])
    return list(groepen.values())


# ------------------------------------------------------------------ main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--alleen-plekken", action="store_true", help="only write the safe-spot json")
    a = ap.parse_args()
    t0 = time.time()
    alle, kampvuren, deur_dozen, vast = lees_plekken()
    cls, mat, X0, Z0, trappen, deuren = laad()
    NX, NZ, NY = cls.shape
    for _, mn, mx in deur_dozen:                   # moving temple doors: open
        cls[mn[0] - X0:mx[0] - X0 + 1, mn[2] - Z0:mx[2] - Z0 + 1, mn[1]:mx[1] + 1] = BK.AIR
    print(f"geladen: {NX}x{NZ}x{NY}, {len(trappen)} traptreden, {len(deuren)} deuren ({time.time() - t0:.0f} s)")
    R, P = maak_runs(cls)
    N = len(R.b)
    src, dst, K, tab = randen(R, NX, NZ)
    print(f"runs {N} (geldig {int(R.geldig.sum())}, max {K} per kolom), randen {len(src)} ({time.time() - t0:.0f} s)")

    def op(p, marge=2):
        return run_op(R, tab, X0, Z0, p[0], p[1], p[2], marge)

    # extra edges: city lifts (both ways), airship
    extra = []
    for k, v in alle.items():
        if k.startswith("lift_") and k.endswith("_voet"):
            top = alle.get(k[:-5] + "_top")
            if top:
                extra.append((op(v), op(top)))
    if "KAMP_LANDING" in vast and "HAVEN_LANDING" in vast:
        extra.append((op(vast["KAMP_LANDING"]), op(vast["HAVEN_LANDING"])))
    for u, w in extra:
        if u >= 0 and w >= 0:
            src = np.append(src, [u, w]).astype(np.int32)
            dst = np.append(dst, [w, u]).astype(np.int32)
    G = sparse.csr_matrix((np.ones(len(src), np.int8), (src, dst)), shape=(N, N))
    GT = G.T.tocsr()

    zaden = {}
    for i, kv in enumerate(kampvuren):
        r = op((kv[0], kv[1] + 1, kv[2]), 1)
        zaden[f"kampvuur {kv[0]},{kv[1]},{kv[2]}"] = r
    zaden["aankomst"] = op(alle.get("aankomst", (0, 65, 1130)))
    goede_zaden = [r for r in zaden.values() if r >= 0]
    FR = bereik(G, goede_zaden)
    BR = bereik(GT, goede_zaden)
    goed = FR & BR & R.geldig
    val = FR & ~BR & R.geldig
    print(f"netwerk {int(goed.sum())}, vallen {int(val.sum())} ({time.time() - t0:.0f} s)")
    if os.environ.get("AUDIT_DEBUG"):
        np.savez(os.path.join(CACHE, "audit_debug.npz"), x=R.x, z=R.z, b=R.b, t=R.t, F=R.F, H=R.H, fc=R.fc, FR=FR, BR=BR,
                 X0=X0, Z0=Z0, src=src, dst=dst)

    # ---- safe spots (the json for kern.veilig)
    staan = goed & ~R.water & (R.fc != BK.FENCE) & (R.fc != BK.CLIMB) & ~R.bodem & (R.t - R.b >= 1)
    ix, iz, b = R.x, R.z, R.b
    pad = mat[ix, iz, b - 1] == BK.M_PAD
    # openness: neighbour columns with a good floor at about the same height
    kol_goed = np.full((NX, NZ), -999, np.int32)
    kol_goed[ix[staan], iz[staan]] = R.F[staan]   # (one per column is enough for the score)
    open_ = np.zeros(N, np.int32)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0), (0, 2), (0, -2)):
        nx_, nz_ = np.clip(ix + dx, 0, NX - 1), np.clip(iz + dz, 0, NZ - 1)
        open_ += (np.abs(kol_goed[nx_, nz_] - R.F) <= 2).astype(np.int32)
    score = open_ * 10 + pad * 25
    kand = np.nonzero(staan & (open_ >= 6))[0]
    sleutel = ((ix[kand] // ROOSTER).astype(np.int64) * 100000 + (iz[kand] // ROOSTER)) * 100 + b[kand] // BAND
    volg = np.lexsort((-score[kand], sleutel))
    sleutel, kand = sleutel[volg], kand[volg]
    eerste = np.ones(len(kand), bool)
    eerste[1:] = sleutel[1:] != sleutel[:-1]
    plek_ids = kand[eerste]
    plekken = []
    for i in plek_ids:
        plekken += [int(ix[i] + X0), int(b[i]), int(iz[i] + Z0), 1 if pad[i] else 0]
    os.makedirs(os.path.dirname(PLEKKEN_JSON), exist_ok=True)
    with open(PLEKKEN_JSON, "w", encoding="utf-8", newline="\n") as f:
        f.write('{"versie": 1, "uitleg": "GENERATED by tools/vadsrijk/audit/audit.py (x, y, z, soort; soort 1 = op een pad)", '
                f'"rooster": {ROOSTER}, "plekken": [')
        f.write(",".join(str(v) for v in plekken))
        f.write("]}\n")
    print(f"veilige plekken: {len(plek_ids)} -> {os.path.relpath(PLEKKEN_JSON, ROOT)}")
    if a.alleen_plekken:
        return

    bevindingen = {}   # soort -> list of dict(x,y,z,gebied,tekst,grootte)

    def voeg(soort, x, y, z, tekst, grootte=1):
        bevindingen.setdefault(soort, []).append({"x": int(x), "y": int(y), "z": int(z), "gebied": gebied(int(x), int(z)),
                                                  "tekst": tekst, "grootte": int(grootte)})

    # ---- 1. pits / traps (FR but not BR): one finding per place where you fall in (the good side of the edge)
    groep_van = np.full(N, -1, np.int32)
    groepen = clusters(R, val, src, dst, N)
    for gi, groep in enumerate(groepen):
        groep_van[groep] = gi
    k = goed[src] & val[dst]
    ing_per = {}
    for s_, d_ in zip(src[k], dst[k]):
        ing_per.setdefault(int(groep_van[d_]), []).append((int(R.x[s_] + X0), int(R.b[s_]), int(R.z[s_] + Z0)))
    for gi, groep in enumerate(groepen):
        kol = len(set(zip(R.x[groep].tolist(), R.z[groep].tolist())))
        laag = groep[np.argmin(R.b[groep])]
        lx, ly, lz = int(R.x[laag] + X0), int(R.b[laag]), int(R.z[laag] + Z0)
        bodem = bool(R.bodem[groep].any())
        water = bool(R.water[groep].any())
        oms = (f"onder de kaart (bedrock y 0; holle ondergrond, {kol} kolommen)" if bodem else
               f"kuil/kamer zonder uitweg ({kol} kolommen, y {int(R.b[groep].min())}..{int(R.b[groep].max())}"
               f"{', met water' if water else ''}; laagste punt {lx} {ly} {lz})")
        ing = ing_per.get(gi, [])
        if not ing:
            voeg("onder" if bodem else "kuil", lx, ly, lz, oms + "; geen ingang gevonden", kol)
            continue
        for g in raster_clusters(sorted(set(ing)), 3):
            x, y, z = g[0]
            voeg("onder" if bodem else "kuil", x, y, z, f"hier val je in: {oms}" + (f" ({len(g)} randblokken)" if len(g) > 1 else ""),
                 kol if len(g) == 1 else max(kol, len(g)))

    # ---- 2. POIs / seeds that are not on the network
    for naam, p in sorted(alle.items()):
        if any(s in naam for s in (".doos", ".min", ".max", "lichtstralen", "plafond", "hendels", "hints", "uitkijk_", "_top", "hoed_",
                                   "bord", "knuffel_", "luchtschip_haven_mast", "spawns", "lichtzwammen", "veilige_plekken")):
            continue
        r = op(p, 1)
        if r < 0:
            voeg("poi", p[0], p[1], p[2], f"`{naam}`: staat in een blok / geen ruimte om te staan")
        elif not goed[r]:
            voeg("poi", p[0], p[1], p[2], f"`{naam}`: {'niet bereikbaar vanaf het netwerk' if not FR[r] else 'je komt er niet meer weg'}")
    for naam, r in zaden.items():
        if r < 0:
            voeg("poi", 0, 0, 0, f"{naam}: geen staanplek naast het kampvuur")

    # per column: is there a good floor at feet height y? (for the stair / door checks)
    def vloeren(x, z):
        ix_, iz_ = x - X0, z - Z0
        if not (0 <= ix_ < NX and 0 <= iz_ < NZ):
            return []
        return [(int(R.F[i]), bool(FR[i])) for i in tab["ID"][ix_, iz_] if i >= 0]

    # ---- 3. stair flights whose end does not connect
    trapset = {(x, y, z): f for x, y, z, f in trappen}
    trap_pts = []
    for x, y, z, f in trappen:
        wx, wz = x + X0, z + Z0
        r = op((wx, y + 1, wz), 0)
        if r < 0 or not FR[r] or R.b[r] != y + 1:
            continue                                   # not walkable / not reachable: decoration or roof
        dx, dz = RICHTING.get(f, (0, -1))
        voor = trapset.get((x + dx, y + 1, z + dz)) == f
        achter = trapset.get((x - dx, y - 1, z - dz)) == f
        if not voor and not achter:
            continue                                   # a single step / bench: not a flight
        if not voor and trapset.get((x + dx, y, z + dz)) != {"north": "south", "south": "north", "east": "west", "west": "east"}.get(f):
            vl = vloeren(wx + dx, wz + dz)
            if not any(2 * y + 2 <= F <= 2 * y + 4 for F, _ in vl):
                naast = ", ".join(f"y {F / 2:g}" for F, _ in vl if abs(F - 2 * y - 2) <= 12) or "geen"
                trap_pts.append((wx, y, wz, f"trap (facing {f}) sluit boven niet aan: bij {wx + dx},{wz + dz} hoort een vloer op y {y + 1}..{y + 2}, "
                                        f"er is: {naast}"))
        if not achter:
            vl = vloeren(wx - dx, wz - dz)
            if not any(2 * y - 1 <= F <= 2 * y + 2 for F, _ in vl):
                naast = ", ".join(f"y {F / 2:g}" for F, _ in vl if abs(F - 2 * y) <= 12) or "geen"
                trap_pts.append((wx, y, wz, f"trap (facing {f}) sluit onder niet aan: bij {wx - dx},{wz - dz} hoort een vloer op y {y - 0.5:g}..{y + 1}, "
                                        f"er is: {naast}"))

    for g in raster_clusters(trap_pts, 1.5):
        x, y, z, tekst = g[0]
        voeg("trap", x, y, z, tekst + (f" (+{len(g) - 1} treden ernaast)" if len(g) > 1 else ""), len(g))

    # ---- 4. doors
    for x, y, z, f, n in deuren:
        wx, wz = x + X0, z + Z0
        dx, dz = RICHTING.get(f, (0, -1))
        kanten = []
        for s, (ax, az) in (("voor", (dx, dz)), ("achter", (-dx, -dz))):
            cx, cz = x + ax, z + az
            vrij = 0 <= cx < NX and 0 <= cz < NZ and P[cx, cz, y] and P[cx, cz, min(y + 1, NY - 1)]
            vloer = any(2 * y - 2 <= F <= 2 * y + 2 for F, _ in vloeren(wx + ax, wz + az))
            if not vrij:
                kanten.append(f"{s} geblokkeerd ({wx + ax},{y},{wz + az})")
            elif not vloer:
                kanten.append(f"{s}: geen vloer ({wx + ax},{wz + az})")
        r = op((wx, y, wz), 0)
        bereikbaar = r >= 0 and bool(goed[r])
        if kanten or not bereikbaar:
            voeg("deur", wx, y, wz, f"{n.split(':')[-1]}: " + ", ".join(kanten + ([] if bereikbaar else ["niet bereikbaar vanaf het netwerk"])))

    # ---- 5. trees / mushrooms on paths, 6. grass floors under roofs, 7. path steps
    boom = mat == BK.M_BOOM
    padblok = (mat == BK.M_PAD) & (cls == BK.SOLID)
    hit = np.zeros_like(padblok)
    hit[:, :, :-2] = padblok[:, :, :-2] & (boom[:, :, 1:-1] | boom[:, :, 2:])
    xs, zs, ys = np.nonzero(hit)
    pts = [(int(x + X0), int(y), int(z + Z0)) for x, z, y in zip(xs, zs, ys)]
    for g in raster_clusters(pts, 2):
        x, y, z = g[0]
        voeg("boom", x, y + 1, z, f"boom/paddenstoel op het pad ({len(g)} padblokken)", len(g))
    del hit, boom
    # grass / flowers in rooms: good runs with a building roof within 7 blocks
    kamer = goed & (R.t < GROOT) & (R.t - R.b <= 7)
    kamer &= mat[R.x, R.z, np.minimum(R.t + 1, NY - 1)] == BK.M_BOUW
    gras = kamer & ((mat[R.x, R.z, R.b - 1] == BK.M_GRAS) | (mat[R.x, R.z, R.b] == BK.M_BLOEM))
    gi = np.nonzero(gras)[0]
    pts = [(int(R.x[i] + X0), int(R.b[i]), int(R.z[i] + Z0)) for i in gi]
    for g in raster_clusters(pts, 2):
        x, y, z = g[len(g) // 2]
        voeg("gras", x, y, z, f"gras/bloemen als vloer onder een dak ({len(g)} blokken)", len(g))
    # path steps of 2+ blocks between two path columns of the surface (no stair in between)
    padrun = goed & (mat[R.x, R.z, R.b - 1] == BK.M_PAD) & (R.t >= GROOT)
    kolF = np.full((NX, NZ), -999, np.int32)
    kolF[R.x[padrun], R.z[padrun]] = R.F[padrun]
    pts = []
    for dx, dz in ((1, 0), (0, 1)):
        A = kolF[:NX - dx, :NZ - dz]
        B = kolF[dx:, dz:]
        stap = (A > -999) & (B > -999) & (np.abs(A - B) >= 4) & (np.abs(A - B) <= 24)
        for x, z in zip(*np.nonzero(stap)):
            pts.append((int(x + X0), int(max(A[x, z], B[x, z]) // 2), int(z + Z0)))
    for g in raster_clusters(pts, 3):
        x, y, z = g[0]
        voeg("stap", x, y, z, f"pad met een stap van 2+ blokken zonder trap ({len(g)} plekken)", len(g))

    # ---- 8. water without a way out within 10 swims
    wnode = R.geldig & R.water & FR
    land = goed & ~R.water
    k = wnode[src] & land[dst]
    uitgang = np.zeros(N, bool)
    uitgang[src[k]] = True
    afst = np.full(N, 99, np.int32)
    afst[uitgang] = 0
    kw = wnode[src] & wnode[dst]
    WT = sparse.csr_matrix((np.ones(int(kw.sum()), np.int8), (dst[kw], src[kw])), shape=(N, N))   # reversed: who reaches me
    front = np.nonzero(uitgang)[0]
    for d in range(1, 11):
        if not len(front):
            break
        nb = WT[front].indices
        nb = np.unique(nb[afst[nb] > d])
        afst[nb] = d
        front = nb
    ver = wnode & (afst > 10)
    for groep in clusters(R, ver, src, dst, N):
        kol = len(set(zip(R.x[groep].tolist(), R.z[groep].tolist())))
        m = groep[len(groep) // 2]
        soort = "zee" if kol > 100000 else "water"
        voeg(soort, R.x[m] + X0, R.b[m], R.z[m] + Z0, f"water zonder uitweg binnen 10 blokken: {kol} kolommen", kol)

    schrijf_rapport(bevindingen, len(plek_ids), time.time() - t0, int(goed.sum()))
    teken(cls, mat, X0, Z0, bevindingen)
    print(f"klaar ({time.time() - t0:.0f} s): " + ", ".join(f"{k} {len(v)}" for k, v in bevindingen.items()))


SOORTEN = [
    ("kuil", "Kuilen/kamers waar je in valt en niet meer uit komt", (255, 40, 40)),
    ("onder", "Door of onder de kaart vallen (bedrock op y 0)", (160, 0, 200)),
    ("trap", "Trappen die niet aansluiten", (255, 150, 0)),
    ("stap", "Paden met een stap van 2+ blokken (geen trap)", (255, 220, 0)),
    ("deur", "Deuren: geblokkeerd of onbereikbaar", (255, 0, 255)),
    ("boom", "Bomen/paddenstoelen op paden", (120, 70, 20)),
    ("gras", "Gras/bloemen als vloer binnen (onder een dak)", (60, 255, 60)),
    ("water", "Water zonder uitweg binnen 10 blokken", (0, 220, 255)),
    ("poi", "POI's/plekken die niet op het netwerk liggen", (255, 255, 255)),
    ("zee", "Open zee verder dan 10 blokken van de kant (laag; ter info)", (0, 90, 255)),
]
GEBIED_VOLG = ["knuffelkamp", "haven", "zwamzoet", "klodderweide", "sporenwoud", "druipgrotten", "zwamtempel", "ring", "buiten zone 1"]


def schrijf_rapport(bev, nplekken, sec, ngoed):
    r = ["# AUDIT_VADSRIJK - walkability audit of the zone-1 map", "",
         f"GENERATED by `python tools/vadsrijk/audit/audit.py` ({time.strftime('%Y-%m-%d %H:%M')}, {sec:.0f} s) over the tiles in "
         "`data/guhs/structure/vadsrijk/tegels/`. Rerun after every map regen. Picture: `tools/vadsrijk/_cache/audit_vadsrijk.png` "
         "(+ `audit_<gebied>.png` crops).", "",
         "Movement model (survival): step up max 1 block (stairs/slabs ok, fence/wall tops 1.5 = too high), fall any height, swim up "
         "to the water surface and climb out 1 block, ladders/vines/cloud streams climbable, city lifts + airship as links, temple "
         "doors open. Network = reachable from a campfire/the arrival AND back. Coordinates are stand/feet positions (x, y, z).", "",
         f"Network: {ngoed} walkable runs; {nplekken} safe spots written for the unstuck key.", "",
         "| soort | aantal |", "|---|---|"]
    for s, titel, _ in SOORTEN:
        r.append(f"| {titel} | {len(bev.get(s, []))} |")
    r.append("")
    for s, titel, _ in SOORTEN:
        lijst = bev.get(s, [])
        if not lijst:
            continue
        r += [f"## {titel} ({len(lijst)})", ""]
        for g in GEBIED_VOLG:
            sub = sorted([b for b in lijst if b["gebied"] == g], key=lambda b: (-b["grootte"], b["x"], b["z"]))
            if not sub:
                continue
            r.append(f"### {g} ({len(sub)})")
            r.append("")
            for b in sub[:150]:
                r.append(f"- `{b['x']} {b['y']} {b['z']}` {b['tekst']}")
            if len(sub) > 150:
                r.append(f"- ... and {len(sub) - 150} more")
            r.append("")
    with open(RAPPORT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(r))
    print("rapport:", os.path.relpath(RAPPORT, ROOT))


def teken(cls, mat, X0, Z0, bev):
    from PIL import Image, ImageDraw
    NX, NZ, NY = cls.shape
    vast = cls != BK.AIR
    ys = np.arange(NY)
    top = np.where(vast, ys[None, None, :], 0).max(axis=2)
    topc = np.take_along_axis(cls, top[:, :, None], 2)[:, :, 0]
    topm = np.take_along_axis(mat, top[:, :, None], 2)[:, :, 0]
    kleur = np.zeros((NX, NZ, 3), np.float32)
    kleur[:] = (150, 150, 150)
    kleur[topm == BK.M_GRAS] = (110, 170, 80)
    kleur[topm == BK.M_BLOEM] = (130, 190, 90)
    kleur[topm == BK.M_BOOM] = (60, 120, 50)
    kleur[topm == BK.M_PAD] = (190, 160, 110)
    kleur[topm == BK.M_BOUW] = (210, 140, 170)
    kleur[topc == BK.WATER] = (70, 110, 200)
    kleur[top <= 1] = (20, 20, 25)
    h = top.astype(np.float32)
    schaduw = np.clip(0.75 + (np.roll(h, 1, 0) - h) * -0.05 + (h - 64) * 0.004, 0.45, 1.25)
    kleur = np.clip(kleur * schaduw[:, :, None] * 0.8, 0, 255).astype(np.uint8)
    img = Image.fromarray(np.ascontiguousarray(np.transpose(kleur, (1, 0, 2))))     # x right, z down
    d = ImageDraw.Draw(img)
    for s, titel, rgb in reversed(SOORTEN):
        for b in bev.get(s, []):
            if s == "poi" and b["x"] == 0 and b["z"] == 0:
                continue
            x, z = b["x"] - X0, b["z"] - Z0
            rr = 3 if s == "zee" else 4 + min(6, int(np.log2(max(1, b["grootte"]))))
            d.ellipse([x - rr, z - rr, x + rr, z + rr], outline=rgb, width=2)
    # legend
    d.rectangle([4, 4, 360, 18 + 14 * len(SOORTEN)], fill=(0, 0, 0))
    for i, (s, titel, rgb) in enumerate(SOORTEN):
        d.ellipse([10, 12 + 14 * i, 20, 22 + 14 * i], outline=rgb, width=2)
        d.text((26, 10 + 14 * i), f"{titel} ({len(bev.get(s, []))})", fill=(255, 255, 255))
    os.makedirs(CACHE, exist_ok=True)
    img.save(os.path.join(CACHE, "audit_vadsrijk.png"))
    for g, x0, z0, x1, z1 in GEBIEDEN[:-1]:
        box = (max(0, x0 - X0 - 20), max(0, z0 - Z0 - 20), min(NX, x1 - X0 + 20), min(NZ, z1 - Z0 + 20))
        c = img.crop(box)
        c = c.resize((c.width * 2, c.height * 2), Image.NEAREST)
        c.save(os.path.join(CACHE, f"audit_{g}.png"))
    print("beeld:", os.path.relpath(os.path.join(CACHE, "audit_vadsrijk.png"), ROOT))


if __name__ == "__main__":
    main()
