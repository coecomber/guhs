"""Builds the zone-3 map "De Schemerwortel" as 64x320x64 structure tiles (same tile mechanism as zone 1/2, CONTRACT §10).

Called from tools/vadsrijk/maak_kaart.py (``--zone 3`` = only zone 3; ``alle`` = 1, 2 and 3) in a SEPARATE process (the render
kit in zone3/kit shares module names with zone 1's kits). Standalone from the project root:
    python tools/vadsrijk/zone3/maak3.py [--alleen "tx,tz;..."] [--werkers N]
Writes src/main/resources/data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt for tx 29..42, tz -7..6 (the revamp grid) and
DELETES the v1 zone-3 tiles outside it (tx 25..46, tz -11..10; never zone 1/2), tools/vadsrijk/_cache/plekken_z3.json +
kaart_z3.png, Z3WereldPlekken.java and POI_Z3.md.

How: every tile is a window (k=1) of the revamp kit (kit/: boom2, plekken2, werf2 = the approved round-20 render generators,
adapted, see the [zone 3 kaart] marks) plus bouw3 (the game parts). The kit works relative to the trunk axis, NOT mirrored:
world x = 2300 + rel x.
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

import plan3 as PL  # noqa: E402
sys.path.append(os.path.join(HIER, ".."))
import indeling as IND  # noqa: E402  (T-WERELD; barriere.py lives there too)

T, HOOG = 64, 320


# ------------------------------------------------------------------------------------------------ mirroring block states
def spiegel(s):
    """Mirror a block state in x (east <-> west)."""
    if "[" not in s:
        return s
    naam, _, rest = s.partition("[")
    props = {}
    for kv in rest.rstrip("]").split(","):
        if "=" in kv:
            k, v = kv.split("=", 1)
            props[k] = v
    ew = {"east": "west", "west": "east"}
    if props.get("facing") in ew:
        props["facing"] = ew[props["facing"]]
    if "east" in props and "west" in props:
        props["east"], props["west"] = props["west"], props["east"]
    if "shape" in props and props["shape"] in ("inner_left", "inner_right", "outer_left", "outer_right"):
        sh = props["shape"]
        props["shape"] = sh.replace("left", "X").replace("right", "left").replace("X", "right")
    if "shape" in props and props["shape"].startswith("ascending_") and props["shape"][10:] in ew:
        props["shape"] = "ascending_" + ew[props["shape"][10:]]
    if props.get("hinge") in ("left", "right"):
        props["hinge"] = "right" if props["hinge"] == "left" else "left"
    if "rotation" in props and props["rotation"].isdigit():
        props["rotation"] = str((16 - int(props["rotation"])) % 16)
    return naam + "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]"


def nalit(s):
    """Campfires come only from bouw3 (unlit = kern checkpoints); the kit's decorative ones are dropped."""
    return s


# ------------------------------------------------------------------------------------------------ one tile
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
        import vibekit as V
        r, bk = V.res()
        lut = np.zeros(n + 64, bool)
        lut[:len(self.lut_op)] = self.lut_op[:len(self.lut_op)]
        for i in range(len(self.lut_op), n):
            s = self.palet.lijst[i]
            if i < 2:
                continue
            naam = s.split("[")[0]
            if naam in self.B.EIGEN or naam.endswith(("_leaves", "barrier")) or "glass" in naam or "slime" in naam or "honey" in naam:
                continue
            try:
                lut[i] = bool(bk.bake(s).full_opaque)
            except Exception:
                lut[i] = False
        self.lut_op = lut
        return lut

    def tegel(self, tx, tz):
        import schemerkit as SK
        import boom2 as B
        import plekken2 as P
        import werf2 as WF
        import bouw3
        x1, z1 = PL.rel_x(tx * T), PL.rel_z(tz * T)
        W = SK.Wereld(x1, 0, z1, x1 + T, HOOG, z1 + T, k=1)
        B.bouw(W, ("terrein", "wortels_boven", "wortelduister", "hangwortels", "takken", "kruin"))
        P.honingtak(W)
        B.bouw(W, ("stam", "schors_leven", "nest", "wolken", "wortelpoort"))
        P.bouw(W)
        WF.bouw(W)
        B.bosjes(W)
        B.rijpad(W)
        B.wenteltrap(W)          # again: through the nest floor and anything built over it
        bouw3.bouw(W)
        pal = []
        for s in W.pal:
            if s.startswith("minecraft:campfire") and "z3kv" not in s and "lit=false" not in s:
                s = "minecraft:air"
            s = s.replace(",z3kv=1", "").replace("z3kv=1,", "")
            pal.append(s)
        lut = np.array([0 if s in ("minecraft:air", "minecraft:cave_air") else self.palet.id(s) for s in pal], np.uint16)
        vol = lut[W.v].copy()
        import barriere          # T-WERELD: the sea + cloud sea round the island (Wolkenkloof west, Wolkenval east)
        barriere.zee_z3(vol, tx, tz, self.palet.id)
        sys.path.insert(0, os.path.join(HIER, "..", "grotten"))
        import grot              # U-GROTTEN: the zone-3 cave (Wortelgrot) under the Wortelvelden
        grot.pas_toe(vol, tx, tz, self.palet.id, self.palet.lijst, 3)
        return vol

    def hol(self, vol):
        """Hollow: an opaque block with 6 opaque neighbours is never seen (removed); a LEAF block deeper than 2 blocks inside a
        leaf/opaque mass is removed too (the crown is a thick leaf shell: keeps a 2-block skin, saves millions of blocks)."""
        from scipy import ndimage
        lut = self.opaak()
        blad = self.blad_lut()
        occ = lut[vol]
        dicht = occ | blad[vol]
        inn = occ.copy()
        inn[0, :, :] = inn[-1, :, :] = False
        inn[:, 0, :] = inn[:, -1, :] = False
        inn[:, :, 0] = inn[:, :, -1] = False
        inn[1:-1, 1:-1, 1:-1] &= occ[:-2, 1:-1, 1:-1] & occ[2:, 1:-1, 1:-1] & occ[1:-1, :-2, 1:-1] &             occ[1:-1, 2:, 1:-1] & occ[1:-1, 1:-1, :-2] & occ[1:-1, 1:-1, 2:]
        diep = ndimage.binary_erosion(dicht, iterations=2, border_value=0) & blad[vol]
        vol[inn | diep] = 0
        return vol

    def blad_lut(self):
        n = len(self.palet.lijst)
        return np.array([s.split("[")[0].endswith("_leaves") for s in self.palet.lijst] + [False] * 64, bool)[:max(n, 1) + 64]

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
    # top view: highest block per column (before hollowing)
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


def maak(alleen=None, werkers=6):
    import poi3
    t0 = time.time()
    os.makedirs(CACHE, exist_ok=True)
    os.makedirs(UIT, exist_ok=True)
    poi3.schrijf(PL.plekken(), "v2 (map)")
    tegels = [(tx, tz) for tx in range(PL.TX0, PL.TX1 + 1) for tz in range(PL.TZ0, PL.TZ1 + 1)]
    if not alleen:
        # the v1 zone-3 tiles outside the revamp grid go (the KaartFeature places nothing where no tile exists)
        weg = 0
        oud = {(tx, tz) for (a0, a1, b0, b1) in PL.OUDE_RASTERS for tx in range(a0, a1 + 1) for tz in range(b0, b1 + 1)}
        for (tx, tz) in sorted(oud):
            if PL.TX0 <= tx <= PL.TX1 and PL.TZ0 <= tz <= PL.TZ1:
                continue
            if IND.van_andere_zone(tx, tz, "z3"):
                continue   # T-WERELD: zone 4 / zone 5 tiles live in the old zone-3 raster now: never delete them
            if True:
                p = os.path.join(UIT, f"t_{tx}_{tz}.nbt")
                if os.path.exists(p):
                    os.remove(p)
                    weg += 1
        print(f"zone 3: {weg} oude tegels buiten het nieuwe raster verwijderd")
    if alleen:
        tegels = [tuple(int(v) for v in s.split(",")) for s in alleen.split(";")]
        tegels = [t for t in tegels if PL.TX0 <= t[0] <= PL.TX1 and PL.TZ0 <= t[1] <= PL.TZ1]
    # heavy tiles first (trunk / town), so the pool ends evenly
    tegels.sort(key=lambda t: abs(t[0] * 64 + 32 - PL.TX_STAM) + abs(t[1] * 64 + 32 - PL.TZ_STAM))
    NX, NZ = (PL.TX1 - PL.TX0 + 1) * T, (PL.TZ1 - PL.TZ0 + 1) * T
    hgt = np.full((NX, NZ), -1, np.int16)
    nm = np.full((NX, NZ), "minecraft:air", dtype=object)
    totaal, grootte, onb = 0, 0, set()
    if werkers > 1 and len(tegels) > 1:
        import multiprocessing as mp
        with mp.get_context("spawn").Pool(werkers) as pool:
            res = pool.imap_unordered(doe_tegel, tegels)
            for n, r in enumerate(res):
                totaal, grootte = verwerk(r, n, len(tegels), hgt, nm, totaal, grootte, onb, t0)
    else:
        for n, t in enumerate(tegels):
            totaal, grootte = verwerk(doe_tegel(t), n, len(tegels), hgt, nm, totaal, grootte, onb, t0)
    print(f"zone 3: {len(tegels)} tegels, {totaal} blokken, {grootte / 1e6:.1f} MB ({time.time() - t0:.0f} s)")
    if onb:
        print("zone 3 ONBEKENDE BLOKKEN:", sorted(onb))
    if not alleen:
        kaartbeeld(hgt, nm)
    # v1: every published stand spot snapped onto the real blocks (a formula spot that landed in a fence/pillar/air moves to the
    # nearest standable block; the builder itself keeps using the plan spots)
    import poicheck3
    vervang = poicheck3.snap(poi3_alle())
    poi3.schrijf(PL.plekken(), "v2 (map, stand spots snapped onto the tiles)", vervang)
    print(f"zone 3: {len(vervang)} plekken op de echte blokken gezet")


def poi3_alle():
    import poi3
    alle = {}
    poi3.plat("", PL.plekken(), alle)
    return alle


def verwerk(r, n, N, hgt, nm, totaal, grootte, onb, t0):
    tx, tz, nb, sz, hy, namen, onbekend, dt = r
    i0, j0 = (tx - PL.TX0) * T, (tz - PL.TZ0) * T
    hgt[i0:i0 + T, j0:j0 + T] = hy
    nm[i0:i0 + T, j0:j0 + T] = namen
    onb |= set(onbekend)
    if n % 20 == 0:
        print(f"  z3 tegel {n}/{N} ({tx},{tz}): {nb} blokken, {dt:.1f} s ({time.time() - t0:.0f} s)", flush=True)
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
    Image.fromarray(out).save(os.path.join(CACHE, "kaart_z3.png"))
    Image.fromarray(out).resize((out.shape[1] // 2, out.shape[0] // 2)).save(os.path.join(CACHE, "kaart_z3_klein.png"))


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
