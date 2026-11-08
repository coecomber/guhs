"""Zone 3 walkability proof: loopcheck over the WHOLE zone in one window (y 4..316), with the Honingplein respawn as the only
safe seed, the lifts (both ways) and the wind columns (up only) as extra edges, and every POI checked:
  - VAL  = places you can reach from the respawn but never get back from (pits, drops without a way up);
  - POI  = a published spot that is not reachable from the respawn, or a trap.
Run from the project root after maak3:  python tools/vadsrijk/zone3/loopcheck3.py [--venster 14]
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
sys.path.insert(0, HIER)
TEGELS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")

import plan3 as PL  # noqa: E402


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--venster", type=int, default=14)
    ap.add_argument("--box", default=None)
    a = ap.parse_args()
    with open(os.path.join(HIER, "..", "_cache", "plekken_z3.json"), encoding="utf-8") as f:
        alle = json.load(f)
    links = []
    for (lid, boven, onder) in PL.liften(alle):
        links.append([boven, onder, True])
    for w in PL.windkolommen():
        links.append([alle[f"wind_{w[0]}_onder"], alle[f"wind_{w[0]}_boven"], False])
    # blocks on purpose (props, cages, lanterns, masts, airships) and the boss's fly-only perches
    sla = ("kampvuur_", "piepvel_kooi", "piepvel_lantaarnpaal", "landmark_", "ei", "knuffelbeeld", "dok_luchtschip",
           "oosttaksteiger_luchtschip", "mika_imker_raatzaal", "mika_imker_luik",
           "grotlantaarn_", "hart_grotlantaarn_", "kruinroofvogel_zit_", "gebouw.nestkapel.eiersokkel",
           "gebouw.nestkapel.droomsteen", "gebouw.jagershut.premiebord", "gebouw.jagershut.jachtbord")
    pois = {k: v for k, v in alle.items() if not k.startswith(sla) and not k.endswith(("doos_min", "doos_max", "_kooi"))}
    zaden = {"seeds": [alle["respawn"]], "links": links, "pois": pois}
    tmp = tempfile.mkdtemp()
    zp = os.path.join(tmp, "zaden.json")
    with open(zp, "w", encoding="utf-8") as f:
        json.dump(zaden, f)
    tdir = os.path.join(tmp, "tegels")
    os.makedirs(tdir)
    for tx in range(PL.TX0, PL.TX1 + 1):
        for tz in range(PL.TZ0, PL.TZ1 + 1):
            p = os.path.join(TEGELS, f"t_{tx}_{tz}.nbt")
            if os.path.exists(p):
                shutil.copy(p, tdir)
    cmd = [sys.executable, os.path.join(HIER, "..", "loopcheck.py"), "--tegels", tdir, "--venster", str(a.venster), "--y0", "4",
           "--y1", "316", "--zaden", zp, "--json", os.path.join(HIER, "..", "_cache", "loopcheck_z3.json")]
    if a.box:
        cmd += ["--box", a.box]
    subprocess.run(cmd, cwd=ROOT, check=True)
    shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
