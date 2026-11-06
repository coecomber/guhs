"""Zone 6 walkability audit (U-Z6WERELD): runs tools/vadsrijk/audit/audit.py (the zone-1 survival audit) over ONLY the zone-6 tiles,
with the zone-6 campfires + the line-E arrival as seeds, the zone-6 areas for grouping, and its own outputs (the shared safe-spot
json of the unstuck key is NOT touched):
    AUDIT_Z6.md (project root, generated: do not commit), tools/vadsrijk/_cache/veilige_plekken_z6.json (the integrator can merge).
Run from the project root after maak6:  python tools/vadsrijk/zone6/audit6.py
"""
import os
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "..", "audit"))
sys.path.insert(0, HIER)
import audit as A  # noqa: E402
import tegellezer as TL  # noqa: E402
import plan6 as PL  # noqa: E402

_tegels = TL.tegels
TL.tegels = lambda d: [t for t in _tegels(d) if PL.TX0 <= t[0] <= PL.TX1 and PL.TZ0 <= t[1] <= PL.TZ1]
A.TL = TL
A.TEGEL_FILTER = lambda t: True
A.PLEKKEN_JAVA = os.path.join(A.ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone6", "wereld",
                              "Z6WereldPlekken.java")
A.RAPPORT = os.path.join(A.ROOT, "AUDIT_Z6.md")
A.PLEKKEN_JSON = os.path.join(A.CACHE, "veilige_plekken_z6.json")
A.GEBIEDEN = [(g[0][3:], g[1], g[2], g[3], g[4]) for g in PL.GEBIEDEN]
A.GEBIED_VOLG = [g[0] for g in A.GEBIEDEN] + ["buiten zone 1"]
_lees = A.lees_plekken


def lees_plekken():
    alle, kampvuren, deuren, vast = _lees()
    kampvuren = kampvuren or [v for k, v in sorted(alle.items()) if k.startswith("kampvuur_")]   # (zone 6 lists campfire ids)
    alle["aankomst"] = alle.get("haven_z6_z5_vertrek", alle.get("rozenkloof_plein"))
    alle["respawn"] = alle["aankomst"]
    return alle, kampvuren, deuren, vast


A.lees_plekken = lees_plekken

if __name__ == "__main__":
    A.main()
