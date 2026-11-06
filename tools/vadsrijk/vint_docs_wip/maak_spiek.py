"""Builds the round-U SPIEKBRIEF.md in the v-int tree: python maak_spiek.py <tree> <old SPIEKBRIEF from git> <kop.md> <zones.md> <feedback.md>
Structure: start (kop.md) -> 1 world (old 1 + U world slices) -> zone 1..6 (old + "nieuw in ronde U" blocks from zones.md) -> systems
(old 6 + U system slices) -> feedback list."""
import os
import re
import sys

boom, oud_pad, kop_pad, zones_pad, fb_pad = sys.argv[1:6]
oud = open(oud_pad, encoding="utf-8").read()
kop = open(kop_pad, encoding="utf-8").read()
zones = open(zones_pad, encoding="utf-8").read()
fb = open(fb_pad, encoding="utf-8").read()


def hoofdstukken(txt):
    """'# N. TITLE' chapters of the old cheat sheet -> {N: body (without the heading)}."""
    delen = re.split(r"^# (\d)\. [^\n]*\n", txt, flags=re.M)
    uit = {}
    for i in range(1, len(delen), 2):
        uit[int(delen[i])] = delen[i + 1].strip("\n- \n")
    return uit


def zone_blok(n):
    m = re.search(r"^@@ZONE " + str(n) + r"\n(.*?)(?=^@@ZONE |\Z)", zones, flags=re.M | re.S)
    return m.group(1).strip() if m else ""


def spiek(naam):
    p = os.path.join(boom, naam)
    if not os.path.exists(p):
        return ""
    t = open(p, encoding="utf-8").read().strip()
    # every heading one level deeper (they live under a ## block)
    t = re.sub(r"^(#+) ", lambda m: "#" * min(6, len(m.group(1)) + 1) + " ", t, flags=re.M)
    if not t.startswith("##"):
        t = "## " + naam[:-3] + "\n\n" + t
    return t


H = hoofdstukken(oud)
WERELD_U = ["SPIEK_U_OVERGANG.md", "SPIEK_U_HEKKEN.md", "SPIEK_U_LUCHTSCHIP.md", "SPIEK_U_KARAKTER.md", "SPIEK_U_TABS.md",
            "SPIEK_U_TOETSEN.md", "SPIEK_U_DUBBEL.md", "SPIEK_U_DECO.md"]
SYSTEEM_U = ["SPIEK_U_SPECS.md", "SPIEK_U_ZICHTBAAR.md", "SPIEK_U_HUD.md", "SPIEK_U_AUDIO2.md", "SPIEK_U_AMBACHT2.md",
             "SPIEK_U_GROTTEN.md", "SPIEK_U_RUITERS.md", "SPIEK_U_TAPIJT.md", "SPIEK_U_GILDEN.md", "SPIEK_U_HUIZEN.md",
             "SPIEK_U_QUESTROBOT.md", "SPIEK_U_GROOTTE.md", "SPIEK_U_SITE.md"]
ZONE5_U = ["SPIEK_U_Z5.md", "SPIEK_U_Z5BINNEN.md", "SPIEK_U_Z5TUIN.md"]
ZONE6_U = ["SPIEK_U_Z6WERELD.md", "SPIEK_U_Z6VERHAAL.md", "SPIEK_U_Z6WEZENS.md", "SPIEK_U_Z6RAID.md", "SPIEK_U_Z6MUZIEK.md"]
gebruikt = set(WERELD_U + SYSTEEM_U + ZONE5_U + ZONE6_U)
rest = sorted(f for f in os.listdir(boom) if f.startswith("SPIEK_U_") and f.endswith(".md") and f not in gebruikt)

out = [kop.strip(), ""]
out += ["# 1. START EN DE NIEUWE WERELD", "",
        "Eerst hoe alles aan elkaar hangt (zee, overgangen, havens, hekken), dan de luchtschepen, je karakters, het nieuwe "
        "Vadsrijk-menu (H) en de toetsen. Ronde U staat bovenaan, de blokken van de nachtronde daaronder (die gelden nog steeds).", ""]
out += [s for s in (spiek(f) for f in WERELD_U) if s]
out += ["", "## Uit de nachtronde (nog steeds geldig)", "", H.get(1, ""), ""]
out += ["# 2. ZONE 1 (Het Tussenland, lv 1-20) EN ZONE 2 (De Splinterpieken, lv 20-35)", "", zone_blok(1), "", zone_blok(2), "",
        "## Uit de nachtronde: zone 1 en 2 (nieuwe coördinaten, oude testblokken)", "", H.get(2, ""), ""]
out += ["# 3. ZONE 3: DE SCHEMERWORTEL (lv 35-50)", "", zone_blok(3), "", "## Uit de nachtronde: zone 3", "", H.get(3, ""), ""]
out += ["# 4. ZONE 4: DE LAMPIONKUST (lv 50-56)", "", zone_blok(4), "", "## Uit de nachtronde: zone 4", "", H.get(4, ""), ""]
out += ["# 5. ZONE 5: HOOGVADS (lv 56-60)", "", zone_blok(5), ""]
out += [s for s in (spiek(f) for f in ZONE5_U) if s]
out += ["", "## Uit de nachtronde: zone 5", "", H.get(5, ""), ""]
z6 = [s for s in (spiek(f) for f in ZONE6_U) if s]
if z6 or zone_blok(6):
    out += ["# 6. ZONE 6: HET ZIJDELAND (lv 60-70)", "", zone_blok(6), ""] + z6 + [""]
out += ["# 7. DE SYSTEMEN", "", "Eerst alles wat in ronde U nieuw is of veranderd, daarna de systemen van de nachtronde.", ""]
out += [s for s in (spiek(f) for f in SYSTEEM_U + rest) if s]
out += ["", "## Uit de nachtronde: de systemen", "", H.get(6, ""), ""]
out += [fb.strip(), ""]
txt = "\n".join(out)
txt = re.sub(r"\n{3,}", "\n\n", txt)
open(os.path.join(boom, "SPIEKBRIEF.md"), "w", encoding="utf-8", newline="\n").write(txt)
print("SPIEKBRIEF.md:", len(txt), "chars,", txt.count("\n"), "lines; extra SPIEK_U files:", rest)
