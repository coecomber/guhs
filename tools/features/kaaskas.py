"""
1.8.0 (vadskracht, De Fabriekshal): de Kaaskas, het pluisbolletje and de Guh-sorteerband. Java: feature/kaaskas.

  build(h)    the machine guhs:kaaskas (a glassy greenhouse kern with a guh face, facing + snoet; vadskracht readout), the
              crop guhs:pluisbolletje_plant (guhs:pluisbolletje is the cave plant of onderwol; 4 stages, tinted from vanilla wheat; the ripe one a white-pink puff) with its seed
              guhs:pluisbolletje_zaad, the (TEMPORARY, see KaaskasFeature) item guhs:pluis, the belt guhs:sorteerband (a low
              slab with arrows, facing), recipes, loot, tags, the two chores' texts (Kassier, Sorteerder) and the blokuitleg
  ftb(fq)     a section of its own in the chapter Guh-technologie (guhs_techniek)

Standalone (from the project root): PYTHONPATH=tools python tools/features/kaaskas.py
"""
import json
import os
import re
import sys

import numpy as np
from PIL import Image

from features import vadskracht as vk
from features import zingendeweide as zw

FTB_CHAPTER = "guhs_techniek"
FTB_SECTIES = [("kaaskas", "De Kaaskas & de sorteerband", "item:guhs:kaaskas", None)]

GLAS = (176, 222, 186)
ROZE = (240, 150, 190)
PLUIS = (250, 236, 244)


def getal(naam):
    """A number of feature/kaaskas/KaaskasGetallen.java (texts never disagree with the code)."""
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "kaaskas", "KaaskasGetallen.java"), encoding="utf-8").read()
    m = re.search(r"\b" + naam + r"\s*=\s*([0-9][0-9_]*)", src)
    if not m:
        raise SystemExit(f"kaaskas: KaaskasGetallen heeft geen {naam}")
    return int(m.group(1).replace("_", ""))


P = "gui.guhs.huisje.overzicht.klus."


def teksten():
    vraag, straal = getal("VRAAG"), getal("STRAAL")
    kas = 2 * straal + 1
    return {  # key -> (Dutch, English)
        "block.guhs.kaaskas": ("Kaaskas", "Cheese Greenhouse"),
        "block.guhs.kaaskas.lore": (f"De kern van een glazen kas. Met vadskracht ({vraag}) en kaassaus groeien de gewassen om hem heen "
                                    f"({kas} bij {kas}) veel sneller. Vahoeg veel knabbels!",
                                    f"The heart of a glass greenhouse. With vadskracht ({vraag}) and kaassaus the crops around it "
                                    f"({kas} by {kas}) grow much faster. Vahoeg many nibbles!"),
        "item.guhs.kaaskas.lore": (f"Zet hem tussen je gewassen en geef hem vadskracht ({vraag}) en een emmer kaassaus.",
                                   f"Put it among your crops and give it vadskracht ({vraag}) and a bucket of kaassaus."),
        "gui.guhs.kaaskas.wacht_saus": ("Wacht op kaassaus (emmer of Sausslang)", "Waiting for kaassaus (bucket or Sauce Hose)"),
        "block.guhs.pluisbolletje_plant": ("Pluisbolletjesplant", "Fluffball Plant"),
        "item.guhs.pluisbolletje_zaad": ("Pluisbolletjeszaad", "Fluffball Seeds"),
        "item.guhs.pluis": ("Pluis", "Fluff"),
        "block.guhs.sorteerband": ("Guh-sorteerband", "Guh Sorting Belt"),
        "block.guhs.sorteerband.lore": ("Wat erop ligt rijdt mee naar het eind. Daar sorteert een guh met het klusje Sorteerder het "
                                        "in de goede kist. Njeg, opgeruimd staat netjes!",
                                        "What lies on it rides along to the end. There a guh with the chore Sorter puts it in the "
                                        "right chest. Njeg, tidy is nice!"),
        "gui.guhs.klus.kassier": ("Kassier: de kas plukken", "Greenhouse keeper: picking the greenhouse"),
        "gui.guhs.klus.kassier.tip": ("Loopt de Kaaskas in, plukt alles wat rijp is in één keer en plant het meteen opnieuw. De oogst "
                                      "gaat naar de kist of de Bank Guh. Alleen in een Kaaskas van jou. (Guhs)",
                                      "Walks into the Cheese Greenhouse, picks everything that is ripe in one go and plants it again "
                                      "right away. The harvest goes to the chest or the Bank Guh. Only in a greenhouse of yours. (Guhs)"),
        P + "kassier.rijp": ("Rijpe gewassen in je Kaaskassen: %s", "Ripe crops in your greenhouses: %s"),
        P + "kassier.groeit": ("Nog niets rijp. Gewassen die in je Kaaskassen groeien: %s", "Nothing ripe yet. Crops growing in your greenhouses: %s"),
        P + "kassier.leeg": ("Kaaskassen zonder gewassen: %s. Plant er iets in!", "Greenhouses without crops: %s. Plant something!"),
        P + "kassier.geen": ("Geen Kaaskas van jou", "No greenhouse of yours"),
        P + "kassier.nodig": ("een Kaaskas die jij zelf hebt neergezet in de klus-area, met gewassen eromheen. Ze plukken alles wat "
                              "rijp is en planten het opnieuw.",
                              "a Cheese Greenhouse you placed yourself in the chore area, with crops around it. They pick everything "
                              "that is ripe and plant it again."),
        "gui.guhs.dagboek.eerste.klusjes_kassier": ("Kassiertje guh", "Little greenhouse guh"),
        "gui.guhs.dagboek.eerste.klusjes_kassier.tekst": ("Ik heb de hele kas leeggeplukt! Het rook naar kaas en plantjes. Vahoeg.",
                                                          "I picked the whole greenhouse empty! It smelled of cheese and little plants. Vahoeg."),
        "gui.guhs.wistjedat.klusjes.kassier": ("Wist je dat er bij %s een kas vol kaasknabbels staat? Ik pluk hem leeg. Njeg!",
                                               "Did you know there is a greenhouse full of nibbles at %s? I pick it empty. Njeg!"),
        "gui.guhs.klus.sorteerder": ("Sorteerder: de band sorteren", "Sorter: sorting the belt"),
        "gui.guhs.klus.sorteerder.tip": ("Wacht aan het eind van een Guh-sorteerband, pakt op wat er aankomt en stopt het in de kist "
                                         "waar al zoiets in ligt. Leg zelf één ding in een kist, dan weet hij waar het hoort. (Guhs)",
                                         "Waits at the end of a Guh Sorting Belt, picks up what arrives and puts it in the chest that "
                                         "already holds that kind. Put one thing in a chest yourself and it knows where it belongs. (Guhs)"),
        P + "sorteerder.wacht": ("Dingen die aan het eind van een sorteerband wachten: %s", "Things waiting at the end of a sorting belt: %s"),
        P + "sorteerder.leeg": ("Sorteerbanden: %s. Er ligt nu niets te wachten", "Sorting belts: %s. Nothing is waiting right now"),
        P + "sorteerder.geen": ("Geen sorteerband in de klus-area", "No sorting belt in the chore area"),
        P + "sorteerder.nodig": ("een Guh-sorteerband in de klus-area, en kisten naast het huisje waar al één van elke soort in ligt. "
                                 "Wat geen eigen kist heeft gaat naar de Bank Guh of een andere kist.",
                                 "a Guh Sorting Belt in the chore area, and chests next to the huisje that already hold one of each kind. "
                                 "What has no chest of its own goes to the Bank Guh or another chest."),
        "gui.guhs.dagboek.eerste.klusjes_sorteerder": ("Sorteerguhtje", "Little sorting guh"),
        "gui.guhs.dagboek.eerste.klusjes_sorteerder.tekst": ("Alles lag in de goede kist. Ik ben vahoeg netjes vandaag.",
                                                             "Everything went into the right chest. I am vahoeg tidy today."),
        "gui.guhs.wistjedat.klusjes.sorteerder": ("Wist je dat bij %s een band rijdt? Ik sorteer alles in de goede kist!",
                                                  "Did you know a belt runs at %s? I sort everything into the right chest!"),
    }


# =====================================================================================================================
# pictures
# =====================================================================================================================
def _kas_voor(img, staat):
    """Little sprouts and a glass pane line in the front, around the face."""
    a = np.asarray(img).copy()
    for x in (1, 14):
        a[2:14, x, :3] = vk._tint(GLAS, 0.8)
    for x, y in ((3, 12), (5, 11), (10, 12), (12, 11)):
        a[y, x, :3] = (90, 170, 80)
        a[y - 1, x, :3] = (250, 214, 92)
    return Image.fromarray(a)


def _pluis_item():
    a = np.zeros((16, 16, 4), np.uint8)
    rng = np.random.default_rng(7)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 8) ** 2) ** 0.5
            if d < 5.6 + rng.normal(0, 0.5):
                k = np.clip(np.array(PLUIS) + rng.normal(0, 6, 3) - (0 if d < 4 else 18), 0, 255)
                a[y, x, :3] = k
                a[y, x, 3] = 255
    a[5, 6, :3] = (255, 255, 255)
    a[9, 10, :3] = ROZE
    return Image.fromarray(a)


def _band():
    a = vk._ruis((236, 170, 200), 5, 31)
    a[:, 0:2, :3] = (170, 110, 140)
    a[:, 14:16, :3] = (170, 110, 140)
    for y0 in (2, 9):  # two arrows pointing north (up in the texture)
        for i in range(4):
            a[y0 + i, 7 - i:9 + i, :3] = (255, 240, 248)
    return Image.fromarray(a)


def build(h):
    A, D, w = h.A, h.D, h.w
    # --- the Kaaskas: a vadskracht machine with a guh face ---------------------------------------------------------------
    vk.machine(h, "kaaskas", GLAS, ROZE, voor=lambda img, staat: img.paste(_kas_voor(img, staat)))
    # --- the pluisbolletje: four stages from vanilla wheat 1/3/5/7, white-pink, the ripe one a puff -----------------------
    for stage, bron in enumerate((1, 3, 5, 7)):
        img = zw.tint(zw.vanilla_png(f"block/wheat_stage{bron}"), (236, 214, 226), 0.2)
        if stage == 3:
            a = np.asarray(img).copy()
            for x, y in ((4, 3), (5, 3), (4, 4), (11, 2), (12, 2), (11, 3), (8, 6), (7, 6)):
                a[y, x] = (*PLUIS, 255)
            img = Image.fromarray(a)
        h.save(img, "block", f"pluisbolletje_plant_stage{stage}.png")
        w(f"{A}/models/block/pluisbolletje_plant_stage{stage}.json", {"parent": "minecraft:block/crop",
                                                               "textures": {"crop": f"guhs:block/pluisbolletje_plant_stage{stage}"}})
    w(f"{A}/blockstates/pluisbolletje_plant.json", {"variants": {f"age={a}": {"model": f"guhs:block/pluisbolletje_plant_stage{a}"} for a in range(4)}})
    h.save(zw.tint(zw.vanilla_png("item/wheat_seeds"), (240, 200, 220), 0.2), "item", "pluisbolletje_zaad.png")
    h.item_model("pluisbolletje_zaad")
    h.save(_pluis_item(), "item", "pluis.png")
    h.item_model("pluis")
    rijp = {"condition": "minecraft:block_state_property", "block": "guhs:pluisbolletje_plant", "properties": {"age": "3"}}
    w(f"{D}/loot_table/blocks/pluisbolletje_plant.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:pluis", "conditions": [rijp],
             "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]},
            {"type": "minecraft:item", "name": "guhs:pluisbolletje_zaad"}]}]},
        {"rolls": 1, "bonus_rolls": 0, "conditions": [rijp], "entries": [{"type": "minecraft:item", "name": "guhs:pluisbolletje_zaad",
            "functions": [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                           "formula": "minecraft:binomial_with_bonus_count", "parameters": {"extra": 2, "probability": 0.5}}]}]}],
        "functions": [{"function": "minecraft:explosion_decay"}]})
    # --- the sorteerband: a low slab with arrows, facing ---------------------------------------------------------------------
    h.save(_band(), "block", "sorteerband_boven.png")
    h.save(Image.fromarray(vk._ruis((200, 130, 160), 5, 32)), "block", "sorteerband_zij.png")
    f = lambda t: {"texture": t}
    w(f"{A}/models/block/sorteerband.json", {"parent": "minecraft:block/block",
        "textures": {"particle": "guhs:block/sorteerband_zij", "boven": "guhs:block/sorteerband_boven", "zij": "guhs:block/sorteerband_zij"},
        "elements": [{"from": [0, 0, 0], "to": [16, 4, 16], "faces": {
            "up": {"texture": "#boven"}, "down": {"texture": "#zij", "cullface": "down"},
            "north": {"texture": "#zij", "uv": [0, 12, 16, 16]}, "south": {"texture": "#zij", "uv": [0, 12, 16, 16]},
            "west": {"texture": "#zij", "uv": [0, 12, 16, 16]}, "east": {"texture": "#zij", "uv": [0, 12, 16, 16]}}}]})
    w(f"{A}/blockstates/sorteerband.json", {"variants": {f"facing={k}": {"model": "guhs:block/sorteerband", **({"y": r} if r else {})}
                                                          for k, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    w(f"{A}/models/item/sorteerband.json", {"parent": "guhs:block/sorteerband"})
    h.self_drop("sorteerband")
    # --- recipes -------------------------------------------------------------------------------------------------------
    h.shaped("kaaskas", ["GGG", "GKG", "IBI"], {"G": "minecraft:glass", "K": "guhs:kaas_knabbels", "I": "minecraft:iron_ingot",
                                                 "B": "minecraft:bucket"}, "guhs:kaaskas")
    h.shapeless("pluisbolletje_zaad", ["minecraft:wheat_seeds", "#minecraft:wool"], "guhs:pluisbolletje_zaad", 2)
    h.shapeless("wol_uit_pluis", ["guhs:pluis", "guhs:pluis", "guhs:pluis", "guhs:pluis"], "minecraft:white_wool")
    h.shaped("sorteerband", ["WWW", "IRI"], {"W": "#minecraft:wool", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"},
             "guhs:sorteerband", 6)
    # --- tags ----------------------------------------------------------------------------------------------------------
    for tag in ("crops", "bee_growables", "maintains_farmland"):
        h.add_tag(f"minecraft/tags/block/{tag}", ["guhs:pluisbolletje_plant"])
    h.add_tag("minecraft/tags/item/villager_plantable_seeds", ["guhs:pluisbolletje_zaad"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:sorteerband"])
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)
    selfcheck(h)


def blokuitleg():
    """(key, nl, en) entries for tools/wiki_site/blokuitleg.py."""
    vraag = getal("VRAAG")
    return [
        ("kaaskas", f"De kern van een glazen kas. Zet hem tussen je gewassen en geef hem vadskracht ({vraag}) en kaassaus (emmer of "
                    "Sausslang).\n\n- Elke ruim een seconde krijgt een groeiend gewas in de kas een duwtje, voor een beetje saus.\n"
                    "- Kaasknabbelplantjes, pluisbolletjes, roze graan, tarwe: elk gewas telt.\n"
                    "- Plukken doe je zelf, met een Oogster of met een guh met het klusje Kassier.",
         f"The heart of a glass greenhouse. Put it among your crops and give it vadskracht ({vraag}) and kaassaus (bucket or Sauce "
         "Hose).\n\n- Every second or so a growing crop in the greenhouse gets a push, for a bit of sauce.\n"
         "- Nibble plants, fluffballs, pink grain, wheat: every crop counts.\n"
         "- Picking is up to you, an Oogster or a guh with the chore Greenhouse keeper."),
        ("sorteerband", "Een lage band die je achter elkaar legt. Wat erop ligt rijdt mee in de richting van de pijltjes.\n\n"
                        "- Aan het eind (geen band meer ervoor) blijft het liggen.\n"
                        "- Een guh met het klusje Sorteerder pakt het op en stopt het in de kist waar al zoiets in ligt.",
         "A low belt you lay in a row. What lies on it rides along in the direction of the arrows.\n\n"
         "- At the end (no belt in front) it stays.\n"
         "- A guh with the chore Sorter picks it up and puts it in the chest that already holds that kind."),
    ]


# =====================================================================================================================
# FTB: a section in Guh-technologie
# =====================================================================================================================
def ftb(fq):
    q, y = fq.q, 0
    q("kaaskas_kas", "Een kas vol kaas", "Maak een &aKaaskas&r, zet hem tussen je gewassen en geef hem &bvadskracht&r en een emmer "
      "&6kaassaus&r. Kijk ze groeien!", "guhs:kaaskas", [fq.item("guhs:kaaskas")], rewards=(("guhs:kaas_knabbels", 16),), x=0, y=y, xp=150)
    q("kaaskas_pluis", "Pluizig!", "Kweek &fpluisbolletjes&r (zaadjes: tarwezaad + wol) en oogst je eerste &fpluis&r.",
      "guhs:pluis", [fq.item("guhs:pluis")], rewards=(("guhs:kaas_knabbels", 8),), x=1.5, y=y, xp=100)
    q("kaaskas_band", "Alles op z'n plek", "Leg een &dGuh-sorteerband&r en zet een guh met het klusje &dSorteerder&r in een huisje "
      "ernaast. Hij stopt alles in de goede kist!", "guhs:sorteerband", [fq.item("guhs:sorteerband")],
      rewards=(("guhs:kaas_knabbels", 8),), x=3, y=y, xp=100)


def selfcheck(h):
    A, D = h.A, h.D
    nodig = [f"{A}/blockstates/{n}.json" for n in ("kaaskas", "pluisbolletje_plant", "sorteerband")]
    nodig += [f"{A}/models/item/{n}.json" for n in ("kaaskas", "pluisbolletje_zaad", "pluis", "sorteerband")]
    nodig += [f"{D}/loot_table/blocks/{n}.json" for n in ("kaaskas", "pluisbolletje_plant", "sorteerband")]
    weg = [p for p in nodig if not os.path.exists(p)]
    if weg:
        raise SystemExit(f"kaaskas self-check failed: {weg}")


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    T = teksten()
    en_pad = os.path.join("tools", "lang", "en", "c217_kaaskas.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in T.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as fh:
            d = json.load(fh)
        d.update({k: v[i] for k, v in T.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as fh:
            json.dump(d, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
    a = os.path.join("src", "main", "resources", "assets", "guhs", "items")
    os.makedirs(a, exist_ok=True)
    for n in ("kaaskas", "pluisbolletje_zaad", "pluis", "sorteerband"):
        with open(os.path.join(a, n + ".json"), "w", encoding="utf-8", newline="\n") as fh:
            json.dump({"model": {"type": "minecraft:model", "model": f"guhs:item/{n}"}}, fh, indent=2)
            fh.write("\n")
    # the blokuitleg entries (appended once, in a marked block of their own)
    bp = os.path.join("tools", "wiki_site", "blokuitleg.py")
    src = open(bp, encoding="utf-8").read()
    if "# <kaaskas>" not in src:
        regels = ["", "# <kaaskas> 1.8.0 vadskracht: de Kaaskas en de Guh-sorteerband (tools/features/kaaskas.py)"]
        for key, nl, en in blokuitleg():
            regels.append(f"UITLEG[{key!r}] = dict(nl={nl!r}, en={en!r}, meer=[])")
        regels.append("# </kaaskas>")
        with open(bp, "a", encoding="utf-8", newline="\n") as fh:
            fh.write("\n".join(regels) + "\n")
    print(f"kaaskas: machine, crop (4 stages), pluis, sorteerband, {len(T)} texts")


if __name__ == "__main__":
    _direct()
