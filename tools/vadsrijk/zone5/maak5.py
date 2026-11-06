"""Builds the zone-5 map "Hoogvads" (the capital on Moeder Wolkwal) as 64x320x64 structure tiles (CONTRACT §10, like zones 1-4).

Called from tools/vadsrijk/maak_kaart.py (``--zone 5`` = only zone 5; ``alle`` = everything) in a SEPARATE process (the render kit
in zone5/kit shares module names with the other zones' kits). Standalone from the project root:
    python tools/vadsrijk/zone5/maak5.py [--alleen "tx,tz;..."] [--geen-snap]
Writes src/main/resources/data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt for tx 38..48, tz -12..-5,
tools/vadsrijk/_cache/plekken_z5.json + kaart_z5.png, Z5WereldPlekken.java and POI_Z5.md (generated: do NOT commit them).

How: the kit (kit/ = the approved round-2 render generators of renders-hoogvads/_bron, [zone 5 kaart] marks) builds the whole
whale + city in ONE local window (~15 s), bouw5 adds the game parts (interiors, doors, railings, piers, rooms, ...), then every
tile is a slice of it turned 180 degrees and lowered DY (plan5.spel). The tiles tx 38..39 are SHARED with zone 4 (its sea and
the Opstijgpier below): the blocks of the existing tile below plan5.NAAD_Y are kept (zone 4 wins below, zone 5 above), so the
order of `maak_kaart --zone 4` / `--zone 5` does not matter as long as zone 5 runs last or zone 4 keeps y >= NAAD_Y empty.
"""
import argparse
import json
import os
import sys
import time

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
sys.path.insert(1, os.path.join(HIER, "kit"))
sys.path.append(os.path.join(HIER, "..", "wereld"))
sys.path.append(os.path.join(HIER, "..", "audit"))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
CACHE = os.path.join(HIER, "..", "_cache")
UIT = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")

import plan5 as PL  # noqa: E402

T, HOOG = PL.T, PL.HOOG


def log(*a):
    print(time.strftime("%H:%M:%S"), "z5", *a, flush=True)


# ------------------------------------------------------------------------------------------------ the local world
def bouw_wereld():
    import hoogkit  # noqa: F401  (jars + the repo's own Hoogvads blocks)
    import palet
    palet.kies("nieuw")
    import schemerkit as SK
    import wereld as WR
    import stad
    import bouw5
    W = SK.Wereld(*WR.BOX, 1.0)
    bouw5.vooraf(W)
    S = stad.bouw(W, figuren=True, log=lambda *a: log(*a))
    info = bouw5.bouw(W, S, log=log)
    return W, S, info


# ------------------------------------------------------------------------------------------------ palette + opacity
class Omzet:
    """Kit palette -> global tile palette (turned 180 degrees, 26.1 names) + the opaque lookup for hollowing."""

    def __init__(self, W):
        import blokken as B
        import export as EX
        import vibekit as V
        self.B = B
        self.pal = B.Palet()
        r, bk = V.res()
        lut = np.zeros(len(W.pal), np.uint16)
        for i, s in enumerate(W.pal):
            naam = s.split("[")[0]
            if naam in ("minecraft:air", "minecraft:cave_air"):
                continue
            lut[i] = self.pal.id(EX.draai180(s))
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
    """uint16 (64, 320, 64) [x, y, z] of tile (tx, tz) in GAME coordinates (global palette ids)."""
    vol = np.zeros((T, HOOG, T), np.uint16)
    x1, y1, z1 = (int(v) for v in W.b[:3])
    X, Y, Z = W.v.shape
    gx = tx * T + np.arange(T)
    gz = tz * T + np.arange(T)
    lx = (PL.GX0 - gx) - x1
    lz = (PL.GZ0 - gz) - z1
    okx = (lx >= 0) & (lx < X)
    okz = (lz >= 0) & (lz < Z)
    if not okx.any() or not okz.any():
        return vol
    gy = np.arange(HOOG)
    ly = (gy - PL.DY) - y1
    oky = (ly >= 0) & (ly < Y) & (gy >= PL.NAAD_Y)
    sub = W.v[np.ix_(lx[okx], ly[oky], lz[okz])]
    vol[np.ix_(np.nonzero(okx)[0], gy[oky], np.nonzero(okz)[0])] = O.lut[sub]
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


def zone4_onder(O, pad):
    """The blocks below NAAD_Y of an existing tile (zone 4 at tx 38..39): (xs, ys, zs, global ids)."""
    if not os.path.exists(pad):
        return None
    import tegellezer as TL
    palette, blokken = TL.lees(pad)
    xs, ys, zs, st = blokken
    m = ys < PL.NAAD_Y
    if not m.any():
        return None
    ids = np.array([O.id(s) for s in palette], np.uint16)
    return xs[m], ys[m], zs[m], ids[st[m]]


def schrijf(O, vol, pad, extra=None):
    import nbtsnel
    if extra is not None:
        xs, ys, zs, ids = extra
        vol[xs, ys, zs] = ids
    xs, ys, zs = np.nonzero(vol)
    ids = vol[xs, ys, zs]
    gebruikt, inv = np.unique(ids, return_inverse=True)
    palette = [O.B.splits(O.pal.lijst[i]) for i in gebruikt]
    return nbtsnel.schrijf(pad, (T, HOOG, T), palette, xs.astype(np.int32), ys.astype(np.int32), zs.astype(np.int32),
                           inv.astype(np.int32), None)


# ------------------------------------------------------------------------------------------------ main
def maak(alleen=None, snap=True):
    import poi5
    t0 = time.time()
    os.makedirs(CACHE, exist_ok=True)
    os.makedirs(UIT, exist_ok=True)
    W, S, info = bouw_wereld()
    log(f"wereld + spel-delen klaar ({time.time() - t0:.0f} s), palet {len(W.pal)}")
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
    totaal, grootte, n_naad = 0, 0, 0
    sys.path.insert(0, os.path.join(HIER, "..", "grotten"))
    import grot as GROT          # U-GROTTEN: the zone-5 cave (Wolkengrotten) in the whale's head
    for (tx, tz) in tegels:
        vol = tegel(W, O, tx, tz)
        GROT.pas_toe(vol, tx, tz, O.id, O.pal.lijst, 5)
        pad = os.path.join(UIT, f"t_{tx}_{tz}.nbt")
        onder = zone4_onder(O, pad)
        if not vol.any() and onder is None:
            if os.path.exists(pad) and not alleen:
                os.remove(pad)    # an old zone-5 tile that is empty now
            continue
        heeft = vol > 0
        hy = np.where(heeft.any(1), HOOG - 1 - np.argmax(heeft[:, ::-1, :], axis=1), -1)
        hid = np.take_along_axis(vol, np.clip(hy, 0, HOOG - 1)[:, None, :], axis=1)[:, 0, :]
        i0, j0 = (tx - PL.TX0) * T, (tz - PL.TZ0) * T
        if 0 <= i0 < NX and 0 <= j0 < NZ:
            hgt[i0:i0 + T, j0:j0 + T] = hy
            nm[i0:i0 + T, j0:j0 + T] = hid
        vol = hol(vol, O.op)
        if onder is not None:
            n_naad += 1
        nb = schrijf(O, vol, pad, onder)
        totaal += nb
        grootte += os.path.getsize(pad)
        log(f"tegel {tx},{tz}: {nb} blokken{' (+ zone 4 onder y %d)' % PL.NAAD_Y if onder is not None else ''}")
    log(f"{len(tegels)} tegels, {totaal} blokken, {grootte / 1e6:.1f} MB, {n_naad} gedeeld met zone 4 ({time.time() - t0:.0f} s)")
    if not alleen:
        kaartbeeld(hgt, nm, O)
    bewaar(W)
    alle = poi5.verzamel(W, S, info, snap_=snap)
    poi5.schrijf(alle, info, W)
    log(f"klaar: {len(alle)} plekken ({time.time() - t0:.0f} s)")
    return W, S, info


def bewaar(W):
    """The local world for the debug/check tools (kijk5.py): _cache/z5/wereld.npz (voxels + palette + window)."""
    d = os.path.join(CACHE, "z5")
    os.makedirs(d, exist_ok=True)
    np.savez_compressed(os.path.join(d, "wereld.npz"), v=W.v, pal=np.array(W.pal), b=np.array(W.b, float))


def kaartbeeld(hgt, nm, O):
    from PIL import Image
    sys.path.insert(0, os.path.join(HIER, "..", "wereld"))
    import kaartbeeld as KB
    namen = [O.pal.lijst[i].split("[")[0] if i else "minecraft:air" for i in range(len(O.pal.lijst))]
    kl = np.array([KB.kleur_van(n) if i else (40, 60, 110) for i, n in enumerate(namen)], np.float32)
    beeld = kl[nm]
    h = hgt.astype(np.float32)
    gx, gz = np.gradient(h)
    licht = np.clip(1 + (gx * 0.5 - gz * 0.7) * 0.1, 0.6, 1.35)
    licht[hgt < 0] = 1
    out = np.clip(beeld * licht[..., None], 0, 255).astype(np.uint8).transpose(1, 0, 2)
    Image.fromarray(out).save(os.path.join(CACHE, "kaart_z5.png"))


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--alleen", default=None)
    ap.add_argument("--geen-snap", action="store_true")
    ap.add_argument("--werkers", type=int, default=1)   # (accepted for maak_kaart; zone 5 builds in one process)
    a = ap.parse_args()
    _t_verbind = time.time()   # U-HEKKEN
    maak(a.alleen, not a.geen_snap)
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))   # U-HEKKEN: connection pass
    import verbind
    verbind.na_sinds(_t_verbind)
