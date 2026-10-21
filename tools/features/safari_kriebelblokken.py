"""
Op Safari! (1.6.0), round 4 slice L3 - the Kriebelhuis + lobby blocks (Java: feature/safari/kriebel/KriebelBlokken.java;
design safari-ontwerp/lobby2/ontwerp.md; contract SAFARI_CONTRACT.md "Ronde 4").

  terrariumglas, kasglas, glasvloer   full glass blocks (spruce frame / white ribs / water edge), translucent
  mierenboerderij                     an ant farm: crawling ants in sandy tunnels (animated texture)
  infobord                            a sign on a post; right-click = the species' Guhdex text (gui.guhs.safari.infobord.<soort>)
  warmtelamp                          a hanging lamp with a glowing orange bulb
  nepkoraal                           never-dying coral, kleur roze/geel/blauw/oranje/paars (right-click: next colour)
  glimpaddenstoel, sterrenplafond     a glowing mushroom, a twinkling night sky (animated)
  schorsplaat                         a flat piece of bark

Writes textures, models, blockstates, client items, loot, recipes, mineable tags and the names (NL + EN appended to the
lang files, English also in tools/lang/en/c170_safari_kriebelblokken.json). Run on its own from the project root:
    python tools/features/safari_kriebelblokken.py
"""
import json
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from features.safari_verblijf import FEITEN          # noqa: E402

RES = os.path.join("src", "main", "resources")
A = os.path.join(RES, "assets", "guhs")
D = os.path.join(RES, "data", "guhs")
EN_BESTAND = os.path.join("tools", "lang", "en", "c170_safari_kriebelblokken.json")
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
KLEUREN = {"roze": (240, 120, 170), "geel": (240, 206, 80), "blauw": (80, 150, 230), "oranje": (240, 140, 60), "paars": (160, 100, 210)}
BLOKKEN = ["terrariumglas", "kasglas", "glasvloer", "mierenboerderij", "infobord", "warmtelamp", "nepkoraal", "glimpaddenstoel",
           "sterrenplafond", "schorsplaat"]

NAMEN = {  # id: (nl, en)
    "terrariumglas": ("Terrariumglas", "Terrarium Glass"),
    "kasglas": ("Kasglas", "Greenhouse Glass"),
    "glasvloer": ("Glasvloer", "Glass Floor"),
    "mierenboerderij": ("Mierenboerderij", "Ant Farm"),
    "infobord": ("Infobord", "Info Board"),
    "warmtelamp": ("Warmtelamp", "Heat Lamp"),
    "nepkoraal": ("Nepkoraal", "Fake Coral"),
    "glimpaddenstoel": ("Glimpaddenstoel", "Glow Mushroom"),
    "sterrenplafond": ("Sterrenplafond", "Starry Ceiling"),
    "schorsplaat": ("Schorsplaat", "Bark Plate"),
}

rng = np.random.default_rng(1606)


# =====================================================================================================================
# writing
# =====================================================================================================================
NL, EN = {}, {}


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def lang(key, nl, en):
    NL[key] = nl
    EN[key] = en


def tex(img, naam, frametime=None, interpolate=False):
    pad = os.path.join(A, "textures", "block", "safari", naam + ".png")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    img.save(pad)
    if frametime:
        w(pad + ".mcmeta", {"animation": {"frametime": frametime, "interpolate": interpolate}})


def img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def vlak(c, var=6, alpha=255, h=16):
    a = np.zeros((h, 16, 4), float)
    a[..., :3] = np.array(c, float) + rng.normal(0, var, (h, 16, 1))
    a[..., 3] = alpha
    return a


def put(a, x, y, c, alpha=255):
    if 0 <= x < a.shape[1] and 0 <= y < a.shape[0]:
        a[y, x, :3] = c
        a[y, x, 3] = alpha


def rand(a, dikte, c, var=8, y0=0):
    for y in range(y0, y0 + 16):
        for x in range(16):
            if min(x, y - y0, 15 - x, 15 - (y - y0)) < dikte:
                a[y, x, :3] = np.array(c, float) + rng.normal(0, var)
                a[y, x, 3] = 255


def el(frm, to, faces, rot=None, licht=None, cull=False):
    e = {"from": frm, "to": to, "faces": {}}
    for f, t in faces.items():
        fd = {"texture": t} if isinstance(t, str) else dict(t)
        if cull:
            fd["cullface"] = f
        e["faces"][f] = fd
    if rot:
        e["rotation"] = rot
    if licht:
        e["light_emission"] = licht
    return e


ALLE6 = ("north", "south", "east", "west", "up", "down")


def model(naam, obj):
    w(os.path.join(A, "models", "block", naam + ".json"), obj)


def kubus(naam, zij, boven=None, onder=None, translucent=False):
    def t(s):
        return {"sprite": s, "force_translucent": True} if translucent else s
    textures = {"particle": t(zij), "zij": t(zij), "boven": t(boven or zij), "onder": t(onder or boven or zij)}
    faces = {f: ("#boven" if f == "up" else "#onder" if f == "down" else "#zij") for f in ALLE6}
    model(naam, {"textures": textures, "elements": [el([0, 0, 0], [16, 16, 16], faces, cull=True)]})


def blockstate(naam, variants):
    w(os.path.join(A, "blockstates", naam + ".json"), {"variants": variants})


def item(naam, block_model=True, textuur=None):
    if block_model:
        w(os.path.join(A, "models", "item", naam + ".json"), {"parent": f"guhs:block/{naam}"})
    else:
        w(os.path.join(A, "models", "item", naam + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": textuur}})
    w(os.path.join(A, "items", naam + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{naam}"}})


def loot(naam):
    w(os.path.join(D, "loot_table", "blocks", naam + ".json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{naam}"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def shaped(naam, pattern, key, count=1):
    res = {"id": f"guhs:{naam}"}
    if count != 1:
        res["count"] = count
    w(os.path.join(D, "recipe", naam + ".json"),
      {"type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern, "key": key, "result": res})


def tag(pad, namen):
    full = os.path.join(RES, "data", "minecraft", "tags", "block", *pad) + ".json"
    d = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
    for n in namen:
        if f"guhs:{n}" not in d["values"]:
            d["values"].append(f"guhs:{n}")
    w(full, d)


# =====================================================================================================================
# textures (16x16)
# =====================================================================================================================
SPAR = (112, 80, 48)


def tex_terrariumglas():
    a = vlak((200, 236, 214), 3, 70)
    for i in range(3, 7):                       # a soft glint
        put(a, i, 9 - i, (255, 255, 255), 140)
    rand(a, 2, SPAR)
    for x in range(16):                         # darker inner edge of the frame
        for y in (2, 13):
            put(a, x, y, (86, 60, 36)) if 2 <= x <= 13 else None
    for y in range(2, 14):
        for x in (2, 13):
            put(a, x, y, (86, 60, 36))
    return img(a)


def tex_kasglas():
    a = vlak((226, 240, 244), 2, 55)
    for i in range(16):
        put(a, i, 0, (250, 250, 250))
        put(a, i, 15, (236, 236, 236))
        put(a, 0, i, (250, 250, 250))
        put(a, 15, i, (236, 236, 236))
        put(a, 8, i, (246, 246, 246))           # the rib in the middle
    for i in range(3, 6):
        put(a, i, 6 - i + 2, (255, 255, 255), 150)
    return img(a)


def tex_glasvloer():
    a = vlak((196, 232, 240), 3, 80)
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            if d < 2:                           # the water edge
                put(a, x, y, (np.array((60, 140, 220)) + rng.normal(0, 10)).tolist(), 190)
    for x in range(1, 15, 3):
        put(a, x, 0, (170, 220, 255), 220)      # little wave crests
        put(a, 15 - x, 15, (170, 220, 255), 220)
    for i in range(4, 8):
        put(a, i, 11 - i, (255, 255, 255), 150)
    return img(a)


def tex_mierenboerderij():
    """8 frames: sandy earth behind glass with a winding tunnel; ants walk along it."""
    tunnel = [(3, 2), (4, 3), (5, 4), (6, 4), (7, 5), (8, 6), (9, 6), (10, 7), (11, 8), (11, 9), (10, 10), (9, 10), (8, 11),
              (7, 11), (6, 12), (5, 12), (4, 11), (3, 10), (3, 9), (4, 8), (5, 8), (6, 7), (5, 6), (4, 5)]
    kamer = [(10, 3), (11, 3), (12, 3), (11, 4), (12, 4), (12, 5)]
    basis = vlak((206, 172, 112), 9)
    for y in range(16):
        for x in range(16):
            if rng.random() < 0.08:
                put(basis, x, y, (176, 140, 90))
    for (x, y) in tunnel + kamer + [(11, 5), (12, 6), (11, 7)]:
        put(basis, x, y, (120, 88, 52))
    for x in range(1, 15):                      # grass on top, behind the glass
        put(basis, x, 1, (110, 170, 80))
    rand(basis, 1, SPAR)
    frames = []
    for f in range(8):
        a = basis.copy()
        for mier in range(4):
            i = (f * 1 + mier * 6) % len(tunnel)
            x, y = tunnel[i]
            put(a, x, y, (24, 18, 16))
        for (x, y) in kamer[(f // 2) % 3::3]:
            put(a, x, y, (40, 26, 20))          # the ants in the little room shuffle about
        for i in range(2, 5):
            a[i, 15 - i, :3] = np.minimum(a[i, 15 - i, :3] + 60, 255)   # a glint on the glass
        frames.append(a)
    return img(np.concatenate(frames, axis=0))


def tex_hout():
    a = vlak((168, 124, 80), 7)
    for y in (3, 7, 11, 15):
        a[y, :, :3] *= 0.82
    return img(a)


def tex_infobord_voor():
    """The board: a cream panel with a pink border, a paw print and a few 'text' lines."""
    a = vlak((244, 232, 204), 3)
    rand(a, 1, (232, 128, 168), 4)
    poot = (124, 82, 48)
    for (x, y) in [(3, 6), (4, 6), (3, 7), (4, 7), (2, 4), (5, 4), (3, 3), (4, 3)]:
        put(a, x, y, poot)
    for (y, x0, x1) in ((4, 8, 13), (6, 8, 12), (8, 8, 13), (10, 3, 12), (12, 3, 10)):
        for x in range(x0, x1 + 1):
            put(a, x, y, (130, 116, 100))
    return img(a)


def tex_lamp():
    kap = vlak((150, 70, 46), 6)
    for x in range(16):
        kap[0, x, :3] = (110, 50, 34)
    ketting = vlak((70, 70, 76), 4)
    for y in range(0, 16, 2):
        ketting[y, :, :3] = (110, 110, 118)
    gloed = vlak((255, 160, 60), 8)
    gloed[6:10, 6:10, :3] = (255, 220, 140)
    return img(kap), img(ketting), img(gloed)


def tex_koraal(c):
    a = np.zeros((16, 16, 4), float)
    takken = [(8, 15, 8, 6), (8, 10, 4, 4), (8, 11, 12, 3), (4, 4, 3, 1), (12, 3, 13, 0), (8, 6, 7, 2), (5, 13, 2, 9), (11, 13, 14, 8)]
    for (x0, y0, x1, y1) in takken:
        n = max(abs(x1 - x0), abs(y1 - y0)) * 2 + 1
        for t in np.linspace(0, 1, n):
            x, y = int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t))
            k = np.array(c, float) * (0.85 + 0.25 * rng.random())
            put(a, x, y, k.tolist())
            put(a, x + 1, y, (k * 0.8).tolist())
        put(a, x1, y1, np.minimum(np.array(c) + 50, 255).tolist())    # light tips
    return img(a)


def tex_paddenstoel():
    steel = vlak((226, 230, 214), 4)
    hoed = vlak((70, 210, 220), 10)
    for (x, y) in [(3, 4), (7, 2), (11, 6), (5, 10), (12, 12), (9, 9)]:
        put(hoed, x, y, (210, 255, 250))
    return img(steel), img(hoed)


def tex_sterrenplafond():
    """4 frames of a dark blue sky; the stars swap brightness so they twinkle (interpolated)."""
    basis = vlak((22, 26, 62), 5)
    for y in range(16):
        for x in range(16):
            if rng.random() < 0.05:
                put(basis, x, y, (40, 40, 90))
    sterren = [(2, 3), (7, 1), (12, 4), (4, 9), (10, 10), (14, 13), (1, 14), (8, 6), (6, 13)]
    frames = []
    for f in range(4):
        a = basis.copy()
        for i, (x, y) in enumerate(sterren):
            fel = (i + f) % 4
            c = [(255, 250, 210), (230, 220, 170), (140, 140, 160), (200, 200, 240)][fel]
            put(a, x, y, c)
            if fel == 0:                        # the brightest gets a little cross
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    put(a, x + dx, y + dy, (170, 170, 140))
        frames.append(a)
    return img(np.concatenate(frames, axis=0))


def tex_schors():
    a = vlak((96, 66, 42), 9)
    for x in range(0, 16, 3):
        for y in range(16):
            if rng.random() < 0.8:
                put(a, x + int(rng.integers(0, 2)), y, (62, 42, 28))
    for _ in range(5):
        put(a, int(rng.integers(0, 16)), int(rng.integers(0, 16)), (110, 130, 70))   # a speck of moss
    return img(a)


# =====================================================================================================================
# the blocks
# =====================================================================================================================
def blokken():
    T = "guhs:block/safari/"
    tex(tex_terrariumglas(), "terrariumglas")
    tex(tex_kasglas(), "kasglas")
    tex(tex_glasvloer(), "glasvloer")
    for n in ("terrariumglas", "kasglas", "glasvloer"):
        kubus(n, T + n, translucent=True)
        blockstate(n, {"": {"model": f"guhs:block/{n}"}})

    tex(tex_mierenboerderij(), "mierenboerderij", frametime=6)
    kubus("mierenboerderij", T + "mierenboerderij", "minecraft:block/spruce_planks")
    blockstate("mierenboerderij", {"": {"model": "guhs:block/mierenboerderij"}})

    tex(tex_sterrenplafond(), "sterrenplafond", frametime=20, interpolate=True)
    kubus("sterrenplafond", T + "sterrenplafond")
    blockstate("sterrenplafond", {"": {"model": "guhs:block/sterrenplafond"}})

    # infobord: front of the board = north (rotated by facing)
    tex(tex_hout(), "infobord_hout")
    tex(tex_infobord_voor(), "infobord_voor")
    h, v = "#hout", "#voor"
    model("infobord", {"textures": {"particle": T + "infobord_hout", "hout": T + "infobord_hout", "voor": T + "infobord_voor"},
                       "elements": [el([7, 0, 7], [9, 9, 9], {f: h for f in ALLE6}),
                                    el([1, 8, 6.5], [15, 16, 9.5], {"north": v, "south": h, "east": h, "west": h, "up": h, "down": h}),
                                    el([0.5, 15.5, 6], [15.5, 16.5, 10], {f: h for f in ALLE6})]})
    blockstate("infobord", {f"facing={f}": ({"model": "guhs:block/infobord", "y": y} if y else {"model": "guhs:block/infobord"})
                            for f, y in FACING_Y.items()})

    kap, ketting, gloed = tex_lamp()
    tex(kap, "warmtelamp_kap")
    tex(ketting, "warmtelamp_ketting")
    tex(gloed, "warmtelamp_gloed")
    model("warmtelamp", {"textures": {"particle": T + "warmtelamp_kap", "kap": T + "warmtelamp_kap", "ketting": T + "warmtelamp_ketting",
                                      "gloed": T + "warmtelamp_gloed"},
                         "elements": [el([7.5, 10, 7.5], [8.5, 16, 8.5], {f: "#ketting" for f in ("north", "south", "east", "west")}),
                                      el([4, 7, 4], [12, 10, 12], {f: "#kap" for f in ALLE6}),
                                      el([5, 10, 5], [11, 11, 11], {f: "#kap" for f in ALLE6}),
                                      el([5.5, 4, 5.5], [10.5, 7, 10.5], {f: "#gloed" for f in ALLE6}, licht=15)]})
    blockstate("warmtelamp", {"": {"model": "guhs:block/warmtelamp"}})

    for k, c in KLEUREN.items():
        tex(tex_koraal(c), f"nepkoraal_{k}")
        model(f"nepkoraal_{k}", {"parent": "minecraft:block/cross", "textures": {"cross": T + f"nepkoraal_{k}"}})
    blockstate("nepkoraal", {f"kleur={k}": {"model": f"guhs:block/nepkoraal_{k}"} for k in KLEUREN})

    steel, hoed = tex_paddenstoel()
    tex(steel, "glimpaddenstoel_steel")
    tex(hoed, "glimpaddenstoel_hoed")
    s, ho = "#steel", "#hoed"
    model("glimpaddenstoel", {"textures": {"particle": T + "glimpaddenstoel_hoed", "steel": T + "glimpaddenstoel_steel",
                                           "hoed": T + "glimpaddenstoel_hoed"},
                              "elements": [el([7, 0, 7], [9, 4, 9], {f: s for f in ALLE6}),
                                           el([5, 4, 5], [11, 6, 11], {f: ho for f in ALLE6}, licht=12),
                                           el([10.5, 0, 4], [11.5, 2, 5], {f: s for f in ALLE6}),
                                           el([10, 2, 3.5], [12, 3, 5.5], {f: ho for f in ALLE6}, licht=12)]})
    blockstate("glimpaddenstoel", {"": {"model": "guhs:block/glimpaddenstoel"}})

    tex(tex_schors(), "schorsplaat")
    model("schorsplaat", {"textures": {"particle": T + "schorsplaat", "schors": T + "schorsplaat"},
                          "elements": [el([0, 0, 0], [16, 2, 16], {"north": "#schors", "south": "#schors", "east": "#schors", "west": "#schors",
                                                                    "up": "#schors", "down": {"texture": "#schors", "cullface": "down"}})]})
    blockstate("schorsplaat", {"": {"model": "guhs:block/schorsplaat"}})

    for n in BLOKKEN:
        if n == "nepkoraal":
            item(n, False, T + "nepkoraal_roze")
        else:
            item(n)
        loot(n)

    # recipes: all craftable (decorative blocks are nicer to have)
    shaped("terrariumglas", ["GGG", "GSG", "GGG"], {"G": "minecraft:glass", "S": "minecraft:spruce_planks"}, 8)
    shaped("kasglas", ["GGG", "GWG", "GGG"], {"G": "minecraft:glass", "W": "minecraft:white_dye"}, 8)
    shaped("glasvloer", ["GGG", "GBG", "GGG"], {"G": "minecraft:glass", "B": "minecraft:light_blue_dye"}, 8)
    shaped("mierenboerderij", ["PGP", "GDG", "PGP"], {"P": "minecraft:spruce_planks", "G": "minecraft:glass_pane", "D": "minecraft:coarse_dirt"}, 2)
    shaped("infobord", ["PPP", "PKP", " S "], {"P": "minecraft:oak_planks", "K": "minecraft:paper", "S": "minecraft:stick"}, 3)
    shaped("warmtelamp", [" N ", "IGI", " O "], {"N": "minecraft:iron_nugget", "I": "minecraft:copper_ingot", "G": "minecraft:glowstone",
                                                 "O": "minecraft:orange_dye"}, 2)
    shaped("nepkoraal", [" B ", "BCB"], {"B": "minecraft:bone_meal", "C": "minecraft:clay_ball"}, 3)
    shaped("glimpaddenstoel", ["G", "M"], {"G": "minecraft:glow_berries", "M": "minecraft:brown_mushroom"}, 2)
    shaped("sterrenplafond", ["BBB", "BGB", "BBB"], {"B": "minecraft:blue_concrete", "G": "minecraft:glowstone_dust"}, 8)
    shaped("schorsplaat", ["LL"], {"L": "minecraft:spruce_log"}, 6)

    tag(["mineable", "axe"], ["mierenboerderij", "infobord", "schorsplaat"])
    tag(["mineable", "pickaxe"], ["warmtelamp", "sterrenplafond"])

    for n, (nl, en) in NAMEN.items():
        lang(f"block.guhs.{n}", nl, en)


# R5: the aaihoek animals and the Kriebelhuis' sea decorations have no verblijfsbord (so no FEITEN), but an infobord can
# still point at them
EXTRA_FEITEN = {
    "geitje": ("Geitjes klimmen overal op, zelfs op andere geitjes. Hoe hoger, hoe vadsiger.",
               "Little goats climb on everything, even on other little goats. The higher, the chonkier."),
    "alpaca": ("Een alpaca spuugt alleen als hij écht boos is. Meestal is hij gewoon heel pluizig.",
               "An alpaca only spits when it's really cross. Mostly it's just very fluffy."),
    "cavia": ("Blije cavia's maken luchtsprongetjes. Dat heet 'popcornen'!",
              "Happy guinea pigs do little jumps in the air. That's called 'popcorning'!"),
    "ezeltje": ("Ezeltjes onthouden plekken en vriendjes heel lang, soms wel 25 jaar.",
                "Little donkeys remember places and friends for a very long time, sometimes 25 years."),
    "anemoon": ("Een anemoon lijkt een bloem, maar het is een dier! De clownvis woont er veilig tussen.",
                "An anemone looks like a flower, but it's an animal! The clownfish lives safely in between."),
    "zee_egel": ("Een zee-egel loopt op honderden piepkleine pootjes, heel langzaam over de bodem.",
                 "A sea urchin walks on hundreds of tiny feet, very slowly across the bottom."),
}


def infoteksten():
    """gui.guhs.safari.infobord.<soort> for every species (its Guhdex fact), plus the general and the empty board."""
    soorten = json.load(open(os.path.join(D, "safari", "soorten.json"), encoding="utf-8"))
    for soort in soorten:
        feit = FEITEN.get(soort) or EXTRA_FEITEN.get(soort)
        if feit:
            nl, en = feit
            lang(f"gui.guhs.safari.infobord.{soort}", nl + " Kijk goed rond, misschien zie je er eentje!",
                 en + " Look around carefully, maybe you'll spot one!")
    lang("gui.guhs.safari.infobord.algemeen",
         "Hier woont: %s! Een echte bewoner van het park. Niet tikken op het glas, dat vinden ze helemaal niet vads.",
         "Living here: %s! A real resident of the park. Don't tap on the glass, they don't find that chonky at all.")
    lang("gui.guhs.safari.infobord.leeg",
         "Dit bord wacht nog op een dier. Het staat hier alvast heel geduldig te wachten, njeg.",
         "This board is still waiting for an animal. It's standing here very patiently, nyeg.")


def af():
    for taal, nieuw in (("nl_nl", NL), ("en_us", EN)):
        pad = os.path.join(A, "lang", f"{taal}.json")
        d = json.load(open(pad, encoding="utf-8"))
        d.update(nieuw)
        with open(pad, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    w(EN_BESTAND, EN)


if __name__ == "__main__":
    blokken()
    infoteksten()
    af()
    print(f"safari_kriebelblokken: {len(BLOKKEN)} blocks, {len(NL)} texts")
