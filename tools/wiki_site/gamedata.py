"""
Reads the game data straight from the project, so the wiki always matches the mod (and picks up changes from a merged tree
when it is run again):

  * lang (assets/guhs/lang/*.json): the names of everything, tooltips, Guhdex texts, emotes, effects, books ...
  * Java enums: GuhVariant (the Guhdex pages), GuhNpcEntity.Kind (the guh characters), GuhClothes (+ where every piece
    comes from: KledingBronnen.bron / KledingBronLijst), the superkompas categories (SuperkompasItem.CATEGORIES)
  * data/guhs: worldgen structures + structure sets + biome tags, biomes + dimensions, biome modifiers (spawns),
    loot tables, recipes, advancements
  * tools/make_ftbquests.py: the FTB quest chapters, sections and quests
"""
import glob
import json
import os
import re
import runpy
import sys
from functools import cached_property

NS = "guhs"


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def read_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


_COMMENT_RE = re.compile(r"""//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'""", re.S)


def strip_java_comments(src):
    """Removes // and /* */ comments (not inside strings or chars)."""
    return _COMMENT_RE.sub(lambda m: m.group(0) if m.group(0)[0] in "\"'" else "", src)


def split_top(text, sep=","):
    """Splits on sep at bracket depth 0 (outside strings)."""
    parts, depth, cur, i = [], 0, [], 0
    while i < len(text):
        c = text[i]
        if c == '"':
            j = i + 1
            while j < len(text) and text[j] != '"':
                j += 2 if text[j] == "\\" else 1
            cur.append(text[i:j + 1])
            i = j + 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        if c == sep and depth == 0:
            parts.append("".join(cur))
            cur = []
        else:
            cur.append(c)
        i += 1
    if "".join(cur).strip():
        parts.append("".join(cur))
    return parts


def enum_constants(src, name):
    """[(CONSTANT, [args...])] of `enum name { ... ; }` in (comment-free) Java source."""
    m = re.search(r"\benum\s+" + name + r"\s*(?:implements[^{]*)?\{", src)
    if not m:
        return []
    i, depth, start = m.end(), 0, m.end()
    body_end = None
    while i < len(src):
        c = src[i]
        if c == '"':
            j = i + 1
            while src[j] != '"':
                j += 2 if src[j] == "\\" else 1
            i = j + 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            if depth == 0:
                body_end = i
                break
            depth -= 1
        elif c == ";" and depth == 0:
            body_end = i
            break
        i += 1
    out = []
    for part in split_top(src[start:body_end]):
        part = part.strip()
        mm = re.match(r"([A-Z][A-Z0-9_]*)\s*(?:\((.*)\))?\s*(?:\{.*\})?$", part, re.S)
        if mm:
            args = [a.strip() for a in split_top(mm.group(2))] if mm.group(2) else []
            out.append((mm.group(1), args))
    return out


def unquote(s):
    s = s.strip()
    return s[1:-1] if len(s) >= 2 and s[0] == s[-1] == '"' else s


def pretty(ident):
    """kaas_knabbels -> Kaas knabbels (only a fallback when there is no name)."""
    s = ident.replace("_", " ").strip()
    return s[:1].upper() + s[1:]


class Game:
    def __init__(self, root):
        root = os.path.abspath(root)
        self.root = root
        self.res = os.path.join(root, "src", "main", "resources")
        self.assets = os.path.join(self.res, "assets", NS)
        self.data = os.path.join(self.res, "data", NS)
        self.java = os.path.join(root, "src", "main", "java", "nl", "juiced", "guhs")
        self.tools = os.path.join(root, "tools")

    # --- lang -----------------------------------------------------------------------------------------------------------
    @cached_property
    def lang_nl(self):
        return read_json(os.path.join(self.assets, "lang", "nl_nl.json"))

    @cached_property
    def lang_en(self):
        return read_json(os.path.join(self.assets, "lang", "en_us.json"))

    def tr(self, key, default=None, lang="nl"):
        d = self.lang_nl if lang == "nl" else self.lang_en
        return d.get(key, default if default is not None else self.lang_nl.get(key))

    def item_name(self, rid):
        """'guhs:x' / 'minecraft:y' / 'x' -> a display name."""
        ns, path = rid.split(":", 1) if ":" in rid else (NS, rid)
        if ns == NS:
            return self.lang_nl.get(f"item.{NS}.{path}") or self.lang_nl.get(f"block.{NS}.{path}") or pretty(path)
        return VANILLA_NAMES.get(path) or pretty(path)

    # --- Java -------------------------------------------------------------------------------------------------------------
    def java_src(self, rel):
        return strip_java_comments(read(os.path.join(self.java, rel)))

    @cached_property
    def java_files(self):
        return sorted(glob.glob(os.path.join(self.java, "**", "*.java"), recursive=True))

    def java_stripped(self, f):
        cache = self.__dict__.setdefault("_stripped", {})
        if f not in cache:
            cache[f] = strip_java_comments(read(f))
        return cache[f]

    @cached_property
    def int_constants(self):
        """{ClassName: {CONST: int}} for every `static final int X = 12` (and a few simple sums)."""
        out = {}
        for f in self.java_files:
            cls = os.path.splitext(os.path.basename(f))[0]
            raw = read(f)
            if "static final int" not in raw:
                continue
            src = self.java_stripped(f)
            consts = {}
            for m in re.finditer(r"static\s+final\s+int\s+([^;]+);", src):
                for part in split_top(m.group(1)):
                    mm = re.match(r"\s*([A-Z][A-Z0-9_]*)\s*=\s*(-?\d+)\s*$", part)
                    if mm:
                        consts[mm.group(1)] = int(mm.group(2))
            out[cls] = consts
        return out

    def resolve_int(self, expr, cls):
        expr = expr.strip()
        if re.fullmatch(r"-?\d+", expr):
            return int(expr)
        if "." in expr:
            c, name = expr.rsplit(".", 1)
            c = c.split(".")[-1]
            return self.int_constants.get(c, {}).get(name)
        v = self.int_constants.get(cls, {}).get(expr)
        if v is None:   # a static import / a constant of a neighbour class
            for consts in self.int_constants.values():
                if expr in consts:
                    return consts[expr]
        return v

    def resolve_string_expr(self, expr, cls):
        """'12 + " visbonnen"' / 'PRIJS_X + " munten"' / '"tekst"' -> the text (None when unknown)."""
        out = []
        for part in split_top(expr, "+"):
            part = part.strip()
            if part.startswith('"'):
                out.append(unquote(part))
            else:
                v = self.resolve_int(part, cls)
                if v is None:
                    return None
                out.append(str(v))
        return "".join(out)

    # --- the Guhdex: GuhVariant ---------------------------------------------------------------------------------------------
    @cached_property
    def npc_kinds(self):
        src = self.java_src(os.path.join("entity", "GuhNpcEntity.java"))
        return [(n.lower(), float(a[0].rstrip("fF")) if a else 1.0) for n, a in enum_constants(src, "Kind")]

    @cached_property
    def variants(self):
        """Every Guhdex page: dict(id, weight, bones, kind) with kind variant / verhaal / npc / creature."""
        src = self.java_src(os.path.join("entity", "GuhVariant.java"))
        consts = enum_constants(src, "GuhVariant")
        names = [n for n, _ in consts]
        first_char = names.index("REISGUH") if "REISGUH" in names else len(names)
        verhaal = set(re.findall(r"this\s*==\s*([A-Z_]+)", src[src.find("isVerhaalGuh()"):src.find("isVerhaalGuh()") + 400]))
        kinds = {k for k, _ in self.npc_kinds}
        out = []
        for i, (n, args) in enumerate(consts):
            vid = n.lower()
            weight = int(args[0]) if args and re.fullmatch(r"\d+", args[0]) else 0
            bones = [unquote(a) for a in args[1:]]
            if n in verhaal:
                kind = "verhaal"
            elif i >= first_char:
                kind = "npc" if vid in kinds else "creature"
            else:
                kind = "variant"
            out.append(dict(id=vid, weight=weight, bones=bones, kind=kind, order=i,
                            rarity=self.lang_nl.get(f"gui.{NS}.guhdex.rarity.{vid}", ""),
                            info=self.lang_nl.get(f"gui.{NS}.guhdex.info.{vid}", "")))
        roll = re.search(r"ROLL_OUT_OF\s*=\s*(\d+)", src)
        self.variant_roll_out_of = int(roll.group(1)) if roll else 1000
        return out

    # --- clothes ------------------------------------------------------------------------------------------------------------
    @cached_property
    def clothes(self):
        """[dict(id, slot, block, bron, prijs)] in enum order (hair = slot haar, no bron)."""
        raw = read(os.path.join(self.java, "entity", "GuhClothes.java"))
        # the marker block each piece sits in (// <balto> ... // </balto>): a hint for its source
        block_of, cur = {}, None
        for line in raw.splitlines():
            m = re.match(r"\s*//\s*<(/?)(\w+)>", line)
            if m:
                cur = None if m.group(1) else m.group(2)
                continue
            m = re.match(r"\s*([A-Z][A-Z0-9_]*)\(Slot\.", line)
            if m and cur:
                block_of[m.group(1)] = cur
        src = strip_java_comments(raw)
        out = []
        for n, args in enum_constants(src, "GuhClothes"):
            slot = args[0].split(".")[-1].lower() if args else "?"
            out.append(dict(id=n.lower(), const=n, slot=slot, bones=[unquote(a) for a in args[1:]], block=block_of.get(n)))
        bron, prijs, loose = self._clothing_sources([c["const"] for c in out])
        for c in out:   # a loop over a list we couldn't read: every piece in that feature's marker block
            if c["const"] not in bron and c["block"] in loose:
                bron[c["const"]] = loose[c["block"]]
        wild = re.search(r"WILD_OUTFITS\s*=\s*List\.of\((.*?)\);", src, re.S)
        wild_ids = set(re.findall(r"\b([A-Z][A-Z0-9_]+)\b", wild.group(1))) if wild else set()
        for c in out:
            c["bron"] = bron.get(c["const"])
            c["prijs"] = prijs.get(c["const"])
            c["wild"] = c["const"] in wild_ids
        return out

    def class_src(self, name):
        """The (comment-free) source of the class with this simple name, or None."""
        if not hasattr(self, "_class_files"):
            self._class_files = {os.path.splitext(os.path.basename(f))[0]: f for f in self.java_files}
        f = self._class_files.get(name)
        return self.java_stripped(f) if f else None

    def _clothing_sources(self, consts):
        bron, prijs, loose = {}, {}, {}
        const_set = set(consts)
        for f in self.java_files:
            if "GameTests" in f or "Screen" in f:
                continue
            raw = read(f)
            if "KledingBronnen" not in raw and "b(\"" not in raw:
                continue
            src = self.java_stripped(f)
            cls = os.path.splitext(os.path.basename(f))[0]
            strings = dict(re.findall(r'static\s+final\s+String\s+([A-Z_]+)\s*=\s*"([^"]*)"', src))

            def sval(expr, here=src, depth=0):
                """A string argument: a literal, a constant of this class, or (bbq2) a constant of another class
                (RingBeloning.BRON), also when that constant is itself another class's constant (BRON = GuhrioKasteel.GROEP)."""
                expr = expr.strip()
                if expr.startswith('"'):
                    return unquote(expr)
                if here is src and re.fullmatch(r"[A-Z_]+", expr) and expr in strings:
                    return strings[expr]
                m = re.fullmatch(r"(?:(\w+)\.)?([A-Z_][A-Z0-9_]*)", expr)
                if not m or depth > 4:
                    return None
                other = self.class_src(m.group(1)) if m.group(1) else here
                d = re.search(r"static\s+final\s+String\s+" + m.group(2) + r"\s*=\s*([^;]+);", other or "")
                return sval(d.group(1), other, depth + 1) if d else None
            # KledingBronLijst: b("bron", X, "prijs")
            if cls == "KledingBronLijst":
                for m in re.finditer(r'\bb\(\s*"(\w+)"\s*,\s*([A-Z0-9_]+)\s*,\s*([^;]+?)\)\s*;', src):
                    bron[m.group(2)] = m.group(1)
                    p = self.resolve_string_expr(m.group(3), cls)
                    if p:
                        prijs[m.group(2)] = p
                continue
            for m in re.finditer(r"KledingBronnen\.bron\(([^;]*?)\)\s*;", src):
                args = split_top(m.group(1))
                if len(args) < 2:
                    continue
                target = args[0].strip().split(".")[-1]
                b = sval(args[1])
                if b is None:
                    continue
                p = self.resolve_string_expr(args[2], cls) if len(args) > 2 else None
                if p is None and len(args) > 2:      # (bbq2) KledingBronnen.prijs("lang key"): the text of that key, when it takes no number
                    pm = re.fullmatch(r'KledingBronnen\.prijs\(\s*"([\w.]+)"\s*\)', args[2].strip())
                    if pm and "%" not in self.lang_nl.get(pm.group(1), "%"):
                        p = self.lang_nl[pm.group(1)]
                if target in const_set:
                    bron[target] = b
                    if p:
                        prijs[target] = p
                else:   # a loop: for (GuhClothes c : LIST) / List.of(...)
                    before = src[:m.start()]
                    loop = list(re.finditer(r"for\s*\(\s*GuhClothes\s+\w+\s*:\s*([^)]*(?:\([^)]*\))?[^)]*)\)", before))
                    if not loop:
                        continue
                    expr = loop[-1].group(1)
                    names = re.findall(r"GuhClothes\.([A-Z0-9_]+)", expr)
                    if not names:
                        lm = re.search(re.escape(expr.strip()) + r"\s*=\s*List\.of\((.*?)\);", src, re.S)
                        if lm:
                            names = re.findall(r"\b([A-Z][A-Z0-9_]+)\b", lm.group(1))
                    if not names:       # (bbq2) a list of another class: for (GuhClothes c : RingBeloning.KLEDING)
                        qm = re.fullmatch(r"(\w+)\.([A-Z_][A-Z0-9_]*)", expr.strip())
                        lm = qm and re.search(r"\b" + qm.group(2) + r"\s*=\s*List\.of\((.*?)\);", self.class_src(qm.group(1)) or "", re.S)
                        if lm:
                            names = re.findall(r"\b([A-Z][A-Z0-9_]+)\b", lm.group(1))
                    if not names:
                        loose[os.path.basename(os.path.dirname(f))] = b
                    for nm in names:
                        if nm in const_set:
                            bron[nm] = b
                            if p:
                                prijs[nm] = p
        # the beroepen: each Beroep enum knows its bron and its clothes
        bv = os.path.join(self.java, "feature", "beroepen", "BeroepenVoortgang.java")
        if os.path.exists(bv):
            src = strip_java_comments(read(bv))
            for n, args in enum_constants(src, "Beroep"):
                bron_id = next((unquote(a) for a in args if a.strip().startswith('"beroep_')), None)
                pieces = re.findall(r"GuhClothes\.([A-Z0-9_]+)", ",".join(args))
                for p in pieces:
                    if bron_id and p not in bron:
                        bron[p] = bron_id
        return bron, prijs, loose

    @cached_property
    def kledingbron_names(self):
        pre = f"gui.{NS}.kledingbron."
        return {k[len(pre):]: v for k, v in self.lang_nl.items() if k.startswith(pre)}

    # --- the superkompas --------------------------------------------------------------------------------------------------
    @cached_property
    def superkompas(self):
        """[(category id, [(kopje or None, [structure ids])])]"""
        src = self.java_src(os.path.join("item", "SuperkompasItem.java"))
        body = src[src.find("CATEGORIES = List.of("):]
        body = body[:body.find(");\n")]
        out = []
        for part in split_top(body[body.find("(") + 1:]):
            part = part.strip()
            m = re.match(r'cat\(\s*"(\w+)"(.*)\)$', part, re.S)
            if m:
                ids = [unquote(a) for a in split_top(m.group(2)) if a.strip().startswith('"')][1:]
                out.append((m.group(1), [(None, ids)]))
                continue
            m = re.match(r'new\s+Category\(\s*"(\w+)"(.*)\)$', part, re.S)
            if m:
                kopjes = [(k, re.findall(r'"(\w+)"', lst)) for k, lst in
                          re.findall(r'new\s+Kopje\(\s*("?\w+"?)\s*,\s*List\.of\(([^)]*)\)\)', m.group(2))]
                out.append((m.group(1), [(None if k == "null" else unquote(k), ids) for k, ids in kopjes]))
        return out

    # --- worldgen -------------------------------------------------------------------------------------------------------------
    def biome_tag(self, tag, _seen=None):
        """'#guhs:has_structure/x' -> [biome ids] (vanilla tags stay as '#minecraft:...')."""
        ns, path = tag.lstrip("#").split(":", 1)
        if ns != NS:
            return [tag]
        f = os.path.join(self.data, "tags", "worldgen", "biome", path + ".json")
        if not os.path.exists(f):
            return []
        out = []
        for v in read_json(f).get("values", []):
            v = v["id"] if isinstance(v, dict) else v
            out += self.biome_tag(v) if v.startswith("#") else [v]
        return out

    def biome_list(self, spec):
        if isinstance(spec, str):
            return self.biome_tag(spec) if spec.startswith("#") else [spec]
        out = []
        for s in spec or []:
            out += self.biome_list(s)
        return out

    @cached_property
    def structure_sets(self):
        out = {}
        for f in glob.glob(os.path.join(self.data, "worldgen", "structure_set", "*.json")):
            d = read_json(f)
            sid = os.path.splitext(os.path.basename(f))[0]
            for s in d["structures"]:
                key = s["structure"].split(":")[1]
                if d["placement"].get("type") == "guhs:gegarandeerd":
                    # (bbq2) the ONE guaranteed copy in new terrain: kept next to the building's own set, which says how rare the
                    # others are; a building with only this set has no others
                    out.setdefault(key, dict(set=sid, placement=d["placement"], weight=s.get("weight", 1), together=[key]))
                    out[key]["gegarandeerd"] = d["placement"]
                    continue
                out[key] = dict(set=sid, placement=d["placement"], weight=s.get("weight", 1),
                                together=[x["structure"].split(":")[1] for x in d["structures"]],
                                **({"gegarandeerd": out[key]["gegarandeerd"]} if "gegarandeerd" in out.get(key, {}) else {}))
        return out

    @cached_property
    def structures(self):
        out = {}
        for f in sorted(glob.glob(os.path.join(self.data, "worldgen", "structure", "*.json"))):
            sid = os.path.splitext(os.path.basename(f))[0]
            d = read_json(f)
            biomes = self.biome_list(d.get("biomes", []))
            out[sid] = dict(id=sid, type=d.get("type"), step=d.get("step"), biomes=biomes,
                            name=self.lang_nl.get(f"structure.{NS}.{sid}"),
                            tooltip=self.lang_nl.get(f"structure.{NS}.{sid}.tooltip"),
                            set=self.structure_sets.get(sid))
        return out

    @cached_property
    def dimension_biomes(self):
        """{dimension id: [biome ids]} (Guhmension: everything that isn't in another dimension)."""
        from .topics import DIMENSIONS
        out = {}
        for f in glob.glob(os.path.join(self.data, "dimension", "*.json")):
            did = os.path.splitext(os.path.basename(f))[0]
            gen = read_json(f)["generator"]
            src = gen.get("biome_source", {})
            if gen.get("type") == "minecraft:flat" and did in DIMENSIONS:
                # (the sea of the Snuffeleiland: one biome in the generator's settings; a flat dimension without a wiki page,
                # like the room inside a Guh House, keeps counting as the Guhmension)
                out[did] = [gen.get("settings", {}).get("biome")]
            elif src.get("type") == "minecraft:fixed":
                out[did] = [src["biome"]]
            else:
                out[did] = [b["biome"] for b in src.get("biomes", [])]
        return out

    @cached_property
    def biomes(self):
        dims = self.dimension_biomes
        out = {}
        for f in sorted(glob.glob(os.path.join(self.data, "worldgen", "biome", "*.json"))):
            bid = os.path.splitext(os.path.basename(f))[0]
            d = read_json(f)
            full = f"{NS}:{bid}"
            dim = next((k for k, v in dims.items() if full in v and k != "guhmension"), None)
            if dim is None:
                dim = "guhmaag" if bid == "guhmaag" else "guhmension"
            out[bid] = dict(id=bid, name=self.lang_nl.get(f"biome.{NS}.{bid}", pretty(bid)), dimension=dim,
                            temperature=d.get("temperature"), downfall=d.get("downfall"),
                            precipitation=d.get("has_precipitation"), effects=d.get("effects", {}),
                            spawners=d.get("spawners", {}), features=d.get("features", []))
        return out

    @cached_property
    def spawns(self):
        """{entity id: [dict(biomes, group, weight, min, max)]} from the biomes and the biome modifiers."""
        out = {}

        def add(entity, biomes, group, s):
            ns, eid = entity.split(":", 1)
            if ns != NS:
                return
            out.setdefault(eid, []).append(dict(biomes=biomes, group=group, weight=s.get("weight"),
                                                min=s.get("minCount"), max=s.get("maxCount")))
        for bid, b in self.biomes.items():
            for group, lst in b["spawners"].items():
                for s in lst:
                    add(s["type"], [f"{NS}:{bid}"], group, s)
        for f in sorted(glob.glob(os.path.join(self.data, "neoforge", "biome_modifier", "*.json"))):
            d = read_json(f)
            if d.get("type") != "neoforge:add_spawns":
                continue
            biomes = self.biome_list(d.get("biomes", []))
            spawners = d.get("spawners", [])
            if isinstance(spawners, dict):
                spawners = [spawners]
            for s in spawners:
                add(s["type"], biomes, "modifier", s)
        return out

    @cached_property
    def ore_modifiers(self):
        out = []
        for f in sorted(glob.glob(os.path.join(self.data, "neoforge", "biome_modifier", "*.json"))):
            d = read_json(f)
            if d.get("type") == "neoforge:add_features":
                out.append(dict(id=os.path.basename(f)[:-5], biomes=self.biome_list(d.get("biomes", [])), features=d.get("features")))
        return out

    # --- items and blocks -----------------------------------------------------------------------------------------------------
    @cached_property
    def item_models(self):
        return {os.path.splitext(f)[0] for f in os.listdir(os.path.join(self.assets, "models", "item"))}

    @cached_property
    def blockstates(self):
        return {os.path.splitext(f)[0] for f in os.listdir(os.path.join(self.assets, "blockstates"))}

    @cached_property
    def items(self):
        """Every item with a name: {id: dict(id, name, block, lore)} (blocks without an item form are left out)."""
        out = {}
        for k, v in self.lang_nl.items():
            m = re.fullmatch(rf"(item|block)\.{NS}\.([a-z0-9_]+)", k)
            if not m:
                continue
            kind, iid = m.groups()
            if kind == "block" and iid not in self.item_models:
                continue
            if iid in out:
                continue
            lore = [self.lang_nl[x] for x in (f"{kind}.{NS}.{iid}.lore", f"{kind}.{NS}.{iid}.tooltip", f"{kind}.{NS}.{iid}.desc")
                    if x in self.lang_nl]
            if iid in ("bank_guh", "bank_upgrade"):      # their text takes the bank's cap ("wel %s van elke soort"): BankStorage.CAP
                cap = re.search(r"int CAP = (\d+);", self.java_src(os.path.join("storage", "BankStorage.java")))
                lore = [x.replace("%s", cap.group(1)) if cap else x for x in lore]
            out[iid] = dict(id=iid, name=v, block=kind == "block" or iid in self.blockstates, lore=lore)
        return out

    # --- recipes --------------------------------------------------------------------------------------------------------------
    @cached_property
    def recipes(self):
        out = []
        for f in sorted(glob.glob(os.path.join(self.data, "recipe", "*.json"))):
            d = read_json(f)
            rid = os.path.splitext(os.path.basename(f))[0]
            t = d.get("type", "").split(":")[-1]
            res = d.get("result", {})
            result = res.get("id") or res.get("item") if isinstance(res, dict) else res
            count = res.get("count", 1) if isinstance(res, dict) else 1
            r = dict(id=rid, type=t, result=result, count=count, grid=None, inputs=[])
            if t == "crafting_shaped":
                pat = d["pattern"]
                grid = [None] * 9
                for y, line in enumerate(pat):
                    for x, ch in enumerate(line):
                        if ch != " ":
                            grid[y * 3 + x] = ingredient(d["key"][ch])
                r["grid"] = grid
            elif t == "crafting_shapeless":
                ings = [ingredient(i) for i in d["ingredients"]]
                r["grid"] = (ings + [None] * 9)[:9]
                r["shapeless"] = True
            elif t in ("smelting", "blasting", "smoking", "campfire_cooking", "stonecutting"):
                r["inputs"] = [ingredient(d["ingredient"])]
            elif t == "smithing_transform":
                r["inputs"] = [ingredient(d["template"]), ingredient(d["base"]), ingredient(d["addition"])]
            else:
                continue
            if r["grid"]:
                r["inputs"] = [g for g in r["grid"] if g]
            out.append(r)
        return out

    # --- loot tables -----------------------------------------------------------------------------------------------------------
    def loot(self, kind, name):
        f = os.path.join(self.data, "loot_table", kind, name + ".json")
        return summarise_loot(read_json(f)) if os.path.exists(f) else None

    @cached_property
    def chest_loot(self):
        return {os.path.basename(f)[:-5]: summarise_loot(read_json(f))
                for f in sorted(glob.glob(os.path.join(self.data, "loot_table", "chests", "*.json")))}

    @cached_property
    def entity_loot(self):
        return {os.path.basename(f)[:-5]: summarise_loot(read_json(f))
                for f in sorted(glob.glob(os.path.join(self.data, "loot_table", "entities", "*.json")))}

    # --- advancements ----------------------------------------------------------------------------------------------------------
    @cached_property
    def advancements(self):
        out = []
        base = os.path.join(self.data, "advancement")
        for f in sorted(glob.glob(os.path.join(base, "**", "*.json"), recursive=True)):
            rel = os.path.relpath(f, base).replace("\\", "/")[:-5]
            d = read_json(f)
            disp = d.get("display")
            if not disp:
                continue
            title = disp.get("title", {})
            desc = disp.get("description", {})
            tkey = title.get("translate") if isinstance(title, dict) else None
            dkey = desc.get("translate") if isinstance(desc, dict) else None
            items = sorted(set(re.findall(r'"items":\s*"guhs:([a-z0-9_]+)"', json.dumps(d.get("criteria", {})))))
            structs = sorted(set(re.findall(r'"structure":\s*"guhs:([a-z0-9_]+)"', json.dumps(d.get("criteria", {})))))
            out.append(dict(id=rel, tab=rel.split("/")[0], parent=d.get("parent"),
                            title=self.lang_nl.get(tkey, tkey or rel) if tkey else (title.get("text") if isinstance(title, dict) else str(title)),
                            desc=self.lang_nl.get(dkey, "") if dkey else (desc.get("text", "") if isinstance(desc, dict) else ""),
                            icon=(disp.get("icon") or {}).get("id") or (disp.get("icon") or {}).get("item"),
                            frame=disp.get("frame", "task"), hidden=d.get("display", {}).get("hidden", False),
                            items=items, structures=structs))
        return out

    # --- FTB quests ------------------------------------------------------------------------------------------------------------
    @cached_property
    def ftb(self):
        """dict(chapters={id: spec}, order=[ids], sections={chapter: [dict(sid, title, quests=[keys])]}, quests={key: tuple})."""
        here = os.getcwd()
        sys.path.insert(0, self.tools)
        try:
            os.chdir(self.root)
            ns = runpy.run_path(os.path.join(self.tools, "make_ftbquests.py"), run_name="wiki_site")
            sections = ns["assign"]()
        finally:
            os.chdir(here)
        quests = {q[0]: q for q in ns["QUESTS"]}
        return dict(chapters=ns["CHAPTERS"], order=ns["ORDER"], sections=sections, quests=quests,
                    groups=ns.get("GROEPEN", {}), group_of=ns.get("groep_van", lambda c: "guhs"),      # (the chapter groups of the sidebar)
                    plain=ns.get("plain", lambda s: re.sub(r"&[0-9a-fk-or]", "", s)))

    # --- misc lang lists -------------------------------------------------------------------------------------------------------
    def lang_group(self, prefix, suffix_filter=None):
        out = {}
        for k, v in self.lang_nl.items():
            if k.startswith(prefix):
                rest = k[len(prefix):]
                if "." not in rest:
                    out[rest] = v
        return out

    @cached_property
    def emotes(self):
        names = self.lang_group(f"emote.{NS}.")
        return [dict(id=e, name=n, desc=self.lang_nl.get(f"emote.{NS}.{e}.description", "")) for e, n in names.items()]

    @cached_property
    def effects(self):
        return self.lang_group(f"effect.{NS}.")

    @cached_property
    def entity_names(self):
        return self.lang_group(f"entity.{NS}.")

    @cached_property
    def villager_professions(self):
        return self.lang_group(f"entity.minecraft.villager.{NS}.")

    @cached_property
    def books(self):
        """The guh library books: {id: dict(title, pages)}."""
        out = {}
        for k, v in self.lang_nl.items():
            m = re.fullmatch(rf"book\.{NS}\.bieb\.(\w+)\.title", k)
            if m:
                bid = m.group(1)
                pages, i = [], 0
                while f"book.{NS}.bieb.{bid}.{i}" in self.lang_nl:
                    pages.append(self.lang_nl[f"book.{NS}.bieb.{bid}.{i}"])
                    i += 1
                out[bid] = dict(title=v, pages=pages)
        return out

    @cached_property
    def personalities(self):
        pre = f"gui.{NS}.personality."
        return [dict(id=k[len(pre):], name=v, desc=self.lang_nl.get(k + ".description", ""))
                for k, v in self.lang_nl.items() if k.startswith(pre) and k.count(".") == 3]

    @cached_property
    def mod_version(self):
        m = re.search(r"^mod_version\s*=\s*(\S+)", read(os.path.join(self.root, "gradle.properties")), re.M)
        return m.group(1) if m else "?"


def ingredient(spec):
    """A recipe ingredient -> 'ns:item' or '#ns:tag' (the first of a list)."""
    if isinstance(spec, list):
        return ingredient(spec[0]) if spec else None
    if isinstance(spec, str):
        return spec
    if "item" in spec:
        return spec["item"]
    if "tag" in spec:
        return "#" + spec["tag"]
    if "items" in spec:
        return ingredient(spec["items"])
    return None


def _num(v):
    if isinstance(v, (int, float)):
        return v, v
    if isinstance(v, dict):
        t = v.get("type", "minecraft:uniform")
        if "min" in v and "max" in v:
            return v["min"], v["max"]
        if t.endswith("constant"):
            return v.get("value", 1), v.get("value", 1)
        if t.endswith("binomial"):
            return 0, v.get("n", 1)
    return 1, 1


def summarise_loot(table):
    """-> [dict(item, min, max, chance or None, weight share, pool)] for every item entry (nested entries flattened)."""
    out = []
    for pi, pool in enumerate(table.get("pools", [])):
        entries = []

        def walk(es):
            for e in es:
                if e.get("type", "").endswith("item"):
                    entries.append(e)
                elif "children" in e:
                    walk(e["children"])
        walk(pool.get("entries", []))
        total = sum(e.get("weight", 1) for e in entries) or 1
        rolls = _num(pool.get("rolls", 1))
        pool_chance = None
        for c in pool.get("conditions", []):
            if c.get("condition", "").endswith("random_chance") or c.get("condition", "").endswith("random_chance_with_enchanted_bonus"):
                pool_chance = c.get("chance") if isinstance(c.get("chance"), (int, float)) else (c.get("unenchanted_chance") or None)
        for e in entries:
            lo, hi = 1, 1
            chance = pool_chance
            for fn in e.get("functions", []):
                if fn.get("function", "").endswith("set_count"):
                    lo, hi = _num(fn.get("count", 1))
            for c in e.get("conditions", []):
                if c.get("condition", "").endswith("random_chance"):
                    chance = c.get("chance")
            out.append(dict(item=e["name"], min=lo, max=hi, chance=chance, share=e.get("weight", 1) / total, pool=pi,
                            rolls=rolls, player_kill=any(c.get("condition", "").endswith("killed_by_player")
                                                          for c in e.get("conditions", []) + pool.get("conditions", []))))
    return out


# names of the vanilla items that show up in our recipes and loot (the rest get a tidied-up id)
VANILLA_NAMES = {
    "iron_ingot": "IJzeren staaf", "gold_ingot": "Gouden staaf", "diamond": "Diamant", "emerald": "Smaragd", "stick": "Stok",
    "redstone": "Redstone", "pink_dye": "Roze kleurstof", "pink_wool": "Roze wol", "white_wool": "Witte wol", "string": "Draad",
    "leather": "Leer", "chest": "Kist", "glass": "Glas", "glass_pane": "Glazen paneel", "paper": "Papier", "book": "Boek",
    "sugar": "Suiker", "wheat": "Tarwe", "egg": "Ei", "milk_bucket": "Emmer melk", "bucket": "Emmer", "water_bucket": "Emmer water",
    "bread": "Brood", "cookie": "Koekje", "cake": "Taart", "honey_bottle": "Honingfles", "glass_bottle": "Glazen fles",
    "netherite_ingot": "Netherietstaaf", "netherite_upgrade_smithing_template": "Netherietupgrade", "charcoal": "Houtskool",
    "coal": "Steenkool", "flint": "Vuursteen", "iron_nugget": "IJzerklompje", "gold_nugget": "Goudklompje", "copper_ingot": "Koperen staaf",
    "amethyst_shard": "Amethistscherf", "feather": "Veer", "bone": "Bot", "bone_meal": "Beendermeel", "slime_ball": "Slijmbal",
    "ender_pearl": "Enderparel", "blaze_powder": "Blazepoeder", "quartz": "Netherkwarts", "clay_ball": "Klei", "brick": "Baksteen",
    "stone": "Steen", "cobblestone": "Keien", "dirt": "Aarde", "sand": "Zand", "oak_planks": "Eikenhouten planken",
    "saddle": "Zadel", "lead": "Lijn", "apple": "Appel", "sweet_berries": "Zoete bessen", "carrot": "Wortel", "potato": "Aardappel",
    "pumpkin": "Pompoen", "melon_slice": "Meloenschijf", "cocoa_beans": "Cacaobonen", "pink_concrete": "Roze beton",
    "white_dye": "Witte kleurstof", "yellow_dye": "Gele kleurstof", "red_dye": "Rode kleurstof", "blue_dye": "Blauwe kleurstof",
    "black_dye": "Zwarte kleurstof", "lime_dye": "Lichtgroene kleurstof", "light_blue_dye": "Lichtblauwe kleurstof",
    "magenta_dye": "Magenta kleurstof", "purple_dye": "Paarse kleurstof", "orange_dye": "Oranje kleurstof", "green_dye": "Groene kleurstof",
    "brown_dye": "Bruine kleurstof", "gray_dye": "Grijze kleurstof", "light_gray_dye": "Lichtgrijze kleurstof", "cyan_dye": "Cyaan kleurstof",
    "torch": "Fakkel", "lantern": "Lantaarn", "campfire": "Kampvuur", "furnace": "Oven", "crafting_table": "Werkbank",
    "pink_petals": "Roze bloemblaadjes", "pink_tulip": "Roze tulp", "peony": "Pioenroos", "shears": "Schaar", "compass": "Kompas",
    "clock": "Klok", "map": "Kaart", "feather_falling": "Veervallen", "ink_sac": "Inktzak", "cod": "Kabeljauw", "salmon": "Zalm",
    "tropical_fish": "Tropische vis", "honeycomb": "Honingraat", "hay_block": "Hooibaal", "snowball": "Sneeuwbal",
    "ice": "IJs", "packed_ice": "Pakijs", "blue_ice": "Blauw ijs", "scute": "Schildpadschub", "turtle_scute": "Schildpadschub",
    "nautilus_shell": "Nautilusschelp", "prismarine_shard": "Prismarienscherf", "prismarine_crystals": "Prismarienkristallen",
    "kelp": "Kelp", "dried_kelp": "Gedroogde kelp", "seagrass": "Zeegras", "bamboo": "Bamboe", "sugar_cane": "Suikerriet",
    "gunpowder": "Buskruit", "fire_charge": "Vuurbal", "magma_cream": "Magmacrème", "glowstone_dust": "Gloeisteenstof",
    "glow_ink_sac": "Gloeiinktzak", "echo_shard": "Echoscherf", "wind_charge": "Windlading", "rabbit_hide": "Konijnenhuid",
    "iron_block": "IJzerblok", "gold_block": "Goudblok", "diamond_block": "Diamantblok", "emerald_block": "Smaragdblok",
    "redstone_block": "Redstoneblok", "lapis_lazuli": "Lapis lazuli", "ender_eye": "Enderoog", "obsidian": "Obsidiaan",
    "crying_obsidian": "Huilende obsidiaan", "blackstone": "Zwartsteen", "basalt": "Basalt", "soul_sand": "Zielenzand",
    "nether_brick": "Netherbaksteen", "golden_apple": "Gouden appel", "golden_carrot": "Gouden wortel", "cookie_": "Koekje",
    "bowl": "Kom", "mushroom_stew": "Paddenstoelensoep", "beetroot": "Biet", "wheat_seeds": "Tarwezaadjes", "flower_pot": "Bloempot",
    "oak_log": "Eikenstam", "oak_slab": "Eikenhouten plaat", "white_carpet": "Wit tapijt", "pink_carpet": "Roze tapijt",
    "note_block": "Nootblok", "jukebox": "Jukebox", "bell": "Bel", "chain": "Ketting", "iron_bars": "IJzeren tralies",
    "tripwire_hook": "Struikeldraadhaak", "hopper": "Trechter", "piston": "Zuiger", "rail": "Spoor", "powered_rail": "Aandrijfspoor",
    "minecart": "Mijnkarretje", "barrel": "Ton", "smoker": "Roker", "blast_furnace": "Hoogoven", "cauldron": "Ketel",
    "stonecutter": "Steenzaag", "loom": "Weefgetouw", "anvil": "Aambeeld", "spyglass": "Verrekijker", "fishing_rod": "Hengel",
    "bow": "Boog", "arrow": "Pijl", "shield": "Schild", "totem_of_undying": "Totem der onsterfelijkheid", "nether_star": "Netherster",
    "heart_of_the_sea": "Hart van de zee", "experience_bottle": "Betoveringsfles", "enchanted_book": "Betoverd boek",
    "name_tag": "Naamkaartje", "painting": "Schilderij", "item_frame": "Lijst", "armor_stand": "Harnasstandaard",
    "white_bed": "Wit bed", "pink_bed": "Roze bed", "honey_block": "Honingblok", "slime_block": "Slijmblok", "snow_block": "Sneeuwblok",
    "moss_block": "Mosblok", "glow_berries": "Gloeibessen", "cherry_leaves": "Kersenbladeren", "cherry_planks": "Kersenhouten planken",
    "spruce_planks": "Sparrenhouten planken", "birch_planks": "Berkenhouten planken", "quartz_block": "Kwartsblok",
    "terracotta": "Terracotta", "pink_terracotta": "Roze terracotta", "white_concrete": "Wit beton", "smooth_stone": "Gladde steen",
}
