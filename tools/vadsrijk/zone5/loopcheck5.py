"""Zone 5 walkability proof (T-Z5WERELD): the shared loopcheck (tools/vadsrijk/loopcheck.py) over ALL zone-5 tiles in one window,
y 118..319 (zone 4 below is not checked here), seeds = the airport station (where the airship lets you off) and the cathedral
square; the t-z5wezens lifts (city rim <-> fins / tail) count as two-way links. Reports:
  - VAL  = places you can reach from the seeds but never get back from;
  - TRAP = stairs that lead nowhere; POI = a published spot not reachable from the seeds.
The fins/tail platforms and the rooms inside the whale are reached by the lifts/hatches (teleports) only.
Run from the project root after maak5:  python tools/vadsrijk/zone5/loopcheck5.py
"""
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

import plan5 as PL  # noqa: E402

# spots that are props, block positions or only reachable by a teleport (lift/hatch/story)
SLA = ("haven_5_4_luchtschip", "haven_5_4_mast", "spuitgat_", "wolkwaldroom", "z5w_wolkwaldroom", "z5w_buikgangen_", "z5w_schuilnest_m", "troonzaal_troon",
       "staartvin_uitkijk")


def main():
    with open(os.path.join(HIER, "..", "_cache", "plekken_z5.json"), encoding="utf-8") as f:
        alle = json.load(f)
    links = []
    for k in ("zuidvin", "noordvin", "staart"):
        b, o = alle.get(f"z5w_lift_{k}_boven"), alle.get(f"z5w_lift_{k}_onder")
        if b and o:
            links.append([b, o, True])
    pois = {k: v for k, v in alle.items() if not k.startswith(SLA)}
    zaden = {"seeds": [alle["luchthaven_station"], alle["kathedraal_plein"]], "links": links, "pois": pois}
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
    cmd = [sys.executable, os.path.join(HIER, "..", "loopcheck.py"), "--tegels", tdir, "--venster", "12", "--y0", str(PL.NAAD_Y),
           "--y1", "320", "--zaden", zp, "--json", os.path.join(HIER, "..", "_cache", "loopcheck_z5.json")]
    cmd += sys.argv[1:]
    subprocess.run(cmd, cwd=ROOT, check=True)
    shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
