"""W-BEREIK: lanterns, chains and paper lanterns OUT of the walking lines, on every Vadsrijk map tile (all zones).

W_BRIEF walkability standard: "No lanterns, chains or decoration ON stairs or paths". Lanterns and chains have collision, so a
lantern on a stair step or a chain hanging into a corridor is a wall for a survival player (round W: the cathedral stairs of zone 5).
The generators place a lot of these; this shared pass (like verbind.py) fixes them all after the tiles are written:
  - HANGING into the walking space: a lantern/chain with only 1 block of air under it above a floor where a neighbour is walkable
    -> the lowest piece goes; a chain that becomes the lowest piece turns into a hanging lantern (so it still looks finished);
  - STANDING on a stair step (any stair with a walkable neighbour step) or on a slab floor with 2+ walkable neighbours -> removed;
  - a loose CHAIN standing on a floor with 2+ walkable neighbours -> removed;
  - a lantern standing on a floor in the open (all 4 neighbours walkable) -> removed.
  - FLOATING fences/walls/bars/panes (air under and over them) hovering 1..5 blocks over a walkable floor -> removed (a
    balustrade left in the air over a stair: you see a wall but walk under it).
Lantern posts (guh_lantaarnpaal), lanterns on walls/fences/posts/tables and lanterns hanging higher than head height stay.

    python tools/vadsrijk/vrijpad.py                 # all tiles
    python tools/vadsrijk/vrijpad.py --alleen 40,-8  # some tiles
maak_kaart.py runs it after the connection pass on every tile written in that run (vrijpad.na_sinds). Idempotent.
"""
import argparse
import os
import sys
import time

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
import verbind as VB  # noqa: E402
import bereikcheck as BC  # noqa: E402

T, HOOG = VB.T, VB.HOOG
HANG_LANTAARN = "minecraft:lantern[hanging=true,waterlogged=false]"


def is_deco(s):
    n = s.split("[")[0].split(":")[-1]
    return n in ("lantern", "soul_lantern", "iron_chain", "chain", "messing_lantaarn") or n.startswith("lampion_")


def is_ketting(s):
    n = s.split("[")[0].split(":")[-1]
    return n in ("iron_chain", "chain")


def pas_tegel(TG, t, cls_lut, deco_lut):
    v = TG.dicht(t)
    if v is None:
        return 0
    if not deco_lut[v].any():
        return 0
    # padded class volume (x+1, y, z+1) with the 4 neighbour tiles' border columns
    C = np.zeros((T + 2, HOOG, T + 2), np.uint8)
    C[1:-1, :, 1:-1] = cls_lut[v]
    for (dx, dz) in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        n = TG.dicht((t[0] + dx, t[1] + dz))
        if n is None:
            continue
        cn = cls_lut[n]
        if dx == -1:
            C[0, :, 1:-1] = cn[T - 1]
        elif dx == 1:
            C[-1, :, 1:-1] = cn[0]
        elif dz == -1:
            C[1:-1, :, 0] = cn[:, :, T - 1]
        else:
            C[1:-1, :, -1] = cn[:, :, 0]
    PAS = np.isin(C, BC.PASSABLE)
    VLOER = np.isin(C, (BC.SOLID, BC.SLAB, BC.STAIR))
    # stand[x, y, z]: feet at y (floor y-1, air y and y+1)
    stand = np.zeros_like(PAS)
    stand[:, 1:-1, :] = VLOER[:, :-2, :] & PAS[:, 1:-1, :] & PAS[:, 2:, :]
    xs, ys, zs = np.nonzero(deco_lut[v])
    weg, lantaarn = [], []
    for x, y, z in zip(xs.tolist(), ys.tolist(), zs.tolist()):
        if y < 2 or y >= HOOG - 2:
            continue
        if deco_lut[v[x, y - 1, z]]:
            continue                                     # judge the lowest piece of a stack only
        X, Z = x + 1, z + 1
        onder, onder2 = C[X, y - 1, Z], C[X, y - 2, Z]
        s = TG.pal[v[x, y, z]]

        def buren(yy):
            return int(stand[X + 1, yy, Z]) + int(stand[X - 1, yy, Z]) + int(stand[X, yy, Z + 1]) + int(stand[X, yy, Z - 1])
        if PAS[X, y - 1, Z] and VLOER[X, y - 2, Z]:
            # hanging with 1 air under it: blocks the walking space when a neighbour is walked at that floor
            if buren(y - 1) >= 1:
                weg.append((x, y, z))
                if y + 1 < HOOG and is_ketting(TG.pal[v[x, y + 1, z]]):
                    lantaarn.append((x, y + 1, z))
            continue
        if onder == BC.STAIR and (buren(y) + buren(y - 1) + buren(y + 1)) >= 1:
            weg.append((x, y, z))
        elif onder == BC.SLAB and buren(y) >= 2:
            weg.append((x, y, z))
        elif onder == BC.SOLID and is_ketting(s) and buren(y) >= 2:
            weg.append((x, y, z))
        elif onder == BC.SOLID and buren(y) >= 4:
            weg.append((x, y, z))
    # floating fences / walls / bars / panes (air under AND over them) hovering over a walkable floor 1..5 below: they read as a
    # balustrade "above the stairs" that you walk under/through (round W: the cathedral's west stair, zone 5)
    fx, fy, fz = np.nonzero(C[1:-1, :, 1:-1] == BC.FENCE)
    for x, y, z in zip(fx.tolist(), fy.tolist(), fz.tolist()):
        if y < 6 or y >= HOOG - 1:
            continue
        X, Z = x + 1, z + 1
        if not (PAS[X, y - 1, Z] and PAS[X, y + 1, Z]):
            continue
        if "gate" in TG.pal[v[x, y, z]]:
            continue
        # W-INT: a corner piece of a RAILING (the connection pass fills diagonal steps over the void) hangs on a supported
        # neighbour piece: keep it, or the two passes fight (verbindcheck los/gat) and the railing gets holes
        if any(C[X + ex, y + ey, Z + ez] == BC.FENCE and not PAS[X + ex, y + ey - 1, Z + ez]
               for ex in (-1, 0, 1) for ez in (-1, 0, 1) for ey in (-1, 0, 1)
               if (ex or ez) and 0 <= X + ex < C.shape[0] and 0 <= Z + ez < C.shape[2] and 1 <= y + ey < HOOG):
            continue
        if any(stand[X, yy, Z] for yy in range(y - 5, y - 1)):
            weg.append((x, y, z))
    if not weg:
        return 0
    lucht = TG.id("minecraft:air")
    hl = TG.id(HANG_LANTAARN)
    for (x, y, z) in weg:
        v[x, y, z] = lucht
    for (x, y, z) in lantaarn:
        v[x, y, z] = hl
    TG.schrijf(t, v, extra=TG.nbt(t))
    TG.cache.pop(t, None)
    return len(weg)


def naloop(tegels=None, map_=VB.UIT, praat=True):
    t0 = time.time()
    TG = VB._Tegels(map_)
    alle = [(tx, tz) for tx, tz, _ in TG.TL.tegels(map_)]
    lijst = alle if tegels is None else [t for t in tegels if t in set(alle)]
    n = 0
    tegels_n = 0
    cls_l, deco_l = [], []
    for t in lijst:
        for nb in (t, (t[0] - 1, t[1]), (t[0] + 1, t[1]), (t[0], t[1] - 1), (t[0], t[1] + 1)):
            TG.dicht(nb)
        for s in TG.pal[len(cls_l):]:
            cls_l.append(BC.klasse(s) if s.split("[")[0] not in VB.LUCHT else BC.AIR)
            deco_l.append(is_deco(s))
        k = pas_tegel(TG, t, np.array(cls_l, np.uint8), np.array(deco_l, bool))
        if k:
            tegels_n += 1
            n += k
    if praat:
        print(f"vrijpad: {len(lijst)} tegels bekeken, {tegels_n} herschreven, {n} lantaarns/kettingen uit de looplijnen "
              f"({time.time() - t0:.0f} s)")
    return n


def na_sinds(t0, map_=VB.UIT):
    sys.path.insert(0, os.path.join(HIER, "audit"))
    import tegellezer as TL
    tegels = [(tx, tz) for tx, tz, p in TL.tegels(map_) if os.path.getmtime(p) >= t0 - 1]
    if tegels:
        naloop(tegels, map_)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--alleen", default=None, help="tx,tz;tx,tz;...")
    a = ap.parse_args()
    tegels = None
    if a.alleen:
        tegels = [tuple(int(v) for v in s.split(",")) for s in a.alleen.split(";") if s.strip()]
    naloop(tegels)


if __name__ == "__main__":
    main()
