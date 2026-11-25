"""
Wiki 1.10.18: ambachten/index.html, the Ambachten levelling guide (spoilers on purpose: players look everything up here).

Per ambacht (Verzamelen, Vissen, Smeden, Koken) a route per tier band (1-11, 12-21, 22-31, 32-39, 40-47, 48+): what unlocks, where
to go (zone + area + spot group with coordinates and map links), the xp per action; every spot group per zone with its positions;
the story craft steps (AmbachtVerhaal.KETENS) and the other quest/story steps; the recipes per level.

Truth = the Java on this branch, read with regexes (like geheimen.py): AmbachtXp (tiers, curve, xp), AmbachtPlekken + EilandRegistratie
+ Zone6Haken + grotten/GrotData (spot groups and their POI anchors), the *WereldPlekken POI files (anchor positions), Materialen,
Recepten (via bron.py's data["ambachten"]), AmbachtVerhaal (chains, story recipes, tracker spots), AmbachtNpcs/HoogvadsGilde (the
"Vertel eens" story levels), Imkeren, Werkplaats. The spot POSITIONS come from a live server scan, converted once to
docs/wiki/vadsrijk_ambacht_plekken.json (group id -> [[x, y, z], ...]); a group missing there falls back to its anchor.

Zone gate: everything of zone N > 1 carries data-zone="N" (the spoiler switch, and the public build strips data-zone > --open-zones);
zone-6 things are left out completely when zone 6 is not open.
"""
import json
import math
import os
import re

from .pages import ESC, t, p, spoiler, zbadge

P = "ambachten/index.html"
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")
POI_BESTANDEN = {1: "wereld/WereldPlekken.java", 2: "zone2/wereld/Z2WereldPlekken.java", 3: "zone3/wereld/Z3WereldPlekken.java",
                 4: "zone4/wereld/Z4WereldPlekken.java", 5: "zone5/wereld/Z5WereldPlekken.java", 6: "zone6/wereld/Z6WereldPlekken.java"}
PLEKKEN_JSON = os.path.join("docs", "wiki", "vadsrijk_ambacht_plekken.json")
AMBACHTEN = [("verzamelen", "Gathering", "Verzamelen"), ("vissen", "Fishing", "Vissen"), ("smeden", "Smithing", "Smeden"), ("koken", "Cooking", "Koken")]
AMB_EN = {a: en for a, en, _ in AMBACHTEN}
AMB_NL = {a: nl for a, _, nl in AMBACHTEN}
SOORT_AMB = {"ERTS": "verzamelen", "HOUT": "verzamelen", "KRUID": "verzamelen", "KORF": "verzamelen", "VIS": "vissen", "DUIK": "vissen"}
SOORT = {"ERTS": ("Ore", "Erts"), "HOUT": ("Wood", "Hout"), "KRUID": ("Herbs", "Kruiden"), "KORF": ("Beehives", "Bijenkorven"),
         "VIS": ("Fishing", "Vissen"), "DUIK": ("Diving", "Duiken")}
MINI = {"ERTS": ("tap three times on the pink", "drie keer tikken op het roze"), "HOUT": ("tap twice, quickly", "twee keer snel tikken"),
        "KRUID": ("hold, let go in the green", "vasthouden, loslaten in het groen"), "VIS": ("wait for BITE! and click", "wacht op BEET! en klik"),
        "DUIK": ("dive down to the coral", "duik naar het koraal"), "KORF": ("smoke, then harvest the combs", "rook, dan de raten oogsten")}
WANNEER = {"": ("always", "altijd"), "nacht": ("at night", "'s nachts"), "regen": ("in the rain", "bij regen"), "duik": ("diving", "duiken")}
UIT = {"eten": ("Dish", "Gerecht"), "drank": ("Potion", "Drankje"), "item": ("Potion", "Drankje"), "sieraad": ("Maatwerk jewellery", "Maatwerk-sieraad"),
       "meester": ("Masterwork jewellery", "Meesterwerk"), "verhaal": ("Story item", "Verhaalvoorwerp"), "slijp": ("Cut crystals", "Kristal slijpen"),
       "kern": ("Boss core", "Bazenkern"), "post": ("Decoration (sent home)", "Versiering (naar huis)")}
# secret spots (the Java says so): their positions sit behind a spoiler click, like the secrets page
GEHEIM_GROEPEN = {"z1_vis_druipmeer", "z2_mijn_ijsgrot"}
BAND_TIP = {   # short route tips per (ambacht, tier); the data rows follow
    ("verzamelen", 1): ("Start on the Ambachtseiland, right next to Knuffelkamp: every kind has 10 spots there and a harvested spot is full again after 20-30 seconds (elsewhere 1-2 minutes). Do the islet's lesson first: the master's first story step gives 3 of every tier-1 material.",
                        "Begin op het Ambachtseiland, vlak naast Knuffelkamp: elke soort heeft daar 10 plekken en een geoogste plek is na 20-30 seconden weer vol (elders 1-2 minuten). Doe eerst de les op het eiland: de eerste verhaalstap van de leermeester geeft 3 van elk tier-1-materiaal."),
    ("vissen", 1): ("The Visvijver and Vissteiger on the Ambachtseiland refill in 20-30 seconds: the fastest start. At night and in the rain other fish bite (35% chance each).",
                    "De Visvijver en de Vissteiger op het Ambachtseiland zijn na 20-30 seconden weer vol: de snelste start. 's Nachts en bij regen bijten andere vissen (elk 35% kans)."),
    ("verzamelen", 3): ("Tier 3 has three hive meadows of its own, and you can also find tier-3 spots in the caves of the later zones.",
                        "Tier 3 heeft drie eigen bijenweides, en je vindt tier-3-plekken ook in de grotten van de latere zones."),
    ("vissen", 4): ("From level 32 you can dive: the dive spots give Roze Koraal and Zeesponsjes (and now and then a Parelschelp).",
                    "Vanaf niveau 32 kun je duiken: de duikplekken geven Roze Koraal en Zeesponsjes (en soms een Parelschelp)."),
}


# ---- reading the Java ------------------------------------------------------------------------------------------------------------
def _java(root, rel):
    try:
        return open(os.path.join(root, JAVA, rel), encoding="utf-8").read()
    except OSError:
        return ""


def _ints(tekst, naam, standaard):
    m = re.search(naam + r"\s*=\s*\{([^}]*)\}", tekst)
    return [int(x) for x in re.findall(r"-?\d+", m[1])] if m else standaard


def _num(tekst, naam, standaard):
    m = re.search(r"\b" + naam + r"\s*=\s*(-?[\d.]+)", tekst)
    return (float(m[1]) if "." in m[1] else int(m[1])) if m else standaard


class Bron:
    """Everything the guide needs, from the repo (no runtime dump needed)."""

    def __init__(self, root):
        self.root = root
        self.notes = []
        lang = os.path.join(root, "src", "main", "resources", "assets", "guhs", "lang")
        self.nl = json.load(open(os.path.join(lang, "nl_nl.json"), encoding="utf-8"))
        self.en = json.load(open(os.path.join(lang, "en_us.json"), encoding="utf-8"))
        xp = _java(root, "ambacht/AmbachtXp.java")
        self.tier_min = _ints(xp, "TIER_MIN", [0, 1, 12, 22, 32, 40, 48])
        self.verzamel = _ints(xp, "VERZAMEL", [0, 30, 52, 62, 75, 90, 105])
        self.schaal = _num(xp, "SCHAAL", 0.33)
        self.te_hoog = _num(xp, "TE_HOOG", 5)
        self.cap = _num(xp, "CAP", 60)
        self.max_tier = _num(xp, "MAX_TIER", 6)
        imk = _java(root, "ambacht/Imkeren.java")
        self.raat_min, self.max_raten, self.per_zone = _num(imk, "RAAT_MINUTEN", 10), _num(imk, "MAX_RATEN", 8), _num(imk, "PER_ZONE", 4)
        vz = _java(root, "ambacht/Verzamelen.java")
        self.zeldzaam_kans, self.zeldzaam_mat = _num(vz, "ZELDZAAM_KANS", 0.05), _num(vz, "ZELDZAAM_MATERIAAL", 0.35)
        self.verhaal_levels = _ints(_java(root, "ambacht/AmbachtNpcs.java"), "VERHAAL_LEVELS", [1, 6, 12, 22, 32])
        gilde = _java(root, "ambacht/HoogvadsGilde.java")
        gl = _ints(gilde, "LEVELS", [40, 48, 54, 60])
        self.gilde_levels = gl if re.search(r"MET_ZONE6\s*=\s*true", gilde) else gl[:2]
        self.poi = self._pois()
        self.materialen = self._materialen()
        self.groepen = self._groepen()
        self.werkplaatsen = self._werkplaatsen()
        av = _java(root, "ambacht/AmbachtVerhaal.java")
        self.ketens = [dict(id=m[1], zone=int(m[2]), quests=re.findall(r'"(\w+)"', m[3]), volgende=m[4])
                       for m in re.finditer(r'new Keten\("(\w+)", (\d), List\.of\(([^)]*)\), "(\w+)"\)', av)]
        self.tracker = {m[1]: self.plek(int(m[2]), m[3]) for m in re.finditer(r'poi\("(\w+)", (\d), "([\w.]+)"\);', av)}
        self.verhaal_recepten = {}
        for m in re.finditer(r'verhaalRecept\("(\w+)", Ambacht\.(\w+), (\d+), (\d+),[^;]*?((?:Recepten\.ingredient\(\d+, "\w+"\)(?:, )?)+)\);', av, re.S):
            ingr = [[int(a), [b_]] for a, b_ in re.findall(r'ingredient\((\d+), "(\w+)"\)', m[5])]
            self.verhaal_recepten[m[1]] = dict(id=m[1], ambacht=m[2].lower(), level=int(m[3]), tier=int(m[4]), soort="verhaal", doel=m[1], aantal=1, in_id=ingr)
        m = re.search(r'Recepten\.ALLE\.put\("klein_vadsdrankje", new Recepten\.Recept\("klein_vadsdrankje", Ambacht\.(\w+), (\d+), (\d+),\s*List\.of\(Recepten\.ingredient\((\d+), "(\w+)"\)\), "item:klein_vadsdrankje", (\d+)\)', av)
        self.klein = dict(id="klein_vadsdrankje", ambacht=m[1].lower(), level=int(m[2]), tier=int(m[3]), soort="item", doel="klein_vadsdrankje",
                          aantal=int(m[6]), in_id=[[int(m[4]), [m[5]]]]) if m else None
        rc = _java(root, "ambacht/Recepten.java")
        self.lijsten = {m[1]: re.findall(r'"(\w+)"', m[2]) for m in re.finditer(r"\b([A-Z][A-Z0-9_]*) = List\.of\(([^)]*)\)", rc)}
        self.meester_level = _ints(rc, "MEESTER_LEVEL", [0, 8, 18, 28, 37, 46, 54])
        self.quests = {}
        npc = os.path.join(root, "src", "main", "resources", "data", "guhs", "vadsrijk", "npc")
        for f in sorted(os.listdir(npc)) if os.path.isdir(npc) else []:
            if f.startswith("quests") and f.endswith(".json"):
                d = json.load(open(os.path.join(npc, f), encoding="utf-8"))
                for q in d["quests"] if isinstance(d, dict) else d:
                    self.quests[q["id"]] = q
        try:
            self.live = json.load(open(os.path.join(root, PLEKKEN_JSON), encoding="utf-8"))
        except (OSError, ValueError):
            self.live = {}
            self.notes.append(f"ambachtgids: {PLEKKEN_JSON} missing: every group shows its anchor only")
        for g in self.groepen:
            la = (self.live.get("_anker") or {}).get(g["id"])
            if la and g["anker"] and la != g["anker"]:
                self.notes.append(f"ambachtgids: live scan of {g['id']} was made for anchor {la}, the Java anchor is {g['anker']} (positions may be stale)")

    # -- lookups
    def nl_(self, k, d=None):
        return self.nl.get(k, d if d is not None else k)

    def en_(self, k, d=None):
        return self.en.get(k) or self.nl.get(k) or (d if d is not None else k)

    def plek(self, zone, pid):
        if pid.startswith("grot_"):
            return self.poi.get(0, {}).get(pid)
        return self.poi.get(zone, {}).get(pid)

    def _pois(self):
        out = {}
        for z, rel in POI_BESTANDEN.items():
            out[z] = {m[1]: [int(m[2]), int(m[3]), int(m[4])]
                      for m in re.finditer(r'Map\.entry\("([\w.]+)", new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)\)', _java(self.root, rel))}
        grot = _java(self.root, "grotten/GrotData.java")
        out[0] = {}
        self.grotten = {}
        for blok in grot.split("new Grot(")[1:]:
            m = re.match(r'"(\w+)", (\d),', blok)
            if not m:
                continue
            self.grotten[m[1]] = int(m[2])
            for e in re.finditer(r'Map\.entry\("(\w+)", new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)\)', blok):
                out[0][f"grot_{m[1]}_{e[1]}"] = [int(e[2]), int(e[3]), int(e[4])]
        return out

    def _materialen(self):
        tekst = _java(self.root, "ambacht/Materialen.java")
        out = {}
        for m in re.finditer(r'\bm\("(\w+)", NodeSoort\.(\w+), (\d+), 0x', tekst):
            out[m[1]] = dict(id=m[1], soort=m[2], tier=int(m[3]), wanneer="")
        for m in re.finditer(r'\bv\("(\w+)", (\d+), 0x[0-9A-Fa-f]+, "(\w*)"\)', tekst):
            out[m[1]] = dict(id=m[1], soort="DUIK" if m[3] == "duik" else "VIS", tier=int(m[2]), wanneer=m[3])
        if not out:
            self.notes.append("ambachtgids: no materials parsed from Materialen.java")
        return out

    def _groepen(self):
        ap = _java(self.root, "ambacht/AmbachtPlekken.java")
        lmin, lmax = _num(ap, "LEEG_MIN", 60), _num(ap, "LEEG_MAX", 120)
        out = []

        def voeg(gid, zone, poi, soort, tier, aantal, straal, honing="", leeg=(lmin, lmax), bron="ap"):
            out.append(dict(id=gid, zone=zone, poi=poi, soort=soort, tier=tier, aantal=aantal, straal=straal, honing=honing, leeg=leeg, bron=bron,
                            anker=self.plek(zone, poi)))
        for m in re.finditer(r'\bg\("(\w+)", (\d), "([\w.]+)", NodeSoort\.(\w+), (\d), (\d+), (\d+), Zoek\.\w+\);', ap):
            voeg(m[1], int(m[2]), m[3], m[4], int(m[5]), int(m[6]), int(m[7]))
        for m in re.finditer(r'\bkorf\("(\w+)", (?:(\d), )?"([\w.]+)", "(\w+)"\);', ap):
            z = int(m[2]) if m[2] else 3
            voeg(m[1], z, m[3], "KORF", z, 4, 36, honing=m[4])
        el = _java(self.root, "ambacht/EilandRegistratie.java")
        el_poi = dict(re.findall(r'"(eiland_\w+)",\s+"(ambacht_[\w.]+)"', el))
        emin, emax = _num(el, "LEEG_MIN", 20), _num(el, "LEEG_MAX", 30)
        honing = (re.search(r'String HONING = "(\w+)"', el) or [None, "klaverhoning"])[1]
        for m in re.finditer(r'AmbachtPlekken\.groep\("(\w+)", (\d), GROEPEN\.get\("\w+"\), NodeSoort\.(\w+), (\d+), (\d+), (\d+), LEEG_MIN, LEEG_MAX\)', el):
            voeg(m[1], int(m[2]), el_poi.get(m[1], ""), m[3], int(m[4]), int(m[5]), int(m[6]), leeg=(emin, emax), bron="eiland")
        for m in re.finditer(r'AmbachtPlekken\.korfGroep\("(\w+)", (\d), GROEPEN\.get\("\w+"\), (\d+), HONING, (\d+), (\d+)\)', el):
            voeg(m[1], int(m[2]), el_poi.get(m[1], ""), "KORF", int(m[3]), int(m[4]), int(m[5]), honing=honing, bron="eiland")
        z6 = _java(self.root, "ambacht/Zone6Haken.java")
        t6 = _num(z6, "TIER", 6)
        for m in re.finditer(r'new Plek\("(\w+)", NodeSoort\.(\w+), "(\w+)", (\d+), (\d+)\)', z6):
            voeg(m[1], 6, m[3], m[2], t6, int(m[4]), int(m[5]))
        mk = re.search(r'KORF_GROEP = "(\w+)", KORF_POI = "(\w+)"', z6)
        if mk:
            voeg(mk[1], 6, mk[2], "KORF", t6, 4, 36, honing=(re.search(r'String HONING = "(\w+)"', z6) or [None, ""])[1])
        gd = _java(self.root, "grotten/GrotData.java")
        for m in re.finditer(r'new Amb\("(\w+)", "(\w+)", (\d), "(\w+)", (\d), (\d+), (\d+), "\w+"\)', gd):
            voeg(f"grot_{m[1]}_{m[2][m[2].rfind('amb'):]}", int(m[3]), m[2], m[4], int(m[5]), int(m[6]), int(m[7]), bron="grot")
        for g in out:
            if not g["anker"]:
                self.notes.append(f"ambachtgids: no anchor position for group {g['id']} (POI {g['poi']})")
        return out

    def _werkplaatsen(self):
        """[(npc, zone, poi, ambacht or None)] of AmbachtPlekken.w(...) + EilandRegistratie's werkplaats(...)."""
        out = [(m[1], int(m[2]), m[3], m[4].lower() if m[4] != "null" else None)
               for m in re.finditer(r'\bw\("(\w+)", (\d), "([\w.]+)", (?:Ambacht\.)?(\w+)\);', _java(self.root, "ambacht/AmbachtPlekken.java"))]
        el = _java(self.root, "ambacht/EilandRegistratie.java")
        meesters = dict(re.findall(r'"(amb_\w+_eiland)",\s+"(ambacht_[\w.]+)"', el))
        for m in re.finditer(r'AmbachtPlekken\.werkplaats\("(\w+)", (\d), (?:MEESTERS\.get\("\w+"\)|"([\w.]+)"), Ambacht\.(\w+), (?:true|false)\)', el):
            out.append((m[1], int(m[2]), m[3] or meesters.get(m[1], ""), m[4].lower()))
        return out


# ---- the xp curve (AmbachtXp) --------------------------------------------------------------------------------------------------
def _xp_voor(br, level):
    return max(1, int(math.floor((40 + 25 * max(1, level) ** 1.6) * br.schaal + 0.5)))


def _totaal(br, level):
    return sum(_xp_voor(br, lv) for lv in range(1, level))


def _tier_top(br, tier):
    return br.cap if tier >= br.max_tier else br.tier_min[tier + 1] - 1


def _maak_xp(br, r):
    eenheden = 3 if r["soort"] == "slijp" else sum(n for n, _ in r["in_id"])
    tier = max(1, min(len(br.verzamel) - 1, r["tier"]))
    return round(0.7 * eenheden * br.verzamel[tier])


# ---- the page -----------------------------------------------------------------------------------------------------------------
class Gids:
    def __init__(self, b):
        self.b = b
        self.br = Bron(b.root)
        self.open = b.open_zones
        for n in self.br.notes:
            b.notities.append(n)

    # -- small helpers
    def dz(self, zone):
        """The data-zone attribute (zone 1: none)."""
        return f' data-zone="{zone}"' if zone > 1 else ""

    def toon(self, zone):
        return zone <= self.open

    def mat(self, mid):
        """(en, nl) name of a material / core / story item."""
        for k in ("item.guhs.vr_amb_" + mid, "item.guhs.vr_amb_kern_" + mid, "item.guhs.vr_amb_verhaal_" + mid, "item.guhs.vr_" + mid):
            if k in self.br.nl:
                return self.br.en_(k), self.br.nl[k]
        return mid.replace("_", " "), mid.replace("_", " ")

    def matnaam(self, mid):
        return t(*self.mat(mid))

    def groepnaam(self, g):
        k = "vadsrijk.ambacht.groep." + g["id"]
        en, nl = self.br.en_(k, g["id"]), self.br.nl_(k, g["id"])
        return t(en[:1].upper() + en[1:], nl[:1].upper() + nl[1:])

    def zone_van(self, g):
        return max(g["zone"], g["tier"])

    def gebied(self, g):
        """The smallest area of the group's zone round its anchor (cave areas count their y range)."""
        a = g["anker"]
        if not a:
            return None
        beste = None
        for ge in self.b.alle["gebieden"]:
            if ge["zone"] != f"zone{g['zone']}" or ge["id"].endswith("ring") or ge["id"] in ("z4_zee", "z5_moeder_wolkwal", "z6_zijdeland"):
                continue
            x0, z0, x1, z1 = ge["doos"]
            lg = ge.get("laag") or {}
            if not (x0 <= a[0] <= x1 and z0 <= a[2] <= z1):
                continue
            if lg.get("min_y") is not None and not (lg["min_y"] - 4 <= a[1] <= lg.get("max_y", 999) + 4):
                continue
            opp = (x1 - x0) * (z1 - z0)
            if beste is None or opp < beste[0]:
                beste = (opp, ge)
        return beste[1] if beste else None

    def l_gebied(self, g):
        ge = self.gebied(g)
        if not ge:
            return ""
        return f'<a href="../kaart.html?plek={ESC(ge["id"])}">{t(ge.get("naam_en") or ge["naam"], ge["naam"])}</a>'

    def kaart(self, pos):
        if not pos:
            return '<span class="muted">-</span>'
        return self.b.l_kaart(pos, P)

    def posities(self, g):
        """(list, from live?)"""
        live = self.br.live.get(g["id"])
        if live:
            return live, True
        return ([g["anker"]] if g["anker"] else []), False

    def nodig(self, g):
        """The level a group needs (hives: Imkeren.korfLevel, one tier softer from zone 4 on)."""
        if g["soort"] == "KORF":
            return 1 if g["tier"] <= 3 else self.br.tier_min[g["tier"] - 2]
        return self.br.tier_min[g["tier"]]

    def geeft(self, g):
        """What a group gives (material links)."""
        if g["soort"] == "KORF":
            ids = [g["honing"], "bijenwas"]
        else:
            ids = [m["id"] for m in self.br.materialen.values() if m["soort"] == g["soort"] and m["tier"] == g["tier"]]
            if g["soort"] == "HOUT" and g["tier"] == 3 and "roze_hars" not in ids:
                ids.append("roze_hars")
        delen = []
        for i in ids:
            m = self.br.materialen.get(i, {})
            w = m.get("wanneer")
            delen.append(self.matnaam(i) + (f' <small class="muted">({t(*WANNEER[w])})</small>' if w and w != "duik" else ""))
        return ", ".join(delen)

    def waar(self, g, met_gebied=True):
        """Zone badge + area + the anchor with a map link (a secret group: behind a spoiler click)."""
        z = self.b.z.get(f"zone{g['zone']}")
        zb = zbadge(f"zone{g['zone']}", z["naam"]) if z else ""
        geb = self.l_gebied(g) if met_gebied else ""
        k = self.kaart(g["anker"])
        if g["id"] in GEHEIM_GROEPEN:
            k = spoiler(k, "Secret spot: click to show", "Geheime plek: klik om te kijken")
        return f'{zb} {geb}<br>{k}'

    def tabel(self, koppen, rijen):
        return ('<div class="tscroll"><table class="data"><thead><tr>' + "".join(f"<th{' class=num' if n else ''}>{k}</th>" for k, n in koppen)
                + "</tr></thead><tbody>" + "".join(rijen) + "</tbody></table></div>")

    def band_kop(self, tier):
        lo = self.br.tier_min[tier]
        hi = _tier_top(self.br, tier)
        lv = f"{lo}+" if tier >= self.br.max_tier else f"{lo}-{hi}"
        return t(f"Level {lv} (tier {tier})", f"Niveau {lv} (tier {tier})")

    def band(self, aid, tier, inner):
        """One tier band of an ambacht: a hidden-by-spoiler block when tier > 1."""
        if not self.toon(tier):
            return ""
        kop = f'<h3 id="{aid}-t{tier}">{self.band_kop(tier)}</h3>'
        if tier <= 1:
            return kop + inner
        note = t(f"Tier {tier} is hidden by your spoiler setting (zone {tier}).", f"Tier {tier} is verborgen door je spoilerinstelling (zone {tier}).")
        return f'<div data-zone="{tier}">{kop}{inner}</div><div class="verstopt-zone" data-zone="{tier}">{note}</div>'

    # -- xp
    def xp_regel(self, tier):
        br = self.br
        xp = br.verzamel[tier]
        half = _tier_top(br, tier) + br.te_hoog + 1
        tekst_en = f"{xp} xp per harvest"
        tekst_nl = f"{xp} xp per oogst"
        if half <= br.cap:
            tekst_en += f" (half from level {half})"
            tekst_nl += f" (de helft vanaf niveau {half})"
        return t(tekst_en, tekst_nl)

    def xp_tabel(self):
        br = self.br
        rijen = []
        for tier in range(1, br.max_tier + 1):
            if not self.toon(tier):
                continue
            lo, hi = br.tier_min[tier], _tier_top(br, tier)
            nodig = _totaal(br, min(br.cap, hi + 1) if tier < br.max_tier else br.cap) - _totaal(br, lo)
            half = hi + br.te_hoog + 1
            maak = round(0.7 * br.verzamel[tier])
            rijen.append(f'<tr{self.dz(tier)}><td class="num">{tier}</td><td>{lo}{"-" + str(hi) if tier < br.max_tier else "-" + str(br.cap)}</td>'
                         f'<td class="num">{br.verzamel[tier]}</td><td class="num">{half if half <= br.cap else "-"}</td>'
                         f'<td class="num">{nodig:,}</td><td class="num">~{math.ceil(nodig / br.verzamel[tier]):,}</td><td class="num">{maak}</td></tr>'.replace(",", "."))
        return self.tabel([("Tier", 1), (t("Levels", "Niveaus"), 0), (t("Xp per harvest", "Xp per oogst"), 1), (t("Half xp from level", "Halve xp vanaf niveau"), 1),
                           (t("Xp for the band", "Xp voor de band"), 1), (t("Harvests (no bonus)", "Oogsten (zonder bonus)"), 1),
                           (t("Making: xp per ingredient", "Maken: xp per ingrediënt"), 1)], rijen)

    # -- gathering / fishing bands
    def groep_rijen(self, groepen, met_soort=True):
        rijen = []
        for g in groepen:
            z = self.zone_van(g)
            if not self.toon(z):
                continue
            n = g["aantal"]
            leeg = f'{g["leeg"][0]}-{g["leeg"][1]} s' if g["soort"] != "KORF" else t(f"{self.br.raat_min} min per comb", f"{self.br.raat_min} min per raat")
            rijen.append(f'<tr{self.dz(z)}><td><b>{self.groepnaam(g)}</b>' + (f'<br><small class="muted">{t(*SOORT[g["soort"]])}</small>' if met_soort else "")
                         + f'</td><td>{self.geeft(g)}</td><td class="num">{self.nodig(g)}</td><td class="num">{n}</td><td>{leeg}</td><td>{self.waar(g)}</td></tr>')
        return rijen

    def groep_tabel(self, groepen):
        rijen = self.groep_rijen(groepen)
        if not rijen:
            return ""
        return self.tabel([(t("Spot group", "Plekgroep"), 0), (t("Gives", "Geeft"), 0), (t("Level", "Niveau"), 1), (t("Spots", "Plekken"), 1),
                           (t("Back after", "Terug na"), 0), (t("Where", "Waar"), 0)], rijen)

    def verzamel_band(self, aid, tier):
        br = self.br
        soorten = ("ERTS", "HOUT", "KRUID") if aid == "verzamelen" else ("VIS", "DUIK")
        mats = [m for m in br.materialen.values() if m["tier"] == tier and m["soort"] in soorten]
        hout_hars = aid == "verzamelen" and tier == 3
        inner = f'<p><span class="badge lv">{self.xp_regel(tier)}</span></p>'
        tip = BAND_TIP.get((aid, tier))
        if tip:
            inner += p(ESC(tip[0]), ESC(tip[1]))
        if mats:
            delen = []
            for s in soorten:
                ms = [m for m in mats if m["soort"] == s]
                if ms:
                    delen.append(f'<li><b>{t(*SOORT[s])}</b>: ' + ", ".join(self.matnaam(m["id"]) + (f' <small class="muted">({t(*WANNEER[m["wanneer"]])})</small>'
                                                                                                if m["wanneer"] and m["wanneer"] != "duik" else "") for m in ms) + "</li>")
            inner += f'<h4>{t("What you get", "Wat je krijgt")}</h4><ul>{"".join(delen)}</ul>'
            if hout_hars:
                inner += p(ESC("Tier-3 wood gives Schemerhout or Roze Hars, plus a 30% chance on an extra Roze Hars every harvest."), ESC("Tier-3-hout geeft Schemerhout of Roze Hars, plus bij elke oogst 30% kans op een extra Roze Hars."))
        groepen = [g for g in br.groepen if g["tier"] == tier and g["soort"] in soorten]
        groepen.sort(key=lambda g: (g["bron"] != "eiland", g["zone"], g["bron"] == "grot", g["soort"], g["id"]))
        inner += f'<h4>{t("Where to go", "Waar naartoe")}</h4>' + (self.groep_tabel(groepen) or f'<p class="leeg">{t("No spots of this tier.", "Geen plekken van deze tier.")}</p>')
        if aid == "verzamelen":
            korven = [g for g in br.groepen if g["soort"] == "KORF" and self.nodig(g) >= br.tier_min[tier] and self.nodig(g) <= _tier_top(br, tier)]
            if korven:
                inner += (f'<h4>{t("Beehives you can place from this band", "Bijenkorven die je vanaf deze band mag neerzetten")}</h4>'
                          + self.groep_tabel(sorted(korven, key=lambda g: (g["zone"], g["id"]))))
        return self.band(aid, tier, inner)

    def verzamel_sectie(self, aid):
        br = self.br
        if aid == "verzamelen":
            intro = p(ESC(f"Gathering = ore, wood, herbs and your own beehives. Every spot shows its tier; you need the tier's level to harvest it. Perfect in the little game = extra vadsig (+20% xp), good = vadsig (+10%). A glittering spot ({round(br.zeldzaam_kans * 100)}% of refills) gives 3x the yield and 3x the xp, plus a {round(br.zeldzaam_mat * 100)}% chance on the rare material (Glinstergruis, Oerbast, Gouden Klaver, Koninginnengelei). A perfect ore swing from tier 2 on has a 25% chance on a raw crystal. Harvesting for 10 minutes without walking 5 blocks halves the yield."),
                      ESC(f"Verzamelen = erts, hout, kruiden en je eigen bijenkorven. Elke plek toont zijn tier; je hebt het niveau van die tier nodig om te oogsten. Perfect in het spelletje = extra vadsig (+20% xp), goed = vadsig (+10%). Een glinsterende plek ({round(br.zeldzaam_kans * 100)}% van de keren) geeft 3x zoveel en 3x de xp, plus {round(br.zeldzaam_mat * 100)}% kans op het zeldzame materiaal (Glinstergruis, Oerbast, Gouden Klaver, Koninginnengelei). Een perfecte ertsslag geeft vanaf tier 2 25% kans op een ruw kristal. 10 minuten oogsten zonder 5 blokken te lopen halveert de opbrengst."))
            intro += p(ESC(f"Beehives: max {br.per_zone} per zone (the Ambachtseiland counts as zone 1). A hive fills one comb every {br.raat_min} minutes (slower with few flowers), max {br.max_raten}. Harvest = xp of YOUR tier per comb, so a full hive is a big xp chunk at any level. Too early (no game) = grumpy bees: sticky, never damage."),
                       ESC(f"Bijenkorven: max {br.per_zone} per zone (het Ambachtseiland telt als zone 1). Een korf vult elke {br.raat_min} minuten een raat (langzamer met weinig bloemen), max {br.max_raten}. Oogsten = de xp van JOUW tier per raat, dus een volle korf is op elk niveau een flinke hap xp. Niks doen in het spelletje = chagrijnige bijen: plakkerig, nooit schade."))
        else:
            intro = p(ESC("Fishing = fishing spots and (from tier 4) coral diving. Per tier there is a normal fish, a night fish and a rain fish: at night and in the rain the special fish bites with 35% chance. Divers find a Parelschelp 6% of the time; a glittering fishing spot can give one too."),
                      ESC("Vissen = visplekken en (vanaf tier 4) koraalduiken. Per tier is er een gewone vis, een nachtvis en een regenvis: 's nachts en bij regen bijt de bijzondere vis met 35% kans. Duikers vinden 6% van de keren een Parelschelp; een glinsterende visplek kan er ook een geven."))
        return intro + "".join(self.verzamel_band(aid, tier) for tier in range(1, self.br.max_tier + 1))

    # -- making bands
    def ingr(self, r):
        delen = []
        for n, opties in r["in_id"]:
            ids = []
            for o in opties:
                ids += self.br.lijsten.get(o, [o]) if o.isupper() else [o]
            mats = [self.br.materialen.get(i) for i in ids]
            if len(ids) > 1 and all(m and m["soort"] == "VIS" for m in mats) and len({m["tier"] for m in mats}) == 1:
                naam = t(f"fish (tier {mats[0]['tier']}, any)", f"vis (tier {mats[0]['tier']}, elke soort)")
            elif len(ids) > 1 and all(m and m["soort"] == "KORF" for m in mats) and len({m["tier"] for m in mats}) == 1:
                naam = t(f"honey (tier {mats[0]['tier']}, any)", f"honing (tier {mats[0]['tier']}, elke soort)")
            else:
                naam = " / ".join(self.matnaam(i) for i in ids)
            delen.append(f"{n}x {naam}")
        if r["soort"] == "slijp":
            delen.insert(0, t("3 crystals of one kind", "3 kristallen van één soort"))
        return ", ".join(delen) or "-"

    def recepten(self, aid):
        out = [dict(r) for r in self.b.d.get("ambachten", {}).get("recepten", []) if r["ambacht"] == aid]
        out += [dict(r) for r in self.br.verhaal_recepten.values() if r["ambacht"] == aid]
        if self.br.klein and self.br.klein["ambacht"] == aid:
            out.append(dict(self.br.klein))
        for r in out:
            r.setdefault("in_id", [])
        out = [r for r in out if self.toon(r["tier"])]
        out.sort(key=lambda r: (r["level"], r["tier"], r["soort"] == "verhaal", r["id"]))
        return out

    def recept_naam(self, r):
        k = "vadsrijk.ambacht.recept." + r["id"]
        return t(self.br.en_(k, r["id"]), self.br.nl_(k, r["id"]))

    def waar_maken(self, aid, tier):
        """Where a tier's recipes can be made (Werkplaats.magHier)."""
        if tier <= 3:
            return t("any workshop (Ambachtseiland, every town)", "elke werkplaats (Ambachtseiland, elke stad)"), 1
        if tier == 4 and aid == "smeden":
            return t("Werkmeester Krul in Lampionhaven, or the Ambachtsgilde in Hoogvads", "Werkmeester Krul in Lampionhaven, of het Ambachtsgilde in Hoogvads"), 5
        if tier <= 5:
            return t("the Ambachtsgilde in Hoogvads", "het Ambachtsgilde in Hoogvads"), 5
        return t("the teachers of Saffraanpoort", "de leermeesters van Saffraanpoort"), 6

    def recept_rijen(self, lijst, aid):
        rijen = []
        for r in lijst:
            z = max(r["tier"], self.b.tekst_zone(self.br.nl_("vadsrijk.ambacht.recept." + r["id"], "")))
            wz = self.waar_maken(aid, r["tier"])[1]
            verhaal = r["soort"] == "verhaal"
            extra = (f'<br><small class="muted">{t("only while its story step asks for it", "alleen tijdens de verhaalstap die erom vraagt")}</small>' if verhaal else "")
            aantal = f' x{r["aantal"]}' if r.get("aantal", 1) > 1 else ""
            rijen.append(f'<tr{self.dz(max(z, wz))}><td class="num">{r["level"]}</td><td><b>{self.recept_naam(r)}</b>{aantal}{extra}</td>'
                         f'<td>{t(*UIT.get(r["soort"], (r["soort"], r["soort"])))}</td><td class="num">{r["tier"]}</td><td>{self.ingr(r)}</td>'
                         f'<td class="num">{_maak_xp(self.br, r)}</td></tr>')
        return rijen

    def recept_tabel(self, lijst, aid):
        rijen = self.recept_rijen(lijst, aid)
        if not rijen:
            return f'<p class="leeg">{t("Nothing new in this band.", "Niets nieuws in deze band.")}</p>'
        return self.tabel([(t("Level", "Niveau"), 1), (t("Recipe", "Recept"), 0), (t("Makes", "Maakt"), 0), ("Tier", 1), (t("Needs", "Nodig"), 0),
                           (t("Xp per make", "Xp per keer"), 1)], rijen)

    def maak_sectie(self, aid):
        br = self.br
        alle = self.recepten(aid)
        if aid == "smeden":
            intro = p(ESC("Smithing makes jewellery from level 1 (ring, necklace, earring, bracelet), Masterwork jewellery with a boss core, boss cores from 5 core splinters, and decoration that goes home by the Post. Maatwerk = an Epic piece where you choose 2 bonuses; Masterwork = Legendary, 3 bonuses + a property."),
                      ESC("Smeden maakt sieraden vanaf niveau 1 (ring, ketting, oorbel, armband), Meesterwerk-sieraden met een bazenkern, bazenkernen uit 5 kernsplinters, en versiering die met de Post naar huis gaat. Maatwerk = een Episch stuk waarop je 2 bonussen kiest; Meesterwerk = Legendarisch, 3 bonussen + een eigenschap."))
        else:
            intro = p(ESC("Cooking makes food and every potion (the shops only sell the Klein Vadsdrankje), and from level 9 it cuts crystals one quality up. Food: one buff at a time, 30 minutes. A feast gives the party nearby the buff too."),
                      ESC("Koken maakt eten en alle drankjes (de winkels verkopen alleen het Klein Vadsdrankje), en vanaf niveau 9 slijpt het kristallen een kwaliteit hoger. Eten: één buff tegelijk, 30 minuten. Een feestmaal geeft de party in de buurt de buff ook."))
        intro += p(ESC("Xp per make = 0.7 x the number of ingredients x the gathering xp of the recipe's tier, +10% per minigame step (Make 10x: no game). So every recipe of a tier gives the same xp per ingredient: level on what is cheapest to gather. More than 5 levels above a tier's band = half xp."),
                   ESC("Xp per keer = 0,7 x het aantal ingrediënten x de verzamel-xp van de tier van het recept, +10% per stap in het spelletje (Maak 10x: geen spelletje). Elk recept van een tier geeft dus evenveel xp per ingrediënt: level op wat het makkelijkst te verzamelen is. Meer dan 5 niveaus boven de band van een tier = halve xp."))
        delen = []
        for tier in range(1, br.max_tier + 1):
            lo, hi = br.tier_min[tier], _tier_top(br, tier)
            lijst = [r for r in alle if lo <= r["level"] <= hi]
            if not self.toon(tier):
                continue
            waar, wz = self.waar_maken(aid, tier)
            kop = (f'<p><span class="badge lv">{t(f"Tier {tier}: {round(0.7 * br.verzamel[tier])} xp per ingredient", f"Tier {tier}: {round(0.7 * br.verzamel[tier])} xp per ingrediënt")}</span> '
                   + f'<span{self.dz(wz)}>{t("Made at", "Maken bij")}: {waar}</span></p>')
            delen.append(self.band(aid, tier, kop + self.recept_tabel(lijst, aid)))
        return intro + "".join(delen)

    # -- every spot group per zone
    def alle_plekken(self):
        br = self.br
        uit = []
        for z in self.b.d["zones"]:
            zn = z["nr"]
            gs = [g for g in br.groepen if g["zone"] == zn and self.toon(self.zone_van(g))]
            if not gs:
                continue
            gs.sort(key=lambda g: ({"eiland": 0, "ap": 1, "grot": 2}.get(g["bron"], 3), ["ERTS", "HOUT", "KRUID", "KORF", "VIS", "DUIK"].index(g["soort"]), g["id"]))
            rijen = []
            n_pos = 0
            for g in gs:
                pos, live = self.posities(g)
                n_pos += len(pos) if live else 0
                spots = (br.live.get("_spots") or {}).get(g["id"])
                if live:
                    lijst = " &middot; ".join(f'<a href="../kaart.html?x={x}&amp;z={zz}">{x} {y} {zz}</a>' for x, y, zz in pos)
                    bron = t(f"{len(pos)} positions from the live server ({spots} of {g['aantal']} spots found)", f"{len(pos)} posities van de live server ({spots} van {g['aantal']} plekken gevonden)")
                    blok = f'<details><summary>{bron}</summary><p class="posities">{lijst}</p></details>'
                else:
                    blok = f'<small class="muted">{t("not scanned on the live server yet: only the anchor (spots lie within " + str(g["straal"]) + " blocks)", "nog niet gescand op de live server: alleen het anker (de plekken liggen binnen " + str(g["straal"]) + " blokken)")}</small>'
                if g["id"] in GEHEIM_GROEPEN:
                    blok = spoiler(blok, "Secret spot: click to show the positions", "Geheime plek: klik voor de posities")
                tier = f'T{g["tier"]}' + (f' <small class="muted">({t("level", "niveau")} {self.nodig(g)})</small>')
                rijen.append(f'<tr{self.dz(self.zone_van(g))}><td><b>{self.groepnaam(g)}</b><br><small class="muted">{t(*SOORT[g["soort"]])}'
                             f'{" &middot; " + t("cave", "grot") if g["bron"] == "grot" else ""}</small></td><td>{tier}</td><td>{self.geeft(g)}</td>'
                             f'<td>{self.waar(g)}{blok}</td></tr>')
            inner = self.tabel([(t("Spot group", "Plekgroep"), 0), ("Tier", 0), (t("Gives", "Geeft"), 0), (t("Where + positions", "Waar + posities"), 0)], rijen)
            extra = t(f"{len(gs)} groups, {n_pos} live positions", f"{len(gs)} groepen, {n_pos} live posities")
            uit.append(self.b.zonevak(z, inner, extra))
        return "".join(uit)

    # -- story steps
    def doel_info(self, d):
        """(ambacht, level, html of what) of a quest goal."""
        br = self.br
        soort, doel = d.get("soort"), d.get("doel", "")
        if soort == "meld" and doel.startswith("ambacht_oogst:"):
            k = doel.split(":", 1)[1].upper()
            k = {"KRUID": "KRUID", "ERTS": "ERTS", "HOUT": "HOUT", "VIS": "VIS"}.get(k, k)
            return SOORT_AMB.get(k, "verzamelen"), 1
        if soort == "meld" and doel.startswith("ambacht_maak:"):
            w = doel.split(":", 1)[1]
            return ("smeden", 1) if w.startswith("sieraad_") else ("koken", 1)
        if soort == "verzamel" and doel.startswith("guhs:vr_amb_verhaal_"):
            r = br.verhaal_recepten.get(doel[len("guhs:vr_amb_verhaal_"):])
            return (r["ambacht"], r["level"]) if r else (None, None)
        if soort == "verzamel" and doel.startswith("guhs:vr_amb_"):
            m = br.materialen.get(doel[len("guhs:vr_amb_"):])
            if m:
                lv = (1 if m["tier"] <= 3 else br.tier_min[m["tier"] - 2]) if m["soort"] == "KORF" else br.tier_min[max(1, m["tier"])]
                return SOORT_AMB[m["soort"]], lv
        if soort == "meld" and doel == "z4_vis:gevangen":
            return "vissen", None
        return None, None

    def doel_plek(self, d):
        pl = d.get("plek") or ""
        if pl.startswith("poi:"):
            return self.br.tracker.get(pl[4:])
        if pl.startswith("npc:"):
            n = self.b.npc.get(pl[4:])
            return n.get("pos") if n else None
        return None

    def quest_titel(self, qid):
        k = f"vadsrijk.npc.q.{qid}.titel"
        q = self.b.q.get(qid)
        if q:
            return self.b.l_quest(qid, P)
        return t(self.br.en_(k, qid), self.br.nl_(k, qid))

    def quest_rij(self, qid, zone):
        q = self.br.quests.get(qid)
        if not q:
            return ""
        delen = []
        for i, d in enumerate(q.get("doelen", [])):
            a, lv = self.doel_info(d)
            if a is None and d.get("soort") in ("gebied", "praat"):
                a, lv = None, None
            k = f"vadsrijk.npc.q.{qid}.doel.{i}"
            tekst = t(self.br.en_(k, ""), self.br.nl_(k, ""))
            amb = (f' <span class="badge">{t(AMB_EN[a], AMB_NL[a])}{" " + str(lv) if lv else ""}</span>' if a else "")
            delen.append(f"<li>{tekst}{amb}<br>{self.kaart(self.doel_plek(d))}</li>")
        return (f'<tr{self.dz(zone)}><td>{zbadge(f"zone{zone}", self.b.z[f"zone{zone}"]["naam"]) if f"zone{zone}" in self.b.z else zone}</td>'
                f'<td><b>{self.quest_titel(qid)}</b><br><small class="muted">{t("player level", "speelniveau")} {q.get("lv", "?")}</small></td>'
                f'<td><ul class="kaal">{"".join(delen)}</ul></td></tr>')

    def verhaal_sectie(self):
        br = self.br
        rijen = []
        for k in br.ketens:
            if not self.toon(k["zone"]):
                continue
            for qid in k["quests"]:
                rijen.append(self.quest_rij(qid, k["zone"]))
        tabel = self.tabel([("Zone", 0), ("Quest", 0), (t("Steps, craft level and where", "Stappen, ambachtniveau en waar"), 0)], rijen)
        uit = p(ESC("The main story has a craft chain in every zone: first gather something, then make a story item from it. The story waits for it: the quest after the chain only starts when both steps are done, and the zone gate asks for every chain of the zones you leave behind. The story item can only be made while its step asks for it."),
                ESC("Het hoofdverhaal heeft in elke zone een ambachtsketen: eerst iets verzamelen, dan er een verhaalvoorwerp van maken. Het verhaal wacht erop: de quest na de keten begint pas als beide stappen klaar zijn, en de zonepoort vraagt om elke keten van de zones die je achter je laat. Het verhaalvoorwerp kun je alleen maken zolang de stap erom vraagt."))
        uit += tabel
        # the side quest(s) with a craft goal
        zij = [q for q in br.quests.values() if q.get("soort") != "hoofd" and any(self.doel_info(d)[0] for d in q.get("doelen", []))]
        if zij:
            zr = []
            for q in zij:
                zn = int(q["id"][1]) if re.match(r"z\d_", q["id"]) else 1
                if self.toon(zn):
                    zr.append(self.quest_rij(q["id"], zn))
            uit += f'<h3>{t("Side quests", "Zijquests")}</h3>' + self.tabel([("Zone", 0), ("Quest", 0), (t("Steps, craft level and where", "Stappen, ambachtniveau en waar"), 0)], zr)
            if any(q["id"] == "z4_s03_vissen" for q in zij) and self.toon(4):
                uit += ('<div data-zone="4">' + p(ESC("Tip for \"Wat bijt er vandaag?\": every fish you catch in zone 4 counts, also with a normal fishing rod (any level). With the craft: the zone-4 fishing spots need Fishing 32, but the cave water in the Koraalgrot (tier 3) also lies in zone 4 and needs only Fishing 22."),
                                                     ESC("Tip voor \"Wat bijt er vandaag?\": elke vis die je in zone 4 vangt telt, ook met een gewone hengel (elk niveau). Met het ambacht: de visplekken van zone 4 vragen Vissen 32, maar het grotwater in de Koraalgrot (tier 3) ligt ook in zone 4 en vraagt maar Vissen 22."))
                        + "</div>")
        # the masters' stories
        lv = br.verhaal_levels
        gl = br.gilde_levels
        uit += f'<h3>{t("The masters tell their story (Vertel eens...)", "De leermeesters vertellen (Vertel eens...)")}</h3>'
        uit += p(ESC(f"Every craft has a little story at its master guhs (choice \"Vertel eens...\"): {len(lv)} steps at craft level {', '.join(map(str, lv))}. Reward per step: 100 coins x the step number, 15% of a level in xp and Knutselpenningen. Step 1 of Gathering and Fishing gives 3 of every tier-1 material of that craft; step {len(lv)} sends a keepsake home (Koken: kokschort, Vissen: vissershoedje, Verzamelen: tuinhoedje, Smeden: mijnhelm)."),
                     ESC(f"Elk ambacht heeft een verhaaltje bij zijn leermeester-guhs (keuze \"Vertel eens...\"): {len(lv)} stapjes op ambachtniveau {', '.join(map(str, lv))}. Beloning per stap: 100 munten x het stapnummer, 15% van een niveau aan xp en Knutselpenningen. Stap 1 van Verzamelen en Vissen geeft 3 van elk tier-1-materiaal van dat ambacht; stap {len(lv)} stuurt een aandenken naar huis (Koken: kokschort, Vissen: vissershoedje, Verzamelen: tuinhoedje, Smeden: mijnhelm)."))
        meesters = {}
        for npc, zone, poi, a in br.werkplaatsen:
            if a and self.toon(zone):
                meesters.setdefault(a, []).append((npc, zone, poi))
        mr = []
        for aid, en, nl in AMBACHTEN:
            namen = []
            for npc, zone, poi in meesters.get(aid, []):
                n = self.b.npc.get(npc)
                naam = ESC(n["naam"]) if n else ESC(npc)
                pos = br.plek(zone, poi)
                namen.append(f'<span{self.dz(zone)}>{naam} {self.kaart(pos)}</span>')
            mr.append(f'<tr><td><b>{t(en, nl)}</b></td><td>{", ".join(map(str, lv))}</td><td>{"<br>".join(namen)}</td></tr>')
        uit += self.tabel([(t("Craft", "Ambacht"), 0), (t("Steps at level", "Stappen op niveau"), 0), (t("Masters", "Leermeesters"), 0)], mr)
        if self.toon(5) and gl:
            gm = {"koken": "z5_leermeester_koken", "smeden": "z5_leermeester_smeden", "verzamelen": "z5_gildemeester", "vissen": "z5_gildemeester"}
            gr = []
            for aid, en, nl in AMBACHTEN:
                n = self.b.npc.get(gm[aid])
                gr.append(f'<tr><td><b>{t(en, nl)}</b></td><td>{", ".join(map(str, gl))}</td><td>{ESC(n["naam"]) if n else gm[aid]} {self.kaart(n.get("pos") if n else None)}</td></tr>')
            uit += ('<div data-zone="5">' + p(ESC(f"After the {len(lv)} steps the story goes on at the Ambachtsgilde in Hoogvads: {len(gl)} more steps per craft, at level {' and '.join(map(str, gl))}. The Koken and Smeden teachers tell their own craft; the Gildemeester tells Gathering and Fishing."),
                                               ESC(f"Na de {len(lv)} stappen gaat het verhaal verder in het Ambachtsgilde in Hoogvads: nog {len(gl)} stappen per ambacht, op niveau {' en '.join(map(str, gl))}. De leermeesters van Koken en Smeden vertellen hun eigen ambacht; de Gildemeester vertelt Verzamelen en Vissen."))
                    + self.tabel([(t("Craft", "Ambacht"), 0), (t("Steps at level", "Stappen op niveau"), 0), (t("Where", "Waar"), 0)], gr) + "</div>")
        uit += f'<h3>{t("Order board", "Bestelbord")}</h3>'
        uit += p(ESC("Every board guh (Klerk, in every town) has the Bestelbord: 3 orders a day (new at 06:00) picked from what your levels can do; each gives 8% of a level in craft xp and coins. 12 orders in a week = the Ambachtskist."),
                 ESC("Elke bordguh (Klerk, in elke stad) heeft het Bestelbord: 3 bestellingen per dag (nieuw om 06:00), gekozen uit wat jouw niveaus kunnen; elke bestelling geeft 8% van een niveau aan ambachts-xp en munten. 12 bestellingen in een week = de Ambachtskist."))
        return uit

    # -- recipes per tier (summary)
    def tier_overzicht(self):
        br = self.br
        rijen = []
        for tier in range(1, br.max_tier + 1):
            if not self.toon(tier):
                continue
            sm = [r for r in self.recepten("smeden") if r["tier"] == tier]
            ko = [r for r in self.recepten("koken") if r["tier"] == tier]
            ring = [r for r in sm if r["soort"] == "sieraad"]
            mw = [r for r in sm if r["soort"] == "meester"]
            post = [r for r in sm if r["soort"] in ("post", "verhaal", "kern")]
            s_txt = []
            if ring:
                s_txt.append(t(f"Maatwerk jewellery from level {min(r['level'] for r in ring)}", f"Maatwerk-sieraden vanaf niveau {min(r['level'] for r in ring)}"))
            if mw:
                s_txt.append(t(f"Masterwork from level {min(r['level'] for r in mw)}", f"Meesterwerk vanaf niveau {min(r['level'] for r in mw)}"))
            for r in post:
                s_txt.append(f"{self.recept_naam(r)} ({r['level']})")
            eten = [r for r in ko if r["soort"] == "eten"]
            drank = [r for r in ko if r["soort"] in ("drank", "item")]
            ander = [r for r in ko if r["soort"] not in ("eten", "drank", "item")]
            k_txt = ([t("Food", "Eten") + ": " + ", ".join(f"{self.recept_naam(r)} ({r['level']})" for r in eten)] if eten else []) \
                + ([t("Potions", "Drankjes") + ": " + ", ".join(f"{self.recept_naam(r)} ({r['level']})" for r in drank)] if drank else []) \
                + [f"{self.recept_naam(r)} ({r['level']})" for r in ander]
            rijen.append(f'<tr{self.dz(tier)}><td class="num">{tier}</td><td>{br.tier_min[tier]}+</td><td>{"<br>".join(s_txt) or "-"}</td><td>{"<br>".join(k_txt) or "-"}</td></tr>')
        return self.tabel([("Tier", 1), (t("From level", "Vanaf niveau"), 0), (t("Smithing", "Smeden"), 0), (t("Cooking", "Koken"), 0)], rijen)

    # -- all of it
    def html(self):
        br = self.br
        sprongen = [("hoe", "How levelling works", "Zo werkt levelen")] + [(a, en, nl) for a, en, nl in AMBACHTEN] + [
            ("plekken", "Every spot", "Alle plekken"), ("verhaal", "Story and quests", "Verhaal en quests"), ("recepten", "Recipes per tier", "Recepten per tier")]
        nav = '<p class="sprong">' + " &middot; ".join(f'<a href="#{i}">{t(en, nl)}</a>' for i, en, nl in sprongen) + "</p>"
        n_live = sum(len(v) for k, v in br.live.items() if not k.startswith("_") and isinstance(v, list)
                     and any(g["id"] == k and self.toon(self.zone_van(g)) for g in br.groepen))
        n_groepen = sum(1 for g in br.groepen if self.toon(self.zone_van(g)))
        lead = p(ESC("The full levelling guide for the four crafts: what each level unlocks, where to go (with coordinates and the map), how much xp everything gives, every gathering spot, and every story step that asks for a craft. Spoilers: this page shows everything (the zone switch at the top still hides later zones)."),
                 ESC("De complete levelgids voor de vier ambachten: wat elk niveau vrijspeelt, waar je naartoe gaat (met coördinaten en de kaart), hoeveel xp alles geeft, elke verzamelplek, en elke verhaalstap die om een ambacht vraagt. Spoilers: deze pagina laat alles zien (de zoneschakelaar bovenaan verbergt latere zones nog wel)."))
        hoe = (f'<h2 id="hoe">{t("How levelling works", "Zo werkt levelen")}</h2>'
               + p(ESC(f"Every craft goes to level {br.cap}. The tiers follow the zones: a tier's materials, spots and recipes open at the tier's first level. Xp to the next level = (40 + 25 x level^1.6) x {br.schaal}. You rise fastest on your own tier: more than {br.te_hoog} levels above a tier's band you only get half xp there. Offline you build up rested xp (double xp, max 2 hours). Higher levels also give more per harvest: +1 per 8 levels above the tier, and a small chance on double."),
                       ESC(f"Elk ambacht gaat tot niveau {br.cap}. De tiers volgen de zones: de materialen, plekken en recepten van een tier gaan open op het eerste niveau van die tier. Xp naar het volgende niveau = (40 + 25 x niveau^1,6) x {str(br.schaal).replace('.', ',')}. Je stijgt het snelst op je eigen tier: meer dan {br.te_hoog} niveaus boven de band van een tier krijg je daar maar de halve xp. Offline bouw je uitgerust-xp op (dubbele xp, max 2 uur). Hogere niveaus geven ook meer per oogst: +1 per 8 niveaus boven de tier, en een kleine kans op dubbel."))
               + self.xp_tabel()
               + p(ESC("The Ambachtenboek (key Y) shows your levels, every recipe and the spots of each zone. Master guhs stand round every town square; the Ambachtseiland next to Knuffelkamp has all four huts in one place. More mechanics on the Mechanics page."),
                   ESC("Het Ambachtenboek (toets Y) toont je niveaus, elk recept en de plekken per zone. Leermeester-guhs staan rond elk stadsplein; het Ambachtseiland naast Knuffelkamp heeft alle vier de huisjes bij elkaar. Meer uitleg op de pagina Systemen."))
               + f'<p><a href="../systemen/ambachten.html">{t("Crafts on the Mechanics page", "Ambachten op de pagina Systemen")}</a></p>')
        delen = [f'<h1>{t("Crafts: the levelling guide", "Ambachten: de levelgids")}</h1>', f'<div class="lead">{lead}</div>', nav, hoe]
        for aid, en, nl in AMBACHTEN:
            body = self.verzamel_sectie(aid) if aid in ("verzamelen", "vissen") else self.maak_sectie(aid)
            delen.append(f'<h2 id="{aid}">{t(en, nl)}</h2>{body}')
        datum = (re.search(r"\d{4}-\d\d-\d\d", br.live.get("_bron", "")) or ["2026-11-25"])[0]
        delen.append(f'<h2 id="plekken">{t("Every spot, per zone", "Alle plekken, per zone")}</h2>'
                     + p(ESC(f"Every gathering group with all the positions its spots move between (a spot comes back full at another of its positions). Positions from a scan of the live server ({datum}); a group the server has not scanned yet shows its anchor. Click a position to see it on the map."),
                             ESC(f"Elke verzamelgroep met alle posities waartussen zijn plekken verschuiven (een plek komt vol terug op een andere van zijn posities). Posities uit een scan van de live server ({datum}); een groep die de server nog niet gescand heeft, toont zijn anker. Klik op een positie om hem op de kaart te zien."))
                     + self.alle_plekken())
        delen.append(f'<h2 id="verhaal">{t("Story steps and quests", "Verhaalstappen en quests")}</h2>' + self.verhaal_sectie())
        delen.append(f'<h2 id="recepten">{t("What each tier lets you make", "Wat je per tier kunt maken")}</h2>' + self.tier_overzicht()
                     + p(ESC("The full recipe lists (with ingredients and xp) are in the Smithing and Cooking sections above."),
                         ESC("De volledige receptlijsten (met ingrediënten en xp) staan hierboven bij Smeden en Koken.")))
        stijl = ('<style>.sprong{font-weight:700;line-height:2}.posities{font:500 12px/1.9 var(--mono,monospace);word-spacing:2px}'
                 'ul.kaal{list-style:none;padding:0;margin:0}ul.kaal li+li{margin-top:6px}h4{margin:14px 0 6px}</style>')
        self.telling = (n_groepen, n_live)
        return stijl + "\n".join(delen)


def pagina(b):
    g = Gids(b)
    main = g.html()
    b.schrijf(P, "Ambachten: de levelgids", "Crafts: the levelling guide", main, P,
              beschrijving="De levelgids voor de ambachten van Het Vadsrijk: per niveau wat je vrijspeelt, waar je heen gaat, xp en alle verzamelplekken.")
    return g
