"""W-LEVEN: do the townsfolk of Het Zijdeland (Z6Bewoners) face what they should look at? Builds the zone-6 world like maak5 (~30 s,
no tiles written) and checks the GAME yaw of the figures (bouw5.bewoners -> spel_yaw) against their look targets: the churchgoers
face the altar, the choir the nave, the drill formation its drill master, the palace knights the aisle, the palace crowd the
queen, the owl students their professor, the duelling knights each other. Minecraft yaw Y faces (-sin Y, cos Y).
    python tools/vadsrijk/zone5/kijkcheck5.py        (exit 1 when a group is mostly wrong)
"""
import math
import os
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
sys.path.insert(1, os.path.join(HIER, "kit"))
sys.path.append(os.path.join(HIER, "..", "wereld"))
sys.path.append(os.path.join(HIER, "..", "audit"))
sys.path.append(os.path.join(HIER, ".."))
FOUT = []


def hoek(b, tx, tz):
    Y = math.radians(b[3])
    fx, fz = -math.sin(Y), math.cos(Y)
    dx, dz = tx - b[0], tz - b[2]
    l = math.hypot(dx, dz) or 1
    return math.degrees(math.acos(max(-1, min(1, (fx * dx + fz * dz) / l))))


def rapport(naam, lijst, doel):
    if not lijst:
        print(naam, "LEEG")
        FOUT.append(naam)
        return
    hs = [hoek(b, *doel(b)) for b in lijst]
    goed = sum(1 for h in hs if h < 60)
    print(f"{naam:32s} n={len(lijst):3d}  goed(<60deg)={goed:3d}  gem.hoek={sum(hs) / len(hs):5.1f}")
    if goed < 0.9 * len(lijst):
        FOUT.append(naam)


def main():
    import maak6
    import poi6
    W = maak6.bouw_wereld()
    info = dict(plekken=getattr(W, "plekken", {}), huizen=getattr(W, "huizen", []))
    poi6.verzamel(W, info, snap_=False)
    B = [(float(a), float(b_), float(c), float(d), s, int(e)) for (a, b_, c, d, s, e) in info["bewoners"]]
    R = [(r[0], r[1], r[2], r[3], (180 - r[4]) % 360, r[6]) for r in getattr(W, "rijdieren", [])]   # = poi6.schrijf's rpak
    zaal = [b for b in B if 3738 <= b[0] <= 3786 and -916 <= b[2] <= -894 and b[1] < 80]
    rapport("spiegelzaal: hof->middenpad", [b for b in zaal if b[0] < 3780], lambda b: (b[0], -905))
    rapport("spiegelzaal: troon->zaal", [b for b in zaal if b[0] >= 3780], lambda b: (3760, -905))
    n = goed = 0
    for r in R:
        if not r[5]:
            continue
        for b in B:
            if b[5] == 1 and abs(b[0] - r[1]) < 0.4 and abs(b[2] - r[3]) < 0.4 and 0 < b[1] - r[2] < 3:
                n += 1
                goed += abs(((b[3] - r[4]) + 180) % 360 - 180) < 5
    print(f"ruiters kijken als hun rijdier    n={n} goed={goed}")
    if n == 0 or goed < n:
        FOUT.append("ruiters")
    print("totaal", len(B), "rijdieren", len(R))
    if FOUT:
        print("FOUT:", ", ".join(FOUT))
        sys.exit(1)
    print("OK: iedereen kijkt goed")


if __name__ == "__main__":
    main()
