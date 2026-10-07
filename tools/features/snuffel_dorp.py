"""
Het Snuffeleiland, slice snuffel-dorp: the ISLAND and the FIRST SERIES (DESIGN_VERHALENPAD C). Java: feature/snuffeldorp.

  - the island itself (snuffel_dorp_bouw.py): the tiles guhs:snuffeldorp/eiland_<i>_<j>, data/guhs/snuffel/eiland.json
    (written AGAIN, over the kern's test island: this module stands after 'snuffel' in FEATURES) and this slice's own spots
    data/guhs/snuffeldorp/dorp.json (scene anchors, the line of the roadblock, how many pages every conversation has);
  - the game tests' floor snuffeldorp_test_vloer;
  - the texts (snuffel_dorp_tekst.py): what the residents say, the four scenes, screen lines, scents, good deeds, the exam.
    The steps of the questline themselves are tuples of snuffel.py (STAPPEN / KORT / EXTRA: the kern's rule);
  - blocks a dog may use on the island (tag guhs:snuffel_bruikbaar): nothing new was needed (doors and gates are in);
  - hidden advancements and the FTB quests of the section "Snuffeldorp" (FTB_CHAPTER / FTB_SECTION: move them by changing
    these two lines; the nine quests of the story's steps are the kern's, snuffel.py).

The wiki entries are in snuffel_dorp_wiki.py.
"""
import json
import os
import re

from . import bbq2
from . import snuffel
from . import snuffel_dorp_bouw as bouw
from . import snuffel_dorp_tekst as tekst

NAME = "snuffel_dorp"
FTB_CHAPTER = "guhs_verhalen"
FTB_SECTION = "Snuffeldorp"
FTB_PORTRAIT = "geo:snuffel_trainer:snuffel_trainer"

KLUSSEN = [  # (resident key, good deed id, FTB title, FTB text, icon)
    ("bakker", "bakker_deegroller", "Bolletjes, bolletjes, bolletjes",
     "&6Bakker Kruimelsnuit&r is zijn deegroller kwijt en bakt al dagen alleen maar bolletjes. Vraag hem ernaar, snuffel de deegroller op "
     "(oranje: iets lekkers) en breng hem terug.", "minecraft:bread"),
    ("visser", "visser_dobber", "Beet of geen beet?",
     "&6Visser Natneus&r mist zijn lievelingsdobber. Die dreef weg met het tij. Snuffel langs de kust ten zuiden van de haven (groen: hij "
     "ruikt naar vis).", "minecraft:fishing_rod"),
    ("juf", "juf_schoolbel", "Eeuwige pauze",
     "Zonder schoolbel is de pauze nooit voorbij. &6Juf Blaffetje&r vindt dat minder leuk dan de pups. Zoek de bel achter in de wei (blauw: "
     "een ding).", "minecraft:bell"),
    ("oma", "oma_bolwol", "Een trui met één mouw",
     "De lila bol wol van &6Oma Wolletje&r is weggerold, het strandpoortje door. Snuffel in het zand aan de westkant van het strand "
     "(groen: wol ruikt naar schaap).", "minecraft:purple_wool"),
    ("tuinder", "tuinder_gietertje", "Dorstige knollen",
     "&6Tuinder Knolletje&r geeft zijn knollen water met zijn hoed, en die lekt. Zijn gietertje ligt ergens aan de oostkant van de wei "
     "(blauw: een ding).", "minecraft:bucket"),
    ("pup", "pup_stuiterbal", "Kef! Bal! Kef!",
     "De stuiterbal van &6Kleine Kwijlebal&r stuiterde het strandpoortje uit. Hij zit vol kwijl, dus je ruikt hem zo (blauw: een ding).",
     "minecraft:slime_ball"),
]
VERBORGEN = ["snuffel_dorp_maatje", "snuffel_dorp_boom", "snuffel_dorp_alle", "snuffel_dorp_versperring"] + [f"snuffel_dorp_daad_{k[0]}" for k in KLUSSEN]


# =====================================================================================================================
# the island
# =====================================================================================================================
def eiland(h):
    b, data, dorp = bouw.eiland(h)
    bouw.tegels(b, data)
    dorp["gesprekken"] = {id: len(paginas) for id, paginas in tekst.GESPREKKEN.items()}
    h.w(f"{h.D}/snuffel/eiland.json", data)
    h.w(f"{h.D}/snuffeldorp/dorp.json", dorp)
    bouw.test_vloer(h).save("snuffeldorp_test_vloer")
    return b, data, dorp


def advancements(h):
    for name in VERBORGEN:
        bbq2.verborgen(h, name)


# =====================================================================================================================
# FTB quests
# =====================================================================================================================
def ftb(fq):
    klein = (("guhs:kaas_knabbels", 4),)
    fq.q("snuffel_dorp_neus", "Neus omlaag, staart omhoog",
         "Je eerste geur! Houd de &fsnuffeltoets&r ingedrukt en loop naar waar de &fgeurmeter&r het hardst uitslaat. Oranje is iets lekkers, "
         "blauw een ding, groen een dier en paars iets vreemds. Alles wat je neus leert, staat in je &fsnuffelboekje&r.",
         "minecraft:bone", [fq.adv("snuffel_eerste_geur")], rewards=klein, deps=["snuffel_snuffeleiland_4"])
    fq.q("snuffel_dorp_maatje", "Alleen jij ziet het",
         "Het ondeugende &abosgeestje&r heeft de halve inboedel van Snuffeldorp verstopt. Nu jij het kunt zien, wil het alles goedmaken: het wijst "
         "de kant op als je iets ruikt, en van elke goede daad groeit zijn boompje.",
         "minecraft:moss_block", [fq.adv("snuffel_dorp_maatje")], rewards=klein, deps=["snuffel_snuffeleiland_6"])
    vorige = "snuffel_dorp_maatje"
    for bewoner, daad, titel, uitleg, icon in KLUSSEN:
        fq.q(f"snuffel_dorp_{bewoner}", titel, uitleg, icon, [fq.adv(f"snuffel_dorp_daad_{bewoner}")], rewards=klein, deps=[vorige])
    fq.q("snuffel_dorp_boom", "Een jong boompje",
         "Vier goede daden: het boompje van je maatje is van &akiem&r via &ascheutje&r en &astruikje&r een &ajong boompje&r geworden. Bij elke stap "
         "zie je het groeien, hoog boven het dorp. Meester Truffelneus vindt je nu klaar voor het examen.",
         "minecraft:oak_sapling", [fq.adv("snuffel_dorp_boom")], rewards=(("guhs:kaas_knabbels", 8),), deps=["snuffel_dorp_maatje"])
    fq.q("snuffel_dorp_alle", "Het hele dorp geholpen",
         "Alle zes de dorpelingen hebben hun spullen terug. Het is weer rustig in Snuffeldorp. Nou ja: de schoolbel luidt, de pup stuitert en de "
         "bakker zingt. Njeg!",
         "minecraft:cake", [fq.adv("snuffel_dorp_alle")], rewards=(("guhs:kaas_knabbels", 12),), deps=["snuffel_dorp_boom"], shape="gear", xp=50)
    fq.q("snuffel_dorp_diploma", "Snuffelpup met diploma",
         "Geslaagd voor het snuffelexamen van Meester Truffelneus: vier verstopte geuren, van elke kleur één. Je bent officieel "
         "&6Snuffelpup&r, rang 1 (de laagste) van 5 (de hoogste). De andere rangen komen in een later verhaal.",
         "minecraft:writable_book", [fq.adv("snuffel_diploma")], rewards=(("guhs:kaas_knabbels", 8),), deps=["snuffel_snuffeleiland_8"])
    fq.q("snuffel_dorp_versperring", "Hier mag je pas door als Snuffelneus",
         "Achter de wei loopt de weg verder, het eiland op: een bos, een heuvel met staande stenen, een vuurtoren. Maar er staat een vriendelijke "
         "&cwegversperring&r. Een Snuffelpup komt er niet langs, ook niet zwemmend. Wordt vervolgd!",
         "minecraft:red_wool", [fq.adv("snuffel_dorp_versperring")], rewards=klein, deps=["snuffel_snuffeleiland_5"])
    fq.q("snuffel_dorp_guhstation", "Het Guhstation",
         "Je maatje had nog iets in zijn verstopplek: een &8Guhstation&r. Zet het thuis neer, klik erop en druk op start: je staat weer op het "
         "eiland, op de plek waar je was. Kwijt? &6Kapitein Zoutsnoet&r in de haven heeft er nog een paar.",
         "guhs:guhstation", [fq.adv("snuffel_guhstation")], rewards=klein, deps=["snuffel_snuffeleiland_9"])


# =====================================================================================================================
# self-check
# =====================================================================================================================
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "snuffeldorp")
KERN = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "snuffel")


def java_bron():
    out = ""
    for f in sorted(os.listdir(JAVA)):
        if f.endswith(".java") and not f.endswith("GameTests.java"):
            out += open(os.path.join(JAVA, f), encoding="utf-8").read()
    return out


def selfcheck(h, b, data, dorp):
    problems = []
    src = java_bron()
    # every conversation the Java names exists, every conversation of the texts is used; the families of the six lost things are complete
    gebruikt = set(re.findall(r'"((?:redder|dokter|trainer|kapitein)\.[a-z0-9_]+)"', src))
    gebruikt.discard("trainer.les")              # (the stem of "trainer.les" + n: the six lesson texts are named below)
    gebruikt |= {f"trainer.les{i}" for i in (1, 2, 3)} | {f"trainer.les{i}_hint" for i in (1, 2, 3)}
    for bewoner, daad, *_ in KLUSSEN:
        gebruikt |= {f"{bewoner}.{wat}" for wat in ("hallo", "vraag", "hint", "terug", "dank")}
    for id in sorted(gebruikt - set(tekst.GESPREKKEN)):
        problems.append(f"conversation {id} has no text")
    for id in sorted(set(tekst.GESPREKKEN) - gebruikt):
        problems.append(f"conversation {id} is never said")
    # lang keys the Java names in full
    for key in sorted(set(re.findall(r'"((?:gui|quest)\.guhs\.snuffeldorp\.[\w.]*\w)"', src))):
        if key not in h.NL and not key.endswith(".doel"):
            problems.append(f"missing lang {key}")
    for naam in dorp["plekken"]:
        if naam != "emmer" and f"gui.guhs.snuffeldorp.doel.{naam}" not in h.NL and naam in ("strandpoort", "dokter", "wei", "plein", "boom"):
            problems.append(f"missing lang gui.guhs.snuffeldorp.doel.{naam}")
    # the scenes: every line and name the script uses has a text, and the other way round
    for id, (titel, regels, namen) in tekst.SCENES.items():
        m = re.search(r'Cutscene\.maak\("' + id + r'"\)(.*?)\.registreer\(\)', src, re.S)
        if not m:
            problems.append(f"scene {id} is not in DorpScenes.java")
            continue
        in_script = set(re.findall(r'\.zeg\(\d+, "[a-z]*", "([a-z_]+)"', m.group(1)))
        for k in sorted(in_script - set(regels)):
            problems.append(f"scene {id}: line {k} has no text")
        for k in sorted(set(regels) - in_script):
            problems.append(f"scene {id}: text {k} is never said")
        for spreker in set(re.findall(r'\.zeg\(\d+, "([a-z]+)"', m.group(1))):
            if spreker not in namen:
                problems.append(f"scene {id}: speaker {spreker} has no name")
    # the ids the Java counts on are in the island's data
    bronnen = {x["id"] for x in data["geurbronnen"]}
    geuren = {g["id"] for g in data["geuren"]}
    sleutels = {x["sleutel"] for x in data["bewoners"]}
    for id in re.findall(r'"(dorp_[a-z_]+)"', src):
        if id not in bronnen:
            problems.append(f"Java names the scent source {id}, the island has none")
    for sleutel in re.findall(r'Bewoners\.zetRol\("([a-z]+)"', src) + ["havenkapitein"] + [k[0] for k in KLUSSEN]:
        if sleutel not in sleutels:
            problems.append(f"Java gives {sleutel} a role, the island has no such resident")
    for sleutel in sorted(sleutels):
        if sleutel not in src and sleutel not in [k[0] for k in KLUSSEN]:
            problems.append(f"resident {sleutel} has no role")
    for g in data["geuren"]:
        if f"gui.guhs.snuffel.geur.{g['id']}" not in h.NL:
            problems.append(f"scent {g['id']} has no name")
    for bewoner, daad, *_ in KLUSSEN:
        if f"gui.guhs.snuffel.daad.{daad}" not in h.NL:
            problems.append(f"good deed {daad} has no name")
        if f'new Klus("{bewoner}", "{daad}", ' not in src:
            problems.append(f"Dorp.KLUSSEN has no ({bewoner}, {daad})")
    if "bosgeestje" not in geuren or "papa_sjaal" not in geuren:
        problems.append("the companion's scent or father's scarf is missing from the island's scents")
    # the text variants of the steps: the kern's list = the texts
    kern = open(os.path.join(KERN, "Snuffel.java"), encoding="utf-8").read()
    m = re.search(r"SLEUTELS = \{(.*?)\};", kern, re.S)
    java_sleutels = set(re.findall(r'"([a-z0-9_]+)"', m.group(1))) if m else set()
    if java_sleutels != set(snuffel.EXTRA):
        problems.append(f"Snuffel.SLEUTELS {sorted(java_sleutels)} != snuffel.EXTRA {sorted(snuffel.EXTRA)}")
    for s in set(re.findall(r'"(\d_[a-z]+|thuis)"', src)):
        if s not in snuffel.EXTRA:
            problems.append(f"Dorp.java returns the text variant {s}, snuffel.EXTRA has none")
    # signs: no line wider than a sign shows (90 px; vanilla's glyph widths, about)
    for regels in b.borden:
        for r in regels:
            if breedte(r) > 90:
                problems.append(f"sign line too wide ({breedte(r)} px): {r}")
    for p in ([f"{h.D}/snuffeldorp/dorp.json", f"{h.D}/structure/snuffeldorp_test_vloer.nbt"]
              + [f"{h.D}/structure/{st['template'].split(':')[1]}.nbt" for st in data["stukken"]]
              + [f"{h.D}/advancement/quest/{n}.json" for n in VERBORGEN]):
        if not os.path.exists(p):
            problems.append(f"missing file {p}")
    if json.load(open(f"{h.D}/snuffel/eiland.json", encoding="utf-8"))["versie"] != bouw.VERSIE:
        problems.append("eiland.json is not this island")
    if problems:
        raise SystemExit("snuffel_dorp self-check failed:\n  " + "\n  ".join(problems))


SMAL = {"i": 2, "l": 3, "t": 4, "I": 4, "f": 5, "k": 5, " ": 4, ".": 2, ",": 2, "!": 2, ":": 2, "'": 2, "(": 4, ")": 4, "~": 7, "*": 4, "1": 6}


def breedte(regel):
    """About how many pixels vanilla's font needs for a sign line."""
    return sum(SMAL.get(c, 6) for c in regel)


def build(h):
    b, data, dorp = eiland(h)
    tekst.schrijf(h)
    advancements(h)
    selfcheck(h, b, data, dorp)
