"""
1.8.0 (playtest 2): the droomweer's droomkistje. Java: feature/droomweer (DroomweerFeature, DroomKistje, DroomKistjeBlock,
client/DroomweerClient). The droomweer itself (the ninth guh-weertje, its weerpotje and texts) lives in tools/features/weer.py.

  build(h)    the droomkistje (a lavender chest with stars and a moon clasp; facing like a chest), its texts (Dutch in nl_nl,
              English in tools/lang/en/c215_droomweer.json)

Standalone (from the project root): PYTHONPATH=tools python tools/features/droomweer.py
"""
import json
import os
import sys

from PIL import Image, ImageDraw

BLOK = "droomkistje"
T = "gui.guhs.droomweer.kistje."

# key -> (Dutch, English)
TEKSTEN = {
    f"block.guhs.{BLOK}": ("Droomkistje", "Dream Chest"),
    f"block.guhs.{BLOK}.lore": ("Staat bovenaan het Sterrenkussenveld, bij de grote ster. Eén keer per dag mag je erin kijken!",
                                "Stands at the top of the Star Cushion Field, by the big star. You may peek inside once a day!"),
    T + "morgen": ("Kom morgen terug, vadsje! Het kistje doet een dutje.", "Come back tomorrow, vadsje! The chest is taking a nap."),
    T + "eerste": ("In het droomkistje zat een weerpotje: Droomweer! Maak het open en je Weer vadser kan dromen. VAHOEG!",
                   "The dream chest held a weather jar: Dream Weather! Open it and your Weather Vadser can dream. VAHOEG!"),
    T + "eerste.titel": ("Droomweer!", "Dream Weather!"),
    T + "eerste.sub": ("Een stukje Guhdromenland, voor thuis", "A piece of Guh Dreamland, for at home"),
    T + "cadeau": ("Het droomkistje gaf je droomzand en %s. Tot morgen, vadsje!", "The dream chest gave you dream sand and %s. See you tomorrow, vadsje!"),
}

LAVENDEL, LAVENDEL_D, LAVENDEL_L = (176, 150, 226), (132, 104, 190), (212, 194, 246)
RAND, GOUD, GOUD_D, STER = (96, 70, 150), (255, 214, 110), (214, 166, 60), (255, 246, 196)


def _hout(seed):
    img = Image.new("RGBA", (16, 16), LAVENDEL + (255,))
    d = ImageDraw.Draw(img)
    for y in (4, 9, 14):
        d.line([0, y, 15, y], fill=LAVENDEL_D + (255,))
    for (x, y) in ((3, 2), (11, 6), (6, 11), (13, 12), (2, 7)):
        d.point((x, y), fill=LAVENDEL_L + (255,))
    d.rectangle([0, 0, 15, 15], outline=RAND + (255,))
    return img


def _sterretjes(img, punten):
    d = ImageDraw.Draw(img)
    for (x, y) in punten:
        d.point([(x, y), (x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)], fill=STER + (255,))
        d.point((x, y), fill=(255, 255, 255, 255))
    return img


def voorkant():
    img = _sterretjes(_hout(1), [(3, 3), (12, 11)])
    d = ImageDraw.Draw(img)
    d.line([0, 6, 15, 6], fill=RAND + (255,))                  # the lid's edge
    # the clasp: a little golden moon
    d.rectangle([6, 5, 9, 9], fill=GOUD + (255,), outline=GOUD_D + (255,))
    d.point([(8, 6), (8, 7)], fill=(255, 240, 180, 255))
    d.point((7, 8), fill=GOUD_D + (255,))
    return img


def zijkant():
    img = _sterretjes(_hout(2), [(4, 10), (11, 3)])
    ImageDraw.Draw(img).line([0, 6, 15, 6], fill=RAND + (255,))
    return img


def deksel():
    img = _sterretjes(_hout(3), [(4, 4), (11, 5), (7, 11), (12, 12)])
    d = ImageDraw.Draw(img)
    d.ellipse([5, 5, 10, 10], outline=GOUD + (255,))         # a little moon on the lid
    d.point([(6, 6), (6, 7)], fill=GOUD + (255,))
    return img


def build(h):
    A, w = h.A, h.w
    h.save(voorkant(), "block", f"{BLOK}_voor.png")
    h.save(zijkant(), "block", f"{BLOK}_zij.png")
    h.save(deksel(), "block", f"{BLOK}_deksel.png")
    tx = {"voor": f"guhs:block/{BLOK}_voor", "zij": f"guhs:block/{BLOK}_zij", "deksel": f"guhs:block/{BLOK}_deksel",
          "particle": f"guhs:block/{BLOK}_zij"}
    elementen = [{"from": [1, 0, 1], "to": [15, 14, 15], "faces": {
        "north": {"uv": [1, 2, 15, 16], "texture": "#voor"}, "south": {"uv": [1, 2, 15, 16], "texture": "#zij"},
        "east": {"uv": [1, 2, 15, 16], "texture": "#zij"}, "west": {"uv": [1, 2, 15, 16], "texture": "#zij"},
        "up": {"uv": [1, 1, 15, 15], "texture": "#deksel"}, "down": {"uv": [1, 1, 15, 15], "texture": "#zij"}}}]
    h.furniture_model(BLOK, elementen, tx)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c215_droomweer.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    with open(os.path.join(a, BLOK + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{BLOK}"}}, f, indent=2)
        f.write("\n")
    print(f"droomweer: droomkistje, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
