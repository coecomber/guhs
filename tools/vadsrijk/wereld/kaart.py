"""The 2D plan of zone 1 (world coordinates, x east / z south): areas, heights, paths, scars, the Kloof, the sea.

Everything is a numpy map over the tile grid X0..X0+NX-1 / Z0..Z0+NZ-1, indexed [x - X0, z - Z0].
The layout follows renders-vadsrijk/_bron/stad/zonekaart.py (world = map - 600); the area boxes follow the contract (kern.Gebieden).
"""
import math

import numpy as np
from scipy import ndimage

TX0, TX1 = -11, 11            # tile range x (inclusive)
TZ0, TZ1 = -11, 18            # tile range z (inclusive)
X0, Z0 = TX0 * 64, TZ0 * 64
NX, NZ = (TX1 - TX0 + 1) * 64, (TZ1 - TZ0 + 1) * 64
ZEE = 62                      # water top

# ---------------------------------------------------------------- layout (world coords)
CENTRA = {  # area -> (centre, colour on the debug map)
    "stad": (0, 0), "weide": (360, 20), "woud": (-350, 100), "grotten": (-360, -390), "tempel": (330, -410),
    "zuid": (0, 400), "zuidoost": (380, 420), "zuidwest": (-380, 470),
}
STAD_BOX = (-128, -128, 127, 127)   # the Zwamzoet scene
KAMPVUREN = {
    "klodderweide": [(230, -130), (410, 200), (480, -80)],
    "sporenwoud": [(-220, 10), (-430, 230), (-300, 340)],
    "druipgrotten": [(-260, -300), (-460, -460), (-180, -480)],
    "zwamtempel": [(150, -248), (232, -347)],
    "haven": [(100, 452)],
}
UITKIJK = {"klodderkijk": (520, -240), "zwamnaald": (-510, -40), "druipspits": (-100, -510), "haventand": (160, 380),
           "slijmnaald": (460, 380)}
ARENA = (410, 90, 32)          # Koning Klodder (x, z, r)
SLAAPHEUVEL = (300, 300)
# fix round 1: starts at (75, -160), clear of the Zwamzoet box (it used to cut under its NE corner)
KLOOF = [(75, -160), (100, -170), (170, -230), (240, -300), (280, -350), (360, -260), (450, -220)]
# the dragon-fossil bridge crosses the Kloof square to it (fix round 1: it used to lie along the gorge): ends on both rims
FOSSIEL = [(151, -181), (119, -219)]
BERG = (330, -470)
TEMPEL_PLATEAU = 151           # top block of the temple plateau
LITTEKENS = [
    [(-640, 580), (-420, 440), (-240, 300), (-130, 220), (-40, 160)],
    [(20, -130), (160, -280), (300, -380), (640, -640)],
    [(100, 630), (220, 450), (380, 320), (640, 220)],
]
GROT_INGANGEN = [(-300, -220), (-420, -300), (-180, -360)]
KRISTALKAMER = (-490, 52, -520)
LICHTSTRALEN = [(-340, 120), (-420, 40), (-240, 220), (-480, 160), (-300, -40), (-360, 300)]
SCHILDPAD = (520, 720)
HAVEN_DORP = (56, 418, 120, 564)
VIJVERS = [(330, -70, 30, 18), (470, 160, 25, 16), (250, 260, 22, 14), (520, 40, 20, 13), (300, 120, 14, 10)]
PADEN = {
    "weide": [(30, -40), (90, -40), (220, -80), (300, -20), (360, 30), (374, 90)],
    "woud": [(-60, 20), (-130, 20), (-220, 40), (-350, 120), (-430, 230), (-300, 340)],
    "grotten": [(-60, -70), (-110, -120), (-180, -220), (-260, -300), (-300, -220)],
    "grotten2": [(-260, -300), (-420, -300)],
    "grotten3": [(-260, -300), (-180, -360), (-180, -480)],
    "grotten4": [(-260, -300), (-380, -400), (-460, -460)],
    "brug": [(30, -100), (70, -112), (120, -150), (151, -181)],
    # north of the Kloof from the bridge up the mountain flank to the foot of the temple stairs (z -369, stand y TP - 6)
    "berg": [(119, -219), (140, -270), (190, -330), (240, -375), (290, -385), (330, -366)],
    "haven": [(10, 100), (20, 130), (80, 180), (100, 300), (95, 400), (92, 470)],
    "zuid": [(100, 300), (160, 375)],
    "weide2": [(220, -80), (230, -130), (330, -200)],
    "weide3": [(360, 30), (480, -80), (520, -240)],
    "slijm": [(374, 90), (410, 200), (460, 380)],
    "slijm2": [(410, 200), (330, 260), (300, 262)],
    "woud2": [(-220, 40), (-300, -40), (-420, 40), (-510, -40)],
    "druip": [(-180, -360), (-100, -510)],
}

# fix round 1: paths that lead to a rock needle stop at the foot of its spiral stair instead of under the rock
for _naam, _pts in PADEN.items():
    for (_ux, _uz) in UITKIJK.values():
        if math.hypot(_pts[-1][0] - _ux, _pts[-1][1] - _uz) < 8:
            _dx, _dz = _pts[-2][0] - _ux, _pts[-2][1] - _uz
            _L = math.hypot(_dx, _dz)
            _pts[-1] = (round(_ux + _dx / _L * 19), round(_uz + _dz / _L * 19))


# ---------------------------------------------------------------- helpers
def ruis(seed, schalen, nx=NX, nz=NZ):
    """fBm value noise in [-~1, ~1]: schalen = [(cell size in blocks, amplitude), ...]."""
    rng = np.random.default_rng(seed)
    out = np.zeros((nx, nz), np.float32)
    for (cel, amp) in schalen:
        gx, gz = nx // cel + 4, nz // cel + 4
        g = rng.random((gx, gz)).astype(np.float32) * 2 - 1
        up = ndimage.zoom(g, cel, order=3, mode="nearest")
        out += amp * up[cel:cel + nx, cel:cel + nz]
    return out


XX, ZZ = np.meshgrid(np.arange(X0, X0 + NX, dtype=np.float32), np.arange(Z0, Z0 + NZ, dtype=np.float32), indexing="ij")


def polyafstand(pts, xx=None, zz=None):
    """Distance from every column to a polyline (+ the param t along it in blocks)."""
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


class Kaart:
    def __init__(self):
        self.n1 = ruis(2, [(160, 1.0), (60, 0.5), (24, 0.25), (9, 0.12)])
        self.n2 = ruis(5, [(300, 1.0), (110, 0.6), (40, 0.3)])
        self.fijn = ruis(9, [(12, 1.0), (5, 0.5)])
        self.land_en_gebieden()
        self.hoogte()
        self.zee()

    # ------------------------------------------------------------ land + areas
    def land_en_gebieden(self):
        x, z, n1 = XX, ZZ, self.n1
        q = np.maximum(np.abs(x), np.abs(z)) / 600
        rond = np.hypot(np.maximum(np.abs(x) - 420, 0), np.maximum(np.abs(z) - 420, 0)) / 180
        land = np.maximum(q, rond) + n1 * 0.06 < 0.985
        # the bay from the city harbour down to the open sea (contract: De Haven at its mouth)
        xoff = 30 * np.sin(z / 90)
        breed = np.where(z < 130, 128, np.where(z < 190, 128 - (z - 130) * 0.9, 74 + (z - 190) * 0.05)) + n1 * 10
        kust_stad = 88 + 3 * np.sin((x + 128) * 0.05)
        baai = (np.abs(x - np.where(z < 190, 0, xoff)) < breed) & (z >= kust_stad)
        land &= ~baai
        self.baai = baai
        self.land = land
        # area index per column: noisy Voronoi (only for materials/decoration; kern uses the contract boxes)
        namen = list(CENTRA)
        dist = np.stack([np.hypot(x - c[0] + self.n1 * 90, z - c[1] + self.n2 * 90) * (1.6 if k == "stad" else 1.0)
                         for k, (c) in ((k, CENTRA[k]) for k in namen)])
        self.namen = namen
        self.gebied = np.argmin(dist, 0).astype(np.uint8)
        self.g = {k: (self.gebied == i) for i, k in enumerate(namen)}
        # soft weights for heights
        self.w = {}
        for k in namen:
            self.w[k] = ndimage.gaussian_filter(self.g[k].astype(np.float32), 18)

    # ------------------------------------------------------------ heights
    def hoogte(self):
        x, z, n1, n2, f = XX, ZZ, self.n1, self.n2, self.fijn
        w = self.w
        H = 66 + 2.5 * n2 + 1.0 * f
        H += w["weide"] * (2 + 5 * n1)
        H += w["woud"] * (3 + 6 * n2 + 3 * n1)
        H += (w["zuid"] + w["zuidoost"] + w["zuidwest"]) * (4 * n1)
        # Druipgrotten: rocky ridged hills
        rug = 1 - np.abs(ruis(13, [(90, 1.0), (40, 0.5), (16, 0.2)]))
        H += w["grotten"] * (8 + 46 * np.clip(rug, 0, 1) ** 2.2 + 6 * n1)
        # the temple mountain: a big peak with a crown of ridges around the summit hall
        d = np.hypot((x - BERG[0]) / 125, (z - BERG[1]) / 105)
        berg = 150 * np.exp(-d ** 2 * 1.3) + 22 * np.exp(-((d - 0.42) / 0.12) ** 2)
        H += berg * (1 + 0.12 * n1) + w["tempel"] * (6 + 10 * rug)
        # temple plateau (south slope) + the summit bowl for the Opperzwam hall
        plat = np.hypot((x - 330) / 76, (z - -455) / 80)
        H = np.where(plat < 1, TEMPEL_PLATEAU, np.where(plat < 1.35, H * (plat - 1) / 0.35 + TEMPEL_PLATEAU * (1.35 - plat) / 0.35, H))
        # the city: flat at 63 under the scene, ramping up outside
        sx0, sz0, sx1, sz1 = STAD_BOX
        ds = np.maximum(np.maximum(sx0 - x, x - sx1), np.maximum(sz0 - z, z - sz1))
        t = np.clip(ds / 48, 0, 1)
        H = 63 * (1 - t) + H * t
        # De Haven village: flat at 64 inside an organic (noisy, rounded) outline, a long soft rim out to the meadows
        hx0, hz0, hx1, hz1 = HAVEN_DORP
        hcx, hcz, hrx, hrz = (hx0 + hx1) / 2, (hz0 + hz1) / 2, (hx1 - hx0) / 2, (hz1 - hz0) / 2
        rr = ((np.abs(x - hcx) / hrx) ** 3 + (np.abs(z - hcz) / hrz) ** 3) ** (1 / 3)
        dh = (rr - 1) * min(hrx, hrz) + 7 * n1 + 2.5 * f
        self.haven_d = dh
        th = np.clip(dh / 26, 0, 1)
        th = th * th * (3 - 2 * th)
        H = 64 * (1 - th) + H * th
        # Koning Klodder arena: a flat round meadow
        da = np.hypot(x - ARENA[0], z - ARENA[1])
        ta = np.clip((da - ARENA[2] - 6) / 20, 0, 1)
        H = 68 * (1 - ta) + H * ta
        # coast: gentle beaches down to the water
        kustd = ndimage.distance_transform_edt(self.land)
        self.kustd = kustd
        kt = np.clip(kustd / 28, 0, 1) ** 0.8
        H = (ZEE + 1) * (1 - kt) + H * kt
        # the Kloof: a deep ravine with a stream
        dk, _ = polyafstand(KLOOF)
        rk = 15 + 4 * n1 + 2 * f
        diep = np.clip(1 - (dk / rk) ** 4, 0, 1)
        self.kloof = dk < rk
        H = H * (1 - diep) + (30 + 2 * f) * diep
        # fix round 1: dry banks (y 34) along both sides of the stream: a walk along the bottom to the ramps
        H = np.where(self.kloof & (dk > 0.3 * rk) & (dk < 0.62 * rk), np.maximum(H, 34), H)
        # no inland lakes by accident: inland ground stays >= 64 (the Kloof and the ponds are carved on purpose)
        H = np.where((kustd > 30) & ~self.kloof, np.maximum(H, 64 + 0.5 * f), H)
        # ponds of Klodderweide (water at their own level); land has no water unless set here
        self.wl = np.full((NX, NZ), -1, np.float32)
        for (px, pz, rx, rz) in VIJVERS:
            e = np.hypot((x - px) / rx, (z - pz) / rz) + 0.08 * f
            rand = np.median(H[(e > 1.1) & (e < 1.4)]) if np.any((e > 1.1) & (e < 1.4)) else 66
            rand = float(rand)
            bodem = rand - 2 - 4 * np.clip(1 - e, 0, 1)          # inside: down to 4 deep
            oever = (rand - 1) + np.clip((e - 1.0) / 0.6, 0, 1) * (H - (rand - 1))   # rim: blend back to the land
            H = np.where(e < 1.0, bodem, np.where(e < 1.6, oever, H))
            self.wl = np.where(e < 1.0, rand - 1, self.wl)
        # stream in the Kloof
        self.wl = np.where(self.kloof & (H < 34), 33, self.wl)
        # paths: smooth the height along every path
        self.pad = np.zeros((NX, NZ), np.float32)
        Hs = ndimage.gaussian_filter(H, 6)
        for naam, pts in PADEN.items():
            dp, _ = polyafstand(pts)
            r = 3.2 if naam in ("berg",) else 2.4
            m = dp < r + 4
            mix = np.clip((dp - r) / 4, 0, 1)
            H = np.where(m & ~self.kloof, Hs * (1 - mix) + H * mix, H)
            self.pad = np.maximum(self.pad, np.clip(1 - dp / r, 0, 1) + (dp < r) * 0.5)
        # fix round 1: no path step higher than 1 block: every path (not the mountain ramp, not the Haven road) gets a smooth
        # height profile along its length with a slope of at most 0.7, flat across its width
        H = self.padprofielen(H)
        # the fossil bridge deck level: both ends get a level landing at the deck height (fix round 1)
        dek = int(round(max(H[idx(*FOSSIEL[0])], H[idx(*FOSSIEL[1])], 70)))
        self.dek = dek
        for (fx, fz) in FOSSIEL:
            dl = np.hypot(XX - fx, ZZ - fz)
            H = np.where(dl < 7, dek, np.where(dl < 13, dek + (H - dek) * (dl - 7) / 6, H))
        # the mountain path: a proper ramp (ignore the smoothed terrain): from the deck up to the foot of the temple stairs
        dp, langs = polyafstand(PADEN["berg"])
        tot = sum(math.dist(a, b) for a, b in zip(PADEN["berg"], PADEN["berg"][1:]))
        ramp = dek + (TEMPEL_PLATEAU - 7 - dek) * np.clip(langs / tot, 0, 1)
        m = dp < 4.5
        rand = (dp >= 4.5) & (dp < 9)
        H = np.where(m, ramp, H)
        H = np.where(rand, np.maximum(H, ramp - (dp - 4.5)), H)
        self.bergpad = m
        H = self.kloofpaden(H)
        H = np.round(H)
        H = self.ontsnap(H)
        self.H = np.where(self.land, H, -1).astype(np.int16)
        self.Hf = H

    # ------------------------------------------------------------ walkability (fix round 1)
    def padprofielen(self, H):
        best = np.full(H.shape, 1e9, np.float32)
        nieuw = H.copy()
        for naam, pts in PADEN.items():
            if naam in ("berg", "haven"):
                continue
            tot = sum(math.dist(a, b) for a, b in zip(pts, pts[1:]))
            n = int(tot) + 1
            # sample the terrain along the line
            prof = np.zeros(n, np.float32)
            acc, k = 0.0, 0
            for (a, b) in zip(pts, pts[1:]):
                L = math.dist(a, b)
                while k < n and k <= acc + L:
                    t = (k - acc) / L
                    i, j = idx(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)
                    prof[k] = H[min(max(i, 0), NX - 1), min(max(j, 0), NZ - 1)]
                    k += 1
                acc += L
            prof[k:] = prof[k - 1] if k else 64
            prof = ndimage.uniform_filter1d(prof, 9, mode="nearest")
            for _ in range(2):
                for k in range(1, n):
                    prof[k] = min(max(prof[k], prof[k - 1] - 0.7), prof[k - 1] + 0.7)
                for k in range(n - 2, -1, -1):
                    prof[k] = min(max(prof[k], prof[k + 1] - 0.7), prof[k + 1] + 0.7)
            dp, langs = polyafstand(pts)
            ph = np.interp(langs, np.arange(n), prof)
            m = (dp < 2.8) & (dp < best) & ~self.kloof
            nieuw = np.where(m, ph, nieuw)
            rand = (dp >= 2.8) & (dp < 6) & (dp < best) & ~self.kloof
            mix = (dp - 2.8) / 3.2
            nieuw = np.where(rand, ph * (1 - mix) + nieuw * mix, nieuw)
            best = np.minimum(best, dp)
        return nieuw

    def kloofpaden(self, H):
        """Ramps out of the Kloof: every ~70 blocks a 5-wide ramp cut along the wall, from the stream bank up to the rim,
        alternating sides, so a player who falls into the gorge can always walk out (FIX_BRIEF walkability)."""
        segs = list(zip(KLOOF, KLOOF[1:]))
        lengte = [math.dist(a, b) for a, b in segs]
        tot = sum(lengte)
        self.kloofpad_lijnen = []
        for k, s0 in enumerate(np.arange(35, tot - 30, 68)):
            acc = 0
            for (a, b), L in zip(segs, lengte):
                if acc + L >= s0:
                    break
                acc += L
            t = (s0 - acc) / L
            cx, cz = a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t
            ux, uz = (b[0] - a[0]) / L, (b[1] - a[1]) / L
            kant = 1 if k % 2 else -1
            nx, nz = -uz * kant, ux * kant
            if math.hypot(cx - 135, cz + 200) < 40:       # not at the fossil bridge
                continue
            # rim distance along the normal 50 blocks further along the gorge (slope ~0.7)
            ex, ez = cx + ux * 50, cz + uz * 50
            d = 6
            while d < 40:
                i, j = idx(ex + nx * d, ez + nz * d)
                if not self.kloof[i, j]:
                    break
                d += 1
            A = (cx + nx * 5, cz + nz * 5)
            B = (ex + nx * (d + 6), ez + nz * (d + 6))
            i, j = idx(*B)
            hb = float(H[i, j])
            lijn = [A, ((A[0] + B[0]) / 2 + nx * 2, (A[1] + B[1]) / 2 + nz * 2), B]
            dp, langs = polyafstand(lijn)
            Lt = sum(math.dist(p, q) for p, q in zip(lijn, lijn[1:]))
            ramp = 34 + (hb - 34) * np.clip(langs / Lt, 0, 1)
            m = dp < 2.6
            H = np.where(m, ramp, H)
            sch = (dp >= 2.6) & (dp < 6)
            H = np.where(sch, np.maximum(H, ramp - (dp - 2.6)), H)
            self.pad = np.maximum(self.pad, np.clip(1 - dp / 2.6, 0, 1) + (dp < 2.6) * 0.5)
            self.kloofpad_lijnen.append(lijn)
        return H

    def ontsnap(self, H):
        """Fill pits: every land column must reach the sea by steps of at most 1 block up (water counts at its surface).
        Priority flood from the sea, lowest first; a column lower than (neighbour - 1) is raised to neighbour - 1. Only the
        wilderness is touched (not under the Zwamzoet, Knuffelkamp and Haven scenes)."""
        import heapq
        nx_, nz_ = H.shape
        E = np.where(self.land, np.maximum(H, np.where(self.wl > H, np.round(self.wl), H)), ZEE).astype(np.int32)
        sx0, sz0, sx1, sz1 = STAD_BOX
        hx0, hz0, hx1, hz1 = HAVEN_DORP
        vrij = self.land & ~((XX >= sx0 - 4) & (XX <= sx1 + 4) & (ZZ >= sz0 - 4) & (ZZ <= sz1 + 4))             & ~((XX >= hx0 - 4) & (XX <= hx1 + 4) & (ZZ >= hz0 - 4) & (ZZ <= hz1 + 4)) & (ZZ < 700)
        zee = ~self.land
        rand = zee & ndimage.binary_dilation(self.land)
        bezocht = zee.copy()
        Ef = E.ravel().tolist()
        vrijf = vrij.ravel()
        bez = bytearray(bezocht.ravel().tobytes())
        hq = [(Ef[i], i) for i in np.flatnonzero(rand)]
        heapq.heapify(hq)
        verhoogd = 0
        N = nx_ * nz_
        while hq:
            v, i = heapq.heappop(hq)
            x, r = divmod(i, nz_)
            for n in ((i - nz_) if x > 0 else -1, (i + nz_) if x < nx_ - 1 else -1, (i - 1) if r > 0 else -1,
                      (i + 1) if r < nz_ - 1 else -1):
                if n < 0 or bez[n]:
                    continue
                bez[n] = 1
                e = Ef[n]
                if e < v - 1 and vrijf[n]:
                    e = v - 1
                    Ef[n] = e
                    verhoogd += 1
                heapq.heappush(hq, (e, n))
        E2 = np.array(Ef, np.int32).reshape(nx_, nz_)
        nat = self.wl > H
        H2 = np.where(self.land & ~nat, np.maximum(H, E2), H)
        # water columns: raise the bed only when the water itself was too low (keep the water level)
        H2 = np.where(self.land & nat & (E2 > np.round(self.wl)), E2, H2)
        self.ontsnap_verhoogd = verhoogd
        print(f"  ontsnap: {verhoogd} kolommen opgehoogd")
        return H2

    # ------------------------------------------------------------ sea floor
    def zee(self):
        d = ndimage.distance_transform_edt(~self.land)
        self.zeed = d
        vloer = ZEE - 1 - np.clip(d / 7, 0, 3)
        # the bay is a bit deeper (boats), the Kloof stream is its own thing
        vloer = np.where(self.baai, np.minimum(vloer, ZEE - 3), vloer)
        self.vloer = np.round(vloer).astype(np.int16)

    def h(self, x, z):
        i, j = idx(x, z)
        return int(self.H[i, j])

    def gebied_van(self, x, z):
        i, j = idx(x, z)
        return self.namen[self.gebied[i, j]]
