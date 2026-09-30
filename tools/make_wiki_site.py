"""
Builds the site of https://guhs.nl/ (GitHub Pages) in docs/site/: the landing page at the root (tools/wiki_site/landing.py,
with 404.html that sends old wiki links on, CNAME, robots.txt and sitemap.xml) and the Guhs wiki as a multi-page static
site in docs/site/wiki/.

Usage (from the project root):
    python tools/wiki_renders.py docs/wiki/img      (only when the renders need to be redrawn)
    python tools/make_wiki_site.py [--base-url https://guhs.nl/wiki/] [--landing docs/site] [--out docs/site/wiki]
    python tools/make_wiki_site.py --no-landing --out docs/site --base-url https://<name>.github.io/guhs/   (only the wiki)

Everything comes from the project itself: the names, lists and numbers from the game data (lang, Java enums, worldgen,
loot tables, recipes, advancements, FTB quests), the texts from the one-page wiki (tools/make_wiki.py), the pictures from
docs/wiki/img. Run it again after the code changed and the site follows. The last step checks every link and picture;
the script exits with 1 when something doesn't resolve.
Requires: pillow (with WebP).
"""
import argparse
import collections
import os
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from wiki_site import check, kb  # noqa: E402
from wiki_site.gamedata import Game  # noqa: E402
from wiki_site.images import Images  # noqa: E402
from wiki_site.pages import Builder  # noqa: E402
from wiki_site.render import Renderer  # noqa: E402
from wiki_site.site import CAT_ORDER, CATEGORIES  # noqa: E402


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=os.path.dirname(HERE), help="the project root (default: the folder above tools/)")
    ap.add_argument("--out", default=None, help="the wiki's output folder (default: <landing>/wiki, or <root>/docs/site with --no-landing)")
    ap.add_argument("--landing", default=None, help="the site root with the landing page (default: <root>/docs/site)")
    ap.add_argument("--no-landing", action="store_true", help="only build the wiki (no landing page, CNAME or root 404)")
    ap.add_argument("--site-url", default=None, help="the public address of the site root (default: --base-url without its last folder)")
    ap.add_argument("--img", default=None, help="the renders of tools/wiki_renders.py (default: <root>/docs/wiki/img)")
    ap.add_argument("--base-url", default=os.environ.get("GUHS_WIKI_URL", "https://guhs.nl/wiki/"),
                    help="the public address of the site, for sitemap.xml (or set GUHS_WIKI_URL)")
    ap.add_argument("--verbose", action="store_true", help="list the knowledge-base chunks without a page and the leftover version numbers")
    args = ap.parse_args()
    root = os.path.abspath(args.root)
    landing_dir = None if args.no_landing else os.path.abspath(args.landing or os.path.join(root, "docs", "site"))
    out = os.path.abspath(args.out or (os.path.join(landing_dir, "wiki") if landing_dir else os.path.join(root, "docs", "site")))
    site_url = args.site_url or (args.base_url.rstrip("/").rsplit("/", 1)[0] + "/")
    img_src = os.path.abspath(args.img or os.path.join(root, "docs", "wiki", "img"))
    t0 = time.time()

    game = Game(root)
    chunks = kb.extract(root)
    images = Images(img_src, game.assets, os.path.join(out, "img"))
    builder = Builder(game, chunks, images)
    site = builder.build()
    os.makedirs(out, exist_ok=True)
    renderer = Renderer(site, builder, images, game, out, args.base_url)
    written = set(renderer.render_all())
    if landing_dir:
        from wiki_site.landing import Landing
        Landing(builder, images, site_url, os.path.relpath(out, landing_dir).replace("\\", "/")).write(landing_dir)
    n_img = images.write()
    # pages that no longer exist
    stale = 0
    for d, _, files in os.walk(out):
        for f in files:
            rel = os.path.relpath(os.path.join(d, f), out).replace("\\", "/")
            if f.endswith(".html") and rel not in written and rel != "404.html":
                os.remove(os.path.join(d, f))
                stale += 1

    res = check.check(landing_dir or out, index_dir=out)
    size = sum(os.path.getsize(os.path.join(d, f)) for d, _, fs in os.walk(out) for f in fs)
    counts = collections.Counter(p.cat for p in site.pages.values() if not p.id.endswith("/index") and p.cat != "home")
    print(f"Guhs wiki site -> {out}  ({time.time() - t0:.0f} s)")
    if landing_dir:
        print(f"  landing page -> {landing_dir} ({site_url}); wiki at {args.base_url}")
    print(f"  pages: {res['pages']} html files ({len(site.pages)} wiki pages + 404)")
    for cat in CAT_ORDER:
        print(f"    {CATEGORIES[cat][1]:<12} {counts[cat]:>4}")
    print(f"  knowledge base: {len(chunks)} chunks, {builder.report['assigned']} on a page, {builder.report['dropped']} left out on purpose, "
          f"{len(builder.report['unassigned'])} without a page")
    print(f"  pictures: {len(images.used)} + {len(images.thumbs)} thumbnails ({n_img} (re)written), "
          f"{len(images.generated)} icons made from textures")
    print(f"  search index: {res['index']} entries; sitemap: {len(written)} urls")
    print(f"  links checked: {res['links']} links + {res['images']} pictures -> {len(res['broken'])} broken")
    print(f"  size: {size / 1e6:.1f} MB; stale pages removed: {stale}")
    if renderer.missing_links:
        print(f"  links to pages that don't exist (written as plain text): {len(renderer.missing_links)}")
        for pid, owners in sorted(renderer.missing_links.items()):
            print(f"    {pid}  (from {', '.join(sorted(owners)[:3])}{'...' if len(owners) > 3 else ''})")
    for n in builder.report["notes"]:
        print("  note:", n)
    if args.verbose:
        for c, target in builder.report["unassigned"]:
            print(f"  no page: {c.id} {c.kind} {c.title_en!r} -> {target}")
        for v in sorted(set(renderer.version_left))[:200]:
            print("  version number left in a text:", v)
    for b in res["broken"][:50]:
        print("  BROKEN:", *b)
    return 1 if res["broken"] else 0


if __name__ == "__main__":
    sys.exit(main())
