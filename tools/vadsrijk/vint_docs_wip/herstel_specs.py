"""Restores lang keys u-specs' standalone generator runs dropped by accident (keeps the obsolete adv10/20/30/50 quest texts out)."""
import json
import re
import subprocess

PAT = re.compile(r'^\s*("(?:[^"\\]|\\.)*")\s*:\s*(.*?),?\s*$')
OUD = re.compile(r"(adv10_|adv20_|z2_adv30_|z3_adv50_)")


def parse(txt):
    keys, raw = [], {}
    for l in txt.splitlines():
        m = PAT.match(l)
        if m:
            k = json.loads(m.group(1))
            if k not in raw:
                keys.append(k)
            raw[k] = m.group(2)
    return keys, raw


def show(rev, f):
    return subprocess.run(["git", "show", f"{rev}:{f}"], capture_output=True).stdout.decode("utf-8")


for taal in ["nl_nl", "en_us"]:
    f = f"src/main/resources/assets/guhs/lang/{taal}.json"
    bk, br = parse(show("eaee06d08", f))
    sk, sr = parse(show("110-u-specs", f))
    ck, cr = parse(open(f, encoding="utf-8").read())
    terug = [k for k in bk if k not in sr and k not in cr and not OUD.search(k)]
    obsolete = [k for k in bk if k not in sr and OUD.search(k)]
    ins = {}
    gezet = set()
    for k in terug:
        i = bk.index(k)
        prev = None
        while i > 0:
            i -= 1
            if bk[i] in cr or bk[i] in gezet:
                prev = bk[i]
                break
        ins.setdefault(prev, []).append(k)
        gezet.add(k)
        cr[k] = br[k]
    seq = []

    def add(k):
        seq.append(k)
        for x in ins.get(k, []):
            add(x)

    for k in ins.get(None, []):
        add(k)
    for k in ck:
        add(k)
    txt = "{\n" + ",\n".join("  " + json.dumps(k, ensure_ascii=False) + ": " + cr[k] for k in seq) + "\n}\n"
    json.loads(txt)
    open(f, "w", encoding="utf-8", newline="\n").write(txt)
    print(taal, "restored", len(terug), "obsolete left out", len(obsolete), "e.g.", terug[:3], [k for k in terug if "reiskaart" in k])
