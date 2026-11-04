"""h-melee: is there room to fight in MELEE in every boss arena? (playtest round 3: "no good spots to stand with melee")

Run from the project root after `python tools/vadsrijk/maak_kaart.py --zone alle`:
    python tools/vadsrijk/audit/arenaruimte.py
For every arena disc (centre + radius as in the Java code) every column is classed at the fight height (centre y +-4):
  droog   = a floor with 2 free blocks above it (air; flowers/grass/reeds count separately as "plant")
  water   = the top in that band is water (you swim: no melee footing)
  anders  = a wall, a pit or a step more than 4 from the centre height
It prints the share per class and the share of dry columns at most 1 block from the centre height ("vlak"), plus a small
ASCII map (. dry, ~ water, " plant, # other). The test: dry >= 60 % of the disc and flat-dry >= 50 %.
"""
import math
import os
import re
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
import blokklassen as BK  # noqa: E402
import tegellezer as TL  # noqa: E402

ROOT = os.environ.get("VR_ROOT") or os.path.abspath(os.path.join(HIER, "..", "..", ".."))   # (VR_ROOT: measure another checkout)
TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
T = 64


def poi(bestand, naam):
    tekst = open(os.path.join(ROOT, bestand), encoding="utf-8").read()
    m = re.search(r"\| `" + re.escape(naam) + r"` \| (-?\d+), (-?\d+), (-?\d+) \|", tekst)
    return tuple(int(v) for v in m.groups()) if m else None


# (name, POI file, POI id, radius in the Java code)
ARENAS = [
    ("Grauwe Klodderreus (npc:kampvijver)", "POI_VADSRIJK.md", "arena_klodderreus.centrum", 26),
    ("Koning Klodder", "POI_VADSRIJK.md", "arena_koning_klodder.centrum", 32),
    ("Opperzwam", "POI_VADSRIJK.md", "arena_opperzwam.centrum", 22),
    ("Galmwolf (z2)", "POI_Z2.md", "arena_galmwolf", 32),
    ("Spiegelzucht (z2)", "POI_Z2.md", "arena_spiegelzucht", 22),
    ("Boormeester (z2)", "POI_Z2.md", "arena_boormeester", 20),
]


def kolommen(cx, cz, r, y0, y1):
    """-> dict (x, z) -> {y: movement class} for the disc, y0..y1."""
    tx0, tx1 = (cx - r) // T, (cx + r) // T
    tz0, tz1 = (cz - r) // T, (cz + r) // T
    W = 2 * r + 1
    cls = np.zeros((W, W, y1 - y0 + 1), np.uint8)
    plant = np.zeros_like(cls, bool)
    for tx in range(tx0, tx1 + 1):
        for tz in range(tz0, tz1 + 1):
            pad = os.path.join(TEGELS, f"t_{tx}_{tz}.nbt")
            if not os.path.exists(pad):
                continue
            pal, (xs, ys, zs, st) = TL.lees(pad)
            lut = np.array([BK.klasse(s)[0] for s in pal], np.uint8)
            wx, wz = xs + tx * T - (cx - r), zs + tz * T - (cz - r)
            k = (wx >= 0) & (wx < W) & (wz >= 0) & (wz < W) & (ys >= y0) & (ys <= y1)
            cls[wx[k], wz[k], ys[k] - y0] = lut[st[k]]
            plant[wx[k], wz[k], ys[k] - y0] = lut[st[k]] == BK.PLANT
    return cls, plant


def meet(naam, c, r):
    cx, cy, cz = c
    y0, y1 = cy - 6, cy + 8
    cls, plant = kolommen(cx, cz, r, y0, y1)
    vrij = (BK.AIR, BK.PLANT)
    tel = {"droog": 0, "plant": 0, "water": 0, "anders": 0}
    vlak = 0
    totaal = 0
    teken = {}
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if dx * dx + dz * dz > r * r:
                continue
            totaal += 1
            col = cls[dx + r, dz + r]
            pl = plant[dx + r, dz + r]
            soort, staan = "anders", None
            for y in range(cy + 4, cy - 5, -1):   # highest stand spot in the band
                i = y - y0
                if col[i] == BK.WATER and all(col[j] in vrij for j in range(i + 1, min(len(col), i + 3))):
                    soort = "water"
                    break
                if col[i - 1] in (BK.SOLID, BK.SLAB, BK.STAIR) and col[i] in vrij and col[i + 1] in vrij:
                    soort = "plant" if pl[i] or pl[i + 1] else "droog"
                    staan = y
                    break
            tel[soort] += 1
            if soort in ("droog", "plant") and abs(staan - cy) <= 1:
                vlak += 1
            teken[(dx, dz)] = {"droog": ".", "plant": '"', "water": "~", "anders": "#"}[soort]
    kaart = []
    for dz in range(-r, r + 1, 2):
        kaart.append("".join(teken.get((dx, dz), " ") for dx in range(-r, r + 1)))
    droog = (tel["droog"] + tel["plant"]) / totaal
    print(f"== {naam}: centre {c}, radius {r}, {totaal} columns")
    print("   " + ", ".join(f"{k} {100 * v / totaal:.0f} %" for k, v in tel.items()) + f"; flat-dry (+-1) {100 * vlak / totaal:.0f} %")
    ok = droog >= 0.6 and vlak / totaal >= 0.5
    print("   " + ("OK: room for melee" if ok else "LITTLE ROOM for melee"))
    for rij in kaart:
        print("   " + rij)
    return ok


def main():
    alleen = sys.argv[1:]
    for naam, bestand, pid, r in ARENAS:
        if alleen and not any(a.lower() in naam.lower() for a in alleen):
            continue
        c = poi(bestand, pid)
        if c is None:
            print(f"== {naam}: POI {pid} not found")
            continue
        meet(naam, c, r)


if __name__ == "__main__":
    main()
