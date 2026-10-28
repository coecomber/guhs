"""Checks every FTB Quests chapter of Guhs for references to assets that do not exist.

- every image (chapter images and custom icons, "guhs:textures/....png") must exist under assets/guhs/textures;
- the English twin that compat/FtbQuestsTaal.image looks for (<chapter>/en/<name>.png) is optional, but when the Dutch
  picture has text painted in (title/kop/slot) and a twin exists, it must have the same size;
- every guhs item used as an icon or task/reward item must have an item definition (items/<id>.json) or an item model.

Run from the guhs project root:  python tools/check_ftbquests_assets.py   (exit code 1 when something is missing)
"""
import glob
import os
import re
import sys

from PIL import Image

ASSETS = "src/main/resources/assets/guhs"
CHAPTERS = "src/main/resources/ftbquests"


def main():
    fouten = []
    bestanden = glob.glob(f"{CHAPTERS}/**/*.json5", recursive=True)
    for f in bestanden:
        tekst = open(f, encoding="utf-8").read()
        for ref in sorted(set(re.findall(r'"guhs:(textures/[^"]+\.png)"', tekst))):
            pad = f"{ASSETS}/{ref}"
            if not os.path.exists(pad):
                fouten.append(f"{f}: missing texture guhs:{ref}")
                continue
            map_, naam = os.path.split(pad)
            en = os.path.join(map_, "en", naam)
            if os.path.exists(en) and Image.open(en).size != Image.open(pad).size:
                fouten.append(f"{f}: English twin of guhs:{ref} has another size")
        for item in sorted(set(re.findall(r'id: "guhs:([a-z0-9_]+)"', tekst))):
            if not (os.path.exists(f"{ASSETS}/items/{item}.json") or os.path.exists(f"{ASSETS}/models/item/{item}.json")):
                fouten.append(f"{f}: no item definition for guhs:{item}")
    for r in fouten:
        print(r)
    print(f"ftbquests assets: {len(bestanden)} chapter files, {len(fouten)} problems")
    return 1 if fouten else 0


if __name__ == "__main__":
    sys.exit(main())
