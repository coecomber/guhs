"""
Loads the Guhs data (src/main/resources/data) into a plain Minecraft 26.1.2 dedicated server as a datapack and reports every
file the game refuses (worldgen registries, recipes, loot tables, advancements, tags, trades, ...).

It does not need the mod to compile: everything the vanilla game does not know (our blocks, items, entities, sounds, and the
feature/structure/placement/pool element types our Java code registers) is replaced by a vanilla stand-in first, so what
remains are real format errors in the JSON. The real check stays runGameTestServer once the mod compiles.

    python tools/check_datapack26.py [--keep] [--gen] [--res <resources dir>]
                                          (from the project root; needs the Gradle caches of the 26.1.2 build)

    --gen   also generates a few chunks in each Guhs dimension (catches worldgen errors that only show while generating)
    --keep  keeps build/check26 for a look at the transformed datapack and the server log
"""
import glob
import json
import os
import re
import shutil
import subprocess
import sys
import time

R = sys.argv[sys.argv.index("--res") + 1] if "--res" in sys.argv else os.path.join("src", "main", "resources")
OUT = os.path.join("build", "check26")
HOME = os.path.expanduser("~")
SERVER_JAR = os.path.join(HOME, ".gradle", "caches", "neoformruntime", "artifacts", "minecraft_26.1.2_server.jar")


def java25():
    for d in sorted(glob.glob(os.path.join(HOME, ".gradle", "jdks", "*25*"))):
        exe = os.path.join(d, "bin", "java.exe" if os.name == "nt" else "java")
        if os.path.exists(exe):
            return exe
    sys.exit("no JDK 25 in ~/.gradle/jdks (build the mod once, foojay downloads it)")


def reports():
    """The vanilla registries (registries.json) and block states (blocks.json) of 26.1.2."""
    rep = os.path.join("build", "vanilla26", "reports")
    if not os.path.exists(os.path.join(rep, "registries.json")):
        os.makedirs(os.path.join("build", "vanilla26"), exist_ok=True)
        subprocess.run([java25(), "-DbundlerMainClass=net.minecraft.data.Main", "-jar", SERVER_JAR, "--reports", "--output",
                        os.path.join("build", "vanilla26")], cwd=".", check=True, stdout=subprocess.DEVNULL)
    regs = json.load(open(os.path.join(rep, "registries.json")))
    blocks = json.load(open(os.path.join(rep, "blocks.json")))
    return {k: set(v["entries"]) for k, v in regs.items()}, blocks


REGS, BLOCKS = None, None
PACK_IDS = set()      # ids our datapack defines itself (worldgen, loot tables, ...)
REPLACED = {}         # (kind, id) -> count


def own(ref):
    return isinstance(ref, str) and ref.lstrip("#").startswith("guhs:")


def known(reg, ref):
    """In the vanilla registry `reg` (our datapack's own ids count for the registries it fills)."""
    return ref in REGS.get(reg, ()) or (reg not in STATIC and ref in PACK_IDS)


STATIC = {"minecraft:block", "minecraft:item", "minecraft:entity_type", "minecraft:sound_event", "minecraft:fluid",
          "minecraft:particle_type", "minecraft:point_of_interest_type"}


def stand_in(kind, ref):
    REPLACED[(kind, ref)] = REPLACED.get((kind, ref), 0) + 1
    return {"block": "minecraft:stone", "item": "minecraft:stick", "entity": "minecraft:pig", "sound": "minecraft:ambient.cave",
            "particle": "minecraft:flame", "fluid": "minecraft:water", "enchantment": "minecraft:unbreaking",
            "effect": "minecraft:speed", "component": "minecraft:custom_data"}.get(kind, "minecraft:stone")


def fix_state(state):
    name = state.get("Name")
    if own(name) and not known("minecraft:block", name):
        state = {"Name": stand_in("block", name)}
    return state


BLOCK_KEYS = {"block", "blocks", "valid_blocks", "can_place_on", "can_grow_through", "replaceable_blocks"}
ITEM_KEYS = {"item", "items", "id", "name", "base", "addition", "template", "ingredient", "ingredients", "icon"}


def scrub(o, key=None, parent=None):
    """Replaces the ids only our Java code knows by vanilla stand-ins, by the JSON key they are under."""
    if isinstance(o, list):
        return [scrub(x, key, parent) for x in o]
    if isinstance(o, dict):
        if "Name" in o and isinstance(o.get("Name"), str):
            o = fix_state(o)
        cond = o.get("condition") or o.get("function")
        if cond in ("minecraft:block_state_property", "minecraft:copy_state") and own(o.get("block"))                 and not known("minecraft:block", o["block"]):
            o = {k: v for k, v in o.items() if k != "properties"}
            if cond == "minecraft:copy_state":
                o["properties"] = []
        out = {}
        for k, v in o.items():
            if k == "type" and isinstance(v, str) and own(v) and ("minCount" in o or "weight" in o and "maxCount" in o):
                out[k] = v if known("minecraft:entity_type", v) else stand_in("entity", v)
            elif k == "type" and isinstance(v, str) and own(v) and key in ("predicate", "entity", "vehicle", "passenger", "child", "parent", "partner"):
                out[k] = v if known("minecraft:entity_type", v) else stand_in("entity", v)
            elif k == "type" and own(v) and key in ("particle", "options", "default_dripstone_particle"):
                out[k] = stand_in("particle", v)
            elif k in ("name", "value") and o.get("type") == "minecraft:loot_table":
                out[k] = v
            elif key == "key" and isinstance(v, str):     # shaped recipe keys
                out[k] = scrub(v, "item", key)
            elif key == "components" or k == "include" and isinstance(v, list):
                if key == "components" and own(k):
                    REPLACED[("component", k)] = 1
                    continue
                out[k] = [c for c in v if not own(c)] if k == "include" else scrub(v, k, key)
            else:
                out[k] = scrub(v, k, key)
        return out
    if isinstance(o, str) and own(o) and not o.startswith("#"):
        if key in ITEM_KEYS:
            return o if known("minecraft:item", o) else stand_in("item", o)
        if key in BLOCK_KEYS:
            return o if known("minecraft:block", o) else stand_in("block", o)
        if key in ("sound", "loop", "sound_event"):
            return o if known("minecraft:sound_event", o) else stand_in("sound", o)
        if key in ("entity_type", "entity_types"):
            return o if known("minecraft:entity_type", o) else stand_in("entity", o)
        if o in PACK_IDS:
            return o
        if key in ("effect", "effects", "id") and parent in ("effects", "minecraft:potion_contents", "custom_effects"):
            return stand_in("effect", o)
        return o if known("minecraft:item", o) else stand_in("item", o)
    return o


# --- our own worldgen types -> vanilla ones (the fields the game knows are still checked) -----------------------------

def vanilla_structure(d):
    t = d.get("type")
    if not own(t):
        return d
    common = {k: d[k] for k in ("biomes", "step", "spawn_overrides", "terrain_adaptation") if k in d}
    if isinstance(d.get("jigsaw"), dict):
        return vanilla_structure(common | d["jigsaw"])
    if "start_pool" in d:
        j = {"type": "minecraft:jigsaw", "start_pool": d["start_pool"], "size": 1, "start_height": {"absolute": 0},
             "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": min(int(d.get("max_distance_from_center", 80)), 116),
             "use_expansion_hack": False}
        if "start_jigsaw_name" in d:
            j["start_jigsaw_name"] = d["start_jigsaw_name"]
        return j | common
    REPLACED[("structure_type", t)] = 1
    return {"type": "minecraft:buried_treasure"} | common


def vanilla_features(o):
    if isinstance(o, dict) and o.get("type") == "minecraft:spring_feature":
        st = o["config"]["state"]
        if own(st.get("Name")):
            REPLACED[("fluid", st["Name"])] = 1
            o["config"]["state"] = {"Name": "minecraft:water", "Properties": {"falling": "true"}}
    if isinstance(o, list):
        out = []
        for x in o:
            if isinstance(x, dict) and own(x.get("type")) and ("placement" not in x):
                REPLACED[("placement_modifier", x["type"])] = 1
                continue  # our placement modifiers
            out.append(vanilla_features(x))
        return out
    if isinstance(o, dict):
        if own(o.get("type")) and "config" in o:
            REPLACED[("feature_type", o["type"])] = 1
            return {"type": "minecraft:no_op", "config": {}}
        return {k: vanilla_features(v) for k, v in o.items()}
    return o


def vanilla_pool(d):
    for e in d.get("elements", []):
        el = e.get("element", {})
        if el.get("element_type") == "guhs:grond_single_pool_element":
            REPLACED[("pool_element", "guhs:grond_single_pool_element")] = 1
            el["element_type"] = "minecraft:single_pool_element"
            el.pop("ground_level_delta", None)
    return d


def vanilla_processors(d):
    procs = [p for p in d.get("processors", []) if not own(p.get("processor_type"))]
    if len(procs) != len(d.get("processors", [])):
        REPLACED[("processor", "guhs:*")] = 1
    d["processors"] = procs
    return d


def vanilla_structure_set(d):
    p = d.get("placement", {})
    if own(p.get("type")):
        REPLACED[("structure_placement", p["type"])] = 1
        d["placement"] = {"type": "minecraft:random_spread", "spacing": 32, "separation": 8, "salt": int(p.get("salt", 1))}
    return d


def tag_values(d, reg):
    vals = []
    for v in d.get("values", []):
        ref = v["id"] if isinstance(v, dict) else v
        if ref.startswith("#c:") or ref.startswith("#neoforge:"):
            continue  # NeoForge's common tags
        if own(ref) and not ref.startswith("#") and reg and not known(reg, ref):
            REPLACED[(reg, ref)] = REPLACED.get((reg, ref), 0) + 1
            continue
        vals.append(v)
    d["values"] = vals
    return d


TAG_REGS = {"block": "minecraft:block", "item": "minecraft:item", "entity_type": "minecraft:entity_type", "fluid": "minecraft:fluid",
            "point_of_interest_type": "minecraft:point_of_interest_type", "painting_variant": None, "damage_type": None,
            "villager_trade": None}


# --- build the pack ----------------------------------------------------------------------------------------------------

SKIP = [os.path.join("data", "neoforge"), os.path.join("data", "guhs", "neoforge"), os.path.join("data", "guhs", "loot_modifiers"),
        os.path.join("data", "guhs", "favorietjes"), os.path.join("data", "guhs", "guheinde")]


def pack_ids(data_root):
    """guhs:<path> of every file in our data folders (the registries our datapack fills itself)."""
    ids = set()
    for path in glob.glob(os.path.join(data_root, "*", "**", "*.json"), recursive=True):
        rel = os.path.relpath(path, data_root).replace(os.sep, "/")
        ns, rest = rel.split("/", 1)
        parts = rest[:-5].split("/")
        depth = 2 if parts[0] in ("worldgen", "tags", "neoforge") else 1  # data/guhs/worldgen/biome/x.json
        if len(parts) > depth:
            ids.add(f"{ns}:{'/'.join(parts[depth:])}")
    for path in glob.glob(os.path.join(data_root, "*", "structure", "**", "*.nbt"), recursive=True):
        rel = os.path.relpath(path, os.path.join(data_root, os.path.relpath(path, data_root).split(os.sep)[0], "structure"))
        ids.add("guhs:" + rel[:-4].replace(os.sep, "/"))
    return ids


def build_pack():
    global REGS, BLOCKS
    REGS, BLOCKS = reports()
    world = os.path.join(OUT, "world")
    pack = os.path.join(world, "datapacks", "guhs")
    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(pack)
    src_data = os.path.join(R, "data")
    PACK_IDS.update(pack_ids(src_data))
    json.dump({"pack": {"description": "Guhs data check", "min_format": [101, 1], "max_format": 101}},
              open(os.path.join(pack, "pack.mcmeta"), "w"))
    for path in glob.glob(os.path.join(src_data, "**", "*"), recursive=True):
        if os.path.isdir(path):
            continue
        rel = os.path.relpath(path, R)
        if any(rel.startswith(s + os.sep) for s in SKIP):
            continue
        dst = os.path.join(pack, rel)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        if not path.endswith(".json"):
            shutil.copyfile(path, dst)
            continue
        d = json.load(open(path, encoding="utf-8"))
        parts = rel.replace(os.sep, "/").split("/")
        folder = parts[2]
        sub = parts[3] if len(parts) > 4 else ""
        if folder == "tags":
            d = tag_values(d, TAG_REGS.get(sub, "minecraft:block") if sub != "worldgen" else None)
        else:
            if folder == "worldgen" and sub == "structure":
                d = vanilla_structure(d)
            elif folder == "worldgen" and sub in ("configured_feature", "placed_feature"):
                d = vanilla_features(d)
            elif folder == "worldgen" and sub == "template_pool":
                d = vanilla_pool(d)
            elif folder == "worldgen" and sub == "processor_list":
                d = vanilla_processors(d)
            elif folder == "worldgen" and sub == "structure_set":
                d = vanilla_structure_set(d)
            elif folder == "worldgen" and sub == "biome":
                d = vanilla_features(d)
            if folder == "dimension_type":
                d.get("attributes", {}).pop("neoforge:custom_skybox", None)
                d.get("attributes", {}).pop("neoforge:custom_weather_effects", None)
            if folder == "worldgen" and sub == "biome":
                d.get("attributes", {}).pop("neoforge:custom_skybox", None)
                d.get("attributes", {}).pop("neoforge:custom_weather_effects", None)
            d = scrub(d)
        json.dump(d, open(dst, "w", encoding="utf-8"), indent=1)
    return world


def run_server(world, gen):
    run_dir = OUT
    open(os.path.join(run_dir, "eula.txt"), "w").write("eula=true\n")
    open(os.path.join(run_dir, "server.properties"), "w").write(
        "level-name=world\nonline-mode=false\nspawn-protection=0\nmax-tick-time=-1\nsync-chunk-writes=false\n"
        "level-type=minecraft\\:flat\ngenerate-structures=false\nserver-port=25599\n")
    log_path = os.path.join(run_dir, "server.log")
    cmds = []
    if gen:
        for dim in ("guhmension", "guhmaag", "barbecuether", "guheinde"):
            cmds.append(f"execute in guhs:{dim} run forceload add -64 -64 64 64")
    cmds.append("stop")
    with open(log_path, "w", encoding="utf-8") as log:
        proc = subprocess.Popen([java25(), "-Xmx3G", "-jar", os.path.abspath(SERVER_JAR), "nogui"], cwd=run_dir,
                                stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
        start = time.time()
        while proc.poll() is None and time.time() - start < 600:
            text = open(log_path, encoding="utf-8", errors="replace").read()
            if "Done (" in text:
                for c in cmds:
                    proc.stdin.write(c + "\n")
                    proc.stdin.flush()
                    time.sleep(15 if "forceload" in c else 1)
                break
            time.sleep(1)
        try:
            proc.wait(timeout=300)
        except subprocess.TimeoutExpired:
            proc.kill()
    return open(log_path, encoding="utf-8", errors="replace").read()


def main():
    if not os.path.isdir(R):
        sys.exit("run from the project root: python tools/check_datapack26.py")
    world = build_pack()
    log = run_server(world, "--gen" in sys.argv)
    problems = []
    lines = log.splitlines()
    for i, line in enumerate(lines):
        if re.search(r"/(ERROR|WARN)\]", line) and ("guhs" in line or "registr" in line.lower() or "Failed" in line):
            block = [line] + [l for l in lines[i + 1:i + 40] if not re.match(r"^\[\d\d:\d\d:\d\d\]", l)]
            problems.append("\n".join(block))
        elif line.startswith("> ") or re.match(r"^\s+- ", line) and "guhs" in line:
            problems.append(line)
    kinds = {}
    for (kind, ref), n in REPLACED.items():
        kinds.setdefault(kind, 0)
        kinds[kind] += 1
    print("stand-ins used (ids only our Java code knows): " + ", ".join(f"{k} {n}" for k, n in sorted(kinds.items())))
    if "Done (" not in log:
        problems.insert(0, "the server did not start (see build/check26/server.log)")
    print("\n\n".join(problems) if problems else "datapack loads without errors")
    if "--keep" not in sys.argv and not problems:
        shutil.rmtree(OUT, ignore_errors=True)


if __name__ == "__main__":
    main()
