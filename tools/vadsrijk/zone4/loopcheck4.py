"""Zone 4 walkability proof (copy of zone3/loopcheck3.py): loopcheck over the WHOLE zone in one window (y 20..150), with the
Havenplein respawn of Lampionhaven as the only safe seed, and every POI checked:
  - VAL  = places you can reach from the respawn but never get back from (pits, drops without a way up);
  - POI  = a published spot that is not reachable from the respawn, or a trap.
Swimming counts (the movement model climbs water), so the islands are connected through the sea like a swimmer; the sailboat
docks are all POIs, so each one must be reachable from the water.
Run from the project root after maak4:  python tools/vadsrijk/zone4/loopcheck4.py [--venster 14]
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

TX0, TZ0, TX1, TZ1 = 27, -12, 39, -5


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--venster", type=int, default=14)
    ap.add_argument("--box", default=None)
    a = ap.parse_args()
    with open(os.path.join(HIER, "..", "_cache", "plekken_z4.json"), encoding="utf-8") as f:
        alle = json.load(f)
    # blocks / water on purpose
    sla = ("kampvuur_", "geheim_", "grotje_", "haven_4_5_mast", "haven_3_4_luchtschip", "haven_4_5_luchtschip", "wrak_oorlogsvlieger",
           "vuurtoren_lamp", "zeeslurf_grotje", "schildpad_lus_midden", "lampionmast", "plein_fontein")
    pois = {k: v for k, v in alle.items() if not k.startswith(sla) and not k.endswith(("doos_min", "doos_max", "_water"))}
    zaden = {"seeds": [alle["respawn"]], "links": [], "pois": pois}
    tmp = tempfile.mkdtemp()
    zp = os.path.join(tmp, "zaden.json")
    with open(zp, "w", encoding="utf-8") as f:
        json.dump(zaden, f)
    tdir = os.path.join(tmp, "tegels")
    os.makedirs(tdir)
    for tx in range(TX0, TX1 + 1):
        for tz in range(TZ0, TZ1 + 1):
            p = os.path.join(TEGELS, f"t_{tx}_{tz}.nbt")
            if os.path.exists(p):
                shutil.copy(p, tdir)
    cmd = [sys.executable, os.path.join(HIER, "..", "loopcheck.py"), "--tegels", tdir, "--venster", str(a.venster), "--y0", "20",
           "--y1", "150", "--zaden", zp, "--json", os.path.join(HIER, "..", "_cache", "loopcheck_z4.json")]
    if a.box:
        cmd += ["--box", a.box]
    subprocess.run(cmd, cwd=ROOT, check=True)
    shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
