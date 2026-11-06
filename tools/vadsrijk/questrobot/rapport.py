"""U-QUESTROBOT: makes QUESTROBOT_RAPPORT.md (project root) from
  1. the robot run (robot.json, written by the gametest VadsrijkQuestRobotGameTests or /guhs vadsrijk questrobot alles);
  2. the static checks (statisch.py: texts, links, actions, MELD/teller sources, items);
  3. the standability of every spot a quest sends you to, on the real map tiles (floor within reach + not boxed in).

Usage (project root):  python tools/vadsrijk/questrobot/rapport.py [path/to/robot.json]
Without a path it looks in run/questrobot/, run/gametest/questrobot/ and build/.
"""
import gzip
import json
import os
import struct
import sys
from collections import deque

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", "..", ".."))
sys.path.insert(0, HIER)
import statisch  # noqa: E402

TEG = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
ZONE_NAAM = {1: "Knuffelkamp / Zwamzoet", 2: "De Splinterpieken", 3: "De Schemerwortel", 4: "De Lampionkust", 5: "Hoogvads", 6: "Zijdeland"}


# ---- tiles (minimal NBT reader: only palette + blocks) ------------------------------------------------------------------------
def lees_tegel(pad):
    data = gzip.decompress(open(pad, "rb").read())
    p = [0]

    def rd(fmt):
        v = struct.unpack_from(fmt, data, p[0])
        p[0] += struct.calcsize(fmt)
        return v[0] if len(v) == 1 else v

    def rstr():
        n = rd(">H")
        s = data[p[0]:p[0] + n].decode()
        p[0] += n
        return s

    def payload(t):
        if t == 1: return rd(">b")
        if t == 2: return rd(">h")
        if t == 3: return rd(">i")
        if t == 4: return rd(">q")
        if t == 5: return rd(">f")
        if t == 6: return rd(">d")
        if t == 8: return rstr()
        if t == 9:
            et = rd(">B"); n = rd(">i")
            return [payload(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = rd(">B")
                if tt == 0:
                    return d
                k = rstr()
                d[k] = payload(tt)
        if t == 11:
            n = rd(">i"); return [rd(">i") for _ in range(n)]
        if t == 7:
            n = rd(">i"); p[0] += n; return None
        if t == 12:
            n = rd(">i"); p[0] += 8 * n; return None
        raise ValueError(t)

    rd(">B"); rstr()
    root = payload(10)
    pal = [x["Name"] for x in root["palette"]]
    d = {}
    for b in root["blocks"]:
        x, y, z = b["pos"]
        d[(x, y, z)] = pal[b["state"]]
    return d


CACHE = {}


def tegel(tx, tz):
    k = (tx, tz)
    if k not in CACHE:
        if len(CACHE) > 24:
            CACHE.pop(next(iter(CACHE)))
        pad = os.path.join(TEG, f"t_{tx}_{tz}.nbt")
        CACHE[k] = lees_tegel(pad) if os.path.exists(pad) else None
    return CACHE[k]


def blok(x, y, z):
    t = tegel(x // 64, z // 64)
    if t is None:
        return None
    return t.get((x - (x // 64) * 64, y, z - (z // 64) * 64), "minecraft:air")


LUCHT_EXACT = {"air", "cave_air", "void_air", "snow", "light", "short_grass", "tall_grass", "fern", "large_fern", "dead_bush", "vine",
               "cobweb", "sugar_cane", "wheat", "carrots", "potatoes", "beetroots", "lily_pad", "bush", "short_dry_grass", "tall_dry_grass",
               "glow_lichen", "moss_carpet", "pale_moss_carpet", "firefly_bush", "lilac", "peony", "rose_bush", "sunflower", "seagrass", "tall_seagrass"}
LUCHT_DEEL = ("tulip", "poppy", "dandelion", "orchid", "allium", "bluet", "daisy", "torch", "carpet", "sapling", "button", "lantern", "sign",
              "rail", "pressure_plate", "candle", "_bed", "_door", "_trapdoor", "fence_gate", "banner", "_head", "skull", "petals", "flowerbed",
              "leaf_litter", "litteken_plant", "cornflower", "lily_of_the_valley", "wither_rose", "berry_bush", "_coral_fan", "pickle")


def lucht(b):
    n = b.split(":", 1)[1].split("[")[0]
    if "water" in n or "lava" in n:
        return False
    if n.endswith(("_block", "_planks", "_slab", "_stairs", "_log", "_wood", "_leaves", "_stem")):
        return False
    return n in LUCHT_EXACT or any(w in n for w in LUCHT_DEEL) or (n.endswith("mushroom"))


def vast(b):
    n = b.split(":", 1)[1]
    return not lucht(b) and "water" not in n and "lava" not in n and "fence" not in n and "_wall" not in n and "bars" not in n


def staan(x, y, z):
    a, b, c = blok(x, y - 1, z), blok(x, y, z), blok(x, y + 1, z)
    return a is not None and vast(a) and lucht(b) and lucht(c)


def vloer(x, y, z, straal=0, dy_max=8):
    for dy in range(0, dy_max + 1):
        for yy in (y - dy, y + dy):
            for dx in range(-straal, straal + 1):
                for dz in range(-straal, straal + 1):
                    if staan(x + dx, yy, z + dz):
                        return (x + dx, yy, z + dz)
    return None


def flood(x, y, z, r=12, max_n=400):
    seen = {(x, y, z)}
    q = deque([(x, y, z)])
    while q and len(seen) < max_n:
        cx, cy, cz = q.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = cx + dx, cz + dz
            if abs(nx - x) > r or abs(nz - z) > r:
                continue
            for dy in (0, 1, -1, -2, -3):
                ny = cy + dy
                if (nx, ny, nz) in seen:
                    break
                if staan(nx, ny, nz) and (dy <= 0 or lucht(blok(cx, cy + 2, cz) or "minecraft:stone")):
                    seen.add((nx, ny, nz))
                    q.append((nx, ny, nz))
                    break
    return len(seen)


def staanbaar(d):
    """None = ok, else (ernst, why)."""
    x, y, z = d["x"], d["y"], d["z"]
    if tegel(x // 64, z // 64) is None:
        # (a quest target outside every generated tile = outside the map; only "info" for a zone whose tiles are not in this tree)
        zone = statisch.zone_van(d["quest"])
        heeft_zone = zone <= 5 or os.path.exists(os.path.join(ROOT, f"POI_Z{zone}.md"))
        return ("fout" if heeft_zone else "info", "ligt buiten de gegenereerde kaarttegels (geen kaart hier)")
    straal = 3 if d["wat"] in ("poi", "plek", "escorte") else 1
    v = vloer(x, y, z, straal=straal, dy_max=10 if d["wat"] != "npc" else 4)
    if v is None:
        return ("fout", f"geen plek om te staan binnen {straal} blokken / {10 if d['wat'] != 'npc' else 4} hoog")
    n = flood(*v)
    if n < 25:
        return ("waarschuwing", f"staanplek {v} is ingesloten ({n} loopbare cellen in de buurt)")
    return None


# what the first robot runs found and U-QUESTROBOT fixed (kept in the report so the next run's "all green" is explained)
GEREPAREERD = [
    "**z4_s11_schildpad** (zone 4) liep vast: het zeewier viel van mobs in gebied `z4_lampionkust`, maar dat gebied bestaat niet "
    "(Gebieden); het viel dus nooit. Nu: elk zone-4-gebied (`gebied:a|b|...`; `NpcHaken.pastMob` kent nu `a|b`). quests_z4.json + generator.",
    "**Escortes zonder eigen tekst** (z10_rammel, z3_s02_opa_bast, z5_s07_kadet): de NPC zei een ontbrekende tekstsleutel "
    "(`...praat.<npc>.0`) als hij ging volgen. Nu de algemene regel '<naam> loopt met je mee!' (QuestMotor).",
    "**Opstijgpier (zone 4 -> 5)**: `Z5VerhaalPlekken.OPSTIJGPIER` viel terug op een placeholder in open zee (2452,66,-520), ~75 blokken "
    "van de echte pier: de tracker van z5_hq01 stuurde je het water in en de zone-5-ticker liep nooit op de pier. Nu de zone-4-pier.",
    "**z4_s16_flessenpost**: de poi `z4_kringstroom` lag 9 blokken BUITEN de zone-4-kaart (z -248, kaart tot z -257, de stroming duwt "
    "je daar terug): onhaalbaar. Nu op het water binnen de kaart (1960,63,-270), in de randstroming.",
    "**Verloren teksten** (een gedeeltelijke lang-regeneratie in 110-u-specs): 20 ambacht-NPC-namen + praatjes (amb_*), de reiskaart-plek "
    "'De Lampionkust', de luchtschip-keuzes/slot-teksten naar Lampionhaven, de rol-knoppen werkplaats/luchtschip_z4. 89 sleutels "
    "teruggezet in nl_nl + en_us (de oude adv-quests en de door u-karakter geschrapte karakter-teksten bewust niet). Hierdoor faalde "
    "ook de bestaande gametest Z4VerhaalGameTests.elkeStapWieEnWaar.",
]
VOOR_LEAD = [
    "**Zone 6** (110-u-z6verhaal) was nog niet gecommit: de robot leest `Quests.alle()`, dus zodra `quests_z6.json` in Quests staat "
    "speelt `/guhs vadsrijk questrobot alles` zone 6 mee (vlaggen zone6_klaar + z6_lijn_e_open worden dan ook gecontroleerd). "
    "Statische controle van de werkboom u-z6verhaal (QR_ROOT): 28 quests, alles in orde behalve **z6_s05_afdingen**: de 3 stappen "
    "`z6_puzzel:afdingen_kardemom|pistache|lokum` worden door geen enkele code gemeld (de afdingpuzzel bestaat nog niet).",
    "**Rijdier-terugbrengquests van zone 1** (rijdier_hoedhupper lv 8, rijdier_knuffelhert lv 10) komen pas vrij als je een rijdier hebt "
    "(`@rijdier`): het eerste rijdier is de zone-3-starter (of kopen bij de stal). Bedoeld? Dan staan ze lang 'onzichtbaar' in zone 1.",
    "Niet echt gespeeld (alleen via de haak gemeld, zie Robot-notities): 39 minigame/moment-meldingen (o.a. de zone-5-verhoren en "
    "bewijsbord, vliegerwedstrijd, volleybal, vissen, de zone-3-bazen), 14 tellers (verzamelplekjes per zone: of er genoeg plekjes "
    "in de wereld liggen is niet gecontroleerd), 4 escortes (lopen) en de gevechten.",
    "Karakter-slots: queststaat zit per karakter in `extra(\"npc\")` (u-karakter wisselt die mee); de robot speelt met een verse speler en "
    "test het wisselen zelf niet.",
]


# ---- report -------------------------------------------------------------------------------------------------------------------
def zoek_robot():
    for p in (os.path.join(ROOT, "run", "questrobot", "robot.json"), os.path.join(ROOT, "run", "gametest", "questrobot", "robot.json"),
              os.path.join(ROOT, "build", "questrobot", "robot.json")):
        if os.path.exists(p):
            return p
    return None


def main():
    pad = sys.argv[1] if len(sys.argv) > 1 else zoek_robot()
    robot = json.load(open(pad, encoding="utf-8")) if pad and os.path.exists(pad) else None
    quests, stat = statisch.controleer()
    per_zone = {}
    for q in quests:
        per_zone.setdefault(statisch.zone_van(q["id"]), []).append(q["id"])
    uit = {u["quest"]: u for u in robot["quests"]} if robot else {}
    stand = []
    if robot:
        for d in robot["doelen"]:
            s = staanbaar(d)
            if s:
                stand.append((d, s))
    stat_per_q = {}
    for b in stat:
        stat_per_q.setdefault(b["quest"], []).append(b)
    stand_per_q = {}
    for d, s in stand:
        stand_per_q.setdefault(d["quest"], []).append((d, s))

    L = ["# QUESTROBOT_RAPPORT", "",
         "Automatisch gemaakt door `tools/vadsrijk/questrobot/rapport.py` uit de speelronde van de questrobot "
         "(gametest `VadsrijkQuestRobotGameTests` of `/guhs vadsrijk questrobot alles`), de statische controle en de staanplek-controle op de kaarttegels.",
         ""]
    L += ["## Samenvatting (Nederlands)", ""]
    if not robot:
        L += ["**Geen robot-run gevonden**: alleen de statische controle staat hieronder.", ""]
    L += ["| Zone | Quests | Gehaald | Vast | Niet gestart | Statische fouten | Staanplek-fouten |", "|---|---|---|---|---|---|---|"]
    for z in sorted(per_zone):
        ids = per_zone[z]
        g = sum(1 for i in ids if uit.get(i, {}).get("status") == "gehaald")
        v = sum(1 for i in ids if uit.get(i, {}).get("status") == "vast")
        n = sum(1 for i in ids if uit.get(i, {}).get("status") == "niet gestart")
        sf = sum(1 for b in stat if b["zone"] == z and b["ernst"] == "fout")
        st = sum(1 for d, s in stand if statisch.zone_van(d["quest"]) == z and s[0] == "fout")
        L.append(f"| {z} {ZONE_NAAM.get(z, '')} | {len(ids)} | {g if robot else '-'} | {v if robot else '-'} | {n if robot else '-'} | {sf} | {st} |")
    L.append("")
    if robot:
        L += ["Zonevlaggen en luchtschip-poorten (gecontroleerd zodra het hoofdverhaal van een zone klaar was):", ""]
        L += [f"- {s}" for s in robot["algemeen"]] or ["- (geen zone helemaal klaar gekomen)"]
        L.append("")
    L += ["Legenda: **gehaald** = van aannemen tot inleveren door de echte quest-motor gespeeld; **vast** = de robot liep vast "
          "(en duwde de quest daarna met de hand door, zodat alles erachter ook getest werd); **niet gestart** = kwam nooit vrij.",
          "", "Wat de robot NIET echt speelt (en via de haak van het spel meldt): gevechten (VERSLAGEN-haak), minigames/momenten "
          "(NpcApi.meld), lopen met een escorte, de luchtschipvluchten. De statische controle kijkt wel of er code bestaat die zo'n melding doet.", ""]

    L += ["## Per zone (technisch)", ""]
    for z in sorted(per_zone):
        L += [f"### Zone {z}: {ZONE_NAAM.get(z, '')}", ""]
        for qid in per_zone[z]:
            u = uit.get(qid)
            sb = stat_per_q.get(qid, [])
            st = stand_per_q.get(qid, [])
            probs = (u or {}).get("problemen", [])
            if u and u["status"] == "gehaald" and not sb and not probs and not [1 for d, s in st if s[0] != "info"]:
                continue
            kop = f"- **{qid}**" + (f" ({u['soort']}): **{u['status']}**" if u else "")
            if u and u["stap"]:
                kop += f" bij {u['stap']}"
            if u and u["waarom"]:
                kop += f": {u['waarom']}"
            L.append(kop)
            for p in probs:
                L.append(f"  - robot: {p}")
            for b in sb:
                L.append(f"  - statisch [{b['ernst']}] {b['soort']}: {b['tekst']}")
            for d, s in st:
                if s[0] != "info":
                    L.append(f"  - staanplek [{s[0]}] {d['wat']} `{d['id']}` @ {d['x']},{d['y']},{d['z']}: {s[1]}")
        ok = sum(1 for i in per_zone[z] if uit.get(i, {}).get("status") == "gehaald" and not stat_per_q.get(i) and not uit[i].get("problemen")
                 and not [1 for d, s in stand_per_q.get(i, []) if s[0] != "info"])
        L += ["", f"({ok} van de {len(per_zone[z])} quests zonder enige opmerking zijn hierboven weggelaten.)", ""]
    if robot:
        L += ["## Robot-notities (wat gesimuleerd is)", ""]
        for qid, u in uit.items():
            if u["notities"]:
                L.append(f"- `{qid}`: " + "; ".join(u["notities"]))
        info = [(d, s) for d, s in stand if s[0] == "info"]
        if info:
            L += ["", f"{len(info)} doelplekken liggen buiten de gegenereerde kaarttegels (niet op staanbaarheid gecontroleerd): "
                  + ", ".join(sorted({d['id'] for d, s in info}))]
    L += ["", "## Gerepareerd (U-QUESTROBOT, 2026-11-06)", ""] + [f"- {x}" for x in GEREPAREERD]
    L += ["", "## Voor de lead (ontwerp / andere slices)", ""] + [f"- {x}" for x in VOOR_LEAD]
    with open(os.path.join(ROOT, "QUESTROBOT_RAPPORT.md"), "w", encoding="utf-8") as h:
        h.write("\n".join(L) + "\n")
    print("\n".join(L[:40]))


if __name__ == "__main__":
    main()
