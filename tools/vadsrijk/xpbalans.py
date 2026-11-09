"""W-BALANS: xp balance per zone of Het Vadsrijk (zones 1-6).

Reads the quest data (src/main/resources/data/guhs/vadsrijk/npc/quests*.json) and models the rest from the Java formulas:
  * curve      kern/Levels.java          stap(l) = round(40 + 25 * l^1.6)   (generic level gates quests)
  * mobs z1    wezens/mob/*.java          ~16 + 3*lv
  * mobs z2    zone2/wezens/mob/*.java    ~34 + 4*lv
  * mobs z3-6  Z3Xp.mob / Z5Wezen.xp5 / Z6Wezen.xp   stap(lv)/55 * gewicht (gewicht ~1)
  * elite      3x a normal mob (kern)
  * bosses     z3-z6 0.35 * stap(lv) (Z3Xp.baas, Z5Baas, Z6Baas); z1/z2 fixed formulas
  * dungeon    grotkerker/GrotKerker + KerkerData: solo run = 3 + 4 pack mobs + 1 elite + GrotBaas (max(150, 0.15*stap(baasLv)))
Bonuses (rested xp, food, guild bond, streak, party) are NOT counted: this is the plain solo baseline.

Simulation per zone (greedy, like a normal player): before each main quest do every side quest that is unlocked and whose level
is met; when a quest's level gate blocks, grind the zone's own mobs (counted as minutes); at the end grind to the TARGET (the level
the next zone needs). Every scenario enters a zone at max(own end level, the zone's entry gate).

Usage:  python tools/vadsrijk/xpbalans.py [--kpm 2.0] [--clears 1] [--md]
"""
import argparse
import glob
import json
import os
import sys

HIER = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HIER, "..", ".."))
QDIR = os.path.join(ROOT, "src", "main", "resources", "data", "guhs", "vadsrijk", "npc")
MAX = 70


def stap(l):
    l = max(1, min(MAX - 1, l))
    return max(1, round(40 + 25 * l ** 1.6))


TOT = [0, 0]
for _l in range(1, MAX + 1):
    TOT.append(TOT[-1] + stap(_l))


def totaal(level):
    return TOT[level]


def level_van(xp):
    l = 1
    while l < MAX and xp >= TOT[l + 1]:
        l += 1
    return l


def level_f(xp):
    """Level with a fraction (52.40 = 40 % into 52)."""
    l = level_van(xp)
    if l >= MAX:
        return float(MAX)
    return l + (xp - TOT[l]) / stap(l)


# ---- zones ---------------------------------------------------------------------------------------------------------------------
# file, entry gate (first main quest lv), target (the level the NEXT zone needs: its first main quest / band), mob areas
# (name, minLv, maxLv), dungeon (name, minLv, maxLv, eliteLv, baasLv), optional first boss kills (name, xp-function args)
ZONES = {
    1: dict(naam="Het Klodderdal", file="quests.json", doel=20, mob="z1",
            gebieden={"klodderweide": (1, 6), "sporenwoud": (6, 13), "druipgrotten": (12, 18), "zwamtempel": (18, 20)},
            kerker=("Zwamgrot", 11, 13, 13, 14)),
    2: dict(naam="De Splinterpieken", file="quests_z2.json", doel=30, mob="z2",
            gebieden={"z2_galmhellingen": (20, 25), "z2_knuffelmeer": (25, 30), "z2_splinterkloof": (30, 34), "z2_spiegelburcht": (33, 35)},
            kerker=("Kristalgrot", 22, 24, 24, 25)),
    3: dict(naam="De Schemerwortel", file="quests_z3.json", doel=50, mob="std",
            gebieden={"z3_kruinzee": (30, 34), "z3_takkenbanen": (34, 38), "z3_schorsstad": (40, 43), "z3_wortelvelden": (43, 46),
                      "z3_wortelduister": (46, 49)},
            kerker=("Wortelgrot", 44, 46, 47, 48)),
    4: dict(naam="De Lampionkust", file="quests_z4.json", doel=56, mob="std",
            gebieden={"rotsjes": (50, 52), "bloemeneiland": (51, 52), "vliegereiland": (52, 53), "koraalbaai": (53, 54), "wrak": (54, 55),
                      "mysterie": (55, 56)},
            kerker=("Koraalgrot", 53, 55, 55, 56)),
    5: dict(naam="Hoogvads", file="quests_z5.json", doel=60, mob="std",
            gebieden={"z5_buikgangen": (57, 59), "z5_wolkrand": (58, 60), "z5_staartvlakte": (58, 60), "z5_schuilnest": (58, 60)},
            kerker=("Wolkengrot", 58, 60, 60, 60)),
    6: dict(naam="Het Zijdeland", file="quests_z6.json", doel=69, mob="std",
            gebieden={"duinen": (60, 62), "palmenkloven": (62, 64), "zoutvlakte": (64, 66), "ruinedal": (66, 68), "moederoase": (68, 70)},
            kerker=("Zingende Zoutgrot", 64, 66, 66, 67)),
}


def mob_xp(zone, lv):
    m = ZONES[zone]["mob"]
    if m == "z1":
        return 16 + 3 * lv
    if m == "z2":
        return 34 + 4 * lv
    return max(1, round(stap(lv) / 55.0))


def baas_std(lv):
    return max(1, round(stap(min(lv, MAX - 1)) * 0.35))


# boss xp per quest event / objective (estimated boss levels from the registries; z3 bosses cap at lv 50 in Z3Xp)
BAAS_EVENT = {
    "guhs:vr_klodderreus": lambda: 180 + 20 * 4,
    "gebied:zwamtempel": lambda: 1500 + 100 * 20,        # Opperzwam
    "gebied:klodderweide": lambda: 600 + 60 * 8,         # Koning Klodder
    "spion:gevangen": lambda: 900 + 80 * 12,             # Mika-spion
    "gebied:z2_galmhellingen": lambda: 3000 + 120 * 24,  # Galmwolf
    "gebied:z2_spiegelburcht": lambda: 4200 + 150 * 34,  # Spiegelzucht
    "z2_boormeester:verslagen": lambda: 4000 + 140 * 30,
    "z3_taleggio:verslagen": lambda: baas_std(38), "z3_brokkel:verslagen": lambda: baas_std(38),
    "z3_roquefort:verslagen": lambda: baas_std(39), "z3_piepvel:verslagen": lambda: baas_std(41),
    "z3_wortelwurger:verslagen": lambda: baas_std(47), "z3_nestwachter:opgevrolijkt": lambda: baas_std(47),
    "z3_bleu:verslagen": lambda: baas_std(48), "z3_vahoegnir:opgevrolijkt": lambda: baas_std(50),
    "z3_imker:verslagen": lambda: baas_std(47),
    "z4_vliegervanger:verslagen": lambda: baas_std(56), "z4_zeeslurf:opgevrolijkt": lambda: baas_std(54),
    "z5_zilverstaart:verslagen": lambda: baas_std(60), "z5_droom:opgevrolijkt": lambda: baas_std(60),
    "z6:zoutkolos": lambda: baas_std(66), "z6_baas:wormelien": lambda: baas_std(63),
}
# optional bosses with no quest (first kill only), per zone
EXTRA_BAZEN = {
    1: [], 2: [], 3: [],
    4: [],
    5: [("Gorgonzola", lambda: baas_std(59)), ("Donderbuik", lambda: baas_std(60))],
    6: [("Astrolabiumwachter", lambda: baas_std(66)), ("Glimmertje", lambda: baas_std(67)), ("Sterrenguh", lambda: baas_std(68))],
}

ZIJ = ("zij", "rijdier")


SCHAAL = {}   # what-if: {zone: (main factor, side factor)} (--schaal 4:1.4:1.2)


def laad(zone):
    with open(os.path.join(QDIR, ZONES[zone]["file"]), encoding="utf-8") as f:
        qs = json.load(f)["quests"]
    if zone in SCHAAL:
        fh, fz = SCHAAL[zone]
        for q in qs:
            q["beloning"]["xp"] = round(q["beloning"]["xp"] * (fh if q["soort"] in ("hoofd", "klasse") else fz))
    return qs


def gebied_lv(zone, doel):
    g = ZONES[zone]["gebieden"]
    naam = doel.split(":", 1)[1] if ":" in doel else doel
    for k, (a, b) in g.items():
        if naam == k or naam.startswith(k) or k.startswith(naam):
            return (a + b) // 2
    lo = min(a for a, _b in g.values())
    return lo + 1


def kill_xp(zone, q):
    """Xp from the kills a quest itself asks for (cheer-ups, elites, bosses, drop collecting)."""
    xp = 0
    for d in q["doelen"]:
        s, doel, n = d["soort"], d["doel"], d["n"]
        if s == "opvrolijk":
            xp += n * mob_xp(zone, gebied_lv(zone, doel))
        elif s == "elite":
            xp += 3 * mob_xp(zone, gebied_lv(zone, doel))
        elif s == "baas" or (s == "meld" and doel in BAAS_EVENT):
            f = BAAS_EVENT.get(doel)
            xp += f() if f else 0
        elif s == "verzamel" and d.get("kans", 0) > 0 and d.get("bron", "").startswith("gebied:"):
            xp += round(n / d["kans"]) * mob_xp(zone, gebied_lv(zone, d["bron"].split("|")[0]))
    return xp


def kerker_xp(zone):
    _n, lo, hi, elv, blv = ZONES[zone]["kerker"]
    mid = (lo + hi) / 2
    pack = 7 * mob_xp(zone, round(mid))
    elite = 3 * mob_xp(zone, elv)
    baas = max(150, round(stap(blv) * 0.15))
    return pack + elite + baas


def grind_rate(zone, level, kpm):
    """Xp per minute grinding the best area of THIS zone the player can handle (area minLv <= level + 2)."""
    best = None
    for a, b in ZONES[zone]["gebieden"].values():
        if a <= level + 2:
            lv = min(b, max(a, level))
            if best is None or lv > best:
                best = lv
    if best is None:
        best = min(a for a, _b in ZONES[zone]["gebieden"].values())
    return kpm * mob_xp(zone, best), best


def simuleer(zone, xp_in, met_zij, clears, kpm, extra_bazen, onderweg=0):
    qs = laad(zone)
    hoofd = [q for q in qs if q["soort"] == "hoofd"]
    zij = [q for q in qs if q["soort"] in ZIJ] if met_zij else []
    if met_zij:   # one class advancement (all three are offered; you do the one of your class)
        kl = [q for q in qs if q["soort"] == "klasse"]
        if kl:
            zij.append(kl[0])
    klaar = set()
    xp = xp_in
    poort_min = 0.0
    bron = dict(hoofd=0, zij=0, kills=0, bazen=0, kerker=0)
    ids_zone = {q["id"] for q in qs}

    def open_(q):
        # prerequisites: done here, from an earlier zone (done), or the mount gate (you own a mount by then)
        return all(n in klaar or n not in ids_zone for n in q["na"])

    def grind_tot(lv):
        nonlocal xp, poort_min
        while level_van(xp) < lv:
            r, _a = grind_rate(zone, level_van(xp), kpm)
            nodig = min(totaal(level_van(xp) + 1), totaal(lv)) - xp
            poort_min += nodig / r
            xp += nodig

    def doe(q, soort):
        nonlocal xp
        k = kill_xp(zone, q)
        if onderweg:   # mobs you fight on the way to the objective (normal play), at the best area level you can handle
            _r, alv = grind_rate(zone, level_van(xp), 1)
            k += onderweg * mob_xp(zone, alv)
        bron["bazen" if any(d["soort"] == "baas" or d["doel"] in BAAS_EVENT for d in q["doelen"]) else "kills"] += k
        xp += k
        xp += q["beloning"]["xp"]
        bron[soort] += q["beloning"]["xp"]
        klaar.add(q["id"])

    kerker_lo = ZONES[zone]["kerker"][1]
    gedaan_clears = 0

    def zijrondje():
        nonlocal xp, gedaan_clears
        bezig = True
        while bezig:
            bezig = False
            for q in zij:
                if q["id"] not in klaar and open_(q) and level_van(xp) >= q["lv"]:
                    doe(q, "zij")
                    bezig = True
        while gedaan_clears < clears and level_van(xp) >= kerker_lo - 1:
            k = kerker_xp(zone)
            xp += k
            bron["kerker"] += k
            gedaan_clears += 1

    for q in hoofd:
        zijrondje()
        grind_tot(q["lv"])
        doe(q, "hoofd")
    zijrondje()
    if extra_bazen:
        for _n, f in EXTRA_BAZEN.get(zone, []):
            k = f()
            xp += k
            bron["bazen"] += k
    # side quests whose level was never met: grind for them only if close (<= 1 level), else skip
    rest = [q for q in zij if q["id"] not in klaar]
    eind_quests = xp
    doel = ZONES[zone]["doel"]
    tekort = max(0, totaal(doel) - eind_quests)
    r, area = grind_rate(zone, level_van(eind_quests), kpm)
    return dict(xp_in=xp_in, lv_in=level_f(xp_in), xp_uit=eind_quests, lv_uit=level_f(eind_quests), poort_min=poort_min,
                tekort=tekort, rest_min=tekort / r if tekort else 0.0, rate=r, area=area, bron=bron, overgeslagen=[q["id"] for q in rest],
                doel=doel)


SCENARIOS = (("A", "hoofd", False, 0), ("B", "hoofd+zij+bazen", True, 0), ("C", "B + 1 kerker-run", True, 1))


def poort(z):
    return min(q["lv"] for q in laad(z) if q["soort"] == "hoofd")


def keten(met_zij, clears, kpm, onderweg):
    """All six zones in a row; each zone entered at max(own xp, the zone's entry gate); the player grinds to the target first."""
    uit = {}
    xp = 0
    for z in range(1, 7):
        xp = max(xp, totaal(poort(z)))
        r = simuleer(z, xp, met_zij, clears, kpm, extra_bazen=met_zij, onderweg=onderweg)
        uit[z] = r
        xp = max(r["xp_uit"], totaal(r["doel"]))
    return uit


def los(met_zij, clears, kpm, onderweg):
    """Every zone on its own, entered at EXACTLY its entry gate (the player who only just made it: the strict test)."""
    return {z: simuleer(z, totaal(poort(z)), met_zij, clears, kpm, extra_bazen=met_zij, onderweg=onderweg) for z in range(1, 7)}


def puur():
    """Quest xp only, no kills, no grinding at all (a pure cumulative number)."""
    out = {}
    m = s = 0
    for z in range(1, 7):
        qs = laad(z)
        m += sum(q["beloning"]["xp"] for q in qs if q["soort"] == "hoofd")
        kl = [q["beloning"]["xp"] for q in qs if q["soort"] == "klasse"]
        s += sum(q["beloning"]["xp"] for q in qs if q["soort"] == "hoofd" or q["soort"] in ZIJ) + (kl[0] if kl else 0)
        out[z] = (level_f(m), level_f(s))
    return out


def _g(r):
    t = r["poort_min"] + r["rest_min"]
    return f"{t:.0f} min" if t >= 0.5 else "0"


def tabel(kpm, onderweg):
    w = print
    w(f"Aannames: solo, geen bonussen (rust/eten/bond/reeks/groep), grinden {kpm:g} kills/min in de beste mobzone van de zone zelf, "
      f"{onderweg} kills onderweg per quest (normaal spelen).")
    w("A = alleen hoofdquests; B = hoofd + zij + 1 klasse-quest + eerste keer elke baas; C = B + 1 kerker-run (solo).")
    w("Grind = minuten mobs in DEZE zone, voor levelpoorten onderweg + tot het doel.")
    for titel, f in (("Keten (zone binnen op het eigen level uit de vorige zone)", keten),
                     ("Los (zone binnen op PRECIES het poortlevel: de strenge toets)", los)):
        res = {k: f(zij, cl, kpm, onderweg) for k, _n, zij, cl in SCENARIOS}
        w("")
        w(f"### {titel}")
        w("")
        w("| Zone | Doel | Binnen A / B | Uit A | Uit B | Uit C | Grind A | Grind B | Grind C | Grindtempo |")
        w("|---|---|---|---|---|---|---|---|---|---|")
        for z in range(1, 7):
            a, b, c = res["A"][z], res["B"][z], res["C"][z]
            w(f"| {z} {ZONES[z]['naam']} | {a['doel']} | {a['lv_in']:.1f} / {b['lv_in']:.1f} | {a['lv_uit']:.2f} | {b['lv_uit']:.2f} | "
              f"{c['lv_uit']:.2f} | {_g(a)} | {_g(b)} | {_g(c)} | {b['rate']:.0f} xp/min (mobs lv {b['area']}) |")
    res = los(True, 1, kpm, onderweg)
    w("")
    w("### Bronnen per zone (scenario C, los)")
    w("")
    w("| Zone | Hoofd-xp | Zij-xp | Kills (quest + onderweg) | Bazen (1e keer) | 1 kerker-run | Kerker | Zij niet gehaald (level) |")
    w("|---|---|---|---|---|---|---|---|")
    for z in range(1, 7):
        b = res[z]["bron"]
        w(f"| {z} | {b['hoofd']} | {b['zij']} | {b['kills']} | {b['bazen']} | {kerker_xp(z)} | {ZONES[z]['kerker'][0]} "
          f"(lv {ZONES[z]['kerker'][1]}-{ZONES[z]['kerker'][2]}) | {', '.join(res[z]['overgeslagen']) or '-'} |")
    p = puur()
    w("")
    w("Puur quest-xp vanaf level 1, zonder een enkele kill (cumulatief): "
      + ", ".join(f"z{z} hoofd {p[z][0]:.1f} / alles {p[z][1]:.1f}" for z in p) + ".")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--kpm", type=float, default=2.0)
    ap.add_argument("--onderweg", type=int, default=5)
    ap.add_argument("--zone", type=int, default=0, help="detail of one zone (entered at its gate)")
    ap.add_argument("--schaal", action="append", default=[], help="what-if quest xp: zone:main:side, e.g. 4:1.4:1.2")
    a = ap.parse_args()
    for x in a.schaal:
        z, fh, fz = x.split(":")
        SCHAAL[int(z)] = (float(fh), float(fz))
    if a.zone:
        z = a.zone
        for k, naam, zij, cl in SCENARIOS:
            r = simuleer(z, totaal(poort(z)), zij, cl, a.kpm, extra_bazen=zij, onderweg=a.onderweg)
            print(f"z{z} {k} {naam:18s} in lv {r['lv_in']:.2f} -> {r['lv_uit']:.2f}; doel {r['doel']}: tekort {r['tekort']} xp = "
                  f"{r['rest_min']:.0f} min (+{r['poort_min']:.0f} min voor poorten) @ {r['rate']:.0f} xp/min; bron {r['bron']}")
        return
    tabel(a.kpm, a.onderweg)


if __name__ == "__main__":
    sys.exit(main())
