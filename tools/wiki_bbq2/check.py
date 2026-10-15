"""
Checks the English of the bbq2 wiki pages against the Dutch notes (run from the project root):

    python tools/wiki_bbq2/check.py            lists every Dutch text without English, and every English text whose Dutch
                                               changed since it was written (exit code 1 when there is one)
    python tools/wiki_bbq2/check.py --stamp    records the present Dutch as "the English is up to date" (tools/wiki_bbq2/en_hash.json);
                                               run it after you brought tools/wiki_bbq2/en.py up to date
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))

import wiki_bbq2  # noqa: E402


def main():
    root = os.path.dirname(os.path.dirname(HERE))
    b = wiki_bbq2.load(root)
    if "--stamp" in sys.argv:
        from wiki_bbq2 import en
        have = set(en.PAGINA) | {"|".join(k) for k in en.TEKST}
        stamps = {k: wiki_bbq2.sha(nl) for k, nl in sorted(b.bron.items()) if k in have}
        with open(os.path.join(HERE, "en_hash.json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(stamps, f, ensure_ascii=False, indent=0, sort_keys=True)
            f.write("\n")
        print(f"stamped {len(stamps)} texts")
        return 0
    for n in b.notes:
        print(n)
    print(f"{len(b.verhalen)} story pages, {len(b.systemen)} mechanic pages, {len(b.tekst)} texts from {len(b.modules)} notes; {len(b.notes)} to look at")
    return 1 if b.notes else 0


if __name__ == "__main__":
    sys.exit(main())
