"""
The quick way to put new English into the game without a full regeneration: rewrites assets/guhs/lang/en_us.json from
nl_nl.json + the overlay (exactly what make_v2.write_lang() and mc26.lang_block_items() together produce), and rebuilds
the FTB Quests texts (python tools/make_ftbquests.py). Run from the project root:

    python tools/lang/apply_en.py          (--no-ftb: only the mod's en_us)

When the Dutch changed (a generator was edited), run the full generation instead: python tools/make_resources.py.
"""
import json
import os
import runpy
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import lang as L  # noqa: E402


def main():
    base = os.path.join(L.root(), "src", "main", "resources", "assets", "guhs", "lang")
    with open(os.path.join(base, "nl_nl.json"), encoding="utf-8") as f:
        nl = json.load(f)
    en, missing = L.english(nl)
    # (generator files are written by json.dump(indent=2, ensure_ascii=False) + newline; keep that format)
    with open(os.path.join(base, "en_us.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(en, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"en_us.json: {len(en)} keys, {missing} still Dutch")
    if "--no-ftb" not in sys.argv:
        os.chdir(L.root())
        sys.argv = [os.path.join("tools", "make_ftbquests.py")]
        runpy.run_path(os.path.join("tools", "make_ftbquests.py"), run_name="__main__")


if __name__ == "__main__":
    main()
