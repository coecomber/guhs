"""
The knowledge base: all the (bilingual) texts of the one-page wiki (tools/make_wiki.py), cut into chunks that the site
generator can put on the right pages.

make_wiki.build() is run with its helpers entry() / section() / h3() swapped for recorders, so nothing has to be copied
from it: when make_wiki.py gets new texts, the site gets them too. A section's body is then cut at its h3 headings into
  * entry  chunks (entry(): a picture, a title, a text and maybe a stats box)
  * card   chunks (<div class="card">: a picture, a title, a short text)
  * figure chunks (<figure> in a gallery: a picture, a title with a subtitle, a text)
  * text   chunks (everything else under one heading: paragraphs, lists, tables, commands)
Recipe grids are left out (the site draws every recipe from the recipe JSON itself).
"""
import html
import importlib.util
import os
import re
import sys

IMG_RE = re.compile(r'src="img/([^"]+)\.png"')
TAG_RE = re.compile(r"<[^>]+>")


def plain(s):
    return html.unescape(TAG_RE.sub("", s or "")).strip()


def split_t(h):
    """'<span lang="en">A</span><span lang="nl">B</span> ...' -> (A, B); plain text -> (text, text)."""
    m = re.search(r'<span lang="en">(.*?)</span><span lang="nl">(.*?)</span>', h or "", re.S)
    if m:
        return m.group(1).strip(), m.group(2).strip()
    s = (h or "").strip()
    return s, s


def take_blocks(src, tag, cls=None):
    """Cuts every balanced <tag class="cls"...>...</tag> out of src: (rest, [(start offset, outer html)])."""
    if cls:
        opener = re.compile(r"<" + tag + r'\b[^>]*class="' + re.escape(cls) + r'"[^>]*>')
    else:
        opener = re.compile(r"<" + tag + r"(\s[^>]*)?>")
    tag_re = re.compile(r"<(/?)" + tag + r"\b[^>]*>")
    out, rest, pos = [], [], 0
    while True:
        m = opener.search(src, pos)
        if not m:
            rest.append(src[pos:])
            break
        depth, i = 0, m.start()
        end = None
        for t in tag_re.finditer(src, m.start()):
            depth += -1 if t.group(1) else 1
            if depth == 0:
                end = t.end()
                break
        if end is None:
            rest.append(src[pos:])
            break
        rest.append(src[pos:m.start()])
        out.append((m.start(), src[m.start():end]))
        pos = end
    return "".join(rest), out


class Chunk(dict):
    __getattr__ = dict.get


def load_make_wiki(tools_dir):
    spec = importlib.util.spec_from_file_location("make_wiki_for_site", os.path.join(tools_dir, "make_wiki.py"))
    mod = importlib.util.module_from_spec(spec)
    sys.path.insert(0, tools_dir)
    spec.loader.exec_module(mod)
    return mod


def extract(root):
    tools_dir = os.path.join(root, "tools")
    mw = load_make_wiki(tools_dir)
    entries, sections, heads = [], [], []

    def entry(image, title_en, title_nl, body, stats=None, wide=False):
        entries.append(dict(image=image, en=title_en, nl=title_nl, body=body, stats=stats or []))
        return f"<!--E{len(entries) - 1}-->"

    def section(sid, en, nl, body, open_=True):
        sections.append(dict(sid=sid, en=en, nl=nl, body=body))
        return ""

    def h3(en, nl):
        heads.append((en, nl))
        return f"<!--H{len(heads) - 1}-->"

    mw.entry, mw.section, mw.h3 = entry, section, h3
    here = os.getcwd()
    try:
        os.chdir(root)
        mw.build()
    finally:
        os.chdir(here)

    chunks = []

    def add(**kw):
        c = Chunk(kw)
        c["id"] = f"{c['section']}:{len(chunks)}"
        c["images"] = IMG_RE.findall(c.get("image_html", "") or "")
        chunks.append(c)
        return c

    for s in sections:
        sid = s["sid"]
        ctx = dict(section=sid, section_title=(s["en"], s["nl"]))
        head = None
        order = 0
        for part in re.split(r"(<!--[EH]\d+-->)", s["body"]):
            m = re.fullmatch(r"<!--([EH])(\d+)-->", part)
            if m and m.group(1) == "H":
                head = heads[int(m.group(2))]
                continue
            if m:
                e = entries[int(m.group(2))]
                order += 1
                add(kind="entry", h3=head, title_en=e["en"], title_nl=e["nl"], image_html=e["image"], body=e["body"],
                    stats=e["stats"], order=order, **ctx)
                continue
            # loose html: recipes out, cards and figures into chunks of their own, sub-headings split the rest
            rest, _ = take_blocks(part, "div", "recipes")
            rest, cards = take_blocks(rest, "div", "card")
            for _, c in cards:
                img_m = re.search(r'<figure class="stage"[^>]*>(.*?)</figure>', c, re.S)
                h_m = re.search(r"<h3>(.*?)</h3>", c, re.S)
                body = c[h_m.end():] if h_m else ""
                body = re.sub(r"</div>\s*$", "", body)
                title, sub = _title_sub(h_m.group(1) if h_m else "")
                order += 1
                add(kind="card", h3=head, title_en=title[0], title_nl=title[1], sub=sub, image_html=img_m.group(1) if img_m else "",
                    body=body, stats=[], order=order, **ctx)
            rest, figs = take_blocks(rest, "figure")
            for _, f in figs:
                if f.startswith("<figure class=\"stage\""):
                    # a bare picture (<figure class="stage">): keep it in the text
                    rest += f
                    continue
                img_m = re.search(r'<div class="stage">(.*?)</div>', f, re.S)
                h_m = re.search(r"<h3>(.*?)</h3>", f, re.S)
                body = f[h_m.end():] if h_m else ""
                body = re.sub(r"</figcaption>\s*</figure>\s*$", "", body)
                title, sub = _title_sub(h_m.group(1) if h_m else "")
                order += 1
                add(kind="figure", h3=head, title_en=title[0], title_nl=title[1], sub=sub, image_html=img_m.group(1) if img_m else "",
                    body=body, stats=[], order=order, **ctx)
            rest = re.sub(r'<div class="(?:gallery|cards|iconrow)">\s*</div>', "", rest)
            # sub-headings written straight into the html (not through h3())
            pieces = re.split(r"(<h3[^>]*>.*?</h3>)", rest, flags=re.S)
            local = head
            for piece in pieces:
                hm = re.fullmatch(r"<h3[^>]*>(.*?)</h3>", piece, re.S)
                if hm:
                    local = split_t(hm.group(1))
                    continue
                if len(plain(piece)) < 3 and "<img" not in piece:
                    continue
                order += 1
                title = local or (s["en"], s["nl"])
                add(kind="text", h3=local, title_en=title[0], title_nl=title[1], image_html=piece, body=piece, stats=[],
                    order=order, **ctx)
    return chunks


def _title_sub(h):
    """A card / figure heading: ((en, nl), subtitle html) (the rarity pill split off, icons dropped)."""
    sub = ""
    m = re.search(r'<span class="rarity">(.*?)</span>(?=\s*$)', h, re.S)
    if m is None:
        m = re.search(r'<span class="rarity">((?:<span lang="en">.*?</span><span lang="nl">.*?</span>)|[^<]*)</span>', h, re.S)
    if m:
        sub = m.group(1)
        h = h[:m.start()] + h[m.end():]
    h = re.sub(r"<img[^>]*>", "", h).strip()
    return split_t(h), sub
