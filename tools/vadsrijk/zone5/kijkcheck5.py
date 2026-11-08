"""W-LEVEN: do the townsfolk of Hoogvads (Z5Bewoners) face what they should look at? Builds the zone-5 world like maak5 (~30 s,
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
    import maak5
    W, S, info = maak5.bouw_wereld()
    B = [(float(a), float(b_), float(c), float(d), s, int(e)) for (a, b_, c, d, s, e) in info["bewoners"]]
    ALT = (2624, -520)
    kath = [b for b in B if 2628 <= b[0] <= 2682 and -536 <= b[2] <= -504 and b[1] < 235]
    rapport("kathedraal: zitters->altaar", [b for b in kath if b[5] == 1], lambda b: ALT)
    koor = [b for b in B if b[4] == "guh_wolk" and 2612 <= b[0] <= 2622 and -528 <= b[2] <= -512 and b[1] > 226]
    rapport("kathedraal: koor->schip", koor, lambda b: (2664, -520))
    # kazerne: the 10x5 formation of knights, the drill master = nearest npc_wachterguh
    from collections import Counter
    rid = [b for b in B if b[5] == 2]
    groot = [b for b in rid if 2694 <= b[0] <= 2780 and -606 <= b[2] <= -546]
    yaws = Counter(round(b[3]) for b in groot)
    form = [b for b in groot if round(b[3]) == yaws.most_common(1)[0][0]]
    if form:
        cx = sum(b[0] for b in form) / len(form)
        cz = sum(b[2] for b in form) / len(form)
        wm = min((b for b in B if b[4] == "npc_wachterguh"), key=lambda b: (b[0] - cx) ** 2 + (b[2] - cz) ** 2)
        rapport("kazerne: formatie->drilmeester", form, lambda b: (wm[0], wm[2]))
        rapport("kazerne: drilmeester->formatie", [wm], lambda b: (cx, cz))
    # paleis: the queen (npc_omaguh in the paleisheuvel), the knights in two rows face the aisle, the crowd faces the queen
    pal = [b for b in B if 2778 <= b[0] <= 2858 and -570 <= b[2] <= -470]
    q = [b for b in pal if b[4] == "npc_omaguh"]
    if q:
        q = q[0]
        rij = [b for b in pal if b[5] == 2 and abs(b[2] - q[2]) < 9 and abs(b[1] - q[1]) < 8]
        rapport("paleis: ridders->middenpad", rij, lambda b: (b[0], q[2]))
        menigte = [b for b in pal if b[5] == 0 and abs(b[2] - q[2]) < 5 and 6 < abs(b[0] - q[0]) < 20 and abs(b[1] - q[1]) < 8]
        rapport("paleis: menigte->koningin", menigte, lambda b: (q[0], q[2]))
        zaal_mid = sum(b[0] for b in rij) / len(rij) if rij else q[0]
        rapport("paleis: koningin->zaal", [q], lambda b: (zaal_mid, q[2]))
    # kazerne duel: two knights 3 apart on the same z, facing each other
    kaz = [b for b in B if b[5] == 2 and 2694 <= b[0] <= 2780 and -606 <= b[2] <= -546]
    for a in kaz:
        for b in kaz:
            if a is not b and abs(a[2] - b[2]) < 0.1 and abs(abs(a[0] - b[0]) - 3) < 0.1 and abs(((a[3] - b[3]) % 360) - 180) < 1:
                rapport("kazerne: duel", [a], lambda _b, b=b: (b[0], b[2]))
    # university: the owl students in a ring round their professor
    profs = [b for b in B if b[4] == "npc_professor_zei_guh"]
    uil = [b for b in B if b[4] == "guh_uiltje" and profs and min((b[0] - p[0]) ** 2 + (b[2] - p[2]) ** 2 for p in profs) < 9]
    def naastePr(b):
        p = min(profs, key=lambda p: (b[0] - p[0]) ** 2 + (b[2] - p[2]) ** 2)
        return (p[0], p[2])
    rapport("uni: uiltjes->professor", uil, naastePr)
    print("totaal", len(B))
    if FOUT:
        print("FOUT:", ", ".join(FOUT))
        sys.exit(1)
    print("OK: iedereen kijkt goed")


if __name__ == "__main__":
    main()
