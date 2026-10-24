"""
1.7.0 "Onder de Wol", ronde 25/26 (design: MineScapeStory/ontwerp-guhs170-mijnen.md): no real lava in the Barbecuether,
everywhere kaasfrituursaus. Java: feature/barbecuether/BbqZonderLava.java (the carver guhs:sausgrot and the one-time
conversion of chunks that already exist).

What this module does (all idempotent, new chunks only):
  - configured_carver/barbecuegrot.json -> type guhs:sausgrot. Vanilla's minecraft:nether_cave ignores lava_level and
    fills every cave below y 31 with real lava; that was the only real lava the Barbecuether generated (its sea, springs
    and deltas are kaasfrituursaus already). barbecuether.py makes it this way too now.
  - checks the noise settings (default fluid kaasfrituursaus, aquifers off, lava router zero) and every configured
    feature / processor list / template pool of the dimension for minecraft:lava, and patches what it finds;
  - every structure template (.nbt) of a Barbecuether structure (jigsaw pools and guhs:burcht tiles reachable from a
    structure whose biomes are Barbecuether biomes): minecraft:lava in the palette becomes guhs:kaasfrituursaus with
    the same level. (At the time of writing none of them held lava; this keeps it that way after a re-export.)

    python tools/features/bbq_zonder_lava.py          (from the project root)
    python tools/features/bbq_zonder_lava.py --check  (only report, change nothing; exit 1 if lava is found)
"""
import glob
import gzip
import io
import json
import os
import struct
import sys

R = os.path.join("src", "main", "resources")
D = os.path.join(R, "data", "guhs")
SAUS = "guhs:kaasfrituursaus"
LAVA = ("minecraft:lava", "minecraft:flowing_lava")
PROCESSORS = set()   # (processor lists used by the Barbecuether's pools, filled by bbq_templates)
BIOMES = {"guhs:houtskoolvlakte", "guhs:asdal", "guhs:satebos", "guhs:worstenwoud", "guhs:rookdelta"}


# --- a small NBT reader / writer (keeps every tag type as it is) ------------------------------------------------------
class Tag:
    __slots__ = ("t", "v")

    def __init__(self, t, v):
        self.t, self.v = t, v


def _read(f, t):
    if t == 1:
        return struct.unpack(">b", f.read(1))[0]
    if t == 2:
        return struct.unpack(">h", f.read(2))[0]
    if t == 3:
        return struct.unpack(">i", f.read(4))[0]
    if t == 4:
        return struct.unpack(">q", f.read(8))[0]
    if t == 5:
        return struct.unpack(">f", f.read(4))[0]
    if t == 6:
        return struct.unpack(">d", f.read(8))[0]
    if t == 7:
        n = struct.unpack(">i", f.read(4))[0]
        return f.read(n)
    if t == 8:
        n = struct.unpack(">H", f.read(2))[0]
        return f.read(n).decode("utf-8")
    if t == 9:
        et, n = struct.unpack(">bi", f.read(5))
        return (et, [_read(f, et) for _ in range(n)])
    if t == 10:
        d = {}
        while True:
            ct = f.read(1)[0]
            if ct == 0:
                return d
            name = f.read(struct.unpack(">H", f.read(2))[0]).decode("utf-8")
            d[name] = Tag(ct, _read(f, ct))
    if t == 11:
        n = struct.unpack(">i", f.read(4))[0]
        return list(struct.unpack(f">{n}i", f.read(4 * n)))
    if t == 12:
        n = struct.unpack(">i", f.read(4))[0]
        return list(struct.unpack(f">{n}q", f.read(8 * n)))
    raise ValueError(f"unknown NBT tag {t}")


def _write(f, t, v):
    if t == 1:
        f.write(struct.pack(">b", v))
    elif t == 2:
        f.write(struct.pack(">h", v))
    elif t == 3:
        f.write(struct.pack(">i", v))
    elif t == 4:
        f.write(struct.pack(">q", v))
    elif t == 5:
        f.write(struct.pack(">f", v))
    elif t == 6:
        f.write(struct.pack(">d", v))
    elif t == 7:
        f.write(struct.pack(">i", len(v)) + v)
    elif t == 8:
        b = v.encode("utf-8")
        f.write(struct.pack(">H", len(b)) + b)
    elif t == 9:
        et, items = v
        f.write(struct.pack(">bi", et if items else (et or 0), len(items)))
        for i in items:
            _write(f, et, i)
    elif t == 10:
        for name, tag in v.items():
            b = name.encode("utf-8")
            f.write(struct.pack(">bH", tag.t, len(b)) + b)
            _write(f, tag.t, tag.v)
        f.write(b"\x00")
    elif t == 11:
        f.write(struct.pack(f">i{len(v)}i", len(v), *v))
    elif t == 12:
        f.write(struct.pack(f">i{len(v)}q", len(v), *v))
    else:
        raise ValueError(f"unknown NBT tag {t}")


def nbt_load(path):
    raw = open(path, "rb").read()
    data = gzip.decompress(raw) if raw[:2] == b"\x1f\x8b" else raw
    f = io.BytesIO(data)
    t = f.read(1)[0]
    name = f.read(struct.unpack(">H", f.read(2))[0]).decode("utf-8")
    return name, Tag(t, _read(f, t))


def nbt_save(path, name, root):
    f = io.BytesIO()
    b = name.encode("utf-8")
    f.write(struct.pack(">bH", root.t, len(b)) + b)
    _write(f, root.t, root.v)
    with open(path, "wb") as out:
        out.write(gzip.compress(f.getvalue(), mtime=0))


def _palette_fix(state):
    """One palette entry (compound): lava -> kaasfrituursaus with the same level. True when it changed."""
    name = state.get("Name")
    if name is None or name.v not in LAVA:
        return False
    name.v = SAUS
    props = state.get("Properties")
    if props is None:
        state["Properties"] = Tag(10, {"level": Tag(8, "0")})
    elif "level" not in props.v:
        props.v["level"] = Tag(8, "0")
    return True


def fix_template(path, write=True):
    """Lava in this template's palette(s) -> kaasfrituursaus. Returns how many palette entries changed."""
    name, root = nbt_load(path)
    n = 0
    palettes = []
    if "palette" in root.v:
        palettes.append(root.v["palette"].v[1])
    if "palettes" in root.v:
        palettes += [p[1] for p in root.v["palettes"].v[1]]
    for pal in palettes:
        for state in pal:
            n += _palette_fix(state)
    if n and write:
        nbt_save(path, name, root)
    return n


# --- which templates belong to the Barbecuether -------------------------------------------------------------------------
def _biome_set(ref):
    refs = [ref] if isinstance(ref, str) else list(ref or [])
    out = set()
    for r in refs:
        if r.startswith("#guhs:"):
            p = os.path.join(D, "tags", "worldgen", "biome", r[6:] + ".json")
            if os.path.exists(p):
                out |= _biome_set(json.load(open(p, encoding="utf-8"))["values"])
        else:
            out.add(r)
    return out


def _loc(rid):
    ns, path = rid.split(":", 1)
    return os.path.join(R, "data", ns, "structure", path + ".nbt")


def _pool_templates(pool_id, seen):
    if pool_id in seen or not pool_id.startswith("guhs:"):
        return set()
    seen.add(pool_id)
    p = os.path.join(D, "worldgen", "template_pool", pool_id[5:] + ".json")
    if not os.path.exists(p):
        return set()
    out = set()

    def walk(el):
        if isinstance(el, dict):
            if "location" in el and isinstance(el["location"], str):
                out.add(_loc(el["location"]))
            if isinstance(el.get("processors"), str) and el["processors"].startswith("guhs:"):
                PROCESSORS.add(el["processors"])
            for v in el.values():
                walk(v)
        elif isinstance(el, list):
            for v in el:
                walk(v)
    pool = json.load(open(p, encoding="utf-8"))
    walk(pool.get("elements", []))
    # jigsaw blocks inside the templates point at further pools
    for t in list(out):
        if os.path.exists(t):
            _, root = nbt_load(t)
            for block in root.v.get("blocks", Tag(9, (10, []))).v[1]:
                nbt = block.get("nbt")
                pool = nbt.v.get("pool") if nbt is not None else None
                if pool is not None and isinstance(pool.v, str):
                    out |= _pool_templates(pool.v, seen)
    return out


def bbq_templates():
    out, seen, structures = set(), set(), []
    for f in sorted(glob.glob(os.path.join(D, "worldgen", "structure", "*.json"))):
        s = json.load(open(f, encoding="utf-8"))
        biomes = _biome_set(s.get("biomes"))
        if not biomes or not biomes <= BIOMES:
            continue
        structures.append(os.path.basename(f)[:-5])
        for key in ("start_pool", "surface_pool"):
            if isinstance(s.get(key), str):
                out |= _pool_templates(s[key], seen)
        if isinstance(s.get("templates"), str):   # guhs:burcht: tiles in structure/<templates>/
            ns, path = s["templates"].split(":", 1)
            out |= set(glob.glob(os.path.join(R, "data", ns, "structure", path, "**", "*.nbt"), recursive=True))
    return structures, sorted(t for t in out if os.path.exists(t))


# --- the json side ----------------------------------------------------------------------------------------------------
def _json_lava(obj):
    """Replace {"Name": "minecraft:lava"...} block states in any json (in place). Returns the count."""
    n = 0
    if isinstance(obj, dict):
        if obj.get("Name") in LAVA:
            obj["Name"] = SAUS
            obj.setdefault("Properties", {}).setdefault("level", "0")
            n += 1
        for k, v in obj.items():
            if isinstance(v, str) and v in LAVA and k in ("block", "state"):
                obj[k] = SAUS
                n += 1
            else:
                n += _json_lava(v)
    elif isinstance(obj, list):
        for v in obj:
            n += _json_lava(v)
    return n


def _w(path, obj):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _features_of_bbq():
    """The configured features placed by the Barbecuether biomes (via their placed features)."""
    out = set()
    for b in BIOMES:
        biome = json.load(open(os.path.join(D, "worldgen", "biome", b[5:] + ".json"), encoding="utf-8"))
        for step in biome.get("features", []):
            for pf in step:
                if not pf.startswith("guhs:"):
                    continue
                p = os.path.join(D, "worldgen", "placed_feature", pf[5:] + ".json")
                if os.path.exists(p):
                    feat = json.load(open(p, encoding="utf-8")).get("feature")
                    if isinstance(feat, str) and feat.startswith("guhs:"):
                        out.add(os.path.join(D, "worldgen", "configured_feature", feat[5:] + ".json"))
    return sorted(p for p in out if os.path.exists(p))


def run(write=True):
    gevonden = []
    # 1. the carver
    carver = os.path.join(D, "worldgen", "configured_carver", "barbecuegrot.json")
    c = json.load(open(carver, encoding="utf-8"))
    if c["type"] != "guhs:sausgrot":
        gevonden.append(f"{carver}: {c['type']} -> guhs:sausgrot")
        c["type"] = "guhs:sausgrot"
        if write:
            _w(carver, c)
    # 2. the noise settings
    ns_path = os.path.join(D, "worldgen", "noise_settings", "barbecuether.json")
    ns = json.load(open(ns_path, encoding="utf-8"))
    if ns["default_fluid"]["Name"] != SAUS:
        gevonden.append(f"{ns_path}: default_fluid {ns['default_fluid']['Name']}")
        ns["default_fluid"] = {"Name": SAUS, "Properties": {"level": "0"}}
    if ns.get("aquifers_enabled"):
        gevonden.append(f"{ns_path}: aquifers on (they place lava deep down)")
        ns["aquifers_enabled"] = False
    if ns["noise"]["min_y"] < -54 and ns["sea_level"] > -54:
        gevonden.append(f"{ns_path}: below y -54 the generator fills caves with lava (min_y {ns['noise']['min_y']})")
    n = _json_lava(ns)
    if n:
        gevonden.append(f"{ns_path}: {n} lava states")
    if write and gevonden and any(ns_path in g for g in gevonden):
        _w(ns_path, ns)
    # 3. the features, processors and pools of the dimension
    structures, templates = bbq_templates()
    processors = [os.path.join(D, "worldgen", "processor_list", pid[5:] + ".json") for pid in sorted(PROCESSORS)]
    for p in _features_of_bbq() + [p for p in processors if os.path.exists(p)]:
        obj = json.load(open(p, encoding="utf-8"))
        n = _json_lava(obj)
        if n:
            gevonden.append(f"{p}: {n} lava states")
            if write:
                _w(p, obj)
    # 4. the structure templates
    for t in templates:
        n = fix_template(t, write)
        if n:
            gevonden.append(f"{t}: {n} lava palette entries")
    print(f"bbq_zonder_lava: {len(structures)} Barbecuether structures, {len(templates)} templates checked; "
          f"{len(gevonden)} {'fixed' if write else 'found'}")
    for g in gevonden:
        print("  " + g)
    return gevonden


if __name__ == "__main__":
    check = "--check" in sys.argv
    found = run(write=not check)
    sys.exit(1 if check and found else 0)
