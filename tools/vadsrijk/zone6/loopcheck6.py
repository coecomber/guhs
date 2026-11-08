"""Zone 6 walkability proof (U-Z6WERELD): the shared loopcheck (tools/vadsrijk/loopcheck.py) over ALL zone-6 tiles, y 30..260
(the Zingende Zoutgrot floor is y 49), seeds = where the airship of line E lets you off (haven_z6_z5_vertrek), the Rozenplein
and the Saffraanpoort palace square. Reports:
  - VAL  = places you can reach from the seeds but never get back from;
  - TRAP = stairs that lead nowhere; POI = a published spot not reachable from the seeds (or with no way back).
Run from the project root after maak6:  python tools/vadsrijk/zone6/loopcheck6.py [--box x0,z0,x1,z1]
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

import plan6 as PL  # noqa: E402

# props / block positions / behind a story door (the crystal wall, the sealed gates)
SLA = ("haven_6_5_luchtschip", "haven_6_5_mast", "palmbaken_ster", "duinbaken_ster", "zoutbaken_ster", "ruinebaken_ster",
       "oasebaken_ster", "zoutgrot_kristalwand", "zoutgrot_schat", "kanaalwijk_eend_")


def main():
    with open(os.path.join(HIER, "..", "_cache", "plekken_z6.json"), encoding="utf-8") as f:
        alle = json.load(f)
    pois = {k: v for k, v in alle.items() if not k.startswith(SLA)}
    zaden = {"seeds": [alle[k] for k in ("haven_z6_z5_vertrek", "rozenkloof_plein", "saffraanpoort_paleisplein") if k in alle],
             "links": [], "pois": pois}
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
    cmd = [sys.executable, os.path.join(HIER, "..", "loopcheck.py"), "--tegels", tdir, "--venster", "16", "--y0", "30",
           "--y1", str(PL.Y_MAX), "--zaden", zp, "--json", os.path.join(HIER, "..", "_cache", "loopcheck_z6.json")]
    cmd += sys.argv[1:]
    subprocess.run(cmd, cwd=ROOT, check=True)
    shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
