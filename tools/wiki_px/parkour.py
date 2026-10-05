"""
The wiki part of the guhpixel slice "parkour" (a stub from the foundation; the slice fills it in).

body(w) returns the HTML of this slice's part, built with the helpers of tools/make_wiki.py: w.h3(en, nl), w.p(en, nl),
w.ul([(en, nl), ...]), w.table(head, rows), w.entry(image, title_en, title_nl, body, stats), w.img(name, alt),
w.recipe_card(id). Every text is written twice, (English, Dutch); the English is hand-written here and follows
tools/lang/GLOSSARY.md and tools/lang/glossary_px. No version numbers ("New in ...").
renders(r) adds this slice's pictures docs/wiki/img/<namespace>_*.png: r is tools/wiki_renders.py, r.OUT the folder.
"""


def body(w):
    return ""


def renders(r):
    pass
