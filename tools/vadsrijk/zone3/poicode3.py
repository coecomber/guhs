"""Zone 3: does every POI id the CODE asks for exist in the generated Z3WereldPlekken (v2)?

The zone-3 slices read spots as w("<id>", fallback...) / wereld("<id>") / Z3WereldPlekken.plek("<id>") / rel(c, "<id>", ...)
and constants named *_POI* = "<id>". A missing id silently falls back to an OLD (pre-revamp) coordinate, so after the revamp
every id must exist (wa(own, anchor, ...) only needs the anchor). Ids built in a loop ("lantaarnring_" + i) are checked as a prefix (some key must start with it).
Also checks that every "poi:<id>" in quests_z3.json is a key of Z3NpcPlekken.POI (poi("<id>", ...) calls).

Run from the project root:  python tools/vadsrijk/zone3/poicode3.py        (exit 1 when something is missing)
"""
import json
import os
import re
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
SRC = os.path.join(ROOT, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")
WP = os.path.join(SRC, "zone3", "wereld", "Z3WereldPlekken.java")
QUESTS = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "vadsrijk", "npc", "quests_z3.json")
NPCP = os.path.join(SRC, "zone3", "npc", "Z3NpcPlekken.java")

# calls whose first string argument is a Z3WereldPlekken id
CALL = re.compile(r'\b(?:w|wo|wereld|wp|Z3WereldPlekken\.plek)\(\s*"([A-Za-z0-9_.]+)"(\s*\+)?')
REL = re.compile(r'\brel\([^,()]+,\s*"([A-Za-z0-9_.]+)"(\s*\+)?')
# wa("<optional own id>", "<anchor id>", ...): the own id may be missing (it falls back to the anchor), the anchor must exist
ANKER = re.compile(r'wa\(\s*"[A-Za-z0-9_.]+",\s*"([A-Za-z0-9_.]+)"')
CONST = re.compile(r'\b[A-Z0-9_]*POI[A-Z0-9_]*\s*=\s*"([A-Za-z0-9_.]+)"')
# files whose w()/wereld() is the zone-3 lookup (other zones have their own w())
BESTANDEN_PREFIX = ("zone3" + os.sep,)
EXTRA = [os.path.join(SRC, "vriendjes")]


def sleutels():
    s = open(WP, encoding="utf-8").read()
    alle = set(re.findall(r'Map\.entry\("([^"]+)",\s*new BlockPos', s))
    # named lists also count (lift/windkolom/uitkijk ids are looked up by id too)
    alle |= set(re.findall(r'new (?:Lift|Windkolom|WereldPlekken\.Uitkijk)\("([^"]+)"', s))
    return alle


def java_bestanden():
    for basis in [os.path.join(SRC, "zone3")] + EXTRA:
        for d, _, fs in os.walk(basis):
            for f in fs:
                if f.endswith(".java") and not f.endswith("Z3WereldPlekken.java"):
                    yield os.path.join(d, f)


def main():
    keys = sleutels()
    mis, gebruikt = [], 0
    for f in java_bestanden():
        rel = os.path.relpath(f, SRC)
        for n, regel in enumerate(open(f, encoding="utf-8"), 1):
            if regel.strip().startswith(("//", "*", "/*")):
                continue
            for rx in (CALL, REL, CONST, ANKER):
                for m in rx.finditer(regel):
                    id_, plus = m.group(1), (m.group(2) if m.re.groups > 1 else None)
                    gebruikt += 1
                    ok = any(k.startswith(id_) for k in keys) if plus else id_ in keys
                    if not ok:
                        mis.append(f"{rel}:{n}: {id_}{' (prefix)' if plus else ''}")
    print(f"{len(keys)} ids in Z3WereldPlekken, {gebruikt} uses in code")
    # quests -> Z3NpcPlekken.POI
    qmis = []
    if os.path.exists(QUESTS):
        npcp = open(NPCP, encoding="utf-8").read()
        poi_ids = set(re.findall(r'\bpoi\("([a-z0-9_]+)"', npcp))
        prefixes = set(re.findall(r'\bpoi\("([a-z0-9_]+)"\s*\+', npcp))
        for m in re.finditer(r'"poi:([a-z0-9_]+)"', open(QUESTS, encoding="utf-8").read()):
            q = m.group(1)
            if q not in poi_ids and not any(q.startswith(p) for p in prefixes):
                qmis.append(q)
    for r in mis:
        print("MISSING", r)
    for q in sorted(set(qmis)):
        print("QUEST POI NOT IN Z3NpcPlekken.POI:", q)
    if mis or qmis:
        print(f"{len(mis)} code ids + {len(set(qmis))} quest pois missing")
        sys.exit(1)
    print("OK: every POI id used in code exists")


if __name__ == "__main__":
    main()
