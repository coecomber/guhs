"""A fast numpy build volume in world coordinates (the zone-2 set pieces): box/cylinder/ellipsoid/cone fills by masks.

Index 0 = leave alone; "minecraft:cave_air" (CARVE) = make air. Becomes a tegels.Stempel via stempel(naam).
"""
import math

import numpy as np

import tegels as TG

CARVE = "minecraft:cave_air"


def trap(blok, facing, half="bottom", shape="straight"):
    return f"{blok}[facing={facing},half={half},shape={shape},waterlogged=false]"


def plaat(blok, typ="bottom"):
    return f"{blok}[type={typ},waterlogged=false]"


TEGEN = {"north": "south", "south": "north", "east": "west", "west": "east"}


class Vol:
    def __init__(self, x0, y0, z0, x1, y1, z1):
        """World box (inclusive corners)."""
        self.o = (int(x0), int(y0), int(z0))
        self.v = np.zeros((int(x1) - int(x0) + 1, int(y1) - int(y0) + 1, int(z1) - int(z0) + 1), np.uint16)
        self.pal = ["minecraft:air"]
        self.idx = {"minecraft:air": 0}

    def id(self, s):
        if ":" not in s.split("[")[0]:
            s = "guhs:" + s
        i = self.idx.get(s)
        if i is None:
            i = len(self.pal)
            self.pal.append(s)
            self.idx[s] = i
        return i

    def _sl(self, x1, y1, z1, x2, y2, z2):
        ox, oy, oz = self.o
        X, Y, Z = self.v.shape
        a = [min(x1, x2) - ox, min(y1, y2) - oy, min(z1, z2) - oz]
        b = [max(x1, x2) - ox + 1, max(y1, y2) - oy + 1, max(z1, z2) - oz + 1]
        a = [max(0, int(math.floor(v))) for v in a]
        b = [min(n, int(math.floor(v))) for v, n in zip(b, (X, Y, Z))]
        if any(bb <= aa for aa, bb in zip(a, b)):
            return None
        return slice(a[0], b[0]), slice(a[1], b[1]), slice(a[2], b[2])

    def box(self, x1, y1, z1, x2, y2, z2, s, keep=False):
        sl = self._sl(x1, y1, z1, x2, y2, z2)
        if sl is None:
            return
        i = self.id(s)
        if keep:
            sub = self.v[sl]
            sub[sub == 0] = i
        else:
            self.v[sl] = i

    def wanden(self, x1, y1, z1, x2, y2, z2, s):
        self.box(x1, y1, z1, x2, y2, z1, s)
        self.box(x1, y1, z2, x2, y2, z2, s)
        self.box(x1, y1, z1, x1, y2, z2, s)
        self.box(x2, y1, z1, x2, y2, z2, s)

    def set(self, x, y, z, s, keep=False):
        ox, oy, oz = self.o
        lx, ly, lz = int(math.floor(x)) - ox, int(math.floor(y)) - oy, int(math.floor(z)) - oz
        X, Y, Z = self.v.shape
        if 0 <= lx < X and 0 <= ly < Y and 0 <= lz < Z:
            if keep and self.v[lx, ly, lz]:
                return
            self.v[lx, ly, lz] = self.id(s)

    def get(self, x, y, z):
        ox, oy, oz = self.o
        lx, ly, lz = int(x) - ox, int(y) - oy, int(z) - oz
        X, Y, Z = self.v.shape
        if 0 <= lx < X and 0 <= ly < Y and 0 <= lz < Z:
            return self.pal[self.v[lx, ly, lz]]
        return "minecraft:air"

    def masker(self, x0, y0, z0, m, s, keep=False):
        """Apply a boolean mask whose [0,0,0] is world (x0, y0, z0)."""
        ox, oy, oz = self.o
        X, Y, Z = self.v.shape
        a = (x0 - ox, y0 - oy, z0 - oz)
        lo = [max(0, -aa) for aa in a]
        hi = [min(n, N - aa) for n, N, aa in zip(m.shape, (X, Y, Z), a)]
        if any(h <= l for l, h in zip(lo, hi)):
            return
        mm = m[lo[0]:hi[0], lo[1]:hi[1], lo[2]:hi[2]]
        sub = self.v[a[0] + lo[0]:a[0] + hi[0], a[1] + lo[1]:a[1] + hi[1], a[2] + lo[2]:a[2] + hi[2]]
        if keep:
            mm = mm & (sub == 0)
        sub[mm] = self.id(s)

    def cyl(self, cx, cz, r, y1, y2, s, hollow=0.0, keep=False):
        x0, z0 = int(math.floor(cx - r - 1)), int(math.floor(cz - r - 1))
        xs = np.arange(x0, int(cx + r + 2))[:, None] + 0.0
        zs = np.arange(z0, int(cz + r + 2))[None, :] + 0.0
        d = np.hypot(xs - cx, zs - cz)
        m2 = d <= r
        if hollow:
            m2 &= d > r - hollow
        m = np.repeat(m2[:, None, :], int(y2) - int(y1) + 1, axis=1)
        self.masker(x0, int(y1), z0, m, s, keep)

    def ell(self, cx, cy, cz, rx, ry, rz, s, hollow=0.0, keep=False, ymin=None, ymax=None):
        x0, y0, z0 = int(math.floor(cx - rx - 1)), int(math.floor(cy - ry - 1)), int(math.floor(cz - rz - 1))
        xs = np.arange(x0, int(cx + rx + 2))[:, None, None] + 0.0
        ys = np.arange(y0, int(cy + ry + 2))[None, :, None] + 0.0
        zs = np.arange(z0, int(cz + rz + 2))[None, None, :] + 0.0
        d = ((xs - cx) / rx) ** 2 + ((ys - cy) / ry) ** 2 + ((zs - cz) / rz) ** 2
        m = d <= 1
        if hollow:
            d2 = ((xs - cx) / max(0.5, rx - hollow)) ** 2 + ((ys - cy) / max(0.5, ry - hollow)) ** 2 + ((zs - cz) / max(0.5, rz - hollow)) ** 2
            m &= d2 > 1
        if ymin is not None:
            m &= ys >= ymin
        if ymax is not None:
            m &= ys <= ymax
        self.masker(x0, y0, z0, m, s, keep)

    def kegel(self, cx, cz, y0, h, r0, r1, s, keep=False, scheef=(0.0, 0.0)):
        """A (tilted) cone/frustum from radius r0 at y0 to r1 at y0+h; scheef = (dx, dz) shift of the top."""
        R = max(r0, r1) + abs(scheef[0]) + abs(scheef[1]) + 2
        x0, z0 = int(math.floor(cx - R)), int(math.floor(cz - R))
        n = int(2 * R) + 2
        xs = np.arange(x0, x0 + n)[:, None, None] + 0.5
        zs = np.arange(z0, z0 + n)[None, None, :] + 0.5
        ys = np.arange(int(y0), int(y0 + h) + 1)[None, :, None]
        t = (ys - y0) / max(1, h)
        mx = cx + scheef[0] * t
        mz = cz + scheef[1] * t
        r = r0 + (r1 - r0) * t
        m = np.hypot(xs - mx, zs - mz) <= r
        self.masker(x0, int(y0), z0, m, s, keep)

    def lijn(self, a, b, s, dik=0):
        """A 3D line of blocks from a to b (world points)."""
        n = int(max(abs(b[0] - a[0]), abs(b[1] - a[1]), abs(b[2] - a[2]))) + 1
        for i in range(n + 1):
            t = i / n
            x, y, z = (a[k] + (b[k] - a[k]) * t for k in range(3))
            if dik:
                self.box(round(x) - dik, round(y), round(z) - dik, round(x) + dik, round(y), round(z) + dik, s)
            else:
                self.set(round(x), round(y), round(z), s)

    def stempel(self, naam):
        return TG.Stempel(naam, self.v, self.pal, self.o)
