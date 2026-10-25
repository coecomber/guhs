"""
1.7.0 "Onder de Wol" (design: MineScapeStory/ontwerp-guhs170-mijnen.md, ronde 41-43; Java: feature/mijnspellen): two of
the five cave minigames, each with its own coin, shop, highscores and clothes, in a cave hall under a little entrance hut
with a Wolkenliftje:

  Karretjes-wissels   the Wisselgrot, Conducteur Piepwiel (npc wisselguh), ertsbonnetjes; conducteurspetje;
                      building blocks bielzenhout (+ trap, plaat), ertsgruissteen, ertsgruis_bakstenen;
                      deco ertskarretje (to sit in), stationsklokje, seinlampje
  Kegelgrot           guh bowling with De Guhde (npc kegelguh), kegelknopen; bowlingshirtje + zweetbandje;
                      building blocks kegelbaanhout (+ trap, plaat), retrotegel, retrotegel_paars;
                      deco kegeltje, wolbalrek, neon_kegelbord

  build(h)  textures, models, blockstates, the 26.1 item definitions, loot, tags (mineable, stairs/slabs, guhs:loaned,
            guhs:knus/grijptickets), advancements, the NPC models and textures, the clothes textures and icons, the texts,
            and (mijnspellen_bouw) the hall / shaft / hut templates, their pools, structures, structure sets and biome tags.

On its own (like the slices since 1.5; a full make_resources is not safe on this tree), from the project root:

    python tools/features/mijnspellen.py

It writes only its own files (and adds to the shared tags idempotently), and merges its texts into nl_nl.json / en_us.json
(English overlay: tools/lang/en/c194_mijnspellen.json).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
if os.path.join(HERE, "..") not in sys.path:
    sys.path.insert(0, os.path.join(HERE, ".."))

from features import mijnspellen_bouw as bouw  # noqa: E402

ERTS_KLEUR = {"kolen": (50, 48, 56), "ijzer": (226, 192, 170), "goud": (250, 214, 60), "vads": (250, 120, 190),
              "kaasknabbel": (248, 160, 40), "diamant": (90, 230, 230)}
WIT, ROZE, ROZE_DONKER, MINT, LILA = (246, 242, 244), (250, 150, 196), (196, 74, 136), (150, 222, 196), (196, 160, 236)
HOUT, HOUT_DONKER = (120, 80, 52), (78, 50, 32)


# =====================================================================================================================
# textures
# =====================================================================================================================
def _clamp(c):
    return tuple(int(max(0, min(255, v))) for v in c)


def _img(base, var, seed, size=16):
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size, 4), np.uint8)
    noise = rng.integers(-var, var + 1, (size, size, 1))
    a[..., :3] = np.clip(np.array(base)[None, None, :] + noise, 0, 255)
    a[..., 3] = 255
    return a


def _planken(base, seed, verticaal=False, bout=None):
    a = _img(base, 8, seed)
    donker = _clamp(c * 0.72 for c in base)
    for i in range(16):
        if i % 4 == 3:
            if verticaal:
                a[:, i, :3] = donker
            else:
                a[i, :, :3] = donker
    rng = np.random.default_rng(seed + 1)
    for r in range(4):                                     # board ends, staggered
        x = int(rng.integers(0, 16))
        if verticaal:
            a[x, r * 4:r * 4 + 3, :3] = donker
        else:
            a[r * 4:r * 4 + 3, x, :3] = donker
    if bout:
        for (x, y) in ((2, 1), (13, 1), (2, 9), (13, 9)):
            a[y, x, :3] = bout
    return a


def _spikkels(base, kleuren, seed, n=14, var=10):
    a = _img(base, var, seed)
    rng = np.random.default_rng(seed + 7)
    for i in range(n):
        x, y = int(rng.integers(0, 15)), int(rng.integers(0, 15))
        c = kleuren[i % len(kleuren)]
        a[y, x, :3] = c
        if rng.random() < 0.5:
            a[y, x + 1, :3] = _clamp(v * 0.85 for v in c)
    return a


def _bakstenen(base, voeg, seed, spikkels=None):
    a = _img(base, 9, seed)
    for y in range(16):
        for x in range(16):
            rij = y // 4
            if y % 4 == 3 or (x + (4 if rij % 2 else 0)) % 8 == 7:
                a[y, x, :3] = voeg
    if spikkels:
        rng = np.random.default_rng(seed + 3)
        for i in range(8):
            x, y = int(rng.integers(0, 16)), int(rng.integers(0, 16))
            if y % 4 != 3:
                a[y, x, :3] = spikkels[i % len(spikkels)]
    return a


def _pijl(a, kleur):
    """An arrow pointing up (north in a top texture)."""
    for y in range(3, 13):
        a[y, 7:9, :3] = kleur
    for i in range(4):
        a[3 + i, 7 - i:9 + i, :3] = kleur


def _ruiten(k1, k2, seed, blok=4):
    a = _img(k1, 4, seed)
    b = _img(k2, 4, seed + 1)
    for y in range(16):
        for x in range(16):
            if (x // blok + y // blok) % 2:
                a[y, x] = b[y, x]
    return a


def _strepen(k1, k2, seed, breed=2, horizontaal=True):
    a = _img(k1, 5, seed)
    for i in range(16):
        if (i // breed) % 2:
            if horizontaal:
                a[i, :, :3] = _clamp(np.array(k2) + 0)
            else:
                a[:, i, :3] = k2
    return a


def _guhgezicht(a, x0, y0, s=1, boos=False):
    """A tiny guh face (8 x 6) at x0, y0."""
    oog = (34, 24, 52)
    for (x, y) in ((1, 1), (1, 2), (6, 1), (6, 2)):
        a[y0 + y, x0 + x, :3] = oog
    if boos:
        a[y0, x0, :3] = oog
        a[y0, x0 + 7, :3] = oog
    for (x, y) in ((0, 3), (7, 3)):
        a[y0 + y, x0 + x, :3] = (255, 150, 188)
    for (x, y) in ((3, 4), (4, 4)) if not boos else ((2, 5), (3, 4), (4, 4), (5, 5)):
        a[y0 + y, x0 + x, :3] = (150, 72, 116)


def textures(h):
    T = {}
    # --- Karretjes ---
    T["bielzenhout"] = _planken(HOUT, 11, bout=(150, 150, 160))
    T["ertsgruissteen"] = _spikkels((128, 124, 132), list(ERTS_KLEUR.values()), 12, n=18)
    T["ertsgruis_bakstenen"] = _bakstenen((136, 130, 138), (92, 88, 96), 13, spikkels=list(ERTS_KLEUR.values()))
    top = _planken(HOUT, 14)
    _pijl(top, ROZE)
    T["karretjes_spoorbegin_top"] = top
    T["karretjes_ertsbak"] = _planken((150, 104, 66), 15, verticaal=True, bout=(170, 170, 180))
    for i, (erts, kleur) in enumerate(ERTS_KLEUR.items()):
        lading = _spikkels(_clamp(c * 0.55 for c in kleur), [kleur, _clamp(c * 1.2 for c in kleur), (255, 255, 255)], 20 + i, n=40, var=14)
        T[f"karretjes_ertsbak_{erts}"] = lading
        voor = _planken((150, 104, 66), 30 + i, verticaal=True)
        voor[5:11, 2:14, :3] = kleur
        voor[5, 2:14, :3] = _clamp(c * 0.7 for c in kleur)
        voor[10, 2:14, :3] = _clamp(c * 0.7 for c in kleur)
        T[f"karretjes_ertsbak_voor_{erts}"] = voor
    T["ertskarretje"] = _img((112, 116, 126), 10, 40)
    for x in range(16):
        T["ertskarretje"][0, x, :3] = (80, 82, 92)
        T["ertskarretje"][15, x, :3] = (80, 82, 92)
    T["ertskarretje"][6:10, 5:11, :3] = ROZE                       # a pink guh heart on the side
    T["ertskarretje_lading"] = _spikkels((70, 66, 74), list(ERTS_KLEUR.values()), 41, n=50, var=12)
    klok = _img((40, 40, 48), 6, 42)
    for y in range(16):
        for x in range(16):
            d = math.dist((x + 0.5, y + 0.5), (8, 8))
            if d < 7:
                klok[y, x, :3] = (250, 248, 240) if d < 6 else ROZE
    klok[4:9, 8, :3] = (30, 30, 40)                                # five past guh
    klok[8, 8:11, :3] = (30, 30, 40)
    T["stationsklokje"] = klok
    T["stationsklokje_zij"] = _img((40, 40, 48), 6, 43)
    sein = _img((36, 36, 44), 5, 44)
    for y in range(16):
        for x in range(16):
            for cy, kleur in ((4.5, (255, 70, 80)), (11.5, (90, 240, 120))):
                if math.dist((x + 0.5, y + 0.5), (8, cy)) < 3.2:
                    sein[y, x, :3] = kleur
    T["seinlampje"] = sein
    # --- Kegelgrot ---
    T["kegelbaanhout"] = _planken((222, 188, 140), 50, verticaal=True)
    lijn = _planken((222, 188, 140), 51, verticaal=True)
    lijn[1:3, :, :3] = ROZE_DONKER
    _pijl(lijn, ROZE)
    T["kegelgrot_baanbegin"] = lijn
    T["retrotegel"] = _ruiten(ROZE, WIT, 52)
    T["retrotegel_paars"] = _ruiten(LILA, MINT, 53)
    goot = _img((214, 214, 222), 5, 54)
    goot[:, 6:10, :3] = (170, 170, 184)
    T["kegelgrot_goot"] = goot
    bumper = _img(ROZE, 8, 55)
    bumper[7:9, :, :3] = ROZE_DONKER
    T["kegelgrot_bumper"] = bumper
    T["kegelgrot_paaltje"] = _strepen(ROZE, WIT, 56, breed=3)
    T["kegelgrot_wolbal"] = _img((246, 120, 178), 14, 57)
    mika = _img((150, 80, 190), 10, 58)
    _guhgezicht(mika, 4, 5, boos=True)
    T["kegelgrot_mikablokje"] = mika
    pin = _img(WIT, 5, 59)
    pin[5:7, :, :3] = ROZE
    pin[8:10, :, :3] = ROZE
    T["kegeltje"] = pin
    kop = _img(WIT, 5, 60)
    _guhgezicht(kop, 4, 6)
    T["kegeltje_gezicht"] = kop
    T["wolbalrek"] = _planken((170, 110, 70), 61)
    T["wolbal_mint"] = _img(MINT, 12, 62)
    T["wolbal_lila"] = _img(LILA, 12, 63)
    neon = _img((26, 16, 36), 3, 64)
    for y in range(16):                                            # a neon pin (left) and ball (right)
        for x in range(16):
            if (x in (3, 6) and 4 <= y <= 12) or (y in (3, 13) and 3 <= x <= 6):
                neon[y, x, :3] = (255, 110, 200)
            if abs(math.dist((x + 0.5, y + 0.5), (11.5, 9.5)) - 2.5) < 0.6:
                neon[y, x, :3] = (110, 240, 255)
    T["neon_kegelbord"] = neon
    for name, a in T.items():
        h.save(Image.fromarray(a.astype(np.uint8)), "block", f"{name}.png")
    return T


ICONEN = {
    "ertsbonnetje": (["................", "..aaaaaaaaaaaa..", ".abbbbbbbbbbbba.", ".abccbbbbbbddba.", ".abccbbbbbbddba.",
                      ".abbbbbbbbbbbba.", ".abbeebbbbffbba.", ".abbeebbbbffbba.", ".abbbbbbbbbbbba.", "..aaaaaaaaaaaa..",
                      "................"],
                     {"a": (150, 100, 60), "b": (250, 236, 200), "c": ERTS_KLEUR["kolen"], "d": ERTS_KLEUR["goud"],
                      "e": ERTS_KLEUR["vads"], "f": ERTS_KLEUR["diamant"]}),
    "kegelknoop": (["................", ".....aaaaaa.....", "...aabbbbbbaa...", "..abbbcbbcbbba..", "..abbbbbbbbbba..",
                    ".abbcbbbbbbcbba.", ".abbbbbddbbbbba.", ".abbbbbddbbbbba.", ".abbcbbbbbbcbba.", "..abbbbbbbbbba..",
                    "..abbbcbbcbbba..", "...aabbbbbbaa...", ".....aaaaaa....."],
                   {"a": ROZE_DONKER, "b": ROZE, "c": (255, 230, 240), "d": WIT}),
    "kegelgrot_wolbal_leen": (["................", ".....aaaaaa.....", "...aabbbbbbaa...", "..abbbbbbbbbba..", "..abbcbbbbbbba..",
                               ".abbccbbbbbbbba.", ".abbbbbbbbbbbba.", ".abbbbbbbdbdbba.", ".abbbbbbbbbbbba.", "..abbbbbbbbbba..",
                               "..abbbbbbbbbba..", "...aabbbbbbaa...", ".....aaaaaa....."],
                              {"a": ROZE_DONKER, "b": (246, 120, 178), "c": (255, 210, 230), "d": (120, 40, 90)}),
    "karretjes_conducteurspetje": (["................", "................", ".....aaaaaa.....", "...aabbbbbbaa...", "..abbbbbbbbbba..",
                                    "..abbbbccbbbbba..", "..addddddddddda..", "..abbbbbbbbbbba..", ".eeeeeeeeeeeeeee.",
                                    "..eeeeeeeeeeee..", "................"],
                                   {"a": (24, 28, 60), "b": (48, 56, 110), "c": ROZE, "d": (240, 200, 70), "e": (30, 30, 40)}),
    "kegelgrot_bowlingshirtje": (["................", "...aa.....aa....", "..abbaaaaaccba..", ".abbbbdddccccba.", ".abbbbdddcccccba",
                                  ".aabbbdddcccaa..", "...abbdddcca....", "...abbdddcca....", "...abbdddcca....", "...abbdddcca....",
                                  "...abbdddcca....", "...aaaaaaaaa...."],
                                 {"a": (90, 60, 90), "b": MINT, "c": ROZE, "d": WIT}),
    "kegelgrot_zweetbandje": (["................", "................", "................", "....aaaaaaaa....", "..aabbbbbbbbaa..",
                               ".abbcbbcbbcbbba.", ".abbcbbcbbcbbba.", "..aabbbbbbbbaa..", "....aaaaaaaa....", "................"],
                              {"a": (150, 40, 120), "b": (240, 80, 180), "c": WIT}),
}


def _icoon(rijen, kleuren):
    a = np.zeros((16, 16, 4), np.uint8)
    top = (16 - len(rijen)) // 2
    for y, rij in enumerate(rijen):
        for x, ch in enumerate(rij[:16]):
            if ch in kleuren:
                a[top + y, x, :3] = kleuren[ch]
                a[top + y, x, 3] = 255
    return Image.fromarray(a)


def iconen(h):
    for name, (rijen, kleuren) in ICONEN.items():
        h.save(_icoon(rijen, kleuren), "item", f"{name}.png")


# =====================================================================================================================
# the clothes (the existing bones, our own textures) and the two NPCs
# =====================================================================================================================
def kleding(h):
    def herverf(bron, naam, verf):
        img = Image.open(os.path.join(h.TEX, "entity", "guh_clothes", f"{bron}.png")).convert("RGBA")
        a = np.asarray(img).copy()
        x0, y0, x1, y1 = img.getbbox()
        blok = a[y0:y1, x0:x1]
        verf(blok, np.random.default_rng(len(naam)))
        h.save(Image.fromarray(a), "entity", "guh_clothes", f"{naam}.png")

    def petje(b, rng):
        hgt, wdt = b.shape[:2]
        b[..., :3] = np.clip(np.array((44, 52, 104)) + rng.normal(0, 4, (hgt, wdt, 1)), 0, 255)
        b[hgt * 5 // 8:hgt * 6 // 8, :, :3] = (240, 200, 70)          # the gold band
        b[hgt // 4:hgt // 4 + 4, wdt // 2 - 3:wdt // 2 + 3, :3] = ROZE   # the badge
        b[..., 3] = 255

    def shirt(b, rng):
        hgt, wdt = b.shape[:2]
        for x in range(wdt):
            kleur = MINT if x < wdt * 0.42 else WIT if x < wdt * 0.58 else ROZE
            b[:, x, :3] = kleur
        b[..., :3] = np.clip(b[..., :3].astype(np.int32) + rng.normal(0, 3, (hgt, wdt, 1)).astype(np.int32), 0, 255)
        b[: hgt // 8, :, :3] = (90, 60, 90)                             # the collar
        b[..., 3] = 255

    def band(b, rng):
        hgt, wdt = b.shape[:2]
        for y in range(hgt):
            b[y, :, :3] = (240, 80, 180) if (y // 4) % 2 == 0 else WIT
        b[..., 3] = 255
    herverf("police_cap", "karretjes_conducteurspetje", petje)
    herverf("spelen_sportshirtje", "kegelgrot_bowlingshirtje", shirt)
    herverf("spelen_zweetbandje", "kegelgrot_zweetbandje", band)


def npcs(h):
    from features import sterrenwacht_hulp as hulp
    c = hulp.cube
    # Conducteur Piepwiel: a navy conductor's cap with a gold band and a pink badge, and a little whistle
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_wisselguh")
    sw = hulp.swatches(geo, ["pet", "band", "klep", "fluit"])
    geo["bones"].append({"name": "conducteur_pet", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-6.2, 25.4, -6.6], [12.4, 3.2, 11.4], sw["pet"]), c([-6.6, 25.0, -7.0], [13.2, 1.2, 12.2], sw["band"]),
        c([-5.0, 25.0, -9.8], [10.0, 0.6, 3.2], sw["klep"]), c([-1.0, 26.6, -7.2], [2.0, 1.4, 0.5], sw["fluit"])]})
    geo["bones"].append({"name": "conducteur_fluit", "parent": "body", "pivot": [0, 11, -4], "cubes": [
        c([-0.6, 9.4, -5.6], [1.2, 1.2, 1.4], sw["fluit"])]})
    hulp.save_geo(h, "guh_npc_wisselguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.93, sat=1.0, val=1.0)
    rng = np.random.default_rng(1707)
    hulp.paint_swatch(a, sw["pet"], (44, 52, 104), rng, 6)
    hulp.paint_swatch(a, sw["band"], (240, 200, 70), rng, 6)
    hulp.paint_swatch(a, sw["klep"], (30, 30, 40), rng, 4)
    hulp.paint_swatch(a, sw["fluit"], (220, 220, 230), rng, 6)
    h.save(Image.fromarray(a), "entity", "npc_wisselguh.png")
    # De Guhde: a pink-and-white sweatband, a mint-pink bowling shirt over his fluff ("the Guhde abides")
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_kegelguh")
    sw = hulp.swatches(geo, ["band", "shirt", "kraag"])
    geo["bones"].append({"name": "guhde_band", "parent": "head", "pivot": [0, 22, -1], "cubes": [
        c([-7.4, 22.0, -7.4], [14.8, 1.8, 13.6], sw["band"])]})
    geo["bones"].append({"name": "guhde_shirt", "parent": "body", "pivot": [0, 7, 0], "cubes": [
        c([-4.8, 3.2, -4.4], [9.6, 8.4, 8.8], sw["shirt"], inflate=0.1), c([-3.6, 11.2, -4.6], [7.2, 1.2, 1.0], sw["kraag"])]})
    hulp.save_geo(h, "guh_npc_kegelguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.95, sat=1.1, val=1.0)

    def strepen(block):
        for y in range(block.shape[0]):
            if (y // 6) % 2:
                block[y, :, :3] = WIT

    def panelen(block):
        for x in range(block.shape[1]):
            block[:, x, :3] = MINT if x < block.shape[1] * 0.42 else WIT if x < block.shape[1] * 0.58 else ROZE
    hulp.paint_swatch(a, sw["band"], (240, 80, 180), rng, 4, strepen)
    hulp.paint_swatch(a, sw["shirt"], MINT, rng, 4, panelen)
    hulp.paint_swatch(a, sw["kraag"], (90, 60, 90), rng, 4)
    h.save(Image.fromarray(a), "entity", "npc_kegelguh.png")


# =====================================================================================================================
# models, blockstates, items, loot, tags
# =====================================================================================================================
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def _box(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None):
    f = {}
    for s in faces:
        t = tex[s] if isinstance(tex, dict) else tex
        f[s] = {"texture": t, **({"uv": uv} if uv else {})}
    return {"from": frm, "to": to, "faces": f}


def _gedraaid(model, extra=None):
    """facing=<f>[,<extra>] -> the model (north = as modelled), turned."""
    out = {}
    for f, r in ROT.items():
        for key, m in (extra or {"": model}).items():
            out[f"facing={f}" + (f",{key}" if key else "")] = {"model": m, **({"y": r} if r else {})}
    return out


BOUW = {  # building blocks: name -> (texture, kind of block, mineable)
    "bielzenhout": ("bielzenhout", "plank", "axe"), "ertsgruissteen": ("ertsgruissteen", "steen", "pickaxe"),
    "ertsgruis_bakstenen": ("ertsgruis_bakstenen", "steen", "pickaxe"),
    "kegelbaanhout": ("kegelbaanhout", "plank", "axe"), "retrotegel": ("retrotegel", "steen", "pickaxe"),
    "retrotegel_paars": ("retrotegel_paars", "steen", "pickaxe"),
}
TRAPPEN = ["bielzenhout", "kegelbaanhout"]
DECO = {"ertskarretje": "pickaxe", "stationsklokje": "pickaxe", "seinlampje": "pickaxe", "wolbalrek": "axe", "neon_kegelbord": "pickaxe",
        "kegeltje": None}


def models(h):
    A, D, w = h.A, h.D, h.w

    def model(name, data):
        w(f"{A}/models/block/{name}.json", data)

    def item_block(name, model_=None):
        w(f"{A}/models/item/{name}.json", {"parent": model_ or f"guhs:block/{name}"})
        w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{name}"}})

    def item_plat(name):
        w(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{name}"}})
        w(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{name}"}})

    def drop(name, functions=None):
        entry = {"type": "minecraft:item", "name": f"guhs:{name}"}
        if functions:
            entry["functions"] = functions
        w(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [entry],
                                                                                         "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    # building blocks (+ stairs and slabs)
    from features.barbecuether import stair_states
    for name, (tex, _soort, _tool) in BOUW.items():
        model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": f"guhs:block/{tex}"}})
        w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"guhs:block/{name}"}}})
        item_block(name)
        drop(name)
    for name in TRAPPEN:
        tx = f"guhs:block/{name}"
        for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
            model(f"{name}_trap{suffix}", {"parent": f"minecraft:block/{parent}", "textures": {"bottom": tx, "top": tx, "side": tx}})
        w(f"{A}/blockstates/{name}_trap.json", {"variants": stair_states(f"guhs:block/{name}_trap")})
        model(f"{name}_plaat", {"parent": "minecraft:block/slab", "textures": {"bottom": tx, "top": tx, "side": tx}})
        model(f"{name}_plaat_top", {"parent": "minecraft:block/slab_top", "textures": {"bottom": tx, "top": tx, "side": tx}})
        w(f"{A}/blockstates/{name}_plaat.json", {"variants": {"type=bottom": {"model": f"guhs:block/{name}_plaat"},
                                                               "type=top": {"model": f"guhs:block/{name}_plaat_top"},
                                                               "type=double": {"model": f"guhs:block/{name}"}}})
        item_block(f"{name}_trap")
        item_block(f"{name}_plaat")
        drop(f"{name}_trap")
        drop(f"{name}_plaat", [{"function": "minecraft:set_count", "add": False, "count": 2, "conditions": [
            {"condition": "minecraft:block_state_property", "block": f"guhs:{name}_plaat", "properties": {"type": "double"}}]}])

    # --- game blocks ---
    model("karretjes_spoorbegin", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/karretjes_spoorbegin_top", "side": "guhs:block/bielzenhout", "bottom": "guhs:block/bielzenhout"}})
    w(f"{A}/blockstates/karretjes_spoorbegin.json", {"variants": {f"baan={b},facing={f}": {"model": "guhs:block/karretjes_spoorbegin",
                                                                                           **({"y": r} if r else {})}
                                                                  for b in range(3) for f, r in ROT.items()}})
    for i, erts in enumerate(bouw.ERTSEN):
        t = {"hout": "guhs:block/karretjes_ertsbak", "voor": f"guhs:block/karretjes_ertsbak_voor_{erts}",
             "lading": f"guhs:block/karretjes_ertsbak_{erts}", "particle": "guhs:block/karretjes_ertsbak"}
        els = [_box([1, 0, 1], [15, 1, 15], "#hout"),
               _box([1, 1, 1], [15, 12, 2], {"north": "#voor", "south": "#hout", "east": "#hout", "west": "#hout", "up": "#hout", "down": "#hout"}),
               _box([1, 1, 14], [15, 12, 15], "#hout"), _box([1, 1, 2], [2, 12, 14], "#hout"), _box([14, 1, 2], [15, 12, 14], "#hout"),
               _box([2, 1, 2], [14, 10, 14], "#lading"), _box([4, 10, 4], [10, 12, 10], "#lading"), _box([8, 10, 7], [12, 11.5, 12], "#lading")]
        model(f"karretjes_ertsbak_{erts}", {"textures": t, "elements": els})
    w(f"{A}/blockstates/karretjes_ertsbak.json", {"variants": {
        f"facing={f},soort={i}": {"model": f"guhs:block/karretjes_ertsbak_{bouw.ERTSEN[min(i, len(bouw.ERTSEN) - 1)]}", **({"y": r} if r else {})}
        for f, r in ROT.items() for i in range(6)}})
    model("kegelgrot_baanbegin", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/kegelgrot_baanbegin", "side": "guhs:block/kegelbaanhout", "bottom": "guhs:block/kegelbaanhout"}})
    w(f"{A}/blockstates/kegelgrot_baanbegin.json", {"variants": {f"baan={b},facing={f}": {"model": "guhs:block/kegelgrot_baanbegin",
                                                                                          **({"y": r} if r else {})}
                                                                 for b in range(3) for f, r in ROT.items()}})
    model("kegelgrot_goot", {"textures": {"particle": "guhs:block/kegelgrot_goot", "t": "guhs:block/kegelgrot_goot"},
                             "elements": [_box([0, 0, 0], [16, 12, 16], "#t")]})
    model("kegelgrot_bumper", {"textures": {"particle": "guhs:block/kegelgrot_bumper", "t": "guhs:block/kegelgrot_bumper"},
                               "elements": [_box([0, 0, 0], [16, 8, 16], "#t"), _box([1, 8, 1], [15, 9, 15], "#t")]})
    model("kegelgrot_paaltje", {"textures": {"particle": "guhs:block/kegelgrot_paaltje", "t": "guhs:block/kegelgrot_paaltje",
                                             "kop": "guhs:block/kegeltje_gezicht"},
                                "elements": [_box([4, 0, 4], [12, 11, 12], "#t"),
                                             _box([4.5, 11, 4.5], [11.5, 14, 11.5], {"north": "#kop", "south": "#t", "east": "#t", "west": "#t",
                                                                                     "up": "#t", "down": "#t"})]})
    model("kegelgrot_wolbal", {"textures": {"particle": "guhs:block/kegelgrot_wolbal", "t": "guhs:block/kegelgrot_wolbal"},
                               "elements": [_box([5.2, 0.4, 5.2], [10.8, 6, 10.8], "#t"), _box([4.8, 1.2, 5.6], [11.2, 5.2, 10.4], "#t"),
                                            _box([5.6, 1.2, 4.8], [10.4, 5.2, 11.2], "#t"), _box([5.6, 0, 5.6], [10.4, 6.4, 10.4], "#t")]})
    model("kegelgrot_mikablokje", {"textures": {"particle": "guhs:block/kegelgrot_mikablokje", "t": "guhs:block/kegelgrot_mikablokje"},
                                   "elements": [_box([2, 0, 2], [14, 10, 14], "#t"), _box([3, 10, 6], [5, 13, 9], "#t"), _box([11, 10, 6], [13, 13, 9], "#t")]})
    for name in ("kegelgrot_goot", "kegelgrot_bumper", "kegelgrot_paaltje", "kegelgrot_wolbal", "kegelgrot_mikablokje"):
        w(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"guhs:block/{name}"}}})

    # --- deco ---
    t = {"particle": "guhs:block/ertskarretje", "kar": "guhs:block/ertskarretje", "lading": "guhs:block/ertskarretje_lading",
         "wiel": "minecraft:block/black_concrete"}
    model("ertskarretje", {"textures": t, "elements": [
        _box([1, 2, 1], [15, 3, 15], "#kar"), _box([1, 3, 1], [15, 10, 2], "#kar"), _box([1, 3, 14], [15, 10, 15], "#kar"),
        _box([1, 3, 2], [2, 10, 14], "#kar"), _box([14, 3, 2], [15, 10, 14], "#kar"), _box([2, 3, 2], [14, 9, 14], "#lading"),
        _box([4, 9, 4], [11, 11, 10], "#lading"),
        _box([0, 0, 2], [1, 3, 5], "#wiel"), _box([15, 0, 2], [16, 3, 5], "#wiel"), _box([0, 0, 11], [1, 3, 14], "#wiel"), _box([15, 0, 11], [16, 3, 14], "#wiel")]})
    model("stationsklokje", {"textures": {"particle": "guhs:block/stationsklokje_zij", "zij": "guhs:block/stationsklokje_zij",
                                          "klok": "guhs:block/stationsklokje"}, "elements": [
        _box([7, 0, 7], [9, 8, 9], "#zij"), _box([5, 0, 5], [11, 1, 11], "#zij"),
        _box([2, 6, 7], [14, 16, 9], {"north": "#klok", "south": "#klok", "east": "#zij", "west": "#zij", "up": "#zij", "down": "#zij"})]})
    model("seinlampje", {"textures": {"particle": "guhs:block/stationsklokje_zij", "zij": "guhs:block/stationsklokje_zij",
                                      "sein": "guhs:block/seinlampje"}, "elements": [
        _box([7, 0, 7], [9, 8, 9], "#zij"), _box([5, 8, 5], [11, 16, 11], {"north": "#sein", "south": "#sein", "east": "#zij", "west": "#zij",
                                                                          "up": "#zij", "down": "#zij"})]})
    model("kegeltje", {"textures": {"particle": "guhs:block/kegeltje", "pin": "guhs:block/kegeltje", "kop": "guhs:block/kegeltje_gezicht",
                                    "oor": "minecraft:block/pink_wool"}, "elements": [
        _box([5.5, 0, 5.5], [10.5, 3, 10.5], "#pin"), _box([5, 3, 5], [11, 8, 11], "#pin"), _box([6.5, 8, 6.5], [9.5, 10, 9.5], "#pin"),
        _box([5.5, 10, 5.5], [10.5, 14, 10.5], {"north": "#kop", "south": "#pin", "east": "#pin", "west": "#pin", "up": "#pin", "down": "#pin"}),
        _box([5.5, 14, 7.5], [7, 15.5, 8.5], "#oor"), _box([9, 14, 7.5], [10.5, 15.5, 8.5], "#oor")]})
    ballen = [("#roze", [1.5, 4, 5.5]), ("#mint", [6, 4, 5.5]), ("#lila", [10.5, 4, 5.5])]
    els = [_box([0, 0, 3], [16, 4, 13], "#hout"), _box([0, 4, 3], [16, 5, 4], "#hout"), _box([0, 4, 12], [16, 5, 13], "#hout")]
    for tex, (x, y, z) in ballen:
        els.append(_box([x, y, z], [x + 4.5, y + 4.5, z + 4.5], tex))
    model("wolbalrek", {"textures": {"particle": "guhs:block/wolbalrek", "hout": "guhs:block/wolbalrek", "roze": "guhs:block/kegelgrot_wolbal",
                                     "mint": "guhs:block/wolbal_mint", "lila": "guhs:block/wolbal_lila"}, "elements": els})
    model("neon_kegelbord", {"textures": {"particle": "guhs:block/neon_kegelbord", "n": "guhs:block/neon_kegelbord",
                                          "rand": "minecraft:block/black_concrete"}, "elements": [
        {**_box([0, 0, 14], [16, 16, 16], {"north": "#n", "south": "#rand", "east": "#rand", "west": "#rand", "up": "#rand", "down": "#rand"}),
         "shade": False}]})
    for name in DECO:
        w(f"{A}/blockstates/{name}.json", {"variants": _gedraaid(f"guhs:block/{name}")})
        item_block(name)
        drop(name)
    # --- items ---
    for name in ("ertsbonnetje", "kegelknoop", "kegelgrot_wolbal_leen", "karretjes_conducteurspetje", "kegelgrot_bowlingshirtje",
                 "kegelgrot_zweetbandje"):
        item_plat(name)


def data(h):
    D = h.D
    hout = [f"guhs:{n}" for n, (_, _, tool) in BOUW.items() if tool == "axe"] + [f"guhs:{n}_trap" for n in TRAPPEN] + \
           [f"guhs:{n}_plaat" for n in TRAPPEN] + [f"guhs:{n}" for n, tool in DECO.items() if tool == "axe"]
    steen = [f"guhs:{n}" for n, (_, _, tool) in BOUW.items() if tool == "pickaxe"] + [f"guhs:{n}" for n, tool in DECO.items() if tool == "pickaxe"]
    h.add_tag("minecraft/tags/block/mineable/axe", hout)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", steen)
    for kind in ("block", "item"):
        h.add_tag(f"minecraft/tags/{kind}/stairs", [f"guhs:{n}_trap" for n in TRAPPEN])
        h.add_tag(f"minecraft/tags/{kind}/slabs", [f"guhs:{n}_plaat" for n in TRAPPEN])
    h.add_tag("guhs/tags/item/loaned", ["guhs:kegelgrot_wolbal_leen"])
    h.add_tag("guhs/tags/item/knus/grijptickets", ["guhs:ertsbonnetje", "guhs:kegelknoop"])
    for name in ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


ADVANCEMENTS = ["karretjes_gespeeld", "karretjes_25", "karretjes_lastig_40", "karretjes_kleding",
                "kegelen_gespeeld", "kegelen_strike", "kegelen_150", "kegelen_perfect", "kegelen_kleding"]


# =====================================================================================================================
# texts
# =====================================================================================================================
TEXTS = {  # key: (English, Dutch)
    # blocks, items, entities
    "block.guhs.karretjes_spoorbegin": ("Tunnel Mouth", "Tunnelmond"),
    "block.guhs.karretjes_ertsbak": ("Ore Bin", "Ertsbak"),
    "block.guhs.bielzenhout": ("Sleeper Wood", "Bielzenhout"),
    "block.guhs.bielzenhout_trap": ("Sleeper Wood Stairs", "Bielzenhouttrap"),
    "block.guhs.bielzenhout_plaat": ("Sleeper Wood Slab", "Bielzenhoutplaat"),
    "block.guhs.ertsgruissteen": ("Ore Grit Stone", "Ertsgruissteen"),
    "block.guhs.ertsgruis_bakstenen": ("Ore Grit Bricks", "Ertsgruisbakstenen"),
    "block.guhs.ertskarretje": ("Ore Cart", "Ertskarretje"),
    "block.guhs.ertskarretje.lore": ("Deco from Conducteur Piepwiel. Right-click to sit in it. Choo choo... it doesn't ride, guhtje.",
                                     "Deco van Conducteur Piepwiel. Rechtsklik om erin te zitten. Tuut tuut... rijden doet hij niet, guhtje."),
    "block.guhs.stationsklokje": ("Station Clock", "Stationsklokje"),
    "block.guhs.stationsklokje.lore": ("It always says five past guh.", "Het is altijd vijf over guh."),
    "block.guhs.seinlampje": ("Signal Lamp", "Seinlampje"),
    "block.guhs.seinlampje.lore": ("Red over green. Gives light, never gives a ticket.", "Rood boven groen. Geeft licht, nooit een boete."),
    "block.guhs.kegelgrot_baanbegin": ("Foul Line", "Foutlijn"),
    "block.guhs.kegelgrot_goot": ("Gutter", "Goot"),
    "block.guhs.kegelgrot_bumper": ("Soft Bumper", "Zachte bumper"),
    "block.guhs.kegelgrot_paaltje": ("Plush Post", "Pluchen paaltje"),
    "block.guhs.kegelgrot_wolbal": ("Wool Ball", "Wolbal"),
    "block.guhs.kegelgrot_mikablokje": ("Mika Block", "Mika-blokje"),
    "block.guhs.kegelbaanhout": ("Lane Wood", "Kegelbaanhout"),
    "block.guhs.kegelbaanhout_trap": ("Lane Wood Stairs", "Kegelbaanhouttrap"),
    "block.guhs.kegelbaanhout_plaat": ("Lane Wood Slab", "Kegelbaanhoutplaat"),
    "block.guhs.retrotegel": ("Retro Tile", "Retrotegel"),
    "block.guhs.retrotegel_paars": ("Lilac Retro Tile", "Lila retrotegel"),
    "block.guhs.kegeltje": ("Plush Pin", "Kegeltje"),
    "block.guhs.kegeltje.lore": ("A plush guh pin from De Guhde. It doesn't fall over here. Piep!",
                                 "Een pluchen guh-kegeltje van De Guhde. Hier valt hij niet om. Piep!"),
    "block.guhs.wolbalrek": ("Wool Ball Rack", "Wolbalrek"),
    "block.guhs.wolbalrek.lore": ("Three wool balls, ready to roll.", "Drie wolballen, klaar om te rollen."),
    "block.guhs.neon_kegelbord": ("Neon Bowling Sign", "Neon-kegelbord"),
    "block.guhs.neon_kegelbord.lore": ("Bzzzt. Gives light, very eighties.", "Bzzzt. Geeft licht, heel erg jaren tachtig."),
    "item.guhs.ertsbonnetje": ("Ore Ticket", "Ertsbonnetje"),
    "item.guhs.kegelknoop": ("Pin Button", "Kegelknoop"),
    "item.guhs.kegelgrot_wolbal_leen": ("De Guhde's Wool Ball", "Wolbal van De Guhde"),
    "item.guhs.kegelgrot_wolbal_leen.lore": ("Look where it should go, hold right-click for power, let go to bowl. Stand left or right "
                                             "for a little hook; sneak for a straight ball.",
                                             "Kijk waar hij heen moet, hou rechtsklik vast voor kracht, laat los om te gooien. Sta links of "
                                             "rechts voor een draaitje; sluip voor een rechte bal."),
    "item.guhs.kegelgrot_wolbal_leen.leen": ("On loan: back to De Guhde after the game.", "Geleend: na het potje terug naar De Guhde."),
    "item.guhs.karretjes_conducteurspetje": ("Conductor's Cap", "Conducteurspetje"),
    "item.guhs.kegelgrot_bowlingshirtje": ("Bowling Shirt", "Bowlingshirtje"),
    "item.guhs.kegelgrot_zweetbandje": ("Bowling Sweatband", "Kegelzweetbandje"),
    "entity.guhs.karretjes_kar": ("Ore Cart", "Ertskarretje"),
    "entity.guhs.kegelgrot_kegel": ("Plush Pin", "Kegeltje"),
    "entity.guhs.kegelgrot_bal": ("Wool Ball", "Wolbal"),
    "entity.guhs.kegelgrot_schuif": ("Sliding Mika Block", "Schuivend Mika-blokje"),
    "entity.guhs.guh_npc.wisselguh": ("Conducteur Piepwiel", "Conducteur Piepwiel"),
    "entity.guhs.guh_npc.kegelguh": ("De Guhde", "De Guhde"),
    "structure.guhs.karretjeswissels": ("The Switch Cave", "De Wisselgrot"),
    "structure.guhs.karretjeswissels.tooltip": ("Cave minigame: sort the ore carts with the switches, ertsbonnetjes and a conductor's cap",
                                                "Grot-minigame: zet de wissels voor de ertskarretjes, ertsbonnetjes en een conducteurspetje"),
    "structure.guhs.kegelgrot": ("The Bowling Cave", "De Kegelgrot"),
    "structure.guhs.kegelgrot.tooltip": ("Cave minigame: guh bowling with De Guhde, kegelknopen and a bowling outfit",
                                         "Grot-minigame: guh-bowlen bij De Guhde, kegelknopen en een bowlingpakje"),
    # the Minigames tab, highscores, clothes, scoreboards
    "gui.guhs.spelgroep.karretjes": ("The Switch Cave", "De Wisselgrot"),
    "gui.guhs.spelgroep.karretjes.waar": ("Under a little station hut somewhere in the Guhmension: down with the Wolkenliftje to the Switch "
                                          "Cave, with Conducteur Piepwiel on the balcony.",
                                          "Onder een stationshutje ergens in de Guhmension: met het Wolkenliftje naar beneden, naar de "
                                          "Wisselgrot, bij Conducteur Piepwiel op het balkon."),
    "gui.guhs.spelgroep.kegelen": ("The Bowling Cave", "De Kegelgrot"),
    "gui.guhs.spelgroep.kegelen.waar": ("Under a little hut with a giant pin on its roof somewhere in the Guhmension: down with the "
                                        "Wolkenliftje, De Guhde is behind the counter.",
                                        "Onder een hutje met een reuzenkegel op het dak ergens in de Guhmension: met het Wolkenliftje naar "
                                        "beneden, De Guhde staat achter de balie."),
    "gui.guhs.kledingbron.karretjes": ("The Switch Cave", "De Wisselgrot"),
    "gui.guhs.kledingbron.kegelen": ("The Bowling Cave", "De Kegelgrot"),
    "gui.guhs.kleding.prijs.munt.ertsbonnetjes": ("%s ore tickets", "%s ertsbonnetjes"),
    "gui.guhs.kleding.prijs.munt.kegelknopen": ("%s pin buttons", "%s kegelknopen"),
    "gui.guhs.highscores.game.karretjes_makkelijk": ("Cart switches (easy)", "Karretjes-wissels (makkelijk)"),
    "gui.guhs.highscores.game.karretjes_medium": ("Cart switches (medium)", "Karretjes-wissels (medium)"),
    "gui.guhs.highscores.game.karretjes_lastig": ("Cart switches (hard)", "Karretjes-wissels (lastig)"),
    "gui.guhs.highscores.game.kegelen_makkelijk": ("Guh bowling (straight lane)", "Guh-bowlen (rechte baan)"),
    "gui.guhs.highscores.game.kegelen_medium": ("Guh bowling (bendy lane)", "Guh-bowlen (bochtige baan)"),
    "gui.guhs.highscores.game.kegelen_lastig": ("Guh bowling (obstacle lane)", "Guh-bowlen (hindernisjesbaan)"),
    "gui.guhs.gids.rij.karretjes_makkelijk": ("Track 1: easy", "Baan 1: makkelijk"),
    "gui.guhs.gids.rij.karretjes_medium": ("Track 2: medium", "Baan 2: medium"),
    "gui.guhs.gids.rij.karretjes_lastig": ("Track 3: hard", "Baan 3: lastig"),
    "gui.guhs.gids.rij.kegelen_makkelijk": ("Straight lane: 10 frames", "Rechte baan: 10 frames"),
    "gui.guhs.gids.rij.kegelen_medium": ("Bendy lane: 10 frames", "Bochtige baan: 10 frames"),
    "gui.guhs.gids.rij.kegelen_lastig": ("Obstacle lane: 10 frames", "Hindernisjesbaan: 10 frames"),
    "gui.guhs.scorebord.karretjes": ("Switch Cave top 3 (carts sorted)", "Wisselgrot top 3 (karretjes goed)"),
    "gui.guhs.scorebord.kegelen": ("Bowling Cave top 3", "Kegelgrot top 3"),
    "gui.guhs.mijnspellen.niet_bouwen": ("Njeg, this cave is a game hall: no building or digging here!",
                                         "Njeg, deze grot is een spelletjeshal: hier niet bouwen of graven!"),
    # --- Karretjes-wissels: Conducteur Piepwiel ---
    "gui.guhs.karretjes.optie.makkelijk": ("Track 1: easy (2 switches)", "Baan 1: makkelijk (2 wissels)"),
    "gui.guhs.karretjes.optie.medium": ("Track 2: medium (3 switches)", "Baan 2: medium (3 wissels)"),
    "gui.guhs.karretjes.optie.lastig": ("Track 3: hard (5 switches)", "Baan 3: lastig (5 wissels)"),
    "gui.guhs.karretjes.optie.winkel": ("The shop", "Het winkeltje"),
    "gui.guhs.karretjes.optie.uitleg": ("How does it work?", "Hoe werkt het?"),
    "gui.guhs.karretjes.optie.stop": ("Stop my shift", "Stop mijn dienst"),
    "quest.guhs.karretjes.hallo1": ("Tuut tuut! Conducteur Piepwiel here. The ore carts are waiting, and so are the bins. Your best: %s / %s / %s.",
                                    "Tuut tuut! Conducteur Piepwiel hier. De ertskarretjes staan klaar, de bakken ook. Jouw beste: %s / %s / %s."),
    "quest.guhs.karretjes.hallo2": ("Welcome to the Switch Cave! Every cart in the right bin, and no derailing... we don't even have that. Your best: %s / %s / %s.",
                                    "Welkom in de Wisselgrot! Elk karretje in de goeie bak, en ontsporen... dat doen we hier niet eens. Jouw beste: %s / %s / %s."),
    "quest.guhs.karretjes.hallo3": ("Psst. The coal cart once ended up in the vads bin. The vads tasted of soot for a week. Njeg! Your best: %s / %s / %s.",
                                    "Psst. Het kolenkarretje is ooit in de vadsbak beland. De vads smaakten een week naar roet. Njeg! Jouw beste: %s / %s / %s."),
    "quest.guhs.karretjes.hallo4": ("All aboard! Well, not you. The ore. Your best: %s / %s / %s.",
                                    "Instappen! Nou ja, jij niet. De ertsen. Jouw beste: %s / %s / %s."),
    "quest.guhs.karretjes.uitleg": ("The carts roll out of the tunnel, each full of one ore: the colour of its load says which bin it belongs in "
                                    "(the ore is also in the wall behind its bin). On the railing there is a lever above every switch: "
                                    "on = right, off = left; the rail shows where the switch sends the next cart. A cart in the wrong bin is gone; "
                                    "%s gone and your shift is over. They come ever faster, guhtje! But calmly at first: the first carts roll "
                                    "slowly, the very first one waits in the tunnel mouth, and the bar at the bottom always says which ore comes "
                                    "next, so you can set the switch early. On track 1 the right bin sparkles. The signs on the balcony wall show it too.",
                                    "De karretjes rollen uit de tunnel, elk vol met één erts: de kleur van de lading zegt in welke bak hij moet "
                                    "(het erts zit ook in de muur achter zijn bak). Op de reling staat boven elke wissel een hendel: aan = "
                                    "rechtsaf, uit = linksaf; de rail laat zien waar de wissel het volgende karretje heen stuurt. Een karretje "
                                    "in de verkeerde bak is weg; %s weg en je dienst zit erop. Ze komen steeds sneller, guhtje! Maar eerst rustig: "
                                    "de eerste karretjes rollen langzaam, de allereerste wacht even in de tunnelmond, en onderin staat steeds welk "
                                    "erts er hierna komt, dan zet je de wissel alvast goed. Op baan 1 glinstert de goeie bak. De bordjes op de "
                                    "balkonmuur laten het ook zien."),
    "quest.guhs.karretjes.bezig": ("%s is on shift right now. Watch from the balcony, it's quite exciting!",
                                   "%s heeft nu dienst. Kijk maar mee vanaf het balkon, het is best spannend!"),
    "quest.guhs.karretjes.bezig_jij": ("You're on shift! %s right, %s gone. Keep your paws on the levers!",
                                       "Je hebt dienst! %s goed, %s weg. Pootjes op de hendels!"),
    "quest.guhs.karretjes.kapot": ("Njeg... I can't find my rails. This cave isn't quite right.",
                                   "Njeg... ik kan mijn sporen niet vinden. Deze grot klopt niet helemaal."),
    "quest.guhs.karretjes.start_makkelijk": ("Track 1! Two switches, three bins. Tuut tuut, here they come...",
                                             "Baan 1! Twee wissels, drie bakken. Tuut tuut, daar komen ze..."),
    "quest.guhs.karretjes.start_medium": ("Track 2! Three switches, four bins. Mind the gold, it's shy.",
                                          "Baan 2! Drie wissels, vier bakken. Pas op het goud, dat is verlegen."),
    "quest.guhs.karretjes.start_lastig": ("Track 3! Five switches, six bins. Even I get dizzy. WAHOOG!",
                                          "Baan 3! Vijf wissels, zes bakken. Daar word zelfs ik duizelig van. VAHOEG!"),
    "quest.guhs.karretjes.hoe": ("Levers on the railing: on = right, off = left (as you look at the tracks). Every cart in the bin of its ore!",
                                 "Hendels op de reling: aan = rechtsaf, uit = linksaf (zoals jij naar de sporen kijkt). Elk karretje in de bak van zijn erts!"),
    "quest.guhs.karretjes.rustig": ("Calm start: the first carts roll slowly and the very first one waits in the tunnel mouth. The bar at the bottom "
                                    "always says which ore comes next: set the switch early!",
                                    "Rustig begin: de eerste karretjes rollen langzaam en de allereerste wacht even in de tunnelmond. Onderin staat "
                                    "steeds welk erts er hierna komt: zet de wissel alvast goed!"),
    "quest.guhs.karretjes.gestopt": ("Shift over! The carts go back to the depot.", "Dienst voorbij! De karretjes gaan terug naar de remise."),
    "quest.guhs.karretjes.weggelopen": ("You left your post... the carts stopped by themselves.", "Je liep weg van je post... de karretjes stopten vanzelf."),
    "quest.guhs.karretjes.te_lang": ("That was a very long shift! Time for a nap, guhtje.", "Dat was een heel lange dienst! Tijd voor een dutje, guhtje."),
    "quest.guhs.karretjes.tien1": ("%s carts right! You're a natural, guhtje.", "%s karretjes goed! Jij bent een natuurtalent, guhtje."),
    "quest.guhs.karretjes.tien2": ("%s! The bins are getting full. Njeg!", "%s! De bakken raken vol. Njeg!"),
    "quest.guhs.karretjes.tien3": ("%s! Shall I make you head conductor?", "%s! Zal ik je hoofdconducteur maken?"),
    "quest.guhs.karretjes.telling": ("Shift over: %s carts in the right bin (%s).", "Dienst voorbij: %s karretjes in de goeie bak (%s)."),
    "quest.guhs.karretjes.record": ("New personal record: %s (was %s)! +5 ertsbonnetjes.", "Nieuw persoonlijk record: %s (was %s)! +5 ertsbonnetjes."),
    "quest.guhs.karretjes.eerste_record": ("Your first shift on this track: %s carts. That's your record now!",
                                           "Je eerste dienst op deze baan: %s karretjes. Dat is nu je record!"),
    "quest.guhs.karretjes.geen_record": ("Your record on this track stays %s.", "Je record op deze baan blijft %s."),
    "quest.guhs.karretjes.munten": ("Conducteur Piepwiel gives you %s ertsbonnetjes.", "Conducteur Piepwiel geeft je %s ertsbonnetjes."),
    "quest.guhs.karretjes.grot_record": ("The best shift of the whole world on this track! Your name is on the board.",
                                         "De beste dienst van de hele wereld op deze baan! Je naam staat op het bord."),
    "quest.guhs.karretjes.oordeel_top": ("Phew! I've never seen anything like it. Want my cap? (No. Buy one. Njeg.)",
                                         "Pfoe! Zoiets heb ik nog nooit gezien. Wil je mijn petje? (Nee. Koop er een. Njeg.)"),
    "quest.guhs.karretjes.oordeel_goed": ("Very good! The ore thanks you, and so do I.", "Heel goed! Het erts bedankt je, en ik ook."),
    "quest.guhs.karretjes.oordeel_aardig": ("Not bad! Next time a bit quicker on the levers.", "Niet slecht! Volgende keer iets vlotter met de hendels."),
    "quest.guhs.karretjes.oordeel_oefenen": ("The coal got mixed up with the vads again... practice makes perfect, guhtje!",
                                             "Het kolen ging weer bij de vads... oefening baart kunst, guhtje!"),
    "gui.guhs.karretjes.aftellen": ("Paws on the levers!", "Pootjes op de hendels!"),
    "gui.guhs.karretjes.tuut": ("TUUT TUUT!", "TUUT TUUT!"),
    "gui.guhs.karretjes.tuut_sub": ("Here come the carts!", "Daar komen de karretjes!"),
    "gui.guhs.karretjes.balk": ("Right: %s | Gone: %s/%s | Next: %s | %s", "Goed: %s | Weg: %s/%s | Volgende: %s | %s"),
    "gui.guhs.karretjes.eerste": ("First cart: %s!", "Eerste karretje: %s!"),
    "gui.guhs.karretjes.klopt": ("%s in the right bin! (%s)", "%s in de goeie bak! (%s)"),
    "gui.guhs.karretjes.fout": ("Oops! %s in the %s bin... %s left", "Oei! %s in de %s-bak... nog %s"),
    "gui.guhs.karretjes.klaar": ("%s carts!", "%s karretjes!"),
    "gui.guhs.karretjes.klaar_sub": ("+%s ertsbonnetjes", "+%s ertsbonnetjes"),
    "gui.guhs.karretjes.erts.kolen": ("Coal", "Kolen"),
    "gui.guhs.karretjes.erts.ijzer": ("Iron", "IJzer"),
    "gui.guhs.karretjes.erts.goud": ("Gold", "Goud"),
    "gui.guhs.karretjes.erts.vads": ("Vads", "Vads"),
    "gui.guhs.karretjes.erts.kaasknabbel": ("Kaasknabbel", "Kaasknabbel"),
    "gui.guhs.karretjes.erts.diamant": ("Diamond", "Diamant"),
    # --- Kegelgrot: De Guhde ---
    "gui.guhs.kegelen.optie.makkelijk": ("Lane 1: straight (easy)", "Baan 1: recht (makkelijk)"),
    "gui.guhs.kegelen.optie.medium": ("Lane 2: bendy (medium)", "Baan 2: bochtig (medium)"),
    "gui.guhs.kegelen.optie.lastig": ("Lane 3: obstacles (hard)", "Baan 3: hindernisjes (lastig)"),
    "gui.guhs.kegelen.optie.winkel": ("The shop", "Het winkeltje"),
    "gui.guhs.kegelen.optie.uitleg": ("How does it work?", "Hoe werkt het?"),
    "gui.guhs.kegelen.optie.stop": ("Stop my game", "Stop mijn potje"),
    "gui.guhs.kegelen.baan.makkelijk": ("Straight lane", "Rechte baan"),
    "gui.guhs.kegelen.baan.medium": ("Bendy lane", "Bochtige baan"),
    "gui.guhs.kegelen.baan.lastig": ("Obstacle lane", "Hindernisjesbaan"),
    "quest.guhs.kegelen.hallo1": ("Hey, guh. The Guhde abides. Fancy a game? Your best: %s / %s / %s.",
                                  "Hé, guh. De Guhde blijft rustig. Zin in een potje? Jouw beste: %s / %s / %s."),
    "quest.guhs.kegelen.hallo2": ("That rug really tied the cave together, you know. Bowl a game? Your best: %s / %s / %s.",
                                  "Dat kleedje maakte de grot echt af, weet je. Potje bowlen? Jouw beste: %s / %s / %s."),
    "quest.guhs.kegelen.hallo3": ("The pins squeak when they fall. Don't worry, they like it. Your best: %s / %s / %s.",
                                  "De kegeltjes piepen als ze vallen. Geen zorgen, dat vinden ze leuk. Jouw beste: %s / %s / %s."),
    "quest.guhs.kegelen.hallo4": ("Strike! Oh, wait, I'm just practising saying it. Your best: %s / %s / %s.",
                                  "Strike! O nee, ik oefen alleen het zeggen. Jouw beste: %s / %s / %s."),
    "quest.guhs.kegelen.uitleg": ("Ten frames, two balls each (a strike: all ten with the first ball, a spare: all ten with two). A strike "
                                  "counts 10 + your next two balls, a spare 10 + the next one; in the 10th frame you get bonus balls. Look where "
                                  "the ball should go, hold right-click for power and let go. Stand left or right on the approach and it hooks "
                                  "towards the middle; sneak for a straight ball.",
                                  "Tien frames, twee ballen per frame (strike: alle tien met de eerste bal, spare: alle tien met twee). Een "
                                  "strike telt 10 + je volgende twee ballen, een spare 10 + de volgende; in het 10e frame krijg je extra ballen. "
                                  "Kijk waar de bal heen moet, hou rechtsklik vast voor kracht en laat los. Sta links of rechts op de aanloop "
                                  "en hij draait naar het midden; sluip voor een rechte bal."),
    "quest.guhs.kegelen.bezig": ("%s is bowling now. Grab a seat, have a kaasknabbel.", "%s is nu aan het bowlen. Pak een stoeltje, neem een kaasknabbel."),
    "quest.guhs.kegelen.bezig_jij": ("You're at %s points, man. Keep rolling.", "Je staat op %s punten, man. Blijf rollen."),
    "quest.guhs.kegelen.kapot": ("Njeg... the lanes are gone? That's... not cool, man.", "Njeg... de banen zijn weg? Dat is... niet cool, man."),
    "quest.guhs.kegelen.vol": ("Your paws are full! Make room for the ball first.", "Je pootjes zitten vol! Maak eerst plek voor de bal."),
    "quest.guhs.kegelen.start_makkelijk": ("The straight lane. Mind the gutters, they're hungry.", "De rechte baan. Pas op de goten, die hebben honger."),
    "quest.guhs.kegelen.start_medium": ("The bendy lane! Bounce off the bumpers, it's allowed.", "De bochtige baan! Kaats via de bumpers, dat mag."),
    "quest.guhs.kegelen.start_lastig": ("The obstacle lane. Two posts and a Mika who won't sit still. Good luck, guh.",
                                        "De hindernisjesbaan. Twee paaltjes en een Mika die niet stil kan zitten. Succes, guh."),
    "quest.guhs.kegelen.hoe": ("Look at the pins, hold right-click, let go. Left or right of the middle = a hook; sneak = straight.",
                               "Kijk naar de kegels, hou rechtsklik vast, laat los. Links of rechts van het midden = een draaitje; sluipen = recht."),
    "quest.guhs.kegelen.gestopt": ("Game over, man. The ball goes back on the rack.", "Potje voorbij, man. De bal gaat terug in het rek."),
    "quest.guhs.kegelen.weggelopen": ("You walked off the approach... the pins went to sleep.", "Je liep weg van de aanloop... de kegeltjes zijn gaan slapen."),
    "quest.guhs.kegelen.te_lang": ("The ball got cold, man. Another time!", "De bal werd koud, man. Een andere keer!"),
    "quest.guhs.kegelen.strike1": ("STRIKE! The pins squeak for joy!", "STRIKE! De kegeltjes piepen van blijdschap!"),
    "quest.guhs.kegelen.strike2": ("Strike. Far out, man.", "Strike. Te gek, man."),
    "quest.guhs.kegelen.strike3": ("All ten! WAHOOG!", "Alle tien! VAHOEG!"),
    "quest.guhs.kegelen.frame": ("Frame %s: %s  (total %s)", "Frame %s: %s  (totaal %s)"),
    "quest.guhs.kegelen.telling": ("Ten frames done: %s points (%s), %s strikes.", "Tien frames klaar: %s punten (%s), %s strikes."),
    "quest.guhs.kegelen.record": ("New personal record: %s (was %s)! +5 kegelknopen.", "Nieuw persoonlijk record: %s (was %s)! +5 kegelknopen."),
    "quest.guhs.kegelen.eerste_record": ("Your first game on this lane: %s points. That's your record now!",
                                         "Je eerste potje op deze baan: %s punten. Dat is nu je record!"),
    "quest.guhs.kegelen.geen_record": ("Your record on this lane stays %s.", "Je record op deze baan blijft %s."),
    "quest.guhs.kegelen.munten": ("De Guhde gives you %s kegelknopen.", "De Guhde geeft je %s kegelknopen."),
    "quest.guhs.kegelen.grot_record": ("The best game of the whole world on this lane! Your name is on the board.",
                                       "Het beste potje van de hele wereld op deze baan! Je naam staat op het bord."),
    "quest.guhs.kegelen.oordeel_top": ("Whoa. That was... like, the best thing I've seen in this cave. Respect, guh.",
                                       "Wow. Dat was... zeg maar, het mooiste wat ik in deze grot heb gezien. Respect, guh."),
    "quest.guhs.kegelen.oordeel_goed": ("Nice game, man. The pins will talk about it for days.", "Mooi potje, man. De kegeltjes hebben het er nog dagen over."),
    "quest.guhs.kegelen.oordeel_aardig": ("Not bad! A little more hook next time.", "Niet slecht! Volgende keer iets meer draai."),
    "quest.guhs.kegelen.oordeel_oefenen": ("The gutters say thank you. Practice, guh, practice.", "De goten zeggen dankjewel. Oefenen, guh, oefenen."),
    "gui.guhs.kegelen.balk": ("Frame %s, ball %s | %s points | %s pins standing", "Frame %s, bal %s | %s punten | %s kegels staan"),
    "gui.guhs.kegelen.wacht": ("Wait for the pins...", "Wacht op de kegeltjes..."),
    "gui.guhs.kegelen.sta": ("Stand on the approach of your lane!", "Ga op de aanloop van je baan staan!"),
    "gui.guhs.kegelen.kijk": ("Look down the lane, at the pins!", "Kijk de baan af, naar de kegeltjes!"),
    "gui.guhs.kegelen.goot": ("Into the gutter...", "De goot in..."),
    "gui.guhs.kegelen.goot_titel": ("Gutter ball!", "Gootbal!"),
    "gui.guhs.kegelen.mis": ("Missed!", "Mis!"),
    "gui.guhs.kegelen.mis_sub": ("Not a single piep", "Geen enkele piep"),
    "gui.guhs.kegelen.strike": ("STRIKE!", "STRIKE!"),
    "gui.guhs.kegelen.strike_sub": ("All ten pins! Piep piep piep!", "Alle tien de kegeltjes! Piep piep piep!"),
    "gui.guhs.kegelen.spare": ("SPARE!", "SPARE!"),
    "gui.guhs.kegelen.spare_sub": ("The rest of them too!", "De rest ook nog!"),
    "gui.guhs.kegelen.kegels": ("%s pins!", "%s kegels!"),
    "gui.guhs.kegelen.kegels_sub": ("%s left standing", "nog %s blijven staan"),
    "gui.guhs.kegelen.klaar": ("%s points!", "%s punten!"),
    "gui.guhs.kegelen.klaar_sub": ("+%s kegelknopen", "+%s kegelknopen"),
    "gui.guhs.kegelen.kracht": ("Power: %s%%", "Kracht: %s%%"),
    "gui.guhs.kegelen.kracht_recht": ("Power: %s%% (straight)", "Kracht: %s%% (recht)"),
}


def texts(h, extra):
    for key, (en, nl) in TEXTS.items():
        h.lang(key, en, nl)
    for key, (en, nl) in extra.items():
        h.lang(key, en, nl)


# =====================================================================================================================
def build(h):
    import make_structures as ms
    textures(h)
    iconen(h)
    kleding(h)
    npcs(h)
    models(h)
    data(h)
    extra = {}
    kinfo, ginfo = bouw.build(h, ms, extra)
    texts(h, extra)
    print(f"mijnspellen: Wisselgrot {len(kinfo['banen'])} networks, Kegelgrot {len(ginfo['banen'])} lanes, {len(TEXTS) + len(extra)} texts")


# =====================================================================================================================
# on its own: python tools/features/mijnspellen.py (from the project root)
# =====================================================================================================================
def _direct():
    import types
    import make_structures as ms
    import make_v2
    R = os.path.join("src", "main", "resources")
    A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
    NL, EN = {}, {}

    def w(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=2, ensure_ascii=False)
            f.write("\n")

    def save(img, *path):
        full = os.path.join(A, "textures", *path)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        img.save(full)

    def add_tag(path, values):
        full = f"{R}/data/{path}.json"
        data_ = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
        for v in values:
            if v not in data_["values"]:
                data_["values"].append(v)
        w(full, data_)

    def lang(key, en, nl):
        EN[key] = en
        NL[key] = nl
    h = types.SimpleNamespace(R=R, A=A, D=D, TEX=os.path.join(A, "textures"), w=w, save=save, add_tag=add_tag, lang=lang,
                              recolour=make_v2.recolour, pinkish=make_v2.pinkish)
    build(h)
    w(os.path.join("tools", "lang", "en", "c194_mijnspellen.json"), dict(sorted(EN.items())))
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data_ = json.load(open(p, encoding="utf-8"))
        data_.update(teksten)
        w(p, data_)
    print(f"mijnspellen: {len(NL)} texts merged")


# --- FTB Quests (tools/make_ftbquests.py): a section per game in the chapter "Onder de Wol", after the three of grotspellen.py --------
FTB_CHAPTER = "guhs_onderwol"
FTB_SECTIES = [("ow_karretjes", "Karretjes-wissels", "npc:wisselguh", ["karretjes_grot", "karretjes_gespeeld", "karretjes_25", "karretjes_lastig",
                                                                        "karretjes_pakje"]),
               ("ow_kegelgrot", "De Kegelgrot", "npc:kegelguh", ["kegelgrot_grot", "kegelgrot_gespeeld", "kegelgrot_strike", "kegelgrot_150",
                                                                  "kegelgrot_perfect", "kegelgrot_pakje"])]


def ftb(fq):
    q, adv = fq.q, fq.adv
    q("karretjes_grot", "De Wisselgrot", "Onder een hutje met een wolkenliftje ligt een grot vol mijnsporen. &6Conducteur Piepwiel&r "
      "zoekt een wisselwachter (Superkompas: Minigames).", "guhs:ertsbonnetje", [fq.structure("karretjeswissels")], xp=100)
    q("karretjes_gespeeld", "Wissels om!", "Sta op het balkon en zet de wissels met de hendels, zodat elk ertskarretje in de bak van zijn "
      "eigen kleur rolt. Drie karretjes kwijt en het is klaar. Je verdient &6ertsbonnetjes&r.", "minecraft:lever",
      [adv("karretjes_gespeeld")], rewards=(("guhs:ertsbonnetje", 4),), xp=150)
    q("karretjes_25", "Op tijd, op het spoor", "Breng 25 karretjes goed aan in een dienst.", "guhs:ertskarretje", [adv("karretjes_25")],
      rewards=(("guhs:ertsbonnetje", 6),), xp=200)
    q("karretjes_lastig", "Hoofdwisselwachter", "Breng 40 karretjes goed aan op het lastige net.", "guhs:seinlampje",
      [adv("karretjes_lastig_40")], rewards=(("guhs:ertsbonnetje", 10),), shape="hexagon", xp=400)
    q("karretjes_pakje", "Conducteur!", "Koop het conducteurspetje bij Conducteur Piepwiel, voor ertsbonnetjes.",
      "guhs:karretjes_conducteurspetje", [adv("karretjes_kleding")], shape="gear", xp=200)
    q("kegelgrot_grot", "De Kegelgrot", "Onder een hutje met een grote kegel op het dak ligt een bowlingbaan in de grot. &6De Guhde&r "
      "leent je een wolbal (Superkompas: Minigames).", "guhs:kegelknoop", [fq.structure("kegelgrot")], xp=100)
    q("kegelgrot_gespeeld", "Tien frames", "Speel een hele partij: kijk waar de bal heen moet, houd rechtsklik vast voor kracht en laat "
      "los. Sta wat opzij voor een beetje effect. Je verdient &6kegelknopen&r.", "guhs:kegeltje", [adv("kegelen_gespeeld")],
      rewards=(("guhs:kegelknoop", 4),), xp=150)
    q("kegelgrot_strike", "STRIKE!", "Gooi alle tien de pluchen guh-kegeltjes in een keer om. Piep piep piep!", "guhs:kegeltje",
      [adv("kegelen_strike")], xp=150)
    q("kegelgrot_150", "Honderdvijftig", "Gooi 150 punten of meer in een partij.", "guhs:wolbalrek", [adv("kegelen_150")],
      rewards=(("guhs:kegelknoop", 8),), xp=250)
    q("kegelgrot_perfect", "De perfecte partij", "300 punten: twaalf strikes op rij. The Guhde abides.", "guhs:neon_kegelbord",
      [adv("kegelen_perfect")], rewards=(("guhs:kegelknoop", 32),), shape="hexagon", xp=750)
    q("kegelgrot_pakje", "Bowlingkampioen", "Koop het bowlingshirtje en het kegelzweetbandje bij De Guhde, voor kegelknopen.",
      "guhs:kegelgrot_bowlingshirtje", [adv("kegelen_kleding")], shape="gear", xp=200)


if __name__ == "__main__":
    _direct()
