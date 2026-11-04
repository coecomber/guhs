"""Zone-2 tile composer: the zone-1 tegels.Wereld (stamps, points, hollowing, nbt writer) on the zone-2 grid and materials.

Only the grid, the surface materials and the per-column terrain differ from zone 1 (snow, ice rock, the frozen lake, the geode
bands, the cloud sea); stamps/points/hollow/write are inherited unchanged.
"""
import numpy as np

import blokken as B
import plan as PL
import tegels as TG
import kaart2 as K2

T, HOOG = TG.T, TG.HOOG

GESTEENTE2 = {
    "sneeuw": ["minecraft:stone", "minecraft:calcite", "minecraft:stone", "minecraft:andesite", "minecraft:diorite",
               "minecraft:tuff", "minecraft:stone", "minecraft:calcite"],
    "galm": ["minecraft:calcite", "minecraft:packed_ice", "minecraft:calcite", "minecraft:diorite", "minecraft:white_terracotta",
             "minecraft:light_blue_terracotta", "minecraft:calcite", "minecraft:snow_block"],
    "kloof": ["minecraft:deepslate", "minecraft:tuff", "minecraft:deepslate", "minecraft:calcite", "minecraft:tuff",
              "minecraft:packed_ice", "minecraft:deepslate", "minecraft:smooth_basalt"],
    "burcht": ["minecraft:tuff", "minecraft:calcite", "minecraft:light_gray_terracotta", "minecraft:tuff", "minecraft:andesite",
               "minecraft:calcite", "minecraft:tuff", "minecraft:light_gray_terracotta"],
    "geode_roze": ["guhs:guh_kristal_blok", "minecraft:pink_terracotta", "minecraft:amethyst_block", "guhs:guh_kristal_blok",
                   "minecraft:pink_concrete", "minecraft:amethyst_block", "guhs:guh_kristal_blok", "minecraft:pink_terracotta"],
    "geode_calciet": ["minecraft:calcite"] * 8,
    "geode_basalt": ["minecraft:smooth_basalt", "minecraft:smooth_basalt", "minecraft:tuff", "minecraft:smooth_basalt",
                     "minecraft:smooth_basalt", "minecraft:basalt[axis=y]", "minecraft:smooth_basalt", "minecraft:tuff"],
    "oever": ["minecraft:stone", "minecraft:gravel", "minecraft:stone", "minecraft:andesite", "minecraft:tuff",
              "minecraft:stone", "minecraft:gravel", "minecraft:stone"],
}
SOORT2 = {k: i for i, k in enumerate(GESTEENTE2)}
SNEEUW = "minecraft:snow_block"
GRAS_SNEEUW = "minecraft:grass_block[snowy=true]"


class Wereld2(TG.Wereld):
    def __init__(self, K):
        self.K = K
        self.palet = B.Palet()
        self.stempels = []
        self.punten = TG.Punten(self.palet)
        self.lo_extra = np.full((PL.NX, PL.NZ), 9999, np.int16)
        self.bezet = np.zeros((PL.NX, PL.NZ), bool)
        self.oppervlak()

    # ------------------------------------------------------------------------------------------------ materials
    def oppervlak(self):
        K, P = self.K, self.palet
        H = K.H.astype(np.int32)
        f = K.fijn
        rnd = np.random.default_rng(91).random((PL.NX, PL.NZ))
        Hp = np.pad(H, 1, mode="edge")
        buur_min = np.minimum.reduce([Hp[:-2, 1:-1], Hp[2:, 1:-1], Hp[1:-1, :-2], Hp[1:-1, 2:]])
        buur_max = np.maximum.reduce([Hp[:-2, 1:-1], Hp[2:, 1:-1], Hp[1:-1, :-2], Hp[1:-1, 2:]])
        self.buur_min = buur_min
        steil = (H - buur_min >= 3) | (buur_max - H >= 4)
        self.steil = steil
        top = np.full((PL.NX, PL.NZ), P.id(SNEEUW), np.uint16)
        sub = np.full((PL.NX, PL.NZ), P.id(SNEEUW), np.uint16)
        soort = np.zeros((PL.NX, PL.NZ), np.uint8)
        x, z = K2.XX, K2.ZZ

        def zet(mask, t=None, s=None, k=None):
            if t is not None:
                top[mask] = P.id(t)
            if s is not None:
                sub[mask] = P.id(s)
            if k is not None:
                soort[mask] = SOORT2[k]
        g = {n: K.gebied == i for i, n in enumerate(K.gnamen)}
        zet(g["z2_galmhellingen"] | (x > 140), None, None, "galm")
        zet(g["z2_splinterkloof"] | (z < -2690), None, None, "kloof")
        zet(K.burcht_plat | (np.hypot(x / 70, (z + 2919) / 95) < 1), None, None, "burcht")
        # the low south and the lake shore: snowy grass with dirt under it
        laag = (H < 92) & (z > -2300) & ~K.stad_vloer
        zet(laag, GRAS_SNEEUW, "minecraft:dirt")
        oever = (K.meer_e >= 1) & (K.meer_e < 1.12)
        zet(oever, "minecraft:gravel", "minecraft:stone", "oever")
        zet(oever & (f > 0.2), SNEEUW)
        zet(K.meer, "minecraft:gravel", "minecraft:clay", "oever")
        zet(K.meer & (f > 0.3), "minecraft:clay")
        # geode bands (the wall's inside face is pink crystal, then calcite, then basalt)
        gw = K.geode_wand
        for band, naam in ((4, "geode_roze"), (5, "geode_calciet"), (6, "geode_basalt")):
            zet(gw & (K.geode_band == band), None, None, naam)
        # steep: bare rock
        gest = np.array([[P.id(b) for b in GESTEENTE2[k]] for k in GESTEENTE2], np.uint16)
        self.gest = gest
        band = ((H + (2 * np.sin(x / 7.0) + 2 * np.cos(z / 9.0) + 8).astype(np.int32)) // 3) % 8
        rots = gest[soort, band]
        top = np.where(steil & (H > 70), rots, top)
        sub = np.where(steil & (H > 70), rots, sub)
        # the geode rim keeps a snow cap; its inner face shows crystal
        # ravine floor: gravel and snow, a frozen stream in the middle
        kb = K.kloof & ~K.kloof_ramp
        zet(kb, "minecraft:gravel", "minecraft:stone", "kloof")
        zet(kb & (f > 0.0), SNEEUW)
        zet(kb & (np.abs(K.kloof_v) < 2.6), "minecraft:packed_ice")
        zet(K.kloof_ramp, "minecraft:cobblestone", "minecraft:stone", "kloof")
        zet(K.kloof_ramp & (rnd < 0.3), "minecraft:gravel")
        # paths
        pad = K.pad_m > 0.5
        zet(pad, "minecraft:dirt_path")
        zet(pad & (rnd < 0.2), "minecraft:coarse_dirt")
        zet(pad & (rnd > 0.88), "minecraft:packed_ice")
        zet(pad & K.stad_vloer, "guhs:knuffelklinkers")
        # town floor (bouw2 stamps the streets/plaza on top)
        zet(K.stad_vloer | K.dok_weg, SNEEUW, "minecraft:stone")
        self.top, self.sub, self.soort = top, sub, soort
        self.boff = (2 * np.sin(x / 7.0) + 2 * np.cos(z / 9.0) + 8).astype(np.int32)
        self.ids = {s: P.id(s) for s in ("minecraft:water", "minecraft:sand", "minecraft:stone", "minecraft:barrier", "minecraft:gravel",
                                          "minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:light_blue_terracotta")}
        # the lake ice: clear ice (plushies show through), packed ice patches, blue ice near the shore
        ijs = np.full((PL.NX, PL.NZ), self.ids["minecraft:ice"], np.uint16)
        ijs[(f > 0.55)] = self.ids["minecraft:packed_ice"]
        ijs[K.meer_e > 0.93] = self.ids["minecraft:blue_ice"]
        self.ijsblok = ijs

    # ------------------------------------------------------------------------------------------------ stamps
    def stempel(self, st, bezet=True):
        st.lut = self.palet.lut(st.pal)
        self.stempels.append(st)
        if bezet:
            voet = np.any(st.v > 0, axis=1)
            ox, oy, oz = st.off
            X, Z = voet.shape
            i0, j0 = ox - PL.X0, oz - PL.Z0
            a0, b0 = max(0, -i0), max(0, -j0)
            a1, b1 = min(X, PL.NX - i0), min(Z, PL.NZ - j0)
            if a1 > a0 and b1 > b0:
                self.bezet[i0 + a0:i0 + a1, j0 + b0:j0 + b1] |= voet[a0:a1, b0:b1]

    # ------------------------------------------------------------------------------------------------ one tile
    def tegel(self, tx, tz):
        K = self.K
        x0, z0 = tx * T, tz * T
        i0, j0 = x0 - PL.X0, z0 - PL.Z0
        sl = (slice(i0, i0 + T), slice(j0, j0 + T))
        H = K.H[sl].astype(np.int32)
        land = H >= 0
        bmin = self.buur_min[sl].astype(np.int32)
        lo = np.minimum(H, bmin) - 1
        lo = np.minimum(lo, H - 1)
        lo = np.minimum(lo, self.lo_extra[sl].astype(np.int32))
        lo = np.maximum(lo, 1)
        y = np.arange(HOOG, dtype=np.int32)[None, :, None]
        Hc, loc = H[:, None, :], lo[:, None, :]
        vol = np.zeros((T, HOOG, T), np.uint16)
        band = ((y + self.boff[sl][:, None, :]) // 3) % 8
        rots = self.gest[self.soort[sl][:, None, :], band]
        vast = land[:, None, :] & (y >= loc) & (y <= Hc)
        vol[vast] = rots[vast]
        diep_sub = np.where(self.steil[sl], 0, 2)[:, None, :]
        m = vast & (y >= Hc - diep_sub) & (y < Hc)
        vol[m] = np.broadcast_to(self.sub[sl][:, None, :], vol.shape)[m]
        m = vast & (y == Hc)
        vol[m] = np.broadcast_to(self.top[sl][:, None, :], vol.shape)[m]
        # the frozen lake: water up to under the ice, then the ice (open water at the outflow)
        meer = K.meer[sl][:, None, :]
        ijsy = PL.MEER_IJS
        m = meer & (y > Hc) & (y < ijsy)
        vol[m] = self.ids["minecraft:water"]
        ijs = K.ijs[sl][:, None, :]
        m = meer & ijs & (y == ijsy)
        vol[m] = np.broadcast_to(self.ijsblok[sl][:, None, :], vol.shape)[m]
        m = meer & ~ijs & (y == ijsy)
        vol[m] = self.ids["minecraft:water"]
        # the cloud sea
        vl = K.vloer[sl].astype(np.int32)
        zee = ~land
        vlc = vl[:, None, :]
        m = zee[:, None, :] & (y == vlc)
        vol[m] = self.ids["minecraft:gravel"]
        m = zee[:, None, :] & (y == vlc - 1)
        vol[m] = self.ids["minecraft:stone"]
        m = zee[:, None, :] & (y > vlc) & (y <= PL.ZEE)
        vol[m] = self.ids["minecraft:water"]
        rand = np.zeros((T, T), bool)
        if tx == PL.TX0: rand[0, :] = True
        if tx == PL.TX1: rand[-1, :] = True
        if tz == PL.TZ0: rand[:, 0] = True
        if tz == PL.TZ1: rand[:, -1] = True
        m = rand[:, None, :] & (y >= 50) & (y <= PL.ZEE + 2)
        vol[m] = self.ids["minecraft:barrier"]
        for st in self.stempels:
            ox, oy, oz = st.off
            X, Y, Z = st.v.shape
            ax0, az0 = max(x0, ox), max(z0, oz)
            ax1, az1 = min(x0 + T, ox + X), min(z0 + T, oz + Z)
            if ax0 >= ax1 or az0 >= az1:
                continue
            ay0, ay1 = max(0, oy), min(HOOG, oy + Y)
            if ay0 >= ay1:
                continue
            blok = st.v[ax0 - ox:ax1 - ox, ay0 - oy:ay1 - oy, az0 - oz:az1 - oz]
            g = st.lut[blok]
            doel = vol[ax0 - x0:ax1 - x0, ay0:ay1, az0 - z0:az1 - z0]
            m = g > 0
            doel[m] = g[m]
        for (x, yy, z, i) in self.punten.per.get((tx, tz), ()):
            vol[x - x0, yy, z - z0] = i
        vol[vol == 1] = 0
        return vol
