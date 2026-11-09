#!/usr/bin/env python3
"""W-SPEL taalcheck: finds Vadsrijk translation keys in code and data that have no text in nl_nl.json / en_us.json.

What it scans (src/main/java and src/main/resources/data/guhs):
  1. literal keys in Java: "vadsrijk.foo.bar" (not followed by "+": that is a prefix);
  2. prefix constants: static final String Z = "vadsrijk.z3npc."; then Z + "scene.f2.onder" and helper calls with a bare
     "scene.f2.onder" in the same file (a bare literal counts when its first part is a family that exists under that prefix);
  3. area names: every area id in the code (new Gebied("id", ...), Gebied.cilinder("id", ...), new Wijk("id", ...),
     record-style area rows in *Plekken/*Registry files) needs "vadsrijk.kern.gebied.<id>" (the zone-change title card);
  4. JSON data: every string value that looks like a vadsrijk key.

The runtime half (every area that is really registered, incl. generated ones) is VadsrijkTaalGameTests.

usage: python tools/vadsrijk/taalcheck.py [repo-root]     exit code 1 when something is missing
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parents[2])
LANG = ROOT / "src/main/resources/assets/guhs/lang"
JAVA = ROOT / "src/main/java"
DATA = ROOT / "src/main/resources/data/guhs"

KEY = r"vadsrijk\.[a-z0-9_]+(?:\.[a-z0-9_]+)+"
LIT = re.compile(r'"(' + KEY + r')"(\s*\+)?')
CONST = re.compile(r'(?:static\s+)?final\s+String\s+([A-Z_][A-Z_0-9]*)\s*=\s*"(vadsrijk\.[a-z0-9_.]*\.)"\s*;')
BARE = re.compile(r'"([a-z0-9_]+(?:\.[a-z0-9_]+)+)"')
AREA = re.compile(r'(?:\bGebied(?:\.cilinder)?|\bWijk)\(\s*"([a-z0-9_]+)"')
# lines in test/robot files that only name a key as an example
SKIP_FILES = {"VadsrijkDialoogGameTests.java"}
# files whose Gebied(...) is another record (spawn bands, not kern areas), or a box that is never registered as an area
SKIP_AREA_FILES = {"Z6WezensPlekken.java", "Z2WezensPlekken.java"}
# keys that are documentation examples in comments
SKIP_KEYS = {"vadsrijk.npc.q.iets.klaar.0", "vadsrijk.npc.q.iets.klaar.1", "vadsrijk.toetsen.uitleg.iets", "vadsrijk.z3npc.x",
             "vadsrijk.mijn.giet.x"}


def laad(naam):
    with open(LANG / naam, encoding="utf-8") as f:
        return json.load(f)


def zonder_commentaar(t):
    t = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), t, flags=re.S)
    return re.sub(r"//[^\n]*", "", t)


def main():
    nl, en = laad("nl_nl.json"), laad("en_us.json")
    families = {}
    for k in nl:
        parts = k.split(".")
        for i in range(2, len(parts)):
            families.setdefault(".".join(parts[:i]) + ".", set()).add(parts[i])
    gezocht = {}   # key -> set of where

    def eis(k, waar):
        if k not in SKIP_KEYS:
            gezocht.setdefault(k, set()).add(waar)

    for f in JAVA.rglob("*.java"):
        if f.name in SKIP_FILES:
            continue
        t = zonder_commentaar(f.read_text(encoding="utf-8"))
        rel = f.relative_to(JAVA).as_posix().split("/")[-1]
        for m in LIT.finditer(t):
            if not m.group(2):
                eis(m.group(1), rel)
        for m in (AREA.finditer(t) if rel not in SKIP_AREA_FILES else ()):
            if m.group(1) not in ("t", "x", "test"):
                eis("vadsrijk.kern.gebied." + m.group(1), rel + " (gebied)")
        consts = {m.group(1): m.group(2) for m in CONST.finditer(t)}
        for naam, prefix in consts.items():
            for m in re.finditer(r"\b" + naam + r'\s*\+\s*"([a-z0-9_]+(?:\.[a-z0-9_]+)*)"(\s*\+)?', t):
                if not m.group(2):
                    eis(prefix + m.group(1), rel)
            fam = families.get(prefix, set())
            for m in BARE.finditer(t):
                lit = m.group(1)
                if lit.startswith("vadsrijk.") or lit.split(".")[0] not in fam:
                    continue
                if t[m.end():m.end() + 3].lstrip().startswith("+"):
                    continue
                k = prefix + lit
                if k in nl:
                    continue
                # only when no other prefix of this file makes it a real key either
                if any(p + lit in nl for p in consts.values()):
                    continue
                # a literal that is a full family of its own (a prefix used with + elsewhere) is not a key
                if (k + ".") in families:
                    continue
                eis(k, rel + " (los)")

    if DATA.exists():
        for f in DATA.rglob("*.json"):
            try:
                data = json.loads(f.read_text(encoding="utf-8"))
            except Exception:
                continue
            stack = [data]
            while stack:
                o = stack.pop()
                if isinstance(o, dict):
                    stack.extend(o.values())
                elif isinstance(o, list):
                    stack.extend(o)
                elif isinstance(o, str) and re.fullmatch(KEY, o):
                    eis(o, f.relative_to(DATA).as_posix())

    mis = {k: w for k, w in gezocht.items() if k not in nl or k not in en}
    for k in sorted(mis):
        talen = ("" if k in nl else "nl ") + ("" if k in en else "en")
        print(f"ONTBREEKT [{talen.strip()}] {k}   <- {', '.join(sorted(mis[k])[:3])}")
    print(f"taalcheck: {len(gezocht)} sleutels gecontroleerd, {len(mis)} ontbreken")
    return 1 if mis else 0


if __name__ == "__main__":
    sys.exit(main())
