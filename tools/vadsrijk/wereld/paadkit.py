"""Small kit for walkable paths in the air and in caves (fix round "verticaal": hoedenpaden.py + druipschacht.py).

Heights are counted in HALF blocks: a path cell has a stand level L (the player's feet at L / 2).
  L even -> a full block at L/2 - 1;  L odd -> a bottom slab at (L-1)/2;  "trap" -> a stair at L/2 - 1 (rises 2 halves).
Neighbouring path cells never differ by more than 1 half (slabs) or 2 halves with a stair, so everything is walkable in
survival without jumping.

Bouw collects blocks + carves in world coordinates and writes them as one stamp (fences/walls get their connections).
"""
import math

import numpy as np

import kaart as KA

CARVE = "minecraft:cave_air"
HEK_FAM = ("_fence",)
MUUR_FAM = ("_wall",)
NIET_VOL = ("slab", "stairs", "fence", "wall", "lantern", "carpet", "ladder", "chain", "door", "pane", "air", "light[",
            "cluster", "bud", "dripstone", "torch", "button", "flower", "petals", "mushroom[", "glimpaddenstoel", "water",
            "sniffer_egg", "turtle_egg", "roots", "candle", "glass", "leaves", "kristal_cluster", "weidekristal")


def trap(blok, facing, half="bottom"):
    return f"{blok}[facing={facing},half={half},shape=straight,waterlogged=false]"


def slab(blok, typ="bottom"):
    return f"{blok}[type={typ},waterlogged=false]"


def vol_blok(s):
    return not any(t in s for t in NIET_VOL)


class Bouw:
    def __init__(self):
        self.b = {}
        self.c = set()

    def set(self, x, y, z, s, keep=False):
        k = (int(x), int(y), int(z))
        if keep and k in self.b:
            return
        self.b[k] = s

    def get(self, x, y, z):
        return self.b.get((int(x), int(y), int(z)))

    def wis(self, x, y, z):
        k = (int(x), int(y), int(z))
        self.b.pop(k, None)
        self.c.add(k)

    def carve(self, x, y, z):
        self.c.add((int(x), int(y), int(z)))

    def fill(self, x1, y1, z1, x2, y2, z2, s, keep=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, s, keep)

    # -------------------------------------------------------- paths
    def dek(self, x, z, L, mat, facing=None, kop=3):
        """One path cell at stand level L (halves) + headroom carve. Returns the y of the deck block."""
        if facing:
            y = L // 2 - 1
            self.set(x, y, z, trap(mat["trap"], facing))
        elif L % 2 == 0:
            y = L // 2 - 1
            self.set(x, y, z, mat["vol"])
        else:
            y = (L - 1) // 2
            self.set(x, y, z, slab(mat["slab"]))
        for dy in range(1, kop + 1):
            k = (x, y + dy, z)
            if k not in self.b:
                self.c.add(k)
        return y

    def reling(self, x, z, L, mat, lamp=False):
        """A rail post next to a path cell at level L: a full block under it and a fence/wall on top."""
        y = (L - 1) // 2
        self.set(x, y, z, mat["vol"], keep=True)
        if not vol_blok(self.b.get((x, y, z), mat["vol"])):
            self.b[(x, y, z)] = mat["vol"]
        self.set(x, y + 1, z, mat["hek"])
        if lamp:
            self.set(x, y + 2, z, "minecraft:lantern[hanging=false,waterlogged=false]")

    # -------------------------------------------------------- output
    def verbind(self):
        """Fence / wall connections like in game (neighbours of this build only)."""
        dirs = {"east": (1, 0), "north": (0, -1), "south": (0, 1), "west": (-1, 0)}
        nieuw = {}
        for (x, y, z), s in self.b.items():
            naam = s.split("[")[0]
            hek = naam.endswith(HEK_FAM)
            muur = naam.endswith(MUUR_FAM)
            if not (hek or muur):
                continue
            con = {}
            for d, (dx, dz) in dirs.items():
                o = self.b.get((x + dx, y, z + dz))
                if o is None:
                    con[d] = False
                    continue
                on = o.split("[")[0]
                con[d] = on.endswith(HEK_FAM + MUUR_FAM) or vol_blok(o)
            if hek:
                props = ",".join(f"{d}={'true' if con[d] else 'false'}" for d in dirs)
                nieuw[(x, y, z)] = f"{naam}[{props},waterlogged=false]"
            else:
                recht = (con["north"] and con["south"] and not con["east"] and not con["west"]) or \
                        (con["east"] and con["west"] and not con["north"] and not con["south"])
                props = ",".join(f"{d}={'low' if con[d] else 'none'}" for d in dirs)
                nieuw[(x, y, z)] = f"{naam}[{props},up={'false' if recht else 'true'},waterlogged=false]"
        self.b.update(nieuw)

    def stempel(self, W, naam, bezet=False):
        import bouwsels as BS
        self.verbind()
        alle = list(self.b) + list(self.c)
        if not alle:
            return
        a = np.array(alle)
        lo, hi = a.min(0), a.max(0)
        S = BS.WS(lo[0], lo[1], lo[2], *(hi - lo + 1))
        for k in self.c:
            if k not in self.b:
                S.set(*k, CARVE)
        for k, s in self.b.items():
            S.set(*k, s)
        W.stempel(S.stempel(naam), bezet=bezet)


# ------------------------------------------------------------ queries on the world built so far
def bezetting(W, K, x0, y0, z0, X, Y, Z):
    """bool[X,Y,Z]: True where terrain or any stamp so far has a block (world box starting at x0,y0,z0)."""
    occ = np.zeros((X, Y, Z), bool)
    for st in W.stempels:
        ox, oy, oz = st.off
        sx, sy, sz = st.v.shape
        ax0, ay0, az0 = max(x0, ox), max(y0, oy), max(z0, oz)
        ax1, ay1, az1 = min(x0 + X, ox + sx), min(y0 + Y, oy + sy), min(z0 + Z, oz + sz)
        if ax0 >= ax1 or ay0 >= ay1 or az0 >= az1:
            continue
        g = st.lut[st.v[ax0 - ox:ax1 - ox, ay0 - oy:ay1 - oy, az0 - oz:az1 - oz]]
        occ[ax0 - x0:ax1 - x0, ay0 - y0:ay1 - y0, az0 - z0:az1 - z0] |= g > 1
    i0, j0 = x0 - KA.X0, z0 - KA.Z0
    H = K.H[i0:i0 + X, j0:j0 + Z].astype(np.int32)
    y = np.arange(y0, y0 + Y)[None, :, None]
    occ |= y <= H[:, None, :]
    return occ


def lijn_cellen(a, b, breed, rand=1.0):
    """Cells around segment a-b: yields (x, z, t, d) with d <= breed + rand (t = 0..1 along the segment)."""
    ax, az = a
    bx, bz = b
    dx, dz = bx - ax, bz - az
    L2 = dx * dx + dz * dz or 1
    m = breed + rand + 1
    for x in range(int(math.floor(min(ax, bx) - m)), int(math.ceil(max(ax, bx) + m)) + 1):
        for z in range(int(math.floor(min(az, bz) - m)), int(math.ceil(max(az, bz) + m)) + 1):
            t = ((x - ax) * dx + (z - az) * dz) / L2
            tc = min(1.0, max(0.0, t))
            d = math.hypot(x - (ax + tc * dx), z - (az + tc * dz))
            if d <= breed + rand:
                yield x, z, tc, d


def hoogte_op(t, La, Lb):
    return int(math.floor(La + (Lb - La) * t + 0.5))


# ------------------------------------------------------------ square spiral (stairs or slab ramp) around a core
RICHT = [("east", (1, 0)), ("south", (0, 1)), ("west", (-1, 0)), ("north", (0, -1))]


def _normaal(d):
    return d[1], -d[0]


def spiraal(cx, cz, a_in, w, L0, Ltop, mat, start=0, stijl="trap", lamp_elke=0, open_begin=3):
    """A square spiral path around (cx, cz): lanes at Chebyshev distance a_in .. a_in+w-1, rail at a_in+w.
    stijl "trap" rises 2 halves per side cell (stairs), "slab" 1 half. Corners are flat landings. After reaching Ltop the
    path runs one more ring flat = the top platform (minus the stairwell under it). Returns (Bouw, info)."""
    B = Bouw()
    dL = 2 if stijl == "trap" else 1
    stappen = []          # [cells, L, facing, rails]
    L = L0
    s = start
    ring = 8 * a_in
    na_top = None
    while True:
        dn, d = RICHT[s % 4]
        _, pd = RICHT[(s - 1) % 4]
        pn = _normaal(pd)
        cel = [(cx + i * pd[0] + j * pn[0], cz + i * pd[1] + j * pn[1]) for i in range(a_in, a_in + w) for j in range(a_in, a_in + w)]
        rl = [(cx + i * pd[0] + (a_in + w) * pn[0], cz + i * pd[1] + (a_in + w) * pn[1]) for i in range(a_in, a_in + w + 1)] + \
             [(cx + (a_in + w) * pd[0] + j * pn[0], cz + (a_in + w) * pd[1] + j * pn[1]) for j in range(a_in, a_in + w)]
        stappen.append([cel, L, None, rl])
        n = _normaal(d)
        for u in range(-(a_in - 1), a_in):
            fac = None
            if L < Ltop:
                L = min(Ltop, L + dL)
                fac = dn if stijl == "trap" else None
            cel = [(cx + u * d[0] + (a_in + k) * n[0], cz + u * d[1] + (a_in + k) * n[1]) for k in range(w)]
            rl = [(cx + u * d[0] + (a_in + w) * n[0], cz + u * d[1] + (a_in + w) * n[1])]
            stappen.append([cel, L, fac, rl])
        if L >= Ltop and na_top is None:
            na_top = len(stappen)
        if na_top is not None and len(stappen) - na_top >= ring:
            break
        s += 1
    # stairwell: a top-level cell over a lower step closer than 3.5 blocks is dropped
    laag = {}
    for cel, L, fac, rl in stappen:
        if L < Ltop:
            for c in cel:
                laag[c] = max(laag.get(c, -99), L)
    gat = set()
    for st in stappen:
        if st[1] == Ltop and st[2] is None:
            hier = [c for c in st[0] if laag.get(c, -99) > Ltop - 7]
            if hier:
                gat |= set(hier)
                st[0] = [c for c in st[0] if c not in hier]
                st[3] = []
    platform = set()
    for i, (cel, L, fac, rl) in enumerate(stappen):
        for (x, z) in cel:
            B.dek(x, z, L, mat, fac)
            if L == Ltop and fac is None:
                platform.add((x, z))
        if i < open_begin:
            continue
        for j, (x, z) in enumerate(rl):
            B.reling(x, z, L, mat, lamp=bool(lamp_elke) and i % lamp_elke == 0 and j == 0)
    # fence on the platform where it drops into the stairwell
    for (x, z) in platform:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, z + dz)
            if q in gat and Ltop - laag.get(q, Ltop) > 2:
                B.set(x, Ltop // 2, z, mat["hek"])
                break
    info = dict(platform=platform, gat=gat, begin=stappen[0][0], Ltop=Ltop, buiten=a_in + w - 1)
    return B, info


def voeg(B, B2):
    B.b.update(B2.b)
    B.c |= B2.c
