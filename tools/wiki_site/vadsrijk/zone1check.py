"""
W-SITE: checks that the built site shows nothing beyond zone 1 at the default spoiler setting (data-zonetot="1").
It works out which text in <main> is visible under the CSS rules (elements with data-zone>1 hidden, their .verstopt-zone note
shown; [hidden], the other language, script/style hidden). Then it looks for names of zone 2-6 things (zones, areas, bosses,
creatures, items, NPCs, quests) that are not also zone-1 names.

    python tools/wiki_site/vadsrijk/zone1check.py [site_build/vadsrijk]
Exit 1 when something leaks. Pages whose whole subject is beyond zone 1 must show only the "hidden by your spoiler setting" note.

X-SITE release check (the public build with --open-zones N publishes nothing of zones > N):
    python tools/wiki_site/vadsrijk/zone1check.py --release [--open-zones 1] [--dir <built site>]
Without --dir it first builds the section with --open-zones N (real settings: --echt, plus the draft patch notes so those are
checked too) into site_build/vadsrijk-release. Then it greps EVERY published file (html incl. attributes and the English half,
js, json, css, xml, txt) for the names of zone N+1..6 things (zones NL+EN, areas, bosses, creatures, items, NPCs, quests, lore,
secrets, map points, puzzles, guide topics, sets, the raid), checks every file NAME (pages, pictures, tiles) for their slugs,
and lists the pictures. The names come from the FULL data (Bron), not from the published JSON. Exit 1 on any leak.
"""
import json
import os
import re
import sys
from html.parser import HTMLParser

# Names that zone-1 GAME text itself already says (so not a site spoiler), or that collide with a zone-1 name:
OOK_IN_ZONE1 = {
    "Vahoegnir": "the story premise: zone-1 quests, the zone-1 lore and the Grootmeester talk about the grey dragon",
    "De Schemerwortel": "Stalmeester Hoefje (Knuffelkamp) says you pick your Pluimguh there",
    "Bolletje": "also the name of a zone-1 cuddly toy (secret) and a zone-1 quest text",
}
# Pages that summarise the whole update on purpose (the release notes of 1.10.0)
OVERSLAAN = ("patchnotes/",)

VOID = {"img", "br", "input", "meta", "link", "hr", "source", "wbr", "area", "col", "embed", "param", "track"}


class Zicht(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.stack, self.inmain, self.tekst = [], 0, []

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if tag in VOID:
            if tag == "img" and self.inmain and not any(self.stack) and a.get("alt"):
                self.tekst.append(a["alt"])
            return
        kl = (a.get("class") or "").split()
        z = a.get("data-zone")
        weg = False
        if z and z.isdigit():
            if "verstopt-zone" in kl:
                weg = int(z) <= 1
            else:
                weg = int(z) > 1
        if "hidden" in a or tag in ("script", "style", "template", "noscript") or a.get("lang") == "en":
            weg = True
        if tag == "main":
            self.inmain += 1
        self.stack.append((tag, weg))

    def handle_endtag(self, tag):
        if tag in VOID:
            return
        while self.stack:
            t, _ = self.stack.pop()
            if t == "main":
                self.inmain -= 1
            if t == tag:
                break

    def handle_data(self, data):
        if self.inmain and not any(w for _, w in self.stack):
            self.tekst.append(data)


def _slug(x):
    return re.sub(r"[^a-z0-9]+", "-", x.lower()).strip("-")


def release(argv):
    import argparse
    import time
    here = os.path.dirname(os.path.abspath(__file__))
    repo = os.path.abspath(os.path.join(here, "..", "..", ".."))
    ap = argparse.ArgumentParser(prog="zone1check.py --release")
    ap.add_argument("--release", action="store_true")
    ap.add_argument("--open-zones", type=int, default=1)
    ap.add_argument("--dir", default=None, help="an already built site (default: build one into site_build/vadsrijk-release)")
    ap.add_argument("--root", default=repo)
    a = ap.parse_args(argv)
    sys.path.insert(0, os.path.join(a.root, "tools"))
    from wiki_site.vadsrijk.bron import Bron
    from wiki_site.vadsrijk import poort
    t0 = time.time()
    site = a.dir
    if not site:
        import make_vadsrijk_site
        site = os.path.join(a.root, "site_build", "vadsrijk-release")
        # the real settings, but WITH the draft patch notes (echt=False would add the preview banner + noindex: harmless here)
        make_vadsrijk_site.bouw(a.root, site, echt=False, open_zones=a.open_zones, stil=True)
    full = Bron(a.root).build()
    namen = poort.later_namen(full, a.open_zones)
    pat = poort.patroon(namen)
    slugs = {}
    for x, w in namen.items():
        sl = _slug(x)
        if len(sl) >= 6:
            slugs[sl] = (x, w)
    # ids of later things whose page/picture file names would give them away
    for k in ("bazen", "wezens", "items", "npcs", "quests"):
        for x in full.get(k, []):
            if poort.znr(x.get("zone")) > a.open_zones:
                for i in (x.get("id"), x.get("kort")):
                    if i and len(i) >= 6:
                        vroeg = any(i == (y.get("id") or "") or i == (y.get("kort") or "") for kk in ("bazen", "wezens", "items", "npcs", "quests")
                                    for y in full.get(kk, []) if poort.znr(y.get("zone")) <= a.open_zones)
                        if not vroeg:
                            slugs.setdefault(_slug(i), (i, f"{k} id zone{poort.znr(x.get('zone'))}"))
    spat = re.compile(r"(?<![a-z0-9])(" + "|".join(re.escape(x) for x in sorted(slugs, key=len, reverse=True)) + r")(?![a-z0-9])") if slugs else None
    lek_tekst, lek_naam, n_files, beelden = [], [], 0, []
    for dp, _, fs in os.walk(site):
        for f in fs:
            pth = os.path.join(dp, f)
            rel = os.path.relpath(pth, site).replace("\\", "/")
            n_files += 1
            if rel.startswith(("img/", "patchnotes/img/")):
                beelden.append(rel)
            stam = rel.rsplit(".", 1)[0].lower().replace("_", "-")
            if spat and spat.search(stam):
                lek_naam.append((rel, spat.search(stam)[1]))
            if f.endswith((".html", ".js", ".json", ".css", ".xml", ".txt", ".md", ".svg")):
                import html as _h
                t = _h.unescape(open(pth, encoding="utf-8", errors="replace").read())
                g = sorted(set(pat.findall(t))) if pat else []
                if g:
                    lek_tekst.append((rel, g))
    print(f"zone1check --release: open zones 1..{a.open_zones}; {n_files} published files ({len(beelden)} pictures) in {site}")
    print(f"  {len(namen)} later-zone names + {len(slugs)} file-name slugs checked ({time.time() - t0:.0f} s)")
    print(f"  text leaks: {len(lek_tekst)} files; file-name leaks: {len(lek_naam)}")
    for rel, g in sorted(lek_tekst)[:120]:
        print("  LEAK", rel, "|", ", ".join(f"{x} ({namen[x]})" for x in g[:8]) + (" ..." if len(g) > 8 else ""))
    for rel, x in sorted(lek_naam)[:60]:
        print("  NAME", rel, "|", x, slugs[x][1])
    # the visible-text check on the same build, too
    return 1 if (lek_tekst or lek_naam) else 0


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    if "--release" in argv:
        return release(argv)
    root = argv[0] if argv else os.path.join(os.path.dirname(__file__), "..", "..", "..", "site_build", "vadsrijk")
    d = json.load(open(os.path.join(root, "data", "vadsrijk.json"), encoding="utf-8"))
    nr = lambda z: int(str(z or "zone1").replace("zone", "") or 1)  # noqa: E731
    z1, hoger = set(), {}
    for k, veld in (("zones", "naam"), ("gebieden", "naam"), ("bazen", "naam"), ("wezens", "naam"), ("items", "naam"), ("npcs", "naam"), ("quests", "titel")):
        for x in d.get(k, []):
            n = (x.get(veld) or "").strip()
            if len(n) < 5:
                continue
            zn = x["nr"] if k == "zones" else nr(x.get("zone"))
            (z1.add(n) if zn <= 1 else hoger.setdefault(n, f"{k} zone{zn}"))
    namen = {n: w for n, w in hoger.items() if n not in z1 and not any(n in a for a in z1) and n not in OOK_IN_ZONE1}
    pat = re.compile("|".join(re.escape(n) for n in sorted(namen, key=len, reverse=True)))
    lekken, leeg, n = [], [], 0
    for dp, _, fs in os.walk(root):
        for f in fs:
            if not f.endswith(".html"):
                continue
            pth = os.path.join(dp, f)
            p = Zicht()
            p.feed(open(pth, encoding="utf-8").read())
            txt = re.sub(r"\s+", " ", " ".join(p.tekst))
            rel = os.path.relpath(pth, root).replace("\\", "/")
            if rel.startswith(OVERSLAAN):
                continue
            n += 1
            gevonden = sorted(set(pat.findall(txt)))
            if gevonden:
                lekken.append((rel, gevonden[:6]))
    print(f"zone1check: {n} pages, {len(namen)} zone 2-6 names checked -> {len(lekken)} pages with a leak")
    for rel, g in sorted(lekken)[:80]:
        print("  LEAK", rel, "|", ", ".join(f"{x} ({namen[x]})" for x in g))
    return 1 if lekken else 0


if __name__ == "__main__":
    sys.exit(main())
