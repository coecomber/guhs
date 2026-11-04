"""Usage (project root): python tools/vadsrijk/loopcheck_doelen.py .
Rough survival check of the quest targets on the real map tiles: is there a floor, is the spot not boxed in?"""
import os
import re
import sys
from collections import deque

ROOT = sys.argv[1]
sys.path.insert(0, os.path.join(ROOT, "tools", "vadsrijk", "wereld"))
import nbtsnel  # noqa

TEG = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "structure", "vadsrijk", "tegels")
CACHE = {}


def tegel(tx, tz):
    k = (tx, tz)
    if k not in CACHE:
        pad = os.path.join(TEG, f"t_{tx}_{tz}.nbt")
        d = {}
        if os.path.exists(pad):
            root = nbtsnel.lees(pad)
            pal = [p["Name"] + ("[" + ",".join(f"{a}={b}" for a, b in sorted(p.get("Properties", {}).items())) + "]" if p.get("Properties") else "")
                   for p in root["palette"]]
            for b in root["blocks"]:
                x, y, z = b["pos"]
                d[(x, y, z)] = pal[b["state"]]
        CACHE[k] = d
    return CACHE[k]


def blok(x, y, z):
    tx, tz = x // 64, z // 64
    return tegel(tx, tz).get((x - tx * 64, y, z - tz * 64), "minecraft:air")


DOOR = ("_door", "_trapdoor", "deur", "_gate", "luik")
LUCHTIG = ("air", "grass", "fern", "flower", "tulip", "poppy", "dandelion", "orchid", "allium", "bluet", "daisy", "torch", "carpet",
           "sapling", "mushroom", "button", "lantern", "sign", "rail", "pressure_plate", "vine", "snow[layers=1", "litteken_plant",
           "short_", "tall_grass", "bush", "petals", "lily", "dead_bush", "candle", "_bed")


def lucht(b):
    n = b.split(":", 1)[1].split("[")[0] + ("[" + b.split("[", 1)[1] if "snow" in b and "[" in b else "")
    if "water" in n or "lava" in n:
        return False
    if any(d in n for d in DOOR):
        return True
    return any(w in n for w in LUCHTIG) and "mushroom_block" not in n and "_wall" not in n


def vast(b):
    n = b.split(":", 1)[1].split("[")[0]
    return not lucht(b) and "water" not in n and "lava" not in n and "fence" not in n and "_wall" not in n


def staan(x, y, z):
    return vast(blok(x, y - 1, z)) and lucht(blok(x, y, z)) and lucht(blok(x, y + 1, z))


def vloer(x, y, z):
    for dy in range(0, 13):
        for yy in (y - dy, y + dy):
            if staan(x, yy, z):
                return yy
    return None


def flood(x, y, z, r=24, max_n=4000):
    seen = {(x, y, z)}
    q = deque([(x, y, z)])
    verst = 0
    while q and len(seen) < max_n:
        cx, cy, cz = q.popleft()
        verst = max(verst, abs(cx - x), abs(cz - z))
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = cx + dx, cz + dz
            if abs(nx - x) > r or abs(nz - z) > r:
                continue
            for dy in (0, 1, -1, -2, -3):
                ny = cy + dy
                if (nx, ny, nz) in seen:
                    break
                if staan(nx, ny, nz) and (dy <= 0 or lucht(blok(cx, cy + 2, cz))):
                    seen.add((nx, ny, nz))
                    q.append((nx, ny, nz))
                    break
    return len(seen), verst


POI = {}
for m in re.finditer(r"\| `([^`]+)` \| (-?\d+), (-?\d+), (-?\d+) \|", open(os.path.join(ROOT, "POI_VADSRIJK.md"), encoding="utf-8").read()):
    POI[m.group(1)] = tuple(int(m.group(i)) for i in (2, 3, 4))


def w(i, dx=0, dz=0):
    x, y, z = POI[i]
    return (x + dx, y, z + dz)


DOELEN = {
    "npc rekruteerder": w("aankomst", 1, 4), "npc kapitein": w("luchtschip_kapitein"), "npc kok": w("kampvuur_kamp", 4, 4),
    "npc priesteres": w("tempeltje"), "npc trainer_ridder": w("training", -5, -1), "npc trainer_jager": w("training", 5, -1),
    "npc trainer_magier": w("training", 0, 6), "npc visser_dobber": (36, 87, 1072),
    "npc grootmeester": w("ordetempel_binnen"), "npc luchtschipper": w("luchtschip_haven_kapitein"),
    "npc boswachter_mos": w("kampvuur_sporenwoud_1", 6, -4), "npc mijnwerker_mopje": w("npc_mopje"),
    "npc tempelwachter_steeltje": (326, 152, -382), "npc klerk": w("questbord", 2, 1), "npc tweeling_tap": (-382, 68, 62),
    "poi kampplein": w("kampvuur_kamp"), "poi grotkamp": w("kampvuur_druipgrotten_1"), "poi tempelpoort": w("deur_zwamtempel.voor"),
    "poi litteken_weide": (380, 68, 320), "poi litteken_woud": (-240, 68, 300), "poi litteken_tempel": (145, 88, -265),
    "poi kampvuur_weide_1": w("kampvuur_klodderweide_1"), "poi vuurtoren": w("kampvuur_vuurtoren"), "poi dobberhoed": w("quest_dobber_hoed"),
    "poi hoedhupperwei": w("hoedhupperwei"), "poi tuba": w("quest_tuba"), "poi kristalkamer": w("kristalkamer"),
    "arena klodderreus deur": w("arena_klodderreus.deur"), "arena koning deur": w("arena_koning_klodder.deur"),
    "arena opperzwam deur": w("arena_opperzwam.deur"), "lift tempel voet": w("lift_tempel_voet"), "lift tempel top": w("lift_tempel_top"),
    "grot ingang 1": w("grot_ingangen.1"), "fossielbrug van": w("fossielbrug.van"), "fossielbrug naar": w("fossielbrug.naar"),
    "aankomst trap top": w("aankomst_trap_top"),
}
for naam, (x, y, z) in DOELEN.items():
    v = vloer(x, y, z)
    if v is None:
        print(f"!! {naam:28s} {x},{y},{z}: NO floor within 12")
        continue
    n, verst = flood(x, v, z)
    vlag = "!!" if n < 60 or verst < 12 else "ok"
    print(f"{vlag} {naam:28s} {x},{y},{z}: stand y {v} (dy {v - y:+d}), walkable {n} cells, reaches {verst} blocks out")
