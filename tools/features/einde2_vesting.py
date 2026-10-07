"""
einde2 (the Guheinde expansion), slice vesting: the modular Mika-vesting.
Java: nl.juiced.guhs.feature.einde2.vesting (E2VestingFeature). The pieces: einde2_vesting_bouw.py; whole vestingen put
together and walked through before the game sees them: einde2_vesting_proef.py; the wiki texts: einde2_vesting_wiki.py.

The structure guhs:mika_vesting keeps everything a world remembers it by: its id, its set (salt 20260926, 20 / 8), its
type and its ground check (features/guheinde_bouw.py writes all that, and its two old templates and their pool stay on
disk: a vesting that was generated before names them). This module runs after it and changes ONE thing in the structure:
the jigsaw inside it starts from the pool einde2_vesting/start (the kern, with the same anchor guhs:mika_vesting_midden)
and may grow DIEPTE pieces deep, MAX_AFSTAND blocks from its middle. So the same chunks start a vesting as before, in old
and new worlds; only what stands there in a NEW chunk is the modular one.

  - the templates and pools einde2_vesting/* and the sidecar data/guhs/einde2/plekken/mika_vesting.json (the markers of
    the build contract: kapitein, kok, keuken, cel_1 .. cel_8, schatkamer, vetbootje_opdracht);
  - three loot tables (the loot rooms come in three tiers): chests/einde2_vesting_gewoon, _goed, _schat;
  - the two hidden advancements of the cells, the texts, the Superkompas tooltip, three FTB quests (a section of its own).

build(h) makes everything (h = make_v2) and stops on any problem of a piece or of a proefvesting.
"""
import json
import os

import features.einde2_kit as kit
from features import einde2_vesting_bouw as vb
from features import einde2_vesting_proef as proef
from features.guheinde import enchanted, item

FTB_CHAPTER = "guhs_guheinde"
FTB_SECTION = "De Mika-vesting"
FTB_PORTRAIT = "geo:mika:mika"

STRUCTUUR = "mika_vesting"
OUDE_TEMPLATES = ["mika_vesting", "mika_vesting_schip"]

TEXTS = {
    "structure.guhs.mika_vesting.tooltip": "Een vesting van paarse Mika-steen op de buiteneilanden: torens, bruggen, een vetschip, "
                                           "de keuken van de kok en cellen vol magere guhs",
    "gui.guhs.einde2_vesting.bevrijd.eerste": "VAHOEG! Je hebt een magere guh uit een Mika-vesting bevrijd. In de andere cellen zitten er nog meer, njeg!",
    "gui.guhs.einde2_vesting.bevrijd.teller": "%s vestingguhs bevrijd. VAHOEG!",
    "gui.guhs.einde2_vesting.bevrijd.zes": "%s vestingguhs bevrijd! De Mika's kijken beteuterd naar hun lege cellen. NJEG...",
    "gui.guhs.einde2_vesting.vleugels.gepakt": "Je grist een paar Guhvleugels uit de kajuit. Mika heeft er nog een hele stapel liggen, njeg!",
    "gui.guhs.einde2_vesting.vleugels.al": "Uit deze kajuit had je al een paar. Niet hebberig worden, njeg.",
}
ADVANCEMENTS = ["einde2_vesting_bevrijd", "einde2_vesting_bevrijder"]


def worldgen(h):
    """The one change to the structure: its jigsaw starts from our pool and may grow."""
    def patch(s):
        j = s["jigsaw"]
        assert s["type"] == "guhs:guheinde_eiland" and j["start_jigsaw_name"] == vb.ANKER, "mika_vesting is not what features/guheinde_bouw.py wrote"
        j["start_pool"] = vb.nid("start")
        j["size"] = vb.DIEPTE
        j["max_distance_from_center"] = vb.MAX_AFSTAND
    h.patch_json(f"{h.D}/worldgen/structure/{STRUCTUUR}.json", patch)


def loot(h):
    """Three tiers, all modest: what a guard keeps, what a raid brings home, what Mika sits on."""
    def chest(name, pools):
        h.w(f"{h.D}/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": pools})

    def roll(lo, hi, entries):
        return {"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries}

    chest("einde2_vesting_gewoon", [roll(2, 4, [
        item("guhs:kaas_knabbels", 2, 6, 6), item("guhs:mika_vet", 1, 3, 5), item("iron_ingot", 1, 4, 4), item("bread", 1, 3, 3),
        item("guhs:guh_kristal", 1, 3, 3), item("gold_ingot", 1, 2, 2), item("guhs:gefrituurde_kaasknabbels", 1, 2, 2),
        item("guhs:kristalkaas_brokje", 1, 3, 2), item("guhs:mika_traan", 1, 1, 1)])])
    chest("einde2_vesting_goed", [roll(3, 5, [
        item("gold_ingot", 2, 5, 5), item("iron_ingot", 2, 6, 5), item("guhs:mika_vet", 2, 5, 5), item("emerald", 1, 4, 3),
        item("guhs:kristalkaas_brokje", 2, 5, 4), item("guhs:kristalkaas", 1, 2, 2), item("guhs:flitskruimel", 1, 2, 2),
        item("guhs:gefrituurde_kaasknabbels", 2, 4, 3), item("diamond", 1, 2, 2), item("guhs:vahoege_vads_ingot", 1, 1, 1),
        enchanted("iron_pickaxe", 2), enchanted("iron_boots", 1)])])
    chest("einde2_vesting_schat", [
        {"rolls": 1, "entries": [item("guhs:block_of_kaasknabbels", 1, 3)]},
        roll(4, 6, [item("diamond", 1, 3, 4), item("emerald", 2, 5, 4), item("guhs:kristalkaas", 1, 3, 4), item("guhs:vahoege_vads_ingot", 1, 2, 3),
                    item("guhs:gefrituurde_kaasknabbels", 3, 6, 4), item("guhs:doosschil", 1, 2, 2), item("guhs:flitskruimel", 1, 3, 2),
                    item("golden_apple", 1, 1, 2), item("guhs:knabbelkristal", 1, 1, 1), enchanted("diamond_sword", 1), enchanted("diamond_helmet", 1),
                    enchanted("diamond_pickaxe", 1)])])


def advancements(h):
    for name in ADVANCEMENTS:
        kit.verborgen(h, name)


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def testkamer(h):
    """The rooms of E2VestingGameTests: a lit floor with a wall to hang a frame on, and one with a cell guh in it the way
    the templates of the vesting hold them (the tag, the variant)."""
    s = h.Structure((7, 5, 7))
    s.fill(0, 0, 0, 6, 0, 6, vb.MSTEEN)
    s.fill(0, 1, 0, 6, 3, 0, vb.MSTEEN)
    s.set(3, 0, 3, vb.LAMP)
    s.save("einde2_vesting_test_kamer")
    b = vb.Stuk(h, "_test", (7, 5, 7))
    b.fill(0, 0, 0, 6, 0, 6, vb.MSTEEN)
    b.set(3, 0, 3, vb.LAMP)
    b.mager(3, 1, 3)
    b.s.save("einde2_vesting_test_cel")


def selfcheck(h, stukken):
    """Everything this module promises is on disk, and what it must leave alone is still there."""
    D = h.D
    missing = []
    for naam in stukken:
        if not os.path.exists(f"{D}/structure/{vb.NS}/{naam}.nbt"):
            missing.append(f"template {naam}")
    for pool in vb.POOLS:
        if not os.path.exists(f"{D}/worldgen/template_pool/{vb.NS}/{pool}.json"):
            missing.append(f"pool {pool}")
    for oud in OUDE_TEMPLATES:
        if not os.path.exists(f"{D}/structure/{oud}.nbt"):
            missing.append(f"the old template {oud} (a vesting generated before names it)")
    if not os.path.exists(f"{D}/worldgen/template_pool/mika_vesting/start.json"):
        missing.append("the old pool mika_vesting/start")
    s = json.load(open(f"{D}/worldgen/structure/{STRUCTUUR}.json", encoding="utf-8"))
    if (s["min_distance"], s["min_surface_y"], s["check_radius"]) != (1100, 50, 10) or s["jigsaw"]["start_pool"] != vb.nid("start"):
        missing.append("the structure's ground check or start pool")
    if s["jigsaw"]["max_distance_from_center"] > 96 or s["jigsaw"]["max_distance_from_center"] + 12 > 128:
        missing.append("max_distance_from_center at most 96")
    p = json.load(open(f"{D}/worldgen/structure_set/{STRUCTUUR}.json", encoding="utf-8"))["placement"]
    if (p["salt"], p["spacing"], p["separation"]) != (20260926, 20, 8):
        missing.append("the set of the vesting changed (never)")
    blad = json.load(open(f"{D}/einde2/plekken/{STRUCTUUR}.json", encoding="utf-8"))
    for id, stuk in vb.PLEKKEN.items():
        if blad["plekken"].get(id, {}).get("stuk") != f"{vb.NS}/{stuk}":
            missing.append(f"plek {id}")
    for tabel in ("einde2_vesting_gewoon", "einde2_vesting_goed", "einde2_vesting_schat", "mika_vesting", "vetschip"):
        if not os.path.exists(f"{D}/loot_table/chests/{tabel}.json"):
            missing.append(f"loot {tabel}")
    for key in TEXTS:
        if key not in h.NL:
            missing.append(f"lang {key}")
    for name in ADVANCEMENTS:
        if not os.path.exists(f"{D}/advancement/quest/{name}.json"):
            missing.append(f"advancement {name}")
    if missing:
        raise SystemExit(f"einde2_vesting: missing: {missing}")


def build(h):
    stukken, problemen = vb.maak(h)
    if not problemen:
        meer, regel = proef.controleer(h, stukken)
        problemen += meer
        print(f"einde2_vesting: {len(stukken)} stukken, {regel}")
    if problemen:
        raise SystemExit("einde2_vesting: " + "\n  ".join(problemen[:60]))
    vb.schrijf(h, stukken)
    kit.jigsaw_blad(h, STRUCTUUR, [(b, b.name) for b in stukken.values() if b.plekken])
    worldgen(h)
    loot(h)
    advancements(h)
    texts(h)
    testkamer(h)
    selfcheck(h, stukken)


# =====================================================================================================================
# FTB quests: a section of its own at the end of the Guheinde chapter (find / free; nothing is locked)
# =====================================================================================================================
def ftb(fq):
    q = fq.q
    q("einde2_vesting_vind", "Een vesting vol torens", "De &dMika-vestingen&r van de buiteneilanden zijn uitgebouwd: een burcht met een "
      "wachtlokaal, een keuken en een troonzaal, bruggen naar zijtorens die op een puntje zweven, en aan de steiger een &6vetschip&r. "
      "Zoek er een (het Superkompas wijst de weg) en loop naar binnen. Elke vesting is anders, njeg!",
      "guhs:mika_steen_pilaar", [fq.structure(STRUCTUUR)], rewards=(("guhs:kaas_knabbels", 12),), shape="octagon")
    q("einde2_vesting_bevrijd", "Logeren bij Mika", "Achter de burcht staat het &dcellenblok&r: \"de logeervleugel\", zeggen de Mika's. "
      "Er zitten magere guhs in. Geef er een een &6kaasknabbel&r door de deur: VAHOEG! Iedere speler kan iedere guh een keer bevrijden.",
      "guhs:kaas_knabbels", [fq.adv("einde2_vesting_bevrijd")], rewards=(("guhs:kaas_knabbels", 10),))
    q("einde2_vesting_bevrijder", "Zes lege cellen", "Bevrijd &6zes&r magere guhs uit de cellen van Mika-vestingen. In het cellenblok "
      "zitten er zes, en in een kerkertoren (\"de logeerkamer\") nog eens twee. De Mika's snappen er niks van: de sleutels waren toch opgegeten?",
      "guhs:gefrituurde_kaasknabbels", [fq.adv("einde2_vesting_bevrijder")], rewards=(("guhs:kaas_knabbels", 24),), shape="diamond")
