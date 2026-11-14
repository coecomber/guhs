"""1.10.3: a central return portal (the zone-1 terugportaal look) on the main square of every safe town, stamped into the map
tiles, plus the one-time world patches for the already generated live world.

User request (2026-11-14): "every safe zone must have at least 1 central portal". The safe towns are the kern areas with
stad = true (docs/wiki/vadsrijk_runtime.json "gebieden"; the Hoogvads districts lie inside z5_hoogvads (except de Luchthaven: its own portal), the Bondenvallei is
stamped at runtime and has its own gate back to Hoogvads); haven already has a central one (wereld/bouwsels.terugportalen,
21 blocks from haven_plein), so it is not in STEDEN. Like speeltuin.py this is ONE deterministic pass over the written tiles
(the towns come from six different generators): it searches a flat, free 9 x 5 spot near the square, away from every published
stand spot (POI_*.md: NPC spots, doors, stalls), streets/paths and the playground, and stamps the portal there.

  python tools/vadsrijk/stadsleven/terugportalen.py --droog     only search + print + write the data/patch files (no tiles)
  python tools/vadsrijk/stadsleven/terugportalen.py             also stamp the tiles (maak_kaart.py runs it after speeltuin.py)
  python tools/vadsrijk/stadsleven/terugportalen.py --stad kandijkom

Outputs (both GENERATED, committed):
  src/main/resources/data/guhs/vadsrijk/terugportalen.txt          one line per town (Java: wereld/TerugportaalPlekken, wiki: bron.py)
  src/main/resources/data/guhs/vadsrijk/patch/portaal_<stad>.json  the live patch (/guhs vadsrijk patch portaal_<stad> [--echt])
A spot once written to terugportalen.txt is kept (the patch and the tiles must agree); delete its line to search again.

The look = wereld/bouwsels.poortje(breed 5, hoog 6) exactly, incl. its Python floor division: frame columns at u -4 and +3,
portal u -3..2 (6 wide), v 0..5 (6 high), frame rows v -1 (in the floor) and 6, a shroomlight at (0, 7), guh ears (zwamzoet.oortje)
centred at u -2 and +2 from v 7, all on a 9 x 5 floor of heart tiles (u -4..4, w -2..2). u runs along the frame, w across it.
"""
import argparse
import json
import os
import sys

import numpy as np

HIER = os.path.dirname(os.path.abspath(__file__))
VADSRIJK = os.path.dirname(HIER)
ROOT = os.path.abspath(os.path.join(VADSRIJK, "..", ".."))
sys.path.insert(0, VADSRIJK)
sys.path.insert(0, HIER)
import verbind as VB  # noqa: E402
import speeltuin as SP  # noqa: E402

RES = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "vadsrijk")
DATA = os.path.join(RES, "terugportalen.txt")
PATCHES = os.path.join(RES, "patch")
T, HOOG = VB.T, VB.HOOG
VRIJ = 10                # clear cells above the floor over the footprint (ears reach v 9)
MAX_R = 44               # search radius around the square
AS = 4                   # half width of the square's N-S and E-W axes that stay free

# town: (zone, kern area, POI id, square (x, stand y, z) = the centre the portal should be near)
STEDEN = {
    "zwamzoet": (1, "zwamzoet", "terugportaal_zwamzoet_plein", (0, 64, 59)),
    "knuffelkamp": (1, "knuffelkamp", "terugportaal_knuffelkamp", (-384, 87, 1025)),
    "kandijkom": (2, "z2_kandijkom", "terugportaal_kandijkom", (704, 91, -1056)),
    "kandijkom_dok": (2, "z2_kandijkom_dok", "terugportaal_kandijkom_dok", (704, 91, -910)),
    "honingtak": (3, "z3_honingtak", "terugportaal_honingtak", (1400, 167, -520)),
    "lampionhaven": (4, "z4_lampionhaven", "terugportaal_lampionhaven", (1848, 67, -467)),
    "hoogvads": (5, "z5_hoogvads", "terugportaal_hoogvads", (2748, 219, -529)),
    "luchthaven": (5, "z5_luchthaven", "terugportaal_luchthaven", (2630, 217, -420)),
    "rozenkloof": (6, "z6_rozenkloof", "terugportaal_rozenkloof", (3392, 69, -714)),
    "saffraanpoort": (6, "z6_saffraanpoort", "terugportaal_saffraanpoort", (3756, 71, -863)),
}
# the kern area boxes (runtime dump "gebieden"): the portal must stand inside
GEBIED = {
    "zwamzoet": (-160, -160, 160, 160), "knuffelkamp": (-512, 900, -256, 1150), "z2_kandijkom": (620, -1140, 788, -974), "z2_kandijkom_dok": (664, -973, 744, -856),
    "z3_honingtak": (1244, -736, 1564, -416), "z4_lampionhaven": (1780, -531, 1912, -409), "z5_hoogvads": (2526, -625, 2907, -415), "z5_luchthaven": (2578, -467, 2682, -345),
    "z6_rozenkloof": (3236, -800, 3500, -652), "z6_saffraanpoort": (3634, -950, 3890, -742),
}
STRAAT = ("klinker", "path", "gravel", "rail", "trap", "stairs", "slab")
HARTJES = "guhs:tegel_hartjes"
STEEN = "guhs:vr_w_poortsteen"


def ontwerp(as_):
    """[(u, v, w, state)]: v 0 = the stand level (the floor is v -1)."""
    b = []
    for u in range(-4, 5):
        for w in range(-2, 3):
            b.append((u, -1, w, HARTJES))
    for u in range(-4, 4):
        for v in range(-1, 7):
            rand = u in (-4, 3) or v in (-1, 6)
            b.append((u, v, 0, STEEN if rand else f"guhs:vr_w_portaal[axis={as_},wakker=true]"))
    b.append((0, 7, 0, "minecraft:shroomlight"))
    for k in (-2, 2):
        for (du, dv) in ((-1, 0), (0, 0), (1, 0), (-1, 1), (0, 1), (1, 1), (0, 2)):
            b.append((k + du, 7 + dv, 0, "minecraft:pink_concrete" if du == 0 and dv < 2 else "minecraft:pink_wool"))
    laatste = {}
    for (u, v, w, s) in b:
        laatste[(u, v, w)] = s          # (the frame's bottom row overrides the heart tile under it)
    return [(u, v, w, s) for (u, v, w), s in laatste.items()]


def wereld(cx, y, cz, as_, u, v, w):
    """(u along the frame, w across) -> world; y = stand level."""
    return (cx + u, y + v, cz + w) if as_ == "x" else (cx + w, y + v, cz + u)


class Blok:
    """The block names around a town square (dense, from the tiles)."""

    def __init__(self, TG, cx, cy, cz):
        self.TG = TG
        self.x0, self.z0, self.y0 = cx - MAX_R - 8, cz - MAX_R - 8, cy - 12
        n = 2 * (MAX_R + 8) + 1
        self.v = np.zeros((n, 34, n), np.int32)
        self.heeft = False
        for tx in range(self.x0 // T, (self.x0 + n - 1) // T + 1):
            for tz in range(self.z0 // T, (self.z0 + n - 1) // T + 1):
                d = TG.dicht((tx, tz))
                if d is None:
                    continue
                self.heeft = True
                ax0, az0 = max(self.x0, tx * T), max(self.z0, tz * T)
                ax1, az1 = min(self.x0 + n - 1, tx * T + T - 1), min(self.z0 + n - 1, tz * T + T - 1)
                self.v[ax0 - self.x0:ax1 - self.x0 + 1, :, az0 - self.z0:az1 - self.z0 + 1] = \
                    d[ax0 - tx * T:ax1 - tx * T + 1, self.y0:self.y0 + 34, az0 - tz * T:az1 - tz * T + 1]

    def naam(self, x, y, z):
        i, j, k = x - self.x0, y - self.y0, z - self.z0
        if 0 <= i < self.v.shape[0] and 0 <= j < self.v.shape[1] and 0 <= k < self.v.shape[2]:
            return self.TG.pal[int(self.v[i, j, k])]
        return "minecraft:air"


def zoek(B, stad, verboden, bezet):
    zone, gid, poi, (mx, my, mz) = STEDEN[stad]
    gx0, gz0, gx1, gz1 = GEBIED[gid]
    beste = None
    for cx in range(mx - MAX_R, mx + MAX_R + 1):
        for cz in range(mz - MAX_R, mz + MAX_R + 1):
            d = ((cx - mx) ** 2 + (cz - mz) ** 2) ** 0.5
            if d > MAX_R or d < 6 or (beste and d >= beste[0]):
                continue
            as_ = "z" if abs(cx - mx) > abs(cz - mz) else "x"      # the frame faces the square
            cel = [wereld(cx, 0, cz, as_, u, 0, w) for u in range(-4, 5) for w in range(-2, 3)]
            if not all(gx0 <= x <= gx1 and gz0 <= z <= gz1 for x, _, z in cel):
                continue
            # keep the square's two main axes free (the walking lines from the gates to the middle, a tower's arches)
            if any(abs(x - mx) <= AS + 1 or abs(z - mz) <= AS + 1 for x, _, z in cel):
                continue
            # one floor height for all 45 cells, solid floor blocks, no street/path
            y = None
            ok = True
            for (x, _, z) in cel:
                f = None
                for yy in range(my + 3, my - 5, -1):
                    if not SP.wisbaar(SP.kaal(B.naam(x, yy, z))):
                        f = yy
                        break
                if f is None or (y is not None and f != y):
                    ok = False
                    break
                y = f
                n = SP.kaal(B.naam(x, f, z))
                if not SP.vloer(n) or any(s in n for s in STRAAT):
                    ok = False
                    break
            if not ok or abs(y + 1 - my) > 3:
                continue
            # clear above, a walkable ring around it (room to walk round, nothing blocked)
            if any(not SP.wisbaar(SP.kaal(B.naam(x, yy, z))) for (x, _, z) in cel for yy in range(y + 1, y + 1 + VRIJ)):
                continue
            ring = [wereld(cx, 0, cz, as_, u, 0, w) for u in range(-5, 6) for w in (-3, 3)] + \
                   [wereld(cx, 0, cz, as_, u, 0, w) for u in (-5, 5) for w in range(-2, 3)]
            if any(not SP.wisbaar(SP.kaal(B.naam(x, yy, z))) for (x, _, z) in ring for yy in (y + 1, y + 2)):
                continue
            if any(SP.wisbaar(SP.kaal(B.naam(x, y, z))) for (x, _, z) in ring):
                continue
            # 2 blocks away from every published stand spot, the playground and the other portals
            vx0, vx1 = min(c[0] for c in cel) - 2, max(c[0] for c in cel) + 2
            vz0, vz1 = min(c[2] for c in cel) - 2, max(c[2] for c in cel) + 2
            if any(vx0 <= px <= vx1 and vz0 <= pz <= vz1 and y - 2 <= py <= y + 4 for (px, py, pz) in verboden):
                continue
            if any(not (vx1 < bx0 or vx0 > bx1 or vz1 < bz0 or vz0 > bz1) for (bx0, bz0, bx1, bz1) in bezet):
                continue
            beste = (d, cx, y + 1, cz, as_)
    return beste


def poi_van(stad, cx, y, cz, as_):
    """The stand spot in front of the portal, on the square's side (zone 1: frame z + 2)."""
    mx, _, mz = STEDEN[stad][3]
    if as_ == "x":
        return [cx, y, cz + (2 if mz >= cz else -2)]
    return [cx + (2 if mx >= cx else -2), y, cz]


def lees_data():
    uit = {}
    if os.path.exists(DATA):
        for r in open(DATA, encoding="utf-8"):
            d = r.split()
            if len(d) >= 7 and d[0] == "terugportaal":
                uit[d[1]] = (int(d[2]), int(d[3]), int(d[4]), d[5])
    return uit


def schrijf_data(plekken):
    with open(DATA, "w", encoding="utf-8", newline="\n") as f:
        f.write("# 1.10.3: the central return portals of the safe towns, GENERATED by tools/vadsrijk/stadsleven/terugportalen.py.\n")
        f.write("# terugportaal <town> <x> <stand y> <z> <axis> <poi id> <poi x y z> <zone>   (x/z = the frame centre column u 0, w 0; the POI stands in front)\n")
        for stad in STEDEN:
            if stad in plekken:
                cx, y, cz, as_ = plekken[stad]
                px, py, pz = poi_van(stad, cx, y, cz, as_)
                f.write(f"terugportaal {stad} {cx} {y} {cz} {as_} {STEDEN[stad][2]} {px} {py} {pz} {STEDEN[stad][0]}\n")


def patch_json(B, stad, plek):
    """The live patch: heart floor + frame bottom may replace the floor blocks seen in the tiles, the rest only air."""
    cx, y, cz, as_ = plek
    zone, gid, poi, _ = STEDEN[stad]
    vloer = sorted({SP.kaal(B.naam(*wereld(cx, y, cz, as_, u, -1, w))) for u in range(-4, 5) for w in range(-2, 3)})
    groepen = {}
    for (u, v, w, s) in ontwerp(as_):
        groepen.setdefault((s, v == -1), []).append(wereld(cx, y, cz, as_, u, v, w))
    dozen = []
    volgorde = [HARTJES, STEEN, "minecraft:shroomlight", "minecraft:pink_concrete", "minecraft:pink_wool"]
    sleutels = sorted(groepen, key=lambda k: (volgorde.index(k[0]) if k[0] in volgorde else 2.5, not k[1]))
    for (s, isvloer) in sleutels:
        kolommen = {}
        for (x, yy, z) in groepen[(s, isvloer)]:
            kolommen.setdefault((x, z), []).append(yy)
        runs = []                                   # (x, z, y0, y1): contiguous along y
        for (x, z), ys in sorted(kolommen.items()):
            ys.sort()
            y0 = ys[0]
            for i in range(1, len(ys) + 1):
                if i == len(ys) or ys[i] != ys[i - 1] + 1:
                    runs.append((x, z, y0, ys[i - 1]))
                    if i < len(ys):
                        y0 = ys[i]
        rijen = []                                  # merge neighbouring runs with the same y range into boxes
        for (x, z, y0, y1) in runs:
            for r in rijen:
                if r[2] == y0 and r[3] == y1 and ((r[1][1] == z and r[0][1] == z and r[1][0] + 1 == x)
                                                  or (r[1][0] == x and r[0][0] == x and r[1][1] + 1 == z)):
                    r[1] = (x, z)
                    break
            else:
                rijen.append([(x, z), (x, z), y0, y1])
        soort = {HARTJES: "vloer", STEEN: "frame", "minecraft:shroomlight": "lamp", "minecraft:pink_concrete": "oortje",
                 "minecraft:pink_wool": "oortje"}.get(s, "portaal")
        for (a_, b_, y0, y1) in rijen:
            d = {"naam": f"{soort} {len(dozen) + 1}", "min": [a_[0], y0, a_[1]], "max": [b_[0], y1, b_[1]], "blok": s}
            if isvloer:
                d["vervang"] = vloer
            dozen.append(d)
    return {
        "_uitleg": f"1.10.3: the central return portal of {stad} (GENERATED by tools/vadsrijk/stadsleven/terugportalen.py, same blocks "
                   f"as the tiles). The floor boxes may replace the square's own floor ({', '.join(vloer)}), everything else only air.",
        "dimensie": "guhs:vadsrijk",
        "beschrijving": f"terugportaal op het plein van {stad}",
        "gebied": gid,
        "poi": {"id": poi, "pos": poi_van(stad, cx, y, cz, as_)},
        "dozen": dozen,
    }


def stempel(TG, plek, gewijzigd):
    cx, y, cz, as_ = plek
    for (u, v, w, s) in ontwerp(as_):
        wx, wy, wz = wereld(cx, y, cz, as_, u, v, w)
        t = (wx // T, wz // T)
        d = gewijzigd.get(t)
        if d is None:
            d = TG.dicht(t)
            if d is None:
                continue
            gewijzigd[t] = d = d.copy()
        d[wx % T, wy, wz % T] = TG.id(s)


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--stad", default=None)
    ap.add_argument("--droog", action="store_true", help="search + data/patch files, no tiles")
    ap.add_argument("--map", default=VB.UIT)
    a = ap.parse_args(argv)
    TG = VB._Tegels(a.map)
    plekken = lees_data()
    verboden = VB.poi_cellen()
    bezet = []
    for (x, y, z) in SP.lees_data().values():
        bezet.append((x - 1, z - 1, x + SP.W, z + SP.D))
    gewijzigd = {}
    os.makedirs(PATCHES, exist_ok=True)
    for stad, (zone, gid, poi, (mx, my, mz)) in STEDEN.items():
        if a.stad and stad != a.stad:
            continue
        B = Blok(TG, mx, my, mz)
        if not B.heeft:
            print(f"terugportaal {stad}: geen tegels, overgeslagen")
            continue
        if stad not in plekken:
            b = zoek(B, stad, verboden, bezet)
            if b is None:                   # a square without a clear cross of streets (a tree town, a round plaza)
                global AS
                AS, oud = -99, AS
                b = zoek(B, stad, verboden, bezet)
                AS = oud
            if b is None:
                print(f"terugportaal {stad}: GEEN vrije vlakke plek binnen {MAX_R} van het plein")
                continue
            plekken[stad] = b[1:]
        cx, y, cz, as_ = plekken[stad]
        bezet.append((cx - 6, cz - 6, cx + 6, cz + 6))
        al = SP.kaal(B.naam(cx, y + 2, cz)) == "guhs:vr_w_portaal"
        print(f"terugportaal {stad}: frame {cx},{y},{cz} as {as_}, poi {poi_van(stad, cx, y, cz, as_)}, "
              f"{((cx - mx) ** 2 + (cz - mz) ** 2) ** 0.5:.0f} van het plein" + (" (staat al in de tegels)" if al else ""))
        if not al:
            with open(os.path.join(PATCHES, f"portaal_{stad}.json"), "w", encoding="utf-8", newline="\n") as f:
                f.write(json.dumps(patch_json(B, stad, plekken[stad]), indent=2, ensure_ascii=False) + "\n")
            if not a.droog:
                stempel(TG, plekken[stad], gewijzigd)
    schrijf_data(plekken)
    for t, d in gewijzigd.items():
        TG.schrijf(t, d, extra={k: r for k, r in TG.nbt(t).items() if d[k]})
    if gewijzigd:
        print(f"terugportalen: {len(gewijzigd)} tegels herschreven")
        if os.environ.get("GUHS_VERBIND_LATER") != "1":
            VB.na(sorted(gewijzigd))


if __name__ == "__main__":
    main()
