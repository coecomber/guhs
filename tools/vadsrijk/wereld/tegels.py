"""Composes the map tiles: terrain columns from the Kaart + stamps (scenes) + points, then hollows and writes .nbt tiles.

A stamp ("Stempel") is a numpy volume v[x,y,z] (scene palette indices) with a palette and a world offset; scene index 0
= leave alone, the block "minecraft:cave_air" = carve (air). Points are single blocks in world coords.
"""
import math
import os

import numpy as np

import blokken as B
import kaart as KA
import nbtsnel

T = 64
HOOG = 320


class Stempel:
    def __init__(self, naam, v, pal, off):
        self.naam, self.v, self.pal, self.off = naam, v, list(pal), tuple(int(o) for o in off)
        self.lut = None

    @property
    def doos(self):
        X, Y, Z = self.v.shape
        ox, oy, oz = self.off
        return ox, oz, ox + X - 1, oz + Z - 1


class Punten:
    def __init__(self, palet):
        self.palet = palet
        self.per = {}

    def set(self, x, y, z, s):
        x, y, z = int(x), int(y), int(z)
        if not 0 <= y < HOOG:
            return
        k = (x // T, z // T)
        self.per.setdefault(k, []).append((x, y, z, self.palet.id(s)))


# rock layers per kind: (list of block names), band height 3
GESTEENTE = {
    "steen": ["minecraft:stone", "minecraft:andesite", "minecraft:stone", "minecraft:tuff", "minecraft:stone",
              "minecraft:calcite", "minecraft:stone", "minecraft:diorite"],
    "grot": ["minecraft:tuff", "minecraft:stone", "minecraft:deepslate", "minecraft:tuff", "minecraft:andesite",
             "minecraft:dripstone_block", "minecraft:stone", "minecraft:cobbled_deepslate"],
    "berg": ["minecraft:pink_terracotta", "minecraft:white_terracotta", "minecraft:calcite", "minecraft:pink_terracotta",
             "minecraft:terracotta", "minecraft:white_terracotta", "minecraft:light_gray_terracotta", "minecraft:calcite"],
    "woud": ["minecraft:stone", "minecraft:mossy_cobblestone", "minecraft:andesite", "minecraft:stone", "minecraft:tuff",
             "minecraft:mossy_cobblestone", "minecraft:stone", "minecraft:andesite"],
}
SOORT = {"steen": 0, "grot": 1, "berg": 2, "woud": 3}


class Wereld:
    def __init__(self, K):
        self.K = K
        self.palet = B.Palet()
        self.stempels = []
        self.punten = Punten(self.palet)
        self.lo_extra = np.full((KA.NX, KA.NZ), 9999, np.int16)   # caves: fill columns down to here
        self.bezet = np.zeros((KA.NX, KA.NZ), bool)                # columns with buildings (no decoration)
        self.oppervlak()

    # ------------------------------------------------------------ materials
    def oppervlak(self):
        K, P = self.K, self.palet
        H = K.H.astype(np.int32)
        n = K.namen
        g = K.gebied
        f = K.fijn
        rnd = np.random.default_rng(77).random((KA.NX, KA.NZ))
        Hp = np.pad(H, 1, mode="edge")
        buur_min = np.minimum.reduce([Hp[:-2, 1:-1], Hp[2:, 1:-1], Hp[1:-1, :-2], Hp[1:-1, 2:]])
        buur_max = np.maximum.reduce([Hp[:-2, 1:-1], Hp[2:, 1:-1], Hp[1:-1, :-2], Hp[1:-1, 2:]])
        self.buur_min = buur_min
        steil = (H - buur_min >= 3) | (buur_max - H >= 4)
        self.steil = steil
        top = np.full((KA.NX, KA.NZ), P.id("minecraft:grass_block[snowy=false]"), np.uint16)
        sub = np.full((KA.NX, KA.NZ), P.id("minecraft:dirt"), np.uint16)
        soort = np.zeros((KA.NX, KA.NZ), np.uint8)

        def zet(mask, t=None, s=None, k=None):
            if t is not None:
                top[mask] = P.id(t) if isinstance(t, str) else t[mask]
            if s is not None:
                sub[mask] = P.id(s)
            if k is not None:
                soort[mask] = SOORT[k]

        G = {k: g == i for i, k in enumerate(n)}
        # Sporenwoud: moss, podzol, mycelium, rooted dirt
        w = G["woud"]
        zet(w, "minecraft:moss_block", "minecraft:dirt", "woud")
        zet(w & (f > 0.25), "minecraft:podzol[snowy=false]")
        zet(w & (f < -0.45), "minecraft:mycelium[snowy=false]")
        zet(w & (rnd < 0.04), "minecraft:rooted_dirt")
        # Druipgrotten: rock with grassy hollows
        gr = G["grotten"]
        zet(gr, "minecraft:stone", "minecraft:stone", "grot")
        zet(gr & (H < 84) & (f > -0.2), "minecraft:grass_block[snowy=false]", "minecraft:dirt")
        zet(gr & (f > 0.55), "minecraft:gravel")
        zet(gr & (f < -0.55) & (H >= 84), "minecraft:tuff")
        zet(gr & (rnd < 0.05) & (H >= 84), "minecraft:andesite")
        # temple mountain: pink badlands bands above 96, grass below
        tp = G["tempel"]
        zet(tp, None, None, "berg")
        zet(tp & (H >= 96), "minecraft:pink_terracotta", "minecraft:white_terracotta")
        zet(tp & (H >= 96) & (f > 0.3), "minecraft:calcite")
        zet(tp & (H < 96) & (f > 0.4), "minecraft:coarse_dirt")
        # Klodderweide + the south: grass with pink meadow patches
        for k in ("weide", "zuid", "zuidoost", "zuidwest"):
            zet(G[k] & (f > 0.5), "guhs:weide_roze_gras")
        # city + haven ground: knuffelgras
        zet(G["stad"], "guhs:knuffelgras")
        hd = K.haven_d + 3 * f
        hv = hd < 0
        zet(hv, "guhs:knuffelgras")
        zet((hd >= 0) & (hd < 7) & (rnd < 0.55 - hd / 14), "guhs:knuffelgras")
        # steep: bare rock on top, no dirt
        steen = np.array([P.id(GESTEENTE[k][0]) for k in GESTEENTE], np.uint16)
        top = np.where(steil & (H > 70), steen[soort], top)
        sub = np.where(steil & (H > 70), steen[soort], sub)
        # beaches, pond and stream beds
        strand = (H <= 65) & (K.kustd < 7)
        zet(strand, "minecraft:sand", "minecraft:sandstone")
        nat = H < K.wl
        zet(nat & ~strand, "minecraft:clay", "minecraft:dirt")
        zet(nat & ~strand & (f > 0.1), "minecraft:sand", "minecraft:dirt")
        zet(K.kloof & (H < 40), "minecraft:gravel", "minecraft:stone", "grot")
        # paths
        pad = K.pad > 0.5
        zet(pad, "minecraft:dirt_path")
        zet(pad & (rnd < 0.18), "minecraft:coarse_dirt")
        zet(pad & (rnd > 0.9), "minecraft:gravel")
        zet(pad & (G["stad"] | hv), "guhs:knuffelklinkers")
        zet(K.bergpad, "minecraft:stone_bricks", None, "berg")
        zet(K.bergpad & (rnd < 0.3), "minecraft:mossy_stone_bricks")
        zet(K.bergpad & (rnd > 0.85), "minecraft:cracked_stone_bricks")
        # the grey dragon scars (client-tinted: grey until zone1_klaar)
        self.litteken = np.zeros((KA.NX, KA.NZ), bool)
        for lijn in KA.LITTEKENS:
            d, _ = KA.polyafstand(lijn)
            self.litteken |= d < 20 + 9 * K.n1 + 5 * f
        groen = np.isin(top, [P.id(s) for s in ("minecraft:grass_block[snowy=false]", "minecraft:moss_block",
                                                 "minecraft:podzol[snowy=false]", "guhs:weide_roze_gras", "guhs:knuffelgras",
                                                 "minecraft:mycelium[snowy=false]", "minecraft:coarse_dirt")])
        zet(self.litteken & groen, "guhs:vr_w_litteken_gras")
        self.top, self.sub, self.soort = top, sub, soort
        self.gest = np.array([[P.id(b) for b in GESTEENTE[k]] for k in GESTEENTE], np.uint16)
        self.boff = (2 * np.sin(KA.XX / 7.0) + 2 * np.cos(KA.ZZ / 9.0) + 8).astype(np.int32)
        self.ids = {s: P.id(s) for s in ("minecraft:water", "minecraft:sand", "minecraft:stone", "minecraft:barrier",
                                          "minecraft:light_blue_terracotta", "minecraft:prismarine", "minecraft:gravel",
                                          "minecraft:sandstone", "minecraft:clay")}

    # ------------------------------------------------------------ stamps
    def stempel(self, st, bezet=True):
        st.lut = self.palet.lut(st.pal)
        self.stempels.append(st)
        self.onder_uitgraving(st)
        if bezet:
            voet = np.any(st.v > 0, axis=1)
            ox, oy, oz = st.off
            X, Z = voet.shape
            i0, j0 = ox - KA.X0, oz - KA.Z0
            a0, b0 = max(0, -i0), max(0, -j0)
            a1, b1 = min(X, KA.NX - i0), min(Z, KA.NZ - j0)
            if a1 > a0 and b1 > b0:
                self.bezet[i0 + a0:i0 + a1, j0 + b0:j0 + b1] |= voet[a0:a1, b0:b1]

    def onder_uitgraving(self, st):
        """Fix round 1: where a stamp carves into the terrain (caves, cuts, plazas), the terrain skin of the carved columns
        AND their neighbours (2 around) must reach below the carved floor, or the hollow space under the skin shows
        through the side of the cut (you could fall out of a cave under the map)."""
        from scipy import ndimage
        uit = st.lut[st.v] == 1
        if not uit.any():
            return
        X, Y, Z = st.v.shape
        heeft = uit.any(1)
        ymin = np.where(heeft, np.argmax(uit, axis=1), 10 ** 4).astype(np.int32)
        ymin = ndimage.minimum_filter(ymin, size=5, mode="constant", cval=10 ** 4)
        ox, oy, oz = st.off
        i0, j0 = ox - KA.X0, oz - KA.Z0
        a0, b0 = max(0, -i0), max(0, -j0)
        a1, b1 = min(X, KA.NX - i0), min(Z, KA.NZ - j0)
        if a1 <= a0 or b1 <= b0:
            return
        doel = self.lo_extra[i0 + a0:i0 + a1, j0 + b0:j0 + b1]
        nieuw = np.where(ymin[a0:a1, b0:b1] < 10 ** 4, ymin[a0:a1, b0:b1] + oy - 2, 9999)
        self.lo_extra[i0 + a0:i0 + a1, j0 + b0:j0 + b1] = np.minimum(doel, nieuw).astype(np.int16)

    def blok_op(self, x, y, z):
        """Global id of what the stamps put at a world position (last stamp wins; 0 = nothing)."""
        for st in reversed(self.stempels):
            ox, oy, oz = st.off
            X, Y, Z = st.v.shape
            if ox <= x < ox + X and oy <= y < oy + Y and oz <= z < oz + Z:
                i = st.v[x - ox, y - oy, z - oz]
                if i:
                    return int(st.lut[i])
        return 0

    # ------------------------------------------------------------ one tile
    def tegel(self, tx, tz):
        K = self.K
        x0, z0 = tx * T, tz * T
        i0, j0 = x0 - KA.X0, z0 - KA.Z0
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
        # rock
        band = ((y + self.boff[sl][:, None, :]) // 3) % 8
        rots = self.gest[self.soort[sl][:, None, :], band]
        vast = land[:, None, :] & (y >= loc) & (y <= Hc)
        vol[vast] = rots[vast]
        diep_sub = np.where(self.steil[sl], 0, 3)[:, None, :]
        m = vast & (y >= Hc - diep_sub) & (y < Hc)
        vol[m] = np.broadcast_to(self.sub[sl][:, None, :], vol.shape)[m]
        m = vast & (y == Hc)
        vol[m] = np.broadcast_to(self.top[sl][:, None, :], vol.shape)[m]
        # water on land (ponds, stream, shallow shore)
        wl = np.round(K.wl[sl]).astype(np.int32)[:, None, :]
        m = land[:, None, :] & (y > Hc) & (y <= wl)
        vol[m] = self.ids["minecraft:water"]
        # sea
        vl = K.vloer[sl].astype(np.int32)
        zd = K.zeed[sl]
        vloerblok = np.where(zd < 5, self.ids["minecraft:sand"], np.where(zd < 16, self.ids["minecraft:prismarine"],
                                                                         self.ids["minecraft:light_blue_terracotta"]))
        zee = ~land
        vlc = vl[:, None, :]
        m = zee[:, None, :] & (y == vlc)
        vol[m] = np.broadcast_to(vloerblok[:, None, :], vol.shape)[m]
        m = zee[:, None, :] & (y == vlc - 1) & (zd < 5)[:, None, :]
        vol[m] = self.ids["minecraft:sandstone"]
        m = zee[:, None, :] & (y > vlc) & (y <= KA.ZEE)
        vol[m] = self.ids["minecraft:water"]
        # the very edge of the map: an invisible wall so the sea never runs into the void
        rand = np.zeros((T, T), bool)
        if tx == KA.TX0: rand[0, :] = True
        if tx == KA.TX1: rand[-1, :] = True
        if tz == KA.TZ0: rand[:, 0] = True
        if tz == KA.TZ1: rand[:, -1] = True
        m = rand[:, None, :] & (y >= 50) & (y <= KA.ZEE + 2)
        vol[m] = self.ids["minecraft:barrier"]
        # stamps
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
        # points
        for (x, yy, z, i) in self.punten.per.get((tx, tz), ()):
            vol[x - x0, yy, z - z0] = i
        vol[vol == 1] = 0      # carve
        return vol

    # ------------------------------------------------------------ hollow + write
    def opaak_lut(self):
        import importlib
        V = importlib.import_module("vibekit")
        r, bk = V.res()
        lut = np.zeros(len(self.palet.lijst) + 16, bool)
        for i, s in enumerate(self.palet.lijst):
            if i < 2:
                continue
            naam = s.split("[")[0]
            if naam in B.EIGEN or naam.endswith(("_leaves", "barrier")) or "glass" in naam or "slime" in naam or "honey" in naam:
                continue
            try:
                lut[i] = bool(bk.bake(s).full_opaque)
            except Exception:
                lut[i] = False
        return lut

    def hol(self, vol, lut):
        occ = lut[vol]
        inn = occ.copy()
        inn[0, :, :] = inn[-1, :, :] = False
        inn[:, 0, :] = inn[:, -1, :] = False
        inn[:, :, 0] = inn[:, :, -1] = False
        inn[1:-1, 1:-1, 1:-1] &= occ[:-2, 1:-1, 1:-1] & occ[2:, 1:-1, 1:-1] & occ[1:-1, :-2, 1:-1] & \
            occ[1:-1, 2:, 1:-1] & occ[1:-1, 1:-1, :-2] & occ[1:-1, 1:-1, 2:]
        vol[inn] = 0
        return vol

    def schrijf(self, vol, pad, extra_nbt=None):
        xs, ys, zs = np.nonzero(vol)
        ids = vol[xs, ys, zs]
        gebruikt, inv = np.unique(ids, return_inverse=True)
        palette = [B.splits(self.palet.lijst[i]) for i in gebruikt]
        return nbtsnel.schrijf(pad, (T, HOOG, T), palette, xs.astype(np.int32), ys.astype(np.int32), zs.astype(np.int32),
                               inv.astype(np.int32), extra_nbt)
