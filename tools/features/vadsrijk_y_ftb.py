"""
1.10 "Het Vadsrijk", round Y, slice Y-FTB: the compact FTB chapter "Het Vadsrijk" and its survival rewards.

The chapter (user decision 2026-11-10) has 14 quests, any character and any game mode counts:
  1      Word rekruut van De Knuffelorde       the Knuffelkamp training done (hq05_klodderreus)
  2-7    Zone N uitgespeeld                    the zone's last main quest handed in
  8      De Sterrendovende Toren verslagen     the first raid clear
  9-14   Alle geheimpjes van zone N            every secret, lost knuffel and puzzle of the zone's Verkenningsboek page
plus five small "Zone N is open!" quests (N = 2..6) that stay invisible until the server opens that zone
(/guhs vadsrijk zones open <n>): the quests of zone N (and the raid behind zone 6) hang behind them with
hide_until_deps_complete, so with openZones = 1 the book shows only the recruit, zone 1 and its secrets: no spoilers.

Every task is a hidden advancement guhs:quest/vr_ftb_<x> that the Java grants (feature/vadsrijk/ftb/VrFtb). Every reward is ONE
decoration block for your own world (no kaasknabbels, no xp, no resources): a little trophy per zone, a raid trophy, a recruit
pennant and a "Geheimpjesdoosje" per zone. They cannot be placed in the Vadsrijk, and FTB rewards cannot be claimed there
(feature/vadsrijk/ftb/FtbClaimRegel + mixin/FtbClaimMixin).

The models use vanilla block textures only (no new pictures). Ids start with "vadsrijk_trofee_" and NOT with "vr_": a "vr_"
item counts as a Vadsrijk item (kern, kleding, ...).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_y_ftb.py
"""
import json
import os

P = "vadsrijk_trofee_"
T = "vadsrijk.ftb."
K = "guh:normal"

ZONES = {1: ("De Knuffelzwammen", "The Snuggle Shrooms"), 2: ("De Splinterpieken", "The Splinter Peaks"),
         3: ("De Schemerwortel", "The Duskroot"), 4: ("De Lampionkust", "The Lantern Coast"), 5: ("Hoogvads", "Highchonk"),
         6: ("Het Zijdeland", "The Silklands")}
# the last main quest of each zone (the Java finds it from the quest data too, VrFtb.eindQuest; a gametest compares both)
EIND = {1: "hq12_schaduw", 2: "z2_hq12_hartsplinter", 3: "z3_hq15_hoogvads", 4: "z4_hq07_opstijgpier", 5: "z5_hq10_stormrand",
        6: "z6_hq10_moederoase"}
REKRUUT_QUEST = "hq05_klodderreus"

FACES = ("north", "south", "east", "west", "up", "down")


def doos(van, tot, tex, rot=None):
    el = {"from": list(van), "to": list(tot), "faces": {f: {"texture": "#" + tex} for f in FACES}}
    if rot:
        el["rotation"] = rot
    return el


def draai(as_, hoek, oorsprong):
    return {"angle": hoek, "axis": as_, "origin": list(oorsprong)}


# ---- the blocks: id -> dict(nl, en, lore_nl, lore_en, light, sound, render, tex {var: vanilla texture}, elements) -------------
def _rekruut():
    return dict(nl="Rekruutvaantje", en="Recruit Pennant",
                tex={"hout": "oak_planks", "paal": "stripped_birch_log", "vlag": "pink_wool", "streep": "white_wool", "knop": "gold_block"},
                el=[doos((5, 0, 5), (11, 1, 11), "hout"),
                    doos((7.5, 1, 7.5), (8.5, 14, 8.5), "paal"),
                    doos((8.5, 8, 7.75), (15, 14, 8.25), "vlag"),
                    doos((8.5, 10.5, 7.7), (15, 11.5, 8.3), "streep"),
                    doos((7, 14, 7), (9, 16, 9), "knop")],
                light=0, sound="WOOL")


def _zone1():
    return dict(nl="Knuffelzwamlampje", en="Snuggle Shroom Lamp",
                tex={"steel": "mushroom_stem", "hoed": "pink_wool", "gloed": "shroomlight", "stip": "white_wool"},
                el=[doos((6, 0, 6), (10, 7, 10), "steel"),
                    doos((4, 6.5, 4), (12, 7, 12), "gloed"),
                    doos((3, 7, 3), (13, 11, 13), "hoed"),
                    doos((5, 11, 5), (11, 12.5, 11), "hoed"),
                    doos((4, 9, 2.9), (6, 10, 3.1), "stip"),
                    doos((10, 8, 12.9), (12, 9, 13.1), "stip"),
                    doos((12.9, 9, 6), (13.1, 10, 8), "stip"),
                    doos((7, 12.5, 7), (9, 12.6, 9), "stip")],
                light=12, sound="WOOL")


def _zone2():
    return dict(nl="Splinternaaldjes", en="Splinter Needles",
                tex={"sneeuw": "snow", "blauw": "blue_ice", "ijs": "packed_ice"},
                el=[doos((3, 0, 3), (13, 2, 13), "sneeuw"),
                    doos((7, 2, 7), (9, 15, 9), "blauw"),
                    doos((7.5, 15, 7.5), (8.5, 16, 8.5), "blauw"),
                    doos((4, 2, 5), (6, 10, 7), "ijs"),
                    doos((10, 2, 9), (12, 12, 11), "ijs"),
                    doos((5, 2, 10), (7, 7, 12), "blauw"),
                    doos((10, 2, 4), (11.5, 6, 5.5), "blauw")],
                light=4, sound="GLASS")


def _zone3():
    return dict(nl="Schemerwortellantaarn", en="Duskroot Lantern",
                tex={"wortel": "mangrove_log", "stam": "dark_oak_log", "licht": "verdant_froglight_side", "dak": "mangrove_planks"},
                el=[doos((2, 0, 7), (14, 1.5, 9), "wortel"),
                    doos((7, 0, 2), (9, 1.5, 14), "wortel"),
                    doos((6.5, 1, 6.5), (9.5, 7, 9.5), "stam"),
                    doos((5, 7, 5), (11, 12, 11), "licht"),
                    doos((4, 12, 4), (12, 13, 12), "dak"),
                    doos((6, 13, 6), (10, 14, 10), "dak"),
                    doos((7.5, 14, 7.5), (8.5, 16, 8.5), "wortel")],
                light=13, sound="WOOD")


def _zone4():
    return dict(nl="Lampionvliegertje", en="Lantern Kite",
                tex={"voet": "bamboo_planks", "stok": "bamboo_block", "lampion": "orange_wool", "rand": "red_wool", "vlieger": "pink_wool",
                     "staart": "yellow_wool"},
                el=[doos((5, 0, 5), (11, 1, 11), "voet"),
                    doos((7.5, 1, 7.5), (8.5, 12, 8.5), "stok"),
                    doos((5.5, 2.5, 5.5), (10.5, 3, 10.5), "rand"),
                    doos((5, 3, 5), (11, 8, 11), "lampion"),
                    doos((5.5, 8, 5.5), (10.5, 8.5, 10.5), "rand"),
                    doos((8.5, 9, 7.75), (14.5, 15, 8.25), "vlieger", draai("z", 45, (11.5, 12, 8))),
                    doos((8.5, 11.75, 7.7), (14.5, 12.25, 8.3), "staart", draai("z", 45, (11.5, 12, 8)))],
                light=13, sound="WOOL")


def _zone5():
    return dict(nl="Hoogvads-roosvenstertje", en="Highchonk Rose Window",
                tex={"voet": "smooth_stone", "steen": "quartz_block_side", "goud": "gold_block", "roze": "magenta_stained_glass",
                     "blauw": "light_blue_stained_glass"},
                el=[doos((3, 0, 6), (13, 1, 10), "voet"),
                    doos((2, 1, 7), (14, 2, 9), "steen"),
                    doos((2, 12, 7), (14, 13, 9), "steen"),
                    doos((2, 2, 7), (3, 12, 9), "steen"),
                    doos((13, 2, 7), (14, 12, 9), "steen"),
                    doos((3, 2, 7.5), (8, 7, 8.5), "roze"),
                    doos((8, 2, 7.5), (13, 7, 8.5), "blauw"),
                    doos((3, 7, 7.5), (8, 12, 8.5), "blauw"),
                    doos((8, 7, 7.5), (13, 12, 8.5), "roze"),
                    doos((7.5, 2, 7.25), (8.5, 12, 8.75), "goud"),
                    doos((3, 6.5, 7.25), (13, 7.5, 8.75), "goud"),
                    doos((5, 13, 7), (11, 14, 9), "steen"),
                    doos((7, 14, 7), (9, 15.5, 9), "goud")],
                light=6, sound="GLASS", render="minecraft:translucent")


def _zone6():
    return dict(nl="Sterrentorentje", en="Little Star Tower",
                tex={"voet": "cut_sandstone", "toren": "sandstone_top", "band": "gold_block", "kop": "chiseled_sandstone", "ster": "glowstone"},
                el=[doos((4, 0, 4), (12, 2, 12), "voet"),
                    doos((5, 2, 5), (11, 11, 11), "toren"),
                    doos((4.5, 6, 4.5), (11.5, 7, 11.5), "band"),
                    doos((6, 11, 6), (10, 13, 10), "kop"),
                    doos((6.5, 13.25, 7.5), (9.5, 16.25, 8.5), "ster", draai("z", 45, (8, 14.75, 8))),
                    doos((7.5, 13.25, 6.5), (8.5, 16.25, 9.5), "ster", draai("x", 45, (8, 14.75, 8)))],
                light=10, sound="STONE")


def _raid():
    return dict(nl="Sterrendovende trofee", en="Stardousing Trophy",
                tex={"voet": "polished_blackstone", "goud": "gold_block", "edel": "amethyst_block", "ring": "diamond_block"},
                el=[doos((4, 0, 4), (12, 2, 12), "voet"),
                    doos((7, 2, 7), (9, 6, 9), "goud"),
                    doos((5.5, 5, 5.5), (10.5, 6, 10.5), "goud"),
                    doos((4.5, 6, 4.5), (11.5, 12, 11.5), "goud"),
                    doos((2.5, 8, 7.5), (4.5, 11, 8.5), "goud"),
                    doos((11.5, 8, 7.5), (13.5, 11, 8.5), "goud"),
                    doos((7, 8, 4), (9, 10, 4.5), "edel"),
                    doos((6.5, 12, 6.5), (9.5, 12.75, 9.5), "ring"),
                    doos((7.25, 12.75, 7.25), (8.75, 13.5, 8.75), "edel")],
                light=4, sound="METAL")


# the "Geheimpjesdoosje" of each zone: one model, its lid and gem in the zone's colours
GEHEIM_KLEUR = {1: ("pink_wool", "amethyst_block"), 2: ("light_blue_wool", "diamond_block"), 3: ("purple_wool", "emerald_block"),
                4: ("orange_wool", "copper_block"), 5: ("white_wool", "lapis_block"), 6: ("yellow_wool", "raw_gold_block")}


def _geheim(n):
    deksel, edel = GEHEIM_KLEUR[n]
    return dict(nl=f"Geheimpjesdoosje van zone {n}", en=f"Little Secrets Box of zone {n}",
                tex={"kist": "spruce_planks", "deksel": deksel, "slot": "gold_block", "edel": edel},
                el=[doos((4, 0, 5), (12, 5, 11), "kist"),
                    doos((3.5, 5, 4.5), (12.5, 7, 11.5), "deksel"),
                    doos((7, 3, 4.5), (9, 5.5, 5), "slot"),
                    doos((7, 7, 7), (9, 8, 9), "edel")],
                light=0, sound="WOOD")


def blokken():
    """id (without the guhs: prefix) -> spec, in creative-tab order."""
    b = {P + "rekruut": _rekruut()}
    for n, f in enumerate((_zone1, _zone2, _zone3, _zone4, _zone5, _zone6), 1):
        b[P + f"zone{n}"] = f()
    b[P + "raid"] = _raid()
    for n in range(1, 7):
        b[P + f"geheim{n}"] = _geheim(n)
    # the tooltip line
    b[P + "rekruut"].update(lore_nl="Beloning: rekruut van De Knuffelorde in Het Vadsrijk",
                            lore_en="Reward: recruit of the Snuggle Order in The Chonkdom")
    for n in range(1, 7):
        b[P + f"zone{n}"].update(lore_nl=f"Beloning: Zone {n} van Het Vadsrijk uitgespeeld",
                                 lore_en=f"Reward: Zone {n} of The Chonkdom completed")
        b[P + f"geheim{n}"].update(lore_nl=f"Beloning: alle geheimpjes van zone {n} van Het Vadsrijk gevonden",
                                   lore_en=f"Reward: every secret of zone {n} of The Chonkdom found")
    b[P + "raid"].update(lore_nl="Beloning: De Sterrendovende Toren van Het Vadsrijk verslagen",
                         lore_en="Reward: The Stardousing Tower of The Chonkdom defeated")
    return b


def bounds(spec):
    """The (x0, y0, z0, x1, y1, z1) box around every element (rotated ones: their unrotated box), clamped to the block."""
    lo, hi = [16.0] * 3, [0.0] * 3
    for e in spec["el"]:
        for i in range(3):
            lo[i] = min(lo[i], e["from"][i])
            hi[i] = max(hi[i], e["to"][i])
    return tuple(max(0.0, v) for v in lo) + tuple(min(16.0, v) for v in hi)


# ---- the texts -------------------------------------------------------------------------------------------------------------
def teksten():
    """key -> (nl, en)."""
    t = {}
    for bid, s in blokken().items():
        t[f"block.guhs.{bid}"] = (s["nl"], s["en"])
        t[f"block.guhs.{bid}.lore"] = (s["lore_nl"], s["lore_en"])
    t[T + "claim.vadsrijk"] = ("Beloningen claim je in je eigen wereld, niet in het Vadsrijk.",
                               "You claim rewards in your own world, not in The Chonkdom.")
    t[T + "plaats.vadsrijk"] = ("Dit pronkstuk is voor je eigen wereld. In het Vadsrijk past het niet.",
                                "This showpiece is for your own world. It does not belong in The Chonkdom.")
    return t


# ---- the FTB quests (make_ftbquests: ftb(fq), FTB_SECTIES, FTB_DEPS, FTB_VELDEN) ---------------------------------------------
FTB_CHAPTER = "guhs_vadsrijk"
FTB_PORTRAIT = "guh:golden"
FTB_SECTIES = [
    ("vr_orde", "De Knuffelorde", "guh:normal", ["vr_ftb_rekruut"] + [f"vr_ftb_poort{n}" for n in range(2, 7)]),
    ("vr_avontuur", "Het avontuur", "guh:golden", [f"vr_ftb_zone{n}" for n in range(1, 7)] + ["vr_ftb_raid"]),
    ("vr_geheimpjes", "Geheimpjes", "guh:choco", [f"vr_ftb_geheimen{n}" for n in range(1, 7)]),
]
# the only real dependencies of the chapter: what lies in a zone hangs behind that zone's "is open!" quest (the zone gate)
FTB_DEPS = {}
for _n in range(2, 7):
    FTB_DEPS[f"vr_ftb_zone{_n}"] = [f"vr_ftb_poort{_n}"]
    FTB_DEPS[f"vr_ftb_geheimen{_n}"] = [f"vr_ftb_poort{_n}"]
FTB_DEPS["vr_ftb_raid"] = ["vr_ftb_poort6"]
# extra FTB Quests fields per quest
FTB_VELDEN = {k: {"hide_dependency_lines": True} for k in (["vr_ftb_rekruut", "vr_ftb_raid"]
                                                           + [f"vr_ftb_{s}{n}" for s in ("zone", "geheimen") for n in range(1, 7)]
                                                           + [f"vr_ftb_poort{n}" for n in range(2, 7)])}
for _k in FTB_DEPS:
    FTB_VELDEN[_k]["hide_until_deps_complete"] = True
for _n in range(2, 7):
    FTB_VELDEN[f"vr_ftb_poort{_n}"].update(invisible=True, size=0.75)

# the Vadsrijk-vitrine of slice y-vitrine (110-y-vitrine): the trophy cabinet for your base, given with the recruit quest
# (that item lives on the y-vitrine branch; integration merges both)
VITRINE = "guhs:vr_vitrine"
CLAIM_REGEL = ("&7Beloning: een pronkstuk voor je eigen wereld. Claimen doe je thuis, in de Guhmensie of de gewone wereld; in het "
               "Vadsrijk zelf kan het niet.")
ELK = "Met welk karakter of in welke modus maakt niet uit: één keer is genoeg."


def ftb(fq):
    q, adv, met = fq.q, fq.adv, fq.met
    q("vr_ftb_rekruut", "Word rekruut van De Knuffelorde",
      "Vind de ingestorte ordetempel in de Guhmensie en stap door het portaal. Op &dKnuffelkamp&r leert Sergeant Pluizebol je het vak: "
      "rond je opleiding af door de Klodderreus op te vrolijken. " + ELK + "\n\n" + CLAIM_REGEL,
      f"guhs:{P}rekruut", [met(adv("vr_ftb_rekruut"), "Opleiding op Knuffelkamp af")], rewards=((f"guhs:{P}rekruut", 1), (VITRINE, 1)), shape="gear")
    for n in range(2, 7):
        q(f"vr_ftb_poort{n}", f"Zone {n} is open!",
          f"De Knuffelorde vliegt nu ook naar &d{ZONES[n][0]}&r. De quests van zone {n} staan nu in dit hoofdstuk.",
          "minecraft:filled_map", [met(adv(f"vr_ftb_poort{n}"), f"Zone {n} is open")], rewards=(), shape="diamond")
    for n in range(1, 7):
        q(f"vr_ftb_zone{n}", f"Zone {n} uitgespeeld",
          f"Speel het verhaal van &d{ZONES[n][0]}&r helemaal uit, tot en met de laatste hoofdquest. " + ELK + "\n\n" + CLAIM_REGEL,
          f"guhs:{P}zone{n}", [met(adv(f"vr_ftb_zone{n}"), f"Zone {n} uitgespeeld")], rewards=((f"guhs:{P}zone{n}", 1),),
          shape="hexagon")
    q("vr_ftb_raid", "De Sterrendovende Toren verslagen",
      "Klim met je groep naar de top van &dDe Sterrendovende Toren&r, de raid van zone 6, en vrolijk &dHertogin Ringeling&r op. "
      "De eerste keer telt. " + ELK + "\n\n" + CLAIM_REGEL,
      f"guhs:{P}raid", [met(adv("vr_ftb_raid"), "De Toren verslagen")], rewards=((f"guhs:{P}raid", 1),), shape="gear")
    for n in range(1, 7):
        q(f"vr_ftb_geheimen{n}", f"Alle geheimpjes van zone {n}",
          f"Vind in &d{ZONES[n][0]}&r alle geheimen, verloren knuffels en puzzels. Je &dVerkenningsboek&r (B) houdt bij wat je nog "
          f"mist. " + ELK + "\n\n" + CLAIM_REGEL,
          f"guhs:{P}geheim{n}", [met(adv(f"vr_ftb_geheimen{n}"), f"Geheimpjes van zone {n}")], rewards=((f"guhs:{P}geheim{n}", 1),))


def wiki_verborgen(open_zones=1):
    """The quest keys a player does not see yet while zones 1..open_zones are open (tools/wiki_site/gamedata.py leaves them out)."""
    weg = [] if open_zones >= 6 else ["vr_ftb_raid"]
    for n in range(max(2, open_zones + 1), 7):
        weg += [f"vr_ftb_zone{n}", f"vr_ftb_geheimen{n}", f"vr_ftb_poort{n}"]
    return weg


def advancement_namen():
    return (["vr_ftb_rekruut", "vr_ftb_raid"] + [f"vr_ftb_zone{n}" for n in range(1, 7)] + [f"vr_ftb_geheimen{n}" for n in range(1, 7)]
            + [f"vr_ftb_poort{n}" for n in range(2, 7)])


# ---- the resources ----------------------------------------------------------------------------------------------------------
def bestanden():
    """{path under src/main/resources: json object}: advancements, block models, blockstates, item models, loot tables."""
    out = {}
    for a in advancement_namen():
        out[f"data/guhs/advancement/quest/{a}.json"] = {"criteria": {"done": {"trigger": "minecraft:impossible"}}}
    for bid, s in blokken().items():
        tex = {k: f"minecraft:block/{v}" for k, v in s["tex"].items()}
        tex["particle"] = tex[next(iter(s["tex"]))]
        model = {"parent": "minecraft:block/block"}
        if s.get("render"):
            model["render_type"] = s["render"]
        model.update(textures=tex, elements=s["el"])
        out[f"assets/guhs/models/block/{bid}.json"] = model
        out[f"assets/guhs/blockstates/{bid}.json"] = {"variants": {
            f"facing={f}": {"model": f"guhs:block/{bid}", **({"y": y} if y else {})}
            for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}
        out[f"assets/guhs/models/item/{bid}.json"] = {"parent": f"guhs:block/{bid}"}
        out[f"assets/guhs/items/{bid}.json"] = {"model": {"type": "minecraft:model", "model": f"guhs:item/{bid}"}}
        out[f"data/guhs/loot_table/blocks/{bid}.json"] = {"type": "minecraft:block", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"guhs:{bid}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
    return out


def build(h):
    for pad, obj in bestanden().items():
        h.w(os.path.join(h.R, *pad.split("/")), obj)
    for k, (nl, en) in teksten().items():
        h.lang(k, en, nl)


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _direct():
    r = os.path.join("src", "main", "resources")
    for pad, obj in bestanden().items():
        _schrijf(os.path.join(r, *pad.split("/")), obj)
    t = teksten()
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(r, "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in t.items()})
        _schrijf(p, data)
    print(f"vadsrijk_y_ftb: {len(blokken())} blocks, {len(advancement_namen())} advancements, {len(t)} texts")
    for bid, s in blokken().items():
        print(f"  {bid}: box {bounds(s)}")


if __name__ == "__main__":
    _direct()
