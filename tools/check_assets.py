"""
Checks the assets: every block has a blockstate, every item a model and a 26.1 client item definition (assets/guhs/items),
every model/texture that is referenced exists (also the minecraft: ones, against the 26.1.2 client jar when the Gradle caches
have it), no 1.21.1-only model features are left, the equipment/GeckoLib assets are in their 26.1 places, and every
block/item/entity has an English and a Dutch name.

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


# Minecraft 26.1.2 itself (for the minecraft: parents/textures/models we use), if the Gradle caches have its client jar
VANILLA = set()
_jar = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "neoformruntime", "artifacts", "minecraft_26.1.2_client.jar")
if os.path.exists(_jar):
    import zipfile
    VANILLA = set(zipfile.ZipFile(_jar).namelist())


def sprite(t):
    return t["sprite"] if isinstance(t, dict) else t


def tex_exists(ref):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    if ns == "minecraft":
        return not VANILLA or f"assets/minecraft/textures/{path}.png" in VANILLA
    if ns != "guhs":
        return True
    return os.path.exists(f"{A}/textures/{path}.png")


def model_exists(ref):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    if ns == "minecraft":
        return not VANILLA or f"assets/minecraft/models/{path}.json" in VANILLA
    return ns != "guhs" or os.path.exists(f"{A}/models/{path}.json")


for path in glob.glob(f"{A}/blockstates/*.json"):
    data = json.load(open(path, encoding="utf-8"))
    used = [m for v in data.get("variants", {}).values() for m in (v if isinstance(v, list) else [v])]
    used += [m for part in data.get("multipart", []) for m in (part["apply"] if isinstance(part["apply"], list) else [part["apply"]])]
    for m in used:
        if not model_exists(m["model"]):
            problems.append(f"{os.path.basename(path)}: missing model {m['model']}")
for path in glob.glob(f"{A}/models/**/*.json", recursive=True):
    data = json.load(open(path, encoding="utf-8"))
    rel = os.path.relpath(path, A)
    if "parent" in data and not model_exists(data["parent"]):
        problems.append(f"{rel}: missing parent {data['parent']}")
    for t in data.get("textures", {}).values():
        t = sprite(t)
        if not t.startswith("#") and not tex_exists(t):
            problems.append(f"{rel}: missing texture {t}")
    # a face names a texture variable; a path there is "Missing texture references" in the game: purple and black
    for e in data.get("elements", []):
        for side, face in e.get("faces", {}).items():
            if not face.get("texture", "").startswith("#"):
                problems.append(f"{rel}: face {side} names the texture {face.get('texture')!r} directly (must be a #variable)")
                break
        else:
            continue
        break
    # gone in 26.1 (tools/mc26.py converts them): item model overrides, render_type, NeoForge model loaders, spawn egg template
    for key in ("overrides", "render_type", "loader"):
        if key in data:
            problems.append(f"{rel}: '{key}' does not exist in 26.1 (run tools/mc26.py)")
    if data.get("parent") in ("minecraft:item/template_spawn_egg", "builtin/entity"):
        problems.append(f"{rel}: parent {data['parent']} does not exist in 26.1")


# --- 26.1 client item definitions: assets/guhs/items/<item>.json for every item --------------------------------------
ITEM_MODEL_TYPES = {"minecraft:model", "minecraft:condition", "minecraft:select", "minecraft:range_dispatch", "minecraft:special",
                    "minecraft:composite", "minecraft:empty", "minecraft:bundle/selected_item"}


def item_model_refs(m, where):
    if not isinstance(m, dict):
        return
    t = m.get("type")
    if t not in ITEM_MODEL_TYPES:
        problems.append(f"{where}: unknown item model type {t}")
    if t == "minecraft:model" and not model_exists(m["model"]):
        problems.append(f"{where}: missing model {m['model']}")
    for k in ("on_true", "on_false", "fallback"):
        item_model_refs(m.get(k), where)
    for c in m.get("cases", []) + m.get("entries", []):
        item_model_refs(c.get("model"), where)
    for c in m.get("models", []):
        item_model_refs(c, where)


defs = {os.path.basename(p)[:-5] for p in glob.glob(f"{A}/items/*.json")}
for name in defs:
    item_model_refs(json.load(open(f"{A}/items/{name}.json", encoding="utf-8")).get("model"), f"items/{name}.json")
referenced = set()
for name in defs:
    referenced |= set(re.findall(r'"guhs:item/([a-z0-9_]+)"', open(f"{A}/items/{name}.json", encoding="utf-8").read())) - {name}
for i in sorted(items | block_items):
    if i not in defs:
        problems.append(f"item {i}: no client item definition (assets/guhs/items/{i}.json)")
# every other item registered anywhere in the code (feature packages) by a literal name
for java in glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True):
    for name in re.findall(r'(?:registerItem|registerSimpleItem|registerSimpleBlockItem)\(\s*"([a-z0-9_]+)"', open(java, encoding="utf-8").read()):
        if name not in defs and not name.endswith("_"):
            problems.append(f"item {name} ({os.path.basename(java)}): no client item definition")
for path in glob.glob(f"{A}/models/item/*.json"):
    name = os.path.basename(path)[:-5]
    if name not in defs and name not in referenced:
        problems.append(f"models/item/{name}.json: no items/{name}.json (and no item definition uses it)")


# --- equipment assets, GeckoLib folders ---------------------------------------------------------------------------------
for path in glob.glob(f"{A}/equipment/*.json"):
    for layer, entries in json.load(open(path, encoding="utf-8"))["layers"].items():
        for e in entries:
            ns, tex = e["texture"].split(":", 1)
            if not os.path.exists(f"{A}/textures/entity/equipment/{layer}/{tex}.png"):
                problems.append(f"equipment/{os.path.basename(path)}: missing textures/entity/equipment/{layer}/{tex}.png")
for old in ("geo", "animations", os.path.join("textures", "models", "armor")):
    if os.path.isdir(os.path.join(A, old)):
        problems.append(f"assets/guhs/{old}: 1.21.1 folder (GeckoLib 5 reads geckolib/models|animations, armour is equipment)")
for path in glob.glob(f"{A}/geckolib/animations/**/*.json", recursive=True):
    if not json.load(open(path, encoding="utf-8")).get("animations"):
        problems.append(f"{os.path.relpath(path, A)}: no animations (GeckoLib 5 refuses it in dev)")

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
    if not os.path.exists(f"{A}/geckolib/models/entity/{e}.geo.json"):
        problems.append(f"entity {e}: no geo model")
    if not os.path.exists(f"{A}/textures/entity/{e}.png"):
        problems.append(f"entity {e}: no texture")

print("\n".join(problems) if problems else "all assets present")
print(f"({len(blocks)} blocks, {len(items | block_items)} items, {len(entities)} entities checked)")
