"""
Op Safari! step 2, slice S3: writes the ride data that zone_s3_rit.py collected (park._s3) to
assets/guhs/safari/{route,kano,kabelbaan,race,sfeer}.json: assets, so the server reads them from the jar and every
client has the same paths (the rides move along them on both sides).
"""
import json
import os

OUT = os.path.join("src", "main", "resources", "assets", "guhs", "safari")


def schrijf(root, park, log=print):
    s3 = getattr(park, "_s3", None)
    if not s3:
        return
    map_ = os.path.join(root, OUT)
    os.makedirs(map_, exist_ok=True)
    for naam in ("route", "kano", "kabelbaan", "race", "sfeer"):
        data = s3.get(naam)
        if data is None:
            continue
        if naam == "route" and s3.get("schemabord"):
            data = dict(data, schemabord=s3["schemabord"])
        with open(os.path.join(map_, naam + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, separators=(",", ":"), ensure_ascii=False)
            f.write("\n")
    log(f"  s3: ritdata geschreven ({', '.join(k for k in ('route', 'kano', 'kabelbaan', 'race', 'sfeer') if s3.get(k))})")
