"""
einde2 (the Guheinde expansion), slice deco: the small decoration buildings of the outer islands.
Java: nl.juiced.guhs.feature.einde2.deco (E2DecoFeature). The templates: einde2_deco_bouw.py (+ _a, _b, _c); the textures
and models of its four little blocks: einde2_deco_tex.py; the wiki texts: einde2_deco_wiki.py.

Twenty little places, each bound to the biome it belongs to (DECO below): scenery with a sign joke and now and then a
modest chest, every one a wink at the Guhvatar's world or something the Mika's left behind. No residents with errands, no
quest, not in the Superkompas, not protected.

  - every building is ONE structure guhs:<id> of the type guhs:einde2_deco_bouwsel (E2DecoStructure.java: a
    guhs:guheinde_eiland that looks at the ground under its own piece: it only starts far from the main island, where the
    island is really there under ALL of it and flat enough) with its own set, its own biome tag has_structure/<id> and
    its own salt (EERSTE_SALT + SALT_STAP x its place in DECO: never change one, never reorder);
  - rare: the sets below; each gives way to every other guhs building (h.VOORRANG 5-19; BouwRuimte keeps the room);
  - the piece stands G blocks deep in the island (start_height -G, see einde2_deco_bouw.py);
  - a sidecar data/guhs/einde2/plekken/<id>.json per building with its spots (bordje, kist, midden, the creatures that live
    there and a named spot or two): a later phase can find a copy (Plekken.bij) and hang something on it;
  - three modest loot tables: chests/einde2_deco, einde2_deco_thee (the tea stall), einde2_deco_mika (what the Mika's left).

build(h) makes everything (h = make_v2).
"""
import json
import os

import features.einde2_kit as kit
from features import einde2_deco_bouw as bouw
from features import einde2_deco_tex as tex

EERSTE_SALT = 21400801
# the salts lie SALT_STAP apart, not next to each other: vanilla seeds the random of a set's grid cell with world seed + salt,
# and java.util.Random gives nearly the same first numbers for seeds that lie close together, so sets with the same grid and
# consecutive salts put their buildings side by side (seen on a dev server: three buildings of one biome in a row, 32-48
# blocks apart; measured: a pair lands within 3 chunks 12 x as often as chance). With this step they are independent
# (W/scratch/deco/salts.py looked for it; E2DecoGameTests.einde2DecoLosVanElkaar keeps it so)
SALT_STAP = 29767
GEWOON = ["gatenkaasheuvels", "korstwoestijn"]
# the grid of a set (chunks): the two common biomes are everywhere, so their buildings stand far apart; a rare biome is a
# patch of a few hundred blocks (2-5 % of the land each), so a building of it needs a finer grid to be met at all: the biome
# itself keeps it rare. Measured with E2DecoGameTests.einde2DecoZeldzaam (it logs what starts per 4000 x 4000 blocks): about
# 20 buildings in the two common biomes together, and one or two per patch of a rare biome
RUIM, FIJN = (48, 20), (14, 6)
POST = (64, 28)           # (the wachtpost is small and fits almost anywhere: a wider grid keeps it as rare as the others)

# id: (the Dutch name, its biomes, (spacing, separation)): IN THE ORDER OF THE SALTS (build contract 3.3)
DECO = {
    "deco_luchtbizonstal": ("Verlaten luchtbizon-stal", ["kruimelwolken"], FIJN),
    "deco_meditatiepaal": ("Meditatiepaal", ["kruimelwolken"], FIJN),
    "deco_luchtbalveld_ruine": ("Luchtbalveld-ruïne", ["kruimelwolken"], FIJN),
    "deco_geestenoase_vijver": ("Geestenoase-vijver", ["schimmelkaasvlakte"], FIJN),
    "deco_ijsvissershutje": ("IJsvissershutje", ["schimmelkaasvlakte"], FIJN),
    "deco_waterstam_kano": ("Waterstam-kano op het droge", ["schimmelkaasvlakte"], FIJN),
    "deco_das_molguh_grot": ("Das-molguh-grot", ["kristalkaasklippen"], FIJN),
    "deco_aardrommel_ring": ("Verlaten Aardrommel-ring", ["kristalkaasklippen"], FIJN),
    "deco_koolkraam_wrak": ("Koolkraam-wrak", ["kristalkaasklippen"], FIJN),
    "deco_sintelstrand_huisje": ("Sintelstrand-zomerhuisje", ["knabbelkoorbos"], FIJN),
    "deco_zonnekrijger_altaar": ("Zonnekrijger-altaar", ["knabbelkoorbos"], FIJN),
    "deco_theekraampje": ("Verlaten theekraampje", ["knabbelkoorbos"], FIJN),
    "deco_mika_wachtpost": ("Mika-wachtpost", GEWOON, POST),
    "deco_vetschipwrak": ("Vetschipwrak", GEWOON, RUIM),
    "deco_karavaanresten": ("Karavaanresten bij een oase", ["korstwoestijn"], RUIM),
    "deco_bevroren_kaasbol": ("Bevroren kaasbol", ["schimmelkaasvlakte"], FIJN),
    "deco_geheime_tunnel": ("Geheime tunnel", ["kristalkaasklippen"], FIJN),
    "deco_boemerangboom": ("Boemerangboom", ["gatenkaasheuvels"], RUIM),
    "deco_pai_sho_tafeltje": ("Pai Sho-tafeltje", ["korstwoestijn"], RUIM),
    "deco_appa_afdruk": ("Appa-afdruk", ["korstwoestijn"], RUIM),
}
TYPE = "guhs:einde2_deco_bouwsel"
MIN_GROND = 52            # the island under the piece is at least this high at nine points of its footprint (E2DecoStructure)
MAX_VERSCHIL = 4          # and those nine differ at most this much: the footing is G = 4 deep, so nothing hangs in the air

TEXTS = {
    "block.guhs.einde2_deco_kool": "Kool",
    "block.guhs.einde2_deco_kool.lore": "MIJN KOLEN! (Niet op springen, njeg)",
    "block.guhs.einde2_deco_windgong": "Windgong",
    "block.guhs.einde2_deco_windgong.lore": "Tingelt in de wind van de leegte. Hangt onder een blok",
    "block.guhs.einde2_deco_eeuwige_vlam": "Eeuwige vlam",
    "block.guhs.einde2_deco_eeuwige_vlam.lore": "Een cadeautje van de eerste draakguhs. Brandt niemand en gaat nooit uit",
    "block.guhs.einde2_deco_lotustegel": "Witte lotustegel",
    "block.guhs.einde2_deco_lotustegel.lore": "De witte lotus opent zich voor wie haar geheimen kent. En voor wie thee meeneemt",
}


# =====================================================================================================================
# loot: modest (a snack, something of the outer islands, now and then one of the slice's own little blocks)
# =====================================================================================================================
def _item(name, lo=1, hi=1, weight=1):
    e = {"type": "minecraft:item", "name": name if ":" in name else f"minecraft:{name}", "weight": weight}
    if (lo, hi) != (1, 1):
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def _rol(lo, hi, entries):
    return {"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries}


def _soms(kans, entries):
    return {"rolls": 1, "entries": entries, "conditions": [{"condition": "minecraft:random_chance", "chance": kans}]}


LOOT = {
    "einde2_deco": [
        _rol(2, 4, [_item("guhs:kaas_knabbels", 2, 5, 8), _item("bread", 1, 2, 4), _item("apple", 1, 3, 3), _item("guhs:kristalkaas_brokje", 1, 3, 3),
                    _item("guhs:knabbelkoorvrucht", 1, 2, 3), _item("guhs:lichtstaafje", 1, 2, 3), _item("guhs:guh_kristal", 1, 2, 2),
                    _item("string", 1, 3, 2), _item("stick", 1, 4, 2), _item("guhs:gepofte_knabbelkoorvrucht", 1, 1, 2)]),
        _soms(0.25, [_item("guhs:einde2_deco_windgong", weight=2), _item("guhs:einde2_deco_kool", 1, 2, 3), _item("guhs:einde2_deco_lotustegel")]),
    ],
    "einde2_deco_thee": [
        _rol(2, 3, [_item("guhs:theekruid", 1, 3, 6), _item("guhs:theekruidzaadjes", 1, 2, 3), _item("guhs:kaas_knabbels", 1, 3, 4),
                    _item("guhs:knabbelkoorvrucht", 1, 2, 2), _item("sugar", 1, 3, 2), _item("flower_pot", 1, 1, 1), _item("bowl", 1, 2, 2)]),
        _soms(0.2, [_item("guhs:einde2_deco_lotustegel")]),
    ],
    "einde2_deco_mika": [
        _rol(2, 4, [_item("guhs:kaas_knabbels", 2, 5, 6), _item("guhs:mika_vet", 1, 3, 5), _item("guhs:gefrituurde_kaasknabbels", 1, 2, 3),
                    _item("guhs:mika_traan", 1, 1, 2), _item("iron_ingot", 1, 3, 3), _item("gold_nugget", 2, 6, 3), _item("guhs:mika_steen", 2, 4, 3),
                    _item("bone", 1, 2, 2)]),
    ],
}


def loot(h):
    for naam, pools in LOOT.items():
        h.w(f"{h.D}/loot_table/chests/{naam}.json", {"type": "minecraft:chest", "pools": pools})
    for blok in tex.BLOCKS:
        h.self_drop(blok)
    h.add_tag("minecraft/tags/block/mineable/hoe", ["guhs:einde2_deco_kool"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:einde2_deco_windgong", "guhs:einde2_deco_lotustegel"])


# =====================================================================================================================
# worldgen: one guhs:guheinde_eiland structure per building
# =====================================================================================================================
def worldgen(h, bouwsels):
    D = h.D
    # (make_v2.bouwruimte() asks how far the pieces of this type reach: a jigsaw whose start piece is centred on the start)
    h.RUIMTE_HOOKS[TYPE] = lambda s, jigsaw_reach: jigsaw_reach(s["jigsaw"], False)
    for i, (id, (naam, biomes, (spacing, separation))) in enumerate(DECO.items()):
        b = bouwsels[id][0]
        template = f"einde2_deco/{id}"
        h.w(f"{D}/worldgen/template_pool/{template}.json", {"fallback": "minecraft:empty", "elements": [
            {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{template}",
                                      "projection": "rigid", "processors": "minecraft:empty"}}]})
        kit.eiland_structuur(h, id, start_pool=f"guhs:{template}", start_jigsaw=f"guhs:{template}", biomes=biomes, spacing=spacing,
                             separation=separation, salt=EERSTE_SALT + i * SALT_STAP, min_surface_y=MIN_GROND, size=1, reach=24, voorrang=5 + i % 15,
                             plekken=[(b, template)])

        # the kit wrote a guhs:guheinde_eiland; a decoration building is a guhs:einde2_deco_bouwsel (E2DecoStructure.java):
        # the same, but it looks at the ground under the piece itself (all of it island, and flat enough), and the piece
        # stands G blocks deep in the island: its ground layer replaces the island's top block in the middle
        def bouwsel(s):
            s["type"] = TYPE
            s.pop("check_radius", None)
            s["max_verschil"] = MAX_VERSCHIL
            s["jigsaw"]["start_height"] = {"absolute": -bouw.G}
        h.patch_json(f"{D}/worldgen/structure/{id}.json", bouwsel)
        kit.lang(h, f"structure.guhs.{id}", naam)


def advancements(h):
    kit.zichtbaar(h, "einde2_deco_kolen", "guheinde_poort", "guhs:einde2_deco_kool", "task", "MIJN KOLEN!",
                  "Spring op een kool. Iemand, ergens, voelt dat")


def texts(h):
    for key, text in TEXTS.items():
        kit.lang(h, key, text)


def testkamer(h):
    """The rooms of E2DecoGameTests: a floor of kaaskorst under open air (for the blocks' own rules), and a field."""
    s = h.Structure((7, 6, 7))
    s.fill(0, 0, 0, 6, 0, 6, "guhs:kaaskorst")
    s.save("einde2_deco_test_kamer")
    # and the field every template is put down on, one after the other (24 x 24 at most, with a rim)
    s = h.Structure((26, 24, 26))
    s.fill(0, 0, 0, 25, 0, 25, "guhs:kaaskorst")
    s.save("einde2_deco_test_veld")


def selfcheck(h, bouwsels):
    """Every building passed its own check and has its structure (of our own type, standing G deep), set, tag, pool,
    sidecar and name; the salts are the twenty of the table; every block has its files and its name."""
    A, D = h.A, h.D
    fout = []
    for id, (b, problemen) in bouwsels.items():
        for p in problemen:
            fout.append(f"{id}: {p}")
    if sorted(bouw.BOUWERS) != sorted(DECO):
        fout.append(f"BOUWERS en DECO verschillen: {sorted(set(bouw.BOUWERS) ^ set(DECO))}")
    for i, id in enumerate(DECO):
        for path in (f"{D}/structure/einde2_deco/{id}.nbt", f"{D}/worldgen/structure/{id}.json", f"{D}/worldgen/structure_set/{id}.json",
                     f"{D}/worldgen/template_pool/einde2_deco/{id}.json", f"{D}/tags/worldgen/biome/has_structure/{id}.json",
                     f"{D}/einde2/plekken/{id}.json"):
            if not os.path.exists(path):
                fout.append(f"mist {path}")
        if f"structure.guhs.{id}" not in h.NL:
            fout.append(f"lang structure.guhs.{id}")
        pad = f"{D}/worldgen/structure_set/{id}.json"
        if os.path.exists(pad):
            p = json.load(open(pad, encoding="utf-8"))["placement"]
            if p["salt"] != EERSTE_SALT + i * SALT_STAP:
                fout.append(f"{id}: salt {p['salt']}")
            if p["spacing"] < 12 or p["separation"] < 5 or p["separation"] >= p["spacing"]:
                fout.append(f"{id}: grid {p['spacing']} / {p['separation']}")
        st = json.load(open(f"{D}/worldgen/structure/{id}.json", encoding="utf-8"))
        if st["type"] != TYPE or st["jigsaw"]["start_height"] != {"absolute": -bouw.G} or "check_radius" in st:
            fout.append(f"{id}: not a {TYPE} that stands {bouw.G} deep")
        if not 5 <= h.VOORRANG.get(id, -1) <= 19:
            fout.append(f"{id}: voorrang {h.VOORRANG.get(id)}")
    for blok in tex.BLOCKS:
        for path in (f"{A}/blockstates/{blok}.json", f"{A}/models/item/{blok}.json", f"{D}/loot_table/blocks/{blok}.json",
                     f"{A}/textures/block/{blok}.png"):
            if not os.path.exists(path):
                fout.append(f"mist {path}")
        for key in (f"block.guhs.{blok}", f"block.guhs.{blok}.lore"):
            if key not in h.NL:
                fout.append(f"lang {key}")
    for naam in LOOT:
        if f"guhs:chests/{naam}" not in bouw.KISTEN:
            fout.append(f"loot {naam} hoort bij geen kist")
    if fout:
        raise SystemExit("einde2_deco: " + "\n  ".join(fout))


def build(h):
    if not hasattr(h, "ms"):
        h.ms = __import__("make_structures")
    tex.textures(h)
    bouwsels = bouw.bouw_alles(h)
    worldgen(h, bouwsels)
    loot(h)
    advancements(h)
    texts(h)
    testkamer(h)
    selfcheck(h, bouwsels)
    for id, (b, _problemen) in bouwsels.items():
        print(f"einde2_deco {id}: {b.W} x {b.H} x {b.D}, {b.stats}")
