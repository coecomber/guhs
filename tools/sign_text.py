"""
Guhs 1.2.0: text in structure templates (signs, book pages, custom names) as translate keys, so every player reads it in
their own language (each client resolves the key; the template itself stays language-independent).

    line(prefix, "Vers uit de")            -> '{"translate":"<prefix>.vers_uit_de","fallback":"Vers uit de"}'
    line(prefix, "Hole %s", key=..., args=[3], color="dark_purple", bold=True)
    ref("gui.guhs.elftocht.dorp.6", "Guhdeloopen")   (a key that already exists in the lang source: not registered again)
    messages(prefix, ["~ Viskraam ~", "Vers uit de", ...])  -> the four messages of a sign side (an NbtList)

Every registered key lands in TEXTS {key: Dutch}; make_v2.write_lang() adds them to nl_nl (and so to en_us, where the
English overlay in tools/lang/en/*.json translates them like every other key). The Dutch text is also the "fallback", so a
client without the key (an old resource pack, a map renderer) still shows the Dutch. Keys are made from the Dutch text
(a slug), so they don't shift when signs are added or moved; a changed text gets a new key (check_en.py lists the old one as
stale). CONTEXT {key: "the whole sign"} helps the translator (python tools/lang/check_en.py --dump <chunk>).

A Minecraft book title and author are plain strings (no components): they can't be translated and stay Dutch.
"""
import json
import re
import unicodedata

TEXTS = {}      # key -> Dutch (registered by line())
CONTEXT = {}    # key -> the whole sign / page the line belongs to (first time seen)
REFS = {}       # keys used through ref(): must exist in the lang source (checked by make_v2.write_lang)


def slug(text, limit=32):
    t = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode().lower()
    t = re.sub(r"[^a-z0-9]+", "_", t).strip("_")
    return t[:limit].rstrip("_")


def register(key, nl, context=None):
    old = TEXTS.get(key)
    if old is not None and old != nl:
        raise ValueError(f"sign_text: {key} is already {old!r}, not {nl!r}")
    TEXTS[key] = nl
    if context and key not in CONTEXT:
        CONTEXT[key] = context
    return key


def key_for(prefix, text):
    """prefix + slug of the text; a different text with the same slug gets a short hash after it."""
    import hashlib
    s = slug(text) or "t"
    key = f"{prefix}.{s}"
    if TEXTS.get(key, text) != text:
        key = f"{key}_{hashlib.md5(text.encode()).hexdigest()[:4]}"
    return key


def component(prefix, text, key=None, args=None, context=None, **style):
    """The component (a dict) for one line. text "" -> "" (an empty line); a dict passes through unchanged."""
    if isinstance(text, dict):
        return text
    if not text:
        return ""
    key = register(key or key_for(prefix, text), text, context)
    out = {"translate": key, "fallback": text}
    if args is not None:
        out["with"] = [str(a) for a in args]
    out.update(style)
    return out


def line(prefix, text, key=None, args=None, context=None, **style):
    return json.dumps(component(prefix, text, key, args, context, **style))


def ref(key, fallback, args=None, **style):
    """A key that the lang source already has (e.g. a village name): only referenced, not registered."""
    REFS[key] = fallback
    out = {"translate": key, "fallback": fallback}
    if args is not None:
        out["with"] = [str(a) for a in args]
    out.update(style)
    return out


def messages(prefix, lines, n=4):
    """The four messages of a sign side; each line is a Dutch str, a ready component (dict) or "" (empty)."""
    from make_structures import NbtList
    lines = (list(lines) + [""] * n)[:n]
    context = " / ".join(l if isinstance(l, str) else (l.get("fallback") or l.get("text") or "") for l in lines if l)
    return NbtList(8, [json.dumps(component(prefix, l, context=context)) if l else json.dumps("") for l in lines])


# --- imported user builds (tools/import_world_builds.py) ------------------------------------------------------------------
# (structure, literal sign line) -> its key; import_world_builds.block_entity() writes these as translate + fallback, and
# patch_imported() fixes the templates already in the repo (the source world isn't in every checkout).
IMPORTED = {
    ("giant_kaasknabbel", "Ik had zn honger"): "sign.guhs.giant_kaasknabbel.honger",
}


def imported_message(build, text):
    """The JSON string for a sign line of an imported build: translatable when it is in IMPORTED, else literal."""
    key = IMPORTED.get((build, text))
    if key:
        register(key, text, context=text)
        return json.dumps({"translate": key, "fallback": text})
    return json.dumps(text)


def _nbt_string(s):
    b = s.encode("utf-8")   # (our strings have no NUL or astral characters: plain UTF-8 is modified UTF-8)
    return len(b).to_bytes(2, "big") + b


def patch_imported(structure_dir):
    """Turns the literal lines of IMPORTED in the imported templates into translate + fallback, in place (only the strings
    change; an NBT list counts its elements, not bytes, so a longer string is fine). Registers the keys either way."""
    import gzip
    import os
    by_build = {}
    for (build, text), key in IMPORTED.items():
        register(key, text, context=text)
        by_build.setdefault(build, []).append((text, key))
    changed = []
    for build, pairs in by_build.items():
        path = os.path.join(structure_dir, build + ".nbt")
        with open(path, "rb") as f:
            raw = gzip.decompress(f.read())
        new = raw
        for text, key in pairs:
            old = _nbt_string(json.dumps(text))
            rep = _nbt_string(json.dumps({"translate": key, "fallback": text}))
            new = new.replace(old, rep)
        if new != raw:
            with open(path, "wb") as f:
                f.write(gzip.compress(new, mtime=0))
            changed.append(build)
    return changed
