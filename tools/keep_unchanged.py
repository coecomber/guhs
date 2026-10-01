"""
After a full regeneration (python tools/make_resources.py): put back the files the generators rewrote without a real change,
so `git status` only shows what really changed.

  - .nbt: gzip stores the time it was written, so every template differs in bytes; restored when the decompressed NBT
    is the same as in git (HEAD).
  - .png: restored when the pixels (and mode/size) are the same.
  - .ogg: the sound generators aren't byte-for-byte reproducible; restored unless named with --keep.
  - text files that only got CRLF line ends (git stores LF): restored.
  - HAND_FIXED: files fixed by hand after the generator (the 26.1.2 visual check, a495d42) that a regeneration would undo.

Run from the project root:  python tools/keep_unchanged.py [--keep path ...] [--dry]
"""
import gzip
import io
import subprocess
import sys

HAND_FIXED = [
    "src/main/resources/assets/guhs/models/block/elftocht_vuurkorf_aan.json",
    "src/main/resources/assets/guhs/models/block/feestbuffettafel_gedekt.json",
    "src/main/resources/assets/guhs/models/block/theetafel_gedekt.json",
]


def git(*args, binary=False):
    out = subprocess.run(["git", *args], capture_output=True, check=True).stdout
    return out if binary else out.decode("utf-8")


def changed_files():
    out = []
    for line in git("status", "--porcelain", "-z").split("\0"):
        if len(line) > 3 and line[:2].strip() == "M":
            out.append(line[3:])
    return out


def same_png(path, head):
    from PIL import Image
    a, b = Image.open(path), Image.open(io.BytesIO(head))
    return a.mode == b.mode and a.size == b.size and a.tobytes() == b.tobytes()


def main():
    keep = set(sys.argv[sys.argv.index("--keep") + 1:]) if "--keep" in sys.argv else set()
    dry = "--dry" in sys.argv
    restore, real = [], []
    # (text files the generators wrote with CRLF but the same content: git normalises to LF, so they aren't really changed)
    differs = set()
    for line in git("diff", "--numstat", "-z").split("\0"):
        parts = line.split("\t")
        if len(parts) == 3 and parts[:2] != ["0", "0"]:
            differs.add(parts[2])
    for path in changed_files():
        if path not in differs:
            restore.append(path)
            continue
        if path in keep:
            real.append(path)
            continue
        if path in HAND_FIXED or path.endswith(".ogg"):
            restore.append(path)
            continue
        if path.endswith(".nbt") or path.endswith(".png"):
            head = git("show", f"HEAD:{path}", binary=True)
            if path.endswith(".nbt"):
                same = gzip.decompress(open(path, "rb").read()) == gzip.decompress(head)
            else:
                same = same_png(path, head)
            (restore if same else real).append(path)
        else:
            real.append(path)
    if restore and not dry:
        for i in range(0, len(restore), 100):
            git("checkout", "--", *restore[i:i + 100])
    print(f"{'would restore' if dry else 'restored'} {len(restore)} files without a real change")
    for path in real:
        print("  really changed:", path)


if __name__ == "__main__":
    main()
