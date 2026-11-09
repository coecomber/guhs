"""X-Z1GROND: foundations for zone 1 (+ the Knuffeleiland / Knuffelkamp tutorial island), the zone-1 counterpart of
zone6/fundering6 (round X: the public release opens zone 1 first, so it must look tidy: "gebouwen sluiten niet aan op de grond,
dingen zweven, water loopt raar").

    grond(K, kamp_npz)  -> (T int16 (NX, NZ) game y of the ground per column): the Kaart's land height (K.H), a pond's water
                           surface where it is above the bed, the sea level (KA.ZEE) on sea columns, and the Knuffelkamp island's
                           own height map (kamp.npz "hoogte", written by kamp/kamp_wereld.py) under the camp scene.
    paden(K)            -> (NX, NZ) bool path columns (a gap of 2+ over a path is a passage under an arch: never filled).
    schrijf_cache(T, p) -> tools/vadsrijk/_cache/z1/terrein.npz, read by tools/vadsrijk/funderingcheck.py (height-based zone 1).
    naloop(tegels, T, p) the pass over the written zone-1 tiles (maak_kaart runs it right after the zone-1 tiles, before the
                           shared overgang/verbind/vrijpad passes). Idempotent.
    python tools/vadsrijk/wereld/fundering1.py [--pas [--alleen tx,tz;...]]
                           writes the cache (Kaart ~25 s); --pas also runs the pass on the existing tiles.

The pass uses the detection of tools/vadsrijk/funderingcheck_hoogte.vind (solid vs not + the ground height, no name lists):
the underside b of the first overhang above a column's ground with 1..16 open cells under it, where the SAME structure meets its
ground right next to it (a column within 5 that is solid at b and b - 1 with its ground at b - 1): a wall / floor / stem / log
row standing on the ground and running on over a dip. Never over water, never in a cave mouth / natural overhang, never in a
passage of 2+ over a built floor or a path, never under a mushroom cap's rim, never in the BEWUST areas (fossil bridge, pier).
  - under a plain full block (a wall, a stem, a log, rock): the same block goes down to the ground (gaps up to 16);
  - under anything else (a stair, slab, fence, door, glass): the column's own ground block (the ground rises; gaps up to 6).
Also:
  - floating bits (a component of <= 64 blocks touching nothing): a pole (fence/wall/log) hanging over something solid goes
    down to it; a lamp (lantern/chain/shroomlight) hanging under a ceiling 1..4 up gets a chain up to it; a lone plant /
    carpet / dripstone in the air is removed; a lone block (a chest) 1..3 over the ground gets ground under it;
  - water that would leak: an air cell beside or under water with something solid (or water) right above it (a hollow under a
    pond rim) is filled with the pond's bed block.
"""
import os
import sys
import time

import numpy as np
from scipy import ndimage

HIER = os.path.dirname(os.path.abspath(__file__))
VR = os.path.abspath(os.path.join(HIER, ".."))
CACHE = os.path.join(VR, "_cache")
sys.path.insert(0, VR)
import funderingcheck_hoogte as FH  # noqa: E402

# areas that float ON PURPOSE (world x1, z1, x2, z2): no foundation there, and funderingcheck skips them
BEWUST = [(100, -240, 170, -160),     # the dragon-fossil bridge over the Kloof (bone ribs + deck between the rims)
          (138, -660, 162, -581)]     # the Noordklifhaven pier out over the cliff edge (on posts)
MAX_KOPIE, MAX_GROND = 16, 6
_NIET_KOPIE = ("half=", "type=", "facing=", "_fence", "_wall", "door", "pane", "glass", "glas", "lamp", "lantaarn", "froglight",
               "gold_block", "goud", "kroonlijst", "kussen", "_bed", "chest", "barrel", "slab", "stairs", "trap", "plaat",
               "hek", "tralie", "ruit", "kaars", "mozaiek", "spiegel", "kristal", "campfire", "anvil", "table", "shroomlight",
               "axis=x", "axis=z", "bone_block")
_PAAL = ("_fence", "_wall", "_log", "_stam", "_post", "paal", "bars")
_LAMP = ("lantern", "chain", "shroomlight", "froglight", "lantaarn", "lampion")
_PLANT = ("mushroom", "moss_carpet", "pointed_dripstone", "short_grass", "fern", "flower", "_carpet", "sapling", "petals")


def grond(K, kamp_npz=None):
    import kaart as KA
    H = K.H.astype(np.int32)
    wl = np.round(K.wl).astype(np.int32)
    T = np.where(H >= 0, np.maximum(H, np.where(wl > H, wl, H)), KA.ZEE).astype(np.int16)
    if kamp_npz is None:
        kamp_npz = os.path.join(CACHE, "kamp.npz")
    if os.path.exists(kamp_npz):
        d = np.load(kamp_npz)
        if "hoogte" in d.files:
            ho = d["hoogte"].astype(np.int32)
            ox, _oy, oz = (int(v) for v in d["off"])
            i0, j0 = ox - KA.X0, oz - KA.Z0
            X, Z = ho.shape
            a0, b0 = max(0, -i0), max(0, -j0)
            a1, b1 = min(X, KA.NX - i0), min(Z, KA.NZ - j0)
            sub = ho[a0:a1, b0:b1]
            doel = T[i0 + a0:i0 + a1, j0 + b0:j0 + b1]
            doel[sub >= 0] = sub[sub >= 0]
    return T


def paden(K):
    """(NX, NZ) bool: the path columns (+1 around): a gap of 2+ over a path is a passage under an arch, never filled."""
    p = (K.pad > 0.5) | K.bergpad
    return ndimage.binary_dilation(p, iterations=1)


def schrijf_cache(T, pad=None):
    import kaart as KA
    d = os.path.join(CACHE, "z1")
    os.makedirs(d, exist_ok=True)
    extra = {} if pad is None else {"pad": pad}
    np.savez_compressed(os.path.join(d, "terrein.npz"), T=T, b=np.array([KA.X0, 0, KA.Z0], float), **extra)


# ------------------------------------------------------------------------------------------------ the pass
def _venster2d(A, x0, z0, gx0, gz0, n, vul):
    W = np.full((n, n), vul, A.dtype)
    a0, c0 = max(0, x0 - gx0), max(0, z0 - gz0)
    a1, c1 = min(n, A.shape[0] - (gx0 - x0)), min(n, A.shape[1] - (gz0 - z0))
    if a1 > a0 and c1 > c0:
        W[a0:a1, c0:c1] = A[gx0 - x0 + a0:gx0 - x0 + a1, gz0 - z0 + c0:gz0 - z0 + c1]
    return W


def naloop(tegels, T, pad, x0, z0, map_=None, praat=True, bewust=None, naam="fundering1"):
    """The pass over the given zone-1 tiles. T / pad: the (NX, NZ) ground heights and path mask with origin (x0, z0)."""
    if os.environ.get("GUHS_GEEN_FUNDERING"):          # (debug: build without the pass, to compare)
        return {}
    sys.path.insert(0, VR)
    import verbind as VB
    t_0 = time.time()
    TG = VB._Tegels(map_ or VB.UIT)
    m = FH.R_RAAK + 1
    n = 64 + 2 * m
    tel = dict(kolommen=0, blokken=0, paal=0, ketting=0, plant=0, onder=0, lek=0, tegels=0)
    luts = {}

    def lut():
        L = TG.pal
        if luts.get("n") != len(L):
            luts["n"] = len(L)
            luts["kl"] = np.array([FH.klasse(s) if i else FH.LUCHT for i, s in enumerate(L)], np.uint8)
            luts["kopie"] = np.array([not any(w in s for w in _NIET_KOPIE) for s in L], bool)
            luts["paal"] = np.array([any(w in s.split("[")[0] for w in _PAAL) for s in L], bool)
            luts["lamp"] = np.array([any(w in s.split("[")[0] for w in _LAMP) for s in L], bool)
            luts["plant"] = np.array([any(w in s.split("[")[0] for w in _PLANT) for s in L], bool)
        return luts
    ketting = TG.id("minecraft:iron_chain[axis=y,waterlogged=false]")
    aarde = TG.id("minecraft:dirt")
    lucht = TG.id("minecraft:air")
    for t in tegels:
        v = TG.dicht(t)
        if v is None:
            continue
        w = VB.venster(TG, t, m)
        L = lut()
        V = L["kl"][w]
        gx0, gz0 = t[0] * 64 - m, t[1] * 64 - m
        Tw = _venster2d(T.astype(np.int32), x0, z0, gx0, gz0, n, -1)
        Pw = _venster2d(pad, x0, z0, gx0, gz0, n, False)
        Bw = FH.bewust_masker(BEWUST if bewust is None else bewust, gx0, gz0, n)
        gaten, buiten = FH.vind(V, Tw, m, Bw, Pw)
        k_ = 0
        for (i, k, ss, bb) in gaten:
            top = int(w[i, bb, k])
            gap = bb - ss
            if L["kopie"][top]:
                vul = top
            elif gap <= MAX_GROND:
                steun = int(w[i, ss - 1, k])
                vul = steun if V[i, ss - 1, k] == FH.GROND else aarde
            else:
                continue
            kol = v[i - m, ss:bb, k - m]
            vrij = L["kl"][kol] <= FH.ZACHTK
            kol[vrij] = vul
            tel["kolommen"] += 1
            tel["blokken"] += int(vrij.sum())
            k_ += 1
        # floating bits (components of any non-air non-water blocks, <= 64, touching nothing)
        iets = (V != FH.LUCHT) & (V != FH.WATER)
        lb, nb = ndimage.label(iets, structure=np.ones((3, 3, 3), bool))
        if nb:
            maat = ndimage.sum(np.ones_like(lb), lb, index=np.arange(1, nb + 1))
            sl = ndimage.find_objects(lb)
            nat = ndimage.binary_dilation(V == FH.WATER) if (V == FH.WATER).any() else None
            for j, mm in enumerate(maat, 1):
                if mm > FH.MAX_BROK:
                    continue
                s_ = sl[j - 1]
                if s_[0].start == 0 or s_[0].stop == n or s_[2].start == 0 or s_[2].stop == n or s_[1].start == 0:
                    continue
                cel = np.argwhere(lb[s_] == j) + np.array([s_[0].start, s_[1].start, s_[2].start])
                if nat is not None and nat[cel[:, 0], cel[:, 1], cel[:, 2]].any():
                    continue
                if Bw is not None and Bw[cel[:, 0], cel[:, 2]].any():
                    continue
                if any(any(q in TG.pal[int(w[c[0], c[1], c[2]])] for q in FH._SLINGER) for c in cel):
                    continue                                   # a garland strung between poles
                k_ += _brokje(v, w, V, cel, m, L, tel, ketting, aarde, lucht)
        # water that would leak into a hollow under a pond rim
        k_ += _lek(v, w, V, m, buiten, tel)
        if k_:
            TG.schrijf(t, v, extra=TG.nbt(t))
            tel["tegels"] += 1
    if praat:
        print(f"{naam}: {len(tegels)} tegels, {tel['tegels']} herschreven: {tel['kolommen']} kolommen / {tel['blokken']} "
              f"blokken fundering, {tel['paal']} palen naar de grond, {tel['ketting']} lampen aan een ketting, {tel['plant']} "
              f"zwevende plantjes weg, {tel['onder']} blokjes onderstut, {tel['lek']} lekgaatjes dicht ({time.time() - t_0:.0f} s)")
    return tel


def _binnen(m, i, k):
    return m <= i < 64 + m and m <= k < 64 + m


def _brokje(v, w, V, cel, m, L, tel, ketting, aarde, lucht):
    """One floating component (window coords cel (N, 3)): fix it when it is a pole, a lamp, a plant or a lone block."""
    ids = w[cel[:, 0], cel[:, 1], cel[:, 2]]
    laag = cel[cel[:, 1] == cel[:, 1].min()]
    hoog = cel[cel[:, 1] == cel[:, 1].max()]
    vast = (V == FH.VAST) | (V == FH.GROND) | (V == FH.HOED)
    # a lone plant / carpet / dripstone (1-2 blocks) in the air: away
    if len(cel) <= 2 and L["plant"][ids].all():
        for (i, y, k) in cel:
            if _binnen(m, i, k):
                v[i - m, y, k - m] = lucht
                tel["plant"] += 1
        return 1
    # a pole hanging over something solid (<= 6 below): the pole goes down to it
    for (i, y, k) in laag:
        if L["paal"][w[i, y, k]] and _binnen(m, i, k):
            for d in range(1, 7):
                if y - d < 1:
                    break
                if vast[i, y - d, k]:
                    for yy in range(y - d + 1, y):
                        v[i - m, yy, k - m] = w[i, y, k]
                    tel["paal"] += 1
                    return 1
                if V[i, y - d, k] == FH.WATER:
                    break
    # a lamp hanging under a ceiling 1..4 up: a chain up to it
    for (i, y, k) in hoog:
        if L["lamp"][w[i, y, k]] and _binnen(m, i, k):
            for d in range(1, 6):
                if y + d >= V.shape[1]:
                    break
                if vast[i, y + d, k]:
                    for yy in range(y + 1, y + d):
                        v[i - m, yy, k - m] = ketting
                    tel["ketting"] += 1
                    return 1
    # a lone block 1..3 over the ground (a chest, a barrel): ground under it
    if len(cel) == 1:
        i, y, k = cel[0]
        if _binnen(m, i, k):
            for d in range(2, 5):
                if y - d < 1:
                    break
                if V[i, y - d, k] == FH.GROND:
                    for yy in range(y - d + 1, y):
                        v[i - m, yy, k - m] = w[i, y - d, k]
                    tel["onder"] += 1
                    return 1
                if V[i, y - d, k] != FH.LUCHT:
                    break
    return 0


def _lek(v, w, V, m, buiten, tel):
    """Air beside / under a water cell (inside this tile), with something solid or water right above that air cell (a hollow
    under a pond rim, not an open waterfall edge): filled with the nearest bed block under the water."""
    X, Y, Z = V.shape
    water = V == FH.WATER
    if not water[m:-m, :, m:-m].any():
        return 0
    n = 0
    lucht = V == FH.LUCHT
    ii, yy, kk = np.nonzero(water[m:-m, :, m:-m])
    for i, y, k in zip((ii + m).tolist(), yy.tolist(), kk.tolist()):
        for (dx, dy, dz) in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
            a, b, c = i + dx, y + dy, k + dz
            if not (_binnen(m, a, c) and 1 <= b < Y - 1) or not lucht[a, b, c]:
                continue
            if V[a, b + 1, c] == FH.LUCHT or V[a, b + 1, c] == FH.ZACHTK:
                continue                                  # open air above: an edge you can see, not a hollow
            bed = None
            for d in range(1, 8):
                if y - d < 0:
                    break
                if V[i, y - d, k] in (FH.GROND, FH.VAST):
                    bed = int(w[i, y - d, k])
                    break
            if bed is None:
                continue
            v[a - m, b, c - m] = bed
            w[a, b, c] = bed
            V[a, b, c] = FH.GROND
            lucht[a, b, c] = False
            n += 1
    tel["lek"] += n
    return n


if __name__ == "__main__":
    import argparse
    sys.path.insert(0, HIER)
    ap = argparse.ArgumentParser()
    ap.add_argument("--pas", action="store_true")
    ap.add_argument("--alleen", default=None)
    a = ap.parse_args()
    import kaart as KA_
    K_ = KA_.Kaart()
    T_, P_ = grond(K_), paden(K_)
    schrijf_cache(T_, P_)
    print("z1 terrein:", T_.shape, int(T_.min()), int(T_.max()))
    if a.pas:
        if a.alleen:
            lijst = [tuple(int(q) for q in s.split(",")) for s in a.alleen.split(";")]
        else:
            lijst = [(tx, tz) for tx in range(KA_.TX0, KA_.TX1 + 1) for tz in range(KA_.TZ0, KA_.TZ1 + 1)]
        naloop(lijst, T_, P_, KA_.X0, KA_.Z0)
