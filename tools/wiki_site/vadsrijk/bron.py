"""
The data export of guhs.nl/vadsrijk: everything the pages and the map need, read from the repo files (and the mod's runtime dump
docs/wiki/vadsrijk_runtime.json when it exists: run `gradlew runGameTestServer -Pgt=VadsrijkWeb` or `/guhs vadsrijk web
wikiexport` and copy vadsrijk_runtime.json there). Re-runnable: when the world layout moves (guhs1100-avontuur/wereldindeling),
regenerate the map + POI files and the runtime dump, and the site follows.

Privacy/spoiler rules live HERE (an allowlist): the public map layers only get the PUBLIEK kinds; secrets (knuffels, treasures,
clues, reliefs, letters, puzzles, cave treasures) get their positions ONLY in geheimen.py's list (user decision 2026-11-08), which the
site shows behind a spoiler click per entry and in a map layer that is off by default. Secret quest goals stay without a position.
Only static game content; never player data.
"""
import collections
import glob
import json
import os
import re

ZONES = [
    # id, voltooiing key, ring area, hub area, quest file, quest prefix, level range, colour
    dict(id="zone1", nr=1, boek="zone1", ring="ring", hub="zwamzoet", quests="quests.json", lv=(1, 20), kleur="#e0559a",
         extra_gebieden=("knuffelkamp", "haven", "zwamzoet", "klodderweide", "sporenwoud", "druipgrotten", "zwamtempel")),
    dict(id="zone2", nr=2, boek="zone2", ring="z2_ring", hub="z2_kandijkom", quests="quests_z2.json", lv=(20, 30), kleur="#5aa9e6",
         extra_gebieden=()),
    dict(id="zone3", nr=3, boek="zone3", ring="z3_ring", hub="z3_honingtak", quests="quests_z3.json", lv=(30, 50), kleur="#e8a33a",
         extra_gebieden=()),
    # U-SITE: zone 4 (De Lampionkust, an archipelago) + zone 5 (Hoogvads, the town on Moeder Wolkwal)
    dict(id="zone4", nr=4, boek="zone4", ring="z4_zee", hub="z4_lampionhaven", quests="quests_z4.json", lv=(50, 56), kleur="#2bb3a3",
         extra_gebieden=()),
    dict(id="zone5", nr=5, boek="zone5", ring="z5_moeder_wolkwal", hub="z5_hoogvads", quests="quests_z5.json", lv=(56, 60), kleur="#9b7be0",
         extra_gebieden=()),
    # U-SITE round 3: zone 6 (Het Zijdeland, the rose desert; the first raid De Sterrendovende Toren)
    dict(id="zone6", nr=6, boek="zone6", ring="z6_zijdeland", hub="z6_rozenkloof", quests="quests_z6.json", lv=(60, 70), kleur="#e07a5f",
         extra_gebieden=()),
]
ZONE_PREFIX = (("z6raid_", "zone6"), ("z6w_", "zone6"), ("z6_", "zone6"), ("z5d_", "zone5"), ("z5_", "zone5"), ("z4_", "zone4"), ("z3_", "zone3"), ("z2_", "zone2"))
# U-SITE: zone-5 places without a vadsrijk.kern.gebied.* key (the districts also come from POI_Z5.md's table)
GEBIED_NAAM = {"z5_moeder_wolkwal": "Moeder Wolkwal", "z5_proefarena": "De Proefarena", "z5_buikgangen": "De Buikgangen",
               "z5_schuilnest": "Het Schuilnest", "z5_wolkrand_zuid": "De Wolkrand (zuid)", "z5_wolkrand_noord": "De Wolkrand (noord)",
               "z5_staartvlakte": "De Staartvlakte"}
NIET_OP_SITE = {"proeftuin"}
# U-SITE round 3: the zone-6 sets (+ the raid's Sterrenzijde) -> a fallback name
Z6_SETS = {"duinwandelaar": "Duinwandelaarset", "duinzijde": "Duinzijdeset", "karavaanwacht": "Karavaanwachtset", "sterrenwacht": "Sterrenwachtset",
           "sterrenzijde": "Sterrenzijdeset"}                   # dev areas
CRITTERS = {"sneeuwhaas", "ijsvogel", "kristalmot", "honingbij", "sporenmot", "lantaarnkever", "wortelmuis"}
NIET_WEZENS = {"vr_proefklodder", "vr_proefkloddertje", "vr_npc_guh", "vr_huisdier", "vr_draakvriendje", "vr_roze_draak",
               "vr_mom_karavaan", "vr_mom_handelaar", "vr_mob_kampklodder", "vr_mob_draak"}
# boss entities that are the real bosses (others are adds/props); arena suffix -> entity
BAZEN = [
    ("zone1", "vr_klodderreus", "klodderreus", None, "vadsrijk.wezens.tip.kampklodder"),
    ("zone1", "vr_baas_koning_klodder", "koning_klodder", "vadsrijk.bazen.tip.koning_klodder", None),
    ("zone1", "vr_baas_mika_spion", "mika_spion", "vadsrijk.bazen.tip.mika_spion", None),
    ("zone1", "vr_baas_opperzwam", "opperzwam", "vadsrijk.bazen.tip.opperzwam", None),
    ("zone2", "vr_z2_baas_galmwolf", "galmwolf", "vadsrijk.z2bazen.tip.galmwolf", None),
    ("zone2", "vr_z2_baas_boormeester", "boormeester", "vadsrijk.z2bazen.tip.boormeester", "vadsrijk.z2bazen.tip.boormeester_paneel"),
    ("zone2", "vr_z2_baas_spiegelzucht", "spiegelzucht", "vadsrijk.z2bazen.tip.spiegelzucht", "vadsrijk.z2bazen.tip.splinterzaal"),
    ("zone3", "vr_z3_baas_kruinroofvogel", "kruinroofvogel", "vadsrijk.z3bazen.tip.kruinroofvogel", None),
    ("zone3", "vr_z3_baas_taleggio", "taleggio", "vadsrijk.z3bazen.tip.taleggio", None),
    ("zone3", "vr_z3_baas_brokkel", "brokkel", "vadsrijk.z3bazen.tip.brokkel", "vadsrijk.z3bazen.tip.kanon"),
    ("zone3", "vr_z3_baas_roquefort", "roquefort", "vadsrijk.z3bazen.tip.roquefort", None),
    ("zone3", "vr_z3_baas_piepvel", "piepvel", "vadsrijk.z3bazen.tip.piepvel", "vadsrijk.z3bazen.tip.lantaarnpaal"),
    ("zone3", "vr_z3_baas_wortelwurger", "wortelwurger", "vadsrijk.z3bazen.tip.wortelwurger", "vadsrijk.z3bazen.tip.aderloop"),
    ("zone3", "vr_z3_baas_nestwachter", "nestwachter", "vadsrijk.z3bazen.tip.nestwachter", "vadsrijk.z3bazen.tip.ringen"),
    ("zone3", "vr_z3_baas_bleu", "bleu", "vadsrijk.z3bazen.tip.bleu", None),
    ("zone3", "vr_z3_baas_vahoegnir", "vahoegnir", "vadsrijk.z3bazen.tip.vahoegnir", None),
    # U-SITE: zones 4 + 5 (their tip cards sit in the z2bazen/z3bazen/z4bazen tip namespaces)
    ("zone4", "vr_z4_baas_zeeslurf", "zeeslurf", "vadsrijk.z4bazen.tip.zeeslurf", "vadsrijk.z3bazen.tip.zeeslurf"),
    ("zone4", "vr_z4_baas_vliegervanger", "vliegervanger", "vadsrijk.z4bazen.tip.vliegervanger", None),
    ("zone5", "vr_z5_baas_gorgonzola", "gorgonzola", "vadsrijk.z2bazen.tip.gorgonzola", "vadsrijk.z3bazen.tip.gorgonzola"),
    ("zone5", "vr_z5_baas_donderbuik", "donderbuik", "vadsrijk.z2bazen.tip.donderbuik", "vadsrijk.z3bazen.tip.donderbuik"),
    ("zone5", "vr_z5_baas_zilverstaart", "zilverstaart", "vadsrijk.z2bazen.tip.zilverstaart", "vadsrijk.z3bazen.tip.zilverstaart"),
    ("zone5", "vr_z5_baas_grauwe_droom", "grauwe_droom", "vadsrijk.z2bazen.tip.grauwe_droom", "vadsrijk.z3bazen.tip.grauwe_droom"),
    # U-SITE round 3: zone 6 (two field bosses + the three bosses of De Verzonken Sterrenwacht); the raid has its own pages (raid.py)
    ("zone6", "vr_z6_baas_wormelien", "wormelien", "vadsrijk.z6wezens.tip.baas_wormelien", None),
    ("zone6", "vr_z6_baas_zoutkolos", "zoutkolos", "vadsrijk.z6wezens.tip.baas_zoutkolos", None),
    ("zone6", "vr_z6_baas_astrolabiumwachter", "astrolabiumwachter", "vadsrijk.z6wezens.tip.baas_astrolabiumwachter", None),
    ("zone6", "vr_z6_baas_glimmertje", "glimmertje", "vadsrijk.z6wezens.tip.baas_glimmertje", None),
    ("zone6", "vr_z6_baas_sterrenguh", "sterrenguh", "vadsrijk.z6wezens.tip.baas_sterrenguh", None),
]
VERHAAL_BAZEN = {"vahoegnir", "mika_spion", "zilverstaart", "grauwe_droom"}     # story twists: behind the spoiler button
SOORT_BAAS = {"klodderreus": "verhaal", "koning_klodder": "wereldbaas", "mika_spion": "verhaal", "opperzwam": "kerker",
              "galmwolf": "wereldbaas", "boormeester": "arena", "spiegelzucht": "kerker", "kruinroofvogel": "wereldbaas",
              "taleggio": "kerker", "brokkel": "kerker", "roquefort": "kerker", "piepvel": "verhaal", "wortelwurger": "kerker",
              "nestwachter": "kerker", "bleu": "kerker", "vahoegnir": "verhaal",
              "zeeslurf": "wereldbaas", "vliegervanger": "kerker", "gorgonzola": "kerker", "donderbuik": "wereldbaas",
              "zilverstaart": "verhaal", "grauwe_droom": "verhaal",
              "wormelien": "wereldbaas", "zoutkolos": "wereldbaas", "astrolabiumwachter": "kerker", "glimmertje": "kerker", "sterrenguh": "kerker"}
# airship lines (the in-game travel map): stop -> (npc id of the captain, zone)
LUCHTSCHIP = {"knuffelkamp": ("kapitein", "zone1"), "dehaven": ("luchtschipper", "zone1"), "kandijkom": ("z2_kapitein", "zone2"),
              "honingtak": ("z3_kapitein", "zone3"),
              # U-SITE: line C (zone 3 -> Lampionhaven) and line D (the Opstijgpier -> Hoogvads); "poi:" = a POI position
              "lampionhaven": ("poi:haven_3_4_kapitein", "zone4"), "opstijgpier": ("poi:haven_4_5_kapitein", "zone4"),
              "hoogvads": ("poi:haven_5_4_kapitein", "zone5"),
              # U-SITE round 3: line E (Hoogvads' Staarthaven -> the Luchthaven of Rozenkloof)
              "staarthaven": ("poi:haven_5_6_vertrekbord", "zone5"), "rozenkloof": ("poi:haven_6_5_kapitein", "zone6")}
STOP_NAAM = {"lampionhaven": "Lampionhaven", "opstijgpier": "De Opstijgpier", "hoogvads": "Hoogvads (Luchthaven)",
             "staarthaven": "Hoogvads (Staarthaven)", "rozenkloof": "Rozenkloof (Luchthaven)"}
LIJNEN = [("knuffelkamp", "dehaven"), ("dehaven", "kandijkom"), ("dehaven", "honingtak"), ("kandijkom", "honingtak"),
          ("honingtak", "lampionhaven"), ("opstijgpier", "hoogvads"), ("staarthaven", "rozenkloof")]

GEHEIM = re.compile(r"^(geheim_(?!.*ingang$)|knuffel_\d|knuffel_ijs|.*aanwijzing_\d|.*_schat(_|$)|schat_\d|relief_|.*_relief_\d|imkerspoor)")
PUBLIEK = [  # (regex on the POI id, kind)
    (re.compile(r"^kampvuur_"), "kampvuur"),
    (re.compile(r"^uitkijk"), "uitkijk"),
    (re.compile(r"^(luchtschip_kapitein|luchtschip_haven_kapitein|dok_kapitein|haven_3_4_kapitein|haven_4_5_kapitein|haven_5_4_kapitein|haven_6_5_kapitein)$"), "luchtschip"),
    (re.compile(r"^terugportaal_"), "portaal"),
    (re.compile(r"^(deur_spiegelburcht_voor|werf_ingang|hart_poort|wrak_ingang|z5w_buik_luik|z6w_sterrenwacht_ingang)$"), "kerker"),
    (re.compile(r"^raid_toren_poort$"), "raid"),   # U-SITE round 3: the raid tower door
    (re.compile(r"^z5w_lift_[a-z]+_(boven|onder)$"), "lift"),
    (re.compile(r"^gondel_(berg|dal)$"), "gondel"),
    (re.compile(r"^(honinglift_boven|honinglift_onder)$"), "lift"),
]


def _lees_json(pad, standaard=None):
    try:
        with open(pad, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return standaard


class Bron:
    def __init__(self, root):
        self.root = root
        lang = os.path.join(root, "src", "main", "resources", "assets", "guhs", "lang")
        self.nl = _lees_json(os.path.join(lang, "nl_nl.json"), {})
        self.en = _lees_json(os.path.join(lang, "en_us.json"), {})
        self.rt = _lees_json(os.path.join(root, "docs", "wiki", "vadsrijk_runtime.json"), None)
        self.notes = []

    # ---- helpers ---------------------------------------------------------------------------------------------------------
    def t(self, key, standaard=None):
        v = self.nl.get(key)
        return v if v is not None else (standaard if standaard is not None else None)

    def tx(self, key, standaard=""):
        """(nl, en) of a key; en falls back on nl."""
        nl = self.nl.get(key, standaard)
        return nl, self.en.get(key, nl)

    @staticmethod
    def schoon(s):
        return (s or "").replace("%%", "%")

    def gebieden(self):
        if self.rt and self.rt.get("gebieden"):
            out = []
            seen = set()
            for g in self.rt["gebieden"]:
                if g["id"] in seen or g["id"] in NIET_OP_SITE or g["id"].startswith(("test", "t_")) or g["id"] == "zone2":
                    continue
                seen.add(g["id"])
                out.append(g)
            if not any(g["id"].startswith("z6_") for g in out):   # U-SITE: an older dump without zone 6
                out += [g for g in self._z6_gebieden() if g["id"] not in seen]
            return out
        return self._gebieden_uit_bron()

    def _gebieden_uit_bron(self):
        """Without the runtime dump: zone 1 from kern/Gebieden.java, zone 2 from Z2WereldPlekken.java, zone 3 from POI_Z3.md."""
        out = []
        java = os.path.join(self.root, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")
        rx = re.compile(r'new (?:Gebieden\.)?Gebied\("([a-z0-9_]+)", (-?\d+), (-?\d+), (-?\d+), (-?\d+), (\d+), (\d+), (true|false)\)')
        for f in ("kern/Gebieden.java", "zone2/wereld/Z2WereldPlekken.java"):
            try:
                tekst = open(os.path.join(java, f), encoding="utf-8").read()
            except OSError:
                continue
            for m in rx.finditer(tekst):
                if m[1] in NIET_OP_SITE:
                    continue
                out.append(dict(id=m[1], doos=[int(m[2]), int(m[3]), int(m[4]), int(m[5])], min_lv=int(m[6]), max_lv=int(m[7]),
                                stad=m[8] == "true"))
        try:
            tekst = open(os.path.join(self.root, "POI_Z3.md"), encoding="utf-8").read()
        except OSError:
            tekst = ""
        rc = re.compile(r"\| `(z3_[a-z_]+)` \| cylinder around \((-?\d+),(-?\d+)\), r (\d+)\.\.(\d+) \| (-?\d+)-(\d+) \| (\d+)-(\d+) \| (yes|no) \|")
        for m in rc.finditer(tekst):
            cx, cz, r0, r1 = int(m[2]), int(m[3]), int(m[4]), int(m[5])
            out.append(dict(id=m[1], doos=[cx - r1, cz - r1, cx + r1, cz + r1], min_lv=int(m[8]), max_lv=int(m[9]), stad=m[10] == "yes",
                            laag=dict(min_y=int(m[6]), max_y=int(m[7]), cx=cx, cz=cz, r_min=r0, r_max=r1)))
        out += self._z6_gebieden()
        if out:
            self.notes.append("no runtime dump: areas read from the Java/POI sources")
        return out

    def poi_bestanden(self):
        """Every `id` | x, y, z row of POI_VADSRIJK.md / POI_Z2.md / POI_Z3.md -> {id: (zone, section, (x, y, z))}."""
        out = {}
        for zone, f in (("zone1", "POI_VADSRIJK.md"), ("zone2", "POI_Z2.md"), ("zone3", "POI_Z3.md"), ("zone4", "POI_Z4.md"), ("zone5", "POI_Z5.md"),
                        ("zone6", "POI_Z6.md")):
            if zone == "zone6" and not os.path.exists(os.path.join(self.root, f)):
                out.update(self._z6_plekken())   # U-SITE: the same list, from the committed generated Java
                continue
            try:
                tekst = open(os.path.join(self.root, f), encoding="utf-8").read()
            except OSError:
                self.notes.append(f"{f} missing")
                continue
            sec = ""
            for regel in tekst.splitlines():
                if regel.startswith("## "):
                    sec = regel[3:]
                m = re.match(r"\| `([^`]+)` \| (-?\d+), (-?\d+), (-?\d+) \|", regel)
                if m:
                    out[(zone, m[1])] = (zone, sec, (int(m[2]), int(m[3]), int(m[4])))
        return out

    def _z6_java(self):
        p = os.path.join(self.root, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", "zone6", "wereld", "Z6WereldPlekken.java")
        try:
            return open(p, encoding="utf-8").read()
        except OSError:
            self.notes.append("Z6WereldPlekken.java missing: no zone-6 places")
            return ""

    def _z6_plekken(self):
        """U-SITE: zone 6's POIs from Z6WereldPlekken.ALLE (generated by poi6.py; POI_Z6.md is the same list, not committed)."""
        out = {}
        for m in re.finditer(r'Map\.entry\("([a-z0-9_]+)", new BlockPos\((-?\d+), (-?\d+), (-?\d+)\)\)', self._z6_java()):
            out[("zone6", m[1])] = ("zone6", "Z6WereldPlekken", (int(m[2]), int(m[3]), int(m[4])))
        return out

    def _z6_gebieden(self):
        """U-SITE: zone 6's areas from Z6WereldPlekken.GEBIEDEN (when the runtime dump has none)."""
        out = []
        for m in re.finditer(r'new Gebied\("([a-z0-9_]+)", (-?\d+), (-?\d+), (-?\d+), (-?\d+), "[a-z_]+", (\d+), (\d+), (true|false)\)', self._z6_java()):
            out.append(dict(id=m[1], doos=[int(m[2]), int(m[3]), int(m[4]), int(m[5])], min_lv=int(m[6]), max_lv=int(m[7]), stad=m[8] == "true"))
        return out

    # ---- the pieces ------------------------------------------------------------------------------------------------------
    def zone_van_gebied(self, gid):
        for pre, zone in ZONE_PREFIX:
            if gid.startswith(pre):
                return zone
        return "zone1"

    def gebied_naam(self, gid):
        """(nl, en) of an area: the kern lang key, else POI_Z5.md's district table, else GEBIED_NAAM."""
        k = "vadsrijk.kern.gebied." + gid
        if k in self.nl:
            return self.tx(k, gid)
        if not hasattr(self, "_wijken"):
            self._wijken = {}
            try:
                tekst = open(os.path.join(self.root, "POI_Z5.md"), encoding="utf-8").read()
                for m in re.finditer(r"^\| `(z5_[a-z_]+)` \| [^|]+\| [a-z_]+ \| ([^|]+?) \|", tekst, re.M):
                    self._wijken[m[1]] = m[2].replace("Hoogvads - ", "").strip()
            except OSError:
                pass
        nl = GEBIED_NAAM.get(gid) or self._wijken.get(gid) or gid.split("_", 1)[-1].replace("_", " ").capitalize()
        if nl[:1].islower():
            nl = nl[0].upper() + nl[1:]
        return nl, nl

    @staticmethod
    def in_gebied(g, x, y, z):
        x0, z0, x1, z1 = g["doos"]
        if not (x0 <= x <= x1 + 0.999 and z0 <= z <= z1 + 0.999):
            return False
        lg = g.get("laag")
        if lg:
            if lg.get("r_max"):
                dx, dz = x - (lg["cx"] + .5), z - (lg["cz"] + .5)
                d2 = dx * dx + dz * dz
                if d2 > lg["r_max"] ** 2 or (lg.get("r_min") and d2 < lg["r_min"] ** 2):
                    return False
            if y is not None and not (lg.get("min_y", -999) <= y < lg.get("max_y", 9999) + 1):
                return False
        return True

    def gebied_op(self, gebieden, x, y, z):
        # U-SITE: the SMALLEST matching area (zone 5's rooms/arenas lie inside the big districts; zones 1-3 are unchanged by this)
        beste, opp = None, None
        for g in gebieden:
            if self.in_gebied(g, x, y, z):
                b = g["doos"]
                a = (b[2] - b[0] + 1) * (b[3] - b[1] + 1)
                if beste is None or a < opp:
                    beste, opp = g, a
        return beste

    def build(self):
        gebieden = self.gebieden()
        pois = self.poi_bestanden()
        self._pois = pois
        rt = self.rt or {}
        npc_plek = {k: v for k, v in (rt.get("npcs", {}).get("plek", {}) or {}).items()}
        poi_plek = {k: v for k, v in (rt.get("npcs", {}).get("poi", {}) or {}).items()}
        for (zone, pid), (_, _, pos) in pois.items():
            poi_plek.setdefault(pid, list(pos))
        data = dict(meta=dict(runtime=bool(self.rt), bron="tools/wiki_site/vadsrijk/bron.py"))

        # areas -> zones
        gb_out = []
        for g in gebieden:
            nl, en = self.gebied_naam(g["id"])
            gb_out.append(dict(id=g["id"], naam=nl, naam_en=en, zone=self.zone_van_gebied(g["id"]), lv=[g["min_lv"], g["max_lv"]],
                               stad=g["stad"], doos=g["doos"], laag=g.get("laag")))
        data["gebieden"] = gb_out

        # secrets: count per area, never a position
        geheimen = collections.Counter()
        for (zone, pid), (_, sec, (x, y, z)) in pois.items():
            if GEHEIM.match(pid):
                g = self.gebied_op(gebieden, x, y, z)
                geheimen[g["id"] if g else zone] += 1
        data["geheimen"] = dict(geheimen)

        # public POIs (allowlist)
        punten = []
        for (zone, pid), (_, sec, (x, y, z)) in sorted(pois.items()):
            if GEHEIM.match(pid):
                continue
            for rx, soort in PUBLIEK:
                if rx.match(pid):
                    g = self.gebied_op(gebieden, x, y, z)
                    naam = self._poi_naam(pid, soort, g)
                    if any(q["soort"] == soort and q["pos"] == [x, y, z] for q in punten):
                        break   # U-SITE: zone 6 has alias ids on one spot (kampvuur_duinen = kampvuur_duinput)
                    punten.append(dict(id=f"{zone}:{pid}", soort=soort, zone=zone, pos=[x, y, z], naam=naam,
                                       gebied=g["id"] if g else None))
                    break
        data["punten"] = punten

        # zones
        zones = []
        for z in ZONES:
            nl, en = self.tx("vadsrijk.voltooiing.zone." + z["boek"], z["id"])
            gs = [g for g in gb_out if g["zone"] == z["id"]]
            zones.append(dict(id=z["id"], nr=z["nr"], naam=nl, naam_en=en, lv=list(z["lv"]), kleur=z["kleur"], hub=z["hub"],
                              ring=z["ring"], gebieden=[g["id"] for g in gs if g["id"] != z["ring"]]))
        data["zones"] = zones

        data["npcs"], npc_zone = self._npcs(npc_plek, rt)
        data["quests"] = self._quests(npc_plek, poi_plek, npc_zone)
        for n in data["npcs"]:
            n["quests"] = [q["id"] for q in data["quests"] if n["id"] in (q["gever"], q["inleveren"])]
        data["wezens"] = self._wezens(rt, gebieden)
        data["items"], data["sets"] = self._items(rt, data["wezens"])
        data["bazen"] = self._bazen(rt, gebieden)
        # boss loot: named boss items by prefix
        for b in data["bazen"]:
            b["buit"] = [i["id"] for i in data["items"] if b["id"] in i.get("bronnen_baas", [])]
        self._baas_extra(data, rt)   # U-SITE: every other drop with its chance
        from . import raid   # U-SITE round 3: the raid (zone 6)
        data["raid"] = raid.data(self)
        data["kristallen"] = self._kristallen(rt)
        data["sterren"] = rt.get("sterren")
        data["zeldzaamheden"] = [dict(r, naam=self.t("vadsrijk.kern.zeldzaamheid." + r["id"], r["id"])) for r in rt.get("zeldzaamheden", [])] \
            or [dict(id=k, naam=self.t("vadsrijk.kern.zeldzaamheid." + k, k), kleur=c) for k, c in
                (("gewoon", "#B0B0B0"), ("ongewoon", "#55DD55"), ("zeldzaam", "#4A9DFF"), ("episch", "#B45CFF"),
                 ("legendarisch", "#FF9A1F"), ("guhddelijk", "#FF5FC8"))]
        data["klassen"] = self._klassen(rt)
        data["huisdieren"] = self._huisdieren()
        data["rijdieren"] = self._rijdieren(rt)
        data["routes"] = self._routes(npc_plek)
        data["gids"] = self._gids()
        # U-SITE: the systems of the night round (t-ambacht, t-puzzels, t-kleding, t-rijdier)
        data["ambachten"] = self._ambachten()
        data["puzzels"] = self._puzzels()
        from . import geheimen   # U-SITE round 3: every secret WITH its location (behind spoiler clicks on the page + an off-by-default map layer)
        data["geheimen_lijst"] = geheimen.data(self, pois, gebieden, punten, data["puzzels"]["lijst"])
        data["kleding"] = self._kleding()
        data["rij_uiterlijk"] = self._rij_uiterlijk()
        data["krachten"] = self._krachten()
        data["lore"] = self._lore()
        data["vertaal"] = self._vertaal(data)
        if not self.rt:
            self.notes.append("docs/wiki/vadsrijk_runtime.json missing: no levels/stats/drops/positions from the game "
                              "(run `gradlew runGameTestServer -Pgt=VadsrijkWeb` and copy run/vadsrijk_runtime.json)")
        return data

    def _poi_naam(self, pid, soort, g):
        gnaam = self.gebied_naam(g["id"])[0] if g else ""
        if soort == "kampvuur":
            return "Kampvuur" + (f" ({gnaam})" if gnaam else "")
        if soort == "uitkijk":
            kaal = pid.replace("uitkijk_", "").replace("uitkijkpunt_", "")
            nl = self.t(f"vadsrijk.wereld.uitkijk.{kaal}") or self.t(f"vadsrijk.wereld.uitkijk.{kaal}.titel") or self.t(f"vadsrijk.wereld.uitkijk.z6_{pid}")
            return "Uitkijkpunt " + (nl or kaal.replace("_", " ").title())
        if soort == "luchtschip":
            return "Luchtschip" + (f" ({gnaam})" if gnaam else "")
        if soort == "portaal":
            return "Terugportaal" + (f" ({gnaam})" if gnaam else "")
        if soort == "kerker":
            return {"deur_spiegelburcht_voor": "De Spiegelburcht", "werf_ingang": "De Kaaszeppelinwerf", "hart_poort": "Het Hart van de Boom",
                    "wrak_ingang": "Het Wrak van de Vliegervanger", "z5w_buik_luik": "De Buikgangen (luik)",
                    "z6w_sterrenwacht_ingang": "De Verzonken Sterrenwacht (luik)"}.get(pid, pid)
        if soort == "raid":
            return "De Sterrendovende Toren (raid)"
        if soort == "gondel":
            return "Gondel (" + ("berg" if "berg" in pid else "dal") + ")"
        if soort == "lift":
            if pid.startswith("z5w_lift_"):
                return "Wolklift " + pid.split("_")[2] + " (" + ("boven" if "boven" in pid else "beneden") + ")"
            return "Honinglift (" + ("boven" if "boven" in pid else "beneden") + ")"
        return pid

    def _npcs(self, plek, rt):
        out, zone_van = [], {}
        rol = (rt.get("npcs", {}) or {}).get("rol", {})
        for k, v in self.nl.items():
            if not k.startswith("vadsrijk.npc.naam."):
                continue
            nid = k[len("vadsrijk.npc.naam."):]
            zone = self.zone_van_gebied(nid)
            zone_van[nid] = zone
            out.append(dict(id=nid, naam=v, zone=zone, waar=self.t("vadsrijk.npc.plek.npc." + nid, ""), pos=plek.get(nid),
                            rol=rol.get(nid), praat=[self.nl[f"vadsrijk.npc.praat.{nid}.{i}"] for i in range(1, 9)
                                                    if f"vadsrijk.npc.praat.{nid}.{i}" in self.nl]))
        return out, zone_van

    def _quests(self, npc_plek, poi_plek, npc_zone):
        out = []
        base = os.path.join(self.root, "src", "main", "resources", "data", "guhs", "vadsrijk", "npc")
        for z in ZONES:
            q = _lees_json(os.path.join(base, z["quests"]), {}).get("quests", [])
            for i, qd in enumerate(q):
                qid = qd["id"]
                p = f"vadsrijk.npc.q.{qid}."
                doelen = []
                for j, d in enumerate(qd["doelen"]):
                    pos = None
                    geheim = d["soort"] == "geheim" or qd["soort"] == "geheim"
                    if not geheim:
                        ref = d.get("plek") or (("npc:" + d["doel"]) if d["soort"] == "praat" else d["doel"] if str(d["doel"]).startswith("poi:") else "")
                        if ref.startswith("npc:"):
                            pos = npc_plek.get(ref[4:])
                        elif ref.startswith("poi:") and not GEHEIM.match(ref[4:]):
                            pos = poi_plek.get(ref[4:])
                    doelen.append(dict(tekst=self.t(p + f"doel.{j}", d["doel"]), soort=d["soort"], n=d["n"], pos=pos, geheim=geheim))

                def dialoog(soort, wie):
                    regels = []
                    for j, spreker in enumerate(qd[soort]):
                        tekst = self.t(p + f"{soort}.{j}")
                        if tekst is None:
                            continue
                        naam = "Jij" if spreker == "jij" else "Verteller" if spreker == "verteller" else \
                            self.t("vadsrijk.npc.naam." + (wie if spreker == "npc" else spreker), spreker)
                        regels.append([spreker if spreker in ("jij", "verteller") else "npc", naam, tekst])
                    return regels
                bel = qd["beloning"]
                out.append(dict(id=qid, zone=z["id"], volgorde=i, soort=qd["soort"], titel=self.t(p + "titel", qid),
                                tekst=self.t(p + "tekst", ""), bezig=self.t(p + "bezig", ""), lv=qd["lv"], na=qd["na"],
                                gever=qd["gever"], inleveren=qd["inleveren"], doelen=doelen,
                                beloning=dict(xp=bel["xp"], munten=bel["munten"], wapen=bel["wapen"], loot=bel["loot"],
                                              items=[[self._itemnaam(i[0]), i[1]] for i in bel["items"]]),
                                aanbod=dialoog("aanbod", qd["gever"]), klaar=dialoog("klaar", qd["inleveren"]),
                                gever_pos=npc_plek.get(qd["gever"])))
        return out

    def _itemnaam(self, iid):
        ns, _, p = iid.partition(":")
        for k in (f"item.{ns}.{p}", f"block.{ns}.{p}"):
            if k in self.nl:
                return self.nl[k]
        return p.replace("_", " ")

    def _wezens(self, rt, gebieden):
        sp_per = collections.defaultdict(list)
        for s in rt.get("spawners", []):
            sp_per[s["type"].split(":")[-1]].append(s)
        lt = rt.get("loottabellen", {})
        out = []
        for k, naam in self.nl.items():
            if not k.startswith("entity.guhs.vr_"):
                continue
            eid = k[len("entity.guhs."):]
            if eid in NIET_WEZENS or "_baas" in eid or eid.startswith(("vr_eco_", "vr_baas", "vr_klodderreus")):
                continue
            m = re.match(r"vr_(?:(z2|z3|z4|z5|z6)_)?mob_(.+)$", eid)
            if not m:
                continue
            zone = {"z2": "zone2", "z3": "zone3", "z4": "zone4", "z5": "zone5", "z6": "zone6"}.get(m[1], "zone1")
            kort = m[2]
            tip_pre = {"zone1": "vadsrijk.wezens.tip.", "zone2": "vadsrijk.z2wezens.tip.", "zone3": "vadsrijk.z3wezens.tip.",
                       "zone4": "vadsrijk.z4wezens.tip.", "zone5": "vadsrijk.z5wezens.tip.", "zone6": "vadsrijk.z6wezens.tip."}[zone]
            sps = sp_per.get(eid, [])
            lvs = [s["min_lv"] for s in sps] + [s["max_lv"] for s in sps]
            gebied_ids = []
            for s in sps:
                x, y, z = s["centrum"]
                g = self.gebied_op(gebieden, x, y if s.get("grot") else None, z)
                if g and g["id"] not in gebied_ids:
                    gebied_ids.append(g["id"])
            tabel = None
            for key in (f"wezens:{kort}", f"z2wezens:{kort}", f"z3wezens:{kort}", f"z4wezens:{kort}", f"z5wezens:{kort}", f"z6wezens:{kort}"):
                if key in lt:
                    tabel = dict(lt[key], id=key)
            elems = sorted({s["element"] for s in sps if s["element"] != "geen"})
            out.append(dict(id=eid, kort=kort, naam=naam, zone=zone, tip=self.schoon(self.t(tip_pre + kort, "")),
                            critter=kort in CRITTERS, lv=[min(lvs), max(lvs)] if lvs else None, elementen=elems,
                            elite=round(max([s["elite_kans"] for s in sps] or [0]) * 100, 1), gebieden=gebied_ids,
                            respawn_s=min([s["respawn_s"] for s in sps] or [0]) or None, aantal_spawners=len(sps), loot=tabel))
        return sorted(out, key=lambda w: (w["zone"], w["lv"][0] if w["lv"] else 99, w["naam"]))

    def _bazen(self, rt, gebieden):
        arenas = {a["id"]: a for a in rt.get("arenas", [])}
        huisdier_van = {h.get("baas"): h["id"] for h in rt.get("huisdieren", []) if h.get("baas")}
        out = []
        for zone, eid, kort, tip, tip2 in BAZEN:
            naam = self.t("entity.guhs." + eid, kort)
            arena = next((a for aid, a in arenas.items() if aid.split(":")[-1].replace("arena_", "") in (kort, "baron_" + kort)
                          or aid.endswith(":" + kort) or kort in aid.split(":")[-1]), None)
            g = self.gebied_op(gebieden, *arena["centrum"]) if arena else None
            out.append(dict(id=kort, entity=eid, naam=naam, zone=zone, soort=SOORT_BAAS.get(kort, "baas"),
                            tip=self.schoon(self.t(tip, "") if tip else ""), tip2=self.schoon(self.t(tip2, "") if tip2 else ""),
                            tip2_titel=self.t(tip2 + ".titel", "") if tip2 else "", spoiler=kort in VERHAAL_BAZEN,
                            arena=dict(id=arena["id"], pos=arena["centrum"], respawn_s=arena["respawn_s"]) if arena else None,
                            gebied=g["id"] if g else None,
                            huisdier=huisdier_van.get("guhs:" + eid) or next((h for h in ("klodderreus", "koning_klodder", "galmwolf", "kruinroofvogel", "piepje", "vahoegje")
                                           if (h == kort) or (h == "piepje" and kort == "piepvel") or (h == "vahoegje" and kort == "vahoegnir")), None),
                            vahoeg=zone == "zone3" and SOORT_BAAS.get(kort) == "kerker"))
        return out

    def _items(self, rt, wezens):
        lt = rt.get("loottabellen", {})
        bronnen = collections.defaultdict(list)
        naam_tabel = {}
        for w in wezens:
            if w.get("loot"):
                naam_tabel[w["loot"]["id"]] = w["id"]
        for tid, t in lt.items():
            for bt in t.get("regels", {}):
                bronnen[bt].append(tid)
        sets = {}
        for sid, stukken in (rt.get("sets") or {}).items():
            key = {"klodder": "vadsrijk.wezens.set.", "zwam": "vadsrijk.wezens.set.", "kristal": "vadsrijk.z2wezens.set.",
                   "mika": "vadsrijk.z2wezens.set."}.get(sid, "vadsrijk.z3wezens.set.")
            if sid in Z6_SETS:   # U-SITE round 3: the zone-6 sets (bonuses at 2/4/6) + the raid set
                k6 = "vadsrijk.z6wezens.set." if sid != "sterrenzijde" else "vadsrijk.z6raid.set."
                naam = self.t(k6 + sid) or self.t(k6 + sid + ".naam") or Z6_SETS[sid]
                sets[sid] = dict(id=sid, naam=naam, stukken=stukken, bonus2=self.schoon(self.t(k6 + sid + ".2", "")),
                                 bonus4=self.schoon(self.t(k6 + sid + ".4", "")), bonus6=self.schoon(self.t(k6 + sid + ".6", "")))
                if sid == "sterrenzijde" and not sets[sid]["bonus2"]:   # the raid set: its bonus text is one effect line
                    sets[sid].update(bonus2="(2) Verdediging +15 en Vads +150", bonus4="(4) Schade tegen bazen +10%")
                continue
            for kk in (key, "vadsrijk.z4wezens.set.", "vadsrijk.items.set."):   # U-SITE: zone-4 sets + the items 2.0 names
                if kk + sid in self.nl:
                    key = kk
                    break
            if sid == "wolkwacht":   # U-SITE: zone 5's set: keys without the set id, bonuses at 2/4/6 pieces
                k5 = "vadsrijk.z5wezens.set."
                sets[sid] = dict(id=sid, naam=self.t(k5 + "stukken", "Wolkwachtset").split(" (")[0], stukken=stukken,
                                 bonus2=self.schoon(self.t(k5 + "2", "")), bonus4=self.schoon(self.t(k5 + "4", "")),
                                 bonus6=self.schoon(self.t(k5 + "6", "")))
                continue
            sets[sid] = dict(id=sid, naam=self.t(key + sid, sid), stukken=stukken,
                             bonus2=self.schoon(self.t(key + sid + ".2", "")), bonus4=self.schoon(self.t(key + sid + ".4", "")))
        set_van = {s: sid for sid, d in sets.items() for s in d["stukken"]}
        benoemd = self._benoemd()
        out = []
        types = rt.get("basistypes") or [dict(id=k[len("vadsrijk.loot."):]) for k in self.nl if re.match(r"vadsrijk\.loot\.[a-z0-9_]+$", k)]
        for bt in types:
            iid = bt["id"]
            naam = self.t("vadsrijk.loot." + iid) or (self._itemnaam(bt["item"]) if bt.get("item") else None) or iid.replace("_", " ").capitalize()
            flav = [self.nl[f"vadsrijk.loot.{iid}.flavour.{i}"] for i in range(8) if f"vadsrijk.loot.{iid}.flavour.{i}" in self.nl]
            zm = re.match(r"z([2-6])(?:baas|droom|raid)_", iid)
            zone = f"zone{zm[1]}" if zm else ("zone5" if iid.startswith("mw_") else "zone6" if set_van.get(iid) in Z6_SETS else None)
            tabellen = bronnen.get(iid, [])
            if zone is None:
                zs = {(f"zone{t[1]}" if re.match(r"z[2-6]", t) else "zone1") for t in tabellen}
                zone = sorted(zs)[0] if zs else "zone1"
            baas = None
            m = re.match(r"(?:z[2-6])?baas_(.+)$", iid)
            bron_baas = []
            for tid in tabellen:
                kort = tid.split(":")[-1]
                for b in BAZEN:
                    if b[2] in kort:
                        bron_baas.append(b[2])
            bn = benoemd.get(iid)
            if bn:
                bron_baas = [bn["baas"]]
            if iid.startswith("z6raid_"):   # U-SITE: the raid's own loot (raid.py lists it)
                bron_baas, m = [], None
            elif m and not bron_baas:
                baas = m[1]
            out.append(dict(id=iid, naam=naam, zone=zone, slot=bt.get("slot"), soort=bt.get("soort"), klasse=bt.get("klasse"),
                            element=bt.get("element"), per_level=bt.get("per_level"), flavour=flav, set=set_van.get(iid),
                            bronnen=[naam_tabel.get(t, t) for t in tabellen if t in naam_tabel],
                            bronnen_baas=sorted(set(bron_baas)), benoemd=bool(m), item=bt.get("item"),
                            vaste_zeldzaamheid=bn["zeldzaamheid"] if bn else None, baaskans=bn["kans"] if bn else None))
        return sorted(out, key=lambda i: (i["zone"], i["slot"] or "", i["naam"])), list(sets.values())

    def _benoemd(self):
        """The named boss items: new Benoemd("<type>", ITEM, Zeldzaamheid.X, "<boss>", <chance>) in the bazen loot classes."""
        out = {}
        java = os.path.join(self.root, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk")
        rx = re.compile(r'new Benoemd\("([a-z0-9_]+)",\s*[A-Z_]+,\s*Zeldzaamheid\.([A-Z]+),\s*"([a-z_]+)",\s*([0-9.]+)\)')
        for f in glob.glob(os.path.join(java, "**", "*Loot.java"), recursive=True):
            for m in rx.finditer(open(f, encoding="utf-8").read()):
                out[m[1]] = dict(zeldzaamheid=m[2].lower(), baas=m[3], kans=float(m[4]))
        return out

    def _kristallen(self, rt):
        out = []
        for k in rt.get("kristallen", []):
            out.append(dict(k, naam=self._itemnaam(k["item"])))
        return out

    def _klassen(self, rt):
        out = []
        ids = [k["id"] for k in rt.get("klassen", [])] or ["knuffelridder", "pluisjager", "vadsmagier"]
        per = {k["id"]: k for k in rt.get("klassen", [])}
        for kid in ids:
            nl = self.t("vadsrijk.klassen.klasse." + kid) or self.t("vadsrijk.kern.klasse." + kid) or kid.capitalize()
            uitleg = " ".join(x for x in (self.t(f"vadsrijk.klassen.kies.rol.{kid}"), self.t(f"vadsrijk.klassen.kies.onder.{kid}")) if x)
            out.append(dict(id=kid, naam=nl, naam_en=nl, wapens=per.get(kid, {}).get("wapens", []), per_level=per.get(kid, {}).get("per_level"),
                            uitleg=uitleg))
        return out

    def _huisdieren(self):
        out = []
        for k in self.nl:
            m = re.match(r"vadsrijk\.huisdier\.([a-z_]+)\.naam$", k)
            if m:
                h = m[1]
                out.append(dict(id=h, naam=self.nl[k], tekst=self.t(f"vadsrijk.huisdier.{h}.tekst", ""), bron=self.t(f"vadsrijk.huisdier.{h}.bron", "")))
        return out

    def _rijdieren(self, rt):
        out = []
        per = {r["id"]: r for r in rt.get("rijdieren", [])}
        for rid in per or ("pluimguh", "knuffelhert", "guhwolf", "wolkenschaapje", "slakkie"):
            r = per.get(rid, {})
            out.append(dict(id=rid, naam=self.t("vadsrijk.economie.rijdier." + rid, rid), tekst=self.t("vadsrijk.economie.rijdier.eigen." + rid, ""),
                            bron=r.get("bron"), prijs=r.get("prijs"), zweeft=r.get("zweeft"), snelheid=[r.get("snelheid_1"), r.get("snelheid_max")]))
        return out

    def _routes(self, npc_plek):
        stops = {}
        pois = getattr(self, "_pois", {})
        for sid, (npc, zone) in LUCHTSCHIP.items():
            if npc.startswith("poi:") and (zone, npc[4:]) in pois:
                stops[sid] = dict(id=sid, zone=zone, pos=list(pois[(zone, npc[4:])][2]), naam=STOP_NAAM.get(sid, sid))
            elif npc in npc_plek:
                stops[sid] = dict(id=sid, zone=zone, pos=npc_plek[npc], naam=self.t("vadsrijk.npc.naam." + npc, npc))
        return [dict(van=a, naar=b, a=stops[a]["pos"], b=stops[b]["pos"], za=stops[a]["zone"], zb=stops[b]["zone"])
                for a, b in LIJNEN if a in stops and b in stops]

    def _gids(self):
        out = []
        for k, v in self.nl.items():
            m = re.match(r"vadsrijk\.(?:npc|z3npc)\.gids\.([a-z0-9_]+)\.titel$", k)
            if m:
                tk = k[:-len(".titel")] + ".tekst"
                out.append(dict(id=m[1], titel=v, tekst=self.schoon(self.t(tk, ""))))
        return out

    def _lore(self):
        """The prose of the npc slices' wiki helpers (tools/features/vadsrijk*_npc_wiki.py: spoiler-free by their own rule)."""
        out = []
        for f in sorted(glob.glob(os.path.join(self.root, "tools", "features", "vadsrijk*npc_wiki.py"))):
            ns = {}
            try:
                exec(compile(open(f, encoding="utf-8").read(), f, "exec"), ns)   # plain data modules (WIKI = {...})
            except Exception as e:   # noqa: BLE001
                self.notes.append(f"lore: {os.path.basename(f)}: {e}")
                continue
            w = ns.get("WIKI", {})
            zone = "zone6" if "z6" in f else "zone5" if "z5" in f else "zone4" if "z4" in f else "zone3" if "z3" in f else "zone2" if "z2" in f else "zone1"
            for sid, v in (w.get("systemen") or {}).items():
                out.append(dict(id=sid, zone=zone, titel=v[0], tekst=v[2], stukken=[]))
            per = {o["id"]: o for o in out}
            for pid, kop, tekst in w.get("tekst", []):
                sid = pid.split("/", 1)[-1]
                if sid in per:
                    per[sid]["stukken"].append([kop, tekst])
        return out

    # ---- U-SITE: all boss drops ------------------------------------------------------------------------------------------
    def _baas_extra(self, data, rt):
        """Per boss: b["kist"] (its chest loot table, if the dump has one) and b["extra"] = [[kind, name, chance text NL, EN]]."""
        lt = rt.get("loottabellen", {})
        uit = self._java(os.path.join("rijdier", "Uiterlijk.java")) + self._java(os.path.join("rijdier", "Aankleden.java"))
        looks = collections.defaultdict(list)   # entity -> [(kind, name)]
        soort = None
        for regel in uit.splitlines():
            m = re.search(r"vacht\(RijdierSoort\.([A-Z_]+)", regel)
            if m:
                soort = m[1].lower()
            for m in re.finditer(r'\{"([a-z_]+)", 0x[0-9A-Fa-f]+, Bron\.baas\("guhs:([a-z0-9_]+)"\)\}', regel):
                rnaam = self.t("vadsrijk.economie.rijdier." + (soort or ""), soort or "")
                looks[m[2]].append(("vacht", f'{self.t(f"vadsrijk.rijdier.vacht.{soort}.{m[1]}", m[1])} ({rnaam})'))
            m = re.search(r'new Variant\(As\.(PATROON|TUIG), "([a-z_]+)".*Bron\.baas\("guhs:([a-z0-9_]+)"\)', regel)
            if m:
                looks[m[3]].append((m[1].lower(), self.t(f"vadsrijk.rijdier.{m[1].lower()}.{m[2]}", m[2])))
            m = re.search(r'SOORT_BRON\.put\(RijdierSoort\.([A-Z_]+), Bron\.baas\("guhs:([a-z0-9_]+)"\)', regel)
            if m:
                looks[m[2]].append(("rijdier", self.t("vadsrijk.economie.rijdier." + m[1].lower(), m[1].lower())))
            m = re.search(r'PLUIM_BRON\.put\(PluimKleur\.([A-Z_]+), Bron\.baas\("guhs:([a-z0-9_]+)"\)', regel)
            if m:
                looks[m[2]].append(("rijdier", "Pluimguh " + self.t("vadsrijk.economie.pluimkleur." + m[1].lower(), m[1].lower())))
        kern = {"zone1": "Zwamkern", "zone2": "Splinterkern", "zone3": "Wortelkern"}
        huisdier = {h["id"]: h for h in data.get("huisdieren", [])} if data.get("huisdieren") else {}
        for b in data["bazen"]:
            eid = b["entity"]
            tabel = lt.get(f"economie:baas_{b['id']}") or lt.get(f"{b['zone'][0]}{b['zone'][-1]}bazen:{b['id']}")
            if not tabel and b["zone"] == "zone5" and b["soort"] != "verhaal":
                tabel = lt.get("z5wezens:baas")
            b["kist"] = tabel
            ex = []
            for kind, naam in looks.get(eid, []):
                if kind == "rijdier":
                    ex.append(["rijdier", naam, "10% per speler per overwinning; zeker na 15 keer (geduldteller)",
                               "10% per player per win; guaranteed after 15 tries (pity counter)"])
                else:
                    ex.append([kind, naam, "de eerste keer dat je hem verslaat", "the first time you beat it"])
            if b.get("huisdier"):
                ex.append(["huisdier", self.t(f"vadsrijk.huisdier.{b['huisdier']}.naam", b["huisdier"]),
                           self.t(f"vadsrijk.huisdier.{b['huisdier']}.bron", "") or "eerste overwinning", None])
            zp = {"zone1": "", "zone2": "z2_", "zone3": "z3_", "zone4": "z4_", "zone5": "z5_", "zone6": "z6_"}[b["zone"]]
            trofee = self.t(f"item.guhs.vr_{zp}baas_trofee_{b['id']}")
            if trofee:
                ex.append(["trofee", trofee, "zeker, bij je eerste overwinning (deco voor thuis)", "sure, on your first win (decoration for home)"])
            if b["soort"] != "verhaal" or b["id"] in ("klodderreus",):
                if b["zone"] in kern:
                    ex.append(["kern", kern[b["zone"]], "15% per helper", "15% per helper"])
                ex.append(["groei", "Groeipluis", "1 per helper, elke overwinning", "1 per helper, every win"])
            if b.get("vahoeg"):
                ex.append(["veer", "Gouden Veren", "in Vahoeg-vorm", "in Vahoeg form"])
                ex.append(["kleding", "Schemervleugels", "3% in Vahoeg-vorm (laatste baas van de kerker 8%)", "3% in Vahoeg form (last boss of the dungeon 8%)"])
            if b["zone"] == "zone6":   # U-SITE round 3: u-z6wezens' extras (Z6BazenLoot)
                if b["id"] in ("wormelien", "zoutkolos"):
                    ex.append(["veer", "Gouden Veren", "2 bij je eerste overwinning, daarna 25% kans op 1", "2 on your first win, then a 25% chance of 1"])
                elif b["id"] == "sterrenguh":
                    ex.append(["veer", "Gouden Veer", "1, elke overwinning", "1, every win"])
                ex.append(["stof", "Sterrenstof", "3-6, elke overwinning", "3-6, every win"])
            b["extra"] = ex

    # ---- U-SITE: the new systems ------------------------------------------------------------------------------------------
    def _java(self, rel):
        try:
            return open(os.path.join(self.root, "src", "main", "java", "nl", "juiced", "guhs", "feature", "vadsrijk", rel), encoding="utf-8").read()
        except OSError:
            self.notes.append(f"{rel} missing")
            return ""

    def _ambachten(self):
        """The 9 crafts: names, gathering spots per zone (lang groups) and every recipe of ambacht/Recepten.java (its loops expanded)."""
        tekst = self._java(os.path.join("ambacht", "Recepten.java"))
        lijsten = {m[1]: re.findall(r'"([a-z_]+)"', m[2]) for m in re.finditer(r"\b([A-Z0-9]+) = List\.of\(([^)]*)\)", tekst)}
        lijsten.update({m[1]: re.findall(r'"([a-z_]+)"', m[2]) for m in re.finditer(r",\s*([A-Z0-9]+) = List\.of\(([^)]*)\)", tekst)})
        regels = []
        lus = re.compile(r"for \(String s : List\.of\(([^)]*)\)\) \{(.*?)\n\s*\}", re.S)
        for m in lus.finditer(tekst):
            for naam in re.findall(r'"([a-z_]+)"', m[1]):
                regels.append(m[2].replace('s + "_', f'"{naam}_').replace('" + s', f'{naam}"'))
        regels.append(lus.sub("", tekst))
        rx = re.compile(r'r\("([a-z0-9_]+)", Ambacht\.([A-Z]+), (\d+), (\d+), "([a-z]+:[a-z0-9_]+)", (\d+)((?:, i\([^)]*\))*)\);')
        recepten = []
        for blok in regels:
            for m in rx.finditer(blok):
                ingr = []
                for im in re.finditer(r"i\((\d+), ([^)]*)\)", m[7] or ""):
                    opties = re.findall(r'"([a-z_]+)"', im[2])
                    if not opties:
                        lijst = im[2].strip()
                        naam = {"VIS1": "vis (tier 1)", "VIS2": "vis (tier 2)", "VIS3": "vis (tier 3)", "HONING": "honing"}.get(lijst, lijst.lower())
                    else:
                        naam = " / ".join(self.t("item.guhs.vr_amb_" + o) or self.t("item.guhs.vr_" + o) or o.replace("_", " ") for o in opties)
                    ingr.append([int(im[1]), naam])
                soort, _, doel = m[5].partition(":")
                recepten.append(dict(id=m[1], ambacht=m[2].lower(), level=int(m[3]), tier=int(m[4]), soort=soort, doel=doel,
                                     aantal=int(m[6]), naam=self.t("vadsrijk.ambacht.recept." + m[1], m[1]), in_=ingr))
        recepten.sort(key=lambda r: (r["ambacht"], r["level"], r["naam"]))
        plekken = collections.defaultdict(list)
        for k, v in self.nl.items():
            m = re.match(r"vadsrijk\.ambacht\.groep\.(z(\d)_([a-z]+)_[a-z0-9_]+)$", k)
            if m:
                plekken[(m[3], int(m[2]))].append(v)
        soort_van = {"pluk": "plukken", "mijn": "mijnen", "hout": "houthakken", "vis": "vissen", "korf": "imkeren"}
        verzamel = collections.defaultdict(dict)
        for (s_, z), lijst in plekken.items():
            verzamel[soort_van.get(s_, s_)][z] = lijst
        namen = {}
        for k, v in self.nl.items():
            mm = re.match(r"vadsrijk\.ambacht\.naam\.([a-z]+)$", k)
            if mm:
                namen[mm[1]] = v
        if not recepten:
            self.notes.append("ambachten: no recipes parsed from Recepten.java")
        return dict(namen=namen, recepten=recepten, plekken={k: dict(v) for k, v in verzamel.items()})

    def _puzzels(self):
        """data/guhs/vadsrijk_puzzels/*.json: name, kind, zone, reward. Never the solution or the exact spot; geheimpjes only counted."""
        out = []
        for f in sorted(glob.glob(os.path.join(self.root, "src", "main", "resources", "data", "guhs", "vadsrijk_puzzels", "*.json"))):
            d = _lees_json(f, {})
            pid = d.get("id")
            if not pid:
                continue
            bel = d.get("beloning") or {}
            ontgr = bel.get("ontgrendel")
            out.append(dict(id=pid, zone=f"zone{d.get('zone', 1)}", soort=d.get("soort"), geheim=d.get("soort") == "geheim",
                            naam=self.t(f"vadsrijk.puzzels.p.{pid}.naam", pid), uitleg=self.t(f"vadsrijk.puzzels.p.{pid}.uitleg", ""),
                            nacht=bool(d.get("nacht")), tijd=d.get("tijd"),
                            munten=bel.get("munten", 0), ontgrendel=self.t("vadsrijk.puzzels.ontgrendel." + ontgr, ontgr) if ontgr else None,
                            bladzijde=bel.get("bladzijde"), _anker=d.get("anker")))
        soorten = {k[len("vadsrijk.puzzels.soort."):]: v for k, v in self.nl.items() if k.startswith("vadsrijk.puzzels.soort.")}
        return dict(lijst=out, soorten=soorten)

    def _kleding(self):
        """The Kledingkast (t-kleding): every cosmetic by kind with its name and how you get it."""
        soorten = {k[len("vadsrijk.kleding.soort."):]: v for k, v in self.nl.items() if k.startswith("vadsrijk.kleding.soort.")}
        krachten = {k[len("vadsrijk.kleding.kracht."):]: v for k, v in self.nl.items() if k.startswith("vadsrijk.kleding.kracht.")}
        dingen = []
        for k, v in self.nl.items():
            m = re.match(r"vadsrijk\.kleding\.ding\.([a-z]+)\.([a-z_]+)$", k)
            if m:
                dingen.append(dict(soort=m[1], id=m[2], naam=v, bron=self.t(k + ".bron", "")))
        return dict(soorten=soorten, krachten=krachten, dingen=dingen)

    def _rij_uiterlijk(self):
        """Mount looks (t-rijdier): coats per mount, patterns, tack; names only (where they come from = the game's own N screen)."""
        vacht = collections.defaultdict(list)
        patroon, tuig = [], []
        for k, v in self.nl.items():
            m = re.match(r"vadsrijk\.rijdier\.vacht\.([a-z_]+)\.([a-z_]+)$", k)
            if m:
                vacht[m[1]].append(v)
            elif re.match(r"vadsrijk\.rijdier\.patroon\.[a-z_]+$", k):
                patroon.append(v)
            elif re.match(r"vadsrijk\.rijdier\.tuig\.[a-z_]+$", k):
                tuig.append(v)
        bronnen = [v for k, v in self.nl.items() if k.startswith("vadsrijk.rijdier.bron.prestatie.")]
        return dict(vacht=dict(vacht), patroon=patroon, tuig=tuig, prestaties=bronnen)

    def _krachten(self):
        """Pet powers (t-rijdier's Maatjes): name, role (Steuner/Snuffelaar), what it does."""
        tekst = self._java(os.path.join("rijdier", "Maatjes.java"))
        out = []
        for m in re.finditer(r'new Kracht\("([a-z_]+)", Rol\.([A-Z]+)', tekst):
            out.append(dict(id=m[1], rol=m[2].lower(), naam=self.t(f"vadsrijk.huisdier.kracht.{m[1]}.naam", m[1]),
                            tekst=self.t(f"vadsrijk.huisdier.kracht.{m[1]}.tekst", "")))
        return out

    def _vertaal(self, data):
        """The lang keys the live JSON uses (boss titles, titles, week seals, contracts): the browser translates them."""
        keys = {}
        for k, v in self.nl.items():
            if k.startswith(("vadsrijk.z3bazen.zegel.", "vadsrijk.z3wezens.contract.", "vadsrijk.z3.systemen.contract.", "vadsrijk.kern.baas.", "vadsrijk.npc.titel.",
                             "vadsrijk.klassen.titel.", "vadsrijk.economie.titel", "vadsrijk.z3.systemen.titel", "vadsrijk.kern.titel")) \
                    or (k.endswith(".titel") and ".titel." not in k and k.startswith("vadsrijk.") and len(v) < 60):
                keys[k] = v
        for b in data["bazen"]:
            if b["arena"]:
                keys["arena:" + b["arena"]["id"]] = b["naam"]
        for k in data["klassen"]:
            keys["klasse:" + k["id"]] = k["naam"]
        for i in data["items"]:
            keys["loot:" + i["id"]] = i["naam"]
        return keys
