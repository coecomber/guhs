"""
1.10.20 (wiki-gids): every quest step WITH its place, and the findables page (vindplekken/index.html), spoilers allowed (user decision
2026-11-25).

  data side:  Gids (used by Bron._quests) gives each quest goal its points: the same rules as the in-game tracker (npc/QuestStap.doel +
              QuestPlek: an explicit "npc:/poi:/gebied:/baas:" target wins, else per goal kind), plus what the tracker cannot show:
              ALL spots of a counted find ("teller:z4_parels" -> the four pearls), both ends of an escort, the series behind a counted
              event (the 7 lantern rings, the refugees of the soup), and for kill/cheer-up goals the middle of the creatures' spawners
              in that area. data() lists every findable per zone and kind (data["vindplekken"]).
  page side:  pagina(b) writes vindplekken/index.html (per zone a closed .zonevak with data-zone, so the zone gate strips later
              zones; per kind a table with name, area, coordinates + map link, the game's own hint text and the quest).

Truth = the Java of this branch, evaluated by javaplek.py (NpcPlekken, Z2NpcPlekken, Z3NpcPlekken, Z4/Z5/Z6VerhaalPlekken + the
generated Z?WereldPlekken maps): that gives exactly what the game uses (checked against the runtime dump: all NPCs/POIs equal).
The runtime dump's NPC spots fill in NPCs these classes do not own (teachers, recruit boards, ...).
"""
import collections
import re

from . import javaplek
from .pages import ESC, t, p, slug, zl

KLASSE = {"zone1": "NpcPlekken", "zone2": "Z2NpcPlekken", "zone3": "Z3NpcPlekken", "zone4": "Z4VerhaalPlekken", "zone5": "Z5VerhaalPlekken",
          "zone6": "Z6VerhaalPlekken"}
NS = {"zone1": "vadsrijk.npc", "zone2": "vadsrijk.z2npc", "zone3": "vadsrijk.z3npc", "zone4": "vadsrijk.z4verhaal", "zone5": "vadsrijk.z5verhaal",
      "zone6": "vadsrijk.z6verhaal"}

# (zone, kind id, Java map ("VONDST:<soort>" = Z6VerhaalPlekken.VONDST[soort]; "POI:<prefix>" = a numbered POI series), the quest counter
#  ("teller:<x>" / zone 1's bare "knuffel"/"aanwijzing"), lang sub-namespace for names/texts, also on the Geheimen page?, en, nl)
SOORTEN = [
    ("zone1", "knuffels", "KNUFFEL", "knuffel", "knuffel", True, "Hidden knuffels", "Verborgen knuffels"),
    ("zone1", "aanwijzingen", "AANWIJZING", "aanwijzing", "aanwijzing", True, "Clues to the Mika spy", "Aanwijzingen naar de Mika-spion"),
    ("zone1", "schatten", "SCHAT", None, "schat", True, "Treasure chests", "Schatkisten"),
    ("zone2", "knuffels", "KNUFFEL", "teller:z2_knuffels", "knuffel", True, "Knuffels under the ice", "Knuffels onder het ijs"),
    ("zone2", "aanwijzingen", "AANWIJZING", "teller:z2_aanwijzingen", "aanwijzing", True, "Clues to the Mika driller", "Aanwijzingen naar de Mika-boormeester"),
    ("zone2", "schatten", "SCHAT", "teller:z2_schatten", "schat", True, "Treasure chests", "Schatkisten"),
    ("zone3", "knuffels", "KNUFFEL", "teller:z3_knuffels", "knuffel", True, "Hidden knuffels", "Verborgen knuffels"),
    ("zone3", "aanwijzingen", "AANWIJZING", "teller:z3_aanwijzingen", "aanwijzing", True, "Clues to the Mika beekeeper", "Aanwijzingen naar de Mika-imker"),
    ("zone3", "schatten", "SCHAT", "teller:z3_schatten", "schat", True, "Treasure chests", "Schatkisten"),
    ("zone3", "reliefs", "RELIEF", "teller:z3_reliefs", "relief", True, "Reliefs (Het Muurverhaal)", "Reliëfs (Het Muurverhaal)"),
    ("zone3", "kuikens", "KUIKEN", "teller:z3_kuikens", "kuiken", False, "Lost plume chicks", "Verdwaalde Pluimkuikens"),
    ("zone3", "lichtjes", "LICHTJE", "teller:z3_lichtjes", "lichtje", False, "Sleeping tree-folk lights", "Slapende boomvolk-lichtjes"),
    ("zone3", "lantaarnringen", "POI:z3_ring_", "z3_lantaarn:ring", None, False, "Lantern rings in the trunk", "Lantaarnringen in de stam"),
    ("zone3", "grotlantaarns", "POI:z3_grotlantaarn_", "z3_grotlantaarn:aan", None, False, "Cave lanterns of the Hart", "Grotlantaarns van het Hart"),
    ("zone4", "tijknuffels", "TIJKNUFFEL", "teller:z4_tijknuffels", "tijknuffel", False, "Tide knuffels (Knuffeltij beach)", "Tijknuffels (Knuffeltij-strand)"),
    ("zone4", "grote_knuffels", "GROTE_KNUFFEL", "poi:z4_grote_knuffel_", None, False, "Big knuffels on the islands", "Grote knuffels op de eilanden"),
    ("zone4", "wrakknuffels", "WRAKKNUFFEL", "teller:z4_wrakknuffels", None, False, "Knuffels in the wreck", "Knuffels in het wrak"),
    ("zone4", "knuffels", "KNUFFEL", "teller:z4_knuffels", "knuffel", True, "Hidden knuffels", "Verborgen knuffels"),
    ("zone4", "schatten", "SCHAT", "teller:z4_schatten", "schat", True, "Treasure chests", "Schatkisten"),
    ("zone4", "flessenpost", "FLESSENPOST", "teller:z4_flessenpost", "fles", True, "Messages in a bottle", "Flessenpost"),
    ("zone4", "mistbrieven", "MISTBRIEVEN", "teller:z4_mistbrieven", "mistbrief", True, "Letters in the mist", "Mistbrieven"),
    ("zone4", "uitzichten", "UITZICHT", "teller:z4_uitzichten", "uitzicht", True, "Views", "Uitzichten"),
    ("zone4", "lampionhaken", "LAMPIONHAKEN", "teller:z4_lampionhaken", None, False, "Lantern hooks", "Lampionhaken"),
    ("zone4", "vliegerspul", "VLIEGERSPUL", "teller:z4_vliegerspul", None, False, "Kite parts", "Vliegerspul"),
    ("zone4", "parels", "PARELS", "teller:z4_parels", None, False, "Pearls", "Parels"),
    ("zone4", "schelpen", "SCHELPEN", "teller:z4_schelpen", None, False, "Shells", "Schelpjes"),
    ("zone4", "bijtjes", "BIJTJES", "teller:z4_bijtjes", None, False, "Bees", "Bijtjes"),
    ("zone4", "bloemen", "BLOEMEN", "teller:z4_bloemen", None, False, "Flowers for the wreaths", "Bloemen voor de kransen"),
    ("zone5", "knuffels", "KNUFFEL", "teller:z5_knuffels", "knuffel", True, "Hidden knuffels", "Verborgen knuffels"),
    ("zone5", "schatten", "SCHAT", "teller:z5_schatten", "schat", True, "Treasure chests", "Schatkisten"),
    ("zone5", "bessen", "BES", "teller:z5_bessen", None, False, "Cloud berries", "Wolkbessen"),
    ("zone5", "bollen", "BOL", "teller:z5_bollen", None, False, "Balls of pink wool", "Bolletjes roze wol"),
    ("zone6", "knuffels", "KNUFFEL", "teller:z6_knuffels", "knuffel", True, "Hidden knuffels", "Verborgen knuffels"),
    ("zone6", "schatten", "SCHAT", "teller:z6_schatten", "schat", True, "Treasure chests", "Schatkisten"),
    ("zone6", "taart", "VONDST:z6_taart", "teller:z6_taart", None, False, "Cake things", "Taartspulletjes"),
    ("zone6", "post", "VONDST:z6_post", "teller:z6_post", None, False, "Letters to deliver", "Brieven bezorgen"),
    ("zone6", "kwastjes", "VONDST:z6_kwastjes", "teller:z6_kwastjes", None, False, "Carpet tassels", "Tapijtkwastjes"),
    ("zone6", "eendjes", "VONDST:z6_eendjes", "teller:z6_eendjes", None, False, "Rubber ducks", "Badeendjes"),
    ("zone6", "dadels", "VONDST:z6_dadels", "teller:z6_dadels", None, False, "Date baskets", "Dadelmandjes"),
    ("zone6", "zoutkreeft", "VONDST:z6_zoutkreeft", "teller:z6_zoutkreeft", None, False, "Salt crayfish", "Zoutkreeftjes"),
    ("zone6", "tabletten", "VONDST:z6_tabletten", "teller:z6_tabletten", "tablet", True, "Stone tablets", "Stenen tabletten"),
    ("zone6", "cocons", "VONDST:z6_cocons", "teller:z6_cocons", None, False, "Silk cocoons", "Zijdecocons"),
]
# a counted event that happens at a series of places: event -> ("poi"|"npc", id prefix)
MELD_REEKS = {"z3_lantaarn:ring": ("poi", "z3_ring_"), "z3_grotlantaarn:aan": ("poi", "z3_grotlantaarn_"),
              "z5_soep:kom": ("npc", "z5_vluchteling_"), "z6_soep:kom": ("npc", "z6_vluchteling_")}
GEEN_EN = {"level": "Just level up: anywhere.", "overal": "Anywhere in the zone.", "onbekend": "No fixed spot."}
GEEN_NL = {"level": "Gewoon levelen: dat kan overal.", "overal": "Overal in de zone.", "onbekend": "Geen vaste plek."}


def _lijst(p):
    return [int(p[0]), int(p[1]), int(p[2])] if p else None


class Gids:
    """The places of the game (evaluated Java) + the goal -> points rules. One per Bron (bron._gids())."""

    def __init__(self, bron):
        self.bron = bron
        self.j = javaplek.Wereld(bron.root)
        self.k = {}
        for z, naam in KLASSE.items():
            kl = self.j.klasse(naam)
            if kl:
                kl.init()
                self.k[z] = kl.statics
                if len(kl.fouten) > 12:
                    bron.notes.append(f"vindplekken: {naam}: {len(kl.fouten)} static members not evaluated (first: {kl.fouten[0]})")
            else:
                self.k[z] = {}
                bron.notes.append(f"vindplekken: {naam}.java missing")
        rt = bron.rt or {}
        self.npc, self.poi, self.gebied, self.vuur, self.baas = {}, {}, {}, {}, {}
        for z in KLASSE:
            st = self.k[z]
            for naam, doel in (("NPC", self.npc), ("POI", self.poi), ("GEBIED_DOEL", self.gebied), ("KAMPVUUR", self.vuur), ("BAAS", self.baas)):
                for a, b in (st.get(naam) or {}).items():
                    if isinstance(b, tuple):
                        doel.setdefault(a, _lijst(b))
        verschil = []
        for a, b in ((rt.get("npcs") or {}).get("plek") or {}).items():   # NPCs these classes do not own (teachers, boards, ...)
            if a not in self.npc:
                self.npc[a] = list(b)
            elif self.npc[a] != list(b):
                verschil.append(a)
        for a, b in ((rt.get("npcs") or {}).get("poi") or {}).items():
            if a not in self.poi:
                self.poi[a] = list(b)
            elif self.poi[a] != list(b):
                verschil.append(a)
        if verschil:
            bron.notes.append(f"vindplekken: {len(verschil)} NPC/POI spots differ between the Java and the runtime dump (Java wins): {verschil[:8]}")
        st = self.k["zone1"]
        self.training = _lijst(st.get("TRAINING")) if isinstance(st.get("TRAINING"), tuple) else None
        self.mika_kraam = _lijst(st.get("MIKA_KRAAM")) if isinstance(st.get("MIKA_KRAAM"), tuple) else None
        self._spawners = rt.get("spawners", [])
        self._gebieden = None

    # ---- the finds -------------------------------------------------------------------------------------------------------------
    def vind_map(self, zone, mapnaam):
        st = self.k.get(zone, {})
        if mapnaam.startswith("VONDST:"):
            m = (st.get("VONDST") or {}).get(mapnaam[7:]) or {}
        elif mapnaam.startswith("POI:"):
            pre = mapnaam[4:]
            m = {k: v for k, v in (st.get("POI") or {}).items() if k.startswith(pre) and k[len(pre):].isdigit()}
        else:
            m = st.get(mapnaam) or {}
        return [(k, _lijst(v)) for k, v in m.items() if isinstance(v, tuple)]

    def soort_van_teller(self, doel):
        for s in SOORTEN:
            if s[3] and (doel == s[3] or (s[3].endswith("_") and doel.startswith(s[3]))):
                return s
        return None

    # ---- areas: the middle of the creatures that live there ---------------------------------------------------------------------
    def wezens_midden(self, gid):
        if self._gebieden is None:
            self._gebieden = {g["id"]: g for g in self.bron.gebieden()}
        g = self._gebieden.get(gid)
        if not g:
            return None
        pts = [s["centrum"] for s in self._spawners if self.bron.in_gebied(g, s["centrum"][0], s["centrum"][1] if s.get("grot") else None, s["centrum"][2])
               and not s["type"].endswith(("proefklodder", "proefkloddertje"))]
        if not pts:
            return None
        x, z = sum(q[0] for q in pts) / len(pts), sum(q[2] for q in pts) / len(pts)
        best = min(pts, key=lambda q: (q[0] - x) ** 2 + (q[2] - z) ** 2)   # a real spawner spot (with a real y), the one nearest the middle
        return [int(best[0]), int(best[1]), int(best[2])], len(pts)

    # ---- one goal ------------------------------------------------------------------------------------------------------------------
    def tekst(self, *keys):
        for k in keys:
            if k and k in self.bron.nl:
                return self.bron.schoon(self.bron.nl[k]), self.bron.schoon(self.bron.en.get(k, self.bron.nl[k]))
        return "", ""

    def doel(self, qd, d):
        """{plekken: [{pos, label, label_en}], waar, waar_en, gebieden, vind, geen} for one goal (the tracker's rules + all spots)."""
        P = "vadsrijk.npc.plek."
        punten, gebieden, keys, vind, geen = [], [], [], None, None

        def zet(pos, nl=None, en=None):
            if not pos:
                return
            for x in punten:
                if x["pos"] == pos:
                    if x["label"] is None and nl:
                        x["label"], x["label_en"] = nl, en or nl
                    return
            punten.append(dict(pos=pos, label=nl, label_en=en or nl))

        soort, doel, bron_ = d["soort"], str(d["doel"]), str(d.get("bron") or "")
        plek = d.get("plek") or ""
        if plek and ":" in plek:
            k, _, v = plek.partition(":")
            pos = {"npc": self.npc, "poi": self.poi, "gebied": self.gebied, "baas": self.baas}.get(k, {}).get(v)
            if pos:
                keys.append(P + k + "." + v)
                zet(pos)
            if k == "gebied":
                gebieden.append(v)
        # per goal kind (QuestStap.doel), always: it adds what an explicit target leaves out
        if soort == "praat":
            keys.append(P + "npc." + doel)
            zet(self.npc.get(doel))
        elif soort == "escorte":
            dest = bron_[4:] if bron_.startswith("poi:") else bron_
            keys += [P + "npc." + doel, P + "poi." + dest]
            zet(self.npc.get(doel), "ophalen", "pick up")
            zet(self.poi.get(dest), "brengen naar", "bring to")
        elif soort in ("gebied", "geheim"):
            if doel.startswith("poi:"):
                keys.append(P + "poi." + doel[4:])
                zet(self.poi.get(doel[4:]))
            elif soort == "gebied":
                for g in doel.split("|"):
                    gebieden.append(g)
                keys.append(P + "gebied." + doel.split("|")[0])
                zet(self.gebied.get(doel.split("|")[0]))
            if soort == "geheim" and not doel.startswith("poi:"):
                s = self.soort_van_teller(doel)
                keys.append(P + "overal." + doel)
                if s:
                    vind = f"{s[0]}-{s[1]}"
                    for i, (fid, pos) in enumerate(self.vind_map(s[0], s[2]), 1):
                        zet(pos, self.vind_naam(s, fid, i)[0], self.vind_naam(s, fid, i)[1])
        elif soort == "kampvuur":
            keys.append(P + "kampvuur." + doel)
            zet(self.vuur.get(doel))
        elif soort in ("opvrolijk", "elite", "verzamel"):
            bron_g = bron_ if soort == "verzamel" else doel
            gs = [g for g in bron_g.replace("gebied:", "").split("|") if g and g != "*"] if bron_g.startswith("gebied:") else []
            for g in gs:
                if g not in gebieden:
                    gebieden.append(g)
            if gs:
                keys.append(P + "gebied." + gs[0])
                zet(self.gebied.get(gs[0]), "hier wijst de questpijl", "the quest arrow points here")
            if len(gs) == 1:
                m = self.wezens_midden(gs[0])
                if m:
                    zet(m[0], "midden van de wezens", "middle of the creatures")
            if not gs and not punten:
                geen = "overal"
        elif soort == "baas":
            b = doel[7:] if doel.startswith("gebied:") else doel[doel.find(":") + 1:]
            keys.append(P + "baas." + b)
            zet(self.baas.get(b))
            if doel.startswith("gebied:"):
                gebieden.append(b)
        elif soort == "klasse":
            keys.append(P + "npc.priesteres")
            zet(self.npc.get("priesteres"))
        elif soort == "pop":
            keys.append(P + "training")
            zet(self.training)
        elif soort == "level":
            geen = "level"
        elif soort == "meld":
            if doel == "spion:ontmaskerd":
                keys.append(P + "npc.akim_kaasguh")
                zet(self.npc.get("akim_kaasguh"))
            elif doel.startswith("opruimen:"):
                wie = "kok" if doel == "opruimen:verkocht" else "rekruteerder"
                keys.append(P + "npc." + wie)
                zet(self.npc.get(wie))
            elif doel.startswith("quiz:"):
                keys.append(P + "npc." + qd["gever"])
                zet(self.npc.get(qd["gever"]))
            elif doel == "zwamtempel:deur":
                keys.append(P + "poi.tempelpoort")
                zet(self.poi.get("tempelpoort"))
            elif doel == "spion:gevangen":
                keys.append(P + "daken")
                zet(self.mika_kraam)
            if doel in MELD_REEKS:
                bak, pre = MELD_REEKS[doel]
                m = self.poi if bak == "poi" else self.npc
                ids = sorted((k for k in m if k.startswith(pre) and k[len(pre):].isdigit()), key=lambda k: int(k[len(pre):]))
                for i, k in enumerate(ids, 1):
                    zet(m[k], f"#{i}")
                s = self.soort_van_teller(doel)
                if s:
                    vind = f"{s[0]}-{s[1]}"
        if soort == "gebied" and doel.startswith("poi:"):
            s = self.soort_van_teller(doel)
            if s:
                vind = f"{s[0]}-{s[1]}"
        if len(punten) > 1 and punten[0]["label"] is None:   # the tracker's own target among several spots
            punten[0]["label"], punten[0]["label_en"] = "hier wijst de questpijl", "the quest arrow points here"
        if not punten and not geen:
            geen = "onbekend"
        waar, waar_en = self.tekst(*keys)
        return dict(plekken=punten, waar=waar, waar_en=waar_en, gebieden=gebieden, vind=vind, geen=geen if not punten else None)

    def vind_naam(self, s, fid, i):
        """(nl, en) name of one find."""
        zone, _, mapnaam, _, sub, *_ = s
        ns = NS[zone]
        if sub == "tijknuffel":
            return self.tekst(f"{ns}.tijknuffel.{fid}.naam") if f"{ns}.tijknuffel.{fid}.naam" in self.bron.nl else (f"#{i}", f"#{i}")
        if sub:
            nl, en = self.tekst(f"{ns}.{sub}.naam.{fid}", f"{ns}.{sub}.titel.{fid}", f"{ns}.{sub}.{fid}" if sub == "knuffel" else None)
            if nl:
                return nl, en
        return f"#{i}", f"#{i}"

    def vind_tekst(self, s, fid):
        zone, _, mapnaam, _, sub, *_ = s
        ns = NS[zone]
        if sub == "tijknuffel":
            return self.tekst(f"{ns}.tijknuffel.{fid}.verhaal")
        if sub == "mistbrief":
            return self.tekst(f"{ns}.mistbrief.mistbrief_{fid.rsplit('_', 1)[-1]}")
        if sub and sub != "knuffel":
            return self.tekst(f"{ns}.{sub}.{fid}")
        return "", ""


def gids(bron):
    if not hasattr(bron, "_vind_gids"):
        bron._vind_gids = Gids(bron)
    return bron._vind_gids


# ---- the findables list ---------------------------------------------------------------------------------------------------------
def data(bron, quests, gebieden):
    """[{zone, soort, id, naam, naam_en, en, nl, geheimen, hint, hint_en, quests, dingen: [{nr, id, naam, naam_en, tekst, tekst_en, pos, gebied, gebied_naam}]}]"""
    G = gids(bron)
    per_teller = collections.defaultdict(list)
    for q in quests:
        for d in q["doelen"]:
            s = G.soort_van_teller(d.get("ref", ""))
            if s and q["id"] not in per_teller[(s[0], s[1])]:
                per_teller[(s[0], s[1])].append(q["id"])

    def gebied(pos):
        g = bron.gebied_op(gebieden, pos[0], pos[1], pos[2])
        return (g["id"], g["naam"]) if g else (None, "")

    out = []
    for s in SOORTEN:
        zone, sid, mapnaam, teller, sub, geh, en, nl = s
        lijst = G.vind_map(zone, mapnaam)
        if not lijst:
            bron.notes.append(f"vindplekken: {zone} {sid}: no spots found ({mapnaam})")
            continue
        qs = list(per_teller.get((zone, sid), []))
        if mapnaam.startswith("VONDST:"):
            vq = (G.k[zone].get("VONDST_QUEST") or {}).get(mapnaam[7:])
            if vq and vq not in qs:
                qs.append(vq)
        hint, hint_en = G.tekst(f"vadsrijk.npc.plek.overal.{teller}" if teller and teller.startswith("teller:") else None,
                                f"vadsrijk.npc.plek.overal.{teller}" if zone == "zone1" and teller else None,
                                "vadsrijk.npc.plek.overal.schat" if zone == "zone1" and sid == "schatten" else None)
        dingen = []
        for i, (fid, pos) in enumerate(lijst, 1):
            naam, naam_en = G.vind_naam(s, fid, i)
            if naam.startswith("#"):   # no own name: the table's number column says it already
                naam = naam_en = ""
            tekst, tekst_en = G.vind_tekst(s, fid)
            gid, gnaam = gebied(pos)
            dingen.append(dict(nr=i, id=fid, naam=naam, naam_en=naam_en, tekst=tekst, tekst_en=tekst_en, pos=pos, gebied=gid, gebied_naam=gnaam))
        out.append(dict(zone=zone, soort=sid, id=f"{zone}-{sid}", en=en, nl=nl, geheimen=geh, hint=hint, hint_en=hint_en, quests=qs, dingen=dingen))
    # the secret quest steps that are one spot each (the scars, Dobber's hat, the tuba, ...): per zone one list
    for z in KLASSE:
        dingen = []
        for q in quests:
            if q["zone"] != z:
                continue
            for d in q["doelen"]:
                if d["geheim"] and str(d.get("ref", "")).startswith("poi:") and d.get("plekken"):
                    pos = d["plekken"][0]["pos"]
                    gid, gnaam = gebied(pos)
                    dingen.append(dict(nr=len(dingen) + 1, id=d["ref"][4:], naam=d["tekst"], naam_en=d["tekst"], tekst=d.get("waar") or "",
                                       tekst_en=d.get("waar_en") or "", pos=pos, gebied=gid, gebied_naam=gnaam, quest=q["id"]))
        if dingen:
            out.append(dict(zone=z, soort="questgeheim", id=f"{z}-questgeheim", en="Secret quest spots", nl="Geheime questplekjes", geheimen=False,
                            hint="", hint_en="", quests=sorted({x["quest"] for x in dingen}, key=[q["id"] for q in quests].index), dingen=dingen))
    volg = {z: i for i, z in enumerate(KLASSE)}
    out.sort(key=lambda s: volg[s["zone"]])
    return out


# ---- the page ---------------------------------------------------------------------------------------------------------------------
def _kaart(b, pos, van, label=""):
    x, y, z = pos
    return f'<a href="{b.r(van)}kaart.html?x={x}&amp;z={z}&amp;geheim=1">{label}<span class="tagref">{x}, {y}, {z}</span></a>'


def _quest_link(b, qid, van):
    q = b.q.get(qid)
    if not q:
        return ESC(qid)
    if q["soort"] == "geheim":
        return zl(f'<a href="{b.r(van)}quests/{slug(qid)}.html">{t("a secret quest", "een geheime quest")}</a>', q.get("zone"))
    return b.l_quest(qid, van)


def pagina(b):
    P = "vindplekken/index.html"
    V = b.d.get("vindplekken") or []
    delen = []
    for z in b.d["zones"]:
        mine = [s for s in V if s["zone"] == z["id"]]
        if not mine:
            continue
        inner = '<p class="muted">' + " &middot; ".join(f'<a href="#{s["id"]}">{t(s["en"], s["nl"])} ({len(s["dingen"])})</a>' for s in mine) + "</p>"
        for s in mine:
            kop = f'<h3 id="{s["id"]}">{t(s["en"], s["nl"])} <small class="muted">({len(s["dingen"])})</small></h3>'
            info = ""
            if s["quests"]:
                info += f'<p><b>{t("Quest", "Quest")}:</b> ' + ", ".join(_quest_link(b, q, P) for q in s["quests"]) + "</p>"
            if s["hint"]:
                info += f'<p class="game-text">{t(s["hint_en"], s["hint"])}</p>'
            if s["geheimen"]:
                info += (f'<p class="muted">{t("These are in the Verkenningsboek too: the story text and the reward are on the", "Deze staan ook in het Verkenningsboek: het verhaaltje en de beloning staan op de")} '
                         f'<a href="../geheimen/index.html#{z["id"]}">{t("Secrets page", "Geheimen-pagina")}</a>.</p>')
            rijen = ""
            for d in s["dingen"]:
                naam = t(d["naam_en"], d["naam"]) if d["naam"] else ""
                if d.get("quest"):
                    naam += f' <small class="muted">({_quest_link(b, d["quest"], P)})</small>'
                tekst = "" if s["geheimen"] else (f'<div class="game-text vp-tekst">{t(d["tekst_en"], d["tekst"])}</div>' if d["tekst"] else "")
                gebied = b.l_gebied(d["gebied"], P) if d.get("gebied") else ESC(d.get("gebied_naam") or "")
                rijen += (f'<tr><td class="num">{d["nr"]}</td><td>{naam}{tekst}</td><td>{gebied}</td>'
                          f'<td class="vp-xyz">{_kaart(b, d["pos"], P)}</td></tr>')
            tab = ('<div class="tscroll"><table class="data"><thead><tr><th class="num">#</th><th>' + t("What", "Wat") + "</th><th>" + t("Area", "Gebied")
                   + "</th><th>x, y, z</th></tr></thead><tbody>" + rijen + "</tbody></table></div>")
            inner += kop + info + tab
        n = sum(len(s["dingen"]) for s in mine)
        delen.append(b.zonevak(z, inner, f'{n} {t("spots", "plekken")}'))
    uitleg_en = ("Everything the quests send you looking for, with coordinates: knuffels, chests, clues, chicks, lights, pearls, shells, "
                 "letters and all the other little things. Per zone and per kind; click the coordinates to see the spot on the map. "
                 "Watch out: this page spoils everything.")
    uitleg_nl = ("Alles waar de quests je naar laten zoeken, met coördinaten: knuffels, kisten, aanwijzingen, kuikens, lichtjes, parels, "
                 "schelpjes, briefjes en al die andere kleine dingetjes. Per zone en per soort; klik op de coördinaten en je ziet de plek op de kaart. "
                 "Let op: deze pagina verklapt alles.")
    sub_en = ("Click a zone to open it. Coordinates are x, y, z (y = height). The quest pages list the places of every step too "
              "(Walkthrough). Some things only show up while their quest runs.")
    sub_nl = ("Klik op een zone om hem open te klappen. Coördinaten zijn x, y, z (y = de hoogte). Op de questpagina's staan de plekken "
              "van elke stap ook (Walkthrough). Sommige dingen zie je pas als hun quest loopt.")
    main = (f'<h1>{t("Findables", "Vindplekken")}</h1>' + p(uitleg_en, uitleg_nl) + f'<p class="muted">{t(sub_en, sub_nl)}</p>' + "".join(delen))
    # a link to #zoneN-kind opens its (closed) zone first
    script = ("<script>(function(){function open_(){var h=decodeURIComponent(location.hash.slice(1));if(!h)return;var e=document.getElementById(h);"
              "if(!e)return;for(var d=e.closest('details');d;d=d.parentElement&&d.parentElement.closest('details'))d.open=true;e.scrollIntoView();}"
              "window.addEventListener('hashchange',open_);open_();})();</script>")
    head = ("<style>.vp-xyz{white-space:nowrap}.vp-tekst{font-size:13px;margin-top:3px}</style>")
    b.schrijf(P, "Vindplekken", "Findables", main, P, head=head, script=script,
              beschrijving="Alle vindplekken van Het Vadsrijk met coördinaten: knuffels, kisten, aanwijzingen en alles waar quests je naar laten zoeken.")
