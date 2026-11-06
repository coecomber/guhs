import re
p = "tools/features/__init__.py"
s = open(p, encoding="utf-8", newline="").read()
blok = re.search(r"\n( *# 1\.10 T-VFX:[^\n]*\n *# so it must stay[^\n]*\n *'vadsrijk_t_vfx',\n)", s)
if blok:
    s = s.replace(blok.group(1), "", 1)
    i = s.index("            ]\n\n\ndef modules")
    s = s[:i] + blok.group(1) + s[i:]
open(p, "w", encoding="utf-8", newline="").write(s)
