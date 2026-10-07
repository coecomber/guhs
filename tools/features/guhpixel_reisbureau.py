"""
Guhpixel slice "reisbureau" (Java: feature/guhpixel/reisbureau; namespace reisbureau; English: tools/lang/en/c38_px_reisbureau.json).

Reisbureau "De Vadsvakantie" (DESIGN_PX section 5): send ONE of your guhs on a trip that takes real time (1, 2, 8 or 24
hours; every real day four trips out of sixteen destinations); it comes back with an ansichtkaart and a souvenir. Independent
of the guhpixel dimension. This module makes:
  - the structure "reisbureau" in the Guhmensie (guhpixel_reisbureau_bouw.py) with its random_spread set and its guaranteed
    copy (ring 4300-5600, sector 1 of 2), the Reisagent-guh (NPC kind reisbureau_agent);
  - the Reisbalie (craftable with the Reisstempel), the Reisstempel, seventeen ansichtkaarten (sixteen destinations + the
    proefreisje), 16 common + 16 rare souvenirs, the Gouden koffertje and the Koffertje (guhpixel_reisbureau_modellen.py and
    guhpixel_reisbureau_tex.py), recipes, loot tables, sounds, the hidden quest advancements;
  - every Dutch text (guhpixel_reisbureau_tekst.py) and the FTB quests of the section "Reisbureau De Vadsvakantie".
The first destination is spelled exactly "Vadsen bij huize Lingsesdijk 86", its picture "Huize Lingsesdijk 86".
"""
import os
import re

from features import guhpixel_lib as lib
from features import guhpixel_reisbureau_bouw as bouw
from features import guhpixel_reisbureau_modellen as modellen
from features import guhpixel_reisbureau_tekst as tekst

NAME = "reisbureau"
SALT = 20301101
GEGARANDEERD = (4300, 5600)
ADVANCEMENTS = ("kennis", "koffer", "proefreis", "eerste_reis", "zeldzaam", "koffertje", "album")

SOUNDS = {
    f"{NAME}.vertrek": [{"name": "minecraft:entity.villager.work_cartographer", "type": "event", "pitch": 1.3, "volume": 0.8},
                        {"name": "minecraft:item.bundle.insert", "type": "event", "pitch": 0.8}],
    f"{NAME}.terug": [{"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 1.2},
                      {"name": "minecraft:block.note_block.chime", "type": "event", "pitch": 1.5}],
    f"{NAME}.stempel": [{"name": "minecraft:block.wood.place", "type": "event", "pitch": 0.7, "volume": 0.9}],
    f"{NAME}.balie": [{"name": "minecraft:block.note_block.bell", "type": "event", "pitch": 1.6, "volume": 0.6}],
}

TEXTS = tekst.alle()


def _namen():
    namen = {}
    for (bid, _m, _k, _naam, _plek, _uitleg, _kaart, souvenir, zeldzaam) in tekst.BESTEMMINGEN:
        namen[f"{NAME}_souvenir_{bid}"] = souvenir
        namen[f"{NAME}_zeldzaam_{bid}"] = zeldzaam
    namen.update(tekst.EXTRA_BLOKKEN)
    return namen


def structuur(h):
    none = {"bounding_box": "piece", "spawns": []}
    h.TEMPLATE_SIZES[NAME] = bouw.W
    h.structure(NAME, h.GUHMENSION_LAND, spacing=80, separation=30, salt=SALT, start_y=-bouw.G, reach=40, centre=bouw.ANCHOR,
                spawn_overrides={"monster": none})
    bouw.build(h).save(NAME)
    lib.gegarandeerd(h, NAME, SALT, 1, GEGARANDEERD)
    lib.test_kamer(h, f"{NAME}_test_balie", (13, 8, 13))


def recepten(h):
    # the Reisbalie needs the Reisstempel of the Reisagent-guh (so: after the three steps); the stamp is used up, he has more
    h.shaped(f"{NAME}_balie", ["PSP", "HHH", "H H"], {"P": "minecraft:paper", "S": f"guhs:{NAME}_stempel", "H": "#minecraft:planks"},
             f"guhs:{NAME}_balie", 1)
    h.shaped(f"{NAME}_koffertje", ["LSL", "PPP"], {"L": "minecraft:leather", "S": "minecraft:stick", "P": "#minecraft:planks"},
             f"guhs:{NAME}_koffertje", 1)
    h.add_tag("minecraft/tags/block/mineable/axe", [f"guhs:{NAME}_balie"])


def build(h):
    lib.teksten(h, TEXTS)
    modellen.build(h, lib, _namen())
    structuur(h)
    recepten(h)
    lib.npc(h, f"{NAME}_agent", TEXTS[f"entity.guhs.guh_npc.{NAME}_agent"], hue=0.47, sat=0.7)
    lib.geluid(h, SOUNDS)
    for a in ADVANCEMENTS:
        lib.quest_adv(h, f"{NAME}_{a}")
    selfcheck(h)


def selfcheck(h):
    blokken = [f"{NAME}_balie"] + list(modellen.alle())
    items = blokken + [f"{NAME}_stempel"] + [f"{NAME}_kaart_{b}" for b in tekst.IDS + [tekst.PROEF[0]]]
    lib.controleer(h, "guhpixel_reisbureau", blokken=blokken, items=items, keys=TEXTS, templates=(NAME, f"{NAME}_test_balie"))
    problems = []
    for f in [f"recipe/{NAME}_balie.json", f"recipe/{NAME}_koffertje.json", f"worldgen/structure/{NAME}.json",
              f"worldgen/structure_set/{NAME}.json", f"worldgen/structure_set/{NAME}_gegarandeerd.json", f"{NAME}/vormen.json"] \
            + [f"advancement/quest/{NAME}_{a}.json" for a in ADVANCEMENTS] + [f"loot_table/blocks/{b}.json" for b in blokken]:
        if not os.path.exists(f"{h.D}/{f}"):
            problems.append(f"missing data/guhs/{f}")
    for t in [f"block/{b}" for b in blokken] + [f"block/{NAME}_{t}" for t in ("glas", "sneeuw", "draai", "logo", "beeld_lingsesdijk",
                                                                             "beeld_balkonie", "bordje_balkonie")] \
            + [f"item/{NAME}_stempel"] + [f"item/{NAME}_kaart_{b}" for b in tekst.IDS + [tekst.PROEF[0]]] + [f"entity/npc_{NAME}_agent"]:
        if not os.path.exists(os.path.join(h.TEX, *t.split("/")) + ".png"):
            problems.append(f"missing texture {t}")
    # the Java table: the same ids, minutes and chances, in the same order
    java = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "guhpixel", "reisbureau")
    src = open(os.path.join(java, "Bestemming.java"), encoding="utf-8").read()
    gevonden = re.findall(r'^\s+[A-Z_]+\("([a-z_]+)", (\d+), (\d+)\)', src, re.M)
    verwacht = [(b[0], str(b[1]), str(b[2])) for b in tekst.BESTEMMINGEN] + [(tekst.PROEF[0], str(tekst.PROEF[1]), "0")]
    if gevonden != verwacht:
        problems.append(f"Bestemming.java differs from guhpixel_reisbureau_tekst.py:\n    {gevonden}\n    {verwacht}")
    if len(tekst.BESTEMMINGEN) != 16 or [b[1] for b in tekst.BESTEMMINGEN] != [60] * 4 + [120] * 4 + [480] * 4 + [1440] * 4:
        problems.append("sixteen destinations, four per duration")
    if TEXTS[f"gui.guhs.{NAME}.bestemming.lingsesdijk"] != "Vadsen bij huize Lingsesdijk 86" \
            or "\"Huize Lingsesdijk 86\"" not in TEXTS[f"block.guhs.{NAME}_souvenir_lingsesdijk"]:
        problems.append("the first destination is 'Vadsen bij huize Lingsesdijk 86', its picture 'Huize Lingsesdijk 86'")
    src = open(os.path.join(java, "Reisagent.java"), encoding="utf-8").read()
    if "KNABBELS = 4" not in src:
        problems.append("Reisagent.KNABBELS is not 4 (the FTB and wiki texts say 4 kaasknabbels, 1 wol, 1 papier)")
    if problems:
        raise SystemExit("guhpixel_reisbureau self-check failed:\n  " + "\n  ".join(problems))


def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    q(f"{NAME}_vinden", "Reisbureau De Vadsvakantie", "Ergens ver weg in de &dGuhmensie&r staat een wit-roze huisje met een "
      "reuzenkoffer op het dak: &dReisbureau \"De Vadsvakantie\"&r. Het &6Superkompas&r wijst de weg (tabblad Knus). Daar kun je "
      "je guh op vakantie sturen!", f"guhs:{NAME}_koffertje", [fq.structure(NAME)], rewards=(("guhs:kaas_knabbels", 12),), shape="circle", xp=100)
    q(f"{NAME}_kennis", "De Reisagent-guh", "Praat met de &dReisagent-guh&r achter de balie. Hij stuurt guhs op reis: ze komen terug "
      "met een ansichtkaart, een souvenir en een zonnebril op.", f"guhs:{NAME}_kaart_om_de_hoek", [adv(f"{NAME}_kennis")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=[f"{NAME}_vinden"], xp=50)
    q(f"{NAME}_koffer", "Koffertje inpakken", "Een guh gaat niet op reis zonder koffertje. Breng de Reisagent-guh &64 kaasknabbels&r "
      "voor onderweg, &61 stuk wol&r als kussentje en &61 papiertje&r voor de ansichtkaart.", "minecraft:paper", [adv(f"{NAME}_koffer")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=[f"{NAME}_kennis"], xp=50)
    q(f"{NAME}_proefreis", "Proefreisje om de hoek", "Zet één van je guhs bij de balie en stuur hem op het &dproefreisje om de "
      "hoek&r. Dat duurt vijf echte minuten. Haal hem daarna op bij de balie: je krijgt de &6Reisstempel&r!",
      f"guhs:{NAME}_stempel", [adv(f"{NAME}_proefreis")], rewards=(("guhs:kaas_knabbels", 12),), deps=[f"{NAME}_koffer"], shape="gear", xp=100)
    q(f"{NAME}_balie", "Een Reisbalie voor thuis", "Maak met de Reisstempel, papier en planken je eigen &dReisbalie&r. Die werkt "
      "precies als de balie in het Reisbureau: elke echte dag vier nieuwe reizen van 1, 2, 8 of 24 uur. Er mag één guh tegelijk "
      "weg, en hij kan nooit kwijtraken.", f"guhs:{NAME}_balie", [item(f"guhs:{NAME}_balie")], rewards=(("guhs:kaas_knabbels", 8),),
      deps=[f"{NAME}_proefreis"], xp=50)
    q(f"{NAME}_eerste_reis", "Goede reis, njeg!", "Stuur een guh op een echte reis en haal hem op als hij terug is. Hij brengt een "
      "&6ansichtkaart&r mee (rechtsklik om te lezen) en een &6souvenir&r voor in huis.", f"guhs:{NAME}_souvenir_kaasmarkt",
      [adv(f"{NAME}_eerste_reis")], rewards=(("guhs:kaas_knabbels", 12),), deps=[f"{NAME}_balie"], xp=100)
    q(f"{NAME}_zeldzaam", "Met een beetje geluk", "Elke reis heeft ook een &dzeldzaam souvenir&r. Hoe langer de reis, hoe groter de "
      "kans: van 1 op 20 bij een reis van 1 uur tot bijna 1 op 3 bij 24 uur. Vind er één!",
      f"guhs:{NAME}_zeldzaam_kaasmarkt", [adv(f"{NAME}_zeldzaam")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),),
      deps=[f"{NAME}_eerste_reis"], shape="gear", xp=150)
    q(f"{NAME}_koffertje", "Het Gouden koffertje", "Een souvenir dat je al hebt wordt een &6stempel&r op je reispas. Bij tien "
      "stempels krijg je van de Reisagent-guh een &6Gouden koffertje&r.", f"guhs:{NAME}_gouden_koffertje",
      [item(f"guhs:{NAME}_gouden_koffertje")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), deps=[f"{NAME}_eerste_reis"], xp=150)
    q(f"{NAME}_album", "Het hele album", "Verzamel het gewone souvenir van &6alle zestien bestemmingen&r, van Vadsen bij huize "
      "Lingsesdijk 86 tot de Thuisblijfvakantie \"Balkonië\". Elke echte dag zijn er vier andere reizen, dus dit duurt even. Je album "
      "staat in de Guhdex.", f"guhs:{NAME}_souvenir_lingsesdijk", [adv(f"{NAME}_album")], rewards=(("guhs:gefrituurde_kaasknabbels", 6),),
      deps=[f"{NAME}_eerste_reis"], shape="hexagon", xp=300)
