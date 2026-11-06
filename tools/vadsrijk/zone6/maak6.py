"""Builds the zone-6 map "Het Zijdeland" (U-Z6WERELD) as 64x320x64 structure tiles (CONTRACT §10, like zones 1-5).

Called from tools/vadsrijk/maak_kaart.py (``--zone 6`` = only zone 6; ``alle`` = everything) in a SEPARATE process (the
render kit in zone6/kit shares module names with the other zones' kits). Standalone from the project root:
    python tools/vadsrijk/zone6/maak6.py [--alleen "tx,tz;..."] [--geen-snap]
Writes src/main/resources/data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt for tx 51..66, tz -18..-7,
tools/vadsrijk/_cache/plekken_z6.json + kaart_z6.png + z6/wereld.npz, Z6WereldPlekken.java and POI_Z6.md
(generated: do NOT commit them).

How: the kit (kit/ = the approved concept generators of renders-zijdeland/_bron, [zone 6 kaart] marks) works in REAL game
coordinates, so the whole zone is ONE local window (plan6.VENSTER, ~1 GB peak) and every tile is a plain slice of it:
land -> roads (bouw6.wegen) -> Rozenkloof -> Saffraanpoort -> landmarks (bouw6) -> green -> figures.
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
sys.path.append(os.path.join(HIER, "..", "audit"))
sys.path.append(os.path.join(HIER, ".."))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
CACHE = os.path.join(HIER, "..", "_cache")
UIT = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")

import plan6 as PL  # noqa: E402

T, HOOG = PL.T, PL.HOOG


def log(*a):
    print(time.strftime("%H:%M:%S"), "z6", *a, flush=True)


def _vrij_van_steden(x, z):
    """No wild palms/flowers in the cities or on the canyon ledges (they have their own green) or on the roads."""
    import zijwereld as ZW
    sx, sz = ZW.SAFFRAAN
    if ((x - sx) / (ZW.STAD_R[0] + 8)) ** 2 + ((z - sz) / (ZW.STAD_R[1] + 8)) ** 2 < 1:
        return True
    if 3290 < x < 3510 and -800 < z < -640:
        return True
    return False


# ------------------------------------------------------------------------------------------------ the local world
def bouw_wereld(figuren=True):
    import hoogkit  # noqa: F401  (jars + the repo's own zone-5/6 blocks)
    import schemerkit as SK
    import zijwereld as ZW
    import rozenkloof as RK
    import saffraanpoort as SP
    import bouw6
    t0 = time.time()
    W = SK.Wereld(*PL.VENSTER, 1.0)
    ZW.bouw_land(W)
    log(f"land ({time.time() - t0:.0f} s)")
    bouw6.wegen(W)
    log(f"wegen ({time.time() - t0:.0f} s)")
    if not os.environ.get("GUHS_Z6_ZONDER_STEDEN"):      # (test switch: the land without the two cities)
        RK.bouw(W, figuren=figuren)
        log(f"Rozenkloof ({time.time() - t0:.0f} s)")
        SP.bouw(W, figuren=figuren)
        log(f"Saffraanpoort ({time.time() - t0:.0f} s)")
    bouw6.landmarks(W, log=lambda *a: log(*a))
    log(f"landmarks ({time.time() - t0:.0f} s)")
    pad_cellen = set()
    for prof in getattr(W, "wegen", {}).values():
        for (x, z, _y) in prof:
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    pad_cellen.add((int(x) + dx, int(z) + dz))
    ZW.groen(W, uitsluit=lambda x, z: _vrij_van_steden(x, z) or (int(x), int(z)) in pad_cellen)
    if figuren:
        import zijfiguren as ZF
        ZF.bevolk(W)
    log(f"groen + figuren ({time.time() - t0:.0f} s), palet {len(W.pal)}")
    return W


# ------------------------------------------------------------------------------------------------ palette + opacity
class Omzet:
    """Kit palette -> global tile palette (26.1 names) + the opaque lookup for hollowing."""

    def __init__(self, W):
        import blokken as B
        import vibekit as V
        self.B = B
        self.pal = B.Palet()
        r, bk = V.res()
        lut = np.zeros(len(W.pal), np.uint16)
        for i, s in enumerate(W.pal):
            naam = s.split("[")[0]
            if naam in ("minecraft:air", "minecraft:cave_air"):
                continue
            lut[i] = self.pal.id(s)
        self.lut = lut
        n = len(self.pal.lijst)
        op = np.zeros(n + 4096, bool)
        for i in range(2, n):
            s = self.pal.lijst[i]
            naam = s.split("[")[0]
            if naam.endswith(("_leaves", "barrier")) or "glass" in naam or "glas" in naam or "water" in naam:
                continue
            try:
                op[i] = bool(bk.bake(s).full_opaque)
            except Exception:
                op[i] = False
        self.op = op

    def id(self, s):
        return self.pal.id(s)


def tegel(W, O, tx, tz):
    """uint16 (64, 320, 64) [x, y, z] of tile (tx, tz) (global palette ids): a plain slice of the window."""
    vol = np.zeros((T, HOOG, T), np.uint16)
    x1, y1, z1 = (int(v) for v in W.b[:3])
    X, Y, Z = W.v.shape
    lx0, lz0 = tx * T - x1, tz * T - z1
    if lx0 < 0 or lz0 < 0 or lx0 + T > X or lz0 + T > Z:
        return vol
    sub = W.v[lx0:lx0 + T, :, lz0:lz0 + T]
    vol[:, y1:y1 + Y, :] = O.lut[sub]
    return vol


def hol(vol, op):
    occ = op[vol]
    inn = occ.copy()
    inn[0, :, :] = inn[-1, :, :] = False
    inn[:, 0, :] = inn[:, -1, :] = False
    inn[:, :, 0] = inn[:, :, -1] = False
    inn[1:-1, 1:-1, 1:-1] &= occ[:-2, 1:-1, 1:-1] & occ[2:, 1:-1, 1:-1] & occ[1:-1, :-2, 1:-1] & \
        occ[1:-1, 2:, 1:-1] & occ[1:-1, 1:-1, :-2] & occ[1:-1, 1:-1, 2:]
    vol = vol.copy()
    vol[inn] = 0
    return vol


def schrijf(O, vol, pad):
    import nbtsnel
    xs, ys, zs = np.nonzero(vol)
    ids = vol[xs, ys, zs]
    gebruikt, inv = np.unique(ids, return_inverse=True)
    palette = [O.B.splits(O.pal.lijst[i]) for i in gebruikt]
    return nbtsnel.schrijf(pad, (T, HOOG, T), palette, xs.astype(np.int32), ys.astype(np.int32), zs.astype(np.int32),
                           inv.astype(np.int32), None)


# ------------------------------------------------------------------------------------------------ main
def maak(alleen=None, snap=True):
    import poi6
    t0 = time.time()
    os.makedirs(CACHE, exist_ok=True)
    os.makedirs(UIT, exist_ok=True)
    W = bouw_wereld()
    log(f"wereld klaar ({time.time() - t0:.0f} s), palet {len(W.pal)}")
    if hasattr(W, "fix_connections"):
        W.fix_connections()
    O = Omzet(W)
    onb = O.B.controleer(O.pal)
    if onb:
        log("ONBEKENDE BLOKKEN:", onb)
    tegels = [(tx, tz) for tx in range(PL.TX0, PL.TX1 + 1) for tz in range(PL.TZ0, PL.TZ1 + 1)]
    if alleen:
        tegels = [tuple(int(v) for v in s.split(",")) for s in alleen.split(";")]
    NX, NZ = (PL.TX1 - PL.TX0 + 1) * T, (PL.TZ1 - PL.TZ0 + 1) * T
    hgt = np.full((NX, NZ), -1, np.int16)
    nm = np.full((NX, NZ), 0, np.uint16)
    totaal, grootte = 0, 0
    import indeling
    for (tx, tz) in tegels:
        if indeling.van_andere_zone(tx, tz, "z6"):
            log(f"tegel {tx},{tz} hoort bij een andere zone: overgeslagen")
            continue
        vol = tegel(W, O, tx, tz)
        pad = os.path.join(UIT, f"t_{tx}_{tz}.nbt")
        if not vol.any():
            if os.path.exists(pad) and not alleen:
                os.remove(pad)
            continue
        heeft = vol > 0
        hy = np.where(heeft.any(1), HOOG - 1 - np.argmax(heeft[:, ::-1, :], axis=1), -1)
        hid = np.take_along_axis(vol, np.clip(hy, 0, HOOG - 1)[:, None, :], axis=1)[:, 0, :]
        i0, j0 = (tx - PL.TX0) * T, (tz - PL.TZ0) * T
        if 0 <= i0 < NX and 0 <= j0 < NZ:
            hgt[i0:i0 + T, j0:j0 + T] = hy
            nm[i0:i0 + T, j0:j0 + T] = hid
        vol = hol(vol, O.op)
        nb = schrijf(O, vol, pad)
        totaal += nb
        grootte += os.path.getsize(pad)
    log(f"{len(tegels)} tegels, {totaal} blokken, {grootte / 1e6:.1f} MB ({time.time() - t0:.0f} s)")
    if not alleen:
        kaartbeeld(hgt, nm, O)
    bewaar(W)
    info = dict(plekken=getattr(W, "plekken", {}), huizen=getattr(W, "huizen", []))
    alle = poi6.verzamel(W, info, snap_=snap)
    poi6.schrijf(alle, info, W)
    log(f"klaar: {len(alle)} plekken, niet gesnapt: {info.get('niet_gesnapt')} ({time.time() - t0:.0f} s)")
    return W, info


def bewaar(W):
    """The local world for the checkers (loopcheck6, kijk6): _cache/z6/wereld.npz (voxels + palette + window)."""
    d = os.path.join(CACHE, "z6")
    os.makedirs(d, exist_ok=True)
    np.savez_compressed(os.path.join(d, "wereld.npz"), v=W.v, pal=np.array(W.pal), b=np.array(W.b, float))


def kaartbeeld(hgt, nm, O):
    from PIL import Image
    import kaartbeeld as KB
    namen = [O.pal.lijst[i].split("[")[0] if i else "minecraft:air" for i in range(len(O.pal.lijst))]
    kl = np.array([KB.kleur_van(n) if i else (40, 60, 110) for i, n in enumerate(namen)], np.float32)
    beeld = kl[nm]
    h = hgt.astype(np.float32)
    gx, gz = np.gradient(h)
    licht = np.clip(1 + (gx * 0.5 - gz * 0.7) * 0.1, 0.6, 1.35)
    licht[hgt < 0] = 1
    out = np.clip(beeld * licht[..., None], 0, 255).astype(np.uint8).transpose(1, 0, 2)
    Image.fromarray(out).save(os.path.join(CACHE, "kaart_z6.png"))


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--alleen", default=None)
    ap.add_argument("--geen-snap", action="store_true")
    ap.add_argument("--werkers", type=int, default=1)   # (accepted for maak_kaart; zone 6 builds in one process)
    a = ap.parse_args()
    maak(a.alleen, not a.geen_snap)
