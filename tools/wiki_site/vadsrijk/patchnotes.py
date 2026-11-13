"""
Patch notes: ONE source for the Discord changelog and the site (user decision W2). One markdown file per release in
docs/patchnotes/<versie>.md:

    ---
    versie: 1.10.0
    datum: 2026-11-20
    titel: Het Vadsrijk!
    samenvatting: one line for the list
    concept: true                      (drafts only show with --concepten; never on the real site)
    discord_beelden: [img/a.png, repo:src/main/resources/...png]   (max 4, Discord's limit)
    ---
    Markdown body: # ## ### headings, - lists, **bold**, *italic*, [links](url), ![alt](img/x.png) pictures.
    :::spoiler Title
    Only on the site, behind a spoiler button (Discord skips the whole block, pictures included).
    :::
    :::zone 3
    X-SITE: only when zone 3 is open (--open-zones >= 3): site AND Discord. Blocks may hold :::spoiler blocks.
    :::
    :::tot 6
    X-SITE: only while zone 6 is NOT open yet (--open-zones < 6), e.g. "more zones follow".
    :::

`python tools/patchnote_discord.py <versie>` prints the Discord payload (text without the spoiler blocks + the discord_beelden);
it only posts with --post and the webhook in the GUHS_DISCORD_WEBHOOK environment variable (never in the repo).
"""
import html
import os
import re

ESC = lambda s: html.escape(s, quote=True)  # noqa: E731


def _kop(tekst):
    m = re.match(r"^---\n(.*?)\n---\n?(.*)$", tekst, re.S)
    if not m:
        return {}, tekst
    meta = {}
    for regel in m[1].splitlines():
        if ":" in regel:
            k, v = regel.split(":", 1)
            v = v.strip()
            if v.startswith("[") and v.endswith("]"):
                v = [x.strip() for x in v[1:-1].split(",") if x.strip()]
            elif v.lower() in ("true", "false"):
                v = v.lower() == "true"
            meta[k.strip()] = v
    return meta, m[2]


def zone_filter(body, open_zones=6):
    """X-SITE: keeps a `:::zone N` block only when N <= open_zones and a `:::tot N` block only when open_zones < N; drops the markers."""
    uit, stack = [], []
    for regel in body.split(chr(10)):
        r = regel.strip()
        m = re.match(r"^:::(zone|tot)\s+(\d+)\s*$", r)
        if m:
            n = int(m[2])
            stack.append(("zone", n <= open_zones) if m[1] == "zone" else ("zone", open_zones < n))
            continue
        if r.startswith(":::spoiler"):
            stack.append(("spoiler", True))
        elif r == ":::" and stack:
            soort, _ = stack.pop()
            if soort == "zone":
                continue
        if all(z for _, z in stack):
            uit.append(regel)
    return chr(10).join(uit)


def lees(map_, concepten=False, open_zones=6):
    out = []
    if not os.path.isdir(map_):
        return out
    for f in os.listdir(map_):
        if not f.endswith(".md") or f.upper().startswith("LEESMIJ"):
            continue
        tekst = open(os.path.join(map_, f), encoding="utf-8").read().replace("\r\n", "\n")
        meta, body = _kop(tekst)
        if meta.get("concept") and not concepten:
            continue
        body = zone_filter(body, open_zones)   # X-SITE
        if open_zones < 6 and meta.get("samenvatting_open"):
            meta["samenvatting"] = meta["samenvatting_open"]
        beelden = [dict(pad=_pad(b)) for b in re.findall(r"!\[[^\]]*\]\(([^)\s]+)\)", body)]
        out.append(dict(versie=str(meta.get("versie", f[:-3])), datum=str(meta.get("datum", "")), titel=str(meta.get("titel", f[:-3])),
                        samenvatting=str(meta.get("samenvatting", "")), concept=bool(meta.get("concept")), body=body,
                        discord_beelden=[_pad(b) for b in (meta.get("discord_beelden") or [])][:4], beelden=beelden))
    out.sort(key=lambda n: [int(x) if x.isdigit() else 0 for x in re.split(r"[.\-]", n["versie"])], reverse=True)
    return out


def _pad(p):
    """'repo:<path>' = relative to the repo root; anything else relative to docs/patchnotes/."""
    return os.path.join("..", "..", p[5:]) if p.startswith("repo:") else p


def _inline(s, img_pre):
    s = ESC(s)
    s = re.sub(r"!\[([^\]]*)\]\(([^)\s]+)\)", lambda m: f'<img src="{img_pre}{os.path.splitext(os.path.basename(m[2]))[0]}.webp" alt="{m[1]}" loading="lazy" style="max-width:100%;border-radius:12px">', s)
    s = re.sub(r"\[([^\]]+)\]\(([^)\s]+)\)", r'<a href="\2">\1</a>', s)
    s = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", s)
    s = re.sub(r"(?<!\*)\*(?!\*)(.+?)\*", r"<i>\1</i>", s)
    return s


def html_van(n, img_pre):
    uit, lijst, para = [], [], []

    def sluit():
        if para:
            uit.append("<p>" + " ".join(para) + "</p>")
            para.clear()
        if lijst:
            uit.append("<ul>" + "".join(f"<li>{x}</li>" for x in lijst) + "</ul>")
            lijst.clear()
    for regel in n["body"].splitlines():
        r = regel.rstrip()
        if r.startswith(":::spoiler"):
            sluit()
            kop = r[len(":::spoiler"):].strip() or "Spoiler"
            uit.append(f'<details class="spoiler"><summary>{ESC(kop)}</summary>')
            continue
        if r == ":::":
            sluit()
            uit.append("</details>")
            continue
        m = re.match(r"^(#{1,3}) (.*)$", r)
        if m:
            sluit()
            lvl = len(m[1]) + 1
            uit.append(f"<h{lvl}>{_inline(m[2], img_pre)}</h{lvl}>")
        elif r.startswith(("- ", "* ")):
            if para:
                sluit()
            lijst.append(_inline(r[2:], img_pre))
        elif not r.strip():
            sluit()
        else:
            if lijst:
                sluit()
            para.append(_inline(r, img_pre))
    sluit()
    return "\n".join(uit)


def discord_tekst(n):
    """The Discord version: the same text without the spoiler blocks and without inline pictures."""
    regels, in_spoiler = [], False
    for r in n["body"].splitlines():
        if r.startswith(":::spoiler"):
            in_spoiler = True
            continue
        if r.strip() == ":::":
            in_spoiler = False
            continue
        if in_spoiler or re.match(r"^\s*!\[", r):
            continue
        r = re.sub(r"^#{1,3} (.*)$", r"**\1**", r)
        regels.append(r)
    tekst = "\n".join(regels).strip()
    return re.sub(r"\n{3,}", "\n\n", tekst)
