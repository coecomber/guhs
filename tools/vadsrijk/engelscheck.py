"""W-ENGELS: the English check for Het Vadsrijk (1.10) - which keys still need (new) English.

The English of Guhs lives in ONE place: the overlay tools/lang/en/*.json (see tools/lang/__init__.py). en_us.json is
built from nl_nl.json + that overlay by `python tools/lang/apply_en.py` (or the full make_resources.py). A Vadsrijk
generator run (python tools/features/vadsrijk_*.py) writes Dutch (or quick English) straight into en_us.json, so ALWAYS
run apply_en.py after a generator run; the overlay then wins again.

    python tools/vadsrijk/engelscheck.py                 report (exit 1 when anything needs work)
    python tools/vadsrijk/engelscheck.py --lijst         also print every key (default: the first 25 per category)
    python tools/vadsrijk/engelscheck.py --todo FILE     write {key: {"nl": Dutch, "draft"?: old English}} of every key that
                                                         needs (new) English to FILE: hand that to a translator, put the
                                                         result in tools/lang/en/c400_vr_aanvulling.json (or any c4xx_vr_ file)
    python tools/vadsrijk/engelscheck.py --merk          after translating: remember the current Dutch of every Vadsrijk key
                                                         that has English (tools/vadsrijk/engels_bron.json), so a later change of
                                                         the Dutch shows up as "Dutch changed"
    python tools/vadsrijk/engelscheck.py --alles         check every key of the mod, not only the Vadsrijk/1.10 ones

Categories:
    GEEN ENGELS      the key is in nl_nl but not in the overlay (en_us stays Dutch)
    NL GEWIJZIGD     the Dutch changed after the English was written (engels_bron.json) - check / retranslate
    GELIJK AAN NL    the English equals the Dutch (allowed for the names/values in engelscheck_allow.json)
    NEDERLANDS?      the English contains Dutch marker words (tools/lang/check_en.py's list + a few Vadsrijk words)
    FORMAAT          %s / §x / &x / \\n / {0} differ between Dutch and English
    VEROUDERD        an overlay key that no generator makes any more (remove it)
    EN_US OUD        en_us.json does not match nl_nl + overlay: run `python tools/lang/apply_en.py`

Scope "Vadsrijk/1.10": keys that start with vadsrijk. / ftb.guhs_vadsrijk, or contain vadsrijk or .vr_, plus every key
that was not in nl_nl.json of the v1.9.7 release (git tag; skipped when git or the tag is missing).
"""
import hashlib
import json
import os
import re
import subprocess
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools"))
sys.path.insert(0, os.path.join(ROOT, "tools", "lang"))
import lang as L  # noqa: E402
import check_en as C  # noqa: E402

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "guhs", "lang")
BRON = os.path.join(HIER, "engels_bron.json")
ALLOW = os.path.join(HIER, "engelscheck_allow.json")
BASIS_TAG = "v1.9.7"
SCOPE = re.compile(r"^(vadsrijk\.|ftb\.guhs_vadsrijk)|vadsrijk|\.vr_")
# Dutch words that betray an untranslated Vadsrijk text (on top of check_en.DUTCH_WORDS)
VADS_WOORDEN = {"vadsrijk", "zwamzoet", "kandijkom", "honingtak", "hoogvads", "knuffelorde", "knuffelkamp", "ridder", "ridders",
                "vadsmunten", "klodder", "zwam", "wolkwal", "schemerwortel", "vahoegnir", "uitgevadsten", "vadsloosheid",
                "pluisjager", "vadsmagiër", "knuffelridder", "opperzwam", "saffraanpoort", "lampionhaven", "rozenkloof",
                "splinterkloof", "galmhellingen", "sporenwoud", "druipgrotten", "nog", "wordt", "zijn", "jouw", "naar",
                "bij", "ook", "maar", "met", "voor", "hier", "daar", "deze", "dit", "dat", "wat", "waar", "moet", "kun",
                "kunt", "heb", "hebt", "heeft", "gaat", "komt", "eerst", "daarna", "klaar", "zoek", "vind", "praat", "breng",
                "haal", "versla", "steek", "rechtsklik", "verdediging", "kracht", "behendigheid", "wijsheid", "schade"}
# English words that look like a Dutch marker but are fine in English
ENGELS_OK = {"die", "had", "hand", "is", "in", "over", "mist", "rug", "arm", "bed", "dam", "fort", "gem", "hop", "kin", "lap",
             "mop", "nap", "pet", "pin", "rat", "rib", "slim", "spin", "step", "tip", "top", "van", "wand", "wig", "want",
             "brand", "hoop", "lever", "stem", "rover", "boot", "bond", "dal", "met", "kind", "door", "worst", "bring",
             "guh", "guhs", "mika", "mikas", "guhdex", "guhmension", "nyeg", "wahoog"}


COMMANDO = re.compile(r"/(?:guhs|bond)\b[^.,;:!?)\n(]*")


def lees(pad):
    with open(pad, encoding="utf-8") as f:
        return json.load(f)


def basis_sleutels():
    try:
        uit = subprocess.check_output(["git", "-C", ROOT, "show", f"{BASIS_TAG}:src/main/resources/assets/guhs/lang/nl_nl.json"],
                                      stderr=subprocess.DEVNULL)
        return set(json.loads(uit.decode("utf-8")))
    except Exception:
        return None


def h(tekst):
    return hashlib.sha1(tekst.encode("utf-8")).hexdigest()[:10]


def nederlands(en, allow_woorden):
    en = COMMANDO.sub(" ", en)   # command text (/guhs vadsrijk party accepteer) stays as typed
    tekst = C._STRIP.sub(" ", en)
    woorden = {w.lower().strip("'") for w in C._WORD.findall(tekst)}
    hits = set(C.dutch_leftovers(en, allow_woorden))
    hits |= {w for w in woorden if w in VADS_WOORDEN}
    return sorted(w for w in hits if w not in ENGELS_OK and w not in allow_woorden)


def main(argv):
    nl = lees(os.path.join(LANG, "nl_nl.json"))
    en_us = lees(os.path.join(LANG, "en_us.json"))
    ftb = lees(L.FTB_SOURCE) if os.path.exists(L.FTB_SOURCE) else {}
    overlay, waar = L.load_overlay(with_files=True)
    bron = lees(BRON) if os.path.exists(BRON) else {}
    allow = lees(ALLOW) if os.path.exists(ALLOW) else {}
    allow_sleutels = set(allow.get("sleutels", []))
    allow_waarden = set(allow.get("waarden", []))
    allow_woorden = {w.lower() for w in allow.get("woorden", [])}
    alles = "--alles" in argv

    bron_nl = dict(nl)
    bron_nl.update(ftb)
    basis = None if alles else basis_sleutels()

    def in_scope(k):
        return alles or bool(SCOPE.search(k)) or (basis is not None and not k.startswith("ftb.") and k not in basis)

    sleutels = [k for k in bron_nl if in_scope(k) and L.needs_english(bron_nl[k]) and not L.derived_item(k, nl)]
    cat = {c: [] for c in ("GEEN ENGELS", "NL GEWIJZIGD", "GELIJK AAN NL", "NEDERLANDS?", "FORMAAT", "VEROUDERD", "EN_US OUD")}
    todo = {}
    for k in sleutels:
        dutch = bron_nl[k]
        if k not in overlay:
            cat["GEEN ENGELS"].append(f"{k}  | {dutch[:70]}")
            todo[k] = {"nl": dutch}
            if not k.startswith("ftb.") and en_us.get(k, dutch) != dutch:
                todo[k]["draft"] = en_us[k]
            continue
        en = overlay[k]
        if k in bron and bron[k] != h(dutch):
            cat["NL GEWIJZIGD"].append(f"{k}  | NL nu: {dutch[:60]}  | EN: {en[:50]}")
            todo[k] = {"nl": dutch, "draft": en}
        if en.strip() == dutch.strip() and k not in allow_sleutels and dutch not in allow_waarden:
            cat["GELIJK AAN NL"].append(f"{k}  | {en[:70]}")
            todo.setdefault(k, {"nl": dutch})
        hits = [] if k in allow_sleutels else nederlands(en, allow_woorden)
        if hits and dutch.strip() != en.strip():
            cat["NEDERLANDS?"].append(f"{k}  [{', '.join(hits)}]  | {en[:70]}  ({waar[k]})")
        for p in C.format_problems(dutch, en, k.startswith("ftb.")):
            cat["FORMAAT"].append(f"{k}: {p}  ({waar[k]})")
    for k in overlay:
        if in_scope(k) and k not in bron_nl:
            cat["VEROUDERD"].append(f"{k}  ({waar[k]})")
    verwacht, _ = L.english(nl, overlay)
    oud = [k for k in nl if in_scope(k) and en_us.get(k) != verwacht[k]]
    if oud:
        cat["EN_US OUD"].append(f"{len(oud)} keys, e.g. {', '.join(oud[:5])}  -> python tools/lang/apply_en.py")

    if "--merk" in argv:
        nieuw = dict(bron)
        for k in sleutels:
            if k in overlay:
                nieuw[k] = h(bron_nl[k])
        nieuw = {k: v for k, v in sorted(nieuw.items()) if k in bron_nl}
        L.write_json(BRON, nieuw)
        print(f"engels_bron.json: {len(nieuw)} keys remembered")
        return 0

    if "--todo" in argv:
        pad = argv[argv.index("--todo") + 1]
        L.write_json(pad, dict(sorted(todo.items())))
        print(f"{pad}: {len(todo)} keys to translate")

    lijst = "--lijst" in argv
    print(f"Vadsrijk/1.10 English check: {len(sleutels)} keys in scope{' (all keys)' if alles else ''}"
          f"{'' if basis is not None or alles else ' (no git tag ' + BASIS_TAG + ': prefix scope only)'}")
    slecht = False
    for c, rijen in cat.items():
        print(f"  {c:<15} {len(rijen)}")
        slecht |= bool(rijen)
    for c, rijen in cat.items():
        if not rijen:
            continue
        print(f"\n== {c} ({len(rijen)})")
        for r in (rijen if lijst else rijen[:25]):
            print("  ", r)
        if not lijst and len(rijen) > 25:
            print(f"   ... and {len(rijen) - 25} more (--lijst)")
    print("\nOK" if not slecht else "\nWORK TO DO (see above; --todo FILE writes the keys for a translator)")
    return 1 if slecht else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
