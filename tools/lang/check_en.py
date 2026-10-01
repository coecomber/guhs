"""
Checks the English overlay (tools/lang/en/*.json) against the Dutch source, per translation chunk (tools/lang/chunks.json).

    python tools/lang/check_en.py                 summary per chunk + every problem (exit 1 on errors)
    python tools/lang/check_en.py --chunk c03_x   only that chunk
    python tools/lang/check_en.py --dump c03_x    the chunk's source: key<TAB>Dutch[<TAB>context] (newlines as \\n)
    python tools/lang/check_en.py --todo c03_x    the same, only the keys without English yet
    python tools/lang/check_en.py --complete      also fail when keys have no English yet
    python tools/lang/check_en.py --warn          also print the warnings (same as Dutch, glossary names, long sign lines)

The Dutch source is assets/guhs/lang/nl_nl.json (made by the generators) plus tools/lang/source_ftb.json (the FTB Quests
book, made by tools/make_ftbquests.py): run the generators first when the Dutch changed (see tools/lang/__init__.py).

Errors (exit code 1): an overlay key in two files (or not a string), stale keys (no longer in the source), a key in the
file of another chunk, placeholders that don't match (%s %d %1$s, the number of arguments and their types), §x colour
codes, FTB &x codes, the number of newlines, {0}-style fields, leading/trailing spaces, and probable Dutch leftovers.
"""
import collections
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import lang as L  # noqa: E402  (tools/lang/__init__.py)

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

# --- format checks ---------------------------------------------------------------------------------------------------------
_PCT = re.compile(r"%(?:(\d+)\$)?([sd%])")


def placeholders(text):
    """Counter of (argument index, type): '%s %s' and '%1$s %2$s' are the same, so English may reorder with %2$s."""
    out, n = collections.Counter(), 0
    for m in _PCT.finditer(text):
        if m.group(2) == "%":
            continue
        if m.group(1):
            i = int(m.group(1))
        else:
            n += 1
            i = n
        out[(i, m.group(2))] += 1
    return out


def codes(pattern, text):
    return collections.Counter(re.findall(pattern, text))


def format_problems(nl, en, ftb):
    probs = []
    if placeholders(nl) != placeholders(en):
        probs.append(f"placeholders {sorted(placeholders(nl).elements())} vs {sorted(placeholders(en).elements())}")
    if codes(r"§[0-9a-fk-or]", nl) != codes(r"§[0-9a-fk-or]", en):
        probs.append("§ colour codes differ")
    if ftb and codes(r"(?<!\\)&[0-9a-fk-or]", nl) != codes(r"(?<!\\)&[0-9a-fk-or]", en):
        probs.append("&x colour codes differ")
    if nl.count("\n") != en.count("\n"):
        probs.append(f"{nl.count(chr(10))} newlines in Dutch, {en.count(chr(10))} in English")
    if codes(r"\{\d+\}", nl) != codes(r"\{\d+\}", en):
        probs.append("{n} fields differ")
    if (nl[:1].isspace(), nl[-1:].isspace()) != (en[:1].isspace(), en[-1:].isspace()):
        probs.append("leading/trailing space differs")
    if not en.strip() and nl.strip():
        probs.append("empty English")
    return probs


# --- Dutch leftovers -------------------------------------------------------------------------------------------------------
DUTCH_WORDS = set("""
de het een van niet je jij jou jouw mijn jullie hij zij wij ons onze niks niets geen nog ook maar naar voor zonder
bij uit wat wie waar hier daar dit deze dat nu nooit altijd erg lekker leuk mooi lief lieve kun kunt moet
moeten heb hebt heeft hebben gaat gaan ging komt komen kom zijn wordt worden bent waren dan als omdat
alle alles iets iemand niemand veel weer samen goed klaar zoek vind vindt maak maakt gemaakt keer eerst daarna
guhtje guhtjes vadsig vadsige vadsiger vadsigst vadsigheid njeg vahoeg vahoege knabbel knabbels kaasknabbel kaasknabbels
kaas knuffel knuffels knuffelen knus gezellig pluisje schattig zieli doei hatsjoe oei
""".split())
DUTCH_STEMS = ("knabbel", "kaasknabbel", "vadsig", "vahoe", "njeg", "guhtje", "knuffel", "gezellig", "pluisje", "heerlijk")
_WORD = re.compile(r"[A-Za-zÀ-ÿ']+")
_STRIP = re.compile(r"§.|(?<!\\)&[0-9a-fk-or]|%(\d+\$)?[sd]")


def dutch_leftovers(en, allow):
    words = [w.lower().strip("'") for w in _WORD.findall(_STRIP.sub(" ", en))]
    hits = sorted({w for w in words if w not in allow and (w in DUTCH_WORDS or any(w.startswith(s) for s in DUTCH_STEMS))})
    return hits


# --- glossary names (tools/lang/GLOSSARY.md tables: | Dutch | English | note |) ---------------------------------------------------
def glossary_names():
    """{Dutch name: English} for the capitalised one-to-one rows (place and character names), for a consistency warning."""
    path = os.path.join(L.HERE, "GLOSSARY.md")
    out = {}
    if not os.path.exists(path):
        return out
    for line in open(path, encoding="utf-8"):
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if not line.startswith("|") or len(cells) < 2 or cells[0] in ("Dutch", "---") or set(cells[0]) <= set("-: "):
            continue
        nl = re.sub(r"\*|\(.*?\)", "", cells[0]).strip()
        en = re.sub(r"\*|\(.*?\)", "", cells[1]).strip()
        if "/" in nl or "/" in en or not nl or not en or nl == en or not nl[0].isupper() or len(nl) < 5 or " " in nl:
            continue
        out[nl] = en
    return out


def main(argv):
    def opt(name):
        return argv[argv.index(name) + 1] if name in argv and argv.index(name) + 1 < len(argv) else None

    chunker = L.Chunker()
    try:
        overlay, where = L.load_overlay(with_files=True)
    except ValueError as e:
        print("ERROR", e)
        return 1
    source = L.nl_source()
    context = json.load(open(L.CONTEXT_FILE, encoding="utf-8")) if os.path.exists(L.CONTEXT_FILE) else {}
    by_chunk = collections.defaultdict(list)
    for k in source:
        by_chunk[chunker(k)].append(k)

    for flag in ("--dump", "--todo"):
        cid = opt(flag)
        if cid:
            if cid not in chunker.ids:
                print(f"unknown chunk {cid}; chunks: {', '.join(chunker.ids)}")
                return 2
            for k in by_chunk[cid]:
                if flag == "--todo" and k in overlay:
                    continue
                row = [k, source[k].replace("\n", "\\n")]
                if k in context and context[k] != source[k]:
                    row.append("[" + context[k].replace("\n", "\\n") + "]")
                print("\t".join(row))
            return 0

    only = opt("--chunk")
    if only and only not in chunker.ids:
        print(f"unknown chunk {only}; chunks: {', '.join(chunker.ids)}")
        return 2
    show_warn = "--warn" in argv
    with open(os.path.join(L.root(), "src", "main", "resources", "assets", "guhs", "lang", "nl_nl.json"), encoding="utf-8") as f:
        all_nl = json.load(f)
    allow = {w.lower() for w in json.load(open(os.path.join(L.HERE, "allow_words.json"), encoding="utf-8"))} \
        if os.path.exists(os.path.join(L.HERE, "allow_words.json")) else set()
    names = glossary_names() if show_warn else {}

    errors, warnings = collections.defaultdict(list), collections.defaultdict(list)
    for k, en in overlay.items():
        cid = chunker(k)
        if only and cid != only:
            continue
        nl = source.get(k)
        if nl is None:
            if k in all_nl or (k.startswith("item.guhs.") and k in all_nl):
                nl = all_nl[k]        # (a text without letters or a derived item name: allowed, just not needed)
            else:
                errors[cid].append(f"{k}: stale (not in the Dutch source any more) [{where[k]}]")
                continue
        if where[k] != cid + ".json":
            errors[cid].append(f"{k}: belongs in {cid}.json, not {where[k]}")
        for p in format_problems(nl, en, k.startswith("ftb.")):
            errors[cid].append(f"{k}: {p}")
        hits = dutch_leftovers(en, allow)
        if hits:
            errors[cid].append(f"{k}: Dutch? {', '.join(hits)}  | {en[:80]}")
        if show_warn:
            if en == nl and L.needs_english(nl):
                warnings[cid].append(f"{k}: same as the Dutch: {en[:60]}")
            for dn, ename in names.items():
                if re.search(r"\b" + re.escape(dn) + r"\b", nl) and ename.lower() not in en.lower():
                    warnings[cid].append(f"{k}: glossary: {dn} -> {ename}?  | {en[:60]}")
            if k.startswith("sign.") and len(en) > max(15, len(nl)):
                warnings[cid].append(f"{k}: long sign line ({len(en)} characters): {en}")

    total = dict(keys=0, chars=0, done=0)
    print(f"{'chunk':<24}{'keys':>7}{'chars':>9}{'English':>9}{'todo':>7}{'errors':>8}  title")
    missing_total = 0
    for cid in chunker.ids:
        if only and cid != only:
            continue
        keys = by_chunk.get(cid, [])
        chars = sum(len(source[k]) for k in keys)
        done = sum(1 for k in keys if k in overlay)
        missing_total += len(keys) - done
        total["keys"] += len(keys)
        total["chars"] += chars
        total["done"] += done
        print(f"{cid:<24}{len(keys):>7}{chars:>9}{done:>9}{len(keys) - done:>7}{len(errors.get(cid, [])):>8}  {chunker.titles[cid]}")
    print(f"{'total':<24}{total['keys']:>7}{total['chars']:>9}{total['done']:>9}{total['keys'] - total['done']:>7}"
          f"{sum(len(v) for v in errors.values()):>8}")
    for cid in sorted(errors):
        print(f"\n== {cid}: {len(errors[cid])} errors")
        for e in errors[cid]:
            print("  ", e)
    for cid in sorted(warnings):
        print(f"\n-- {cid}: {len(warnings[cid])} warnings")
        for e in warnings[cid]:
            print("  ", e)
    failed = any(errors.values()) or ("--complete" in argv and missing_total)
    print("\nFAIL" if failed else "\nOK", f"({sum(len(v) for v in errors.values())} errors, {missing_total} keys without English)")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
