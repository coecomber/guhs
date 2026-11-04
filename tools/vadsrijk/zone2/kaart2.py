"""The 2D plan of zone 2 "De Splinterpieken": heights, masks and areas over the zone-2 tile grid (indexed [x - X0, z - Z0]).

Layout: plan.py. The massif rises out of a cloud sea; Kandijkom sits in a flat geode bowl in the middle (floor 90), the
Galmhellingen climb east to snowy crystal peaks, the Bevroren Knuffelmeer lies in a basin in the west, the Splinterkloof cuts
east-west through the high north plateau and the Spiegelburcht peak stands north of it.
"""
import math

import numpy as np
from scipy import ndimage

import plan as PL

NX, NZ, X0, Z0 = PL.NX, PL.NZ, PL.X0, PL.Z0
XX, ZZ = np.meshgrid(np.arange(X0, X0 + NX, dtype=np.float32), np.arange(Z0, Z0 + NZ, dtype=np.float32), indexing="ij")


def ruis(seed, schalen):
    rng = np.random.default_rng(seed)
    out = np.zeros((NX, NZ), np.float32)
    for (cel, amp) in schalen:
        gx, gz = NX // cel + 4, NZ // cel + 4
        g = rng.random((gx, gz)).astype(np.float32) * 2 - 1
        up = ndimage.zoom(g, cel, order=3, mode="nearest")
        out += amp * up[cel:cel + NX, cel:cel + NZ]
    return out


def polyafstand(pts, xx=None, zz=None):
    xx = XX if xx is None else xx
    zz = ZZ if zz is None else zz
    best = np.full(xx.shape, 1e9, np.float32)
    langs = np.zeros(xx.shape, np.float32)
    acc = 0.0
    for (ax, az), (bx, bz) in zip(pts, pts[1:]):
        dx, dz = bx - ax, bz - az
        L2 = dx * dx + dz * dz
        t = np.clip(((xx - ax) * dx + (zz - az) * dz) / L2, 0, 1)
        d = np.hypot(xx - (ax + t * dx), zz - (az + t * dz))
        m = d < best
        best = np.where(m, d, best)
        langs = np.where(m, acc + t * math.sqrt(L2), langs)
        acc += math.sqrt(L2)
    return best, langs


def idx(x, z):
    return int(x - X0), int(z - Z0)


def smooth(t):
    t = np.clip(t, 0, 1)
    return t * t * (3 - 2 * t)


def kloof_z(x):
    """Centre line z of the Splinterkloof at x (the polyline runs west -> east)."""
    xs = [p[0] for p in PL.KLOOF]
    zs = [p[1] for p in PL.KLOOF]
    return np.interp(x, xs, zs)


SOORTEN = ["sneeuw", "galm", "kloof", "burcht", "geode_roze", "geode_calciet", "geode_basalt", "oever"]


class Kaart2:
    def __init__(self):
        self.n1 = ruis(21, [(170, 1.0), (64, 0.5), (24, 0.25), (9, 0.1)])
        self.n2 = ruis(23, [(320, 1.0), (120, 0.6), (40, 0.3)])
        self.fijn = ruis(29, [(12, 1.0), (5, 0.5)])
        self.rug = np.clip(1 - np.abs(ruis(31, [(110, 1.0), (45, 0.5), (18, 0.2)])), 0, 1)
        self.pads = []        # (x, z, r, top, fall) flattened pads (kept exactly)
        self.land_hoogte()
        self.zee()

    # ------------------------------------------------------------------------------------------------ heights
    def land_hoogte(self):
        x, z, n1, n2, f, rug = XX, ZZ, self.n1, self.n2, self.fijn, self.rug
        zx0, zz0, zx1, zz1 = PL.ZONE
        rand = np.minimum.reduce([x - (zx0 - 40), (zx1 + 40) - x, z - (zz0 - 40), (zz1 + 40) - z]) + n1 * 14
        self.land = rand > 0
        kustd = ndimage.distance_transform_edt(self.land)
        self.kustd = kustd
        # ---- base massif (compact island: everything within ~300 of Kandijkom)
        H = 100 + 6 * n2 + 3 * n1 + 1.2 * f
        H -= np.clip((z + 2330) / 120, 0, 1) * 10           # the south sinks towards the dock
        # Galmhellingen: rising east to ridged crystal peaks
        oost = smooth((x - 75) / 150)
        H += oost * (24 + 22 * rug ** 1.6 + 6 * n1)
        px_, pz_ = PL.GALMPIEK
        H += 54 * np.exp(-(((x - px_) / 34) ** 2 + ((z - pz_) / 30) ** 2))
        # the west: hills round the lake basin
        west = smooth((-x - 80) / 140)
        H += west * (8 + 10 * rug ** 2 + 4 * n1)
        # the high north plateau (south rim of the Splinterkloof, then the north plateau)
        noord = smooth((-z - 2468) / 46)
        H = H * (1 - noord) + np.maximum(H, PL.KLOOF_RAND_ZUID + 4 * n1 + 3 * rug) * noord
        noord2 = smooth((-z - 2556) / 14)
        H = H * (1 - noord2) + (PL.KLOOF_RAND_NOORD + 8 * n1 + 12 * rug ** 2) * noord2
        # ---- the Spiegelburcht peak: a cone with a flat top plateau (ellipse)
        bx, bz = PL.BURCHT
        e = np.hypot((x - bx) / 46, (z - (bz - 4)) / 74)
        kegel = PL.BURCHT_TOP - (e - 1) * 34
        H = np.where(e < 1, PL.BURCHT_TOP, np.maximum(H, np.minimum(kegel, PL.BURCHT_TOP)))
        self.burcht_plat = e < 1
        # ---- the lake basin
        mx, mz, rx, rz = PL.MEER
        em = np.hypot((x - mx) / rx, (z - mz) / rz) + 0.04 * n1
        self.meer_e = em
        oever = PL.MEER_IJS + 2 + smooth((em - 1.0) / 0.5) * (H - PL.MEER_IJS - 2)
        bodem = PL.MEER_IJS - 3 - 7 * np.clip((1 - em) / 0.5, 0, 1) + 1.5 * f
        H = np.where(em < 1, bodem, np.where(em < 1.5, np.minimum(H, oever), H))
        self.meer = em < 1
        ix, iz, ir = PL.MEER_EILAND
        di = np.hypot(x - ix, z - iz)
        H = np.where(di < ir + 6, np.maximum(H, PL.MEER_IJS + 3 - smooth((di - ir + 2) / 8) * 12), H)
        self.meer &= ~(di < ir)
        # ---- the geode bowl of Kandijkom: flat floor, tiers (terraces) against the crystal ring wall, 4 gates
        sx, sz = PL.STAD
        r = np.hypot(x - sx, z - sz)
        hoek = np.arctan2(z - sz, x - sx)          # 0 = east, +pi/2 = south
        self.geode_r, self.geode_hoek = r, hoek
        noordheid = np.clip(-np.sin(hoek), 0, 1)
        top = PL.STAD_TOP + (26 + (PL.GEODE_HOOG - 26) * noordheid) * (1 + 0.12 * n1 + 0.15 * rug)
        R0 = PL.GEODE_R
        wand = np.where(r < R0, 0.0, np.where(r < R0 + 4, 1.0, np.clip(1 - (r - R0 - 4) / (PL.GEODE_DIK + 26), 0, 1) ** 0.8))
        hw = PL.STAD_TOP + (top - PL.STAD_TOP) * wand
        dx_, dz_ = x - sx, z - sz
        self.poort = np.zeros_like(self.land)
        self.poort_s = np.zeros((NX, NZ), np.float32)   # distance along the gate axis
        straat = np.zeros_like(self.land)
        for naam, (ux, uz) in PL.POORTEN.items():
            s_ = dx_ * ux + dz_ * uz
            p_ = np.abs(dx_ * uz - dz_ * ux)
            pm = (s_ > 0) & (p_ <= PL.POORT_HALF)
            straat |= pm
            self.poort |= pm & (s_ < R0 + PL.GEODE_DIK + 12)
            self.poort_s = np.where(pm, s_, self.poort_s)
        H = np.where(r < R0, PL.STAD_TOP, np.where(r < R0 + 60, np.maximum(H * smooth((r - R0) / 60) + hw * (1 - smooth((r - R0) / 60)), hw), H))
        self.straat = straat & (r < R0)
        ta, tb = PL.TIER_A, PL.TIER_B
        tierA = (r >= ta[0]) & (r < ta[1]) & ~straat
        tierB = (r >= tb[0]) & (r < tb[1]) & ~straat
        H = np.where(tierA, ta[2], np.where(tierB, tb[2], H))
        # stair lanes beside every gate street (|p| 5..7): 90 -> 95 at s 30..34, 95 -> 100 at s 45..49
        self.trap_baan = np.zeros_like(self.land)
        self.trap_richting = np.zeros((NX, NZ), np.int8)
        self.trap_hoogte = np.zeros((NX, NZ), np.int16)
        for k, (naam, (ux, uz)) in enumerate(PL.POORTEN.items()):
            s_ = dx_ * ux + dz_ * uz
            p_ = np.abs(dx_ * uz - dz_ * ux)
            lane = (s_ > 28.5) & (s_ < 61) & (p_ > PL.POORT_HALF + 0.5) & (p_ <= PL.POORT_HALF + 3.5)
            sr = np.floor(s_ + 0.5)
            hl = PL.STAD_TOP + np.clip(sr - 29, 0, 5) + np.clip(sr - 44, 0, 5)
            H = np.where(lane, hl, H)
            self.trap_baan |= lane
            self.trap_richting = np.where(lane, k + 1, self.trap_richting)
        self.tier = (tierA | tierB | self.trap_baan) & (r < R0)
        # the dock road: a ledge south out of the geode at the town floor, then the land drops away (the pier stands on posts)
        dock = (np.abs(x) <= 14) & (z > sz + R0 - 4) & (z <= -2251)
        H = np.where(dock, PL.STAD_TOP, H)
        ez = np.hypot(x / 60, (z + 2222) / 34)
        zuid = (z > -2251) & (ez < 1.6)
        H = np.where(zuid, np.minimum(H, 72 + 3 * n1 + smooth((ez - 0.8) / 0.8) * (H - 72)), H)
        self.dok_weg = dock
        # gates: the street runs flat (town floor) straight through the wall
        doorgang = self.poort & (self.poort_s >= R0 - 2) & (self.poort_s < R0 + PL.GEODE_DIK + 6)
        H = np.where(doorgang, PL.STAD_TOP, H)
        self.geode_wand = (r >= R0) & (r < R0 + PL.GEODE_DIK + 8) & ~self.poort
        self.geode_band = np.where(r < R0 + 3, 4, np.where(r < R0 + 7, 5, 6))
        self.stad_vloer = r < R0
        # ---- pads (arena, saddle, stations, campfires, bridge heads): exact flat circles with a soft rim
        H = self.pad(PL.GALM_ARENA[0], PL.GALM_ARENA[1], PL.GALM_ARENA[2] + 2, PL.GALM_ARENA_TOP, 22, H0=H)
        dz2 = PL.DRAKENZADEL
        H = self.pad(dz2[0], dz2[1], dz2[2] + 3, PL.DRAKENZADEL_TOP, 18, H0=H)
        H = self.pad(PL.GONDEL_BERG[0], PL.GONDEL_BERG[1], 10, PL.GONDEL_BERG_TOP, 18, H0=H)
        H = self.pad(PL.MIKA_FORT[0], PL.MIKA_FORT[1], 20, PL.MIKA_FORT_TOP, 20, H0=H)
        H = self.pad(PL.STAL[0], PL.STAL[1], 12, PL.STAD_TOP, 10, H0=H)
        bx3 = PL.BRUGGEN[2]
        kz0 = float(kloof_z(bx3))
        H = self.pad(bx3, kz0 + 25, 7, PL.BRUG_TOP, 16, H0=H)
        H = self.pad(bx3, kz0 - 25, 7, PL.BRUG_TOP, 12, H0=H)
        # ---- the Splinterkloof: vertical walls, floor 64, a frozen stream; ledge ramps up the walls
        zc = kloof_z(x)
        w = PL.KLOOF_BREED / 2 + 2.5 * n1 + 1.2 * f
        v = z - zc
        self.kloof_v, self.kloof_w = v, w
        kloof = (np.abs(v) < w) & (np.abs(x) < PL.KLOOF_X)
        H = np.where(kloof, PL.KLOOF_BODEM + 1.2 * np.clip(f, 0, 1) + 2 * smooth((np.abs(v) - w + 6) / 6), H)
        self.kloof_hoofd = kloof.copy()
        # the Galmspleet: a narrow gorge from the ravine south under the gondola, its floor rises to the land at its south end
        dsp, lsp = polyafstand(PL.GALMSPLEET)
        Lsp = sum(math.dist(a, b) for a, b in zip(PL.GALMSPLEET, PL.GALMSPLEET[1:]))
        spleet = (dsp < PL.GALMSPLEET_BREED / 2 + 1.5 * n1) & (z > zc + w - 4)
        eind = PL.GALMSPLEET[-1]
        hend = float(H[idx(*eind)])
        vs = PL.KLOOF_BODEM + 2 + np.clip(lsp / Lsp, 0, 1) ** 1.2 * (hend - PL.KLOOF_BODEM - 2)
        H = np.where(spleet, np.minimum(H, vs), H)
        self.spleet = spleet
        kloof |= spleet
        self.kloof = kloof
        self.kloof_ramp = np.zeros_like(self.land)
        for t_ in PL.KLOOF_TRAPPEN:
            if t_ is None:
                continue
            x0, rx_, kant = t_
            rand_h = PL.KLOOF_RAND_ZUID if kant > 0 else PL.KLOOF_RAND_NOORD
            lengte = 1.5 * (rand_h - PL.KLOOF_BODEM) + 10
            dx = (x - x0) * rx_
            if kant > 0:
                lane = (v > w - 6) & (v < w + 0.5)
            else:
                lane = (v < -w + 6) & (v > -w - 0.5)
            lane &= (dx >= 0) & (dx <= lengte) & (np.abs(x) < PL.KLOOF_X)
            ramp = PL.KLOOF_BODEM + np.clip(dx, 0, None) / 1.5
            H = np.where(lane, np.minimum(np.maximum(ramp, PL.KLOOF_BODEM), np.maximum(H, ramp)), H)
            self.kloof_ramp |= lane
        # ---- paths: a road profile along each polyline (smoothed, slope limited), cut/filled into the land
        self.pad_m = np.zeros((NX, NZ), np.float32)
        self.pad_kern = np.zeros_like(self.land)
        self.pad_d = np.full((NX, NZ), 1e9, np.float32)
        for naam, pts in PL.PADEN.items():
            H = self.weg(H, pts)
        # ---- the gondola cable: keep a free corridor under it
        (ax, az), (bx_, bz_) = PL.GONDEL_DAL, PL.GONDEL_BERG
        ya, yb = PL.GONDEL_DAL_TOP + PL.GONDEL_KABEL[0], PL.GONDEL_BERG_TOP + PL.GONDEL_KABEL[1]
        dk, langs = polyafstand([(ax, az), (bx_, bz_)])
        L = math.dist((ax, az), (bx_, bz_))
        ky = ya + (yb - ya) * np.clip(langs / L, 0, 1)
        self.kabel = (dk < 6) & (r > PL.GEODE_R - 2)
        H = np.where(self.kabel, np.minimum(H, ky - 7 + smooth((dk - 3) / 3) * 3), H)
        # ---- kept pads again (paths must not move them)
        for (px, pz, pr, ptop, fall) in self.pads:
            d = np.hypot(x - px, z - pz)
            H = np.where(d <= pr, ptop, H)
        H = np.where(self.stad_vloer & ~self.tier, PL.STAD_TOP, H)
        H = np.where(dock, PL.STAD_TOP, H)
        # ---- the coast: cliffs down into the cloud sea
        kt = smooth(kustd / 40) ** 0.7
        H = (PL.ZEE + 2) * (1 - kt) + H * kt
        self.Hf = H
        Hi = np.where(self.land, np.round(H), -1).astype(np.int16)
        vast = self.meer | (self.kloof & (kustd > 30)) | self.stad_vloer | ~self.land
        for _ in range(4):
            P_ = np.pad(Hi, 1, mode="edge")
            nb = np.sort(np.stack([P_[:-2, 1:-1], P_[2:, 1:-1], P_[1:-1, :-2], P_[1:-1, 2:]]), axis=0)
            doel = nb[1] - 1
            Hi = np.where(~vast & (Hi < doel), doel, Hi).astype(np.int16)
        self.H = Hi
        # lake water + ice
        self.ijs = self.meer.copy()
        sx_, sz_, sr = PL.SCHOTSEN
        self.open_water = self.meer & (np.hypot(x - sx_, z - sz_) < sr + 3 * n1)
        self.ijs &= ~self.open_water
        self.H[self.open_water] = PL.MEER_IJS - 1
        # areas (for materials): index per column
        self.gebied = np.zeros((NX, NZ), np.uint8)
        for i, g in enumerate(reversed(PL.GEBIEDEN)):
            gid, a, b, c, d = g[0], g[1], g[2], g[3], g[4]
            m = (x >= a) & (x <= c) & (z >= b) & (z <= d)
            self.gebied[m] = len(PL.GEBIEDEN) - 1 - i
        self.gnamen = [g[0] for g in PL.GEBIEDEN]

    def pad(self, px, pz, r, top, fall, H0):
        d = np.hypot(XX - px, ZZ - pz)
        t = smooth((d - r) / fall)
        self.pads.append((px, pz, r, top, fall))
        self._H = H0 * t + top * (1 - t)
        return self._H

    def weg(self, H, pts, breed=2.6, rand=5.0):
        # sample the profile along the polyline
        lengte = sum(math.dist(a, b) for a, b in zip(pts, pts[1:]))
        n = max(2, int(lengte) + 1)
        xs, zs = [], []
        for i in range(n):
            s = i * lengte / (n - 1)
            acc = 0.0
            for (ax, az), (bx, bz) in zip(pts, pts[1:]):
                L = math.dist((ax, az), (bx, bz))
                if s <= acc + L or (bx, bz) == pts[-1]:
                    t = 0 if L == 0 else min(1, (s - acc) / L)
                    xs.append(ax + t * (bx - ax))
                    zs.append(az + t * (bz - az))
                    break
                acc += L
        ii = np.clip(np.round(np.array(xs) - X0).astype(int), 0, NX - 1)
        jj = np.clip(np.round(np.array(zs) - Z0).astype(int), 0, NZ - 1)
        prof = H[ii, jj].astype(np.float64)
        prof = ndimage.gaussian_filter1d(prof, 8, mode="nearest")
        prof[0], prof[-1] = H[ii[0], jj[0]], H[ii[-1], jj[-1]]
        # slope limit 0.7 per block (forward + backward), the ends fixed
        for _ in range(3):
            for i in range(1, n):
                prof[i] = min(max(prof[i], prof[i - 1] - 0.7), prof[i - 1] + 0.7)
            for i in range(n - 2, -1, -1):
                prof[i] = min(max(prof[i], prof[i + 1] - 0.7), prof[i + 1] + 0.7)
        dp, langs = polyafstand(pts)
        k = np.clip(np.round(langs / lengte * (n - 1)).astype(int), 0, n - 1)
        hp = prof[k]
        mix = smooth((dp - breed) / rand)
        m = (dp < breed + rand) & ~self.kloof_buiten_ramp(dp)
        H = np.where(m, hp * (1 - mix) + H * mix, H)
        self.pad_kern |= dp < breed
        self.pad_d = np.minimum(self.pad_d, dp)
        self.pad_m = np.maximum(self.pad_m, np.clip(1 - dp / breed, 0, 1) + (dp < breed) * 0.5)
        return H

    def kloof_buiten_ramp(self, dp):
        return self.kloof & ~self.kloof_ramp

    # ------------------------------------------------------------------------------------------------ sea
    def zee(self):
        d = ndimage.distance_transform_edt(~self.land)
        self.zeed = d
        vloer = PL.ZEE - 2 - np.clip(d / 6, 0, 8)
        vloer = np.where(d > 40, np.maximum(vloer, PL.ZEE - 2 - np.clip((90 - d) / 6, 0, 8)), vloer)   # the open cloud sea is shallow (small tiles)
        self.vloer = np.round(vloer).astype(np.int16)

    def h(self, x, z):
        i, j = idx(x, z)
        return int(self.H[i, j])

    def gebied_van(self, x, z):
        i, j = idx(x, z)
        return self.gnamen[self.gebied[i, j]]
