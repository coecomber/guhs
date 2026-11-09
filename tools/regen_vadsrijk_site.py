"""
W-SITE: ONE command that regenerates everything guhs.nl/vadsrijk is built from, then builds the site. Run it in the tree
you release from (after the last model/zone/quest change):

    python tools/regen_vadsrijk_site.py                  renders (only changed models) + vanilla icons + map + site
    python tools/regen_vadsrijk_site.py --dump           + first the runtime dump (gametest VadsrijkWeb via gradle_slot.sh)
    python tools/regen_vadsrijk_site.py --alles          re-render every creature/boss (not only changed ones)
    python tools/regen_vadsrijk_site.py --tegels <dir>   map tiles from another folder (default: the tree's committed .nbt tiles)

Steps (each one is skipped with a note when its input is missing):
 1. [--dump] gradle runGameTestServer -Pgt=VadsrijkWeb (JAVA_HOME = the Prism JDK, GUHS_SLOTS=3), then copy
    run/vadsrijk_runtime.json -> docs/wiki/vadsrijk_runtime.json. Refuses in v-int (the user's client tree: no gradle there).
 2. tools/wiki_site/vadsrijk/renders.py        creature/boss renders -> docs/wiki/vadsrijk_renders/ (needs the render pipeline
                                                renders-mounts/_bron + base-ontwerpen/_bron next to the repo; hash cache)
 3. tools/wiki_site/vadsrijk/vanilla_iconen.py  vanilla textures borrowed by item models -> docs/wiki/vadsrijk_vanilla/
 4. tools/wiki_site/vadsrijk/kaart.py           map tiles -> docs/wiki/vadsrijk_kaart/
 5. tools/make_vadsrijk_site.py                 the site -> site_build/vadsrijk (preview) or --echt / --out like that script
Afterwards: commit docs/wiki/vadsrijk_* (generated, small) with the release.
"""
import argparse
import os
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
PY = sys.executable


def stap(naam, cmd, **kw):
    print(f"\n== {naam}: {' '.join(cmd)}", flush=True)
    r = subprocess.run(cmd, cwd=ROOT, **kw)
    if r.returncode:
        print(f"!! {naam} ended with {r.returncode}")
    return r.returncode


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--dump", action="store_true", help="first regenerate the runtime dump (gradle gametest VadsrijkWeb)")
    ap.add_argument("--alles", action="store_true", help="re-render every creature/boss")
    ap.add_argument("--geen-renders", action="store_true", help="skip the renders (e.g. without the render pipeline)")
    ap.add_argument("--tegels", default=None, help="map tile folder for kaart.py")
    ap.add_argument("--echt", action="store_true", help="build the real site (no banner, indexable)")
    ap.add_argument("--out", default=None)
    ap.add_argument("--open-zones", default=None, help="X-SITE: passed to make_vadsrijk_site.py (default there: 1)")
    a = ap.parse_args()
    fouten = 0
    if a.dump:
        if os.path.basename(ROOT).lower() == "v-int":
            print("!! not in v-int (the user's client tree): run --dump in your own worktree and copy docs/wiki/vadsrijk_runtime.json")
            return 2
        slot = os.path.join(os.path.dirname(ROOT), "gradle_slot.sh")
        env = dict(os.environ, GUHS_SLOTS=os.environ.get("GUHS_SLOTS", "3"),
                   JAVA_HOME=os.path.join(os.environ.get("APPDATA", ""), "PrismLauncher", "java", "java-runtime-delta"))
        if stap("runtime dump", ["bash", slot.replace("\\", "/"), ROOT.replace("\\", "/"), "runGameTestServer", "-Pgt=VadsrijkWeb"], env=env) == 0:
            src = os.path.join(ROOT, "run", "vadsrijk_runtime.json")
            if not os.path.exists(src):
                src = os.path.join(ROOT, "run", "gametestserver", "vadsrijk_runtime.json")
            shutil.copyfile(src, os.path.join(ROOT, "docs", "wiki", "vadsrijk_runtime.json"))
            print("   copied", src, "-> docs/wiki/vadsrijk_runtime.json")
        else:
            fouten += 1
    if not a.geen_renders:
        fouten += bool(stap("renders", [PY, "tools/wiki_site/vadsrijk/renders.py"] + (["--alles"] if a.alles else [])))
    fouten += bool(stap("vanilla icons", [PY, "tools/wiki_site/vadsrijk/vanilla_iconen.py"]))
    fouten += bool(stap("map tiles", [PY, "tools/wiki_site/vadsrijk/kaart.py"] + (["--tegels", a.tegels] if a.tegels else [])))
    site = [PY, "tools/make_vadsrijk_site.py"] + (["--echt"] if a.echt else []) + (["--out", a.out] if a.out else []) + (["--open-zones", a.open_zones] if a.open_zones else [])
    rc = stap("site", site)
    print(f"\nregen_vadsrijk_site: {'OK' if not (fouten or rc) else 'with problems (see above)'}")
    return rc or (1 if fouten else 0)


if __name__ == "__main__":
    sys.exit(main())
