"""Builds the zone-4 map "De Lampionkust" as 64x320x64 structure tiles (same tile mechanism as zones 1-3, CONTRACT §10).

Called from tools/vadsrijk/maak_kaart.py (``--zone 4`` = only zone 4; ``alle`` = 1, 2, 3 and 4) in a SEPARATE process (the
render kit in zone4/kit shares module names with the other zones' kits). Standalone from the project root:
    python tools/vadsrijk/zone4/maak4.py [--alleen "tx,tz;..."] [--werkers N]
Writes src/main/resources/data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt for tx 27..39, tz -12..-5,
tools/vadsrijk/_cache/plekken_z4.json + kaart_z4.png, Z4WereldPlekken.java and POI_Z4.md (all generated: never commit); dieren4 adds zone4/dieren/Z4DierenPlekken.java (T-Z4DIEREN).

How: every tile is a window (k=1, y 0..319, world coordinates) of the approved render generators (kit/: kust = the sea and the
islands, haven = Lampionhaven, eilanden = the island themes; copies of renders-lampionkust/_bron with [zone 4 kaart] marks)
plus bouw4 (the game parts). The ground is solid from y 0 (the kit's columns start at the window bottom). The player's own
sailboat of the render and the render-only Wrak are left out (bouw4 builds the real Wrak dungeon).
Tiles tx 38..39 are shared with zone 5 (Hoogvads floats above y 170): these tiles hold only zone 4 below y 170.
"""
import argparse
import os
import sys
import time

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
sys.path.insert(1, os.path.join(HIER, "kit"))
sys.path.append(os.path.join(HIER, "..", "wereld"))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
CACHE = os.path.join(HIER, "..", "_cache")
UIT = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")

import plan4 as PL  # noqa: E402

T, HOOG = 64, 320
BLAD_EXTRA = {"guhs:guhwaii_palm_blad"}       # LeavesBlocks without "_leaves" in the name: must be persistent
EILANDBOUW = ("strandfeest", "vlieger", "koraal", "vuurtoren", "bloemen", "schildpad", "knuffeltij", "mysterie", "pier")


def staat(s):
    """Kit state -> game state: campfires unlit (kern checkpoints), palm leaves persistent."""
    if s.startswith("minecraft:campfire"):
        return "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]"
    naam = s.split("[")[0]
    if naam in BLAD_EXTRA:
        return naam + "[distance=7,persistent=true,waterlogged=false]"
    return s


class Werker:
    def __init__(self):
        import blokken as B
        self.B = B
        self.palet = B.Palet()
        self.lut_op = np.zeros(0, bool)

    def opaak(self):
        n = len(self.palet.lijst)
        if len(self.lut_op) >= n:
            return self.lut_op
        import mijnkit  # noqa: F401  (sets the asset zips)
        import vibekit as V
        r, bk = V.res()
        lut = np.zeros(n + 64, bool)
        lut[:len(self.lut_op)] = self.lut_op[:len(self.lut_op)]
        for i in range(len(self.lut_op), n):
            s = self.palet.lijst[i]
            if i < 2:
                continue
            naam = s.split("[")[0]
            if naam in self.B.EIGEN or naam.endswith(("_leaves", "barrier", "_blad")) or "glass" in naam or "slime" in naam \
                    or "honey" in naam or "water" in naam or "bubble" in naam:
                continue
            try:
                lut[i] = bool(bk.bake(s).full_opaque)
            except Exception:
                lut[i] = False
        self.lut_op = lut
        return lut

    def tegel(self, tx, tz):
        import schemerkit as SK
        import kust as KU
        import haven as HV
        import eilanden as EI
        import bouw4
        x1, z1 = tx * T, tz * T
        W = SK.Wereld(x1, 0, z1, x1 + T, HOOG, z1 + T, k=1)
        W.vrij = lambda x, z: HV.vrij(x, z) and EI.vrij(x, z)
        KU.bouw(W, ("terrein", "schuim"))
        HV.bouw(W, leven=False)
        EI.bouw(W, leven=False, alleen=EILANDBOUW)
        KU.begroeiing(W)
        KU.plantjes(W)
        bouw4.bouw(W)
        W.fix_connections()
        pal = [staat(s) for s in W.pal]
        lut = np.array([0 if s in ("minecraft:air", "minecraft:cave_air") else self.palet.id(s) for s in pal], np.uint16)
        return lut[W.v].copy()

    def hol(self, vol):
        """Hollow: an opaque block with 6 opaque neighbours is never seen (removed)."""
        lut = self.opaak()
        occ = lut[vol]
        inn = occ.copy()
        inn[0, :, :] = inn[-1, :, :] = False
        inn[:, 0, :] = inn[:, -1, :] = False
        inn[:, :, 0] = inn[:, :, -1] = False
        inn[1:-1, 1:-1, 1:-1] &= occ[:-2, 1:-1, 1:-1] & occ[2:, 1:-1, 1:-1] & occ[1:-1, :-2, 1:-1] & \
            occ[1:-1, 2:, 1:-1] & occ[1:-1, 1:-1, :-2] & occ[1:-1, 1:-1, 2:]
        # keep the bottom layer (y 0) so nothing ever looks into the void
        inn[:, 0, :] = False
        vol[inn] = 0
        return vol

    def schrijf(self, vol, pad):
        import nbtsnel
        xs, ys, zs = np.nonzero(vol)
        ids = vol[xs, ys, zs]
        gebruikt, inv = np.unique(ids, return_inverse=True)
        palette = [self.B.splits(self.palet.lijst[i]) for i in gebruikt]
        return nbtsnel.schrijf(pad, (T, HOOG, T), palette, xs.astype(np.int32), ys.astype(np.int32), zs.astype(np.int32),
                               inv.astype(np.int32), None)


_W = None


def doe_tegel(t):
    global _W
    if _W is None:
        _W = Werker()
    tx, tz = t
    t0 = time.time()
    vol = _W.tegel(tx, tz)
    heeft = vol > 0
    hy = np.where(heeft.any(1), HOOG - 1 - np.argmax(heeft[:, ::-1, :], axis=1), -1)
    hid = np.take_along_axis(vol, np.clip(hy, 0, HOOG - 1)[:, None, :], axis=1)[:, 0, :]
    top = [_W.palet.lijst[i].split("[")[0] for i in range(len(_W.palet.lijst))]
    namen = np.array(top, dtype=object)[hid]
    vol = _W.hol(vol)
    pad = os.path.join(UIT, f"t_{tx}_{tz}.nbt")
    nb = _W.schrijf(vol, pad)
    onbekend = sorted({n for n in set(top) if not _W.B.bestaat(n)})
    return tx, tz, nb, os.path.getsize(pad), hy.astype(np.int16), namen, onbekend, time.time() - t0


def maak(alleen=None, werkers=4):
    import poi4
    t0 = time.time()
    os.makedirs(CACHE, exist_ok=True)
    os.makedirs(UIT, exist_ok=True)
    poi4.schrijf(PL.plekken(), "v1 (map, plan heights)")
    tegels = [(tx, tz) for tx in range(PL.TX0, PL.TX1 + 1) for tz in range(PL.TZ0, PL.TZ1 + 1)]
    if alleen:
        tegels = [tuple(int(v) for v in s.split(",")) for s in alleen.split(";")]
        tegels = [t for t in tegels if PL.TX0 <= t[0] <= PL.TX1 and PL.TZ0 <= t[1] <= PL.TZ1]
    # heavy tiles first (Lampionhaven), so the pool ends evenly
    tegels.sort(key=lambda t: abs(t[0] * 64 + 32 - 1850) + abs(t[1] * 64 + 32 + 470))
    NX, NZ = (PL.TX1 - PL.TX0 + 1) * T, (PL.TZ1 - PL.TZ0 + 1) * T
    hgt = np.full((NX, NZ), -1, np.int16)
    nm = np.full((NX, NZ), "minecraft:air", dtype=object)
    totaal, grootte, onb = 0, 0, set()
    if werkers > 1 and len(tegels) > 1:
        import multiprocessing as mp
        with mp.get_context("spawn").Pool(werkers) as pool:
            for n, r in enumerate(pool.imap_unordered(doe_tegel, tegels)):
                totaal, grootte = verwerk(r, n, len(tegels), hgt, nm, totaal, grootte, onb, t0)
    else:
        for n, t in enumerate(tegels):
            totaal, grootte = verwerk(doe_tegel(t), n, len(tegels), hgt, nm, totaal, grootte, onb, t0)
    print(f"zone 4: {len(tegels)} tegels, {totaal} blokken, {grootte / 1e6:.1f} MB ({time.time() - t0:.0f} s)")
    if onb:
        print("zone 4 ONBEKENDE BLOKKEN:", sorted(onb))
    if not alleen:
        kaartbeeld(hgt, nm)
        # every published stand spot snapped onto the real blocks
        import poicheck4
        vervang = poicheck4.snap(poi_alle())
        poi4.schrijf(PL.plekken(), "v2 (map, stand spots snapped onto the tiles)", vervang)
        print(f"zone 4: {len(vervang)} plekken op de echte blokken gezet")
        import dieren4   # [T-Z4DIEREN] kites + Ouwe Dobber's path -> Z4DierenPlekken.java (generated)
        dieren4.schrijf()


def poi_alle():
    import poi4
    alle = {}
    poi4.plat("", PL.plekken(), alle)
    return alle


def verwerk(r, n, N, hgt, nm, totaal, grootte, onb, t0):
    tx, tz, nb, sz, hy, namen, onbekend, dt = r
    i0, j0 = (tx - PL.TX0) * T, (tz - PL.TZ0) * T
    hgt[i0:i0 + T, j0:j0 + T] = hy
    nm[i0:i0 + T, j0:j0 + T] = namen
    onb |= set(onbekend)
    if n % 10 == 0:
        print(f"  z4 tegel {n}/{N} ({tx},{tz}): {nb} blokken, {dt:.1f} s ({time.time() - t0:.0f} s)", flush=True)
    return totaal + nb, grootte + sz


def kaartbeeld(hgt, nm):
    from PIL import Image
    import kaartbeeld as KB
    namen = sorted(set(nm.ravel().tolist()))
    kl = {n: KB.kleur_van(n) for n in namen}
    beeld = np.zeros(hgt.shape + (3,), np.float32)
    for n in namen:
        beeld[nm == n] = kl[n]
    h = hgt.astype(np.float32)
    gx, gz = np.gradient(h)
    licht = np.clip(1 + (gx * 0.5 - gz * 0.7) * 0.1, 0.6, 1.35)
    out = np.clip(beeld * licht[..., None], 0, 255).astype(np.uint8).transpose(1, 0, 2)
    Image.fromarray(out).save(os.path.join(CACHE, "kaart_z4.png"))


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--alleen", default=None)
    ap.add_argument("--werkers", type=int, default=4)
    a = ap.parse_args()
    _t_verbind = time.time()   # U-HEKKEN
    maak(a.alleen, a.werkers)
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))   # U-HEKKEN: connection pass
    import verbind
    verbind.na_sinds(_t_verbind)
