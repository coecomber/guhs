"""
Op Safari! (1.6.0): generates Safaripark Vadse Bergen (see tools/features/safari_park/ and SAFARI_CONTRACT.md).

  python tools/features/safari_park.py              tiles + park.json + zones.json + plekken.json
  python tools/features/safari_park.py --preview    also a top-down map and iso renders of a few spots
                                                    (written to ../../safari-ontwerp/park/, outside the repo)
Run from the project root. Takes about 2-3 minutes (the iso previews longer).
"""
import json
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

from safari_park import kit, terrein, plekken as plk          # noqa: E402

VERSIE = 9      # raise on every change of the park (the game re-stamps when the jar's versie > the stored one)


def _json(root, rel, data):
    path = os.path.join(root, kit.OUT_DATA, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write("\n")
    return path


def zones_json(park):
    cel = 16
    n = kit.W // cel
    rij = []
    for cz in range(n):
        for cx in range(n):
            blok = park.zone[cx * cel:(cx + 1) * cel, cz * cel:(cz + 1) * cel]
            binnen = blok[blok >= 0]
            if binnen.size < cel * cel * 0.25:
                rij.append(".")
            else:
                rij.append(str(int(__import__("numpy").bincount(binnen).argmax())))
    return {"cel": cel, "breedte": n, "zones": terrein.ZONES, "raster": "".join(rij),
            "uitleg": "raster[cz * breedte + cx] = index in zones ('.' = decor buiten het park); cel 16x16 kolommen"}


def zone_modules():
    """Every tools/features/safari_park/zone_<naam>.py (landmarks of one zone, the entreedorp too), in name order.
    A module has bouw(park, log) and may have plekken(park, plekken) to adjust the animal homes of its zone."""
    import glob
    import importlib
    map_ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "safari_park")
    mods = [importlib.import_module("safari_park." + os.path.basename(f)[:-3])
            for f in sorted(glob.glob(os.path.join(map_, "zone_*.py")))]
    return sorted(mods, key=lambda m: bool(getattr(m, "NA_ZONES", False)))     # (stable: name order otherwise)


def kijkplekken_afmaken(park, plekken, log=print):
    """Round 3 (the river pipeline slice): every guh chair (kit.stoel) that is not part of a registered viewpoint
    becomes one (chairs within 6 blocks are one hut: the first chair is the kijk-guh's seat, the view is the chairs'
    facing); every viewpoint without favourite spots gets a first guess of 2: the homes in its view (in front of the
    window within 6..30 blocks, or around a tower within 30), the spot being the home's centre pulled a bit toward the
    viewpoint. The zone slices refine this with park.favoriet()."""
    import math
    from safari_park.terrein import ZONES
    stap = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
    gebruikt = set()
    for k in park.kijkplekken:
        if k.get("stoel"):
            gebruikt.add(tuple(k["stoel"]))
    clusters = []
    for st in park.stoelen:
        x, y, z, f = st
        if (x, y, z) in gebruikt:
            continue
        for c in clusters:
            if abs(c[0][0] - x) <= 6 and abs(c[0][2] - z) <= 6 and abs(c[0][1] - y) <= 3:
                c.append(st)
                break
        else:
            clusters.append([st])
    for c in clusters:
        x, y, z, f = c[0]
        # an existing tower / viewpoint close by owns the chairs
        if any(abs(k["oog"][0] - x) <= 6 and abs(k["oog"][2] - z) <= 6 for k in park.kijkplekken):
            continue
        zi = int(park.zone_vol[x, z])
        zone = ZONES[zi] if zi >= 0 else "?"
        dx, dz = stap.get(f, (0, 1))
        park.kijkplek(f"kijkhut_{zone}_{x}_{z}", zone, (x + 0.5, y + 1.6, z + 0.5), (dx, dz), (x, y, z))
    n_fav = 0
    for k in park.kijkplekken:
        if k["favorieten"]:
            continue
        ox, oy, oz = k["oog"]
        r = k.get("richting")
        kand = []
        for p in plekken:
            hx, hy, hz = p["centrum"]
            d = math.hypot(hx - ox, hz - oz)
            if d > 30 or d < (6 if r else 3):
                continue
            if r and ((hx - ox) * r[0] + (hz - oz) * r[1]) / max(d, 1e-6) < 0.45:
                continue                            # not in front of the window
            kand.append((d, p))
        kand.sort(key=lambda t: t[0])
        gezien = set()
        for d, p in kand:
            if p["soort"] in gezien:
                continue
            gezien.add(p["soort"])
            hx, hy, hz = p["centrum"]
            t = min(0.35, 4.0 / max(d, 1))
            fx, fz = int(round(hx + (ox - hx) * t)), int(round(hz + (oz - hz) * t))
            if not (park.binnen[fx, fz] and park.zone[fx, fz] == park.zone[hx, hz]):
                fx, fz = hx, hz
            fy = int(park.hoogte[fx, fz]) + 1
            park.favoriet(f"{k['naam']}_{p['soort']}", k, (fx, fy, fz), [p["soort"]], straal=3)
            n_fav += 1
            if len(gezien) >= 2:
                break
    log(f"  kijkplekken: {len(park.kijkplekken)} ({len(park.stoelen)} stoelen), {n_fav} favorieten (eerste schatting)")


def maak(root=".", preview=False, log=print):
    t0 = time.time()
    park = kit.Park()
    terrein.bouw(park, log)
    modules = zone_modules()
    nagelopen = False
    for m in modules:
        if getattr(m, "NA_ZONES", False) and not nagelopen:
            terrein.bruggen_nalopen(park, log)          # round 3: every path-over-water stretch has a bridge deck
            nagelopen = True
        t1 = time.time()
        m.bouw(park, log)
        log(f"  {m.__name__.split('.')[-1]}: {time.time() - t1:.0f}s")
    if not nagelopen:
        terrein.bruggen_nalopen(park, log)
    plekken = plk.maak(park)
    for m in modules:
        if hasattr(m, "plekken"):
            m.plekken(park, plekken)
    plk.vermenigvuldig(park, plekken, log)      # test round 2: ~3x the animals, spread over their zones
    kijkplekken_afmaken(park, plekken, log)     # round 3: seats -> viewpoints, first-guess favourite spots
    totaal = plk.controleer(plekken)
    log(f"  plekken: {len(plekken)} huizen, {totaal} dieren")
    stats = park.schrijf(root)
    __import__("safari_park.route_data").route_data.schrijf(root, park, log)   # S3: ride data (assets/guhs/safari)
    log(f"  tegels geschreven {time.time() - t0:.0f}s: {sum(n for _, n in stats)} blokken")
    x, y, z, yaw = park.ingang
    _json(root, "park.json", {"versie": VERSIE, "tegels": kit.TEGELS, "tegelgrootte": kit.TEGEL, "hoogte": kit.H,
                              "waterhoogte": kit.WATER_Y, "ingang": [x, y, z], "ingang_yaw": yaw})
    _json(root, "zones.json", zones_json(park))
    _json(root, "plekken.json", plekken)
    _json(root, "kijkplekken.json", {"kijkplekken": park.kijkplekken,
                                     "uitleg": "per kijkplek: oog, kijkrichting (null = rondom, een toren), stoel (de "
                                               "kijk-guh), favorieten = plekken in beeld waar de dieren af en toe heen "
                                               "gaan"})
    if preview:
        import safari_park.preview as pv
        pv.alles(park, plekken, log)
    log(f"klaar in {time.time() - t0:.0f}s")
    return park, stats


def build(h):
    """make_resources hook (optional): the park is big, so it is normally built on its own."""
    maak(".")


if __name__ == "__main__":
    maak(".", preview="--preview" in sys.argv)
