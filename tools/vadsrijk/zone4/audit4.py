"""Zone 4 walkability audit: runs tools/vadsrijk/audit/audit.py (the zone-1 survival audit) over ONLY the zone-4 tiles, with
the zone-4 campfires + the Havenplein respawn as seeds, the zone-4 areas for grouping, and its own outputs (the shared
safe-spot json of the unstuck key is NOT touched):
    AUDIT_Z4.md (project root, generated: do not commit), tools/vadsrijk/_cache/audit_vadsrijk.png (zone 4 picture),
    tools/vadsrijk/_cache/veilige_plekken_z4.json (the zone-4 safe spots; the integrator can merge them).
Run from the project root after maak4:  python tools/vadsrijk/zone4/audit4.py
"""
import os
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, "..", "audit"))
sys.path.insert(0, HIER)
import audit as A  # noqa: E402
import tegellezer as TL  # noqa: E402

TX0, TZ0, TX1, TZ1 = 27, -12, 39, -5
_tegels = TL.tegels
TL.tegels = lambda d: [t for t in _tegels(d) if TX0 <= t[0] <= TX1 and TZ0 <= t[1] <= TZ1]
A.TL = TL
A.TEGEL_FILTER = lambda t: True   # (T-INT: TL.tegels above already keeps only the zone-4 box)
A.PLEKKEN_JAVA = os.path.join(A.ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone4", "wereld",
                              "Z4WereldPlekken.java")
A.RAPPORT = os.path.join(A.ROOT, "AUDIT_Z4.md")
A.PLEKKEN_JSON = os.path.join(A.CACHE, "veilige_plekken_z4.json")
A.GEBIEDEN = [("lampionhaven", 1770, -550, 1930, -390), ("knuffeltij", 1870, -385, 2000, -305),
              ("strandfeest", 1940, -645, 2050, -565), ("bloemeneiland", 2035, -450, 2135, -360),
              ("koraalbaai", 2120, -630, 2250, -500), ("vliegereiland", 2255, -700, 2345, -630),
              ("vuurtoreneiland", 2368, -528, 2432, -472), ("pierkop", 2445, -590, 2540, -540),
              ("mysterie", 2445, -360, 2515, -300), ("wrak", 2140, -715, 2200, -685), ("schildpad", 2200, -480, 2280, -410),
              ("zee", 1728, -768, 2559, -257)]
A.GEBIED_VOLG = [g[0] for g in A.GEBIEDEN] + ["buiten zone 1"]
_lees = A.lees_plekken


def lees_plekken():
    alle, kampvuren, deuren, vast = _lees()
    alle["aankomst"] = alle["respawn"]
    return alle, kampvuren, deuren, vast


A.lees_plekken = lees_plekken

if __name__ == "__main__":
    A.main()
