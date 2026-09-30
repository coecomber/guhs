"""
Checks the assets: every block has a blockstate, every item a model, every model/texture that is referenced exists,
and every block/item/entity has an English and a Dutch name.

Run from the project root:  python tools/check_assets.py
"""
import glob
import json
import os
import re

A = os.path.join("src", "main", "resources", "assets", "guhs")
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs")
problems = []


def registered(kind):
    """Names registered in Mod<kind>.java (plus the ones made in loops)."""
    src = open(os.path.join(JAVA, "registry", f"Mod{kind}.java"), encoding="utf-8").read()
    names = set(re.findall(r'register(?:Block|Item|SimpleBlock|SimpleItem)?\(\s*"([a-z0-9_]+)"', src))
    return names


blocks = registered("Blocks")
blocks |= {f"lampion_{c}" for c in ("roze", "geel", "mint")}
colours = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
           "brown", "green", "red", "black"]
blocks |= {f"{c}_{k}" for c in colours for k in ("zitzak", "kussen")}
blocks -= {"lampion_", "potted_"}
items = registered("Items") | {f"macaron_{c}" for c in ("roze", "mint", "citroen", "choco")}
items.discard("macaron_")
# clothes are registered in a loop over GuhClothes
clothes_src = open(os.path.join(JAVA, "entity", "GuhClothes.java"), encoding="utf-8").read()
items |= {m.lower() for m in re.findall(r'^\s{4}([A-Z_]+)\(', clothes_src, re.M)}
# block items (registerSimpleBlockItem) use the block's name
block_items = set(re.findall(r'registerSimpleBlockItem\(ModBlocks\.([A-Z_]+)\)', open(os.path.join(JAVA, "registry", "ModItems.java"), encoding="utf-8").read()))
block_items = {b.lower() for b in block_items}
block_items |= {"guh_stoel", "guh_tafel", "guh_bank", "guh_kast", "vlaggetjes", "kaasbloem", "guhoortjes", "roze_guhbloem", "knabbelroos",
                "roze_gras", "zaadbak"} | {f"lampion_{c}" for c in ("roze", "geel", "mint")} | {f"{c}_{k}" for c in colours for k in ("zitzak", "kussen")}
entities = set(re.findall(r'ENTITY_TYPES\.register\("([a-z0-9_]+)"', open(os.path.join(JAVA, "registry", "ModEntities.java"), encoding="utf-8").read()))

no_state = {"guh_portal", "kaas_saus", "maagzuur"}
for b in sorted(blocks):
    if not os.path.exists(f"{A}/blockstates/{b}.json"):
        problems.append(f"block {b}: no blockstate")
for i in sorted(items | block_items):
    if not os.path.exists(f"{A}/models/item/{i}.json"):
        problems.append(f"item {i}: no item model")


def tex_exists(ref):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    if ns != "guhs":
        return True
    return os.path.exists(f"{A}/textures/{path}.png")


def model_exists(ref):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    return ns != "guhs" or os.path.exists(f"{A}/models/{path}.json")


for path in glob.glob(f"{A}/blockstates/*.json"):
    data = json.load(open(path, encoding="utf-8"))
    for v in data.get("variants", {}).values():
        for m in (v if isinstance(v, list) else [v]):
            if not model_exists(m["model"]):
                problems.append(f"{os.path.basename(path)}: missing model {m['model']}")
for path in glob.glob(f"{A}/models/**/*.json", recursive=True):
    data = json.load(open(path, encoding="utf-8"))
    if "parent" in data and not model_exists(data["parent"]):
        problems.append(f"{path}: missing parent {data['parent']}")
    for t in data.get("textures", {}).values():
        if not t.startswith("#") and not tex_exists(t):
            problems.append(f"{os.path.relpath(path, A)}: missing texture {t}")
    for o in data.get("overrides", []):
        if not model_exists(o["model"]):
            problems.append(f"{path}: missing override model {o['model']}")

for lang in ("en_us", "nl_nl"):
    names = json.load(open(f"{A}/lang/{lang}.json", encoding="utf-8"))
    for b in blocks:
        if f"block.guhs.{b}" not in names and b not in ("guh_wheel_part", "slee_rail_part"):
            problems.append(f"{lang}: no name for block {b}")
    for i in items:
        if f"item.guhs.{i}" not in names and not os.path.exists(f"{A}/blockstates/{i}.json"):
            problems.append(f"{lang}: no name for item {i}")
    for e in entities:
        if f"entity.guhs.{e}" not in names and e != "guh_seat":
            problems.append(f"{lang}: no name for entity {e}")

for e in entities:
    if e in ("guh_seat", "guh", "mika", "guh_npc", "mika_baas", "quest_guh", "nether_mika", "guh_slime"):
        continue
    if not os.path.exists(f"{A}/geo/entity/{e}.geo.json"):
        problems.append(f"entity {e}: no geo model")
    if not os.path.exists(f"{A}/textures/entity/{e}.png"):
        problems.append(f"entity {e}: no texture")

print("\n".join(problems) if problems else "all assets present")
print(f"({len(blocks)} blocks, {len(items | block_items)} items, {len(entities)} entities checked)")
