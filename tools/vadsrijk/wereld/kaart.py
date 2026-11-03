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
    "zwamtempel": [(200, -240), (400, -300)],
    "haven": [(100, 452)],
}
UITKIJK = {"klodderkijk": (520, -240), "zwamnaald": (-510, -40), "druipspits": (-100, -510), "haventand": (160, 380),
           "slijmnaald": (460, 380)}
ARENA = (410, 90, 32)          # Koning Klodder (x, z, r)
SLAAPHEUVEL = (300, 300)
KLOOF = [(40, -130), (100, -170), (170, -230), (240, -300), (280, -350), (360, -260), (450, -220)]
FOSSIEL = [(140, -205), (205, -265)]
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
HAVEN_DORP = (60, 420, 125, 560)
VIJVERS = [(330, -70, 30, 18), (470, 160, 25, 16), (250, 260, 22, 14), (520, 40, 20, 13), (300, 120, 14, 10)]
PADEN = {
    "weide": [(30, -40), (90, -40), (220, -80), (300, -20), (360, 30), (374, 90)],
    "woud": [(-60, 20), (-130, 20), (-220, 40), (-350, 120), (-430, 230), (-300, 340)],
    "grotten": [(-60, -70), (-110, -120), (-180, -220), (-260, -300), (-300, -220)],
    "grotten2": [(-260, -300), (-420, -300)],
    "grotten3": [(-260, -300), (-180, -360), (-180, -480)],
    "grotten4": [(-260, -300), (-380, -400), (-460, -460)],
    "brug": [(30, -100), (80, -140), (120, -185), (140, -205)],
    "berg": [(205, -265), (230, -280), (400, -292), (410, -305), (400, -322), (280, -345), (272, -360), (285, -372), (330, -380), (330, -386)],
    "haven": [(10, 100), (20, 130), (80, 180), (100, 300), (95, 400), (92, 470)],
    "zuid": [(100, 300), (160, 375)],
    "weide2": [(220, -80), (230, -130), (330, -200)],
    "weide3": [(360, 30), (480, -80), (520, -240)],
    "slijm": [(374, 90), (410, 200), (460, 380)],
    "slijm2": [(410, 200), (330, 260), (300, 270)],
    "woud2": [(-220, 40), (-300, -40), (-420, 40), (-510, -40)],
    "druip": [(-180, -360), (-100, -510)],
}


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
        # De Haven village: flat at 64
        hx0, hz0, hx1, hz1 = HAVEN_DORP
        dh = np.maximum(np.maximum(hx0 - x, x - hx1), np.maximum(hz0 - z, z - hz1))
        th = np.clip(dh / 20, 0, 1)
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
        # the mountain path: a proper ramp (ignore the smoothed terrain): from y 70 up to the plateau
        dp, langs = polyafstand(PADEN["berg"])
        tot = sum(math.dist(a, b) for a, b in zip(PADEN["berg"], PADEN["berg"][1:]))
        ramp = 72 + (TEMPEL_PLATEAU - 72) * np.clip(langs / tot, 0, 1)
        m = dp < 4.5
        rand = (dp >= 4.5) & (dp < 9)
        H = np.where(m, ramp, H)
        H = np.where(rand, np.maximum(H, ramp), H)
        self.bergpad = m
        self.H = np.where(self.land, np.round(H), -1).astype(np.int16)
        self.Hf = H

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
