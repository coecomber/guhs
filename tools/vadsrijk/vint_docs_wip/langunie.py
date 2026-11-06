"""Key union of a conflicted lang json: python langunie.py <path> (run in the repo root during a merge)."""
import json, re, subprocess, sys

PAT = re.compile(r'^\s*("(?:[^"\\]|\\.)*")\s*:\s*(.*?),?\s*$')


def show(rev, path):
    r = subprocess.run(["git", "show", f"{rev}:{path}"], capture_output=True)
    return r.stdout.decode("utf-8") if r.returncode == 0 else "{\n}\n"


def parse(txt):
    keys, raw = [], {}
    for line in txt.splitlines():
        m = PAT.match(line)
        if not m:
            continue
        k = json.loads(m.group(1))
        if k not in raw:
            keys.append(k)
        raw[k] = m.group(2)
    return keys, raw


path = sys.argv[1]
base = subprocess.run(["git", "merge-base", "HEAD", "MERGE_HEAD"], capture_output=True, text=True).stdout.split()
bk, braw = parse(show(base[0], path)) if base else ([], {})
ok, oraw = parse(show("HEAD", path))
tk, traw = parse(show("MERGE_HEAD", path))
res = dict(oraw)
for k in tk:
    if k in res:
        if traw[k] != braw.get(k) and oraw[k] == braw.get(k):
            res[k] = traw[k]
        elif traw[k] != oraw[k] and traw[k] != braw.get(k) and oraw[k] != braw.get(k):
            print("both changed, keeping ours:", k)
ins = {}
prev = None
for k in tk:
    if k not in res:
        if k in braw and traw[k] == braw[k]:
            continue   # we deleted it, they left it alone
        res[k] = traw[k]
        ins.setdefault(prev, []).append(k)
    else:
        prev = k
seq = list(ins.get(None, []))
for k in ok:
    seq.append(k)
    seq.extend(ins.get(k, []))
tset = set(tk)
dele = [k for k in seq if k in braw and k not in tset and oraw.get(k) == braw.get(k)]
seq = [k for k in seq if k not in set(dele)]
lines = ["{"]
for i, k in enumerate(seq):
    lines.append("  " + json.dumps(k, ensure_ascii=False) + ": " + res[k] + ("," if i < len(seq) - 1 else ""))
lines.append("}")
txt = "\n".join(lines) + "\n"
json.loads(txt)
open(path, "w", encoding="utf-8", newline="\n").write(txt)
print(path, "keys:", len(seq), "added:", sum(len(v) for v in ins.values()), "deleted:", len(dele))
