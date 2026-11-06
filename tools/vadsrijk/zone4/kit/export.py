"""Export van zone 4 "De Lampionkust" naar blokken in WERELDcoördinaten (voor een kaartbouwer).

    python export.py [x1 z1 x2 z2] [--tegel 128] [--uit map]
        schrijft per tegel <uit>/tegel_<x>_<z>.npz met: v (uint16 [X,Y,Z]), pal (lijst blockstates), oorsprong (x,y,z)
        + <uit>/npcs.json (guh-figuurtjes: x,y,z,yaw,skin,zit). Niet-blok-dingen (vliegers, touwen,
        zeilen, vlaggetjes) zitten NIET in de export: die moeten entities/nieuwe blokken worden, zie LEESMIJ.

    In Python:
        import export
        for (x, y, z, state) in export.blokken(1740, -716, 2540, -266):   # alle niet-luchtblokken, wereldcoördinaten
            ...
        export.tegel(x1, z1, x2, z2) -> W (schemerkit.Wereld, k=1): W.v[i,j,k] = index in W.pal, oorsprong W.b[:3]

Standaardvak = de landgrens van zone 4 uit de wereldindeling (kust.LAND_Z4: x 1740..2540, z -716..-266).
y loopt van 30 (zeebodem) tot 170; water staat tot en met y 62 (kust.ZEE).
Zonder de verte (de Schemerwortel = zone 3, Moeder Wolkwal/Hoogvads = zone 5); zet verte=True om die mee te nemen.
"""
import os, sys, json, argparse
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import schemerkit as SK  # noqa: E402
import kust as KU  # noqa: E402
import haven as HV  # noqa: E402
import eilanden as EI  # noqa: E402

Y1, Y2 = 30, 170


def tegel(x1, z1, x2, z2, verte=False, leven=True):
    """Bouwt het vak [x1,x2) x [z1,z2) op ware schaal (k=1) en geeft de Wereld terug."""
    W = SK.Wereld(x1, Y1, z1, x2, Y2, z2, 1.0)
    W.vrij = lambda x, z: HV.vrij(x, z) and EI.vrij(x, z)
    delen = ("terrein", "schuim") + (("verte",) if verte else ())
    KU.bouw(W, delen)
    HV.bouw(W, leven=leven)
    EI.bouw(W, leven=leven)
    KU.begroeiing(W)
    KU.plantjes(W)
    W.fix_connections()
    return W


def blokken(x1, z1, x2, z2, stap=128, verte=False):
    """Generator over alle niet-luchtblokken: (x, y, z, blockstate) in wereldcoördinaten."""
    for tx in range(x1, x2, stap):
        for tz in range(z1, z2, stap):
            W = tegel(tx, tz, min(tx + stap, x2), min(tz + stap, z2), verte=verte)
            ox, oy, oz = W.b[:3]
            for (i, j, k) in np.argwhere(W.v > 0):
                yield (int(ox + i), int(oy + j), int(oz + k), W.pal[W.v[i, j, k]])


def npcs(W):
    ox, oy, oz = W.b[:3]
    return [dict(x=ox + g[0], y=oy + g[1], z=oz + g[2], yaw=g[3], skin=g[4], zit=bool(g[5])) for g in W.guhs]


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("vak", nargs="*", type=int, default=list(KU.LAND_Z4))
    ap.add_argument("--tegel", type=int, default=128)
    ap.add_argument("--uit", default=os.path.join(HERE, "..", "export"))
    ap.add_argument("--verte", action="store_true")
    a = ap.parse_args()
    x1, z1, x2, z2 = a.vak
    os.makedirs(a.uit, exist_ok=True)
    alle_npcs = []
    for tx in range(x1, x2, a.tegel):
        for tz in range(z1, z2, a.tegel):
            W = tegel(tx, tz, min(tx + a.tegel, x2), min(tz + a.tegel, z2), verte=a.verte)
            np.savez_compressed(os.path.join(a.uit, f"tegel_{tx}_{tz}.npz"), v=W.v, pal=np.array(W.pal),
                                oorsprong=np.array(W.b[:3]))
            alle_npcs += npcs(W)
            print("tegel", tx, tz, len(W.pal), "blokken", flush=True)
    with open(os.path.join(a.uit, "npcs.json"), "w", encoding="utf-8") as f:
        json.dump(alle_npcs, f, indent=1)
    print("klaar:", a.uit)
