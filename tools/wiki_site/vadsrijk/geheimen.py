"""
U-SITE round 3: every hidden thing of the Vadsrijk WITH its location, each behind its own spoiler click (user decision 2026-11-08,
replaces the old "secrets never on the map" rule).

Covered (zones 1-6): hidden knuffels, treasures, clues / torn pages / letters / tablets / reliefs, puzzles (incl. the zone-6 star
beacons and the cave puzzles), cave treasures, viewpoints and campfires. Each entry: what it is, where (area + coordinates), how to get
there (the game's own hint text where it has one), the reward, and a "Toon op kaart" link (kaart.html?x=..&z=..&geheim=1). The page
shows only "<kind> #n: klik om te onthullen" until the reader opens one. The map gets a separate layer "Geheimen tonen (spoilers!)",
OFF by default, and its markers are never in the map search.

Sources: the positions = the POI files (and the committed Z6WereldPlekken.java); which POI is which secret and its name/text = the
mod's own *Plekken.java maps (KNUFFEL, SCHAT, AANWIJZING, RELIEF, MISTBRIEVEN, FLESSENPOST, the zone-6 tablets) + the lang file;
rewards = the find handlers (NpcHaken / Z?Haken / Grotten: coins below, Episch for the one special chest per zone).
"""
import os
import re

from .pages import ESC, t, p

JAVA = ("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")
PLEKKEN = {"zone1": "npc/NpcPlekken.java", "zone2": "zone2/npc/Z2NpcPlekken.java", "zone3": "zone3/npc/Z3NpcPlekken.java",
           "zone4": "zone4/verhaal/Z4VerhaalPlekken.java", "zone5": "zone5/verhaal/Z5VerhaalPlekken.java", "zone6": "zone6/verhaal/Z6VerhaalPlekken.java"}
NS = {"zone1": "vadsrijk.npc", "zone2": "vadsrijk.z2npc", "zone3": "vadsrijk.z3npc", "zone4": "vadsrijk.z4verhaal", "zone5": "vadsrijk.z5verhaal",
      "zone6": "vadsrijk.z6verhaal"}
# map name in *Plekken.java -> kind
SOORT_VAN = {"KNUFFEL": "knuffel", "SCHAT": "schat", "AANWIJZING": "aanwijzing", "RELIEF": "relief", "MISTBRIEVEN": "brief", "FLESSENPOST": "brief",
             "TABLET": "brief", "UITKIJK": "uitkijk"}
# treasure coins per zone + the one Episch chest (NpcHaken / Z2Haken ... Z6Haken "schatten")
SCHAT_MUNTEN = {"zone1": 2500, "zone2": 6000, "zone3": 9000, "zone4": 15000, "zone5": 15000, "zone6": 20000}
SCHAT_EPISCH = {"kristalkamer", "burchtdak", "onder_de_tak", "mist", "staartvin", "mesa"}
SOORTEN = [  # kind, en, nl (headers + summaries)
    ("knuffel", "Hidden knuffels", "Verborgen knuffels"), ("schat", "Treasures", "Schatten"),
    ("aanwijzing", "Clues and traces", "Aanwijzingen en sporen"), ("relief", "Reliefs", "Reliëfs"),
    ("brief", "Letters, notes and tablets", "Brieven, briefjes en tabletten"), ("puzzel", "Puzzles and star beacons", "Puzzels en sterrenbakens"),
    ("grotschat", "Cave treasures", "Grotschatten"), ("uitkijk", "Viewpoints", "Uitkijkpunten"), ("kampvuur", "Campfires", "Kampvuren")]
ENKEL = {"knuffel": ("Hidden knuffel", "Verborgen knuffel"), "schat": ("Treasure", "Schat"), "aanwijzing": ("Clue", "Aanwijzing"),
         "relief": ("Relief", "Reliëf"), "brief": ("Letter", "Briefje"), "puzzel": ("Puzzle", "Puzzel"), "grotschat": ("Cave treasure", "Grotschat"),
         "uitkijk": ("Viewpoint", "Uitkijkpunt"), "kampvuur": ("Campfire", "Kampvuur")}


def _koords(body):
    """{x, y, z} rows -> (x, y, z); {x, z} rows (y given in the call) -> None (the POI file has the spot)."""
    return [(int(a), int(b), int(c)) if c else None for a, b, c in re.findall(r"\{(-?\d+), (-?\d+)(?:, (-?\d+))?\}", body)]


def _lees_plekken(tekst):
    """[(MAP, id, poi, (x, y, z) or None)] from the patterns the *Plekken classes use."""
    out = []
    for m in re.finditer(r'\b([A-Z_]+)\.put\("([a-z0-9_]+)", w\("([a-z0-9_]+)"(?:, (-?\d+), (-?\d+), (-?\d+))?\)', tekst):
        out.append((m[1], m[2], m[3], (int(m[4]), int(m[5]), int(m[6])) if m[4] else None))
    ints = {m[1]: _koords(m[2]) for m in re.finditer(r"int\[\]\[\] (\w+) = \{(.*?)\};", tekst, re.S)}
    strs = {m[1]: re.findall(r'"([a-z0-9_]+)"', m[2]) for m in re.finditer(r"String\[\] (\w+) = \{(.*?)\};", tekst, re.S)}
    for m in re.finditer(r'\b([A-Z_]+)\.put\((?:"([a-z_]+)" \+ \(i \+ 1\)|(\w+)\[i\]), w\("([a-z0-9_]+)" \+ \(i \+ 1\), (\w+)\[i\]\[0\]', tekst):
        k = ints.get(m[5], [])
        for i, xyz in enumerate(k):
            nid = (m[2] + str(i + 1)) if m[2] else (strs.get(m[3], [None] * len(k))[i] if i < len(strs.get(m[3], [])) else None)
            if nid:
                out.append((m[1], nid, m[4] + str(i + 1), xyz))
    consts = {m[1]: int(m[2]) for m in re.finditer(r"static final int (\w+) = (-?\d+);", tekst)}
    for m in re.finditer(r'\b([A-Z_]+)\.put\("([a-z0-9_]+)", p\((-?\d+), (-?\d+|\w+), (-?\d+)\)\)', tekst):   # zone 2's clues (no POI)
        y = int(m[4]) if m[4].lstrip("-").isdigit() else consts.get(m[4])
        if y is not None:
            out.append((m[1], m[2], "_p_" + m[2], (int(m[3]), y, int(m[5]))))
    for m in re.finditer(r'poi\("(z5_uitkijk_[a-z_]+)", w\("([a-z0-9_]+)", (-?\d+), (-?\d+), (-?\d+)\)\)', tekst):   # zone 5's viewpoints
        out.append(("UITKIJK", m[1], m[2], (int(m[3]), int(m[4]), int(m[5]))))
    for m in re.finditer(r'vul\((\w+), "([a-z_]+)", "([a-z_]+)", new int\[\]\[\] \{(.*?)\}\);', tekst, re.S):
        for i, xyz in enumerate(_koords(m[4])):
            out.append((m[1], m[2] + str(i + 1), m[3] + str(i + 1), xyz))
    for m in re.finditer(r'vondst\("z6_tabletten", "[^"]+", "([a-z_]+)", new int\[\]\[\] \{(.*?)\}\);', tekst, re.S):
        for i, xyz in enumerate(_koords(m[2])):
            out.append(("TABLET", "tablet_" + str(i + 1), m[1] + str(i + 1), xyz))
    return out


def data(bron, pois, gebieden, punten, puzzels):
    root = bron.root
    nl = bron.nl
    suffix = {}
    for k in nl:
        if k.startswith("vadsrijk."):
            suffix.setdefault(k.rsplit(".", 1)[-1], []).append(k)

    def zoek(ns, soortkey, nid):
        for k in (f"{ns}.{soortkey}.{nid}",):
            if k in nl:
                return bron.schoon(nl[k])
        for k in suffix.get(nid, []):
            if k.startswith(ns + ".") and not k.endswith((".naam", ".titel")) and len(nl[k]) > 3 and ".q." not in k and ".npc.naam" not in k:
                return bron.schoon(nl[k])
        return ""

    def waar(zone, poi, xyz):
        for cand in (poi, poi.replace("_z6_", "_"), poi.replace("geheim_z6_", "geheim_")):
            if (zone, cand) in pois:
                return list(pois[(zone, cand)][2])
        return list(xyz) if xyz else None

    def gebied(pos):
        g = bron.gebied_op(gebieden, pos[0], pos[1], pos[2]) if pos else None
        return (g["id"], bron.gebied_naam(g["id"])[0]) if g else (None, "")

    out, gezien = [], set()
    for zone, rel in PLEKKEN.items():
        try:
            tekst = open(os.path.join(root, *JAVA, *rel.split("/")), encoding="utf-8").read()
        except OSError:
            bron.notes.append(f"geheimen: {rel} missing")
            continue
        ns = NS[zone]
        for mapnaam, nid, poi, xyz in _lees_plekken(tekst):
            soort = SOORT_VAN.get(mapnaam)
            if not soort or (zone, soort, nid) in gezien:
                continue
            pos = waar(zone, poi, xyz)
            if not pos:
                continue
            gezien.add((zone, soort, nid))
            gid, gnaam = gebied(pos)
            if soort == "knuffel":
                naam = bron.t(f"{ns}.knuffel.{nid}") or f"Knuffel {nid.split('_')[-1]}"
                tekst_nl = f"Een verloren knuffel{(' in ' + gnaam) if gnaam else ''}. Hij glinstert een beetje; loop ertegenaan of rechtsklik."
                bel = "Telt mee voor de knuffelquest van deze zone en voor je Verkenningsboek."
            elif soort == "schat":
                naam = f"Schat: {gnaam}" if gnaam else "Schat"
                tekst_nl = zoek(ns, "schat", nid)
                bel = (f"Een uitrustingsstuk op jouw level ({'Episch' if nid in SCHAT_EPISCH else 'Zeldzaam'}) + "
                       f"{SCHAT_MUNTEN[zone]} brons ({SCHAT_MUNTEN[zone] // 100} zilver).")
            elif soort == "aanwijzing":
                naam = bron.t(f"{ns}.aanwijzing.naam") or "Aanwijzing"
                tekst_nl = zoek(ns, "aanwijzing", nid)
                bel = "Telt mee voor de geheime quest van deze zone (alle vijf = de ontknoping)."
            elif soort == "uitkijk":
                naam = "Uitkijkpunt " + (bron.t(f"vadsrijk.wereld.uitkijk.{nid}") or nid.split("_", 2)[-1].replace("_", " ").title())
                tekst_nl = ""
                bel = "Een panorama + een vinkje in je Verkenningsboek."
            elif soort == "relief":
                naam = bron.t(f"{ns}.relief.naam") or "Reliëf"
                tekst_nl = zoek(ns, "relief", nid)
                bel = "Telt mee voor Het Muurverhaal (alle acht samen vertellen het verhaal van het ei)."
            else:
                sub = {"MISTBRIEVEN": "mistbrief", "FLESSENPOST": "fles", "TABLET": "tablet"}[mapnaam]
                naam = bron.t(f"{ns}.{sub}.naam") or {"fles": "Flessenpost", "mistbrief": "Een briefje", "tablet": "Stenen tablet"}[sub]
                tekst_nl = zoek(ns, sub, nid)
                bel = "Een stukje van het verhaal; telt mee voor de bijbehorende quest en je Verkenningsboek."
            out.append(dict(zone=zone, soort=soort, id=f"{zone}:{soort}:{nid}", naam=naam, tekst=tekst_nl, pos=pos, gebied=gid,
                            gebied_naam=gnaam, beloning=bel))
    # puzzles: anchor POI (+dx/dz); the cave puzzles + treasures from GrotData; zone 6's star beacons
    grotten = _grotten(root)
    for pz in puzzels:
        pos, anker = None, pz.get("_anker") or {}
        plek = anker.get("plek", "")
        if anker.get("bron") == "grot":
            g = grotten.get(re.sub(r"^grot_|_puzzel$", "", plek))
            pos = list(g["puzzel"]) if g and g.get("puzzel") else None
        else:
            for (zone, pid), (_, _, xyz) in pois.items():
                if pid == plek:
                    pos = [xyz[0] + anker.get("dx", 0), xyz[1], xyz[2] + anker.get("dz", 0)]
                    break
        if not pos:
            continue
        gid, gnaam = gebied(pos)
        bel = ", ".join(x for x in ((f"{pz['munten']} brons" if pz.get("munten") else ""), (pz.get("ontgrendel") or ""),
                                    ("een bladzijde voor je Verkenningsboek" if pz.get("bladzijde") else "")) if x) or "De puzzel opent iets in de buurt."
        out.append(dict(zone=pz["zone"], soort="puzzel", id=f"{pz['zone']}:puzzel:{pz['id']}", naam=pz["naam"], tekst=pz.get("uitleg") or "",
                        pos=pos, gebied=gid, gebied_naam=gnaam, beloning=bel + (" (alleen 's nachts)" if pz.get("nacht") else "")))
    for b in ("palmbaken", "zoutbaken", "ruinebaken", "duinbaken", "oasebaken"):
        if ("zone6", b + "_puzzel") in pois:
            pos = list(pois[("zone6", b + "_puzzel")][2])
            gid, gnaam = gebied(pos)
            out.append(dict(zone="zone6", soort="puzzel", id=f"zone6:puzzel:{b}", naam=f"Sterrenbaken: {b[:-5].capitalize()}baken",
                            tekst="Volg de sterren van het sterrenbeeld in de volgorde van de hint (de Sterrenlens laat overdag de nachthemel zien).",
                            pos=pos, gebied=gid, gebied_naam=gnaam, beloning="Het baken brandt weer: een sterrenscherf en een visioen van Moeder Bron."))
    for gid_, g in grotten.items():
        if not g.get("schat"):
            continue
        zone = f"zone{g['zone']}"
        pos = list(g["schat"])
        _, gnaam = gebied(pos)
        naam = bron.t(f"vadsrijk.grotten.naam.{gid_}", gid_)
        out.append(dict(zone=zone, soort="grotschat", id=f"{zone}:grotschat:{gid_}", naam=f"Schat van {naam}",
                        tekst=f"{bron.t('vadsrijk.grotten.onder.' + gid_, '')}. Ingang: x {g['ingang'][0]}, y {g['ingang'][1]}, z {g['ingang'][2]}; de kist ligt achterin de grot."
                        if g.get("ingang") else bron.t("vadsrijk.grotten.onder." + gid_, ""),
                        pos=pos, gebied=None, gebied_naam=naam, beloning=f"Een kist met buit + {600 * g['zone'] ** 2 + 400} brons."))
    if ("zone6", "zoutgrot_schat") in pois:   # zone 6's cave lives in the map itself (u-z6wereld)
        pos = list(pois[("zone6", "zoutgrot_schat")][2])
        out.append(dict(zone="zone6", soort="grotschat", id="zone6:grotschat:zoutgrot", naam="Schat van De Zingende Zoutgrot",
                        tekst="Onder De Zoutvlakte. Zing het karavaanlied op de vijf kristallen (zijquest 'Het lied van de Zoutgrot'): dan gaat de kristalwand open.",
                        pos=pos, gebied=None, gebied_naam="De Zingende Zoutgrot", beloning="De kist van de Eerste Karavaan: het tapijtpatroon 'karavaan'."))
    # viewpoints + campfires (public on the map already; listed here with coordinates too)
    for pt in punten:
        if pt["soort"] in ("uitkijk", "kampvuur"):
            out.append(dict(zone=pt["zone"], soort=pt["soort"], id=pt["id"], naam=pt["naam"], tekst="", pos=pt["pos"], gebied=pt.get("gebied"),
                            gebied_naam=bron.gebied_naam(pt["gebied"])[0] if pt.get("gebied") else "",
                            beloning="Een panorama + een vinkje in je Verkenningsboek." if pt["soort"] == "uitkijk"
                            else "Rechtsklik: je checkpoint (hier word je wakker als je flauwvalt) + een vinkje in je Verkenningsboek."))
    volgorde = {s[0]: i for i, s in enumerate(SOORTEN)}
    out.sort(key=lambda g: (g["zone"], volgorde.get(g["soort"], 99), g["naam"]))
    return out


def _grotten(root):
    """grotten/GrotData.java -> {id: {zone, ingang, puzzel, schat}}."""
    try:
        tekst = open(os.path.join(root, *JAVA, "grotten", "GrotData.java"), encoding="utf-8").read()
    except OSError:
        return {}
    out = {}
    for m in re.finditer(r'new Grot\("([a-z]+)", (\d+),(.*?)\)\)\)', tekst, re.S):
        d = dict(zone=int(m[2]))
        for k, x, y, z in re.findall(r'Map\.entry\("([a-z_0-9]+)", new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)\)', m[3]):
            d[k] = (int(x), int(y), int(z))
        out[m[1]] = d
    return out


def pagina(b):
    """geheimen/index.html: per zone (collapsed), per kind a list of spoiler entries."""
    P = "geheimen/index.html"
    G = b.d.get("geheimen_lijst") or []
    delen = []
    for z in b.d["zones"]:
        mine = [g for g in G if g["zone"] == z["id"]]
        if not mine:
            continue
        inner = ""
        for soort, en, nl in SOORTEN:
            lijst = [g for g in mine if g["soort"] == soort]
            if not lijst:
                continue
            if soort == "puzzel" and z["nr"] < 6:   # X-SITE: the star beacons are zone 6 only
                en, nl = "Puzzles", "Puzzels"
            items = ""
            for i, g in enumerate(lijst, 1):
                x, y, zz = g["pos"]
                kaart = f'<a class="knop licht" href="../kaart.html?x={x}&amp;z={zz}&amp;geheim=1">{t("Show on map", "Toon op kaart")}</a>'
                body = (f'<p><b>{ESC(g["naam"])}</b>{(" &middot; " + ESC(g["gebied_naam"])) if g["gebied_naam"] else ""}</p>'
                        + (f'<p class="game-text">{ESC(g["tekst"])}</p>' if g["tekst"] else "")
                        + f'<p><span class="tagref">x {x}, y {y}, z {zz}</span></p>'
                        + f'<p><b>{t("Reward", "Beloning")}:</b> {ESC(g["beloning"])}</p><p>{kaart}</p>')
                en1, nl1 = ENKEL[soort]
                items += (f'<details class="spoiler geheim" id="{ESC(g["id"].replace(":", "-"))}"><summary>{t(f"{en1} #{i}: click to reveal", f"{nl1} #{i}: klik om te onthullen")}</summary>'
                          f'{body}</details>')
            inner += f'<h3>{t(en, nl)} ({len(lijst)})</h3>{items}'
        delen.append(b.zonevak(z, inner, f'{len(mine)} {t("hidden things", "verborgen dingen")}'))
    main = (f'<h1>{t("Secrets", "Geheimen")}</h1>'
            + p("Every hidden thing of the Vadsrijk WITH its location: knuffels, treasures, clues and letters, puzzles, cave treasures, viewpoints and campfires. Each one stays hidden until you click it, so you only spoil what you want. On the map they are in the layer \"Show secrets (spoilers!)\", which is off by default.",
                "Elk verborgen ding van het Vadsrijk MET de plek: knuffels, schatten, aanwijzingen en briefjes, puzzels, grotschatten, uitkijkpunten en kampvuren. Elk blijft verborgen tot je erop klikt, dus je verklapt alleen wat je zelf wilt. Op de kaart staan ze in de laag \"Geheimen tonen (spoilers!)\", die standaard uit staat.")
            + f'<p class="muted">{t("Click a zone to open it. Puzzle solutions are never on this site; quest walkthroughs are on the quest pages (spoiler button).", "Klik op een zone om hem open te klappen. Oplossingen van puzzels staan nergens op deze site; walkthroughs van quests staan op de questpagina’s (spoilerknop).")}</p>'
            + "".join(delen))
    b.schrijf(P, "Geheimen", "Secrets", main, P, beschrijving="Alle geheimen van Het Vadsrijk met hun plek, elk achter een spoilerklik.")
