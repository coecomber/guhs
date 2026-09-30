"""
Turns the pages into HTML files: the page template (header with search, NL/EN and theme toggles, the category sidebar,
breadcrumbs, the infobox), the knowledge-base blocks (cleaned of release framing and auto-linked), the category lists, the
home page, the 404 page, the search index, sitemap.xml and the favicon. Every link and picture is written relative, so the
site works under any sub-path (GitHub Pages: /guhs/).
"""
import html
import json
import os
import re
from xml.sax.saxutils import escape as xml_escape

from PIL import Image

from . import assets
from . import topics as T
from .kb import plain, split_t
from .pages import SITE_VERSION, clean_title, first_sentences
from .site import CAT_ICON, CAT_ORDER, CATEGORIES, esc, fold, p, t

SECTION_ORDER = ["start", "mobs", "care", "personalities", "maag", "sled", "guhdex", "food", "items", "vads", "redstone", "guhmension",
                 "villages", "structures", "reizen", "new21", "new22", "new23", "new24", "rare24", "new25", "new26", "new27", "new28",
                 "new281", "new29", "fixes210", "new210", "fixes2101", "new30", "more", "recipes"]
AUTOLINK_SKIP_TAGS = {"a", "h1", "h2", "h3", "h4", "code", "summary", "button", "th", "dt", "script", "style", "kbd", "b"}
STOP = {"guh", "guhs", "vads", "njeg", "vahoeg", "kaas", "knus", "samen", "blij", "lief", "stil", "emotes", "effecten", "items", "blokken",
        "slee", "blokje", "klein", "groot", "medium", "makkelijk", "lastig", "kist", "hart", "eten", "vis", "ster", "gras", "tong", "tand",
        "wol", "roze", "kleding", "outfit", "hula", "surfen", "guhdex", "model", "klant", "speelgoed", "knuffel", "set", "portaal",
        "kapper", "bakker", "juf", "opa", "baas", "koning", "gezellig", "besties", "uitgerust", "zweverig", "onvahoeg", "blosjes",
        "sjokkel's", "mint", "choco", "snow", "starry", "ghost", "golden", "rainbow", "teckel", "iron", "diamond", "netherite", "inside",
        "the gate", "the guh head", "together", "chores", "toys", "events", "favourites", "commands", "advancements", "surfing", "jobs", "personalities", "effects", "clothes", "stories", "guhland", "guh-sneeuw", "bewoner", "sjoelen", "doolhof", "katapult", "circuit"}
CAT_PRIORITY = ["guhs", "npcs", "diertjes", "wezens", "minigames", "verhalen", "bouwwerken", "biomen", "dimensies", "systemen", "kleding",
                "blokken", "items"]
LIST_COLUMNS = {
    "guhs": [("rarity", "Rarity", "Zeldzaamheid", "num")],
    "kleding": [("slot", "Slot", "Vak", "num"), ("bron", "Source", "Bron", "txt")],
    "items": [("recipe", "Recipe", "Recept", "num"), ("used", "Used in", "Gebruikt in", "num")],
    "blokken": [("recipe", "Recipe", "Recept", "num"), ("used", "Used in", "Gebruikt in", "num")],
    "bouwwerken": [("tab", "Super compass", "Superkompas", "txt"), ("rarity", "Rarity", "Zeldzaamheid", "num")],
    "biomen": [("dim", "Dimension", "Dimensie", "txt"), ("structures", "Structures", "Bouwwerken", "num")],
    "minigames": [("group", "Group", "Groep", "txt")],
    "verhalen": [("kind", "Kind", "Soort", "num")],
}


def trie_regex(words):
    """One regex for many words (a trie, so matching stays fast with thousands of names)."""
    trie = {}
    for w in words:
        node = trie
        for ch in w:
            node = node.setdefault(ch, {})
        node[""] = True

    def build(node):
        if len(node) == 1 and "" in node:
            return ""
        alts, end = [], "" in node
        for ch in sorted(k for k in node if k):
            alts.append(re.escape(ch) + build(node[ch]))
        body = alts[0] if len(alts) == 1 else "(?:" + "|".join(alts) + ")"
        return f"(?:{body})?" if end else body
    return build(trie)


V = r"(?:2\.(?:10\.1|10|8\.1|[1-9])|3\.0)(?![\d.]*\d)"      # the release numbers the one-page wiki talks about
NOT_A_UNIT = r"(?!\s*(?:%|blocks|blokken|x\b|&times;|times|keer|hearts|hartjes))"


def deversion(s):
    """Takes the release history out of a text: 'Since 2.9 clothes are...' -> 'Clothes are...', '(new in 2.7)' -> ''."""
    s = re.sub(r"\s*\((?:new in |nieuw in |since |sinds |vanaf |in |see |zie )?" + V + r"(?:[;,][^)]*)?\)", "", s)
    s = re.sub(r"\s+(?:since|sinds) " + V + r";\s*(?:it used to be|vroeger)[^)]*(?=\))", "", s)
    s = re.sub(r"\s*\((?:since|sinds) " + V + r"\s*;\s*", "(", s)
    s = re.sub(r"[:,;]?\s*(?:[Ss]ee|[Zz]ie) (?:Fixes in |Nieuw in |New in )?" + V + r"\b", "", s)
    s = re.sub(r"\b(?:Also new|Ook nieuw|New|Nieuwe?|Fixes) in " + V + r":?\s*", "", s)
    s = re.sub(r"(^|[.!?:]\s+)(?:[Ss]ince|[Ss]inds|[Vv]anaf|In) " + V + r",?\s+([\w(])", lambda m: m.group(1) + m.group(2).upper(), s)
    s = re.sub(r",?\s+(?:since|sinds|vanaf) " + V + r"\b", "", s)
    s = re.sub(r"\s+(?:in|of|van|uit|from|as in|zoals in|from before|van vóor|before|vóor) " + V + NOT_A_UNIT + r"\b", "", s)
    s = re.sub(r"\b" + V + r"[- ](?=[A-Za-z])" + NOT_A_UNIT, "", s)
    s = re.sub(r"\(\s*\)", "", s)
    return s


class Renderer:
    def __init__(self, site, builder, images, game, out, base_url):
        self.site, self.b, self.im, self.g, self.out, self.base_url = site, builder, images, game, out, base_url.rstrip("/") + "/"
        self.missing_links = {}
        self.version_left = []
        self.outlinks = {}
        self.alt = {}
        for iid, pid in builder.item_page.items():
            for cat in ("items", "blokken", "kleding"):
                self.alt[f"{cat}/{iid}"] = pid

    # --- paths ------------------------------------------------------------------------------------------------------------------
    def resolve(self, pid):
        if pid in self.site.pages:
            return pid
        return self.alt.get(pid)

    @staticmethod
    def rel(from_path, to_path):
        depth = from_path.count("/")
        return "../" * depth + to_path

    # --- auto-links ---------------------------------------------------------------------------------------------------------------
    def build_aliases(self):
        best = {}
        for pg in self.site.pages.values():
            if pg.no_autolink or pg.cat == "home" or pg.id.endswith("/index"):
                continue
            names = set(pg.aliases) | {pg.title}
            if pg.cat in ("systemen", "minigames", "verhalen", "dimensies") and pg.title_en:
                names.add(pg.title_en)
            for n in names:
                n = plain(n)
                key = fold(n)
                if len(key) < 4 or key in STOP or not re.search(r"[a-z]", key):
                    continue
                prio = CAT_PRIORITY.index(pg.cat) if pg.cat in CAT_PRIORITY else 99
                if key not in best or prio < best[key][0]:
                    best[key] = (prio, pg.id)
        self.alias = {k: v[1] for k, v in best.items()}
        self.alias_re = re.compile(r"(?<![\w-])(" + trie_regex(sorted(self.alias, key=len, reverse=True)) + r")(?![\w-])", re.I)

    def autolink(self, h, page, linked):
        out, stack = [], []
        for part in re.split(r"(<[^>]+>)", h):
            if part.startswith("<"):
                m = re.match(r"<(/?)([a-zA-Z0-9]+)", part)
                if m:
                    name = m.group(2).lower()
                    if m.group(1):
                        if name in stack:
                            while stack and stack.pop() != name:
                                pass
                    elif not part.endswith("/>") and name not in ("img", "br", "hr", "input", "meta", "link", "source", "wbr"):
                        if name in AUTOLINK_SKIP_TAGS or 'class="rarity"' in part or 'class="count"' in part:
                            stack.append(name)
                        elif stack:
                            stack.append(name)
                out.append(part)
                continue
            if stack or not part.strip():
                out.append(part)
                continue

            def sub(m):
                key = fold(m.group(1))
                pid = self.alias.get(key)
                if not pid or pid == page.id or pid in linked:
                    return m.group(0)
                linked.add(pid)
                return f'<a href="@@{pid}@@" class="auto">{m.group(1)}</a>'
            out.append(self.alias_re.sub(sub, part))
        return "".join(out)

    # --- cleaning the knowledge base ------------------------------------------------------------------------------------------------
    def clean_body(self, h):
        h = re.sub(r"<a href=['\"]#[^'\"]*['\"]>(.*?)</a>", r"\1", h, flags=re.S)
        h = "".join(part if part.startswith("<") else deversion(part) for part in re.split(r"(<[^>]+>)", h))
        h = re.sub(r"guhs-\d+\.\d+\.\d+\.jar", f"guhs-{SITE_VERSION}.jar", h)
        h = re.sub(r'src="img/([^"]+)\.png"', r'src="@img:\1@"', h)
        h = re.sub(r'<div class="recipes">\s*</div>', "", h)
        for m in re.finditer(r"[^<>]{0,40}\b" + V + NOT_A_UNIT + r"[^<>]{0,40}", h):
            self.version_left.append(m.group(0).strip())
        return h

    def chunk_html(self, c, page, linked, last_head):
        title_en, title_nl = clean_title(c.title_en or ""), clean_title(c.title_nl or "")
        body = self.clean_body(c.body or "")
        body = self.autolink(body, page, linked)
        img = self.clean_body(c.image_html or "")
        if c.kind == "entry":
            stats = ""
            if c.stats:
                stats = '<dl class="stats">' + "".join(f"<div><dt>{t(a, b)}</dt><dd>{v}</dd></div>" for (a, b), v in c.stats) + "</dl>"
            wide = "shot" in img or "structure_" in img or not img.strip()
            if not img.strip():
                return f'<section class="kb-entry wide"><div class="kb-text"><h2>{t(title_en, title_nl)}</h2>{body}{stats}</div></section>'
            return f'<section class="kb-entry{" wide" if wide else ""}"><div class="stage">{img}</div><div class="kb-text"><h2>{t(title_en, title_nl)}</h2>{body}{stats}</div></section>'
        if c.kind in ("card", "figure"):
            sub = f'<span class="rarity">{self.clean_body(c.sub)}</span>' if c.sub and plain(c.sub) else ""
            return f'<figure><div class="stage">{img}</div><figcaption><h3>{t(title_en, title_nl)}{sub}</h3>{body}</figcaption></figure>'
        head = ""
        if c.h3 and fold(plain(c.h3[1])) != fold(page.title) and (title_en, title_nl) != last_head:
            head = f"<h2>{t(title_en, title_nl)}</h2>"
        return f'<section class="kb">{head}{body}</section>'

    def kb_html(self, page, linked):
        chunks = sorted(page.chunks, key=lambda c: (SECTION_ORDER.index(c.section) if c.section in SECTION_ORDER else 99,
                                                    int(c.id.split(":")[1])))
        # the short summaries of the overview sections go away when the page has a real text
        if any(c.kind in ("entry", "text") for c in chunks):
            chunks = [c for c in chunks if not (c.section == "mobs" and c.kind == "card")]
        out, gallery, last_head = [], [], None
        main_pic = page.images[0] if page.images else page.thumb
        lead = fold(plain(page.lead_nl) + " " + plain(page.lead_en))
        keep = []
        for c in chunks:
            if c.kind == "entry" and c.images and c.images[0] == main_pic and len(c.images) == 1:
                c = type(c)(c)
                c["image_html"] = ""
            elif c.kind in ("card", "figure") and c.images and c.images[0] == main_pic:
                body = fold(plain(split_t(c.body)[1] if 'lang="nl"' in (c.body or "") else c.body))
                if not body or body[:60] in lead:
                    continue
                c = type(c)(c)
                c["image_html"] = ""
            keep.append(c)
        chunks = keep
        for c in chunks:
            if c.kind in ("card", "figure"):
                head = (clean_title(c.h3[0]), clean_title(c.h3[1])) if c.h3 else None
                if not gallery and head and head != last_head and fold(plain(head[1])) != fold(page.title):
                    out.append(f"<h2>{t(*head)}</h2>")
                    last_head = head
                gallery.append(self.chunk_html(c, page, linked, last_head))
                continue
            if gallery:
                out.append('<div class="gallery">' + "".join(gallery) + "</div>")
                gallery = []
            out.append(self.chunk_html(c, page, linked, last_head))
            if c.kind == "text" and c.h3:
                last_head = (clean_title(c.title_en), clean_title(c.title_nl))
        if gallery:
            out.append('<div class="gallery">' + "".join(gallery) + "</div>")
        return "".join(out)

    # --- one page -----------------------------------------------------------------------------------------------------------------
    def content_html(self, page):
        if page.data.get("guide_html"):
            return page.data["guide_html"]
        linked = set()
        parts = []
        en = "" if page.data.get("lead_en_kb") else page.lead_en
        nl = "" if page.data.get("lead_nl_kb") else page.lead_nl
        if en or nl:
            if en and nl:
                lead = p(en, nl)
            elif nl:
                lead = f'<p lang="nl">{nl}</p>' + ("" if page.lead_en else f'<p lang="en">{nl}</p>')
            else:
                lead = f'<p lang="en">{en}</p>' + ("" if page.lead_nl else f'<p lang="nl">{en}</p>')
            parts.append(self.autolink(f'<div class="lead">{lead}</div>', page, linked))
        data = "".join(f'<section class="data" id="{key}"><h2>{t(h_en, h_nl)}</h2>{body}</section>' for key, h_en, h_nl, body in page.sections)
        kb = self.kb_html(page, linked)
        if page.cat in ("items", "blokken", "kleding", "biomen"):
            parts += [data, kb]
        else:
            parts += [kb, data]
        rows = page.data.get("table_rows")
        if rows:
            blocks = []
            for head, cells in rows:
                blocks.append(f'<dl class="rowbox">{self.autolink(self.clean_body(cells), page, linked)}</dl>')
            parts.append(f'<section class="data"><h2>{t("At a glance", "In het kort")}</h2>{"".join(blocks)}</section>')
        shown = set(re.findall(r"@img:([^@]+)@", "".join(parts)))
        shown.add(page.thumb)
        extra = [n for n in page.images if n not in shown and self.im.has(n)]
        if extra:
            parts.append('<section class="data"><h2>' + t("Pictures", "Plaatjes") + '</h2><div class="pics">' + "".join(
                f'<figure>{self.b.img(n, page.title, "shot" if n.startswith("shot") else "")}</figure>' for n in extra[:12]) + "</div></section>")
        return "".join(parts)

    def infobox_html(self, page):
        if page.cat == "home" or page.id.endswith("/index") or page.data.get("guide"):
            return ""
        pic = page.images[0] if page.images else page.thumb
        img = self.b.img(pic, page.title, "shot" if pic and pic.startswith("shot") else "") if pic else ""
        rows = "".join(f"<div><dt>{t(en, nl)}</dt><dd>{v}</dd></div>" for en, nl, v in page.infobox)
        title = t(page.title_en, page.title)
        return (f'<aside class="infobox" aria-label="Info">' + (f'<div class="stage">{img}</div>' if img else "")
                + f'<div class="ititle">{title}</div><dl>{rows}</dl></aside>')

    def related_html(self, page, backlinks):
        parts = []
        rel = [r for r in page.related if r in self.site.pages]
        if rel:
            parts.append("<h2>" + t("See also", "Zie ook") + '</h2><div class="cards">' + "".join(self.card(self.site.pages[r]) for r in rel[:16]) + "</div>")
        back = [b for b in backlinks.get(page.id, []) if b not in rel and b != page.id]
        if back:
            by_cat = {}
            for b in back:
                by_cat.setdefault(self.site.pages[b].cat, []).append(b)
            blocks = []
            for cat in CAT_ORDER:
                if cat in by_cat:
                    d = CATEGORIES[cat]
                    blocks.append(f"<h3>{t(d[2], d[1])}</h3>" + ", ".join(
                        f'<a href="@@{b}@@">{t(esc(self.site.pages[b].title_en), esc(self.site.pages[b].title))}</a>'
                        for b in sorted(by_cat[cat], key=lambda x: fold(self.site.pages[x].title))[:40]))
            parts.append('<section class="backlinks"><h2>' + t("Pages that link here", "Pagina's die hierheen linken") + "</h2>" + "".join(blocks) + "</section>")
        return "".join(parts)

    def card(self, pg):
        img = f'<img src="@thumb:{pg.thumb}@" alt="" loading="lazy">' if pg.thumb and self.im.has(pg.thumb) else ""
        kind = CATEGORIES[pg.cat][1] if pg.cat in CATEGORIES else ""
        kind_en = CATEGORIES[pg.cat][2] if pg.cat in CATEGORIES else ""
        return f'<a href="@@{pg.id}@@">{img}<span>{t(esc(pg.title_en), esc(pg.title))}<small>{t(kind_en, kind)}</small></span></a>'

    # --- the frame ------------------------------------------------------------------------------------------------------------------
    def sidebar(self, page):
        if not hasattr(self, "_counts"):
            self._counts = {cat: len([p_ for p_ in self.site.by_cat(cat) if not p_.id.endswith("/index")]) for cat in CATEGORIES}
        counts = self._counts
        items = []
        for cat in CAT_ORDER:
            d = CATEGORIES[cat]
            icon = CAT_ICON.get(cat)
            img = f'<img src="@thumb:{icon}@" alt="">' if icon and self.im.has(icon) else ""
            here = ' class="here"' if page.cat == cat and not page.data.get("guide") else ""
            cur = ' aria-current="page"' if page.id == f"{cat}/index" else ""
            items.append(f'<li><a href="@@{cat}/index@@"{here}{cur}>{img}{t(d[2], d[1])}<span class="n">{counts[cat]}</span></a></li>')
        start = [("index", "Home", "Home"), ("systemen/aan-de-slag", "&#9733; Getting started", "&#9733; Aan de slag"), ("systemen/temmen", "Your first guh", "Je eerste guh"), ("dimensies/guhmension", "The Guhmension", "De Guhmensie"),
                 ("systemen/superkompas", "The super compass", "Het superkompas"), ("systemen/ftb-quests", "FTB quests", "FTB-quests")]
        cur_attr = ' aria-current="page"'
        first = "".join(f'<li><a href="@@{pid}@@"{cur_attr if page.id == pid else ""}>{t(en, nl)}</a></li>' for pid, en, nl in start)
        return (f'<nav class="side" id="side" aria-label="Wiki"><h2>{t("Start", "Begin")}</h2><ul>{first}</ul>'
                f'<h2>{t("Categories", "Categorieën")}</h2><ul>{"".join(items)}</ul></nav>')

    def crumbs(self, page):
        if page.cat == "home":
            return ""
        parts = [f'<a href="@@index@@">Home</a>']
        if not page.id.endswith("/index") and not page.data.get("guide"):
            d = CATEGORIES[page.cat]
            parts.append(f'<a href="@@{page.cat}/index@@">{t(d[4], d[3])}</a>')
        parts.append(f'<span aria-current="page">{t(esc(page.title_en), esc(page.title))}</span>')
        return '<nav class="crumbs" aria-label="Breadcrumb">' + '<span aria-hidden="true">&rsaquo;</span>'.join(parts) + "</nav>"

    def frame(self, page, main, title_text, description, root_override=None, head_extra=""):
        root = "../" * page.path.count("/") if root_override is None else root_override
        search = ('<div class="search" role="search"><span class="mag" aria-hidden="true"></span>'
                  '<input id="q" type="search" autocomplete="off" placeholder="Zoek in de wiki..." data-ph-nl="Zoek in de wiki... (druk /)" '
                  'data-ph-en="Search the wiki... (press /)" aria-label="Zoeken / Search" aria-controls="results">'
                  '<div class="results" id="results" role="listbox"></div></div>')
        tools = ('<div class="tools"><span class="seg" role="group" aria-label="Taal / Language">'
                 '<button type="button" data-setlang="nl" aria-pressed="true">NL</button><button type="button" data-setlang="en" aria-pressed="false">EN</button>'
                 '</span><button class="icon-btn" id="theme" type="button" aria-label="Thema">&#9790;</button></div>')
        header = (f'<header class="top"><div class="in"><button class="icon-btn menu-btn" id="menu" type="button" aria-label="Menu" aria-expanded="false" '
                  f'aria-controls="side">&#9776;</button><a class="brand" href="@@index@@"><img src="{root}favicon-64.png" alt="" width="32" height="32">'
                  f'<span class="word">Guhs Wiki</span></a><span class="ver" title="Versie">v{SITE_VERSION}</span>{search}{tools}</div></header>')
        footer = (f'<footer class="site"><div class="in">{t("The Guhs wiki, for Guhs " + SITE_VERSION + " (Minecraft 1.21.1, NeoForge). Every picture is rendered from the mod&#39;s own models and textures. Model by Lieke.", "De Guhs-wiki, voor Guhs " + SITE_VERSION + " (Minecraft 1.21.1, NeoForge). Alle plaatjes zijn gerenderd uit de modellen en textures van de mod zelf. Model door Lieke.")}'
                  f' &middot; <a href="@@systemen/commandos@@">{t("Commands", "Commando&#39;s")}</a></div></footer>')
        return f"""<!doctype html>
<html lang="nl" data-lang="nl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{esc(title_text)}</title>
<meta name="description" content="{esc(description)}">
<meta name="theme-color" content="#d9467a">
<script>{assets.EARLY_JS}</script>
{head_extra}<link rel="icon" type="image/png" sizes="32x32" href="{root}favicon-32.png">
<link rel="icon" href="{root}favicon.ico" sizes="any">
<link rel="apple-touch-icon" href="{root}apple-touch-icon.png">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Fredoka:wght@500;600;700&amp;family=Nunito+Sans:ital,wght@0,400;0,700;1,400&amp;family=JetBrains+Mono:wght@500;600&amp;display=swap">
<link rel="stylesheet" href="{root}assets/site.css">
</head>
<body data-root="{root}">
<a class="skip" href="#main">{t("Skip to content", "Naar de inhoud")}</a>
{header}
<div class="shell">
{self.sidebar(page)}
<main id="main">
{main}
</main>
</div>
{footer}
<script src="{root}assets/site.js" defer></script>
</body>
</html>
"""

    def page_main(self, page, backlinks):
        kind = f'<span class="kind">{t(page.kind_en, page.kind_nl)}</span>' if page.kind_nl else ""
        sub = f'<span class="en-sub" lang="nl">{esc(page.title_en)}</span>' if page.title_en and page.title_en != page.title and page.cat in ("systemen", "minigames", "verhalen", "dimensies") else ""
        h1 = f"<h1>{t(esc(page.title_en), esc(page.title))}{sub}</h1>"
        body = page.data["content"]
        info = self.infobox_html(page)
        rel = self.related_html(page, backlinks)
        cls = "article guide" if page.data.get("guide") else "article"
        return f'{self.crumbs(page)}{kind}{h1}<div class="{cls}"><div class="content">{body}{rel}</div>{info}</div>'

    # --- tokens -> relative paths -------------------------------------------------------------------------------------------------------
    def finalize(self, page_path, h, owner):
        def title_of(pid):
            pg = self.site.pages[pid]
            return t(esc(pg.title_en), esc(pg.title))

        def link(m):
            pre, pid, attrs, label = m.group(1), m.group(2), m.group(3), m.group(4)
            real = self.resolve(pid)
            if not real:
                self.missing_links.setdefault(pid, set()).add(owner)
                return label.replace("@T@", esc(pid.split("/")[-1]))
            label = label.replace("@T@", title_of(real))
            return f'<a {pre}href="{self.rel(page_path, self.site.pages[real].path)}"{attrs}>{label}</a>'
        h = re.sub(r'src="img/([^"]+)\.png"', r'src="@img:\1@"', h)
        h = re.sub(r'<a ([^>]*?)href="@@([^"@]+)@@"([^>]*)>(.*?)</a>', link, h, flags=re.S)

        def img(m):
            tag, name = m.group(0), m.group(1)
            thumb = tag.find("@thumb:") >= 0
            url = self.im.use(name, thumb=thumb)
            if not url:
                return ""
            tag = tag.replace(f"@{'thumb' if thumb else 'img'}:{name}@", self.rel(page_path, url))
            if not thumb and 'class="px"' not in tag and max(self.im.size(name)) <= 64:
                tag = tag.replace("<img ", '<img data-pix="1" ', 1)
            return tag
        h = re.sub(r'<img[^>]*src="@(?:img|thumb):([^@"]+)@"[^>]*>', img, h)
        return h

    # --- category pages and home ------------------------------------------------------------------------------------------------------
    def list_main(self, cat, page, backlinks):
        d = CATEGORIES[cat]
        pages = [p_ for p_ in self.site.by_cat(cat) if not p_.id.endswith("/index")]
        def order(x):
            v = x.data.get("sort", x.title)
            return (0, v, "") if isinstance(v, int) else (1, 0, fold(str(v)))
        pages.sort(key=order)
        cols = LIST_COLUMNS.get(cat, [])
        kinds = sorted({(x.kind_nl, x.kind_en) for x in pages if x.kind_nl})
        chips = ""
        if len(kinds) > 1:
            chips = "".join(f'<button class="chip" type="button" data-kind="{esc(nl)}" aria-pressed="false">{t(esc(en), esc(nl))}</button>' for nl, en in kinds)
        head = ('<tr><th></th><th data-sort="txt">' + t("Name", "Naam") + '</th><th data-sort="txt">' + t("Kind", "Soort") + "</th>"
                + "".join(f'<th data-sort="{typ}">{t(en, nl)}</th>' for _, en, nl, typ in cols) + "<th>" + t("About", "Over") + "</th></tr>")
        rows = []
        for x in pages:
            thumb = f'<img src="@thumb:{x.thumb}@" alt="" loading="lazy">' if x.thumb and self.im.has(x.thumb) else ""
            summary = first_sentences(plain(x.lead_nl), 110)
            summary_en = first_sentences(plain(x.lead_en), 110) if x.lead_en else summary
            extra = ""
            for key, _, _, typ in cols:
                v = x.columns.get(key)
                if v:
                    num_cls = ' class="num"' if typ == "num" and re.fullmatch(r"-?\d+", str(v[1] or "0")) else ""
                    extra += f'<td data-v="{esc(str(v[0]))}"{num_cls}>{v[1]}</td>'
                else:
                    extra += '<td data-v=""></td>'
            text = fold(" ".join([x.title, x.title_en] + sorted(x.aliases) + [summary]))
            rows.append(f'<tr data-kind="{esc(x.kind_nl)}" data-text="{esc(text)}"><td class="th">{thumb}</td>'
                        f'<td class="name" data-v="{esc(fold(x.title))}"><a href="@@{x.id}@@">{t(esc(x.title_en), esc(x.title))}</a></td>'
                        f"<td>{t(esc(x.kind_en), esc(x.kind_nl))}</td>{extra}<td class=\"sum\">{t(esc(summary_en), esc(summary))}</td></tr>")
        kb = page.data.get("content", "")
        filters = (f'<div class="filters"><input id="filter" type="search" placeholder="Filter..." aria-label="Filter">{chips}'
                   f'<span class="count-line"><span id="shown">{len(pages)}</span> / {len(pages)}</span></div>')
        table = f'<div class="tscroll"><table class="list"><thead>{head}</thead><tbody>{"".join(rows)}</tbody></table></div>'
        return f'{self.crumbs(page)}<h1>{t(d[4], d[3])}</h1>{kb}{filters}{table}'

    def home_main(self, page):
        g = self.g
        start = next((c for c in self.b.chunks if c.section == "start"), None)
        start_html = self.clean_body(start.body) if start else ""
        intro_p = start_html.split("<ul>", 1)
        intro, install = (intro_p[0], "<ul>" + intro_p[1]) if len(intro_p) == 2 else (start_html, "")
        counts = {cat: len([x for x in self.site.by_cat(cat) if not x.id.endswith("/index")]) for cat in CATEGORIES}
        tiles = []
        for cat in CAT_ORDER:
            d = CATEGORIES[cat]
            icon = CAT_ICON.get(cat)
            img = f'<img src="@thumb:{icon}@" alt="" loading="lazy">' if icon and self.im.has(icon) else ""
            tiles.append(f'<a href="@@{cat}/index@@"><span class="stage">{img}</span><b>{t(d[2], d[1])}</b><small>{counts[cat]} '
                         f'{t("pages", "pagina&#39;s")}</small></a>')
        facts = [v for k, v in g.lang_nl.items() if k.startswith("gui.guhs.wistjedat.") and "%s" not in v and v.startswith("Wist je dat")]
        fact_html = "".join(f'<li class="fact">{esc(f)}</li>' for f in facts[:: max(1, len(facts) // 5)][:5])
        dl = ('<div class="dl">'
              '<a class="btn" href="https://www.curseforge.com/minecraft/mc-mods/guhs" rel="noopener">CurseForge</a>'
              '<a class="btn" href="https://modrinth.com/mod/guhs" rel="noopener">Modrinth</a>'
              f'<a class="btn" href="https://github.com/coecomber/guhs/releases" rel="noopener">GitHub &middot; {t("releases", "downloads")}</a></div>')
        hero_img = self.b.img("guh", "Een guh") if self.im.has("guh") else ""
        stats = (f'<ul class="pills"><li>{t("Version", "Versie")} <b>{SITE_VERSION}</b></li><li>Minecraft <b>1.21.1</b></li><li>NeoForge</li>'
                 f'<li>GeckoLib <b>4.8+</b></li><li><b>{counts["guhs"]}</b> {t("kinds of guh", "guhsoorten")}</li>'
                 f'<li><b>{counts["bouwwerken"]}</b> {t("structures", "bouwwerken")}</li><li><b>{counts["minigames"]}</b> minigames</li></ul>')
        what = p("Guhs is a Minecraft mod full of <b>lieve vadsige guhs</b>: chubby pink plush mice you can tame, dress up, ride and cuddle. "
                 "Walk through a portal of blocks of kaasknabbels into the <b>Guhmension</b>: a pink world of wool and cheese sauce with guh villages, "
                 "a guh theme park, minigames, stories, critters, a very hot barbecue dimension and an endgame against Opper-Mika.",
                 "Guhs is een Minecraft-mod vol <b>lieve vadsige guhs</b>: mollige roze knuffelmuisjes die je kunt temmen, aankleden, berijden en knuffelen. "
                 "Stap door een portaal van blokken kaasknabbels de <b>Guhmensie</b> in: een roze wereld van wol en kaassaus met guhdorpen, "
                 "een guhpretpark, minigames, verhalen, diertjes, een heel hete barbecuedimensie en een eindspel tegen Opper-Mika.")
        cta = (f'<a class="start-cta" href="@@systemen/aan-de-slag@@"><span class="cta-art">{self.b.img("npc_reisguh", "") if self.im.has("npc_reisguh") else ""}</span>'
               f'<span class="cta-txt"><small>{t("New here? Start here!", "Nieuw hier? Begin hier!")}</small><b>{t("Getting started", "Aan de slag")}</b>'
               f'<span>{t("The step-by-step guide: kaasknabbels, your first guh, the portal, the Reisguh, the super compass and your first goals.", "De stap-voor-stapgids: kaasknabbels, je eerste guh, het portaal, de Reisguh, het superkompas en je eerste doelen.")}</span></span>'
               f'<span class="go">{t("Read the guide", "Lees de gids")} &rarr;</span></a>')
        first = ("<ol>"
                 f'<li>{t("Find a guh and feed it kaasknabbels until it is tame.", "Zoek een guh en voer hem kaasknabbels tot hij tam is.")} '
                 f'<a href="@@systemen/temmen@@">{t("Taming", "Temmen")}</a></li>'
                 f'<li>{t("Make blocks of kaasknabbels and build a portal frame, like a Nether portal.", "Maak blokken kaasknabbels en bouw een portaalframe, zoals een netherportaal.")} '
                 f'<a href="@@dimensies/guhmension@@">{t("The Guhmension", "De Guhmensie")}</a></li>'
                 f'<li>{t("In the Guhmension you get a Guhdex; buy a super compass to find every building.", "In de Guhmensie krijg je een Guhdex; met een superkompas vind je elk gebouw.")} '
                 f'<a href="@@systemen/superkompas@@">{t("Super compass", "Superkompas")}</a></li>'
                 f'<li>{t("Play the minigames, follow the stories and collect clothes for your guh.", "Speel de minigames, volg de verhalen en verzamel kleding voor je guh.")} '
                 f'<a href="@@minigames/index@@">Minigames</a> &middot; <a href="@@verhalen/index@@">{t("Stories", "Verhalen")}</a></li></ol>')
        return (f'<section class="hero"><div><h1>{t("Welcome to the <em>Guhs</em> wiki", "Welkom op de <em>Guhs</em>-wiki")}</h1>'
                f'<p class="tagline">{t("Everything about the lieve vadsige guhs, for Minecraft 1.21.1.", "Alles over de lieve vadsige guhs, voor Minecraft 1.21.1.")}</p>'
                f'{stats}</div><div class="hero-art">{hero_img}</div></section>'
                f'{cta}'
                f'<div class="box" id="wat"><h2>{t("What is Guhs?", "Wat is Guhs?")}</h2>{what}{intro}</div>'
                f'<h2>{t("Browse the wiki", "Blader door de wiki")}</h2><div class="tiles">{"".join(tiles)}</div>'
                f'<div class="box" id="start"><h2>{t("In short", "In het kort")}</h2>{first}'
                f'<p><a href="@@systemen/aan-de-slag@@">{t("The whole guide, step by step", "De hele gids, stap voor stap")} &rarr;</a></p></div>'
                f'<div class="box" id="installeren"><h2>{t("Install", "Installeren")}</h2>{install}</div>'
                f'<div class="box" id="download"><h2>{t("Download", "Downloaden")}</h2>'
                f'<p>{t("Guhs " + SITE_VERSION + " will be available on these sites:", "Guhs " + SITE_VERSION + " komt op deze sites:")}</p>{dl}</div>'
                + (f'<div class="box"><h2>{t("Did you know? (from the game, in Dutch)", "Wist je dat?")}</h2><ul>{fact_html}</ul></div>' if fact_html else ""))

    # --- everything ------------------------------------------------------------------------------------------------------------------
    def render_all(self):
        self.build_aliases()
        pages = list(self.site.pages.values())
        for pg in pages:
            if pg.cat == "home":
                pg.data["content"] = ""
                continue
            pg.data["content"] = self.content_html(pg)
        backlinks = {}
        for pg in pages:
            txt = pg.data.get("content", "") + "".join(v for _, _, v in pg.infobox)
            for pid in set(re.findall(r'href="@@([^"@]+)@@"', txt)):
                real = self.resolve(pid)
                if real and real != pg.id and not pg.id.endswith("/index") and pg.cat != "home":
                    backlinks.setdefault(real, []).append(pg.id)
        written = []
        for pg in pages:
            if pg.cat == "home":
                main = self.home_main(pg)
                title = f"Guhs Wiki {SITE_VERSION}"
                desc = "De wiki van Guhs, de Minecraft-mod vol lieve vadsige guhs. The wiki of Guhs, the Minecraft mod full of chubby pink guhs."
            elif pg.id.endswith("/index"):
                main = self.list_main(pg.cat, pg, backlinks)
                title = f"{pg.title} - Guhs Wiki"
                desc = plain(pg.lead_nl)
            else:
                main = self.page_main(pg, backlinks)
                title = f"{pg.title} - Guhs Wiki"
                desc = first_sentences(plain(pg.lead_nl) or plain(pg.lead_en), 155)
            h = self.frame(pg, main, title, desc)
            h = self.finalize(pg.path, h, pg.id)
            dst = os.path.join(self.out, pg.path)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with open(dst, "w", encoding="utf-8", newline="\n") as f:
                f.write(h)
            written.append(pg.path)
        self.write_404()
        self.write_search_index()
        self.write_sitemap(written)
        self.write_assets()
        return written

    def write_404(self):
        dirs = json.dumps(sorted({CATEGORIES[c][0] for c in CATEGORIES}))
        head = ("<script>(function(){var p=location.pathname,r=p.replace(/[^\\/]*$/,''),d=" + dirs +
                ";for(var i=0;i<d.length;i++){var k=p.indexOf('/'+d[i]+'/');if(k>=0){r=p.slice(0,k+1);break;}}"
                "document.write('<base href=\"'+r+'\">');})();</script>\n")
        from .site import Page
        pg = Page("home", "404", "Niet gevonden", "Not found")
        main = (f'<h1>{t("Njeg! Page not found", "Njeg! Pagina niet gevonden")}</h1>'
                f'<p>{t("This page does not exist (any more). Try the search box at the top, or go back to the start.", "Deze pagina bestaat niet (meer). Probeer het zoekvak bovenaan, of ga terug naar het begin.")}</p>'
                f'<p><a href="@@index@@">{t("To the home page", "Naar de startpagina")}</a></p>'
                + (self.b.img("guh_sitting", "") if self.im.has("guh_sitting") else ""))
        h = self.frame(pg, main, "Niet gevonden - Guhs Wiki", "Pagina niet gevonden", root_override="", head_extra=head)
        h = self.finalize("404.html", h, "404")
        with open(os.path.join(self.out, "404.html"), "w", encoding="utf-8", newline="\n") as f:
            f.write(h)

    def write_search_index(self):
        out = []
        for pg in self.site.pages.values():
            if pg.cat == "home":
                continue
            d = CATEGORIES[pg.cat]
            thumb = self.im.use(pg.thumb, thumb=True) if pg.thumb and self.im.has(pg.thumb) else ""
            e = dict(t=plain(pg.title), c=d[1] if not pg.id.endswith("/index") else "Lijst", u=pg.path, i=thumb or "",
                     s=first_sentences(plain(pg.lead_nl), 120))
            if pg.title_en != pg.title:
                e["e"] = plain(pg.title_en)
            if d[2] != d[1]:
                e["ce"] = d[2] if not pg.id.endswith("/index") else "List"
            al = sorted(a for a in pg.aliases if a != pg.title)
            if al:
                e["k"] = " ".join(al)
            if pg.data.get("guide"):
                e["p"] = 40
                e["c"], e["ce"] = "Gids", "Guide"
            elif pg.id.endswith("/index"):
                e["p"] = 30
            elif pg.cat in ("guhs", "npcs", "minigames", "verhalen", "systemen", "dimensies", "bouwwerken", "biomen", "wezens", "diertjes"):
                e["p"] = 10
            out.append(e)
        os.makedirs(os.path.join(self.out, "assets"), exist_ok=True)
        with open(os.path.join(self.out, "assets", "search-index.js"), "w", encoding="utf-8", newline="\n") as f:
            f.write("window.GUHS_INDEX=" + json.dumps(out, ensure_ascii=False, separators=(",", ":")) + ";\n")
        self.search_count = len(out)

    def write_sitemap(self, paths):
        urls = "".join(f"<url><loc>{xml_escape(self.base_url + ('' if p_ == 'index.html' else p_))}</loc></url>\n" for p_ in sorted(paths))
        with open(os.path.join(self.out, "sitemap.xml"), "w", encoding="utf-8", newline="\n") as f:
            f.write('<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n' + urls + "</urlset>\n")
        with open(os.path.join(self.out, "robots.txt"), "w", encoding="utf-8", newline="\n") as f:
            f.write(f"User-agent: *\nAllow: /\nSitemap: {self.base_url}sitemap.xml\n")

    def write_assets(self):
        os.makedirs(os.path.join(self.out, "assets"), exist_ok=True)
        with open(os.path.join(self.out, "assets", "site.css"), "w", encoding="utf-8", newline="\n") as f:
            f.write(assets.CSS.strip() + "\n")
        with open(os.path.join(self.out, "assets", "site.js"), "w", encoding="utf-8", newline="\n") as f:
            f.write(assets.JS.strip() + "\n")
        with open(os.path.join(self.out, ".nojekyll"), "w") as f:
            f.write("")
        src = None
        for name in ("icon_picked_up_guh", "guh"):
            if self.im.has(name):
                src = self.im._load(name)
                break
        if src is not None:
            bbox = src.getbbox()
            if bbox:
                src = src.crop(bbox)
            side = max(src.size)
            sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
            sq.paste(src, ((side - src.width) // 2, (side - src.height) // 2))
            resample = Image.NEAREST if side <= 64 else Image.LANCZOS
            sq.resize((32, 32), resample).save(os.path.join(self.out, "favicon-32.png"))
            sq.resize((64, 64), resample).save(os.path.join(self.out, "favicon-64.png"))
            sq.resize((180, 180), resample).save(os.path.join(self.out, "apple-touch-icon.png"))
            sq.resize((64, 64), resample).save(os.path.join(self.out, "favicon.ico"), sizes=[(16, 16), (32, 32), (48, 48)])
