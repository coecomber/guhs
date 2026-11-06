"""U-GROTTEN: the cave engine. One hand-made atmosphere cave per zone (grotplan.py), built as a voxel "stamp" that every zone's map
maker applies to its tiles BEFORE hollowing (one hook per maker: ``grotten.pas_toe(vol, tx, tz, id_fn, namen, zone)``).

Geometry is 2.5D (a floor height + a ceiling per column) plus explicit 3D parts (spiral stairs, entrance structures, deco):
  * kamer  : an elliptic room with a flat floor S (stand height; .5 = a bottom slab) and a dome ceiling;
  * gang   : a tunnel along waypoints (x, z, S); the floor follows the line (max 0.5 per block, checked), so it is walkable;
  * meer   : a lake inside a room (bed, water top, ice patches);
  * water  : flooded tunnels / air pockets (zone 4);
  * trap   : a spiral stair (2 wide ring round a 3x3 glowing pillar, <= 0.5 per cell = slabs/blocks, 6 up per turn, head room 5);
The shell (2 blocks round every air/water cell) is filled with the cave's own rock where the tile is empty or soft, the inner
layer is re-skinned with the theme's wall blocks, but ONLY below the column's natural ground top, so a cave that reaches the
surface stays open there (mouths) and nothing is ever built in the open air by accident.

Op codes per cell (box arrays ``op``/``blk``):
  0 untouched | LUCHT air | ZACHT air unless wood/roots (zone 3 keeps the world tree's roots) | ZET force block |
  MUUR wall layer (overwrite below ground, keeps wood in zone 3) | VUL fill if empty/soft (below ground)
"""
import math
import os
import sys

import numpy as np
from scipy import ndimage

HIER = os.path.dirname(os.path.abspath(__file__))
T, HOOG = 64, 320
LUCHT, ZACHT, ZET, MUUR, VUL = 1, 2, 3, 4, 5
STIJG = 6.0          # spiral stairs: blocks up per full turn (inner lane steps stay <= 0.5)


# ------------------------------------------------------------------------------------------------ small helpers
def _hash(x, z, zaad):
    h = (np.asarray(x, np.int64) * 374761393 + np.asarray(z, np.int64) * 668265263 + zaad * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def ruis(x, z, zaad, cel=8.0):
    """Smooth value noise in [-1, 1] (bilinear, deterministic)."""
    fx, fz = np.asarray(x, float) / cel, np.asarray(z, float) / cel
    x0, z0 = np.floor(fx).astype(np.int64), np.floor(fz).astype(np.int64)
    tx, tz = fx - x0, fz - z0
    tx, tz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
    a, b = _hash(x0, z0, zaad), _hash(x0 + 1, z0, zaad)
    c, d = _hash(x0, z0 + 1, zaad), _hash(x0 + 1, z0 + 1, zaad)
    return (a + (b - a) * tx + (c - a) * tz + (a - b - c + d) * tx * tz) * 2 - 1


def splits(s):
    naam, _, rest = s.partition("[")
    props = {}
    if rest:
        for kv in rest.rstrip("]").split(","):
            k, v = kv.split("=")
            props[k] = v
    return naam, props


def is_hout(naam):
    n = naam.split("[")[0]
    return n.endswith(("_log", "_wood", "_roots", "_stem")) and "mushroom" not in n or n in ("minecraft:mangrove_roots", "minecraft:muddy_mangrove_roots")


_ZACHT_WOORDEN = ("air", "water", "grass", "fern", "flower", "tulip", "poppy", "dandelion", "allium", "bluet", "orchid", "daisy", "petals",
                  "leaves", "bladeren", "_blad", "vine", "snow[", "seagrass", "kelp", "carpet", "mushroom[", "bush", "sapling", "lily",
                  "coral_fan", "coral[", "sea_pickle", "glimpaddenstoel", "schimmelpaddenstoel", "riet", "guhbloem", "hanging_roots",
                  "glow_lichen", "spore_blossom", "pointed_dripstone", "lantern", "torch", "light")
_GEEN_GROND = ("leaves", "bladeren", "_blad", "mushroom_block", "mushroom_stem", "paddenstoel_hoed", "paddenstoel_steel", "_log", "_wood",
               "lantern", "torch", "fence", "wall[", "_wall", "glass", "pane", "chain", "carpet", "flower", "grass[", "short_grass", "tall_grass",
               "fern", "vine", "light", "barrier", "slab", "stairs", "trap", "planks", "wool", "door", "banner", "bell", "candle",
               "kelp", "seagrass", "lily", "sapling", "bush", "petals", "pickle", "coral_fan", "snow[", "rail", "button", "plate")


def _zacht(s):
    if s in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void"):
        return True
    return any(w in s for w in _ZACHT_WOORDEN)


def _grond(s):
    n = s.split("[")[0]
    if n in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void"):
        return False
    if "water" in n and "waterlogged" not in n:
        return True
    return not any(w in s for w in _GEEN_GROND)


# ------------------------------------------------------------------------------------------------ the cave
class Grot:
    """One cave: elements in WORLD coordinates (the plan converts world-local zone coordinates via indeling.py)."""

    def __init__(self, gid, zone, zaad=1):
        self.id, self.zone, self.zaad = gid, zone, zaad
        self.kamers, self.gangen, self.meren, self.trappen = [], [], [], []
        self.bouwwerk = []          # callables (grot) -> None that run after the shell (explicit 3D parts)
        self.deco = []              # callables (grot, rng) -> None
        self.monden = []            # (x, y, z, r): no shell near an opening (z4: the underwater mouth)
        self.kolom = []             # (x, z, dy, state): a block relative to the column's ground top (dy 0 = replace the top, 1 = on it)
        self.bewaar_hout = False
        self.thema = {}
        self.plekken = {}
        self.vrij = []              # (x, z, r): keep free of deco (puzzle boxes, chests, spots)
        self.gebouwd = False

    # ---- elements ----------------------------------------------------------------------------------------------------------
    def kamer(self, naam, cx, cz, rx, rz, S, h, hmin=4.0, water=None, wobbel=0.12):
        k = dict(naam=naam, cx=cx, cz=cz, rx=rx, rz=rz, S=float(S), h=h, hmin=hmin, water=water, wobbel=wobbel, i=len(self.kamers))
        self.kamers.append(k)
        return k

    def gang(self, punten, breed=3.4, h=4.2, water=None):
        g = dict(punten=[(float(x), float(z), float(S)) for (x, z, S) in punten], breed=breed, h=h, water=water)
        self.gangen.append(g)
        return g

    def meer(self, cx, cz, rx, rz, top, bodem, ijs=0.0):
        self.meren.append(dict(cx=cx, cz=cz, rx=rx, rz=rz, top=top, bodem=float(bodem), ijs=ijs))

    def trap(self, cx, cz, S_top, S_bot, theta_bot, opening=True):
        """A spiral from S_bot (its lowest tread points at theta_bot) up to S_top. Returns theta_top (where the top arrives)."""
        dS = S_top - S_bot
        th_top = theta_bot + 2 * math.pi * dS / STIJG
        self.trappen.append(dict(cx=cx, cz=cz, top=float(S_top), bot=float(S_bot), th_bot=theta_bot, th_top=th_top, open=opening))
        return th_top % (2 * math.pi)

    # ---- build -------------------------------------------------------------------------------------------------------------
    def _doos(self):
        xs, zs, ys_lo, ys_hi = [], [], [], []
        for k in self.kamers:
            xs += [k["cx"] - k["rx"] * 1.3, k["cx"] + k["rx"] * 1.3]
            zs += [k["cz"] - k["rz"] * 1.3, k["cz"] + k["rz"] * 1.3]
            ys_lo.append(k["S"] - 3)
            ys_hi.append(k["S"] + k["h"] + 4)
        for g in self.gangen:
            for (x, z, S) in g["punten"]:
                xs += [x - g["breed"] - 2, x + g["breed"] + 2]
                zs += [z - g["breed"] - 2, z + g["breed"] + 2]
                ys_lo.append(S - 3)
                ys_hi.append(S + g["h"] + 4)
        for t in self.trappen:
            xs += [t["cx"] - 14, t["cx"] + 14]
            zs += [t["cz"] - 14, t["cz"] + 14]
            ys_lo.append(t["bot"] - 3)
            ys_hi.append(t["top"] + 26)
        m = 4
        self.x0, self.z0 = int(math.floor(min(xs))) - m, int(math.floor(min(zs))) - m
        self.y0 = max(1, int(math.floor(min(ys_lo))) - m)
        x1, z1 = int(math.ceil(max(xs))) + m, int(math.ceil(max(zs))) + m
        y1 = min(HOOG - 1, int(math.ceil(max(ys_hi))) + m)
        self.X, self.Y, self.Z = x1 - self.x0 + 1, y1 - self.y0 + 1, z1 - self.z0 + 1

    def bouw(self):
        if self.gebouwd:
            return self
        self._doos()
        X, Y, Z = self.X, self.Y, self.Z
        self.pal = ["minecraft:air"]
        self.pidx = {"minecraft:air": 0}
        self.op = np.zeros((X, Y, Z), np.int8)
        self.blk = np.zeros((X, Y, Z), np.int16)
        xx, zz = np.meshgrid(np.arange(X) + self.x0, np.arange(Z) + self.z0, indexing="ij")
        self.xx, self.zz = xx, zz
        F = np.full((X, Z), np.nan)
        C = np.full((X, Z), -999, np.int32)
        Wt = np.full((X, Z), -999, np.int32)
        eig = np.full((X, Z), -1, np.int32)        # owner: room index, 100+ tunnel index
        pad = np.zeros((X, Z), bool)               # walkway (tunnel centre lines): no deco
        ijs = np.zeros((X, Z), bool)
        z_ = self.zaad
        # rooms
        for k in self.kamers:
            dx, dz = (xx - k["cx"]) / k["rx"], (zz - k["cz"]) / k["rz"]
            hoek = np.arctan2(dz, dx)
            reff = 1 + k["wobbel"] * ruis(np.cos(hoek) * 9 + k["i"] * 31, np.sin(hoek) * 9, z_ + 3 + k["i"], 3.0)
            d = np.sqrt(dx * dx + dz * dz) / reff
            m = (d <= 1.0) & (np.isnan(F) | (eig >= 100))
            S = k["S"]
            a = math.ceil(S)
            dak = a + k["hmin"] + (k["h"] - k["hmin"]) * np.sqrt(np.clip(1 - d * d, 0, 1)) + 1.2 * ruis(xx, zz, z_ + 11 + k["i"], 4.0)
            F[m] = S
            C[m] = np.maximum(np.where(m, np.round(dak).astype(np.int32), -999), C)[m]
            eig[m] = k["i"]
            if k["water"] is not None:
                Wt[m] = k["water"]
        # tunnels
        for gi, g in enumerate(self.gangen):
            P = g["punten"]
            best = np.full((X, Z), 1e9)
            Sbest = np.zeros((X, Z))
            for (ax, az, aS), (bx, bz, bS) in zip(P[:-1], P[1:]):
                vx, vz = bx - ax, bz - az
                L2 = max(1e-6, vx * vx + vz * vz)
                t = np.clip(((xx - ax) * vx + (zz - az) * vz) / L2, 0, 1)
                px, pz = ax + t * vx, az + t * vz
                dd = np.hypot(xx - px, zz - pz)
                beter = dd < best
                best = np.where(beter, dd, best)
                Sbest = np.where(beter, aS + t * (bS - aS), Sbest)
            half = g["breed"] / 2 + 0.6 * ruis(xx, zz, z_ + 21 + gi, 5.0)
            m = best <= half
            pad |= best <= 0.9
            Sq = np.round(Sbest * 2) / 2
            vrij = m & np.isnan(F)
            F[vrij] = Sq[vrij]
            eig[vrij] = 100 + gi
            a = np.ceil(Sq)
            dak = np.round(a + g["h"] + 0.8 * ruis(xx, zz, z_ + 31 + gi, 3.0) - 0.8 * (best / np.maximum(0.5, half)) ** 2).astype(np.int32)
            C = np.where(m, np.maximum(C, dak), C)
            if g["water"] is not None:
                Wt = np.where(vrij, g["water"], Wt)
        # lakes (inside rooms): the bed, the water top, ice patches
        for mi, me in enumerate(self.meren):
            dx, dz = (xx - me["cx"]) / me["rx"], (zz - me["cz"]) / me["rz"]
            d = np.sqrt(dx * dx + dz * dz) * (1 + 0.1 * ruis(xx, zz, z_ + 41 + mi, 3.0))
            m = d <= 1.0
            bed = np.where(d > 0.75, me["top"], me["bodem"])     # a 1-block-deep rim, then the deep part
            F[m] = bed[m]
            Wt[m] = me["top"]
            # ice only on the 1-deep rim (a frozen shore ring): the deep middle stays open, nobody gets stuck under ice
            ijs |= m & (d > 0.75) & (me["ijs"] > 0)
        self.F, self.C, self.Wt, self.eig, self.pad, self.ijs = F, C, Wt, eig, pad, ijs
        self._controleer_vloer()
        th = self.thema
        # 3D: air / water / floors
        for i in range(X):
            for j in range(Z):
                S = F[i, j]
                if np.isnan(S):
                    continue
                a = int(math.ceil(S))
                c = int(C[i, j])
                w = int(Wt[i, j])
                for y in range(a, c):
                    yi = y - self.y0
                    if not 0 <= yi < Y:
                        continue
                    if y <= w:
                        self._zet_i(i, yi, j, ZET, "minecraft:ice" if (ijs[i, j] and y == w) else "minecraft:water")
                    elif self.bewaar_hout and y >= a + 3:
                        self.op[i, yi, j] = ZACHT
                    else:
                        self.op[i, yi, j] = LUCHT
                halfje = S != a
                if halfje:
                    self._zet_i(i, a - 1 - self.y0, j, ZET, self._kies(th["plaat"], i, j, a - 1))
                    self._zet_i(i, a - 2 - self.y0, j, ZET, self._kies(th["vloer"], i, j, a - 2))
                else:
                    self._zet_i(i, a - 1 - self.y0, j, ZET, self._kies(th["vloer"], i, j, a - 1))
                    self._zet_i(i, a - 2 - self.y0, j, ZET, self._kies(th["vul"], i, j, a - 2))
        for t in self.trappen:
            self._trap(t)
        self._schil()
        self._controleer_water()
        for f in self.bouwwerk:
            f(self)
        for k in ("binnen", "hal", "elite", "schat", "ingang", "uitgang"):     # stand POIs stay free of deco
            if k in self.plekken:
                v = self.plekken[k]
                self.vrij.append((v[0], v[2], 1.6))
        rng = np.random.default_rng(self.zaad * 7919)
        for f in self.deco:
            f(self, rng)
        self.gebouwd = True
        return self

    def _controleer_vloer(self):
        """Walkability: neighbouring dry floor cells never differ by more than 0.5 (a slab step)."""
        F, W = self.F, self.Wt
        droog = ~np.isnan(F) & (W < -900)
        fout = []
        for (di, dj) in ((1, 0), (0, 1)):
            a = F[:F.shape[0] - di, :F.shape[1] - dj]
            b = F[di:, dj:]
            m = droog[:F.shape[0] - di, :F.shape[1] - dj] & droog[di:, dj:]
            dif = np.abs(a - b)
            bad = m & (dif > 0.5)
            for i, j in zip(*np.nonzero(bad)):
                fout.append((int(i + self.x0), int(j + self.z0), float(a[i, j]), float(b[i, j])))
        self.vloerfouten = fout
        if fout:
            print(f"grot {self.id}: {len(fout)} vloerstappen > 0.5 (eerste: {fout[:4]})", flush=True)

    def _controleer_water(self):
        """Water must never sit next to cave air at the same height (it would flow into the room)."""
        wid = self.pidx.get("minecraft:water")
        if wid is None:
            self.waterfouten = 0
            return
        w = (self.op == ZET) & (self.blk == wid)
        lucht = (self.op == LUCHT) | (self.op == ZACHT)
        n = 0
        voorbeeld = None
        for (di, dj) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            b = np.roll(lucht, (di, dj), axis=(0, 2))
            m = w & b
            if m.any() and voorbeeld is None:
                i, y, j = [int(v[0]) for v in np.nonzero(m)]
                voorbeeld = (i + self.x0, y + self.y0, j + self.z0)
            n += int(m.sum())
        self.waterfouten = n
        if n:
            print(f"grot {self.id}: {n} watercellen naast lucht (bv. {voorbeeld})", flush=True)

    # ---- the spiral stair --------------------------------------------------------------------------------------------------
    def _trap(self, t):
        th = self.thema
        cx, cz = t["cx"], t["cz"]
        top, bot = t["top"], t["bot"]
        for x in range(int(cx) - 4, int(cx) + 5):
            for z in range(int(cz) - 4, int(cz) + 5):
                dx, dz = x - cx, z - cz
                r2 = dx * dx + dz * dz
                if r2 > 3.6 ** 2:
                    continue
                kern = max(abs(dx), abs(dz)) <= 1.01
                # carve the shaft from the bottom floor up to above the top (the top structure closes it if needed)
                for y in range(int(math.ceil(bot)), int(top) + 7):
                    if kern and y > top + 2:
                        self.zet(x, y, z, None, op=LUCHT)
                    else:
                        self.zet(x, y, z, None if not kern else self._kies(th["pilaar"], x, z, y), op=LUCHT if not kern else ZET)
                # the bottom floor under the shaft
                a = int(math.ceil(bot))
                self.zet(x, a - 1, z, self._kies(th["vloer"], x, z, a - 1))
                self.zet(x, a - 2, z, self._kies(th["vul"], x, z, a - 2))
                if kern:
                    continue
                hoek = math.atan2(dz, dx)
                # every turn of the helix that passes this cell gets a tread (STIJG up per turn)
                d0 = (hoek - t["th_bot"]) % (2 * math.pi)
                span = t["th_top"] - t["th_bot"]
                hoogste = bot
                k = 0
                while d0 + 2 * math.pi * k <= span + 1e-6:
                    S = bot + (d0 + 2 * math.pi * k) / (2 * math.pi) * STIJG
                    S = math.floor(S * 2 + 1e-6) / 2
                    hoogste = S
                    if S > bot:
                        a = int(math.ceil(S))
                        if S != a:
                            self.zet(x, a - 1, z, self._kies(th["trap_plaat"], x, z, a - 1))
                        else:
                            self.zet(x, a - 1, z, self._kies(th["trap_blok"], x, z, a - 1))
                    k += 1
                # guard at the top: a fence where the highest tread of this cell is well below the top level
                if t["open"] and hoogste <= top - 3:
                    self.zet(x, int(math.ceil(top)), z, th.get("hek", "minecraft:spruce_fence"))
        self.vrij.append((cx, cz, 4.5))

    # ---- shell -------------------------------------------------------------------------------------------------------------
    def _schil(self):
        th = self.thema
        binnen = (self.op == LUCHT) | (self.op == ZACHT)
        water = np.zeros_like(binnen)
        wid = self.pidx.get("minecraft:water")
        iid = self.pidx.get("minecraft:ice")
        if wid is not None:
            water |= (self.op == ZET) & (self.blk == wid)
        if iid is not None:
            water |= (self.op == ZET) & (self.blk == iid)
        kern = binnen | water
        bezet = self.op != 0
        s = np.ones((3, 3, 3), bool)
        d1 = ndimage.binary_dilation(kern, s) & ~bezet
        d2 = ndimage.binary_dilation(kern | d1, s) & ~bezet & ~d1
        # mouths: no shell there
        if self.monden:
            X, Y, Z = self.op.shape
            gx, gy, gz = np.meshgrid(np.arange(X) + self.x0, np.arange(Y) + self.y0, np.arange(Z) + self.z0, indexing="ij")
            for (mx, my, mz, r) in self.monden:
                m = (gx - mx) ** 2 + (gy - my) ** 2 + (gz - mz) ** 2 <= r * r
                d1 &= ~m
                d2 &= ~m
        for (soort, masker, lijst) in ((MUUR, d1, th["muur"]), (VUL, d2, th["vul"])):
            ii, yy, jj = np.nonzero(masker)
            if not len(ii):
                continue
            keuze = self._kies_v(lijst, ii + self.x0, yy + self.y0, jj + self.z0)
            self.op[ii, yy, jj] = soort
            self.blk[ii, yy, jj] = keuze

    # ---- palette / setting -------------------------------------------------------------------------------------------------
    def bid(self, s):
        i = self.pidx.get(s)
        if i is None:
            i = self.pidx[s] = len(self.pal)
            self.pal.append(s)
        return i

    def _kies(self, lijst, x, z, y=0):
        """A weighted pick from [(state, weight), ...] (or a plain state), deterministic per position."""
        if isinstance(lijst, str):
            return lijst
        tot = sum(w for _, w in lijst)
        r = float(_hash(int(x) * 31 + int(y) * 7, int(z), self.zaad + 97)) * tot
        for s, w in lijst:
            r -= w
            if r <= 0:
                return s
        return lijst[-1][0]

    def _kies_v(self, lijst, xs, ys, zs):
        if isinstance(lijst, str):
            return np.full(len(xs), self.bid(lijst), np.int16)
        ids = np.array([self.bid(s) for s, _ in lijst], np.int16)
        w = np.cumsum([wt for _, wt in lijst], dtype=float)
        # blotchy: half noise, half hash, so patches form
        r = (_hash(xs * 31 + ys * 7, zs, self.zaad + 97) * 0.5 + (ruis(xs + ys * 0.5, zs - ys * 0.3, self.zaad + 5, 4.0) * 0.5 + 0.5) * 0.5)
        k = np.searchsorted(w / w[-1], np.clip(r, 0, 0.999999))
        return ids[k]

    def _zet_i(self, i, yi, j, op, s):
        if 0 <= i < self.X and 0 <= yi < self.Y and 0 <= j < self.Z:
            self.op[i, yi, j] = op
            self.blk[i, yi, j] = 0 if s is None else self.bid(s)

    def zet(self, x, y, z, s, op=ZET):
        self._zet_i(int(round(x)) - self.x0, int(y) - self.y0, int(round(z)) - self.z0, op, s)

    def lees(self, x, y, z):
        """(op, state) at a world cell (0, None outside the box)."""
        i, yi, j = int(round(x)) - self.x0, int(y) - self.y0, int(round(z)) - self.z0
        if 0 <= i < self.X and 0 <= yi < self.Y and 0 <= j < self.Z:
            o = int(self.op[i, yi, j])
            return o, (self.pal[self.blk[i, yi, j]] if o in (ZET, MUUR, VUL) else None)
        return 0, None

    def is_lucht(self, x, y, z):
        o, _ = self.lees(x, y, z)
        return o in (LUCHT, ZACHT)

    def is_vast(self, x, y, z):
        o, s = self.lees(x, y, z)
        return o in (MUUR, VUL) or (o == ZET and s not in ("minecraft:water", "minecraft:ice") and not _zacht(s or ""))

    # ---- queries for deco ------------------------------------------------------------------------------------------------
    def stand(self, x, z):
        """Stand y of the floor in this column (None outside / water)."""
        i, j = int(round(x)) - self.x0, int(round(z)) - self.z0
        if not (0 <= i < self.X and 0 <= j < self.Z) or np.isnan(self.F[i, j]) or self.Wt[i, j] > -900:
            return None
        return int(math.ceil(self.F[i, j]))

    def is_halfje(self, x, z):
        i, j = int(round(x)) - self.x0, int(round(z)) - self.z0
        return 0 <= i < self.X and 0 <= j < self.Z and not np.isnan(self.F[i, j]) and self.F[i, j] != math.ceil(self.F[i, j])

    def bezet_vrij(self, x, z, extra=0.0):
        for (vx, vz, r) in self.vrij:
            if (x - vx) ** 2 + (z - vz) ** 2 <= (r + extra) ** 2:
                return True
        return False

    def vloercellen(self, eigenaar=None, rand=None, pad_ok=False, heel=True):
        """[(x, y, z)] stand positions on dry floor (air above for 2), optionally only in room `eigenaar` (index), only within
        `rand` = (dmin, dmax) of that room's normalised radius."""
        uit = []
        for i in range(self.X):
            for j in range(self.Z):
                S = self.F[i, j]
                if np.isnan(S) or self.Wt[i, j] > -900:
                    continue
                if heel and S != math.ceil(S):
                    continue
                e = int(self.eig[i, j])
                if eigenaar is not None and e != eigenaar:
                    continue
                if not pad_ok and self.pad[i, j]:
                    continue
                x, z = i + self.x0, j + self.z0
                y = int(math.ceil(S))
                if not (self.is_lucht(x, y, z) and self.is_lucht(x, y + 1, z)):
                    continue
                if self.bezet_vrij(x, z):
                    continue
                if rand is not None and e >= 0 and e < 100:
                    k = self.kamers[e]
                    d = math.hypot((x - k["cx"]) / k["rx"], (z - k["cz"]) / k["rz"])
                    if not rand[0] <= d <= rand[1]:
                        continue
                uit.append((x, y, z))
        return uit

    def plafondcellen(self, eigenaar=None):
        """[(x, y, z)]: the highest air cell of a column with solid rock right above (for hanging things)."""
        uit = []
        for i in range(self.X):
            for j in range(self.Z):
                if np.isnan(self.F[i, j]) or (eigenaar is not None and int(self.eig[i, j]) != eigenaar):
                    continue
                c = int(self.C[i, j]) - 1
                x, z = i + self.x0, j + self.z0
                if self.is_lucht(x, c, z) and self.is_vast(x, c + 1, z) and c - int(math.ceil(self.F[i, j])) >= 3:
                    uit.append((x, c, z))
        return uit

    def langs_gangen(self, stap=7.0, kant=1.0):
        """Points along every tunnel, every `stap` blocks, pushed to one side: [(x, y, z, richting)]."""
        uit = []
        for gi, g in enumerate(self.gangen):
            if g["water"] is not None:
                continue
            P = g["punten"]
            s_ = 3.0
            teller = 0
            for (ax, az, aS), (bx, bz, bS) in zip(P[:-1], P[1:]):
                L = math.hypot(bx - ax, bz - az)
                while s_ < L:
                    t = s_ / L
                    nx, nz = -(bz - az) / L, (bx - ax) / L
                    zij = kant * (1 if teller % 2 == 0 else -1) * max(0.8, g["breed"] / 2 - 0.4)
                    x, z = ax + (bx - ax) * t + nx * zij, az + (bz - az) * t + nz * zij
                    y = self.stand(x, z)
                    if y is not None and not self.is_halfje(x, z):
                        uit.append((int(round(x)), y, int(round(z)), (nx * zij, nz * zij)))
                    teller += 1
                    s_ += stap
                s_ -= L
        return uit


# ------------------------------------------------------------------------------------------------ apply to a tile
_LUT = {}


def _luts(namen):
    n = len(namen)
    key = id(namen)
    oud = _LUT.get(key)
    if oud is not None and oud[0] == n:
        return oud[1], oud[2], oud[3]
    zacht = np.array([_zacht(s) for s in namen] + [False] * 4096, bool)
    grond = np.array([_grond(s) for s in namen] + [False] * 4096, bool)
    hout = np.array([is_hout(s) for s in namen] + [False] * 4096, bool)
    zacht[0] = True
    if n > 1 and namen[1] in ("minecraft:cave_air",):
        zacht[1] = True
        grond[1] = False
    _LUT[key] = (n, zacht, grond, hout)
    return zacht, grond, hout


def pas_toe_grot(g, vol, tx, tz, id_fn, namen, conflict=None):
    """Applies cave g to tile (tx, tz): vol[x, y, z] uint16 global ids (pre-hollow), id_fn(state) -> global id, namen = the
    global palette list (index = id; may grow while id_fn adds)."""
    g.bouw()
    bx0, bx1 = max(g.x0, tx * T), min(g.x0 + g.X, tx * T + T)
    bz0, bz1 = max(g.z0, tz * T), min(g.z0 + g.Z, tz * T + T)
    if bx0 >= bx1 or bz0 >= bz1:
        return 0
    gids = np.array([id_fn(s) if i else 0 for i, s in enumerate(g.pal)], np.uint16)
    zacht, grond, hout = _luts(namen)
    ix0, ix1, iz0, iz1 = bx0 - tx * T, bx1 - tx * T, bz0 - tz * T, bz1 - tz * T
    gx0, gx1, gz0, gz1 = bx0 - g.x0, bx1 - g.x0, bz0 - g.z0, bz1 - g.z0
    y0, y1 = g.y0, g.y0 + g.Y
    kol = vol[ix0:ix1, :, iz0:iz1]
    gm = grond[kol]
    heeft = gm.any(1)
    top = np.where(heeft, HOOG - 1 - np.argmax(gm[:, ::-1, :], axis=1), -1)   # (nx, nz) natural ground top
    # a column without ground (a giant mushroom stem or a trunk stands on it) takes the highest ground round it
    if (top < 0).any() and (top >= 0).any():
        rond = ndimage.maximum_filter(top, size=9, mode="nearest")
        top = np.where(top < 0, rond, top)
    sub = vol[ix0:ix1, y0:y1, iz0:iz1]
    op = g.op[gx0:gx1, :, gz0:gz1]
    bl = gids[g.blk[gx0:gx1, :, gz0:gz1]]
    ys = np.arange(y0, y1)[None, :, None]
    onder = ys <= top[:, None, :]
    houtig = hout[sub]
    n = 0
    if conflict is not None:
        # cave air where the pre-cave world had an enclosed empty cell (another room / cave): reported, still carved
        bodem = np.where(heeft, np.argmax(gm, axis=1), HOOG)
        leeg = (sub == 0) & ((op == LUCHT) | (op == ZACHT)) & onder & (ys > bodem[:, None, :])
        conflict[0] += int(leeg.sum())
    m = op == LUCHT
    sub[m] = 0
    n += int(m.sum())
    m = (op == ZACHT) & ~houtig
    sub[m] = 0
    m = op == ZET
    sub[m] = bl[m]
    n += int(m.sum())
    m = (op == MUUR) & onder & ~(houtig & g.bewaar_hout)
    sub[m] = bl[m]
    m = (op == VUL) & onder & zacht[sub]
    sub[m] = bl[m]
    # column ops (ground-relative)
    for (x, z, dy, s) in g.kolom:
        if bx0 <= x < bx1 and bz0 <= z < bz1:
            t = int(top[x - bx0, z - bz0])
            if t >= 0 and 0 <= t + dy < HOOG:
                vol[x - tx * T, t + dy, z - tz * T] = id_fn(s)
    return n


# ------------------------------------------------------------------------------------------------ registry for the makers
_GROTTEN = None


def grotten(zone=None):
    global _GROTTEN
    if _GROTTEN is None:
        sys.path.insert(0, HIER)
        import grotplan
        _GROTTEN = grotplan.alle()
    return [g for g in _GROTTEN if zone is None or g.zone == zone]


def staten(zone):
    """Every block state the caves of a zone may write (pre-register before an opaque lookup is computed)."""
    uit = []
    for g in grotten(zone):
        g.bouw()
        uit += list(g.pal[1:])
        uit += [s for (_, _, _, s) in g.kolom]
    return uit


def pas_toe(vol, tx, tz, id_fn, namen, zone):
    """THE hook for the map makers: apply every cave of `zone` that touches world tile (tx, tz)."""
    n = 0
    c = [0]
    for g in grotten(zone):
        n += pas_toe_grot(g, vol, tx, tz, id_fn, namen, c)
    if c[0] > 50:
        print(f"grotten: tegel {tx},{tz}: {c[0]} grotcellen in een al bestaande holte (zone {zone})", flush=True)
    return n


def tegels(zone=None):
    """World tiles the caves of a zone touch (for --alleen runs)."""
    uit = set()
    for g in grotten(zone):
        g.bouw()
        for tx in range(g.x0 // T, (g.x0 + g.X - 1) // T + 1):
            for tz in range(g.z0 // T, (g.z0 + g.Z - 1) // T + 1):
                uit.add((tx, tz))
    return sorted(uit)
