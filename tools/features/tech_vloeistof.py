"""
bbq2 (tech-vloeistof): saus door slangen. Java: feature/techsaus.

Six blocks, each a little guh with ears and the machine face of features/vadskracht.py:
  sauspomp         stands on a source block of kaassaus, kaasfrituursaus or water and lifts it on vadskracht; the source stays
  sausslang        the thin hose that joins pumps, vats and machines (it holds nothing itself)
  sausvat          16 buckets of one sauce (the four of the tag guhs:techniek_sauzen, milk too); tap it with a bucket; keeps
                   its sauce when broken (item component guhs:techsaus_inhoud, loot function copy_components)
  brouwautomaat    the Guhbrouwketel that brews by itself: a bucket of kaassaus + one ingredient + three bottles = three drankjes
  frituurautomaat  the frying pan that fries by itself, in kaasfrituursaus
  grillkoolpers    two blocks high: a bucket of frituursaus + a bucket of water = one block of grillkool
The Guhbrouwketel and the frying pan you work by hand are not touched.

This module makes: the looks (features/tech_vloeistof_modellen.py: textures, models, blockstates, item models), the loot tables,
the tags (guhs:vadskracht for the hover readout, mineable), the recipes (the Saus tier: each needs a grillspies or blubroom),
the texts, five hidden advancements quest/tech_vloeistof_* (for the FTB chapter of tech-quests) with their visible twins in
the tab Guh-technologie, and the test room techsaus_test_kamer. No FTB quests here: the chapter Guh-technologie is written by
features/tech_quests.py. The wiki texts are in features/tech_vloeistof_wiki.py.

Numbers in the texts come from the Java side (SausGetallen.java, VadsGetallen.java) through getal(): never type one.
"""
import os
import re

from features import bbq2, vadskracht
from features import tech_vloeistof_modellen as modellen

BLOKKEN = ("sauspomp", "sausslang", "sausvat", "brouwautomaat", "frituurautomaat", "grillkoolpers")
MACHINES = ("sauspomp", "brouwautomaat", "frituurautomaat", "grillkoolpers")     # they use vadskracht (the hover readout)
# what a player gets the first time (Java: SausBeloning): name -> (parent, icon, frame, title, text)
BELONINGEN = {
    "gepompt": (None, "guhs:sauspomp", "task", "Saus uit de grond",
                "Zet een Sauspomp op een bron en laat hem op vadskracht een hele emmer omhoog slurpen"),
    "getapt": ("tech_vloeistof_gepompt", "guhs:sausvat", "task", "Tapje, njeg?",
               "Tap met een emmer saus uit een Sausvat"),
    "gebrouwen": ("tech_vloeistof_getapt", "guhs:brouwautomaat", "task", "Roeren is voor vroeger",
                  "Laat een Brouwautomaat drie Guhdrankjes voor je brouwen"),
    "gefrituurd": ("tech_vloeistof_getapt", "guhs:frituurautomaat", "task", "Vanzelf vahoeg",
                   "Laat een Frituurautomaat kaasknabbels voor je frituren"),
    "geperst": ("tech_vloeistof_getapt", "guhs:grillkoolpers", "goal", "Onder druk wordt alles grillkool",
                "Pers frituursaus en water samen tot een blok grillkool in de Grillkoolpers"),
}

_GETALLEN = None


def getal(naam):
    """A number of feature/techsaus/SausGetallen.java (a name that points at VadsGetallen is looked up there)."""
    global _GETALLEN
    if _GETALLEN is None:
        src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "techsaus", "SausGetallen.java"), encoding="utf-8").read()
        _GETALLEN = {}
        for m in re.finditer(r"\b([A-Z][A-Z0-9_]*)\s*=\s*(VadsGetallen\.[A-Z0-9_]+|[0-9][0-9_]*)", src):
            waarde = m.group(2)
            _GETALLEN[m.group(1)] = vadskracht.getal(waarde.split(".")[1]) if waarde.startswith("Vads") else int(waarde.replace("_", ""))
    if naam not in _GETALLEN:
        raise SystemExit(f"tech_vloeistof: SausGetallen heeft geen {naam}")
    return _GETALLEN[naam]


def emmers(mb):
    """mB as buckets in Dutch: 16000 -> "16", 2500 -> "2,5"."""
    return str(mb // 1000) if mb % 1000 == 0 else f"{mb // 1000},{mb % 1000 // 100}"


# =====================================================================================================================
# texts
# =====================================================================================================================
K = "gui.guhs.techsaus."


def teksten():
    per_seconde = getal("POMP_PER_TIK") * 20
    snacks_per_emmer = 1000 // getal("FRITUUR_SAUS")
    return {
        "block.guhs.sauspomp": "Sauspomp",
        "block.guhs.sauspomp.lore": f"Zet hem bovenop een bron van saus of water: op {getal('POMP')} vadskracht slurpt hij een emmer per "
                                    f"{1000 // per_seconde} tellen omhoog, de Sausslang in. De bron raakt nooit op. Njeg!",
        "block.guhs.sausslang": "Sausslang",
        "block.guhs.sausslang.lore": "Een dun slangetje voor saus. Leg het van je Sauspomp naar een Sausvat of een machine: het sluit "
                                     f"vanzelf aan. Hooguit {getal('SLANG_MAX')} slangen achter elkaar, daarna heeft de saus geen zin meer.",
        "block.guhs.sausvat": "Sausvat",
        "block.guhs.sausvat.lore": f"Bewaart {emmers(getal('VAT'))} emmers van één saus: kaassaus, kaasfrituursaus, water of melk. Tap eruit met "
                                   "een emmer of giet er een in. Machines slurpen er door een Sausslang uit. Wat erin zit gaat mee als je het vat oppakt.",
        "block.guhs.brouwautomaat": "Brouwautomaat",
        "block.guhs.brouwautomaat.lore": f"De Guhbrouwketel die zelf roert, op {getal('BROUWAUTOMAAT')} vadskracht. Een emmer kaassaus, één "
                                         f"ingrediënt en {getal('BROUW_FLESJES')} glazen flesjes erin: er borrelen {getal('BROUW_FLESJES')} "
                                         "Guhdrankjes uit. Grillspiespoeder is niet nodig, vads!",
        "block.guhs.frituurautomaat": "Frituurautomaat",
        "block.guhs.frituurautomaat.lore": f"De frituurpan die zelf frituurt, in kaasfrituursaus en op {getal('FRITUURAUTOMAAT')} vadskracht. "
                                           f"Knabbels erin, gefrituurde knabbels eruit: een emmer saus is goed voor {snacks_per_emmer} stuks. Vahoeg!",
        "block.guhs.grillkoolpers": "Grillkoolpers",
        "block.guhs.grillkoolpers.lore": f"Twee blokken hoog. Perst op {getal('GRILLKOOLPERS')} vadskracht een emmer kaasfrituursaus en een emmer "
                                         "water samen tot een blok grillkool. Zonder verbrande pootjes.",
        # the four sauces, amounts
        K + "saus.kaassaus": "kaassaus",
        K + "saus.frituursaus": "kaasfrituursaus",
        K + "saus.water": "water",
        K + "saus.melk": "melk",
        K + "getal": "%s,%s",
        K + "tank": "%2$s van de %3$s emmers %1$s",
        K + "tank.leeg": "Leeg (er passen %s emmers saus in)",
        K + "tank.leeg_van": "Geen %s (er passen %s emmers in)",
        # what a machine waits for (the hover readout and a click with an empty hand)
        K + "wacht": "Wacht op %s",
        K + "wacht.vol": "iemand die hem leeghaalt: hij zit vol, njeg!",
        K + "wacht.bron": "een bron: zet de pomp bovenop saus of water",
        K + "wacht.andere_saus": "een lege tank: er zit nog %s in",
        K + "wacht.ingredient": "een ingrediënt",
        K + "wacht.saus": "%s",
        K + "wacht.flesjes": f"{getal('BROUW_FLESJES')} glazen flesjes",
        K + "wacht.snack": "kaasknabbels of een guhvis om te frituren",
        K + "slang_te_lang": "Deze slang is te lang, njeg: na %s slangen heeft de saus geen zin meer",
        K + "brouwt": "Er borrelt %s: %s%%",
        K + "perst": "Hij perst: %s%%",
        # clicks
        K + "vol": "Dat past er niet meer bij, njeg",
        K + "emmer_past_niet": "Die emmer past hier niet: verkeerde saus, te vol of te leeg",
        K + "vat.leeg": "Het Sausvat is leeg",
        K + "vat.te_weinig": "Er zit nog geen hele emmer %s in",
        K + "vat.geen_saus": "Dat is geen saus voor een Sausvat",
        K + "vat.andere_saus": "Er zit al %s in: één saus per vat, njeg",
        K + "vat.vol": "Het Sausvat zit vol %s",
        K + "vat.item": "Inhoud: %s emmers %s",
    }


def texts(h):
    for key, nl in teksten().items():
        h.lang(key, nl, nl)


# =====================================================================================================================
# blocks: looks, loot, tags, recipes
# =====================================================================================================================
def blocks(h):
    modellen.build(h)
    for naam in BLOKKEN:
        if naam != "sausvat":
            h.self_drop(naam)
    # a broken Sausvat keeps its sauce (like the Bank Guh keeps its stomach)
    h.w(f"{h.D}/loot_table/blocks/sausvat.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:sausvat", "functions": [
            {"function": "minecraft:copy_components", "source": "block_entity", "include": ["guhs:techsaus_inhoud"]}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    vadskracht.toon(h, *MACHINES)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{n}" for n in BLOKKEN if n != "sausvat"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:sausvat"])


def recipes(h):
    """The Saus tier (CONTRACT_130 7): every recipe needs a grillspies or blubroom; the machines grow out of the hand tools."""
    ijzer, slang, spies, draad = "minecraft:iron_ingot", "guhs:sausslang", "guhs:grillspies", "guhs:guh_wire"
    h.shaped("sausslang", ["KCK", "KGK", "KCK"], {"K": "minecraft:dried_kelp", "C": "minecraft:copper_ingot", "G": spies}, "guhs:sausslang", 8)
    h.shaped("sauspomp", ["IPI", "SBS", "IGI"], {"I": ijzer, "P": "minecraft:piston", "S": slang, "B": "minecraft:bucket", "G": spies},
             "guhs:sauspomp")
    h.shaped("sausvat", ["I I", "PBP", "IGI"], {"I": ijzer, "P": "#minecraft:planks", "B": "minecraft:barrel", "G": spies}, "guhs:sausvat")
    h.shaped("brouwautomaat", [" R ", "SKS", "IDI"], {"R": "guhs:blubroom", "S": slang, "K": "guhs:guhbrouwketel", "I": ijzer, "D": draad},
             "guhs:brouwautomaat")
    h.shaped("frituurautomaat", [" G ", "SFS", "IDI"], {"G": spies, "S": slang, "F": "guhs:frying_pan", "I": ijzer, "D": draad},
             "guhs:frituurautomaat")
    h.shaped("grillkoolpers", ["IPI", "SGS", "HDH"], {"I": ijzer, "P": "minecraft:piston", "S": slang, "G": spies,
                                                     "H": "guhs:houtskoolsteen", "D": draad}, "guhs:grillkoolpers")


def advancements(h):
    for naam, (ouder, icoon, frame, titel, tekst) in BELONINGEN.items():
        bbq2.verborgen(h, f"tech_vloeistof_{naam}")
        bbq2.zichtbaar(h, "techniek", f"tech_vloeistof_{naam}", ouder or "root", icoon, frame, titel, tekst)


def test_templates(h):
    """techsaus_test_kamer: 11 x 6 x 7 with a stone floor two blocks thick (the tests dig their sources into the top layer)."""
    s = h.Structure((11, 6, 7))
    s.fill(0, 0, 0, 10, 1, 6, "minecraft:stone")
    s.save("techsaus_test_kamer")


# =====================================================================================================================
# self-check
# =====================================================================================================================
def selfcheck(h):
    import json
    A, D = h.A, h.D
    problems = []
    for naam in BLOKKEN:
        for pad in (f"{A}/blockstates/{naam}.json", f"{A}/models/item/{naam}.json", f"{A}/models/block/{naam}.json",
                    f"{D}/loot_table/blocks/{naam}.json", f"{D}/recipe/{naam}.json"):
            if not os.path.exists(pad):
                problems.append(pad)
        # every model a blockstate names exists, and every texture a model names
        state = json.load(open(f"{A}/blockstates/{naam}.json", encoding="utf-8"))
        refs = [v["model"] for v in state.get("variants", {}).values()] + [p["apply"]["model"] for p in state.get("multipart", [])]
        for ref in sorted(set(refs)) + [f"guhs:block/{naam}"]:
            pad = f"{A}/models/block/{ref.split('/', 1)[1]}.json"
            if not os.path.exists(pad):
                problems.append(f"{naam}: model {ref}")
                continue
            m = json.load(open(pad, encoding="utf-8"))
            for tex in m.get("textures", {}).values():
                if tex.startswith("guhs:block/") and not os.path.exists(os.path.join(h.TEX, "block", tex.split("/", 1)[1] + ".png")):
                    problems.append(f"{ref}: texture {tex}")
    # the vat's block states are all there (4 facings x 5 sauces x 5 levels), the press's too
    if len(json.load(open(f"{A}/blockstates/sausvat.json", encoding="utf-8"))["variants"]) != 100:
        problems.append("sausvat: not 100 block states")
    if len(json.load(open(f"{A}/blockstates/grillkoolpers.json", encoding="utf-8"))["variants"]) != 24:
        problems.append("grillkoolpers: not 24 block states")
    problems += [k for k in teksten() if k not in h.NL]
    for naam in BELONINGEN:
        for pad in (f"{D}/advancement/quest/tech_vloeistof_{naam}.json", f"{D}/advancement/techniek/tech_vloeistof_{naam}.json"):
            if not os.path.exists(pad):
                problems.append(pad)
    tag = json.load(open(f"{D}/tags/block/vadskracht.json", encoding="utf-8"))["values"]
    problems += [f"tag guhs:vadskracht: {n}" for n in MACHINES if f"guhs:{n}" not in tag]
    if not os.path.exists(f"{D}/structure/techsaus_test_kamer.nbt"):
        problems.append("techsaus_test_kamer.nbt")
    # the texts promise what the code does
    if getal("VAT") % 1000 or getal("BROUW_SAUS") != 1000 or getal("PERS_SAUS") != 1000 or getal("PERS_WATER") != 1000:
        problems.append("SausGetallen: the texts say whole buckets (VAT, BROUW_SAUS, PERS_SAUS, PERS_WATER)")
    if problems:
        raise SystemExit("tech_vloeistof self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    blocks(h)
    recipes(h)
    texts(h)
    advancements(h)
    test_templates(h)
    selfcheck(h)
