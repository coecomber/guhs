"""
1.10.3: the secrets page mirrors the Verkenningsboek (key B) exactly.

The page used to have its own kinds, order and numbering ("Verborgen knuffel #3", sorted by name), which did not match the book
in the game. Now it follows the book: the runtime dump's "boek" (WikiExport.boek: zones, categories, entries in book order, the
same name/hint lang keys), numbered 1..n per category. The book shows the same numbers (BoekScherm, 1.10.3). The old secrets list
(geheimen.data) still supplies the extras per spot (game text, reward) and the map's secrets layer.
"""
import re

from .pages import ESC, t, p


def _fmt(sjabloon, args):
    """Java's %s / %1$s / %% on a lang string."""
    it = iter(args)

    def sub(m):
        if m[0] == "%%":
            return "%"
        if m[1]:
            i = int(m[1]) - 1
            return str(args[i]) if i < len(args) else ""
        return str(next(it, ""))
    return re.sub(r"%%|%(?:(\d+)\$)?s", sub, sjabloon or "")


def _tekst(bron, key, arg):
    """(nl, en) of a book name/hint: key + optional arg ("langkey#literal" = a lang key and a number, see BoekScherm.tekst)."""
    if not key:
        return "", ""
    uit = []
    for taal in (bron.nl, bron.en):
        if arg:
            if "#" in arg:
                a, n = arg.split("#", 1)
                args = [taal.get(a, bron.nl.get(a, a)), n]
            else:
                args = [taal.get(arg, bron.nl.get(arg, arg))]
        else:
            args = []
        uit.append(bron.schoon(_fmt(taal.get(key, bron.nl.get(key, key)), args)))
    return uit[0], uit[1]


def data(bron, oud):
    """[{zone, naam, en, categorieen: [{id, naam, en, later, dingen: [{nr, id, naam, en, hint, hint_en, geheim, pos, ...}]}]}]."""
    rt = (bron.rt or {}).get("boek") or []
    if not rt:
        bron.notes.append("boekspiegel: the runtime dump has no 'boek' (run the VadsrijkWeb gametest): the secrets page falls back on the old list")
    per_zone = {}
    for g in oud:
        if g["soort"] != "verborgen":   # (1.10.21: the secret quests/bosses/places are matched by their "boek" row, not by place)
            per_zone.setdefault(g["zone"], []).append(g)
    per_id = {g["id"]: g for g in oud}
    verborgen = {(g["zone"], g["boek"][1]): g for g in oud if g["soort"] == "verborgen" and g.get("boek")}

    def zoek_oud(zone, pos, rid=""):
        """1.10.21: first by id (the book's "<zN_>knuffels_<id>" = the list's "<zone>:knuffel:<id>"), then by place."""
        m = re.match(r"(?:z\d_)?(knuffels|aanwijzingen|schatten|reliefs|brieven|flessenpost)_(.+)$", rid)
        if m:
            soort = {"knuffels": "knuffel", "aanwijzingen": "aanwijzing", "schatten": "schat", "reliefs": "relief"}.get(m[1], "brief")
            g = per_id.get(f"{zone}:{soort}:{m[2]}")
            if g:
                return g
        best, bd = None, 6 ** 2
        for g in per_zone.get(zone, []):
            d = (g["pos"][0] - pos[0]) ** 2 + (g["pos"][2] - pos[2]) ** 2
            if d <= bd and abs(g["pos"][1] - pos[1]) <= 12:
                best, bd = g, d
        return best
    gecorrigeerd, erbij = [], []

    later_nl = bron.t("vadsrijk.voltooiing.cat.later", "%s (later)")
    later_en = bron.en.get("vadsrijk.voltooiing.cat.later", "%s (later)")
    out = []
    for z in rt:
        cats = []
        for c in z.get("categorieen", []):
            cn, ce = _tekst(bron, c["naam"], "")
            if c.get("later"):
                cn, ce = _fmt(later_nl, [cn]), _fmt(later_en, [ce])
            dingen = []
            for i, d in enumerate(c.get("dingen", []), 1):
                nn, ne = _tekst(bron, d["n"], d.get("na", ""))
                hn, he = _tekst(bron, d.get("h", ""), d.get("ha", ""))
                e = dict(nr=i, id=d["id"], naam=nn, en=ne, hint=hn, hint_en=he, geheim=bool(d.get("geheim")), pos=d.get("pos"))
                v = verborgen.get((z["id"], d["id"]))
                if v:   # 1.10.21: a secret boss row: its place + how to find it come from the secrets list
                    v["boek_nr"] = (c["id"], cn, ce, i)
                    e.update(pos=v["pos"], gebied_naam=v["gebied_naam"], verborgen=v["id"])
                elif e["pos"]:
                    g = zoek_oud(z["id"], e["pos"], d["id"])
                    if g:
                        if list(g["pos"]) != list(e["pos"]) and g["soort"] not in ("uitkijk", "kampvuur", "puzzel"):
                            gecorrigeerd.append(f'{g["id"]} {g["pos"]} -> {e["pos"]}')   # the book = where the game counts it
                            g["pos"] = list(e["pos"])
                        e.update(tekst=g["tekst"], beloning=g["beloning"], gebied_naam=g["gebied_naam"], lijst=g["id"])
                    elif e["geheim"]:   # a secret only the book knows (e.g. the Grauwe brief): on the map layer too
                        g = dict(zone=z["id"], soort="brief" if "brieven" in d["id"] else "uitkijk" if "uitzicht" in c["id"] or "uitkijk" in c["id"] else "schat",
                                 id=f'{z["id"]}:boek:{d["id"]}', naam=nn, tekst="", pos=list(e["pos"]), gebied=None, gebied_naam="",
                                 beloning="Een vinkje in je Verkenningsboek.")
                        oud.append(g)
                        erbij.append(g["id"])
                        e["lijst"] = g["id"]
                dingen.append(e)
            cats.append(dict(id=c["id"], naam=cn, en=ce, later=bool(c.get("later")), dingen=dingen))
        zn, ze = _tekst(bron, z["naam"], "")
        out.append(dict(zone=z["id"], naam=zn, en=ze, categorieen=cats))
    if gecorrigeerd:
        bron.notes.append(f"boekspiegel: {len(gecorrigeerd)} secret spots moved to where the book counts them: " + "; ".join(gecorrigeerd))
    if erbij:
        bron.notes.append(f"boekspiegel: {len(erbij)} book-only secrets added to the map layer: " + ", ".join(erbij))
    return out


def _rij(b, P, zone, c, e, vnr=None):
    """One book row: the summary is what the book shows (the name, or ??? + the hint for a secret); the click shows the rest."""
    nr = f'<span class="boeknr">{e["nr"]}.</span> '
    if e["geheim"]:
        zicht = f'??? <span class="muted">{t(e["hint_en"], e["hint"])}</span>'
    else:
        zicht = t(e["en"], e["naam"])
    body = ""
    if e["geheim"]:
        body += f'<p><b>{t(e["en"], e["naam"])}</b>{(" &middot; " + ESC(e["gebied_naam"])) if e.get("gebied_naam") else ""}</p>'
    elif e.get("gebied_naam"):
        body += f'<p>{ESC(e["gebied_naam"])}</p>'
    if c["id"] in ("hoofd", "zij") and e["id"] in b.q:
        body += f'<p>{t("Quest page", "Questpagina")}: {b.l_quest(e["id"], P)}</p>'
    if e.get("verborgen") and vnr and e["verborgen"] in vnr:   # 1.10.21: a secret boss: how to find him is in the secrets section
        n = vnr[e["verborgen"]]
        body += (f'<p>{t("How to find it", "Hoe je hem vindt")}: <a href="#{ESC(e["verborgen"].replace(":", "-"))}">'
                 f'{t(f"Secret #{n} below", f"Geheim #{n} hieronder")}</a></p>')
    if e.get("tekst"):
        body += f'<p class="game-text">{ESC(e["tekst"])}</p>'
    if e.get("pos"):
        x, y, zz = e["pos"]
        body += f'<p><span class="tagref">x {x}, y {y}, z {zz}</span></p>'
    if e.get("beloning"):
        body += f'<p><b>{t("Reward", "Beloning")}:</b> {ESC(e["beloning"])}</p>'
    if e.get("pos"):
        x, y, zz = e["pos"]
        body += f'<p><a class="knop licht" href="../kaart.html?x={x}&amp;z={zz}&amp;geheim=1">{t("Show on map", "Toon op kaart")}</a></p>'
    rid = ESC(f'{zone}-{c["id"]}-{e["nr"]}')
    anker = f'<span id="{ESC(e["lijst"].replace(":", "-"))}"></span>' if e.get("lijst") else ""   # (the map layer links to the list id)
    if not body:
        return f'{anker}<div class="boekrij" id="{rid}">{nr}{zicht}</div>'
    return f'{anker}<details class="spoiler geheim boekrij" id="{rid}"><summary>{nr}{zicht}</summary>{body}</details>'


def _plek(x, y, z, label=""):
    """One spot: the coordinates + a map link (secrets layer on)."""
    return (f'{label}<span class="tagref">x {x}, y {y}, z {z}</span> '
            f'<a href="../kaart.html?x={x}&amp;z={z}&amp;geheim=1">{t("map", "kaart")}</a>')


def _verborgen_rij(b, P, n, g, bazen):
    """1.10.21: one hidden quest / boss / place: ??? + a teaser; the click shows where, how to get in or start it, what you need
    and the reward."""
    from .pages import slug
    zicht = f'<span class="boeknr">{n}.</span> ??? <span class="muted">{t(g["hint"]["en"], g["hint"]["nl"])}</span>'
    body = (f'<p><b>{t(g["naam_en"], g["naam"])}</b> &middot; <span class="badge">{t(g["label"]["en"], g["label"]["nl"])}</span>'
            + (f' &middot; {ESC(g["gebied_naam"])}' if g.get("gebied_naam") else "") + '</p>')
    body += f'<p>{t(g["tekst_en"], g["tekst"])}</p>'
    x, y, zz = g["pos"]
    plekken = [f'<li>{_plek(x, y, zz, t("Here: ", "Hier: "))}</li>']
    for pl in g.get("plekken") or []:
        plekken.append(f'<li>{_plek(*pl["pos"], t(pl["label"]["en"], pl["label"]["nl"]) + ": ")}</li>')
    body += f'<ul>{"".join(plekken)}</ul>'
    links = []
    if g.get("quest") and g["quest"] in b.q:
        links.append(f'{t("Quest", "Quest")}: {b.l_quest(g["quest"], P)}')
    if g.get("baas") and g["baas"] in bazen:
        links.append(f'{t("Boss page", "Baaspagina")}: <a href="../bazen/{slug(g["baas"])}.html">{ESC(bazen[g["baas"]])}</a>')
    if g.get("pagina"):
        links.append(f'<a href="../{g["pagina"]["url"]}">{t(g["pagina"]["en"], g["pagina"]["nl"])}</a>')
    if g.get("boek_nr"):
        cid, cn, ce, nr = g["boek_nr"]
        links.append(f'{t("In your Verkenningsboek", "In je Verkenningsboek")}: '
                     f'<a href="#{ESC(g["zone"])}-{ESC(cid)}-{nr}">{t(ce, cn)} #{nr}</a>')
    body += "".join(f"<p>{x}</p>" for x in links)
    body += f'<p><b>{t("Reward", "Beloning")}:</b> {t(g["beloning_en"], g["beloning"])}</p>'
    body += f'<p><a class="knop licht" href="../kaart.html?x={x}&amp;z={zz}&amp;geheim=1">{t("Show on map", "Toon op kaart")}</a></p>'
    return f'<details class="spoiler geheim boekrij" id="{ESC(g["id"].replace(":", "-"))}"><summary>{zicht}</summary>{body}</details>'


def pagina(b):
    """geheimen/index.html: the Verkenningsboek per zone, the same categories, order and numbers as in the game; below each zone
    (1.10.21) its hidden quests, hidden bosses and secret places."""
    from . import geheimen
    B = {z["zone"]: z for z in b.d.get("boek") or []}
    if not B:
        return geheimen.pagina(b)
    P = "geheimen/index.html"
    bazen = {x["id"]: x["naam"] for x in b.d.get("bazen", [])}
    delen = []
    for z in b.d["zones"]:
        bz = B.get(z["id"])
        V = [g for g in b.d.get("geheimen_lijst") or [] if g["zone"] == z["id"] and g["soort"] == "verborgen"]
        if not bz and not V:
            continue
        vnr = {g["id"]: i for i, g in enumerate(V, 1)}
        inner, n = "", 0
        for c in (bz or {}).get("categorieen", []):
            if not c["dingen"]:
                continue
            n += 0 if c["later"] else len(c["dingen"])
            inner += (f'<h3>{t(c["en"], c["naam"])} <small class="muted">({len(c["dingen"])})</small></h3>'
                      + "".join(_rij(b, P, z["id"], c, e, vnr) for e in c["dingen"]))
        if V:
            inner += (f'<h3 id="{ESC(z["id"])}-verborgen">{t("Secret quests, bosses and places", "Geheime quests, bazen en plekken")} '
                      f'<small class="muted">({len(V)})</small></h3>'
                      f'<p class="muted">{t("Not a book category: the hidden quests, bosses without a quest and secret places of this zone. Where the book counts one, it says which row.", "Geen boekcategorie: de verborgen quests, bazen zonder quest en geheime plekken van deze zone. Telt het boek er een, dan staat erbij welke regel.")}</p>'
                      + "".join(_verborgen_rij(b, P, i, g, bazen) for i, g in enumerate(V, 1)))
        delen.append(b.zonevak(z, inner, f'{n} {t("things in the book", "dingen in het boek")}'
                               + (f' &middot; {len(V)} {t("secret quests, bosses and places", "geheime quests, bazen en plekken")}' if V else "")))
    uitleg_en = ("This page follows the Verkenningsboek (key B) exactly: per zone the same categories, in the same order, with the same "
                 "numbers. Where the book shows ??? you see the same hint here; click a row to see what it is and where it is (with "
                 "coordinates). At the bottom of each zone: its secret quests, hidden bosses and secret places, and how to get in. "
                 "Everything stays hidden until you click, so you only spoil what you want.")
    uitleg_nl = ("Deze pagina volgt het Verkenningsboek (toets B) precies: per zone dezelfde categorieën, in dezelfde volgorde, met "
                 "dezelfde nummers. Waar het boek ??? laat zien, zie je hier dezelfde hint; klik op een regel om te zien wat het is en "
                 "waar het ligt (met coördinaten). Onderaan elke zone: de geheime quests, verborgen bazen en geheime plekken, en hoe je "
                 "erin komt. Alles blijft verborgen tot je klikt, dus je verklapt alleen wat je zelf wilt.")
    sub_en = ("Click a zone to open it. On the map the secrets are in the layer \"Show secrets (spoilers!)\", off by default. "
              "Puzzle solutions are never on this site.")
    sub_nl = ("Klik op een zone om hem open te klappen. Op de kaart staan de geheimen in de laag \"Geheimen tonen (spoilers!)\", die "
              "standaard uit staat. Oplossingen van puzzels staan nergens op deze site.")
    main = (f'<h1>{t("Secrets and the Verkenningsboek", "Geheimen en het Verkenningsboek")}</h1>' + p(uitleg_en, uitleg_nl)
            + f'<p class="muted">{t(sub_en, sub_nl)}</p>' + "".join(delen))
    b.schrijf(P, "Geheimen", "Secrets", main, P, beschrijving="Het Verkenningsboek per zone met elk geheim en zijn plek, elk achter een spoilerklik.")
