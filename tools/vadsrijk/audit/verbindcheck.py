"""U-HEKKEN: the connection check over all Vadsrijk map tiles -> AUDIT_VERBIND.md (+ a summary on stdout).

Counts what the connection pass (tools/vadsrijk/verbind.py) would still change, i.e. connecting blocks that are NOT
connected the way the game would connect them:
  * los        a fence/wall/pane/bars block that misses a connection to a connectable neighbour (the user's "loose post");
  * fout       a connection state that is wrong the other way (connected to nothing, wrong wall height/post);
  * as         a chain/rope or garland (vlaggetjes, feestslingers) with the wrong axis for its line;
  * gat        a diagonal step or 1-up step in a LINE that is not bridged.
After `maak_kaart` (which runs the pass) all four must be ~0. Run: python tools/vadsrijk/audit/verbindcheck.py [--alleen tx,tz;...]
Exit code 0 always (it reports; integration decides).
"""
import argparse
import collections
import os
import sys
import time

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HIER, ".."))
sys.path.insert(0, HIER)
import verbind as V  # noqa: E402
import tegellezer as TL  # noqa: E402

ROOT = V.ROOT
UIT = os.path.join(ROOT, "AUDIT_VERBIND.md")


def soort_fout(oud, nieuw, waarom):
    if waarom == "gat":
        return "gat"
    no, po = V.splits(oud)
    nn, pn = V.splits(nieuw)
    if "axis" in pn and po.get("axis") != pn.get("axis"):
        return "as"
    for d in V.NAAM:
        a, b = po.get(d, "false"), pn.get(d)
        if b is None:
            continue
        if a in ("false", "none") and b not in ("false", "none"):
            return "los"
    return "fout"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--alleen", default=None)
    ap.add_argument("--map", default=V.UIT)
    ap.add_argument("--werkers", type=int, default=6)
    ap.add_argument("--geen-md", action="store_true")
    a = ap.parse_args()
    t0 = time.time()
    alle = sorted((tx, tz) for tx, tz, _ in TL.tegels(a.map))
    if a.alleen:
        kies = {tuple(int(v) for v in s.split(",")) for s in a.alleen.split(";") if s.strip()}
        alle = [t for t in alle if t in kies]
    uit = V._ronde(a.map, alle, False, a.werkers, monsters=True)
    tel = collections.Counter()
    per_blok = collections.Counter()
    voorbeelden = collections.defaultdict(list)
    per_tegel = collections.Counter()
    for (t, n, rand, proef) in uit:
        for (x, y, z, oud, nieuw, waarom) in proef:
            s = soort_fout(oud, nieuw, waarom)
            tel[s] += 1
            per_tegel[t] += 1
            blok = (nieuw if waarom == "gat" else oud).split("[")[0]
            per_blok[(s, blok)] += 1
            if len(voorbeelden[s]) < 25:
                voorbeelden[s].append((x, y, z, oud, nieuw))
    totaal = sum(tel.values())
    print(f"verbindcheck: {len(alle)} tegels, los {tel['los']}, fout {tel['fout']}, as {tel['as']}, gat {tel['gat']} "
          f"(totaal {totaal}) ({time.time() - t0:.0f} s)", flush=True)
    if a.geen_md:
        return
    r = ["# AUDIT_VERBIND - connecting blocks in the Vadsrijk tiles", "",
         "**GENERATED** by `tools/vadsrijk/audit/verbindcheck.py` (runs with `maak_kaart --audit`). Counts what the "
         "connection pass `tools/vadsrijk/verbind.py` would still change; after a regen everything must be ~0. "
         "Rules: `tools/vadsrijk/LEESMIJ_BOUWREGELS.md`.", "",
         f"Tiles: {len(alle)}. Total open: **{totaal}**.", "",
         "| kind | count | meaning |", "|---|---|---|",
         f"| los | {tel['los']} | fence/wall/pane misses a connection to a connectable neighbour (loose post) |",
         f"| fout | {tel['fout']} | wrong state the other way (connected to nothing, wall height/post) |",
         f"| as | {tel['as']} | chain/rope/garland with the wrong axis |",
         f"| gat | {tel['gat']} | diagonal or 1-up step in a line not bridged |", ""]
    if per_blok:
        r += ["## Per block", "", "| kind | block | count |", "|---|---|---|"]
        r += [f"| {s} | `{b}` | {n} |" for (s, b), n in per_blok.most_common(40)] + [""]
    for s in ("los", "fout", "as", "gat"):
        if voorbeelden[s]:
            r += [f"## Examples: {s}", "", "| x y z | now | should be |", "|---|---|---|"]
            r += [f"| {x} {y} {z} | `{o}` | `{n}` |" for (x, y, z, o, n) in voorbeelden[s]] + [""]
    if per_tegel:
        r += ["## Worst tiles", "", "| tile | open |", "|---|---|"]
        r += [f"| {t[0]},{t[1]} | {n} |" for t, n in per_tegel.most_common(15)] + [""]
    with open(UIT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(r))


if __name__ == "__main__":
    main()
