"""
Builds guhs.nl/vadsrijk: the Vadsrijk section of the Guhs site (wiki, world map, guide, highscores, profiles, live info, patch
notes), NL + EN, and checks every link and picture (exit 1 when something doesn't resolve).

Local preview (the default): sample live data + draft patch notes, into <root>/site_build/vadsrijk:
    python tools/make_vadsrijk_site.py
    python -m http.server 8000 -d site_build        ->  http://localhost:8000/vadsrijk/
The real site (what tools/make_wiki_site.py --vadsrijk does): live data from live.guhs.nl, no drafts:
    python tools/make_vadsrijk_site.py --out docs/site/vadsrijk --live-url https://live.guhs.nl/ --echt

Inputs (all in the repo): quests*.json, lang, POI_*.md, tools/features/vadsrijk*_npc_wiki.py, docs/patchnotes/*.md,
docs/wiki/vadsrijk_runtime.json (the mod's WikiExport dump) and docs/wiki/vadsrijk_kaart/ (the map tiles of
tools/wiki_site/vadsrijk/kaart.py). After the world layout moves: regenerate the map + POI files, then kaart.py, then the runtime
dump (runGameTestServer -Pgt=VadsrijkWeb), then this.
"""
import argparse
import json
import os
import shutil
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from wiki_site import check  # noqa: E402
from wiki_site.vadsrijk.bron import Bron  # noqa: E402
from wiki_site.vadsrijk.pages import Bouwer  # noqa: E402
from wiki_site.vadsrijk import voorbeeld  # noqa: E402


def bouw(root, out, live_url=None, echt=False, wiki_url="https://guhs.nl/wiki/", site_url="https://guhs.nl/", stil=False):
    """Builds the section into `out`; returns (pages written, notes). Used by make_wiki_site.py too."""
    t0 = time.time()
    root = os.path.abspath(root)
    out = os.path.abspath(out)
    bron = Bron(root)
    data = bron.build()
    from wiki_site.site import MOD_VERSION
    if os.path.isdir(out):
        for _ in range(5):   # (Windows: a file indexer/AV can hold a folder for a moment)
            shutil.rmtree(out, ignore_errors=True)
            if not os.path.exists(out):
                break
            time.sleep(1)
    os.makedirs(out, exist_ok=True)
    kaart_bron = os.path.join(root, "docs", "wiki", "vadsrijk_kaart")
    kaart = None
    if os.path.exists(os.path.join(kaart_bron, "kaart.json")):
        kaart = json.load(open(os.path.join(kaart_bron, "kaart.json"), encoding="utf-8"))
        shutil.copytree(os.path.join(kaart_bron, "tegels"), os.path.join(out, "kaart", "tegels"), dirs_exist_ok=True)
    else:
        bron.notes.append("no map tiles (docs/wiki/vadsrijk_kaart): run python tools/wiki_site/vadsrijk/kaart.py")
    voorbeeld_aan = not echt and not live_url
    b = Bouwer(data, root, out, live_url or "@live-voorbeeld/", voorbeeld_aan, wiki_url, site_url, kaart_bron, concepten=not echt, versie=MOD_VERSION)
    b.kaart = kaart
    pages = b.alles()
    if voorbeeld_aan:
        voorbeeld.schrijf(os.path.join(out, "live-voorbeeld"), data)
    res = check.check(out)
    size = sum(os.path.getsize(os.path.join(d, f)) for d, _, fs in os.walk(out) for f in fs)
    if not stil:
        print(f"Vadsrijk site -> {out}  ({time.time() - t0:.0f} s)")
        print(f"  pages: {len(pages)}; quests {len(data['quests'])}, npcs {len(data['npcs'])}, creatures {len(data['wezens'])}, bosses {len(data['bazen'])}, "
              f"items {len(data['items'])} ({len(b.iconen)} with an icon), map points {len(data['punten'])}, secrets counted {sum(data['geheimen'].values())}")
        print(f"  runtime dump: {'yes' if data['meta']['runtime'] else 'NO'}; map tiles: {'yes' if kaart else 'NO'}; live: {'sample data' if voorbeeld_aan else live_url}")
        print(f"  links checked: {res['links']} links + {res['images']} pictures -> {len(res['broken'])} broken; size {size / 1e6:.1f} MB")
        for n in bron.notes + b.notities:
            print("  note:", n)
        for x in res["broken"][:40]:
            print("  BROKEN:", *x)
    return pages, res


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=os.path.dirname(HERE))
    ap.add_argument("--out", default=None, help="default: <root>/site_build/vadsrijk")
    ap.add_argument("--live-url", default=None, help="where the live JSON is (https://live.guhs.nl/); default: sample data")
    ap.add_argument("--echt", action="store_true", help="the real site: no drafts, no sample data")
    a = ap.parse_args()
    out = a.out or os.path.join(a.root, "site_build", "vadsrijk")
    _, res = bouw(a.root, out, a.live_url, a.echt)
    return 1 if res["broken"] else 0


if __name__ == "__main__":
    sys.exit(main())
