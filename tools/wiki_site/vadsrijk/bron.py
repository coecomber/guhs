"""
The data export of guhs.nl/vadsrijk: everything the pages and the map need, read from the repo files (and the mod's runtime dump
docs/wiki/vadsrijk_runtime.json when it exists: run `gradlew runGameTestServer -Pgt=VadsrijkWeb` or `/guhs vadsrijk web
wikiexport` and copy vadsrijk_runtime.json there). Re-runnable: when the world layout moves (guhs1100-avontuur/wereldindeling),
regenerate the map + POI files and the runtime dump, and the site follows.

Privacy/spoiler rules live HERE (an allowlist): secrets (knuffels, treasures, clues, reliefs, secret quest goals) never get a
position in the output; only a count per area. Only static game content; never player data.
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
]
NIET_OP_SITE = {"proeftuin"}                   # dev areas
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
]
VERHAAL_BAZEN = {"vahoegnir", "mika_spion"}     # story twists: behind the spoiler button
SOORT_BAAS = {"klodderreus": "verhaal", "koning_klodder": "wereldbaas", "mika_spion": "verhaal", "opperzwam": "kerker",
              "galmwolf": "wereldbaas", "boormeester": "arena", "spiegelzucht": "kerker", "kruinroofvogel": "wereldbaas",
              "taleggio": "kerker", "brokkel": "kerker", "roquefort": "kerker", "piepvel": "verhaal", "wortelwurger": "kerker",
              "nestwachter": "kerker", "bleu": "kerker", "vahoegnir": "verhaal"}
# airship lines (the in-game travel map): stop -> (npc id of the captain, zone)
LUCHTSCHIP = {"knuffelkamp": ("kapitein", "zone1"), "dehaven": ("luchtschipper", "zone1"), "kandijkom": ("z2_kapitein", "zone2"),
              "honingtak": ("z3_kapitein", "zone3")}
LIJNEN = [("knuffelkamp", "dehaven"), ("dehaven", "kandijkom"), ("dehaven", "honingtak"), ("kandijkom", "honingtak")]

GEHEIM = re.compile(r"^(geheim_(?!.*ingang$)|knuffel_\d|knuffel_ijs|.*aanwijzing_\d|.*_schat(_|$)|schat_\d|relief_|.*_relief_\d|imkerspoor)")
PUBLIEK = [  # (regex on the POI id, kind)
    (re.compile(r"^kampvuur_"), "kampvuur"),
    (re.compile(r"^uitkijk"), "uitkijk"),
    (re.compile(r"^(luchtschip_kapitein|luchtschip_haven_kapitein|dok_kapitein)$"), "luchtschip"),
    (re.compile(r"^terugportaal_"), "portaal"),
    (re.compile(r"^(deur_spiegelburcht_voor|werf_ingang|hart_poort)$"), "kerker"),
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
        if out:
            self.notes.append("no runtime dump: areas read from the Java/POI sources")
        return out

    def poi_bestanden(self):
        """Every `id` | x, y, z row of POI_VADSRIJK.md / POI_Z2.md / POI_Z3.md -> {id: (zone, section, (x, y, z))}."""
        out = {}
        for zone, f in (("zone1", "POI_VADSRIJK.md"), ("zone2", "POI_Z2.md"), ("zone3", "POI_Z3.md")):
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

    # ---- the pieces ------------------------------------------------------------------------------------------------------
    def zone_van_gebied(self, gid):
        if gid.startswith("z3_"):
            return "zone3"
        if gid.startswith("z2_"):
            return "zone2"
        return "zone1"

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
        for g in gebieden:
            if self.in_gebied(g, x, y, z):
                return g
        return None

    def build(self):
        gebieden = self.gebieden()
        pois = self.poi_bestanden()
        rt = self.rt or {}
        npc_plek = {k: v for k, v in (rt.get("npcs", {}).get("plek", {}) or {}).items()}
        poi_plek = {k: v for k, v in (rt.get("npcs", {}).get("poi", {}) or {}).items()}
        for (zone, pid), (_, _, pos) in pois.items():
            poi_plek.setdefault(pid, list(pos))
        data = dict(meta=dict(runtime=bool(self.rt), bron="tools/wiki_site/vadsrijk/bron.py"))

        # areas -> zones
        gb_out = []
        for g in gebieden:
            nl, en = self.tx("vadsrijk.kern.gebied." + g["id"], g["id"])
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
        data["lore"] = self._lore()
        data["vertaal"] = self._vertaal(data)
        if not self.rt:
            self.notes.append("docs/wiki/vadsrijk_runtime.json missing: no levels/stats/drops/positions from the game "
                              "(run `gradlew runGameTestServer -Pgt=VadsrijkWeb` and copy run/vadsrijk_runtime.json)")
        return data

    def _poi_naam(self, pid, soort, g):
        gnaam = self.t("vadsrijk.kern.gebied." + g["id"], g["id"]) if g else ""
        if soort == "kampvuur":
            return "Kampvuur" + (f" ({gnaam})" if gnaam else "")
        if soort == "uitkijk":
            kaal = pid.replace("uitkijk_", "").replace("uitkijkpunt_", "")
            nl = self.t(f"vadsrijk.wereld.uitkijk.{kaal}") or self.t(f"vadsrijk.wereld.uitkijk.{kaal}.titel")
            return "Uitkijkpunt " + (nl or kaal.replace("_", " ").title())
        if soort == "luchtschip":
            return "Luchtschip" + (f" ({gnaam})" if gnaam else "")
        if soort == "portaal":
            return "Terugportaal" + (f" ({gnaam})" if gnaam else "")
        if soort == "kerker":
            return {"deur_spiegelburcht_voor": "De Spiegelburcht", "werf_ingang": "De Kaaszeppelinwerf", "hart_poort": "Het Hart van de Boom"}.get(pid, pid)
        if soort == "gondel":
            return "Gondel (" + ("berg" if "berg" in pid else "dal") + ")"
        if soort == "lift":
            return "Honinglift (" + ("boven" if "boven" in pid else "beneden") + ")"
        return pid

    def _npcs(self, plek, rt):
        out, zone_van = [], {}
        rol = (rt.get("npcs", {}) or {}).get("rol", {})
        for k, v in self.nl.items():
            if not k.startswith("vadsrijk.npc.naam."):
                continue
            nid = k[len("vadsrijk.npc.naam."):]
            zone = "zone3" if nid.startswith("z3_") else "zone2" if nid.startswith("z2_") else "zone1"
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
            m = re.match(r"vr_(?:(z2|z3)_)?mob_(.+)$", eid)
            if not m:
                continue
            zone = {"z2": "zone2", "z3": "zone3"}.get(m[1], "zone1")
            kort = m[2]
            tip_pre = {"zone1": "vadsrijk.wezens.tip.", "zone2": "vadsrijk.z2wezens.tip.", "zone3": "vadsrijk.z3wezens.tip."}[zone]
            sps = sp_per.get(eid, [])
            lvs = [s["min_lv"] for s in sps] + [s["max_lv"] for s in sps]
            gebied_ids = []
            for s in sps:
                x, y, z = s["centrum"]
                g = self.gebied_op(gebieden, x, y if s.get("grot") else None, z)
                if g and g["id"] not in gebied_ids:
                    gebied_ids.append(g["id"])
            tabel = None
            for key in (f"wezens:{kort}", f"z2wezens:{kort}", f"z3wezens:{kort}"):
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
                            huisdier=next((h for h in ("klodderreus", "koning_klodder", "galmwolf", "kruinroofvogel", "piepje", "vahoegje")
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
            zone = "zone3" if iid.startswith("z3baas_") else "zone2" if iid.startswith("z2baas_") else None
            tabellen = bronnen.get(iid, [])
            if zone is None:
                zs = {("zone3" if t.startswith("z3") else "zone2" if t.startswith("z2") else "zone1") for t in tabellen}
                zone = sorted(zs)[0] if zs else ("zone1" if iid.startswith("baas_") else "zone1")
            baas = None
            m = re.match(r"(?:z[23])?baas_(.+)$", iid)
            bron_baas = []
            for tid in tabellen:
                kort = tid.split(":")[-1]
                for b in BAZEN:
                    if b[2] in kort:
                        bron_baas.append(b[2])
            bn = benoemd.get(iid)
            if bn:
                bron_baas = [bn["baas"]]
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
        for sid, (npc, zone) in LUCHTSCHIP.items():
            if npc in npc_plek:
                stops[sid] = dict(id=sid, zone=zone, pos=npc_plek[npc], naam=self.t("vadsrijk.npc.naam." + npc, npc))
        return [dict(van=a, naar=b, a=stops[a]["pos"], b=stops[b]["pos"]) for a, b in LIJNEN if a in stops and b in stops]

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
            zone = "zone3" if "z3" in f else "zone2" if "z2" in f else "zone1"
            for sid, v in (w.get("systemen") or {}).items():
                out.append(dict(id=sid, zone=zone, titel=v[0], tekst=v[2], stukken=[]))
            per = {o["id"]: o for o in out}
            for pid, kop, tekst in w.get("tekst", []):
                sid = pid.split("/", 1)[-1]
                if sid in per:
                    per[sid]["stukken"].append([kop, tekst])
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
