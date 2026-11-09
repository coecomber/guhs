"""
The pages of guhs.nl/vadsrijk: static HTML in NL + EN (both in every page; the language switch shows one), the same frame and
fonts as the wiki, the Vadsrijk navigation, the spoiler switch ("toon tot zone X") and spoiler buttons. The game texts are
Dutch in both languages (the game's en_us is the same Dutch), the site's own words are translated.

Spoilers (user decisions W1/W2): a zone switch hides everything of later zones; dialogue, walkthrough steps, secret quests and
story bosses sit behind a spoiler button; secrets are never on the map (only "er liggen hier N geheimen").
"""
import html
import json
import os
import re
import shutil

from . import stijl

ESC = lambda s: html.escape(str(s if s is not None else ""), quote=True)  # noqa: E731

ZONE_NR = {"zone1": 1, "zone2": 2, "zone3": 3, "zone4": 4, "zone5": 5, "zone6": 6}   # U-SITE: + zones 4, 5, 6
SLOT = {"wapen": ("Weapon", "Wapen"), "hoofd": ("Head", "Hoofd"), "borst": ("Chest", "Borst"), "benen": ("Legs", "Benen"),
        "voeten": ("Feet", "Voeten"), "ring": ("Ring", "Ring"), "ketting": ("Necklace", "Ketting"), "oorbel": ("Earring", "Oorbel")}
STAT = {"vads": ("Vads", "Vads"), "kracht": ("Strength", "Kracht"), "behendigheid": ("Agility", "Behendigheid"),
        "wijsheid": ("Wisdom", "Wijsheid"), "verdediging": ("Defence", "Verdediging"), "kritiek": ("Crit %", "Kritiek %"),
        "kritiekschade": ("Crit damage %", "Kritiekschade %"), "aanvalssnelheid": ("Attack speed %", "Aanvalssnelheid %"),
        "loopsnelheid": ("Move speed %", "Loopsnelheid %"), "cooldownreductie": ("Cooldown %", "Cooldownreductie %"),
        "elementschade": ("Element damage %", "Elementschade %"), "elementweerstand": ("Element resist %", "Elementweerstand %"),
        "levensroof": ("Life steal %", "Levensroof %"), "healkracht": ("Heal power %", "Healkracht %"), "schade": ("Damage", "Schade")}
SOORT_Q = {"hoofd": ("Main story", "Hoofdverhaal"), "zij": ("Side quest", "Zijquest"), "klasse": ("Class trial", "Klassenproef"),
           "rijdier": ("Mount", "Rijdier"), "geheim": ("Secret", "Geheim")}
SOORT_B = {"wereldbaas": ("World boss", "Wereldbaas"), "kerker": ("Dungeon boss", "Kerkerbaas"), "arena": ("Arena boss", "Arenabaas"),
           "verhaal": ("Story boss", "Verhaalbaas"), "baas": ("Boss", "Baas")}
# W-SITE: the old "event of the day" week rhythm was made up by the site (not game data) and is gone.


GIDS_ZONE = {"rijdieren": 3}   # W-SITE: guide topics without a zN_ prefix that talk about a later zone


def znr(z):
    """W-SITE: 'zone4' -> 4 (None -> 1)."""
    try:
        return int(str(z or "zone1").replace("zone", "") or 1)
    except ValueError:
        return 1


def komma(delen):
    """W-SITE: an inline comma list whose items keep their zone (a hidden item takes its comma with it)."""
    li = []
    for b in delen:
        m = re.match(r'^<span data-zone="(\d+)">(.*)</span>$', b, flags=re.S)
        li.append(f'<li data-zone="{m.group(1)}">{m.group(2)}</li>' if m else f"<li>{b}</li>")
    return '<ul class="komma">' + "".join(li) + "</ul>"


def zl(html, zone):
    """W-SITE: wrap a link to something of zone > 1 so the spoiler switch hides it (zone 1 stays bare)."""
    n = znr(zone)
    return html if n <= 1 else f'<span data-zone="{n}">{html}</span>'


def t(en, nl):
    if en == nl:
        return ESC(nl)
    return f'<span lang="en">{ESC(en)}</span><span lang="nl">{ESC(nl)}</span>'


def th(en, nl):
    """Like t() but the arguments are already html."""
    if en == nl:
        return nl
    return f'<span lang="en">{en}</span><span lang="nl">{nl}</span>'


def p(en, nl):
    return f'<p lang="en">{en}</p><p lang="nl">{nl}</p>'


def slug(s):
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-") or "x"


def lv(r):
    if not r:
        return ""
    return f'<span class="badge lv">lv {r[0]}{"-" + str(r[1]) if r[1] != r[0] else ""}</span>'


def zbadge(zone, naam):
    n = ZONE_NR.get(zone, 1)
    return f'<span class="badge z{n}">{ESC(naam)}</span>'


def spoiler(inner, en="Spoiler: click to show", nl="Spoiler: klik om te kijken"):
    return f'<details class="spoiler"><summary>{t(en, nl)}</summary>{inner}</details>'


def elems(lijst):
    return " ".join(f'<span class="el {e}">{e}</span>' for e in lijst)


def grootico(ic):
    return f'<img class="icoon groot" src="../img/items/{ic}" alt=""> ' if ic else ""


def feiten(rijen):
    return '<dl class="feiten">' + "".join(f"<div><dt>{k}</dt><dd>{v}</dd></div>" for k, v in rijen if v not in (None, "", [])) + "</dl>"



# T-INT: t-ambacht's hiscore lists (id, English, Dutch), in the order of the mod's Ambacht enum + the total
AMBACHT_LIJSTEN = [("mijnen", "Mining", "Mijnen"), ("houthakken", "Woodcutting", "Houthakken"), ("plukken", "Picking", "Plukken"),
                   ("vissen", "Fishing", "Vissen"), ("imkeren", "Beekeeping", "Imkeren"), ("smeden", "Smithing", "Smeden"),
                   ("naaien", "Sewing", "Naaien"), ("koken", "Cooking", "Koken"), ("brouwen", "Brewing", "Brouwen"),
                   ("totaal", "Crafts total", "Ambachten totaal")]

class Bouwer:
    NAV = [("index.html", "Start", "Start"), ("kaart.html", "Map", "Kaart"), ("gids.html", "Guide", "Gids"),
           ("zones/index.html", "Zones", "Zones"), ("quests/index.html", "Quests", "Quests"), ("wezens/index.html", "Creatures", "Wezens"),
           ("bazen/index.html", "Bosses", "Bazen"), ("items/index.html", "Items", "Items"), ("systemen/index.html", "Mechanics", "Systemen"),
           ("geheimen/index.html", "Secrets", "Geheimen"), ("verhaal.html", "Lore", "Verhaal"), ("highscores.html", "Highscores", "Highscores"), ("live.html", "Live", "Live"),
           ("patchnotes/index.html", "Patch notes", "Patch notes")]

    def __init__(self, data, root, out, live_url, voorbeeld=False, wiki_url="https://guhs.nl/wiki/", site_url="https://guhs.nl/",
                 kaart_bron=None, concepten=False, versie="?", noindex=False, volledig=None, open_zones=6):
        self.d, self.root, self.out = data, root, out
        # X-SITE: --open-zones. self.d = only the open zones (what the pages list); self.alle = everything (look-ups, so a link to a
        # later thing still comes out wrapped in <span data-zone=N> and poort.strip_html removes it)
        self.alle = volledig or data
        self.open_zones = open_zones
        self.live_url, self.voorbeeld = live_url, False   # W-SITE: sample data is gone; the live parts always read live_url
        self.wiki_url, self.site_url = wiki_url, site_url
        self.kaart_bron = kaart_bron
        self.concepten = concepten
        self.versie = versie
        self.noindex = noindex   # U-SITE: a hidden preview (robots noindex + a banner)
        self.geschreven = []
        self.z = {z["id"]: z for z in self.alle["zones"]}
        self.npc = {n["id"]: n for n in self.alle["npcs"]}
        self.q = {q["id"]: q for q in self.alle["quests"]}
        self.items = {i["id"]: i for i in self.alle["items"]}
        self.wezen = {w["id"]: w for w in self.alle["wezens"]}
        self.gebied = {g["id"]: g for g in self.alle["gebieden"]}
        self.iconen = {}
        self.renders = {}   # U-SITE: render id -> file in img/renders
        self.kaart = None
        self.notities = []

    # ---- frame -------------------------------------------------------------------------------------------------------------
    def schrijf(self, path, titel_nl, titel_en, main, actief="", head="", body_attr="", script="", beschrijving="", body_klasse=""):
        r = "../" * path.count("/")
        nav = "".join(f'<a href="{r}{u}"{" aria-current=page" if u == actief else ""}>{t(en, nl)}</a>' for u, en, nl in self.NAV)
        zonesel = ('<label class="zoneslot" title="Spoilers"><span>' + t("Show up to", "Toon tot") + '</span><select id="zonetot" aria-label="Spoilers">'
                   + "".join(f'<option value="{z["nr"]}">{t("tutorial + zone 1", "tutorial + zone 1") if z["nr"] == 1 else t("zone", "zone") + " " + str(z["nr"])}</option>' for z in self.d["zones"]) + "</select></label>")   # X-SITE: option 1 names the tutorial
        tools = ('<div class="tools">' + zonesel + '<span class="seg" role="group" aria-label="Taal / Language"><button type="button" data-setlang="nl" '
                 'aria-pressed="true">NL</button><button type="button" data-setlang="en" aria-pressed="false">EN</button></span>'
                 '<button class="icon-btn" id="theme" type="button" aria-label="Thema">&#9790;</button></div>')
        header = (f'<header class="top"><div class="in"><a class="brand" href="{r}index.html"><img src="{r}img/favicon-64.png" alt="" width="32" height="32">'
                  f'<span class="word">Het Vadsrijk<small>guhs.nl</small></span></a><span class="ver" title="Guhs">v{ESC(self.versie)}</span>'
                  f'<span style="flex:1"></span>{tools}</div><nav class="vr-nav" aria-label="Vadsrijk">{nav}</nav></header>')
        footer = (f'<footer class="site"><div class="in">' + t("Het Vadsrijk: the RPG dimension of Guhs. Built from the game's own data. ",
                                                                "Het Vadsrijk: de RPG-dimensie van Guhs. Gebouwd uit de data van het spel zelf. ")
                  + f'<a href="{self.wiki_url}">{t("Guhs wiki", "Guhs-wiki")}</a> &middot; <a href="{self.site_url}">guhs.nl</a> &middot; '
                  f'<a href="{r}privacy.html">{t("Privacy", "Privacy")}</a></div></footer>')
        live = ESC(self.live_url if not self.live_url.startswith("@") else r + self.live_url[1:])
        doc = f"""<!doctype html>
<html lang="nl" data-lang="nl" data-zonetot="1">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{ESC(titel_nl)} - Het Vadsrijk</title>
<meta name="description" content="{ESC(beschrijving or titel_nl)}">
{'<meta name="robots" content="noindex, nofollow">' if self.noindex else ''}
<meta name="theme-color" content="#c2577f">
<script>{EARLY_JS}</script>
<link rel="icon" type="image/png" sizes="32x32" href="{r}img/favicon-32.png">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Fredoka:wght@500;600;700&amp;family=Nunito+Sans:ital,wght@0,400;0,700;1,400&amp;family=JetBrains+Mono:wght@500;600&amp;display=swap">
<link rel="stylesheet" href="{r}assets/site.css">
<link rel="stylesheet" href="{r}assets/vadsrijk.css">
{head}</head>
<body data-root="{r}" data-live="{live}"{(' class="' + body_klasse + '"') if body_klasse else ''}{body_attr}>
<a class="skip" href="#main">{t("Skip to content", "Naar de inhoud")}</a>
{header}
{self._voorbeeldbalk()}
<main id="main" class="vr-shell">
{main}
</main>
{footer}
<script src="{r}data/vertaal.js"></script>
<script src="{r}assets/vadsrijk.js"></script>
{script}</body>
</html>
"""
        f = os.path.join(self.out, path)
        os.makedirs(os.path.dirname(f), exist_ok=True)
        with open(f, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(doc)
        self.geschreven.append(path)

    def r(self, path):
        return "../" * path.count("/")

    def render(self, rid, van, klasse="render", alt=""):
        """U-SITE: <img> of a committed 3/4 render (docs/wiki/vadsrijk_renders), or "" when there is none."""
        f = self.renders.get(rid)
        if not f:
            return ""
        return f'<img class="{klasse}" src="{self.r(van)}img/renders/{f}" alt="{ESC(alt)}" loading="lazy">'

    def zonevak(self, z, inner, extra=""):
        """U-SITE: one zone as a closed <details> (opens per zone)."""
        return (f'<details class="zonevak" data-zone="{z["nr"]}" id="{z["id"]}" style="--zk:{z["kleur"]}"><summary><span class="zv-titel">{ESC(z["naam"])}</span> '
                f'{lv(z["lv"])} <small class="muted">{extra}</small></summary><div class="zv-in">{inner}</div></details>'
                f'<div class="verstopt-zone" data-zone="{z["nr"]}">{t("Hidden by your spoiler setting.", "Verborgen door je spoilerinstelling.")}</div>')

    def _voorbeeldbalk(self):
        """U-SITE: the preview banner (not released yet; live parts start once the server runs the update). W-SITE: no sample data any more."""
        if not self.noindex:
            return ""
        return ('<div class="voorbeeldbalk" role="note">' + t("Preview of Guhs 1.10.0 \"Het Vadsrijk\": not released yet. The live parts (server, highscores, players on the map) start once the server runs the update.",
                                                             "Voorbeeld van Guhs 1.10.0 \"Het Vadsrijk\": nog niet uit. De live-delen (server, highscores, spelers op de kaart) starten zodra de server de update draait.") + "</div>")

    # ---- links -------------------------------------------------------------------------------------------------------------
    def zone_naam(self, zid):
        z = self.z.get(zid)
        return z["naam"] if z else zid

    def tekst_zone(self, *teksten):
        """W-SITE: the highest zone whose own names (zones, areas, bosses, creatures, items, NPCs, quests) appear in the text, else 1.
        Used to tag rows of free-text tables (recipes, wardrobe, mounts, pets) so the spoiler switch hides them."""
        if not hasattr(self, "_tz"):   # X-SITE: the names of poort.zone_namen (also without "De"/"Het", whole words)
            from . import poort
            namen = poort.zone_namen(self.alle)
            self._tz = (namen, poort.patroon(namen))
        if not hasattr(self, "_tz"):
            z1, hoger = set(), {}
            for k, veld in (("zones", "naam"), ("gebieden", "naam"), ("bazen", "naam"), ("wezens", "naam"), ("items", "naam"), ("npcs", "naam"), ("quests", "titel")):
                for x in self.alle.get(k, []):   # X-SITE: all zones, also when only some are published
                    n = (x.get(veld) or "").strip()
                    if len(n) < 5:
                        continue
                    zn = x["nr"] if k == "zones" else znr(x.get("zone"))
                    if zn <= 1:
                        z1.add(n)
                    else:
                        hoger[n] = max(hoger.get(n, 0), zn)
            namen = {n: z for n, z in hoger.items() if n not in z1 and not any(n in a for a in z1)}
            self._tz = (namen, re.compile("|".join(re.escape(n) for n in sorted(namen, key=len, reverse=True))) if namen else None)
        namen, pat = self._tz
        if not pat:
            return 1
        return max([namen[m] for tx in teksten for m in pat.findall(tx or "")] or [1])

    def l_npc(self, nid, van):
        n = self.npc.get(nid)
        if not n:
            return ESC(nid)
        return zl(f'<a href="{self.r(van)}npcs/{slug(nid)}.html">{ESC(n["naam"])}</a>', n.get("zone"))

    def l_quest(self, qid, van):
        q = self.q.get(qid)
        if not q:
            return ESC(qid)
        return zl(f'<a href="{self.r(van)}quests/{slug(qid)}.html">{ESC(q["titel"])}</a>', q.get("zone"))

    def l_item(self, iid, van, icoon=True):
        i = self.items.get(iid)
        if not i:
            return ESC(iid)
        ic = self.icoon(iid, van) if icoon else ""
        return zl(f'<a href="{self.r(van)}items/{slug(iid)}.html">{ic}{ESC(i["naam"])}</a>', i.get("zone"))

    def l_wezen(self, wid, van):
        w = self.wezen.get(wid)
        if not w:
            return ESC(wid)
        return zl(f'<a href="{self.r(van)}wezens/{slug(w["kort"])}.html">{ESC(w["naam"])}</a>', w.get("zone"))

    def l_gebied(self, gid, van):
        g = self.gebied.get(gid)
        naam = g["naam"] if g else gid
        return zl(f'<a href="{self.r(van)}kaart.html?plek={ESC(gid)}">{ESC(naam)}</a>', g.get("zone") if g else None)

    def l_kaart(self, pos, van, label=None):
        if not pos:
            return ""
        x, z = pos[0], pos[2]
        return f'<a href="{self.r(van)}kaart.html?x={x}&amp;z={z}">{ESC(label) if label else ""}<span class="tagref">x {x}, y {pos[1]}, z {z}</span></a>'

    def icoon(self, iid, van):
        f = self.iconen.get(iid)
        if not f:
            return ""
        return f'<img class="icoon" src="{self.r(van)}img/items/{f}" alt="" loading="lazy"> '

    # ---- assets ------------------------------------------------------------------------------------------------------------
    def assets(self):
        a = os.path.join(self.out, "assets")
        os.makedirs(a, exist_ok=True)
        import sys
        sys.path.insert(0, os.path.join(self.root, "tools"))
        from wiki_site import assets as wa   # the wiki's own stylesheet: the same look
        global EARLY_JS
        EARLY_JS = wa.EARLY_JS
        with open(os.path.join(a, "site.css"), "w", encoding="utf-8", newline="\n") as f:
            f.write(wa.CSS.strip() + "\n")
        with open(os.path.join(a, "vadsrijk.css"), "w", encoding="utf-8", newline="\n") as f:
            f.write(stijl.CSS.strip() + "\n")
        with open(os.path.join(a, "vadsrijk.js"), "w", encoding="utf-8", newline="\n") as f:
            f.write(stijl.JS.strip() + "\n")
        with open(os.path.join(a, "kaart.js"), "w", encoding="utf-8", newline="\n") as f:
            f.write(stijl.KAART_JS.strip() + "\n")
        img = os.path.join(self.out, "img")
        os.makedirs(os.path.join(img, "items"), exist_ok=True)
        tex = os.path.join(self.root, "src", "main", "resources", "assets", "guhs", "textures")
        for bron, doel in (("ftbquests/guhs_vadsrijk/title.png", "titel.png"), ("ftbquests/guhs_vadsrijk/welkom.png", "welkom.png"),
                           ("ftbquests/guhs_vadsrijk/kop_vr_zwamzoet.png", "kop_zone1.png"), ("ftbquests/guhs_vadsrijk/kop_vr_kamp.png", "kop_kamp.png"),
                           ("ftbquests/guhs_vadsrijk/kop_vr_z2_kandijkom.png", "kop_zone2.png"), ("ftbquests/guhs_vadsrijk/kop_vr_z3_honingtak.png", "kop_zone3.png"),
                           ("ftbquests/guhs_vadsrijk/kop_vr_z4_kust.png", "kop_zone4.png"), ("ftbquests/guhs_vadsrijk/kop_vr_z5_stad.png", "kop_zone5.png"),
                           ("ftbquests/guhs_vadsrijk/kop_vr_verhaal.png", "kop_verhaal.png"), ("ftbquests/guhs_vadsrijk/kop_vr_klassen.png", "kop_klassen.png"),
                           ("gui/vadsrijk/reiskaart.png", "reiskaart.png")):
            s = os.path.join(tex, bron)
            if os.path.exists(s):
                _webp(s, os.path.join(img, doel.replace(".png", ".webp")))
        # favicon: the guh face coin / the wiki's own guh picture
        from PIL import Image
        # U-SITE round 3: zone 6 has no FTB header yet -> a 1024x160 band cut from the story concept picture (Rozenkloof)
        kop6, bron6 = os.path.join(img, "kop_zone6.webp"), os.path.join(self.root, "docs", "wiki", "vadsrijk_verhaal", "zijdeland.webp")
        if not os.path.exists(kop6) and os.path.exists(bron6):
            im = Image.open(bron6).convert("RGB")
            w = 1024
            im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
            top = max(0, (im.height - 160) // 2)
            im.crop((0, top, w, top + 160)).save(kop6, "WEBP", quality=82)
        fav = None
        for c in (os.path.join(tex, "item", "vadsrijk", "economie", "vadsmunt_goud.png"), os.path.join(self.root, "docs", "wiki", "img", "icon_picked_up_guh.png"),
                  os.path.join(self.root, "docs", "wiki", "img", "guh.png")):
            if os.path.exists(c):
                fav = c
                break
        if fav is None:
            for d, _, fs in os.walk(os.path.join(tex, "item", "vadsrijk", "economie")):
                for x in fs:
                    if "munt" in x:
                        fav = os.path.join(d, x)
                        break
        if fav:
            im = Image.open(fav).convert("RGBA")
            bb = im.getbbox()
            if bb:
                im = im.crop(bb)
            side = max(im.size)
            sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
            sq.paste(im, ((side - im.width) // 2, (side - im.height) // 2))
            rs = Image.NEAREST if side <= 64 else Image.LANCZOS
            sq.resize((32, 32), rs).save(os.path.join(img, "favicon-32.png"))
            sq.resize((64, 64), rs).save(os.path.join(img, "favicon-64.png"))
        # item icons: assets/guhs/models/item/<id>.json -> layer0
        for i in self.d["items"]:
            iid = (i.get("item") or "")
            kandidaten = [iid.split(":")[-1]] if iid else []
            kandidaten += ["vr_" + i["id"], "vr_baas_" + i["id"].replace("baas_", ""), "vr_mob_" + i["id"], "vr_z2_" + i["id"],
                           "vr_z2_mob_" + i["id"], "vr_z3_" + i["id"], "vr_z3_mob_" + i["id"], "vr_kl_" + i["id"], "vr_item_" + i["id"],
                           "vr_z2_" + i["id"].replace("z2baas_", "baas_"), "vr_z3_" + i["id"].replace("z3baas_", "baas_"),
                           "vr_z4_" + i["id"], "vr_z4_mob_" + i["id"], "vr_z4_" + i["id"].replace("z4baas_", "baas_"),
                           "vr_z5_" + i["id"], "vr_z5_mob_" + i["id"], "vr_z5_" + i["id"].replace("z5baas_", "baas_"),
                           "vr_z5_" + i["id"].replace("z5droom_", "droom_"), "vr_" + i["id"].replace("mw_", "mw_"),
                           "vr_z6_" + i["id"], "vr_z6_mob_" + i["id"], "vr_z6_" + i["id"].replace("z6baas_", "baas_"), "vr_" + i["id"]]   # U-SITE: zone 6 + raid
            for k in kandidaten:
                png = self._item_textuur(k)
                if png:
                    naam = slug(i["id"]) + ".png"
                    shutil.copyfile(png, os.path.join(img, "items", naam))
                    self.iconen[i["id"]] = naam
                    break
        rb = os.path.join(self.root, "docs", "wiki", "vadsrijk_renders")   # U-SITE: committed creature/boss renders
        if os.path.isdir(rb):
            os.makedirs(os.path.join(img, "renders"), exist_ok=True)
            for f in os.listdir(rb):
                if f.endswith(".webp"):
                    shutil.copyfile(os.path.join(rb, f), os.path.join(img, "renders", f))
                    self.renders[f[:-5]] = f
        else:
            self.notities.append("no renders (docs/wiki/vadsrijk_renders): run python tools/wiki_site/vadsrijk/renders.py")
        if len(self.iconen) < len(self.d["items"]) * 0.6:
            self.notities.append(f"item icons: {len(self.iconen)}/{len(self.d['items'])} found")

    def _item_textuur(self, iid):
        res = os.path.join(self.root, "src", "main", "resources", "assets", "guhs")
        m = os.path.join(res, "models", "item", iid + ".json")
        if not os.path.exists(m):
            return None
        try:
            d = json.load(open(m, encoding="utf-8"))
        except ValueError:
            return None
        for _ in range(3):
            tx = (d.get("textures") or {}).get("layer0")
            if tx:
                ns, _, pth = tx.partition(":")
                if not pth:
                    ns, pth = "minecraft", ns
                if ns == "guhs":
                    f = os.path.join(res, "textures", pth + ".png")
                    return f if os.path.exists(f) else None
                # W-SITE: items that reuse a vanilla texture -> the committed copy in docs/wiki/vadsrijk_vanilla (vanilla_iconen.py)
                f = os.path.join(self.root, "docs", "wiki", "vadsrijk_vanilla", pth.replace("/", "_") + ".png")
                return f if os.path.exists(f) else None
            par = d.get("parent", "")
            if par.startswith("guhs:item/"):
                m = os.path.join(res, "models", "item", par.split("/", 1)[1] + ".json")
                if os.path.exists(m):
                    d = json.load(open(m, encoding="utf-8"))
                    continue
            return None
        return None

    def data_js(self):
        """data/kaart.js (the map) + data/vertaal.js (lang keys the live JSON uses) + data/vadsrijk.json (everything, for tools)."""
        d = self.d
        dmap = os.path.join(self.out, "data")
        os.makedirs(dmap, exist_ok=True)
        zones = []
        for z in d["zones"]:
            gs = [g for g in d["gebieden"] if g["zone"] == z["id"] and not g["id"].endswith("ring")]
            mid = None
            ring = self.gebied.get(z["ring"]) or self.gebied.get(z["hub"])
            if ring:
                b = ring["doos"]
                mid = [(b[0] + b[2]) / 2, b[1] - 90]
            zones.append(dict(id=z["id"], nr=z["nr"], naam=z["naam"], lv=z["lv"], mid=mid))
        steden = []
        for g in d["gebieden"]:
            if g["stad"] and not g["id"].endswith("_dok"):
                b, lg = g["doos"], g.get("laag") or {}
                c = [lg["cx"], 0, lg["cz"]] if lg.get("r_max") else [(b[0] + b[2]) // 2, 0, (b[1] + b[3]) // 2]
                steden.append(dict(id=g["id"], naam=g["naam"], zone=g["zone"], pos=c, url=f"zones/{g['zone']}.html"))
        bazen = [dict(id=b["id"], naam=b["naam"], zone=b["zone"], soort=b["soort"], pos=b["arena"]["pos"], respawn_s=b["arena"]["respawn_s"],
                      arena=b["arena"]["id"], url=f"bazen/{slug(b['id'])}.html") for b in d["bazen"] if b.get("arena") and not b.get("spoiler")]
        qg = {}
        for q in d["quests"]:
            if q["soort"] == "geheim" or not q.get("gever_pos"):
                continue
            n = self.npc.get(q["gever"])
            e = qg.setdefault(q["gever"], dict(id=q["gever"], naam=n["naam"] if n else q["gever"], pos=q["gever_pos"], zone=q["zone"], hoofd=False, quests=[]))
            e["hoofd"] = e["hoofd"] or q["soort"] == "hoofd"
            e["quests"].append(dict(titel=q["titel"], url=f"quests/{slug(q['id'])}.html"))
        routes = d["routes"]   # U-SITE: bron.py gives each stop's zone (za/zb)
        ringen = {z["ring"] for z in d["zones"]}
        kaart = dict(kaart=self.kaart or dict(x0=-700, z0=-3100, x1=3000, z1=1200), gebieden=[dict(g, laag=g.get("laag"), ring=g["id"] in ringen) for g in d["gebieden"]],
                     zones=zones, zonekleur={z["id"]: z["kleur"] for z in d["zones"]}, steden=steden, punten=d["punten"], bazen=bazen,
                     questgevers=list(qg.values()), routes=routes, geheimen=d["geheimen"],
                     # U-SITE round 3: the secrets layer (off by default, not searchable)
                     geheimpunten=[dict(id=g["id"].replace(":", "-"), soort=g["soort"], naam=g["naam"], pos=g["pos"], zone=g["zone"], beloning=g["beloning"])
                                   for g in d.get("geheimen_lijst", [])])
        with open(os.path.join(dmap, "kaart.js"), "w", encoding="utf-8", newline="\n") as f:
            f.write("window.VR_KAART=" + json.dumps(kaart, ensure_ascii=False, separators=(",", ":")) + ";\n")
        vt = dict(d["vertaal"])
        for z in d["zones"]:
            vt["zone:" + z["id"]] = z["naam"]
        for s, (en, nl) in SLOT.items():
            vt["slot:" + s] = nl
        for k in ("kamp", "haven", "zwamzoet", "weide", "woud", "plein", "markt"):
            vt.setdefault("stop:" + k, k.capitalize())
        with open(os.path.join(dmap, "vertaal.js"), "w", encoding="utf-8", newline="\n") as f:
            f.write("window.VR_VERTAAL=" + json.dumps(vt, ensure_ascii=False, separators=(",", ":")) + ";\n")
        with open(os.path.join(dmap, "vadsrijk.json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({k: v for k, v in d.items() if k != "vertaal"}, f, ensure_ascii=False, indent=1)

    # ---- pages -------------------------------------------------------------------------------------------------------------
    def alles(self):
        self.assets()
        self.data_js()
        self.index()
        self.kaartpagina()
        self.gids()
        self.zones()
        self.quests()
        self.npcs()
        self.wezens()
        self.bazen()
        from . import raid   # U-SITE round 3: De Sterrendovende Toren
        raid.paginas(self)
        from . import geheimen
        geheimen.pagina(self)
        self.items_()
        self.systemen()
        self.verhaal()
        self.highscores()
        self.speler()
        self.live()
        self.patchnotes()
        self.privacy()
        self.notfound()
        return self.geschreven

    def index(self):
        P = "index.html"
        zk = "".join(
            f'<a class="kaartje zonekaart" data-zone="{z["nr"]}" style="--zk:{z["kleur"]}" href="zones/{z["id"]}.html">'
            f'<img src="img/kop_{z["id"]}.webp" alt="" style="width:100%;border-radius:10px;margin-bottom:8px" loading="lazy">'
            f'<h3>{ESC(z["naam"])}</h3><p>{lv(z["lv"])} {t("Zone", "Zone")} {z["nr"]} &middot; {ESC(self.gebied.get(z["hub"], {}).get("naam", ""))}</p></a>'
            for z in self.d["zones"]) + "".join(
            f'<div class="verstopt-zone" data-zone="{z["nr"]}">{t("Zone " + str(z["nr"]) + " is hidden by your spoiler setting.", "Zone " + str(z["nr"]) + " is verborgen door je spoilerinstelling.")}</div>'
            for z in self.d["zones"] if z["nr"] > 1)
        if self.open_zones < 6:   # X-SITE: one card for every zone that is not published yet
            from .poort import teaser
            zk += teaser(t)
        snel = [("gids.html", "New here? The guide", "Nieuw? De gids", "Your first hour, step by step.", "Je eerste uur, stap voor stap."),
                ("kaart.html", "The world map", "De wereldkaart", "Zones, levels, campfires, bosses, airships.", "Zones, levels, kampvuren, bazen, luchtschepen."),
                ("quests/index.html", "Quests", "Quests", "Every quest with a walkthrough (spoiler button).", "Elke quest met een walkthrough (spoilerknop)."),
                ("items/index.html", "Item database", "Itemdatabase", "Stats, sets and where it drops.", "Stats, sets en waar het valt."),
                ("highscores.html", "Highscores", "Highscores", "Who is the most vahoeg?", "Wie is het meest vahoeg?"),
                ("live.html", "Live", "Live", "Boss timers, the caravan, the week seal.", "Baastimers, de karavaan, het weekzegel.")]
        snelh = "".join(f'<a class="kaartje" href="{u}"><h3>{t(a, b)}</h3><p>{t(c, d)}</p></a>' for u, a, b, c, d in snel)
        n_q = len([q for q in self.d["quests"] if q["soort"] != "geheim"])
        main = f"""
<section class="vr-hero">
<img class="kop" src="img/titel.webp" alt="Het Vadsrijk" width="1024" height="256">
<h1>{t("The Vadsrijk", "Het Vadsrijk")}</h1>
{p("The RPG dimension of Guhs: six zones, three classes, a raid, coloured loot, bosses with real mechanics, an airship and a whole lot of grumpy creatures that only need a cuddle. Everything below comes straight from the game.",
   "De RPG-dimensie van Guhs: zes zones, drie klassen, een raid, buit met kleurtjes, bazen met echte mechanics, een luchtschip en heel veel chagrijnige wezens die alleen een knuffel nodig hebben. Alles hieronder komt rechtstreeks uit het spel.") if self.open_zones >= 6 else
  p("The RPG dimension of Guhs: three classes, coloured loot, bosses with real mechanics and a whole lot of grumpy creatures that only need a cuddle. Everything below comes straight from the game.",
    "De RPG-dimensie van Guhs: drie klassen, buit met kleurtjes, bazen met echte mechanics en heel veel chagrijnige wezens die alleen een knuffel nodig hebben. Alles hieronder komt rechtstreeks uit het spel.")}
<div class="knoppen"><a class="knop" href="gids.html">{t("Start here", "Begin hier")}</a><a class="knop licht" href="kaart.html">{t("World map", "Wereldkaart")}</a><a class="knop licht" href="https://guhs.nl/">{t("Play on the server", "Speel op de server")}</a></div>
</section>
<h2>{t("Right now", "Nu")}</h2>
{self._status_tegels()}
<h2>{t("The zones", "De zones")}</h2>
<div class="grid">{zk}</div>
<h2>{t("Find your way", "Zoek je weg")}</h2>
<div class="grid">{snelh}</div>
<p class="muted">{t(f"{n_q} quests, {len(self.d['wezens'])} creatures, {len(self.d['bazen'])} bosses, {len(self.d['items'])} items, {len(self.d['npcs'])} characters.",
                    f"{n_q} quests, {len(self.d['wezens'])} wezens, {len(self.d['bazen'])} bazen, {len(self.d['items'])} items, {len(self.d['npcs'])} personages.")}</p>
"""
        self.schrijf(P, "Het Vadsrijk", "The Vadsrijk", main, "index.html", beschrijving="Het Vadsrijk: wiki, wereldkaart, highscores en live info van de RPG-dimensie van Guhs.")

    def _status_tegels(self):
        return (f'<div class="live-rij" data-live-status>'
                f'<div class="live-tegel"><div class="k">{t("Server", "Server")}</div><div class="v" data-k="status">...</div><div class="s">play.guhs.nl</div></div>'
                f'<div class="live-tegel"><div class="k">{t("Players online", "Spelers online")}</div><div class="v" data-k="spelers">-</div><div class="s" data-k="namen"></div></div>'
                f'<div class="live-tegel"><div class="k">{t("In the Vadsrijk", "In het Vadsrijk")}</div><div class="v" data-k="rijk">-</div><div class="s">{t("adventurers", "avonturiers")}</div></div>'
                f'<div class="live-tegel"><div class="k">Guhs</div><div class="v" data-k="versie">-</div><div class="s"><a href="https://pack.guhs.nl/">pack.guhs.nl</a></div></div></div>')

    def kaartpagina(self):
        head = f'<link rel="stylesheet" href="{stijl.LEAFLET_CSS}">'
        main = f"""<h1 class="sr">{t("World map", "Wereldkaart")}</h1>
<div style="position:relative"><div id="kaart" role="application" aria-label="Wereldkaart / World map"></div>
<div class="kaart-paneel"><div class="kaart-zoek"><input id="kzoek" type="search" autocomplete="off" placeholder="Zoek een plek, baas of NPC..." aria-label="Zoeken / Search"><div class="res" id="kres"></div></div>
<span class="kaart-info" id="coords">x - z -</span><span class="kaart-info spelers" id="spelersnoot">...</span></div></div>
<div class="vr-shell"><p class="muted">{t("A top-down of the real map tiles. Secrets are in the layer Show secrets (spoilers!) (off by default; every secret with its spot is on the Secrets page, behind a click). Click a marker for details; every place has its own link (e.g. kaart.html?plek=zwamzoet).",
  "Een bovenaanzicht van de echte kaarttegels. Geheimen staan in de laag Geheimen tonen (spoilers!) (standaard uit; elk geheim met zijn plek staat op de pagina Geheimen, achter een klik). Klik op een markering voor details; elke plek heeft een eigen link (bijv. kaart.html?plek=zwamzoet).")}</p></div>"""
        script = f'<script src="{stijl.LEAFLET_JS}"></script><script src="data/kaart.js"></script><script src="assets/kaart.js"></script>'
        self.schrijf("kaart.html", "Wereldkaart", "World map", main, "kaart.html", head=head, script=script, body_klasse="kaart-pagina",
                     beschrijving="De interactieve wereldkaart van Het Vadsrijk.")

    def gids(self):
        P = "gids.html"
        stappen = [
            ("Find the portal temple in the Guhmension. Werver Wimpel waits there: talk to him first, then the old portal wakes up.",
             "Zoek de portaaltempel in de Guhmensie. Werver Wimpel wacht daar: praat eerst met hem, dan wordt het oude portaal wakker."),
            ("Set your render distance to 32 chunks (at least 20): Options > Video Settings. The Vadsrijk is hand-built, so it can go much higher than at home.",
             "Zet je weergaveafstand op 32 chunks (minstens 20): Opties > Video-instellingen. Het Vadsrijk is met de hand gebouwd, dus het kan veel hoger dan thuis."),
            ("Your own things stay home safely. Inside you get an adventurer's inventory.", "Je eigen spullen blijven veilig thuis. Binnen krijg je een avonturiersinventaris."),
            ("You arrive on Knuffelkamp (lv 1-3), the training island. Sergeant Pluizebol sends you to the campfire. Follow the golden ! (the main story).",
             "Je komt aan op Knuffelkamp (lv 1-3), het oefeneiland. Sergeant Pluizebol stuurt je naar het kampvuur. Volg de gouden ! (het hoofdverhaal)."),
            ("Choose your class at Zuster Zachtjes: Knuffelridder (close, sturdy), Pluisjager (from afar, nimble) or Vadsmagier (elements and healing). You can switch later.",
             "Kies je klasse bij Zuster Zachtjes: Knuffelridder (dichtbij, stevig), Pluisjager (van ver, beweeglijk) of Vadsmagiër (elementen en heling). Wisselen kan later."),
            ("Right-click every campfire: it becomes your checkpoint. Fainted? You wake up there; you lose nothing.",
             "Rechtsklik elk kampvuur: dan is het je checkpoint. Flauwgevallen? Je wordt daar wakker; je verliest niks."),
            ("Stuck in a hole or in the water? Press U (Losknuffelen).", "Zit je vast in een kuil of in het water? Druk op U (Losknuffelen)."),
            ("Finish the camp's story and the airship takes you to Zwamzoet. From there the whole Vadsrijk opens up.",
             "Maak het verhaal van het kamp af en het luchtschip brengt je naar Zwamzoet. Vanaf daar gaat het hele Vadsrijk open."),
        ]
        stappen_h = '<ol class="stappen">' + "".join(f"<li>{t(en, nl)}</li>" for en, nl in stappen) + "</ol>"
        toetsen = [("J", "Quest log", "Questlog"), ("B", "Exploration book", "Verkenningsboek"), ("R / V / G", "Skills 1-3", "Vaardigheden 1-3"),
                   ("Z", "Ultimate skill", "Ultieme vaardigheid"), ("C", "Knuffelsprong (dodge jump)", "Knuffelsprong"), ("U", "Unstuck (Losknuffelen)", "Losknuffelen"),
                   ("X", "Mount: get on / off (on zone 4's water: your sailboat)", "Rijdier: op- en afstappen (op het water van zone 4: je zeilbootje)"),
                   ("N", "Mounts screen (dress up)", "Rijdierscherm (aankleden)"), ("P", "Pets", "Huisdieren"), ("K", "Skill tree", "Skillboom"),
                   ("Y", "Crafts book", "Ambachtenboek"), ("I", "Knapsack", "Knapzak"), ("O", "Wardrobe", "Kledingkast"),
                   ("Alt+1/2/3", "Potion belt", "Drankjesriem")]
        toets_h = '<div class="tscroll"><table class="data"><thead><tr><th>' + t("Key", "Toets") + "</th><th>" + t("What", "Wat") + "</th></tr></thead><tbody>" + "".join(
            f"<tr><td><kbd>{ESC(k)}</kbd></td><td>{t(en, nl)}</td></tr>" for k, en, nl in toetsen) + "</tbody></table></div>"
        rijen = []
        for g in sorted(self.d["gebieden"], key=lambda g: (g["lv"][0], g["lv"][1])):
            if g["id"].endswith("ring") or g["id"].endswith("_dok") or g["id"] in {z["ring"] for z in self.d["zones"]}:
                continue
            n = ZONE_NR[g["zone"]]
            rijen.append(f'<tr data-zone="{n}"><td class="num" data-s="{g["lv"][0]}">{g["lv"][0]}-{g["lv"][1]}</td><td>{zbadge(g["zone"], self.zone_naam(g["zone"]))}</td>'
                         f'<td>{self.l_gebied(g["id"], P)}{" &#9873;" if g["stad"] else ""}</td>'
                         f'<td>{t("town: shops, quests, safe", "stad: winkels, quests, veilig") if g["stad"] else t("adventure", "avontuur")}</td></tr>')
        waar = ('<div class="tscroll"><table class="data sort"><thead><tr><th class="num">' + t("Level", "Level") + "</th><th>Zone</th><th>"
                + t("Area", "Gebied") + "</th><th>" + t("What", "Wat") + "</th></tr></thead><tbody>" + "".join(rijen) + "</tbody></table></div>")
        topics = "".join(f'<div class="kaartje" data-zone="{int(g["id"][1]) if re.match(r"z[2-6]_", g["id"]) else GIDS_ZONE.get(g["id"], 1)}"><h3>{ESC(g["titel"])}</h3><p>{ESC(g["tekst"])}</p></div>'
                         for g in self.d["gids"])
        elem = (f'<p>{t("Fire beats Mushroom beats Slime beats Fire: x1.5 on a weakness, x0.7 the other way round. Pink (vads) is extra strong against everything that lost its vads.", "Vuur wint van Zwam, Zwam van Slijm, Slijm van Vuur: x1,5 op een zwakte en x0,7 andersom. Roze (vads) is extra sterk tegen alles wat ontvadst is.")}</p>')
        main = f"""<h1>{t("Guide for new adventurers", "Gids voor nieuwe avonturiers")}</h1>
<p class="lead">{t("Your first hour in the Vadsrijk, the keys, where to go at which level, and everything the in-game guide (J > Gids) tells you.", "Je eerste uur in het Vadsrijk, de toetsen, waar je op welk level heen gaat, en alles wat de gids in het spel (J > Gids) je vertelt.")}</p>
<div class="tw"><div>
<h2 id="eerste-uur">{t("Your first hour", "Je eerste uur")}</h2>{stappen_h}
<h2 id="waarheen">{t("Where do I go now?", "Waar moet ik nu heen?")}</h2>{waar}
<h2 id="elementen">{t("The element triangle", "De elementendriehoek")}</h2>{elem}
<h2 id="gids">{t("The in-game guide", "De gids uit het spel")}</h2><div class="grid">{topics}</div>
</div><aside class="zij"><div class="kaartje"><h3>{t("Keys", "Toetsen")}</h3>{toets_h}</div>
<div class="kaartje"><h3>{t("Play", "Spelen")}</h3><p><a href="https://guhs.nl/">{t("The official server", "De officiële server")}</a><br><a href="https://pack.guhs.nl/">pack.guhs.nl</a> {t("(the modpack)", "(het modpack)")}</p></div></aside></div>"""
        self.schrijf(P, "Gids", "Guide", main, P, beschrijving="Gids voor nieuwe spelers in Het Vadsrijk.")

    # zones
    def zones(self):
        kaarten = "".join(f'<a class="kaartje zonekaart" data-zone="{z["nr"]}" style="--zk:{z["kleur"]}" href="{z["id"]}.html"><img src="../img/kop_{z["id"]}.webp" alt="" style="width:100%;border-radius:10px" loading="lazy">'
                          f'<h3>{ESC(z["naam"])}</h3><p>{lv(z["lv"])}</p></a>' for z in self.d["zones"])
        if self.open_zones < 6:   # X-SITE
            from .poort import teaser
            kaarten += teaser(t)
        self.schrijf("zones/index.html", "Zones", "Zones", f'<h1>{t("Zones", "Zones")}</h1><div class="grid">{kaarten}</div>', "zones/index.html")
        for z in self.d["zones"]:
            P = f"zones/{z['id']}.html"
            n = z["nr"]
            gs = [self.gebied[g] for g in z["gebieden"] if g in self.gebied]
            gr = "".join(f'<tr><td>{self.l_gebied(g["id"], P)}{" &#9873;" if g["stad"] else ""}</td><td class="num" data-s="{g["lv"][0]}">{g["lv"][0]}-{g["lv"][1]}</td>'
                         f'<td class="num">{self.d["geheimen"].get(g["id"], 0) or ""}</td>'
                         f'<td class="num">{sum(1 for x in self.d["punten"] if x["gebied"] == g["id"] and x["soort"] == "kampvuur") or ""}</td></tr>'
                         for g in sorted(gs, key=lambda g: g["lv"][0]))
            gtab = ('<div class="tscroll"><table class="data sort"><thead><tr><th>' + t("Area", "Gebied") + '</th><th class="num">Level</th><th class="num">'
                    + t("Secrets", "Geheimen") + '</th><th class="num">' + t("Campfires", "Kampvuren") + "</th></tr></thead><tbody>" + gr + "</tbody></table></div>")
            bz = "".join(self._baas_kaartje(b, P) for b in self.d["bazen"] if b["zone"] == z["id"])
            if z["id"] == "zone6":   # U-SITE round 3
                from . import raid
                bz += raid.kaartje(self, P)
            wz = ", ".join(self.l_wezen(w["id"], P) for w in self.d["wezens"] if w["zone"] == z["id"] and not w["critter"])
            hq = [q for q in self.d["quests"] if q["zone"] == z["id"] and q["soort"] == "hoofd"]
            zq = [q for q in self.d["quests"] if q["zone"] == z["id"] and q["soort"] in ("zij", "klasse", "rijdier")]
            ql = '<ol>' + "".join(f"<li>{self.l_quest(q['id'], P)} {lv([q['lv'], q['lv']])}</li>" for q in hq) + "</ol>"
            zl = '<ul>' + "".join(f"<li>{self.l_quest(q['id'], P)} <small class='muted'>{t(*SOORT_Q[q['soort']])}</small></li>" for q in zq) + "</ul>"
            sets = [s for s in self.d["sets"] if any(self.items.get(x, {}).get("zone") == z["id"] for x in s["stukken"])]
            sl = "".join(f'<li><a href="../systemen/sets.html#{s["id"]}">{ESC(s["naam"])}</a></li>' for s in sets)
            lore = "".join(f'<h3>{ESC(l["titel"])}</h3><p>{ESC(l["tekst"])}</p>' + "".join(f"<h4>{ESC(k)}</h4><p>{ESC(v)}</p>" for k, v in l["stukken"])
                           for l in self.d["lore"] if l["zone"] == z["id"])
            geheim = sum(v for k, v in self.d["geheimen"].items() if (self.gebied.get(k, {}).get("zone") or k) == z["id"])
            info = feiten([(t("Levels", "Levels"), lv(z["lv"])), (t("Town", "Stad"), self.l_gebied(z["hub"], P)),
                           (t("Quests", "Quests"), str(len(hq) + len(zq))), (t("Secrets", "Geheimen"), str(geheim)),
                           (t("Campfires", "Kampvuren"), str(sum(1 for x in self.d["punten"] if x["zone"] == z["id"] and x["soort"] == "kampvuur")))])
            main = f"""<div data-zone="{n}"><img src="../img/kop_{z['id']}.webp" alt="" style="width:100%;border-radius:16px" width="1024" height="160">
<h1>{ESC(z['naam'])} <small class="muted">{t('zone', 'zone')} {n}</small></h1>
<div class="tw"><div>{''.join(f'<p class="lead">{ESC(l["tekst"])}</p>' for l in self.d["lore"] if l["zone"] == z["id"])[:1200] if False else ''}
<h2>{t('Areas', 'Gebieden')}</h2>{gtab}
<h2>{t('Bosses', 'Bazen')}</h2><div class="grid">{bz}</div>
<h2>{t('Creatures', 'Wezens')}</h2><p>{wz or '<span class="leeg">-</span>'}</p>
<h2>{t('Main story', 'Hoofdverhaal')}</h2>{ql}
<h2>{t('Side quests', 'Zijquests')}</h2>{zl}
{('<h2>' + t('Sets', 'Sets') + '</h2><ul>' + sl + '</ul>') if sl else ''}
{('<h2>' + t('About this zone', 'Over deze zone') + '</h2>' + lore) if lore else ''}
</div><aside class="zij"><div class="kaartje">{info}<p><a class="knop" href="../kaart.html?plek={z['hub']}">{t('On the map', 'Op de kaart')}</a></p></div></aside></div></div>
<div class="verstopt-zone" data-zone="{n}">{t('This zone is hidden by your spoiler setting (top right).', 'Deze zone is verborgen door je spoilerinstelling (rechtsboven).')}</div>"""
            self.schrijf(P, z["naam"], z["naam_en"], main, "zones/index.html", beschrijving=f"{z['naam']}: zone {n} van Het Vadsrijk (lv {z['lv'][0]}-{z['lv'][1]}).")

    def _baas_kaartje(self, b, P):
        n = ZONE_NR[b["zone"]]
        img = self.render("baas_" + b["id"], P, "render klein")
        inner = (f'<a class="baasfoto" href="{self.r(P)}bazen/{slug(b["id"])}.html">{img}</a>' if img else "") + f'<h3><a href="{self.r(P)}bazen/{slug(b["id"])}.html">{ESC(b["naam"])}</a></h3><p><span class="badge">{t(*SOORT_B.get(b["soort"], SOORT_B["baas"]))}</span></p>'
        if b.get("spoiler"):
            inner = spoiler(inner, "Story boss: click to show", "Verhaalbaas: klik om te kijken")
        return f'<div class="kaartje" data-zone="{n}">{inner}</div>'

    # quests
    def quests(self):
        P = "quests/index.html"
        delen = []
        for z in self.d["zones"]:
            n = z["nr"]
            rijen = []
            for soort in ("hoofd", "zij", "klasse", "rijdier", "geheim"):
                qs = [q for q in self.d["quests"] if q["zone"] == z["id"] and q["soort"] == soort]
                if not qs:
                    continue
                lijst = "".join(f'<tr><td class="num">{q["lv"]}</td><td>{self.l_quest(q["id"], P)}</td><td>{self.l_npc(q["gever"], P)}</td>'
                                f'<td class="num">{q["beloning"]["xp"]}</td></tr>' for q in qs)
                tab = ('<div class="tscroll"><table class="data"><thead><tr><th class="num">Lv</th><th>Quest</th><th>' + t("Given by", "Gever")
                       + '</th><th class="num">XP</th></tr></thead><tbody>' + lijst + "</tbody></table></div>")
                kop = f"<h3>{t(*SOORT_Q[soort])} ({len(qs)})</h3>"
                rijen.append(kop + (spoiler(tab, "Secret quests: click to show", "Geheime quests: klik om te kijken") if soort == "geheim" else tab))
            nq = len([q for q in self.d["quests"] if q["zone"] == z["id"] and q["soort"] != "geheim"])
            delen.append(self.zonevak(z, "".join(rijen), f"{nq} quests"))
        main = (f'<h1>Quests</h1><p class="lead">{t("Every quest of the Vadsrijk. Each quest page has the steps with places on the map (walkthrough) and the dialogue, both behind a spoiler button.", "Elke quest van het Vadsrijk. Elke questpagina heeft de stappen met plekken op de kaart (walkthrough) en de dialoog, allebei achter een spoilerknop.")} {t("Click a zone to open it.", "Klik op een zone om hem open te klappen.")}</p>'
                + "".join(delen))
        self.schrijf(P, "Quests", "Quests", main, P)
        per_zone = {}
        for q in self.d["quests"]:
            per_zone.setdefault((q["zone"], q["soort"]), []).append(q)
        for q in self.d["quests"]:
            P = f"quests/{slug(q['id'])}.html"
            n = ZONE_NR[q["zone"]]
            rij = per_zone[(q["zone"], q["soort"])]
            i = rij.index(q)
            vorige = self.l_quest(rij[i - 1]["id"], P) if i > 0 else ""
            volgende = self.l_quest(rij[i + 1]["id"], P) if i + 1 < len(rij) else ""
            stappen = []
            for d in q["doelen"]:
                waar = ""
                if d["geheim"]:
                    waar = f'<div class="waar muted">{t("A secret: find it yourself!", "Een geheim: zoek het zelf!")}</div>'
                elif d["pos"]:
                    waar = f'<div class="waar">{self.l_kaart(d["pos"], P, "")}</div>'
                stappen.append(f"<li>{ESC(d['tekst'])}{(' <small class=muted>(' + str(d['n']) + 'x)</small>') if d['n'] > 1 else ''}{waar}</li>")
            stap_h = '<ol class="stappen">' + "".join(stappen) + "</ol>"

            def dial(regels):
                return '<ul class="dialoog">' + "".join(f'<li class="{w}"><b>{ESC(naam)}</b>{ESC(tekst)}</li>' for w, naam, tekst in regels) + "</ul>"
            bel = q["beloning"]
            belh = ", ".join([f"{bel['xp']} xp"] + ([f"{bel['munten']} {t('bronze', 'brons')}"] if bel["munten"] else [])
                             + ([t("a weapon for your class", "een wapen voor je klasse")] if bel["wapen"] else [])
                             + ([t(f"{bel['loot']} loot", f"{bel['loot']} buit")] if bel["loot"] else [])
                             + [f"{ESC(nm)} x{c}" for nm, c in bel["items"]])
            info = feiten([("Zone", zbadge(q["zone"], self.zone_naam(q["zone"]))), (t("Kind", "Soort"), t(*SOORT_Q[q["soort"]])), ("Level", str(q["lv"])),
                           (t("Given by", "Gever"), self.l_npc(q["gever"], P)), (t("Hand in", "Inleveren"), self.l_npc(q["inleveren"], P)),
                           (t("Reward", "Beloning"), belh), (t("Before", "Daarvoor"), ", ".join(self.l_quest(x, P) for x in q["na"])),
                           (t("Previous", "Vorige"), vorige), (t("Next", "Volgende"), volgende)])
            body = f'<p class="lead game-text">{ESC(q["tekst"])}</p>' if q["tekst"] else ""
            body += f'<h2>{t("Walkthrough", "Walkthrough")}</h2>' + spoiler(stap_h, "Show the steps", "Laat de stappen zien")
            if q["bezig"]:
                body += f'<h3>{t("Hint while you are busy", "Tip als je bezig bent")}</h3>' + spoiler(f'<p class="game-text">{ESC(q["bezig"])}</p>')
            if q["aanbod"] or q["klaar"]:
                body += f'<h2>{t("Dialogue", "Dialoog")}</h2>' + spoiler(
                    (f'<h3>{t("When you take it", "Bij het aannemen")}</h3>' + dial(q["aanbod"]) if q["aanbod"] else "")
                    + (f'<h3>{t("When you finish", "Bij het inleveren")}</h3>' + dial(q["klaar"]) if q["klaar"] else ""), "Show the dialogue", "Laat de dialoog zien")
            if q["soort"] == "geheim":
                body = spoiler(body, "A secret quest: click to show", "Een geheime quest: klik om te kijken")
            main = (f'<div data-zone="{n}"><nav class="crumbs"><a href="index.html">Quests</a><span aria-hidden="true">&rsaquo;</span>{ESC(self.zone_naam(q["zone"]))}</nav>'
                    f'<h1>{ESC(q["titel"]) if q["soort"] != "geheim" else t("Secret quest", "Geheime quest")}</h1><div class="tw"><div>{body}</div>'
                    f'<aside class="zij"><div class="kaartje">{info}</div></aside></div></div>'
                    f'<div class="verstopt-zone" data-zone="{n}">{t("This quest is in a zone hidden by your spoiler setting.", "Deze quest zit in een zone die je spoilerinstelling verbergt.")}</div>')
            titel = q["titel"] if q["soort"] != "geheim" else "Geheime quest"
            self.schrijf(P, titel, titel, main, "quests/index.html", beschrijving=f"Quest {titel} ({self.zone_naam(q['zone'])}).")

    # npcs
    def npcs(self):
        P = "npcs/index.html"
        delen = []
        for z in self.d["zones"]:
            ns = sorted([x for x in self.d["npcs"] if x["zone"] == z["id"]], key=lambda x: (not x["quests"], x["naam"]))
            rij = "".join(f'<tr><td><a href="{slug(x["id"])}.html">{ESC(x["naam"])}</a></td><td>{ESC(x["waar"])}</td><td class="num">{len(x["quests"]) or ""}</td></tr>' for x in ns)
            delen.append(f'<section data-zone="{z["nr"]}"><h2>{ESC(z["naam"])}</h2><div class="tscroll"><table class="data sort" id="npc-{z["id"]}"><thead><tr><th>'
                         + t("Name", "Naam") + "</th><th>" + t("Where", "Waar") + '</th><th class="num">Quests</th></tr></thead><tbody>' + rij + "</tbody></table></div></section>")
        self.schrijf(P, "Personages", "Characters", f'<h1>{t("Characters", "Personages")}</h1>' + "".join(delen), "zones/index.html")
        for x in self.d["npcs"]:
            P = f"npcs/{slug(x['id'])}.html"
            n = ZONE_NR[x["zone"]]
            qs = "".join(f"<li>{self.l_quest(q, P)}</li>" for q in x["quests"])
            praat = "".join(f'<li>{ESC(r)}</li>' for r in x["praat"])
            info = feiten([("Zone", zbadge(x["zone"], self.zone_naam(x["zone"]))), (t("Where", "Waar"), ESC(x["waar"])),
                           (t("Map", "Kaart"), self.l_kaart(x["pos"], P) if x["pos"] else "")])
            main = (f'<div data-zone="{n}"><nav class="crumbs"><a href="index.html">{t("Characters", "Personages")}</a></nav><h1>{ESC(x["naam"])}</h1><div class="tw"><div>'
                    + (f'<h2>Quests</h2><ul>{qs}</ul>' if qs else "")
                    + (f'<h2>{t("Says", "Zegt")}</h2><ul class="dialoog">{praat}</ul>' if praat else "")
                    + f'</div><aside class="zij"><div class="kaartje">{info}</div></aside></div></div><div class="verstopt-zone" data-zone="{n}">{t("Hidden by your spoiler setting.", "Verborgen door je spoilerinstelling.")}</div>')
            self.schrijf(P, x["naam"], x["naam"], main, "zones/index.html")

    # creatures
    def wezens(self):
        P = "wezens/index.html"
        def rij(w):
            n = ZONE_NR[w["zone"]]
            return (f'<tr data-zone="{n}"><td><a class="metfoto" href="{slug(w["kort"])}.html">{self.render("wezen_" + w["kort"], P, "render mini")}{ESC(w["naam"])}</a></td><td data-v="{w["zone"]}">{zbadge(w["zone"], self.zone_naam(w["zone"]))}</td>'
                    f'<td class="num" data-s="{(w["lv"] or [0])[0]}">{(str(w["lv"][0]) + "-" + str(w["lv"][1])) if w["lv"] else ""}</td>'
                    f'<td>{elems(w["elementen"])}</td>'
                    f'<td>{", ".join(ESC(self.gebied.get(g, {}).get("naam", g)) for g in w["gebieden"][:3])}</td><td class="tipcel">{ESC(w["tip"])}</td></tr>')
        vecht = [w for w in self.d["wezens"] if not w["critter"]]
        crit = [w for w in self.d["wezens"] if w["critter"]]
        filt = (f'<div class="filters"><input type="search" data-filter="#wz" placeholder="Zoek / search..." aria-label="Zoek">'
                f'<select data-filter="#wz" data-filter-col="1" aria-label="Zone"><option value="">Alle zones / all zones</option>'
                + "".join(f'<option value="{z["id"]}" data-zone="{z["nr"]}">{ESC(z["naam"])}</option>' for z in self.d["zones"]) + '</select><span class="muted"><span id="wz-n">'
                + str(len(vecht)) + "</span> " + t("creatures", "wezens") + "</span></div>")
        kop = ("<thead><tr><th>" + t("Name", "Naam") + "</th><th>Zone</th><th class='num'>Level</th><th>" + t("Element", "Element") + "</th><th>"
               + t("Where", "Waar") + "</th><th>" + t("Tip", "Tip") + "</th></tr></thead>")
        main = (f'<h1>{t("Creatures", "Wezens")}</h1><p class="lead">{t("Every grumpy creature you can cheer up, with its levels, element, where it lives and how to beat it. Bosses have their own pages.", "Elk chagrijnig wezen dat je kunt opvrolijken, met levels, element, waar het woont en hoe je het verslaat. Bazen hebben een eigen pagina.")} '
                f'<a href="../bazen/index.html">{t("Bosses", "Bazen")}</a>.</p>{filt}<div class="tscroll"><table class="data sort" id="wz" data-pagina="20">{kop}<tbody>'
                + "".join(rij(w) for w in vecht) + "</tbody></table></div>"
                + f'<h2>{t("Peaceful critters", "Vredige diertjes")}</h2><div class="tscroll"><table class="data">{kop}<tbody>' + "".join(rij(w) for w in crit) + "</tbody></table></div>")
        self.schrijf(P, "Wezens", "Creatures", main, P)
        for w in self.d["wezens"]:
            P = f"wezens/{slug(w['kort'])}.html"
            n = ZONE_NR[w["zone"]]
            loot = ""
            if w.get("loot"):
                L = w["loot"]
                som = sum(L["regels"].values()) or 1
                rijen = "".join(f'<tr><td>{self.l_item(b, P)}</td><td class="num">{L["kans"] * v / som * 100:.1f}%</td></tr>'
                                for b, v in sorted(L["regels"].items(), key=lambda kv: -kv[1]))
                ro, ka, ge = L["rollen"], L["kans"] * 100, L["geluk"]
                loot = (f'<h2>{t("Loot", "Buit")}</h2><p>{t(f"{ro} roll(s), each {ka:.0f}% chance on gear; rarity rolled with luck {ge}.", f"{ro} keer rollen, elke keer {ka:.0f}% kans op uitrusting; de zeldzaamheid met geluk {ge}.")} '
                        f'{t("Coins", "Munten")}: {L["munten_min"]}-{L["munten_max"]} {t("bronze", "brons")}.</p><div class="tscroll"><table class="data"><thead><tr><th>Item</th><th class="num">'
                        + t("Chance per roll", "Kans per rol") + "</th></tr></thead><tbody>" + rijen + "</tbody></table></div>")
            info = feiten([("Zone", zbadge(w["zone"], self.zone_naam(w["zone"]))), ("Level", lv(w["lv"])),
                           (t("Element", "Element"), elems(w["elementen"])),
                           (t("Elite chance", "Elitekans"), f'{w["elite"]}%' if w["elite"] else ""),
                           (t("Respawn", "Respawn"), f'{w["respawn_s"]} s' if w.get("respawn_s") else ""),
                           (t("Where", "Waar"), ", ".join(self.l_gebied(g, P) for g in w["gebieden"]))])
            main = (f'<div data-zone="{n}"><nav class="crumbs"><a href="index.html">{t("Creatures", "Wezens")}</a></nav><h1>{ESC(w["naam"])}</h1><div class="tw"><div>'
                    + self.render("wezen_" + w["kort"], P, "render groot", w["naam"])
                    + (f'<h2>{t("How to beat it", "Zo versla je hem")}</h2><p class="note">{ESC(w["tip"])}</p>' if w["tip"] else "")
                    + (f'<p>{t("A peaceful critter: it never attacks.", "Een vredig diertje: het valt nooit aan.")}</p>' if w["critter"] else "")
                    + loot + f'</div><aside class="zij"><div class="kaartje">{info}</div></aside></div></div><div class="verstopt-zone" data-zone="{n}">{t("Hidden by your spoiler setting.", "Verborgen door je spoilerinstelling.")}</div>')
            self.schrijf(P, w["naam"], w["naam"], main, "wezens/index.html")

    def bazen(self):
        P = "bazen/index.html"
        delen = []
        for z in self.d["zones"]:
            bz = [b for b in self.d["bazen"] if b["zone"] == z["id"]]
            from . import raid
            rk = raid.kaartje(self, P) if z["id"] == "zone6" else ""   # U-SITE round 3: the raid card
            delen.append(self.zonevak(z, '<div class="grid">' + "".join(self._baas_kaartje(b, P) for b in bz) + rk + "</div>", f'{len(bz)} {t("bosses", "bazen")}' + (" + raid" if rk else "")))
        regels = p("Every boss: a fight for everyone nearby. Your loot is personal: everyone who did at least 5% of the work gets their own chest. Fainted? You wake up at the arena door. Bosses get stronger with more players.",
                   "Elke baas: een gevecht voor iedereen in de buurt. Buit is persoonlijk: iedereen die minstens 5% van het werk deed krijgt een eigen kist. Flauwgevallen? Je wordt wakker bij de arenadeur. Bazen worden sterker met meer spelers.")
        self.schrijf(P, "Bazen", "Bosses", f'<h1>{t("Bosses", "Bazen")}</h1>{regels}<p class="muted">{t("Click a zone to open it.", "Klik op een zone om hem open te klappen.")}</p>' + "".join(delen), P)
        for b in self.d["bazen"]:
            P = f"bazen/{slug(b['id'])}.html"
            n = ZONE_NR[b["zone"]]
            mech = ""
            if b["tip"]:
                mech += f'<p class="note">{ESC(b["tip"])}</p>'
            if b["tip2"]:
                mech += f'<h3>{ESC(b["tip2_titel"])}</h3><p class="note">{ESC(b["tip2"])}</p>'
            vahoeg = (f'<h2>{t("Vahoeg mode", "Vahoeg-modus")}</h2>' + p("Put a Vahoeg seal on the altar at the dungeon door (from lv 50): for 30 minutes every boss of the dungeon is in its Vahoeg form: lv 52, x1.5 HP, x1.25 damage, one extra mechanic and the week seal. They drop Golden Feathers.",
                                                                          "Leg een Vahoegzegel op het altaar bij de kerkerdeur (vanaf lv 50): 30 minuten lang is elke baas van de kerker in zijn Vahoeg-vorm: lv 52, x1,5 levens, x1,25 schade, één extra mechanic en het weekzegel. Ze laten Gouden Veren vallen.")) if b["vahoeg"] else ""
            buit = "".join(f"<li>{self.l_item(i, P)}{self._kans_named(i)}</li>" for i in b["buit"])
            alle_buit = self._alle_buit(b, P)
            huisdier = next((h for h in self.d["huisdieren"] if h["id"] == b["huisdier"]), None)
            info = feiten([("Zone", zbadge(b["zone"], self.zone_naam(b["zone"]))), (t("Kind", "Soort"), t(*SOORT_B.get(b["soort"], SOORT_B["baas"]))),
                           (t("Where", "Waar"), self.l_gebied(b["gebied"], P) if b["gebied"] else ""),
                           (t("Respawn", "Respawn"), f'{b["arena"]["respawn_s"] // 60} min' if b.get("arena") and b["arena"]["respawn_s"] >= 60 else (f'{b["arena"]["respawn_s"]} s' if b.get("arena") else "")),
                           (t("Map", "Kaart"), self.l_kaart(b["arena"]["pos"], P) if b.get("arena") and not b["spoiler"] else ""),
                           (t("Pet", "Huisdier"), ESC(huisdier["naam"]) if huisdier else "")])
            body = (self.render("baas_" + b["id"], P, "render groot", b["naam"]) + f'<h2>{t("Mechanics", "Mechanics")}</h2>{mech or "<p class=leeg>-</p>"}{vahoeg}'
                    + (f'<h2>{t("Named loot", "Benoemde buit")}</h2><ul>{buit}</ul>' if buit else "") + alle_buit
                    + (f'<h2>{t("Pet", "Huisdier")}</h2><p><b>{ESC(huisdier["naam"])}</b>: {ESC(huisdier["tekst"])} <i>{ESC(huisdier["bron"])}</i></p>' if huisdier else ""))
            if b["spoiler"]:
                body = spoiler(body, "A story boss: click to show", "Een verhaalbaas: klik om te kijken")
            titel = b["naam"]
            main = (f'<div data-zone="{n}"><nav class="crumbs"><a href="index.html">{t("Bosses", "Bazen")}</a></nav><h1>{ESC(titel) if not b["spoiler"] else t("Story boss", "Verhaalbaas")}</h1><div class="tw"><div>{body}</div>'
                    f'<aside class="zij">{"" if b["spoiler"] else "<div class=kaartje>" + info + "</div>"}</aside></div></div><div class="verstopt-zone" data-zone="{n}">{t("Hidden by your spoiler setting.", "Verborgen door je spoilerinstelling.")}</div>')
            self.schrijf(P, titel if not b["spoiler"] else "Verhaalbaas", titel, main, "bazen/index.html")

    def _zz(self, i):
        z = i.get("vaste_zeldzaamheid")
        return f'<span class="zz {z}">{ESC(z)}</span>' if z else t("rolled", "willekeurig")

    def _kans_named(self, iid):
        i = self.items.get(iid) or {}
        if not i.get("baaskans"):
            return ""
        zz = i.get("vaste_zeldzaamheid") or ""
        return f' <span class="badge lv">{i["baaskans"] * 100:g}%</span> <span class="zz {zz}">{ESC(zz)}</span>'

    def _loot_tabel(self, L, P):
        som = sum(L["regels"].values()) or 1
        rijen = "".join(f'<tr><td>{self.l_item(b, P)}</td><td class="num">{L["kans"] * v / som * 100:.1f}%</td></tr>'
                        for b, v in sorted(L["regels"].items(), key=lambda kv: -kv[1]))
        return ('<div class="tscroll"><table class="data"><thead><tr><th>Item</th><th class="num">' + t("Chance per roll", "Kans per rol")
                + "</th></tr></thead><tbody>" + rijen + "</tbody></table></div>")

    def _alle_buit(self, b, P):
        """U-SITE: every drop of a boss with its chance: the chest table, named items (above) and the extras."""
        uit = f'<h2>{t("All drops", "Alle buit")}</h2>'
        uit += p("Every helper (at least 5% of the work) gets their own chest; the chances below are per player.", "Elke helper (minstens 5% van het werk) krijgt een eigen kist; de kansen hieronder zijn per speler.")
        L = b.get("kist")
        if L:
            ro, ge = L["rollen"], L["geluk"]
            uit += (f'<h3>{t("The boss chest", "De baaskist")}</h3><p>{t(f"{ro} rolls, rarity rolled with luck {ge}.", f"{ro} keer rollen, de zeldzaamheid met geluk {ge}.")} '
                    f'{t("Coins", "Munten")}: {L["munten_min"]}-{L["munten_max"]} {t("bronze", "brons")}.</p>' + self._loot_tabel(L, P))
        soort = {"rijdier": ("Mount", "Rijdier"), "vacht": ("Mount coat", "Rijdiervacht"), "patroon": ("Mount pattern", "Rijdierpatroon"),
                 "tuig": ("Mount tack", "Rijdiertuig"), "huisdier": ("Pet", "Huisdier"), "trofee": ("Trophy", "Trofee"), "kern": ("Boss core", "Bazenkern"),
                 "groei": ("Growth fluff", "Groeipluis"), "veer": ("Feathers", "Veren"), "kleding": ("Wardrobe", "Kledingkast"),
                 "stof": ("Star dust", "Sterrenstof")}
        ex = b.get("extra") or []
        if ex:
            uit += (f'<h3>{t("Also", "Ook")}</h3><div class="tscroll"><table class="data"><thead><tr><th>{t("Kind", "Soort")}</th><th>{t("What", "Wat")}</th><th>'
                    + t("Chance", "Kans") + "</th></tr></thead><tbody>"
                    + "".join(f'<tr><td>{t(*soort.get(k, (k, k)))}</td><td>{ESC(n)}</td><td>{t(en or nl, nl)}</td></tr>' for k, n, nl, en in ex)
                    + "</tbody></table></div>")
        return uit

    # items
    def _stats(self, per_level, lvl):
        s = (per_level or {}).get(str(lvl)) or {}
        return ", ".join(f"{STAT.get(k, (k, k))[1]} {v:g}" for k, v in s.items())

    def items_(self):
        P = "items/index.html"
        rijen = []
        for i in self.d["items"]:
            n = ZONE_NR.get(i["zone"], 1)
            sl = SLOT.get(i["slot"] or "", ("", ""))
            kl = self.d_klasse(i["klasse"])
            rijen.append(f'<tr data-zone="{n}"><td>{self.l_item(i["id"], P)}{" &#9733;" if i["benoemd"] else ""}</td><td data-v="{i["slot"] or ""}">{t(*sl)}</td>'
                         f'<td data-v="{i["klasse"] or "alle"}">{ESC(kl)}</td><td data-v="{i["zone"]}">{zbadge(i["zone"], self.zone_naam(i["zone"]))}</td>'
                         f'<td data-v="{i.get("vaste_zeldzaamheid") or "willekeurig"}">{self._zz(i)}</td>'
                         f'<td>{ESC(next((s["naam"] for s in self.d["sets"] if s["id"] == i["set"]), ""))}</td><td>{self._stats(i["per_level"], 10)}</td></tr>')
        zone_opties = "".join(f'<option value="{z["id"]}" data-zone="{z["nr"]}">{ESC(z["naam"])}</option>' for z in self.d["zones"])   # W-SITE
        opties = lambda pairs: "".join(f'<option value="{v}">{l}</option>' for v, l in pairs)  # noqa: E731
        filt = (f'<div class="filters"><input type="search" data-filter="#it" placeholder="Zoek / search..." aria-label="Zoek">'
                f'<select data-filter="#it" data-filter-col="1" aria-label="Slot"><option value="">Alle slots / all slots</option>{opties((k, v[1]) for k, v in SLOT.items())}</select>'
                f'<select data-filter="#it" data-filter-col="2" aria-label="Klasse"><option value="">Alle klassen / all classes</option>{opties([(k["id"], ESC(k["naam"])) for k in self.d["klassen"]] + [("alle", "Elke klasse / every class")])}</select>'
                f'<select data-filter="#it" data-filter-col="3" aria-label="Zone"><option value="">Alle zones / all zones</option>{zone_opties}</select>'
                f'<select data-filter="#it" data-filter-col="4" aria-label="Zeldzaamheid"><option value="">Alle zeldzaamheden / all rarities</option>'
                f'{opties([("willekeurig", "Willekeurig / rolled")] + [(z["id"], ESC(z["naam"])) for z in self.d["zeldzaamheden"]])}</select>'
                f'<span class="muted"><span id="it-n">{len(rijen)}</span> items</span></div>')
        uitleg = p("Every base type of gear. The real item rolls a rarity (more bonuses and x1.10..x1.75 base stats), a level and random bonuses; the stats below are the base at level 10. &#9733; = a named boss item.",
                   "Elk basistype uitrusting. Het echte item rolt een zeldzaamheid (meer bonussen en x1,10..x1,75 basisstats), een level en willekeurige bonussen; de stats hieronder zijn de basis op level 10. &#9733; = een benoemd baasitem.")
        main = (f'<h1>{t("Item database", "Itemdatabase")}</h1>{uitleg}{filt}<div class="tscroll"><table class="data sort" id="it" data-pagina="25"><thead><tr><th>Item</th><th>Slot</th><th>'
                + t("Class", "Klasse") + "</th><th>Zone</th><th>" + t("Rarity", "Zeldzaamheid") + "</th><th>Set</th><th>" + t("Base stats (lv 10)", "Basisstats (lv 10)") + "</th></tr></thead><tbody>" + "".join(rijen) + "</tbody></table></div>")
        self.schrijf(P, "Itemdatabase", "Item database", main, P)
        for i in self.d["items"]:
            P = f"items/{slug(i['id'])}.html"
            n = ZONE_NR.get(i["zone"], 1)
            pl = i.get("per_level") or {}
            keys = []
            for v in pl.values():
                for k in v:
                    if k not in keys:
                        keys.append(k)
            stab = ""
            if pl:
                stab = ('<div class="tscroll"><table class="data"><thead><tr><th>Level</th>' + "".join(f'<th class="num">{t(*STAT.get(k, (k, k)))}</th>' for k in keys)
                        + "</tr></thead><tbody>" + "".join(f'<tr><td>{L}</td>' + "".join(f'<td class="num">{v.get(k, 0):g}</td>' for k in keys) + "</tr>" for L, v in pl.items())
                        + "</tbody></table></div>")
            setd = next((s for s in self.d["sets"] if s["id"] == i["set"]), None)
            _bz = {x["id"]: x for x in self.d["bazen"]}   # W-SITE: each source carries its zone (hidden above the spoiler setting)
            bronnen = [self.l_wezen(b, P) for b in sorted(i["bronnen"], key=lambda b: znr((self.wezen.get(b) or {}).get("zone")))] + [
                zl(f'<a href="../bazen/{slug(b)}.html">{ESC(_bz[b]["naam"])}</a>', _bz[b].get("zone")) for b in i["bronnen_baas"] if b in _bz and not _bz[b]["spoiler"]]
            if i["id"].startswith("z6raid_") and self.d.get("raid"):   # U-SITE round 3
                bronnen.append(f'<a href="../raid/index.html#buit">{ESC(self.d["raid"]["naam"])}</a>')
            if i.get("baaskans"):
                bronnen[-1:] = [bronnen[-1] + f' ({i["baaskans"] * 100:g}%, <span class="zz {i["vaste_zeldzaamheid"]}">{ESC(i["vaste_zeldzaamheid"])}</span>)'] if bronnen else []
            flav = "".join(f'<li class="game-text">{ESC(f)}</li>' for f in i["flavour"])
            info = feiten([("Zone", zbadge(i["zone"], self.zone_naam(i["zone"]))), ("Slot", t(*SLOT.get(i["slot"] or "", ("", "")))),
                           (t("Class", "Klasse"), ESC(self.d_klasse(i["klasse"]))), (t("Kind", "Soort"), ESC(i["soort"] or "")),
                           (t("Element", "Element"), f'<span class="el {i["element"]}">{i["element"]}</span>' if i.get("element") and i["element"] != "geen" else ""),
                           ("Set", f'<a href="../systemen/sets.html#{setd["id"]}">{ESC(setd["naam"])}</a>' if setd else "")])
            ic = self.iconen.get(i["id"])
            main = (f'<div data-zone="{n}"><nav class="crumbs"><a href="index.html">Items</a></nav><h1>{grootico(ic)}{ESC(i["naam"])}</h1><div class="tw"><div>'
                    + (f'<ul class="dialoog">{flav}</ul>' if flav else "")
                    + (f'<h2>{t("Base stats per level", "Basisstats per level")}</h2>{stab}<p class="muted">{t("Times the rarity factor (Gewoon x1.00 ... Guhddelijk x1.75), plus the rolled bonuses, stars and sockets.", "Maal de zeldzaamheidsfactor (Gewoon x1,00 ... Guhddelijk x1,75), plus de gerolde bonussen, sterren en sokjes.")}</p>' if stab else "")
                    + (f'<h2>Set: {ESC(setd["naam"])}</h2><p>{ESC(setd["bonus2"])}<br>{ESC(setd["bonus4"])}{("<br>" + ESC(setd["bonus6"])) if setd.get("bonus6") else ""}</p>' if setd else "")
                    + f'<h2>{t("Where to get it", "Waar te krijgen")}</h2>{komma(bronnen) if bronnen else "<p>" + t("Shops, quests or a boss chest.", "Winkels, quests of een baaskist.") + "</p>"}'
                    + f'</div><aside class="zij"><div class="kaartje">{info}</div></aside></div></div><div class="verstopt-zone" data-zone="{n}">{t("Hidden by your spoiler setting.", "Verborgen door je spoilerinstelling.")}</div>')
            self.schrijf(P, i["naam"], i["naam"], main, "items/index.html")

    def t_rijdier(self, rid):
        r = next((x for x in self.d["rijdieren"] if x["id"] == rid), None)
        return r["naam"] if r else rid.capitalize()

    def d_klasse(self, k):
        if not k:
            return "Alle klassen"
        for x in self.d["klassen"]:
            if x["id"] == k:
                return x["naam"]
        return k

    # mechanics
    def systemen(self):
        P = "systemen/index.html"
        kaarten = [("sets.html", "Sets", "Sets", "Set bonuses for 2 and 4 pieces.", "Setbonussen voor 2 en 4 stukken."),
                   ("kristallen.html", "Rarities, stars and crystals", "Zeldzaamheid, sterren en kristallen", "Loot colours, the smith's stars, gems in sockets.", "Buitkleuren, de sterren van de smid, edelstenen in sokjes."),
                   ("klassen.html", "Classes", "Klassen", "Knuffelridder, Pluisjager, Vadsmagiër.", "Knuffelridder, Pluisjager, Vadsmagiër."),
                   ("rijdieren.html", "Mounts and pets", "Rijdieren en huisdieren", "How to get every mount and pet.", "Hoe je elk rijdier en huisdier krijgt."),
                   ("../npcs/index.html", "Characters", "Personages", "Everyone who lives in the Vadsrijk.", "Iedereen die in het Vadsrijk woont.")]
        from . import systemen2   # U-SITE: the night round's systems
        kaarten = kaarten[:4] + systemen2.KAARTEN + kaarten[4:]
        main = f'<h1>{t("Mechanics", "Systemen")}</h1><div class="grid">' + "".join(f'<a class="kaartje" href="{u}"><h3>{t(a, b)}</h3><p>{t(c, d)}</p></a>' for u, a, b, c, d in kaarten) + "</div>"
        self.schrijf(P, "Systemen", "Mechanics", main, P)
        # sets
        P = "systemen/sets.html"
        blok = ""
        for s in self.d["sets"]:
            z = next((self.items[x]["zone"] for x in s["stukken"] if x in self.items), "zone1")
            blok += (f'<section data-zone="{ZONE_NR[z]}" class="kaartje" style="margin:12px 0"><h2 id="{s["id"]}" style="margin-top:0">{ESC(s["naam"])} {zbadge(z, self.zone_naam(z))}</h2>'
                     f'<p>{" &middot; ".join(self.l_item(x, P) for x in s["stukken"])}</p><p>{ESC(s["bonus2"])}<br>{ESC(s["bonus4"])}{("<br>" + ESC(s["bonus6"])) if s.get("bonus6") else ""}</p></section>')
        self.schrijf(P, "Sets", "Sets", f'<h1>Sets</h1>{blok or "<p class=leeg>-</p>"}', "systemen/index.html")
        # rarities, stars, crystals
        P = "systemen/kristallen.html"
        zz = self.d["zeldzaamheden"]
        som = sum(z.get("gewicht", 0) for z in zz) or 1
        ztab = ('<div class="tscroll"><table class="data"><thead><tr><th>' + t("Rarity", "Zeldzaamheid") + '</th><th class="num">' + t("Bonuses", "Bonussen")
                + '</th><th class="num">' + t("Base stats", "Basisstats") + '</th><th class="num">' + t("Drop chance (normal)", "Kans (gewoon)") + "</th></tr></thead><tbody>"
                + "".join(f'<tr><td><span class="zz {z["id"]}" style="color:{z.get("kleur", "")}">{ESC(z["naam"])}</span></td><td class="num">{z.get("min_bonus", "")}-{z.get("max_bonus", "")}</td>'
                          f'<td class="num">x{z.get("factor", 1):g}</td><td class="num">{z.get("gewicht", 0) / som * 100:.1f}%</td></tr>' for z in zz) + "</tbody></table></div>")
        st = self.d.get("sterren") or {}
        stab = ""
        if st.get("rijen"):
            stab = ('<div class="tscroll"><table class="data"><thead><tr><th>' + t("Stars", "Sterren") + '</th><th class="num">' + t("Success", "Lukt") + '</th><th class="num">'
                    + t("Lose a star", "Ster kwijt") + '</th><th class="num">' + t("Verpluisd (broken)", "Verpluisd") + '</th><th class="num">' + t("Stones", "Steentjes")
                    + '</th><th class="num">' + t("Price lv 10", "Prijs lv 10") + '</th><th class="num">' + t("Price lv 30", "Prijs lv 30") + "</th></tr></thead><tbody>"
                    + "".join(f'<tr><td>{r["van"]} &rarr; {r["van"] + 1}</td><td class="num">{r["kans"] * 100:.0f}%</td><td class="num">{r["kwijt"] * 100:.1f}%</td>'
                              f'<td class="num">{r["verpluisd"] * 100:.1f}%</td><td class="num">{r["steentjes"]}</td><td class="num">{r["prijs_lv10"]}</td><td class="num">{r["prijs_lv30"]}</td></tr>' for r in st["rijen"])
                    + "</tbody></table></div>")
        ktab = ""
        if self.d["kristallen"]:
            ktab = ('<div class="tscroll"><table class="data"><thead><tr><th>' + t("Crystal", "Kristal") + "</th><th>Stat</th><th class='num'>" + t("Rough", "Ruw")
                    + "</th><th class='num'>" + t("Cut", "Geslepen") + "</th><th class='num'>" + t("Radiant", "Stralend") + "</th></tr></thead><tbody>"
                    + "".join(f'<tr data-zone="{3 if "z3" in k["item"] else 2}"><td><span class="swatch" style="background:{k["kleur"]}"></span>{ESC(k["naam"])}</td><td>{t(*STAT.get(k["stat"], (k["stat"], k["stat"])))}</td>'
                              + "".join(f'<td class="num">{v:g}</td>' for v in k["waarde"]) + "</tr>" for k in self.d["kristallen"]) + "</tbody></table></div>")
        main = (f'<h1>{t("Rarities, stars and crystals", "Zeldzaamheid, sterren en kristallen")}</h1><h2>{t("Rarities", "Zeldzaamheden")}</h2>{ztab}'
                f'<p class="muted">{t("Bosses roll with more luck: the rarer colours come more often.", "Bazen rollen met meer geluk: de zeldzamere kleuren komen vaker.")}</p>'
                f'<h2>{t("Stars (the smith)", "Sterren (de smid)")}</h2>{p("The smith puts stars on your gear, with a little risk. From 3 stars a failure can cost a star; from 6 stars an item can become verpluisd (the smith repairs it).", "De smid zet sterren op je spullen, met een beetje risico. Vanaf 3 sterren kan mislukken een ster kosten; vanaf 6 sterren kan een item verpluisd raken (de smid repareert het).")}{stab or "<p class=leeg>-</p>"}'
                f'<section data-zone="2"><h2>{t("Crystals and sockets", "Kristallen en sokjes")}</h2>{p("Gear can have 0, 1 or 2 sockets. Put a crystal in at the jeweller; normal creatures drop rough ones, elites cut ones, bosses radiant ones.", "Uitrusting kan 0, 1 of 2 sokjes hebben. Zet er een kristal in bij de juwelier; gewone wezens laten ruwe vallen, elites geslepen, bazen stralende.")}{ktab}</section>')
        self.schrijf(P, "Zeldzaamheid, sterren en kristallen", "Rarities, stars and crystals", main, "systemen/index.html")
        # classes
        P = "systemen/klassen.html"
        blok = ""
        for k in self.d["klassen"]:
            pl = k.get("per_level") or {}
            keys = []
            for v in pl.values():
                for x in v:
                    if x not in keys:
                        keys.append(x)
            tab = ('<div class="tscroll"><table class="data"><thead><tr><th>Level</th>' + "".join(f'<th class="num">{t(*STAT.get(x, (x, x)))}</th>' for x in keys) + "</tr></thead><tbody>"
                   + "".join(f'<tr><td>{L}</td>' + "".join(f'<td class="num">{v.get(x, 0):g}</td>' for x in keys) + "</tr>" for L, v in pl.items()) + "</tbody></table></div>") if pl else ""
            blok += f'<section class="kaartje" style="margin:12px 0"><h2 id="{k["id"]}" style="margin-top:0">{ESC(k["naam"])}</h2>{("<p>" + ESC(k["uitleg"]) + "</p>") if k["uitleg"] else ""}<p>{t("Weapons", "Wapens")}: {ESC(", ".join(k["wapens"]))}</p>{tab}</section>'
        intro = next((g["tekst"] for g in self.d["gids"] if g["id"] == "klassen"), "")
        self.schrijf(P, "Klassen", "Classes", f'<img src="../img/kop_klassen.webp" alt="" style="width:100%;border-radius:16px"><h1>{t("Classes", "Klassen")}</h1><p class="lead">{ESC(intro)}</p>{blok}', "systemen/index.html")
        # mounts + pets
        P = "systemen/rijdieren.html"
        bron = {"pluimguh": ("Your first mount, chosen in the Pluimstal (zone 3).", "Je eerste rijdier, gekozen in de Pluimstal (zone 3)."), "quest": ("A quest reward.", "Een questbeloning."),
                "stal": ("Bought at a stable.", "Te koop bij een stal."),
                "geheim": ("Secret: find hidden cuddly toys in every zone.", "Geheim: vind verstopte knuffels in alle zones."),
                "veren": ("Trade Golden Feathers (N screen).", "Ruil Gouden Veren (N-scherm)."), "baas": ("Beat a boss for the first time.", "Versla een baas voor het eerst.")}
        rt = "".join(f'<tr data-zone="{self.tekst_zone(r["naam"], r["tekst"], r.get("bron") or "")}"><td>{ESC(r["naam"])}</td><td>{ESC(r["tekst"])}</td><td>{t(*bron.get(r["bron"] or "", ("", "")))}{(" (" + str(r["prijs"]) + " " + t("bronze", "brons") + ")") if r.get("prijs") else ""}</td></tr>' for r in self.d["rijdieren"])
        ht = "".join(f'<tr data-zone="{self.tekst_zone(h["naam"], h["tekst"], h["bron"])}"><td>{ESC(h["naam"])}</td><td>{ESC(h["tekst"])}</td><td>{spoiler("<p>" + ESC(h["bron"]) + "</p>", "How to get it", "Hoe krijg je hem")}</td></tr>' for h in self.d["huisdieren"])
        main = (f'<h1>{t("Mounts and pets", "Rijdieren en huisdieren")}</h1>' + ((f'<h2>{t("Mounts", "Rijdieren")}</h2><div class="tscroll"><table class="data"><thead><tr><th>'
                + t("Mount", "Rijdier") + "</th><th>" + t("What", "Wat") + "</th><th>" + t("How", "Hoe") + "</th></tr></thead><tbody>" + rt + "</tbody></table></div>") if rt else "")   # X-SITE: no mounts before zone 3
                + f'<h2>{t("Pets", "Huisdieren")}</h2><div class="tscroll"><table class="data"><thead><tr><th>' + t("Pet", "Huisdier") + "</th><th>" + t("What", "Wat") + "</th><th>"
                + t("How", "Hoe") + "</th></tr></thead><tbody>" + ht + "</tbody></table></div>")
        # U-SITE: dressing up mounts + pet powers (t-rijdier)
        ru = self.d.get("rij_uiterlijk") or {}
        if ru:
            vacht = "".join(f'<li data-zone="{self.tekst_zone(self.t_rijdier(r), " ".join(v))}"><b>{ESC(self.t_rijdier(r))}</b>: {ESC(", ".join(v))}</li>' for r, v in ru.get("vacht", {}).items())
            main += (f'<h2>{t("Dressing up", "Aankleden")}</h2>' + p("Press N, choose a mount and click Aankleden: coat, pattern and tack. Looks never change speed or jump. Get them at the stable, with Golden Feathers, from achievements, bosses and secrets.",
                                                                   "Druk N, kies een rijdier en klik Aankleden: vacht, patroon en tuig. Uiterlijk verandert nooit de snelheid of sprong. Je krijgt ze bij de stal, met Gouden Veren, van prestaties, bazen en geheimen.")
                     + f'<ul><li><b>{t("Patterns", "Patronen")}</b>: {ESC(", ".join(ru.get("patroon", [])))}</li><li><b>{t("Tack", "Tuig")}</b>: {ESC(", ".join(ru.get("tuig", [])))}</li></ul>'
                     + f'<h3>{t("Coats", "Vachten")}</h3><ul>{vacht}</ul>')
        kr = self.d.get("krachten") or []
        if kr:
            main += (f'<h2>{t("Pet powers", "Krachten van huisdieren")}</h2>' + p("Pets never fight and are never hit. Each one is a Steuner (helps you) or a Snuffelaar (finds things), with its own power, and grows to level 10 by fighting along.",
                                                                                 "Huisdieren vechten nooit mee en worden nooit geraakt. Elk is een Steuner (helpt jou) of een Snuffelaar (vindt dingen), met een eigen kracht, en groeit tot level 10 door mee te gaan in gevechten.")
                     + '<div class="tscroll"><table class="data"><thead><tr><th>' + t("Power", "Kracht") + "</th><th>" + t("Role", "Rol") + "</th><th>" + t("What", "Wat")
                     + "</th></tr></thead><tbody>" + "".join(f'<tr data-zone="{self.tekst_zone(k["naam"], k["tekst"])}"><td>{ESC(k["naam"])}</td><td>{t("Supporter", "Steuner") if k["rol"] == "steuner" else t("Sniffer", "Snuffelaar")}</td><td>{ESC(k["tekst"])}</td></tr>' for k in kr)
                     + "</tbody></table></div>")
        self.schrijf(P, "Rijdieren en huisdieren", "Mounts and pets", main, "systemen/index.html")
        systemen2.alles(self)

    def verhaal(self):
        from . import verhaal as _vh   # U-SITE: the story page overhaul (chapters, cards, timeline, world map)
        _vh.verhaal(self)

    def highscores(self):  # (T-INT: + AMBACHT_LIJSTEN)
        lijsten = [dict(id="generiek", en="Level", nl="Level", k_en="Level", k_nl="Level"),
                   dict(id="knuffelridder", en="Knuffelridder", nl="Knuffelridder", k_en="Class level", k_nl="Klasse-level"),
                   dict(id="pluisjager", en="Pluisjager", nl="Pluisjager", k_en="Class level", k_nl="Klasse-level"),
                   dict(id="vadsmagier", en="Vadsmagiër", nl="Vadsmagiër", k_en="Class level", k_nl="Klasse-level"),
                   dict(id="opgevrolijkt", en="Cheered up", nl="Opgevrolijkt", k_en="Creatures", k_nl="Wezens"),
                   dict(id="bazen", en="Bosses", nl="Bazen", k_en="Bosses beaten", k_nl="Bazen opgevrolijkt"),
                   dict(id="verkenning", en="Exploration book", nl="Verkenningsboek", k_en="% explored", k_nl="% verkend")]
        # T-INT: the Ambachten lists of t-ambacht (hiscores/ambacht_<id>.json, written by the mod's WebExport)
        lijsten += [dict(id="ambacht_" + i, en=en, nl=nl, k_en="Level", k_nl="Level") for i, en, nl in AMBACHT_LIJSTEN]
        main = (f'<h1>Highscores</h1><p>{t("Everyone is on the lists (name + number); ties go to whoever got there first. Click a name for the profile.", "Iedereen staat op de lijsten (naam + getal); bij gelijkspel wint wie er het eerst was. Klik op een naam voor het profiel.")}</p>'
                f"<div data-hiscores='{ESC(json.dumps(lijsten, ensure_ascii=False))}'><div class=\"hs-tabs\" role=\"group\"></div>"
                f'<div class="filters"><input type="search" placeholder="Zoek een speler / find a player..." aria-label="Zoek"></div>'
                f'<div class="tscroll"><table class="data"><thead><tr><th class="num">#</th><th>{t("Player", "Speler")}</th><th class="num" data-k="kop">Level</th><th class="num">XP</th></tr></thead><tbody></tbody></table></div>'
                f'<p class="muted">{t("Updated", "Bijgewerkt")}: <span data-k="tijd">-</span></p></div>')
        self.schrijf("highscores.html", "Highscores", "Highscores", main, "highscores.html")

    def speler(self):
        main = f'<div data-profiel><div data-k="uit"><p class="leeg">...</p></div></div><noscript>{t("This page needs JavaScript.", "Deze pagina heeft JavaScript nodig.")}</noscript>'
        self.schrijf("speler.html", "Speler", "Player", main, "highscores.html")

    def live(self):
        main = (f'<h1>Live</h1>{self._status_tegels()}<div data-live-wereld>'
                f'<div class="live-rij"><div class="live-tegel"><div class="k">{t("Daily reset (06:00)", "Dagreset (06:00)")}</div><div class="v" data-k="reset">-</div></div>'
                f'<div class="live-tegel" data-zone="3"><div class="k">{t("Vahoeg week seal", "Vahoeg-weekzegel")}</div><div class="v" style="font-size:20px" data-k="zegel">-</div><div class="s">{t("changes every Monday", "wisselt elke maandag")}</div></div>'
                f'<div class="live-tegel" data-zone="3"><div class="k">{t("Contracts today", "Contracten vandaag")}</div><div class="s" data-k="contracten">-</div></div></div>'
                f'<h2>{t("The caravan", "De karavaan")}</h2><p data-k="karavaan">-</p>'
                f'<h2>{t("Boss timers", "Baastimers")}</h2><div class="tscroll"><table class="data timers"><thead><tr><th>{t("Boss", "Baas")}</th><th>{t("Now", "Nu")}</th>'
                f'<th class="num">{t("Back in", "Terug over")}</th><th class="num">Respawn</th></tr></thead><tbody data-k="bazen"></tbody></table></div></div>'
                f'<p class="muted">{t("Read from live.guhs.nl (refreshes by itself). Player positions are only on the world map (layer Show players), and you can hide yourself with /guhs vadsrijk kaart verberg.", "Gelezen van live.guhs.nl (ververst vanzelf). Spelersposities staan alleen op de wereldkaart (laag Spelers tonen), en je kunt jezelf verbergen met /guhs vadsrijk kaart verberg.")}</p>')
        self.schrijf("live.html", "Live", "Live", main, "live.html")

    def patchnotes(self):
        from . import patchnotes as PN
        notes = PN.lees(os.path.join(self.root, "docs", "patchnotes"), self.concepten, self.open_zones)   # X-SITE: :::zone blocks
        os.makedirs(os.path.join(self.out, "patchnotes", "img"), exist_ok=True)
        lijst = ""
        for n in notes:
            P = f"patchnotes/{slug(n['versie'])}.html"
            for b in n["beelden"]:
                src = os.path.join(self.root, "docs", "patchnotes", b["pad"])
                if os.path.exists(src):
                    _webp(src, os.path.join(self.out, "patchnotes", "img", os.path.splitext(os.path.basename(b["pad"]))[0] + ".webp"), max_w=1600)
            main = (f'<nav class="crumbs"><a href="index.html">Patch notes</a></nav><h1>{ESC(n["titel"])}</h1><p class="muted">Guhs {ESC(n["versie"])} &middot; {ESC(n["datum"])}'
                    + (' &middot; <span class="badge">' + t("draft", "concept") + "</span>" if n["concept"] else "") + "</p>" + PN.html_van(n, "img/"))
            self.schrijf(P, n["titel"], n["titel"], main, "patchnotes/index.html")
            lijst += f'<a class="kaartje" href="{slug(n["versie"])}.html"><h3>{ESC(n["titel"])}</h3><p class="muted">Guhs {ESC(n["versie"])} &middot; {ESC(n["datum"])}</p><p>{ESC(n["samenvatting"])}</p></a>'
        main = (f'<h1>Patch notes</h1><p>{t("The same text as the Discord post, here with more pictures (spoilers behind a button).", "Dezelfde tekst als de Discord-post, hier met meer plaatjes (spoilers achter een knop).")}</p>'
                f'<div class="grid">{lijst or "<p class=leeg>-</p>"}</div>')
        self.schrijf("patchnotes/index.html", "Patch notes", "Patch notes", main, "patchnotes/index.html")

    def privacy(self):
        main = (f'<h1>Privacy</h1>' + p("What the Vadsrijk pages show about players: your Minecraft name, your levels and class levels, how many creatures and bosses you cheered up, your Exploration book %, your gear (type, rarity, level, stars), how many pets and mounts you have, your title and the DAY you were last seen. Your skin head comes from mc-heads.net.",
                                         "Wat de Vadsrijk-pagina's over spelers laten zien: je Minecraft-naam, je levels en klasse-levels, hoeveel wezens en bazen je opvrolijkte, je Verkenningsboek-%, je uitrusting (soort, zeldzaamheid, level, sterren), hoeveel huisdieren en rijdieren je hebt, je titel en de DAG dat je het laatst gezien bent. Je skinhoofdje komt van mc-heads.net.")
                + p("The world map can show where you are in the Vadsrijk while you are online (about every 10 seconds; never inside dungeons, the raid or houses, there it only says \"in a dungeon\"). Don't want that? Type /guhs vadsrijk kaart verberg in game (and /guhs vadsrijk kaart toon to be visible again).",
                    "De wereldkaart kan laten zien waar je in het Vadsrijk bent terwijl je online bent (ongeveer elke 10 seconden; nooit in kerkers, de raid of huisjes, daar staat alleen \"in een kerker\"). Liever niet? Typ in het spel /guhs vadsrijk kaart verberg (en /guhs vadsrijk kaart toon om weer zichtbaar te zijn).")
                + p("Never: your IP address, your chat, your coins or who you traded with. The server writes these files itself; this site only reads them.",
                    "Nooit: je IP-adres, je chat, je munten of met wie je handelde. De server schrijft deze bestanden zelf; deze site leest ze alleen.")
                + p("Want to be left out? Ask the server admin on Discord.", "Wil je er niet op? Vraag het de serverbeheerder op Discord."))
        self.schrijf("privacy.html", "Privacy", "Privacy", main, "")

    def notfound(self):
        main = f'<h1>{t("Not found", "Niet gevonden")}</h1><p><a href="index.html">{t("Back to the Vadsrijk", "Terug naar het Vadsrijk")}</a></p>'
        self.schrijf("404.html", "Niet gevonden", "Not found", main, "")


EARLY_JS = ""


def _webp(src, dst, max_w=None):
    from PIL import Image
    im = Image.open(src).convert("RGBA")
    if max_w and im.width > max_w:
        im = im.resize((max_w, round(im.height * max_w / im.width)), Image.LANCZOS)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    im.save(dst, "WEBP", quality=88)
