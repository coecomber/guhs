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
        per_zone.setdefault(g["zone"], []).append(g)

    def zoek_oud(zone, pos):
        best, bd = None, 6 ** 2
        for g in per_zone.get(zone, []):
            d = (g["pos"][0] - pos[0]) ** 2 + (g["pos"][2] - pos[2]) ** 2
            if d <= bd and abs(g["pos"][1] - pos[1]) <= 12:
                best, bd = g, d
        return best

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
                if e["pos"]:
                    g = zoek_oud(z["id"], e["pos"])
                    if g:
                        e.update(tekst=g["tekst"], beloning=g["beloning"], gebied_naam=g["gebied_naam"])
                dingen.append(e)
            cats.append(dict(id=c["id"], naam=cn, en=ce, later=bool(c.get("later")), dingen=dingen))
        zn, ze = _tekst(bron, z["naam"], "")
        out.append(dict(zone=z["id"], naam=zn, en=ze, categorieen=cats))
    return out


def _rij(b, P, zone, c, e):
    """One book row: the summary is what the book shows (the name, or ??? + the hint for a secret); the click shows the rest."""
    nr = f'<span class="boeknr">{e["nr"]}.</span> '
    if e["geheim"]:
        zicht = f'??? <span class="muted">{t(ESC(e["hint_en"]), ESC(e["hint"]))}</span>'
    else:
        zicht = t(ESC(e["en"]), ESC(e["naam"]))
    body = ""
    if e["geheim"]:
        body += f'<p><b>{t(ESC(e["en"]), ESC(e["naam"]))}</b>{(" &middot; " + ESC(e["gebied_naam"])) if e.get("gebied_naam") else ""}</p>'
    elif e.get("gebied_naam"):
        body += f'<p>{ESC(e["gebied_naam"])}</p>'
    if c["id"] in ("hoofd", "zij") and e["id"] in b.q:
        body += f'<p>{t("Quest page", "Questpagina")}: {b.l_quest(e["id"], P)}</p>'
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
    if not body:
        return f'<div class="boekrij" id="{rid}">{nr}{zicht}</div>'
    return f'<details class="spoiler geheim boekrij" id="{rid}"><summary>{nr}{zicht}</summary>{body}</details>'


def pagina(b):
    """geheimen/index.html: the Verkenningsboek per zone, the same categories, order and numbers as in the game."""
    from . import geheimen
    B = {z["zone"]: z for z in b.d.get("boek") or []}
    if not B:
        return geheimen.pagina(b)
    P = "geheimen/index.html"
    delen = []
    for z in b.d["zones"]:
        bz = B.get(z["id"])
        if not bz:
            continue
        inner, n = "", 0
        for c in bz["categorieen"]:
            if not c["dingen"]:
                continue
            n += 0 if c["later"] else len(c["dingen"])
            inner += (f'<h3>{t(ESC(c["en"]), ESC(c["naam"]))} <small class="muted">({len(c["dingen"])})</small></h3>'
                      + "".join(_rij(b, P, z["id"], c, e) for e in c["dingen"]))
        delen.append(b.zonevak(z, inner, f'{n} {t("things in the book", "dingen in het boek")}'))
    uitleg_en = ("This page follows the Verkenningsboek (key B) exactly: per zone the same categories, in the same order, with the same "
                 "numbers. Where the book shows ??? you see the same hint here; click a row to see what it is and where it is (with "
                 "coordinates). Everything stays hidden until you click, so you only spoil what you want.")
    uitleg_nl = ("Deze pagina volgt het Verkenningsboek (toets B) precies: per zone dezelfde categorieën, in dezelfde volgorde, met "
                 "dezelfde nummers. Waar het boek ??? laat zien, zie je hier dezelfde hint; klik op een regel om te zien wat het is en "
                 "waar het ligt (met coördinaten). Alles blijft verborgen tot je klikt, dus je verklapt alleen wat je zelf wilt.")
    sub_en = ("Click a zone to open it. On the map the secrets are in the layer \"Show secrets (spoilers!)\", off by default. "
              "Puzzle solutions are never on this site.")
    sub_nl = ("Klik op een zone om hem open te klappen. Op de kaart staan de geheimen in de laag \"Geheimen tonen (spoilers!)\", die "
              "standaard uit staat. Oplossingen van puzzels staan nergens op deze site.")
    main = (f'<h1>{t("Secrets and the Verkenningsboek", "Geheimen en het Verkenningsboek")}</h1>' + p(uitleg_en, uitleg_nl)
            + f'<p class="muted">{t(sub_en, sub_nl)}</p>' + "".join(delen))
    b.schrijf(P, "Geheimen", "Secrets", main, P, beschrijving="Het Verkenningsboek per zone met elk geheim en zijn plek, elk achter een spoilerklik.")
