"""
De Spiesburcht (2.7.0, slice 2): the creatures, buildings, brewing and the boss of the Guhbarbecuether.

  - creatures: the Rookguh (ghast parody, always peaceful: feed it until it's VAHOEG and floats home), the Vonk-Mika
    (blaze: grillspiesen), the Knekel-Mika (wither skeleton: now and then a verkoolde mikakop), the Aangebrande Mika
    (the wither: a T of as_blok with three verkoolde mikakoppen, in any dimension; drops the gloeister), and the Asguh
    (a guh variant of the Asdal); models in spiesburcht_modellen.py
  - the Nether-Mikas trade vahoege vads ingots (loot table gameplay/nether_mika_ruil)
  - the structures guhs:spiesburcht (fortress) and guhs:mika_grillpaleis (bastion): spiesburcht_burcht.py
  - the Guhbrouwketel with the four Guhdrankjes, and the Knabbelbaken: spiesburcht_tex.py (textures/models)
  - spawns per Barbecuether biome, loot, recipes, tags, advancements (in the Barbecuether tab), FTB quests, lang

build(h) makes everything (h = make_v2). Java: src/main/java/nl/juiced/guhs/feature/spiesburcht/.
"""
import json
import os

from . import spiesburcht_burcht as burcht
from . import spiesburcht_modellen as modellen
from . import spiesburcht_tex as tex

NAME = "spiesburcht"
BIOMES = ["houtskoolvlakte", "satebos", "worstenwoud", "asdal", "rookdelta"]
BLOCKS = ["verkoolde_mikakop", "verkoolde_mikakop_muur", "guhbrouwketel", "knabbelbaken"]
ITEMS = ["grillspies", "grillspiespoeder", "gloeister", "gloeiend_kooltje", "drankje_van_vahoegheid", "rookloopdrankje",
         "sluipknabbeldrankje", "guhsprongdrankje", "rookguh_spawn_egg", "vonk_mika_spawn_egg", "knekel_mika_spawn_egg",
         "aangebrande_mika_spawn_egg"]
ENTITIES = ["rookguh", "vonk_mika", "knekel_mika", "aangebrande_mika"]

# --- the Asguh: glowing ember cheeks (a variant bone, see GuhVariant.ASGUH and VARIANT_BONES) ---
_H = [0, 6, -2]
BONES = {
    "asguh_wangen": ("head", _H, "asguh_wang", [([4.4, 5.0, -12.4], [3.0, 1.9, 0.45], 0), ([-7.4, 5.0, -12.4], [3.0, 1.9, 0.45], 0)]),
}


def variants(rng, v):
    def cheeks():
        a = v.fabric((255, 120, 60), rng, 18)
        px = a.shape[0]
        for y in range(px):
            for x in range(px):
                d = ((x - px / 2) ** 2 + (y - px / 2) ** 2) ** 0.5
                if d < px * 0.3:
                    a[y, x] = (255, 200, 110)
        return a
    return {"asguh": ((118, 112, 116), {"asguh_wang": cheeks})}


# =====================================================================================================================
# spawns
# =====================================================================================================================
# 1.4.1: the weights only set the MIX within a biome. How many aggressive mobs walk around in the open is Drukte.MAX_BOOS (Java,
# feature/spiesburcht/Drukte: the natural spawner filled vanilla's cap of 70 around a player, whatever the weights). The
# Knekel-Mika of the Asdal went from 10 to 7 (Nether-Mika 8, features/barbecuether.py): with half the cap that leaves a bit
# less than half of them (measured around a player: 17 -> 8).
SPAWNS = {  # biome: (monsters to add, creatures)
    "houtskoolvlakte": ([("guhs:vonk_mika", 1, 1, 1)], [("guhs:rookguh", 6, 1, 1)]),
    "satebos": ([], []),
    "worstenwoud": ([], []),
    "asdal": ([("guhs:knekel_mika", 7, 1, 2)], [("guhs:guh", 14, 2, 3), ("guhs:rookguh", 2, 1, 1)]),
    "rookdelta": ([("guhs:vonk_mika", 3, 1, 1)], [("guhs:rookguh", 10, 1, 2)]),
}


def spawns(h):
    for b, (monsters, creatures) in SPAWNS.items():
        def add(d, monsters=monsters, creatures=creatures):
            ours = {m[0] for m in monsters + creatures}
            d["spawners"]["monster"] = [e for e in d["spawners"].get("monster", []) if e["type"] not in ours] + \
                [{"type": t, "weight": w, "minCount": lo, "maxCount": hi} for t, w, lo, hi in monsters]
            d["spawners"]["creature"] = [e for e in d["spawners"].get("creature", []) if e["type"] not in ours] + \
                [{"type": t, "weight": w, "minCount": lo, "maxCount": hi} for t, w, lo, hi in creatures]
        h.patch_json(f"{h.D}/worldgen/biome/{b}.json", add)


def grounded_forests(h):
    """The giant saté skewers and sausages only grow out of their nylium (like the Nether's fungi), so they don't sprout
    in the halls of the Spiesburcht or the Mika-grillpaleis (a patch of the Barbecuether's placed features)."""
    only_on_nylium = {"type": "minecraft:block_predicate_filter", "predicate": {
        "type": "minecraft:matching_block_tag", "offset": [0, -1, 0], "tag": "minecraft:nylium"}}
    for name in ("bbq_satespiezen", "bbq_braadworsten"):
        def add(d):
            if only_on_nylium not in d["placement"]:
                d["placement"].insert(len(d["placement"]) - 1, only_on_nylium)
        h.patch_json(f"{h.D}/worldgen/placed_feature/{name}.json", add)


# =====================================================================================================================
# the buildings
# =====================================================================================================================
def structures(h):
    D = h.D
    built = burcht.build_all(h)
    problems = []
    for name, (_nx, _nz, _anchor, probs, _b) in built.items():
        problems += probs
    if problems:
        print("spiesburcht geometry check found problems:\n  " + "\n  ".join(problems[:60]))
        raise SystemExit(f"spiesburcht: fix the building templates ({len(problems)} problems, see above)")
    print("spiesburcht: geometry checks ok (spiesburcht, mika_grillpaleis)")
    h.Structure((16, 16, 16)).save("spiesburcht_testkamer")          # (an empty room for the GameTests)
    settings = {
        "spiesburcht": {"placement": "brug", "min_y": 42, "max_y": 64, "reach": 40, "step": "underground_decoration",
                        "biomes": BIOMES, "spawns": [("guhs:vonk_mika", 10, 1, 2), ("guhs:knekel_mika", 8, 1, 2), ("guhs:nether_mika", 4, 1, 1)]},
        "mika_grillpaleis": {"placement": "paleis", "min_y": 33, "max_y": 33, "reach": 26, "step": "surface_structures",
                             "biomes": [b for b in BIOMES if b != "rookdelta"], "spawns": [("guhs:nether_mika", 12, 1, 3)]},
    }
    for name, (nx, nz, anchor, _p, _b) in built.items():
        s = settings[name]
        h.w(f"{D}/worldgen/structure/{name}.json", {
            "type": "guhs:burcht", "biomes": f"#guhs:has_structure/{name}", "step": s["step"], "terrain_adaptation": "none",
            "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": [
                {"type": t, "weight": w, "minCount": lo, "maxCount": hi} for t, w, lo, hi in s["spawns"]]}},
            "templates": f"guhs:{name}", "tiles_x": nx, "tiles_z": nz, "tile_size": burcht.TILE, "anchor": list(anchor),
            "placement": s["placement"], "min_y": s["min_y"], "max_y": s["max_y"], "reach": s["reach"]})
        h.w(f"{D}/tags/worldgen/biome/has_structure/{name}.json", {"values": [f"guhs:{b}" for b in s["biomes"]]})
    # one set for both (like the Nether's fortresses and bastions): if one can't stand somewhere, the other may
    h.w(f"{D}/worldgen/structure_set/barbecue_burchten.json", {
        "structures": [{"structure": "guhs:spiesburcht", "weight": 2}, {"structure": "guhs:mika_grillpaleis", "weight": 2}],
        "placement": {"type": "minecraft:random_spread", "spacing": 20, "separation": 5, "salt": 27020027,
                      "exclusion_zone": {"other_set": "guhs:barbecueput", "chunk_count": 3}}})
    return built


# =====================================================================================================================
# loot, recipes, tags
# =====================================================================================================================
def table(entries, rolls=(3, 6)):
    return {"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": [
        {"type": "minecraft:item", "name": item, "weight": wgt, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]} for item, wgt, lo, hi in entries]}


def loot(h):
    D = h.D
    looting = {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
               "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}
    by_player = {"condition": "minecraft:killed_by_player"}

    def entity(name, pools):
        h.w(f"{D}/loot_table/entities/{name}.json", {"type": "minecraft:entity", "pools": pools})

    def pool(item, lo, hi, conditions=(), extra=()):
        return {"rolls": 1, "entries": [{"type": "minecraft:item", "name": item, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}, looting, *extra]}],
            "conditions": list(conditions)}
    entity("vonk_mika", [pool("guhs:grillspies", 0, 1, [by_player]), pool("guhs:gloeikoolgruis", 0, 2)])
    entity("knekel_mika", [pool("minecraft:coal", -1, 1), pool("guhs:verkoold_guhbot", 0, 2),
                           {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:verkoolde_mikakop"}], "conditions": [
                               by_player, {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                           "unenchanted_chance": 0.025, "enchanted_chance": {"type": "minecraft:linear", "base": 0.035,
                                                                                               "per_level_above_first": 0.01}}]}])
    entity("aangebrande_mika", [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:gloeister"}]},
                                pool("guhs:gloeikoolgruis", 4, 8), pool("guhs:grillkool", 1, 3)])
    entity("rookguh", [])
    # the Nether-Mikas' trade for one vahoege vads ingot (like piglin bartering)
    h.w(f"{D}/loot_table/gameplay/nether_mika_ruil.json", {"type": "minecraft:barter", "pools": [table([
        ("guhs:rookloopdrankje", 8, 1, 1), ("guhs:sluipknabbeldrankje", 4, 1, 1), ("minecraft:iron_nugget", 10, 9, 36),
        ("guhs:gloeikoolgruis", 20, 4, 10), ("guhs:kaas_knabbels", 40, 4, 12), ("guhs:grillkool", 40, 1, 1), ("guhs:aanmaakblokje", 10, 1, 1),
        ("minecraft:string", 20, 3, 9), ("guhs:guhbraadworst", 20, 2, 4), ("guhs:as_blok", 40, 2, 8), ("guhs:houtskoolsteen_stenen", 40, 4, 12),
        ("minecraft:ender_pearl", 10, 2, 4), ("guhs:gegrilde_kaasknabbelsate", 15, 1, 2), ("guhs:grillspies", 5, 1, 1),
        ("guhs:as_aarde", 20, 8, 16), ("minecraft:leather", 10, 2, 4)], rolls=(1, 1))]})
    chests = {
        "spiesburcht": [("minecraft:iron_ingot", 10, 1, 5), ("minecraft:gold_ingot", 12, 1, 3), ("minecraft:diamond", 3, 1, 2),
                        ("guhs:grillkool", 8, 2, 4), ("guhs:gloeikoolgruis", 10, 2, 6), ("guhs:gegrilde_kaasknabbelsate", 10, 1, 4),
                        ("minecraft:saddle", 6, 1, 1), ("guhs:vahoege_vads_ingot", 3, 1, 1), ("guhs:guhbraadworst", 8, 1, 3),
                        ("guhs:aanmaakblokje", 5, 1, 1), ("guhs:rookloopdrankje", 5, 1, 1), ("guhs:grillspies", 4, 1, 2),
                        ("guhs:kaas_knabbels", 12, 4, 12), ("minecraft:golden_horse_armor", 4, 1, 1)],
        "spiesburcht_tuintje": [("guhs:pindascheutjes", 10, 2, 6), ("guhs:sate_zwammetje", 8, 1, 3), ("guhs:kaas_knabbels", 12, 4, 10),
                                ("guhs:kaasknabbelsate", 10, 1, 4), ("minecraft:bone_meal", 10, 2, 6), ("guhs:gloeikoolgruis", 6, 1, 4),
                                ("guhs:pindasaus_nylium", 6, 1, 3), ("guhs:drankje_van_vahoegheid", 3, 1, 1)],
        "grillpaleis_schat": [("guhs:vahoege_vads_ingot", 20, 1, 3), ("guhs:vahoege_vads", 12, 1, 3), ("guhs:compressed_super_vahoege_vads", 2, 1, 1),
                              ("minecraft:diamond", 8, 1, 2), ("guhs:kaas_knabbels", 20, 12, 32), ("guhs:block_of_kaasknabbels", 12, 1, 3),
                              ("guhs:gefrituurde_kaasknabbels", 12, 3, 8), ("guhs:vahoege_vads_sword", 2, 1, 1), ("guhs:vahoege_vads_helmet", 2, 1, 1),
                              ("guhs:drankje_van_vahoegheid", 6, 1, 2), ("guhs:verkoolde_mikakop", 1, 1, 1), ("minecraft:gold_block", 3, 1, 1)],
        "grillpaleis_voorraad": [("guhs:kaas_knabbels", 40, 8, 24), ("guhs:block_of_kaasknabbels", 8, 1, 2), ("guhs:gefrituurde_kaasknabbels", 15, 4, 8),
                                 ("guhs:kaasknabbelsate", 15, 1, 4), ("minecraft:iron_nugget", 10, 4, 12), ("minecraft:string", 8, 2, 6),
                                 ("guhs:gloeikoolgruis", 10, 2, 5), ("guhs:guhbraadworst", 10, 1, 3), ("guhs:vahoege_vads_ingot", 2, 1, 1)],
    }
    for name, entries in chests.items():
        h.w(f"{D}/loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [table(entries)]})
    for b in ("guhbrouwketel", "knabbelbaken"):
        h.self_drop(b)
    h.self_drop("verkoolde_mikakop")
    h.self_drop("verkoolde_mikakop_muur", "verkoolde_mikakop")


def recipes(h):
    h.shapeless("grillspiespoeder", ["guhs:grillspies"], "guhs:grillspiespoeder", 2)
    h.shaped("guhbrouwketel", ["I I", "III", "TST"], {"I": "minecraft:iron_ingot", "T": "guhs:roosterijzer_tralies", "S": "guhs:grillspies"},
             "guhs:guhbrouwketel")
    h.shaped("knabbelbaken", ["GGG", "GSG", "KKK"], {"G": "minecraft:glass", "S": "guhs:gloeister", "K": "guhs:grillkool"}, "guhs:knabbelbaken")


BROUW = {  # Brouwsel: ingredients (item tag guhs:brouwsel/<id>)
    "vahoegheid": ["guhs:kaas_knabbels", "guhs:gefrituurde_kaasknabbels"],
    "rookloop": ["guhs:gloeikoolgruis", "guhs:guhbraadworst"],
    # moeraskaas comes from the Kaasmoeras (another part of 2.7): optional, so the recipe also works without it
    "sluipknabbel": [{"id": "guhs:moeraskaas", "required": False}, "guhs:mika_vet"],
    "guhsprong": ["guhs:guh_slimeball"],
}


def tags(h):
    for brew, items in BROUW.items():
        h.add_tag(f"guhs/tags/item/brouwsel/{brew}", items)
    h.add_tag("guhs/tags/item/vads_uitrusting", [f"guhs:vahoege_vads_{p}" for p in ("helmet", "chestplate", "leggings", "boots")])
    # 1.3.1: only blocks of vahoege vads (written whole, not add_tag: the knabbel block, the vads ore and gatenkaas are out)
    h.w(f"{h.D}/tags/block/knabbelbaken_basis.json", {"replace": False, "values": ["guhs:block_of_vahoege_vads"]})
    h.add_tag("guhs/tags/block/aangebrande_mika_basis", ["guhs:as_blok", "guhs:as_aarde"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:guhbrouwketel"])
    h.add_tag("minecraft/tags/block/wither_immune", ["guhs:knabbelbaken"])
    h.add_tag("minecraft/tags/entity_type/fall_damage_immune", ["guhs:rookguh", "guhs:vonk_mika", "guhs:aangebrande_mika"])


# =====================================================================================================================
# lang
# =====================================================================================================================
LANG = {
    "entity.guhs.rookguh": "Rookguh",
    "entity.guhs.vonk_mika": "Vonk-Mika",
    "entity.guhs.knekel_mika": "Knekel-Mika",
    "entity.guhs.aangebrande_mika": "Aangebrande Mika",
    "entity.guhs.gloeiend_kooltje": "Gloeiend kooltje",
    "entity.guhs.brandend_kooltje": "Brandend kooltje",
    "entity.guhs.guh.asguh": "Asguh",
    "block.guhs.verkoolde_mikakop": "Verkoolde mikakop",
    "block.guhs.verkoolde_mikakop_muur": "Verkoolde mikakop",
    "block.guhs.guhbrouwketel": "Guhbrouwketel",
    "block.guhs.knabbelbaken": "Knabbelbaken",
    "item.guhs.grillspies": "Grillspies",
    "item.guhs.grillspiespoeder": "Grillspiespoeder",
    "item.guhs.gloeister": "Gloeister",
    "item.guhs.gloeiend_kooltje": "Gloeiend kooltje",
    "item.guhs.drankje_van_vahoegheid": "Drankje van Vahoegheid",
    "item.guhs.rookloopdrankje": "Rookloopdrankje",
    "item.guhs.sluipknabbeldrankje": "Sluipknabbeldrankje",
    "item.guhs.guhsprongdrankje": "Guhsprongdrankje",
    "item.guhs.rookguh_spawn_egg": "Rookguh-spawnei",
    "item.guhs.vonk_mika_spawn_egg": "Vonk-Mika-spawnei",
    "item.guhs.knekel_mika_spawn_egg": "Knekel-Mika-spawnei",
    "item.guhs.aangebrande_mika_spawn_egg": "Aangebrande-Mika-spawnei",
    "item.guhs.guhdrankje.verzadiging": "Een volle buik (verzadiging)",
    "item.guhs.guhdrankje.vahoegheid.lore": "Kaasbouillon met knabbels. Je rent als een guh achter een knabbel aan: VAHOEG!",
    "item.guhs.guhdrankje.rookloop.lore": "Kaasbouillon met gloeikool. Door rook, vuur en kaasfrituursaus zonder schroeiplekken.",
    "item.guhs.guhdrankje.sluipknabbel.lore": "Kaasbouillon met moeraskaas of Mika's vet. Zo stil als een knabbel in een zak, njeg.",
    "item.guhs.guhdrankje.guhsprong.lore": "Kaasbouillon met guhslijm. Boing! En zachtjes weer naar beneden.",
    "structure.guhs.spiesburcht": "Spiesburcht",
    "structure.guhs.spiesburcht.tooltip": "Een burcht van houtskoolsteen met lange bruggen, Vonk-Mika's en een pindasaus-tuintje (Barbecuether)",
    "structure.guhs.mika_grillpaleis": "Mika-grillpaleis",
    "structure.guhs.mika_grillpaleis.tooltip": "Het paleis van de Nether-Mika's: een berg gestolen kaasknabbels en vads-schatkisten (Barbecuether)",
    "gui.guhs.guhdex.rarity.asguh": "Zeldzaamheid: Ongewoon (alleen in het Asdal)",
    "gui.guhs.guhdex.info.asguh": "Een grijze, roetige guh met gloeiende wangetjes uit het Asdal van de Barbecuether. Vuur doet hem niks. Voer hem een kaasknabbel en hij gloeit van blijdschap!",
    # 2.10.1: the Rookguh's own Guhdex page, with your saved Rookguhs (was a line above the whole Guhdex)
    "gui.guhs.guhdex.rookguhs": "Rookguhs gered: %s",
    "gui.guhs.guhdex.rarity.rookguh": "Zeldzaamheid: Ongewoon (zweeft rond in de Barbecuether)",
    "gui.guhs.guhdex.info.rookguh": "Een zielig mager guhspookje van rook, verdwaald in de Barbecuether. Voer hem kaasknabbels (uit je hand of gegooid): "
                                    "met elke knabbel wordt hij ronder en rozer, en dan zweeft hij VAHOEG naar huis. Hem slaan? Njeg, nooit!",
    # the Rookguh
    "quest.guhs.rookguh.honger": "De Rookguh kijkt je zielig aan... Hij wil kaasknabbels! (nog %s)",
    "quest.guhs.rookguh.gevoerd": "Njom! De Rookguh wordt al wat ronder. Nog %s knabbels tot VAHOEG!",
    "quest.guhs.rookguh.vahoeg": "VAHOEG! Ik ben weer vadsig genoeg om naar huis te zweven!",
    "quest.guhs.rookguh.niet_slaan": "Njeg! Een Rookguh sla je niet, die voer je.",
    "quest.guhs.rookguh.gered": "Je hebt al %s Rookguh(s) gered! (zie de Rookguh in je Guhdex)",
    # the Aangebrande Mika
    "quest.guhs.aangebrande_mika.wakker": "WIE HEEFT MIJ... AANGEBRAND?! Mijn knabbels! Njeg, dat zul je bezuren!",
    "quest.guhs.aangebrande_mika.doorgebakken": "NJEG! Nu word ik pas echt knapperig! Vonk-Mika's, help!",
    "quest.guhs.aangebrande_mika.hijgen": "*hijg* *puf* ...even afkoelen, njeg...",
    "quest.guhs.aangebrande_mika.dood": "NJEG... IK BEN... DOORGEBAKKEN!",
    # the Guhbrouwketel
    "quest.guhs.guhbrouwketel.vol_vuur": "De barbecue brandt al volop, njeg!",
    "quest.guhs.guhbrouwketel.gestookt": "Sssss... de barbecue onder de ketel brandt!",
    "quest.guhs.guhbrouwketel.al_vol": "De pan zit al vol.",
    "quest.guhs.guhbrouwketel.saus_erin": "Blub! Een pan vol kaasbouillon. Roer er nu een ingrediënt door.",
    "quest.guhs.guhbrouwketel.nog_even": "Nog even laten pruttelen...",
    "quest.guhs.guhbrouwketel.leeg": "De pan is leeg. Eerst een emmer kaassaus erin!",
    "quest.guhs.guhbrouwketel.eerst_ingredient": "Alleen kaasbouillon? Roer er eerst een ingrediënt door, vads!",
    "quest.guhs.guhbrouwketel.eerst_saus": "Eerst een emmer kaassaus in de pan!",
    "quest.guhs.guhbrouwketel.al_brouwsel": "Er pruttelt al een brouwsel in de pan.",
    "quest.guhs.guhbrouwketel.koud": "De barbecue is koud! Stook hem op met grillspiespoeder.",
    "quest.guhs.guhbrouwketel.roeren": "Roer, roer... het pruttelt!",
    "quest.guhs.guhbrouwketel.status": "Vuur: %s brouwsels - pan: %s porties %s",
    "quest.guhs.guhbrouwketel.brouwsel.bouillon": "kaasbouillon",
    "quest.guhs.guhbrouwketel.brouwsel.vahoegheid": "Drankje van Vahoegheid",
    "quest.guhs.guhbrouwketel.brouwsel.rookloop": "Rookloopdrankje",
    "quest.guhs.guhbrouwketel.brouwsel.sluipknabbel": "Sluipknabbeldrankje",
    "quest.guhs.guhbrouwketel.brouwsel.guhsprong": "Guhsprongdrankje",
    # the Knabbelbaken
    "quest.guhs.knabbelbaken.geen_piramide": "Zet het Knabbelbaken op een piramide van blokken vahoege vads!",
    "quest.guhs.knabbelbaken.gekozen": "Knabbelbaken: %s (%s lagen, %s blokken ver)",
    "quest.guhs.knabbelbaken.gunst.vahoeg": "VAHOEG (snelheid en een volle buik)",
    "quest.guhs.knabbelbaken.gunst.guhsprong": "Guhsprong (hoger springen)",
    "quest.guhs.knabbelbaken.gunst.vadsschild": "Vadsschild (weerstand)",
    "quest.guhs.knabbelbaken.gunst.knabbelherstel": "Knabbelherstel (genezing)",
    # the Nether-Mikas
    "quest.guhs.nether_mika.boos": "GRRR, njeg! Jij hebt ons geslagen. Geen handel!",
    "quest.guhs.nether_mika.bezig": "Njeg, even wachten! Ik ben nog aan het snuffelen.",
    "quest.guhs.nether_mika.hmm": "Hmmm... vads... *snuf snuf*",
    "quest.guhs.nether_mika.ruil": "De Nether-Mika gooit je iets toe. Njeg, eerlijk is eerlijk!",
    "quest.guhs.asguh.blij": "De Asguh gloeit helemaal van blijdschap!",
    "quest.guhs.spiesburcht.drank_vahoeg": "VAHOEG! Je voelt je supervads!",
    # signs
    "sign.guhs.spiesburcht.welkom1": "Welkom in de",
    "sign.guhs.spiesburcht.welkom2": "SPIESBURCHT!",
    "sign.guhs.spiesburcht.welkom3": "Pas op: vonken, njeg",
    "sign.guhs.spiesburcht.vonk1": "Vonk-Mika's!",
    "sign.guhs.spiesburcht.vonk2": "Niet aanraken:",
    "sign.guhs.spiesburcht.vonk3": "heet, heet, HEET!",
    "sign.guhs.spiesburcht.tuin1": "Pindasaus-tuintje",
    "sign.guhs.spiesburcht.tuin2": "Niet plukken,",
    "sign.guhs.spiesburcht.tuin3": "njeg! (of toch?)",
    "sign.guhs.spiesburcht.beeld1": "Voor de laatste guh",
    "sign.guhs.spiesburcht.beeld2": "die hier te lang",
    "sign.guhs.spiesburcht.beeld3": "op de grill lag.",
    "sign.guhs.grillpaleis.poort1": "MIKA-GRILLPALEIS",
    "sign.guhs.grillpaleis.poort2": "Guhs verboden!",
    "sign.guhs.grillpaleis.poort3": "(behalve met vads)",
    "sign.guhs.grillpaleis.stapel1": "ONZE knabbels.",
    "sign.guhs.grillpaleis.stapel2": "Eerlijk gejat.",
    "sign.guhs.grillpaleis.stapel3": "Afblijven, njeg!",
    "sign.guhs.grillpaleis.troon1": "Troon van de",
    "sign.guhs.grillpaleis.troon2": "Grote Nether-Mika",
    "sign.guhs.grillpaleis.troon3": "(even knabbels jatten)",
    "sign.guhs.grillpaleis.knuffel1": "Gejatte knuffelguhs",
    "sign.guhs.grillpaleis.knuffel2": "Losgeld: 1 vads",
    "sign.guhs.grillpaleis.knuffel3": "per knuffel",
    # advancements (the Barbecuether tab)
    "advancements.guhs.barbecuether.spiesburcht.title": "Een burcht vol spiesen",
    "advancements.guhs.barbecuether.spiesburcht.description": "Vind een Spiesburcht in de Barbecuether",
    "advancements.guhs.barbecuether.grillspies.title": "Van de grill gegrist",
    "advancements.guhs.barbecuether.grillspies.description": "Versla een Vonk-Mika en pak zijn grillspies",
    "advancements.guhs.barbecuether.brouwen.title": "Guh-alchemist",
    "advancements.guhs.barbecuether.brouwen.description": "Brouw een Guhdrankje in een Guhbrouwketel",
    "advancements.guhs.barbecuether.grillpaleis.title": "Wie heeft onze knabbels?",
    "advancements.guhs.barbecuether.grillpaleis.description": "Vind het Mika-grillpaleis vol gestolen kaasknabbels",
    "advancements.guhs.barbecuether.ruilen.title": "Vads tegen buit",
    "advancements.guhs.barbecuether.ruilen.description": "Ruil een vahoege vads-staaf met een Nether-Mika",
    "advancements.guhs.barbecuether.rookguh.title": "Vahoeg naar huis",
    "advancements.guhs.barbecuether.rookguh.description": "Voer een Rookguh tot hij vahoeg naar huis zweeft",
    "advancements.guhs.barbecuether.rookguh_redder.title": "Rookguh-redder",
    "advancements.guhs.barbecuether.rookguh_redder.description": "Red 10 Rookguhs",
    "advancements.guhs.barbecuether.mikakop.title": "Hoofdzaak",
    "advancements.guhs.barbecuether.mikakop.description": "Bemachtig een verkoolde mikakop",
    "advancements.guhs.barbecuether.aangebrande_mika.title": "Wie heeft dit aangebrand?",
    "advancements.guhs.barbecuether.aangebrande_mika.description": "Roep de Aangebrande Mika op",
    "advancements.guhs.barbecuether.gloeister.title": "Doorgebakken!",
    "advancements.guhs.barbecuether.gloeister.description": "Versla de Aangebrande Mika en pak de gloeister",
    "advancements.guhs.barbecuether.knabbelbaken.title": "Een baken van knabbels",
    "advancements.guhs.barbecuether.knabbelbaken.description": "Laat een Knabbelbaken branden op een piramide",
    "advancements.guhs.barbecuether.asguh.title": "Gloeiende wangetjes",
    "advancements.guhs.barbecuether.asguh.description": "Tem een Asguh in het Asdal",
}

QUEST_ADVANCEMENTS = ["rookguh_gered", "rookguh_redder", "guhdrankje_gedronken", "guhdrankje_gebrouwen", "knabbelbaken_aan",
                      "nether_mika_ruil", "aangebrande_mika_opgeroepen", "aangebrande_mika_verslagen", "asguh_gevoerd", "seen_asguh", "seen_rookguh"] + \
                     [f"found_{e}" for e in ENTITIES]


def advancements(h):
    D = h.D

    def adv(name, parent, icon, frame, criteria, requirements=None, hidden=False):
        data = {"parent": f"guhs:barbecuether/{parent}",
                "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.barbecuether.{name}.title"},
                            "description": {"translate": f"advancements.guhs.barbecuether.{name}.description"},
                            "frame": frame, "show_toast": True, "announce_to_chat": True, "hidden": hidden},
                "criteria": criteria}
        if requirements:
            data["requirements"] = requirements
        h.w(f"{D}/advancement/barbecuether/{name}.json", data)

    def has(*items):
        return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": list(items) if len(items) > 1 else items[0]}]}}
    done = {"done": {"trigger": "minecraft:impossible"}}
    adv("spiesburcht", "binnen", "guhs:gebeitelde_houtskoolsteen_stenen", "task",
        {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": "guhs:spiesburcht"}}}}})
    adv("grillspies", "spiesburcht", "guhs:grillspies", "task", {"done": has("guhs:grillspies")})
    adv("brouwen", "grillspies", "guhs:guhbrouwketel", "goal", {"done": has("guhs:drankje_van_vahoegheid", "guhs:rookloopdrankje",
                                                                             "guhs:sluipknabbeldrankje", "guhs:guhsprongdrankje")})
    adv("mikakop", "spiesburcht", "guhs:verkoolde_mikakop", "task", {"done": has("guhs:verkoolde_mikakop")})
    adv("aangebrande_mika", "mikakop", "guhs:as_blok", "goal",
        {"done": {"trigger": "minecraft:summoned_entity", "conditions": {"entity": {"type": "guhs:aangebrande_mika"}}}})
    adv("gloeister", "aangebrande_mika", "guhs:gloeister", "challenge", {"done": has("guhs:gloeister")})
    adv("knabbelbaken", "gloeister", "guhs:knabbelbaken", "goal", done)
    adv("grillpaleis", "binnen", "guhs:block_of_kaasknabbels", "task",
        {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": "guhs:mika_grillpaleis"}}}}})
    adv("ruilen", "grillpaleis", "guhs:vahoege_vads_ingot", "task", done)
    adv("rookguh", "binnen", "guhs:kaas_knabbels", "task", done)
    adv("rookguh_redder", "rookguh", "guhs:gefrituurde_kaasknabbels", "challenge", done)
    adv("asguh", "binnen", "guhs:as_blok", "goal", {"done": {"trigger": "minecraft:tame_animal", "conditions": {
        "entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                    "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"asguh\"}"}}]}}})
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    h.w(f"{D}/advancement/quest/tamed_asguh.json", {"criteria": {"done": {"trigger": "minecraft:tame_animal", "conditions": {
        "entity": [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"asguh\"}"}}]}}}})


# =====================================================================================================================
# the Asguh's glowing cheeks
# =====================================================================================================================
def asguh_glow(h):
    """The glowmask of the Asguh: the swatch its cheek bone samples on the guh texture (made by make_guh_variants.py)."""
    from PIL import Image
    geo = json.load(open(os.path.join(h.A, "geckolib", "models", "entity", "guh.geo.json"), encoding="utf-8"))
    bones = {b["name"]: b for b in geo["minecraft:geometry"][0]["bones"]}
    img = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    if "asguh_wangen" not in bones:
        print("spiesburcht: run tools/make_guh_variants.py first (the Asguh's cheek bone is not in guh.geo.json yet)")
        h.save(img, "entity", "guh_asguh_glowmask.png")
        return False
    px = img.load()
    for c in bones["asguh_wangen"]["cubes"]:
        for face in c["uv"].values():
            u, v = face["uv"]
            w, hh = face["uv_size"]
            for y in range(int(v * 4), int((v + hh) * 4)):
                for x in range(int(u * 4), int((u + w) * 4)):
                    if 0 <= x < 512 and 0 <= y < 512:
                        d = ((x - (u + w / 2) * 4) ** 2 + (y - (v + hh / 2) * 4) ** 2) ** 0.5
                        px[x, y] = (255, 200, 110, 255) if d < 5 else (255, 130, 60, 255)
    h.save(img, "entity", "guh_asguh_glowmask.png")
    return True


# =====================================================================================================================
# build
# =====================================================================================================================
def build(h):
    modellen.build(h)
    asguh_glow(h)
    tex.build(h)
    loot(h)
    recipes(h)
    tags(h)
    spawns(h)
    grounded_forests(h)
    structures(h)
    advancements(h)
    for key, text in LANG.items():
        h.lang(key, text, text)
    selfcheck_assets(h)


def selfcheck_assets(h):
    """check_assets.py only knows the registry classes: this checks our own blocks, items and entities."""
    A = h.A
    missing = []
    for b in BLOCKS:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ITEMS + ["verkoolde_mikakop", "guhbrouwketel", "knabbelbaken"]:
        if not os.path.exists(f"{A}/models/item/{i}.json"):
            missing.append(f"item model {i}")
        if i in ITEMS and f"item.guhs.{i}" not in h.NL:
            missing.append(f"lang item.guhs.{i}")
    for e in ENTITIES:
        for path in (f"{A}/geckolib/models/entity/{e}.geo.json", f"{A}/geckolib/animations/entity/{e}.animation.json", f"{A}/textures/entity/{e}.png"):
            if not os.path.exists(path):
                missing.append(path)
        if f"entity.guhs.{e}" not in h.NL:
            missing.append(f"lang entity.guhs.{e}")
    for e in ("vonk_mika", "knekel_mika", "aangebrande_mika"):
        if not os.path.exists(f"{A}/textures/entity/{e}_glowmask.png"):
            missing.append(f"glowmask {e}")
    for path, _dirs, files in os.walk(f"{A}/models"):
        for f in files:
            if f.startswith(("verkoolde_mikakop", "guhbrouwketel", "knabbelbaken")) or f[:-5] in ITEMS:
                model = json.load(open(os.path.join(path, f), encoding="utf-8"))
                for t in model.get("textures", {}).values():
                    if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                        missing.append(f"texture {t} ({f})")
    if missing:
        raise SystemExit(f"spiesburcht assets missing: {missing}")


# =====================================================================================================================
# FTB quests (rows y = 64 and 65.5, nothing locked)
# =====================================================================================================================
def ftb(fq):
    q, item, adv, structure, kill = fq.q, fq.item, fq.adv, fq.structure, fq.kill
    y = 64
    q("sb_burcht", "Een burcht vol spiesen", "Diep in de Barbecuether staan &6Spiesburchten&r: burchten van houtskoolsteen met lange bruggen. Het superkompas (Barbecue > Spiesburcht) weet er een.",
      "guhs:gebeitelde_houtskoolsteen_stenen", [structure("spiesburcht")], rewards=(("guhs:gegrilde_kaasknabbelsate", 4),), x=-8, y=y, shape="hexagon", xp=150)
    q("sb_vonk", "Van de grill gegrist", "Versla een &6Vonk-Mika&r (bij zijn spawner in de Spiesburcht) en pak zijn &6grillspies&r.",
      "guhs:grillspies", [item("guhs:grillspies")], rewards=(("guhs:gloeikoolgruis", 8),), x=-6, y=y)
    q("sb_knekel", "Knekel-Mika's", "Versla 5 &8Knekel-Mika's&r. Ze wonen in de Spiesburcht en in het Asdal.", "guhs:verkoold_guhbot",
      [kill("guhs:knekel_mika", 5)], rewards=(("guhs:kaas_knabbels", 12),), x=-4, y=y)
    q("sb_ketel", "De Guhbrouwketel", "Maak een &6Guhbrouwketel&r (ijzer, roosterijzer tralies en een grillspies) en stook hem op met grillspiespoeder.",
      "guhs:guhbrouwketel", [item("guhs:guhbrouwketel")], x=-2, y=y)
    q("sb_brouwen", "Guh-alchemist", "Kaassaus in de pan, een ingrediënt erdoor (knabbels, gloeikoolgruis, moeraskaas of Mika's vet, guhslijm) en vullen maar: brouw een Guhdrankje.",
      "guhs:drankje_van_vahoegheid", [adv("guhdrankje_gebrouwen")], rewards=(("guhs:grillspiespoeder", 4),), x=0, y=y, shape="gear", xp=150)
    q("sb_alle_drankjes", "Alle Guhdrankjes", "Brouw ze allemaal: Vahoegheid, Rookloop, Sluipknabbel en Guhsprong.", "guhs:guhsprongdrankje",
      [item("guhs:drankje_van_vahoegheid"), item("guhs:rookloopdrankje"), item("guhs:sluipknabbeldrankje"), item("guhs:guhsprongdrankje")],
      rewards=(("guhs:kaas_knabbels", 24),), x=2, y=y, shape="rsquare", xp=200)
    q("sb_mikakop", "Hoofdzaak", "Knekel-Mika's laten heel soms een &8verkoolde mikakop&r vallen. Verzamel er drie.", "guhs:verkoolde_mikakop",
      [item("guhs:verkoolde_mikakop", 3)], x=4, y=y, shape="diamond", xp=150)
    q("sb_baas", "Doorgebakken!", "Zet 4 asblokken in een T en drie verkoolde mikakoppen erop: de &cAangebrande Mika&r wordt wakker! Versla hem voor de &6gloeister&r.",
      "guhs:gloeister", [adv("aangebrande_mika_verslagen")], rewards=(("guhs:vahoege_vads_ingot", 4),), x=6, y=y, shape="octagon", xp=500)
    q("sb_baken", "Een baken van knabbels", "Maak een &6Knabbelbaken&r (glas, de gloeister, grillkool), zet het op een piramide van &dblokken vahoege vads&r (negen vadsstaven per blok) en kies een guh-effect. Ook je guhs krijgen het!",
      "guhs:knabbelbaken", [adv("knabbelbaken_aan")], rewards=(("guhs:block_of_kaasknabbels", 2),), x=8, y=y, shape="gear", xp=300)
    y2 = 65.5
    q("sb_rookguh", "Vahoeg naar huis", "Een zielige &7Rookguh&r zweeft rond in de Rookdelta. Voer hem kaasknabbels (of gooi ze naar hem) tot hij vahoeg naar huis zweeft.",
      "guhs:kaas_knabbels", [adv("rookguh_gered")], rewards=(("guhs:gefrituurde_kaasknabbels", 4),), x=-8, y=y2, shape="hexagon", xp=150)
    q("sb_rookguh_10", "Rookguh-redder", "Red 10 Rookguhs. Je Guhdex houdt bij hoeveel het er al zijn.", "guhs:gefrituurde_kaasknabbels",
      [adv("rookguh_redder")], rewards=(("guhs:vahoege_vads_ingot", 2),), x=-6, y=y2, shape="octagon", xp=300)
    q("sb_paleis", "Wie heeft onze knabbels?", "Vind het &cMika-grillpaleis&r: een paleis van roosterijzer boven de kaasfrituursaus, vol gestolen kaasknabbels.",
      "guhs:block_of_kaasknabbels", [structure("mika_grillpaleis")], rewards=(("guhs:kaas_knabbels", 16),), x=-4, y=y2, shape="hexagon", xp=150)
    q("sb_ruilen", "Vads tegen buit", "Draag iets van vahoege vads, dan laten de Nether-Mika's je met rust. Geef er een een vahoege vads-staaf: hij gooit je iets terug!",
      "guhs:vahoege_vads_ingot", [adv("nether_mika_ruil")], rewards=(("guhs:rookloopdrankje", 1),), x=-2, y=y2)
    q("sb_asguh", "Gloeiende wangetjes", "In het Asdal woont de &7Asguh&r: grijs en roetig, met gloeiende wangetjes. Vuur doet hem niks. Tem er een!",
      "guhs:as_blok", [adv("tamed_asguh")], rewards=(("guhs:kaas_knabbels", 16),), x=0, y=y2, shape="gear", xp=200)
    q("sb_dex", "De Barbecuether in de Guhdex", "Ga vlak naast een Rookguh, een Vonk-Mika, een Knekel-Mika en een Asguh staan.", "guhs:guhdex",
      [adv("found_rookguh"), adv("found_vonk_mika"), adv("found_knekel_mika"), adv("seen_asguh")], rewards=(("guhs:kaas_knabbels", 16),), x=2, y=y2)
    q("sb_asguh_blij", "Asguh-knuffel", "Geef een Asguh een kaasknabbel en zie hem gloeien van blijdschap.", "guhs:kaas_knabbels",
      [adv("asguh_gevoerd")], x=4, y=y2, shape="diamond")
    q("sb_sluip", "Zo stil als een knabbel", "Drink een Guhdrankje. Probeer de Sluipknabbel eens: dan hoort niemand je meer.", "guhs:sluipknabbeldrankje",
      [adv("guhdrankje_gedronken")], x=6, y=y2, shape="diamond")
