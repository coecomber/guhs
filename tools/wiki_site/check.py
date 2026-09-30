"""The link checker: every internal link, picture, script and stylesheet of the site must resolve (and #fragments too)."""
import json
import os
import re
from urllib.parse import unquote

ATTR = re.compile(r'(?:href|src)="([^"]*)"')
ID = re.compile(r'\sid="([^"]+)"')


def check(out):
    html_files, all_files = [], set()
    for d, _, files in os.walk(out):
        for f in files:
            all_files.add(os.path.normcase(os.path.normpath(os.path.join(d, f))))
            if f.endswith(".html"):
                html_files.append(os.path.join(d, f))
    exists = lambda p: os.path.normcase(os.path.normpath(p)) in all_files
    ids_cache = {}

    def ids_of(path):
        if path not in ids_cache:
            with open(path, encoding="utf-8") as fh:
                ids_cache[path] = set(ID.findall(fh.read()))
        return ids_cache[path]

    broken, n_links, n_imgs, external = [], 0, 0, set()
    for path in html_files:
        with open(path, encoding="utf-8") as fh:
            text = fh.read()
        base = out if os.path.basename(path) == "404.html" else os.path.dirname(path)   # 404.html sets its own <base>
        for url in ATTR.findall(text):
            if not url or url.startswith(("http://", "https://", "mailto:", "data:", "javascript:")):
                if url.startswith("http"):
                    external.add(url.split("/")[2])
                continue
            if url.startswith("'+") or "'+" in url:     # inside the 404 page's little script
                continue
            target, _, frag = url.partition("#")
            target = unquote(target.split("?")[0])
            if url.startswith("#"):
                if frag and frag not in ids_of(path):
                    broken.append((os.path.relpath(path, out), url, "missing #id"))
                continue
            full = os.path.normpath(os.path.join(base, target))
            if not full.startswith(os.path.normpath(out)):
                broken.append((os.path.relpath(path, out), url, "outside the site"))
                continue
            if url.endswith((".webp", ".png", ".ico")):
                n_imgs += 1
            else:
                n_links += 1
            if not exists(full):
                broken.append((os.path.relpath(path, out), url, "missing file"))
            elif frag and full.endswith(".html") and frag not in ids_of(full):
                broken.append((os.path.relpath(path, out), url, "missing #id"))
    # the search index
    idx_file = os.path.join(out, "assets", "search-index.js")
    n_index = 0
    if os.path.exists(idx_file):
        with open(idx_file, encoding="utf-8") as fh:
            data = json.loads(fh.read().split("=", 1)[1].rstrip().rstrip(";"))
        for e in data:
            n_index += 1
            for key in ("u", "i"):
                if e.get(key) and not exists(os.path.join(out, e[key])):
                    broken.append(("assets/search-index.js", e[key], "missing file"))
    return dict(pages=len(html_files), links=n_links, images=n_imgs, index=n_index, broken=broken, external=sorted(external))
