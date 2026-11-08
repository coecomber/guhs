"""Resolve conflict hunks by keeping BOTH sides (ours, then theirs): python beide.py <file>..."""
import sys

for path in sys.argv[1:]:
    src = open(path, encoding="utf-8", newline="").read().split("\n")
    out, mode, n = [], None, 0
    for line in src:
        if line.startswith("<<<<<<< "):
            mode = "ours"; n += 1
            continue
        if line.startswith("||||||| ") and mode == "ours":
            mode = "base"
            continue
        if line.startswith("=======") and mode in ("ours", "base"):
            mode = "theirs"
            continue
        if line.startswith(">>>>>>> ") and mode == "theirs":
            mode = None
            continue
        if mode == "base":
            continue
        out.append(line)
    open(path, "w", encoding="utf-8", newline="").write("\n".join(out))
    print(path, n, "hunks")
