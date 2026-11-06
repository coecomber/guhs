"""
1.10 night round T, slice T-Z5WERELD: the new Hoogvads building blocks (zone 5, the capital on Moeder Wolkwal).

The "minimal epic set" of renders-hoogvads/BLOKKEN_VOORSTEL.md (the bold "moet" blocks, 30 blockstates): rose marble and
cloud marble (+ polished, stairs, slabs, walls, columns), the carved guh face, golden cornices + column capitals + roof tiles,
cathedral glass with GOLDEN lead, whale skin for Moeder Wolkwal and the Hoogvads banner. The concept prefix hv_ is dropped.
The textures/models come from the approved concept (renders-hoogvads/_bron/jars/hv-concept.zip, made by concept_blokken.py);
they are copied into the repo once (src/main/resources/assets/guhs/textures/block/hoogvads/) so the build never needs the zip.
The other concept blocks (marble tiles, gold band, checkerboard, filigree, statue marble, cloud stone) are NOT added: the map
palette (tools/vadsrijk/zone5/kit/palet.py) maps them to existing Guhs blocks.

Java: feature/vadsrijk/zone5/wereld/Z5Blokken.java (registers BLOKKEN below in the same order, puts them in the "Guhs: Bouwen" tab).
Standalone (from the project root): python tools/features/vadsrijk_t_z5blokken.py [--zip <hv-concept.zip>]
"""
import io
import json
import os
import sys
import zipfile

RES = os.path.join("src", "main", "resources")
A = os.path.join(RES, "assets", "guhs")
D = os.path.join(RES, "data", "guhs")
TEXDIR = os.path.join(A, "textures", "block", "hoogvads")
ZIP = os.path.join(os.path.dirname(os.path.abspath(__file__)), *([".."] * 4), "renders-hoogvads", "_bron", "jars", "hv-concept.zip")

# id -> (soort, NL, EN). soort: blok | trap | plaat | muur | zuil | glas | vaandel
BLOKKEN = {
    "roze_marmer": ("blok", "Roze marmer", "Rose Marble"),
    "roze_marmer_gepolijst": ("blok", "Gepolijst roze marmer", "Polished Rose Marble"),
    "roze_marmer_gepolijst_trap": ("trap", "Gepolijste roze marmeren trap", "Polished Rose Marble Stairs"),
    "roze_marmer_gepolijst_plaat": ("plaat", "Gepolijste roze marmeren plaat", "Polished Rose Marble Slab"),
    "roze_marmer_gepolijst_muur": ("muur", "Gepolijst roze marmeren muurtje", "Polished Rose Marble Wall"),
    "roze_marmer_zuil": ("zuil", "Roze marmeren zuil", "Rose Marble Column"),
    "roze_marmer_guhgezicht": ("blok", "Roze marmer met guhgezicht", "Carved Guh-Face Rose Marble"),
    "wolkmarmer": ("blok", "Wolkmarmer", "Cloud Marble"),
    "wolkmarmer_gepolijst": ("blok", "Gepolijst wolkmarmer", "Polished Cloud Marble"),
    "wolkmarmer_gepolijst_trap": ("trap", "Gepolijste wolkmarmeren trap", "Polished Cloud Marble Stairs"),
    "wolkmarmer_gepolijst_plaat": ("plaat", "Gepolijste wolkmarmeren plaat", "Polished Cloud Marble Slab"),
    "wolkmarmer_gepolijst_muur": ("muur", "Gepolijst wolkmarmeren muurtje", "Polished Cloud Marble Wall"),
    "wolkmarmer_zuil": ("zuil", "Wolkmarmeren zuil", "Cloud Marble Column"),
    "gouden_kroonlijst": ("blok", "Gouden kroonlijst", "Golden Cornice"),
    "gouden_kroonlijst_trap": ("trap", "Gouden kroonlijsttrap", "Golden Cornice Stairs"),
    "gouden_kroonlijst_plaat": ("plaat", "Gouden kroonlijstplaat", "Golden Cornice Slab"),
    "gouden_zuilkop": ("zuil", "Gouden zuilkop", "Golden Capital"),
    "gouden_dakpan": ("blok", "Gouden dakpannen", "Golden Roof Tiles"),
    "gouden_dakpan_trap": ("trap", "Gouden dakpantrap", "Golden Roof Tile Stairs"),
    "gouden_dakpan_plaat": ("plaat", "Gouden dakpanplaat", "Golden Roof Tile Slab"),
    "kathedraalglas_guh": ("glas", "Kathedraalglas (guh)", "Cathedral Glass (Guh)"),
    "kathedraalglas_hart": ("glas", "Kathedraalglas (hartje)", "Cathedral Glass (Heart)"),
    "kathedraalglas_roos": ("glas", "Kathedraalglas (roos)", "Cathedral Glass (Rose)"),
    "kathedraalglas_ruitjes": ("glas", "Kathedraalglas (ruitjes)", "Cathedral Glass (Diamonds)"),
    "walvishuid": ("blok", "Walvishuid", "Whale Skin"),
    "walvishuid_licht": ("blok", "Lichte walvishuid", "Light Whale Skin"),
    "walvisbuik": ("blok", "Walvisbuik", "Whale Belly"),
    "walvishuid_wolkpok": ("blok", "Walvishuid met wolkpokje", "Cloud-Barnacled Whale Skin"),
    "hoogvads_vaandel_boven": ("vaandel", "Hoogvads-vaandel (boven)", "Hoogvads Banner (Top)"),
    "hoogvads_vaandel_onder": ("vaandel", "Hoogvads-vaandel (onder)", "Hoogvads Banner (Bottom)"),
}
# concept id (hv_...) of each block
CONCEPT = {k: "hv_" + k for k in BLOKKEN}
CONCEPT.update({"gouden_kroonlijst": "hv_goudlijst", "gouden_kroonlijst_trap": "hv_goudlijst_trap",
                "gouden_kroonlijst_plaat": "hv_goudlijst_plaat"})
# concept texture name -> repo texture name (under block/hoogvads/)
TEX = {"hv_goudlijst": "gouden_kroonlijst", "hv_vaandel_tex": "hoogvads_vaandel", "hv_vaandel_boven_tex": "hoogvads_vaandel_boven",
       "hv_vaandel_onder_tex": "hoogvads_vaandel_onder"}
# recipes: base block <- shapeless ingredients (count 2/4); the shapes come from the stonecutter
BASIS = {
    "roze_marmer": (["pink_terracotta", "quartz_block"], 2),
    "wolkmarmer": (["white_terracotta", "quartz_block"], 2),
    "gouden_kroonlijst": (["gold_ingot", "quartz_block"], 2),
    "gouden_dakpan": (["gold_ingot", "pink_terracotta"], 2),
    "walvishuid": (["light_blue_terracotta", "white_wool"], 2),
    "walvisbuik": (["white_terracotta", "white_wool"], 2),
    "hoogvads_vaandel_onder": (["pink_wool", "gold_ingot"], 1),
}
ZAAG = {   # stonecutter: from -> [results]
    "roze_marmer": ["roze_marmer_gepolijst", "roze_marmer_gepolijst_trap", "roze_marmer_gepolijst_plaat", "roze_marmer_gepolijst_muur",
                    "roze_marmer_zuil", "roze_marmer_guhgezicht"],
    "wolkmarmer": ["wolkmarmer_gepolijst", "wolkmarmer_gepolijst_trap", "wolkmarmer_gepolijst_plaat", "wolkmarmer_gepolijst_muur",
                   "wolkmarmer_zuil"],
    "gouden_kroonlijst": ["gouden_kroonlijst_trap", "gouden_kroonlijst_plaat", "gouden_zuilkop"],
    "gouden_dakpan": ["gouden_dakpan_trap", "gouden_dakpan_plaat"],
    "walvishuid": ["walvishuid_licht", "walvishuid_wolkpok"],
    "hoogvads_vaandel_onder": ["hoogvads_vaandel_boven"],
}
GLAS_RECEPT = {"kathedraalglas_guh": "pink_stained_glass", "kathedraalglas_hart": "magenta_stained_glass",
               "kathedraalglas_roos": "purple_stained_glass", "kathedraalglas_ruitjes": "white_stained_glass"}


def _w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


DELEN = {"inner", "outer", "top", "post", "side", "side_tall", "horizontal"}   # model parts of a block (trap_inner, plaat_top, ...)


def _hernoem(o):
    """Concept references -> repo references (models + textures)."""
    s = json.dumps(o)
    for c, n in TEX.items():
        s = s.replace(f'"guhs:block/{c}"', f'"guhs:block/hoogvads/{n}"')
    # texture refs: guhs:block/hv_<tex> -> guhs:block/hoogvads/<tex>; model refs are handled per file (same names)
    import re
    s = re.sub(r'"guhs:block/hv_([a-z0-9_]+)"', lambda m: f'"guhs:block/hoogvads/{TEX.get("hv_" + m.group(1), m.group(1))}"', s)
    return json.loads(s)


def _model_naam(concept_model):
    """hv_goudlijst_trap_inner -> gouden_kroonlijst_trap_inner (block model names, without the prefix)."""
    n = concept_model[3:]
    if n.startswith("goudlijst"):
        n = "gouden_kroonlijst" + n[len("goudlijst"):]
    return n


def bouw(zip_pad=ZIP):
    if not os.path.exists(zip_pad):
        # the resources are committed; the zip is only needed to (re)make them
        if all(os.path.exists(os.path.join(A, "blockstates", f"{k}.json")) for k in BLOKKEN):
            return False
        raise SystemExit(f"vadsrijk_t_z5blokken: concept zip missing: {zip_pad}")
    z = zipfile.ZipFile(zip_pad)
    namen = z.namelist()
    # textures
    os.makedirs(TEXDIR, exist_ok=True)
    for n in namen:
        if n.startswith("assets/guhs/textures/block/hv_") and n.endswith(".png"):
            c = os.path.basename(n)[:-4]
            doel = TEX.get(c, c[3:])
            with open(os.path.join(TEXDIR, doel + ".png"), "wb") as f:
                f.write(z.read(n))
    # models: every concept model of our blocks (and their parts: _trap_inner, _plaat_top, _muur_post, _horizontal ...)
    wil = tuple(CONCEPT[k] for k in BLOKKEN)
    for n in namen:
        if n.startswith("assets/guhs/models/block/hv_") and n.endswith(".json"):
            c = os.path.basename(n)[:-5]
            if not any(c == w_ or (c.startswith(w_ + "_") and c[len(w_) + 1:] in DELEN) for w_ in wil):
                continue
            m = _hernoem(json.loads(z.read(n)))
            m = json.loads(json.dumps(m).replace('"guhs:block/hv_', '"guhs:block/'))   # (no model parents point at hv_ models)
            _w(os.path.join(A, "models", "block", _model_naam(c) + ".json"), m)
    # only keep the textures our models use
    gebruikt = set()
    import re
    for f in os.listdir(os.path.join(A, "models", "block")):
        if f[:-5] in BLOKKEN or any(f[:-5].startswith(k + "_") and f[len(k) + 1:-5] in DELEN for k in BLOKKEN):
            with open(os.path.join(A, "models", "block", f), encoding="utf-8") as fh:
                gebruikt |= set(re.findall(r'guhs:block/hoogvads/([a-z0-9_]+)', fh.read()))
    gebruikt |= {k for k in BLOKKEN if BLOKKEN[k][0] == "blok"}
    for f in os.listdir(TEXDIR):
        if f[:-4] not in gebruikt:
            os.remove(os.path.join(TEXDIR, f))
    # blockstates (model refs renamed)
    for k in BLOKKEN:
        bs = json.loads(z.read(f"assets/guhs/blockstates/{CONCEPT[k]}.json"))
        s = json.dumps(bs)
        import re
        s = re.sub(r'"guhs:block/(hv_[a-z0-9_]+)"', lambda m: f'"guhs:block/{_model_naam(m.group(1))}"', s)
        _w(os.path.join(A, "blockstates", k + ".json"), json.loads(s))
    return True


def extra():
    """Item models, client items, wall inventory models, loot tables, tags, recipes."""
    tags = {"stairs": [], "slabs": [], "walls": [], "mineable/pickaxe": []}
    for k, (soort, nl, en) in BLOKKEN.items():
        model = f"guhs:block/{k}"
        if soort == "muur":
            basis = k[:-len("_muur")]
            _w(os.path.join(A, "models", "block", k + "_inventory.json"),
               {"parent": "minecraft:block/wall_inventory", "textures": {"wall": f"guhs:block/hoogvads/{basis}"}})
            model = f"guhs:block/{k}_inventory"
            tags["walls"].append(k)
        elif soort == "trap":
            tags["stairs"].append(k)
        elif soort == "plaat":
            tags["slabs"].append(k)
        elif soort == "vaandel":
            model = f"guhs:block/{k}"
        _w(os.path.join(A, "models", "item", k + ".json"), {"parent": model})
        _w(os.path.join(A, "items", k + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{k}"}})
        entry = {"type": "minecraft:item", "name": f"guhs:{k}"}
        if soort == "plaat":
            entry["functions"] = [{"function": "minecraft:set_count", "count": 2, "add": False,
                                   "conditions": [{"condition": "minecraft:block_state_property", "block": f"guhs:{k}",
                                                   "properties": {"type": "double"}}]},
                                  {"function": "minecraft:explosion_decay"}]
        pool = {"rolls": 1, "bonus_rolls": 0, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}
        if soort == "glas":
            pool["conditions"] = [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
                {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]
        _w(os.path.join(D, "loot_table", "blocks", k + ".json"), {"type": "minecraft:block", "pools": [pool]})
        if soort not in ("glas", "vaandel"):
            tags["mineable/pickaxe"].append(k)
    for pad, namen in tags.items():
        full = os.path.join(RES, "data", "minecraft", "tags", "block", *pad.split("/")) + ".json"
        d = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
        for n in namen:
            if f"guhs:{n}" not in d["values"]:
                d["values"].append(f"guhs:{n}")
        _w(full, d)
        if pad in ("stairs", "slabs", "walls"):
            full = os.path.join(RES, "data", "minecraft", "tags", "item", pad) + ".json"
            d = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
            for n in namen:
                if f"guhs:{n}" not in d["values"]:
                    d["values"].append(f"guhs:{n}")
            _w(full, d)
    rec = os.path.join(D, "recipe")
    for k, (ingr, n) in BASIS.items():
        _w(os.path.join(rec, f"hoogvads_{k}.json"),
           {"type": "minecraft:crafting_shapeless", "category": "building", "ingredients": ["minecraft:" + i for i in ingr],
            "result": {"id": f"guhs:{k}", "count": n}})
    for van, lijst in ZAAG.items():
        for k in lijst:
            res = {"id": f"guhs:{k}"}
            if BLOKKEN[k][0] == "plaat":
                res["count"] = 2
            _w(os.path.join(rec, f"hoogvads_{k}_zagen.json"), {"type": "minecraft:stonecutting", "ingredient": f"guhs:{van}", "result": res})
    for k, glas in GLAS_RECEPT.items():
        _w(os.path.join(rec, f"hoogvads_{k}.json"),
           {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["GGG", "GOG", "GGG"],
            "key": {"G": "minecraft:" + glas, "O": "minecraft:gold_nugget"}, "result": {"id": f"guhs:{k}", "count": 8}})


def teksten():
    t = {f"block.guhs.{k}": (nl, en) for k, (_, nl, en) in BLOKKEN.items()}
    return t


def build(h):
    bouw()
    extra()
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)    # make_v2.lang(key, en, nl)
    licht_bouw()             # U-Z5BINNEN
    for k, (nl, en) in licht_teksten().items():
        h.lang(k, en, nl)


def _direct(zip_pad):
    nieuw = bouw(zip_pad)
    extra()
    licht_bouw()             # U-Z5BINNEN
    for i, taal in enumerate(("nl_nl", "en_us")):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in teksten().items()})
        data.update({k: v[i] for k, v in licht_teksten().items()})
        _w(p, data)
    print(f"vadsrijk_t_z5blokken: {len(BLOKKEN)} blocks ({'from the concept zip' if nieuw else 'resources kept'}), {len(teksten())} texts")



# ================================================================================================ U-Z5BINNEN: light blocks
# The Hoogvads light blocks for the interiors (Java: feature/vadsrijk/zone5/binnen/Z5Licht.java, same ids). The models are
# generated here (small gold/candle/crystal elements; flames and crystals with "light_emission"), the 3 textures are drawn once
# into textures/block/hoogvads_licht/ (committed; only redrawn when missing).
LICHT = {
    "kroonluchter": ("Kroonluchter", "Chandelier"),
    "gouden_kaarsenrand": ("Gouden kaarsenrand", "Golden Candle Rail"),
    "kristallen_hanger": ("Kristallen hanger", "Crystal Pendant"),
    "gouden_wandluchter": ("Gouden wandluchter", "Golden Wall Sconce"),
    "gouden_kandelaar": ("Gouden kandelaar", "Golden Candelabrum"),
    "gouden_kroonlijst_muur": ("Gouden kroonlijstmuurtje", "Golden Cornice Wall"),
    "gouden_lichtlijst": ("Gouden lichtlijst", "Glowing Golden Cornice"),
}
LICHT_TEXDIR = os.path.join(A, "textures", "block", "hoogvads_licht")
_LT = "guhs:block/hoogvads_licht/"
_TEXS = {"goud": "minecraft:block/gold_block", "kaars": _LT + "kaars", "vlam": _LT + "vlam", "kristal": _LT + "kristal",
         "particle": "minecraft:block/gold_block"}


def _licht_texturen():
    os.makedirs(LICHT_TEXDIR, exist_ok=True)
    nodig = [n for n in ("kaars", "vlam", "kristal") if not os.path.exists(os.path.join(LICHT_TEXDIR, n + ".png"))]
    if not nodig:
        return
    from PIL import Image
    for n in nodig:
        im = Image.new("RGBA", (16, 16))
        for x in range(16):
            for y in range(16):
                if n == "kaars":      # warm white wax, a soft vertical sheen
                    k = 1.0 - 0.06 * abs(x - 5) / 5 - (0.04 if y > 12 else 0)
                    c = (int(252 * k), int(244 * k), int(222 * k))
                elif n == "vlam":     # white-yellow core, orange edge
                    d = min(1.0, ((x - 7.5) ** 2 / 30 + (y - 9) ** 2 / 50) ** 0.5)
                    c = (255, int(250 - 80 * d), int(215 - 175 * d))
                else:                 # crystal: pale with lavender facets and white glints
                    f = ((x + y) // 4 + (x - y + 16) // 5) % 3
                    c = [(236, 236, 255), (214, 206, 246), (248, 246, 255)][f]
                    if (x + 2 * y) % 11 == 0:
                        c = (255, 255, 255)
                im.putpixel((x, y), c + (255,))
        im.save(os.path.join(LICHT_TEXDIR, n + ".png"))


def _el(f, t, tex, emit=0, rot=None):
    """A model element with all 6 faces (uv sized to the face, inside the texture)."""
    faces = {}
    for d in ("north", "south", "east", "west", "up", "down"):
        if d in ("north", "south"):
            w, h = t[0] - f[0], t[1] - f[1]
        elif d in ("east", "west"):
            w, h = t[2] - f[2], t[1] - f[1]
        else:
            w, h = t[0] - f[0], t[2] - f[2]
        w, h = min(max(w, 0.5), 16), min(max(h, 0.5), 16)
        u0, v0 = (16 - w) / 2, (16 - h) / 2
        faces[d] = {"uv": [round(u0, 3), round(v0, 3), round(u0 + w, 3), round(v0 + h, 3)], "texture": "#" + tex}
    e = {"from": [round(v, 3) for v in f], "to": [round(v, 3) for v in t], "faces": faces}
    if rot:
        e["rotation"] = rot
    if emit:
        e["light_emission"] = emit
    return e


def _staaf(cx, cz, y, hoek, lengte, dik, hoog, tex="goud"):
    """A horizontal bar centred on (cx, y, cz) pointing at `hoek` degrees in the x-z plane (atan2(z, x)); multiples of 22.5.
    Model y-rotation (like the game): an x-bar turned by a points at -a, a z-bar at 90 - a; |a| <= 45."""
    def norm(a):
        return (a + 90) % 180 - 90
    th = norm(-hoek)
    if abs(th) <= 45.01:
        f, t = (cx - lengte / 2, y, cz - dik / 2), (cx + lengte / 2, y + hoog, cz + dik / 2)
    else:
        th = norm(90 - hoek)
        f, t = (cx - dik / 2, y, cz - lengte / 2), (cx + dik / 2, y + hoog, cz + lengte / 2)
    th = round(th / 22.5) * 22.5
    rot = {"origin": [round(cx, 3), round(y, 3), round(cz, 3)], "axis": "y", "angle": th} if abs(th) > 0.01 else None
    return _el(f, t, tex, rot=rot)


def _ring(r, y, n=16, dik=1.0, hoog=1.0, c=8.0):
    import math
    L = 2 * r * math.sin(math.pi / n) + 0.35
    return [_staaf(c + r * math.cos(2 * math.pi * k / n), c + r * math.sin(2 * math.pi * k / n), y, 360.0 * k / n + 90, L, dik, hoog)
            for k in range(n)]


def _spaak(r0, r1, y, hoek, dik=0.9, hoog=0.9, c=8.0):
    import math
    rm = (r0 + r1) / 2
    a = math.radians(hoek)
    return _staaf(c + rm * math.cos(a), c + rm * math.sin(a), y, hoek, r1 - r0, dik, hoog)


def _kaars(x, z, y, h, dik=1.4, kop=True):
    """A cup + candle + flame standing on y."""
    d = dik / 2
    uit = []
    if kop:
        uit.append(_el((x - d - 0.5, y, z - d - 0.5), (x + d + 0.5, y + 0.6, z + d + 0.5), "goud"))
        y += 0.6
    uit.append(_el((x - d, y, z - d), (x + d, y + h, z + d), "kaars"))
    uit.append(_el((x - 0.4, y + h, z - 0.4), (x + 0.4, y + h + 1.6, z + 0.4), "vlam", emit=15))
    return uit


def _druppel(x, z, y_top, lang, emit=10):
    """A crystal drop hanging from y_top: a rod of crystal and a faceted (45 degree) drop under it."""
    yb = y_top - lang
    return [_el((x - 0.45, yb, z - 0.45), (x + 0.45, y_top, z + 0.45), "kristal", emit=emit),
            _el((x - 0.75, yb - 1.6, z - 0.75), (x + 0.75, yb, z + 0.75), "kristal", emit=emit,
                rot={"origin": [round(x, 3), round(yb, 3), round(z, 3)], "axis": "y", "angle": 45}),
            _el((x - 0.3, yb - 2.4, z - 0.3), (x + 0.3, yb - 1.6, z + 0.3), "kristal", emit=emit)]


def _model(elements):
    return {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": dict(_TEXS), "elements": elements}


def licht_modellen():
    import math
    M = {}
    # ---- kroonluchter: one block, a round chandelier 3 blocks wide: a hook + rod, a crown ring with 8 candles, the big ring
    #      (r 17 px) on 8 arms with 16 candles and 16 crystal drops, a bowl with a small crystal ring and a drop at the bottom
    e = [_el((7, 14.5, 7), (9, 16, 9), "goud"), _el((5.5, 13.6, 5.5), (10.5, 14.5, 10.5), "goud"),
         _el((7.4, -7, 7.4), (8.6, 13.6, 8.6), "goud"), _el((6.4, 9, 6.4), (9.6, 10.4, 9.6), "goud"),
         _el((5.8, 0, 5.8), (10.2, 1.8, 10.2), "goud")]
    e += _ring(7.5, 8, n=16, dik=0.8, hoog=0.8)
    for k in range(8):
        e.append(_spaak(1, 7.5, 8, 45 * k, dik=0.7, hoog=0.7))
        a = math.radians(45 * k + 22.5)
        e += _kaars(8 + 7.5 * math.cos(a), 8 + 7.5 * math.sin(a), 8.8, 2.6, dik=1.1)
    e += _ring(17, 1, n=16, dik=1.2, hoog=1.1)
    e += _ring(15.5, -0.4, n=16, dik=0.6, hoog=0.6)
    for k in range(8):
        e.append(_spaak(2, 17, 0.6, 45 * k, dik=0.9, hoog=0.9))
    for k in range(16):
        a = math.radians(22.5 * k)
        x, z = 8 + 17 * math.cos(a), 8 + 17 * math.sin(a)
        e += _kaars(x, z, 2.1, 3.6)
        e += _druppel(x, z, 1, 3.5 if k % 2 else 1.8)
    e += [_el((5, -6, 5), (11, -3.5, 11), "goud"), _el((6.3, -3.5, 6.3), (9.7, 0, 9.7), "goud"),
          _el((7, -9, 7), (9, -6, 9), "goud")]
    e += _ring(6.5, -5, n=16, dik=0.6, hoog=0.6)
    for k in range(8):
        a = math.radians(45 * k + 22.5)
        e += _druppel(8 + 6.5 * math.cos(a), 8 + 6.5 * math.sin(a), -5, 2.6)
    e += _druppel(8, 8, -9, 2.2, emit=12)
    M["kroonluchter"] = _model(e)
    # ---- gouden_kaarsenrand: node (cup), arm (north, turned for the others), candle, crystal drop
    M["gouden_kaarsenrand_kern"] = _model([_el((6.5, 1.4, 6.5), (9.5, 3, 9.5), "goud"), _el((7, 0.4, 7), (9, 1.4, 9), "goud")])
    M["gouden_kaarsenrand_arm"] = _model([_el((7.3, 1.8, 0), (8.7, 2.8, 6.5), "goud"), _el((7.6, 0.6, 0), (8.4, 1.2, 7), "goud"),
                                          _el((7.2, 1.6, 2.6), (8.8, 3.1, 3.6), "goud")])
    M["gouden_kaarsenrand_kaars"] = _model(_kaars(8, 8, 3, 5.2, dik=1.6))
    M["gouden_kaarsenrand_kristal"] = _model(_druppel(8, 8, 0.6, 3.6))
    # ---- kristallen_hanger: a gold cap, a gold wire, two crystal beads and a big faceted drop
    M["kristallen_hanger"] = _model([
        _el((7, 15, 7), (9, 16, 9), "goud"), _el((7.8, 8, 7.8), (8.2, 15, 8.2), "goud"),
        _el((7.2, 12, 7.2), (8.8, 13.6, 8.8), "kristal", emit=10),
        _el((7.2, 9, 7.2), (8.8, 10.6, 8.8), "kristal", emit=10, rot={"origin": [8, 9.8, 8], "axis": "y", "angle": 45}),
        _el((6.6, 3, 6.6), (9.4, 8, 9.4), "kristal", emit=10, rot={"origin": [8, 5.5, 8], "axis": "y", "angle": 45}),
        _el((7.3, 1, 7.3), (8.7, 3, 8.7), "kristal", emit=10), _el((7.7, 0, 7.7), (8.3, 1, 8.3), "kristal", emit=10)])
    # ---- gouden_wandluchter (facing north: the wall is at z 16, it sticks out to z 0): back plate, arm, crossbar, 3 candles
    e = [_el((5.5, 3, 15), (10.5, 13, 16), "goud"), _el((6.8, 5.6, 13.6), (9.2, 8.2, 15), "goud"),
         _el((7.4, 6.4, 8), (8.6, 7.4, 13.6), "goud"), _el((2, 6.4, 7.6), (14, 7.4, 8.6), "goud"),
         _el((2, 7.4, 7.6), (3, 9, 8.6), "goud"), _el((13, 7.4, 7.6), (14, 9, 8.6), "goud"),
         _el((7.4, 7.4, 7.6), (8.6, 10, 8.6), "goud")]
    e += _kaars(2.5, 8.1, 9, 3.4, dik=1.3) + _kaars(13.5, 8.1, 9, 3.4, dik=1.3) + _kaars(8, 8.1, 10, 4.2, dik=1.4)
    e += _druppel(8, 8.1, 6.4, 2.2, emit=8) + _druppel(2.5, 8.1, 6.4, 1.2, emit=8) + _druppel(13.5, 8.1, 6.4, 1.2, emit=8)
    M["gouden_wandluchter"] = _model(e)
    # ---- gouden_kandelaar: base, stem with knots, a cross of arms at 22.5, five candles (the model is 2 blocks tall)
    e = [_el((4, 0, 4), (12, 1, 12), "goud"), _el((5, 1, 5), (11, 2, 11), "goud"), _el((6.5, 2, 6.5), (9.5, 3, 9.5), "goud"),
         _el((7.2, 3, 7.2), (8.8, 22.5, 8.8), "goud"), _el((6.2, 8, 6.2), (9.8, 9.5, 9.8), "goud"),
         _el((6, 15, 6), (10, 16.5, 10), "goud"), _el((6.5, 21, 6.5), (9.5, 22.5, 9.5), "goud"),
         _el((2, 22.5, 7.5), (14, 23.5, 8.5), "goud"), _el((7.5, 22.5, 2), (8.5, 23.5, 14), "goud")]
    e += _kaars(8, 8, 23.5, 5.2, dik=1.6)
    for (x, z) in ((2.5, 8), (13.5, 8), (8, 2.5), (8, 13.5)):
        e += _kaars(x, z, 23.5, 4.0, dik=1.3)
    M["gouden_kandelaar"] = _model(e)
    # ---- wall + glowing cornice
    tex = "guhs:block/hoogvads/gouden_kroonlijst"
    M["gouden_kroonlijst_muur_post"] = {"parent": "minecraft:block/template_wall_post", "textures": {"wall": tex}}
    M["gouden_kroonlijst_muur_side"] = {"parent": "minecraft:block/template_wall_side", "textures": {"wall": tex}}
    M["gouden_kroonlijst_muur_side_tall"] = {"parent": "minecraft:block/template_wall_side_tall", "textures": {"wall": tex}}
    M["gouden_kroonlijst_muur_inventory"] = {"parent": "minecraft:block/wall_inventory", "textures": {"wall": tex}}
    M["gouden_lichtlijst"] = {"parent": "minecraft:block/cube_all", "textures": {"all": tex}}
    return M


def licht_blockstates():
    g = "guhs:block/"
    zij = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

    def apply(model, y, **kw):
        a = {"model": g + model}
        if y:
            a["y"] = y
        a.update(kw)
        return a
    bs = {
        "kroonluchter": {"variants": {"": {"model": g + "kroonluchter"}}},
        "kristallen_hanger": {"variants": {"": {"model": g + "kristallen_hanger"}}},
        "gouden_kandelaar": {"variants": {"": {"model": g + "gouden_kandelaar"}}},
        "gouden_lichtlijst": {"variants": {"": {"model": g + "gouden_lichtlijst"}}},
        "gouden_wandluchter": {"variants": {f"facing={f}": apply("gouden_wandluchter", y) for f, y in zij}},
    }
    mp = [{"apply": {"model": g + "gouden_kaarsenrand_kern"}},
          {"when": {"kaars": "true"}, "apply": {"model": g + "gouden_kaarsenrand_kaars"}},
          {"when": {"kristal": "true"}, "apply": {"model": g + "gouden_kaarsenrand_kristal"}}]
    mp += [{"when": {f: "true"}, "apply": apply("gouden_kaarsenrand_arm", y)} for f, y in zij]
    bs["gouden_kaarsenrand"] = {"multipart": mp}
    w = [{"when": {"up": "true"}, "apply": {"model": g + "gouden_kroonlijst_muur_post"}}]
    for soort, m in (("low", "gouden_kroonlijst_muur_side"), ("tall", "gouden_kroonlijst_muur_side_tall")):
        w += [{"when": {f: soort}, "apply": apply(m, y, uvlock=True)} for f, y in zij]
    bs["gouden_kroonlijst_muur"] = {"multipart": w}
    return bs


def licht_bouw():
    _licht_texturen()
    for n, m in licht_modellen().items():
        _w(os.path.join(A, "models", "block", n + ".json"), m)
    for n, b in licht_blockstates().items():
        _w(os.path.join(A, "blockstates", n + ".json"), b)
    for k in LICHT:
        model = f"guhs:block/{k}_inventory" if k == "gouden_kroonlijst_muur" else f"guhs:block/{k}"
        _w(os.path.join(A, "models", "item", k + ".json"), {"parent": model})
        _w(os.path.join(A, "items", k + ".json"), {"model": {"type": "minecraft:model", "model": f"guhs:item/{k}"}})
        _w(os.path.join(D, "loot_table", "blocks", k + ".json"),
           {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{k}"}],
                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    tags = {("block", "mineable/pickaxe"): list(LICHT), ("block", "walls"): ["gouden_kroonlijst_muur"],
            ("item", "walls"): ["gouden_kroonlijst_muur"]}
    for (soort, pad), namen in tags.items():
        full = os.path.join(RES, "data", "minecraft", "tags", soort, *pad.split("/")) + ".json"
        d = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
        for n in namen:
            if f"guhs:{n}" not in d["values"]:
                d["values"].append(f"guhs:{n}")
        _w(full, d)
    rec = os.path.join(D, "recipe")
    recepten = {
        "kroonluchter": (["minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:candle", "minecraft:amethyst_shard"], 1),
        "gouden_kaarsenrand": (["minecraft:gold_nugget", "minecraft:gold_nugget", "minecraft:candle"], 2),
        "kristallen_hanger": (["minecraft:gold_nugget", "minecraft:amethyst_shard"], 2),
        "gouden_wandluchter": (["minecraft:gold_ingot", "minecraft:candle"], 2),
        "gouden_kandelaar": (["minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:candle"], 1),
        "gouden_lichtlijst": (["guhs:gouden_kroonlijst", "minecraft:glowstone_dust"], 1),
    }
    for k, (ingr, n) in recepten.items():
        _w(os.path.join(rec, f"hoogvads_{k}.json"),
           {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingr, "result": {"id": f"guhs:{k}", "count": n}})
    _w(os.path.join(rec, "hoogvads_gouden_kroonlijst_muur_zagen.json"),
       {"type": "minecraft:stonecutting", "ingredient": "guhs:gouden_kroonlijst", "result": {"id": "guhs:gouden_kroonlijst_muur"}})


def licht_teksten():
    return {f"block.guhs.{k}": v for k, v in LICHT.items()}


if __name__ == "__main__":
    a = sys.argv[1:]
    _direct(a[a.index("--zip") + 1] if "--zip" in a else ZIP)
