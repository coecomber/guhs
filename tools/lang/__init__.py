"""
Guhs 1.2.0: the English translation.

The Dutch text stays the source: the generators (make_resources.py, make_v2.py, features/*.py, make_ftbquests.py,
sign_text.py) write assets/guhs/lang/nl_nl.json and the Dutch FTB Quests texts exactly as before. The English lives in an
overlay: tools/lang/en/<chunk>.json, flat {key: English}. At the end of the generation:

    en_us = nl_nl, with every key the overlay has replaced by its English

so a key nobody translated yet simply stays Dutch (the build never breaks halfway through translating). The same goes for
the FTB Quests book (keys "ftb.<chapter>.q.<quest>.title" etc., see make_ftbquests.py; their Dutch source is written to
tools/lang/source_ftb.json).

    tools/lang/chunks.json      which keys belong to which translation chunk (longest prefix wins)
    tools/lang/en/<chunk>.json  the English of that chunk (all files are merged; a key may be in only one)
    tools/lang/check_en.py      missing / stale keys, placeholder mismatches, Dutch leftovers; --dump <chunk> lists the source
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
EN_DIR = os.path.join(HERE, "en")
CHUNKS_FILE = os.path.join(HERE, "chunks.json")
FTB_SOURCE = os.path.join(HERE, "source_ftb.json")
CONTEXT_FILE = os.path.join(HERE, "source_context.json")


def load_overlay(with_files=False):
    """All tools/lang/en/*.json merged into one {key: English}. A key in two files is an error."""
    out, where = {}, {}
    if os.path.isdir(EN_DIR):
        for name in sorted(os.listdir(EN_DIR)):
            if not name.endswith(".json"):
                continue
            with open(os.path.join(EN_DIR, name), encoding="utf-8") as f:
                data = json.load(f)
            for k, v in data.items():
                if k in out:
                    raise ValueError(f"tools/lang/en: {k} is in both {where[k]} and {name}")
                if not isinstance(v, str):
                    raise ValueError(f"tools/lang/en/{name}: {k} is not a string")
                out[k], where[k] = v, name
    return (out, where) if with_files else out


_LETTERS = re.compile(r"[A-Za-zÀ-ÿ]")
_CODES = re.compile(r"§.|&[0-9a-fk-or]|%(\d+\$)?[sd%]")


def needs_english(value):
    """Does this Dutch text need a translation at all? (Not when it has no letters: "%s / %s", "<3", "+%s"...)"""
    return bool(_LETTERS.search(_CODES.sub("", value)))


# the tooltip suffixes mc26.lang_block_items() copies from block.guhs.<name>.<suffix> to item.guhs.<name>.<suffix> (mc26.TOOLTIP_SUFFIXES)
TOOLTIP_SUFFIXES = ("lore", "loan", "tooltip")


def derived_block(key):
    """The block.guhs.* key an item.guhs.* key may be a copy of (its name, or a tooltip line), else None."""
    if not key.startswith("item.guhs."):
        return None
    rest = key[len("item.guhs."):]
    parts = rest.split(".")
    if len(parts) == 1 or (len(parts) == 2 and parts[1] in TOOLTIP_SUFFIXES):
        return "block.guhs." + rest
    return None


def derived_item(key, nl):
    """item.guhs.X (or its tooltip line item.guhs.X.lore/.loan/.tooltip) with exactly the text of block.guhs.X(.lore...):
    mc26.lang_block_items() copies it (in every language), so it takes the block's English and needs no overlay entry of
    its own."""
    block = derived_block(key)
    return block is not None and block in nl and nl[block] == nl[key]


def english(nl, overlay=None):
    """en_us from nl_nl: the overlay's English where it has the key, else the Dutch. Returns (en, untranslated count)."""
    overlay = load_overlay() if overlay is None else overlay
    en, missing = {}, 0
    for k, v in nl.items():
        if k in overlay:
            en[k] = overlay[k]
        elif derived_item(k, nl) and derived_block(k) in overlay:
            en[k] = overlay[derived_block(k)]
        else:
            en[k] = v
            if needs_english(v) and not derived_item(k, nl):
                missing += 1
    return en, missing


# --- chunks ------------------------------------------------------------------------------------------------------------------
def load_chunks():
    with open(CHUNKS_FILE, encoding="utf-8") as f:
        return json.load(f)


class Chunker:
    """key -> chunk id: the chunk with the longest matching prefix (chunks.json), else the default chunk."""

    def __init__(self, spec=None):
        spec = spec or load_chunks()
        self.default = spec["default"]
        self.ids = [c["id"] for c in spec["chunks"]]
        self.titles = {c["id"]: c.get("title", "") for c in spec["chunks"]}
        rules = []
        for c in spec["chunks"]:
            for p in c["prefixes"]:
                rules.append((p, c["id"]))
        seen = {}
        for p, cid in rules:
            if p in seen and seen[p] != cid:
                raise ValueError(f"chunks.json: prefix {p!r} is in {seen[p]} and {cid}")
            seen[p] = cid
        self.rules = sorted(seen.items(), key=lambda r: -len(r[0]))

    def __call__(self, key):
        for p, cid in self.rules:
            if key.startswith(p):
                return cid
        return self.default


# --- the Dutch source of everything that needs English ----------------------------------------------------------------------
def root():
    return os.path.dirname(os.path.dirname(HERE))


def nl_source():
    """{key: Dutch} of every key that needs an English text: nl_nl (minus derived item names and texts without letters) and
    the FTB Quests book (tools/lang/source_ftb.json)."""
    with open(os.path.join(root(), "src", "main", "resources", "assets", "guhs", "lang", "nl_nl.json"), encoding="utf-8") as f:
        nl = json.load(f)
    out = {k: v for k, v in nl.items() if needs_english(v) and not derived_item(k, nl)}
    if os.path.exists(FTB_SOURCE):
        with open(FTB_SOURCE, encoding="utf-8") as f:
            out.update({k: v for k, v in json.load(f).items() if needs_english(v)})
    return out


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
