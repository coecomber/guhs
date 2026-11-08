"""Zone 3 POI check: is every published stand spot (POI_Z3 / Z3WereldPlekken) really standable in the generated tiles?
Feet + head passable, something solid-ish under the feet. Run from the project root after maak_kaart.py --zone 3:
    python tools/vadsrijk/zone3/poicheck3.py
Spots that are blocks on purpose (cages, the egg, the mast, statue, wall marks, landmark directions) are skipped.
"""
import json
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "..", "audit"))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
import tegellezer as TL  # noqa: E402

SLA_EXACT = {"ei", "knuffelbeeld", "dok_luchtschip", "oosttaksteiger_luchtschip", "bloesemboompje", "flash_f1_kwastgat", "vingerafdruk"}   # (W-SCHEPEN: the *_mast spots are stand spots now)
SLA_PREFIX = ("landmark_", "piepvel_lantaarnpaal", "piepvel_kooi", "grotlantaarn", "hart_grotlantaarn")
DOOR = ("air", "carpet", "petals", "lantern", "chain", "ladder", "short_grass", "fern", "flower", "torch", "campfire", "water",
        "lily_pad", "moss_carpet", "glimpaddenstoel", "glimplant", "cave_vines", "hanging_moss", "vine", "button", "lever", "slab[type=bottom",
        "pressure_plate", "rail", "sapling", "tulip", "poppy", "dandelion")


def door(s):
    # (r-int) match the block NAME (+ the bottom-slab state): "water" used to match every "waterlogged=false" state, so leaves,
    # fences and full slabs counted as passable (kuiken_6 sat inside the crown-1 leaves and was never snapped)
    if s is None:
        return True
    naam = s.split("[")[0]
    return any(w in naam for w in DOOR) or s.startswith(("minecraft:", "guhs:")) and "_slab[" in s and "type=bottom" in s


class Lezer:
    def __init__(self):
        self.cache = {}

    def __call__(self, x, y, z):
        tx, tz = x // 64, z // 64
        if (tx, tz) not in self.cache:
            p = os.path.join(TEGELS, f"t_{tx}_{tz}.nbt")
            d = {}
            if os.path.exists(p):
                pal, (xs, ys, zs, st) = TL.lees(p)
                for a, b, c, s in zip(xs.tolist(), ys.tolist(), zs.tolist(), st.tolist()):
                    d[(a, b, c)] = pal[s]
            self.cache[(tx, tz)] = d
        return self.cache[(tx, tz)].get((x - tx * 64, y, z - tz * 64))


def overslaan(k):
    return (k in SLA_EXACT or k.startswith(SLA_PREFIX) or k.endswith(("doos_min", "doos_max", "_kooi", "premiebord", "jachtbord"))
            or k.startswith("kampvuur_") or k in ("piepvel_troon", "hart_vahoeg_altaar", "werf_vahoeg_altaar", "ei"))


def staat(blok, x, y, z):
    voet, hoofd, onder = blok(x, y, z), blok(x, y + 1, z), blok(x, y - 1, z)
    fout = []
    if not door(voet):
        fout.append(f"voet {voet}")
    if not door(hoofd):
        fout.append(f"hoofd {hoofd}")
    if onder is None or ("water" in onder and "waterlogged" not in onder):
        fout.append("geen vloer" if onder is None else "water")
    elif hek(onder):
        fout.append(f"op een hek {onder}")   # (r-int) not a floor: you cannot stand on a fence/wall top
    return fout


def hek(s):
    naam = (s or "").split("[")[0]
    return naam.endswith(("_fence", "_wall", "_pane", "iron_bars")) or "lantaarnpaal" in naam



def snap(alle, blok=None):
    """For every stand spot that is not standable in the tiles: the nearest standable spot (same column down first, then up,
    then rings out to 5). -> {id: [x, y, z]} (only the moved ones)."""
    blok = blok or Lezer()
    uit = {}
    for k, (x, y, z) in sorted(alle.items()):
        if overslaan(k) or not staat(blok, x, y, z):
            continue
        beste = None
        # (r-int) a spot buried in blocks (feet inside leaves/ground) looks UP first: going down found hollows inside the crown
        v = (blok(x, y, z) or "").split("[")[0]
        omhoog = v.endswith(("_leaves", "moss_block", "grass_block", "dirt", "knuffelgras", "podzol", "terracotta", "concrete_powder"))   # terrain only (not a bell/bed/wall)
        dys = (0, 1, 2, 3, 4, 5, 6, -1, -2, -3, -4) if omhoog else (0, -1, -2, -3, -4, -5, -6, -7, -8, 1, 2, 3, 4)
        for r in range(0, 6):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if max(abs(dx), abs(dz)) != r:
                        continue
                    for dy in dys:
                        if not staat(blok, x + dx, y + dy, z + dz):
                            c = (abs(dx) + abs(dz)) * 2 + abs(dy)
                            if beste is None or c < beste[0]:
                                beste = (c, [x + dx, y + dy, z + dz])
                            break
            if beste is not None and beste[0] <= 2 * r + 2:
                break
        if beste is not None:
            uit[k] = beste[1]
    return uit


def main():
    alle = json.load(open(os.path.join(HIER, "..", "_cache", "plekken_z3.json"), encoding="utf-8"))
    blok = Lezer()
    slecht = []
    for k, (x, y, z) in sorted(alle.items()):
        if overslaan(k):
            continue
        fout = staat(blok, x, y, z)
        if fout:
            slecht.append((k, (x, y, z), fout))
    for k, p, f in slecht:
        print(f"{k:40s} {p}  {'; '.join(f)}")
    print(f"poicheck3: {len(slecht)} van {len(alle)} plekken niet sta-baar")


if __name__ == "__main__":
    main()
