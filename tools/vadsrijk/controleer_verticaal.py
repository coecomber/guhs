"""Walkability check of the Hoedenpaden + Druipschacht (fix round "verticaal") on the composed map, survival rules.

Run from the project root AFTER `python tools/vadsrijk/maak_kaart.py` (it reuses its _cache scenes; ~3 min):
    python tools/vadsrijk/controleer_verticaal.py
It builds the world in memory (no tiles written), composes the tiles round both structures and flood-fills stand states at
half-block surfaces: steps up <= 1 block (slabs/stairs/jump), ladders + water climb, falling any height (never through a
floor). Reports every POI that cannot be reached from the tower feet / the cave entrance, and every "trap" state (reachable
but no way back to the forest floor / the cave entrance). Exit code 1 on any problem.
"""
import os
import sys
import time

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "wereld"))
sys.path.insert(0, os.path.join(HIER, "stad"))
import kaart as KA  # noqa: E402
import tegels as TG  # noqa: E402


DOOR = ("air", "short_grass", "fern", "tall_grass", "flower", "tulip", "allium", "daisy", "cornflower", "bluet", "petals",
        "carpet", "glimpaddenstoel", "schimmelpaddenstoel", "guhbloem", "guhoortjes", "litteken_plant", "light[", "ladder",
        "water", "firefly_bush", "riet", "lily_pad", "waterlelie", "sapling", "vine", "lichen", "button", "torch", "cave_vines",
        "pressure_plate", "roze_guhbloem", "minecraft:red_mushroom", "minecraft:brown_mushroom", "dead_bush", "spore_blossom",
        "hanging_roots")
GEEN_STEUN = ("fence", "_wall", "lantern", "chain", "pointed_dripstone", "cluster", "_bud", "lightning_rod", "sniffer_egg")


def klassen(pal):
    n = len(pal)
    door, vol, half, water, ladder = (np.zeros(n + 1, bool) for _ in range(5))
    for i, s in enumerate(pal):
        if i <= 1:
            door[i] = True
            continue
        nm = s.lower()
        if "mushroom_block" in nm or "mushroom_stem" in nm:
            vol[i] = True
            continue
        if any(t in nm for t in DOOR):
            door[i] = True
            water[i] = "water" in nm and "lily" not in nm and "lelie" not in nm
            ladder[i] = "ladder" in nm
            continue
        if any(t in nm for t in GEEN_STEUN):
            continue
        if ("slab" in nm and "type=bottom" in nm) or ("stairs" in nm and "half=bottom" in nm):
            half[i] = True
        else:
            vol[i] = True
    return door, vol, half, water, ladder


def staten(vol_ids, pal, y0, y1):
    """bool[X, 2*(y1-y0), Z]: index h -> surface at y0 + h/2."""
    door, vol, half, water, ladder = klassen(pal)
    v = vol_ids[:, y0 - 1:y1 + 3, :]
    D, V, Hf, Wt, Ld = door[v], vol[v], half[v], water[v], ladder[v]
    X, _, Z = vol_ids.shape
    NH = 2 * (y1 - y0)
    S = np.zeros((X, NH, Z), bool)
    for Y in range(y0, y1):
        k = Y - y0 + 1          # index of block Y in v
        even = (V[:, k - 1, :] | Ld[:, k, :] | Wt[:, k, :]) & D[:, k, :] & D[:, k + 1, :]
        S[:, 2 * (Y - y0), :] = even
        odd = Hf[:, k, :] & D[:, k + 1, :] & D[:, k + 2, :]
        S[:, 2 * (Y - y0) + 1, :] = odd
    return S


def dichtste_staat(S, x, h, z, r=2):
    """A valid state near (x, h, z) (h = 2*(y - y0))."""
    best = None
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            for dh in range(-3, 4):
                xx, hh, zz = x + dx, h + dh, z + dz
                if 0 <= xx < S.shape[0] and 0 <= hh < S.shape[1] and 0 <= zz < S.shape[2] and S[xx, hh, zz]:
                    d = abs(dx) + abs(dz) + abs(dh) * 0.5
                    if best is None or d < best[0]:
                        best = (d, (xx, hh, zz))
    return best[1] if best else None


# ---------------------------------------------------------------- v2: proper falling (no moving through floors)
def lucht(vol_ids, pal, y0, y1):
    door, vol, half, water, ladder = klassen(pal)
    v = vol_ids[:, y0 - 1:y1 + 3, :]
    D = door[v]
    X, _, Z = vol_ids.shape
    A = np.zeros((X, 2 * (y1 - y0), Z), bool)
    for Y in range(y0, y1):
        k = Y - y0 + 1
        A[:, 2 * (Y - y0), :] = D[:, k, :] & D[:, k + 1, :]
        A[:, 2 * (Y - y0) + 1, :] = D[:, k, :] & D[:, k + 1, :] & D[:, k + 2, :]
    klim = np.zeros_like(A)
    for Y in range(y0, y1):
        k = Y - y0 + 1
        klim[:, 2 * (Y - y0), :] = ladder[v][:, k, :] | water[v][:, k, :]
    return A, klim


def _sh(a, dx, dz):
    out = np.zeros_like(a)
    X, _, Z = a.shape
    out[max(dx, 0):X + min(dx, 0), :, max(dz, 0):Z + min(dz, 0)] = a[max(-dx, 0):X + min(-dx, 0), :, max(-dz, 0):Z + min(-dz, 0)]
    return out


def _hh(a, d):
    out = np.zeros_like(a)
    if d > 0:
        out[:, d:, :] = a[:, :-d, :]
    else:
        out[:, :d, :] = a[:, -d:, :]
    return out


def _val(F, A, S):
    """Fall down from air cells F until the first stand state."""
    land = np.zeros_like(S)
    carry = np.zeros(F[:, 0, :].shape, bool)
    for h in range(S.shape[1] - 1, -1, -1):
        carry = (carry | F[:, h, :]) & (A[:, h, :] | S[:, h, :])
        land[:, h, :] = carry & S[:, h, :]
        carry &= ~S[:, h, :]
    return land


DIRS = ((1, 0), (-1, 0), (0, 1), (0, -1))


def vooruit(S, A, klim, zaden, max_it=4000):
    R = np.zeros_like(S)
    for (x, h, z) in zaden:
        R[x, h, z] = True
    for it in range(max_it):
        new = R.copy()
        for dx, dz in DIRS:
            Rs = _sh(R, dx, dz)
            new |= (Rs | _hh(Rs, 1) | _hh(Rs, 2)) & S
            new |= _val(Rs & A & ~S, A, S)
        kv = R & klim
        new |= (_hh(kv, 2) | _hh(kv, -2)) & S & klim
        if (new == R).all():
            return R
        R = new
    return R


def ontsnap(S, A, klim, doel, max_it=4000):
    """States from which the target set can be reached (walking, climbing, falling any height)."""
    E = doel & S
    for it in range(max_it):
        new = E.copy()
        for dx, dz in DIRS:
            Es = _sh(E, -dx, -dz)
            new |= (Es | _hh(Es, -1) | _hh(Es, -2)) & S
            # air cells (in the target's column) whose fall lands in E
            U = np.zeros_like(S)
            prev_land = np.zeros(S[:, 0, :].shape, bool)
            prev_U = np.zeros_like(prev_land)
            for h in range(S.shape[1]):
                if h:
                    prev_land = S[:, h - 1, :] & E[:, h - 1, :]
                    prev_U = U[:, h - 1, :] & ~S[:, h - 1, :]
                U[:, h, :] = A[:, h, :] & ~S[:, h, :] & (prev_land | prev_U)
            new |= _sh(U, -dx, -dz) & S
        ke = E & klim
        new |= (_hh(ke, 2) | _hh(ke, -2)) & S & klim
        if (new == E).all():
            return E
        E = new
    return E


def laad(naam):
    npz = os.path.join(HIER, "_cache", naam + ".npz")
    d = np.load(npz)
    return TG.Stempel(naam, d["v"], [str(s) for s in d["pal"]], d["off"]), d

def wereld(alles=True):
    t = time.time()
    K = KA.Kaart(); W = TG.Wereld(K)
    kamp, kd = laad("kamp"); stad, _ = laad("stad")
    W.stempel(kamp); W.stempel(stad)
    import bouwsels
    if alles:
        P = bouwsels.bouw_alles(W, K, kd)["wereld"]
    else:
        P = {}
        bouwsels.grotten(W, K, P); bouwsels.sporenwoud(W, K, P)
        __import__("hoedenpaden").bouw(W, K, P); __import__("druipschacht").bouw(W, K, P)
    print("wereld", round(time.time() - t), "s")
    return K, W, P

def regio(W, x0, z0, x1, z1):
    """Composite block ids for the tile-aligned region covering x0..x1, z0..z1 -> (vol, ox, oz)."""
    tx0, tz0, tx1, tz1 = x0 // 64, z0 // 64, x1 // 64, z1 // 64
    vol = np.zeros(((tx1 - tx0 + 1) * 64, 320, (tz1 - tz0 + 1) * 64), np.uint16)
    for tx in range(tx0, tx1 + 1):
        for tz in range(tz0, tz1 + 1):
            vol[(tx - tx0) * 64:(tx - tx0 + 1) * 64, :, (tz - tz0) * 64:(tz - tz0 + 1) * 64] = W.tegel(tx, tz)
    return vol, tx0 * 64, tz0 * 64


def test(naam, B, zaad_keys, doel_pre, y0, y1, grond=True, toon=15):
    t = time.time()
    pts = np.array([k for k in B.b])
    x0, z0 = pts[:, 0].min() - 20, pts[:, 2].min() - 20
    x1, z1 = pts[:, 0].max() + 20, pts[:, 2].max() + 20
    vol, ox, oz = regio(W, x0, z0, x1, z1)
    S = staten(vol, W.palet.lijst, y0, y1)
    A, klim = lucht(vol, W.palet.lijst, y0, y1)
    st = lambda p: dichtste_staat(S, p[0] - ox, 2 * (p[1] - y0), p[2] - oz)
    zaden = [st(P[k]) for k in zaad_keys]
    R = vooruit(S, A, klim, [z for z in zaden if z])
    if grond:   # target = standing on the terrain (forest floor)
        X, NH, Z = S.shape
        i0, j0 = x0 // 64 * 64 - KA.X0, z0 // 64 * 64 - KA.Z0
        H = K.H[i0:i0 + X, j0:j0 + Z].astype(int)
        hh = np.arange(NH)[None, :, None]
        doel = np.abs(hh - 2 * (H[:, None, :] + 1 - y0)) <= 1
    else:
        doel = np.zeros_like(S)
        for z in zaden:
            doel[z] = True
    E = ontsnap(S, A, klim, doel | False)
    fout = 0
    for k, p in sorted(P.items()):
        if not any(k.startswith(d) for d in doel_pre) or not isinstance(p, list):
            continue
        s = st(p)
        ok = s is not None and R[s]
        fout += not ok
        print(f"  {k:32s} {p}  {'OK' if ok else 'NIET BEREIKBAAR'}")
    val = R & ~E
    print(f"  {naam}: bereikbaar {int(R.sum())} staten; vallen (bereikbaar maar geen weg terug): {int(val.sum())}  ({time.time()-t:.0f} s)")
    for (x, h, z) in np.argwhere(val)[:toon]:
        print("    ", x + ox, y0 + h / 2, z + oz)
    return fout + int(val.sum() > 0)


def main():
    global K, W, P
    K, W, P = wereld(alles=True)
    import hoedenpaden as HP
    import druipschacht as DS
    f1 = test("HOEDENPADEN", HP.LAATSTE, [k for k in P if k.startswith("hoedenpad_trap_")], ("hoedenpad", "geheim_hoed"), 60, 140)
    y0 = P["druipschacht_meer"][1] - 4
    f2 = test("DRUIPSCHACHT", DS.LAATSTE, ["druipschacht_ingang"], ("druipschacht_ingang", "druipschacht_bodem", "druipschacht_balkon",
              "druipschacht_richel", "druipschacht_waterval"), y0, y0 + 100, grond=False)
    print("FOUTEN", f1, f2)
    sys.exit(1 if (f1 or f2) else 0)


if __name__ == "__main__":
    main()
