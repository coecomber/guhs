"""
1.7.0 "Onder de Wol", ronde 29: water in the Guhmension, rarer than in the overworld and cute (Java: feature/onderwol/
OnderWolWater.java). This module writes:

  1. beekjes (step 1, lakes; after the vijvertjes):
       guhs:onderwol_kronkelbeekje   shallow winding brooks (1-3 wide) through low ground, from a bronnetje to a pond or
                                     other water, with a little rapid now and then, viltkiezel, lily pads, reeds
       guhs:onderwol_bronbeekje      a narrow brook from a spring on a hill down to a pond, with 1-2 block steps
  2. grotmeertjes (step 7, underground_decoration): guhs:onderwol_grotmeertje, still ponds in the low cave floors
     (y 14-60) with a glowing rim of lichtmos and glinsterpaddenstoelen; every Guhmension biome but the Gatenkaasgrotten
  3. watervalletjes in the caves (step 8, fluid_springs): guhs:onderwol_grotwaterval (viltsteen, y 46-64) and
     guhs:onderwol_grotwaterval_diep (knuffelsteen, y 26-45, rarer): vanilla water springs, far fewer than vanilla's
  4. the blocks bronnetje (a bubbling spring stone, waterloggable) and glinsterriet (a glittering pink reed): textures,
     blockstates, models, item models, 26.1 item definitions, loot, tags, nl/en texts.

Idempotent: the biome lists lose our ids and get them re-appended at the end of their step. NOTE: onderwol.py rewrites
the six cave biomes from scratch (glitterspelonk, pluisgrot, slaapgrot, ...): run this module again after onderwol.py.

    PYTHONPATH=tools python tools/features/onderwol_water.py        (from the worktree root)
"""
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from features.onderwol_tex import rng, leeg, ruis, zet, img, mix   # noqa: E402
from features.onderwol_tex2 import viltkiezel                      # noqa: E402

R = os.path.join("src", "main", "resources")
A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
WG = f"{D}/worldgen"
CUTOUT = "minecraft:cutout"


def g(*names):
    return [f"guhs:{n}" for n in names]


# --- which biomes get what -------------------------------------------------------------------------------------------
# the ordinary surface biomes (no seas, caves, the three lake regions of biomes3 or the Beverbeek; no cold ones either:
# guhpolder and the toendra; guhwaii and the kaasmoeras have water of their own)
KRONKEL = g("guh_fields", "guh_meadows", "pink_puffs", "kaas_flats", "knabbel_crumbs", "knuffeldal", "vadswoud", "mikas_biome", "bleekwoud")
BRON = g("guh_fields", "guh_meadows", "pink_puffs", "knabbel_crumbs", "guh_peaks", "vads_cliffs", "knuffeldal", "vadswoud", "bleekwoud")
NIET_GROT = g("gatenkaasgrotten")     # (the cheese caves: saus country)


def grot_biomes():
    dim = json.load(open(f"{D}/dimension/guhmension.json", encoding="utf-8"))
    return sorted({e["biome"] for e in dim["generator"]["biome_source"]["biomes"]} - set(NIET_GROT))


def stappen():
    grot = grot_biomes()
    return {
        1: [("onderwol_kronkelbeekje", KRONKEL), ("onderwol_bronbeekje", BRON)],
        7: [("onderwol_grotmeertje", grot)],
        8: [("onderwol_grotwaterval", grot), ("onderwol_grotwaterval_diep", grot)],
    }


BLOKKEN = ["bronnetje", "glinsterriet"]
NAMEN = {  # id: (Dutch, English)
    "bronnetje": ("Bronnetje", "Little Spring"),
    "glinsterriet": ("Glinsterriet", "Glitter Reed"),
}
LORE = {  # id: (Dutch, English)
    "bronnetje": ("Een bubbelend bronsteentje. Onder water borrelen er belletjes uit en glinstert het roze boven het beekje.",
                  "A bubbling little spring stone. Under water it bubbles, and pink glitter sparkles above the brook."),
    "glinsterriet": ("Groeit langs de beekjes en glinstert zachtjes roze, vooral 's nachts.",
                     "Grows along the brooks and softly glitters pink, especially at night."),
}


# =====================================================================================================================
# helpers (the same as onderwol.py's _direct)
# =====================================================================================================================
NL, EN = {}, {}


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def patch_json(path, fn):
    data = json.load(open(path, encoding="utf-8"))
    fn(data)
    w(path, data)


def add_tag(path, values):
    full = f"{R}/data/{path}.json"
    data = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    w(full, data)


def lang(key, en, nl):
    EN[key] = en
    NL[key] = nl


# =====================================================================================================================
# textures
# =====================================================================================================================
def bronnetje_top():
    """Felt pebbles with the spring mouth in the middle: a plum rim, a pale pink-aqua swirl and two white glints."""
    t = viltkiezel()
    for y in range(16):
        for x in range(16):
            d = np.hypot(x - 7.5, y - 7.5)
            if d < 2.2:
                f = d / 2.2
                t[y, x, :3] = mix((252, 236, 246), (236, 176, 214), f)
            elif d < 3.1:
                t[y, x, :3] = (246, 196, 226) if (x + y) % 3 else (226, 150, 196)
            elif d < 3.8:
                t[y, x, :3] = mix(tuple(t[y, x, :3]), (150, 78, 118), 0.65)
    for (x, y) in [(6, 6), (9, 8)]:
        zet(t, x, y, (255, 255, 255))
    zet(t, 8, 6, (220, 246, 250))
    zet(t, 7, 9, (220, 246, 250))
    return t


def bronnetje_zijkant():
    t = viltkiezel()
    t[..., :3] *= 0.92
    t[0:1, :, :3] = np.clip(t[0:1, :, :3] + 18, 0, 255)
    return t


RIET = [(170, 84, 128), (206, 116, 160), (232, 150, 190), (248, 194, 222)]


def glinsterriet():
    """Four slender stalks, pink to blush from the foot up, a soft plume on the two tallest and little white glints."""
    r = rng(2901)
    t = leeg()
    stengels = [(4, 9, -1), (7, 14, 0), (10, 12, 1), (12, 7, 1)]   # (x at the foot, height, lean)
    for x0, hoogte, helling in stengels:
        for i in range(hoogte):
            y = 15 - i
            x = x0 + (helling if i > hoogte * 0.6 else 0)
            f = i / max(hoogte - 1, 1)
            k = RIET[min(3, int(f * 3.2))]
            zet(t, x, y, k)
            if i == hoogte - 1 and hoogte >= 12:        # the plume
                for (dx, dy, kk) in [(0, -1, (250, 214, 232)), (0, 0, (244, 180, 212)), (-1, 0, (236, 160, 200)), (1, 0, (236, 160, 200)),
                                     (0, 1, (232, 150, 190)), (-1, 1, (214, 124, 168)), (1, 1, (214, 124, 168)), (0, 2, (214, 124, 168))]:
                    zet(t, x + dx, y + dy, kk)
            elif i > 2 and r.random() < 0.18:           # a little leaf
                zet(t, x + (1 if r.random() < 0.5 else -1), y, RIET[1])
    for (x, y) in [(7, 1), (10, 3), (4, 6), (12, 8), (9, 9)]:
        zet(t, x, y, (255, 255, 255))
    for (x, y) in [(6, 3), (11, 5), (3, 9)]:
        zet(t, x, y, (255, 214, 238), 190)
    return t


def textures():
    for naam, t in (("bronnetje_top", bronnetje_top()), ("bronnetje_zijkant", ruis(bronnetje_zijkant(), 1.5, 2902)),
                    ("glinsterriet", glinsterriet())):
        p = os.path.join(A, "textures", "block", f"{naam}.png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        img(t).save(p)


# =====================================================================================================================
# blocks, items, loot, tags, texts
# =====================================================================================================================
def _vlak(van, tot, cull_onder=False):
    faces = {f: {"texture": "#side"} for f in ("north", "south", "east", "west")}
    faces["up"] = {"texture": "#top"}
    faces["down"] = {"texture": "#bottom", **({"cullface": "down"} if cull_onder else {})}
    return {"from": van, "to": tot, "faces": faces}


def blocks_and_items():
    # the bronnetje: three stacked rounded layers of felt pebbles, the spring mouth on top
    w(f"{A}/models/block/bronnetje.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "guhs:block/bronnetje_zijkant", "top": "guhs:block/bronnetje_top", "side": "guhs:block/bronnetje_zijkant",
                     "bottom": "guhs:block/viltkiezel"},
        "elements": [_vlak([1, 0, 1], [15, 2, 15], True), _vlak([3, 2, 3], [13, 4, 13]), _vlak([5, 4, 5], [11, 5, 11])]})
    w(f"{A}/blockstates/bronnetje.json", {"variants": {f"waterlogged={v}": {"model": "guhs:block/bronnetje"} for v in ("false", "true")}})
    w(f"{A}/models/item/bronnetje.json", {"parent": "guhs:block/bronnetje"})
    # the glinsterriet: a cross, like a plant
    w(f"{A}/models/block/glinsterriet.json", {"parent": "minecraft:block/cross", "render_type": CUTOUT, "textures": {"cross": "guhs:block/glinsterriet"}})
    w(f"{A}/blockstates/glinsterriet.json", {"variants": {"": {"model": "guhs:block/glinsterriet"}}})
    w(f"{A}/models/item/glinsterriet.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "guhs:block/glinsterriet"}})
    for b in BLOKKEN:
        w(f"{A}/items/{b}.json", {"model": {"type": "minecraft:model", "model": f"guhs:item/{b}"}})
        w(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{b}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": f"guhs:blocks/{b}"})
    add_tag("minecraft/tags/block/mineable/pickaxe", g("bronnetje"))
    add_tag("minecraft/tags/block/replaceable_by_trees", g("glinsterriet"))
    add_tag("minecraft/tags/block/sword_efficient", g("glinsterriet"))
    add_tag("guhs/tags/worldgen/biome/onderwol/beekjes", sorted(set(KRONKEL) | set(BRON)))
    for b, (nl, en) in NAMEN.items():
        lang(f"block.guhs.{b}", en, nl)
    for b, (nl, en) in LORE.items():
        lang(f"block.guhs.{b}.lore", en, nl)


# =====================================================================================================================
# the features
# =====================================================================================================================
def _hoogte(lo, hi):
    return {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}


def features():
    cf, pf = f"{WG}/configured_feature", f"{WG}/placed_feature"
    op_oppervlak = lambda zeldzaam: [{"type": "minecraft:rarity_filter", "chance": zeldzaam}, {"type": "minecraft:in_square"},  # noqa: E731
                                     {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
    # 1. the brooks (many attempts fail on purpose: the Java only builds one where it fits and stays put)
    for naam, zeldzaam in (("onderwol_kronkelbeekje", 7), ("onderwol_bronbeekje", 6)):
        w(f"{cf}/{naam}.json", {"type": f"guhs:{naam}", "config": {}})
        w(f"{pf}/{naam}.json", {"feature": f"guhs:{naam}", "placement": op_oppervlak(zeldzaam)})
    # 2. the cave ponds: a few tries in the cave layers; the Java looks down for the floor
    w(f"{cf}/onderwol_grotmeertje.json", {"type": "guhs:onderwol_grotmeertje", "config": {}})
    w(f"{pf}/onderwol_grotmeertje.json", {"feature": "guhs:onderwol_grotmeertje", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:count", "count": 4}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": _hoogte(16, 64)}, {"type": "minecraft:biome"}]})
    # 3. the cave waterfalls: vanilla springs in the cave walls (vanilla tries 25 times per chunk; these 2 and 1 in 2)
    w(f"{cf}/onderwol_grotwaterval.json", {"type": "minecraft:spring_feature", "config": {
        "state": {"Name": "minecraft:water", "Properties": {"falling": "true"}}, "requires_block_below": True, "rock_count": 4, "hole_count": 1,
        "valid_blocks": ["guhs:viltsteen", "guhs:knuffelsteen", "guhs:roze_knuffelsteen"]}})
    w(f"{pf}/onderwol_grotwaterval.json", {"feature": "guhs:onderwol_grotwaterval", "placement": [
        {"type": "minecraft:count", "count": 2}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": _hoogte(46, 64)},
        {"type": "minecraft:biome"}]})
    w(f"{pf}/onderwol_grotwaterval_diep.json", {"feature": "guhs:onderwol_grotwaterval", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": _hoogte(26, 45)},
        {"type": "minecraft:biome"}]})


def biomes():
    per_biome = {}
    alle = set()
    for stap, lijst in stappen().items():
        for naam, biomes_ in lijst:
            alle.add(f"guhs:{naam}")
            for b in biomes_:
                per_biome.setdefault(b, {}).setdefault(stap, []).append(f"guhs:{naam}")
    for b in grot_biomes():          # (also clean the biomes that lost all of ours)
        per_biome.setdefault(b, {})

    def voeg_toe(d, ps):
        f = d["features"]
        while len(f) < 11:
            f.append([])
        for stap in range(len(f)):
            f[stap] = [x for x in f[stap] if x not in alle]
        for stap, ids in ps.items():
            f[stap].extend(ids)
    for b, ps in per_biome.items():
        pad = f"{WG}/biome/{b.split(':')[1]}.json"
        if os.path.exists(pad):
            patch_json(pad, lambda d, ps=ps: voeg_toe(d, ps))


def selfcheck():
    problemen = []
    for t in ("bronnetje_top", "bronnetje_zijkant", "glinsterriet", "viltkiezel"):
        if not os.path.exists(f"{A}/textures/block/{t}.png"):
            problemen.append(f"texture missing: {t}")
    for b in g("viltsteen", "knuffelsteen", "roze_knuffelsteen", "viltkiezel", "lichtmos", "lichtmosblok", "glinsterpaddenstoel", "guh_waterlelie",
               "bloesemmeertje_riet", "roze_gras"):
        if not os.path.exists(f"{A}/blockstates/{b.split(':')[1]}.json"):
            problemen.append(f"block missing: {b}")
    for tag in ("onderwol/oppervlakgrond", "onderwol/laagsteen"):
        if not os.path.exists(f"{D}/tags/block/{tag}.json"):
            problemen.append(f"block tag missing: guhs:{tag} (onderwol.py / onderwol_oppervlak.py)")
    for stap, lijst in stappen().items():
        for naam, biomes_ in lijst:
            feat = json.load(open(f"{WG}/placed_feature/{naam}.json", encoding="utf-8"))["feature"]
            if not os.path.exists(f"{WG}/configured_feature/{feat.split(':')[1]}.json"):
                problemen.append(f"configured feature missing: {feat}")
            for b in biomes_:
                pad = f"{WG}/biome/{b.split(':')[1]}.json"
                if not os.path.exists(pad):
                    problemen.append(f"biome file missing: {b}")
                    continue
                d = json.load(open(pad, encoding="utf-8"))
                if d["features"][stap].count(f"guhs:{naam}") != 1:
                    problemen.append(f"{b}: {naam} not (once) in step {stap}")
    if problemen:
        raise SystemExit("onderwol_water self-check failed:\n  " + "\n  ".join(problemen))


def build():
    textures()
    blocks_and_items()
    features()
    biomes()
    for taal, teksten in (("nl_nl", NL), ("en_us", EN)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update(teksten)
        w(p, data)
    selfcheck()
    st = stappen()
    print(f"onderwol_water: {len(BLOKKEN)} blocks, {len(NL)} texts, {sum(len(l) for l in st.values())} placed features in "
          f"{len({b for l in st.values() for _, bs in l for b in bs})} biomes")


if __name__ == "__main__":
    build()
