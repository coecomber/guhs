"""
W-SITE: checks that the built site shows nothing beyond zone 1 at the default spoiler setting (data-zonetot="1").
It works out which text in <main> is visible under the CSS rules (elements with data-zone>1 hidden, their .verstopt-zone note
shown; [hidden], the other language, script/style hidden). Then it looks for names of zone 2-6 things (zones, areas, bosses,
creatures, items, NPCs, quests) that are not also zone-1 names.

    python tools/wiki_site/vadsrijk/zone1check.py [site_build/vadsrijk]
Exit 1 when something leaks. Pages whose whole subject is beyond zone 1 must show only the "hidden by your spoiler setting" note.
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


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "..", "..", "site_build", "vadsrijk")
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
