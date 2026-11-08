"""U-QUESTROBOT, the static half: what the quest data promises vs. what the code, the texts and the items can deliver.

Usage (project root):  python tools/vadsrijk/questrobot/statisch.py [uit.json]

Reads every quests*.json (zone 6 is picked up automatically as soon as quests_z6.json exists) and checks:
  - texts: titel/tekst/bezig/doel.i/aanbod.i/klaar.i/praat.<npc>.i and the tracker's "where" keys, in nl_nl AND en_us, not blank;
  - links: 'na' quests exist and come earlier in the questlog; speakers are known NPCs; level gates <= the cap (60);
  - actions (actie_aanname/actie_klaar) are built in or registered in QuestMotor.ACTIES;
  - MELD keys ("soort:doel") and GEHEIM "teller:<map>" counters have something in the code that reports/fills them;
  - items (verzamel / start_items / rewards) have an item model and a name.
Writes a JSON list of findings (for questrobot_rapport.py) and prints a summary.
"""
import glob
import json
import os
import re
import sys

# (QR_ROOT=<another worktree> checks that tree read-only, e.g. a zone-6 branch that is not merged yet)
ROOT = os.environ.get("QR_ROOT") or os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
JAVA = os.path.join(ROOT, "src", "main", "java")


def lees_cap():
    """The level cap from kern/Levels.java (MAX), so a branch that raises it (zone 6: 70) is checked against its own cap."""
    try:
        with open(os.path.join(JAVA, "nl", "juiced", "guhs", "feature", "vadsrijk", "kern", "Levels.java"), encoding="utf-8") as h:
            return int(re.search(r"public static final int MAX = (\d+);", h.read()).group(1))
    except (OSError, AttributeError):
        return 60


CAP = lees_cap()
INGEBOUWD = {"klasse", "bam", "kampklodder", "luchtschip", "cliffhanger", "ontmasker", "rijdier", "advancement", "start"}


def zone_van(qid):
    if len(qid) > 3 and qid[0] == "z" and "2" <= qid[1] <= "9" and qid[2] == "_":
        return int(qid[1])
    return 1


def quest_bestanden():
    def volgorde(f):
        m = re.search(r"quests_z(\d+)\.json$", f)
        return int(m.group(1)) if m else 1
    return sorted(glob.glob(os.path.join(RES, "data", "guhs", "vadsrijk", "npc", "quests*.json")), key=volgorde)


def laad_quests():
    alle = []
    for f in quest_bestanden():
        with open(f, encoding="utf-8") as h:
            for q in json.load(h)["quests"]:
                q["_bestand"] = os.path.basename(f)
                alle.append(q)
    return alle


def java_bronnen():
    l = {}
    for f in glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True):
        if "GameTest" in f or os.sep + "questrobot" + os.sep in f:
            continue
        with open(f, encoding="utf-8", errors="replace") as h:
            l[f] = h.read()
    return l


def controleer():
    with open(os.path.join(RES, "assets", "guhs", "lang", "nl_nl.json"), encoding="utf-8") as h:
        nl = json.load(h)
    with open(os.path.join(RES, "assets", "guhs", "lang", "en_us.json"), encoding="utf-8") as h:
        en = json.load(h)
    quests = laad_quests()
    pos = {q["id"]: i for i, q in enumerate(quests)}
    bron = java_bronnen()
    alle_java = "\n".join(bron.values())
    per_map = {}
    for f, t in bron.items():
        per_map[os.path.dirname(f)] = per_map.get(os.path.dirname(f), "") + "\n" + t
    acties = set(re.findall(r'ACTIES\.put\("([^"]+)"', alle_java))
    npcs = {k[len("vadsrijk.npc.naam."):] for k in nl if k.startswith("vadsrijk.npc.naam.")}
    items_dir = os.path.join(RES, "assets", "guhs", "items")
    bev = []

    def fout(q, ernst, soort, tekst):
        bev.append({"quest": q["id"], "zone": zone_van(q["id"]), "ernst": ernst, "soort": soort, "tekst": tekst})

    def tekst(q, key, wat):
        for naam, t in (("nl_nl", nl), ("en_us", en)):
            if key not in t:
                fout(q, "fout", "tekst", f"{wat}: sleutel {key} ontbreekt in {naam}")
                return
            if not str(t[key]).strip():
                fout(q, "fout", "tekst", f"{wat}: sleutel {key} is leeg in {naam}")
                return

    def item(q, iid, wat):
        ns, _, pad = iid.partition(":") if ":" in iid else ("guhs", ":", iid)
        if ns != "guhs":
            return
        if not os.path.exists(os.path.join(items_dir, pad + ".json")):
            fout(q, "fout", "item", f"{wat}: item {iid} heeft geen item-model (bestaat het wel?)")
        if f"item.guhs.{pad}" not in nl:
            fout(q, "waarschuwing", "item", f"{wat}: item {iid} heeft geen naam (item.guhs.{pad})")

    def meld_bron(sleutel):
        if '"' + sleutel + '"' in alle_java:
            return True
        soort, _, doel = sleutel.partition(":")
        for f, t in bron.items():
            if "meld" not in t:
                continue
            soort_ok = ('"' + soort + '"' in t) or any(soort.startswith(p) for p in re.findall(r'"([a-z0-9]+_)"\s*\+', t))
            # (the doel may come from a variable: an id defined elsewhere in the same package counts too)
            if not soort_ok:
                continue
            doel_ok = doel == "*" or re.search(r'"(?:[a-z0-9]+_)?' + re.escape(doel) + '"', per_map.get(os.path.dirname(f), "")) is not None
            if soort_ok and doel_ok:
                return True
        return False

    for q in quests:
        p = f"vadsrijk.npc.q.{q['id']}"
        tekst(q, p + ".titel", "titel")
        tekst(q, p + ".tekst", "omschrijving")
        tekst(q, p + ".bezig", "'hoe gaat het'-regel")
        for i, _ in enumerate(q.get("aanbod", [])):
            tekst(q, f"{p}.aanbod.{i}", f"aanbod-dialoog pagina {i}")
        for i, _ in enumerate(q.get("klaar", [])):
            tekst(q, f"{p}.klaar.{i}", f"inlever-dialoog pagina {i}")
        if not q.get("aanbod") and not q.get("auto"):
            fout(q, "fout", "dialoog", "geen aanbod-dialoog (de gever zegt niets bij het aanbieden)")
        if not q.get("klaar"):
            fout(q, "waarschuwing", "dialoog", "geen inlever-dialoog")
        for npc, sprekers in q.get("praat", {}).items():
            for i, _ in enumerate(sprekers):
                tekst(q, f"{p}.praat.{npc}.{i}", f"praat-dialoog {npc} pagina {i}")
        for lijst in [q.get("aanbod", []), q.get("klaar", [])] + list(q.get("praat", {}).values()):
            for s in lijst:
                s = s.split(":")[0]
                if s not in ("npc", "jij", "verteller") and s not in npcs:
                    fout(q, "fout", "dialoog", f"spreker '{s}' is geen bekende NPC")
        for wie in {q.get("gever", ""), q.get("inleveren", q.get("gever", ""))}:
            if wie and wie not in npcs:
                fout(q, "fout", "npc", f"NPC '{wie}' heeft geen naam (vadsrijk.npc.naam.{wie})")
            if wie:
                tekst(q, f"vadsrijk.npc.plek.npc.{wie}", f"waar-tekst van {wie}")
        if q.get("lv", 1) > CAP:
            fout(q, "fout", "level", f"levelpoort lv {q['lv']} > cap {CAP}")
        for n in q.get("na", []):
            if n.startswith("@"):
                continue
            if n not in pos:
                fout(q, "fout", "volgorde", f"'na' {n} bestaat niet: de quest komt nooit vrij")
            elif pos[n] > pos[q["id"]]:
                fout(q, "waarschuwing", "volgorde", f"'na' {n} staat later in de questlog")
        for a in (q.get("actie_aanname", ""), q.get("actie_klaar", "")):
            if a:
                k = a.split(":")[0]
                if k not in INGEBOUWD and k not in acties:
                    fout(q, "fout", "actie", f"actie '{a}' is nergens geregistreerd (QuestMotor.ACTIES)")
                if k == "start" and a.split(":")[1] not in pos:
                    fout(q, "fout", "actie", f"actie '{a}' start een quest die niet bestaat")
        for i, d in enumerate(q.get("doelen", [])):
            tekst(q, f"{p}.doel.{i}", f"stap {i} ({d['soort']})")
            plek = d.get("plek", "")
            if plek:
                tekst(q, "vadsrijk.npc.plek." + plek.replace(":", ".", 1), f"stap {i} waar-tekst")
            soort, doel = d["soort"], d.get("doel", "*")
            if soort == "meld" and not meld_bron(doel):
                fout(q, "fout", "meld", f"stap {i}: niemand in de code meldt '{doel}' (NpcApi.meld)")
            if soort == "geheim" and doel.startswith("teller:") and '"' + doel[7:] + '"' not in alle_java:
                fout(q, "fout", "teller", f"stap {i}: teller-map '{doel[7:]}' wordt nergens gevuld")
            if soort in ("praat", "escorte") and doel not in npcs:
                fout(q, "fout", "npc", f"stap {i}: NPC '{doel}' bestaat niet")
            if soort == "praat":
                tekst(q, f"vadsrijk.npc.plek.npc.{doel}", f"stap {i} waar-tekst van {doel}")
            if soort == "level" and d.get("n", 1) > CAP:
                fout(q, "fout", "level", f"stap {i}: level {d.get('n')} > cap {CAP}")
            if soort == "verzamel":
                item(q, d["doel"], f"stap {i}")
                if not d.get("kans") and not any(x[0] == d["doel"] for x in q.get("start_items", [])):
                    fout(q, "waarschuwing", "item", f"stap {i}: {d['doel']} valt van niets (kans 0) en is geen start-item")
        for x in q.get("start_items", []):
            item(q, x[0], "start-item")
        for x in q.get("beloning", {}).get("items", []):
            item(q, x[0], "beloning")
    return quests, bev


def main():
    quests, bev = controleer()
    print(f"{len(quests)} quests, {len(bev)} bevindingen")
    for b in bev:
        print(f"  [{b['ernst']}] z{b['zone']} {b['quest']}: {b['soort']}: {b['tekst']}")
    if len(sys.argv) > 1:
        with open(sys.argv[1], "w", encoding="utf-8") as h:
            json.dump({"quests": len(quests), "bevindingen": bev}, h, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main()
