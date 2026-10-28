"""
1.8.0 (Guhdromenland): droomzand and the dream blocks (dromen-ontwerp/BOUWPLAN_180.md, D). Java: feature/droomblokken
(DroomblokkenFeature, DroomBlokken, DroomblokkenGameTests).

  build(h)    textures (from the concept textures of the dream renders, dromen-ontwerp/_bron/concept_blokken.py; animated
              where the block moves: zeepbelglas, droomzandblok, droomzee), models, blockstates, loot tables, recipes,
              tags and texts (Dutch in nl_nl, English in tools/lang/en/c212_droomblokken.json), plus a self-check

Reused, NOT made again: sterrenhemel (= guhs:sterrenplafond), maansteen (+tegels/plaat/trap), zeepbel.

Standalone (from the project root): python tools/features/droomblokken.py   (writes only its own files, including the 26.1
item definitions; a full tools/mc26.py run is not needed)
"""
import json
import os
import sys

import numpy as np
from PIL import Image

BLOKKEN = ["zeepbelglas", "droomzandblok", "slaapliedjesgras", "maanbloemetje", "droomzee"]

# key -> (Dutch, English)
TEKSTEN = {
    "item.guhs.droomzand": ("Droomzand", "Dream Sand"),
    "item.guhs.droomzand.lore": ("Glinstert als een slaperig sterretje. De Zandguh zegt: niet in je ogen wrijven, vads!",
                                 "Glitters like a sleepy little star. The Sand Guh says: don't rub it in your eyes, vads!"),
    "block.guhs.zeepbelglas": ("Zeepbelglas", "Bubble Glass"),
    "block.guhs.zeepbelglas.lore": ("Doorzichtig, met alle kleuren van een zeepbel. Niet prikken, njeg!",
                                    "See-through, with every colour of a soap bubble. No poking, njeg!"),
    "block.guhs.droomzandblok": ("Droomzandblok", "Block of Dream Sand"),
    "block.guhs.droomzandblok.lore": ("Negen droomzandjes in één blok. De glittertjes dwarrelen omhoog, want ze willen terug naar de sterren.",
                                      "Nine dream sands in one block. The glitter drifts up: it wants to go back to the stars."),
    "block.guhs.slaapliedjesgras": ("Slaapliedjesgras", "Lullaby Grass"),
    "block.guhs.slaapliedjesgras.lore": ("Neuriet zachtjes als je eroverheen loopt. Op je tenen? Dan slaapt het verder.",
                                         "Hums softly when you walk over it. On tiptoes? Then it keeps sleeping."),
    "block.guhs.maanbloemetje": ("Maanbloemetje", "Moon Flower"),
    "block.guhs.maanbloemetje.lore": ("Gaat alleen 's nachts open en gloeit dan zachtjes. Overdag doet het een dutje, net als een vadsige guh.",
                                      "Only opens at night, and then glows softly. By day it naps, just like a vadsig guh."),
    "block.guhs.droomzee": ("Droomzee", "Dream Sea"),
    "block.guhs.droomzee.lore": ("De zachte zee onder de droomeilandjes. Je zakt er heel langzaam doorheen, als door een wolkje.",
                                 "The soft sea under the dream islands. You sink through it ever so slowly, like through a little cloud."),
}

rng = np.random.default_rng(1808)


# ------------------------------------------------------------------------------------------------ textures (16x16 RGBA)
def _ruis(base, amp, a=255):
    t = np.zeros((16, 16, 4), np.float32)
    t[..., :3] = base
    t[..., :3] += rng.normal(0, amp, (16, 16, 1))
    t[..., 3] = a
    return t


def _leeg():
    return np.zeros((16, 16, 4), np.float32)


def _px(t, x, y, c, a=255):
    if 0 <= x < 16 and 0 <= y < 16:
        t[y, x, :3] = c
        t[y, x, 3] = a


def _img(t):
    return Image.fromarray(np.clip(t, 0, 255).astype(np.uint8), "RGBA")


def _strip(frames):
    """An animated texture: the frames stacked vertically (16 x 16n)."""
    out = Image.new("RGBA", (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        out.paste(_img(f), (0, 16 * i))
    return out


def zeepbelglas(fase):
    t = _leeg()
    for y in range(16):
        for x in range(16):
            h = ((x + y) / 30.0 + 0.15 * np.sin(x / 2.5) + fase) % 1.0
            t[y, x, :3] = np.array([0.5 + 0.5 * np.cos(2 * np.pi * (h + k / 3)) for k in range(3)]) * 70 + 185
            t[y, x, 3] = 95
    t[0, :, 3] = t[15, :, 3] = t[:, 0, 3] = t[:, 15, 3] = 150
    for (x, y) in ((3, 3), (4, 3), (3, 4), (11, 10)):  # glansje
        _px(t, x, y, (255, 255, 255), 220)
    return t


def droomzand_basis():
    return _ruis((206, 210, 232), 8)


def droomzand_frame(basis, seed):
    t = basis.copy()
    r = np.random.default_rng(seed)
    for _ in range(14):
        x, y = r.integers(0, 16, 2)
        _px(t, x, y, [(255, 255, 255), (255, 236, 250), (236, 246, 255), (255, 246, 214)][r.integers(0, 4)])
    return t


def droomzee(fase):
    t = _ruis((150, 140, 224), 5, a=170)
    for y in (3, 9, 14):
        for x in range(16):
            if (x + y + fase) % 7 < 3:
                _px(t, x, y, (214, 206, 255), 200)
    r = np.random.default_rng(40 + fase)
    for _ in range(4):
        x, y = r.integers(0, 16, 2)
        _px(t, x, y, (255, 246, 255), 230)
    return t


def gras_top():
    t = _ruis((178, 168, 230), 7)
    for _ in range(18):
        x, y = rng.integers(0, 16, 2)
        t[y, x, :3] += rng.choice([-20, 18])
    for (x, y) in ((10, 4), (10, 5), (10, 6), (9, 7), (10, 7), (11, 3)):  # muzieknootje
        _px(t, x, y, (255, 246, 214))
    for (x, y) in ((4, 10), (4, 11), (4, 12), (3, 13), (4, 13), (5, 9)):  # nog eentje
        _px(t, x, y, (255, 226, 240))
    return t


def gras_zij(pluis, top):
    t = pluis.copy()
    t[..., :3] = t[..., :3] * 0.85 + np.array((206, 196, 236)) * 0.15
    for x in range(16):
        h = 3 + (x * 7 % 3)
        t[:h, x] = top[:h, x]
    return t


def maanbloemetje(open_):
    t = _leeg()
    steel = (120, 170, 150)
    for y in range(8, 16):
        _px(t, 7 + (y % 3 == 0), y, steel)
    _px(t, 5, 12, steel)
    _px(t, 6, 11, steel)
    if open_:
        cx, cy, r = 7, 5, 3.2
        for y in range(16):
            for x in range(16):
                d = np.hypot(x - cx, y - cy)
                d2 = np.hypot(x - cx - 1.6, y - cy + 1.0)
                if d <= r and d2 > r * 0.8:  # sikkelmaantje
                    _px(t, x, y, (255, 244, 196))
        for (x, y) in ((11, 3), (3, 8), (12, 9)):
            _px(t, x, y, (255, 255, 236))
    else:  # dicht knopje, met een zilver puntje
        for (x, y) in ((7, 5), (8, 5), (7, 6), (8, 6), (7, 7), (8, 7), (7, 4)):
            _px(t, x, y, (196, 206, 170))
        _px(t, 8, 4, (236, 236, 214))
    return t


def droomzand_item():
    t = _leeg()
    hoopje = [(x, y) for y in range(8, 15) for x in range(2, 14) if (x - 7.5) ** 2 / 36 + (y - 14.5) ** 2 / 40 <= 1]
    for (x, y) in hoopje:
        v = rng.normal(0, 9)
        _px(t, x, y, np.array((210, 214, 236)) + v)
    for (x, y) in ((5, 10), (9, 9), (11, 12), (7, 12), (4, 13)):
        _px(t, x, y, (255, 255, 255))
    for (x, y, c) in ((6, 5, (255, 246, 214)), (10, 3, (255, 236, 250)), (3, 7, (236, 246, 255)), (12, 6, (255, 255, 255))):
        _px(t, x, y, c)  # opdwarrelende glittertjes
    return t


# ------------------------------------------------------------------------------------------------ build
def _anim(h, naam, frames, frametime, interpolate=True):
    h.save(_strip(frames), "block", f"{naam}.png")
    pad = os.path.join(h.TEX, "block", f"{naam}.png.mcmeta")
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"animation": {"frametime": frametime, "interpolate": interpolate}}, f, indent=2)
        f.write("\n")


def build(h):
    A, D, w = h.A, h.D, h.w
    pluis = np.array(Image.open(os.path.join(h.TEX, "block", "pluiswolblok.png")).convert("RGBA"), np.float32)[:16, :16]

    # zeepbelglas: kleuren schuiven langzaam
    _anim(h, "zeepbelglas", [zeepbelglas(i / 8) for i in range(8)], 6)
    w(f"{A}/models/block/zeepbelglas.json", {"parent": "minecraft:block/cube_all",
                                             "textures": {"all": {"sprite": "guhs:block/zeepbelglas", "force_translucent": True}}})
    # droomzandblok: glittertjes springen
    basis = droomzand_basis()
    _anim(h, "droomzandblok", [droomzand_frame(basis, 70 + i) for i in range(4)], 8, False)
    w(f"{A}/models/block/droomzandblok.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/droomzandblok"}})
    # droomzee: golfjes schuiven
    _anim(h, "droomzee", [droomzee(i) for i in range(7)], 5)
    w(f"{A}/models/block/droomzee.json", {"parent": "minecraft:block/cube_all",
                                          "textures": {"all": {"sprite": "guhs:block/droomzee", "force_translucent": True}}})
    # slaapliedjesgras
    top = gras_top()
    h.save(_img(top), "block", "slaapliedjesgras_top.png")
    h.save(_img(gras_zij(pluis, top)), "block", "slaapliedjesgras_zij.png")
    w(f"{A}/models/block/slaapliedjesgras.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/slaapliedjesgras_top", "side": "guhs:block/slaapliedjesgras_zij", "bottom": "guhs:block/pluiswolblok"}})
    # maanbloemetje: open ('s nachts) en dicht (knopje)
    for staat, naam in ((True, "maanbloemetje_open"), (False, "maanbloemetje_dicht")):
        h.save(_img(maanbloemetje(staat)), "block", f"{naam}.png")
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/cross",
                                            "textures": {"cross": f"guhs:block/{naam}"}})
    w(f"{A}/blockstates/maanbloemetje.json", {"variants": {"open=true": {"model": "guhs:block/maanbloemetje_open"},
                                                           "open=false": {"model": "guhs:block/maanbloemetje_dicht"}}})
    h.item_model("maanbloemetje", "guhs:block/maanbloemetje_open")
    for b in ("zeepbelglas", "droomzandblok", "droomzee", "slaapliedjesgras"):
        w(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"guhs:block/{b}"}}})
        w(f"{A}/models/item/{b}.json", {"parent": f"guhs:block/{b}"})
    # droomzand-item
    h.save(_img(droomzand_item()), "item", "droomzand.png")
    h.item_model("droomzand")

    # loot: alles laat zichzelf vallen, behalve de droomzee (hoort bij de droom)
    for b in ("zeepbelglas", "droomzandblok", "slaapliedjesgras", "maanbloemetje"):
        h.self_drop(b)

    # recepten
    h.shaped("droomzandblok", ["ZZZ", "ZZZ", "ZZZ"], {"Z": "guhs:droomzand"}, "guhs:droomzandblok", 1)
    h.shapeless("droomzand_uit_blok", ["guhs:droomzandblok"], "guhs:droomzand", 9)
    h.shaped("zeepbelglas", ["GGG", "GZG", "GGG"], {"G": "minecraft:glass", "Z": "guhs:droomzand"}, "guhs:zeepbelglas", 8)
    h.shapeless("slaapliedjesgras", ["minecraft:grass_block"] * 4 + ["guhs:droomzand"], "guhs:slaapliedjesgras", 4)
    h.shapeless("maanbloemetje", ["#minecraft:small_flowers", "guhs:droomzand"], "guhs:maanbloemetje", 2)

    # tags
    h.add_tag("minecraft/tags/block/mineable/shovel", ["guhs:droomzandblok", "guhs:slaapliedjesgras"])
    h.add_tag("minecraft/tags/block/dirt", ["guhs:slaapliedjesgras"])
    h.add_tag("minecraft/tags/block/impermeable", ["guhs:zeepbelglas"])

    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    selfcheck(h)


def selfcheck(h):
    problems = []
    for b in BLOKKEN:
        for p in (f"{h.A}/blockstates/{b}.json", f"{h.A}/models/item/{b}.json"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
        if f"block.guhs.{b}" not in TEKSTEN or f"block.guhs.{b}.lore" not in TEKSTEN:
            problems.append(f"no text for {b}")
    for r in ("droomzandblok", "droomzand_uit_blok", "zeepbelglas", "slaapliedjesgras", "maanbloemetje"):
        if not os.path.exists(f"{h.D}/recipe/{r}.json"):
            problems.append(f"missing recipe {r}")
    for k, (nl, en) in TEKSTEN.items():
        if nl.count("%s") != en.count("%s"):
            problems.append(f"%s count differs: {k}")
    if problems:
        raise SystemExit("droomblokken self-check failed:\n  " + "\n  ".join(problems))


def _direct():
    """Standalone: only its own files (nl_nl gets the Dutch texts, en_us the English ones; the English also goes into
    tools/lang/en/c212_droomblokken.json so a full make_resources run keeps it)."""
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    en_pad = os.path.join("tools", "lang", "en", "c212_droomblokken.json")
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
    _mc26()
    print(f"droomblokken: droomzand + {len(BLOKKEN)} blocks, {len(TEKSTEN)} texts")


def _mc26():
    """Only its own items in their 26.1 shape (client item definitions; tools/mc26.py does this for the whole tree)."""
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in ["droomzand"] + BLOKKEN:
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, f, indent=2)
            f.write("\n")


if __name__ == "__main__":
    _direct()
