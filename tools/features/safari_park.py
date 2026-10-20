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

VERSIE = 7      # raise on every change of the park (the game re-stamps when the jar's versie > the stored one)


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


def maak(root=".", preview=False, log=print):
    t0 = time.time()
    park = kit.Park()
    terrein.bouw(park, log)
    modules = zone_modules()
    for m in modules:
        t1 = time.time()
        m.bouw(park, log)
        log(f"  {m.__name__.split('.')[-1]}: {time.time() - t1:.0f}s")
    plekken = plk.maak(park)
    for m in modules:
        if hasattr(m, "plekken"):
            m.plekken(park, plekken)
    plk.vermenigvuldig(park, plekken, log)      # test round 2: ~3x the animals, spread over their zones
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
