"""
X-SITE: the zone gate of guhs.nl/vadsrijk (`--open-zones N`, the site side of the mod's zone-gate setting).

With N < 6 the build PUBLISHES only the tutorial (Knuffelkamp) and zones 1..N. Later zones are not hidden behind the spoiler
switch, they are simply not there: no pages, no data, no JSON, no map tiles, no renders. They show up as one teaser card
"Binnenkort: nieuwe zones". Raising N later is a rebuild + publish. N = 6 builds the full site (nothing filtered).

How (in make_vadsrijk_site.bouw):
  1. filter_data(full, N)  -> the data the pages iterate over (zones, areas, NPCs, quests, creatures, items, bosses, secrets,
                              lore, guide topics, routes, puzzles, pets/mounts/powers, crafts, crystals, sets, raid).
                              The Bouwer keeps the FULL data for its look-ups, so a link to a later thing still comes out wrapped
                              in <span data-zone=M> (the spoiler markup), and step 2 removes it.
  2. strip_html(out, N)    -> removes every element with data-zone > N (+ its "hidden by your spoiler setting" note) from
                              every page. Everything the spoiler switch would hide at "zone N" is therefore gone from the files.
  3. tegels(...)           -> copies only the map tiles that touch zones <= N; the parts of a tile that belong to later zones
                              become transparent. The map frame shrinks to the open zones.
  4. ruim_beelden(out)     -> deletes every picture under img/ that no page, script or JSON references any more.
  5. later_namen(full, N)  -> the names of later-zone things (used to filter free text and by the release check zone1check.py).
"""
import copy
import json
import os
import re
import shutil
from html.parser import HTMLParser

MAX_ZONE = 6
TEASER_NL = "Binnenkort: nieuwe zones"
TEASER_EN = "Coming soon: new zones"

# Names that zone-1 GAME text itself already says (so not a site spoiler), or that collide with a zone-1 name
OOK_IN_ZONE1 = {
    "Vahoegnir": "the story premise: zone-1 quests, the zone-1 lore and the Grootmeester talk about the grey dragon",
    "De Schemerwortel": "Stalmeester Hoefje (Knuffelkamp) says you pick your Pluimguh there",
    "Bolletje": "also the name of a zone-1 cuddly toy (secret) and a zone-1 quest text",
}
GIDS_ZONE = {"rijdieren": 3}
RIJDIER_ZONE = 3   # the first mount is chosen in the Pluimstal (zone 3)   # guide topics without a zN_ prefix that talk about a later zone (same as pages.GIDS_ZONE)


def znr(z):
    try:
        return int(str(z or "zone1").replace("zone", "") or 1)
    except ValueError:
        return 1


def gids_zone(g):
    m = re.match(r"z([2-6])_", g["id"])
    return int(m[1]) if m else GIDS_ZONE.get(g["id"], 1)


# ---- names ------------------------------------------------------------------------------------------------------------------
def _namen(full):
    """(zone-1 names, {later name: zone}) from every named thing in the data."""
    lager, hoger = {}, {}

    def zet(n, zn):
        n = (n or "").strip()
        if len(n) < 5:
            return
        lager[n] = min(lager.get(n, 99), zn)
        hoger[n] = max(hoger.get(n, 0), zn)
        m = re.match(r"^(De|Het|de|het|The|the) (.{5,})$", n)   # "De Lampionkust" is also written "de Lampionkust" / "Lampionkust"
        if m:
            zet(m[2], zn)
    for k, veld in (("zones", "naam"), ("zones", "naam_en"), ("gebieden", "naam"), ("bazen", "naam"), ("wezens", "naam"), ("items", "naam"),
                    ("npcs", "naam"), ("quests", "titel"), ("lore", "titel"), ("geheimen_lijst", "naam"), ("punten", "naam")):
        for x in full.get(k, []):
            zet(x.get(veld), x["nr"] if k == "zones" else znr(x.get("zone")))
    for x in (full.get("puzzels") or {}).get("lijst", []):
        zet(x.get("naam"), znr(x.get("zone")))
    for g in full.get("gids", []):
        if re.match(r"z[2-6]_", g["id"]):   # (the unprefixed topics are generic words: "Rijdieren", "Klassen", ...)
            zet(g.get("titel"), gids_zone(g))
    for s in full.get("sets", []):
        zs = [znr(i.get("zone")) for i in full.get("items", []) if i["id"] in s.get("stukken", [])]
        zet(s.get("naam"), min(zs) if zs else 1)
    R = full.get("raid") or {}
    if R:
        zet(R.get("naam"), 6)
        zet("The Star-Dousing Tower", 6)
        for v in R.get("verdiepingen", []):
            zet(v.get("titel"), 6)
            zet(v.get("titel_en"), 6)
            for w in v.get("bazen", []):
                zet(w.get("naam"), 6)
        for t in R.get("titels", []) or []:
            zet(t, 6)
    return lager, hoger


def later_namen(full, n=1):
    """{name: 'what (zone M)'} of every thing that only exists beyond zone n (minus the allowed zone-1 story names)."""
    lager, hoger = _namen(full)
    vroeg = {x for x, z in lager.items() if z <= n}
    uit = {}
    for x, z in lager.items():
        if z > n and x not in vroeg and not any(x in a for a in vroeg) and not _ook(x):
            uit[x] = f"zone{z}"
    return uit


def _ook(x):
    """A name zone-1 game text says itself (also without its article: "Schemerwortel")."""
    return any(x == o or o.endswith(" " + x) for o in OOK_IN_ZONE1)


def patroon(namen, heel=False):
    """heel=False (tagging/filtering): any occurrence, also inside a longer word ("Honingbijvleugels"): hiding too much is safe.
    heel=True: whole words only, used for nothing but reporting."""
    if not namen:
        return None
    alt = "|".join(re.escape(x) for x in sorted(namen, key=len, reverse=True))
    return re.compile(r"(?<!\w)(?:" + alt + r")(?!\w)" if heel else alt)


def zone_namen(full):
    """{name: first zone} of every name that is not also a zone-1 name: for tagging free text (Bouwer.tekst_zone)."""
    lager, _ = _namen(full)
    vroeg = {x for x, z in lager.items() if z <= 1}
    return {x: z for x, z in lager.items() if z > 1 and x not in vroeg and not any(x in a for a in vroeg) and not _ook(x)}


def tekst_zone(pat, namen_zone, *teksten):
    if not pat:
        return 1
    return max([namen_zone[m] for t in teksten for m in pat.findall(t or "")] or [1])


# ---- data -------------------------------------------------------------------------------------------------------------------
def filter_data(full, n):
    """A deep copy of `full` with only the tutorial + zones 1..n. n >= 6: an unchanged copy."""
    d = copy.deepcopy(full)
    if n >= MAX_ZONE:
        return d
    lager, hoger = _namen(full)
    vroeg = {x for x, z in lager.items() if z <= n}
    nz = {x: z for x, z in lager.items() if z > n and x not in vroeg and not any(x in a for a in vroeg) and not _ook(x)}
    streng = {x: z for x, z in lager.items() if z > n and x not in vroeg and not any(x in a for a in vroeg)}   # incl. the story names
    spat = patroon(streng)
    pat = patroon(nz)

    def tz(*t):
        return tekst_zone(pat, nz, *t)
    ok = lambda x: znr(x.get("zone")) <= n  # noqa: E731
    d["zones"] = [z for z in d["zones"] if z["nr"] <= n]
    d["gebieden"] = [g for g in d["gebieden"] if ok(g)]
    gids_ = {g["id"] for g in d["gebieden"]}
    for z in d["zones"]:
        z["gebieden"] = [g for g in z["gebieden"] if g in gids_]
    for k in ("punten", "npcs", "quests", "wezens", "items", "bazen", "geheimen_lijst", "lore"):
        d[k] = [x for x in d.get(k, []) if ok(x)]
    d["punten"] = [p for p in d["punten"] if tz(p.get("naam")) <= n]
    d["geheimen"] = {k: v for k, v in d["geheimen"].items() if k in gids_ or (k.startswith("zone") and znr(k) <= n)}
    d["routes"] = [r for r in d["routes"] if max(znr(r.get("za")), znr(r.get("zb"))) <= n]
    d["gids"] = [g for g in d["gids"] if gids_zone(g) <= n and tz(g["titel"], g["tekst"]) <= n]
    items = {i["id"] for i in d["items"]}
    d["sets"] = [s for s in d["sets"] if all(x in items for x in s["stukken"])]
    d["raid"] = {} if n < 6 else d.get("raid")
    d["kristallen"] = [k for k in d.get("kristallen", []) if (3 if "z3" in k["item"] else 2) <= n]
    for k, velden in (("huisdieren", ("naam", "tekst", "bron")), ("rijdieren", ("naam", "tekst", "bron")), ("krachten", ("naam", "tekst"))):
        d[k] = [x for x in d.get(k, []) if tz(*(str(x.get(v) or "") for v in velden)) <= n]
    if n < RIJDIER_ZONE:   # the first mount (the Pluimguh) is chosen in zone 3: no mounts before that
        d["rijdieren"] = []
        d["rij_uiterlijk"] = {}
    for x in d["npcs"]:   # zone-1 NPCs that dream out loud about a later place ("Ooit vis ik in de Lampionkust.")
        if isinstance(x.get("praat"), list):
            x["praat"] = [r for r in x["praat"] if not (isinstance(r, str) and spat and spat.search(r))]
    am = d.get("ambachten") or {}
    if am:
        am["recepten"] = [r for r in am.get("recepten", []) if tz(r.get("naam"), " ".join(str(x[1]) for x in r.get("in_", []))) <= n]
        am["plekken"] = {a: {t: v for t, v in tiers.items() if int(t) <= n and tz(" ".join(v)) <= n} for a, tiers in am.get("plekken", {}).items()}
        am["plekken"] = {a: v for a, v in am["plekken"].items() if v}
    pz = d.get("puzzels") or {}
    if pz:
        pz["lijst"] = [x for x in pz.get("lijst", []) if ok(x) and tz(x.get("naam"), x.get("uitleg")) <= n]
    kl = d.get("kleding") or {}
    if isinstance(kl.get("dingen"), list):
        kl["dingen"] = [x for x in kl["dingen"] if tz(x.get("naam"), x.get("bron")) <= n]
    ru = d.get("rij_uiterlijk") or {}
    rij = {r["id"] for r in d["rijdieren"]}
    for k in ("vacht", "patroon", "tuig"):
        if isinstance(ru.get(k), dict):
            ru[k] = {r: v for r, v in ru[k].items() if r in rij and tz(r, " ".join(map(str, v))) <= n}
    if isinstance(ru.get("prestaties"), list):
        ru["prestaties"] = [x for x in ru["prestaties"] if tz(x) <= n and not re.search(r"zone [2-6]", x)]
    # nested references to removed things (item sources, boss loot, NPC quest lists, ...)
    weg = set()
    for k in ("gebieden", "npcs", "quests", "wezens", "items", "bazen"):
        weg |= {x["id"] for x in full.get(k, [])} - {x["id"] for x in d.get(k, [])}
    weg |= {w["kort"] for w in full.get("wezens", []) if w["id"] in weg and w.get("kort")}

    def schoon(x):
        if isinstance(x, list):
            return [schoon(y) for y in x if not (isinstance(y, str) and y in weg) and not (isinstance(y, dict) and (y.get("id") in weg or znr(y.get("zone")) > n))]
        if isinstance(x, dict):
            return {k: schoon(v) for k, v in x.items() if k not in weg}
        return x
    for k in list(d):
        if k not in ("meta", "vertaal", "zones"):
            d[k] = schoon(d[k])
    # the browser's lang keys
    vt = {}
    for k, v in (d.get("vertaal") or {}).items():
        if k.startswith("loot:") and k[5:] not in items:
            continue
        if k.startswith("arena:") and not any((b.get("arena") or {}).get("id") == k[6:] for b in d["bazen"]):
            continue
        if k.startswith("zone:") and znr(k[5:]) > n:
            continue
        if re.search(r"(^|[._:])z[2-6][a-z]*([._]|$)", k) or tz(str(v)) > n or (spat and spat.search(str(v))):
            continue
        vt[k] = v
    d["vertaal"] = vt
    d["meta"] = dict(d.get("meta") or {}, open_zones=n)
    return d


# ---- html -------------------------------------------------------------------------------------------------------------------
VOID = {"img", "br", "input", "meta", "link", "hr", "source", "wbr", "area", "col", "embed", "param", "track"}


class _Knip(HTMLParser):
    """Finds the [start, end) offsets of every element with data-zone > n (outermost only)."""

    def __init__(self, tekst, n):
        super().__init__(convert_charrefs=True)
        self.n, self.tekst = n, tekst
        self.regels = [0]
        for m in re.finditer("\n", tekst):
            self.regels.append(m.end())
        self.stack, self.knip = [], []

    def off(self):
        r, c = self.getpos()
        return self.regels[r - 1] + c

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        z = a.get("data-zone")
        weg = bool(z and z.isdigit() and int(z) > self.n)
        start = self.off()
        if tag in VOID:
            if weg and not any(w for _, w, _ in self.stack):
                self.knip.append((start, self.tekst.index(">", start) + 1))
            return
        self.stack.append((tag, weg, start))

    def handle_startendtag(self, tag, attrs):
        a = dict(attrs)
        z = a.get("data-zone")
        if z and z.isdigit() and int(z) > self.n and not any(w for _, w, _ in self.stack):
            s = self.off()
            self.knip.append((s, self.tekst.index(">", s) + 1))

    def handle_endtag(self, tag):
        if tag in VOID:
            return
        e = self.tekst.index(">", self.off()) + 1
        while self.stack:
            t, weg, s = self.stack.pop()
            if weg and not any(w for _, w, _ in self.stack):
                self.knip.append((s, e))
            if t == tag:
                break


def strip_tekst(tekst, n):
    p = _Knip(tekst, n)
    p.feed(tekst)
    p.close()
    if not p.knip:
        return tekst
    uit, i = [], 0
    for s, e in sorted(p.knip):
        if s < i:
            continue
        uit.append(tekst[i:s])
        i = e
    uit.append(tekst[i:])
    t = "".join(uit)
    t = re.sub(r'<ul class="komma"></ul>', "", t)
    return t


def strip_html(out, n):
    """Removes every element with data-zone > n from every page in `out`. Returns the number of pages changed."""
    if n >= MAX_ZONE:
        return 0
    k = 0
    for dp, _, fs in os.walk(out):
        for f in fs:
            if f.endswith(".html"):
                pth = os.path.join(dp, f)
                t = open(pth, encoding="utf-8").read()
                t2 = strip_tekst(t, n)
                if t2 != t:
                    with open(pth, "w", encoding="utf-8", newline="\n") as fh:
                        fh.write(t2)
                    k += 1
    return k


# ---- map --------------------------------------------------------------------------------------------------------------------
def kaart_vlakken(full, n, rand=24):
    """(open rectangles, closed rectangles) in blocks: areas of zones <= n (with a margin) and of zones > n."""
    info = full.get("_kaart") or {}
    x0, z0, x1, z1 = info.get("x0", -10 ** 6), info.get("z0", -10 ** 6), info.get("x1", 10 ** 6), info.get("z1", 10 ** 6)
    open_, dicht = [], []
    for g in full["gebieden"]:
        b = g["doos"]
        if b[2] < x0 or b[0] > x1 or b[3] < z0 or b[1] > z1:
            continue   # far away (e.g. an instanced valley): not on the map
        (open_ if znr(g["zone"]) <= n else dicht).append(b)
    open_ = [[b[0] - rand, b[1] - rand, b[2] + rand, b[3] + rand] for b in open_]
    return open_, dicht


def tegels(src, dst, kaart, full, n):
    """Copies the Leaflet tiles of `src` to `dst`. n < 6: only tiles that touch the open zones, with the later zones masked
    (transparent). Returns the map frame {x0, z0, x1, z1} for the page (the open zones' bounding box when n < 6)."""
    if n >= MAX_ZONE:
        shutil.copytree(src, dst, dirs_exist_ok=True)
        return None
    import numpy as np
    from PIL import Image
    open_, dicht = kaart_vlakken(dict(full, _kaart=kaart), n)
    if not open_:
        return None
    bx0, bz0 = min(b[0] for b in open_), min(b[1] for b in open_)
    bx1, bz1 = max(b[2] for b in open_), max(b[3] for b in open_)
    T = kaart.get("tegel", 256)
    for zs in os.listdir(src):
        z = int(zs)
        s = 2.0 ** z
        for xs in os.listdir(os.path.join(src, zs)):
            ix = int(xs)
            for f in os.listdir(os.path.join(src, zs, xs)):
                iy = int(f.split(".")[0])
                # block rectangle of this tile
                tx0, tz0 = ix * T / s, iy * T / s
                tx1, tz1 = (ix + 1) * T / s, (iy + 1) * T / s
                if tx1 <= bx0 or tx0 >= bx1 or tz1 <= bz0 or tz0 >= bz1:
                    continue
                im = Image.open(os.path.join(src, zs, xs, f)).convert("RGBA")
                a = np.array(im)
                xs_ = tx0 + (np.arange(T) + 0.5) / s
                zs_ = tz0 + (np.arange(T) + 0.5) / s
                X, Z = np.meshgrid(xs_, zs_)
                laat = (X >= bx0) & (X < bx1) & (Z >= bz0) & (Z < bz1)
                for b in dicht:
                    laat &= ~((X >= b[0]) & (X <= b[2]) & (Z >= b[1]) & (Z <= b[3]))
                if not laat.any():
                    continue
                a[..., 3] = np.where(laat, a[..., 3], 0)
                if a[..., 3].max() == 0:
                    continue
                d = os.path.join(dst, zs, xs)
                os.makedirs(d, exist_ok=True)
                Image.fromarray(a, "RGBA").save(os.path.join(d, f), "WEBP", quality=82, method=5)
    return dict(x0=int(bx0), z0=int(bz0), x1=int(bx1), z1=int(bz1))


# ---- pictures ---------------------------------------------------------------------------------------------------------------
def ruim_beelden(out):
    """Deletes every file under img/ (and patchnotes/img/) that no html/js/json/css file mentions by name. Returns the count."""
    tekst = []
    for dp, _, fs in os.walk(out):
        for f in fs:
            if f.endswith((".html", ".js", ".json", ".css")):
                tekst.append(open(os.path.join(dp, f), encoding="utf-8", errors="replace").read())
    alles = "\n".join(tekst)
    weg = 0
    for sub in ("img", os.path.join("patchnotes", "img")):
        r = os.path.join(out, sub)
        for dp, _, fs in os.walk(r):
            if os.path.basename(dp) == "img" and sub == "img" and dp == r:
                pass
            for f in fs:
                if f.startswith("favicon"):
                    continue
                rel = os.path.relpath(os.path.join(dp, f), out).replace("\\", "/")
                kort = rel.split("/", 1)[1] if rel.startswith("img/") else rel
                if f not in alles and kort not in alles:
                    os.remove(os.path.join(dp, f))
                    weg += 1
    for dp, ds, fs in sorted(os.walk(out), key=lambda x: -len(x[0])):
        if not os.listdir(dp):
            os.rmdir(dp)
    return weg


def teaser(t, klasse="kaartje"):
    """The one card that stands for every closed zone (no names, no pictures, no spoilers)."""
    return (f'<div class="{klasse} binnenkort"><h3>{t(TEASER_EN, TEASER_NL)}</h3>'
            f'<p>{t("More of the Vadsrijk opens up later. Until then: zone 1 is all yours.", "Later gaat er meer van het Vadsrijk open. Tot die tijd is zone 1 helemaal van jou.")}</p></div>')


def lees_json(pad):
    with open(pad, encoding="utf-8") as f:
        return json.load(f)


# ---- the main Guhs wiki (/wiki/) -------------------------------------------------------------------------------------------
def _sl(x):
    return re.sub(r"[^a-z0-9]+", "_", x.lower()).strip("_")


def filter_wiki(site, root, n):
    """X-SITE: the main wiki makes its own pages for every vr_ item/block/creature and every Vadsrijk area (from the mod's lang +
    assets). With n < 6 the pages of Vadsrijk things beyond zone n are dropped before rendering; index pages, the search index
    and the sitemap follow, and links to dropped pages are removed from the pages that stay. Checked by
    zone1check --release --dir docs/site/wiki. Returns the dropped page ids.
    Which pages: id with vr_/vadsrijk (zone = vr_zN, a later name in the title/text, or a later slug in the id), or any page
    whose id names a later zone/area ("hoogvads_vaandel")."""
    if n >= MAX_ZONE:
        return []
    from .bron import Bron
    full = Bron(root).build()
    nz = {x: znr(z) for x, z in later_namen(full, n).items()}
    for x in OOK_IN_ZONE1:   # a page ABOUT the Schemerwortel is a zone-3 page, even if zone-1 text may name it
        for k, z in _namen(full)[0].items():
            if (k == x or x.endswith(" " + k)) and z > n and x != "Vahoegnir" and x != "Bolletje":
                nz[k] = z
    pat = patroon(nz)
    slugs = {}
    for x, z in nz.items():
        if len(_sl(x)) >= 6:
            slugs[_sl(x)] = z
    for k in ("bazen", "wezens", "items", "npcs", "quests", "gebieden"):
        for x in full.get(k, []):
            zx = znr(x.get("zone"))
            if zx > n:
                for i in (x.get("id"), x.get("kort")):
                    if i and len(i) >= 6:
                        slugs.setdefault(_sl(re.sub(r"^z[2-6]_", "", i)), zx)
    plek = {}
    plek.update({_sl(re.sub(r"^(de|het|the) ", "", (g.get("naam") or "").lower())): znr(g["zone"]) for g in full["gebieden"] if znr(g["zone"]) > n})
    plek.update({_sl(re.sub(r"^(de|het|the) ", "", (z.get(k) or "").lower())): z["nr"] for z in full["zones"] if z["nr"] > n for k in ("naam", "naam_en")})
    plek = {k: v for k, v in plek.items() if len(k) >= 6}

    amb = {}   # crafting things: the lowest recipe tier (tier = zone) that makes or uses them
    for r in (full.get("ambachten") or {}).get("recepten", []):
        for x in [r.get("naam"), r.get("doel")] + [i[1] for i in r.get("in_", [])]:
            if x:
                amb[_sl(str(x))] = min(amb.get(_sl(str(x)), 99), int(r.get("tier") or 1))

    def in_id(tabel, low):
        return [z for k, z in tabel.items() if re.search(r"(^|_)" + re.escape(k) + r"(_|$)", low)]
    weg = []
    for pid, pg in list(site.pages.items()):
        low = _sl(pid.split("/", 1)[-1])
        z = max(in_id(plek, low) or [0])
        if "vr_" in pid.lower() or "vadsrijk" in pid.lower():
            m = re.search(r"vr_z([2-6])", pid.lower())
            z = max(z, int(m[1]) if m else 1, max(in_id(slugs, low) or [1]))
            # Y-INT: the FTB pronkstukken of round Y (vadsrijk_trofee_zoneN / _geheimN, _raid = zone 6): their names carry zone names
            mt = re.search(r"vadsrijk_trofee_(?:zone|geheim)([1-6])", pid.lower())
            z = max(z, int(mt[1]) if mt else 1, 6 if "vadsrijk_trofee_raid" in pid.lower() else 1)
            tekst = " ".join([pg.title or "", pg.title_en or "", pg.lead_nl or "", pg.lead_en or ""]
                             + [str(v) for _, _, v in pg.infobox] + [x[3] for x in pg.sections])
            if pat:
                z = max([z] + [nz[x] for x in pat.findall(tekst)])
            if "vr_amb_" in pid.lower():
                ta = [amb[k] for k in (_sl(pg.title or ""), re.sub(r"^(drank|eten)_", "", low.replace("vr_amb_", ""))) if k in amb]
                if ta:
                    z = max(z, min(ta))
        if z > n:
            weg.append(pid)
    for pid in weg:
        del site.pages[pid]
    if weg:
        alt = "|".join(re.escape(p) for p in weg)
        rx_li = re.compile(r'<li[^>]*>\s*<a href="@@(?:' + alt + r')@@"[^>]*>.*?</a>[^<]*</li>', re.S)
        rx_a = re.compile(r'(?:,\s*)?<a href="@@(?:' + alt + r')@@"[^>]*>.*?</a>', re.S)

        def schoon(h):
            if not h or "@@" not in h:
                return h
            h = rx_a.sub("", rx_li.sub("", h))
            return re.sub(r"^\s*,\s*", "", h)
        for pg in site.pages.values():
            pg.related = [r for r in pg.related if r not in weg]
            if pg.parent in weg:
                pg.parent = None
            pg.lead_nl, pg.lead_en = schoon(pg.lead_nl), schoon(pg.lead_en)
            pg.infobox = [(a, b, schoon(v) if isinstance(v, str) else v) for a, b, v in pg.infobox]
            pg.infobox = [x for x in pg.infobox if x[2] not in ("", None)]
            pg.sections = [(k, a, b, schoon(h)) for k, a, b, h in pg.sections]
    return weg
