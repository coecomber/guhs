"""
W-SITE: copies the vanilla textures that Vadsrijk item models borrow (layer0 = "minecraft:item/...", e.g. vr_knuffelharnas ->
golden_chestplate) out of a Minecraft client jar into docs/wiki/vadsrijk_vanilla/<item_x>.png (committed, tiny), so the site has
an icon for every item without needing the jar in CI.

    python tools/wiki_site/vadsrijk/vanilla_iconen.py [--jar <minecraft client jar>]
Default jar: ~/.gradle/caches/neoformruntime/artifacts/minecraft_26.1.2_client.jar
"""
import argparse
import glob
import json
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jar", default=os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_26.1.2_client.jar"))
    a = ap.parse_args()
    modellen = os.path.join(ROOT, "src", "main", "resources", "assets", "guhs", "models", "item")
    nodig = set()
    for f in glob.glob(os.path.join(modellen, "vr_*.json")):
        try:
            tx = (json.load(open(f, encoding="utf-8")).get("textures") or {}).get("layer0", "")
        except ValueError:
            continue
        if tx.startswith("minecraft:") or (tx and ":" not in tx):
            nodig.add(tx.split(":")[-1])
    if not os.path.exists(a.jar):
        print("no client jar:", a.jar)
        return 1
    uit = os.path.join(ROOT, "docs", "wiki", "vadsrijk_vanilla")
    os.makedirs(uit, exist_ok=True)
    n = 0
    with zipfile.ZipFile(a.jar) as z:
        for pth in sorted(nodig):
            naam = "assets/minecraft/textures/" + pth + ".png"
            try:
                data = z.read(naam)
            except KeyError:
                print("  not in the jar:", naam)
                continue
            with open(os.path.join(uit, pth.replace("/", "_") + ".png"), "wb") as fh:
                fh.write(data)
            n += 1
    print(f"vanilla icons: {n} of {len(nodig)} -> {uit}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
