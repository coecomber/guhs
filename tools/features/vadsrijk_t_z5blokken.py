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


def _direct(zip_pad):
    nieuw = bouw(zip_pad)
    extra()
    for i, taal in enumerate(("nl_nl", "en_us")):
        p = os.path.join(A, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in teksten().items()})
        _w(p, data)
    print(f"vadsrijk_t_z5blokken: {len(BLOKKEN)} blocks ({'from the concept zip' if nieuw else 'resources kept'}), {len(teksten())} texts")


if __name__ == "__main__":
    a = sys.argv[1:]
    _direct(a[a.index("--zip") + 1] if "--zip" in a else ZIP)
